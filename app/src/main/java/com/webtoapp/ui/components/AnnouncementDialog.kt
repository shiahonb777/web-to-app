package com.webtoapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.webtoapp.core.cloud.AnnouncementData
import com.webtoapp.core.i18n.Strings

/**
 * announcementdialog- Premium refactored
 *
 * for HomeScreen from announcement
 * Note
 * • type gradient
 * • animationicon
 * • contentcardarea
 * • Material 3 button
 */
@Composable
fun AnnouncementDialog(
    announcement: AnnouncementData,
    onDismiss: () -> Unit,
    onAction: ((String) -> Unit)? = null
) {
    // type config
    val typeConfig = remember(announcement.type) {
        when (announcement.type) {
            "warning" -> AnnouncementTypeConfig(
                icon = Icons.Filled.Warning,
                gradientColors = listOf(Color(0xFFFFA726), Color(0xFFFF7043)),
                surfaceTint = Color(0xFFFFF3E0),
                accentColor = Color(0xFFFF9800),
                label = "⚠️"
            )
            "error" -> AnnouncementTypeConfig(
                icon = Icons.Filled.Error,
                gradientColors = listOf(Color(0xFFEF5350), Color(0xFFC62828)),
                surfaceTint = Color(0xFFFFEBEE),
                accentColor = Color(0xFFF44336),
                label = "🚨"
            )
            "success" -> AnnouncementTypeConfig(
                icon = Icons.Filled.CheckCircle,
                gradientColors = listOf(Color(0xFF66BB6A), Color(0xFF2E7D32)),
                surfaceTint = Color(0xFFE8F5E9),
                accentColor = Color(0xFF4CAF50),
                label = "✅"
            )
            else -> AnnouncementTypeConfig(
                icon = Icons.Filled.Campaign,
                gradientColors = listOf(Color(0xFF42A5F5), Color(0xFF1565C0)),
                surfaceTint = Color(0xFFE3F2FD),
                accentColor = Color(0xFF2196F3),
                label = "📢"
            )
        }
    }

    // animation
    val infiniteTransition = rememberInfiniteTransition(label = "announcement_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val entryScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "entryScale"
    )
    val entryAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "entryAlpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .graphicsLayer {
                    scaleX = entryScale
                    scaleY = entryScale
                    alpha = entryAlpha
                },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column {
                // ═══════════════════════════════════════
                // topgradient
                // ═══════════════════════════════════════
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = typeConfig.gradientColors,
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // animationicon
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                typeConfig.icon,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Note
                        Text(
                            text = announcement.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    // closebutton
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = Strings.close,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // ═══════════════════════════════════════
                // contentarea
                // ═══════════════════════════════════════
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    // contentcard
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = typeConfig.surfaceTint.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = typeConfig.label,
                                fontSize = 20.sp,
                                modifier = Modifier.padding(end = 12.dp, top = 2.dp)
                            )
                            Text(
                                text = announcement.content,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 26.sp
                            )
                        }
                    }
                    
                    // prefer label( prefer display)
                    if (announcement.priority >= 8) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFF5722).copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.PriorityHigh,
                                    null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color(0xFFFF5722)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    Strings.announcementHighPriority,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFF5722),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // ═══════════════════════════════════════
                    // button
                    // ═══════════════════════════════════════
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (announcement.actionUrl != null && announcement.actionText != null) {
                            // action: button
                            PremiumOutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    Strings.cloudDismiss,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            PremiumButton(
                                onClick = {
                                    onAction?.invoke(announcement.actionUrl)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    announcement.actionText,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            // action: button
                            PremiumButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    Strings.gotIt,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * announcementtype config
 */
private data class AnnouncementTypeConfig(
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val surfaceTint: Color,
    val accentColor: Color,
    val label: String
)
