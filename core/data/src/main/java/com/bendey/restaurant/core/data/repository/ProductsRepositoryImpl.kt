package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.data.imports.RestaurantProductExcelImporter
import com.bendey.restaurant.core.data.imports.RestaurantProductTemplateExporter
import com.bendey.restaurant.core.domain.catalog.BulkImportProgress
import com.bendey.restaurant.core.domain.catalog.BulkImportRow
import com.bendey.restaurant.core.domain.catalog.BulkImportRowError
import com.bendey.restaurant.core.domain.catalog.BulkImportValidationResult
import com.bendey.restaurant.core.domain.catalog.ProductImportRepository
import com.bendey.restaurant.core.domain.catalog.ProductPresentation
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.domain.products.CategoryItem
import com.bendey.restaurant.core.domain.products.IgvAffectation
import com.bendey.restaurant.core.domain.products.ProductFormInput
import com.bendey.restaurant.core.domain.products.ProductItem
import com.bendey.restaurant.core.domain.products.ProductListQuery
import com.bendey.restaurant.core.domain.products.ProductsRepository
import com.bendey.restaurant.core.domain.products.ProductDetail
import com.bendey.restaurant.core.domain.products.generateProductCode
import com.bendey.restaurant.core.network.api.ProductsApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.dto.BulkImportItemDto
import com.bendey.restaurant.core.network.dto.BulkImportRequestDto
import com.bendey.restaurant.core.network.dto.CategoryDto
import com.bendey.restaurant.core.network.dto.CategoryUpsertRequestDto
import com.bendey.restaurant.core.network.dto.CreateProductRequestDto
import com.bendey.restaurant.core.network.dto.ProductDto
import com.bendey.restaurant.core.network.dto.ProductPresentationDto
import com.bendey.restaurant.core.network.dto.UpdateProductRequestDto
import com.bendey.restaurant.core.network.error.NetworkErrorMapper
import javax.inject.Inject
import javax.inject.Singleton

private const val BULK_CHUNK_SIZE = 25

@Singleton
class ProductsRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : ProductsRepository {

    private val api: ProductsApi
        get() = tenantRetrofitProvider.create()

    override suspend fun listProducts(query: ProductListQuery): AppResult<Pair<List<ProductItem>, Int>> = apiCall {
        val response = api.listProducts(
            query = query.query,
            catalogOnly = null,
            page = query.page,
            perPage = query.perPage,
            categoryId = query.categoryId,
            preparationArea = query.preparationArea?.takeIf { it.isNotBlank() },
        )
        response.data.map { it.toDomain() } to (response.total ?: response.data.size)
    }

    override suspend fun getProduct(id: Int): AppResult<ProductItem> = apiCall {
        api.getProduct(id).data.toDomain()
    }

    override suspend fun getProductDetail(id: Int): AppResult<ProductDetail> = apiCall {
        val response = api.getProduct(id)
        ProductDetail(
            product = response.data.toDomain(),
            modifierGroupIds = response.modifierGroupIds,
            presentations = response.presentations.map { it.toDomain() },
        )
    }

    override suspend fun createProduct(input: ProductFormInput): AppResult<ProductItem> = apiCall {
        api.createProduct(input.toCreateDto()).data.toDomain()
    }

    override suspend fun updateProduct(id: Int, input: ProductFormInput): AppResult<ProductItem> = apiCall {
        api.updateProduct(id, input.toUpdateDto())
        api.getProduct(id).data.toDomain()
    }

    override suspend fun deleteProduct(id: Int): AppResult<Unit> = apiCall {
        api.deleteProduct(id)
    }

    override suspend fun listCategories(): AppResult<List<CategoryItem>> = apiCall {
        api.listCategories().data.map { it.toDomain() }
    }

    override suspend fun createCategory(name: String, description: String): AppResult<CategoryItem> = apiCall {
        api.createCategory(CategoryUpsertRequestDto(name = name.trim(), description = description.trim()))
            .data.toDomain()
    }

    override suspend fun updateCategory(
        id: Int,
        name: String,
        description: String,
    ): AppResult<CategoryItem> = apiCall {
        api.updateCategory(id, CategoryUpsertRequestDto(name = name.trim(), description = description.trim()))
            .data.toDomain()
    }

    override suspend fun deleteCategory(id: Int): AppResult<Unit> = apiCall {
        api.deleteCategory(id)
    }

    override suspend fun searchForComboEditor(query: String, page: Int): AppResult<Pair<List<ProductItem>, Int>> =
        listProducts(
            ProductListQuery(
                query = query.trim(),
                page = page,
                perPage = 25,
            ),
        )
}

