package com.example.piliai.utils

/**
 * 审核17：B站 API 返回的图源（pic/face/cover）多为 http://，
 * Android 9+ 默认禁明文请求 → 图片全部加载失败。
 * 统一在 URL 入口重写为 https（hdslb/akamaized 域名均支持 https）。
 * 接收 String? 以便直接用于可空 model 表达式。
 */
fun String?.toHttpsUrl(): String =
    if (this != null && startsWith("http://")) "https://" + substring(7) else this ?: ""
