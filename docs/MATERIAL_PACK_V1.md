# School Material Pack v1

教材资源包是一个普通 ZIP 文件。它与 APK 独立存放，允许按教材或科目单独导入、替换和删除。

## 目录结构

```text
math-grade7-volume1.school.zip
├── manifest.json
├── catalog.json
└── books/
    └── textbook.pdf
```

`manifest.json` 必须位于 ZIP 根目录，`catalog.json` 与 PDF 的路径由清单声明。

## manifest.json

```json
{
  "schemaVersion": 1,
  "packId": "math-grade7-volume1",
  "version": "1.0.0",
  "title": "七年级数学上册",
  "subject": "数学",
  "catalog": "catalog.json",
  "pdf": {
    "path": "books/textbook.pdf",
    "sha256": "PDF 文件的 SHA-256",
    "pageIndexOffset": 0
  }
}
```

字段说明：

- `schemaVersion`：当前固定为 `1`。
- `packId`：资源包稳定标识，只能使用小写字母、数字、点、下划线和短横线。
- `version`：教材包自身版本，不等于 App 版本。
- `catalog`：当前教材目录 JSON 路径。
- `pdf.path`：教材 PDF 在 ZIP 内的相对路径。
- `pdf.sha256`：导入时必须通过校验，防止文件损坏或混用版本。
- `pdf.pageIndexOffset`：印刷页码到 PDF 页索引的偏移量。

页码换算公式：

```text
PDF 索引 = 印刷页码 - 1 + pageIndexOffset
```

例如教材印刷第 10 页实际位于 PDF 第 13 个页面（索引 12），则偏移量为 `3`。

## catalog.json

当前目录契约只接受 `book + lessons`，不再兼容旧的章节嵌套目录格式。

```json
{
  "schemaVersion": 1,
  "book": {
    "id": "math-grade7-volume1",
    "title": "七年级数学上册",
    "subject": "数学",
    "grade": 7,
    "volume": 1
  },
  "lessons": [
    {
      "id": "number-line",
      "title": "数轴",
      "pageStart": 15,
      "pageEnd": 20,
      "orderIndex": 0
    }
  ]
}
```

每节课必须提供稳定 `id`、标题和有效印刷页码范围。可按当前课程树需要增加 `role`、`path` 与 `orderIndex`，但 App 不会把旧格式自动迁移成新格式。

## 安全限制

导入器会执行以下检查：

- 拒绝绝对路径和 `..` 路径穿越。
- 最多允许 10,000 个 ZIP 条目。
- 单文件解压后最大约 1.6 GB。
- 整包解压后最大约 2.2 GB。
- 必须包含清单、目录和 PDF。
- PDF 必须通过清单声明的 SHA-256。
- 新包完全校验成功后才替换已安装版本。

## 构建资源包

仓库提供 [`scripts/build_material_pack.py`](../scripts/build_material_pack.py)：

```bash
python scripts/build_material_pack.py \
  --pdf "/path/to/数学七年级上册.pdf" \
  --catalog app/src/main/assets/catalog/math-grade7-volume1.json \
  --output math-grade7-volume1.school.zip \
  --pack-id math-grade7-volume1 \
  --version 1.0.0 \
  --title "七年级数学上册" \
  --subject "数学" \
  --page-index-offset 0
```

PDF 通常已经压缩，因此脚本使用 ZIP 存储模式，避免浪费时间再次压缩。
