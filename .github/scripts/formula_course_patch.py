from __future__ import annotations

import json
import re
from pathlib import Path


def read(path: str) -> str:
    return Path(path).read_text(encoding="utf-8")


def write(path: str, content: str) -> None:
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8")


def replace(path: str, old: str, new: str, count: int = 1) -> None:
    content = read(path)
    actual = content.count(old)
    if actual < count:
        raise RuntimeError(f"{path}: expected at least {count} occurrences, found {actual}: {old[:120]!r}")
    write(path, content.replace(old, new, count))


replace(
    "settings.gradle.kts",
    "        mavenCentral()\n    }\n}",
    "        mavenCentral()\n        maven(\"https://jitpack.io\")\n    }\n}",
)
replace(
    "app/build.gradle.kts",
    '    implementation("androidx.compose.ui:ui-tooling-preview")\n',
    '    implementation("androidx.compose.ui:ui-tooling-preview")\n'
    '    implementation("com.github.rikkahub.jlatexmath-android:jlatexmath:1.5")\n',
)

write(
    "app/src/main/java/com/majortomman/school/ui/SchoolFormula.kt",
    r'''package com.majortomman.school.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import ru.noties.jlatexmath.JLatexMathDrawable

/**
 * School course formulas use pure LaTeX math syntax.
 *
 * Rendering is an APK concern: course packages never name or depend on JLaTeXMath.
 * Long formulas keep their mathematical layout and become horizontally scrollable
 * instead of being squeezed into vertical text.
 */
@Composable
internal fun SchoolFormula(
    latex: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
) {
    val density = LocalDensity.current
    val defaultColor = MaterialTheme.colorScheme.onBackground
    val defaultSize = MaterialTheme.typography.headlineMedium.fontSize
    val resolvedColor = if (color == Color.Unspecified) {
        if (style.color == Color.Unspecified) defaultColor else style.color
    } else {
        color
    }
    val resolvedSize = if (style.fontSize == TextUnit.Unspecified) defaultSize else style.fontSize
    val drawable = remember(latex, resolvedColor, resolvedSize, density.density, density.fontScale) {
        runCatching {
            JLatexMathDrawable.builder(latex.trim())
                .textSize(with(density) { resolvedSize.toPx() })
                .color(resolvedColor.toArgb())
                .padding(0)
                .align(JLatexMathDrawable.ALIGN_LEFT)
                .build()
        }.getOrNull()
    }

    if (drawable == null) {
        Text(text = latex, modifier = modifier, color = resolvedColor, style = style)
        return
    }

    val scrollState = rememberScrollState()
    with(density) {
        Row(
            modifier = modifier.fillMaxWidth().horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.Center,
        ) {
            Canvas(
                modifier = Modifier.size(
                    width = drawable.bounds.width().toDp(),
                    height = drawable.bounds.height().toDp(),
                ),
            ) {
                drawable.draw(drawContext.canvas.nativeCanvas)
            }
        }
    }
}
''',
)

ui_path = "app/src/main/java/com/majortomman/school/ui/SchoolUiSystem.kt"
ui = read(ui_path)
marker = "@Composable\ninternal fun SchoolCompactTopBar("
start = ui.index(marker)
ui = ui[:start] + r'''@Composable
internal fun SchoolCompactTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    actionEnabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SchoolUiMetrics.minTouchHeight)
            .padding(horizontal = 22.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "返回",
            modifier = Modifier.clickable(onClick = onBack).padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (actionLabel == null) TextAlign.End else TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                modifier = Modifier.clickable(enabled = actionEnabled, onClick = onAction).padding(vertical = 8.dp),
                color = if (actionEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.28f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}
'''
write(ui_path, ui)

