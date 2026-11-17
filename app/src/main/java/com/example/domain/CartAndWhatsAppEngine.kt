package com.example.domain

import com.example.model.CartItem
import com.example.model.OrderInquiry
import com.example.model.Store
import com.example.ui.i18n.AppLang
import com.example.ui.i18n.AppStrings
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class CartTotals(
    val totalItems: Int,
    val subtotal: Double,
)

data class ValidationResult(
    val valid: Boolean,
    val errorMessage: String? = null,
)

object CartAndWhatsAppEngine {

    fun calculateCartTotals(items: List<CartItem>): CartTotals {
        val validItems = items.filter { it.quantity > 0 && it.product.price > 0.0 }
        val count = validItems.sumOf { it.quantity }
        val subtotal = validItems.sumOf { it.product.price * it.quantity }
        return CartTotals(totalItems = count, subtotal = subtotal)
    }

    fun addOrIncrementItem(
        current: List<CartItem>,
        item: CartItem,
    ): List<CartItem> {
        if (item.quantity <= 0) return current
        val existing = current.find { it.product.id == item.product.id }
        return if (existing == null) {
            current + item
        } else {
            current.map {
                if (it.product.id == item.product.id) {
                    it.copy(quantity = (it.quantity + item.quantity).coerceAtMost(99))
                } else {
                    it
                }
            }
        }
    }

    fun updateItemQuantity(
        current: List<CartItem>,
        productId: String,
        newQuantity: Int,
    ): List<CartItem> {
        if (newQuantity <= 0) {
            return current.filterNot { it.product.id == productId }
        }
        return current.map {
            if (it.product.id == productId) {
                it.copy(quantity = newQuantity.coerceAtMost(99))
            } else {
                it
            }
        }
    }

    fun normalizeWhatsAppPhone(raw: String): String {
        val trimmed = raw.trim()
        val digitsOnly = buildString {
            for (ch in trimmed) {
                if (ch.isDigit()) append(ch)
            }
        }
        return if (digitsOnly.startsWith("00")) {
            digitsOnly.drop(2)
        } else {
            digitsOnly
        }
    }

    fun sanitizeSlug(raw: String): String {
        val lowered = raw.trim().lowercase()
        val mapped = buildString {
            for (ch in lowered) {
                when {
                    ch in 'a'..'z' || ch in '0'..'9' -> append(ch)
                    ch == '-' || ch == '_' || ch == ' ' -> append('-')
                }
            }
        }.replace(Regex("-+"), "-").trim('-')
        return mapped
    }

    fun buildItemsSummary(items: List<CartItem>, lang: AppLang): String {
        return items.filter { it.quantity > 0 }.joinToString(" | ") { item ->
            val name = if (lang == AppLang.AR) item.product.nameAr else item.product.nameEn.ifBlank { item.product.nameAr }
            "$name × ${item.quantity}"
        }
    }

    fun buildWhatsAppOrderMessage(
        store: Store,
        items: List<CartItem>,
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        notes: String,
        lang: AppLang,
    ): String {
        val strings = AppStrings(lang)
        val totals = calculateCartTotals(items)
        val isAr = lang == AppLang.AR
        val sb = StringBuilder()

        if (isAr) {
            sb.appendLine("مرحباً ${store.name}، أود إرسال طلب جديد عبر منصة سوقي:")
            sb.appendLine("------------------------------")
            items.filter { it.quantity > 0 }.forEachIndexed { idx, item ->
                val unitPrice = strings.formatCurrency(item.product.price, store.currency)
                val lineTotal = strings.formatCurrency(item.lineTotal, store.currency)
                sb.appendLine("${idx + 1}. ${item.product.nameAr} × ${item.quantity} ($unitPrice) = $lineTotal")
            }
            sb.appendLine("------------------------------")
            sb.appendLine("عدد القطع: ${totals.totalItems}")
            sb.appendLine("الإجمالي التقديري: ${strings.formatCurrency(totals.subtotal, store.currency)}")
            sb.appendLine("الاسم: ${customerName.trim()}")
            sb.appendLine("الهاتف: ${customerPhone.trim()}")
            sb.appendLine("العنوان: ${deliveryAddress.trim()}")
            if (notes.isNotBlank()) {
                sb.appendLine("ملاحظات: ${notes.trim()}")
            }
            sb.append("تنبيه: هذا طلب مبدئي ولا يُعد مؤكداً إلا بعد تأكيد التاجر.")
        } else {
            sb.appendLine("Hello ${store.name}, I would like to place a new order inquiry via Souqi:")
            sb.appendLine("------------------------------")
            items.filter { it.quantity > 0 }.forEachIndexed { idx, item ->
                val pName = item.product.nameEn.ifBlank { item.product.nameAr }
                val unitPrice = strings.formatCurrency(item.product.price, store.currency)
                val lineTotal = strings.formatCurrency(item.lineTotal, store.currency)
                sb.appendLine("${idx + 1}. $pName × ${item.quantity} ($unitPrice) = $lineTotal")
            }
            sb.appendLine("------------------------------")
            sb.appendLine("Items: ${totals.totalItems}")
            sb.appendLine("Estimated Total: ${strings.formatCurrency(totals.subtotal, store.currency)}")
            sb.appendLine("Customer: ${customerName.trim()}")
            sb.appendLine("Phone: ${customerPhone.trim()}")
            sb.appendLine("Address: ${deliveryAddress.trim()}")
            if (notes.isNotBlank()) {
                sb.appendLine("Notes: ${notes.trim()}")
            }
            sb.append("Notice: This order is pending and NOT confirmed until confirmed by the merchant.")
        }

        return sb.toString()
    }

