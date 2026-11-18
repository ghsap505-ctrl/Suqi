import React, { useState } from "react";
import { ShieldAlert, ShieldCheck } from "lucide-react";
import { AppLang, Store, SubscriptionPlan, SubscriptionStatus } from "../types";
import { formatCurrency } from "../lib/whatsapp";
import { StatusBadge } from "./VisualHelpers";

interface AdminDashboardViewProps {
  lang: AppLang;
  isAdmin: boolean;
  stores: Store[];
  plans: SubscriptionPlan[];
  inquiriesCount: number;
  onUpdateStore: (
    storeId: string,
    payload: { verified: boolean; active: boolean; planId: string; subscriptionStatus: SubscriptionStatus }
  ) => Promise<void>;
  onSavePlan: (plan: SubscriptionPlan) => Promise<void>;
}

export const AdminDashboardView: React.FC<AdminDashboardViewProps> = ({
  lang,
  isAdmin,
  stores,
  plans,
  inquiriesCount,
  onUpdateStore,
  onSavePlan,
}) => {
  const isAr = lang === "ar";
  const [planNameAr, setPlanNameAr] = useState("");
  const [planNameEn, setPlanNameEn] = useState("");
  const [planUsd, setPlanUsd] = useState("15");
  const [planYer, setPlanYer] = useState("8000");
  const [planMax, setPlanMax] = useState("150");
  const [planDescAr, setPlanDescAr] = useState("");
  const [statusMsg, setStatusMsg] = useState<string | null>(null);

  if (!isAdmin) {
    return (
      <div className="space-y-6 pb-10">
        <div className="rounded-xl border border-amber-200 bg-amber-50 p-5 flex items-start gap-3">
          <ShieldAlert className="w-5 h-5 text-amber-800 shrink-0 mt-0.5" />
          <div className="space-y-1">
            <h2 className="font-bold text-amber-950">{isAr ? "صلاحيات الإدارة مطلوبة" : "Admin Privileges Required"}</h2>
            <p className="text-sm text-amber-900">
              {isAr
                ? "إدارة المتاجر وتفعيل الاشتراكات يدوياً متاحة فقط لحساب المشرف المعتمد في قواعد الأمان."
                : "Store verification, manual subscription activation, and plan editing are restricted to verified platform administrators."}
            </p>
          </div>
        </div>

        <div className="rounded-xl border border-[#E7E2D8] bg-white p-5 space-y-3">
          <h3 className="font-bold text-slate-900">{isAr ? "باقات المنصة الحالية" : "Current Platform Plans"}</h3>
          <div className="grid sm:grid-cols-3 gap-3">
            {plans.map((p) => (
              <div key={p.id} className="rounded-lg border border-[#E7E2D8] bg-[#FAF7F2] p-4 space-y-1">
                <div className="font-bold text-slate-900">{isAr ? p.nameAr : p.nameEn}</div>
                <div className="text-sm font-semibold text-souqi-teal tabular-nums">
                  {formatCurrency(p.priceMonthlyUsd, "USD", lang)} / {formatCurrency(p.priceMonthlyYer, "YER", lang)}
                </div>
                <div className="text-xs text-slate-600">
                  {isAr ? `الحد الأقصى: ${p.maxProducts} منتج` : `Limit: ${p.maxProducts} products`}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  const handleSaveNewPlan = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!planNameAr.trim()) return;
    await onSavePlan({
      id: "",
      nameAr: planNameAr.trim(),
      nameEn: planNameEn.trim() || planNameAr.trim(),
      priceMonthlyUsd: Number(planUsd) || 0,
      priceMonthlyYer: Number(planYer) || 0,
      maxProducts: Math.max(1, Math.trunc(Number(planMax) || 50)),
      descriptionAr: planDescAr.trim() || "باقة مخصصة للمتاجر النشطة.",
      descriptionEn: planDescAr.trim() || "Custom storefront plan.",
      active: true,
      sortOrder: plans.length + 1,
    });
    setPlanNameAr("");
    setPlanNameEn("");
    setPlanDescAr("");
    setStatusMsg(isAr ? "تم حفظ الباقة بنجاح" : "Plan saved");
    setTimeout(() => setStatusMsg(null), 2500);
  };

  return (
    <div className="space-y-6 pb-12">
      <div className="flex items-center gap-2">
        <ShieldCheck className="w-6 h-6 text-souqi-teal" />
        <h1 className="text-2xl font-bold text-slate-900">{isAr ? "لوحة إدارة المنصة" : "Platform Admin Console"}</h1>
      </div>

      {statusMsg && (
        <div className="rounded-xl bg-emerald-50 border border-emerald-200 px-4 py-3 text-sm text-emerald-800">
          {statusMsg}
        </div>
      )}

      {/* Summary Stats */}
      <div className="grid sm:grid-cols-3 gap-4">
        <div className="rounded-xl border border-[#E7E2D8] bg-white p-5">
          <div className="text-xs text-slate-500">{isAr ? "إجمالي المتاجر" : "Total Stores"}</div>
          <div className="text-2xl font-bold text-slate-900 tabular-nums">{stores.length}</div>
        </div>
        <div className="rounded-xl border border-[#E7E2D8] bg-white p-5">
          <div className="text-xs text-slate-500">{isAr ? "المتاجر الموثقة" : "Verified Stores"}</div>
          <div className="text-2xl font-bold text-souqi-teal tabular-nums">
            {stores.filter((s) => s.verified).length}
          </div>
        </div>
        <div className="rounded-xl border border-[#E7E2D8] bg-white p-5">
          <div className="text-xs text-slate-500">{isAr ? "إجمالي استفسارات الطلب" : "Order Inquiries"}</div>
          <div className="text-2xl font-bold text-slate-900 tabular-nums">{inquiriesCount}</div>
        </div>
      </div>

      {/* Stores & Manual Subscription Activation */}
      <section className="space-y-3">
        <h2 className="font-bold text-lg text-slate-900">
          {isAr ? "إدارة المتاجر والاشتراكات اليدوية" : "Stores & Manual Subscriptions"}
        </h2>
        <div className="space-y-3">
          {stores.map((s) => (
            <div key={s.id} className="rounded-xl border border-[#E7E2D8] bg-white p-5 space-y-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <div className="font-bold text-slate-900">{s.name}</div>
                  <div className="text-xs text-slate-500" dir="ltr">
                    /{s.slug} • {s.city} • {s.whatsappPhone}
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <StatusBadge label={s.verified ? (isAr ? "موثق" : "Verified") : isAr ? "غير موثق" : "Unverified"} tone={s.verified ? "teal" : "stone"} />
                  <StatusBadge
                    label={s.subscriptionStatus}
                    tone={s.subscriptionStatus === "active" ? "green" : s.subscriptionStatus === "suspended" ? "red" : "amber"}
                  />
                </div>
              </div>

              <div className="flex flex-wrap items-center gap-2 pt-1">
                <select
                  value={s.planId}
                  onChange={(e) =>
                    onUpdateStore(s.id, {
                      verified: s.verified,
                      active: s.active,
                      planId: e.target.value,
                      subscriptionStatus: s.subscriptionStatus,
                    })
                  }
                  className="px-3 py-1.5 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-xs font-semibold"
                >
                  {plans.map((p) => (
                    <option key={p.id} value={p.id}>
                      {isAr ? p.nameAr : p.nameEn}
                    </option>
                  ))}
                </select>

                <select
                  value={s.subscriptionStatus}
                  onChange={(e) =>
                    onUpdateStore(s.id, {
                      verified: s.verified,
                      active: s.active,
                      planId: s.planId,
                      subscriptionStatus: e.target.value as SubscriptionStatus,
                    })
                  }
                  className="px-3 py-1.5 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-xs font-semibold"
                >
                  <option value="trial">trial</option>
                  <option value="pending">pending</option>
                  <option value="active">active</option>
                  <option value="suspended">suspended</option>
                </select>

                <button
                  type="button"
                  onClick={() =>
                    onUpdateStore(s.id, {
                      verified: !s.verified,
                      active: s.active,
                      planId: s.planId,
                      subscriptionStatus: s.subscriptionStatus,
                    })
                  }
                  className="px-3 py-1.5 rounded-lg border border-[#E2DDD3] text-xs font-semibold hover:bg-stone-50"
                >
                  {s.verified ? (isAr ? "إلغاء التوثيق" : "Unverify") : isAr ? "توثيق المتجر" : "Verify Store"}
                </button>

                <button
                  type="button"
                  onClick={() =>
                    onUpdateStore(s.id, {
                      verified: s.verified,
                      active: !s.active,
                      planId: s.planId,
                      subscriptionStatus: s.subscriptionStatus,
                    })
                  }
                  className="px-3 py-1.5 rounded-lg border border-[#E2DDD3] text-xs font-semibold hover:bg-stone-50"
                >
                  {s.active ? (isAr ? "إيقاف النشر" : "Deactivate") : isAr ? "تفعيل النشر" : "Activate"}
                </button>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Plans Management */}
      <section className="grid lg:grid-cols-12 gap-6 items-start">
        <form onSubmit={handleSaveNewPlan} className="lg:col-span-5 rounded-xl border border-[#E7E2D8] bg-white p-5 space-y-3">
          <h3 className="font-bold text-slate-900">{isAr ? "إضافة باقة تسعير" : "Create Pricing Plan"}</h3>
          <input
            type="text"
            value={planNameAr}
            onChange={(e) => setPlanNameAr(e.target.value)}
            placeholder={isAr ? "اسم الباقة بالعربية *" : "Plan Name (AR) *"}
            className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm"
          />
          <input
            type="text"
            value={planNameEn}
            onChange={(e) => setPlanNameEn(e.target.value)}
            placeholder={isAr ? "الاسم بالإنجليزية" : "Plan Name (EN)"}
            className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm"
          />
          <div className="grid grid-cols-3 gap-2">
            <input
              type="number"
              min="0"
              value={planUsd}
              onChange={(e) => setPlanUsd(e.target.value)}
              placeholder="USD"
              className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm"
            />
            <input
              type="number"
              min="0"
              value={planYer}
              onChange={(e) => setPlanYer(e.target.value)}
              placeholder="YER"
              className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm"
            />
            <input
              type="number"
              min="1"
              value={planMax}
              onChange={(e) => setPlanMax(e.target.value)}
              placeholder={isAr ? "الحد" : "Max"}
              className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm"
            />
          </div>
          <textarea
            rows={2}
            value={planDescAr}
            onChange={(e) => setPlanDescAr(e.target.value)}
            placeholder={isAr ? "وصف الباقة" : "Plan description"}
            className="w-full px-3 py-2 rounded-lg border border-[#E2DDD3] bg-[#FAF7F2] text-sm"
          />
          <button type="submit" className="w-full py-2.5 rounded-xl bg-souqi-teal text-white font-semibold text-sm">
            {isAr ? "إضافة الباقة" : "Add Plan"}
          </button>
        </form>

        <div className="lg:col-span-7 space-y-2.5">
          {plans.map((p) => (
            <div key={p.id} className="rounded-xl border border-[#E7E2D8] bg-white p-4 flex items-center justify-between">
              <div>
                <div className="font-bold text-slate-900">{isAr ? p.nameAr : p.nameEn}</div>
                <div className="text-xs text-slate-600">{isAr ? p.descriptionAr : p.descriptionEn}</div>
              </div>
              <div className="text-end text-xs font-bold text-souqi-teal tabular-nums">
                <div>{formatCurrency(p.priceMonthlyUsd, "USD", lang)}</div>
                <div>{formatCurrency(p.priceMonthlyYer, "YER", lang)}</div>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
};
