from pathlib import Path


def replace(path: str, old: str, new: str, count: int = 1) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    actual = text.count(old)
    if actual < count:
        raise SystemExit(f"{path}: expected at least {count} occurrence(s), found {actual}: {old[:100]!r}")
    file.write_text(text.replace(old, new, count), encoding="utf-8")


# Shared subject/textbook components: fixed heights must grow with text.
path = "app/src/main/java/com/majortomman/school/ui/SubjectTextbookCenterComponents.kt"
replace(path, "import androidx.compose.foundation.layout.height\n", "import androidx.compose.foundation.layout.height\nimport androidx.compose.foundation.layout.heightIn\n")
replace(path, ".height(82.dp)", ".heightIn(min = 82.dp)")
replace(path, ".height(48.dp)", ".heightIn(min = 48.dp)")
replace(
    path,
    """        fontSize = 13.sp,
    )
}

@Composable
internal fun CenterBack""",
    """        fontSize = 13.sp,
        maxLines = 1,
        softWrap = false,
    )
}

@Composable
internal fun CenterBack""",
)

# Subject selection: long labels own remaining width; trailing status never gets squeezed vertically.
path = "app/src/main/java/com/majortomman/school/ui/SubjectTextbookSelectionPages.kt"
replace(
    path,
    "Text(stage.label, color = SelectionWhite, fontSize = 31.sp, fontWeight = FontWeight.Medium)",
    "Text(stage.label, modifier = Modifier.weight(1f).padding(end = 16.dp), color = SelectionWhite, fontSize = 31.sp, fontWeight = FontWeight.Medium)",
)
replace(
    path,
    "Text(subject.title, color = SelectionWhite, fontSize = 28.sp, fontWeight = FontWeight.Medium)",
    "Text(subject.title, modifier = Modifier.weight(1f).padding(end = 16.dp), color = SelectionWhite, fontSize = 28.sp, fontWeight = FontWeight.Medium)",
)

# Verification hub: boundary status remains a compact trailing value.
path = "app/src/main/java/com/majortomman/school/ui/VerificationHubScreen.kt"
replace(
    path,
    '            Text("判断边界", color = InteractivePurple, fontSize = 13.sp, fontWeight = FontWeight.Bold)',
    '            Text("判断边界", modifier = Modifier.weight(1f).padding(end = 12.dp), color = InteractivePurple, fontSize = 13.sp, fontWeight = FontWeight.Bold)',
)
replace(
    path,
    """                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))""",
    """                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
            )
        }
        Spacer(Modifier.height(8.dp))""",
)

# Main bottom navigation: seven tabs get equal width and labels never turn vertical.
path = "app/src/main/java/com/majortomman/school/ui/SchoolApp.kt"
replace(
    path,
    """                modifier = Modifier
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 3.dp, vertical = 4.dp),""",
    """                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 1.dp, vertical = 4.dp),""",
)
replace(
    path,
    """                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )""",
    """                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    softWrap = false,
                )""",
)

# Today/fallback path headers: reserve width for trailing values instead of squeezing them.
path = "app/src/main/java/com/majortomman/school/ui/ScenePreviewScreens.kt"
replace(
    path,
    """                        text = "${plan.reviewItems.size} 项复习",
                        color =""",
    """                        text = "${plan.reviewItems.size} 项复习",
                        modifier = Modifier.weight(1f),
                        color =""",
)
replace(
    path,
    """                        text = "查看路径",
                        modifier = Modifier.clickable(onClick = onOpenPath),
                        color = SceneWhite.copy(alpha = 0.54f),
                        fontSize = 13.sp,
                    )""",
    """                        text = "查看路径",
                        modifier = Modifier.clickable(onClick = onOpenPath),
                        color = SceneWhite.copy(alpha = 0.54f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        softWrap = false,
                    )""",
)
replace(
    path,
    """                        text = "有理数",
                        color = SceneWhite,""",
    """                        text = "有理数",
                        modifier = Modifier.weight(1f).padding(end = 16.dp),
                        color = SceneWhite,""",
)
replace(
    path,
    """                        color = SceneWhite.copy(alpha = 0.38f),
                        fontSize = 14.sp,
                    )""",
    """                        color = SceneWhite.copy(alpha = 0.38f),
                        fontSize = 14.sp,
                        maxLines = 1,
                        softWrap = false,
                    )""",
)
replace(
    path,
    '            Text("继续", color = SceneBlue, fontSize = 13.sp)',
    '            Text("继续", color = SceneBlue, fontSize = 13.sp, maxLines = 1, softWrap = false)',
)

