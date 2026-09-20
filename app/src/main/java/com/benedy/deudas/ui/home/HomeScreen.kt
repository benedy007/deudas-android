package com.benedy.deudas.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.benedy.deudas.BuildConfig
import com.benedy.deudas.R
import com.benedy.deudas.data.repository.DebtCrmRepository
import com.benedy.deudas.ui.auth.AuthUiState
import com.benedy.deudas.ui.components.ElevatedActionCard
import com.benedy.deudas.ui.components.ScreenGradient
import com.benedy.deudas.ui.theme.HubChargeContainer
import com.benedy.deudas.ui.theme.HubChargeContent
import com.benedy.deudas.ui.theme.HubChargeIconEnd
import com.benedy.deudas.ui.theme.HubChargeIconStart
import com.benedy.deudas.ui.theme.HubClientsContainer
import com.benedy.deudas.ui.theme.HubClientsContent
import com.benedy.deudas.ui.theme.HubClientsIconEnd
import com.benedy.deudas.ui.theme.HubClientsIconStart
import com.benedy.deudas.ui.theme.HubHistoryContainer
import com.benedy.deudas.ui.theme.HubHistoryContent
import com.benedy.deudas.ui.theme.HubHistoryIconEnd
import com.benedy.deudas.ui.theme.HubHistoryIconStart
import com.benedy.deudas.ui.theme.HubProductsContainer
import com.benedy.deudas.ui.theme.HubProductsContent
import com.benedy.deudas.ui.theme.HubProductsIconEnd
import com.benedy.deudas.ui.theme.HubProductsIconStart
import com.benedy.deudas.ui.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: AuthUiState,
    dashboard: DebtCrmRepository.DashboardStats = DebtCrmRepository.DashboardStats(),
    onCharge: () -> Unit,
    onViewClients: () -> Unit,
    onViewProducts: () -> Unit,
    onPaymentHistory: () -> Unit,
    onSettings: () -> Unit
) {
    val session = uiState.session
    val isGuest = session?.isGuest == true
    val displayName = when {
        isGuest -> stringResource(R.string.guest_display_name)
        else -> session?.displayName?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.user_fallback)
    }

    Scaffold(
        containerColor = Color.Transparent,
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
                    IconButton(onClick = onSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.settings_content_description)
                        )
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
                Text(
                    text = stringResource(R.string.hub_greeting, displayName),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.dashboard_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        DashboardStat(
                            label = stringResource(R.string.dashboard_total_por_cobrar),
                            value = formatMoney(dashboard.totalPorCobrar)
                        )
                        DashboardStat(
                            label = stringResource(R.string.dashboard_cobrado_mes),
                            value = formatMoney(dashboard.cobradoDelMes)
                        )
                        DashboardStat(
                            label = stringResource(R.string.dashboard_cobrado_dia),
                            value = formatMoney(dashboard.cobradoEnElDia)
                        )
                    }
                }

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
                    icon = Icons.Outlined.History,
                    title = stringResource(R.string.action_payment_history),
                    description = stringResource(R.string.action_payment_history_desc),
                    onClick = onPaymentHistory,
                    elevation = 10.dp,
                    containerColor = HubHistoryContainer,
                    contentColor = HubHistoryContent,
                    iconGradient = listOf(HubHistoryIconStart, HubHistoryIconEnd),
                    iconTint = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DashboardStat(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
