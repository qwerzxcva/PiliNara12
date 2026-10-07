package com.example.piliai.todaywatch

import kotlin.math.ln
import kotlin.math.pow

/**
 * 「今日推荐」算法（纯函数直译自 today-watch-new 分支的 Dart 实现，零 Android 依赖）。
 *
 * 核心：创作者亲和度 + 主题偏好 + MMR 多样性排序。
 * 输入：观看历史 + 候选视频；输出：UP 主榜 + 推荐队列。
 * 可单测（与 Dart 端相同输入对拍）。
 */

// ==================== 数据模型 ====================

/** 观看历史项（对齐 B 站 history/cursor 返回） */
data class WatchHistoryItem(
    val bvid: String = "",
    val title: String = "",
    val tagName: String = "",
    val authorMid: Long = 0L,
    val authorName: String = "",
    val kid: Long = 0L,          // 分区 id（partition-id 信号；B 站接口不返回时为 0）
    val viewAt: Long = 0L,       // 观看时间（秒）
    val progress: Long = -1L,    // 观看进度（秒），-1 表示未知
    val duration: Long = 0L,     // 视频时长（秒）
)

/** 候选视频（对齐首页推荐流） */
data class RcmdCandidate(
    val bvid: String = "",
    val title: String = "",
    val cover: String = "",
    val duration: Long = 0L,     // 秒
    val pubdate: Long = 0L,      // 秒
    val ownerMid: Long = 0L,
    val ownerName: String = "",
    val viewCount: Long = 0L,
    val likeCount: Long = 0L,
    val danmakuCount: Long = 0L,
)

/** 创作者信号（跨会话持久化的兴趣分） */
data class CreatorSignal(val mid: Long, val name: String = "", val score: Double, val watchCount: Int = 1)

/** 负反馈信号（硬过滤） */
data class PenaltySignals(
    val consumedBvids: Set<String> = emptySet(),
    val dislikedBvids: Set<String> = emptySet(),
    val dislikedCreatorMids: Set<Long> = emptySet(),
    val dislikedKeywords: Set<String> = emptySet(),
)

enum class TodayWatchMode { RELAX, LEARN }
enum class TodayWatchStrategy { BALANCED, AFFINITY, EXPLORE }

/** UP 主排名项 */
data class CreatorRank(val mid: Long, val name: String, val score: Double, val watchCount: Int)

/** 推荐计划结果 */
data class TodayWatchPlan(
    val mode: TodayWatchMode,
    val upRanks: List<CreatorRank>,
    val videoQueue: List<RcmdCandidate>,
    val explanationByBvid: Map<String, String>,
    val scoreByBvid: Map<String, Double>,
    val confidenceByBvid: Map<String, Double>,
    val historySampleCount: Int,
    val nightSignalUsed: Boolean,
)

// ==================== 内部结构 ====================

private data class StrategyWeights(
    val interest: Double, val mode: Double, val freshness: Double,
    val quality: Double, val exploration: Double, val diversity: Double,
)

private data class CandidateFeatures(
    val creatorAffinity: Double, val topicAffinity: Double, val interest: Double,
    val modeFit: Double, val freshness: Double, val quality: Double,
    val exploration: Double, val topics: Set<String> = emptySet(),
)

private data class ScoredCandidate(
    val video: RcmdCandidate,
    val originalIndex: Int,
    val baseScore: Double,
    val confidence: Double,
    val features: CandidateFeatures,
    val explanation: String,
)

// ==================== 关键词常量 ====================

private val RELAX_KEYWORDS = listOf("音乐", "vlog", "日常", "搞笑", "轻松", "治愈", "asmr", "旅行", "美食", "游戏")
private val LEARN_KEYWORDS = listOf("教程", "科普", "知识", "学习", "原理", "实战", "复盘", "编程", "数学", "英语", "课程", "技术", "分析", "入门", "进阶")
private val TOPIC_KEYWORDS = listOf(
    "music" to listOf("音乐", "唱", "歌", "演奏", "翻唱", "live"),
    "learn" to listOf("教程", "科普", "知识", "学习", "原理", "实战", "复盘", "编程", "数学", "英语", "课程", "技术", "分析", "入门", "进阶", "kotlin", "android"),
    "game" to listOf("游戏", "实况", "通关", "原神", "崩坏", "minecraft"),
    "food" to listOf("美食", "做饭", "料理", "探店"),
    "travel" to listOf("旅行", "旅游", "城市", "徒步", "露营", "vlog"),
    "relax" to listOf("日常", "搞笑", "轻松", "治愈", "asmr"),
)

