package com.webtoapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.RandomAppNameGenerator
import com.webtoapp.core.i18n.Strings

/**
 * 带随机按钮的应用名称输入框
 * 
 * @param value 当前输入值
 * @param onValueChange 值变化回调
 * @param modifier 修饰符
 * @param placeholder 占位文字（可选，默认使用 Strings.inputAppName）
 * @param imeAction 键盘动作（可选，默认 ImeAction.Next）
 */
@Composable
fun AppNameTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    imeAction: ImeAction = ImeAction.Next
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(Strings.labelAppName) },
        placeholder = { Text(placeholder ?: Strings.inputAppName) },
        leadingIcon = { Icon(Icons.Outlined.Badge, null) },
        trailingIcon = {
            IconButton(
                onClick = {
                    onValueChange(RandomAppNameGenerator.generate())
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Casino,
                    contentDescription = Strings.randomNameTooltip,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(imeAction = imeAction)
    )
}

/**
 * 简化版应用名称输入框（无前导图标）
 */
@Composable
fun AppNameTextFieldSimple(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(Strings.labelAppName) },
        placeholder = { Text(placeholder ?: Strings.inputAppName) },
        trailingIcon = {
            IconButton(
                onClick = {
                    onValueChange(RandomAppNameGenerator.generate())
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Casino,
                    contentDescription = Strings.randomNameTooltip,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}
