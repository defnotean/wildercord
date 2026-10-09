#!/usr/bin/env python3
"""Sword-ID-isolated cutting-edge spans at native-sized HUD presets. OFFLINE ONLY.

Uses unmodified software-rendered pixels and the unchanged native cyan/4-connected/
minimum-eight-pixel/16-GUI-span policy. Skin, sleeves and other cyan pixels cannot count.
Input exports must match the current authored source and original verified textures.
"""
from __future__ import annotations
import argparse
from collections import deque
import hashlib
import io
import json
from pathlib import Path
from zipfile import ZipFile
import numpy as np
from PIL import Image
import preview_articulated_combat as base
from earned_counter_offline import dependencies

ROOT = Path(__file__).resolve().parents[1]


def measure(raster, gui):
    rgb = raster.rgb.astype(float); r, g, b = rgb[:, :, 0], rgb[:, :, 1], rgb[:, :, 2]
    top = raster.h - 42 * gui
    mask = (raster.ids == 2) & (g > 75) & (r < g * .7) & (b > g * .65) & (b < g * 1.3)
    mask[top:] = False
    count = int(mask.sum()); best = 0.; components = []
    for y, x in zip(*np.where(mask)):
        if not mask[y, x]:
            continue
        todo = deque([(x, y)]); mask[y, x] = False; size = 0; lo_x = hi_x = x; lo_y = hi_y = y
        while todo:
            xx, yy = todo.popleft(); size += 1
            lo_x, hi_x = min(lo_x, xx), max(hi_x, xx); lo_y, hi_y = min(lo_y, yy), max(hi_y, yy)
            for nx, ny in [(xx - 1, yy), (xx + 1, yy), (xx, yy - 1), (xx, yy + 1)]:
                if 0 <= nx < raster.w and 0 <= ny < top and mask[ny, nx]:
                    mask[ny, nx] = False; todo.append((nx, ny))
        if size >= 8:
            span = float(max((hi_x - lo_x + 1) / gui, (hi_y - lo_y + 1) / gui))
            best = max(best, span); components.append({'pixels': size, 'spanGui': span})
    return {'swordOnlyCyanPixelsAboveHud': count, 'largestCuttingEdgeSpanGui': best,
            'minimumSpanGui': 16, 'connectedComponentsAtLeast8Pixels': components, 'passes': bool(count >= 8 and best >= 16)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--export', dest='export_dir', type=Path, required=True)
    parser.add_argument('--verification', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args(); out = args.out.resolve(); out.mkdir(parents=True, exist_ok=True)
    if any(out.iterdir()): raise ValueError('Use a fresh evidence directory')
    export = args.export_dir.resolve(); provenance = json.loads((export / 'report.json').read_text())
    current = base.sha(ROOT / 'src/main/java/dev/wildercord/aura/ArticulatedCombatPose.java')
    if provenance['sourceSha256']['src/main/java/dev/wildercord/aura/ArticulatedCombatPose.java'] != current or not provenance['sourceUnchanged']:
        raise ValueError('Stale or unstable pose export')
    jars, hashes = dependencies(args.verification)
    official = next(p for p in jars if p.name == 'minecraft-26.3-client.jar')
    with ZipFile(official) as jar:
        pixels = jar.read('assets/minecraft/textures/item/diamond_sword.png')
        diamond = np.array(Image.open(io.BytesIO(pixels)).convert('RGBA'))
        iron = np.array(Image.open(io.BytesIO(jar.read('assets/minecraft/textures/item/iron_sword.png'))).convert('RGBA'))
    if not np.array_equal(diamond[:, :, 3], iron[:, :, 3]): raise ValueError('Diamond/iron opaque geometry differs')
    preview = base.Preview(export, export / 'asset-cache'); preview.tex['sword'] = diamond
    records, cells = [], []
    for clip, ages in [('unmoved', [3.875, 6, 6.5, 6.875, 10, 20]), ('null_parry', [2.625, 4, 4.5, 4.875, 7.5, 16])]:
        for slim in ['wide', 'slim']:
            for left in [False, True]:
                for age in ages:
                    frame = preview.frame(clip, left, age)
                    for yaw, pitch in [(0, 0), (-25, -20), (25, -20), (0, 20), (0, -20)]:
                        meshes, _ = preview.meshes(slim, frame, left, True, yaw, pitch)
                        for width, height, gui in [(854, 480, 2), (1280, 720, 3), (1280, 960, 4), (1920, 810, 3)]:
                            raster = base.Raster(width, height, True, 70); image = raster.draw(meshes)
                            measured = measure(raster, gui)
                            row = {'clip': clip, 'skin': slim, 'left': left, 'age': age, 'yaw': yaw, 'pitch': pitch,
                                   'viewport': [width, height, gui], **measured}; records.append(row)
                            if width == 854 and slim == 'wide' and age in [6, 4] and yaw == 0:
                                name = f'{clip}_{"left" if left else "right"}_active_pitch{pitch}.png'; image.save(out / name)
                                cells.append(base.caption(image.resize((427, 240)), name[:-4], f'SWORD-ID edge {measured["largestCuttingEdgeSpanGui"]:g} GUI px / minimum16'))
                            if not measured['passes']:
                                print('FAIL', row, flush=True)
    base.sheet(cells, 3, 'Sword-only cutting-edge readability', 'OFFLINE native-sized presets; skin/cuffs never count; unchanged16-GUI threshold.', out / 'active_readability.png')
    report = {'label': 'OFFLINE source-raster evidence, not native acceptance', 'passed': all(r['passes'] for r in records),
              'sourcePoseSha256': current, 'scriptSha256': base.sha(Path(__file__)), 'diamondTextureSha256': hashlib.sha256(pixels).hexdigest(),
              'dependencySha256': hashes, 'sourceExport': str(export), 'results': records,
              'limits': ['Bare actual-source rig and sword only; armor source preview/gates remain separate.',
                         'Native lighting/materials/continuous motion and GPU readability remain pending.']}
    (out / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    print('Samples', len(records), 'minimum span', min(r['largestCuttingEdgeSpanGui'] for r in records), 'passed', report['passed'])
    if not report['passed']: raise SystemExit(1)


if __name__ == '__main__': main()
