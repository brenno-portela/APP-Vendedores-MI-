package com.xateenergia.vendedoresminum.data.repository

/**
 * Dados usados para relacionar o vendedor autenticado aos clientes que lhe foram atribuidos.
 *
 * O backoffice salva nome, e-mail e apelido de exibicao no perfil. Mantemos todos para
 * continuar compativel com os cadastros antigos e com importacoes que usam o e-mail como
 * responsavel.
 */
data class SellerIdentity(
    val uid: String,
    val name: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val state: String? = null
) {
    val displayLabel: String
        get() = name?.takeIf { it.isNotBlank() }
            ?: displayName?.takeIf { it.isNotBlank() }
            ?: email?.takeIf { it.isNotBlank() }
            ?: "vendedor"

    fun hasAssignmentIdentifier(): Boolean {
        return listOf(name, displayName, email).any { !it.isNullOrBlank() }
    }
}
