package com.xateenergia.vendedoresminum.presentation.screens.myday

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xateenergia.vendedoresminum.domain.model.SharedRouteAssignment
import com.xateenergia.vendedoresminum.presentation.components.AppScaffold
import com.xateenergia.vendedoresminum.presentation.components.EmptyState
import com.xateenergia.vendedoresminum.presentation.components.LoadingState
import com.xateenergia.vendedoresminum.presentation.components.MinumActionRow
import com.xateenergia.vendedoresminum.presentation.components.MinumMetricCard
import com.xateenergia.vendedoresminum.presentation.components.MinumSectionHeader
import com.xateenergia.vendedoresminum.presentation.theme.MinumColorTokens
import com.xateenergia.vendedoresminum.presentation.theme.MinumSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Agenda de campo do vendedor. Esta tela nao cria rotas: ela mostra o que ja
 * foi atribuido, o que precisa de retorno e abre a navegacao da rota escolhida.
 */
@Composable
fun MyDayScreen(
    onBack: () -> Unit,
    onOpenSharedRoute: (String) -> Unit,
    onOpenCustomer: (Long) -> Unit,
    onOpenCustomers: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: MyDayViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val nowMillis by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000L)
        }
    }

    AppScaffold(title = "Meu dia", onBack = onBack) { padding ->
        when {
            state.isLoading -> LoadingState(
                message = "Montando sua agenda...",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MinumSpacing.Lg, vertical = MinumSpacing.Xl),
                verticalArrangement = Arrangement.spacedBy(MinumSpacing.Xl)
            ) {
                MinumSectionHeader(
                    eyebrow = "AGENDA DE CAMPO",
                    title = "Sua operacao hoje",
                    subtitle = formatAgendaDate(state.today)
                )

                state.error?.let { message ->
                    AgendaMessage(message = message)
                }

                MyDayPrimaryRoute(
                    route = state.priorityRoute,
                    completedStops = state.completedStops,
                    totalStops = state.totalStops,
                    onOpenRoute = onOpenSharedRoute
                )

                DayMetrics(
                    completedStops = state.completedStops,
                    remainingStops = state.remainingStops,
                    timeInRouteSeconds = state.timeInRouteSeconds(nowMillis)
                )

                RevisitSection(
                    revisits = state.revisitsDueToday,
                    onOpenCustomer = onOpenCustomer
                )

                SharedRoutesSection(
                    routes = state.agendaRoutes,
                    onOpenRoute = onOpenSharedRoute
                )

                Column(verticalArrangement = Arrangement.spacedBy(MinumSpacing.Md)) {
                    Text(
                        text = "Acessos rapidos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    MinumActionRow(
                        icon = Icons.Default.Groups,
                        title = "Clientes",
                        subtitle = "Busque dados, contatos e informacoes comerciais antes de sair.",
                        onClick = onOpenCustomers
                    )
                    MinumActionRow(
                        icon = Icons.Default.History,
                        title = "Historico de rotas",
                        subtitle = "Revise visitas, feedbacks e rotas concluidas.",
                        onClick = onOpenHistory
                    )
                }
            }
        }
    }
}

@Composable
private fun MyDayPrimaryRoute(
    route: SharedRouteAssignment?,
    completedStops: Int,
    totalStops: Int,
    onOpenRoute: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MinumColorTokens.Brand.PrimaryDark),
        border = BorderStroke(1.dp, MinumColorTokens.Brand.Primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(MinumSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(MinumSpacing.Md)
        ) {
            Text(
                text = if (route?.status?.lowercase(Locale.ROOT) == "in_progress") {
                    "ROTA EM ANDAMENTO"
                } else {
                    "PRIORIDADE DA AGENDA"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MinumColorTokens.Brand.Light,
                fontWeight = FontWeight.SemiBold
            )

            if (route == null) {
                Text(
                    text = "Nenhuma rota pendente agora.",
                    style = MaterialTheme.typography.titleLarge,
                    color = MinumColorTokens.Text.Inverse,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "As rotas enviadas pela administracao vao aparecer aqui quando estiverem disponiveis.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MinumColorTokens.Brand.Light
                )
            } else {
                Text(
                    text = route.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MinumColorTokens.Text.Inverse,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DarkRouteMeta(Icons.Default.Groups, "$completedStops/$totalStops com resultado")
                    route.dueDate?.let { DarkRouteMeta(Icons.Default.CalendarToday, "Ate ${formatShortDate(it)}") }
                }
                route.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MinumColorTokens.Brand.Light,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(
                    onClick = { onOpenRoute(route.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MinumColorTokens.Brand.Energy,
                        contentColor = MinumColorTokens.Brand.PrimaryDark
                    )
                ) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null)
                    Spacer(Modifier.width(MinumSpacing.Sm))
                    Text(if (route.status.lowercase(Locale.ROOT) == "in_progress") "Continuar navegacao" else "Iniciar navegacao")
                }
            }
        }
    }
}

@Composable
private fun DayMetrics(
    completedStops: Int,
    remainingStops: Int,
    timeInRouteSeconds: Long
) {
    Column(verticalArrangement = Arrangement.spacedBy(MinumSpacing.Md)) {
        Text(
            text = "Meta do dia",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Md)) {
            MinumMetricCard(
                label = "Com resultado",
                value = completedStops.toString(),
                icon = Icons.Default.Route,
                modifier = Modifier.weight(1f),
                accent = MinumColorTokens.Brand.Energy
            )
            MinumMetricCard(
                label = "Restantes",
                value = remainingStops.toString(),
                icon = Icons.Default.Map,
                modifier = Modifier.weight(1f),
                accent = MinumColorTokens.Brand.Blue
            )
        }
        MinumMetricCard(
            label = "Tempo em rota",
            value = if (timeInRouteSeconds > 0L) formatDuration(timeInRouteSeconds) else "-",
            icon = Icons.Default.Schedule,
            modifier = Modifier.fillMaxWidth(),
            accent = MinumColorTokens.Brand.Primary
        )
    }
}

