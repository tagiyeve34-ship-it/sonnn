package com.ailenezareti.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (Prefs.isPaired(context)) {
                ContextCompat.startForegroundService(context, Intent(context, LocationTrackingService::class.java))
                SyncWorker.schedule(context)
            }
        }
    }
}
