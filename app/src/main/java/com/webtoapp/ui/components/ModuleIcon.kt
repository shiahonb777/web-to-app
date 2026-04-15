package com.webtoapp.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.webtoapp.util.SvgIconMapper

/**
 * unified moduleicon
 * 
 * support icon
 * 1. `drawable: xxx`- Android drawable( drawable: ic_ext_bewlycat)
 * 2. `asset: xxx`- assets directory file( asset: extensions/bewlycat/assets/icon- 512.
 * 3. - SvgIconMapper Material Icon( "extension", "shield", "🐱")
 */
@Composable
fun ModuleIcon(
    iconId: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    when {
        // 1. drawable: "drawable: ic_ext_bewlycat"
        iconId.startsWith("drawable:") -> {
            val resName = iconId.removePrefix("drawable:")
            val context = LocalContext.current
            val resId = remember(resName) {
                context.resources.getIdentifier(resName, "drawable", context.packageName)
            }
            if (resId != 0) {
                Icon(
                    painter = painterResource(id = resId),
                    contentDescription = contentDescription,
                    modifier = modifier,
                    tint = Color.Unspecified  // , tint
                )
            } else {
                // Fallback: drawable, defaulticon
                Icon(
                    SvgIconMapper.getIcon("extension"),
                    contentDescription = contentDescription,
                    modifier = modifier,
                    tint = tint
                )
            }
        }
        
        // 2. assets: "asset: extensions/bewlycat/assets/icon- 512. png"
        iconId.startsWith("asset:") -> {
            val assetPath = iconId.removePrefix("asset:")
            val context = LocalContext.current
            val bitmap = remember(assetPath) {
                try {
                    context.assets.open(assetPath).use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                } catch (e: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = contentDescription,
                    modifier = modifier,
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    SvgIconMapper.getIcon("extension"),
                    contentDescription = contentDescription,
                    modifier = modifier,
                    tint = tint
                )
            }
        }
        
        // 3. SvgIconMapper( Material Icon ID or emoji)
        else -> {
            Icon(
                SvgIconMapper.getIcon(iconId),
                contentDescription = contentDescription,
                modifier = modifier,
                tint = tint
            )
        }
    }
}
