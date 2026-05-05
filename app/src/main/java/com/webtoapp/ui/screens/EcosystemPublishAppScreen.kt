package com.webtoapp.ui.screens
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.cloud.AppDownloadManager
import com.webtoapp.core.cloud.AppStoreItem
import com.webtoapp.core.cloud.AppStoreListResponse
import com.webtoapp.core.cloud.CloudApiClient
import com.webtoapp.core.cloud.StoreModuleInfo
import com.webtoapp.core.cloud.ActivationCode
import com.webtoapp.core.cloud.ActivationSettings
import com.webtoapp.core.cloud.Announcement
import com.webtoapp.core.cloud.UpdateConfig
import com.webtoapp.core.cloud.AppUser
import com.webtoapp.core.cloud.GeoDistribution
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.PremiumFilterChip
import com.webtoapp.ui.components.PremiumTextField
import com.webtoapp.ui.components.ApkExportPreflightPanel
import com.webtoapp.ui.design.*
import com.webtoapp.ui.viewmodel.CloudViewModel
import com.webtoapp.ui.screens.ecosystem.AnimatedCounter
import com.webtoapp.ui.screens.ecosystem.Avatar
import com.webtoapp.ui.screens.ecosystem.EcosystemMotion
import com.webtoapp.ui.screens.ecosystem.GlassDivider
import com.webtoapp.ui.screens.ecosystem.LikeBurstEffect
import com.webtoapp.ui.screens.ecosystem.ModuleCard
import com.webtoapp.ui.screens.ecosystem.StaggeredItem
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.data.model.toManifestJson
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EcosystemPublishAppSheet(
    apiClient: CloudApiClient,
    onDismiss: () -> Unit,
    onPublished: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = androidx.compose.ui.platform.LocalContext.current


    val db = remember { com.webtoapp.data.database.AppDatabase.getInstance(context) }
    val allProjects by db.webAppDao().getAllWebApps().collectAsStateWithLifecycle(initialValue = emptyList())


    var selectedProject by remember { mutableStateOf<com.webtoapp.data.model.WebApp?>(null) }
    var showProjectPicker by remember { mutableStateOf(false) }


    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("other") }
    var versionName by remember { mutableStateOf("1.0.0") }
    var versionCode by remember { mutableStateOf("1") }
    var packageName by remember { mutableStateOf("") }
    var iconUrl by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var screenshotUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var contactEmail by remember { mutableStateOf("") }
    var websiteUrl by remember { mutableStateOf("") }
    var privacyPolicyUrl by remember { mutableStateOf("") }
    var isPublishing by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var uploadStatus by remember { mutableStateOf("") }
    var publishFailureReport by remember { mutableStateOf<SheetFailureReport?>(null) }


    var enableActivation by remember { mutableStateOf(false) }
    var enableDeviceBinding by remember { mutableStateOf(false) }
    var activationCodes by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedCodeTemplate by remember { mutableStateOf<String?>(null) }
    var customCodeInput by remember { mutableStateOf("") }



    var selectedApkFile by remember { mutableStateOf<java.io.File?>(null) }
    var detectedPackageName by remember { mutableStateOf<String?>(null) }
    var detectedVersionName by remember { mutableStateOf<String?>(null) }
    var detectedVersionCode by remember { mutableStateOf<Int?>(null) }


    var isBuilding by remember { mutableStateOf(false) }
    var buildProgress by remember { mutableIntStateOf(0) }
    var buildProgressText by remember { mutableStateOf("") }
    var buildFailureReport by remember { mutableStateOf<SheetFailureReport?>(null) }
    var showBuildFailureDialog by remember { mutableStateOf(false) }
    var preflightReport by remember { mutableStateOf<com.webtoapp.core.apkbuilder.ApkExportPreflightReport?>(null) }

    fun startApkBuild(project: com.webtoapp.data.model.WebApp) {
        buildFailureReport = null
        showBuildFailureDialog = false
        val nextPreflight = com.webtoapp.core.apkbuilder.ApkExportPreflight.check(context, project)
        preflightReport = nextPreflight
        if (nextPreflight.hasErrors) {
            return
        }
        isBuilding = true
        buildProgress = 0
        buildProgressText = Strings.preparingBuild
        scope.launch {
            val apkBuilder = com.webtoapp.core.apkbuilder.ApkBuilder(context)
            val result = apkBuilder.buildApk(project) { p, t ->
                buildProgress = p
                buildProgressText = t
            }
            when (result) {
                is com.webtoapp.core.apkbuilder.BuildResult.Success -> {
                    selectedApkFile = result.apkFile
                    runCatching {
                        val pm = context.packageManager
                        val info = pm.getPackageArchiveInfo(
                            result.apkFile.absolutePath,
                            android.content.pm.PackageManager.GET_ACTIVITIES or android.content.pm.PackageManager.GET_SERVICES or android.content.pm.PackageManager.GET_PROVIDERS
                        )
                        detectedPackageName = info?.packageName
                        detectedVersionName = info?.versionName
                        detectedVersionCode = if (info != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            info.longVersionCode.toInt()
                        } else info?.versionCode
                    }
                    buildFailureReport = null
                    showBuildFailureDialog = false
                    preflightReport = null
                }
                is com.webtoapp.core.apkbuilder.BuildResult.Error -> {
                    buildFailureReport = buildApkBuildFailureReport(
                        context = context,
                        project = project,
                        error = result
                    )
                    showBuildFailureDialog = true
                }
            }
            isBuilding = false
        }
    }


    var screenshotUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    val screenshotPickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        screenshotUris = screenshotUris + uris
    }


    var iconUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { iconUri = it }
    }

    val categories = listOf(
        "tools" to Strings.catTools, "social" to Strings.catSocial, "education" to Strings.catEducation,
        "entertainment" to Strings.catEntertainment, "productivity" to Strings.catProductivity,
        "lifestyle" to Strings.catLifestyle, "business" to Strings.catBusiness,
        "news" to Strings.catNews, "finance" to Strings.catFinance,
        "health" to Strings.catHealth, "other" to Strings.catOther
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.92f),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { PublishAppHeaderBanner() }


                item {
                    PublishAppProjectSelector(
                        selectedProject = selectedProject,
                        selectedApkFile = selectedApkFile,
                        onClick = { showProjectPicker = true }
                    )
                }

                item {
                    PublishAppBuildPanel(
                        selectedProject = selectedProject,
                        selectedApkFile = selectedApkFile,
                        isBuilding = isBuilding,
                        buildProgress = buildProgress,
                        buildProgressText = buildProgressText,
                        buildFailureReport = buildFailureReport,
                        onStartBuild = { selectedProject?.let(::startApkBuild) },
                        onViewBuildError = { showBuildFailureDialog = true }
                    )
                }


                item {
                    PublishAppInfoSection(
                        name = name,
                        onNameChange = { name = it },
                        iconUri = iconUri,
                        iconUrl = iconUrl,
                        onSelectIcon = { iconPickerLauncher.launch("image/*") },
                        versionName = versionName,
                        onVersionNameChange = { versionName = it },
                        versionCode = versionCode,
                        onVersionCodeChange = { versionCode = it.filter(Char::isDigit) },
                        packageName = packageName,
                        onPackageNameChange = { packageName = it }
                    )
                }

                item {
                    PublishAppMetadataSection(
                        selectedCategory = selectedCategory,
                        categories = categories,
                        onCategoryChange = { selectedCategory = it },
                        description = description,
                        onDescriptionChange = { description = it },
                        tags = tags,
                        onTagsChange = { tags = it }
                    )
                }


                item {
                    PublishAppScreenshotsSection(
                        screenshotUrls = screenshotUrls,
                        screenshotUris = screenshotUris,
                        onRemoveRemoteScreenshot = { index ->
                            screenshotUrls = screenshotUrls.toMutableList().also { it.removeAt(index) }
                        },
                        onRemoveLocalScreenshot = { index ->
                            screenshotUris = screenshotUris.toMutableList().also { it.removeAt(index) }
                        },
                        onPickScreenshots = { screenshotPickerLauncher.launch("image/*") }
                    )
                }





                item {
                    PublishAppContactSection(
                        contactEmail = contactEmail,
                        onContactEmailChange = { contactEmail = it },
                        websiteUrl = websiteUrl,
                        onWebsiteUrlChange = { websiteUrl = it },
                        privacyPolicyUrl = privacyPolicyUrl,
                        onPrivacyPolicyUrlChange = { privacyPolicyUrl = it }
                    )
                }

                item {
                    PublishAppActivationSection(
                        enableActivation = enableActivation,
                        onEnableActivationChange = { enableActivation = it },
                        enableDeviceBinding = enableDeviceBinding,
                        onEnableDeviceBindingChange = { enableDeviceBinding = it },
                        selectedCodeTemplate = selectedCodeTemplate,
                        onSelectedCodeTemplateChange = { selectedCodeTemplate = it },
                        activationCodes = activationCodes,
                        onActivationCodesChange = { activationCodes = it },
                        customCodeInput = customCodeInput,
                        onCustomCodeInputChange = { customCodeInput = it }
                    )
                }


                if (isPublishing && uploadProgress > 0f) {
                    item {
                        PublishAppUploadProgress(
                            uploadStatus = uploadStatus,
                            uploadProgress = uploadProgress
                        )
                    }
                }


                item {
                    PublishAppSubmitAction(
                        isPublishing = isPublishing,
                        isBuilding = isBuilding,
                        onClick = {
                            if (selectedProject == null) {
                                scope.launch { snackbarHostState.showSnackbar(Strings.selectAppToPublish) }
                                return@PublishAppSubmitAction
                            }
                            if (name.isBlank() || description.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar(Strings.ecosystemFillRequired) }
                                return@PublishAppSubmitAction
                            }
                            if (screenshotUrls.isEmpty() && screenshotUris.isEmpty()) {
                                scope.launch { snackbarHostState.showSnackbar(Strings.ecosystemAddScreenshot) }
                                return@PublishAppSubmitAction
                            }
                            scope.launch {
                                isPublishing = true
                                uploadProgress = 0f


                                fun uriToTempFile(uri: android.net.Uri, prefix: String, ext: String): java.io.File? {
                                    return try {
                                        val input = context.contentResolver.openInputStream(uri) ?: return null
                                        val tempFile = java.io.File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.$ext")
                                        tempFile.outputStream().use { out -> input.copyTo(out) }
                                        input.close()
                                        tempFile
                                    } catch (e: Exception) { null }
                                }


                                var finalIconUrl = iconUrl.ifBlank { null }
                                if (iconUri != null) {
                                    uploadStatus = Strings.uploadingIconAlready
                                    val iconFile = uriToTempFile(iconUri!!, "icon", "png")
                                    if (iconFile != null) {
                                        when (val r = apiClient.uploadAsset(iconFile, "image/png")) {
                                            is com.webtoapp.core.auth.AuthResult.Success -> finalIconUrl = r.data
                                            is com.webtoapp.core.auth.AuthResult.Error -> {
                                                val summary = Strings.iconUploadFailed.format(r.message)
                                                publishFailureReport = buildSheetFailureReport(
                                                    title = Strings.appPublishFailed,
                                                    stage = Strings.uploadIconStage,
                                                    summary = summary,
                                                    contextLines = listOf(
                                                        "project=${selectedProject?.name ?: "unknown"}",
                                                        "iconUri=${iconUri}",
                                                        "name=$name",
                                                        "versionName=$versionName",
                                                        "versionCode=$versionCode"
                                                    )
                                                )
                                                iconFile.delete()
                                                isPublishing = false
                                                uploadProgress = 0f
                                                uploadStatus = summary
                                                return@launch
                                            }
                                        }
                                        iconFile.delete()
                                    } else {
                                        val summary = Strings.iconReadFailed
                                        publishFailureReport = buildSheetFailureReport(
                                            title = Strings.appPublishFailed,
                                            stage = Strings.readIconStage,
                                            summary = summary,
                                            contextLines = listOf(
                                                "project=${selectedProject?.name ?: "unknown"}",
                                                "iconUri=${iconUri}",
                                                "name=$name"
                                            )
                                        )
                                        isPublishing = false
                                        uploadProgress = 0f
                                        uploadStatus = summary
                                        return@launch
                                    }
                                }


                                val allScreenshotUrls = screenshotUrls.toMutableList()
                                if (screenshotUris.isNotEmpty()) {
                                    val total = screenshotUris.size
                                    for ((idx, uri) in screenshotUris.withIndex()) {
                                        uploadStatus = Strings.uploadingScreenshot.format(idx + 1, total)
                                        uploadProgress = (idx.toFloat()) / (total + 2)
                                        val scrFile = uriToTempFile(uri, "screenshot_$idx", "png")
                                        if (scrFile != null) {
                                            when (val r = apiClient.uploadAsset(scrFile, "image/png")) {
                                                is com.webtoapp.core.auth.AuthResult.Success -> allScreenshotUrls.add(r.data)
                                                is com.webtoapp.core.auth.AuthResult.Error -> {
                                                    val summary = Strings.screenshotUploadFailed.format(idx + 1, r.message)
                                                    publishFailureReport = buildSheetFailureReport(
                                                        title = Strings.appPublishFailed,
                                                        stage = Strings.uploadScreenshotStage,
                                                        summary = summary,
                                                        contextLines = listOf(
                                                            "project=${selectedProject?.name ?: "unknown"}",
                                                            "screenshotIndex=${idx + 1}",
                                                            "screenshotUri=$uri",
                                                            "name=$name"
                                                        )
                                                    )
                                                    scrFile.delete()
                                                    isPublishing = false
                                                    uploadProgress = 0f
                                                    uploadStatus = summary
                                                    return@launch
                                                }
                                            }
                                            scrFile.delete()
                                        } else {
                                            val summary = Strings.screenshotReadFailed.format(idx + 1)
                                            publishFailureReport = buildSheetFailureReport(
                                                title = Strings.appPublishFailed,
                                                stage = Strings.readScreenshotStage,
                                                summary = summary,
                                                contextLines = listOf(
                                                    "project=${selectedProject?.name ?: "unknown"}",
                                                    "screenshotIndex=${idx + 1}",
                                                    "screenshotUri=$uri"
                                                )
                                            )
                                            isPublishing = false
                                            uploadProgress = 0f
                                            uploadStatus = summary
                                            return@launch
                                        }
                                    }
                                }


                                var apkUrlGithub: String? = null
                                if (selectedApkFile != null && selectedApkFile!!.exists()) {
                                    try {
                                        uploadStatus = Strings.uploadingApk
                                        when (val r = apiClient.uploadAsset(
                                            selectedApkFile!!,
                                            "application/vnd.android.package-archive"
                                        ) { progress -> uploadProgress = 0.5f + progress * 0.4f }) {
                                            is com.webtoapp.core.auth.AuthResult.Success -> apkUrlGithub = r.data
                                            is com.webtoapp.core.auth.AuthResult.Error -> {
                                                val summary = Strings.apkUploadFailed.format(r.message)
                                                publishFailureReport = buildSheetFailureReport(
                                                    title = Strings.appPublishFailed,
                                                    stage = Strings.uploadApkStage,
                                                    summary = summary,
                                                    contextLines = listOf(
                                                        "project=${selectedProject?.name ?: "unknown"}",
                                                        "apk=${selectedApkFile!!.absolutePath}",
                                                        "apkSize=${selectedApkFile!!.length()}",
                                                        "name=$name",
                                                        "versionName=$versionName",
                                                        "versionCode=$versionCode"
                                                    )
                                                )
                                                isPublishing = false
                                                uploadProgress = 0f
                                                uploadStatus = summary
                                                return@launch
                                            }
                                        }
                                    } catch (e: Exception) {
                                        val summary = Strings.apkUploadFailed.format(e.message)
                                        publishFailureReport = buildSheetFailureReport(
                                            title = Strings.appPublishFailed,
                                            stage = Strings.uploadApkStage,
                                            summary = summary,
                                            contextLines = listOf(
                                                "project=${selectedProject?.name ?: "unknown"}",
                                                "apk=${selectedApkFile!!.absolutePath}",
                                                "apkSize=${selectedApkFile!!.length()}",
                                                "name=$name"
                                            ),
                                            throwable = e
                                        )
                                        isPublishing = false
                                        uploadProgress = 0f
                                        uploadStatus = summary
                                        return@launch
                                    }
                                } else {

                                    scope.launch {
                                        snackbarHostState.showSnackbar(Strings.noApkWarning)
                                    }
                                }

                                val sourceBundleUrl = selectedProject?.let { buildSourceBundleUrl(it) }
                                val buildManifest = buildAppManifestJson(
                                    selectedProject = selectedProject,
                                    name = name,
                                    description = description,
                                    category = selectedCategory,
                                    versionName = detectedVersionName ?: versionName,
                                    versionCode = detectedVersionCode ?: (versionCode.toIntOrNull() ?: 1),
                                    packageName = detectedPackageName ?: packageName.ifBlank { null },
                                    apkUrl = apkUrlGithub,
                                    sourceBundleUrl = sourceBundleUrl
                                )

                                uploadStatus = Strings.publishingAppInfo
                                uploadProgress = 0.95f
                                val result = apiClient.publishApp(
                                    name = name,
                                    description = description,
                                    category = selectedCategory,
                                    versionName = detectedVersionName ?: versionName,
                                    versionCode = detectedVersionCode ?: (versionCode.toIntOrNull() ?: 1),
                                    packageName = detectedPackageName ?: packageName.ifBlank { null },
                                    icon = finalIconUrl,
                                    tags = tags.ifBlank { null },
                                    screenshots = allScreenshotUrls,
                                    apkUrlGithub = apkUrlGithub,
                                    apkUrlGitee = null,
                                    contactEmail = contactEmail.ifBlank { null },
                                    websiteUrl = websiteUrl.ifBlank { null },
                                    privacyPolicyUrl = privacyPolicyUrl.ifBlank { null },
                                    appSourceType = mapAppTypeToSourceType(selectedProject?.appType),
                                    visibility = "public",
                                    reviewMode = "public_first",
                                    sourceBundleUrl = sourceBundleUrl,
                                    buildManifestJson = buildManifest
                                )
                                isPublishing = false
                                uploadProgress = 0f
                                uploadStatus = ""
                                when (result) {
                                    is com.webtoapp.core.auth.AuthResult.Success -> {
                                        snackbarHostState.showSnackbar(Strings.ecosystemPublishAppSuccess)
                                        onPublished()
                                    }
                                    is com.webtoapp.core.auth.AuthResult.Error -> {
                                        val summary = "${Strings.ecosystemLoadFailed}: ${result.message}"
                                        publishFailureReport = buildSheetFailureReport(
                                            title = Strings.appPublishFailed,
                                            stage = Strings.submitAppInfoStage,
                                            summary = summary,
                                            contextLines = listOf(
                                                "project=${selectedProject?.name ?: "unknown"}",
                                                "name=$name",
                                                "versionName=$versionName",
                                                "versionCode=$versionCode",
                                                "apkUrlGithub=${apkUrlGithub ?: "null"}"
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }


    if (showProjectPicker) {
        PublishAppProjectPickerDialog(
            allProjects = allProjects,
            selectedProject = selectedProject,
            preflightReport = preflightReport,
            isBuilding = isBuilding,
            onSelectProject = { project ->
                selectedProject = project
                name = project.name
                val exportConfig = project.apkExportConfig
                versionName = exportConfig?.customVersionName ?: "1.0.0"
                versionCode = (exportConfig?.customVersionCode ?: 1).toString()
                packageName = exportConfig?.customPackageName ?: project.packageName ?: ""
                iconUri = project.iconPath?.let { android.net.Uri.fromFile(java.io.File(it)) }
                selectedApkFile = findLatestBuiltApk(context, project)
                detectedPackageName = null
                detectedVersionName = null
                detectedVersionCode = null
                showProjectPicker = false
            },
            onDismiss = { showProjectPicker = false }
        )
    }

    publishFailureReport?.let { report ->
        SheetFailureReportDialog(
            report = report,
            onDismiss = { publishFailureReport = null }
        )
    }

    if (showBuildFailureDialog && buildFailureReport != null) {
        SheetFailureReportDialog(
            report = buildFailureReport!!,
            onDismiss = { showBuildFailureDialog = false }
        )
    }
}

@Composable
private fun PublishAppHeaderBanner() {
    WtaStatusBanner(
        title = Strings.ecosystemPublishApp,
        message = Strings.selectAppToPublish,
        tone = WtaStatusTone.Info
    )
}

@Composable
private fun PublishAppProjectSelector(
    selectedProject: com.webtoapp.data.model.WebApp?,
    selectedApkFile: java.io.File?,
    onClick: () -> Unit
) {
    WtaSettingCard(onClick = onClick) {
        WtaSettingRow(
            title = Strings.selectApp,
            subtitle = selectedProject?.name ?: Strings.tapToSelectApp,
            icon = Icons.Outlined.Apps,
            onClick = onClick,
            iconContent = {
                if (selectedProject?.iconPath != null) {
                    AsyncImage(
                        model = selectedProject.iconPath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Android,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            trailing = {
                if (selectedProject != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            selectedProject.appType.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selectedApkFile != null) {
                            Text(
                                Strings.ecosystemApkReady,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )
    }
}

@Composable
private fun PublishAppBuildPanel(
    selectedProject: com.webtoapp.data.model.WebApp?,
    selectedApkFile: java.io.File?,
    isBuilding: Boolean,
    buildProgress: Int,
    buildProgressText: String,
    buildFailureReport: SheetFailureReport?,
    onStartBuild: () -> Unit,
    onViewBuildError: () -> Unit
) {
    if (selectedProject == null) return
    when {
        selectedApkFile != null && !isBuilding -> {
            WtaStatusBanner(
                title = Strings.ecosystemApkReady,
                message = "${selectedApkFile.name} · ${selectedApkFile.length() / 1024} KB",
                tone = WtaStatusTone.Success,
                actionLabel = Strings.rebuild,
                onAction = onStartBuild
            )
        }
        isBuilding -> {
            WtaSettingCard {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(Strings.buildingApk, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }
                        Text(
                            "$buildProgress%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { buildProgress.coerceIn(0, 100) / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        buildProgressText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
        buildFailureReport != null -> {
            WtaStatusBanner(
                title = Strings.buildFailed,
                message = buildFailureReport.summary,
                tone = WtaStatusTone.Error,
                actionLabel = Strings.viewFullError,
                onAction = onViewBuildError
            )
        }
        else -> {
            WtaStatusBanner(
                title = Strings.needBuildBeforePublish,
                message = Strings.oneClickBuildApk,
                tone = WtaStatusTone.Warning,
                actionLabel = Strings.oneClickBuildApk,
                onAction = onStartBuild
            )
        }
    }
}

@Composable
private fun PublishAppInfoSection(
    name: String,
    onNameChange: (String) -> Unit,
    iconUri: android.net.Uri?,
    iconUrl: String,
    onSelectIcon: () -> Unit,
    versionName: String,
    onVersionNameChange: (String) -> Unit,
    versionCode: String,
    onVersionCodeChange: (String) -> Unit,
    packageName: String,
    onPackageNameChange: (String) -> Unit
) {
    WtaSection(
        title = Strings.basicInfo,
        description = Strings.appNameIconVersion,
        level = WtaCapabilityLevel.Common,
        headerStyle = WtaSectionHeaderStyle.Quiet
    ) {
        WtaTextFieldRow(
            title = Strings.appNameLabel,
            value = name,
            onValueChange = onNameChange,
            placeholder = Strings.appNamePlaceholder
        )
        WtaSettingRow(
            title = if (iconUri != null || iconUrl.isNotBlank()) Strings.changeIcon else Strings.selectIcon,
            subtitle = Strings.selectIconFromGallery,
            icon = Icons.Outlined.Image,
            onClick = onSelectIcon,
            iconContent = {
                when {
                    iconUri != null -> AsyncImage(
                        model = iconUri,
                        contentDescription = Strings.iconPreview,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    iconUrl.isNotBlank() -> AsyncImage(
                        model = iconUrl,
                        contentDescription = Strings.iconPreview,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    else -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Android,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        )
        WtaTextFieldRow(
            title = Strings.versionNameLabel,
            value = versionName,
            onValueChange = onVersionNameChange,
            placeholder = "1.0.0"
        )
        WtaTextFieldRow(
            title = Strings.versionCodeLabel,
            value = versionCode,
            onValueChange = onVersionCodeChange,
            placeholder = "1"
        )
        WtaTextFieldRow(
            title = Strings.packageNameLabel,
            value = packageName,
            onValueChange = onPackageNameChange,
            placeholder = "com.example.myapp"
        )
    }
}

@Composable
private fun PublishAppMetadataSection(
    selectedCategory: String,
    categories: List<Pair<String, String>>,
    onCategoryChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    tags: String,
    onTagsChange: (String) -> Unit
) {
    WtaSection(
        title = "Category",
        description = Strings.selectAppCategory,
        level = WtaCapabilityLevel.Common,
        headerStyle = WtaSectionHeaderStyle.Quiet
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { (key, label) ->
                PremiumFilterChip(
                    selected = selectedCategory == key,
                    onClick = { onCategoryChange(key) },
                    label = { Text(label, fontSize = 12.sp) }
                )
            }
        }
        WtaTextFieldRow(
            title = Strings.appDescLabel,
            value = description,
            onValueChange = onDescriptionChange,
            placeholder = Strings.appDescPlaceholder,
            singleLine = false
        )
        WtaTextFieldRow(
            title = Strings.tagsLabel,
            value = tags,
            onValueChange = onTagsChange,
            placeholder = Strings.tagsPlaceholder
        )
    }
}

@Composable
private fun PublishAppScreenshotsSection(
    screenshotUrls: List<String>,
    screenshotUris: List<android.net.Uri>,
    onRemoveRemoteScreenshot: (Int) -> Unit,
    onRemoveLocalScreenshot: (Int) -> Unit,
    onPickScreenshots: () -> Unit
) {
    WtaSection(
        title = Strings.screenshotsRequired,
        description = Strings.addScreenshots,
        level = WtaCapabilityLevel.Common,
        headerStyle = WtaSectionHeaderStyle.Quiet
    ) {
        if (screenshotUris.isNotEmpty() || screenshotUrls.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(screenshotUrls.size) { index ->
                    ScreenshotPreviewItem(
                        model = screenshotUrls[index],
                        contentDescription = Strings.screenshotLabel.format(index + 1),
                        onRemove = { onRemoveRemoteScreenshot(index) }
                    )
                }
                items(screenshotUris.size) { index ->
                    ScreenshotPreviewItem(
                        model = screenshotUris[index],
                        contentDescription = Strings.newScreenshotLabel.format(index + 1),
                        onRemove = { onRemoveLocalScreenshot(index) }
                    )
                }
            }
        }
        FilledTonalButton(
            onClick = onPickScreenshots,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(WtaRadius.Button)
        ) {
            Icon(
                Icons.Outlined.AddPhotoAlternate,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(Strings.addScreenshotsFromAlbum, fontWeight = FontWeight.Medium)
        }
        Text(
            Strings.screenshotsAdded.format(screenshotUrls.size + screenshotUris.size),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ScreenshotPreviewItem(
    model: Any,
    contentDescription: String,
    onRemove: () -> Unit
) {
    Box(modifier = Modifier.size(85.dp, 150.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(WtaRadius.Card))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        FilledIconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp)
                .size(22.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Filled.Close, null, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun PublishAppContactSection(
    contactEmail: String,
    onContactEmailChange: (String) -> Unit,
    websiteUrl: String,
    onWebsiteUrlChange: (String) -> Unit,
    privacyPolicyUrl: String,
    onPrivacyPolicyUrlChange: (String) -> Unit
) {
    WtaSection(
        title = Strings.contactInfo,
        description = Strings.contactInfoHint,
        level = WtaCapabilityLevel.Common,
        headerStyle = WtaSectionHeaderStyle.Quiet
    ) {
        WtaTextFieldRow(
            title = Strings.emailLabel,
            value = contactEmail,
            onValueChange = onContactEmailChange,
            placeholder = "name@example.com"
        )
        WtaTextFieldRow(
            title = Strings.websiteLabel,
            value = websiteUrl,
            onValueChange = onWebsiteUrlChange,
            placeholder = "https://example.com"
        )
        WtaTextFieldRow(
            title = Strings.privacyPolicyUrlLabel,
            value = privacyPolicyUrl,
            onValueChange = onPrivacyPolicyUrlChange,
            placeholder = "https://example.com/privacy"
        )
    }
}

@Composable
private fun PublishAppActivationSection(
    enableActivation: Boolean,
    onEnableActivationChange: (Boolean) -> Unit,
    enableDeviceBinding: Boolean,
    onEnableDeviceBindingChange: (Boolean) -> Unit,
    selectedCodeTemplate: String?,
    onSelectedCodeTemplateChange: (String?) -> Unit,
    activationCodes: List<String>,
    onActivationCodesChange: (List<String>) -> Unit,
    customCodeInput: String,
    onCustomCodeInputChange: (String) -> Unit
) {
    WtaSection(
        title = Strings.activationCodeConfig,
        description = Strings.userActivationCodeHint,
        level = WtaCapabilityLevel.Advanced,
        headerStyle = WtaSectionHeaderStyle.Quiet,
        collapsible = false
    ) {
        WtaToggleRow(
            title = Strings.activationCodeConfig,
            checked = enableActivation,
            onCheckedChange = onEnableActivationChange,
            icon = Icons.Outlined.VpnKey,
            subtitle = Strings.userActivationCodeHint
        )
        if (enableActivation) {
            WtaToggleRow(
                title = Strings.deviceBindingLabel,
                subtitle = Strings.oneCodeOneDeviceAlt,
                checked = enableDeviceBinding,
                onCheckedChange = onEnableDeviceBindingChange
            )
            Text(
                Strings.quickGenerate,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "numeric6" to Strings.numericCodeLabel,
                    "standard" to Strings.standardCodeLabel,
                    "uuid" to Strings.uuidCodeLabel
                ).forEach { (id, label) ->
                    val selected = selectedCodeTemplate == id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(WtaRadius.Button))
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                }
                            )
                            .clickable {
                                onSelectedCodeTemplateChange(id)
                                val gen = when (id) {
                                    "numeric6" -> (1..5).map { String.format("%06d", (100000..999999).random()) }
                                    "standard" -> {
                                        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
                                        (1..5).map { (1..3).joinToString("-") { (1..4).map { chars.random() }.joinToString("") } }
                                    }
                                    else -> (1..5).map { java.util.UUID.randomUUID().toString() }
                                }
                                onActivationCodesChange(activationCodes + gen)
                            }
                    ) {
                        Text(
                            label,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            WtaSettingCard {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PremiumTextField(
                        value = customCodeInput,
                        onValueChange = onCustomCodeInputChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(Strings.enterCustomActivationCode, fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(WtaRadius.Control),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    )
                    FilledTonalButton(
                        onClick = {
                            if (customCodeInput.isNotBlank()) {
                                onActivationCodesChange(activationCodes + customCodeInput.trim())
                                onCustomCodeInputChange("")
                            }
                        },
                        enabled = customCodeInput.isNotBlank(),
                        shape = RoundedCornerShape(WtaRadius.Button),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
            if (activationCodes.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        Strings.addedCodesCount.format(activationCodes.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    TextButton(onClick = {
                        onActivationCodesChange(emptyList())
                        onSelectedCodeTemplateChange(null)
                    }) {
                        Text(Strings.clearAll, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    activationCodes.forEachIndexed { idx, code ->
                        WtaSettingCard {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    code,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                IconButton(
                                    onClick = {
                                        onActivationCodesChange(activationCodes.toMutableList().also { it.removeAt(idx) })
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PublishAppUploadProgress(
    uploadStatus: String,
    uploadProgress: Float
) {
    WtaSettingCard {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        uploadStatus.ifBlank { Strings.uploading },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    "${(uploadProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            LinearProgressIndicator(
                progress = { uploadProgress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PublishAppSubmitAction(
    isPublishing: Boolean,
    isBuilding: Boolean,
    onClick: () -> Unit
) {
    WtaActionBar {
        FilledTonalButton(
            onClick = onClick,
            enabled = !isPublishing && !isBuilding,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(WtaRadius.Button),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (isPublishing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(Strings.publishing, fontWeight = FontWeight.SemiBold)
            } else {
                Icon(Icons.Outlined.Publish, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(Strings.ecosystemPublishApp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PublishAppProjectPickerDialog(
    allProjects: List<com.webtoapp.data.model.WebApp>,
    selectedProject: com.webtoapp.data.model.WebApp?,
    preflightReport: com.webtoapp.core.apkbuilder.ApkExportPreflightReport?,
    isBuilding: Boolean,
    onSelectProject: (com.webtoapp.data.model.WebApp) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(Strings.selectAppToPublish, fontWeight = FontWeight.Bold)
                Text(
                    Strings.localAppsCount.format(allProjects.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (allProjects.isEmpty()) {
                    item {
                        WtaEmptyState(
                            title = Strings.noAppsCreateFirst,
                            message = Strings.noAppsCreateFirst,
                            icon = Icons.Outlined.Apps,
                            actionLabel = Strings.close,
                            onAction = onDismiss
                        )
                    }
                }
                if (!isBuilding && preflightReport?.hasErrors == true) {
                    item {
                        ApkExportPreflightPanel(report = preflightReport)
                    }
                }
                items(allProjects.size) { index ->
                    val project = allProjects[index]
                    WtaSettingCard(onClick = { onSelectProject(project) }) {
                        WtaSettingRow(
                            title = project.name,
                            subtitle = project.appType.name,
                            icon = Icons.Outlined.Android,
                            onClick = { onSelectProject(project) },
                            iconContent = {
                                if (project.iconPath != null) {
                                    AsyncImage(
                                        model = project.iconPath,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        MaterialTheme.colorScheme.primaryContainer,
                                                        MaterialTheme.colorScheme.tertiaryContainer
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.Android,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            trailing = {
                                if (selectedProject?.id == project.id) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        Icons.Outlined.ChevronRight,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.ecosystemCancel)
            }
        }
    )
}

private fun findLatestBuiltApk(
    context: android.content.Context,
    project: com.webtoapp.data.model.WebApp
): java.io.File? {
    val apkBuilder = com.webtoapp.core.apkbuilder.ApkBuilder(context)
    val builtApks = apkBuilder.getBuiltApks()
    val sanitizedName = project.name.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5._-]"), "_")
    return builtApks
        .filter { it.name.contains(sanitizedName, ignoreCase = true) }
        .maxByOrNull { it.lastModified() }
}

private fun mapAppTypeToSourceType(appType: com.webtoapp.data.model.AppType?): String {
    return when (appType) {
        com.webtoapp.data.model.AppType.HTML -> "HTML"
        com.webtoapp.data.model.AppType.FRONTEND -> "FRONTEND"
        com.webtoapp.data.model.AppType.WORDPRESS -> "WORDPRESS"
        com.webtoapp.data.model.AppType.NODEJS_APP -> "NODE"
        com.webtoapp.data.model.AppType.PHP_APP -> "PHP"
        com.webtoapp.data.model.AppType.PYTHON_APP -> "PYTHON"
        com.webtoapp.data.model.AppType.GO_APP -> "GO"
        com.webtoapp.data.model.AppType.GALLERY -> "GALLERY"
        com.webtoapp.data.model.AppType.IMAGE -> "IMAGE"
        com.webtoapp.data.model.AppType.VIDEO -> "VIDEO"
        com.webtoapp.data.model.AppType.MULTI_WEB -> "MULTI_WEB"
        else -> "WEB"
    }
}

private fun buildSourceBundleUrl(project: com.webtoapp.data.model.WebApp): String? {
    val raw = when (project.appType) {
        com.webtoapp.data.model.AppType.HTML,
        com.webtoapp.data.model.AppType.FRONTEND -> project.htmlConfig?.projectDir
        com.webtoapp.data.model.AppType.NODEJS_APP -> project.nodejsConfig?.sourceProjectPath
        com.webtoapp.data.model.AppType.PHP_APP -> project.phpAppConfig?.documentRoot
        com.webtoapp.data.model.AppType.PYTHON_APP -> project.pythonAppConfig?.entryModule
        com.webtoapp.data.model.AppType.GO_APP -> project.goAppConfig?.binaryName
        com.webtoapp.data.model.AppType.WORDPRESS -> project.wordpressConfig?.themeName
        else -> null
    }
    return raw?.takeIf { it.isNotBlank() }?.let { "local://$it" }
}

private fun buildAppManifestJson(
    selectedProject: com.webtoapp.data.model.WebApp?,
    name: String,
    description: String,
    category: String,
    versionName: String,
    versionCode: Int,
    packageName: String?,
    apkUrl: String?,
    sourceBundleUrl: String?
): com.google.gson.JsonObject {
    return com.google.gson.JsonObject().apply {
        addProperty("appType", mapAppTypeToSourceType(selectedProject?.appType))
        addProperty("name", name)
        addProperty("description", description)
        addProperty("category", category)
        addProperty("versionName", versionName)
        addProperty("versionCode", versionCode)
        packageName?.let { addProperty("packageName", it) }
        apkUrl?.let { addProperty("apkUrl", it) }
        sourceBundleUrl?.let { addProperty("sourceBundleUrl", it) }
    }
}
