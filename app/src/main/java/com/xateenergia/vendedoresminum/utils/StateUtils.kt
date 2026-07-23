package com.xateenergia.vendedoresminum.utils

import java.text.Normalizer
import java.util.Locale

object StateUtils {
    private val stateNamesToUf = mapOf(
        "ACRE" to "AC",
        "ALAGOAS" to "AL",
        "AMAPA" to "AP",
        "AMAZONAS" to "AM",
        "BAHIA" to "BA",
        "CEARA" to "CE",
        "DISTRITO FEDERAL" to "DF",
        "ESPIRITO SANTO" to "ES",
        "GOIAS" to "GO",
        "MARANHAO" to "MA",
        "MATO GROSSO" to "MT",
        "MATO GROSSO DO SUL" to "MS",
        "MINAS GERAIS" to "MG",
        "PARA" to "PA",
        "PARAIBA" to "PB",
        "PARANA" to "PR",
        "PERNAMBUCO" to "PE",
        "PIAUI" to "PI",
        "RIO DE JANEIRO" to "RJ",
        "RIO GRANDE DO NORTE" to "RN",
        "RIO GRANDE DO SUL" to "RS",
        "RONDONIA" to "RO",
        "RORAIMA" to "RR",
        "SANTA CATARINA" to "SC",
        "SAO PAULO" to "SP",
        "SERGIPE" to "SE",
        "TOCANTINS" to "TO"
    )

    /**
     * Padroniza estados brasileiros para UF, aceitando tanto "MS" quanto
     * "Mato Grosso do Sul". Isso evita que vendedores fiquem sem clientes
     * quando a planilha/backoffice envia nomes completos ou valores com acento.
     */
    fun normalizeUf(value: String?): String? {
        val normalized = value
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.withoutAccents()
            ?.uppercase(Locale.ROOT)
            ?: return null

        if (normalized.length == 2) return normalized

        val ufFromToken = "\\b[A-Z]{2}\\b".toRegex()
            .findAll(normalized)
            .map { it.value }
            .firstOrNull { it in stateNamesToUf.values }
        if (ufFromToken != null) return ufFromToken

        val exactUf = stateNamesToUf[normalized]
        if (exactUf != null) return exactUf

        return stateNamesToUf.entries
            .sortedByDescending { it.key.length }
            .firstOrNull { (stateName, _) -> normalized.contains(stateName) }
            ?.value
            ?: normalized
    }

    private fun String.withoutAccents(): String {
        return Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
    }
}
