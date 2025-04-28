package com.example.vpn_app_android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import java.io.FileInputStream
import java.io.FileOutputStream

class MyVpnService : VpnService() {
    companion object {
        const val ACTION_STOP_VPN = "com.example.vpn_app_android.STOP_VPN"
    }//intent especial para que la vpn se detenga
    private var running = false
    private var vpnInterface: ParcelFileDescriptor? = null
    private val channelId = "vpn_channel"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_VPN) {
            stopVpn()
            return START_NOT_STICKY
        }

        startForeground(1, createNotification())

        val builder = Builder()
            .setSession("DemoVPN")
            .addAddress("10.0.0.2", 24)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("8.8.8.8")

        try {
            builder.addAllowedApplication("com.whatsapp")
            builder.addAllowedApplication("com.android.chrome")
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            stopSelf()
            return START_NOT_STICKY
        }

        vpnInterface = builder.establish()

        if (vpnInterface == null) {
            println("❌ No se pudo establecer la interfaz VPN")
        } else {
            println("✅ Interfaz VPN establecida correctamente")
        }

        vpnInterface?.let {
            val inputStream = FileInputStream(it.fileDescriptor)
            val outputStream = FileOutputStream(it.fileDescriptor)

            Thread {
                running = true
                val buffer = ByteArray(32767)
                while (running) {
                    val length = inputStream.read(buffer)
                    if (length > 0) {
                        println("DemoVPN: Paquete capturado de $length bytes")
                    }
                }
            }.start()
        }

        return START_STICKY
    }



    private fun createNotification(): Notification {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "VPN Service", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("VPN activa")
            .setContentText("Tu VPN está protegiendo la conexión")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onRevoke() {
        running = false
        println("🧹 onRevoke ejecutado")
        vpnInterface?.close()
        stopSelf()
    }
    private fun stopVpn() {
        running = false
        println("🛑 VPN detenida manualmente")
        vpnInterface?.close()
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }
    override fun onDestroy() {
        running = false
        println("🧹 onDestroy ejecutado")
        vpnInterface?.close()
        vpnInterface = null
        super.onDestroy()
    }
}