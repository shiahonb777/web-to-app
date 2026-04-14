package com.webtoapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.sample.TypedSampleProject
import com.webtoapp.ui.theme.LocalAppTheme
import androidx.compose.ui.graphics.Color

/**
 * itemcard
 * 
 * support PHP / Python / Go / DocsSite type item.
 * UI mode SampleProjectCard. kt( gradient, Play button, label) .
 */
@Composable
fun TypedSampleProjectsCard(
    title: String,
    subtitle: String,
    samples: List<TypedSampleProject>,
    onSelectSample: (TypedSampleProject) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    
    val containerModifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(theme.shapes.cardRadius))
        .background(MaterialTheme.colorScheme.surface)
    
    Box(modifier = containerModifier) {
        Column(modifier = Modifier.padding(20.dp)) {
            // area
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Science,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(14.dp))
                
                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(theme.shapes.buttonRadius))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        Strings.quickExperience,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            samples.forEachIndexed { index, sample ->
                TypedSampleProjectItem(
                    sample = sample,
                    onClick = { onSelectSample(sample) }
                )
                if (index < samples.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

/**
 * item
 */
@Composable
private fun TypedSampleProjectItem(
    sample: TypedSampleProject,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    val sampleColor = Color(sample.brandColor)
    
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "scale"
    )
    
    val itemModifier = Modifier
        .fillMaxWidth()
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clip(RoundedCornerShape(theme.shapes.cardRadius * 0.75f))
        .background(if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f))
        .clickable {
            isPressed = true
            onClick()
        }
        .padding(14.dp)
    
    Row(
        modifier = itemModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // icon
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            sampleColor.copy(alpha = 0.15f),
                            sampleColor.copy(alpha = 0.05f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(com.webtoapp.util.SvgIconMapper.getIcon(sample.icon), contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        }
        
        Spacer(modifier = Modifier.width(14.dp))
        
        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
            Text(
                sample.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(3.dp))
            
            Text(
                sample.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                sample.tags.take(3).forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(sampleColor.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = sampleColor
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Play button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            sampleColor.copy(alpha = 0.2f),
                            sampleColor.copy(alpha = 0.1f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = Strings.run,
                tint = sampleColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
