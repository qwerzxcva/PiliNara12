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
import com.example.pilinara.piliplus.ExoPlayerPlugin

/**
 * Pure native Android Activity using Jetpack Compose UI.
 * This replaces Flutter for better performance and native integration.
 */
class NativeMainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Keep screen on during playback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NativeApp()
                }
            }
        }
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Handle foldable devices and orientation changes
    }
}

@Composable
fun NativeApp() {
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
 * Flutter Activity - maintained for gradual migration
 * Will be removed once all features are ported to native
 */
class FlutterMainActivity : io.flutter.embedding.android.FlutterActivity() {
    
    override fun configureFlutterEngine(flutterEngine: io.flutter.embedding.engine.FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        // Register native plugins
        ExoPlayerPlugin.register(this, flutterEngine)
        
        // Setup method channel for native communication
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
