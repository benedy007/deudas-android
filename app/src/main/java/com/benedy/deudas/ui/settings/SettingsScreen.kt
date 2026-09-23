package com.benedy.deudas.ui.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benedy.deudas.BuildConfig
import com.benedy.deudas.R
import com.benedy.deudas.data.settings.CompanySettings
import com.benedy.deudas.ui.auth.AuthUiState
import com.benedy.deudas.ui.backup.BackupViewModel
import com.benedy.deudas.ui.util.CrmTextExport
import com.benedy.deudas.ui.util.formatDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    uiState: AuthUiState,
    backupViewModel: BackupViewModel,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val backupState by backupViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as Activity
    val session = uiState.session
    val isGuest = session?.isGuest == true

    val authLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        backupViewModel.clearPendingConsent()
        if (result.resultCode == Activity.RESULT_OK) {
            backupViewModel.onAuthorizationResult(activity, result.data)
        } else {
            backupViewModel.onAuthorizationCancelled()
        }
    }

    LaunchedEffect(backupState.pendingConsent) {
        val sender = backupState.pendingConsent ?: return@LaunchedEffect
        authLauncher.launch(IntentSenderRequest.Builder(sender).build())
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(ui.saved) {
        if (ui.saved) {
            kotlinx.coroutines.delay(1500)
            viewModel.clearSavedFlag()
        }
    }

    LaunchedEffect(ui.pendingShare) {
        val pending = ui.pendingShare ?: return@LaunchedEffect
        try {
            CrmTextExport.shareTextFile(context, pending.shareUri, pending.fileName)
        } catch (_: Exception) {
            // Share sheet failure still reported via exportStatus if set
        } finally {
            viewModel.clearPendingShare()
        }
    }

    LaunchedEffect(ui.exportStatus) {
        val msg = ui.exportStatus ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearExportStatus()
    }

    if (backupState.showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { backupViewModel.dismissRestoreConfirm() },
            title = { Text(stringResource(R.string.backup_restore_confirm_title)) },
            text = { Text(stringResource(R.string.backup_restore_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { backupViewModel.confirmRestore(activity) }) {
                    Text(stringResource(R.string.backup_restore_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { backupViewModel.dismissRestoreConfirm() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = backupState.isRefreshingStatus,
            onRefresh = { backupViewModel.refreshBackupStatus(activity) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = stringResource(R.string.settings_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))

                // —— Cuenta ——
                SettingsSectionHeader(
                    icon = Icons.Outlined.Person,
                    title = stringResource(R.string.settings_section_account)
                )
                SettingsSectionCard {
                    ListItem(
                        headlineContent = {
                            Text(
                                if (isGuest) {
                                    stringResource(R.string.settings_session_guest)
                                } else {
                                    session?.displayName?.takeIf { it.isNotBlank() }
                                        ?: stringResource(R.string.user_fallback)
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        supportingContent = {
                            if (!isGuest) {
                                Column {
                                    session?.email?.takeIf { it.isNotBlank() }?.let { email ->
                                        Text(email)
                                    }
                                    Text(stringResource(R.string.settings_session_google))
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                    HorizontalDivider()
                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .height(48.dp),
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.exit_guest))
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // —— Respaldo ——
                SettingsSectionHeader(
                    icon = Icons.Outlined.CloudUpload,
                    title = stringResource(R.string.settings_section_backup)
                )
                SettingsSectionCard {
                    if (isGuest) {
                        Text(
                            text = stringResource(R.string.backup_guest_message),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val last = backupState.lastBackupAtMs
                        Text(
                            text = if (last != null) {
                                stringResource(R.string.backup_last_at, formatDateTime(last))
                            } else {
                                stringResource(R.string.backup_last_none)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.backup_refresh_hint),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { backupViewModel.requestBackup(activity) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(48.dp),
                            enabled = !backupState.isBusy,
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 2.dp
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.backup_to_drive))
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { backupViewModel.requestRestoreConfirm() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(48.dp),
                            enabled = !backupState.isBusy,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.restore_from_drive))
                        }
                        if (backupState.isBusy) {
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.backup_in_progress),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        backupState.statusMessage?.let { msg ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = msg,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (backupState.isError) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }


                Spacer(Modifier.height(20.dp))

                // —— Exportar ——
                SettingsSectionHeader(
                    icon = Icons.Outlined.Description,
                    title = stringResource(R.string.settings_section_export)
                )
                SettingsSectionCard {
                    Text(
                        text = stringResource(R.string.settings_export_hint),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { viewModel.exportToText(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(48.dp),
                        enabled = !ui.exporting,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (ui.exporting) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_exporting))
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_export_text))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Spacer(Modifier.height(20.dp))

                // —— Compañía ——
                SettingsSectionHeader(
                    icon = Icons.Outlined.Business,
                    title = stringResource(R.string.settings_section_company)
                )
                SettingsSectionCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            Text(
                                text = stringResource(R.string.settings_saved),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        ui.error?.let { err ->
                            Text(
                                text = stringResource(R.string.settings_save_failed, err),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // —— Acerca de ——
                SettingsSectionHeader(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.settings_section_about)
                )
                SettingsSectionCard {
                    ListItem(
                        headlineContent = {
                            Text(stringResource(R.string.settings_app_version, BuildConfig.VERSION_NAME))
                        },
                        supportingContent = {
                            Text(stringResource(R.string.app_name))
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SettingsSectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        content()
    }
}
