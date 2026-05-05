package com.webtoapp.core.pwa





data class PwaManifest(
    val name: String? = null,
    val shortName: String? = null,
    val startUrl: String? = null,
    val scope: String? = null,
    val display: String? = null,
    val themeColor: String? = null,
    val backgroundColor: String? = null,
    val icons: List<PwaIcon> = emptyList(),
    val orientation: String? = null,
    val description: String? = null,
    val lang: String? = null,
    val dir: String? = null
)




data class PwaIcon(
    val src: String,
    val sizes: String? = null,
    val type: String? = null,
    val purpose: String? = null
) {




    val maxSizePixels: Int
        get() {
            if (sizes.isNullOrBlank() || sizes == "any") return Int.MAX_VALUE
            return sizes.split(" ")
                .mapNotNull { sizeStr ->
                    sizeStr.split("x", "X").firstOrNull()?.toIntOrNull()
                }
                .maxOrNull() ?: 0
        }
}




data class PwaAnalysisResult(

    val isPwa: Boolean = false,

    val suggestedName: String? = null,

    val suggestedIconUrl: String? = null,

    val suggestedThemeColor: String? = null,

    val suggestedBackgroundColor: String? = null,

    val suggestedDisplay: String? = null,

    val suggestedOrientation: String? = null,

    val startUrl: String? = null,

    val scope: String? = null,

    val manifest: PwaManifest? = null,

    val source: PwaDataSource = PwaDataSource.NONE,

    val errorMessage: String? = null
)




enum class PwaDataSource {
    MANIFEST,
    META_TAGS,
    NONE
}




sealed class PwaAnalysisState {
    data object Idle : PwaAnalysisState()
    data object Analyzing : PwaAnalysisState()
    data class Success(val result: PwaAnalysisResult) : PwaAnalysisState()
    data class Error(val message: String) : PwaAnalysisState()
}
