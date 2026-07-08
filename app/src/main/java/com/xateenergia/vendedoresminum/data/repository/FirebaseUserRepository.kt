package com.xateenergia.vendedoresminum.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Singleton
class FirebaseUserRepository @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val firebaseAuth: FirebaseAuth
) {
    /**
     * Le o estado do vendedor logado em users/{uid}/state.
     *
     * Se nao houver usuario autenticado ou se o perfil nao tiver estado, retornamos null para
     * impedir que a Home carregue clientes de outro estado por engano.
     */
    suspend fun getCurrentUserState(): String? = withContext(Dispatchers.IO) {
        val uid = firebaseAuth.currentUser?.uid ?: return@withContext null

        firebaseDatabase
            .getReference("users")
            .child(uid)
            .child("state")
            .get()
            .await()
            .getValue(String::class.java)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.uppercase(Locale.ROOT)
    }
}
