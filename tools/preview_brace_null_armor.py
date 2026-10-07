#!/usr/bin/env python3
"""Bounded OFFLINE Unmoved/Null Parry production armor and shell polygon contact sheets.

No game, GPU, Loom, installation, network request, publication or native acceptance.
Fresh source geometry runs only with checksum-verified pristine runtime dependencies.
"""
from __future__ import annotations
import argparse
import json
import os
from pathlib import Path
import subprocess
import numpy as np
from PIL import ImageDraw
import preview_articulated_combat as base
from preview_articulated_shared_armor import ArmorRaster, armor_textures, combined
from earned_counter_offline import dependencies, java_bin

ROOT = Path(__file__).resolve().parents[1]
JAVA_SOURCES = [
    'src/main/java/dev/wildercord/aura/ArticulatedCombatPose.java',
    'src/main/java/dev/wildercord/aura/ArticulatedArmorMesh.java',
    'src/main/java/dev/wildercord/aura/MastersArtRules.java',
    'src/main/java/dev/wildercord/aura/MastersStyleRules.java',
    'src/client/java/dev/wildercord/client/mixin/ModelPartChildrenAccessor.java',
    'src/client/java/dev/wildercord/client/combat/ArticulatedRig.java',
    'src/client/java/dev/wildercord/client/combat/ArticulatedArmorGeometry.java',
    'src/client/java/dev/wildercord/client/combat/ArticulatedAuraShellGeometry.java',
    'src/client/java/dev/wildercord/client/render/AuraShellLayer.java',
    'tools/ExportArticulatedGeometry.java', 'tools/ExportBraceNullPose.java', 'tools/ExportBraceNullArmor.java',
]
SOURCE_PATHS = JAVA_SOURCES + [
    'src/client/java/dev/wildercord/client/combat/ArticulatedCombat.java',
    'src/client/java/dev/wildercord/client/combat/ArticulatedViewModel.java',
    'src/client/java/dev/wildercord/client/combat/ArticulatedArmorRenderer.java',
    'src/client/java/dev/wildercord/client/combat/ArticulatedAuraShellRenderer.java',
    'tools/ExportArticulatedSharedArmor.java', 'tools/preview_articulated_shared_armor.py',
    'tools/preview_articulated_combat.py', 'tools/earned_counter_offline.py', 'tools/preview_brace_null_armor.py',
]
CLIPS = [('unmoved', [3.875, 6, 10, 20]), ('null_parry', [2.625, 4, 7.5, 16])]
LABEL = 'OFFLINE SOURCE-DRIVEN PREVIEW, NOT GAME FOOTAGE'
LIMITS = [
    'Software rasterization of production setupAnim ModelPart polygons; no Minecraft client, GPU, mixin runtime, native acceptance or observed gameplay.',
    'Four sampled phases per clip, both hands and widths; not continuous geometry, collision, equipment compatibility or deferred-submission validation.',
    'Third-person partial-weight samples omit live idle, breathing, held-arm and head/free-look baseline.',
    'All four original vanilla equipment-slot bakes are shown; first-person armor uses chestplate arms-only geometry.',
    'Original netherite alpha uses nearest filtering and alpha >127. Resource packs, material passes, trim, enchantment glint and native lighting are omitted.',
    'Sword display uses the official ItemTransform export; opaque sprite boundary edges are reconstructed from the original iron-sword texture.',
    'Hand FOV70; camera placement is the production weighted clamp/clearance/transform. Vertical sheets supply +/-90-degree pitch deltas, clamped to +/-20 before weighting. They are not a world-camera or body-pitch test.',
    'Shell images are cyan X-ray wireframes of actual production deformed exterior polygons over skin/sword. Hidden edges are drawn; no texture, emissive shader, alpha, glow, bloom or material equivalence is claimed.',
    'Source-verified supporting main/client classes provide unrelated types. Every listed pose, mesh, rig, armor and shell implementation is freshly compiled.',
    'The verified Fabric declaration fixture is used for javac signatures only, never on the runtime classpath; runtime Minecraft jar bytes remain original.',
]


def sources():
    return {name: base.sha(ROOT / name) for name in SOURCE_PATHS}


