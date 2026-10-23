package com.bendey.restaurant.core.data.di

import com.bendey.restaurant.core.data.onboarding.OnboardingRepositoryImpl
import com.bendey.restaurant.core.data.onboarding.WizardPreferencesStore
import com.bendey.restaurant.core.data.session.StationPreferencesStore
import com.bendey.restaurant.core.domain.waiter.StationPreferences
import com.bendey.restaurant.core.data.repository.AuthRepositoryImpl
import com.bendey.restaurant.core.data.repository.BillingRepositoryImpl
import com.bendey.restaurant.core.data.repository.CashRepositoryImpl
import com.bendey.restaurant.core.data.repository.CombosRepositoryImpl
import com.bendey.restaurant.core.data.repository.ContactsRepositoryImpl
import com.bendey.restaurant.core.data.repository.DashboardRepositoryImpl
import com.bendey.restaurant.core.data.repository.DeliveryRepositoryImpl
import com.bendey.restaurant.core.data.repository.InventoryRepositoryImpl
import com.bendey.restaurant.core.data.repository.KitchenRepositoryImpl
import com.bendey.restaurant.core.data.repository.PendingApprovalRepositoryImpl
import com.bendey.restaurant.core.domain.pendingapproval.PendingApprovalRepository
import com.bendey.restaurant.core.data.repository.MesasRepositoryImpl
import com.bendey.restaurant.core.data.repository.ModifiersRepositoryImpl
import com.bendey.restaurant.core.data.repository.PreparationAreasRepositoryImpl
import com.bendey.restaurant.core.data.repository.PosRepositoryImpl
import com.bendey.restaurant.core.data.repository.ProfileRepositoryImpl
import com.bendey.restaurant.core.data.repository.PurchasesRepositoryImpl
import com.bendey.restaurant.core.data.repository.ProductImageRepositoryImpl
import com.bendey.restaurant.core.data.repository.ProductImportRepositoryImpl
import com.bendey.restaurant.core.data.repository.ProductionRepositoryImpl
import com.bendey.restaurant.core.data.repository.SubscriptionRepositoryImpl
import com.bendey.restaurant.core.data.repository.ProductsRepositoryImpl
import com.bendey.restaurant.core.data.repository.SalesRepositoryImpl
import com.bendey.restaurant.core.data.repository.DigitalMenuRepositoryImpl
import com.bendey.restaurant.core.data.repository.MarketplaceRepositoryImpl
import com.bendey.restaurant.core.data.repository.ManualPaymentRepositoryImpl
import com.bendey.restaurant.core.data.repository.DeliveryReportRepositoryImpl
import com.bendey.restaurant.core.domain.delivery.DeliveryReportRepository
import com.bendey.restaurant.core.data.repository.SettlementRepositoryImpl
import com.bendey.restaurant.core.domain.delivery.ManualPaymentRepository
import com.bendey.restaurant.core.domain.delivery.SettlementRepository
import com.bendey.restaurant.core.domain.marketplace.MarketplaceRepository
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
import com.bendey.restaurant.core.domain.catalog.PreparationAreasRepository
import com.bendey.restaurant.core.domain.catalog.ProductImageRepository
import com.bendey.restaurant.core.domain.catalog.ProductImportRepository
import com.bendey.restaurant.core.domain.catalog.SettingsRepository
import com.bendey.restaurant.core.domain.digitalmenu.DigitalMenuRepository
import com.bendey.restaurant.core.domain.contacts.ContactsRepository
import com.bendey.restaurant.core.domain.dashboard.DashboardRepository
import com.bendey.restaurant.core.domain.inventory.InventoryRepository
import com.bendey.restaurant.core.domain.onboarding.OnboardingRepository
import com.bendey.restaurant.core.domain.onboarding.wizard.WizardPreferences
import com.bendey.restaurant.core.domain.restaurant.KitchenRepository
import com.bendey.restaurant.core.domain.restaurant.MesasRepository
import com.bendey.restaurant.core.domain.restaurant.PosRepository
import com.bendey.restaurant.core.domain.production.ProductionRepository
import com.bendey.restaurant.core.domain.profile.ProfileRepository
import com.bendey.restaurant.core.domain.subscription.SubscriptionRepository
import com.bendey.restaurant.core.domain.products.ProductsRepository
import com.bendey.restaurant.core.domain.purchases.PurchasesRepository
import com.bendey.restaurant.core.domain.sales.SalesRepository
import com.bendey.restaurant.core.data.session.SessionExpiryCoordinator
import com.bendey.restaurant.core.domain.session.UserSessionStore
import com.bendey.restaurant.core.network.session.NetworkSessionProvider
import com.bendey.restaurant.core.network.session.SessionExpiryReporter
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
    abstract fun bindSessionExpiryReporter(impl: SessionExpiryCoordinator): SessionExpiryReporter

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
    abstract fun bindInventoryRepository(impl: InventoryRepositoryImpl): InventoryRepository

    @Binds
    @Singleton
    abstract fun bindProductStockRepository(
        impl: com.bendey.restaurant.core.data.repository.ProductStockRepositoryImpl,
    ): com.bendey.restaurant.core.domain.products.ProductStockRepository

    @Binds
    @Singleton
    abstract fun bindSoldReportRepository(
        impl: com.bendey.restaurant.core.data.repository.SoldReportRepositoryImpl,
    ): com.bendey.restaurant.core.domain.sales.SoldReportRepository

    @Binds
    @Singleton
    abstract fun bindInventoryCountRepository(
        impl: com.bendey.restaurant.core.data.repository.InventoryCountRepositoryImpl,
    ): com.bendey.restaurant.core.domain.inventory.InventoryCountRepository

    @Binds
    @Singleton
    abstract fun bindTipsRepository(
        impl: com.bendey.restaurant.core.data.repository.TipsRepositoryImpl,
    ): com.bendey.restaurant.core.domain.tips.TipsRepository

    @Binds
    @Singleton
    abstract fun bindRcShareRepository(
        impl: com.bendey.restaurant.core.data.repository.RcShareRepositoryImpl,
    ): com.bendey.restaurant.core.domain.rcshare.RcShareRepository

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
    abstract fun bindPendingApprovalRepository(impl: PendingApprovalRepositoryImpl): PendingApprovalRepository

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
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindPurchasesRepository(impl: PurchasesRepositoryImpl): PurchasesRepository

    @Binds
    @Singleton
    abstract fun bindModifiersRepository(impl: ModifiersRepositoryImpl): ModifiersRepository

    @Binds
    @Singleton
    abstract fun bindPreparationAreasRepository(impl: PreparationAreasRepositoryImpl): PreparationAreasRepository

    @Binds
    @Singleton
    abstract fun bindCombosRepository(impl: CombosRepositoryImpl): CombosRepository

    @Binds
    @Singleton
    abstract fun bindProductionRepository(impl: ProductionRepositoryImpl): ProductionRepository

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(impl: SubscriptionRepositoryImpl): SubscriptionRepository

    @Binds
    @Singleton
    abstract fun bindDeliveryRepository(impl: DeliveryRepositoryImpl): DeliveryRepository

    @Binds
    @Singleton
    abstract fun bindDigitalMenuRepository(impl: DigitalMenuRepositoryImpl): DigitalMenuRepository

    @Binds
    @Singleton
    abstract fun bindMarketplaceRepository(impl: MarketplaceRepositoryImpl): MarketplaceRepository

    @Binds
    @Singleton
    abstract fun bindSettlementRepository(impl: SettlementRepositoryImpl): SettlementRepository

    @Binds
    @Singleton
    abstract fun bindManualPaymentRepository(impl: ManualPaymentRepositoryImpl): ManualPaymentRepository

    @Binds
    @Singleton
    abstract fun bindDeliveryZonesRepository(impl: com.bendey.restaurant.core.data.repository.DeliveryZonesRepositoryImpl): com.bendey.restaurant.core.domain.delivery.DeliveryZonesRepository

    @Binds
    @Singleton
    abstract fun bindDeliveryEarningsRepository(impl: com.bendey.restaurant.core.data.repository.DeliveryEarningsRepositoryImpl): com.bendey.restaurant.core.domain.delivery.DeliveryEarningsRepository

    @Binds
    @Singleton
    abstract fun bindDeliveryReportRepository(impl: DeliveryReportRepositoryImpl): DeliveryReportRepository

    @Binds
    @Singleton
    abstract fun bindOpsRepository(impl: com.bendey.restaurant.core.data.repository.OpsRepositoryImpl): com.bendey.restaurant.core.domain.ops.OpsRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindProductImportRepository(impl: ProductImportRepositoryImpl): ProductImportRepository

    @Binds
    @Singleton
    abstract fun bindProductImageRepository(impl: ProductImageRepositoryImpl): ProductImageRepository

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(impl: OnboardingRepositoryImpl): OnboardingRepository

    @Binds
    @Singleton
    abstract fun bindWizardPreferences(impl: WizardPreferencesStore): WizardPreferences

    @Binds
    @Singleton
    abstract fun bindStationPreferences(impl: StationPreferencesStore): StationPreferences
}
