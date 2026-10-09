package com.example.piliai.playback

import java.net.URI

class SourcePlaybackRequest(
    val videoUrl: String,
    val sourceId: Long,
    val sourceName: String,
    val episodeUrl: String,
    val referer: String = "",
    val userAgent: String = "",
    private val cookies: String = ""
) {
    init {
        requireHttpUrl(videoUrl)
        requireHttpUrl(episodeUrl)
        require(sourceId > 0L)
        require(sourceName.isNotBlank())
        for (value in listOf(referer, userAgent, cookies)) {
            require('\r' !in value && '\n' !in value) {
                "Invalid playback request header"
            }
        }
    }

    fun headersFor(destinationUrl: String): Map<String, String> {
        val destination = requireHttpUrl(destinationUrl)
        val episode = requireHttpUrl(episodeUrl)
        return buildMap {
            if (userAgent.isNotBlank()) put("User-Agent", userAgent)
            if (referer.isNotBlank()) put("Referer", referer)
            // Source cookies belong to the episode origin, not an arbitrary CDN.
            if (cookies.isNotBlank() && sameOrigin(episode, destination)) {
                put("Cookie", cookies)
            }
        }
    }

    override fun toString(): String =
        "SourcePlaybackRequest(sourceId=$sourceId, headers=redacted)"

    private fun requireHttpUrl(value: String): URI {
        val uri = URI(value)
        require(uri.scheme.equals("https", ignoreCase = true) ||
            uri.scheme.equals("http", ignoreCase = true)) {
            "Playback requests require HTTP or HTTPS"
        }
        require(!uri.host.isNullOrBlank() && uri.userInfo == null) {
            "Playback URL must have a host and no embedded credentials"
        }
        return uri
    }

    private fun sameOrigin(first: URI, second: URI): Boolean =
        first.scheme.equals(second.scheme, ignoreCase = true) &&
            first.host.equals(second.host, ignoreCase = true) &&
            effectivePort(first) == effectivePort(second)

    private fun effectivePort(uri: URI): Int = when {
        uri.port >= 0 -> uri.port
        uri.scheme.equals("https", ignoreCase = true) -> 443
        else -> 80
    }
}
