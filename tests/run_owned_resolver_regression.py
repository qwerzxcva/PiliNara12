#!/usr/bin/env python3
"""Test production resolver selection using synthetic subscriptions.

No network requests, database writes, Android lifecycle verification, or real
credentials are involved.
"""
from pathlib import Path
import os
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
source = (ROOT / 'android/app/src/main/kotlin/com/example/piliai/ui/subscribe/SubscribeViewModel.kt').read_text(encoding='utf-8')
start = source.index('    fun reportNotPlayable(')
end = source.index('\n    }', start) + len('\n    }')
method = source[start:end].replace('com.example.piliai.AppContext.get()', 'TestContext')
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
object TestContext { fun getString(id: Int): String = id.toString() }
class Repository {
    val calls = mutableListOf<String>()
    val response = CompletableDeferred<List<String>>()
    suspend fun searchableWebSourceNames(url: String): List<String> {
        calls.add(url)
        return response.await()
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
    val item = SubscribeItemEntity(7L, "Synthetic title", "Resolver A")
    suspend fun scenario(names: List<String>, expected: String?, error: Int?) {
        val h = Harness()
        try {
            h.repository.response.complete(names)
            h.reportNotPlayable(item)
            h.browseJob!!.join()
            check(h.repository.calls == listOf(owned))
            check(h.searches == if (expected == null) emptyList()
                else listOf(Triple(owned, expected, item.title)))
            check(h._state.value.errorMessage == error?.toString())
            check(!h._state.value.isRefreshing)
        } finally { h.close() }
    }
    scenario(listOf("Resolver A", "Resolver B"), "Resolver A", null)
    scenario(listOf("Only resolver"), "Only resolver", null)
    scenario(emptyList(), null, Messages.subscribe_no_matching_resolver)
    scenario(listOf("Other A", "Other B"), null, Messages.subscribe_choose_resolver)
    println("PASS production resolver: ownership, exact name, sole resolver and ambiguous configurations")

    for (source in listOf<Source?>(null, Source(7L, owned, false))) {
        val h = Harness()
        try {
            h._state.value = h._state.value.copy(sources = listOfNotNull(source))
            h.reportNotPlayable(item)
            check(h.repository.calls.isEmpty() && h.searches.isEmpty())
            check(h._state.value.errorMessage == Messages.subscribe_source_unavailable.toString())
        } finally { h.close() }
    }
    println("PASS production resolver: missing or disabled sources never query another subscription")

    for (change in listOf("disabled", "changed-url", "obsolete", "cancelled", "failure")) {
        val h = Harness()
        try {
            h.reportNotPlayable(item)
            val job = h.browseJob!!
            when (change) {
                "disabled" -> h._state.value = h._state.value.copy(sources = listOf(Source(7L, owned, false)))
                "changed-url" -> h._state.value = h._state.value.copy(sources = listOf(Source(7L, "https://fixture.test/replaced", true)))
                "obsolete" -> h.browseGeneration += 1
                "cancelled" -> job.cancel()
            }
            if (change == "failure") {
                h.repository.response.completeExceptionally(IllegalStateException("Controlled failure"))
            } else {
                h.repository.response.complete(listOf("Resolver A"))
            }
            job.join()
            check(h.searches.isEmpty()) { "Unexpected search after $change" }
            if (change == "disabled" || change == "changed-url") {
                check(h._state.value.errorMessage == Messages.subscribe_source_unavailable.toString())
            }
            if (change == "failure") {
                check(h._state.value.errorMessage == Messages.subscribe_resolver_load_failed.toString())
                check(!h._state.value.isRefreshing)
            }
        } finally { h.close() }
    }
    println("PASS production resolver: source changes, obsolete requests, cancellation and visible failures")
}
'''
configured = os.environ.get('KOTLIN_LIB')
candidates = sorted(Path.home().glob('.gradle/wrapper/dists/gradle-*/*/gradle-*/lib'))
candidates = [p for p in candidates if list(p.glob('kotlin-compiler-embeddable-*.jar'))]
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
    temporary = Path(directory)
    test = temporary / 'OwnedResolverRegression.kt'
    test.write_text(program, encoding='utf-8')
    classes = temporary / 'classes'
    subprocess.run([
        'java', '-cp', str(lib / '*'), 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
        '-no-stdlib', '-no-reflect', '-classpath', classpath,
        '-d', str(classes), str(test)
    ], check=True, timeout=60)
    subprocess.run([
        'java', '-cp', str(classes) + os.pathsep + classpath,
        'OwnedResolverRegressionKt'
    ], check=True, timeout=20)
