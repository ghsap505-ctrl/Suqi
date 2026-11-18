import React, { useEffect, useState } from "react";
import { BarChart3, FolderKanban, Inbox, LogIn, PackagePlus, Settings, Store as StoreIcon, Trash2 } from "lucide-react";
import { AppLang, Category, CurrencyCode, InquiryStatus, OrderInquiry, Product, Store } from "../types";
import { formatCurrency, sanitizeSlug } from "../lib/whatsapp";
import { StatusBadge } from "./VisualHelpers";

interface MerchantDashboardViewProps {
  lang: AppLang;
  isDemoMode: boolean;
  isSignedIn: boolean;
  store: Store | null;
  categories: Category[];
  products: Product[];
  inquiries: OrderInquiry[];
  onSignIn: () => void;
  onSaveStore: (payload: {
    id?: string;
    name: string;
    slug: string;
    description: string;
    city: string;
    whatsappPhone: string;
    currency: CurrencyCode;
    accentKey: Store["accentKey"];
    coverPreset: Store["coverPreset"];
    active: boolean;
  }) => Promise<void>;
  onSaveCategory: (cat: { id?: string; nameAr: string; nameEn: string; sortOrder: number; active: boolean }) => Promise<void>;
  onDeleteCategory: (catId: string) => Promise<void>;
  onSaveProduct: (prod: {
    id?: string;
    categoryId: string;
    nameAr: string;
    nameEn: string;
    descriptionAr: string;
    descriptionEn: string;
    price: number;
    stockCount: number;
    available: boolean;
    imagePreset: string;
  }) => Promise<void>;
  onDeleteProduct: (prodId: string) => Promise<void>;
  onUpdateInquiryStatus: (inquiryId: string, status: InquiryStatus) => Promise<void>;
}

