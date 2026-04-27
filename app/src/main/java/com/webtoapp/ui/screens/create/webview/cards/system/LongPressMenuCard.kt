package com.webtoapp.ui.screens.create.webview.cards.system

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.BubbleChart
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Mouse
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.data.model.LongPressMenuStyle
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.PremiumFilterChip

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LongPressMenuCard(
    style: LongPressMenuStyle,
    onStyleChange: (LongPressMenuStyle) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "arrowRotation"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (expanded) 400 else 200,
            easing = FastOutSlowInEasing
        ),
        label = "contentAlpha"
    )

    data class StyleOption(
        val style: LongPressMenuStyle,
        val name: String,
        val icon: ImageVector,
        val accentColor: Color
    )

    val styleOptions = listOf(
        StyleOption(LongPressMenuStyle.FULL, AppStringsProvider.current().longPressMenuStyleFull, Icons.AutoMirrored.Outlined.ViewList, Color(0xFF6366F1)),
        StyleOption(LongPressMenuStyle.SIMPLE, AppStringsProvider.current().longPressMenuStyleSimple, Icons.Outlined.ViewAgenda, Color(0xFF22C55E)),
        StyleOption(LongPressMenuStyle.IOS, AppStringsProvider.current().longPressMenuStyleIos, Icons.Outlined.PhoneIphone, Color(0xFF3B82F6)),
        StyleOption(LongPressMenuStyle.FLOATING, AppStringsProvider.current().longPressMenuStyleFloating, Icons.Outlined.BubbleChart, Color(0xFFF97316)),
        StyleOption(LongPressMenuStyle.CONTEXT, AppStringsProvider.current().longPressMenuStyleContext, Icons.Outlined.Mouse, Color(0xFF8B5CF6)),
        StyleOption(LongPressMenuStyle.DISABLED, AppStringsProvider.current().longPressMenuStyleDisabled, Icons.Outlined.Block, Color(0xFF9CA3AF))
    )

    val selectedOption = styleOptions.find { it.style == style } ?: styleOptions[0]
    val isEnabled = style != LongPressMenuStyle.DISABLED

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(weight = 1f, fill = true)
                ) {
                    SystemCardFeatureIcon(
                        icon = Icons.AutoMirrored.Outlined.ListAlt,
                        enabled = isEnabled
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = AppStringsProvider.current().longPressMenuSettings,
                            style = MaterialTheme.typography.titleMedium
                        )
                        AnimatedContent(
                            targetState = selectedOption,
                            transitionSpec = {
                                fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                            },
                            label = "styleLabelCrossfade"
                        ) { option ->
                            Text(
                                text = option.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = option.accentColor
                            )
                        }
                    }
                }
                Icon(
                    Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = arrowRotation
                    }
                )
            }

            SystemCardExpandContent(visible = expanded) {
                Column(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .graphicsLayer { alpha = contentAlpha },
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        styleOptions.forEach { option ->
                            val isSelected = option.style == style
                            PremiumFilterChip(
                                selected = isSelected,
                                onClick = { onStyleChange(option.style) },
                                label = { Text(option.name) },
                                leadingIcon = {
                                    Icon(
                                        option.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isSelected) option.accentColor
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }

                    Crossfade(
                        targetState = selectedOption,
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = FastOutSlowInEasing
                        ),
                        label = "styleDetailCrossfade"
                    ) { currentOption ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) {
                                Color.White.copy(alpha = 0.06f)
                            } else {
                                currentOption.accentColor.copy(alpha = 0.04f)
                            },
                            border = BorderStroke(
                                1.dp,
                                currentOption.accentColor.copy(alpha = 0.15f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                color = currentOption.accentColor.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            currentOption.icon,
                                            contentDescription = null,
                                            tint = currentOption.accentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                                        Text(
                                            text = currentOption.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = currentOption.accentColor
                                        )
                                    }
                                }

                                if (currentOption.style != LongPressMenuStyle.DISABLED) {
                                    LongPressMenuStylePreview(
                                        style = currentOption.style,
                                        accentColor = currentOption.accentColor
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
private fun LongPressMenuStylePreview(
    style: LongPressMenuStyle,
    accentColor: Color
) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(
                color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) {
                    Color.White.copy(alpha = 0.05f)
                } else {
                    Color.White.copy(alpha = 0.72f)
                },
                shape = RoundedCornerShape(10.dp)
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            LongPressMenuStyle.FULL, LongPressMenuStyle.SIMPLE -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(3.dp)
                                    .background(
                                        color = onSurfaceColor.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            repeat(if (style == LongPressMenuStyle.FULL) 3 else 2) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(accentColor.copy(alpha = 0.2f), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .weight(weight = 1f, fill = true)
                                            .height(10.dp)
                                            .background(
                                                onSurfaceColor.copy(alpha = 0.12f),
                                                RoundedCornerShape(4.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            LongPressMenuStyle.IOS -> {
                Surface(
                    modifier = Modifier.width(160.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 6.dp
                ) {
                    Column {
                        repeat(3) { index ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(10.dp)
                                        .background(
                                            onSurfaceColor.copy(alpha = 0.15f),
                                            RoundedCornerShape(3.dp)
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(
                                            accentColor.copy(alpha = 0.25f),
                                            CircleShape
                                        )
                                )
                            }
                            if (index < 2) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 14.dp),
                                    color = onSurfaceColor.copy(alpha = 0.08f)
                                )
                            }
                        }
                    }
                }
            }

            LongPressMenuStyle.FLOATING -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier.align(Alignment.Center),
                        shape = CircleShape,
                        color = accentColor,
                        tonalElevation = 6.dp
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    val positions = listOf(
                        Alignment.TopCenter,
                        Alignment.CenterStart,
                        Alignment.CenterEnd,
                        Alignment.BottomCenter
                    )
                    val icons = listOf(
                        Icons.Default.Download,
                        Icons.Default.ContentCopy,
                        Icons.Default.Share,
                        Icons.Default.OpenInBrowser
                    )
                    positions.forEachIndexed { idx, align ->
                        Surface(
                            modifier = Modifier
                                .align(align)
                                .padding(
                                    when (align) {
                                        Alignment.TopCenter -> PaddingValues(top = 4.dp)
                                        Alignment.BottomCenter -> PaddingValues(bottom = 4.dp)
                                        Alignment.CenterStart -> PaddingValues(start = 16.dp)
                                        Alignment.CenterEnd -> PaddingValues(end = 16.dp)
                                        else -> PaddingValues(0.dp)
                                    }
                                ),
                            shape = CircleShape,
                            color = accentColor.copy(alpha = 0.65f - idx * 0.08f),
                            tonalElevation = 3.dp
                        ) {
                            Box(
                                modifier = Modifier.size(30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icons[idx], null, tint = Color.White, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }

            LongPressMenuStyle.CONTEXT -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopStart
                ) {
                    Surface(
                        modifier = Modifier
                            .padding(start = 16.dp, top = 12.dp)
                            .width(120.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 6.dp
                    ) {
                        Column(modifier = Modifier.padding(vertical = 3.dp)) {
                            repeat(4) { index ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (index == 0) accentColor.copy(alpha = 0.08f)
                                            else Color.Transparent
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(
                                                if (index == 0) accentColor
                                                else onSurfaceColor.copy(alpha = 0.25f),
                                                RoundedCornerShape(3.dp)
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .weight(weight = 1f, fill = true)
                                            .height(8.dp)
                                            .background(
                                                onSurfaceColor.copy(alpha = 0.15f),
                                                RoundedCornerShape(3.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            LongPressMenuStyle.DISABLED -> Unit
        }
    }
}
