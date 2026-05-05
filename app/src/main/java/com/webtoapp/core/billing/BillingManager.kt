package com.webtoapp.core.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow




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




sealed class PurchaseState {
    data object Idle : PurchaseState()
    data object Loading : PurchaseState()
    data class Success(val plan: SubscriptionPlan) : PurchaseState()
    data class Error(val message: String) : PurchaseState()
}




data class SubscriptionInfo(
    val plan: SubscriptionPlan,
    val isActive: Boolean,
    val expiryTimeMillis: Long = 0,
    val autoRenewing: Boolean = false,
    val purchaseToken: String = ""
)













class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"
    }

    private var billingClient: BillingClient? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())



    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

    private val _currentSubscription = MutableStateFlow<SubscriptionInfo?>(null)
    val currentSubscription: StateFlow<SubscriptionInfo?> = _currentSubscription.asStateFlow()


    var onPurchaseVerified: ((purchaseToken: String, productId: String) -> Unit)? = null





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
                    val delayMs = 3000L * (1L shl reconnectAttempts)
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





    suspend fun queryProducts() {
        val client = billingClient ?: return
        val allProducts = mutableListOf<ProductDetails>()


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




    fun launchPurchase(activity: Activity, plan: SubscriptionPlan) {
        val productDetails = _products.value.find { it.productId == plan.productId }
        if (productDetails == null) {
            _purchaseState.value = PurchaseState.Error("商品信息未加载")
            return
        }

        val offerToken = if (plan.isLifetime) {

            null
        } else {

            productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
                ?: run {
                    _purchaseState.value = PurchaseState.Error("无可用优惠")
                    return
                }
        }

        launchPurchase(activity, productDetails, offerToken)
    }





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


            onPurchaseVerified?.invoke(purchase.purchaseToken, productId)

            AppLogger.i(TAG, "Purchase successful: ${plan.displayName}")
        }
    }





    suspend fun queryCurrentSubscription() {
        val client = billingClient ?: return


        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val subsResult = client.queryPurchasesAsync(subsParams)


        val inappParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val inappResult = client.queryPurchasesAsync(inappParams)


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