export const MerchantDashboardView: React.FC<MerchantDashboardViewProps> = ({
  lang,
  isDemoMode,
  isSignedIn,
  store,
  categories,
  products,
  inquiries,
  onSignIn,
  onSaveStore,
  onSaveCategory,
  onDeleteCategory,
  onSaveProduct,
  onDeleteProduct,
  onUpdateInquiryStatus,
}) => {
  const isAr = lang === "ar";
  const [tab, setTab] = useState<"overview" | "products" | "categories" | "inquiries" | "settings">("overview");
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Store Form State
  const [name, setName] = useState(store?.name || "");
  const [slug, setSlug] = useState(store?.slug || "");
  const [description, setDescription] = useState(store?.description || "");
  const [city, setCity] = useState(store?.city || "صنعاء");
  const [whatsappPhone, setWhatsappPhone] = useState(store?.whatsappPhone || "+967");
  const [currency, setCurrency] = useState<CurrencyCode>(store?.currency || "YER");
  const [accentKey, setAccentKey] = useState<Store["accentKey"]>(store?.accentKey || "teal");
  const [active, setActive] = useState<boolean>(store?.active ?? true);

  // Category Form State
  const [catNameAr, setCatNameAr] = useState("");
  const [catNameEn, setCatNameEn] = useState("");
  const [catSortOrder, setCatSortOrder] = useState("1");

  // Product Form State
  const [prodId, setProdId] = useState<string | undefined>(undefined);
  const [prodCatId, setProdCatId] = useState(categories[0]?.id || "");
  const [prodNameAr, setProdNameAr] = useState("");
  const [prodNameEn, setProdNameEn] = useState("");
  const [prodDescAr, setProdDescAr] = useState("");
  const [prodPrice, setProdPrice] = useState("");
  const [prodStock, setProdStock] = useState("10");
  const [prodPreset, setProdPreset] = useState("honey");
  const [prodAvailable, setProdAvailable] = useState(true);

  useEffect(() => {
    if (store) {
      setName(store.name);
      setSlug(store.slug);
      setDescription(store.description);
      setCity(store.city);
      setWhatsappPhone(store.whatsappPhone);
      setCurrency(store.currency);
      setAccentKey(store.accentKey);
      setActive(store.active);
    }
  }, [store]);

  useEffect(() => {
    if (!prodCatId && categories.length > 0) {
      setProdCatId(categories[0].id);
    }
  }, [categories, prodCatId]);

  if (!isDemoMode && !isSignedIn) {
    return (
      <div className="rounded-2xl border border-[#E7E2D8] bg-white p-8 text-center space-y-4 max-w-lg mx-auto">
        <StoreIcon className="w-10 h-10 text-souqi-teal mx-auto" />
        <h2 className="text-xl font-bold text-slate-900">
          {isAr ? "سجّل دخولك لإدارة متجرك" : "Sign in to manage your store"}
        </h2>
        <p className="text-sm text-slate-600">
          {isAr
            ? "تُحمى بيانات كل متجر وصلاحياته بحساب Google لضمان عزل المتاجر وأمان الطلبات."
            : "Each merchant store is tenant-isolated under your Google account."}
        </p>
        <button
          type="button"
          onClick={onSignIn}
          className="inline-flex items-center gap-2 px-5 py-3 rounded-xl bg-souqi-teal text-white font-semibold text-sm hover:bg-souqi-tealLight"
        >
          <LogIn className="w-4 h-4" />
          <span>{isAr ? "تسجيل الدخول عبر Google" : "Sign in with Google"}</span>
        </button>
      </div>
    );
  }

  const notify = (msg: string) => {
    setErrorMessage(null);
    setStatusMessage(msg);
    setTimeout(() => setStatusMessage(null), 3000);
  };

  const handleStoreSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    const cleanSlug = sanitizeSlug(slug || name);
    const cleanPhone = whatsappPhone.replace(/[^\d+]/g, "");
    if (name.trim().length < 2) {
      setErrorMessage(isAr ? "اسم المتجر قصير جداً" : "Store name too short");
      return;
    }
    if (cleanSlug.length < 2) {
      setErrorMessage(isAr ? "استخدم أحرفاً إنجليزية للرابط المختصر (slug)" : "Use English letters for slug");
      return;
    }
    if (!/^[+0-9]{7,20}$/.test(cleanPhone)) {
      setErrorMessage(isAr ? "رقم الواتساب غير صالح (مثال +967770123456)" : "Invalid WhatsApp phone");
      return;
    }
    try {
      await onSaveStore({
        id: store?.id,
        name: name.trim(),
        slug: cleanSlug,
        description: description.trim(),
        city: city.trim() || "صنعاء",
        whatsappPhone: cleanPhone,
        currency,
        accentKey,
        coverPreset: store?.coverPreset || "hero",
        active,
      });
      notify(isAr ? "تم حفظ إعدادات المتجر" : "Store settings saved");
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : "Error saving store");
    }
  };

  const handleAddCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!store) return;
    if (!catNameAr.trim()) return;
    try {
      await onSaveCategory({
        nameAr: catNameAr.trim(),
        nameEn: catNameEn.trim() || catNameAr.trim(),
        sortOrder: Number(catSortOrder) || 1,
        active: true,
      });
      setCatNameAr("");
      setCatNameEn("");
      notify(isAr ? "تمت إضافة القسم" : "Category added");
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : "Error adding category");
    }
  };

  const handleSaveProductSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!store) return;
    const price = Number(prodPrice);
    if (!prodNameAr.trim() || Number.isNaN(price) || price <= 0) {
      setErrorMessage(isAr ? "أدخل اسم المنتج وسعراً أكبر من 0" : "Enter valid name and price > 0");
      return;
    }
    const categoryId = prodCatId || categories[0]?.id || "general";
    try {
      await onSaveProduct({
        id: prodId,
        categoryId,
        nameAr: prodNameAr.trim(),
        nameEn: prodNameEn.trim() || prodNameAr.trim(),
        descriptionAr: prodDescAr.trim(),
        descriptionEn: prodDescAr.trim(),
        price,
        stockCount: Math.max(0, Math.trunc(Number(prodStock) || 0)),
        available: prodAvailable,
        imagePreset: prodPreset,
      });
      setProdId(undefined);
      setProdNameAr("");
      setProdNameEn("");
      setProdDescAr("");
      setProdPrice("");
      notify(isAr ? "تم حفظ المنتج" : "Product saved");
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : "Error saving product");
    }
  };

  const confirmedTotal = inquiries
    .filter((i) => i.status === "confirmed")
    .reduce((acc, i) => acc + i.totalAmount, 0);

  const tabs = [
    { id: "overview" as const, label: isAr ? "نظرة عامة" : "Overview", icon: BarChart3 },
    { id: "products" as const, label: isAr ? "المنتجات" : "Products", icon: PackagePlus },
    { id: "categories" as const, label: isAr ? "الأقسام" : "Categories", icon: FolderKanban },
    { id: "inquiries" as const, label: isAr ? "الطلبات" : "Inquiries", icon: Inbox },
    { id: "settings" as const, label: isAr ? "إعدادات المتجر" : "Settings", icon: Settings },
  ];

  return (
    <div className="space-y-6 pb-12">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">
            {isAr ? "لوحة التاجر" : "Merchant Dashboard"}
          </h1>
          <p className="text-sm text-slate-600">
            {store ? store.name : isAr ? "أنشئ متجرك الأول للبدء" : "Create your first store to get started"}
          </p>
        </div>
        {store && (
          <div className="flex items-center gap-2">
            <StatusBadge label={store.planId} tone="teal" />
            <StatusBadge
              label={store.subscriptionStatus}
              tone={store.subscriptionStatus === "active" ? "green" : store.subscriptionStatus === "suspended" ? "red" : "amber"}
            />
          </div>
        )}
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1">
        {tabs.map((t) => {
          const Icon = t.icon;
          const activeTab = tab === t.id;
          return (
            <button
              key={t.id}
              type="button"
              onClick={() => setTab(t.id)}
              className={`inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-semibold whitespace-nowrap border transition-colors ${
                activeTab
                  ? "bg-souqi-teal text-white border-souqi-teal"
                  : "bg-white text-slate-700 border-[#E7E2D8] hover:bg-stone-50"
              }`}
            >
              <Icon className="w-4 h-4" />
              <span>{t.label}</span>
            </button>
          );
        })}
      </div>

      {statusMessage && (
        <div className="rounded-xl bg-emerald-50 border border-emerald-200 px-4 py-3 text-sm text-emerald-800 font-medium">
          {statusMessage}
        </div>
      )}
      {errorMessage && (
        <div className="rounded-xl bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700 font-medium">
          {errorMessage}
        </div>
      )}

      {!store && tab !== "settings" ? (
        <div className="rounded-xl border border-[#E7E2D8] bg-white p-8 text-center space-y-3">
          <p className="font-bold text-slate-900">{isAr ? "لا يوجد متجر مرتبط بحسابك بعد" : "No store yet"}</p>
          <button
            type="button"
            onClick={() => setTab("settings")}
            className="px-4 py-2.5 rounded-xl bg-souqi-teal text-white text-sm font-semibold"
          >
            {isAr ? "إنشاء متجر الآن" : "Create Store"}
          </button>
        </div>
      ) : null}

      {store && tab === "overview" && (
        <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="rounded-xl border border-[#E7E2D8] bg-white p-5 space-y-1">
            <div className="text-xs text-slate-500">{isAr ? "المنتجات" : "Products"}</div>
            <div className="text-2xl font-bold text-slate-900 tabular-nums">{products.length}</div>
          </div>
          <div className="rounded-xl border border-[#E7E2D8] bg-white p-5 space-y-1">
            <div className="text-xs text-slate-500">{isAr ? "الأقسام" : "Categories"}</div>
            <div className="text-2xl font-bold text-slate-900 tabular-nums">{categories.length}</div>
          </div>
          <div className="rounded-xl border border-[#E7E2D8] bg-white p-5 space-y-1">
            <div className="text-xs text-slate-500">{isAr ? "طلبات واتساب" : "Order Inquiries"}</div>
            <div className="text-2xl font-bold text-slate-900 tabular-nums">{inquiries.length}</div>
          </div>
          <div className="rounded-xl border border-[#E7E2D8] bg-white p-5 space-y-1">
            <div className="text-xs text-slate-500">{isAr ? "مبيعات مؤكدة" : "Confirmed Sales"}</div>
            <div className="text-xl font-bold text-souqi-teal tabular-nums">
              {formatCurrency(confirmedTotal, store.currency, lang)}
            </div>
          </div>
        </div>
      )}

      {store && tab === "categories" && (
        <div className="grid lg:grid-cols-12 gap-6 items-start">
          <form onSubmit={handleAddCategory} className="lg:col-span-5 rounded-xl border border-[#E7E2D8] bg-white p-5 space-y-3">
            <h3 className="font-bold text-slate-900">{isAr ? "إضافة قسم جديد" : "Add Category"}</h3>
            <input
              type="text"
              value={catNameAr}
              onChange={(e) => setCatNameAr(e.target.value)}
              placeholder={isAr ? "اسم القسم بالعربية *" : "Arabic Name *"}
              className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm"
            />
            <input
              type="text"
              value={catNameEn}
              onChange={(e) => setCatNameEn(e.target.value)}
              placeholder={isAr ? "الاسم بالإنجليزية (اختياري)" : "English Name"}
              className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm"
            />
            <input
              type="number"
              min={0}
              max={500}
              value={catSortOrder}
              onChange={(e) => setCatSortOrder(e.target.value)}
              placeholder={isAr ? "الترتيب" : "Sort order"}
              className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm"
            />
            <button type="submit" className="w-full py-2.5 rounded-xl bg-souqi-teal text-white font-semibold text-sm">
              {isAr ? "حفظ القسم" : "Save Category"}
            </button>
          </form>

          <div className="lg:col-span-7 space-y-2.5">
            {categories.map((c) => (
              <div key={c.id} className="rounded-xl border border-[#E7E2D8] bg-white px-4 py-3 flex items-center justify-between">
                <div>
                  <div className="font-semibold text-slate-900">{isAr ? c.nameAr : c.nameEn}</div>
                  <div className="text-xs text-slate-500">#{c.sortOrder}</div>
                </div>
                <button
                  type="button"
                  onClick={() => onDeleteCategory(c.id)}
                  className="p-2 rounded-lg text-slate-400 hover:text-red-600 hover:bg-red-50"
                  aria-label={isAr ? "حذف القسم" : "Delete category"}
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {store && tab === "products" && (
        <div className="grid lg:grid-cols-12 gap-6 items-start">
          <form onSubmit={handleSaveProductSubmit} className="lg:col-span-5 rounded-xl border border-[#E7E2D8] bg-white p-5 space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="font-bold text-slate-900">
                {prodId ? (isAr ? "تعديل المنتج" : "Edit Product") : isAr ? "إضافة منتج جديد" : "Add Product"}
              </h3>
              {prodId && (
                <button
                  type="button"
                  onClick={() => {
                    setProdId(undefined);
                    setProdNameAr("");
                    setProdPrice("");
                  }}
                  className="text-xs text-souqi-teal underline"
                >
                  {isAr ? "جديد" : "New"}
                </button>
              )}
            </div>

            {categories.length > 0 && (
              <select
                value={prodCatId}
                onChange={(e) => setProdCatId(e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm"
              >
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {isAr ? c.nameAr : c.nameEn}
                  </option>
                ))}
              </select>
            )}

            <input
              type="text"
              value={prodNameAr}
              onChange={(e) => setProdNameAr(e.target.value)}
              placeholder={isAr ? "اسم المنتج بالعربية *" : "Product Name (AR) *"}
              className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm"
            />
            <input
              type="text"
              value={prodNameEn}
              onChange={(e) => setProdNameEn(e.target.value)}
              placeholder={isAr ? "الاسم بالإنجليزية (اختياري)" : "Product Name (EN)"}
              className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm"
            />
            <textarea
              rows={2}
              value={prodDescAr}
              onChange={(e) => setProdDescAr(e.target.value)}
              placeholder={isAr ? "وصف مختصر للمنتج" : "Short description"}
              className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm"
            />
            <div className="grid grid-cols-2 gap-2.5">
              <input
                type="number"
                step="any"
                min="0.1"
                value={prodPrice}
                onChange={(e) => setProdPrice(e.target.value)}
                placeholder={isAr ? `السعر (${store.currency})` : `Price (${store.currency})`}
                className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm"
              />
              <input
                type="number"
                min="0"
                value={prodStock}
                onChange={(e) => setProdStock(e.target.value)}
                placeholder={isAr ? "المخزون" : "Stock"}
                className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm"
              />
            </div>

            <select
              value={prodPreset}
              onChange={(e) => setProdPreset(e.target.value)}
              className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm"
            >
              <option value="honey">{isAr ? "أيقونة: عسل / غذائي" : "Preset: Honey / Food"}</option>
              <option value="coffee">{isAr ? "أيقونة: بن / مشروبات" : "Preset: Coffee"}</option>
              <option value="abaya">{isAr ? "أيقونة: ملابس / أزياء" : "Preset: Fashion"}</option>
              <option value="earbuds">{isAr ? "أيقونة: سماعات" : "Preset: Audio"}</option>
              <option value="watch">{isAr ? "أيقونة: ساعات / إلكترونيات" : "Preset: Watch"}</option>
              <option value="cosmetics">{isAr ? "أيقونة: تجميل" : "Preset: Cosmetics"}</option>
            </select>

            <label className="flex items-center gap-2 text-sm text-slate-700 cursor-pointer">
              <input
                type="checkbox"
                checked={prodAvailable}
                onChange={(e) => setProdAvailable(e.target.checked)}
                className="rounded border-stone-300 text-souqi-teal"
              />
              <span>{isAr ? "متوفر للطلب حالياً" : "Available for order"}</span>
            </label>

            <button type="submit" className="w-full py-2.5 rounded-xl bg-souqi-teal text-white font-semibold text-sm">
              {isAr ? "حفظ المنتج" : "Save Product"}
            </button>
          </form>

          <div className="lg:col-span-7 space-y-2.5">
            {products.map((p) => (
              <div key={p.id} className="rounded-xl border border-[#E7E2D8] bg-white p-4 flex items-center justify-between gap-3">
                <div className="min-w-0">
                  <div className="font-bold text-slate-900 truncate">{isAr ? p.nameAr : p.nameEn || p.nameAr}</div>
                  <div className="text-xs text-slate-500 flex items-center gap-2 mt-0.5 tabular-nums">
                    <span className="text-souqi-teal font-semibold">{formatCurrency(p.price, store.currency, lang)}</span>
                    <span>•</span>
                    <span>{isAr ? `الكمية: ${p.stockCount}` : `Stock: ${p.stockCount}`}</span>
                  </div>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <button
                    type="button"
                    onClick={() => {
                      setProdId(p.id);
                      setProdCatId(p.categoryId);
                      setProdNameAr(p.nameAr);
                      setProdNameEn(p.nameEn);
                      setProdDescAr(p.descriptionAr);
                      setProdPrice(String(p.price));
                      setProdStock(String(p.stockCount));
                      setProdPreset(p.imagePreset);
                      setProdAvailable(p.available);
                    }}
                    className="px-3 py-1.5 rounded-lg border border-[#E2DDD3] text-xs font-semibold hover:bg-stone-50"
                  >
                    {isAr ? "تعديل" : "Edit"}
                  </button>
                  <button
                    type="button"
                    onClick={() => onDeleteProduct(p.id)}
                    className="p-2 rounded-lg text-slate-400 hover:text-red-600 hover:bg-red-50"
                    aria-label={isAr ? "حذف المنتج" : "Delete product"}
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {store && tab === "inquiries" && (
        <div className="space-y-3">
          {inquiries.length === 0 ? (
            <div className="rounded-xl border border-[#E7E2D8] bg-white p-8 text-center text-sm text-slate-500">
              {isAr ? "لا توجد طلبات واردة بعد." : "No incoming order inquiries yet."}
            </div>
          ) : (
            inquiries.map((inq) => {
              const locked = inq.status === "confirmed" || inq.status === "cancelled";
              return (
                <div key={inq.id} className="rounded-xl border border-[#E7E2D8] bg-white p-5 space-y-3">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div>
                      <div className="font-bold text-slate-900">{inq.customerName}</div>
                      <div className="text-xs text-slate-500" dir="ltr">
                        {inq.customerPhone} • {inq.deliveryAddress}
                      </div>
                    </div>
                    <StatusBadge
                      label={inq.status}
                      tone={inq.status === "confirmed" ? "green" : inq.status === "cancelled" ? "red" : "amber"}
                    />
                  </div>
                  <p className="text-sm text-slate-700 bg-[#FAF7F2] rounded-lg p-3">{inq.itemsSummary}</p>
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <span className="font-bold text-souqi-teal tabular-nums">
                      {formatCurrency(inq.totalAmount, inq.currency, lang)}
                    </span>
                    {!locked && (
                      <div className="flex flex-wrap gap-2">
                        <button
                          type="button"
                          onClick={() => onUpdateInquiryStatus(inq.id, "contacted")}
                          className="px-3 py-1.5 rounded-lg border border-[#E2DDD3] text-xs font-semibold hover:bg-stone-50"
                        >
                          {isAr ? "تم التواصل" : "Contacted"}
                        </button>
                        <button
                          type="button"
                          onClick={() => onUpdateInquiryStatus(inq.id, "confirmed")}
                          className="px-3 py-1.5 rounded-lg bg-emerald-700 text-white text-xs font-semibold"
                        >
                          {isAr ? "تأكيد الطلب" : "Confirm"}
                        </button>
                        <button
                          type="button"
                          onClick={() => onUpdateInquiryStatus(inq.id, "cancelled")}
                          className="px-3 py-1.5 rounded-lg bg-red-50 text-red-700 border border-red-200 text-xs font-semibold"
                        >
                          {isAr ? "إلغاء" : "Cancel"}
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}

      {tab === "settings" && (
        <form onSubmit={handleStoreSubmit} className="rounded-xl border border-[#E7E2D8] bg-white p-6 space-y-4 max-w-2xl">
          <h3 className="font-bold text-lg text-slate-900">
            {store ? (isAr ? "تحديث بيانات المتجر" : "Update Store Profile") : isAr ? "إنشاء متجر جديد" : "Create New Store"}
          </h3>

          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">{isAr ? "اسم المتجر *" : "Store Name *"}</label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {isAr ? "الرابط المختصر (إنجليزي) *" : "Store Slug *"}
              </label>
              <input
                type="text"
                dir="ltr"
                value={slug}
                onChange={(e) => setSlug(e.target.value)}
                placeholder="sanaa-honey"
                className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm"
              />
            </div>
          </div>

          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">{isAr ? "المدينة *" : "City *"}</label>
              <input
                type="text"
                value={city}
                onChange={(e) => setCity(e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {isAr ? "رقم واتساب بالرمز الدولي *" : "WhatsApp Phone *"}
              </label>
              <input
                type="tel"
                dir="ltr"
                value={whatsappPhone}
                onChange={(e) => setWhatsappPhone(e.target.value)}
                placeholder="+967770123456"
                className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">{isAr ? "وصف المتجر" : "Description"}</label>
            <textarea
              rows={2}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm"
            />
          </div>

          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">{isAr ? "العملة" : "Currency"}</label>
              <select
                value={currency}
                onChange={(e) => setCurrency(e.target.value as CurrencyCode)}
                className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm"
              >
                <option value="YER">YER</option>
                <option value="SAR">SAR</option>
                <option value="USD">USD</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">{isAr ? "لون المتجر" : "Accent Color"}</label>
              <select
                value={accentKey}
                onChange={(e) => setAccentKey(e.target.value as Store["accentKey"])}
                className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm"
              >
                <option value="teal">{isAr ? "أزرق بحري" : "Teal"}</option>
                <option value="amber">{isAr ? "ذهبي عسلي" : "Amber"}</option>
                <option value="terracotta">{isAr ? "طيني يمني" : "Terracotta"}</option>
                <option value="emerald">{isAr ? "أخضر زمردي" : "Emerald"}</option>
              </select>
            </div>
          </div>

          <label className="flex items-center gap-2 text-sm text-slate-700 cursor-pointer">
            <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
            <span>{isAr ? "المتجر منشور للعملاء" : "Store visible in public directory"}</span>
          </label>

          <button type="submit" className="px-5 py-2.5 rounded-xl bg-souqi-teal text-white font-semibold text-sm">
            {isAr ? "حفظ الإعدادات" : "Save Settings"}
          </button>
        </form>
      )}
    </div>
  );
};
