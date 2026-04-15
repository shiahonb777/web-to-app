package com.webtoapp.core.cloud

import android.os.Build
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.webtoapp.core.cloud.api.ActivationApi
import com.webtoapp.core.cloud.api.BackupApi
import com.webtoapp.core.cloud.api.NotificationApi
import com.webtoapp.core.cloud.api.ProjectApi
import com.webtoapp.core.cloud.internal.CloudApiSupport
import com.webtoapp.core.cloud.internal.CloudJsonParser
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
import java.util.concurrent.TimeUnit

/**
 * Note: brief English comment.
 *
 * Note: brief English comment.
 */
class CloudApiClient(private val tokenManager: TokenManager, context: android.content.Context? = null) {

    companion object {
        const val BASE_URL = "https://api.shiaho.sbs"
        private const val TAG = "CloudApiClient"
    }

    private val apiSupport = CloudApiSupport(tokenManager, context)
    private val jsonMediaType = apiSupport.jsonMediaType
    private val client = apiSupport.client
    private val parser = CloudJsonParser()
    private val activationApi = ActivationApi(apiSupport)
    private val backupApi = BackupApi(apiSupport)
    private val notificationApi = NotificationApi(apiSupport)
    private val projectApi = ProjectApi(apiSupport, parser)

    suspend fun redeemCode(code: String): AuthResult<RedeemResult> = activationApi.redeemCode(code)

    suspend fun previewRedeemCode(code: String): AuthResult<RedeemPreview> = activationApi.previewRedeemCode(code)

    suspend fun getActivationHistory(): AuthResult<List<ActivationRecord>> = activationApi.getActivationHistory()

    suspend fun getDevices(): AuthResult<List<DeviceInfo>> = activationApi.getDevices()