// ==================== 主入口 ====================

fun buildTodayWatchPlan(
    historyVideos: List<WatchHistoryItem>,
    candidateVideos: List<RcmdCandidate>,
    mode: TodayWatchMode,
    eyeCareNightActive: Boolean = false,
    nowEpochSec: Long = 0L,
    upRankLimit: Int = 5,
    queueLimit: Int = 20,
    creatorSignals: List<CreatorSignal> = emptyList(),
    penaltySignals: PenaltySignals = PenaltySignals(),
    strategy: TodayWatchStrategy = TodayWatchStrategy.BALANCED,
): TodayWatchPlan {
    val now = if (nowEpochSec > 0) nowEpochSec else System.currentTimeMillis() / 1000

    // 清理并按观看时间倒序
    val cleanedHistory = historyVideos
        .filter { it.bvid.isNotEmpty() }
        .sortedByDescending { it.viewAt }

    // 1) 近期创作者分 + 主题分
    val recentCreatorScores = mutableMapOf<Long, Double>()
    val recentCreatorCounts = mutableMapOf<Long, Int>()
    val creatorNames = mutableMapOf<Long, String>()
    val rawTopicScores = mutableMapOf<String, Double>()

    for (item in cleanedHistory) {
        val completion = estimateCompletionRatio(item)
        val affinity = watchAffinityScore(completion, recencyBonus(item.viewAt, now))
        if (item.authorMid > 0) {
            recentCreatorScores[item.authorMid] = (recentCreatorScores[item.authorMid] ?: 0.0) + affinity
            recentCreatorCounts[item.authorMid] = (recentCreatorCounts[item.authorMid] ?: 0) + 1
            creatorNames[item.authorMid] = item.authorName.ifBlank { "UP主${item.authorMid}" }
        }
        for (topic in resolveTopicKeys(item)) {
            rawTopicScores[topic] = (rawTopicScores[topic] ?: 0.0) + affinity
        }
    }

    // 2) 持久化创作者信号（max 合并）
    val persistedCreatorScores = mutableMapOf<Long, Double>()
    val persistedCreatorCounts = mutableMapOf<Long, Int>()
    for (s in creatorSignals.filter { it.mid > 0 }) {
        val cur = persistedCreatorScores[s.mid]
        if (cur == null || cur < s.score) persistedCreatorScores[s.mid] = s.score
        val cc = persistedCreatorCounts[s.mid]
        if (cc == null || cc < s.watchCount) persistedCreatorCounts[s.mid] = s.watchCount
        creatorNames.putIfAbsent(s.mid, s.name.ifBlank { "UP主${s.mid}" })
    }

    // 3) 归一化 + 融合
    val normRecent = normalizePositiveScores(recentCreatorScores)
    val normPersisted = normalizePositiveScores(persistedCreatorScores)
    val creatorAffinity = mutableMapOf<Long, Double>()
    for (mid in normRecent.keys + normPersisted.keys) {
        val r = normRecent[mid]
        val p = normPersisted[mid]
        creatorAffinity[mid] = when {
            r != null && p != null -> r * 0.65 + p * 0.35
            r != null -> r
            else -> p ?: 0.0
        }
    }
    val topicAffinity = normalizePositiveScores(rawTopicScores)

    // 4) UP 主排名
    val creators = creatorAffinity.entries.map { (mid, score) ->
        CreatorRank(
            mid = mid,
            name = creatorNames[mid] ?: "UP主$mid",
            score = score,
            watchCount = maxOf(recentCreatorCounts[mid] ?: 0, persistedCreatorCounts[mid] ?: 0),
        )
    }.sortedWith(compareByDescending<CreatorRank> { it.score }.thenByDescending { it.watchCount })

    // 5) 过滤候选（负反馈硬过滤）
    val eligible = candidateVideos
        .filter { it.bvid.isNotEmpty() && it.title.isNotEmpty() }
        .filter { !penaltySignals.consumedBvids.contains(it.bvid) }
        .filter { !penaltySignals.dislikedBvids.contains(it.bvid) }
        .filter { !penaltySignals.dislikedCreatorMids.contains(it.ownerMid) }

    // 6) 评分
    val weights = strategyWeights(strategy)
    val scored = mutableListOf<ScoredCandidate>()
    for ((i, video) in eligible.withIndex()) {
        val topics = resolveTopicKeysForRcmd(video)
        val creatorScore = creatorAffinity[video.ownerMid] ?: 0.0
        val topicScore = if (topics.isEmpty()) 0.0 else topics.maxOf { topicAffinity[it] ?: 0.0 }

        val interest = (creatorScore * 0.65 + topicScore * 0.35).coerceIn(0.0, 1.0)
        val modeFit = modeFitScore(video, mode, eyeCareNightActive)
        val freshness = continuousFreshnessScore(video.pubdate, now)
        val quality = buildCandidateQualityScore(video)
        val exploration = explorationScore(creatorScore, topicScore, topics)

        val score = (interest * weights.interest + modeFit * weights.mode +
            freshness * weights.freshness + quality * weights.quality +
            exploration * weights.exploration).coerceIn(0.0, 1.0)

        val features = CandidateFeatures(creatorScore, topicScore, interest, modeFit,
            freshness, quality, exploration, topics)
        scored += ScoredCandidate(video, i, score, score, features,
            buildRecommendationExplanation(video, mode, eyeCareNightActive, features))
    }

    // 7) MMR 多样性排序
    val selected = buildDiverseQueue(scored, queueLimit.coerceIn(1, 60), weights.diversity)

    return TodayWatchPlan(
        mode = mode,
        upRanks = creators.take(upRankLimit.coerceIn(1, 20)),
        videoQueue = selected.map { it.video },
        explanationByBvid = selected.associate { it.video.bvid to it.explanation },
        scoreByBvid = selected.associate { it.video.bvid to it.baseScore },
        confidenceByBvid = selected.associate { it.video.bvid to it.confidence },
        historySampleCount = cleanedHistory.size,
        nightSignalUsed = eyeCareNightActive,
    )
}