# Current curriculum path: let typography choose line height and protect the trailing action.
path = "app/src/main/java/com/majortomman/school/ui/CurriculumTreeScreen.kt"
replace(path, "                lineHeight = 24.sp,\n", "")
replace(
    path,
    '            Text("继续", color = TreeBlue, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp))',
    '            Text("继续", color = TreeBlue, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp), maxLines = 1, softWrap = false)',
)

# Question bank: vertical mode rows, growing input, weighted actions and protected status values.
path = "app/src/main/java/com/majortomman/school/ui/MathQuestionBankScreen.kt"
replace(path, "import androidx.compose.foundation.layout.height\n", "import androidx.compose.foundation.layout.height\nimport androidx.compose.foundation.layout.heightIn\n")
replace(
    path,
    """    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !disabled, onClick = onClick)
            .padding(vertical = 21.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.fillMaxWidth(0.72f)) {
            Text(mode.label, color = BankWhite, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(mode.description, color = BankMuted, fontSize = 13.sp, lineHeight = 19.sp)
        }
        Text(if (disabled) "准备中" else suffix, color = BankYellow, fontSize = 12.sp, textAlign = TextAlign.End)
    }""",
    """    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !disabled, onClick = onClick)
            .padding(vertical = 21.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(mode.label, color = BankWhite, fontSize = 24.sp, fontWeight = FontWeight.Medium)
        Text(mode.description, color = BankMuted, fontSize = 13.sp, lineHeight = 19.sp)
        Text(if (disabled) "准备中" else suffix, color = BankYellow, fontSize = 12.sp, maxLines = 2)
    }""",
)
replace(
    path,
    '            Text(title, color = BankWhite, fontSize = 16.sp)\n            Text("$percent% · $attempts 次", color = BankMuted, fontSize = 13.sp)',
    '            Text(title, modifier = Modifier.weight(1f).padding(end = 12.dp), color = BankWhite, fontSize = 16.sp)\n            Text("$percent% · $attempts 次", color = BankMuted, fontSize = 13.sp, maxLines = 1, softWrap = false)',
)
replace(
    path,
    '            Text("返回题库", color = BankMuted, fontSize = 14.sp, modifier = Modifier.clickable(onClick = onBack))\n            Text(mode.label, color = BankYellow, fontSize = 12.sp)',
    '            Text("返回题库", color = BankMuted, fontSize = 14.sp, modifier = Modifier.weight(1f).clickable(onClick = onBack))\n            Text(mode.label, color = BankYellow, fontSize = 12.sp, maxLines = 1, softWrap = false)',
)
replace(
    path,
    ".fillMaxWidth().height(if (question.type == MathQuestionType.STEP_BY_STEP) 150.dp else 56.dp)",
    ".fillMaxWidth().heightIn(min = if (question.type == MathQuestionType.STEP_BY_STEP) 150.dp else 56.dp)",
)
replace(
    path,
    """private fun BankAction(
    text: String,
    accent: Color,
    enabled: Boolean = true,""",
    """private fun BankAction(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,""",
)
replace(
    path,
    """        modifier = Modifier
            .border(1.dp, if (enabled) accent else BankLine, RoundedCornerShape(5.dp))""",
    """        modifier = modifier
            .border(1.dp, if (enabled) accent else BankLine, RoundedCornerShape(5.dp))""",
)
replace(
    path,
    """                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                BankAction(
                    text = if (hintLevel < question.hints.size) "提示 ${hintLevel + 1}" else "提示已展开",
                    accent = BankYellow,""",
    """                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BankAction(
                    text = if (hintLevel < question.hints.size) "提示 ${hintLevel + 1}" else "提示已展开",
                    accent = BankYellow,
                    modifier = Modifier.weight(1f),""",
)
replace(
    path,
    """                    accent = BankBlue,
                    enabled = !submitting""",
    """                    accent = BankBlue,
                    modifier = Modifier.weight(1f),
                    enabled = !submitting""",
)
replace(
    path,
    """                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                BankAction("返回题库", BankWhite, onClick = onBack)""",
    """                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BankAction("返回题库", BankWhite, modifier = Modifier.weight(1f), onClick = onBack)""",
)
replace(
    path,
    """                    BankBlue,
                    enabled = !loadingNext,""",
    """                    BankBlue,
                    modifier = Modifier.weight(1f),
                    enabled = !loadingNext,""",
)
replace(
    path,
    '        Text(text, color = if (enabled) accent else BankMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)',
    '        Text(text, color = if (enabled) accent else BankMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false)',
)

