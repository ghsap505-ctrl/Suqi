package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.Category
import com.example.model.Product
import com.example.ui.components.ProductVisual
import com.example.ui.i18n.AppLang
import com.example.ui.i18n.AppStrings

@Composable
fun CategoryEditDialog(
    lang: AppLang,
    initial: Category?,
    onDismiss: () -> Unit,
    onSave: (nameAr: String, nameEn: String, sortOrder: Long, active: Boolean) -> Unit,
) {
    val isAr = lang == AppLang.AR
    var nameAr by remember(initial) { mutableStateOf(initial?.nameAr ?: "") }
    var nameEn by remember(initial) { mutableStateOf(initial?.nameEn ?: "") }
    var sortText by remember(initial) { mutableStateOf((initial?.sortOrder ?: 1L).toString()) }
    var active by remember(initial) { mutableStateOf(initial?.active ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) {
                    if (isAr) "إضافة قسم جديد" else "New Category"
                } else {
                    if (isAr) "تعديل القسم" else "Edit Category"
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text(if (isAr) "اسم القسم (عربي) *" else "Category Name (Arabic) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("category_name_ar_input"),
                )
                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text(if (isAr) "اسم القسم (إنجليزي)" else "Category Name (English)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = sortText,
                    onValueChange = { sortText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(if (isAr) "ترتيب الظهور" else "Sort Order") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (isAr) "قسم نشط" else "Active Category")
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(nameAr, nameEn, sortText.toLongOrNull() ?: 1L, active) },
                modifier = Modifier.testTag("save_category_btn"),
            ) {
                Text(if (isAr) "حفظ" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isAr) "إلغاء" else "Cancel")
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductEditDialog(
    lang: AppLang,
    categories: List<Category>,
    initial: Product?,
    onDismiss: () -> Unit,
    onSave: (
        categoryId: String,
        nameAr: String,
        nameEn: String,
        descriptionAr: String,
        priceText: String,
        stockText: String,
        available: Boolean,
        imagePreset: String,
        imageUri: String?,
    ) -> Unit,
) {
    val isAr = lang == AppLang.AR
    val strings = remember(lang) { AppStrings(lang) }
    var nameAr by remember(initial) { mutableStateOf(initial?.nameAr ?: "") }
    var nameEn by remember(initial) { mutableStateOf(initial?.nameEn ?: "") }
    var descAr by remember(initial) { mutableStateOf(initial?.descriptionAr ?: "") }
    var priceText by remember(initial) { mutableStateOf(initial?.price?.let { if (it > 0) it.toString() else "" } ?: "") }
    var stockText by remember(initial) { mutableStateOf((initial?.stockCount ?: 10L).toString()) }
    var categoryId by remember(initial, categories) {
        mutableStateOf(initial?.categoryId?.takeIf { it.isNotBlank() } ?: categories.firstOrNull()?.id.orEmpty())
    }
    var available by remember(initial) { mutableStateOf(initial?.available ?: true) }
    var imagePreset by remember(initial) { mutableStateOf(initial?.imagePreset ?: "honey") }
    var imageUri by remember(initial) { mutableStateOf(initial?.imageUri) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            imageUri = uri.toString().take(500)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) {
                    if (isAr) "إضافة منتج جديد" else "New Product"
                } else {
                    if (isAr) "تعديل المنتج" else "Edit Product"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ProductVisual(
                        imagePreset = imagePreset,
                        imageUri = imageUri,
                        contentDescription = nameAr.ifBlank { "Product" },
                        modifier = Modifier.size(64.dp),
                    )
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.pickImageBtn, style = MaterialTheme.typography.labelMedium)
                    }
                }

                Text(
                    text = if (isAr) "أو اختر رمزاً افتراضياً:" else "Or choose icon preset:",
                    style = MaterialTheme.typography.labelMedium,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("honey" to "عسل", "coffee" to "قهوة", "abaya" to "ملابس", "earbuds" to "إلكترونيات", "watch" to "ساعات").forEach { (key, arLabel) ->
                        FilterChip(
                            selected = imagePreset == key,
                            onClick = { imagePreset = key },
                            label = { Text(if (isAr) arLabel else key) },
                        )
                    }
                }

                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text(if (isAr) "اسم المنتج (عربي) *" else "Product Name (Arabic) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_name_input"),
                )
                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text(if (isAr) "اسم المنتج (إنجليزي)" else "Product Name (English)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = descAr,
                    onValueChange = { descAr = it },
                    label = { Text(if (isAr) "الوصف" else "Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text(if (isAr) "السعر *" else "Price *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("product_price_input"),
                    )
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it.filter { c -> c.isDigit() } },
                        label = { Text(if (isAr) "الكمية" else "Stock") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                Text(
                    text = if (isAr) "القسم *" else "Category *",
                    style = MaterialTheme.typography.labelMedium,
                )
                if (categories.isEmpty()) {
                    Text(
                        text = if (isAr) "أضف قسماً أولاً قبل إضافة المنتجات" else "Create a category first",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = categoryId == cat.id,
                                onClick = { categoryId = cat.id },
                                label = { Text(if (isAr) cat.nameAr else cat.nameEn.ifBlank { cat.nameAr }) },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (isAr) "متوفر للطلب" else "Available for order")
                    Switch(checked = available, onCheckedChange = { available = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(categoryId, nameAr, nameEn, descAr, priceText, stockText, available, imagePreset, imageUri)
                },
                enabled = categories.isNotEmpty(),
                modifier = Modifier.testTag("save_product_btn"),
            ) {
                Text(if (isAr) "حفظ" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isAr) "إلغاء" else "Cancel")
            }
        },
    )
}
