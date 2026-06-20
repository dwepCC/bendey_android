package com.bendey.restaurant.core.data.mapper

import com.bendey.restaurant.core.domain.model.AuthUser
import com.bendey.restaurant.core.domain.model.BranchBrief
import com.bendey.restaurant.core.domain.model.TenantBinding
import com.bendey.restaurant.core.domain.model.UserSession
import com.bendey.restaurant.core.network.dto.LoginResponseDto
import com.bendey.restaurant.core.network.dto.TenantByRucDto

fun TenantByRucDto.toDomain(ruc: String): TenantBinding {
    val slug = slug.ifBlank { tenantSlug.orEmpty() }
    val apiUrl = apiUrl.orEmpty()
    return TenantBinding(
        slug = slug,
        name = name,
        ruc = ruc,
        apiUrl = apiUrl,
        tokenConsultaDatos = tokenConsultaDatos,
    )
}

fun LoginResponseDto.toDomain(): UserSession = UserSession(
    token = token,
    user = AuthUser(
        id = user.id,
        name = user.name,
        email = user.email,
        role = user.role,
        employeeType = user.employeeType,
        staffId = user.staffId,
    ),
    restaurantPermissions = restaurantPermissions.orEmpty(),
    modules = modules.orEmpty(),
    permissions = permissions.orEmpty(),
    activeBranch = activeBranch?.let { BranchBrief(it.id, it.name, it.isMain == true) },
    canSwitchBranch = canSwitchBranch == true,
    allowedBranches = allowedBranches.orEmpty().map {
        BranchBrief(it.id, it.name, it.isMain == true)
    },
)
