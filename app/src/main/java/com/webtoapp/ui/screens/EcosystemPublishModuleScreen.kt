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
import com.webtoapp.ui.components.ApkExportPreflightPanel
import com.webtoapp.ui.design.WtaScreen
import com.webtoapp.ui.design.WtaRadius
import com.webtoapp.ui.design.WtaSettingCard
import com.webtoapp.ui.design.WtaStatusBanner
import com.webtoapp.ui.design.WtaStatusTone
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
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EcosystemPublishModuleSheet(
    apiClient: CloudApiClient,
    onDismiss: () -> Unit,
    onPublished: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current


    val extensionManager = remember { com.webtoapp.core.extension.ExtensionManager.getInstance(context) }
    val localModules by extensionManager.modules.collectAsState()


    var selectedModule by remember { mutableStateOf<com.webtoapp.core.extension.ExtensionModule?>(null) }
    var showModulePicker by remember { mutableStateOf(false) }


    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var shareCode by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("tools") }
    var tags by remember { mutableStateOf("") }
    var versionName by remember { mutableStateOf("1.0.0") }
    var versionCode by remember { mutableIntStateOf(1) }
    var isPublishing by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var uploadStatus by remember { mutableStateOf("") }
    var publishFailureReport by remember { mutableStateOf<SheetFailureReport?>(null) }

    fun uriToTempFile(uri: android.net.Uri, prefix: String, ext: String): File? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.$ext")
            tempFile.outputStream().use { out -> input.copyTo(out) }
            input.close()
            tempFile
        } catch (_: Exception) {
            null
        }
    }


    var iconUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var iconUrl by remember { mutableStateOf("") }
    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { iconUri = it } }


    var screenshotUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    val screenshotPickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
    ) { uris -> screenshotUris = screenshotUris + uris }

    val moduleCategories = listOf(
        "tools" to Strings.catTools, "ui" to Strings.catUi, "media" to Strings.catMedia,
        "social" to Strings.catSocial, "productivity" to Strings.catProductivity,
        "education" to Strings.catEducation, "entertainment" to Strings.catEntertainment,
        "developer" to Strings.catDeveloper, "other" to Strings.catOther
    )


    fun mapCategory(cat: com.webtoapp.core.extension.ModuleCategory): String = when (cat) {
        com.webtoapp.core.extension.ModuleCategory.CONTENT_FILTER,
        com.webtoapp.core.extension.ModuleCategory.CONTENT_ENHANCE -> "tools"
        com.webtoapp.core.extension.ModuleCategory.STYLE_MODIFIER,
        com.webtoapp.core.extension.ModuleCategory.THEME -> "ui"
        com.webtoapp.core.extension.ModuleCategory.MEDIA,
        com.webtoapp.core.extension.ModuleCategory.VIDEO,
        com.webtoapp.core.extension.ModuleCategory.IMAGE,
        com.webtoapp.core.extension.ModuleCategory.AUDIO -> "media"
        com.webtoapp.core.extension.ModuleCategory.SOCIAL -> "social"
        com.webtoapp.core.extension.ModuleCategory.FUNCTION_ENHANCE,
        com.webtoapp.core.extension.ModuleCategory.AUTOMATION,
        com.webtoapp.core.extension.ModuleCategory.DATA_EXTRACT,
        com.webtoapp.core.extension.ModuleCategory.DATA_SAVE -> "productivity"
        com.webtoapp.core.extension.ModuleCategory.READING,
        com.webtoapp.core.extension.ModuleCategory.TRANSLATE -> "education"
        com.webtoapp.core.extension.ModuleCategory.SHOPPING,
        com.webtoapp.core.extension.ModuleCategory.NAVIGATION,
        com.webtoapp.core.extension.ModuleCategory.INTERACTION -> "entertainment"
        com.webtoapp.core.extension.ModuleCategory.DEVELOPER,
        com.webtoapp.core.extension.ModuleCategory.SECURITY,
        com.webtoapp.core.extension.ModuleCategory.ANTI_TRACKING -> "developer"
        com.webtoapp.core.extension.ModuleCategory.ACCESSIBILITY,
        com.webtoapp.core.extension.ModuleCategory.OTHER -> "other"
    }


    if (showModulePicker) {
        AlertDialog(
            onDismissRequest = { showModulePicker = false },
            title = {
                Column {
                    Text(Strings.selectModuleToPublish, fontWeight = FontWeight.Bold)
                    Text(
                        Strings.localModulesCount.format(localModules.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            },
            text = {
                if (localModules.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(WtaRadius.IconPlate))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Extension, null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                            }
                            Text(Strings.noLocalModules,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center)
                            Text(Strings.createModuleFirst,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(localModules, key = { it.id }) { module ->
                            val isSelected = selectedModule?.id == module.id
                            WtaSettingCard(
                                onClick = {
                                    selectedModule = module

                                    name = module.name
                                    description = module.description
                                    selectedCategory = mapCategory(module.category)
                                    tags = module.tags.joinToString(",")
                                    versionName = module.version.name
                                    versionCode = module.version.code

                                    val loadedModule = extensionManager.ensureCodeLoaded(module)
                                    shareCode = loadedModule.toShareCode()
                                    showModulePicker = false
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(WtaRadius.IconPlate))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = if (isSelected) 0.14f else 0.08f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Extension, null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(module.name,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                module.category.getDisplayName(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                            Text(
                                                "v\${module.version.name}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Filled.CheckCircle, null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModulePicker = false }) {
                    Text(Strings.ecosystemCancel)
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.92f),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                item {
                    WtaSettingCard(contentPadding = PaddingValues(16.dp)) {
                        Text(
                            Strings.ecosystemPublishModule,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            Strings.selectModuleToPublish,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }


                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text(Strings.selectModule, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        Strings.selectCreatedLocalModule,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                item {
                    WtaSettingCard(onClick = { showModulePicker = true }) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedModule != null) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(WtaRadius.IconPlate))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Extension, null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(selectedModule!!.name, fontWeight = FontWeight.SemiBold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            selectedModule!!.category.getDisplayName(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "v${selectedModule!!.version.name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Icon(Icons.Outlined.SwapHoriz, null, modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Icon(Icons.Outlined.Extension, null, modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    Strings.tapToSelectModule,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }


                    if (selectedModule != null && shareCode.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        WtaStatusBanner(
                            title = Strings.shareCodeAutoGenerated,
                            message = Strings.charCount.format(shareCode.length),
                            tone = WtaStatusTone.Success
                        )
                    }
                }


                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text(Strings.basicInfo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        Strings.moduleNameIconVersion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }


                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(Strings.moduleNameLabel) },
                        placeholder = { Text(Strings.moduleNamePlaceholder) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(WtaRadius.Control),
                        leadingIcon = { Icon(Icons.Outlined.Extension, null, modifier = Modifier.size(20.dp)) }
                    )
                }


                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(56.dp)
                        ) {
                            if (iconUri != null) {
                                AsyncImage(
                                    model = iconUri,
                                    contentDescription = Strings.iconPreview,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(WtaRadius.IconPlate)),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (iconUrl.isNotBlank()) {
                                AsyncImage(
                                    model = iconUrl,
                                    contentDescription = Strings.iconPreview,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(WtaRadius.IconPlate)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(WtaRadius.IconPlate))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Extension, null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { iconPickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(WtaRadius.Button),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.AddPhotoAlternate, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (iconUri != null) Strings.changeIcon else Strings.selectIcon)
                            }
                            Text(Strings.selectModuleIconFromGallery,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }


                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text("Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        Strings.selectModuleCategory,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(moduleCategories) { (key, label) ->
                            PremiumFilterChip(
                                selected = selectedCategory == key,
                                onClick = { selectedCategory = key },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }


                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = versionName,
                            onValueChange = { versionName = it },
                            label = { Text(Strings.versionNameLabel) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(WtaRadius.Control)
                        )
                        OutlinedTextField(
                            value = versionCode.toString(),
                            onValueChange = { versionCode = it.toIntOrNull() ?: 1 },
                            label = { Text(Strings.versionCodeLabel) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(WtaRadius.Control)
                        )
                    }
                }


                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text(Strings.descriptionAndTags, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        Strings.describeModuleFeatures,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(Strings.moduleDescLabel) },
                        placeholder = { Text(Strings.moduleDescPlaceholder) },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(WtaRadius.Control)
                    )
                }

                item {
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text(Strings.tagsLabel) },
                        placeholder = { Text(Strings.moduleTagsPlaceholder) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(WtaRadius.Control)
                    )
                }


                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text(Strings.screenshotsOptional, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        Strings.addScreenshotsPreview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                if (screenshotUris.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(screenshotUris.size) { index ->
                                Box(modifier = Modifier.size(85.dp, 150.dp)) {
                                    AsyncImage(
                                        model = screenshotUris[index],
                                        contentDescription = Strings.screenshotLabel.format(index + 1),
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(WtaRadius.Control)),
                                        contentScale = ContentScale.Crop
                                    )
                                    FilledIconButton(
                                        onClick = {
                                            screenshotUris = screenshotUris.toMutableList().also { it.removeAt(index) }
                                        },
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
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { screenshotPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(WtaRadius.Button)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.AddPhotoAlternate, null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Strings.addScreenshotFromGallery,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (screenshotUris.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(Strings.addedScreenshotsCount.format(screenshotUris.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }


                if (selectedModule == null) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape))
                            Text(Strings.manualShareCodeTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(Strings.noShareCodeLocalModule,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }

                    item {
                        OutlinedTextField(
                            value = shareCode,
                            onValueChange = { shareCode = it },
                            label = { Text(Strings.moduleShareCodeLabel) },
                            placeholder = { Text(Strings.pasteModuleShareCode) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(WtaRadius.Control),
                            supportingText = { Text(Strings.shareCodeExportHint) },
                            minLines = 3,
                            maxLines = 6
                        )
                    }
                }


                if (isPublishing && uploadProgress > 0f) {
                    item {
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
                                        Text(uploadStatus.ifBlank { Strings.uploading },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium)
                                    }
                                    Text("${(uploadProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(5.dp)
                                        .clip(RoundedCornerShape(WtaRadius.Button))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(uploadProgress.coerceIn(0f, 1f))
                                            .fillMaxHeight()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                                ),
                                                RoundedCornerShape(WtaRadius.Button)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }


                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    FilledTonalButton(
                        enabled = !isPublishing,
                        onClick = {
                            if (name.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar(Strings.enterModuleName) }
                                return@FilledTonalButton
                            }
                            if (shareCode.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar(Strings.selectModuleOrShareCode) }
                                return@FilledTonalButton
                            }
                            scope.launch {
                                isPublishing = true
                                uploadProgress = 0f


                                var finalIconUrl = iconUrl.ifBlank { null }
                                if (iconUri != null) {
                                    uploadStatus = Strings.uploadingIconAlready
                                    uploadProgress = 0.1f
                                    val iconFile = uriToTempFile(iconUri!!, "icon", "png")
                                    if (iconFile != null) {
                                        when (val r = apiClient.uploadAsset(iconFile, "image/png")) {
                                            is com.webtoapp.core.auth.AuthResult.Success -> finalIconUrl = r.data
                                            is com.webtoapp.core.auth.AuthResult.Error -> {
                                                val summary = Strings.iconUploadFailed.format(r.message)
                                                publishFailureReport = buildSheetFailureReport(
                                                    title = Strings.modulePublishFailed,
                                                    stage = Strings.uploadIconStage,
                                                    summary = summary,
                                                    contextLines = listOf(
                                                        "module=${selectedModule?.name ?: "manual"}",
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
                                            title = Strings.modulePublishFailed,
                                            stage = Strings.readIconStage,
                                            summary = summary,
                                            contextLines = listOf(
                                                "module=${selectedModule?.name ?: "manual"}",
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


                                val allScreenshotUrls = mutableListOf<String>()
                                if (screenshotUris.isNotEmpty()) {
                                    val total = screenshotUris.size
                                    for ((idx, uri) in screenshotUris.withIndex()) {
                                        uploadStatus = Strings.uploadingScreenshot.format(idx + 1, total)
                                        uploadProgress = 0.2f + (idx.toFloat() / (total + 2)) * 0.5f
                                        val shotFile = uriToTempFile(uri, "module_screenshot_$idx", "png")
                                        if (shotFile != null) {
                                            when (val r = apiClient.uploadAsset(shotFile, "image/png")) {
                                                is com.webtoapp.core.auth.AuthResult.Success -> allScreenshotUrls.add(r.data)
                                                is com.webtoapp.core.auth.AuthResult.Error -> {
                                                    val summary = Strings.screenshotUploadFailed.format(idx + 1, r.message)
                                                    publishFailureReport = buildSheetFailureReport(
                                                        title = Strings.modulePublishFailed,
                                                        stage = Strings.uploadScreenshotStage,
                                                        summary = summary,
                                                        contextLines = listOf(
                                                            "module=${selectedModule?.name ?: "manual"}",
                                                            "screenshotIndex=${idx + 1}",
                                                            "screenshotUri=$uri",
                                                            "name=$name"
                                                        )
                                                    )
                                                    shotFile.delete()
                                                    isPublishing = false
                                                    uploadProgress = 0f
                                                    uploadStatus = summary
                                                    return@launch
                                                }
                                            }
                                            shotFile.delete()
                                        }
                                    }
                                }

                                uploadStatus = Strings.publishingModule
                                uploadProgress = 0.8f

                                try {
                                    val result = apiClient.publishModule(
                                        name = name,
                                        description = description,
                                        icon = finalIconUrl,
                                        category = selectedCategory,
                                        tags = tags.ifBlank { null },
                                        versionName = versionName.ifBlank { null },
                                        versionCode = versionCode,
                                        shareCode = shareCode,
                                        visibility = "public",
                                        reviewMode = "public_first",
                                        screenshots = allScreenshotUrls
                                    )
                                    uploadProgress = 1f
                                    when (result) {
                                        is com.webtoapp.core.auth.AuthResult.Success -> {
                                            snackbarHostState.showSnackbar(Strings.ecosystemPublishModuleSuccess)
                                            onPublished()
                                        }
                                        is com.webtoapp.core.auth.AuthResult.Error -> {
                                            val summary = Strings.publishFailed.format(result.message)
                                            publishFailureReport = buildSheetFailureReport(
                                                title = Strings.modulePublishFailed,
                                                stage = Strings.submitModuleInfoStage,
                                                summary = summary,
                                                contextLines = listOf(
                                                    "module=${selectedModule?.name ?: "manual"}",
                                                    "name=$name",
                                                    "versionName=$versionName",
                                                    "versionCode=$versionCode",
                                                    "selectedCategory=$selectedCategory"
                                                )
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    val summary = Strings.networkError.format(e.message)
                                    publishFailureReport = buildSheetFailureReport(
                                        title = Strings.modulePublishFailed,
                                        stage = Strings.requestPublishStage,
                                        summary = summary,
                                        contextLines = listOf(
                                            "module=${selectedModule?.name ?: "manual"}",
                                            "name=$name",
                                            "versionName=$versionName",
                                            "versionCode=$versionCode"
                                        ),
                                        throwable = e
                                    )
                                } finally {
                                    isPublishing = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(WtaRadius.Button)
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Strings.publishing, fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(Icons.Outlined.Publish, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Strings.ecosystemPublishModule, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }

    publishFailureReport?.let { report ->
        SheetFailureReportDialog(
            report = report,
            onDismiss = { publishFailureReport = null }
        )
    }
}
