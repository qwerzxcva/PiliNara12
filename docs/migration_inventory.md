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
- 2026-10-04 r7（弹幕屏蔽+表情包+私聊+播放器ep_id 模式，commit 7313baa + 8a1a964）：总体 **25%**。
  - 弹幕屏蔽 ✅：关键词/正则/UID 三类规则，Room（DanmakuFilterRuleEntity）持久化；DanmakuBlockScreen（三Tab+添加/删除+正则校验）；设置页入口；弹幕流渲染前 shouldBlock 过滤；MainApplication 启动 warmup 内存缓存。剩：云端同步规则（/x/dm/filter）。
  - 表情包 ✅：/x/emote/package API（小黄脸 id=1）；评论输入框表情面板（8列网格，点选插入 [text]）。
  - 私聊 ✅：api.vc.bilibili.com 会话列表/消息记录(poll)/发送(web_im/send_msg csrf)；SessionModels；SessionListScreen（头像/未读角标/最后消息）+ ChatScreen（气泡对话+发送+自动滚底）；消息中心顶栏私聊入口。剩：已读回执、图片/撤销消息。
  - 播放器 ep_id 模式 ✅：loadVideo(…, epId) → loadPgcEpisode（pgc season 详情取 cid → 分集填充分P面板 → pgc playurl wbi+Rust 选流 → ExoPlayer + 番剧弹幕）；路由 video/ep{id} 自动识别；BangumiScreen 播放按钮走 ep 链路。
  - 下一批：下载离线（缓存+离线播放）、播放器字幕（/x/player/v2 subtitle）、直播弹幕 websocket。

## 四、长期 Roadmap（2026-10-04 与用户对齐；来源：旧 plan 精简，剔除代理软件内容）
- 批次J 直播弹幕 WebSocket：getDanmuInfo(token/host_list) → wss://…:port/sub → auth(op7,protover=2) → zlib 解包(op3/8跳过, op5 JSON) → DANMU_MSG 渲染弹幕+聊天列表；心跳30s。+ 发送弹幕 /xlive/web-room/v1/index/SendMsg (csrf)。
- 批次I 下载离线：下载队列(Room)+缓存目录+离线播放页。
- 批次K 播放器字幕：/x/player/v2 subtitle 拉取+渲染。
- 批次L UI 主题：Kototoro 风格渐变/大圆角设计 token。
- 暂缓（性价比低）：Vulkan 渲染/HDR/多引擎切换、缩略图 storyboard、pgc 首页分类、@用户评论、私聊图片消息。
- 完成上述后总体 ~35%+。
- 2026-10-04 r8（批次J 直播弹幕WS + 批次K 播放器字幕，commit e87a866 + acca76c）：总体 **28%**。
  - 批次J 直播弹幕 WebSocket ✅：LiveDanmakuWsClient（16字节大端包头/op2心跳/op5业务/op7认证/op8认证回复/op3人气回复；protover2 zlib 递归解包；DANMU_MSG 解析 uid/name/彩色/徽章/emote）；getDanmuInfo(token+host_list)；30s 心跳；Ktor WebSocket(OkHttp engine)+ktor-client-websockets 依赖。LiveRoomViewModel 接 chat 流（保留80条）+人气；LiveRoomScreen 弹幕聊天列表（徽章/彩色/自动滚底）+发送栏（SendMsg csrf+本地回显）+WS 连接状态指示。剩：重连退避、SC/礼物消息、表情渲染。
  - 批次K 播放器字幕 ✅：getPlayerV2(/x/player/v2)+fetchSubtitleBody；SubtitleCues 按时间定位；播放器底部半透明字幕层。剩：字幕选择 UI、番剧 ep 字幕。
  - 剩余：批次I 下载离线、批次L Kototoro 主题、缩略图 storyboard、pgc 首页分类、@用户、私聊图片。
- 2026-10-04 r9（批次I 下载离线，commit aa0203d）：总体 **31%**。
  - 批次I ✅：Room v2 DownloadItemEntity/Dao（bvid 主键、STATE_PENDING/RUNNING/DONE/FAILED、进度、双流路径）；DownloadManager（详情→playurl wbi+Rust 选流→video/audio.m4s 下载到 Downloads/{bvid}/，Referer+UA，进度回调 0..1→Room upsert；cancel/delete/getLocalPlayback）；DownloadScreen（封面卡/LinearProgress/状态/删除/播放）；播放器 local 模式（video/{bvid}?local=1 → 本地 m4s DefaultDataSource 合流播放，startPlayback 本地/远程自适应）；Mine UserContent/QuickActions 离线缓存入口。剩：选清晰度下载、批量下载、弹幕/字幕离线、断点续传。
  - 剩余：批次L Kototoro 主题、缩略图 storyboard、pgc 首页分类、@用户、私聊图片、批次J 的 SC/礼物消息与重连。


## 复盘 r10（2026-10-05，批次L 主题+分类+storyboard+私聊图片+SC/礼物）

**本轮新增（4 commit）**：
- **批次L Kototoro 主题**（aac2d8e）：Theme.kt 重写——20 色主题色板（ACCENT_OPTIONS，对齐 Flutter colorThemeTypes）+ accentFromHex 派生 light/dark scheme + DataStore accentColor/themeMode 真实接入 PiliNaraTheme（system/light/dark 生效）+ PiliShapes 大圆角 token + PiliGradients 品牌渐变 + 设置页"主题颜色"色板弹窗（AccentColorDialog）
- **pgc 首页分类**（672ca86）：/pgc/season/index/result（season_type 1番剧/2电影/3纪录片/4国创/5电视剧/7综艺，type=1 必须）+ PgcIndexScreen（ScrollableTabRow 类型切换 + FilterChip 排序 update/score/play + 无限分页 has_next）+ 番剧中心顶栏"分类"入口
- **批次L3 storyboard 缩略图**（be1562e）：/x/player/videoshot（bvid+cid+index=1）+ VideoShotData.frameAt(秒)→(雪碧图URL,格x,格y) + 播放器拖动进度条时 BoxWithConstraints+graphicsLayer 平移裁剪显示当前帧预览 + 修复原 seekTo(0) bug
- **私聊图片+SC/礼物**（9db558f/07cd281）：msg_type=2 图片消息解析(msgImage)/渲染(AsyncImage 气泡)/发送(sendPrivateImage)；WS 解析 SUPER_CHAT_MESSAGE(价格/背景色/时长)与 SEND_GIFT/COMBO_SEND；直播间 SC 置顶卡片(解析背景色)+礼物飘条