    fun buildWhatsAppUriString(whatsappPhone: String, message: String): String {
        val normalizedPhone = normalizeWhatsAppPhone(whatsappPhone)
        val encodedText = URLEncoder.encode(message, StandardCharsets.UTF_8.name())
            .replace("+", "%20")
        return "https://wa.me/$normalizedPhone?text=$encodedText"
    }

    fun validateCheckoutInput(
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        notes: String,
        items: List<CartItem>,
        lang: AppLang,
    ): ValidationResult {
        val isAr = lang == AppLang.AR
        if (items.none { it.quantity > 0 }) {
            return ValidationResult(false, if (isAr) "السلة فارغة" else "Cart is empty")
        }
        if (customerName.trim().length < 2 || customerName.trim().length > 80) {
            return ValidationResult(false, if (isAr) "يرجى إدخال اسم صحيح (2-80 حرفاً)" else "Enter a valid name (2-80 chars)")
        }
        val phoneDigits = normalizeWhatsAppPhone(customerPhone)
        if (phoneDigits.length < 7 || phoneDigits.length > 20) {
            return ValidationResult(false, if (isAr) "يرجى إدخال رقم هاتف صحيح" else "Enter a valid phone number")
        }
        if (deliveryAddress.trim().length < 3 || deliveryAddress.trim().length > 240) {
            return ValidationResult(false, if (isAr) "يرجى إدخال عنوان التوصيل" else "Enter a valid delivery address")
        }
        if (notes.trim().length > 400) {
            return ValidationResult(false, if (isAr) "الملاحظات طويلة جداً" else "Notes exceed 400 chars")
        }
        return ValidationResult(true)
    }

    fun validateStoreInput(
        name: String,
        slug: String,
        city: String,
        whatsappPhone: String,
        description: String,
        lang: AppLang,
    ): ValidationResult {
        val isAr = lang == AppLang.AR
        if (name.trim().length < 2 || name.trim().length > 80) {
            return ValidationResult(false, if (isAr) "اسم المتجر يجب أن يكون 2-80 حرفاً" else "Store name must be 2-80 chars")
        }
        val cleanSlug = sanitizeSlug(slug)
        if (cleanSlug.length < 2 || cleanSlug.length > 60 || !Regex("^[a-z0-9\\-]+$").matches(cleanSlug)) {
            return ValidationResult(false, if (isAr) "الرابط يجب أن يحتوي على أحرف إنجليزية صغيرة وأرقام وشرطة فقط" else "Slug must contain lowercase letters, numbers, and hyphens")
        }
        if (city.trim().isEmpty() || city.trim().length > 60) {
            return ValidationResult(false, if (isAr) "يرجى إدخال المدينة" else "Please enter a city")
        }
        val cleanedPhone = whatsappPhone.trim().replace(" ", "").replace("-", "")
        if (cleanedPhone.length < 7 || cleanedPhone.length > 20 || !Regex("^[+0-9]+$").matches(cleanedPhone)) {
            return ValidationResult(false, if (isAr) "رقم الواتساب غير صالح (استخدم رمز الدولة مثل +96777...)" else "Invalid WhatsApp number (use country code e.g. +96777...)")
        }
        if (description.trim().length > 500) {
            return ValidationResult(false, if (isAr) "الوصف يجب ألا يتجاوز 500 حرف" else "Description must be <= 500 chars")
        }
        return ValidationResult(true)
    }

    fun validateProductInput(
        nameAr: String,
        priceText: String,
        stockText: String,
        categoryId: String,
        lang: AppLang,
    ): ValidationResult {
        val isAr = lang == AppLang.AR
        if (nameAr.trim().isEmpty() || nameAr.trim().length > 100) {
            return ValidationResult(false, if (isAr) "اسم المنتج مطلوب (حتى 100 حرف)" else "Product name is required (max 100 chars)")
        }
        if (categoryId.isBlank()) {
            return ValidationResult(false, if (isAr) "يرجى اختيار قسم للمنتج" else "Please select a category")
        }
        val price = priceText.toDoubleOrNull()
        if (price == null || price <= 0.0 || price > 100_000_000.0) {
            return ValidationResult(false, if (isAr) "يرجى إدخال سعر أكبر من صفر" else "Price must be greater than 0")
        }
        val stock = stockText.toLongOrNull()
        if (stock == null || stock < 0L || stock > 100_000L) {
            return ValidationResult(false, if (isAr) "الكمية يجب أن تكون رقماً غير سالب" else "Stock count must be 0 or greater")
        }
        return ValidationResult(true)
    }

    // Tenant isolation helpers mirroring firestore.rules
    fun canMutateStore(store: Store, actorUid: String, isAdmin: Boolean): Boolean {
        if (actorUid.isBlank()) return false
        return isAdmin || store.ownerUid == actorUid
    }

    fun canMutateInquiryStatus(
        inquiry: OrderInquiry,
        actorUid: String,
        isAdmin: Boolean,
    ): Boolean {
        if (actorUid.isBlank()) return false
        if (isAdmin) return true
        val isTerminal = inquiry.status == "confirmed" || inquiry.status == "cancelled"
        return inquiry.storeOwnerUid == actorUid && !isTerminal
    }
}
