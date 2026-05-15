package com.webtoapp.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.webtoapp.core.i18n.RandomAppNameGenerator
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.design.WtaTextField

/**
 * App name input with a built-in dice button that fills the field with a
 * locale-aware random name. Uses [WtaTextField] so it inherits the filled
 * style, focus indicator, and haptic consistent with every other input in
 * the app.
 */
@Composable
fun AppNameTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    imeAction: ImeAction = ImeAction.Next
) {
    WtaTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = Strings.labelAppName,
        placeholder = placeholder ?: Strings.inputAppName,
        leadingIcon = Icons.Outlined.Badge,
        trailingIcon = {
            IconButton(
                onClick = { onValueChange(RandomAppNameGenerator.generate()) }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Casino,
                    contentDescription = Strings.randomNameTooltip,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = imeAction)
    )
}

@Composable
fun AppNameTextFieldSimple(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null
) {
    WtaTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = Strings.labelAppName,
        placeholder = placeholder ?: Strings.inputAppName,
        trailingIcon = {
            IconButton(
                onClick = { onValueChange(RandomAppNameGenerator.generate()) }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Casino,
                    contentDescription = Strings.randomNameTooltip,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        singleLine = true
    )
}
