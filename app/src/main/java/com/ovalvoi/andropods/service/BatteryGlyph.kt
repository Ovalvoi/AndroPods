package com.ovalvoi.andropods.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import com.ovalvoi.andropods.ui.BatteryBand
import com.ovalvoi.andropods.ui.theme.BatteryColors

/**
 * Draws a battery level as a small ring, for the notification's custom layout.
 *
 * RemoteViews cannot host Compose, and a fixed set of drawables would need one
 * asset per level per component, so each glyph is rendered to a Bitmap here
 * instead. They are small (a few KB) and only redrawn when a level actually
 * changes, which for a beacon arriving in 10% steps is rare.
 *
 * The colours come from [BatteryColors.Shared], the same regulated set the
 * rest of the app uses, so a red 15% in the shade is the identical red the
 * readout shows.
 */
object BatteryGlyph {

    /**
     * One ring: a track, a sweep for the level, and the number inside it.
     *
     * The number is drawn into the bitmap rather than placed in a neighbouring
     * TextView because a RemoteViews row of alternating icons and labels wraps
     * unpredictably across launchers; one self-contained image per component
     * always lays out the same way.
     *
     * @param level 0-100, or null for "no reading", which draws the track and
     *   a dash.
     * @param isCase the case takes its own pink, matching the app.
     */
    fun render(
        context: Context,
        level: Int?,
        isCase: Boolean = false,
        isRemembered: Boolean = false,
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (SIZE_DP * density).toInt().coerceAtLeast(1)
        val bitmap = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bitmap)

        val stroke = STROKE_DP * density
        val inset = stroke / 2f
        val bounds = RectF(inset, inset, sizePx - inset, sizePx - inset)

        val colors = BatteryColors.Shared
        val levelColor = when {
            level == null -> TRACK_COLOR
            isCase -> colors.case
            else -> when (BatteryBand.of(level)) {
                BatteryBand.GOOD -> colors.good
                BatteryBand.LOW -> colors.low
                BatteryBand.CRITICAL -> colors.critical
                BatteryBand.UNKNOWN -> colors.good
            }
        }
        val argb = levelColor.toArgb()

        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
        }

        ring.color = TRACK_COLOR.toArgb()
        canvas.drawArc(bounds, 0f, FULL_SWEEP, false, ring)

        if (level != null && level > 0) {
            ring.color = argb
            canvas.drawArc(bounds, START_ANGLE, level / 100f * FULL_SWEEP, false, ring)
        }

        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = argb
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = if (level != null && level == 100) {
                // "100" needs to fit inside the same circle as "90".
                TEXT_DP * density * FULL_CHARGE_SHRINK
            } else {
                TEXT_DP * density
            }
        }
        val text = when {
            level == null -> "-"
            isRemembered -> "~$level"
            else -> "$level"
        }
        // Centre on the glyph's cap height rather than its baseline, or the
        // number sits visibly low in the ring.
        val metrics = label.fontMetrics
        val baseline = sizePx / 2f - (metrics.ascent + metrics.descent) / 2f
        canvas.drawText(text, sizePx / 2f, baseline, label)

        return bitmap
    }

    private fun androidx.compose.ui.graphics.Color.toArgb(): Int =
        android.graphics.Color.argb(
            (alpha * 255).toInt(),
            (red * 255).toInt(),
            (green * 255).toInt(),
            (blue * 255).toInt(),
        )

    /** The unlit part of the ring. Neutral grey reads on a light or dark shade. */
    private val TRACK_COLOR = androidx.compose.ui.graphics.Color(0x40808080)

    private const val SIZE_DP = 30f
    private const val STROKE_DP = 3f
    private const val TEXT_DP = 11f
    private const val FULL_SWEEP = 360f
    /** Twelve o'clock; Canvas angles start at three o'clock. */
    private const val START_ANGLE = -90f
    private const val FULL_CHARGE_SHRINK = 0.78f
}
