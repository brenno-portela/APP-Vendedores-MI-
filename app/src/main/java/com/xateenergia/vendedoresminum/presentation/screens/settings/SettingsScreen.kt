package com.xateenergia.vendedoresminum.presentation.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xateenergia.vendedoresminum.presentation.components.AppScaffold
import com.xateenergia.vendedoresminum.presentation.components.MinumSectionHeader
import com.xateenergia.vendedoresminum.presentation.theme.MinumColorTokens
import com.xateenergia.vendedoresminum.presentation.theme.MinumSpacing

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showConfirmClear by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    AppScaffold(title = "Configuracoes", onBack = onBack) { padding ->
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MinumSpacing.Lg, vertical = MinumSpacing.Xl),
                verticalArrangement = Arrangement.spacedBy(MinumSpacing.Lg)
            ) {
                MinumSectionHeader(
                    eyebrow = "PREFERENCIAS",
                    title = "Ajustes para o seu dia",
                    subtitle = "Personalize o planejamento no aparelho sem alterar a base compartilhada."
                )

                SettingsPanel(
                    title = "Planejamento",
                    subtitle = "Defina como uma nova visita comeca.",
                    icon = Icons.Default.Route
                ) {
                    PreferenceLabel(
                        title = "Raio padrao de busca",
                        subtitle = "Usado ao abrir uma nova rota de visitas."
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Sm)
                    ) {
                        listOf(1.0, 2.0, 5.0, 10.0, 20.0).forEach { radius ->
                            FilterChip(
                                selected = state.defaultRadiusKm == radius,
                                onClick = { viewModel.setDefaultRadius(radius) },
                                label = { Text("${radius.toInt()} km") }
                            )
                        }
                    }
                }

                SettingsPanel(
                    title = "Mapa e clientes",
                    subtitle = "Ajustes de consulta para trabalhar com mais clareza.",
                    icon = Icons.Default.Map
                ) {
                    PreferenceLabel(
                        title = "Visualizacao do mapa",
                        subtitle = "Escolha a camada usada ao planejar visitas."
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Sm)
                    ) {
                        listOf("NORMAL" to "Padrao", "SATELLITE" to "Satelite").forEach { (mode, label) ->
                            FilterChip(
                                selected = state.mapMode == mode,
                                onClick = { viewModel.setMapMode(mode) },
                                label = { Text(label) }
                            )
                        }
                    }
                    PreferenceToggle(
                        icon = Icons.Default.FilterAlt,
                        title = "Priorizar clientes ativos",
                        subtitle = "Oculta cadastros inativos ao iniciar uma busca.",
                        checked = state.onlyActiveByDefault,
                        onCheckedChange = viewModel::setOnlyActiveByDefault
                    )
                }

                SettingsPanel(
                    title = "Sincronizacao",
                    subtitle = "Informacoes operacionais compartilhadas com seguranca.",
                    icon = Icons.Default.CloudDone
                ) {
                    PreferenceLabel(
                        title = "Firebase em tempo real",
                        subtitle = "Clientes, rotas e feedbacks acompanham as alteracoes feitas no backoffice."
                    )
                    Text(
                        text = "O historico mostrado no aplicativo vem do Firebase. Ao excluir uma rota no backoffice, ela deixa de aparecer aqui automaticamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                SettingsPanel(
                    title = "Armazenamento local",
                    subtitle = "Use apenas quando precisar renovar o cache deste aparelho.",
                    icon = Icons.Default.DeleteOutline
                ) {
                    OutlinedButton(
                        onClick = { showConfirmClear = true },
                        enabled = !state.isClearing,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MinumColorTokens.Border.Strong)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = MinumColorTokens.Feedback.Error
                        )
                        Spacer(Modifier.width(MinumSpacing.Sm))
                        Text("Limpar cache deste aparelho")
                    }
                    Text(
                        text = "Esta acao nao exclui clientes, rotas ou feedbacks do Firebase.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showConfirmClear) {
        AlertDialog(
            onDismissRequest = { showConfirmClear = false },
            title = { Text("Limpar cache local?") },
            text = {
                Text(
                    "Os dados salvos neste aparelho serao renovados. Clientes, rotas e feedbacks do Firebase continuarao protegidos."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmClear = false
                        viewModel.clearAllData()
                    }
                ) {
                    Text("Limpar cache")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClear = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun SettingsPanel(
    title: String,
    subtitle: String,
    icon: ImageVector,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MinumColorTokens.Brand.Primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(MinumSpacing.Sm))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun PreferenceLabel(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PreferenceToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MinumColorTokens.Brand.Primary)
            Spacer(Modifier.width(MinumSpacing.Sm))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(MinumSpacing.Md))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MinumColorTokens.Brand.Energy,
                checkedTrackColor = MinumColorTokens.Brand.Primary
            )
        )
    }
}