def class_hashes(compiled):
    return {str(path.relative_to(compiled)): base.sha(path)
            for kind in ['main', 'client'] for path in sorted((compiled / kind).rglob('*.class'))}


def export(out, cache, compiled, report):
    receipt_path = compiled / 'verification-receipt.json'
    receipt = json.loads(receipt_path.read_text())
    if (not isinstance(receipt.get('tracked_source_sha256_before'), str)
            or receipt['tracked_source_sha256_before'] != receipt.get('tracked_source_sha256_after')
            or receipt.get('head') != receipt.get('head_after')):
        raise ValueError('Supporting compilation receipt lacks unchanged source/HEAD evidence')
    passed = {row['stage'] for row in receipt.get('stages', []) if row.get('exit_code') == 0}
    if not {'main', 'client'}.issubset(passed):
        raise ValueError('Supporting main/client compilation did not pass')
    if base.sha(cache / 'dependency-receipt.json') != receipt['dependency_receipt_sha256']:
        raise ValueError('Supporting compilation used a different dependency receipt')
    jars, hashes = dependencies(cache)
    if any('compile' in str(path).lower() or 'transform' in str(path).lower() for path in jars):
        raise ValueError('Edited or compile-only jar entered runtime dependency list')
    official = next(path for path in jars if path.name == 'minecraft-26.3-client.jar')
    transform_path = cache / 'transform-receipt.json'
    if base.sha(transform_path) != receipt['transform_receipt_sha256']:
        raise ValueError('Supporting compilation used a different transform receipt')
    fixture = cache / 'minecraft-26.3-fabric-compile.jar'
    transform = json.loads(transform_path.read_text())
    if base.sha(fixture) != transform['output_sha256']:
        raise ValueError('Compile-only Fabric declaration fixture checksum mismatch')
    java = java_bin(cache)
    toolchain_receipt = java.parent.parent / 'temurin25-receipt.json'
    toolchain = json.loads(toolchain_receipt.read_text())
    if java.parent.resolve() != Path(toolchain['java_home']).resolve() or not toolchain['release'].startswith('jdk-25.'):
        raise ValueError('Expected recorded Java 25 toolchain')
    archive = toolchain_receipt.parent / 'downloads' / toolchain['source'].split('/')[-1]
    if base.sha(archive) != toolchain['sha256']:
        raise ValueError('Java 25 official archive differs from verified receipt')
    report.update({'dependencySha256': hashes, 'dependencyReceiptSha256': base.sha(cache / 'dependency-receipt.json'),
                   'supportingCompilation': str(receipt_path), 'supportingCompilationSha256': base.sha(receipt_path),
                   'supportingClassesBefore': class_hashes(compiled), 'compileOnlyFixtureSha256': base.sha(fixture),
                   'toolchain': toolchain, 'toolchainExecutableSha256': {n: base.sha(java / n) for n in ['java', 'javac']}})
    classes = out / 'classes'; classes.mkdir()
    cp = os.pathsep.join(map(str, [*jars, compiled / 'main', compiled / 'client']))
    command = [str(java / 'javac'), '--release', '25', '-proc:none', '-sourcepath', '',
               '-cp', str(fixture) + os.pathsep + cp, '-d', str(classes), *[str(ROOT / s) for s in JAVA_SOURCES]]
    report['commands'] = [command]
    with (out / 'export.log').open('w') as log:
        subprocess.run(command, cwd=ROOT, stdout=log, stderr=log, check=True)
        for main, filename in [('ExportArticulatedGeometry', 'geometry.json'), ('ExportBraceNullPose', 'poses.json'),
                               ('ExportBraceNullArmor', 'armor.json')]:
            command = [str(java / 'java'), '-XX:-UsePerfData', '-Xmx2G', '-cp', str(classes) + os.pathsep + cp, main]
            report['commands'].append(command)
            with (out / filename).open('w') as stream:
                subprocess.run(command, cwd=ROOT, stdout=stream, stderr=log, check=True)
    report['supportingClassesAfter'] = class_hashes(compiled)
    if report['supportingClassesBefore'] != report['supportingClassesAfter']:
        raise ValueError('Supporting classes changed during export')
    asset_cache = out / 'asset-cache'; (asset_cache / 'originals').mkdir(parents=True)
    (asset_cache / 'originals/minecraft-client.jar').symlink_to(official)
    return asset_cache


