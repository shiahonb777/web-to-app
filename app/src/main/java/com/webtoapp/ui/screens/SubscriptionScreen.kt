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
import com.webtoapp.ui.design.WtaActionBar
import com.webtoapp.ui.design.WtaCapabilityLevel
import com.webtoapp.ui.design.WtaScreen
import com.webtoapp.ui.design.WtaSection
import com.webtoapp.ui.design.WtaSectionDivider
import com.webtoapp.ui.design.WtaSettingCard
import com.webtoapp.ui.design.WtaStatusBanner
import com.webtoapp.ui.design.WtaStatusTone
import com.webtoapp.ui.viewmodel.AuthState
import com.webtoapp.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import com.webtoapp.ui.components.EnhancedElevatedCard


private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}




private enum class UserTier {
    FREE, PRO_MONTHLY, PRO_QUARTERLY, PRO_YEARLY, PRO_LIFETIME, ULTRA_MONTHLY, ULTRA_QUARTERLY, ULTRA_YEARLY, ULTRA_LIFETIME
}

private fun UserTier.isPro() = this in listOf(UserTier.PRO_MONTHLY, UserTier.PRO_QUARTERLY, UserTier.PRO_YEARLY, UserTier.PRO_LIFETIME)
private fun UserTier.isUltra() = this in listOf(UserTier.ULTRA_MONTHLY, UserTier.ULTRA_QUARTERLY, UserTier.ULTRA_YEARLY, UserTier.ULTRA_LIFETIME)
private fun UserTier.isLifetime() = this == UserTier.PRO_LIFETIME || this == UserTier.ULTRA_LIFETIME







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
    val purchaseState by billingManager.purchaseState.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()


    LaunchedEffect(Unit) {
        if (!isConnected) {
            billingManager.connect()
        }
    }


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
    val currentPlanName = remember(userTier) {
        when (userTier) {
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
    }


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

    WtaScreen(
        title = Strings.selectPlan,
        onBack = onBack,
        snackbarHostState = snackbarHostState
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                WtaSection(
                    title = Strings.unlockAllFeatures,
                    description = Strings.chooseYourPlan,
                    headerStyle = com.webtoapp.ui.design.WtaSectionHeaderStyle.Hidden
                ) {
                    WtaStatusBanner(
                        title = currentPlanName,
                        message = if (userTier.isLifetime()) Strings.validForever else Strings.subscriptionDisclaimer,
                        tone = if (userTier.isUltra()) WtaStatusTone.Success else WtaStatusTone.Info
                    )
                }
            }

            item {
                WtaSection(
                    title = Strings.periodLifetime,
                    description = Strings.chooseYourPlan,
                    level = WtaCapabilityLevel.Common,
                    headerStyle = com.webtoapp.ui.design.WtaSectionHeaderStyle.Quiet,
                    collapsible = false
                ) {
                    WtaActionBar {
                        PeriodChip(Strings.periodMonthly, selectedPeriod == 0) { selectedPeriod = 0 }
                        PeriodChip(Strings.periodQuarterly, selectedPeriod == 1) { selectedPeriod = 1 }
                        PeriodChip(Strings.periodYearly, selectedPeriod == 2) { selectedPeriod = 2 }
                        PeriodChip(Strings.periodLifetime, selectedPeriod == 3) { selectedPeriod = 3 }
                    }
                }
            }

            item {
                WtaSection(
                    title = Strings.tierFree,
                    description = Strings.freeForever,
                    level = WtaCapabilityLevel.Common,
                    headerStyle = com.webtoapp.ui.design.WtaSectionHeaderStyle.Quiet,
                    collapsible = false
                ) {
                    FreeCard(isCurrent = userTier == UserTier.FREE)
                }
            }

            item {
                val isCurrentPro = when (selectedPeriod) {
                    0 -> userTier == UserTier.PRO_MONTHLY
                    1 -> userTier == UserTier.PRO_QUARTERLY
                    2 -> userTier == UserTier.PRO_YEARLY
                    3 -> userTier == UserTier.PRO_LIFETIME
                    else -> false
                }
                val isDowngrade = userTier.isUltra()
                val proPlan = when (selectedPeriod) {
                    0 -> SubscriptionPlan.PRO_MONTHLY
                    1 -> SubscriptionPlan.PRO_QUARTERLY
                    2 -> SubscriptionPlan.PRO_YEARLY
                    else -> SubscriptionPlan.PRO_LIFETIME
                }
                val proPrice = billingManager.getFormattedPrice(proPlan) ?: when (selectedPeriod) {
                    0 -> "$3"
                    1 -> "$8.10"
                    2 -> "$28.80"
                    else -> "$99"
                }
                val proPeriod = when (selectedPeriod) {
                    0 -> Strings.perMonth
                    1 -> Strings.perQuarter
                    2 -> Strings.perYear
                    else -> Strings.oneTime
                }
                WtaSection(
                    title = Strings.tierPro,
                    description = proPrice,
                    level = WtaCapabilityLevel.Advanced,
                    headerStyle = com.webtoapp.ui.design.WtaSectionHeaderStyle.Quiet,
                    collapsible = false
                ) {
                    SubscriptionCard(
                        tierName = Strings.tierPro,
                        price = proPrice,
                        period = proPeriod,
                        gradient = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        ),
                        features = proFeatures() + if (selectedPeriod == 3) {
                            listOf(FeatureItem(Icons.Outlined.AllInclusive, Strings.neverExpires, Strings.oneTimePurchaseLifetime))
                        } else {
                            emptyList()
                        },
                        isCurrent = isCurrentPro,
                        isDowngrade = isDowngrade,
                        isLoading = purchaseState is PurchaseState.Loading,
                        isLifetime = selectedPeriod == 3,
                        onSubscribe = {
                            if (!isCurrentPro && !isDowngrade && activity != null) {
                                billingManager.launchPurchase(activity, proPlan)
                            }
                        }
                    )
                }
            }

            item {
                val isCurrentUltra = when (selectedPeriod) {
                    0 -> userTier == UserTier.ULTRA_MONTHLY
                    1 -> userTier == UserTier.ULTRA_QUARTERLY
                    2 -> userTier == UserTier.ULTRA_YEARLY
                    3 -> userTier == UserTier.ULTRA_LIFETIME
                    else -> false
                }
                val ultraPlan = when (selectedPeriod) {
                    0 -> SubscriptionPlan.ULTRA_MONTHLY
                    1 -> SubscriptionPlan.ULTRA_QUARTERLY
                    2 -> SubscriptionPlan.ULTRA_YEARLY
                    else -> SubscriptionPlan.ULTRA_LIFETIME
                }
                val ultraPrice = billingManager.getFormattedPrice(ultraPlan) ?: when (selectedPeriod) {
                    0 -> "$9"
                    1 -> "$24.30"
                    2 -> "$86.40"
                    else -> "$199"
                }
                val ultraPeriod = when (selectedPeriod) {
                    0 -> Strings.perMonth
                    1 -> Strings.perQuarter
                    2 -> Strings.perYear
                    else -> Strings.oneTime
                }
                WtaSection(
                    title = Strings.tierUltra,
                    description = ultraPrice,
                    level = WtaCapabilityLevel.Lab,
                    headerStyle = com.webtoapp.ui.design.WtaSectionHeaderStyle.Quiet,
                    collapsible = false
                ) {
                    SubscriptionCard(
                        tierName = Strings.tierUltra,
                        price = ultraPrice,
                        period = ultraPeriod,
                        gradient = listOf(
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.error
                        ),
                        isRecommended = true,
                        features = ultraFeatures() + if (selectedPeriod == 3) {
                            listOf(FeatureItem(Icons.Outlined.AllInclusive, Strings.neverExpires, Strings.oneTimePurchaseLifetime))
                        } else {
                            emptyList()
                        },
                        isCurrent = isCurrentUltra,
                        isLoading = purchaseState is PurchaseState.Loading,
                        isLifetime = selectedPeriod == 3,
                        upgradeNote = if (userTier == UserTier.PRO_LIFETIME) Strings.proUpgradeNote else null,
                        onSubscribe = {
                            if (!isCurrentUltra && activity != null) {
                                billingManager.launchPurchase(activity, ultraPlan)
                            }
                        }
                    )
                }
            }

            item {
                WtaSettingCard {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            Strings.restorePurchase,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            Strings.subscriptionDisclaimer,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                }
            }
        }
    }
}



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



