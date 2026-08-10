package com.ailenezareti.monitor

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    private fun baseUrl() = BuildConfig.SERVER_BASE_URL

    /**
     * Serverə data göndərir: type = "location" | "usage" | "call" | "new_app" | "heartbeat"
     * Uğurlu olarsa true qaytarır.
     */
    fun ingest(context: android.content.Context, type: String, payload: JSONObject): Boolean {
        val token = Prefs.deviceToken(context)
        if (token.isBlank()) return false

        val body = JSONObject().apply {
            put("device_token", token)
            put("type", type)
            put("payload", payload)
        }

        val request = Request.Builder()
            .url("${baseUrl()}/ingest.php")
            .post(body.toString().toRequestBody(JSON))
            .build()

        return try {
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    /** Serverdən bloklama siyahısı və geofence-ləri alır. Xəta olarsa null qaytarır. */
    fun fetchDeviceConfig(context: android.content.Context): JSONObject? {
        val token = Prefs.deviceToken(context)
        if (token.isBlank()) return null

        val request = Request.Builder()
            .url("${baseUrl()}/device_config.php?device_token=$token")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val text = resp.body?.string() ?: return null
                JSONObject(text)
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Tokenin serverdə etibarlı olduğunu yoxlayır (ilk cütləmə zamanı) */
    fun verifyToken(context: android.content.Context, token: String): Boolean {
        val request = Request.Builder()
            .url("${baseUrl()}/device_config.php?device_token=$token")
            .get()
            .build()
        return try {
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }
}
