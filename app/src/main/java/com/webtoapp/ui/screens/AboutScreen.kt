package com.webtoapp.ui.screens

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.webtoapp.R
import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.DataBackupCard
import com.webtoapp.ui.design.WtaCard
import com.webtoapp.ui.design.WtaCardTone
import com.webtoapp.ui.design.WtaIconTitle
import com.webtoapp.ui.design.WtaScreen
import com.webtoapp.ui.design.WtaSection
import com.webtoapp.ui.design.WtaSectionHeaderStyle
import com.webtoapp.ui.design.WtaSpacing
import com.webtoapp.ui.design.rememberHapticClick
import com.webtoapp.ui.design.wtaPressScale

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val versionName = remember(context) { context.currentVersionName() }
    val versionCode = remember(context) { context.currentVersionCode() }

    WtaScreen(
        title = Strings.about,
        snackbarHostState = snackbarHostState,
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    horizontal = WtaSpacing.ScreenHorizontal,
                    vertical = WtaSpacing.ScreenVertical
                ),
            verticalArrangement = Arrangement.spacedBy(WtaSpacing.SectionGap)
        ) {
            AuthorHeroCard(
                versionName = versionName,
                versionCode = versionCode
            )

            ContactGrid()

            WtaSection(
                title = Strings.aboutThisApp,
                headerStyle = WtaSectionHeaderStyle.Quiet
            ) {
                LocalOnlyInfoCard()
            }

            WtaSection(
                title = Strings.dataBackupTitle,
                headerStyle = WtaSectionHeaderStyle.Quiet
            ) {
                DataBackupCard()
            }

            LegalTabContent()

            MadeWithLoveFooter()
        }
    }
}

/**
 * Hero card. The centerpiece of the About screen: a large avatar, the author
 * name, a subtitle, and a compact version pill that can be tapped to copy
 * the full build string for bug reports.
 */
