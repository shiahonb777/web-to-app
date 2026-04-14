package com.webtoapp.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Html
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Rocket
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.data.model.AppType

@Composable
fun FeatureChip(
    icon: ImageVector,
    label: String
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun AppTypeChip(appType: AppType) {
    val (icon, label, containerColor) = when (appType) {
        AppType.WEB -> Triple(
            Icons.Outlined.Public,
            AppStringsProvider.current().appTypeWeb,
            MaterialTheme.colorScheme.primaryContainer
        )
        AppType.IMAGE -> Triple(
            Icons.Outlined.Image,
            AppStringsProvider.current().appTypeImage,
            MaterialTheme.colorScheme.tertiaryContainer
        )
        AppType.VIDEO -> Triple(
            Icons.Outlined.VideoLibrary,
            AppStringsProvider.current().appTypeVideo,
            MaterialTheme.colorScheme.tertiaryContainer
        )
        AppType.HTML -> Triple(
            Icons.Outlined.Html,
            AppStringsProvider.current().appTypeHtml,
            MaterialTheme.colorScheme.secondaryContainer
        )
        AppType.GALLERY -> Triple(
            Icons.Outlined.PhotoLibrary,
            AppStringsProvider.current().appTypeGallery,
            MaterialTheme.colorScheme.tertiaryContainer
        )
        AppType.FRONTEND -> Triple(
            Icons.Outlined.Rocket,
            AppStringsProvider.current().appTypeFrontend,
            MaterialTheme.colorScheme.primaryContainer
        )
        AppType.WORDPRESS -> Triple(
            Icons.Outlined.Newspaper,
            AppStringsProvider.current().appTypeWordPress,
            MaterialTheme.colorScheme.primaryContainer
        )
        AppType.NODEJS_APP -> Triple(
            Icons.Outlined.Terminal,
            AppStringsProvider.current().appTypeNodeJs,
            MaterialTheme.colorScheme.secondaryContainer
        )
        AppType.PHP_APP -> Triple(
            Icons.Outlined.DataObject,
            AppStringsProvider.current().appTypePhp,
            MaterialTheme.colorScheme.secondaryContainer
        )
        AppType.PYTHON_APP -> Triple(
            Icons.Outlined.Psychology,
            AppStringsProvider.current().appTypePython,
            MaterialTheme.colorScheme.secondaryContainer
        )
        AppType.GO_APP -> Triple(
            Icons.Outlined.Speed,
            AppStringsProvider.current().appTypeGo,
            MaterialTheme.colorScheme.primaryContainer
        )
        AppType.MULTI_WEB -> Triple(
            Icons.Outlined.Language,
            AppStringsProvider.current().appTypeMultiWeb,
            MaterialTheme.colorScheme.primaryContainer
        )
    }

    val contentColor = when (appType) {
        AppType.WEB,
        AppType.FRONTEND,
        AppType.WORDPRESS,
        AppType.GO_APP,
        AppType.MULTI_WEB -> MaterialTheme.colorScheme.onPrimaryContainer
        AppType.IMAGE,
        AppType.VIDEO,
        AppType.GALLERY -> MaterialTheme.colorScheme.onTertiaryContainer
        AppType.HTML,
        AppType.NODEJS_APP,
        AppType.PHP_APP,
        AppType.PYTHON_APP -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}
