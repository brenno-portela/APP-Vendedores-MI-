package com.xateenergia.vendedoresminum.presentation.screens.visit

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
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
import com.xateenergia.vendedoresminum.domain.model.Customer
import com.xateenergia.vendedoresminum.domain.model.NearbyCustomer
import com.xateenergia.vendedoresminum.presentation.components.AppScaffold
import com.xateenergia.vendedoresminum.presentation.components.EmptyState
import com.xateenergia.vendedoresminum.presentation.components.NearbyCustomerCard
import com.xateenergia.vendedoresminum.presentation.utils.ExternalIntents
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitMapScreen(
    onBack: () -> Unit,
    onCustomerClick: (Long) -> Unit,
    sharedRouteId: String? = null,
    viewModel: VisitPlanningViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showCustomerPicker by remember { mutableStateOf(false) }
    var feedbackCustomer by remember { mutableStateOf<Customer?>(null) }
    var feedbackWasVisited by remember { mutableStateOf<Boolean?>(null) }

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

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(state.savedFeedbackCustomerId) {
        val savedCustomerId = state.savedFeedbackCustomerId ?: return@LaunchedEffect
        if (savedCustomerId == feedbackCustomer?.id) {
            feedbackCustomer = null
            feedbackWasVisited = null
        }
        viewModel.consumeSavedFeedbackCustomer()
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
        sharedRouteId?.let(viewModel::loadSharedRoute)
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
                onStopNavigation = viewModel::stopNavigation,
                onStopMarkerClick = { customer ->
                    feedbackCustomer = customer
                    feedbackWasVisited = null
                }
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            val selectedCustomer = feedbackCustomer
            val wasVisited = feedbackWasVisited
            if (selectedCustomer != null && wasVisited == null) {
                StopVisitDecisionSheet(
                    customer = selectedCustomer,
                    onDismiss = { feedbackCustomer = null },
                    onVisited = {
                        viewModel.recordStopCheckIn(selectedCustomer)
                        feedbackWasVisited = true
                    },
                    onNotVisited = { feedbackWasVisited = false },
                    onCall = { ExternalIntents.dial(context, selectedCustomer.phone) }
                )
            }
            if (selectedCustomer != null && wasVisited != null) {
                StopFeedbackSheet(
                    customer = selectedCustomer,
                    wasVisited = wasVisited,
                    isSaving = state.isSavingStopFeedback,
                    onDismiss = {
                        feedbackCustomer = null
                        feedbackWasVisited = null
                    },
                    onBack = { feedbackWasVisited = null },
                    onSave = { feedback, notVisitedReason, commercialOutcome, nextAction, nextActionDueDate ->
                        viewModel.saveStopFeedback(
                            customer = selectedCustomer,
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
        }
        return
    }

    AppScaffold(title = "Mapa de visita", onBack = onBack) { padding ->
        BottomSheetScaffold(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            sheetPeekHeight = 108.dp,
            sheetDragHandle = { BottomSheetDefaults.DragHandle() },
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
                    onStopNavigation = viewModel::stopNavigation,
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
    onStopNavigation: () -> Unit,
    onCustomerSelected: (Long) -> Unit,
    onCustomerClick: (Long) -> Unit,
    onCallClick: (String?) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp, max = 620.dp)
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ResultHeader(
                state = state,
                onSelectAll = onSelectAll,
                onClearSelection = onClearSelection,
                onOptimize = onOptimize,
                onSave = onSave,
                onStartNavigation = onStartNavigation,
                onStopNavigation = onStopNavigation
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Direcoes da rota",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            state.routeInstructions.take(4).forEachIndexed { index, instruction ->
                Text(
                    text = "${index + 1}. ${instruction.text}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = state.originLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                IconButton(onClick = onApplyCoordinate) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Aplicar coordenadas")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.addressQuery,
                    onValueChange = onAddressChange,
                    label = { Text("Buscar endereco") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onSearchAddress,
                    enabled = !state.isGeocoding
                ) {
                    if (state.isGeocoding) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Search, contentDescription = "Buscar endereco")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onUseCurrentLocation,
                    enabled = !state.isLocating,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null)
                    Text(if (state.isLocating) "Localizando" else "Atual")
                }
                OutlinedButton(
                    onClick = onPickCustomer,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Text("Cliente base")
                }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.FilterAlt, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text("Filtros", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1.0, 2.0, 5.0, 10.0, 20.0).forEach { radius ->
                    FilterChip(
                        selected = state.radiusKm == radius,
                        onClick = { onRadiusChange(radius) },
                        label = { Text("${radius.toInt()} km") }
                    )
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterDropdown("Segmento", state.segment, state.filterOptions.segments, onSegmentChange)
                FilterDropdown("Cidade", state.city, state.filterOptions.cities, onCityChange)
                FilterDropdown("Estado", state.stateUf, state.filterOptions.states, onStateChange)
                FilterDropdown("Status", state.status, state.filterOptions.statuses, onStatusChange)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Somente com telefone", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = state.onlyWithPhone, onCheckedChange = onOnlyWithPhoneChange)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Somente ativos", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = state.onlyActive, onCheckedChange = onOnlyActiveChange)
            }
        }
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
        OutlinedButton(onClick = { expanded = true }) {
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
                val customerPoint = Point.fromLngLat(item.customer.longitude, item.customer.latitude)

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
    onStopMarkerClick: (Customer) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapboxNavigationInstance by remember { mutableStateOf<MapboxNavigation?>(null) }
    var navigationController by remember { mutableStateOf<OfficialNavigationController?>(null) }
    var requestedRouteKey by remember { mutableStateOf<String?>(null) }

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
        if (waypoints.size < 2) return@LaunchedEffect

        val routeKey = waypoints.joinToString("|") { "${it.latitude},${it.longitude}" }
        if (requestedRouteKey == routeKey) return@LaunchedEffect
        requestedRouteKey = routeKey

        val points = waypoints.map { Point.fromLngLat(it.longitude, it.latitude) }
        navigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(context)
                .coordinatesList(points)
                .waypointNamesList(listOf("Inicio") + state.optimizedStops.map { it.customer.name })
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
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {
                    requestedRouteKey = null
                }
            }
        )
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            val root = FrameLayout(viewContext)
            val mapView = MapView(viewContext)
            val locationProvider = NavigationLocationProvider()
            val navigationHud = NavigationHud(viewContext, onStopNavigation)

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

            root.addView(
                mapView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )

            root.addView(
                navigationHud.topPanel,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP
                ).apply {
                    setMargins(
                        viewContext.dp(12),
                        viewContext.dp(16),
                        viewContext.dp(12),
                        0
                    )
                }
            )

            root.addView(
                navigationHud.bottomPanel,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
                ).apply {
                    setMargins(
                        viewContext.dp(12),
                        0,
                        viewContext.dp(12),
                        viewContext.dp(16)
                    )
                }
            )

            navigationController = OfficialNavigationController(
                context = viewContext,
                mapView = mapView,
                locationProvider = locationProvider,
                navigationHud = navigationHud
            )
            val controller = navigationController
            mapView.getMapboxMap().loadStyle(Style.STANDARD) {
                controller?.renderStopMarkers(
                    stops = state.optimizedStops,
                    stopVisitStatuses = state.stopVisitStatuses,
                    onStopMarkerClick = onStopMarkerClick
                )
            }

            root
        },
        update = {
            navigationController?.renderStopMarkers(
                stops = state.optimizedStops,
                stopVisitStatuses = state.stopVisitStatuses,
                onStopMarkerClick = onStopMarkerClick
            )
        }
    )
}

