package com.webtoapp.ui.screens

import android.net.Uri
import com.webtoapp.ui.components.PremiumButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.data.model.*
import com.webtoapp.ui.components.*
import com.webtoapp.util.MediaStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.components.EnhancedElevatedCard

/**
 * create/edit app
 * support / select, management, settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGalleryAppScreen(
    webAppRepository: com.webtoapp.data.repository.WebAppRepository,
    existingAppId: Long? = null,  // editmode appID
    onBack: () -> Unit,
    onCreated: (
        name: String,
        galleryConfig: GalleryConfig,
        iconUri: Uri?,
        themeType: String
    ) -> Unit
) {
    val context = LocalContext.current
    val isEditMode = existingAppId != null
    
    // editmodeload app
    var existingApp by remember { mutableStateOf<WebApp?>(null) }
    LaunchedEffect(existingAppId) {
        if (existingAppId != null) {
            existingApp = webAppRepository.getWebAppById(existingAppId).first()
        }
    }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    // App
    var appName by remember { mutableStateOf("") }
    var appIcon by remember { mutableStateOf<Uri?>(null) }
    var appIconPath by remember { mutableStateOf<String?>(null) }
    var themeType by remember { mutableStateOf("AURORA") }
    
    // Medialist
    var galleryItems by remember { mutableStateOf<List<GalleryItem>>(emptyList()) }
    var isLoadingMedia by remember { mutableStateOf(false) }
    
    // Note
    var categories by remember { mutableStateOf<List<GalleryCategory>>(emptyList()) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<GalleryCategory?>(null) }
    
    // Playsettings
    var playMode by remember { mutableStateOf(GalleryPlayMode.SEQUENTIAL) }
    var imageInterval by remember { mutableIntStateOf(3) }
    var loop by remember { mutableStateOf(true) }
    var autoPlay by remember { mutableStateOf(false) }
    var shuffleOnLoop by remember { mutableStateOf(false) }
    var videoAutoNext by remember { mutableStateOf(true) }
    var enableAudio by remember { mutableStateOf(true) }
    
    // Showsettings
    var defaultView by remember { mutableStateOf(GalleryViewMode.GRID) }
    var gridColumns by remember { mutableIntStateOf(3) }
    var sortOrder by remember { mutableStateOf(GallerySortOrder.CUSTOM) }
    var showThumbnailBar by remember { mutableStateOf(true) }
    var showMediaInfo by remember { mutableStateOf(true) }
    var orientation by remember { mutableStateOf(SplashOrientation.PORTRAIT) }
    var backgroundColor by remember { mutableStateOf("#000000") }
    var rememberPosition by remember { mutableStateOf(false) }
    
    // UI state
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedItems by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var showItemDetailDialog by remember { mutableStateOf<GalleryItem?>(null) }
    
    // editmode: load app UIstate
    LaunchedEffect(existingApp) {
        existingApp?.let { app ->
            // load
            appName = app.name
            appIconPath = app.iconPath
            themeType = app.themeType
            
            // load config
            app.galleryConfig?.let { config ->
                galleryItems = config.items
                categories = config.categories
                
                // settings
                playMode = config.playMode
                imageInterval = config.imageInterval
                loop = config.loop
                autoPlay = config.autoPlay
                shuffleOnLoop = config.shuffleOnLoop
                videoAutoNext = config.videoAutoNext
                enableAudio = config.enableAudio
                
                // displaysettings
                defaultView = config.defaultView
                gridColumns = config.gridColumns
                sortOrder = config.sortOrder
                showThumbnailBar = config.showThumbnailBar
                showMediaInfo = config.showMediaInfo
                orientation = config.orientation
                backgroundColor = config.backgroundColor
                rememberPosition = config.rememberPosition
            }
        }
    }
    
    // Fileselect
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            isLoadingMedia = true
            scope.launch {
                val items = uris.mapNotNull { uri ->
                    val type = MediaStorage.getMediaType(context, uri)
                    type?.let { uri to it }
                }
                if (items.isNotEmpty()) {
                    val newItems = MediaStorage.saveGalleryMediaBatch(context, items)
                    galleryItems = galleryItems + newItems.mapIndexed { index, item ->
                        item.copy(sortIndex = galleryItems.size + index)
                    }
                }
                isLoadingMedia = false
            }
        }
    }
    
    val iconPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { appIcon = it } }
    
    // create
    val canCreate = galleryItems.isNotEmpty()
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(AppStringsProvider.current().galleryCreateTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, AppStringsProvider.current().back)
                    }
                },
                actions = {
                    // Selectmode display button
                    if (isSelectionMode && selectedItems.isNotEmpty()) {
                        IconButton(onClick = {
                            galleryItems = galleryItems.filterNot { it.id in selectedItems }
                            selectedItems = emptySet()
                            isSelectionMode = false
                        }) {
                            Icon(Icons.Default.Delete, AppStringsProvider.current().delete)
                        }
                    }
                    
                    TextButton(
                        onClick = {
                            val finalIconUri = appIconPath?.let { Uri.parse("file://$it") } ?: appIcon
                            val config = GalleryConfig(
                                items = galleryItems,
                                categories = categories,
                                playMode = playMode,
                                imageInterval = imageInterval,
                                loop = loop,
                                autoPlay = autoPlay,
                                shuffleOnLoop = shuffleOnLoop,
                                defaultView = defaultView,
                                gridColumns = gridColumns,
                                sortOrder = sortOrder,
                                backgroundColor = backgroundColor,
                                showThumbnailBar = showThumbnailBar,
                                showMediaInfo = showMediaInfo,
                                orientation = orientation,
                                enableAudio = enableAudio,
                                videoAutoNext = videoAutoNext,
                                rememberPosition = rememberPosition
                            )
                            onCreated(
                                appName.ifBlank { AppStringsProvider.current().galleryApp },
                                config,
                                finalIconUri,
                                themeType
                            )
                        },
                        enabled = canCreate
                    ) {
                        Text(AppStringsProvider.current().btnCreate)
                    }
                }
            )
        }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // toplabel
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text(AppStringsProvider.current().galleryTabMedia) },
                    icon = { Icon(Icons.Outlined.PhotoLibrary, null) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text(AppStringsProvider.current().galleryTabPlayback) },
                    icon = { Icon(Icons.Outlined.PlayArrow, null) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text(AppStringsProvider.current().galleryTabDisplay) },
                    icon = { Icon(Icons.Outlined.Tune, null) }
                )
            }
            
            // contentarea
            when (selectedTabIndex) {
                0 -> MediaManagementTab(
                    items = galleryItems,
                    categories = categories,
                    isLoading = isLoadingMedia,
                    selectedItems = selectedItems,
                    isSelectionMode = isSelectionMode,
                    onAddMedia = {
                        mediaPickerLauncher.launch(arrayOf("image/*", "video/*"))
                    },
                    onItemClick = { item ->
                        if (isSelectionMode) {
                            selectedItems = if (item.id in selectedItems) {
                                selectedItems - item.id
                            } else {
                                selectedItems + item.id
                            }
                        } else {
                            showItemDetailDialog = item
                        }
                    },
                    onItemLongClick = { item ->
                        isSelectionMode = true
                        selectedItems = setOf(item.id)
                    },
                    onDeleteItem = { item ->
                        MediaStorage.deleteGalleryMedia(item)
                        galleryItems = galleryItems.filter { it.id != item.id }
                    },
                    onUpdateItem = { updated ->
                        galleryItems = galleryItems.map { if (it.id == updated.id) updated else it }
                    },
                    onReorderItems = { newItems ->
                        galleryItems = newItems.mapIndexed { index, item -> 
                            item.copy(sortIndex = index)
                        }
                    },
                    onAddCategory = {
                        editingCategory = null
                        showCategoryDialog = true
                    },
                    onEditCategory = { cat ->
                        editingCategory = cat
                        showCategoryDialog = true
                    },
                    onDeleteCategory = { cat ->
                        categories = categories.filter { it.id != cat.id }
                        // Remove reference
                        galleryItems = galleryItems.map { item ->
                            if (item.categoryId == cat.id) item.copy(categoryId = null) else item
                        }
                    },
                    appName = appName,
                    onAppNameChange = { appName = it },
                    appIcon = appIcon,
                    appIconPath = appIconPath,
                    onSelectIcon = { iconPickerLauncher.launch("image/*") },
                    onSelectIconFromLibrary = { path ->
                        appIconPath = path
                        appIcon = null
                    }
                )
                
                1 -> PlaybackSettingsTab(
                    playMode = playMode,
                    onPlayModeChange = { playMode = it },
                    imageInterval = imageInterval,
                    onImageIntervalChange = { imageInterval = it },
                    loop = loop,
                    onLoopChange = { loop = it },
                    autoPlay = autoPlay,
                    onAutoPlayChange = { autoPlay = it },
                    shuffleOnLoop = shuffleOnLoop,
                    onShuffleOnLoopChange = { shuffleOnLoop = it },
                    videoAutoNext = videoAutoNext,
                    onVideoAutoNextChange = { videoAutoNext = it },
                    enableAudio = enableAudio,
                    onEnableAudioChange = { enableAudio = it },
                    rememberPosition = rememberPosition,
                    onRememberPositionChange = { rememberPosition = it }
                )
                
                2 -> DisplaySettingsTab(
                    defaultView = defaultView,
                    onDefaultViewChange = { defaultView = it },
                    gridColumns = gridColumns,
                    onGridColumnsChange = { gridColumns = it },
                    sortOrder = sortOrder,
                    onSortOrderChange = { sortOrder = it },
                    showThumbnailBar = showThumbnailBar,
                    onShowThumbnailBarChange = { showThumbnailBar = it },
                    showMediaInfo = showMediaInfo,
                    onShowMediaInfoChange = { showMediaInfo = it },
                    orientation = orientation,
                    onOrientationChange = { orientation = it },
                    backgroundColor = backgroundColor,
                    onBackgroundColorChange = { backgroundColor = it }
                )
            }
        }
    }
    
    // Category edit dialog
    if (showCategoryDialog) {
        CategoryEditDialog(
            category = editingCategory,
            onDismiss = { showCategoryDialog = false },
            onSave = { newCategory ->
                if (editingCategory != null) {
                    categories = categories.map { 
                        if (it.id == newCategory.id) newCategory else it 
                    }
                } else {
                    categories = categories + newCategory.copy(sortIndex = categories.size)
                }
                showCategoryDialog = false
            }
        )
    }
    
    // Media details dialog
    showItemDetailDialog?.let { item ->
        MediaItemDetailDialog(
            item = item,
            categories = categories,
            onDismiss = { showItemDetailDialog = null },
            onUpdate = { updated ->
                galleryItems = galleryItems.map { if (it.id == updated.id) updated else it }
                showItemDetailDialog = null
            },
            onDelete = {
                MediaStorage.deleteGalleryMedia(item)
                galleryItems = galleryItems.filter { it.id != item.id }
                showItemDetailDialog = null
            }
        )
    }
        }
}

/**
 * managementlabel
 */
