package com.webtoapp.core.cloud.api

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.webtoapp.core.cloud.CloudApiClient
import com.webtoapp.core.cloud.internal.CloudApiSupport
import com.webtoapp.core.cloud.internal.CloudJsonParser
import com.webtoapp.core.cloud.model.CloudProject
import com.webtoapp.core.cloud.model.ManifestDownloadResult
import com.webtoapp.core.cloud.model.ManifestSyncResult
import com.webtoapp.core.cloud.model.ProjectActivationCode
import com.webtoapp.core.cloud.model.ProjectAnnouncement
import com.webtoapp.core.cloud.model.ProjectConfig
import com.webtoapp.core.cloud.model.ProjectVersion
import com.webtoapp.core.cloud.model.ProjectWebhook
import com.webtoapp.core.auth.AuthResult
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

internal class ProjectApi(
    private val support: CloudApiSupport,
    private val parser: CloudJsonParser,
) {
    private val client = support.client
    private val jsonMediaType = support.jsonMediaType

    suspend fun listProjects(): AuthResult<List<CloudProject>> = support.authRequest {
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/projects")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val dataArr = json.getAsJsonArray("data") ?: return@authRequest AuthResult.Success(emptyList())
            AuthResult.Success(dataArr.map { element -> parser.parseProject(element.asJsonObject) })
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    suspend fun createProject(
        name: String,
        description: String? = null,
        githubRepo: String? = null,
        giteeRepo: String? = null,
    ): AuthResult<CloudProject> = support.authRequest {
        val body = JsonObject().apply {
            addProperty("project_name", name)
            description?.let { addProperty("description", it) }
            githubRepo?.let { addProperty("github_repo", it) }
            giteeRepo?.let { addProperty("gitee_repo", it) }
        }
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/projects")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            AuthResult.Success(parser.parseProject(json.getAsJsonObject("data")))
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    suspend fun deleteProject(projectId: Int): AuthResult<String> = support.authRequest {
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/projects/$projectId")
            .header("Authorization", "Bearer $it")
            .delete()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            AuthResult.Success("项目已删除")
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    suspend fun updateProject(
        projectId: Int,
        name: String? = null,
        description: String? = null,
        githubRepo: String? = null,
        giteeRepo: String? = null,
    ): AuthResult<CloudProject> = support.authRequest {
        val body = JsonObject().apply {
            name?.let { addProperty("project_name", it) }
            description?.let { addProperty("description", it) }
            githubRepo?.let { addProperty("github_repo", it) }
            giteeRepo?.let { addProperty("gitee_repo", it) }
        }
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/projects/$projectId")
            .header("Authorization", "Bearer $it")
            .put(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            AuthResult.Success(parser.parseProject(json.getAsJsonObject("data")))
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    suspend fun publishVersion(
        projectId: Int,
        apkFile: File,
        versionCode: Int,
        versionName: String,
        title: String? = null,
        changelog: String? = null,
        uploadTo: String = "github",
    ): AuthResult<ProjectVersion> = support.authRequest {
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("version_code", versionCode.toString())
            .addFormDataPart("version_name", versionName)
            .addFormDataPart("upload_to", uploadTo)
            .addFormDataPart(
                "apk_file",
                apkFile.name,
                apkFile.asRequestBody("application/vnd.android.package-archive".toMediaType()),
            )
        title?.let { multipart.addFormDataPart("title", it) }
        changelog?.let { multipart.addFormDataPart("changelog", it) }

        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/projects/$projectId/versions/publish")
            .header("Authorization", "Bearer $it")
            .post(multipart.build())
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            AuthResult.Success(parseProjectVersion(data, versionCode, versionName))
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    suspend fun listVersions(projectId: Int): AuthResult<List<ProjectVersion>> = support.authRequest {
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/projects/$projectId/versions")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val dataArr = json.getAsJsonArray("data") ?: return@authRequest AuthResult.Success(emptyList())
            AuthResult.Success(dataArr.map { element ->
                parseProjectVersion(element.asJsonObject)
            })
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    suspend fun generateProjectCodes(
        projectId: Int,
        count: Int = 10,
        maxUses: Int = 1,
        prefix: String = "",
    ): AuthResult<List<ProjectActivationCode>> = support.authRequest {
        val body = JsonObject().apply {
            addProperty("count", count)
            addProperty("max_uses", maxUses)
            if (prefix.isNotBlank()) {
                addProperty("prefix", prefix)
            }
        }
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/projects/$projectId/codes/generate")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            val codesArr = data?.getAsJsonArray("codes")
            val codes = codesArr?.map { element ->
                val code = element.asString
                ProjectActivationCode(
                    id = 0,
                    code = code,
                    status = "unused",
                    maxUses = 1,
                    usedCount = 0,
                    deviceId = null,
                    createdAt = "",
                    usedAt = null,
                )
            } ?: emptyList()
            AuthResult.Success(codes)
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    suspend fun listProjectCodes(
        projectId: Int,
        status: String? = null,
        page: Int = 1,
    ): AuthResult<List<ProjectActivationCode>> = support.authRequest {
        val url = buildString {
            append("${CloudApiClient.BASE_URL}/api/v1/projects/$projectId/codes?page=$page")
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
            AuthResult.Success(dataArr.map { element -> parser.parseActivationCode(element.asJsonObject) })
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    suspend fun createProjectAnnouncement(
        projectId: Int,
        title: String,
        content: String,
        priority: Int = 0,
    ): AuthResult<ProjectAnnouncement> = support.authRequest {
        val body = JsonObject().apply {
            addProperty("title", title)
            addProperty("content", content)
            addProperty("priority", priority)
        }
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/projects/$projectId/announcements")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            AuthResult.Success(parser.parseProjectAnnouncement(json.getAsJsonObject("data")))
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    suspend fun listProjectAnnouncements(projectId: Int): AuthResult<List<ProjectAnnouncement>> = support.authRequest {
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/projects/$projectId/announcements")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val dataArr = json.getAsJsonArray("data") ?: return@authRequest AuthResult.Success(emptyList())
            AuthResult.Success(dataArr.map { element -> parser.parseProjectAnnouncement(element.asJsonObject) })
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    suspend fun updateProjectAnnouncement(
        projectId: Int,
        annId: Int,
        title: String? = null,
        content: String? = null,
        isActive: Boolean? = null,
    ): AuthResult<String> = support.authRequest {
        val body = JsonObject().apply {
            title?.let { addProperty("title", it) }
            content?.let { addProperty("content", it) }
            isActive?.let { addProperty("is_active", it) }
        }
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/projects/$projectId/announcements/$annId")
            .header("Authorization", "Bearer $it")
            .put(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            AuthResult.Success("更新成功")
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    suspend fun deleteProjectAnnouncement(projectId: Int, annId: Int): AuthResult<String> = support.authRequest {
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/projects/$projectId/announcements/$annId")
            .header("Authorization", "Bearer $it")
            .delete()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            AuthResult.Success("删除成功")
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    suspend fun createProjectConfig(
        projectId: Int,
        key: String,
        value: String,
        description: String? = null,
    ): AuthResult<ProjectConfig> = support.authRequest {
        val body = JsonObject().apply {
            addProperty("config_key", key)
            addProperty("config_value", value)
            description?.let { addProperty("description", it) }
        }
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/projects/$projectId/configs")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            AuthResult.Success(
                ProjectConfig(
                    id = data?.get("id")?.asInt ?: 0,
                    key = data?.get("config_key")?.asString ?: key,
                    value = data?.get("config_value")?.asString ?: value,
                    description = data?.get("description")?.asString,
                    isActive = data?.get("is_active")?.asBoolean ?: true,
                ),
            )
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    suspend fun listProjectConfigs(projectId: Int): AuthResult<List<ProjectConfig>> = support.authRequest {
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/projects/$projectId/configs")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val dataArr = json.getAsJsonArray("data") ?: return@authRequest AuthResult.Success(emptyList())
            AuthResult.Success(dataArr.map { element ->
                val obj = element.asJsonObject
                ProjectConfig(
                    id = obj.get("id")?.asInt ?: 0,
                    key = obj.get("config_key")?.asString ?: "",
                    value = obj.get("config_value")?.asString ?: "",
                    description = obj.get("description")?.asString,
                    isActive = obj.get("is_active")?.asBoolean ?: true,
                )
            })
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    suspend fun updateProjectConfig(
        projectId: Int,
        cfgId: Int,
        value: String? = null,
        description: String? = null,
        isActive: Boolean? = null,
    ): AuthResult<String> = support.authRequest {
        val body = JsonObject().apply {
            value?.let { addProperty("config_value", it) }
            description?.let { addProperty("description", it) }
            isActive?.let { addProperty("is_active", it) }
        }
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/projects/$projectId/configs/$cfgId")
            .header("Authorization", "Bearer $it")
            .put(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            AuthResult.Success("更新成功")
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    suspend fun deleteProjectConfig(projectId: Int, cfgId: Int): AuthResult<String> = support.authRequest {
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/projects/$projectId/configs/$cfgId")
            .header("Authorization", "Bearer $it")
            .delete()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            AuthResult.Success("删除成功")
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    suspend fun createWebhook(
        projectId: Int,
        webhookUrl: String,
        events: List<String>,
        secret: String? = null,
    ): AuthResult<ProjectWebhook> = support.authRequest {
        val body = JsonObject().apply {
            addProperty("url", webhookUrl)
            val eventsArray = com.google.gson.JsonArray()
            events.forEach { eventsArray.add(it) }
            add("events", eventsArray)
            secret?.let { addProperty("secret", it) }
        }
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/projects/$projectId/webhooks")
            .header("Authorization", "Bearer $it")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            AuthResult.Success(parser.parseWebhook(json.getAsJsonObject("data")))
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    suspend fun listWebhooks(projectId: Int): AuthResult<List<ProjectWebhook>> = support.authRequest {
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/projects/$projectId/webhooks")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val dataArr = json.getAsJsonArray("data") ?: return@authRequest AuthResult.Success(emptyList())
            AuthResult.Success(dataArr.map { element -> parser.parseWebhook(element.asJsonObject) })
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    suspend fun deleteWebhook(projectId: Int, webhookId: Int): AuthResult<String> = support.authRequest {
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/projects/$projectId/webhooks/$webhookId")
            .header("Authorization", "Bearer $it")
            .delete()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            AuthResult.Success("Webhook 已删除")
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    suspend fun uploadManifest(
        projectId: Int,
        manifestJson: String,
        manifestVersion: Int,
    ): AuthResult<ManifestSyncResult> = support.authRequest {
        val body = JsonObject().apply {
            add("manifest", JsonParser.parseString(manifestJson))
            addProperty("manifest_version", manifestVersion)
        }
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/projects/$projectId/manifest")
            .header("Authorization", "Bearer $it")
            .put(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            val conflict = data?.get("conflict")?.asBoolean ?: false
            AuthResult.Success(
                ManifestSyncResult(
                    success = !conflict,
                    manifestVersion = data?.get("manifest_version")?.asInt ?: data?.get("server_version")?.asInt ?: 0,
                    syncedAt = data?.get("synced_at")?.asString,
                    conflict = conflict,
                ),
            )
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    suspend fun downloadManifest(projectId: Int): AuthResult<ManifestDownloadResult> = support.authRequest {
        val request = Request.Builder()
            .url("${CloudApiClient.BASE_URL}/api/v1/projects/$projectId/manifest")
            .header("Authorization", "Bearer $it")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val json = JsonParser.parseString(responseBody).asJsonObject
            val data = json.getAsJsonObject("data")
            val manifestObj = data?.get("manifest")
            AuthResult.Success(
                ManifestDownloadResult(
                    manifestJson = if (manifestObj != null && !manifestObj.isJsonNull) manifestObj.toString() else null,
                    manifestVersion = data?.get("manifest_version")?.asInt ?: 0,
                    syncedAt = data?.get("synced_at")?.asString,
                ),
            )
        } else {
            AuthResult.Error(support.parseError(responseBody, response.code))
        }
    }

    private fun parseProjectVersion(
        data: JsonObject?,
        defaultVersionCode: Int = 0,
        defaultVersionName: String = "",
    ): ProjectVersion = ProjectVersion(
        id = data?.get("id")?.asInt ?: 0,
        versionCode = data?.get("version_code")?.asInt ?: defaultVersionCode,
        versionName = data?.get("version_name")?.asString ?: defaultVersionName,
        title = data?.get("title")?.asString,
        changelog = data?.get("changelog")?.asString,
        downloadUrlGithub = data?.get("download_url_github")?.asString,
        downloadUrlGitee = data?.get("download_url_gitee")?.asString,
        isForceUpdate = data?.get("is_force_update")?.asBoolean ?: false,
        createdAt = data?.get("created_at")?.asString,
    )
}
