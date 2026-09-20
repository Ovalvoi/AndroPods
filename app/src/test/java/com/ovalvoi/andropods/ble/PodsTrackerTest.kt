package com.ovalvoi.andropods.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PodsTrackerTest {

    private var clock = 0L
    private fun tracker() = PodsTracker(nowMs = { clock })

    /** Distinct states so assertions can tell the pairs apart by battery. */
    private fun state(left: Int) = PodsState(
        model = PodsModel.AIRPODS_GEN_2,
        leftBattery = left,
        rightBattery = left,
        caseBattery = 100,
        isLeftCharging = false,
        isRightCharging = false,
        isCaseCharging = false,
        isLidOpen = true,
        lidOpenCounter = 0,
        rawLeftInEar = false,
        rawRightInEar = false,
    )

    @Test
    fun returnsNullBeforeAnyAdvertisement() {
        assertNull(tracker().current())
    }

    @Test
    fun adoptsTheFirstPairItSees() {
        val t = tracker()
        assertEquals(50, t.onAdvertisement("AA", -70, state(50))?.leftBattery)
    }

    @Test
    fun prefersTheStrongerOfTwoNewPairs() {
        val t = tracker()
        t.onAdvertisement("AA", -80, state(50))
        assertEquals(60, t.onAdvertisement("BB", -50, state(60))?.leftBattery)
    }

    @Test
    fun keepsTheIncumbentAgainstAMarginallyStrongerRival() {
        // The train-carriage case: a stranger's pods a few dB stronger must not
        // hijack the readout.
        val t = tracker()
        t.onAdvertisement("AA", -60, state(50))
        val result = t.onAdvertisement("BB", -55, state(60))
        assertEquals(50, result?.leftBattery)
    }

    @Test
    fun yieldsToARivalThatClearsTheHysteresisMargin() {
        val t = tracker()
        t.onAdvertisement("AA", -60, state(50))
        // -45 beats -60 by 15dB, comfortably over the 8dB margin.
        assertEquals(60, t.onAdvertisement("BB", -45, state(60))?.leftBattery)
    }

    @Test
    fun doesNotFlapWhileSignalsJitterWithinTheMargin() {
        val t = tracker()
        t.onAdvertisement("AA", -60, state(50))
        repeat(10) { i ->
            clock += 100
            t.onAdvertisement("BB", -57 + (i % 3), state(60))
            t.onAdvertisement("AA", -62 + (i % 3), state(50))
        }
        assertEquals(50, t.current()?.leftBattery)
    }

    @Test
    fun tracksUpdatesFromTheIncumbent() {
        val t = tracker()
        t.onAdvertisement("AA", -60, state(50))
        clock += 1_000
        assertEquals(40, t.onAdvertisement("AA", -60, state(40))?.leftBattery)
    }

    @Test
    fun forgetsAPairThatStopsBroadcasting() {
        val t = tracker()
        t.onAdvertisement("AA", -60, state(50))
        clock += PodsTracker.CANDIDATE_TTL_MS + 1
        assertNull(t.current())
    }

    @Test
    fun promotesTheRemainingPairAfterTheIncumbentExpires() {
        // Incumbent goes quiet (pocketed, out of range); a weaker pair that is
        // still broadcasting should take over rather than nothing showing.
        val t = tracker()
        t.onAdvertisement("AA", -50, state(50))
        clock += 10_000
        t.onAdvertisement("BB", -80, state(60))
        clock += PodsTracker.CANDIDATE_TTL_MS - 9_000  // AA stale, BB still fresh
        assertEquals(60, t.current()?.leftBattery)
    }

    @Test
    fun readoptsAfterReset() {
        val t = tracker()
        t.onAdvertisement("AA", -50, state(50))
        t.reset()
        assertNull(t.current())
        assertEquals(60, t.onAdvertisement("BB", -80, state(60))?.leftBattery)
    }
}
