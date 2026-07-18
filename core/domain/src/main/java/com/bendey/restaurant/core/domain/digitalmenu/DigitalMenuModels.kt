package com.bendey.restaurant.core.domain.digitalmenu

data class MenuSocialLinks(
    val instagram: String = "",
    val facebook: String = "",
    val tiktok: String = "",
    val whatsapp: String = "",
)

enum class MenuThemeMode { BENDEY_DEFAULT, CUSTOM }

enum class MenuStyleVariant { GLASS, SOLID }

data class MenuConfig(
    val welcomeTitle: String = "",
    val welcomeDescription: String = "",
    val showPrices: Boolean = true,
    val whatsapp: String = "",
    val publicTakeawayEnabled: Boolean = false,
    val publicDeliveryEnabled: Boolean = false,
    val themeMode: MenuThemeMode = MenuThemeMode.BENDEY_DEFAULT,
    val primaryColorHex: String = BENDEY_OFFICIAL_COLOR_HEX,
    val backgroundImageBase64: String = "",
    val styleVariant: MenuStyleVariant = MenuStyleVariant.GLASS,
)

/** Tema oficial Bendey Resto — usado por defecto en toda carta digital sin personalizar. */
const val BENDEY_OFFICIAL_COLOR_HEX = "#C9393B"

data class StaffMenuSettings(
    val menuEnabled: Boolean,
    val menuPublicToken: String,
    val menuUrl: String,
    val menuConfig: MenuConfig,
)

data class PublicationChannel(
    val channel: String,
    val enabled: Boolean,
)

data class TableMenuQr(
    val tableId: Int,
    val tableName: String,
    val publicToken: String,
    val menuUrl: String,
    val qrPngBase64: String? = null,
)

fun List<PublicationChannel>.isMenuChannelEnabled(): Boolean =
    any { it.channel == "MENU" && it.enabled }
