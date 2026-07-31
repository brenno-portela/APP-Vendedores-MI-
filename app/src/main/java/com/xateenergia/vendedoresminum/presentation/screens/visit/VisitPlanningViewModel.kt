package com.xateenergia.vendedoresminum.presentation.screens.visit

import android.annotation.SuppressLint
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.xateenergia.vendedoresminum.data.repository.CustomerRepository
import com.xateenergia.vendedoresminum.data.repository.DAILY_ROUTE_CREATION_LIMIT
import com.xateenergia.vendedoresminum.data.repository.FirebaseRouteQuotaRepository
import com.xateenergia.vendedoresminum.data.repository.MapboxDirectionsRepository
import com.xateenergia.vendedoresminum.data.repository.PlannedRouteRepository
import com.xateenergia.vendedoresminum.data.repository.SettingsRepository
import com.xateenergia.vendedoresminum.domain.model.Coordinate
import com.xateenergia.vendedoresminum.domain.model.Customer
import com.xateenergia.vendedoresminum.domain.model.CustomerFilters
import com.xateenergia.vendedoresminum.domain.model.NearbyCustomer
import com.xateenergia.vendedoresminum.domain.model.RouteInstruction
import com.xateenergia.vendedoresminum.domain.usecase.FindNearbyCustomersUseCase
import com.xateenergia.vendedoresminum.domain.usecase.GeocodeAddressUseCase
import com.xateenergia.vendedoresminum.domain.usecase.OptimizeVisitOrderUseCase
import com.xateenergia.vendedoresminum.domain.usecase.SavePlannedRouteUseCase
import com.xateenergia.vendedoresminum.utils.GeoUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VisitPlanningViewModel @Inject constructor(
    private val findNearbyCustomersUseCase: FindNearbyCustomersUseCase,
    private val optimizeVisitOrderUseCase: OptimizeVisitOrderUseCase,
    private val savePlannedRouteUseCase: SavePlannedRouteUseCase,
    private val geocodeAddressUseCase: GeocodeAddressUseCase,
    private val mapboxDirectionsRepository: MapboxDirectionsRepository,
    private val customerRepository: CustomerRepository,
    private val plannedRouteRepository: PlannedRouteRepository,
    private val firebaseRouteQuotaRepository: FirebaseRouteQuotaRepository,
    private val settingsRepository: SettingsRepository,
    private val fusedLocationProviderClient: FusedLocationProviderClient
) : ViewModel() {

    private val _state = MutableStateFlow(VisitUiState())
    val state: StateFlow<VisitUiState> = _state

    private val customerQuery = MutableStateFlow("")
    private var searchJob: Job? = null
    private var roadRouteJob: Job? = null
    private var locationCallback: LocationCallback? = null

    init {
        loadDailyRouteQuota()
        viewModelScope.launch {
            val defaultRadius = settingsRepository.defaultRadiusKm.first()
            val onlyActive = settingsRepository.onlyActiveByDefault.first()
            _state.update { it.copy(radiusKm = defaultRadius, onlyActive = onlyActive) }
        }
        viewModelScope.launch {
            settingsRepository.mapMode.collect { mode ->
                _state.update { it.copy(mapMode = mode) }
            }
        }
        viewModelScope.launch {
            combine(
                customerRepository.observeSegments(),
                customerRepository.observeCities(),
                customerRepository.observeStates(),
                customerRepository.observeStatuses()
            ) { segments, cities, states, statuses ->
                FilterOptions(segments = segments, cities = cities, states = states, statuses = statuses)
            }.collect { options ->
                _state.update { it.copy(filterOptions = options) }
            }
        }
        viewModelScope.launch {
            customerQuery
                .flatMapLatest { query -> customerRepository.observeCustomers(query) }
                .collect { customers ->
                    _state.update { it.copy(customerSuggestions = customers.take(30)) }
                }
        }
    }

    // ========== AÇÕES ==========

    fun setManualLatitude(value: String) {
        _state.update { it.copy(manualLatitude = value) }
    }

    fun setManualLongitude(value: String) {
        _state.update { it.copy(manualLongitude = value) }
    }

    fun setAddressQuery(value: String) {
        _state.update { it.copy(addressQuery = value) }
    }

    fun setCustomerSearchQuery(value: String) {
        customerQuery.value = value
        _state.update { it.copy(customerSearchQuery = value) }
    }

    fun applyManualCoordinate() {
        val latitude = _state.value.manualLatitude.parseCoordinate()
        val longitude = _state.value.manualLongitude.parseCoordinate()
        if (latitude == null || longitude == null || !GeoUtils.isValidCoordinate(latitude, longitude)) {
            showMessage("Informe latitude entre -90 e 90 e longitude entre -180 e 180.")
            return
        }
        setOrigin(Coordinate(latitude, longitude), "Prospecto manual")
    }

    fun searchAddress() {
        val query = _state.value.addressQuery.trim()
        if (query.isBlank()) {
            showMessage("Digite um endereço para buscar.")
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isGeocoding = true, message = null) }
            runCatching {
                geocodeAddressUseCase(query)
            }.onSuccess { coordinate ->
                if (coordinate == null) {
                    _state.update { it.copy(isGeocoding = false, message = "Endereço não encontrado.") }
                } else {
                    _state.update { it.copy(isGeocoding = false) }
                    setOrigin(coordinate, query)
                }
            }.onFailure { throwable ->
                _state.update {
                    it.copy(
                        isGeocoding = false,
                        message = throwable.message ?: "Falha ao buscar endereço."
                    )
                }
            }
        }
    }

    fun selectCustomerAsOrigin(customer: Customer) {
        setOrigin(customer.coordinate, customer.name)
    }

    fun setMapSelectedOrigin(coordinate: Coordinate) {
        setOrigin(coordinate, "Ponto selecionado no mapa")
    }

    @SuppressLint("MissingPermission")
    fun useCurrentLocation() {
        startLocationTracking()
        viewModelScope.launch {
            _state.update { it.copy(isLocating = true, message = null) }
            runCatching {
                fusedLocationProviderClient.lastLocation.await()
            }.onSuccess { location ->
                if (location == null) {
                    _state.update { it.copy(isLocating = false, message = "Localização atual indisponível.") }
                } else {
                    _state.update { it.copy(isLocating = false) }
                    setOrigin(Coordinate(location.latitude, location.longitude), "Localização atual")
                }
            }.onFailure { throwable ->
                _state.update {
                    it.copy(
                        isLocating = false,
                        message = throwable.message ?: "Não foi possível obter sua localização."
                    )
                }
            }
        }
    }

    fun setRadiusKm(value: Double) {
        _state.update { it.copy(radiusKm = value) }
        refreshNearby()
    }

    fun setSegment(value: String?) {
        _state.update { it.copy(segment = value) }
        refreshNearby()
    }

    fun setCity(value: String?) {
        _state.update { it.copy(city = value) }
        refreshNearby()
    }

    fun setState(value: String?) {
        _state.update { it.copy(stateUf = value) }
        refreshNearby()
    }

    fun setStatus(value: String?) {
        _state.update { it.copy(status = value) }
        refreshNearby()
    }

    fun setOnlyWithPhone(value: Boolean) {
        _state.update { it.copy(onlyWithPhone = value) }
        refreshNearby()
    }

    fun setOnlyActive(value: Boolean) {
        _state.update { it.copy(onlyActive = value) }
        refreshNearby()
    }

    fun toggleCustomerSelection(customerId: Long) {
        _state.update { current ->
            val selected = if (customerId in current.selectedCustomerIds) {
                current.selectedCustomerIds - customerId
            } else {
                current.selectedCustomerIds + customerId
            }
            current.copy(selectedCustomerIds = selected).withOptimizedRoute()
        }
        refreshRoadRoute()
    }

    fun selectAllNearby() {
        _state.update { current ->
            current.copy(selectedCustomerIds = current.nearbyCustomers.map { it.customer.id }.toSet()).withOptimizedRoute()
        }
        refreshRoadRoute()
    }

    fun clearSelection() {
        roadRouteJob?.cancel()
        _state.update {
            it.copy(
                selectedCustomerIds = emptySet(),
                roadRoutePoints = emptyList(),
                roadRouteDistanceMeters = null,
                roadRouteDurationSeconds = null,
                routeInstructions = emptyList(),
                isNavigationActive = false,
                navigationWaypoints = emptyList(),
                isRouteLoading = false
            ).withOptimizedRoute()
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationTracking() {
        if (locationCallback != null) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMinUpdateIntervalMillis(2_000L)
            .setMinUpdateDistanceMeters(5f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let(::updateCurrentLocation)
            }
        }

        locationCallback = callback
        fusedLocationProviderClient.requestLocationUpdates(request, callback, null)
        viewModelScope.launch {
            runCatching { fusedLocationProviderClient.lastLocation.await() }
                .getOrNull()
                ?.let(::updateCurrentLocation)
        }
    }

    fun optimizeRoute() {
        _state.update { it.withOptimizedRoute() }
        refreshRoadRoute()
    }

    fun saveRoute() {
        val current = _state.value
        if (current.isRouteQuotaLoading) {
            showMessage("Aguarde a verificacao do limite diario de rotas.")
            return
        }
        if (current.dailyRoutesCreated >= current.dailyRouteLimit) {
            showMessage("Limite diario de ${current.dailyRouteLimit} rotas atingido. Tente novamente amanha.")
            return
        }
        val origin = current.origin
        if (origin == null) {
            showMessage("Defina o prospecto principal antes de salvar.")
            return
        }
        val orderedStops = current.optimizedStops.ifEmpty {
            optimizeVisitOrderUseCase(origin, current.nearbyCustomers.filter { it.customer.id in current.selectedCustomerIds })
        }
        if (orderedStops.isEmpty()) {
            showMessage("Selecione pelo menos um cliente próximo.")
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, message = null) }
            var reservationCreated = false
            try {
                val quota = firebaseRouteQuotaRepository.reserveRouteCreation()
                reservationCreated = true
                _state.update { it.copy(dailyRoutesCreated = quota.used, dailyRouteLimit = quota.limit) }

                val routeId = savePlannedRouteUseCase(
                    name = "Rota ${System.currentTimeMillis()}",
                    mainCustomerName = current.originLabel,
                    origin = origin,
                    radiusKm = current.radiusKm,
                    orderedStops = orderedStops,
                    routeDistanceMeters = current.roadRouteDistanceMeters,
                    routeDurationSeconds = current.roadRouteDurationSeconds,
                    startLocation = current.currentLocation
                )
                _state.update {
                    it.copy(
                        activeRouteId = routeId,
                        stopVisitStatuses = emptyMap(),
                        savedFeedbackCustomerId = null,
                        isSaving = false,
                        message = "Rota salva com ${orderedStops.size} paradas. Navegacao iniciada no app."
                    ).startNavigationMode(orderedStops)
                }
                markNavigationStarted(routeId)
            } catch (throwable: Throwable) {
                if (reservationCreated) {
                    runCatching { firebaseRouteQuotaRepository.releaseRouteCreation() }
                        .getOrNull()
                        ?.let { quota ->
                            _state.update { it.copy(dailyRoutesCreated = quota.used, dailyRouteLimit = quota.limit) }
                        }
                }
                _state.update {
                    it.copy(
                        isSaving = false,
                        message = throwable.message ?: "Falha ao salvar rota."
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    // ========== MÉTODOS PRIVADOS ==========

    private fun setOrigin(coordinate: Coordinate, label: String) {
        _state.update {
            it.copy(
                origin = coordinate,
                originLabel = label,
                manualLatitude = coordinate.latitude.toString(),
                manualLongitude = coordinate.longitude.toString(),
                selectedCustomerIds = emptySet(),
                optimizedStops = emptyList(),
                roadRoutePoints = emptyList(),
                roadRouteDistanceMeters = null,
                roadRouteDurationSeconds = null,
                routeInstructions = emptyList(),
                isNavigationActive = false,
                navigationWaypoints = emptyList(),
                stopVisitStatuses = emptyMap(),
                savedFeedbackCustomerId = null,
                isSavingStopFeedback = false,
                isRouteLoading = false
            )
        }
        refreshNearby()
    }

    private fun refreshNearby() {
        val current = _state.value
        val origin = current.origin ?: return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.update { it.copy(isSearching = true, message = null) }
            runCatching {
                findNearbyCustomersUseCase(origin, current.toFilters())
            }.onSuccess { nearby ->
                _state.update {
                    it.copy(
                        isSearching = false,
                        nearbyCustomers = nearby,
                        selectedCustomerIds = it.selectedCustomerIds.intersect(nearby.map { item -> item.customer.id }.toSet())
                    ).withOptimizedRoute()
                }
                refreshRoadRoute()
            }.onFailure { throwable ->
                _state.update {
                    it.copy(
                        isSearching = false,
                        message = throwable.message ?: "Falha ao calcular clientes próximos."
                    )
                }
            }
        }
    }

    private fun VisitUiState.toFilters(): CustomerFilters {
        return CustomerFilters(
            radiusKm = radiusKm,
            segment = segment,
            city = city,
            state = stateUf,
            status = status,
            onlyWithPhone = onlyWithPhone,
            onlyActive = onlyActive
        )
    }

    private fun VisitUiState.withOptimizedRoute(): VisitUiState {
        val origin = origin ?: return copy(optimizedStops = emptyList(), nearbyCustomers = nearbyCustomers.map { it.copy(routeOrder = null) })
        val selected = nearbyCustomers.filter { it.customer.id in selectedCustomerIds }
        val ordered = optimizeVisitOrderUseCase(origin, selected)
        val orderById = ordered.associate { it.customer.id to it.routeOrder }
        return copy(
            optimizedStops = ordered,
            nearbyCustomers = nearbyCustomers.map { item ->
                item.copy(routeOrder = orderById[item.customer.id])
            }
        )
    }

    private fun refreshRoadRoute() {
        val current = _state.value
        val origin = current.origin
        val orderedStops = current.optimizedStops

        roadRouteJob?.cancel()

        if (origin == null || orderedStops.isEmpty()) {
            _state.update {
                it.copy(
                    roadRoutePoints = emptyList(),
                    roadRouteDistanceMeters = null,
                    roadRouteDurationSeconds = null,
                    routeInstructions = emptyList(),
                    isNavigationActive = false,
                    navigationWaypoints = emptyList(),
                    isRouteLoading = false
                )
            }
            return
        }

        val routeCoordinates = listOf(origin) + orderedStops.map { it.customer.coordinate }
        if (routeCoordinates.size > 25) {
            _state.update {
                it.copy(
                    roadRoutePoints = emptyList(),
                    roadRouteDistanceMeters = null,
                    roadRouteDurationSeconds = null,
                    routeInstructions = emptyList(),
                    isNavigationActive = false,
                    navigationWaypoints = emptyList(),
                    isRouteLoading = false,
                    message = "O Mapbox calcula ate 25 pontos por rota. Selecione no maximo 24 clientes."
                )
            }
            return
        }

        roadRouteJob = viewModelScope.launch {
            _state.update { it.copy(isRouteLoading = true, message = null) }
            runCatching {
                mapboxDirectionsRepository.getDrivingRoute(routeCoordinates)
            }.onSuccess { roadRoute ->
                _state.update {
                    it.copy(
                        roadRoutePoints = roadRoute.points,
                        roadRouteDistanceMeters = roadRoute.distanceMeters,
                        roadRouteDurationSeconds = roadRoute.durationSeconds,
                        routeInstructions = roadRoute.instructions,
                        isRouteLoading = false
                    )
                }
            }.onFailure { throwable ->
                _state.update {
                    it.copy(
                        roadRoutePoints = emptyList(),
                        roadRouteDistanceMeters = null,
                        roadRouteDurationSeconds = null,
                        routeInstructions = emptyList(),
                        isNavigationActive = false,
                        navigationWaypoints = emptyList(),
                        isRouteLoading = false,
                        message = throwable.message ?: "Nao foi possivel calcular a rota pelas ruas."
                    )
                }
            }
        }
    }

    fun startNavigation() {
        val current = _state.value
        val origin = current.origin
        if (origin == null || current.optimizedStops.isEmpty()) {
            showMessage("Selecione pelo menos uma parada antes de iniciar.")
            return
        }
        startLocationTracking()
        _state.update { it.startNavigationMode() }
        _state.value.activeRouteId?.let(::markNavigationStarted)
    }

    fun stopNavigation() {
        _state.update {
            it.copy(
                isNavigationActive = false,
                navigationWaypoints = emptyList()
            )
        }
    }

    fun saveStopFeedback(customer: Customer, wasVisited: Boolean, feedback: String) {
        val current = _state.value
        val routeId = current.activeRouteId
        val location = current.currentLocation

        when {
            routeId == null -> showMessage("Salve a rota antes de registrar uma visita.")
            location == null -> showMessage("Aguardando sua localizacao. Ative o GPS e tente novamente.")
            feedback.trim().length < MINIMUM_FEEDBACK_LENGTH -> {
                showMessage("O feedback precisa ter pelo menos $MINIMUM_FEEDBACK_LENGTH caracteres.")
            }
            else -> {
                viewModelScope.launch {
                    _state.update { it.copy(isSavingStopFeedback = true, message = null) }
                    runCatching {
                        plannedRouteRepository.saveStopFeedback(
                            routeId = routeId,
                            customer = customer,
                            wasVisited = wasVisited,
                            feedback = feedback.trim(),
                            location = location
                        )
                    }.onSuccess {
                        _state.update {
                            it.copy(
                                isSavingStopFeedback = false,
                                stopVisitStatuses = it.stopVisitStatuses + (
                                    customer.id to if (wasVisited) "visited" else "not_visited"
                                ),
                                savedFeedbackCustomerId = customer.id,
                                message = "Feedback de ${customer.name} salvo."
                            )
                        }
                    }.onFailure { throwable ->
                        _state.update {
                            it.copy(
                                isSavingStopFeedback = false,
                                message = throwable.message ?: "Nao foi possivel salvar o feedback."
                            )
                        }
                    }
                }
            }
        }
    }

    fun consumeSavedFeedbackCustomer() {
        _state.update { it.copy(savedFeedbackCustomerId = null) }
    }

    private fun String.parseCoordinate(): Double? {
        return trim().replace(",", ".").toDoubleOrNull()
    }

    private fun showMessage(message: String) {
        _state.update { it.copy(message = message) }
    }

    private fun loadDailyRouteQuota() {
        viewModelScope.launch {
            _state.update { it.copy(isRouteQuotaLoading = true) }
            runCatching { firebaseRouteQuotaRepository.getTodayQuota() }
                .onSuccess { quota ->
                    _state.update {
                        it.copy(
                            dailyRoutesCreated = quota.used,
                            dailyRouteLimit = quota.limit,
                            isRouteQuotaLoading = false
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isRouteQuotaLoading = false,
                            message = throwable.message ?: "Nao foi possivel verificar o limite diario de rotas."
                        )
                    }
                }
        }
    }

    private fun updateCurrentLocation(location: Location) {
        _state.update {
            it.copy(currentLocation = Coordinate(location.latitude, location.longitude))
        }
    }

    private fun markNavigationStarted(routeId: Long) {
        viewModelScope.launch {
            plannedRouteRepository.updateRouteNavigationStatus(routeId, "em andamento")
        }
    }

    override fun onCleared() {
        locationCallback?.let { fusedLocationProviderClient.removeLocationUpdates(it) }
        locationCallback = null
        super.onCleared()
    }
}

private fun VisitUiState.startNavigationMode(
    orderedStops: List<NearbyCustomer> = optimizedStops
): VisitUiState {
    if (orderedStops.isEmpty()) return this
    val plannedOrigin = origin ?: return this
    val start = currentLocation
        ?.takeIf { GeoUtils.haversineDistanceMeters(it, plannedOrigin) <= MAX_START_DISTANCE_FROM_ROUTE_METERS }
        ?: plannedOrigin
    val waypoints = listOf(start) + orderedStops.map { it.customer.coordinate }
    return copy(
        isNavigationActive = true,
        navigationWaypoints = waypoints
    )
}

private const val MAX_START_DISTANCE_FROM_ROUTE_METERS = 50_000.0
private const val MINIMUM_FEEDBACK_LENGTH = 20

data class VisitUiState(
    val dailyRouteLimit: Int = DAILY_ROUTE_CREATION_LIMIT,
    val dailyRoutesCreated: Int = 0,
    val isRouteQuotaLoading: Boolean = true,
    val activeRouteId: Long? = null,
    val origin: Coordinate? = null,
    val originLabel: String = "Prospecto principal",
    val manualLatitude: String = "",
    val manualLongitude: String = "",
    val addressQuery: String = "",
    val customerSearchQuery: String = "",
    val customerSuggestions: List<Customer> = emptyList(),
    val radiusKm: Double = 5.0,
    val segment: String? = null,
    val city: String? = null,
    val stateUf: String? = null,
    val status: String? = null,
    val onlyWithPhone: Boolean = false,
    val onlyActive: Boolean = true,
    val mapMode: String = "NORMAL",
    val filterOptions: FilterOptions = FilterOptions(),
    val nearbyCustomers: List<NearbyCustomer> = emptyList(),
    val selectedCustomerIds: Set<Long> = emptySet(),
    val optimizedStops: List<NearbyCustomer> = emptyList(),
    val currentLocation: Coordinate? = null,
    val roadRoutePoints: List<Coordinate> = emptyList(),
    val roadRouteDistanceMeters: Double? = null,
    val roadRouteDurationSeconds: Double? = null,
    val routeInstructions: List<RouteInstruction> = emptyList(),
    val isNavigationActive: Boolean = false,
    val navigationWaypoints: List<Coordinate> = emptyList(),
    val stopVisitStatuses: Map<Long, String> = emptyMap(),
    val isSavingStopFeedback: Boolean = false,
    val savedFeedbackCustomerId: Long? = null,
    val isSearching: Boolean = false,
    val isGeocoding: Boolean = false,
    val isLocating: Boolean = false,
    val isSaving: Boolean = false,
    val isRouteLoading: Boolean = false,
    val message: String? = null
)

data class FilterOptions(
    val segments: List<String> = emptyList(),
    val cities: List<String> = emptyList(),
    val states: List<String> = emptyList(),
    val statuses: List<String> = emptyList()
)
