package com.ovalvoi.andropods

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Environment probe, not a product test.
 *
 * Runs the barest possible BLE scans -- no filters, no service, none of the
 * app's own logic -- purely to establish whether this device delivers ANY
 * advertisement. If this sees nothing, no amount of work on PodsScanner can
 * help, and the problem is the phone or its surroundings.
 *
 * Two phases, because scan *settings* can themselves suppress results on some
 * controllers: first the app's aggressive extended-PHY settings, then the
 * plainest legacy-only scan Android offers. Disagreement between the two
 * phases points at the settings; agreement on zero points at the radio.
 *
 * Run it with the permission guaranteed, or it measures nothing:
 *   adb install -r -g app-debug.apk && adb install -r -g app-debug-androidTest.apk
 *   adb shell am instrument -w -e class com.ovalvoi.andropods.RawScanProbeTest \
 *       com.ovalvoi.andropods.test/androidx.test.runner.AndroidJUnitRunner
 * Gradle's connectedAndroidTest reinstalls the app, which WIPES runtime grants,
 * so a probe launched that way can silently run without BLUETOOTH_SCAN.
 */
class RawScanProbeTest {

    @Test
    fun probeRawAdvertisements() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val adapter = context.getSystemService(BluetoothManager::class.java).adapter
        // Override with: am instrument ... -e seconds 60
        phaseSeconds = InstrumentationRegistry.getArguments()
            .getString("seconds")?.toLongOrNull() ?: DEFAULT_PHASE_SECONDS
        val scanner = adapter.bluetoothLeScanner

        val scanGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_SCAN,
        ) == PackageManager.PERMISSION_GRANTED
        Log.i(TAG, "adapter enabled=${adapter.isEnabled} scanner=${scanner != null} BLUETOOTH_SCAN granted=$scanGranted")
        if (!scanGranted) {
            Log.e(TAG, "BLUETOOTH_SCAN not granted -- results below are meaningless")
        }

        val aggressive = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setLegacy(false)
            .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .build()

        val plain = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        runPhase(scanner, "AGGRESSIVE extended-PHY, MAX matches", aggressive)
        runPhase(scanner, "PLAIN legacy-only, defaults", plain)
    }

    private fun runPhase(scanner: BluetoothLeScanner, label: String, settings: ScanSettings) {
        var total = 0
        var apple = 0
        var failed = -1
        val seen = mutableSetOf<String>()
        val latch = CountDownLatch(1)

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                total++
                seen += result.device.address
                val raw = result.scanRecord?.bytes
                if (raw != null) {
                    val hex = raw.joinToString("") { "%02X".format(it) }
                    if (hex.contains("FF4C00")) {
                        apple++
                        if (apple <= 10) Log.i(TAG, "APPLE rssi=${result.rssi} $hex")
                    }
                }
                if (total <= 5) {
                    Log.i(TAG, "ADV #$total ${result.device.address} rssi=${result.rssi}")
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                Log.i(TAG, "BATCH of ${results.size}")
                results.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
            }

            override fun onScanFailed(errorCode: Int) {
                failed = errorCode
                Log.e(TAG, "SCAN FAILED code=$errorCode")
                latch.countDown()
            }
        }

        Log.i(TAG, "=== PHASE START [$label]: ${phaseSeconds}s, no filter ===")
        try {
            scanner.startScan(emptyList(), settings, callback)
        } catch (e: SecurityException) {
            Log.e(TAG, "startScan threw SecurityException", e)
            return
        }
        latch.await(phaseSeconds, TimeUnit.SECONDS)
        scanner.stopScan(callback)

        Log.i(TAG, "=== PHASE RESULT [$label]: total=$total distinct=${seen.size} apple=$apple failed=$failed ===")
    }

    private var phaseSeconds = DEFAULT_PHASE_SECONDS

    private companion object {
        const val TAG = "RawScanProbe"
        const val DEFAULT_PHASE_SECONDS = 20L
    }
}
