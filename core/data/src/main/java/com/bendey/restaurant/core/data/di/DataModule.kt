package com.bendey.restaurant.core.data.di

import com.bendey.restaurant.core.data.repository.AuthRepositoryImpl
import com.bendey.restaurant.core.data.repository.BillingRepositoryImpl
import com.bendey.restaurant.core.data.repository.CashRepositoryImpl
import com.bendey.restaurant.core.data.repository.CombosRepositoryImpl
import com.bendey.restaurant.core.data.repository.ContactsRepositoryImpl
import com.bendey.restaurant.core.data.repository.DashboardRepositoryImpl
import com.bendey.restaurant.core.data.repository.DeliveryRepositoryImpl
import com.bendey.restaurant.core.data.repository.KitchenRepositoryImpl
import com.bendey.restaurant.core.data.repository.MesasRepositoryImpl
import com.bendey.restaurant.core.data.repository.ModifiersRepositoryImpl
import com.bendey.restaurant.core.data.repository.PosRepositoryImpl
import com.bendey.restaurant.core.data.repository.ProductImageRepositoryImpl
import com.bendey.restaurant.core.data.repository.ProductImportRepositoryImpl
import com.bendey.restaurant.core.data.repository.ProductsRepositoryImpl
import com.bendey.restaurant.core.data.repository.SalesRepositoryImpl
import com.bendey.restaurant.core.data.repository.SettingsRepositoryImpl
import com.bendey.restaurant.core.data.repository.TenantRepositoryImpl
import com.bendey.restaurant.core.data.session.SessionManager
import com.bendey.restaurant.core.domain.auth.AuthRepository
import com.bendey.restaurant.core.domain.auth.TenantRepository
import com.bendey.restaurant.core.domain.billing.BillingRepository
import com.bendey.restaurant.core.domain.cash.CashRepository
import com.bendey.restaurant.core.domain.catalog.CombosRepository
import com.bendey.restaurant.core.domain.catalog.DeliveryRepository
import com.bendey.restaurant.core.domain.catalog.ModifiersRepository
import com.bendey.restaurant.core.domain.catalog.ProductImageRepository
import com.bendey.restaurant.core.domain.catalog.ProductImportRepository
import com.bendey.restaurant.core.domain.catalog.SettingsRepository
import com.bendey.restaurant.core.domain.contacts.ContactsRepository
import com.bendey.restaurant.core.domain.dashboard.DashboardRepository
import com.bendey.restaurant.core.domain.restaurant.KitchenRepository
import com.bendey.restaurant.core.domain.restaurant.MesasRepository
import com.bendey.restaurant.core.domain.restaurant.PosRepository
import com.bendey.restaurant.core.domain.products.ProductsRepository
import com.bendey.restaurant.core.domain.sales.SalesRepository
import com.bendey.restaurant.core.domain.session.UserSessionStore
import com.bendey.restaurant.core.network.session.NetworkSessionProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindNetworkSessionProvider(impl: SessionManager): NetworkSessionProvider

    @Binds
    @Singleton
    abstract fun bindUserSessionStore(impl: SessionManager): UserSessionStore

    @Binds
    @Singleton
    abstract fun bindTenantRepository(impl: TenantRepositoryImpl): TenantRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindPosRepository(impl: PosRepositoryImpl): PosRepository

    @Binds
    @Singleton
    abstract fun bindMesasRepository(impl: MesasRepositoryImpl): MesasRepository

    @Binds
    @Singleton
    abstract fun bindKitchenRepository(impl: KitchenRepositoryImpl): KitchenRepository

    @Binds
    @Singleton
    abstract fun bindCashRepository(impl: CashRepositoryImpl): CashRepository

    @Binds
    @Singleton
    abstract fun bindBillingRepository(impl: BillingRepositoryImpl): BillingRepository

    @Binds
    @Singleton
    abstract fun bindSalesRepository(impl: SalesRepositoryImpl): SalesRepository

    @Binds
    @Singleton
    abstract fun bindProductsRepository(impl: ProductsRepositoryImpl): ProductsRepository

    @Binds
    @Singleton
    abstract fun bindContactsRepository(impl: ContactsRepositoryImpl): ContactsRepository

    @Binds
    @Singleton
    abstract fun bindModifiersRepository(impl: ModifiersRepositoryImpl): ModifiersRepository

    @Binds
    @Singleton
    abstract fun bindCombosRepository(impl: CombosRepositoryImpl): CombosRepository

    @Binds
    @Singleton
    abstract fun bindDeliveryRepository(impl: DeliveryRepositoryImpl): DeliveryRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindProductImportRepository(impl: ProductImportRepositoryImpl): ProductImportRepository

    @Binds
    @Singleton
    abstract fun bindProductImageRepository(impl: ProductImageRepositoryImpl): ProductImageRepository
}
