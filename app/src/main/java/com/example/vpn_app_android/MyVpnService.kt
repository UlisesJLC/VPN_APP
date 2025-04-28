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
    }

    private var running = false
    private var vpnInterface: ParcelFileDescriptor? = null
    private val channelId = "vpn_channel"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_VPN) {
            println("🛑 Acción STOP_VPN recibida")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            running = false
            vpnInterface?.close()
            vpnInterface = null
            stopSelf()
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
                try {
                    while (running) {
                        val length = inputStream.read(buffer)
                        if (length > 0) {
                            val sourceIP = "${buffer[12].toInt() and 0xFF}.${buffer[13].toInt() and 0xFF}.${buffer[14].toInt() and 0xFF}.${buffer[15].toInt() and 0xFF}"
                            val destIP = "${buffer[16].toInt() and 0xFF}.${buffer[17].toInt() and 0xFF}.${buffer[18].toInt() and 0xFF}.${buffer[19].toInt() and 0xFF}"
                            val protocolNumber = buffer[9].toInt() and 0xFF
                            val protocol = when (protocolNumber) {
                                1 -> "ICMP"
                                6 -> "TCP"
                                17 -> "UDP"
                                else -> "Desconocido"
                            }

                            var sourcePort = -1
                            var destPort = -1
                            var serviceType = ""

                            if (protocol == "TCP" || protocol == "UDP") {
                                sourcePort = ((buffer[20].toInt() and 0xFF) shl 8) or (buffer[21].toInt() and 0xFF)
                                destPort = ((buffer[22].toInt() and 0xFF) shl 8) or (buffer[23].toInt() and 0xFF)

                                serviceType = when (destPort) {
                                    80 -> "HTTP"
                                    443 -> "HTTPS"
                                    53 -> "DNS"
                                    123 -> "NTP"
                                    25 -> "SMTP"
                                    110 -> "POP3"
                                    143 -> "IMAP"
                                    22 -> "SSH"
                                    993 -> "IMAPS"
                                    995 -> "POP3S"
                                    587 -> "SMTP Secure"
                                    5222 -> "XMPP (Chat)"
                                    3478 -> "STUN (WebRTC)"
                                    1935 -> "RTMP (Streaming)"
                                    else -> "Otro"
                                }
                            }

                            println("📦 Paquete capturado: $length bytes | IP origen: $sourceIP:$sourcePort | IP destino: $destIP:$destPort | Protocolo: $protocol | Servicio: $serviceType")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    println("Hilo finalizado correctamente")
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
        vpnInterface = null
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
