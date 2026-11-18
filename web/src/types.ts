export type AppLang = "ar" | "en";
export type CurrencyCode = "YER" | "SAR" | "USD";
export type SubscriptionStatus = "trial" | "active" | "pending" | "suspended";
export type InquiryStatus = "pending_whatsapp" | "contacted" | "confirmed" | "cancelled";

export interface Store {
  id: string;
  ownerUid: string;
  name: string;
  slug: string;
  description: string;
  city: string;
  whatsappPhone: string;
  currency: CurrencyCode;
  accentKey: "teal" | "amber" | "emerald" | "terracotta";
  coverPreset: "hero" | "honey" | "fashion" | "electronics";
  active: boolean;
  verified: boolean;
  planId: string;
  subscriptionStatus: SubscriptionStatus;
  demoMode?: boolean;
}

export interface Category {
  id: string;
  storeId: string;
  ownerUid: string;
  nameAr: string;
  nameEn: string;
  sortOrder: number;
  active: boolean;
}

export interface Product {
  id: string;
  storeId: string;
  ownerUid: string;
  categoryId: string;
  nameAr: string;
  nameEn: string;
  descriptionAr: string;
  descriptionEn: string;
  price: number;
  stockCount: number;
  available: boolean;
  imagePreset: string;
}

export interface OrderInquiry {
  id: string;
  storeId: string;
  storeName: string;
  storeOwnerUid: string;
  customerUid: string;
  customerName: string;
  customerPhone: string;
  deliveryAddress: string;
  notes: string;
  itemsSummary: string;
  itemCount: number;
  totalAmount: number;
  currency: CurrencyCode;
  status: InquiryStatus;
}

export interface SubscriptionPlan {
  id: string;
  nameAr: string;
  nameEn: string;
  priceMonthlyUsd: number;
  priceMonthlyYer: number;
  maxProducts: number;
  descriptionAr: string;
  descriptionEn: string;
  active: boolean;
  sortOrder: number;
}

export interface CartItem {
  product: Product;
  quantity: number;
}
