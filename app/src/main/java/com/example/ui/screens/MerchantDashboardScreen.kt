package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.CartAndWhatsAppEngine
import com.example.model.Category
import com.example.model.OrderInquiry
import com.example.model.Product
import com.example.model.Store
import com.example.ui.components.ExternalIntentHelper
import com.example.ui.components.ProductVisual
import com.example.ui.components.StatusPill
import com.example.ui.i18n.AppLang
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.SouqiAmber
import com.example.ui.theme.SouqiAmberContainer
import com.example.ui.theme.SouqiEmerald
import com.example.ui.theme.SouqiEmeraldContainer
import com.example.ui.theme.SouqiOnAmberContainer
import com.example.ui.theme.SouqiOnEmeraldContainer
import com.example.ui.theme.SouqiTeal
import com.example.ui.theme.SouqiTerracotta
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantDashboardScreen(
    lang: AppLang,
    myStoresState: UiState<List<Store>>,
    categoriesState: UiState<List<Category>>,
    productsState: UiState<List<Product>>,
    inquiriesState: UiState<List<OrderInquiry>>,
    isBusy: Boolean,
    onSaveStore: (
        existingStoreId: String,
        name: String,
        slug: String,
        city: String,
        whatsappPhone: String,
        description: String,
        currency: String,
        accentKey: String,
        coverPreset: String,
        logoUri: String?,
        active: Boolean,
    ) -> Unit,
    onSaveCategory: (storeId: String, categoryId: String, nameAr: String, nameEn: String, sortOrder: Long, active: Boolean) -> Unit,
    onDeleteCategory: (storeId: String, categoryId: String) -> Unit,
    onSaveProduct: (
        storeId: String,
        productId: String,
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
    onDeleteProduct: (storeId: String, productId: String) -> Unit,
    onUpdateInquiryStatus: (OrderInquiry, String) -> Unit,
    onOpenStorefront: (Store) -> Unit,
) {
    val strings = remember(lang) { AppStrings(lang) }
    val isAr = lang == AppLang.AR

    when (myStoresState) {
        is UiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                Text(myStoresState.message, color = MaterialTheme.colorScheme.error)
            }
        }
        is UiState.Success -> {
            val myStore = myStoresState.data.firstOrNull()
            if (myStore == null) {
                MerchantStoreFormContent(
                    lang = lang,
                    strings = strings,
                    existingStore = null,
                    isBusy = isBusy,
                    onSave = { name, slug, city, wa, desc, curr, accent, cover, logo, active ->
                        onSaveStore("", name, slug, city, wa, desc, curr, accent, cover, logo, active)
                    },
                )
            } else {
                var selectedSubTab by remember { mutableIntStateOf(0) }
                val categories = (categoriesState as? UiState.Success)?.data.orEmpty()
                val products = (productsState as? UiState.Success)?.data.orEmpty()
                val inquiries = (inquiriesState as? UiState.Success)?.data.orEmpty()

                var showCategoryDialog by remember { mutableStateOf(false) }
                var editingCategory by remember { mutableStateOf<Category?>(null) }
                var showProductDialog by remember { mutableStateOf(false) }
                var editingProduct by remember { mutableStateOf<Product?>(null) }

                Column(modifier = Modifier.fillMaxSize()) {
                    PrimaryTabRow(selectedTabIndex = selectedSubTab) {
                        Tab(
                            selected = selectedSubTab == 0,
                            onClick = { selectedSubTab = 0 },
                            text = { Text(strings.merchantOverviewTab) },
                        )
                        Tab(
                            selected = selectedSubTab == 1,
                            onClick = { selectedSubTab = 1 },
                            text = { Text(strings.merchantCatalogTab) },
                        )
                        Tab(
                            selected = selectedSubTab == 2,
                            onClick = { selectedSubTab = 2 },
                            text = { Text(strings.merchantInquiriesTab) },
                        )
                        Tab(
                            selected = selectedSubTab == 3,
                            onClick = { selectedSubTab = 3 },
                            text = { Text(strings.merchantSettingsTab) },
                        )
                    }

                    when (selectedSubTab) {
                        0 -> MerchantOverviewTab(
                            lang = lang,
                            strings = strings,
                            store = myStore,
                            products = products,
                            inquiries = inquiries,
                            onOpenStorefront = { onOpenStorefront(myStore) },
                        )
                        1 -> MerchantCatalogTab(
                            lang = lang,
                            strings = strings,
                            store = myStore,
                            categories = categories,
                            products = products,
                            onAddCategory = { editingCategory = null; showCategoryDialog = true },
                            onEditCategory = { editingCategory = it; showCategoryDialog = true },
                            onDeleteCategory = { onDeleteCategory(myStore.id, it.id) },
                            onAddProduct = { editingProduct = null; showProductDialog = true },
                            onEditProduct = { editingProduct = it; showProductDialog = true },
                            onDeleteProduct = { onDeleteProduct(myStore.id, it.id) },
                        )
                        2 -> MerchantInquiriesTab(
                            lang = lang,
                            strings = strings,
                            inquiries = inquiries,
                            onUpdateStatus = onUpdateInquiryStatus,
                        )
                        3 -> MerchantStoreFormContent(
                            lang = lang,
                            strings = strings,
                            existingStore = myStore,
                            isBusy = isBusy,
                            onSave = { name, slug, city, wa, desc, curr, accent, cover, logo, active ->
                                onSaveStore(myStore.id, name, slug, city, wa, desc, curr, accent, cover, logo, active)
                            },
                        )
                    }
                }

                if (showCategoryDialog) {
                    CategoryEditDialog(
                        lang = lang,
                        initial = editingCategory,
                        onDismiss = { showCategoryDialog = false },
                        onSave = { nameAr, nameEn, sortOrder, active ->
                            onSaveCategory(
                                myStore.id,
                                editingCategory?.id.orEmpty(),
                                nameAr,
                                nameEn,
                                sortOrder,
                                active,
                            )
                            showCategoryDialog = false
                        },
                    )
                }

                if (showProductDialog) {
                    ProductEditDialog(
                        lang = lang,
                        categories = categories,
                        initial = editingProduct,
                        onDismiss = { showProductDialog = false },
                        onSave = { catId, nameAr, nameEn, descAr, priceText, stockText, available, preset, uri ->
                            onSaveProduct(
                                myStore.id,
                                editingProduct?.id.orEmpty(),
                                catId,
                                nameAr,
                                nameEn,
                                descAr,
                                priceText,
                                stockText,
                                available,
                                preset,
                                uri,
                            )
                            showProductDialog = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MerchantOverviewTab(
    lang: AppLang,
    strings: AppStrings,
    store: Store,
    products: List<Product>,
    inquiries: List<OrderInquiry>,
    onOpenStorefront: () -> Unit,
) {
    val context = LocalContext.current
    val isAr = lang == AppLang.AR
    val availableCount = products.count { it.available }
    val pendingCount = inquiries.count { it.status == "pending_whatsapp" || it.status == "contacted" }
    val confirmedCount = inquiries.count { it.status == "confirmed" }
    val cancelledCount = inquiries.count { it.status == "cancelled" }
    val estimatedRevenue = inquiries.filter { it.status != "cancelled" }.sumOf { it.totalAmount }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(store.name, style = MaterialTheme.typography.titleLarge)
                            Text("souqi.app/s/${store.slug}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        StatusPill(
                            label = strings.statusLabel(store.subscriptionStatus),
                            containerColor = SouqiAmberContainer,
                            contentColor = SouqiOnAmberContainer,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onOpenStorefront, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isAr) "معاينة المتجر" else "View Store")
                        }
                        OutlinedButton(
                            onClick = { ExternalIntentHelper.shareStoreLink(context, store.name, store.slug, isAr) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.shareStore)
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetricBox(
                    title = if (isAr) "المنتجات" else "Products",
                    value = "$availableCount / ${products.size}",
                    modifier = Modifier.weight(1f),
                )
                MetricBox(
                    title = if (isAr) "الطلبات الواردة" else "Inquiries",
                    value = inquiries.size.toString(),
                    modifier = Modifier.weight(1f),
                )
                MetricBox(
                    title = if (isAr) "قيمة الطلبات" else "Est. Value",
                    value = strings.formatCurrency(estimatedRevenue, store.currency),
                    modifier = Modifier.weight(1.3f),
                )
            }
        }

        item {
            Card(
                modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isAr) "تحليل حالات الطلبات" else "Inquiry Status Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    val maxVal = maxOf(1, pendingCount, confirmedCount, cancelledCount)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                    ) {
                        val barWidth = size.width / 5f
                        val bars = listOf(
                            pendingCount to SouqiAmber,
                            confirmedCount to SouqiEmerald,
                            cancelledCount to SouqiTerracotta,
                        )
                        bars.forEachIndexed { idx, (count, color) ->
                            val ratio = count.toFloat() / maxVal.toFloat()
                            val barHeight = (size.height - 12f) * ratio.coerceAtLeast(0.08f)
                            val x = (idx * 1.6f + 0.6f) * barWidth
                            drawRoundRect(
                                color = color,
                                topLeft = Offset(x, size.height - barHeight),
                                size = Size(barWidth * 0.85f, barHeight),
                                cornerRadius = CornerRadius(14f, 14f),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Text("${if (isAr) "معلقة" else "Pending"}: $pendingCount", style = MaterialTheme.typography.labelSmall)
                        Text("${if (isAr) "مؤكدة" else "Confirmed"}: $confirmedCount", style = MaterialTheme.typography.labelSmall)
                        Text("${if (isAr) "ملغاة" else "Cancelled"}: $cancelledCount", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MerchantCatalogTab(
    lang: AppLang,
    strings: AppStrings,
    store: Store,
    categories: List<Category>,
    products: List<Product>,
    onAddCategory: () -> Unit,
    onEditCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onAddProduct: () -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
) {
    val isAr = lang == AppLang.AR
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (isAr) "الأقسام (${categories.size})" else "Categories (${categories.size})", style = MaterialTheme.typography.titleLarge)
                Button(onClick = onAddCategory, modifier = Modifier.testTag("merchant_add_category_btn")) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.addCategoryBtn)
                }
            }
        }
        items(categories, key = { it.id }) { cat ->
            Card(modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (isAr) cat.nameAr else cat.nameEn.ifBlank { cat.nameAr }, style = MaterialTheme.typography.titleSmall)
                    Row {
                        IconButton(onClick = { onEditCategory(cat) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { onDeleteCategory(cat) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (isAr) "المنتجات (${products.size})" else "Products (${products.size})", style = MaterialTheme.typography.titleLarge)
                Button(onClick = onAddProduct, modifier = Modifier.testTag("merchant_add_product_btn")) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.addProductBtn)
                }
            }
        }

        if (products.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                ) {
                    Text(
                        text = if (isAr) "لم تضف أي منتجات بعد. أنشئ قسماً ثم أضف أول منتج لك!" else "No products yet. Create a category then add your first product!",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        } else {
            items(products, key = { it.id }) { prod ->
                Card(modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ProductVisual(
                            imagePreset = prod.imagePreset,
                            imageUri = prod.imageUri,
                            contentDescription = prod.nameAr,
                            modifier = Modifier.size(56.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (isAr) prod.nameAr else prod.nameEn.ifBlank { prod.nameAr }, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${strings.formatCurrency(prod.price, store.currency)} • ${if (prod.available) strings.availableLabel else strings.unavailableLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onEditProduct(prod) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { onDeleteProduct(prod) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MerchantInquiriesTab(
    lang: AppLang,
    strings: AppStrings,
    inquiries: List<OrderInquiry>,
    onUpdateStatus: (OrderInquiry, String) -> Unit,
) {
    val context = LocalContext.current
    val isAr = lang == AppLang.AR
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (inquiries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                ) {
                    Text(
                        text = if (isAr) "لا توجد طلبات واردة بعد." else "No order inquiries yet.",
                        modifier = Modifier.padding(18.dp),
                    )
                }
            }
        } else {
            items(inquiries, key = { it.id }) { inquiry ->
                val isTerminal = inquiry.status == "confirmed" || inquiry.status == "cancelled"
                ElevatedCard(modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(inquiry.customerName, style = MaterialTheme.typography.titleMedium)
                            StatusPill(
                                label = strings.statusLabel(inquiry.status),
                                containerColor = if (inquiry.status == "confirmed") SouqiEmeraldContainer else SouqiAmberContainer,
                                contentColor = if (inquiry.status == "confirmed") SouqiOnEmeraldContainer else SouqiOnAmberContainer,
                            )
                        }
                        Text("${inquiry.customerPhone} • ${inquiry.deliveryAddress}", style = MaterialTheme.typography.bodySmall)
                        Text(inquiry.itemsSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${strings.subtotalLabel}: ${strings.formatCurrency(inquiry.totalAmount, inquiry.currency)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val msg = if (isAr) "مرحباً ${inquiry.customerName}، بخصوص طلبك من ${inquiry.storeName}" else "Hello ${inquiry.customerName}, regarding your inquiry at ${inquiry.storeName}"
                                    val url = CartAndWhatsAppEngine.buildWhatsAppUriString(inquiry.customerPhone, msg)
                                    ExternalIntentHelper.openWhatsAppUrl(context, url)
                                },
                            ) {
                                Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (isAr) "واتساب العميل" else "WhatsApp")
                            }

                            if (!isTerminal) {
                                Button(onClick = { onUpdateStatus(inquiry, "confirmed") }) {
                                    Text(if (isAr) "تأكيد الطلب" else "Confirm")
                                }
                                TextButton(onClick = { onUpdateStatus(inquiry, "cancelled") }) {
                                    Text(if (isAr) "إلغاء" else "Cancel", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MerchantStoreFormContent(
    lang: AppLang,
    strings: AppStrings,
    existingStore: Store?,
    isBusy: Boolean,
    onSave: (
        name: String,
        slug: String,
        city: String,
        whatsappPhone: String,
        description: String,
        currency: String,
        accentKey: String,
        coverPreset: String,
        logoUri: String?,
        active: Boolean,
    ) -> Unit,
) {
    val isAr = lang == AppLang.AR
    var name by remember(existingStore?.id) { mutableStateOf(existingStore?.name ?: "") }
    var slug by remember(existingStore?.id) { mutableStateOf(existingStore?.slug ?: "") }
    var city by remember(existingStore?.id) { mutableStateOf(existingStore?.city ?: "صنعاء") }
    var whatsappPhone by remember(existingStore?.id) { mutableStateOf(existingStore?.whatsappPhone ?: "+96777") }
    var description by remember(existingStore?.id) { mutableStateOf(existingStore?.description ?: "") }
    var currency by remember(existingStore?.id) { mutableStateOf(existingStore?.currency ?: "YER") }
    var accentKey by remember(existingStore?.id) { mutableStateOf(existingStore?.accentKey ?: "teal") }
    var coverPreset by remember(existingStore?.id) { mutableStateOf(existingStore?.coverPreset ?: "hero") }
    var logoUri by remember(existingStore?.id) { mutableStateOf(existingStore?.logoUri) }
    var active by remember(existingStore?.id) { mutableStateOf(existingStore?.active ?: true) }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) logoUri = uri.toString().take(500)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = if (existingStore == null) strings.merchantOnboardingTitle else strings.merchantSettingsTab,
                    style = MaterialTheme.typography.headlineSmall,
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isAr) "اسم المتجر *" else "Store Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("merchant_store_name_input"),
                )
                OutlinedTextField(
                    value = slug,
                    onValueChange = { slug = it },
                    label = { Text(if (isAr) "الرابط المختصر (إنجليزي مثل sanaa-shop) *" else "Store Slug (e.g. sanaa-shop) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("merchant_store_slug_input"),
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text(if (isAr) "المدينة *" else "City *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = whatsappPhone,
                    onValueChange = { whatsappPhone = it },
                    label = { Text(if (isAr) "رقم واتساب لاستقبال الطلبات *" else "WhatsApp Number *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("merchant_store_wa_input"),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (isAr) "وصف المتجر" else "Store Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(if (isAr) "العملة" else "Currency", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("YER", "SAR", "USD").forEach { curr ->
                        FilterChip(selected = currency == curr, onClick = { currency = curr }, label = { Text(curr) })
                    }
                }

                Text(if (isAr) "لون وهوية المتجر" else "Store Theme Accent", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("teal" to "فيروزي", "amber" to "ذهبي", "emerald" to "زمردي", "terracotta" to "طيني").forEach { (key, arLabel) ->
                        FilterChip(selected = accentKey == key, onClick = { accentKey = key }, label = { Text(if (isAr) arLabel else key) })
                    }
                }

                Text(if (isAr) "صورة الغلاف الافتراضية" else "Cover Preset", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("hero" to "عام", "honey" to "عسل وبهارات", "fashion" to "أزياء", "electronics" to "إلكترونيات").forEach { (key, arLabel) ->
                        FilterChip(selected = coverPreset == key, onClick = { coverPreset = key }, label = { Text(if (isAr) arLabel else key) })
                    }
                }

                OutlinedButton(
                    onClick = { logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (logoUri.isNullOrBlank()) strings.pickImageBtn else if (isAr) "تم اختيار صورة — اضغط للتغيير" else "Photo selected — tap to change")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (isAr) "المتجر مفتوح لاستقبال الطلبات" else "Store Active")
                    Switch(checked = active, onCheckedChange = { active = it })
                }

                Button(
                    onClick = {
                        onSave(name, slug, city, whatsappPhone, description, currency, accentKey, coverPreset, logoUri, active)
                    },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("merchant_save_store_btn"),
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(strings.saveChangesBtn)
                    }
                }
            }
        }
    }
}
