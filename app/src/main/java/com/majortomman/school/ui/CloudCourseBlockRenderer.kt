package com.majortomman.school.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.learning.course.CourseCheckpoint
import com.majortomman.school.learning.course.CourseExample
import com.majortomman.school.learning.course.CourseExplanation
import com.majortomman.school.learning.course.CourseFormula
import com.majortomman.school.learning.course.CourseKeyIdea
import com.majortomman.school.learning.course.CourseLesson
import com.majortomman.school.learning.course.CoursePractice
import com.majortomman.school.learning.course.CourseQuestion
import com.majortomman.school.learning.course.CourseScene
import com.majortomman.school.learning.course.CourseSceneStep
import com.majortomman.school.learning.course.CourseSceneTemplate
import com.majortomman.school.learning.course.CourseSourceLink
import com.majortomman.school.learning.course.CourseStep
import com.majortomman.school.learning.course.CourseSummaryStep

@Composable
internal fun AuthoredLessonContent(
    lesson: CourseLesson,
    textbookAvailable: Boolean,
    onOpenTextbook: (Int) -> Unit,
) {
    lesson.steps.forEachIndexed { index, step ->
        if (index > 0) Spacer(Modifier.height(22.dp))
        AuthoredStep(step, lesson, textbookAvailable, onOpenTextbook)
    }
    if (lesson.practice.isNotEmpty()) {
        Spacer(Modifier.height(32.dp))
        OpenSectionTitle("独立练习", InteractiveBlue)
        Spacer(Modifier.height(14.dp))
        lesson.practice.forEachIndexed { index, practice ->
            if (index > 0) Spacer(Modifier.height(24.dp))
            PracticeBlock(index + 1, practice)
        }
    }
    Spacer(Modifier.height(32.dp))
    OpenSectionTitle("这一课记住", InteractiveYellow)
    Spacer(Modifier.height(12.dp))
    lesson.summary.forEach { item ->
        Text("— $item", color = InteractiveWhite.copy(alpha = 0.84f), fontSize = 16.sp, lineHeight = 26.sp, modifier = Modifier.padding(vertical = 4.dp))
    }
}

@Composable
private fun AuthoredStep(step: CourseStep, lesson: CourseLesson, textbookAvailable: Boolean, onOpenTextbook: (Int) -> Unit) {
    when (step) {
        is CourseExplanation -> {
            step.title?.let { OpenSectionTitle(it, InteractiveBlue); Spacer(Modifier.height(10.dp)) }
            Text(step.text, color = InteractiveWhite.copy(alpha = 0.9f), fontSize = 17.sp, lineHeight = 29.sp)
        }
        is CourseQuestion -> {
            OpenSectionTitle("先想一想", InteractiveYellow)
            Spacer(Modifier.height(10.dp))
            Text(step.prompt, color = InteractiveWhite, fontSize = 22.sp, lineHeight = 34.sp, fontWeight = FontWeight.Medium)
            step.hint?.let { Text("提示：$it", color = InteractiveMuted, fontSize = 13.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 10.dp)) }
        }
        is CourseKeyIdea -> {
            Box(Modifier.fillMaxWidth().height(2.dp).background(InteractiveBlue.copy(alpha = 0.72f)))
            Spacer(Modifier.height(10.dp))
            step.title?.let { Text(it, color = InteractiveBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            Text(step.text, color = InteractiveWhite, fontSize = 18.sp, lineHeight = 29.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 5.dp))
        }
        is CourseFormula -> {
            Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveYellow.copy(alpha = 0.3f)))
            Text(step.expression, modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp), color = InteractiveYellow, fontSize = 25.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            step.note?.let { Text(it, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), color = InteractiveMuted, fontSize = 13.sp, textAlign = TextAlign.Center) }
            Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveYellow.copy(alpha = 0.3f)))
        }
        is CourseExample -> {
            OpenSectionTitle(step.title, InteractiveBlue)
            Spacer(Modifier.height(10.dp))
            Text(step.prompt, color = InteractiveWhite, fontSize = 17.sp, lineHeight = 27.sp, fontWeight = FontWeight.Medium)
            step.steps.forEachIndexed { index, item ->
                Row(Modifier.padding(top = 9.dp), verticalAlignment = Alignment.Top) {
                    Text("${index + 1}", color = InteractiveYellow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.padding(horizontal = 6.dp))
                    Text(item, modifier = Modifier.weight(1f), color = InteractiveWhite.copy(alpha = 0.82f), fontSize = 15.sp, lineHeight = 24.sp)
                }
            }
            Text("答案：${step.answer}", color = InteractiveYellow, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 12.dp))
        }
        is CourseSceneStep -> CourseSceneView(step.scene, lesson.steps.filterIsInstance<CourseFormula>().firstOrNull()?.expression)
        is CourseCheckpoint -> {
            OpenSectionTitle("检查一下", InteractiveGreen)
            Spacer(Modifier.height(10.dp))
            Text(step.prompt, color = InteractiveWhite, fontSize = 18.sp, lineHeight = 28.sp)
            Text("参考：${step.expectedAnswer}", color = InteractiveGreen, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
            Text(step.explanation, color = InteractiveMuted, fontSize = 13.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 6.dp))
        }
        is CourseSourceLink -> {
            val reference = lesson.references[step.referenceIndex]
            Text(
                "↗ ${reference.label} · 查看教材第 ${reference.pageStart}${if (reference.pageEnd > reference.pageStart) "—${reference.pageEnd}" else ""} 页",
                modifier = Modifier.fillMaxWidth().clickable(enabled = textbookAvailable) { onOpenTextbook(reference.pageStart) }.padding(vertical = 9.dp),
                color = if (textbookAvailable) InteractiveYellow else InteractiveMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        is CourseSummaryStep -> Text(step.text, color = InteractiveWhite.copy(alpha = 0.84f), fontSize = 16.sp, lineHeight = 26.sp)
    }
}

