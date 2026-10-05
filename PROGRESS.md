# PiliNara 逐行审计（2026-10-05）

CI: Run 37342380440 全绿（Rust/APK/Verify）

## 本轮修复的 P1（真 bug）

### Rust JNI 边界 panic 清零
JNI 导出函数内 panic 会 abort 整个进程，Kotlin runCatching 拦不住。
- WebpNativeLib_finalize: 补 encoder_ptr==0 判空（防 Box::from_raw(0) UB）
  + new_byte_array/set_byte_array_region unwrap → match
- AudioNativeLib_normalize: get_array_length 补 l>0；
  三处 unwrap → match（失败返回空数组/null）
- 核查：danmaku.rs 用 unwrap_or（安全）；playurl.rs 的 unwrap 在 #[cfg(test)] 内
- 验证：cargo clean -p pilinara-native && cargo build --lib 通过，零警告

### 前两轮 P0（JNI 符号）
- 方法名不匹配 x4（create→nativeCreate 等）
- 包路径缺 danmaku x7
- 11 个符号现已逐一对账一致，#[no_mangle] 11/11

## 审计结论（逐项）

| 检查项 | 结果 |
|---|---|
| `!!` 非空断言 | 4 处，均在判空分支内（nav!=null / else / !isNullOrEmpty），安全 |
| runBlocking（主线程卡死） | 0 |
| GlobalScope（泄漏） | 0 |
| collectAsState（非生命周期感知） | 0（已全部 WithLifecycle） |
| ViewModel 持 Context | 3 个，全部用 applicationContext，无泄漏 |
| 主线程网络/DB | 0 |
| 硬编码明文密钥/http | 无；UrlFix 正确 http→https |
| AndroidManifest cleartext | 未开启（Android 9+ 默认禁 http）→ UrlFix 是必需且正确的 |
| 空 onClick | 1 处「关于」（无害，弹窗未接） |

## 发现的"写了但没接线"（P2）
Room 建了 8 张表，但**设置/缓存实际走 DataStore(StorageManager)**，
因此以下 DAO 定义后从未被调用：
- SettingDao / VideoSettingDao / LocalCacheDao / TodayWatchFeedbackDao
- getUserInfo 仅 1 处，saveUserInfo 0 处
实际在用：DownloadItemDao（下载，完整）、LoginAccountDao（登录持久化）、
DanmakuFilterRuleDao（弹幕屏蔽）
→ 属于冗余而非崩溃；删除需同步改 entities+迁移，风险 > 收益，建议保留或后续单独清理。

## 已验证功能完整（非纯 UI）
- DownloadManager：download/pause/resume/cancel/delete/本地播放/本地弹幕 全套
- VideoPlayerViewModel：loadVideo/loadDanmakuFor/loadSubtitles/热力曲线 全套
- HomeRepository：真调 apiClient.popularVideos + 错误处理

## 仍无法验证
无真机/模拟器：播放、弹幕渲染、UI 观感、运行时行为均未实测。
