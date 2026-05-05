package com.webtoapp.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.webtoapp.core.cloud.CloudApiClient
import com.webtoapp.core.cloud.ReviewSubmissionInfo
import com.webtoapp.core.cloud.StoreModuleInfo
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.design.WtaRadius
import com.webtoapp.ui.design.WtaStatusBanner
import com.webtoapp.ui.design.WtaStatusTone
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EcosystemModuleManagementSheet(
    module: StoreModuleInfo,
    apiClient: CloudApiClient,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentModule by remember(module) { mutableStateOf(module) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val gradients = rememberMgmtGradients()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.94f),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            EnhancedElevatedCard(
                shape = RoundedCornerShape(WtaRadius.Card),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(WtaRadius.Card))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Extension,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.size(14.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = currentModule.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ModuleStatusBadge("v${currentModule.versionName ?: "1.0.0"}", MaterialTheme.colorScheme.primary)
                            ModuleStatusBadge("Module Console", MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModuleTabChip(
                    label = "概览",
                    icon = Icons.Outlined.Settings,
                    selected = selectedTab == 0,
                    gradient = gradients.blue,
                    onClick = { selectedTab = 0 }
                )
                ModuleTabChip(
                    label = "提审",
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    selected = selectedTab == 1,
                    gradient = gradients.purple,
                    onClick = { selectedTab = 1 }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> ModuleOverviewTab(module = currentModule)
                else -> ModuleReviewTab(
                    module = currentModule,
                    apiClient = apiClient,
                    onModuleChanged = { currentModule = it }
                )
            }
        }
    }
}