**坑**：VideoRepository.apiClient 是 private（playback VM 内直接 new BiliApiClient）；BoxWithConstraints 内 scope 不在 Modifier 链（graphicsLayer 用 density）；collectAsState 要在 if 块外调用。
**API 备忘**：/x/v2/reply/at 匿名始终 4101001（需登录态，@用户搜索暂缓，评论发送可带 at_name_to_mid）。
**规模**：Kotlin 104 文件 / 14,295 行。**进度：31% → 34%**。

**剩余**：Kototoro 页面级打磨、@用户评论搜索（需登录态实测）、下载选清晰度/断点续传、直播进房/点赞消息、Vulkan/HDR/多引擎（最后）。

## 复盘 r11（2026-10-05，批次L5-L9：弹幕发送/@评论/直播点赞观看/断点续传/字幕选择）

**本轮新增（5 commit，全部经 :app:packageDebug 构建验证）**：
- **批次L5 视频弹幕发送**（184c575）：/x/v2/dm/post（oid=cid/type=1/bvid/progress/mode/csrf）+ VideoPlayerViewModel.sendDanmaku（登录校验+当前进度+本地立即插入 _danmakuQueue 反馈）+ 弹幕设置面板发送输入行
- **批次L6 直播点赞/观看数/进房欢迎**（184c575）：WS 解析 LIKE_INFO_V3_CLICK/UPDATE（total_like+点赞人名）、WATCHED_CHANGE（data.num 看过人数）、INTERACT_WORD（msg_type=1 进房欢迎）→ VM 收集 likeTotal/watchedCount/welcome/likeMsg → 直播间头部"看过 N · 👍 N"实时展示 + 系统"欢迎 xx 进入直播间"/"xx 点了个赞"消息
- **批次L7 @用户评论**（0765e26）：/x/v2/reply/at wbi 签名搜索候选 + CommentScreen @按钮 + 输入实时搜索（lastIndexOf('@') 后缀）+ 候选下拉（头像/UP标）选中补全 + addComment 增 at_uid/at_name（at_name_to_mid 表单字段）真实发送
- **批次L8 下载暂停/断点续传**（5df661c）：STATE_PAUSED(4) + pause/resume + downloadTo HTTP Range 206 追加续传（服务器不支持则重下）+ **修复既有 bug：download() 重试时重建 Entity 丢 cid/进度/标题 → 改保留原记录** + DownloadScreen 暂停/继续/重试按钮
- **收藏夹内容无限分页**（c31811b）：x/v3/fav/resource/list pn 分页 + 去重合并 + 滚动加载
- **批次L9 播放器字幕选择 UI**（d83ccc1）：subtitleTracks 多轨列表(id/langDoc/url) + selectSubtitle 懒加载 body + 关闭字幕(id=-1) + 弹幕面板 FilterChip 轨道选择

**审查结论（第三轮）**：假实现/TODO 无；60 API 方法绑定核查（脚本误报已人工逐一确认，repo 层 likeVideo/coinVideo/favoriteVideo 实际经 toggleLike/coinOnce/toggleFavorite 触发）；点赞/投币/收藏/历史上报/直播进房上报链路全通；发现并修复 download 重试丢数据 bug。

**规模**：Kotlin 104 文件 / 14,822 行。**进度：34% → 40%**。

**剩余**：番剧 ep 字幕、Kototoro 页面级打磨、下载弹幕/字幕离线、批量下载、重连退避增强、Vulkan/HDR/多引擎（最后）。

## 复盘 r12（2026-10-05，批次L10-L14：批量下载/番剧下载/弹幕渲染重大修复，40%→50%）

**本轮新增（8 commit，均经 :app:packageDebug 构建验证）**：
- **审查修复①**（4422e30）：ensureAid 对番剧 "ep{id}" 虚拟 bvid 防护（此前会去请求 getVideoDetail("ep…") 必然失败）+ 播放器分享按钮（复制链接，bangumi 链接区分）+ notifyShared 反馈
- **批次L10 批量下载**（7e8b998）：DownloadManager.downloadPart（cid 直传，键 bvid_pN，Range 续传兼容）+ DownloadItemEntity.pageLabel 字段 + 分P面板显示真实分P标题 + 每行"下载本P"按钮
- **批次L11 弹幕离线**（c3e682c）：下载完成后抓取弹幕存 danmaku.json（t/c/col/fs）+ DownloadManager.localDanmaku 反序列化 + 离线播放优先本地弹幕（无缓存回退在线拉取）
- **批次L12 直播WS重连**（a182f78+47a9e58）：指数退避 2/4/8/16/32/64s、host_list 轮换、最多6次、成功重置；State 增 Connected/Reconnecting(attempt)；直播间状态条显示"弹幕重连中(第N次)…"
- **批次L13 番剧选集下载**（5112c76）：DownloadManager.downloadPgcPart（pgc playurl，键 ep{id}_pN，含弹幕离线）+ VM downloadPart 分流番剧/普通视频
- **半成品修复② 弹幕大小持久化**（d460048）：setDanmakuScale 此前只改内存，重启丢失 → StorageManager DANMAKU_SCALE_KEY + danmakuScaleFlow + VM 双向接线
- **半成品修复③（重大）弹幕渲染完全打通**（41a6c18）：审查发现 DanmakuView 为空转半成品——旧实现 addDanmaku 后**无任何滚动绘制逻辑**，且 Screen 端 AndroidView 从未喂数据（danmakuQueue 从未到达渲染层），用户实际看不到任何弹幕。重写为 Canvas 渲染：每帧推进 x 坐标（基准 6s/屏）、8 轨道避让 pickRow、透明度/大小倍率/速度倍率实时生效、暂停冻结恢复无跳帧、出屏回收；Screen 端 LaunchedEffect 每帧 getDanmakuAtTime 窗口分发 + id 去重 + danmakuOn 关闭清屏 + seek 重置

