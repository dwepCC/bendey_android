package com.bendey.restaurant.feature.combos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bendey.restaurant.core.designsystem.theme.BendeyColors
import com.bendey.restaurant.core.domain.catalog.BranchItem
import com.bendey.restaurant.core.domain.catalog.ComboBranchSetting
import com.bendey.restaurant.core.domain.catalog.ComboEditorTab
import com.bendey.restaurant.core.domain.catalog.ComboFixedItem
import com.bendey.restaurant.core.domain.catalog.ComboFormInput
import com.bendey.restaurant.core.domain.catalog.ComboSlot
import com.bendey.restaurant.core.domain.catalog.ComboSlotOption
import com.bendey.restaurant.core.domain.catalog.ComboType
import com.bendey.restaurant.core.domain.catalog.usesFixed
import com.bendey.restaurant.core.domain.catalog.usesSlots
import com.bendey.restaurant.core.domain.products.ProductItem
import com.bendey.restaurant.core.ui.components.BendeyPrimaryButton
import com.bendey.restaurant.core.ui.components.BendeyTextField

@Composable
fun ComboEditorSheet(
    open: Boolean,
    form: ComboFormInput,
    editorTab: ComboEditorTab,
    branches: List<BranchItem>,
    productSearchQuery: String,
    productSearchResults: List<ProductItem>,
    productSearchLoading: Boolean,
    loading: Boolean,
    error: String?,
    isEditing: Boolean,
    activePicker: ComboProductPickerTarget?,
    onDismiss: () -> Unit,
    onTabChange: (ComboEditorTab) -> Unit,
    onFormChange: ((ComboFormInput) -> ComboFormInput) -> Unit,
    onProductSearchChange: (String) -> Unit,
    onOpenPicker: (ComboProductPickerTarget) -> Unit,
    onClosePicker: () -> Unit,
    onSelectProduct: (ComboProductPickerTarget, ProductItem) -> Unit,
    onAddBranchRow: () -> Unit,
    onSave: () -> Unit,
) {
    if (!open) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BendeyColors.Background,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (isEditing) "Editar combo" else "Nuevo combo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            form.comboType.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = BendeyColors.OnSurfaceVariant,
                        )
                    }
                    BendeyPrimaryButton(
                        text = if (loading) "Guardando…" else "Guardar",
                        onClick = onSave,
                        enabled = !loading,
                    )
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ComboEditorTab.entries.forEach { tab ->
                        FilterChip(
                            selected = editorTab == tab,
                            onClick = { onTabChange(tab) },
                            label = { Text(tab.label) },
                        )
                    }
                }

                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (editorTab) {
                        ComboEditorTab.GENERAL -> GeneralTab(form, onFormChange)
                        ComboEditorTab.FIXED -> FixedTab(
                            form = form,
                            activePicker = activePicker,
                            productSearchQuery = productSearchQuery,
                            productSearchResults = productSearchResults,
                            productSearchLoading = productSearchLoading,
                            onFormChange = onFormChange,
                            onProductSearchChange = onProductSearchChange,
                            onOpenPicker = onOpenPicker,
                            onClosePicker = onClosePicker,
                            onSelectProduct = onSelectProduct,
                        )
                        ComboEditorTab.SLOTS -> SlotsTab(
                            form = form,
                            activePicker = activePicker,
                            productSearchQuery = productSearchQuery,
                            productSearchResults = productSearchResults,
                            productSearchLoading = productSearchLoading,
                            onFormChange = onFormChange,
                            onProductSearchChange = onProductSearchChange,
                            onOpenPicker = onOpenPicker,
                            onClosePicker = onClosePicker,
                            onSelectProduct = onSelectProduct,
                        )
                        ComboEditorTab.BRANCHES -> BranchesTab(
                            form = form,
                            branches = branches,
                            onFormChange = onFormChange,
                            onAddBranchRow = onAddBranchRow,
                        )
                    }
                    error?.let {
                        Text(it, color = BendeyColors.Error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

data class ComboProductPickerTarget(
    val kind: Kind,
    val fixedIndex: Int = -1,
    val slotIndex: Int = -1,
    val optionIndex: Int = -1,
) {
    enum class Kind { FIXED, SLOT_OPTION }
}

@Composable
private fun GeneralTab(
    form: ComboFormInput,
    onFormChange: ((ComboFormInput) -> ComboFormInput) -> Unit,
) {
    BendeyTextField(form.name, { v -> onFormChange { it.copy(name = v) } }, "Nombre *")
    BendeyTextField(form.basePrice, { v -> onFormChange { it.copy(basePrice = v) } }, "Precio base *")
    BendeyTextField(
        form.description,
        { v -> onFormChange { it.copy(description = v) } },
        "Descripción",
        singleLine = false,
    )
    Text("Tipo de combo", fontWeight = FontWeight.SemiBold)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ComboType.entries.chunked(2).forEach { rowTypes ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowTypes.forEach { type ->
                    FilterChip(
                        selected = form.comboType == type,
                        onClick = { onFormChange { it.copy(comboType = type) } },
                        label = { Text(type.label) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowTypes.size == 1) {
                    Row(Modifier.weight(1f)) {}
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Activo")
        Switch(form.active, { checked -> onFormChange { it.copy(active = checked) } })
    }
    if (form.comboType.showPromoDates()) {
        BendeyTextField(form.validFrom, { v -> onFormChange { it.copy(validFrom = v) } }, "Vigencia desde (YYYY-MM-DD)")
        BendeyTextField(form.validTo, { v -> onFormChange { it.copy(validTo = v) } }, "Vigencia hasta (YYYY-MM-DD)")
    }
}

@Composable
private fun FixedTab(
    form: ComboFormInput,
    activePicker: ComboProductPickerTarget?,
    productSearchQuery: String,
    productSearchResults: List<ProductItem>,
    productSearchLoading: Boolean,
    onFormChange: ((ComboFormInput) -> ComboFormInput) -> Unit,
    onProductSearchChange: (String) -> Unit,
    onOpenPicker: (ComboProductPickerTarget) -> Unit,
    onClosePicker: () -> Unit,
    onSelectProduct: (ComboProductPickerTarget, ProductItem) -> Unit,
) {
    if (!form.usesFixed() && !form.comboType.usesFixedByDefault()) {
        Text(
            "Este tipo de combo no usa productos fijos. Cambie el tipo o agregue productos.",
            style = MaterialTheme.typography.bodySmall,
            color = BendeyColors.OnSurfaceVariant,
        )
    }
    form.fixedItems.forEachIndexed { index, item ->
        FixedItemCard(
            item = item,
            pickerOpen = activePicker?.kind == ComboProductPickerTarget.Kind.FIXED && activePicker.fixedIndex == index,
            productSearchQuery = productSearchQuery,
            productSearchResults = productSearchResults,
            productSearchLoading = productSearchLoading,
            onProductSearchChange = onProductSearchChange,
            onOpenPicker = { onOpenPicker(ComboProductPickerTarget(ComboProductPickerTarget.Kind.FIXED, fixedIndex = index)) },
            onClosePicker = onClosePicker,
            onSelectProduct = { product ->
                onSelectProduct(ComboProductPickerTarget(ComboProductPickerTarget.Kind.FIXED, fixedIndex = index), product)
            },
            onQuantityChange = { quantity ->
                onFormChange { state ->
                    state.copy(
                        fixedItems = state.fixedItems.mapIndexed { i, fixed ->
                            if (i == index) fixed.copy(quantity = quantity) else fixed
                        },
                    )
                }
            },
            onRemove = {
                onFormChange { state ->
                    val next = state.fixedItems.filterIndexed { i, _ -> i != index }
                    state.copy(fixedItems = next.ifEmpty { listOf(ComboFixedItem(productId = 0)) })
                }
            },
        )
    }
    TextButton(
        onClick = {
            onFormChange { it.copy(fixedItems = it.fixedItems + ComboFixedItem(productId = 0)) }
        },
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Text("Agregar producto fijo")
    }
}

@Composable
private fun FixedItemCard(
    item: ComboFixedItem,
    pickerOpen: Boolean,
    productSearchQuery: String,
    productSearchResults: List<ProductItem>,
    productSearchLoading: Boolean,
    onProductSearchChange: (String) -> Unit,
    onOpenPicker: () -> Unit,
    onClosePicker: () -> Unit,
    onSelectProduct: (ProductItem) -> Unit,
    onQuantityChange: (Double) -> Unit,
    onRemove: () -> Unit,
) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.productName?.takeIf { it.isNotBlank() } ?: if (item.productId > 0) "Producto #${item.productId}" else "Seleccionar producto",
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = BendeyColors.Error)
                }
            }
            BendeyTextField(
                item.quantity.toString(),
                { v -> onQuantityChange(v.replace(",", ".").toDoubleOrNull() ?: 1.0) },
                "Cantidad",
            )
            ProductPickerSection(
                open = pickerOpen,
                query = productSearchQuery,
                results = productSearchResults,
                loading = productSearchLoading,
                onOpen = onOpenPicker,
                onClose = onClosePicker,
                onQueryChange = onProductSearchChange,
                onSelect = onSelectProduct,
            )
        }
    }
}

