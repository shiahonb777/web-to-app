package com.webtoapp.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Switch
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView

/**
 * MD3 Switch + 弹簧物理引擎 + 按压拉伸
 *
 * 视觉上完全是 Material Design 3 Switch，
 * 交互时增加：
 *   - 按压横向拉伸 + 纵向压缩（液态挤压感）
 *   - 松手弹簧过冲回弹
 *   - 触觉反馈
 */
@Composable
fun PremiumSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // ── 弹簧物理：按压拉伸 ──
    // 按下时横向微扩 + 纵向微缩 → 液态挤压感
    val stretchX by animateFloatAsState(
        targetValue = if (isPressed && enabled) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "switchStretchX"
    )
    val stretchY by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "switchStretchY"
    )

    Switch(
        checked = checked,
        onCheckedChange = { newValue ->
            // 触觉反馈
            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
            onCheckedChange(newValue)
        },
        modifier = modifier.graphicsLayer {
            scaleX = stretchX
            scaleY = stretchY
        },
        enabled = enabled,
        interactionSource = interactionSource
    )
}
