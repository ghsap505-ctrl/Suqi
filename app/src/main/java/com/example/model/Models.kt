package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: String = "merchant",
    val preferredLang: String = "ar",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName,
        "email" to email,
        "role" to "merchant",
        "preferredLang" to preferredLang,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class SubscriptionPlan(
    @DocumentId val id: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val priceMonthlyUsd: Double = 0.0,
    val priceMonthlyYer: Double = 0.0,
    val maxProducts: Long = 20L,
    val descriptionAr: String = "",
    val descriptionEn: String = "",
    val active: Boolean = true,
    val sortOrder: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "nameAr" to nameAr,
        "nameEn" to nameEn,
        "priceMonthlyUsd" to priceMonthlyUsd,
        "priceMonthlyYer" to priceMonthlyYer,
        "maxProducts" to maxProducts,
        "descriptionAr" to descriptionAr,
        "descriptionEn" to descriptionEn,
        "active" to active,
        "sortOrder" to sortOrder,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "nameAr" to nameAr,
        "nameEn" to nameEn,
        "priceMonthlyUsd" to priceMonthlyUsd,
        "priceMonthlyYer" to priceMonthlyYer,
        "maxProducts" to maxProducts,
        "descriptionAr" to descriptionAr,
        "descriptionEn" to descriptionEn,
        "active" to active,
        "sortOrder" to sortOrder,
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class Store(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val name: String = "",
    val slug: String = "",
    val description: String = "",
    val city: String = "",
    val whatsappPhone: String = "",
    val currency: String = "YER",
    val accentKey: String = "teal",
    val coverPreset: String = "hero",
    val logoUri: String? = null,
    val active: Boolean = true,
    val verified: Boolean = false,
    val planId: String = "starter",
    val subscriptionStatus: String = "trial",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val demoMode: Boolean = false, // Excluded from Firestore write payloads; used only for local demo showcase
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "ownerUid" to ownerUid,
            "name" to name,
            "slug" to slug,
            "description" to description,
            "city" to city,
            "whatsappPhone" to whatsappPhone,
            "currency" to currency,
            "accentKey" to accentKey,
            "coverPreset" to coverPreset,
            "logoUri" to logoUri?.takeIf { it.isNotBlank() },
            "active" to active,
            "verified" to false,
            "planId" to "starter",
            "subscriptionStatus" to "trial",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }

    fun toMerchantUpdateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "name" to name,
            "slug" to slug,
            "description" to description,
            "city" to city,
            "whatsappPhone" to whatsappPhone,
            "currency" to currency,
            "accentKey" to accentKey,
            "coverPreset" to coverPreset,
            "logoUri" to logoUri?.takeIf { it.isNotBlank() },
            "active" to active,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class Category(
    @DocumentId val id: String = "",
    val storeId: String = "",
    val ownerUid: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val sortOrder: Long = 0L,
    val active: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "storeId" to storeId,
        "ownerUid" to ownerUid,
        "nameAr" to nameAr,
        "nameEn" to nameEn,
        "sortOrder" to sortOrder,
        "active" to active,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "nameAr" to nameAr,
        "nameEn" to nameEn,
        "sortOrder" to sortOrder,
        "active" to active,
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val storeId: String = "",
    val ownerUid: String = "",
    val categoryId: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val descriptionAr: String = "",
    val descriptionEn: String = "",
    val price: Double = 0.0,
    val stockCount: Long = 0L,
    val available: Boolean = true,
    val imagePreset: String = "honey",
    val imageUri: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "storeId" to storeId,
            "ownerUid" to ownerUid,
            "categoryId" to categoryId,
            "nameAr" to nameAr,
            "nameEn" to nameEn,
            "descriptionAr" to descriptionAr,
            "descriptionEn" to descriptionEn,
            "price" to price,
            "stockCount" to stockCount,
            "available" to available,
            "imagePreset" to imagePreset,
            "imageUri" to imageUri?.takeIf { it.isNotBlank() },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }

    fun toUpdateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "categoryId" to categoryId,
            "nameAr" to nameAr,
            "nameEn" to nameEn,
            "descriptionAr" to descriptionAr,
            "descriptionEn" to descriptionEn,
            "price" to price,
            "stockCount" to stockCount,
            "available" to available,
            "imagePreset" to imagePreset,
            "imageUri" to imageUri?.takeIf { it.isNotBlank() },
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class OrderInquiry(
    @DocumentId val id: String = "",
    val storeId: String = "",
    val storeName: String = "",
    val storeOwnerUid: String = "",
    val customerUid: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val deliveryAddress: String = "",
    val notes: String = "",
    val itemsSummary: String = "",
    val itemCount: Long = 1L,
    val totalAmount: Double = 0.0,
    val currency: String = "YER",
    val status: String = "pending_whatsapp",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "storeId" to storeId,
        "storeName" to storeName,
        "storeOwnerUid" to storeOwnerUid,
        "customerUid" to customerUid,
        "customerName" to customerName,
        "customerPhone" to customerPhone,
        "deliveryAddress" to deliveryAddress,
        "notes" to notes,
        "itemsSummary" to itemsSummary,
        "itemCount" to itemCount,
        "totalAmount" to totalAmount,
        "currency" to currency,
        "status" to "pending_whatsapp",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

data class CartItem(
    val product: Product,
    val quantity: Int,
) {
    val lineTotal: Double get() = product.price * quantity
}
