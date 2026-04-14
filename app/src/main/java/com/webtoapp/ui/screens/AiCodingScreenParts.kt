package com.webtoapp.ui.screens

import androidx.compose.animation.*
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumOutlinedButton
import com.webtoapp.ui.components.PremiumFilterChip
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.ai.coding.*
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.coding.* // Note

@Composable
internal fun WelcomeContent(
    onNewChat: () -> Unit,
    onSelectTemplate: () -> Unit,
    onOpenTutorial: () -> Unit,
    quickPrompts: List<String>,
    onQuickPrompt: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // Logoarea
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                Icons.Filled.Code,
                contentDescription = null,
                modifier = Modifier.padding(18.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            Strings.htmlCodingAssistant,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            Strings.aiHelpsGenerateWebpage,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // button
        PremiumButton(
            onClick = onNewChat,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(Strings.startNewConversation, style = MaterialTheme.typography.titleMedium)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PremiumOutlinedButton(
                onClick = onSelectTemplate,
                modifier = Modifier
                    .weight(weight = 1f, fill = true)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.Palette, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(Strings.templates)
            }
            
            PremiumOutlinedButton(
                onClick = onOpenTutorial,
                modifier = Modifier
                    .weight(weight = 1f, fill = true)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.School, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(Strings.tutorial)
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // hint
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                Strings.quickStart,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // hint card
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            quickPrompts.take(4).chunked(2).forEach { rowPrompts ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowPrompts.forEach { prompt ->
                        Surface(
                            modifier = Modifier
                                .weight(weight = 1f, fill = true)
                                .clickable { onQuickPrompt(prompt) },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = prompt,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    // if,
                    if (rowPrompts.size == 1) {
                        Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun LoadingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            Strings.aiThinking,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
internal fun ImageGeneratingIndicator(prompt: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.tertiary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                Strings.generatingImage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
            Text(
                prompt.take(50) + if (prompt.length > 50) "..." else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
internal fun SessionDrawerContent(
    sessions: List<AiCodingSession>,
    currentSessionId: String?,
    onSessionClick: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onNewSession: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxHeight()) {
        // header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(Strings.conversationHistory, style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, Strings.close)
            }
        }
        
        // newbutton
        FilledTonalButton(
            onClick = onNewSession,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(Strings.newConversation)
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        
        // Sessionlist
        LazyColumn(modifier = Modifier.weight(weight = 1f, fill = true)) {
            items(sessions, key = { it.id }) { session ->
                SessionListItem(
                    session = session,
                    isSelected = session.id == currentSessionId,
                    onClick = { onSessionClick(session.id) },
                    onDelete = { onDeleteSession(session.id) }
                )
            }
            
            if (sessions.isEmpty()) {
                item {
                    Text(
                        Strings.noConversationRecords,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
internal fun TemplatesSheetContent(
    templates: List<StyleTemplate>,
    styles: List<StyleReference>,
    selectedTemplateId: String?,
    selectedStyleId: String?,
    onTemplateSelect: (String?) -> Unit,
    onStyleSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Note
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                Strings.selectStyleTemplate, 
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            // current hint
            if (selectedTemplateId != null || selectedStyleId != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = Strings.selected,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            Strings.selectTemplateHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // label
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(Strings.designTemplates) },
                icon = { Icon(Icons.Outlined.Palette, null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(Strings.styleReferences) },
                icon = { Icon(Icons.Outlined.Style, null, modifier = Modifier.size(18.dp)) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when (selectedTab) {
            0 -> {
                // LazyVerticalGrid
                Text(
                    Strings.totalTemplates.replace("%d", "${templates.size}"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(templates) { template ->
                        StyleTemplateCard(
                            template = template,
                            isSelected = template.id == selectedTemplateId,
                            onClick = {
                                onTemplateSelect(
                                    if (template.id == selectedTemplateId) null else template.id
                                )
                            }
                        )
                    }
                }
            }
            1 -> {
                // list
                Text(
                    Strings.totalStyleReferences.replace("%d", "${styles.size}"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(styles) { style ->
                        StyleReferenceCard(
                            style = style,
                            isSelected = style.id == selectedStyleId,
                            onClick = {
                                onStyleSelect(
                                    if (style.id == selectedStyleId) null else style.id
                                )
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
internal fun TutorialSheetContent(
    chapters: List<AiCodingTutorial.TutorialChapter>,
    onDismiss: () -> Unit
) {
    var selectedChapterId by remember { mutableStateOf<String?>(null) }
    var selectedSectionIndex by remember { mutableIntStateOf(0) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Note
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                Strings.usageTutorial, 
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                Strings.chapters.replace("%d", "${chapters.size}"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (selectedChapterId == null) {
            // list
            if (chapters.isEmpty()) {
                // Emptystatehint
                Box(
                    modifier = Modifier.fillMaxWidth().weight(weight = 1f, fill = true),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        Strings.noTutorialContent,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    items(chapters) { chapter ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedChapterId = chapter.id
                                    selectedSectionIndex = 0
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Icon(
                                        Icons.Outlined.MenuBook, 
                                        null,
                                        modifier = Modifier.padding(8.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                                    Text(
                                        chapter.title,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        Strings.sections.replace("%d", "${chapter.sections.size}"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    null,
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // content
            val chapter = chapters.find { it.id == selectedChapterId }
            chapter?.let {
                // backbutton
                Surface(
                    modifier = Modifier
                        .clickable { selectedChapterId = null },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            it.title, 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // label
                ScrollableTabRow(
                    selectedTabIndex = selectedSectionIndex,
                    edgePadding = 0.dp
                ) {
                    it.sections.forEachIndexed { index, section ->
                        Tab(
                            selected = index == selectedSectionIndex,
                            onClick = { selectedSectionIndex = index },
                            text = { Text(section.title, maxLines = 1) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // content
                val section = it.sections.getOrNull(selectedSectionIndex)
                section?.let { sec ->
                    Column(
                        modifier = Modifier
                            .weight(weight = 1f, fill = true)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // contenttext
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp
                        ) {
                            Text(
                                sec.content,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp),
                                lineHeight = 24.sp
                            )
                        }
                        
                        // code
                        sec.codeExample?.let { code ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                Strings.codeExample,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E1E1E)
                            ) {
                                Text(
                                    code,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD4D4D4),
                                    modifier = Modifier.padding(16.dp),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        
                        // hint
                        if (sec.tips.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.Lightbulb,
                                            null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            Strings.tips,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    sec.tips.forEach { tip ->
                                        Text(
                                            "• $tip",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun CheckpointsSheetContent(
    checkpoints: List<ProjectCheckpoint>,
    currentIndex: Int,
    onRestore: (String) -> Unit,
    onDelete: (String) -> Unit,
    onCreateCheckpoint: () -> Unit,
    onSaveProject: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(Strings.versionManagement, style = MaterialTheme.typography.titleLarge)
            Row {
                FilledTonalButton(onClick = onCreateCheckpoint) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Strings.saveVersion)
                }
                Spacer(modifier = Modifier.width(8.dp))
                PremiumButton(onClick = onSaveProject) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Strings.export)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (checkpoints.isEmpty()) {
            Text(
                Strings.noSavedVersions,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                itemsIndexed(checkpoints.reversed()) { index, checkpoint ->
                    CheckpointListItem(
                        checkpoint = checkpoint,
                        isCurrent = (checkpoints.size - 1 - index) == currentIndex,
                        onRestore = { onRestore(checkpoint.id) },
                        onDelete = { onDelete(checkpoint.id) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
internal fun EditMessageDialog(
    message: AiCodingMessage,
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>) -> Unit
) {
    var editedContent by remember { mutableStateOf(message.content) }
    var editedImages by remember { mutableStateOf(message.images) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.editMessage) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = editedContent,
                    onValueChange = { editedContent = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    maxLines = 10
                )
                
                if (editedImages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        Strings.imagesCount.replace("%d", "${editedImages.size}"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    Strings.editWarning,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            PremiumButton(
                onClick = { onConfirm(editedContent, editedImages) },
                enabled = editedContent.isNotBlank()
            ) {
                Text(Strings.resend)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.cancel)
            }
        }
    )
}

@Composable
internal fun SaveProjectDialog(
    storage: AiCodingStorage,
    files: List<ProjectFile>,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit
) {
    var projectName by remember { mutableStateOf("my-html-project") }
    var selectedDirIndex by remember { mutableIntStateOf(0) }
    var createFolder by remember { mutableStateOf(true) }
    
    val availableDirs = remember { storage.getAvailableSaveDirectories() }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.saveProject) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text(Strings.projectName) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text(Strings.saveLocation, style = MaterialTheme.typography.labelMedium)
                
                availableDirs.forEachIndexed { index, (name, _) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDirIndex = index }
                    ) {
                        RadioButton(
                            selected = index == selectedDirIndex,
                            onClick = { selectedDirIndex = index }
                        )
                        Text(name)
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = createFolder,
                        onCheckedChange = { createFolder = it }
                    )
                    Text(Strings.createProjectFolder)
                }
                
                Text(
                    Strings.willSaveFiles.replace("%d", "${files.size}"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            PremiumButton(
                onClick = {
                    val (_, dir) = availableDirs[selectedDirIndex]
                    val config = SaveConfig(
                        directory = dir.absolutePath,
                        projectName = projectName,
                        createFolder = createFolder,
                        overwrite = true
                    )
                    val result = storage.saveProject(config, files)
                    result.onSuccess { savedDir ->
                        onSaved(savedDir.absolutePath)
                    }.onFailure { e ->
                        // Error handling call
                    }
                },
                enabled = projectName.isNotBlank() && files.isNotEmpty()
            ) {
                Text(Strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.cancel)
            }
        }
    )
}

/**
 * repositorypanelcontent
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CodeLibrarySheetContent(
    items: List<CodeLibraryItem>,
    onPreview: (CodeLibraryItem) -> Unit,
    onUseContent: (CodeLibraryItem) -> Unit,
    onExportToProject: (CodeLibraryItem) -> Unit,
    onToggleFavorite: (CodeLibraryItem) -> Unit,
    onDelete: (CodeLibraryItem) -> Unit,
    onDismiss: () -> Unit
) {
    var filterFavorites by remember { mutableStateOf(false) }
    val filteredItems = if (filterFavorites) items.filter { it.isFavorite } else items
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Note
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                Strings.codeLibrary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row {
                PremiumFilterChip(
                    selected = filterFavorites,
                    onClick = { filterFavorites = !filterFavorites },
                    label = { Text(Strings.favorites) },
                    leadingIcon = if (filterFavorites) {
                        { Icon(Icons.Default.Favorite, null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            Strings.aiCodeAutoSaved,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.FolderOpen,
                        null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (filterFavorites) Strings.noFavorites else Strings.codeLibraryEmpty,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(filteredItems) { item ->
                    CodeLibraryItemCard(
                        item = item,
                        onPreview = { onPreview(item) },
                        onUseContent = { onUseContent(item) },
                        onExportToProject = { onExportToProject(item) },
                        onToggleFavorite = { onToggleFavorite(item) },
                        onDelete = { onDelete(item) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * repositoryitemcard
 */
@Composable
private fun CodeLibraryItemCard(
    item: CodeLibraryItem,
    onPreview: () -> Unit,
    onUseContent: () -> Unit,
    onExportToProject: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()) }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.isFavorite) {
                        Icon(
                            Icons.Default.Favorite,
                            null,
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(weight = 1f, fill = false)
                    )
                }
                Text(
                    dateFormat.format(java.util.Date(item.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                item.userPrompt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Filelabel
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                item.files.take(3).forEach { file ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            file.name,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (item.files.size > 3) {
                    Text(
                        "+${item.files.size - 3}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PremiumOutlinedButton(
                    onClick = onPreview,
                    modifier = Modifier.weight(weight = 1f, fill = true),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Outlined.Visibility, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Strings.preview, style = MaterialTheme.typography.labelMedium)
                }
                PremiumOutlinedButton(
                    onClick = onUseContent,
                    modifier = Modifier.weight(weight = 1f, fill = true),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Strings.use, style = MaterialTheme.typography.labelMedium)
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, null)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (item.isFavorite) Strings.unfavorite else Strings.favorite) },
                            onClick = { onToggleFavorite(); showMenu = false },
                            leadingIcon = {
                                Icon(
                                    if (item.isFavorite) Icons.Default.FavoriteBorder else Icons.Default.Favorite,
                                    null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(Strings.exportToProjectLibrary) },
                            onClick = { onExportToProject(); showMenu = false },
                            leadingIcon = { Icon(Icons.Outlined.FolderCopy, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(Strings.delete, color = Color(0xFFE53935)) },
                            onClick = { onDelete(); showMenu = false },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = Color(0xFFE53935)) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * checkpointpanelcontent
 */
@Composable
internal fun ConversationCheckpointsSheetContent(
    checkpoints: List<ConversationCheckpoint>,
    onRollback: (ConversationCheckpoint) -> Unit,
    onDelete: (ConversationCheckpoint) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.getDefault()) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            Strings.conversationCheckpoints,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            Strings.rollbackHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (checkpoints.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.History,
                        null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(Strings.noCheckpoints, color = MaterialTheme.colorScheme.outline)
                    Text(
                        Strings.autoCreateCheckpointHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(checkpoints) { checkpoint ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                                Text(
                                    checkpoint.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        dateFormat.format(java.util.Date(checkpoint.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            Strings.messagesCount.replace("%d", "${checkpoint.messageCount}"),
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (checkpoint.codeLibraryIds.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                Strings.codesCount.replace("%d", "${checkpoint.codeLibraryIds.size}"),
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Row {
                                IconButton(onClick = { onRollback(checkpoint) }) {
                                    Icon(
                                        Icons.Outlined.Restore,
                                        Strings.rollback,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { onDelete(checkpoint) }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        Strings.delete,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
