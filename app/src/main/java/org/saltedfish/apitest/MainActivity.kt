package org.saltedfish.apitest

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.magnifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryModelOf
import org.saltedfish.apitest.ui.theme.APITestTheme
import org.saltedfish.apitest.ui.theme.LineColors
import kotlin.math.absoluteValue

class MainActivity : ComponentActivity() {
    val TAG = "MainActivity"

    @OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val locationPermissionState = rememberPermissionState(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            var chartEntryModel = remember {
                entryModelOf(0)
            }

            val wiFiInfoList = remember { mutableStateListOf<ScanResult>() }
            val entryList5G = remember { mutableStateListOf<List<NamedFloatEntry>>() }
            val entryList24G = remember {
                mutableStateListOf<List<NamedFloatEntry>>()
            }
            val SSIDList24G = remember {
                mutableStateListOf<String>()
            }
            val SSIDList5G = remember {
                mutableStateListOf<String>()
            }
            LaunchedEffect(true) {
                val wifiScanReceiver = object : BroadcastReceiver() {
                    @SuppressLint("MissingPermission")
                    override fun onReceive(context: Context, intent: Intent) {
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

                        wiFiInfoList.clear()
                        entryList24G.clear()
                        entryList5G.clear()
                        SSIDList24G.clear()
                        SSIDList5G.clear()

                        Log.i(TAG, wiFiInfoList.toString())
                        //todo 3.6Ghz?
                        scanResults.forEach { scanResult ->
                            if (scanResult.SSID.isEmpty()) scanResult.SSID="<Hidden>"
                            val entrys =
                                if (scanResult.frequency > 5000) entryList5G else entryList24G
                            val SSIDs = if (scanResult.frequency > 5000) SSIDList5G else SSIDList24G
                            SSIDs.add(scanResult.SSID)
                            entrys.add(
                                if (scanResult.channelWidth == CHANNEL_WIDTH_80MHZ_PLUS_MHZ) {
                                    //TODO: Do not Support 80+80
                                    listOf(NamedFloatEntry(0.0f, 0.0f,scanResult.SSID))
                                } else {
                                    val center = scanResult.frequency;
                                    val length = scanResult.getChannelWidth() / 2;
                                    val start = getChannelIndex(center - length);
                                    val end = getChannelIndex(center + length);
                                    listOf(
                                        NamedFloatEntry(start.toFloat(), 0.0f, scanResult.SSID),
                                        NamedFloatEntry(
                                            getChannelIndex(center).toFloat(),
                                            scanResult.level.absoluteValue.toFloat(),
                                            scanResult.SSID
                                        ),
                                        NamedFloatEntry(end.toFloat(), 0.0f,scanResult.SSID)
                                    )
                                }
                            )
                        }

                        chartEntryModel = ChartEntryModelProducer(*entryList5G.toTypedArray()).getModel()
                        wiFiInfoList.addAll(scanResults)
                    }
                }
                val intentFilter = IntentFilter()
                intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                applicationContext.registerReceiver(wifiScanReceiver, intentFilter)
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
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = "\uD83D\uDEDC WiFi Info",
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                },
                                Modifier.padding(all = 10.dp)
                            )
                        }, contentWindowInsets = WindowInsets(0, 0, 0, 0)

                    ) {
                        Column(
                            modifier = Modifier
                                .padding(it)
                                .padding(horizontal = 20.dp)
                        ) {
                            if (wiFiInfoList.isNotEmpty()) Card(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 20.dp)
                            ) {
                                ProvideChartStyle(m3ChartStyle(entityColors = LineColors)) {
                                    Chart(
                                        modifier= Modifier.padding(5.dp).padding(top = 15.dp),
                                        chart = lineChart(),
                                        model = chartEntryModel,
                                        startAxis = startAxis(),
                                        bottomAxis = bottomAxis(tickOmitSpacing = listOf(65f..99f)),
                                        marker = rememberMarker()
                                        )
                                }
                            }
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(wiFiInfoList) { wiFiInfo ->
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(intrinsicSize = IntrinsicSize.Max)
                                            .padding(vertical = 5.dp)
                                    ) {
                                        Column() {
                                            Text(
                                                text = wiFiInfo.SSID,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = if (wiFiInfo.frequency > 3000) FontWeight.Bold else FontWeight.Normal
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text(
                                                text = wiFiInfo.BSSID,
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                        }
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Tag(
                                                text = "${wiFiInfo.level.absoluteValue}",
                                                leadingIcon = {
                                                    Icon(
                                                        painterResource(R.drawable.baseline_square_foot_24),
                                                        contentDescription = "Distance",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                })
                                            Row() {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                                    Tag(text = "WiFi${wiFiInfo.wifiStandard}")
                                                }
                                                Tag(text = "${wiFiInfo.frequency} Mhz")


                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }


    @Composable
    fun Tag(
        modifier: Modifier = Modifier.padding(all = 2.dp),
        text: String,
        leadingIcon: @Composable () -> Unit = {},
    ) {
        Surface(
            shape = RoundedCornerShape(20),
            modifier = modifier,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary
            ),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {

            Row(
                modifier = Modifier.padding(horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically

            ) {
                leadingIcon()
                Spacer(modifier = Modifier.width(1.dp))
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
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

}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    APITestTheme {
    }
}
