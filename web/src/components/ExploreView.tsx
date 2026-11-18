import React, { useMemo, useState } from "react";
import { BadgeCheck, MapPin, Search, Store as StoreIcon } from "lucide-react";
import { AppLang, Store } from "../types";
import { getAccentClasses } from "./VisualHelpers";

interface ExploreViewProps {
  lang: AppLang;
  stores: Store[];
  loading: boolean;
  onSelectStore: (store: Store) => void;
}

export const ExploreView: React.FC<ExploreViewProps> = ({ lang, stores, loading, onSelectStore }) => {
  const isAr = lang === "ar";
  const [queryText, setQueryText] = useState("");
  const [cityFilter, setCityFilter] = useState("ALL");

  const cities = useMemo(() => {
    const set = new Set<string>();
    stores.forEach((s) => {
      if (s.city.trim()) set.add(s.city.trim());
    });
    return Array.from(set);
  }, [stores]);

  const filtered = useMemo(() => {
    const q = queryText.trim().toLowerCase();
    return stores.filter((s) => {
      if (!s.active) return false;
      if (cityFilter !== "ALL" && s.city !== cityFilter) return false;
      if (!q) return true;
      return (
        s.name.toLowerCase().includes(q) ||
        s.city.toLowerCase().includes(q) ||
        s.description.toLowerCase().includes(q) ||
        s.slug.toLowerCase().includes(q)
      );
    });
  }, [stores, queryText, cityFilter]);

  return (
    <div className="space-y-6 pb-10">
      <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">{isAr ? "دليل متاجر سوقي" : "Souqi Store Directory"}</h1>
          <p className="text-sm text-slate-600">
            {isAr
              ? "تصفح المتاجر النشطة، اختر منتجاتك، وأرسل طلبك مباشرة إلى واتساب التاجر."
              : "Browse active storefronts, build your cart, and send inquiries straight to the merchant's WhatsApp."}
          </p>
        </div>
      </div>

      {/* Search & City Filter */}
      <div className="rounded-xl border border-[#E7E2D8] bg-white p-4 flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-slate-400 absolute top-1/2 -translate-y-1/2 start-3.5" />
          <input
            type="search"
            value={queryText}
            onChange={(e) => setQueryText(e.target.value)}
            placeholder={isAr ? "ابحث باسم المتجر أو المدينة..." : "Search by store name or city..."}
            className="w-full ps-10 pe-3 py-2.5 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm focus:outline-none focus:border-souqi-teal"
          />
        </div>

        <select
          value={cityFilter}
          onChange={(e) => setCityFilter(e.target.value)}
          aria-label={isAr ? "تصفية حسب المدينة" : "Filter by city"}
          className="px-3.5 py-2.5 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm text-slate-800 focus:outline-none focus:border-souqi-teal"
        >
          <option value="ALL">{isAr ? "جميع المدن" : "All Cities"}</option>
          {cities.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>
      </div>

      {loading ? (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-48 rounded-xl border border-[#E7E2D8] bg-white p-5 animate-pulse space-y-3">
              <div className="h-5 bg-stone-200 rounded w-1/2" />
              <div className="h-4 bg-stone-100 rounded w-full" />
              <div className="h-4 bg-stone-100 rounded w-3/4" />
            </div>
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <div className="rounded-xl border border-[#E7E2D8] bg-white p-10 text-center space-y-2">
          <StoreIcon className="w-8 h-8 text-slate-400 mx-auto" />
          <p className="font-semibold text-slate-800">{isAr ? "لا توجد متاجر مطابقة لبحثك" : "No matching stores found"}</p>
          <p className="text-sm text-slate-500">
            {isAr ? "جرّب تغيير كلمة البحث أو اختيار جميع المدن." : "Try another search query or reset the city filter."}
          </p>
        </div>
      ) : (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map((store) => {
            const accent = getAccentClasses(store.accentKey);
            return (
              <article
                key={store.id}
                className="rounded-xl border border-[#E7E2D8] bg-white overflow-hidden flex flex-col justify-between hover:border-souqi-teal/50 transition-colors"
              >
                <div>
                  <div className={`h-2.5 w-full ${accent.bg}`} />
                  <div className="p-5 space-y-3">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <h2 className="font-bold text-lg text-slate-900 flex items-center gap-1.5">
                          <span>{store.name}</span>
                          {store.verified && <BadgeCheck className="w-4 h-4 text-souqi-teal shrink-0" />}
                        </h2>
                        <div className="flex items-center gap-2 text-xs text-slate-500 mt-1">
                          <span className="inline-flex items-center gap-1">
                            <MapPin className="w-3.5 h-3.5" />
                            {store.city}
                          </span>
                          <span>•</span>
                          <span dir="ltr">/{store.slug}</span>
                        </div>
                      </div>
                      {store.demoMode && (
                        <span className="px-2 py-0.5 rounded-md bg-stone-100 text-stone-600 text-[11px] font-semibold">
                          {isAr ? "تجريبي" : "Demo"}
                        </span>
                      )}
                    </div>

                    <p className="text-sm text-slate-600 line-clamp-2 leading-relaxed">{store.description}</p>
                  </div>
                </div>

                <div className="px-5 pb-5 pt-2 flex items-center justify-between gap-3">
                  <span className="text-xs font-semibold text-slate-500">{store.currency}</span>
                  <button
                    type="button"
                    onClick={() => onSelectStore(store)}
                    className="px-4 py-2 rounded-lg bg-souqi-teal hover:bg-souqi-tealLight text-white text-sm font-semibold transition-colors"
                  >
                    {isAr ? "دخول المتجر" : "Visit Store"}
                  </button>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
};
