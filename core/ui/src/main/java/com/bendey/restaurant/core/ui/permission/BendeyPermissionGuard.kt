package com.bendey.restaurant.core.ui.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.bendey.restaurant.core.domain.permission.RestaurantFeature
import com.bendey.restaurant.core.domain.permission.RestaurantPermissions

data class PermissionContext(
    val permissions: List<String> = emptyList(),
    val employeeType: String? = null,
) {
    fun hasPerm(perm: String): Boolean = RestaurantPermissions.hasPermission(permissions, perm)

    fun canAccess(feature: RestaurantFeature): Boolean =
        RestaurantPermissions.canAccessFeature(permissions, feature, employeeType)

    fun isAdmin(): Boolean = RestaurantPermissions.isRestaurantAdmin(permissions)
}

@Composable
fun rememberPermissionContext(
    permissions: List<String>,
    employeeType: String?,
): PermissionContext = remember(permissions, employeeType) {
    PermissionContext(permissions, employeeType)
}

/** Oculta contenido si el usuario no tiene el permiso corto. */
@Composable
fun RequirePermission(
    perm: String,
    context: PermissionContext,
    content: @Composable () -> Unit,
) {
    if (context.hasPerm(perm)) content()
}

/** Oculta contenido si no tiene acceso al feature. */
@Composable
fun RequireFeature(
    feature: RestaurantFeature,
    context: PermissionContext,
    content: @Composable () -> Unit,
) {
    if (context.canAccess(feature)) content()
}

/** Solo administrador restaurante (`s.m`). */
@Composable
fun RequireRestaurantAdmin(
    context: PermissionContext,
    content: @Composable () -> Unit,
) {
    if (context.isAdmin()) content()
}

/** Alias de RequireFeature para claridad semántica. */
@Composable
fun FeatureGuard(
    feature: RestaurantFeature,
    context: PermissionContext,
    content: @Composable () -> Unit,
) {
    RequireFeature(feature, context, content)
}
