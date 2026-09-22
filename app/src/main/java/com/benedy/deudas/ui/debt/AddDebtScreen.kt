package com.benedy.deudas.ui.debt

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benedy.deudas.R
import com.benedy.deudas.data.local.entity.PlanFrequency
import com.benedy.deudas.ui.util.formatMoney
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddDebtScreen(
    viewModel: AddDebtViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es", "DO")) }

    LaunchedEffect(ui.saved) {
        if (ui.saved) onSaved()
    }

    if (ui.creditWarn) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCreditWarn() },
            title = { Text(stringResource(R.string.credit_limit_warn_title)) },
            text = { Text(stringResource(R.string.credit_limit_warn_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDespiteCreditWarn() }) {
                    Text(stringResource(R.string.credit_limit_warn_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCreditWarn() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = ui.fechaEntrega ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { viewModel.onFechaEntrega(it) }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_debt)) },
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (products.isNotEmpty()) {
                Text(stringResource(R.string.choose_product), style = MaterialTheme.typography.titleMedium)
                products.forEach { product ->
                    val selected = ui.selectedProductId == product.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectProduct(product) },
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(product.name, style = MaterialTheme.typography.titleSmall)
                            Text(formatMoney(product.price), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.or_custom_debt), style = MaterialTheme.typography.titleMedium)
            }

            OutlinedTextField(
                value = ui.description,
                onValueChange = viewModel::onDescription,
                label = { Text(stringResource(R.string.debt_description)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = ui.amount,
                onValueChange = viewModel::onAmount,
                label = { Text(stringResource(R.string.debt_amount)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            val fechaText = ui.fechaEntrega?.let { dateFmt.format(Date(it)) }.orEmpty()
            OutlinedTextField(
                value = fechaText,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.fecha_entrega)) },
                placeholder = { Text(stringResource(R.string.fecha_entrega_optional)) },
                trailingIcon = {
                    Row {
                        if (ui.fechaEntrega != null) {
                            IconButton(onClick = viewModel::clearFechaEntrega) {
                                Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.clear_date))
                            }
                        }
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = stringResource(R.string.fecha_entrega))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.plan_section_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.plan_section_optional),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = ui.planFrequency == null,
                    onClick = { viewModel.onPlanFrequency(null) },
                    label = { Text(stringResource(R.string.plan_freq_none)) }
                )
                FilterChip(
                    selected = ui.planFrequency == PlanFrequency.WEEKLY,
                    onClick = { viewModel.onPlanFrequency(PlanFrequency.WEEKLY) },
                    label = { Text(stringResource(R.string.plan_freq_weekly)) }
                )
                FilterChip(
                    selected = ui.planFrequency == PlanFrequency.BIWEEKLY,
                    onClick = { viewModel.onPlanFrequency(PlanFrequency.BIWEEKLY) },
                    label = { Text(stringResource(R.string.plan_freq_biweekly)) }
                )
                FilterChip(
                    selected = ui.planFrequency == PlanFrequency.MONTHLY,
                    onClick = { viewModel.onPlanFrequency(PlanFrequency.MONTHLY) },
                    label = { Text(stringResource(R.string.plan_freq_monthly)) }
                )
            }

            if (ui.planFrequency != null) {
                OutlinedTextField(
                    value = ui.planPercent,
                    onValueChange = viewModel::onPlanPercent,
                    label = { Text(stringResource(R.string.plan_percent_label)) },
                    supportingText = { Text(stringResource(R.string.plan_percent_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                val cuota = ui.calculatedCuota()
                if (cuota != null) {
                    Text(
                        text = stringResource(R.string.plan_cuota_preview, formatMoney(cuota)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            when (ui.error) {
                "required" -> Text(stringResource(R.string.field_required), color = MaterialTheme.colorScheme.error)
                "amount" -> Text(stringResource(R.string.invalid_amount), color = MaterialTheme.colorScheme.error)
                "plan_percent" -> Text(stringResource(R.string.plan_percent_invalid), color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = viewModel::save, enabled = !ui.saving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
