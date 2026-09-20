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
import android.util.Log
import androidx.core.content.ContextCompat
import com.ovalvoi.andropods.data.SettingsStore

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
 * Registered at runtime from the Application rather than the manifest: these
 * are implicit broadcasts, and manifest registration for them is unreliable on
 * modern Android.
 */
class BondReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val device = IntentCompat.getDevice(intent) ?: return
        if (!hasConnectPermission(context)) return
        if (!isAudioDevice(device)) return

        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                Log.d(TAG, "Audio device connected; starting service")
                PodsService.start(context)
            }

            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                Log.d(TAG, "Audio device disconnected; stopping service")
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

    /**
     * Filter to bonded audio devices.
     *
     * Deliberately not matched on name: it is user-editable and localised. The
     * BLE beacon's model ID confirms the pods are Gen 2 later; this check only
     * needs to avoid waking for a keyboard or a car.
     *
     * Both property reads below require BLUETOOTH_CONNECT. The caller checks it
     * first, but the grant can be revoked between that check and these reads,
     * so the SecurityException is caught rather than assumed away.
     */
    private fun isAudioDevice(device: BluetoothDevice): Boolean = try {
        device.bondState == BluetoothDevice.BOND_BONDED &&
            device.bluetoothClass?.majorDeviceClass ==
            android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO
    } catch (e: SecurityException) {
        Log.w(TAG, "BLUETOOTH_CONNECT revoked while inspecting device", e)
        false
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
         * Start the service if bonded audio pods are *already* connected.
         *
         * [BondReceiver] only fires on the connect edge, so without this a cold
         * start -- first install, app reopened, or a reboot -- with the pods
         * already in your ears would show nothing until you reconnected them.
         */
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
                    device.bluetoothClass?.majorDeviceClass ==
                        android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO &&
                        device.isConnectedCompat()
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "BLUETOOTH_CONNECT revoked while checking bonded devices", e)
                emptyList()
            }

            Log.d(TAG, "bonded audio devices connected: ${connected.size}")
            if (connected.isNotEmpty()) {
                Log.d(TAG, "Audio device already connected at startup; starting service")
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
