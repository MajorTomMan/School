package com.majortomman.school.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiScaleSourcePolicyTest {
    @Test
    fun settingsTabsScrollInsteadOfWrappingLabels() {
        val shared = uiFile("SchoolUiSystem.kt").readText(Charsets.UTF_8)
        val settings = uiFile("MaterialSettingsScreen.kt").readText(Charsets.UTF_8)
        assertTrue("统一 Tab 必须支持横向滚动", "horizontalScroll(rememberScrollState())" in shared)
        assertTrue("Tab 标签必须保持单行", "softWrap = false" in shared)
        assertTrue("设置页必须使用统一 Tab", "SchoolScrollableTabs(" in settings)
    }

    @Test
    fun textInputsUseMinimumHeightInsteadOfFixedHeight() {
        val settings = uiFile("MaterialSettingsScreen.kt").readText(Charsets.UTF_8)
        assertTrue("输入框必须使用自适应最小高度", "heightIn(min = SchoolUiMetrics.textInputMinHeight)" in settings)
        assertFalse("输入框不能恢复固定 52dp 高度", ".fillMaxWidth().height(52.dp)" in settings)
    }

    @Test
    fun authoredLessonsUseSemanticPagerAndLastPracticePages() {
        val lesson = uiFile("InteractiveLessonScreen.kt").readText(Charsets.UTF_8)
        assertTrue("Lesson 必须通过语义分页组合器生成页面", "composeLessonPresentation(authoredLesson)" in lesson)
        assertTrue("Lesson 必须显示统一翻页底栏", "LessonPagerFooter(" in lesson)
        assertFalse("Lesson 不得恢复整课长页 renderer", "AuthoredLessonContent(" in lesson)
    }

    private fun uiFile(name: String): File = repositoryFile("app/src/main/java/com/majortomman/school/ui/$name")

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
