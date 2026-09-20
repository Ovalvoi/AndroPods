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
 * Environment probe, not a product test: does the controller's *batch* scan
 * path deliver advertisements when the regular per-packet path does not?
 *
 * Reproduces the recipe MaterialPods (a working AirPods app) was observed to
 * use in its bytecode -- SCAN_MODE_BALANCED, setReportDelay(4000), and an
 * Apple manufacturer-data filter of 27 zero bytes with a zero mask -- then
 * varies one thing at a time. Batch results arrive via onBatchScanResults
 * from the controller's own result storage, a different firmware path from
 * onScanResult.
 *
 *   adb shell am instrument -w -e seconds 40 \
 *       -e class com.ovalvoi.andropods.BatchScanProbeTest \
 *       com.ovalvoi.andropods.test/androidx.test.runner.AndroidJUnitRunner
 */
class BatchScanProbeTest {

    private var phaseSeconds = 40L

    @Test
    fun probeBatchScanning() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        phaseSeconds = InstrumentationRegistry.getArguments()
            .getString("seconds")?.toLongOrNull() ?: phaseSeconds
        val scanner = context.getSystemService(BluetoothManager::class.java).adapter.bluetoothLeScanner

        val appleAny27 = ScanFilter.Builder()
            .setManufacturerData(0x004C, ByteArray(27), ByteArray(27))
            .build()
        val appleType07 = ScanFilter.Builder()
            .setManufacturerData(0x004C, byteArrayOf(0x07), byteArrayOf(0xFF.toByte()))
            .build()

        fun settings(mode: Int, delayMs: Long) = ScanSettings.Builder()
            .setScanMode(mode)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setReportDelay(delayMs)
            .build()

        runPhase(scanner, "A: MaterialPods exact -- BALANCED, batch 4000ms, Apple 27-zero filter",
            settings(ScanSettings.SCAN_MODE_BALANCED, 4000), listOf(appleAny27))
        runPhase(scanner, "B: batch 4000ms, LOW_LATENCY, no filter",
            settings(ScanSettings.SCAN_MODE_LOW_LATENCY, 4000), emptyList())
        runPhase(scanner, "C: batch 4000ms, BALANCED, Apple type-07 filter (full mask)",
            settings(ScanSettings.SCAN_MODE_BALANCED, 4000), listOf(appleType07))
        runPhase(scanner, "D: control -- BALANCED, no batch, Apple 27-zero filter",
            settings(ScanSettings.SCAN_MODE_BALANCED, 0), listOf(appleAny27))
    }

    private fun runPhase(
        scanner: BluetoothLeScanner,
        label: String,
        settings: ScanSettings,
        filters: List<ScanFilter>,
    ) {
        var direct = 0
        var batched = 0
        var batches = 0
        var apple = 0
        var pods = 0
        val seen = mutableSetOf<String>()
        val latch = CountDownLatch(1)

        fun record(result: ScanResult, via: String) {
            seen += result.device.address
            val raw = result.scanRecord?.bytes ?: return
            val hex = raw.joinToString("") { "%02X".format(it) }
            if (!hex.contains("FF4C00")) return
            apple++
            val isPods = hex.contains("FF4C0007")
            if (isPods) pods++
            if (apple <= 12 || isPods && pods <= 12) {
                Log.i(TAG, "$via ${if (isPods) "PODS" else "APPLE"} rssi=${result.rssi} $hex")
            }
        }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                direct++
                record(result, "direct")
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                batches++
                batched += results.size
                Log.i(TAG, "BATCH #$batches size=${results.size}")
                results.forEach { record(it, "batch") }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "SCAN FAILED code=$errorCode")
                latch.countDown()
            }
        }

        Log.i(TAG, "=== PHASE START [$label] ${phaseSeconds}s ===")
        try {
            scanner.startScan(filters, settings, callback)
        } catch (e: SecurityException) {
            Log.e(TAG, "startScan threw", e)
            return
        }
        latch.await(phaseSeconds, TimeUnit.SECONDS)
        // Flush whatever the controller still holds before stopping.
        runCatching { scanner.flushPendingScanResults(callback) }
        Thread.sleep(500)
        scanner.stopScan(callback)
        Log.i(TAG, "=== PHASE RESULT [$label]: batches=$batches batched=$batched direct=$direct " +
            "distinct=${seen.size} apple=$apple pods=$pods ===")
    }

    private companion object {
        const val TAG = "BatchScanProbe"
    }
}
