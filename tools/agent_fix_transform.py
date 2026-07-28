from pathlib import Path

path = Path("tools/agent_apply_section_assessment.py")
text = path.read_text(encoding="utf-8")

signature_old = '''replace_once(
    "app/src/main/java/com/majortomman/school/ui/CloudCourseLessonScreen.kt",
    ''' + '"""' + '''    nextLessonTitle: String?,
    onOpenTextbook: (Int) -> Unit,''' + '"""' + ''',
    ''' + '"""' + '''    nextLessonTitle: String?,
    pagesOverride: List<CoursePage>? = null,
    onOpenTextbook: (Int) -> Unit,''' + '"""' + ''',
)'''
signature_new = '''replace_once(
    "app/src/main/java/com/majortomman/school/ui/CloudCourseLessonScreen.kt",
    ''' + '"""' + '''fun CloudCourseLessonScreen(
    lesson: Lesson,
    installedMaterial: InstalledMaterialPack,
    nextLessonTitle: String?,
    onOpenTextbook: (Int) -> Unit,''' + '"""' + ''',
    ''' + '"""' + '''fun CloudCourseLessonScreen(
    lesson: Lesson,
    installedMaterial: InstalledMaterialPack,
    nextLessonTitle: String?,
    pagesOverride: List<CoursePage>? = null,
    onOpenTextbook: (Int) -> Unit,''' + '"""' + ''',
)'''
if text.count(signature_old) != 1:
    raise SystemExit(f"expected one signature transform, found {text.count(signature_old)}")
text = text.replace(signature_old, signature_new)

font_old = '''replace_once(
    "app/src/main/java/com/majortomman/school/ui/CloudCourseLessonScreen.kt",
    '                    fontSize = 10.sp,',
    '                    fontSize = 12.sp,',
)'''
font_new = '''replace_once(
    "app/src/main/java/com/majortomman/school/ui/CloudCourseLessonScreen.kt",
    ''' + '"""' + '''                Text(
                    "学习环节 ${pagerState.currentPage + 1} / ${pages.size}",
                    color = InteractiveMuted,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                )''' + '"""' + ''',
    ''' + '"""' + '''                Text(
                    "学习环节 ${pagerState.currentPage + 1} / ${pages.size}",
                    color = InteractiveMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )''' + '"""' + ''',
)'''
if text.count(font_old) != 1:
    raise SystemExit(f"expected one font transform, found {text.count(font_old)}")
text = text.replace(font_old, font_new)
path.write_text(text, encoding="utf-8")
