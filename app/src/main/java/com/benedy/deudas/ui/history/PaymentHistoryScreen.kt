package com.benedy.deudas.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benedy.deudas.R
import com.benedy.deudas.ui.util.formatDate
import com.benedy.deudas.ui.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistoryScreen(
    viewModel: PaymentHistoryViewModel,
    title: String = stringResource(R.string.payment_history_title),
    onBack: () -> Unit,
    onOpenReceipt: (paymentId: Long) -> Unit
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val deleting by viewModel.deleting.collectAsStateWithLifecycle()
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }

    if (pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = { if (!deleting) pendingDeleteId = null },
            title = { Text(stringResource(R.string.delete_payment_title)) },
            text = { Text(stringResource(R.string.delete_payment_message)) },
            confirmButton = {
                TextButton(
                    enabled = !deleting,
                    onClick = {
                        val id = pendingDeleteId ?: return@TextButton
                        viewModel.deletePayment(id) {
                            pendingDeleteId = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.delete_payment_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !deleting,
                    onClick = { pendingDeleteId = null }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
        if (items.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.payment_history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.receiptPaymentId }) { item ->
                    PaymentHistoryCard(
                        item = item,
                        onClick = { onOpenReceipt(item.receiptPaymentId) },
                        onDelete = { pendingDeleteId = item.receiptPaymentId }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentHistoryCard(
    item: PaymentHistoryItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .padding(top = 8.dp)
                    .clickable(onClick = onClick)
            ) {
                Text(
                    text = item.clientName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatMoney(item.totalAmount),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatDate(item.dateMs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.allocationCount > 1) {
                    Text(
                        text = stringResource(R.string.payment_history_allocations, item.allocationCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.payment_history_resend_hint),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.delete_payment_confirm),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
