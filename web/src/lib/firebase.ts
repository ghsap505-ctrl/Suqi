import { initializeApp, getApps } from "firebase/app";
import { getAuth, GoogleAuthProvider, signInWithPopup, signOut } from "firebase/auth";
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  getFirestore,
  limit,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where,
} from "firebase/firestore";
import { Category, InquiryStatus, OrderInquiry, Product, Store, SubscriptionPlan } from "../types";

const apiKey = import.meta.env.VITE_FIREBASE_API_KEY as string | undefined;
const authDomain = import.meta.env.VITE_FIREBASE_AUTH_DOMAIN as string | undefined;
const projectId = import.meta.env.VITE_FIREBASE_PROJECT_ID as string | undefined;
const appId = import.meta.env.VITE_FIREBASE_APP_ID as string | undefined;
const databaseId =
  (import.meta.env.VITE_FIRESTORE_DATABASE_ID as string | undefined) ||
  "ai-studio-android-b074c050-371c-477b-80a6-667bdf5c3fb1";

export const isFirebaseConfigured = Boolean(apiKey && projectId && appId);

const app = isFirebaseConfigured
  ? getApps()[0] ??
    initializeApp({
      apiKey,
      authDomain,
      projectId,
      appId,
    })
  : null;

export const auth = app ? getAuth(app) : null;
export const db = app ? getFirestore(app, databaseId) : null;

export async function signInWithGoogleWeb() {
  if (!auth) throw new Error("Firebase Web configuration missing (.env.local)");
  const provider = new GoogleAuthProvider();
  return signInWithPopup(auth, provider);
}

export async function signOutWeb() {
  if (auth) await signOut(auth);
}

export async function ensureUserProfileWeb(uid: string, displayName: string, email: string) {
  if (!db) return;
  const ref = doc(db, "users", uid);
  const snap = await getDoc(ref);
  if (!snap.exists()) {
    await setDoc(ref, {
      userId: uid,
      displayName: displayName.slice(0, 80) || "تاجر سوقي",
      email: email.slice(0, 120) || "merchant@souqi.app",
      role: "merchant",
      preferredLang: "ar",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  }
}

export async function fetchActiveStoresWeb(): Promise<Store[]> {
  if (!db) return [];
  const q = query(collection(db, "stores"), where("active", "==", true), limit(60));
  const snap = await getDocs(q);
  return snap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<Store, "id">) }));
}

export async function fetchMyStoresWeb(uid: string): Promise<Store[]> {
  if (!db) return [];
  const q = query(collection(db, "stores"), where("ownerUid", "==", uid), limit(10));
  const snap = await getDocs(q);
  return snap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<Store, "id">) }));
}

export async function fetchStoreCatalogWeb(store: Store, viewerUid?: string, isAdmin = false) {
  if (!db) return { categories: [] as Category[], products: [] as Product[] };
  const ownerAccess = isAdmin || (viewerUid && viewerUid === store.ownerUid);
  const catQuery = query(
    collection(db, "stores", store.id, "categories"),
    ownerAccess ? where("ownerUid", "==", store.ownerUid) : where("active", "==", true),
    limit(50)
  );
  const prodQuery = query(
    collection(db, "stores", store.id, "products"),
    ownerAccess ? where("ownerUid", "==", store.ownerUid) : where("available", "==", true),
    limit(150)
  );
  const [catSnap, prodSnap] = await Promise.all([getDocs(catQuery), getDocs(prodQuery)]);
  const categories = catSnap.docs
    .map((d) => ({ id: d.id, ...(d.data() as Omit<Category, "id">) }))
    .sort((a, b) => a.sortOrder - b.sortOrder);
  const products = prodSnap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<Product, "id">) }));
  return { categories, products };
}

export async function createStoreWeb(uid: string, store: Omit<Store, "id" | "ownerUid" | "verified" | "planId" | "subscriptionStatus">) {
  if (!db) throw new Error("Firebase not configured");
  const ref = doc(collection(db, "stores"));
  await setDoc(ref, {
    ownerUid: uid,
    name: store.name,
    slug: store.slug,
    description: store.description,
    city: store.city,
    whatsappPhone: store.whatsappPhone,
    currency: store.currency,
    accentKey: store.accentKey,
    coverPreset: store.coverPreset,
    active: store.active,
    verified: false,
    planId: "starter",
    subscriptionStatus: "trial",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });
  return ref.id;
}

