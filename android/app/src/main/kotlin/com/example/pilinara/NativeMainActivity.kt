package com.example.pilinara

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.pilinara.piliplus.ExoPlayerPlugin

/**
 * Pure native Android Activity using Jetpack Compose UI.
 * Gradually replacing Flutter for native performance.
 */
class NativeMainActivity : ComponentActivity() {
    
    private var exoPlayer: ExoPlayer? = null
    private var playerView: PlayerView? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Keep screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NativeAppContent()
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        exoPlayer?.play()
    }
    
    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Handle configuration changes
    }
}

@Composable
fun NativeAppContent() {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "PiliNara Native\n(Jetpack Compose)",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

/**
 * Flutter Activity - maintains Flutter for gradual migration
 */
class FlutterMainActivity : io.flutter.embedding.android.FlutterActivity() {
    
    override fun configureFlutterEngine(flutterEngine: io.flutter.embedding.engine.FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        // Register native plugins
        ExoPlayerPlugin.register(this, flutterEngine)
        
        // Setup method channel
        io.flutter.plugin.common.MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            "PiliNara"
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "openNativeVideoPlayer" -> {
                    val videoUrl = call.argument<String>("videoUrl") ?: ""
                    val headers = call.argument<Map<String, String>>("headers") ?: emptyMap()
                    openNativeVideoPlayer(videoUrl, headers)
                    result.success(null)
                }
                else -> result.notImplemented()
            }
        }
    }
    
    private fun openNativeVideoPlayer(videoUrl: String, headers: Map<String, String>) {
        val intent = VideoPlayerActivity.newInstance(this, videoUrl, headers)
        startActivity(intent)
    }
}
