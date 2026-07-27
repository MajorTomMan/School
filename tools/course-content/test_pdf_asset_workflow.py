from __future__ import annotations

import json
from pathlib import Path
import tempfile
import unittest

import fitz

from pdf_asset_workflow import materialize, parse_pages, scan


class PdfAssetWorkflowTest(unittest.TestCase):
    def make_pdf(self, path: Path) -> None:
        document = fitz.open()
        page = document.new_page(width=500, height=700)
        shape = page.new_shape()
        shape.draw_line((70, 180), (430, 180))
        for x in range(90, 431, 40):
            shape.draw_line((x, 170), (x, 190))
        shape.finish(color=(0, 0, 0), width=1.5)
        shape.commit()
        page.insert_text((75, 150), "number line origin unit length")

        grid = page.new_shape()
        for x in (80, 180, 280, 380):
            grid.draw_line((x, 320), (x, 440))
        for y in (320, 360, 400, 440):
            grid.draw_line((80, y), (380, y))
        grid.finish(color=(0, 0, 0), width=1)
        grid.commit()
        page.insert_text((85, 305), "table statistics total")
        document.save(path)

    def test_page_parser(self) -> None:
        self.assertEqual([2, 3, 4, 8], parse_pages("2-4,8"))

    def test_scan_and_approved_materialization(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            pdf = root / "book.pdf"
            self.make_pdf(pdf)
            first = scan(pdf, [1], 0, root / "a", .002, 8, 2)
            second = scan(pdf, [1], 0, root / "b", .002, 8, 2)
            self.assertEqual(
                [item["id"] for item in first["candidates"]],
                [item["id"] for item in second["candidates"]],
            )
            self.assertTrue(first["candidates"])
            decisions_path = root / "a" / "review-decisions.json"
            decisions = json.loads(decisions_path.read_text(encoding="utf-8"))
            for item in decisions["decisions"]:
                item.update(status="rejected", action="ignore")
            selected = decisions["decisions"][0]
            selected.update(
                status="approved",
                action="source_crop",
                assetId="approved-crop",
                outputPath="assets/figures/approved-crop.png",
            )
            decisions_path.write_text(json.dumps(decisions), encoding="utf-8")
            result = materialize(
                pdf,
                root / "a" / "review.json",
                decisions_path,
                root / "runtime",
                2,
            )
            self.assertEqual(1, len(result["assets"]))
            self.assertTrue((root / "runtime" / result["assets"][0]["path"]).is_file())

    def test_pending_blocks_export(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            pdf = root / "book.pdf"
            self.make_pdf(pdf)
            scan(pdf, [1], 0, root / "review", .002, 8, 2)
            with self.assertRaisesRegex(ValueError, "pending"):
                materialize(
                    pdf,
                    root / "review" / "review.json",
                    root / "review" / "review-decisions.json",
                    root / "out",
                )


if __name__ == "__main__":
    unittest.main()