export async function updateStoreWeb(storeId: string, patch: Partial<Store>) {
  if (!db) throw new Error("Firebase not configured");
  await updateDoc(doc(db, "stores", storeId), {
    ...patch,
    updatedAt: serverTimestamp(),
  });
}

export async function upsertCategoryWeb(storeId: string, uid: string, cat: Partial<Category> & { id?: string }) {
  if (!db) throw new Error("Firebase not configured");
  const sortOrder = Math.max(0, Math.min(500, Math.trunc(Number(cat.sortOrder ?? 1))));
  if (!cat.id) {
    const ref = doc(collection(db, "stores", storeId, "categories"));
    await setDoc(ref, {
      storeId,
      ownerUid: uid,
      nameAr: (cat.nameAr || "").trim().slice(0, 60),
      nameEn: (cat.nameEn || cat.nameAr || "").trim().slice(0, 60),
      sortOrder,
      active: cat.active ?? true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
    return ref.id;
  } else {
    await updateDoc(doc(db, "stores", storeId, "categories", cat.id), {
      nameAr: (cat.nameAr || "").trim().slice(0, 60),
      nameEn: (cat.nameEn || cat.nameAr || "").trim().slice(0, 60),
      sortOrder,
      active: cat.active ?? true,
      updatedAt: serverTimestamp(),
    });
    return cat.id;
  }
}

export async function deleteCategoryWeb(storeId: string, catId: string) {
  if (!db) return;
  await deleteDoc(doc(db, "stores", storeId, "categories", catId));
}

export async function upsertProductWeb(storeId: string, uid: string, prod: Partial<Product> & { id?: string }) {
  if (!db) throw new Error("Firebase not configured");
  const stockCount = Math.max(0, Math.min(100000, Math.trunc(Number(prod.stockCount ?? 0))));
  const price = Number(prod.price ?? 0);
  if (!prod.id) {
    const ref = doc(collection(db, "stores", storeId, "products"));
    await setDoc(ref, {
      storeId,
      ownerUid: uid,
      categoryId: prod.categoryId,
      nameAr: (prod.nameAr || "").trim().slice(0, 100),
      nameEn: (prod.nameEn || prod.nameAr || "").trim().slice(0, 100),
      descriptionAr: (prod.descriptionAr ?? "").trim().slice(0, 600),
      descriptionEn: (prod.descriptionEn ?? "").trim().slice(0, 600),
      price,
      stockCount,
      available: prod.available ?? true,
      imagePreset: prod.imagePreset ?? "honey",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
    return ref.id;
  } else {
    await updateDoc(doc(db, "stores", storeId, "products", prod.id), {
      categoryId: prod.categoryId,
      nameAr: (prod.nameAr || "").trim().slice(0, 100),
      nameEn: (prod.nameEn || prod.nameAr || "").trim().slice(0, 100),
      descriptionAr: (prod.descriptionAr ?? "").trim().slice(0, 600),
      descriptionEn: (prod.descriptionEn ?? "").trim().slice(0, 600),
      price,
      stockCount,
      available: prod.available ?? true,
      imagePreset: prod.imagePreset ?? "honey",
      updatedAt: serverTimestamp(),
    });
    return prod.id;
  }
}

export async function deleteProductWeb(storeId: string, productId: string) {
  if (!db) return;
  await deleteDoc(doc(db, "stores", storeId, "products", productId));
}

export async function submitOrderInquiryWeb(customerUid: string, inquiry: Omit<OrderInquiry, "id" | "customerUid" | "status">) {
  if (!db) throw new Error("Firebase not configured");
  const ref = doc(collection(db, "order_inquiries"));
  await setDoc(ref, {
    ...inquiry,
    itemCount: Math.max(1, Math.min(500, Math.trunc(Number(inquiry.itemCount)))),
    customerUid,
    status: "pending_whatsapp",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });
  return ref.id;
}

export async function fetchMerchantInquiriesWeb(uid: string): Promise<OrderInquiry[]> {
  if (!db) return [];
  const q = query(collection(db, "order_inquiries"), where("storeOwnerUid", "==", uid), limit(100));
  const snap = await getDocs(q);
  return snap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<OrderInquiry, "id">) }));
}

