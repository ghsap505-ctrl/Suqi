package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.SampleCatalog
import com.example.model.OrderInquiry
import com.example.model.Store
import com.example.model.SubscriptionPlan
import com.example.ui.components.StatusPill
import com.example.ui.i18n.AppLang
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.SouqiAmberContainer
import com.example.ui.theme.SouqiEmeraldContainer
import com.example.ui.theme.SouqiOnAmberContainer
import com.example.ui.theme.SouqiOnEmeraldContainer
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    lang: AppLang,
    isAdmin: Boolean,
    myStore: Store?,
    adminStoresState: UiState<List<Store>>,
    adminPlansState: UiState<List<SubscriptionPlan>>,
    adminInquiriesState: UiState<List<OrderInquiry>>,
    activePlansState: UiState<List<SubscriptionPlan>>,
    isBusy: Boolean,
    onUpdateStoreSubscription: (storeId: String, verified: Boolean, active: Boolean, planId: String, status: String) -> Unit,
    onUpsertPlan: (SubscriptionPlan) -> Unit,
    onSeedPlans: () -> Unit,
) {
    val strings = remember(lang) { AppStrings(lang) }
    val isAr = lang == AppLang.AR

    if (!isAdmin) {
        val visiblePlans = (activePlansState as? UiState.Success)?.data ?: SampleCatalog.defaultPlans
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.adminRestrictedTitle, style = MaterialTheme.typography.titleLarge)
                        }
                        Text(strings.adminRestrictedDesc, style = MaterialTheme.typography.bodyMedium)
                        if (myStore != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${if (isAr) "باقة متجرك الحالية" else "Current Store Plan"}: ${myStore.planId} (${strings.statusLabel(myStore.subscriptionStatus)})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = strings.pricingTitle,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                )
            }

            items(visiblePlans, key = { it.id }) { plan ->
                Card(modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isAr) plan.nameAr else plan.nameEn, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (plan.priceMonthlyUsd <= 0) (if (isAr) "مجاناً" else "Free") else strings.formatCurrency(plan.priceMonthlyYer, "YER"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(if (isAr) plan.descriptionAr else plan.descriptionEn, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val stores = (adminStoresState as? UiState.Success)?.data.orEmpty()
    val plans = ((adminPlansState as? UiState.Success)?.data.orEmpty()).ifEmpty { SampleCatalog.defaultPlans }
    val inquiries = (adminInquiriesState as? UiState.Success)?.data.orEmpty()

    var editingStore by remember { mutableStateOf<Store?>(null) }
    var editingPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }
    var showNewPlanDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(strings.adminStoresTab) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(strings.adminPlansTab) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text(strings.adminReportsTab) })
        }

        when (selectedTab) {
            0 -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(stores, key = { it.id }) { store ->
                    ElevatedCard(modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(store.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${store.city} • /s/${store.slug}", style = MaterialTheme.typography.bodySmall)
                                }
                                StatusPill(
                                    label = strings.statusLabel(store.subscriptionStatus),
                                    containerColor = if (store.subscriptionStatus == "active") SouqiEmeraldContainer else SouqiAmberContainer,
                                    contentColor = if (store.subscriptionStatus == "active") SouqiOnEmeraldContainer else SouqiOnAmberContainer,
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${if (isAr) "الباقة" else "Plan"}: ${store.planId}", style = MaterialTheme.typography.labelMedium)
                                if (store.verified) {
                                    Icon(Icons.Filled.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                }
                                Spacer(Modifier.weight(1f))
                                OutlinedButton(onClick = { editingStore = store }) {
                                    Text(if (isAr) "إدارة الاشتراك" else "Manage Plan")
                                }
                            }
                        }
                    }
                }
            }

            1 -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { editingPlan = null; showNewPlanDialog = true },
                            modifier = Modifier.weight(1f).testTag("admin_add_plan_btn"),
                        ) {
                            Text(if (isAr) "باقة جديدة" else "New Plan")
                        }
                        OutlinedButton(
                            onClick = onSeedPlans,
                            enabled = !isBusy,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.seedDataBtn)
                        }
                    }
                }

                items(plans, key = { it.id }) { plan ->
                    Card(modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if (isAr) plan.nameAr else plan.nameEn, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${strings.formatCurrency(plan.priceMonthlyYer, "YER")} / ${strings.formatCurrency(plan.priceMonthlyUsd, "USD")} • ${plan.maxProducts} ${if (isAr) "منتج" else "products"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { editingPlan = plan; showNewPlanDialog = true }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit Plan")
                            }
                        }
                    }
                }
            }

            2 -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    val activeCount = stores.count { it.active }
                    val verifiedCount = stores.count { it.verified }
                    val paidSubCount = stores.count { it.subscriptionStatus == "active" }
                    Column(
                        modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(if (isAr) "تقرير المنصة المباشر" else "Live Platform Report", style = MaterialTheme.typography.titleLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ReportCard(if (isAr) "إجمالي المتاجر" else "Total Stores", stores.size.toString(), Modifier.weight(1f))
                            ReportCard(if (isAr) "المتاجر النشطة" else "Active Stores", activeCount.toString(), Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ReportCard(if (isAr) "الموثقة" else "Verified", verifiedCount.toString(), Modifier.weight(1f))
                            ReportCard(if (isAr) "اشتراكات مفعلة" else "Active Subs", paidSubCount.toString(), Modifier.weight(1f))
                        }
                        ReportCard(
                            title = if (isAr) "إجمالي طلبات واتساب المسجلة" else "Recorded WhatsApp Inquiries",
                            value = inquiries.size.toString(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    val targetStore = editingStore
    if (targetStore != null) {
        StoreSubscriptionDialog(
            lang = lang,
            store = targetStore,
            plans = plans,
            onDismiss = { editingStore = null },
            onSave = { verified, active, planId, status ->
                onUpdateStoreSubscription(targetStore.id, verified, active, planId, status)
                editingStore = null
            },
        )
    }

    if (showNewPlanDialog) {
        PlanEditDialog(
            lang = lang,
            initial = editingPlan,
            onDismiss = { showNewPlanDialog = false },
            onSave = { plan ->
                onUpsertPlan(plan)
                showNewPlanDialog = false
            },
        )
    }
}

@Composable
private fun ReportCard(title: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StoreSubscriptionDialog(
    lang: AppLang,
    store: Store,
    plans: List<SubscriptionPlan>,
    onDismiss: () -> Unit,
    onSave: (verified: Boolean, active: Boolean, planId: String, status: String) -> Unit,
) {
    val isAr = lang == AppLang.AR
    var verified by remember(store) { mutableStateOf(store.verified) }
    var active by remember(store) { mutableStateOf(store.active) }
    var planId by remember(store) { mutableStateOf(store.planId) }
    var subStatus by remember(store) { mutableStateOf(store.subscriptionStatus) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(store.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isAr) "توثيق المتجر" else "Verified Store")
                    Switch(checked = verified, onCheckedChange = { verified = it })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isAr) "المتجر نشط" else "Store Active")
                    Switch(checked = active, onCheckedChange = { active = it })
                }
                Text(if (isAr) "الباقة" else "Plan", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    plans.forEach { p ->
                        FilterChip(
                            selected = planId == p.id,
                            onClick = { planId = p.id },
                            label = { Text(if (isAr) p.nameAr else p.nameEn) },
                        )
                    }
                }
                Text(if (isAr) "حالة الاشتراك" else "Subscription Status", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("trial", "active", "pending", "suspended").forEach { st ->
                        FilterChip(
                            selected = subStatus == st,
                            onClick = { subStatus = st },
                            label = { Text(st) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(verified, active, planId, subStatus) }) {
                Text(if (isAr) "حفظ" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isAr) "إلغاء" else "Cancel") }
        },
    )
}

