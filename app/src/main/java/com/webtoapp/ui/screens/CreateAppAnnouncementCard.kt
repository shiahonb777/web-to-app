package com.webtoapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import com.webtoapp.ui.animation.CardExpandTransition
import com.webtoapp.ui.animation.CardCollapseTransition
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.webtoapp.ui.viewmodel.EditState

/**
 * announcementsettingscard
 * itemunified UI: CollapsibleCardHeader + SettingsSwitch + PremiumTextField +.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementCard(
    editState: EditState,
    onEnabledChange: (Boolean) -> Unit,
    onAnnouncementChange: (Announcement) -> Unit
) {
    var showPreview by remember { mutableStateOf(false) }

    // previewdialog
    if (showPreview && (editState.announcement.title.isNotBlank() || editState.announcement.content.isNotBlank())) {
        AnnouncementDialog(
            config = AnnouncementConfig(
                announcement = editState.announcement,
                template = AnnouncementTemplate.valueOf(editState.announcement.template.name),
                showEmoji = editState.announcement.showEmoji,
                animationEnabled = editState.announcement.animationEnabled
            ),
            onDismiss = { showPreview = false },
            onLinkClick = { /* previewmode handle */ }
        )
    }

    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // header: icon + +
            CollapsibleCardHeader(
                icon = Icons.Outlined.Campaign,
                title = Strings.popupAnnouncement,
                checked = editState.announcementEnabled,
                onCheckedChange = onEnabledChange
            )

            AnimatedVisibility(
                visible = editState.announcementEnabled,
                enter = CardExpandTransition,
                exit = CardCollapseTransition
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // select
                    AnnouncementTemplateSelector(
                        selectedTemplate = AnnouncementTemplate.valueOf(
                            editState.announcement.template.name
                        ),
                        onTemplateSelected = { template ->
                            onAnnouncementChange(
                                editState.announcement.copy(
                                    template = AnnouncementTemplateType.valueOf(template.name)
                                )
                            )
                        }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // announcement
                    PremiumTextField(
                        value = editState.announcement.title,
                        onValueChange = {
                            onAnnouncementChange(editState.announcement.copy(title = it))
                        },
                        label = { Text(Strings.announcementTitle) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // announcementcontent
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

                    // URL
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

                    // button( onlywhen URL display)
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

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // display
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

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // settings
                    Text(
                        Strings.announcementTriggerSettings,
                        style = MaterialTheme.typography.labelLarge
                    )

                    SettingsSwitch(
                        title = Strings.announcementTriggerOnLaunch,
                        subtitle = Strings.announcementTriggerOnLaunchHint,
                        checked = editState.announcement.triggerOnLaunch,
                        onCheckedChange = {
                            onAnnouncementChange(editState.announcement.copy(triggerOnLaunch = it))
                        }
                    )

                    SettingsSwitch(
                        title = Strings.announcementTriggerOnNoNetwork,
                        subtitle = Strings.announcementTriggerOnNoNetworkHint,
                        checked = editState.announcement.triggerOnNoNetwork,
                        onCheckedChange = {
                            onAnnouncementChange(editState.announcement.copy(triggerOnNoNetwork = it))
                        }
                    )

                    // Note
                    var intervalExpanded by remember { mutableStateOf(false) }
                    val intervalOptions = listOf(0, 1, 3, 5, 10, 15, 30, 60)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                Strings.announcementTriggerInterval,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                Strings.announcementTriggerIntervalHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        ExposedDropdownMenuBox(
                            expanded = intervalExpanded,
                            onExpandedChange = { intervalExpanded = it },
                            modifier = Modifier.width(110.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        if (editState.announcement.triggerIntervalMinutes == 0)
                                            Strings.announcementIntervalDisabled
                                        else
                                            "${editState.announcement.triggerIntervalMinutes} ${Strings.minutesShort}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalExpanded)
                                }
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

                    // Note
                    AnimatedVisibility(
                        visible = editState.announcement.triggerIntervalMinutes > 0
                    ) {
                        SettingsSwitch(
                            title = Strings.announcementTriggerIntervalIncludeLaunch,
                            subtitle = Strings.announcementTriggerOnLaunchHint,
                            checked = editState.announcement.triggerIntervalIncludeLaunch,
                            onCheckedChange = {
                                onAnnouncementChange(editState.announcement.copy(triggerIntervalIncludeLaunch = it))
                            }
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // advanced
                    Text(
                        Strings.announcementAdvancedOptions,
                        style = MaterialTheme.typography.labelLarge
                    )

                    SettingsSwitch(
                        title = Strings.showEmoji,
                        subtitle = Strings.announcementEmojiHint,
                        checked = editState.announcement.showEmoji,
                        onCheckedChange = {
                            onAnnouncementChange(editState.announcement.copy(showEmoji = it))
                        }
                    )

                    SettingsSwitch(
                        title = Strings.enableAnimation,
                        subtitle = Strings.announcementAnimationHint,
                        checked = editState.announcement.animationEnabled,
                        onCheckedChange = {
                            onAnnouncementChange(editState.announcement.copy(animationEnabled = it))
                        }
                    )

                    SettingsSwitch(
                        title = Strings.announcementRequireConfirmLabel,
                        subtitle = Strings.announcementRequireConfirmHint,
                        checked = editState.announcement.requireConfirmation,
                        onCheckedChange = {
                            onAnnouncementChange(editState.announcement.copy(requireConfirmation = it))
                        }
                    )

                    SettingsSwitch(
                        title = Strings.announcementAllowNeverShowLabel,
                        subtitle = Strings.announcementAllowNeverShowHint,
                        checked = editState.announcement.allowNeverShow,
                        onCheckedChange = {
                            onAnnouncementChange(editState.announcement.copy(allowNeverShow = it))
                        }
                    )

                    // previewbutton
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
