package com.xateenergia.vendedoresminum.presentation.screens.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xateenergia.vendedoresminum.presentation.components.AppScaffold
import com.xateenergia.vendedoresminum.presentation.components.CustomerListCard
import com.xateenergia.vendedoresminum.presentation.components.EmptyState
import com.xateenergia.vendedoresminum.presentation.components.MinumSectionHeader
import com.xateenergia.vendedoresminum.presentation.theme.MinumColorTokens
import com.xateenergia.vendedoresminum.presentation.theme.MinumSpacing

@Composable
fun CustomerListScreen(
    onBack: () -> Unit,
    onCustomerClick: (Long) -> Unit,
    viewModel: CustomerListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    AppScaffold(title = "Clientes", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = MinumSpacing.Lg, vertical = MinumSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(MinumSpacing.Md)
        ) {
            MinumSectionHeader(
                eyebrow = "BASE COMPARTILHADA",
                title = "Clientes do seu territorio",
                subtitle = "Consulte dados e contatos antes de iniciar a visita."
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MinumColorTokens.Surface.Elevated),
                border = BorderStroke(1.dp, MinumColorTokens.Border.Default),
                elevation = CardDefaults.cardElevation(defaultElevation = MinumSpacing.Xs)
            ) {
                Column(
                    modifier = Modifier.padding(MinumSpacing.Md),
                    verticalArrangement = Arrangement.spacedBy(MinumSpacing.Sm)
                ) {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::setQuery,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        label = { Text("Buscar por cliente, cidade ou endereco") }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${state.customers.size} clientes encontrados",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (state.isSyncing) {
                            Text(
                                text = "Atualizando",
                                style = MaterialTheme.typography.labelMedium,
                                color = MinumColorTokens.Brand.Primary
                            )
                        }
                    }
                    if (state.isSyncing) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MinumColorTokens.Brand.Primary,
                            trackColor = MinumColorTokens.Surface.Subtle
                        )
                    }
                }
            }

            state.syncMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MinumColorTokens.Feedback.Error
                )
            }

            if (state.customers.isEmpty()) {
                EmptyState(
                    title = "Nenhum cliente encontrado",
                    message = "Aguarde a sincronizacao do Firebase ou ajuste sua busca.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(MinumSpacing.Sm)
                ) {
                    items(state.customers, key = { it.id }) { customer ->
                        CustomerListCard(
                            customer = customer,
                            onClick = { onCustomerClick(customer.id) }
                        )
                    }
                }
            }
        }
    }
}
