package com.bendey.restaurant.core.domain.products

import com.bendey.restaurant.core.domain.model.AppResult

interface ProductsRepository {
    suspend fun listProducts(query: ProductListQuery): AppResult<Pair<List<ProductItem>, Int>>
    suspend fun getProduct(id: Int): AppResult<ProductItem>
    suspend fun getProductDetail(id: Int): AppResult<ProductDetail>
    suspend fun createProduct(input: ProductFormInput): AppResult<ProductItem>
    suspend fun updateProduct(id: Int, input: ProductFormInput): AppResult<ProductItem>
    suspend fun deleteProduct(id: Int): AppResult<Unit>

    suspend fun listCategories(): AppResult<List<CategoryItem>>
    suspend fun createCategory(name: String, description: String = ""): AppResult<CategoryItem>
    suspend fun updateCategory(id: Int, name: String, description: String = ""): AppResult<CategoryItem>
    suspend fun deleteCategory(id: Int): AppResult<Unit>
    suspend fun searchForComboEditor(query: String, page: Int = 1): AppResult<Pair<List<ProductItem>, Int>>
    suspend fun getStockSummary(productIds: List<Int>): AppResult<Map<Int, Double>>
}