@Composable
private fun MediaManagementTab(
    items: List<GalleryItem>,
    categories: List<GalleryCategory>,
    isLoading: Boolean,
    selectedItems: Set<String>,
    isSelectionMode: Boolean,
    onAddMedia: () -> Unit,
    onItemClick: (GalleryItem) -> Unit,
    onItemLongClick: (GalleryItem) -> Unit,
    onDeleteItem: (GalleryItem) -> Unit,
    onUpdateItem: (GalleryItem) -> Unit,
    onReorderItems: (List<GalleryItem>) -> Unit,
    onAddCategory: () -> Unit,
    onEditCategory: (GalleryCategory) -> Unit,
    onDeleteCategory: (GalleryCategory) -> Unit,
    appName: String,
    onAppNameChange: (String) -> Unit,
    appIcon: Uri?,
    appIconPath: String?,
    onSelectIcon: () -> Unit,
    onSelectIconFromLibrary: (String) -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App card
        EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = AppStringsProvider.current().labelAppInfo,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                AppNameTextFieldSimple(
                    value = appName,
                    onValueChange = onAppNameChange,
                    placeholder = AppStringsProvider.current().galleryApp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                IconPickerWithLibrary(
                    iconUri = appIcon,
                    iconPath = appIconPath,
                    onSelectFromGallery = onSelectIcon,
                    onSelectFromLibrary = onSelectIconFromLibrary
                )
            }
        }
        
        // management
        if (categories.isNotEmpty() || items.isNotEmpty()) {
            EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppStringsProvider.current().galleryCategories,
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(onClick = onAddCategory) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStringsProvider.current().add)
                        }
                    }
                    
                    if (categories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categories) { category ->
                                CategoryChip(
                                    category = category,
                                    itemCount = items.count { it.categoryId == category.id },
                                    onClick = { onEditCategory(category) },
                                    onDelete = { onDeleteCategory(category) }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Medialistcard
        EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = AppStringsProvider.current().galleryMediaList,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${items.size} ${AppStringsProvider.current().galleryItemCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(onClick = onAddMedia) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStringsProvider.current().galleryAddMedia)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (items.isEmpty()) {
                    // Emptystate
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(onClick = onAddMedia),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = AppStringsProvider.current().galleryClickToAdd,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = AppStringsProvider.current().gallerySupportTypes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    // Media
                    MediaGrid(
                        items = items,
                        selectedItems = selectedItems,
                        isSelectionMode = isSelectionMode,
                        onItemClick = onItemClick,
                        onItemLongClick = onItemLongClick
                    )
                }
            }
        }
        
        // Note
        if (items.isNotEmpty()) {
            EnhancedElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        icon = Icons.Outlined.Image,
                        value = items.count { it.type == GalleryItemType.IMAGE }.toString(),
                        label = AppStringsProvider.current().galleryImages
                    )
                    StatItem(
                        icon = Icons.Outlined.Videocam,
                        value = items.count { it.type == GalleryItemType.VIDEO }.toString(),
                        label = AppStringsProvider.current().galleryVideos
                    )
                    StatItem(
                        icon = Icons.Outlined.Storage,
                        value = formatFileSize(items.sumOf { it.fileSize }),
                        label = AppStringsProvider.current().galleryTotalSize
                    )
                }
            }
        }
    }
}

