package com.xateenergia.vendedoresminum.presentation.screens.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xateenergia.vendedoresminum.domain.model.Customer
import com.xateenergia.vendedoresminum.presentation.components.AppScaffold
import com.xateenergia.vendedoresminum.presentation.components.EmptyState
import com.xateenergia.vendedoresminum.presentation.components.LoadingState
import com.xateenergia.vendedoresminum.presentation.components.MinumLine
import com.xateenergia.vendedoresminum.presentation.theme.MinumColorTokens
import com.xateenergia.vendedoresminum.presentation.theme.MinumRadii
import com.xateenergia.vendedoresminum.presentation.theme.MinumSpacing
import com.xateenergia.vendedoresminum.presentation.utils.ExternalIntents
import java.util.Locale

@Composable
fun CustomerDetailScreen(
    customerId: Long,
    onBack: () -> Unit,
    viewModel: CustomerDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(customerId) {
        viewModel.load(customerId)
    }

    AppScaffold(
        title = "Detalhes do cliente",
        onBack = onBack
    ) { padding ->
        when {
            state.isLoading -> LoadingState(
                message = "Carregando cliente...",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )

            state.customer == null -> EmptyState(
                title = "Cliente indisponivel",
                message = state.error ?: "Nao foi possivel carregar este cadastro.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )

            else -> {
                val customer = state.customer!!
                CustomerDetailsContent(
                    customer = customer,
                    onCall = { ExternalIntents.dial(context, customer.phone) },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun CustomerDetailsContent(
    customer: Customer,
    onCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MinumSpacing.Lg, vertical = MinumSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(MinumSpacing.Md)
    ) {
        CustomerIdentityHeader(customer = customer, onCall = onCall)

        CustomerDetailSection(
            icon = Icons.Default.Info,
            title = "Visao geral",
            subtitle = "Dados que identificam este cadastro."
        ) {
            CustomerDataRows(
                items = listOf(
                    CustomerDataItem("Oportunidade", customer.opportunity),
                    CustomerDataItem("Cliente original", customer.clientName),
                    CustomerDataItem("CNPJ/CPF", customer.cnpjCpf),
                    CustomerDataItem("ID externo", customer.externalId),
                    CustomerDataItem("Status do cadastro", if (customer.active) "Ativo" else "Inativo"),
                    CustomerDataItem("Pais", customer.country)
                )
            )
        }

        CustomerDetailSection(
            icon = Icons.Default.Place,
            title = "Contato e localizacao",
            subtitle = "Use estas informacoes para chegar e falar com o cliente."
        ) {
            CustomerDataRows(
                items = listOf(
                    CustomerDataItem("Telefone", customer.phone),
                    CustomerDataItem("E-mail", customer.email),
                    CustomerDataItem("Logradouro", customer.address),
                    CustomerDataItem("Cidade", customer.city),
                    CustomerDataItem("Estado", customer.state),
                    CustomerDataItem("Latitude", customer.latitude.toDisplayCoordinate()),
                    CustomerDataItem("Longitude", customer.longitude.toDisplayCoordinate())
                )
            )
        }

        CustomerDetailSection(
            icon = Icons.Default.Business,
            title = "Contexto comercial",
            subtitle = "Informacoes para preparar a abordagem."
        ) {
            CustomerDataRows(
                items = listOf(
                    CustomerDataItem("Segmento", customer.segment),
                    CustomerDataItem("Status comercial", customer.status),
                    CustomerDataItem("Estagio do funil", customer.pipelineStage),
                    CustomerDataItem("Receita esperada", customer.expectedRevenue),
                    CustomerDataItem("Responsavel", customer.responsavel),
                    CustomerDataItem("Vendedor", customer.responsableSalesperson),
                    CustomerDataItem("Distribuidor", customer.distributor),
                    CustomerDataItem("Origem", customer.origem),
                    CustomerDataItem("Tags", customer.tags)
                )
            )
        }

        CustomerDetailSection(
            icon = Icons.Default.Person,
            title = "Observacoes",
            subtitle = "Historico e anotacoes recebidos da base."
        ) {
            CustomerDataRows(
                items = listOf(
                    CustomerDataItem("Notas", customer.notes),
                    CustomerDataItem("Ultima atualizacao", customer.ultimaAtualizacao)
                )
            )
        }
    }
}

@Composable
private fun CustomerIdentityHeader(
    customer: Customer,
    onCall: () -> Unit
) {
    val companyName = customer.clientName
        ?.takeIf { it.isNotBlank() && it != customer.name }
        ?: customer.cnpjCpf?.takeIf { it.isNotBlank() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MinumColorTokens.Surface.Elevated),
        border = BorderStroke(1.dp, MinumColorTokens.Border.Default),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(MinumSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(MinumSpacing.Md)
        ) {
            Text(
                text = "CLIENTE",
                style = MaterialTheme.typography.labelMedium,
                color = MinumColorTokens.Brand.Primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = customer.name.ifBlank { "Cliente sem nome" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            companyName?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Xs)
                ) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        tint = MinumColorTokens.Text.Muted,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MinumColorTokens.Text.Secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(MinumSpacing.Sm)) {
                CustomerStatusPill(
                    label = if (customer.active) "Cadastro ativo" else "Cadastro inativo",
                    highlighted = customer.active
                )
                customer.segment?.takeIf { it.isNotBlank() }?.let { segment ->
                    CustomerStatusPill(label = segment, highlighted = false)
                }
            }

            customer.fullAddress.takeIf { it.isNotBlank() }?.let { address ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Xs)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MinumColorTokens.Brand.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MinumColorTokens.Text.Secondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            MinumLine()

            Button(
                onClick = onCall,
                enabled = !customer.phone.isNullOrBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MinumColorTokens.Brand.Primary,
                    contentColor = MinumColorTokens.Text.Inverse
                )
            ) {
                Icon(Icons.Default.Call, contentDescription = null)
                Spacer(Modifier.width(MinumSpacing.Sm))
                Text(if (customer.phone.isNullOrBlank()) "Telefone indisponivel" else "Ligar para cliente")
            }
        }
    }
}

@Composable
private fun CustomerStatusPill(
    label: String,
    highlighted: Boolean
) {
    val background = if (highlighted) {
        MinumColorTokens.Surface.Subtle
    } else {
        MinumColorTokens.Surface.Default
    }
    val textColor = if (highlighted) {
        MinumColorTokens.Brand.PrimaryDark
    } else {
        MinumColorTokens.Text.Secondary
    }

    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = textColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(MinumRadii.Medium))
            .background(background)
            .padding(horizontal = MinumSpacing.Sm, vertical = MinumSpacing.Xs)
    )
}

