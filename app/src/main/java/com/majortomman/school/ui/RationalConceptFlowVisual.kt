package com.majortomman.school.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.learning.course.CourseSceneData

private data class RationalFormExample(
    val id: String,
    val tabLabel: String,
    val display: String,
    val sourceKind: String,
    val numerator: String,
    val denominator: String,
)

private val integerFractionExamples = listOf(
    RationalFormExample("positive_integer", "2", "2", "正整数", "2", "1"),
    RationalFormExample("negative_integer", "−3", "−3", "负整数", "−3", "1"),
    RationalFormExample("zero", "0", "0", "0", "0", "1"),
)

private val rationalDefinitionExamples = integerFractionExamples + listOf(
    RationalFormExample("finite_decimal", "0.5", "0.5", "有限小数", "1", "2"),
    RationalFormExample("repeating_decimal", "0.3循环", "0.3（3循环）", "无限循环小数", "1", "3"),
)

/**
 * “有理数的概念”原创交互。使用连续画布、等号和引导线表达关系，不使用卡片容器。
 */
@Composable
internal fun RationalConceptFlowVisual(data: CourseSceneData) {
    val definitionMode = data.string("mode") == "definition"
    val examples = if (definitionMode) rationalDefinitionExamples else integerFractionExamples
    var selectedId by rememberSaveable(data.string("mode")) {
        mutableStateOf(examples.first().id)
    }
    val selected = examples.firstOrNull { it.id == selectedId } ?: examples.first()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "点选一种写法，观察它怎样保持数值不变地写成分数",
            modifier = Modifier.fillMaxWidth(),
            color = InteractiveMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            examples.forEach { example ->
                val active = example.id == selected.id
                val indicatorColor by animateColorAsState(
                    targetValue = if (active) InteractiveBlue else InteractiveLine,
                    animationSpec = tween(durationMillis = 180),
                    label = "rational-tab-${example.id}",
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedId = example.id }
                        .padding(horizontal = 2.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = example.tabLabel,
                        color = if (active) InteractiveWhite else InteractiveMuted,
                        fontSize = if (definitionMode) 12.sp else 15.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(5.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(if (active) 2.dp else 1.dp)
                            .background(indicatorColor),
                    )
                }
            }
        }

        AnimatedContent(
            targetState = selected,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            transitionSpec = {
                (fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 8 })
                    .togetherWith(fadeOut(tween(120)) + slideOutVertically(tween(160)) { -it / 10 })
            },
            label = "rational-form-transition",
        ) { example ->
            RationalRelationship(
                example = example,
                definitionMode = definitionMode,
            )
        }

        Text(
            text = if (definitionMode) {
                "共同形式：a/b（a、b为整数，b≠0）"
            } else {
                "把整数写成分母为1的分数，数值不会改变。"
            },
            modifier = Modifier.fillMaxWidth(),
            color = InteractiveMuted,
            fontSize = 11.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RationalRelationship(
    example: RationalFormExample,
    definitionMode: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = example.sourceKind,
            color = InteractiveMuted,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveLine))
        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = example.display,
                color = InteractiveWhite,
                fontSize = if (example.id == "repeating_decimal") 22.sp else 31.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "  =  ",
                color = InteractiveBlue,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
            )
            FractionForm(
                numerator = example.numerator,
                denominator = example.denominator,
            )
        }

        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveLine))
        Spacer(Modifier.height(10.dp))
        Text(
            text = "左右两边表示同一个数",
            color = InteractiveBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )

        if (definitionMode) {
            Text(
                text = "↓  可以写成两个整数之比",
                color = InteractiveMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 12.dp, bottom = 7.dp),
            )
            Text(
                text = "有理数",
                color = InteractiveYellow,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Box(
                Modifier
                    .width(76.dp)
                    .height(2.dp)
                    .background(InteractiveYellow.copy(alpha = 0.76f)),
            )
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = if (definitionMode) {
                "${example.display}可以写成${example.numerator}/${example.denominator}，因此它属于有理数。"
            } else {
                "给${example.display}补上分母1，只改变写法，不改变大小。"
            },
            modifier = Modifier.fillMaxWidth(),
            color = InteractiveWhite.copy(alpha = 0.82f),
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FractionForm(
    numerator: String,
    denominator: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = numerator,
            color = InteractiveYellow,
            fontSize = 25.sp,
            lineHeight = 27.sp,
            fontWeight = FontWeight.Medium,
        )
        Box(
            Modifier
                .width(64.dp)
                .height(2.dp)
                .background(InteractiveYellow),
        )
        Text(
            text = denominator,
            color = InteractiveYellow,
            fontSize = 25.sp,
            lineHeight = 27.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
