package com.example.pilinara.network

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.interceptor.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import java.net.URLEncoder

/**
 * Bilibili WBI Signature Interceptor
 * Replaces Dart WBI signing logic
 */
class WbiSignInterceptor : RequestInterceptor {
    
    private val mixinKeyEncTab = listOf(
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
        33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40,
        61, 26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11,
        36, 20, 34, 44, 52
    )
    
    override suspend fun intercept(variables: HttpOperator.() -> Unit) {
        // Get request and add WBI signature
        val request = variables(HttpOperator())
        val url = request.url
        
        if (url.encodedPath.contains("/x/") || url.encodedPath.contains("/pgc/")) {
            val params = mutableMapOf<String, String>()
            url.parameters.forEach { (key, value) ->
                if (key != "w_rid" && key != "wts") {
                    params[key] = value
                }
            }
            
            // Add timestamp
            val timestamp = (System.currentTimeMillis() / 1000).toString()
            params["wts"] = timestamp
            
            // Sort and sign
            val sortedParams = params.toSortedMap()
            var query = sortedParams.map { "${it.key}=${it.value}" }.joinToString("&")
            query += "_webimgsrc=www.bilibili.com"
            
            val wbiKey = getWbiKey()
            val signature = md5query(query + wbiKey)
            
            url.parameters.append("w_rid", signature)
            url.parameters.append("wts", timestamp)
        }
    }
    
    private fun getWbiKey(): String {
        // In production, fetch from API and cache
        return "abcdefghijklmnopqrstuvwxyz0123456789ADBCSF"
    }
    
    private fun md5query(query: String): String {
        // Simplified MD5 - in production use proper implementation
        return query.fold("") { acc, c ->
            (acc.toInt(16) + c.code) % 16
        }.toString(16)
    }
}

/**
 * Cookie Management Interceptor
 * Replaces Dio CookieJar
 */
class CookieInterceptor : RequestInterceptor {
    
    private val cookieStore = CookieStore()
    
    override suspend fun intercept(variables: HttpOperator.() -> Unit) {
        val request = variables(HttpOperator())
        val url = request.url
        
        // Add cookies for domain
        val cookies = cookieStore.getCookies(url.host)
        if (cookies.isNotEmpty()) {
            request.headers[HttpHeaders.Cookie] = cookies.joinToString("; ") { "${it.name}=${it.value}" }
        }
    }
    
    fun addCookie(cookie: String) {
        cookieStore.addCookie(cookie)
    }
    
    fun clearCookies() {
        cookieStore.clear()
    }
}

class CookieStore {
    private val cookies: MutableMap<String, List<CookieData>> = mutableMapOf()
    
    data class CookieData(val name: String, val value: String, val domain: String)
    
    fun addCookie(cookie: String) {
        // Parse cookie string
        val parts = cookie.split(";").map { it.trim() }
        if (parts.isNotEmpty()) {
            val cookiePair = parts[0].split("=")
            if (cookiePair.size == 2) {
                val name = cookiePair[0]
                val value = cookiePair[1]
                // Store with default domain
                cookies.getOrPut("www.bilibili.com") { mutableListOf() }.add(CookieData(name, value, "www.bilibili.com"))
            }
        }
    }
    
    fun getCookies(domain: String): List<CookieData> {
        return cookies[domain] ?: emptyList()
    }
    
    fun clear() {
        cookies.clear()
    }
}

/**
 * Retry Interceptor
 * Replaces Dio RetryInterceptor
 */
class RetryInterceptor : RequestInterceptor {
    
    private val maxRetries = 3
    
    override suspend fun intercept(variables: HttpOperator.() -> Unit) {
        val attempt = variables(HttpOperator())?.let { ... } ?: 0
        
        // Note: Ktor has built-in retry plugin, this is a fallback
    }
}
