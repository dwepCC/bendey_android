package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.catalog.BranchItem
import com.bendey.restaurant.core.domain.catalog.ComboBranchSetting
import com.bendey.restaurant.core.domain.catalog.ComboFixedItem
import com.bendey.restaurant.core.domain.catalog.ComboFormInput
import com.bendey.restaurant.core.domain.catalog.ComboItem
import com.bendey.restaurant.core.domain.catalog.ComboSlot
import com.bendey.restaurant.core.domain.catalog.ComboSlotOption
import com.bendey.restaurant.core.domain.catalog.ComboType
import com.bendey.restaurant.core.domain.catalog.buildComboSaveSlots
import com.bendey.restaurant.core.domain.catalog.normalizeComboDateInput
import com.bendey.restaurant.core.domain.catalog.showPromoDates
import com.bendey.restaurant.core.domain.catalog.usesFixed
import com.bendey.restaurant.core.domain.catalog.CombosRepository
import com.bendey.restaurant.core.domain.catalog.CompanyConfig
import com.bendey.restaurant.core.domain.catalog.CompanyConfigFormInput
import com.bendey.restaurant.core.domain.catalog.DeliveryCompany
import com.bendey.restaurant.core.domain.catalog.DeliveryCompanyFormInput
import com.bendey.restaurant.core.domain.catalog.DeliveryDriver
import com.bendey.restaurant.core.domain.catalog.DeliveryDriverFormInput
import com.bendey.restaurant.core.domain.catalog.DeliveryRepository
import com.bendey.restaurant.core.domain.catalog.ModifierGroup
import com.bendey.restaurant.core.domain.catalog.ModifierGroupFormInput
import com.bendey.restaurant.core.domain.catalog.ModifierOption
import com.bendey.restaurant.core.domain.catalog.ModifierSelectionMode
import com.bendey.restaurant.core.domain.catalog.ModifiersRepository
import com.bendey.restaurant.core.domain.catalog.RestaurantSettings
import com.bendey.restaurant.core.domain.catalog.SettingsRepository
import com.bendey.restaurant.core.domain.catalog.SunatConfig
import com.bendey.restaurant.core.domain.catalog.SunatConfigFormInput
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.network.api.CombosApi
import com.bendey.restaurant.core.network.api.DeliveryApi
import com.bendey.restaurant.core.network.api.ModifierGroupsApi
import com.bendey.restaurant.core.network.api.SettingsApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.dto.ComboBranchSettingDto
import com.bendey.restaurant.core.network.dto.ComboDto
import com.bendey.restaurant.core.network.dto.ComboFixedItemDto
import com.bendey.restaurant.core.network.dto.ComboSlotDto
import com.bendey.restaurant.core.network.dto.ComboSlotOptionDto
import com.bendey.restaurant.core.network.dto.ComboUpsertRequestDto
import com.bendey.restaurant.core.network.dto.CompanyConfigDto
import com.bendey.restaurant.core.network.dto.DeliveryCompanyDto
import com.bendey.restaurant.core.network.dto.DeliveryCompanyUpsertRequestDto
import com.bendey.restaurant.core.network.dto.DeliveryDriverDto
import com.bendey.restaurant.core.network.dto.DeliveryDriverUpsertRequestDto
import com.bendey.restaurant.core.network.dto.ModifierGroupDto
import com.bendey.restaurant.core.network.dto.ModifierGroupUpsertRequestDto
import com.bendey.restaurant.core.network.dto.ModifierOptionDto
import com.bendey.restaurant.core.network.dto.RestaurantSettingsUpdateRequestDto
import com.bendey.restaurant.core.network.error.NetworkErrorMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModifiersRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : ModifiersRepository {

    private val api: ModifierGroupsApi
        get() = tenantRetrofitProvider.create()

    override suspend fun listModifierGroups(): AppResult<List<ModifierGroup>> = catalogApiCall {
        api.listModifierGroups().data.map { it.toDomain() }
    }

    override suspend fun createModifierGroup(input: ModifierGroupFormInput): AppResult<ModifierGroup> =
        catalogApiCall {
            api.createModifierGroup(input.toDto()).group.toDomain()
        }

    override suspend fun updateModifierGroup(id: Int, input: ModifierGroupFormInput): AppResult<ModifierGroup> =
        catalogApiCall {
            api.updateModifierGroup(id, input.toDto()).group.toDomain()
        }

    override suspend fun deleteModifierGroup(id: Int): AppResult<Unit> = catalogApiCall {
        api.deleteModifierGroup(id)
    }
}

