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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.learning.language.EnglishForm
import com.majortomman.school.learning.language.EnglishLexeme
import com.majortomman.school.learning.language.EnglishWordFormVerifier
import com.majortomman.school.learning.language.JapaneseForm
import com.majortomman.school.learning.language.JapaneseLanguageVerifier
import com.majortomman.school.learning.language.JapaneseLexeme
import com.majortomman.school.learning.language.JapaneseParticleRule
import com.majortomman.school.learning.language.LanguageAnswerPolicy
import com.majortomman.school.learning.language.LanguageAnswerVerifier
import com.majortomman.school.learning.language.LanguageToken
import com.majortomman.school.learning.language.TokenRole
import com.majortomman.school.learning.verification.DiagnosticResult
import com.majortomman.school.learning.verification.VerificationStatus

private val EnglishPlayLexeme = EnglishLexeme(
    lemma = "play",
    forms = mapOf(
        EnglishForm.BASE to "play",
        EnglishForm.THIRD_PERSON_SINGULAR to "plays",
        EnglishForm.PAST to "played",
        EnglishForm.PAST_PARTICIPLE to "played",
        EnglishForm.GERUND to "playing",
    ),
)

private val JapaneseIkuLexeme = JapaneseLexeme(
    dictionaryForm = "行く",
    reading = "いく",
    forms = mapOf(
        JapaneseForm.DICTIONARY to "行く",
        JapaneseForm.POLITE_PRESENT to "行きます",
        JapaneseForm.POLITE_NEGATIVE to "行きません",
        JapaneseForm.POLITE_PAST to "行きました",
        JapaneseForm.POLITE_PAST_NEGATIVE to "行きませんでした",
    ),
)

private val SchoolDirectionParticleRule = JapaneseParticleRule(
    preferred = "へ",
    accepted = setOf("へ", "に"),
    explanationByParticle = mapOf(
        "へ" to "「へ」表示移动方向，在这个助词位置读作「え」。",
        "に" to "「に」也可表示到达点；本样板优先展示教材常见的「学校へ行きます」。",
        "を" to "「を」通常标记动作直接涉及的对象，不适合这里的移动方向。",
        "で" to "「で」常用于动作发生的场所，不表示这里的移动目标。",
    ),
)

@Composable
internal fun EnglishLanguageLabSample() {
    var selectedForm by rememberSaveable { mutableStateOf("plays") }
    var answer by rememberSaveable { mutableStateOf("I'm a student.") }
    val formResult = EnglishWordFormVerifier.verify(
        lexeme = EnglishPlayLexeme,
        requestedForm = EnglishForm.THIRD_PERSON_SINGULAR,
        actual = selectedForm,
    )
    val answerResult = LanguageAnswerVerifier.verifyText(
        expected = "I am a student.",
        actual = answer,
        policy = LanguageAnswerPolicy(
            ignoreCase = true,
            ignorePunctuation = true,
            contractionEquivalents = mapOf("I'm" to "I am"),
        ),
    )

    SectionTitle("英语句子、词形与等价表达", InteractiveBlue)
    Spacer(Modifier.height(12.dp))
    Text(
        "技术样板按教材配置的词语和句型做确定性验证，不尝试猜任意自然语言。当前验证第三人称单数词形，以及 I am / I'm 的可接受等价表达。",
        color = InteractiveMuted,
        fontSize = 15.sp,
        lineHeight = 23.sp,
    )
    Spacer(Modifier.height(22.dp))

    Text("句子结构轨道", color = InteractiveBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        LanguageRoleSegment("主语", "He", Modifier.weight(0.75f))
        LanguageRoleSegment("谓语", selectedForm, Modifier.weight(0.9f))
        LanguageRoleSegment("宾语", "basketball", Modifier.weight(1.25f))
        LanguageRoleSegment("时间", "after school", Modifier.weight(1.35f))
    }
    Spacer(Modifier.height(18.dp))

    Text("选择 play 的当前词形", color = InteractiveMuted, fontSize = 13.sp)
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        listOf("play", "plays", "played").forEach { form ->
            LanguageFlatChoice(
                label = form,
                selected = selectedForm == form,
                color = InteractiveBlue,
                modifier = Modifier.weight(1f),
            ) { selectedForm = form }
        }
    }
    Spacer(Modifier.height(16.dp))
    LanguageDiagnosticBlock("词形诊断", formResult, InteractiveBlue)

    Spacer(Modifier.height(30.dp))
    Text("可接受表达验证", color = InteractiveBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(
        "目标含义：我是学生。当前题目允许 I am 与 I'm 两种写法，并忽略句末标点差异。",
        color = InteractiveMuted,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    )
    Spacer(Modifier.height(14.dp))
    LanguageTextInput(
        label = "输入英语句子",
        value = answer,
        color = InteractiveBlue,
        onValueChange = { answer = it.take(80) },
    )
    Spacer(Modifier.height(16.dp))
    LanguageDiagnosticBlock("表达诊断", answerResult, InteractiveBlue)
}

