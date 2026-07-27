# PDF 图表与示意图人工复核流程

本流程用于处理教材中出现的图片、表格、数轴、坐标图和其他示意图。工具只生成候选，不编写课程正文，不把 OCR 或自动识别结果当作教材原文。

## 处理原则

1. 教材 PDF 是唯一图像来源，必须先校验文件 SHA-256。
2. 自动扫描只负责发现候选区域、生成复核裁剪和给出处理建议。
3. 每个候选必须由人工标记为 `approved`、`rejected` 或保留 `pending`。
4. `pending` 候选默认阻止正式素材导出。
5. 简单数轴、坐标系和几何图优先使用 APK 原生场景重建。
6. 简单表格优先人工核对后写成结构化 `columns` / `rows`。
7. 复杂图、实物照片或必须保留教材版式的内容才使用 PDF 高清裁剪。
8. 课程正文、题干、答案和解析仍需逐句人工录入并与教材核对。

## 生成候选

以七年级上册教材第 2～11 页为例：

```bash
python3 tools/course-content/pdf_asset_workflow.py scan \
  --pdf build/course-source/义务教育教科书·数学七年级上册.pdf \
  --pages 2-11 \
  --page-index-offset 7 \
  --output build/pdf-review/pep-math-7-1
```

输出目录包括：

- `review.json`：机器可读候选、PDF 坐标、附近文字和处理建议；
- `review.md`：包含候选裁剪的人工复核报告；
- `review-decisions.json`：全部状态为 `pending` 的决策模板；
- `crops/*.png`：仅用于复核的预览裁剪。

`review.json` 中的建议值只有：

- `native_scene`：建议使用 APK 原生场景；
- `structured_table`：建议人工核对为结构化表格；
- `source_crop`：建议保留教材 PDF 裁剪。

建议不是结论，人工可以修改。

## 审批决策

编辑 `review-decisions.json`：

```json
{
  "candidateId": "p011-vector-xxxxxxxxxx",
  "status": "approved",
  "action": "source_crop",
  "assetId": "number-line-practice-source",
  "outputPath": "assets/figures/number-line-practice-source.png",
  "notes": "已人工核对教材第11页，只保留题目数轴区域"
}
```

不使用的候选应设置为：

```json
{
  "status": "rejected",
  "action": "ignore"
}
```

原生场景或结构化表格也可以标记 `approved`，但不会生成图片文件；其业务内容仍需人工写入课程文件。

## 导出已批准裁剪

```bash
python3 tools/course-content/pdf_asset_workflow.py materialize \
  --pdf build/course-source/义务教育教科书·数学七年级上册.pdf \
  --review build/pdf-review/pep-math-7-1/review.json \
  --decisions tools/course-content/manual/pep-math-7-1/asset-decisions.json \
  --output build/generated-course/pep-math-7-1 \
  --render-scale 3
```

导出前会检查：

- PDF SHA-256 是否与复核源一致；
- 决策文件是否属于同一份复核报告；
- 是否仍存在 `pending` 候选；
- 候选 ID 是否重复或缺失；
- `assetId` 与 `assets/` 路径是否合法；
- 只导出 `approved + source_crop` 的 PNG。

结果写入课程目录和 `materialized-assets.json`。正式 `assessments.json` 仍必须声明图片 ID、路径、真实宽高和媒体类型，并由 APK 在启用前再次校验。

## 禁止事项

- 不允许将自动提取文本直接写成教材正文；
- 不允许未审批候选进入课程包；
- 不允许裁剪区域越过 PDF 页面；
- 不允许课程包执行脚本或携带任意渲染代码；
- 不允许为了适配屏幕而篡改图表含义、刻度、单位或数据。
