package com.webtoapp.core.cloud

import android.os.Build
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.webtoapp.core.auth.AuthResult
import com.webtoapp.core.auth.TokenManager
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit


object SubscriptionTier {
    const val FREE = "free"
    const val PRO = "pro"
    const val ULTRA = "ultra"
    const val LIFETIME = "lifetime"
}






class CloudApiClient(private val tokenManager: TokenManager, context: android.content.Context? = null) {

    companion object {
        const val BASE_URL = "https://api.shiaho.sbs"
        private const val TAG = "CloudApiClient"
        private const val HTTP_CACHE_SIZE = 10L * 1024 * 1024
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()


    private val httpCache: okhttp3.Cache? = context?.let {
        try { okhttp3.Cache(java.io.File(it.cacheDir, "http_cache"), HTTP_CACHE_SIZE) }
        catch (_: Exception) { null }
    }

    private val client = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .connectionPool(okhttp3.ConnectionPool(maxIdleConnections = 8, keepAliveDuration = 30, timeUnit = TimeUnit.SECONDS))
        .apply { httpCache?.let { cache(it) } }
        .build()






    suspend fun redeemCode(code: String): AuthResult<RedeemResult> = authRequest {
        val body = JsonObject().apply {
            addProperty("code", code)
            addProperty("device_id", tokenManager.getDeviceId())
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/activation/redeem")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val msg = json.get("message")?.asString ?: "兑换成功"
            val data = json.getAsJsonObject("data")
            AuthResult.Success(RedeemResult(
                message = msg,
                planType = data?.get("plan_type")?.asString ?: "",
                daysAdded = data?.get("duration_days")?.asInt ?: 0
            ))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun previewRedeemCode(code: String): AuthResult<RedeemPreview> = authRequest {
        val body = JsonObject().apply {
            addProperty("code", code)
            addProperty("device_id", tokenManager.getDeviceId())
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/activation/preview")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            AuthResult.Success(RedeemPreview(
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
            ))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun getActivationHistory(): AuthResult<List<ActivationRecord>> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/activation/history")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val dataArr = json.getAsJsonArray("data") ?: return@authRequest AuthResult.Success(emptyList())
            val list = dataArr.map { el ->
                val obj = el.asJsonObject
                ActivationRecord(
                    id = obj.get("id")?.asInt ?: 0,
                    type = obj.get("type")?.asString ?: "",
                    planType = obj.get("plan_type")?.asString ?: "",
                    proStart = obj.get("pro_start")?.asString,
                    proEnd = obj.get("pro_end")?.asString,
                    note = obj.get("note")?.asString,
                    createdAt = obj.get("created_at")?.asString
                )
            }
            AuthResult.Success(list)
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }






    suspend fun getDevices(): AuthResult<List<DeviceInfo>> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/user/devices")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val dataArr = json.getAsJsonArray("data") ?: return@authRequest AuthResult.Success(emptyList())
            val list = dataArr.map { el ->
                val obj = el.asJsonObject
                DeviceInfo(
                    id = obj.get("id")?.asInt ?: 0,
                    deviceId = obj.get("device_id")?.asString ?: "",
                    deviceName = obj.get("device_name")?.asString ?: "",
                    deviceOs = obj.get("device_os")?.asString ?: "",
                    appVersion = obj.get("app_version")?.asString,
                    ipAddress = obj.get("ip_address")?.asString,
                    country = obj.get("country")?.asString,
                    lastActiveAt = obj.get("last_active_at")?.asString,
                    isCurrent = obj.get("device_id")?.asString == tokenManager.getDeviceId()
                )
            }
            AuthResult.Success(list)
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun removeDevice(deviceId: Int): AuthResult<String> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/user/devices/$deviceId")
            .header("Authorization", "Bearer $it")
            .delete()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            AuthResult.Success(json.get("message")?.asString ?: "设备已解绑")
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }






    suspend fun getAnnouncements(appVersion: String? = null): AuthResult<List<AnnouncementData>> =
        withContext(Dispatchers.IO) {
            try {
                val urlBuilder = StringBuilder("$BASE_URL/api/v1/announcements")
                appVersion?.let { urlBuilder.append("?app_version=$it") }

                val reqBuilder = Request.Builder().url(urlBuilder.toString()).get()
                tokenManager.getAccessToken()?.let { reqBuilder.header("Authorization", "Bearer $it") }
                val response = client.newCall(reqBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val json = JsonParser.parseString(responseBody).asJsonObject
                    val dataArr = json.getAsJsonArray("data") ?: return@withContext AuthResult.Success(emptyList())
                    val list = dataArr.map { el ->
                        val obj = el.asJsonObject
                        AnnouncementData(
                            id = obj.get("id")?.asInt ?: 0,
                            title = obj.get("title")?.asString ?: "",
                            content = obj.get("content")?.asString ?: "",
                            type = obj.get("display_type")?.asString ?: obj.get("type")?.asString ?: "popup",
                            actionUrl = obj.get("action_url")?.asString,
                            actionText = obj.get("action_text")?.asString,
                            priority = obj.get("priority")?.asInt ?: 0,
                            imageUrl = obj.get("image_url")?.asString
                        )
                    }
                    AuthResult.Success(list)
                } else {
                    AuthResult.Error("获取公告失败")
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Get announcements failed", e)
                AuthResult.Error("网络连接失败")
            }
        }






    suspend fun checkAppUpdate(currentVersionCode: Int): AuthResult<AppUpdateInfo?> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/api/v1/app-version/check?current_version_code=$currentVersionCode")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val json = JsonParser.parseString(responseBody).asJsonObject
                    val data = json.getAsJsonObject("data")
                    if (data == null || !data.has("has_update") || !data.get("has_update").asBoolean) {
                        return@withContext AuthResult.Success(null)
                    }

                    val latest = data.getAsJsonObject("latest_version") ?: data
                    AuthResult.Success(AppUpdateInfo(
                        hasUpdate = true,
                        versionCode = latest.get("version_code")?.asInt ?: 0,
                        versionName = latest.get("version_name")?.asString ?: "",
                        title = latest.get("title")?.asString,
                        changelog = latest.get("changelog")?.asString,
                        downloadUrl = latest.get("download_url")?.asString ?: "",
                        isForceUpdate = data.get("is_force")?.asBoolean ?: latest.get("is_force_update")?.asBoolean ?: false,
                        fileSize = latest.get("file_size")?.asLong
                    ))
                } else {
                    AuthResult.Error("检查更新失败")
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Check app update failed", e)
                AuthResult.Error("网络连接失败")
            }
        }






