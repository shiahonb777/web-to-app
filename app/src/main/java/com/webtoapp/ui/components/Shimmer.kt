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

/** Lightweight shimmer effect using Compose animation — no extra dependencies. */
@Composable
fun ShimmerBrush(target: @Composable (brush: Brush) -> Unit) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            Color(0x1A888888),
            Color(0x33888888),
            Color(0x1A888888),
        ),
        start = Offset(progress * 600f, 0f),
        end = Offset(progress * 600f + 300f, 0f)
    )
    target(brush)
}

/** Skeleton placeholder for a post card during loading. */
@Composable
fun PostCardSkeleton(modifier: Modifier = Modifier) {
    ShimmerBrush { brush ->
        Row(
            modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Avatar
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(brush)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                // Username line
                Box(
                    modifier = Modifier.width(100.dp).height(14.dp)
                        .clip(RoundedCornerShape(4.dp)).background(brush)
                )
                Spacer(Modifier.height(6.dp))
                // Subtitle line
                Box(
                    modifier = Modifier.width(160.dp).height(10.dp)
                        .clip(RoundedCornerShape(4.dp)).background(brush)
                )
                Spacer(Modifier.height(10.dp))
                // Content line 1
                Box(
                    modifier = Modifier.fillMaxWidth().height(12.dp)
                        .clip(RoundedCornerShape(4.dp)).background(brush)
                )
                Spacer(Modifier.height(6.dp))
                // Content line 2
                Box(
                    modifier = Modifier.fillMaxWidth(0.7f).height(12.dp)
                        .clip(RoundedCornerShape(4.dp)).background(brush)
                )
                Spacer(Modifier.height(10.dp))
                // Action row
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Box(Modifier.width(32.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                    Box(Modifier.width(32.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                    Box(Modifier.width(32.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                }
            }
        }
    }
}

/** Show N skeleton post cards. */
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
