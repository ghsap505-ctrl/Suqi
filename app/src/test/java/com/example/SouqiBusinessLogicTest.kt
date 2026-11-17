package com.example

import com.example.domain.CartAndWhatsAppEngine
import com.example.domain.SampleCatalog
import com.example.model.CartItem
import com.example.model.OrderInquiry
import com.example.model.Product
import com.example.model.Store
import com.example.ui.i18n.AppLang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SouqiBusinessLogicTest {

    private val demoStore = SampleCatalog.demoStores.first()
    private val sampleProductA = Product(
        id = "p1",
        storeId = demoStore.id,
        ownerUid = demoStore.ownerUid,
        categoryId = "c1",
        nameAr = "عسل سدر",
        nameEn = "Sidr Honey",
        price = 25000.0,
        stockCount = 10L,
        available = true,
    )
    private val sampleProductB = Product(
        id = "p2",
        storeId = demoStore.id,
        ownerUid = demoStore.ownerUid,
        categoryId = "c2",
        nameAr = "بن حرازي",
        nameEn = "Harazi Coffee",
        price = 8500.0,
        stockCount = 15L,
        available = true,
    )

    @Test
    fun `cart totals calculate item count and subtotal accurately`() {
        var cart = emptyList<CartItem>()
        cart = CartAndWhatsAppEngine.addOrIncrementItem(cart, CartItem(sampleProductA, 2))
        cart = CartAndWhatsAppEngine.addOrIncrementItem(cart, CartItem(sampleProductB, 3))
        val totals = CartAndWhatsAppEngine.calculateCartTotals(cart)
        assertEquals(5, totals.totalItems)
        assertEquals(75500.0, totals.subtotal, 0.001)

        val updated = CartAndWhatsAppEngine.updateItemQuantity(cart, "p1", 1)
        val updatedTotals = CartAndWhatsAppEngine.calculateCartTotals(updated)
        assertEquals(4, updatedTotals.totalItems)
        assertEquals(50500.0, updatedTotals.subtotal, 0.001)

        val removed = CartAndWhatsAppEngine.updateItemQuantity(updated, "p2", 0)
        assertEquals(1, removed.size)
        assertEquals("p1", removed.first().product.id)
    }

    @Test
    fun `whatsapp order message includes store, items, total, and non-confirmation notice`() {
        val items = listOf(CartItem(sampleProductA, 2))
        val msgAr = CartAndWhatsAppEngine.buildWhatsAppOrderMessage(
            store = demoStore,
            items = items,
            customerName = "أحمد الحميري",
            customerPhone = "+967 771 234 567",
            deliveryAddress = "صنعاء - حدة",
            notes = "عصراً",
            lang = AppLang.AR,
        )
        assertTrue(msgAr.contains(demoStore.name))
        assertTrue(msgAr.contains("عسل سدر × 2"))
        assertTrue(msgAr.contains("50,000"))
        assertTrue(msgAr.contains("لا يُعد مؤكداً إلا بعد تأكيد التاجر"))

        val uri = CartAndWhatsAppEngine.buildWhatsAppUriString(demoStore.whatsappPhone, msgAr)
        assertTrue(uri.startsWith("https://wa.me/967770123456?text="))
        assertFalse(uri.contains(" "))
    }

    @Test
    fun `phone normalization and slug sanitization handle edge cases`() {
        assertEquals("967771234567", CartAndWhatsAppEngine.normalizeWhatsAppPhone("00967 771-234-567"))
        assertEquals("967733000111", CartAndWhatsAppEngine.normalizeWhatsAppPhone("+967 733 000 111"))
        assertEquals("sanaa-honey-shop", CartAndWhatsAppEngine.sanitizeSlug("  Sanaa Honey_Shop!!  "))
    }

    @Test
    fun `input validation rejects invalid checkout and store payloads`() {
        val badCheckout = CartAndWhatsAppEngine.validateCheckoutInput(
            customerName = "A",
            customerPhone = "123",
            deliveryAddress = "x",
            notes = "",
            items = listOf(CartItem(sampleProductA, 1)),
            lang = AppLang.AR,
        )
        assertFalse(badCheckout.valid)

        val badStore = CartAndWhatsAppEngine.validateStoreInput(
            name = "م",
            slug = "!",
            city = "",
            whatsappPhone = "abc",
            description = "",
            lang = AppLang.EN,
        )
        assertFalse(badStore.valid)
    }

    @Test
    fun `tenant isolation guards block cross-merchant store and terminal inquiry mutations`() {
        val store = Store(id = "s1", ownerUid = "merchant_1", name = "Shop")
        assertTrue(CartAndWhatsAppEngine.canMutateStore(store, "merchant_1", isAdmin = false))
        assertFalse(CartAndWhatsAppEngine.canMutateStore(store, "merchant_2", isAdmin = false))
        assertTrue(CartAndWhatsAppEngine.canMutateStore(store, "admin_uid", isAdmin = true))

        val pendingInquiry = OrderInquiry(id = "i1", storeOwnerUid = "merchant_1", status = "pending_whatsapp")
        assertTrue(CartAndWhatsAppEngine.canMutateInquiryStatus(pendingInquiry, "merchant_1", isAdmin = false))
        assertFalse(CartAndWhatsAppEngine.canMutateInquiryStatus(pendingInquiry, "merchant_2", isAdmin = false))

        val confirmedInquiry = pendingInquiry.copy(status = "confirmed")
        assertFalse(CartAndWhatsAppEngine.canMutateInquiryStatus(confirmedInquiry, "merchant_1", isAdmin = false))
        assertTrue(CartAndWhatsAppEngine.canMutateInquiryStatus(confirmedInquiry, "admin_uid", isAdmin = true))
    }
}
