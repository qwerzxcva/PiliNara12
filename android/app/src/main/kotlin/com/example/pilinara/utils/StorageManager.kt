package com.example.pilinara.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * App-wide storage manager
 * Replaces Flutter Hive CE storage
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class StorageManager(private val context: Context) {
    
    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("themeMode")
        private val VIDEO_QUALITY_KEY = stringPreferencesKey("defaultVideoQa")
        private val AUTO_PLAY_KEY = booleanPreferencesKey("autoPlayEnable")
        private val FULLSCREEN_MODE_KEY = stringPreferencesKey("fullScreenMode")
        private val UI_SCALE_KEY = floatPreferencesKey("uiScale")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val DARK_MODE_KEY = booleanPreferencesKey("darkMode")
        private val NOTIFICATION_ENABLED_KEY = booleanPreferencesKey("notificationEnabled")
        
        // Video settings
        private val PLAYBACK_SPEED_KEY = floatPreferencesKey("playbackSpeed")
        private val VOLUME_KEY = floatPreferencesKey("volume")
        private val SUBTITLE_ENABLED_KEY = booleanPreferencesKey("subtitleEnabled")
        private val SUBTITLE_FONT_SIZE_KEY = intPreferencesKey("subtitleFontSize")
        
        // Danmaku settings
        private val DANMAKU_ENABLED_KEY = booleanPreferencesKey("enableShowDanmaku")
        private val DANMAKU_OPACITY_KEY = floatPreferencesKey("danmakuOpacity")
        private val DANMAKU_FONT_SIZE_KEY = intPreferencesKey("danmakuFontSize")
        private val DANMAKU_SPEED_KEY = floatPreferencesKey("danmakuSpeed")
        
        // Account settings
        private val IS_LOGGED_IN_KEY = booleanPreferencesKey("isLoggedIn")
        private val USER_MID_KEY = longPreferencesKey("userMid")
        private val USER_NAME_KEY = stringPreferencesKey("userName")
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("accessToken")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refreshToken")
    }
    
    // Theme
    val themeModeFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[THEME_MODE_KEY] ?: "system" }
    
    val videoQualityFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[VIDEO_QUALITY_KEY] ?: "auto" }
    
    val autoPlayFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[AUTO_PLAY_KEY] ?: false }
    
    // Video playback
    val playbackSpeedFlow: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[PLAYBACK_SPEED_KEY] ?: 1.0f }
    
    val volumeFlow: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[VOLUME_KEY] ?: 1.0f }
    
    val subtitleEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[SUBTITLE_ENABLED_KEY] ?: true }
    
    // Danmaku
    val danmakuEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[DANMAKU_ENABLED_KEY] ?: true }
    
    val danmakuOpacityFlow: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[DANMAKU_OPACITY_KEY] ?: 1.0f }
    
    // Account
    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[IS_LOGGED_IN_KEY] ?: false }
    
    val userMidFlow: Flow<Long> = context.dataStore.data
        .map { preferences -> preferences[USER_MID_KEY] ?: 0L }
    
    val userNameFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[USER_NAME_KEY] ?: "" }
    
    // Methods
    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode
        }
    }
    
    suspend fun setVideoQuality(quality: String) {
        context.dataStore.edit { preferences ->
            preferences[VIDEO_QUALITY_KEY] = quality
        }
    }
    
    suspend fun setAutoPlay(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_PLAY_KEY] = enabled
        }
    }
    
    suspend fun setPlaybackSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[PLAYBACK_SPEED_KEY] = speed
        }
    }
    
    suspend fun setVolume(volume: Float) {
        context.dataStore.edit { preferences ->
            preferences[VOLUME_KEY] = volume
        }
    }
    
    suspend fun setSubtitleEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SUBTITLE_ENABLED_KEY] = enabled
        }
    }
    
    suspend fun setDanmakuEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DANMAKU_ENABLED_KEY] = enabled
        }
    }
    
    suspend fun setDanmakuOpacity(opacity: Float) {
        context.dataStore.edit { preferences ->
            preferences[DANMAKU_OPACITY_KEY] = opacity
        }
    }
    
    suspend fun login(mid: Long, name: String, accessToken: String, refreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN_KEY] = true
            preferences[USER_MID_KEY] = mid
            preferences[USER_NAME_KEY] = name
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[REFRESH_TOKEN_KEY] = refreshToken
        }
    }
    
    suspend fun logout() {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN_KEY] = false
            preferences[USER_MID_KEY] = 0L
            preferences[USER_NAME_KEY] = ""
            preferences[ACCESS_TOKEN_KEY] = ""
            preferences[REFRESH_TOKEN_KEY] = ""
        }
    }
}
