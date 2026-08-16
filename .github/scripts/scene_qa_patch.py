from pathlib import Path
import re


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding='utf-8')
    if old not in text:
        raise SystemExit(f'expected snippet not found in {path}: {old[:80]!r}')
    path.write_text(text.replace(old, new, 1), encoding='utf-8')


renderer = Path('app/src/main/java/com/majortomman/school/ui/CloudCourseBlockRenderer.kt')
replace_once(
    renderer,
    'CourseSceneTemplate.SUBTRACTION_TRANSFORM, CourseSceneTemplate.DIVISION_TRANSFORM -> FormulaProcessVisual(scene.data.string("expression").ifBlank { formulaFallback.orEmpty() })',
    'CourseSceneTemplate.SUBTRACTION_TRANSFORM, CourseSceneTemplate.DIVISION_TRANSFORM -> FormulaProcessVisual(formulaFallback.orEmpty())',
)

visuals = Path('app/src/main/java/com/majortomman/school/ui/CloudCourseVisualizations.kt')
text = visuals.read_text(encoding='utf-8')
text = text.replace('import androidx.compose.material3.Slider\n', 'import androidx.compose.material3.MaterialTheme\nimport androidx.compose.material3.Slider\n', 1)
old_formula = '''@Composable
internal fun FormulaProcessVisual(formula: String?) {
    Text(
        formula.orEmpty(),
        modifier = Modifier.fillMaxWidth().padding(18.dp),
        color = InteractiveYellow,
        fontSize = 22.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
    )
}'''
new_formula = '''@Composable
internal fun FormulaProcessVisual(formula: String?) {
    SchoolFormula(
        latex = formula.orEmpty(),
        modifier = Modifier.fillMaxWidth().padding(18.dp),
        color = InteractiveYellow,
        style = MaterialTheme.typography.headlineMedium,
    )
}'''
if old_formula not in text:
    raise SystemExit('FormulaProcessVisual block not found')
visuals.write_text(text.replace(old_formula, new_formula, 1), encoding='utf-8')

