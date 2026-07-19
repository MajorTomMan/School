package com.majortomman.school.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class RationalExample(
    val id: String,
    val value: String,
    val position: Float,
    val signLabel: String,
    val formLabel: String,
    val pointColor: Color,
)

private val rationalExamples = listOf(
    RationalExample("negative_integer", "−2", -2f, "负数", "整数", InteractiveBlue),
    RationalExample("negative_fraction", "−1/2", -0.5f, "负数", "分数", InteractiveYellow),
    RationalExample("zero", "0", 0f, "0", "整数", InteractiveWhite),
    RationalExample("positive_fraction", "1/3", 1f / 3f, "正数", "分数", InteractiveBlue),
    RationalExample("positive_integer", "2", 2f, "正数", "整数", InteractiveBlue),
)

/**
 * 用具体数字同时呈现“按符号分类”和“按表示形式分类”，再把同一组数字放回数轴。
 * 页面先建立数字直觉，再总结分类标准，避免让学习者先解读抽象的二维矩阵。
 */
@Composable
internal fun RationalExamplesVisual() {
    var selectedId by rememberSaveable { mutableStateOf("negative_fraction") }
    val selected = rationalExamples.first { it.id == selectedId }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "有理数示例与分类",
                color = InteractiveWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "同一个数可以按不同标准分类",
                color = InteractiveMuted,
                fontSize = 9.sp,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            rationalExamples.forEach { example ->
                RationalExampleTile(
                    example = example,
                    selected = example.id == selectedId,
                    modifier = Modifier.weight(1f),
                    onSelect = { selectedId = example.id },
                )
            }
        }

        Text(
            text = "${selected.value}：按符号是${selected.signLabel}，按表示形式是${selected.formLabel}。",
            modifier = Modifier.fillMaxWidth(),
            color = InteractiveMuted,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(1.dp))
        Text(
            text = "数轴上的位置",
            color = InteractiveWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val left = 24f
            val right = size.width - 24f
            val axisY = size.height * 0.43f
            val min = -2.5f
            val max = 2.5f
            fun xFor(value: Float): Float = left + (value - min) / (max - min) * (right - left)

            drawLine(
                color = InteractiveWhite.copy(alpha = 0.78f),
                start = Offset(left, axisY),
                end = Offset(right, axisY),
                strokeWidth = 3f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = InteractiveWhite.copy(alpha = 0.78f),
                start = Offset(right, axisY),
                end = Offset(right - 12f, axisY - 8f),
                strokeWidth = 3f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = InteractiveWhite.copy(alpha = 0.78f),
                start = Offset(right, axisY),
                end = Offset(right - 12f, axisY + 8f),
                strokeWidth = 3f,
                cap = StrokeCap.Round,
            )

            (-2..2).forEach { tick ->
                val x = xFor(tick.toFloat())
                drawLine(
                    color = InteractiveWhite.copy(alpha = if (tick == 0) 0.72f else 0.35f),
                    start = Offset(x, axisY - if (tick == 0) 10f else 7f),
                    end = Offset(x, axisY + if (tick == 0) 10f else 7f),
                    strokeWidth = if (tick == 0) 3f else 2f,
                )
            }

            rationalExamples.forEachIndexed { index, example ->
                val x = xFor(example.position)
                val active = example.id == selectedId
                drawCircle(
                    color = example.pointColor.copy(alpha = if (active) 1f else 0.72f),
                    radius = if (active) 9f else 7f,
                    center = Offset(x, axisY),
                )
                drawRationalLabel(
                    text = example.value,
                    x = x,
                    y = axisY + if (index % 2 == 0) 38f else 62f,
                    color = if (active) example.pointColor else InteractiveWhite.copy(alpha = 0.78f),
                    textSize = if (active) 23f else 20f,
                )
            }
        }
    }
}

@Composable
private fun RationalExampleTile(
    example: RationalExample,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
) {
    val shape = RoundedCornerShape(6.dp)
    Column(
        modifier = modifier
            .background(
                if (selected) example.pointColor.copy(alpha = 0.09f) else InteractivePanel.copy(alpha = 0.18f),
                shape,
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) example.pointColor.copy(alpha = 0.9f) else InteractiveLine,
                shape = shape,
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 3.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = example.value,
            color = InteractiveWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            RationalTag(example.signLabel, InteractiveBlue)
            RationalTag(example.formLabel, InteractiveYellow)
        }
    }
}

@Composable
private fun RationalTag(label: String, color: Color) {
    Box(
        modifier = Modifier
            .border(1.dp, color.copy(alpha = 0.42f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRationalLabel(
    text: String,
    x: Float,
    y: Float,
    color: Color,
    textSize: Float,
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        this.textSize = textSize
        textAlign = Paint.Align.CENTER
    }
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
}
