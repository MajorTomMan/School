package com.majortomman.school.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.data.AiSettings
import com.majortomman.school.data.PreferencesRepository
import com.majortomman.school.learning.verification.VerificationHubCatalog
import com.majortomman.school.learning.verification.VerificationSubject

@Composable
internal fun VerificationHubScreen() {
    val context = LocalContext.current
    val settingsFlow = remember(context) { PreferencesRepository(context.applicationContext).aiSettings }
    val aiSettings by settingsFlow.collectAsState(initial = AiSettings())
    var specializedVisible by rememberSaveable { mutableStateOf(false) }
    var selectedName by rememberSaveable { mutableStateOf(VerificationSubject.MATHEMATICS.name) }
    val selected = VerificationSubject.valueOf(selectedName)
    val capability = VerificationHubCatalog.capability(selected)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(InteractiveBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text("验证", color = InteractiveWhite, fontSize = 38.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "验证任意题目的答案、做题过程或作答图片。系统优先使用本地确定性规则；需要理解图片或开放内容时才使用你配置的 AI。",
            color = InteractiveMuted,
            fontSize = 15.sp,
            lineHeight = 23.sp,
        )
        Spacer(Modifier.height(24.dp))
        GenericVerificationWorkspace(aiSettings)
        Spacer(Modifier.height(34.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveLine))
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clickable { specializedVisible = !specializedVisible }.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("专项验证工具", color = InteractiveWhite, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(if (specializedVisible) "收起" else "展开", color = InteractiveBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Text("公式、复平面、物理模型、化学与语言规则实验仍保留在这里；它们不是通用验证入口。", color = InteractiveMuted, fontSize = 12.sp, lineHeight = 19.sp)

        if (specializedVisible) {
            Spacer(Modifier.height(18.dp))
            VerificationHubCatalog.subjects.chunked(2).forEach { rowItems ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    rowItems.forEach { subject ->
                        VerificationSubjectChoice(subject, subject == selected, Modifier.weight(1f)) { selectedName = subject.name }
                    }
                    if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(14.dp))
            VerificationBoundary(capability.deterministic, capability.limitation)
            Spacer(Modifier.height(30.dp))
            when (selected) {
                VerificationSubject.MATHEMATICS -> MathematicsVerificationPanel()
                VerificationSubject.PHYSICS -> PhysicsVerificationPanel()
                VerificationSubject.CHEMISTRY -> ChemistryVerificationPanel()
                VerificationSubject.BIOLOGY -> BiologyVerificationPanel()
                VerificationSubject.ENGLISH -> LanguageVerificationPanel(english = true)
                VerificationSubject.JAPANESE -> LanguageVerificationPanel(english = false)
            }
        }
        Spacer(Modifier.height(52.dp))
    }
}

@Composable
private fun VerificationSubjectChoice(subject: VerificationSubject, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clickable(onClick = onClick).padding(vertical = 12.dp)) {
        Text(subject.label, color = if (selected) InteractiveBlue else InteractiveWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(subject.subtitle, color = InteractiveMuted, fontSize = 12.sp, lineHeight = 17.sp)
        Spacer(Modifier.height(10.dp))
        Box(modifier = Modifier.fillMaxWidth().height(if (selected) 2.dp else 1.dp).background(if (selected) InteractiveBlue else InteractiveLine))
    }
}

@Composable
private fun VerificationBoundary(deterministic: Boolean, limitation: String) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("判断边界", modifier = Modifier.weight(1f).padding(end = 12.dp), color = InteractivePurple, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(if (deterministic) "确定性计算" else "规则分析", color = if (deterministic) InteractiveGreen else InteractiveYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }
        Spacer(Modifier.height(8.dp))
        Text(limitation, color = InteractiveWhite.copy(alpha = 0.72f), fontSize = 14.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(InteractiveLine))
    }
}
