package com.webtoapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.webtoapp.core.i18n.RandomAppNameGenerator
import com.webtoapp.core.i18n.Strings

/**
 * button app input
 * 
 * @param value currentinput
 * @param onValueChange
 * @param modifier
 * @param placeholder( optional, default Strings. inputAppName)
 * @param imeAction keyboard( optional, default ImeAction. Next)
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
 * app input( icon)
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
