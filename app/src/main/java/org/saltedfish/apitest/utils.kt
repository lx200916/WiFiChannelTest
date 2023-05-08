package org.saltedfish.apitest

import android.net.wifi.ScanResult
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.patrykandpatrick.vico.core.component.shape.ShapeComponent
import com.patrykandpatrick.vico.core.entry.ChartEntry
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.marker.Marker

fun ScanResult.getChannelWidth():Int{
    return when(this.channelWidth){
        0->20
        1->40
        2->80
        3->160
        4->160
        5->320
        else->0
    }
}
fun ScanResult.getChannelWidthFriendlyName():String{
    return when(this.channelWidth){
        0->"20Mhz"
        1->"40Mhz"
        2->"80Mhz"
        3->"160Mhz"
        4->"80Mhz+80Mhz"
        5->"320Mhz"
        else->"Unknown"
    }
}
fun getChannelIndex(channel:Int):Int{
    return when(channel){
        in 2400..2484->(channel-2407)/5
        in 4900..4980->(channel-4900)/5+180
        in 5000..5865->(channel-5035)/5+7
        else->0
    }
}

public data class NamedFloatEntry(
    override val x: Float,
    override val y: Float,
    val name:String
) : ChartEntry {
    override fun withY(y: Float): ChartEntry = FloatEntry(
        x = x,
        y = y,
    )
}
