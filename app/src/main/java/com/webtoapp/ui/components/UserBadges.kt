package com.webtoapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webtoapp.core.cloud.SubscriptionTier
import com.webtoapp.core.i18n.Strings





@Composable
fun DeveloperBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF6C5CE7),
                            Color(0xFF00B4D8)
                        )
                    ),
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    Icons.Filled.Code, null,
                    modifier = Modifier.size(11.dp),
                    tint = Color.White
                )
                Text(
                    Strings.badgeDeveloper,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}






@Composable
private fun DiamondIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val path = Path().apply {
            moveTo(cx, 0f)
            lineTo(w, cy)
            lineTo(cx, h)
            lineTo(0f, cy)
            close()
        }
        drawPath(path, Color.White)
    }
}


@Composable
private fun LightningIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.7f, 0f)
            lineTo(w * 0.25f, h * 0.5f)
            lineTo(w * 0.5f, h * 0.5f)
            lineTo(w * 0.3f, h)
            lineTo(w * 0.75f, h * 0.5f)
            lineTo(w * 0.5f, h * 0.5f)
            close()
        }
        drawPath(path, Color.White)
    }
}


@Composable
private fun InfinityIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cy = h / 2f
        val rx = w * 0.3f
        val ry = h * 0.4f
        val cx1 = w * 0.3f
        val cx2 = w * 0.7f
        val path = Path().apply {

            moveTo(w * 0.5f, cy - ry * 0.15f)

            cubicTo(cx2 + rx * 0.6f, cy - ry * 1.3f, cx2 + rx * 1.1f, cy + ry * 0.5f, cx2, cy + ry * 0.7f)

            cubicTo(cx2 - rx * 0.5f, cy + ry * 1.1f, w * 0.5f, cy + ry * 0.5f, w * 0.5f, cy + ry * 0.15f)

            cubicTo(cx1 - rx * 0.6f, cy + ry * 1.3f, cx1 - rx * 1.1f, cy - ry * 0.5f, cx1, cy - ry * 0.7f)

            cubicTo(cx1 + rx * 0.5f, cy - ry * 1.1f, w * 0.5f, cy - ry * 0.5f, w * 0.5f, cy - ry * 0.15f)
            close()
        }
        drawPath(path, Color.White)
    }
}






@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF2196F3), Color(0xFF42A5F5))
                    ),
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                DiamondIcon(Modifier.size(11.dp))
                Text(Strings.badgePro, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = Color.White, letterSpacing = 0.3.sp)
            }
        }
    }
}


@Composable
fun UltraBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFFFB300), Color(0xFFFF6D00))
                    ),
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                LightningIcon(Modifier.size(11.dp))
                Text(Strings.badgeUltra, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = Color.White, letterSpacing = 0.3.sp)
            }
        }
    }
}


@Composable
fun LifetimeBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFE91E63),
                            Color(0xFF9C27B0),
                            Color(0xFF2196F3),
                            Color(0xFF4CAF50),
                            Color(0xFFFFB300)
                        )
                    ),
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                InfinityIcon(Modifier.size(11.dp))
                Text(Strings.badgeLifetime, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = Color.White, letterSpacing = 0.3.sp)
            }
        }
    }
}













@Composable
fun UserTitleBadges(
    isDeveloper: Boolean = false,
    subscriptionTier: String = SubscriptionTier.FREE,
    modifier: Modifier = Modifier
) {
    if (!isDeveloper && subscriptionTier == SubscriptionTier.FREE) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        when (subscriptionTier) {
            SubscriptionTier.LIFETIME -> LifetimeBadge()
            SubscriptionTier.ULTRA -> UltraBadge()
            SubscriptionTier.PRO -> ProBadge()
        }
        if (isDeveloper) DeveloperBadge()
    }
}
