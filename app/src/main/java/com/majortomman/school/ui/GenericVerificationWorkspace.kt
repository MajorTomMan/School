package com.majortomman.school.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.ai.OpenAiCompatibleClient
import com.majortomman.school.ai.VerificationImage
import com.majortomman.school.data.AiSettings
import com.majortomman.school.learning.verification.AnswerVerificationMethod
import com.majortomman.school.learning.verification.AnswerVerificationRequest
import com.majortomman.school.learning.verification.AnswerVerificationResult
import com.majortomman.school.learning.verification.AnswerVerificationVerdict
import com.majortomman.school.learning.verification.DeterministicAnswerVerifier
import com.majortomman.school.learning.verification.VerificationHubCatalog
import com.majortomman.school.learning.verification.VerificationSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun GenericVerificationWorkspace(aiSettings: AiSettings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var subjectName by rememberSaveable { mutableStateOf(AUTO_SUBJECT) }
    var question by rememberSaveable { mutableStateOf("") }
    var workProcess by rememberSaveable { mutableStateOf("") }
    var answer by rememberSaveable { mutableStateOf("") }
    var referenceAnswer by rememberSaveable { mutableStateOf("") }
    var questionImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var answerImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<AnswerVerificationResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val questionImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        questionImageUri = uri?.toString()
        result = null
        error = null
    }
    val answerImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        answerImageUri = uri?.toString()
        result = null
        error = null
    }
    val subjectHint = subjectName.takeUnless { it == AUTO_SUBJECT }?.let(VerificationSubject::valueOf)
    val canVerify = !busy && (question.isNotBlank() || questionImageUri != null) && (answer.isNotBlank() || answerImageUri != null)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("通用验证", color = InteractiveWhite, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text("输入任意题目、做题过程和最终答案。文字能确定判断时优先本地验证；图片、开放题或跨学科内容再使用 AI。", color = InteractiveMuted, fontSize = 14.sp, lineHeight = 22.sp)

        Text("学科提示", color = InteractiveWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        VerificationSubjectSelector(subjectName) {
            subjectName = it
            result = null
            error = null
        }

        VerificationMultilineField("题目", "粘贴题目文字；也可以只添加题目图片。", question, 112) {
            question = it.take(8_000)
            result = null
        }
        VerificationImagePicker("题目图片", questionImageUri, { questionImageLauncher.launch("image/*") }) {
            questionImageUri = null
            result = null
        }

        VerificationMultilineField("做题过程（可选）", "写下计算、推导、论证、翻译过程或判断依据。", workProcess, 112) {
            workProcess = it.take(12_000)
            result = null
        }

        VerificationMultilineField("最终答案", "写最终答案；如果答案只存在于手写图片中，也可以留空。", answer, 72) {
            answer = it.take(8_000)
            result = null
        }
        VerificationImagePicker("答案 / 作答图片", answerImageUri, { answerImageLauncher.launch("image/*") }) {
            answerImageUri = null
            result = null
        }

        VerificationMultilineField("参考答案（可选）", "有标准答案就填；没有也可以让验证器独立求解核对。", referenceAnswer, 64) {
            referenceAnswer = it.take(8_000)
            result = null
        }

        Box(
            modifier = Modifier.fillMaxWidth().height(50.dp).clickable(enabled = canVerify) {
                val request = AnswerVerificationRequest(
                    subjectHint = subjectHint,
                    question = question.trim(),
                    workProcess = workProcess.trim(),
                    answer = answer.trim(),
                    referenceAnswer = referenceAnswer.trim().ifBlank { null },
                )
                result = null
                error = null
                scope.launch {
                    busy = true
                    runCatching {
                        val hasImages = questionImageUri != null || answerImageUri != null
                        val deterministic = if (!hasImages) DeterministicAnswerVerifier.verify(request) else null
                        if (deterministic != null) deterministic else {
                            require(aiSettings.endpoint.isNotBlank() && aiSettings.model.isNotBlank()) { "当前内容需要 AI 验证，请先在设置中配置 AI 接口和模型。" }
                            val questionImages = questionImageUri?.let { listOf(readVerificationImage(context, Uri.parse(it))) }.orEmpty()
                            val answerImages = answerImageUri?.let { listOf(readVerificationImage(context, Uri.parse(it))) }.orEmpty()
                            OpenAiCompatibleClient(aiSettings).verifyGenericAnswer(request, questionImages, answerImages)
                        }
                    }.fold(onSuccess = { result = it }, onFailure = { error = it.message ?: "验证失败" })
                    busy = false
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            if (busy) CircularProgressIndicator(modifier = Modifier.height(22.dp), color = InteractiveBlue, strokeWidth = 2.dp)
            else Text("验证答案", color = if (canVerify) InteractiveBlue else InteractiveMuted.copy(alpha = 0.42f), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(if (canVerify) 2.dp else 1.dp).background(if (canVerify) InteractiveBlue else InteractiveLine))
        }

        error?.let { Text(it, color = InteractiveRed, fontSize = 13.sp, lineHeight = 20.sp) }
        result?.let(::VerificationResultPanel)
    }
}

@Composable
private fun VerificationSubjectSelector(selected: String, onSelected: (String) -> Unit) {
    val items = listOf(AUTO_SUBJECT to "自动") + VerificationHubCatalog.subjects.map { it.name to it.label }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { (value, label) ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f).clickable { onSelected(value) }.padding(vertical = 8.dp),
                        color = if (selected == value) InteractiveBlue else InteractiveMuted,
                        fontSize = 13.sp,
                        fontWeight = if (selected == value) FontWeight.Bold else FontWeight.Normal,
                    )
                }
                repeat(4 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun VerificationMultilineField(label: String, hint: String, value: String, minHeight: Int, onValueChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, color = InteractiveWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = minHeight.dp),
            textStyle = TextStyle(color = InteractiveWhite, fontSize = 15.sp, lineHeight = 22.sp),
            cursorBrush = SolidColor(InteractiveBlue),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    if (value.isBlank()) Text(hint, color = InteractiveMuted.copy(alpha = 0.62f), fontSize = 13.sp, lineHeight = 20.sp)
                    inner()
                }
            },
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveLine))
    }
}

