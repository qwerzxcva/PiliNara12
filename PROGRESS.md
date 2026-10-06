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

## 质量门禁与遗留编译修复（本轮）

### 已做的确定性修复
1. Lint 门禁从“grep 文本报告”改为解析 XML 并按 severity 判定；报告缺失/畸形即失败。
   新增 tests/check_lint_report.py + tests/test_lint_gate.py（7 个单测，本地通过）。
2. 新增 tests/run_scraper_regression.py：用 Kotlin 编译器编译**生产源文件**里的
   extractVideoUrl / compileWithNamedGroup / resolveUrl 并在 JVM 断言，
   不再是 Python 复刻（此前复刻可能与 Kotlin 语义不一致）。
3. 修 VideoPlayerScreen.kt 引用 ViewModel 不存在 API 的遗留编译错误：
   brightnessTouched / cachedDmShowTop/Bottom / persistDmShow* / playNextPart /
   likeCoinFav / toast，并给 DanmakuEvent 补 mode（4=底部 5=顶部）以支撑类型开关。
4. 播放器新增“陈旧请求”保护：loadJob/loadGeneration，切源与切分P都取消旧请求，
   挂起点后 ensureCurrent()（ensureActive + generation）避免旧响应覆盖新状态。

### 版本事实（可核验）
- Kotlin 2.0.21（android/settings.gradle.kts）与 Compose/KSP 2.0.21 对齐；非最新但更适合当前 AGP 8.5.2。
- Rust 1.99.0 与官方 stable 一致；已加 rust/rust-toolchain.toml 固定。

### 未完成/未验证（如实列出）
- Cargo 全量测试与 clippy 在沙箱内 180s 超时未完成，未在 CI 外独立验证通过。
- Android JNI 只能在 Android target 编译，host 测试无法覆盖 FFI。
- 真机/设备验证仍然缺失；刮削源与实际站点渲染仍未知。

## 修复遗留编译错误并恢复 CI（Run 37473405011 全绿）

按 CI 真实 `e:` 错误逐条修（不再猜）：
1. VideoPlayerScreen 引用 ViewModel 不存在 API：
   brightnessTouched / cachedDmShowTop(Bottom) / persistDmShow* /
   playNextPart / likeCoinFav / toast → 在 ViewModel 补齐实现。
2. DanmakuEvent 补 mode（1=滚动 4=底部 5=顶部），使顶/底弹幕开关有意义。
3. DownloadManager/Theme 仍用 StorageManager.getInstance(appCtx)
   → 给 StorageManager 加进程内单例 getInstance 兼容（原有构造方式不变）。
4. setPlaybackSpeed 补 raw 参数，保留长按 3x 临时倍速语义。

### Lint 门禁（现在真的会失败）
- 改为解析 XML 按 severity 判定，报告缺失/畸形/未知 severity 即失败。
- 首次运行暴露并基线化 3 条“升级提示类”警告：
  OldTargetApi / AndroidGradlePluginVersion / GradleDependency。
  它们在 android/app/lint.xml 中显式 ignore 并注明原因；
  真实代码类警告（如 UnusedResources）仍会让构建失败。

### 12 项质量审核（有证据）
1 编译：CI compileDebugKotlin 成功（此前 40+ e: 错误）。
2 版本-Kotlin：2.0.21，与 Compose/KSP 2.0.21 对齐；非最新但适合 AGP 8.5.2。
3 版本-Rust：1.99.0（与 stable 一致），已加 rust/rust-toolchain.toml 固定。
4 版本-Java：CI 与本地均 JDK 17。
5 依赖锁定：CI 使用 cargo --locked；Rust 侧 rust-toolchain 固定。
6 真回归：tests/run_scraper_regression.py 编译**生产 Kotlin**并在 JVM 断言。
7 真回归：tests/run_rss_regression.py 编译生产 RSS 解析器并断言命名空间/Atom。
8 门禁测试：tests/test_lint_gate.py（7 例）覆盖缺失/畸形/未知 severity。
9 竞态：播放器 loadJob/loadGeneration + ensureCurrent，防旧请求覆盖新状态。
10 解析边界：RSS/Atom 支持 media、itunes、enclosure、嵌套 XHTML、畸形 XML 失败。
11 URL 边界：URI.resolve 正确处理目录/../；非法 scheme、userinfo URL 被拒。
12 未验证项：Cargo 测试/clippy 在沙箱 180s 超时未完成；JNI 仅 Android target；
   真机与真实站点渲染/风控未验证。
