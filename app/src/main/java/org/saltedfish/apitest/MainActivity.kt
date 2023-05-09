package org.saltedfish.apitest

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.saltedfish.apitest.ui.theme.APITestTheme
import kotlin.math.absoluteValue

class MainActivity : ComponentActivity() {
    val TAG = "MainActivity"

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val locationPermissionState = rememberPermissionState(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            val client = remember {
                HttpClient(OkHttp) {
                    engine {
                        // Configure an engine
                    }

                }
            }
            val navIndex = remember {
                mutableStateOf(0)
            }
            val wiFiInfoList = remember { mutableStateListOf<ScanResult>() }
            var (APInfoJson, setAPINfoJSon) = remember {
                mutableStateOf<JSONObject?>(null)
            }
            LaunchedEffect(true) {
                val wifiScanReceiver = object : BroadcastReceiver() {
                    @SuppressLint("MissingPermission")
                    override fun onReceive(context: Context, intent: Intent) {
                        wiFiInfoList.clear()
                        val success =
                            intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                        if (!locationPermissionState.status.isGranted) {
                            Log.e(TAG, "No permission to scan wifi")
                            return
                        } else {
                            if (!success) Log.e(TAG, "Failed to scan wifi");
                        }
                        val scanResults =
                            wifiManager.scanResults.sortedWith(compareBy { item -> item.level.absoluteValue })
                        wiFiInfoList.addAll(scanResults)
                        Log.i(TAG, scanResults.size.toString())
                        Log.i(TAG, "Scan Updated")
                    }
                }
                val intentFilter = IntentFilter()
                intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                applicationContext.registerReceiver(wifiScanReceiver, intentFilter)
            }
            LaunchedEffect(true) {

                val resp = getAPInfo(client);
                if (resp != null) {
                    setAPINfoJSon(resp)
                }
            }

            LaunchedEffect(locationPermissionState.status) {
                if (!locationPermissionState.status.isGranted) {
                    Log.e(TAG, "No permission to scan wifi")
                    locationPermissionState.launchPermissionRequest()
                } else {
                    val success = wifiManager.startScan()
                    if (!success) Log.e(TAG, "Failed to scan wifi");
                }
            }
            APITestTheme {
                if (!locationPermissionState.status.isGranted) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(all = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "\uD83E\uDD2A",
                                style = MaterialTheme.typography.headlineLarge,
                                fontSize = 100.sp,
                            )
                            Text(
                                text = "We Need `Fine Location` Premission to Scan WiFi",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Button(onClick = {
                                locationPermissionState.launchPermissionRequest()
                            }, Modifier.padding(top = 10.dp)) {
                                Text(
                                    text = "Grant Permission",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                        }

                    }
                } else
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),

                        bottomBar = {
                            BottomAppBar(
                                actions = {
                                    navIconButton(
                                        icon = painterResource(id = R.drawable.baseline_wifi_24),
                                        onClick = { navIndex.value = 0 },
                                        isActive = navIndex.value == 0
                                    )
                                    navIconButton(
                                        icon = painterResource(id = R.drawable.round_speed_24),
                                        onClick = { navIndex.value = 1 },
                                        isActive = navIndex.value == 1
                                    )
                                    if (APInfoJson != null) navIconButton(
                                        icon = painterResource(id = R.drawable.router_24px),
                                        onClick = { navIndex.value = 2 },
                                        isActive = navIndex.value == 2
                                    )

                                },
                                floatingActionButton = {
                                    Row {
                                        FloatingActionButton(
                                            onClick = { /* do something */ },
                                        ) {
                                            Icon(Icons.Filled.Refresh, "Localized description")
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        FloatingActionButton(
                                            onClick = { /* do something */ },

                                            ) {
                                            Icon(
                                                painterResource(id = R.drawable.upload_48px),
                                                "Localized description"
                                            )
                                        }
                                    }
                                })
                        }
                    ) {
//                        WiFiListScreen(wiFiInfoList.toList(),it)
                        when (navIndex.value) {
                            0 -> WiFiListScreen(wiFiInfoList.toList(), it)
                            1 -> SpeedInfoScreen(it)
                            2 -> if (APInfoJson != null) APScreen(it, APInfoJson)
                        }
                    }
            }
        }

    }


    fun scanResultstoEntry(scanResult: ScanResult): List<Pair<Number, Number>> {
        if (scanResult.channelWidth == CHANNEL_WIDTH_80MHZ_PLUS_MHZ) {
            //TODO: Do not Support 80+80
            return listOf(Pair(0.0, 0.0))
        } else {
            val center = scanResult.frequency;
            val length = scanResult.getChannelWidth() / 2;
            val start = getChannelIndex(center - length);
            val end = getChannelIndex(center + length);
            return listOf(
                Pair(start.toFloat(), 0.0),
                Pair(getChannelIndex(center).toFloat(), scanResult.level.absoluteValue.toFloat()),
                Pair(end.toFloat(), 0.0)
            )
        }
    }

    suspend fun getAPInfo(client: HttpClient): JSONObject? {
        return withContext(Dispatchers.IO) {
            try{
            val resp = client.post("https://buptnet.icu/api/wireless/diag").bodyAsText()
            Log.i(TAG, resp)
            JSONObject(resp)
        } catch (err: Throwable) {
            Log.e(TAG, "Network Error:${err}")
            null
        }}

    }

    @Composable
    fun navIconButton(isActive: Boolean, icon: Painter, onClick: () -> Unit) {
        IconButton(
            onClick = onClick,
            enabled = !isActive,
            colors = IconButtonDefaults.iconButtonColors(disabledContentColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(painter = icon, contentDescription = "Localized description", Modifier.size(32.dp))
        }
    }

}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    APITestTheme {
    }
}
