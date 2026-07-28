from pathlib import Path

path = Path("tools/agent_apply_section_assessment.py")
text = path.read_text(encoding="utf-8")
old = '''replace_once(
    "app/src/main/java/com/majortomman/school/ui/CloudCourseLessonScreen.kt",
    '                    fontSize = 10.sp,',
    '                    fontSize = 12.sp,',
)'''
new = '''replace_once(
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
if text.count(old) != 1:
    raise SystemExit(f"expected one transform snippet, found {text.count(old)}")
path.write_text(text.replace(old, new), encoding="utf-8")
