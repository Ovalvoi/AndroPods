package com.ovalvoi.andropods

import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Environment probe, not a product test: which scan configuration makes this
 * phone deliver the pods' beacon? Starts from the configuration a working app
 * (MaterialPods) was observed using and changes ONE variable per phase toward
 * the configuration AndroPods uses, so the variable that kills reception
 * stands out.
 *
 *   adb shell am force-stop com.pryshedko.materialpods   # no merged scans
 *   adb shell am instrument -w -e seconds 15 \
 *       -e class com.ovalvoi.andropods.MatrixScanProbeTest \
 *       com.ovalvoi.andropods.test/androidx.test.runner.AndroidJUnitRunner
 */
class MatrixScanProbeTest {

    private class Config(
        val label: String,
        val mode: Int,
        val delayMs: Long,
        val legacy: Boolean?,          // null = leave builder defaults alone
        val phyAll: Boolean,
        val maxMatches: Boolean,
        val filters: List<ScanFilter>,
    )

    @Test
    fun probeMatrix() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val seconds = InstrumentationRegistry.getArguments()
            .getString("seconds")?.toLongOrNull() ?: 15L
        val scanner = context.getSystemService(BluetoothManager::class.java).adapter.bluetoothLeScanner

        val appleAny27 = ScanFilter.Builder()
            .setManufacturerData(0x004C, ByteArray(27), ByteArray(27)).build()
        val appleType07 = ScanFilter.Builder()
            .setManufacturerData(0x004C, byteArrayOf(0x07), byteArrayOf(0xFF.toByte())).build()

        val bal = ScanSettings.SCAN_MODE_BALANCED
        val low = ScanSettings.SCAN_MODE_LOW_LATENCY
        val configs = listOf(
            Config("1 MaterialPods exact: BALANCED batch5000 legacy/1M any27", bal, 5000, null, false, false, listOf(appleAny27)),
            Config("2 ...but LOW_LATENCY",                                     low, 5000, null, false, false, listOf(appleAny27)),
            Config("3 ...but regular (delay 0)",                              bal, 0,    null, false, false, listOf(appleAny27)),
            Config("4 ...but legacy=false + PHY ALL",                         bal, 5000, false, true, false, listOf(appleAny27)),
            Config("5 ...but type-07/FF filter",                              bal, 5000, null, false, false, listOf(appleType07)),
            Config("6 ...but MATCH_NUM_MAX + AGGRESSIVE",                     bal, 5000, null, false, true, listOf(appleAny27)),
            Config("7 AndroPods exact: LOW_LATENCY delay0 legacy=false ALL MAX type07", low, 0, false, true, true, listOf(appleType07)),
            Config("8 unfiltered BALANCED regular",                            bal, 0,    null, false, false, emptyList()),
        )
        for (c in configs) runPhase(scanner, c, seconds)
    }

    private fun runPhase(scanner: BluetoothLeScanner, c: Config, seconds: Long) {
        val settings = ScanSettings.Builder()
            .setScanMode(c.mode)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setReportDelay(c.delayMs)
            .apply {
                if (c.legacy != null) setLegacy(c.legacy)
                if (c.phyAll) setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
                if (c.maxMatches) {
                    setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                    setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                }
            }
            .build()

        var total = 0
        var apple = 0
        var full = 0      // Apple manufacturer data >= 27 bytes (the battery frame)
        var type07 = 0
        var logged = 0
        val latch = CountDownLatch(1)

        fun record(r: ScanResult) {
            total++
            val raw = r.scanRecord?.bytes ?: return
            val hex = raw.joinToString("") { "%02X".format(it) }
            val at = hex.indexOf("FF4C00")
            if (at < 0) return
            apple++
            // AD length byte sits two hex chars before the FF; length includes the type byte.
            val adLen = hex.substring(at - 2, at).toInt(16)
            if (adLen - 3 >= 27) full++
            if (hex.startsWith("07", at + 6)) type07++
            if (logged++ < 6) Log.i(TAG, "  rssi=${r.rssi} len=${adLen - 3} $hex")
        }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) = record(result)
            override fun onBatchScanResults(results: MutableList<ScanResult>) { results.forEach { record(it) } }
            override fun onScanFailed(errorCode: Int) { Log.e(TAG, "SCAN FAILED $errorCode"); latch.countDown() }
        }

        Log.i(TAG, "=== START [${c.label}] ${seconds}s ===")
        try {
            scanner.startScan(c.filters, settings, callback)
        } catch (e: SecurityException) {
            Log.e(TAG, "startScan threw", e); return
        }
        latch.await(seconds, TimeUnit.SECONDS)
        if (c.delayMs > 0) { runCatching { scanner.flushPendingScanResults(callback) }; Thread.sleep(500) }
        scanner.stopScan(callback)
        Log.i(TAG, "=== RESULT [${c.label}]: total=$total apple=$apple full27=$full type07=$type07 ===")
        Thread.sleep(1000)
    }

    private companion object {
        const val TAG = "MatrixScanProbe"
    }
}
