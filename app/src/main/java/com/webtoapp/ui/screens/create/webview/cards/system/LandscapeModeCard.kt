package com.webtoapp.ui.screens.create.webview.cards.system

import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.StayCurrentLandscape
import androidx.compose.material.icons.outlined.StayCurrentPortrait
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.data.model.OrientationMode
import com.webtoapp.ui.components.CollapsibleCardHeader
import com.webtoapp.ui.components.EnhancedElevatedCard

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun LandscapeModeCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    orientationMode: OrientationMode = if (enabled) OrientationMode.LANDSCAPE else OrientationMode.PORTRAIT,
    onOrientationModeChange: (OrientationMode) -> Unit = { mode ->
        onEnabledChange(mode == OrientationMode.LANDSCAPE)
    }
) {
    val isCustomOrientation = orientationMode != OrientationMode.PORTRAIT
    var advancedExpanded by remember {
        mutableStateOf(
            orientationMode in listOf(
                OrientationMode.REVERSE_PORTRAIT,
                OrientationMode.REVERSE_LANDSCAPE,
                OrientationMode.SENSOR_PORTRAIT,
                OrientationMode.SENSOR_LANDSCAPE
            )
        )
    }

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            CollapsibleCardHeader(
                icon = Icons.Outlined.ScreenRotation,
                title = AppStringsProvider.current().orientationModeLabel,
                checked = isCustomOrientation,
                onCheckedChange = { checked ->
                    onOrientationModeChange(if (checked) OrientationMode.LANDSCAPE else OrientationMode.PORTRAIT)
                }
            )

            SystemCardExpandContent(visible = isCustomOrientation) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = AppStringsProvider.current().orientationModeHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = AppStringsProvider.current().orientationBasicLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    listOf(
                        Triple(OrientationMode.LANDSCAPE, Icons.Outlined.StayCurrentLandscape, AppStringsProvider.current().orientationLandscape),
                        Triple(OrientationMode.AUTO, Icons.Outlined.ScreenRotation, AppStringsProvider.current().orientationAuto)
                    ).forEachIndexed { index, (mode, icon, label) ->
                        OrientationModeItem(
                            icon = icon,
                            title = label,
                            subtitle = when (mode) {
                                OrientationMode.LANDSCAPE -> AppStringsProvider.current().orientationLandscapeDesc
                                OrientationMode.AUTO -> AppStringsProvider.current().orientationAutoDesc
                                else -> ""
                            },
                            selected = orientationMode == mode,
                            onClick = { onOrientationModeChange(mode) }
                        )
                        if (index == 0) Spacer(modifier = Modifier.height(6.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { advancedExpanded = !advancedExpanded },
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = AppStringsProvider.current().orientationAdvancedLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            val advancedArrowRotation by animateFloatAsState(
                                targetValue = if (advancedExpanded) 180f else 0f,
                                animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
                                label = "advancedOrientationArrow"
                            )
                            Icon(
                                Icons.Filled.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.graphicsLayer { rotationZ = advancedArrowRotation }
                            )
                        }
                    }

                    SystemCardExpandContent(visible = advancedExpanded) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = AppStringsProvider.current().orientationReversedLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OrientationModeItem(
                                icon = Icons.Outlined.StayCurrentPortrait,
                                title = AppStringsProvider.current().orientationReversePortrait,
                                subtitle = AppStringsProvider.current().orientationReversePortraitDesc,
                                selected = orientationMode == OrientationMode.REVERSE_PORTRAIT,
                                onClick = { onOrientationModeChange(OrientationMode.REVERSE_PORTRAIT) },
                                iconRotation = 180f
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OrientationModeItem(
                                icon = Icons.Outlined.StayCurrentLandscape,
                                title = AppStringsProvider.current().orientationReverseLandscape,
                                subtitle = AppStringsProvider.current().orientationReverseLandscapeDesc,
                                selected = orientationMode == OrientationMode.REVERSE_LANDSCAPE,
                                onClick = { onOrientationModeChange(OrientationMode.REVERSE_LANDSCAPE) },
                                iconRotation = 180f
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = AppStringsProvider.current().orientationSensorLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OrientationModeItem(
                                icon = Icons.Outlined.StayCurrentPortrait,
                                title = AppStringsProvider.current().orientationSensorPortrait,
                                subtitle = AppStringsProvider.current().orientationSensorPortraitDesc,
                                selected = orientationMode == OrientationMode.SENSOR_PORTRAIT,
                                onClick = { onOrientationModeChange(OrientationMode.SENSOR_PORTRAIT) }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OrientationModeItem(
                                icon = Icons.Outlined.StayCurrentLandscape,
                                title = AppStringsProvider.current().orientationSensorLandscape,
                                subtitle = AppStringsProvider.current().orientationSensorLandscapeDesc,
                                selected = orientationMode == OrientationMode.SENSOR_LANDSCAPE,
                                onClick = { onOrientationModeChange(OrientationMode.SENSOR_LANDSCAPE) }
                            )
                        }
                    }

                    val currentModeHint = when (orientationMode) {
                        OrientationMode.AUTO -> AppStringsProvider.current().orientationAutoHint
                        OrientationMode.SENSOR_PORTRAIT -> AppStringsProvider.current().orientationSensorPortraitHint
                        OrientationMode.SENSOR_LANDSCAPE -> AppStringsProvider.current().orientationSensorLandscapeHint
                        else -> null
                    }

                    SystemCardExpandContent(visible = currentModeHint != null) {
                        if (currentModeHint != null) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Outlined.Info,
                                            null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = currentModeHint,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
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
}

@Composable
private fun OrientationModeItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    iconRotation: Float = 0f
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "orientationBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(200),
        label = "orientationBg"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 0.75.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = iconRotation }
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (selected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
