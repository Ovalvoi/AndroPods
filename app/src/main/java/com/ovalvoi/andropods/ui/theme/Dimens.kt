package com.ovalvoi.andropods.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Every size the UI draws with, in one place.
 *
 * These were previously literals scattered through the screens, which is how
 * the corner radius of a card and the corner radius of the pill inside it
 * drifted apart. Naming them by role rather than by value means a change to
 * "the large card radius" lands everywhere it should.
 *
 * Sizes are in dp and scale with display density; text is in sp and therefore
 * also honours the user's font-size setting.
 */
object Dimens {

    /** Corner radii. Large and soft throughout -- nothing here is sharp. */
    val CornerCard = 32.dp
    val CornerTile = 20.dp
    val CornerPill = 999.dp

    /** The screen's outer gutter, and the rhythm between its sections. */
    val ScreenGutter = 20.dp
    val SectionGap = 28.dp
    val ItemGap = 12.dp
    val TightGap = 6.dp

    /** Inner padding for the two card shapes. */
    val CardPadding = 18.dp
    val TilePadding = 12.dp

    /**
     * The earbud rings, which are the screen's primary read.
     *
     * [RingDiameterMax] is what a comfortable phone gets; the screen shrinks
     * toward [RingDiameterMin] on a small or short display rather than
     * clipping, so this stays responsive without hardcoding a device.
     */
    val RingDiameterMax = 132.dp
    val RingDiameterMin = 96.dp
    val RingStroke = 8.dp

    /** The case card's smaller ring, and the popup's smaller one again. */
    val RingDiameterCase = 58.dp
    val RingStrokeCase = 5.dp

    /** Icon sizes by role. */
    val IconEarbud = 46.dp
    val IconCase = 24.dp
    val IconInline = 16.dp
    val IconChip = 18.dp

    /** Minimum interactive size; below this a control is hard to hit. */
    val TouchTarget = 48.dp

    /** The circular accent button behind the popup's Bluetooth mark. */
    val ChipButton = 38.dp

    /** Hairline borders. Visible as an edge, never as a line. */
    val BorderHairline = 1.dp

    /** Below this screen height the layout switches to its compact metrics. */
    val CompactHeightThreshold = 700.dp

    /** Letter spacing for the small all-caps labels (LEFT / RIGHT / CASE). */
    val LabelTracking = 1.4.sp
}
