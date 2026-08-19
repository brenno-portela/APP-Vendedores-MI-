package com.xateenergia.vendedoresminum.di

import com.google.android.gms.location.FusedLocationProviderClient
import com.xateenergia.vendedoresminum.shared.location.AndroidFusedLocationProvider
import com.xateenergia.vendedoresminum.shared.location.LocationProvider
import com.xateenergia.vendedoresminum.shared.location.PlatformLocationProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SharedModule {
    @Provides
    @Singleton
    fun provideSharedLocationProvider(
        fusedLocationProviderClient: FusedLocationProviderClient
    ): LocationProvider {
        return AndroidFusedLocationProvider(fusedLocationProviderClient)
    }

    @Provides
    @Singleton
    fun providePlatformLocationProvider(
        fusedLocationProviderClient: FusedLocationProviderClient
    ): PlatformLocationProvider {
        return AndroidFusedLocationProvider(fusedLocationProviderClient)
    }
}
