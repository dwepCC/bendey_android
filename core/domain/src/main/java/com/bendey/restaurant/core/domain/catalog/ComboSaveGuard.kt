package com.bendey.restaurant.core.domain.catalog

fun ComboType.usesSlots(): Boolean =
    this == ComboType.CONFIGURABLE || this == ComboType.BUILD_YOUR_OWN

fun ComboType.usesFixedByDefault(): Boolean =
    this == ComboType.FIXED || this == ComboType.FAMILY || this == ComboType.PROMO

fun ComboType.showPromoDates(): Boolean = this == ComboType.PROMO

fun validateComboSaveSlotsType(comboType: ComboType, slots: List<ComboSlot>): String? {
    if (!comboType.usesSlots() && slots.any { it.name.isNotBlank() || it.options.any { o -> o.productId > 0 } }) {
        return "Para guardar opciones del cliente, elija tipo «Configurable» o «Arma tu combo»."
    }
    return null
}

fun ComboFormInput.usesFixed(): Boolean =
    comboType.usesFixedByDefault() || fixedItems.any { it.productId > 0 }

fun buildComboSaveSlots(comboType: ComboType, slots: List<ComboSlot>): List<ComboSlot> =
    if (comboType.usesSlots()) slots else emptyList()

fun normalizeComboDateInput(value: String): String? =
    value.trim().takeIf { it.isNotEmpty() }

