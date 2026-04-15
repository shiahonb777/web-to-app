package com.webtoapp.core.wordpress

import android.content.Context
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.core.sample.SampleProjectExtractor
import com.webtoapp.core.sample.TypedSampleProject

/**
 * Note: brief English comment.
 */
object WordPressSampleManager {
    
    fun getSampleProjects(): List<TypedSampleProject> {
        val suffix = SampleProjectExtractor.getLanguageSuffix()
        return listOf(
            TypedSampleProject(
                id = "wp-blog$suffix",
                name = AppStringsProvider.current().sampleWpBlogName,
                description = AppStringsProvider.current().sampleWpBlogDesc,
                frameworkName = "WordPress",
                icon = "📝",
                tags = listOf("WP 6.4", AppStringsProvider.current().sampleTagBlog, AppStringsProvider.current().sampleTagSqlite),
                brandColor = 0xFF21759B
            ),
            TypedSampleProject(
                id = "wp-woocommerce$suffix",
                name = AppStringsProvider.current().sampleWpWooName,
                description = AppStringsProvider.current().sampleWpWooDesc,
                frameworkName = "WooCommerce",
                icon = "🛒",
                tags = listOf("WooCommerce", AppStringsProvider.current().sampleTagEcommerce, AppStringsProvider.current().sampleTagSqlite),
                brandColor = 0xFF96588A
            ),
            TypedSampleProject(
                id = "wp-portfolio$suffix",
                name = AppStringsProvider.current().sampleWpPortfolioName,
                description = AppStringsProvider.current().sampleWpPortfolioDesc,
                frameworkName = "WordPress",
                icon = "🎨",
                tags = listOf(AppStringsProvider.current().sampleTagPortfolio, AppStringsProvider.current().sampleTagResponsive, AppStringsProvider.current().sampleTagSqlite),
                brandColor = 0xFF0073AA
            )
        )
    }
    
    suspend fun extractSampleProject(
        context: Context,
        projectId: String
    ): Result<String> = SampleProjectExtractor.extractSampleProject(context, projectId)
}
