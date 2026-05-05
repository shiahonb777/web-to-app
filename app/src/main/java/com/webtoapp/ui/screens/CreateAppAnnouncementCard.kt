package com.webtoapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import com.webtoapp.ui.animation.CardExpandTransition
import com.webtoapp.ui.animation.CardCollapseTransition
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.webtoapp.R
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.*
import com.webtoapp.ui.components.*
import com.webtoapp.ui.components.announcement.AnnouncementDialog
import com.webtoapp.ui.components.announcement.AnnouncementConfig
import com.webtoapp.ui.components.announcement.AnnouncementTemplate
import com.webtoapp.ui.components.announcement.AnnouncementTemplateSelector
import com.webtoapp.ui.components.announcement.toStoredTemplate
import com.webtoapp.ui.components.announcement.toUiTemplate
import com.webtoapp.ui.design.*
import com.webtoapp.ui.viewmodel.EditState





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementCard(
    editState: EditState,
    onEnabledChange: (Boolean) -> Unit,
    onAnnouncementChange: (Announcement) -> Unit
) {
    var showPreview by remember { mutableStateOf(false) }


    if (showPreview && (editState.announcement.title.isNotBlank() || editState.announcement.content.isNotBlank())) {
        AnnouncementDialog(
            config = AnnouncementConfig(
                announcement = editState.announcement,
                template = editState.announcement.template.toUiTemplate(),
                showEmoji = editState.announcement.showEmoji,
                animationEnabled = editState.announcement.animationEnabled
            ),
            onDismiss = { showPreview = false },
            onLinkClick = {  }
        )
    }

    WtaSettingCard {
        Column(verticalArrangement = Arrangement.spacedBy(WtaSpacing.ContentGap)) {

            WtaToggleRow(
                icon = Icons.Outlined.Campaign,
                title = Strings.popupAnnouncement,
                subtitle = if (editState.announcementEnabled) Strings.enabled else Strings.disabled,
                checked = editState.announcementEnabled,
                onCheckedChange = onEnabledChange
            )

            AnimatedVisibility(
                visible = editState.announcementEnabled,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = WtaSpacing.RowHorizontal,
                        vertical = WtaSpacing.ContentGap
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {


                    AnnouncementTemplateSelector(
                        selectedTemplate = editState.announcement.template.toUiTemplate(),
                        onTemplateSelected = { template ->
                            onAnnouncementChange(
                                editState.announcement.copy(
                                    template = template.toStoredTemplate()
                                )
                            )
                        }
                    )

                    WtaSectionDivider(modifier = Modifier.padding(horizontal = 0.dp))


                    PremiumTextField(
                        value = editState.announcement.title,
                        onValueChange = {
                            onAnnouncementChange(editState.announcement.copy(title = it))
                        },
                        label = { Text(Strings.announcementTitle) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )


                    PremiumTextField(
                        value = editState.announcement.content,
                        onValueChange = {
                            onAnnouncementChange(editState.announcement.copy(content = it))
                        },
                        label = { Text(Strings.announcementContent) },
                        supportingText = {
                            Text(
                                "${editState.announcement.content.length}/500",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (editState.announcement.content.length > 500)
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )


                    PremiumTextField(
                        value = editState.announcement.linkUrl ?: "",
                        onValueChange = {
                            onAnnouncementChange(editState.announcement.copy(linkUrl = it.ifBlank { null }))
                        },
                        label = { Text(Strings.linkUrl) },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )


                    AnimatedVisibility(
                        visible = !editState.announcement.linkUrl.isNullOrBlank()
                    ) {
                        PremiumTextField(
                            value = editState.announcement.linkText ?: "",
                            onValueChange = {
                                onAnnouncementChange(editState.announcement.copy(linkText = it.ifBlank { null }))
                            },
                            label = { Text(Strings.linkButtonText) },
                            placeholder = { Text(Strings.viewDetails) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    WtaSectionDivider(modifier = Modifier.padding(horizontal = 0.dp))


                    Text(
                        Strings.displayFrequency,
                        style = MaterialTheme.typography.labelLarge
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PremiumFilterChip(
                            selected = editState.announcement.showOnce,
                            onClick = { onAnnouncementChange(editState.announcement.copy(showOnce = true)) },
                            label = { Text(Strings.showOnce) },
                            leadingIcon = if (editState.announcement.showOnce) {
                                { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                            } else null
                        )
                        PremiumFilterChip(
                            selected = !editState.announcement.showOnce,
                            onClick = { onAnnouncementChange(editState.announcement.copy(showOnce = false)) },
                            label = { Text(Strings.everyLaunch) },
                            leadingIcon = if (!editState.announcement.showOnce) {
                                { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                            } else null
                        )
                    }

                    WtaSectionDivider(modifier = Modifier.padding(horizontal = 0.dp))


                    Text(
                        Strings.announcementTriggerSettings,
                        style = MaterialTheme.typography.labelLarge
                    )

                    WtaToggleRow(
                        title = Strings.announcementTriggerOnLaunch,
                        subtitle = Strings.announcementTriggerOnLaunchHint,
                        icon = Icons.Outlined.RocketLaunch,
                        checked = editState.announcement.triggerOnLaunch,
                        onCheckedChange = {
                            onAnnouncementChange(editState.announcement.copy(triggerOnLaunch = it))
                        }
                    )

                    WtaToggleRow(
                        title = Strings.announcementTriggerOnNoNetwork,
                        subtitle = Strings.announcementTriggerOnNoNetworkHint,
                        icon = Icons.Outlined.CloudOff,
                        checked = editState.announcement.triggerOnNoNetwork,
                        onCheckedChange = {
                            onAnnouncementChange(editState.announcement.copy(triggerOnNoNetwork = it))
                        }
                    )


                    var intervalExpanded by remember { mutableStateOf(false) }
                    val intervalOptions = listOf(0, 1, 3, 5, 10, 15, 30, 60)

                    WtaSettingRow(
                        title = Strings.announcementTriggerInterval,
                        subtitle = Strings.announcementTriggerIntervalHint,
                        icon = Icons.Outlined.Timer
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = intervalExpanded,
                            onExpandedChange = { intervalExpanded = it },
                            modifier = Modifier.width(110.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { intervalExpanded = true },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(WtaRadius.Button)
                            ) {
                                Text(
                                    if (editState.announcement.triggerIntervalMinutes == 0)
                                        Strings.announcementIntervalDisabled
                                    else
                                        "${editState.announcement.triggerIntervalMinutes} ${Strings.minutesShort}",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalExpanded)
                            }
                            ExposedDropdownMenu(
                                expanded = intervalExpanded,
                                onDismissRequest = { intervalExpanded = false }
                            ) {
                                intervalOptions.forEach { interval ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (interval == editState.announcement.triggerIntervalMinutes) {
                                                    Icon(
                                                        Icons.Filled.Check, null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                }
                                                Text(
                                                    if (interval == 0) Strings.announcementIntervalDisabled
                                                    else "$interval ${Strings.minutesShort}"
                                                )
                                            }
                                        },
                                        onClick = {
                                            onAnnouncementChange(editState.announcement.copy(triggerIntervalMinutes = interval))
                                            intervalExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }


                    AnimatedVisibility(
                        visible = editState.announcement.triggerIntervalMinutes > 0
                    ) {
                        WtaToggleRow(
                            title = Strings.announcementTriggerIntervalIncludeLaunch,
                            subtitle = Strings.announcementTriggerOnLaunchHint,
                            icon = Icons.Outlined.PlayCircle,
                            checked = editState.announcement.triggerIntervalIncludeLaunch,
                            onCheckedChange = {
                                onAnnouncementChange(editState.announcement.copy(triggerIntervalIncludeLaunch = it))
                            }
                        )
                    }

                    WtaSectionDivider(modifier = Modifier.padding(horizontal = 0.dp))


                    Text(
                        Strings.announcementAdvancedOptions,
                        style = MaterialTheme.typography.labelLarge
                    )

                    WtaToggleRow(
                        title = Strings.showEmoji,
                        subtitle = Strings.announcementEmojiHint,
                        icon = Icons.Outlined.EmojiEmotions,
                        checked = editState.announcement.showEmoji,
                        onCheckedChange = {
                            onAnnouncementChange(editState.announcement.copy(showEmoji = it))
                        }
                    )

                    WtaToggleRow(
                        title = Strings.enableAnimation,
                        subtitle = Strings.announcementAnimationHint,
                        icon = Icons.Outlined.Animation,
                        checked = editState.announcement.animationEnabled,
                        onCheckedChange = {
                            onAnnouncementChange(editState.announcement.copy(animationEnabled = it))
                        }
                    )

                    WtaToggleRow(
                        title = Strings.announcementRequireConfirmLabel,
                        subtitle = Strings.announcementRequireConfirmHint,
                        icon = Icons.Outlined.TaskAlt,
                        checked = editState.announcement.requireConfirmation,
                        onCheckedChange = {
                            onAnnouncementChange(editState.announcement.copy(requireConfirmation = it))
                        }
                    )

                    WtaToggleRow(
                        title = Strings.announcementAllowNeverShowLabel,
                        subtitle = Strings.announcementAllowNeverShowHint,
                        icon = Icons.Outlined.VisibilityOff,
                        checked = editState.announcement.allowNeverShow,
                        onCheckedChange = {
                            onAnnouncementChange(editState.announcement.copy(allowNeverShow = it))
                        }
                    )


                    PremiumOutlinedButton(
                        onClick = { showPreview = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = editState.announcement.title.isNotBlank() || editState.announcement.content.isNotBlank()
                    ) {
                        Icon(Icons.Outlined.Preview, null, Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Strings.previewAnnouncementEffect)
                    }
                }
            }
        }
    }
}
