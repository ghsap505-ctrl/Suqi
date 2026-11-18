import { AppLang } from "../types";

export function getStrings(lang: AppLang) {
  const isAr = lang === "ar";
  return {
    brand: isAr ? "سوقي" : "Souqi",
    tagline: isAr ? "متاجر واتساب للتجار في اليمن والعالم العربي" : "WhatsApp Storefronts for Arab Merchants",
    navHome: isAr ? "الرئيسية" : "Home",
    navExplore: isAr ? "السوق" : "Explore",
    navStore: isAr ? "المتجر" : "Storefront",
    navMerchant: isAr ? "لوحة التاجر" : "Merchant",
    navAdmin: isAr ? "الإدارة" : "Admin",
    langToggle: isAr ? "EN" : "عربي",
    signInGoogle: isAr ? "دخول عبر Google" : "Sign in with Google",
    signOut: isAr ? "خروج" : "Sign Out",
    demoNotice: isAr
      ? "وضع العرض التجريبي نشط (أضف مفاتيح Firebase في .env.local للمزامنة السحابية)"
      : "Demo Mode active (set Firebase keys in .env.local for live cloud sync)",
    heroBadge: isAr ? "بدون عمولات • مناسب للإنترنت الضعيف" : "0% Commission • Fast on Slow Networks",
    heroTitle: isAr
      ? "أنشئ متجرك الاحترافي خلال دقائق واستقبل طلباتك مباشرة عبر واتساب"
      : "Launch your storefront in minutes and receive orders straight on WhatsApp",
    heroSubtitle: isAr
      ? "حل عملي للتجار وأصحاب المحلات في اليمن والعالم العربي لعرض المنتجات ومشاركة رابط المتجر واستقبال طلبات منسقة دون الحاجة لبوابات دفع معقدة."
      : "Built for local shops across Yemen and the Arab world to showcase products, share a store link, and take formatted WhatsApp orders without complex payment gateways.",
    browseDemoBtn: isAr ? "جرّب المتجر التجريبي" : "Try Demo Storefront",
    openMerchantBtn: isAr ? "افتح متجرك الآن" : "Create Your Store",
    disclaimer: isAr
      ? "تنبيه: إرسال الطلب يفتح واتساب التاجر، ولا يُعد الطلب مؤكداً إلا بعد تأكيد التاجر."
      : "Notice: Submitting opens the merchant's WhatsApp. Your order is NOT confirmed until confirmed by the merchant.",
    statusLabel(status: string) {
      const map: Record<string, [string, string]> = {
        pending_whatsapp: ["بانتظار التأكيد", "Pending"],
        contacted: ["تم التواصل", "Contacted"],
        confirmed: ["مؤكد", "Confirmed"],
        cancelled: ["ملغي", "Cancelled"],
        trial: ["تجريبي", "Trial"],
        active: ["نشط", "Active"],
        pending: ["بانتظار التفعيل", "Pending"],
        suspended: ["موقوف", "Suspended"],
      };
      const pair = map[status];
      return pair ? (isAr ? pair[0] : pair[1]) : status;
    },
  };
}
