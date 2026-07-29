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
            runCatching {
                savePlannedRouteUseCase(
                    name = "Rota ${System.currentTimeMillis()}",
                    mainCustomerName = current.originLabel,
                    origin = origin,
                    radiusKm = current.radiusKm,
                    orderedStops = orderedStops,
                    routeDistanceMeters = current.roadRouteDistanceMeters,
                    routeDurationSeconds = current.roadRouteDurationSeconds,
                    startLocation = current.currentLocation
                )
            }.onSuccess { routeId ->
                _state.update {
                    it.copy(
                        activeRouteId = routeId,
                        isSaving = false,
                        message = "Rota salva com ${orderedStops.size} paradas. Navegacao iniciada no app."
                    ).startNavigationMode()
                }
                markNavigationStarted(routeId)
            }.onFailure { throwable ->
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
        if (current.roadRoutePoints.size < 2) {
            showMessage("Calcule uma rota com pelo menos uma parada antes de iniciar.")
            return
        }
        startLocationTracking()
        _state.update { it.startNavigationMode() }
        _state.value.activeRouteId?.let(::markNavigationStarted)
    }

    fun stopNavigation() {
        val routeId = _state.value.activeRouteId
        _state.update {
            it.copy(
                isNavigationActive = false,
                navigationWaypoints = emptyList()
            )
        }
        if (routeId != null) {
            viewModelScope.launch {
                plannedRouteRepository.updateRouteNavigationStatus(
                    routeId = routeId,
                    status = "concluida",
                    isCompleted = true
                )
            }
        }
    }

    private fun String.parseCoordinate(): Double? {
        return trim().replace(",", ".").toDoubleOrNull()
    }

    private fun showMessage(message: String) {
        _state.update { it.copy(message = message) }
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

private fun VisitUiState.startNavigationMode(): VisitUiState {
    if (roadRoutePoints.size < 2) return this
    val start = currentLocation ?: origin ?: return this
    val waypoints = listOf(start) + optimizedStops.map { it.customer.coordinate }
    return copy(
        isNavigationActive = true,
        navigationWaypoints = waypoints
    )
}

data class VisitUiState(
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
