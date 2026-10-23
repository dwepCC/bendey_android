package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.products.PresentationStock
import com.bendey.restaurant.core.domain.products.ProductStock
import com.bendey.restaurant.core.domain.products.ProductStockRepository
import com.bendey.restaurant.core.network.api.ProductStockApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.error.apiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductStockRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : ProductStockRepository {
    private val api: ProductStockApi
        get() = tenantRetrofitProvider.create()

    override suspend fun stock(productId: Int): AppResult<ProductStock> = apiCall {
        val d = api.stock(productId).data
        ProductStock(
            total = d?.total ?: 0.0,
            minStock = d?.minStock ?: 0.0,
            presentations = d?.presentations.orEmpty()
                .filter { it.active != false }
                .map { PresentationStock(it.presentationId ?: 0, it.name.orEmpty(), it.quantity ?: 0.0, it.minStock ?: 0.0) },
        )
    }
}
