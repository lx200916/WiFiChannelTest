package com.patrykandpatrick.vico.core.legend


import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.core.chart.draw.ChartDrawContext
import com.patrykandpatrick.vico.core.component.Component
import com.patrykandpatrick.vico.core.component.dimension.Padding
import com.patrykandpatrick.vico.core.component.text.HorizontalPosition
import com.patrykandpatrick.vico.core.component.text.TextComponent
import com.patrykandpatrick.vico.core.component.text.VerticalPosition
import com.patrykandpatrick.vico.core.context.MeasureContext
import com.patrykandpatrick.vico.core.dimensions.MutableDimensions
import com.patrykandpatrick.vico.core.dimensions.emptyDimensions
import com.patrykandpatrick.vico.core.extension.half
import com.patrykandpatrick.vico.core.legend.VerticalLegend.Item
public open class HorizontalLegend(
    public var items: Collection<Item>,
    public var iconSizeDp: Float,
    public var iconPaddingDp: Float,
    public var lineSpacingDp: Float = 0f,
    public var spacingDp: Float = 0f,
    override val padding: MutableDimensions = emptyDimensions(),
) : Legend, Padding {
    // Cache the height of each line
    private val heights = mutableListOf<Float>()

    // Cache the items in each line
    private val lines = mutableListOf<MutableList<Item>>(mutableListOf())
    override val bounds: RectF = RectF()
    override fun getHeight(context: MeasureContext, availableWidth: Float): Float = with(context) {
        if (items.isEmpty()) return@with 0f
        lines.clear()
        lines.add(mutableListOf())
        var height = maxOf(
            items.first().getHeight(context, availableWidth),
            iconSizeDp.pixels,
        )
        heights.add(height)
        buildLines(context, availableWidth) {
            val currentHeight = maxOf(it.getHeight(context, availableWidth), iconSizeDp.pixels)
            heights.add(currentHeight)
            height += currentHeight
        }
        height + (lines.size - 1) * lineSpacingDp.pixels + padding.verticalDp.pixels
    }

    override fun draw(context: ChartDrawContext): Unit = with(context) {
        var currentTop = bounds.top + padding.topDp.pixels
        // isLtr? startX means the line starts at X from left : it starts at X from right
        val startX = if (isLtr) {
            chartBounds.left + padding.startDp.pixels
        } else {
            chartBounds.right - padding.startDp.pixels - iconSizeDp.pixels
        }
        val availableWidth = chartBounds.width()
        if (lines.isEmpty()) {
            buildLines(context, availableWidth)
        }

        lines.forEachIndexed { index, item ->
            var currentStart = 0f
            val currentLineHeight = heights.getOrElse(index) { item.first().getHeight(context, availableWidth) }
            val centerY = currentTop + currentLineHeight.half

            item.forEach {
                it.icon.draw(
                    context = context,
                    left = startX + currentStart,
                    top = centerY - iconSizeDp.half.pixels,
                    right = startX + iconSizeDp.pixels + currentStart,
                    bottom = centerY + iconSizeDp.half.pixels,
                )
                currentStart += if (isLtr) {
                    (iconSizeDp + iconPaddingDp).pixels
                } else {
                    -iconPaddingDp.pixels
                }
                it.label.drawText(
                    context = context,
                    text = it.labelText,
                    textX = startX + currentStart,
                    textY = centerY,
                    horizontalPosition = HorizontalPosition.End,
                    verticalPosition = VerticalPosition.Center,
                    maxTextWidth =
                    (chartBounds.width() - (iconSizeDp + iconPaddingDp + padding.horizontalDp).pixels).toInt(),
                )
                currentStart += if (isLtr) {
                    it.getOriginalLabelWidth(context, availableWidth) + spacingDp.pixels
                } else {
                    -(it.getOriginalLabelWidth(context, availableWidth) + spacingDp.pixels + iconSizeDp.pixels)
                }
            }
            currentTop += currentLineHeight + lineSpacingDp.pixels
        }
    }

    protected fun buildLines(context: MeasureContext, availableWidth: Float, callback: (it: Item) -> Unit = {}): Unit =
        with(context) {
            var remainWidth = availableWidth
            var currentLine = 0
            lines.clear()
            lines.add(mutableListOf())
            items.forEach {
                remainWidth -= it.getOriginalWidth(context, availableWidth) - spacingDp.pixels
                if (remainWidth >= 0 || remainWidth == availableWidth) {
                    lines[currentLine].add(it)
                    return@forEach
                }
                if (remainWidth < 0) {
                    // Line break
                    currentLine++
                    remainWidth = availableWidth - it.getOriginalWidth(context, availableWidth)
                    lines.add(mutableListOf(it))
                    callback(it)
                }
            }
        }
    protected open fun Item.getHeight(
        context: MeasureContext,
        availableWidth: Float,
    ): Float = with(context) {
        label.getHeight(
            context = context,
            text = labelText,
            width = (availableWidth - iconSizeDp.pixels - iconPaddingDp.pixels).toInt(),
        )
    }

    protected open fun Item.getOriginalLabelWidth(
        context: MeasureContext,
        availableWidth: Float,
    ): Float = with(context) {
        label.getWidth(
            context = context,
            text = labelText,
            width = (availableWidth - iconSizeDp.pixels - iconPaddingDp.pixels).toInt(),
        )
    }

    protected open fun Item.getOriginalWidth(
        context: MeasureContext,
        availableWidth: Float,
    ): Float = with(context) {
        this@getOriginalWidth.getOriginalLabelWidth(context, availableWidth) + (iconSizeDp + iconPaddingDp).pixels
    }
}
@Composable
public fun horizonLegend(
    items: Collection<Item>,
    iconSize: Dp,
    iconPadding: Dp,
    spacing: Dp = 0.dp,
    lineSpacingDp: Dp = 0.dp,
    padding: MutableDimensions = emptyDimensions(),
): HorizontalLegend = remember(items, iconSize, iconPadding, spacing,lineSpacingDp ,padding) {
    HorizontalLegend(
        items = items,
        iconSizeDp = iconSize.value,
        iconPaddingDp = iconPadding.value,
        spacingDp = spacing.value,
        lineSpacingDp = lineSpacingDp.value,
        padding = padding,
    )
}
@Composable
public fun horizonLegendItem(
    icon: Component,
    label: TextComponent,
    labelText: CharSequence,
): Item = remember(icon, label, labelText) {
   Item(
        icon = icon,
        label = label,
        labelText = labelText,
    )
}
