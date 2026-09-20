package com.ovalvoi.andropods

import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.ovalvoi.andropods.ble.ProximityPayload
import org.junit.Test

/**
 * Environment probe, not a product test: dump every proximity-pairing frame
 * with the address it came from and the parser's reading of it, so the two
 * pods' frames can be compared side by side. Used to check that left/right
 * follow the physical bud regardless of which pod is broadcasting.
 *
 *   adb shell am instrument -w -e seconds 60 \
 *       -e class com.ovalvoi.andropods.FrameDumpProbeTest \
 *       com.ovalvoi.andropods.test/androidx.test.runner.AndroidJUnitRunner
 */
class FrameDumpProbeTest {

    @Test
    fun dumpFrames() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val seconds = InstrumentationRegistry.getArguments().getString("seconds")?.toLongOrNull() ?: 60L
        val scanner = context.getSystemService(BluetoothManager::class.java).adapter.bluetoothLeScanner

        val callback = object : ScanCallback() {
            override fun onBatchScanResults(results: MutableList<ScanResult>) = results.forEach(::dump)
            override fun onScanResult(callbackType: Int, result: ScanResult) = dump(result)
            override fun onScanFailed(errorCode: Int) { Log.e(TAG, "SCAN FAILED $errorCode") }
        }

        scanner.startScan(
            listOf(ScanFilter.Builder().setManufacturerData(0x004C, byteArrayOf(0x07), byteArrayOf(0xFF.toByte())).build()),
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).setReportDelay(5000).build(),
            callback,
        )
        Log.i(TAG, "=== DUMP START ${seconds}s ===")
        Thread.sleep(seconds * 1000)
        scanner.stopScan(callback)
        Log.i(TAG, "=== DUMP END ===")
    }

    private fun dump(result: ScanResult) {
        val raw = result.scanRecord?.bytes ?: return
        val hex = raw.joinToString("") { "%02X".format(it) }
        val at = hex.indexOf("FF4C0007")
        if (at < 0) return
        val p = hex.substring(at + 8)   // from the 0x07 type byte
        if (p.length < 18) return
        val parsed = ProximityPayload.parseRawScanRecord(raw)
        Log.i(
            TAG,
            "addr=${result.device.address} rssi=${result.rssi} " +
                "status=${p.substring(10, 12)} pods=${p.substring(12, 14)} flagsCase=${p.substring(14, 16)} lid=${p.substring(16, 18)}" +
                "  -> L=${parsed?.leftBattery} R=${parsed?.rightBattery} case=${parsed?.caseBattery} " +
                "chgL=${parsed?.isLeftCharging} chgR=${parsed?.isRightCharging} inEarL=${parsed?.rawLeftInEar} inEarR=${parsed?.rawRightInEar}",
        )
    }

    private companion object {
        const val TAG = "FrameDump"
    }
}
