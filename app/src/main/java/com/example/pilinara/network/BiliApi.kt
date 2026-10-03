package com.example.pilinara.network

object BiliApi {
    // API 基础地址
    const val BASE_URL = "https://api.bilibili.com"
    const val SEARCH_URL = "https://search.bilibili.com"
    const val LIVE_URL = "https://api.live.bilibili.com"
    
    // 接口路径
    const val VIDEO_SEARCH = "/x/web-interface/search/type"
    const val VIDEO_INFO = "/x/web-interface/view"
    const val VIDEO_DANMAKU = "/x/v1/dm/list.so"
    const val USER_INFO = "/x/space/wbi/acc/info"
    const val USER_SPACE = "/x/space/wbi/arc/search"
    const val VIDEO_PLAYINFO = "/x/player/playurl"
    const val VIDEO_COMMENT = "/x/v2/reply"
    const val VIDEO_SUBTITLE = "/x/copyright/subtitle/list"
    
    // 登录相关
    const val QRLOGIN_QRCODE = "/passport/qrcode/create"
    const val QRLOGIN_POLL = "/passport/qrcode/query"
    const val LOGIN_WITH_COOKIE = "/passport/login/svip"
}
