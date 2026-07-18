package com.bendey.restaurant.core.domain.digitalmenu

import com.bendey.restaurant.core.domain.model.AppResult

interface DigitalMenuRepository {
    suspend fun getSettings(): AppResult<StaffMenuSettings>
    suspend fun updateSettings(menuEnabled: Boolean, config: MenuConfig): AppResult<StaffMenuSettings>
    suspend fun regenerateMenuToken(): AppResult<Pair<String, String>>
    suspend fun getProductPublicationChannels(productId: Int): AppResult<List<PublicationChannel>>
    suspend fun setMenuChannelEnabled(productId: Int, enabled: Boolean): AppResult<Unit>
    suspend fun getTableMenuQr(tableId: Int, includePng: Boolean = true): AppResult<TableMenuQr>
    suspend fun rotateTableMenuToken(tableId: Int): AppResult<TableMenuQr>
}
