package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.CartAndWhatsAppEngine
import com.example.model.CartItem
import com.example.model.Category
import com.example.model.Product
import com.example.model.Store
import com.example.ui.components.ExternalIntentHelper
import com.example.ui.components.ProductVisual
import com.example.ui.components.StatusPill
import com.example.ui.components.StoreCoverImage
import com.example.ui.components.accentColorForKey
import com.example.ui.i18n.AppLang
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.SouqiAmberContainer
import com.example.ui.theme.SouqiEmeraldContainer
import com.example.ui.theme.SouqiOnAmberContainer
import com.example.ui.theme.SouqiOnEmeraldContainer
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorefrontAndCartScreen(
    lang: AppLang,
    store: Store,
    categoriesState: UiState<List<Category>>,
    productsState: UiState<List<Product>>,
    cartItems: List<CartItem>,
    isBusy: Boolean,
    onAddToCart: (Product) -> Unit,
    onUpdateCartQuantity: (String, Int) -> Unit,
    onClearCart: () -> Unit,
    onSubmitWhatsAppOrder: (
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        notes: String,
        onReady: (uri: String, message: String) -> Unit,
    ) -> Unit,
    onNotify: (String) -> Unit,
) {
    val context = LocalContext.current
    val strings = remember(lang) { AppStrings(lang) }
    val isAr = lang == AppLang.AR

    var selectedCategoryId by remember(store.id) { mutableStateOf<String?>(null) }
    var searchQuery by remember(store.id) { mutableStateOf("") }
    var detailProduct by remember { mutableStateOf<Product?>(null) }
    var showCartSheet by remember { mutableStateOf(false) }

    val cartTotals = remember(cartItems) { CartAndWhatsAppEngine.calculateCartTotals(cartItems) }
    val categories = (categoriesState as? UiState.Success)?.data.orEmpty().filter { it.active }
    val products = (productsState as? UiState.Success)?.data.orEmpty()

    val filteredProducts = remember(products, selectedCategoryId, searchQuery) {
        val q = searchQuery.trim().lowercase()
        products.filter { prod ->
            val matchesCat = selectedCategoryId == null || prod.categoryId == selectedCategoryId
            val matchesSearch = q.isEmpty() ||
                prod.nameAr.lowercase().contains(q) ||
                prod.nameEn.lowercase().contains(q) ||
                prod.descriptionAr.lowercase().contains(q)
            matchesCat && matchesSearch
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCartSheet = true },
                containerColor = accentColorForKey(store.accentKey),
                contentColor = Color.White,
                modifier = Modifier.testTag("open_cart_fab"),
                icon = {
                    BadgedBox(
                        badge = {
                            if (cartTotals.totalItems > 0) {
                                Badge { Text(cartTotals.totalItems.toString()) }
                            }
                        }
                    ) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = strings.cartTitle)
                    }
                },
                text = {
                    val label = if (cartTotals.totalItems == 0) {
                        strings.cartTitle
                    } else {
                        "${strings.cartTitle} (${strings.formatCurrency(cartTotals.subtotal, store.currency)})"
                    }
                    Text(label)
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Store Hero Header
            item {
                Box(
                    modifier = Modifier
                        .widthIn(max = 780.dp)
                        .fillMaxWidth()
                        .height(190.dp),
                ) {
                    StoreCoverImage(
                        coverPreset = store.coverPreset,
                        logoUri = store.logoUri,
                        storeName = store.name,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                    ) {
                        Surface(
                            onClick = {
                                ExternalIntentHelper.shareStoreLink(context, store.name, store.slug, isAr)
                            },
                            color = Color.Black.copy(alpha = 0.52f),
                            contentColor = Color.White,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("share_store_btn"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = strings.shareStore, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.shareStore, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (store.demoMode) {
                                StatusPill(strings.demoBadge, SouqiAmberContainer, SouqiOnAmberContainer)
                            }
                            if (store.verified) {
                                StatusPill(strings.verifiedBadge, SouqiEmeraldContainer, SouqiOnEmeraldContainer, Icons.Filled.Verified)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(store.name, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${store.city} • ${store.whatsappPhone}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f),
                            )
                        }
                    }
                }
            }

            // Store description + paused notice + search + categories
            item {
                Column(
                    modifier = Modifier
                        .widthIn(max = 780.dp)
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!store.active) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = strings.storePausedNotice,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                    if (store.description.isNotBlank()) {
                        Text(
                            text = store.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(strings.searchProductsHint) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("storefront_product_search"),
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCategoryId == null,
                                onClick = { selectedCategoryId = null },
                                label = { Text(strings.allCategories) },
                            )
                        }
                        items(categories, key = { it.id }) { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = cat.id },
                                label = { Text(if (isAr) cat.nameAr else cat.nameEn.ifBlank { cat.nameAr }) },
                            )
                        }
                    }
                }
            }

            when (productsState) {
                is UiState.Loading -> {
                    item {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Text(
                            text = productsState.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                is UiState.Success -> {
                    if (filteredProducts.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .widthIn(max = 780.dp)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            ) {
                                Text(
                                    text = if (isAr) "لا توجد منتجات مطابقة في هذا المتجر حالياً." else "No matching products found in this store.",
                                    modifier = Modifier.padding(20.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    } else {
                        items(filteredProducts, key = { it.id }) { product ->
                            ProductStorefrontCard(
                                product = product,
                                currency = store.currency,
                                storeActive = store.active,
                                strings = strings,
                                isAr = isAr,
                                onOpenDetail = { detailProduct = product },
                                onAddToCart = { onAddToCart(product) },
                            )
                        }
                    }
                }
            }
        }
    }

    // Product Detail Sheet
    val activeDetail = detailProduct
    if (activeDetail != null) {
        ModalBottomSheet(onDismissRequest = { detailProduct = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProductVisual(
                    imagePreset = activeDetail.imagePreset,
                    imageUri = activeDetail.imageUri,
                    contentDescription = activeDetail.nameAr,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                )
                Text(
                    text = if (isAr) activeDetail.nameAr else activeDetail.nameEn.ifBlank { activeDetail.nameAr },
                    style = MaterialTheme.typography.headlineSmall,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = strings.formatCurrency(activeDetail.price, store.currency),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    StatusPill(
                        label = if (activeDetail.available) strings.availableLabel else strings.unavailableLabel,
                        containerColor = if (activeDetail.available) SouqiEmeraldContainer else MaterialTheme.colorScheme.errorContainer,
                        contentColor = if (activeDetail.available) SouqiOnEmeraldContainer else MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
                val desc = if (isAr) activeDetail.descriptionAr else activeDetail.descriptionEn.ifBlank { activeDetail.descriptionAr }
                if (desc.isNotBlank()) {
                    Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = {
                        onAddToCart(activeDetail)
                        detailProduct = null
                    },
                    enabled = activeDetail.available && store.active,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) {
                    Icon(Icons.Filled.AddShoppingCart, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.addToCart)
                }
            }
        }
    }

    // Cart & WhatsApp Checkout Bottom Sheet
    if (showCartSheet) {
        ModalBottomSheet(onDismissRequest = { showCartSheet = false }) {
            CartCheckoutSheetContent(
                store = store,
                cartItems = cartItems,
                strings = strings,
                isAr = isAr,
                isBusy = isBusy,
                onUpdateQuantity = onUpdateCartQuantity,
                onClearCart = onClearCart,
                onSubmitOrder = { name, phone, address, notes ->
                    onSubmitWhatsAppOrder(name, phone, address, notes) { waUri, waText ->
                        val launched = ExternalIntentHelper.openWhatsAppUrl(context, waUri)
                        if (!launched) {
                            ExternalIntentHelper.copyToClipboard(context, "Souqi Order", waText)
                            onNotify(
                                if (isAr) "واتساب غير مثبت — تم نسخ نص الطلب للحافظة" else "WhatsApp not installed — order text copied to clipboard"
                            )
                        }
                        showCartSheet = false
                    }
                },
                onCopyMessageOnly = { name, phone, address, notes ->
                    val msg = CartAndWhatsAppEngine.buildWhatsAppOrderMessage(
                        store = store,
                        items = cartItems,
                        customerName = name.ifBlank { if (isAr) "عميل" else "Customer" },
                        customerPhone = phone.ifBlank { "-" },
                        deliveryAddress = address.ifBlank { "-" },
                        notes = notes,
                        lang = lang,
                    )
                    ExternalIntentHelper.copyToClipboard(context, "Souqi Order", msg)
                    onNotify(if (isAr) "تم نسخ نص الطلب" else "Order text copied")
                },
            )
        }
    }
}

@Composable
private fun ProductStorefrontCard(
    product: Product,
    currency: String,
    storeActive: Boolean,
    strings: AppStrings,
    isAr: Boolean,
    onOpenDetail: () -> Unit,
    onAddToCart: () -> Unit,
) {
    ElevatedCard(
        onClick = onOpenDetail,
        modifier = Modifier
            .widthIn(max = 780.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProductVisual(
                imagePreset = product.imagePreset,
                imageUri = product.imageUri,
                contentDescription = product.nameAr,
                modifier = Modifier.size(84.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isAr) product.nameAr else product.nameEn.ifBlank { product.nameAr },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val desc = if (isAr) product.descriptionAr else product.descriptionEn.ifBlank { product.descriptionAr }
                if (desc.isNotBlank()) {
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = strings.formatCurrency(product.price, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = if (product.available) strings.availableLabel else strings.unavailableLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (product.available) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        )
                    }
                    Button(
                        onClick = onAddToCart,
                        enabled = product.available && storeActive,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("add_to_cart_${product.id}"),
                    ) {
                        Icon(Icons.Filled.AddShoppingCart, contentDescription = strings.addToCart, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.addToCart, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun CartCheckoutSheetContent(
    store: Store,
    cartItems: List<CartItem>,
    strings: AppStrings,
    isAr: Boolean,
    isBusy: Boolean,
    onUpdateQuantity: (String, Int) -> Unit,
    onClearCart: () -> Unit,
    onSubmitOrder: (String, String, String, String) -> Unit,
    onCopyMessageOnly: (String, String, String, String) -> Unit,
) {
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var deliveryAddress by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val totals = remember(cartItems) { CartAndWhatsAppEngine.calculateCartTotals(cartItems) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(strings.cartTitle, style = MaterialTheme.typography.headlineSmall)
            if (cartItems.isNotEmpty()) {
                TextButton(onClick = onClearCart) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.clearCart)
                }
            }
        }

        if (cartItems.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = strings.cartEmpty,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(18.dp),
                )
            }
        } else {
            cartItems.forEach { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isAr) item.product.nameAr else item.product.nameEn.ifBlank { item.product.nameAr },
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = strings.formatCurrency(item.lineTotal, store.currency),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onUpdateQuantity(item.product.id, item.quantity - 1) },
                                modifier = Modifier.testTag("cart_dec_${item.product.id}"),
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                            }
                            Text(
                                text = item.quantity.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                            IconButton(
                                onClick = { onUpdateQuantity(item.product.id, item.quantity + 1) },
                                modifier = Modifier.testTag("cart_inc_${item.product.id}"),
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase")
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(strings.subtotalLabel, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = strings.formatCurrency(totals.subtotal, store.currency),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("cart_subtotal_text"),
                )
            }

            OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it },
                label = { Text(strings.customerNameLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("checkout_name_input"),
            )
            OutlinedTextField(
                value = customerPhone,
                onValueChange = { customerPhone = it },
                label = { Text(strings.customerPhoneLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("checkout_phone_input"),
            )
            OutlinedTextField(
                value = deliveryAddress,
                onValueChange = { deliveryAddress = it },
                label = { Text(strings.deliveryAddressLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("checkout_address_input"),
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(strings.orderNotesLabel) },
                modifier = Modifier.fillMaxWidth().testTag("checkout_notes_input"),
            )

            Surface(
                color = SouqiAmberContainer,
                contentColor = SouqiOnAmberContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = strings.orderConfirmationDisclaimer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Button(
                onClick = { onSubmitOrder(customerName, customerPhone, deliveryAddress, notes) },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("send_whatsapp_order_btn"),
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Chat, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.sendViaWhatsApp)
                }
            }

            OutlinedButton(
                onClick = { onCopyMessageOnly(customerName, customerPhone, deliveryAddress, notes) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.copyOrderMessage)
            }
        }
    }
}
