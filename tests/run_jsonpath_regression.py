#!/usr/bin/env python3
"""Compile and exercise production JSON-path, configuration, and URL methods.

This focused JVM regression does not compile the Android UI or HTTP clients.
"""
from pathlib import Path
import os
import re
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'android/app/src/main/kotlin/com/example/piliai/data/repository/AnimekoScraper.kt'
source = SOURCE.read_text(encoding='utf-8')


def production_method(name):
    declaration = re.search(
        rf'(?m)^    (?:private )?fun {re.escape(name)}\(', source
    )
    if declaration is None:
        raise RuntimeError(f'Production method not found: {name}')
    following = re.search(
        r'(?m)^    (?:private )?fun \w+\(', source[declaration.end():]
    )
    if following is None:
        raise RuntimeError(f'Cannot determine production method boundary: {name}')
    end = declaration.end() + following.start()
    return source[declaration.start():end]


# Preserve production models and configuration decoding, without pulling in jsoup.
model_end = source.index('    /**\n     * 第一步：搜索页')
program = source[:model_end]
for method in ('parseSubjectsByJsonPath', 'jsonPathField', 'isHttpUrl', 'resolveUrl'):
    program += production_method(method) + '\n'
program += r'''
    fun regressionParse(text: String, baseUrl: String, cfg: SearchConfig): List<Subject> =
        parseSubjectsByJsonPath(text, baseUrl, cfg)
}

fun main() {
    val base = "https://source.test/catalog/search.json"
    val dot = AnimekoScraper.SearchConfig(
        subjectFormatId = "json-path-indexed",
        selectNamesJsonPath = "\$[*].name",
        selectLinksJsonPath = "\$[*].url"
    )
    val rows = """[
        {"name":"First","url":"../show/1"},
        {"name":"Second","url":"/show/2"},
        {"name":"Third","url":"//cdn.test/show/3"},
        {"name":"Duplicate","url":"/show/2"},
        {"name":"Invalid scheme","url":"javascript:alert(1)"},
        {"name":"Embedded credentials","url":"https://user:pass@cdn.test/show/4"},
        {"name":"","url":"/show/5"},
        {"name":"Missing URL"},
        {"name":123,"url":"/show/6"},
        {"name":"Object URL","url":{"value":"/show/7"}},
        null,
        42
    ]"""
    val expected = listOf(
        AnimekoScraper.Subject("First", "https://source.test/show/1"),
        AnimekoScraper.Subject("Second", "https://source.test/show/2"),
        AnimekoScraper.Subject("Third", "https://cdn.test/show/3")
    )
    check(AnimekoScraper.regressionParse(rows, base, dot) == expected)
    println("PASS production JSON-path: paired rows, relative URLs, deduplication, invalid entries")

    val bracket = dot.copy(
        selectNamesJsonPath = "\$[*]['name']",
        selectLinksJsonPath = "\$[*]['url']"
    )
    check(AnimekoScraper.regressionParse(rows, base, bracket) == expected)
    check(AnimekoScraper.regressionParse("[]", base, dot).isEmpty())
    println("PASS production JSON-path: bracket fields and empty arrays")

    check(runCatching {
        AnimekoScraper.regressionParse(rows, base, dot.copy(selectNamesJsonPath = "\$.data[*].name"))
    }.isFailure)
    check(runCatching {
        AnimekoScraper.regressionParse(rows, base, dot.copy(selectLinksJsonPath = "\$..url"))
    }.isFailure)
    check(runCatching {
        AnimekoScraper.regressionParse("""{"unrelated":[{"name":"Wrong","url":"/wrong"}]}""", base, dot)
    }.isFailure)
    check(runCatching {
        AnimekoScraper.regressionParse("{malformed", base, dot)
    }.isFailure)
    println("PASS production JSON-path: unsupported paths, wrong root, malformed JSON")

    val configuration = """{
        "exportedMediaSourceDataList": {
            "mediaSources": [{
                "factoryId": "web-selector",
                "arguments": {
                    "name": "Controlled fixture",
                    "searchConfig": {
                        "subjectFormatId": "json-path-indexed",
                        "selectorSubjectFormatJsonPathIndexed": {
                            "selectNames": "$[*].name",
                            "selectLinks": "$[*].url"
                        }
                    }
                }
            }]
        }
    }"""
    val parsed = AnimekoScraper.parseConfig(configuration).single()
    check(parsed.cfg.subjectFormatId == "json-path-indexed")
    check(parsed.cfg.selectNamesJsonPath == dot.selectNamesJsonPath)
    check(parsed.cfg.selectLinksJsonPath == dot.selectLinksJsonPath)
    check(AnimekoScraper.regressionParse(rows, base, parsed.cfg) == expected)
    println("PASS production configuration: JSON-path fields reach the parser")
}
'''

configured = os.environ.get('KOTLIN_LIB')
if configured:
    compiler_lib = Path(configured)
else:
    candidates = sorted(Path.home().glob(
        '.gradle/wrapper/dists/gradle-8.14*-bin/*/gradle-8.14*/lib'
    ))
    if not candidates:
        raise RuntimeError('Set KOTLIN_LIB to an installed standalone Kotlin compiler library directory')
    compiler_lib = candidates[-1]
stdlibs = sorted(compiler_lib.glob('kotlin-stdlib-*.jar'))
if not stdlibs:
    raise RuntimeError(f'Kotlin standard library is missing from {compiler_lib}')

cache = Path.home() / '.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlinx'
dependencies = [stdlibs[-1]]
for artifact in ('kotlinx-serialization-json-jvm', 'kotlinx-serialization-core-jvm'):
    jars = sorted((cache / artifact / '1.7.3').glob('*/*.jar'))
    if not jars:
        raise RuntimeError(f'Missing cached project dependency: {artifact}:1.7.3')
    dependencies.append(jars[0])
classpath = os.pathsep.join(map(str, dependencies))

with tempfile.TemporaryDirectory(prefix='piliai-jsonpath-') as directory:
    temporary = Path(directory)
    kotlin_file = temporary / 'JsonPathRegression.kt'
    kotlin_file.write_text(program, encoding='utf-8')
    classes = temporary / 'classes'
    subprocess.run([
        'java', '-cp', str(compiler_lib / '*'),
        'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
        '-no-stdlib', '-no-reflect', '-classpath', classpath,
        '-d', str(classes), str(kotlin_file)
    ], check=True, timeout=60)
    subprocess.run([
        'java', '-cp', str(classes) + os.pathsep + classpath,
        'com.example.piliai.data.repository.JsonPathRegressionKt'
    ], check=True, timeout=30)
