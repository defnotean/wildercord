#!/usr/bin/env python3
"""Bounded OFFLINE shared-player armor/skin/sword contact sheets; never game footage.

Uses only the already-verified official Java 25 and pristine runtime dependency cache.
Exports production setupAnim ModelPart quads for four keyframes per clip, wide/slim,
and both hands. No installation, client launch, network or external write is performed.
"""
from __future__ import annotations

import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import subprocess
import zipfile

import numpy as np
from PIL import Image, ImageDraw
import preview_articulated_combat as base

ROOT = Path(__file__).resolve().parents[1]
JAVA_SOURCES = [
    'src/main/java/dev/wildercord/aura/ArticulatedCombatPose.java',
    'src/main/java/dev/wildercord/aura/MastersArtRules.java',
    'src/main/java/dev/wildercord/aura/ArticulatedArmorMesh.java',
    'src/client/java/dev/wildercord/client/mixin/ModelPartChildrenAccessor.java',
    'src/client/java/dev/wildercord/client/combat/ArticulatedRig.java',
    'src/client/java/dev/wildercord/client/combat/ArticulatedArmorGeometry.java',
    'tools/ExportArticulatedGeometry.java',
    'tools/ExportArticulatedPose.java',
    'tools/ExportArticulatedSharedArmor.java',
]
SOURCE_PATHS = JAVA_SOURCES + [
    'src/main/java/dev/wildercord/aura/world/GaleRepriseRules.java',
    'src/main/java/dev/wildercord/aura/world/StoneFractureRules.java',
    'src/client/java/dev/wildercord/client/combat/ArticulatedCombat.java',
    'src/client/java/dev/wildercord/client/combat/ArticulatedViewModel.java',
    'src/client/java/dev/wildercord/client/combat/ArticulatedArmorRenderer.java',
    'tools/preview_articulated_combat.py',
    'tools/preview_articulated_shared_armor.py',
]
LABEL = 'OFFLINE SOURCE-DRIVEN PREVIEW, NOT GAME FOOTAGE'
LIMITS = [
    'Offline software rasterization of exported production ModelPart geometry; no Minecraft client, GPU, mixin runtime or native acceptance.',
    'Four pure-pose keyframes per clip are a bounded visual sample, not continuous geometry or exhaustive collision validation.',
    'Third-person partial-weight samples omit the live idle, breathing, held-arm and head/free-look baseline.',
    'Full armor means all four original vanilla equipment-slot bakes; first person uses the production chestplate arms-only mesh.',
    'Original netherite humanoid/leggings alpha is sampled with nearest filtering and alpha >127; resource packs are omitted.',
    'Material passes, trim, enchantment glint, native lighting and the deferred renderer are omitted; simple offline directional shading is used.',
    'The iron-sword display transform is exported from official ItemTransform.apply; its opaque sprite edge mesh is reconstructed.',
    'Camera-space first-person sheets use the source hand FOV of 70 and exact exported weighted free-look/clearance transforms.',
]


def sources():
    return {name: base.sha(ROOT / name) for name in SOURCE_PATHS}


def export(out, verify, report):
    toolchain = json.loads((verify / 'toolchain_manifest.json').read_text())
    java = Path(toolchain['java']['directory']) / 'bin'
    if not toolchain['java']['checksum_verified'] or not toolchain['java']['version'].startswith('25.'):
        raise ValueError('A verified official Java 25 toolchain is required')
    dependencies, seen, hashes = [], set(), {}
    for name in ['dependency_manifest.json', 'embedded_manifest.json']:
        for entry in json.loads((verify / name).read_text()):
            path = verify / entry['path']
            if path.suffix != '.jar' or 'iris' in str(path) or path.name in seen:
                continue
            digest = base.sha(path)
            if digest != entry['sha256']:
                raise ValueError(f'Dependency checksum mismatch: {path}')
            if 'compile_only' in path.parts:
                raise ValueError('Rewritten compile-only jars cannot be used for geometry evidence')
            seen.add(path.name)
            dependencies.append(path)
            hashes[str(path)] = digest
    required = [verify / 'originals/minecraft-client.jar', verify / 'originals/minecraft-server-inner.jar']
    if any(path not in dependencies for path in required):
        raise ValueError('Pristine official runtime jars are missing from the verified manifests')
    dependencies = required + [path for path in dependencies if path not in required]
    report['dependencySha256'] = hashes
    report['toolchain'] = toolchain['java']
    report['toolchainExecutableSha256'] = {name: base.sha(java / name) for name in ['java', 'javac']}
    cp = os.pathsep.join(map(str, dependencies))
    classes = out / 'classes'
    classes.mkdir()
    commands = [[str(java / 'javac'), '--release', '25', '-proc:none', '-sourcepath', str(ROOT / 'src/main/java'),
                 '-cp', cp, '-d', str(classes), *[str(ROOT / name) for name in JAVA_SOURCES]]]
    report['commands'] = commands
    with (out / 'export.log').open('w') as log:
        subprocess.run(commands[0], cwd=ROOT, stdout=log, stderr=log, check=True)
        for main, filename, args in [('ExportArticulatedGeometry', 'geometry.json', []),
                                     ('ExportArticulatedPose', 'poses.json', ['--shared-player']),
                                     ('ExportArticulatedSharedArmor', 'armor.json', [])]:
            command = [str(java / 'java'), '-Xmx2G', '-cp', str(classes) + os.pathsep + cp, main, *args]
            commands.append(command)
            with (out / filename).open('w') as stream:
                subprocess.run(command, cwd=ROOT, stdout=stream, stderr=log, check=True)


