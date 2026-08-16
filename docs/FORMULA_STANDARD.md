# School 数学公式规范

课程包中的公式统一使用标准 LaTeX 数学语法，App 负责把公式渲染到界面。

## 数据约定

`CourseFormula.expression` 只保存数学表达式本身，不保存 `$...$`、`$$...$$`、`\(...\)` 或 `\[...\]` 分隔符，也不保存渲染器名称。

示例：

```json
{
  "type": "formula",
  "expression": "\\frac{-b\\pm\\sqrt{b^2-4ac}}{2a}",
  "note": "公式说明放在普通正文中。"
}
```

课程数据不得使用 Unicode 上下标、Unicode 数学运算符或中文句子代替 LaTeX。中文解释放在 `note`、`explanation`、`keyIdea` 等正文区域。

## 显示职责

- 课程包只负责标准 LaTeX。
- APK 通过 `SchoolFormula` 统一渲染，当前实现使用 JLaTeXMath Android。
- 长公式保持正常数学布局；超出屏幕宽度时允许横向滚动，不把公式压成竖排。
- 渲染失败时回退显示原始表达式，避免课程页面崩溃。
- 用户文字缩放通过 Compose `sp` / `LocalDensity` 同步作用到公式字号。

## 教材入口

教材 PDF 是课程参考资料，不作为正文步骤。每个 Lesson 继续保留 `references` 页码元数据，App 在课程顶部右侧提供唯一的 `PDF` 入口，并跳转到当前 Lesson 的首个教材参考页。

## 第三方实现说明

当前 APK 使用 `rikkahub/jlatexmath-android` 1.5（JLaTeXMath Android fork）作为公式绘制实现。课程 JSON 不依赖该实现，未来替换渲染器不需要修改课程格式。
