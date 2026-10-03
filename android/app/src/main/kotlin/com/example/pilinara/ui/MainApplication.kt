package com.example.pilinara

import android.app.Application
import com.example.pilinara.database.DatabaseInitializer

/**
 * Main Application class
 * Initializes all native and database components
 */
class MainApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize database
        DatabaseInitializer().initialize(this)
        
        // Load native libraries
        try {
            System.loadLibrary("pilinara_native")
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.e("PiliNaraApp", "Failed to load native library: ${e.message}")
        }
    }
}
