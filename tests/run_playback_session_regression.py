#!/usr/bin/env python3
"""Exercise the production playback registry with synthetic requests.

A JVM ViewModel stub verifies registry behavior only, not Android lifecycle,
rotation, navigation, media decoding, or GPU-next rendering.
"""
from pathlib import Path
import os
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
PLAYBACK = ROOT / 'android/app/src/main/kotlin/com/example/piliai/playback'
configured = os.environ.get('KOTLIN_LIB')
candidates = sorted(Path.home().glob('.gradle/wrapper/dists/gradle-*/*/gradle-*/lib'))
candidates = [path for path in candidates if list(path.glob('kotlin-compiler-embeddable-*.jar'))]
if configured:
    compiler = Path(configured)
elif candidates:
    compiler = candidates[-1]
else:
    raise RuntimeError('Set KOTLIN_LIB to an installed Kotlin compiler library directory')
stdlibs = sorted(compiler.glob('kotlin-stdlib-*.jar'))
if not stdlibs:
    raise RuntimeError(f'Kotlin standard library is missing from {compiler}')
stdlib = stdlibs[-1]

STUB = '''package androidx.lifecycle
open class ViewModel {
    protected open fun onCleared() {}
    fun regressionClear() { onCleared() }
}
'''
PROGRAM = r'''
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
    println("PASS production session: opaque IDs preserve distinct request identity")

    session.remove(firstId)
    check(session.request(firstId) == null)
    check(session.request(secondId) === second)
    session.remove(firstId)
    session.remove("unknown")
    println("PASS production session: removal is isolated and idempotent")

    val ownedId = session.register(fixture(9L))
    val owner = SourcePlaybackEntryOwner(session, ownedId)
    check(session.request(ownedId) != null)
    owner.regressionClear()
    check(session.request(ownedId) == null)
    check(session.request(secondId) === second)
    owner.regressionClear()
    check(session.request(secondId) === second)
    println("PASS production entry owner: cleanup releases only its own request")

    session.regressionClear()
    check(session.request(firstId) == null)
    check(session.request(secondId) == null)
    check(SourcePlaybackSession().request(secondId) == null)
    println("PASS production session: clearing releases requests; new sessions do not restore credentials")
}
'''

with tempfile.TemporaryDirectory(prefix='piliai-playback-session-') as directory:
    temporary = Path(directory)
    stub = temporary / 'ViewModel.kt'
    stub.write_text(STUB, encoding='utf-8')
    test = temporary / 'PlaybackSessionRegression.kt'
    test.write_text(PROGRAM, encoding='utf-8')
    classes = temporary / 'classes'
    subprocess.run([
        'java', '-cp', str(compiler / '*'),
        'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
        '-no-stdlib', '-no-reflect', '-classpath', str(stdlib),
        '-d', str(classes), str(stub),
        str(PLAYBACK / 'SourcePlaybackRequest.kt'),
        str(PLAYBACK / 'SourcePlaybackSession.kt'), str(test)
    ], check=True, timeout=60)
    subprocess.run([
        'java', '-cp', str(classes) + os.pathsep + str(stdlib),
        'PlaybackSessionRegressionKt'
    ], check=True, timeout=20)
