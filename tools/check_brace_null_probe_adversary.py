#!/usr/bin/env python3
"""Actual receipt probe with minimal Minecraft caller shapes; CPU contract evidence only."""
import argparse, hashlib, json, os, subprocess
from pathlib import Path
from earned_counter_offline import dependencies, java_bin
from freeze_brace_null_baseline import SOURCES
ROOT = Path(__file__).resolve().parents[1]

def main():
    parser = argparse.ArgumentParser(description=__doc__); parser.add_argument('--out', type=Path, required=True); parser.add_argument('--verification', type=Path, required=True)
    args = parser.parse_args(); out = args.out.resolve(); out.mkdir(parents=True, exist_ok=True)
    if any(out.iterdir()): raise ValueError('Use a fresh evidence directory')
    classes = out / 'classes'; classes.mkdir(); java = java_bin(args.verification); jars, hashes = dependencies(args.verification)
    files = [ROOT / p for p in SOURCES] + [ROOT / 'src/main/java/dev/wildercord/aura/MastersViewMotion.java', ROOT / 'src/client/java/dev/wildercord/client/MastersHandMotionState.java']
    files += [ROOT / f'src/gametest/java/dev/wildercord/gametest/{n}.java' for n in ['BraceNullCaptureProbe', 'BraceNullPhaseContract', 'BraceNullTransformOracle', 'BraceNullPlayerWidth', 'BraceNullItemDrawProbe']]
    files += sorted((ROOT / 'tools/tests/brace_null_probe').rglob('*.java'))
    sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
    before = {str(p.relative_to(ROOT)): sha(p) for p in files}; cp = os.pathsep.join(map(str, jars))
    commands = [[str(java / 'javac'), '--release', '25', '-proc:none', '-sourcepath', '', '-cp', cp, '-d', str(classes), *map(str, files)],
                [str(java / 'java'), '-XX:-UsePerfData', '-cp', str(classes) + os.pathsep + cp, 'ProbeAdversary']]
    report = {'label': 'Actual source probe; minimal caller shapes; not native acceptance', 'sourceBefore': before, 'dependencies': hashes, 'commands': commands, 'passed': False, 'stages': []}
    try:
        for name, cmd in zip(['compile','run'], commands):
            with (out / f'{name}.log').open('w') as log: code = subprocess.run(cmd, stdout=log, stderr=subprocess.STDOUT).returncode
            report['stages'].append({'name': name, 'exitCode': code})
            if code: raise AssertionError(name + ' failed')
        report['passed'] = True
    finally:
        report['sourceAfter'] = {str(p.relative_to(ROOT)): sha(p) for p in files}; report['sourceUnchanged'] = before == report['sourceAfter']; report['passed'] &= report['sourceUnchanged']
        (out / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    print(out / 'report.json')
if __name__ == '__main__': main()
