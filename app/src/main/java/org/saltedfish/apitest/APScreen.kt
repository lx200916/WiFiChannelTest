package org.saltedfish.apitest

import android.graphics.Typeface
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.compose.component.textComponent
import com.patrykandpatrick.vico.compose.dimensions.dimensionsOf
import com.patrykandpatrick.vico.compose.legend.verticalLegend
import com.patrykandpatrick.vico.compose.legend.verticalLegendItem
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.compose.style.currentChartStyle
import com.patrykandpatrick.vico.core.axis.Axis
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.ChartEntry
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.legend.horizonLegend
import com.patrykandpatrick.vico.core.legend.horizonLegendItem
import org.json.JSONObject
import org.saltedfish.apitest.shapes.RoundedStarShape
import org.saltedfish.apitest.ui.theme.LineColors
import java.util.Date
val XAxislist= listOf<String>("终端接受","终端发送","干扰")

@Composable
fun APScreen(paddingValues: PaddingValues,APInfo:JSONObject) {

    val apInfo = remember(APInfo){
        APInfo.getObjectOpt("ap")
    }
    val staInfo = remember(APInfo){
        APInfo.getObjectOpt("sta")
    }
    if (apInfo == null || staInfo == null) {
        return
    }
    val apChartEntrys = remember(apInfo){
        if (APInfo.getObjectOpt("ap_trends")?.getArrayOpt("current_channel_utilization") == null) {
            return@remember null
        }
        val apTrends = APInfo.getObjectOpt("ap_trends")!!
        val currentChannelUtilization = apTrends.getArrayOpt("current_channel_utilization")!!
        val latest_snapshot_time = (apTrends.getIntOpt("latest_snapshot_time")-(currentChannelUtilization.length()-1)*2*60)*1000L

        val chartEntrys = listOf<MutableList<ChartEntry>>(mutableListOf(), mutableListOf(),
            mutableListOf()
        )
        for (i in 0 until currentChannelUtilization.length()) {
            val entry = currentChannelUtilization.getString(i)
            val entries = entry.split(",")
            if (entries.size != 4) {
                continue
            }
            val all = entries.last().toFloat()
            var stackedY = 0f;
            for (x in 0..2){
                val percent = entries[x].toLong()/all*100
                val time = latest_snapshot_time+i*2*60*1000
                stackedY+=percent
//                Log.i("AP", time.toString())
                chartEntrys[x].add(DatetimeEntry(Date(time),XAxislist[x],i.toFloat(),stackedY,percent))
            }
        }
             ChartEntryModelProducer(chartEntrys).getModel()
    }
    val axisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, chartValues ->
        (chartValues.chartEntryModel.entries.first().getOrNull(value.toInt()) as? DatetimeEntry)
            ?.localDate
            ?.let {
//                Log.i("APScreen",android.text.format.DateFormat.format("yyyy-MM-dd hh:mm:ss",it).toString())
                android.text.format.DateFormat.format("HH\nmm",it).toString() }.toString()
    }
    val roundedStarShape = remember {
        RoundedStarShape(
            sides = 9,
            curve = 0.09,
            rotation = 0f,
            iterations = 360
        )
    }
