#!/usr/bin/env python3
"""Bounded, isolated mutation evidence for the Brace/Null immutable-reference gate.

Every case gets a fresh copy of the current pure production closure, unchanged tests,
unchanged frozen resources, and a separate class directory. Only the named mutation
is applied to that case's copied production file. Working-tree sources and cached
project classes are never compiled or loaded by a case. This is offline CPU evidence.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET

from earned_counter_offline import dependencies, java_bin
from freeze_brace_null_baseline import HELPER, SOURCES

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = 'dev.wildercord.aura.'
PRESERVATION = 'MastersBraceNullPreservationTest'
INVARIANTS = 'ArticulatedBraceNullPoseTest'
SOCKET_TEST = 'everyFrameKeepsLevelSolesRigidBonesImmutableMatricesAndContinuousHandSockets'
STYLE = 'src/main/java/dev/wildercord/aura/MastersStyleAnimation.java'
ARTICULATED = 'src/main/java/dev/wildercord/aura/ArticulatedCombatPose.java'
TESTS = [f'src/test/java/dev/wildercord/aura/{name}.java' for name in [PRESERVATION, INVARIANTS]]
RESOURCES = ['src/test/resources/dev/wildercord/aura/brace-null-legacy-poses.json',
             *[str(path.relative_to(ROOT)) for path in sorted(
                 (ROOT / 'src/test/resources/dev/wildercord/aura/brace-null-legacy-poses.tsv.gz.base64').iterdir())]]


def sha(data):
    return hashlib.sha256(data).hexdigest()


def mutations():
    # Long source anchors deliberately fail closed when the audited source shape changes.
    # The first four cases each change one scalar in exactly one ID22 pose key.
    return [
        {'name': 'classic-body-key', 'path': STYLE,
         'old': 'private static final Motion GLACIER_MIRROR = new Motion(\n\t\tpose(j(.03F, .22F, -.10F)',
         'new': 'private static final Motion GLACIER_MIRROR = new Motion(\n\t\tpose(j(.031F, .22F, -.10F)',
         'failure': 'Classic body changed for clip 22'},
        {'name': 'classic-hand-key', 'path': STYLE,
         'old': 'h(.10F, .24F, -.22F, -28, 26, -32)',
         'new': 'h(.101F, .24F, -.22F, -28, 26, -32)',
         'failure': 'Classic first person changed for clip 22'},
        {'name': 'articulated-world-key', 'path': ARTICULATED,
         'old': 'private static final Motion GLACIER_MIRROR_MOTION = new Motion(\n\t\tnew Key(v(-.10F, .78F, .08F)',
         'new': 'private static final Motion GLACIER_MIRROR_MOTION = new Motion(\n\t\tnew Key(v(-.101F, .78F, .08F)',
         'failure': 'articulated world changed for clip 22'},
        {'name': 'articulated-view-key', 'path': ARTICULATED,
         'old': 'new ViewKey(v(-8.1F, -4.95F, 3.4F)',
         'new': 'new ViewKey(v(-8.101F, -4.95F, 3.4F)',
         'failure': 'articulated first person changed for clip 22'},
        {'name': 'old-unsupported-admission', 'path': ARTICULATED,
         'old': 'public static boolean supportsPlayer(int move) {\n\t\treturn move == SPELLCUT',
         'new': 'public static boolean supportsPlayer(int move) {\n\t\treturn move == 5 || move == SPELLCUT',
         'failure': 'articulated world changed for clip 5'},
        {'name': 'new-unmoved-socket-offset', 'path': ARTICULATED,
         'old': '\t\treturn sample(motion, age, windup, 1, windup + Math.min(4, recovery * .25F), windup + recovery, leftHanded);',
         'new': '\t\tPose result = sample(motion, age, windup, 1, windup + Math.min(4, recovery * .25F), windup + recovery, leftHanded);\n'
                '\t\tif (move == UNMOVED) {\n'
                '\t\t\tTransform[] changed = result.local.clone();\n'
                '\t\t\tJoint socket = leftHanded ? Joint.LEFT_SOCKET : Joint.RIGHT_SOCKET;\n'
                '\t\t\tTransform original = changed[socket.ordinal()];\n'
                '\t\t\tchanged[socket.ordinal()] = new Transform(original.x(), original.y() + .25F, original.z(), original.rotation());\n'
                '\t\t\treturn new Pose(result.weight, result.phase, changed, result.camera);\n'
                '\t\t}\n\t\treturn result;',
         'failureMethod': SOCKET_TEST},
    ]


def junit_results(directory):
    cases = []
    for path in sorted(directory.glob('TEST-*.xml')):
        for case in ET.parse(path).getroot().iter('testcase'):
            failures = list(case.findall('failure')) + list(case.findall('error'))
            cases.append({'name': case.attrib.get('name'), 'class': case.attrib.get('classname'),
                          'skipped': case.find('skipped') is not None,
                          'failures': [{'type': item.attrib.get('type'), 'message': item.attrib.get('message', ''),
                                        'detail': item.text or ''} for item in failures]})
    if not cases:
        raise AssertionError('JUnit did not emit any testcase evidence')
    return cases


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--verification', type=Path, default=Path('/workspace/shared/wildercord_standalone'))
    args = parser.parse_args()
    out = args.out.resolve()
    if out == ROOT or out.is_relative_to(ROOT):
        raise ValueError('Mutation evidence must be written outside the working tree')
    out.mkdir(parents=True, exist_ok=True)
    if any(out.iterdir()):
        raise ValueError('Use a fresh output directory to preserve every case and log')
    files = sorted(set([*SOURCES, *TESTS, HELPER, *RESOURCES]))
    tracked = files + ['tools/check_brace_null_mutations.py', 'tools/freeze_brace_null_baseline.py',
                       'tools/earned_counter_offline.py']
    snapshot = {name: (ROOT / name).read_bytes() for name in tracked}
    before = {name: sha(data) for name, data in snapshot.items()}
    jars, dependency_hashes = dependencies(args.verification)
    junit = next(path for path in jars if path.name.startswith('junit-platform-console-standalone-'))
    java = java_bin(args.verification)
    report = {'kind': 'isolated offline mutation evidence', 'native': 'not run', 'passed': False,
              'sourceBefore': before, 'dependencySha256': dependency_hashes,
              'classpathPolicy': 'Fresh case classes, checksum-verified original JUnit, copied frozen resources only.',
              'cases': []}
    try:
        for mutation in [None, *mutations()]:
            if any((ROOT / name).read_bytes() != data for name, data in snapshot.items()):
                raise AssertionError('Working-tree source changed after the mutation snapshot')
            name = mutation['name'] if mutation else 'control'
            work = out / name
            case_sources = work / 'source'
            classes = work / 'classes'
            classes.mkdir(parents=True)
            for relative in files:
                target = case_sources / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(snapshot[relative])
            record = {'name': name, 'mutation': mutation, 'commands': [], 'stages': [], 'passed': False}
            report['cases'].append(record)
            if mutation:
                target = case_sources / mutation['path']
                original = target.read_text()
                occurrences = original.count(mutation['old'])
                record['replacementOccurrences'] = occurrences
                if occurrences != 1:
                    raise AssertionError(f'{name}: expected exactly one mutation anchor, found {occurrences}')
                target.write_text(original.replace(mutation['old'], mutation['new']))
            record['copiedSourceSha256'] = {path: sha((case_sources / path).read_bytes()) for path in files}
            changed = [path for path in files if record['copiedSourceSha256'][path] != before[path]]
            if changed != ([mutation['path']] if mutation else []):
                raise AssertionError(f'{name}: unexpected changes outside the one intended production source: {changed}')
            record['changedProductionSources'] = changed
            compile_command = [str(java / 'javac'), '-proc:none', '--release', '25', '-encoding', 'UTF-8',
                               '-sourcepath', str(case_sources / 'src/main/java'), '-cp', str(junit),
                               '-d', str(classes), *[str(case_sources / path) for path in [*SOURCES, *TESTS, HELPER]]]
            selections = ['--select-class=' + PACKAGE + PRESERVATION]
            if mutation is None:
                selections.append('--select-class=' + PACKAGE + INVARIANTS)
            elif mutation.get('failureMethod'):
                selections.append('--select-method=' + PACKAGE + INVARIANTS + '#' + SOCKET_TEST)
            test_command = [str(java / 'java'), '-XX:-UsePerfData', '-Xmx512m', '-cp',
                            os.pathsep.join([str(classes), str(junit), str(case_sources / 'src/test/resources')]),
                            'org.junit.platform.console.ConsoleLauncher', 'execute', '--disable-banner',
                            '--disable-ansi-colors', '--details=tree', '--fail-if-no-tests',
                            '--reports-dir=' + str(work / 'junit-reports'), *selections]
            for stage, command in [('compile', compile_command), ('junit', test_command)]:
                record['commands'].append(command)
                log = work / f'{stage}.log'
                with log.open('w') as stream:
                    code = subprocess.run(command, cwd=work, stdout=stream, stderr=subprocess.STDOUT).returncode
                record['stages'].append({'stage': stage, 'exitCode': code, 'log': str(log)})
                if stage == 'compile' and code:
                    raise AssertionError(f'{name}: compiler failure is not mutation detection')
            record['testcases'] = junit_results(work / 'junit-reports')
            failed = [case for case in record['testcases'] if case['failures']]
            if any(case['skipped'] for case in record['testcases']):
                raise AssertionError(f'{name}: selected tests must not be skipped')
            expected_count = 33 if mutation is None else 26 if mutation.get('failureMethod') else 25
            if len(record['testcases']) != expected_count:
                raise AssertionError(f'{name}: expected {expected_count} tests, found {len(record["testcases"])}')
            if mutation is None:
                if code != 0 or failed:
                    raise AssertionError('Unmodified control did not pass')
            else:
                if code != 1 or len(failed) != 1:
                    raise AssertionError(f'{name}: expected exactly one test failure, got exit {code} and {len(failed)} failures')
                failure = failed[0]
                if mutation.get('failureMethod'):
                    if failure['class'] != PACKAGE + INVARIANTS or mutation['failureMethod'] not in failure['name']:
                        raise AssertionError(f'{name}: wrong invariant failed')
                elif not any(mutation['failure'] in item['message'] for item in failure['failures']):
                    raise AssertionError(f'{name}: the expected frozen palette mismatch was not observed')
                if not all(item['type'] == 'org.opentest4j.AssertionFailedError' for item in failure['failures']):
                    raise AssertionError(f'{name}: a runtime error is not accepted as mutation detection')
            record['passed'] = True
            (work / 'report.json').write_text(json.dumps(record, indent=2) + '\n')
            print(f'{name}: expected {"passing control" if mutation is None else "assertion failure"} verified', flush=True)
        report['passed'] = True
    finally:
        report['sourceAfter'] = {name: sha((ROOT / name).read_bytes()) for name in tracked}
        report['sourceUnchanged'] = before == report['sourceAfter']
        report['passed'] &= report['sourceUnchanged']
        (out / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    if not report['passed']:
        raise AssertionError('Mutation evidence or source stability failed')
    print(out / 'report.json')


if __name__ == '__main__':
    main()
