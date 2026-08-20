package com.xateenergia.vendedoresminum.presentation.screens.visit

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.mapbox.geojson.Point
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardSatelliteStyle
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.xateenergia.vendedoresminum.domain.model.Coordinate
import com.xateenergia.vendedoresminum.domain.model.AttendancePanelMode
import com.xateenergia.vendedoresminum.domain.model.Customer
import com.xateenergia.vendedoresminum.domain.model.NearbyCustomer
import com.xateenergia.vendedoresminum.presentation.components.AppScaffold
import com.xateenergia.vendedoresminum.presentation.components.EmptyState
import com.xateenergia.vendedoresminum.presentation.components.MinumLine
import com.xateenergia.vendedoresminum.presentation.components.NearbyCustomerCard
import com.xateenergia.vendedoresminum.presentation.theme.MinumColorTokens
import com.xateenergia.vendedoresminum.presentation.theme.MinumRadii
import com.xateenergia.vendedoresminum.presentation.theme.MinumSpacing
import com.xateenergia.vendedoresminum.presentation.utils.ExternalIntents
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitMapScreen(
    onBack: () -> Unit,
    onCustomerClick: (Long) -> Unit,
    onNavigateToHistory: () -> Unit,
    sharedRouteId: String? = null,
    viewModel: VisitPlanningViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showCustomerPicker by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.startLocationTracking()
        }
    }

    val requestCurrentLocation = {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            viewModel.useCurrentLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // A rota compartilhada nao usa a localizacao como nova origem do planejador.
    // Ela so precisa iniciar o rastreamento para abrir a navegacao oficial.
    val requestSharedRouteLocation = {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            viewModel.startLocationTracking()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(state.shouldNavigateToHistory) {
        if (!state.shouldNavigateToHistory) return@LaunchedEffect
        viewModel.consumeHistoryNavigation()
        onNavigateToHistory()
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            viewModel.startLocationTracking()
        }
    }

    LaunchedEffect(sharedRouteId) {
        sharedRouteId?.let { routeId ->
            requestSharedRouteLocation()
            viewModel.loadSharedRoute(routeId)
        }
    }

    // A navegacao guiada depende da permissao para receber o GPS em tempo real.
    // Pedimos novamente ao entrar nesse modo, pois o vendedor pode ter planejado a rota
    // sem antes tocar no botao de localizar a propria posicao.
    LaunchedEffect(state.isNavigationActive) {
        if (!state.isNavigationActive) return@LaunchedEffect

        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            viewModel.startLocationTracking()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    if (state.isNavigationActive) {
        Box(modifier = Modifier.fillMaxSize()) {
            OfficialMapboxNavigationExperience(
                state = state,
                modifier = Modifier.fillMaxSize(),
                onStopNavigation = viewModel::requestNavigationFinish,
                onNextStop = viewModel::navigateToNextStop,
                onShowPendingStops = viewModel::showPendingStops,
                onStopMarkerClick = viewModel::openAttendance
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            val attendanceCustomer = state.attendanceCustomerId?.let { customerId ->
                state.optimizedStops.firstOrNull { it.customer.id == customerId }?.customer
                    ?: state.nearbyCustomers.firstOrNull { it.customer.id == customerId }?.customer
            }
            if (attendanceCustomer != null && state.attendancePanelMode != AttendancePanelMode.HIDDEN) {
                VisitAttendanceSheet(
                    mode = state.attendancePanelMode,
                    customer = attendanceCustomer,
                    attendances = state.attendanceHistoryByCustomer[attendanceCustomer.id].orEmpty(),
                    activeAttendance = state.activeAttendance?.takeIf { it.customerId == attendanceCustomer.id },
                    currentLocation = state.currentLocation,
                    isSaving = state.isSavingAttendance,
                    onDismiss = viewModel::closeAttendancePanel,
                    onStartCheckIn = { viewModel.startCustomerCheckIn(attendanceCustomer) },
                    onCheckout = viewModel::checkoutActiveAttendance,
                    onNewCheckIn = { viewModel.startCustomerCheckIn(attendanceCustomer) },
                    onCall = { ExternalIntents.dial(context, attendanceCustomer.phone) },
                    onSaveOutcome = { wasVisited, feedback, notVisitedReason, commercialOutcome, nextAction, nextActionDueDate ->
                        viewModel.saveAttendanceOutcome(
                            customer = attendanceCustomer,
                            wasVisited = wasVisited,
                            feedback = feedback,
                            notVisitedReason = notVisitedReason,
                            commercialOutcome = commercialOutcome,
                            nextAction = nextAction,
                            nextActionDueDate = nextActionDueDate
                        )
                    }
                )
            }
            if (state.showIncompleteRouteDialog) {
                IncompleteRouteSheet(
                    completedStops = state.completedStopCount(),
                    totalStops = state.optimizedStops.size,
                    isSaving = state.isFinishingNavigation,
                    onDismiss = viewModel::dismissIncompleteRouteDialog,
                    onConfirm = viewModel::finishNavigationAsNotCompleted
                )
            }
            if (state.showPendingStopsSheet) {
                PendingStopsSheet(
                    state = state,
                    onDismiss = viewModel::dismissPendingStops,
                    onNavigateToStop = viewModel::navigateToPendingStop
                )
            }
        }
        return
    }

    // Uma rota recebida da agenda nunca deve cair no criador de rota. Enquanto
    // o GPS e o Mapbox sao preparados, mantemos um estado de passagem claro.
    if (sharedRouteId != null) {
        SharedRouteLaunchingScreen(
            routeName = state.sharedRouteName,
            hasLoadedRoute = state.activeSharedRouteId == sharedRouteId,
            hasLocation = state.currentLocation != null,
            isLoading = state.isRouteLoading,
            message = state.message,
            onBack = onBack,
            onRequestLocation = requestSharedRouteLocation,
            onRetry = {
                requestSharedRouteLocation()
                viewModel.loadSharedRoute(sharedRouteId)
            },
            onStartNow = viewModel::startNavigation
        )
        return
    }

    AppScaffold(title = "Mapa de visita", onBack = onBack) { padding ->
        BottomSheetScaffold(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            sheetPeekHeight = 132.dp,
            sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            sheetContainerColor = MinumColorTokens.Surface.Elevated,
            sheetContentColor = MinumColorTokens.Text.Primary,
            sheetTonalElevation = 0.dp,
            sheetShadowElevation = 8.dp,
            sheetDragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = MinumSpacing.Sm, bottom = MinumSpacing.Xs)
                        .width(44.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MinumColorTokens.Border.Strong)
                )
            },
            sheetContent = {
                RouteBottomSheetContent(
                    state = state,
                    onLatitudeChange = viewModel::setManualLatitude,
                    onLongitudeChange = viewModel::setManualLongitude,
                    onApplyCoordinate = viewModel::applyManualCoordinate,
                    onAddressChange = viewModel::setAddressQuery,
                    onSearchAddress = viewModel::searchAddress,
                    onUseCurrentLocation = requestCurrentLocation,
                    onPickCustomer = { showCustomerPicker = true },
                    onRadiusChange = viewModel::setRadiusKm,
                    onSegmentChange = viewModel::setSegment,
                    onCityChange = viewModel::setCity,
                    onStateChange = viewModel::setState,
                    onStatusChange = viewModel::setStatus,
                    onOnlyWithPhoneChange = viewModel::setOnlyWithPhone,
                    onOnlyActiveChange = viewModel::setOnlyActive,
                    onSelectAll = viewModel::selectAllNearby,
                    onClearSelection = viewModel::clearSelection,
                    onOptimize = viewModel::optimizeRoute,
                    onSave = viewModel::saveRoute,
                    onStartNavigation = viewModel::startNavigation,
                    onCustomerSelected = viewModel::toggleCustomerSelection,
                    onCustomerClick = onCustomerClick,
                    onCallClick = { phone -> ExternalIntents.dial(context, phone) }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { _ ->
            VisitMap(
                state = state,
                modifier = Modifier.fillMaxSize(),
                onMapClick = viewModel::setMapSelectedOrigin,
                onMarkerClick = onCustomerClick
            )
        }
    }

    if (showCustomerPicker) {
        CustomerPickerDialog(
            query = state.customerSearchQuery,
            customers = state.customerSuggestions,
            onQueryChange = viewModel::setCustomerSearchQuery,
            onDismiss = { showCustomerPicker = false },
            onSelect = { customer ->
                viewModel.selectCustomerAsOrigin(customer)
                showCustomerPicker = false
            }
        )
    }
}

/**
 * Lista operacional para retomar um cliente adiado ou uma tentativa marcada
 * como nao visitada. O historico de tentativas permanece associado a cada
 * parada, por isso o vendedor pode fazer um novo check-in sem perder o primeiro.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PendingStopsSheet(
    state: VisitUiState,
    onDismiss: () -> Unit,
    onNavigateToStop: (Long) -> Unit
) {
    val pendingStops = state.pendingStops()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MinumColorTokens.Surface.Elevated,
        contentColor = MinumColorTokens.Text.Primary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MinumSpacing.Lg)
                .padding(bottom = MinumSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(MinumSpacing.Md)
        ) {
            Text(
                text = "Pendencias da rota",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Escolha a parada para a qual deseja navegar. Nenhum atendimento sera apagado.",
                style = MaterialTheme.typography.bodyMedium,
                color = MinumColorTokens.Text.Secondary
            )
            MinumLine()

            LazyColumn(
                modifier = Modifier.heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(MinumSpacing.Sm)
            ) {
                items(pendingStops, key = { stop -> stop.customer.id }) { stop ->
                    val customer = stop.customer
                    val visitStatus = state.stopVisitStatuses[customer.id]
                    val attemptCount = state.attendanceHistoryByCustomer[customer.id].orEmpty().size
                    val isCurrentTarget = customer.id == state.navigationTargetCustomerId
                    val statusLabel = when {
                        isCurrentTarget -> "Destino atual"
                        visitStatus == "not_visited" -> "Retorno necessario"
                        customer.id in state.deferredNavigationCustomerIds -> "Adiado para depois"
                        else -> "Aguardando atendimento"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrentTarget) {
                                MinumColorTokens.Brand.Light.copy(alpha = 0.32f)
                            } else {
                                MinumColorTokens.Surface.Default
                            }
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isCurrentTarget) {
                                MinumColorTokens.Brand.Primary
                            } else {
                                MinumColorTokens.Border.Default
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(MinumSpacing.Md),
                            verticalArrangement = Arrangement.spacedBy(MinumSpacing.Sm)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = customer.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = listOfNotNull(customer.city, customer.state)
                                            .joinToString(" - ")
                                            .ifBlank { customer.address.orEmpty() },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MinumColorTokens.Text.Secondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                AssistChip(
                                    onClick = {},
                                    enabled = false,
                                    label = { Text(statusLabel) }
                                )
                            }
                            if (attemptCount > 0) {
                                Text(
                                    text = "$attemptCount tentativa(s) registrada(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MinumColorTokens.Text.Muted
                                )
                            }
                            OutlinedButton(
                                onClick = { onNavigateToStop(customer.id) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isCurrentTarget,
                                border = BorderStroke(1.dp, MinumColorTokens.Brand.Primary),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MinumColorTokens.Brand.PrimaryDark
                                )
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = null)
                                Spacer(Modifier.width(MinumSpacing.Sm))
                                Text(if (isCurrentTarget) "Navegando para esta parada" else "Navegar ate aqui")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteBottomSheetContent(
    state: VisitUiState,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onApplyCoordinate: () -> Unit,
    onAddressChange: (String) -> Unit,
    onSearchAddress: () -> Unit,
    onUseCurrentLocation: () -> Unit,
    onPickCustomer: () -> Unit,
    onRadiusChange: (Double) -> Unit,
    onSegmentChange: (String?) -> Unit,
    onCityChange: (String?) -> Unit,
    onStateChange: (String?) -> Unit,
    onStatusChange: (String?) -> Unit,
    onOnlyWithPhoneChange: (Boolean) -> Unit,
    onOnlyActiveChange: (Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onOptimize: () -> Unit,
    onSave: () -> Unit,
    onStartNavigation: () -> Unit,
    onCustomerSelected: (Long) -> Unit,
    onCustomerClick: (Long) -> Unit,
    onCallClick: (String?) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 132.dp, max = 680.dp)
            .padding(horizontal = MinumSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(MinumSpacing.Md)
    ) {
        item {
            ResultHeader(
                state = state,
                onSelectAll = onSelectAll,
                onClearSelection = onClearSelection,
                onOptimize = onOptimize,
                onSave = onSave,
                onStartNavigation = onStartNavigation
            )
        }
        item {
            ProspectPanel(
                state = state,
                onLatitudeChange = onLatitudeChange,
                onLongitudeChange = onLongitudeChange,
                onApplyCoordinate = onApplyCoordinate,
                onAddressChange = onAddressChange,
                onSearchAddress = onSearchAddress,
                onUseCurrentLocation = onUseCurrentLocation,
                onPickCustomer = onPickCustomer
            )
        }
        item {
            FilterPanel(
                state = state,
                onRadiusChange = onRadiusChange,
                onSegmentChange = onSegmentChange,
                onCityChange = onCityChange,
                onStateChange = onStateChange,
                onStatusChange = onStatusChange,
                onOnlyWithPhoneChange = onOnlyWithPhoneChange,
                onOnlyActiveChange = onOnlyActiveChange
            )
        }
        if (state.routeInstructions.isNotEmpty()) {
            item {
                RouteInstructionPanel(state = state)
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MinumSpacing.Sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(MinumSpacing.Xs)
                ) {
                    Text(
                        text = "Clientes para a rota",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Toque em um cliente para consultar os dados ou marque para incluir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MinumColorTokens.Text.Secondary
                    )
                }
                Text(
                    text = "${state.selectedCustomerIds.size}/${state.nearbyCustomers.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MinumColorTokens.Brand.Primary
                )
            }
        }
        if (state.nearbyCustomers.isEmpty()) {
            item {
                EmptyState(
                    title = if (state.origin == null) "Defina o prospecto" else "Nenhum cliente no raio",
                    message = if (state.origin == null) {
                        "Use coordenadas, endereco, mapa, localizacao atual ou cliente existente."
                    } else {
                        "Aumente o raio ou ajuste os filtros."
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
        } else {
            items(state.nearbyCustomers, key = { it.customer.id }) { item ->
                NearbyCustomerCard(
                    item = item,
                    isSelected = item.customer.id in state.selectedCustomerIds,
                    onSelectedChange = { onCustomerSelected(item.customer.id) },
                    onDetailsClick = { onCustomerClick(item.customer.id) },
                    onCallClick = { onCallClick(item.customer.phone) },
                    onNavigateClick = {},
                    showNavigateButton = false
                )
            }
        }
    }
}

@Composable
private fun RouteInstructionPanel(state: VisitUiState) {
    PlannerSection(
        icon = Icons.Default.Route,
        title = "Trajeto calculado",
        subtitle = "Confira os primeiros movimentos antes de iniciar."
    ) {
        state.routeInstructions.take(4).forEachIndexed { index, instruction ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Sm),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MinumColorTokens.Brand.Primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(MinumSpacing.Xs))
                        .background(MinumColorTokens.Surface.Subtle)
                        .padding(horizontal = MinumSpacing.Sm, vertical = MinumSpacing.Xs)
                )
                Text(
                    text = instruction.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MinumColorTokens.Text.Secondary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = MinumSpacing.Xs)
                )
            }
        }
    }
}

@Composable
private fun ProspectPanel(
    state: VisitUiState,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onApplyCoordinate: () -> Unit,
    onAddressChange: (String) -> Unit,
    onSearchAddress: () -> Unit,
    onUseCurrentLocation: () -> Unit,
    onPickCustomer: () -> Unit
) {
    PlannerSection(
        icon = Icons.Default.LocationOn,
        title = "Ponto de partida",
        subtitle = "Defina onde a rota deve comecar."
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MinumRadii.Medium))
                .background(MinumColorTokens.Surface.Subtle)
                .padding(MinumSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Sm)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MinumColorTokens.Brand.Primary
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Origem selecionada",
                    style = MaterialTheme.typography.labelMedium,
                    color = MinumColorTokens.Text.Secondary
                )
                Text(
                    text = state.originLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Text(
            text = "Buscar um endereco",
            style = MaterialTheme.typography.labelLarge,
            color = MinumColorTokens.Text.Primary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.addressQuery,
                onValueChange = onAddressChange,
                label = { Text("Endereco ou cidade") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onSearchAddress,
                enabled = !state.isGeocoding
            ) {
                if (state.isGeocoding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MinumColorTokens.Brand.Primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Buscar endereco",
                        tint = MinumColorTokens.Brand.Primary
                    )
                }
            }
        }

        Text(
            text = "Ou use coordenadas",
            style = MaterialTheme.typography.labelLarge,
            color = MinumColorTokens.Text.Primary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Sm)
        ) {
            OutlinedTextField(
                value = state.manualLatitude,
                onValueChange = onLatitudeChange,
                label = { Text("Latitude") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.manualLongitude,
                onValueChange = onLongitudeChange,
                label = { Text("Longitude") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedButton(
            onClick = onApplyCoordinate,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MinumColorTokens.Border.Strong),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MinumColorTokens.Brand.PrimaryDark)
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null)
            Spacer(Modifier.width(MinumSpacing.Sm))
            Text("Usar coordenadas")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Sm)
        ) {
            OutlinedButton(
                onClick = onUseCurrentLocation,
                enabled = !state.isLocating,
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, MinumColorTokens.Border.Strong),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MinumColorTokens.Brand.PrimaryDark)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null)
                Spacer(Modifier.width(MinumSpacing.Xs))
                Text(if (state.isLocating) "Localizando" else "Minha posicao")
            }
            OutlinedButton(
                onClick = onPickCustomer,
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, MinumColorTokens.Border.Strong),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MinumColorTokens.Brand.PrimaryDark)
            ) {
                Icon(Icons.Default.Business, contentDescription = null)
                Spacer(Modifier.width(MinumSpacing.Xs))
                Text("Cliente base")
            }
        }
    }
}

@Composable
private fun FilterPanel(
    state: VisitUiState,
    onRadiusChange: (Double) -> Unit,
    onSegmentChange: (String?) -> Unit,
    onCityChange: (String?) -> Unit,
    onStateChange: (String?) -> Unit,
    onStatusChange: (String?) -> Unit,
    onOnlyWithPhoneChange: (Boolean) -> Unit,
    onOnlyActiveChange: (Boolean) -> Unit
) {
    PlannerSection(
        icon = Icons.Default.FilterAlt,
        title = "Encontrar clientes",
        subtitle = "Ajuste o alcance e os filtros da visita."
    ) {
        Text(
            text = "Raio de busca",
            style = MaterialTheme.typography.labelLarge,
            color = MinumColorTokens.Text.Primary
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Sm)
        ) {
            listOf(1.0, 2.0, 5.0, 10.0, 20.0).forEach { radius ->
                FilterChip(
                    selected = state.radiusKm == radius,
                    onClick = { onRadiusChange(radius) },
                    label = { Text("${radius.toInt()} km") }
                )
            }
        }

        Text(
            text = "Refinar busca",
            style = MaterialTheme.typography.labelLarge,
            color = MinumColorTokens.Text.Primary
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Sm)
        ) {
            FilterDropdown("Segmento", state.segment, state.filterOptions.segments, onSegmentChange)
            FilterDropdown("Cidade", state.city, state.filterOptions.cities, onCityChange)
            FilterDropdown("Estado", state.stateUf, state.filterOptions.states, onStateChange)
            FilterDropdown("Status", state.status, state.filterOptions.statuses, onStatusChange)
        }

        HorizontalDivider(color = MinumColorTokens.Border.Default)
        PlannerSwitchRow(
            label = "Somente clientes com telefone",
            checked = state.onlyWithPhone,
            onCheckedChange = onOnlyWithPhoneChange
        )
        PlannerSwitchRow(
            label = "Somente clientes ativos",
            checked = state.onlyActive,
            onCheckedChange = onOnlyActiveChange
        )
    }
}

@Composable
private fun FilterDropdown(
    label: String,
    selected: String?,
    options: List<String>,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            border = BorderStroke(1.dp, MinumColorTokens.Border.Strong),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MinumColorTokens.Text.Primary)
        ) {
            Text(selected ?: label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Todos") },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PlannerSection(
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

@Composable
private fun PlannerSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MinumColorTokens.Text.Primary
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MinumColorTokens.Text.Inverse,
                checkedTrackColor = MinumColorTokens.Brand.Primary,
                uncheckedThumbColor = MinumColorTokens.Text.Muted,
                uncheckedTrackColor = MinumColorTokens.Surface.Default,
                uncheckedBorderColor = MinumColorTokens.Border.Strong
            )
        )
    }
}

@Composable
private fun VisitMap(
    state: VisitUiState,
    modifier: Modifier,
    onMapClick: (Coordinate) -> Unit,
    onMarkerClick: (Long) -> Unit
) {
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(-46.6333, -23.5505))
            zoom(11.0)
        }
    }

    LaunchedEffect(state.origin) {
        val origin = state.origin
        if (origin != null) {
            mapViewportState.easeTo(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(origin.longitude, origin.latitude))
                    .zoom(13.5)
                    .build()
            )
        }
    }

    val originPoint = state.origin?.let { Point.fromLngLat(it.longitude, it.latitude) }
    val currentLocationPoint = state.currentLocation?.let { Point.fromLngLat(it.longitude, it.latitude) }
    val roadLinePoints = state.roadRoutePoints.map {
        Point.fromLngLat(it.longitude, it.latitude)
    }

    Box(modifier = modifier) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            onMapClickListener = { point ->
                onMapClick(Coordinate(point.latitude(), point.longitude()))
                true
            },
            style = {
                if (state.mapMode == "SATELLITE") {
                    MapboxStandardSatelliteStyle()
                } else {
                    MapboxStandardStyle()
                }
            }
        ) {
            if (originPoint != null) {
                // Marcador do prospecto/origem escolhido pelo vendedor.
                CircleAnnotation(point = originPoint) {
                    circleColor = Color(0xFFB9382F)
                    circleRadius = 8.0
                    circleStrokeColor = Color.White
                    circleStrokeWidth = 2.5
                }
            }

            if (currentLocationPoint != null) {
                // Posicao ao vivo do vendedor, atualizada pelo GPS do aparelho.
                CircleAnnotation(point = currentLocationPoint) {
                    circleColor = Color(0xFF5889FB)
                    circleRadius = 9.0
                    circleStrokeColor = Color.White
                    circleStrokeWidth = 3.0
                }
            }

            state.nearbyCustomers.forEach { item ->
                val selected = item.customer.id in state.selectedCustomerIds
                val navCoord = item.customer.navigationCoordinate
                val customerPoint = Point.fromLngLat(navCoord.longitude, navCoord.latitude)

                // Clientes usam os acentos Minum: azul selecionado e verde para proximidade.
                CircleAnnotation(point = customerPoint) {
                    interactionsState.onClicked {
                        onMarkerClick(item.customer.id)
                        true
                    }
                    circleColor = if (selected) Color(0xFF5889FB) else Color(0xFF009279)
                    circleRadius = if (selected) 7.0 else 6.0
                    circleStrokeColor = Color.White
                    circleStrokeWidth = 2.0
                }
            }

            if (roadLinePoints.size > 1) {
                // Linha real calculada pelo Mapbox Directions, seguindo ruas e restricoes de direcao.
                PolylineAnnotation(points = roadLinePoints) {
                    lineColor = Color(0xFF009279)
                    lineWidth = 5.0
                }
            }
        }
    }
}

/** Solicita um motivo antes de encerrar uma rota que ainda possui pendencias. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun IncompleteRouteSheet(
    completedStops: Int,
    totalStops: Int,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    val isReasonValid = reason.trim().isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MinumColorTokens.Surface.Elevated,
        contentColor = MinumColorTokens.Text.Primary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Encerrar rota nao realizada",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            MinumLine()
            Text(
                text = "$completedStops de $totalStops clientes receberam um feedback.",
                style = MaterialTheme.typography.titleMedium,
                color = MinumColorTokens.Text.Primary
            )
            Text(
                text = "Para encerrar agora, informe por que a rota nao foi realizada por completo. O motivo sera salvo no historico.",
                style = MaterialTheme.typography.bodyMedium,
                color = MinumColorTokens.Text.Secondary
            )
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Motivo da nao realizacao") },
                placeholder = { Text("Ex.: faltou tempo para concluir as visitas") },
                isError = reason.isNotBlank() && !isReasonValid,
                supportingText = { Text("Esse motivo sera visivel no historico da rota.") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                enabled = !isSaving
            )
            Button(
                onClick = { onConfirm(reason) },
                enabled = isReasonValid && !isSaving,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onError,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Salvar como nao realizada")
            }
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continuar navegando")
            }
        }
    }
}

/**
 * Estado exclusivo de entrada para uma rota enviada pelo backoffice. Assim o
 * vendedor recebe uma explicacao objetiva enquanto o GPS prepara a navegacao.
 */
@Composable
private fun SharedRouteLaunchingScreen(
    routeName: String?,
    hasLoadedRoute: Boolean,
    hasLocation: Boolean,
    isLoading: Boolean,
    message: String?,
    onBack: () -> Unit,
    onRequestLocation: () -> Unit,
    onRetry: () -> Unit,
    onStartNow: () -> Unit
) {
    val title = when {
        !hasLoadedRoute && isLoading -> "Carregando rota compartilhada"
        !hasLoadedRoute -> "Rota indisponivel"
        !hasLocation -> "Preparando sua navegacao"
        else -> "Rota pronta para iniciar"
    }
    val description = when {
        !hasLoadedRoute && isLoading -> "Estamos buscando as paradas definidas para voce."
        !hasLoadedRoute -> message ?: "Nao foi possivel abrir esta rota agora. Tente novamente em instantes."
        !hasLocation -> "Permita a localizacao para posicionar voce no mapa e iniciar a navegacao pela rota correta."
        else -> "Seu GPS foi localizado. A navegacao desta rota sera aberta dentro do aplicativo."
    }

    AppScaffold(title = "Rota compartilhada", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = MinumSpacing.Lg, vertical = MinumSpacing.Xl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MinumColorTokens.Brand.PrimaryDark),
                border = BorderStroke(1.dp, MinumColorTokens.Brand.Primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(MinumSpacing.Xl),
                    verticalArrangement = Arrangement.spacedBy(MinumSpacing.Lg),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MinumColorTokens.Brand.Energy)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            tint = MinumColorTokens.Brand.Energy,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text(
                        text = routeName ?: "Sua rota de campo",
                        style = MaterialTheme.typography.titleMedium,
                        color = MinumColorTokens.Brand.Light,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MinumColorTokens.Text.Inverse,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MinumColorTokens.Brand.Light
                    )
                    MinumLine()
                    when {
                        hasLoadedRoute && hasLocation -> {
                            Button(
                                onClick = onStartNow,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MinumColorTokens.Brand.Energy,
                                    contentColor = MinumColorTokens.Brand.PrimaryDark
                                )
                            ) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null)
                                Spacer(Modifier.width(MinumSpacing.Sm))
                                Text("Iniciar navegacao")
                            }
                        }

                        hasLoadedRoute -> {
                            Button(
                                onClick = onRequestLocation,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MinumColorTokens.Brand.Energy,
                                    contentColor = MinumColorTokens.Brand.PrimaryDark
                                )
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = null)
                                Spacer(Modifier.width(MinumSpacing.Sm))
                                Text("Ativar localizacao")
                            }
                        }

                        else -> {
                            Button(
                                onClick = onRetry,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MinumColorTokens.Brand.Energy,
                                    contentColor = MinumColorTokens.Brand.PrimaryDark
                                )
                            ) {
                                Text("Tentar novamente")
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MinumColorTokens.Brand.Light)
                    ) {
                        Text("Voltar para Meu dia")
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun StopVisitDecisionSheet(
    customer: Customer,
    onDismiss: () -> Unit,
    onVisited: () -> Unit,
    onNotVisited: () -> Unit,
    onCall: () -> Unit
) {
    val companyName = customer.clientName
        ?.takeIf { it.isNotBlank() }
        ?: customer.cnpjCpf?.takeIf { it.isNotBlank() }
        ?: "Empresa nao informada"

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Registrar visita",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(customer.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Empresa: $companyName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!customer.phone.isNullOrBlank()) {
                OutlinedButton(onClick = onCall, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Ligar para cliente")
                }
            }
            Text(
                text = "Este cliente foi visitado?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Button(onClick = onVisited, modifier = Modifier.fillMaxWidth()) {
                Text("Sim, visitei")
            }
            OutlinedButton(onClick = onNotVisited, modifier = Modifier.fillMaxWidth()) {
                Text("Nao foi possivel realizar a visita")
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun StopFeedbackSheet(
    customer: Customer,
    wasVisited: Boolean,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    onSave: (String, String?, String?, String, String) -> Unit
) {
    var feedback by remember(customer.id, wasVisited) { mutableStateOf("") }
    var notVisitedReason by remember(customer.id, wasVisited) { mutableStateOf<String?>(null) }
    var commercialOutcome by remember(customer.id, wasVisited) { mutableStateOf<String?>(null) }
    var nextAction by remember(customer.id, wasVisited) { mutableStateOf("") }
    var nextActionDueDate by remember(customer.id, wasVisited) { mutableStateOf("") }
    val feedbackLength = feedback.trim().length
    val canSave = feedbackLength >= MINIMUM_STOP_FEEDBACK_LENGTH && !isSaving
    val quickOptions = if (wasVisited) COMMERCIAL_OUTCOME_OPTIONS else NOT_VISITED_REASON_OPTIONS

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (wasVisited) "Como foi a visita?" else "Por que a visita nao foi realizada?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = customer.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "O feedback, sua localizacao atual e o horario serao enviados ao historico.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = feedback,
                onValueChange = { feedback = it },
                label = { Text("Feedback da visita") },
                placeholder = {
                    Text(
                        if (wasVisited) "Descreva o resultado da conversa..." else "Descreva o motivo..."
                    )
                },
                supportingText = {
                    Text("$feedbackLength/$MINIMUM_STOP_FEEDBACK_LENGTH caracteres minimos")
                },
                isError = feedback.isNotBlank() && feedbackLength < MINIMUM_STOP_FEEDBACK_LENGTH,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )
            Text(
                text = if (wasVisited) "Resultado comercial (opcional)" else "Motivo padronizado (opcional)",
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickOptions.forEach { option ->
                    val selected = if (wasVisited) commercialOutcome == option else notVisitedReason == option
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (wasVisited) commercialOutcome = option else notVisitedReason = option
                        },
                        label = { Text(option) }
                    )
                }
            }
            OutlinedTextField(
                value = nextAction,
                onValueChange = { nextAction = it },
                label = { Text("Proximo passo (opcional)") },
                placeholder = { Text("Ex.: Enviar proposta comercial") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = nextActionDueDate,
                onValueChange = { nextActionDueDate = it },
                label = { Text("Data de retorno (opcional)") },
                placeholder = { Text("AAAA-MM-DD") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(
                onClick = {
                    onSave(
                        feedback,
                        notVisitedReason,
                        commercialOutcome,
                        nextAction,
                        nextActionDueDate
                    )
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Salvar feedback")
                }
            }
            TextButton(onClick = onBack, enabled = !isSaving, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Voltar")
            }
        }
    }
}