    suspend fun removeDevice(deviceId: Int): AuthResult<String> = activationApi.removeDevice(deviceId)

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
                    .url("$BASE_URL/api/v1/remote-config")
                    .get()
                tokenManager.getAccessToken()?.let { reqBuilder.header("Authorization", "Bearer $it") }
                val response = client.newCall(reqBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val json = JsonParser.parseString(responseBody).asJsonObject
                    val data = json.getAsJsonObject("data")
                    val configsObj = data?.getAsJsonObject("configs")
                    if (configsObj == null) return@withContext AuthResult.Success(emptyList())
                    val list = configsObj.entrySet().map { entry ->
                        RemoteConfigItem(
                            key = entry.key,
                            value = entry.value.asString,
                            description = null
                        )
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

    suspend fun listProjects(): AuthResult<List<CloudProject>> = projectApi.listProjects()

    suspend fun createProject(
        name: String,
        description: String? = null,
        githubRepo: String? = null,
        giteeRepo: String? = null,
    ): AuthResult<CloudProject> = projectApi.createProject(name, description, githubRepo, giteeRepo)

    suspend fun deleteProject(projectId: Int): AuthResult<String> = projectApi.deleteProject(projectId)

    suspend fun updateProject(
        projectId: Int,
        name: String? = null,
        description: String? = null,
        githubRepo: String? = null,
        giteeRepo: String? = null,
    ): AuthResult<CloudProject> = projectApi.updateProject(projectId, name, description, githubRepo, giteeRepo)

    suspend fun publishVersion(
        projectId: Int,
        apkFile: File,
        versionCode: Int,
        versionName: String,
        title: String? = null,
        changelog: String? = null,
        uploadTo: String = "github"
    ): AuthResult<ProjectVersion> =
        projectApi.publishVersion(projectId, apkFile, versionCode, versionName, title, changelog, uploadTo)

    suspend fun listVersions(projectId: Int): AuthResult<List<ProjectVersion>> = projectApi.listVersions(projectId)

    /**
     * Step 1: Request a short-lived upload token from our server.
     * The server generates a GitHub App installation token (1h expiry)
     * and creates the release on GitHub.
     */
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

    /**
     * Step 2: Upload APK directly to GitHub using the temporary token.
     * The file goes from user's device → GitHub, not through our server.
     */
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

    /**
     * Step 3: Confirm the upload to our server so it records the version.
     */
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

    /**
     * Upload any media asset (image, video, icon) to GitHub via our server's asset token.
     * Returns the download URL of the uploaded asset.
     */
    suspend fun uploadAsset(
        file: java.io.File,
        contentType: String = "image/png",
        onProgress: (Float) -> Unit = {}
    ): AuthResult<String> = authRequest {
        val tokenBody = okhttp3.FormBody.Builder()
            .add("file_name", file.name)
            .add("content_type", contentType)
            .build()

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

    suspend fun generateProjectCodes(
        projectId: Int,
        count: Int = 10,
        maxUses: Int = 1,
        prefix: String = "",
    ): AuthResult<List<ProjectActivationCode>> = projectApi.generateProjectCodes(projectId, count, maxUses, prefix)

    suspend fun listProjectCodes(
        projectId: Int,
        status: String? = null,
        page: Int = 1,
    ): AuthResult<List<ProjectActivationCode>> = projectApi.listProjectCodes(projectId, status, page)

    suspend fun createProjectAnnouncement(
        projectId: Int,
        title: String,
        content: String,
        priority: Int = 0,
    ): AuthResult<ProjectAnnouncement> = projectApi.createProjectAnnouncement(projectId, title, content, priority)

    suspend fun listProjectAnnouncements(projectId: Int): AuthResult<List<ProjectAnnouncement>> =
        projectApi.listProjectAnnouncements(projectId)

    suspend fun updateProjectAnnouncement(
        projectId: Int,
        annId: Int,
        title: String? = null,
        content: String? = null,
        isActive: Boolean? = null,
    ): AuthResult<String> = projectApi.updateProjectAnnouncement(projectId, annId, title, content, isActive)

    suspend fun deleteProjectAnnouncement(projectId: Int, annId: Int): AuthResult<String> =
        projectApi.deleteProjectAnnouncement(projectId, annId)

    suspend fun createProjectConfig(
        projectId: Int,
        key: String,
        value: String,
        description: String? = null,
    ): AuthResult<ProjectConfig> = projectApi.createProjectConfig(projectId, key, value, description)

    suspend fun listProjectConfigs(projectId: Int): AuthResult<List<ProjectConfig>> =
        projectApi.listProjectConfigs(projectId)

    suspend fun updateProjectConfig(
        projectId: Int,
        cfgId: Int,
        value: String? = null,
        description: String? = null,
        isActive: Boolean? = null,
    ): AuthResult<String> = projectApi.updateProjectConfig(projectId, cfgId, value, description, isActive)

    suspend fun deleteProjectConfig(projectId: Int, cfgId: Int): AuthResult<String> =
        projectApi.deleteProjectConfig(projectId, cfgId)

    suspend fun createWebhook(
        projectId: Int,
        webhookUrl: String,
        events: List<String>,
        secret: String? = null,
    ): AuthResult<ProjectWebhook> = projectApi.createWebhook(projectId, webhookUrl, events, secret)

    suspend fun listWebhooks(projectId: Int): AuthResult<List<ProjectWebhook>> = projectApi.listWebhooks(projectId)

    suspend fun deleteWebhook(projectId: Int, webhookId: Int): AuthResult<String> =
        projectApi.deleteWebhook(projectId, webhookId)

    suspend fun verifySubscription(purchaseToken: String, productId: String): AuthResult<String> =
        activationApi.verifySubscription(purchaseToken, productId)

    suspend fun voteModule(moduleId: Int, voteType: String = "up"): AuthResult<String> = authRequest {
        val body = JsonObject().apply { addProperty("vote_type", voteType) }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/community/modules/$moduleId/vote")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            AuthResult.Success("投票成功")
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
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

    suspend fun listFavorites(page: Int = 1, size: Int = 20): AuthResult<Pair<List<StoreModuleInfo>, Int>> = authRequest {
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
                // Note: brief English comment.
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
                        teamBadges = d?.let { parseTeamBadges(it) } ?: emptyList(),
                        createdAt = d?.get("created_at")?.let { if (it.isJsonNull) null else it.asString }
                    ))
                } else AuthResult.Error(parseError(responseBody, response.code))
            } catch (e: Exception) {
                AuthResult.Error("网络错误: ${e.message}")
            }
        }
    suspend fun listNotifications(
        page: Int = 1,
        size: Int = 20,
        unreadOnly: Boolean = false,
    ): AuthResult<Pair<List<NotificationItem>, Int>> = notificationApi.listNotifications(page, size, unreadOnly)
    suspend fun getUnreadNotificationCount(): AuthResult<Int> = notificationApi.getUnreadNotificationCount()
    suspend fun markNotificationRead(notificationId: Int): AuthResult<Unit> =
        notificationApi.markNotificationRead(notificationId)
    suspend fun markAllNotificationsRead(): AuthResult<Unit> = notificationApi.markAllNotificationsRead()
    private suspend fun <T> authRequest(block: suspend (token: String) -> AuthResult<T>): AuthResult<T> =
        apiSupport.authRequest(block)

