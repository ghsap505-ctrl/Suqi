# سوقي (Souqi) — Arabic-First Multi-Tenant WhatsApp E-Commerce Platform (Android)

Souqi is an Arabic-first (`RTL` default, `LTR` English toggle) multi-tenant e-commerce Android application built with **Kotlin**, **Jetpack Compose (Material 3)**, **Firebase Auth (Google Sign-In via Credential Manager)**, and **Cloud Firestore** (Enterprise named database).

## Key Features
- **Public Landing & Auth Gate (`AuthLandingScreen`)**: Hero banner, benefits, 3-step onboarding flow, pricing preview, expandable FAQ, and Google Sign-In (`GetSignInWithGoogleOption`).
- **Marketplace Directory (`ExploreScreen`)**: Interactive Yemeni/Arab demo storefronts (Sana'a Honey, Dar Al-Anaqa, TechZone Aden) + live multi-tenant cloud stores with search.
- **Public Storefront & WhatsApp Cart (`StorefrontAndCartScreen`)**: Store cover, share link (`https://souqi.app/s/<slug>`), category filters, product search, stock availability, product detail bottom sheet, quantity-editable cart, customer delivery form, explicit non-confirmation disclaimer, and URL-encoded `https://wa.me/...` order generation (plus clipboard fallback).
- **Merchant Dashboard (`MerchantDashboardScreen`)**: Store onboarding & settings (with zero-permission Android Photo Picker), Categories & Products CRUD, order inquiry lifecycle management, and overview analytics chart.
- **Platform Admin Console (`AdminDashboardScreen`)**: Role-gated store verification/suspension, manual subscription activation (no paid payment gateway in MVP), pricing plans CRUD, and platform reports.

## Architecture & Security
- **Multi-Tenant Isolation**:
  - `stores/{storeId}` stores `ownerUid`, `planId`, `subscriptionStatus`, and `verified`.
  - `stores/{storeId}/categories/{categoryId}` and `stores/{storeId}/products/{productId}` enforce parent-store ownership in `firestore.rules`.
  - `order_inquiries/{inquiryId}` isolates customer PII (`customerPhone`, `deliveryAddress`) strictly to `customerUid`, `storeOwnerUid`, and verified platform admins, with terminal state locking (`confirmed` / `cancelled`).
- **Files**:
  - `firebase-blueprint.json`: Firestore entity & collection IR.
  - `firestore.rules`: Hardened zero-trust security rules.
  - `firebase.json`: Single-database emulator config (`port 8085`).
  - `firestore.test.js`: Security rules tests (`@firebase/rules-unit-testing`).

## Local Setup & Verification
1. Provision Firebase in AI Studio (`app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml`).
2. Build APK: `gradle :app:assembleDebug`
3. Run unit & Robolectric tests: `gradle :app:testDebugUnitTest`
4. Run Firestore rules tests (with Firebase CLI + Node):
   ```bash
   firebase emulators:start --only firestore --project demo-no-project
   FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
   ```

## Production Readiness & Known Limitations
- **MVP Payments**: Subscription upgrades are manually activated by the platform admin (`trial` / `active` / `pending` / `suspended`); no online payment gateway is integrated.
- **Image Storage**: Product and store photos selected via Android Photo Picker store local `Uri` references; for cross-device merchant media sync, configure Firebase Storage in the Firebase Console.
- **Public Store Web Links**: Shared `https://souqi.app/s/<slug>` links require configuring Android App Links / a companion web host for browser fallback.
