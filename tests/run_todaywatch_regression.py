#!/usr/bin/env python3
"""编译生产 TodayWatchAlgorithm.kt 并跑行为对拍测试（与 Dart 端相同输入）。"""
from pathlib import Path
import subprocess, tempfile

ROOT = Path(__file__).resolve().parents[1]
algo = (ROOT / 'android/app/src/main/kotlin/com/example/piliai/todaywatch/TodayWatchAlgorithm.kt').read_text(encoding='utf-8')

test = '''
fun approx(a: Double, b: Double, eps: Double = 1e-6): Boolean = kotlin.math.abs(a - b) < eps

fun main() {
    val now = 1_700_000_000L

    // 历史：UP1 高完成度近期观看；UP2 低完成度
    val history = listOf(
        WatchHistoryItem(bvid="BV1", title="编程教程", authorMid=1, authorName="UP1",
            viewAt=now-3600, progress=540, duration=600),   // 完成90% 近期
        WatchHistoryItem(bvid="BV2", title="美食探店", authorMid=2, authorName="UP2",
            viewAt=now-10*86400, progress=60, duration=600) // 完成10% 10天前
    )
    // 候选：UP1 的编程视频(应排前) vs UP3 的新视频 vs UP2 的美食视频(完成度低)
    val candidates = listOf(
        RcmdCandidate(bvid="C1", title="kotlin 编程进阶教程", ownerMid=1, ownerName="UP1",
            duration=600, pubdate=now-86400, viewCount=100000, likeCount=5000, danmakuCount=200),
        RcmdCandidate(bvid="C2", title="搞笑日常 vlog", ownerMid=3, ownerName="UP3",
            duration=300, pubdate=now-86400, viewCount=50000, likeCount=2000, danmakuCount=50),
        RcmdCandidate(bvid="C3", title="美食做饭教程", ownerMid=2, ownerName="UP2",
            duration=600, pubdate=now-86400, viewCount=80000, likeCount=3000, danmakuCount=80)
    )
    val plan = buildTodayWatchPlan(
        historyVideos = history,
        candidateVideos = candidates,
        mode = TodayWatchMode.LEARN,
        nowEpochSec = now,
    )

    // 1) UP1 应排创作者榜第一（完成度高+近期）
    check(plan.upRanks.isNotEmpty()) { "upRanks 为空" }
    check(plan.upRanks[0].mid == 1L) { "UP1 应排第一，实际 " + plan.upRanks[0].mid }

    // 2) 常看 UP1 的 C1 应得分高于新 UP3 的 C2（balanced 下 interest 占大头）
    val s1 = plan.scoreByBvid["C1"] ?: 0.0
    val s2 = plan.scoreByBvid["C2"] ?: 0.0
    check(s1 > s2) { "C1($s1) 应 > C2($s2)" }

    // 3) 推荐理由非空
    check((plan.explanationByBvid["C1"] ?: "").isNotEmpty()) { "C1 应有推荐理由" }

    // 4) 负反馈过滤：disliked C1 后应被剔除
    val plan2 = buildTodayWatchPlan(history, candidates, TodayWatchMode.LEARN,
        nowEpochSec = now, penaltySignals = PenaltySignals(dislikedBvids = setOf("C1")))
    check(plan2.videoQueue.none { it.bvid == "C1" }) { "C1 被点踩后仍出现" }

    // 5) 空历史降级：全部候选 exploration=1，仍出单
    val plan3 = buildTodayWatchPlan(emptyList(), candidates, TodayWatchMode.RELAX, nowEpochSec = now)
    check(plan3.videoQueue.isNotEmpty()) { "空历史应仍能出推荐" }

    println("PASS: creator rank, score order, explanation, dislike filter, empty-history degrade")
}
'''

lib = Path('/opt/gradle-8.14.2/lib')
stdlib = next(lib.glob('kotlin-stdlib-*.jar'))
with tempfile.TemporaryDirectory(prefix='todaywatch-') as d:
    dd = Path(d)
    (dd / 'Algo.kt').write_text('package com.example.piliai.todaywatch\n\n' + algo.split('package com.example.piliai.todaywatch',1)[1], encoding='utf-8')
    (dd / 'Test.kt').write_text('import com.example.piliai.todaywatch.*\n' + test, encoding='utf-8')
    subprocess.run(['java','-cp',str(lib/'*'),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
        '-no-stdlib','-no-reflect','-classpath',str(stdlib),'-d',str(dd/'classes'),
        str(dd/'Algo.kt'), str(dd/'Test.kt')], check=True)
    subprocess.run(['java','-cp',f"{dd/'classes'}:{stdlib}",'TestKt'], check=True)
