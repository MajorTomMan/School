#!/usr/bin/env python3
"""Human-review workflow for textbook PDF images, tables and diagrams.

`scan` only creates candidates and review crops. `materialize` only exports crops whose decisions
were explicitly marked approved. Neither command writes course prose or trusts OCR as source text.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
from typing import Any

import fitz


def digest(path: Path) -> str:
    value = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(chunk)
    return value.hexdigest()


def parse_pages(raw: str) -> list[int]:
    pages: set[int] = set()
    for token in raw.split(","):
        token = token.strip()
        if not token:
            continue
        if "-" in token:
            start, end = map(int, token.split("-", 1))
            if start <= 0 or end < start:
                raise argparse.ArgumentTypeError(f"invalid page range: {token}")
            pages.update(range(start, end + 1))
        else:
            page = int(token)
            if page <= 0:
                raise argparse.ArgumentTypeError("printed pages must be positive")
            pages.add(page)
    if not pages:
        raise argparse.ArgumentTypeError("at least one printed page is required")
    return sorted(pages)


def rounded(rect: fitz.Rect) -> list[float]:
    return [round(float(item), 2) for item in (rect.x0, rect.y0, rect.x1, rect.y1)]


def expand(rect: fitz.Rect, page: fitz.Rect, padding: float) -> fitz.Rect:
    return fitz.Rect(
        max(page.x0, rect.x0 - padding), max(page.y0, rect.y0 - padding),
        min(page.x1, rect.x1 + padding), min(page.y1, rect.y1 + padding),
    )


def area_ratio(rect: fitz.Rect, page: fitz.Page) -> float:
    return rect.get_area() / page.rect.get_area() if page.rect.get_area() else 0.0


def nearby_text(page: fitz.Page, rect: fitz.Rect) -> str:
    clip = expand(rect, page.rect, 28)
    text = " ".join(str(block[4]) for block in page.get_text("blocks", clip=clip) if len(block) >= 5)
    return re.sub(r"\s+", " ", text).strip()[:240]


def stable_id(printed_page: int, kind: str, rect: fitz.Rect) -> str:
    key = f"{printed_page}|{kind}|" + ",".join(f"{item:.2f}" for item in rounded(rect))
    return f"p{printed_page:03d}-{kind}-{hashlib.sha1(key.encode()).hexdigest()[:10]}"


def suggest(kind: str, text: str, count: int) -> tuple[str, list[str]]:
    if kind == "table":
        return "structured_table", ["PyMuPDF detected a table region", "manual cell verification required"]
    if kind == "image":
        return "source_crop", ["PDF contains an embedded raster image"]
    if any(term in text for term in ("数轴", "原点", "单位长度", "正方向", "坐标", "刻度")):
        return "native_scene", ["nearby text has axis or coordinate semantics", "prefer APK native scene"]
    return "source_crop", [f"clustered {count} vector drawing paths", "human must decide whether reconstruction is safe"]


def page_candidates(page: fitz.Page, printed_page: int, index: int, minimum: float, padding: float) -> list[dict[str, Any]]:
    raw: list[dict[str, Any]] = []
    for info in page.get_image_info(xrefs=True):
        rect = fitz.Rect(info["bbox"])
        if area_ratio(rect, page) < minimum or rect.width < 30 or rect.height < 24:
            continue
        action, reasons = suggest("image", nearby_text(page, rect), 1)
        raw.append(candidate(printed_page, index, "image", rect, page, padding, action, reasons))

    drawings = page.get_drawings()
    drawing_rects = [fitz.Rect(item["rect"]) for item in drawings if item.get("rect")]
    cluster = getattr(page, "cluster_drawings", None)
    clusters = cluster(x_tolerance=3, y_tolerance=3) if cluster else drawing_rects
    for value in clusters:
        rect = fitz.Rect(value)
        ratio = area_ratio(rect, page)
        if ratio < minimum or ratio > 0.42 or rect.width < 40 or rect.height < 20:
            continue
        if rect.y1 < page.rect.y0 + 34 or rect.y0 > page.rect.y1 - 34:
            continue
        count = sum(1 for item in drawing_rects if not (rect & item).is_empty)
        text = nearby_text(page, rect)
        action, reasons = suggest("vector", text, count)
        raw.append(candidate(printed_page, index, "vector", rect, page, padding, action, reasons))

    finder = getattr(page, "find_tables", None)
    if finder:
        try:
            tables = finder().tables
        except Exception:
            tables = []
        for table in tables:
            rect = fitz.Rect(table.bbox)
            if area_ratio(rect, page) < minimum:
                continue
            action, reasons = suggest("table", nearby_text(page, rect), 0)
            reasons[0] += f" ({getattr(table, 'row_count', '?')}×{getattr(table, 'col_count', '?')})"
            raw.append(candidate(printed_page, index, "table", rect, page, padding, action, reasons))

    kept: list[dict[str, Any]] = []
    priority = {"table": 3, "image": 2, "vector": 1}
    for item in sorted(raw, key=lambda value: (value["score"], priority[value["kind"]]), reverse=True):
        rect = fitz.Rect(item["bbox"])
        duplicate = False
        for existing in kept:
            other = fitz.Rect(existing["bbox"])
            intersection = rect & other
            smaller = min(rect.get_area(), other.get_area())
            if smaller and not intersection.is_empty and intersection.get_area() / smaller >= 0.78:
                duplicate = True
                break
        if not duplicate:
            kept.append(item)
    return sorted(kept, key=lambda value: (value["bbox"][1], value["bbox"][0], value["kind"]))


def candidate(page_number: int, index: int, kind: str, rect: fitz.Rect, page: fitz.Page,
              padding: float, action: str, reasons: list[str]) -> dict[str, Any]:
    clip = expand(rect, page.rect, padding)
    return {
        "id": stable_id(page_number, kind, rect), "printedPage": page_number, "pdfPageIndex": index,
        "kind": kind, "bbox": rounded(rect), "clip": rounded(clip),
        "score": round(min(1.0, 0.45 + area_ratio(rect, page) * 2.5), 3),
        "suggestedAction": action, "reasons": reasons, "nearbyText": nearby_text(page, rect),
    }


def render(page: fitz.Page, clip: list[float], target: Path, scale: float) -> tuple[int, int]:
    target.parent.mkdir(parents=True, exist_ok=True)
    pixmap = page.get_pixmap(matrix=fitz.Matrix(scale, scale), clip=fitz.Rect(clip), alpha=False)
    pixmap.save(target)
    return pixmap.width, pixmap.height


def scan(pdf: Path, pages: list[int], offset: int, output: Path, minimum: float, padding: float, scale: float) -> dict[str, Any]:
    output.mkdir(parents=True, exist_ok=True)
    document = fitz.open(pdf)
    candidates: list[dict[str, Any]] = []
    for printed in pages:
        index = printed + offset - 1
        if index not in range(document.page_count):
            raise ValueError(f"printed page {printed} resolves outside the PDF")
        candidates.extend(page_candidates(document[index], printed, index, minimum, padding))
    for item in candidates:
        relative = Path("crops") / f"{item['id']}.png"
        width, height = render(document[item["pdfPageIndex"]], item["clip"], output / relative, scale)
        item.update(reviewCrop=relative.as_posix(), reviewWidth=width, reviewHeight=height)
    source = {"filename": pdf.name, "sha256": digest(pdf), "pageCount": document.page_count,
              "pageIndexOffset": offset, "printedPages": pages}
    review = {"source": source, "settings": {"minAreaRatio": minimum, "paddingPoints": padding,
              "renderScale": scale}, "candidates": candidates}
    decisions = {"source": source, "decisions": [{"candidateId": item["id"], "status": "pending",
        "action": item["suggestedAction"], "assetId": item["id"][:63],
        "outputPath": f"assets/figures/{item['id']}.png", "notes": ""} for item in candidates]}
    (output / "review.json").write_text(json.dumps(review, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (output / "review-decisions.json").write_text(json.dumps(decisions, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    write_report(review, output / "review.md")
    return review


def write_report(review: dict[str, Any], target: Path) -> None:
    lines = ["# PDF 图表候选人工复核报告", "", f"- PDF：`{review['source']['filename']}`",
             f"- SHA-256：`{review['source']['sha256']}`", f"- 候选数：{len(review['candidates'])}", "",
             "> 自动结果只提供候选。正文、题意、图表含义和处理方式必须人工核对。", ""]
    for item in review["candidates"]:
        lines += [f"## {item['id']}", "", f"- 教材页：{item['printedPage']}", f"- 类型：`{item['kind']}`",
                  f"- 建议：`{item['suggestedAction']}`", f"- PDF 坐标：`{item['clip']}`",
                  f"- 原因：{'；'.join(item['reasons'])}", f"- 附近文字：{item['nearbyText'] or '（无）'}", "",
                  f"![{item['id']}]({item['reviewCrop']})", ""]
    target.write_text("\n".join(lines), encoding="utf-8")


def safe_path(raw: str) -> PurePosixPath:
    path = PurePosixPath(raw)
    if path.is_absolute() or not str(path).startswith("assets/") or path.suffix.lower() != ".png":
        raise ValueError(f"invalid PNG asset path: {raw}")
    if any(part in {"", ".", ".."} for part in path.parts):
        raise ValueError(f"unsafe asset path: {raw}")
    return path


def materialize(pdf: Path, review_path: Path, decisions_path: Path, output: Path,
                scale: float = 3.0, allow_pending: bool = False) -> dict[str, Any]:
    review = json.loads(review_path.read_text(encoding="utf-8"))
    decisions = json.loads(decisions_path.read_text(encoding="utf-8"))
    if decisions.get("source") != review.get("source") or digest(pdf) != review["source"]["sha256"]:
        raise ValueError("review, decisions and PDF source do not match")
    candidates = {item["id"]: item for item in review["candidates"]}
    seen: set[str] = set()
    assets: list[dict[str, Any]] = []
    document = fitz.open(pdf)
    for decision in decisions.get("decisions", []):
        candidate_id = str(decision.get("candidateId") or "")
        if candidate_id not in candidates or candidate_id in seen:
            raise ValueError(f"unknown or duplicate candidateId: {candidate_id}")
        seen.add(candidate_id)
        status, action = decision.get("status"), decision.get("action")
        if status == "pending" and not allow_pending:
            raise ValueError(f"candidate still pending: {candidate_id}")
        if status != "approved" or action != "source_crop":
            continue
        asset_id = str(decision.get("assetId") or "")
        if not re.fullmatch(r"[a-z][a-z0-9_-]{0,62}", asset_id):
            raise ValueError(f"invalid assetId: {asset_id}")
        relative = safe_path(str(decision.get("outputPath") or ""))
        target = output.joinpath(*relative.parts)
        item = candidates[candidate_id]
        width, height = render(document[item["pdfPageIndex"]], item["clip"], target, scale)
        assets.append({"id": asset_id, "path": relative.as_posix(), "mediaType": "image/png",
                       "width": width, "height": height, "sha256": digest(target),
                       "source": {"printedPage": item["printedPage"], "clip": item["clip"],
                                  "candidateId": candidate_id}})
    missing = set(candidates) - seen
    if missing and not allow_pending:
        raise ValueError(f"missing decisions: {', '.join(sorted(missing))}")
    manifest = {"sourceSha256": review["source"]["sha256"], "renderScale": scale, "assets": assets}
    output.mkdir(parents=True, exist_ok=True)
    (output / "materialized-assets.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return manifest


def arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    commands = parser.add_subparsers(dest="command", required=True)
    scan_parser = commands.add_parser("scan")
    scan_parser.add_argument("--pdf", type=Path, required=True)
    scan_parser.add_argument("--pages", type=parse_pages, required=True)
    scan_parser.add_argument("--page-index-offset", type=int, required=True)
    scan_parser.add_argument("--output", type=Path, required=True)
    scan_parser.add_argument("--min-area-ratio", type=float, default=0.004)
    scan_parser.add_argument("--padding", type=float, default=8.0)
    scan_parser.add_argument("--render-scale", type=float, default=2.5)
    export = commands.add_parser("materialize")
    export.add_argument("--pdf", type=Path, required=True)
    export.add_argument("--review", type=Path, required=True)
    export.add_argument("--decisions", type=Path, required=True)
    export.add_argument("--output", type=Path, required=True)
    export.add_argument("--render-scale", type=float, default=3.0)
    export.add_argument("--allow-pending", action="store_true")
    return parser.parse_args()


def main() -> int:
    args = arguments()
    if not args.pdf.is_file():
        raise SystemExit(f"PDF not found: {args.pdf}")
    if args.command == "scan":
        result = scan(args.pdf.resolve(), args.pages, args.page_index_offset, args.output.resolve(),
                      args.min_area_ratio, args.padding, args.render_scale)
        print(f"candidates: {len(result['candidates'])}")
    else:
        result = materialize(args.pdf.resolve(), args.review.resolve(), args.decisions.resolve(),
                             args.output.resolve(), args.render_scale, args.allow_pending)
        print(f"materialized assets: {len(result['assets'])}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
