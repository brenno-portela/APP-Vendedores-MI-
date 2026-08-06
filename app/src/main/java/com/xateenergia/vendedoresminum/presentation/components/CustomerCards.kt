package com.xateenergia.vendedoresminum.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xateenergia.vendedoresminum.domain.model.Customer
import com.xateenergia.vendedoresminum.domain.model.NearbyCustomer
import com.xateenergia.vendedoresminum.presentation.theme.MinumColorTokens
import com.xateenergia.vendedoresminum.presentation.theme.MinumSpacing
import com.xateenergia.vendedoresminum.utils.Formatters

@Composable
fun NearbyCustomerCard(
    item: NearbyCustomer,
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    onDetailsClick: () -> Unit,
    onCallClick: () -> Unit,
    onNavigateClick: () -> Unit,
    showNavigateButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onDetailsClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectedChange
            )
            Spacer(Modifier.width(4.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.customer.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text(Formatters.distance(item.distanceMeters)) }
                    )
                }
                if (item.customer.fullAddress.isNotBlank()) {
                    Text(
                        text = item.customer.fullAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                item.routeOrder?.let { order ->
                    Text(
                        text = "Ordem sugerida: $order",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onCallClick, enabled = !item.customer.phone.isNullOrBlank()) {
                Icon(Icons.Default.Call, contentDescription = "Ligar")
            }
            if (showNavigateButton) {
                IconButton(onClick = onNavigateClick) {
                    Icon(Icons.Default.Directions, contentDescription = "Navegar")
                }
            }
            IconButton(onClick = onDetailsClick) {
                Icon(Icons.Default.Info, contentDescription = "Detalhes")
            }
        }
    }
}

@Composable
fun CustomerListCard(
    customer: Customer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MinumColorTokens.Surface.Elevated),
        border = BorderStroke(1.dp, MinumColorTokens.Border.Default),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MinumSpacing.Md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MinumSpacing.Xs)
            ) {
                Text(
                    text = customer.name.ifBlank { "Cliente sem nome" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                customer.clientName
                    ?.takeIf { it.isNotBlank() && it != customer.name }
                    ?.let { company ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = MinumColorTokens.Text.Muted,
                                modifier = Modifier.width(16.dp)
                            )
                            Spacer(Modifier.width(MinumSpacing.Xs))
                            Text(
                                text = company,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                if (customer.fullAddress.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MinumColorTokens.Text.Muted,
                            modifier = Modifier.width(16.dp)
                        )
                        Spacer(Modifier.width(MinumSpacing.Xs))
                        Text(
                            text = customer.fullAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Sm)) {
                    customer.segment?.takeIf { it.isNotBlank() }?.let { segment ->
                        AssistChip(
                            onClick = onClick,
                            label = { Text(segment, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                    (customer.pipelineStage ?: customer.status)
                        ?.takeIf { it.isNotBlank() }
                        ?.let { status ->
                            Text(
                                text = status,
                                style = MaterialTheme.typography.labelMedium,
                                color = MinumColorTokens.Brand.Primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                }
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Abrir detalhes",
                tint = MinumColorTokens.Brand.Primary
            )
        }
    }
}

