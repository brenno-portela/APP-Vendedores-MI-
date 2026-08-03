package com.xateenergia.vendedoresminum.presentation.screens.sharedroutes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xateenergia.vendedoresminum.domain.model.SharedRouteAssignment
import com.xateenergia.vendedoresminum.presentation.components.AppScaffold

@Composable
fun SharedRoutesScreen(
    onBack: () -> Unit,
    onStartRoute: (String) -> Unit,
    viewModel: SharedRoutesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    AppScaffold(title = "Rotas compartilhadas", onBack = onBack) { padding ->
        when {
            state.isLoading -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }

            state.error != null -> Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Text(state.error ?: "Nao foi possivel carregar suas rotas.", color = MaterialTheme.colorScheme.error)
            }

            state.routes.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Nenhuma rota compartilhada", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                Text(
                    "Quando um administrador atribuir uma rota, ela aparecera aqui.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Rotas atribuidas pelo administrador para voce cumprir.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(state.routes, key = { it.id }) { route ->
                    SharedRouteCard(route = route, onStart = { onStartRoute(route.id) })
                }
            }
        }
    }
}

@Composable
private fun SharedRouteCard(route: SharedRouteAssignment, onStart: () -> Unit) {
    val isFinished = route.status == "completed" || route.status == "concluida"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(route.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(route.dueDate?.let { "Cumprir ate $it" } ?: "Sem data definida") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
                )
                AssistChip(
                    onClick = {},
                    label = { Text("Meta ${route.targetCompletionPercent}%") },
                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) }
                )
            }
            RouteInfo(icon = Icons.Default.People, text = "${route.stops.size} clientes selecionados")
            route.estimatedDurationSeconds?.let { RouteInfo(Icons.Default.DirectionsCar, "Tempo estimado ${formatDuration(it)}") }
            route.estimatedDistanceMeters?.let { RouteInfo(Icons.Default.DirectionsCar, "${formatDistance(it)} estimados") }
            route.notes?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text("Paradas", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            route.stops.forEach { stop ->
                Text(
                    text = "${stop.order}. ${stop.customer.name}${stop.customer.city?.let { city -> " - $city" }.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onStart, enabled = !isFinished, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.DirectionsCar, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isFinished) "Rota concluida" else "Iniciar rota")
            }
        }
    }
}

@Composable
private fun RouteInfo(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 8.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatDistance(meters: Double): String = if (meters >= 1_000) {
    "${"%.1f".format(meters / 1_000)} km"
} else {
    "${meters.toInt()} m"
}

private fun formatDuration(seconds: Double): String {
    val totalMinutes = (seconds / 60).toInt().coerceAtLeast(1)
    return if (totalMinutes >= 60) "${totalMinutes / 60}h ${totalMinutes % 60}min" else "$totalMinutes min"
}