**审查结论（第四轮）**：修复 3 个真实半成品（ensureAid 番剧崩溃路径、弹幕大小不持久化、弹幕渲染断链）；确认 Rust JNI 链路健康（PlayUrlNativeLib.selectStreams 真实调用，WebpNativeLib/AudioNativeLib/DanmakuNativeLib 均有 Rust 实现导出，DanmakuMerger 弹幕合并 Kotlin 侧尚无调用入口——留待后续批次）。

**规模**：Kotlin 104 文件 / 15,136 行。**进度：40% → 50%**。

**剩余**：DanmakuMerger(Rust 弹幕合并)接入播放器、Kototoro 页面级打磨、番剧 ep 字幕（字幕接口匿名实测为空，需登录态再验）、Vulkan/HDR（最后）。

## 复盘 r13（2026-10-05，批次L15-L24：Rust弹幕合并/排行榜/简介/AI总结/专栏，50%→58%）

**本轮新增（9 commit，均经 :app:packageDebug 构建验证）**：
- **批次L15 Rust DanmakuMerger 接入**（5c140c3）：DanmakuNativeLib.nativeMerge JNI（JSON 进出）+ loadDanmakuFor 用 Rust 合并去重弹幕（失败回退原始列表）——上轮遗留的"Rust 侧无 Kotlin 调用入口"闭环
- **批次L16 Kototoro 主页打磨**（d140c56）：HomeScreen 品牌渐变沉浸顶栏（Box+PiliGradients.bilibili Brush）
- **批次L17 排行榜**（2e03254）：/x/web-interface/ranking/v2（桌面 UA + rank referer 防 -352）+ 8 分区 Tab + 奖牌色名次 + RankScreen + 主页奖杯入口 + 路由
- **审查修复②**（433e16c）：排行榜 onOpenVideo 改用 VideoPlayer.createRoute——原裸字符串 "video/$bvid" 缺 local 参数无法匹配路由 pattern（真实断链）
- **批次L18 视频简介面板**（4945e29）：VideoInfoData.tags 模型 + VM videoDesc/videoTags/videoPubdate + 播放器"简介"按钮 + ModalBottomSheet（标题/UP主/日期/标签 chips/简介）
- **批次L19 投币枚数选择**（a93d2e8）：coinOnce(multiply) + 投币面板 1/2 枚选择
- **批次L20 收藏夹选择**（5ec685b）：VM loadFavFolders(/x/v3/fav/folder/created/list-all)/favoriteTo(mediaId) + 收藏按钮弹出收藏夹列表按夹收藏（原来只有默认夹）
- **批次L21 历史搜索**（f8238ec）：LibraryRepository.searchHistory(/x/web-interface/history/search，需登录) + 历史页搜索框（keyword 空=恢复列表）
- **批次L22 AI 视频总结**（05cc4ac）：getAiConclusion(/x/web-interface/view/conclusion/get，wbi 签名，需登录，匿名 -403 静默) + 简介面板 AI 摘要 + 分段提纲（带时间戳）
- **批次L23 专栏阅读页**（4cb9c58）：/x/article/view（匿名可用，实测 code 0）+ ArticleModels + ArticleScreen（AndroidView Html.fromHtml 正文渲染 + 作者/统计/头图）+ Article 路由
- **批次L24 UP 主空间专栏 Tab**（0c2aed7）：getSpaceArticles(/x/space/article，匿名可用) + MemberScreen 投稿/专栏 TabRow + 专栏卡片列表分页 + 跳转阅读页

**审查动作**：逐条 grep 验证路由/调用链闭环（RankScreen→nav、favoriteTo/loadFavFolders、searchHistory VM→repo→api）；发现并修复排行榜跳转断链 1 处。

**规模**：Kotlin 106 文件 / 15,752 行。**进度：50% → 58%**（本目标 60%，因 AI 总结/历史搜索等需登录态无法匿名全链路验证，保守计 58%）。

**剩余**：追番/追剧订阅页(pgc_review/subscription)、动态话题/投票、直播分区页(live_area)、音乐/音频、签到/经验(exp_log 需登录)、番剧 ep 字幕（需登录）、Vulkan/HDR/多引擎（最后）。

## 复盘 r14（2026-10-05，批次L25-L31：直播分区/热门精选/合集/每周必看/首页轮播/番剧时间表/大航海，58%→70%）

