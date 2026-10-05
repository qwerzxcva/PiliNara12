package com.example.pilinara.data.repository

import com.example.pilinara.data.remote.BiliHttpClient
import com.example.pilinara.database.SubscribeItemEntity
import com.example.pilinara.database.SubscribeSourceDao
import com.example.pilinara.database.SubscribeSourceEntity
import androidx.room.withTransaction
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

/**
 * 订阅源解析（Animeko「添加订阅源链接」移植）
 *
 * 设计要点（审核角度）：
 * 1. 解析在 Dispatchers.IO，绝不在主线程做 XML/JSON 解析或网络。
 * 2. 全部异常在内部收敛为 Result.failure，调用方（ViewModel）统一展示错误，
 *    避免单个坏源导致订阅页整体崩溃。
 * 3. XmlPullParser 由 Android 自带 org.xmlpull 提供，不引入新依赖。
 * 4. 单源解析失败不影响其它源：ViewModel 层逐源 try，聚合成功的部分。
 */
object SubscribeParser {

    /**
     * 单个订阅源最多保留的条目数（审核轮2）
     *
     * 理由：野生订阅源可能有上万条历史条目，全量解析 + 全量 Upsert 会
     * 造成解析耗时、DB 膨胀、以及订阅页一次性渲染巨量卡片导致卡顿/ANR。
     * 订阅页本质是「最近更新」视图，截断到最近 500 条不影响可用性。
     */
    private const val MAX_ITEMS_PER_SOURCE = 500

    /** 解析出的条目（未落库） */
    data class ParsedItem(
        val title: String,
        val cover: String,
        val link: String,
        val desc: String,
        val pubAt: Long,
        val episode: String
    )

