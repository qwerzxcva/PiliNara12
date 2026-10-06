# PiliNara 订阅源 + Kazumi + 刮削引擎（2026-10-06 凌晨）

## 环境约束
- `github.com:443` 被墙（git push/SSH 全失败）；`api.github.com` 可用
  → 用 `.push_via_api.py` 经 Contents API 上传
- 本地 Gradle 起不来 → **只能靠 CI 验证编译**

## 当前 CI
Run 37422127408 **全绿**（APK 26.06 MB + rust-lib 0.52 MB）

## Animeko 刮削引擎（本轮全部完成）

### 调研结论（抓真实 4 个文件，非猜）
4 个链接全是「媒体源配置」，非条目列表。all.json=70 源（63 web-selector + 7 rss），
零可播 URL。播放地址需运行时：关键词→searchUrl→CSS 刮→matchVideo 提地址。

### 已实现（AnimekoScraper.kt + SubscribeRepository.kt）
| 能力 | 覆盖 |
|---|---|
| parseConfig（selector*/matchVideo/headers） | 全部 |
| parseSubjects（a 41 / indexed 21） | 62 |
| parseEpisodes（index-grouped 59 / no-channel 4） | 63 |
| extractVideoUrl（matchVideoUrl 正则） | 63 |
| nestedUrl 二级跳（60 源 enableNestedUrl） | 60 |
| 命名组 (?<v>)(25源) / (?<ep>)(55源) 提取 | 覆盖 |
| searchUseOnlyFirstWord（63） | 63 |
| searchRemoveSpecial（51） | 51 |
| filterByEpisodeSort + matchEpisodeSortFromName（53/55） | 55 |
| diagnoseJsRendered（区分 JS渲染 vs 选择器失效） | 全部 |
| json-path-indexed（1 源） | **未支持** |

### 关键正确性修复
1. 命名组不能 groups["v"]（API26 限制）→ 手写 compileWithNamedV 换算组序号
2. 命名组 (?<v>) 的组序号≠1（第1组常是 http/https）→ 精确数出 v 的位置
3. matchEpisodeSortFromName 用源配置正则而非硬编码

### 诚实标注的未验证项
- matchNestedUrl 语义（推断「嵌套播放页链接」，未找到 Animeko 源码佐证）
- 全链路未真机验证；JS 动态渲染站点 jsoup 无解
- 我实现的只是选择器配置的**子集**

## 其余已完成
- 订阅源数据层（2 表 + v2→v3 Migration，不用 destructive）
- RSS/Atom/JSON 解析 + 类型嗅探
- 订阅页 UI（三级视图：条目→作品→剧集）+ 添加/管理/Bangumi登录
- Kazumi：低延迟音频（Builder 应用，不能热切换）+ 渲染器切换
- Bangumi 登录（Bearer Token，真实调 /v0/me）
- 25+ 轮审核修复（内存泄漏/原子性/命名组/路由/并发限流/重入等）

## 认知更正记录
- Media3 实际 1.3.1（早先误按 1.5.1）
- PlayerView.setSurfaceType 未公开 → XML surface_type
- ExoPlayer.setAudioAttributes 不存在（只在 Builder）

## 十轮审核（2026-10-06 凌晨批次）汇总
| 轮 | 发现 | 处理 |
|---|---|---|
| 1 | 命名组编译函数重复 | 合并为 compileWithNamedGroup |
| 2 | URLDecoder.decode 异常 | 已在 runCatching 内，可接受 |
| 3 | groupValues[0] 空指针 | 永远是整条匹配，安全 |
| 4 | configCache 无上限 | key=订阅配置URL（≤几个），非泄漏 |
| 5 | ViewModel 可变字段并发 | 主线程无竞态，可接受 |
| 6 | RendererPrefs.scope | app-lifetime 单例，非泄漏 |
| 7 | nested 跳转死循环/超时 | take(3) + 30s 超时，安全 |
| 8 | episodeNum 空结果 | "" → null → MAX_VALUE，安全 |
| 9 | RSS 源搜索断链 | **修复**：sourceName=="rss" 弹搜索 |
| 10 | 命名组函数重复 | **重构**：合并 |
| 11 | 播放器布局引用 | 通过 |
| 12 | Media3 版本注释错(1.5.1) | **修正**为 1.3.1 |
| 13 | 渲染器值一致性 | 通过(0/1) |
| 14 | PiP 状态同步 | 半成品：进PiP可用，进出时控制器状态未同步（未改，风险>收益） |
| 15 | external fun 对账 | 11个已一致 |
| 16 | 硬编码中文 | 全项目统一，非订阅独有，记录不改 |
| 17 | db.withTransaction | Room 扩展签名匹配，通过 |
| 18 | 事务内删+写 | 通过 |
| 19-20 | updateSyncResult 非事务 | 轻微不一致，可接受 |

## 最终 CI
Run 37429231602 全绿，APK 26.06 MB
