package com.benedy.deudas.ui.clients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Person
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benedy.deudas.R
import com.benedy.deudas.data.local.entity.ClientEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsListScreen(
    viewModel: ClientsListViewModel,
    title: String = stringResource(R.string.clients_title),
    subtitle: String? = null,
    onBack: () -> Unit,
    onAddClient: () -> Unit,
    onClientClick: (Long) -> Unit
) {
    val clients by viewModel.clients.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClient, elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(defaultElevation = 10.dp)) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_client_title))
            }
        }
    ) { padding ->
        if (clients.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                }
                Text(
                    text = stringResource(R.string.clients_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (subtitle != null) {
                    item {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
                items(clients, key = { it.id }) { client ->
                    ClientRow(client = client, onClick = { onClientClick(client.id) })
                }
            }
        }
    }
}

@Composable
private fun ClientRow(client: ClientEntity, onClick: () -> Unit) {
    val extras = listOfNotNull(
        client.direccionCasa?.takeIf { it.isNotBlank() },
        client.lugarTrabajo?.takeIf { it.isNotBlank() },
        client.direccionTrabajo?.takeIf { it.isNotBlank() }
    )
    val supporting = if (extras.isEmpty()) {
        client.phone
    } else {
        client.phone + "\n" + extras.joinToString(" · ")
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        ListItem(
            headlineContent = { Text(client.name) },
            supportingContent = { Text(supporting) },
            leadingContent = {
                Icon(Icons.Outlined.Person, contentDescription = null)
            },
            modifier = Modifier.clickable(onClick = onClick)
        )
    }
}
