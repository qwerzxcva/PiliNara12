#!/usr/bin/env python3
"""Exercise the production RSS search and cancellation methods on the JVM.

Uses controlled responses and synthetic source data; makes no network requests.
Does not verify Android navigation, Room, or actual media playback.
"""
from pathlib import Path
import os
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
source = (ROOT / 'android/app/src/main/kotlin/com/example/piliai/ui/subscribe/SubscribeViewModel.kt').read_text(encoding='utf-8')

def method(name):
    start = source.index('    fun ' + name + '(')
    end = source.index('\n    }', start) + len('\n    }')
    return source[start:end]

program = r'''
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

data class Source(val id: Long, val name: String, val enabled: Boolean)
data class SubscribeItemEntity(
    val sourceId: Long,
    val title: String,
    val cover: String,
    val link: String,
    val desc: String,
    val episode: String,
    val sourceName: String
)
data class ParsedItem(
    val title: String,
    val cover: String = "",
    val link: String = "https://fixture.test/video.mp4",
    val desc: String = "",
    val episode: String = ""
)
data class ParsedSource(val items: List<ParsedItem>)
data class UiState(
    val sources: List<Source>,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)
object SubscribeParser {
    val responses = mutableMapOf<String, CompletableDeferred<Result<ParsedSource>>>()
    suspend fun searchAnimekoSource(url: String, keyword: String, factoryId: String): Result<ParsedSource> {
        check(url == "https://fixture.test/feed")
        check(factoryId == "rss")
        return responses.getValue(keyword).await()
    }
}
class SearchHarness {
    private val owner = SupervisorJob()
    val viewModelScope = CoroutineScope(owner + Dispatchers.Unconfined)
    val _state = MutableStateFlow(UiState(listOf(Source(7L, "Imported source", true))))
    val _searchResults = MutableStateFlow<List<SubscribeItemEntity>>(emptyList())
    val _episodes = MutableStateFlow<List<String>>(emptyList())
    val _currentSubject = MutableStateFlow<String?>(null)
    var refreshJob: Job? = null
    var searchJob: Job? = null
    var browseJob: Job? = null
    var browseGeneration = 0L
    var activeSourceUrl = ""
    var activeSourceName = ""
    fun close() = owner.cancel()
''' + method('searchInSource') + '\n' + method('clearSearch') + r'''
}
fun response(key: String): CompletableDeferred<Result<ParsedSource>> {
    val deferred = CompletableDeferred<Result<ParsedSource>>()
    SubscribeParser.responses[key] = deferred
    return deferred
}
fun search(harness: SearchHarness, key: String, sourceId: Long = 7L) {
    harness.searchInSource("https://fixture.test/feed", key, "rss", sourceId)
}
fun success(title: String) = Result.success(ParsedSource(listOf(ParsedItem(title))))
fun main() = runBlocking {
    val harness = SearchHarness()
    try {
        val first = response("A")
        search(harness, "A")
        val obsolete = harness.searchJob!!
        val second = response("B")
        search(harness, "B")
        check(obsolete.isCancelled)
        second.complete(success("Newest"))
        harness.searchJob!!.join()
        first.complete(success("Obsolete"))
        yield()
        val item = harness._searchResults.value.single()
        check(item.title == "Newest")
        check(item.sourceId == 7L && item.sourceName == "Imported source")
        check(!harness._state.value.isRefreshing)
        println("PASS production search: newer query cancels older query and preserves source identity")

        val pending = response("clear")
        search(harness, "clear")
        val clearedJob = harness.searchJob!!
        harness.clearSearch()
        pending.complete(success("Must not reappear"))
        yield()
        check(clearedJob.isCancelled)
        check(harness._searchResults.value.isEmpty())
        check(!harness._state.value.isRefreshing)
        println("PASS production clearSearch: pending response cannot repopulate results")

        harness._state.value = harness._state.value.copy(sources = listOf(Source(7L, "Imported source", true)))
        val disabled = response("disabled")
        search(harness, "disabled")
        harness._state.value = harness._state.value.copy(sources = listOf(Source(7L, "Imported source", false)))
        disabled.complete(success("Must not publish"))
        harness.searchJob!!.join()
        check(harness._searchResults.value.isEmpty())
        check(harness._state.value.errorMessage != null)
        check(!harness._state.value.isRefreshing)
        println("PASS production search: disabling source during request prevents result publication")

        val prior = harness.searchJob
        search(harness, "not-requested", 99L)
        check(harness.searchJob === prior)
        check(harness._state.value.errorMessage != null)
        println("PASS production search: unknown source does not start a request")

        harness._state.value = harness._state.value.copy(sources = listOf(Source(7L, "Imported source", true)))
        val failed = response("failure")
        search(harness, "failure")
        failed.complete(Result.failure(IllegalStateException("Controlled source failure")))
        harness.searchJob!!.join()
        check(harness._state.value.errorMessage == "Controlled source failure")
        check(!harness._state.value.isRefreshing)
        println("PASS production search: source failure is visible and loading state resets")

        val empty = response("empty")
        search(harness, "empty")
        empty.complete(Result.success(ParsedSource(emptyList())))
        harness.searchJob!!.join()
        check(harness._searchResults.value.isEmpty())
        check(harness._state.value.infoMessage != null)
        check(!harness._state.value.isRefreshing)
        println("PASS production search: empty response stays empty with an explicit message")
    } finally {
        harness.close()
    }
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
    raise RuntimeError('No cached coroutine JVM library available')
classpath = os.pathsep.join(map(str, [stdlib, coroutines[-1]]))
with tempfile.TemporaryDirectory(prefix='piliai-subscription-search-') as directory:
    directory = Path(directory)
    kotlin = directory / 'SubscriptionSearchRegression.kt'
    kotlin.write_text(program, encoding='utf-8')
    classes = directory / 'classes'
    subprocess.run([
        'java', '-cp', str(lib / '*'), 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
        '-no-stdlib', '-no-reflect', '-classpath', classpath,
        '-d', str(classes), str(kotlin)
    ], check=True, timeout=60)
    subprocess.run([
        'java', '-cp', str(classes) + os.pathsep + classpath,
        'SubscriptionSearchRegressionKt'
    ], check=True, timeout=20)