lesson_path = "app/src/main/java/com/majortomman/school/ui/InteractiveLessonScreen.kt"
replace(
    lesson_path,
    "    if (pageIndex !in pages.indices) pageIndex = 0\n\n    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {\n"
    "        SchoolCompactTopBar(title = authoredLesson.title, onBack = onBack)\n",
    "    if (pageIndex !in pages.indices) pageIndex = 0\n"
    "    val textbookReference = authoredLesson.references.firstOrNull()\n\n"
    "    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {\n"
    "        SchoolCompactTopBar(\n"
    "            title = authoredLesson.title,\n"
    "            onBack = onBack,\n"
    "            actionLabel = if (textbookReference != null) \"PDF\" else null,\n"
    "            onAction = { textbookReference?.let { onOpenTextbook(it.pageStart) } },\n"
    "            actionEnabled = textbookReference != null && installedMaterial.pdfFile.isFile,\n"
    "        )\n",
)
replace(
    lesson_path,
    "                LessonPresentationPageContent(\n"
    "                    page = page,\n"
    "                    lesson = authoredLesson,\n"
    "                    textbookAvailable = installedMaterial.pdfFile.isFile,\n"
    "                    onOpenTextbook = onOpenTextbook,\n"
    "                )\n",
    "                LessonPresentationPageContent(page = page, lesson = authoredLesson)\n",
)
replace(
    lesson_path,
    "private fun LessonPresentationPageContent(\n"
    "    page: LessonPresentationPage,\n"
    "    lesson: com.majortomman.school.learning.course.CourseLesson,\n"
    "    textbookAvailable: Boolean,\n"
    "    onOpenTextbook: (Int) -> Unit,\n"
    ") {\n",
    "private fun LessonPresentationPageContent(\n"
    "    page: LessonPresentationPage,\n"
    "    lesson: com.majortomman.school.learning.course.CourseLesson,\n"
    ") {\n",
)
replace(
    lesson_path,
    "        is LessonPresentationPage.Teaching -> {\n"
    "            AuthoredTeachingPageContent(page.steps, lesson, textbookAvailable, onOpenTextbook)\n"
    "        }\n",
    "        is LessonPresentationPage.Teaching -> AuthoredTeachingPageContent(page.steps, lesson)\n",
)

renderer_path = "app/src/main/java/com/majortomman/school/ui/CloudCourseBlockRenderer.kt"
replace(
    renderer_path,
    "internal fun AuthoredTeachingPageContent(\n"
    "    steps: List<CourseStep>,\n"
    "    lesson: CourseLesson,\n"
    "    textbookAvailable: Boolean,\n"
    "    onOpenTextbook: (Int) -> Unit,\n"
    ") {\n"
    "    steps.forEachIndexed { index, step ->\n"
    "        if (index > 0) Spacer(Modifier.height(SchoolUiMetrics.sectionGap))\n"
    "        AuthoredStep(step, lesson, textbookAvailable, onOpenTextbook)\n"
    "    }\n"
    "}\n",
    "internal fun AuthoredTeachingPageContent(steps: List<CourseStep>, lesson: CourseLesson) {\n"
    "    steps.forEachIndexed { index, step ->\n"
    "        if (index > 0) Spacer(Modifier.height(SchoolUiMetrics.sectionGap))\n"
    "        AuthoredStep(step, lesson)\n"
    "    }\n"
    "}\n",
)
replace(
    renderer_path,
    "private fun AuthoredStep(step: CourseStep, lesson: CourseLesson, textbookAvailable: Boolean, onOpenTextbook: (Int) -> Unit) {\n",
    "private fun AuthoredStep(step: CourseStep, lesson: CourseLesson) {\n",
)
replace(
    renderer_path,
    '''        is CourseFormula -> {
            Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveYellow.copy(alpha = 0.3f)))
            Text(
                text = step.expression,
                modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                color = InteractiveYellow,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
            step.note?.let {
                Text(it, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), color = InteractiveMuted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveYellow.copy(alpha = 0.3f)))
        }
''',
    '''        is CourseFormula -> {
            Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveYellow.copy(alpha = 0.3f)))
            SchoolFormula(
                latex = step.expression,
                modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                color = InteractiveYellow,
                style = MaterialTheme.typography.headlineMedium,
            )
            step.note?.let {
                Text(it, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), color = InteractiveMuted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveYellow.copy(alpha = 0.3f)))
        }
''',
)
content = read(renderer_path)
pattern = re.compile(r'''        is CourseSourceLink -> \{\n(?:.*\n)*?        \}\n        is CourseSummaryStep ->''')
content, replaced_count = pattern.subn("        is CourseSourceLink -> Unit\n        is CourseSummaryStep ->", content, count=1)
if replaced_count != 1:
    raise RuntimeError(f"{renderer_path}: failed to replace CourseSourceLink renderer")
write(renderer_path, content)

