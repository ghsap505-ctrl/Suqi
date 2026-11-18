import test from "node:test";
import assert from "node:assert/strict";
import {
  buildWhatsAppOrderMessage,
  buildWhatsAppUrl,
  calculateCartTotals,
  normalizeWhatsAppPhone,
  sanitizeSlug,
  validateCheckout,
} from "./whatsapp.ts";
import { demoProductsByStore, demoStores } from "../data/sampleCatalog.ts";

test("calculateCartTotals computes item count and subtotal accurately", () => {
  const storeProducts = demoProductsByStore.demo_sanaa_honey;
  const totals = calculateCartTotals([
    { product: storeProducts[0], quantity: 1 }, // 32000
    { product: storeProducts[1], quantity: 2 }, // 9500 * 2 = 19000
  ]);
  assert.equal(totals.totalItems, 3);
  assert.equal(totals.subtotal, 51000);
});

test("normalizeWhatsAppPhone strips symbols and leading 00", () => {
  assert.equal(normalizeWhatsAppPhone("+967 770-123-456"), "967770123456");
  assert.equal(normalizeWhatsAppPhone("00967733654321"), "967733654321");
});

test("sanitizeSlug formats store slug cleanly", () => {
  assert.equal(sanitizeSlug("  Sanaa  Honey_Shop! "), "sanaa-honey-shop");
});

test("buildWhatsAppOrderMessage includes non-confirmation notice and encoded URL", () => {
  const store = demoStores[0];
  const products = demoProductsByStore.demo_sanaa_honey;
  const msg = buildWhatsAppOrderMessage({
    store,
    items: [{ product: products[0], quantity: 1 }],
    customerName: "أحمد علي",
    customerPhone: "+967771112233",
    deliveryAddress: "صنعاء - شارع الزبيري",
    notes: "يرجى الاتصال قبل الوصول",
    lang: "ar",
  });

  assert.match(msg, /لا يُعد مؤكداً إلا بعد تأكيد التاجر/);
  assert.match(msg, /32,000/);

  const url = buildWhatsAppUrl(store.whatsappPhone, msg);
  assert.ok(url.startsWith("https://wa.me/967770123456?text="));
});

test("validateCheckout catches empty cart and invalid phone", () => {
  const errEmpty = validateCheckout({
    items: [],
    customerName: "Ali",
    customerPhone: "+967770123456",
    deliveryAddress: "Sanaa",
    notes: "",
    lang: "en",
  });
  assert.equal(errEmpty, "Cart is empty");

  const products = demoProductsByStore.demo_sanaa_honey;
  const errPhone = validateCheckout({
    items: [{ product: products[0], quantity: 1 }],
    customerName: "Ali",
    customerPhone: "123",
    deliveryAddress: "Sanaa",
    notes: "",
    lang: "en",
  });
  assert.equal(errPhone, "Enter a valid phone number");
});
