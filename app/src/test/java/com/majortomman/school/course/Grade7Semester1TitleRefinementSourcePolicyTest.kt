package com.majortomman.school.course

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Grade7Semester1TitleRefinementSourcePolicyTest {
    @Test
    fun titleRefinerNeverMarksPagesAsManuallyVerified() {
        val source = repositoryFile("tools/course_quality/refine_pep_math_7_1_titles.py")
            .readText(Charsets.UTF_8)

        assertTrue("必须识别教材自动生成标题", "GENERIC_TITLE" in source)
        assertTrue(
            "自动标题只能标记为待人工复核",
            "content-inference-pending-manual-review" in source,
        )
        assertFalse(
            "机械标题工具不得把页面标成 verified",
            "page[\"reviewStatus\"] = \"verified\"" in source,
        )
    }

    @Test
    fun printedPageRepairPreservesPrintedPageCoordinateSystem() {
        val source = repositoryFile("tools/course_quality/refine_pep_math_7_1_titles.py")
            .readText(Charsets.UTF_8)

        assertTrue("印刷页必须从1开始", "PRINTED_PAGE_MIN = 1" in source)
        assertTrue("印刷页必须到195结束", "PRINTED_PAGE_MAX = 195" in source)
        assertTrue("PDF 前置页偏移必须保持7", "pageIndexOffset\") != 7" in source)
        assertTrue(
            "补页必须标注待人工复核原因",
            "fill-missing-printed-page-pending-manual-review" in source,
        )
        assertTrue(
            "补页时必须验证前后页顺序",
            "previous_value <= missing <= next_value" in source,
        )
    }

    @Test
    fun titleInferenceUsesTeachingContentInsteadOfPageNumbers() {
        val source = repositoryFile("tools/course_quality/refine_pep_math_7_1_titles.py")
            .readText(Charsets.UTF_8)

        listOf("例题解析", "课堂练习", "探究与思考", "概念与定义", "性质与法则").forEach {
            label -> assertTrue("缺少教学内容标题类型：$label", label in source)
        }
        assertTrue("标题应读取页面 block 内容", "page_text(page)" in source)
        assertTrue("标题应保留小节上下文", "section.get(\"title\"" in source)
    }

    private fun repositoryFile(relative: String): File {
        var current = File(System.getProperty("user.dir"))
        repeat(8) {
            val candidate = File(current, relative)
            if (candidate.isFile) return candidate
            current = current.parentFile ?: return@repeat
        }
        error("无法定位仓库文件：$relative")
    }
}
