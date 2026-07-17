package com.majortomman.school.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.learning.science.chemistry.organic.FunctionalGroupDetector
import com.majortomman.school.learning.science.chemistry.organic.OrganicBondOrder
import com.majortomman.school.learning.science.chemistry.organic.OrganicIsomerAnalyzer
import com.majortomman.school.learning.science.chemistry.organic.OrganicMolecule
import com.majortomman.school.learning.science.chemistry.organic.OrganicMoleculeLayout
import com.majortomman.school.learning.science.chemistry.organic.OrganicNotationParser
import com.majortomman.school.learning.science.chemistry.organic.TextbookOrganicReactionTemplates
import kotlin.math.hypot

@Composable
internal fun OrganicChemistryLabSample() {
    var firstText by rememberSaveable { mutableStateOf("CCO") }
    var secondText by rememberSaveable { mutableStateOf("COC") }
    val firstResult = runCatching { OrganicNotationParser.parse(firstText) }
    val secondResult = runCatching { OrganicNotationParser.parse(secondText) }

    SectionTitle("有机化学分子图内核", InteractivePurple)
    Spacer(Modifier.height(12.dp))
    Text(
        "结构式会转成原子—化学键图，再进行价态、分子式、官能团、碳骨架和同分异构判断。当前输入采用受限的 SMILES 风格技术记法。",
        color = InteractiveMuted,
        fontSize = 15.sp,
        lineHeight = 23.sp,
    )
    Spacer(Modifier.height(22.dp))

    OrganicInput("结构 A", firstText) { firstText = it.take(100) }
    Spacer(Modifier.height(16.dp))
    firstResult.fold(
        onSuccess = { molecule -> OrganicMoleculeSummary(molecule) },
        onFailure = { OrganicError(it.message ?: "无法解析结构 A。") },
    )

    Spacer(Modifier.height(28.dp))
    OrganicInput("结构 B", secondText) { secondText = it.take(100) }
    Spacer(Modifier.height(16.dp))
    secondResult.fold(
        onSuccess = { molecule ->
            OrganicLine("分子式", molecule.molecularFormula())
            OrganicLine(
                "官能团",
                FunctionalGroupDetector.detect(molecule).joinToString("、") { it.type.label }.ifBlank { "未识别" },
            )
        },
        onFailure = { OrganicError(it.message ?: "无法解析结构 B。") },
    )

    Spacer(Modifier.height(24.dp))
    if (firstResult.isSuccess && secondResult.isSuccess) {
        val comparison = OrganicIsomerAnalyzer.compare(firstResult.getOrThrow(), secondResult.getOrThrow())
        Text(
            comparison.message,
            color = if (comparison.constitutionalIsomers) InteractiveYellow else InteractiveBlue,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
        )
    }

    Spacer(Modifier.height(26.dp))
    Text("教材限定反应模板", color = InteractiveGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(10.dp))
    val hydrogenation = TextbookOrganicReactionTemplates.etheneHydrogenation
    OrganicLine("反应类型", hydrogenation.reactionClass)
    OrganicLine("反应物", "C=C + H-H")
    OrganicLine("生成物", hydrogenation.products.joinToString(" + ") { it.molecularFormula() })
    OrganicLine("条件", hydrogenation.conditions.joinToString("、"))
    OrganicLine("守恒", if (hydrogenation.conservation().balanced) "元素和电荷守恒" else "不守恒")

    Spacer(Modifier.height(16.dp))
    Text(
        "可试：C=C、CC#C、CC(=O)O、CC(=O)OCC、c1ccccc1、CCO 与 COC。底层不会根据任意底物猜产物，只执行教材明确启用且通过守恒校验的反应模板。",
        color = InteractiveMuted,
        fontSize = 13.sp,
        lineHeight = 20.sp,
    )
}

