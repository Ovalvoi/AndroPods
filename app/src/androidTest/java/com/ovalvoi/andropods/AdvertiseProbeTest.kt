package com.ovalvoi.andropods

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Environment probe, not a product test: turn the phone into a known BLE
 * transmitter so a second receiver (e.g. a PC scanning next to it) can tell
 * whether the phone's LE radio transmits at all. Pairs with RawScanProbeTest,
 * which answers the receive-side question.
 *
 *   adb shell am instrument -w -e seconds 40 \
 *       -e class com.ovalvoi.andropods.AdvertiseProbeTest \
 *       com.ovalvoi.andropods.test/androidx.test.runner.AndroidJUnitRunner
 *
 * Payload: company 0xFFFF, "PIXELPROBE". Non-connectable, high power, fastest
 * legal interval, so any working receiver within a few metres sees it.
 */
class AdvertiseProbeTest {

    @Test
    fun advertiseForAWhile() {
        // The test package's own context: BLUETOOTH_ADVERTISE is declared by the
        // instrumentation manifest, not the app's, and a check against the
        // app's package would report it missing even though the shared UID
        // holds it.
        val context = InstrumentationRegistry.getInstrumentation().context
        val seconds = InstrumentationRegistry.getArguments()
            .getString("seconds")?.toLongOrNull() ?: 30L
        val adapter = context.getSystemService(BluetoothManager::class.java).adapter
        val advertiser = adapter.bluetoothLeAdvertiser

        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_ADVERTISE,
        ) == PackageManager.PERMISSION_GRANTED
        Log.i(TAG, "advertiser=${advertiser != null} BLUETOOTH_ADVERTISE granted=$granted (advisory only)")
        if (advertiser == null) {
            Log.e(TAG, "no advertiser")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .setTimeout(0)
            .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addManufacturerData(0xFFFF, "PIXELPROBE".toByteArray(Charsets.US_ASCII))
            .build()

        val started = CountDownLatch(1)
        val callback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                Log.i(TAG, "ADVERTISING STARTED mode=${settingsInEffect.mode} tx=${settingsInEffect.txPowerLevel}")
                started.countDown()
            }

            override fun onStartFailure(errorCode: Int) {
                Log.e(TAG, "ADVERTISING FAILED code=$errorCode")
                started.countDown()
            }
        }

        try {
            advertiser.startAdvertising(settings, data, callback)
        } catch (e: SecurityException) {
            Log.e(TAG, "startAdvertising threw SecurityException", e)
            return
        }
        started.await(5, TimeUnit.SECONDS)
        Log.i(TAG, "=== ADVERTISING for ${seconds}s ===")
        Thread.sleep(seconds * 1000)
        advertiser.stopAdvertising(callback)
        Log.i(TAG, "=== ADVERTISING STOPPED ===")
    }

    private companion object {
        const val TAG = "AdvertiseProbe"
    }
}
