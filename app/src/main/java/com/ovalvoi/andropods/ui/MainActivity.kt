package com.ovalvoi.andropods.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.ovalvoi.andropods.data.PodsRepository
import com.ovalvoi.andropods.data.SettingsStore
import com.ovalvoi.andropods.service.BondReceiver
import com.ovalvoi.andropods.ui.theme.AndroPodsTheme
import com.ovalvoi.andropods.ui.theme.resolve

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* State flows from the service; denial degrades to the idle screen. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestMissingPermissions()

        // BondReceiver only sees the connect edge, so pods that were already
        // connected when the app opened would otherwise never start the
        // service. Runs here rather than in Application.onCreate() because a
        // foreground service cannot be started from the background.
        BondReceiver.startIfAlreadyConnected(this)

        setContent {
            val settings by SettingsStore.settings.collectAsState()
            val isDark = settings.darkMode.resolve()

            // enableEdgeToEdge() above read the *system* dark setting for the
            // status-bar icon colour. When the user overrides it in Settings,
            // re-apply so the icons keep contrasting with the app's background.
            LaunchedEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDark },
                    navigationBarStyle = SystemBarStyle.auto(NAV_BAR_LIGHT_SCRIM, NAV_BAR_DARK_SCRIM) { isDark },
                )
            }

            AndroPodsTheme(colorTheme = settings.colorTheme, darkTheme = isDark) {
                // Two screens, one Activity; a navigation library would be
                // ceremony for a readout and its options page.
                var showSettings by rememberSaveable { mutableStateOf(false) }
                if (showSettings) {
                    BackHandler { showSettings = false }
                    SettingsScreen(
                        settings = settings,
                        onChange = SettingsStore::update,
                        onBack = { showSettings = false },
                    )
                } else {
                    val connection by PodsRepository.state.collectAsState()
                    val deviceName by PodsRepository.deviceName.collectAsState()
                    PodsScreen(
                        connection = connection,
                        onOpenSettings = { showSettings = true },
                        deviceName = deviceName,
                    )
                }
            }
        }
    }

    /**
     * Ask only for what is not already granted.
     *
     * Note there is no ACCESS_FINE_LOCATION here: BLUETOOTH_SCAN carries
     * neverForLocation in the manifest, which removes the location requirement
     * and the prompt that goes with it.
     */
    private fun requestMissingPermissions() {
        val required = buildList {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
    }

    private companion object {
        // The defaults enableEdgeToEdge() uses for a three-button navigation bar.
        val NAV_BAR_LIGHT_SCRIM = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        val NAV_BAR_DARK_SCRIM = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