    suspend fun getRemoteConfig(): AuthResult<List<RemoteConfigItem>> =
        withContext(Dispatchers.IO) {
            try {
                val reqBuilder = Request.Builder()
                    .url("$BASE_URL/api/v1/app-version/remote-config")
                    .get()
                tokenManager.getAccessToken()?.let { reqBuilder.header("Authorization", "Bearer $it") }
                val response = client.newCall(reqBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val json = JsonParser.parseString(responseBody).asJsonObject
                    val data = json.getAsJsonObject("data") ?: json
                    val configsObj = data.getAsJsonObject("configs")
                        ?: data.getAsJsonObject("config")
                    val list = when {
                        configsObj != null -> configsObj.entrySet().map { entry ->
                            val valueElement = entry.value
                            val value = if (valueElement.isJsonObject && valueElement.asJsonObject.has("value")) {
                                valueElement.asJsonObject.get("value").asString
                            } else {
                                valueElement.asString
                            }
                            RemoteConfigItem(
                                key = entry.key,
                                value = value,
                                description = null
                            )
                        }
                        data.has("configs") && data.get("configs").isJsonArray -> data.getAsJsonArray("configs").map { el ->
                            val obj = el.asJsonObject
                            RemoteConfigItem(
                                key = obj.get("key")?.asString ?: "",
                                value = obj.get("value")?.asString ?: "",
                                description = obj.get("description")?.let { if (it.isJsonNull) null else it.asString }
                            )
                        }.filter { it.key.isNotBlank() }
                        else -> emptyList()
                    }
                    AuthResult.Success(list)
                } else {
                    AuthResult.Error("获取远程配置失败")
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Get remote config failed", e)
                AuthResult.Error("网络连接失败")
            }
        }






    suspend fun listProjects(): AuthResult<List<CloudProject>> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val dataArr = json.getAsJsonArray("data") ?: return@authRequest AuthResult.Success(emptyList())
            AuthResult.Success(dataArr.map { parseProject(it.asJsonObject) })
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun createProject(name: String, description: String? = null,
                              githubRepo: String? = null, giteeRepo: String? = null): AuthResult<CloudProject> = authRequest {
        val body = JsonObject().apply {
            addProperty("project_name", name)
            description?.let { addProperty("description", it) }
            githubRepo?.let { addProperty("github_repo", it) }
            giteeRepo?.let { addProperty("gitee_repo", it) }
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            AuthResult.Success(parseProject(json.getAsJsonObject("data")))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun deleteProject(projectId: Int): AuthResult<String> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId")
            .header("Authorization", "Bearer $it")
            .delete()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            AuthResult.Success("项目已删除")
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun updateProject(projectId: Int, name: String? = null, description: String? = null,
                              githubRepo: String? = null, giteeRepo: String? = null): AuthResult<CloudProject> = authRequest {
        val body = JsonObject().apply {
            name?.let { addProperty("project_name", it) }
            description?.let { addProperty("description", it) }
            githubRepo?.let { addProperty("github_repo", it) }
            giteeRepo?.let { addProperty("gitee_repo", it) }
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId")
            .header("Authorization", "Bearer $it")
            .put(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            AuthResult.Success(parseProject(json.getAsJsonObject("data")))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun publishVersion(
        projectId: Int,
        apkFile: File,
        versionCode: Int,
        versionName: String,
        title: String? = null,
        changelog: String? = null,
        uploadTo: String = "github"
    ): AuthResult<ProjectVersion> = authRequest {
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("version_code", versionCode.toString())
            .addFormDataPart("version_name", versionName)
            .addFormDataPart("upload_to", uploadTo)
            .addFormDataPart("apk_file", apkFile.name, apkFile.asRequestBody("application/vnd.android.package-archive".toMediaType()))
        title?.let { multipart.addFormDataPart("title", it) }
        changelog?.let { multipart.addFormDataPart("changelog", it) }

        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/versions/publish")
            .header("Authorization", "Bearer $it")
            .post(multipart.build())
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            AuthResult.Success(ProjectVersion(
                id = data?.get("id")?.asInt ?: 0,
                versionCode = data?.get("version_code")?.asInt ?: versionCode,
                versionName = data?.get("version_name")?.asString ?: versionName,
                title = data?.get("title")?.asString,
                changelog = data?.get("changelog")?.asString,
                downloadUrlGithub = data?.get("download_url_github")?.asString,
                downloadUrlGitee = data?.get("download_url_gitee")?.asString,
                isForceUpdate = data?.get("is_force_update")?.asBoolean ?: false,
                createdAt = data?.get("created_at")?.asString
            ))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun listVersions(projectId: Int): AuthResult<List<ProjectVersion>> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/versions")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val dataArr = json.getAsJsonArray("data") ?: return@authRequest AuthResult.Success(emptyList())
            AuthResult.Success(dataArr.map { el ->
                val obj = el.asJsonObject
                ProjectVersion(
                    id = obj.get("id")?.asInt ?: 0,
                    versionCode = obj.get("version_code")?.asInt ?: 0,
                    versionName = obj.get("version_name")?.asString ?: "",
                    title = obj.get("title")?.asString,
                    changelog = obj.get("changelog")?.asString,
                    downloadUrlGithub = obj.get("download_url_github")?.asString,
                    downloadUrlGitee = obj.get("download_url_gitee")?.asString,
                    isForceUpdate = obj.get("is_force_update")?.asBoolean ?: false,
                    createdAt = obj.get("created_at")?.asString
                )
            })
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }










    suspend fun requestUploadToken(
        projectId: Int,
        versionCode: Int,
        versionName: String,
        title: String?,
        changelog: String?,
        fileName: String,
    ): AuthResult<DirectUploadToken> = authRequest {
        val form = okhttp3.FormBody.Builder()
            .add("version_code", versionCode.toString())
            .add("version_name", versionName)
            .add("file_name", fileName)
        title?.let { form.add("title", it) }
        changelog?.let { form.add("changelog", it) }

        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/upload-token")
            .header("Authorization", "Bearer $it")
            .post(form.build())
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            AuthResult.Success(DirectUploadToken(
                token = data?.get("token")?.asString ?: "",
                expiresAt = data?.get("expires_at")?.asString ?: "",
                uploadUrl = data?.get("upload_url")?.asString ?: "",
                releaseId = data?.get("release_id")?.asInt ?: 0,
                owner = data?.get("owner")?.asString ?: "",
                repo = data?.get("repo")?.asString ?: "",
                tag = data?.get("tag")?.asString ?: "",
                contentType = data?.get("content_type")?.asString ?: "application/vnd.android.package-archive",
            ))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }





    suspend fun directUploadToGithub(
        uploadInfo: DirectUploadToken,
        apkFile: File,
        onProgress: (Float) -> Unit = {},
    ): AuthResult<String> = withContext(Dispatchers.IO) {
        try {
            val fileSize = apkFile.length()
            val requestBody = object : okhttp3.RequestBody() {
                override fun contentType() = uploadInfo.contentType.toMediaType()
                override fun contentLength() = fileSize
                override fun writeTo(sink: okio.BufferedSink) {
                    val buffer = ByteArray(8192)
                    var uploaded = 0L
                    apkFile.inputStream().use { input ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            sink.write(buffer, 0, read)
                            uploaded += read
                            onProgress(uploaded.toFloat() / fileSize)
                        }
                    }
                }
            }


            val uploadClient = client.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.MINUTES)
                .build()

            val request = Request.Builder()
                .url(uploadInfo.uploadUrl)
                .header("Authorization", "Bearer ${uploadInfo.token}")
                .header("Accept", "application/vnd.github+json")
                .post(requestBody)
                .build()

            AppLogger.i(TAG, "Direct upload starting to ${uploadInfo.uploadUrl}")
            val response = uploadClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JsonParser.parseString(body).asJsonObject
                val downloadUrl = json.get("browser_download_url")?.asString ?: ""
                AppLogger.i(TAG, "Direct upload success: $downloadUrl")
                AuthResult.Success(downloadUrl)
            } else {
                AppLogger.e(TAG, "Direct upload failed: ${response.code} $body")
                AuthResult.Error("GitHub upload failed: ${response.code}")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Direct upload exception", e)
            AuthResult.Error("Upload failed: ${e.message}")
        }
    }




    suspend fun confirmDirectUpload(
        projectId: Int,
        versionCode: Int,
        versionName: String,
        title: String?,
        changelog: String?,
        downloadUrl: String,
        fileSize: Long,
    ): AuthResult<ProjectVersion> = authRequest {
        val form = okhttp3.FormBody.Builder()
            .add("version_code", versionCode.toString())
            .add("version_name", versionName)
            .add("download_url", downloadUrl)
            .add("file_size", fileSize.toString())
        title?.let { form.add("title", it) }
        changelog?.let { form.add("changelog", it) }

        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/versions/confirm-direct")
            .header("Authorization", "Bearer $it")
            .post(form.build())
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            AuthResult.Success(ProjectVersion(
                id = data?.get("id")?.asInt ?: 0,
                versionCode = data?.get("version_code")?.asInt ?: versionCode,
                versionName = data?.get("version_name")?.asString ?: versionName,
                title = data?.get("title")?.asString,
                changelog = data?.get("changelog")?.asString,
                downloadUrlGithub = data?.get("download_url_github")?.asString,
                downloadUrlGitee = data?.get("download_url_gitee")?.asString,
                isForceUpdate = data?.get("is_force_update")?.asBoolean ?: false,
                createdAt = data?.get("created_at")?.asString
            ))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }





    suspend fun uploadAsset(
        file: java.io.File,
        contentType: String = "image/png",
        onProgress: (Float) -> Unit = {}
    ): AuthResult<String> = authRequest {

        val jsonBody = """{"file_name":"${file.name}","content_type":"$contentType"}"""
        val tokenRequest = Request.Builder()
            .url("$BASE_URL/api/v1/assets/upload-token")
            .header("Authorization", "Bearer $it")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        val tokenResponse = client.newCall(tokenRequest).execute()
        val tokenRespBody = tokenResponse.body?.string() ?: ""

        if (!tokenResponse.isSuccessful) {
            return@authRequest AuthResult.Error(parseError(tokenRespBody, tokenResponse.code))
        }

        val tokenJson = JsonParser.parseString(tokenRespBody).asJsonObject
        val tokenData = tokenJson.getAsJsonObject("data")
        val token = tokenData.get("token").asString
        val uploadUrl = tokenData.get("upload_url").asString
        val fileName = tokenData.get("file_name").asString
        val ct = tokenData.get("content_type").asString


        val fileBody = object : okhttp3.RequestBody() {
            override fun contentType() = ct.toMediaType()
            override fun contentLength() = file.length()
            override fun writeTo(sink: okio.BufferedSink) {
                val totalBytes = file.length()
                var bytesWritten = 0L
                file.inputStream().use { input ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        sink.write(buffer, 0, read)
                        bytesWritten += read
                        onProgress(bytesWritten.toFloat() / totalBytes)
                    }
                }
            }
        }

        val uploadRequest = Request.Builder()
            .url("$uploadUrl?name=$fileName")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .post(fileBody)
            .build()

        val uploadResponse = client.newCall(uploadRequest).execute()
        val uploadRespBody = uploadResponse.body?.string() ?: ""

        if (uploadResponse.isSuccessful) {
            val uploadJson = JsonParser.parseString(uploadRespBody).asJsonObject
            val downloadUrl = uploadJson.get("browser_download_url").asString
            AuthResult.Success(downloadUrl)
        } else {
            AuthResult.Error("Asset upload failed: ${uploadResponse.code}")
        }
    }


    suspend fun getAnalytics(projectId: Int, days: Int = 7): AuthResult<AnalyticsData> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/analytics?days=$days")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            val summary = data?.getAsJsonObject("summary")
            AuthResult.Success(AnalyticsData(
                totalInstalls = summary?.get("total_installs")?.asInt ?: data?.get("total_installs")?.asInt ?: 0,
                totalOpens = summary?.get("total_opens")?.asInt ?: data?.get("total_opens")?.asInt ?: 0,
                totalActive = summary?.get("total_active_users")?.asInt ?: data?.get("total_active")?.asInt ?: 0,
                totalCrashes = summary?.get("total_crashes")?.asInt ?: data?.get("total_crashes")?.asInt ?: 0,
                totalDownloads = summary?.get("total_downloads")?.asInt ?: 0,
                totalDevices = summary?.get("total_devices")?.asInt ?: 0,
                avgDailyActive = summary?.get("avg_daily_active")?.asFloat ?: 0f,
                dailyStats = (data?.getAsJsonArray("trend") ?: data?.getAsJsonArray("daily"))?.map { el ->
                    val d = el.asJsonObject
                    DailyStat(
                        date = d.get("date")?.asString ?: "",
                        installs = d.get("installs")?.asInt ?: 0,
                        opens = d.get("opens")?.asInt ?: 0,
                        active = d.get("active")?.asInt ?: d.get("active_users")?.asInt ?: 0,
                        crashes = d.get("crashes")?.asInt ?: 0,
                        downloads = d.get("downloads")?.asInt ?: 0
                    )
                } ?: emptyList(),
                countryDistribution = data?.getAsJsonObject("country_distribution")?.entrySet()?.associate {
                    it.key to it.value.asInt
                } ?: emptyMap(),
                versionDistribution = data?.getAsJsonObject("version_distribution")?.entrySet()?.associate {
                    it.key to it.value.asInt
                } ?: emptyMap(),
                deviceDistribution = data?.getAsJsonObject("device_distribution")?.entrySet()?.associate {
                    it.key to it.value.asInt
                } ?: emptyMap(),
                osDistribution = data?.getAsJsonObject("os_distribution")?.entrySet()?.associate {
                    it.key to it.value.asInt
                } ?: emptyMap()
            ))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }






    suspend fun publishVersionR2(
        projectId: Int, apkFile: File, versionCode: Int, versionName: String,
        title: String? = null, changelog: String? = null
    ): AuthResult<ProjectVersion> = authRequest {
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("version_code", versionCode.toString())
            .addFormDataPart("version_name", versionName)
            .addFormDataPart("apk_file", apkFile.name, apkFile.asRequestBody("application/vnd.android.package-archive".toMediaType()))
        title?.let { multipart.addFormDataPart("title", it) }
        changelog?.let { multipart.addFormDataPart("changelog", it) }

        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/versions/publish-r2")
            .header("Authorization", "Bearer $it")
            .post(multipart.build())
            .build()

        val uploadClient = client.newBuilder()
            .writeTimeout(5, java.util.concurrent.TimeUnit.MINUTES)
            .readTimeout(5, java.util.concurrent.TimeUnit.MINUTES)
            .build()

        val response = uploadClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            AuthResult.Success(ProjectVersion(
                id = data?.get("id")?.asInt ?: 0,
                versionCode = data?.get("version_code")?.asInt ?: versionCode,
                versionName = data?.get("version_name")?.asString ?: versionName,
                title = data?.get("title")?.asString,
                changelog = data?.get("changelog")?.asString,
                downloadUrlGithub = data?.get("download_url_github")?.asString,
                downloadUrlGitee = data?.get("download_url_gitee")?.asString,
                isForceUpdate = data?.get("is_force_update")?.asBoolean ?: false,
                createdAt = data?.get("created_at")?.asString
            ))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }






    suspend fun registerPushToken(projectId: Int, fcmToken: String, deviceId: String): AuthResult<String> = authRequest {
        val body = JsonObject().apply {
            addProperty("fcm_token", fcmToken)
            addProperty("device_id", deviceId)
            addProperty("platform", "android")
            addProperty("device_model", Build.MODEL)
            addProperty("os_version", "Android ${Build.VERSION.RELEASE}")
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/push/register")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            AuthResult.Success("推送 Token 注册成功")
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun sendPushNotification(projectId: Int, title: String, body: String,
                                      targetType: String = "all",
                                      targetUserIds: List<Int>? = null): AuthResult<String> = authRequest {
        val jsonBody = JsonObject().apply {
            addProperty("title", title)
            addProperty("body", body)
            addProperty("target_type", targetType)
            targetUserIds?.let {
                val arr = com.google.gson.JsonArray()
                it.forEach { id -> arr.add(id) }
                add("target_user_ids", arr)
            }
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/push/send")
            .header("Authorization", "Bearer $it")
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val msg = json.get("message")?.asString ?: "推送已发送"
            AuthResult.Success(msg)
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }






    suspend fun downloadBackup(projectId: Int, backupId: Int): AuthResult<String> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/backups/$backupId/download")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            val downloadUrl = data?.get("download_url")?.asString ?: data?.get("url")?.asString ?: ""
            AuthResult.Success(downloadUrl)
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }






    suspend fun getAnalyticsOverview(projectId: Int): AuthResult<JsonObject> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/analytics/overview")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            AuthResult.Success(json.getAsJsonObject("data") ?: JsonObject())
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun getAnalyticsTrend(projectId: Int, days: Int = 7): AuthResult<JsonObject> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/analytics/trend?days=$days")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            AuthResult.Success(json.getAsJsonObject("data") ?: JsonObject())
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun getAnalyticsGeo(projectId: Int): AuthResult<Map<String, Int>> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/analytics/geo")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            val map = data?.entrySet()?.associate { it.key to (it.value?.asInt ?: 0) } ?: emptyMap()
            AuthResult.Success(map)
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun getAnalyticsDevices(projectId: Int): AuthResult<Map<String, Int>> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/analytics/devices")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            val map = data?.entrySet()?.associate { it.key to (it.value?.asInt ?: 0) } ?: emptyMap()
            AuthResult.Success(map)
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun getAnalyticsVersions(projectId: Int): AuthResult<Map<String, Int>> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/analytics/versions")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            val map = data?.entrySet()?.associate { it.key to (it.value?.asInt ?: 0) } ?: emptyMap()
            AuthResult.Success(map)
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }






    suspend fun generateProjectCodes(projectId: Int, count: Int = 10, maxUses: Int = 1, prefix: String = ""): AuthResult<List<ProjectActivationCode>> = authRequest {
        val body = JsonObject().apply {
            addProperty("count", count)
            addProperty("max_uses", maxUses)
            if (prefix.isNotBlank()) addProperty("prefix", prefix)
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/codes/generate")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            val codesArr = data?.getAsJsonArray("codes")
            val codes = codesArr?.map { el ->
                val code = el.asString
                ProjectActivationCode(
                    id = 0, code = code, status = "unused",
                    maxUses = 1, usedCount = 0, deviceId = null,
                    createdAt = "", usedAt = null
                )
            } ?: emptyList()
            AuthResult.Success(codes)
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun listProjectCodes(projectId: Int, status: String? = null, page: Int = 1): AuthResult<List<ProjectActivationCode>> = authRequest {
        val url = buildString {
            append("$BASE_URL/api/v1/projects/$projectId/codes?page=$page")
            status?.let { append("&status=$it") }
        }
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val dataArr = json.getAsJsonArray("data") ?: return@authRequest AuthResult.Success(emptyList())
            AuthResult.Success(dataArr.map { parseActivationCode(it.asJsonObject) })
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }






    suspend fun createProjectAnnouncement(projectId: Int, title: String, content: String, priority: Int = 0): AuthResult<ProjectAnnouncement> = authRequest {
        val body = JsonObject().apply {
            addProperty("title", title)
            addProperty("content", content)
            addProperty("priority", priority)
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/announcements")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            AuthResult.Success(parseProjectAnnouncement(json.getAsJsonObject("data")))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun listProjectAnnouncements(projectId: Int): AuthResult<List<ProjectAnnouncement>> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/announcements")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val dataArr = json.getAsJsonArray("data") ?: return@authRequest AuthResult.Success(emptyList())
            AuthResult.Success(dataArr.map { parseProjectAnnouncement(it.asJsonObject) })
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun updateProjectAnnouncement(projectId: Int, annId: Int, title: String? = null, content: String? = null, isActive: Boolean? = null): AuthResult<String> = authRequest {
        val body = JsonObject().apply {
            title?.let { addProperty("title", it) }
            content?.let { addProperty("content", it) }
            isActive?.let { addProperty("is_active", it) }
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/announcements/$annId")
            .header("Authorization", "Bearer $it")
            .put(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) AuthResult.Success("更新成功")
        else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun deleteProjectAnnouncement(projectId: Int, annId: Int): AuthResult<String> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/announcements/$annId")
            .header("Authorization", "Bearer $it")
            .delete()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) AuthResult.Success("删除成功")
        else AuthResult.Error(parseError(responseBody, response.code))
    }






    suspend fun createProjectConfig(projectId: Int, key: String, value: String, description: String? = null): AuthResult<ProjectConfig> = authRequest {
        val body = JsonObject().apply {
            addProperty("config_key", key)
            addProperty("config_value", value)
            description?.let { addProperty("description", it) }
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/configs")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            AuthResult.Success(ProjectConfig(
                id = data?.get("id")?.asInt ?: 0,
                key = data?.get("config_key")?.asString ?: key,
                value = data?.get("config_value")?.asString ?: value,
                description = data?.get("description")?.asString,
                isActive = data?.get("is_active")?.asBoolean ?: true
            ))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun listProjectConfigs(projectId: Int): AuthResult<List<ProjectConfig>> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/configs")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val dataArr = json.getAsJsonArray("data") ?: return@authRequest AuthResult.Success(emptyList())
            AuthResult.Success(dataArr.map { el ->
                val obj = el.asJsonObject
                ProjectConfig(
                    id = obj.get("id")?.asInt ?: 0,
                    key = obj.get("config_key")?.asString ?: "",
                    value = obj.get("config_value")?.asString ?: "",
                    description = obj.get("description")?.asString,
                    isActive = obj.get("is_active")?.asBoolean ?: true
                )
            })
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun updateProjectConfig(projectId: Int, cfgId: Int, value: String? = null, description: String? = null, isActive: Boolean? = null): AuthResult<String> = authRequest {
        val body = JsonObject().apply {
            value?.let { addProperty("config_value", it) }
            description?.let { addProperty("description", it) }
            isActive?.let { addProperty("is_active", it) }
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/configs/$cfgId")
            .header("Authorization", "Bearer $it")
            .put(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) AuthResult.Success("更新成功")
        else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun deleteProjectConfig(projectId: Int, cfgId: Int): AuthResult<String> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/configs/$cfgId")
            .header("Authorization", "Bearer $it")
            .delete()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) AuthResult.Success("删除成功")
        else AuthResult.Error(parseError(responseBody, response.code))
    }






    suspend fun createWebhook(projectId: Int, webhookUrl: String, events: List<String>, secret: String? = null): AuthResult<ProjectWebhook> = authRequest {
        val body = JsonObject().apply {
            addProperty("url", webhookUrl)
            val eventsArray = com.google.gson.JsonArray()
            events.forEach { eventsArray.add(it) }
            add("events", eventsArray)
            secret?.let { addProperty("secret", it) }
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/webhooks")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            AuthResult.Success(parseWebhook(data))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun listWebhooks(projectId: Int): AuthResult<List<ProjectWebhook>> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/webhooks")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val dataArr = json.getAsJsonArray("data") ?: return@authRequest AuthResult.Success(emptyList())
            AuthResult.Success(dataArr.map { parseWebhook(it.asJsonObject) })
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun deleteWebhook(projectId: Int, webhookId: Int): AuthResult<String> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/webhooks/$webhookId")
            .header("Authorization", "Bearer $it")
            .delete()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) AuthResult.Success("Webhook 已删除")
        else AuthResult.Error(parseError(responseBody, response.code))
    }






    suspend fun verifySubscription(purchaseToken: String, productId: String): AuthResult<String> = authRequest {
        val body = JsonObject().apply {
            addProperty("purchase_token", purchaseToken)
            addProperty("product_id", productId)
            addProperty("platform", "android")
        }

        val request = Request.Builder()
            .url("$BASE_URL/api/v1/billing/verify")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            AuthResult.Success(json.get("message")?.asString ?: "订阅已激活")
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }






    suspend fun toggleModuleLike(moduleId: Int): AuthResult<LikeResponse> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/modules/$moduleId/like")
            .header("Authorization", "Bearer $it")
            .post("".toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            AuthResult.Success(LikeResponse(
                liked = data?.get("liked")?.asBoolean ?: false,
                likeCount = data?.get("like_count")?.asInt ?: 0
            ))
        } else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun getModuleLikeStatus(moduleId: Int): AuthResult<Boolean> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/modules/$moduleId/like/status")
            .header("Authorization", "Bearer $it")
            .get()
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            AuthResult.Success(data?.get("liked")?.asBoolean ?: false)
        } else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun voteModule(moduleId: Int, voteType: String = "up"): AuthResult<String> = authRequest {
        val body = JsonObject().apply { addProperty("vote_type", voteType) }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/community/modules/$moduleId/vote")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success("投票成功")
        else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun unvoteModule(moduleId: Int): AuthResult<String> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/community/modules/$moduleId/vote")
            .header("Authorization", "Bearer $it")
            .delete()
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success("已取消投票")
        else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun likeStoreModule(moduleId: Int): AuthResult<LikeResponse> = toggleModuleLike(moduleId)


    suspend fun addFavorite(moduleId: Int): AuthResult<String> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/community/modules/$moduleId/favorite")
            .header("Authorization", "Bearer $it")
            .post("".toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success("已收藏")
        else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun removeFavorite(moduleId: Int): AuthResult<String> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/community/modules/$moduleId/favorite")
            .header("Authorization", "Bearer $it")
            .delete()
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success("已取消收藏")
        else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun listFavorites(page: Int = 1, size: Int = 20): AuthResult<Pair<List<ModuleItem>, Int>> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/community/favorites?page=$page&size=$size")
            .header("Authorization", "Bearer $it")
            .get()
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            val total = data?.get("total")?.asInt ?: 0
            val modules = data?.getAsJsonArray("modules")?.map { parseStoreModule(it.asJsonObject) } ?: emptyList()
            AuthResult.Success(Pair(modules, total))
        } else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun addComment(moduleId: Int, content: String, parentId: Int? = null): AuthResult<String> = authRequest {
        val body = JsonObject().apply {
            addProperty("content", content)
            parentId?.let { id -> addProperty("parent_id", id) }
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/community/modules/$moduleId/comments")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success("评论已发布")
        else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun listComments(moduleId: Int, page: Int = 1, size: Int = 20): AuthResult<List<ModuleComment>> {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/community/modules/$moduleId/comments?page=$page&size=$size")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val arr = json.getAsJsonObject("data")?.getAsJsonArray("comments")
                val comments = arr?.map { c ->
                    val o = c.asJsonObject
                    ModuleComment(
                        id = o.get("id")?.asInt ?: 0,
                        content = o.get("content")?.asString ?: "",
                        userId = o.get("user_id")?.asInt ?: 0,
                        userName = o.get("user_name")?.asString ?: "",
                        userAvatar = o.get("user_avatar")?.asString,
                        parentId = o.get("parent_id")?.asInt,
                        createdAt = o.get("created_at")?.asString,
                        updatedAt = o.get("updated_at")?.asString
                    )
                } ?: emptyList()
                AuthResult.Success(comments)
            } else AuthResult.Error(parseError(responseBody, response.code))
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.message}")
        }
    }


    suspend fun deleteComment(commentId: Int): AuthResult<Unit> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/community/comments/$commentId")
            .header("Authorization", "Bearer $it")
            .delete()
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit)
        else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun reportModule(moduleId: Int, reason: String, details: String? = null): AuthResult<String> = authRequest {
        val body = JsonObject().apply {
            addProperty("module_id", moduleId)
            addProperty("reason", reason)
            details?.let { addProperty("details", it) }
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/community/reports")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success("举报已提交")
        else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun reportStoreModule(moduleId: Int, reason: String): AuthResult<Unit> = authRequest {
        val body = JsonObject().apply { addProperty("reason", reason) }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/modules/$moduleId/report")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit)
        else AuthResult.Error(parseError(responseBody, response.code))
    }






    suspend fun followUser(userId: Int): AuthResult<String> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/community/users/$userId/follow")
            .header("Authorization", "Bearer $it")
            .post("".toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success("已关注")
        else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun unfollowUser(userId: Int): AuthResult<String> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/community/users/$userId/follow")
            .header("Authorization", "Bearer $it")
            .delete()
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success("已取消关注")
        else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun getCommunityFeed(page: Int = 1, size: Int = 20): AuthResult<List<CommunityPostItem>> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/community/feed?page=$page&size=$size")
            .header("Authorization", "Bearer $it")
            .get()
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val dataArr = json.getAsJsonArray("data") ?: return@authRequest AuthResult.Success(emptyList())
            AuthResult.Success(dataArr.map { parseCommunityPost(it.asJsonObject) })
        } else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun getFollowingFeed(page: Int = 1, size: Int = 20): AuthResult<List<CommunityPostItem>> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/community/feed/following?page=$page&size=$size")
            .header("Authorization", "Bearer $it")
            .get()
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            val postsArr = data?.getAsJsonArray("posts") ?: return@authRequest AuthResult.Success(emptyList())
            AuthResult.Success(postsArr.map { parseCommunityPost(it.asJsonObject) })
        } else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun getTrendingFeed(page: Int = 1, size: Int = 20): AuthResult<List<CommunityPostItem>> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/community/feed/trending?page=$page&size=$size")
            .header("Authorization", "Bearer $it")
            .get()
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            val postsArr = data?.getAsJsonArray("posts") ?: return@authRequest AuthResult.Success(emptyList())
            AuthResult.Success(postsArr.map { parseCommunityPost(it.asJsonObject) })
        } else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun getUserFollowers(userId: Int, page: Int = 1, size: Int = 20): AuthResult<List<CommunityUserProfile>> {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/community/users/$userId/followers?page=$page&size=$size")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val data = json.getAsJsonObject("data")
                val usersArr = data?.getAsJsonArray("users") ?: return AuthResult.Success(emptyList())
                AuthResult.Success(usersArr.map { parseSimpleUserProfile(it.asJsonObject) })
            } else AuthResult.Error(parseError(responseBody, response.code))
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.message}")
        }
    }


