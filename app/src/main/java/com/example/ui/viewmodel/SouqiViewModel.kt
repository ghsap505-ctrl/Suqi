package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SouqiRepository
import com.example.domain.CartAndWhatsAppEngine
import com.example.domain.SampleCatalog
import com.example.model.CartItem
import com.example.model.Category
import com.example.model.OrderInquiry
import com.example.model.Product
import com.example.model.Store
import com.example.model.SubscriptionPlan
import com.example.model.UserProfile
import com.example.ui.i18n.AppLang
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class MainTab {
    EXPLORE,
    STOREFRONT,
    MERCHANT,
    ADMIN,
}

@OptIn(ExperimentalCoroutinesApi::class)
class SouqiViewModel(
    private val repository: SouqiRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    val isEmailVerified: Boolean,
    val currentUserDisplayName: String,
) : ViewModel() {

    private val _lang = MutableStateFlow(AppLang.AR)
    val lang: StateFlow<AppLang> = _lang.asStateFlow()

    private val _activeTab = MutableStateFlow(MainTab.EXPLORE)
    val activeTab: StateFlow<MainTab> = _activeTab.asStateFlow()

    private val _selectedStore = MutableStateFlow(SampleCatalog.demoStores.first())
    val selectedStore: StateFlow<Store> = _selectedStore.asStateFlow()

    private val _cartStoreId = MutableStateFlow<String?>(null)
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _localDemoInquiries = MutableStateFlow(SampleCatalog.demoInquiries)
    val localDemoInquiries: StateFlow<List<OrderInquiry>> = _localDemoInquiries.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    private fun <T> Flow<T>.toUiState(
        tag: String,
        fallbackError: String,
    ): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(tag, "Stream error", rootError)
                emit(UiState.Error(rootError.message ?: fallbackError))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .toUiState(TAG, "Failed to load profile")

    val isAdmin: StateFlow<Boolean> = userProfileState.map { state ->
        val profileRole = (state as? UiState.Success)?.data?.role
        val bootstrapped = isEmailVerified && currentUserEmail.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true)
        bootstrapped || profileRole == "admin"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    val cloudStoresState: StateFlow<UiState<List<Store>>> =
        repository.observeActiveStores()
            .toUiState(TAG, "Failed to load active stores")

    val myStoresState: StateFlow<UiState<List<Store>>> =
        repository.observeMyStores(currentUserId)
            .toUiState(TAG, "Failed to load merchant stores")

    val myPrimaryStore: StateFlow<Store?> = myStoresState.map { state ->
        (state as? UiState.Success)?.data?.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val plansState: StateFlow<UiState<List<SubscriptionPlan>>> =
        repository.observeActivePlans()
            .map { plans -> plans.ifEmpty { SampleCatalog.defaultPlans } }
            .toUiState(TAG, "Failed to load plans")

    val selectedStoreCategories: StateFlow<UiState<List<Category>>> =
        combine(selectedStore, isAdmin) { store, admin -> store to admin }
            .flatMapLatest { (store, admin) ->
                if (store.demoMode) {
                    flowOf(SampleCatalog.demoCategoriesByStore[store.id].orEmpty())
                } else {
                    repository.observeStoreCategories(store.id, store.ownerUid, admin)
                }
            }
            .toUiState(TAG, "Failed to load categories")

    val selectedStoreProducts: StateFlow<UiState<List<Product>>> =
        combine(selectedStore, isAdmin) { store, admin -> store to admin }
            .flatMapLatest { (store, admin) ->
                if (store.demoMode) {
                    flowOf(SampleCatalog.demoProductsByStore[store.id].orEmpty())
                } else {
                    repository.observeStoreProducts(store.id, store.ownerUid, admin)
                }
            }
            .toUiState(TAG, "Failed to load products")

    val myStoreCategoriesState: StateFlow<UiState<List<Category>>> =
        combine(myPrimaryStore, isAdmin) { store, admin -> store to admin }
            .flatMapLatest { (store, admin) ->
                if (store == null) {
                    flowOf(emptyList())
                } else {
                    repository.observeStoreCategories(store.id, store.ownerUid, admin)
                }
            }
            .toUiState(TAG, "Failed to load merchant categories")

    val myStoreProductsState: StateFlow<UiState<List<Product>>> =
        combine(myPrimaryStore, isAdmin) { store, admin -> store to admin }
            .flatMapLatest { (store, admin) ->
                if (store == null) {
                    flowOf(emptyList())
                } else {
                    repository.observeStoreProducts(store.id, store.ownerUid, admin)
                }
            }
            .toUiState(TAG, "Failed to load merchant products")

    val merchantInquiriesState: StateFlow<UiState<List<OrderInquiry>>> =
        repository.observeMerchantInquiries(currentUserId)
            .toUiState(TAG, "Failed to load merchant inquiries")

    val adminStoresState: StateFlow<UiState<List<Store>>> =
        isAdmin.flatMapLatest { admin ->
            if (admin) repository.observeAllStoresForAdmin() else flowOf(emptyList())
        }.toUiState(TAG, "Failed to load admin stores")

    val adminPlansState: StateFlow<UiState<List<SubscriptionPlan>>> =
        isAdmin.flatMapLatest { admin ->
            if (admin) repository.observeAllPlansForAdmin() else flowOf(emptyList())
        }.toUiState(TAG, "Failed to load admin plans")

    val adminInquiriesState: StateFlow<UiState<List<OrderInquiry>>> =
        isAdmin.flatMapLatest { admin ->
            if (admin) repository.observeAllInquiriesForAdmin() else flowOf(emptyList())
        }.toUiState(TAG, "Failed to load admin inquiries")

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(_lang.value.code)
        }
    }

    fun toggleLanguage() {
        _lang.value = _lang.value.toggle()
    }

    fun selectTab(tab: MainTab) {
        _activeTab.value = tab
    }

    fun openStorefront(store: Store) {
        if (_selectedStore.value.id != store.id && _cartStoreId.value != null && _cartStoreId.value != store.id) {
            _cartItems.value = emptyList()
            _cartStoreId.value = null
        }
        _selectedStore.value = store
        _activeTab.value = MainTab.STOREFRONT
    }

    fun addToCart(product: Product) {
        val store = _selectedStore.value
        if (!store.active) {
            emitMessage(if (_lang.value == AppLang.AR) "هذا المتجر متوقف مؤقتاً" else "Store is paused")
            return
        }
        if (!product.available) {
            emitMessage(if (_lang.value == AppLang.AR) "المنتج غير متوفر حالياً" else "Product unavailable")
            return
        }
        val currentCartStore = _cartStoreId.value
        val baseItems = if (currentCartStore != null && currentCartStore != store.id) {
            emptyList()
        } else {
            _cartItems.value
        }
        _cartStoreId.value = store.id
        _cartItems.value = CartAndWhatsAppEngine.addOrIncrementItem(baseItems, CartItem(product, 1))
        val msg = if (_lang.value == AppLang.AR) {
            "تمت إضافة ${product.nameAr} إلى السلة"
        } else {
            "Added ${product.nameEn.ifBlank { product.nameAr }} to cart"
        }
        emitMessage(msg)
    }

    fun updateCartQuantity(productId: String, newQuantity: Int) {
        val updated = CartAndWhatsAppEngine.updateItemQuantity(_cartItems.value, productId, newQuantity)
        _cartItems.value = updated
        if (updated.isEmpty()) {
            _cartStoreId.value = null
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _cartStoreId.value = null
    }

    fun submitOrderInquiry(
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        notes: String,
        onReadyForWhatsApp: (whatsappUri: String, messageText: String) -> Unit,
    ) {
        val store = _selectedStore.value
        val items = _cartItems.value
        val currentLang = _lang.value
        val validation = CartAndWhatsAppEngine.validateCheckoutInput(
            customerName = customerName,
            customerPhone = customerPhone,
            deliveryAddress = deliveryAddress,
            notes = notes,
            items = items,
            lang = currentLang,
        )
        if (!validation.valid) {
            emitMessage(validation.errorMessage ?: "Invalid order data")
            return
        }

        val totals = CartAndWhatsAppEngine.calculateCartTotals(items)
        val summary = CartAndWhatsAppEngine.buildItemsSummary(items, currentLang)
        val waMessage = CartAndWhatsAppEngine.buildWhatsAppOrderMessage(
            store = store,
            items = items,
            customerName = customerName,
            customerPhone = customerPhone,
            deliveryAddress = deliveryAddress,
            notes = notes,
            lang = currentLang,
        )
        val waUri = CartAndWhatsAppEngine.buildWhatsAppUriString(store.whatsappPhone, waMessage)

        val inquiry = OrderInquiry(
            id = if (store.demoMode) "demo_${System.currentTimeMillis()}" else "",
            storeId = store.id,
            storeName = store.name,
            storeOwnerUid = store.ownerUid,
            customerUid = currentUserId,
            customerName = customerName.trim(),
            customerPhone = customerPhone.trim(),
            deliveryAddress = deliveryAddress.trim(),
            notes = notes.trim(),
            itemsSummary = summary,
            itemCount = totals.totalItems.toLong(),
            totalAmount = totals.subtotal,
            currency = store.currency,
            status = "pending_whatsapp",
        )

        if (store.demoMode) {
            _localDemoInquiries.value = listOf(inquiry) + _localDemoInquiries.value
            clearCart()
            onReadyForWhatsApp(waUri, waMessage)
            emitMessage(
                if (currentLang == AppLang.AR) {
                    "تم تجهيز رسالة واتساب للطلب التجريبي (لا يتأكد الطلب إلا بعد رد التاجر)"
                } else {
                    "WhatsApp order inquiry prepared (order stays pending until merchant confirms)"
                }
            )
            return
        }

        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.submitOrderInquiry(inquiry)
            _isBusy.value = false
            result.onSuccess {
                clearCart()
                onReadyForWhatsApp(waUri, waMessage)
                emitMessage(
                    if (currentLang == AppLang.AR) {
                        "تم حفظ الطلب المبدئي وفتح واتساب"
                    } else {
                        "Order inquiry saved and WhatsApp opened"
                    }
                )
            }.onFailure { err ->
                emitMessage(err.localizedMessage ?: "Failed to record order inquiry")
            }
        }
    }

    fun saveMerchantStore(
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
    ) {
        val currentLang = _lang.value
        val cleanSlug = CartAndWhatsAppEngine.sanitizeSlug(slug)
        val validation = CartAndWhatsAppEngine.validateStoreInput(
            name = name,
            slug = cleanSlug,
            city = city,
            whatsappPhone = whatsappPhone,
            description = description,
            lang = currentLang,
        )
        if (!validation.valid) {
            emitMessage(validation.errorMessage ?: "Invalid store input")
            return
        }

        val cleanedPhone = whatsappPhone.trim().replace(" ", "").replace("-", "")
        val store = Store(
            id = existingStoreId,
            ownerUid = currentUserId,
            name = name.trim(),
            slug = cleanSlug,
            description = description.trim(),
            city = city.trim(),
            whatsappPhone = cleanedPhone,
            currency = currency,
            accentKey = accentKey,
            coverPreset = coverPreset,
            logoUri = logoUri,
            active = active,
        )

        viewModelScope.launch {
            _isBusy.value = true
            val result = if (existingStoreId.isBlank()) {
                repository.createStore(store)
            } else {
                repository.updateStore(store).map { existingStoreId }
            }
            _isBusy.value = false
            result.onSuccess {
                emitMessage(if (currentLang == AppLang.AR) "تم حفظ بيانات المتجر" else "Store saved")
            }.onFailure { err ->
                emitMessage(err.localizedMessage ?: "Failed to save store")
            }
        }
    }

    fun saveCategory(
        storeId: String,
        categoryId: String,
        nameAr: String,
        nameEn: String,
        sortOrder: Long,
        active: Boolean,
    ) {
        val currentLang = _lang.value
        if (nameAr.trim().isEmpty()) {
            emitMessage(if (currentLang == AppLang.AR) "اسم القسم مطلوب" else "Category name is required")
            return
        }
        val category = Category(
            id = categoryId,
            storeId = storeId,
            ownerUid = currentUserId,
            nameAr = nameAr.trim().take(60),
            nameEn = nameEn.trim().ifBlank { nameAr.trim() }.take(60),
            sortOrder = sortOrder.coerceIn(0L, 500L),
            active = active,
        )
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.upsertCategory(category)
            _isBusy.value = false
            res.onSuccess {
                emitMessage(if (currentLang == AppLang.AR) "تم حفظ القسم" else "Category saved")
            }.onFailure { err ->
                emitMessage(err.localizedMessage ?: "Failed to save category")
            }
        }
    }

    fun deleteCategory(storeId: String, categoryId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.deleteCategory(storeId, categoryId)
            _isBusy.value = false
            res.onFailure { err -> emitMessage(err.localizedMessage ?: "Failed to delete category") }
        }
    }

    fun saveProduct(
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
    ) {
        val currentLang = _lang.value
        val validation = CartAndWhatsAppEngine.validateProductInput(
            nameAr = nameAr,
            priceText = priceText,
            stockText = stockText,
            categoryId = categoryId,
            lang = currentLang,
        )
        if (!validation.valid) {
            emitMessage(validation.errorMessage ?: "Invalid product")
            return
        }

        val product = Product(
            id = productId,
            storeId = storeId,
            ownerUid = currentUserId,
            categoryId = categoryId,
            nameAr = nameAr.trim().take(100),
            nameEn = nameEn.trim().ifBlank { nameAr.trim() }.take(100),
            descriptionAr = descriptionAr.trim().take(600),
            descriptionEn = descriptionAr.trim().take(600),
            price = priceText.toDouble(),
            stockCount = stockText.toLong(),
            available = available,
            imagePreset = imagePreset,
            imageUri = imageUri,
        )
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.upsertProduct(product)
            _isBusy.value = false
            res.onSuccess {
                emitMessage(if (currentLang == AppLang.AR) "تم حفظ المنتج" else "Product saved")
            }.onFailure { err ->
                emitMessage(err.localizedMessage ?: "Failed to save product")
            }
        }
    }

    fun deleteProduct(storeId: String, productId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.deleteProduct(storeId, productId)
            _isBusy.value = false
            res.onFailure { err -> emitMessage(err.localizedMessage ?: "Failed to delete product") }
        }
    }

    fun updateInquiryStatus(inquiry: OrderInquiry, newStatus: String) {
        if (!CartAndWhatsAppEngine.canMutateInquiryStatus(inquiry, currentUserId, isAdmin.value)) {
            emitMessage(if (_lang.value == AppLang.AR) "لا يمكن تعديل طلب مؤكد أو ملغي" else "Terminal orders cannot be modified")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateInquiryStatus(inquiry.id, newStatus)
            _isBusy.value = false
            res.onSuccess {
                emitMessage(if (_lang.value == AppLang.AR) "تم تحديث حالة الطلب" else "Order status updated")
            }.onFailure { err ->
                emitMessage(err.localizedMessage ?: "Failed to update status")
            }
        }
    }

    fun adminUpdateStoreSubscription(
        storeId: String,
        verified: Boolean,
        active: Boolean,
        planId: String,
        subscriptionStatus: String,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.adminUpdateStoreSubscription(
                storeId = storeId,
                verified = verified,
                active = active,
                planId = planId,
                subscriptionStatus = subscriptionStatus,
            )
            _isBusy.value = false
            res.onSuccess {
                emitMessage(if (_lang.value == AppLang.AR) "تم تحديث اشتراك المتجر" else "Store subscription updated")
            }.onFailure { err ->
                emitMessage(err.localizedMessage ?: "Admin update failed")
            }
        }
    }

    fun adminUpsertPlan(plan: SubscriptionPlan) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.adminUpsertPlan(plan)
            _isBusy.value = false
            res.onSuccess {
                emitMessage(if (_lang.value == AppLang.AR) "تم حفظ الباقة" else "Plan saved")
            }.onFailure { err ->
                emitMessage(err.localizedMessage ?: "Failed to save plan")
            }
        }
    }

    fun seedDefaultPlansToCloud() {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.seedDefaultPlans()
            _isBusy.value = false
            res.onSuccess {
                emitMessage(if (_lang.value == AppLang.AR) "تمت مزامنة الباقات الافتراضية" else "Default plans seeded")
            }.onFailure { err ->
                emitMessage(err.localizedMessage ?: "Seed failed")
            }
        }
    }

    private fun emitMessage(msg: String) {
        _snackbarEvents.tryEmit(msg)
    }

    private companion object {
        const val TAG = "SouqiVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
        const val BOOTSTRAPPED_ADMIN_EMAIL = "alhawa.alhamdani@gmail.com"
    }
}
