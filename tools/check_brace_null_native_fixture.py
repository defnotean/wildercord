#!/usr/bin/env python3
"""Compile the additive pair-only native fixture and run existing pure pixel-policy checks.

This is never a native client run. The transformed Minecraft jar is compile-only;
execution uses pristine verified runtime jars and the recorded supporting classes.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
from earned_counter_offline import dependencies, java_bin
from freeze_brace_null_baseline import SOURCES as PURE_SOURCES

ROOT = Path(__file__).resolve().parents[1]
JAVA = [f'src/gametest/java/dev/wildercord/gametest/{name}.java' for name in [
    'NativeBodySubmission', 'NativeBodySubmissionChecks', 'BraceNullPhaseContract', 'BraceNullTransformOracle', 'BraceNullPlayerWidth', 'BraceNullItemDrawProbe', 'BraceNullCaptureProbe', 'MastersCaptureProbe', 'WildercordMastersArtsPresentationTest']]
JAVA += [f'src/gametest/java/dev/wildercord/gametest/mixin/{name}.java' for name in [
    'BraceNullPlayerWidthMixin', 'BraceNullWorldItemMixin', 'BraceNullItemSubmitMixin', 'BraceNullItemDrawMixin', 'BraceNullBodyBaselineMixin', 'BraceNullHandEntryMixin', 'BraceNullPlayerPaletteMixin', 'BraceNullDeferredPaletteMixin', 'BraceNullNativeSubmitMixin', 'MastersBodyRenderProbeMixin', 'MastersHandRenderProbeMixin']]
JAVA += ['tools/CheckMastersCaptureProbe.java', 'tools/CheckBraceNullPhaseContract.java', 'tools/CheckBraceNullWorldItemOracle.java', *PURE_SOURCES]
SOURCES = JAVA + ['src/gametest/resources/masters-capture-gametest.mixins.json',
    'src/client/java/dev/wildercord/client/fx/HitStop.java',
    'src/client/java/dev/wildercord/client/MastersArtPose.java',
    'src/main/java/dev/wildercord/aura/MastersArtAnimation.java',
    'src/main/java/dev/wildercord/aura/MastersStyleAnimation.java',
    'tools/check_brace_null_native_fixture.py', 'tools/earned_counter_offline.py', 'tools/freeze_brace_null_baseline.py']


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--verification', type=Path, required=True)
    parser.add_argument('--compiled', type=Path, required=True)
    args = parser.parse_args()
    out = args.out.resolve(); out.mkdir(parents=True, exist_ok=True)
    if any(out.iterdir()):
        raise ValueError('Use a fresh evidence directory')
    cache, supporting = args.verification.resolve(), args.compiled.resolve()
    receipt = supporting / 'verification-receipt.json'
    verified = json.loads(receipt.read_text())
    if verified.get('tracked_source_sha256_before') != verified.get('tracked_source_sha256_after') or not verified.get('tracked_source_sha256_before'):
        raise ValueError('Supporting source was not verified stable')
    if not {'main', 'client', 'gametest'} <= {r['stage'] for r in verified['stages'] if r['exit_code'] == 0}:
        raise ValueError('Supporting compile stages did not pass')
    jars, hashes = dependencies(cache); java = java_bin(cache)
    fixture = cache / 'minecraft-26.3-fabric-compile.jar'
    transformed = json.loads((cache / 'transform-receipt.json').read_text())
    if sha(fixture) != transformed['output_sha256']:
        raise ValueError('Compile-only fixture checksum changed')
    before = {p: sha(ROOT / p) for p in SOURCES}
    classes = out / 'classes'; classes.mkdir()
    runtime = os.pathsep.join(map(str, [*jars, *[supporting / name for name in ['main', 'client', 'gametest']]]))
    commands = [
        [str(java / 'javac'), '--release', '25', '-proc:none', '-sourcepath', '', '-cp', str(fixture) + os.pathsep + runtime,
         '-d', str(classes), *[str(ROOT / p) for p in JAVA]],
        [str(java / 'java'), '-XX:-UsePerfData', '-cp', str(classes) + os.pathsep + runtime, 'dev.wildercord.gametest.NativeBodySubmissionChecks'],
        [str(java / 'java'), '-XX:-UsePerfData', '-cp', str(classes) + os.pathsep + runtime, 'CheckMastersCaptureProbe'],
        [str(java / 'java'), '-XX:-UsePerfData', '-cp', str(classes) + os.pathsep + runtime, 'dev.wildercord.aura.CheckBraceNullPhaseContract'],
        [str(java / 'java'), '-XX:-UsePerfData', '-cp', str(classes) + os.pathsep + runtime, 'CheckBraceNullWorldItemOracle'],
        [str(java / 'javap'), '-classpath', str(next(p for p in jars if p.name == 'minecraft-26.3-client.jar')),
         '-c', 'net.minecraft.client.renderer.entity.EntityRenderer']]
    report = {'native': 'not run', 'passed': False, 'sourceBefore': before, 'dependencySha256': hashes,
        'compileOnlyFixtureSha256': sha(fixture), 'supportingReceiptSha256': sha(receipt),
        'commands': commands, 'stages': [], 'limits': ['No GPU, client launch, mixin application, actual screenshot or observer acceptance.']}
    try:
        for name, command in zip(['compile', 'native-submit-provenance', 'pixel-policy', 'phase-contract', 'world-item-oracle', 'official-age-clock'], commands):
            with (out / (name + '.log')).open('w') as log:
                code = subprocess.run(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT).returncode
            report['stages'].append({'stage': name, 'exitCode': code})
            if code:
                raise AssertionError(f'{name} failed')
        bytecode = (out / 'official-age-clock.log').read_text()
        start = bytecode.index('Field net/minecraft/world/entity/Entity.tickCount:I')
        fragment = bytecode[start:start + 340]
        if not all(token in fragment for token in ['i2f', 'fload_3', 'fadd', 'EntityRenderState.ageInTicks:F']):
            raise AssertionError('Original EntityRenderer ageInTicks provenance changed')
        report['officialAgeClock'] = fragment
        report['passed'] = True
    finally:
        report['sourceAfter'] = {p: sha(ROOT / p) for p in SOURCES}
        report['sourceUnchanged'] = report['sourceBefore'] == report['sourceAfter']
        report['passed'] &= report['sourceUnchanged']
        (out / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    if not report['passed']:
        raise AssertionError('Source changed during fixture check')
    print(out / 'report.json')


if __name__ == '__main__':
    main()