@Composable
private fun SlotsTab(
    form: ComboFormInput,
    activePicker: ComboProductPickerTarget?,
    productSearchQuery: String,
    productSearchResults: List<ProductItem>,
    productSearchLoading: Boolean,
    onFormChange: ((ComboFormInput) -> ComboFormInput) -> Unit,
    onProductSearchChange: (String) -> Unit,
    onOpenPicker: (ComboProductPickerTarget) -> Unit,
    onClosePicker: () -> Unit,
    onSelectProduct: (ComboProductPickerTarget, ProductItem) -> Unit,
) {
    if (!form.comboType.usesSlots()) {
        Text(
            "Los slots solo aplican a combos «Con opciones» o «Arma tu combo».",
            style = MaterialTheme.typography.bodySmall,
            color = BendeyColors.OnSurfaceVariant,
        )
    }
    form.slots.forEachIndexed { slotIndex, slot ->
        SlotCard(
            slot = slot,
            slotIndex = slotIndex,
            activePicker = activePicker,
            productSearchQuery = productSearchQuery,
            productSearchResults = productSearchResults,
            productSearchLoading = productSearchLoading,
            onFormChange = onFormChange,
            onProductSearchChange = onProductSearchChange,
            onOpenPicker = onOpenPicker,
            onClosePicker = onClosePicker,
            onSelectProduct = onSelectProduct,
            onRemoveSlot = {
                onFormChange { state ->
                    state.copy(slots = state.slots.filterIndexed { i, _ -> i != slotIndex })
                }
            },
        )
    }
    TextButton(
        onClick = {
            onFormChange {
                it.copy(
                    slots = it.slots + ComboSlot(
                        name = "",
                        options = listOf(ComboSlotOption(productId = 0)),
                    ),
                )
            }
        },
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Text("Agregar slot")
    }
}

