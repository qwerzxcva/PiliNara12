# B站 Web 端 API 接入清单（供 PiliNara12 使用）

> 依据 SocialSisterYi/bilibili-API-collect（上游仓库已被 GitHub 下架，default branch `deprecated` 仅剩 README；本文以活跃镜像 pskdje/bilibili-API-collect 及 sessionhu.github.io 文档站为准）。所有关键接口均于 2026-10-04 用真实请求验证过（匿名无 Cookie）。

## 0. 全局要求（先做）

- 所有 api.bilibili.com 请求带：`User-Agent`（正常浏览器 UA，勿含 curl/python 等敏感子串）、`Referer: https://www.bilibili.com/`。
- 首次启动先获取 `buvid3`：`GET https://api.bilibili.com/x/frontend/finger/spi` → `data.b_3` / `data.b_4`，写入 Cookie（`buvid3=b_3; buvid4=b_4`），持久化。点赞/投币/搜索等接口要求 Cookie 含 buvid3，否则 -412/风控。
- 搜索接口官方建议：无 Cookie 时先 GET 一遍 https://www.bilibili.com 拿全套 Cookie 再调用。

## 1. 接口清单

### 1.1 视频详情（含 cid）
- URL: `GET https://api.bilibili.com/x/web-interface/view`（wbi 版：`/x/web-interface/wbi/view`，需签名）
- 参数: `bvid` 或 `aid`（二选一）
- 鉴权: 无需登录（受限视频需 SESSDATA）；普通版无需 wbi 签名（已实测匿名可用）
- 返回: `data.cid`（单P）/ `data.pages[].cid`（分P）、`data.aid`、`data.title`、`data.stat` 等
- 示例: `https://api.bilibili.com/x/web-interface/view?bvid=BV1GJ411x7h7`

### 1.2 播放地址 playurl（视频流）
- URL: `GET https://api.bilibili.com/x/player/wbi/playurl`（旧 `/x/player/playurl` 已基本失效，实测 -404）
- 鉴权: **Wbi 签名必须** + Cookie（SESSDATA，匿名亦可但清晰度受限）
- 参数: `bvid|avid`、`cid`(必要)、`qn`、`fnval`、`fnver=0`、`fourk=1`
- `fnval`: `16`=DASH；可按位或 `64`(HDR,需大会员) `128`(4K,需大会员+fourk=1) `256`(杜比) `512`(杜比视界) `1024`(8K) `2048`(AV1)。常规视频/番剧可加 `4048` 全开（普通用 16 即可）
- 清晰度实测（匿名 + wbi 签名，fnval=16）:
  - 返回 DASH video ids 仅 `32(480P)`/`16(360P)`，`accept_quality=[112,80,64,32,16]`
  - **匿名最高 480P（32）**；720P(64) 及以上需登录；1080P60/1080P+/4K/HDR/杜比需大会员
  - 无 Cookie 请求未签名的旧接口返回 -404
- URL 有效期 120min；分P需换 cid 重新请求
- 示例（需 wts/w_rid）: `https://api.bilibili.com/x/player/wbi/playurl?bvid=...&cid=...&qn=64&fnval=16&fnver=0&fourk=1&wts=...&w_rid=...`

### 1.3 点赞/投币/收藏（写操作）
全部 POST，`Content-Type: application/x-www-form-urlencoded`，Cookie 必须含 `SESSDATA + bili_jct + buvid3`，body 带 `csrf=<bili_jct 值>`。

- 点赞: `POST https://api.bilibili.com/x/web-interface/archive/like`
  - body: `aid|bvid`, `like`(1赞/2取消), `csrf`
- 判断是否已赞: `GET /x/web-interface/archive/has/like?bvid=`
- 投币: `POST https://api.bilibili.com/x/web-interface/coin/add`
  - body: `aid|bvid`, `multiply`(1或2,上限2), `select_like`(0/1), `csrf`
- 收藏: `POST https://api.bilibili.com/x/v3/fav/resource/deal`
  - body: `rid=<avid>`, `type=2`, `add_media_ids=<mlid,...>`, `del_media_ids=`, `csrf`, `platform=web`
- 常见错误码: -101 未登录, -111 csrf 校验失败, -104 硬币不足, 34002 不能给自己投币

### 1.4 搜索
- 综合搜索: `GET https://api.bilibili.com/x/web-interface/wbi/search/all/v2?keyword=...`
- 分类搜索: `GET https://api.bilibili.com/x/web-interface/wbi/search/type?search_type=video&keyword=...&page=`
- 鉴权: **Wbi 签名 + Cookie 含 buvid3 + Referer(.bilibili.com) + 正常 UA**；缺 Cookie 常见 -412
- 返回: `data.result[]`（type: video/user/bili_user/media_bangumi…），视频项含 `bvid/aid/title(<em>高亮)/pic/author/play`

### 1.5 评论
- 懒加载版: `GET https://api.bilibili.com/x/v2/reply/wbi/main`（**Wbi 签名**；签名错返回 -403）
  - 参数: `type=1`(视频), `oid=<aid>`, `mode`(0/3热度,2时间), `pagination_str={"offset":""}` 或 `next`
  - 未登录可看（部分场景需 buvid3）；发评论需登录
- 旧版: `GET /x/v2/reply?type=1&oid=...&pn=1&sort=1`（无签名，可作回退）
- 发评论: `POST /x/v2/reply/add` body: `oid,type=1,message,csrf,plat=1`