private data class FeatureItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String
)

@Composable
private fun PeriodChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(com.webtoapp.ui.design.WtaRadius.Button),
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




@Composable
private fun FreeCard(isCurrent: Boolean) {
    WtaSettingCard(contentPadding = PaddingValues(0.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f))
                    .padding(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        Strings.tierFree,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        Strings.freeForever,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "$0",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            WtaSectionDivider()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            feature.icon,
                            null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                feature.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (feature.subtitle.isNotEmpty()) {
                                Text(
                                    feature.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(com.webtoapp.ui.design.WtaRadius.Button),
                    enabled = false
                ) {
                    Text(
                        if (isCurrent) Strings.currentScheme else Strings.basicPlan,
                        fontWeight = FontWeight.SemiBold
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
        shape = RoundedCornerShape(com.webtoapp.ui.design.WtaRadius.Card),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = if (isRecommended) {
            Modifier.border(1.5.dp, Brush.linearGradient(gradient), RoundedCornerShape(com.webtoapp.ui.design.WtaRadius.Card))
        } else Modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isRecommended) {
                        Surface(
                            shape = RoundedCornerShape(com.webtoapp.ui.design.WtaRadius.Button),
                            color = gradient.first().copy(alpha = 0.12f)
                        ) {
                            Text(
                                Strings.recommended,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                                color = gradient.first(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            tierName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (isLifetime) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(com.webtoapp.ui.design.WtaRadius.Button),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    "∞ " + Strings.periodLifetime,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            price,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (period.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                period,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }

                    if (upgradeNote != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(com.webtoapp.ui.design.WtaRadius.Button),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.TrendingUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = gradient.first()
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    upgradeNote,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }


            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                features.forEach { feature ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            feature.icon, null,
                            modifier = Modifier.size(18.dp),
                            tint = gradient.first()
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
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


                PremiumButton(
                    onClick = onSubscribe,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(com.webtoapp.ui.design.WtaRadius.Button),
                    enabled = !isCurrent && !isLoading && !isDowngrade,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = gradient.first()
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            when {
                                isCurrent -> Strings.currentScheme
                                isDowngrade -> Strings.hasHigherPlan
                                isLifetime -> Strings.purchaseLifetime
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