@Composable
private fun SlotCard(
    slot: ComboSlot,
    slotIndex: Int,
    activePicker: ComboProductPickerTarget?,
    productSearchQuery: String,
    productSearchResults: List<ProductItem>,
    productSearchLoading: Boolean,
    onFormChange: ((ComboFormInput) -> ComboFormInput) -> Unit,
    onProductSearchChange: (String) -> Unit,
    onOpenPicker: (ComboProductPickerTarget) -> Unit,
    onClosePicker: () -> Unit,
    onSelectProduct: (ComboProductPickerTarget, ProductItem) -> Unit,
    onRemoveSlot: () -> Unit,
) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Slot ${slotIndex + 1}", fontWeight = FontWeight.SemiBold)
                IconButton(onClick = onRemoveSlot) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = BendeyColors.Error)
                }
            }
            BendeyTextField(
                slot.name,
                { name ->
                    onFormChange { state ->
                        state.copy(
                            slots = state.slots.mapIndexed { i, current ->
                                if (i == slotIndex) current.copy(name = name) else current
                            },
                        )
                    }
                },
                "Nombre del slot *",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BendeyTextField(
                    slot.minPick.toString(),
                    { value ->
                        onFormChange { state ->
                            state.copy(
                                slots = state.slots.mapIndexed { i, current ->
                                    if (i == slotIndex) current.copy(minPick = value.toIntOrNull()?.coerceAtLeast(1) ?: 1) else current
                                },
                            )
                        }
                    },
                    "Mín.",
                    modifier = Modifier.weight(1f),
                )
                BendeyTextField(
                    slot.maxPick.toString(),
                    { value ->
                        onFormChange { state ->
                            state.copy(
                                slots = state.slots.mapIndexed { i, current ->
                                    if (i == slotIndex) current.copy(maxPick = value.toIntOrNull()?.coerceAtLeast(1) ?: 1) else current
                                },
                            )
                        }
                    },
                    "Máx.",
                    modifier = Modifier.weight(1f),
                )
            }
            slot.options.forEachIndexed { optionIndex, option ->
                SlotOptionRow(
                    option = option,
                    pickerOpen = activePicker?.kind == ComboProductPickerTarget.Kind.SLOT_OPTION &&
                        activePicker.slotIndex == slotIndex &&
                        activePicker.optionIndex == optionIndex,
                    productSearchQuery = productSearchQuery,
                    productSearchResults = productSearchResults,
                    productSearchLoading = productSearchLoading,
                    onProductSearchChange = onProductSearchChange,
                    onOpenPicker = {
                        onOpenPicker(
                            ComboProductPickerTarget(
                                ComboProductPickerTarget.Kind.SLOT_OPTION,
                                slotIndex = slotIndex,
                                optionIndex = optionIndex,
                            ),
                        )
                    },
                    onClosePicker = onClosePicker,
                    onSelectProduct = { product ->
                        onSelectProduct(
                            ComboProductPickerTarget(
                                ComboProductPickerTarget.Kind.SLOT_OPTION,
                                slotIndex = slotIndex,
                                optionIndex = optionIndex,
                            ),
                            product,
                        )
                    },
                    onUpgradeChange = { upgrade ->
                        onFormChange { state ->
                            state.copy(
                                slots = state.slots.mapIndexed { sIdx, currentSlot ->
                                    if (sIdx != slotIndex) currentSlot
                                    else currentSlot.copy(
                                        options = currentSlot.options.mapIndexed { oIdx, currentOption ->
                                            if (oIdx != optionIndex) currentOption
                                            else currentOption.copy(upgradePrice = upgrade)
                                        },
                                    )
                                },
                            )
                        }
                    },
                    onRemove = {
                        onFormChange { state ->
                            state.copy(
                                slots = state.slots.mapIndexed { sIdx, currentSlot ->
                                    if (sIdx != slotIndex) currentSlot
                                    else {
                                        val next = currentSlot.options.filterIndexed { oIdx, _ -> oIdx != optionIndex }
                                        currentSlot.copy(
                                            options = next.ifEmpty { listOf(ComboSlotOption(productId = 0)) },
                                        )
                                    }
                                },
                            )
                        }
                    },
                )
            }
            TextButton(
                onClick = {
                    onFormChange { state ->
                        state.copy(
                            slots = state.slots.mapIndexed { i, current ->
                                if (i != slotIndex) current
                                else current.copy(options = current.options + ComboSlotOption(productId = 0))
                            },
                        )
                    }
                },
            ) {
                Text("Agregar opción")
            }
        }
    }
}

