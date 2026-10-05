#!/usr/bin/env python3
"""HUD composition preflight using exported production geometry, never native acceptance.

Uses only already-approved local dependencies via preview_articulated_combat.py. The HUD is
an explicitly conservative reserved band, not a reconstruction or replacement of Minecraft's
HUD. Actual HUD-on screenshots from ArticulatedFirstPersonCompositionTest remain mandatory.
"""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw
import preview_articulated_combat as preview

ROOT = Path(__file__).resolve().parents[1]
VIEWPORTS = [(854, 480, 2), (1280, 720, 3), (1280, 960, 4), (1920, 810, 3)]
AGES = [0, 2.625, 4, 7, 10, 14]
SOURCES = ['src/main/java/dev/wildercord/aura/ArticulatedCombatPose.java',
           'src/client/java/dev/wildercord/client/combat/ArticulatedViewModel.java',
           'src/client/java/dev/wildercord/client/combat/ArticulatedRig.java',
           'tools/preview_articulated_combat.py', 'tools/check_articulated_composition.py']


def hashes():
    return {path: hashlib.sha256((ROOT / path).read_bytes()).hexdigest() for path in SOURCES}


def render(p, variant, left, age, width, height, gui):
    f = p.frame('spellcut', left, age)
    raster = preview.Raster(width, height, True, 70)
    meshes, grip = p.meshes(variant, f, left, True)
    dominant = 'LEFT_' if left else 'RIGHT_'
    identities = {'SWORD': 1, dominant + 'HAND': 2, dominant + 'FOREARM': 3,
                  ('RIGHT_' if left else 'LEFT_') + 'HAND': 4,
                  ('RIGHT_' if left else 'LEFT_') + 'FOREARM': 5}
    for points, uv, texture, joint, overlay in meshes:
        q = raster.project(points)
        normal = np.cross(points[1] - points[0], points[2] - points[0])
        normal /= max(np.linalg.norm(normal), 1e-10)
        shade = .7 + .3 * abs(float(normal @ np.array([-.3, -.7, -.65]) / np.linalg.norm([-.3, -.7, -.65])))
        for triangle in [[0, 1, 2], [0, 2, 3]]:
            raster.triangle(q[triangle], uv[triangle], texture, shade, identities.get(joint, 6))
    hud_top = height - 42 * gui
    counts = {name: int((raster.ids[:hud_top] == identity).sum()) for name, identity in
              [('sword', 1), ('swordHand', 2), ('swordForearm', 3), ('guardHand', 4), ('guardForearm', 5)]}
    image = Image.fromarray(raster.rgb)
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, hud_top, width - 1, height - 1), fill=(36, 41, 50))
    draw.text((10, hud_top + 6), 'CONSERVATIVE HUD RESERVE (not the game HUD)', fill=(210, 215, 225))
    draw.line((width // 2 - 6, height // 2, width // 2 + 6, height // 2), fill=(230, 235, 240))
    draw.line((width // 2, height // 2 - 6, width // 2, height // 2 + 6), fill=(230, 235, 240))
    projected = raster.project(np.array([grip]))[0]
    result = {'variant': variant, 'left': left, 'age': age, 'viewport': [width, height], 'gui': gui,
              'handFov': 70, 'hudReserveTop': hud_top, 'gripScreen': projected[:2].tolist(),
              'visiblePixelsAboveHudReserve': counts,
              'crosshairCovered': bool(raster.ids[height // 2, width // 2]),
              'gripAboveHudReserve': bool(projected[1] < hud_top)}
    return result, image


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--baseline', type=Path, required=True, help='Unmodified base geometry.json and poses.json export')
    parser.add_argument('--verification', type=Path, default=preview.DEFAULT_VERIFY)
    args = parser.parse_args()
    out = args.out.resolve()
    out.mkdir(parents=True, exist_ok=True)
    if any(out.iterdir()):
        raise ValueError('Use an empty output directory so earlier evidence cannot be overwritten')
    before = hashes()
    preview.export(out, args.verification)
    current = preview.Preview(out, args.verification)
    baseline = preview.Preview(args.baseline.resolve(), args.verification)
    # Stronger than a prose claim: all world/body palettes, weights, phases and samples must match.
    body_equal = all({k: v for k, v in a.items() if k != 'firstPerson'} ==
                     {k: v for k, v in b.items() if k != 'firstPerson'}
                     for ca, cb in zip(current.poses['clips'], baseline.poses['clips'], strict=True)
                     for a, b in zip(ca['frames'], cb['frames'], strict=True))
    records, comparisons, cells = [], [], []
    for variant in ['wide', 'slim']:
        for left in [False, True]:
            for width, height, gui in VIEWPORTS:
                for age in AGES:
                    record, image = render(current, variant, left, age, width, height, gui)
                    records.append(record)
                    if (width, height) == (854, 480):
                        old, old_image = render(baseline, variant, left, age, width, height, gui)
                        comparisons.append({'before': old, 'after': record})
                        if variant == 'wide' and not left and age in [0, 2.625, 4, 7]:
                            cells.extend([preview.caption(old_image, f'BEFORE / t={age:g} / offline HUD reserve'),
                                          preview.caption(image, f'AFTER / t={age:g} / offline HUD reserve')])
    preview.sheet(cells, 2, 'Source geometry before/after at 854x480, GUI 2, hand FOV 70',
                  'Native HUD-on acceptance is pending. This conservative reserve is not the game HUD.', out / 'comparison.png')
    intersections = preview.intersection_audit(current)
    unsafe = [r for r in intersections if r.get('firstPerson') and not r.get('sameWrist', True)
              and r.get('intersectingSamples', 0) > 0]
    after = hashes()
    passes = (before == after and body_equal and not unsafe and all(
        r['gripAboveHudReserve'] and not r['crosshairCovered']
        and all(r['visiblePixelsAboveHudReserve'][name] >= 12 for name in ['swordHand', 'swordForearm', 'guardHand', 'guardForearm'])
        for r in records))
    report = {'kind': 'offline production-geometry preflight', 'nativeAcceptance': 'not run',
              'worldBodyPalettesAndTimelinesUnchanged': body_equal, 'sourceBefore': before, 'sourceAfter': after,
              'sourceUnchanged': before == after, 'records': records, 'beforeAfter': comparisons,
              'intersections': intersections, 'passes': passes,
              'limits': ['No native rasterization, equipment lighting, glint, depth sorting or actual HUD is simulated.',
                         'Skin and sword pixel counts use the official Steve/Alex and iron sword assets. Native fixture uses the live skin and diamond sword.',
                         'Counts use depth-resolved skin geometry, including sleeves, with a full-width 42-GUI-pixel HUD reserve.',
                         'Pixel matrix samples six authored ages. Separate preview and armor scripts audit all 129 ages and weighted free-look grids.',
                         'Native fixture logs the actual live skin width; synthetic wide/slim submit checks do not claim live screenshots in both widths.',
                         'Head/body poses are byte-identical to the base; the first-person mesh contains arms only. Wrist hinge overlaps are reported separately from cross-arm collisions.']}
    (out / 'composition.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps({'passes': passes, 'bodyUnchanged': body_equal, 'viewports': len(records),
                      'minVisiblePixelsAboveHud': {k: min(r['visiblePixelsAboveHudReserve'][k] for r in records)
                                                  for k in records[0]['visiblePixelsAboveHudReserve']},
                      'native': 'not run'}, indent=2))
    if not passes:
        raise AssertionError('Composition preflight failed; inspect composition.json')


if __name__ == '__main__':
    main()
