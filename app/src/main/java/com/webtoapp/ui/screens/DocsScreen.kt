package com.webtoapp.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.theme.LocalAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

// ── Doc item data model ──────────────────────────────────────────
data class DocItem(
    val fileName: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    /** Optional: Chinese-specific file name. If null, [fileName] is used for all languages. */
    val chineseFileName: String? = null,
)

// ── Doc list (matches assets/docs/) ─────────────────────────────
private val DOC_ITEMS = listOf(
    DocItem("README.md", "readme", "readmeDesc", Icons.Outlined.Article, chineseFileName = "README_CN.md"),
    DocItem("project-overview.md", "projectOverview", "projectOverviewDesc", Icons.Outlined.Info, chineseFileName = "project-overview_CN.md"),
    DocItem("architecture-guide.md", "architectureGuide", "architectureGuideDesc", Icons.Outlined.AccountTree, chineseFileName = "architecture-guide_CN.md"),
    DocItem("data-model-guide.md", "dataModelGuide", "dataModelGuideDesc", Icons.Outlined.Storage, chineseFileName = "data-model-guide_CN.md"),
    DocItem("shell-mode-guide.md", "shellModeGuide", "shellModeGuideDesc", Icons.Outlined.PhoneAndroid, chineseFileName = "shell-mode-guide_CN.md"),
    DocItem("extension-module-guide.md", "extensionModuleGuide", "extensionModuleGuideDesc", Icons.Outlined.Extension, chineseFileName = "extension-module-guide_CN.md"),
    DocItem("security-features-guide.md", "securityFeaturesGuide", "securityFeaturesGuideDesc", Icons.Outlined.Security, chineseFileName = "security-features-guide_CN.md"),
    DocItem("build-and-release-guide.md", "buildAndReleaseGuide", "buildAndReleaseGuideDesc", Icons.Outlined.Build, chineseFileName = "build-and-release-guide_CN.md"),
    DocItem("i18n-localization-guide.md", "i18nLocalizationGuide", "i18nLocalizationGuideDesc", Icons.Outlined.Language, chineseFileName = "i18n-localization-guide_CN.md"),
    DocItem("unit-test-guide.md", "unitTestGuide", "unitTestGuideDesc", Icons.Outlined.Science, chineseFileName = "unit-test-guide_CN.md"),
    DocItem("feedback-guide.md", "feedbackGuide", "feedbackGuideDesc", Icons.Outlined.Feedback, chineseFileName = "feedback-guide_CN.md"),
    DocItem("_doc-manual.md", "docManual", "docManualDesc", Icons.Outlined.MenuBook, chineseFileName = "_doc-manual_CN.md"),
    DocItem("_doc-maintenance.md", "docMaintenance", "docMaintenanceDesc", Icons.Outlined.AutoFixHigh, chineseFileName = "_doc-maintenance_CN.md"),
    DocItem("CHANGELOG.md", "changelog", "changelogDesc", Icons.Outlined.Update, chineseFileName = "CHANGELOG_CN.md"),
    DocItem("CODE_OF_CONDUCT.md", "codeOfConduct", "codeOfConductDesc", Icons.Outlined.Handshake, chineseFileName = "CODE_OF_CONDUCT_CN.md"),
    DocItem("CONTRIBUTING.md", "contributing", "contributingDesc", Icons.Outlined.MergeType, chineseFileName = "CONTRIBUTING_CN.md"),
    DocItem("shell-build-guide.md", "shellBuildGuide", "shellBuildGuideDesc", Icons.Outlined.Construction, chineseFileName = "shell-build-guide_CN.md"),
)

// ── Resolve i18n title / description from Strings ───────────────
private fun resolveTitle(key: String): String = when (key) {
    "readme" -> Strings.docReadme
    "projectOverview" -> Strings.docProjectOverview
    "architectureGuide" -> Strings.docArchitectureGuide
    "dataModelGuide" -> Strings.docDataModelGuide
    "shellModeGuide" -> Strings.docShellModeGuide
    "extensionModuleGuide" -> Strings.docExtensionModuleGuide
    "securityFeaturesGuide" -> Strings.docSecurityFeaturesGuide
    "buildAndReleaseGuide" -> Strings.docBuildAndReleaseGuide
    "i18nLocalizationGuide" -> Strings.docI18nLocalizationGuide
    "unitTestGuide" -> Strings.docUnitTestGuide
    "feedbackGuide" -> Strings.docFeedbackGuide
    "docManual" -> Strings.docDocManual
    "docMaintenance" -> Strings.docDocMaintenance
    "changelog" -> Strings.docChangelog
    "codeOfConduct" -> Strings.docCodeOfConduct
    "contributing" -> Strings.docContributing
    "shellBuildGuide" -> Strings.docShellBuildGuide
    else -> key
}

