package com.xateenergia.vendedoresminum.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.xateenergia.vendedoresminum.utils.StateUtils
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
     * Carrega a identidade que relaciona o vendedor autenticado aos clientes da sua carteira.
     *
     * Nome, displayName e e-mail sao mantidos porque as planilhas podem usar qualquer uma
     * dessas formas no campo Responsavel.
     */
    suspend fun getCurrentSellerIdentity(): SellerIdentity? = withContext(Dispatchers.IO) {
        val currentUser = firebaseAuth.currentUser ?: return@withContext null
        val profile = firebaseDatabase
            .getReference("users")
            .child(currentUser.uid)
            .get()
            .await()

        SellerIdentity(
            uid = currentUser.uid,
            name = profile.firstNonBlankString("name"),
            displayName = profile.firstNonBlankString("displayName", "display_name")
                ?: currentUser.displayName?.trim()?.takeIf { it.isNotBlank() },
            email = profile.firstNonBlankString("email")
                ?: currentUser.email?.trim()?.takeIf { it.isNotBlank() },
            state = StateUtils.normalizeUf(profile.firstNonBlankString("state"))
        )
    }

    /**
     * Mantem compatibilidade com pontos do app que ainda precisam apenas da UF do perfil.
     */
    suspend fun getCurrentUserState(): String? {
        return getCurrentSellerIdentity()?.state
    }

    private fun DataSnapshot.firstNonBlankString(vararg keys: String): String? {
        return keys.asSequence()
            .mapNotNull { key -> child(key).getValue(String::class.java) }
            .map(String::trim)
            .firstOrNull { value -> value.isNotBlank() }
    }
}