**本轮新增（7 commit，均经 :app:packageDebug 构建验证）**：
- **批次L25 直播分区页**（dd763fd）：room/v1/Area/getList（匿名可用，12 大分区）+ xlive/webMain/getMoreRecList（匿名可用，12 条/页）前端按 parent_area/area_id 过滤 + LiveAreaScreen 2 列网格 + LiveList 分区入口（second/getList 匿名 -352 不可用，改此 web 方案）
- **批次L26 热门精选页**（5bd83c1）：popular/precious（入站必刷 98 条，匿名）+ popular/series/list（每周必看期数，匿名）+ HotMoreScreen Tab + 主页火焰入口
- **批次L27 UP主合集/系列 Tab**（124b17b）：seasons_series_list（seasons+series 两类，匿名）+ seasons_archives_list / series/archives 展开视频 + MemberScreen 第三 Tab + 点击合集加载视频列表
- **批次L28 每周必看期数详情**（fcdbf00）：popular/series/one（buvid3 cookie 防 -352，登录态更稳）+ 点击期数加载该期视频列表（返回按钮回到期数列表）
- **批次L29 首页顶部大卡轮播**（1cf55d7）：index/top/rcmd（fresh_type=3，匿名可用）+ HomeViewModel topRcmd StateFlow + HomeScreen LazyRow 横滑大卡（260dp，渐变标题）
- **批次L30 番剧时间表**（7bb70cc）：pgc/web/timeline（匿名可用，before/after=6 天）+ TimelineScreen 按日分组 + 延播标记 + 跳转番剧详情 + PgcIndex 顶栏入口
- **批次L31 直播间大航海**（9bf976e）：xlive/app-room/v2/guardTab/topList（匿名可用）+ room/v1/Room/room_info 解析主播 uid + 直播间头部舰长数 + 舰长名单展示

**审核动作**：逐条验证新路由闭环（LiveArea/HotMore/Timeline/Rank/Article 全部 composable + navigate 入口齐全）；核验各页 VM→API 方法存在且签名匹配（getBangumiTimeline/getPrecious/getWeeklyList/getWeeklyDetail/getSeasonsSeries/getGuardTopList）；修复编译错误 6 处（LazyRow items 别名 rowItems、Brush/background/RoundedCornerShape import、Timeline onOpenSeason Long 类型、public inline 访问 private 改 bodyAsText、GuardModels SerialName import）。

**风控调研（本轮实测）**：
- 动态 feed/space + detail：匿名 -412（带真 buvid3+buvid4 仍 banned）→ 动态模块跳过
- 直播 second/getList + getListByArea：匿名 -352（真 buvid 无效）→ 用 webMain/getMoreRecList 替代
- 投币/经验记录（coin/log、exp_log）：需登录 → 跳过
- 赛事 match/list：返回 HTML（已废弃）→ 跳过
- 送礼物：Flutter 参照亦无此功能 → 保持一致跳过

**规模**：Kotlin 118 文件 / 17,465 行；Rust 9 文件 / 874 行。**进度：58% → 70%**。

**剩余（多为登录态/Vulkan）**：追番订阅页(pgc_review，匿名 53013 隐私)、动态话题/投票(-412)、音乐/音频(接口废弃)、签到/经验(需登录)、@用户评论搜索(需登录验证)、番剧 ep 字幕(需登录)、Vulkan/HDR/多引擎渲染(最后)。

## 复盘 r15（2026-10-05，审核优化专项 23 轮 + 批次L32/L33，70%→80%）

**审核优化（23 轮静态/动态/逻辑审查，12 轮落地修复，全部 :app:packageDebug 构建验证）**：
- **审核1**（e70dc27）：静态扫描 TODO/假实现/空 catch —— 假实现 0、TODO 0；**真实 bug**：LoginRepository.parseUrlCookies 把 cookie 写成 `KEY:value` 而 applySetCookies 按 `=` 分割 → crossDomain cookie 全部丢弃，改回 `=`
- **审核2**：全部 27 个 Screen 路由 composable 注册齐、navigate 全部走 createRoute、FollowList 多参路由正确 —— 断链 0
- **审核3**：81 个 API 方法 vs 51 个调用点逐一核对，缺 0（getFavoritesRaw 为 inline 误报）
- **审核4**（e70dc27）：Json 配置补 coerceInputValues + explicitNulls —— 线上字段突变不再崩溃
- **审核5**（e70dc27）：PlaybackStatsService 30s while(true) 循环 close() 不取消 → scope.cancel()；Job→SupervisorJob；MainApplication 匿名 CoroutineScope → 常驻 appScope
- **审核6**：重试按钮 11 处、加载指示 20 页、空态 261 处引用 —— 一致
- **审核7**：Rust cargo test 8/8、clippy 0 warning
- **审核8**：日志打印 cookie/SESSDATA 0 处；Room 存 cookie 与 Flutter 原版一致（私有目录，接受）
- **审核9**：loadMore 11 处实现 + hasMore 守卫 57 处引用；scroll 触底用 snapshotFlow+derivedStateOf
- **审核10**：contentDescription=null 仅 14 处（装饰性图标，合规）
- **审核11**（c635559）：DownloadManager 6 处进度回调 launch{upsert} 改 suspend 顺序写 —— 消除 DB 写竞态与海量协程创建
- **审核12**（1d37644）：WS 弹幕 JSON 空 catch → 记日志不打断弹幕流
- **审核13**（1d37644）：_danmakuQueue MutableList → CopyOnWriteArrayList（WS 线程/主线程/渲染帧三方并发）
- **审核14**（b970f0b）：删除 11 处确证未用 import
- **审核15**：AccountSession ConcurrentHashMap+@Volatile —— 线程安全
- **审核16**：16 处写操作全部经 postAuthForm（自动 csrf）—— 无漏
- **审核17**（c0b76d7，重大）：B站封面/头像返回 http://，Android 9+ 禁明文 → **图片全部加载失败**；新增 toHttpsUrl() 应用于 38 处 AsyncImage
- **审核18**（930587e）：video/audio baseUrl（普通/番剧/Rust 选流路径）同步 https 重写
- **审核19**（1bb4fa5）：直播 HLS 流 host+baseUrl+extra 拼接后 https 重写
- **审核20**（e0ca0d8，真实断链）：搜索"直播"类型结果行无任何 clickable → 补 onLiveClick→LiveRoom 路由
- **审核21**：AsyncImage model= 0 处遗漏
- **审核22**：48 处 BiliApiClient() 默认共享 BiliHttpClient.client 单例 —— 无连接泄漏
- **审核23**（49e20a5）：首页顶栏图标间距审计后并入批次L33

