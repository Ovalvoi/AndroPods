package com.ovalvoi.andropods.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fixtures are synthesised from the documented layout rather than captured from
 * hardware. They pin the decode rules precisely; they cannot prove the rules
 * match a real Gen 2, which is what the on-device check is for.
 */
class ProximityPayloadTest {

    private fun hex(s: String): ByteArray =
        s.replace(" ", "").chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    /**
     * Build a payload with explicit fields.
     *
     * @param status byte 5
     * @param pods byte 6
     * @param flagsCase byte 7
     * @param lid byte 8
     */
    private fun payload(
        status: Int,
        pods: Int,
        flagsCase: Int,
        lid: Int = 0x08,
        model: Int = PodsModel.AIRPODS_GEN_2,
    ): ByteArray =
        byteArrayOf(
            0x07, 0x19, 0x01,
            ((model shr 8) and 0xFF).toByte(), (model and 0xFF).toByte(),
            status.toByte(), pods.toByte(), flagsCase.toByte(), lid.toByte(),
        ) + ByteArray(16)                 // AES tail: unused, but real frames carry it

    // --- battery decode ---------------------------------------------------

    @Test
    fun decodesNibbleAsDecileTimesTen() {
        assertEquals(0, ProximityPayload.decodeBattery(0x0))
        assertEquals(70, ProximityPayload.decodeBattery(0x7))
        assertEquals(90, ProximityPayload.decodeBattery(0x9))
    }

    @Test
    fun treatsNibbleAThroughEAsFull() {
        assertEquals(100, ProximityPayload.decodeBattery(0xA))
        assertEquals(100, ProximityPayload.decodeBattery(0xE))
    }

    @Test
    fun returnsNullForUnknownBatterySentinel() {
        assertNull(ProximityPayload.decodeBattery(0xF))
    }

    @Test
    fun doesNotUseTheDecileMidpointFormula() {
        // Guards the documented choice of n*10 over AirStatus's n*10+5.
        assertEquals(70, ProximityPayload.decodeBattery(0x7))
    }

    // --- left/right orientation -------------------------------------------

    @Test
    fun assignsLeftAndRightWhenLeftPodIsPrimary() {
        // bit5 set => left primary => not flipped => low nibble is left.
        val state = ProximityPayload.parse(payload(status = 0x20, pods = 0x85, flagsCase = 0x03))!!
        assertEquals(50, state.leftBattery)
        assertEquals(80, state.rightBattery)
    }

    @Test
    fun transposesLeftAndRightWhenRightPodIsPrimary() {
        // bit5 clear => flipped => high nibble becomes left.
        val state = ProximityPayload.parse(payload(status = 0x00, pods = 0x85, flagsCase = 0x03))!!
        assertEquals(80, state.leftBattery)
        assertEquals(50, state.rightBattery)
    }

    @Test
    fun reportsSamePhysicalPodLevelsRegardlessOfWhichPodBroadcasts() {
        // The regression that matters: as the primary pod alternates, a given
        // physical bud must keep its reading. Same physical state, both framings.
        val leftPrimary = ProximityPayload.parse(payload(0x20, 0x85, 0x03))!!
        val rightPrimary = ProximityPayload.parse(payload(0x00, 0x58, 0x03))!!
        assertEquals(leftPrimary.leftBattery, rightPrimary.leftBattery)
        assertEquals(leftPrimary.rightBattery, rightPrimary.rightBattery)
    }

    // --- charging flags ----------------------------------------------------

    @Test
    fun decodesChargingFlagsWhenNotFlipped() {
        val state = ProximityPayload.parse(payload(0x20, 0x55, flagsCase = 0x15))!!
        assertTrue(state.isLeftCharging)
        assertFalse(state.isRightCharging)
        assertFalse(state.isCaseCharging)
    }

