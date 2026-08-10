package com.ailenezareti.monitor

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CallLog
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        syncCallLog()
        // Cihaz aktivdirsə, hərəkət olmasa belə "last_seen"-i təzələmək üçün yüngül siqnal
        ApiClient.ingest(applicationContext, "heartbeat", JSONObject())
        return Result.success()
    }

    // ---------- Zəng metadatası (məzmun deyil) ----------
    private fun syncCallLog() {
        if (applicationContext.checkSelfPermission(android.Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED) return

        val lastSync = Prefs.lastCallSyncTime(applicationContext)
        val uri = CallLog.Calls.CONTENT_URI
        val projection = arrayOf(
            CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE, CallLog.Calls.DURATION, CallLog.Calls.DATE
        )
        val selection = "${CallLog.Calls.DATE} > ?"
        val cursor: Cursor? = applicationContext.contentResolver.query(
            uri, projection, selection, arrayOf(lastSync.toString()), "${CallLog.Calls.DATE} ASC"
        )

        cursor?.use {
            var maxDate = lastSync
            val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            while (it.moveToNext()) {
                val number = it.getString(0) ?: ""
                val name = it.getString(1)
                val type = when (it.getInt(2)) {
                    CallLog.Calls.OUTGOING_TYPE -> "outgoing"
                    CallLog.Calls.MISSED_TYPE -> "missed"
                    else -> "incoming"
                }
                val duration = it.getInt(3)
                val date = it.getLong(4)
                if (date > maxDate) maxDate = date

                val payload = JSONObject().apply {
                    put("number", number)
                    if (name != null) put("contact_name", name)
                    put("call_type", type)
                    put("duration_sec", duration)
                    put("occurred_at", df.format(Date(date)))
                }
                ApiClient.ingest(applicationContext, "call", payload)
            }
            Prefs.setLastCallSyncTime(applicationContext, maxDate)
        }
    }

    companion object {
        const val WORK_NAME = "ailenezareti_sync"
        const val INTERVAL_MINUTES = 15L

        fun schedule(context: Context) {
            val request = androidx.work.PeriodicWorkRequestBuilder<SyncWorker>(
                INTERVAL_MINUTES, TimeUnit.MINUTES
            ).build()

            androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