@Composable
private fun AuthorHeroCard(
    versionName: String,
    versionCode: Long
) {
    val context = LocalContext.current
    val byLine = aboutAuthorByLine()
    val versionCopied = versionCopiedToast()

    WtaCard(
        modifier = Modifier.fillMaxWidth(),
        tone = WtaCardTone.Elevated,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 24.dp,
            vertical = 28.dp
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar with subtle ring
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.avatar_shiaho),
                    contentDescription = Strings.authorAvatar,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "WebToApp",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = byLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = Strings.aboutAppDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(18.dp))

            VersionPill(
                versionName = versionName,
                versionCode = versionCode,
                onCopy = {
                    val label = "WebToApp version"
                    val text = "WebToApp v$versionName ($versionCode)"
                    context.copyToClipboard(label, text)
                    Toast.makeText(
                        context,
                        versionCopied,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }
}

@Composable
private fun VersionPill(
    versionName: String,
    versionCode: Long,
    onCopy: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hapticClick = rememberHapticClick(onCopy)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = hapticClick
            )
            .wtaPressScale(interactionSource, pressedScale = 0.95f)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "v$versionName",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "·",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = versionCode.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Outlined.ContentCopy,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Contact grid. A 2x3 grid of social/contact channels. Each tile is a
 * self-contained button that opens the link (or copies the QQ group) with
 * haptic feedback.
 *
 * Links match the canonical set in the README. Kept monochrome on purpose
 * so no single brand color screams for attention.
 */
@Composable
private fun ContactGrid() {
    val context = LocalContext.current
    val groupWord = groupWord()
    val qqCopied = qqCopiedToast()
    val authorLabel = authorLabel()
    val contactTitle = contactSectionTitle()

    val entries = remember(groupWord, qqCopied, authorLabel) {
        listOf(
            ContactEntry(
                icon = Icons.Outlined.Code,
                label = "GitHub",
                value = "shiahonb777/web-to-app",
                action = ContactAction.OpenUrl("https://github.com/shiahonb777/web-to-app")
            ),
            ContactEntry(
                icon = Icons.Outlined.Send,
                label = "Telegram",
                value = "@webtoapp777",
                action = ContactAction.OpenUrl("https://t.me/webtoapp777")
            ),
            ContactEntry(
                icon = Icons.Outlined.Tag,
                label = "X (Twitter)",
                value = "@shiaho777",
                action = ContactAction.OpenUrl("https://x.com/shiaho777")
            ),
            ContactEntry(
                icon = Icons.Outlined.PlayCircleOutline,
                label = "Bilibili",
                value = "b23.tv/8mGDo2N",
                action = ContactAction.OpenUrl("https://b23.tv/8mGDo2N")
            ),
            ContactEntry(
                icon = Icons.Outlined.Groups,
                label = "QQ $groupWord",
                value = "1041130206",
                action = ContactAction.CopyText(
                    label = "QQ Group",
                    text = "1041130206",
                    toast = qqCopied
                )
            ),
            ContactEntry(
                icon = Icons.Outlined.Forum,
                label = authorLabel,
                value = "shiaho",
                action = ContactAction.OpenUrl("https://github.com/shiahonb777")
            )
        )
    }

    WtaSection(
        title = contactTitle,
        headerStyle = WtaSectionHeaderStyle.Quiet
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(WtaSpacing.CardGap)) {
            entries.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(WtaSpacing.CardGap)) {
                    pair.forEach { entry ->
                        ContactTile(
                            entry = entry,
                            modifier = Modifier.weight(1f),
                            onAction = { action ->
                                when (action) {
                                    is ContactAction.OpenUrl -> context.openUrl(action.url)
                                    is ContactAction.CopyText -> {
                                        context.copyToClipboard(action.label, action.text)
                                        Toast.makeText(
                                            context,
                                            action.toast,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        )
                    }
                    // Pad a trailing empty cell if the last row has only one entry
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private data class ContactEntry(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val action: ContactAction
)

private sealed interface ContactAction {
    data class OpenUrl(val url: String) : ContactAction
    data class CopyText(
        val label: String,
        val text: String,
        val toast: String
    ) : ContactAction
}

@Composable
private fun ContactTile(
    entry: ContactEntry,
    modifier: Modifier = Modifier,
    onAction: (ContactAction) -> Unit
) {
    WtaCard(
        onClick = { onAction(entry.action) },
        modifier = modifier,
        tone = WtaCardTone.Surface,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    entry.icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.value,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Outlined.Link,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun LocalOnlyInfoCard() {
    WtaCard(
        modifier = Modifier.fillMaxWidth(),
        tone = WtaCardTone.Surface
    ) {
        WtaIconTitle(
            icon = Icons.Outlined.Language,
            title = localOnlyTitle()
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = localOnlySummary(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LocalOnlyBullet(text = localOnlyBulletProjects())
                LocalOnlyBullet(text = localOnlyBulletRuntime())
                LocalOnlyBullet(text = localOnlyBulletExport())
                LocalOnlyBullet(text = localOnlyBulletRemoved())
            }
        }
    }
}

@Composable
private fun LocalOnlyBullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        )
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LegalTabContent() {
    WtaSection(
        title = Strings.legalDisclaimer,
        headerStyle = WtaSectionHeaderStyle.Quiet,
        collapsible = true,
        initiallyExpanded = false
    ) {
        WtaCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                LegalSection(Strings.legalDisclaimerTitle1, Strings.legalDisclaimerContent1)
                LegalSection(Strings.legalDisclaimerTitle2, Strings.legalDisclaimerContent2)
                LegalSection(Strings.legalDisclaimerTitle3, Strings.legalDisclaimerContent3)
                LegalSection(Strings.legalDisclaimerTitle4, Strings.legalDisclaimerContent4)
                LegalSection(Strings.legalDisclaimerTitle5, Strings.legalDisclaimerContent5)
                LegalSection(Strings.legalDisclaimerTitle6, Strings.legalDisclaimerContent6)
            }
        }
    }
}

@Composable
private fun LegalSection(title: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MadeWithLoveFooter() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.FavoriteBorder,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = Strings.madeWithLove,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

// ---- Helpers ---------------------------------------------------------------

private fun Context.currentVersionName(): String {
    return try {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        packageInfo.versionName ?: ""
    } catch (_: PackageManager.NameNotFoundException) {
        ""
    }
}

private fun Context.currentVersionCode(): Long {
    return try {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    } catch (_: PackageManager.NameNotFoundException) {
        0L
    }
}

private fun Context.copyToClipboard(label: String, text: String) {
    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun Context.openUrl(url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, Strings.cannotOpenLink, Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        Toast.makeText(this, Strings.cannotOpenLink, Toast.LENGTH_SHORT).show()
    }
}

// ---- Locale-aware strings --------------------------------------------------

@Composable
private fun aboutAuthorByLine(): String = when (Strings.currentLanguage.value) {
    AppLanguage.CHINESE -> "开发者 Shiaho"
    AppLanguage.ENGLISH -> "by Shiaho"
    AppLanguage.ARABIC -> "بواسطة Shiaho"
}

@Composable
private fun contactSectionTitle(): String = when (Strings.currentLanguage.value) {
    AppLanguage.CHINESE -> "联系作者"
    AppLanguage.ENGLISH -> "Get in touch"
    AppLanguage.ARABIC -> "تواصل مع المؤلف"
}

@Composable
private fun authorLabel(): String = when (Strings.currentLanguage.value) {
    AppLanguage.CHINESE -> "其他项目"
    AppLanguage.ENGLISH -> "More projects"
    AppLanguage.ARABIC -> "مشاريع أخرى"
}

@Composable
private fun groupWord(): String = when (Strings.currentLanguage.value) {
    AppLanguage.CHINESE -> "群"
    AppLanguage.ENGLISH -> "Group"
    AppLanguage.ARABIC -> "مجموعة"
}

@Composable
private fun qqCopiedToast(): String = when (Strings.currentLanguage.value) {
    AppLanguage.CHINESE -> "QQ 群号已复制"
    AppLanguage.ENGLISH -> "QQ group number copied"
    AppLanguage.ARABIC -> "تم نسخ رقم مجموعة QQ"
}

@Composable
private fun versionCopiedToast(): String = when (Strings.currentLanguage.value) {
    AppLanguage.CHINESE -> "版本号已复制"
    AppLanguage.ENGLISH -> "Version copied"
    AppLanguage.ARABIC -> "تم نسخ الإصدار"
}

@Composable
private fun localOnlyTitle(): String = when (Strings.currentLanguage.value) {
    AppLanguage.CHINESE -> "当前版本已收缩为纯本地工作流"
    AppLanguage.ENGLISH -> "Local-only workflow"
    AppLanguage.ARABIC -> "سير عمل محلي بالكامل"
}

@Composable
private fun localOnlySummary(): String = when (Strings.currentLanguage.value) {
    AppLanguage.CHINESE -> "项目创建、编辑、运行与导出都围绕设备本地资源、本地数据库和本地文件流转。"
    AppLanguage.ENGLISH -> "Project creation, editing, runtime, and export all revolve around device-local resources, the local database, and local files."
    AppLanguage.ARABIC -> "إنشاء المشروع وتحريره وتشغيله وتصديره يدور الآن بالكامل حول موارد الجهاز المحلية وقاعدة البيانات المحلية والملفات المحلية."
}

@Composable
private fun localOnlyBulletProjects(): String = when (Strings.currentLanguage.value) {
    AppLanguage.CHINESE -> "所有应用项目数据保存在本地数据库和本地文件中。"
    AppLanguage.ENGLISH -> "All app project data stays in the local database and local files."
    AppLanguage.ARABIC -> "جميع بيانات المشاريع تبقى داخل قاعدة البيانات المحلية والملفات المحلية."
}

@Composable
private fun localOnlyBulletRuntime(): String = when (Strings.currentLanguage.value) {
    AppLanguage.CHINESE -> "前端、HTML、媒体、Node.js、PHP、Python、Go 与多站点运行模式都走本地运行时。"
    AppLanguage.ENGLISH -> "Frontend, HTML, media, Node.js, PHP, Python, Go, and multi-site modes all run through the local runtime."
    AppLanguage.ARABIC -> "أوضاع الواجهة و HTML والوسائط و Node.js و PHP و Python و Go والمواقع المتعددة تعمل جميعها عبر بيئة تشغيل محلية."
}

@Composable
private fun localOnlyBulletExport(): String = when (Strings.currentLanguage.value) {
    AppLanguage.CHINESE -> "导出的壳应用只包含本地配置与本地运行能力。"
    AppLanguage.ENGLISH -> "Exported shell apps now contain only local configuration and local runtime capability."
    AppLanguage.ARABIC -> "تطبيقات الغلاف المصدرة تحتوي الآن على إعدادات محلية وقدرات تشغيل محلية فقط."
}

@Composable
private fun localOnlyBulletRemoved(): String = when (Strings.currentLanguage.value) {
    AppLanguage.CHINESE -> "当前界面只保留本地项目、本地运行、本地备份与本地导出所需入口。"
    AppLanguage.ENGLISH -> "The interface now keeps only the entry points required for local projects, local runtime, local backup, and local export."
    AppLanguage.ARABIC -> "تحتفظ الواجهة الآن فقط بالمداخل اللازمة للمشاريع المحلية والتشغيل المحلي والنسخ الاحتياطي المحلي والتصدير المحلي."
}
