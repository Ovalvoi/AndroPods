package com.ovalvoi.andropods.ui

import org.junit.Assert.assertSame
import org.junit.Test

class BatteryBandTest {

    @Test
    fun unknownLevelHasNoBand() {
        assertSame(BatteryBand.UNKNOWN, BatteryBand.of(null))
    }

    @Test
    fun twentyPercentAndBelowIsCritical() {
        listOf(0, 10, 20).forEach { assertSame("$it%", BatteryBand.CRITICAL, BatteryBand.of(it)) }
    }

    @Test
    fun thirtyToFortyPercentIsLow() {
        listOf(21, 30, 40).forEach { assertSame("$it%", BatteryBand.LOW, BatteryBand.of(it)) }
    }

    @Test
    fun fiftyPercentAndAboveIsGood() {
        listOf(41, 50, 70, 100).forEach { assertSame("$it%", BatteryBand.GOOD, BatteryBand.of(it)) }
    }
}