private class OfficialNavigationController(
    context: Context,
    private val mapView: MapView,
    private val locationProvider: NavigationLocationProvider,
    private val navigationHud: NavigationHud
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
        routeLineApi.setNavigationRoutes(routes) { routeDrawData ->
            mapboxMap.getStyle { style ->
                routeLineView.renderRouteDrawData(style, routeDrawData)
            }
        }

        routes.firstOrNull()?.let { route ->
            navigationHud.renderRouteReady(route)
            viewportDataSource.onRouteChanged(route)
            viewportDataSource.evaluate()
            followRoute()
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

        navigationHud.renderRouteProgress(routeProgress)
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

    /** Desenha as paradas da rota e conecta cada marcador a gaveta de feedback. */
    fun renderStopMarkers(
        stops: List<NearbyCustomer>,
        stopVisitStatuses: Map<Long, String>,
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
                val markerColor = when (stopVisitStatuses[customer.id]) {
                    "visited" -> 0xFF009279.toInt()
                    "not_visited" -> 0xFFB9382F.toInt()
                    else -> 0xFF5889FB.toInt()
                }
                val annotation = manager.create(
                    CircleAnnotationOptions()
                        .withPoint(Point.fromLngLat(customer.longitude, customer.latitude))
                        .withCircleColor(markerColor)
                        .withCircleRadius(10.0)
                        .withCircleStrokeColor(AndroidColor.WHITE)
                        .withCircleStrokeWidth(3.0)
                )
                customerByAnnotationId[annotation.id] = customer
            }
        }
    }
}

