package com.example.xposuredetectorsmart.ui.registration

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.xposuredetectorsmart.ui.components.AppHeader
import com.example.xposuredetectorsmart.ui.components.GlassCard
import com.example.xposuredetectorsmart.ui.components.HudBackground
import com.example.xposuredetectorsmart.ui.components.HudButtonLabel

@Composable
fun WorkerRegistrationScreen(
    industryId: String,
    onDone: () -> Unit,
    viewModel: WorkerRegistrationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val exportedFile by viewModel.exportedFile.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(exportedFile) {
        val file = exportedFile ?: return@LaunchedEffect
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share worker QR"))
        viewModel.consumeExportedFile()
    }

    HudBackground {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp)) {
            AppHeader(title = "Register Worker", icon = Icons.Filled.PersonAdd)
            Spacer(Modifier.height(20.dp))

            when (val s = state) {
                is RegistrationState.Success -> RegisteredWorkerCard(
                    workerId = s.workerId,
                    qrBitmap = s.qrBitmap,
                    onShare = { viewModel.shareQr(s.qrBitmap, s.workerId) },
                    onRegisterAnother = { viewModel.reset() },
                    onDone = onDone,
                )
                else -> RegistrationForm(
                    industryId = industryId,
                    state = s,
                    onRegister = { name, employeeCode -> viewModel.register(industryId, name, employeeCode) },
                )
            }
        }
    }
}

@Composable
private fun RegistrationForm(
    industryId: String,
    state: RegistrationState,
    onRegister: (name: String, employeeCode: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var employeeCode by remember { mutableStateOf("") }
    val isSaving = state is RegistrationState.Saving

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("Industry: $industryId", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Worker name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = employeeCode,
                onValueChange = { employeeCode = it },
                label = { Text("Employee code (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state is RegistrationState.Error) {
                Spacer(Modifier.height(8.dp))
                Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onRegister(name, employeeCode.takeIf { it.isNotBlank() }) },
                enabled = !isSaving,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    HudButtonLabel("Register & Generate QR")
                }
            }
        }
    }
}

@Composable
private fun RegisteredWorkerCard(
    workerId: String,
    qrBitmap: Bitmap,
    onShare: () -> Unit,
    onRegisterAnother: () -> Unit,
    onDone: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("WORKER REGISTERED", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("ID: $workerId", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "Worker wristband QR code",
                modifier = Modifier.size(220.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Print or share this QR onto the worker's wristband. It stays valid for their entire tenure at this industry.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onShare, shape = MaterialTheme.shapes.extraLarge) { HudButtonLabel("Share / Print QR") }
            Spacer(Modifier.height(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRegisterAnother, shape = MaterialTheme.shapes.extraLarge) {
                    HudButtonLabel("Register Another Worker")
                }
                OutlinedButton(onClick = onDone, shape = MaterialTheme.shapes.extraLarge) { HudButtonLabel("Done") }
            }
        }
    }
}
