#!/usr/bin/env python3
"""Source-only NPC school silhouette preview. No game footage or native acceptance."""
from __future__ import annotations
import argparse
import json
from pathlib import Path
import subprocess
import numpy as np
from PIL import Image
import preview_articulated_combat as base

ROOT = Path(__file__).resolve().parents[1]
SOURCES = ['src/main/java/dev/wildercord/aura/ArticulatedCombatPose.java',
           'src/main/java/dev/wildercord/aura/world/GaleRepriseRules.java',
           'src/main/java/dev/wildercord/aura/world/StoneFractureRules.java',
           'src/client/java/dev/wildercord/client/combat/ArticulatedCombat.java',
           'src/client/java/dev/wildercord/client/combat/ArticulatedRig.java',
           'tools/ExportArticulatedPose.java', 'tools/preview_articulated_combat.py',
           'tools/preview_articulated_schools.py']


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--verification', type=Path, default=base.DEFAULT_VERIFY)
    args = parser.parse_args()
    out = args.out.resolve()
    out.mkdir(parents=True, exist_ok=True)
    if any(out.iterdir()):
        raise ValueError('Use a fresh output directory to preserve earlier evidence')
    before = {p: base.sha(ROOT / p) for p in SOURCES}
    base.export(out, args.verification)
    # Default export retains exactly its old Spellcut/sweep set. Schools are requested explicitly.
    java = Path(json.loads((args.verification / 'toolchain_manifest.json').read_text())['java']['directory']) / 'bin/java'
    command = [str(java), '-cp', str(out / 'classes'), 'ExportArticulatedPose', '--schools']
    with (out / 'poses.json').open('w') as f:
        subprocess.run(command, stdout=f, check=True)
    preview = base.Preview(out, args.verification)
    records = []
    for school, ages, labels in [('gale', [4, 9, 11, 16, 22, 38], ['GATHER', 'LEAD STEP', 'TRAIL STEP', 'REPLY WARNING', 'REPLY', 'RECOVERY']),
                                 ('stone', [4, 12, 24, 28, 32, 52], ['PLANT', 'BRACE', 'OVERHEAD', 'DROP', 'FRACTURE', 'RECOVERY'])]:
        clip = 'gale_crosswind' if school == 'gale' else 'stone_fracture'
        texture = ROOT / f'src/main/resources/assets/wildercord/textures/entity/duelist/{school}.png'
        preview.tex['master'] = np.asarray(Image.open(texture).convert('RGBA'))
        cells = []
        for left in [False, True]:
            for age, label in zip(ages, labels, strict=True):
                frame = preview.frame(clip, left, age)
                raster = base.Raster(440, 425)
                meshes, grip = preview.meshes('master', frame, left)
                image = raster.draw(meshes)
                cells.append(base.caption(image, f'{school.upper()} / {"LEFT" if left else "RIGHT"} / {label}',
                                          f't={age:g} ticks | pure authored pose | weight={frame["weight"]:.3f}'))
                records.append({'school': school, 'left': left, 'age': age, 'visibleSwordPixels': int((raster.ids == 2).sum())})
        base.sheet(cells, 6, f'{school.title()} segmented body and held-sword silhouette',
                   'Actual base rig / school skin. Clothing accessories, baseline/head-look, native lighting and server travel are omitted.',
                   out / f'{school}_school_offline.png')
    # Use the exported native ItemTransform.apply display matrices, not a guessed blade tilt.
    directions = []
    for clip, age in [('gale_crosswind', 22), ('stone_fracture', 24), ('stone_fracture', 32)]:
        for left in [False, True]:
            frame = preview.frame(clip, left, age)
            joint = 'LEFT_SOCKET' if left else 'RIGHT_SOCKET'
            item = (base.matrix(frame['world'][preview.joints[joint]]) @ base.rot('x', -90)
                    @ base.rot('y', 180) @ base.trans(0, 1.327, -1.439)
                    @ base.scale(16, 16, 16) @ base.matrix(preview.geom['itemDisplay'][int(left)]))
            points = base.transform([[3.5 / 16, 3.5 / 16, .5], [13.5 / 16, 13.5 / 16, .5]], item)
            direction = points[1] - points[0]
            direction /= np.linalg.norm(direction)
            if clip == 'gale_crosswind':
                passed = abs(direction[0]) < .03 and abs(direction[1]) < .03 and direction[2] < -.99
            elif age == 24:
                passed = abs(direction[0]) < .03 and direction[1] < -.98 and 0 < direction[2] < .15
            else:
                passed = abs(direction[0]) < .03 and direction[1] > .65 and direction[2] < -.65
            directions.append({'clip': clip, 'left': left, 'age': age, 'bladeDirection': direction.tolist(), 'passes': bool(passed)})
    after = {p: base.sha(ROOT / p) for p in SOURCES}
    report = {'label': 'OFFLINE SOURCE-DRIVEN PREVIEW, NOT GAME FOOTAGE', 'sourceSha256': before,
              'sourceUnchanged': before == after, 'schoolExportCommand': command, 'samples': records, 'canonicalBladeDirections': directions,
              'limits': ['Pure authored body/weapon geometry only; native Master hood, mantle, cloak and scabbard are omitted.',
                         'Partial-weight poses omit live vanilla ready-arm/head-look baseline and cannot establish transition quality.',
                         'Gale ankle offsets do not depict the server entity travel; no movement or damage test is implied.',
                         'GPU skin/layer/fallback/grip acceptance requires actual native captures and visual review.']}
    (out / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    if before != after:
        raise RuntimeError('Sources changed during export')
    if not all(record['passes'] for record in directions):
        raise AssertionError('Authored weapon direction contradicts its intended point/overhead/ground reply')
    print(json.dumps({'output': str(out), 'sourceUnchanged': report['sourceUnchanged'], 'samples': len(records)}))


if __name__ == '__main__':
    main()
