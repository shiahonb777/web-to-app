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









@Composable
fun ModuleIcon(
    iconId: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    when {

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
                    tint = Color.Unspecified
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
