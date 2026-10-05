# PiliNara 审核进度 (2026-10-05)

## CI 状态
- Run 37318565537 = 全绿（Rust success / APK success / Verify success）
- 产物：pilinara-debug 26.98 MB + rust-lib-arm64 545 KB

## 本轮完成

### 1. Kototoro 风格主题改造（Theme.kt + StorageManager.kt）
提取自上游 Kototoro-app/Kototoro 的 colors.xml / themes.xml：
- 补全 MD3 全部 surface 分层容器字段（lowest/low/high/highest/variant/dim/bright + inverse + scrim）
- 三套分层体系：
  - Amoled：bg/surface=#000000，容器 #121212~#303030 梯度
  - Dark：MD3 柔和深色 #16121A 系（含 tertiary fondament）
  - Light：MD3 基线 #FFFBFF
- PiliSemantic 语义色：green #388E3C / red #D32F2F / yellow #FBC02D / warning #E65100 / nsfw #FF8A65,#FFD54F / iosBlue #007AFF
- tertiary 接语义绿，error 接语义红
- 公共 API 全部保留（PinkPrimary/ACCENT_OPTIONS/PiliShapes/PiliGradients/accentFromHex/PiliNaraTheme）→ 未破坏其它文件

### 2. AMOLED 纯黑开关（端到端可用）
- StorageManager：AMOLED_KEY + amoledFlow + setAmoled（DataStore 持久化，默认 false）
- SettingsViewModel：state.amoled + amoledFlow 收集 + setAmoled
- SettingsScreen：外观区新增「AMOLED 纯黑」SettingSwitch
- Theme：isDark && amoled → 走 Amoled 纯黑 scheme

### 3. Rust 编译错误修复（CI 实际报出的）
- E0425: jni::sys 缺 jdouble → 补导入（唯一硬错误）
- unused_imports x2：移除 JPrimitiveArray、HashMap
- non_camel_case_types：jboolean 加 #[allow(...)]
- 本地 cargo check 验证：Finished + 零警告

### 4. 断链修复
- 删除死代码 ui/main/ProfileScreen.kt（206行，grep 零引用；设置按钮空 onClick、isLoggedIn 硬编码 false，仅残留 Flutter 迁移空壳）
- MineScreen 菜单「设置」项 {} → onSettingsClick（此前点击无反应）

## 审核结论（已确认功能是否为空壳）
| 检查项 | 结论 |
|---|---|
| TODO/FIXME/占位 | 无（仅若干注释误命中） |
| 硬编码假数据(mock/fake/example.com) | 无 |
| Repository 是否真调 API | 是，7 个 Repo 均走 Ktor/Wbi |
| 各 Screen 是否加载真实数据 | 有（ProfileScreen 死代码除外，已删） |
| 导航可达性 | 全部 navigate 目标均有 composable |

## 仍待办（下一轮）
1. 手机号/短信登录：LoginRepository 仅有二维码（generateQr/pollQr）。B站短信登录需风控参数（buvid3/gaia/geetest），需调研可行性，不可臆造。
2. 视觉对照：Kototoro 布局风格（卡片/列表密度）尚未系统性套用到各页面，本轮仅做完色彩 token 层。
3. 播放/弹幕/评论核心链路真机验证。
