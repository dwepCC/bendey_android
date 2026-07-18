package com.bendey.restaurant.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MenuSocialLinksDto(
    val instagram: String? = null,
    val facebook: String? = null,
    val tiktok: String? = null,
    val whatsapp: String? = null,
)

@Serializable
data class MenuConfigDto(
    @SerialName("welcome_title") val welcomeTitle: String? = null,
    @SerialName("welcome_description") val welcomeDescription: String? = null,
    @SerialName("show_prices") val showPrices: Boolean = true,
    @SerialName("default_branch_id") val defaultBranchId: Int? = null,
    @SerialName("social_links") val socialLinks: MenuSocialLinksDto? = null,
    @SerialName("map_url") val mapUrl: String? = null,
    @SerialName("public_takeaway_enabled") val publicTakeawayEnabled: Boolean = false,
    @SerialName("public_delivery_enabled") val publicDeliveryEnabled: Boolean = false,
    @SerialName("theme_mode") val themeMode: String? = null,
    @SerialName("primary_color_hex") val primaryColorHex: String? = null,
    @SerialName("background_image_base64") val backgroundImageBase64: String? = null,
    @SerialName("style_variant") val styleVariant: String? = null,
)

@Serializable
data class StaffMenuSettingsDto(
    @SerialName("menu_enabled") val menuEnabled: Boolean = false,
    @SerialName("menu_public_token") val menuPublicToken: String = "",
    @SerialName("menu_url") val menuUrl: String = "",
    @SerialName("menu_config") val menuConfig: MenuConfigDto = MenuConfigDto(),
)

@Serializable
data class StaffMenuSettingsUpdateDto(
    @SerialName("menu_enabled") val menuEnabled: Boolean,
    @SerialName("menu_config") val menuConfig: MenuConfigDto,
)

@Serializable
data class StaffMenuSettingsUpdateResponseDto(
    val success: Boolean = true,
    val data: StaffMenuSettingsDto,
)

@Serializable
data class MenuTokenRegenerateDataDto(
    @SerialName("menu_public_token") val menuPublicToken: String,
    @SerialName("menu_url") val menuUrl: String,
)

@Serializable
data class MenuTokenRegenerateResponseDto(
    val success: Boolean = true,
    val data: MenuTokenRegenerateDataDto,
)

@Serializable
data class PublicationChannelDto(
    val channel: String,
    val enabled: Boolean,
)

@Serializable
data class ProductPublicationChannelsDto(
    @SerialName("product_id") val productId: Int,
    val channels: List<PublicationChannelDto> = emptyList(),
)

@Serializable
data class ProductPublicationChannelsUpdateDto(
    val channels: List<PublicationChannelDto>,
)

@Serializable
data class ProductPublicationChannelsUpdateResponseDto(
    val success: Boolean = true,
    val data: ProductPublicationChannelsDto,
)

@Serializable
data class TableMenuQrDto(
    @SerialName("table_id") val tableId: Int,
    @SerialName("table_name") val tableName: String = "",
    @SerialName("public_token") val publicToken: String = "",
    @SerialName("menu_url") val menuUrl: String = "",
    @SerialName("qr_png_base64") val qrPngBase64: String? = null,
)

@Serializable
data class TableMenuQrRotateResponseDto(
    val success: Boolean = true,
    val data: TableMenuQrDto,
)