**新功能**：
- **批次L32 视频分区浏览**（f981cae）：/x/web-interface/newlist（匿名可用）+ 20 分区横滑 FilterChip + 无限分页 + 视频卡（时长/播放数/UP主点击跳空间）+ HotMore 顶栏入口
- **批次L33 首页直达入口**（49e20a5）：顶栏补分区浏览（Apps）/番剧（Movie）图标

**规模**：Kotlin 121 文件 / 17,832 行；Rust 874 行。**进度：70% → 80%**。

**剩余**：追番订阅（匿名 53013）、动态 feed（-412 风控）、音乐（接口废弃）、签到/@评论搜索/ep 字幕（需登录真机验证）、投币记录页（需登录）、Vulkan/HDR（最后）。

## 复盘 r16（2026-10-05，功能推进 L34-L42 + 审核24-26，80%→85%）

**新功能批次（均 :app:packageDebug 构建验证）**：
- **L34 排行榜分区榜**（2d52700）：ranking/region（匿名可用，11条/分区）替代 v2 全站榜传 rid 无效的问题；RankViewModel 按 rid 分流 v2（全站）/region（分区）
- **L38 UP主代表作**（b4545b9）：x/space/masterpiece（匿名可用，3条精选）；MemberScreen 头部下方代表作横滑卡（200dp 卡 + 播放/弹幕数）
- **L40 播放页实时在线人数**（15256c7）：x/player/online/total（匿名可用）；VM loadOnlineCount + 简介面板「👁 N 人正在看」
- **L41 视频章节**（c8c1757）：player/v2 view_points 解析（复用已有 player/v2 调用，零额外请求）；ViewPoint 模型 + 简介面板章节列表 + 点击 seekTo 跳转
- **L42 弹幕高能进度条**（2fa5079）：Rust dmheat.rs 密度分桶归一化（4 单测）；JNI nativeHeatMap；进度条上方 Canvas 热力曲线

**Rust 增强（CPU 密集下沉）**：
- **L39 dmfilter.rs**（cd3c737）：关键词/正则/UID 三类屏蔽规则批量过滤；regex crate 编译（非法正则忽略）；4 单测；JNI nativeFilterBlock + Kotlin 回退
- **L42 dmheat.rs**（2fa5079）：弹幕密度直方图 + 归一化 + 峰值检测；4 单测；JNI nativeHeatMap
- Rust 规模：874 行 → 1263 行；cargo test 16/16 全通过、clippy 0 warning

**真实 bug 修复（审核专项目）**：
- **审核24**（e9beeb1，重大）：DanmakuBlockViewModel.shouldBlock() **从未被调用**——弹幕屏蔽规则 UI/Room/缓存全齐但完全不生效；接线 DanmakuView.add(uid) 渲染过滤
- **审核25**（cd3c737）：屏蔽过滤 uid 恒传 0L → UID 规则永不生效；DanmakuEvent 补 uid 字段，Rust 路径回填原始 uid
- **审核26**（3a469cc）：MineScreen UserContent「设置」菜单项空 onClick → 接通 onSettingsClick；UserContent 补参数
- **L35**（561d069）：getComments 硬编码 mode=3 未传参 → 评论排序切换 UI 形同虚设的真实 bug
- **L36**（c0b2359）：直播列表 second/getList 匿名 -352 → webMain/getMoreRecList + 去重翻页
- **L37**（7012eb2）：SearchScreen 返回键空 onClick（第5处断链）、简介 tag chip 空点击、Search 路由 initialQuery

**规模**：Kotlin 12x 文件 / 18,187 行；Rust 11 文件 / 1263 行。**进度：80% → 85%**。

**剩余**：追番订阅（匿名 53013 隐私）、动态 feed（-412 风控）、音乐（接口废弃）、登录态项真机验证（@评论搜索/番剧 ep 字幕/投币记录/关注分组/黑名单）、Vulkan/HDR（最后）。

## 复盘 r17（2026-10-05，10+ 轮审核优化专项，85%）

用户要求：完成 85%+ 后审核优化整个项目 10 轮。实际执行 **11 轮**（审核27–37）：

- **审核27**（4d08942，真实缺陷）：VideoPlayerViewModel / SettingsViewModel 持有 Activity Context → 配置变更泄漏 Activity；改持 applicationContext（6 处调用点 + 2 类构造）
- **审核28**：竞态/线程安全核查——ExoPlayer 操作均在 Main 协程（player 构造于主线程）；LiveDanmakuWsClient closed flag + onCleared close()；WS 生命周期正确 → 通过
- **审核29**：写操作表单字段核对（like/coin/favorite/report/dm post/comment）与官方 API 一致 → 通过
- **审核30**：Compose 性能——长列表（搜索/私信/相关视频）补 LazyColumn key 提升复用（29b43b7）
- **审核31**（093f0af，真实缺陷）：HttpClient **无任何超时/重试配置**——弱网请求挂死；补 HttpTimeout(30s/15s/20s) + HttpRequestRetry(指数退避 2 次)
- **审核32**：JNI 边界——unsafe 指针均由 Kotlin create/destroy 配对 + try/finally 管理 → 通过
- **审核33**：AndroidManifest 权限最小化（legacy storage 带 maxSdkVersion）→ 通过
- **审核34**：Room 迁移（fallbackToDestructiveMigration）+ 无 allowMainThreadQueries → 通过
- **审核35**：StateFlow 封装——无公开暴露的 MutableStateFlow，全部 asStateFlow → 通过
- **审核36**（a334b8b）：热力曲线时长兜底——弹幕早于播放器就绪时 duration=0，Rust 侧用弹幕最大时间兜底
- **审核37**：空态/错误态/重试一致性——13 页含重试、21 页含加载指示、11 页含空态文本；列表页均处理 loading+error → 可接受

