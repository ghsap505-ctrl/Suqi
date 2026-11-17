package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.SampleCatalog
import com.example.model.Store
import com.example.ui.components.StatusPill
import com.example.ui.components.StoreCoverImage
import com.example.ui.i18n.AppLang
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.SouqiAmberContainer
import com.example.ui.theme.SouqiEmerald
import com.example.ui.theme.SouqiEmeraldContainer
import com.example.ui.theme.SouqiOnAmberContainer
import com.example.ui.theme.SouqiOnEmeraldContainer
import com.example.ui.viewmodel.UiState

@Composable
fun ExploreScreen(
    lang: AppLang,
    cloudStoresState: UiState<List<Store>>,
    onOpenStore: (Store) -> Unit,
    onCreateStoreClick: () -> Unit,
) {
    val strings = remember(lang) { AppStrings(lang) }
    val isAr = lang == AppLang.AR
    var searchQuery by remember { mutableStateOf("") }

    val filteredDemos = remember(searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) SampleCatalog.demoStores
        else SampleCatalog.demoStores.filter {
            it.name.lowercase().contains(q) || it.city.lowercase().contains(q) || it.description.lowercase().contains(q)
        }
    }

    val cloudStores = (cloudStoresState as? UiState.Success)?.data.orEmpty()
    val filteredCloud = remember(cloudStores, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) cloudStores
        else cloudStores.filter {
            it.name.lowercase().contains(q) || it.city.lowercase().contains(q) || it.slug.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isAr) "افتح متجرك على سوقي مجاناً" else "Launch your Souqi store free",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (isAr) "رابط مستقل + كتالوج منتجات + طلبات واتساب منظمة" else "Custom slug + product catalog + formatted WhatsApp orders",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Button(
                            onClick = onCreateStoreClick,
                            modifier = Modifier.testTag("explore_create_store_btn"),
                        ) {
                            Icon(Icons.Filled.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.createYourStoreCta)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchStoresHint) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("explore_search_input"),
                )
            }
        }

        // Cloud Stores Section
        item {
            Column(modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth()) {
                Text(
                    text = strings.liveStoresHeader,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }

        when (cloudStoresState) {
            is UiState.Loading -> {
                item {
                    Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Text(
                        text = cloudStoresState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            is UiState.Success -> {
                if (filteredCloud.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        ) {
                            Text(
                                text = if (isAr) "لا توجد متاجر سحابية منشورة بعد — يمكنك إنشاء متجرك الآن من تبويب التاجر أو استعراض المتاجر التجريبية أدناه." else "No live cloud stores yet — create yours from the Merchant tab or explore the interactive demo stores below.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                } else {
                    items(filteredCloud, key = { it.id }) { store ->
                        StoreMarketplaceCard(
                            store = store,
                            strings = strings,
                            isAr = isAr,
                            onClick = { onOpenStore(store) },
                        )
                    }
                }
            }
        }

        // Interactive Demo Stores Section
        item {
            Column(modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth()) {
                Text(
                    text = strings.demoStoresHeader,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }

        if (filteredDemos.isEmpty()) {
            item {
                Text(strings.noStoresFound, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(filteredDemos, key = { it.id }) { store ->
                StoreMarketplaceCard(
                    store = store,
                    strings = strings,
                    isAr = isAr,
                    onClick = { onOpenStore(store) },
                )
            }
        }
    }
}

@Composable
private fun StoreMarketplaceCard(
    store: Store,
    strings: AppStrings,
    isAr: Boolean,
    onClick: () -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .widthIn(max = 760.dp)
            .fillMaxWidth()
            .testTag("store_card_${store.id}"),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp),
            ) {
                StoreCoverImage(
                    coverPreset = store.coverPreset,
                    logoUri = store.logoUri,
                    storeName = store.name,
                    modifier = Modifier.fillMaxSize(),
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (store.demoMode) {
                        StatusPill(
                            label = strings.demoBadge,
                            containerColor = SouqiAmberContainer,
                            contentColor = SouqiOnAmberContainer,
                        )
                    }
                    if (store.verified) {
                        StatusPill(
                            label = strings.verifiedBadge,
                            containerColor = SouqiEmeraldContainer,
                            contentColor = SouqiOnEmeraldContainer,
                            icon = Icons.Filled.Verified,
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp),
                ) {
                    Text(
                        text = store.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${store.city} • /s/${store.slug}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = store.description.ifBlank { if (isAr) "متجر إلكتروني على منصة سوقي" else "Souqi storefront" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                FilledTonalButton(onClick = onClick) {
                    Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.openStoreBtn)
                }
            }
        }
    }
}