def shell_points(record, first, placement):
    camera = base.matrix(record['viewPlacements'][placement]['camera']) if first else np.eye(4)
    return [base.transform(np.array(face)[:, :3], camera) for face in record['shell']['first' if first else 'third']]


def wireframe(image, raster, polygons):
    draw = ImageDraw.Draw(image)
    for face in polygons:
        points = raster.project(face)
        if np.any(points[:, 2] <= .05):
            continue
        xy = [tuple(p) for p in points[:, :2]]
        draw.line([*xy, xy[0]], fill=(68, 217, 222), width=1)
    return image


def render(out, cache, report):
    preview = base.Preview(out, cache)
    exported = json.loads((out / 'armor.json').read_text())
    records = {(r['clip'], r['variant'], r['left'], r['age']): r for r in exported['frames']}
    expected = {(clip, variant, left, age) for clip, ages in CLIPS for variant in ['wide', 'slim']
                for left in [False, True] for age in ages}
    if len(exported['frames']) != 32 or set(records) != expected:
        raise ValueError('Expected 32 exact Brace/Null armor records')
    for clip in preview.poses['clips']:
        if (clip['tell'], clip['recovery']) != ((6, 16) if clip['id'] == 'unmoved' else (4, 14)):
            raise ValueError('Pose timing differs from accepted windows')
    report['armorAssets'] = {}; textures = armor_textures(cache, report)
    report['skinAndSwordAssets'] = {k: v for k, v in preview.asset_sources.items() if k != 'master'}
    report['geometryParity'] = preview.geom['verification']
    report['braceNullRigPaletteParity'] = exported['verification']
    report['presentationDomain'] = [{'animation': 24, 'art': 'unmoved', 'windup': 6, 'recovery': 16},
                                    {'animation': 25, 'art': 'null_parry', 'windup': 4, 'recovery': 14}]
    report['smokeChecks'], report['sheets'] = [], []
    for clip, ages in CLIPS:
        for placement in [-1, 0, 1, 2, 3, 4]:
            first = placement >= 0; index = max(placement, 0)
            cells, shell_cells = [], []
            for variant in ['wide', 'slim']:
                for left in [False, True]:
                    for age in ages:
                        record = records[(clip, variant, left, age)]
                        meshes = combined(preview, record, textures, first, index)
                        raster = ArmorRaster(500, 282 if first else 450, first, 70)
                        if not first: raster.factor *= .85
                        image = raster.draw(meshes)
                        camera = record['viewPlacements'][index]
                        subtitle = (f't={age:g} | w={record["weight"]:.3f} | yaw {camera["yaw"]:+.1f} pitch {camera["pitch"]:+.1f}'
                                    if first else f't={age:g} | w={record["weight"]:.3f} | four equipment slots')
                        title = f'{variant.upper()} / {"LEFT" if left else "RIGHT"} / {record["sampleLabel"].upper()}'
                        cells.append(base.caption(image, title, subtitle))
                        shell = shell_points(record, first, index)
                        vertices = np.concatenate([m[0] for m in meshes if m[3] != 'SWORD']
                                                  + [m[0] for m in meshes if m[3] == 'SWORD'][2:])
                        check = {'clip': clip, 'variant': variant, 'left': left, 'age': age, 'firstPerson': first,
                                 'placement': camera['name'] if first else 'third', 'skinPixels': int((raster.ids == 1).sum()),
                                 'swordPixels': int((raster.ids == 2).sum()), 'armorPixels': int((raster.ids == 3).sum()),
                                 'finiteVertices': bool(np.isfinite(vertices).all()), 'shellFinite': bool(np.isfinite(shell).all()),
                                 'shellPolygonCount': len(shell)}
                        if first:
                            check.update({'nearestCameraZ': float(vertices[:, 2].max()),
                                          'nearPlaneClear': bool(vertices[:, 2].max() < -.05),
                                          'shellNearestCameraZ': float(np.concatenate(shell)[:, 2].max()),
                                          'shellNearPlaneClear': bool(np.concatenate(shell)[:, 2].max() < -.05),
                                          'crosshairCovered': bool(raster.ids[raster.h // 2, raster.w // 2])})
                        report['smokeChecks'].append(check)
                        if placement in [-1, 0]:
                            skin = [m for m in meshes if not m[3].startswith('ARMOR_')]
                            shell_raster = ArmorRaster(500, 282 if first else 450, first, 70)
                            if not first: shell_raster.factor *= .85
                            shell_image = wireframe(shell_raster.draw(skin), shell_raster, shell)
                            shell_subtitle = subtitle if first else f't={age:g} | w={record["weight"]:.3f} | skin + shell wireframe'
                            shell_cells.append(base.caption(shell_image, title, shell_subtitle))
            suffix = record['viewPlacements'][index]['name'] if first else 'third'
            filename = f'{clip}_netherite_{suffix}_offline.png'
            title = clip.replace('_', ' ').title() + ' / ' + ('first-person armor / ' + suffix.replace('_', ' ') if first else 'full netherite over skin')
            subtitle = ('Production polygons, stock skin/sword. ' + ('Hand FOV70. ' if first else 'Fixed orthographic inspection camera. ')
                        + 'Material/glint/trim/native lighting omitted.')
            if placement in [3, 4]:
                subtitle = f'Pitch-delta input {record["viewPlacements"][index]["inputPitchDelta"]:+g} degrees, clamped to +/-20 then weighted. FOV70. Not world-camera pitch.'
            base.sheet(cells, 4, title, subtitle, out / filename)
            report['sheets'].append({'path': filename, 'sha256': base.sha(out / filename), 'cells': len(cells), 'kind': 'armor'})
            print(filename, flush=True)
            if shell_cells:
                filename = f'{clip}_shell_wireframe_{suffix}_offline.png'
                base.sheet(shell_cells, 4, clip.replace('_', ' ').title() + ' / geometric shell wireframe / ' + suffix,
                           'Actual deformed shell polygons; X-ray hidden edges. GEOMETRIC ONLY: no shell texture, material, glow, bloom or native lighting.', out / filename)
                report['sheets'].append({'path': filename, 'sha256': base.sha(out / filename), 'cells': len(shell_cells), 'kind': 'shell-wireframe'})
                print(filename, flush=True)
    checks = report['smokeChecks']
    report['paletteAgreement'] = 'All 32 body/view exports matched the separately exported source skin/sword palettes at absolute tolerance 1e-6.'
    report['boundedSmokePasses'] = all(r['finiteVertices'] and r['shellFinite'] and r['armorPixels'] > 0 and r['swordPixels'] > 0
                                     and r.get('nearPlaneClear', True) and r.get('shellNearPlaneClear', True) for r in checks)
    report['canonicalFirstPersonCrosshairClear'] = all(not r['crosshairCovered'] for r in checks if r['placement'] == 'canonical')
    report['anyPlacementCrosshairCoverage'] = [r for r in checks if r.get('crosshairCovered', False)]
    if not report['boundedSmokePasses'] or not report['canonicalFirstPersonCrosshairClear']:
        raise AssertionError('Bounded visual smoke failed; all sheets/report retained for review')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--verification', type=Path, required=True)
    parser.add_argument('--compiled', type=Path, required=True)
    args = parser.parse_args()
    out = args.out.resolve(); out.mkdir(parents=True, exist_ok=True)
    if any(out.iterdir()): raise ValueError('Use a fresh empty output directory')
    before = sources()
    report = {'label': LABEL, 'sourceSha256': before, 'limits': LIMITS, 'passed': False, 'native': 'not run'}
    try:
        cache = export(out, args.verification.resolve(), args.compiled.resolve(), report)
        render(out, cache, report)
        report['passed'] = True
    finally:
        report['sourceAfterSha256'] = sources(); report['sourceUnchanged'] = before == report['sourceAfterSha256']
        report['passed'] &= report['sourceUnchanged']
        report['exportSha256'] = {n: base.sha(out / n) for n in ['geometry.json', 'poses.json', 'armor.json'] if (out / n).exists()}
        (out / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    if not report['passed']: raise AssertionError('Offline bounded evidence failed')
    print(f'Bounded offline armor/shell smoke passes: {out / "report.json"}; native unrun.', flush=True)


if __name__ == '__main__':
    main()
