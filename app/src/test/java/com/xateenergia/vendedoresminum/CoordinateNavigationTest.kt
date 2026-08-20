package com.xateenergia.vendedoresminum

import com.xateenergia.vendedoresminum.domain.model.Customer
import org.junit.Assert.assertEquals
import org.junit.Test

class CoordinateNavigationTest {

    @Test
    fun `navigationCoordinate usa navigationLatitude e navigationLongitude quando presentes`() {
        val customer = Customer(
            id = 1,
            name = "Empresa Teste",
            latitude = -20.49134,
            longitude = -54.64875,
            navigationLatitude = -20.43175,
            navigationLongitude = -54.65722
        )

        val navCoord = customer.navigationCoordinate
        assertEquals(-20.43175, navCoord.latitude, 0.00001)
        assertEquals(-54.65722, navCoord.longitude, 0.00001)
    }

    @Test
    fun `navigationCoordinate usa latitude e longitude simples de fallback quando navigationLatitude eh nulo`() {
        val customer = Customer(
            id = 2,
            name = "Empresa Antiga",
            latitude = -20.49134,
            longitude = -54.64875,
            navigationLatitude = null,
            navigationLongitude = null
        )

        val navCoord = customer.navigationCoordinate
        assertEquals(-20.49134, navCoord.latitude, 0.00001)
        assertEquals(-54.64875, navCoord.longitude, 0.00001)
    }
}
