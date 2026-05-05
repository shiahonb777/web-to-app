package com.webtoapp.core.image

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.webtoapp.core.cloud.GitHubAccelerator
import com.webtoapp.core.i18n.Strings

data class CommunityImageSource(
    val primaryUrl: String?,
    val fallbacks: List<String> = emptyList()
)

private data class ResolvedCommunityImage(
    val primary: String?,
    val candidates: List<String>
)

@Composable
fun CommunityImage(
    url: String?,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    crossfade: Boolean = false,
    accelerate: Boolean = true,
) {
    CommunityImage(
        source = CommunityImageSource(primaryUrl = url),
        width = width,
        height = height,
        modifier = modifier,
        contentScale = contentScale,
        crossfade = crossfade,
        accelerate = accelerate,
    )
}

@Composable
fun CommunityImage(
    source: CommunityImageSource,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    crossfade: Boolean = false,
    accelerate: Boolean = true,
) {
    val context = LocalContext.current
    val resolved = remember(source, accelerate) { resolveCommunitySource(source, accelerate) }
    var requestIndex by remember(resolved.candidates) { mutableIntStateOf(0) }
    val currentUrl = resolved.candidates.getOrNull(requestIndex) ?: resolved.primary

    if (currentUrl.isNullOrBlank()) {
        CommunityImageFallback(
            modifier = modifier,
            retryEnabled = false,
            onRetry = null
        )
        return
    }

    val requestBuilder = ImageRequest.Builder(context)
        .data(currentUrl)
        .size(width.value.toInt(), height.value.toInt())
        .scale(Scale.FILL)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCacheKey(currentUrl)
        .diskCacheKey(currentUrl)

    if (crossfade) {
        requestBuilder.crossfade(true)
    }

    val imageLoader = remember(context) { OptimizedImageLoader.get(context) }

    SubcomposeAsyncImage(
        model = requestBuilder.build(),
        imageLoader = imageLoader,
        contentDescription = null,
        modifier = modifier,
        contentScale = contentScale,
        loading = {
            CommunityImagePlaceholder(modifier = Modifier.matchParentSize())
        },
        success = {
            SubcomposeAsyncImageContent()
        },
        error = {
            val canRetry = requestIndex < resolved.candidates.lastIndex
            CommunityImageFallback(
                modifier = Modifier.matchParentSize(),
                retryEnabled = true,
                onRetry = {
                    requestIndex = if (canRetry) requestIndex + 1 else 0
                }
            )
        }
    )
}

@Composable
fun CommunityMediaImage(
    urlGitee: String?,
    urlGithub: String?,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    crossfade: Boolean = false,
) {
    val primary = GitHubAccelerator.pickBestUrl(urlGitee, urlGithub)
    val fallbackUrls = buildList {
        if (!urlGitee.isNullOrBlank() && urlGitee != primary) add(urlGitee)
        if (!urlGithub.isNullOrBlank()) {
            addAll(GitHubAccelerator.accelerateWithFallbacks(urlGithub))
        }
    }.distinct().filter { it.isNotBlank() && it != primary }

    CommunityImage(
        source = CommunityImageSource(
            primaryUrl = primary,
            fallbacks = fallbackUrls
        ),
        width = width,
        height = height,
        modifier = modifier,
        contentScale = contentScale,
        crossfade = crossfade,
        accelerate = false,
    )
}

@Composable
fun CommunityAvatarImage(
    avatarUrl: String?,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    CommunityImage(
        url = avatarUrl,
        width = size,
        height = size,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        crossfade = true,
    )
}

@Composable
fun EcosystemAvatarImage(
    avatarUrl: String?,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
) = CommunityAvatarImage(
    avatarUrl = avatarUrl,
    size = size,
    modifier = modifier,
)

@Composable
private fun CommunityImagePlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.BrokenImage,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun CommunityImageFallback(
    modifier: Modifier = Modifier,
    retryEnabled: Boolean,
    onRetry: (() -> Unit)?
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(10.dp)
        ) {
            Icon(
                Icons.Outlined.BrokenImage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = Strings.imageLoadFailed,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (retryEnabled && onRetry != null) {
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = onRetry,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(Strings.retry, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private fun resolveCommunitySource(
    source: CommunityImageSource,
    accelerate: Boolean
): ResolvedCommunityImage {
    val primary = resolveUrl(source.primaryUrl, accelerate)
    val fallbackCandidates = buildList {
        source.primaryUrl?.let { original ->
            if (accelerate) addAll(GitHubAccelerator.accelerateWithFallbacks(original))
            else add(original)
        }
        source.fallbacks.forEach { fallback ->
            if (accelerate) addAll(GitHubAccelerator.accelerateWithFallbacks(fallback))
            else add(fallback)
        }
    }.distinct().filter { it.isNotBlank() }

    val candidates = buildList {
        if (!primary.isNullOrBlank()) add(primary)
        addAll(fallbackCandidates.filter { it != primary })
    }

    return ResolvedCommunityImage(
        primary = primary,
        candidates = candidates
    )
}

private fun resolveUrl(url: String?, accelerate: Boolean): String? {
    if (url.isNullOrBlank()) return null
    return if (accelerate) GitHubAccelerator.accelerate(url) ?: url else url
}