private val COMMERCIAL_OUTCOME_OPTIONS = listOf(
    "Interessado",
    "Proposta solicitada",
    "Em negociacao",
    "Venda realizada",
    "Sem interesse"
)

private val NOT_VISITED_REASON_OPTIONS = listOf(
    "Sem responsavel no local",
    "Cliente fechado",
    "Endereco incorreto",
    "Reagendar visita",
    "Sem interesse"
)

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@SuppressLint("MissingPermission")
@Composable
private fun OfficialMapboxNavigationExperience(
    state: VisitUiState,
    modifier: Modifier,
    onStopNavigation: () -> Unit,
    onNextStop: () -> Unit,
    onShowPendingStops: () -> Unit,
    onStopMarkerClick: (Customer) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapboxNavigationInstance by remember { mutableStateOf<MapboxNavigation?>(null) }
    var navigationController by remember { mutableStateOf<OfficialNavigationController?>(null) }
    var requestedRouteKey by remember { mutableStateOf<String?>(null) }
    val navigationPresentationState = remember { mutableStateOf(NavigationPresentationState()) }
    val uiHandler = remember { Handler(Looper.getMainLooper()) }
    val navigationPresentation = navigationPresentationState.value
    val navigationTarget = state.optimizedStops
        .firstOrNull { it.customer.id == state.navigationTargetCustomerId }
    val pendingStopCount = state.pendingStops().size

    val navigationObserver = remember {
        object : MapboxNavigationObserver {
            override fun onAttached(mapboxNavigation: MapboxNavigation) {
                mapboxNavigationInstance = mapboxNavigation
                navigationController?.attach(mapboxNavigation)
            }

            override fun onDetached(mapboxNavigation: MapboxNavigation) {
                navigationController?.detach(mapboxNavigation)
                if (mapboxNavigationInstance == mapboxNavigation) {
                    mapboxNavigationInstance = null
                }
            }
        }
    }

    LaunchedEffect(mapboxNavigationInstance, navigationController) {
        val navigation = mapboxNavigationInstance
        val controller = navigationController
        if (navigation != null && controller != null) {
            controller.attach(navigation)
        }
    }

    DisposableEffect(lifecycleOwner, navigationObserver) {
        MapboxNavigationApp.attach(lifecycleOwner)
        MapboxNavigationApp.registerObserver(navigationObserver)
        onDispose {
            navigationController?.detach(mapboxNavigationInstance)
            mapboxNavigationInstance?.setNavigationRoutes(emptyList())
            mapboxNavigationInstance?.stopTripSession()
            MapboxNavigationApp.unregisterObserver(navigationObserver)
            MapboxNavigationApp.detach(lifecycleOwner)
        }
    }

    LaunchedEffect(mapboxNavigationInstance, state.navigationWaypoints) {
        val navigation = mapboxNavigationInstance ?: return@LaunchedEffect
        val waypoints = state.navigationWaypoints
        if (waypoints.size < 2) {
            requestedRouteKey = null
            navigation.setNavigationRoutes(emptyList())
            navigationController?.clearRoute()
            return@LaunchedEffect
        }

        val routeKey = waypoints.joinToString("|") { "${it.latitude},${it.longitude}" }
        if (requestedRouteKey == routeKey) return@LaunchedEffect
        requestedRouteKey = routeKey
        navigationPresentationState.value = NavigationPresentationState(
            instruction = "Atualizando percurso",
            maneuverDistanceLabel = "Calculando a melhor rota pelas ruas"
        )

        val points = waypoints.map { Point.fromLngLat(it.longitude, it.latitude) }
        navigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(context)
                .coordinatesList(points)
                .waypointNamesList(
                    listOf(
                        "Sua posicao",
                        navigationTarget?.customer?.name ?: "Proxima parada"
                    )
                )
                .build(),
            object : NavigationRouterCallback {
                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: String
                ) {
                    navigation.setNavigationRoutes(routes)
                    navigation.startTripSession(withForegroundService = false)
                    navigationController?.followRoute()
                }

                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    requestedRouteKey = null
                    uiHandler.post {
                        navigationPresentationState.value = NavigationPresentationState(
                            instruction = "Nao foi possivel atualizar o trajeto",
                            maneuverDistanceLabel = "Confira a conexao e tente novamente"
                        )
                    }
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {
                    requestedRouteKey = null
                }
            }
        )
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                val mapView = MapView(viewContext)
                val locationProvider = NavigationLocationProvider()

                mapView.location.apply {
                    setLocationProvider(locationProvider)
                    locationPuck = LocationPuck2D(
                        bearingImage = ImageHolder.from(com.mapbox.navigation.ui.maps.R.drawable.mapbox_navigation_puck_icon)
                    )
                    enabled = true
                    puckBearingEnabled = true
                }
                state.navigationWaypoints.firstOrNull()?.let { start ->
                    mapView.getMapboxMap().setCamera(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(start.longitude, start.latitude))
                            .zoom(15.5)
                            .pitch(45.0)
                            .build()
                    )
                }

                navigationController = OfficialNavigationController(
                    context = viewContext,
                    mapView = mapView,
                    locationProvider = locationProvider,
                    onNavigationPresentationChange = { presentation ->
                        navigationPresentationState.value = presentation
                    }
                )
                val controller = navigationController
                mapView.getMapboxMap().loadStyle(Style.STANDARD) {
                    controller?.renderStopMarkers(
                        stops = state.optimizedStops,
                        stopVisitStatuses = state.stopVisitStatuses,
                        navigationTargetCustomerId = state.navigationTargetCustomerId,
                        deferredCustomerIds = state.deferredNavigationCustomerIds,
                        onStopMarkerClick = onStopMarkerClick
                    )
                }

                mapView
            },
            update = {
                navigationController?.renderStopMarkers(
                    stops = state.optimizedStops,
                    stopVisitStatuses = state.stopVisitStatuses,
                    navigationTargetCustomerId = state.navigationTargetCustomerId,
                    deferredCustomerIds = state.deferredNavigationCustomerIds,
                    onStopMarkerClick = onStopMarkerClick
                )
            }
        )

        NavigationGuidanceOverlay(
            presentation = navigationPresentation,
            destinationName = navigationTarget?.customer?.name,
            pendingStopCount = pendingStopCount,
            onNextStop = onNextStop,
            onShowPendingStops = onShowPendingStops,
            onStopNavigation = onStopNavigation,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private class OfficialNavigationController(
    context: Context,
    private val mapView: MapView,
    private val locationProvider: NavigationLocationProvider,
    private val onNavigationPresentationChange: (NavigationPresentationState) -> Unit
) {
    private val mapboxMap = mapView.getMapboxMap()
    private val viewportDataSource = MapboxNavigationViewportDataSource(mapboxMap)
    private val navigationCamera = NavigationCamera(
        mapboxMap = mapboxMap,
        cameraPlugin = mapView.camera,
        viewportDataSource = viewportDataSource
    )
    private val routeLineApi = MapboxRouteLineApi(MapboxRouteLineApiOptions.Builder().build())
    private val routeLineView = MapboxRouteLineView(MapboxRouteLineViewOptions.Builder(context).build())
    private val routeArrowApi = MapboxRouteArrowApi()
    private val routeArrowView = MapboxRouteArrowView(RouteArrowOptions.Builder(context).build())
    private val mainHandler = Handler(Looper.getMainLooper())
    private var attachedNavigation: MapboxNavigation? = null
    private var stopAnnotationManager: CircleAnnotationManager? = null
    private val customerByAnnotationId = mutableMapOf<String, Customer>()
    private var onStopMarkerClick: (Customer) -> Unit = {}

    private val locationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: com.mapbox.common.location.Location) = Unit

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val enhancedLocation = locationMatcherResult.enhancedLocation
            locationProvider.changePosition(enhancedLocation, locationMatcherResult.keyPoints)
            viewportDataSource.onLocationChanged(enhancedLocation)
            viewportDataSource.evaluate()
            navigationCamera.requestNavigationCameraToFollowing()
        }
    }

    private val routesObserver = RoutesObserver { result ->
        val routes = result.navigationRoutes
        if (routes.isEmpty()) {
            clearRoute()
        } else {
            routeLineApi.setNavigationRoutes(routes) { routeDrawData ->
                mapboxMap.getStyle { style ->
                    routeLineView.renderRouteDrawData(style, routeDrawData)
                }
            }

            routes.firstOrNull()?.let { route ->
                publishNavigationPresentation(
                    NavigationPresentationState(
                        instruction = "Siga pela rota planejada",
                        maneuverDistanceLabel = "Aguardando sua posicao",
                        remainingDistanceMeters = route.directionsRoute.distance(),
                        remainingDurationSeconds = route.directionsRoute.duration(),
                        isRouteReady = true
                    )
                )
                viewportDataSource.onRouteChanged(route)
                viewportDataSource.evaluate()
                followRoute()
            }
        }
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()
        navigationCamera.requestNavigationCameraToFollowing()

        routeLineApi.updateWithRouteProgress(routeProgress) { update ->
            mapboxMap.getStyle { style ->
                routeLineView.renderRouteLineUpdate(style, update)
            }
        }

        mapboxMap.getStyle { style ->
            routeArrowView.renderManeuverUpdate(
                style,
                routeArrowApi.addUpcomingManeuverArrow(routeProgress)
            )
        }

        val stepProgress = routeProgress.currentLegProgress?.currentStepProgress
        val instruction = stepProgress?.step?.maneuver()?.instruction()
            ?.takeIf { it.isNotBlank() }
            ?: "Siga pela rota"
        val nextManeuverDistance = stepProgress?.distanceRemaining?.toDouble()
            ?: routeProgress.distanceRemaining.toDouble()
        val remainingDistanceMeters = routeProgress.distanceRemaining.toDouble()
        val remainingDurationSeconds = routeProgress.durationRemaining
        publishNavigationPresentation(
            NavigationPresentationState(
                instruction = instruction,
                maneuverDistanceLabel = "Em ${formatDistance(nextManeuverDistance)}",
                remainingDistanceMeters = remainingDistanceMeters,
                remainingDurationSeconds = remainingDurationSeconds,
                isRouteReady = true
            )
        )
    }

    fun attach(mapboxNavigation: MapboxNavigation) {
        if (attachedNavigation == mapboxNavigation) return
        detach(attachedNavigation)
        attachedNavigation = mapboxNavigation
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
    }

    fun detach(mapboxNavigation: MapboxNavigation?) {
        val navigation = mapboxNavigation ?: return
        if (attachedNavigation != navigation) return
        navigation.unregisterLocationObserver(locationObserver)
        navigation.unregisterRoutesObserver(routesObserver)
        navigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxMap.getStyle { style ->
            routeArrowView.render(style, routeArrowApi.clearArrows())
            routeLineApi.clearRouteLine { clearValue ->
                routeLineView.renderClearRouteLineValue(style, clearValue)
            }
        }
        routeLineApi.cancel()
        attachedNavigation = null
    }

    fun followRoute() {
        viewportDataSource.evaluate()
        navigationCamera.requestNavigationCameraToOverview()
        navigationCamera.requestNavigationCameraToFollowing()
    }

    fun clearRoute() {
        mapboxMap.getStyle { style ->
            routeArrowView.render(style, routeArrowApi.clearArrows())
            routeLineApi.clearRouteLine { clearValue ->
                routeLineView.renderClearRouteLineValue(style, clearValue)
            }
        }
        publishNavigationPresentation(
            NavigationPresentationState(
                instruction = "Nenhuma parada ativa",
                maneuverDistanceLabel = "Escolha uma pendencia para continuar"
            )
        )
    }

    private fun publishNavigationPresentation(presentation: NavigationPresentationState) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onNavigationPresentationChange(presentation)
        } else {
            mainHandler.post { onNavigationPresentationChange(presentation) }
        }
    }

    /** Desenha as paradas da rota e conecta cada marcador a gaveta de feedback. */
    fun renderStopMarkers(
        stops: List<NearbyCustomer>,
        stopVisitStatuses: Map<Long, String>,
        navigationTargetCustomerId: Long?,
        deferredCustomerIds: Set<Long>,
        onStopMarkerClick: (Customer) -> Unit
    ) {
        this.onStopMarkerClick = onStopMarkerClick
        mapboxMap.getStyle {
            val manager = stopAnnotationManager ?: mapView.annotations
                .createCircleAnnotationManager()
                .also { annotationManager ->
                    annotationManager.addClickListener { annotation ->
                        customerByAnnotationId[annotation.id]?.let(this.onStopMarkerClick)
                        true
                    }
                    stopAnnotationManager = annotationManager
                }

            manager.deleteAll()
            customerByAnnotationId.clear()
            stops.forEach { stop ->
                val customer = stop.customer
                val markerColor = when {
                    customer.id == navigationTargetCustomerId -> 0xFFFDF083.toInt()
                    stopVisitStatuses[customer.id] == "visited" -> 0xFF009279.toInt()
                    stopVisitStatuses[customer.id] == "not_visited" -> 0xFFB9382F.toInt()
                    customer.id in deferredCustomerIds -> 0xFF5889FB.toInt()
                    else -> 0xFF00D2AE.toInt()
                }
                val markerStrokeColor = if (customer.id == navigationTargetCustomerId) {
                    0xFF00463A.toInt()
                } else {
                    AndroidColor.WHITE
                }
                val annotation = manager.create(
                    CircleAnnotationOptions()
                        .withPoint(Point.fromLngLat(customer.longitude, customer.latitude))
                        .withCircleColor(markerColor)
                        .withCircleRadius(10.0)
                        .withCircleStrokeColor(markerStrokeColor)
                        .withCircleStrokeWidth(3.0)
                )
                customerByAnnotationId[annotation.id] = customer
            }
        }
    }
}

