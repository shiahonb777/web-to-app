package com.webtoapp.core.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 订阅计划定义
 */
enum class SubscriptionPlan(
    val productId: String,
    val displayName: String,
    val tierName: String,
    val productType: String = BillingClient.ProductType.SUBS
) {
    PRO_MONTHLY("pro_monthly", "Pro 月度", "pro"),
    PRO_QUARTERLY("pro_quarterly", "Pro 季度", "pro"),
    PRO_YEARLY("pro_yearly", "Pro 年度", "pro"),
    PRO_LIFETIME("pro_lifetime", "Pro 终身", "pro", BillingClient.ProductType.INAPP),
    ULTRA_MONTHLY("ultra_monthly", "Ultra 月度", "ultra"),
    ULTRA_QUARTERLY("ultra_quarterly", "Ultra 季度", "ultra"),
    ULTRA_YEARLY("ultra_yearly", "Ultra 年度", "ultra"),
    ULTRA_LIFETIME("ultra_lifetime", "Ultra 终身", "ultra", BillingClient.ProductType.INAPP);

    val isLifetime: Boolean get() = productType == BillingClient.ProductType.INAPP

    companion object {
        fun fromProductId(productId: String): SubscriptionPlan? =
            entries.find { it.productId == productId }

        val allProductIds: List<String> get() = entries.map { it.productId }

        val subsProductIds: List<String> get() = entries.filter { it.productType == BillingClient.ProductType.SUBS }.map { it.productId }

        val inappProductIds: List<String> get() = entries.filter { it.productType == BillingClient.ProductType.INAPP }.map { it.productId }
    }
}

/**
 * 购买状态
 */
sealed class PurchaseState {
    data object Idle : PurchaseState()
    data object Loading : PurchaseState()
    data class Success(val plan: SubscriptionPlan) : PurchaseState()
    data class Error(val message: String) : PurchaseState()
}

/**
 * 订阅信息
 */
data class SubscriptionInfo(
    val plan: SubscriptionPlan,
    val isActive: Boolean,
    val expiryTimeMillis: Long = 0,
    val autoRenewing: Boolean = false,
    val purchaseToken: String = ""
)

/**
 * Google Play Billing 管理器
 *
 * 封装 BillingClient，管理订阅购买流程。
 *
 * 使用方式:
 * 1. 在 Application 或 Activity 中初始化 BillingManager
 * 2. 调用 connect() 建立连接
 * 3. 调用 queryProducts() 加载可购买的商品
 * 4. 调用 launchPurchase() 发起购买
 * 5. 购买结果通过 purchaseState Flow 观察
 */
