package com.xateenergia.vendedoresminum.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.xateenergia.vendedoresminum.data.dao.CustomerDao
import com.xateenergia.vendedoresminum.data.database.AppDatabase
import com.xateenergia.vendedoresminum.data.entities.CustomerEntity
import com.xateenergia.vendedoresminum.domain.model.Customer
import com.xateenergia.vendedoresminum.domain.model.CustomerFilters
import com.xateenergia.vendedoresminum.utils.BoundingBox
import com.xateenergia.vendedoresminum.utils.GeoUtils
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class CustomerRepository @Inject constructor(
    private val database: AppDatabase,
    private val customerDao: CustomerDao
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val firebaseDatabase = FirebaseDatabase.getInstance()
    private val syncState = MutableStateFlow(CustomerSyncState())

    init {
        startFirebaseSync()
    }

    fun observeSyncState(): Flow<CustomerSyncState> = syncState

    fun observeCount(): Flow<Int> = customerDao.observeCount()

    fun observeCustomers(query: String?): Flow<List<Customer>> {
        val normalizedQuery = query?.trim()?.takeIf { it.isNotBlank() }
        return customerDao.observeCustomers(normalizedQuery).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getById(id: Long): Customer? {
        return customerDao.getById(id)?.toDomain()
    }

    suspend fun getCandidates(box: BoundingBox, filters: CustomerFilters): List<Customer> {
        return customerDao.getCandidatesInBoundingBox(
            minLatitude = box.minLatitude,
            maxLatitude = box.maxLatitude,
            minLongitude = box.minLongitude,
            maxLongitude = box.maxLongitude,
            onlyActive = filters.onlyActive,
            onlyWithPhone = filters.onlyWithPhone,
            segment = filters.segment,
            city = filters.city,
            state = filters.state,
            status = filters.status
        ).map { it.toDomain() }
    }

    suspend fun deleteAll() {
        database.withTransaction {
            customerDao.deleteAll()
        }
    }

    fun observeSegments(): Flow<List<String>> = customerDao.observeSegments()
    fun observeCities(): Flow<List<String>> = customerDao.observeCities()
    fun observeStates(): Flow<List<String>> = customerDao.observeStates()
    fun observeStatuses(): Flow<List<String>> = customerDao.observeStatuses()

    private fun startFirebaseSync() {
        repositoryScope.launch {
            observeCurrentUid()
                .distinctUntilChanged()
                .flatMapLatest { uid ->
                    if (uid == null) {
                        syncState.value = CustomerSyncState()
                        flowOf(null)
                    } else {
                        observeUserState(uid)
                    }
                }
                .flatMapLatest { userState ->
                    when {
                        userState == null -> flowOf(emptyList())
                        userState.isBlank() -> {
                            syncState.value = CustomerSyncState(
                                isLoading = false,
                                userState = null,
                                message = "Seu perfil nao possui um estado associado. Contate o administrador."
                            )
                            flowOf(emptyList())
                        }
                        else -> observeCustomersFromFirebase(userState)
                    }
                }
                .collect { customers ->
                    database.withTransaction {
                        customerDao.deleteAll()
                        if (customers.isNotEmpty()) {
                            customerDao.insertAll(customers)
                        }
                    }
                }
        }
    }

    private fun observeCurrentUid(): Flow<String?> = callbackFlow {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }

        auth.addAuthStateListener(listener)
        trySend(auth.currentUser?.uid)

        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private fun observeUserState(uid: String): Flow<String?> = callbackFlow {
        val userRef = firebaseDatabase.getReference("users").child(uid).child("state")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val state = snapshot.getValue(String::class.java)?.trim()?.uppercase(Locale.ROOT)
                trySend(state)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Erro ao observar estado do usuario", error.toException())
                syncState.value = CustomerSyncState(
                    isLoading = false,
                    message = "Nao foi possivel carregar o estado do seu perfil."
                )
                close(error.toException())
            }
        }

        userRef.addValueEventListener(listener)
        awaitClose { userRef.removeEventListener(listener) }
    }

    private fun observeCustomersFromFirebase(userState: String): Flow<List<CustomerEntity>> = callbackFlow {
        val normalizedUserState = userState.trim().uppercase(Locale.ROOT)
        val customersRef = firebaseDatabase.getReference("customers")

        syncState.value = CustomerSyncState(
            isLoading = true,
            userState = normalizedUserState,
            message = null
        )

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val importedAt = System.currentTimeMillis()
                val customers = snapshot.children
                    .mapNotNull { child -> child.toCustomerEntity(importedAt) }
                    .filter { entity ->
                        entity.state?.trim()?.uppercase(Locale.ROOT) == normalizedUserState
                    }

                syncState.value = CustomerSyncState(
                    isLoading = false,
                    userState = normalizedUserState,
                    message = null
                )
                trySend(customers)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Erro ao sincronizar clientes", error.toException())
                syncState.value = CustomerSyncState(
                    isLoading = false,
                    userState = normalizedUserState,
                    message = "Sem conexao com o Firebase. Mostrando dados salvos no aparelho."
                )
            }
        }

        customersRef.addValueEventListener(listener)
        awaitClose { customersRef.removeEventListener(listener) }
    }

    private fun DataSnapshot.toCustomerEntity(importedAt: Long): CustomerEntity? {
        val key = key.orEmpty()
        val latitude = firstDouble("latitude", "lat", "coordLat", "coordinateLatitude")
        val longitude = firstDouble("longitude", "lng", "lon", "long", "coordLong", "coordinateLongitude")

        if (latitude == null || longitude == null || !GeoUtils.isValidCoordinate(latitude, longitude)) {
            Log.w(TAG, "Cliente ignorado por coordenadas invalidas: $key")
            return null
        }

        val opportunity = firstString("opportunity", "oportunidade")
        val clientName = firstString("clientName", "client-name", "client_name", "Client - Name")
        val name = firstString("name", "nome", "cliente", "empresa") ?: opportunity ?: clientName ?: "Cliente $key"
        val status = firstString("status", "situacao", "ativo") ?: "Ativo"

        return CustomerEntity(
            id = stableIdFor(key),
            name = name,
            address = firstString("address", "endereco", "dealAddress", "deal-address", "Deal - Address"),
            city = firstString("city", "cidade", "municipio", "Client - City"),
            state = firstString("state", "uf", "estado", "clientState", "client-state", "Client - State")
                ?.uppercase(Locale.ROOT),
            latitude = latitude,
            longitude = longitude,
            phone = firstString("phone", "telefone", "celular", "whatsapp", "clientPhone", "client-phone"),
            segment = firstString("segment", "segmento", "dealSegment", "deal-segment", "Deal - Segment"),
            status = status,
            notes = firstString("notes", "observacoes", "obs", "dealNotes", "deal-notes", "Deal - Notes"),
            importedAt = importedAt,
            active = status.isActiveStatus(),
            opportunity = opportunity,
            cnpjCpf = firstString("cnpjCpf", "cnpj", "cpf", "documento"),
            externalId = firstString("externalId", "external_id", "id").takeUnless { it == key } ?: key,
            email = firstString("email", "clientEmail", "client-email", "Client - Email"),
            responsavel = firstString("responsavel", "responsable", "owner"),
            ultimaAtualizacao = firstString("ultimaAtualizacao", "updatedAt", "updated_at"),
            distributor = firstString("distributor", "dealDistributor", "deal-distributor"),
            responsableSalesperson = firstString(
                "responsableSalesperson",
                "responsibleSalesperson",
                "salesperson",
                "vendedor",
                "deal-responsablesalesperson"
            ),
            tags = firstString("tags", "dealTags", "deal-tags"),
            expectedRevenue = firstString("expectedRevenue", "dealExpectedRevenue", "deal-expectedrevenue"),
            origem = firstString("origem", "fonte", "dealOrigem", "deal-origem"),
            pipelineStage = firstString("pipelineStage", "pipeline-stage", "stage", "dealPipelineStage"),
            clientName = clientName,
            country = firstString("country", "pais")
        )
    }

    private fun DataSnapshot.firstString(vararg keys: String): String? {
        return keys.asSequence()
            .mapNotNull { key -> child(key).value }
            .mapNotNull { value ->
                when (value) {
                    is String -> value.trim()
                    is Number, is Boolean -> value.toString()
                    else -> null
                }
            }
            .firstOrNull { it.isNotBlank() }
    }

    private fun DataSnapshot.firstDouble(vararg keys: String): Double? {
        return keys.asSequence()
            .mapNotNull { key -> child(key).value }
            .mapNotNull { value ->
                when (value) {
                    is Number -> value.toDouble()
                    is String -> value.trim().replace(",", ".").toDoubleOrNull()
                    else -> null
                }
            }
            .firstOrNull()
    }

    private fun String.isActiveStatus(): Boolean {
        return trim().lowercase(Locale.ROOT) !in setOf(
            "inativo",
            "inactive",
            "cancelado",
            "cancelada",
            "bloqueado",
            "nao",
            "n",
            "false",
            "0"
        )
    }

    private fun stableIdFor(key: String): Long {
        var hash = 1125899906842597L
        key.forEach { char -> hash = 31 * hash + char.code }
        return hash and Long.MAX_VALUE
    }

    companion object {
        private const val TAG = "CustomerRepository"
    }
}

data class CustomerSyncState(
    val isLoading: Boolean = false,
    val userState: String? = null,
    val message: String? = null
)
