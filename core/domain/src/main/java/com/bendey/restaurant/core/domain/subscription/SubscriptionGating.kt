package com.bendey.restaurant.core.domain.subscription

/** Módulo de facturación electrónica — único módulo restringido por plan hoy. */
const val BILLING_MODULE_KEY = "billing"

fun hasModule(modules: List<String>, moduleKey: String): Boolean = modules.contains(moduleKey)
