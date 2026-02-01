package com.webtoapp.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.i18n.Strings

/**
 * 强制运行倒计时覆盖层
 *
 * 显示剩余时间，并提供密码退出功能
 */
@Composable
fun ForcedRunCountdownOverlay(
    remainingMs: Long,
    allowEmergencyExit: Boolean,
    emergencyPassword: String?,
    onEmergencyExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    // 格式化剩余时间
    val formattedTime = remember(remainingMs) {
        val totalSeconds = (remainingMs.coerceAtLeast(0) / 1000).toInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
            shadowElevation = 4.dp,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = allowEmergencyExit && !emergencyPassword.isNullOrEmpty()
            ) {
                showPasswordDialog = true
            }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 锁图标（可点击退出时显示不同图标）
                Icon(
                    imageVector = if (allowEmergencyExit && !emergencyPassword.isNullOrEmpty())
                        Icons.Filled.LockOpen else Icons.Filled.Lock,
                    contentDescription = if (allowEmergencyExit) Strings.tapToExit else null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp)
                )

                Column {
                    Text(
                        text = Strings.forcedRunActive,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    // 提示文字
                    if (allowEmergencyExit && !emergencyPassword.isNullOrEmpty()) {
                        Text(
                            text = Strings.tapToExit,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }

    // 密码输入对话框
    if (showPasswordDialog) {
        EmergencyExitPasswordDialog(
            correctPassword = emergencyPassword ?: "",
            onDismiss = {
                showPasswordDialog = false
                passwordError = false
            },
            onPasswordCorrect = {
                showPasswordDialog = false
                onEmergencyExit()
            },
            onPasswordError = {
                passwordError = true
            }
        )
    }
}

/**
 * 紧急退出密码对话框
 */
@Composable
private fun EmergencyExitPasswordDialog(
    correctPassword: String,
    onDismiss: () -> Unit,
    onPasswordCorrect: () -> Unit,
    onPasswordError: () -> Unit
) {
    var inputPassword by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var attempts by remember { mutableIntStateOf(0) }
    val maxAttempts = 5

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = Strings.enterExitPassword,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = Strings.exitPasswordPrompt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = inputPassword,
                    onValueChange = {
                        inputPassword = it
                        showError = false
                    },
                    label = { Text(Strings.passwordLabel) },
                    placeholder = { Text(Strings.passwordPlaceholder) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (inputPassword == correctPassword) {
                                onPasswordCorrect()
                            } else {
                                attempts++
                                showError = true
                                onPasswordError()
                                inputPassword = ""
                            }
                        }
                    ),
                    isError = showError,
                    supportingText = if (showError) {
                        {
                            Text(
                                text = Strings.passwordAttemptsRemaining.format(maxAttempts - attempts),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (attempts >= maxAttempts) {
                    Text(
                        text = Strings.tooManyPasswordAttempts,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (inputPassword == correctPassword) {
                        onPasswordCorrect()
                    } else {
                        attempts++
                        showError = true
                        onPasswordError()
                        inputPassword = ""
                    }
                },
                enabled = inputPassword.isNotEmpty() && attempts < maxAttempts
            ) {
                Text(Strings.confirmExit)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.btnCancel)
            }
        }
    )
}
