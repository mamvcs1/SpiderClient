package com.spiderclient

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Lightweight UI-test build: Xray core is intentionally excluded.
 * Connection requests report that core support has not been added yet.
 */
class SpiderVpnService : Service() {
    companion object {
        const val ACTION_START = "com.spiderclient.action.START_VPN"
        const val ACTION_STOP = "com.spiderclient.action.STOP_VPN"
        const val EXTRA_RAW_CONFIG = "raw_config"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("spider_client", MODE_PRIVATE)
        prefs.edit()
            .putBoolean("vpn_connected", false)
            .putString("vpn_error", "This lightweight test build does not include Xray core.")
            .apply()
        stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
