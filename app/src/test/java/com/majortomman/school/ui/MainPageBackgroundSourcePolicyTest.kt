package com.majortomman.school.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MainPageBackgroundSourcePolicyTest {
    @Test
    fun mainPagesDelegateFullScreenBackgroundToAppHost() {
        val expectations = mapOf(
            "SubjectTextbookCenterV2.kt" to "private val CenterBlack = Color.Transparent",
            "SubjectTextbookCenterComponents.kt" to "private val CenterBlack = Color.Transparent",
            "ScenePreviewScreens.kt" to "private val SceneBlack = Color.Transparent",
            "CurriculumTreeScreen.kt" to "private val TreeBlack = Color.Transparent",
            "MinimalRemainingScreens.kt" to "private val MinimalBlack = Color.Transparent",
            "MathQuestionBankScreen.kt" to "private val BankBlack = Color.Transparent",
            "MaterialSettingsScreen.kt" to "private val SettingsBlack = Color.Transparent",
            "InteractiveLessonScreen.kt" to "internal val InteractiveBlack = Color.Transparent",
        )

        expectations.forEach { (name, expected) ->
            val source = uiFile(name).readText(Charsets.UTF_8)
            assertTrue("$name 的主页面背景必须交给 AppBackgroundHost", expected in source)
        }
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