@Composable
private fun VerificationImagePicker(label: String, uri: String?, onPick: () -> Unit, onClear: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, color = InteractiveWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (uri == null) "添加图片" else "更换图片", modifier = Modifier.clickable(onClick = onPick).padding(vertical = 7.dp), color = InteractiveBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (uri != null) {
                Text("已选择", color = InteractiveGreen, fontSize = 12.sp)
                Text("移除", modifier = Modifier.clickable(onClick = onClear).padding(vertical = 7.dp), color = InteractiveMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun VerificationResultPanel(result: AnswerVerificationResult) {
    val color = when (result.verdict) {
        AnswerVerificationVerdict.CORRECT -> InteractiveGreen
        AnswerVerificationVerdict.INCORRECT -> InteractiveRed
        AnswerVerificationVerdict.PARTIALLY_CORRECT, AnswerVerificationVerdict.AMBIGUOUS -> InteractiveYellow
        AnswerVerificationVerdict.UNSUPPORTED -> InteractiveMuted
    }
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(color.copy(alpha = 0.72f)))
        Text(
            when (result.verdict) {
                AnswerVerificationVerdict.CORRECT -> "验证通过"
                AnswerVerificationVerdict.INCORRECT -> "答案不正确"
                AnswerVerificationVerdict.PARTIALLY_CORRECT -> "部分正确"
                AnswerVerificationVerdict.AMBIGUOUS -> "暂时无法唯一判断"
                AnswerVerificationVerdict.UNSUPPORTED -> "当前无法可靠验证"
            },
            color = color,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
        Text("${result.subject} · ${if (result.method == AnswerVerificationMethod.DETERMINISTIC) "本地确定性验证" else "AI 验证"}", color = InteractiveMuted, fontSize = 12.sp)
        Text(result.feedback, color = InteractiveWhite, fontSize = 14.sp, lineHeight = 22.sp)
        result.referenceAnswer?.let { Text("参考答案：$it", color = InteractiveYellow, fontSize = 14.sp, lineHeight = 22.sp) }
        if (result.explanation.isNotBlank()) Text(result.explanation, color = InteractiveWhite.copy(alpha = 0.82f), fontSize = 13.sp, lineHeight = 21.sp)
        result.limitation?.let { Text("边界：$it", color = InteractiveMuted, fontSize = 12.sp, lineHeight = 19.sp) }
    }
}

private suspend fun readVerificationImage(context: android.content.Context, uri: Uri): VerificationImage = withContext(Dispatchers.IO) {
    val mediaType = context.contentResolver.getType(uri) ?: error("无法识别图片格式")
    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) continue
            total += count
            require(total <= VerificationImage.MAX_IMAGE_BYTES) { "单张验证图片不能超过 ${VerificationImage.MAX_IMAGE_BYTES / 1024 / 1024}MB" }
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    } ?: error("无法读取图片")
    VerificationImage(bytes, mediaType)
}

private const val AUTO_SUBJECT = "AUTO"
