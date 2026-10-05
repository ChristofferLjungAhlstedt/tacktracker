package com.blindsail.tacktracker.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat

/** Milestone 2: debug screen with raw and fused heading. Sensors run only while visible. */
class MainActivity : ComponentActivity() {
    private val viewModel: DebugViewModel by viewModels()
    private var hasLocationPermission by mutableStateOf(false)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasLocationPermission = granted
            if (granted) viewModel.restart()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasLocationPermission = checkLocationPermission()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DebugScreen(
                        viewModel = viewModel,
                        hasLocationPermission = hasLocationPermission,
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        hasLocationPermission = checkLocationPermission()
        viewModel.start()
    }

    override fun onStop() {
        viewModel.stop()
        super.onStop()
    }

    private fun checkLocationPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}
