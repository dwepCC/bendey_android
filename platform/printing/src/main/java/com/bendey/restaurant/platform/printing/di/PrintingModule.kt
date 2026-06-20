package com.bendey.restaurant.platform.printing.di

import com.bendey.restaurant.platform.printing.transport.AndroidPrinterTransport
import com.bendey.restaurant.platform.printing.transport.PrinterRepository
import com.bendey.restaurant.platform.printing.transport.PrinterRepositoryImpl
import com.bendey.restaurant.platform.printing.transport.PrinterTransport
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PrintingModule {

    @Binds
    @Singleton
    abstract fun bindPrinterTransport(impl: AndroidPrinterTransport): PrinterTransport

    companion object {
        @Provides
        @Singleton
        fun providePrinterRepository(transport: PrinterTransport): PrinterRepository =
            PrinterRepositoryImpl(transport)
    }
}