@Composable
private fun ModuleOverviewTab(module: StoreModuleInfo) {
    val tone = moduleReviewTone(module.reviewStatus)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ModuleMetricCard(
                    label = "下载",
                    value = module.downloads.toString(),
                    modifier = Modifier.weight(1f)
                )
                ModuleMetricCard(
                    label = "评分",
                    value = String.format("%.1f", module.rating),
                    modifier = Modifier.weight(1f)
                )
                ModuleMetricCard(
                    label = "点赞",
                    value = module.likeCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            WtaStatusBanner(
                message = "审核状态：${moduleReviewLabel(module.reviewStatus)}",
                tone = tone
            )
        }
        item {
            EnhancedElevatedCard(
                shape = RoundedCornerShape(WtaRadius.Card),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModuleInfoRow("可见性", moduleVisibilityLabel(module.visibility))
                    ModuleInfoRow("审核模式", moduleReviewModeLabel(module.reviewMode))
                    ModuleInfoRow("验证状态", moduleVerificationLabel(module.verificationStatus))
                    module.runtimeKey?.takeIf { it.isNotBlank() }?.let {
                        ModuleInfoRow("Runtime Key", it)
                    }
                    module.createdAt?.let { ModuleInfoRow("创建时间", it) }
                    module.updatedAt?.let { ModuleInfoRow("更新时间", it) }
                }
            }
        }
        if (!module.description.isNullOrBlank()) {
            item {
                EnhancedElevatedCard(
                    shape = RoundedCornerShape(WtaRadius.Card),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("描述", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = module.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
        module.rejectionReason?.takeIf { it.isNotBlank() }?.let { reason ->
            item {
                WtaStatusBanner(
                    message = "拒绝原因：$reason",
                    tone = WtaStatusTone.Error
                )
            }
        }
        module.adminReplyText?.takeIf { it.isNotBlank() }?.let { reply ->
            item {
                WtaStatusBanner(
                    message = "管理员回复：$reply",
                    tone = WtaStatusTone.Info
                )
            }
        }
    }
}

@Composable
private fun ModuleReviewTab(
    module: StoreModuleInfo,
    apiClient: CloudApiClient,
    onModuleChanged: (StoreModuleInfo) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var submissions by remember { mutableStateOf<List<ReviewSubmissionInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var reviewMode by remember(module) { mutableStateOf(module.reviewMode ?: "public_first") }
    var requestedVisibility by remember(module) { mutableStateOf(module.visibility ?: "public") }
    var submitMessage by remember { mutableStateOf("") }
    var assetUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitProgress by remember { mutableFloatStateOf(0f) }
    var submitStatus by remember { mutableStateOf<String?>(null) }

    val assetPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        assetUris = (assetUris + uris).take(8)
    }

    fun uriToTempFile(uri: android.net.Uri, prefix: String): File? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val ext = context.contentResolver.getType(uri)?.substringAfterLast('/') ?: "png"
            val tempFile = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.$ext")
            tempFile.outputStream().use { output -> input.copyTo(output) }
            input.close()
            tempFile
        } catch (_: Exception) {
            null
        }
    }

    fun load() {
        scope.launch {
            isLoading = true
            errorMsg = null
            when (val result = apiClient.getModuleReviewSubmissions(module.id)) {
                is com.webtoapp.core.auth.AuthResult.Success -> submissions = result.data
                is com.webtoapp.core.auth.AuthResult.Error -> errorMsg = result.message
            }
            isLoading = false
        }
    }

    fun submit() {
        scope.launch {
            isSubmitting = true
            submitStatus = null
            val assetUrls = mutableListOf<String>()
            for ((index, uri) in assetUris.withIndex()) {
                submitStatus = "正在上传图片 ${index + 1}/${assetUris.size}"
                submitProgress = if (assetUris.isEmpty()) 0f else index.toFloat() / assetUris.size.toFloat()
                val tempFile = uriToTempFile(uri, "module_review")
                if (tempFile == null) {
                    errorMsg = "读取提审图片失败"
                    isSubmitting = false
                    return@launch
                }
                when (val upload = apiClient.uploadAsset(tempFile, "image/png")) {
                    is com.webtoapp.core.auth.AuthResult.Success -> assetUrls.add(upload.data)
                    is com.webtoapp.core.auth.AuthResult.Error -> {
                        tempFile.delete()
                        errorMsg = upload.message
                        isSubmitting = false
                        return@launch
                    }
                }
                tempFile.delete()
            }

            submitStatus = "正在提交提审"
            when (
                val result = apiClient.submitModuleReviewSubmission(
                    moduleId = module.id,
                    reviewMode = reviewMode,
                    requestedVisibility = requestedVisibility,
                    submitMessage = submitMessage.ifBlank { null },
                    assetUrls = assetUrls,
                )
            ) {
                is com.webtoapp.core.auth.AuthResult.Success -> {
                    submitMessage = ""
                    assetUris = emptyList()
                    submitProgress = 1f
                    submitStatus = "提审已提交"
                    onModuleChanged(
                        module.copy(
                            visibility = requestedVisibility,
                            reviewMode = reviewMode,
                            reviewStatus = if (reviewMode == "review_first") "pending_review" else "public_unreviewed",
                            rejectionReason = null,
                            adminReplyText = null,
                        )
                    )
                    load()
                }
                is com.webtoapp.core.auth.AuthResult.Error -> {
                    errorMsg = result.message
                }
            }
            isSubmitting = false
        }
    }

    LaunchedEffect(module.id) { load() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            EnhancedElevatedCard(
                shape = RoundedCornerShape(WtaRadius.Card),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null)
                        Text("重新提审", fontWeight = FontWeight.Bold)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("公开方式", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = requestedVisibility == "public",
                                onClick = { requestedVisibility = "public" },
                                label = { Text("公开") }
                            )
                            FilterChip(
                                selected = requestedVisibility == "private",
                                onClick = { requestedVisibility = "private" },
                                label = { Text("私有") }
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("审核模式", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = reviewMode == "public_first",
                                onClick = { reviewMode = "public_first" },
                                label = { Text("先公开后审") }
                            )
                            FilterChip(
                                selected = reviewMode == "review_first",
                                onClick = { reviewMode = "review_first" },
                                label = { Text("先审后公开") }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = submitMessage,
                        onValueChange = { submitMessage = it.take(5000) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("补充说明") },
                        placeholder = { Text("可填写本次修改内容、审核说明或申诉理由") },
                        minLines = 3,
                        maxLines = 5
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "附件图片 ${assetUris.size}/8",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = { assetPicker.launch("image/*") },
                                enabled = !isSubmitting
                            ) {
                                Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("选择图片")
                            }
                        }
                        if (assetUris.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(assetUris, key = { it.toString() }) { uri ->
                                    Surface(
                                        shape = RoundedCornerShape(WtaRadius.Control),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Text(
                                                text = uri.lastPathSegment ?: "image",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.heightIn(min = 16.dp)
                                            )
                                            IconButton(
                                                onClick = { assetUris = assetUris.filterNot { it == uri } },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(Icons.Outlined.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    submitStatus?.let {
                        WtaStatusBanner(
                            message = if (isSubmitting && submitProgress > 0f) "$it ${(submitProgress * 100).toInt()}%" else it,
                            tone = WtaStatusTone.Info
                        )
                    }
                    errorMsg?.let {
                        WtaStatusBanner(
                            message = it,
                            tone = WtaStatusTone.Error
                        )
                    }

                    FilledTonalButton(
                        onClick = { submit() },
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isSubmitting) "提交中..." else "提交提审")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("提审历史", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = { load() }, enabled = !isLoading) {
                    Text(Strings.retry)
                }
            }
        }

        if (isLoading) {
            item { WtaStatusBanner(message = Strings.loading, tone = WtaStatusTone.Info) }
        } else if (errorMsg != null && submissions.isEmpty()) {
            item { WtaStatusBanner(message = errorMsg ?: Strings.loading, tone = WtaStatusTone.Error) }
        } else if (submissions.isEmpty()) {
            item { WtaStatusBanner(message = Strings.noData, tone = WtaStatusTone.Info) }
        } else {
            items(submissions, key = { it.id }) { submission ->
                EnhancedElevatedCard(
                    shape = RoundedCornerShape(WtaRadius.Card),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ModuleStatusBadge(moduleReviewLabel(submission.status), moduleReviewColor(submission.status))
                            ModuleStatusBadge(moduleReviewModeLabel(submission.reviewMode), MaterialTheme.colorScheme.tertiary)
                            ModuleStatusBadge(moduleVisibilityLabel(submission.requestedVisibility), MaterialTheme.colorScheme.primary)
                        }

                        submission.submitMessage?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                        submission.rejectionReason?.takeIf { it.isNotBlank() }?.let {
                            WtaStatusBanner(message = "拒绝原因：$it", tone = WtaStatusTone.Error)
                        }
                        submission.adminReplyText?.takeIf { it.isNotBlank() }?.let {
                            WtaStatusBanner(message = "管理员回复：$it", tone = WtaStatusTone.Info)
                        }
                        if (submission.submissionAssets.isNotEmpty()) {
                            ModuleAssetStrip(title = "提交附件", urls = submission.submissionAssets)
                        }
                        if (submission.decisionAssets.isNotEmpty()) {
                            ModuleAssetStrip(title = "回复附件", urls = submission.decisionAssets)
                        }
                        submission.createdAt?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    EnhancedElevatedCard(
        shape = RoundedCornerShape(WtaRadius.Card),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ModuleInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ModuleStatusBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(WtaRadius.Button),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun ModuleTabChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    gradient: List<Color>,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(WtaRadius.Control)),
        color = Color.Transparent,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(WtaRadius.Control))
                .background(
                    if (selected) {
                        Brush.linearGradient(gradient)
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceContainerHighest,
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        )
                    }
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ModuleAssetStrip(title: String, urls: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(urls, key = { it }) { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(WtaRadius.Control)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

private fun moduleVisibilityLabel(value: String?): String = when (value) {
    "private" -> "私有"
    else -> "公开"
}

private fun moduleReviewModeLabel(value: String?): String = when (value) {
    "review_first" -> "先审后公开"
    else -> "先公开后审"
}

private fun moduleReviewLabel(value: String?): String = when (value) {
    "public_reviewed" -> "已审公开"
    "public_unreviewed" -> "公开未审"
    "pending_review" -> "审核中"
    "approved" -> "已通过"
    "rejected" -> "已拒绝"
    "pending" -> "待审核"
    else -> "草稿"
}

private fun moduleVerificationLabel(value: String?): String = when (value) {
    "verified" -> "已验证"
    "private_only" -> "仅私有"
    "not_required" -> "无需验证"
    else -> value ?: "未知"
}

private fun moduleReviewColor(value: String?): Color = when (value) {
    "public_reviewed", "approved" -> Color(0xFF2E7D32)
    "public_unreviewed", "pending_review", "pending" -> Color(0xFFE65100)
    "rejected" -> Color(0xFFC62828)
    else -> Color(0xFF546E7A)
}

private fun moduleReviewTone(value: String?): WtaStatusTone = when (value) {
    "public_reviewed", "approved" -> WtaStatusTone.Success
    "rejected" -> WtaStatusTone.Error
    "public_unreviewed", "pending_review", "pending" -> WtaStatusTone.Warning
    else -> WtaStatusTone.Info
}