@Composable
private fun OrganicMoleculeSummary(molecule: OrganicMolecule) {
    OrganicMoleculeCanvas(molecule)
    Spacer(Modifier.height(14.dp))
    OrganicLine("分子式", molecule.molecularFormula())
    OrganicLine("原子 / 化学键", "${molecule.atoms.size} / ${molecule.bonds.size}")
    OrganicLine(
        "官能团",
        FunctionalGroupDetector.detect(molecule).joinToString("、") { it.type.label }.ifBlank { "未识别" },
    )
    val skeleton = molecule.carbonSkeleton()
    OrganicLine("最长碳链", if (skeleton.longestChain.isEmpty()) "无" else "${skeleton.longestChain.size} 个碳")
    OrganicLine("价态检查", if (molecule.validateValence().isEmpty()) "通过" else molecule.validateValence().joinToString { it.message })
}

@Composable
private fun OrganicMoleculeCanvas(molecule: OrganicMolecule) {
    val layout = OrganicMoleculeLayout.layout(molecule)
    val positions = layout.associateBy { it.atomId }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(InteractivePanel.copy(alpha = 0.26f)),
    ) {
        fun point(atomId: com.majortomman.school.learning.science.chemistry.organic.OrganicAtomId): Offset {
            val position = positions.getValue(atomId)
            return Offset(position.x.toFloat() * size.width, position.y.toFloat() * size.height)
        }

        molecule.bonds.forEach { bond ->
            val start = point(bond.atomA)
            val end = point(bond.atomB)
            val dx = end.x - start.x
            val dy = end.y - start.y
            val length = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
            val perpendicular = Offset(-dy / length * 4.dp.toPx(), dx / length * 4.dp.toPx())
            when (bond.order) {
                OrganicBondOrder.SINGLE,
                OrganicBondOrder.AROMATIC,
                -> drawLine(
                    if (bond.order == OrganicBondOrder.AROMATIC) InteractiveYellow else InteractiveWhite.copy(alpha = 0.72f),
                    start,
                    end,
                    2.dp.toPx(),
                    StrokeCap.Round,
                )
                OrganicBondOrder.DOUBLE -> {
                    drawLine(InteractiveWhite.copy(alpha = 0.78f), start + perpendicular, end + perpendicular, 2.dp.toPx())
                    drawLine(InteractiveWhite.copy(alpha = 0.78f), start - perpendicular, end - perpendicular, 2.dp.toPx())
                }
                OrganicBondOrder.TRIPLE -> {
                    drawLine(InteractiveWhite.copy(alpha = 0.78f), start, end, 2.dp.toPx())
                    drawLine(InteractiveWhite.copy(alpha = 0.72f), start + perpendicular, end + perpendicular, 1.5.dp.toPx())
                    drawLine(InteractiveWhite.copy(alpha = 0.72f), start - perpendicular, end - perpendicular, 1.5.dp.toPx())
                }
            }
        }

        molecule.atoms.forEach { atom ->
            val center = point(atom.id)
            val color = when (atom.element.symbol) {
                "C" -> InteractiveWhite
                "O" -> InteractiveRed
                "N" -> InteractiveBlue
                "S" -> InteractiveYellow
                "H" -> InteractiveMuted
                else -> InteractiveGreen
            }
            drawCircle(InteractiveBlack, 14.dp.toPx(), center)
            drawCircle(color.copy(alpha = 0.32f), 13.dp.toPx(), center)
            val paint = Paint().apply {
                isAntiAlias = true
                textSize = 13.sp.toPx()
                this.color = color.toArgb()
                textAlign = Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val baseline = center.y - (paint.ascent() + paint.descent()) / 2f
            drawContext.canvas.nativeCanvas.drawText(atom.element.symbol, center.x, baseline, paint)
        }
    }
}

@Composable
private fun OrganicInput(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = InteractiveMuted, fontSize = 12.sp)
        Spacer(Modifier.height(7.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = InteractiveWhite, fontSize = 21.sp),
            cursorBrush = SolidColor(InteractivePurple),
            singleLine = true,
        )
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveLine))
    }
}

@Composable
private fun OrganicLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = InteractiveMuted, fontSize = 13.sp, modifier = Modifier.weight(0.32f))
        Text(
            value,
            color = InteractiveWhite,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.68f),
        )
    }
}

@Composable
private fun OrganicError(message: String) {
    Text(message, color = InteractiveRed, fontSize = 14.sp, lineHeight = 21.sp)
}