/** Dados de percurso recebidos dos observadores oficiais do Mapbox Navigation. */
private data class NavigationPresentationState(
    val instruction: String = "Preparando navegacao",
    val maneuverDistanceLabel: String = "Aguardando GPS",
    val remainingDistanceMeters: Double? = null,
    val remainingDurationSeconds: Double? = null,
    val isRouteReady: Boolean = false
)

/**
 * Sobreposicao Compose que deixa a navegacao legivel com uma mao, sem cobrir
 * o mapa com controles nativos. A rota, a seta e os recalculos continuam sob
 * responsabilidade integral do SDK oficial do Mapbox.
 */
@Composable
private fun NavigationGuidanceOverlay(
    presentation: NavigationPresentationState,
    destinationName: String?,
    pendingStopCount: Int,
    onNextStop: () -> Unit,
    onShowPendingStops: () -> Unit,
    onStopNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasActiveDestination = !destinationName.isNullOrBlank()
    val hasAnotherStop = hasActiveDestination && pendingStopCount > 1
    val distanceLabel = presentation.remainingDistanceMeters?.let(::formatDistance)
        ?: if (presentation.isRouteReady) "Calculando" else "--"
    val durationLabel = presentation.remainingDurationSeconds?.let(::formatDuration)
        ?: if (presentation.isRouteReady) "Calculando" else "--"
    val etaLabel = presentation.remainingDurationSeconds?.let(::formatEta) ?: "--:--"

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = MinumSpacing.Lg, vertical = MinumSpacing.Sm)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(MinumRadii.Medium),
                color = MinumColorTokens.Brand.PrimaryDark,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(MinumSpacing.Md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Md)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(MinumRadii.Small))
                            .background(MinumColorTokens.Brand.Energy.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            tint = MinumColorTokens.Brand.Energy,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(MinumSpacing.Xs)
                    ) {
                        Text(
                            text = "PROXIMA MANOBRA",
                            style = MaterialTheme.typography.labelSmall,
                            color = MinumColorTokens.Brand.Light,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = presentation.instruction,
                            style = MaterialTheme.typography.titleMedium,
                            color = MinumColorTokens.Text.Inverse,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = presentation.maneuverDistanceLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MinumColorTokens.Brand.Light,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = MinumSpacing.Lg, vertical = MinumSpacing.Md)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(MinumRadii.Medium),
                color = MinumColorTokens.Surface.Elevated,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, MinumColorTokens.Border.Default)
            ) {
                Column(
                    modifier = Modifier.padding(MinumSpacing.Lg),
                    verticalArrangement = Arrangement.spacedBy(MinumSpacing.Md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Sm)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(MinumRadii.Small))
                                .background(MinumColorTokens.Surface.Subtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Route,
                                contentDescription = null,
                                tint = MinumColorTokens.Brand.Primary,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PROXIMA PARADA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MinumColorTokens.Text.Muted,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = destinationName ?: "Escolha uma pendencia para continuar",
                                style = MaterialTheme.typography.titleSmall,
                                color = MinumColorTokens.Text.Primary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (pendingStopCount > 0) {
                            Text(
                                text = "$pendingStopCount",
                                style = MaterialTheme.typography.labelMedium,
                                color = MinumColorTokens.Brand.PrimaryDark,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(MinumRadii.Small))
                                    .background(MinumColorTokens.Brand.Light.copy(alpha = 0.45f))
                                    .padding(horizontal = MinumSpacing.Sm, vertical = MinumSpacing.Xs)
                            )
                        }
                    }

                    MinumLine()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NavigationMetric(
                            label = "Restam",
                            value = distanceLabel,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(38.dp)
                                .background(MinumColorTokens.Border.Default)
                        )
                        NavigationMetric(
                            label = "Tempo",
                            value = durationLabel,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(38.dp)
                                .background(MinumColorTokens.Border.Default)
                        )
                        NavigationMetric(
                            label = "Chegada",
                            value = etaLabel,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Sm)
                    ) {
                        OutlinedButton(
                            onClick = onShowPendingStops,
                            enabled = pendingStopCount > 0,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            border = BorderStroke(1.dp, MinumColorTokens.Border.Strong),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MinumColorTokens.Brand.PrimaryDark
                            )
                        ) {
                            Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(MinumSpacing.Xs))
                            Text("Pendencias", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Button(
                            onClick = if (hasAnotherStop) onNextStop else onShowPendingStops,
                            enabled = if (hasAnotherStop) true else pendingStopCount > 0,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MinumColorTokens.Brand.Primary,
                                contentColor = MinumColorTokens.Text.Inverse
                            )
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(MinumSpacing.Xs))
                            Text(
                                text = if (hasAnotherStop) "Proxima" else "Escolher",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    TextButton(
                        onClick = onStopNavigation,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MinumColorTokens.Feedback.Error
                        )
                    ) {
                        Text("Encerrar navegacao")
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MinumColorTokens.Text.Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MinumColorTokens.Text.Primary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ResultHeader(
    state: VisitUiState,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onOptimize: () -> Unit,
    onSave: () -> Unit,
    onStartNavigation: () -> Unit
) {
    val routeDistance = state.roadRouteDistanceMeters
    val routeDuration = state.roadRouteDurationSeconds
    val quotaReached = !state.isRouteQuotaLoading &&
        state.dailyRoutesCreated >= state.dailyRouteLimit

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = MinumSpacing.Xs),
        verticalArrangement = Arrangement.spacedBy(MinumSpacing.Md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MinumSpacing.Xs)
            ) {
                Text(
                    text = "PLANEJAMENTO",
                    style = MaterialTheme.typography.labelMedium,
                    color = MinumColorTokens.Brand.Primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Planejar nova visita",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (state.origin == null) {
                        "Defina a origem e escolha os clientes da rota."
                    } else {
                        "Selecione os clientes e confirme o melhor trajeto."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MinumColorTokens.Text.Secondary
                )
            }
            if (state.isSearching || state.isRouteLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MinumColorTokens.Brand.Primary,
                    strokeWidth = 2.dp
                )
            }
        }
        MinumLine()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Sm)
        ) {
            RouteSummaryMetric(
                value = state.selectedCustomerIds.size.toString(),
                label = "selecionados",
                modifier = Modifier.weight(1f)
            )
            RouteSummaryMetric(
                value = routeDistance?.let(::formatDistance) ?: "--",
                label = "distancia",
                modifier = Modifier.weight(1f)
            )
            RouteSummaryMetric(
                value = routeDuration?.let(::formatDuration) ?: "--",
                label = "tempo estimado",
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = when {
                state.isRouteQuotaLoading -> "Verificando seu limite diario de rotas..."
                quotaReached -> "Limite diario de ${state.dailyRouteLimit} rotas atingido."
                else -> "${state.dailyRoutesCreated} de ${state.dailyRouteLimit} rotas criadas hoje"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (quotaReached) {
                MinumColorTokens.Feedback.Error
            } else {
                MinumColorTokens.Text.Secondary
            }
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MinumSpacing.Sm)
        ) {
            AssistChip(
                onClick = onSelectAll,
                leadingIcon = { Icon(Icons.Default.SelectAll, contentDescription = null) },
                label = { Text("Selecionar todos") }
            )
            AssistChip(onClick = onClearSelection, label = { Text("Limpar selecao") })
            AssistChip(
                onClick = onOptimize,
                enabled = state.selectedCustomerIds.size > 1,
                leadingIcon = { Icon(Icons.Default.Route, contentDescription = null) },
                label = { Text("Otimizar ordem") }
            )
        }

        Button(
            onClick = onSave,
            enabled = state.selectedCustomerIds.isNotEmpty() &&
                !state.isSaving &&
                !state.isRouteQuotaLoading &&
                !quotaReached,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MinumColorTokens.Brand.Primary,
                contentColor = MinumColorTokens.Text.Inverse
            )
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MinumColorTokens.Text.Inverse,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(MinumSpacing.Sm))
            }
            Text(
                when {
                    state.isSaving -> "Salvando rota"
                    state.isRouteQuotaLoading -> "Verificando limite"
                    quotaReached -> "Limite diario atingido"
                    else -> "Salvar rota"
                }
            )
        }

        OutlinedButton(
            onClick = onStartNavigation,
            enabled = state.roadRoutePoints.size > 1 && !state.isRouteLoading,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MinumColorTokens.Border.Strong),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MinumColorTokens.Brand.PrimaryDark)
        ) {
            Icon(Icons.Default.Navigation, contentDescription = null)
            Spacer(Modifier.width(MinumSpacing.Sm))
            Text("Iniciar navegacao")
        }
    }
}

