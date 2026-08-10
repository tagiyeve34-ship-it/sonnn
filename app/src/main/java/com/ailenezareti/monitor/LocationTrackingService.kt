package com.ailenezareti.monitor

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class LocationTrackingService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var callback: LocationCallback
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val CHANNEL_ID = "ailenezareti_status"
        const val NOTIF_ID = 1

        // Ağıllı GPS: telefon hərəkət etmirsə sorğu göndərilmir, batareyaya qənaət olunur.
        // Yalnız cihaz bu məsafədən çox yerini dəyişəndə yeni lokasiya göndərilir.
        const val MOVEMENT_THRESHOLD_METERS = 100f
        // Sistem bu tezlikdən çox tez-tez yoxlamasın (amma yalnız hərəkət olduqda göndərəcək)
        const val CHECK_INTERVAL_MS = 3 * 60_000L // 3 dəqiqə
        const val MIN_UPDATE_INTERVAL_MS = 60_000L // 1 dəqiqə
    }

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        startForegroundWithNotification()
        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // sistem servisi öldürsə, yenidən başlatsın
    }

    private fun startForegroundWithNotification() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Təhlükəsizlik Statusu", NotificationManager.IMPORTANCE_LOW
        )
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_body))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun startLocationUpdates() {
        // setMinUpdateDistanceMeters — sistem/GPS çipi özü idarə edir: cihaz həmin məsafədən
        // az yer dəyişibsə heç bir hadisə yaranmır, callback belə çağrılmır (batareya qənaəti).
        // Cihaz MOVEMENT_THRESHOLD_METERS-dən çox yer dəyişən kimi dərhal yeni lokasiya gəlir.
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CHECK_INTERVAL_MS)
            .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)
            .setMinUpdateDistanceMeters(MOVEMENT_THRESHOLD_METERS)
            .build()

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { sendLocation(it) }
            }
        }

        try {
            fusedClient.requestLocationUpdates(request, callback, mainLooper)
        } catch (e: SecurityException) {
            // İcazə yoxdursa servis heç nə edə bilməz; MainActivity icazəni yenidən istəyəcək
        }
    }

    private fun sendLocation(location: Location) {
        scope.launch {
            val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
            val battery = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

            val payload = JSONObject().apply {
                put("lat", location.latitude)
                put("lng", location.longitude)
                put("accuracy", location.accuracy)
                put("battery", battery)
                put("recorded_at", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
            }
            ApiClient.ingest(applicationContext, "location", payload)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedClient.removeLocationUpdates(callback)
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