class ArmorRaster(base.Raster):
    """Same software raster as the bare preview, with separate armor/skin/item IDs."""
    def draw(self, meshes):
        for points, uv, texture, joint, overlay in meshes:
            projected = self.project(points)
            normal = np.cross(points[1] - points[0], points[2] - points[0])
            normal /= max(np.linalg.norm(normal), 1e-10)
            light = np.array([-.3, -.7, -.65])
            light /= np.linalg.norm(light)
            shade = .68 + .32 * abs(float(normal.dot(light)))
            identity = 3 if joint.startswith('ARMOR_') else 2 if joint == 'SWORD' else 1
            for triangle in [[0, 1, 2], [0, 2, 3]]:
                self.triangle(projected[triangle], uv[triangle], texture, shade, identity)
        image = Image.fromarray(self.rgb)
        if self.first:
            draw = ImageDraw.Draw(image)
            x, y = self.w // 2, self.h // 2
            draw.line((x - 7, y, x + 7, y), fill=(220, 228, 230))
            draw.line((x, y - 7, x, y + 7), fill=(220, 228, 230))
            draw.rectangle((x - self.w * .025, y - self.h * .025, x + self.w * .025, y + self.h * .025), outline=(52, 79, 88))
        return image


def armor_textures(verify, report):
    jar = verify / 'originals/minecraft-client.jar'
    textures = {}
    with zipfile.ZipFile(jar) as archive:
        for key in ['humanoid', 'humanoid_leggings']:
            entry = f'assets/minecraft/textures/entity/equipment/{key}/netherite.png'
            data = archive.read(entry)
            textures[key] = np.array(Image.open(io.BytesIO(data)).convert('RGBA'))
            report['armorAssets'][key] = {'jar': str(jar), 'entry': entry, 'sha256': hashlib.sha256(data).hexdigest(),
                                        'size': list(textures[key].shape[:2]), 'alphaValues': np.unique(textures[key][:, :, 3]).tolist()}
    return textures


def combined(preview, record, textures, first, placement=0):
    variant, left = record['variant'], record['left']
    frame = preview.frame(record['clip'], left, record['age'])
    if abs(frame['age'] - record['age']) > 1e-7:
        raise ValueError('Armor/skin keyframe mismatch')
    for exported, body in [('bodyWorld', frame), ('viewWorld', frame['firstPerson'])]:
        if not np.allclose(record[exported], body['world'], rtol=0, atol=1e-6):
            raise ValueError('Armor and skin/sword palette exports differ')
    meshes, _ = preview.meshes(variant, frame, left, first)
    camera = base.matrix(record['viewPlacements'][placement]['camera']) if first else np.eye(4)
    if first and placement:
        relative = camera @ np.linalg.inv(base.matrix(record['viewPlacements'][0]['camera']))
        meshes = [(base.transform(p, relative), uv, texture, joint, overlay) for p, uv, texture, joint, overlay in meshes]
    slots = [record['first']] if first else record['third']
    for slot in slots:
        texture = textures['humanoid_leggings' if slot['slot'] == 'LEGS' else 'humanoid']
        for face in slot['faces']:
            vertices = np.array(face)
            meshes.append((base.transform(vertices[:, :3], camera), vertices[:, 3:], texture, 'ARMOR_' + slot['slot'], False))
    return meshes