@Composable
internal fun JapaneseLanguageLabSample() {
    var showReading by rememberSaveable { mutableStateOf(true) }
    var particle by rememberSaveable { mutableStateOf("へ") }
    var verbForm by rememberSaveable { mutableStateOf("行きます") }
    val particleResult = JapaneseLanguageVerifier.verifyParticle(SchoolDirectionParticleRule, particle)
    val formResult = JapaneseLanguageVerifier.verifyForm(
        lexeme = JapaneseIkuLexeme,
        requestedForm = JapaneseForm.POLITE_PRESENT,
        actual = verbForm,
    )
    val sentence = "私は学校${particle}${verbForm}。"
    val sentenceCorrect = particleResult.status == VerificationStatus.CORRECT &&
        formResult.status == VerificationStatus.CORRECT

    val tokens = listOf(
        LanguageToken("watashi", "私", reading = "わたし", romanization = "watashi", meaning = "我", role = TokenRole.SUBJECT),
        LanguageToken("wa", "は", reading = "わ", romanization = "wa", meaning = "主题助词", role = TokenRole.PARTICLE),
        LanguageToken("gakkou", "学校", reading = "がっこう", romanization = "gakkō", meaning = "学校", role = TokenRole.PLACE),
        LanguageToken("direction", particle, reading = if (particle == "へ") "え" else particle, role = TokenRole.PARTICLE),
        LanguageToken("iku", verbForm, reading = japaneseVerbReading(verbForm), meaning = "去", role = TokenRole.PREDICATE),
    )

    SectionTitle("日语读音、助词与活用", InteractiveYellow)
    Spacer(Modifier.height(12.dp))
    Text(
        "当前样板把汉字、假名读音、助词和动词活用分开保存。读音只是显示层；助词与活用由教材配置的规则分别验证。",
        color = InteractiveMuted,
        fontSize = 15.sp,
        lineHeight = 23.sp,
    )
    Spacer(Modifier.height(18.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("句子分段", color = InteractiveYellow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(
            if (showReading) "隐藏读音" else "显示读音",
            modifier = Modifier.clickable { showReading = !showReading }.padding(vertical = 8.dp),
            color = InteractiveYellow,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
        tokens.forEach { token ->
            JapaneseTokenSegment(
                token = token,
                showReading = showReading,
                modifier = Modifier.weight(japaneseTokenWeight(token.surface)),
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    Text(
        sentence,
        color = if (sentenceCorrect) InteractiveWhite else InteractiveYellow,
        fontSize = 25.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Medium,
    )
    Spacer(Modifier.height(24.dp))

    Text("移动方向使用哪个助词", color = InteractiveMuted, fontSize = 13.sp)
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        listOf("へ", "に", "を", "で").forEach { option ->
            LanguageFlatChoice(
                label = option,
                selected = particle == option,
                color = InteractiveYellow,
                modifier = Modifier.weight(1f),
            ) { particle = option }
        }
    }
    Spacer(Modifier.height(14.dp))
    LanguageDiagnosticBlock("助词诊断", particleResult, InteractiveYellow)

    Spacer(Modifier.height(28.dp))
    Text("对老师礼貌说明现在要去学校", color = InteractiveMuted, fontSize = 13.sp)
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        listOf("行く", "行きます", "行きました").forEach { option ->
            LanguageFlatChoice(
                label = option,
                selected = verbForm == option,
                color = InteractiveYellow,
                modifier = Modifier.weight(1f),
            ) { verbForm = option }
        }
    }
    Spacer(Modifier.height(14.dp))
    LanguageDiagnosticBlock("活用与语体诊断", formResult, InteractiveYellow)
}

@Composable
private fun LanguageRoleSegment(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = InteractiveMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(7.dp))
        Text(
            value,
            color = InteractiveWhite,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(9.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).background(InteractiveBlue.copy(alpha = 0.68f)))
    }
}

@Composable
private fun JapaneseTokenSegment(token: LanguageToken, showReading: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (showReading) token.reading.orEmpty() else " ",
            color = InteractiveMuted,
            fontSize = 10.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            token.surface,
            color = InteractiveWhite,
            fontSize = 19.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).background(InteractiveYellow.copy(alpha = 0.62f)))
    }
}

@Composable
private fun LanguageFlatChoice(
    label: String,
    selected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            color = if (selected) color else InteractiveMuted,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(9.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(if (selected) 2.dp else 1.dp)
                .background(if (selected) color else InteractiveLine),
        )
    }
}

@Composable
private fun LanguageTextInput(
    label: String,
    value: String,
    color: Color,
    onValueChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = InteractiveMuted, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = InteractiveWhite, fontSize = 21.sp, lineHeight = 29.sp),
            cursorBrush = SolidColor(color),
            singleLine = false,
        )
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveLine))
    }
}

@Composable
private fun LanguageDiagnosticBlock(title: String, result: DiagnosticResult, color: Color) {
    val statusColor = when (result.status) {
        VerificationStatus.CORRECT -> InteractiveGreen
        VerificationStatus.INCORRECT -> InteractiveRed
        VerificationStatus.UNSUPPORTED -> InteractiveYellow
        VerificationStatus.INPUT_IN_PROGRESS,
        VerificationStatus.READY,
        -> color
    }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(statusColor.copy(alpha = 0.48f)))
        Spacer(Modifier.height(4.dp))
        Text(title, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        result.steps.forEach { step ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(step.title, color = InteractiveMuted, fontSize = 12.sp, modifier = Modifier.weight(0.36f))
                Text(
                    step.expression,
                    color = when (step.correct) {
                        true -> InteractiveGreen
                        false -> InteractiveRed
                        null -> InteractiveWhite
                    },
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.weight(0.64f),
                    textAlign = TextAlign.End,
                )
            }
        }
        Text(result.message, color = InteractiveWhite.copy(alpha = 0.82f), fontSize = 14.sp, lineHeight = 21.sp)
    }
}

private fun japaneseVerbReading(form: String): String = when (form) {
    "行く" -> "いく"
    "行きます" -> "いきます"
    "行きました" -> "いきました"
    else -> ""
}

private fun japaneseTokenWeight(surface: String): Float = when {
    surface.length >= 4 -> 1.65f
    surface.length >= 2 -> 1.25f
    else -> 0.75f
}