/**
 * Painel visual da navegacao. Ele nao usa os widgets prontos do SDK porque eles dependem
 * de atributos de tema que nao existem na tela Compose. Os valores exibidos continuam vindo
 * exclusivamente do RouteProgressObserver oficial do Mapbox.
 */
private class NavigationHud(
    private val context: Context,
    onStopNavigation: () -> Unit
) {
    private val nextInstruction = navigationText(
        text = "Preparando navegacao...",
        sizeSp = 20f,
        color = AndroidColor.WHITE,
        bold = true,
        maxLines = 2
    )
    private val maneuverDistance = navigationText(
        text = "Calculando proxima manobra",
        sizeSp = 14f,
        color = 0xFFA4E0CE.toInt(),
        maxLines = 1
    )
    private val remainingDistance = navigationText(
        text = "Rota sendo calculada",
        sizeSp = 27f,
        color = 0xFF00463A.toInt(),
        bold = true,
        maxLines = 1
    )
    private val remainingTime = navigationText(
        text = "Aguarde a localizacao do GPS",
        sizeSp = 16f,
        color = 0xFF315D54.toInt(),
        maxLines = 1
    )
    private val stopSummary = navigationText(
        text = "Rota planejada",
        sizeSp = 14f,
        color = 0xFF526662.toInt(),
        maxLines = 1
    )

    val topPanel: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(context.dp(16), context.dp(14), context.dp(16), context.dp(14))
        background = context.roundedBackground(0xFF00463A.toInt())
        elevation = context.dp(6).toFloat()

        addView(
            navigationText(
                text = "PROXIMA MANOBRA",
                sizeSp = 12f,
                color = 0xFFA4E0CE.toInt(),
                bold = true,
                maxLines = 1
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        addView(
            nextInstruction,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = context.dp(3) }
        )
        addView(
            maneuverDistance,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = context.dp(4) }
        )
    }

    val bottomPanel: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(context.dp(16), context.dp(14), context.dp(16), context.dp(14))
        background = context.roundedBackground(AndroidColor.WHITE)
        elevation = context.dp(6).toFloat()

        addView(
            remainingDistance,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        addView(
            remainingTime,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = context.dp(2) }
        )
        addView(
            stopSummary,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = context.dp(4) }
        )
        addView(
            android.widget.Button(context).apply {
                text = "Encerrar navegacao"
                setTextColor(AndroidColor.WHITE)
                textSize = 15f
                isAllCaps = false
                background = context.roundedBackground(0xFFC62828.toInt(), cornerRadiusDp = 10)
                setOnClickListener { onStopNavigation() }
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                context.dp(48)
            ).apply { topMargin = context.dp(10) }
        )
    }

    fun renderRouteReady(route: NavigationRoute) {
        val routeDistance = route.directionsRoute.distance()
        val routeDuration = route.directionsRoute.duration()
        updateUi {
            nextInstruction.text = "Siga pela rota planejada"
            maneuverDistance.text = "A navegacao inicia ao localizar sua posicao"
            remainingDistance.text = formatDistance(routeDistance)
            remainingTime.text = "${formatDuration(routeDuration)} restantes • chega ${formatEta(routeDuration)}"
            stopSummary.text = "Rota pronta"
        }
    }

    fun renderRouteProgress(routeProgress: RouteProgress) {
        val stepProgress = routeProgress.currentLegProgress?.currentStepProgress
        val instruction = stepProgress?.step?.maneuver()?.instruction()
            ?.takeIf { it.isNotBlank() }
            ?: "Siga pela rota"
        val nextManeuverDistance = stepProgress?.distanceRemaining?.toDouble()
            ?: routeProgress.distanceRemaining.toDouble()
        val remainingDistanceMeters = routeProgress.distanceRemaining.toDouble()
        val remainingDurationSeconds = routeProgress.durationRemaining
        val remainingWaypoints = routeProgress.remainingWaypoints

        updateUi {
            nextInstruction.text = instruction
            maneuverDistance.text = "Em ${formatDistance(nextManeuverDistance)}"
            remainingDistance.text = "${formatDistance(remainingDistanceMeters)} restantes"
            remainingTime.text = "${formatDuration(remainingDurationSeconds)} • chega ${formatEta(remainingDurationSeconds)}"
            stopSummary.text = when {
                remainingWaypoints <= 1 -> "Destino final"
                else -> "$remainingWaypoints paradas restantes"
            }
        }
    }

    private fun navigationText(
        text: String,
        sizeSp: Float,
        color: Int,
        bold: Boolean = false,
        maxLines: Int
    ): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = sizeSp
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            this.maxLines = maxLines
        }
    }

    private fun updateUi(block: () -> Unit) {
        topPanel.post(block)
    }
}

