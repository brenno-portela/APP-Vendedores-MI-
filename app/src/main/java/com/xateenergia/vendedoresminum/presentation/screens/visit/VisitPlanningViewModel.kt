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
import com.xateenergia.vendedoresminum.data.repository.DailyRouteLimitReachedException
import com.xateenergia.vendedoresminum.data.repository.FirebaseRouteQuotaRepository
import com.xateenergia.vendedoresminum.data.repository.FirebaseRouteTelemetryRepository
import com.xateenergia.vendedoresminum.data.repository.FirebaseSharedRouteRepository
import com.xateenergia.vendedoresminum.data.repository.FirebaseVisitAttendanceRepository
import com.xateenergia.vendedoresminum.data.repository.MapboxDirectionsRepository
import com.xateenergia.vendedoresminum.data.repository.PlannedRouteRepository
import com.xateenergia.vendedoresminum.data.repository.SettingsRepository
import com.xateenergia.vendedoresminum.data.repository.VisitAttendanceRepository
import com.xateenergia.vendedoresminum.domain.model.AttendancePanelMode
import com.xateenergia.vendedoresminum.domain.model.Coordinate
import com.xateenergia.vendedoresminum.domain.model.Customer
import com.xateenergia.vendedoresminum.domain.model.CustomerFilters
import com.xateenergia.vendedoresminum.domain.model.NearbyCustomer
import com.xateenergia.vendedoresminum.domain.model.RouteInstruction
import com.xateenergia.vendedoresminum.domain.model.VisitAttendance
import com.xateenergia.vendedoresminum.domain.model.VisitAttendanceStatus
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
import java.util.UUID

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
    private val firebaseSharedRouteRepository: FirebaseSharedRouteRepository,
    private val firebaseRouteQuotaRepository: FirebaseRouteQuotaRepository,
    private val firebaseRouteTelemetryRepository: FirebaseRouteTelemetryRepository,
    private val visitAttendanceRepository: VisitAttendanceRepository,
    private val firebaseVisitAttendanceRepository: FirebaseVisitAttendanceRepository,
    private val settingsRepository: SettingsRepository,
    private val fusedLocationProviderClient: FusedLocationProviderClient
) : ViewModel() {

    private val _state = MutableStateFlow(VisitUiState())
    val state: StateFlow<VisitUiState> = _state

    private val customerQuery = MutableStateFlow("")
    private var searchJob: Job? = null
    private var roadRouteJob: Job? = null
    private var attendanceJob: Job? = null
    private var observedAttendanceRouteId: String? = null
    private var locationCallback: LocationCallback? = null
    private var routeTelemetrySession: RouteTelemetrySession? = null

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
        setOrigin(customer.navigationCoordinate, customer.name)
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
        finishActiveRouteTelemetryIfNeeded()
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
        try {
            fusedLocationProviderClient.requestLocationUpdates(request, callback, null)
        } catch (_: SecurityException) {
            locationCallback = null
            showMessage("Permita a localizacao para iniciar a navegacao da rota.")
            return
        }
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
        if (current.activeSharedRouteId != null) {
            showMessage("Esta rota foi atribuida pelo administrador e nao precisa ser salva novamente.")
            return
        }
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
                        optimizedStops = orderedStops,
                        stopVisitStatuses = emptyMap(),
                        navigationTargetCustomerId = null,
                        deferredNavigationCustomerIds = emptySet(),
                        showPendingStopsSheet = false,
                        attendancePanelMode = AttendancePanelMode.HIDDEN,
                        attendanceCustomerId = null,
                        activeAttendance = null,
                        attendanceHistoryByCustomer = emptyMap(),
                        savedFeedbackCustomerId = null,
                        isSaving = false,
                        message = "Rota salva com ${orderedStops.size} paradas. Navegacao iniciada no app."
                    ).startNavigationMode(orderedStops)
                }
                markNavigationStarted()
            } catch (throwable: Throwable) {
                if (reservationCreated) {
                    runCatching { firebaseRouteQuotaRepository.releaseRouteCreation() }
                        .getOrNull()
                        ?.let { quota ->
                            _state.update { it.copy(dailyRoutesCreated = quota.used, dailyRouteLimit = quota.limit) }
                        }
                }
                if (throwable is DailyRouteLimitReachedException) {
                    loadDailyRouteQuota()
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

    /** Carrega uma rota recebida do backoffice preservando a ordem definida pelo administrador. */
    fun loadSharedRoute(routeId: String) {
        if (_state.value.activeSharedRouteId == routeId) return
        finishActiveRouteTelemetryIfNeeded()
        attendanceJob?.cancel()
        attendanceJob = null
        observedAttendanceRouteId = null

        viewModelScope.launch {
            _state.update { it.copy(isRouteLoading = true, message = null) }
            runCatching { firebaseSharedRouteRepository.getAssignedRoute(routeId) }
                .onSuccess { assignment ->
                    if (assignment == null || assignment.stops.isEmpty()) {
                        _state.update {
                            it.copy(
                                isRouteLoading = false,
                                message = "Esta rota compartilhada nao esta mais disponivel."
                            )
                        }
                        return@onSuccess
                    }

                    val orderedStops = assignment.stops.sortedBy { it.order }.map { stop ->
                        NearbyCustomer(
                            customer = stop.customer,
                            distanceMeters = 0.0,
                            selected = true,
                            routeOrder = stop.order
                        )
                    }
                    val feedbackStatuses = assignment.stops.mapNotNull { stop ->
                        stop.status.takeIf { it == "visited" || it == "not_visited" }
                            ?.let { status -> stop.customer.id to status }
                    }.toMap()

                    _state.update {
                        it.copy(
                            activeRouteId = null,
                            activeSharedRouteId = assignment.id,
                            sharedRouteName = assignment.name,
                            sharedRouteDueDate = assignment.dueDate,
                            sharedRouteTargetCompletionPercent = assignment.targetCompletionPercent,
                            sharedRouteNotes = assignment.notes,
                            sharedRouteStatus = assignment.status,
                            sharedStopIds = assignment.stops.associate { stop -> stop.customer.id to stop.id },
                            origin = orderedStops.first().customer.navigationCoordinate,
                            originLabel = assignment.name,
                            nearbyCustomers = orderedStops,
                            selectedCustomerIds = orderedStops.map { item -> item.customer.id }.toSet(),
                            optimizedStops = orderedStops,
                            stopVisitStatuses = feedbackStatuses,
                            navigationTargetCustomerId = null,
                            deferredNavigationCustomerIds = emptySet(),
                            showPendingStopsSheet = false,
                            attendancePanelMode = AttendancePanelMode.HIDDEN,
                            attendanceCustomerId = null,
                            activeAttendance = null,
                            attendanceHistoryByCustomer = emptyMap(),
                            roadRoutePoints = emptyList(),
                            roadRouteDistanceMeters = assignment.estimatedDistanceMeters,
                            roadRouteDurationSeconds = assignment.estimatedDurationSeconds,
                            routeInstructions = emptyList(),
                            isRouteLoading = false,
                            isNavigationActive = false,
                            navigationWaypoints = emptyList(),
                            shouldAutoStartSharedRoute = true
                        )
                    }
                    // A tela solicita a permissao em tempo de execucao e so entao
                    // inicia o rastreamento. Evita SecurityException ao abrir uma
                    // rota compartilhada pela primeira vez em um aparelho.
                    refreshRoadRoute()
                    if (_state.value.currentLocation != null) {
                        startNavigation()
                    } else {
                        showMessage("Rota compartilhada carregada. Aguardando sua localizacao para iniciar a navegacao.")
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isRouteLoading = false,
                            message = throwable.message ?: "Nao foi possivel carregar a rota compartilhada."
                        )
                    }
                }
        }
    }

    // ========== MÉTODOS PRIVADOS ==========

    private fun setOrigin(coordinate: Coordinate, label: String) {
        finishActiveRouteTelemetryIfNeeded()
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
                navigationTargetCustomerId = null,
                deferredNavigationCustomerIds = emptySet(),
                showPendingStopsSheet = false,
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

        val routeCoordinates = if (current.activeSharedRouteId != null) {
            val start = current.currentLocation ?: origin
            listOf(start) + orderedStops.map { it.customer.navigationCoordinate }.dropWhile { it == start }
        } else {
            listOf(origin) + orderedStops.map { it.customer.navigationCoordinate }
        }
        if (routeCoordinates.size < 2) {
            _state.update {
                it.copy(
                    roadRoutePoints = emptyList(),
                    routeInstructions = emptyList(),
                    isRouteLoading = false
                )
            }
            return
        }
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
        if (current.activeSharedRouteId != null && current.currentLocation == null) {
            showMessage("Aguardando sua localizacao para iniciar a rota compartilhada.")
            return
        }
        if (current.pendingStops().isEmpty()) {
            showMessage("Todas as paradas desta rota ja foram concluídas.")
            return
        }
        startLocationTracking()
        _state.update { it.copy(shouldAutoStartSharedRoute = false).startNavigationMode() }
        markNavigationStarted()
    }

    /**
     * Direciona para a proxima parada sem registrar um resultado artificial para
     * a parada atual. Ela continua na lista de pendencias para o vendedor voltar
     * quando for possivel atendê-la.
     */
    fun navigateToNextStop() {
        val current = _state.value
        if (!current.isNavigationActive) return
        val activeAttendance = current.activeAttendance
        if (activeAttendance?.isOpen == true) {
            showMessage("Finalize o checkout de ${activeAttendance.customerName} antes de trocar de parada.")
            return
        }

        val currentTargetId = current.navigationTargetCustomerId
        val nextStop = current.nextAutomaticStop(afterCustomerId = currentTargetId)
        if (nextStop == null) {
            showMessage("Nao ha outra parada nova. Use Ver pendencias para escolher um retorno.")
            return
        }

        _state.update { state ->
            val deferred = buildSet {
                addAll(state.deferredNavigationCustomerIds)
                currentTargetId
                    ?.takeIf { targetId -> state.stopVisitStatuses[targetId] != "visited" }
                    ?.let(::add)
            }
            state.withNavigationTarget(
                target = nextStop,
                deferredCustomerIds = deferred - nextStop.customer.id
            )
        }
    }

    /** Exibe as paradas que ainda precisam de um atendimento concluido. */
    fun showPendingStops() {
        val current = _state.value
        if (current.pendingStops().isEmpty()) {
            showMessage("Nao ha pendencias nesta rota.")
            return
        }
        _state.update { it.copy(showPendingStopsSheet = true) }
    }

    fun dismissPendingStops() {
        _state.update { it.copy(showPendingStopsSheet = false) }
    }

    /** Permite retomar uma parada ignorada ou uma visita nao concluida. */
    fun navigateToPendingStop(customerId: Long) {
        val current = _state.value
        if (!current.isNavigationActive) return
        val activeAttendance = current.activeAttendance
        if (activeAttendance?.isOpen == true) {
            showMessage("Finalize o checkout de ${activeAttendance.customerName} antes de trocar de parada.")
            return
        }
        val target = current.optimizedStops.firstOrNull { it.customer.id == customerId } ?: return
        if (current.stopVisitStatuses[customerId] == "visited") {
            showMessage("Esta parada ja foi concluida.")
            return
        }

        _state.update { state ->
            val deferred = buildSet {
                addAll(state.deferredNavigationCustomerIds)
                state.navigationTargetCustomerId
                    ?.takeIf { targetId ->
                        targetId != customerId && state.stopVisitStatuses[targetId] != "visited"
                    }
                    ?.let(::add)
            }
            state.withNavigationTarget(
                target = target,
                deferredCustomerIds = deferred - customerId
            ).copy(showPendingStopsSheet = false)
        }
    }

    /**
     * O encerramento da navegacao sempre deixa uma situacao explicita no
     * historico. Com todas as paradas reportadas, a rota e concluida; caso
     * contrario, a interface solicita o motivo da nao realizacao.
     */
    fun requestNavigationFinish() {
        val current = _state.value
        if (!current.isNavigationActive || current.isFinishingNavigation) return
        val activeAttendance = current.activeAttendance
        if (activeAttendance?.isOpen == true) {
            _state.update {
                it.copy(
                    attendanceCustomerId = activeAttendance.customerId,
                    attendancePanelMode = activeAttendance.toPanelMode()
                )
            }
            showMessage("Finalize o checkout de ${activeAttendance.customerName} antes de encerrar a rota.")
            return
        }

        val completedStops = current.completedStopCount()
        if (current.optimizedStops.isNotEmpty() && completedStops == current.optimizedStops.size) {
            finishNavigationWithStatus(isCompleted = true, reason = null)
        } else {
            _state.update { it.copy(showIncompleteRouteDialog = true) }
        }
    }

    fun dismissIncompleteRouteDialog() {
        if (_state.value.isFinishingNavigation) return
        _state.update { it.copy(showIncompleteRouteDialog = false) }
    }

    fun finishNavigationAsNotCompleted(reason: String) {
        val normalizedReason = reason.trim()
        if (normalizedReason.isBlank()) {
            showMessage("Informe o motivo para encerrar a rota como nao realizada.")
            return
        }
        finishNavigationWithStatus(isCompleted = false, reason = normalizedReason)
    }

    private fun finishNavigationWithStatus(isCompleted: Boolean, reason: String?) {
        val current = _state.value
        val localRouteId = current.activeRouteId
        val sharedRouteId = current.activeSharedRouteId
        if (localRouteId == null && sharedRouteId == null) {
            showMessage("Nao foi possivel identificar a rota em andamento.")
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isFinishingNavigation = true,
                    showIncompleteRouteDialog = false,
                    message = null
                )
            }
            runCatching {
                if (sharedRouteId != null) {
                    if (isCompleted) {
                        firebaseSharedRouteRepository.completeRoute(sharedRouteId)
                    } else {
                        firebaseSharedRouteRepository.markRouteNotCompleted(
                            routeId = sharedRouteId,
                            reason = requireNotNull(reason)
                        )
                    }
                } else {
                    plannedRouteRepository.updateRouteNavigationStatus(
                        routeId = requireNotNull(localRouteId),
                        status = if (isCompleted) "completed" else "not_completed",
                        isCompleted = isCompleted,
                        reason = reason
                    )
                }
            }.onSuccess {
                finishActiveRouteTelemetryIfNeeded()
                attendanceJob?.cancel()
                attendanceJob = null
                observedAttendanceRouteId = null
                _state.update {
                    it.copy(
                        isNavigationActive = false,
                        navigationWaypoints = emptyList(),
                        navigationTargetCustomerId = null,
                        deferredNavigationCustomerIds = emptySet(),
                        showPendingStopsSheet = false,
                        attendancePanelMode = AttendancePanelMode.HIDDEN,
                        attendanceCustomerId = null,
                        activeAttendance = null,
                        sharedRouteStatus = if (isCompleted) "completed" else "not_completed",
                        isFinishingNavigation = false,
                        shouldNavigateToHistory = true,
                        message = if (isCompleted) "Rota realizada e salva no historico." else "Rota salva como nao realizada."
                    )
                }
            }.onFailure { throwable ->
                _state.update {
                    it.copy(
                        isFinishingNavigation = false,
                        message = throwable.message ?: "Nao foi possivel encerrar a rota. Tente novamente."
                    )
                }
            }
        }
    }

    fun consumeHistoryNavigation() {
        _state.update { it.copy(shouldNavigateToHistory = false) }
    }

    /** Abre a etapa correta para qualquer cliente tocado no mapa, sem depender da ordem da rota. */
    fun openAttendance(customer: Customer) {
        val current = _state.value
        if (!current.isNavigationActive) {
            showMessage("Inicie a navegacao para registrar um atendimento.")
            return
        }

        val active = current.activeAttendance
        when {
            active != null && active.customerId != customer.id -> {
                _state.update {
                    it.copy(
                        attendanceCustomerId = active.customerId,
                        attendancePanelMode = active.toPanelMode()
                    )
                }
                showMessage("Existe um atendimento em andamento para ${active.customerName}. Faca o checkout antes de iniciar outro.")
            }

            active != null -> {
                _state.update {
                    it.copy(
                        attendanceCustomerId = customer.id,
                        attendancePanelMode = active.toPanelMode()
                    )
                }
            }

            current.attendanceHistoryByCustomer[customer.id].orEmpty().isEmpty() -> {
                _state.update {
                    it.copy(
                        attendanceCustomerId = customer.id,
                        attendancePanelMode = AttendancePanelMode.PRE_CHECK_IN
                    )
                }
            }

            else -> {
                _state.update {
                    it.copy(
                        attendanceCustomerId = customer.id,
                        attendancePanelMode = AttendancePanelMode.RETURN_LIST
                    )
                }
            }
        }
    }

    fun closeAttendancePanel() {
        _state.update {
            it.copy(
                attendanceCustomerId = null,
                attendancePanelMode = AttendancePanelMode.HIDDEN
            )
        }
    }

    /** Cria o registro local antes de sincronizar para que o cronometro sobreviva ao fechamento do app. */
    fun startCustomerCheckIn(customer: Customer) {
        val current = _state.value
        val active = current.activeAttendance
        if (active != null) {
            openAttendance(customer)
            return
        }
        val routeId = telemetryRouteId(current)
        val stopId = current.telemetryStopId(customer)
        val location = current.currentLocation
        if (routeId == null || stopId == null) {
            showMessage("Nao foi possivel identificar a parada desta rota.")
            return
        }
        if (location == null) {
            showMessage("Aguardando sua localizacao. Ative o GPS e tente novamente.")
            return
        }

        val now = System.currentTimeMillis()
        val attendance = VisitAttendance(
            id = UUID.randomUUID().toString(),
            routeId = routeId,
            stopId = stopId,
            customerId = customer.id,
            customerName = customer.name,
            status = VisitAttendanceStatus.IN_PROGRESS,
            checkInAt = now,
            checkInLocation = location,
            checkInAccuracyMeters = current.currentLocationAccuracyMeters,
            checkInDistanceToCustomerMeters = GeoUtils.haversineDistanceMeters(location, customer.coordinate),
            updatedAt = now
        )

        viewModelScope.launch {
            visitAttendanceRepository.save(attendance)
            _state.update {
                it.copy(
                    activeAttendance = attendance,
                    attendanceCustomerId = customer.id,
                    attendancePanelMode = AttendancePanelMode.IN_PROGRESS,
                    message = null
                )
            }
            runCatching {
                firebaseVisitAttendanceRepository.start(
                    attendance = attendance,
                    customer = customer,
                    mirrorToSharedRoute = current.activeSharedRouteId != null
                )
            }.onFailure { throwable ->
                showMessage(
                    throwable.message
                        ?: "Check-in salvo no aparelho. A sincronizacao com o Firebase sera tentada no checkout."
                )
            }
        }
    }

    /** Fecha apenas o tempo de permanencia; o resultado e salvo na etapa seguinte. */
    fun checkoutActiveAttendance() {
        val current = _state.value
        val active = current.activeAttendance
        val location = current.currentLocation
        if (active == null) {
            showMessage("Nenhum atendimento esta em andamento.")
            return
        }
        if (active.status == VisitAttendanceStatus.AWAITING_FEEDBACK) {
            _state.update { it.copy(attendancePanelMode = AttendancePanelMode.POST_CHECK_OUT) }
            return
        }
        if (location == null) {
            showMessage("Aguardando sua localizacao. Ative o GPS antes do checkout.")
            return
        }

        val now = System.currentTimeMillis()
        val checkedOut = active.copy(
            status = VisitAttendanceStatus.AWAITING_FEEDBACK,
            checkOutAt = now,
            checkOutLocation = location,
            checkOutAccuracyMeters = current.currentLocationAccuracyMeters,
            checkOutDistanceToCustomerMeters = current.optimizedStops
                .firstOrNull { it.customer.id == active.customerId }
                ?.let { stop -> GeoUtils.haversineDistanceMeters(location, stop.customer.coordinate) },
            visitDurationSeconds = ((now - active.checkInAt) / 1_000L).coerceAtLeast(0L),
            updatedAt = now
        )
        val customer = current.customerForAttendance(active.customerId) ?: run {
            showMessage("Nao foi possivel localizar os dados do cliente para o checkout.")
            return
        }

        viewModelScope.launch {
            visitAttendanceRepository.save(checkedOut)
            _state.update {
                it.copy(
                    activeAttendance = checkedOut,
                    attendanceCustomerId = checkedOut.customerId,
                    attendancePanelMode = AttendancePanelMode.POST_CHECK_OUT
                )
            }
            runCatching {
                firebaseVisitAttendanceRepository.recordCheckout(
                    attendance = checkedOut,
                    customer = customer,
                    mirrorToSharedRoute = current.activeSharedRouteId != null
                )
            }.onFailure { throwable ->
                showMessage(
                    throwable.message
                        ?: "Checkout preservado no aparelho. Salve o resultado para concluir a sincronizacao."
                )
            }
        }
    }

    fun saveAttendanceOutcome(
        customer: Customer,
        wasVisited: Boolean,
        feedback: String,
        notVisitedReason: String?,
        commercialOutcome: String?,
        nextAction: String,
        nextActionDueDate: String
    ) {
        val current = _state.value
        val active = current.activeAttendance
        val normalizedFeedback = feedback.trim()
        val normalizedNextAction = nextAction.trim().takeIf { it.isNotBlank() }
        val normalizedNextActionDate = nextActionDueDate.trim().takeIf { it.isNotBlank() }
        val normalizedNotVisitedReason = notVisitedReason?.trim()?.takeIf { it.isNotBlank() }
        val normalizedCommercialOutcome = commercialOutcome?.trim()?.takeIf { it.isNotBlank() }

        when {
            active == null || active.customerId != customer.id -> {
                showMessage("Inicie e finalize o checkout antes de salvar o atendimento.")
            }
            active.status != VisitAttendanceStatus.AWAITING_FEEDBACK -> {
                showMessage("Faca o checkout antes de informar o resultado da visita.")
            }
            normalizedFeedback.length < MINIMUM_FEEDBACK_LENGTH -> {
                showMessage("O feedback precisa ter pelo menos $MINIMUM_FEEDBACK_LENGTH caracteres.")
            }
            normalizedNextActionDate != null && normalizedNextAction == null -> {
                showMessage("Descreva a proxima acao antes de informar uma data de retorno.")
            }
            normalizedNextActionDate != null && !NEXT_ACTION_DATE_REGEX.matches(normalizedNextActionDate) -> {
                showMessage("Use a data de retorno no formato AAAA-MM-DD.")
            }
            else -> {
                val completed = active.copy(
                    status = if (wasVisited) VisitAttendanceStatus.VISITED else VisitAttendanceStatus.NOT_VISITED,
                    feedback = normalizedFeedback,
                    notVisitedReason = normalizedNotVisitedReason,
                    commercialOutcome = normalizedCommercialOutcome,
                    nextAction = normalizedNextAction,
                    nextActionDueDate = normalizedNextActionDate,
                    updatedAt = System.currentTimeMillis()
                )
                viewModelScope.launch {
                    _state.update { it.copy(isSavingAttendance = true, message = null) }
                    runCatching {
                        firebaseVisitAttendanceRepository.complete(
                            attendance = completed,
                            customer = customer,
                            mirrorToSharedRoute = current.activeSharedRouteId != null
                        )
                        visitAttendanceRepository.save(completed)
                    }.onSuccess {
                        _state.update { state ->
                            state.copy(
                                activeAttendance = null,
                                attendanceCustomerId = customer.id,
                                attendancePanelMode = AttendancePanelMode.RETURN_LIST,
                                isSavingAttendance = false,
                                stopVisitStatuses = state.stopVisitStatuses + (customer.id to completed.status.firebaseValue),
                                message = "Atendimento de ${customer.name} salvo."
                            ).advanceNavigationAfterAttendance(customer.id)
                        }
                    }.onFailure { throwable ->
                        _state.update {
                            it.copy(
                                isSavingAttendance = false,
                                message = throwable.message ?: "Nao foi possivel salvar o atendimento. Tente novamente."
                            )
                        }
                    }
                }
            }
        }
    }

    fun saveStopFeedback(
        customer: Customer,
        wasVisited: Boolean,
        feedback: String,
        notVisitedReason: String?,
        commercialOutcome: String?,
        nextAction: String,
        nextActionDueDate: String
    ) {
        val current = _state.value
        val routeId = current.activeRouteId
        val location = current.currentLocation
        val normalizedNextAction = nextAction.trim().takeIf { it.isNotBlank() }
        val normalizedNextActionDate = nextActionDueDate.trim().takeIf { it.isNotBlank() }
        val normalizedNotVisitedReason = notVisitedReason?.trim()?.takeIf { it.isNotBlank() }
        val normalizedCommercialOutcome = commercialOutcome?.trim()?.takeIf { it.isNotBlank() }

        when {
            routeId == null && current.activeSharedRouteId == null -> {
                showMessage("Salve a rota antes de registrar uma visita.")
            }
            location == null -> showMessage("Aguardando sua localizacao. Ative o GPS e tente novamente.")
            feedback.trim().length < MINIMUM_FEEDBACK_LENGTH -> {
                showMessage("O feedback precisa ter pelo menos $MINIMUM_FEEDBACK_LENGTH caracteres.")
            }
            normalizedNextActionDate != null && normalizedNextAction == null -> {
                showMessage("Descreva a proxima acao antes de informar uma data de retorno.")
            }
            normalizedNextActionDate != null && !NEXT_ACTION_DATE_REGEX.matches(normalizedNextActionDate) -> {
                showMessage("Use a data de retorno no formato AAAA-MM-DD.")
            }
            else -> {
                viewModelScope.launch {
                    _state.update { it.copy(isSavingStopFeedback = true, message = null) }
                    val sharedRouteId = current.activeSharedRouteId
                    val distanceToCustomerMeters = GeoUtils.haversineDistanceMeters(location, customer.coordinate)
                    runCatching {
                        if (sharedRouteId != null) {
                            val stopId = current.sharedStopIds[customer.id]
                                ?: error("Parada compartilhada nao encontrada.")
                            firebaseSharedRouteRepository.saveStopFeedback(
                                routeId = sharedRouteId,
                                stopId = stopId,
                                customer = customer,
                                wasVisited = wasVisited,
                                feedback = feedback.trim(),
                                location = location,
                                locationAccuracyMeters = current.currentLocationAccuracyMeters,
                                distanceToCustomerMeters = distanceToCustomerMeters,
                                notVisitedReason = normalizedNotVisitedReason,
                                commercialOutcome = normalizedCommercialOutcome,
                                nextAction = normalizedNextAction,
                                nextActionDueDate = normalizedNextActionDate
                            )
                        } else {
                            plannedRouteRepository.saveStopFeedback(
                                routeId = requireNotNull(routeId),
                                customer = customer,
                                wasVisited = wasVisited,
                                feedback = feedback.trim(),
                                location = location,
                                locationAccuracyMeters = current.currentLocationAccuracyMeters,
                                distanceToCustomerMeters = distanceToCustomerMeters,
                                notVisitedReason = normalizedNotVisitedReason,
                                commercialOutcome = normalizedCommercialOutcome,
                                nextAction = normalizedNextAction,
                                nextActionDueDate = normalizedNextActionDate
                            )
                        }
                    }.onSuccess {
                        _state.update { state ->
                            state.copy(
                                isSavingStopFeedback = false,
                                stopVisitStatuses = state.stopVisitStatuses + (
                                    customer.id to if (wasVisited) "visited" else "not_visited"
                                ),
                                savedFeedbackCustomerId = customer.id,
                                message = "Feedback de ${customer.name} salvo."
                            ).advanceNavigationAfterAttendance(customer.id)
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

    /** O check-in acontece no toque "Sim, visitei" e nao cria uma etapa extra na UI. */
    fun recordStopCheckIn(customer: Customer) {
        val current = _state.value
        val location = current.currentLocation
        if (customer.id in current.checkedInCustomerIds || location == null) return
        if (current.activeRouteId == null && current.activeSharedRouteId == null) return

        viewModelScope.launch {
            val distanceToCustomerMeters = GeoUtils.haversineDistanceMeters(location, customer.coordinate)
            runCatching {
                val sharedRouteId = current.activeSharedRouteId
                if (sharedRouteId != null) {
                    val stopId = current.sharedStopIds[customer.id]
                        ?: error("Parada compartilhada nao encontrada.")
                    firebaseSharedRouteRepository.recordStopCheckIn(
                        routeId = sharedRouteId,
                        stopId = stopId,
                        customer = customer,
                        location = location,
                        locationAccuracyMeters = current.currentLocationAccuracyMeters,
                        distanceToCustomerMeters = distanceToCustomerMeters
                    )
                } else {
                    plannedRouteRepository.recordStopCheckIn(
                        routeId = requireNotNull(current.activeRouteId),
                        customer = customer,
                        location = location,
                        locationAccuracyMeters = current.currentLocationAccuracyMeters,
                        distanceToCustomerMeters = distanceToCustomerMeters
                    )
                }
            }.onSuccess {
                _state.update { it.copy(checkedInCustomerIds = it.checkedInCustomerIds + customer.id) }
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
        val shouldAutoStart = _state.value.shouldAutoStartSharedRoute
        val coordinate = Coordinate(location.latitude, location.longitude)
        val accuracyMeters = location.accuracy.takeIf { location.hasAccuracy() }
        _state.update {
            it.copy(
                currentLocation = coordinate,
                currentLocationAccuracyMeters = accuracyMeters
            )
        }
        if (_state.value.isNavigationActive) {
            trackRouteTelemetry(coordinate, accuracyMeters)
        }
        if (shouldAutoStart) {
            startNavigation()
        }
    }

    private fun markNavigationStarted() {
        observeAttendancesForActiveRoute(restoreOpenPanel = true)
        startRouteTelemetryIfPossible()
        val sharedRouteId = _state.value.activeSharedRouteId
        val localRouteId = _state.value.activeRouteId
        viewModelScope.launch {
            // A navegacao local nao pode ser interrompida por uma indisponibilidade
            // momentanea das regras, rede ou do Firebase. O status sera sincronizado
            // novamente nas proximas atualizacoes da rota.
            runCatching {
                if (sharedRouteId != null) {
                    firebaseSharedRouteRepository.markNavigationStarted(sharedRouteId)
                } else if (localRouteId != null) {
                    plannedRouteRepository.updateRouteNavigationStatus(localRouteId, "em andamento")
                }
            }.onFailure { throwable ->
                showMessage(
                    throwable.message
                        ?: "A navegacao foi iniciada, mas nao foi possivel atualizar o status da rota agora."
                )
            }
        }
    }

    /** Inicia uma sessao somente quando existe GPS confiavel para registra-la. */
    private fun startRouteTelemetryIfPossible() {
        val current = _state.value
        if (!current.isNavigationActive) return
        val location = current.currentLocation ?: return
        val routeId = telemetryRouteId(current) ?: return
        if (routeTelemetrySession?.routeId == routeId) return

        val startedAt = System.currentTimeMillis()
        val session = RouteTelemetrySession(
            routeId = routeId,
            routeName = current.sharedRouteName ?: "Rota $routeId",
            startedAtMillis = startedAt,
            plannedDistanceMeters = current.roadRouteDistanceMeters,
            plannedDurationSeconds = current.roadRouteDurationSeconds,
            lastLocation = location,
            lastLocationAtMillis = startedAt,
            lastPersistedAtMillis = startedAt
        )
        routeTelemetrySession = session

        viewModelScope.launch {
            runCatching {
                firebaseRouteTelemetryRepository.startSession(
                    routeId = session.routeId,
                    routeName = session.routeName,
                    location = location,
                    locationAccuracyMeters = current.currentLocationAccuracyMeters,
                    plannedDistanceMeters = session.plannedDistanceMeters,
                    plannedDurationSeconds = session.plannedDurationSeconds,
                    stopCount = current.optimizedStops.size,
                    startedAtClient = session.startedAtMillis
                )
            }.onFailure { throwable ->
                showMessage(throwable.message ?: "Nao foi possivel iniciar o registro da rota.")
            }
        }
    }

    /**
     * Soma apenas deslocamentos plausiveis e envia um ponto de progresso no
     * maximo por minuto ou a cada 250 metros para conter custo e ruido de GPS.
     */
    private fun trackRouteTelemetry(location: Coordinate, accuracyMeters: Float?) {
        if (accuracyMeters != null && accuracyMeters > MAX_TELEMETRY_ACCURACY_METERS) return

        val previousSession = routeTelemetrySession
        startRouteTelemetryIfPossible()
        val session = routeTelemetrySession ?: return
        if (previousSession?.routeId != session.routeId) return

        val now = System.currentTimeMillis()
        val previousLocation = session.lastLocation ?: run {
            session.lastLocation = location
            session.lastLocationAtMillis = now
            return
        }
        val elapsedMillis = (now - session.lastLocationAtMillis).coerceAtLeast(0L)
        val segmentDistanceMeters = GeoUtils.haversineDistanceMeters(previousLocation, location)

        if (
            elapsedMillis in 1..MAX_TELEMETRY_INTERVAL_MILLIS &&
            segmentDistanceMeters <= MAX_TELEMETRY_SEGMENT_METERS
        ) {
            val elapsedSeconds = elapsedMillis / 1_000L
            if (segmentDistanceMeters >= MIN_MOVEMENT_DISTANCE_METERS) {
                session.actualDistanceMeters += segmentDistanceMeters
                session.movingDurationSeconds += elapsedSeconds
            } else {
                session.stoppedDurationSeconds += elapsedSeconds
            }
        }

        session.lastLocation = location
        session.lastLocationAtMillis = now
        session.locationSampleCount += 1
        // Chegada e saida agora sao registradas somente nos toques explicitos
        // de check-in e checkout. A telemetria continua medindo a rota toda.

        val shouldPersist =
            now - session.lastPersistedAtMillis >= TELEMETRY_PERSIST_INTERVAL_MILLIS ||
                session.actualDistanceMeters - session.lastPersistedDistanceMeters >= TELEMETRY_PERSIST_DISTANCE_METERS
        if (!shouldPersist) return

        session.lastPersistedAtMillis = now
        session.lastPersistedDistanceMeters = session.actualDistanceMeters
        val elapsedSeconds = ((now - session.startedAtMillis) / 1_000L).coerceAtLeast(0L)
        viewModelScope.launch {
            runCatching {
                firebaseRouteTelemetryRepository.recordProgress(
                    routeId = session.routeId,
                    routeName = session.routeName,
                    location = location,
                    locationAccuracyMeters = accuracyMeters,
                    actualDistanceMeters = session.actualDistanceMeters,
                    actualDurationSeconds = elapsedSeconds,
                    movingDurationSeconds = session.movingDurationSeconds,
                    stoppedDurationSeconds = session.stoppedDurationSeconds,
                    locationSampleCount = session.locationSampleCount,
                    capturedAtClient = now
                )
            }
        }
    }

    private fun finishRouteTelemetrySession(
        session: RouteTelemetrySession,
        current: VisitUiState
    ) {
        val now = System.currentTimeMillis()
        val completedStops = current.completedStopCount()
        val completionPercent = if (current.optimizedStops.isEmpty()) {
            0
        } else {
            (completedStops * 100 / current.optimizedStops.size).coerceIn(0, 100)
        }

        viewModelScope.launch {
            runCatching {
                firebaseRouteTelemetryRepository.finishSession(
                    routeId = session.routeId,
                    routeName = session.routeName,
                    location = current.currentLocation,
                    locationAccuracyMeters = current.currentLocationAccuracyMeters,
                    actualDistanceMeters = session.actualDistanceMeters,
                    actualDurationSeconds = ((now - session.startedAtMillis) / 1_000L).coerceAtLeast(0L),
                    movingDurationSeconds = session.movingDurationSeconds,
                    stoppedDurationSeconds = session.stoppedDurationSeconds,
                    locationSampleCount = session.locationSampleCount,
                    completionPercent = completionPercent,
                    finishedAtClient = now
                )
            }.onFailure { throwable ->
                showMessage(throwable.message ?: "Nao foi possivel sincronizar o resumo da rota.")
            }
        }
    }

    /** Fecha a sessao atual antes de trocar ou limpar a rota em exibicao. */
    private fun finishActiveRouteTelemetryIfNeeded() {
        val telemetrySession = routeTelemetrySession ?: return
        routeTelemetrySession = null
        finishRouteTelemetrySession(telemetrySession, _state.value)
    }

    /** Mantem a gaveta recuperavel apos recriar a tela ou o processo Android. */
    private fun observeAttendancesForActiveRoute(restoreOpenPanel: Boolean) {
        val routeId = telemetryRouteId(_state.value) ?: return
        if (observedAttendanceRouteId == routeId) return

        attendanceJob?.cancel()
        observedAttendanceRouteId = routeId
        attendanceJob = viewModelScope.launch {
            visitAttendanceRepository.observeForRoute(routeId).collect { attendances ->
                _state.update { state ->
                    state.copy(
                        attendanceHistoryByCustomer = attendances.groupBy { it.customerId },
                        activeAttendance = attendances.firstOrNull { it.isOpen }
                    )
                }
            }
        }
        if (restoreOpenPanel) {
            viewModelScope.launch {
                val openAttendance = visitAttendanceRepository.findOpenForRoute(routeId) ?: return@launch
                _state.update {
                    it.copy(
                        activeAttendance = openAttendance,
                        attendanceCustomerId = openAttendance.customerId,
                        attendancePanelMode = openAttendance.toPanelMode()
                    )
                }
            }
        }
    }

    private fun telemetryRouteId(current: VisitUiState): String? {
        return current.activeSharedRouteId
            ?: current.activeRouteId?.let(firebaseRouteTelemetryRepository::ownedRouteId)
    }

    override fun onCleared() {
        attendanceJob?.cancel()
        locationCallback?.let { fusedLocationProviderClient.removeLocationUpdates(it) }
        locationCallback = null
        super.onCleared()
    }
}

private fun VisitUiState.startNavigationMode(
    orderedStops: List<NearbyCustomer> = optimizedStops
): VisitUiState {
    if (orderedStops.isEmpty()) return this
    val existingTarget = navigationTargetCustomerId
        ?.let { customerId -> orderedStops.firstOrNull { it.customer.id == customerId } }
        ?.takeIf { stopVisitStatuses[it.customer.id] != "visited" }
    return withNavigationTarget(existingTarget ?: nextAutomaticStop())
}

/**
 * Mantem o SDK de navegacao com apenas um destino operacional por vez. Assim,
 * apos o feedback, o Mapbox recebe uma rota nova a partir do GPS atual e nao
 * tenta continuar guiando o vendedor para a primeira parada da lista antiga.
 */
private fun VisitUiState.withNavigationTarget(
    target: NearbyCustomer?,
    deferredCustomerIds: Set<Long> = deferredNavigationCustomerIds
): VisitUiState {
    val plannedOrigin = origin
    if (target == null || plannedOrigin == null) {
        return copy(
            isNavigationActive = true,
            navigationTargetCustomerId = null,
            deferredNavigationCustomerIds = deferredCustomerIds,
            navigationWaypoints = emptyList()
        )
    }
    // Durante a navegacao, o ponto de partida e sempre o GPS mais recente.
    // A origem planejada serve apenas de alternativa enquanto o aparelho ainda
    // nao entregou uma localizacao confiavel.
    val start = currentLocation ?: plannedOrigin
    return copy(
        isNavigationActive = true,
        navigationTargetCustomerId = target.customer.id,
        deferredNavigationCustomerIds = deferredCustomerIds,
        navigationWaypoints = listOf(start, target.customer.navigationCoordinate)
    )
}

/** Todas as paradas sem visita concluida, inclusive retornos necessarios. */
fun VisitUiState.pendingStops(): List<NearbyCustomer> {
    return optimizedStops.filter { stop -> stopVisitStatuses[stop.customer.id] != "visited" }
}

/**
 * A sequencia automatica inclui apenas clientes ainda sem desfecho. Clientes
 * marcados como nao visitados ficam disponiveis na lista de pendencias para
 * uma nova tentativa, sem bloquear o restante do roteiro.
 */
private fun VisitUiState.nextAutomaticStop(afterCustomerId: Long? = null): NearbyCustomer? {
    val automaticStops = optimizedStops.filter { stop ->
        stopVisitStatuses[stop.customer.id] == null &&
            stop.customer.id !in deferredNavigationCustomerIds
    }
    if (automaticStops.isEmpty()) return null
    if (afterCustomerId == null) return automaticStops.first()

    val currentIndex = automaticStops.indexOfFirst { it.customer.id == afterCustomerId }
    return if (currentIndex >= 0) {
        automaticStops.drop(currentIndex + 1).firstOrNull()
            ?: automaticStops.firstOrNull { it.customer.id != afterCustomerId }
    } else {
        automaticStops.first()
    }
}

private fun VisitUiState.advanceNavigationAfterAttendance(customerId: Long): VisitUiState {
    val nextStop = nextAutomaticStop(afterCustomerId = customerId)
    return withNavigationTarget(
        target = nextStop,
        deferredCustomerIds = deferredNavigationCustomerIds - customerId
    )
}

private fun VisitUiState.telemetryStopId(customer: Customer): String? {
    return if (activeSharedRouteId != null) {
        sharedStopIds[customer.id]
    } else {
        customer.externalId?.takeIf { it.isNotBlank() } ?: customer.id.toString()
    }
}

private fun VisitUiState.customerForAttendance(customerId: Long): Customer? {
    return optimizedStops.firstOrNull { it.customer.id == customerId }?.customer
        ?: nearbyCustomers.firstOrNull { it.customer.id == customerId }?.customer
}

private fun VisitAttendance.toPanelMode(): AttendancePanelMode {
    return when (status) {
        VisitAttendanceStatus.IN_PROGRESS -> AttendancePanelMode.IN_PROGRESS
        VisitAttendanceStatus.AWAITING_FEEDBACK -> AttendancePanelMode.POST_CHECK_OUT
        VisitAttendanceStatus.VISITED,
        VisitAttendanceStatus.NOT_VISITED -> AttendancePanelMode.RETURN_LIST
    }
}

private const val MINIMUM_FEEDBACK_LENGTH = 20
private const val MAX_TELEMETRY_ACCURACY_METERS = 75f
private const val MAX_TELEMETRY_INTERVAL_MILLIS = 120_000L
private const val MAX_TELEMETRY_SEGMENT_METERS = 2_000.0
private const val MIN_MOVEMENT_DISTANCE_METERS = 10.0
private const val TELEMETRY_PERSIST_INTERVAL_MILLIS = 60_000L
private const val TELEMETRY_PERSIST_DISTANCE_METERS = 250.0
private val NEXT_ACTION_DATE_REGEX = Regex("\\d{4}-\\d{2}-\\d{2}")

/** Estado local da sessao; os agregados sao enviados ao Firebase periodicamente. */
private data class RouteTelemetrySession(
    val routeId: String,
    val routeName: String,
    val startedAtMillis: Long,
    val plannedDistanceMeters: Double?,
    val plannedDurationSeconds: Double?,
    var lastLocation: Coordinate? = null,
    var lastLocationAtMillis: Long = startedAtMillis,
    var actualDistanceMeters: Double = 0.0,
    var movingDurationSeconds: Long = 0L,
    var stoppedDurationSeconds: Long = 0L,
    var locationSampleCount: Int = 1,
    var lastPersistedAtMillis: Long = startedAtMillis,
    var lastPersistedDistanceMeters: Double = 0.0
)

data class VisitUiState(
    val dailyRouteLimit: Int = DAILY_ROUTE_CREATION_LIMIT,
    val dailyRoutesCreated: Int = 0,
    val isRouteQuotaLoading: Boolean = true,
    val activeRouteId: Long? = null,
    val activeSharedRouteId: String? = null,
    val sharedRouteName: String? = null,
    val sharedRouteDueDate: String? = null,
    val sharedRouteTargetCompletionPercent: Int? = null,
    val sharedRouteNotes: String? = null,
    val sharedRouteStatus: String? = null,
    val sharedStopIds: Map<Long, String> = emptyMap(),
    val shouldAutoStartSharedRoute: Boolean = false,
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
    val currentLocationAccuracyMeters: Float? = null,
    val roadRoutePoints: List<Coordinate> = emptyList(),
    val roadRouteDistanceMeters: Double? = null,
    val roadRouteDurationSeconds: Double? = null,
    val routeInstructions: List<RouteInstruction> = emptyList(),
    val isNavigationActive: Boolean = false,
    val navigationWaypoints: List<Coordinate> = emptyList(),
    val navigationTargetCustomerId: Long? = null,
    val deferredNavigationCustomerIds: Set<Long> = emptySet(),
    val showPendingStopsSheet: Boolean = false,
    val stopVisitStatuses: Map<Long, String> = emptyMap(),
    val attendancePanelMode: AttendancePanelMode = AttendancePanelMode.HIDDEN,
    val attendanceCustomerId: Long? = null,
    val activeAttendance: VisitAttendance? = null,
    val attendanceHistoryByCustomer: Map<Long, List<VisitAttendance>> = emptyMap(),
    val isSavingAttendance: Boolean = false,
    val checkedInCustomerIds: Set<Long> = emptySet(),
    val isSavingStopFeedback: Boolean = false,
    val isFinishingNavigation: Boolean = false,
    val showIncompleteRouteDialog: Boolean = false,
    val shouldNavigateToHistory: Boolean = false,
    val savedFeedbackCustomerId: Long? = null,
    val isSearching: Boolean = false,
    val isGeocoding: Boolean = false,
    val isLocating: Boolean = false,
    val isSaving: Boolean = false,
    val isRouteLoading: Boolean = false,
    val message: String? = null
)

/** Apenas uma visita concluida conta para encerrar a rota como realizada. */
fun VisitUiState.completedStopCount(): Int {
    return optimizedStops.count { stop ->
        stopVisitStatuses[stop.customer.id] == "visited"
    }
}

data class FilterOptions(
    val segments: List<String> = emptyList(),
    val cities: List<String> = emptyList(),
    val states: List<String> = emptyList(),
    val statuses: List<String> = emptyList()
)