@Composable
private fun RouteSummaryMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(MinumRadii.Medium))
            .background(MinumColorTokens.Surface.Subtle)
            .padding(MinumSpacing.Md),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MinumColorTokens.Brand.PrimaryDark,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MinumColorTokens.Text.Secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatDistance(meters: Double): String {
    return if (meters >= 1000.0) {
        "${"%.1f".format(meters / 1000.0)} km"
    } else {
        "${meters.toInt()} m"
    }
}

private fun formatDuration(seconds: Double): String {
    val minutes = (seconds / 60.0).toInt().coerceAtLeast(1)
    return if (minutes >= 60) {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        if (remainingMinutes == 0) "${hours}h" else "${hours}h ${remainingMinutes}min"
    } else {
        "${minutes} min"
    }
}

private fun formatEta(seconds: Double): String {
    val finishAt = Date(System.currentTimeMillis() + (seconds * 1000).toLong())
    return SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(finishAt)
}

private const val MINIMUM_STOP_FEEDBACK_LENGTH = 20

@Composable
private fun CustomerPickerDialog(
    query: String,
    customers: List<Customer>,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelect: (Customer) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecionar cliente principal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text("Buscar cliente") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(customers, key = { it.id }) { customer ->
                        TextButton(
                            onClick = { onSelect(customer) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(customer.name, fontWeight = FontWeight.SemiBold)
                                if (customer.fullAddress.isNotBlank()) {
                                    Text(
                                        customer.fullAddress,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}