### 1.6 弹幕
- XML 全量: `GET https://api.bilibili.com/x/v1/dm/list.so?oid=<cid>`（= `https://comment.bilibili.com/{cid}.xml`），**deflate 压缩，需解压**
- protobuf 分段（推荐，6min一包，上限6000条/包）:
  - `GET https://api.bilibili.com/x/v2/dm/web/seg.so?type=1&oid=<cid>&pid=<aid>&segment_index=1`
  - 新版需 wbi: `https://api.bilibili.com/x/v2/dm/wbi/web/seg.so`（签名）
  - 半匿名：无 SESSDATA 部分视频只返回部分弹幕；返回 protobuf（DmSegMobileReply）
- 弹幕元数据（总时长/分段数）: `GET /x/v2/dm/wbi/web/view?type=1&oid=<cid>`（wbi）

### 1.7 用户动态
- 全部关注动态: `GET https://api.bilibili.com/x/polymer/web-dynamic/v1/feed/all?platform=web&features=itemOpusStyle,listOnlyfans,opusBigCover,onlyfansVote,decorationCard,onlyfansAssetsV2,forwardListHidden,ugcDelete`
  - Cookie(SESSDATA) 必要；翻页用返回的 `data.offset`
- 指定用户动态: `GET https://api.bilibili.com/x/polymer/web-dynamic/v1/feed/space?host_mid=<mid>&offset=`
  - 未登录: 需 Cookie 含 buvid3 + Wbi 签名 + `dm_img_list`/`dm_img_str`/`dm_img_inter`/`dm_cover_img_str` 系列风控参数（有运气成分，建议登录态使用）；登录态最稳
- 动态详情: `GET /x/polymer/web-dynamic/v1/detail?id=<dyn_id>`

### 1.8 Wbi 签名算法
1. `GET https://api.bilibili.com/x/web-interface/nav`（无需登录）→ `data.wbi_img.img_url / sub_url`
   - `img_key = 文件名去扩展名`，`sub_key = 同`。（已实测可取；每日更替，建议缓存+过期刷新）
2. `raw = img_key + sub_key`，按固定置换表 `MIXIN_KEY_ENC_TAB = [46,47,18,2,53,8,23,32,15,50,10,31,58,3,45,35,27,43,5,49,33,9,42,19,29,28,14,39,12,38,41,13,37,48,7,16,24,55,40,61,26,17,0,1,60,51,30,4,22,25,54,21,56,59,6,63,57,62,11,36,20,34,44,52]` 重排字符，截前 32 位 → `mixin_key`
3. 参数加 `wts = 当前秒级时间戳`；按 key 字典序排序；value 过滤 `!'()*` 字符；URL encode（value 中 `+` 编为 `%2B` 等，注意与标准 urlencode 差异：`*` 不转义、`+` 转义）
4. `query = k1=v1&k2=v2...`，`w_rid = md5(query + mixin_key)`
5. 最终请求 query 追加 `&wts=...&w_rid=...`
- 签名错误表现: 返回 -352（或 comment 接口 -403）+ `v_voucher`

### 1.9 风控要点
- buvid3：点赞/投币/搜索等强依赖；缺失→-412 或风控。用 finger/spi 获取后持久化，不要复制他人示例值。
- Referer：API 建议带 `https://www.bilibili.com/`；视频流 URL 下载/播放时**必须**带 Referer 否则 403。
- UA：不能含 curl/python/okhttp 等子串；安卓 WebView UA 可用。
- -352/-412 表示被风控，通常是 wbi 未签、参数序错、缺 buvid3 或请求过快；加随机间隔（≥1s）。
- 部分 CDN（upos-hz-mirrorakam.akamaized.net 等）海外直连慢，可按 PiliPlus 方案做 CDN 列表切换。

## 2. 同类开源客户端（README/实现参考）

- **BiliPai**（Kotlin + Compose，与本项目技术栈最接近）: https://github.com/jay3-yy/BiliPai
  - 覆盖：视频、番剧、直播、动态、消息、离线缓存、插件系统、大屏适配；依赖 Media3/ExoPlayer、DanmakuRenderEngine；明确以 bilibili-API-collect 为 API 文档、PiliPlus 为播放链路参考。
- **PiliPlus**（Flutter，功能最全）: https://github.com/bggRGjQaUbCoE/PiliPlus
  - 覆盖：动态编辑、DLNA 投屏、离线缓存、弹幕交互、跳片头尾、登录(扫码/密码/短信)、投票、富文本评论/动态、直播分区、关注分组、稍后再看、WebDAV 备份、CDN 切换等。
- **PiliPalaX**（pilipala 活跃分支，Flutter）: https://github.com/orz12/PiliPalaX （原版 https://github.com/guozhigq/pilipala ）
  - 覆盖：推荐/热门、直播、番剧、离线缓存、回复评论、弹幕、搜索、登录等基础全量。
- 注：本项目 `android/app/.../piliplus/` 包下是自写的 Media3 辅助类（MediaUri、BufferPolicy、SuperResolution、AudioNormalization），与 Flutter 的 PiliPlus 项目无直接代码关系，仅命名相似。

## 3. 落地建议（对应项目已知问题）

1. 视频点击无反应：view 接口取 cid → playurl 需 **wbi 签名**，且必须带 Referer；先实现 §1.8 签名工具类（Kotlin 用 `MessageDigest MD5` + `TreeMap` 排序）。
2. 匿名仅 480P：登录态（扫码已实现）把 SESSDATA 带入 playurl 即可到 1080P。
3. 设置页缺失：最小可用项 = 清晰度偏好、Cookie 管理、UA/Referer 开关、buvid3 重置。
4. 微信登录按钮：B站无微信登录 API，删除；保留 扫码(qrcode) / 密码(SMS/captcha, 复杂) / Cookie 导入 三种。
