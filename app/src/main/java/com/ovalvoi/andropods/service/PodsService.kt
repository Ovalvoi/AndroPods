package com.ovalvoi.andropods.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.bluetooth.le.ScanSettings
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.ovalvoi.andropods.ble.PodsScanner
import com.ovalvoi.andropods.ble.PodsState
import com.ovalvoi.andropods.ble.PodsTracker
import com.ovalvoi.andropods.data.CaseMemory
import com.ovalvoi.andropods.data.LastCaseReading
import com.ovalvoi.andropods.data.PodsRepository
import com.ovalvoi.andropods.data.SettingsStore
import com.ovalvoi.andropods.ui.overlay.ConnectPopupHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn

/**
 * Foreground service that scans while the AirPods are connected.
 *
 * Lifecycle is driven entirely by [BondReceiver]: started on ACL connect,
 * stopped on ACL disconnect. It deliberately does not run while the pods are
 * away -- a permanently resident scanner is the main way apps like this drain a
 * battery, and there is nothing to report when the pods are in a bag.
 *
 * Android 14+ requires the connectedDevice foreground-service type (declared in
 * the manifest) and BLUETOOTH_CONNECT held at the moment startForeground runs,
 * or the platform throws.
 */
class PodsService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val tracker = PodsTracker()
    private lateinit var scanner: PodsScanner
    private lateinit var caseMemory: CaseMemory
    private lateinit var popupHost: ConnectPopupHost

    private var scanJob: Job? = null
    private var wasLidOpen = false

    /**
     * Whether the connect popup has already been shown for this connection.
     *
     * The service is started on the ACL connect edge and destroyed on
     * disconnect, so an instance field is exactly one connection's worth of
     * memory -- no reset logic, and reconnecting shows the popup again.
     */
    private var hasShownConnectPopup = false

    override fun onCreate() {
        super.onCreate()
        scanner = PodsScanner(this)
        caseMemory = CaseMemory.persistent(this)
        popupHost = ConnectPopupHost(this)
        PodsNotifications.ensureChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT) ||
            !hasPermission(Manifest.permission.BLUETOOTH_SCAN)
        ) {
            // Permission revoked while we were away. Stop cleanly rather than
            // letting startForeground throw.
            Log.w(TAG, "Missing Bluetooth permissions; stopping")
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundSafely()
        PodsRepository.onSearching()
        startScanning()

        // Not sticky: restarting without a live ACL connection would scan for
        // pods that are not there. BondReceiver restarts us when they return.
        return START_NOT_STICKY
    }

    private fun startForegroundSafely() {
        val notification = PodsNotifications.ongoing(this, null)
        try {
            ServiceCompat.startForeground(
                this,
                PodsNotifications.ONGOING_NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                } else {
                    0
                },
            )
        } catch (e: Exception) {
            // Covers ForegroundServiceStartNotAllowedException (API 31+) and
            // the API 34 type/permission mismatch. Neither is recoverable here.
            Log.e(TAG, "startForeground rejected", e)
            stopSelf()
        }
    }

    /**
     * BLUETOOTH_SCAN is verified in onStartCommand, but can be revoked between
     * that check and the scan actually starting, so the SecurityException is
     * handled rather than assumed away.
     */
    @SuppressLint("MissingPermission")
    private fun startScanning() {
        if (scanJob?.isActive == true) return

        // BALANCED is enough. An earlier note here claimed only LOW_LATENCY
        // delivered results on this hardware; that was measured on the broken
        // per-packet path. With batch delivery (see PodsScanner) BALANCED and
        // LOW_LATENCY yield the same one frame per window, so take the cheaper
        // duty cycle.
        scanJob = scanner.advertisements(ScanSettings.SCAN_MODE_BALANCED)
            .onEach { advertisement ->
                val state = tracker.onAdvertisement(
                    advertisement.address,
                    advertisement.rssi,
                    advertisement.state,
                ) ?: return@onEach

                val lastCase = caseMemory.observe(state)
                PodsRepository.onState(state, lastCase)
                showConnectPopupOnce(state, lastCase)
                notifyLidTransition(state, lastCase)
                updateOngoing(state, lastCase)
            }
            .catch { cause ->
                Log.e(TAG, "Scan flow failed", cause)
                if (cause is SecurityException) stopSelf()
            }
            .launchIn(scope)
    }

    /**
     * Slide the battery card in the first time this connection yields a real
     * reading.
     *
     * Deliberately keyed on the first *decoded beacon* rather than on the ACL
     * connect edge: at connect time there are no numbers yet, and a card full
     * of dashes that fills in a second later is worse than one that arrives
     * already correct.
     */
    private fun showConnectPopupOnce(state: PodsState, lastCase: LastCaseReading?) {
        if (hasShownConnectPopup) return
        val settings = SettingsStore.settings.value
        if (!settings.showConnectPopup) return
        hasShownConnectPopup = true
        popupHost.show(state, lastCase, settings)
    }

    /**
     * Fire the popup on the shut -> open edge only, not on every packet.
     *
     * "Open" is only meaningful while a pod is in the case: with both pods out,
     * the lid byte reads as open on every frame (captured on hardware: byte 8
     * is 0x01 in that state), which used to fire a "case opened" popup the
     * moment the pods were first seen in your ears. A pod in the case is
     * exactly when the case level is known, so that is the gate.
     */
    private fun notifyLidTransition(state: PodsState, lastCase: LastCaseReading?) {
        val isLidOpen = state.isLidOpen && state.caseBattery != null
        if (isLidOpen && !wasLidOpen && hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
            val settings = SettingsStore.settings.value
            getSystemService(NotificationManager::class.java).notify(
                PodsNotifications.LID_NOTIFICATION_ID,
                PodsNotifications.lidOpened(
                    this,
                    state,
                    lastCase,
                    timeoutMs = settings.popupTimeoutMs.takeIf { settings.autoDismissPopup },
                ),
            )
        }
        wasLidOpen = isLidOpen
    }

    private fun updateOngoing(state: PodsState, lastCase: LastCaseReading?) {
        if (!hasPermission(Manifest.permission.POST_NOTIFICATIONS)) return
        getSystemService(NotificationManager::class.java).notify(
            PodsNotifications.ONGOING_NOTIFICATION_ID,
            PodsNotifications.ongoing(this, state, lastCase),
        )
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        popupHost.destroy()
        scanJob?.cancel()
        scope.cancel()
        tracker.reset()
        PodsRepository.onDisconnected()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "PodsService"

        fun start(context: android.content.Context) {
            ContextCompat.startForegroundService(context, Intent(context, PodsService::class.java))
        }

        fun stop(context: android.content.Context) {
            context.stopService(Intent(context, PodsService::class.java))
        }
    }
}
