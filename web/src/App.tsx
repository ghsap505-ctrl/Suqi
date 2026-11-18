import React, { useCallback, useEffect, useMemo, useState } from "react";
import { onAuthStateChanged, User } from "firebase/auth";
import { Globe, Home, LogIn, LogOut, Shield, ShoppingBag, Store as StoreIcon } from "lucide-react";
import {
  AppLang,
  Category,
  CurrencyCode,
  InquiryStatus,
  OrderInquiry,
  Product,
  Store,
  SubscriptionPlan,
  SubscriptionStatus,
} from "./types";
import {
  defaultPlans,
  demoCategoriesByStore,
  demoInquiries,
  demoProductsByStore,
  demoStores,
} from "./data/sampleCatalog";
import { getStrings } from "./i18n/strings";
import {
  adminSavePlanWeb,
  adminUpdateStoreWeb,
  auth,
  checkIsAdminWeb,
  createStoreWeb,
  deleteCategoryWeb,
  deleteProductWeb,
  ensureUserProfileWeb,
  fetchActiveStoresWeb,
  fetchAdminDashboardWeb,
  fetchMerchantInquiriesWeb,
  fetchMyStoresWeb,
  fetchPlansWeb,
  fetchStoreCatalogWeb,
  isFirebaseConfigured,
  signInWithGoogleWeb,
  signOutWeb,
  submitOrderInquiryWeb,
  updateInquiryStatusWeb,
  updateStoreWeb,
  upsertCategoryWeb,
  upsertProductWeb,
} from "./lib/firebase";
import { LandingView } from "./components/LandingView";
import { ExploreView } from "./components/ExploreView";
import { StorefrontView } from "./components/StorefrontView";
import { MerchantDashboardView } from "./components/MerchantDashboardView";
import { AdminDashboardView } from "./components/AdminDashboardView";

type RouteState =
  | { page: "home" }
  | { page: "explore" }
  | { page: "store"; slug: string }
  | { page: "merchant" }
  | { page: "admin" };

