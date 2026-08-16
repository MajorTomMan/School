from pathlib import Path


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding='utf-8')
    if old not in text:
        raise SystemExit(f'expected snippet not found in {path}: {old[:100]!r}')
    path.write_text(text.replace(old, new, 1), encoding='utf-8')


renderer = Path('app/src/main/java/com/majortomman/school/ui/CloudCourseBlockRenderer.kt')
replace_once(
    renderer,
    'CourseSceneTemplate.ADDITION_PROCESS -> SignedUnitVisual()',
    'CourseSceneTemplate.ADDITION_PROCESS -> SignedMovementNumberLineVisual()',
)

visuals = Path('app/src/main/java/com/majortomman/school/ui/CloudCourseVisualizations.kt')
text = visuals.read_text(encoding='utf-8')
old_power = '''@Composable
internal fun PowerVisual() {
    var base by rememberSaveable { mutableStateOf(-2f) }
    var exponent by rememberSaveable { mutableStateOf(3f) }
    val baseValue = base.roundToInt()
    val exponentValue = exponent.roundToInt().coerceIn(1, 6)
    val result = (1..exponentValue).fold(1) { current, _ -> current * baseValue }
    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceEvenly) {
        Slider(value = base, onValueChange = { base = it.roundToInt().toFloat() }, valueRange = -4f..4f, steps = 7)
        Slider(value = exponent, onValueChange = { exponent = it.roundToInt().toFloat() }, valueRange = 1f..6f, steps = 4)
        Text(
            "$baseValue ^ $exponentValue = $result",
            color = InteractiveYellow,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}'''
new_power = r'''@Composable
internal fun PowerVisual() {
    var base by rememberSaveable { mutableStateOf(-2f) }
    var exponent by rememberSaveable { mutableStateOf(3f) }
    val baseValue = base.roundToInt()
    val exponentValue = exponent.roundToInt().coerceIn(1, 6)
    val result = (1..exponentValue).fold(1) { current, _ -> current * baseValue }
    val factor = if (baseValue < 0) "($baseValue)" else baseValue.toString()
    val expansion = List(exponentValue) { factor }.joinToString("\\cdot")
    val expression = "$factor^{$exponentValue}=$expansion=$result"
    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceEvenly) {
        Slider(value = base, onValueChange = { base = it.roundToInt().toFloat() }, valueRange = -4f..4f, steps = 7)
        Slider(value = exponent, onValueChange = { exponent = it.roundToInt().toFloat() }, valueRange = 1f..6f, steps = 4)
        Text(
            "底数 $baseValue　指数 $exponentValue",
            color = InteractiveMuted,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        SchoolFormula(
            latex = expression,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            color = InteractiveYellow,
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}'''
if old_power not in text:
    raise SystemExit('PowerVisual block not found')
visuals.write_text(text.replace(old_power, new_power, 1), encoding='utf-8')

