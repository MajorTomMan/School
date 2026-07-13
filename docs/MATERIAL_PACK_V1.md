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
- `catalog`：课程目录 JSON；v1 导入时会确认文件存在，后续版本会读取知识点、场景与练习。
- `pdf.path`：教材 PDF 在 ZIP 内的相对路径。
- `pdf.sha256`：导入时必须通过校验，防止文件损坏或混用版本。
- `pdf.pageIndexOffset`：印刷页码到 PDF 页索引的偏移量。

页码换算公式：

```text
PDF 索引 = 印刷页码 - 1 + pageIndexOffset
```

例如教材印刷第 10 页实际位于 PDF 第 13 个页面（索引 12），则偏移量为 `3`。

## catalog.json 最小格式

v1 暂时只验证该文件存在，建议保留以下结构，方便后续处理器升级：

```json
{
  "schemaVersion": 1,
  "bookId": "math-grade7-volume1",
  "chapters": [
    {
      "id": "rational-numbers",
      "title": "第一章 有理数",
      "lessons": [
        {
          "id": "number-line",
          "title": "数轴",
          "pages": [15, 20]
        }
      ]
    }
  ]
}
```

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
