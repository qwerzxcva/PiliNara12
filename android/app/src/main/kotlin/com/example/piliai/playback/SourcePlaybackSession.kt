package com.example.piliai.playback

import androidx.lifecycle.ViewModel
import java.util.UUID

/** Navigation carries only an opaque ID; source credentials remain in memory. */
class SourcePlaybackSession : ViewModel() {
    private val requests = mutableMapOf<String, SourcePlaybackRequest>()

    @Synchronized
    fun register(request: SourcePlaybackRequest): String {
        val id = UUID.randomUUID().toString()
        requests[id] = request
        return id
    }

    @Synchronized
    fun request(id: String): SourcePlaybackRequest? = requests[id]

    @Synchronized
    fun remove(id: String) {
        requests.remove(id)
    }

    @Synchronized
    override fun onCleared() {
        requests.clear()
    }
}

/** Retained across rotation, cleared when its navigation entry is removed. */
class SourcePlaybackEntryOwner(
    private val session: SourcePlaybackSession,
    private val requestId: String
) : ViewModel() {
    override fun onCleared() {
        session.remove(requestId)
        super.onCleared()
    }
}
