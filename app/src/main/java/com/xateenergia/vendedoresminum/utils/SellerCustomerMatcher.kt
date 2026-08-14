package com.xateenergia.vendedoresminum.utils

import com.xateenergia.vendedoresminum.data.entities.CustomerEntity
import com.xateenergia.vendedoresminum.data.repository.SellerIdentity
import java.text.Normalizer
import java.util.Locale

/**
 * Centraliza a regra que define a carteira de cada vendedor.
 *
 * A importacao pode gravar o responsavel como nome completo ou e-mail e usa mais de um
 * nome de campo por compatibilidade com exportacoes do Odoo. Por isso a comparacao ignora
 * maiusculas, acentos e espacos extras, sem usar o estado como permissao de exibicao.
 */
object SellerCustomerMatcher {

    fun matches(customer: CustomerEntity, seller: SellerIdentity): Boolean {
        val sellerIdentifiers = sellerIdentifiers(seller)
        if (sellerIdentifiers.isEmpty()) return false

        val customerAssignees = listOf(customer.responsavel, customer.responsableSalesperson)
            .flatMap(::identityCandidates)

        return customerAssignees.any { assignee ->
            sellerIdentifiers.any { sellerIdentifier ->
                representsSamePerson(assignee, sellerIdentifier)
            }
        }
    }

    private fun sellerIdentifiers(seller: SellerIdentity): Set<String> {
        return listOf(seller.name, seller.displayName, seller.email)
            .flatMap(::identityCandidates)
            .toSet()
    }

    private fun identityCandidates(value: String?): List<String> {
        val rawValue = value?.trim().orEmpty()
        if (rawValue.isBlank()) return emptyList()

        return buildList {
            add(normalize(rawValue))
            rawValue.split(ASSIGNEE_SEPARATOR)
                .map(::normalize)
                .filter { it.isNotBlank() }
                .forEach(::add)
            EMAIL_PATTERN.findAll(rawValue)
                .map { match -> normalize(match.value) }
                .filter { it.isNotBlank() }
                .forEach(::add)
        }.distinct()
    }

    private fun representsSamePerson(first: String, second: String): Boolean {
        if (first == second) return true
        if ('@' in first || '@' in second) return false

        val firstTokens = meaningfulNameTokens(first)
        val secondTokens = meaningfulNameTokens(second)
        return firstTokens.size >= 2 &&
            secondTokens.size >= 2 &&
            firstTokens.first() == secondTokens.first() &&
            firstTokens.last() == secondTokens.last()
    }

    private fun meaningfulNameTokens(value: String): List<String> {
        return value.split(' ')
            .filter { token -> token.isNotBlank() && token !in NAME_CONNECTORS }
    }

    private fun normalize(value: String): String {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .lowercase(Locale.ROOT)
            .trim()
            .replace(WHITESPACE, " ")
    }

    private val ASSIGNEE_SEPARATOR = Regex("[,;|/\\n]+")
    private val EMAIL_PATTERN = Regex(
        """[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}""",
        RegexOption.IGNORE_CASE
    )
    private val DIACRITICS = Regex("\\p{M}+")
    private val WHITESPACE = Regex("\\s+")
    private val NAME_CONNECTORS = setOf("da", "das", "de", "do", "dos", "e")
}