@Composable
private fun CustomerDetailSection(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MinumColorTokens.Surface.Elevated),
        border = BorderStroke(1.dp, MinumColorTokens.Border.Default),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(MinumSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(MinumSpacing.Md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Sm)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(MinumRadii.Medium))
                        .background(MinumColorTokens.Surface.Subtle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MinumColorTokens.Brand.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MinumColorTokens.Text.Secondary
                    )
                }
            }
            HorizontalDivider(color = MinumColorTokens.Border.Default)
            content()
        }
    }
}

private data class CustomerDataItem(
    val label: String,
    val value: String?
)

@Composable
private fun CustomerDataRows(items: List<CustomerDataItem>) {
    items.forEachIndexed { index, item ->
        CustomerDataRow(item)
        if (index < items.lastIndex) {
            HorizontalDivider(color = MinumColorTokens.Border.Default)
        }
    }
}

@Composable
private fun CustomerDataRow(item: CustomerDataItem) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MinumSpacing.Xs)
    ) {
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium,
            color = MinumColorTokens.Text.Muted
        )
        Text(
            text = item.value?.takeIf { it.isNotBlank() } ?: "Nao informado",
            style = MaterialTheme.typography.bodyMedium,
            color = MinumColorTokens.Text.Primary
        )
    }
}

private fun Double.toDisplayCoordinate(): String = String.format(Locale.US, "%.6f", this)
