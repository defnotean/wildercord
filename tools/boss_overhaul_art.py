"""Overhaul Bedrock geo rigs, animations, and multi-layer emissive textures for Archivist, Thalassor, and Cinder Warden."""
from __future__ import annotations

import json
from pathlib import Path

from PIL import Image


def _round_numbers(obj):
    if isinstance(obj, float):
        return round(obj, 4)
    if isinstance(obj, list):
        return [_round_numbers(x) for x in obj]
    if isinstance(obj, dict):
        return {k: _round_numbers(v) for k, v in obj.items()}
    return obj


def _write_compact_json(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, separators=(",", ":"), ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")


_RIGS = (
    ("archivist", "archivist", ("diffuse", "eyes", "runes")),
    ("thalassor", "thalassor", ("diffuse", "glow")),
    ("cinder_warden", "cinder_warden", ("diffuse", "glow", "eyes")),
)


def write(g):
    for eid, stem, layers in _RIGS:
        src_dir = g.ROOT / "art" / "blockbench" / eid
        for layer in layers:
            dst_suffix = "" if layer == "diffuse" else f"_{layer}"
            with Image.open(src_dir / f"{eid}_{layer}.png") as img:
                g.save(img.convert("RGBA"), g.ASSETS / f"textures/entity/overhaul/{eid}{dst_suffix}.png")

        geo_data = _round_numbers(json.loads((src_dir / f"{stem}.geo.json").read_text(encoding="utf-8")))
        anim_data = _round_numbers(json.loads((src_dir / f"{stem}.animation.json").read_text(encoding="utf-8")))
        _write_compact_json(g.ASSETS / f"models/entity/overhaul/{eid}.geo.json", geo_data)
        _write_compact_json(g.ASSETS / f"animations/entity/overhaul/{eid}.animation.json", anim_data)
