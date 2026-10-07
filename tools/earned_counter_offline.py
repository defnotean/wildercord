"""Offline export from either recorded original-runtime cache; never a native game run."""
from __future__ import annotations
import hashlib
import json
import os
from pathlib import Path
import subprocess
import preview_articulated_combat as base

ROOT = Path(__file__).resolve().parents[1]


def java_bin(cache):
    manifest = cache / 'toolchain_manifest.json'
    if manifest.exists():
        return Path(json.loads(manifest.read_text())['java']['directory']) / 'bin'
    configured = os.environ.get('JAVA_HOME')
    if not configured:
        raise ValueError('Set JAVA_HOME to the verified Java 25 directory for the standalone cache')
    return Path(configured) / 'bin'


def dependencies(cache):
    """Reject edited/unrecorded jars; never load the transformed compile-only Minecraft jar."""
    standalone = cache / 'dependency-receipt.json'
    if standalone.exists():
        records = [{'path': row['path'], 'sha256': row['sha256']}
                   for row in json.loads(standalone.read_text()) if str(row['path']).endswith('.jar')]
    else:
        records = [row for name in ['dependency_manifest.json', 'embedded_manifest.json']
                   for row in json.loads((cache / name).read_text()) if str(row['path']).endswith('.jar')]
    jars, hashes, seen = [], {}, set()
    for row in records:
        jar = cache / row['path']
        if 'iris' in str(jar) or jar.name in seen:
            continue
        digest = hashlib.sha256(jar.read_bytes()).hexdigest()
        if digest != row['sha256']:
            raise ValueError(f'Runtime dependency checksum mismatch: {jar}')
        jars.append(jar); hashes[str(jar)] = digest; seen.add(jar.name)
    return jars, hashes


def export(out, cache):
    if not (cache / 'dependency-receipt.json').exists():
        base.export(out, cache)
        return cache
    jars, hashes = dependencies(cache)
    java = java_bin(cache)
    classes = out / 'classes'; classes.mkdir(exist_ok=True)
    sources = ['src/main/java/dev/wildercord/aura/ArticulatedCombatPose.java',
               'src/client/java/dev/wildercord/client/mixin/ModelPartChildrenAccessor.java',
               'src/client/java/dev/wildercord/client/combat/ArticulatedRig.java',
               'tools/ExportArticulatedGeometry.java', 'tools/ExportArticulatedPose.java']
    cp = os.pathsep.join(map(str, jars))
    commands = [[str(java / 'javac'), '-proc:none', '--release', '25', '-sourcepath', str(ROOT / 'src/main/java'),
                 '-cp', cp, '-d', str(classes), *[str(ROOT / path) for path in sources]]]
    with (out / 'export.log').open('w') as log:
        subprocess.run(commands[0], stdout=log, stderr=log, check=True)
        for main, filename in [('ExportArticulatedGeometry', 'geometry.json'), ('ExportArticulatedPose', 'poses.json')]:
            command = [str(java / 'java'), '-XX:-UsePerfData', '-cp', str(classes) + os.pathsep + cp, main]
            commands.append(command)
            with (out / filename).open('w') as stream:
                subprocess.run(command, stdout=stream, stderr=log, check=True)
    # Preview reads only this official jar's textures. The alias changes no jar bytes.
    asset_cache = out / 'asset-cache'
    (asset_cache / 'originals').mkdir(parents=True)
    official = next(jar for jar in jars if jar.name == 'minecraft-26.3-client.jar')
    (asset_cache / 'originals/minecraft-client.jar').symlink_to(official)
    (out / 'dependency-sha256.json').write_text(json.dumps(hashes, indent=2) + '\n')
    (out / 'commands.json').write_text(json.dumps(commands, indent=2) + '\n')
    return asset_cache
