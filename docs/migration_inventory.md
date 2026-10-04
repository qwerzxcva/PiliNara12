# Flutter → Kotlin+Rust 功能移植总清单（Master Inventory）

> 参照物：`~/flutter_ref/`（git 0635112^ 取出的 Flutter 版 lib/，1422 dart 文件，~48万行，其中 grpc/ 24.9万行为自动生成可忽略，实际业务代码约 23 万行）
> 现状：Kotlin 76 文件 / 8214 行 + Rust 773 行
> 约定：✅已移植 🟨部分/框架 🟥未开始 ➖Flutter独有（评估后不移植或后期再说）

## 一、总体进度（按功能域加权估算：约 8%）

| 功能域 | Flutter 规模 | 状态 | Kotlin 对应 |
|---|---|---|---|
| 视频播放链路 | pages/video/ 21380行 | 🟨 25% | playback/VideoPlayerScreen+ViewModel（DASH合并、倍速、PiP、音量；缺：选集/多P、字幕、缩略图预览、手势、投屏） |
| 设置 | pages/setting/ 11687行 | 🟨 10% | ui/settings/SettingsScreen（DataStore 少量项；缺：主题/播放设置/弹幕设置/cookie管理/网络代理等全套） |
| 直播 | pages/live*/ 7000+行 | 🟥 5% | ui/live/LiveRoomScreen 仅 UI 壳 |
| 动态 | pages/dynamics*/ 10000+行 | 🟨 15% | DynamicsScreen 聚合流+翻页（缺：详情/评论/转发/发布/话题/图片九宫格） |
| 弹幕 | pages/danmaku/ 2351行+utils | 🟨 40% | DanmakuView+Rust danmaku.rs（缺：屏蔽规则UI/合并/密度调节接UI） |
| 收藏 | pages/fav*/ 3500+行 | 🟨 30% | LibraryScreens 三屏（缺：收藏夹管理/排序/搜索/fav_panel选夹收藏） |
| 搜索 | pages/search*/ 4000+行 | 🟨 30% | SearchScreen（缺：热搜/筛选/分区/历史记录） |
| UP主空间 | pages/member*/ 9000+行 | 🟥 0% | 无 |
| 番剧影视 | pages/pgc*/ 2000+行 | 🟥 0% | 无 |
| 文章专栏 | pages/article*/ 3000+行 | 🟥 0% | 无 |
| 音频 | pages/audio/ 2165行 | 🟥 0% | 无 |
| 私信 | pages/whisper*/ 3000+行 | 🟥 0% | MessageScreen 仅壳 |
| 消息中心 | pages/msg_feed_top/ 1471行 | 🟥 0% | MessageScreen 仅壳 |
| 下载/离线 | pages/download/ 3265行 | 🟥 0% | 无（有 DocumentsProvider 骨架） |
| 登录 | pages/login/ 1991行 | 🟨 40% | 扫码登录+Room 持久化（缺：密码/短信/多账号/cookie校验） |
| 历史/稍后再看 | pages/history+later/ 1700行 | 🟨 25% | HistoryScreen 列表（缺：搜索/暂停记录/稍后再看） |
| 关注/粉丝 | pages/follow+fan/ 1600行 | 🟥 0% | 无 |
| 我的 | pages/mine/ 1752行 | 🟨 30% | MineScreen 基础 |
| 排行榜/热门系列 | pages/rank+popular*/ | 🟥 0% | 无 |
| 今日Watch | pages/today_watch/ | 🟥 0% | 无 |
| AI助手 | pages/ai_chat/ 1762行 | ➖ 依赖B站AI接口，后议 |
| 音乐识别 | pages/music/ 1167行 | ➖ 后议 |
| SponsorBlock | 1218行 | ➖ 后议 |
| DLNA投屏 | 3265+行 | ➖ 后议 |
| WebDAV备份 | 1000行 | ➖ 后议 |
| 弹幕屏蔽 | pages/danmaku_block/ | 🟥 0% | DB表已有，无UI无API |
| 评论 | pages/main_reply/ | 🟨 30% | CommentScreen（缺：回复楼中楼/表情/点赞评论/排序） |
| 底层服务 | services/ 8230行 | 🟨 10% | 部分散落（无 version/upgrade、无 badge、无 quick_start） |
| utils 工具 | utils/ 21883行 | 🟨 15% | wbi/账号/storage 有；无 emote/extension/global_data 等 |
| API 层 | http/ 9815行(29文件) | 🟨 20% | BiliApiClient 集中一个文件，缺大量接口 |