// ==================== 辅助函数（与 Dart 一一对应） ====================

private fun estimateCompletionRatio(item: WatchHistoryItem): Double {
    if (item.progress < 0) return 0.35
    if (item.duration <= 0) return (item.progress.toDouble() / 600.0).coerceIn(0.0, 1.0)
    return (item.progress.toDouble() / item.duration).coerceIn(0.0, 1.0)
}

private fun watchAffinityScore(completion: Double, recencyBonus: Double): Double {
    val completionScore = when {
        completion >= 0.9 -> 1.85
        completion >= 0.6 -> 0.9 + completion * 0.75
        completion >= 0.3 -> 0.25 + completion * 0.45
        else -> 0.1
    }
    return completionScore + recencyBonus * if (completion >= 0.6) 1.0 else 0.35
}

private fun recencyBonus(viewAt: Long, now: Long): Double {
    if (viewAt <= 0) return 0.25
    val days = ((now - viewAt) / 86400.0).coerceAtLeast(0.0)
    return when {
        days <= 1.0 -> 1.0
        days <= 3.0 -> 0.8
        days <= 7.0 -> 0.6
        days <= 30.0 -> 0.35
        else -> 0.15
    }
}

private fun resolveTopicKeys(item: WatchHistoryItem): Set<String> {
    val keywords = "${item.title} ${item.tagName}".lowercase()
    val topics = mutableSetOf<String>()
    if (item.kid > 0) topics.add("partition-id:${item.kid}")
    for ((topic, kws) in TOPIC_KEYWORDS) {
        if (kws.any { keywords.contains(it) }) topics.add("topic:$topic")
    }
    return topics
}

private fun resolveTopicKeysForRcmd(video: RcmdCandidate): Set<String> {
    val keywords = video.title.lowercase()
    val topics = mutableSetOf<String>()
    for ((topic, kws) in TOPIC_KEYWORDS) {
        if (kws.any { keywords.contains(it) }) topics.add("topic:$topic")
    }
    return topics
}