textbook = Path('app/src/main/java/com/majortomman/school/ui/TextbookMathVisualizations.kt')
text = textbook.read_text(encoding='utf-8')
text = text.replace(
    'drawCenteredText(data.string("left", "x + 3"), size.width * .25f, beamY + 92f, InteractiveBlue, 30f)\n    drawCenteredText(data.string("right", "7"), size.width * .75f, beamY + 92f, InteractiveYellow, 30f)',
    'drawCenteredMultilineText(data.string("left", "x + 3"), size.width * .25f, beamY + 92f, InteractiveBlue, 24f)\n    drawCenteredMultilineText(data.string("right", "7"), size.width * .75f, beamY + 92f, InteractiveYellow, 24f)',
    1,
)
old_projection = '''private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawProjection() {
    val x = size.width * .28f
    val y = size.height * .25f
    val w = size.width * .38f
    val h = size.height * .42f
    val d = size.width * .13f
    val points = listOf(
        Offset(x, y), Offset(x + w, y), Offset(x + w, y + h), Offset(x, y + h),
        Offset(x + d, y - d), Offset(x + w + d, y - d), Offset(x + w + d, y + h - d), Offset(x + d, y + h - d),
    )
    val edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0, 4 to 5, 5 to 6, 6 to 7, 7 to 4, 0 to 4, 1 to 5, 2 to 6, 3 to 7)
    edges.forEach { (a, b) -> drawLine(InteractiveBlue, points[a], points[b], 3f) }
    drawArrow(size.width * .73f, size.height * .48f, size.width * .9f, size.height * .48f, InteractiveYellow)
}'''
new_projection = '''private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawProjection() {
    val x = size.width * .08f
    val y = size.height * .32f
    val w = size.width * .26f
    val h = size.height * .34f
    val d = size.width * .09f
    val points = listOf(
        Offset(x, y), Offset(x + w, y), Offset(x + w, y + h), Offset(x, y + h),
        Offset(x + d, y - d), Offset(x + w + d, y - d), Offset(x + w + d, y + h - d), Offset(x + d, y + h - d),
    )
    val edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0, 4 to 5, 5 to 6, 6 to 7, 7 to 4, 0 to 4, 1 to 5, 2 to 6, 3 to 7)
    edges.forEach { (a, b) -> drawLine(InteractiveBlue, points[a], points[b], 3f) }
    drawCenteredText("立体图形", size.width * .22f, size.height * .83f, InteractiveBlue, 20f)
    drawArrow(size.width * .42f, size.height * .49f, size.width * .53f, size.height * .49f, InteractiveYellow)

    val viewX = size.width * .62f
    val frontY = size.height * .18f
    val viewWidth = size.width * .24f
    val viewHeight = size.height * .16f
    drawRect(InteractiveYellow, Offset(viewX, frontY), androidx.compose.ui.geometry.Size(viewWidth, viewHeight), style = Stroke(3f))
    drawCenteredText("正面", viewX + viewWidth / 2, frontY - 12f, InteractiveWhite, 18f)

    val sideWidth = size.width * .1f
    val sideY = size.height * .48f
    drawRect(InteractiveYellow, Offset(viewX + (viewWidth - sideWidth) / 2, sideY), androidx.compose.ui.geometry.Size(sideWidth, viewHeight), style = Stroke(3f))
    drawCenteredText("侧面", viewX + viewWidth / 2, sideY - 12f, InteractiveWhite, 18f)

    val topHeight = size.height * .07f
    val topY = size.height * .78f
    drawRect(InteractiveYellow, Offset(viewX, topY), androidx.compose.ui.geometry.Size(viewWidth, topHeight), style = Stroke(3f))
    drawCenteredText("上面", viewX + viewWidth / 2, topY - 12f, InteractiveWhite, 18f)
}'''
if old_projection not in text:
    raise SystemExit('drawProjection block not found')
text = text.replace(old_projection, new_projection, 1)
needle = '''private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCenteredText(
    text: String,
    x: Float,
    y: Float,
    color: Color,
    textSize: Float,
) {'''
helper = '''private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCenteredMultilineText(
    text: String,
    x: Float,
    y: Float,
    color: Color,
    textSize: Float,
    maxCharsPerLine: Int = 7,
) {
    val clean = text.trim()
    val lines = if (clean.length <= maxCharsPerLine) listOf(clean) else clean.chunked(maxCharsPerLine)
    val lineHeight = visualTextSizePx(textSize) * 1.12f
    val firstY = y - (lines.size - 1) * lineHeight / 2f
    lines.forEachIndexed { index, line -> drawCenteredText(line, x, firstY + index * lineHeight, color, textSize) }
}

''' + needle
if needle not in text:
    raise SystemExit('drawCenteredText declaration not found')
textbook.write_text(text.replace(needle, helper, 1), encoding='utf-8')

Path('version.properties').write_text('VERSION_NAME=0.26.7\nVERSION_CODE=46\n', encoding='utf-8')
Path('.release-notes/current.md').write_text('''## 修改点

- 继续做七上课程场景的第二轮 App 级验收，把有理数加法改为可调起点与位移的数轴演示，让“连续位移看加法”和画面表达保持一致。
- 乘方场景改用标准 LaTeX 动态展示“幂 → 重复乘法 → 结果”的完整展开链，并明确当前底数与指数。
- “从不同方向看立体图形”改为同时展示立体图形以及正面、侧面、上面三个二维视图，不再只画一根观察方向箭头。

## 修复点

- 修复有理数加法场景使用正负筹码抵消、与课程当前讲解的连续位移思路不一致的问题。
- 修复乘方场景只显示普通文本 `a ^ n = result`、没有直观解释指数表示重复乘法的问题。
- 修复方程建模天平中的长标签固定单行显示、在窄屏容易相互挤压或越界的问题，长文字现在会自动分行居中。
''', encoding='utf-8')
