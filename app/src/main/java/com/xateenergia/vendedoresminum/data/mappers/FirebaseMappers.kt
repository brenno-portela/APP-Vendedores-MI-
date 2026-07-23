package com.xateenergia.vendedoresminum.data.mappers

import com.google.firebase.database.DataSnapshot
import com.xateenergia.vendedoresminum.data.entities.CustomerEntity
import com.xateenergia.vendedoresminum.utils.StateUtils
import java.util.Locale
import kotlin.math.absoluteValue

/**
 * Converte a entidade local de cliente para um mapa simples aceito pelo Firebase Realtime Database.
 *
 * Mantemos os nomes dos campos iguais aos da CustomerEntity para facilitar filtros, consultas
 * e compatibilidade com a estrutura esperada em customers/{id}.
 */
fun CustomerEntity.toFirebaseMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "name" to name,
        "address" to address,
        "city" to city,
        "state" to state,
        "latitude" to latitude,
        "longitude" to longitude,
        "phone" to phone,
        "segment" to segment,
        "status" to status,
        "notes" to notes,
        "importedAt" to importedAt,
        "active" to active,
        "opportunity" to opportunity,
        "cnpjCpf" to cnpjCpf,
        "externalId" to externalId,
        "email" to email,
        "responsavel" to responsavel,
        "ultimaAtualizacao" to ultimaAtualizacao,
        "distributor" to distributor,
        "responsableSalesperson" to responsableSalesperson,
        "tags" to tags,
        "expectedRevenue" to expectedRevenue,
        "origem" to origem,
        "pipelineStage" to pipelineStage,
        "clientName" to clientName,
        "country" to country
    )
}

/**
 * Converte um snapshot de customers/{id} para CustomerEntity.
 *
 * O Firebase usa chave textual, enquanto a entidade Room usa Long. Por isso, quando o campo
 * "id" nao esta gravado como numero, geramos um Long estavel a partir da chave do snapshot.
 */
fun DataSnapshot.toCustomerEntity(): CustomerEntity? {
    val firebaseKey = key.orEmpty()
    val latitude = child("latitude").asDouble() ?: return null
    val longitude = child("longitude").asDouble() ?: return null
    val name = child("name").asString()
        ?: child("clientName").asString()
        ?: child("opportunity").asString()
        ?: "Cliente $firebaseKey"

    return CustomerEntity(
        id = child("id").asLong() ?: stableLongFromKey(firebaseKey),
        name = name,
        address = child("address").asString(),
        city = child("city").asString(),
        state = StateUtils.normalizeUf(firstString("state", "uf", "estado", "clientState", "client-state", "Client - State")),
        latitude = latitude,
        longitude = longitude,
        phone = child("phone").asString(),
        segment = child("segment").asString(),
        status = child("status").asString(),
        notes = child("notes").asString(),
        importedAt = child("importedAt").asLong() ?: System.currentTimeMillis(),
        active = child("active").asBoolean() ?: true,
        opportunity = child("opportunity").asString(),
        cnpjCpf = child("cnpjCpf").asString(),
        externalId = child("externalId").asString() ?: firebaseKey.takeIf { it.isNotBlank() },
        email = child("email").asString(),
        responsavel = child("responsavel").asString(),
        ultimaAtualizacao = child("ultimaAtualizacao").asString(),
        distributor = child("distributor").asString(),
        responsableSalesperson = child("responsableSalesperson").asString(),
        tags = child("tags").asString(),
        expectedRevenue = child("expectedRevenue").asString(),
        origem = child("origem").asString(),
        pipelineStage = child("pipelineStage").asString(),
        clientName = child("clientName").asString(),
        country = child("country").asString()
    )
}

private fun DataSnapshot.asString(): String? {
    return when (val rawValue = value) {
        is String -> rawValue.trim().takeIf { it.isNotBlank() }
        is Number, is Boolean -> rawValue.toString()
        else -> null
    }
}

private fun DataSnapshot.firstString(vararg keys: String): String? {
    return keys.asSequence()
        .mapNotNull { key -> child(key).asString() }
        .firstOrNull { it.isNotBlank() }
}

private fun DataSnapshot.asDouble(): Double? {
    return when (val rawValue = value) {
        is Number -> rawValue.toDouble()
        is String -> rawValue.trim().replace(",", ".").toDoubleOrNull()
        else -> null
    }
}

private fun DataSnapshot.asLong(): Long? {
    return when (val rawValue = value) {
        is Number -> rawValue.toLong()
        is String -> rawValue.trim().toLongOrNull()
        else -> null
    }
}

private fun DataSnapshot.asBoolean(): Boolean? {
    return when (val rawValue = value) {
        is Boolean -> rawValue
        is Number -> rawValue.toInt() != 0
        is String -> when (rawValue.trim().lowercase(Locale.ROOT)) {
            "true", "sim", "s", "1", "ativo", "active" -> true
            "false", "nao", "n", "0", "inativo", "inactive" -> false
            else -> null
        }
        else -> null
    }
}

private fun stableLongFromKey(key: String): Long {
    if (key.isBlank()) return 0L

    var hash = 1125899906842597L
    key.forEach { char -> hash = 31 * hash + char.code }
    return hash.absoluteValue
}