@Composable
private fun PracticeBlock(number: Int, practice: CoursePractice) {
    Text("$number. ${practice.prompt}", color = InteractiveWhite, fontSize = 17.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium)
    Text("答案：${practice.answer}", color = InteractiveGreen, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
    practice.analysis.forEach { Text("— $it", color = InteractiveMuted, fontSize = 13.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 4.dp)) }
}

@Composable
private fun OpenSectionTitle(title: String, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.height(2.dp).weight(0.08f).background(color))
        Text(title, modifier = Modifier.weight(0.92f), color = InteractiveWhite, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun CourseSceneView(scene: CourseScene, formulaFallback: String?) {
    Box(modifier = Modifier.fillMaxWidth().height(when (scene.template) {
        CourseSceneTemplate.OPPOSITE_QUANTITIES, CourseSceneTemplate.RATIONAL_CLASSIFICATION, CourseSceneTemplate.INTEGER_TO_FRACTION, CourseSceneTemplate.NUMBER_LINE -> 420.dp
        else -> 320.dp
    })) {
        when (scene.template) {
            CourseSceneTemplate.OPPOSITE_QUANTITIES -> OppositeQuantitiesSceneVisual(scene.data)
            CourseSceneTemplate.RATIONAL_CLASSIFICATION -> RationalConceptFlowVisual(scene.data)
            CourseSceneTemplate.INTEGER_TO_FRACTION -> IntegerToFractionTextbookVisual()
            CourseSceneTemplate.NUMBER_LINE -> NumberLineLessonVisual(scene.data)
            CourseSceneTemplate.OPPOSITE_NUMBERS -> AdjustableNumberLine(NumberLineMode.OPPOSITE)
            CourseSceneTemplate.ABSOLUTE_VALUE -> AbsoluteValueNumberLineVisual()
            CourseSceneTemplate.NUMBER_COMPARISON -> ComparisonVisual()
            CourseSceneTemplate.ADDITION_PROCESS -> SignedUnitVisual()
            CourseSceneTemplate.SUBTRACTION_TRANSFORM, CourseSceneTemplate.DIVISION_TRANSFORM -> FormulaProcessVisual(scene.data.string("expression").ifBlank { formulaFallback.orEmpty() })
            CourseSceneTemplate.MULTIPLICATION_SIGN -> SignRuleVisual()
            CourseSceneTemplate.POWER_PROCESS -> PowerVisual()
            else -> TextbookMathVisual(scene.template, scene.data)
        }
    }
}
