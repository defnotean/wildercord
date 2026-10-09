#!/usr/bin/env python3
"""Reproduce source-only articulated armor geometry evidence, without launching Minecraft.

Uses already-verified official dependency cache; no downloads, installers, shader/GPU setup,
Mixin runtime, client fixture or native acceptance are performed or implied.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_VERIFY = Path('/workspace/shared/wildercord_compile_verification')
SOURCES = [
    'src/main/java/dev/wildercord/aura/ArticulatedCombatPose.java',
    'src/main/java/dev/wildercord/aura/ArtRules.java',
    'src/main/java/dev/wildercord/aura/MastersArtRules.java',
    'src/main/java/dev/wildercord/aura/MastersStyleRules.java',
    'src/main/java/dev/wildercord/aura/ArticulatedArmorMesh.java',
    'src/client/java/dev/wildercord/client/mixin/ModelPartChildrenAccessor.java',
    'src/client/java/dev/wildercord/client/combat/ArticulatedRig.java',
    'src/client/java/dev/wildercord/client/combat/ArticulatedArmorGeometry.java',
    'src/client/java/dev/wildercord/client/combat/ArticulatedArmorAssets.java',
    'src/gametest/java/dev/wildercord/client/combat/ArticulatedArmorInputChecks.java',
    'tools/CheckArticulatedArmorGeometry.java',
    'tools/CheckArticulatedArmorView.java',
    'tools/CheckArticulatedArmorPalette.java',
    'tools/check_articulated_armor.py',
]
INSPECTED = [
    'src/client/java/dev/wildercord/client/combat/ArticulatedViewModel.java',
    'src/client/java/dev/wildercord/client/combat/ArticulatedArmorRenderer.java',
]


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def sources() -> dict[str, str]:
    return {name: sha(ROOT / name) for name in SOURCES + INSPECTED}


def run(out: Path, verify: Path, shared_player: bool = False, opening_styles: bool = False, hail_sky: bool = False, ground_fields: bool = False) -> None:
    if sum([shared_player, opening_styles, hail_sky, ground_fields]) > 1:
        raise ValueError('Select one bounded geometry domain')
    out.mkdir(parents=True, exist_ok=True)
    if any(out.iterdir()):
        raise ValueError('Use a fresh empty output directory to preserve prior evidence')
    before = sources()
    java = Path(json.loads((verify / 'toolchain_manifest.json').read_text())['java']['directory']) / 'bin'
    dependencies = []
    seen = set()
    hashes = {}
    for manifest in ['dependency_manifest.json', 'embedded_manifest.json']:
        for entry in json.loads((verify / manifest).read_text()):
            path = verify / entry['path']
            if path.suffix != '.jar' or 'iris' in str(path) or path.name in seen:
                continue
            digest = sha(path)
            if digest != entry['sha256']:
                raise ValueError(f'Dependency checksum mismatch: {path}')
            seen.add(path.name)
            dependencies.append(path)
            hashes[str(path)] = digest
    # Only original official runtime jars. Compile-only rewritten copies never enter this audit.
    required = [verify / 'originals/minecraft-client.jar', verify / 'originals/minecraft-server-inner.jar']
    for path in required:
        if path not in dependencies:
            raise ValueError(f'Official runtime jar missing from verified dependency manifest: {path}')
    dependencies = required + [path for path in dependencies if path not in required]
    classes = out / 'classes'
    classes.mkdir()
    cp = os.pathsep.join(map(str, dependencies))
    commands = [[str(java / 'javac'), '--release', '25', '-sourcepath', str(ROOT / 'src/main/java'), '-proc:none', '-cp', cp, '-d', str(classes),
                 *[str(ROOT / name) for name in SOURCES if name.endswith('.java')]]]
    report = {'kind': 'offline source geometry', 'native': 'not run', 'mixinRuntime': 'not run',
              'source_before': before, 'dependency_sha256': hashes, 'passed': False,
              'geometry_domain': ['collapse', 'red_rain'] if ground_fields else ['hailfall', 'skyfall'] if hail_sky else ['kindling_draw', 'frostbite'] if opening_styles else ['rising_break', 'driving_cut'] if shared_player else ['spellcut'],
              'palette_input_domain': 'existing immutable-palette and equipment-input regressions'}
    try:
        with (out / 'compile.log').open('w') as log:
            subprocess.run(commands[0], cwd=ROOT, stdout=log, stderr=log, check=True)
        for main, filename in [('dev.wildercord.client.combat.ArticulatedArmorInputChecks', 'inputs.json'),
                               ('CheckArticulatedArmorGeometry', 'geometry.json'), ('CheckArticulatedArmorView', 'view.json'),
                               ('CheckArticulatedArmorPalette', 'palette.json')]:
            # Concurrent isolated verifiers can share a PID-named perf-data file. Keep the
            # JVM's optional performance counters from injecting a lock warning into JSON.
            command = [str(java / 'java'), '-XX:-UsePerfData', '-Xmx2G', '-cp', str(classes) + os.pathsep + cp, main]
            if (shared_player or opening_styles or hail_sky or ground_fields) and main in ['CheckArticulatedArmorGeometry', 'CheckArticulatedArmorView']:
                command.append('--ground-fields' if ground_fields else '--hail-sky' if hail_sky else '--opening-styles' if opening_styles else '--shared-player')
            commands.append(command)
            with (out / filename).open('w') as result, (out / (main + '.log')).open('w') as log:
                subprocess.run(command, cwd=ROOT, stdout=result, stderr=log, check=True)
            result = json.loads((out / filename).read_text())
            if result.get('passes') is not True:
                raise ValueError(f'{main} did not affirm its scoped geometry checks')
        report['passed'] = True
    finally:
        after = sources()
        report['source_after'] = after
        report['source_unchanged'] = before == after
        report['passed'] &= before == after
        (out / 'commands.json').write_text(json.dumps(commands, indent=2) + '\n')
        (out / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    if before != after:
        raise RuntimeError('Sources changed during the audit; this result is invalid')
    print(f'Offline geometry checks passed. Evidence: {out / "report.json"}. Native rendering remains unrun.')


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--verification', type=Path, default=DEFAULT_VERIFY)
    domain = parser.add_mutually_exclusive_group()
    domain.add_argument('--shared-player', action='store_true', help='Audit Rising Break and Driving Cut geometry instead of the default Spellcut domain')
    domain.add_argument('--opening-styles', action='store_true', help='Audit only Kindling Draw and Frostbite (presentation IDs 3 and 4) in their accepted 6/12 windows')
    domain.add_argument('--hail-sky', action='store_true', help='Audit Hailfall and Skyfall in their accepted 8/16 and 6/16 windows')
    domain.add_argument('--ground-fields', action='store_true', help='Audit Collapse and Red Rain in their accepted 8/18 and 8/16 windows')
    args = parser.parse_args()
    run(args.out.resolve(), args.verification.resolve(), args.shared_player, args.opening_styles, args.hail_sky, args.ground_fields)


if __name__ == '__main__':
    main()
