package com.ailenezareti.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class PackageAddedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_PACKAGE_ADDED) return
        val packageName = intent.data?.schemeSpecificPart ?: return
        if (packageName == context.packageName) return // özü haqqında bildiriş göndərməsin

        val appName = try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
            packageName
        }

        CoroutineScope(Dispatchers.IO).launch {
            val payload = JSONObject().apply {
                put("package", packageName)
                put("name", appName)
            }
            ApiClient.ingest(context, "new_app", payload)
        }
    }
}
