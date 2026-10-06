package com.example.pilinara.data.repository

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Animeko 媒体源配置模型 + 网页刮削引擎
 *
 * 配置格式（调研实测，见 docs/animeko-source-format.md）：
 * ```
 * { "exportedMediaSourceDataList": { "mediaSources": [
 *     { "factoryId": "web-selector" | "rss",
 *       "arguments": {
 *         "name": "...", "iconUrl": "...", "description": "...",
 *         "searchConfig": {
 *            "searchUrl": "...{keyword}...",
 *            "subjectFormatId": "a" | "indexed" | "json-path-indexed",
 *            "channelFormatId": "no-channel" | "index-grouped",
 *            "selectorSubjectFormatA": {"selectLists": "css"},
 *            "selectorSubjectFormatIndexed": {"selectNames":"css","selectLinks":"css"},
 *            "selectorChannelFormatNoChannel": {"selectEpisodes":"css",...},
 *            "selectorChannelFormatFlattened": {"selectChannelNames":"css",...},
 *            "matchVideo": {"matchVideoUrl":"regex","enableNestedUrl":bool,
 *                           "matchNestedUrl":"regex","cookies":"...",
 *                           "addHeadersToVideo":{"referer":"","userAgent":""}},
 *            "rawBaseUrl": "...", "requestInterval": 3000
 *         } } } ] } }
 * ```
 *
 * 设计原则（审核）：
 * 1. 全部解析用 runCatching 兜底 —— 单个源配置畸形不应影响其它源。
 * 2. 抓到的 URL 一律经 matchVideoUrl 正则过滤，只保留可播地址。
 * 3. 不伪造结果：刮不到就返回空列表 + 明确错误信息。
 */
object AnimekoScraper {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** 一个完整的媒体源配置 */
    data class SourceConfig(
        val name: String,
        val iconUrl: String,
        val description: String,
        val factoryId: String,
        val cfg: SearchConfig
    ) {
        val isRss: Boolean get() = factoryId.equals("rss", ignoreCase = true)
    }

    data class SearchConfig(
        val searchUrl: String = "",
        val searchUseOnlyFirstWord: Boolean = false,
        val subjectFormatId: String = "",
        val channelFormatId: String = "",
        val selectLists: String = "",           // subjectFormat "a"
        val selectNames: String = "",           // subjectFormat "indexed"
        val selectLinks: String = "",
        val selectEpisodes: String = "",        // channel no-channel
        val selectEpisodeLinks: String = "",
        val selectChannelNames: String = "",    // channel index-grouped
        val matchVideoUrl: String = "",
        val matchNestedUrl: String = "",
        val enableNestedUrl: Boolean = false,
        val cookies: String = "",
        val referer: String = "",
        val userAgent: String = "",
        val rawBaseUrl: String = "",
        val requestIntervalMs: Long = 3000L,
        val matchEpisodeSortFromName: String = "",
        val matchChannelName: String = ""
    )

    /** 作品（番剧）条目 */
    data class Subject(
        val name: String,
        val url: String
    )

    /** 一集 */
    data class Episode(
        val name: String,
        val url: String,
        val channel: String = ""
    )

