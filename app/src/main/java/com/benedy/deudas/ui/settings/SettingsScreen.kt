package com.benedy.deudas.ui.settings

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
import com.benedy.deudas.data.settings.CompanySettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    LaunchedEffect(ui.saved) {
        if (ui.saved) {
            // Brief confirmation then clear flag (stay on screen)
            kotlinx.coroutines.delay(1500)
            viewModel.clearSavedFlag()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
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
            Text(
                text = stringResource(R.string.settings_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = ui.companyName,
                onValueChange = viewModel::onCompanyName,
                label = { Text(stringResource(R.string.settings_company_name)) },
                placeholder = { Text(CompanySettings.DEFAULT_COMPANY_NAME) },
                supportingText = {
                    Text(stringResource(R.string.settings_company_name_hint))
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = ui.companyPhone,
                onValueChange = viewModel::onCompanyPhone,
                label = { Text(stringResource(R.string.settings_company_phone)) },
                placeholder = { Text(stringResource(R.string.settings_company_phone_hint)) },
                supportingText = {
                    Text(stringResource(R.string.settings_company_phone_support))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = ui.receiptFooter,
                onValueChange = viewModel::onReceiptFooter,
                label = { Text(stringResource(R.string.settings_receipt_footer)) },
                placeholder = { Text(CompanySettings.DEFAULT_RECEIPT_FOOTER) },
                supportingText = {
                    Text(stringResource(R.string.settings_receipt_footer_hint))
                },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = viewModel::save,
                enabled = !ui.saving && ui.loaded,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (ui.saving) stringResource(R.string.settings_saving)
                    else stringResource(R.string.save)
                )
            }

            if (ui.saved) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_saved),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            ui.error?.let { err ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_save_failed, err),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
