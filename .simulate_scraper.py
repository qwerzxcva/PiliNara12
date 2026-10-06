#!/usr/bin/env python3
"""模拟 AnimekoScraper 的核心算法，用真实配置数据验证正确性。

复刻 Kotlin 版的：
1. compileWithNamedGroup：把 (?<name>) 转普通组 + 计算组序号
2. extractVideoUrl：正则提视频地址（url= 解码 / 命名组 / 整条匹配）
3. episodeNum / matchEpisodeSortFromName：集数提取排序
4. absUrl：相对链接补全
5. isLikelyPlayable：可播放性判定
6. buildSearchUrl：searchUseOnlyFirstWord + searchRemoveSpecial
"""
import json
import re
import urllib.parse
import sys
import argparse
from pathlib import Path
import xml.etree.ElementTree as ET

def load_sources(path):
    with open(path, encoding='utf-8') as f:
        d = json.load(f)
    return d['exportedMediaSourceDataList']['mediaSources']

def compile_with_named_group(pattern, name):
    """复刻 Kotlin compileWithNamedGroup"""
    marker = "(?<%s>" % name
    idx = pattern.find(marker)
    if idx < 0:
        return re.compile(pattern), None
    prefix = pattern[:idx]
    group_no = 1
    i = 0
    while i < len(prefix):
        c = prefix[i]
        if c == '(':
            is_noncap = prefix.startswith('(?:', i) or prefix.startswith('(?=', i) \
                or prefix.startswith('(?!', i) or prefix.startswith('(?<=', i) \
                or prefix.startswith('(?<!', i)
            if not is_noncap:
                group_no += 1
        i += 1
    return re.compile(pattern.replace(marker, '(')), group_no

def expand_to_url(html, hit_index):
    """复刻 Kotlin expandToUrl：特征分支命中时恢复完整 http(s) URL。"""
    if hit_index < 0 or hit_index >= len(html):
        return ""
    start = hit_index
    probe = hit_index
    while probe > 0 and hit_index - probe < 256:
        probe -= 1
        if html[probe] in '\"\'<> \n':
            break
        if html[probe:probe + 8].lower() == 'https://':
            start = probe
            break
        if html[probe:probe + 7].lower() == 'http://':
            start = probe
            break
    if not html[start:start + 4].lower() == 'http':
        return ""
    end = start
    while end < len(html) and html[end] not in '\"\'<> \n\t':
        end += 1
    return html[start:end]

def extract_video_url(html, pattern):
    """复刻 Kotlin extractVideoUrl（含特征分支扩展）。"""
    if not pattern:
        return ""
    re_obj, v_group = compile_with_named_group(pattern, "v")
    m = re_obj.search(html)
    if not m:
        return ""
    raw = ""
    if v_group is not None:
        try:
            raw = m.group(v_group) or ""
        except IndexError:
            raw = ""
    if not raw:
        raw = m.group(0) or ""
    if not raw:
        return ""
    if raw.lower().startswith('http'):
        return raw
    # akamaized/bilivideo.com 等分支只匹配特征词，需恢复完整 URL。
    return expand_to_url(html, m.start())

def episode_num(name, custom_pattern):
    """复刻 Kotlin episodeNum"""
    if custom_pattern:
        re_obj, idx = compile_with_named_group(custom_pattern, "ep")
        m = re_obj.search(name)
        if m:
            raw = m.group(idx) if idx is not None else m.group(0)
            if raw:
                digits = ''.join(ch for ch in raw if ch.isdigit())
                if digits:
                    return int(digits)
    for pat in [r'第\s*(\d+)', r'(?:EP|Ep|ep)\s*(\d+)', r'(?:^|[^0-9])(\d{1,4})(?:[^0-9]|$)']:
        m = re.search(pat, name)
        if m:
            try:
                return int(m.group(1))
            except (ValueError, IndexError):
                pass
    return 2**31 - 1  # Int.MAX_VALUE

def abs_url(v, base):
    if not v or not v.strip():
        return ""
    v = v.strip()
    if v.startswith('//'):
        scheme = urllib.parse.urlparse(base).scheme or 'https'
        return scheme + ':' + v
    return urllib.parse.urljoin(base, v)

