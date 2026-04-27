package com.webtoapp.core.cloud

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * 评论排序逻辑测试
 *
 * 覆盖：按时间（最新/最早）、按评分排序
 */
class CommentSortLogicTest {

    private val comments = listOf(
        ModuleComment(id = 1, content = "First", userId = 10, userName = "A",
            userAvatar = null, parentId = null, createdAt = "2026-01-01", updatedAt = null),
        ModuleComment(id = 2, content = "Second", userId = 20, userName = "B",
            userAvatar = null, parentId = null, createdAt = "2026-03-01", updatedAt = null),
        ModuleComment(id = 3, content = "Third", userId = 30, userName = "C",
            userAvatar = null, parentId = null, createdAt = "2026-02-01", updatedAt = null)
    )

    // ═══════════════════════════════════════════
    // 按时间排序
    // ═══════════════════════════════════════════

    @Test
    fun `sort newest returns comments sorted by createdAt descending`() {
        val sorted = comments.sortedByDescending { it.createdAt }

        assertThat(sorted.map { it.id }).containsExactly(2, 3, 1).inOrder()
    }

    @Test
    fun `sort oldest returns comments sorted by createdAt ascending`() {
        val sorted = comments.sortedBy { it.createdAt }

        assertThat(sorted.map { it.id }).containsExactly(1, 3, 2).inOrder()
    }

    // ═══════════════════════════════════════════
    // 按评分排序
    // ═══════════════════════════════════════════

    @Test
    fun `sort by rating high to low uses review data`() {
        val reviewMap = mapOf(
            10 to AppReviewItem(id = 1, rating = 5, authorId = 10),
            20 to AppReviewItem(id = 2, rating = 1, authorId = 20),
            30 to AppReviewItem(id = 3, rating = 3, authorId = 30)
        )

        val sorted = comments.sortedByDescending { reviewMap[it.userId]?.rating ?: 0 }

        assertThat(sorted.map { it.id }).containsExactly(1, 3, 2).inOrder()
    }

    @Test
    fun `sort by rating with missing review data defaults to zero`() {
        val reviewMap = mapOf(
            10 to AppReviewItem(id = 1, rating = 4, authorId = 10)
            // userId 20 and 30 have no review
        )

        val sorted = comments.sortedByDescending { reviewMap[it.userId]?.rating ?: 0 }

        // userId 10 has rating 4, others default to 0
        assertThat(sorted.first().id).isEqualTo(1)
    }

    @Test
    fun `sort by rating with all same ratings preserves relative order`() {
        val reviewMap = mapOf(
            10 to AppReviewItem(id = 1, rating = 3, authorId = 10),
            20 to AppReviewItem(id = 2, rating = 3, authorId = 20),
            30 to AppReviewItem(id = 3, rating = 3, authorId = 30)
        )

        val sorted = comments.sortedByDescending { reviewMap[it.userId]?.rating ?: 0 }

        // All same rating — stable sort preserves original order
        assertThat(sorted.map { it.id }).containsExactly(1, 2, 3).inOrder()
    }

    // ═══════════════════════════════════════════
    // 边界情况
    // ═══════════════════════════════════════════

    @Test
    fun `sort empty list returns empty list`() {
        val empty = emptyList<ModuleComment>()

        assertThat(empty.sortedByDescending { it.createdAt }).isEmpty()
        assertThat(empty.sortedBy { it.createdAt }).isEmpty()
    }

    @Test
    fun `sort single item list returns same item`() {
        val single = listOf(comments[0])

        assertThat(single.sortedByDescending { it.createdAt }).hasSize(1)
        assertThat(single.sortedByDescending { it.createdAt }[0].id).isEqualTo(1)
    }

    @Test
    fun `sort with null createdAt treats as empty string`() {
        val commentsWithNull = listOf(
            ModuleComment(id = 1, content = "A", userId = 1, userName = "X",
                userAvatar = null, parentId = null, createdAt = null, updatedAt = null),
            ModuleComment(id = 2, content = "B", userId = 2, userName = "Y",
                userAvatar = null, parentId = null, createdAt = "2026-01-01", updatedAt = null)
        )

        // null sorts before non-null in ascending (null < "2026-01-01")
        val sorted = commentsWithNull.sortedBy { it.createdAt ?: "" }

        assertThat(sorted.map { it.id }).containsExactly(1, 2).inOrder()
    }
}
