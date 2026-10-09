#!/usr/bin/env python3
"""Exercise the production in-memory playback registry with synthetic requests.

A minimal ViewModel stub permits JVM execution. This verifies registry behavior,
not Android navigation lifecycle delivery, configuration changes or playback.
"""
from pathlib import Path
import os
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
PLAYBACK = ROOT / 'android/app/src/main/kotlin/com/example/piliai/playback'
configured = os.environ.get('KOTLIN_LIB')
candidates = sorted(Path.home().glob(
    '.gradle/wrapper/dists/gradle-8.14*-bin/*/gradle-8.14*/lib'
))
if not configured and not candidates:
    raise RuntimeError('Set KOTLIN_LIB to an installed compiler library directory')
lib = Path(configured) if configured else candidates[-1]
stdlib = next(lib.glob('kotlin-stdlib-*.jar'))

stub = '''package androidx.lifecycle
open class ViewModel {
    protected open fun onCleared() {}
    fun regressionClear() { onCleared() }
}
'''
program = r'''
import com.example.piliai.playback.SourcePlaybackRequest
import com.example.piliai.playback.SourcePlaybackSession
import com.example.piliai.playback.SourcePlaybackEntryOwner
import java.util.UUID

fun fixture(sourceId: Long) = SourcePlaybackRequest(
    videoUrl = "https://source.test/video.m3u8?signature=synthetic",
    sourceId = sourceId,
    sourceName = "Synthetic resolver",
    episodeUrl = "https://source.test/episode/1",
    cookies = "session=synthetic"
)

fun main() {
    val session = SourcePlaybackSession()
    val first = fixture(7L)
    val second = fixture(8L)
    val firstId = session.register(first)
    val secondId = session.register(second)
    check(firstId != secondId)
    UUID.fromString(firstId)
    UUID.fromString(secondId)
    check(session.request(firstId) === first)
    check(session.request(secondId) === second)
    check("synthetic" !in firstId && "source.test" !in firstId)
    check(session.request("unknown") == null)
    println("PASS production session: opaque distinct IDs and correct request identity")

    session.remove(firstId)
    check(session.request(firstId) == null)
    check(session.request(secondId) === second)
    session.remove(firstId)
    session.remove("unknown")
    println("PASS production session: removal is isolated and idempotent")

    val ownedRequest = fixture(9L)
    val ownedId = session.register(ownedRequest)
    val entryOwner = SourcePlaybackEntryOwner(session, ownedId)
    check(session.request(ownedId) === ownedRequest)
    entryOwner.regressionClear()
    check(session.request(ownedId) == null)
    check(session.request(secondId) === second)
    entryOwner.regressionClear()
    check(session.request(secondId) === second)
    println("PASS production entry owner: clearing releases only its own request and is idempotent")

    session.regressionClear()
    check(session.request(firstId) == null)
    check(session.request(secondId) == null)
    println("PASS production session: onCleared releases registered requests")

    val recreated = SourcePlaybackSession()
    check(recreated.request(secondId) == null)
    println("PASS production session: a new session does not restore credentials")
    println("Android lifecycle delivery, system-back cleanup and rotation still require separate verification.")
}
'''

with tempfile.TemporaryDirectory(prefix='piliai-playback-session-') as directory:
    directory = Path(directory)
    viewmodel = directory / 'ViewModel.kt'
    viewmodel.write_text(stub, encoding='utf-8')
    test = directory / 'PlaybackSessionRegression.kt'
    test.write_text(program, encoding='utf-8')
    classes = directory / 'classes'
    subprocess.run([
        'java', '-cp', str(lib / '*'),
        'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
        '-no-stdlib', '-no-reflect', '-classpath', str(stdlib),
        '-d', str(classes), str(viewmodel),
        str(PLAYBACK / 'SourcePlaybackRequest.kt'),
        str(PLAYBACK / 'SourcePlaybackSession.kt'), str(test)
    ], check=True, timeout=60)
    subprocess.run([
        'java', '-cp', str(classes) + os.pathsep + str(stdlib),
        'PlaybackSessionRegressionKt'
    ], check=True, timeout=20)