textbook = Path('app/src/main/java/com/majortomman/school/ui/TextbookMathVisualizations.kt')
text = textbook.read_text(encoding='utf-8')
text = text.replace(
    'CourseSceneTemplate.GEOMETRY -> drawGeometry(data.string("shape", "triangle"))',
    'CourseSceneTemplate.GEOMETRY -> drawGeometry(data.string("shape").ifBlank { data.string("title") })',
    1,
)
text = text.replace(
    'CourseSceneTemplate.PROJECTION -> drawProjection()\n                else -> Unit',
    'CourseSceneTemplate.PROJECTION -> drawProjection()\n                CourseSceneTemplate.DIAGRAM -> drawDiagram(data)\n                else -> Unit',
    1,
)
start = text.index('private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGeometry(shape: String) {')
end = text.index('private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTransformation(mode: String) {')
new_geometry = r'''private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGeometry(shape: String) {
    when {
        shape.contains("角") -> drawAngleGeometry()
        shape.contains("物体") || shape.contains("几何对象") -> drawObjectAbstraction()
        shape.contains("circle", true) -> {
            val c = Offset(size.width / 2, size.height / 2)
            val r = minOf(size.width, size.height) * .32f
            drawCircle(InteractiveBlue, r, c, style = Stroke(4f))
            drawLine(InteractiveYellow, c, Offset(c.x + r, c.y), 3f)
            drawCenteredText("O", c.x - 18f, c.y + 8f, InteractiveWhite, 22f)
        }
        shape.contains("parallel", true) -> {
            drawLine(InteractiveBlue, Offset(45f, size.height * .32f), Offset(size.width - 40f, size.height * .22f), 4f)
            drawLine(InteractiveBlue, Offset(45f, size.height * .72f), Offset(size.width - 40f, size.height * .62f), 4f)
            drawLine(InteractiveYellow, Offset(size.width * .36f, 30f), Offset(size.width * .62f, size.height - 25f), 4f)
        }
        else -> {
            val a = Offset(size.width * .5f, size.height * .14f)
            val b = Offset(size.width * .17f, size.height * .82f)
            val c = Offset(size.width * .83f, size.height * .82f)
            drawLine(InteractiveBlue, a, b, 4f)
            drawLine(InteractiveBlue, b, c, 4f)
            drawLine(InteractiveBlue, c, a, 4f)
            drawCenteredText("A", a.x, a.y - 14f, InteractiveWhite, 22f)
            drawCenteredText("B", b.x - 12f, b.y + 28f, InteractiveWhite, 22f)
            drawCenteredText("C", c.x + 12f, c.y + 28f, InteractiveWhite, 22f)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawObjectAbstraction() {
    val leftX = size.width * .2f
    val rightX = size.width * .78f
    val firstY = size.height * .34f
    val secondY = size.height * .72f
    val boxWidth = size.width * .22f
    val boxHeight = size.height * .22f

    drawRect(
        InteractiveBlue,
        topLeft = Offset(leftX - boxWidth / 2, firstY - boxHeight / 2),
        size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
        style = Stroke(4f),
    )
    drawCenteredText("纸盒", leftX, firstY + boxHeight * .8f, InteractiveMuted, 20f)
    drawArrow(leftX + boxWidth * .7f, firstY, rightX - boxWidth * .7f, firstY, InteractiveYellow)
    drawRect(
        InteractiveYellow,
        topLeft = Offset(rightX - boxWidth / 2, firstY - boxHeight / 2),
        size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
        style = Stroke(4f),
    )
    drawCenteredText("长方体", rightX, firstY + boxHeight * .8f, InteractiveWhite, 22f)

    val canWidth = size.width * .18f
    val canHeight = size.height * .22f
    drawRect(
        InteractiveBlue,
        topLeft = Offset(leftX - canWidth / 2, secondY - canHeight / 2),
        size = androidx.compose.ui.geometry.Size(canWidth, canHeight),
        style = Stroke(4f),
    )
    drawOval(
        InteractiveBlue,
        topLeft = Offset(leftX - canWidth / 2, secondY - canHeight / 2 - 8f),
        size = androidx.compose.ui.geometry.Size(canWidth, 16f),
        style = Stroke(3f),
    )
    drawCenteredText("圆罐", leftX, secondY + canHeight * .8f, InteractiveMuted, 20f)
    drawArrow(leftX + boxWidth * .7f, secondY, rightX - boxWidth * .7f, secondY, InteractiveYellow)
    drawRect(
        InteractiveYellow,
        topLeft = Offset(rightX - canWidth / 2, secondY - canHeight / 2),
        size = androidx.compose.ui.geometry.Size(canWidth, canHeight),
        style = Stroke(4f),
    )
    drawOval(
        InteractiveYellow,
        topLeft = Offset(rightX - canWidth / 2, secondY - canHeight / 2 - 8f),
        size = androidx.compose.ui.geometry.Size(canWidth, 16f),
        style = Stroke(3f),
    )
    drawCenteredText("圆柱", rightX, secondY + canHeight * .8f, InteractiveWhite, 22f)
    drawCenteredText("忽略颜色、材料等细节，只保留形状特征", size.width / 2, size.height * .08f, InteractiveMuted, 20f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAngleGeometry() {
    val o = Offset(size.width * .38f, size.height * .68f)
    val a = Offset(size.width * .78f, size.height * .68f)
    val b = Offset(size.width * .63f, size.height * .2f)
    drawLine(InteractiveBlue, o, a, 5f, StrokeCap.Round)
    drawLine(InteractiveYellow, o, b, 5f, StrokeCap.Round)
    drawArrowHead(a, 1f, 0f, InteractiveBlue)
    val dx = b.x - o.x
    val dy = b.y - o.y
    val length = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    drawArrowHead(b, dx / length, dy / length, InteractiveYellow)
    drawCircle(InteractiveWhite, 8f, o)
    drawCenteredText("O", o.x - 20f, o.y + 30f, InteractiveWhite, 22f)
    drawCenteredText("A", a.x + 14f, a.y + 28f, InteractiveBlue, 22f)
    drawCenteredText("B", b.x + 18f, b.y - 8f, InteractiveYellow, 22f)
    drawCenteredText("∠AOB", size.width * .58f, size.height * .48f, InteractiveWhite, 28f)
    drawCenteredText("公共端点 O + 两条射线", size.width / 2, size.height * .9f, InteractiveMuted, 20f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDiagram(data: CourseSceneData) {
    val title = data.string("title")
    when {
        title.contains("端点") || title.contains("直线") || title.contains("射线") -> drawLineRaySegmentDiagram()
        title.contains("场地") || title.contains("田径") -> drawAthleticsFieldDiagram()
        else -> {
            drawLine(InteractiveBlue, Offset(size.width * .16f, size.height * .5f), Offset(size.width * .84f, size.height * .5f), 4f)
            drawCenteredText(title.ifBlank { "示意图" }, size.width / 2, size.height * .34f, InteractiveWhite, 24f)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLineRaySegmentDiagram() {
    val left = size.width * .18f
    val right = size.width * .82f
    val lineY = size.height * .22f
    val rayY = size.height * .5f
    val segmentY = size.height * .78f

    drawLine(InteractiveBlue, Offset(left, lineY), Offset(right, lineY), 4f, StrokeCap.Round)
    drawArrowHead(Offset(left, lineY), -1f, 0f, InteractiveBlue)
    drawArrowHead(Offset(right, lineY), 1f, 0f, InteractiveBlue)
    drawCenteredText("直线：没有端点，向两方无限延伸", size.width / 2, lineY - 34f, InteractiveWhite, 20f)

    drawCircle(InteractiveYellow, 7f, Offset(left, rayY))
    drawLine(InteractiveYellow, Offset(left, rayY), Offset(right, rayY), 4f, StrokeCap.Round)
    drawArrowHead(Offset(right, rayY), 1f, 0f, InteractiveYellow)
    drawCenteredText("射线：1个端点，向一方无限延伸", size.width / 2, rayY - 34f, InteractiveWhite, 20f)

    drawCircle(InteractiveWhite, 7f, Offset(left, segmentY))
    drawCircle(InteractiveWhite, 7f, Offset(right, segmentY))
    drawLine(InteractiveWhite, Offset(left, segmentY), Offset(right, segmentY), 4f, StrokeCap.Round)
    drawCenteredText("线段：2个端点，长度可以测量", size.width / 2, segmentY - 34f, InteractiveWhite, 20f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAthleticsFieldDiagram() {
    val outerLeft = size.width * .12f
    val outerTop = size.height * .16f
    val outerWidth = size.width * .76f
    val outerHeight = size.height * .68f
    drawRect(
        InteractiveBlue,
        topLeft = Offset(outerLeft, outerTop),
        size = androidx.compose.ui.geometry.Size(outerWidth, outerHeight),
        style = Stroke(4f),
    )
    val inset = 24f
    drawRect(
        InteractiveBlue.copy(alpha = .55f),
        topLeft = Offset(outerLeft + inset, outerTop + inset),
        size = androidx.compose.ui.geometry.Size(outerWidth - inset * 2, outerHeight - inset * 2),
        style = Stroke(3f),
    )
    val startX = outerLeft + outerWidth * .22f
    val finishX = outerLeft + outerWidth * .78f
    drawLine(InteractiveYellow, Offset(startX, outerTop), Offset(startX, outerTop + outerHeight), 4f)
    drawLine(InteractiveWhite, Offset(finishX, outerTop), Offset(finishX, outerTop + outerHeight), 4f)
    drawLine(
        InteractiveMuted,
        Offset(outerLeft + inset, outerTop + outerHeight / 2),
        Offset(outerLeft + outerWidth - inset, outerTop + outerHeight / 2),
        2f,
    )
    drawCenteredText("起点", startX, outerTop - 18f, InteractiveYellow, 20f)
    drawCenteredText("终点", finishX, outerTop - 18f, InteractiveWhite, 20f)
    drawCenteredText("跑道边界保持平行，起终点位置用线段确定", size.width / 2, size.height * .94f, InteractiveMuted, 19f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrowHead(end: Offset, dx: Float, dy: Float, color: Color) {
    val length = 16f
    val wing = 9f
    val px = -dy
    val py = dx
    drawLine(color, end, Offset(end.x - dx * length + px * wing, end.y - dy * length + py * wing), 3f, StrokeCap.Round)
    drawLine(color, end, Offset(end.x - dx * length - px * wing, end.y - dy * length - py * wing), 3f, StrokeCap.Round)
}

'''
textbook.write_text(text[:start] + new_geometry + text[end:], encoding='utf-8')

Path('version.properties').write_text('VERSION_NAME=0.26.6\nVERSION_CODE=45\n', encoding='utf-8')
Path('.release-notes/current.md').write_text('''## 修改点

- 对七上课程的可视化场景继续做 App 级验收，为直线/射线/线段、角、实物抽象和田径场设计补上与知识点直接对应的专用示意图。
- 减法与除法的转化场景统一复用课程中的标准 LaTeX 公式，并通过 `SchoolFormula` 渲染，不再维护第二套 Unicode 公式文本。
- 新增发布说明新鲜度 CI：每次 APK/构建发布都必须重新编写本次说明，禁止继承上一版本的列表条目。

## 修复点

- 修复 `diagram` 类型课程场景没有渲染分支、导致“直线/射线/线段”和“田径运动会比赛场地”出现空白区域的问题。
- 修复“从真实物体抽象几何图形”和“角”两个场景都退化成默认三角形、图形与当前知识点不对应的问题。
- 修复减法、除法转化场景仍以普通文本显示公式，和新版标准 LaTeX 公式层不一致的问题。
''', encoding='utf-8')
