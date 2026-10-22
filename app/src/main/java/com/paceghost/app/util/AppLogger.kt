package com.paceghost.app.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

object AppLogger {

    private const val LOG_DIR_NAME = "logs"
    private const val CURRENT_LOG_FILE = "paceghost.log"
    private const val BACKUP_LOG_FILE = "paceghost.log.1"
    private const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024L // 5 MB

    private val writeExecutor = Executors.newSingleThreadExecutor()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private var logDir: File? = null
    private var isCrashHandlerInstalled = false

    fun init(context: Context) {
        synchronized(this) {
            if (logDir == null) {
                logDir = File(context.applicationContext.filesDir, LOG_DIR_NAME).apply {
                    if (!exists()) mkdirs()
                }
            }
            if (!isCrashHandlerInstalled) {
                setupCrashHandler()
                isCrashHandlerInstalled = true
            }
        }
        i("AppLogger", "AppLogger initialized. OS: Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT}), Model: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
    }

    private fun setupCrashHandler() {
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("AppLogger", "Uncaught exception on thread ${thread.name}", throwable)
                writeLogEntryDirect("FATAL", "CrashHandler", "Uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)
            } catch (t: Throwable) {
                Log.e("AppLogger", "Failed to log crash synchronously", t)
            } finally {
                originalHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        writeLogEntry("DEBUG", tag, message, null)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        writeLogEntry("INFO", tag, message, null)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
        writeLogEntry("WARN", tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        writeLogEntry("ERROR", tag, message, throwable)
    }

    fun getLogFile(context: Context): File? {
        init(context)
        val file = File(logDir, CURRENT_LOG_FILE)
        return if (file.exists() && file.length() > 0) file else null
    }

    fun clearLogs(context: Context) {
        init(context)
        writeExecutor.execute {
            try {
                val current = File(logDir, CURRENT_LOG_FILE)
                if (current.exists()) current.delete()
                val backup = File(logDir, BACKUP_LOG_FILE)
                if (backup.exists()) backup.delete()
            } catch (e: Exception) {
                Log.e("AppLogger", "Failed to clear logs", e)
            }
        }
    }

    private fun writeLogEntry(level: String, tag: String, message: String, throwable: Throwable?) {
        val timestamp = synchronized(dateFormat) { dateFormat.format(Date()) }
        val threadName = Thread.currentThread().name

        writeExecutor.execute {
            writeLogEntryDirect(level, tag, message, throwable)
        }
    }

    private fun writeLogEntryDirect(level: String, tag: String, message: String, throwable: Throwable?) {
        val timestamp = synchronized(dateFormat) { dateFormat.format(Date()) }
        val threadName = Thread.currentThread().name
        val dir = logDir ?: return
        try {
            val currentFile = File(dir, CURRENT_LOG_FILE)
            checkRotation(currentFile, dir)

            FileWriter(currentFile, true).use { writer ->
                val stackTrace = throwable?.let {
                    val sw = StringWriter()
                    it.printStackTrace(PrintWriter(sw))
                    "\n" + sw.toString()
                } ?: ""
                writer.write("[$timestamp][$level][$threadName][$tag] $message$stackTrace\n")
            }
        } catch (e: Exception) {
            Log.e("AppLogger", "Failed to write log entry", e)
        }
    }

    private fun checkRotation(file: File, dir: File) {
        if (file.exists() && file.length() >= MAX_FILE_SIZE_BYTES) {
            val backupFile = File(dir, BACKUP_LOG_FILE)
            if (backupFile.exists()) {
                backupFile.delete()
            }
            file.renameTo(backupFile)
        }
    }
}