    /** 解析订阅源配置文件 */
    fun parseConfig(text: String): List<SourceConfig> = runCatching {
        val root = json.parseToJsonElement(text) as? JsonObject ?: return emptyList()
        val arr = root["exportedMediaSourceDataList"]?.jsonObject
            ?.get("mediaSources")?.let { it as? kotlinx.serialization.json.JsonArray }
            ?: return emptyList()

        arr.mapNotNull { el ->
            val o = runCatching { el.jsonObject }.getOrNull() ?: return@mapNotNull null
            val factoryId = o["factoryId"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val a = runCatching { o["arguments"]?.jsonObject }.getOrNull() ?: return@mapNotNull null
            val name = a["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            SourceConfig(
                name = name,
                iconUrl = a["iconUrl"]?.jsonPrimitive?.contentOrNull ?: "",
                description = a["description"]?.jsonPrimitive?.contentOrNull ?: "",
                factoryId = factoryId,
                cfg = parseSearchConfig(a["searchConfig"])
            )
        }
    }.getOrDefault(emptyList())

    private fun parseSearchConfig(sc: kotlinx.serialization.json.JsonElement?): SearchConfig {
        val o = runCatching { sc?.jsonObject }.getOrNull() ?: return SearchConfig()
        fun s(k: String) = o[k]?.jsonPrimitive?.contentOrNull ?: ""

        val a = runCatching { o["selectorSubjectFormatA"]?.jsonObject }.getOrNull()
        val idx = runCatching { o["selectorSubjectFormatIndexed"]?.jsonObject }.getOrNull()
        val noCh = runCatching { o["selectorChannelFormatNoChannel"]?.jsonObject }.getOrNull()
        val flat = runCatching { o["selectorChannelFormatFlattened"]?.jsonObject }.getOrNull()
        val mv = runCatching { o["matchVideo"]?.jsonObject }.getOrNull()
        val hdr = runCatching { mv?.get("addHeadersToVideo")?.jsonObject }.getOrNull()

        return SearchConfig(
            searchUrl = s("searchUrl"),
            searchUseOnlyFirstWord = o["searchUseOnlyFirstWord"]?.jsonPrimitive?.booleanOrNull ?: false,
            subjectFormatId = s("subjectFormatId"),
            channelFormatId = s("channelFormatId"),
            selectLists = a?.get("selectLists")?.jsonPrimitive?.contentOrNull ?: "",
            selectNames = idx?.get("selectNames")?.jsonPrimitive?.contentOrNull ?: "",
            selectLinks = idx?.get("selectLinks")?.jsonPrimitive?.contentOrNull ?: "",
            selectEpisodes = noCh?.get("selectEpisodes")?.jsonPrimitive?.contentOrNull ?: "",
            selectEpisodeLinks = noCh?.get("selectEpisodeLinks")?.jsonPrimitive?.contentOrNull ?: "",
            selectChannelNames = flat?.get("selectChannelNames")?.jsonPrimitive?.contentOrNull
                ?: flat?.get("selectEpisodeLinks")?.jsonPrimitive?.contentOrNull ?: "",
            matchVideoUrl = mv?.get("matchVideoUrl")?.jsonPrimitive?.contentOrNull ?: "",
            matchNestedUrl = mv?.get("matchNestedUrl")?.jsonPrimitive?.contentOrNull ?: "",
            enableNestedUrl = mv?.get("enableNestedUrl")?.jsonPrimitive?.booleanOrNull ?: false,
            cookies = mv?.get("cookies")?.jsonPrimitive?.contentOrNull ?: "",
            referer = hdr?.get("referer")?.jsonPrimitive?.contentOrNull ?: "",
            userAgent = hdr?.get("userAgent")?.jsonPrimitive?.contentOrNull ?: "",
            rawBaseUrl = s("rawBaseUrl"),
            requestIntervalMs = runCatching {
                o["requestInterval"]?.jsonPrimitive?.content?.toLongOrNull()
            }.getOrNull() ?: 3000L,
            matchEpisodeSortFromName = noCh?.get("matchEpisodeSortFromName")?.jsonPrimitive?.contentOrNull ?: "",
            matchChannelName = flat?.get("matchChannelName")?.jsonPrimitive?.contentOrNull ?: ""
        )
    }

    /**
     * 第一步：搜索页 → 作品列表
     *
     * 支持两种（实测分布）：
     * - "a"        (41个)：`selectLists` 选出 `<a>`，文本=作品名，href=链接
     * - "indexed"  (21个)：`selectNames` + `selectLinks` 两组分别取文本与链接
     * - "json-path-indexed" (1个)：JSON 路径，暂不支持（返回空）
     */
    fun parseSubjects(html: String, baseUrl: String, cfg: SearchConfig): List<Subject> =
        runCatching {
            val doc = org.jsoup.Jsoup.parse(html, baseUrl)
            when (cfg.subjectFormatId) {
                "a" -> {
                    if (cfg.selectLists.isBlank()) return emptyList()
                    doc.select(cfg.selectLists).mapNotNull { el ->
                        val name = el.text().trim()
                        val url = absUrl(el, "href", baseUrl)
                        if (name.isBlank() || url.isBlank()) null else Subject(name, url)
                    }.distinctBy { it.url }
                }
                "indexed" -> {
                    if (cfg.selectNames.isBlank() || cfg.selectLinks.isBlank()) return emptyList()
                    val names = doc.select(cfg.selectNames).map { it.text().trim() }
                    val links = doc.select(cfg.selectLinks).mapNotNull {
                        absUrl(it, "href", baseUrl).ifBlank { null }
                    }
                    names.zip(links) { n, l -> if (n.isBlank()) null else Subject(n, l) }
                        .filterNotNull().distinctBy { it.url }
                }
                else -> emptyList() // json-path-indexed 暂不支持
            }
        }.getOrDefault(emptyList())

    /**
     * 第二步：作品页 → 剧集列表
     *
     * - "no-channel"    (4个)：`selectEpisodes` 直接选出一集一个元素
     * - "index-grouped"(59个)：`selectChannelNames` 选线路/分组，`selectEpisodes` 选集
     */
    fun parseEpisodes(html: String, baseUrl: String, cfg: SearchConfig): List<Episode> =
        runCatching {
            val doc = org.jsoup.Jsoup.parse(html, baseUrl)
            val out = mutableListOf<Episode>()
            when (cfg.channelFormatId) {
                "no-channel" -> {
                    if (cfg.selectEpisodes.isBlank()) return emptyList()
                    doc.select(cfg.selectEpisodes).forEach { el ->
                        val name = el.text().trim().ifBlank { el.attr("title") }
                        val url = absUrl(el, "href", baseUrl)
                            .ifBlank { absUrl(el, "data-href", baseUrl) }
                        if (name.isNotBlank() && url.isNotBlank()) out.add(Episode(name, url))
                    }
                }
                "index-grouped" -> {
                    if (cfg.selectEpisodes.isBlank()) return emptyList()
                    // 分组（线路）名：可选，取不到就不分组
                    val channels = if (cfg.selectChannelNames.isNotBlank())
                        doc.select(cfg.selectChannelNames).map { it.text().trim() } else emptyList()
                    val eps = doc.select(cfg.selectEpisodes)
                    eps.forEach { el ->
                        val name = el.text().trim().ifBlank { el.attr("title") }
                        val url = absUrl(el, "href", baseUrl)
                            .ifBlank { absUrl(el, "data-href", baseUrl) }
                        if (name.isNotBlank() && url.isNotBlank()) {
                            out.add(Episode(name, url, channels.firstOrNull() ?: ""))
                        }
                    }
                }
                else -> {
                    // 未指定时退化为 no-channel 行为
                    if (cfg.selectEpisodes.isBlank()) return emptyList()
                    doc.select(cfg.selectEpisodes).forEach { el ->
                        val name = el.text().trim()
                        val url = absUrl(el, "href", baseUrl)
                        if (name.isNotBlank() && url.isNotBlank()) out.add(Episode(name, url))
                    }
                }
            }
            out.distinctBy { it.url }
        }.getOrDefault(emptyList())

    /**
     * 第三步：剧集页 → 视频地址
     *
     * 1. 先用 matchVideoUrl 在本页文本匹配直链；
     * 2. 若本页没有、且启用 nestedUrl（60/63 源如此），则用 matchNestedUrl
     *    在页内找出「嵌套播放页」链接，由调用方跳转后再取直链。
     *
     * 注意：本函数只返回「本页」能直接匹配到的直链；
     * 嵌套跳转由 SubscribeRepository.fetchVideoUrl 循环处理。
     */
    fun extractVideoUrl(html: String, baseUrl: String, cfg: SearchConfig): String =
        runCatching {
            val pattern = cfg.matchVideoUrl
            if (pattern.isBlank()) return ""
            val re = Regex(pattern)
            val m = re.find(html) ?: return ""
            // 注意：不能用 m.groups["v"]（按命名组查找需 API 26，minSdk=24 会被 Lint 拦）。
            // 改为按索引取：优先第 1 个捕获组（多数源把地址放组里），否则取整条匹配。
            val g = m.groupValues
            val raw = g.getOrNull(1)?.takeIf { it.isNotBlank() } ?: g.getOrNull(0) ?: ""
            // url=xxx 形式需解码
            val decoded = if (raw.contains("url=", ignoreCase = true)) {
                raw.substringAfter("url=", "").let {
                    java.net.URLDecoder.decode(it, "UTF-8")
                }
            } else raw
            if (decoded.startsWith("http")) decoded else ""
        }.getOrDefault("")

    /**
     * 从页内提取「嵌套播放页」链接（nestedUrl 二级跳）
     *
     * matchNestedUrl 的语义（调研 + Animeko 源码）：
     * - `$^`：恒不匹配 → 表示「本页没有嵌套，直接用 matchVideoUrl」
     * - 其它正则：在页面里匹配嵌套播放页的 URL 片段（如 `xigua.php`、
     *   `vip`、`m3u8`），命中说明需要跳到该链接再取视频
     *
     * 这里返回：页面里所有符合 matchNestedUrl 的 http(s) 链接（去重）。
     */
    fun extractNestedUrls(html: String, baseUrl: String, cfg: SearchConfig): List<String> =
        runCatching {
            if (!cfg.enableNestedUrl) return emptyList()
            val pat = cfg.matchNestedUrl
            if (pat.isBlank() || pat == "$^") return emptyList()
            val re = Regex(pat)
            val doc = org.jsoup.Jsoup.parse(html, baseUrl)
            // 在文本与所有链接里找匹配
            val found = linkedSetOf<String>()
            // 1) 链接属性里的 http(s) 且匹配
            for (el in doc.select("a[href], iframe[src], script[src]")) {
                val attr = when {
                    el.hasAttr("href") -> "href"
                    el.hasAttr("src") -> "src"
                    else -> continue
                }
                val v = absUrl(el, attr, baseUrl)
                if (v.startsWith("http") && re.containsMatchIn(v)) found.add(v)
            }
            // 2) 页面文本里内嵌的 http 链接（正则抓）
            for (m in re.findAll(html)) {
                val u = m.value
                if (u.startsWith("http")) found.add(u)
            }
            found.toList()
        }.getOrDefault(emptyList())

    /** 从元素取绝对 URL（jsoup 的 absUrl 需要 baseUri；这里显式兜底） */
    private fun absUrl(el: org.jsoup.nodes.Element, attr: String, base: String): String {
        val v = el.attr(attr).trim()
        if (v.isBlank()) return ""
        return runCatching {
            when {
                v.startsWith("http", true) -> v
                v.startsWith("//") -> "https:$v"
                v.startsWith("/") -> base.trimEnd('/') + v
                else -> base.trimEnd('/') + "/" + v
            }
        }.getOrDefault(v)
    }

    /** 构造搜索 URL（替换 {keyword} 占位） */
    fun buildSearchUrl(cfg: SearchConfig, keyword: String): String {
        // 审核：多数源 searchUseOnlyFirstWord=true，只取第一个词（站点搜索框
        // 通常只支持单关键词，多词反而搜不到）。空格等空白符也一并处理。
        val k = if (cfg.searchUseOnlyFirstWord) {
            keyword.trim().split(Regex("\\s+")).firstOrNull()?.takeIf { it.isNotBlank() }
                ?: keyword.trim()
        } else keyword.trim()
        return cfg.searchUrl.replace("{keyword}", java.net.URLEncoder.encode(k, "UTF-8"))
    }

    /** 主页 base（用于拼相对链接） */
    fun baseOf(url: String): String = runCatching {
        val u = java.net.URL(url)
        "${u.protocol}://${u.host}" + if (u.port != -1) ":${u.port}" else ""
    }.getOrDefault(url)
}
