package com.ovalvoi.andropods.ui

/**
 * Which colour a battery level gets: green, orange or red.
 *
 * AirPods report in 10% steps, so the bands are chosen on those steps: 10 and
 * 20 are red, 30 and 40 orange, 50 and up green. Pure Kotlin so it is unit
 * tested; the actual colours live in `ui/theme/BatteryColors.kt` because they
 * differ between light and dark.
 */
enum class BatteryBand {
    GOOD,
    LOW,
    CRITICAL,
    UNKNOWN;

    companion object {
        const val CRITICAL_MAX_PERCENT = 20
        const val LOW_MAX_PERCENT = 40

        fun of(level: Int?): BatteryBand = when {
            level == null -> UNKNOWN
            level <= CRITICAL_MAX_PERCENT -> CRITICAL
            level <= LOW_MAX_PERCENT -> LOW
            else -> GOOD
        }
    }
}
