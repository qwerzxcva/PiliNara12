package com.example.piliai.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout

object SourceHttpClient {
    val client = HttpClient(OkHttp) {
        expectSuccess = true
        // OkHttp owns redirects so the interceptor can inspect every network hop.
        followRedirects = false
        engine {
            config {
                followRedirects(true)
                followSslRedirects(false)
                addNetworkInterceptor { chain ->
                    val original = chain.call().request().url
                    val request = chain.request()
                    val destination = request.url
                    val sameOrigin = original.scheme == destination.scheme &&
                        original.host == destination.host &&
                        original.port == destination.port
                    val outbound = if (sameOrigin) {
                        request
                    } else {
                        request.newBuilder()
                            .removeHeader("Cookie")
                            .removeHeader("Authorization")
                            .removeHeader("Proxy-Authorization")
                            .build()
                    }
                    chain.proceed(outbound)
                }
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 20_000
        }
    }
}
