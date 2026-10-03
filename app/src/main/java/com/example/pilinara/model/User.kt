package com.example.pilinara.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val mid: Long = 0,
    val uname: String = "",
    val face: String = "",
    val sign: String = "",
    val levelInfo: LevelInfo = LevelInfo(),
    val official: Official = Official(),
    val vip: Vip = Vip(),
    val fans: Int = 0,
    val friend: Int = 0,
    val attention: Int = 0,
    val coins: Long = 0,
    val following: Boolean = false,
    val follower: Boolean = false
) {
    @Serializable
    data class LevelInfo(
        val currentLevel: Int = 0,
        val currentExp: Long = 0,
        val nextExp: Long = 0
    )
    
    @Serializable
    data class Official(
        val role: Int = 0,
        val title: String = "",
        val desc: String = ""
    )
    
    @Serializable
    data class Vip(
        val vipType: Int = 0,
        val vipStatus: Int = 0,
        val themeType: Int = 0,
        val label: Label = Label()
    ) {
        @Serializable
        data class Label(
            val path: String = "",
            val text: String = "",
            val textColor: String = "",
            val bgColor: String = ""
        )
    }
}
