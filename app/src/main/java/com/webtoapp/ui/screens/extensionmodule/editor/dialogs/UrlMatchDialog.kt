package com.webtoapp.ui.screens.extensionmodule.editor.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.extension.UrlMatchRule
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.PremiumTextField

@Composable
fun UrlMatchDialog(
    urlMatches: List<UrlMatchRule>,
    onUrlMatchesChange: (List<UrlMatchRule>) -> Unit,
    onDismiss: () -> Unit
) {
    var newPattern by remember { mutableStateOf("") }
    var isRegex by remember { mutableStateOf(false) }
    var isExclude by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.urlMatchRules) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (urlMatches.isNotEmpty()) {
                    urlMatches.forEachIndexed { index, rule ->
                        val ruleTint = if (rule.exclude) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    ruleTint.copy(alpha = 0.12f),
                                                    ruleTint.copy(alpha = 0.04f)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Link,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = ruleTint
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        rule.pattern,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (rule.isRegex) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f))
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    Strings.regex,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.tertiary,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(ruleTint.copy(alpha = 0.10f))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                if (rule.exclude) Strings.exclude else Strings.include,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ruleTint,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        onUrlMatchesChange(urlMatches.filterIndexed { i, _ -> i != index })
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = Strings.delete,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .height(0.5.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    )
                }

                PremiumTextField(
                    value = newPattern,
                    onValueChange = { newPattern = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(Strings.urlPattern) },
                    placeholder = { Text("*.example.com/*") },
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isRegex, onCheckedChange = { isRegex = it })
                        Text(Strings.regexExpression)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isExclude, onCheckedChange = { isExclude = it })
                        Text(Strings.excludeRule)
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = {
                        if (newPattern.isNotBlank()) {
                            onUrlMatchesChange(urlMatches + UrlMatchRule(newPattern, isRegex, isExclude))
                            newPattern = ""
                            isRegex = false
                            isExclude = false
                        }
                    },
                    enabled = newPattern.isNotBlank()
                ) {
                    Text(Strings.add)
                }
                TextButton(onClick = onDismiss) {
                    Text(Strings.done)
                }
            }
        }
    )
}
