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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benedy.deudas.R
import com.benedy.deudas.data.local.entity.ClientEntity
import com.benedy.deudas.data.local.entity.DebtEntity
import com.benedy.deudas.ui.util.WhatsAppHelper
import com.benedy.deudas.ui.util.formatDate
import com.benedy.deudas.ui.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    viewModel: ClientDetailViewModel,
    chargeMode: Boolean,
    onBack: () -> Unit,
    onAddDebt: () -> Unit,
    onRegisterPayment: (debtId: Long?) -> Unit,
    onPaymentHistory: () -> Unit
) {
    val client by viewModel.client.collectAsStateWithLifecycle()
    val debts by viewModel.debts.collectAsStateWithLifecycle()
    val total by viewModel.totalRemaining.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                contentPadding = padding,
                onOpenWhatsApp = { WhatsAppHelper.openChat(context, current.phone) },
                onAddDebt = onAddDebt,
                onRegisterPayment = onRegisterPayment,
                onPaymentHistory = onPaymentHistory
            )
        }
    }
}

@Composable
private fun ClientDetailBody(
    client: ClientEntity,
    debts: List<DebtEntity>,
    total: Double,
    contentPadding: PaddingValues,
    onOpenWhatsApp: () -> Unit,
    onAddDebt: () -> Unit,
    onRegisterPayment: (Long?) -> Unit,
    onPaymentHistory: () -> Unit
) {
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
                    Text(
                        text = client.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = client.phone, style = MaterialTheme.typography.bodyLarge)
                    ClientAddressLines(client)
                    val notes = client.notes
                    if (!notes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onOpenWhatsApp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.open_whatsapp))
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
                FilledTonalButton(
                    onClick = onAddDebt,
                    modifier = Modifier.weight(1f)
                ) {
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
            OutlinedButton(
                onClick = onPaymentHistory,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.History, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.client_payment_history_title))
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
            items(debts, key = { it.id }) { debt ->
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
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
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
