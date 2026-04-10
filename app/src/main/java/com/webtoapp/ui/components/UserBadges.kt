package com.webtoapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.webtoapp.core.cloud.TeamBadgeInfo
import com.webtoapp.core.i18n.Strings


// ═══════════════════════════════════════════
// Developer Badge
// ═══════════════════════════════════════════

/**
 * Gradient "Developer" badge — shown next to username when
 * the user has published ≥1 app/module to the store.
 */
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


// ═══════════════════════════════════════════
// Team Badge
// ═══════════════════════════════════════════

/**
 * Team identity badge — shows team name with role-based coloring.
 * Owner = gold, Admin = blue, Member = gray.
 * Clicking navigates to team detail.
 */
@Composable
fun TeamBadge(
    badge: TeamBadgeInfo,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (badge.role) {
        "owner" -> Color(0xFFFFB300).copy(alpha = 0.12f) to Color(0xFFFFB300)
        "admin" -> Color(0xFF2196F3).copy(alpha = 0.12f) to Color(0xFF2196F3)
        "editor" -> Color(0xFF4CAF50).copy(alpha = 0.12f) to Color(0xFF4CAF50)
        else -> MaterialTheme.colorScheme.surfaceContainerHighest to
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    }

    val roleLabel = when (badge.role) {
        "owner" -> Strings.badgeTeamOwner
        "admin" -> Strings.badgeTeamAdmin
        else -> Strings.badgeTeamMember
    }

    Surface(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        ),
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Team avatar mini
            if (badge.avatarUrl != null) {
                AsyncImage(
                    model = badge.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp).clip(CircleShape)
                )
            } else {
                Icon(
                    Icons.Filled.Groups, null,
                    modifier = Modifier.size(11.dp),
                    tint = textColor
                )
            }
            Text(
                badge.name,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1
            )
            // Role indicator dot
            Surface(
                shape = CircleShape,
                color = textColor.copy(alpha = 0.6f),
                modifier = Modifier.size(3.dp)
            ) {}
            Text(
                roleLabel,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = textColor.copy(alpha = 0.7f)
            )
        }
    }
}


// ═══════════════════════════════════════════
// Combined Title Badges Row
// ═══════════════════════════════════════════

/**
 * Displays developer badge + team badges inline.
 * Use this next to any username display.
 *
 * @param isDeveloper Whether the user has published ≥1 module
 * @param teamBadges List of team memberships
 * @param onTeamClick Callback when a team badge is tapped
 */
@Composable
fun UserTitleBadges(
    isDeveloper: Boolean,
    teamBadges: List<TeamBadgeInfo>,
    onTeamClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (!isDeveloper && teamBadges.isEmpty()) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isDeveloper) {
            DeveloperBadge()
        }
        teamBadges.forEach { badge ->
            TeamBadge(
                badge = badge,
                onClick = onTeamClick?.let { { it(badge.id) } }
            )
        }
    }
}
