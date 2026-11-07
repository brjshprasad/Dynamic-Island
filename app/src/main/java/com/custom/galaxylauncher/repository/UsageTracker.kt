package com.custom.galaxylauncher.repository

import android.content.Context
import android.content.SharedPreferences
import com.custom.galaxylauncher.model.AppInfo

class UsageTracker(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_usage_prefs", Context.MODE_PRIVATE)

    fun recordAppLaunch(packageName: String) {
        val count = prefs.getInt(packageName, 0)
        prefs.edit().putInt(packageName, count + 1).apply()
    }

    fun getFrequentlyUsedApps(allApps: List<AppInfo>, count: Int = 8): List<AppInfo> {
        val scoredApps = allApps.map { app ->
            val launchCount = prefs.getInt(app.packageName, 0)
            app to launchCount
        }

        // If user hasn't launched many apps yet, prioritize common daily apps like Phone, Browser, Camera
        val sorted = scoredApps.sortedWith(
            compareByDescending<Pair<AppInfo, Int>> { it.second }
                .thenBy { it.first.label.lowercase() }
        ).map { it.first }

        return sorted.take(count)
    }
}
