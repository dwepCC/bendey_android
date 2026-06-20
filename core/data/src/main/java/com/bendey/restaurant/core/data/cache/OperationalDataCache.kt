package com.bendey.restaurant.core.data.cache

import com.bendey.restaurant.core.domain.billing.CheckoutMeta
import com.bendey.restaurant.core.domain.restaurant.ProductCategory
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Caché en memoria de datos operativos estables (series, contactos, métodos de pago, categorías).
 * Equivalente a refs/contextos de React (`BranchCheckoutSeriesContext`, `posCheckoutMetaLoadedRef`).
 */
@Singleton
class OperationalDataCache @Inject constructor() {
    private val checkoutMetaByBranch = ConcurrentHashMap<Int, CheckoutMeta>()
    @Volatile
    private var categories: List<ProductCategory>? = null

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

    fun clearAll() {
        checkoutMetaByBranch.clear()
        categories = null
    }
}
