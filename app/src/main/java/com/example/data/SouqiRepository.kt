package com.example.data

import android.content.Context
import com.example.R
import com.example.domain.SampleCatalog
import com.example.model.Category
import com.example.model.OrderInquiry
import com.example.model.Product
import com.example.model.Store
import com.example.model.SubscriptionPlan
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SouqiRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.guardFirestoreErrors(
        uid: String,
        operationType: OperationType,
        path: String,
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    suspend fun ensureUserProfile(preferredLang: String = "ar"): Result<UserProfile> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("Not authenticated"))
        val uid = currentUser.uid
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (snap.exists()) {
                val profile = snap.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfile(
                    id = uid,
                    userId = uid,
                    displayName = currentUser.displayName ?: "تاجر سوقي",
                    email = currentUser.email ?: "",
                    role = "merchant",
                    preferredLang = preferredLang,
                )
                Result.success(profile)
            } else {
                val newProfile = UserProfile(
                    id = uid,
                    userId = uid,
                    displayName = (currentUser.displayName ?: "تاجر سوقي").take(80),
                    email = (currentUser.email ?: "merchant@souqi.app").take(120),
                    role = "merchant",
                    preferredLang = preferredLang,
                )
                docRef.set(newProfile.toCreateMap()).await()
                Result.success(newProfile)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
        require(uid.isNotBlank()) { "UID must not be blank" }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .guardFirestoreErrors(uid, OperationType.GET, path)
        )
    }

    fun observeActiveStores(): Flow<List<Store>> = flow {
        val uid = requireUserId()
        val path = "stores"
        emitAll(
            db.collection("stores")
                .whereEqualTo("active", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Store::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .guardFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeMyStores(uid: String): Flow<List<Store>> = flow {
        require(uid.isNotBlank()) { "UID must not be blank" }
        val path = "stores"
        emitAll(
            db.collection("stores")
                .whereEqualTo("ownerUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Store::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .guardFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeAllStoresForAdmin(): Flow<List<Store>> = flow {
        val uid = requireUserId()
        val path = "stores"
        emitAll(
            db.collection("stores")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Store::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .guardFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeStoreCategories(
        storeId: String,
        storeOwnerUid: String,
        isAdmin: Boolean,
    ): Flow<List<Category>> = flow {
        val uid = requireUserId()
        val path = "stores/$storeId/categories"
        val baseRef = db.collection("stores").document(storeId).collection("categories")
        val query = if (isAdmin || uid == storeOwnerUid) {
            baseRef.whereEqualTo("ownerUid", storeOwnerUid)
        } else {
            baseRef.whereEqualTo("active", true)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(Category::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.sortOrder }
                }
                .guardFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeStoreProducts(
        storeId: String,
        storeOwnerUid: String,
        isAdmin: Boolean,
    ): Flow<List<Product>> = flow {
        val uid = requireUserId()
        val path = "stores/$storeId/products"
        val baseRef = db.collection("stores").document(storeId).collection("products")
        val query = if (isAdmin || uid == storeOwnerUid) {
            baseRef.whereEqualTo("ownerUid", storeOwnerUid)
        } else {
            baseRef.whereEqualTo("available", true)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .guardFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeActivePlans(): Flow<List<SubscriptionPlan>> = flow {
        val uid = requireUserId()
        val path = "plans"
        emitAll(
            db.collection("plans")
                .whereEqualTo("active", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SubscriptionPlan::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.sortOrder }
                }
                .guardFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeAllPlansForAdmin(): Flow<List<SubscriptionPlan>> = flow {
        val uid = requireUserId()
        val path = "plans"
        emitAll(
            db.collection("plans")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SubscriptionPlan::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.sortOrder }
                }
                .guardFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeMerchantInquiries(uid: String): Flow<List<OrderInquiry>> = flow {
        require(uid.isNotBlank()) { "UID must not be blank" }
        val path = "order_inquiries"
        emitAll(
            db.collection("order_inquiries")
                .whereEqualTo("storeOwnerUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(OrderInquiry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .guardFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeAllInquiriesForAdmin(): Flow<List<OrderInquiry>> = flow {
        val uid = requireUserId()
        val path = "order_inquiries"
        emitAll(
            db.collection("order_inquiries")
                .snapshots()
                .map { snap ->
                    snap.toObjects(OrderInquiry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .guardFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    suspend fun createStore(store: Store): Result<String> {
        var docPath = "stores"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("stores").document()
            docPath = docRef.path
            val payload = store.copy(ownerUid = uid).toCreateMap()
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateStore(store: Store): Result<Unit> {
        val docPath = "stores/${store.id}"
        return try {
            requireUserId()
            require(store.id.isNotBlank()) { "Store ID must not be blank" }
            db.collection("stores").document(store.id)
                .update(store.toMerchantUpdateMap())
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertCategory(category: Category): Result<String> {
        val isCreate = category.id.isBlank()
        var docPath = "stores/${category.storeId}/categories"
        return try {
            val uid = requireUserId()
            require(category.storeId.isNotBlank()) { "Store ID required" }
            val colRef = db.collection("stores").document(category.storeId).collection("categories")
            if (isCreate) {
                val docRef = colRef.document()
                docPath = docRef.path
                docRef.set(category.copy(ownerUid = uid).toCreateMap()).await()
                Result.success(docRef.id)
            } else {
                val docRef = colRef.document(category.id)
                docPath = docRef.path
                docRef.update(category.toUpdateMap()).await()
                Result.success(category.id)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(storeId: String, categoryId: String): Result<Unit> {
        val docPath = "stores/$storeId/categories/$categoryId"
        return try {
            requireUserId()
            db.collection("stores").document(storeId)
                .collection("categories").document(categoryId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertProduct(product: Product): Result<String> {
        val isCreate = product.id.isBlank()
        var docPath = "stores/${product.storeId}/products"
        return try {
            val uid = requireUserId()
            require(product.storeId.isNotBlank()) { "Store ID required" }
            val colRef = db.collection("stores").document(product.storeId).collection("products")
            if (isCreate) {
                val docRef = colRef.document()
                docPath = docRef.path
                docRef.set(product.copy(ownerUid = uid).toCreateMap()).await()
                Result.success(docRef.id)
            } else {
                val docRef = colRef.document(product.id)
                docPath = docRef.path
                docRef.update(product.toUpdateMap()).await()
                Result.success(product.id)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(storeId: String, productId: String): Result<Unit> {
        val docPath = "stores/$storeId/products/$productId"
        return try {
            requireUserId()
            db.collection("stores").document(storeId)
                .collection("products").document(productId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun submitOrderInquiry(inquiry: OrderInquiry): Result<String> {
        var docPath = "order_inquiries"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("order_inquiries").document()
            docPath = docRef.path
            docRef.set(inquiry.copy(customerUid = uid).toCreateMap()).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateInquiryStatus(inquiryId: String, newStatus: String): Result<Unit> {
        val docPath = "order_inquiries/$inquiryId"
        return try {
            requireUserId()
            require(newStatus in setOf("pending_whatsapp", "contacted", "confirmed", "cancelled"))
            db.collection("order_inquiries").document(inquiryId)
                .update(
                    mapOf(
                        "status" to newStatus,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateStoreSubscription(
        storeId: String,
        verified: Boolean,
        active: Boolean,
        planId: String,
        subscriptionStatus: String,
    ): Result<Unit> {
        val docPath = "stores/$storeId"
        return try {
            requireUserId()
            db.collection("stores").document(storeId)
                .update(
                    mapOf(
                        "verified" to verified,
                        "active" to active,
                        "planId" to planId,
                        "subscriptionStatus" to subscriptionStatus,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun adminUpsertPlan(plan: SubscriptionPlan): Result<String> {
        val planId = plan.id.ifBlank {
            db.collection("plans").document().id
        }
        val docPath = "plans/$planId"
        return try {
            requireUserId()
            val docRef = db.collection("plans").document(planId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(plan.toUpdateMap()).await()
            } else {
                docRef.set(plan.toCreateMap()).await()
            }
            Result.success(planId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun seedDefaultPlans(): Result<Unit> {
        return try {
            requireUserId()
            SampleCatalog.defaultPlans.forEach { plan ->
                val docRef = db.collection("plans").document(plan.id)
                if (!docRef.get().await().exists()) {
                    docRef.set(plan.toCreateMap()).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "plans")
            Result.failure(e)
        }
    }
}
