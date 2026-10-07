#!/usr/bin/env python3
"""Original-runtime armor and funded-shell CPU checks for Backdraft/Rooted Parry.

Requires the recorded standalone dependency cache and source-verified main/client classes.
Freshly compiles all pose/mesh/rig/shell implementation and audit sources. Never a native gate.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
from earned_counter_offline import dependencies, java_bin

ROOT = Path(__file__).resolve().parents[1]
SOURCES = [
    'src/main/java/dev/wildercord/aura/ArticulatedCombatPose.java',
    'src/main/java/dev/wildercord/aura/ArticulatedArmorMesh.java',
    'src/main/java/dev/wildercord/aura/MastersStyleRules.java',
    'src/client/java/dev/wildercord/client/mixin/ModelPartChildrenAccessor.java',
    'src/client/java/dev/wildercord/client/combat/ArticulatedRig.java',
    'src/client/java/dev/wildercord/client/combat/ArticulatedArmorGeometry.java',
    'src/client/java/dev/wildercord/client/combat/ArticulatedAuraShellGeometry.java',
    'src/client/java/dev/wildercord/client/render/AuraShellLayer.java',
    'tools/CheckArticulatedArmorGeometry.java', 'tools/CheckArticulatedArmorView.java',
    'tools/CheckArticulatedAuraShell.java', 'tools/CheckArticulatedAuraShellClearance.java',
    'tools/check_earned_counter_geometry.py', 'tools/earned_counter_offline.py',
]


def hashes():
    return {name: hashlib.sha256((ROOT / name).read_bytes()).hexdigest() for name in SOURCES}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--verification', type=Path, default=Path('/workspace/shared/wildercord_standalone'))
    parser.add_argument('--compiled', type=Path, required=True, help='Source-verified supporting main/client classes, never transformed runtime jars')
    args = parser.parse_args()
    out = args.out.resolve(); out.mkdir(parents=True, exist_ok=True)
    if any(out.iterdir()):
        raise ValueError('Use a fresh output directory to preserve diagnostics')
    before = hashes()
    jars, dependency_hashes = dependencies(args.verification)
    java = java_bin(args.verification)
    classes = out / 'classes'; classes.mkdir()
    cp = os.pathsep.join(map(str, [*jars, args.compiled / 'main', args.compiled / 'client']))
    transform_receipt = json.loads((args.verification / 'transform-receipt.json').read_text())
    compile_jar = args.verification / 'minecraft-26.3-fabric-compile.jar'
    if hashlib.sha256(compile_jar.read_bytes()).hexdigest() != transform_receipt['output_sha256']:
        raise ValueError('Compile-only Fabric fixture checksum mismatch')
    compile_cp = str(compile_jar) + os.pathsep + cp
    commands = [[str(java / 'javac'), '-proc:none', '--release', '25', '-sourcepath', '',
                 '-cp', compile_cp, '-d', str(classes), *[str(ROOT / source) for source in SOURCES if source.endswith('.java')]]]
    receipt = args.compiled / 'verification-receipt.json'
    if not receipt.exists():
        raise ValueError('Supporting compilation needs its verification-receipt.json')
    source_receipt = json.loads(receipt.read_text())
    if not isinstance(source_receipt.get('tracked_source_sha256_before'), str) or source_receipt.get('tracked_source_sha256_before') != source_receipt.get('tracked_source_sha256_after'):
        raise ValueError('Supporting compilation does not record stable source hashes')
    passed_stages = {row['stage'] for row in source_receipt.get('stages', []) if row.get('exit_code') == 0}
    if not {'main', 'client'}.issubset(passed_stages):
        raise ValueError('Supporting main/client compilation must have passed')
    report = {'kind': 'offline original-runtime geometry', 'native': 'not run', 'passed': False,
              'clips': ['backdraft', 'rooted_parry'], 'windows': [[4, 14], [6, 16]], 'sourceBefore': before,
              'dependencySha256': dependency_hashes, 'supportingCompilation': str(receipt),
              'supportingCompilationSha256': hashlib.sha256(receipt.read_bytes()).hexdigest(),
              'compileOnlyFixtureSha256': transform_receipt['output_sha256'],
              'limits': ['No Minecraft launch, GPU/material/trim/glint rendering, deferred submission or real observer evidence.',
                         'Supporting types use the supplied compile output; all listed presentation implementations are freshly compiled.']}
    try:
        with (out / 'compile.log').open('w') as log:
            subprocess.run(commands[0], cwd=ROOT, stdout=log, stderr=log, check=True)
        for audit, filename in [('CheckArticulatedArmorGeometry', 'armor.json'), ('CheckArticulatedArmorView', 'armor-view.json'),
                                ('CheckArticulatedAuraShell', 'shell.json'), ('CheckArticulatedAuraShellClearance', 'shell-clearance.json')]:
            command = [str(java / 'java'), '-XX:-UsePerfData', '-Xmx2G', '-cp', str(classes) + os.pathsep + cp, audit, '--earned-counters']
            commands.append(command)
            with (out / filename).open('w') as stream, (out / (audit + '.log')).open('w') as log:
                subprocess.run(command, cwd=ROOT, stdout=stream, stderr=log, check=True)
            result = json.loads((out / filename).read_text())
            if result.get('passes') is not True:
                raise AssertionError(f'{audit} did not pass its bounded domain')
        report['passed'] = True
    finally:
        report['sourceAfter'] = hashes(); report['sourceUnchanged'] = before == report['sourceAfter']
        report['passed'] &= report['sourceUnchanged']
        (out / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
        (out / 'commands.json').write_text(json.dumps(commands, indent=2) + '\n')
    if not report['passed']:
        raise AssertionError('Source geometry audit failed')
    print(f'Offline armor and funded-shell checks passed: {out / "report.json"}; native rendering not run.')


if __name__ == '__main__':
    main()