/**
 * Note
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaGrid(
    items: List<GalleryItem>,
    selectedItems: Set<String>,
    isSelectionMode: Boolean,
    onItemClick: (GalleryItem) -> Unit,
    onItemLongClick: (GalleryItem) -> Unit
) {
    val context = LocalContext.current
    
    // , scroll
    val gridHeight = ((items.size + 2) / 3) * 120
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .height(gridHeight.coerceAtMost(360).dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false
    ) {
        items(items, key = { it.id }) { item ->
            val isSelected = item.id in selectedItems
            
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = if (isSelected) 3.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = MaterialTheme.shapes.small
                    )
                    .combinedClickable(
                        onClick = { onItemClick(item) },
                        onLongClick = { onItemLongClick(item) }
                    )
            ) {
                // Note
                val imagePath = item.thumbnailPath ?: item.path
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(imagePath))
                        .crossfade(true)
                        .build(),
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Video
                if (item.type == GalleryItemType.VIDEO) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp)
                            .background(
                                Color.Black.copy(alpha = 0.6f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color.White
                            )
                            if (item.duration > 0) {
                                Text(
                                    text = item.formattedDuration,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
                
                // Note
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(24.dp)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary 
                                else Color.White.copy(alpha = 0.8f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * settingslabel
 */
