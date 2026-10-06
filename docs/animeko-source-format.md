# Animeko 订阅源真实格式调研（2026-10-05）

## 结论（抓真实文件核实，非猜测）

用户给的 4 个链接：
- https://raw.githubusercontent.com/MajoSissi/animeko-source/main/dist/all.json （173 KB, 70 个源）
- https://raw.githubusercontent.com/Nier4ever/ani-sub/main/css.json （40 KB, 15 个源）
- https://sub.creamycake.org/v1/css1.json （48 KB, 18 个源）
- https://sub.creamycake.org/v1/bt1.json

**全部是 Animeko「媒体源配置文件」，不是条目列表。**

统一结构：
```json
{
  "exportedMediaSourceDataList": {
    "mediaSources": [
      {
        "factoryId": "web-selector" | "rss",
        "version": 2,
        "arguments": {
          "name": "酱紫社(修复)",
          "description": "直连",
          "iconUrl": "http://...png",
          "searchConfig": { "searchUrl": "...?wd={keyword}", ...选择器... },
          "tier": ...
        }
      }
    ]
  }
}
```

实测统计（all.json）：
- 70 个源，factoryId 只有 `web-selector`(63) 与 `rss`(7)
- arguments 出现过的键仅：name(70) / description(70) / iconUrl(70) / searchConfig(70) / tier(14)
- **没有 matchVideo 字段**、**没有任何可播放 URL**

## 这对实现的含义

1. 这类文件**不能直接产出可播放条目**。播放地址必须运行时两步走：
   a. 用关键词请求 `searchConfig.searchUrl`（其中 `{keyword}` 占位）
   b. 用 CSS 选择器刮页面 → 再按 matchVideo 规则提取视频地址
   → 即需要实现一个 **CSS 选择器引擎 + JS/视频地址提取器**

2. 因此「订阅页显示封面和条目」的正确做法是：
   - 把每个 mediaSource 当作一个**数据源**展示（名称 + iconUrl 封面 + description）
   - 用户点某个源 → 输入关键词搜索 → 走该源的 searchConfig 刮削 → 得到剧集列表

3. `rss` 类型的源（如动漫花园 share.dmhy.org）**可以**直接产出可播条目，
   因为其 searchUrl 指向标准 RSS（`.../rss.xml?keyword={keyword}`），
   RSS 解析是现成的（我已实现）。

## 已决定实现
- 新增 `AnimekoSourceConfig` 解析：`exportedMediaSourceDataList.mediaSources[]`
  → 每个源落成 SubscribeSourceEntity（name/iconUrl/type=RSS 或 WEB_SELECTOR）
- web-selector 源：暂不能刮（需 CSS 引擎），UI 上明确标注"需关键词搜索/暂不支持自动刮削"
- rss 源：可直接用现成 RSS 解析出条目（含磁力/直链）
- 保留原有 RSS/JSON 条目解析，兼容普通订阅源

## 未验证的语义假设（重要）
- `matchNestedUrl` 的语义：我按「匹配嵌套播放页的 URL 特征，本页无直链时跳到该页再取」实现，
  依据是取值如 `^.+(m3u8|vip|xigua\.php).+\?`（xigua.php/vip 是典型播放页文件名）、
  且 `$^`（永不匹配）占 15 个对应「无需嵌套」。
  **此语义未找到 Animeko 源码佐证，属推断，真机/真实抓取验证前不能确定。**
- `searchUseOnlyFirstWord`：按「只取首词」实现（依据 63 源有此字段且多数站点搜索框只支持单关键词）。
