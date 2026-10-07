#!/usr/bin/env python3
"""Focused Java25 pose/armor/preservation tests. Offline CPU evidence, never a native gate."""
from __future__ import annotations
import argparse
import json
import os
from pathlib import Path
import subprocess
from earned_counter_offline import dependencies, java_bin
from preview_articulated_combat import sha
from freeze_brace_null_baseline import HELPER, SOURCES as LEGACY_SOURCES

ROOT = Path(__file__).resolve().parents[1]
TESTS = ['ArticulatedBraceNullPoseTest', 'ArticulatedBraceNullArmorTest',
         'MastersBraceNullCompositionTest', 'MastersBraceNullPreservationTest',
         'ArticulatedMirrorRipostePoseTest', 'ArticulatedMirrorRiposteArmorTest',
         'MastersMirrorRiposteCompositionTest', 'MastersMirrorRipostePreservationTest',
         'ArticulatedEarnedCounterPoseTest', 'ArticulatedEarnedCounterArmorTest',
         'ArticulatedGroundFieldPoseTest', 'ArticulatedHailfallSkyfallPoseTest',
         'ArticulatedCombatPoseTest', 'ArticulatedEmberKilnPoseTest', 'ArticulatedMasterSchoolPoseTest',
         'ArticulatedOpeningStylePoseTest', 'ArticulatedSharedPlayerPoseTest', 'ArticulatedStoneFaultMarchPoseTest',
         'CrimsonMoonPoseTest', 'MastersArtAnimationTest', 'MastersStyleAnimationTest', 'MastersViewMotionTest']

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--verification', type=Path, default=Path('/workspace/shared/wildercord_standalone'))
    args = parser.parse_args(); out = args.out.resolve(); out.mkdir(parents=True, exist_ok=True)
    if any(out.iterdir()):
        raise ValueError('Use a fresh evidence directory')
    files = [ROOT / 'src/test/java/dev/wildercord/aura' / f'{name}.java' for name in TESTS]
    files.append(ROOT / HELPER)
    files.append(ROOT / 'src/test/java/dev/wildercord/aura/CheckCrimsonMoonPose.java')
    # Hash the pure pose dependency closure, including the shared armor/material/view helpers.
    sources = [*[ROOT / path for path in LEGACY_SOURCES],
               *[ROOT / 'src/main/java/dev/wildercord/aura' / f'{name}.java'
                 for name in ['ArticulatedArmorMesh', 'AuraShellMaterial', 'MastersViewMotion']], *files,
               ROOT / 'src/test/resources/dev/wildercord/aura/brace-null-legacy-poses.json',
               *sorted((ROOT / 'src/test/resources/dev/wildercord/aura/brace-null-legacy-poses.tsv.gz.base64').iterdir()),
               ROOT / 'src/test/resources/dev/wildercord/aura/mirror-riposte-legacy-poses.json',
               *sorted((ROOT / 'src/test/resources/dev/wildercord/aura/mirror-riposte-legacy-poses.tsv.gz.base64').iterdir()),
               Path(__file__), ROOT / 'tools/earned_counter_offline.py', ROOT / 'tools/freeze_brace_null_baseline.py']
    before = {str(path.relative_to(ROOT)): sha(path) for path in sources}
    jars, dependency_hashes = dependencies(args.verification); java = java_bin(args.verification)
    cp = os.pathsep.join(map(str, jars)); classes = out / 'classes'; classes.mkdir()
    commands = [[str(java / 'javac'), '-proc:none', '--release', '25', '-sourcepath', str(ROOT / 'src/main/java'),
                 '-cp', cp, '-d', str(classes), *map(str, files)],
                [str(java / 'java'), '-XX:-UsePerfData', '-Xmx2G', '-cp', os.pathsep.join([str(classes), cp, str(ROOT / 'src/test/resources')]),
                 'org.junit.platform.console.ConsoleLauncher', 'execute', '--disable-banner', '--details=summary',
                 '--reports-dir=' + str(out / 'junit-reports'), *['--select-class=dev.wildercord.aura.' + name for name in TESTS]]]
    report = {'label': 'OFFLINE SOURCE TESTS, NOT NATIVE GAME ACCEPTANCE', 'sourceSha256Before': before,
              'dependencySha256': dependency_hashes, 'commands': commands, 'stages': []}
    code = 0
    for name, command in zip(['compile', 'junit'], commands):
        with (out / f'{name}.log').open('w') as log:
            code = subprocess.run(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT).returncode
        report['stages'].append({'name': name, 'exitCode': code, 'log': str(out / f'{name}.log')})
        if code:
            break
    report['sourceSha256After'] = {str(path.relative_to(ROOT)): sha(path) for path in sources}
    report['sourceUnchanged'] = before == report['sourceSha256After']
    report['passed'] = code == 0 and report['sourceUnchanged']
    (out / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    print(out / 'report.json')
    if not report['passed']:
        raise SystemExit(code or 1)

if __name__ == '__main__':
    main()