@Composable
private fun PlaybackSettingsTab(
    playMode: GalleryPlayMode,
    onPlayModeChange: (GalleryPlayMode) -> Unit,
    imageInterval: Int,
    onImageIntervalChange: (Int) -> Unit,
    loop: Boolean,
    onLoopChange: (Boolean) -> Unit,
    autoPlay: Boolean,
    onAutoPlayChange: (Boolean) -> Unit,
    shuffleOnLoop: Boolean,
    onShuffleOnLoopChange: (Boolean) -> Unit,
    videoAutoNext: Boolean,
    onVideoAutoNextChange: (Boolean) -> Unit,
    enableAudio: Boolean,
    onEnableAudioChange: (Boolean) -> Unit,
    rememberPosition: Boolean,
    onRememberPositionChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Playmode
        EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = AppStringsProvider.current().galleryPlayMode,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlayModeOption(
                        icon = Icons.AutoMirrored.Outlined.ArrowForward,
                        label = AppStringsProvider.current().galleryModeSequential,
                        selected = playMode == GalleryPlayMode.SEQUENTIAL,
                        onClick = { onPlayModeChange(GalleryPlayMode.SEQUENTIAL) },
                        modifier = Modifier.weight(weight = 1f, fill = true)
                    )
                    PlayModeOption(
                        icon = Icons.Outlined.Shuffle,
                        label = AppStringsProvider.current().galleryModeShuffle,
                        selected = playMode == GalleryPlayMode.SHUFFLE,
                        onClick = { onPlayModeChange(GalleryPlayMode.SHUFFLE) },
                        modifier = Modifier.weight(weight = 1f, fill = true)
                    )
                    PlayModeOption(
                        icon = Icons.Outlined.Repeat,
                        label = AppStringsProvider.current().galleryModeSingleLoop,
                        selected = playMode == GalleryPlayMode.SINGLE_LOOP,
                        onClick = { onPlayModeChange(GalleryPlayMode.SINGLE_LOOP) },
                        modifier = Modifier.weight(weight = 1f, fill = true)
                    )
                }
            }
        }
        
        // Image settings
        EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = AppStringsProvider.current().galleryImageSettings,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Image
                Text(
                    text = "${AppStringsProvider.current().galleryImageInterval}: ${imageInterval}s",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = imageInterval.toFloat(),
                    onValueChange = { onImageIntervalChange(it.toInt()) },
                    valueRange = 1f..30f,
                    steps = 28,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Video settings
        EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = AppStringsProvider.current().galleryVideoSettings,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                SettingsRow(
                    title = AppStringsProvider.current().enableAudio,
                    subtitle = AppStringsProvider.current().galleryEnableAudioHint
                ) {
                    PremiumSwitch(
                        checked = enableAudio,
                        onCheckedChange = onEnableAudioChange
                    )
                }
                
                SettingsRow(
                    title = AppStringsProvider.current().galleryVideoAutoNext,
                    subtitle = AppStringsProvider.current().galleryVideoAutoNextHint
                ) {
                    PremiumSwitch(
                        checked = videoAutoNext,
                        onCheckedChange = onVideoAutoNextChange
                    )
                }
            }
        }
        
        // settings
        EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = AppStringsProvider.current().galleryGeneralSettings,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                SettingsRow(
                    title = AppStringsProvider.current().galleryAutoPlay,
                    subtitle = AppStringsProvider.current().galleryAutoPlayHint
                ) {
                    PremiumSwitch(
                        checked = autoPlay,
                        onCheckedChange = onAutoPlayChange
                    )
                }
                
                SettingsRow(
                    title = AppStringsProvider.current().loopPlay,
                    subtitle = AppStringsProvider.current().galleryLoopHint
                ) {
                    PremiumSwitch(
                        checked = loop,
                        onCheckedChange = onLoopChange
                    )
                }
                
                AnimatedVisibility(visible = loop) {
                    SettingsRow(
                        title = AppStringsProvider.current().galleryShuffleOnLoop,
                        subtitle = AppStringsProvider.current().galleryShuffleOnLoopHint
                    ) {
                        PremiumSwitch(
                            checked = shuffleOnLoop,
                            onCheckedChange = onShuffleOnLoopChange
                        )
                    }
                }
                
                SettingsRow(
                    title = AppStringsProvider.current().galleryRememberPosition,
                    subtitle = AppStringsProvider.current().galleryRememberPositionHint
                ) {
                    PremiumSwitch(
                        checked = rememberPosition,
                        onCheckedChange = onRememberPositionChange
                    )
                }
            }
        }
    }
}

