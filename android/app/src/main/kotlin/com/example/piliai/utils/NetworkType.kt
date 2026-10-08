package com.example.piliai.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * 审核221：网络类型判断（Flutter defaultVideoQaCellular 依赖）。
 * WiFi/以太网 → false；蜂窝数据 → true；未知 → false（按最优体验）。
 */
object NetworkType {

    fun isCellular(context: Context): Boolean = runCatching {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }.getOrDefault(false)
}
