package com.bendey.restaurant.core.realtime.di

import com.bendey.restaurant.core.realtime.RealtimeClient
import com.bendey.restaurant.core.realtime.StubRealtimeClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RealtimeModule {

    @Provides
    @Singleton
    fun provideRealtimeClient(): RealtimeClient = StubRealtimeClient()
}
