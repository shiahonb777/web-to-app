package com.webtoapp.data.model.webapp.config

import androidx.compose.runtime.Stable
import com.webtoapp.util.toFileSizeString

data class MediaConfig(
    val mediaPath: String,                         // Media file path
    val enableAudio: Boolean = true,               // Enable audio for video
    val loop: Boolean = true,                      // Loop playback (video)
    val autoPlay: Boolean = true,                  // Auto-play (video)
    val fillScreen: Boolean = true,                // Fill the screen
    val orientation: SplashOrientation = SplashOrientation.PORTRAIT, // Display orientation
    val backgroundColor: String = "#000000",       // Background color
    val keepScreenOn: Boolean = true               // Keep screen awake
)

@Stable
data class GalleryConfig(
    val items: List<GalleryItem> = emptyList(),
    val categories: List<GalleryCategory> = emptyList(),
    val playMode: GalleryPlayMode = GalleryPlayMode.SEQUENTIAL,
    val imageInterval: Int = 3,
    val loop: Boolean = true,
    val autoPlay: Boolean = false,
    val shuffleOnLoop: Boolean = false,
    val defaultView: GalleryViewMode = GalleryViewMode.GRID,
    val gridColumns: Int = 3,
    val sortOrder: GallerySortOrder = GallerySortOrder.CUSTOM,
    val backgroundColor: String = "#000000",
    val showThumbnailBar: Boolean = true,
    val showMediaInfo: Boolean = true,
    val orientation: SplashOrientation = SplashOrientation.PORTRAIT,
    val enableAudio: Boolean = true,
    val videoAutoNext: Boolean = true,
    val rememberPosition: Boolean = false
) {
    fun getItemsByCategory(categoryId: String?): List<GalleryItem> {
        return if (categoryId == null) {
            items
        } else {
            items.filter { it.categoryId == categoryId }
        }
    }
    
    fun getSortedItems(categoryId: String? = null): List<GalleryItem> {
        val filtered = getItemsByCategory(categoryId)
        return when (sortOrder) {
            GallerySortOrder.CUSTOM -> filtered.sortedBy { it.sortIndex }
            GallerySortOrder.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            GallerySortOrder.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            GallerySortOrder.DATE_ASC -> filtered.sortedBy { it.createdAt }
            GallerySortOrder.DATE_DESC -> filtered.sortedByDescending { it.createdAt }
            GallerySortOrder.TYPE -> filtered.sortedBy { it.type.ordinal }
        }
    }
    
    val imageCount: Int get() = items.count { it.type == GalleryItemType.IMAGE }
    val videoCount: Int get() = items.count { it.type == GalleryItemType.VIDEO }
    val totalCount: Int get() = items.size
}

data class GalleryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val path: String,
    val type: GalleryItemType,
    val name: String = "",
    val categoryId: String? = null,
    val duration: Long = 0,
    val thumbnailPath: String? = null,
    val sortIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val width: Int = 0,
    val height: Int = 0,
    val fileSize: Long = 0
) {
    val formattedDuration: String
        get() {
            if (type != GalleryItemType.VIDEO || duration <= 0) return ""
            val seconds = (duration / 1000) % 60
            val minutes = (duration / 1000 / 60) % 60
            val hours = duration / 1000 / 60 / 60
            return if (hours > 0) {
                String.format(java.util.Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, seconds)
            }
        }
    
    val formattedFileSize: String
        get() = if (fileSize <= 0) "" else fileSize.toFileSizeString()
}

data class GalleryCategory(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val icon: String = "folder",
    val color: String = "#6200EE",
    val sortIndex: Int = 0
)

enum class GalleryItemType {
    IMAGE,
    VIDEO
}

enum class GalleryPlayMode {
    SEQUENTIAL,
    SHUFFLE,
    SINGLE_LOOP
}

enum class GalleryViewMode {
    GRID,
    LIST,
    TIMELINE
}

enum class GallerySortOrder {
    CUSTOM,
    NAME_ASC,
    NAME_DESC,
    DATE_ASC,
    DATE_DESC,
    TYPE
}

enum class BgmPlayMode {
    LOOP,
    SEQUENTIAL,
    SHUFFLE
}

enum class BgmTag {
    PURE_MUSIC,
    POP,
    ROCK,
    CLASSICAL,
    JAZZ,
    ELECTRONIC,
    FOLK,
    CHINESE_STYLE,
    ANIME,
    GAME,
    MOVIE,
    HEALING,
    EXCITING,
    SAD,
    ROMANTIC,
    RELAXING,
    WORKOUT,
    SLEEP,
    STUDY,
    OTHER;
    
