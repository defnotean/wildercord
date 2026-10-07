#!/usr/bin/env python3
"""Source-driven Unmoved/Null Parry pose/mesh review. OFFLINE, never native gameplay evidence."""
from __future__ import annotations
import argparse
import json
import math
from pathlib import Path
import subprocess
import numpy as np
from PIL import Image, ImageDraw
import preview_articulated_combat as base

ROOT = Path(__file__).resolve().parents[1]
SOURCES = ['src/main/java/dev/wildercord/aura/ArticulatedCombatPose.java',
		   'src/main/java/dev/wildercord/aura/ArtRules.java',
		   'src/main/java/dev/wildercord/aura/MastersArtRules.java',
           'src/main/java/dev/wildercord/aura/MastersStyleAnimation.java',
           'src/main/java/dev/wildercord/aura/MastersArtAnimation.java',
           'src/main/java/dev/wildercord/aura/MastersStyleRules.java',
		   'src/main/java/dev/wildercord/aura/world/GaleRepriseRules.java',
		   'src/main/java/dev/wildercord/aura/world/StoneFractureRules.java',
           'src/client/java/dev/wildercord/client/combat/ArticulatedCombat.java',
           'src/client/java/dev/wildercord/client/combat/ArticulatedRig.java',
           'src/client/java/dev/wildercord/client/combat/ArticulatedViewModel.java',
           'tools/ExportArticulatedPose.java', 'tools/ExportBraceNullPose.java', 'tools/ExportArticulatedGeometry.java',
           'tools/preview_articulated_combat.py', 'tools/earned_counter_offline.py', 'tools/preview_articulated_brace_null.py']


def hand_collisions(preview):
    """Base-cuboid SAT excludes expected hinge overlap and never implies gameplay collision."""
    pairs = [('RIGHT_HAND', 'LEFT_HAND'), ('RIGHT_FOREARM', 'LEFT_HAND'),
             ('RIGHT_HAND', 'LEFT_FOREARM'), ('RIGHT_FOREARM', 'LEFT_FOREARM')]
    records = []
    for clip in ['unmoved', 'null_parry']:
        for variant in ['wide', 'slim']:
            for left in [False, True]:
                for first in [False, True]:
                    frames = preview.clips[(clip, left)]['frames']
                    def obb(frame, joint):
                        cube = next(c for c in preview.variants[variant]['cubes'] if c['path'].split('/')[-1] == joint)
                        vertices = np.array([v[:3] for f in cube['faces'] for v in f['vertices']])
                        lo, hi = vertices.min(0), vertices.max(0)
                        matrix = base.matrix((frame['firstPerson'] if first else frame)['world'][preview.joints[joint]])
                        return base.transform([(lo + hi) / 2], matrix)[0], matrix[:3, :3], (hi - lo) / 2
                    for a, b in pairs:
                        overlap = []
                        for frame in frames:
                            ac, aa, ah = obb(frame, a)
                            bc, ba, bh = obb(frame, b)
                            separated = False
                            for axis in [*aa.T, *ba.T, *[np.cross(i, j) for i in aa.T for j in ba.T]]:
                                length = np.linalg.norm(axis)
                                if length < 1e-8:
                                    continue
                                axis = axis / length
                                depth = np.abs(aa.T @ axis) @ ah + np.abs(ba.T @ axis) @ bh - abs((ac - bc) @ axis)
                                if depth <= 1e-5:
                                    separated = True
                                    break
                            if not separated:
                                overlap.append(frame['age'])
                        records.append({'clip': clip, 'variant': variant, 'left': left, 'firstPerson': first,
                                        'pair': [a, b], 'crossArmIntersectingAges': overlap})
    return records


