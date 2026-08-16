from pathlib import Path

path = Path('app/src/main/java/com/majortomman/school/ui/TextbookMathVisualizations.kt')
text = path.read_text(encoding='utf-8')
old = 'CourseSceneTemplate.DIAGRAM -> drawDiagram(data)'
new = 'CourseSceneTemplate.DECLARATIVE_DIAGRAM -> drawDiagram(data)'
if old not in text:
    raise SystemExit('expected DIAGRAM dispatch not found')
path.write_text(text.replace(old, new, 1), encoding='utf-8')
