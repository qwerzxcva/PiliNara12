package com.example.pilinara.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.pilinara.R
import com.example.pilinara.ui.MainActivity
import com.ryanheise.audioservice.AudioService
import com.ryanheise.audioservice.AudioServiceBackground

/**
 * Audio Service for background playback
 * Replaces Flutter audio_service
 */
class PiliAudioService : AudioService() {
    
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSessionCompat
        private const val CHANNEL_ID = "pilinara_audio"
    private const val NOTIFICATION_ID = 1001
    
    override fun onCreate() {
        super.onCreate()
        
        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PiliNara Audio",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        
        // Initialize player
        player = ExoPlayer.Builder(applicationContext)
            .setHandleAudioBecomingNoisy(true)
            .build()
        
        // Initialize media session
        mediaSession = MediaSessionCompat(applicationContext, "PiliAudioService")
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                player.play()
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
            }
            
            override fun onPause() {
                player.pause()
                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
            }
            
            override fun onSkipToPrevious() {
                // TODO: Skip to previous
            }
            
            override fun onSkipToNext() {
                // TODO: Skip to next
            }
            
            override fun onSeekTo(pos: Long) {
                player.seekTo(pos)
            }
        })
        
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or 
                             MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        
        player.playWhenReady = true
        player.prepare()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }
    
    override fun onDestroy() {
        player.release()
        mediaSession.release()
        super.onDestroy()
    }
    
    private fun updatePlaybackState(state: Int) {
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(state, player.currentPosition, 1.0f)
                .build()
        )
    }
    
    private fun showNotification(mediaItem: MediaItem) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = android.app.Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("PiliNara")
            .setContentText(mediaItem.mediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE) ?: "")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(android.app.Notification.PRIORITY_LOW)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
    }
}
