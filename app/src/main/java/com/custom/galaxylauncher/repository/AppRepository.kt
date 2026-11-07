package com.custom.galaxylauncher.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.custom.galaxylauncher.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        val apps = mutableListOf<AppInfo>()

        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            // Don't show our own launcher inside the app drawer
            if (pkg == context.packageName) continue

            val label = info.loadLabel(pm).toString()
            val icon = try {
                info.loadIcon(pm)
            } catch (e: Exception) {
                null
            }

            apps.add(
                AppInfo(
                    label = label,
                    packageName = pkg,
                    activityName = info.activityInfo.name,
                    icon = icon
                )
            )
        }

        apps.sortedBy { it.label.lowercase() }
    }

    fun launchApp(app: AppInfo): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
