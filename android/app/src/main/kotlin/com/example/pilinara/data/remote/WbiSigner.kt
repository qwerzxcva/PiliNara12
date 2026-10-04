package com.example.pilinara.data.remote

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.util.TreeMap

/**
 * B站 Wbi 签名（算法参考 bilibili-API-collect §1.8）
 *
 * 流程：
 * 1. GET /x/web-interface/nav 取 wbi_img.img_url/sub_url → img_key, sub_key（每日更替，缓存）
 * 2. mixin_key = 按 MIXIN_KEY_ENC_TAB 置换 (img_key+sub_key) 后截前 32 位
 * 3. 参数 + wts=当前秒级时间戳 → 字典序排序 → 过滤 value 中 !'()* → urlencode
 * 4. w_rid = md5(query + mixin_key)
 */
object WbiSigner {

    private const val NAV_URL = "https://api.bilibili.com/x/web-interface/nav"

    private val MIXIN_KEY_ENC_TAB = intArrayOf(
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
        33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40, 61,
        26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36,
        20, 34, 44, 52
    )

    @Volatile private var imgKey: String? = null
    @Volatile private var subKey: String? = null
    @Volatile private var fetchedAtMs: Long = 0L
    private val mutex = Mutex()

    /** 签名参数：在原有 query 基础上加 wts 和 w_rid */
    suspend fun sign(params: Map<String, String>): Map<String, String> {
        val (img, sub) = getKeys()
        val mixinKey = mixin(img, sub)

        val all = TreeMap<String, String>().apply {
            putAll(params)
            put("wts", (System.currentTimeMillis() / 1000).toString())
        }
        val query = all.entries.joinToString("&") { (k, v) ->
            val clean = v.filter { it !in "!'()*" }
            "${urlEncode(k)}=${urlEncode(clean)}"
        }
        val wRid = md5Hex(query + mixinKey)
        return all + mapOf("w_rid" to wRid)
    }

    private suspend fun getKeys(): Pair<String, String> {
        val cached = cachedKeys()
        if (cached != null) return cached
        mutex.withLock {
            cachedKeys()?.let { return it }
            val json = BiliHttpClient.client.get(NAV_URL) {
                header("Referer", "https://www.bilibili.com")
            }.bodyAsText()
            val obj = org.json.JSONObject(json)
            val wbi = obj.optJSONObject("data")?.optJSONObject("wbi_img")
                ?: error("nav 接口未返回 wbi_img")
            val img = wbi.optString("img_url").substringAfterLast('/').substringBefore('.')
            val sub = wbi.optString("sub_url").substringAfterLast('/').substringBefore('.')
            require(img.isNotEmpty() && sub.isNotEmpty()) { "wbi key 解析失败" }
            imgKey = img
            subKey = sub
            fetchedAtMs = System.currentTimeMillis()
            return img to sub
        }
    }

    private fun cachedKeys(): Pair<String, String>? {
        // 官方每日更替，缓存 4 小时保险
        val ik = imgKey
        val sk = subKey
        return if (ik != null && sk != null && System.currentTimeMillis() - fetchedAtMs < 4 * 3600_000L) {
            ik to sk
        } else null
    }

    private fun mixin(imgKey: String, subKey: String): String {
        val raw = imgKey + subKey
        val sb = StringBuilder(32)
        for (i in MIXIN_KEY_ENC_TAB) {
            if (i < raw.length) sb.append(raw[i])
        }
        return sb.toString().take(32)
    }

    /** B站风格 urlencode：'!'*'() 外全部转义，空格→%20 */
    private fun urlEncode(s: String): String {
        val sb = StringBuilder()
        for (ch in s) {
            when {
                ch.isLetterOrDigit() || ch in "._-~*" -> sb.append(ch)
                else -> sb.append('%').append(String.format("%02X", ch.code))
            }
        }
        return sb.toString()
    }

    private fun md5Hex(s: String): String {
        val d = MessageDigest.getInstance("MD5").digest(s.toByteArray(Charsets.UTF_8))
        return d.joinToString("") { String.format("%02x", it) }
    }
}