/**
 * displaysettingslabel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisplaySettingsTab(
    defaultView: GalleryViewMode,
    onDefaultViewChange: (GalleryViewMode) -> Unit,
    gridColumns: Int,
    onGridColumnsChange: (Int) -> Unit,
    sortOrder: GallerySortOrder,
    onSortOrderChange: (GallerySortOrder) -> Unit,
    showThumbnailBar: Boolean,
    onShowThumbnailBarChange: (Boolean) -> Unit,
    showMediaInfo: Boolean,
    onShowMediaInfoChange: (Boolean) -> Unit,
    orientation: SplashOrientation,
    onOrientationChange: (SplashOrientation) -> Unit,
    backgroundColor: String,
    onBackgroundColorChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // mode
        EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = AppStringsProvider.current().galleryViewMode,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ViewModeOption(
                        icon = Icons.Outlined.GridView,
                        label = AppStringsProvider.current().galleryViewGrid,
                        selected = defaultView == GalleryViewMode.GRID,
                        onClick = { onDefaultViewChange(GalleryViewMode.GRID) },
                        modifier = Modifier.weight(weight = 1f, fill = true)
                    )
                    ViewModeOption(
                        icon = Icons.AutoMirrored.Outlined.ViewList,
                        label = AppStringsProvider.current().galleryViewList,
                        selected = defaultView == GalleryViewMode.LIST,
                        onClick = { onDefaultViewChange(GalleryViewMode.LIST) },
                        modifier = Modifier.weight(weight = 1f, fill = true)
                    )
                    ViewModeOption(
                        icon = Icons.Outlined.Timeline,
                        label = AppStringsProvider.current().galleryViewTimeline,
                        selected = defaultView == GalleryViewMode.TIMELINE,
                        onClick = { onDefaultViewChange(GalleryViewMode.TIMELINE) },
                        modifier = Modifier.weight(weight = 1f, fill = true)
                    )
                }
                
                // Note
                AnimatedVisibility(visible = defaultView == GalleryViewMode.GRID) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "${AppStringsProvider.current().galleryGridColumns}: $gridColumns",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = gridColumns.toFloat(),
                            onValueChange = { onGridColumnsChange(it.toInt()) },
                            valueRange = 2f..5f,
                            steps = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        
        // Sort
        EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = AppStringsProvider.current().gallerySortOrder,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                var expanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    PremiumTextField(
                        value = sortOrder.toDisplayString(),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        GallerySortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = { Text(order.toDisplayString()) },
                                onClick = {
                                    onSortOrderChange(order)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Play settings
        EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = AppStringsProvider.current().galleryPlayerSettings,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                SettingsRow(
                    title = AppStringsProvider.current().galleryShowThumbnailBar,
                    subtitle = AppStringsProvider.current().galleryShowThumbnailBarHint
                ) {
                    PremiumSwitch(
                        checked = showThumbnailBar,
                        onCheckedChange = onShowThumbnailBarChange
                    )
                }
                
                SettingsRow(
                    title = AppStringsProvider.current().galleryShowMediaInfo,
                    subtitle = AppStringsProvider.current().galleryShowMediaInfoHint
                ) {
                    PremiumSwitch(
                        checked = showMediaInfo,
                        onCheckedChange = onShowMediaInfoChange
                    )
                }
                
                SettingsRow(
                    title = AppStringsProvider.current().landscapeMode,
                    subtitle = AppStringsProvider.current().landscapeModeHint
                ) {
                    PremiumSwitch(
                        checked = orientation == SplashOrientation.LANDSCAPE,
                        onCheckedChange = {
                            onOrientationChange(
                                if (it) SplashOrientation.LANDSCAPE else SplashOrientation.PORTRAIT
                            )
                        }
                    )
                }
            }
        }
        
        // color
        EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = AppStringsProvider.current().galleryBackgroundColor,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("#000000", "#1A1A1A", "#2D2D2D", "#FFFFFF").forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(color)))
                                .border(
                                    width = if (backgroundColor == color) 3.dp else 1.dp,
                                    color = if (backgroundColor == color) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable { onBackgroundColorChange(color) }
                        )
                    }
                }
            }
        }
    }
}

// Note

@Composable
private fun PlayModeOption(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (selected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (selected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ViewModeOption(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlayModeOption(icon, label, selected, onClick, modifier)
}

@Composable
private fun CategoryChip(
    category: GalleryCategory,
    itemCount: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color(android.graphics.Color.parseColor(category.color)).copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(com.webtoapp.util.SvgIconMapper.getIcon(category.icon), contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${category.name} ($itemCount)",
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.Close,
                contentDescription = AppStringsProvider.current().delete,
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = onDelete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
    }
}

/**
 * editdialog
 */
@Composable
private fun CategoryEditDialog(
    category: GalleryCategory?,
    onDismiss: () -> Unit,
    onSave: (GalleryCategory) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var icon by remember { mutableStateOf(category?.icon ?: "folder") }
    var color by remember { mutableStateOf(category?.color ?: "#6200EE") }
    
    val availableIcons = listOf("folder", "movie", "camera", "music_note", "heart", "star", "palette", "target", "home", "auto_awesome")
    val availableColors = listOf("#6200EE", "#03DAC6", "#FF5722", "#4CAF50", "#2196F3", "#E91E63")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category != null) AppStringsProvider.current().galleryEditCategory else AppStringsProvider.current().galleryAddCategory) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                PremiumTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(AppStringsProvider.current().galleryCategoryName) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(AppStringsProvider.current().galleryCategoryIcon, style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableIcons) { iconId ->
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { icon = iconId },
                            shape = CircleShape,
                            color = if (icon == iconId)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(com.webtoapp.util.SvgIconMapper.getIcon(iconId), contentDescription = null, modifier = Modifier.size(20.dp), tint = if (icon == iconId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(AppStringsProvider.current().galleryCategoryColor, style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableColors) { colorStr ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(colorStr)))
                                .border(
                                    width = if (color == colorStr) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                                .clickable { color = colorStr }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newCategory = category?.copy(name = name, icon = icon, color = color)
                        ?: GalleryCategory(name = name, icon = icon, color = color)
                    onSave(newCategory)
                },
                enabled = name.isNotBlank()
            ) {
                Text(AppStringsProvider.current().save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStringsProvider.current().cancel)
            }
        }
    )
}