@Singleton
class CombosRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : CombosRepository {

    private val api: CombosApi
        get() = tenantRetrofitProvider.create()

    override suspend fun listCombos(): AppResult<List<ComboItem>> = catalogApiCall {
        api.listCombos().data.map { it.toListItem() }
    }

    override suspend fun getCombo(id: Int): AppResult<ComboFormInput> = catalogApiCall {
        api.getCombo(id).data.toFormInput()
    }

    override suspend fun createCombo(input: ComboFormInput): AppResult<ComboItem> = catalogApiCall {
        api.createCombo(input.toDto()).data.toListItem()
    }

    override suspend fun updateCombo(id: Int, input: ComboFormInput): AppResult<ComboItem> = catalogApiCall {
        api.updateCombo(id, input.toDto()).data.toListItem()
    }

    override suspend fun deleteCombo(id: Int): AppResult<Unit> = catalogApiCall {
        api.deleteCombo(id)
    }
}

@Singleton
class DeliveryRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : DeliveryRepository {

    private val api: DeliveryApi
        get() = tenantRetrofitProvider.create()

    override suspend fun listDrivers(): AppResult<List<DeliveryDriver>> = catalogApiCall {
        api.listDeliveryDrivers().data.map { it.toDomain() }
    }

    override suspend fun createDriver(input: DeliveryDriverFormInput): AppResult<Unit> = catalogApiCall {
        api.createDeliveryDriver(input.toDto())
    }

    override suspend fun updateDriver(id: Int, input: DeliveryDriverFormInput): AppResult<Unit> = catalogApiCall {
        api.updateDeliveryDriver(id, input.toDto())
    }

    override suspend fun deleteDriver(id: Int): AppResult<Unit> = catalogApiCall {
        api.deleteDeliveryDriver(id)
    }

    override suspend fun listCompanies(): AppResult<List<DeliveryCompany>> = catalogApiCall {
        api.listDeliveryCompanies().data.map { it.toDomain() }
    }

    override suspend fun createCompany(input: DeliveryCompanyFormInput): AppResult<Unit> = catalogApiCall {
        api.createDeliveryCompany(DeliveryCompanyUpsertRequestDto(name = input.name.trim()))
    }

    override suspend fun updateCompany(id: Int, name: String, active: Boolean): AppResult<Unit> = catalogApiCall {
        api.updateDeliveryCompany(id, DeliveryCompanyUpsertRequestDto(name = name.trim(), active = active))
    }

