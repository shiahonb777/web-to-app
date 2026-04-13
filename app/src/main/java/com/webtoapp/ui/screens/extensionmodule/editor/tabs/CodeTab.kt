package com.webtoapp.ui.screens.extensionmodule.editor.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.CodeSnippetSelectorDialog
import com.webtoapp.ui.components.PremiumFilterChip
import com.webtoapp.ui.components.PremiumTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeTab(
    code: String,
    onCodeChange: (String) -> Unit,
    cssCode: String,
    onCssCodeChange: (String) -> Unit
) {
    var showJsTab by remember { mutableStateOf(true) }
    var showCodeSnippetSelector by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumFilterChip(
                    selected = showJsTab,
                    onClick = { showJsTab = true },
                    label = { Text("JavaScript") },
                    leadingIcon = if (showJsTab) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    }
                )
                PremiumFilterChip(
                    selected = !showJsTab,
                    onClick = { showJsTab = false },
                    label = { Text("CSS") },
                    leadingIcon = if (!showJsTab) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    }
                )
            }

            if (showJsTab) {
                FilledTonalButton(
                    onClick = { showCodeSnippetSelector = true },
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Strings.codeSnippets)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.04f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (showJsTab) Strings.availableFunctions else Strings.cssTips,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        if (showJsTab) Strings.jsFunctionsHint else Strings.cssHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        PremiumTextField(
            value = if (showJsTab) code else cssCode,
            onValueChange = { if (showJsTab) onCodeChange(it) else onCssCodeChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(weight = 1f, fill = true),
            label = { Text(if (showJsTab) Strings.javascriptCode else Strings.cssCode) },
            placeholder = {
                Text(if (showJsTab) Strings.jsCodePlaceholder else Strings.cssCodePlaceholder)
            },
            textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        )
    }

    if (showCodeSnippetSelector) {
        CodeSnippetSelectorDialog(
            onDismiss = { showCodeSnippetSelector = false },
            onSelect = { snippet ->
                val newCode = if (code.isBlank()) snippet.code else code + "\n\n" + snippet.code
                onCodeChange(newCode)
            }
        )
    }
}