function parseHashRoute(hash: string): RouteState {
  const clean = hash.replace(/^#/, "").replace(/^\/+/, "");
  if (!clean || clean === "") return { page: "home" };
  if (clean === "explore") return { page: "explore" };
  if (clean === "merchant") return { page: "merchant" };
  if (clean === "admin") return { page: "admin" };
  if (clean.startsWith("s/")) {
    const slug = decodeURIComponent(clean.slice(2).split("?")[0] || "").trim();
    if (slug) return { page: "store", slug };
  }
  return { page: "home" };
}

export function App() {
  const [lang, setLang] = useState<AppLang>("ar");
  const [route, setRoute] = useState<RouteState>(() => parseHashRoute(window.location.hash));
  const [user, setUser] = useState<User | null>(null);
  const [isAdmin, setIsAdmin] = useState(false);
  const [loadingDirectory, setLoadingDirectory] = useState(false);
  const [loadingStore, setLoadingStore] = useState(false);

  // Catalog & Multi-tenant State (Demo Mode seeded + Cloud merged)
  const [stores, setStores] = useState<Store[]>(demoStores);
  const [plans, setPlans] = useState<SubscriptionPlan[]>(defaultPlans);
  const [categoriesMap, setCategoriesMap] = useState<Record<string, Category[]>>(demoCategoriesByStore);
  const [productsMap, setProductsMap] = useState<Record<string, Product[]>>(demoProductsByStore);
  const [inquiries, setInquiries] = useState<OrderInquiry[]>(demoInquiries);
  const [adminStores, setAdminStores] = useState<Store[]>(demoStores);
  const [adminInquiriesCount, setAdminInquiriesCount] = useState<number>(demoInquiries.length);
  const [authError, setAuthError] = useState<string | null>(null);

  const t = useMemo(() => getStrings(lang), [lang]);
  const isDemoMode = !isFirebaseConfigured || !user;

  useEffect(() => {
    document.documentElement.dir = lang === "ar" ? "rtl" : "ltr";
    document.documentElement.lang = lang;
  }, [lang]);

  useEffect(() => {
    const onHash = () => setRoute(parseHashRoute(window.location.hash));
    window.addEventListener("hashchange", onHash);
    return () => window.removeEventListener("hashchange", onHash);
  }, []);

  const navigate = useCallback((nextHash: string) => {
    window.location.hash = nextHash;
    window.scrollTo({ top: 0, behavior: "auto" });
  }, []);

  // Auth listener when Firebase Web keys are configured
  useEffect(() => {
    if (!auth) return;
    const unsub = onAuthStateChanged(auth, async (nextUser) => {
      setUser(nextUser);
      if (nextUser) {
        try {
          await ensureUserProfileWeb(
            nextUser.uid,
            nextUser.displayName || "تاجر سوقي",
            nextUser.email || "merchant@souqi.app"
          );
          const adminFlag = await checkIsAdminWeb(nextUser.uid, nextUser.email, nextUser.emailVerified);
          setIsAdmin(adminFlag);
        } catch {
          setIsAdmin(false);
        }
      } else {
        setIsAdmin(false);
      }
    });
    return () => unsub();
  }, []);

  // Sync live Firestore directory & merchant data when signed in
  const refreshCloudData = useCallback(async () => {
    if (!isFirebaseConfigured || !user) return;
    setLoadingDirectory(true);
    try {
      const [cloudStores, myStores, cloudPlans, cloudInquiries] = await Promise.all([
        fetchActiveStoresWeb(),
        fetchMyStoresWeb(user.uid),
        fetchPlansWeb(),
        fetchMerchantInquiriesWeb(user.uid),
      ]);

      const mergedById = new Map<string, Store>();
      demoStores.forEach((s) => mergedById.set(s.id, s));
      cloudStores.forEach((s) => mergedById.set(s.id, { ...s, demoMode: false }));
      myStores.forEach((s) => mergedById.set(s.id, { ...s, demoMode: false }));
      setStores(Array.from(mergedById.values()));

      if (cloudPlans.length > 0) setPlans(cloudPlans);
      if (cloudInquiries.length > 0) setInquiries(cloudInquiries);

      if (isAdmin) {
        const adminData = await fetchAdminDashboardWeb();
        setAdminStores(adminData.stores.length > 0 ? adminData.stores : demoStores);
        if (adminData.plans.length > 0) setPlans(adminData.plans);
        setAdminInquiriesCount(adminData.inquiriesCount);
      }
    } catch {
      // Keep demo data intact if network or rules fail
    } finally {
      setLoadingDirectory(false);
    }
  }, [user, isAdmin]);

  useEffect(() => {
    if (user && isFirebaseConfigured) {
      refreshCloudData();
    }
  }, [user, isAdmin, refreshCloudData]);

  // Active storefront resolution
  const activeStore = useMemo(() => {
    if (route.page === "store") {
      return stores.find((s) => s.slug === route.slug) ?? stores[0] ?? null;
    }
    return stores[0] ?? null;
  }, [route, stores]);

  // Load storefront catalog on demand
  useEffect(() => {
    if (!activeStore || activeStore.demoMode || !isFirebaseConfigured || !user) return;
    let cancelled = false;
    setLoadingStore(true);
    fetchStoreCatalogWeb(activeStore, user.uid, isAdmin)
      .then(({ categories, products }) => {
        if (cancelled) return;
        setCategoriesMap((prev) => ({ ...prev, [activeStore.id]: categories }));
        setProductsMap((prev) => ({ ...prev, [activeStore.id]: products }));
      })
      .catch(() => {
        // Ignore gracefully
      })
      .finally(() => {
        if (!cancelled) setLoadingStore(false);
      });
    return () => {
      cancelled = true;
    };
  }, [activeStore, user, isAdmin]);

  // Merchant's own store (in cloud mode: owned by user.uid; in demo mode: first demo store)
  const merchantStore = useMemo(() => {
    if (user && isFirebaseConfigured) {
      return stores.find((s) => s.ownerUid === user.uid && !s.demoMode) ?? null;
    }
    return stores[0] ?? null;
  }, [stores, user]);

  // Preload merchant store catalog
  useEffect(() => {
    if (!merchantStore || merchantStore.demoMode || !isFirebaseConfigured || !user) return;
    fetchStoreCatalogWeb(merchantStore, user.uid, isAdmin)
      .then(({ categories, products }) => {
        setCategoriesMap((prev) => ({ ...prev, [merchantStore.id]: categories }));
        setProductsMap((prev) => ({ ...prev, [merchantStore.id]: products }));
      })
      .catch(() => {});
  }, [merchantStore, user, isAdmin]);

  const handleSignIn = async () => {
    setAuthError(null);
    if (!isFirebaseConfigured) {
      setAuthError(
        lang === "ar"
          ? "مفاتيح Firebase Web غير مضافة في ملف .env.local؛ التطبيق يعمل حالياً في الوضع التجريبي."
          : "Firebase Web keys are not set in .env.local; running in Demo Mode."
      );
      return;
    }
    try {
      await signInWithGoogleWeb();
    } catch (err) {
      setAuthError(err instanceof Error ? err.message : "Sign-in failed");
    }
  };

  const handleRecordInquiry = async (payload: Omit<OrderInquiry, "id" | "customerUid" | "status">) => {
    const newInquiry: OrderInquiry = {
      ...payload,
      id: `inq_${Date.now()}`,
      customerUid: user?.uid || "demo_customer",
      status: "pending_whatsapp",
    };

    if (user && isFirebaseConfigured && !activeStore?.demoMode) {
      const id = await submitOrderInquiryWeb(user.uid, payload);
      newInquiry.id = id;
    }
    setInquiries((prev) => [newInquiry, ...prev]);
  };

  const handleSaveStore = async (payload: {
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
  }) => {
    if (user && isFirebaseConfigured) {
      if (payload.id && !payload.id.startsWith("demo_")) {
        await updateStoreWeb(payload.id, {
          name: payload.name,
          slug: payload.slug,
          description: payload.description,
          city: payload.city,
          whatsappPhone: payload.whatsappPhone,
          currency: payload.currency,
          accentKey: payload.accentKey,
          coverPreset: payload.coverPreset,
          active: payload.active,
        });
      } else {
        await createStoreWeb(user.uid, {
          name: payload.name,
          slug: payload.slug,
          description: payload.description,
          city: payload.city,
          whatsappPhone: payload.whatsappPhone,
          currency: payload.currency,
          accentKey: payload.accentKey,
          coverPreset: payload.coverPreset,
          active: payload.active,
        });
      }
      await refreshCloudData();
      return;
    }

    // Demo state update
    setStores((prev) =>
      prev.map((s) =>
        s.id === (payload.id || prev[0]?.id)
          ? {
              ...s,
              name: payload.name,
              slug: payload.slug,
              description: payload.description,
              city: payload.city,
              whatsappPhone: payload.whatsappPhone,
              currency: payload.currency,
              accentKey: payload.accentKey,
              active: payload.active,
            }
          : s
      )
    );
  };

  const handleSaveCategory = async (cat: {
    id?: string;
    nameAr: string;
    nameEn: string;
    sortOrder: number;
    active: boolean;
  }) => {
    if (!merchantStore) return;
    if (user && isFirebaseConfigured && !merchantStore.demoMode) {
      const id = await upsertCategoryWeb(merchantStore.id, user.uid, cat);
      const saved: Category = {
        id,
        storeId: merchantStore.id,
        ownerUid: user.uid,
        ...cat,
      };
      setCategoriesMap((prev) => ({
        ...prev,
        [merchantStore.id]: [...(prev[merchantStore.id] || []).filter((c) => c.id !== id), saved],
      }));
      return;
    }
    const id = cat.id || `cat_${Date.now()}`;
    const saved: Category = {
      id,
      storeId: merchantStore.id,
      ownerUid: merchantStore.ownerUid,
      ...cat,
    };
    setCategoriesMap((prev) => ({
      ...prev,
      [merchantStore.id]: [...(prev[merchantStore.id] || []).filter((c) => c.id !== id), saved],
    }));
  };

  const handleDeleteCategory = async (catId: string) => {
    if (!merchantStore) return;
    if (user && isFirebaseConfigured && !merchantStore.demoMode) {
      await deleteCategoryWeb(merchantStore.id, catId);
    }
    setCategoriesMap((prev) => ({
      ...prev,
      [merchantStore.id]: (prev[merchantStore.id] || []).filter((c) => c.id !== catId),
    }));
  };

  const handleSaveProduct = async (prod: {
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
  }) => {
    if (!merchantStore) return;
    if (user && isFirebaseConfigured && !merchantStore.demoMode) {
      const id = await upsertProductWeb(merchantStore.id, user.uid, prod);
      const saved: Product = { id, storeId: merchantStore.id, ownerUid: user.uid, ...prod };
      setProductsMap((prev) => ({
        ...prev,
        [merchantStore.id]: [...(prev[merchantStore.id] || []).filter((p) => p.id !== id), saved],
      }));
      return;
    }
    const id = prod.id || `prod_${Date.now()}`;
    const saved: Product = { id, storeId: merchantStore.id, ownerUid: merchantStore.ownerUid, ...prod };
    setProductsMap((prev) => ({
      ...prev,
      [merchantStore.id]: [...(prev[merchantStore.id] || []).filter((p) => p.id !== id), saved],
    }));
  };

  const handleDeleteProduct = async (prodId: string) => {
    if (!merchantStore) return;
    if (user && isFirebaseConfigured && !merchantStore.demoMode) {
      await deleteProductWeb(merchantStore.id, prodId);
    }
    setProductsMap((prev) => ({
      ...prev,
      [merchantStore.id]: (prev[merchantStore.id] || []).filter((p) => p.id !== prodId),
    }));
  };

  const handleUpdateInquiryStatus = async (inquiryId: string, status: InquiryStatus) => {
    if (user && isFirebaseConfigured && !inquiryId.startsWith("demo_") && !inquiryId.startsWith("inq_")) {
      await updateInquiryStatusWeb(inquiryId, status);
    }
    setInquiries((prev) => prev.map((i) => (i.id === inquiryId ? { ...i, status } : i)));
  };

  const handleAdminUpdateStore = async (
    storeId: string,
    payload: { verified: boolean; active: boolean; planId: string; subscriptionStatus: SubscriptionStatus }
  ) => {
    if (user && isFirebaseConfigured && isAdmin) {
      await adminUpdateStoreWeb(storeId, payload);
      await refreshCloudData();
      return;
    }
    setAdminStores((prev) => prev.map((s) => (s.id === storeId ? { ...s, ...payload } : s)));
  };

  const handleAdminSavePlan = async (plan: SubscriptionPlan) => {
    if (user && isFirebaseConfigured && isAdmin) {
      await adminSavePlanWeb(plan);
      await refreshCloudData();
      return;
    }
    const id = plan.id || `plan_${Date.now()}`;
    setPlans((prev) => [...prev, { ...plan, id }]);
  };

  const navItems = [
    { id: "home" as const, label: t.navHome, hash: "#/", icon: Home },
    { id: "explore" as const, label: t.navExplore, hash: "#/explore", icon: ShoppingBag },
    { id: "store" as const, label: t.navStore, hash: `#/s/${activeStore?.slug || "sanaa-honey"}`, icon: StoreIcon },
    { id: "merchant" as const, label: t.navMerchant, hash: "#/merchant", icon: StoreIcon },
    { id: "admin" as const, label: t.navAdmin, hash: "#/admin", icon: Shield },
  ];

  return (
    <div className="min-h-screen bg-[#FAF7F2] text-slate-900 flex flex-col">
      {/* Top Header */}
      <header className="sticky top-0 z-40 bg-white/95 backdrop-blur border-b border-[#E7E2D8]">
        <div className="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between gap-3">
          <button
            type="button"
            onClick={() => navigate("#/")}
            className="flex items-center gap-2.5 text-start shrink-0"
          >
            <div className="w-9 h-9 rounded-xl bg-souqi-teal text-white font-display font-bold flex items-center justify-center text-lg">
              س
            </div>
            <div>
              <div className="font-display font-bold text-lg leading-none text-slate-900">{t.brand}</div>
              <div className="text-[11px] text-slate-500 hidden sm:block">{t.tagline}</div>
            </div>
          </button>

          {/* Desktop Navigation */}
          <nav className="hidden md:flex items-center gap-1">
            {navItems.map((item) => {
              const isCurrent = route.page === item.id;
              return (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => navigate(item.hash)}
                  className={`px-3.5 py-2 rounded-lg text-sm font-semibold transition-colors ${
                    isCurrent ? "bg-teal-50 text-souqi-teal" : "text-slate-700 hover:bg-stone-100"
                  }`}
                >
                  {item.label}
                </button>
              );
            })}
          </nav>

          {/* Actions */}
          <div className="flex items-center gap-2 shrink-0">
            <button
              type="button"
              onClick={() => setLang((prev) => (prev === "ar" ? "en" : "ar"))}
              className="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-xs font-semibold text-slate-800 hover:bg-stone-200/60"
            >
              <Globe className="w-3.5 h-3.5" />
              <span>{t.langToggle}</span>
            </button>

            {user ? (
              <button
                type="button"
                onClick={() => signOutWeb()}
                className="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg border border-[#E2DDD3] text-xs font-semibold text-slate-700 hover:bg-stone-100"
              >
                <LogOut className="w-3.5 h-3.5" />
                <span className="hidden sm:inline">{t.signOut}</span>
              </button>
            ) : (
              <button
                type="button"
                onClick={handleSignIn}
                className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-lg bg-souqi-teal text-white text-xs font-semibold hover:bg-souqi-tealLight"
              >
                <LogIn className="w-3.5 h-3.5" />
                <span>{t.signInGoogle}</span>
              </button>
            )}
          </div>
        </div>
      </header>

      {/* Demo / Config Notice */}
      {(!isFirebaseConfigured || authError) && (
        <div className="bg-amber-50 border-b border-amber-200 text-amber-900 text-xs py-2 px-4 text-center">
          {authError || t.demoNotice}
        </div>
      )}

      {/* Main Content */}
      <main className="flex-1 max-w-6xl w-full mx-auto px-4 pt-6 pb-20 md:pb-8">
        {route.page === "home" && (
          <LandingView
            lang={lang}
            plans={plans}
            onBrowseDemo={() => navigate("#/s/sanaa-honey")}
            onOpenExplore={() => navigate("#/explore")}
            onOpenMerchant={() => navigate("#/merchant")}
          />
        )}

        {route.page === "explore" && (
          <ExploreView
            lang={lang}
            stores={stores}
            loading={loadingDirectory}
            onSelectStore={(store) => navigate(`#/s/${store.slug}`)}
          />
        )}

        {route.page === "store" && (
          <StorefrontView
            lang={lang}
            store={activeStore}
            categories={activeStore ? categoriesMap[activeStore.id] || [] : []}
            products={activeStore ? productsMap[activeStore.id] || [] : []}
            loading={loadingStore}
            onRecordInquiry={handleRecordInquiry}
            onBackToExplore={() => navigate("#/explore")}
          />
        )}

        {route.page === "merchant" && (
          <MerchantDashboardView
            lang={lang}
            isDemoMode={!isFirebaseConfigured}
            isSignedIn={Boolean(user)}
            store={merchantStore}
            categories={merchantStore ? categoriesMap[merchantStore.id] || [] : []}
            products={merchantStore ? productsMap[merchantStore.id] || [] : []}
            inquiries={inquiries.filter((i) => !merchantStore || i.storeId === merchantStore.id)}
            onSignIn={handleSignIn}
            onSaveStore={handleSaveStore}
            onSaveCategory={handleSaveCategory}
            onDeleteCategory={handleDeleteCategory}
            onSaveProduct={handleSaveProduct}
            onDeleteProduct={handleDeleteProduct}
            onUpdateInquiryStatus={handleUpdateInquiryStatus}
          />
        )}

        {route.page === "admin" && (
          <AdminDashboardView
            lang={lang}
            isAdmin={isAdmin || !isFirebaseConfigured}
            stores={isFirebaseConfigured ? adminStores : stores}
            plans={plans}
            inquiriesCount={isFirebaseConfigured ? adminInquiriesCount : inquiries.length}
            onUpdateStore={handleAdminUpdateStore}
            onSavePlan={handleAdminSavePlan}
          />
        )}
      </main>

      {/* Desktop Footer */}
      <footer className="hidden md:block border-t border-[#E7E2D8] bg-white py-5 text-xs text-slate-500">
        <div className="max-w-6xl mx-auto px-4 flex items-center justify-between">
          <span>
            {lang === "ar"
              ? "© سوقي — منصة المتاجر العربية عبر واتساب"
              : "© Souqi — Arabic WhatsApp Storefronts"}
          </span>
          <span>{lang === "ar" ? "يعمل على الويب والحاسوب والهاتف" : "Web • Desktop • Mobile"}</span>
        </div>
      </footer>

      {/* Mobile Bottom Navigation */}
      <nav className="md:hidden fixed bottom-0 inset-x-0 z-40 bg-white border-t border-[#E7E2D8] grid grid-cols-5 h-14">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isCurrent = route.page === item.id;
          return (
            <button
              key={item.id}
              type="button"
              onClick={() => navigate(item.hash)}
              className={`flex flex-col items-center justify-center gap-0.5 text-[11px] font-semibold ${
                isCurrent ? "text-souqi-teal" : "text-slate-500"
              }`}
            >
              <Icon className="w-4 h-4" />
              <span className="truncate max-w-full px-1">{item.label}</span>
            </button>
          );
        })}
      </nav>
    </div>
  );
}

export default App;
