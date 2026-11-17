package com.example.ui.i18n

import androidx.compose.ui.unit.LayoutDirection
import java.text.NumberFormat
import java.util.Locale

enum class AppLang(val code: String, val layoutDirection: LayoutDirection) {
    AR("ar", LayoutDirection.Rtl),
    EN("en", LayoutDirection.Ltr);

    fun toggle(): AppLang = if (this == AR) EN else AR
}

data class AppStrings(
    val lang: AppLang,
) {
    private val isAr get() = lang == AppLang.AR

    val appTitle get() = if (isAr) "سوقي" else "Souqi"
    val appTagline get() = if (isAr) "منصة المتاجر والطلب عبر واتساب" else "WhatsApp-First Multi-Tenant Storefronts"
    val toggleLangLabel get() = if (isAr) "EN" else "عربي"

    // Navigation Tabs
    val navExplore get() = if (isAr) "السوق" else "Explore"
    val navStorefront get() = if (isAr) "المتجر" else "Storefront"
    val navMerchant get() = if (isAr) "التاجر" else "Merchant"
    val navAdmin get() = if (isAr) "الإدارة" else "Admin"

    // Landing & Auth
    val heroBadge get() = if (isAr) "مصمم للتجار في اليمن والعالم العربي" else "Built for Yemen & Arab Merchants"
    val heroHeadline get() = if (isAr) "أنشئ متجرك الاحترافي واستقبل طلباتك مباشرة عبر واتساب" else "Launch your storefront in minutes and receive orders on WhatsApp"
    val heroSubhead get() = if (isAr) "لا حاجة لبوابات دفع معقدة. شارك رابط متجرك، اعرض منتجاتك بوضوح، ودع عملاءك يطلبون بضغطة زر على واتساب." else "No complex payment gateways needed. Share your store link, showcase products cleanly, and receive formatted orders straight on WhatsApp."
    val signInGoogle get() = if (isAr) "المتابعة بحساب Google" else "Continue with Google"
    val signOut get() = if (isAr) "تسجيل الخروج" else "Sign Out"
    val whySouqi get() = if (isAr) "لماذا سوقي؟" else "Why Souqi?"
    val howItWorks get() = if (isAr) "كيف تعمل المنصة؟" else "How It Works"
    val pricingTitle get() = if (isAr) "باقات الاشتراك" else "Pricing Plans"
    val faqTitle get() = if (isAr) "الأسئلة الشائعة" else "Frequently Asked Questions"
    val footerCopy get() = if (isAr) "سوقي — تمكين التجارة المحلية في اليمن والعالم العربي" else "Souqi — Empowering local businesses across Yemen & the Arab world"

    // Explore
    val searchStoresHint get() = if (isAr) "ابحث عن متجر أو مدينة..." else "Search stores or cities..."
    val demoStoresHeader get() = if (isAr) "متاجر عرض تفاعلية (تجريبية)" else "Interactive Demo Storefronts"
    val liveStoresHeader get() = if (isAr) "متاجر التجار السحابية" else "Live Cloud Stores"
    val demoBadge get() = if (isAr) "متجر تجريبي" else "Demo Store"
    val verifiedBadge get() = if (isAr) "متجر موثق" else "Verified"
    val openStoreBtn get() = if (isAr) "تصفح المتجر" else "Browse Store"
    val createYourStoreCta get() = if (isAr) "أنشئ متجرك الآن" else "Create Your Store"
    val noStoresFound get() = if (isAr) "لا توجد متاجر مطابقة للبحث" else "No matching stores found"

    // Storefront & Cart
    val searchProductsHint get() = if (isAr) "ابحث في منتجات المتجر..." else "Search store products..."
    val allCategories get() = if (isAr) "الكل" else "All"
    val availableLabel get() = if (isAr) "متوفر" else "In Stock"
    val unavailableLabel get() = if (isAr) "غير متوفر" else "Out of Stock"
    val addToCart get() = if (isAr) "أضف للسلة" else "Add to Cart"
    val shareStore get() = if (isAr) "مشاركة الرابط" else "Share Link"
    val cartTitle get() = if (isAr) "سلة المشتريات" else "Shopping Cart"
    val cartEmpty get() = if (isAr) "سلتك فارغة حالياً. أضف منتجات من المتجر للطلب عبر واتساب." else "Your cart is empty. Add products to place a WhatsApp order."
    val subtotalLabel get() = if (isAr) "الإجمالي التقديري" else "Estimated Total"
    val customerNameLabel get() = if (isAr) "الاسم الكامل *" else "Full Name *"
    val customerPhoneLabel get() = if (isAr) "رقم الهاتف / واتساب *" else "Phone / WhatsApp *"
    val deliveryAddressLabel get() = if (isAr) "عنوان التوصيل (المدينة والحي) *" else "Delivery Address (City & Street) *"
    val orderNotesLabel get() = if (isAr) "ملاحظات إضافية (اختياري)" else "Order Notes (Optional)"
    val orderConfirmationDisclaimer get() = if (isAr) "تنبيه: إرسال الطلب يفتح محادثة واتساب مع التاجر. لا يُعد الطلب مؤكداً إلا بعد تأكيد التاجر." else "Notice: Sending opens a WhatsApp chat with the merchant. Orders are NOT confirmed until the merchant confirms."
    val sendViaWhatsApp get() = if (isAr) "إرسال الطلب عبر واتساب" else "Send Order via WhatsApp"
    val copyOrderMessage get() = if (isAr) "نسخ نص الطلب" else "Copy Order Text"
    val clearCart get() = if (isAr) "تفريغ السلة" else "Clear Cart"
    val storePausedNotice get() = if (isAr) "هذا المتجر متوقف مؤقتاً عن استقبال الطلبات." else "This store is temporarily paused."

    // Merchant Dashboard
    val merchantOnboardingTitle get() = if (isAr) "أنشئ متجرك الإلكتروني" else "Create Your Storefront"
    val merchantOverviewTab get() = if (isAr) "ملخص" else "Overview"
    val merchantCatalogTab get() = if (isAr) "المنتجات" else "Catalog"
    val merchantInquiriesTab get() = if (isAr) "الطلبات" else "Inquiries"
    val merchantSettingsTab get() = if (isAr) "الإعدادات" else "Settings"
    val addCategoryBtn get() = if (isAr) "قسم جديد" else "New Category"
    val addProductBtn get() = if (isAr) "منتج جديد" else "New Product"
    val saveChangesBtn get() = if (isAr) "حفظ التغييرات" else "Save Changes"
    val pickImageBtn get() = if (isAr) "اختيار صورة من الجهاز" else "Pick Photo from Device"

    // Admin Dashboard
    val adminStoresTab get() = if (isAr) "المتاجر" else "Stores"
    val adminPlansTab get() = if (isAr) "الباقات" else "Plans"
    val adminReportsTab get() = if (isAr) "التقارير" else "Reports"
    val adminRestrictedTitle get() = if (isAr) "صلاحية مشرف المنصة" else "Platform Admin Access"
    val adminRestrictedDesc get() = if (isAr) "إدارة الباقات والاشتراكات وتوثيق المتاجر متاحة لحسابات المشرفين المعتمدين فقط. يمكنك متابعة حالة باقتك الحالية أدناه وطلب التفعيل اليدوي عبر الدعم." else "Plan management, manual subscription activation, and store verification are restricted to verified platform administrators."
    val seedDataBtn get() = if (isAr) "نشر الباقات ومتجر العرض للسحابة" else "Seed Default Plans & Showcase Store"

    fun statusLabel(status: String): String = when (status) {
        "pending_whatsapp" -> if (isAr) "بانتظار التأكيد" else "Pending Confirmation"
        "contacted" -> if (isAr) "تم التواصل" else "Contacted"
        "confirmed" -> if (isAr) "مؤكد" else "Confirmed"
        "cancelled" -> if (isAr) "ملغي" else "Cancelled"
        "trial" -> if (isAr) "تجريبي" else "Trial"
        "active" -> if (isAr) "نشط" else "Active"
        "pending" -> if (isAr) "بانتظار التفعيل" else "Pending Activation"
        "suspended" -> if (isAr) "موقوف" else "Suspended"
        else -> status
    }

    fun formatCurrency(amount: Double, currency: String): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = if (currency == "USD") 2 else 0
            minimumFractionDigits = if (currency == "USD" && amount % 1.0 != 0.0) 2 else 0
        }
        val formatted = formatter.format(amount)
        return when (currency) {
            "YER" -> if (isAr) "$formatted ر.ي" else "$formatted YER"
            "SAR" -> if (isAr) "$formatted ر.س" else "$formatted SAR"
            "USD" -> "$$formatted"
            else -> "$formatted $currency"
        }
    }
}
