package com.benedy.deudas.ui.products

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Inventory2
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benedy.deudas.R
import com.benedy.deudas.ui.components.EmptyState
import com.benedy.deudas.ui.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsListScreen(
    viewModel: ProductsListViewModel,
    onBack: () -> Unit,
    onAddProduct: () -> Unit
) {
    val products by viewModel.products.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.products_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddProduct,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_product_title))
            }
        }
    ) { padding ->
        if (products.isEmpty()) {
            EmptyState(
                message = stringResource(R.string.products_empty),
                title = stringResource(R.string.empty_products_title),
                icon = Icons.Outlined.Inventory2,
                actionLabel = stringResource(R.string.empty_products_cta),
                onAction = onAddProduct,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 88.dp
                ),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = products,
                    key = { "product-${it.id}" },
                    contentType = { "product" }
                ) { p ->
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        ListItem(
                            headlineContent = { Text(p.name) },
                            supportingContent = {
                                Text(
                                    buildString {
                                        append(formatMoney(p.price))
                                        if (!p.notes.isNullOrBlank()) append(" · ").append(p.notes)
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