class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"
    }

    private var billingClient: BillingClient? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ─── 状态 ───

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

    private val _currentSubscription = MutableStateFlow<SubscriptionInfo?>(null)
    val currentSubscription: StateFlow<SubscriptionInfo?> = _currentSubscription.asStateFlow()

    // 服务端验证回调
    var onPurchaseVerified: ((purchaseToken: String, productId: String) -> Unit)? = null

    // ═══════════════════════════════════════════
    // CONNECTION
    // ═══════════════════════════════════════════

    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5

    fun connect() {
        if (billingClient?.isReady == true) {
            _isConnected.value = true
            reconnectAttempts = 0
            return
        }

        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isConnected.value = true
                    reconnectAttempts = 0
                    AppLogger.i(TAG, "Billing connected")

                    // 自动加载商品和当前订阅
                    scope.launch {
                        queryProducts()
                        queryCurrentSubscription()
                    }
                } else {
                    _isConnected.value = false
                    AppLogger.e(TAG, "Billing connect failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                _isConnected.value = false
                if (reconnectAttempts < maxReconnectAttempts) {
                    val delayMs = 3000L * (1L shl reconnectAttempts) // Exponential backoff
                    reconnectAttempts++
                    AppLogger.w(TAG, "Billing disconnected, retry $reconnectAttempts/$maxReconnectAttempts in ${delayMs}ms")
                    scope.launch {
                        delay(delayMs)
                        connect()
                    }
                } else {
                    AppLogger.e(TAG, "Billing reconnect failed after $maxReconnectAttempts attempts")
                }
            }
        })
    }

    fun disconnect() {
        billingClient?.endConnection()
        billingClient = null
        _isConnected.value = false
        scope.cancel()
    }

    // ═══════════════════════════════════════════
    // PRODUCTS
    // ═══════════════════════════════════════════

    suspend fun queryProducts() {
        val client = billingClient ?: return
        val allProducts = mutableListOf<ProductDetails>()

        // 查询订阅商品 (SUBS)
        if (SubscriptionPlan.subsProductIds.isNotEmpty()) {
            val subsProductList = SubscriptionPlan.subsProductIds.map { productId ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            }
            val subsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(subsProductList)
                .build()
            val subsResult = client.queryProductDetails(subsParams)
            if (subsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                allProducts.addAll(subsResult.productDetailsList ?: emptyList())
            } else {
                AppLogger.e(TAG, "Query SUBS products failed: ${subsResult.billingResult.debugMessage}")
            }
        }

        // 查询一次性商品 (INAPP) — 终身套餐
        if (SubscriptionPlan.inappProductIds.isNotEmpty()) {
            val inappProductList = SubscriptionPlan.inappProductIds.map { productId ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            }
            val inappParams = QueryProductDetailsParams.newBuilder()
                .setProductList(inappProductList)
                .build()
            val inappResult = client.queryProductDetails(inappParams)
            if (inappResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                allProducts.addAll(inappResult.productDetailsList ?: emptyList())
            } else {
                AppLogger.e(TAG, "Query INAPP products failed: ${inappResult.billingResult.debugMessage}")
            }
        }

        _products.value = allProducts
        AppLogger.i(TAG, "Loaded ${allProducts.size} products (${SubscriptionPlan.subsProductIds.size} subs + ${SubscriptionPlan.inappProductIds.size} inapp)")
    }

    // ═══════════════════════════════════════════
    // PURCHASE
    // ═══════════════════════════════════════════

    fun launchPurchase(activity: Activity, productDetails: ProductDetails, offerToken: String?) {
        val client = billingClient
        if (client == null || !client.isReady) {
            _purchaseState.value = PurchaseState.Error("支付服务未就绪")
            return
        }

        _purchaseState.value = PurchaseState.Loading

        val paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
        if (!offerToken.isNullOrBlank()) {
            paramsBuilder.setOfferToken(offerToken)
        }
        val productDetailsParams = paramsBuilder.build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val result = client.launchBillingFlow(activity, billingFlowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _purchaseState.value = PurchaseState.Error("无法启动支付: ${result.debugMessage}")
        }
    }

    /**
     * 便捷方法：按计划类型购买
     */
    fun launchPurchase(activity: Activity, plan: SubscriptionPlan) {
        val productDetails = _products.value.find { it.productId == plan.productId }
        if (productDetails == null) {
            _purchaseState.value = PurchaseState.Error("商品信息未加载")
            return
        }

        val offerToken = if (plan.isLifetime) {
            // 一次性商品(INAPP) 在 billing 7.0+ 不再需要 offerToken
            null
        } else {
            // 订阅(SUBS) 使用 subscriptionOfferDetails
            productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
                ?: run {
                    _purchaseState.value = PurchaseState.Error("无可用优惠")
                    return
                }
        }

        launchPurchase(activity, productDetails, offerToken)
    }

    // ═══════════════════════════════════════════
    // PURCHASE CALLBACK
    // ═══════════════════════════════════════════

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    scope.launch { handlePurchase(purchase) }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseState.value = PurchaseState.Idle
                AppLogger.i(TAG, "Purchase canceled by user")
            }
            else -> {
                _purchaseState.value = PurchaseState.Error(
                    "支付失败 (${billingResult.responseCode}): ${billingResult.debugMessage}"
                )
                AppLogger.e(TAG, "Purchase error: ${billingResult.debugMessage}")
            }
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            AppLogger.w(TAG, "Purchase pending: ${purchase.purchaseState}")
            return
        }

        // 确认购买（必须在 3 天内确认，否则 Google 会自动退款）
        if (!purchase.isAcknowledged) {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            val result = billingClient?.acknowledgePurchase(acknowledgePurchaseParams)
            if (result?.responseCode != BillingClient.BillingResponseCode.OK) {
                AppLogger.e(TAG, "Acknowledge failed: ${result?.debugMessage}")
                _purchaseState.value = PurchaseState.Error("确认购买失败")
                return
            }
        }

        // 查找对应 plan
        val productId = purchase.products.firstOrNull() ?: return
        val plan = SubscriptionPlan.fromProductId(productId)
        if (plan != null) {
            _purchaseState.value = PurchaseState.Success(plan)
            _currentSubscription.value = SubscriptionInfo(
                plan = plan,
                isActive = true,
                autoRenewing = purchase.isAutoRenewing,
                purchaseToken = purchase.purchaseToken
            )

            // 通知服务端验证
            onPurchaseVerified?.invoke(purchase.purchaseToken, productId)

            AppLogger.i(TAG, "Purchase successful: ${plan.displayName}")
        }
    }

    // ═══════════════════════════════════════════
    // SUBSCRIPTION QUERY
    // ═══════════════════════════════════════════

    suspend fun queryCurrentSubscription() {
        val client = billingClient ?: return

        // 查询订阅购买 (SUBS)
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val subsResult = client.queryPurchasesAsync(subsParams)

        // 查询一次性购买 (INAPP) — 终身套餐
        val inappParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val inappResult = client.queryPurchasesAsync(inappParams)

        // 合并所有有效购买，优先取高等级
        val allPurchases = mutableListOf<Purchase>()
        if (subsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            allPurchases.addAll(subsResult.purchasesList.filter { p ->
                p.purchaseState == Purchase.PurchaseState.PURCHASED
            })
        }
        if (inappResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            allPurchases.addAll(inappResult.purchasesList.filter { p ->
                p.purchaseState == Purchase.PurchaseState.PURCHASED
            })
        }

        // 优先选择 Ultra，其次 Pro，其次终身
        val activePurchase = allPurchases.maxByOrNull { purchase ->
            val productId = purchase.products.firstOrNull() ?: ""
            val plan = SubscriptionPlan.fromProductId(productId)
            when {
                plan == SubscriptionPlan.ULTRA_LIFETIME -> 4
                plan?.tierName == "ultra" -> 3
                plan == SubscriptionPlan.PRO_LIFETIME -> 2
                plan?.tierName == "pro" -> 1
                else -> 0
            }
        }

        if (activePurchase != null) {
            val productId = activePurchase.products.firstOrNull()
            val plan = productId?.let { SubscriptionPlan.fromProductId(it) }
            if (plan != null) {
                _currentSubscription.value = SubscriptionInfo(
                    plan = plan,
                    isActive = true,
                    autoRenewing = activePurchase.isAutoRenewing,
                    purchaseToken = activePurchase.purchaseToken
                )
            }
        } else {
            _currentSubscription.value = null
        }
    }

    // ═══════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════

    /**
     * 获取商品的格式化价格
     */
    fun getFormattedPrice(plan: SubscriptionPlan): String? {
        val product = _products.value.find { it.productId == plan.productId }
        return if (plan.isLifetime) {
            product?.oneTimePurchaseOfferDetails?.formattedPrice
        } else {
            val offer = product?.subscriptionOfferDetails?.firstOrNull()
            val phase = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()
            phase?.formattedPrice
        }
    }

    /**
     * 获取商品的价格（微单位）
     */
    fun getPriceMicros(plan: SubscriptionPlan): Long? {
        val product = _products.value.find { it.productId == plan.productId }
        return if (plan.isLifetime) {
            product?.oneTimePurchaseOfferDetails?.priceAmountMicros
        } else {
            val offer = product?.subscriptionOfferDetails?.firstOrNull()
            val phase = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()
            phase?.priceAmountMicros
        }
    }

    fun resetPurchaseState() {
        _purchaseState.value = PurchaseState.Idle
    }
}