def hud_sample(preview, clip, variant, left, age, viewport):
    width, height, gui = viewport
    frame = preview.frame(clip, left, age)
    raster = base.Raster(width, height, True, 70)
    meshes, grip = preview.meshes(variant, frame, left, True)
    dominant = 'LEFT_' if left else 'RIGHT_'
    guard = 'RIGHT_' if left else 'LEFT_'
    identities = {'SWORD': 1, dominant + 'HAND': 2, dominant + 'FOREARM': 3,
                  guard + 'HAND': 4, guard + 'FOREARM': 5}
    for points, uv, texture, joint, overlay in meshes:
        projected = raster.project(points)
        for triangle in [[0, 1, 2], [0, 2, 3]]:
            raster.triangle(projected[triangle], uv[triangle], texture, .85, identities.get(joint, 6))
    hud_top = height - 42 * gui
    names = ['sword', 'swordHand', 'swordForearm', 'guardHand', 'guardForearm']
    counts = {name: int((raster.ids[:hud_top] == identity).sum()) for identity, name in enumerate(names, 1)}
    projected_grip = raster.project(np.array([grip]))[0]
    result = {'clip': clip, 'variant': variant, 'left': left, 'age': age, 'viewport': viewport,
              'visiblePixelsAboveHudReserve': counts, 'gripAboveHudReserve': bool(projected_grip[1] < hud_top),
              'crosshairCovered': bool(raster.ids[height // 2, width // 2])}
    image = Image.fromarray(raster.rgb)
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, hud_top, width - 1, height - 1), fill=(36, 41, 50))
    draw.text((10, hud_top + 6), 'CONSERVATIVE HUD RESERVE (not the game HUD)', fill=(210, 215, 225))
    draw.line((width // 2 - 6, height // 2, width // 2 + 6, height // 2), fill=(230, 235, 240))
    draw.line((width // 2, height // 2 - 6, width // 2, height // 2 + 6), fill=(230, 235, 240))
    return result, image

def release_inspection(preview, out):
    """Actual opaque sprite extremes at each counter form's chamber, release and recovery."""
    records, cells = [], []
    for left in [False, True]:
        for clip, ages in [('unmoved', [3.875, 4.5, 5, 6, 7, 10]), ('null_parry', [2.625, 3, 3.5, 4, 5, 7.5])]:
            for age in ages:
                frame = preview.frame(clip, left, age)
                meshes, grip = preview.meshes('wide', frame, left, True)
                opaque = np.concatenate([m[0] for m in meshes if m[3] == 'SWORD'][2:])
                tip = opaque[np.argmax(np.linalg.norm(opaque - grip, axis=1))]
                projected = base.Raster(854, 480, True, 70).project(np.array([grip, tip]))
                vector = projected[1, :2] - projected[0, :2]
                angle = math.degrees(math.atan2(vector[1], vector[0]))
                records.append({'clip': clip, 'left': left, 'age': age, 'phase': frame['phase'], 'viewport': [854, 480],
                                'handFov': 70, 'gripPixels': projected[0, :2].tolist(),
                                'farthestOpaqueTipPixels': projected[1, :2].tolist(),
                                'projectedBladeAngleDegrees': angle, 'projectedBladeLengthPixels': float(np.linalg.norm(vector))})
                cells.append(base.caption(base.Raster(420, 236, True, 70).draw(meshes),
                                          f'{clip} / {"LEFT" if left else "RIGHT"} / t={age:g}',
                                          f'{frame["phase"]} | blade angle {angle:.1f} degrees | OFFLINE'))
    base.sheet(cells, 6, 'Unmoved brace / Null Parry counter',
               'Source geometry; opaque sprite extremes. Each earned counter has one physical ACTIVE release; the opening does not replay the blade.',
               out / 'brace_null_release_interval_offline.png')
    return {'samples': records}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--verification', type=Path, default=Path('/workspace/shared/wildercord_standalone'))
    parser.add_argument('--visual-only', action='store_true')
    args = parser.parse_args()
    out = args.out.resolve()
    out.mkdir(parents=True, exist_ok=True)
    if any(out.iterdir()):
        raise ValueError('Use a fresh output directory to preserve evidence')
    before = {name: base.sha(ROOT / name) for name in SOURCES}
    from earned_counter_offline import export, java_bin
    asset_cache = export(out, args.verification)
    java = java_bin(args.verification) / 'java'
    subprocess.run([str(java.with_name('javac')), '-proc:none', '--release', '25', '-cp', str(out / 'classes'), '-d', str(out / 'classes'), str(ROOT / 'tools/ExportBraceNullPose.java')], check=True)
    command = [str(java), '-cp', str(out / 'classes'), 'ExportBraceNullPose']
    with (out / 'poses.json').open('w') as stream:
        subprocess.run(command, stdout=stream, check=True)
    preview = base.Preview(out, asset_cache)
    for clip, ages in [('unmoved', [3.875, 6, 10, 20]), ('null_parry', [2.625, 4, 7.5, 16])]:
        for first in [False, True]:
            cells = []
            for variant, left in [(variant, left) for variant in ['wide', 'slim'] for left in [False, True]]:
                for age in ages:
                    frame = preview.frame(clip, left, age)
                    raster = base.Raster(500, 282 if first else 450, first, 70)
                    if not first:
                        raster.factor *= .85  # Keep the complete finish and blade tip inside the inspection frame.
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
                         'Armor, funded Aura shell, resource packs, deferred submission, actual input and native visual quality remain separate acceptance gates.',
                         'Ages at 0.125-tick intervals cover accepted Unmoved 6/16 and Null Parry 4/14 windows; geometry has no server collision authority.']}
    report['releaseInspection'] = release_inspection(preview, out)
    if not args.visual_only:
        near, occlusion = [], []
        for clip in ['unmoved', 'null_parry']:
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
        collisions = hand_collisions(preview)
        report['crossArmBaseCuboids'] = collisions
        hud, hud_cells = [], []
        for clip in ['unmoved', 'null_parry']:
            for variant in ['wide', 'slim']:
                for left in [False, True]:
                    for viewport in [[854, 480, 2], [1280, 720, 3], [1280, 960, 4], [1920, 810, 3]]:
                        for age in ([0, 3.875, 6, 10, 20, 22] if clip == 'unmoved' else [0, 2.625, 4, 7.5, 16, 18]):
                            record, rendered = hud_sample(preview, clip, variant, left, age, viewport)
                            hud.append(record)
                            if viewport == [854, 480, 2] and variant == 'wide' and not left and age in ([3.875, 6, 10] if clip == 'unmoved' else [2.625, 4, 7.5]):
                                hud_cells.append(base.caption(rendered, f'{clip} / {age:g} ticks / HUD70', 'OFFLINE conservative reserve'))
        base.sheet(hud_cells, 3, 'Unmoved and Null Parry / native-sized HUD composition preflight',
                   'Reserved band only, never Minecraft HUD screenshots. Live materials and shell require native acceptance.', out / 'hud_reserve_offline.png')
        report['hudSamples'] = hud
        report['nearPlane'] = near
        report['canonicalAimPixels'] = occlusion
    after = {name: base.sha(ROOT / name) for name in SOURCES}
    report['sourceUnchanged'] = before == after
    (out / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    assert before == after, 'Sources changed during export'
    if not args.visual_only:
        assert all(not row['crossArmIntersectingAges'] for row in collisions), 'Independently posed hands/forearms intersect'
        assert all(r['gripAboveHudReserve'] and not r['crosshairCovered'] and all(v >= 12 for v in r['visiblePixelsAboveHudReserve'].values()) for r in hud), 'HUD-reserve composition failed'
        assert all(row['passes'] for row in near), 'Opaque geometry intersects near plane'
        assert all(not row['crosshairOccludedAges'] for row in occlusion), 'Opaque geometry covers canonical reticle'
    print(out, flush=True)


if __name__ == '__main__':
    main()
