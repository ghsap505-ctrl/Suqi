package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AuthRepository
import com.example.data.SouqiRepository
import com.example.data.authStateFlow
import com.example.ui.i18n.AppLang
import com.example.ui.i18n.AppStrings
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthLandingScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.MerchantDashboardScreen
import com.example.ui.screens.StorefrontAndCartScreen
import com.example.ui.theme.SouqiTheme
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.SouqiViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SouqiTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var guestLang by remember { mutableStateOf(AppLang.AR) }
    val user = currentUser

    if (user == null) {
        CompositionLocalProvider(LocalLayoutDirection provides guestLang.layoutDirection) {
            AuthLandingScreen(
                lang = guestLang,
                onToggleLang = { guestLang = guestLang.toggle() },
                onAuthSuccess = {},
            )
        }
    } else {
        key(user.uid) {
            AuthenticatedSouqiApp(user = user)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedSouqiApp(user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: SouqiViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                SouqiViewModel(
                    repository = SouqiRepository(db),
                    currentUserId = user.uid,
                    currentUserEmail = user.email.orEmpty(),
                    isEmailVerified = user.isEmailVerified,
                    currentUserDisplayName = user.displayName.orEmpty(),
                )
            }
        },
    )

    val lang by viewModel.lang.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val cloudStoresState by viewModel.cloudStoresState.collectAsStateWithLifecycle()
    val myStoresState by viewModel.myStoresState.collectAsStateWithLifecycle()
    val myPrimaryStore by viewModel.myPrimaryStore.collectAsStateWithLifecycle()
    val selectedStore by viewModel.selectedStore.collectAsStateWithLifecycle()
    val categoriesState by viewModel.selectedStoreCategories.collectAsStateWithLifecycle()
    val productsState by viewModel.selectedStoreProducts.collectAsStateWithLifecycle()
    val myCategoriesState by viewModel.myStoreCategoriesState.collectAsStateWithLifecycle()
    val myProductsState by viewModel.myStoreProductsState.collectAsStateWithLifecycle()
    val merchantInquiriesState by viewModel.merchantInquiriesState.collectAsStateWithLifecycle()
    val adminStoresState by viewModel.adminStoresState.collectAsStateWithLifecycle()
    val adminPlansState by viewModel.adminPlansState.collectAsStateWithLifecycle()
    val adminInquiriesState by viewModel.adminInquiriesState.collectAsStateWithLifecycle()
    val plansState by viewModel.plansState.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val strings = remember(lang) { AppStrings(lang) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (activeTab != MainTab.EXPLORE) {
        BackHandler { viewModel.selectTab(MainTab.EXPLORE) }
    }

    CompositionLocalProvider(LocalLayoutDirection provides lang.layoutDirection) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 680.dp

            val navItems = listOf(
                Triple(MainTab.EXPLORE, strings.navExplore, Icons.Filled.Explore to Icons.Outlined.Explore),
                Triple(MainTab.STOREFRONT, strings.navStorefront, Icons.Filled.Storefront to Icons.Outlined.Storefront),
                Triple(MainTab.MERCHANT, strings.navMerchant, Icons.Filled.Dashboard to Icons.Outlined.Dashboard),
                Triple(MainTab.ADMIN, strings.navAdmin, Icons.Filled.AdminPanelSettings to Icons.Outlined.AdminPanelSettings),
            )

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = strings.appTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                        navigationIcon = {
                            TextButton(
                                onClick = { viewModel.toggleLanguage() },
                                modifier = Modifier.testTag("main_lang_toggle"),
                            ) {
                                Icon(Icons.Filled.Language, contentDescription = strings.toggleLangLabel)
                                Spacer(Modifier.width(4.dp))
                                Text(strings.toggleLangLabel)
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    AuthRepository.signOut(
                                        credentialManager = credentialManager,
                                        scope = scope,
                                    )
                                },
                                modifier = Modifier.testTag("sign_out_btn"),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = strings.signOut,
                                )
                            }
                        },
                    )
                },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar {
                            navItems.forEach { (tab, label, icons) ->
                                val selected = activeTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = { Icon(if (selected) icons.first else icons.second, contentDescription = label) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    if (useRail) {
                        NavigationRail {
                            navItems.forEach { (tab, label, icons) ->
                                val selected = activeTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = { Icon(if (selected) icons.first else icons.second, contentDescription = label) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("rail_${tab.name.lowercase()}"),
                                )
                            }
                        }
                    }

                    when (activeTab) {
                        MainTab.EXPLORE -> ExploreScreen(
                            lang = lang,
                            cloudStoresState = cloudStoresState,
                            onOpenStore = { store -> viewModel.openStorefront(store) },
                            onCreateStoreClick = { viewModel.selectTab(MainTab.MERCHANT) },
                        )
                        MainTab.STOREFRONT -> StorefrontAndCartScreen(
                            lang = lang,
                            store = selectedStore,
                            categoriesState = categoriesState,
                            productsState = productsState,
                            cartItems = cartItems,
                            isBusy = isBusy,
                            onAddToCart = { viewModel.addToCart(it) },
                            onUpdateCartQuantity = { id, qty -> viewModel.updateCartQuantity(id, qty) },
                            onClearCart = { viewModel.clearCart() },
                            onSubmitWhatsAppOrder = { name, phone, addr, notes, onReady ->
                                viewModel.submitOrderInquiry(name, phone, addr, notes, onReady)
                            },
                            onNotify = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            },
                        )
                        MainTab.MERCHANT -> MerchantDashboardScreen(
                            lang = lang,
                            myStoresState = myStoresState,
                            categoriesState = myCategoriesState,
                            productsState = myProductsState,
                            inquiriesState = merchantInquiriesState,
                            isBusy = isBusy,
                            onSaveStore = { id, name, slug, city, wa, desc, curr, accent, cover, logo, active ->
                                viewModel.saveMerchantStore(id, name, slug, city, wa, desc, curr, accent, cover, logo, active)
                            },
                            onSaveCategory = { storeId, catId, nameAr, nameEn, sort, active ->
                                viewModel.saveCategory(storeId, catId, nameAr, nameEn, sort, active)
                            },
                            onDeleteCategory = { storeId, catId ->
                                viewModel.deleteCategory(storeId, catId)
                            },
                            onSaveProduct = { storeId, prodId, catId, nameAr, nameEn, descAr, price, stock, avail, preset, uri ->
                                viewModel.saveProduct(storeId, prodId, catId, nameAr, nameEn, descAr, price, stock, avail, preset, uri)
                            },
                            onDeleteProduct = { storeId, prodId ->
                                viewModel.deleteProduct(storeId, prodId)
                            },
                            onUpdateInquiryStatus = { inq, status ->
                                viewModel.updateInquiryStatus(inq, status)
                            },
                            onOpenStorefront = { store -> viewModel.openStorefront(store) },
                        )
                        MainTab.ADMIN -> AdminDashboardScreen(
                            lang = lang,
                            isAdmin = isAdmin,
                            myStore = myPrimaryStore,
                            adminStoresState = adminStoresState,
                            adminPlansState = adminPlansState,
                            adminInquiriesState = adminInquiriesState,
                            activePlansState = plansState,
                            isBusy = isBusy,
                            onUpdateStoreSubscription = { id, verified, active, planId, status ->
                                viewModel.adminUpdateStoreSubscription(id, verified, active, planId, status)
                            },
                            onUpsertPlan = { plan -> viewModel.adminUpsertPlan(plan) },
                            onSeedPlans = { viewModel.seedDefaultPlansToCloud() },
                        )
                    }
                }
            }
        }
    }
}
