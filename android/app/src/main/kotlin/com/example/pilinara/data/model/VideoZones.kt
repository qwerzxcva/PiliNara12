package com.example.pilinara.data.model

import kotlinx.serialization.Serializable

/** 批次L32：视频分区树（静态，与 web 端主分区对齐；避免再依赖接口） */
data class Zone(
    val name: String,
    val rid: Int
)

val VIDEO_ZONES = listOf(
    Zone("动画", 1), Zone("游戏", 4), Zone("鬼畜", 13),
    Zone("音乐", 3), Zone("舞蹈", 129), Zone("影视", 181),
    Zone("娱乐", 5), Zone("知识", 36), Zone("科技", 188),
    Zone("数码", 188), Zone("美食", 211), Zone("汽车", 223),
    Zone("时尚", 155), Zone("运动", 234), Zone("动物", 217),
    Zone("vlog", 138), Zone("绘画", 24), Zone("人工智能", 122),
    Zone("广告", 166), Zone("公开课", 39)
)
