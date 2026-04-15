package com.webtoapp.core.cloud.api

import com.google.gson.JsonParser
import com.webtoapp.core.cloud.CloudApiClient
import com.webtoapp.core.cloud.internal.CloudApiSupport
import com.webtoapp.core.cloud.model.BackupCreateResult
import com.webtoapp.core.cloud.model.BackupItem
import com.webtoapp.core.cloud.model.BackupListResponse
import com.webtoapp.core.cloud.model.BackupRecord
import com.webtoapp.core.auth.AuthResult
import java.io.File
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

interface BackupApiContract {
    suspend fun listBackups(projectId: Int): AuthResult<List<BackupRecord>>
    suspend fun createBackup(projectId: Int, platform: String, zipFile: File): AuthResult<BackupRecord>
    suspend fun listBackups(): AuthResult<BackupListResponse>
    suspend fun createBackup(): AuthResult<BackupCreateResult>
    suspend fun deleteBackup(filename: String): AuthResult<String>
}

internal class BackupApi(
    private val support: CloudApiSupport,
) : BackupApiContract {
    private val client = support.client
    private val jsonMediaType = support.jsonMediaType

    override suspend fun listBackups(projectId: Int): AuthResult<List<BackupRecord>> = support.authRequest {
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/projects/$projectId/backups")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val dataArray = json.getAsJsonArray("data")
            val backups = dataArray?.map { element ->
                val item = element.asJsonObject
                BackupRecord(
                    id = item.get("id")?.asInt ?: 0,
                    platform = item.get("platform")?.asString ?: "",
                    status = item.get("status")?.asString ?: "",
                    repoUrl = item.get("repo_url")?.asString,
                    fileSize = item.get("file_size")?.asLong ?: 0,
                    createdAt = item.get("created_at")?.asString,
                )
            } ?: emptyList()
            AuthResult.Success(backups)
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    override suspend fun createBackup(projectId: Int, platform: String, zipFile: File): AuthResult<BackupRecord> = support.authRequest {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", zipFile.name, zipFile.asRequestBody("application/zip".toMediaType()))
            .build()

        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/projects/$projectId/backup?platform=$platform")
            .header("Authorization", "Bearer $it")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            AuthResult.Success(
                BackupRecord(
                    id = data?.get("id")?.asInt ?: 0,
                    platform = data?.get("platform")?.asString ?: platform,
                    status = data?.get("status")?.asString ?: "unknown",
                    repoUrl = data?.get("repo_url")?.asString,
                    fileSize = data?.get("file_size")?.asLong ?: 0,
                    createdAt = null,
                ),
            )
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    override suspend fun listBackups(): AuthResult<BackupListResponse> = support.authRequest { token ->
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/backups")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            val backups = data?.getAsJsonArray("backups")?.map { element ->
                val obj = element.asJsonObject
                BackupItem(
                    filename = obj.get("filename")?.asString ?: "",
                    size = obj.get("size_str")?.asString ?: obj.get("size")?.asString ?: "0KB",
                    sizeBytes = obj.get("size_bytes")?.asLong ?: obj.get("size")?.asLong ?: 0,
                    downloadUrl = obj.get("download_url")?.asString,
                    createdAt = obj.get("created_at")?.asString,
                )
            } ?: emptyList()
            AuthResult.Success(
                BackupListResponse(
                    backups = backups,
                    count = data?.get("count")?.asInt ?: 0,
                    quota = data?.get("quota")?.asInt ?: 0,
                    tier = data?.get("tier")?.asString ?: "free",
                ),
            )
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    override suspend fun createBackup(): AuthResult<BackupCreateResult> = support.authRequest { token ->
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/backups")
            .header("Authorization", "Bearer $token")
            .post("".toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            AuthResult.Success(
                BackupCreateResult(
                    filename = data?.get("filename")?.asString ?: "",
                    size = data?.get("size")?.asInt ?: 0,
                    downloadUrl = data?.get("download_url")?.asString,
                    projectCount = data?.get("project_count")?.asInt ?: 0,
                    moduleCount = data?.get("module_count")?.asInt ?: 0,
                    message = json.get("message")?.asString ?: "Backup created",
                ),
            )
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    override suspend fun deleteBackup(filename: String): AuthResult<String> = support.authRequest { token ->
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/backups/$filename")
            .header("Authorization", "Bearer $token")
            .delete()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            AuthResult.Success(json.get("message")?.asString ?: "已删除")
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }
}
