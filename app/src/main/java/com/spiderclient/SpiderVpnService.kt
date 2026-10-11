package com.spiderclient

import android.content.Intent
import android.net.VpnService
import android.os.IBinder

/**
 * VPN service entry point.
 *
 * The Android VPN consent flow is handled by MainActivity via VpnService.prepare().
 * A real tunnel must not be reported as connected until the Xray engine and
 * the TUN-to-core data path are integrated and running.
 */
class SpiderVpnService : VpnService() {
    companion object {
        const val ACTION_START = "com.spiderclient.action.START_VPN"
        const val ACTION_STOP = "com.spiderclient.action.STOP_VPN"
        const val EXTRA_RAW_CONFIG = "raw_config"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("spider_client", MODE_PRIVATE)
        when (intent?.action) {
            ACTION_STOP -> {
                prefs.edit()
                    .putBoolean("vpn_connected", false)
                    .putString("vpn_error", "")
                    .apply()
                stopSelf()
            }
            ACTION_START -> {
                // Do not create a black-hole TUN or fake a successful connection.
                // Xray core + TUN traffic forwarding must be integrated before this
                // service can safely establish a usable VPN connection.
                prefs.edit()
                    .putBoolean("vpn_connected", false)
                    .putString(
                        "vpn_error",
                        "VPN permission is available, but real connection is not ready: Xray core and TUN traffic forwarding are not integrated yet."
                    )
                    .apply()
                stopSelf(startId)
            }
            else -> stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onRevoke() {
        getSharedPreferences("spider_client", MODE_PRIVATE).edit()
            .putBoolean("vpn_connected", false)
            .putString("vpn_error", "VPN permission was revoked by Android.")
            .apply()
        stopSelf()
        super.onRevoke()
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)
}
