package com.ovalvoi.andropods.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.ovalvoi.andropods.ble.PodsState

/**
 * The case level as last reported, with when it was reported.
 *
 * Gen 2's case has no radio. A pod reports the case level only while sitting
 * in it, so with both pods in your ears the beacon says "unknown" -- and so
 * would an iPhone. The honest thing to show in that state is the last reading
 * we did get, clearly marked as such, which is what this remembers.
 */
data class LastCaseReading(
    val level: Int,
    val isCharging: Boolean,
    val seenAtMs: Long,
) {
    fun ageMs(nowMs: Long): Long = (nowMs - seenAtMs).coerceAtLeast(0)
}

/**
 * Remembers the most recent known case level across beacons, service
 * restarts and reboots.
 *
 * Pure logic over a tiny [Storage] seam so it is unit-testable; the
 * SharedPreferences adapter is the only Android-specific part.
 */
class CaseMemory(
    private val storage: Storage,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    interface Storage {
        fun read(): LastCaseReading?
        fun write(reading: LastCaseReading)
    }

    private var cached: LastCaseReading? = storage.read()

    val last: LastCaseReading? get() = cached

    /**
     * Fold one decoded beacon in.
     *
     * @return the reading to show for the case: the live one when the beacon
     *   carries it, otherwise the remembered one, otherwise null.
     */
    fun observe(state: PodsState): LastCaseReading? {
        val live = state.caseBattery ?: return cached
        val reading = LastCaseReading(live, state.isCaseCharging, nowMs())
        cached = reading
        storage.write(reading)
        return reading
    }

    companion object {
        fun persistent(context: Context): CaseMemory =
            CaseMemory(PrefsStorage(context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)))

        private const val PREFS_NAME = "case_memory"
    }

    private class PrefsStorage(private val prefs: SharedPreferences) : Storage {
        override fun read(): LastCaseReading? {
            if (!prefs.contains(KEY_LEVEL)) return null
            return LastCaseReading(
                level = prefs.getInt(KEY_LEVEL, 0),
                isCharging = prefs.getBoolean(KEY_CHARGING, false),
                seenAtMs = prefs.getLong(KEY_SEEN_AT, 0L),
            )
        }

        override fun write(reading: LastCaseReading) {
            prefs.edit {
                putInt(KEY_LEVEL, reading.level)
                putBoolean(KEY_CHARGING, reading.isCharging)
                putLong(KEY_SEEN_AT, reading.seenAtMs)
            }
        }

        private companion object {
            const val KEY_LEVEL = "level"
            const val KEY_CHARGING = "charging"
            const val KEY_SEEN_AT = "seen_at"
        }
    }
}