/**
 * dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaItemDetailDialog(
    item: GalleryItem,
    categories: List<GalleryCategory>,
    onDismiss: () -> Unit,
    onUpdate: (GalleryItem) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(item.name) }
    var categoryId by remember { mutableStateOf(item.categoryId) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStringsProvider.current().galleryMediaDetail) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MaterialTheme.shapes.medium)
                ) {
                    val imagePath = item.thumbnailPath ?: item.path
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(imagePath))
                            .crossfade(true)
                            .build(),
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    
                    if (item.type == GalleryItemType.VIDEO) {
                        Icon(
                            Icons.Default.PlayCircle,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .align(Alignment.Center),
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Name
                PremiumTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(AppStringsProvider.current().name) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // select
                if (categories.isNotEmpty()) {
                    Text(AppStringsProvider.current().galleryCategory, style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        PremiumTextField(
                            value = categories.find { it.id == categoryId }?.name ?: AppStringsProvider.current().galleryNoCategory,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(AppStringsProvider.current().galleryNoCategory) },
                                onClick = {
                                    categoryId = null
                                    expanded = false
                                }
                            )
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        categoryId = cat.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Info
                EnhancedElevatedCard(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "${AppStringsProvider.current().galleryType}: ${if (item.type == GalleryItemType.IMAGE) AppStringsProvider.current().image else AppStringsProvider.current().video}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (item.duration > 0) {
                            Text(
                                text = "${AppStringsProvider.current().galleryDuration}: ${item.formattedDuration}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = "${AppStringsProvider.current().gallerySize}: ${item.formattedFileSize}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (item.width > 0 && item.height > 0) {
                            Text(
                                text = "${AppStringsProvider.current().galleryDimensions}: ${item.width} × ${item.height}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onUpdate(item.copy(name = name, categoryId = categoryId))
                }
            ) {
                Text(AppStringsProvider.current().save)
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(AppStringsProvider.current().delete)
                }
                TextButton(onClick = onDismiss) {
                    Text(AppStringsProvider.current().cancel)
                }
            }
        }
    )
}

// Note

private fun GallerySortOrder.toDisplayString(): String {
    return when (this) {
        GallerySortOrder.CUSTOM -> AppStringsProvider.current().gallerySortCustom
        GallerySortOrder.NAME_ASC -> AppStringsProvider.current().gallerySortNameAsc
        GallerySortOrder.NAME_DESC -> AppStringsProvider.current().gallerySortNameDesc
        GallerySortOrder.DATE_ASC -> AppStringsProvider.current().gallerySortDateAsc
        GallerySortOrder.DATE_DESC -> AppStringsProvider.current().gallerySortDateDesc
        GallerySortOrder.TYPE -> AppStringsProvider.current().gallerySortType
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(java.util.Locale.getDefault(), "%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format(java.util.Locale.getDefault(), "%.1f MB", bytes / 1024.0 / 1024.0)
        else -> String.format(java.util.Locale.getDefault(), "%.2f GB", bytes / 1024.0 / 1024.0 / 1024.0)
    }
}

// ==================== V2version: ====================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CreateGalleryAppScreenV2(
    webAppRepository: com.webtoapp.data.repository.WebAppRepository,
    existingAppId: Long? = null,
    onBack: () -> Unit,
    onCreated: (
        name: String,
        galleryConfig: GalleryConfig,
        iconUri: Uri?,
        themeType: String
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    // from load app( editmode)
    var existingApp by remember { mutableStateOf<WebApp?>(null) }
    LaunchedEffect(existingAppId) {
        if (existingAppId != null) {
            existingApp = webAppRepository.getWebAppById(existingAppId).first()
        }
    }

    // state
    var appName by remember { mutableStateOf("") }
    var appIcon by remember { mutableStateOf<Uri?>(null) }
    var appIconPath by remember { mutableStateOf<String?>(null) }

    var galleryItems by remember { mutableStateOf<List<GalleryItem>>(emptyList()) }
    var isLoadingMedia by remember { mutableStateOf(false) }

    var selectedItems by remember { mutableStateOf<Set<String>>(emptySet()) }
    val isSelectionMode = selectedItems.isNotEmpty()

    // settings
    var loop by remember { mutableStateOf(true) }
    var autoPlay by remember { mutableStateOf(false) }
    var imageInterval by remember { mutableIntStateOf(3) }
    var enableAudio by remember { mutableStateOf(true) }
    var videoAutoNext by remember { mutableStateOf(true) }

    // UI
    var showAppInfoSection by remember { mutableStateOf(true) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var previewItem by remember { mutableStateOf<GalleryItem?>(null) }

    // from app
    LaunchedEffect(existingApp) {
        existingApp?.let { app ->
            appName = app.name
            app.iconPath?.let { appIconPath = it }
            app.galleryConfig?.let { config ->
                galleryItems = config.items
                loop = config.loop
                autoPlay = config.autoPlay
                imageInterval = config.imageInterval
                enableAudio = config.enableAudio
                videoAutoNext = config.videoAutoNext
            }
        }
    }

    // Select
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            isLoadingMedia = true
            scope.launch {
                val pairs = uris.mapNotNull { uri ->
                    val type = MediaStorage.getMediaType(context, uri)
                    type?.let { uri to it }
                }
                if (pairs.isNotEmpty()) {
                    val newItems = MediaStorage.saveGalleryMediaBatch(context, pairs)
                    galleryItems = galleryItems + newItems.mapIndexed { index, item ->
                        item.copy(sortIndex = galleryItems.size + index)
                    }
                }
            }
            isLoadingMedia = false
        }
    }
    val iconPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { appIcon = it } }

    val canCreate = galleryItems.isNotEmpty()

    // top( selectmode/ mode)
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AnimatedContent(
                targetState = isSelectionMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "topbar"
            ) { selecting ->
                if (selecting) {
                    TopAppBar(
                        title = { Text("${selectedItems.size} ${AppStringsProvider.current().galleryItemCount}") },
                        navigationIcon = {
                            IconButton(onClick = { selectedItems = emptySet() }) {
                                Icon(Icons.Default.Close, AppStringsProvider.current().btnCancel)
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                selectedItems = if (selectedItems.size == galleryItems.size) emptySet() else galleryItems.map { it.id }.toSet()
                            }) { Icon(Icons.Default.CheckBox, null) }
                            IconButton(onClick = {
                                selectedItems.forEach { id ->
                                    galleryItems.find { it.id == id }?.let { MediaStorage.deleteGalleryMedia(it) }
                                }
                                galleryItems = galleryItems.filterNot { it.id in selectedItems }
                                selectedItems = emptySet()
                            }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    )
                } else {
                    TopAppBar(
                        title = { Text(if (existingAppId != null) AppStringsProvider.current().editApp else AppStringsProvider.current().galleryCreateTitle) },
                        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, AppStringsProvider.current().back) } },
                        actions = {
                            IconButton(onClick = { showSettingsSheet = true }) { Icon(Icons.Outlined.Settings, null) }
                            TextButton(
                                onClick = {
                                    val finalIconUri = appIconPath?.let { Uri.parse("file://$it") } ?: appIcon
                                    val config = GalleryConfig(
                                        items = galleryItems,
                                        categories = emptyList(),
                                        playMode = GalleryPlayMode.SEQUENTIAL,
                                        imageInterval = imageInterval,
                                        loop = loop,
                                        autoPlay = autoPlay,
                                        shuffleOnLoop = false,
                                        defaultView = GalleryViewMode.GRID,
                                        gridColumns = 3,
                                        sortOrder = GallerySortOrder.CUSTOM,
                                        backgroundColor = "#000000",
                                        showThumbnailBar = true,
                                        showMediaInfo = true,
                                        orientation = SplashOrientation.PORTRAIT,
                                        enableAudio = enableAudio,
                                        videoAutoNext = videoAutoNext,
                                        rememberPosition = false
                                    )
                                    onCreated(appName.ifBlank { AppStringsProvider.current().galleryApp }, config, finalIconUri, "AURORA")
                                },
                                enabled = canCreate
                            ) { Text(if (existingAppId != null) AppStringsProvider.current().btnSave else AppStringsProvider.current().btnCreate) }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                ExtendedFloatingActionButton(
                    onClick = { mediaPickerLauncher.launch(arrayOf("image/*", "video/*")) },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text(AppStringsProvider.current().galleryAddMedia) }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Collapsible app info
            AnimatedVisibility(visible = showAppInfoSection, enter = expandVertically(), exit = shrinkVertically()) {
                Surface(color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer).clickable { iconPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            val iconToShow = appIconPath ?: appIcon?.toString()
                            if (iconToShow != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(if (appIconPath != null) File(appIconPath!!) else appIcon).crossfade(true).build(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Outlined.PhotoLibrary, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        PremiumTextField(
                            value = appName,
                            onValueChange = { appName = it },
                            placeholder = { Text(AppStringsProvider.current().galleryApp) },
                            modifier = Modifier.weight(weight = 1f, fill = true),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
            Surface(modifier = Modifier.fillMaxWidth().clickable { showAppInfoSection = !showAppInfoSection }, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
                Icon(if (showAppInfoSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, modifier = Modifier.fillMaxWidth().padding(4.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Stats bar
            if (galleryItems.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatChip(icon = Icons.Outlined.Image, value = galleryItems.count { it.type == GalleryItemType.IMAGE })
                        StatChip(icon = Icons.Outlined.Videocam, value = galleryItems.count { it.type == GalleryItemType.VIDEO })
                    }
                    Text(text = formatFileSize(galleryItems.sumOf { it.fileSize }), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Media grid
            Box(modifier = Modifier.weight(weight = 1f, fill = true)) {
                if (isLoadingMedia) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (galleryItems.isEmpty()) {
                    EmptyGalleryState(onAddMedia = { mediaPickerLauncher.launch(arrayOf("image/*", "video/*")) })
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(galleryItems, key = { it.id }) { item ->
                            GalleryMediaItem(
                                item = item,
                                isSelected = item.id in selectedItems,
                                isSelectionMode = isSelectionMode,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedItems = if (item.id in selectedItems) selectedItems - item.id else selectedItems + item.id
                                    } else {
                                        previewItem = item
                                    }
                                },
                                onLongClick = { selectedItems = setOf(item.id) },
                                modifier = Modifier.animateItem()
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    // Set BottomSheet
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            GallerySettingsSheet(
                loop = loop, onLoopChange = { loop = it },
                autoPlay = autoPlay, onAutoPlayChange = { autoPlay = it },
                imageInterval = imageInterval, onImageIntervalChange = { imageInterval = it },
                enableAudio = enableAudio, onEnableAudioChange = { enableAudio = it },
                videoAutoNext = videoAutoNext, onVideoAutoNextChange = { videoAutoNext = it },
                onDismiss = { showSettingsSheet = false }
            )
        }
    }

    // Preview dialog
    previewItem?.let { item ->
        MediaPreviewDialog(
            item = item,
            onDismiss = { previewItem = null },
            onDelete = {
                MediaStorage.deleteGalleryMedia(item)
                galleryItems = galleryItems.filter { it.id != item.id }
                previewItem = null
            }
        )
    }
}

// ============ V2 helper components ============
@Composable
private fun StatChip(icon: ImageVector, value: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text = value.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun EmptyGalleryState(onAddMedia: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().clickable(onClick = onAddMedia).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.AddPhotoAlternate, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = AppStringsProvider.current().galleryClickToAdd, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = AppStringsProvider.current().gallerySupportTypes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryMediaItem(
    item: GalleryItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(File(item.thumbnailPath ?: item.path)).crossfade(true).build(),
            contentDescription = item.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (item.type == GalleryItemType.VIDEO) {
            Box(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(32.dp).background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))
                )
            )
            if (item.duration > 0) {
                Text(item.formattedDuration, modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp), style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
            Icon(Icons.Default.PlayCircle, null, modifier = Modifier.align(Alignment.Center).size(36.dp), tint = Color.White.copy(alpha = 0.9f))
        }
        if (isSelectionMode) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f))
                    .border(width = 2.dp, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = Color.White) }
            }
        }
        if (isSelected) {
            Box(modifier = Modifier.fillMaxSize().border(width = 3.dp, color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp)))
        }
    }
}

@Composable
private fun GallerySettingsSheet(
    loop: Boolean, onLoopChange: (Boolean) -> Unit,
    autoPlay: Boolean, onAutoPlayChange: (Boolean) -> Unit,
    imageInterval: Int, onImageIntervalChange: (Int) -> Unit,
    enableAudio: Boolean, onEnableAudioChange: (Boolean) -> Unit,
    videoAutoNext: Boolean, onVideoAutoNextChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsSection(title = AppStringsProvider.current().galleryGeneralSettings) {
            SettingsSwitch(title = AppStringsProvider.current().loopPlay, subtitle = AppStringsProvider.current().galleryLoopHint, checked = loop, onCheckedChange = onLoopChange)
            SettingsSwitch(title = AppStringsProvider.current().galleryAutoPlay, subtitle = AppStringsProvider.current().galleryAutoPlayHint, checked = autoPlay, onCheckedChange = onAutoPlayChange)
        }
        SettingsSection(title = AppStringsProvider.current().galleryImageSettings) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(AppStringsProvider.current().galleryImageInterval, style = MaterialTheme.typography.bodyMedium)
                    Text("${imageInterval}s", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Slider(value = imageInterval.toFloat(), onValueChange = { onImageIntervalChange(it.toInt()) }, valueRange = 1f..15f, steps = 13, modifier = Modifier.fillMaxWidth())
            }
        }
        SettingsSection(title = AppStringsProvider.current().galleryVideoSettings) {
            SettingsSwitch(title = AppStringsProvider.current().enableAudio, subtitle = AppStringsProvider.current().galleryEnableAudioHint, checked = enableAudio, onCheckedChange = onEnableAudioChange)
            SettingsSwitch(title = AppStringsProvider.current().galleryVideoAutoNext, subtitle = AppStringsProvider.current().galleryVideoAutoNextHint, checked = videoAutoNext, onCheckedChange = onVideoAutoNextChange)
        }
        Spacer(modifier = Modifier.height(16.dp))
        PremiumButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(AppStringsProvider.current().btnConfirm) }
    }
}



@Composable
private fun MediaPreviewDialog(item: GalleryItem, onDismiss: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(File(item.thumbnailPath ?: item.path)).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    if (item.type == GalleryItemType.VIDEO) {
                        Icon(Icons.Default.PlayCircle, null, modifier = Modifier.size(64.dp).align(Alignment.Center), tint = Color.White.copy(alpha = 0.9f))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = item.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    InfoChip(label = if (item.type == GalleryItemType.IMAGE) AppStringsProvider.current().image else AppStringsProvider.current().video, icon = if (item.type == GalleryItemType.IMAGE) Icons.Outlined.Image else Icons.Outlined.Videocam)
                    if (item.duration > 0) { InfoChip(label = item.formattedDuration, icon = Icons.Outlined.Timer) }
                    InfoChip(label = item.formattedFileSize, icon = Icons.Outlined.Storage)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(AppStringsProvider.current().btnOk) } },
        dismissButton = { TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(AppStringsProvider.current().delete) } }
    )
}
