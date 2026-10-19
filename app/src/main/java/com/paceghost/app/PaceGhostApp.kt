package com.paceghost.app

import android.app.Application
import org.osmdroid.config.Configuration

class PaceGhostApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize OsmDroid user agent configuration
        Configuration.getInstance().userAgentValue = packageName
    }
}
