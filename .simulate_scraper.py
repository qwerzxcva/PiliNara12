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

def extract_video_url(html, pattern):
    """复刻 Kotlin extractVideoUrl"""
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
    decoded = raw
    if 'url=' in raw.lower():
        decoded = raw.split('url=', 1)[1]
        decoded = urllib.parse.unquote(decoded)
    if decoded.startswith('http'):
        return decoded
    return ""

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
    if v.lower().startswith('http'):
        return v
    if v.startswith('//'):
        return 'https:' + v
    if v.startswith('/'):
        return base.rstrip('/') + v
    return base.rstrip('/') + '/' + v

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

def build_search_url(cfg, keyword):
    k = keyword
    if cfg.get('searchUseOnlyFirstWord'):
        k = keyword.strip().split()[0] if keyword.strip().split() else keyword.strip()
    if cfg.get('searchRemoveSpecial'):
        k = re.sub(r'[^\w\s]', '', k, flags=re.UNICODE).strip()
    return cfg.get('searchUrl', '').replace('{keyword}', urllib.parse.quote(k))

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
    for pat, name in cases:
        re_obj, idx = compile_with_named_group(pat, name)
        print(f"  组 {name}: 序号={idx}  转换后={re_obj.pattern[:60]}")
    print()

def test_extract_video():
    print("=== 测试2: extractVideoUrl ===")
    # 模拟真实页面含 url= 编码的视频地址
    html1 = 'var video="url=' + urllib.parse.quote('https://cdn.example.com/v/1.m3u8?token=abc') + '";'
    pat1 = r"url=(?<v>.+playlist.m3u8)"
    print("  命名组 url= 解码:", extract_video_url(html1, pat1))
    
    # 直链在页面
    html2 = 'window.playurl = "https://v.cdn.com/a.mp4?sign=xx";'
    pat2 = r"(^http(s)?:\/\/(?!.*http(s)?:\/\/).+((\.mp4)|(\.mkv)|(m3u8)).*(\?.+)?)"
    print("  直链提取:", extract_video_url(html2, pat2))
    print()

def test_episode_sort():
    print("=== 测试3: episodeNum 集数排序 ===")
    custom = r"第\s*(?<ep>.+)\s*[话集]"
    names = ["第1集", "第10集", "第2集", "SP特别篇", "EP03", "P5"]
    sorted_names = sorted(names, key=lambda n: episode_num(n, custom))
    print("  排序前:", names)
    print("  排序后:", sorted_names)
    print()

def test_abs_url():
    print("=== 测试4: absUrl ===")
    base = "https://www.example.com/vod/detail/1.html"
    for v in ["/play/1.m3u8", "//cdn.com/a.mp4", "play/2.m3u8", "https://x.com/y.mp4"]:
        print(f"  {v:30} -> {abs_url(v, base)}")
    print()

def test_search_url():
    print("=== 测试5: buildSearchUrl ===")
    cfg = {"searchUrl": "https://x.com/search/{keyword}.html", "searchUseOnlyFirstWord": True, "searchRemoveSpecial": True}
    print("  关键词'海贼王 第1季!':", build_search_url(cfg, "海贼王 第1季!"))
    print()

def test_all_sources():
    print("=== 测试6: 用真实 all.json 跑通 70 源配置 ===")
    try:
        ms = load_sources('/workspace/tmp/all.json')
    except Exception as e:
        print("  (无法读 all.json，跳过):", e)
        return
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
    print(f"  web-selector 源: {len(web)} 个，matchVideoUrl 正则编译通过 {ok} 个，失败 {fail} 个")
    print()

if __name__ == '__main__':
    test_named_group()
    test_extract_video()
    test_episode_sort()
    test_abs_url()
    test_search_url()
    test_all_sources()
    print("=== 全部模拟测试完成 ===")
