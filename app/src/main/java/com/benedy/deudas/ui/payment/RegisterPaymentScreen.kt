package com.benedy.deudas.ui.payment

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benedy.deudas.R
import com.benedy.deudas.ui.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterPaymentScreen(
    viewModel: RegisterPaymentViewModel,
    onBack: () -> Unit,
    onPaid: (paymentId: Long) -> Unit
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val debts by viewModel.openDebts.collectAsStateWithLifecycle()
    val totalRemaining by viewModel.totalRemaining.collectAsStateWithLifecycle()

    LaunchedEffect(ui.paymentId) {
        ui.paymentId?.let { onPaid(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.register_payment)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.payment_waterfall_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.total_owing, formatMoney(totalRemaining)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.payment_applies_to_total),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }

            Text(
                stringResource(R.string.open_debts_oldest_first),
                style = MaterialTheme.typography.titleMedium
            )
            if (debts.isEmpty()) {
                Text(stringResource(R.string.no_debts), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                debts.forEachIndexed { index, debt ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                text = "${index + 1}. ${debt.description}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(stringResource(R.string.remaining_label, formatMoney(debt.remainingBalance)))
                            if (index == 0) {
                                Text(
                                    text = stringResource(R.string.payment_oldest_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = ui.amount,
                onValueChange = viewModel::onAmount,
                label = { Text(stringResource(R.string.payment_amount)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            if (totalRemaining > 0) {
                OutlinedButton(
                    onClick = viewModel::fillTotal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.pay_full_total))
                }
            }
            OutlinedTextField(
                value = ui.note,
                onValueChange = viewModel::onNote,
                label = { Text(stringResource(R.string.payment_note)) },
                modifier = Modifier.fillMaxWidth()
            )
            when (ui.error) {
                "amount" -> Text(stringResource(R.string.invalid_amount), color = MaterialTheme.colorScheme.error)
                "over_total" -> Text(stringResource(R.string.payment_over_total), color = MaterialTheme.colorScheme.error)
                "no_debts" -> Text(stringResource(R.string.no_debts), color = MaterialTheme.colorScheme.error)
                "failed" -> Text(stringResource(R.string.payment_failed), color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            val amountValue = ui.amount.replace(',', '.').toDoubleOrNull()
            val confirmEnabled = !ui.saving && debts.isNotEmpty() &&
                amountValue != null && amountValue > 0 && amountValue <= totalRemaining + 1e-9
            Button(
                onClick = viewModel::save,
                enabled = confirmEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