    val displayName: String get() = when (this) {
        PURE_MUSIC -> com.webtoapp.core.i18n.Strings.bgmTagPureMusic
        POP -> com.webtoapp.core.i18n.Strings.bgmTagPop
        ROCK -> com.webtoapp.core.i18n.Strings.bgmTagRock
        CLASSICAL -> com.webtoapp.core.i18n.Strings.bgmTagClassical
        JAZZ -> com.webtoapp.core.i18n.Strings.bgmTagJazz
        ELECTRONIC -> com.webtoapp.core.i18n.Strings.bgmTagElectronic
        FOLK -> com.webtoapp.core.i18n.Strings.bgmTagFolk
        CHINESE_STYLE -> com.webtoapp.core.i18n.Strings.bgmTagChineseStyle
        ANIME -> com.webtoapp.core.i18n.Strings.bgmTagAnime
        GAME -> com.webtoapp.core.i18n.Strings.bgmTagGame
        MOVIE -> com.webtoapp.core.i18n.Strings.bgmTagMovie
        HEALING -> com.webtoapp.core.i18n.Strings.bgmTagHealing
        EXCITING -> com.webtoapp.core.i18n.Strings.bgmTagExciting
        SAD -> com.webtoapp.core.i18n.Strings.bgmTagSad
        ROMANTIC -> com.webtoapp.core.i18n.Strings.bgmTagRomantic
        RELAXING -> com.webtoapp.core.i18n.Strings.bgmTagRelaxing
        WORKOUT -> com.webtoapp.core.i18n.Strings.bgmTagWorkout
        SLEEP -> com.webtoapp.core.i18n.Strings.bgmTagSleep
        STUDY -> com.webtoapp.core.i18n.Strings.bgmTagStudy
        OTHER -> com.webtoapp.core.i18n.Strings.bgmTagOther
    }
}

data class LrcLine(
    val startTime: Long,
    val endTime: Long,
    val text: String,
    val translation: String? = null
)

data class LrcData(
    val lines: List<LrcLine> = emptyList(),
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val language: String? = null
)

data class LrcTheme(
    val id: String,
    val name: String,
    val fontFamily: String = "default",
    val fontSize: Float = 18f,
    val textColor: String = "#FFFFFF",
    val highlightColor: String = "#FFD700",
    val backgroundColor: String = "#80000000",
    val strokeColor: String? = null,
    val strokeWidth: Float = 0f,
    val shadowEnabled: Boolean = true,
    val animationType: LrcAnimationType = LrcAnimationType.FADE,
    val position: LrcPosition = LrcPosition.BOTTOM,
    val showTranslation: Boolean = true
)

enum class LrcAnimationType {
    NONE, FADE, SLIDE_UP, SLIDE_LEFT, SCALE, TYPEWRITER, KARAOKE;
    
    val displayName: String get() = when (this) {
        NONE -> com.webtoapp.core.i18n.Strings.lrcAnimNone
        FADE -> com.webtoapp.core.i18n.Strings.lrcAnimFade
        SLIDE_UP -> com.webtoapp.core.i18n.Strings.lrcAnimSlideUp
        SLIDE_LEFT -> com.webtoapp.core.i18n.Strings.lrcAnimSlideLeft
        SCALE -> com.webtoapp.core.i18n.Strings.lrcAnimScale
        TYPEWRITER -> com.webtoapp.core.i18n.Strings.lrcAnimTypewriter
        KARAOKE -> com.webtoapp.core.i18n.Strings.lrcAnimKaraoke
    }
}

enum class LrcPosition {
    TOP, CENTER, BOTTOM;
    
    val displayName: String get() = when (this) {
        TOP -> com.webtoapp.core.i18n.Strings.lrcPosTop
        CENTER -> com.webtoapp.core.i18n.Strings.lrcPosCenter
        BOTTOM -> com.webtoapp.core.i18n.Strings.lrcPosBottom
    }
}

data class BgmItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val path: String,
    val coverPath: String? = null,
    val isAsset: Boolean = false,
    val tags: List<BgmTag> = emptyList(),
    val sortOrder: Int = 0,
    val lrcData: LrcData? = null,
    val lrcPath: String? = null,
    val duration: Long = 0
)

data class BgmConfig(
    val playlist: List<BgmItem> = emptyList(),
    val playMode: BgmPlayMode = BgmPlayMode.LOOP,
    val volume: Float = 0.5f,
    val autoPlay: Boolean = true,
    val showLyrics: Boolean = true,
    val lrcTheme: LrcTheme? = null
)
