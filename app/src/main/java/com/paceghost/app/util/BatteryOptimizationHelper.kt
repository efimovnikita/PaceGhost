package com.paceghost.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

object BatteryOptimizationHelper {

    private const val PREFS_NAME = "paceghost_prefs"
    private const val KEY_DONT_ASK_BATTERY = "pref_dont_ask_battery_optimization"

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        val isIgnoring = powerManager.isIgnoringBatteryOptimizations(context.packageName)
        AppLogger.i("BatteryOpt", "isIgnoringBatteryOptimizations: $isIgnoring")
        return isIgnoring
    }

    fun shouldPromptBatteryOptimization(context: Context): Boolean {
        if (isIgnoringBatteryOptimizations(context)) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dontAsk = prefs.getBoolean(KEY_DONT_ASK_BATTERY, false)
        return !dontAsk
    }

    fun setDontAskAgain(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DONT_ASK_BATTERY, true).apply()
        AppLogger.i("BatteryOpt", "User selected 'Don't ask again' for battery optimization")
    }

    fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            AppLogger.w("BatteryOpt", "Failed to open ignore battery optimization settings, opening app details", e)
            try {
                val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                AppLogger.e("BatteryOpt", "Failed to open app details settings", e2)
            }
        }
    }
}
