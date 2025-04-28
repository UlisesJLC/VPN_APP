import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vpn_app_android.MyVpnService
import com.example.vpn_app_android.ui.theme.Vpn_app_androidTheme

class MainActivity : ComponentActivity() {
    private var vpnRunning by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Vpn_app_androidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (vpnRunning) "VPN activa" else "VPN inactiva",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (vpnRunning) Color(0xFF4CAF50) else Color(0xFFF44336),
                            modifier = Modifier.padding(bottom = 32.dp)
                        )
                        Button(
                            onClick = {
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
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!vpnRunning) MaterialTheme.colorScheme.primary else Color.Red
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        ) {
                            Text(
                                text = if (!vpnRunning) "Iniciar VPN" else "Detener VPN",
                                fontSize = 20.sp,
                                color = Color.White
                            )
                        }
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