## 二、本批次实施顺序（由用户核心使用路径决定）

### 批次A：播放器补全（pages/video 精华）
1. 多P/选集（pages 列表切换、详情 pages[] 已有）
2. 播放器手势：亮度（左半屏竖滑）/音量（右半屏竖滑）/快进（横滑）
3. 清晰度切换菜单（dash.video[] 的 id 列表 + 登录态对应）
4. 弹幕开关/透明度/大小的设置面板
5. 字幕（subtitle 字段，subUrl 已在 playurl 返回）
6. 相关视频推荐（/x/web-interface/archive/related）
7. 视频简介展开、标签、UP主卡片跳转

### 批次B：搜索+热搜
1. 热搜榜（/x/web-interface/search/square?limit=10 → hot_search）
2. 搜索历史（Room 存储）
3. 结果分类筛选（视频/直播/用户，order/duration 过滤参数）
4. 搜索建议实时联想（已有 suggest API，接输入框）

### 批次C：UP主空间（member）
1. 空间主页（/x/space/wbi/acc/info，wbi）
2. 投稿列表（/x/space/wbi/arc/search，wbi+分页）
3. 关注/取关（/x/relation/modify，csrf）
4. 粉丝/关注数（/x/relation/stat）
5. 空间动态 tab（复用 dynamics feed/space）

### 批次D：番剧/影视（pgc）
1. 番剧首页（/pgc/index）
2. 番剧详情（/pgc/view/web/season?season_id=/ep_id=）
3. 播放（playurl 传 ep_id，与普通视频共用链路）
4. 追番/取消（/pgc/app/follow/add，csrf）

### 批次E：评论区增强
1. 楼中楼回复（/x/v2/reply/reply）
2. 评论点赞（/x/v2/reply/action，csrf）
3. 发评论（/x/v2/reply/add，csrf）
4. 表情包（emote 包）

### 批次F：直播
1. 直播列表（/xlive/web-interface/index/getAllList）
2. 直播间信息（/xlive/web-room/v2/index/getRoomPlayInfo + 链路）
3. 直播播放（HLS 流 ExoPlayer 原生支持）
4. 直播弹幕（websocket）

### 批次G：稍后再看 + 历史增强
1. 稍后再看列表（/x/v2/history/toview/web）
2. 添加/删除稍后再看（csrf）
3. 历史搜索、暂停/恢复记录开关

### 批次H：关注/粉丝 + 消息
1. 关注列表（/x/relation/followings）
2. 粉丝列表（/x/relation/followers）
3. 消息中心真实数据（/x/msgfeed/unread）

### 批次I：下载离线 + 其余
（下载、webdav、dlna、sponsorblock、audio、article 等后期）

## 三、复盘记录
- 2026-10-04 初建：总体 8%。上批完成：互动状态/默认收藏夹/动态页。
- 2026-10-04 r2（批次A+B+C 完成，commit 56d4fac + 9ff4406）：总体 **10%**。
  - 批次A 播放器补全 ✅：多P pages、清晰度切换（dash cachedQualities + Rust选流）、相关视频（archive/related）、弹幕开关/设置面板、分P面板、恢复进度 startPlayback(resumePositionMs)、手势层。剩：字幕、缩略图预览、投屏。
  - 批次B 搜索增强 ✅：热搜榜 /x/v2/search/trending/ranking、搜索历史（进程内）、分类筛选 video/bili_user/live、排序/时长筛选、结果三种卡片真实渲染、无限翻页、建议联想接输入框。剩：历史 Room 持久化、分区搜索。
  - 批次C UP主空间 ✅：SpaceModels（SpaceInfo/RelationStat/SpaceArchive）、getSpaceInfo(wbi)/getRelationStat/getSpaceArchives(wbi+分页)/modifyFollow(csrf)、MemberScreen（头像/等级/签名/粉丝数/投稿卡片/排序/翻页/关注）、member/{mid} 路由接搜索用户点击。剩：空间动态 tab、收藏的 UP。
  - 下一批：批次D 番剧（pgc/index + season 详情 + ep_id 播放）、批次E 评论区增强（楼中楼/点赞/发评论）。
