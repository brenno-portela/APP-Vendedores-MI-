package com.xateenergia.vendedoresminum.presentation.screens.home

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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xateenergia.vendedoresminum.presentation.components.AppScaffold
import com.xateenergia.vendedoresminum.presentation.components.MinumActionRow
import com.xateenergia.vendedoresminum.presentation.components.MinumMetricCard
import com.xateenergia.vendedoresminum.presentation.components.MinumSectionHeader
import com.xateenergia.vendedoresminum.presentation.theme.MinumColorTokens
import com.xateenergia.vendedoresminum.presentation.theme.MinumSpacing

@Composable
fun HomeScreen(
    onNewVisitClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSharedRoutesClick: () -> Unit,
    onLogoutClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    AppScaffold(
        title = "Inicio",
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
                title = "Sua rota comeca aqui.",
                subtitle = "Organize visitas, acompanhe clientes proximos e registre cada resultado com clareza."
            )

            if (state.isSyncingCustomers) {
                Column(verticalArrangement = Arrangement.spacedBy(MinumSpacing.Sm)) {
                    Text(
                        text = "Atualizando sua base de clientes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

            Row(horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Md)) {
                MinumMetricCard(
                    label = "Clientes",
                    value = state.customerCount.toString(),
                    icon = Icons.Default.Groups,
                    modifier = Modifier.weight(1f)
                )
                MinumMetricCard(
                    label = "Rotas salvas",
                    value = state.plannedRoutesCount.toString(),
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    modifier = Modifier.weight(1f),
                    accent = MinumColorTokens.Brand.Blue
                )
            }

            Button(
                onClick = onNewVisitClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MinumColorTokens.Brand.Primary,
                    contentColor = MinumColorTokens.Text.Inverse
                )
            ) {
                Icon(Icons.Default.Map, contentDescription = null)
                Spacer(Modifier.width(MinumSpacing.Sm))
                Text("Planejar nova visita")
            }

            Column(verticalArrangement = Arrangement.spacedBy(MinumSpacing.Md)) {
                Text(text = "Acessos", style = MaterialTheme.typography.titleMedium)
                MinumActionRow(
                    icon = Icons.Default.Groups,
                    title = "Clientes",
                    subtitle = "Consulte sua base sincronizada e encontre oportunidades proximas.",
                    onClick = onCustomersClick
                )
                MinumActionRow(
                    icon = Icons.Default.DirectionsCar,
                    title = "Rotas compartilhadas",
                    subtitle = "Inicie as rotas atribuidas pela administracao.",
                    onClick = onSharedRoutesClick
                )
                MinumActionRow(
                    icon = Icons.Default.History,
                    title = "Historico de rotas",
                    subtitle = "Revise as visitas realizadas e seus feedbacks.",
                    onClick = onHistoryClick
                )
                MinumActionRow(
                    icon = Icons.Default.Settings,
                    title = "Configuracoes",
                    subtitle = "Ajuste preferencias de rota, mapa e dados locais.",
                    onClick = onSettingsClick
                )
            }
        }
    }
}
