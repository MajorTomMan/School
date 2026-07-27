package com.majortomman.school.learning.content

import com.majortomman.school.learning.course.CourseScene

@JvmInline
value class ContentAssetId(val value: String) {
    init {
        require(IDENTIFIER.matches(value)) { "assetId 格式无效：$value" }
    }

    override fun toString(): String = value

    private companion object {
        val IDENTIFIER = Regex("^[a-z][a-z0-9_-]{0,63}$")
    }
}

enum class LearningTextStyle {
    BODY,
    PROMPT,
    CAPTION,
    EXPLANATION,
}

/**
 * Shared immutable content used by assessment stems, choices and explanations.
 * Rendering remains APK-owned; course packages only select these declarative nodes.
 */
sealed interface LearningContent {
    data class Heading(
        val text: String,
    ) : LearningContent {
        init {
            require(text.isNotBlank()) { "heading text 不能为空" }
        }
    }

    data class Text(
        val text: String,
        val style: LearningTextStyle = LearningTextStyle.BODY,
    ) : LearningContent {
        init {
            require(text.isNotBlank()) { "text 不能为空" }
        }
    }

    data class Formula(
        val expression: String,
        val conditions: List<String> = emptyList(),
    ) : LearningContent {
        init {
            require(expression.isNotBlank()) { "formula expression 不能为空" }
            require(conditions.all(String::isNotBlank)) { "formula conditions 不能包含空字符串" }
        }
    }

    data class ItemList(
        val items: List<String>,
    ) : LearningContent {
        init {
            require(items.isNotEmpty()) { "list items 不能为空" }
            require(items.all(String::isNotBlank)) { "list items 不能包含空字符串" }
        }
    }

    data class Image(
        val assetId: ContentAssetId,
        val altText: String,
        val caption: String? = null,
    ) : LearningContent {
        init {
            require(altText.isNotBlank()) { "image altText 不能为空" }
            require(caption == null || caption.isNotBlank()) { "image caption 不能是空字符串" }
        }
    }

    data class Table(
        val columns: List<String>,
        val rows: List<List<String>>,
        val caption: String? = null,
        val sourceAssetId: ContentAssetId? = null,
    ) : LearningContent {
        init {
            require(columns.isNotEmpty()) { "table columns 不能为空" }
            require(columns.all(String::isNotBlank)) { "table columns 不能包含空字符串" }
            require(rows.isNotEmpty()) { "table rows 不能为空" }
            require(rows.all { it.size == columns.size }) { "table 每行单元格数量必须等于列数" }
            require(rows.flatten().all(String::isNotBlank)) { "table 单元格不能是空字符串" }
            require(caption == null || caption.isNotBlank()) { "table caption 不能是空字符串" }
        }
    }

    data class Scene(
        val scene: CourseScene,
    ) : LearningContent
}

fun LearningContent.referencedAssetIds(): Set<ContentAssetId> = when (this) {
    is LearningContent.Image -> setOf(assetId)
    is LearningContent.Table -> setOfNotNull(sourceAssetId)
    is LearningContent.Formula,
    is LearningContent.Heading,
    is LearningContent.ItemList,
    is LearningContent.Scene,
    is LearningContent.Text,
    -> emptySet()
}
