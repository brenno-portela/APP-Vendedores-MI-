package com.xateenergia.vendedoresminum

import android.app.Application
import com.mapbox.common.MapboxOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SalesRouteApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val token = getString(R.string.mapbox_access_token)
        if (token.isNotBlank()) {
            MapboxOptions.accessToken = token
        }

        MapboxNavigationApp.setup(
            NavigationOptions.Builder(this)
                .build()
        )
    }
}