@Composable
private fun RevisitSection(
    revisits: List<MyDayRevisit>,
    onOpenCustomer: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(MinumSpacing.Md)) {
        Text(
            text = "Retornos de hoje",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (revisits.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MinumColorTokens.Surface.Subtle),
                border = BorderStroke(1.dp, MinumColorTokens.Border.Default),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(MinumSpacing.Lg),
                    horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MinumColorTokens.Brand.Primary
                    )
                    Text(
                        text = "Nenhum retorno programado para hoje.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MinumColorTokens.Text.Secondary
                    )
                }
            }
        } else {
            revisits.forEach { revisit ->
                RevisitCard(revisit = revisit, onClick = { onOpenCustomer(revisit.customer.id) })
            }
        }
    }
}

@Composable
private fun RevisitCard(
    revisit: MyDayRevisit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MinumColorTokens.Surface.Elevated),
        border = BorderStroke(1.dp, MinumColorTokens.Border.Default),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(MinumSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(MinumSpacing.Sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = revisit.customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                StatusPill(
                    label = if (revisit.isOverdue) "Em atraso" else "Hoje",
                    color = if (revisit.isOverdue) MinumColorTokens.Feedback.Error else MinumColorTokens.Brand.Primary
                )
            }
            Text(
                text = revisit.nextAction,
                style = MaterialTheme.typography.bodyMedium,
                color = MinumColorTokens.Text.Secondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Origem: ${revisit.routeName} | ${formatShortDate(revisit.dueDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MinumColorTokens.Text.Muted
            )
        }
    }
}

@Composable
private fun SharedRoutesSection(
    routes: List<SharedRouteAssignment>,
    onOpenRoute: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(MinumSpacing.Md)) {
        Text(
            text = "Rotas compartilhadas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (routes.isEmpty()) {
            EmptyState(
                title = "Nenhuma rota atribuida",
                message = "Quando uma rota for enviada para voce, ela aparecera nesta agenda."
            )
        } else {
            routes.forEach { route ->
                SharedRouteAgendaCard(route = route, onOpenRoute = onOpenRoute)
            }
        }
    }
}

@Composable
private fun SharedRouteAgendaCard(
    route: SharedRouteAssignment,
    onOpenRoute: (String) -> Unit
) {
    val isFinished = route.status.lowercase(Locale.ROOT) in setOf(
        "completed", "concluida", "not_completed", "nao_concluida"
    )
    val isInProgress = route.status.lowercase(Locale.ROOT) == "in_progress"
    val resultCount = route.stops.count { it.status.lowercase(Locale.ROOT) in setOf("visited", "not_visited") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MinumColorTokens.Surface.Elevated),
        border = BorderStroke(1.dp, MinumColorTokens.Border.Default),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(MinumSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(MinumSpacing.Md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Md),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MinumSpacing.Xs)) {
                    Text(
                        text = route.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$resultCount/${route.stops.size} paradas com resultado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MinumColorTokens.Text.Secondary
                    )
                }
                StatusPill(
                    label = routeStatusLabel(route.status),
                    color = when {
                        isInProgress -> MinumColorTokens.Brand.Primary
                        isFinished -> MinumColorTokens.Text.Muted
                        else -> MinumColorTokens.Brand.Energy
                    }
                )
            }

            route.dueDate?.let { date ->
                Row(horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Xs), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.width(16.dp),
                        tint = MinumColorTokens.Brand.Primary
                    )
                    Text(
                        text = "Cumprir ate ${formatShortDate(date)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MinumColorTokens.Text.Secondary
                    )
                }
            }

            Button(
                onClick = { onOpenRoute(route.id) },
                enabled = !isFinished,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MinumColorTokens.Brand.Primary,
                    contentColor = MinumColorTokens.Text.Inverse
                )
            ) {
                Icon(Icons.Default.DirectionsCar, contentDescription = null)
                Spacer(Modifier.width(MinumSpacing.Sm))
                Text(
                    when {
                        isFinished -> "Rota encerrada"
                        isInProgress -> "Continuar navegacao"
                        else -> "Iniciar navegacao"
                    }
                )
            }
        }
    }
}

@Composable
private fun DarkRouteMeta(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.width(16.dp),
            tint = MinumColorTokens.Brand.Energy
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MinumColorTokens.Brand.Light,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusPill(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.13f),
        contentColor = color,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = MinumSpacing.Sm, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun AgendaMessage(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MinumColorTokens.Feedback.Error.copy(alpha = 0.1f),
        contentColor = MinumColorTokens.Feedback.Error,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(MinumSpacing.Md),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun routeStatusLabel(status: String): String = when (status.lowercase(Locale.ROOT)) {
    "in_progress" -> "Em andamento"
    "completed", "concluida" -> "Concluida"
    "not_completed", "nao_concluida" -> "Nao concluida"
    else -> "Aguardando"
}

private fun formatAgendaDate(isoDate: String): String = runCatching {
    LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM", Locale("pt", "BR")))
}.getOrDefault(isoDate)

private fun formatShortDate(isoDate: String): String = runCatching {
    LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern("dd/MM", Locale("pt", "BR")))
}.getOrDefault(isoDate)

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3_600L
    val minutes = (seconds % 3_600L) / 60L
    return when {
        hours > 0L -> "${hours}h ${minutes.toString().padStart(2, '0')}min"
        minutes > 0L -> "$minutes min"
        else -> "Agora"
    }
}