    suspend fun getUserFollowing(userId: Int, page: Int = 1, size: Int = 20): AuthResult<List<CommunityUserProfile>> {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/community/users/$userId/following?page=$page&size=$size")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val data = json.getAsJsonObject("data")
                val usersArr = data?.getAsJsonArray("users") ?: return AuthResult.Success(emptyList())
                AuthResult.Success(usersArr.map { parseSimpleUserProfile(it.asJsonObject) })
            } else AuthResult.Error(parseError(responseBody, response.code))
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.message}")
        }
    }


    suspend fun searchUsers(query: String, page: Int = 1, size: Int = 20): AuthResult<List<CommunityUserProfile>> {
        return try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/community/users/search?q=$encodedQuery&page=$page&size=$size")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val data = json.getAsJsonObject("data")
                val usersArr = data?.getAsJsonArray("users") ?: return AuthResult.Success(emptyList())
                AuthResult.Success(usersArr.map { parseSimpleUserProfile(it.asJsonObject) })
            } else AuthResult.Error(parseError(responseBody, response.code))
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.message}")
        }
    }


    suspend fun getTrendingModules(page: Int = 1, size: Int = 20): AuthResult<List<StoreModuleInfo>> {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/community/modules/trending?page=$page&size=$size")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val dataArr = json.getAsJsonArray("data") ?: return AuthResult.Success(emptyList())
                AuthResult.Success(dataArr.map { parseStoreModule(it.asJsonObject) })
            } else AuthResult.Error(parseError(responseBody, response.code))
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.message}")
        }
    }


    suspend fun getFeaturedModules(page: Int = 1, size: Int = 20): AuthResult<List<StoreModuleInfo>> {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/community/modules/featured?page=$page&size=$size")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val dataArr = json.getAsJsonArray("data") ?: return AuthResult.Success(emptyList())
                AuthResult.Success(dataArr.map { parseStoreModule(it.asJsonObject) })
            } else AuthResult.Error(parseError(responseBody, response.code))
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.message}")
        }
    }


    suspend fun getUserProfile(userId: Int): AuthResult<CommunityUserProfile> =
        withContext(Dispatchers.IO) {
            try {
                val requestBuilder = Request.Builder()
                    .url("$BASE_URL/api/v1/community/users/$userId")
                    .get()

                tokenManager.getAccessToken()?.let { requestBuilder.header("Authorization", "Bearer $it") }
                val response = client.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JsonParser.parseString(responseBody).asJsonObject
                    val d = json.getAsJsonObject("data")
                    AuthResult.Success(CommunityUserProfile(
                        id = d?.get("id")?.asInt ?: 0,
                        username = d?.get("username")?.asString ?: "",
                        displayName = d?.get("display_name")?.let { if (it.isJsonNull) null else it.asString },
                        avatarUrl = d?.get("avatar_url")?.let { if (it.isJsonNull) null else it.asString },
                        bio = d?.get("bio")?.let { if (it.isJsonNull) null else it.asString },
                        appCount = d?.get("published_apps_count")?.asInt ?: 0,
                        moduleCount = d?.get("published_modules_count")?.asInt ?: d?.get("module_count")?.asInt ?: 0,
                        followerCount = d?.get("follower_count")?.asInt ?: 0,
                        followingCount = d?.get("following_count")?.asInt ?: 0,
                        isFollowing = d?.get("is_following")?.asBoolean ?: false,
                        isDeveloper = d?.get("is_developer")?.asBoolean ?: false,
                        createdAt = d?.get("created_at")?.let { if (it.isJsonNull) null else it.asString }
                    ))
                } else AuthResult.Error(parseError(responseBody, response.code))
            } catch (e: Exception) {
                AuthResult.Error("网络错误: ${e.message}")
            }
        }






    suspend fun listNotifications(page: Int = 1, size: Int = 20, unreadOnly: Boolean = false): AuthResult<Pair<List<NotificationItem>, Int>> = authRequest {
        val params = "?page=$page&size=$size" + if (unreadOnly) "&unread_only=true" else ""
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/notifications$params")
            .header("Authorization", "Bearer $it")
            .get()
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            val unreadCount = data?.get("unread_count")?.asInt ?: 0
            val notifications = data?.getAsJsonArray("notifications")?.map { n ->
                val o = n.asJsonObject
                NotificationItem(
                    id = o.get("id")?.asInt ?: 0,
                    type = o.get("type")?.asString ?: "",
                    title = o.get("title")?.asString,
                    content = o.get("content")?.asString,
                    refType = o.get("ref_type")?.asString,
                    refId = o.get("ref_id")?.asInt,
                    actorId = o.get("actor_id")?.asInt,
                    isRead = o.get("is_read")?.asBoolean ?: false,
                    createdAt = o.get("created_at")?.asString
                )
            } ?: emptyList()
            AuthResult.Success(Pair(notifications, unreadCount))
        } else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun getUnreadNotificationCount(): AuthResult<Int> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/notifications/unread-count")
            .header("Authorization", "Bearer $it")
            .get()
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val count = json.getAsJsonObject("data")?.get("unread_count")?.asInt ?: 0
            AuthResult.Success(count)
        } else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun markNotificationRead(notificationId: Int): AuthResult<Unit> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/notifications/read/$notificationId")
            .header("Authorization", "Bearer $it")
            .post("".toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit)
        else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun markAllNotificationsRead(): AuthResult<Unit> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/notifications/read-all")
            .header("Authorization", "Bearer $it")
            .post("".toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit)
        else AuthResult.Error(parseError(responseBody, response.code))
    }






    private val refreshMutex = Mutex()


    private suspend fun <T> authRequest(block: suspend (token: String) -> AuthResult<T>): AuthResult<T> =
        withContext(Dispatchers.IO) {
            try {
                val token = tokenManager.getAccessToken()
                    ?: return@withContext AuthResult.Error("未登录，请先登录")
                val result = block(token)

                if (result is AuthResult.Error && result.message.contains("HTTP 401")) {

                    refreshMutex.withLock {

                        val currentToken = tokenManager.getAccessToken()
                        if (currentToken != null && currentToken != token) {

                            return@withContext block(currentToken)
                        }
                        val refresh = tokenManager.getRefreshToken()
                            ?: return@withContext AuthResult.Error("登录已过期，请重新登录")
                        val refreshBody = JsonObject().apply { addProperty("refresh_token", refresh) }
                        val refreshReq = Request.Builder()
                            .url("$BASE_URL/api/v1/auth/refresh")
                            .post(refreshBody.toString().toRequestBody(jsonMediaType))
                            .build()
                        val refreshResp = client.newCall(refreshReq).execute()
                        if (refreshResp.isSuccessful) {
                            val rJson = JsonParser.parseString(refreshResp.body?.string() ?: "").asJsonObject
                            val rData = rJson.getAsJsonObject("data")
                            val newToken = rData.get("access_token").asString
                            tokenManager.saveTokens(
                                newToken,
                                rData.get("refresh_token").asString
                            )
                            block(newToken)
                        } else {
                            tokenManager.clearTokens()
                            AuthResult.Error("登录已过期，请重新登录")
                        }
                    }
                } else result
            } catch (e: Exception) {
                AppLogger.e(TAG, "API request failed", e)
                AuthResult.Error("网络连接失败: ${e.message}")
            }
        }

    suspend fun listEcosystemFeed(
        type: String = "all",
        sort: String = "latest",
        page: Int = 1,
        size: Int = 20,
        search: String? = null
    ): AuthResult<EcosystemFeedResponse> = withContext(Dispatchers.IO) {
        try {
            val query = mutableListOf(
                "type=${urlEncode(type)}",
                "sort=${urlEncode(sort)}",
                "page=$page",
                "size=$size"
            )
            if (!search.isNullOrBlank()) query += "search=${urlEncode(search)}"
            val requestBuilder = Request.Builder()
                .url("$BASE_URL/api/v2/ecosystem/feed?${query.joinToString("&")}")
                .get()
            tokenManager.getAccessToken()?.let { requestBuilder.header("Authorization", "Bearer $it") }
            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val data = JsonParser.parseString(responseBody).asJsonObject.getAsJsonObject("data")
                AuthResult.Success(parseEcosystemFeed(data))
            } else {
                AuthResult.Error(parseError(responseBody, response.code))
            }
        } catch (e: Exception) {
            AuthResult.Error("网络错误: ${e.message}")
        }
    }

    suspend fun getEcosystemItem(type: String, id: Int): AuthResult<EcosystemItem> =
        withContext(Dispatchers.IO) {
            try {
                val requestBuilder = Request.Builder()
                    .url("$BASE_URL/api/v2/ecosystem/items/${urlEncode(type)}/$id")
                    .get()
                tokenManager.getAccessToken()?.let { requestBuilder.header("Authorization", "Bearer $it") }
                val response = client.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val data = JsonParser.parseString(responseBody).asJsonObject.getAsJsonObject("data")
                    AuthResult.Success(parseEcosystemItem(data))
                } else {
                    AuthResult.Error(parseError(responseBody, response.code))
                }
            } catch (e: Exception) {
                AuthResult.Error("网络错误: ${e.message}")
            }
        }

    suspend fun getEcosystemUser(userId: Int): AuthResult<EcosystemUserContent> =
        withContext(Dispatchers.IO) {
            try {
                val requestBuilder = Request.Builder()
                    .url("$BASE_URL/api/v2/ecosystem/users/$userId")
                    .get()
                tokenManager.getAccessToken()?.let { requestBuilder.header("Authorization", "Bearer $it") }
                val response = client.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val data = JsonParser.parseString(responseBody).asJsonObject.getAsJsonObject("data")
                    AuthResult.Success(parseEcosystemUser(data))
                } else {
                    AuthResult.Error(parseError(responseBody, response.code))
                }
            } catch (e: Exception) {
                AuthResult.Error("网络错误: ${e.message}")
            }
        }

    suspend fun createEcosystemPost(
        content: String,
        title: String? = null,
        tags: List<String> = emptyList(),
        media: List<EcosystemMedia> = emptyList()
    ): AuthResult<EcosystemItem> =
        authRequest { token ->
            val body = JsonObject().apply {
                addProperty("content", content)
                title?.let { addProperty("title", it) }
                add("tags", com.google.gson.JsonArray().apply { tags.forEach { add(it) } })
                add("media", com.google.gson.JsonArray().apply {
                    media.forEach { item ->
                        add(JsonObject().apply {
                            addProperty("type", item.type)
                            item.url?.let { addProperty("url", it) }
                            item.thumbnailUrl?.let { addProperty("thumbnail_url", it) }
                        })
                    }
                })
            }
            val request = Request.Builder()
                .url("$BASE_URL/api/v2/ecosystem/posts")
                .header("Authorization", "Bearer $token")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val data = JsonParser.parseString(responseBody).asJsonObject.getAsJsonObject("data")
                AuthResult.Success(parseEcosystemItem(data))
            } else {
                AuthResult.Error(parseError(responseBody, response.code))
            }
        }

    suspend fun listEcosystemComments(type: String, id: Int, page: Int = 1, size: Int = 30): AuthResult<EcosystemCommentsResponse> =
        withContext(Dispatchers.IO) {
            try {
                val requestBuilder = Request.Builder()
                    .url("$BASE_URL/api/v2/ecosystem/items/${urlEncode(type)}/$id/comments?page=$page&size=$size")
                    .get()
                tokenManager.getAccessToken()?.let { requestBuilder.header("Authorization", "Bearer $it") }
                val response = client.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val data = JsonParser.parseString(responseBody).asJsonObject.getAsJsonObject("data")
                    AuthResult.Success(parseEcosystemComments(data))
                } else {
                    AuthResult.Error(parseError(responseBody, response.code))
                }
            } catch (e: Exception) {
                AuthResult.Error("网络错误: ${e.message}")
            }
        }

    suspend fun listEcosystemBookmarks(type: String = "all", page: Int = 1, size: Int = 20): AuthResult<EcosystemFeedResponse> =
        authRequest { token ->
            val request = Request.Builder()
                .url("$BASE_URL/api/v2/ecosystem/bookmarks?type=${urlEncode(type)}&page=$page&size=$size")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val data = JsonParser.parseString(responseBody).asJsonObject.getAsJsonObject("data")
                AuthResult.Success(parseEcosystemFeed(data))
            } else {
                AuthResult.Error(parseError(responseBody, response.code))
            }
        }

    suspend fun listEcosystemNotifications(page: Int = 1, size: Int = 20, unreadOnly: Boolean = false): AuthResult<EcosystemNotificationsResponse> =
        authRequest { token ->
            val params = "?page=$page&size=$size" + if (unreadOnly) "&unread_only=true" else ""
            val request = Request.Builder()
                .url("$BASE_URL/api/v2/ecosystem/notifications$params")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val data = JsonParser.parseString(responseBody).asJsonObject.getAsJsonObject("data")
                AuthResult.Success(parseEcosystemNotifications(data))
            } else {
                AuthResult.Error(parseError(responseBody, response.code))
            }
        }

    suspend fun markEcosystemNotificationRead(notificationId: Int): AuthResult<Unit> =
        authRequest { token ->
            val request = Request.Builder()
                .url("$BASE_URL/api/v2/ecosystem/notifications/$notificationId/read")
                .header("Authorization", "Bearer $token")
                .post("".toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) AuthResult.Success(Unit) else AuthResult.Error(parseError(responseBody, response.code))
        }

    suspend fun markAllEcosystemNotificationsRead(): AuthResult<Unit> =
        authRequest { token ->
            val request = Request.Builder()
                .url("$BASE_URL/api/v2/ecosystem/notifications/read-all")
                .header("Authorization", "Bearer $token")
                .post("".toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) AuthResult.Success(Unit) else AuthResult.Error(parseError(responseBody, response.code))
        }

    suspend fun addEcosystemComment(type: String, id: Int, content: String, parentId: Int? = null): AuthResult<EcosystemComment> =
        authRequest { token ->
            val body = JsonObject().apply {
                addProperty("content", content)
                parentId?.let { addProperty("parent_id", it) }
            }
            val request = Request.Builder()
                .url("$BASE_URL/api/v2/ecosystem/items/${urlEncode(type)}/$id/comments")
                .header("Authorization", "Bearer $token")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val data = JsonParser.parseString(responseBody).asJsonObject.getAsJsonObject("data")
                AuthResult.Success(parseEcosystemComment(data))
            } else {
                AuthResult.Error(parseError(responseBody, response.code))
            }
        }

    suspend fun toggleEcosystemLike(type: String, id: Int): AuthResult<EcosystemToggleResult> = authRequest { token ->
        val request = Request.Builder()
            .url("$BASE_URL/api/v2/ecosystem/items/${urlEncode(type)}/$id/like")
            .header("Authorization", "Bearer $token")
            .post("".toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val data = JsonParser.parseString(responseBody).asJsonObject.getAsJsonObject("data")
            AuthResult.Success(EcosystemToggleResult(
                active = data?.get("liked")?.asBoolean ?: false,
                count = data?.get("like_count")?.asInt ?: 0
            ))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }

    suspend fun toggleEcosystemBookmark(type: String, id: Int): AuthResult<EcosystemToggleResult> = authRequest { token ->
        val request = Request.Builder()
            .url("$BASE_URL/api/v2/ecosystem/items/${urlEncode(type)}/$id/bookmark")
            .header("Authorization", "Bearer $token")
            .post("".toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val data = JsonParser.parseString(responseBody).asJsonObject.getAsJsonObject("data")
            AuthResult.Success(EcosystemToggleResult(active = data?.get("bookmarked")?.asBoolean ?: false))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }

    suspend fun reportEcosystemItem(type: String, id: Int, reason: String, description: String? = null): AuthResult<Unit> =
        authRequest { token ->
            val body = JsonObject().apply {
                addProperty("reason", reason)
                description?.let { addProperty("description", it) }
            }
            val request = Request.Builder()
                .url("$BASE_URL/api/v2/ecosystem/items/${urlEncode(type)}/$id/report")
                .header("Authorization", "Bearer $token")
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) AuthResult.Success(Unit) else AuthResult.Error(parseError(responseBody, response.code))
        }

    private fun urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun parseEcosystemFeed(obj: JsonObject?): EcosystemFeedResponse {
        val items = obj?.getAsJsonArray("items")?.map { parseEcosystemItem(it.asJsonObject) } ?: emptyList()
        return EcosystemFeedResponse(
            items = items,
            total = obj?.get("total")?.asInt ?: items.size,
            page = obj?.get("page")?.asInt ?: 1,
            size = obj?.get("size")?.asInt ?: items.size
        )
    }

    private fun parseEcosystemItem(obj: JsonObject): EcosystemItem {
        val media = obj.getAsJsonArray("media")?.map { mediaEl ->
            val mo = mediaEl.asJsonObject
            EcosystemMedia(
                id = mo.get("id")?.asInt ?: 0,
                type = mo.get("type")?.let { if (it.isJsonNull) "image" else it.asString } ?: "image",
                url = mo.get("url")?.let { if (it.isJsonNull) null else it.asString },
                thumbnailUrl = mo.get("thumbnail_url")?.let { if (it.isJsonNull) null else it.asString }
            )
        } ?: emptyList()
        val authorObj = obj.getAsJsonObject("author")
        val statsObj = obj.getAsJsonObject("stats")
        val viewerObj = obj.getAsJsonObject("viewer_state")
        val metaObj = obj.getAsJsonObject("meta")
        val meta = metaObj?.entrySet()?.associate { (key, value) ->
            key to if (value == null || value.isJsonNull) null else value.asString
        } ?: emptyMap()
        return EcosystemItem(
            type = obj.get("type")?.asString ?: "post",
            id = obj.get("id")?.asInt ?: 0,
            title = obj.get("title")?.let { if (it.isJsonNull) "" else it.asString } ?: "",
            summary = obj.get("summary")?.let { if (it.isJsonNull) "" else it.asString } ?: "",
            content = obj.get("content")?.let { if (it.isJsonNull) "" else it.asString } ?: "",
            category = obj.get("category")?.let { if (it.isJsonNull) "other" else it.asString } ?: "other",
            tags = obj.getAsJsonArray("tags")?.map { it.asString } ?: emptyList(),
            icon = obj.get("icon")?.let { if (it.isJsonNull) null else it.asString },
            media = media,
            author = parseEcosystemAuthor(authorObj),
            stats = EcosystemStats(
                views = statsObj?.get("views")?.asInt ?: 0,
                likes = statsObj?.get("likes")?.asInt ?: 0,
                comments = statsObj?.get("comments")?.asInt ?: 0,
                downloads = statsObj?.get("downloads")?.asInt ?: 0,
                rating = statsObj?.get("rating")?.asFloat ?: 0f,
                ratingCount = statsObj?.get("rating_count")?.asInt ?: 0
            ),
            viewerState = EcosystemViewerState(
                liked = viewerObj?.get("liked")?.asBoolean ?: false,
                bookmarked = viewerObj?.get("bookmarked")?.asBoolean ?: false,
                own = viewerObj?.get("own")?.asBoolean ?: false
            ),
            meta = meta,
            createdAt = obj.get("created_at")?.let { if (it.isJsonNull) null else it.asString },
            updatedAt = obj.get("updated_at")?.let { if (it.isJsonNull) null else it.asString }
        )
    }

    private fun parseEcosystemAuthor(obj: JsonObject?): EcosystemAuthor = EcosystemAuthor(
        id = obj?.get("id")?.asInt ?: 0,
        username = obj?.get("username")?.let { if (it.isJsonNull) "?" else it.asString } ?: "?",
        displayName = obj?.get("display_name")?.let { if (it.isJsonNull) null else it.asString },
        avatarUrl = obj?.get("avatar_url")?.let { if (it.isJsonNull) null else it.asString },
        isDeveloper = obj?.get("is_developer")?.asBoolean ?: false
    )

    private fun parseEcosystemComments(obj: JsonObject?): EcosystemCommentsResponse {
        val comments = obj?.getAsJsonArray("comments")?.map { parseEcosystemComment(it.asJsonObject) } ?: emptyList()
        return EcosystemCommentsResponse(
            comments = comments,
            total = obj?.get("total")?.asInt ?: comments.size,
            page = obj?.get("page")?.asInt ?: 1,
            size = obj?.get("size")?.asInt ?: comments.size
        )
    }

    private fun parseEcosystemNotifications(obj: JsonObject?): EcosystemNotificationsResponse {
        val notifications = obj?.getAsJsonArray("notifications")?.map { item ->
            val o = item.asJsonObject
            EcosystemNotification(
                id = o.get("id")?.asInt ?: 0,
                type = o.get("type")?.let { if (it.isJsonNull) "" else it.asString } ?: "",
                title = o.get("title")?.let { if (it.isJsonNull) null else it.asString },
                content = o.get("content")?.let { if (it.isJsonNull) null else it.asString },
                refType = o.get("ref_type")?.let { if (it.isJsonNull) null else it.asString },
                refId = o.get("ref_id")?.let { if (it.isJsonNull) null else it.asInt },
                actor = parseEcosystemAuthor(o.getAsJsonObject("actor")),
                isRead = o.get("is_read")?.asBoolean ?: false,
                createdAt = o.get("created_at")?.let { if (it.isJsonNull) null else it.asString }
            )
        } ?: emptyList()
        return EcosystemNotificationsResponse(
            notifications = notifications,
            unreadCount = obj?.get("unread_count")?.asInt ?: 0,
            total = obj?.get("total")?.asInt ?: notifications.size,
            page = obj?.get("page")?.asInt ?: 1,
            size = obj?.get("size")?.asInt ?: notifications.size
        )
    }

    private fun parseEcosystemUser(obj: JsonObject?): EcosystemUserContent {
        val profileObj = obj?.getAsJsonObject("profile")
        return EcosystemUserContent(
            profile = EcosystemProfile(
                id = profileObj?.get("id")?.asInt ?: 0,
                username = profileObj?.get("username")?.let { if (it.isJsonNull) "?" else it.asString } ?: "?",
                displayName = profileObj?.get("display_name")?.let { if (it.isJsonNull) null else it.asString },
                avatarUrl = profileObj?.get("avatar_url")?.let { if (it.isJsonNull) null else it.asString },
                bio = profileObj?.get("bio")?.let { if (it.isJsonNull) null else it.asString },
                followerCount = profileObj?.get("follower_count")?.asInt ?: 0,
                followingCount = profileObj?.get("following_count")?.asInt ?: 0
            ),
            posts = obj?.getAsJsonArray("posts")?.map { parseEcosystemItem(it.asJsonObject) } ?: emptyList(),
            apps = obj?.getAsJsonArray("apps")?.map { parseEcosystemItem(it.asJsonObject) } ?: emptyList(),
            modules = obj?.getAsJsonArray("modules")?.map { parseEcosystemItem(it.asJsonObject) } ?: emptyList()
        )
    }

    private fun parseEcosystemComment(obj: JsonObject): EcosystemComment {
        val replies = obj.getAsJsonArray("replies")?.map { parseEcosystemComment(it.asJsonObject) } ?: emptyList()
        return EcosystemComment(
            id = obj.get("id")?.asInt ?: 0,
            content = obj.get("content")?.let { if (it.isJsonNull) "" else it.asString } ?: "",
            parentId = obj.get("parent_id")?.let { if (it.isJsonNull) null else it.asInt },
            likeCount = obj.get("like_count")?.asInt ?: 0,
            author = parseEcosystemAuthor(obj.getAsJsonObject("author")),
            createdAt = obj.get("created_at")?.let { if (it.isJsonNull) null else it.asString },
            replies = replies
        )
    }

    private fun parseProject(obj: JsonObject): CloudProject = CloudProject(
        id = obj.get("id")?.asInt ?: 0,
        name = obj.get("project_name")?.asString ?: obj.get("name")?.asString ?: "",
        description = obj.get("description")?.asString,
        projectKey = obj.get("project_key")?.asString ?: "",
        packageName = obj.get("package_name")?.asString,
        githubRepo = obj.get("github_repo")?.asString,
        giteeRepo = obj.get("gitee_repo")?.asString,
        createdAt = obj.get("created_at")?.asString,
        isActive = obj.get("is_active")?.asBoolean ?: true,
        totalInstalls = obj.get("total_installs")?.asInt ?: 0,
        totalOpens = obj.get("total_opens")?.asInt ?: 0
    )

    private fun parseActivationCode(obj: JsonObject): ProjectActivationCode = ProjectActivationCode(
        id = obj.get("id")?.asInt ?: 0,
        code = obj.get("code")?.asString ?: "",
        status = obj.get("status")?.asString ?: "unused",
        maxUses = obj.get("max_uses")?.asInt ?: 1,
        usedCount = obj.get("used_count")?.asInt ?: 0,
        deviceId = obj.get("device_id")?.asString,
        createdAt = obj.get("created_at")?.asString ?: "",
        usedAt = obj.get("used_at")?.asString
    )

    private fun parseProjectAnnouncement(obj: JsonObject?): ProjectAnnouncement = ProjectAnnouncement(
        id = obj?.get("id")?.asInt ?: 0,
        title = obj?.get("title")?.asString ?: "",
        content = obj?.get("content")?.asString ?: "",
        isActive = obj?.get("is_active")?.asBoolean ?: true,
        priority = obj?.get("priority")?.asInt ?: 0,
        createdAt = obj?.get("created_at")?.asString ?: ""
    )

    private fun parseWebhook(obj: JsonObject?): ProjectWebhook = ProjectWebhook(
        id = obj?.get("id")?.asInt ?: 0,
        url = obj?.get("url")?.asString ?: "",
        events = obj?.getAsJsonArray("events")?.map { it.asString } ?: emptyList(),
        secret = obj?.get("secret")?.asString,
        isActive = obj?.get("is_active")?.asBoolean ?: true,
        failureCount = obj?.get("failure_count")?.asInt ?: 0,
        lastTriggeredAt = obj?.get("last_triggered_at")?.asString
    )






    suspend fun uploadManifest(projectId: Int, manifestJson: String, manifestVersion: Int): AuthResult<ManifestSyncResult> = authRequest {
        val body = JsonObject().apply {
            add("manifest", JsonParser.parseString(manifestJson))
            addProperty("manifest_version", manifestVersion)
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/manifest")
            .header("Authorization", "Bearer $it")
            .put(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            val conflict = data?.get("conflict")?.asBoolean ?: false
            AuthResult.Success(ManifestSyncResult(
                success = !conflict,
                manifestVersion = data?.get("manifest_version")?.asInt ?: data?.get("server_version")?.asInt ?: 0,
                syncedAt = data?.get("synced_at")?.asString,
                conflict = conflict
            ))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun downloadManifest(projectId: Int): AuthResult<ManifestDownloadResult> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/manifest")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            val manifestObj = data?.get("manifest")
            AuthResult.Success(ManifestDownloadResult(
                manifestJson = if (manifestObj != null && !manifestObj.isJsonNull) manifestObj.toString() else null,
                manifestVersion = data?.get("manifest_version")?.asInt ?: 0,
                syncedAt = data?.get("synced_at")?.asString
            ))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }






    suspend fun listStoreModules(
        category: String? = null, search: String? = null,
        sort: String = "downloads", order: String = "desc",
        page: Int = 1, size: Int = 20
    ): AuthResult<Pair<List<StoreModuleInfo>, Int>> = withContext(Dispatchers.IO) {
        try {
            val params = buildString {
                append("?page=$page&size=$size&sort=$sort&order=$order")
                if (category != null) append("&category=$category")
                if (search != null) append("&search=$search")
            }
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/modules$params")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val data = json.getAsJsonObject("data")
                val total = data?.get("total")?.asInt ?: 0
                val modules = data?.getAsJsonArray("modules")?.map { parseStoreModule(it.asJsonObject) } ?: emptyList()
                AuthResult.Success(Pair(modules, total))
            } else {
                AuthResult.Error(parseError(responseBody, response.code))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "listStoreModules failed", e)
            AuthResult.Error("Network error: ${e.message}")
        }
    }


    suspend fun getStoreModuleById(moduleId: Int): AuthResult<StoreModuleInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/modules/$moduleId")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val data = json.getAsJsonObject("data")
                AuthResult.Success(parseStoreModule(data))
            } else {
                AuthResult.Error(parseError(responseBody, response.code))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "getStoreModuleById failed", e)
            AuthResult.Error("Network error: ${e.message}")
        }
    }


    suspend fun getMyPublishedModules(page: Int = 1, size: Int = 50): AuthResult<Pair<List<StoreModuleInfo>, Int>> = withContext(Dispatchers.IO) {
        try {
            val token = tokenManager.getAccessToken()
                ?: return@withContext AuthResult.Error("Not logged in")
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/community/me/modules?page=$page&size=$size")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val data = json.getAsJsonObject("data")
                val total = data?.get("total")?.asInt ?: 0
                val modules = data?.getAsJsonArray("modules")?.map { parseStoreModule(it.asJsonObject) } ?: emptyList()
                AuthResult.Success(Pair(modules, total))
            } else {
                AuthResult.Error(parseError(responseBody, response.code))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "getMyPublishedModules failed", e)
            AuthResult.Error("Network error: ${e.message}")
        }
    }


    suspend fun publishModule(
        name: String, description: String, icon: String?,
        category: String?, tags: String?, versionName: String?,
        versionCode: Int, shareCode: String,
        visibility: String = "public",
        reviewMode: String = "public_first",
        screenshots: List<String> = emptyList()
    ): AuthResult<StoreModuleInfo> = authRequest {
        val body = JsonObject().apply {
            addProperty("name", name)
            addProperty("description", description)
            addProperty("icon", icon)
            addProperty("category", category)
            addProperty("tags", tags)
            addProperty("version_name", versionName)
            addProperty("version_code", versionCode)
            addProperty("share_code", shareCode)
            addProperty("visibility", visibility)
            addProperty("review_mode", reviewMode)
            val screenshotsArray = com.google.gson.JsonArray()
            screenshots.forEach { screenshotsArray.add(it) }
            add("screenshots", screenshotsArray)
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/modules")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            AuthResult.Success(parseStoreModule(json.getAsJsonObject("data")))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }








    suspend fun downloadStoreModule(moduleId: Int): AuthResult<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/modules/$moduleId/download")
                .post("".toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val data = json.getAsJsonObject("data")
                val storageUrlGithub = data?.get("storage_url_github")?.let { if (it.isJsonNull) null else it.asString }
                val storageUrlGitee = data?.get("storage_url_gitee")?.let { if (it.isJsonNull) null else it.asString }
                val shareCode = data?.get("share_code")?.let { if (it.isJsonNull) null else it.asString }


                val downloadedCode = downloadGzipModule(GitHubAccelerator.accelerate(storageUrlGithub))
                    ?: downloadGzipModule(storageUrlGitee)
                    ?: shareCode

                if (downloadedCode.isNullOrBlank()) {
                    AuthResult.Error("模块数据为空")
                } else {
                    AuthResult.Success(downloadedCode)
                }
            } else {
                AuthResult.Error(parseError(responseBody, response.code))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "downloadStoreModule failed", e)
            AuthResult.Error("Network error: ${e.message}")
        }
    }


    private fun downloadGzipModule(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return try {
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bytes = response.body?.bytes() ?: return null

                if (bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()) {
                    java.util.zip.GZIPInputStream(java.io.ByteArrayInputStream(bytes)).use { gzis ->
                        gzis.bufferedReader(Charsets.UTF_8).readText()
                    }
                } else {

                    String(bytes, Charsets.UTF_8)
                }
            } else null
        } catch (e: Exception) {
            AppLogger.e(TAG, "Download gzip module failed from $url", e)
            null
        }
    }

    private fun parseStoreModule(obj: JsonObject?): ModuleItem = ModuleItem(
        id = obj?.get("id")?.asInt ?: 0,
        name = obj?.get("name")?.let { if (it.isJsonNull) "" else it.asString } ?: "",
        description = obj?.get("description")?.let { if (it.isJsonNull) null else it.asString },
        icon = obj?.get("icon")?.let { if (it.isJsonNull) null else it.asString },
        category = obj?.get("category")?.let { if (it.isJsonNull) null else it.asString },
        tags = try { obj?.getAsJsonArray("tags")?.map { it.asString } ?: emptyList() } catch (_: Exception) { emptyList() },
        versionName = obj?.get("version_name")?.let { if (it.isJsonNull) null else it.asString },
        versionCode = obj?.get("version_code")?.asInt ?: 0,
        downloads = obj?.get("downloads")?.asInt ?: 0,
        rating = obj?.get("rating")?.asFloat ?: 0f,
        ratingCount = obj?.get("rating_count")?.asInt ?: 0,
        likeCount = obj?.get("like_count")?.asInt ?: 0,
        isFeatured = obj?.get("is_featured")?.asBoolean ?: false,
        authorName = obj?.get("author_name")?.let { if (it.isJsonNull) "" else it.asString }
            ?: obj?.getAsJsonObject("author")?.get("username")?.let { if (it.isJsonNull) "" else it.asString }
            ?: "",
        authorId = obj?.get("author_id")?.asInt
            ?: obj?.getAsJsonObject("author")?.get("id")?.asInt
            ?: 0,
        shareCode = obj?.get("share_code")?.let { if (it.isJsonNull) null else it.asString },
        moduleType = obj?.get("module_type")?.let { if (it.isJsonNull) "extension" else it.asString } ?: "extension",
        isApproved = obj?.boolValue("isApproved", "is_approved") ?: true,
        visibility = obj?.stringValue("visibility"),
        reviewMode = obj?.stringValue("review_mode", "reviewMode"),
        reviewStatus = obj?.stringValue("review_status", "reviewStatus"),
        verificationStatus = obj?.stringValue("verification_status", "verificationStatus"),
        reviewSortPenalty = obj?.floatValue("review_sort_penalty", "reviewSortPenalty") ?: 0f,
        rejectionReason = obj?.stringValue("rejection_reason", "rejectionReason"),
        adminReplyText = obj?.stringValue("admin_reply_text", "adminReplyText"),
        runtimeKey = obj?.stringValue("runtime_key", "runtimeKey"),
        screenshots = try { obj?.getAsJsonArray("screenshots")?.map { it.asString } ?: emptyList() } catch (_: Exception) { emptyList() },

        userVote = obj?.get("user_vote")?.let { if (it.isJsonNull) null else it.asString },
        isFavorited = obj?.get("is_favorited")?.asBoolean ?: false,
        isLiked = obj?.get("is_liked")?.asBoolean ?: false,
        createdAt = obj?.get("created_at")?.let { if (it.isJsonNull) null else it.asString },
        updatedAt = obj?.get("updated_at")?.let { if (it.isJsonNull) null else it.asString }
    )




    suspend fun reviewStoreModule(moduleId: Int, rating: Int, comment: String? = null): AuthResult<Unit> = authRequest {
        val body = JsonObject().apply {
            addProperty("rating", rating)
            comment?.let { addProperty("comment", it) }
            addProperty("device_model", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/modules/$moduleId/review")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit)
        else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun getModuleReviews(moduleId: Int, page: Int = 1, size: Int = 20): Result<AppReviewsResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/modules/$moduleId/reviews?page=$page&size=$size")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val data = json.getAsJsonObject("data")
                val reviews = data?.getAsJsonArray("reviews")?.map { el ->
                    val r = el.asJsonObject
                    AppReviewItem(
                        id = r.get("id")?.asInt ?: 0,
                        rating = r.get("rating")?.asInt ?: 0,
                        comment = r.get("comment")?.let { if (it.isJsonNull) null else it.asString },
                        authorName = r.get("author_name")?.asString ?: "Unknown",
                        authorId = r.get("author_id")?.asInt ?: 0,
                        deviceModel = r.get("device_model")?.let { if (it.isJsonNull) null else it.asString },
                        ipAddress = r.get("ip_address")?.let { if (it.isJsonNull) null else it.asString },
                        createdAt = r.get("created_at")?.let { if (it.isJsonNull) null else it.asString }
                    )
                } ?: emptyList()

                val ratingDist = mutableMapOf<Int, Int>()
                data?.getAsJsonObject("rating_distribution")?.let { distObj ->
                    for (star in 1..5) {
                        distObj.get(star.toString())?.asInt?.let { ratingDist[star] = it }
                    }
                }

                if (ratingDist.isEmpty() && reviews.isNotEmpty()) {
                    reviews.groupingBy { it.rating }.eachCount().also { ratingDist.putAll(it) }
                }
                Result.success(AppReviewsResponse(
                    total = data?.get("total")?.asInt ?: 0,
                    page = data?.get("page")?.asInt ?: 1,
                    reviews = reviews,
                    ratingDistribution = ratingDist
                ))
            } else {
                Result.failure(Exception(parseError(responseBody, response.code)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }








    suspend fun listRemoteScripts(projectId: Int): AuthResult<List<RemoteScriptInfo>> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/scripts")
            .header("Authorization", "Bearer $it")
            .get()
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonArray("data")
            val scripts = data?.map { parseRemoteScript(it.asJsonObject) } ?: emptyList()
            AuthResult.Success(scripts)
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun createRemoteScript(projectId: Int, name: String, code: String,
                                    description: String? = null, runAt: String = "document_end",
                                    urlPattern: String? = null, priority: Int = 0
    ): AuthResult<RemoteScriptInfo> = authRequest {
        val body = JsonObject().apply {
            addProperty("name", name)
            addProperty("code", code)
            addProperty("description", description)
            addProperty("run_at", runAt)
            addProperty("url_pattern", urlPattern)
            addProperty("priority", priority)
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/scripts")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            AuthResult.Success(parseRemoteScript(json.getAsJsonObject("data")))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun updateRemoteScript(projectId: Int, scriptId: Int, fields: Map<String, Any?>
    ): AuthResult<RemoteScriptInfo> = authRequest {
        val body = JsonObject()
        fields.forEach { (k, v) ->
            when (v) {
                is String -> body.addProperty(k, v)
                is Boolean -> body.addProperty(k, v)
                is Int -> body.addProperty(k, v)
                null -> body.add(k, com.google.gson.JsonNull.INSTANCE)
            }
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/scripts/$scriptId")
            .header("Authorization", "Bearer $it")
            .put(body.toString().toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            AuthResult.Success(parseRemoteScript(json.getAsJsonObject("data")))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun deleteRemoteScript(projectId: Int, scriptId: Int): AuthResult<Unit> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/scripts/$scriptId")
            .header("Authorization", "Bearer $it")
            .delete()
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            AuthResult.Success(Unit)
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }

    private fun parseRemoteScript(obj: JsonObject?): RemoteScriptInfo = RemoteScriptInfo(
        id = obj?.get("id")?.asInt ?: 0,
        name = obj?.get("name")?.asString ?: "",
        description = obj?.get("description")?.asString,
        code = obj?.get("code")?.asString ?: "",
        runAt = obj?.get("run_at")?.asString ?: "document_end",
        urlPattern = obj?.get("url_pattern")?.asString,
        priority = obj?.get("priority")?.asInt ?: 0,
        isActive = obj?.get("is_active")?.asBoolean ?: true,
        version = obj?.get("version")?.asInt ?: 1,
        createdAt = obj?.get("created_at")?.asString,
        updatedAt = obj?.get("updated_at")?.asString
    )






    suspend fun listBackups(projectId: Int): AuthResult<List<BackupRecord>> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/backups")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val dataArray = json.getAsJsonArray("data")
            val backups = dataArray?.map { el ->
                val b = el.asJsonObject
                BackupRecord(
                    id = b.get("id")?.asInt ?: 0,
                    platform = b.get("platform")?.asString ?: "",
                    status = b.get("status")?.asString ?: "",
                    repoUrl = b.get("repo_url")?.asString,
                    fileSize = b.get("file_size")?.asLong ?: 0,
                    createdAt = b.get("created_at")?.asString
                )
            } ?: emptyList()
            AuthResult.Success(backups)
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun createBackup(projectId: Int, platform: String, zipFile: File): AuthResult<BackupRecord> = authRequest {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", zipFile.name, zipFile.asRequestBody("application/zip".toMediaType()))
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/backup?platform=$platform")
            .header("Authorization", "Bearer $it")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            AuthResult.Success(BackupRecord(
                id = data?.get("id")?.asInt ?: 0,
                platform = data?.get("platform")?.asString ?: platform,
                status = data?.get("status")?.asString ?: "unknown",
                repoUrl = data?.get("repo_url")?.asString,
                fileSize = data?.get("file_size")?.asLong ?: 0,
                createdAt = null
            ))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }

    private fun parseError(body: String, statusCode: Int = 0): String = try {
        val json = JsonParser.parseString(body).asJsonObject
        val msg = json.get("detail")?.asString ?: json.get("message")?.asString ?: "操作失败"

        if (statusCode == 401) "HTTP 401: $msg" else msg
    } catch (_: Exception) { if (statusCode > 0) "HTTP $statusCode" else "操作失败" }

    private fun JsonObject.stringValue(vararg keys: String): String? {
        for (key in keys) {
            val value = get(key) ?: continue
            if (!value.isJsonNull) return value.asString
        }
        return null
    }

    private fun JsonObject.intValue(vararg keys: String): Int? {
        for (key in keys) {
            val value = get(key) ?: continue
            if (!value.isJsonNull) return value.asInt
        }
        return null
    }

    private fun JsonObject.floatValue(vararg keys: String): Float? {
        for (key in keys) {
            val value = get(key) ?: continue
            if (!value.isJsonNull) return value.asFloat
        }
        return null
    }

    private fun JsonObject.boolValue(vararg keys: String): Boolean? {
        for (key in keys) {
            val value = get(key) ?: continue
            if (!value.isJsonNull) return value.asBoolean
        }
        return null
    }

    private fun parseReviewSubmission(obj: JsonObject?): ReviewSubmissionInfo = ReviewSubmissionInfo(
        id = obj?.get("id")?.asInt ?: 0,
        moduleId = obj?.intValue("module_id", "moduleId") ?: 0,
        reviewMode = obj?.stringValue("review_mode", "reviewMode") ?: "public_first",
        requestedVisibility = obj?.stringValue("requested_visibility", "requestedVisibility") ?: "public",
        status = obj?.stringValue("status") ?: "pending",
        submitMessage = obj?.stringValue("submit_message", "submitMessage"),
        rejectionReason = obj?.stringValue("rejection_reason", "rejectionReason"),
        adminReplyText = obj?.stringValue("admin_reply_text", "adminReplyText"),
        reviewedBy = obj?.intValue("reviewed_by", "reviewedBy"),
        decidedAt = obj?.stringValue("decided_at", "decidedAt"),
        createdAt = obj?.stringValue("created_at", "createdAt"),
        updatedAt = obj?.stringValue("updated_at", "updatedAt"),
        submissionAssets = obj?.getAsJsonArray("submission_assets")?.mapNotNull { asset ->
            val assetObj = asset.asJsonObject
            assetObj.stringValue("asset_url", "assetUrl")
        } ?: emptyList(),
        decisionAssets = obj?.getAsJsonArray("decision_assets")?.mapNotNull { asset ->
            val assetObj = asset.asJsonObject
            assetObj.stringValue("asset_url", "assetUrl")
        } ?: emptyList(),
    )






    suspend fun listStoreApps(
        category: String? = null, search: String? = null,
        sort: String = "downloads", order: String = "desc",
        page: Int = 1, size: Int = 20
    ): Result<AppStoreListResponse> = withContext(Dispatchers.IO) {
        try {
            val params = mutableListOf("sort=$sort", "order=$order", "page=$page", "size=$size")
            if (!category.isNullOrBlank()) params.add("category=$category")
            if (!search.isNullOrBlank()) params.add("search=$search")
            val url = "$BASE_URL/api/v1/app-store/apps?${params.joinToString("&")}"

            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val data = json.getAsJsonObject("data")
                val apps = data?.getAsJsonArray("apps")?.map { parseStoreApp(it.asJsonObject) } ?: emptyList()
                val categories = data?.getAsJsonArray("categories")?.map { it.asString } ?: emptyList()
                Result.success(AppStoreListResponse(
                    total = data?.get("total")?.asInt ?: 0,
                    page = data?.get("page")?.asInt ?: 1,
                    size = data?.get("size")?.asInt ?: 20,
                    apps = apps,
                    categories = categories
                ))
            } else {
                Result.failure(Exception(parseError(responseBody, response.code)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun getStoreAppDetail(appId: Int): Result<AppStoreItem> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$BASE_URL/api/v1/app-store/apps/$appId").get().build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val data = json.getAsJsonObject("data")
                Result.success(parseStoreApp(data!!))
            } else {
                Result.failure(Exception(parseError(responseBody, response.code)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun downloadStoreApp(appId: Int): Result<Map<String, String?>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/app-store/apps/$appId/download")
                .post("".toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val data = json.getAsJsonObject("data")
                Result.success(mapOf(
                    "apk_url_github" to data?.get("apk_url_github")?.let { if (it.isJsonNull) null else it.asString },
                    "apk_url_gitee" to data?.get("apk_url_gitee")?.let { if (it.isJsonNull) null else it.asString }
                ))
            } else {
                Result.failure(Exception(parseError(responseBody, response.code)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun reviewStoreApp(appId: Int, rating: Int, comment: String? = null): AuthResult<Unit> = authRequest {
        val body = JsonObject().apply {
            addProperty("rating", rating)
            comment?.let { addProperty("comment", it) }
            addProperty("device_model", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/app-store/apps/$appId/review")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit)
        else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun likeStoreApp(appId: Int): AuthResult<LikeResponse> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/app-store/apps/$appId/like")
            .header("Authorization", "Bearer $it")
            .post("".toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            AuthResult.Success(LikeResponse(
                liked = data?.get("liked")?.asBoolean ?: false,
                likeCount = data?.get("like_count")?.asInt ?: 0
            ))
        } else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun getLikeStatus(appId: Int): AuthResult<Boolean> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/app-store/apps/$appId/like/status")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            AuthResult.Success(data?.get("liked")?.asBoolean ?: false)
        } else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun getAppReviews(appId: Int, page: Int = 1, size: Int = 20): Result<AppReviewsResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/app-store/apps/$appId/reviews?page=$page&size=$size")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            AppLogger.d(TAG, "getAppReviews HTTP ${response.code}, body length=${responseBody.length}")
            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val data = json.getAsJsonObject("data")
                AppLogger.d(TAG, "getAppReviews data=$data")
                val reviews = data?.getAsJsonArray("reviews")?.map { el ->
                    val r = el.asJsonObject
                    AppReviewItem(
                        id = r.get("id")?.asInt ?: 0,
                        rating = r.get("rating")?.asInt ?: 0,
                        comment = r.get("comment")?.let { if (it.isJsonNull) null else it.asString },
                        authorName = r.get("author_name")?.asString ?: "Unknown",
                        authorId = r.get("author_id")?.asInt ?: 0,
                        deviceModel = r.get("device_model")?.let { if (it.isJsonNull) null else it.asString },
                        ipAddress = r.get("ip_address")?.let { if (it.isJsonNull) null else it.asString },
                        createdAt = r.get("created_at")?.let { if (it.isJsonNull) null else it.asString }
                    )
                } ?: emptyList()
                AppLogger.d(TAG, "getAppReviews parsed ${reviews.size} reviews")
                Result.success(AppReviewsResponse(
                    total = data?.get("total")?.asInt ?: 0,
                    page = data?.get("page")?.asInt ?: 1,
                    reviews = reviews
                ))
            } else {
                AppLogger.e(TAG, "getAppReviews failed: HTTP ${response.code} body=$responseBody")
                Result.failure(Exception(parseError(responseBody, response.code)))
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "getAppReviews exception", e)
            Result.failure(e)
        }
    }


    suspend fun reportStoreApp(appId: Int, reason: String, description: String? = null): AuthResult<Unit> = authRequest {
        val body = JsonObject().apply {
            addProperty("reason", reason)
            description?.let { addProperty("description", it) }
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/app-store/apps/$appId/report")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit)
        else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun listMyApps(): AuthResult<MyAppsResponse> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/app-store/my-apps")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            val apps = data?.getAsJsonArray("apps")?.map { parseStoreApp(it.asJsonObject) } ?: emptyList()
            AuthResult.Success(MyAppsResponse(
                apps = apps,
                count = data?.get("count")?.asInt ?: 0,
                quota = data?.get("quota")?.asInt ?: 0,
                tier = data?.get("tier")?.asString ?: "free"
            ))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun updateStoreApp(appId: Int, name: String, description: String, category: String): AuthResult<String> = authRequest {
        val body = JsonObject().apply {
            addProperty("name", name)
            addProperty("description", description)
            addProperty("category", category)
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/app-store/apps/$appId")
            .header("Authorization", "Bearer $it")
            .put(body.toString().toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success("应用信息已更新")
        else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun deleteStoreApp(appId: Int): AuthResult<String> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/app-store/apps/$appId")
            .header("Authorization", "Bearer $it")
            .delete()
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success("应用已删除")
        else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun publishApp(
        name: String, description: String, category: String,
        versionName: String, versionCode: Int,
        packageName: String? = null, icon: String? = null,
        tags: String? = null, screenshots: List<String> = emptyList(),
        videoUrl: String? = null,
        apkUrlGithub: String? = null, apkUrlGitee: String? = null,
        contactEmail: String? = null, contactPhone: String? = null,
        groupChatUrl: String? = null, websiteUrl: String? = null,
        privacyPolicyUrl: String? = null,
        appSourceType: String = "WEB",
        visibility: String = "public",
        reviewMode: String = "public_first",
        sourceBundleUrl: String? = null,
        buildManifestJson: JsonObject? = null,
        legacyProjectId: Int? = null
    ): AuthResult<AppStoreItem> = authRequest {
        val body = JsonObject().apply {
            addProperty("name", name)
            addProperty("description", description)
            addProperty("category", category)
            addProperty("version_name", versionName)
            addProperty("version_code", versionCode)
            packageName?.let { addProperty("package_name", it) }
            icon?.let { addProperty("icon", it) }
            tags?.let { addProperty("tags", it) }
            val screenshotsArray = com.google.gson.JsonArray()
            screenshots.forEach { screenshotsArray.add(it) }
            add("screenshots", screenshotsArray)
            videoUrl?.let { addProperty("video_url", it) }
            apkUrlGithub?.let { addProperty("apk_url_github", it) }
            apkUrlGitee?.let { addProperty("apk_url_gitee", it) }
            contactEmail?.let { addProperty("contact_email", it) }
            contactPhone?.let { addProperty("contact_phone", it) }
            groupChatUrl?.let { addProperty("group_chat_url", it) }
            websiteUrl?.let { addProperty("website_url", it) }
            privacyPolicyUrl?.let { addProperty("privacy_policy_url", it) }
            addProperty("app_source_type", appSourceType)
            addProperty("visibility", visibility)
            addProperty("review_mode", reviewMode)
            sourceBundleUrl?.let { addProperty("source_bundle_url", it) }
            buildManifestJson?.let { add("build_manifest_json", it) }
            legacyProjectId?.let { addProperty("legacy_project_id", it) }
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/app-store/apps")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            AuthResult.Success(parseStoreApp(json.getAsJsonObject("data")))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun deleteMyApp(appId: Int): AuthResult<Unit> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/app-store/apps/$appId")
            .header("Authorization", "Bearer $it")
            .delete()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit)
        else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun deleteMyModule(moduleId: Int): AuthResult<Unit> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/modules/$moduleId")
            .header("Authorization", "Bearer $it")
            .delete()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit)
        else AuthResult.Error(parseError(responseBody, response.code))
    }






    suspend fun getActivationSettings(appId: Int): AuthResult<ActivationSettings> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/app-store/apps/$appId/activation")
            .header("Authorization", "Bearer $it")
            .get().build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(body).asJsonObject.getAsJsonObject("data")
            val codes = json.getAsJsonArray("codes")?.map { c ->
                val o = c.asJsonObject
                ActivationCode(
                    id = o.get("id")?.asInt ?: 0, code = o.get("code")?.asString ?: "",
                    appId = appId,
                    isUsed = o.boolValue("isUsed", "is_used") ?: false,
                    usedByDeviceId = o.stringValue("usedByDeviceId", "used_by_device_id"),
                    usedByUserId = o.stringValue("usedByUserId", "used_by_user_id"),
                    usedAt = o.stringValue("usedAt", "used_at"),
                    createdAt = o.stringValue("createdAt", "created_at"),
                    expiresAt = o.stringValue("expiresAt", "expires_at"),
                    maxUses = o.intValue("maxUses", "max_uses") ?: 1,
                    currentUses = o.intValue("currentUses", "current_uses") ?: 0
                )
            } ?: emptyList()
            AuthResult.Success(ActivationSettings(
                enabled = json.get("enabled")?.asBoolean ?: false,
                deviceBindingEnabled = json.boolValue("deviceBindingEnabled", "device_binding_enabled") ?: false,
                maxDevicesPerCode = json.intValue("maxDevicesPerCode", "max_devices_per_code") ?: 1,
                activationHelpUrl = json.stringValue("activationHelpUrl", "activation_help_url"),
                codes = codes,
                totalCodes = json.intValue("totalCodes", "total_codes") ?: codes.size,
                usedCodes = json.intValue("usedCodes", "used_codes") ?: codes.count { it.isUsed }
            ))
        } else AuthResult.Error(parseError(body, response.code))
    }


    suspend fun createActivationCodes(appId: Int, codes: List<String>, maxUses: Int = 1): AuthResult<ActivationCodeBatchResult> = authRequest {
        val jsonBody = JsonObject().apply {
            val arr = com.google.gson.JsonArray()
            codes.forEach { c -> arr.add(c) }
            add("codes", arr)
            addProperty("max_uses", maxUses)
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/app-store/apps/$appId/activation/codes")
            .header("Authorization", "Bearer $it")
            .post(jsonBody.toString().toRequestBody(jsonMediaType)).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val data = JsonParser.parseString(body).asJsonObject.getAsJsonObject("data")
            AuthResult.Success(
                ActivationCodeBatchResult(
                    created = data?.intValue("created") ?: 0,
                    skipped = data?.intValue("skipped") ?: 0,
                    quotaUsed = data?.intValue("quotaUsed", "quota_used") ?: 0,
                    quotaTotal = data?.intValue("quotaTotal", "quota_total") ?: 0
                )
            )
        } else AuthResult.Error(parseError(body, response.code))
    }


    suspend fun deleteActivationCode(appId: Int, codeId: Int): AuthResult<Unit> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/app-store/apps/$appId/activation/codes/$codeId")
            .header("Authorization", "Bearer $it").delete().build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit) else AuthResult.Error(parseError(body, response.code))
    }


    suspend fun updateActivationSettings(appId: Int, enabled: Boolean, deviceBinding: Boolean, maxDevices: Int, activationHelpUrl: String? = null): AuthResult<Unit> = authRequest {
        val jsonBody = JsonObject().apply {
            addProperty("enabled", enabled)
            addProperty("device_binding_enabled", deviceBinding)
            addProperty("max_devices_per_code", maxDevices)
            activationHelpUrl?.let { addProperty("activation_help_url", it) }
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/app-store/apps/$appId/activation/settings")
            .header("Authorization", "Bearer $it")
            .put(jsonBody.toString().toRequestBody(jsonMediaType)).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit) else AuthResult.Error(parseError(body, response.code))
    }


    suspend fun getAnnouncements(appId: Int): AuthResult<List<Announcement>> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/app-store/apps/$appId/announcements")
            .header("Authorization", "Bearer $it").get().build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val arr = JsonParser.parseString(body).asJsonObject.getAsJsonArray("data")
            AuthResult.Success(arr?.map { a ->
                val o = a.asJsonObject
                Announcement(
                    id = o.get("id")?.asInt ?: 0, appId = appId,
                    title = o.get("title")?.asString ?: "", content = o.get("content")?.asString ?: "",
                    type = o.get("type")?.asString ?: "info",
                    isActive = o.boolValue("isActive", "is_active") ?: true,
                    isPinned = o.boolValue("isPinned", "is_pinned") ?: false,
                    createdAt = o.stringValue("createdAt", "created_at"),
                    expiresAt = o.stringValue("expiresAt", "expires_at"),
                    viewCount = o.intValue("viewCount", "view_count") ?: 0
                )
            } ?: emptyList())
        } else AuthResult.Error(parseError(body, response.code))
    }


    suspend fun createAnnouncement(appId: Int, title: String, content: String, type: String, isPinned: Boolean = false): AuthResult<Announcement> = authRequest {
        val jsonBody = JsonObject().apply {
            addProperty("title", title); addProperty("content", content)
            addProperty("type", type); addProperty("is_pinned", isPinned)
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/app-store/apps/$appId/announcements")
            .header("Authorization", "Bearer $it")
            .post(jsonBody.toString().toRequestBody(jsonMediaType)).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val o = JsonParser.parseString(body).asJsonObject.getAsJsonObject("data")
            AuthResult.Success(Announcement(
                id = o.get("id")?.asInt ?: 0, appId = appId,
                title = title, content = content, type = type, isPinned = isPinned
            ))
        } else AuthResult.Error(parseError(body, response.code))
    }


    suspend fun deleteAnnouncement(appId: Int, announcementId: Int): AuthResult<Unit> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/app-store/apps/$appId/announcements/$announcementId")
            .header("Authorization", "Bearer $it").delete().build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit) else AuthResult.Error(parseError(body, response.code))
    }


    suspend fun getUpdateConfig(appId: Int): AuthResult<UpdateConfig> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/app-store/apps/$appId/update-config")
            .header("Authorization", "Bearer $it").get().build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val o = JsonParser.parseString(body).asJsonObject.getAsJsonObject("data")
            AuthResult.Success(UpdateConfig(
                id = o.get("id")?.asInt ?: 0, appId = appId,
                latestVersionName = o.stringValue("latestVersionName", "latest_version_name") ?: "",
                latestVersionCode = o.intValue("latestVersionCode", "latest_version_code") ?: 0,
                updateTitle = o.stringValue("updateTitle", "update_title") ?: "",
                updateContent = o.stringValue("updateContent", "update_content") ?: "",
                apkUrl = o.stringValue("apkUrl", "apk_url"),
                sourceAppId = o.intValue("sourceAppId", "source_app_id"),
                sourceAppName = o.stringValue("sourceAppName", "source_app_name"),
                isForceUpdate = o.boolValue("isForceUpdate", "is_force_update") ?: false,
                minVersionCode = o.intValue("minVersionCode", "min_version_code") ?: 0,
                templateId = o.stringValue("templateId", "template_id") ?: "simple",
                isActive = o.boolValue("isActive", "is_active") ?: false
            ))
        } else AuthResult.Error(parseError(body, response.code))
    }


    suspend fun pushUpdate(
        appId: Int, versionName: String, versionCode: Int,
        title: String, content: String, sourceAppId: Int?,
        isForceUpdate: Boolean, minVersionCode: Int, templateId: String
    ): AuthResult<UpdateConfig> = authRequest {
        val jsonBody = JsonObject().apply {
            addProperty("version_name", versionName)
            addProperty("version_code", versionCode)
            addProperty("title", title); addProperty("content", content)
            sourceAppId?.let { addProperty("source_app_id", it) }
            addProperty("is_force_update", isForceUpdate)
            addProperty("min_version_code", minVersionCode)
            addProperty("template_id", templateId)
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/app-store/apps/$appId/update-config")
            .header("Authorization", "Bearer $it")
            .post(jsonBody.toString().toRequestBody(jsonMediaType)).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (response.isSuccessful) {
            AuthResult.Success(UpdateConfig(
                appId = appId, latestVersionName = versionName,
                latestVersionCode = versionCode, updateTitle = title,
                updateContent = content, sourceAppId = sourceAppId,
                isForceUpdate = isForceUpdate, templateId = templateId, isActive = true
            ))
        } else AuthResult.Error(parseError(body, response.code))
    }


    suspend fun getAppUsers(appId: Int, page: Int = 1, limit: Int = 50): AuthResult<List<AppUser>> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/app-store/apps/$appId/users?page=$page&limit=$limit")
            .header("Authorization", "Bearer $it").get().build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val arr = JsonParser.parseString(body).asJsonObject.getAsJsonArray("data")
            AuthResult.Success(arr?.map { u ->
                val o = u.asJsonObject
                AppUser(
                    id = o.get("id")?.asString ?: "",
                    deviceModel = o.stringValue("deviceModel", "device_model"),
                    osVersion = o.stringValue("osVersion", "os_version"),
                    appVersion = o.stringValue("appVersion", "app_version"),
                    country = o.stringValue("country"),
                    region = o.stringValue("region"),
                    city = o.stringValue("city"),
                    ipAddress = o.stringValue("ipAddress", "ip_address"),
                    firstSeenAt = o.stringValue("firstSeenAt", "first_seen_at"),
                    lastSeenAt = o.stringValue("lastSeenAt", "last_seen_at"),
                    activationCode = o.stringValue("activationCode", "activation_code"),
                    externalUserId = o.stringValue("externalUserId", "external_user_id"),
                    isActive = o.boolValue("isActive", "is_active") ?: true
                )
            } ?: emptyList())
        } else AuthResult.Error(parseError(body, response.code))
    }


    suspend fun getUserGeoDistribution(appId: Int): AuthResult<List<GeoDistribution>> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/app-store/apps/$appId/users/geo")
            .header("Authorization", "Bearer $it").get().build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val arr = JsonParser.parseString(body).asJsonObject.getAsJsonArray("data")
            AuthResult.Success(arr?.map { g ->
                val o = g.asJsonObject
                GeoDistribution(
                    country = o.get("country")?.asString ?: "",
                    countryCode = o.stringValue("countryCode", "country_code") ?: "",
                    count = o.get("count")?.asInt ?: 0,
                    percentage = o.get("percentage")?.asFloat ?: 0f,
                    regions = o.getAsJsonArray("regions")?.map { r ->
                        val ro = r.asJsonObject
                        RegionInfo(ro.get("region")?.asString ?: "", ro.get("count")?.asInt ?: 0, ro.get("percentage")?.asFloat ?: 0f)
                    } ?: emptyList()
                )
            } ?: emptyList())
        } else AuthResult.Error(parseError(body, response.code))
    }

    suspend fun getAppQuota(appId: Int): AuthResult<AppQuotaInfo> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/app-store/apps/$appId/quota")
            .header("Authorization", "Bearer $it")
            .get()
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val o = JsonParser.parseString(body).asJsonObject.getAsJsonObject("data")
            AuthResult.Success(
                AppQuotaInfo(
                    tier = o?.stringValue("tier") ?: "free",
                    retentionDays = o?.intValue("retentionDays", "retention_days") ?: 0,
                    appPublishLimit = o?.intValue("appPublishLimit", "app_publish_limit") ?: 0,
                    activationCodesTotalLimit = o?.intValue("activationCodesTotalLimit", "activation_codes_total_limit") ?: 0,
                    activationCodesDailyLimit = o?.intValue("activationCodesDailyLimit", "activation_codes_daily_limit") ?: 0,
                    activationCodesCurrent = o?.intValue("activationCodesCurrent", "activation_codes_current") ?: 0,
                    announcementDailyLimit = o?.intValue("announcementDailyLimit", "announcement_daily_limit") ?: 0,
                    updateDailyLimit = o?.intValue("updateDailyLimit", "update_daily_limit") ?: 0,
                    reviewSubmissionDailyLimit = o?.intValue("reviewSubmissionDailyLimit", "review_submission_daily_limit") ?: 0,
                    appReviewSubmissionDailyLimit = o?.intValue("appReviewSubmissionDailyLimit", "app_review_submission_daily_limit") ?: 0,
                    runtimeEventsDailyLimit = o?.intValue("runtimeEventsDailyLimit", "runtime_events_daily_limit") ?: 0,
                    reviewStatus = o?.stringValue("reviewStatus", "review_status"),
                    verificationStatus = o?.stringValue("verificationStatus", "verification_status"),
                    visibility = o?.stringValue("visibility"),
                )
            )
        } else AuthResult.Error(parseError(body, response.code))
    }

    suspend fun listAppBans(appId: Int): AuthResult<List<AppBanItem>> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/app-store/apps/$appId/bans")
            .header("Authorization", "Bearer $it")
            .get()
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val arr = JsonParser.parseString(body).asJsonObject.getAsJsonArray("data")
            AuthResult.Success(arr?.map { item ->
                val o = item.asJsonObject
                AppBanItem(
                    id = o.get("id")?.asInt ?: 0,
                    ruleType = o.stringValue("ruleType", "rule_type") ?: "",
                    targetValue = o.stringValue("targetValue", "target_value") ?: "",
                    reason = o.stringValue("reason"),
                    revokedAt = o.stringValue("revokedAt", "revoked_at"),
                    createdAt = o.stringValue("createdAt", "created_at")
                )
            } ?: emptyList())
        } else AuthResult.Error(parseError(body, response.code))
    }

    suspend fun createAppBan(appId: Int, ruleType: String, targetValue: String, reason: String? = null): AuthResult<Int> = authRequest {
        val jsonBody = JsonObject().apply {
            addProperty("rule_type", ruleType)
            addProperty("target_value", targetValue)
            reason?.let { addProperty("reason", it) }
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/app-store/apps/$appId/bans")
            .header("Authorization", "Bearer $it")
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val data = JsonParser.parseString(body).asJsonObject.getAsJsonObject("data")
            AuthResult.Success(data?.get("id")?.asInt ?: 0)
        } else AuthResult.Error(parseError(body, response.code))
    }

    suspend fun revokeAppBan(appId: Int, banId: Int): AuthResult<Unit> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/app-store/apps/$appId/bans/$banId")
            .header("Authorization", "Bearer $it")
            .delete()
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit) else AuthResult.Error(parseError(body, response.code))
    }

    suspend fun submitAppReviewSubmission(
        appId: Int,
        reviewMode: String,
        requestedVisibility: String,
        submitMessage: String? = null,
        assetUrls: List<String> = emptyList(),
    ): AuthResult<ReviewSubmissionInfo> = authRequest {
        val jsonBody = JsonObject().apply {
            addProperty("review_mode", reviewMode)
            addProperty("requested_visibility", requestedVisibility)
            submitMessage?.let { addProperty("submit_message", it) }
            val arr = com.google.gson.JsonArray()
            assetUrls.forEach { arr.add(it) }
            add("asset_urls", arr)
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/app-store/apps/$appId/review-submissions")
            .header("Authorization", "Bearer $it")
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val data = JsonParser.parseString(body).asJsonObject.getAsJsonObject("data")
            AuthResult.Success(parseReviewSubmission(data))
        } else AuthResult.Error(parseError(body, response.code))
    }

    suspend fun getAppReviewSubmissions(appId: Int): AuthResult<List<ReviewSubmissionInfo>> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/app-store/apps/$appId/review-submissions")
            .header("Authorization", "Bearer $it")
            .get()
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val items = JsonParser.parseString(body).asJsonObject.getAsJsonObject("data")?.getAsJsonArray("items")
            AuthResult.Success(items?.map { parseReviewSubmission(it.asJsonObject) } ?: emptyList())
        } else AuthResult.Error(parseError(body, response.code))
    }

    suspend fun getModuleReviewSubmissions(moduleId: Int): AuthResult<List<ReviewSubmissionInfo>> = authRequest {
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/modules/$moduleId/review-submissions")
            .header("Authorization", "Bearer $it")
            .get()
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val items = JsonParser.parseString(body).asJsonObject.getAsJsonObject("data")?.getAsJsonArray("items")
            AuthResult.Success(items?.map { parseReviewSubmission(it.asJsonObject) } ?: emptyList())
        } else AuthResult.Error(parseError(body, response.code))
    }

    suspend fun submitModuleReviewSubmission(
        moduleId: Int,
        reviewMode: String,
        requestedVisibility: String,
        submitMessage: String? = null,
        assetUrls: List<String> = emptyList(),
    ): AuthResult<Unit> = authRequest {
        val jsonBody = JsonObject().apply {
            addProperty("review_mode", reviewMode)
            addProperty("requested_visibility", requestedVisibility)
            submitMessage?.let { addProperty("submit_message", it) }
            val arr = com.google.gson.JsonArray()
            assetUrls.forEach { arr.add(it) }
            add("asset_urls", arr)
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/modules/$moduleId/review-submissions")
            .header("Authorization", "Bearer $it")
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit) else AuthResult.Error(parseError(body, response.code))
    }






    suspend fun sendPushNotification(
        projectId: Int,
        title: String,
        body: String,
        pushType: String = "announcement",
        versionName: String? = null,
        versionCode: Int? = null,
        forceUpdate: Boolean = false
    ): AuthResult<Unit> = authRequest { token ->
        val formBody = okhttp3.FormBody.Builder()
            .add("title", title)
            .add("body", body)
            .add("push_type", pushType)
        versionName?.let { formBody.add("version_name", it) }
        versionCode?.let { formBody.add("version_code", it.toString()) }
        if (pushType == "update") formBody.add("force_update", forceUpdate.toString())

        val request = Request.Builder()
            .url("$BASE_URL/api/v1/projects/$projectId/push/send")
            .header("Authorization", "Bearer $token")
            .post(formBody.build())
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit)
        else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun getPushHistory(projectId: Int, page: Int = 1): Result<PushHistoryResponse> = withContext(Dispatchers.IO) {
        try {
            val token = tokenManager.getAccessToken() ?: return@withContext Result.failure(Exception("Not logged in"))
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/projects/$projectId/push/history?page=$page")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val data = json.getAsJsonObject("data")
                val records = data?.getAsJsonArray("records")?.map { el ->
                    val r = el.asJsonObject
                    PushHistoryItem(
                        id = r.get("id")?.asInt ?: 0,
                        title = r.get("title")?.asString ?: "",
                        body = r.get("body")?.let { if (it.isJsonNull) "" else it.asString } ?: "",
                        targetType = r.get("target_type")?.asString ?: r.get("topic")?.asString ?: "all",
                        sentCount = r.get("sent_count")?.asInt ?: 0,
                        createdAt = r.get("created_at")?.let { if (it.isJsonNull) null else it.asString },
                    )
                } ?: emptyList()

                Result.success(PushHistoryResponse(
                    total = data?.get("total")?.asInt ?: 0,
                    page = data?.get("page")?.asInt ?: 1,
                    dailyUsed = data?.get("daily_used")?.asInt ?: 0,
                    dailyLimit = data?.get("daily_limit")?.asInt ?: 3,
                    tier = data?.get("tier")?.asString ?: "pro",
                    records = records
                ))
            } else {
                Result.failure(Exception(parseError(responseBody, response.code)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseStoreApp(obj: JsonObject): AppStoreItem = AppStoreItem(
        id = obj.get("id")?.asInt ?: 0,
        name = obj.get("name")?.asString ?: "",
        icon = obj.get("icon")?.let { if (it.isJsonNull) null else it.asString },
        category = obj.get("category")?.asString ?: "other",
        tags = obj.getAsJsonArray("tags")?.map { it.asString } ?: emptyList(),
        versionName = obj.get("version_name")?.asString ?: "1.0",
        versionCode = obj.intValue("version_code", "versionCode") ?: 0,
        packageName = obj.get("package_name")?.let { if (it.isJsonNull) null else it.asString },
        downloads = obj.get("downloads")?.asInt ?: 0,
        rating = obj.get("rating")?.asFloat ?: 0f,
        ratingCount = obj.get("rating_count")?.asInt ?: 0,
        likeCount = obj.get("like_count")?.asInt ?: 0,
        isFeatured = obj.get("is_featured")?.asBoolean ?: false,
        screenshots = obj.getAsJsonArray("screenshots")?.map { it.asString } ?: emptyList(),
        authorName = obj.get("author_name")?.asString ?: "Unknown",
        authorId = obj.get("author_id")?.asInt ?: 0,
        createdAt = obj.get("created_at")?.let { if (it.isJsonNull) null else it.asString },
        description = obj.get("description")?.let { if (it.isJsonNull) null else it.asString },
        videoUrl = obj.get("video_url")?.let { if (it.isJsonNull) null else it.asString },
        apkUrlGithub = obj.get("apk_url_github")?.let { if (it.isJsonNull) null else it.asString },
        apkUrlGitee = obj.get("apk_url_gitee")?.let { if (it.isJsonNull) null else it.asString },
        contactEmail = obj.get("contact_email")?.let { if (it.isJsonNull) null else it.asString },
        contactPhone = obj.get("contact_phone")?.let { if (it.isJsonNull) null else it.asString },
        groupChatUrl = obj.get("group_chat_url")?.let { if (it.isJsonNull) null else it.asString },
        paymentQrUrl = obj.get("payment_qr_url")?.let { if (it.isJsonNull) null else it.asString },
        websiteUrl = obj.get("website_url")?.let { if (it.isJsonNull) null else it.asString },
        privacyPolicyUrl = obj.get("privacy_policy_url")?.let { if (it.isJsonNull) null else it.asString },
        visibility = obj.stringValue("visibility"),
        reviewMode = obj.stringValue("review_mode", "reviewMode"),
        reviewStatus = obj.stringValue("review_status", "reviewStatus"),
        verificationStatus = obj.stringValue("verification_status", "verificationStatus"),
        appSourceType = obj.stringValue("app_source_type", "appSourceType"),
        runtimeKey = obj.stringValue("runtime_key", "runtimeKey"),
        reviewSortPenalty = obj.floatValue("review_sort_penalty", "reviewSortPenalty") ?: 0f,
        rejectionReason = obj.stringValue("rejection_reason", "rejectionReason"),
        adminReplyText = obj.stringValue("admin_reply_text", "adminReplyText"),
        sourceBundleUrl = obj.stringValue("source_bundle_url", "sourceBundleUrl"),
        buildManifestJson = obj.getAsJsonObject("build_manifest_json")?.toString(),
        legacyProjectId = obj.intValue("legacy_project_id", "legacyProjectId"),
        lastReviewedAt = obj.stringValue("last_reviewed_at", "lastReviewedAt"),
    )






    suspend fun listCommunityPosts(page: Int = 1, size: Int = 10, tag: String? = null, search: String? = null, postType: String? = null): AuthResult<CommunityFeedResponse> =
        withContext(Dispatchers.IO) {
            try {
                val urlBuilder = StringBuilder("$BASE_URL/api/v1/community/posts?page=$page&size=$size")
                tag?.let { urlBuilder.append("&tag=$it") }
                search?.let { urlBuilder.append("&search=$it") }
                postType?.let { urlBuilder.append("&post_type=$it") }
                val requestBuilder = Request.Builder().url(urlBuilder.toString()).get()

                tokenManager.getAccessToken()?.let { requestBuilder.header("Authorization", "Bearer $it") }
                val response = client.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JsonParser.parseString(responseBody).asJsonObject
                    val data = json.getAsJsonObject("data")
                    val posts = data?.getAsJsonArray("posts")?.map { parseCommunityPost(it.asJsonObject) } ?: emptyList()
                    AuthResult.Success(CommunityFeedResponse(posts = posts, total = data?.get("total")?.asInt ?: 0))
                } else AuthResult.Error(parseError(responseBody, response.code))
            } catch (e: Exception) {
                AuthResult.Error("网络错误: ${e.message}")
            }
        }


    suspend fun getCommunityPost(postId: Int): AuthResult<CommunityPostItem> =
        withContext(Dispatchers.IO) {
            try {
                val requestBuilder = Request.Builder().url("$BASE_URL/api/v1/community/posts/$postId").get()
                tokenManager.getAccessToken()?.let { requestBuilder.header("Authorization", "Bearer $it") }
                val response = client.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JsonParser.parseString(responseBody).asJsonObject
                    AuthResult.Success(parseCommunityPost(json.getAsJsonObject("data")))
                } else AuthResult.Error(parseError(responseBody, response.code))
            } catch (e: Exception) {
                AuthResult.Error("网络错误: ${e.message}")
            }
        }


    suspend fun createCommunityPost(
        content: String,
        tags: List<String>,
        media: List<PostMediaInput> = emptyList(),
        appLinks: List<PostAppLinkInput> = emptyList(),
        postType: String = "discussion",

        appName: String? = null,
        appIconUrl: String? = null,
        sourceType: String? = null,
        sourceUrl: String? = null,
        projectRecipe: String? = null,

        title: String? = null,
        difficulty: String? = null,
        steps: List<Pair<String, String?>>? = null,
    ): AuthResult<CommunityPostItem> = authRequest { token ->
        val body = JsonObject().apply {
            addProperty("content", content)
            addProperty("post_type", postType)
            add("tags", com.google.gson.JsonArray().apply { tags.forEach { add(it) } })
            add("media", com.google.gson.JsonArray().apply {
                media.forEach { m -> add(JsonObject().apply {
                    addProperty("media_type", m.mediaType)
                    m.urlGithub?.let { addProperty("url_github", it) }
                    m.urlGitee?.let { addProperty("url_gitee", it) }
                    m.thumbnailUrl?.let { addProperty("thumbnail_url", it) }
                }) }
            })
            add("app_links", com.google.gson.JsonArray().apply {
                appLinks.forEach { al -> add(JsonObject().apply {
                    addProperty("link_type", al.linkType)
                    al.storeModuleId?.let { addProperty("store_module_id", it) }
                    al.appName?.let { addProperty("app_name", it) }
                    al.appIcon?.let { addProperty("app_icon", it) }
                    al.appDescription?.let { addProperty("app_description", it) }
                }) }
            })

            appName?.let { addProperty("app_name", it) }
            appIconUrl?.let { addProperty("app_icon_url", it) }
            sourceType?.let { addProperty("source_type", it) }
            sourceUrl?.let { addProperty("source_url", it) }
            projectRecipe?.let { addProperty("project_recipe", it) }

            title?.let { addProperty("title", it) }
            difficulty?.let { addProperty("difficulty", it) }
            steps?.let { stepsList ->
                add("steps", com.google.gson.JsonArray().apply {
                    stepsList.forEach { (stepContent, stepImage) ->
                        add(JsonObject().apply {
                            addProperty("content", stepContent)
                            stepImage?.let { addProperty("image_url", it) }
                        })
                    }
                })
            }
        }
        val request = Request.Builder().url("$BASE_URL/api/v1/community/posts").header("Authorization", "Bearer $token")
            .post(body.toString().toRequestBody(jsonMediaType)).build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            AuthResult.Success(parseCommunityPost(json.getAsJsonObject("data")))
        } else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun deleteCommunityPost(postId: Int): AuthResult<Unit> = authRequest { token ->
        val request = Request.Builder().url("$BASE_URL/api/v1/community/posts/$postId").header("Authorization", "Bearer $token").delete().build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit)
        else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun togglePostLike(postId: Int): AuthResult<PostLikeResult> = authRequest { token ->
        val request = Request.Builder().url("$BASE_URL/api/v1/community/posts/$postId/like").header("Authorization", "Bearer $token")
            .post("".toRequestBody(jsonMediaType)).build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            AuthResult.Success(PostLikeResult(liked = data?.get("liked")?.asBoolean ?: false, likeCount = data?.get("like_count")?.asInt ?: 0))
        } else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun sharePost(postId: Int): AuthResult<Unit> = authRequest { token ->
        val request = Request.Builder().url("$BASE_URL/api/v1/community/posts/$postId/share").header("Authorization", "Bearer $token")
            .post("".toRequestBody(jsonMediaType)).build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit)
        else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun toggleCommentLike(postId: Int, commentId: Int): AuthResult<CommentLikeResult> = authRequest { token ->
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/community/posts/$postId/comments/$commentId/like")
            .header("Authorization", "Bearer $token")
            .post("".toRequestBody(jsonMediaType)).build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            AuthResult.Success(CommentLikeResult(
                liked = data?.get("liked")?.asBoolean ?: false,
                likeCount = data?.get("like_count")?.asInt ?: 0
            ))
        } else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun togglePostBookmark(postId: Int): AuthResult<BookmarkResult> = authRequest { token ->
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/community/posts/$postId/bookmark")
            .header("Authorization", "Bearer $token")
            .post("".toRequestBody(jsonMediaType)).build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            AuthResult.Success(BookmarkResult(bookmarked = data?.get("bookmarked")?.asBoolean ?: false))
        } else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun listPostComments(postId: Int, page: Int = 1, size: Int = 30): AuthResult<CommentsResponse> =
        withContext(Dispatchers.IO) {
            try {
                val requestBuilder = Request.Builder()
                    .url("$BASE_URL/api/v1/community/posts/$postId/comments?page=$page&size=$size").get()
                tokenManager.getAccessToken()?.let { requestBuilder.header("Authorization", "Bearer $it") }
                val response = client.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JsonParser.parseString(responseBody).asJsonObject
                    val dataObj = json.get("data")
                    if (dataObj != null && dataObj.isJsonObject) {

                        val data = dataObj.asJsonObject
                        val total = data.get("total")?.asInt ?: 0
                        val comments = data.getAsJsonArray("comments")?.map { parsePostComment(it.asJsonObject) } ?: emptyList()
                        AuthResult.Success(CommentsResponse(total = total, page = page, comments = comments))
                    } else if (dataObj != null && dataObj.isJsonArray) {

                        val comments = dataObj.asJsonArray.map { parsePostComment(it.asJsonObject) }
                        AuthResult.Success(CommentsResponse(total = comments.size, page = 1, comments = comments))
                    } else {
                        AuthResult.Success(CommentsResponse(total = 0, page = 1, comments = emptyList()))
                    }
                } else AuthResult.Error(parseError(responseBody, response.code))
            } catch (e: Exception) {
                AuthResult.Error("网络错误: ${e.message}")
            }
        }


    suspend fun addPostComment(postId: Int, content: String, parentId: Int? = null, media: List<PostMediaInput> = emptyList()): AuthResult<PostCommentItem> = authRequest { token ->
        val body = JsonObject().apply {
            addProperty("content", content)
            parentId?.let { addProperty("parent_id", it) }
            if (media.isNotEmpty()) {
                add("media", com.google.gson.JsonArray().apply {
                    media.forEach { m -> add(JsonObject().apply {
                        addProperty("media_type", m.mediaType)
                        m.urlGithub?.let { addProperty("url_github", it) }
                        m.urlGitee?.let { addProperty("url_gitee", it) }
                        m.thumbnailUrl?.let { addProperty("thumbnail_url", it) }
                    }) }
                })
            }
        }
        val request = Request.Builder().url("$BASE_URL/api/v1/community/posts/$postId/comment").header("Authorization", "Bearer $token")
            .post(body.toString().toRequestBody(jsonMediaType)).build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            AuthResult.Success(parsePostComment(json.getAsJsonObject("data")))
        } else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun reportPost(postId: Int, reason: String, details: String? = null): AuthResult<Unit> = authRequest { token ->
        val body = JsonObject().apply {
            addProperty("reason", reason)
            details?.let { addProperty("details", it) }
        }
        val request = Request.Builder().url("$BASE_URL/api/v1/community/posts/$postId/report").header("Authorization", "Bearer $token")
            .post(body.toString().toRequestBody(jsonMediaType)).build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit)
        else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun getUserPosts(userId: Int, page: Int = 1, size: Int = 20): AuthResult<CommunityFeedResponse> =
        withContext(Dispatchers.IO) {
            try {
                val requestBuilder = Request.Builder().url("$BASE_URL/api/v1/community/posts/user/$userId?page=$page&size=$size").get()
                tokenManager.getAccessToken()?.let { requestBuilder.header("Authorization", "Bearer $it") }
                val response = client.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JsonParser.parseString(responseBody).asJsonObject
                    val data = json.getAsJsonObject("data")
                    val posts = data?.getAsJsonArray("posts")?.map { parseCommunityPost(it.asJsonObject) } ?: emptyList()
                    AuthResult.Success(CommunityFeedResponse(posts = posts, total = data?.get("total")?.asInt ?: 0))
                } else AuthResult.Error(parseError(responseBody, response.code))
            } catch (e: Exception) {
                AuthResult.Error("网络错误: ${e.message}")
            }
        }


    suspend fun getUserModules(userId: Int, page: Int = 1, size: Int = 20): AuthResult<Pair<List<ModuleItem>, Int>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/community/users/$userId/modules?page=$page&size=$size")
                .get().build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val json = JsonParser.parseString(responseBody).asJsonObject
                val data = json.getAsJsonObject("data")
                val total = data?.get("total")?.asInt ?: 0
                val modules = data?.getAsJsonArray("modules")?.map { parseStoreModule(it.asJsonObject) } ?: emptyList()
                AuthResult.Success(Pair(modules, total))
            } else AuthResult.Error(parseError(responseBody, response.code))
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.message}")
        }
    }


    suspend fun editPost(postId: Int, content: String): AuthResult<CommunityPostItem> = authRequest { token ->
        val body = JsonObject().apply { addProperty("content", content) }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/community/posts/$postId")
            .header("Authorization", "Bearer $token")
            .put(body.toString().toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            AuthResult.Success(parseCommunityPost(json.getAsJsonObject("data")))
        } else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun deletePost(postId: Int): AuthResult<Unit> = authRequest { token ->
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/community/posts/$postId")
            .header("Authorization", "Bearer $token")
            .delete()
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit)
        else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun deletePostComment(postId: Int, commentId: Int): AuthResult<Unit> = authRequest { token ->
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/community/posts/$postId/comments/$commentId")
            .header("Authorization", "Bearer $token")
            .delete()
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit)
        else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun getUserActivity(userId: Int): AuthResult<UserActivityInfo> =
        withContext(Dispatchers.IO) {
            try {
                val requestBuilder = Request.Builder().url("$BASE_URL/api/v1/community/users/$userId/activity").get()
                tokenManager.getAccessToken()?.let { requestBuilder.header("Authorization", "Bearer $it") }
                val response = client.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JsonParser.parseString(responseBody).asJsonObject
                    val d = json.getAsJsonObject("data")
                    AuthResult.Success(UserActivityInfo(
                        isOnline = d?.get("is_online")?.asBoolean ?: false,
                        lastSeenAt = d?.get("last_seen_at")?.let { if (it.isJsonNull) null else it.asString },
                        todaySeconds = d?.get("today_seconds")?.asInt ?: 0,
                        monthSeconds = d?.get("month_seconds")?.asInt ?: 0,
                        yearSeconds = d?.get("year_seconds")?.asInt ?: 0,
                    ))
                } else AuthResult.Error(parseError(responseBody, response.code))
            } catch (e: Exception) {
                AuthResult.Error("网络错误: ${e.message}")
            }
        }


    suspend fun sendHeartbeat(): AuthResult<Unit> = authRequest { token ->
        val request = Request.Builder().url("$BASE_URL/api/v1/community/activity/heartbeat").header("Authorization", "Bearer $token")
            .post("".toRequestBody(jsonMediaType)).build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit)
        else AuthResult.Error(parseError(responseBody, response.code))
    }


    suspend fun getCommunityTags(): AuthResult<CommunityTagsResponse> =
        withContext(Dispatchers.IO) {
            try {
                val requestBuilder = Request.Builder().url("$BASE_URL/api/v1/community/tags").get()
                tokenManager.getAccessToken()?.let { requestBuilder.header("Authorization", "Bearer $it") }
                val response = client.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JsonParser.parseString(responseBody).asJsonObject
                    val data = json.getAsJsonObject("data")
                    val categories = mutableMapOf<String, List<String>>()
                    data?.getAsJsonObject("categories")?.entrySet()?.forEach { (key, value) ->
                        categories[key] = value.asJsonArray.map { it.asString }
                    }
                    val all = data?.getAsJsonArray("all")?.map { it.asString } ?: emptyList()
                    AuthResult.Success(CommunityTagsResponse(categories = categories, all = all))
                } else AuthResult.Error(parseError(responseBody, response.code))
            } catch (e: Exception) {
                AuthResult.Error("网络错误: ${e.message}")
            }
        }


    suspend fun getDiscover(): AuthResult<DiscoverResponse> =
        withContext(Dispatchers.IO) {
            try {
                val requestBuilder = Request.Builder().url("$BASE_URL/api/v1/community/discover").get()
                tokenManager.getAccessToken()?.let { requestBuilder.header("Authorization", "Bearer $it") }
                val response = client.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JsonParser.parseString(responseBody).asJsonObject
                    val data = json.getAsJsonObject("data")
                    AuthResult.Success(DiscoverResponse(
                        featuredShowcases = data?.getAsJsonArray("featured_showcases")?.map { parseCommunityPost(it.asJsonObject) } ?: emptyList(),
                        trending = data?.getAsJsonArray("trending")?.map { parseCommunityPost(it.asJsonObject) } ?: emptyList(),
                        latestTutorials = data?.getAsJsonArray("latest_tutorials")?.map { parseCommunityPost(it.asJsonObject) } ?: emptyList(),
                        unansweredQuestions = data?.getAsJsonArray("unanswered_questions")?.map { parseCommunityPost(it.asJsonObject) } ?: emptyList(),
                    ))
                } else AuthResult.Error(parseError(responseBody, response.code))
            } catch (e: Exception) {
                AuthResult.Error("网络错误: ${e.message}")
            }
        }


    suspend fun importRecipe(postId: Int): AuthResult<RecipeImportResult> = authRequest { token ->
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/community/posts/$postId/import-recipe")
            .header("Authorization", "Bearer $token")
            .post("".toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            val authorObj = data?.getAsJsonObject("author")
            AuthResult.Success(RecipeImportResult(
                recipe = data?.getAsJsonObject("recipe")?.toString() ?: "{}",
                appName = data?.get("app_name")?.let { if (it.isJsonNull) null else it.asString },
                appIconUrl = data?.get("app_icon_url")?.let { if (it.isJsonNull) null else it.asString },
                sourceType = data?.get("source_type")?.let { if (it.isJsonNull) null else it.asString },
                authorId = authorObj?.get("id")?.asInt ?: 0,
                authorUsername = authorObj?.get("username")?.let { if (it.isJsonNull) null else it.asString },
            ))
        } else AuthResult.Error(parseError(responseBody, response.code))
    }

    private fun parseCommunityPost(obj: JsonObject): CommunityPostItem {
        val author = obj.getAsJsonObject("author")
        val media = obj.getAsJsonArray("media")?.map { m ->
            val mo = m.asJsonObject
            PostMediaItem(id = mo.get("id")?.asInt ?: 0, mediaType = mo.get("media_type")?.asString ?: "image",
                urlGithub = mo.get("url_github")?.let { if (it.isJsonNull) null else it.asString },
                urlGitee = mo.get("url_gitee")?.let { if (it.isJsonNull) null else it.asString },
                thumbnailUrl = mo.get("thumbnail_url")?.let { if (it.isJsonNull) null else it.asString })
        } ?: emptyList()
        val tags = obj.getAsJsonArray("tags")?.map { it.asString } ?: emptyList()
        val appLinks = obj.getAsJsonArray("app_links")?.map { al ->
            val alo = al.asJsonObject
            val sm = alo.getAsJsonObject("store_module")
            PostAppLinkItem(id = alo.get("id")?.asInt ?: 0, linkType = alo.get("link_type")?.asString ?: "store",
                storeModuleId = alo.get("store_module_id")?.let { if (it.isJsonNull) null else it.asInt },
                appName = alo.get("app_name")?.let { if (it.isJsonNull) null else it.asString },
                appIcon = alo.get("app_icon")?.let { if (it.isJsonNull) null else it.asString },
                appDescription = alo.get("app_description")?.let { if (it.isJsonNull) null else it.asString },
                storeModuleDownloads = sm?.get("downloads")?.asInt,
                storeModuleRating = sm?.get("rating")?.asFloat,
                storeModuleType = sm?.get("module_type")?.asString)
        } ?: emptyList()

        return CommunityPostItem(
            id = obj.get("id")?.asInt ?: 0, content = obj.get("content")?.asString ?: "",
            createdAt = obj.get("created_at")?.let { if (it.isJsonNull) null else it.asString },
            likeCount = obj.get("like_count")?.asInt ?: 0, shareCount = obj.get("share_count")?.asInt ?: 0,
            commentCount = obj.get("comment_count")?.asInt ?: 0, viewCount = obj.get("view_count")?.asInt ?: 0,
            isLiked = obj.get("is_liked")?.asBoolean ?: false,
            isOwnPost = obj.get("is_own_post")?.asBoolean ?: false,
            authorIsFollowing = obj.get("author_is_following")?.asBoolean ?: false,
            authorId = author?.get("id")?.asInt ?: 0, authorUsername = author?.get("username")?.asString ?: "?",
            authorDisplayName = author?.get("display_name")?.let { if (it.isJsonNull) null else it.asString },
            authorAvatarUrl = author?.get("avatar_url")?.let { if (it.isJsonNull) null else it.asString },
            authorIsDeveloper = author?.get("is_developer")?.asBoolean ?: false,
            authorSubscriptionTier = author?.get("subscription_tier")?.let { if (it.isJsonNull) SubscriptionTier.FREE else it.asString } ?: SubscriptionTier.FREE,
            tags = tags, media = media, appLinks = appLinks,

            postType = obj.get("post_type")?.asString ?: "discussion",
            appName = obj.get("app_name")?.let { if (it.isJsonNull) null else it.asString },
            appIconUrl = obj.get("app_icon_url")?.let { if (it.isJsonNull) null else it.asString },
            sourceType = obj.get("source_type")?.let { if (it.isJsonNull) null else it.asString },
            hasRecipe = obj.get("has_recipe")?.asBoolean ?: false,
            recipeImportCount = obj.get("recipe_import_count")?.asInt ?: 0,
            title = obj.get("title")?.let { if (it.isJsonNull) null else it.asString },
            difficulty = obj.get("difficulty")?.let { if (it.isJsonNull) null else it.asString },
            isResolved = obj.get("is_resolved")?.let { if (it.isJsonNull) null else it.asBoolean },
        )
    }

    private fun parsePostComment(obj: JsonObject): PostCommentItem {
        val author = obj.getAsJsonObject("author")
        val replies = obj.getAsJsonArray("replies")?.map { parsePostComment(it.asJsonObject) } ?: emptyList()
        val media = obj.getAsJsonArray("media")?.map { m ->
            val mo = m.asJsonObject
            PostMediaItem(id = mo.get("id")?.asInt ?: 0, mediaType = mo.get("media_type")?.asString ?: "image",
                urlGithub = mo.get("url_github")?.let { if (it.isJsonNull) null else it.asString },
                urlGitee = mo.get("url_gitee")?.let { if (it.isJsonNull) null else it.asString },
                thumbnailUrl = mo.get("thumbnail_url")?.let { if (it.isJsonNull) null else it.asString })
        } ?: emptyList()
        return PostCommentItem(
            id = obj.get("id")?.asInt ?: 0, content = obj.get("content")?.asString ?: "",
            createdAt = obj.get("created_at")?.let { if (it.isJsonNull) null else it.asString },
            likeCount = obj.get("like_count")?.asInt ?: 0, parentId = obj.get("parent_id")?.let { if (it.isJsonNull) null else it.asInt },
            authorId = author?.get("id")?.asInt ?: 0, authorUsername = author?.get("username")?.asString ?: "?",
            authorDisplayName = author?.get("display_name")?.let { if (it.isJsonNull) null else it.asString },
            authorAvatarUrl = author?.get("avatar_url")?.let { if (it.isJsonNull) null else it.asString },
            authorIsDeveloper = author?.get("is_developer")?.asBoolean ?: false,
            authorSubscriptionTier = author?.get("subscription_tier")?.let { if (it.isJsonNull) SubscriptionTier.FREE else it.asString } ?: SubscriptionTier.FREE,
            media = media, replies = replies)
    }

    private fun parseSimpleUserProfile(obj: JsonObject): CommunityUserProfile {
        return CommunityUserProfile(
            id = obj.get("id")?.asInt ?: 0,
            username = obj.get("username")?.asString ?: "",
            displayName = obj.get("display_name")?.let { if (it.isJsonNull) null else it.asString },
            avatarUrl = obj.get("avatar_url")?.let { if (it.isJsonNull) null else it.asString },
            bio = obj.get("bio")?.let { if (it.isJsonNull) null else it.asString },
            appCount = obj.get("published_apps_count")?.asInt ?: 0,
            moduleCount = obj.get("published_modules_count")?.asInt ?: obj.get("module_count")?.asInt ?: 0,
            followerCount = obj.get("follower_count")?.asInt ?: 0,
            followingCount = obj.get("following_count")?.asInt ?: 0,
            isFollowing = obj.get("is_following")?.asBoolean ?: false,
            isDeveloper = obj.get("is_developer")?.asBoolean ?: false,
            subscriptionTier = obj.get("subscription_tier")?.let { if (it.isJsonNull) SubscriptionTier.FREE else it.asString } ?: SubscriptionTier.FREE
        )
    }






    suspend fun listBackups(): AuthResult<BackupListResponse> = authRequest { token ->
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/backups")
            .header("Authorization", "Bearer $token")
            .get().build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            val backups = data?.getAsJsonArray("backups")?.map { el ->
                val obj = el.asJsonObject
                BackupItem(
                    filename = obj.get("filename")?.asString ?: "",
                    size = obj.get("size_str")?.asString ?: obj.get("size")?.asString ?: "0KB",
                    sizeBytes = obj.get("size_bytes")?.asLong ?: obj.get("size")?.asLong ?: 0,
                    downloadUrl = obj.get("download_url")?.asString,
                    createdAt = obj.get("created_at")?.asString
                )
            } ?: emptyList()
            AuthResult.Success(BackupListResponse(
                backups = backups,
                count = data?.get("count")?.asInt ?: 0,
                quota = data?.get("quota")?.asInt ?: 0,
                tier = data?.get("tier")?.asString ?: "free"
            ))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun createBackup(): AuthResult<BackupCreateResult> = authRequest { token ->
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/backups")
            .header("Authorization", "Bearer $token")
            .post("".toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            AuthResult.Success(BackupCreateResult(
                filename = data?.get("filename")?.asString ?: "",
                size = data?.get("size")?.asInt ?: 0,
                downloadUrl = data?.get("download_url")?.asString,
                projectCount = data?.get("project_count")?.asInt ?: 0,
                moduleCount = data?.get("module_count")?.asInt ?: 0,
                message = json.get("message")?.asString ?: "Backup created"
            ))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }


    suspend fun deleteBackup(filename: String): AuthResult<String> = authRequest { token ->
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/backups/$filename")
            .header("Authorization", "Bearer $token")
            .delete().build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            AuthResult.Success(json.get("message")?.asString ?: "已删除")
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }
}





data class RedeemResult(val message: String, val planType: String, val daysAdded: Int)

data class RedeemPreview(
    val currentTier: String,
    val currentPlan: String,
    val currentExpiresAt: String?,
    val currentIsLifetime: Boolean,
    val newTier: String,
    val newPlan: String,
    val newExpiresAt: String?,
    val newIsLifetime: Boolean,
    val isUpgrade: Boolean,
    val codeTier: String,
    val codePlanType: String,
    val durationDays: Int,
)

data class MyAppsResponse(
    val apps: List<AppStoreItem>,
    val count: Int,
    val quota: Int,
    val tier: String
)

data class ActivationRecord(
    val id: Int, val type: String, val planType: String,
    val proStart: String?, val proEnd: String?,
    val note: String?, val createdAt: String?
)

data class DeviceInfo(
    val id: Int, val deviceId: String, val deviceName: String, val deviceOs: String,
    val appVersion: String?, val ipAddress: String?, val country: String?,
    val lastActiveAt: String?, val isCurrent: Boolean = false
)

data class AnnouncementData(
    val id: Int, val title: String, val content: String, val type: String,
    val actionUrl: String?, val actionText: String?,
    val priority: Int, val imageUrl: String?
)

data class AppUpdateInfo(
    val hasUpdate: Boolean, val versionCode: Int, val versionName: String,
    val title: String?, val changelog: String?, val downloadUrl: String,
    val isForceUpdate: Boolean, val fileSize: Long?
)

data class RemoteConfigItem(val key: String, val value: String, val description: String?)

data class CloudProject(
    val id: Int, val name: String, val description: String?,
    val projectKey: String, val packageName: String? = null,
    val githubRepo: String?, val giteeRepo: String?,
    val createdAt: String?, val isActive: Boolean = true,
    val totalInstalls: Int = 0, val totalOpens: Int = 0
)

data class ProjectVersion(
    val id: Int, val versionCode: Int, val versionName: String,
    val title: String?, val changelog: String?,
    val downloadUrlGithub: String?, val downloadUrlGitee: String?,
    val isForceUpdate: Boolean = false, val createdAt: String?
)


data class DirectUploadToken(
    val token: String,
    val expiresAt: String,
    val uploadUrl: String,
    val releaseId: Int,
    val owner: String,
    val repo: String,
    val tag: String,
    val contentType: String = "application/vnd.android.package-archive",
)

data class AnalyticsData(
    val totalInstalls: Int, val totalOpens: Int, val totalActive: Int,
    val totalCrashes: Int, val totalDownloads: Int = 0, val totalDevices: Int = 0,
    val avgDailyActive: Float = 0f,
    val dailyStats: List<DailyStat>,
    val countryDistribution: Map<String, Int> = emptyMap(),
    val versionDistribution: Map<String, Int> = emptyMap(),
    val deviceDistribution: Map<String, Int> = emptyMap(),
    val osDistribution: Map<String, Int> = emptyMap()
)

data class DailyStat(
    val date: String, val installs: Int, val opens: Int, val active: Int,
    val crashes: Int = 0, val downloads: Int = 0
)

data class BackupRecord(
    val id: Int, val platform: String, val status: String,
    val repoUrl: String?, val fileSize: Long,
    val createdAt: String?
)

data class ManifestSyncResult(
    val success: Boolean, val manifestVersion: Int,
    val syncedAt: String?, val conflict: Boolean = false
)

data class ManifestDownloadResult(
    val manifestJson: String?, val manifestVersion: Int,
    val syncedAt: String?
)

data class PushHistoryItem(
    val id: Int,
    val title: String,
    val body: String,
    val targetType: String,
    val sentCount: Int,
    val createdAt: String?
)



data class ProjectActivationCode(
    val id: Int, val code: String, val status: String,
    val maxUses: Int, val usedCount: Int, val deviceId: String?,
    val createdAt: String, val usedAt: String?
)

data class ProjectAnnouncement(
    val id: Int, val title: String, val content: String,
    val isActive: Boolean, val priority: Int, val createdAt: String
)

data class ProjectConfig(
    val id: Int, val key: String, val value: String,
    val description: String?, val isActive: Boolean
)

data class ProjectWebhook(
    val id: Int, val url: String, val events: List<String>,
    val secret: String?, val isActive: Boolean,
    val failureCount: Int, val lastTriggeredAt: String?
)












data class ModuleItem(
    val id: Int,
    val name: String,
    val description: String? = null,
    val icon: String? = null,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val versionName: String? = null,
    val versionCode: Int = 0,
    val downloads: Int = 0,
    val rating: Float = 0f,
    val ratingCount: Int = 0,
    val likeCount: Int = 0,
    val isFeatured: Boolean = false,
    val authorName: String = "",
    val authorId: Int = 0,
    val shareCode: String? = null,
    val moduleType: String = "extension",
    val isApproved: Boolean = true,
    val visibility: String? = null,
    val reviewMode: String? = null,
    val reviewStatus: String? = null,
    val verificationStatus: String? = null,
    val reviewSortPenalty: Float = 0f,
    val rejectionReason: String? = null,
    val adminReplyText: String? = null,
    val runtimeKey: String? = null,
    val screenshots: List<String> = emptyList(),

    val userVote: String? = null,
    val isFavorited: Boolean = false,
    val isLiked: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {

    val downloadCount: Int get() = downloads
    val averageRating: Float get() = rating
}


typealias StoreModuleInfo = ModuleItem


typealias CommunityModuleDetail = ModuleItem

data class RemoteScriptInfo(
    val id: Int, val name: String, val description: String?,
    val code: String, val runAt: String, val urlPattern: String?,
    val priority: Int, val isActive: Boolean, val version: Int,
    val createdAt: String?, val updatedAt: String?
)



data class ModuleComment(
    val id: Int, val content: String, val userId: Int,
    val userName: String, val userAvatar: String?,
    val parentId: Int? = null,
    val createdAt: String?, val updatedAt: String?,
    val replies: List<ModuleComment> = emptyList()
)

data class CommunityUserProfile(
    val id: Int, val username: String, val displayName: String?,
    val avatarUrl: String?, val bio: String?,
    val appCount: Int = 0, val moduleCount: Int = 0,
    val followerCount: Int = 0,
    val followingCount: Int = 0, val isFollowing: Boolean = false,
    val isDeveloper: Boolean = false,
    val subscriptionTier: String = SubscriptionTier.FREE,
    val createdAt: String? = null
)

data class NotificationItem(
    val id: Int, val type: String, val title: String?,
    val content: String?, val refType: String?,
    val refId: Int?, val actorId: Int?,
    val isRead: Boolean, val createdAt: String?
)

data class FeedItem(
    val id: Int, val type: String, val actorName: String,
    val actorAvatar: String?, val targetName: String?,
    val targetId: Int?, val createdAt: String?
)



data class AppStoreListResponse(
    val total: Int, val page: Int, val size: Int,
    val apps: List<AppStoreItem>,
    val categories: List<String> = emptyList()
)

data class AppStoreItem(
    val id: Int,
    val name: String,
    val icon: String? = null,
    val category: String = "other",
    val tags: List<String> = emptyList(),
    val versionName: String = "1.0",
    val versionCode: Int = 0,
    val packageName: String? = null,
    val downloads: Int = 0,
    val rating: Float = 0f,
    val ratingCount: Int = 0,
    val likeCount: Int = 0,
    val isFeatured: Boolean = false,
    val screenshots: List<String> = emptyList(),
    val authorName: String = "Unknown",
    val authorId: Int = 0,
    val createdAt: String? = null,

    val description: String? = null,
    val videoUrl: String? = null,
    val apkUrlGithub: String? = null,
    val apkUrlGitee: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    val groupChatUrl: String? = null,
    val paymentQrUrl: String? = null,
    val websiteUrl: String? = null,
    val privacyPolicyUrl: String? = null,
    val visibility: String? = null,
    val reviewMode: String? = null,
    val reviewStatus: String? = null,
    val verificationStatus: String? = null,
    val appSourceType: String? = null,
    val runtimeKey: String? = null,
    val reviewSortPenalty: Float = 0f,
    val rejectionReason: String? = null,
    val adminReplyText: String? = null,
    val sourceBundleUrl: String? = null,
    val buildManifestJson: String? = null,
    val legacyProjectId: Int? = null,
    val lastReviewedAt: String? = null,
)



data class LikeResponse(
    val liked: Boolean,
    val likeCount: Int
)

data class AppReviewItem(
    val id: Int,
    val rating: Int,
    val comment: String? = null,
    val authorName: String = "Unknown",
    val authorId: Int = 0,
    val deviceModel: String? = null,
    val ipAddress: String? = null,
    val createdAt: String? = null
)

data class AppReviewsResponse(
    val total: Int,
    val page: Int,
    val reviews: List<AppReviewItem>,
    val ratingDistribution: Map<Int, Int> = emptyMap()
)



data class PushHistoryResponse(
    val total: Int,
    val page: Int,
    val dailyUsed: Int,
    val dailyLimit: Int,
    val tier: String,
    val records: List<PushHistoryItem>
)





data class BackupListResponse(
    val backups: List<BackupItem>,
    val count: Int = 0,
    val quota: Int = 0,
    val tier: String = "free"
)

data class BackupItem(
    val filename: String,
    val size: String = "0KB",
    val sizeBytes: Long = 0,
    val downloadUrl: String? = null,
    val createdAt: String? = null
)

data class BackupCreateResult(
    val filename: String,
    val size: Int = 0,
    val downloadUrl: String? = null,
    val projectCount: Int = 0,
    val moduleCount: Int = 0,
    val message: String = ""
)



data class CommunityFeedResponse(val posts: List<CommunityPostItem>, val total: Int)

data class CommunityPostItem(
    val id: Int,
    val content: String,
    val createdAt: String? = null,
    val likeCount: Int = 0,
    val shareCount: Int = 0,
    val commentCount: Int = 0,
    val viewCount: Int = 0,
    val isLiked: Boolean = false,
    val isOwnPost: Boolean = false,
    val authorIsFollowing: Boolean = false,
    val authorId: Int = 0,
    val authorUsername: String = "?",
    val authorDisplayName: String? = null,
    val authorAvatarUrl: String? = null,
    val authorIsDeveloper: Boolean = false,
    val authorSubscriptionTier: String = SubscriptionTier.FREE,
    val tags: List<String> = emptyList(),
    val media: List<PostMediaItem> = emptyList(),
    val appLinks: List<PostAppLinkItem> = emptyList(),

    val postType: String = "discussion",
    val appName: String? = null,
    val appIconUrl: String? = null,
    val sourceType: String? = null,
    val hasRecipe: Boolean = false,
    val recipeImportCount: Int = 0,
    val title: String? = null,
    val difficulty: String? = null,
    val isResolved: Boolean? = null,
)

data class PostMediaInput(
    val mediaType: String = "image",
    val urlGithub: String? = null,
    val urlGitee: String? = null,
    val thumbnailUrl: String? = null
)

data class PostMediaItem(
    val id: Int,
    val mediaType: String = "image",
    val urlGithub: String? = null,
    val urlGitee: String? = null,
    val thumbnailUrl: String? = null
)

data class PostAppLinkInput(
    val linkType: String = "store",
    val storeModuleId: Int? = null,
    val appName: String? = null,
    val appIcon: String? = null,
    val appDescription: String? = null
)

data class PostAppLinkItem(
    val id: Int,
    val linkType: String = "store",
    val storeModuleId: Int? = null,
    val appName: String? = null,
    val appIcon: String? = null,
    val appDescription: String? = null,
    val storeModuleDownloads: Int? = null,
    val storeModuleRating: Float? = null,
    val storeModuleType: String? = null
)

data class PostLikeResult(val liked: Boolean, val likeCount: Int)
data class CommentLikeResult(val liked: Boolean, val likeCount: Int)
data class BookmarkResult(val bookmarked: Boolean)


data class DiscoverResponse(
    val featuredShowcases: List<CommunityPostItem> = emptyList(),
    val trending: List<CommunityPostItem> = emptyList(),
    val latestTutorials: List<CommunityPostItem> = emptyList(),
    val unansweredQuestions: List<CommunityPostItem> = emptyList(),
)


data class RecipeImportResult(
    val recipe: String,
    val appName: String? = null,
    val appIconUrl: String? = null,
    val sourceType: String? = null,
    val authorId: Int = 0,
    val authorUsername: String? = null,
)


data class CommunityTagsResponse(
    val categories: Map<String, List<String>> = emptyMap(),
    val all: List<String> = emptyList(),
)

data class PostCommentItem(
    val id: Int,
    val content: String,
    val createdAt: String? = null,
    val likeCount: Int = 0,
    val parentId: Int? = null,
    val authorId: Int = 0,
    val authorUsername: String = "?",
    val authorDisplayName: String? = null,
    val authorAvatarUrl: String? = null,
    val authorIsDeveloper: Boolean = false,
    val authorSubscriptionTier: String = SubscriptionTier.FREE,
    val media: List<PostMediaItem> = emptyList(),
    val replies: List<PostCommentItem> = emptyList()
)


data class CommentsResponse(
    val total: Int = 0,
    val page: Int = 1,
    val comments: List<PostCommentItem> = emptyList()
)

data class UserActivityInfo(
    val isOnline: Boolean = false,
    val lastSeenAt: String? = null,
    val todaySeconds: Int = 0,
    val monthSeconds: Int = 0,
    val yearSeconds: Int = 0
)





data class ActivationCode(
    val id: Int = 0,
    val code: String,
    val appId: Int = 0,
    val isUsed: Boolean = false,
    val usedByDeviceId: String? = null,
    val usedByUserId: String? = null,
    val usedAt: String? = null,
    val createdAt: String? = null,
    val expiresAt: String? = null,
    val maxUses: Int = 1,
    val currentUses: Int = 0
)

data class ActivationSettings(
    val enabled: Boolean = false,
    val deviceBindingEnabled: Boolean = false,
    val maxDevicesPerCode: Int = 1,
    val activationHelpUrl: String? = null,
    val codes: List<ActivationCode> = emptyList(),
    val totalCodes: Int = 0,
    val usedCodes: Int = 0
)

data class Announcement(
    val id: Int = 0,
    val appId: Int = 0,
    val title: String = "",
    val content: String = "",
    val type: String = "info",
    val isActive: Boolean = true,
    val isPinned: Boolean = false,
    val createdAt: String? = null,
    val expiresAt: String? = null,
    val viewCount: Int = 0
)

data class AnnouncementTemplate(
    val id: String,
    val name: String,
    val icon: String,
    val title: String,
    val content: String,
    val type: String
)

data class UpdateConfig(
    val id: Int = 0,
    val appId: Int = 0,
    val latestVersionName: String = "",
    val latestVersionCode: Int = 0,
    val updateTitle: String = "",
    val updateContent: String = "",
    val apkUrl: String? = null,
    val sourceAppId: Int? = null,
    val sourceAppName: String? = null,
    val isForceUpdate: Boolean = false,
    val minVersionCode: Int = 0,
    val templateId: String = "simple",
    val isActive: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class UpdateTemplate(
    val id: String,
    val name: String,
    val preview: String,
    val style: String
)

data class AppUser(
    val id: String,
    val deviceModel: String? = null,
    val osVersion: String? = null,
    val appVersion: String? = null,
    val country: String? = null,
    val region: String? = null,
    val city: String? = null,
    val ipAddress: String? = null,
    val firstSeenAt: String? = null,
    val lastSeenAt: String? = null,
    val activationCode: String? = null,
    val externalUserId: String? = null,
    val isActive: Boolean = true
)

data class ActivationCodeBatchResult(
    val created: Int = 0,
    val skipped: Int = 0,
    val quotaUsed: Int = 0,
    val quotaTotal: Int = 0,
)

data class AppQuotaInfo(
    val tier: String = "free",
    val retentionDays: Int = 0,
    val appPublishLimit: Int = 0,
    val activationCodesTotalLimit: Int = 0,
    val activationCodesDailyLimit: Int = 0,
    val activationCodesCurrent: Int = 0,
    val announcementDailyLimit: Int = 0,
    val updateDailyLimit: Int = 0,
    val reviewSubmissionDailyLimit: Int = 0,
    val appReviewSubmissionDailyLimit: Int = 0,
    val runtimeEventsDailyLimit: Int = 0,
    val reviewStatus: String? = null,
    val verificationStatus: String? = null,
    val visibility: String? = null,
)

data class AppBanItem(
    val id: Int,
    val ruleType: String,
    val targetValue: String,
    val reason: String? = null,
    val revokedAt: String? = null,
    val createdAt: String? = null,
)

data class ReviewSubmissionInfo(
    val id: Int,
    val moduleId: Int = 0,
    val reviewMode: String = "public_first",
    val requestedVisibility: String = "public",
    val status: String = "pending",
    val submitMessage: String? = null,
    val rejectionReason: String? = null,
    val adminReplyText: String? = null,
    val reviewedBy: Int? = null,
    val decidedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val submissionAssets: List<String> = emptyList(),
    val decisionAssets: List<String> = emptyList(),
)

data class GeoDistribution(
    val country: String,
    val countryCode: String,
    val count: Int,
    val percentage: Float,
    val regions: List<RegionInfo> = emptyList()
)

data class RegionInfo(
    val region: String,
    val count: Int,
    val percentage: Float
)
