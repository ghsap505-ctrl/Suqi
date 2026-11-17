const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const MERCHANT_A_UID = "merchant_a_123";
const MERCHANT_B_UID = "merchant_b_456";
const CUSTOMER_UID = "customer_789";
const ADMIN_EMAIL = "alhawa.alhamdani@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write stores, products, or inquiries", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("stores").where("active", "==", true).get());
  await assertFails(unauth.collection("order_inquiries").get());
  await assertFails(
    unauth.collection("stores").doc("store_1").set({
      ownerUid: MERCHANT_A_UID,
      name: "Unauthorized",
    })
  );
});

test("Tenant isolation: Merchant A creates store/category/product; Merchant B cannot mutate them", async () => {
  const dbA = testEnv.authenticatedContext(MERCHANT_A_UID).firestore();
  const dbB = testEnv.authenticatedContext(MERCHANT_B_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    dbA.collection("stores").doc("store_a").set({
      ownerUid: MERCHANT_A_UID,
      name: "عسل صنعاء",
      slug: "sanaa-honey",
      description: "عسل سدر يمني أصلي",
      city: "صنعاء",
      whatsappPhone: "+967770001111",
      currency: "YER",
      accentKey: "amber",
      coverPreset: "honey",
      active: true,
      verified: false,
      planId: "starter",
      subscriptionStatus: "trial",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Merchant cannot self-verify or self-upgrade plan
  await assertFails(
    dbA.collection("stores").doc("store_a").update({
      verified: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    dbA.collection("stores").doc("store_a").update({
      planId: "business",
      subscriptionStatus: "active",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Merchant A creates category & product in their store
  await assertSucceeds(
    dbA.collection("stores").doc("store_a").collection("categories").doc("cat_1").set({
      storeId: "store_a",
      ownerUid: MERCHANT_A_UID,
      nameAr: "عسل السدر",
      nameEn: "Sidr Honey",
      sortOrder: 1,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    dbA.collection("stores").doc("store_a").collection("products").doc("prod_1").set({
      storeId: "store_a",
      ownerUid: MERCHANT_A_UID,
      categoryId: "cat_1",
      nameAr: "عسل سدر دوعني",
      nameEn: "Doani Sidr Honey",
      descriptionAr: "عسل طبيعي 100%",
      descriptionEn: "100% pure natural honey",
      price: 25000,
      stockCount: 15,
      available: true,
      imagePreset: "honey",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Merchant B cannot create or modify products inside Merchant A's store
  await assertFails(
    dbB.collection("stores").doc("store_a").collection("products").doc("prod_hijack").set({
      storeId: "store_a",
      ownerUid: MERCHANT_B_UID,
      categoryId: "cat_1",
      nameAr: "منتج مزور",
      nameEn: "Fake",
      descriptionAr: "",
      descriptionEn: "",
      price: 100,
      stockCount: 1,
      available: true,
      imagePreset: "honey",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(
    dbB.collection("stores").doc("store_a").update({
      name: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Order inquiry PII isolation & terminal state locking", async () => {
  const dbA = testEnv.authenticatedContext(MERCHANT_A_UID).firestore();
  const dbB = testEnv.authenticatedContext(MERCHANT_B_UID).firestore();
  const dbCust = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    dbA.collection("stores").doc("store_a").set({
      ownerUid: MERCHANT_A_UID,
      name: "متجر الأناقة",
      slug: "anaqa-shop",
      description: "ملابس وعطور",
      city: "عدن",
      whatsappPhone: "+967733000111",
      currency: "YER",
      accentKey: "teal",
      coverPreset: "fashion",
      active: true,
      verified: false,
      planId: "starter",
      subscriptionStatus: "trial",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    dbCust.collection("order_inquiries").doc("inq_1").set({
      storeId: "store_a",
      storeName: "متجر الأناقة",
      storeOwnerUid: MERCHANT_A_UID,
      customerUid: CUSTOMER_UID,
      customerName: "أحمد علي",
      customerPhone: "+967771234567",
      deliveryAddress: "عدن - كريتر",
      notes: "التوصيل عصراً",
      itemsSummary: "عباية كتان x 1",
      itemCount: 1,
      totalAmount: 18000,
      currency: "YER",
      status: "pending_whatsapp",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Merchant B cannot read Customer's PII order inquiry
  await assertFails(dbB.collection("order_inquiries").doc("inq_1").get());
  // Unfiltered list fails
  await assertFails(dbB.collection("order_inquiries").get());

  // Merchant A can read their store's inquiries & transition status to confirmed
  await assertSucceeds(
    dbA.collection("order_inquiries").where("storeOwnerUid", "==", MERCHANT_A_UID).get()
  );
  await assertSucceeds(
    dbA.collection("order_inquiries").doc("inq_1").update({
      status: "confirmed",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Once confirmed (terminal state), Merchant A cannot revert status
  await assertFails(
    dbA.collection("order_inquiries").doc("inq_1").update({
      status: "cancelled",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Role security & bootstrapped admin plan/subscription controls", async () => {
  const dbCust = testEnv.authenticatedContext(CUSTOMER_UID, {
    email: "user@example.com",
    email_verified: true,
  }).firestore();
  const dbSpoofAdmin = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  const dbAdmin = testEnv.authenticatedContext("admin_uid", {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 2000);

  // Regular user cannot self-assign admin role
  await assertFails(
    dbCust.collection("users").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      displayName: "Customer",
      email: "user@example.com",
      role: "admin",
      preferredLang: "ar",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Unverified email spoofing admin fails
  await assertFails(
    dbSpoofAdmin.collection("plans").doc("pro").set({
      nameAr: "برو",
      nameEn: "Pro",
      priceMonthlyUsd: 15,
      priceMonthlyYer: 8000,
      maxProducts: 200,
      descriptionAr: "خطة التجار المتقدمة",
      descriptionEn: "Advanced merchant plan",
      active: true,
      sortOrder: 2,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Verified bootstrapped admin succeeds
  await assertSucceeds(
    dbAdmin.collection("plans").doc("pro").set({
      nameAr: "باقة النمو",
      nameEn: "Growth Plan",
      priceMonthlyUsd: 15,
      priceMonthlyYer: 8000,
      maxProducts: 200,
      descriptionAr: "خطة التجار المتقدمة",
      descriptionEn: "Advanced merchant plan",
      active: true,
      sortOrder: 2,
      createdAt: now,
      updatedAt: now,
    })
  );
});
