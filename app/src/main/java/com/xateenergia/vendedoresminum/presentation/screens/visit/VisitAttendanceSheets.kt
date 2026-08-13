package com.xateenergia.vendedoresminum.presentation.screens.visit

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xateenergia.vendedoresminum.domain.model.AttendancePanelMode
import com.xateenergia.vendedoresminum.domain.model.Coordinate
import com.xateenergia.vendedoresminum.domain.model.Customer
import com.xateenergia.vendedoresminum.domain.model.VisitAttendance
import com.xateenergia.vendedoresminum.domain.model.VisitAttendanceStatus
import com.xateenergia.vendedoresminum.presentation.theme.MinumColorTokens
import com.xateenergia.vendedoresminum.utils.GeoUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private const val MINIMUM_ATTENDANCE_FEEDBACK_LENGTH = 20

/**
 * Gaveta unificada do novo fluxo manual. Cada estado usa o mesmo cliente e o
 * mesmo registro de atendimento, evitando que um retorno apague a visita anterior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitAttendanceSheet(
    mode: AttendancePanelMode,
    customer: Customer,
    attendances: List<VisitAttendance>,
    activeAttendance: VisitAttendance?,
    currentLocation: Coordinate?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onStartCheckIn: () -> Unit,
    onCheckout: () -> Unit,
    onNewCheckIn: () -> Unit,
    onCall: () -> Unit,
    onSaveOutcome: (Boolean, String, String?, String?, String, String) -> Unit
) {
    when (mode) {
        AttendancePanelMode.HIDDEN -> Unit
        AttendancePanelMode.PRE_CHECK_IN -> PreCheckInSheet(
            customer = customer,
            attendances = attendances,
            currentLocation = currentLocation,
            onDismiss = onDismiss,
            onStartCheckIn = onStartCheckIn,
            onCall = onCall
        )
        AttendancePanelMode.IN_PROGRESS -> activeAttendance?.let { attendance ->
            InProgressAttendanceSheet(
                customer = customer,
                attendance = attendance,
                onDismiss = onDismiss,
                onCheckout = onCheckout,
                onCall = onCall
            )
        }
        AttendancePanelMode.POST_CHECK_OUT -> activeAttendance?.let { attendance ->
            CheckoutOutcomeSheet(
                customer = customer,
                attendance = attendance,
                isSaving = isSaving,
                onDismiss = onDismiss,
                onCall = onCall,
                onSaveOutcome = onSaveOutcome
            )
        }
        AttendancePanelMode.RETURN_LIST -> ReturnListSheet(
            customer = customer,
            attendances = attendances,
            onDismiss = onDismiss,
            onNewCheckIn = onNewCheckIn,
            onCall = onCall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreCheckInSheet(
    customer: Customer,
    attendances: List<VisitAttendance>,
    currentLocation: Coordinate?,
    onDismiss: () -> Unit,
    onStartCheckIn: () -> Unit,
    onCall: () -> Unit
) {
    val todayCount = attendances.count { isToday(it.checkInAt) }
    val distance = currentLocation?.let { GeoUtils.haversineDistanceMeters(it, customer.coordinate) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        AttendanceSheetContent {
            AttendanceIdentity(customer = customer, title = "Atendimento ao cliente")
            VisitInfoCard(
                icon = Icons.Default.LocationOn,
                title = "Distancia atual",
                value = distance?.let(::formatDistance) ?: "GPS indisponivel",
                supporting = if (distance == null) "Ative a localizacao para validar o check-in." else "Medida em linha reta ate o cliente."
            )
            VisitInfoCard(
                icon = Icons.Default.Timer,
                title = "Atendimentos hoje",
                value = if (todayCount == 1) "1 atendimento" else "$todayCount atendimentos",
                supporting = if (todayCount == 0) "Nenhum check-in feito neste cliente hoje." else "Os atendimentos anteriores permanecem no historico."
            )
            if (!customer.phone.isNullOrBlank()) {
                OutlinedButton(onClick = onCall, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Call, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ligar para cliente")
                }
            }
            Button(
                onClick = onStartCheckIn,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MinumColorTokens.Brand.Primary)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Iniciar check-in")
            }
            Text(
                text = "O horario e a posicao do GPS serao registrados no inicio do atendimento.",
                style = MaterialTheme.typography.bodySmall,
                color = MinumColorTokens.Text.Secondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InProgressAttendanceSheet(
    customer: Customer,
    attendance: VisitAttendance,
    onDismiss: () -> Unit,
    onCheckout: () -> Unit,
    onCall: () -> Unit
) {
    var now by remember(attendance.id) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(attendance.id) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val elapsedSeconds = ((now - attendance.checkInAt) / 1_000L).coerceAtLeast(0L)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        AttendanceSheetContent {
            Text(
                text = "Check-in em andamento",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MinumColorTokens.Brand.PrimaryDark
            )
            AttendanceIdentity(customer = customer)
            Card(
                colors = CardDefaults.cardColors(containerColor = MinumColorTokens.Surface.Subtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = MinumColorTokens.Brand.Primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = formatDuration(elapsedSeconds),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MinumColorTokens.Brand.PrimaryDark
                    )
                    Text(
                        text = "Desde ${formatHour(attendance.checkInAt)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MinumColorTokens.Text.Secondary
                    )
                }
            }
            GpsValidationCard(
                distanceMeters = attendance.checkInDistanceToCustomerMeters,
                accuracyMeters = attendance.checkInAccuracyMeters,
                actionLabel = "no check-in"
            )
            if (!customer.phone.isNullOrBlank()) {
                OutlinedButton(onClick = onCall, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Call, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ligar para cliente")
                }
            }
            Button(
                onClick = onCheckout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MinumColorTokens.Brand.PrimaryDark,
                    contentColor = Color.White
                )
            ) {
                Text("Fazer checkout")
            }
            Text(
                text = "Ao fazer checkout, informe o resultado e o feedback deste atendimento.",
                style = MaterialTheme.typography.bodySmall,
                color = MinumColorTokens.Text.Secondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckoutOutcomeSheet(
    customer: Customer,
    attendance: VisitAttendance,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onCall: () -> Unit,
    onSaveOutcome: (Boolean, String, String?, String?, String, String) -> Unit
) {
    var wasVisited by remember(attendance.id) { mutableStateOf(true) }
    var feedback by remember(attendance.id) { mutableStateOf("") }
    var notVisitedReason by remember(attendance.id) { mutableStateOf<String?>(null) }
    var commercialOutcome by remember(attendance.id) { mutableStateOf<String?>(null) }
    var nextAction by remember(attendance.id) { mutableStateOf("") }
    var nextActionDueDate by remember(attendance.id) { mutableStateOf("") }
    val feedbackLength = feedback.trim().length
    val canSave = feedbackLength >= MINIMUM_ATTENDANCE_FEEDBACK_LENGTH && !isSaving

    ModalBottomSheet(onDismissRequest = onDismiss) {
        AttendanceSheetContent(scrollable = true) {
            Text(
                text = "Finalizar atendimento",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MinumColorTokens.Brand.PrimaryDark
            )
            AttendanceIdentity(customer = customer)
            CheckoutSummary(attendance)
            GpsValidationCard(
                distanceMeters = attendance.checkOutDistanceToCustomerMeters,
                accuracyMeters = attendance.checkOutAccuracyMeters,
                actionLabel = "no checkout"
            )
            if (!customer.phone.isNullOrBlank()) {
                TextButton(onClick = onCall, modifier = Modifier.align(Alignment.Start)) {
                    Icon(Icons.Default.Call, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Ligar para cliente")
                }
            }
            Text("Qual foi o resultado?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = wasVisited,
                    onClick = { wasVisited = true },
                    label = { Text("Visitado") }
                )
                FilterChip(
                    selected = !wasVisited,
                    onClick = { wasVisited = false },
                    label = { Text("Nao visitado") }
                )
            }
            OutlinedTextField(
                value = feedback,
                onValueChange = { feedback = it },
                label = { Text(if (wasVisited) "Como foi a visita?" else "Por que nao foi possivel atender?") },
                placeholder = { Text(if (wasVisited) "Descreva a conversa e o proximo passo..." else "Descreva o motivo do nao atendimento...") },
                supportingText = { Text("$feedbackLength/$MINIMUM_ATTENDANCE_FEEDBACK_LENGTH caracteres minimos") },
                isError = feedback.isNotBlank() && feedbackLength < MINIMUM_ATTENDANCE_FEEDBACK_LENGTH,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                enabled = !isSaving
            )
            Text(
                text = if (wasVisited) "Resultado comercial (opcional)" else "Motivo padronizado (opcional)",
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val options = if (wasVisited) COMMERCIAL_OUTCOMES else NOT_VISITED_REASONS
                options.forEach { option ->
                    val selected = if (wasVisited) commercialOutcome == option else notVisitedReason == option
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (wasVisited) commercialOutcome = option else notVisitedReason = option
                        },
                        label = { Text(option) },
                        enabled = !isSaving
                    )
                }
            }
            OutlinedTextField(
                value = nextAction,
                onValueChange = { nextAction = it },
                label = { Text("Proximo passo (opcional)") },
                placeholder = { Text("Ex.: Enviar proposta comercial") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                singleLine = true
            )
            OutlinedTextField(
                value = nextActionDueDate,
                onValueChange = { nextActionDueDate = it },
                label = { Text("Data de retorno (opcional)") },
                placeholder = { Text("AAAA-MM-DD") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                singleLine = true
            )
            Button(
                onClick = {
                    onSaveOutcome(
                        wasVisited,
                        feedback,
                        notVisitedReason,
                        commercialOutcome,
                        nextAction,
                        nextActionDueDate
                    )
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MinumColorTokens.Brand.Primary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Salvar atendimento")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReturnListSheet(
    customer: Customer,
    attendances: List<VisitAttendance>,
    onDismiss: () -> Unit,
    onNewCheckIn: () -> Unit,
    onCall: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        AttendanceSheetContent(scrollable = true) {
            Text(
                text = "Historico de atendimentos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MinumColorTokens.Brand.PrimaryDark
            )
            AttendanceIdentity(customer = customer)
            if (attendances.isEmpty()) {
                Text(
                    text = "Nenhum atendimento registrado para este cliente nesta rota.",
                    color = MinumColorTokens.Text.Secondary
                )
            } else {
                attendances.sortedByDescending { it.checkInAt }.forEachIndexed { index, attendance ->
                    PreviousAttendanceCard(attendance = attendance, number = attendances.size - index)
                }
            }
            if (!customer.phone.isNullOrBlank()) {
                OutlinedButton(onClick = onCall, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Call, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ligar para cliente")
                }
            }
            Button(
                onClick = onNewCheckIn,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MinumColorTokens.Brand.Primary)
            ) {
                Text("Novo check-in")
            }
        }
    }
}

@Composable
private fun AttendanceSheetContent(
    scrollable: Boolean = false,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 720.dp)
        .padding(horizontal = 20.dp, vertical = 12.dp)
    Column(
        modifier = if (scrollable) modifier.verticalScroll(rememberScrollState()) else modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun AttendanceIdentity(customer: Customer, title: String? = null) {
    title?.let {
        Text(it, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MinumColorTokens.Brand.PrimaryDark)
    }
    Text(customer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    val company = customer.clientName?.takeIf { it.isNotBlank() }
        ?: customer.cnpjCpf?.takeIf { it.isNotBlank() }
        ?: "Empresa nao informada"
    Text(
        text = company,
        style = MaterialTheme.typography.bodyMedium,
        color = MinumColorTokens.Text.Secondary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun VisitInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    supporting: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MinumColorTokens.Surface.Subtle)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MinumColorTokens.Brand.Primary)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = MinumColorTokens.Text.Secondary)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = MinumColorTokens.Text.Secondary)
            }
        }
    }
}

@Composable
private fun GpsValidationCard(distanceMeters: Double?, accuracyMeters: Float?, actionLabel: String) {
    val isNear = distanceMeters != null && distanceMeters <= 150
    val title = if (isNear) "GPS proximo do cliente" else "Validacao do GPS"
    val supporting = buildString {
        append("Distancia ")
        append(distanceMeters?.let(::formatDistance) ?: "indisponivel")
        append(" $actionLabel")
        accuracyMeters?.let { append(" | precisao ±${it.toInt()} m") }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isNear) MinumColorTokens.Surface.Subtle else MinumColorTokens.Surface.Default
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (isNear) MinumColorTokens.Brand.Primary else MinumColorTokens.Feedback.Warning
            )
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = MinumColorTokens.Text.Secondary)
            }
        }
    }
}

@Composable
private fun CheckoutSummary(attendance: VisitAttendance) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MinumColorTokens.Surface.Subtle)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Resumo do atendimento", style = MaterialTheme.typography.labelLarge, color = MinumColorTokens.Text.Secondary)
            Text("Check-in: ${formatDateTime(attendance.checkInAt)}")
            Text("Checkout: ${attendance.checkOutAt?.let(::formatDateTime) ?: "-"}")
            Text(
                "Permanencia: ${attendance.visitDurationSeconds?.let(::formatDuration) ?: "-"}",
                fontWeight = FontWeight.SemiBold,
                color = MinumColorTokens.Brand.PrimaryDark
            )
        }
    }
}

@Composable
private fun PreviousAttendanceCard(attendance: VisitAttendance, number: Int) {
    val isVisited = attendance.status == VisitAttendanceStatus.VISITED
    val statusLabel = when (attendance.status) {
        VisitAttendanceStatus.VISITED -> "Visitado"
        VisitAttendanceStatus.NOT_VISITED -> "Nao visitado"
        VisitAttendanceStatus.IN_PROGRESS -> "Em atendimento"
        VisitAttendanceStatus.AWAITING_FEEDBACK -> "Aguardando feedback"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MinumColorTokens.Surface.Default)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Atendimento $number", fontWeight = FontWeight.SemiBold)
                Text(
                    statusLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isVisited) MinumColorTokens.Brand.Primary else MinumColorTokens.Feedback.Warning
                )
            }
            Text("Entrada: ${formatDateTime(attendance.checkInAt)} | Saida: ${attendance.checkOutAt?.let(::formatHour) ?: "-"}", style = MaterialTheme.typography.bodySmall)
            Text("Permanencia: ${attendance.visitDurationSeconds?.let(::formatDuration) ?: "-"}", style = MaterialTheme.typography.bodySmall)
            attendance.feedback?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MinumColorTokens.Text.Secondary)
            }
        }
    }
}

private val COMMERCIAL_OUTCOMES = listOf(
    "Interessado",
    "Proposta solicitada",
    "Em negociacao",
    "Venda realizada",
    "Sem interesse"
)

private val NOT_VISITED_REASONS = listOf(
    "Sem responsavel no local",
    "Cliente fechado",
    "Endereco incorreto",
    "Reagendar visita",
    "Sem interesse"
)

private fun isToday(timestamp: Long): Boolean {
    val formatter = SimpleDateFormat("yyyyMMdd", Locale("pt", "BR"))
    return formatter.format(Date(timestamp)) == formatter.format(Date())
}

private fun formatDateTime(timestamp: Long): String =
    SimpleDateFormat("dd/MM, HH:mm", Locale("pt", "BR")).format(Date(timestamp))

private fun formatHour(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date(timestamp))

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3_600
    val minutes = (seconds % 3_600) / 60
    val remainingSeconds = seconds % 60
    return when {
        hours > 0 -> "%dh %02dmin".format(hours, minutes)
        minutes > 0 -> "%d min".format(minutes)
        else -> "%d s".format(remainingSeconds)
    }
}

private fun formatDistance(meters: Double): String {
    return if (meters < 1_000) "${meters.toInt()} m" else "%.1f km".format(Locale("pt", "BR"), meters / 1_000)
}
