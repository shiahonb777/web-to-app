package com.webtoapp.core.cloud.api

import com.google.gson.JsonParser
import com.webtoapp.core.cloud.CloudApiClient
import com.webtoapp.core.cloud.internal.CloudApiSupport
import com.webtoapp.core.cloud.model.NotificationItem
import com.webtoapp.core.auth.AuthResult
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

interface NotificationApiContract {
    suspend fun listNotifications(page: Int = 1, size: Int = 20, unreadOnly: Boolean = false): AuthResult<Pair<List<NotificationItem>, Int>>
    suspend fun getUnreadNotificationCount(): AuthResult<Int>
    suspend fun markNotificationRead(notificationId: Int): AuthResult<Unit>
    suspend fun markAllNotificationsRead(): AuthResult<Unit>
}

internal class NotificationApi(
    private val support: CloudApiSupport,
) : NotificationApiContract {
    private val client = support.client
    private val jsonMediaType = support.jsonMediaType

    override suspend fun listNotifications(
        page: Int,
        size: Int,
        unreadOnly: Boolean,
    ): AuthResult<Pair<List<NotificationItem>, Int>> = support.authRequest {
        val params = "?page=$page&size=$size" + if (unreadOnly) "&unread_only=true" else ""
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/notifications$params")
            .header("Authorization", "Bearer $it")
            .get()
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            val unreadCount = data?.get("unread_count")?.asInt ?: 0
            val notifications = data?.getAsJsonArray("notifications")?.map { element ->
                val obj = element.asJsonObject
                NotificationItem(
                    id = obj.get("id")?.asInt ?: 0,
                    type = obj.get("type")?.asString ?: "",
                    title = obj.get("title")?.asString,
                    content = obj.get("content")?.asString,
                    refType = obj.get("ref_type")?.asString,
                    refId = obj.get("ref_id")?.asInt,
                    actorId = obj.get("actor_id")?.asInt,
                    isRead = obj.get("is_read")?.asBoolean ?: false,
                    createdAt = obj.get("created_at")?.asString,
                )
            } ?: emptyList()
            AuthResult.Success(Pair(notifications, unreadCount))
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    override suspend fun getUnreadNotificationCount(): AuthResult<Int> = support.authRequest {
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/notifications/unread-count")
            .header("Authorization", "Bearer $it")
            .get()
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val count = json.getAsJsonObject("data")?.get("unread_count")?.asInt ?: 0
            AuthResult.Success(count)
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    override suspend fun markNotificationRead(notificationId: Int): AuthResult<Unit> = support.authRequest {
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/notifications/read/$notificationId")
            .header("Authorization", "Bearer $it")
            .post("".toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit)
        else AuthResult.Error(support.parseError(responseBody, response.code))
    }

    override suspend fun markAllNotificationsRead(): AuthResult<Unit> = support.authRequest {
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/notifications/read-all")
            .header("Authorization", "Bearer $it")
            .post("".toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit)
        else AuthResult.Error(support.parseError(responseBody, response.code))
    }
}
