package com.benedy.deudas.ui.receipt

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benedy.deudas.DeudasApp
import com.benedy.deudas.R
import com.benedy.deudas.data.settings.CompanySettings
import com.benedy.deudas.ui.util.ReceiptImageRenderer
import com.benedy.deudas.ui.util.WhatsAppHelper
import com.benedy.deudas.ui.util.formatDate
import com.benedy.deudas.ui.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(
    viewModel: ReceiptViewModel,
    onDone: () -> Unit,
    onDeleted: () -> Unit = onDone
) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val deleting by viewModel.deleting.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val settingsRepo = remember {
        (context.applicationContext as DeudasApp).settingsRepository
    }
    var companySettings by remember { mutableStateOf(CompanySettings()) }
    var showShareChooser by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    androidx.compose.runtime.LaunchedEffect(Unit) {
        companySettings = settingsRepo.settings.first()
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { if (!deleting) showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_payment_title)) },
            text = { Text(stringResource(R.string.delete_payment_message)) },
            confirmButton = {
                TextButton(
                    enabled = !deleting,
                    onClick = {
                        viewModel.deletePayment(
                            onDone = {
                                showDeleteConfirm = false
                                onDeleted()
                            },
                            onError = { msg ->
                                showDeleteConfirm = false
                                Toast.makeText(
                                    context,
                                    msg.ifBlank { context.getString(R.string.delete_payment_not_latest) },
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                ) {
                    Text(stringResource(R.string.delete_payment_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !deleting,
                    onClick = { showDeleteConfirm = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.receipt_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    val r = data
                    if (r != null && r.canDelete) {
                        IconButton(
                            enabled = !deleting,
                            onClick = { showDeleteConfirm = true }
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.delete_payment_confirm),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        val r = data
        if (r == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text("…")
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(r.clientName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.receipt_paid) + ": " + formatMoney(r.amount),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (r.allocations.size > 1) {
                        Text(
                            stringResource(R.string.receipt_applied_to),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        r.allocations.forEach { line ->
                            Text(
                                "• ${line.debtDescription}: ${formatMoney(line.amount)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        Text(
                            stringResource(R.string.receipt_concept) + ": " + r.debtDescription,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Text(
                        stringResource(R.string.receipt_remaining) + ": " + formatMoney(r.remaining),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        stringResource(R.string.receipt_date) + ": " + formatDate(r.dateMs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(18.dp),
                onClick = { showShareChooser = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.send_whatsapp))
            }
            if (r.canDelete) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    enabled = !deleting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.delete_payment_confirm))
                }
            }
            OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.done))
            }
        }

        if (showShareChooser) {
            ModalBottomSheet(
                onDismissRequest = { showShareChooser = false },
                sheetState = sheetState
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.share_receipt_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.share_receipt_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Button(
                        onClick = {
                            showShareChooser = false
                            shareReceiptImage(
                                context = context,
                                data = r,
                                settings = companySettings
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Image, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(stringResource(R.string.share_as_image))
                            Text(
                                stringResource(R.string.share_as_image_hint),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            showShareChooser = false
                            val text = WhatsAppHelper.buildReceiptText(
                                clientName = r.clientName,
                                amount = r.amount,
                                debtDescription = r.debtDescription,
                                dateMs = r.dateMs,
                                remaining = r.remaining,
                                companyName = companySettings.displayName(),
                                companyPhone = companySettings.displayPhone(),
                                footerNote = companySettings.displayFooter()
                            )
                            WhatsAppHelper.shareTextToWhatsApp(context, r.clientPhone, text)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.TextSnippet, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.share_as_text))
                    }

                    TextButton(
                        onClick = { showShareChooser = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}

private fun shareReceiptImage(
    context: android.content.Context,
    data: ReceiptData,
    settings: CompanySettings
) {
    try {
        val uri = ReceiptImageRenderer.renderForShare(
            context = context,
            clientName = data.clientName,
            amount = data.amount,
            debtDescription = data.debtDescription,
            dateMs = data.dateMs,
            remaining = data.remaining,
            companyName = settings.displayName(),
            companyPhone = settings.displayPhone(),
            footerNote = settings.displayFooter()
        )
        WhatsAppHelper.shareImageToWhatsApp(
            context = context,
            phone = data.clientPhone,
            imageUri = uri,
            caption = null
        )
    } catch (e: Exception) {
        val detail = e.message?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName
        Toast.makeText(
            context,
            context.getString(R.string.share_receipt_failed_detail, detail),
            Toast.LENGTH_LONG
        ).show()
    }
}