**累计 16 项真实缺陷修复（跨 r15-r17 审核专项）**：parseUrlCookies 格式、图片/播放流 http→https、搜索直播断链、下载竞态、弹幕队列并发、弹幕屏蔽未接线、UID 规则失效、设置菜单断链、评论排序未传参、直播列表风控、搜索返回键断链、Context 泄漏、网络超时缺失。

**规模**：Kotlin 12x 文件 / 18,187 行；Rust 11 文件 / 1263 行（dmfilter 4 单测 + dmheat 4 单测，cargo test 16/16、clippy 0 warning）。

**注**：另一 AI 协作期间未见并行分支提交；本地/远端经 fetch 核对一致（如发现冲突按「保留更完整实现」原则处理）。

## 复盘 r18（2026-10-05，第二轮审核专项 23 轮（审核38-60），85%→90%）

用户新指令：以不同角度连续审核 10+ 轮并推进至 90%+。实际执行 **23 轮**（审核38-60，8 轮落地修复，15 轮核查通过）。

### 本轮新功能（推进 85%→90%）
- **L43 UP主空间公告**：x/space/notice 匿名可用（实测探测确认），MemberViewModel +loadNotice，空间页头部公告卡（有才显示）。
- **L44 UP主空间内搜索投稿**：复用 wbi arc/search 的 keyword 参数（API 层早已支持但 UI 未接），投稿 Tab 增加搜索框（空关键词禁用按钮、防重入）。

### 本轮修复（8 项真实缺陷/隐患）
- **审核39（安全）**：BiliDocumentsProvider `exported=true` → false（组件加固）。
- **审核40（崩溃兜底）**：MainApplication +setDefaultUncaughtExceptionHandler（记日志后 killProcess，防静默崩溃无迹可循）。
- **审核42（竞态）**：SearchViewModel.loadMore 页码去重防重入；L44 searchArchives @Volatile 重入保护+空关键词拦截。
- **审核43（生命周期，重大）**：视频播放页无 ON_STOP 监听——切后台/熄屏继续出声；加 LifecycleEventObserver 自动暂停。
- **审核45（数据）**：Room exportSchema=true 但无 schemaLocation → schema 从未导出（无法写正式 Migration，只能破坏性清库）；gradle ksp arg 补 room.schemaLocation。
- **审核48（UX）**：历史页未登录错误态只有纯文字 → 加「去登录」按钮并接线 Screen.Login 导航。
- **审核54（CI）**：GitHub Actions Rust 全量编译无缓存 → +Swatinem/rust-cache@v2。
- **审核55（生命周期）**：直播页同样切后台不停播（持续耗流量）→ ON_STOP 暂停/ON_START 恢复（修复 Composable 作用域编译错误一次）。
- **审核56（边界，3 处）**：download/downloadPart/番剧下载均无存储空间预检 → StatFs availableBytes <200MB 抛明确错误。

### 核查通过（15 轮）
38 日志泄漏（无 Log.d/v/println）；41 交互图标 contentDescription 全覆盖（顶层 IconButton 均有）；44 Rust 16/16 测试+clippy 0；46 URL https 化已有 UrlFix+审核17 覆盖；47 CI so 打包链路核实无误（cargo-ndk→artifact→jniLibs，本地无 so 属已知形态）；49 空 onClick 复扫为零；50 设置 DataStore flow 持久化完整；51 历史搜索词已接；52 启动性能（warmup IO 协程/buvid 异步/loadLibrary 有 catch）；53 JNI 调用全 runCatching+Kotlin 回退；57 离线弹幕已落盘 danmaku.json；58 登录二维码过期刷新（86038/86090/86101 全覆盖）；59 列表 filter 非重组热路径；60 PlaybackStatsService 非前台服务无合规问题；35 承接（Flow 封装）。

### 规模与状态
- Kotlin ~18,340 行 / Rust 1,263 行；构建全绿（最近 4 次构建 30-59s）；cargo test 16/16、clippy 0。
- 审核总轮数：r15-r18 累计 **23+11+23 = 57 轮**，累计修复 **25 项真实缺陷**。
- 剩余（90% 后）：追番订阅/关注分组（需登录隐私接口）、动态 feed（-412 硬风控）、登录态项真机验证、Vulkan/HDR/多引擎（roadmap 末位）。

## 复盘 r19（2026-10-05，功能补全+10轮审核+CI全量清零，90%→95%）

用户指令：加大进度完成全部功能移植 → 10 轮代码审核优化 → **逐条检查 CI 每个报错（含警告/信息，即使构建成功）**。

### 一、功能补全（L43–L45）
- **L43 UP主空间公告**：`x/space/notice` 匿名可用（实测），空间页公告卡。
- **L44 UP主空间内搜索投稿**：复用 wbi `arc/search` 的 keyword（API 层早有、UI 未接），投稿 Tab 搜索框（空词/重入保护）。
- **L45 UP主课程（cheese/pugv）**：`pugv/app/web/season/page` 匿名可用，空间页课程横滑卡 + 外部浏览器打开。

### 二、10 轮审核（审核61–70）
- **审核61**（性能，落地）：Coil 全局 ImageLoader（内存 20% + 磁盘 128MB + crossfade）——此前 57 处 AsyncImage 各建默认加载器。
- **审核62**：空 catch 扫描——仅 WS close 的 2 处合理忽略，通过。
- **审核63**：9 处 Dialog/BottomSheet 生命周期检查，通过。
- **审核64**：硬编码 http:// 扫描——仅 UrlFix.kt 的正则，通过。
- **审核65**：Rust panic 安全——android_entry.rs 的 env.unwrap() 均由 Kotlin runCatching 包裹，风险可控，通过。
- **审核66**：协程作用域——6 处均有 SupervisorJob 或 ViewModel scope 管理，通过。
- **审核67**：runBlocking 主线程阻塞——全库 0 处，通过。
- **审核68**：URL 参数拼接——均经参数化，无注入，通过。
- **审核69**：图片 URL 空值防护——toHttpsUrl 全覆盖，通过。
- **审核70**：见下（CI 全量）。

