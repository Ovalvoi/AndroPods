package com.ovalvoi.andropods.data

import com.ovalvoi.andropods.ble.PodsModel
import com.ovalvoi.andropods.ble.PodsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CaseMemoryTest {

    private class FakeStorage(var stored: LastCaseReading? = null) : CaseMemory.Storage {
        var writes = 0
        override fun read() = stored
        override fun write(reading: LastCaseReading) { stored = reading; writes++ }
    }

    private fun state(case: Int?, caseCharging: Boolean = false) = PodsState(
        model = PodsModel.AIRPODS_GEN_2,
        leftBattery = 100, rightBattery = 90, caseBattery = case,
        isLeftCharging = false, isRightCharging = false, isCaseCharging = caseCharging,
        isLidOpen = false, lidOpenCounter = 0,
        rawLeftInEar = false, rawRightInEar = false,
    )

    @Test
    fun returnsNullWhenTheCaseHasNeverBeenSeen() {
        val memory = CaseMemory(FakeStorage(), nowMs = { 1_000 })
        assertNull(memory.observe(state(case = null)))
        assertNull(memory.last)
    }

    @Test
    fun remembersALiveCaseReadingWithItsTime() {
        val storage = FakeStorage()
        val memory = CaseMemory(storage, nowMs = { 5_000 })

        val reading = memory.observe(state(case = 80, caseCharging = true))

        assertEquals(LastCaseReading(80, true, 5_000), reading)
        assertEquals(reading, storage.stored)
        assertEquals(1, storage.writes)
    }

    @Test
    fun keepsTheLastReadingOncePodsLeaveTheCase() {
        val storage = FakeStorage()
        var now = 5_000L
        val memory = CaseMemory(storage) { now }
        memory.observe(state(case = 80))

        now = 65_000
        val remembered = memory.observe(state(case = null))!!

        assertEquals(80, remembered.level)
        assertEquals(5_000, remembered.seenAtMs)
        assertEquals(60_000, remembered.ageMs(now))
        assertEquals(1, storage.writes) // unknown readings are never persisted
    }

    @Test
    fun aFreshReadingReplacesTheRememberedOne() {
        val memory = CaseMemory(FakeStorage(), nowMs = { 1 })
        memory.observe(state(case = 80))
        memory.observe(state(case = null))

        assertEquals(30, memory.observe(state(case = 30))!!.level)
    }

    @Test
    fun loadsWhatWasPersistedBeforeTheProcessRestarted() {
        val storage = FakeStorage(stored = LastCaseReading(60, false, 42))
        val memory = CaseMemory(storage, nowMs = { 100 })

        assertSame(storage.stored, memory.last)
        assertEquals(60, memory.observe(state(case = null))!!.level)
    }

    @Test
    fun ageNeverGoesNegativeIfTheClockMovesBack() {
        val reading = LastCaseReading(50, false, seenAtMs = 10_000)
        assertEquals(0, reading.ageMs(nowMs = 9_000))
        assertTrue(reading.ageMs(nowMs = 10_001) > 0)
        assertFalse(reading.isCharging)
    }
}