    override suspend fun deleteCompany(id: Int): AppResult<Unit> = catalogApiCall {
        api.deleteDeliveryCompany(id)
    }
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : SettingsRepository {

    private val api: SettingsApi
        get() = tenantRetrofitProvider.create()

    override suspend fun getCompanyConfig(): AppResult<CompanyConfig> = catalogApiCall {
        api.getCompanyConfig().toDomain()
    }

    override suspend fun updateCompanyConfig(input: CompanyConfigFormInput): AppResult<CompanyConfig> =
        catalogApiCall {
            val current = api.getCompanyConfig()
            api.updateCompanyConfig(
                current.copy(
                    businessName = input.businessName.trim().ifBlank { current.businessName },
                    tradeName = input.tradeName.trim().ifBlank { current.tradeName },
                    address = input.address.trim().ifBlank { current.address },
                    phone = input.phone.trim().ifBlank { current.phone },
                    email = input.email.trim().ifBlank { current.email },
                ),
            ).data.toDomain()
        }

    override suspend fun getSunatConfig(): AppResult<SunatConfig> = catalogApiCall {
        api.getSunatConfig().toDomain()
    }

    override suspend fun updateSunatConfig(input: SunatConfigFormInput): AppResult<SunatConfig> = catalogApiCall {
        val current = api.getSunatConfig()
        val taxRate = input.taxRate.replace(",", ".").toDoubleOrNull() ?: current.taxRate
        api.updateSunatConfig(
            current.copy(
                taxRate = taxRate,
                igvRegime = input.igvRegime.trim().ifBlank { current.igvRegime },
                taxBenefitZone = input.taxBenefitZone,
            ),
        )
        api.getSunatConfig().toDomain()
    }

    override suspend fun listBranches(): AppResult<List<BranchItem>> = catalogApiCall {
        api.listBranches().data.map {
            BranchItem(
                id = it.id,
                name = it.name,
                address = it.address,
                phone = it.phone,
                isMain = it.isMain,
                active = it.active ?: true,
            )
        }
    }

    override suspend fun getRestaurantSettings(): AppResult<RestaurantSettings> = catalogApiCall {
        RestaurantSettings(hasDeletionPin = api.getRestaurantSettings().hasDeletionPin)
    }

    override suspend fun updateDeletionPin(pin: String): AppResult<Unit> = catalogApiCall {
        api.updateRestaurantSettings(RestaurantSettingsUpdateRequestDto(deletionPin = pin))
    }
}

private inline fun <T> catalogApiCall(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: Exception) {
    val mapped = NetworkErrorMapper.map(e)
    AppResult.Error(mapped.message ?: "Error de conexión", mapped)
}

private fun ModifierGroupDto.toDomain() = ModifierGroup(
    id = id,
    name = name,
    required = required,
    selectionMode = ModifierSelectionMode.fromApi(
        selectionMode ?: if (multiSelect == true) "multiple" else "single",
    ),
    minSelect = minSelect ?: 0,
    maxSelect = maxSelect ?: 0,
    options = options.map {
        ModifierOption(id = it.id, name = it.name, extraPrice = it.extraPrice)
    },
)

private fun ModifierGroupFormInput.toDto(): ModifierGroupUpsertRequestDto {
    val min = minSelect.toIntOrNull() ?: 0
    val max = maxSelect.toIntOrNull() ?: 0
    return ModifierGroupUpsertRequestDto(
        name = name.trim(),
        required = required,
        multiSelect = selectionMode != ModifierSelectionMode.SINGLE,
        minSelect = min,
        maxSelect = max,
        selectionMode = selectionMode.apiValue,
        options = options
            .filter { it.name.trim().isNotEmpty() }
            .map { ModifierOptionDto(id = it.id, name = it.name.trim(), extraPrice = it.extraPrice) },
    )
}

private fun ComboDto.toListItem() = ComboItem(
    id = id,
    name = name,
    description = description,
    comboType = ComboType.fromApi(comboType),
    basePrice = basePrice,
    active = active,
    fixedItemsCount = fixedItems.size,
    slotsCount = slots.size,
    validFrom = validFrom,
    validTo = validTo,
)

private fun ComboDto.toFormInput() = ComboFormInput(
    name = name,
    description = description.orEmpty(),
    comboType = ComboType.fromApi(comboType),
    basePrice = basePrice.toString(),
    active = active,
    validFrom = toDateInput(validFrom),
    validTo = toDateInput(validTo),
    fixedItems = fixedItems.map {
        ComboFixedItem(
            id = it.id,
            productId = it.productId,
            presentationId = it.presentationId,
            quantity = it.quantity,
        )
    }.ifEmpty { listOf(ComboFixedItem(productId = 0)) },
    slots = slots.map { slot ->
        ComboSlot(
            id = slot.id,
            name = slot.name,
            minPick = slot.minPick,
            maxPick = slot.maxPick,
            options = slot.options.map { option ->
                ComboSlotOption(
                    id = option.id,
                    productId = option.productId,
                    presentationId = option.presentationId,
                    quantity = option.quantity,
                    upgradePrice = option.upgradePrice,
                )
            }.ifEmpty { listOf(ComboSlotOption(productId = 0)) },
        )
    },
    branchSettings = branchSettings.map {
        ComboBranchSetting(
            branchId = it.branchId,
            active = it.active,
            priceOverride = it.priceOverride?.toString().orEmpty(),
        )
    },
)

private fun ComboFormInput.toDto(): ComboUpsertRequestDto {
    val price = basePrice.replace(",", ".").toDoubleOrNull()
        ?: throw IllegalArgumentException("Precio base inválido")
    val fixedPayload = fixedItems
        .filter { it.productId > 0 }
        .mapIndexed { index, item ->
            ComboFixedItemDto(
                id = item.id,
                productId = item.productId,
                presentationId = item.presentationId,
                quantity = if (item.quantity > 0) item.quantity else 1.0,
                sortOrder = index,
            )
        }
    val slotsPayload = buildComboSaveSlots(comboType, slots)
        .filter { it.name.trim().isNotEmpty() }
        .mapIndexed { slotIndex, slot ->
            ComboSlotDto(
                id = slot.id,
                name = slot.name.trim(),
                minPick = slot.minPick.coerceAtLeast(1),
                maxPick = slot.maxPick.coerceAtLeast(1),
                sortOrder = slotIndex,
                options = slot.options
                    .filter { it.productId > 0 }
                    .mapIndexed { optionIndex, option ->
                        ComboSlotOptionDto(
                            id = option.id,
                            productId = option.productId,
                            presentationId = option.presentationId,
                            quantity = if (option.quantity > 0) option.quantity else 1.0,
                            upgradePrice = option.upgradePrice,
                            sortOrder = optionIndex,
                        )
                    },
            )
        }
    return ComboUpsertRequestDto(
        name = name.trim(),
        description = description.trim().ifBlank { null },
        comboType = comboType.apiValue,
        basePrice = price,
        active = active,
        validFrom = if (comboType.showPromoDates()) normalizeComboDateInput(validFrom) else null,
        validTo = if (comboType.showPromoDates()) normalizeComboDateInput(validTo) else null,
        fixedItems = if (usesFixed()) fixedPayload else emptyList(),
        slots = slotsPayload,
        branchSettings = branchSettings.map {
            ComboBranchSettingDto(
                branchId = it.branchId,
                active = it.active,
                priceOverride = it.priceOverride.replace(",", ".").toDoubleOrNull(),
            )
        },
    )
}

private fun toDateInput(value: String?): String {
    val raw = value?.trim().orEmpty()
    if (raw.length >= 10) return raw.take(10)
    return raw
}

private fun DeliveryDriverDto.toDomain() = DeliveryDriver(
    id = id,
    name = name,
    phone = phone.orEmpty(),
    vehicleType = vehicleType.orEmpty(),
    plate = plate.orEmpty(),
    active = active,
    notes = notes.orEmpty(),
    deliveryCompanyId = deliveryCompanyId,
    deliveryCompanyName = deliveryCompany?.name,
)

private fun DeliveryDriverFormInput.toDto() = DeliveryDriverUpsertRequestDto(
    name = name.trim(),
    phone = phone.trim().ifBlank { null },
    vehicleType = vehicleType.trim().ifBlank { null },
    plate = plate.trim().ifBlank { null },
    notes = notes.trim().ifBlank { null },
    active = active,
    deliveryCompanyId = deliveryCompanyId,
)

private fun DeliveryCompanyDto.toDomain() = DeliveryCompany(
    id = id,
    name = name,
    active = active,
)

private fun CompanyConfigDto.toDomain() = CompanyConfig(
    businessName = businessName,
    tradeName = tradeName,
    ruc = ruc,
    address = address,
    phone = phone,
    email = email,
    currency = currency,
    logoUrl = logoUrl,
)

private fun com.bendey.restaurant.core.network.dto.SunatConfigDto.toDomain() = SunatConfig(
    sunatEnabled = sunatEnabled,
    taxRate = taxRate,
    igvRegime = igvRegime,
    taxBenefitZone = taxBenefitZone,
)
