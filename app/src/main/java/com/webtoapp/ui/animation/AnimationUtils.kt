package com.webtoapp.ui.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay













@Composable
fun StaggeredAnimatedItem(
    index: Int,
    modifier: Modifier = Modifier,
    staggerDelayMs: Long = 50L,
    slideOffsetDp: Int = 40,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index * staggerDelayMs)
        visible = true
    }

    val density = LocalDensity.current
    val slideOffsetPx = with(density) { slideOffsetDp.dp.roundToPx() }


    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = spring(
                dampingRatio = 0.85f,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + slideInVertically(
            initialOffsetY = { slideOffsetPx },
            animationSpec = spring(
                dampingRatio = 0.7f,
                stiffness = Spring.StiffnessMediumLow
            )
        ),
        modifier = modifier
    ) {
        content()
    }
}







@Composable
fun Modifier.breathingFloat(
    floatAmountDp: Float = 6f,
    rotationDegrees: Float = 2f,
    durationMs: Int = 3000
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")

    val translationY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = floatAmountDp,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = -rotationDegrees,
        targetValue = rotationDegrees,
        animationSpec = infiniteRepeatable(
            animation = tween((durationMs * 1.3f).toInt(), easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    return this.graphicsLayer {
        this.translationY = -translationY * density
        this.rotationZ = rotation
        this.scaleX = scale
        this.scaleY = scale
    }
}







@Composable
fun AnimatedDialogContent(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dialogScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "dialogAlpha"
    )

    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
    ) {
        content()
    }
}






val SnackbarEnterTransition: EnterTransition = slideInVertically(
    initialOffsetY = { it },
    animationSpec = spring(
        dampingRatio = 0.7f,
        stiffness = Spring.StiffnessMediumLow
    )
) + fadeIn(
    animationSpec = tween(200)
)

val SnackbarExitTransition: ExitTransition = slideOutHorizontally(
    targetOffsetX = { it },
    animationSpec = tween(250, easing = FastOutSlowInEasing)
) + fadeOut(
    animationSpec = tween(200)
)







fun tabSlideDirection(previousTab: Int, currentTab: Int): Int {
    return if (currentTab > previousTab) 1 else -1
}




fun tabEnterTransition(direction: Int): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> direction * fullWidth / 4 },
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    ) + fadeIn(
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    )
}




fun tabExitTransition(direction: Int): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { fullWidth -> -direction * fullWidth / 4 },
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    ) + fadeOut(
        animationSpec = tween(250, easing = FastOutSlowInEasing)
    )
}








val CardExpandTransition: EnterTransition = expandVertically(
    animationSpec = spring(
        dampingRatio = 0.82f,
        stiffness = 350f
    ),
    expandFrom = androidx.compose.ui.Alignment.Top,
    clip = true
)

val CardCollapseTransition: ExitTransition = shrinkVertically(

    animationSpec = tween(
        durationMillis = 280,
        easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
    ),
    shrinkTowards = androidx.compose.ui.Alignment.Top,
    clip = true
)






data class RippleAnimState(
    val isActive: Boolean = false,
    val progress: Float = 0f
)

@Composable
fun rememberRippleAnimatable(): Animatable<Float, AnimationVector1D> {
    return remember { Animatable(0f) }
}
