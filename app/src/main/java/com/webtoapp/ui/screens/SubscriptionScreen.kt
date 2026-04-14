package com.webtoapp.ui.screens

import android.app.Activity
import com.webtoapp.ui.components.PremiumButton
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.core.billing.*
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.viewmodel.AuthState
import com.webtoapp.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.components.EnhancedElevatedCard

/** Safely find the Activity from a Context, traversing ContextWrapper chain. */
private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * usercurrent
 */
private enum class UserTier {
    FREE, PRO_MONTHLY, PRO_QUARTERLY, PRO_YEARLY, PRO_LIFETIME, ULTRA_MONTHLY, ULTRA_QUARTERLY, ULTRA_YEARLY, ULTRA_LIFETIME
}

private fun UserTier.isPro() = this in listOf(UserTier.PRO_MONTHLY, UserTier.PRO_QUARTERLY, UserTier.PRO_YEARLY, UserTier.PRO_LIFETIME)
private fun UserTier.isUltra() = this in listOf(UserTier.ULTRA_MONTHLY, UserTier.ULTRA_QUARTERLY, UserTier.ULTRA_YEARLY, UserTier.ULTRA_LIFETIME)
private fun UserTier.isLifetime() = this == UserTier.PRO_LIFETIME || this == UserTier.ULTRA_LIFETIME

