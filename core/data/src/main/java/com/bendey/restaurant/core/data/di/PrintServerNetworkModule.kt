package com.bendey.restaurant.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/** Cliente HTTP dedicado para impresión LAN (sin auth del backend central). */
@Module
@InstallIn(SingletonComponent::class)
object PrintServerNetworkModule {

    @Provides
    @Singleton
    @Named("printServer")
    fun providePrintServerOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(130, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
}
