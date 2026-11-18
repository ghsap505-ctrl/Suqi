import React, { useMemo, useState } from "react";
import {
  AlertCircle,
  BadgeCheck,
  Check,
  Copy,
  MapPin,
  MessageCircle,
  Minus,
  Plus,
  Search,
  Share2,
  ShoppingBag,
  Trash2,
  X,
} from "lucide-react";
import { AppLang, CartItem, Category, OrderInquiry, Product, Store } from "../types";
import {
  buildItemsSummary,
  buildWhatsAppOrderMessage,
  buildWhatsAppUrl,
  calculateCartTotals,
  formatCurrency,
  validateCheckout,
} from "../lib/whatsapp";
import { getAccentClasses, ProductPresetVisual } from "./VisualHelpers";

interface StorefrontViewProps {
  lang: AppLang;
  store: Store | null;
  categories: Category[];
  products: Product[];
  loading: boolean;
  onRecordInquiry: (inquiry: Omit<OrderInquiry, "id" | "customerUid" | "status">) => Promise<void>;
  onBackToExplore: () => void;
}

export const StorefrontView: React.FC<StorefrontViewProps> = ({
  lang,
  store,
  categories,
  products,
  loading,
  onRecordInquiry,
  onBackToExplore,
}) => {
  const isAr = lang === "ar";
  const [selectedCat, setSelectedCat] = useState<string>("ALL");
  const [searchQuery, setSearchQuery] = useState("");
  const [detailProduct, setDetailProduct] = useState<Product | null>(null);
  const [cart, setCart] = useState<CartItem[]>([]);
  const [customerName, setCustomerName] = useState("");
  const [customerPhone, setCustomerPhone] = useState("");
  const [deliveryAddress, setDeliveryAddress] = useState("");
  const [notes, setNotes] = useState("");
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [lastOrderMsg, setLastOrderMsg] = useState<string | null>(null);
  const [copiedLink, setCopiedLink] = useState(false);
  const [copiedMsg, setCopiedMsg] = useState(false);
  const [mobileCartOpen, setMobileCartOpen] = useState(false);

  const filteredProducts = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return products.filter((p) => {
      if (selectedCat !== "ALL" && p.categoryId !== selectedCat) return false;
      if (!q) return true;
      return (
        p.nameAr.toLowerCase().includes(q) ||
        p.nameEn.toLowerCase().includes(q) ||
        p.descriptionAr.toLowerCase().includes(q)
      );
    });
  }, [products, selectedCat, searchQuery]);

  const totals = useMemo(() => calculateCartTotals(cart), [cart]);

  if (!store) {
    return (
      <div className="rounded-xl border border-[#E7E2D8] bg-white p-10 text-center space-y-3">
        <ShoppingBag className="w-8 h-8 text-slate-400 mx-auto" />
        <p className="font-bold text-slate-800">{isAr ? "لم يتم اختيار متجر" : "No store selected"}</p>
        <button
          type="button"
          onClick={onBackToExplore}
          className="px-4 py-2 rounded-lg bg-souqi-teal text-white text-sm font-semibold"
        >
          {isAr ? "تصفح المتاجر" : "Explore Stores"}
        </button>
      </div>
    );
  }

  const accent = getAccentClasses(store.accentKey);

  const addToCart = (product: Product) => {
    if (!product.available) return;
    setCart((prev) => {
      const idx = prev.findIndex((i) => i.product.id === product.id);
      if (idx >= 0) {
        const next = [...prev];
        next[idx] = { ...next[idx], quantity: Math.min(99, next[idx].quantity + 1) };
        return next;
      }
      return [...prev, { product, quantity: 1 }];
    });
  };

  const updateQty = (productId: string, delta: number) => {
    setCart((prev) =>
      prev
        .map((i) => (i.product.id === productId ? { ...i, quantity: i.quantity + delta } : i))
        .filter((i) => i.quantity > 0)
    );
  };

  const handleShareStore = async () => {
    const url = `${window.location.origin}${window.location.pathname}#/s/${store.slug}`;
    try {
      await navigator.clipboard.writeText(url);
      setCopiedLink(true);
      setTimeout(() => setCopiedLink(false), 2000);
    } catch {
      setCopiedLink(false);
    }
  };

  const handleSubmitWhatsApp = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    const err = validateCheckout({
      items: cart,
      customerName,
      customerPhone,
      deliveryAddress,
      notes,
      lang,
    });
    if (err) {
      setFormError(err);
      return;
    }

    const message = buildWhatsAppOrderMessage({
      store,
      items: cart,
      customerName,
      customerPhone,
      deliveryAddress,
      notes,
      lang,
    });
    const url = buildWhatsAppUrl(store.whatsappPhone, message);

    setSubmitting(true);
    try {
      await onRecordInquiry({
        storeId: store.id,
        storeName: store.name,
        storeOwnerUid: store.ownerUid,
        customerName: customerName.trim(),
        customerPhone: customerPhone.trim(),
        deliveryAddress: deliveryAddress.trim(),
        notes: notes.trim(),
        itemsSummary: buildItemsSummary(cart, lang),
        itemCount: totals.totalItems,
        totalAmount: totals.subtotal,
        currency: store.currency,
      });
      setLastOrderMsg(message);
      window.open(url, "_blank", "noopener,noreferrer");
    } catch (submitErr) {
      setFormError(submitErr instanceof Error ? submitErr.message : isAr ? "تعذر تسجيل الطلب" : "Failed to submit");
    } finally {
      setSubmitting(false);
    }
  };

  const copyLastMessage = async () => {
    if (!lastOrderMsg) return;
    try {
      await navigator.clipboard.writeText(lastOrderMsg);
      setCopiedMsg(true);
      setTimeout(() => setCopiedMsg(false), 2000);
    } catch {
      // Ignore clipboard failures
    }
  };

  const renderCartPanel = () => (
    <div className="rounded-xl border border-[#E7E2D8] bg-white p-5 space-y-4">
      <div className="flex items-center justify-between border-b border-[#EFEAE0] pb-3">
        <div className="flex items-center gap-2 font-bold text-slate-900">
          <ShoppingBag className="w-5 h-5 text-souqi-teal" />
          <span>{isAr ? "سلة الطلبات" : "Order Cart"}</span>
          {totals.totalItems > 0 && (
            <span className="px-2 py-0.5 rounded-full bg-teal-50 text-souqi-teal text-xs tabular-nums">
              {totals.totalItems}
            </span>
          )}
        </div>
        {cart.length > 0 && (
          <button
            type="button"
            onClick={() => setCart([])}
            className="text-xs text-slate-500 hover:text-red-600 inline-flex items-center gap-1"
          >
            <Trash2 className="w-3.5 h-3.5" />
            <span>{isAr ? "إفراغ" : "Clear"}</span>
          </button>
        )}
      </div>

      {cart.length === 0 ? (
        <p className="text-sm text-slate-500 py-4 text-center">
          {isAr ? "أضف منتجات من القائمة لإرسال طلبك عبر واتساب." : "Add items from the catalog to build a WhatsApp order."}
        </p>
      ) : (
        <form onSubmit={handleSubmitWhatsApp} className="space-y-4">
          <div className="space-y-2.5 max-h-60 overflow-y-auto pe-1">
            {cart.map((item) => (
              <div key={item.product.id} className="flex items-center justify-between gap-2 text-sm border-b border-stone-100 pb-2">
                <div className="min-w-0 flex-1">
                  <div className="font-medium text-slate-900 truncate">
                    {isAr ? item.product.nameAr : item.product.nameEn || item.product.nameAr}
                  </div>
                  <div className="text-xs text-slate-500 tabular-nums">
                    {formatCurrency(item.product.price * item.quantity, store.currency, lang)}
                  </div>
                </div>
                <div className="flex items-center gap-1.5 shrink-0">
                  <button
                    type="button"
                    onClick={() => updateQty(item.product.id, -1)}
                    aria-label={isAr ? "إنقاص الكمية" : "Decrease quantity"}
                    className="w-7 h-7 rounded-md border border-[#DFD8CA] flex items-center justify-center hover:bg-stone-100"
                  >
                    <Minus className="w-3.5 h-3.5" />
                  </button>
                  <span className="w-6 text-center font-semibold tabular-nums">{item.quantity}</span>
                  <button
                    type="button"
                    onClick={() => updateQty(item.product.id, 1)}
                    aria-label={isAr ? "زيادة الكمية" : "Increase quantity"}
                    className="w-7 h-7 rounded-md border border-[#DFD8CA] flex items-center justify-center hover:bg-stone-100"
                  >
                    <Plus className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            ))}
          </div>

          <div className="flex items-center justify-between font-bold text-slate-900 pt-1">
            <span>{isAr ? "الإجمالي التقديري" : "Estimated Total"}</span>
            <span className="text-souqi-teal tabular-nums">{formatCurrency(totals.subtotal, store.currency, lang)}</span>
          </div>

          <div className="space-y-2.5">
            <input
              type="text"
              value={customerName}
              onChange={(e) => setCustomerName(e.target.value)}
              placeholder={isAr ? "الاسم الكامل *" : "Full Name *"}
              className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm focus:outline-none focus:border-souqi-teal"
            />
            <input
              type="tel"
              dir="ltr"
              value={customerPhone}
              onChange={(e) => setCustomerPhone(e.target.value)}
              placeholder={isAr ? "رقم الهاتف (مثال: +967770000000) *" : "Phone (+967770000000) *"}
              className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm text-start focus:outline-none focus:border-souqi-teal"
            />
            <input
              type="text"
              value={deliveryAddress}
              onChange={(e) => setDeliveryAddress(e.target.value)}
              placeholder={isAr ? "عنوان التوصيل (المدينة / الحي) *" : "Delivery Address *"}
              className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm focus:outline-none focus:border-souqi-teal"
            />
            <textarea
              rows={2}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder={isAr ? "ملاحظات إضافية (اختياري)" : "Order notes (optional)"}
              className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm focus:outline-none focus:border-souqi-teal"
            />
          </div>

          <div className="rounded-lg bg-amber-50 border border-amber-200 p-3 flex items-start gap-2 text-xs text-amber-900">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>
              {isAr
                ? "تنبيه: إرسال الطلب يفتح واتساب التاجر، ولا يُعد الطلب مؤكداً إلا بعد تأكيد التاجر."
                : "Notice: Submitting sends a WhatsApp inquiry. Orders are NOT confirmed until confirmed by the merchant."}
            </span>
          </div>

          {formError && (
            <div className="rounded-lg bg-red-50 border border-red-200 p-2.5 text-xs text-red-700 font-medium">
              {formError}
            </div>
          )}

          <button
            type="submit"
            disabled={submitting}
            className="w-full py-3 px-4 rounded-xl bg-souqi-whatsapp hover:opacity-95 disabled:opacity-60 text-white font-bold text-sm inline-flex items-center justify-center gap-2 transition-opacity"
          >
            <MessageCircle className="w-4 h-4" />
            <span>{submitting ? (isAr ? "جاري التجهيز..." : "Preparing...") : isAr ? "إرسال الطلب عبر واتساب" : "Send Order via WhatsApp"}</span>
          </button>

          {lastOrderMsg && (
            <div className="rounded-lg bg-emerald-50 border border-emerald-200 p-3 space-y-2 text-xs text-emerald-900">
              <div className="font-semibold">
                {isAr ? "تم تجهيز رسالة الطلب بنجاح." : "WhatsApp order message prepared."}
              </div>
              <button
                type="button"
                onClick={copyLastMessage}
                className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-white border border-emerald-300 text-emerald-800 font-semibold"
              >
                {copiedMsg ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                <span>{copiedMsg ? (isAr ? "تم النسخ" : "Copied") : isAr ? "نسخ نص الطلب احتياطياً" : "Copy message"}</span>
              </button>
            </div>
          )}
        </form>
      )}
    </div>
  );

  return (
    <div className="space-y-6 pb-20 lg:pb-10">
      {/* Store Banner */}
      <section className={`rounded-2xl ${accent.banner} text-white p-6 sm:p-8`}>
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="space-y-2">
            <div className="flex items-center gap-2 flex-wrap">
              <h1 className="text-2xl sm:text-3xl font-bold">{store.name}</h1>
              {store.verified && (
                <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-white/15 text-xs font-semibold">
                  <BadgeCheck className="w-3.5 h-3.5" />
                  {isAr ? "متجر موثق" : "Verified"}
                </span>
              )}
            </div>
            <p className="text-sm sm:text-base text-white/90 max-w-2xl leading-relaxed">{store.description}</p>
            <div className="flex items-center flex-wrap gap-3 text-xs text-white/80 pt-1">
              <span className="inline-flex items-center gap-1">
                <MapPin className="w-3.5 h-3.5" />
                {store.city}
              </span>
              <span>•</span>
              <span dir="ltr">#{store.slug}</span>
              <span>•</span>
              <span>{store.currency}</span>
            </div>
          </div>

          <div className="flex items-center gap-2 shrink-0">
            <button
              type="button"
              onClick={handleShareStore}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-white/15 hover:bg-white/25 text-white text-xs sm:text-sm font-semibold transition-colors"
            >
              {copiedLink ? <Check className="w-4 h-4" /> : <Share2 className="w-4 h-4" />}
              <span>{copiedLink ? (isAr ? "تم نسخ الرابط" : "Link Copied") : isAr ? "مشاركة رابط المتجر" : "Share Link"}</span>
            </button>
          </div>
        </div>
      </section>

      <div className="grid lg:grid-cols-12 gap-6 items-start">
        {/* Catalog */}
        <div className="lg:col-span-8 space-y-5">
          {/* Search + Category chips */}
          <div className="rounded-xl border border-[#E7E2D8] bg-white p-4 space-y-3">
            <div className="relative">
              <Search className="w-4 h-4 text-slate-400 absolute top-1/2 -translate-y-1/2 start-3.5" />
              <input
                type="search"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder={isAr ? "ابحث عن منتج داخل المتجر..." : "Search products in this store..."}
                className="w-full ps-10 pe-3 py-2.5 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm focus:outline-none focus:border-souqi-teal"
              />
            </div>

            {categories.length > 0 && (
              <div className="flex items-center gap-2 overflow-x-auto pb-1">
                <button
                  type="button"
                  onClick={() => setSelectedCat("ALL")}
                  className={`px-3.5 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap border transition-colors ${
                    selectedCat === "ALL"
                      ? "bg-souqi-teal text-white border-souqi-teal"
                      : "bg-[#FAF7F2] text-slate-700 border-[#DFD8CA] hover:bg-stone-200/60"
                  }`}
                >
                  {isAr ? "الكل" : "All"}
                </button>
                {categories.map((cat) => (
                  <button
                    key={cat.id}
                    type="button"
                    onClick={() => setSelectedCat(cat.id)}
                    className={`px-3.5 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap border transition-colors ${
                      selectedCat === cat.id
                        ? "bg-souqi-teal text-white border-souqi-teal"
                        : "bg-[#FAF7F2] text-slate-700 border-[#DFD8CA] hover:bg-stone-200/60"
                    }`}
                  >
                    {isAr ? cat.nameAr : cat.nameEn || cat.nameAr}
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Product Grid */}
          {loading ? (
            <div className="grid sm:grid-cols-2 gap-4">
              {[1, 2, 3, 4].map((n) => (
                <div key={n} className="h-64 rounded-xl border border-[#E7E2D8] bg-white p-4 animate-pulse space-y-3">
                  <div className="h-32 bg-stone-100 rounded-lg" />
                  <div className="h-4 bg-stone-200 rounded w-2/3" />
                  <div className="h-4 bg-stone-100 rounded w-1/3" />
                </div>
              ))}
            </div>
          ) : filteredProducts.length === 0 ? (
            <div className="rounded-xl border border-[#E7E2D8] bg-white p-10 text-center space-y-2">
              <p className="font-semibold text-slate-800">{isAr ? "لا توجد منتجات مطابقة" : "No matching products"}</p>
              <p className="text-sm text-slate-500">
                {isAr ? "اختر قسماً آخر أو امسح مربع البحث." : "Try selecting another category or clearing the search."}
              </p>
            </div>
          ) : (
            <div className="grid sm:grid-cols-2 gap-4">
              {filteredProducts.map((prod) => (
                <article
                  key={prod.id}
                  className="rounded-xl border border-[#E7E2D8] bg-white p-4 flex flex-col justify-between space-y-3"
                >
                  <div className="space-y-3">
                    <button
                      type="button"
                      onClick={() => setDetailProduct(prod)}
                      className="w-full text-start focus:outline-none"
                    >
                      <ProductPresetVisual preset={prod.imagePreset} className="h-32" />
                    </button>

                    <div className="space-y-1">
                      <div className="flex items-start justify-between gap-2">
                        <button
                          type="button"
                          onClick={() => setDetailProduct(prod)}
                          className="font-bold text-slate-900 hover:text-souqi-teal text-start leading-snug"
                        >
                          {isAr ? prod.nameAr : prod.nameEn || prod.nameAr}
                        </button>
                      </div>
                      <p className="text-xs text-slate-600 line-clamp-2">
                        {isAr ? prod.descriptionAr : prod.descriptionEn || prod.descriptionAr}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center justify-between pt-2 border-t border-stone-100">
                    <div>
                      <div className="font-bold text-souqi-teal tabular-nums">
                        {formatCurrency(prod.price, store.currency, lang)}
                      </div>
                      <div className="text-[11px] text-slate-500">
                        {prod.available
                          ? isAr
                            ? `متوفر (${prod.stockCount})`
                            : `In stock (${prod.stockCount})`
                          : isAr
                          ? "نفد حالياً"
                          : "Out of stock"}
                      </div>
                    </div>

                    <button
                      type="button"
                      disabled={!prod.available}
                      onClick={() => addToCart(prod)}
                      className="px-3.5 py-2 rounded-lg bg-souqi-teal hover:bg-souqi-tealLight disabled:bg-stone-200 disabled:text-stone-500 text-white text-xs font-semibold transition-colors"
                    >
                      {prod.available ? (isAr ? "أضف للسلة" : "Add") : isAr ? "غير متوفر" : "Unavailable"}
                    </button>
                  </div>
                </article>
              ))}
            </div>
          )}
        </div>

        {/* Desktop Sticky Cart */}
        <aside className="hidden lg:block lg:col-span-4 sticky top-20">{renderCartPanel()}</aside>
      </div>

      {/* Mobile Floating Cart Bar */}
      {totals.totalItems > 0 && (
        <div className="lg:hidden fixed bottom-16 inset-x-4 z-30">
          <button
            type="button"
            onClick={() => setMobileCartOpen(true)}
            className="w-full py-3.5 px-5 rounded-xl bg-souqi-teal text-white shadow-lg flex items-center justify-between font-bold text-sm"
          >
            <span className="inline-flex items-center gap-2">
              <ShoppingBag className="w-4 h-4" />
              {isAr ? `عرض السلة (${totals.totalItems})` : `View Cart (${totals.totalItems})`}
            </span>
            <span className="tabular-nums">{formatCurrency(totals.subtotal, store.currency, lang)}</span>
          </button>
        </div>
      )}

      {/* Mobile Cart Drawer */}
      {mobileCartOpen && (
        <div className="lg:hidden fixed inset-0 z-50 bg-black/40 flex items-end">
          <div className="w-full max-h-[88vh] overflow-y-auto bg-[#FAF7F2] rounded-t-2xl p-4 space-y-3">
            <div className="flex justify-end">
              <button
                type="button"
                onClick={() => setMobileCartOpen(false)}
                className="p-2 rounded-lg bg-white border border-[#E7E2D8] text-slate-700"
                aria-label={isAr ? "إغلاق السلة" : "Close cart"}
              >
                <X className="w-4 h-4" />
              </button>
            </div>
            {renderCartPanel()}
          </div>
        </div>
      )}

      {/* Product Detail Modal */}
      {detailProduct && (
        <div className="fixed inset-0 z-50 bg-black/45 flex items-center justify-center p-4">
          <div className="w-full max-w-md rounded-2xl bg-white border border-[#E7E2D8] p-5 space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="font-bold text-lg text-slate-900">
                {isAr ? detailProduct.nameAr : detailProduct.nameEn || detailProduct.nameAr}
              </h3>
              <button
                type="button"
                onClick={() => setDetailProduct(null)}
                className="p-1.5 rounded-lg hover:bg-stone-100 text-slate-600"
                aria-label={isAr ? "إغلاق" : "Close"}
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <ProductPresetVisual preset={detailProduct.imagePreset} className="h-44" />

            <p className="text-sm text-slate-600 leading-relaxed">
              {isAr
                ? detailProduct.descriptionAr || "لا يوجد وصف إضافي."
                : detailProduct.descriptionEn || detailProduct.descriptionAr || "No description."}
            </p>

            <div className="flex items-center justify-between pt-2 border-t border-stone-100">
              <span className="text-lg font-bold text-souqi-teal tabular-nums">
                {formatCurrency(detailProduct.price, store.currency, lang)}
              </span>
              <button
                type="button"
                disabled={!detailProduct.available}
                onClick={() => {
                  addToCart(detailProduct);
                  setDetailProduct(null);
                }}
                className="px-4 py-2.5 rounded-xl bg-souqi-teal hover:bg-souqi-tealLight disabled:bg-stone-200 text-white text-sm font-semibold"
              >
                {detailProduct.available ? (isAr ? "إضافة إلى السلة" : "Add to Cart") : isAr ? "غير متوفر" : "Unavailable"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
