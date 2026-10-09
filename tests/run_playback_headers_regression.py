#!/usr/bin/env python3
"""Compile the production playback-request policy and test synthetic credentials.

No network requests are made. This test does not verify navigation, actual
OkHttp redirects, media decoding, or GPU-next rendering.
"""
from pathlib import Path
import os
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
source = ROOT / 'android/app/src/main/kotlin/com/example/piliai/playback/SourcePlaybackRequest.kt'
program = r'''
import com.example.piliai.playback.SourcePlaybackRequest

fun request(
    video: String = "https://source.test/video.m3u8?signature=synthetic",
    episode: String = "https://source.test/episode/1",
    cookie: String = "session=synthetic",
    referer: String = "https://source.test/episode/1",
    agent: String = "piliAI-test"
) = SourcePlaybackRequest(video, 7L, "Controlled resolver", episode, referer, agent, cookie)

fun main() {
    val playback = request()
    for (url in listOf(
        playback.videoUrl,
        "https://SOURCE.test:443/segment.ts",
        "https://source.test/encryption-key"
    )) {
        check(playback.headersFor(url)["Cookie"] == "session=synthetic")
    }
    for (url in listOf(
        "https://cdn.test/segment.ts",
        "https://source.test:444/segment.ts",
        "http://source.test/segment.ts",
        "https://source.test.attacker.test/segment.ts"
    )) {
        val headers = playback.headersFor(url)
        check("Cookie" !in headers)
        check("Authorization" !in headers)
        check("Proxy-Authorization" !in headers)
        check(headers["User-Agent"] == "piliAI-test")
        check(headers["Referer"] == "https://source.test/episode/1")
    }
    println("PASS production headers: manifests, segments, keys, host, port and scheme boundaries")

    val remoteVideo = request(video = "https://cdn.test/video.mpd")
    check("Cookie" !in remoteVideo.headersFor(remoteVideo.videoUrl))
    check("Cookie" !in request(cookie = "").headersFor(playback.videoUrl))
    for (invalid in listOf(
        "file:///private/video",
        "https://user:password@source.test/video",
        "relative/video",
        "javascript:alert(1)"
    )) {
        check(runCatching { request(video = invalid) }.isFailure)
        check(runCatching { request(episode = invalid) }.isFailure)
        check(runCatching { playback.headersFor(invalid) }.isFailure)
    }
    check(runCatching { request(cookie = "synthetic\r\nInjected: yes") }.isFailure)
    check(runCatching { request(agent = "test\nInjected: yes") }.isFailure)
    check(runCatching { request(referer = "test\rInjected: yes") }.isFailure)
    check(runCatching {
        SourcePlaybackRequest(playback.videoUrl, 0L, "Resolver", playback.episodeUrl)
    }.isFailure)
    check(runCatching {
        SourcePlaybackRequest(playback.videoUrl, 7L, "", playback.episodeUrl)
    }.isFailure)
    println("PASS production request: invalid URLs, embedded credentials, header injection and source identity")

    val description = playback.toString()
    check("session=synthetic" !in description)
    check("signature=synthetic" !in description)
    check(playback.episodeUrl !in description)
    check("headers=redacted" in description)
    println("PASS production diagnostics: cookies and signed URLs remain redacted")
}
'''
configured = os.environ.get('KOTLIN_LIB')
candidates = sorted(Path.home().glob('.gradle/wrapper/dists/gradle-8.14*-bin/*/gradle-8.14*/lib'))
if not configured and not candidates:
    raise RuntimeError('Set KOTLIN_LIB to an installed compiler library directory')
lib = Path(configured) if configured else candidates[-1]
stdlib = next(lib.glob('kotlin-stdlib-*.jar'))
with tempfile.TemporaryDirectory(prefix='piliai-playback-headers-') as directory:
    directory = Path(directory)
    test = directory / 'PlaybackHeadersRegression.kt'
    test.write_text(program, encoding='utf-8')
    classes = directory / 'classes'
    subprocess.run([
        'java', '-cp', str(lib / '*'), 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
        '-no-stdlib', '-no-reflect', '-classpath', str(stdlib),
        '-d', str(classes), str(source), str(test)
    ], check=True, timeout=60)
    subprocess.run([
        'java', '-cp', str(classes) + os.pathsep + str(stdlib),
        'PlaybackHeadersRegressionKt'
    ], check=True, timeout=20)
