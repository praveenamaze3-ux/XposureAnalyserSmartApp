package com.example.xposuredetectorsmart.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.fragment.app.FragmentActivity

/** Gates [content] behind a biometric prompt when the user has enabled biometric lock. */
@Composable
fun BiometricGate(
    workerId: String,
    viewModel: BiometricGateViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val biometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val lockState by viewModel.lockState.collectAsState()
    val activity = LocalContext.current as? FragmentActivity

    LaunchedEffect(biometricEnabled) {
        viewModel.evaluate(biometricEnabled)
    }

    when (lockState) {
        is LockState.Unlocked -> content()
        is LockState.Checking -> Unit
        is LockState.Locked -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Dose data locked", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Fingerprint required to view sensitive exposure data.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { activity?.let { viewModel.requestUnlock(it, workerId) } }) {
                    Text("Unlock")
                }
            }
        }
    }
}
