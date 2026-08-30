package com.example.xposuredetectorsmart.ui.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.xposuredetectorsmart.ui.components.GlassCard
import com.example.xposuredetectorsmart.ui.components.HudBackground
import com.example.xposuredetectorsmart.ui.components.HudButtonLabel

@Composable
fun AdminPinGateScreen(
    onGranted: (industryId: String) -> Unit,
    viewModel: AdminPinViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        val granted = state as? PinGateState.Granted ?: return@LaunchedEffect
        onGranted(granted.industryId)
    }

    HudBackground {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    when (val s = state) {
                        is PinGateState.CheckingIndustry -> CircularProgressIndicator()

                        is PinGateState.NeedsIndustrySetup -> {
                            var industryId by remember { mutableStateOf("") }
                            Text("SUPERVISOR SETUP", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Enter this device's industry id to continue",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(
                                value = industryId,
                                onValueChange = { industryId = it },
                                label = { Text("Industry ID") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            s.error?.let {
                                Spacer(Modifier.height(8.dp))
                                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.submitIndustryId(industryId) },
                                enabled = !s.isChecking,
                                shape = MaterialTheme.shapes.extraLarge,
                            ) { HudButtonLabel(if (s.isChecking) "Checking..." else "Continue") }
                        }

                        is PinGateState.EnteringPin -> {
                            var pin by remember { mutableStateOf("") }
                            Text("SUPERVISOR PIN", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Enter the supervisor PIN to register a new worker.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(
                                value = pin,
                                onValueChange = { pin = it },
                                label = { Text("PIN") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            s.error?.let {
                                Spacer(Modifier.height(8.dp))
                                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.submitPin(pin) },
                                shape = MaterialTheme.shapes.extraLarge,
                            ) { HudButtonLabel("Unlock") }
                        }

                        is PinGateState.Granted -> Unit
                    }
                }
            }
        }
    }
}
