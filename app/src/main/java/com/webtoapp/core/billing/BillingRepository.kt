package com.webtoapp.core.billing

import com.webtoapp.core.auth.AuthResult
import com.webtoapp.core.cloud.CloudApiClient
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.launch

/**
 * 支付业务层
 *
 * 连接 BillingManager（Google Play）和 CloudApiClient（服务器验证）。
 *
 * 购买流程:
 * 1. BillingManager 处理 Google Play 购买
 * 2. 购买成功后，BillingRepository 将 purchaseToken 发送到服务器
 * 3. 服务器通过 Google Play Developer API 验证后激活订阅
 * 4. 刷新用户信息
 */
class BillingRepository(
    val billingManager: BillingManager,
    private val cloudApi: CloudApiClient
) {
    companion object {
        private const val TAG = "BillingRepository"
    }

    init {
        // 设置购买验证回调
        billingManager.onPurchaseVerified = { token, productId ->
            verifyWithServer(token, productId)
        }
    }

    /**
     * 初始化：连接 Billing 并查询当前订阅
     */
    fun initialize() {
        billingManager.connect()
    }

    /**
     * 将购买 token 发送到服务器验证
     */
    private fun verifyWithServer(purchaseToken: String, productId: String) {
        AppLogger.i(TAG, "Verifying purchase with server: $productId")
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                when (val result = cloudApi.verifySubscription(purchaseToken, productId)) {
                    is AuthResult.Success -> {
                        AppLogger.i(TAG, "Server verification success: ${result.data}")
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Server verification failed: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Server verification exception: ${e.message}")
            }
        }
    }

    /**
     * 恢复购买（重新查询 Google Play 订阅状态）
     */
    suspend fun restorePurchases() {
        billingManager.queryCurrentSubscription()
    }

    /**
     * 清理
     */
    fun destroy() {
        billingManager.disconnect()
    }
}