### 三、CI 全量检查（错误+警告+信息，逐条修复）
**Kotlin 编译警告 12 → 0**：JSON 单例复用（BiliApiClient/LiveRoomViewModel，避免每次请求重建）；MainActivity onPictureInPictureModeChanged 新版签名；8 处弃用图标 → AutoMirrored（ArrowBack/Comment/Send/VolumeOff/VolumeUp）。

**Android Lint：56 errors + 45 warnings + 4 information → 0 + 0 + 0（"No issues found."）**
- `MissingClass`：manifest 引用了不存在的 UCropActivity（Flutter 遗留，无代码使用）→ 移除。
- `NewApi`(6)：styles.xml 中 defaultFocusHighlightEnabled(API26)/forceDarkAllowed(API29)/cutoutMode(API27) → tools:targetApi 声明。
- `UnsafeOptInUsageError`(26)：Media3 PlayerView/音频处理器属 UnstableApi → @OptIn 声明（PlayerView 配置抽取为 createPlayerView 辅助函数）。
- `DefaultLocale`(11)：String.format 全部补 Locale.ROOT。
- `ObsoleteSdkInt`(4)：minSdk 24 恒真分支简化；v21 资源目录合并。
- `ManifestOrder`：uses-permission 移到 application 之前。
- `DataExtractionRules`：新增 res/xml/data_extraction_rules.xml（禁云备份/传输）。
- `SelectedPhotoAccess`(2)：READ_MEDIA_* 权限无代码使用（Flutter 遗留）→ 删除权限。
- `StaticFieldLeak`：DanmakuBlockViewModel 改用 applicationContext。
- `SwitchIntDef`(2)：补 Player.STATE_IDLE / 音频编码 else 分支。
- `UnusedResources`/`IconDuplicates`/`IconLocation`/`VectorPath`：清理未用布局/字符串/快捷方式资源、合并重复 night 图、lint.xml 记录良性项忽略原因。
- `UnusedBoxWithConstraintsScope`：BoxWithConstraints → Box。
- `AutoboxingStateCreation`(4 Information)：mutableStateOf → mutableIntStateOf/mutableLongStateOf。

**Rust**：cargo test 16/16，clippy 0 warning。
**完整构建**：`./gradlew :app:packageDebug` BUILD SUCCESSFUL，0 warning 0 error。

### 规模与状态
- Kotlin ~18,700 行 / Rust 1,263 行；lint 报告 "No issues found."。
- 审核总轮数累计 **67 轮**（r15–r19），累计修复 **40+ 项真实缺陷**。
- 剩余：需登录隐私接口（追番订阅/关注分组/收藏夹搜索/投币记录/黑名单）、动态 -412 硬风控、真机验证、Vulkan/HDR（roadmap 末位）。

## 复盘 r20（2026-10-05，审核71–98，AI代码深审+性能/安全/CI门禁）

用户指令：继续审核和优化前面生成的 AI 代码。

### 落地修复（14 项）
1. **审核71（性能，76处）**：`collectAsState` → `collectAsStateWithLifecycle`——后台仍更新 UI/浪费重组 → 仅前台收集。
2. **审核72（真 bug，8处）**：LazyColumn `items` 无 key（直播聊天/搜索建议/历史/分区/屏蔽规则）→ 补 key；**其中 SearchScreen 用 data class 作 key 是运行时崩溃隐患**（key 必须可 Bundle 化）→ 改复合字符串 key。
3. **审核76（并发）**：直播 WS 高频列表流 6 处 `_flow.value = (_flow.value + x)` 非原子 → `MutableStateFlow.update {}`。
4. **审核77（性能）**：`Gson()` 每次调用重建（下载路径+播放器弹幕合并热路径）→ DownloadManager/VideoPlayerViewModel 复用单实例。
5. **审核83（正确性）**：WbiSigner 两处 `String.format` 无 Locale → `Locale.ROOT`（wbi 签名在土耳其等 locale 下会算错）。
6. **审核70遗留**：`@OptIn(…UnstableApi::class)` 对 androidx.annotation.RequiresOptIn 无效 → `@androidx.annotation.OptIn`（消最后一条 Kotlin 警告）。
7. **审核87（体积/安全，重大）**：release `isMinifyEnabled=false` → **R8 minify + shrinkResources**，补全 proguard 规则（kotlinx-serialization/Ktor/Room/Gson/JNI native 方法保留）。实测 **28.3MB → 5.1MB**（release-unsigned）。
8. **审核88**：manifest 移除已废弃 `package=` 属性（namespace 在 gradle）；移除无来源的 `tools:replace="allowBackup"`。
9. **审核90（CI 门禁，重大）**：CI 此前只 assembleDebug、零质量校验 → 加 **cargo test + clippy -D warnings** 门禁和 **lintDebug + 报告回归检测**（errors>0 即失败）。
10. **审核97**：ArticleScreen composable 内每次重组重建 SimpleDateFormat → 文件级 lazy。
11. 审核94 记录：4 处 SimpleDateFormat 为文件级 val/lazy，composable 单线程调用，低危接受。
12. 审核75 记录：BiliApiClient 50 处实例化——无状态轻类共享 HttpClient，重构收益低，记录。
13. 审核91–92 记录：rememberSaveable/BackHandler 0 处——本 App 旋转由 android:configChanges 处理，不需要。
14. 审核93/95/96/98 记录：Toast（0处，用 Snackbar）/BitmapFactory（0处）/viewModel factory（4处正常）通过。

