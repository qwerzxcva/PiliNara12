# PiliNara 审核进度 (2026-10-04 续)

## 项目规模（最新）
- Kotlin: 123 文件 / 17935 行
- Rust: 863 行
- CI: Run 37304098212 = success（APK 已产出）

## 审核发现的问题

### 已确认问题
1. **ProfileScreen.kt 死代码**（206 行，ui/main/）
   - 未被任何地方引用（grep 无引用）
   - `IconButton(onClick = { /* settings */ })` 空实现
   - `isLoggedIn` 硬编码 false → UserHeader/UserStats/UserActions/UserTabs 全是死分支
   - 实际「我的」页用的是 MineScreen（已完整接线设置/登录）
   - 建议：删除该死文件

2. **登录页缺手机号/短信登录**
   - LoginScreen.kt 只有二维码（generateQr/pollQr）
   - LoginApiClient 只有 qrcode/generate + qrcode/poll
   - 用户明确抱怨过「没有手机号登录」（之前误做成微信登录是错误方向）
   - 注意：B站官方短信登录需风控密钥（buvid3/gaia 等），需调研可行性

3. **MineScreen 菜单项断链**（待确认）
   - 第183行 `MenuItemRow(MenuItemData("设置", Icons.Default.Settings, {}))` — 菜单里"设置" onClick 为空 {}
   - 但顶栏 onSettingsClick 已接线 → 菜单项点击无效
   - 需修：菜单"设置"也应跳设置页

### 已验证正常的
- HomeScreen 搜索按钮 + 视频卡片点击 → 已接线（我之前修复，仍在最新代码）
- 导航路由：所有 navigate 目标均有 composable（Bangumi/FollowList 是多行形式，可达）
- DownloadScreen 真接 DownloadManager（observeAll/delete/pause/resume）
- 无 TODO/FIXME、无硬编码 fake/mock 数据
- Repository 层：Home/Video/Search/Login/User/Library/DownloadManager 7 个