def is_likely_playable(url):
    if not url:
        return False
    lower = url.lower()
    if lower.startswith('magnet:') or lower.endswith('.torrent'):
        return False
    if '.m3u8' in lower or '.mpd' in lower:
        return True
    media_ext = ['.mp4', '.m4v', '.webm', '.mkv', '.flv', '.mp3', '.m4a', '.aac', '.flac', '.ogg', '.wav', '.ts']
    path = urllib.parse.urlparse(url).path or ''
    if any(path.lower().endswith(e) for e in media_ext):
        return True
    if lower.endswith('.html') or lower.endswith('.htm') or lower.endswith('.php'):
        return False
    return False

def simulate_rss_atom():
    """验证 RSS/Atom 的核心语义：enclosure 优先、媒体封面不误判、Atom rel。"""
    xml = '''<feed xmlns="http://www.w3.org/2005/Atom" xmlns:m="http://search.yahoo.com/mrss/">
      <title>测试订阅</title>
      <entry><title>第一集</title>
        <link rel="self" href="https://x/self"/>
        <link rel="alternate" href="https://x/page/1"/>
        <link rel="enclosure" href="https://cdn/x.mp4"/>
        <m:content medium="image" url="https://img/x.jpg"/>
        <summary><![CDATA[<b>说明</b>]]></summary>
      </entry>
    </feed>'''
    root = ET.fromstring(xml)
    ns = {'a': 'http://www.w3.org/2005/Atom', 'm': 'http://search.yahoo.com/mrss/'}
    entry = root.find('a:entry', ns)
    assert entry is not None
    links = {x.get('rel', 'alternate'): x.get('href', '') for x in entry.findall('a:link', ns)}
    assert links['alternate'] == 'https://x/page/1'
    assert links['enclosure'] == 'https://cdn/x.mp4'
    image = entry.find('m:content', ns)
    assert image is not None and image.get('url') == 'https://img/x.jpg'
    print('  RSS/Atom：alternate、enclosure、media image 语义通过')

def simulate_json_shapes():
    """验证 JSON 数组、items/list/data 形态及坏条目跳过。"""
    cases = [
        {'items': [{'title': 'A', 'link': 'https://x/a.mp4'}, {'title': '坏条目'}]},
        {'list': [{'name': 'B', 'url': 'https://x/b.m3u8'}]},
        [{'name': 'C', 'playUrl': 'https://x/c.mp4'}],
    ]
    for value in cases:
        root = value if isinstance(value, dict) else {'items': value}
        arr = root.get('items') or root.get('list') or root.get('data')
        assert isinstance(arr, list)
        valid = [x for x in arr if isinstance(x, dict) and (x.get('link') or x.get('url') or x.get('playUrl'))]
        assert valid
    print('  JSON：items/list/data + 坏条目跳过通过')

def build_search_url(cfg, keyword):
    k = keyword
    if cfg.get('searchUseOnlyFirstWord'):
        k = keyword.strip().split()[0] if keyword.strip().split() else keyword.strip()
    if cfg.get('searchRemoveSpecial'):
        k = re.sub(r'[^\w\s]', '', k, flags=re.UNICODE).strip()
    # 对齐 java.net.URLEncoder.encode：空格编码为 +，而非 quote() 的 %20。
    return cfg.get('searchUrl', '').replace('{keyword}', urllib.parse.quote_plus(k, safe=''))

# ==================== 测试 ====================
def test_named_group():
    print("=== 测试1: 命名组序号计算 ===")
    cases = [
        # 视频地址命名组 (?<v>)
        (r"(^http(s)?:\/\/(?!.*http(s)?:\/\/).+((\.mp4)|(\.mkv)|(m3u8)).*(\?.+)?)|(akamaized)|(bilivideo.com)|(url=(?<v>.+playlist.m3u8))", "v"),
        (r"(?<v>https?:\/\/(?:[^\/]*\.)?(vdownload|abre-videos|xvideos).*?(1080).*\.(m3u8|mp4|vip|xigua\.php)(?:\?.+)?)", "v"),
        # 集数命名组 (?<ep>)
        (r"第\s*(?<ep>.+)\s*[话集]", "ep"),
        (r"(第\s*(?<ep>.+)\s*[话集])|1080P", "ep"),
    ]
    for (pat, name), expected in zip(cases, [12, 1, 1, 2]):
        re_obj, idx = compile_with_named_group(pat, name)
        assert idx == expected, (name, idx, expected)
        print(f"  组 {name}: 序号={idx}  转换后={re_obj.pattern[:60]}")
    print()