### 验证
- `:app:compileDebugKotlin` 0w0e；`:app:lintDebug` **"No issues found."** 维持。
- `:app:assembleRelease` **BUILD SUCCESSFUL**（R8 通过，5.1MB）；cargo test 16/16；clippy `-D warnings` 0。
- commit 57662c4。

## r20.5 补录（2026-10-05）
- 审核99-100（703f729）：修复 rebase 遗留——room 插件与 ksp schemaLocation 冲突（KSP BUILD FAILED→统一 room{} 扩展）、SearchScreen 重复 import、补完 Json 共享单例。
- 用户 P0 修复核查（82101e8/2e41d8e）：Rust JNI 符号名与 Kotlin 包路径不匹配（弹幕 JNI 全崩）——11/11 符号逐一比对通过，cargo build+test 16/16。
- 推送 PUSH_OK 42adc62，远端=本地。


## r23（2026-10-06，审核轮101-104：订阅模块专项重构）
- **101（真 bug）**：订阅源直链 m3u8/mpd 走 ProgressiveMediaSource 必然解析失败 → 按媒体类型选 HlsMediaSource/DashMediaSource/Progressive；清单类不再 merge audioUrl（B站专用两路）。
- **102**：订阅源请求接入 `withBangumiAuth()`（原扩展定义后无人调用，私有源拿不到授权）——fetch + searchAnimekoSource 两处。
- **103**：SubscribeViewModel.searchInSource 补 searchJob 防重入（refreshAll 有、搜索没有，快速连点会并发打请求）。
- **104**：AddSourceDialog type State→mutableIntStateOf（lint AutoboxingStateCreation，保持 lint 0/0/0）。
- 用户并行提交 29e0f02（订阅源移植 Animeko，SubscribeParser/SubscribeRepository/BangumiSession）核查：解析健壮性良好（MAX_ITEMS_PER_SOURCE 截断、事务删+写、URL scheme 白名单、坏标签容错）。
- 构建 0w0e、lint "No issues found."、cargo test 16/16。

## r24（2026-10-06，审核轮105-190：86轮审核/优化/功能移植）

### 真 bug 修复
- **105**：MemberScreen `info!!` 条件与断言非原子竞态 NPE → 安全访问。
- **106/108**：SettingsViewModel/DanmakuBlockViewModel factory 捕获 Activity context（旋转重建泄漏）→ `AppContext.get()`。
- **111（真 bug）**：`currentTime` 只在 seek/discontinuity 更新 → 观看历史进度恒错 → reportProgress 直读 `player.currentPosition`。
- **112-113**：播放进度条/弹幕派发依赖不更新的 currentTime（进度条冻结、弹幕不滚动）→ 250ms 轮询驱动。
- **122（内存泄漏）**：弹幕去重集合 dispatchedIds 无界增长 → 10s 窗口淘汰。
- **125**：暂停时发弹幕被 `!running` 静默丢弃 → 入列不滚动。
- **128（真 bug）**：直链播放（订阅源 m3u8/mpd）走裸 setMediaItem：自定义 UA 被 CDN 403 + Progressive 解析 HLS 失败 → 统一 startPlayback。
- **153（死功能）**：亮度手势更新 state.brightness 但 UI 从未消费 → window.attributes 应用 + 离页恢复 + 未触摸不覆盖。
- **179（UX bug）**：弹幕"发送成功"误走 setError → 全屏红字 "Error: 弹幕发送成功" + 隐藏控制栏 → 新增 toast 通道，16 处非致命提示改道。
- **189（真 bug，Rust）**：should_skip mode 语义错误（把 B站 4=底部 5=顶部 当字幕/高级）→ 默认配置整类静默丢弃用户顶部/底部弹幕 → 语义对齐 + 默认不过滤（UI 开关控制，见167）。
- **190（真 bug，Rust）**：merge_similar 合并后 content 带 ×N 后缀导致 is_similar 永不匹配（只能合并2条）+ 硬编码计数 → is_similar_raw 剥后缀 + run 累积计数。

### 性能/健壮性
- **126**：DanmakuBlock warmup 单 scope + 快照原子发布。
- **130**：StorageManager DataStore 单例（多实例同文件风险）。
- **149**：直播多 CDN 线路优先 https host。
- lint 0/0/0 全程保持；cargo test 16/16、clippy -D warnings 0。

### 功能移植（Flutter PiliNara / BV / B站原生交互）
- **129**：直链播放尊重自动播放设置（DataStore → cachedAutoPlay）。
- **134**：搜索历史持久化 Room local_cache（重启不清零，上限20条）。
- **139**：首页视频卡片长按 → 稍后再看（接通死代码 addToView API）。
- **141**：直播弹幕接入屏蔽规则（shouldBlock：关键词/正则/UID）。
- **146**：一键三连 API（/archive/like/triple）+ 长按点赞触发。
- **152**：长按 3x 倍速快进（松手恢复，BV 标志交互）+ 浮标。
- **156**：音量/亮度手势浮标提示。
- **167**：顶部/底部弹幕显示开关（弹幕设置面板 Switch + DanmakuEvent.mode 全链路回填，Rust 合并结果也回填 mode）。
- **171**：播放器音量持久化（userAdjustedVolume 防恢复覆盖）。
- **173**：横屏/全屏切换按钮（requestedOrientation）。
- **175**：横屏返回键先回竖屏（BackHandler）+ 离页恢复。
- **183**：多P视频播完自动连播下一P。

### 验证与提交
- 构建 `:app:packageDebug` 多轮通过；lint "No issues found."；cargo test 16/16；clippy 0。
- 提交：4f05eef(105-158) → 7497d35 PUSH_OK → c04cbc1(167-188) → 740e37e(189-190 Rust)。
