package com.benedy.deudas.ui.clients

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.benedy.deudas.R
import com.benedy.deudas.ui.components.ClientAvatar
import com.benedy.deudas.ui.util.ClientPhotoHelper
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClientScreen(
    viewModel: AddClientViewModel,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var cameraFile by remember { mutableStateOf<File?>(null) }
    var pendingCamera by remember { mutableStateOf(false) }

    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val file = cameraFile
        if (success && file != null && file.exists()) {
            try {
                val relative = ClientPhotoHelper.persistFromFile(
                    context,
                    file,
                    ui.photoPath
                )
                val preview = ClientPhotoHelper.resolveFile(context, relative)?.let { Uri.fromFile(it).toString() }
                viewModel.onPhotoChosen(preview, relative)
            } catch (_: Exception) {
                // keep previous photo
            }
        }
        cameraFile = null
    }

    val requestCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingCamera) {
            pendingCamera = false
            val file = ClientPhotoHelper.createCameraTempFile(context)
            cameraFile = file
            takePicture.launch(ClientPhotoHelper.cameraTempUri(context, file))
        } else {
            pendingCamera = false
        }
    }

    val pickGallery = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val relative = ClientPhotoHelper.persistPhoto(context, uri, ui.photoPath)
                val preview = ClientPhotoHelper.resolveFile(context, relative)?.let { Uri.fromFile(it).toString() }
                viewModel.onPhotoChosen(preview, relative)
            } catch (_: Exception) {
            }
        }
    }

    fun launchCamera() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val file = ClientPhotoHelper.createCameraTempFile(context)
            cameraFile = file
            takePicture.launch(ClientPhotoHelper.cameraTempUri(context, file))
        } else {
            pendingCamera = true
            requestCamera.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(ui.savedClientId) {
        ui.savedClientId?.let { onSaved(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (ui.isEdit) stringResource(R.string.edit_client_title)
                        else stringResource(R.string.add_client_title)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Photo section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val preview = ui.photoPreviewUri
                if (preview != null) {
                    AsyncImage(
                        model = preview,
                        contentDescription = stringResource(R.string.client_photo),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clickable { launchCamera() }
                    )
                } else {
                    ClientAvatar(
                        photoPath = ui.photoPath,
                        size = 72.dp,
                        modifier = Modifier.clickable { launchCamera() }
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { launchCamera() }) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.client_photo_camera))
                    }
                    OutlinedButton(onClick = { pickGallery.launch("image/*") }) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.client_photo_gallery))
                    }
                    if (ui.photoPath != null || ui.photoPreviewUri != null) {
                        OutlinedButton(onClick = {
                            ClientPhotoHelper.deletePhoto(context, ui.photoPath)
                            viewModel.onPhotoCleared()
                        }) {
                            Icon(Icons.Outlined.Delete, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.client_photo_remove))
                        }
                    }
                }
            }
            Text(
                text = stringResource(R.string.client_photo_optional),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            OutlinedTextField(
                value = ui.name,
                onValueChange = viewModel::onName,
                label = { Text(stringResource(R.string.client_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = ui.phone,
                onValueChange = viewModel::onPhone,
                label = { Text(stringResource(R.string.client_phone)) },
                placeholder = { Text(stringResource(R.string.client_phone_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = ui.direccionCasa,
                onValueChange = viewModel::onDireccionCasa,
                label = { Text(stringResource(R.string.client_home_address)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = ui.lugarTrabajo,
                onValueChange = viewModel::onLugarTrabajo,
                label = { Text(stringResource(R.string.client_workplace)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = ui.direccionTrabajo,
                onValueChange = viewModel::onDireccionTrabajo,
                label = { Text(stringResource(R.string.client_work_address)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = ui.creditLimitText,
                onValueChange = viewModel::onCreditLimit,
                label = { Text(stringResource(R.string.client_credit_limit)) },
                placeholder = { Text(stringResource(R.string.client_credit_limit_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = { Text(stringResource(R.string.client_credit_limit_support)) },
                modifier = Modifier.fillMaxWidth()
            )
            if (!ui.isEdit) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = ui.saldoInicialText,
                    onValueChange = viewModel::onSaldoInicial,
                    label = { Text(stringResource(R.string.client_saldo_inicial)) },
                    placeholder = { Text(stringResource(R.string.client_saldo_inicial_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text(stringResource(R.string.client_saldo_inicial_support)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = ui.notes,
                onValueChange = viewModel::onNotes,
                label = { Text(stringResource(R.string.client_notes)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            if (ui.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = when (ui.error) {
                        "db" -> "No se pudo guardar. Revisa el almacenamiento e intenta de nuevo."
                        "credit" -> stringResource(R.string.invalid_amount)
                        "saldo" -> stringResource(R.string.invalid_amount)
                        else -> stringResource(R.string.field_required)
                    },
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = viewModel::save,
                enabled = !ui.saving && ui.loaded,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
