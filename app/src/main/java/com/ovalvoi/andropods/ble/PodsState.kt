package com.ovalvoi.andropods.ble

/**
 * A decoded snapshot of one AirPods proximity-pairing advertisement.
 *
 * Battery levels are nullable by design. Apple encodes "unknown" as nibble 0xF
 * and AirPods emit it routinely -- a pod in the case with the lid shut reports
 * unknown, not zero. Callers must render null as "no reading", never as empty.
 */
data class PodsState(
    val model: Int,
    val leftBattery: Int?,
    val rightBattery: Int?,
    val caseBattery: Int?,
    val isLeftCharging: Boolean,
    val isRightCharging: Boolean,
    val isCaseCharging: Boolean,
    val isLidOpen: Boolean,
    val lidOpenCounter: Int,
    /**
     * Raw in-ear bits, decoded for diagnostics only.
     *
     * These are known-unreliable: they report "in ear" with the buds sitting in
     * a shut case. LibrePods does not trust them either and sources ear
     * detection from the AAP channel instead. Log these; do not drive UI or
     * media control from them.
     */
    val rawLeftInEar: Boolean,
    val rawRightInEar: Boolean,
) {
    val isKnownModel: Boolean get() = model == PodsModel.AIRPODS_GEN_2
}

object PodsModel {
    /** AirPods (2nd generation, 2019) -- A2031 / A2032. */
    const val AIRPODS_GEN_2 = 0x0F20

    fun displayName(model: Int): String = when (model) {
        AIRPODS_GEN_2 -> "AirPods (2nd generation)"
        else -> "Unknown Apple audio device (0x%04X)".format(model)
    }
}
