package com.webtoapp.core.billing

import com.webtoapp.core.auth.AuthResult
import com.webtoapp.core.cloud.CloudApiClient
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.launch












class BillingRepository(
    val billingManager: BillingManager,
    private val cloudApi: CloudApiClient
) {
    companion object {
        private const val TAG = "BillingRepository"
    }

    init {

        billingManager.onPurchaseVerified = { token, productId ->
            verifyWithServer(token, productId)
        }
    }




    fun initialize() {
        billingManager.connect()
    }




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




    suspend fun restorePurchases() {
        billingManager.queryCurrentSubscription()
    }




    fun destroy() {
        billingManager.disconnect()
    }
}