- 2026-10-04 r3（批次E+G 完成，commit ef00f55 + 276f3ac）：总体 **13%**。
  - 批次E 评论区增强 ✅：CommentNode 补 rpid/rpid层级/ctime/rcount/action；getReplyList 楼中楼、likeComment、addComment（csrf）；CommentViewModel（bv→aid、mode 3/2、楼中楼展开、点赞±1、发评论刷新）；CommentScreen 整页重写删假数据（头像/时间/心形/排序Chip/翻页/发送栏）。剩：表情包、@用户、图片评论。
  - 批次G 稍后再看+历史 ✅：getHistoryCursor(cursor 翻页)、getToView/addToView/delToView/delHistory(csrf)；历史长按删除+触底翻页；ToViewScreen 新页+路由+Mine 入口+播放器"稍后看"按钮。剩：历史搜索、暂停记录开关。
  - 累计待办：批次D 番剧、批次F 直播真实链路、批次H 关注/粉丝+消息、批次I 下载离线。
- 2026-10-04 r4（批次F 完成，commit b3c26bc）：总体 **15%**。
  - 批次F 直播 ✅：getRoomPlayInfo(qn=10000 协议/格式/编码全展开)、getInfoByRoom、roomEntryAction 进房上报；pickStream 流选择（HLS>FLV、avc 优先）；LiveRoomScreen 重写——独立 ExoPlayer 播 m3u8、真实标题/分区/人气/未开播态/重试。剩：直播弹幕 websocket、送礼/舰队、清晰度切换、直播列表页（second/getList）。
  - 下一步：批次D 番剧、批次H 关注/粉丝+消息中心真实数据。
- 2026-10-04 r5（批次H 完成，commit 10ad519）：总体 **17%**。
  - 批次H ✅：getFollowings/getFollowers、getMsgUnread/Reply/At/Like 四接口；MessageScreen 整页重写（三 Tab+未读角标+真实消息卡片+登录引导+来源视频跳转）；FollowListScreen 新页（关注/粉丝，点击进空间）+ MemberScreen 粉丝/关注数可点。剩：私聊会话(session_svr)、系统通知。
  - 剩余大批次：D 番剧(pgc)、直播列表页/弹幕ws、设置页扩展、下载离线、表情包、我的页面完善。
- 2026-10-04 r6（批次D+直播列表+设置页完成，commit c89e25f + 32300fc + 播放器接线）：总体 **20%**。
  - 批次D 番剧 ✅：getPgcSeason/getPgcPlayUrl(wbi)/followBangumi(csrf)；Rust select_streams 兼容 result.dash；BangumiScreen（封面/简介/统计/选集横滑/追番/播放）；路由 + Mine 追番中心入口。剩：pgc 首页分类、播放器内 ep_id 模式。
  - 直播列表 ✅：getLiveList(second/getList 分页) + LiveListScreen 网格（人气/分区/主播）+ 路由。
  - 设置页扩展 ✅：StorageManager 补齐 setter；SettingsViewModel 七项状态机；SettingsScreen 重写（Switch/Slider/单选弹窗全真实持久化）；**播放器 ViewModel 读 DataStore**（默认清晰度 qn 映射 + 弹幕开关/透明度真实生效）。
  - 剩余：弹幕屏蔽规则、下载离线、表情包、私聊、播放器字幕/缩略图、直播弹幕ws。