/**
 * Note
 *
 * Free, Pro, Ultra.
 * support, switch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    billingManager: BillingManager,
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val isConnected by billingManager.isConnected.collectAsStateWithLifecycle()
    val products by billingManager.products.collectAsStateWithLifecycle()
    val purchaseState by billingManager.purchaseState.collectAsStateWithLifecycle()
    val currentSub by billingManager.currentSubscription.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    // ensure Billing( if Application initializefailed)
    LaunchedEffect(Unit) {
        if (!isConnected) {
            billingManager.connect()
        }
    }

    // fromuser current
    val userTier = remember(authState) {
        when (val state = authState) {
            is AuthState.LoggedIn -> {
                val user = state.user
                if (!user.isPro) UserTier.FREE
                else when (user.proPlan) {
                    "pro_monthly" -> UserTier.PRO_MONTHLY
                    "pro_quarterly" -> UserTier.PRO_QUARTERLY
                    "pro_yearly" -> UserTier.PRO_YEARLY
                    "pro_lifetime", "lifetime" -> UserTier.PRO_LIFETIME
                    "ultra_monthly" -> UserTier.ULTRA_MONTHLY
                    "ultra_quarterly" -> UserTier.ULTRA_QUARTERLY
                    "ultra_yearly" -> UserTier.ULTRA_YEARLY
                    "ultra_lifetime" -> UserTier.ULTRA_LIFETIME
                    else -> UserTier.FREE
                }
            }
            else -> UserTier.FREE
        }
    }

    // 0=, 1=, 2=, 3=
    var selectedPeriod by remember { mutableIntStateOf(
        when {
            userTier.isLifetime() -> 3
            userTier == UserTier.PRO_YEARLY || userTier == UserTier.ULTRA_YEARLY -> 2
            userTier == UserTier.PRO_QUARTERLY || userTier == UserTier.ULTRA_QUARTERLY -> 1
            userTier == UserTier.PRO_MONTHLY || userTier == UserTier.ULTRA_MONTHLY -> 0
            else -> 2
        }
    ) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // handle
    LaunchedEffect(purchaseState) {
        when (val state = purchaseState) {
            is PurchaseState.Success -> {
                snackbarHostState.showSnackbar(Strings.subscriptionSuccess.format(state.plan.displayName))
                billingManager.resetPurchaseState()
            }
            is PurchaseState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                billingManager.resetPurchaseState()
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(Strings.selectPlan) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, Strings.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        LazyColumn(
            modifier = Modifier,
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Note
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        Strings.unlockAllFeatures,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        Strings.chooseYourPlan,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // switch: / /
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            PeriodChip(
                                text = Strings.periodMonthly,
                                selected = selectedPeriod == 0,
                                onClick = { selectedPeriod = 0 }
                            )
                            PeriodChip(
                                text = Strings.periodQuarterly,
                                selected = selectedPeriod == 1,
                                onClick = { selectedPeriod = 1 }
                            )
                            PeriodChip(
                                text = Strings.periodYearly,
                                selected = selectedPeriod == 2,
                                onClick = { selectedPeriod = 2 }
                            )
                            PeriodChip(
                                text = Strings.periodLifetime,
                                selected = selectedPeriod == 3,
                                onClick = { selectedPeriod = 3 }
                            )
                        }
                    }
                }
            }

            // currentuserstatehint( Free)
            if (userTier != UserTier.FREE) {
                item {
                    val planDisplayName = when (userTier) {
                        UserTier.PRO_MONTHLY -> Strings.proMonthly
                        UserTier.PRO_QUARTERLY -> Strings.proQuarterly
                        UserTier.PRO_YEARLY -> Strings.proYearly
                        UserTier.PRO_LIFETIME -> Strings.proLifetime
                        UserTier.ULTRA_MONTHLY -> Strings.ultraMonthly
                        UserTier.ULTRA_QUARTERLY -> Strings.ultraQuarterly
                        UserTier.ULTRA_YEARLY -> Strings.ultraYearly
                        UserTier.ULTRA_LIFETIME -> Strings.ultraLifetime
                        else -> Strings.tierFree
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (userTier.isUltra())
                            Color(0xFFFFD700).copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle, null,
                                tint = if (userTier.isUltra()) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    Strings.currentPlan.format(planDisplayName),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (userTier.isLifetime()) {
                                    Text(
                                        Strings.validForever,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ═══ Free ═══
            item {
                FreeCard(isCurrent = userTier == UserTier.FREE)
            }

            // ═══ Pro ═══
            item {
                val isCurrentPro = when (selectedPeriod) {
                    0 -> userTier == UserTier.PRO_MONTHLY
                    1 -> userTier == UserTier.PRO_QUARTERLY
                    2 -> userTier == UserTier.PRO_YEARLY
                    3 -> userTier == UserTier.PRO_LIFETIME
                    else -> false
                }
                // ifuser Ultra, Pro display" "
                val isDowngrade = userTier.isUltra()

                when (selectedPeriod) {
                    0 -> {
                        val plan = SubscriptionPlan.PRO_MONTHLY
                        val price = billingManager.getFormattedPrice(plan) ?: "$3"
                        SubscriptionCard(
                            tierName = Strings.tierPro,
                            price = price,
                            period = Strings.perMonth,
                            gradient = listOf(Color(0xFF42A5F5), Color(0xFF1E88E5)),
                            features = proFeatures(),
                            isCurrent = isCurrentPro,
                            isDowngrade = isDowngrade,
                            isLoading = purchaseState is PurchaseState.Loading,
                            onSubscribe = { if (!isCurrentPro && !isDowngrade && activity != null) billingManager.launchPurchase(activity, plan) }
                        )
                    }
                    1 -> {
                        val plan = SubscriptionPlan.PRO_QUARTERLY
                        val price = billingManager.getFormattedPrice(plan) ?: "$8.10"
                        SubscriptionCard(
                            tierName = Strings.tierPro,
                            price = price,
                            period = Strings.perQuarter,
                            gradient = listOf(Color(0xFF42A5F5), Color(0xFF1E88E5)),
                            features = proFeatures(),
                            isCurrent = isCurrentPro,
                            isDowngrade = isDowngrade,
                            isLoading = purchaseState is PurchaseState.Loading,
                            onSubscribe = { if (!isCurrentPro && !isDowngrade && activity != null) billingManager.launchPurchase(activity, plan) }
                        )
                    }
                    2 -> {
                        val plan = SubscriptionPlan.PRO_YEARLY
                        val price = billingManager.getFormattedPrice(plan) ?: "$28.80"
                        SubscriptionCard(
                            tierName = Strings.tierPro,
                            price = price,
                            period = Strings.perYear,
                            gradient = listOf(Color(0xFF42A5F5), Color(0xFF1E88E5)),
                            features = proFeatures(),
                            isCurrent = isCurrentPro,
                            isDowngrade = isDowngrade,
                            isLoading = purchaseState is PurchaseState.Loading,
                            onSubscribe = { if (!isCurrentPro && !isDowngrade && activity != null) billingManager.launchPurchase(activity, plan) }
                        )
                    }
                    3 -> {
                        SubscriptionCard(
                            tierName = Strings.tierPro,
                            price = "$99",
                            period = Strings.oneTime,
                            gradient = listOf(Color(0xFF42A5F5), Color(0xFF1E88E5)),
                            features = proFeatures() + listOf(
                                FeatureItem(Icons.Outlined.AllInclusive, Strings.neverExpires, Strings.oneTimePurchaseLifetime)
                            ),
                            isCurrent = isCurrentPro,
                            isDowngrade = isDowngrade,
                            isLoading = purchaseState is PurchaseState.Loading,
                            isLifetime = true,
                            onSubscribe = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(Strings.redeemWithActivationCode)
                                }
                            }
                        )
                    }
                }
            }

            // ═══ Ultra ═══
            item {
                val isCurrentUltra = when (selectedPeriod) {
                    0 -> userTier == UserTier.ULTRA_MONTHLY
                    1 -> userTier == UserTier.ULTRA_QUARTERLY
                    2 -> userTier == UserTier.ULTRA_YEARLY
                    3 -> userTier == UserTier.ULTRA_LIFETIME
                    else -> false
                }

                when (selectedPeriod) {
                    0 -> {
                        val plan = SubscriptionPlan.ULTRA_MONTHLY
                        val price = billingManager.getFormattedPrice(plan) ?: "$9"
                        SubscriptionCard(
                            tierName = Strings.tierUltra,
                            price = price,
                            period = Strings.perMonth,
                            gradient = listOf(Color(0xFFF093FB), Color(0xFFF5576C)),
                            isRecommended = true,
                            features = ultraFeatures(),
                            isCurrent = isCurrentUltra,
                            isLoading = purchaseState is PurchaseState.Loading,
                            onSubscribe = { if (!isCurrentUltra && activity != null) billingManager.launchPurchase(activity, plan) }
                        )
                    }
                    1 -> {
                        val plan = SubscriptionPlan.ULTRA_QUARTERLY
                        val price = billingManager.getFormattedPrice(plan) ?: "$24.30"
                        SubscriptionCard(
                            tierName = Strings.tierUltra,
                            price = price,
                            period = Strings.perQuarter,
                            gradient = listOf(Color(0xFFF093FB), Color(0xFFF5576C)),
                            isRecommended = true,
                            features = ultraFeatures(),
                            isCurrent = isCurrentUltra,
                            isLoading = purchaseState is PurchaseState.Loading,
                            onSubscribe = { if (!isCurrentUltra && activity != null) billingManager.launchPurchase(activity, plan) }
                        )
                    }
                    2 -> {
                        val plan = SubscriptionPlan.ULTRA_YEARLY
                        val price = billingManager.getFormattedPrice(plan) ?: "$86.40"
                        SubscriptionCard(
                            tierName = Strings.tierUltra,
                            price = price,
                            period = Strings.perYear,
                            gradient = listOf(Color(0xFFF093FB), Color(0xFFF5576C)),
                            isRecommended = true,
                            features = ultraFeatures(),
                            isCurrent = isCurrentUltra,
                            isLoading = purchaseState is PurchaseState.Loading,
                            onSubscribe = { if (!isCurrentUltra && activity != null) billingManager.launchPurchase(activity, plan) }
                        )
                    }
                    3 -> {
                        SubscriptionCard(
                            tierName = Strings.tierUltra,
                            price = "$199",
                            period = Strings.oneTime,
                            gradient = listOf(Color(0xFFF093FB), Color(0xFFF5576C)),
                            isRecommended = true,
                            features = ultraFeatures() + listOf(
                                FeatureItem(Icons.Outlined.AllInclusive, Strings.neverExpires, Strings.oneTimePurchaseLifetime)
                            ),
                            isCurrent = isCurrentUltra,
                            isLoading = purchaseState is PurchaseState.Loading,
                            isLifetime = true,
                            upgradeNote = if (userTier == UserTier.PRO_LIFETIME) Strings.proUpgradeNote else null,
                            onSubscribe = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(Strings.redeemWithActivationCode)
                                }
                            }
                        )
                    }
                }
            }

            // Note
            item {
                TextButton(
                    onClick = {
                        scope.launch {
                            billingManager.queryCurrentSubscription()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(Strings.restorePurchase, textDecoration = TextDecoration.Underline)
                }
            }

            // Note
            item {
                Text(
                    Strings.subscriptionDisclaimer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
        }
}

// list

private fun proFeatures() = listOf(
    FeatureItem(Icons.Outlined.Cloud, Strings.proCloudProjects, Strings.proCloudProjectsDesc),
    FeatureItem(Icons.Outlined.VpnKey, Strings.proActivationCodeSystem, Strings.proActivationCodeSystemDesc),
    FeatureItem(Icons.Outlined.Update, Strings.proAutoUpdates, Strings.proAutoUpdatesDesc),
    FeatureItem(Icons.Outlined.Campaign, Strings.proAnnouncements, Strings.proAnnouncementsDesc),
    FeatureItem(Icons.Outlined.Settings, Strings.proRemoteConfig, Strings.proRemoteConfigDesc),
    FeatureItem(Icons.Outlined.Webhook, Strings.proWebhook, Strings.proWebhookDesc),
    FeatureItem(Icons.Outlined.Analytics, Strings.proAnalytics, Strings.proAnalyticsDesc),
)

private fun ultraFeatures() = listOf(
    FeatureItem(Icons.Filled.Star, Strings.ultraIncludesAllPro, ""),
    FeatureItem(Icons.Outlined.Notifications, Strings.ultraFcmPush, Strings.ultraFcmPushDesc),
    FeatureItem(Icons.Outlined.VpnKey, Strings.ultraActivationCodeLimit, Strings.ultraActivationCodeLimitDesc),
    FeatureItem(Icons.Outlined.Campaign, Strings.ultraAnnouncementLimit, Strings.ultraAnnouncementLimitDesc),
    FeatureItem(Icons.Outlined.Cloud, Strings.ultraProjectLimit, Strings.ultraProjectLimitDesc),
    FeatureItem(Icons.Outlined.Storage, Strings.ultraR2Storage, Strings.ultraR2StorageDesc),
    FeatureItem(Icons.Outlined.Speed, Strings.ultraPrioritySupport, Strings.ultraPrioritySupportDesc),
)

// Note

private data class FeatureItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String
)

@Composable
private fun PeriodChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}

/**
 * Free card
 */
