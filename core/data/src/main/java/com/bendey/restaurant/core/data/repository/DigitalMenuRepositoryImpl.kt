package com.bendey.restaurant.core.data.repository

import com.bendey.restaurant.core.domain.digitalmenu.BENDEY_OFFICIAL_COLOR_HEX
import com.bendey.restaurant.core.domain.digitalmenu.DigitalMenuRepository
import com.bendey.restaurant.core.domain.digitalmenu.MenuConfig
import com.bendey.restaurant.core.domain.digitalmenu.MenuStyleVariant
import com.bendey.restaurant.core.domain.digitalmenu.MenuThemeMode
import com.bendey.restaurant.core.domain.digitalmenu.PublicationChannel
import com.bendey.restaurant.core.domain.digitalmenu.StaffMenuSettings
import com.bendey.restaurant.core.domain.digitalmenu.TableMenuQr
import com.bendey.restaurant.core.domain.model.AppResult
import com.bendey.restaurant.core.network.api.DigitalMenuApi
import com.bendey.restaurant.core.network.client.TenantRetrofitProvider
import com.bendey.restaurant.core.network.dto.MenuConfigDto
import com.bendey.restaurant.core.network.dto.MenuSocialLinksDto
import com.bendey.restaurant.core.network.dto.ProductPublicationChannelsUpdateDto
import com.bendey.restaurant.core.network.dto.PublicationChannelDto
import com.bendey.restaurant.core.network.dto.StaffMenuSettingsDto
import com.bendey.restaurant.core.network.dto.StaffMenuSettingsUpdateDto
import com.bendey.restaurant.core.network.dto.TableMenuQrDto
import com.bendey.restaurant.core.network.error.NetworkErrorMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DigitalMenuRepositoryImpl @Inject constructor(
    private val tenantRetrofitProvider: TenantRetrofitProvider,
) : DigitalMenuRepository {

    private val api: DigitalMenuApi
        get() = tenantRetrofitProvider.create()

    override suspend fun getSettings(): AppResult<StaffMenuSettings> = apiCall {
        api.getMenuSettings().toDomain()
    }

    override suspend fun updateSettings(menuEnabled: Boolean, config: MenuConfig): AppResult<StaffMenuSettings> =
        apiCall {
            api.updateMenuSettings(
                StaffMenuSettingsUpdateDto(
                    menuEnabled = menuEnabled,
                    menuConfig = config.toDto(),
                ),
            ).data.toDomain()
        }

    override suspend fun regenerateMenuToken(): AppResult<Pair<String, String>> = apiCall {
        val data = api.regenerateMenuToken().data
        data.menuPublicToken to data.menuUrl
    }

    override suspend fun getProductPublicationChannels(productId: Int): AppResult<List<PublicationChannel>> =
        apiCall {
            api.getProductPublicationChannels(productId).channels.map { it.toDomain() }
        }

    override suspend fun setMenuChannelEnabled(productId: Int, enabled: Boolean): AppResult<Unit> = apiCall {
        api.setProductPublicationChannels(
            productId,
            ProductPublicationChannelsUpdateDto(
                channels = listOf(PublicationChannelDto(channel = "MENU", enabled = enabled)),
            ),
        )
    }

    override suspend fun getTableMenuQr(tableId: Int, includePng: Boolean): AppResult<TableMenuQr> = apiCall {
        api.getTableMenuQr(tableId, png = if (includePng) 1 else null).toDomain()
    }

    override suspend fun rotateTableMenuToken(tableId: Int): AppResult<TableMenuQr> = apiCall {
        api.rotateTableMenuToken(tableId).data.toDomain()
    }

    private inline fun <T> apiCall(block: () -> T): AppResult<T> = try {
        AppResult.Success(block())
    } catch (e: Exception) {
        AppResult.Error(NetworkErrorMapper.map(e).message ?: "Error de conexión", cause = e)
    }
}

private val HEX_COLOR_REGEX = Regex("^#[0-9A-Fa-f]{6}$")

private fun StaffMenuSettingsDto.toDomain() = StaffMenuSettings(
    menuEnabled = menuEnabled,
    menuPublicToken = menuPublicToken,
    menuUrl = menuUrl,
    menuConfig = MenuConfig(
        welcomeTitle = menuConfig.welcomeTitle.orEmpty(),
        welcomeDescription = menuConfig.welcomeDescription.orEmpty(),
        showPrices = menuConfig.showPrices,
        whatsapp = menuConfig.socialLinks?.whatsapp.orEmpty(),
        publicTakeawayEnabled = menuConfig.publicTakeawayEnabled,
        publicDeliveryEnabled = menuConfig.publicDeliveryEnabled,
        themeMode = if (menuConfig.themeMode == "custom") MenuThemeMode.CUSTOM else MenuThemeMode.BENDEY_DEFAULT,
        primaryColorHex = menuConfig.primaryColorHex?.takeIf { HEX_COLOR_REGEX.matches(it) } ?: BENDEY_OFFICIAL_COLOR_HEX,
        backgroundImageBase64 = menuConfig.backgroundImageBase64.orEmpty(),
        styleVariant = if (menuConfig.styleVariant == "solid") MenuStyleVariant.SOLID else MenuStyleVariant.GLASS,
    ),
)

private fun MenuConfig.toDto() = MenuConfigDto(
    welcomeTitle = welcomeTitle.ifBlank { null },
    welcomeDescription = welcomeDescription.ifBlank { null },
    showPrices = showPrices,
    socialLinks = if (whatsapp.isNotBlank()) MenuSocialLinksDto(whatsapp = whatsapp) else null,
    publicTakeawayEnabled = publicTakeawayEnabled,
    publicDeliveryEnabled = publicDeliveryEnabled,
    themeMode = if (themeMode == MenuThemeMode.CUSTOM) "custom" else "bendey_default",
    primaryColorHex = primaryColorHex.ifBlank { null },
    backgroundImageBase64 = backgroundImageBase64.ifBlank { null },
    styleVariant = if (styleVariant == MenuStyleVariant.SOLID) "solid" else "glass",
)

private fun PublicationChannelDto.toDomain() = PublicationChannel(channel = channel, enabled = enabled)

private fun TableMenuQrDto.toDomain() = TableMenuQr(
    tableId = tableId,
    tableName = tableName,
    publicToken = publicToken,
    menuUrl = menuUrl,
    qrPngBase64 = qrPngBase64,
)
