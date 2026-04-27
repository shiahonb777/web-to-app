package com.webtoapp.core.cloud.api

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.webtoapp.core.cloud.CloudApiClient
import com.webtoapp.core.cloud.internal.CloudApiSupport
import com.webtoapp.core.cloud.model.ActivationRecord
import com.webtoapp.core.cloud.model.DeviceInfo
import com.webtoapp.core.cloud.model.RedeemPreview
import com.webtoapp.core.cloud.model.RedeemResult
import com.webtoapp.core.auth.AuthResult
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

interface ActivationApiContract {
    suspend fun redeemCode(code: String): AuthResult<RedeemResult>
    suspend fun previewRedeemCode(code: String): AuthResult<RedeemPreview>
    suspend fun getActivationHistory(): AuthResult<List<ActivationRecord>>
    suspend fun getDevices(): AuthResult<List<DeviceInfo>>
    suspend fun removeDevice(deviceId: Int): AuthResult<String>
    suspend fun verifySubscription(purchaseToken: String, productId: String): AuthResult<String>
}

internal class ActivationApi(
    private val support: CloudApiSupport,
) : ActivationApiContract {
    private val client = support.client
    private val tokenManager = support.tokenManager
    private val jsonMediaType = support.jsonMediaType

    override suspend fun redeemCode(code: String): AuthResult<RedeemResult> = support.authRequest {
        val body = JsonObject().apply {
            addProperty("code", code)
            addProperty("device_id", tokenManager.getDeviceId())
        }
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/activation/redeem")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val message = json.get("message")?.asString ?: "兑换成功"
            val data = json.getAsJsonObject("data")
            AuthResult.Success(
                RedeemResult(
                    message = message,
                    planType = data?.get("plan_type")?.asString ?: "",
                    daysAdded = data?.get("duration_days")?.asInt ?: 0,
                ),
            )
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    override suspend fun previewRedeemCode(code: String): AuthResult<RedeemPreview> = support.authRequest {
        val body = JsonObject().apply {
            addProperty("code", code)
            addProperty("device_id", tokenManager.getDeviceId())
        }
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/activation/preview")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            AuthResult.Success(
                RedeemPreview(
                    currentTier = data?.get("current_tier")?.asString ?: "free",
                    currentPlan = data?.get("current_plan")?.asString ?: "free",
                    currentExpiresAt = data?.get("current_expires_at")?.asString,
                    currentIsLifetime = data?.get("current_is_lifetime")?.asBoolean ?: false,
                    newTier = data?.get("new_tier")?.asString ?: "",
                    newPlan = data?.get("new_plan")?.asString ?: "",
                    newExpiresAt = data?.get("new_expires_at")?.asString,
                    newIsLifetime = data?.get("new_is_lifetime")?.asBoolean ?: false,
                    isUpgrade = data?.get("is_upgrade")?.asBoolean ?: false,
                    codeTier = data?.get("code_tier")?.asString ?: "",
                    codePlanType = data?.get("code_plan_type")?.asString ?: "",
                    durationDays = data?.get("duration_days")?.asInt ?: 0,
                ),
            )
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    override suspend fun getActivationHistory(): AuthResult<List<ActivationRecord>> = support.authRequest {
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/activation/history")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val dataArray = json.getAsJsonArray("data") ?: return@authRequest AuthResult.Success(emptyList())
            val list = dataArray.map { element ->
                val obj = element.asJsonObject
                ActivationRecord(
                    id = obj.get("id")?.asInt ?: 0,
                    type = obj.get("type")?.asString ?: "",
                    planType = obj.get("plan_type")?.asString ?: "",
                    proStart = obj.get("pro_start")?.asString,
                    proEnd = obj.get("pro_end")?.asString,
                    note = obj.get("note")?.asString,
                    createdAt = obj.get("created_at")?.asString,
                )
            }
            AuthResult.Success(list)
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    override suspend fun getDevices(): AuthResult<List<DeviceInfo>> = support.authRequest {
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/user/devices")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val dataArray = json.getAsJsonArray("data") ?: return@authRequest AuthResult.Success(emptyList())
            val list = dataArray.map { element ->
                val obj = element.asJsonObject
                DeviceInfo(
                    id = obj.get("id")?.asInt ?: 0,
                    deviceId = obj.get("device_id")?.asString ?: "",
                    deviceName = obj.get("device_name")?.asString ?: "",
                    deviceOs = obj.get("device_os")?.asString ?: "",
                    appVersion = obj.get("app_version")?.asString,
                    ipAddress = obj.get("ip_address")?.asString,
                    country = obj.get("country")?.asString,
                    lastActiveAt = obj.get("last_active_at")?.asString,
                    isCurrent = obj.get("device_id")?.asString == tokenManager.getDeviceId(),
                )
            }
            AuthResult.Success(list)
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    override suspend fun removeDevice(deviceId: Int): AuthResult<String> = support.authRequest {
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/user/devices/$deviceId")
            .header("Authorization", "Bearer $it")
            .delete()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            AuthResult.Success(json.get("message")?.asString ?: "设备已解绑")
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    override suspend fun verifySubscription(purchaseToken: String, productId: String): AuthResult<String> = support.authRequest {
        val body = JsonObject().apply {
            addProperty("purchase_token", purchaseToken)
            addProperty("product_id", productId)
            addProperty("platform", "android")
        }

        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/billing/verify")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            AuthResult.Success(json.get("message")?.asString ?: "订阅已激活")
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }
}