@Composable
private fun FreeCard(isCurrent: Boolean) {
    EnhancedElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column {
            // header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        Strings.tierFree,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "$0",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 36.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            Strings.freeForever,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }

            // list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val freeFeatures = listOf(
                    FeatureItem(Icons.Outlined.PhoneAndroid, Strings.freeUnlimitedApps, Strings.freeUnlimitedAppsDesc),
                    FeatureItem(Icons.Outlined.Build, Strings.freeLocalBuild, Strings.freeLocalBuildDesc),
                    FeatureItem(Icons.Outlined.Extension, Strings.freeExtensionSystem, Strings.freeExtensionSystemDesc),
                    FeatureItem(Icons.Outlined.Storefront, Strings.freeMarketplace, Strings.freeMarketplaceDesc),
                    FeatureItem(Icons.Outlined.SmartToy, Strings.freeAiAssistant, Strings.freeAiAssistantDesc),
                )
                freeFeatures.forEach { feature ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            feature.icon, null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                feature.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (feature.subtitle.isNotEmpty()) {
                                Text(
                                    feature.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = false
                ) {
                    Text(
                        if (isCurrent) Strings.currentScheme else Strings.basicPlan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriptionCard(
    tierName: String,
    price: String,
    period: String,
    gradient: List<Color>,
    features: List<FeatureItem>,
    isCurrent: Boolean,
    isLoading: Boolean,
    isRecommended: Boolean = false,
    isLifetime: Boolean = false,
    isDowngrade: Boolean = false,
    upgradeNote: String? = null,
    onSubscribe: () -> Unit
) {
    EnhancedElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = if (isRecommended) {
            Modifier.border(2.dp, Brush.linearGradient(gradient), RoundedCornerShape(20.dp))
        } else Modifier
    ) {
        Column {
            // header( gradient)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(gradient))
                    .padding(24.dp)
            ) {
                Column {
                    if (isRecommended) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Text(
                                Strings.recommended,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            tierName,
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isLifetime) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    "∞ " + Strings.periodLifetime,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            price,
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 36.sp
                        )
                        if (period.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                period,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    // hint
                    if (upgradeNote != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.TrendingUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    upgradeNote,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                features.forEach { feature ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            feature.icon, null,
                            modifier = Modifier.size(20.dp),
                            tint = gradient.first()
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                feature.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (feature.subtitle.isNotEmpty()) {
                                Text(
                                    feature.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // button
                PremiumButton(
                    onClick = onSubscribe,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isCurrent && !isLoading && !isDowngrade,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = gradient.first()
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            when {
                                isCurrent -> Strings.currentScheme
                                isDowngrade -> Strings.hasHigherPlan
                                isLifetime -> Strings.redeemWithActivationCode
                                else -> Strings.subscribeTierName.format(tierName)
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
