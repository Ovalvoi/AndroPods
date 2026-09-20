package com.ovalvoi.andropods.data

import com.ovalvoi.andropods.ble.PodsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for what the UI and the notification both show.
 *
 * A process-wide singleton rather than an injected dependency: the foreground
 * service writes, the Activity reads, and they have separate lifecycles, so the
 * state has to outlive both. Small enough that a DI framework would be
 * ceremony; if this grows, that is the point to revisit.
 */
object PodsRepository {

    private val _state = MutableStateFlow<PodsConnection>(PodsConnection.Disconnected)
    val state: StateFlow<PodsConnection> = _state.asStateFlow()

    fun onDisconnected() {
        _state.value = PodsConnection.Disconnected
    }

    /** Connected over classic Bluetooth, but no beacon decoded yet. */
    fun onSearching() {
        _state.value = PodsConnection.Searching
    }

    fun onState(state: PodsState, lastCase: LastCaseReading?) {
        _state.value = PodsConnection.Live(state, lastCase)
    }
}

/**
 * Connection is a state machine, not a nullable [PodsState]. "Not connected"
 * and "connected but no reading yet" produce very different UI, and collapsing
 * them into null loses that.
 */
sealed interface PodsConnection {
    data object Disconnected : PodsConnection
    data object Searching : PodsConnection
    /**
     * @param lastCase the most recent case reading, live or remembered. Null
     *   only when the case has never been seen. Compare [PodsState.caseBattery]
     *   to tell live from remembered: live is non-null.
     */
    data class Live(val state: PodsState, val lastCase: LastCaseReading? = null) : PodsConnection
}
