package com.xateenergia.vendedoresminum.di

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.functions.FirebaseFunctions
import com.xateenergia.vendedoresminum.data.dao.CustomerDao
import com.xateenergia.vendedoresminum.data.dao.PlannedRouteDao
import com.xateenergia.vendedoresminum.data.dao.VisitAttendanceDao
import com.xateenergia.vendedoresminum.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        // Usa as migracoes declaradas no banco para nao apagar um check-in em
        // andamento quando o aplicativo recebe atualizacao.
        return AppDatabase.getInstance(context)
    }

    @Provides
    fun provideCustomerDao(database: AppDatabase): CustomerDao = database.customerDao()

    @Provides
    fun providePlannedRouteDao(database: AppDatabase): PlannedRouteDao = database.plannedRouteDao()

    @Provides
    fun provideVisitAttendanceDao(database: AppDatabase): VisitAttendanceDao = database.visitAttendanceDao()

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase = FirebaseDatabase.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    /** As Cloud Functions do projeto ficam na mesma regiao do backoffice. */
    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions = FirebaseFunctions.getInstance("southamerica-east1")
}