validator = "tools/course-content/validate_authored_course.py"
replace(
    validator,
    'STEP_TYPES = {"explanation", "question", "keyIdea", "formula", "example", "scene", "checkpoint", "sourceLink", "summary"}\n',
    'STEP_TYPES = {"explanation", "question", "keyIdea", "formula", "example", "scene", "checkpoint", "summary"}\n'
    'CJK = re.compile(r"[\\u3400-\\u9fff]")\n'
    'NON_LATEX_MATH = set("²³⁴⁵⁶⁷⁸⁹₀₁₂₃₄₅₆₇₈₉−×÷≤≥≠Σαβγθπ°′″")\n',
)
replace(
    validator,
    '''                    if step_type == "sourceLink":
                        ref_index = step.get("referenceIndex")
                        require(isinstance(ref_index, int) and 0 <= ref_index < len(references), f"{stw}: referenceIndex out of range")
                    if step_type == "scene":
''',
    '''                    if step_type == "formula":
                        exact(step, {"type", "expression", "note"}, stw)
                        expression = text(step, "expression", stw)
                        require("$" not in expression and "\\\\(" not in expression and "\\\\)" not in expression and "\\\\[" not in expression and "\\\\]" not in expression, f"{stw}.expression: store pure LaTeX math without delimiters")
                        require(CJK.search(expression) is None, f"{stw}.expression: Chinese prose belongs outside formulas")
                        require(not any(char in NON_LATEX_MATH for char in expression), f"{stw}.expression: use LaTeX commands instead of Unicode math glyphs")
                        note = step.get("note")
                        require(note is None or (isinstance(note, str) and note.strip()), f"{stw}.note: null or non-empty string required")
                    if step_type == "scene":
''',
)

example_path = Path("courses/examples/pep-math-7-1-course.json")
example = json.loads(example_path.read_text(encoding="utf-8"))
example_steps = example["chapters"][0]["sections"][0]["lessons"][0]["steps"]
example_steps[:] = [step for step in example_steps if step["type"] != "sourceLink"]
for step in example_steps:
    if step["type"] == "formula":
        step["expression"] = r"+3,\qquad -3"
example_path.write_text(json.dumps(example, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

write(
    "docs/FORMULA_STANDARD.md",
    r'''# School 数学公式规范

课程包中的公式统一使用标准 LaTeX 数学语法，App 负责把公式渲染到界面。

## 数据约定

`CourseFormula.expression` 只保存数学表达式本身，不保存 `$...$`、`$$...$$`、`\(...\)` 或 `\[...\]` 分隔符，也不保存渲染器名称。

示例：

```json
{
  "type": "formula",
  "expression": "\\frac{-b\\pm\\sqrt{b^2-4ac}}{2a}",
  "note": "公式说明放在普通正文中。"
}
```

课程数据不得使用 Unicode 上下标、Unicode 数学运算符或中文句子代替 LaTeX。中文解释放在 `note`、`explanation`、`keyIdea` 等正文区域。

## 显示职责

- 课程包只负责标准 LaTeX。
- APK 通过 `SchoolFormula` 统一渲染，当前实现使用 JLaTeXMath Android。
- 长公式保持正常数学布局；超出屏幕宽度时允许横向滚动，不把公式压成竖排。
- 渲染失败时回退显示原始表达式，避免课程页面崩溃。
- 用户文字缩放通过 Compose `sp` / `LocalDensity` 同步作用到公式字号。

## 教材入口

教材 PDF 是课程参考资料，不作为正文步骤。每个 Lesson 继续保留 `references` 页码元数据，App 在课程顶部右侧提供唯一的 `PDF` 入口，并跳转到当前 Lesson 的首个教材参考页。

## 第三方实现说明

当前 APK 使用 `rikkahub/jlatexmath-android` 1.5（JLaTeXMath Android fork）作为公式绘制实现。课程 JSON 不依赖该实现，未来替换渲染器不需要修改课程格式。
''',
)

replace("version.properties", "VERSION_NAME=0.26.4\nVERSION_CODE=43\n", "VERSION_NAME=0.26.5\nVERSION_CODE=44\n")
notes = ".release-notes/current.md"
replace(
    notes,
    "## 修改点\n\n",
    "## 修改点\n\n"
    "- 课程公式统一使用标准 LaTeX 数学语法，新增 `SchoolFormula` 原生 Compose 渲染层；课程数据不再绑定具体第三方渲染器。\n"
    "- Lesson 教材引用收口到顶部右侧唯一 `PDF` 入口，正文不再插入教材跳转步骤；入口直接定位到当前 Lesson 的教材参考页。\n"
    "- 七上课程正文按教材原有知识顺序、术语和讲解节奏重新校正表达，保留教材式的引入与归纳风格，同时继续使用原创措辞和原创例题练习。\n",
)
replace(
    notes,
    "## 修复点\n\n",
    "## 修复点\n\n"
    "- 修复分数、根式、上下标、角度和方程等公式只能以普通文本显示的问题；长公式不再通过压缩文字适配窄屏。\n"
    "- 清理七上课程中 64 个正文 `sourceLink` 和非公式型“流程公式”，20 个真正公式全部改为纯 LaTeX 表达式。\n",
)