//    val iconMap = mapOf<String,>()

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .padding(5.dp)
                .padding(bottom = 0.dp),
            shape = roundedStarShape,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = staInfo.getStringOpt("client_health"),
                modifier = Modifier.padding(30.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 50.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Text(
            text = apInfo.getStringOpt("ap_name"),
            style = MaterialTheme.typography.titleLarge,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            softWrap = true,
            modifier = Modifier
        )
        Text(
            text = "${apInfo.getStringOpt("ap_eth_mac_address")} @ ${staInfo.getStringOpt("ssid")}",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Light,
            softWrap = true,
            modifier = Modifier
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .height(IntrinsicSize.Max)
                .fillMaxWidth()
        ) {
            DetailsCard(
                Modifier.weight(1f),
                "AP Model",
                apInfo.getStringOpt("ap_model"),
                leadingIcon = {rememberVectorPainter(Icons.Filled.Settings)}
            )
            DetailsCard(
                Modifier.weight(1f),
                "AP Online Time",
                apInfo.getFormatedDuration("ap_uptime"),
                leadingIcon = { painterResource(id = R.drawable.online_prediction_48px)}
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .height(IntrinsicSize.Max)
                .fillMaxWidth()
        ) {
            DetailsCard(
                Modifier.weight(1f),
                "Device Mac",
                staInfo.getStringOpt("sta_mac_address"),
                leadingIcon = { painterResource(id = R.drawable.memory_48px)}
            )
            DetailsCard(
                Modifier.weight(1f),
                "Transferred Data Last 1min",
                staInfo.getFormatedSize("total_data_bytes"),
                leadingIcon = { painterResource(id = R.drawable.spoke_48px)}
            )

        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .height(IntrinsicSize.Max)
                .fillMaxWidth()
        ) {
            DetailsCard(
                Modifier.weight(1f),
                "Data Rate",
                "${staInfo.getFormatedSpeed("speed")}/${staInfo.getFormatedSpeed("max_negotiated_rate")}",
                leadingIcon = { painterResource(id = R.drawable.leak_add_48px)}
            )
            DetailsCard(
                Modifier.weight(1f),
                "Client Transferred Frames Last 1min",
                staInfo.getStringOpt("total_data_frames"),
                leadingIcon = { painterResource(id = R.drawable.connect_without_contact_48px)}
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .height(IntrinsicSize.Max)
                .fillMaxWidth()
        ) {
            DetailsCard(
                Modifier.weight(1f),
                "Transferred Data Last 1min",
                staInfo.getFormatedSize("total_data_bytes"),
                leadingIcon = { painterResource(id = R.drawable.stream_48px)}
            )
            DetailsCard(
                Modifier.weight(1f),
                "Throughput Last 1min",
                staInfo.getFormatedSize("total_data_throughput"),

                leadingIcon = { painterResource(id = R.drawable.throughtput_48px)}
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .height(IntrinsicSize.Max)
                .fillMaxWidth()
        ) {
            DetailsCard(
                Modifier.weight(1f),
                "Client Online Since:",
                remember {
                    staInfo.getFormatedDateTime("client_timestamp")
                },
                leadingIcon = { painterResource(id = R.drawable.memory_48px)}
            )
            DetailsCard(
                Modifier.weight(1f),
                "Connected Device",
                apInfo.getStringOpt("sta_count"),
                leadingIcon = { painterResource(id = R.drawable.devices_other_48px)}
            )
        }
        if (apChartEntrys!=null) Card(
            Modifier
                .fillMaxWidth()
        ) {
            ProvideChartStyle(m3ChartStyle(entityColors = LineColors).let {  it.copy( it.axis.copy(axisLabelLineCount = 2, )) }) {
                Chart(
                    modifier = Modifier
                        .padding(5.dp)
                        .padding(vertical = 5.dp),
//                    chart = lineChart(axisValuesOverrider = AxisValuesOverrider.fixed(minY = 0f, maxY = 100f,)),
                    chart = lineChart(),
                    model = apChartEntrys,
                    startAxis = startAxis(),
//                        bottomAxis = bottomAxis(tickOmitSpacing = listOf(65f..99f)),
                    bottomAxis = bottomAxis(valueFormatter = axisValueFormatter, labelRotationDegrees = 0f, title = "AP Channels Usage"),
                    marker = rememberMarker(),
                    legend = rememberLegend(),

                )
            }
        }
        Spacer(modifier =Modifier.height(10.dp))
    }
}



@Composable
fun DetailsCard(
    modifier: Modifier = Modifier,
    key: String,
    value: String,
    leadingIcon: (@Composable ()->Painter)? = null
) {
    Card(
        modifier
            .fillMaxHeight()
            .padding(horizontal = 5.dp)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if (leadingIcon != null) Icon(
                modifier=Modifier.size(32.dp),
                painter = leadingIcon(),
                contentDescription = "Item icon",
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = key,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier =Modifier.weight(1f))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                )
        }
    }


}

fun LazyGridScope.DetailsCardItem(
    modifier: Modifier = Modifier,
    key: String,
    value: String,
    leadingIcon: (@Composable ()->Painter)? = null
) {
    item {
        DetailsCard(modifier, key, value, leadingIcon)
    }


}
@Composable
private fun rememberLegend() = horizonLegend(
    items = XAxislist.mapIndexed { index, chartName ->
        horizonLegendItem(
            icon = shapeComponent(Shapes.pillShape, LineColors[index]),
            label = textComponent(
                color = currentChartStyle.axis.axisLabelColor,
                textSize = 12.sp,
                typeface = Typeface.MONOSPACE,
            ),
            labelText = chartName,
        )
    },
    iconSize = 8.dp,
    iconPadding = 10.dp,
    spacing = 10.dp,
    padding = dimensionsOf(top = 8.dp),
)
class DatetimeEntry(
    val localDate: Date,
    override val name:String,
    override val x: Float,
    override val y: Float,
    override val Yv:Float
) : NamedFloatEntry(x, y,Yv,name) {
    override fun withY(y: Float) = DatetimeEntry(localDate,name, x,Yv, y)
}