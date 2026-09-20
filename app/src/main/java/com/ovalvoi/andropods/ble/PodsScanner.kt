package com.ovalvoi.andropods.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Wraps the BLE scanner and emits decoded AirPods state.
 *
 * Two details here are load-bearing and easy to get wrong:
 *
 * 1. Results are decoded from [ScanResult.getScanRecord]'s *raw bytes*, not
 *    from getManufacturerSpecificData(). See [ProximityPayload.parseRawScanRecord].
 *
 * 2. The hardware filter matches Apple's company ID and the 0x07 type byte
 *    only -- never the length byte, which varies across firmware. A filter is
 *    mandatory rather than an optimisation: since Android 8.1 the platform
 *    stops unfiltered scans outright when the screen is off.
 */
class PodsScanner(context: Context) {

    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)
            ?.adapter

    val isBluetoothEnabled: Boolean get() = adapter?.isEnabled == true

    /**
     * Emit decoded state for every matching advertisement.
     *
     * Cold flow: scanning starts on collection and stops on cancellation, so
     * the radio is never left running past the service that owns it.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun advertisements(scanMode: Int = ScanSettings.SCAN_MODE_BALANCED): Flow<Advertisement> =
        callbackFlow {
            val scanner = adapter?.bluetoothLeScanner
            if (scanner == null) {
                Log.w(TAG, "No BLE scanner; Bluetooth is off or unavailable")
                close()
                return@callbackFlow
            }

            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    val raw = result.scanRecord?.bytes ?: return
                    if (DEBUG_RAW) {
                        val hex = raw.joinToString("") { "%02X".format(it) }
                        if (hex.contains("FF4C00")) {
                            Log.d(TAG, "APPLE rssi=${result.rssi} raw=$hex")
                        } else {
                            Log.d(TAG, "ADV ${result.device.address} rssi=${result.rssi} $hex")
                        }
                    }
                    val state = ProximityPayload.parseRawScanRecord(raw) ?: return
                    trySend(Advertisement(result.device.address, result.rssi, state))
                }

                // The primary delivery path (see BATCH_REPORT_DELAY_MS). The
                // controller de-duplicates within a window, so expect about one
                // frame per advertiser per window, not a stream.
                override fun onBatchScanResults(results: MutableList<ScanResult>) {
                    results.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
                }

                override fun onScanFailed(errorCode: Int) {
                    Log.e(TAG, "BLE scan failed: $errorCode")
                    close(ScanFailedException(errorCode))
                }
            }

            // Batch delivery is the load-bearing setting, not an optimisation.
            // On the Pixel 7 / Android 17 test device the controller's regular
            // per-packet result path delivers nothing at all -- not one frame
            // from pods touching the phone, in any scan mode, on any PHY, with
            // or without a filter -- while its batch path delivers the pods'
            // full frame at -45 dBm every window. Verified with an eight-way
            // matrix (MatrixScanProbeTest) that varied one setting at a time:
            // report delay was the only one that mattered. MaterialPods works
            // on the same phone for exactly this reason.
            //
            // Everything else is left at defaults on purpose. The beacon is a
            // legacy 1M advertisement, so an extended all-PHY scan only splits
            // scan time across PHYs it will never see it on.
            val settings = ScanSettings.Builder()
                .setScanMode(scanMode)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setReportDelay(BATCH_REPORT_DELAY_MS)
                .build()

            // Always filtered, in every mode. An unfiltered scan is suppressed
            // by the platform when the screen is off (Android 8.1+), which
            // silently starves the foreground service. See DEBUG_RAW.
            val filters = listOf(appleProximityFilter())

            // startScan throws SecurityException synchronously when the caller
            // lacks BLUETOOTH_SCAN -- onScanFailed is never invoked for this, so
            // it must be caught here or the failure is invisible.
            try {
                scanner.startScan(filters, settings, callback)
            } catch (e: SecurityException) {
                Log.e(TAG, "startScan denied: BLUETOOTH_SCAN not held", e)
                close(e)
                return@callbackFlow
            }
            Log.d(TAG, "Scan started, mode=$scanMode")

            awaitClose {
                runCatching { scanner.stopScan(callback) }
                    .onFailure { Log.w(TAG, "stopScan failed", it) }
                Log.d(TAG, "Scan stopped")
            }
        }

    /**
     * Match Apple manufacturer data whose first byte is 0x07.
     *
     * The mask is one byte long, covering the type byte alone: the length byte
     * at index 1 differs between firmware generations and matching it would
     * silently drop valid frames.
     *
     * The mask VALUE must be 0xFF. A mask is bitwise -- 0x01 compares only the
     * lowest bit, which let every odd Apple type (0x01, 0x05, 0x0F, 0x13 ...)
     * through the hardware filter and relied on the parser to reject them.
     */
    private fun appleProximityFilter(): ScanFilter = ScanFilter.Builder()
        .setManufacturerData(
            ProximityPayload.APPLE_COMPANY_ID,
            byteArrayOf(ProximityPayload.TYPE_PROXIMITY_PAIRING),
            byteArrayOf(0xFF.toByte()),
        )
        .build()

    data class Advertisement(
        val address: String,
        val rssi: Int,
        val state: PodsState,
    )

    class ScanFailedException(val errorCode: Int) :
        RuntimeException("BLE scan failed with code $errorCode")

    private companion object {
        const val TAG = "PodsScanner"

        /**
         * Batch window. The platform rounds anything shorter up to 5 s, so ask
         * for 5 s explicitly rather than pretend a smaller number means more.
         * One frame per window is plenty for a battery readout, and the
         * controller does the buffering, so the AP sleeps between windows.
         */
        const val BATCH_REPORT_DELAY_MS = 5_000L

        /**
         * Bring-up diagnostics: log the raw hex of every advertisement that
         * passes the hardware filter.
         *
         * This used to also drop the ScanFilter entirely, which is never safe
         * from a service: since Android 8.1 the platform suppresses UNFILTERED
         * scans while the screen is off. The filter is now applied
         * unconditionally, so enabling this only adds logging. For a truly
         * unfiltered look at the air, use RawScanProbeTest instead.
         */
        const val DEBUG_RAW = false
    }
}
