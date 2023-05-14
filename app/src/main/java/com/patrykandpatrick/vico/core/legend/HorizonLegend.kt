package com.patrykandpatrick.vico.core.legend


import android.graphics.RectF
import android.util.Log
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
import kotlin.math.max

/**
 * [VerticalLegend] displays legend items in a vertical list.
 *
 * @param items a [Collection] of [Item]s to be displayed by this [VerticalLegend].
 * @param iconSizeDp defines the size of all [Item.icon]s.
 * @param iconPaddingDp defines the padding between each [Item.icon] and its corresponding [Item.label].
 * @param lineSpacingDp define the vertical spacing between lines.
 * @param spacingDp defines the horizon spacing between each [Item] in line.
 * @param padding defines the padding of the content.
 */
public open class HorizonLegend(
    var items: Collection<Item>,
    var iconSizeDp: Float,
    var iconPaddingDp: Float,
    var lineSpacingDp :Float = 0f,
    var spacingDp: Float = 0f,
    override val padding: MutableDimensions = emptyDimensions(),
) : Legend, Padding {

    private val heights = mutableListOf<Float>()
    private val lines = mutableListOf<MutableList<Item>>(mutableListOf())

    override val bounds: RectF = RectF()

    override fun getHeight(context: MeasureContext, availableWidth: Float): Float = with(context) {

        if (items.isEmpty()) return@with 0f
        lines.clear()
        lines.add(mutableListOf())
        var height = 0f+ maxOf(items.first().getHeight(context, availableWidth),iconSizeDp);
        var remainWidth = availableWidth;
        var currentLine = 0;
        heights.add(height)
        items.forEach{
            if (remainWidth>it.getOrignalWidth(context,availableWidth)){
                remainWidth-=it.getOrignalWidth(context,availableWidth);
            if (remainWidth>spacingDp.pixels){
                remainWidth-=spacingDp.pixels;
                lines[currentLine].add(it)
                return@forEach
            }
            }
            currentLine++;
            remainWidth = availableWidth;
            lines.add(mutableListOf(it))
            val currentHeight = maxOf(it.getHeight(context, availableWidth),iconSizeDp.pixels)
            heights.add(currentHeight)
            height+=currentHeight+lineSpacingDp.pixels
        }
        height
    }

    override fun draw(context: ChartDrawContext): Unit = with(context) {
        var currentTop = bounds.top + padding.topDp.pixels
        // isLtr startX means the line starts at X from left or it starts at X from right()
        val startX = if (isLtr) {
            chartBounds.left + padding.startDp.pixels
        } else {
            chartBounds.right - padding.startDp.pixels - iconSizeDp.pixels
        }
        val availableWidth = chartBounds.width()
        if (lines.isEmpty()){
            var remainWidth = availableWidth;
            var currentLine = 0;
            items.forEach{
                if (remainWidth>it.getOrignalWidth(context,availableWidth)){
                    remainWidth-=it.getOrignalWidth(context,availableWidth);
                    if (remainWidth>spacingDp.pixels){
                        remainWidth-=spacingDp.pixels;
                        lines[currentLine].add(it)
                        return@forEach
                    }
                }
                currentLine++;
                remainWidth = availableWidth;
                lines.add(mutableListOf(it))
            }
        }

        lines.forEachIndexed {index,item->
            var currentStart = 0f
            val currentLineHeight = heights.getOrElse(index){item.first().getHeight(context,availableWidth)}
            val centerY = currentTop + currentLineHeight.half

            item.forEachIndexed { ix,it->
                it.icon.draw(
                context = context,
                left = startX+if (isLtr) currentStart else -currentStart,
                top = centerY - iconSizeDp.half.pixels,
                right = startX + iconSizeDp.pixels +if (isLtr) currentStart else -currentStart,
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
                    textX = startX+(if (isLtr) currentStart else -currentStart),
                    textY = centerY,
                    horizontalPosition = HorizontalPosition.End,
                    verticalPosition = VerticalPosition.Center,
                )
                currentStart += if (isLtr) {
                    it.getOrignalLabelWidthDp(context,availableWidth) + spacingDp.pixels
                } else {
                    -it.getOrignalLabelWidthDp(context,availableWidth) + spacingDp.pixels
                }
            }
            currentTop += currentLineHeight + lineSpacingDp.pixels
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
    protected open fun Item.getOrignalLabelWidthDp(
        context: MeasureContext,
        availableWidth: Float,
    ): Float = with(context) {
        label.getWidth(
            context = context,
            text = labelText,
            width = (availableWidth - iconSizeDp.pixels - iconPaddingDp.pixels).toInt(),
        )
    }
    protected open fun Item.getOrignalWidth(
        context: MeasureContext,
        availableWidth: Float,
    ): Float = with(context) {
        this@getOrignalWidth.getOrignalLabelWidthDp(context,availableWidth) +iconSizeDp.pixels+iconPaddingDp.pixels
    }

    /**
     * Defines the appearance of an item of a [Legend].
     *
     * @param icon the [Component] used as the item’s icon.
     * @param label the [TextComponent] used for the label.
     * @param labelText the text content of the label.
     */
    public class Item(
        public val icon: Component,
        public val label: TextComponent,
        public val labelText: CharSequence,
    )
}
@Composable
public fun horizonLegend(
    items: Collection<HorizonLegend.Item>,
    iconSize: Dp,
    iconPadding: Dp,
    spacing: Dp = 0.dp,
    lineSpacingDp: Dp = 0.dp,
    padding: MutableDimensions = emptyDimensions(),
): HorizonLegend = remember(items, iconSize, iconPadding, spacing,lineSpacingDp ,padding) {
    HorizonLegend(
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
): HorizonLegend.Item = remember(icon, label, labelText) {
    HorizonLegend.Item(
        icon = icon,
        label = label,
        labelText = labelText,
    )
}