private fun modeFitScore(video: RcmdCandidate, mode: TodayWatchMode, eyeCareNight: Boolean): Double {
    val title = video.title.lowercase()
    val durationMin = (video.duration / 60.0).coerceAtLeast(0.0)
    val intensity = if (video.viewCount > 0) video.danmakuCount.toDouble() / video.viewCount else 0.0
    val relaxCue = RELAX_KEYWORDS.any { title.contains(it) }
    val learnCue = LEARN_KEYWORDS.any { title.contains(it) }

    val base = if (mode == TodayWatchMode.RELAX) {
        val durationFit = when {
            durationMin < 2.0 -> 0.3; durationMin <= 12.0 -> 1.0
            durationMin <= 20.0 -> 0.75; durationMin <= 35.0 -> 0.45; else -> 0.15
        }
        val calmFit = if (intensity < 0.004) 1.0 else if (intensity < 0.01) 0.65 else 0.2
        (durationFit * 0.45 + calmFit * 0.25 + (if (relaxCue) 0.30 else 0.12) - (if (learnCue) 0.22 else 0.0)).coerceIn(0.0, 1.0)
    } else {
        val durationFit = when {
            durationMin < 5.0 -> 0.2; durationMin < 10.0 -> 0.55
            durationMin <= 35.0 -> 1.0; durationMin <= 55.0 -> 0.7; else -> 0.35
        }
        (durationFit * 0.55 + (if (learnCue) 0.45 else 0.12) - (if (relaxCue && durationMin < 12.0) 0.2 else 0.0)).coerceIn(0.0, 1.0)
    }

    if (eyeCareNight) {
        return (base * 0.75 + nightFriendlyScore(video) * 0.25).coerceIn(0.0, 1.0)
    }
    return base
}

private fun nightFriendlyScore(video: RcmdCandidate): Double {
    val durationMin = (video.duration / 60.0).coerceAtLeast(0.0)
    val intensity = if (video.viewCount > 0) video.danmakuCount.toDouble() / video.viewCount else 0.0
    val durationFit = when {
        durationMin <= 15.0 -> 1.0; durationMin <= 25.0 -> 0.7
        durationMin <= 45.0 -> 0.35; else -> 0.1
    }
    val calmFit = if (intensity < 0.006) 1.0 else if (intensity < 0.012) 0.6 else 0.2
    return durationFit * 0.6 + calmFit * 0.4
}

private fun continuousFreshnessScore(pubdate: Long, nowEpochSec: Long): Double {
    if (pubdate <= 0) return 0.5
    val ageDays = ((nowEpochSec - pubdate).coerceIn(0, Int.MAX_VALUE.toLong()) / 86400.0)
    return 2.0.pow(-ageDays / 30.0).coerceIn(0.0, 1.0)
}

private fun explorationScore(creatorAffinity: Double, topicAffinity: Double, topics: Set<String>): Double {
    val unseenCreator = if (creatorAffinity < 0.05) 1.0 else 0.0
    val unseenTopic = if (topics.isEmpty() || topicAffinity < 0.05) 1.0 else 0.0
    return unseenCreator * 0.6 + unseenTopic * 0.4
}

private fun buildCandidateQualityScore(video: RcmdCandidate): Double {
    val viewLog = ln(1.0 + video.viewCount)
    val engagement = smoothedEngagementRate(video)
    return (viewLog / 10.0).coerceIn(0.0, 1.0) * 0.6 + engagement * 0.4
}

private fun smoothedEngagementRate(video: RcmdCandidate): Double {
    val view = video.viewCount.toDouble().coerceAtLeast(0.0)
    return (video.likeCount.toDouble() / (view + 2000.0)).coerceIn(0.0, 1.0)
}

private fun <K> normalizePositiveScores(scores: Map<K, Double>): Map<K, Double> {
    if (scores.isEmpty()) return emptyMap()
    val max = scores.values.maxOrNull() ?: 0.0
    if (max <= 0) return emptyMap()
    return scores.mapValues { (it.value / max).coerceIn(0.0, 1.0) }
}

