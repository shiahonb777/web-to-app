package com.webtoapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.webtoapp.core.ai.LrcTaskManager
import com.webtoapp.data.model.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 任务筛选类型
 */
enum class TaskFilterType(val displayName: String) {
    ALL("全部"),
    PROCESSING("进行中"),
    COMPLETED("已完成"),
    FAILED("失败")
}

/**
 * LRC 生成任务管理界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LrcTaskManagerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val taskManager = remember { LrcTaskManager.getInstance(context) }
    
    // 任务列表
    val tasks by taskManager.tasksFlow.collectAsState(initial = emptyList())
    
    // 筛选类型
    var selectedFilter by remember { mutableStateOf(TaskFilterType.ALL) }
    
    // 筛选后的任务列表
    val filteredTasks = remember(tasks, selectedFilter) {
        when (selectedFilter) {
            TaskFilterType.ALL -> tasks
            TaskFilterType.PROCESSING -> tasks.filter { 
                it.status == LrcTaskStatus.PENDING || it.status == LrcTaskStatus.PROCESSING 
            }
            TaskFilterType.COMPLETED -> tasks.filter { it.status == LrcTaskStatus.COMPLETED }
            TaskFilterType.FAILED -> tasks.filter { it.status == LrcTaskStatus.FAILED }
        }.sortedByDescending { it.createdAt }
    }
    
    // 统计数据
    val stats = remember(tasks) {
        mapOf(
            TaskFilterType.ALL to tasks.size,
            TaskFilterType.PROCESSING to tasks.count { 
                it.status == LrcTaskStatus.PENDING || it.status == LrcTaskStatus.PROCESSING 
            },
            TaskFilterType.COMPLETED to tasks.count { it.status == LrcTaskStatus.COMPLETED },
            TaskFilterType.FAILED to tasks.count { it.status == LrcTaskStatus.FAILED }
        )
    }
    
    // 展开的任务详情
    var expandedTaskId by remember { mutableStateOf<String?>(null) }
    
    // 确认清空对话框
    var showClearDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("生成任务管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (tasks.any { it.status == LrcTaskStatus.COMPLETED || it.status == LrcTaskStatus.FAILED }) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Outlined.DeleteSweep, "清空历史")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 筛选标签
            ScrollableTabRow(
                selectedTabIndex = TaskFilterType.entries.indexOf(selectedFilter),
                edgePadding = 16.dp,
                divider = {}
            ) {
                TaskFilterType.entries.forEach { filter ->
                    val count = stats[filter] ?: 0
                    Tab(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(filter.displayName)
                                if (count > 0) {
                                    Badge(
                                        containerColor = if (selectedFilter == filter)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text("$count")
                                    }
                                }
                            }
                        }
                    )
                }
            }
            
            // 任务列表
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            when (selectedFilter) {
                                TaskFilterType.ALL -> "暂无生成任务"
                                TaskFilterType.PROCESSING -> "没有正在进行的任务"
                                TaskFilterType.COMPLETED -> "没有已完成的任务"
                                TaskFilterType.FAILED -> "没有失败的任务"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "在音乐选择器中点击「生成字幕」开始",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskItem(
                            task = task,
                            isExpanded = expandedTaskId == task.id,
                            onToggleExpand = {
                                expandedTaskId = if (expandedTaskId == task.id) null else task.id
                            },
                            onDelete = {
                                scope.launch {
                                    taskManager.deleteTask(task.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    
    // 确认清空对话框
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Outlined.DeleteSweep, null) },
            title = { Text("清空历史记录") },
            text = { Text("确定要清空所有已完成和失败的任务记录吗？正在进行的任务不会被删除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            taskManager.clearCompletedTasks()
                        }
                        showClearDialog = false
                    }
                ) {
                    Text("清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 任务项
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskItem(
    task: LrcTask,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggleExpand
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 主信息行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 状态图标
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                when (task.status) {
                                    LrcTaskStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
                                    LrcTaskStatus.PROCESSING -> MaterialTheme.colorScheme.primaryContainer
                                    LrcTaskStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                                    LrcTaskStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when (task.status) {
                            LrcTaskStatus.PENDING -> Icon(
                                Icons.Outlined.Schedule,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LrcTaskStatus.PROCESSING -> CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            LrcTaskStatus.COMPLETED -> Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            LrcTaskStatus.FAILED -> Icon(
                                Icons.Default.Error,
                                null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    // 标题和时间
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            task.bgmName,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            dateFormat.format(Date(task.createdAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // 状态标签
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (task.status) {
                        LrcTaskStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
                        LrcTaskStatus.PROCESSING -> MaterialTheme.colorScheme.primaryContainer
                        LrcTaskStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                        LrcTaskStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Text(
                        when (task.status) {
                            LrcTaskStatus.PENDING -> "等待中"
                            LrcTaskStatus.PROCESSING -> "生成中"
                            LrcTaskStatus.COMPLETED -> "已完成"
                            LrcTaskStatus.FAILED -> "失败"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (task.status) {
                            LrcTaskStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                            LrcTaskStatus.PROCESSING -> MaterialTheme.colorScheme.primary
                            LrcTaskStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                            LrcTaskStatus.FAILED -> MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
            
            // 进度条（处理中时显示）
            if (task.status == LrcTaskStatus.PROCESSING && task.progress > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = task.progress / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(MaterialTheme.shapes.small)
                )
                Text(
                    "${task.progress}%",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 展开详情
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 详情信息
                    DetailRow("任务ID", task.id.take(8) + "...")
                    DetailRow("文件路径", task.bgmPath.substringAfterLast("/"))
                    DetailRow("创建时间", dateFormat.format(Date(task.createdAt)))
                    task.completedAt?.let {
                        DetailRow("完成时间", dateFormat.format(Date(it)))
                    }
                    task.errorMessage?.let {
                        DetailRow("错误信息", it, isError = true)
                    }
                    task.resultLrc?.let { lrc ->
                        DetailRow("歌词行数", "${lrc.lines.size} 行")
                    }
                    
                    // 操作按钮
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("删除")
                        }
                    }
                }
            }
        }
    }
}

/**
 * 详情行
 */
@Composable
private fun DetailRow(
    label: String,
    value: String,
    isError: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) MaterialTheme.colorScheme.error 
                   else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 200.dp)
        )
    }
}
