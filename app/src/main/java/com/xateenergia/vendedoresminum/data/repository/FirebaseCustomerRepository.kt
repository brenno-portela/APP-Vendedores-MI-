package com.xateenergia.vendedoresminum.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.xateenergia.vendedoresminum.data.entities.CustomerEntity
import com.xateenergia.vendedoresminum.data.mappers.toCustomerEntity
import com.xateenergia.vendedoresminum.data.mappers.toFirebaseMap
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Singleton
class FirebaseCustomerRepository @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase
) {
    private val customersRef = firebaseDatabase.getReference("customers")

    /**
     * Observa clientes em tempo real filtrando pelo estado do vendedor.
     *
     * A consulta usa orderByChild("state").equalTo(state), entao o campo state precisa existir
     * em cada customers/{id} e deve estar no mesmo formato salvo no perfil do usuario.
     */
    fun observeCustomersForState(state: String): Flow<List<CustomerEntity>> = callbackFlow {
        val normalizedState = state.trim().uppercase(Locale.ROOT)
        val query = customersRef.orderByChild("state").equalTo(normalizedState)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val customers = snapshot.children.mapNotNull { child ->
                    child.toCustomerEntity()
                }
                trySend(customers)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    /**
     * Busca um cliente diretamente pela chave textual usada em customers/{key}.
     */
    suspend fun getCustomerById(key: String): CustomerEntity? = withContext(Dispatchers.IO) {
        customersRef.child(key).get().await().toCustomerEntity()
    }

    /**
     * Salva um cliente no Firebase. Quando externalId existe, ele vira a chave principal;
     * caso contrario usamos o id local e, se ele ainda nao existir, criamos uma chave nova.
     */
    suspend fun saveCustomer(customer: CustomerEntity): Unit = withContext(Dispatchers.IO) {
        val key = customer.externalId
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: customer.id.takeIf { it > 0L }?.toString()
            ?: customersRef.push().key
            ?: error("Nao foi possivel gerar uma chave para o cliente.")

        customersRef.child(key).setValue(customer.toFirebaseMap()).await()
    }

    /**
     * Salva varios clientes em uma unica atualizacao atomica no no customers.
     */
    suspend fun saveCustomers(customers: List<CustomerEntity>): Unit = withContext(Dispatchers.IO) {
        if (customers.isEmpty()) return@withContext

        val updates = customers.associate { customer ->
            val key = customer.externalId
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: customer.id.takeIf { it > 0L }?.toString()
                ?: customersRef.push().key
                ?: error("Nao foi possivel gerar uma chave para o cliente.")

            key to customer.toFirebaseMap()
        }

        customersRef.updateChildren(updates).await()
    }

    /**
     * Remove todos os clientes do no customers.
     */
    suspend fun deleteAll(): Unit = withContext(Dispatchers.IO) {
        customersRef.removeValue().await()
    }
}
