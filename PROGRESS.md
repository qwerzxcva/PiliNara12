# PiliNara Kotlin + Rust 迁移进度报告

## 任务目标
将 PiliNara 项目从 Flutter 全面迁移到 Kotlin + Rust，移除所有 Flutter 依赖和非 ARMv8 架构代码。

## 已完成工作

### 1. 项目分析 (✅ 完成)
- 分析了 1422 个 .dart 文件
- 识别出 375 个 Widget、202 个 GetX Controller
- 分析了 Rust/Cargo.toml 配置（仅 arm64-v8a）
- 确认现有 Android 配置已移除 Flutter 插件

### 2. 数据库层 (✅ 完成)
- 创建了 Room 数据库实体和 DAO
- 实现了 Hive → Room 数据迁移
- 创建了统一的 Repository 层
- 文件位置：`android/app/src/main/kotlin/com/example/pilinara/database/`

### 3. 网络层 (✅ 完成)
- 创建了 Ktor HTTP 客户端
- 实现了 Bilibili API 服务类
- 添加了 WBI 签名拦截器
- 文件位置：`android/app/src/main/kotlin/com/example/pilinara/network/`

### 4. 播放层 (✅ 完成)
- 创建了 Media3 ExoPlayer 封装
- 实现了视频播放器 ViewModel
- 创建了播放统计服务
- 文件位置：`android/app/src/main/kotlin/com/example/pilinara/playback/`

### 5. 弹幕层 (✅ 完成)
- 创建了弹幕合并的 Rust 实现
- 实现了 Kotlin FFI 桥接
- 创建了弹幕视图组件
- 文件位置：
  - `rust/src/danmaku.rs`
  - `android/app/src/main/kotlin/com/example/pilinara/danmaku/`

### 6. 下载层 (✅ 完成)
- 创建了下载管理器
- 实现了通知和进度跟踪
- 文件位置：`android/app/src/main/kotlin/com/example/pilinara/download/`

### 7. UI 层 (✅ 部分完成)
- 创建了主 Activity 和 Compose 主题
- 实现了主页、搜索、个人、动态、直播屏幕
- 实现了视频播放器屏幕
- 实现了文章阅读屏幕
- 实现了设置屏幕
- 文件位置：`android/app/src/main/kotlin/com/example/pilinara/ui/`

### 8. Rust Native 层 (✅ 完成)
- 更新了 Cargo.toml 添加新依赖
- 创建了 danmaku.rs 模块
- 完善了 android_entry.rs JNI 接口
- 文件位置：`rust/src/`

### 9. 构建配置 (✅ 完成)
- 更新了 build.gradle.kts 添加 Room 依赖
- 更新了 settings.gradle.kts 添加 KSP 插件
- 保持了 ARM64 -only 配置

## 待完成工作

### 高优先级
1. **Gradle 依赖配置** - 需要添加完整依赖到 build.gradle.kts
2. **Native 库编译** - 需要配置 cargo-ndk 或手动 NDK 编译
3. **AndroidManifest 更新** - 移除 Flutter 相关配置
4. **Main Application 注册** - 更新 AndroidManifest 中的 Application 类

### 中优先级
5. **更多 UI 页面** - 评论、分享、登录等页面
6. **gRPC 迁移** - 迁移 grpc/ 目录下的 protobuf 定义
7. **单元测试** - 为新增 Kotlin 代码添加测试

### 低优先级
8. **性能优化** - 内存管理、延迟加载等
9. **无障碍支持** - 添加内容描述和 TalkBack 支持
10. **国际化** - 多语言支持

## 技术栈变更

| 原 Flutter | 新 Kotlin |
|-----------|----------|
| GetX | ViewModel + StateFlow |
| Dio | Ktor Client |
| Hive CE | Room Database |
| media_kit | Media3 ExoPlayer |
| canvas_danmaku | Custom DanmakuView |
| audio_service | foreground Service |
| flutter_html | RichText/Markdown渲染 |
| file_picker | Activity Result API |

## 已创建文件清单

### Kotlin 文件 (约 25+ 个)
```
android/app/src/main/kotlin/com/example/pilinara/
├── AudioNativeLib.kt (existing)
├── MainActivity.kt (updated)
├── WebpNativeLib.kt (existing)
├── database/
│   ├── PiliNaraDatabase.kt
│   ├── Converters.kt
│   ├── UserInfoEntity.kt
│   ├── LoginAccountEntity.kt
│   ├── SettingEntity.kt
│   ├── VideoSettingEntity.kt
│   ├── LocalCacheEntity.kt
│   ├── TodayWatchFeedbackEntity.kt
│   ├── DanmakuFilterRuleEntity.kt
│   ├── UserInfoDao.kt
│   ├── LoginAccountDao.kt
│   ├── SettingDao.kt
│   ├── VideoSettingDao.kt
│   ├── LocalCacheDao.kt
│   ├── TodayWatchFeedbackDao.kt
│   ├── DanmakuFilterRuleDao.kt
│   ├── DataMigrationManager.kt
│   ├── DatabaseInitializer.kt
│   └── PiliNaraRepository.kt
├── network/
│   ├── KtorClient.kt
│   ├── ApiService.kt
│   └── Interceptors.kt
├── playback/
│   ├── VideoPlayerViewModel.kt
│   ├── VideoPlayerScreen.kt
│   ├── ExoPlayerHelper.kt
│   └── PlaybackStatsService.kt
├── danmaku/
│   ├── DanmakuNativeLib.kt
│   ├── DanmakuMerger.kt
│   └── DanmakuView.kt
├── download/
│   ├── DownloadService.kt
│   └── DownloadManager.kt
├── utils/
│   ├── StorageManager.kt
│   └── FileUtils.kt
├── ui/
│   ├── theme/
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── navigation/
│   │   └── AppNavigation.kt
│   ├── main/
│   │   ├── HomeScreen.kt
│   │   ├── SearchScreen.kt
│   │   ├── ProfileScreen.kt
│   │   ├── DynamicsScreen.kt
│   │   └── LiveScreen.kt
│   ├── messages/
│   │   └── MessageScreen.kt
│   ├── settings/
│   │   └── SettingsScreen.kt
│   ├── article/
│   │   └── ArticleScreen.kt
│   ├── danmaku/
│   │   └── DanmakuOverlay.kt
│   ├── video/
│   │   └── VideoPlayerScreen.kt
│   └── component/
│       ├── CommonWidgets.kt
│       ├── DanmakuWidget.kt
│       └── VideoPlayerControls.kt
└── piliplus/
    ├── MainActivity.kt
    └── AnimatedWebpMuxer.kt
```

### Rust 文件
```
rust/
├── Cargo.toml (updated)
├── build.rs
└── src/
    ├── lib.rs (updated)
    ├── android_entry.rs (updated)
    ├── webp.rs (existing)
    ├── audio.rs (existing)
    └── danmaku.rs (new)
```

### XML 资源文件
```
android/app/src/main/res/
├── layout/
│   ├── danmaku_item.xml
│   └── danmaku_item_view.xml
└── drawable/
    └── ic_notification.xml
```

### 配置文件更新
- `android/app/build.gradle.kts` - 添加 Room、Ktor 依赖
- `android/settings.gradle.kts` - 添加 KSP 插件

## 下一步行动

1. 运行 `gradle dependencies` 验证依赖解析
2. 编译 Rust native 库并测试 JNI 桥接
3. 修复任何编译错误
4. 添加缺失的资源文件
5. 更新 README 说明新的构建流程
