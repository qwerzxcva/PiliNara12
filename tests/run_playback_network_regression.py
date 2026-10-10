#!/usr/bin/env python3
"""Test the production playback interceptor against controlled loopback servers.

Only synthetic credentials are used. Android lifecycle, HTTPS redirects,
media decoding and GPU-next rendering are not verified.
"""
from pathlib import Path
import os
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
PLAYBACK = ROOT / 'android/app/src/main/kotlin/com/example/piliai/playback'
source = (PLAYBACK / 'VideoPlayerViewModel.kt').read_text(encoding='utf-8')
start = source.index('                    val httpClient = sourceHttpClient.newBuilder()')
end = source.index('                        .build()', start) + len('                        .build()')
production = source[start:end]
configured = os.environ.get('KOTLIN_LIB')
candidates = sorted(Path.home().glob('.gradle/wrapper/dists/gradle-*/*/gradle-*/lib'))
candidates = [p for p in candidates if list(p.glob('kotlin-compiler-embeddable-*.jar'))]
if not configured and not candidates:
    raise RuntimeError('Set KOTLIN_LIB to an installed compiler library directory')
lib = Path(configured) if configured else candidates[-1]
stdlib = next(lib.glob('kotlin-stdlib-*.jar'))
cache = Path.home() / '.gradle/caches/modules-2/files-2.1'


def dependency(group, artifact, version):
    jars = sorted((cache / group / artifact / version).glob('*/*.jar'))
    if not jars:
        raise RuntimeError(f'Missing cached dependency: {group}:{artifact}:{version}')
    return jars[0]


classpath = os.pathsep.join(map(str, [
    stdlib,
    dependency('com.squareup.okhttp3', 'okhttp', '4.12.0'),
    dependency('com.squareup.okio', 'okio-jvm', '3.9.1'),
]))
program = r'''
import com.example.piliai.playback.SourcePlaybackRequest
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.Collections
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

fun main() {
    val observed = Collections.synchronizedList(mutableListOf<Pair<String, Map<String, String>>>())
    val owned = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    val other = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    val ownedUrl = "http://127.0.0.1:${owned.address.port}"
    val otherUrl = "http://127.0.0.1:${other.address.port}"
    fun configure(server: HttpServer, label: String) {
        server.createContext("/") { exchange ->
            try {
                observed.add(label + exchange.requestURI.path to
                    exchange.requestHeaders.entries.associate {
                        it.key.lowercase() to it.value.joinToString("; ")
                    })
                when (exchange.requestURI.path) {
                    "/redirect" -> {
                        exchange.responseHeaders.add("Location", "$otherUrl/segment.ts")
                        exchange.sendResponseHeaders(302, -1)
                    }
                    "/same-origin-redirect" -> {
                        exchange.responseHeaders.add("Location", "$ownedUrl/key")
                        exchange.sendResponseHeaders(302, -1)
                    }
                    else -> {
                        val body = "controlled fixture".toByteArray()
                        exchange.sendResponseHeaders(200, body.size.toLong())
                        exchange.responseBody.use { it.write(body) }
                    }
                }
            } finally {
                exchange.close()
            }
        }
    }
    configure(owned, "owned")
    configure(other, "other")
    val sourceHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(false)
        .build()
    val sourceRequest: SourcePlaybackRequest? = SourcePlaybackRequest(
        "$ownedUrl/manifest.m3u8", 7L, "Controlled resolver",
        "$ownedUrl/episode", "$ownedUrl/episode", "piliAI-regression",
        "session=synthetic-source"
    )
''' + production + r'''
    try {
        owned.start()
        other.start()
        for (path in listOf("/manifest.m3u8", "/segment.ts", "/key", "/same-origin-redirect", "/redirect")) {
            val request = Request.Builder().url(ownedUrl + path)
                .header("Cookie", "unrelated=must-not-survive")
                .header("Authorization", "Bearer synthetic-unrelated")
                .header("Proxy-Authorization", "synthetic-unrelated")
                .build()
            httpClient.newCall(request).execute().use { response ->
                check(response.isSuccessful)
                response.body?.string()
            }
        }
        check(observed.size == 7) { "Expected five initial requests and two redirects" }
        check(observed.any { it.first == "other/segment.ts" })
        for ((label, headers) in observed) {
            check("authorization" !in headers) { "Authorization leaked to $label" }
            check("proxy-authorization" !in headers) { "Proxy authorization leaked to $label" }
            check(headers["user-agent"] == "piliAI-regression")
            check(headers["referer"] == "$ownedUrl/episode")
            if (label.startsWith("owned")) {
                check(headers["cookie"] == "session=synthetic-source")
            } else {
                check("cookie" !in headers) { "Source cookie crossed an origin boundary" }
            }
        }
        println("PASS production interceptor: manifest, segment and key requests use source headers")
        println("PASS production redirects: same-origin cookie retained, cross-port cookie removed")
        println("PASS production interceptor: unrelated cookies and authorization removed")
    } finally {
        owned.stop(0)
        other.stop(0)
        sourceHttpClient.dispatcher.cancelAll()
        sourceHttpClient.connectionPool.evictAll()
        sourceHttpClient.dispatcher.executorService.shutdown()
    }
}
'''
with tempfile.TemporaryDirectory(prefix='piliai-playback-network-') as directory:
    directory = Path(directory)
    test = directory / 'PlaybackNetworkRegression.kt'
    test.write_text(program, encoding='utf-8')
    classes = directory / 'classes'
    subprocess.run([
        'java', '-cp', str(lib / '*'), 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
        '-no-stdlib', '-no-reflect', '-classpath', classpath,
        '-d', str(classes), str(PLAYBACK / 'SourcePlaybackRequest.kt'), str(test)
    ], check=True, timeout=60)
    subprocess.run([
        'java', '--add-modules', 'jdk.httpserver',
        '-cp', str(classes) + os.pathsep + classpath, 'PlaybackNetworkRegressionKt'
    ], check=True, timeout=30)
print('Loopback HTTP verification only; Android lifecycle, HTTPS, decoding and GPU-next remain unverified.')
