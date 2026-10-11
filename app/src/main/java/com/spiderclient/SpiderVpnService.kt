package com.spiderclient

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import libXray.DialerController
import libXray.LibXray
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

/**
 * Android VPN front-end for the official XTLS/libXray runtime.
 *
 * The Android VpnService owns the TUN descriptor; Xray's native TUN inbound
 * consumes that descriptor via the xray.tun.fd config environment value.
 */
class SpiderVpnService : VpnService() {
    companion object {
        const val ACTION_START = "com.spiderclient.action.START_VPN"
        const val ACTION_STOP = "com.spiderclient.action.STOP_VPN"
        const val EXTRA_RAW_CONFIG = "raw_config"
        private const val CHANNEL_ID = "spider_vpn"
        private const val NOTIFICATION_ID = 7412
        private const val TAG = "SpiderVpnService"
    }

    private val worker = Executors.newSingleThreadExecutor()
    @Volatile private var tun: ParcelFileDescriptor? = null
    @Volatile private var running = false

    private val dialerController = object : DialerController {
        override fun ProtectFd(fd: Int): Boolean = protect(fd)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                worker.execute { stopCoreAndTunnel() }
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val raw = intent.getStringExtra(EXTRA_RAW_CONFIG).orEmpty()
                if (raw.isBlank()) {
                    reportError("No server configuration was supplied.")
                    stopSelf(startId)
                    return START_NOT_STICKY
                }
                startForeground(NOTIFICATION_ID, buildNotification("Starting secure connection…"))
                worker.execute {
                    try {
                        startCoreAndTunnel(raw)
                    } catch (e: Exception) {
                        Log.e(TAG, "VPN start failed", e)
                        stopCoreAndTunnel()
                        reportError(e.message ?: "Xray could not start.")
                        stopForeground(true)
                        stopSelf(startId)
                    }
                }
                return START_NOT_STICKY
            }
            else -> {
                stopSelf(startId)
                return START_NOT_STICKY
            }
        }
    }

    private fun startCoreAndTunnel(raw: String) {
        if (running) stopCoreAndTunnel()
        val shareResponse = invoke(
            JSONObject()
                .put("apiVersion", 3)
                .put("method", "convertShareLinksToXrayJson")
                .put("payload", JSONObject().put("text", raw))
        )
        val outbounds = shareResponse.optJSONObject("data")?.optJSONArray("outbounds")
            ?: throw IllegalArgumentException("Xray could not parse this configuration: ${shareResponse.optString("error")}")
        if (outbounds.length() == 0) throw IllegalArgumentException("The configuration has no valid Xray outbound.")

        val controller = dialerController
        LibXray.registerDialerController(controller)
        LibXray.setDNS(controller, "1.1.1.1:53")

        val established = Builder()
            .setSession("Spider Client")
            .setMtu(1500)
            .addAddress("198.18.0.1", 30)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("1.1.1.1")
            .establish() ?: throw IllegalStateException("Android did not establish the VPN interface. Grant VPN permission and try again.")

        tun = established
        val xrayConfig = JSONObject()
            .put("log", JSONObject().put("loglevel", "warning"))
            .put("env", JSONObject().put("xray.tun.fd", established.fd))
            .put("inbounds", JSONArray().put(
                JSONObject()
                    .put("tag", "spider-tun-in")
                    .put("listen", "127.0.0.1")
                    .put("port", 0)
                    .put("protocol", "tun")
                    .put("settings", JSONObject().put("name", "spider0").put("mtu", 1500))
            ))
            .put("outbounds", outbounds)

        val response = invoke(
            JSONObject()
                .put("apiVersion", 3)
                .put("method", "runXray")
                .put("payload", JSONObject().put("xrayJson", xrayConfig.toString()))
        )
        if (!response.optBoolean("success", false)) {
            throw IllegalStateException(response.optString("error").ifBlank { "Xray failed to start." })
        }

        running = true
        getSharedPreferences("spider_client", MODE_PRIVATE).edit()
            .putBoolean("vpn_connected", true)
            .putString("vpn_error", "")
            .apply()
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification("Connected · Xray is running"))
    }

    private fun invoke(request: JSONObject): JSONObject {
        val rawResponse = LibXray.invoke(request.toString())
        val response = try { JSONObject(rawResponse) } catch (e: Exception) {
            throw IllegalStateException("libXray returned an invalid response.")
        }
        if (!response.optBoolean("success", false)) {
            throw IllegalStateException(response.optString("error").ifBlank { "libXray request failed." })
        }
        return response
    }

    private fun stopCoreAndTunnel() {
        try {
            if (running) {
                val response = JSONObject(
                    LibXray.invoke(JSONObject().put("apiVersion", 3).put("method", "stopXray").put("payload", JSONObject()).toString())
                )
                if (!response.optBoolean("success", false)) Log.w(TAG, "Xray stop returned: ${response.optString("error")}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Ignoring Xray shutdown error", e)
        } finally {
            running = false
            try { LibXray.resetDNS() } catch (e: Exception) { Log.w(TAG, "DNS reset failed", e) }
            try { tun?.close() } catch (_: Exception) { }
            tun = null
            getSharedPreferences("spider_client", MODE_PRIVATE).edit()
                .putBoolean("vpn_connected", false)
                .apply()
            stopForeground(true)
            stopSelf()
        }
    }

    private fun reportError(message: String) {
        getSharedPreferences("spider_client", MODE_PRIVATE).edit()
            .putBoolean("vpn_connected", false)
            .putString("vpn_error", message)
            .apply()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Spider VPN", NotificationManager.IMPORTANCE_LOW)
            channel.description = "Shows the current Spider Client VPN connection state."
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(message: String): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("Spider Client")
                .setContentText(message)
                .setContentIntent(openApp)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("Spider Client")
                .setContentText(message)
                .setContentIntent(openApp)
                .setOngoing(true)
                .build()
        }
    }

    override fun onRevoke() {
        worker.execute { stopCoreAndTunnel() }
        super.onRevoke()
    }

    override fun onDestroy() {
        worker.execute { stopCoreAndTunnel() }
        worker.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)
}