def render(out, verify, report):
    preview = base.Preview(out, verify)
    exported = json.loads((out / 'armor.json').read_text())
    records = {(r['clip'], r['variant'], r['left'], r['age']): r for r in exported['frames']}
    if len(records) != 32 or len(exported['frames']) != 32:
        raise ValueError('Expected 32 distinct armor pose records')
    report['armorAssets'] = {}
    textures = armor_textures(verify, report)
    report['skinAndSwordAssets'] = {key: value for key, value in preview.asset_sources.items() if key != 'master'}
    report['geometryParity'] = preview.geom['verification']
    report['smokeChecks'], report['sheets'] = [], []
    for clip, ages in [('rising_break', [5.25, 8, 12, 21]), ('driving_cut', [4, 6, 9.5, 16])]:
        for first, stress in [(False, False), (True, False), (True, True)]:
            cells = []
            for variant in ['wide', 'slim']:
                for left in [False, True]:
                    for age in ages:
                        record = records[(clip, variant, left, age)]
                        placement = (1 if left else 2) if stress else 0
                        meshes = combined(preview, record, textures, first, placement)
                        raster = ArmorRaster(500, 282 if first else 450, first, 70)
                        if not first:
                            raster.factor *= .85
                        image = raster.draw(meshes)
                        subtitle = f't={age:g} | weight={record["weight"]:.3f} | '
                        if first:
                            p = record['viewPlacements'][placement]
                            subtitle += f'HUD70 yaw {p["yaw"]:+.1f}, pitch {p["pitch"]:+.1f}'
                        else:
                            subtitle += 'HEAD + CHEST + LEGS + FEET'
                        cells.append(base.caption(image, f'{variant.upper()} / {"LEFT" if left else "RIGHT"} / {record["phase"]}', subtitle))
                        # Smoke evidence only, over the rendered bounded keyframes. Parent numeric gates are separate.
                        opaque_meshes = [m[0] for m in meshes if m[3] != 'SWORD'] + [m[0] for m in meshes if m[3] == 'SWORD'][2:]
                        vertices = np.concatenate(opaque_meshes)
                        check = {'clip': clip, 'variant': variant, 'left': left, 'age': age, 'firstPerson': first,
                                 'freeLookStress': stress, 'skinPixels': int((raster.ids == 1).sum()),
                                 'swordPixels': int((raster.ids == 2).sum()), 'armorPixels': int((raster.ids == 3).sum()),
                                 'finiteVertices': bool(np.isfinite(vertices).all())}
                        if first:
                            check['nearestCameraZ'] = float(vertices[:, 2].max())
                            check['nearPlaneClear'] = check['nearestCameraZ'] < -.05
                            check['crosshairCovered'] = bool(raster.ids[raster.h // 2, raster.w // 2])
                        report['smokeChecks'].append(check)
            suffix = 'first_free_look' if stress else 'first' if first else 'third'
            filename = f'{clip}_netherite_{suffix}_offline.png'
            title = clip.replace('_', ' ').title() + ' / ' + ('enhanced first-person armor arms' if first else 'full netherite over skin')
            if stress:
                title += ' / free-look stress'
            subtitle = 'Production shell + stock skin/sword; original texture alpha. Material/glint/trim/native lighting omitted.'
            base.sheet(cells, 4, title, subtitle, out / filename)
            report['sheets'].append({'path': filename, 'sha256': base.sha(out / filename), 'cells': len(cells)})
            print(filename, flush=True)
    checks = report['smokeChecks']
    report['boundedSmokePasses'] = all(row['finiteVertices'] and row['armorPixels'] > 0 and row['swordPixels'] > 0
                                     and row.get('nearPlaneClear', True) for row in checks)
    report['canonicalFirstPersonCrosshairClear'] = all(not row['crosshairCovered'] for row in checks
                                                      if row['firstPerson'] and not row['freeLookStress'])
    if not report['boundedSmokePasses'] or not report['canonicalFirstPersonCrosshairClear']:
        raise AssertionError('Bounded armor/skin/sword smoke check failed; inspect the report before acceptance')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--verification', type=Path, default=base.DEFAULT_VERIFY)
    args = parser.parse_args()
    out, verify = args.out.resolve(), args.verification.resolve()
    out.mkdir(parents=True, exist_ok=True)
    if any(out.iterdir()):
        raise ValueError('Use a fresh empty output directory to preserve evidence')
    before = sources()
    report = {'label': LABEL, 'sourceSha256': before, 'limits': LIMITS, 'passed': False}
    try:
        export(out, verify, report)
        render(out, verify, report)
        report['passed'] = True
    finally:
        report['sourceAfterSha256'] = sources()
        report['sourceUnchanged'] = before == report['sourceAfterSha256']
        report['passed'] &= report['sourceUnchanged']
        report['exportSha256'] = {name: base.sha(out / name) for name in ['geometry.json', 'poses.json', 'armor.json'] if (out / name).exists()}
        (out / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    if not report['sourceUnchanged']:
        raise RuntimeError('Sources changed during export; this evidence is invalid')
    print(f'Bounded offline smoke checks passed; native rendering remains unrun. Evidence: {out / "report.json"}', flush=True)


if __name__ == '__main__':
    main()
