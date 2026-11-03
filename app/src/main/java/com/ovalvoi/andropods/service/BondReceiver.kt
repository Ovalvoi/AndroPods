package com.ovalvoi.andropods.service

import android.Manifest
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.ovalvoi.andropods.data.PodsRepository
import com.ovalvoi.andropods.data.SettingsStore

/**
 * The name the user gave these pods in Android's Bluetooth settings.
 *
 * Worth showing because it is the only thing that identifies *whose* pods
 * these are: every Gen 2 broadcasts the same model ID, so the model name
 * alone cannot tell one pair from another. Null when the name is unset or
 * BLUETOOTH_CONNECT was revoked mid-flight, in which case the UI falls back
 * to the model name.
 */
private fun BluetoothDevice.readableName(): String? = try {
    // alias first, then name. getName() is the name the device *broadcasts*
    // -- every pair of Gen 2 AirPods says "AirPods" -- while a rename in
    // Android's Bluetooth settings is stored separately as the alias. Reading
    // only the name meant a renamed pair still showed the generic one.
    (alias ?: name)?.takeIf { it.isNotBlank() }
} catch (e: SecurityException) {
    Log.w("BondReceiver", "BLUETOOTH_CONNECT revoked while reading device name", e)
    null
}

/**
 * Starts and stops [PodsService] as the AirPods connect and disconnect over
 * classic Bluetooth.
 *
 * The classic ACL link is the app's identity anchor. The BLE beacon cannot
 * prove whose pods it came from -- the address rotates and the model ID is
 * shared by every Gen 2 in existence -- but a classic connection to a *bonded*
 * device is unambiguous. Scanning only while that link is up is what keeps the
 * readout trustworthy, and keeps the radio off the rest of the time.
 *
 * Registered twice, on purpose:
 *
 * - In the manifest, so the system wakes the app on a connect even after its
 *   process was killed (swiped away, reclaimed for memory, after a reboot).
 *   ACL and A2DP connection-state broadcasts are on Android's exemption list
 *   for implicit broadcasts, so this costs nothing while idle: no process, no
 *   service, no wakeups -- the app only runs when a Bluetooth link changes.
 * - At runtime from the Application, the path that was verified on-device,
 *   kept as a fallback for any build that is stingy with manifest delivery.
 *
 * While the process is alive both copies see the same broadcast, so
 * [isDuplicate] drops the second delivery.
 */
class BondReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val device = IntentCompat.getDevice(intent) ?: return
        if (isDuplicate(intent, device)) return
        if (!hasConnectPermission(context)) return
        if (!device.isAirPods()) return

        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                Log.d(TAG, "AirPods connected; starting service")
                PodsRepository.onDeviceName(device.readableName())
                PodsService.start(context)
            }

            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                Log.d(TAG, "AirPods disconnected; stopping service")
                PodsService.stop(context)
            }

            // The A2DP profile, not the ACL link, is the moment audio can
            // actually reach the pods; ACL comes up a few seconds earlier.
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                if (state == BluetoothProfile.STATE_CONNECTED && SettingsStore.settings.value.resumeMusicOnConnect) {
                    Log.d(TAG, "A2DP connected; resuming music")
                    MediaResumer.resumeAfterAudioConnected(context)
                }
            }
        }
    }

    private fun hasConnectPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    private object IntentCompat {
        @Suppress("DEPRECATION")
        fun getDevice(intent: Intent): BluetoothDevice? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }
    }

    companion object {
        private const val TAG = "BondReceiver"

        /**
         * Longer than the gap between the manifest and runtime deliveries of
         * one broadcast (milliseconds), far shorter than a real disconnect and
         * reconnect.
         */
        private const val DUPLICATE_WINDOW_MS = 2_000L

        /** Process-wide: the two receiver instances share it. Main thread only. */
        private val lastSeen = HashMap<String, Long>()

        /**
         * True when this exact event was already handled by the other
         * registration. Keyed on the profile state too, so A2DP connecting
         * and disconnecting are never mistaken for each other.
         */
        private fun isDuplicate(intent: Intent, device: BluetoothDevice): Boolean {
            val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
            val key = "${intent.action}|${device.address}|$state"
            val now = SystemClock.elapsedRealtime()
            val previous = lastSeen.put(key, now)
            return previous != null && now - previous < DUPLICATE_WINDOW_MS
        }

        /**
         * Start the service if bonded AirPods are *already* connected.
         *
         * [BondReceiver] only fires on the connect edge, so without this a cold
         * start -- first install, app reopened, or a reboot -- with the pods
         * already in your ears would show nothing until you reconnected them.
         */
        /**
         * Apple's AirPods/Beats service UUID (the AAP control channel), listed
         * in the SDP records every AirPods model publishes. Other headphones,
         * car kits and speakers do not carry it.
         */
        private val AIRPODS_UUID = ParcelUuid.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a")

        /**
         * Filter to bonded AirPods, so a car, speaker or other headset never
         * starts the service (and its notification).
         *
         * Primary signal is the AirPods SDP UUID. The UUID list is cached at
         * pairing but can be missing on some stacks, so the broadcast name is
         * a fallback -- getName(), not the alias, so a pair renamed in
         * Bluetooth settings still matches.
         *
         * These reads require BLUETOOTH_CONNECT. The caller checks it first,
         * but the grant can be revoked in between, so the SecurityException
         * is caught rather than assumed away.
         */
        private fun BluetoothDevice.isAirPods(): Boolean = try {
            bondState == BluetoothDevice.BOND_BONDED &&
                bluetoothClass?.majorDeviceClass ==
                android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO &&
                (uuids?.contains(AIRPODS_UUID) == true ||
                    name?.contains("AirPods", ignoreCase = true) == true)
        } catch (e: SecurityException) {
            Log.w(TAG, "BLUETOOTH_CONNECT revoked while inspecting device", e)
            false
        }

        fun startIfAlreadyConnected(context: Context) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) !=
                PackageManager.PERMISSION_GRANTED
            ) return

            val manager = context.getSystemService(BluetoothManager::class.java) ?: return
            val connected = try {
                // GATT is the only connected-device list reachable without a
                // profile proxy; for classic audio devices we fall back to
                // checking bonded devices for an active ACL link.
                manager.adapter?.bondedDevices.orEmpty().filter { device ->
                    device.isAirPods() && device.isConnectedCompat()
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "BLUETOOTH_CONNECT revoked while checking bonded devices", e)
                emptyList()
            }

            Log.d(TAG, "bonded AirPods connected: ${connected.size}")
            if (connected.isNotEmpty()) {
                Log.d(TAG, "AirPods already connected at startup; starting service")
                PodsRepository.onDeviceName(connected.first().readableName())
                PodsService.start(context)
            }
        }

        /**
         * There is no public isConnected(); the hidden method is the only way
         * to ask. Reflection failing is not fatal -- we simply do not
         * early-start, and the next connect edge starts us normally.
         */
        private fun BluetoothDevice.isConnectedCompat(): Boolean = try {
            val connected = javaClass.getMethod("isConnected").invoke(this) as? Boolean ?: false
            Log.d(TAG, "isConnected($address) = $connected")
            connected
        } catch (e: Exception) {
            Log.w(TAG, "isConnected() unavailable on this build", e)
            false
        }

        fun register(context: Context): BondReceiver {
            val receiver = BondReceiver()
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            }
            // EXPORTED is required, not optional. ACL broadcasts are sent by
            // the Bluetooth process (uid 1002), which is not the system uid, so
            // a NOT_EXPORTED receiver never sees them: on Android 14+ the
            // service was never started or stopped on connect/disconnect.
            // Verified on-device -- four ACL transitions, zero callbacks. Both
            // actions are protected broadcasts, so nothing else can spoof them.
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED,
            )
            return receiver
        }
    }
}
