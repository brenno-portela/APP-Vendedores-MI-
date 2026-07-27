package com.xateenergia.vendedoresminum.presentation.screens.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xateenergia.vendedoresminum.domain.model.PlannedRouteSummary
import com.xateenergia.vendedoresminum.presentation.components.AppScaffold
import com.xateenergia.vendedoresminum.presentation.components.EmptyState
import com.xateenergia.vendedoresminum.utils.Formatters

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedRouteForStatus by remember { mutableStateOf<PlannedRouteSummary?>(null) }

    AppScaffold(title = "Histórico", onBack = onBack) { padding ->
        if (state.routes.isEmpty()) {
            EmptyState(
                title = "Nenhuma rota planejada",
                message = "Salve uma rota na tela de visita para vê-la aqui.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.routes, key = { it.id }) { route ->
                    HistoryCard(
                        route = route,
                        onUpdateStatusClick = { selectedRouteForStatus = route }
                    )
                }
            }
        }

        selectedRouteForStatus?.let { route ->
            RouteStatusDialog(
                route = route,
                onDismiss = { selectedRouteForStatus = null },
                onSaveStatus = { isCompleted, reason ->
                    viewModel.updateRouteStatus(route.id, isCompleted, reason)
                    selectedRouteForStatus = null
                }
            )
        }
    }
}

@Composable
private fun HistoryCard(
    route: PlannedRouteSummary,
    onUpdateStatusClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Route, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(route.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            route.mainCustomerName?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(onClick = {}, label = { Text("${route.stopCount} paradas") })
                AssistChip(onClick = {}, label = { Text("${route.radiusKm.toInt()} km") })
                AssistChip(onClick = {}, label = { Text(Formatters.dateTime(route.createdAt)) })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusBadge(isCompleted = route.isCompleted, reason = route.notCompletedReason)

                OutlinedButton(onClick = onUpdateStatusClick) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(if (route.isCompleted || route.notCompletedReason != null) "Alterar status" else "Registrar status")
                }
            }

            if (!route.isCompleted && !route.notCompletedReason.isNullOrBlank()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Motivo da não realização:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = route.notCompletedReason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    isCompleted: Boolean,
    reason: String?
) {
    when {
        isCompleted -> {
            AssistChip(
                onClick = {},
                label = { Text("Concluída", fontWeight = FontWeight.SemiBold) },
                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    leadingIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
        !reason.isNullOrBlank() -> {
            AssistChip(
                onClick = {},
                label = { Text("Não realizada", fontWeight = FontWeight.SemiBold) },
                leadingIcon = { Icon(Icons.Default.Cancel, contentDescription = null) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    labelColor = MaterialTheme.colorScheme.onErrorContainer,
                    leadingIconContentColor = MaterialTheme.colorScheme.error
                )
            )
        }
        else -> {
            AssistChip(
                onClick = {},
                label = { Text("Pendente", fontWeight = FontWeight.SemiBold) },
                leadingIcon = { Icon(Icons.Default.Pending, contentDescription = null) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    leadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun RouteStatusDialog(
    route: PlannedRouteSummary,
    onDismiss: () -> Unit,
    onSaveStatus: (isCompleted: Boolean, reason: String?) -> Unit
) {
    var isCompleted by remember { mutableStateOf(route.isCompleted) }
    var notCompletedChoice by remember { mutableStateOf(!route.isCompleted && route.notCompletedReason != null) }
    var reason by remember { mutableStateOf(route.notCompletedReason ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val quickReasons = listOf(
        "Cliente ausente",
        "Problema no transporte",
        "Tempo insuficiente",
        "Cliente reagendou",
        "Endereço incorreto"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Status da Rota", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "A rota \"${route.name}\" foi realizada?",
                    style = MaterialTheme.typography.bodyMedium
                )

                Column(Modifier.selectableGroup()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isCompleted,
                                onClick = {
                                    isCompleted = true
                                    notCompletedChoice = false
                                    errorMessage = null
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isCompleted,
                            onClick = null
                        )
                        Text(
                            text = "Sim, rota realizada",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = notCompletedChoice,
                                onClick = {
                                    isCompleted = false
                                    notCompletedChoice = true
                                    errorMessage = null
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = notCompletedChoice,
                            onClick = null
                        )
                        Text(
                            text = "Não realizada",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                if (notCompletedChoice) {
                    Text(
                        text = "Qual o motivo da não realização?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickReasons.forEach { quick ->
                            FilterChip(
                                selected = reason == quick,
                                onClick = {
                                    reason = quick
                                    errorMessage = null
                                },
                                label = { Text(quick, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = reason,
                        onValueChange = {
                            reason = it
                            errorMessage = null
                        },
                        label = { Text("Motivo da não realização *") },
                        placeholder = { Text("Descreva o motivo...") },
                        isError = errorMessage != null,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    errorMessage?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (notCompletedChoice && reason.isBlank()) {
                        errorMessage = "Informe o motivo para salvar."
                    } else {
                        onSaveStatus(isCompleted, if (notCompletedChoice) reason.trim() else null)
                    }
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
