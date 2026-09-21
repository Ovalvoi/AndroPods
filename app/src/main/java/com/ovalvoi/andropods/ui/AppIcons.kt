package com.ovalvoi.andropods.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The app's icons as vectors, tinted by whoever draws them.
 *
 * The earbud and case glyphs are drawn here to match the launcher icon; the
 * rest are Material Symbols paths (Apache 2.0). Kept in code rather than
 * pulling in material-icons-extended, which would add ~20 MB of icons to the
 * debug build for the eight used here.
 */
object AppIcons {

    /** Left bud: head to the upper right, stem hanging down on the outside. */
    val PodLeft: ImageVector by lazy {
        icon(
            "PodLeft",
            "M8,7.5a5.5,5.5 0 1,0 11,0a5.5,5.5 0 1,0 -11,0z",
            "M8.5,9h4v11a2,2 0 0 1 -4,0z",
        )
    }

    /** Right bud, mirror of [PodLeft]. */
    val PodRight: ImageVector by lazy {
        icon(
            "PodRight",
            "M5,7.5a5.5,5.5 0 1,0 11,0a5.5,5.5 0 1,0 -11,0z",
            "M11.5,9h4v11a2,2 0 0 1 -4,0z",
        )
    }

    /** Charging case, lid shut: a rounded box with the lid seam and the status LED cut out. */
    val CaseClosed: ImageVector by lazy {
        icon(
            "CaseClosed",
            "M5,4h14a3,3 0 0 1 3,3v12a3,3 0 0 1 -3,3h-14a3,3 0 0 1 -3,-3v-12a3,3 0 0 1 3,-3z" +
                "M4.5,9h15v1.6h-15z" +
                "M10.9,15.5a1.1,1.1 0 1,0 2.2,0a1.1,1.1 0 1,0 -2.2,0z",
            fillType = PathFillType.EvenOdd,
        )
    }

    /** Charging case with the lid lifted clear of the body. */
    val CaseOpen: ImageVector by lazy {
        icon(
            "CaseOpen",
            "M5,11h14a3,3 0 0 1 3,3v5a3,3 0 0 1 -3,3h-14a3,3 0 0 1 -3,-3v-5a3,3 0 0 1 3,-3z" +
                "M6,2h12a2.5,2.5 0 0 1 2.5,2.5v1.5a2.5,2.5 0 0 1 -2.5,2.5h-12a2.5,2.5 0 0 1 -2.5,-2.5v-1.5a2.5,2.5 0 0 1 2.5,-2.5z" +
                "M10.9,16.5a1.1,1.1 0 1,0 2.2,0a1.1,1.1 0 1,0 -2.2,0z",
            fillType = PathFillType.EvenOdd,
        )
    }

    val Bolt: ImageVector by lazy {
        icon("Bolt", "M13,2L4.5,13.5H11L10,22L19.5,10.5H13Z")
    }

    val Bluetooth: ImageVector by lazy {
        icon(
            "Bluetooth",
            "M17.71,7.71L12,2h-1v7.59L6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 11,14.41V22h1l5.71,-5.71 " +
                "-4.3,-4.29 4.3,-4.29zM13,5.83l1.88,1.88L13,9.59V5.83zm1.88,10.46L13,18.17v-3.76l1.88,1.88z",
        )
    }

    val BluetoothOff: ImageVector by lazy {
        icon(
            "BluetoothOff",
            "M13,5.83l1.88,1.88 -1.6,1.6 1.41,1.41 3.02,-3.02L12,2h-1v5.03l2,2v-3.2zM5.41,4L4,5.41 10.59,12 " +
                "5,17.59 6.41,19 11,14.41V22h1l4.29,-4.29 2.3,2.29L20,18.59 5.41,4zM13,18.17v-3.76l1.88,1.88L13,18.17z",
        )
    }