@Composable
private fun PlanEditDialog(
    lang: AppLang,
    initial: SubscriptionPlan?,
    onDismiss: () -> Unit,
    onSave: (SubscriptionPlan) -> Unit,
) {
    val isAr = lang == AppLang.AR
    var nameAr by remember(initial) { mutableStateOf(initial?.nameAr ?: "") }
    var nameEn by remember(initial) { mutableStateOf(initial?.nameEn ?: "") }
    var priceYer by remember(initial) { mutableStateOf((initial?.priceMonthlyYer ?: 0.0).toString()) }
    var priceUsd by remember(initial) { mutableStateOf((initial?.priceMonthlyUsd ?: 0.0).toString()) }
    var maxProducts by remember(initial) { mutableStateOf((initial?.maxProducts ?: 50L).toString()) }
    var descAr by remember(initial) { mutableStateOf(initial?.descriptionAr ?: "") }
    var descEn by remember(initial) { mutableStateOf(initial?.descriptionEn ?: "") }
    var active by remember(initial) { mutableStateOf(initial?.active ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) (if (isAr) "إضافة باقة" else "New Plan") else (if (isAr) "تعديل الباقة" else "Edit Plan")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nameAr, onValueChange = { nameAr = it }, label = { Text("الاسم (عربي)") }, singleLine = true)
                OutlinedTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text("Name (English)") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = priceYer, onValueChange = { priceYer = it }, label = { Text("YER/mo") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = priceUsd, onValueChange = { priceUsd = it }, label = { Text("USD/mo") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = maxProducts, onValueChange = { maxProducts = it.filter { c -> c.isDigit() } }, label = { Text("Max Products") }, singleLine = true)
                OutlinedTextField(value = descAr, onValueChange = { descAr = it }, label = { Text("الوصف (عربي)") })
                OutlinedTextField(value = descEn, onValueChange = { descEn = it }, label = { Text("Description (EN)") })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isAr) "باقة مفعلة" else "Active")
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameAr.trim().length >= 2 && descAr.trim().length >= 2) {
                        onSave(
                            SubscriptionPlan(
                                id = initial?.id.orEmpty(),
                                nameAr = nameAr.trim(),
                                nameEn = nameEn.trim().ifBlank { nameAr.trim() },
                                priceMonthlyYer = priceYer.toDoubleOrNull() ?: 0.0,
                                priceMonthlyUsd = priceUsd.toDoubleOrNull() ?: 0.0,
                                maxProducts = (maxProducts.toLongOrNull() ?: 50L).coerceIn(1L, 10000L),
                                descriptionAr = descAr.trim(),
                                descriptionEn = descEn.trim().ifBlank { descAr.trim() },
                                active = active,
                                sortOrder = initial?.sortOrder ?: 10L,
                            )
                        )
                    }
                }
            ) {
                Text(if (isAr) "حفظ" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isAr) "إلغاء" else "Cancel") }
        },
    )
}