private fun Context.dp(value: Int): Int {
    return (value * resources.displayMetrics.density).toInt()
}

private fun Context.roundedBackground(color: Int, cornerRadiusDp: Int = 14): GradientDrawable {
    return GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(cornerRadiusDp).toFloat()
    }
}

@Composable
private fun ResultHeader(
    state: VisitUiState,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onOptimize: () -> Unit,
    onSave: () -> Unit,
    onStartNavigation: () -> Unit,
    onStopNavigation: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "${state.nearbyCustomers.size} clientes proximos",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${state.selectedCustomerIds.size} selecionados",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (state.isRouteQuotaLoading) {
                        "Verificando limite diario de rotas..."
                    } else {
                        "${state.dailyRoutesCreated}/${state.dailyRouteLimit} rotas criadas hoje"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!state.isRouteQuotaLoading && state.dailyRoutesCreated >= state.dailyRouteLimit) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                val distance = state.roadRouteDistanceMeters
                val duration = state.roadRouteDurationSeconds
                if (distance != null && duration != null) {
                    Text(
                        text = "${formatDistance(distance)} • ${formatDuration(duration)} • termina ${formatEta(duration)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (state.isRouteLoading) {
                    Text(
                        text = "Calculando rota pelas ruas...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (state.isSearching || state.isRouteLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(onClick = onSelectAll, leadingIcon = { Icon(Icons.Default.SelectAll, null) }, label = { Text("Selecionar") })
            AssistChip(onClick = onClearSelection, label = { Text("Limpar") })
            AssistChip(onClick = onOptimize, leadingIcon = { Icon(Icons.Default.Route, null) }, label = { Text("Otimizar") })
            AssistChip(
                onClick = if (state.isNavigationActive) onStopNavigation else onStartNavigation,
                enabled = state.roadRoutePoints.size > 1,
                leadingIcon = {
                    Icon(
                        if (state.isNavigationActive) Icons.Default.StopCircle else Icons.Default.Navigation,
                        contentDescription = null
                    )
                },
                label = { Text(if (state.isNavigationActive) "Encerrar" else "Iniciar") }
            )
            Button(
                onClick = onSave,
                enabled = state.selectedCustomerIds.isNotEmpty() &&
                    !state.isSaving &&
                    !state.isRouteQuotaLoading &&
                    state.dailyRoutesCreated < state.dailyRouteLimit
            ) {
                Text(
                    when {
                        state.isSaving -> "Salvando"
                        state.isRouteQuotaLoading -> "Verificando limite"
                        state.dailyRoutesCreated >= state.dailyRouteLimit -> "Limite diario atingido"
                        else -> "Salvar rota"
                    }
                )
            }
        }
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
