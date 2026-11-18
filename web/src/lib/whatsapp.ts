import type { AppLang, CartItem, CurrencyCode, Store } from "../types";

export function formatCurrency(amount: number, currency: CurrencyCode, lang: AppLang): string {
  const formatted = new Intl.NumberFormat("en-US", {
    maximumFractionDigits: currency === "USD" ? 2 : 0,
  }).format(amount);

  if (currency === "YER") return lang === "ar" ? `${formatted} ر.ي` : `${formatted} YER`;
  if (currency === "SAR") return lang === "ar" ? `${formatted} ر.س` : `${formatted} SAR`;
  return `$${formatted}`;
}

export function normalizeWhatsAppPhone(raw: string): string {
  const digits = raw.replace(/\D/g, "");
  return digits.startsWith("00") ? digits.slice(2) : digits;
}

export function sanitizeSlug(raw: string): string {
  return raw
    .trim()
    .toLowerCase()
    .replace(/[_\s]+/g, "-")
    .replace(/[^a-z0-9-]/g, "")
    .replace(/-+/g, "-")
    .replace(/^-|-$/g, "");
}

export function calculateCartTotals(items: CartItem[]): { totalItems: number; subtotal: number } {
  let totalItems = 0;
  let subtotal = 0;
  for (const item of items) {
    if (item.quantity > 0 && item.product.price > 0) {
      totalItems += item.quantity;
      subtotal += item.product.price * item.quantity;
    }
  }
  return { totalItems, subtotal };
}

export function buildItemsSummary(items: CartItem[], lang: AppLang): string {
  return items
    .filter((i) => i.quantity > 0)
    .map((i) => {
      const name = lang === "ar" ? i.product.nameAr : i.product.nameEn || i.product.nameAr;
      return `${name} × ${i.quantity}`;
    })
    .join(" | ");
}

export function buildWhatsAppOrderMessage(params: {
  store: Store;
  items: CartItem[];
  customerName: string;
  customerPhone: string;
  deliveryAddress: string;
  notes: string;
  lang: AppLang;
}): string {
  const { store, items, customerName, customerPhone, deliveryAddress, notes, lang } = params;
  const totals = calculateCartTotals(items);
  const lines: string[] = [];

  if (lang === "ar") {
    lines.push(`مرحباً ${store.name}، أود إرسال طلب جديد عبر منصة سوقي:`);
    lines.push("------------------------------");
    items
      .filter((i) => i.quantity > 0)
      .forEach((item, idx) => {
        const unit = formatCurrency(item.product.price, store.currency, lang);
        const total = formatCurrency(item.product.price * item.quantity, store.currency, lang);
        lines.push(`${idx + 1}. ${item.product.nameAr} × ${item.quantity} (${unit}) = ${total}`);
      });
    lines.push("------------------------------");
    lines.push(`عدد القطع: ${totals.totalItems}`);
    lines.push(`الإجمالي التقديري: ${formatCurrency(totals.subtotal, store.currency, lang)}`);
    lines.push(`الاسم: ${customerName.trim()}`);
    lines.push(`الهاتف: ${customerPhone.trim()}`);
    lines.push(`العنوان: ${deliveryAddress.trim()}`);
    if (notes.trim()) lines.push(`ملاحظات: ${notes.trim()}`);
    lines.push("تنبيه: هذا طلب مبدئي ولا يُعد مؤكداً إلا بعد تأكيد التاجر.");
  } else {
    lines.push(`Hello ${store.name}, I would like to place a new order inquiry via Souqi:`);
    lines.push("------------------------------");
    items
      .filter((i) => i.quantity > 0)
      .forEach((item, idx) => {
        const name = item.product.nameEn || item.product.nameAr;
        const unit = formatCurrency(item.product.price, store.currency, lang);
        const total = formatCurrency(item.product.price * item.quantity, store.currency, lang);
        lines.push(`${idx + 1}. ${name} × ${item.quantity} (${unit}) = ${total}`);
      });
    lines.push("------------------------------");
    lines.push(`Items: ${totals.totalItems}`);
    lines.push(`Estimated Total: ${formatCurrency(totals.subtotal, store.currency, lang)}`);
    lines.push(`Customer: ${customerName.trim()}`);
    lines.push(`Phone: ${customerPhone.trim()}`);
    lines.push(`Address: ${deliveryAddress.trim()}`);
    if (notes.trim()) lines.push(`Notes: ${notes.trim()}`);
    lines.push("Notice: This order is pending and NOT confirmed until confirmed by the merchant.");
  }

  return lines.join("\n");
}

export function buildWhatsAppUrl(phone: string, message: string): string {
  const digits = normalizeWhatsAppPhone(phone);
  return `https://wa.me/${digits}?text=${encodeURIComponent(message)}`;
}

export function validateCheckout(params: {
  items: CartItem[];
  customerName: string;
  customerPhone: string;
  deliveryAddress: string;
  notes: string;
  lang: AppLang;
}): string | null {
  const { items, customerName, customerPhone, deliveryAddress, notes, lang } = params;
  const isAr = lang === "ar";
  if (!items.some((i) => i.quantity > 0)) return isAr ? "السلة فارغة" : "Cart is empty";
  if (customerName.trim().length < 2 || customerName.trim().length > 80) {
    return isAr ? "يرجى إدخال اسم صحيح (2-80 حرفاً)" : "Enter a valid name (2-80 chars)";
  }
  const digits = normalizeWhatsAppPhone(customerPhone);
  if (digits.length < 7 || digits.length > 20) {
    return isAr ? "يرجى إدخال رقم هاتف صحيح" : "Enter a valid phone number";
  }
  if (deliveryAddress.trim().length < 3 || deliveryAddress.trim().length > 240) {
    return isAr ? "يرجى إدخال عنوان التوصيل" : "Enter delivery address";
  }
  if (notes.trim().length > 400) {
    return isAr ? "الملاحظات طويلة جداً" : "Notes exceed 400 chars";
  }
  return null;
}
