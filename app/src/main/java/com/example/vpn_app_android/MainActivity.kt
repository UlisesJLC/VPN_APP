package com.example.vpn_app_android

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vpn_app_android.ui.theme.Vpn_app_androidTheme

class MainActivity : ComponentActivity() {
    private var vpnRunning by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Vpn_app_androidTheme {
                Column(modifier = Modifier.padding(20.dp)) {
                    Button(onClick = {
                        if (!vpnRunning) {
                            val intent = VpnService.prepare(this@MainActivity)
                            if (intent != null) {
                                startActivityForResult(intent, 0)
                            } else {
                                onActivityResult(0, RESULT_OK, null)
                            }
                        } else {
                            stopService(Intent(this@MainActivity, MyVpnService::class.java))
                            vpnRunning = false
                        }
                    }) {
                        Text(if (!vpnRunning) "Iniciar VPN Demo" else "Detener VPN")
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val intent = Intent(this, MyVpnService::class.java)
            startService(intent)
            vpnRunning = true
        }
    }
}