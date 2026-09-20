package com.ovalvoi.andropods.ble

/**
 * Parser for Apple's "proximity pairing" BLE advertisement (company 0x004C,
 * message type 0x07) -- the beacon that carries AirPods battery state.
 *
 * Pure Kotlin, no Android dependencies, so the bit-twiddling below is testable
 * against recorded hex fixtures with no device, no Bluetooth and no permissions.
 *
 * Layout is cross-verified against four independent open-source implementations
 * (librepods, LightPods, airpods-on-android, AirStatus). Offsets are measured
 * from the first byte AFTER the two-byte company ID.
 *
 *   0      message type, always 0x07
 *   1      payload length, 0x19 (25) on current firmware
 *   3..4   device model, big-endian. AirPods Gen 2 == 0x0F20
 *   5      status: primary-pod, in-case and in-ear bits
 *   6      pods battery, two nibbles
 *   7      high nibble = charging flags, low nibble = case battery
 *   8      lid state and open counter
 *   11..26 AES-encrypted tail. Ignored -- Gen 2 populates the plaintext nibbles.
 *
 * Deliberately NOT used: the field table in furiousMAC/continuity. It counts
 * from a different base and describes a layout where left/right occupy separate
 * whole bytes, which contradicts every working implementation.
 */
object ProximityPayload {

    const val APPLE_COMPANY_ID = 0x004C
    const val TYPE_PROXIMITY_PAIRING: Byte = 0x07

    /**
     * Shortest payload that actually carries battery state.
     *
     * Reading up to offset 8 needs only 9 bytes, but 9 is not a safe gate.
     * Gen 2 also emits a SHORTER 0x07 variant while connected, whose bytes
     * 5..10 are the pod's MAC address rather than status and battery nibbles:
     *
     *   14FF4C00 07 0F 00 0F20 D0DAD70F1448 15 E2E223 0200
     *
     * That frame passes a 9-byte check and decodes into confident nonsense --
     * address bytes read as battery deciles and lid flags. It carries 17
     * payload bytes; the proximity-pairing frame that holds battery carries 24.
     * Requiring 20 separates them with margin either way, without matching the
     * length byte at data[1] -- that byte varies across firmware and filtering
     * on it silently drops valid frames (see README).
     *
     * Captured from the real pair; see README "AirPods Gen 2 confirmed present".
     */
    private const val MIN_LENGTH = 20

