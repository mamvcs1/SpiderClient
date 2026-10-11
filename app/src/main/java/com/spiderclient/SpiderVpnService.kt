package com.spiderclient

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import libXray.DialerController
import libXray.LibXray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

class SpiderVpnService : VpnService() {
    companion object {
        const val ACTION_START = "com.spiderclient.action.START_VPN"
        const val ACTION_STOP = "com.spiderclient.action.STOP_VPN"
        const val EXTRA_RAW_CONFIG = "raw_config"
        private const val CHANNEL_ID = "spider_vpn"
        private const val NOTIFICATION_ID = 7301
        private const val TAG = "SpiderVpnService"
    }

    private var tun: ParcelFileDescriptor? = null
    private val stopping = AtomicBoolean(false)
    private val prefs by lazy { getSharedPreferences("spider_client", MODE_PRIVATE) }
    private val dialerController = object : DialerController {
        override fun protectFd(fd: Long): Boolean = this@SpiderVpnService.protect(fd.toInt())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopCore()
                stopSelf()
            }
            ACTION_START -> {
                val raw = intent.getStringExtra(EXTRA_RAW_CONFIG).orEmpty()
                if (raw.isBlank()) {
                    fail("No server configuration selected")
                    stopSelf()
                } else if (tun == null) {
                    createNotificationChannel()
                    startForeground(NOTIFICATION_ID, notification("Starting Xray core…"))
                    Thread { startCore(raw) }.start()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startCore(raw: String) {
        try {
            stopping.set(false)
            val builder = Builder()
                .setSession("Spider Client")
                .setMtu(1500)
                .addAddress("10.111.0.1", 30)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("8.8.8.8")
            if (Build.VERSION.SDK_INT >= 21) {
                builder.addAddress("fd00:111:111::1", 126)
                builder.addRoute("::", 0)
            }
            tun = builder.establish() ?: throw IllegalStateException("Android did not establish the VPN interface")

            LibXray.registerDialerController(dialerController)
            LibXray.setDNS(dialerController, "8.8.8.8:53")

            val convertRequest = JSONObject()
                .put("apiVersion", 3)
                .put("method", "convertShareLinksToXrayJson")
                .put("payload", JSONObject().put("text", raw))
            val converted = JSONObject(LibXray.invoke(convertRequest.toString()))
            if (!converted.optBoolean("success", false)) {
                throw IllegalArgumentException(converted.optString("error", "Could not parse server configuration"))
            }
            val config = converted.optJSONObject("data")
                ?: throw IllegalArgumentException("Xray returned an empty configuration")
            val env = config.optJSONObject("env") ?: JSONObject()
            env.put("xray.tun.fd", tun!!.fd)
            config.put("env", env)

            prefs.edit().putBoolean("vpn_connected", false).putString("vpn_error", "").apply()
            val runRequest = JSONObject()
                .put("apiVersion", 3)
                .put("method", "runXray")
                .put("payload", JSONObject().put("xrayJson", config.toString()))
            Thread {
                try {
                    val response = JSONObject(LibXray.invoke(runRequest.toString()))
                    if (!response.optBoolean("success", false) && !stopping.get()) {
                        fail(response.optString("error", "Xray core stopped with an error"))
                    }
                } catch (e: Exception) {
                    if (!stopping.get()) fail(e.message ?: "Xray core failed")
                }
            }.start()

            var started = false
            for (attempt in 0 until 20) {
                if (stopping.get()) return
                Thread.sleep(500)
                val state = try {
                    JSONObject(LibXray.invoke(JSONObject()
                        .put("apiVersion", 3)
                        .put("method", "getXrayState")
                        .toString()))
                } catch (_: Exception) { JSONObject() }
                if (state.optBoolean("success", false) &&
                    state.optJSONObject("data")?.optBoolean("running", false) == true) {
                    started = true
                    break
                }
            }
            if (!started) throw IllegalStateException("Xray core did not report a running state")
            prefs.edit().putBoolean("vpn_connected", true).apply()
            updateNotification("Spider Client is connected")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN", e)
            fail(e.message ?: "Failed to start Xray VPN")
            stopCore()
            stopSelf()
        }
    }

    private fun stopCore() {
        if (!stopping.compareAndSet(false, true)) return
        try {
            LibXray.invoke(JSONObject().put("apiVersion", 3).put("method", "stopXray").toString())
        } catch (e: Exception) {
            Log.w(TAG, "Xray stop request failed", e)
        }
        try { LibXray.resetDNS() } catch (_: Exception) { }
        try { tun?.close() } catch (_: Exception) { }
        tun = null
        prefs.edit().putBoolean("vpn_connected", false).apply()
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE) else @Suppress("DEPRECATION") stopForeground(true)
    }

    private fun fail(message: String) {
        Log.e(TAG, message)
        prefs.edit().putBoolean("vpn_connected", false).putString("vpn_error", message).apply()
        updateNotification("Connection failed: $message")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "Spider Client VPN", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun notification(message: String): Notification {
        val pending = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        )
        return if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.spider_icon)
                .setContentTitle("Spider Client")
                .setContentText(message)
                .setContentIntent(pending)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setSmallIcon(R.drawable.spider_icon)
                .setContentTitle("Spider Client")
                .setContentText(message)
                .setContentIntent(pending)
                .setOngoing(true)
                .build()
        }
    }

    private fun updateNotification(message: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(message))
    }

    override fun onDestroy() {
        stopCore()
        super.onDestroy()
    }
}