    @Test
    fun transposesChargingFlagsWhenFlipped() {
        // Same 0x1 bit, right pod primary => it now means the RIGHT pod.
        val state = ProximityPayload.parse(payload(0x00, 0x55, flagsCase = 0x15))!!
        assertFalse(state.isLeftCharging)
        assertTrue(state.isRightCharging)
    }

    @Test
    fun decodesCaseChargingIndependentlyOfPodOrientation() {
        val notFlipped = ProximityPayload.parse(payload(0x20, 0x55, 0x45))!!
        val flipped = ProximityPayload.parse(payload(0x00, 0x55, 0x45))!!
        assertTrue(notFlipped.isCaseCharging)
        assertTrue(flipped.isCaseCharging)
    }

    @Test
    fun decodesCaseBatteryFromTheLowNibble() {
        val state = ProximityPayload.parse(payload(0x20, 0x55, flagsCase = 0x08))!!
        assertEquals(80, state.caseBattery)
    }

    // --- lid ---------------------------------------------------------------

    @Test
    fun readsLidAsOpenWhenBitThreeIsClear() {
        // Inverted: 0 means open. The easiest bit in the format to get backwards.
        assertTrue(ProximityPayload.parse(payload(0x20, 0x55, 0x03, lid = 0x00))!!.isLidOpen)
    }

    @Test
    fun readsLidAsShutWhenBitThreeIsSet() {
        assertFalse(ProximityPayload.parse(payload(0x20, 0x55, 0x03, lid = 0x08))!!.isLidOpen)
    }

    @Test
    fun extractsTheLidOpenCounterFromTheLowThreeBits() {
        assertEquals(5, ProximityPayload.parse(payload(0x20, 0x55, 0x03, lid = 0x0D))!!.lidOpenCounter)
    }

    // --- real-world states -------------------------------------------------

    @Test
    fun reportsUnknownLevelsForPodsStowedWithTheLidShut() {
        val state = ProximityPayload.parse(payload(0x20, 0xFF, flagsCase = 0x08, lid = 0x08))!!
        assertNull(state.leftBattery)
        assertNull(state.rightBattery)
        assertEquals(80, state.caseBattery)
        assertFalse(state.isLidOpen)
    }

    @Test
    fun reportsOnePodUnknownWhenOnlyOneIsStowed() {
        val state = ProximityPayload.parse(payload(0x20, 0xF7, flagsCase = 0x09))!!
        assertEquals(70, state.leftBattery)
        assertNull(state.rightBattery)
    }

    @Test
    fun identifiesAirPodsGen2ByModelId() {
        val state = ProximityPayload.parse(payload(0x20, 0x55, 0x03))!!
        assertEquals(PodsModel.AIRPODS_GEN_2, state.model)
        assertTrue(state.isKnownModel)
    }

    // --- real hardware capture ---------------------------------------------

    @Test
    fun decodesTheFrameCapturedFromTheRealGen2Pair() {
        // Received on the Pixel 7 at -45 dBm via batch scan, pods out of the
        // case and connected: the full 25-byte proximity-pairing frame that an
        // earlier README claimed Gen 2 never sends while connected. It does.
        //
        //   1E FF 4C00 | 07 19 01 0F20 | 01 | A9 | 8F | 01 | 00 04 C0 D1 ...
        //                type len ? model  st   pods case lid  encrypted tail
        val raw = hex(
            "1E FF 4C 00 07 19 01 0F 20 01 A9 8F 01 " +
                "00 04 C0 D1 B9 25 94 25 F3 48 1F 56 89 E3 4E 40 39 EF"
        )
        val state = ProximityPayload.parseRawScanRecord(raw)!!

        assertEquals(PodsModel.AIRPODS_GEN_2, state.model)
        // status 0x01: bit 5 clear => right pod is primary => nibbles flipped.
        assertEquals(100, state.leftBattery)   // high nibble 0xA
        assertEquals(90, state.rightBattery)   // low nibble 0x9
        // Pods are out of the case, so the case level is unknown -- exactly
        // the 0xF sentinel, mapped to null and never to 0.
        assertNull(state.caseBattery)
        assertFalse(state.isCaseCharging)
        // These two are the readings to hold against an iPhone / MaterialPods
        // for the n*10 vs n*10+5 question and the left/right assignment.
    }

