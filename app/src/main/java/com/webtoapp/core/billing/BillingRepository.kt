package com.webtoapp.core.billing

import com.webtoapp.core.auth.AuthResult
import com.webtoapp.core.cloud.CloudApiClient
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.launch

/**
 * Note: brief English comment.
 *
 * Note: brief English comment.
 *
 * Note: brief English comment.
 * Note: brief English comment.
 * Note: brief English comment.
 * Note: brief English comment.
 * Note: brief English comment.
 */
class BillingRepository(
    val billingManager: BillingManager,
    private val cloudApi: CloudApiClient
) {
    companion object {
        private const val TAG = "BillingRepository"
    }

    init {
        // Note: brief English comment.
        billingManager.onPurchaseVerified = { token, productId ->
            verifyWithServer(token, productId)
        }
    }

    /**
     * Note: brief English comment.
     */
    fun initialize() {
        billingManager.connect()
    }

    /**
     * Note: brief English comment.
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
     * Note: brief English comment.
     */
    suspend fun restorePurchases() {
        billingManager.queryCurrentSubscription()
    }

    /**
     * Note: brief English comment.
     */
    fun destroy() {
        billingManager.disconnect()
    }
}
