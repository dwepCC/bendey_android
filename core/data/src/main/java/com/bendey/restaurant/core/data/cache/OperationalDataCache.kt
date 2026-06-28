package com.bendey.restaurant.core.data.cache

import com.bendey.restaurant.core.domain.billing.CheckoutMeta
import com.bendey.restaurant.core.domain.catalog.BranchItem
import com.bendey.restaurant.core.domain.catalog.CompanyConfig
import com.bendey.restaurant.core.domain.catalog.RestaurantSettings
import com.bendey.restaurant.core.domain.catalog.SunatConfig
import com.bendey.restaurant.core.domain.restaurant.ProductCategory
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class CachedTenantSettings(
    val config: CompanyConfig,
    val sunat: SunatConfig,
    val settings: RestaurantSettings,
    val branches: List<BranchItem>,
)

/**
 * Caché en memoria de datos operativos estables (series, contactos, métodos de pago, categorías).
 * Equivalente a refs/contextos de React (`BranchCheckoutSeriesContext`, `posCheckoutMetaLoadedRef`).
 */
@Singleton
class OperationalDataCache @Inject constructor() {
    private val checkoutMetaByBranch = ConcurrentHashMap<Int, CheckoutMeta>()
    @Volatile
    private var categories: List<ProductCategory>? = null
    @Volatile
    private var tenantSettings: CachedTenantSettings? = null

    fun getCheckoutMeta(branchId: Int): CheckoutMeta? = checkoutMetaByBranch[branchId]

    fun setCheckoutMeta(branchId: Int, meta: CheckoutMeta) {
        checkoutMetaByBranch[branchId] = meta
    }

    fun getCategories(): List<ProductCategory>? = categories

    fun setCategories(value: List<ProductCategory>) {
        categories = value
    }

    fun clearCheckoutMeta(branchId: Int? = null) {
        if (branchId == null) checkoutMetaByBranch.clear()
        else checkoutMetaByBranch.remove(branchId)
    }

    fun getTenantSettings(): CachedTenantSettings? = tenantSettings

    fun setTenantSettings(value: CachedTenantSettings) {
        tenantSettings = value
    }

    fun updateCompanyConfig(config: CompanyConfig) {
        tenantSettings = tenantSettings?.copy(config = config)
    }

    fun updateSunatConfig(sunat: SunatConfig) {
        tenantSettings = tenantSettings?.copy(sunat = sunat)
    }

    fun updateRestaurantSettings(settings: RestaurantSettings) {
        tenantSettings = tenantSettings?.copy(settings = settings)
    }

    fun updateBranches(branches: List<BranchItem>) {
        tenantSettings = tenantSettings?.copy(branches = branches)
    }

    fun clearTenantSettings() {
        tenantSettings = null
    }

    fun clearAll() {
        checkoutMetaByBranch.clear()
        categories = null
        tenantSettings = null
    }
}