private fun resolveDesc(key: String): String = when (key) {
    "readme" -> Strings.docReadmeDesc
    "projectOverview" -> Strings.docProjectOverviewDesc
    "architectureGuide" -> Strings.docArchitectureGuideDesc
    "dataModelGuide" -> Strings.docDataModelGuideDesc
    "shellModeGuide" -> Strings.docShellModeGuideDesc
    "extensionModuleGuide" -> Strings.docExtensionModuleGuideDesc
    "securityFeaturesGuide" -> Strings.docSecurityFeaturesGuideDesc
    "buildAndReleaseGuide" -> Strings.docBuildAndReleaseGuideDesc
    "i18nLocalizationGuide" -> Strings.docI18nLocalizationGuideDesc
    "unitTestGuide" -> Strings.docUnitTestGuideDesc
    "feedbackGuide" -> Strings.docFeedbackGuideDesc
    "docManual" -> Strings.docDocManualDesc
    "docMaintenance" -> Strings.docDocMaintenanceDesc
    "changelog" -> Strings.docChangelogDesc
    "codeOfConduct" -> Strings.docCodeOfConductDesc
    "contributing" -> Strings.docContributingDesc
    "shellBuildGuide" -> Strings.docShellBuildGuideDesc
    else -> key
}

// ════════════════════════════════════════════════════════════════
//  Main DocsScreen
// ════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocsScreen(
    onBack: () -> Unit
) {
    var selectedDoc by remember { mutableStateOf<DocItem?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectedDoc != null) resolveTitle(selectedDoc!!.title) else Strings.docTitle,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedDoc != null) selectedDoc = null else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            )
        }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = selectedDoc,
                transitionSpec = {
                    if (targetState != null) {
                        slideInHorizontally { it / 3 } + fadeIn() togetherWith
                                slideOutHorizontally { -it / 3 } + fadeOut()
                    } else {
                        slideInHorizontally { -it / 3 } + fadeIn() togetherWith
                                slideOutHorizontally { it / 3 } + fadeOut()
                    }
                },
                label = "docs_transition"
            ) { doc ->
                if (doc == null) {
                    DocListScreen(
                        onDocClick = { selectedDoc = it }
                    )
                } else {
                    DocDetailScreen(
                        docItem = doc,
                        context = context,
                        onNavigateToDoc = { newDoc ->
                            selectedDoc = newDoc
                        }
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  Doc List — card grid
// ════════════════════════════════════════════════════════════════
@Composable
private fun DocListScreen(
    onDocClick: (DocItem) -> Unit
) {
    val theme = LocalAppTheme.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = 8.dp, bottom = 24.dp
        )
    ) {
        item {
            // Header
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = Strings.docTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Strings.docSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(DOC_ITEMS) { doc ->
            DocCard(
                doc = doc,
                onClick = { onDocClick(doc) }
            )
        }
    }
}

@Composable
private fun DocCard(
    doc: DocItem,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    val cardShape = RoundedCornerShape(theme.shapes.cardRadius)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = doc.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resolveTitle(doc.title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = resolveDesc(doc.description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  Doc Detail — WebView-based Markdown viewer
// ════════════════════════════════════════════════════════════════

private val markdownParser: Parser by lazy {
    Parser.builder()
        .extensions(listOf(TablesExtension.create()))
        .build()
}

private val htmlRenderer: HtmlRenderer by lazy {
    HtmlRenderer.builder()
        .extensions(listOf(TablesExtension.create()))
        .build()
}

@Composable
private fun DocDetailScreen(
    docItem: DocItem,
    context: Context,
    onNavigateToDoc: (DocItem) -> Unit
) {
    var htmlContent by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    val resolvedFileName = if (docItem.chineseFileName != null &&
        Strings.currentLanguage.value == AppLanguage.CHINESE
    ) docItem.chineseFileName else docItem.fileName

    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.surface.luminance() <= 0.5f

    LaunchedEffect(resolvedFileName) {
        withContext(Dispatchers.IO) {
            try {
                val md = context.assets.open("docs/$resolvedFileName")
                    .bufferedReader().use { it.readText() }
                val node = markdownParser.parse(md)
                val body = htmlRenderer.render(node)
                htmlContent = wrapHtml(body, isDark)
            } catch (e: Exception) {
                val errHtml = "<h1>Error</h1><p>Failed to load document: ${e.message}</p>"
                htmlContent = wrapHtml(errHtml, isDark)
            }
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator()
        }
        return
    }

    DocWebView(
        html = htmlContent,
        context = context,
        onNavigateToDoc = onNavigateToDoc
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DocWebView(
    html: String,
    context: Context,
    onNavigateToDoc: (DocItem) -> Unit
) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean {
                        val url = request.url.toString()
                        // Internal doc link: ./xxx.md or xxx.md
                        if (url.startsWith("doc:///")) {
                            val fileName = url.removePrefix("doc:///")
                            val docItem = DOC_ITEMS.find {
                                it.fileName == fileName || it.chineseFileName == fileName
                            }
                            if (docItem != null) {
                                onNavigateToDoc(docItem)
                            }
                            return true
                        }
                        // External link: open in browser
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            try {
                                ctx.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                )
                            } catch (_: Exception) {}
                            return true
                        }
                        // Anchor links: let WebView handle
                        return false
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        super.onPageFinished(view, url)
                        // Inject JS to intercept internal .md links
                        view.evaluateJavascript("""
                            (function() {
                                var links = document.querySelectorAll('a[href]');
                                links.forEach(function(link) {
                                    var href = link.getAttribute('href');
                                    if (href && !href.startsWith('http') && !href.startsWith('#') && !href.startsWith('javascript')) {
                                        if (href.endsWith('.md') || href.includes('.md')) {
                                            var name = href.replace(/^\.\//, '').replace(/^\//, '');
                                            link.setAttribute('href', 'doc:///' + name);
                                        }
                                    }
                                });
                            })();
                        """.trimIndent(), null)
                    }
                }
                loadDataWithBaseURL("https://localhost", html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://localhost", html, "text/html", "UTF-8", null)
        },
        modifier = Modifier.fillMaxSize()
    )
}

// ── HTML wrapper with themed CSS ──────────────────────────────────
private fun wrapHtml(body: String, isDark: Boolean): String {
    val bg = if (isDark) "#1c1b1f" else "#fffbfe"
    val fg = if (isDark) "#e6e1e5" else "#1c1b1f"
    val fgSecondary = if (isDark) "#c9c5ca" else "#49454f"
    val fgTertiary = if (isDark) "#938f99" else "#79747e"
    val primary = if (isDark) "#d0bcff" else "#6750a4"
    val primaryContainer = if (isDark) "#4f378b" else "#eaddff"
    val surfaceVariant = if (isDark) "#49454f" else "#e7e0ec"
    val codeBg = if (isDark) "#2b2930" else "#f3edf7"
    val blockquoteBorder = if (isDark) "#9a82db" else "#7f67be"
    val tableHeaderBg = if (isDark) "#37302e" else "#f0e6f6"
    val hrColor = if (isDark) "#49454f" else "#cac4d0"

    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=3">
<style>
* { box-sizing: border-box; margin: 0; padding: 0; }
body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    font-size: 15px; line-height: 1.7; color: $fg; background: $bg;
    padding: 16px; padding-bottom: 48px; word-wrap: break-word;
}
h1 { font-size: 26px; font-weight: 700; margin: 24px 0 12px; color: $fg;
     border-bottom: 3px solid $primary; padding-bottom: 8px; }
h2 { font-size: 22px; font-weight: 700; margin: 20px 0 10px; color: $fg; }
h3 { font-size: 19px; font-weight: 600; margin: 16px 0 8px; color: $fg; }
h4 { font-size: 17px; font-weight: 600; margin: 14px 0 6px; color: $fg; }
h5 { font-size: 15px; font-weight: 600; margin: 12px 0 6px; color: $fgSecondary; }
h6 { font-size: 14px; font-weight: 600; margin: 10px 0 4px; color: $fgTertiary; }
p { margin: 0 0 10px; }
a { color: $primary; text-decoration: none; }
a:hover { text-decoration: underline; }
strong { font-weight: 700; }
em { font-style: italic; }
code {
    font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
    font-size: 13px; background: $codeBg; color: $primary;
    padding: 2px 6px; border-radius: 4px;
}
pre {
    background: $codeBg; border-radius: 10px; padding: 14px;
    margin: 0 0 14px; overflow-x: auto;
    border: 1px solid $surfaceVariant;
}
pre code { background: none; padding: 0; color: $fgSecondary; font-size: 13px; line-height: 1.5; }
blockquote {
    border-left: 4px solid $blockquoteBorder; margin: 0 0 12px;
    padding: 8px 14px; background: $codeBg; border-radius: 0 8px 8px 0;
}
blockquote p { margin: 0 0 4px; color: $fgSecondary; }
ul, ol { margin: 0 0 10px; padding-left: 24px; }
li { margin: 4px 0; }
li > ul, li > ol { margin: 2px 0; }
hr { border: none; height: 1px; background: $hrColor; margin: 16px 0; }
img { max-width: 100%; height: auto; border-radius: 8px; margin: 8px 0; }
table { width: 100%; border-collapse: collapse; margin: 0 0 14px;
        border-radius: 10px; overflow: hidden; border: 1px solid $surfaceVariant; }
th { background: $tableHeaderBg; color: $primary; font-weight: 600;
     padding: 8px 10px; text-align: left; font-size: 13px; }
td { padding: 8px 10px; border-top: 1px solid $surfaceVariant; font-size: 14px; }
tr:nth-child(even) td { background: $codeBg; }
details { margin: 8px 0; background: $codeBg; border-radius: 8px; padding: 8px 12px; }
summary { font-weight: 600; color: $primary; cursor: pointer; }
div { margin: 8px 0; }
div[align="center"] { text-align: center; }
p[align="center"] { text-align: center; }
/* Badge images */
img[alt*="Stars"], img[alt*="Forks"], img[alt*="License"],
img[alt*="Android"], img[alt*="Kotlin"], img[alt*="Jetpack"],
img[alt*="Material"] { height: 24px; border-radius: 4px; vertical-align: middle; }
/* Emoji support */
.emoji { font-style: normal; }
</style>
</head>
<body>
$body
</body>
</html>
""".trimIndent()
}

private fun androidx.compose.ui.graphics.Color.luminance(): Float {
    return (0.2126f * red + 0.7152f * green + 0.0722f * blue)
}
