package com.benedy.deudas.ui.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AddBusiness
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benedy.deudas.BuildConfig
import com.benedy.deudas.R
import com.benedy.deudas.ui.auth.AuthUiState
import com.benedy.deudas.ui.backup.BackupViewModel
import com.benedy.deudas.ui.components.ElevatedActionCard
import com.benedy.deudas.ui.components.ScreenGradient
import com.benedy.deudas.ui.theme.HubAddClientContainer
import com.benedy.deudas.ui.theme.HubAddClientContent
import com.benedy.deudas.ui.theme.HubAddClientIconEnd
import com.benedy.deudas.ui.theme.HubAddClientIconStart
import com.benedy.deudas.ui.theme.HubAddProductContainer
import com.benedy.deudas.ui.theme.HubAddProductContent
import com.benedy.deudas.ui.theme.HubAddProductIconEnd
import com.benedy.deudas.ui.theme.HubAddProductIconStart
import com.benedy.deudas.ui.theme.HubChargeContainer
import com.benedy.deudas.ui.theme.HubChargeContent
import com.benedy.deudas.ui.theme.HubChargeIconEnd
import com.benedy.deudas.ui.theme.HubChargeIconStart
import com.benedy.deudas.ui.theme.HubClientsContainer
import com.benedy.deudas.ui.theme.HubClientsContent
import com.benedy.deudas.ui.theme.HubClientsIconEnd
import com.benedy.deudas.ui.theme.HubClientsIconStart
import com.benedy.deudas.ui.theme.HubProductsContainer
import com.benedy.deudas.ui.theme.HubProductsContent
import com.benedy.deudas.ui.theme.HubProductsIconEnd
import com.benedy.deudas.ui.theme.HubProductsIconStart
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: AuthUiState,
    backupViewModel: BackupViewModel,
    onLogout: () -> Unit,
    onAddClient: () -> Unit,
    onCharge: () -> Unit,
    onAddProduct: () -> Unit,
    onViewClients: () -> Unit,
    onViewProducts: () -> Unit
) {
    val session = uiState.session
    val isGuest = session?.isGuest == true
    val backupState by backupViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as Activity

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
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        OutlinedButton(
                            onClick = onLogout,
                            modifier = Modifier.padding(end = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.exit_guest))
                        }
                    }
                }
            )
        }
    ) { padding ->
        ScreenGradient(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (isGuest) {
                    Text(
                        text = stringResource(R.string.guest_display_name) + " · " +
                            stringResource(R.string.test_mode_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = session?.displayName
                            ?: stringResource(R.string.user_fallback),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    session?.email?.takeIf { it.isNotBlank() }?.let { email ->
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.hub_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isGuest) {
                        stringResource(R.string.hub_subtitle_guest)
                    } else {
                        stringResource(R.string.hub_subtitle_google)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))

                ElevatedActionCard(
                    icon = Icons.Outlined.People,
                    title = stringResource(R.string.action_view_clients),
                    description = stringResource(R.string.action_view_clients_desc),
                    onClick = onViewClients,
                    elevation = 12.dp,
                    containerColor = HubClientsContainer,
                    contentColor = HubClientsContent,
                    iconGradient = listOf(HubClientsIconStart, HubClientsIconEnd),
                    iconTint = Color.White
                )
                ElevatedActionCard(
                    icon = Icons.Outlined.Inventory2,
                    title = stringResource(R.string.action_view_products),
                    description = stringResource(R.string.action_view_products_desc),
                    onClick = onViewProducts,
                    elevation = 12.dp,
                    containerColor = HubProductsContainer,
                    contentColor = HubProductsContent,
                    iconGradient = listOf(HubProductsIconStart, HubProductsIconEnd),
                    iconTint = Color.White
                )
                ElevatedActionCard(
                    icon = Icons.Outlined.PersonAdd,
                    title = stringResource(R.string.action_add_client),
                    description = stringResource(R.string.action_add_client_desc),
                    onClick = onAddClient,
                    elevation = 10.dp,
                    containerColor = HubAddClientContainer,
                    contentColor = HubAddClientContent,
                    iconGradient = listOf(HubAddClientIconStart, HubAddClientIconEnd),
                    iconTint = Color.White
                )
                ElevatedActionCard(
                    icon = Icons.Outlined.Payments,
                    title = stringResource(R.string.action_charge),
                    description = stringResource(R.string.action_charge_desc),
                    onClick = onCharge,
                    elevation = 10.dp,
                    containerColor = HubChargeContainer,
                    contentColor = HubChargeContent,
                    iconGradient = listOf(HubChargeIconStart, HubChargeIconEnd),
                    iconTint = Color.White
                )
                ElevatedActionCard(
                    icon = Icons.Outlined.AddBusiness,
                    title = stringResource(R.string.action_add_product),
                    description = stringResource(R.string.action_add_product_desc),
                    onClick = onAddProduct,
                    elevation = 10.dp,
                    containerColor = HubAddProductContainer,
                    contentColor = HubAddProductContent,
                    iconGradient = listOf(HubAddProductIconStart, HubAddProductIconEnd),
                    iconTint = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.backup_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (isGuest) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.backup_guest_message),
                            modifier = Modifier.padding(18.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Button(
                        onClick = { backupViewModel.requestBackup(activity) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !backupState.isBusy,
                        shape = RoundedCornerShape(18.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 6.dp,
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
                    OutlinedButton(
                        onClick = { backupViewModel.requestRestoreConfirm() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !backupState.isBusy,
                        shape = RoundedCornerShape(18.dp)
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (backupState.isError) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