    private fun parseProject(obj: JsonObject): CloudProject = parser.parseProject(obj)

    private fun parseActivationCode(obj: JsonObject): ProjectActivationCode = parser.parseActivationCode(obj)

    private fun parseProjectAnnouncement(obj: JsonObject?): ProjectAnnouncement = parser.parseProjectAnnouncement(obj)

    private fun parseWebhook(obj: JsonObject?): ProjectWebhook = parser.parseWebhook(obj)
    suspend fun uploadManifest(
        projectId: Int,
        manifestJson: String,
        manifestVersion: Int,
    ): AuthResult<ManifestSyncResult> = projectApi.uploadManifest(projectId, manifestJson, manifestVersion)
    suspend fun downloadManifest(projectId: Int): AuthResult<ManifestDownloadResult> =
        projectApi.downloadManifest(projectId)
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
        versionCode: Int, shareCode: String
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

    /** Note: brief English comment.
     *
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     */
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

                // Note: brief English comment.
                val downloadedCode = downloadGzipModule(storageUrlGithub)
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
                // Note: brief English comment.
                if (bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()) {
                    java.util.zip.GZIPInputStream(java.io.ByteArrayInputStream(bytes)).use { gzis ->
                        gzis.bufferedReader(Charsets.UTF_8).readText()
                    }
                } else {
                    // Note: brief English comment.
                    String(bytes, Charsets.UTF_8)
                }
            } else null
        } catch (e: Exception) {
            AppLogger.e(TAG, "Download gzip module failed from $url", e)
            null
        }
    }

    private fun parseStoreModule(obj: JsonObject?): StoreModuleInfo = parser.parseStoreModule(obj)
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
                Result.success(AppReviewsResponse(
                    total = data?.get("total")?.asInt ?: 0,
                    page = data?.get("page")?.asInt ?: 1,
                    reviews = reviews
                ))
            } else {
                Result.failure(Exception(parseError(responseBody, response.code)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun likeStoreModule(moduleId: Int): AuthResult<LikeResponse> = authRequest {
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

    private fun parseRemoteScript(obj: JsonObject?): RemoteScriptInfo = parser.parseRemoteScript(obj)
    suspend fun listBackups(projectId: Int): AuthResult<List<BackupRecord>> = backupApi.listBackups(projectId)
    suspend fun createBackup(projectId: Int, platform: String, zipFile: File): AuthResult<BackupRecord> =
        backupApi.createBackup(projectId, platform, zipFile)

    private fun parseError(body: String, statusCode: Int = 0): String =
        apiSupport.parseError(body, statusCode)
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
            addProperty("module_id", appId)
            addProperty("reason", reason)
            description?.let { addProperty("description", it) }
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/community/reports")
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
            .url("$BASE_URL/api/v1/modules/$appId")
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
            .url("$BASE_URL/api/v1/modules/$appId")
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
        privacyPolicyUrl: String? = null
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
                    appId = appId, isUsed = o.get("is_used")?.asBoolean ?: false,
                    usedByDeviceId = o.get("used_by_device_id")?.asString,
                    usedByUserId = o.get("used_by_user_id")?.asString,
                    usedAt = o.get("used_at")?.asString, createdAt = o.get("created_at")?.asString,
                    expiresAt = o.get("expires_at")?.asString,
                    maxUses = o.get("max_uses")?.asInt ?: 1,
                    currentUses = o.get("current_uses")?.asInt ?: 0
                )
            } ?: emptyList()
            AuthResult.Success(ActivationSettings(
                enabled = json.get("enabled")?.asBoolean ?: false,
                deviceBindingEnabled = json.get("device_binding_enabled")?.asBoolean ?: false,
                maxDevicesPerCode = json.get("max_devices_per_code")?.asInt ?: 1,
                codes = codes, totalCodes = json.get("total_codes")?.asInt ?: codes.size,
                usedCodes = json.get("used_codes")?.asInt ?: codes.count { it.isUsed }
            ))
        } else AuthResult.Error(parseError(body, response.code))
    }
    suspend fun createActivationCodes(appId: Int, codes: List<String>, maxUses: Int = 1): AuthResult<List<ActivationCode>> = authRequest {
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
            val arr = JsonParser.parseString(body).asJsonObject.getAsJsonArray("data")
            AuthResult.Success(arr?.map { c ->
                val o = c.asJsonObject
                ActivationCode(id = o.get("id")?.asInt ?: 0, code = o.get("code")?.asString ?: "", appId = appId)
            } ?: emptyList())
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
    suspend fun updateActivationSettings(appId: Int, enabled: Boolean, deviceBinding: Boolean, maxDevices: Int): AuthResult<Unit> = authRequest {
        val jsonBody = JsonObject().apply {
            addProperty("enabled", enabled)
            addProperty("device_binding_enabled", deviceBinding)
            addProperty("max_devices_per_code", maxDevices)
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
                    isActive = o.get("is_active")?.asBoolean ?: true,
                    isPinned = o.get("is_pinned")?.asBoolean ?: false,
                    createdAt = o.get("created_at")?.asString,
                    expiresAt = o.get("expires_at")?.asString,
                    viewCount = o.get("view_count")?.asInt ?: 0
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
                latestVersionName = o.get("latest_version_name")?.asString ?: "",
                latestVersionCode = o.get("latest_version_code")?.asInt ?: 0,
                updateTitle = o.get("update_title")?.asString ?: "",
                updateContent = o.get("update_content")?.asString ?: "",
                apkUrl = o.get("apk_url")?.asString,
                isForceUpdate = o.get("is_force_update")?.asBoolean ?: false,
                minVersionCode = o.get("min_version_code")?.asInt ?: 0,
                templateId = o.get("template_id")?.asString ?: "simple",
                isActive = o.get("is_active")?.asBoolean ?: false
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
                    deviceModel = o.get("device_model")?.asString,
                    osVersion = o.get("os_version")?.asString,
                    appVersion = o.get("app_version")?.asString,
                    country = o.get("country")?.asString, region = o.get("region")?.asString,
                    city = o.get("city")?.asString, ipAddress = o.get("ip_address")?.asString,
                    firstSeenAt = o.get("first_seen_at")?.asString,
                    lastSeenAt = o.get("last_seen_at")?.asString,
                    activationCode = o.get("activation_code")?.asString,
                    isActive = o.get("is_active")?.asBoolean ?: true
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
                    countryCode = o.get("country_code")?.asString ?: "",
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

    private fun parseStoreApp(obj: JsonObject): AppStoreItem = parser.parseStoreApp(obj)
    suspend fun listTeams(): AuthResult<TeamListResponse> = authRequest { token ->
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/teams")
            .header("Authorization", "Bearer $token")
            .get().build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            val teams = data?.getAsJsonArray("teams")?.map { el ->
                val t = el.asJsonObject
                TeamItem(
                    id = t.get("id")?.asInt ?: 0,
                    name = t.get("name")?.asString ?: "",
                    description = t.get("description")?.let { if (it.isJsonNull) null else it.asString },
                    ownerName = t.get("owner_name")?.asString ?: "?",
                    ownerId = t.get("owner_id")?.asInt ?: 0,
                    memberCount = t.get("member_count")?.asInt ?: 0,
                    pendingRequests = t.get("pending_requests")?.asInt ?: 0,
                    isPublic = t.get("is_public")?.asBoolean ?: true,
                    createdAt = t.get("created_at")?.let { if (it.isJsonNull) null else it.asString },
                )
            } ?: emptyList()

            AuthResult.Success(TeamListResponse(
                teams = teams,
                quotaUsed = data?.get("quota_used")?.asInt ?: 0,
                quotaLimit = data?.get("quota_limit")?.asInt ?: 0,
                memberLimit = data?.get("member_limit")?.asInt ?: 0,
                tier = data?.get("tier")?.asString ?: "free",
            ))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }
    suspend fun createTeam(name: String, description: String? = null): AuthResult<TeamItem> = authRequest { token ->
        val body = JsonObject().apply {
            addProperty("name", name)
            description?.let { addProperty("description", it) }
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/teams")
            .header("Authorization", "Bearer $token")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val d = json.getAsJsonObject("data")
            AuthResult.Success(TeamItem(
                id = d?.get("id")?.asInt ?: 0,
                name = d?.get("name")?.asString ?: name,
                description = d?.get("description")?.let { if (it.isJsonNull) null else it.asString },
                ownerName = d?.get("owner_name")?.asString ?: "?",
                ownerId = d?.get("owner_id")?.asInt ?: 0,
                memberCount = d?.get("member_count")?.asInt ?: 1,
            ))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }
    suspend fun getTeamMembers(teamId: Int): AuthResult<List<TeamMemberItem>> = authRequest { token ->
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/teams/$teamId/members")
            .header("Authorization", "Bearer $token")
            .get().build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            val members = data?.getAsJsonArray("members")?.map { el ->
                val m = el.asJsonObject
                TeamMemberItem(
                    id = m.get("id")?.asInt ?: 0,
                    userId = m.get("user_id")?.asInt ?: 0,
                    username = m.get("username")?.asString ?: "?",
                    displayName = m.get("display_name")?.let { if (it.isJsonNull) null else it.asString },
                    avatarUrl = m.get("avatar_url")?.let { if (it.isJsonNull) null else it.asString },
                    role = m.get("role")?.asString ?: "viewer",
                    contribution = m.get("contribution")?.asInt ?: 0,
                    createdAt = m.get("created_at")?.let { if (it.isJsonNull) null else it.asString },
                )
            } ?: emptyList()
            AuthResult.Success(members)
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }
    suspend fun inviteTeamMember(teamId: Int, username: String, role: String = "viewer"): AuthResult<Unit> = authRequest { token ->
        val body = JsonObject().apply {
            addProperty("username", username)
            addProperty("role", role)
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/teams/$teamId/members")
            .header("Authorization", "Bearer $token")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit)
        else AuthResult.Error(parseError(responseBody, response.code))
    }
    suspend fun removeTeamMember(teamId: Int, memberId: Int): AuthResult<Unit> = authRequest { token ->
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/teams/$teamId/members/$memberId")
            .header("Authorization", "Bearer $token")
            .delete().build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit)
        else AuthResult.Error(parseError(responseBody, response.code))
    }
    suspend fun deleteTeam(teamId: Int): AuthResult<Unit> = authRequest { token ->
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/teams/$teamId")
            .header("Authorization", "Bearer $token")
            .delete().build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit)
        else AuthResult.Error(parseError(responseBody, response.code))
    }
    suspend fun searchTeams(query: String, page: Int = 1, size: Int = 20): AuthResult<TeamSearchResponse> = authRequest { token ->
        val url = "$BASE_URL/api/v1/teams/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}&page=$page&size=$size"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get().build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            val teams = data?.getAsJsonArray("teams")?.map { el ->
                val t = el.asJsonObject
                TeamSearchItem(
                    id = t.get("id")?.asInt ?: 0,
                    name = t.get("name")?.asString ?: "",
                    description = t.get("description")?.let { if (it.isJsonNull) null else it.asString },
                    ownerName = t.get("owner_name")?.asString ?: "?",
                    ownerId = t.get("owner_id")?.asInt ?: 0,
                    memberCount = t.get("member_count")?.asInt ?: 0,
                    isMember = t.get("is_member")?.asBoolean ?: false,
                    hasPendingRequest = t.get("has_pending_request")?.asBoolean ?: false,
                    createdAt = t.get("created_at")?.let { if (it.isJsonNull) null else it.asString },
                )
            } ?: emptyList()
            AuthResult.Success(TeamSearchResponse(
                teams = teams,
                total = data?.get("total")?.asInt ?: 0,
                page = data?.get("page")?.asInt ?: 1,
            ))
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }
    suspend fun requestJoinTeam(teamId: Int, message: String? = null): AuthResult<Unit> = authRequest { token ->
        val body = JsonObject().apply {
            message?.let { addProperty("message", it) }
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/teams/$teamId/join")
            .header("Authorization", "Bearer $token")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit)
        else AuthResult.Error(parseError(responseBody, response.code))
    }
    suspend fun getJoinRequests(teamId: Int, status: String = "pending"): AuthResult<List<TeamJoinRequestItem>> = authRequest { token ->
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/teams/$teamId/join-requests?status=$status")
            .header("Authorization", "Bearer $token")
            .get().build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            val requests = data?.getAsJsonArray("requests")?.map { el ->
                val r = el.asJsonObject
                TeamJoinRequestItem(
                    id = r.get("id")?.asInt ?: 0,
                    userId = r.get("user_id")?.asInt ?: 0,
                    username = r.get("username")?.asString ?: "?",
                    displayName = r.get("display_name")?.let { if (it.isJsonNull) null else it.asString },
                    avatarUrl = r.get("avatar_url")?.let { if (it.isJsonNull) null else it.asString },
                    message = r.get("message")?.let { if (it.isJsonNull) null else it.asString },
                    status = r.get("status")?.asString ?: "pending",
                    createdAt = r.get("created_at")?.let { if (it.isJsonNull) null else it.asString },
                )
            } ?: emptyList()
            AuthResult.Success(requests)
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }
    suspend fun reviewJoinRequest(teamId: Int, requestId: Int, action: String, role: String = "viewer"): AuthResult<Unit> = authRequest { token ->
        val body = JsonObject().apply {
            addProperty("action", action)
            addProperty("role", role)
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/teams/$teamId/join-requests/$requestId")
            .header("Authorization", "Bearer $token")
            .put(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit)
        else AuthResult.Error(parseError(responseBody, response.code))
    }
    suspend fun getTeamRanking(teamId: Int): AuthResult<List<TeamRankingItem>> = authRequest { token ->
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/teams/$teamId/ranking")
            .header("Authorization", "Bearer $token")
            .get().build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            val ranking = data?.getAsJsonArray("ranking")?.map { el ->
                val r = el.asJsonObject
                TeamRankingItem(
                    rank = r.get("rank")?.asInt ?: 0,
                    memberId = r.get("member_id")?.asInt ?: 0,
                    userId = r.get("user_id")?.asInt ?: 0,
                    username = r.get("username")?.asString ?: "?",
                    displayName = r.get("display_name")?.let { if (it.isJsonNull) null else it.asString },
                    avatarUrl = r.get("avatar_url")?.let { if (it.isJsonNull) null else it.asString },
                    role = r.get("role")?.asString ?: "viewer",
                    contribution = r.get("contribution")?.asInt ?: 0,
                )
            } ?: emptyList()
            AuthResult.Success(ranking)
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }
    suspend fun listCommunityPosts(page: Int = 1, size: Int = 20, tag: String? = null, search: String? = null, postType: String? = null): AuthResult<CommunityFeedResponse> =
        withContext(Dispatchers.IO) {
            try {
                val urlBuilder = StringBuilder("$BASE_URL/api/v1/community/posts?page=$page&size=$size")
                tag?.let { urlBuilder.append("&tag=$it") }
                search?.let { urlBuilder.append("&search=$it") }
                postType?.let { urlBuilder.append("&post_type=$it") }
                val requestBuilder = Request.Builder().url(urlBuilder.toString()).get()
                // Note: brief English comment.
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
        steps: List<Pair<String, String?>>? = null,  // List of (content, imageUrl)
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
                        // New paginated format: {"total": N, "comments": [...]}
                        val data = dataObj.asJsonObject
                        val total = data.get("total")?.asInt ?: 0
                        val comments = data.getAsJsonArray("comments")?.map { parsePostComment(it.asJsonObject) } ?: emptyList()
                        AuthResult.Success(CommentsResponse(total = total, page = page, comments = comments))
                    } else if (dataObj != null && dataObj.isJsonArray) {
                        // Old non-paginated format (backwards compat)
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
    suspend fun addPostComment(postId: Int, content: String, parentId: Int? = null): AuthResult<PostCommentItem> = authRequest { token ->
        val body = JsonObject().apply {
            addProperty("content", content)
            parentId?.let { addProperty("parent_id", it) }
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
    suspend fun getUserModules(userId: Int, page: Int = 1, size: Int = 20): AuthResult<Pair<List<StoreModuleInfo>, Int>> = withContext(Dispatchers.IO) {
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
                val modules = data?.getAsJsonArray("modules")?.map { elem ->
                    val m = elem.asJsonObject
                    val author = m.getAsJsonObject("author")
                    StoreModuleInfo(
                        id = m.get("id")?.asInt ?: 0,
                        name = m.get("name")?.asString ?: "",
                        description = m.get("description")?.let { if (it.isJsonNull) null else it.asString },
                        icon = m.get("icon")?.let { if (it.isJsonNull) null else it.asString },
                        category = m.get("category")?.let { if (it.isJsonNull) null else it.asString },
                        tags = m.getAsJsonArray("tags")?.map { it.asString } ?: emptyList(),
                        versionName = m.get("version_name")?.let { if (it.isJsonNull) null else it.asString },
                        downloads = m.get("downloads")?.asInt ?: 0,
                        rating = m.get("rating")?.asFloat ?: 0f,
                        ratingCount = m.get("rating_count")?.asInt ?: 0,
                        isFeatured = m.get("is_featured")?.asBoolean ?: false,
                        authorName = author?.get("username")?.asString ?: "?",
                        shareCode = m.get("share_code")?.let { if (it.isJsonNull) null else it.asString },
                        createdAt = m.get("created_at")?.let { if (it.isJsonNull) null else it.asString },
                    )
                } ?: emptyList()
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

    private fun parseCommunityPost(obj: JsonObject): CommunityPostItem = parser.parseCommunityPost(obj)

    private fun parsePostComment(obj: JsonObject): PostCommentItem = parser.parsePostComment(obj)

    private fun parseTeamBadges(authorObj: JsonObject): List<TeamBadgeInfo> = parser.parseTeamBadges(authorObj)

    private fun parseSimpleUserProfile(obj: JsonObject): CommunityUserProfile = parser.parseSimpleUserProfile(obj)
    suspend fun associateModuleTeam(
        moduleId: Int,
        teamId: Int,
        contributors: List<ContributorInput>
    ): AuthResult<Unit> = authRequest { token ->
        val contribArray = com.google.gson.JsonArray().apply {
            contributors.forEach { c ->
                add(JsonObject().apply {
                    addProperty("user_id", c.userId)
                    addProperty("contributor_role", c.contributorRole)
                    addProperty("contribution_points", c.contributionPoints)
                    c.description?.let { addProperty("description", it) }
                })
            }
        }
        val body = JsonObject().apply {
            addProperty("team_id", teamId)
            add("contributors", contribArray)
        }
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/module-team/$moduleId")
            .header("Authorization", "Bearer $token")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit)
        else AuthResult.Error(parseError(responseBody, response.code))
    }
    suspend fun getModuleTeam(moduleId: Int): AuthResult<ModuleTeamInfo?> = authRequest { token ->
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/module-team/$moduleId")
            .header("Authorization", "Bearer $token")
            .get().build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.get("data")
            if (data == null || data.isJsonNull) {
                AuthResult.Success(null)
            } else {
                val d = data.asJsonObject
                AuthResult.Success(parseModuleTeamInfo(d))
            }
        } else {
            AuthResult.Error(parseError(responseBody, response.code))
        }
    }
    suspend fun removeModuleTeam(moduleId: Int): AuthResult<Unit> = authRequest { token ->
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/module-team/$moduleId")
            .header("Authorization", "Bearer $token")
            .delete().build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) AuthResult.Success(Unit)
        else AuthResult.Error(parseError(responseBody, response.code))
    }
    suspend fun getUserTeamWorks(userId: Int, page: Int = 1, size: Int = 20): AuthResult<UserTeamWorksResponse> =
        withContext(Dispatchers.IO) {
            try {
                val requestBuilder = Request.Builder()
                    .url("$BASE_URL/api/v1/users/$userId/team-works?page=$page&size=$size")
                    .get()
                tokenManager.getAccessToken()?.let { requestBuilder.header("Authorization", "Bearer $it") }
                val response = client.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JsonParser.parseString(responseBody).asJsonObject
                    val data = json.getAsJsonObject("data")
                    val works = data?.getAsJsonArray("works")?.map { el ->
                        val w = el.asJsonObject
                        TeamWorkItem(
                            id = w.get("id")?.asInt ?: 0,
                            name = w.get("name")?.asString ?: "",
                            moduleType = w.get("module_type")?.asString ?: "app",
                            icon = w.get("icon")?.let { if (it.isJsonNull) null else it.asString },
                            downloads = w.get("downloads")?.asInt ?: 0,
                            rating = w.get("rating")?.asFloat ?: 0f,
                            authorName = w.get("author_name")?.asString ?: "?",
                            contributorRole = w.get("contributor_role")?.asString ?: "member",
                            contributionPoints = w.get("contribution_points")?.asInt ?: 0,
                            contributionDescription = w.get("contribution_description")?.let { if (it.isJsonNull) null else it.asString },
                            teamId = w.get("team_id")?.let { if (it.isJsonNull) null else it.asInt },
                            teamName = w.get("team_name")?.let { if (it.isJsonNull) null else it.asString },
                        )
                    } ?: emptyList()
                    AuthResult.Success(UserTeamWorksResponse(
                        works = works,
                        total = data?.get("total")?.asInt ?: 0,
                    ))
                } else {
                    AuthResult.Error(parseError(responseBody, response.code))
                }
            } catch (e: Exception) {
                AuthResult.Error("网络错误: ${e.message}")
            }
        }

    private fun parseModuleTeamInfo(d: JsonObject): ModuleTeamInfo = parser.parseModuleTeamInfo(d)
    suspend fun listBackups(): AuthResult<BackupListResponse> = backupApi.listBackups()
    suspend fun createBackup(): AuthResult<BackupCreateResult> = backupApi.createBackup()
    suspend fun deleteBackup(filename: String): AuthResult<String> = backupApi.deleteBackup(filename)
}
