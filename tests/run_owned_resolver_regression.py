#!/usr/bin/env python3
"""Test production resolver selection with synthetic subscriptions and controlled responses.
No network, Android build, database writes, or real credentials are used.
"""
from pathlib import Path
import os
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
source = (ROOT / 'android/app/src/main/kotlin/com/example/piliai/ui/subscribe/SubscribeViewModel.kt').read_text(encoding='utf-8')
start = source.index('    fun reportNotPlayable(')
end = source.index('\n    }', start) + len('\n    }')
method = source[start:end]
# Replace only Android dependencies with controlled test doubles.
method = method.replace('com.example.piliai.AppContext.get()', 'TestContext')
method = method.replace('com.example.piliai.R.string.', 'Messages.')
program = r'''
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

data class Source(val id: Long, val url: String, val enabled: Boolean)
data class SubscribeItemEntity(val sourceId: Long, val title: String, val sourceName: String)
data class UiState(
    val sources: List<Source>,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false
)
object Messages {
    const val subscribe_source_unavailable = 1
    const val subscribe_no_matching_resolver = 2
    const val subscribe_choose_resolver = 3
    const val subscribe_resolver_load_failed = 4
}
object TestContext {
    fun getString(id: Int): String = id.toString()
}
class Repository {
    val calls = mutableListOf<String>()
    val responses = mutableMapOf<String, CompletableDeferred<List<String>>>()
    suspend fun searchableWebSourceNames(url: String): List<String> {
        calls.add(url)
        return responses.getValue(url).await()
    }
}
class Harness {
    private val owner = SupervisorJob()
    val viewModelScope = CoroutineScope(owner + Dispatchers.Unconfined)
    val repository = Repository()
    val _state = MutableStateFlow(UiState(listOf(
        Source(1L, "https://fixture.test/unrelated", true),
        Source(7L, "https://fixture.test/owned", true)
    )))
    var browseJob: Job? = null
    var searchJob: Job? = null
    var refreshJob: Job? = null
    var browseGeneration = 0L
    val searches = mutableListOf<Triple<String, String, String>>()
    fun searchWebSource(url: String, name: String, keyword: String) {
        searches.add(Triple(url, name, keyword))
    }
    fun close() = owner.cancel()
''' + method + r'''
}
fun main() = runBlocking {
    val owned = "https://fixture.test/owned"
    val item = SubscribeItemEntity(7L, "Episode title", "Resolver A")
    suspend fun scenario(names: List<String>, expectedName: String?, expectedError: Int?) {
        val h = Harness()
        try {
            h.repository.responses[owned] = CompletableDeferred(names)
            h.reportNotPlayable(item)
            h.browseJob!!.join()
            check(h.repository.calls == listOf(owned)) { "Selected an unrelated subscription" }
            if (expectedName == null) check(h.searches.isEmpty())
            else check(h.searches.single() == Triple(owned, expectedName, item.title))
            check(h._state.value.errorMessage == expectedError?.toString())
            check(!h._state.value.isRefreshing)
        } finally { h.close() }
    }
    scenario(listOf("Resolver A", "Resolver B"), "Resolver A", null)
    scenario(listOf("Only resolver"), "Only resolver", null)
    scenario(emptyList(), null, Messages.subscribe_no_matching_resolver)
    scenario(listOf("Other A", "Other B"), null, Messages.subscribe_choose_resolver)
    println("PASS production resolver: owned subscription, exact name, sole resolver, empty and ambiguous configurations")

    val unavailable = Harness()
    try {
        unavailable.reportNotPlayable(item.copy(sourceId = 99L))
        check(unavailable.repository.calls.isEmpty())
        check(unavailable.searches.isEmpty())
        check(unavailable._state.value.errorMessage == Messages.subscribe_source_unavailable.toString())
    } finally { unavailable.close() }
    println("PASS production resolver: missing subscription never queries another subscription")

    val disabled = Harness()
    try {
        val response = CompletableDeferred<List<String>>()
        disabled.repository.responses[owned] = response
        disabled.reportNotPlayable(item)
        disabled._state.value = disabled._state.value.copy(sources = listOf(Source(7L, owned, false)))
        response.complete(listOf("Resolver A"))
        disabled.browseJob!!.join()
        check(disabled.searches.isEmpty())
        check(disabled._state.value.errorMessage == Messages.subscribe_source_unavailable.toString())
    } finally { disabled.close() }
    println("PASS production resolver: disabling subscription during lookup prevents search")

    val stale = Harness()
    try {
        val response = CompletableDeferred<List<String>>()
        stale.repository.responses[owned] = response
        stale.reportNotPlayable(item)
        stale.browseGeneration += 1
        response.complete(listOf("Resolver A"))
        stale.browseJob!!.join()
        check(stale.searches.isEmpty())
    } finally { stale.close() }
    println("PASS production resolver: obsolete lookup cannot trigger a search")

    val failed = Harness()
    try {
        val response = CompletableDeferred<List<String>>()
        failed.repository.responses[owned] = response
        failed.reportNotPlayable(item)
        response.completeExceptionally(IllegalStateException("Controlled configuration failure"))
        failed.browseJob!!.join()
        check(failed.searches.isEmpty())
        check(failed._state.value.errorMessage == Messages.subscribe_resolver_load_failed.toString())
        check(!failed._state.value.isRefreshing)
    } finally { failed.close() }
    println("PASS production resolver: configuration failure is visible and loading resets")
}
'''
configured = os.environ.get('KOTLIN_LIB')
candidates = sorted(Path.home().glob('.gradle/wrapper/dists/gradle-8.14*-bin/*/gradle-8.14*/lib'))
if not configured and not candidates:
    raise RuntimeError('Set KOTLIN_LIB to an installed compiler library directory')
lib = Path(configured) if configured else candidates[-1]
stdlib = next(lib.glob('kotlin-stdlib-*.jar'))
coroutines = sorted(lib.glob('kotlinx-coroutines-core-jvm-*.jar'))
if not coroutines:
    coroutines = sorted((Path.home() / '.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm').glob('*/*/*.jar'))
if not coroutines:
    raise RuntimeError('Coroutine JVM library is not cached')
classpath = os.pathsep.join(map(str, [stdlib, coroutines[-1]]))
with tempfile.TemporaryDirectory(prefix='piliai-owned-resolver-') as directory:
    directory = Path(directory)
    kotlin = directory / 'OwnedResolverRegression.kt'
    kotlin.write_text(program, encoding='utf-8')
    classes = directory / 'classes'
    subprocess.run([
        'java', '-cp', str(lib / '*'), 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
        '-no-stdlib', '-no-reflect', '-classpath', classpath,
        '-d', str(classes), str(kotlin)
    ], check=True, timeout=60)
    subprocess.run([
        'java', '-cp', str(classes) + os.pathsep + classpath,
        'OwnedResolverRegressionKt'
    ], check=True, timeout=20)