    val Palette: ImageVector by lazy {
        icon(
            "Palette",
            "M12,3c-4.97,0 -9,4.03 -9,9s4.03,9 9,9c0.83,0 1.5,-0.67 1.5,-1.5 0,-0.39 -0.15,-0.74 -0.39,-1.01 " +
                "-0.23,-0.26 -0.38,-0.61 -0.38,-0.99 0,-0.83 0.67,-1.5 1.5,-1.5H16c2.76,0 5,-2.24 5,-5 0,-4.42 " +
                "-4.03,-8 -9,-8zm-5.5,9c-0.83,0 -1.5,-0.67 -1.5,-1.5S5.67,9 6.5,9 8,9.67 8,10.5 7.33,12 6.5,12zm3,-4" +
                "C8.67,8 8,7.33 8,6.5S8.67,5 9.5,5s1.5,0.67 1.5,1.5S10.33,8 9.5,8zm5,0c-0.83,0 -1.5,-0.67 -1.5,-1.5" +
                "S13.67,5 14.5,5s1.5,0.67 1.5,1.5S15.33,8 14.5,8zm3,4c-0.83,0 -1.5,-0.67 -1.5,-1.5S16.67,9 17.5,9" +
                "s1.5,0.67 1.5,1.5 -0.67,1.5 -1.5,1.5z",
        )
    }

    /** Crescent moon: dark mode. */
    val Moon: ImageVector by lazy {
        icon(
            "Moon",
            "M12,3c-4.97,0 -9,4.03 -9,9s4.03,9 9,9 9,-4.03 9,-9c0,-0.46 -0.04,-0.92 -0.1,-1.36 -0.98,1.37 " +
                "-2.58,2.26 -4.4,2.26 -2.98,0 -5.4,-2.42 -5.4,-5.4 0,-1.81 0.89,-3.42 2.26,-4.4 -0.44,-0.06 " +
                "-0.9,-0.1 -1.36,-0.1z",
        )
    }

    /** Sun: light mode. A filled disc plus eight stroked rays. */
    val Sun: ImageVector by lazy {
        ImageVector.Builder("Sun", 24.dp, 24.dp, 24f, 24f)
            .addPath(addPathNodes("M8,12a4,4 0 1,0 8,0a4,4 0 1,0 -8,0z"), fill = SolidColor(Color.Black))
            .addPath(
                addPathNodes(
                    "M18.5,12H21M5.5,12H3M12,5.5V3M12,18.5V21M16.6,7.4L18.36,5.64M7.4,16.6L5.64,18.36" +
                        "M16.6,16.6L18.36,18.36M7.4,7.4L5.64,5.64",
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
            )
            .build()
    }

    /** Half-filled disc with rays: follow the system's light/dark setting. */
    val BrightnessAuto: ImageVector by lazy {
        icon(
            "BrightnessAuto",
            "M20,15.31L23.31,12 20,8.69V4h-4.69L12,0.69 8.69,4H4v4.69L0.69,12 4,15.31V20h4.69L12,23.31 " +
                "15.31,20H20v-4.69zM12,18V6c3.31,0 6,2.69 6,6s-2.69,6 -6,6z",
        )
    }

    val MusicNote: ImageVector by lazy {
        icon("MusicNote", "M12,3v10.55c-0.59,-0.34 -1.27,-0.55 -2,-0.55 -2.21,0 -4,1.79 -4,4s1.79,4 4,4 4,-1.79 4,-4V7h4V3h-6z")
    }

    val TouchApp: ImageVector by lazy {
        icon(
            "TouchApp",
            "M9,11.24V7.5C9,6.12 10.12,5 11.5,5S14,6.12 14,7.5v3.74c1.21,-0.81 2,-2.18 2,-3.74C16,5.01 13.99,3 " +
                "11.5,3S7,5.01 7,7.5c0,1.56 0.79,2.93 2,3.74zm9.84,4.63l-4.54,-2.26c-0.17,-0.07 -0.35,-0.11 " +
                "-0.54,-0.11H13v-6c0,-0.83 -0.67,-1.5 -1.5,-1.5S10,6.67 10,7.5v10.74c-3.6,-0.76 -3.54,-0.75 " +
                "-3.67,-0.75 -0.31,0 -0.59,0.13 -0.79,0.33l-0.79,0.8 4.94,4.94c0.27,0.27 0.65,0.44 1.06,0.44h6.79" +
                "c0.75,0 1.33,-0.55 1.44,-1.28l0.75,-5.27c0.01,-0.07 0.02,-0.14 0.02,-0.2 0,-0.62 -0.38,-1.16 " +
                "-0.91,-1.38z",
        )
    }

    private fun icon(
        name: String,
        vararg pathData: String,
        fillType: PathFillType = PathFillType.NonZero,
    ): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .apply {
                pathData.forEach { data ->
                    addPath(addPathNodes(data), fill = SolidColor(Color.Black), pathFillType = fillType)
                }
            }
            .build()
}
