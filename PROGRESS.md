# PiliNara12 feat/arm64-rust-build 进度记录

## 目标
创建分支 feat/arm64-rust-build，将 CI 改为仅构建 ARMv8 (aarch64) APK（Kotlin + Rust 双语言），合并上游 PR 分支，最终全流程通过 CI。

## 进度

### ✅ 已完成
1. **分支创建**：从 main 创建 feat/arm64-rust-build，已推送到远程
2. **Rust CI 构建成功**：Job 1 (Build Rust native library) 已通过
3. **CI Workflow**：`.github/workflows/build-arm64-apk.yml` 已配置
4. **Gradle 配置**：
   - Gradle 9.3.1 已配置到 gradle-wrapper.properties
   - gradlew wrapper script 已添加
   - compileSdk = 34, targetSdk = 34
   - NDK 版本硬编码为 "29.0.14206865"
   - Flutter plugin loader 已注释

### ❌ 当前阻塞
**Kotlin 编译失败**：`MainActivity.kt` 引用了 Flutter 生成的代码：
- `io.flutter.embedding.android.FlutterActivity`
- `com.ryanheise.audioservice.AudioServiceActivity`
- `io.flutter.plugins.GeneratedPluginRegistrant`

这些类在移除 Flutter gradle plugin 后不再可用。

**原因分析**：原项目是 Flutter 项目，MainActivity 继承自 FlutterActivity。CI 环境中缺少完整的 Flutter SDK（只有 wanxiang 包装脚本，没有实际的 Flutter SDK），无法运行 `flutter pub get` 来生成插件代码。

### 🔧 待解决方案
需要选择以下方案之一：
1. **方案 A**：在 CI 中安装完整 Flutter SDK 并运行 `flutter pub get`，保留 Flutter plugin
2. **方案 B**：修改 MainActivity.kt 使其不依赖 FlutterActivity，改为普通 Android Activity
3. **方案 C**：使用 wanxiang-build 工具链的 Flutter 支持

### 上游分支列表（已 fetch）
- origin/feat/animeko-features
- origin/feat/bilibro-freerate
- origin/feat/bottom-nav-history
- origin/feat/comment-danmaku-search
- origin/feat/danmaku-fps-decouple
- origin/feat/kotlin-rust-arm64（已存在，之前 AI 创建的）
- origin/feat/kotlin-rust-build
- origin/feat/native-only
- origin/feat/sdr2hdr-support
- origin/feature/exoplayer-integration-new
- origin/feature/exoplayer-max
- origin/feature/hdr-sdr-port-p12
- origin/feature/piliplus-max
- origin/feature/today-watch-new

### 下一步
1. 确定 Kotlin 构建方案（推荐方案 A：在 CI 中安装 Flutter SDK）
2. 修复 build-arm64-apk.yml 以支持 Flutter 构建
3. 触发 CI 验证
4. 合并上游分支
5. 代码审查

## 技术细节
- NDK: r29 (29.0.14206865)
- Gradle: 9.3.1
- AGP: 9.1.0（通过 WanXiang 镜像）
- Kotlin: 2.0.21
- Flutter: 3.47.0（需要完整 SDK）
