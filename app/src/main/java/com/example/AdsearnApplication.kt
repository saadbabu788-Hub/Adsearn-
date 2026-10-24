package com.example

import android.app.Application
import android.os.Build
import android.util.Log
import android.webkit.WebView
import java.io.File

class AdsearnApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initWebViewDirectories()
        com.example.notification.DailySpinNotificationHelper.createNotificationChannel(this)
        com.example.notification.DailyNotificationScheduler.createNotificationChannel(this)
        com.example.notification.DailyNotificationScheduler.rescheduleAll(this)
    }

    private fun initWebViewDirectories() {
        try {
            // Configure process suffix for multi-process WebView isolation
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val currentProcess = getProcessName()
                if (packageName != currentProcess) {
                    WebView.setDataDirectorySuffix(currentProcess)
                }
            }

            // Pre-create WebView cache hierarchies to ensure accessibility for Chromium simple cache backend
            val webViewDirNames = listOf(
                "WebView",
                "WebView/Default",
                "WebView/Default/HTTP Cache",
                "WebView/Default/HTTP Cache/Code Cache",
                "WebView/Default/HTTP Cache/Code Cache/js",
                "WebView/Default/HTTP Cache/Code Cache/wasm"
            )

            for (dirRelPath in webViewDirNames) {
                val targetDir = File(cacheDir, dirRelPath)
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }
                targetDir.setReadable(true, false)
                targetDir.setWritable(true, false)
                targetDir.setExecutable(true, false)
            }

            // Clean up any zero-length or stale variations seed files
            val appWebviewDir = File(dataDir, "app_webview")
            if (appWebviewDir.exists()) {
                listOf("variations_seed", "variations_seed_new", "variations_stamp").forEach { name ->
                    val seedFile = File(appWebviewDir, name)
                    if (seedFile.exists() && seedFile.length() == 0L) {
                        seedFile.delete()
                    }
                }
            }
        } catch (t: Throwable) {
            Log.w("AdsearnApplication", "WebView cache setup notice: ${t.message}")
        }
    }
}
