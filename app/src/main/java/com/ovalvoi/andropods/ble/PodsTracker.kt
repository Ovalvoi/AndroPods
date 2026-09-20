package com.ovalvoi.andropods.ble

/**
 * Decides which nearby AirPods are *yours* when several are broadcasting.
 *
 * This is necessary because the beacon does not identify a specific pair.
 * AirPods use resolvable private addresses that rotate roughly every 15
 * minutes, and the IRK needed to resolve them is only obtainable through the
 * AAP handshake we deliberately do not perform. Two identical Gen 2 pairs are
 * indistinguishable by model ID alone.
 *
 * So identity is heuristic: pick the strongest signal, but make the incumbent
 * sticky so a passing stranger on a train does not hijack the readout. A rival
 * must beat the current pick by [SWITCH_MARGIN_DB] to take over. The real
 * guarantee comes from upstream -- the service only scans while your bonded
 * AirPods are connected over classic Bluetooth.
 *
 * Not thread-safe; call from a single scan callback thread.
 */
class PodsTracker(
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    private class Candidate(
        val address: String,
        var rssi: Int,
        var state: PodsState,
        var lastSeenMs: Long,
    )

    private val candidates = mutableMapOf<String, Candidate>()
    private var incumbentAddress: String? = null

    /**
     * Record one decoded advertisement.
     *
     * @return the state of the pair we believe is yours, or null if nothing is
     *   currently credible.
     */
    fun onAdvertisement(address: String, rssi: Int, state: PodsState): PodsState? {
        val now = nowMs()

        candidates[address]?.also {
            it.rssi = rssi
            it.state = state
            it.lastSeenMs = now
        } ?: run {
            candidates[address] = Candidate(address, rssi, state, now)
        }

        expireStale(now)
        return select()
    }

    /** Drop the incumbent and all candidates, e.g. on disconnect. */
    fun reset() {
        candidates.clear()
        incumbentAddress = null
    }

    /** Re-evaluate without a new advertisement, expiring anything stale. */
    fun current(): PodsState? {
        expireStale(nowMs())
        return select()
    }

    private fun expireStale(now: Long) {
        candidates.entries.removeAll { now - it.value.lastSeenMs > CANDIDATE_TTL_MS }
        if (incumbentAddress !in candidates) incumbentAddress = null
    }

    private fun select(): PodsState? {
        val strongest = candidates.values.maxByOrNull { it.rssi } ?: return null
        val incumbent = incumbentAddress?.let { candidates[it] }

        // No incumbent, or a rival clears it by the hysteresis margin.
        if (incumbent == null || strongest.rssi > incumbent.rssi + SWITCH_MARGIN_DB) {
            incumbentAddress = strongest.address
            return strongest.state
        }
        return incumbent.state
    }

    companion object {
        /**
         * How much stronger a rival must be to displace the incumbent. Roughly
         * a doubling of apparent closeness; small enough that genuinely moving
         * to your own pods wins, large enough that RSSI jitter does not.
         */
        const val SWITCH_MARGIN_DB = 8

        /** Forget a pair we have not heard from for this long. */
        const val CANDIDATE_TTL_MS = 15_000L
    }
}