@Composable
private fun SlotOptionRow(
    option: ComboSlotOption,
    pickerOpen: Boolean,
    productSearchQuery: String,
    productSearchResults: List<ProductItem>,
    productSearchLoading: Boolean,
    onProductSearchChange: (String) -> Unit,
    onOpenPicker: () -> Unit,
    onClosePicker: () -> Unit,
    onSelectProduct: (ProductItem) -> Unit,
    onUpgradeChange: (Double) -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                option.productName?.takeIf { it.isNotBlank() }
                    ?: if (option.productId > 0) "Producto #${option.productId}" else "Opción sin producto",
                style = MaterialTheme.typography.bodyMedium,
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = BendeyColors.Error)
            }
        }
        BendeyTextField(
            option.upgradePrice.toString(),
            { v -> onUpgradeChange(v.replace(",", ".").toDoubleOrNull() ?: 0.0) },
            "Precio adicional",
        )
        ProductPickerSection(
            open = pickerOpen,
            query = productSearchQuery,
            results = productSearchResults,
            loading = productSearchLoading,
            onOpen = onOpenPicker,
            onClose = onClosePicker,
            onQueryChange = onProductSearchChange,
            onSelect = onSelectProduct,
        )
    }
}

@Composable
private fun BranchesTab(
    form: ComboFormInput,
    branches: List<BranchItem>,
    onFormChange: ((ComboFormInput) -> ComboFormInput) -> Unit,
    onAddBranchRow: () -> Unit,
) {
    if (form.branchSettings.isEmpty()) {
        Text(
            "Sin configuración por sucursal. Agregue filas para activar o sobreescribir precio.",
            style = MaterialTheme.typography.bodySmall,
            color = BendeyColors.OnSurfaceVariant,
        )
    }
    form.branchSettings.forEachIndexed { index, setting ->
        BranchSettingCard(
            setting = setting,
            branches = branches,
            onChange = { updated ->
                onFormChange { state ->
                    state.copy(
                        branchSettings = state.branchSettings.mapIndexed { i, current ->
                            if (i == index) updated else current
                        },
                    )
                }
            },
            onRemove = {
                onFormChange { state ->
                    state.copy(branchSettings = state.branchSettings.filterIndexed { i, _ -> i != index })
                }
            },
        )
    }
    TextButton(onClick = onAddBranchRow) {
        Icon(Icons.Default.Add, contentDescription = null)
        Text("Agregar sucursal")
    }
}