def test_extract_video():
    print("=== 模拟视频提取（生产实现另有 Kotlin 回归） ===")
    url = "https://cdn.akamaized.net/video/index.m3u8?sig=a%2Bb"
    html = '<script>var u="' + url + '";</script>'
    got = extract_video_url(html, r"(akamaized)")
    assert got == url, (got, url)
    direct = "https://ordinary.cdn.test/a.m3u8"
    assert extract_video_url(direct, r"^https?://.+m3u8$") == direct
    assert extract_video_url('text only', r"(akamaized)") == ""
    print("PASS exact URL assertions")

def test_episode_sort():
    print("=== 测试3: episodeNum 集数排序 ===")
    custom = r"第\s*(?<ep>.+)\s*[话集]"
    names = ["第1集", "第10集", "第2集", "SP特别篇", "EP03", "P5"]
    sorted_names = sorted(names, key=lambda n: episode_num(n, custom))
    assert sorted_names == ['第1集', '第2集', 'EP03', 'P5', '第10集', 'SP特别篇'], sorted_names
    print("  排序前:", names)
    print("  排序后:", sorted_names)
    print()

def test_abs_url():
    print("=== 测试4: absUrl ===")
    base = "https://www.example.com/vod/detail/1.html"
    expected = {
        "/play/1.m3u8": "https://www.example.com/play/1.m3u8",
        "//cdn.com/a.mp4": "https://cdn.com/a.mp4",
        "play/2.m3u8": "https://www.example.com/vod/detail/play/2.m3u8",
        "https://x.com/y.mp4": "https://x.com/y.mp4",
    }
    for v, want in expected.items():
        got = abs_url(v, base)
        assert got == want, (v, got, want)
        print(f"  {v:30} -> {got}")
    print()

def test_search_url():
    print("=== 测试5: buildSearchUrl ===")
    cfg = {"searchUrl": "https://x.com/search/{keyword}.html", "searchUseOnlyFirstWord": True, "searchRemoveSpecial": True}
    got = build_search_url(cfg, "海贼王 第1季!")
    assert "第1季" not in got and "+" not in got, got
    assert "海贼王" in urllib.parse.unquote_plus(got.rsplit('/', 1)[-1].split('.', 1)[0]), got
    print("  关键词'海贼王 第1季!':", got)
    print()

def test_all_sources(config):
    print("=== 配置正则编译检查（不代表源可播放） ===")
    ms = load_sources(config)
    ok = 0; fail = 0
    web = [m for m in ms if m.get('factoryId')=='web-selector']
    for m in web:
        sc = m['arguments'].get('searchConfig', {})
        mv = sc.get('matchVideo', {})
        # 验证 matchVideoUrl 正则能编译
        pat = mv.get('matchVideoUrl', '')
        if pat:
            try:
                compile_with_named_group(pat, 'v')
                ok += 1
            except re.error as e:
                fail += 1
                print(f"  正则编译失败: {m['arguments']['name']}: {e}")
        # 验证 matchEpisodeSortFromName 能编译
        ep = sc.get('selectorChannelFormatNoChannel', {}).get('matchEpisodeSortFromName', '')
        if ep:
            try:
                compile_with_named_group(ep, 'ep')
            except re.error as e:
                fail += 1
                print(f"  集数正则失败: {m['arguments']['name']}: {e}")
    assert web, 'No web-selector sources in supplied configuration'
    assert ok == len(web) and fail == 0, (ok, len(web), fail)
    print(f"  web-selector 源: {len(web)} 个，matchVideoUrl 正则编译通过 {ok} 个，失败 {fail} 个")
    print()

if __name__ == '__main__':
    test_named_group()
    test_extract_video()
    test_episode_sort()
    test_abs_url()
    test_search_url()
    simulate_rss_atom()
    simulate_json_shapes()
    parser = argparse.ArgumentParser()
    parser.add_argument('--config', type=Path, help='Explicit source JSON; missing/bad config fails')
    args = parser.parse_args()
    if args.config is not None:
        test_all_sources(args.config)
    else:
        print('NOT RUN: external source configuration (supply --config PATH)')
    print("=== 已选择的模拟检查完成；不等同 Kotlin/设备验证 ===")
