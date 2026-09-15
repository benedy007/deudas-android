package com.benedy.deudas.ui.clients

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benedy.deudas.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClientScreen(
    viewModel: AddClientViewModel,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    LaunchedEffect(ui.savedClientId) {
        ui.savedClientId?.let { onSaved(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_client_title)) },
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
                        else -> stringResource(R.string.field_required)
                    },
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = viewModel::save,
                enabled = !ui.saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
