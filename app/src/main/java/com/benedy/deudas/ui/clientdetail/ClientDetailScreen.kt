package com.benedy.deudas.ui.clientdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benedy.deudas.DeudasApp
import com.benedy.deudas.R
import com.benedy.deudas.data.local.entity.ClientEntity
import com.benedy.deudas.data.local.entity.CobranzaNoteEntity
import com.benedy.deudas.data.local.entity.DebtEntity
import com.benedy.deudas.ui.components.ClientAvatar
import com.benedy.deudas.ui.util.StatementImageRenderer
import com.benedy.deudas.ui.util.WhatsAppHelper
import com.benedy.deudas.ui.util.debtPlanLabel
import com.benedy.deudas.ui.util.formatDate
import com.benedy.deudas.ui.util.formatMoney
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    viewModel: ClientDetailViewModel,
    chargeMode: Boolean,
    onBack: () -> Unit,
    onAddDebt: () -> Unit,
    onRegisterPayment: (debtId: Long?) -> Unit,
    onPaymentHistory: () -> Unit,
    onEditClient: () -> Unit
) {
    val client by viewModel.client.collectAsStateWithLifecycle()
    val debts by viewModel.debts.collectAsStateWithLifecycle()
    val total by viewModel.totalRemaining.collectAsStateWithLifecycle()
    val notes by viewModel.cobranzaNotes.collectAsStateWithLifecycle()
    val noteText by viewModel.noteText.collectAsStateWithLifecycle()
    val promisedDate by viewModel.promisedDateMs.collectAsStateWithLifecycle()
    val noteError by viewModel.noteError.collectAsStateWithLifecycle()
    val statementBusy by viewModel.statementBusy.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as DeudasApp
    var showPromisePicker by remember { mutableStateOf(false) }
    var statementError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (chargeMode) {
                            stringResource(R.string.charge_title)
                        } else {
                            client?.name ?: stringResource(R.string.clients_title)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEditClient) {
                        Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.edit_client_title))
                    }
                }
            )
        }
    ) { padding ->
        val current = client
        if (current == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("…")
            }
        } else {
            ClientDetailBody(
                client = current,
                debts = debts,
                total = total,
                notes = notes,
                noteText = noteText,
                promisedDate = promisedDate,
                noteError = noteError,
                statementBusy = statementBusy,
                contentPadding = padding,
                onOpenWhatsApp = { WhatsAppHelper.openChat(context, current.phone) },
                onReminder = {
                    scope.launch {
                        val settings = withContext(Dispatchers.IO) {
                            app.settingsRepository.settings.first()
                        }
                        val msg = viewModel.buildReminderMessage(
                            clientName = current.name,
                            remaining = total,
                            debts = debts,
                            companyName = settings.companyName.ifBlank { "Deudas" }
                        )
                        WhatsAppHelper.openChat(context, current.phone, msg)
                    }
                },
                onShareStatement = {
                    scope.launch {
                        viewModel.setStatementBusy(true)
                        statementError = null
                        try {
                            val statement = withContext(Dispatchers.IO) {
                                viewModel.loadStatement()
                            }
                            if (statement == null) {
                                statementError = "empty"
                            } else {
                                val settings = withContext(Dispatchers.IO) {
                                    app.settingsRepository.settings.first()
                                }
                                val uri = withContext(Dispatchers.Default) {
                                    StatementImageRenderer.renderForShare(
                                        context = context,
                                        statement = statement,
                                        companyName = settings.companyName,
                                        companyPhone = settings.companyPhone.takeIf { it.isNotBlank() }
                                    )
                                }
                                WhatsAppHelper.shareImageSystem(context, uri)
                            }
                        } catch (e: Exception) {
                            statementError = e.message ?: "error"
                        } finally {
                            viewModel.setStatementBusy(false)
                        }
                    }
                },
                onAddDebt = onAddDebt,
                onRegisterPayment = onRegisterPayment,
                onPaymentHistory = onPaymentHistory,
                onNoteText = viewModel::onNoteText,
                onPickPromiseDate = { showPromisePicker = true },
                onClearPromiseDate = { viewModel.onPromisedDate(null) },
                onSaveNote = viewModel::addCobranzaNote,
                onDeleteNote = viewModel::deleteCobranzaNote
            )
        }
    }

    if (showPromisePicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showPromisePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { viewModel.onPromisedDate(it) }
                    showPromisePicker = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showPromisePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (statementError != null) {
        AlertDialog(
            onDismissRequest = { statementError = null },
            confirmButton = {
                TextButton(onClick = { statementError = null }) { Text(stringResource(R.string.done)) }
            },
            title = { Text(stringResource(R.string.statement_error_title)) },
            text = { Text(stringResource(R.string.share_receipt_failed)) }
        )
    }
}

