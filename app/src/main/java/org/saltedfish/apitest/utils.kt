package org.saltedfish.apitest

import android.net.wifi.ScanResult
import android.net.wifi.ScanResult.WIFI_STANDARD_LEGACY
import android.os.Build
import androidx.annotation.RequiresApi
import com.patrykandpatrick.vico.core.entry.ChartEntry
import com.patrykandpatrick.vico.core.entry.FloatEntry
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

fun ScanResult.getChannelWidth(): Int {
    return when (this.channelWidth) {
        0 -> 20
        1 -> 40
        2 -> 80
        3 -> 160
        4 -> 160
        5 -> 320
        else -> 0
    }
}

//https://cs.android.com/android/platform/superproject/+/master:packages/modules/Wifi/framework/java/android/net/wifi/ScanResult.java;bpv=0;bpt=1
@RequiresApi(Build.VERSION_CODES.R)
fun ScanResult.StandardToString(): String {
    return when (this.wifiStandard) {
        WIFI_STANDARD_LEGACY -> "legacy"
        ScanResult.WIFI_STANDARD_11N -> "11n"
        ScanResult.WIFI_STANDARD_11AC -> "11ac"
        ScanResult.WIFI_STANDARD_11AX -> "11ax"
        ScanResult.WIFI_STANDARD_11AD -> "11ad"
        ScanResult.WIFI_STANDARD_11BE -> "11be"
        else -> "unknown"

    }
}

fun ScanResult.getChannelWidthFriendlyName(): String {
    return when (this.channelWidth) {
        0 -> "20Mhz"
        1 -> "40Mhz"
        2 -> "80Mhz"
        3 -> "160Mhz"
        4 -> "80Mhz+80Mhz"
        5 -> "320Mhz"
        else -> "Unknown"
    }
}

fun getChannelIndex(channel: Int): Int {
    return when (channel) {
        in 2400..2484 -> (channel - 2407) / 5
        in 4900..4980 -> (channel - 4900) / 5 + 180
        in 5000..5865 -> (channel - 5035) / 5 + 7
        else -> 0
    }
}

open class NamedFloatEntry(
    override val x: Float,
    override val y: Float,
    open val Yv: Float = -1f,
    open val name: String
) : ChartEntry {
    override fun withY(y: Float): ChartEntry = FloatEntry(
        x = x,
        y = y,
    )
}

fun JSONObject.getStringOpt(key: String): String {
    return if (this.has(key)) {
        this.getString(key)
    } else {
        ""
    }
}

fun JSONObject.getIntOpt(key: String): Long {
    return if (this.has(key)) {
        this.getLong(key)
    } else {
        0
    }
}

fun JSONObject.getObjectOpt(key: String): JSONObject? {
    return if (this.has(key)) {
        this.getJSONObject(key)
    } else {
        null
    }
}

fun JSONObject.getArrayOpt(key: String): JSONArray? {
    return if (this.has(key)) {
        this.getJSONArray(key)
    } else {
        null
    }
}

//parse String value to int then convert to XXd XXh XXm XXs
fun JSONObject.getFormatedDuration(key: String): String {
    val duration = this.getIntOpt(key)
    val day = duration / 86400
    val hour = duration % 86400 / 3600
    val minute = duration % 3600 / 60
    val second = duration % 60
    return "${if (day > 0) "${day}d" else ""}${if (hour > 0) " ${hour}h" else ""}${if (minute > 0) " ${minute}m" else ""}${if (second > 0) " ${second}s" else ""}"
}

//parse String value to number of bytes then convert to File Size String
fun JSONObject.getFormatedSize(key: String): String {
    val size = this.getIntOpt(key)
    val k = 1024
    val m = k * 1024
    val g = m * 1024
    return when {
        size >= g -> "${(size.toFloat() / g).toString().take(4)}GB"
        size >= m -> "${(size.toFloat() / m).toString().take(4)}MB"
        size >= k -> "${(size.toFloat() / k).toString().take(4)}KB"
        else -> "$size B"
    }
}

fun JSONObject.getFormatedSpeed(key: String): String {
    val speed = this.getIntOpt(key)
    val k = 1000
    val m = k * 1000
    val g = m * 1000
    return when {
        speed >= g -> "${(speed.toFloat() / g).toString().take(4)}Gbps"
        speed >= m -> "${(speed.toFloat() / m).toString().take(4)}Mbps"
        speed >= k -> "${(speed.toFloat() / k).toString().take(4)}Kbps"
        else -> "$speed Bps"
    }
}

fun JSONObject.getFormatedDateTime(key: String): String {
    val date = this.getIntOpt(key)
    return android.text.format.DateFormat.format("MM-dd HH:mm:ss", java.util.Date(date * 1000L))
        .toString()
}

fun JSONObject.getTimeStamp(key: String): Date {
    val date = this.getIntOpt(key)
    return Date(date * 1000L)
}


fun JSONObject.getFormatedPercentage(key: String): String {
    val percentage = this.getIntOpt(key)
    return "${percentage}%"
}




