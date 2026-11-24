#!/usr/bin/env python3
"""Unit tests for Wildercord Bedrock pack & Geyser mappings exporter."""

import json
import pathlib
import tempfile
import unittest
import uuid
import zipfile

import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import bedrock_export as be  # noqa: E402


class BedrockExportTest(unittest.TestCase):
    def test_deterministic_uuids(self):
        u1 = be.det_uuid("wildercord.bedrock.rp.header")
        u2 = be.det_uuid("wildercord.bedrock.rp.header")
        self.assertEqual(u1, u2)
        self.assertEqual(str(uuid.UUID(u1)), u1)
        self.assertNotEqual(u1, be.det_uuid("wildercord.bedrock.bp.header"))

    def test_export_builds_valid_console_ready_pack(self):
        root = be.repo_root()
        with tempfile.TemporaryDirectory() as tmp:
            summary = be.build_pack(root, pathlib.Path(tmp), include_sounds=False)
            errors = be.validate_outputs(summary["rp_mcpack"], summary["mcaddon"], summary["mappings"])
            self.assertEqual(errors, [], f"Validation errors: {errors}")

            self.assertGreaterEqual(summary["items"], 300)
            self.assertGreaterEqual(summary["runes"], 700)
            self.assertGreaterEqual(summary["blocks"], 60)
            self.assertGreaterEqual(summary["entities"], 100)
            self.assertLess(summary["rp_mb"], 80.0)

            mappings = json.loads(summary["mappings"].read_text(encoding="utf-8"))
            self.assertIn("wildercord:twine_cord", mappings["items"])
            self.assertIn("wildercord:master_blade/ember", mappings["items"])
            self.assertEqual("minecraft:netherite_sword", mappings["items"]["wildercord:master_blade/ember"]["javaCarrier"])
            self.assertEqual("wildercord:master_blade/ember", mappings["items"]["wildercord:master_blade/ember"]["itemModel"])
            self.assertEqual("minecraft:written_book", mappings["items"]["wildercord:three_bows"]["javaCarrier"])
            self.assertEqual("minecraft:leather", mappings["items"]["wildercord:backpack"]["javaCarrier"])
            self.assertIn("wildercord:self", mappings["runes"])
            self.assertIn("wildercord:wellstone", mappings["blocks"])
            self.assertIn("wildercord:archivist", mappings["entities"])
            self.assertIn("wildercord:sigil", mappings["particles"])

            # Verify no duplicate customModelData ints
            cmds = [v["customModelData"] for v in mappings["items"].values()] + [
                v["customModelData"] for v in mappings["runes"].values()
            ]
            self.assertEqual(len(cmds), len(set(cmds)))

            with zipfile.ZipFile(summary["rp_mcpack"]) as zf:
                man = json.loads(zf.read("manifest.json"))
                self.assertEqual(man["format_version"], 2)
                self.assertEqual(man["header"]["min_engine_version"], [1, 21, 0])
                self.assertIn("texts/en_US.lang", zf.namelist())
                # Ensure animated echo_cord texture was normalized to square 16x16
                from PIL import Image
                import io
                with Image.open(io.BytesIO(zf.read("textures/items/wildercord/echo_cord.png"))) as img:
                    self.assertEqual(img.size[0], img.size[1])


if __name__ == "__main__":
    unittest.main()