@Singleton
class ProductImportRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
    private val excelImporter: RestaurantProductExcelImporter,
    private val templateExporter: RestaurantProductTemplateExporter,
) : ProductImportRepository {

    private val api: ProductsApi
        get() = tenantRetrofitProvider.create()

    override suspend fun validateExcel(bytes: ByteArray): BulkImportValidationResult =
        excelImporter.validate(bytes)

    override suspend fun importRows(
        rows: List<BulkImportRow>,
        categories: Map<String, Int>,
    ): AppResult<BulkImportProgress> = apiCall {
        var created = 0
        val failed = mutableListOf<BulkImportRowError>()
        var processed = 0

        rows.chunked(BULK_CHUNK_SIZE).forEach { chunk ->
            val payload = chunk.map { row ->
                BulkImportItemDto(
                    rowNumber = row.rowNumber,
                    name = row.name,
                    code = row.code.takeIf { it.isNotBlank() },
                    description = row.description.takeIf { it.isNotBlank() },
                    salePrice = row.salePrice,
                    unit = row.unit,
                    categoryName = row.categoryName.takeIf { it.isNotBlank() },
                    igvAffectationType = row.igvAffectationType,
                    priceIncludesIgv = row.priceIncludesIgv,
                    manageStock = row.manageStock,
                    initialStock = row.initialStock.takeIf { it > 0 },
                    preparationArea = row.preparationArea.takeIf { it.isNotBlank() },
                )
            }
            val response = api.bulkImportRestaurant(BulkImportRequestDto(items = payload))
            created += response.data.created
            failed += response.data.failed.map {
                BulkImportRowError(row = it.row, column = "import", message = it.error)
            }
            processed += chunk.size
        }

        BulkImportProgress(
            totalRows = rows.size,
            processedRows = processed,
            created = created,
            failed = failed,
        )
    }

    override fun generateTemplateBytes(): ByteArray = templateExporter.generateBytes()
}

private inline fun <T> apiCall(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: Exception) {
    val mapped = NetworkErrorMapper.map(e)
    AppResult.Error(mapped.message ?: "Error de conexión", mapped)
}

private fun ProductDto.toDomain() = ProductItem(
    id = id,
    code = code,
    name = name,
    description = description?.takeIf { it.isNotBlank() },
    salePrice = salePrice,
    purchasePrice = purchasePrice,
    unit = unit,
    categoryId = categoryId,
    categoryName = categoryName?.takeIf { it.isNotBlank() },
    preparationArea = preparationArea?.takeIf { it.isNotBlank() },
    imageUrl = imageUrl?.takeIf { it.isNotBlank() },
    manageStock = manageStock,
    hasModifiers = hasModifiers,
    hasVariants = hasVariants,
    availableForSale = availableForSale,
    igvAffectationType = igvAffectationType ?: "10",
    priceIncludesIgv = priceIncludesIgv ?: true,
    active = active,
)

private fun CategoryDto.toDomain() = CategoryItem(
    id = id,
    name = name,
    description = description?.takeIf { it.isNotBlank() },
)

private fun ProductPresentationDto.toDomain() = ProductPresentation(
    id = id,
    name = name,
    salePrice = salePrice,
    description = description.orEmpty(),
    sortOrder = sortOrder ?: 0,
    active = active ?: true,
)

private fun ProductPresentation.toDto() = ProductPresentationDto(
    id = id,
    name = name.trim(),
    salePrice = salePrice,
    description = description.takeIf { it.isNotBlank() },
    sortOrder = sortOrder,
    active = active,
)

private fun ProductFormInput.toCreateDto(): CreateProductRequestDto {
    val salePrice = salePrice.replace(",", ".").toDoubleOrNull()
        ?: throw IllegalArgumentException("Precio de venta inválido")
    val purchase = purchasePrice.replace(",", ".").toDoubleOrNull()?.takeIf { it > 0 } ?: 0.0
    val initial = initialStock.replace(",", ".").toDoubleOrNull()?.takeIf { it > 0 }
    val activePresentations = presentations.filter { it.name.trim().isNotEmpty() }
    return CreateProductRequestDto(
        name = name.trim(),
        code = code.trim().ifBlank { generateProductCode() },
        description = description.trim(),
        salePrice = salePrice,
        purchasePrice = purchase,
        categoryId = categoryId,
        preparationArea = preparationArea.apiValue,
        igvAffectationType = igvAffectation.code,
        priceIncludesIgv = if (IgvAffectation.isGravado(igvAffectation.code)) priceIncludesIgv else false,
        manageStock = manageStock,
        initialStock = if (manageStock) initial else null,
        hasModifiers = hasModifiers || modifierGroupIds.isNotEmpty(),
        hasVariants = hasVariants || activePresentations.isNotEmpty(),
        modifierGroupIds = modifierGroupIds,
        presentations = activePresentations.map { it.toDto() },
        availableForSale = availableForSale,
    )
}

private fun ProductFormInput.toUpdateDto(): UpdateProductRequestDto {
    val activePresentations = presentations.filter { it.name.trim().isNotEmpty() }
    return UpdateProductRequestDto(
        name = name.trim(),
        code = code.trim().ifBlank { null },
        description = description.trim(),
        salePrice = salePrice.replace(",", ".").toDoubleOrNull(),
        purchasePrice = purchasePrice.replace(",", ".").toDoubleOrNull()?.takeIf { it > 0 },
        categoryId = categoryId,
        preparationArea = preparationArea.apiValue,
        igvAffectationType = igvAffectation.code,
        priceIncludesIgv = if (IgvAffectation.isGravado(igvAffectation.code)) priceIncludesIgv else false,
        manageStock = manageStock,
        availableForSale = availableForSale,
        isRestaurant = true,
        hasModifiers = hasModifiers || modifierGroupIds.isNotEmpty(),
        hasVariants = hasVariants || activePresentations.isNotEmpty(),
        modifierGroupIds = modifierGroupIds,
        presentations = activePresentations.map { it.toDto() },
    )
}