private fun strategyWeights(strategy: TodayWatchStrategy): StrategyWeights = when (strategy) {
    TodayWatchStrategy.BALANCED -> StrategyWeights(0.34, 0.20, 0.16, 0.15, 0.15, 0.18)
    TodayWatchStrategy.AFFINITY -> StrategyWeights(0.52, 0.18, 0.10, 0.15, 0.05, 0.08)
    TodayWatchStrategy.EXPLORE -> StrategyWeights(0.20, 0.18, 0.22, 0.15, 0.25, 0.30)
}

private fun buildDiverseQueue(candidates: List<ScoredCandidate>, queueLimit: Int, diversityStrength: Double): List<ScoredCandidate> {
    if (candidates.isEmpty()) return emptyList()
    val remaining = candidates.toMutableList()
    val selected = mutableListOf<ScoredCandidate>()
    val creatorCounts = mutableMapOf<Long, Int>()
    val topicCounts = mutableMapOf<String, Int>()
    val topPreviewLimit = 6
    val topPreviewRepeatLimit = 2

    while (selected.size < queueLimit && remaining.isNotEmpty()) {
        var pool = if (selected.size < topPreviewLimit) {
            remaining.filter { c ->
                val creatorOk = (creatorCounts[c.video.ownerMid] ?: 0) < topPreviewRepeatLimit
                val topic = c.features.topics.firstOrNull()
                val topicOk = topic == null || (topicCounts[topic] ?: 0) < topPreviewRepeatLimit
                creatorOk && topicOk
            }
        } else remaining
        if (pool.isEmpty()) pool = remaining

        var picked = pool.first()
        var bestScore = Double.NEGATIVE_INFINITY
        for (c in pool) {
            val s = c.baseScore - diversityStrength * maximumSimilarity(c, selected)
            if (s > bestScore) { bestScore = s; picked = c }
        }

        val adjusted = (picked.baseScore - diversityStrength * maximumSimilarity(picked, selected)).coerceIn(0.0, 1.0)
        selected += picked.copy(baseScore = adjusted, confidence = adjusted)
        remaining.remove(picked)

        creatorCounts[picked.video.ownerMid] = (creatorCounts[picked.video.ownerMid] ?: 0) + 1
        picked.features.topics.firstOrNull()?.let { topicCounts[it] = (topicCounts[it] ?: 0) + 1 }
    }
    return selected
}

private fun maximumSimilarity(candidate: ScoredCandidate, selected: List<ScoredCandidate>): Double {
    if (selected.isEmpty()) return 0.0
    var maxSim = 0.0
    for (e in selected) {
        val sameCreator = candidate.video.ownerMid != 0L && candidate.video.ownerMid == e.video.ownerMid
        val topicOverlap = candidate.features.topics.isNotEmpty() && e.features.topics.isNotEmpty() &&
            candidate.features.topics.any { e.features.topics.contains(it) }
        val sim = (if (sameCreator) 0.6 else 0.0) + (if (topicOverlap) 0.4 else 0.0)
        if (sim > maxSim) maxSim = sim
    }
    return maxSim
}

private fun buildRecommendationExplanation(
    video: RcmdCandidate, mode: TodayWatchMode, eyeCareNight: Boolean, f: CandidateFeatures,
): String {
    val reasons = mutableListOf<String>()
    if (f.modeFit >= 0.7) reasons.add(if (mode == TodayWatchMode.RELAX) "轻松向" else "学习向")
    if (f.freshness >= 0.75) reasons.add("近期更新")
    if (eyeCareNight && nightFriendlyScore(video) >= 0.7) reasons.add("夜间友好")
    if (f.creatorAffinity >= 0.45) reasons.add("常看UP")
    if (f.topicAffinity >= 0.45) reasons.add("常看分区")
    if (f.exploration >= 0.8) reasons.add("新UP探索")
    if (f.quality >= 0.75) reasons.add("优质内容")
    if (reasons.isEmpty()) reasons.add(if (mode == TodayWatchMode.RELAX) "轻松向" else "学习向")
    return reasons.distinct().take(3).joinToString(" · ")
}
