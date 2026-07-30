package com.majortomman.school.course

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Grade7Semester1PageContractSourcePolicyTest {
    @Test
    fun verifierKeepsPrintedPagesSeparateFromPdfIndices() {
        val source = repositoryFile("tools/course_quality/verify_pep_math_7_1_pages.py")
            .readText(Charsets.UTF_8)

        assertTrue("七上 PDF 必须保持 202 页契约", "EXPECTED_PDF_PAGE_COUNT = 202" in source)
        assertTrue("教材正文前有 7 页前置内容", "EXPECTED_PAGE_INDEX_OFFSET = 7" in source)
        assertTrue("sourcePage 应覆盖印刷页 1～195", "EXPECTED_PRINTED_PAGE_MIN = 1" in source)
        assertTrue("sourcePage 应覆盖印刷页 1～195", "EXPECTED_PRINTED_PAGE_MAX = 195" in source)
        assertTrue("第一章章首页应为印刷页1", "SectionPageContract(r\"^第一章\", 1, 1)" in source)
        assertTrue("第六章小结应覆盖印刷页184～195", "6: (184, 195)" in source)
        assertFalse(
            "校验器不得把 sourcePage 直接写成 PDF 索引 8～202",
            "EXPECTED_PRINTED_PAGE_MIN = 8" in source || "EXPECTED_PRINTED_PAGE_MAX = 202" in source,
        )
    }

    @Test
    fun genericGeneratedTitlesRemainAReleaseBacklog() {
        val source = repositoryFile("tools/course_quality/verify_pep_math_7_1_pages.py")
            .readText(Charsets.UTF_8)

        assertTrue("逐页精校必须识别自动生成的临时标题", "page.title.generic" in source)
        assertTrue("临时标题只能是 warning，不能被误判为已精校", "逐页精校时必须改为内容标题" in source)
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
