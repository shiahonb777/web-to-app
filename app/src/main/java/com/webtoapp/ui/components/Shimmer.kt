package com.webtoapp.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.HorizontalDivider


@Composable
fun ShimmerBrush(target: @Composable (brush: Brush) -> Unit) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )
    val isDark = com.webtoapp.ui.theme.LocalIsDarkTheme.current
    // Highlights slightly brighter than the surrounding surface so the shimmer
    // reads as a subtle sweep rather than a grey stripe. Values hand-picked
    // for both light and dark mode to sit just above the noise floor.
    val baseColor = if (isDark) Color(0x14FFFFFF) else Color(0x14000000)
    val peakColor = if (isDark) Color(0x2EFFFFFF) else Color(0x1F000000)
    val brush = Brush.linearGradient(
        colors = listOf(baseColor, peakColor, baseColor),
        start = Offset(progress * 600f, 0f),
        end = Offset(progress * 600f + 300f, 0f)
    )
    target(brush)
}


@Composable
fun PostCardSkeleton(modifier: Modifier = Modifier) {
    ShimmerBrush { brush ->
        Row(
            modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {

            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(brush)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {

                Box(
                    modifier = Modifier.width(100.dp).height(14.dp)
                        .clip(RoundedCornerShape(4.dp)).background(brush)
                )
                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier.width(160.dp).height(10.dp)
                        .clip(RoundedCornerShape(4.dp)).background(brush)
                )
                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier.fillMaxWidth().height(12.dp)
                        .clip(RoundedCornerShape(4.dp)).background(brush)
                )
                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(0.7f).height(12.dp)
                        .clip(RoundedCornerShape(4.dp)).background(brush)
                )
                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Box(Modifier.width(32.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                    Box(Modifier.width(32.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                    Box(Modifier.width(32.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                }
            }
        }
    }
}


@Composable
fun PostCardSkeletonList(count: Int = 4, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        repeat(count) { index ->
            PostCardSkeleton()
            if (index < count - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = Color.Transparent
                )
            }
        }
    }
}