    // --- malformed input ---------------------------------------------------

    @Test
    fun rejectsANonProximityMessageType() {
        val findMy = hex("12 19 01 0F 20 20 55 03 08")
        assertNull(ProximityPayload.parse(findMy))
    }

    @Test
    fun rejectsATruncatedPayloadWithoutThrowing() {
        assertNull(ProximityPayload.parse(hex("07 19 01 0F")))
        assertNull(ProximityPayload.parse(ByteArray(0)))
    }

    @Test
    fun rejectsTheShortConnectedVariantRatherThanMisreadingItsMacAddress() {
        // Captured from the real pair. Length 0x0F, and bytes 5..10 are the MAC
        // address, not status and battery. Decoding it would report the address
        // byte 0xDA as "left 100%, right 100%" and 0xD7 as a lid state.
        val connected = hex("07 0F 00 0F 20 D0 DA D7 0F 14 48 15 E2 E2 23 02 00")
        assertNull(ProximityPayload.parse(connected))
    }

    @Test
    fun rejectsModelsOtherThanGen2() {
        // AirPods Pro: same 0x07 type, but battery lives in the encrypted tail,
        // so this layout would decode to nonsense.
        assertNull(ProximityPayload.parse(payload(0x20, 0x55, 0x03, model = 0x0E20)))
    }

    @Test
    fun findsAProximityPayloadThatEndsExactlyAtTheBufferEnd() {
        // Off-by-one guard: a final AD structure whose last byte is also the
        // record's last byte is valid, not truncated.
        val raw = hex(
            "1B FF 4C 00 07 19 01 0F 20 20 85 03 08 " +
                "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"
        )
        assertEquals(28, raw.size)  // 0x1B + 1, i.e. nothing follows
        val state = ProximityPayload.parseRawScanRecord(raw)!!
        assertEquals(50, state.leftBattery)
    }

    // --- raw scan record walking -------------------------------------------

    @Test
    fun findsTheProximityPayloadInsideARawAdvertisement() {
        //  02 01 1A            flags
        //  1B FF 4C 00 ...     manufacturer data, Apple, 0x07 payload
        val raw = hex(
            "02 01 1A 1B FF 4C 00 07 19 01 0F 20 20 85 03 08 " +
                "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"
        )
        val state = ProximityPayload.parseRawScanRecord(raw)!!
        assertEquals(50, state.leftBattery)
        assertEquals(80, state.rightBattery)
    }

    @Test
    fun findsTheProximityPayloadEvenWhenAFindMyFramePrecedesIt() {
        // The SparseArray trap: getManufacturerSpecificData() would keep only
        // the last Apple entry and lose the 0x07 frame. Walking the raw record
        // must still find it.
        val raw = hex(
            "02 01 1A " +
                "05 FF 4C 00 12 02 " +                              // Apple 0x12 first
                "1B FF 4C 00 07 19 01 0F 20 20 85 03 08 " +         // Apple 0x07 second
                "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"
        )
        val state = ProximityPayload.parseRawScanRecord(raw)!!
        assertEquals(50, state.leftBattery)
    }

    @Test
    fun ignoresAdvertisementsFromOtherVendors() {
        val raw = hex("02 01 1A 05 FF 75 00 42 42")  // Samsung
        assertNull(ProximityPayload.parseRawScanRecord(raw))
    }

    @Test
    fun handlesZeroPaddingWithoutLooping() {
        val raw = hex("02 01 1A 00 00 00 00")
        assertNull(ProximityPayload.parseRawScanRecord(raw))
    }
}
