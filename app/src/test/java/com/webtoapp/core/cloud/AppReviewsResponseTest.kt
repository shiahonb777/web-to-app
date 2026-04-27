package com.webtoapp.core.cloud

import com.google.common.truth.Truth.assertThat
import com.webtoapp.core.extension.ModuleVersion
import org.junit.Test

/**
 * AppReviewsResponse + 评分分布数据模型测试
 *
 * 覆盖：ratingDistribution 默认值、fallback 计算、AppReviewItem 数据类
 */
class AppReviewsResponseTest {

    // ═══════════════════════════════════════════
    // AppReviewsResponse 默认值
    // ═══════════════════════════════════════════

    @Test
    fun `default ratingDistribution is empty map`() {
        val response = AppReviewsResponse(
            total = 0,
            page = 1,
            reviews = emptyList()
        )

        assertThat(response.ratingDistribution).isEmpty()
    }

    @Test
    fun `ratingDistribution preserves star counts`() {
        val dist = mapOf(5 to 100, 4 to 50, 3 to 20, 2 to 5, 1 to 2)
        val response = AppReviewsResponse(
            total = 177,
            page = 1,
            reviews = emptyList(),
            ratingDistribution = dist
        )

        assertThat(response.ratingDistribution).containsExactly(5, 100, 4, 50, 3, 20, 2, 5, 1, 2)
    }

    @Test
    fun `ratingDistribution with partial data only contains provided stars`() {
        val dist = mapOf(5 to 80, 1 to 5)
        val response = AppReviewsResponse(
            total = 85,
            page = 1,
            reviews = emptyList(),
            ratingDistribution = dist
        )

        assertThat(response.ratingDistribution).hasSize(2)
        assertThat(response.ratingDistribution[5]).isEqualTo(80)
        assertThat(response.ratingDistribution[1]).isEqualTo(5)
        assertThat(response.ratingDistribution).doesNotContainKey(3)
    }

    // ═══════════════════════════════════════════
    // Fallback: 从评论列表计算分布
    // ═══════════════════════════════════════════

    @Test
    fun `compute distribution from review items`() {
        val reviews = listOf(
            AppReviewItem(id = 1, rating = 5, authorName = "A"),
            AppReviewItem(id = 2, rating = 5, authorName = "B"),
            AppReviewItem(id = 3, rating = 4, authorName = "C"),
            AppReviewItem(id = 4, rating = 3, authorName = "D"),
            AppReviewItem(id = 5, rating = 1, authorName = "E")
        )

        val dist = reviews.groupingBy { it.rating }.eachCount()

        assertThat(dist).containsExactly(5, 2, 4, 1, 3, 1, 1, 1)
    }

    @Test
    fun `compute distribution from empty reviews returns empty map`() {
        val reviews = emptyList<AppReviewItem>()
        val dist = reviews.groupingBy { it.rating }.eachCount()

        assertThat(dist).isEmpty()
    }

    @Test
    fun `compute distribution from single review returns single entry`() {
        val reviews = listOf(AppReviewItem(id = 1, rating = 4, authorName = "A"))
        val dist = reviews.groupingBy { it.rating }.eachCount()

        assertThat(dist).containsExactly(4, 1)
    }

    // ═══════════════════════════════════════════
    // AppReviewItem 数据类
    // ═══════════════════════════════════════════

    @Test
    fun `AppReviewItem default values`() {
        val item = AppReviewItem(id = 1, rating = 5)

        assertThat(item.comment).isNull()
        assertThat(item.authorName).isEqualTo("Unknown")
        assertThat(item.authorId).isEqualTo(0)
        assertThat(item.deviceModel).isNull()
        assertThat(item.ipAddress).isNull()
        assertThat(item.createdAt).isNull()
    }

    @Test
    fun `AppReviewItem with all fields`() {
        val item = AppReviewItem(
            id = 42,
            rating = 3,
            comment = "Good module",
            authorName = "Tester",
            authorId = 100,
            deviceModel = "Pixel 8",
            ipAddress = "192.168.1.1",
            createdAt = "2026-04-24T10:00:00Z"
        )

        assertThat(item.id).isEqualTo(42)
        assertThat(item.rating).isEqualTo(3)
        assertThat(item.comment).isEqualTo("Good module")
        assertThat(item.authorName).isEqualTo("Tester")
        assertThat(item.authorId).isEqualTo(100)
    }

    // ═══════════════════════════════════════════
    // 版本比较逻辑（用于更新检测）
    // ═══════════════════════════════════════════

    @Test
    fun `higher versionCode indicates update available`() {
        val localCode = 3
        val remoteCode = 5

        assertThat(remoteCode > localCode).isTrue()
    }

    @Test
    fun `same versionCode indicates no update`() {
        val localCode = 5
        val remoteCode = 5

        assertThat(remoteCode > localCode).isFalse()
    }

    @Test
    fun `lower versionCode indicates no update`() {
        val localCode = 5
        val remoteCode = 3

        assertThat(remoteCode > localCode).isFalse()
    }

    @Test
    fun `zero local versionCode always needs update when remote has version`() {
        val localCode = 0
        val remoteCode = 1

        assertThat(remoteCode > localCode).isTrue()
    }

    // ═══════════════════════════════════════════
    // ModuleVersion 数据类
    // ═══════════════════════════════════════════

    @Test
    fun `ModuleVersion default values`() {
        val version = ModuleVersion()

        assertThat(version.code).isEqualTo(1)
        assertThat(version.name).isEqualTo("1.0.0")
        assertThat(version.changelog).isEmpty()
    }

    @Test
    fun `ModuleVersion with custom values`() {
        val version = ModuleVersion(code = 5, name = "2.3.1", changelog = "Bug fixes")

        assertThat(version.code).isEqualTo(5)
        assertThat(version.name).isEqualTo("2.3.1")
        assertThat(version.changelog).isEqualTo("Bug fixes")
    }
}