export async function updateInquiryStatusWeb(inquiryId: string, status: InquiryStatus) {
  if (!db) return;
  await updateDoc(doc(db, "order_inquiries", inquiryId), {
    status,
    updatedAt: serverTimestamp(),
  });
}

export async function checkIsAdminWeb(uid: string, email?: string | null, emailVerified?: boolean): Promise<boolean> {
  if (emailVerified && email === "alhawa.alhamdani@gmail.com") return true;
  if (!db) return false;
  try {
    const snap = await getDoc(doc(db, "admins", uid));
    return snap.exists();
  } catch {
    return false;
  }
}

export async function fetchPlansWeb(): Promise<SubscriptionPlan[]> {
  if (!db) return [];
  const snap = await getDocs(query(collection(db, "plans"), where("active", "==", true), limit(20)));
  return snap.docs
    .map((d) => ({ id: d.id, ...(d.data() as Omit<SubscriptionPlan, "id">) }))
    .sort((a, b) => a.sortOrder - b.sortOrder);
}

export async function fetchAdminDashboardWeb(): Promise<{
  stores: Store[];
  plans: SubscriptionPlan[];
  inquiriesCount: number;
}> {
  if (!db) return { stores: [], plans: [], inquiriesCount: 0 };
  const [storesSnap, plansSnap, inqSnap] = await Promise.all([
    getDocs(query(collection(db, "stores"), limit(100))),
    getDocs(query(collection(db, "plans"), limit(20))),
    getDocs(query(collection(db, "order_inquiries"), limit(200))),
  ]);
  return {
    stores: storesSnap.docs.map((d) => ({ id: d.id, ...(d.data() as Omit<Store, "id">) })),
    plans: plansSnap.docs
      .map((d) => ({ id: d.id, ...(d.data() as Omit<SubscriptionPlan, "id">) }))
      .sort((a, b) => a.sortOrder - b.sortOrder),
    inquiriesCount: inqSnap.size,
  };
}

export async function adminUpdateStoreWeb(
  storeId: string,
  payload: { verified: boolean; active: boolean; planId: string; subscriptionStatus: string }
) {
  if (!db) return;
  await updateDoc(doc(db, "stores", storeId), {
    ...payload,
    updatedAt: serverTimestamp(),
  });
}

export async function adminSavePlanWeb(plan: SubscriptionPlan) {
  if (!db) return;
  const id = plan.id || doc(collection(db, "plans")).id;
  const ref = doc(db, "plans", id);
  const existing = await getDoc(ref);
  const maxProducts = Math.max(1, Math.min(10000, Math.trunc(Number(plan.maxProducts))));
  const sortOrder = Math.max(0, Math.min(100, Math.trunc(Number(plan.sortOrder))));
  if (existing.exists()) {
    await updateDoc(ref, {
      nameAr: plan.nameAr.trim().slice(0, 60),
      nameEn: plan.nameEn.trim().slice(0, 60),
      priceMonthlyUsd: Number(plan.priceMonthlyUsd),
      priceMonthlyYer: Number(plan.priceMonthlyYer),
      maxProducts,
      descriptionAr: plan.descriptionAr.trim().slice(0, 300),
      descriptionEn: plan.descriptionEn.trim().slice(0, 300),
      active: plan.active,
      sortOrder,
      updatedAt: serverTimestamp(),
    });
  } else {
    await setDoc(ref, {
      nameAr: plan.nameAr.trim().slice(0, 60),
      nameEn: plan.nameEn.trim().slice(0, 60),
      priceMonthlyUsd: Number(plan.priceMonthlyUsd),
      priceMonthlyYer: Number(plan.priceMonthlyYer),
      maxProducts,
      descriptionAr: plan.descriptionAr.trim().slice(0, 300),
      descriptionEn: plan.descriptionEn.trim().slice(0, 300),
      active: plan.active,
      sortOrder,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  }
}
