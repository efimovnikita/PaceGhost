package com.paceghost.app

import android.app.Application
import com.paceghost.app.util.AppLogger
import org.osmdroid.config.Configuration

class PaceGhostApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        // Initialize OsmDroid user agent configuration
        Configuration.getInstance().userAgentValue = packageName
    }
}