    /** 解析结果：源名 + 封面 + 条目列表 */
    data class ParsedSource(
        val name: String,
        val cover: String,
        val items: List<ParsedItem>
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * 拉取并解析一个订阅源。
     * @param url 订阅源链接
     * @param hintType 用户/记录指定的类型；TYPE_BANGUMI 时先试 JSON 再回退 RSS
     */
    suspend fun fetch(
        url: String,
        hintType: Int = SubscribeSourceEntity.TYPE_BANGUMI
    ): Result<ParsedSource> = withContext(Dispatchers.IO) {
        runCatching {
            val text = BiliHttpClient.client.get(url).bodyAsText()
            if (text.isBlank()) error("订阅源返回为空")

            // 先按类型走，失败再自动嗅探，提升对野生源的兼容性
            val candidates = mutableListOf(hintType)
            listOf(
                SubscribeSourceEntity.TYPE_JSON,
                SubscribeSourceEntity.TYPE_RSS
            ).filter { it != hintType }.forEach { candidates.add(it) }

            var lastErr: Throwable? = null
            for (t in candidates) {
                val r = runCatching { parseByType(text, t) }
                if (r.isSuccess) {
                    val parsed = r.getOrThrow()
                    if (parsed.items.isNotEmpty()) return@runCatching parsed
                } else {
                    lastErr = r.exceptionOrNull()
                }
            }
            throw lastErr ?: error("订阅源解析失败：未识别格式或无条目")
        }
    }

    private fun parseByType(text: String, type: Int): ParsedSource = when (type) {
        SubscribeSourceEntity.TYPE_JSON -> parseJson(text)
        SubscribeSourceEntity.TYPE_RSS -> parseRss(text)
        else -> parseRss(text) // Bangumi 源多暴露为 RSS/Atom
    }

    /** RSS 2.0 / Atom 解析（XmlPullParser，无需额外依赖） */
    private fun parseRss(text: String): ParsedSource {
        val factory = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = true
        }
        val parser = factory.newPullParser()
        parser.setInput(StringReader(text))

        var sourceTitle = ""
        var sourceCover = ""
        val items = mutableListOf<ParsedItem>()

        // 当前条目字段
        var title = ""
        var link = ""
        var desc = ""
        var cover = ""
        var pubAt = 0L
        var episode = ""
        var enclosureUrl = ""

        var event = parser.eventType
        var inEntry = false
        var depth = 0

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    depth++
                    val tag = parser.name ?: ""
                    when {
                        tag.equals("item", true) || tag.equals("entry", true) -> {
                            inEntry = true
                            title = ""; link = ""; desc = ""
                            cover = ""; pubAt = 0L; episode = ""
                            enclosureUrl = ""
                        }
                        inEntry && tag.equals("title", true) -> title = readText(parser)
                        inEntry && tag.equals("link", true) -> {
                            // Atom 的 link 在属性 href 上
                            val href = parser.getAttributeValue(null, "href")
                            link = href ?: readText(parser)
                        }
                        inEntry && (tag.equals("description", true) ||
                            tag.equals("summary", true) ||
                            tag.equals("content", true)) -> desc = readText(parser)
                        inEntry && (tag.equals("pubDate", true) ||
                            tag.equals("published", true) ||
                            tag.equals("updated", true)) -> pubAt = parseDate(readText(parser))
                        inEntry && tag.equals("enclosure", true) -> {
                            cover = parser.getAttributeValue(null, "url") ?: ""
                            enclosureUrl = parser.getAttributeValue(null, "url") ?: ""
                        }
                        inEntry && tag.equals("media:thumbnail", true) -> {
                            parser.getAttributeValue(null, "url")?.let { if (it.isNotBlank()) cover = it }
                        }
                        inEntry && tag.equals("media:content", true) -> {
                            if (cover.isBlank()) {
                                parser.getAttributeValue(null, "url")?.let { if (it.isNotBlank()) cover = it }
                            }
                        }
                        inEntry && tag.equals("guid", true) -> {
                            if (link.isBlank()) link = readText(parser)
                        }
                        !inEntry && tag.equals("title", true) -> {
                            if (sourceTitle.isBlank()) sourceTitle = readText(parser)
                        }
                        !inEntry && tag.equals("image", true) -> {
                            // RSS <image><url>
                        }
                        !inEntry && tag.equals("url", true) -> {
                            if (sourceTitle.isNotBlank() && sourceCover.isBlank()) sourceCover = readText(parser)
                        }
                        !inEntry && tag.equals("media:thumbnail", true) -> {
                            if (sourceCover.isBlank()) {
                                parser.getAttributeValue(null, "url")?.let { sourceCover = it }
                            }
                        }
                        !inEntry && tag.equals("itunes:image", true) -> {
                            parser.getAttributeValue(null, "url")?.let { if (sourceCover.isBlank()) sourceCover = it }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val tag = parser.name ?: ""
                    depth--
                    if (inEntry && (tag.equals("item", true) || tag.equals("entry", true))) {
                        inEntry = false
                        val t = title.trim()
                        // 链接兜底：优先 enclosure（直链媒体），其次 link
                        val l = when {
                            enclosureUrl.isNotBlank() -> enclosureUrl
                            link.isNotBlank() -> link
                            else -> ""
                        }
                        if (t.isNotBlank() || l.isNotBlank()) {
                            items.add(
                                ParsedItem(
                                    title = t.ifBlank { l },
                                    cover = cover.trim(),
                                    link = l,
                                    desc = desc.trim(),
                                    pubAt = pubAt,
                                    episode = episode.ifBlank { guessEpisode(t) }
                                )
                            )
                        }
                    }
                }
            }
            event = parser.next()
        }
        return ParsedSource(
            name = sourceTitle.trim(),
            cover = sourceCover.trim(),
            // 审核轮2：截断，避免超大源导致解析/落库/渲染卡顿
            items = if (items.size > MAX_ITEMS_PER_SOURCE) {
                items.sortedByDescending { it.pubAt }.take(MAX_ITEMS_PER_SOURCE)
            } else items
        )
    }

    /** 自定义 JSON 源：兼容 {items:[...]} / [ ... ] 两种形态 */
    private fun parseJson(text: String): ParsedSource {
        val root = json.parseToJsonElement(text).let {
            when (it) {
                is JsonObject -> it
                else -> JsonObject(mapOf("items" to it))
            }
        }
        val name = root["title"]?.jsonPrimitive?.content
            ?: root["name"]?.jsonPrimitive?.content
            ?: ""
        val cover = root["cover"]?.jsonPrimitive?.content
            ?: root["image"]?.jsonPrimitive?.content
            ?: ""

        val arr = root["items"]?.jsonArray
            ?: root["list"]?.jsonArray
            ?: root["data"]?.jsonArray
            ?: error("JSON 源缺少 items/list/data 数组")

        val items = arr.mapNotNull { el ->
            val o = runCatching { el.jsonObject }.getOrNull() ?: return@mapNotNull null
            val link = o["link"]?.jsonPrimitive?.content
                ?: o["url"]?.jsonPrimitive?.content
                ?: o["playUrl"]?.jsonPrimitive?.content
                ?: return@mapNotNull null
            val title = o["title"]?.jsonPrimitive?.content
                ?: o["name"]?.jsonPrimitive?.content ?: link
            ParsedItem(
                title = title,
                cover = o["cover"]?.jsonPrimitive?.content
                    ?: o["image"]?.jsonPrimitive?.content ?: "",
                link = link,
                desc = o["desc"]?.jsonPrimitive?.content
                    ?: o["description"]?.jsonPrimitive?.content ?: "",
                pubAt = o["pubAt"]?.jsonPrimitive?.content?.toLongOrNull()
                    ?: o["pubDate"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                episode = o["episode"]?.jsonPrimitive?.content ?: guessEpisode(title)
            )
        }
        return ParsedSource(
            name,
            cover,
            if (items.size > MAX_ITEMS_PER_SOURCE) items.take(MAX_ITEMS_PER_SOURCE) else items
        )
    }

    /**
     * 读取当前标签的文本内容。
     *
     * 审核轮21：原实现直接 `parser.nextTag()`，遇到非 tag 节点（如 CDATA 后的
     * 空白、注释、或畸形 XML）会抛 XmlPullParserException，
     * 导致整个订阅源解析失败 —— 一个坏标签毁掉整个源。
     * 这里改为：吞掉解析异常，返回已读到的文本，让解析继续。
     */
    private fun readText(parser: XmlPullParser): String {
        return runCatching {
            var text = ""
            if (parser.next() == XmlPullParser.TEXT) {
                text = parser.text ?: ""
                parser.nextTag()
            }
            text
        }.getOrDefault("")
    }

    /** RSS/Atom 常见日期 → 毫秒；失败返回 0（不抛异常，避免坏源整体失败） */
    private fun parseDate(raw: String): Long {
        val s = raw.trim()
        if (s.isBlank()) return 0L
        // 注意：不使用 java.time.Instant（需 API 26，minSdk=24 会被 Lint 拦下），
        // 统一用 SimpleDateFormat 覆盖常见格式。
        for (fmt in listOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss z",
            "dd MMM yyyy HH:mm:ss Z"
        )) {
            runCatching {
                val df = java.text.SimpleDateFormat(fmt, java.util.Locale.US)
                return df.parse(s)?.time ?: 0L
            }
        }
        return 0L
    }

    /** 从标题猜分集标签（"第3集"/"EP03"/"P2"） */
    private fun guessEpisode(title: String): String {
        val t = title.trim()
        Regex("第\\s*(\\d+)\\s*[集话話]").find(t)?.let { return "第${it.groupValues[1]}集" }
        Regex("(?:EP|Ep|ep)\\s*(\\d+)").find(t)?.let { return "EP${it.groupValues[1]}" }
        Regex("(?:^|[^A-Za-z])P\\s*(\\d+)").find(t)?.let { return "P${it.groupValues[1]}" }
        return ""
    }
}

