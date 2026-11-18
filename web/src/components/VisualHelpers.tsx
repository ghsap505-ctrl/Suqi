import React from "react";
import { Coffee, Droplets, Headphones, Package, ShoppingBag, Smartphone, Sparkles, Watch } from "lucide-react";
import { Store } from "../types";

export function getAccentClasses(accentKey: Store["accentKey"]) {
  switch (accentKey) {
    case "amber":
      return {
        bg: "bg-amber-700",
        softBg: "bg-amber-50",
        text: "text-amber-800",
        border: "border-amber-200",
        banner: "bg-gradient-to-l from-amber-800 via-amber-700 to-amber-900",
      };
    case "terracotta":
      return {
        bg: "bg-souqi-terracotta",
        softBg: "bg-orange-50",
        text: "text-orange-900",
        border: "border-orange-200",
        banner: "bg-gradient-to-l from-[#9D3E2B] via-[#C8553D] to-[#7A2E20]",
      };
    case "emerald":
      return {
        bg: "bg-souqi-emerald",
        softBg: "bg-emerald-50",
        text: "text-emerald-900",
        border: "border-emerald-200",
        banner: "bg-gradient-to-l from-emerald-900 via-emerald-800 to-teal-900",
      };
    default:
      return {
        bg: "bg-souqi-teal",
        softBg: "bg-teal-50",
        text: "text-teal-900",
        border: "border-teal-200",
        banner: "bg-gradient-to-l from-[#0B3945] via-[#0F4C5C] to-[#175B6D]",
      };
  }
}

export function ProductPresetVisual({ preset, className = "h-36" }: { preset: string; className?: string }) {
  const configs: Record<string, { bg: string; iconColor: string; label: string; Icon: React.ComponentType<{ className?: string }> }> = {
    honey: { bg: "bg-amber-50 border-amber-100", iconColor: "text-amber-700", label: "عسل طبيعي", Icon: Droplets },
    coffee: { bg: "bg-stone-100 border-stone-200", iconColor: "text-stone-700", label: "بن يمني", Icon: Coffee },
    abaya: { bg: "bg-orange-50 border-orange-100", iconColor: "text-[#C8553D]", label: "أزياء", Icon: ShoppingBag },
    earbuds: { bg: "bg-slate-100 border-slate-200", iconColor: "text-slate-700", label: "صوتيات", Icon: Headphones },
    watch: { bg: "bg-teal-50 border-teal-100", iconColor: "text-souqi-teal", label: "إلكترونيات", Icon: Watch },
    phone: { bg: "bg-sky-50 border-sky-100", iconColor: "text-sky-800", label: "هواتف", Icon: Smartphone },
    cosmetics: { bg: "bg-rose-50 border-rose-100", iconColor: "text-rose-700", label: "تجميل", Icon: Sparkles },
  };
  const cfg = configs[preset] ?? {
    bg: "bg-stone-100 border-stone-200",
    iconColor: "text-souqi-teal",
    label: "منتج",
    Icon: Package,
  };
  const Icon = cfg.Icon;

  return (
    <div className={`w-full rounded-xl border flex items-center justify-center ${cfg.bg} ${className}`}>
      <div className="flex flex-col items-center gap-1.5 select-none">
        <div className="w-12 h-12 rounded-full bg-white/90 border border-black/5 flex items-center justify-center shadow-sm">
          <Icon className={`w-6 h-6 ${cfg.iconColor}`} />
        </div>
      </div>
    </div>
  );
}

export function StatusBadge({ label, tone }: { label: string; tone: "teal" | "green" | "amber" | "red" | "stone" }) {
  const tones = {
    teal: "bg-teal-50 text-teal-800 border-teal-200",
    green: "bg-emerald-50 text-emerald-800 border-emerald-200",
    amber: "bg-amber-50 text-amber-800 border-amber-200",
    red: "bg-red-50 text-red-800 border-red-200",
    stone: "bg-stone-100 text-stone-700 border-stone-200",
  };
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border ${tones[tone]}`}>
      {label}
    </span>
  );
}
