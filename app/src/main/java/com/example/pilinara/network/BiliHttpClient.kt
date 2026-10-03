package com.example.pilinara.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

object BiliHttpClient {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        
        defaultRequest {
            url("https://api.bilibili.com")
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
            header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36")
        }
    }
    
    suspend fun <T : Any> get(path: String, block: HttpRequestBuilder.() -> Unit = {}): T = withContext(Dispatchers.IO) {
        client.get(path) {
            block()
        }.body()
    }
    
    suspend fun <T : Any> post(path: String, body: Any = null, block: HttpRequestBuilder.() -> Unit = {}): T = withContext(Dispatchers.IO) {
        client.post(path) {
            if (body != null) {
                setBody(body)
            }
            block()
        }.body()
    }
    
    suspend fun <T : Any> postForm(path: String, form: Map<String, String>): T = withContext(Dispatchers.IO) {
        client.post(path) {
            formParameters {
                form.forEach { (key, value) ->
                    append(key, value)
                }
            }
        }.body()
    }
}
