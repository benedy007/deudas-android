package com.benedy.deudas.ui.clients

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benedy.deudas.R
import com.benedy.deudas.ui.components.ClientAvatar
import com.benedy.deudas.ui.components.EmptyState
import com.benedy.deudas.ui.theme.ClientOverdueBg
import com.benedy.deudas.ui.theme.ClientRecentBg
import com.benedy.deudas.ui.theme.ClientStatusOnBg
import com.benedy.deudas.ui.util.WhatsAppHelper
import com.benedy.deudas.ui.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsListScreen(
    viewModel: ClientsListViewModel,
    title: String = stringResource(R.string.clients_title),
    subtitle: String? = null,
    emptyMessage: String? = null,
    showAddFab: Boolean = true,
    onBack: () -> Unit,
    onAddClient: () -> Unit,
    onClientClick: (Long) -> Unit
) {
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val listReady by viewModel.listReady.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val debtFilter by viewModel.debtFilter.collectAsStateWithLifecycle()
    var showSortSheet by remember { mutableStateOf(false) }
    val sortSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showSortSheet = true }) {
                        Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = stringResource(R.string.clients_sort))
                    }
                }
            )
        },
        floatingActionButton = {
            if (showAddFab) {
                FloatingActionButton(
                    onClick = onAddClient,
                    modifier = Modifier.navigationBarsPadding(),
                    elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(defaultElevation = 10.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_client_title))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                label = { Text(stringResource(R.string.clients_search_hint)) },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQuery("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.clear_search))
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )

            AnimatedVisibility(visible = viewModel.showDebtFilter) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = debtFilter == ClientDebtFilter.ALL,
                        onClick = { viewModel.onDebtFilter(ClientDebtFilter.ALL) },
                        label = { Text(stringResource(R.string.client_filter_all)) }
                    )
                    FilterChip(
                        selected = debtFilter == ClientDebtFilter.WITH_DEBT,
                        onClick = { viewModel.onDebtFilter(ClientDebtFilter.WITH_DEBT) },
                        label = { Text(stringResource(R.string.client_filter_with_debt)) }
                    )
                    FilterChip(
                        selected = debtFilter == ClientDebtFilter.WITHOUT_DEBT,
                        onClick = { viewModel.onDebtFilter(ClientDebtFilter.WITHOUT_DEBT) },
                        label = { Text(stringResource(R.string.client_filter_without_debt)) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = sort == ClientSortMode.DEBT_HIGH_TO_LOW,
                    onClick = { viewModel.onSortMode(ClientSortMode.DEBT_HIGH_TO_LOW) },
                    label = { Text(stringResource(R.string.sort_chip_debt)) }
                )
                FilterChip(
                    selected = sort == ClientSortMode.NAME_AZ,
                    onClick = { viewModel.onSortMode(ClientSortMode.NAME_AZ) },
                    label = { Text(stringResource(R.string.sort_chip_name)) }
                )
                FilterChip(
                    selected = sort == ClientSortMode.FECHA_ENTREGA,
                    onClick = { viewModel.onSortMode(ClientSortMode.FECHA_ENTREGA) },
                    label = { Text(stringResource(R.string.sort_chip_fecha)) }
                )
            }

            Spacer(Modifier.height(4.dp))

            AnimatedContent(
                targetState = Triple(listReady, clients.isEmpty(), debtFilter to (query to sort)),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "clientsListContent"
            ) { (ready, isEmpty, _) ->
                when {
                    !ready -> {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    isEmpty -> {
                        val message = when {
                            query.isNotBlank() -> stringResource(R.string.clients_search_empty)
                            debtFilter != ClientDebtFilter.ALL -> stringResource(R.string.empty_filter_clients)
                            emptyMessage != null -> emptyMessage
                            else -> stringResource(R.string.clients_empty)
                        }
                        EmptyState(
                            message = message,
                            title = if (query.isBlank() && debtFilter == ClientDebtFilter.ALL) {
                                stringResource(R.string.empty_clients_title)
                            } else null,
                            icon = Icons.Outlined.People,
                            actionLabel = if (showAddFab && query.isBlank() && debtFilter == ClientDebtFilter.ALL) {
                                stringResource(R.string.empty_clients_cta)
                            } else null,
                            onAction = if (showAddFab) onAddClient else null
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 88.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (subtitle != null) {
                                item(key = "subtitle", contentType = "subtitle") {
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                            }
                            items(
                                items = clients,
                                key = { "client-${it.client.id}" },
                                contentType = { "client" }
                            ) { item ->
                                ClientRow(item = item, onClick = { onClientClick(item.client.id) })
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
            sheetState = sortSheetState
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    text = stringResource(R.string.clients_sort_sheet_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.sort_by_debt)) },
                    modifier = Modifier.clickable {
                        viewModel.onSortMode(ClientSortMode.DEBT_HIGH_TO_LOW)
                        showSortSheet = false
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.sort_by_name)) },
                    modifier = Modifier.clickable {
                        viewModel.onSortMode(ClientSortMode.NAME_AZ)
                        showSortSheet = false
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.sort_by_fecha_entrega)) },
                    modifier = Modifier.clickable {
                        viewModel.onSortMode(ClientSortMode.FECHA_ENTREGA)
                        showSortSheet = false
                    }
                )
                TextButton(
                    onClick = { showSortSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

/**
 * Compact list row: avatar + name + total adeudado + WhatsApp.
 * Status colors: recent payment (green) wins over overdue (red).
 */
@Composable
private fun ClientRow(item: ClientListItem, onClick: () -> Unit) {
    val client = item.client
    val context = LocalContext.current

    val usesStatusBg = item.hasRecentPayment || item.isOverdue
    val containerColor = when {
        item.hasRecentPayment -> ClientRecentBg
        item.isOverdue -> ClientOverdueBg
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = if (usesStatusBg) ClientStatusOnBg else MaterialTheme.colorScheme.onSurface
    val iconColor = if (usesStatusBg) ClientStatusOnBg else MaterialTheme.colorScheme.onSurfaceVariant
    val waTint = if (usesStatusBg) Color(0xFF1B5E20) else Color(0xFF25D366)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClientAvatar(
                photoPath = client.photoPath,
                size = 44.dp,
                iconTint = iconColor,
                placeholderBg = if (usesStatusBg) Color.White.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = client.name,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatMoney(item.totalRemaining),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (usesStatusBg) contentColor.copy(alpha = 0.92f)
                    else MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
            if (client.phone.isNotBlank()) {
                IconButton(
                    onClick = { WhatsAppHelper.openChat(context, client.phone) },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Chat,
                        contentDescription = stringResource(R.string.open_whatsapp),
                        tint = waTint
                    )
                }
            }
        }
    }
}
