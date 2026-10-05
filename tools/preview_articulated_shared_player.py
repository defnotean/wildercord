#!/usr/bin/env python3
"""Source-driven shared-player pose/mesh review. OFFLINE, never native gameplay evidence."""
from __future__ import annotations
import argparse
import json
from pathlib import Path
import subprocess
import numpy as np
import preview_articulated_combat as base

ROOT = Path(__file__).resolve().parents[1]
SOURCES = ['src/main/java/dev/wildercord/aura/ArticulatedCombatPose.java',
           'src/main/java/dev/wildercord/aura/MastersArtRules.java',
           'src/client/java/dev/wildercord/client/combat/ArticulatedCombat.java',
           'src/client/java/dev/wildercord/client/combat/ArticulatedRig.java',
           'src/client/java/dev/wildercord/client/combat/ArticulatedViewModel.java',
           'tools/ExportArticulatedPose.java', 'tools/ExportArticulatedGeometry.java',
           'tools/preview_articulated_combat.py', 'tools/preview_articulated_shared_player.py']


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--verification', type=Path, default=base.DEFAULT_VERIFY)
    parser.add_argument('--visual-only', action='store_true')
    args = parser.parse_args()
    out = args.out.resolve()
    out.mkdir(parents=True, exist_ok=True)
    if any(out.iterdir()):
        raise ValueError('Use a fresh output directory to preserve evidence')
    before = {name: base.sha(ROOT / name) for name in SOURCES}
    base.export(out, args.verification)
    java = Path(json.loads((args.verification / 'toolchain_manifest.json').read_text())['java']['directory']) / 'bin/java'
    command = [str(java), '-cp', str(out / 'classes'), 'ExportArticulatedPose', '--shared-player']
    with (out / 'poses.json').open('w') as stream:
        subprocess.run(command, stdout=stream, check=True)
    preview = base.Preview(out, args.verification)
    for clip, ages in [('rising_break', [5.25, 8, 12, 21]), ('driving_cut', [4, 6, 9.5, 16])]:
        for first in [False, True]:
            cells = []
            for variant, left in [('wide', False), ('slim', True)]:
                for age in ages:
                    frame = preview.frame(clip, left, age)
                    raster = base.Raster(500, 282 if first else 450, first, 70)
                    if not first:
                        raster.factor *= .85  # Keep the high Rising finish and blade tip inside the inspection frame.
                    meshes, grip = preview.meshes(variant, frame, left, first)
                    cells.append(base.caption(raster.draw(meshes), f'{variant.upper()} / {"LEFT" if left else "RIGHT"} / {frame["phase"]}',
                                              f't={age:g} ticks | weight={frame["weight"]:.3f} | source geometry'))
            base.sheet(cells, 4, f'{clip.replace("_", " ").title()} / {"first person" if first else "third person"}',
                       'Original pure poses; live idle/head baseline, armor layers, lighting and native rendering are omitted.',
                       out / f'{clip}_{"first" if first else "third"}_offline.png')
    report = {'label': 'OFFLINE SOURCE-DRIVEN PREVIEW, NOT GAME FOOTAGE', 'sourceSha256': before,
              'exportCommand': command, 'geometryParity': preview.geom['verification'], 'geometryChecks': preview.geometry_checks(),
              'limits': ['Offline software rasterization of exported production mesh and pose matrices, not Minecraft/GPU capture.',
                         'The item display matrix is exported from official ItemTransform.apply; its opaque sprite edge mesh is reconstructed.',
                         'Third-person partial-weight samples omit live held-arm/head/breathing baselines.',
                         'Armor, resource packs, deferred submission, actual input and native visual quality remain separate acceptance gates.']}
    if not args.visual_only:
        near, occlusion = [], []
        for clip in ['rising_break', 'driving_cut']:
            for variant in ['wide', 'slim']:
                for left in [False, True]:
                    closest, where, checked = -np.inf, None, 0
                    worst_center, crosshair, min_sword, peak = 0, [], 10**9, 0
                    for frame in preview.clips[(clip, left)]['frames']:
                        meshes, grip = preview.meshes(variant, frame, left, True)
                        canonical = np.concatenate([m[0] for m in meshes if m[3] != 'SWORD']
                                                   + [m[0] for m in meshes if m[3] == 'SWORD'][2:])
                        origin = np.array(frame['firstPerson']['origin'])
                        for yaw, pitch, clearance in preview.geom['clearanceGrid']:
                            weight = frame['weight']
                            vertices = base.transform(canonical - origin, base.rot('y', -yaw * weight) @ base.rot('x', -pitch * weight))
                            vertices += origin + [0, -.8 * clearance * weight, -clearance * weight]
                            z = float(vertices[:, 2].max())
                            checked += 1
                            if z > closest:
                                closest, where = z, {'age': frame['age'], 'yaw': yaw, 'pitch': pitch}
                        raster = base.Raster(256, 144, True, 70)
                        raster.draw(meshes)
                        center = float((raster.ids[68:76, 121:135] > 0).mean())
                        worst_center = max(worst_center, center)
                        peak = max(peak, float((raster.ids > 0).mean()))
                        min_sword = min(min_sword, int((raster.ids == 2).sum()))
                        if raster.ids[72, 128] > 0:
                            crosshair.append(frame['age'])
                    near.append({'clip': clip, 'variant': variant, 'left': left, 'placementsTested': checked,
                                 'nearestCameraZ': closest, 'nearPlaneZ': -.05, 'passes': closest < -.05, 'worst': where})
                    occlusion.append({'clip': clip, 'variant': variant, 'left': left, 'handFov': 70,
                                      'maxCentral5PercentCoverage': worst_center, 'crosshairOccludedAges': crosshair,
                                      'minSwordPixelsAt256x144': min_sword, 'maxScreenCoverage': peak})
                    print(clip, variant, left, 'near', closest, 'center', worst_center, 'sword', min_sword, flush=True)
        report['nearPlane'] = near
        report['canonicalAimPixels'] = occlusion
    after = {name: base.sha(ROOT / name) for name in SOURCES}
    report['sourceUnchanged'] = before == after
    (out / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    assert before == after, 'Sources changed during export'
    if not args.visual_only:
        assert all(row['passes'] for row in near), 'Opaque geometry intersects near plane'
        assert all(not row['crosshairOccludedAges'] for row in occlusion), 'Opaque geometry covers canonical reticle'
    print(out, flush=True)


if __name__ == '__main__':
    main()