# Course storage settings: book titles own flexible width; sizes/status stay single-line.
path = "app/src/main/java/com/majortomman/school/ui/CourseStorageSettingsPage.kt"
replace(
    path,
    """                modifier = Modifier.clickable(
                    enabled = !checking""",
    """                modifier = Modifier.weight(1f).padding(end = 12.dp).clickable(
                    enabled = !checking""",
)
replace(
    path,
    '            downloadState.downloadLabel()?.let { Text(it, color = CourseSettingsMuted, fontSize = 12.sp) }',
    '            downloadState.downloadLabel()?.let { Text(it, color = CourseSettingsMuted, fontSize = 12.sp, maxLines = 1, softWrap = false) }',
)
replace(
    path,
    """            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(book.label""",
    """            Column(modifier = Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(book.label""",
)
replace(
    path,
    """                fontSize = 12.sp,
            )
        }
        if (confirmingDelete)""",
    """                fontSize = 12.sp,
                maxLines = 1,
                softWrap = false,
            )
        }
        if (confirmingDelete)""",
)

# PDF reader buttons grow instead of clipping at 130/150%.
path = "app/src/main/java/com/majortomman/school/ui/PdfTextbookScreen.kt"
replace(path, "import androidx.compose.foundation.layout.height\n", "import androidx.compose.foundation.layout.height\nimport androidx.compose.foundation.layout.heightIn\n")
replace(path, ".height(45.dp)\n            .clickable", ".heightIn(min = 45.dp)\n            .clickable")

# Update dialog: protect metadata/progress values; stack three available-update actions.
path = "app/src/main/java/com/majortomman/school/ui/UpdateDialog.kt"
replace(
    path,
    '                        Text("下载进度", color = UpdateMuted, fontSize = 13.sp)\n                        Text("${state.progress}%", color = UpdateBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)',
    '                        Text("下载进度", modifier = Modifier.weight(1f), color = UpdateMuted, fontSize = 13.sp)\n                        Text("${state.progress}%", color = UpdateBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)',
)
replace(
    path,
    """                is UpdateState.Available -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (!mandatory) {
                        Text("忽略此版本", Modifier.clickable { onIgnore(state.manifest) }, color = UpdateMuted, fontSize = 13.sp)
                        Text("稍后提醒", Modifier.clickable { onLater(state.manifest) }, color = UpdateWhite.copy(alpha = 0.72f), fontSize = 13.sp)
                    }
                    Text(
                        "下载并升级",
                        Modifier.clickable { onDownload(state.manifest) },
                        color = UpdateBlue,
                        fontWeight = FontWeight.Bold,
                    )
                }""",
    """                is UpdateState.Available -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (!mandatory) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("忽略此版本", Modifier.clickable { onIgnore(state.manifest) }, color = UpdateMuted, fontSize = 13.sp, maxLines = 1, softWrap = false)
                            Text("稍后提醒", Modifier.clickable { onLater(state.manifest) }, color = UpdateWhite.copy(alpha = 0.72f), fontSize = 13.sp, maxLines = 1, softWrap = false)
                        }
                    }
                    Text(
                        "下载并升级",
                        Modifier.clickable { onDownload(state.manifest) }.padding(vertical = 4.dp),
                        color = UpdateBlue,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                    )
                }""",
)
replace(
    path,
    '        Text(label, color = UpdateMuted, fontSize = 12.sp)\n        Text(value, color = UpdateWhite.copy(alpha = 0.82f), fontSize = 13.sp)',
    '        Text(label, modifier = Modifier.weight(1f).padding(end = 12.dp), color = UpdateMuted, fontSize = 12.sp)\n        Text(value, color = UpdateWhite.copy(alpha = 0.82f), fontSize = 13.sp, maxLines = 1, softWrap = false)',
)

# Review/legacy inputs: right-side statuses remain compact; text fields can grow.
path = "app/src/main/java/com/majortomman/school/ui/MinimalRemainingScreens.kt"
replace(path, "import androidx.compose.foundation.layout.height\n", "import androidx.compose.foundation.layout.height\nimport androidx.compose.foundation.layout.heightIn\n")
replace(
    path,
    '                Text("${progress.accuracyPercent}%", color = MinimalYellow, fontWeight = FontWeight.Bold)',
    '                Text("${progress.accuracyPercent}%", color = MinimalYellow, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)',
)
replace(path, "                Text(trailing, color = MinimalMuted)", "                Text(trailing, color = MinimalMuted, maxLines = 1, softWrap = false)")
replace(
    path,
    '            Text(if (item.correct) "正确" else "复习", color = color)',
    '            Text(if (item.correct) "正确" else "复习", color = color, maxLines = 1, softWrap = false)',
)
replace(path, "modifier = Modifier.fillMaxWidth().height(minHeight)", "modifier = Modifier.fillMaxWidth().heightIn(min = minHeight)")