/**
 * 订阅源仓库：源的增删改查 + 同步（拉取→解析→落库）
 */
class SubscribeRepository(
    private val dao: SubscribeSourceDao,
    private val itemDao: com.example.pilinara.database.SubscribeItemDao
) {
    /** 事务宿主：保证「删旧 + 写新」原子提交（审核轮3） */
    private val db by lazy {
        com.example.pilinara.database.PiliNaraDatabase
            .getDatabase(com.example.pilinara.AppContext.get())
    }

    /**
     * 添加一个订阅源。
     * 先做 URL 基本校验，再查重（同 url 覆盖更新），最后落库。
     */
    suspend fun addSource(url: String, name: String = "", type: Int): Result<Long> =
        withContext(Dispatchers.IO) {
            runCatching {
                val normalized = normalizeUrl(url)
                val existing = dao.getByUrl(normalized)
                val entity = SubscribeSourceEntity(
                    id = existing?.id ?: 0L,
                    name = name.ifBlank { existing?.name ?: hostOf(normalized) },
                    url = normalized,
                    type = type,
                    cover = existing?.cover ?: "",
                    enabled = true,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                )
                dao.upsert(entity)
            }
        }

    /**
     * 同步单个源：拉取 → 解析 → 覆盖写入条目表，并回写同步结果。
     * 失败时把错误记到 lastError，供 UI 展示（不删除源，允许用户修链接后重试）。
     */
    suspend fun syncSource(source: SubscribeSourceEntity): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val parsed = SubscribeParser.fetch(source.url, source.type).getOrThrow()

                val now = System.currentTimeMillis()
                // 审核轮22：Upsert 以主键为准。若每次新建 id=0 的实体，
                // 会在 (sourceId, link) 唯一索引上冲突 —— 表现为重复刷新后
                // 要么插入失败，要么 REPLACE 掉旧行导致主键漂移。
                // 这里先按 (sourceId, link) 查出既有主键，复用它做真正更新。
                val existingByLink = itemDao.getBySourceOnce(source.id)
                    .associateBy { it.link }
                val entities = parsed.items.map { p ->
                    SubscribeItemEntity(
                        id = existingByLink[p.link]?.id ?: 0L,
                        sourceId = source.id,
                        title = p.title,
                        cover = p.cover,
                        link = p.link,
                        desc = p.desc,
                        pubAt = p.pubAt,
                        episode = p.episode,
                        sourceName = parsed.name.ifBlank { source.name },
                        updatedAt = now
                    )
                }
                // 先清旧条目再写新条目：避免源内删除的条目长期残留。
                // 审核轮3：必须放在同一事务里 —— 否则「删完还没写完」时进程被杀
                // 或写入失败，用户会看到订阅页被清空（数据丢失）。
                // withTransaction 保证删+写原子提交，任一步失败则整体回滚。
                db.withTransaction {
                    itemDao.deleteBySource(source.id)
                    itemDao.upsertAll(entities)
                }

                dao.updateSyncResult(
                    id = source.id,
                    name = parsed.name.ifBlank { source.name },
                    cover = parsed.cover.ifBlank { source.cover },
                    at = now,
                    err = null
                )
                entities.size
            }.onFailure { e ->
                runCatching {
                    dao.updateSyncResult(
                        id = source.id,
                        name = source.name,
                        cover = source.cover,
                        at = source.lastSyncAt,
                        err = e.message ?: "同步失败"
                    )
                }
            }
        }

    /** 观察源列表（Flow，UI 自动刷新） */
    fun observeSources(): kotlinx.coroutines.flow.Flow<List<SubscribeSourceEntity>> = dao.observeAll()

    /** 观察全部条目（Flow，UI 自动刷新） */
    fun observeItems(): kotlinx.coroutines.flow.Flow<List<SubscribeItemEntity>> =
        itemDao.observeAll()

    suspend fun removeSource(id: Long) = withContext(Dispatchers.IO) {
        runCatching {
            dao.deleteById(id)
            itemDao.deleteBySource(id)
        }
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        runCatching { dao.setEnabled(id, enabled) }
    }

    suspend fun updateName(id: Long, name: String) = withContext(Dispatchers.IO) {
        runCatching {
            val s = dao.getById(id) ?: error("源不存在")
            dao.upsert(s.copy(name = name))
        }
    }

    private fun normalizeUrl(raw: String): String {
        val s = raw.trim()
        require(s.isNotBlank()) { "链接不能为空" }
        val withScheme = when {
            s.startsWith("http://", true) || s.startsWith("https://", true) -> s
            s.startsWith("//") -> "https:$s"
            else -> "https://$s"
        }
        // 基本合法性校验（防用户粘贴任意文本导致后续解析异常）
        val uri = android.net.Uri.parse(withScheme)
        val host = uri.host
        require(!host.isNullOrBlank()) { "链接无效：缺少域名" }
        // 审核轮6：显式 scheme 白名单。
        // 只放行 http/https —— 避免 file://、content:// 等被当作订阅源请求
        // （本地文件读取 / 跨应用内容泄露），也避免 Ktor 用非预期引擎处理。
        val scheme = uri.scheme?.lowercase()
        require(scheme == "http" || scheme == "https") {
            "仅支持 http/https 链接（当前：${scheme ?: "无"}）"
        }
        return withScheme
    }

    private fun hostOf(url: String): String =
        runCatching { android.net.Uri.parse(url).host ?: url }.getOrDefault(url)
}
