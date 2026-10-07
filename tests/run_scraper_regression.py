#!/usr/bin/env python3
"""Compile verbatim production Kotlin URL/regex methods (no Python reimplementation)."""
from pathlib import Path
import os
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
source = (ROOT / 'android/app/src/main/kotlin/com/example/piliai/data/repository/AnimekoScraper.kt').read_text(encoding='utf-8')
start = source.index('    fun extractVideoUrl(')
end = source.index('    fun extractNestedUrls(', start)
# Last KDoc belongs to extractNestedUrls, strip it off.
end = source.rfind('\n    /**', start, end)
url_start = source.index('    private fun resolveUrl(')
url_end = source.index('\n    }', url_start) + len('\n    }')
methods = source[start:end] + '\n' + source[url_start:url_end]
program = '''object Scraper {
    data class SearchConfig(val matchVideoUrl: String)
''' + methods + '''
    fun groupIndex(pattern: String) = compileWithNamedGroup(pattern, "v").second
    fun resolve(value: String, base: String) = resolveUrl(value, base)
}
fun main() {
    val cfg = Scraper.SearchConfig("(^https?://.+m3u8.*)|(akamaized)|(url=(?<v>https?://[^\\\" ]+))")
    val url = "https://ordinary.cdn.test/episode.m3u8?sig=a%2Bb&expires=123"
    check(Scraper.extractVideoUrl("<script>var u=\\\"$url\\\";</script>", "https://site.test/play", cfg) == url)
    check(Scraper.extractVideoUrl("akamaized text <script>var u=\\\"$url\\\";</script>", "https://site.test/play", cfg) == url)
    val escaped = url.replace("/", "\\\\/")
    check(Scraper.extractVideoUrl("{\\\"url\\\":\\\"$escaped\\\"}", "https://site.test/play", cfg) == url)
    val wrapper = "https://player.test/?url=https://media.test/a.mp4"
    val named = Scraper.SearchConfig("url=(?<v>https?://.+)")
    check(Scraper.extractVideoUrl(wrapper, "https://site.test", named) == "https://media.test/a.mp4")
    check(Scraper.extractVideoUrl("url=https%3A%2F%2Fcdn.test%2Fa.m3u8", "https://site.test", Scraper.SearchConfig("url=(?<v>[^ ]+)")) == "https://cdn.test/a.m3u8")
    check(Scraper.extractVideoUrl("akamaized prose", "https://site.test", cfg).isEmpty())
    check(Scraper.extractVideoUrl("httpx://bad", "https://site.test", cfg).isEmpty())
    check(Scraper.extractVideoUrl("https://user:pass@cdn.test/a.m3u8", "https://site.test", cfg).isEmpty())
    check(Scraper.extractVideoUrl(url, "https://site.test", Scraper.SearchConfig("[invalid")).isEmpty())
    println("PASS production URL extraction: ordinary CDN, prose, escapes, named/encoded URLs, invalid URLs")
    check(Scraper.groupIndex("(?:x)(a)(?<v>.+)") == 2)
    check(Scraper.groupIndex("\\\\((?<v>.+)") == 1)
    check(Scraper.groupIndex("[(](?<v>.+)") == 1)
    check(Scraper.groupIndex("(?i)(?<v>.+)") == 1)
    check(Scraper.groupIndex("(?<=x)(a)(?<v>.+)") == 2)
    println("PASS production capture index: escaped parentheses, character class, flags, lookbehind")
    val base = "https://site.test/vod/detail/1.html"
    check(Scraper.resolve("../play/2", base) == "https://site.test/vod/play/2")
    check(Scraper.resolve("/play/2", base) == "https://site.test/play/2")
    check(Scraper.resolve("//cdn.test/a", "http://site.test") == "http://cdn.test/a")
    println("PASS production URI resolve")
}
'''
lib = Path(os.environ.get('KOTLIN_LIB') or (sorted(Path.home().glob('.gradle/wrapper/dists/gradle-8.14*-bin/*/gradle-8.14*/lib')) or [None])[-1] or Path('/opt/gradle-8.14.2/lib'))
stdlib = next(lib.glob('kotlin-stdlib-*.jar'))
with tempfile.TemporaryDirectory(prefix='pilinara-scraper-') as directory:
    d = Path(directory)
    kt = d / 'ScraperRegression.kt'
    kt.write_text(program, encoding='utf-8')
    subprocess.run(['java', '-cp', str(lib / '*'), 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', '-no-stdlib', '-no-reflect', '-classpath', str(stdlib), '-d', str(d / 'classes'), str(kt)], check=True)
    subprocess.run(['java', '-cp', f'{d / "classes"}:{stdlib}', 'ScraperRegressionKt'], check=True)
