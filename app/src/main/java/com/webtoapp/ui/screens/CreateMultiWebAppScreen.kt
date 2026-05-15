package com.webtoapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.MultiWebConfig
import com.webtoapp.data.model.MultiWebSite
import com.webtoapp.data.model.HtmlFileType
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.PremiumTextField
import com.webtoapp.ui.components.RuntimeIconPickerCard
import com.webtoapp.ui.screens.create.WtaCreateFlowScaffold
import com.webtoapp.ui.screens.create.WtaCreateFlowSection
import java.util.UUID




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMultiWebAppScreen(
    existingAppId: Long = 0L,
    onBack: () -> Unit,
    onCreated: (
        name: String,
        multiWebConfig: MultiWebConfig,
        iconUri: Uri?,
        themeType: String
    ) -> Unit
) {
    val isEdit = existingAppId > 0L


    var appName by remember { mutableStateOf("") }
    var appIcon by remember { mutableStateOf<Uri?>(null) }
    var landscapeMode by remember { mutableStateOf(false) }


    var sites by remember { mutableStateOf<List<MultiWebSite>>(emptyList()) }


    var displayMode by remember { mutableStateOf("TABS") }


    var existingApps by remember { mutableStateOf<List<com.webtoapp.data.model.WebApp>>(emptyList()) }
    LaunchedEffect(Unit) {
        val repo = org.koin.java.KoinJavaComponent.get<com.webtoapp.data.repository.WebAppRepository>(
            com.webtoapp.data.repository.WebAppRepository::class.java
        )
        repo.allWebApps.collect { existingApps = it }
    }


    var refreshInterval by remember { mutableStateOf(30) }


    var showAddSiteDialog by remember { mutableStateOf(false) }
    var editingSite by remember { mutableStateOf<MultiWebSite?>(null) }
    var pendingLocalSiteUri by remember { mutableStateOf<Uri?>(null) }


    LaunchedEffect(existingAppId) {
        if (existingAppId > 0L) {
            val existingApp = org.koin.java.KoinJavaComponent
                .get<com.webtoapp.data.repository.WebAppRepository>(
                    com.webtoapp.data.repository.WebAppRepository::class.java
                ).getWebApp(existingAppId)
            existingApp?.let { app ->
                appName = app.name
                app.iconPath?.let { appIcon = android.net.Uri.parse(it) }
                app.multiWebConfig?.let { config ->
                    sites = config.sites
                    displayMode = config.displayMode
                    refreshInterval = config.refreshInterval
                    landscapeMode = config.landscapeMode
                }
            }
        }
    }

    val iconPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { appIcon = it } }

    val localSiteFilePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> pendingLocalSiteUri = uri }

    val canCreate = sites.isNotEmpty()
    val accentColor = MaterialTheme.colorScheme.onSurface

    WtaCreateFlowScaffold(
        title = if (isEdit) Strings.editApp else Strings.createMultiWebApp,
        onBack = onBack,
        actions = {
            TextButton(
                onClick = {
                    onCreated(
                        appName.ifBlank { "Multi-Site App" },
                        MultiWebConfig(
                            sites = sites,
                            displayMode = displayMode,
                            refreshInterval = refreshInterval,
                            showSiteIcons = true,
                            landscapeMode = landscapeMode,
                            projectId = ""
                        ),
                        appIcon,
                        "AURORA"
                    )
                },
                enabled = canCreate
            ) {
                Text(
                    if (isEdit) Strings.btnSave else Strings.btnCreate,
                    fontWeight = FontWeight.SemiBold,
                    color = if (canCreate) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WtaCreateFlowSection(title = Strings.labelBasicInfo) {
                    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            RuntimeIconPickerCard(
                                appIcon = appIcon,
                                onSelectIcon = { iconPickerLauncher.launch("image/*") }
                            )
                            PremiumTextField(
                                value = appName,
                                onValueChange = { appName = it },
                                label = { Text(Strings.labelAppName) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
            }


            WtaCreateFlowSection(title = Strings.appConfig) {
                EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            Strings.multiWebDisplayMode,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val modes = listOf("TABS", "CARDS", "FEED", "DRAWER")
                            val modeLabels = listOf(
                                Strings.multiWebModeTabs,
                                Strings.multiWebModeCards,
                                Strings.multiWebModeFeed,
                                Strings.multiWebModeDrawer
                            )
                            modes.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                                    onClick = { displayMode = mode },
                                    selected = displayMode == mode
                                ) {
                                    Text(modeLabels[index])
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            when (displayMode) {
                                "TABS" -> Strings.multiWebModeTabsDesc
                                "CARDS" -> Strings.multiWebModeCardsDesc
                                "FEED" -> Strings.multiWebModeFeedDesc
                                else -> Strings.multiWebModeDrawerDesc
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )


                        AnimatedVisibility(
                            visible = displayMode == "FEED",
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = accentColor.copy(alpha = 0.08f)
                            ) {
                                Text(
                                    Strings.multiWebFeedTip,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = accentColor,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }


            WtaCreateFlowSection(title = Strings.preview) {
                EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                Strings.multiWebSiteList,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (sites.isNotEmpty()) {
                                Text(
                                    Strings.multiWebSiteCount.replace("%d", sites.size.toString()),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (sites.isEmpty()) {

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Outlined.Apps, null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    Strings.multiWebNoSites,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = {
                                        editingSite = null
                                        showAddSiteDialog = true
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(Strings.multiWebAddSite)
                                }
                            }
                        } else {

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                sites.forEachIndexed { index, site ->
                                    SiteItem(
                                        site = site,
                                        showFeedConfig = displayMode == "FEED",
                                        onEdit = {
                                            editingSite = site
                                            showAddSiteDialog = true
                                        },
                                        onDelete = {
                                            sites = sites.toMutableList().also { it.removeAt(index) }
                                        },
                                        onToggleEnabled = { enabled ->
                                            sites = sites.toMutableList().also {
                                                it[index] = site.copy(enabled = enabled)
                                            }
                                        },
                                        onMoveUp = if (index > 0) {
                                            { sites = sites.toMutableList().also { val item = it.removeAt(index); it.add(index - 1, item) } }
                                        } else null,
                                        onMoveDown = if (index < sites.size - 1) {
                                            { sites = sites.toMutableList().also { val item = it.removeAt(index); it.add(index + 1, item) } }
                                        } else null
                                    )
                                }


                                OutlinedButton(
                                    onClick = {
                                        editingSite = null
                                        showAddSiteDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(Strings.multiWebAddSite)
                                }
                            }
                        }
                    }
                }
            }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

    if (showAddSiteDialog) {
        AddSiteDialog(
            editingSite = editingSite,
            showFeedFields = displayMode == "FEED",
            newSortIndex = sites.size,
            existingApps = existingApps,
            pendingLocalSiteUri = pendingLocalSiteUri,
            onPickLocalSiteFile = { localSiteFilePickerLauncher.launch("text/html") },
            onConsumeLocalSiteFile = { pendingLocalSiteUri = null },
            onDismiss = {
                showAddSiteDialog = false
                editingSite = null
                pendingLocalSiteUri = null
            },
            onSave = { site ->
                if (editingSite != null) {
                    sites = sites.map { if (it.id == editingSite!!.id) site else it }
                } else {
                    sites = sites + site
                }
                showAddSiteDialog = false
                editingSite = null
                pendingLocalSiteUri = null
            },
            onBatchSave = { newSites ->
                sites = sites + newSites
                showAddSiteDialog = false
                editingSite = null
                pendingLocalSiteUri = null
            }
        )
    }
}





@Composable
private fun SiteItem(
    site: MultiWebSite,
    showFeedConfig: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (site.enabled)
            MaterialTheme.colorScheme.surfaceContainerLow
        else
            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Apps, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))


            Column(modifier = Modifier.weight(1f)) {
                Text(
                    site.name.ifBlank { Strings.multiWebTypeExisting },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    site.url.ifBlank { Strings.multiWebTypeExisting },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (showFeedConfig && site.cssSelector.isNotBlank()) {
                    Text(
                        "CSS: ${site.cssSelector}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }


            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(Strings.multiWebEditSite) },
                        onClick = { showMenu = false; onEdit() },
                        leadingIcon = { Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text(if (site.enabled) Strings.multiWebDisableSite else Strings.multiWebEnableSite) },
                        onClick = { showMenu = false; onToggleEnabled(!site.enabled) },
                        leadingIcon = {
                            Icon(
                                if (site.enabled) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                null, modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    if (onMoveUp != null || onMoveDown != null) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(Strings.multiWebMoveUp) },
                            onClick = { showMenu = false; onMoveUp?.invoke() },
                            leadingIcon = { Icon(Icons.Outlined.KeyboardArrowUp, null, modifier = Modifier.size(18.dp)) },
                            enabled = onMoveUp != null
                        )
                        DropdownMenuItem(
                            text = { Text(Strings.multiWebMoveDown) },
                            onClick = { showMenu = false; onMoveDown?.invoke() },
                            leadingIcon = { Icon(Icons.Outlined.KeyboardArrowDown, null, modifier = Modifier.size(18.dp)) },
                            enabled = onMoveDown != null
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(Strings.multiWebDeleteSite, color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = {
                            Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }
        }
    }
}





@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSiteDialog(
    editingSite: MultiWebSite?,
    showFeedFields: Boolean,
    newSortIndex: Int = 0,
    existingApps: List<com.webtoapp.data.model.WebApp> = emptyList(),
    pendingLocalSiteUri: Uri? = null,
    onPickLocalSiteFile: () -> Unit = {},
    onConsumeLocalSiteFile: () -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (MultiWebSite) -> Unit,
    onBatchSave: (List<MultiWebSite>) -> Unit = {}
) {
    val accentColor = MaterialTheme.colorScheme.onSurface


    if (editingSite != null) {
        EditSiteDialog(
            editingSite = editingSite,
            showFeedFields = showFeedFields,
            existingApps = existingApps,
            pendingLocalSiteUri = pendingLocalSiteUri,
            onPickLocalSiteFile = onPickLocalSiteFile,
            onConsumeLocalSiteFile = onConsumeLocalSiteFile,
            onDismiss = onDismiss,
            onSave = onSave
        )
        return
    }


    var sourceType by remember { mutableStateOf("URL") }
    var siteName by remember { mutableStateOf("") }
    var siteUrl by remember { mutableStateOf("") }
    var localFileName by remember { mutableStateOf("") }
    var localFileUri by remember { mutableStateOf("") }
    var cssSelector by remember { mutableStateOf("") }
    var selectedAppIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var filterType by remember { mutableStateOf<String?>(null) }
    var filterCategoryId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(pendingLocalSiteUri) {
        pendingLocalSiteUri?.let { uri ->
            val pickedName = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "index.html"
            localFileName = pickedName
            localFileUri = uri.toString()
            if (siteName.isBlank()) siteName = pickedName.substringBeforeLast('.')
            onConsumeLocalSiteFile()
        }
    }


    val eligibleApps = existingApps.filter { it.appType != com.webtoapp.data.model.AppType.MULTI_WEB }


    val availableTypes = remember(eligibleApps) {
        eligibleApps.map { it.appType.name }.distinct()
    }


    var categories by remember { mutableStateOf<List<com.webtoapp.data.model.AppCategory>>(emptyList()) }
    LaunchedEffect(Unit) {
        val repo = org.koin.java.KoinJavaComponent.get<com.webtoapp.data.repository.AppCategoryRepository>(
            com.webtoapp.data.repository.AppCategoryRepository::class.java
        )
        repo.allCategories.collect { categories = it }
    }


    val filteredApps = remember(eligibleApps, filterType, filterCategoryId) {
        eligibleApps.filter { app ->
            val typeMatch = filterType == null || app.appType.name == filterType
            val categoryMatch = when {
                filterCategoryId == null -> true
                filterCategoryId == -1L -> app.categoryId == null
                else -> app.categoryId == filterCategoryId
            }
            typeMatch && categoryMatch
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Outlined.AddCircleOutline, null, tint = accentColor, modifier = Modifier.size(32.dp))
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(Strings.multiWebAddSite, fontWeight = FontWeight.SemiBold)
                if (sourceType == "EXISTING" && selectedAppIds.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accentColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            "${selectedAppIds.size}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val types = listOf("URL", "LOCAL", "EXISTING")
                    val labels = listOf(Strings.multiWebTypeUrl, Strings.multiWebTypeLocal, Strings.multiWebTypeExisting)
                    types.forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = sourceType == type,
                            onClick = { sourceType = type },
                            shape = SegmentedButtonDefaults.itemShape(index, types.size)
                        ) {
                            Text(labels[index], maxLines = 1)
                        }
                    }
                }

                if (sourceType == "URL") {
                    PremiumTextField(
                        value = siteName,
                        onValueChange = { siteName = it },
                        label = { Text(Strings.multiWebSiteName) },
                        leadingIcon = { Icon(Icons.Outlined.Label, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    PremiumTextField(
                        value = siteUrl,
                        onValueChange = {
                            siteUrl = it
                            if (siteName.isBlank()) siteName = guessSiteNameFromUrl(it)
                        },
                        label = { Text(Strings.multiWebSiteUrl) },
                        placeholder = { Text("https://example.com") },
                        leadingIcon = { Icon(Icons.Outlined.Link, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        )
                    )
                    if (showFeedFields) {
                        PremiumTextField(
                            value = cssSelector,
                            onValueChange = { cssSelector = it },
                            label = { Text(Strings.multiWebCssSelector) },
                            placeholder = { Text(Strings.multiWebCssSelectorHint) },
                            leadingIcon = { Icon(Icons.Outlined.FilterAlt, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else if (sourceType == "LOCAL") {
                    PremiumTextField(
                        value = siteName,
                        onValueChange = { siteName = it },
                        label = { Text(Strings.multiWebSiteName) },
                        leadingIcon = { Icon(Icons.Outlined.Label, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = onPickLocalSiteFile,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.UploadFile, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(localFileName.ifBlank { Strings.multiWebSelectFile })
                    }
                    if (showFeedFields) {
                        PremiumTextField(
                            value = cssSelector,
                            onValueChange = { cssSelector = it },
                            label = { Text(Strings.multiWebCssSelector) },
                            placeholder = { Text(Strings.multiWebCssSelectorHint) },
                            leadingIcon = { Icon(Icons.Outlined.FilterAlt, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    if (eligibleApps.isNotEmpty() && availableTypes.size > 1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = filterType == null,
                                onClick = { filterType = null },
                                label = { Text(Strings.all, fontSize = 12.sp) }
                            )
                        }
                        items(availableTypes.size) { index ->
                            val type = availableTypes[index]
                            val (icon, label) = appTypeFilterInfo(type)
                            FilterChip(
                                selected = filterType == type,
                                onClick = { filterType = if (filterType == type) null else type },
                                label = { Text(label, fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(icon, null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                    }


                    if (eligibleApps.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = filterCategoryId == null,
                                onClick = { filterCategoryId = null },
                                label = { Text(Strings.allApps, fontSize = 12.sp) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = filterCategoryId == -1L,
                                onClick = { filterCategoryId = -1L },
                                label = { Text(Strings.uncategorized, fontSize = 12.sp) }
                            )
                        }
                        items(categories.size) { index ->
                            val cat = categories[index]
                            FilterChip(
                                selected = filterCategoryId == cat.id,
                                onClick = { filterCategoryId = if (filterCategoryId == cat.id) null else cat.id },
                                label = { Text(cat.name, fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        com.webtoapp.util.SvgIconMapper.getIcon(cat.icon),
                                        null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }
                    }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))


                    if (eligibleApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Apps, null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                Strings.multiWebNoApps,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            Strings.noSearchResult,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    } else {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                selectedAppIds = if (selectedAppIds.size == filteredApps.size) {
                                    emptySet()
                                } else {
                                    filteredApps.map { it.id }.toSet()
                                }
                            }
                        ) {
                            Text(
                                if (selectedAppIds.size == filteredApps.size) Strings.deselectAll else Strings.selectAll,
                                fontSize = 12.sp,
                                color = accentColor
                            )
                        }
                    }

                    filteredApps.forEach { app ->
                        val isSelected = app.id in selectedAppIds
                        Card(
                            onClick = {
                                selectedAppIds = if (isSelected) {
                                    selectedAppIds - app.id
                                } else {
                                    selectedAppIds + app.id
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else
                                    MaterialTheme.colorScheme.surface
                            ),
                            border = if (isSelected) CardDefaults.outlinedCardBorder(true) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Language, null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        app.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        app.url.ifBlank { app.appType.name },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        selectedAppIds = if (isSelected) selectedAppIds - app.id else selectedAppIds + app.id
                                    },
                                    modifier = Modifier.size(24.dp),
                                    colors = CheckboxDefaults.colors(checkedColor = accentColor)
                                )
                            }
                        }
                    }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (sourceType == "URL") {
                        onSave(
                            MultiWebSite(
                                id = UUID.randomUUID().toString(),
                                name = siteName.trim().ifBlank { guessSiteNameFromUrl(siteUrl) },
                                url = normalizeSiteUrl(siteUrl),
                                type = "URL",
                                cssSelector = cssSelector.trim(),
                                enabled = true,
                                sortIndex = newSortIndex
                            )
                        )
                        return@Button
                    }
                    if (sourceType == "LOCAL") {
                        onSave(
                            MultiWebSite(
                                id = UUID.randomUUID().toString(),
                                name = siteName.trim().ifBlank { localFileName.substringBeforeLast('.').ifBlank { Strings.multiWebTypeLocal } },
                                type = "LOCAL",
                                localFilePath = localFileName.ifBlank { "index.html" },
                                localFileUri = localFileUri,
                                cssSelector = cssSelector.trim(),
                                enabled = true,
                                sortIndex = newSortIndex
                            )
                        )
                        return@Button
                    }
                    val newSites = filteredApps.filter { it.id in selectedAppIds }.map { app ->
                        var localFilePath = ""
                        var sourceProjectId = ""
                        if (app.htmlConfig != null && app.htmlConfig!!.projectId.isNotBlank()) {
                            val entryFile = app.htmlConfig!!.files.firstOrNull { it.type == HtmlFileType.HTML }
                            localFilePath = entryFile?.name ?: "index.html"
                            sourceProjectId = app.htmlConfig!!.projectId
                        }
                        MultiWebSite(
                            id = UUID.randomUUID().toString(),
                            name = app.name,
                            url = app.url,
                            type = "EXISTING",
                            localFilePath = localFilePath,
                            sourceAppId = app.id,
                            sourceProjectId = sourceProjectId,
                            enabled = true,
                            sortIndex = newSortIndex + selectedAppIds.indexOf(app.id)
                        )
                    }
                    onBatchSave(newSites)
                },
                enabled = when (sourceType) {
                    "URL" -> siteUrl.isNotBlank()
                    "LOCAL" -> localFileName.isNotBlank()
                    else -> selectedAppIds.isNotEmpty()
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text(if (sourceType == "EXISTING" && selectedAppIds.size > 1) "${Strings.btnSave} (${selectedAppIds.size})" else Strings.btnSave)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings.cancel) }
        }
    )
}





@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSiteDialog(
    editingSite: MultiWebSite,
    showFeedFields: Boolean,
    existingApps: List<com.webtoapp.data.model.WebApp>,
    pendingLocalSiteUri: Uri? = null,
    onPickLocalSiteFile: () -> Unit = {},
    onConsumeLocalSiteFile: () -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (MultiWebSite) -> Unit
) {
    var sourceType by remember(editingSite.id) { mutableStateOf(editingSite.type.ifBlank { "URL" }) }
    var name by remember { mutableStateOf(editingSite.name) }
    var url by remember { mutableStateOf(editingSite.url) }
    var localFilePath by remember { mutableStateOf(editingSite.localFilePath) }
    var localFileUri by remember { mutableStateOf(editingSite.localFileUri) }
    var sourceAppId by remember { mutableStateOf(editingSite.sourceAppId) }
    var sourceProjectId by remember { mutableStateOf(editingSite.sourceProjectId) }
    var cssSelector by remember { mutableStateOf(editingSite.cssSelector) }
    var selectedAppId by remember { mutableStateOf(editingSite.sourceAppId) }
    LaunchedEffect(pendingLocalSiteUri) {
        pendingLocalSiteUri?.let { uri ->
            val pickedName = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "index.html"
            localFilePath = pickedName
            localFileUri = uri.toString()
            if (name.isBlank()) name = pickedName.substringBeforeLast('.')
            onConsumeLocalSiteFile()
        }
    }

    val accentColor = MaterialTheme.colorScheme.onSurface
    val eligibleApps = existingApps.filter { it.appType != com.webtoapp.data.model.AppType.MULTI_WEB }
    val isValid = when (sourceType) {
        "URL" -> url.isNotBlank()
        "LOCAL" -> localFilePath.isNotBlank()
        else -> selectedAppId > 0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Outlined.Edit, null, tint = accentColor, modifier = Modifier.size(32.dp))
        },
        title = {
            Text(Strings.multiWebEditSite, fontWeight = FontWeight.SemiBold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val types = listOf("URL", "LOCAL", "EXISTING")
                    val labels = listOf(Strings.multiWebTypeUrl, Strings.multiWebTypeLocal, Strings.multiWebTypeExisting)
                    types.forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = sourceType == type,
                            onClick = { sourceType = type },
                            shape = SegmentedButtonDefaults.itemShape(index, types.size)
                        ) {
                            Text(labels[index], maxLines = 1)
                        }
                    }
                }

                if (sourceType == "URL") {
                    PremiumTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(Strings.multiWebSiteName) },
                        leadingIcon = { Icon(Icons.Outlined.Label, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    PremiumTextField(
                        value = url,
                        onValueChange = {
                            url = it
                            if (name.isBlank()) name = guessSiteNameFromUrl(it)
                        },
                        label = { Text(Strings.multiWebSiteUrl) },
                        placeholder = { Text("https://example.com") },
                        leadingIcon = { Icon(Icons.Outlined.Link, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        )
                    )
                } else if (sourceType == "LOCAL") {
                    PremiumTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(Strings.multiWebSiteName) },
                        leadingIcon = { Icon(Icons.Outlined.Label, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = onPickLocalSiteFile,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.UploadFile, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(localFilePath.ifBlank { Strings.multiWebSelectFile })
                    }
                } else if (eligibleApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Apps, null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                Strings.multiWebNoApps,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    eligibleApps.forEach { app ->
                        val isSelected = selectedAppId == app.id
                        Card(
                            onClick = {
                                selectedAppId = if (isSelected) 0L else app.id
                                if (!isSelected) {
                                    name = app.name
                                    url = app.url
                                    sourceAppId = app.id
                                    sourceType = "EXISTING"
                                    if (app.htmlConfig != null && app.htmlConfig!!.projectId.isNotBlank()) {
                                        val entryFile = app.htmlConfig!!.files.firstOrNull { it.type == HtmlFileType.HTML }
                                        localFilePath = entryFile?.name ?: "index.html"
                                        sourceProjectId = app.htmlConfig!!.projectId
                                    }
                                } else {
                                    sourceAppId = 0L
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else
                                    MaterialTheme.colorScheme.surface
                            ),
                            border = if (isSelected) CardDefaults.outlinedCardBorder(true) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Language, null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        app.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        app.url.ifBlank { app.appType.name },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Outlined.CheckCircle, null,
                                        tint = accentColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }


                if (showFeedFields) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "Feed Extraction",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                    PremiumTextField(
                        value = cssSelector,
                        onValueChange = { cssSelector = it },
                        label = { Text(Strings.multiWebCssSelector) },
                        placeholder = { Text(Strings.multiWebCssSelectorHint) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        MultiWebSite(
                            id = editingSite.id,
                            name = name.trim().ifBlank {
                                if (sourceType == "URL") guessSiteNameFromUrl(url)
                                else localFilePath.substringBeforeLast('.').ifBlank { Strings.multiWebTypeLocal }
                            },
                            url = if (sourceType == "URL") normalizeSiteUrl(url) else if (sourceType == "EXISTING") url.trim() else "",
                            type = sourceType,
                            localFilePath = if (sourceType == "LOCAL" || sourceType == "EXISTING") localFilePath.trim() else "",
                            localFileUri = if (sourceType == "LOCAL") localFileUri else "",
                            sourceAppId = if (sourceType == "EXISTING") sourceAppId else 0L,
                            sourceProjectId = if (sourceType == "EXISTING") sourceProjectId else "",
                            cssSelector = cssSelector.trim(),
                            enabled = editingSite.enabled,
                            sortIndex = editingSite.sortIndex
                        )
                    )
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) { Text(Strings.btnSave) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings.cancel) }
        }
    )
}




private fun appTypeFilterInfo(typeName: String): Pair<androidx.compose.ui.graphics.vector.ImageVector, String> {
    return when (typeName) {
        "WEB" -> Icons.Outlined.Public to Strings.appTypeWeb
        "IMAGE" -> Icons.Outlined.Image to Strings.appTypeImage
        "VIDEO" -> Icons.Outlined.VideoLibrary to Strings.appTypeVideo
        "HTML" -> Icons.Outlined.Html to Strings.appTypeHtml
        "GALLERY" -> Icons.Outlined.PhotoLibrary to Strings.appTypeGallery
        "FRONTEND" -> Icons.Outlined.Rocket to Strings.appTypeFrontend
        "WORDPRESS" -> Icons.Outlined.Newspaper to Strings.appTypeWordPress
        "NODEJS_APP" -> Icons.Outlined.Terminal to Strings.appTypeNodeJs
        "PHP_APP" -> Icons.Outlined.DataObject to Strings.appTypePhp
        "PYTHON_APP" -> Icons.Outlined.Psychology to Strings.appTypePython
        "GO_APP" -> Icons.Outlined.Speed to Strings.appTypeGo
        "MULTI_WEB" -> Icons.Outlined.Language to Strings.appTypeMultiWeb
        else -> Icons.Outlined.Apps to typeName
    }
}

private fun normalizeSiteUrl(value: String): String {
    val trimmed = value.trim()
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
}

private fun guessSiteNameFromUrl(value: String): String {
    return runCatching {
        java.net.URL(normalizeSiteUrl(value)).host.removePrefix("www.").substringBeforeLast('.').replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
    }.getOrNull()?.ifBlank { Strings.multiWebTypeUrl } ?: Strings.multiWebTypeUrl
}
