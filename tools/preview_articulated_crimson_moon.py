#!/usr/bin/env python3
"""Fresh source-driven Crimson Moon mesh sheets. Offline; no native rendering claim."""
from __future__ import annotations
import argparse
import json
import os
from pathlib import Path
import subprocess
import numpy as np
import preview_articulated_combat as base
import preview_articulated_ground_fields as fields

ROOT = Path(__file__).resolve().parents[1]
SOURCES = fields.SOURCES + ['tools/preview_articulated_crimson_moon.py']


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--verification', type=Path, default=Path('/workspace/shared/wildercord_standalone'))
    parser.add_argument('--java', type=Path, default=Path('/workspace/shared/toolchains/jdk-25.0.4.1+1/bin'))
    args = parser.parse_args()
    out = args.out.resolve(); out.mkdir(parents=True, exist_ok=True)
    if any(out.iterdir()):
        raise ValueError('Use an empty output directory to preserve evidence')
    before = {name: base.sha(ROOT / name) for name in SOURCES}
    entries = json.loads((args.verification / 'dependency-receipt.json').read_text())
    dependencies = {entry['path']: entry['sha256'] for entry in entries}
    for name, digest in dependencies.items():
        if base.sha(Path(name)) != digest:
            raise ValueError(f'Dependency checksum mismatch: {name}')
    original = args.verification / 'deps/minecraft-26.3-client.jar'
    if str(original) not in dependencies:
        raise ValueError('The untouched official client jar must have a verified dependency receipt')
    jars = [original] + [Path(name) for name in sorted(dependencies) if name.endswith('.jar') and Path(name) != original]
    if any(str(p) not in dependencies for p in jars):
        raise ValueError('Every offline runtime dependency must have a receipt')
    cp = os.pathsep.join(map(str, jars))
    classes = out / 'classes'; classes.mkdir()
    names = ['src/main/java/dev/wildercord/aura/ArticulatedCombatPose.java',
             'src/client/java/dev/wildercord/client/mixin/ModelPartChildrenAccessor.java',
             'src/client/java/dev/wildercord/client/combat/ArticulatedRig.java',
             'tools/ExportArticulatedGeometry.java', 'tools/ExportArticulatedPose.java']
    commands = [[str(args.java / 'javac'), '--release', '25', '-proc:none', '-sourcepath', str(ROOT / 'src/main/java'),
                 '-cp', cp, '-d', str(classes), *[str(ROOT / name) for name in names]]]
    with (out / 'export.log').open('w') as log:
        subprocess.run(commands[-1], stdout=log, stderr=log, check=True)
        for exporter, filename in [('ExportArticulatedGeometry', 'geometry.json'), ('ExportArticulatedPose', 'poses.json')]:
            command = [str(args.java / 'java'), '-cp', str(classes) + os.pathsep + cp, exporter, '--crimson-moon']
            commands.append(command)
            with (out / filename).open('w') as stream:
                subprocess.run(command, stdout=stream, stderr=log, check=True)
    # Existing rasterizer reads pristine assets via this documented local link, never a rewritten compile jar.
    assets = out / 'assets/originals'; assets.mkdir(parents=True)
    (assets / 'minecraft-client.jar').symlink_to(original)
    preview = base.Preview(out, out / 'assets')
    hud = []
    for first, reverse in [(False, False), (False, True), (True, False)]:
        cells = []
        for variant in ['wide', 'slim']:
            for left in [False, True]:
                for age in [6.5, 10, 14, 27]:
                    frame = preview.frame('crimson_moon', left, age)
                    raster = base.Raster(500, 282 if first else 450, first, 70)
                    if not first:
                        raster.factor *= .85
                        if reverse:
                            raster.eye = np.array([-37., -11., 65.]); target = np.array([0., 9., 0.])
                            f = target - raster.eye; f /= np.linalg.norm(f)
                            right = np.cross(f, [0, -1, 0]); right /= np.linalg.norm(right)
                            raster.camera = np.array([right, np.cross(right, f), f])
                    meshes, _ = preview.meshes(variant, frame, left, first)
                    cells.append(base.caption(raster.draw(meshes), f'{variant} / {"left" if left else "right"} / age {age:g}',
                                              f'{frame["phase"]} | OFFLINE source geometry'))
        base.sheet(cells, 4, 'Crimson Moon / ' + ('first person' if first else 'opposite oblique' if reverse else 'front oblique'),
                   'Release at 10; damage at 11–15. Pure source pose, no live baseline, armor, shell or native pixels.',
                   out / ('moon_first_offline.png' if first else 'moon_opposite_offline.png' if reverse else 'moon_third_offline.png'))
    for variant in ['wide', 'slim']:
        for left in [False, True]:
            for viewport in [[854, 480, 2], [1280, 720, 3], [1280, 960, 4], [1920, 810, 3]]:
                for age in [0, 6.5, 10, 14, 27, 30]:
                    record, _ = fields.hud_sample(preview, 'crimson_moon', variant, left, age, viewport)
                    hud.append(record)
    near = []
    for variant in ['wide', 'slim']:
        for left in [False, True]:
            closest, where, samples = -np.inf, None, 0
            for frame in preview.clips[('crimson_moon', left)]['frames']:
                meshes, _ = preview.meshes(variant, frame, left, True)
                opaque = np.concatenate([m[0] for m in meshes if m[3] != 'SWORD'] + [m[0] for m in meshes if m[3] == 'SWORD'][2:])
                origin = np.array(frame['firstPerson']['origin'])
                for yaw, pitch, clearance in preview.geom['clearanceGrid']:
                    w = frame['weight']
                    vertices = base.transform(opaque - origin, base.rot('y', -yaw * w) @ base.rot('x', -pitch * w))
                    vertices += origin + [0, -.8 * clearance * w, -clearance * w]
                    z = float(vertices[:, 2].max()); samples += 1
                    if z > closest: closest, where = z, {'age': frame['age'], 'yaw': yaw, 'pitch': pitch}
            near.append({'variant': variant, 'left': left, 'placements': samples, 'nearestCameraZ': closest, 'worst': where, 'passes': closest < -.05})
    after = {name: base.sha(ROOT / name) for name in SOURCES}
    report = {'label': 'OFFLINE SOURCE-DRIVEN PREVIEW, NOT GAME FOOTAGE', 'sourceSha256': before,
              'sourceUnchanged': before == after, 'dependencySha256': dependencies,
              'artifactSha256': {p.name: base.sha(p) for p in out.iterdir() if p.suffix in {'.json', '.png'}},
              'toolchainExecutableSha256': {name: base.sha(args.java / name) for name in ['java', 'javac']},
              'commands': commands, 'officialModelPartParity': preview.geom['verification'],
              'geometryChecks': preview.geometry_checks(), 'hudSamples': hud, 'nearPlane': near,
              'limits': ['No native game, mixin, deferred renderer, server input or actual screenshot is represented.',
                         'Third-person pure-pose partial weight omits live held-arm, head, idle and breathing baselines.',
                         'Netherite, funded Aura shell, material passes and genuine remote observers remain native gates.',
                         'Opaque iron-sword edges are reconstructed from official sprite alpha; item transforms are official.',
                         'The three deferred geometry, complete frame/item identity and fallback-eligibility proof gaps remain open.']}
    (out / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    assert before == after, 'Sources changed during export'
    assert all(row['passes'] for row in near), 'Actual opaque geometry intersects near plane'
    assert all(row['gripAboveHudReserve'] and not row['crosshairCovered'] and all(n >= 12 for n in row['visiblePixelsAboveHudReserve'].values()) for row in hud), 'Existing HUD composition policy failed'
    print(out, flush=True)


if __name__ == '__main__':
    main()
