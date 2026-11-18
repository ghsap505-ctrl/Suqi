import React, { useState } from "react";
import {
  ArrowLeft,
  ArrowRight,
  BadgeCheck,
  Check,
  ChevronDown,
  MessageCircle,
  Share2,
  ShieldCheck,
  Store as StoreIcon,
  Zap,
} from "lucide-react";
import { AppLang, SubscriptionPlan } from "../types";
import { faqs } from "../data/sampleCatalog";
import { formatCurrency } from "../lib/whatsapp";

interface LandingViewProps {
  lang: AppLang;
  plans: SubscriptionPlan[];
  onBrowseDemo: () => void;
  onOpenExplore: () => void;
  onOpenMerchant: () => void;
}

export const LandingView: React.FC<LandingViewProps> = ({
  lang,
  plans,
  onBrowseDemo,
  onOpenExplore,
  onOpenMerchant,
}) => {
  const isAr = lang === "ar";
  const [openFaq, setOpenFaq] = useState<number | null>(0);
  const ArrowIcon = isAr ? ArrowLeft : ArrowRight;

  const benefits = isAr
    ? [
        {
          icon: MessageCircle,
          title: "طلبات مرتبة عبر واتساب",
          body: "بدل رسائل متفرقة وصور غير واضحة، يصلك الطلب منسقاً بالأسماء والكميات والعنوان والإجمالي.",
        },
        {
          icon: Zap,
          title: "خفيف وسريع على الإنترنت الضعيف",
          body: "واجهة بسيطة وسريعة التحميل تناسب عملاء الهاتف في جميع المحافظات دون استهلاك بيانات.",
        },
        {
          icon: Share2,
          title: "رابط متجر مستقل لهويتك",
          body: "شارك رابط متجرك في بايو إنستغرام وفيسبوك وحالات واتساب مع شعارك وألوان محلك.",
        },
        {
          icon: ShieldCheck,
          title: "0% عمولة على المبيعات",
          body: "لا وسطاء ولا خصم من مبيعاتك؛ الاتفاق والدفع والتوصيل يتم مباشرة بينك وبين عميلك.",
        },
      ]
    : [
        {
          icon: MessageCircle,
          title: "Structured WhatsApp Orders",
          body: "Receive clean, formatted order messages with item quantities, subtotal, phone, and address.",
        },
        {
          icon: Zap,
          title: "Built for Slow Networks",
          body: "Lightweight pages that load rapidly on mobile connections without heavy scripts.",
        },
        {
          icon: Share2,
          title: "Shareable Store Link",
          body: "Post your storefront link on WhatsApp Status, Instagram, and Facebook with custom store branding.",
        },
        {
          icon: ShieldCheck,
          title: "0% Commission on Sales",
          body: "Zero transaction cuts. Payment and delivery stay directly between merchant and customer.",
        },
      ];

  const steps = isAr
    ? [
        { num: "01", title: "سجّل بيانات محلك", body: "اختر اسم المتجر والرابط المختصر ورقم الواتساب والعملة المناسبة خلال دقيقة." },
        { num: "02", title: "أضف أقسامك ومنتجاتك", body: "رتّب المنتجات مع الأسعار والمخزون وحالة التوفر بسهولة من الهاتف أو الحاسوب." },
        { num: "03", title: "شارك الرابط واستقبل الطلبات", body: "يتصفح الزبائن متجرك ويضيفون للسلة ثم يرسلون الطلب مباشرة إلى واتسابك." },
      ]
    : [
        { num: "01", title: "Set up your shop", body: "Pick your store name, unique slug, WhatsApp number, and currency in one minute." },
        { num: "02", title: "Publish categories & items", body: "Manage product prices, stock counts, and availability from mobile or desktop." },
        { num: "03", title: "Share link & take orders", body: "Shoppers build a cart and send a formatted order inquiry directly to your WhatsApp." },
      ];

  return (
    <div className="space-y-14 pb-12">
      {/* Hero Section */}
      <section className="rounded-2xl border border-[#E7E2D8] bg-white overflow-hidden">
        <div className="grid lg:grid-cols-12 gap-8 p-6 sm:p-10 items-center">
          <div className="lg:col-span-7 space-y-5">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-teal-50 border border-teal-200 text-souqi-teal text-xs font-semibold">
              <BadgeCheck className="w-4 h-4" />
              <span>{isAr ? "منصة متاجر عربية • 0% عمولة على الطلبات" : "Arabic Storefronts • 0% Order Commission"}</span>
            </div>

            <h1 className="text-2xl sm:text-4xl font-bold text-slate-900 leading-snug">
              {isAr
                ? "أنشئ متجرك الإلكتروني خلال دقائق واستقبل طلباتك عبر واتساب"
                : "Launch your online shop in minutes and receive orders on WhatsApp"}
            </h1>

            <p className="text-slate-600 text-base sm:text-lg leading-relaxed max-w-2xl">
              {isAr
                ? "صُمم سوقي لأصحاب المحلات والمتاجر في اليمن والعالم العربي لعرض المنتجات باحترافية، وتنظيم السلة، واستقبال الطلبات مباشرة دون تعقيدات الدفع الإلكتروني."
                : "Built for local shops across Yemen and the Arab world to showcase products, manage a cart, and receive direct WhatsApp orders without online payment friction."}
            </p>

            <div className="flex flex-wrap gap-3 pt-1">
              <button
                type="button"
                onClick={onOpenMerchant}
                className="inline-flex items-center gap-2 px-5 py-3 rounded-xl bg-souqi-teal hover:bg-souqi-tealLight text-white font-semibold text-sm sm:text-base transition-colors"
              >
                <StoreIcon className="w-4 h-4" />
                <span>{isAr ? "أنشئ متجرك الآن" : "Create Your Store"}</span>
                <ArrowIcon className="w-4 h-4" />
              </button>

              <button
                type="button"
                onClick={onBrowseDemo}
                className="inline-flex items-center gap-2 px-5 py-3 rounded-xl bg-[#FAF7F2] hover:bg-stone-200/70 border border-[#DFD8CA] text-slate-800 font-semibold text-sm sm:text-base transition-colors"
              >
                <span>{isAr ? "معاينة متجر تجريبي" : "Try Demo Storefront"}</span>
              </button>

              <button
                type="button"
                onClick={onOpenExplore}
                className="inline-flex items-center gap-2 px-4 py-3 rounded-xl text-souqi-teal hover:bg-teal-50 font-semibold text-sm transition-colors"
              >
                <span>{isAr ? "تصفح المتاجر" : "Browse Directory"}</span>
              </button>
            </div>
          </div>

          {/* Clean Preview Card */}
          <div className="lg:col-span-5">
            <div className="rounded-xl border border-[#E7E2D8] bg-[#FAF7F2] p-5 space-y-4">
              <div className="flex items-center justify-between border-b border-[#E7E2D8] pb-3">
                <div>
                  <div className="font-display font-bold text-slate-900">
                    {isAr ? "مناحل وخيرات صنعاء" : "Sana'a Honey & Roastery"}
                  </div>
                  <div className="text-xs text-slate-500" dir="ltr">
                    souqi.app/#/s/sanaa-honey
                  </div>
                </div>
                <span className="px-2.5 py-1 rounded-full bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-semibold">
                  {isAr ? "متجر موثق" : "Verified"}
                </span>
              </div>

              <div className="space-y-2.5 text-sm">
                <div className="flex items-center justify-between bg-white rounded-lg p-3 border border-[#EAE5DC]">
                  <span className="font-medium text-slate-800">
                    {isAr ? "عسل سدر دوعني (1 كجم)" : "Doani Sidr Honey (1kg)"}
                  </span>
                  <span className="font-bold text-souqi-teal tabular-nums">32,000 {isAr ? "ر.ي" : "YER"}</span>
                </div>
                <div className="flex items-center justify-between bg-white rounded-lg p-3 border border-[#EAE5DC]">
                  <span className="font-medium text-slate-800">
                    {isAr ? "بن حرازي محمص (500 جم)" : "Harazi Coffee (500g)"}
                  </span>
                  <span className="font-bold text-souqi-teal tabular-nums">9,500 {isAr ? "ر.ي" : "YER"}</span>
                </div>
              </div>

              <div className="rounded-lg bg-souqi-whatsapp/10 border border-souqi-whatsapp/25 p-3 flex items-center justify-between text-xs text-souqi-whatsapp font-semibold">
                <span className="flex items-center gap-2">
                  <MessageCircle className="w-4 h-4" />
                  {isAr ? "جاهز للإرسال كرسالة واتساب منسقة" : "Ready as a formatted WhatsApp order"}
                </span>
                <span className="tabular-nums">41,500</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Benefits Grid */}
      <section className="space-y-5">
        <div className="space-y-1">
          <h2 className="text-xl sm:text-2xl font-bold text-slate-900">
            {isAr ? "مصمم ليناسب واقع التجارة المحلية" : "Built for Real Local Commerce"}
          </h2>
          <p className="text-sm text-slate-600">
            {isAr ? "كل ما يحتاجه المحل التجاري بدون تعقيد أو خطوات زائدة." : "Everything a local shop needs without bloat."}
          </p>
        </div>

        <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {benefits.map((item) => {
            const Icon = item.icon;
            return (
              <div key={item.title} className="rounded-xl border border-[#E7E2D8] bg-white p-5 space-y-2.5">
                <div className="w-10 h-10 rounded-lg bg-teal-50 text-souqi-teal flex items-center justify-center">
                  <Icon className="w-5 h-5" />
                </div>
                <h3 className="font-bold text-slate-900 text-base">{item.title}</h3>
                <p className="text-sm text-slate-600 leading-relaxed">{item.body}</p>
              </div>
            );
          })}
        </div>
      </section>

      {/* How It Works */}
      <section className="space-y-5">
        <h2 className="text-xl sm:text-2xl font-bold text-slate-900">
          {isAr ? "كيف يعمل سوقي في 3 خطوات؟" : "How Souqi Works in 3 Steps"}
        </h2>
        <div className="grid md:grid-cols-3 gap-4">
          {steps.map((s) => (
            <div key={s.num} className="rounded-xl border border-[#E7E2D8] bg-white p-5 space-y-2">
              <span className="inline-block font-display text-sm font-bold text-souqi-amber tabular-nums">{s.num}</span>
              <h3 className="font-bold text-slate-900 text-base">{s.title}</h3>
              <p className="text-sm text-slate-600 leading-relaxed">{s.body}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Pricing Plans */}
      <section className="space-y-5">
        <div className="space-y-1">
          <h2 className="text-xl sm:text-2xl font-bold text-slate-900">
            {isAr ? "باقات واضحة تناسب حجم متجرك" : "Simple Plans for Every Shop"}
          </h2>
          <p className="text-sm text-slate-600">
            {isAr
              ? "ابدأ مجاناً، وتفعّل الباقات المدفوعة يدوياً عبر الإدارة دون الحاجة لبطاقة بنكية."
              : "Start free. Paid plans are activated manually by platform admins without online payment gateways."}
          </p>
        </div>

        <div className="grid md:grid-cols-3 gap-4">
          {plans.map((plan, idx) => (
            <div
              key={plan.id}
              className={`rounded-xl border p-6 flex flex-col justify-between space-y-5 ${
                idx === 1 ? "border-souqi-teal bg-white ring-1 ring-souqi-teal/20" : "border-[#E7E2D8] bg-white"
              }`}
            >
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <h3 className="font-bold text-lg text-slate-900">{isAr ? plan.nameAr : plan.nameEn}</h3>
                  {idx === 1 && (
                    <span className="text-xs px-2.5 py-0.5 rounded-full bg-teal-50 text-souqi-teal font-semibold">
                      {isAr ? "الأكثر طلباً" : "Popular"}
                    </span>
                  )}
                </div>

                <div className="flex items-baseline gap-2">
                  <span className="text-2xl font-bold text-souqi-teal tabular-nums">
                    {plan.priceMonthlyUsd === 0
                      ? isAr
                        ? "مجاني"
                        : "Free"
                      : formatCurrency(plan.priceMonthlyUsd, "USD", lang)}
                  </span>
                  {plan.priceMonthlyYer > 0 && (
                    <span className="text-xs text-slate-500 tabular-nums">
                      / {formatCurrency(plan.priceMonthlyYer, "YER", lang)} {isAr ? "شهرياً" : "/ mo"}
                    </span>
                  )}
                </div>

                <p className="text-sm text-slate-600 leading-relaxed">{isAr ? plan.descriptionAr : plan.descriptionEn}</p>

                <ul className="space-y-1.5 text-sm text-slate-700 pt-1">
                  <li className="flex items-center gap-2">
                    <Check className="w-4 h-4 text-emerald-700 shrink-0" />
                    <span>
                      {isAr ? `حتى ${plan.maxProducts} منتج` : `Up to ${plan.maxProducts} products`}
                    </span>
                  </li>
                  <li className="flex items-center gap-2">
                    <Check className="w-4 h-4 text-emerald-700 shrink-0" />
                    <span>{isAr ? "طلبات واتساب غير محدودة" : "Unlimited WhatsApp order inquiries"}</span>
                  </li>
                </ul>
              </div>

              <button
                type="button"
                onClick={onOpenMerchant}
                className={`w-full py-2.5 px-4 rounded-xl text-sm font-semibold transition-colors ${
                  idx === 1
                    ? "bg-souqi-teal text-white hover:bg-souqi-tealLight"
                    : "bg-[#FAF7F2] text-slate-800 border border-[#DFD8CA] hover:bg-stone-200/60"
                }`}
              >
                {isAr ? "ابدأ بهذه الباقة" : "Get Started"}
              </button>
            </div>
          ))}
        </div>
      </section>

      {/* FAQ */}
      <section className="space-y-4">
        <h2 className="text-xl sm:text-2xl font-bold text-slate-900">
          {isAr ? "الأسئلة الشائعة" : "Frequently Asked Questions"}
        </h2>
        <div className="space-y-2.5">
          {faqs.map((item, i) => {
            const isOpen = openFaq === i;
            return (
              <div key={i} className="rounded-xl border border-[#E7E2D8] bg-white overflow-hidden">
                <button
                  type="button"
                  onClick={() => setOpenFaq(isOpen ? null : i)}
                  className="w-full px-5 py-4 flex items-center justify-between text-start font-semibold text-slate-900 text-sm sm:text-base hover:bg-stone-50"
                >
                  <span>{isAr ? item.qAr : item.qEn}</span>
                  <ChevronDown className={`w-4 h-4 text-slate-500 transition-transform ${isOpen ? "rotate-180" : ""}`} />
                </button>
                {isOpen && (
                  <div className="px-5 pb-4 text-sm text-slate-600 leading-relaxed border-t border-stone-100 pt-3">
                    {isAr ? item.aAr : item.aEn}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </section>

      {/* Final CTA + Footer */}
      <section className="rounded-2xl bg-souqi-teal text-white p-6 sm:p-8 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="space-y-1">
          <h3 className="text-lg sm:text-xl font-bold">
            {isAr ? "جاهز لإطلاق رابط متجرك اليوم؟" : "Ready to launch your storefront today?"}
          </h3>
          <p className="text-sm text-teal-100">
            {isAr ? "ابدأ الآن مجاناً وشارك منتجاتك مع عملائك عبر واتساب." : "Start free and share your catalog via WhatsApp."}
          </p>
        </div>
        <button
          type="button"
          onClick={onOpenMerchant}
          className="px-5 py-3 rounded-xl bg-white text-souqi-teal font-bold text-sm hover:bg-teal-50 transition-colors shrink-0"
        >
          {isAr ? "فتح لوحة التاجر" : "Open Merchant Dashboard"}
        </button>
      </section>
    </div>
  );
};