    /**
     * Decode a proximity-pairing payload.
     *
     * @param data manufacturer-specific bytes with the company ID already
     *   stripped, i.e. data[0] is the message type.
     * @return the decoded state, or null if this is not a well-formed 0x07
     *   message. Returning null rather than throwing is intentional: malformed
     *   and unrelated frames are normal on a busy radio, not exceptional.
     */
    fun parse(data: ByteArray): PodsState? {
        if (data.size < MIN_LENGTH) return null
        if (data[0] != TYPE_PROXIMITY_PAIRING) return null

        val model = ((data[3].toInt() and 0xFF) shl 8) or (data[4].toInt() and 0xFF)

        // Every field below is Gen 2's plaintext-nibble layout. Newer AirPods
        // moved battery into the encrypted tail and would decode to garbage
        // here, as would any other Apple device emitting a 0x07 frame -- a
        // HomePod or Watch in the room, or a stranger's pods on a train.
        // Rejecting unknown models is what keeps those out of the tracker.
        if (model != PodsModel.AIRPODS_GEN_2) return null

        val status = data[5].toInt() and 0xFF
        val podsBattery = data[6].toInt() and 0xFF
        val flagsAndCase = data[7].toInt() and 0xFF
        val lid = data[8].toInt() and 0xFF

        // Either pod can be the broadcaster. Bit 5 says the left pod is primary;
        // when it is not, every left/right field below is transposed.
        val isPrimaryLeft = (status shr 5) and 0x01 == 1
        val isThisPodInCase = (status shr 6) and 0x01 == 1
        val flipped = !isPrimaryLeft

        val leftNibble = if (flipped) (podsBattery shr 4) and 0x0F else podsBattery and 0x0F
        val rightNibble = if (flipped) podsBattery and 0x0F else (podsBattery shr 4) and 0x0F
        val caseNibble = flagsAndCase and 0x0F

        val chargeFlags = (flagsAndCase shr 4) and 0x0F
        val leftChargeBit = if (flipped) 0x02 else 0x01
        val rightChargeBit = if (flipped) 0x01 else 0x02

        // In-ear assignment is transposed by primary-pod XOR in-case, not by
        // the primary-pod bit alone.
        val earSwapped = isPrimaryLeft xor isThisPodInCase

        // Lid bit is inverted: 0 means open.
        val isLidOpen = (lid shr 3) and 0x01 == 0

        return PodsState(
            model = model,
            leftBattery = decodeBattery(leftNibble),
            rightBattery = decodeBattery(rightNibble),
            caseBattery = decodeBattery(caseNibble),
            isLeftCharging = chargeFlags and leftChargeBit != 0,
            isRightCharging = chargeFlags and rightChargeBit != 0,
            isCaseCharging = chargeFlags and 0x04 != 0,
            isLidOpen = isLidOpen,
            lidOpenCounter = lid and 0x07,
            rawLeftInEar = if (earSwapped) status and 0x08 != 0 else status and 0x02 != 0,
            rawRightInEar = if (earSwapped) status and 0x02 != 0 else status and 0x08 != 0,
        )
    }

    /**
     * Convert a battery nibble to a percentage.
     *
     * The nibble is a decile. We use n*10, matching librepods, LightPods and
     * airpods-on-android, and matching what iOS displays. AirStatus instead
     * reports n*10+5 ("decile midpoint"); no source offers evidence for it and
     * it would not agree with an iPhone sitting next to the phone.
     *
     * 0xF means unknown and maps to null. 0xA..0xE are treated as full.
     */
    internal fun decodeBattery(nibble: Int): Int? = when (nibble) {
        in 0x0..0x9 -> nibble * 10
        in 0xA..0xE -> 100
        else -> null
    }

    /**
     * Locate and decode the Apple 0x07 payload inside a full raw advertisement.
     *
     * Walks the length-type-value structure directly rather than using
     * ScanRecord.getManufacturerSpecificData(). That accessor returns a
     * SparseArray keyed by company ID and keeps only the LAST entry per key, so
     * a Find My (0x12) frame in the same packet silently evicts the 0x07 frame
     * we need. This bug is invisible until it is not.
     *
     * @param raw the bytes from ScanRecord.getBytes().
     */
    fun parseRawScanRecord(raw: ByteArray): PodsState? {
        var i = 0
        while (i < raw.size) {
            val length = raw[i].toInt() and 0xFF
            if (length == 0) return null          // end-of-data padding
            // A structure occupies bytes i .. i+length inclusive, so it is
            // valid when it ends exactly at the last byte. Using >= here
            // dropped any AD structure that ran to the end of the buffer --
            // including, on a tightly packed advertisement, the 0x07 frame.
            if (i + length > raw.size - 1) return null // truncated frame

            val type = raw[i + 1].toInt() and 0xFF
            if (type == 0xFF && length >= 3) {     // manufacturer-specific data
                val companyId = (raw[i + 2].toInt() and 0xFF) or
                    ((raw[i + 3].toInt() and 0xFF) shl 8)   // little-endian
                if (companyId == APPLE_COMPANY_ID) {
                    val payload = raw.copyOfRange(i + 4, i + 1 + length)
                    parse(payload)?.let { return it }
                    // Not a 0x07 frame -- keep walking, another AD structure
                    // in this same packet may carry it.
                }
            }
            i += length + 1
        }
        return null
    }
}
