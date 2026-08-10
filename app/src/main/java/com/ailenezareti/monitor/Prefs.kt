package com.ailenezareti.monitor

import android.content.Context

object Prefs {
    private const val FILE = "ailenezareti_prefs"
    private const val KEY_TOKEN = "device_token"
    private const val KEY_PAIRED = "paired"
    private const val KEY_CHILD_NAME = "child_name"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // Build zamanı bişirilmiş token varsa onu, yoxdursa manual saxlanılanı qaytarır
    fun deviceToken(ctx: Context): String {
        val baked = BuildConfig.SERVER_DEVICE_TOKEN
        if (baked.isNotBlank() && baked != "BURAYA_DASHBOARDDAN_ALDIGINIZ_KODU_YAZIN") {
            return baked
        }
        return prefs(ctx).getString(KEY_TOKEN, "") ?: ""
    }

    fun setManualToken(ctx: Context, token: String) {
        prefs(ctx).edit().putString(KEY_TOKEN, token).apply()
    }

    fun isPaired(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_PAIRED, false)

    fun setPaired(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_PAIRED, value).apply()
    }

    fun childName(ctx: Context): String = prefs(ctx).getString(KEY_CHILD_NAME, "") ?: ""

    fun setChildName(ctx: Context, name: String) {
        prefs(ctx).edit().putString(KEY_CHILD_NAME, name).apply()
    }

    // Zəng qeydi sinxronizasiyasının harada dayandığını izləyir (təkrar göndərməsin deyə)
    private const val KEY_LAST_CALL_SYNC = "last_call_sync"
    fun lastCallSyncTime(ctx: Context): Long = prefs(ctx).getLong(KEY_LAST_CALL_SYNC, 0L)
    fun setLastCallSyncTime(ctx: Context, ts: Long) {
        prefs(ctx).edit().putLong(KEY_LAST_CALL_SYNC, ts).apply()
    }
}