@Composable
private fun BranchSettingCard(
    setting: ComboBranchSetting,
    branches: List<BranchItem>,
    onChange: (ComboBranchSetting) -> Unit,
    onRemove: () -> Unit,
) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BendeyColors.Surface)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(setting.branchName ?: branches.firstOrNull { it.id == setting.branchId }?.name ?: "Sucursal #${setting.branchId}", fontWeight = FontWeight.SemiBold)
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = BendeyColors.Error)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Activo en sucursal")
                Switch(setting.active, { active -> onChange(setting.copy(active = active)) })
            }
            BendeyTextField(
                setting.priceOverride,
                { value -> onChange(setting.copy(priceOverride = value)) },
                "Precio override (vacío = precio base)",
            )
        }
    }
}

@Composable
private fun ProductPickerSection(
    open: Boolean,
    query: String,
    results: List<ProductItem>,
    loading: Boolean,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSelect: (ProductItem) -> Unit,
) {
    if (!open) {
        TextButton(onClick = onOpen) { Text("Buscar producto") }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Buscar producto", fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onClose) { Text("Cerrar") }
        }
        BendeyTextField(query, onQueryChange, "Nombre o código")
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
        } else if (results.isEmpty() && query.isNotBlank()) {
            Text("Sin resultados", style = MaterialTheme.typography.bodySmall, color = BendeyColors.OnSurfaceVariant)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(results, key = { it.id }) { product ->
                    TextButton(
                        onClick = { onSelect(product) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(product.name, fontWeight = FontWeight.Medium)
                            Text(
                                "${product.code} · S/ ${product.salePrice}",
                                style = MaterialTheme.typography.bodySmall,
                                color = BendeyColors.OnSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun ComboType.showPromoDates(): Boolean = this == ComboType.PROMO

private fun ComboFormInput.usesFixed(): Boolean =
    comboType.usesFixedByDefault() || fixedItems.any { it.productId > 0 }

private fun ComboType.usesFixedByDefault(): Boolean =
    this == ComboType.FIXED || this == ComboType.FAMILY || this == ComboType.PROMO

private fun ComboType.usesSlots(): Boolean =
    this == ComboType.CONFIGURABLE || this == ComboType.BUILD_YOUR_OWN