@Composable
private fun ClientDetailBody(
    client: ClientEntity,
    debts: List<DebtEntity>,
    total: Double,
    notes: List<CobranzaNoteEntity>,
    noteText: String,
    promisedDate: Long?,
    noteError: String?,
    statementBusy: Boolean,
    contentPadding: PaddingValues,
    onOpenWhatsApp: () -> Unit,
    onReminder: () -> Unit,
    onShareStatement: () -> Unit,
    onAddDebt: () -> Unit,
    onRegisterPayment: (Long?) -> Unit,
    onPaymentHistory: () -> Unit,
    onNoteText: (String) -> Unit,
    onPickPromiseDate: () -> Unit,
    onClearPromiseDate: () -> Unit,
    onSaveNote: () -> Unit,
    onDeleteNote: (Long) -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es", "DO")) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ClientAvatar(photoPath = client.photoPath, size = 64.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = client.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = client.phone, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    ClientAddressLines(client)
                    client.creditLimit?.takeIf { it > 0 }?.let { lim ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.credit_limit_label, formatMoney(lim)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    val notesProfile = client.notes
                    if (!notesProfile.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = notesProfile,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = onOpenWhatsApp, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.open_whatsapp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onReminder,
                        enabled = total > 0,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.NotificationsActive, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.reminder_whatsapp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onShareStatement,
                        enabled = !statementBusy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Description, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.share_statement))
                    }
                }
            }
        }
        item {
            Text(
                text = stringResource(R.string.total_owing, formatMoney(total)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(onClick = onAddDebt, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.add_debt))
                }
                Button(
                    onClick = { onRegisterPayment(null) },
                    modifier = Modifier.weight(1f),
                    enabled = debts.any { it.remainingBalance > 0 }
                ) {
                    Icon(Icons.Outlined.Payments, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.register_payment))
                }
            }
        }
        item {
            OutlinedButton(onClick = onPaymentHistory, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.History, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.client_payment_history_title))
            }
        }

        // Cobranza notes
        item {
            Text(
                text = stringResource(R.string.cobranza_notes_title),
                style = MaterialTheme.typography.titleMedium
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = onNoteText,
                        label = { Text(stringResource(R.string.cobranza_note_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onPickPromiseDate) {
                            Text(
                                if (promisedDate != null) {
                                    stringResource(
                                        R.string.promised_pay_date,
                                        dateFmt.format(Date(promisedDate))
                                    )
                                } else {
                                    stringResource(R.string.promised_pay_pick)
                                }
                            )
                        }
                        if (promisedDate != null) {
                            TextButton(onClick = onClearPromiseDate) {
                                Text(stringResource(R.string.clear_date))
                            }
                        }
                    }
                    if (noteError != null) {
                        Text(
                            text = stringResource(R.string.field_required),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Button(onClick = onSaveNote, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.cobranza_note_save))
                    }
                }
            }
        }
        items(notes, key = { "note-${it.id}" }) { note ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(note.text, style = MaterialTheme.typography.bodyLarge)
                        note.promisedDate?.let { pd ->
                            Text(
                                stringResource(
                                    R.string.promised_pay_date,
                                    dateFmt.format(Date(pd))
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            formatDate(note.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDeleteNote(note.id) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete_payment_confirm))
                    }
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.debts_section),
                style = MaterialTheme.typography.titleMedium
            )
        }
        if (debts.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.no_debts),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(debts, key = { "debt-${it.id}" }) { debt ->
                DebtCard(debt = debt, onPay = { onRegisterPayment(debt.id) })
            }
        }
    }
}

@Composable
private fun ClientAddressLines(client: ClientEntity) {
    val homeLabel = stringResource(R.string.client_home_address)
    val workPlaceLabel = stringResource(R.string.client_workplace)
    val workAddrLabel = stringResource(R.string.client_work_address)
    val lines = buildList {
        client.direccionCasa?.takeIf { it.isNotBlank() }?.let { add(homeLabel to it) }
        client.lugarTrabajo?.takeIf { it.isNotBlank() }?.let { add(workPlaceLabel to it) }
        client.direccionTrabajo?.takeIf { it.isNotBlank() }?.let { add(workAddrLabel to it) }
    }
    if (lines.isEmpty()) return
    Spacer(modifier = Modifier.height(8.dp))
    lines.forEach { (label, value) ->
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun DebtCard(debt: DebtEntity, onPay: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = debt.description,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stringResource(R.string.original_label, formatMoney(debt.originalAmount)))
            Text(
                text = stringResource(R.string.remaining_label, formatMoney(debt.remainingBalance)),
                color = if (debt.remainingBalance <= 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
            debtPlanLabel(debt)?.let { planText ->
                Text(
                    text = planText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = formatDate(debt.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (debt.remainingBalance > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onPay, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.register_payment))
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.paid_full_hint),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
