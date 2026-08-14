package com.xateenergia.vendedoresminum.presentation.screens.home

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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xateenergia.vendedoresminum.presentation.components.AppScaffold
import com.xateenergia.vendedoresminum.presentation.components.MinumActionRow
import com.xateenergia.vendedoresminum.presentation.components.MinumMetricCard
import com.xateenergia.vendedoresminum.presentation.components.MinumSectionHeader
import com.xateenergia.vendedoresminum.presentation.theme.MinumColorTokens
import com.xateenergia.vendedoresminum.presentation.theme.MinumSpacing

/**
 * Inicio do aplicativo. A agenda operacional fica em Meu dia, enquanto esta
 * tela organiza os atalhos cotidianos do vendedor sem abrir uma rota por engano.
 */
@Composable
fun HomeScreen(
    onMyDayClick: () -> Unit,
    onNewVisitClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onLogoutClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    AppScaffold(
        title = "Minum",
        actions = {
            IconButton(onClick = onLogoutClick) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sair")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MinumSpacing.Lg, vertical = MinumSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(MinumSpacing.Xl)
        ) {
            MinumSectionHeader(
                eyebrow = "OPERACAO DE CAMPO",
                title = "Seu trabalho, organizado.",
                subtitle = "Acesse a agenda de hoje ou planeje uma nova visita quando precisar."
            )

            if (state.isSyncingCustomers) {
                Column(verticalArrangement = Arrangement.spacedBy(MinumSpacing.Sm)) {
                    Text(
                        text = "Atualizando sua carteira",
                        style = MaterialTheme.typography.bodySmall,
                        color = MinumColorTokens.Text.Secondary
                    )
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MinumColorTokens.Brand.Energy,
                        trackColor = MinumColorTokens.Surface.Subtle
                    )
                }
            }

            state.syncMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MinumColorTokens.Feedback.Error
                )
            }

            HomeAgendaCard(
                routeCount = state.activeSharedRoutes.size,
                stopCount = state.assignedStopsCount,
                onOpenMyDay = onMyDayClick
            )

            Row(horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Md)) {
                MinumMetricCard(
                    label = "Rotas ativas",
                    value = state.activeSharedRoutes.size.toString(),
                    icon = Icons.Default.DirectionsCar,
                    modifier = Modifier.weight(1f),
                    accent = MinumColorTokens.Brand.Energy
                )
                MinumMetricCard(
                    label = "Na sua carteira",
                    value = state.customerCount.toString(),
                    icon = Icons.Default.Groups,
                    modifier = Modifier.weight(1f),
                    accent = MinumColorTokens.Brand.Blue
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(MinumSpacing.Md)) {
                Text(
                    text = "Acessos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                MinumActionRow(
                    icon = Icons.Default.Map,
                    title = "Planejar nova visita",
                    subtitle = "Crie uma rota pontual com clientes proximos da sua carteira.",
                    onClick = onNewVisitClick
                )
                MinumActionRow(
                    icon = Icons.Default.Groups,
                    title = "Clientes",
                    subtitle = "Consulte contatos, dados comerciais e localizacao antes de sair.",
                    onClick = onCustomersClick
                )
                MinumActionRow(
                    icon = Icons.Default.History,
                    title = "Historico de rotas",
                    subtitle = "Revise visitas, feedbacks e resultados anteriores.",
                    onClick = onHistoryClick
                )
                MinumActionRow(
                    icon = Icons.Default.Settings,
                    title = "Configuracoes",
                    subtitle = "Ajuste preferencias do mapa, do aparelho e da sua conta.",
                    onClick = onSettingsClick
                )
            }
        }
    }
}

@Composable
private fun HomeAgendaCard(
    routeCount: Int,
    stopCount: Int,
    onOpenMyDay: () -> Unit
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MinumColorTokens.Brand.Energy
                )
                Text(
                    text = "MEU DIA",
                    style = MaterialTheme.typography.labelMedium,
                    color = MinumColorTokens.Brand.Light,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = if (routeCount > 0) {
                    "$routeCount ${if (routeCount == 1) "rota pronta" else "rotas prontas"} para voce"
                } else {
                    "Sua agenda de campo"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = MinumColorTokens.Text.Inverse,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (stopCount > 0) {
                    "$stopCount clientes para acompanhar, com retornos e metas do dia."
                } else {
                    "Veja rotas compartilhadas, retornos marcados e seus acessos rapidos."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MinumColorTokens.Brand.Light
            )
            Button(
                onClick = onOpenMyDay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MinumColorTokens.Brand.Energy,
                    contentColor = MinumColorTokens.Brand.PrimaryDark
                )
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null)
                Spacer(Modifier.width(MinumSpacing.Sm))
                Text("Abrir Meu dia")
            }
        }
    }
}
