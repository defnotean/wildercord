#!/usr/bin/env python3
"""Classic first-person source projection diagrams. OFFLINE, never native game frames."""
from __future__ import annotations
import argparse
import io
import json
import os
from pathlib import Path
import subprocess
from zipfile import ZipFile
import numpy as np
from PIL import Image, ImageDraw
import check_masters_first_person as classic
import preview_articulated_combat as base
from earned_counter_offline import dependencies, java_bin

ROOT = Path(__file__).resolve().parents[1]
SOURCES = ['src/main/java/dev/wildercord/aura/MastersArtAnimation.java',
           'src/main/java/dev/wildercord/aura/MastersStyleAnimation.java',
           'src/main/java/dev/wildercord/aura/MastersStyleRules.java',
           'src/main/java/dev/wildercord/aura/ArtRules.java',
           'tools/ExportMirrorRiposteClassic.java', 'tools/check_masters_first_person.py',
           'tools/preview_articulated_combat.py', 'tools/preview_classic_mirror_riposte.py']

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--verification', type=Path, default=Path('/workspace/shared/wildercord_standalone'))
    args = parser.parse_args()
    out = args.out.resolve(); out.mkdir(parents=True, exist_ok=True)
    if any(out.iterdir()):
        raise ValueError('Use a fresh evidence directory')
    before = {name: base.sha(ROOT / name) for name in SOURCES}
    jars, hashes = dependencies(args.verification); java = java_bin(args.verification)
    cp = os.pathsep.join(map(str, jars)); classes = out / 'classes'; classes.mkdir()
    commands = [[str(java / 'javac'), '--release', '25', '-proc:none', '-sourcepath', str(ROOT / 'src/main/java'),
                 '-cp', cp, '-d', str(classes), str(ROOT / 'tools/ExportMirrorRiposteClassic.java')],
                [str(java / 'java'), '-XX:-UsePerfData', '-cp', str(classes) + os.pathsep + cp, 'ExportMirrorRiposteClassic']]
    subprocess.run(commands[0], check=True)
    with (out / 'poses.json').open('w') as stream:
        subprocess.run(commands[1], stdout=stream, check=True)
    samples = classic.read_samples(out / 'poses.json')
    official = next(path for path in jars if path.name == 'minecraft-26.3-client.jar')
    with ZipFile(official) as jar:
        texture = np.array(Image.open(io.BytesIO(jar.read('assets/minecraft/textures/item/diamond_sword.png'))).convert('RGBA'))
    meshes = classic.mesh(texture); records = []
    for move in [22, 23]:
        info = samples[move, 0]; cells = []
        windup, recovery = info['windup'], info['recovery']
        for left in [False, True]:
            for age in [round(windup * .65 * 4) / 4, windup, windup + min(4, recovery / 4), windup + recovery - 2]:
                row = samples[move, age]
                context = next(view for view in row['views'] if view['left'] == left and view['pitchDelta'] == 0)
                matrix = classic.projection() @ classic.hand_matrix(context) @ classic.item_matrix(left)
                opaque = classic.mask(matrix, meshes[2], hud_top=720)
                cyan = classic.mask(matrix, meshes[0], hud_top=720)
                panel = Image.new('RGB', (1280, 720), base.BG); draw = ImageDraw.Draw(panel)
                draw.rectangle((0, 594, 1279, 719), fill=(36, 41, 50))
                draw.text((12, 606), 'OFFLINE HUD RESERVE: 42 x GUI SCALE 3', fill=base.FG)
                draw.line((630, 360, 650, 360), fill=base.FG); draw.line((640, 350, 640, 370), fill=base.FG)
                panel.paste('#b7c2cc', mask=Image.fromarray(opaque)); panel.paste('#2fbcb1', mask=Image.fromarray(cyan))
                cells.append(base.caption(panel.resize((500, 281)), f'{"LEFT" if left else "RIGHT"} / t={age:g}',
                                          'Classic source silhouette / FOV 70 / OFFLINE'))
                records.append({'move': move, 'left': left, 'age': age, **classic.measure(row, context, meshes)})
        base.sheet(cells, 4, f'{info["name"].replace("_", " ").title()} / Classic first person',
                   'Source-derived opaque sword projection, no player arm, texture shading, armor, shell or native rendering.',
                   out / f'{info["name"]}_classic_first_offline.png')
    after = {name: base.sha(ROOT / name) for name in SOURCES}
    report = {'label': 'OFFLINE SOURCE GEOMETRY, NOT GAME FOOTAGE', 'sourceSha256': before, 'sourceUnchanged': before == after,
              'dependencySha256': hashes, 'commands': commands, 'samples': records,
              'limits': ['Flat opaque silhouette projection only; full-clock/free-look composition is covered by the JUnit geometry test.',
                         'No GPU, native HUD, player-arm composition, shell/material/lighting or in-game acceptance claim.']}
    (out / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    assert before == after
    print(out)

if __name__ == '__main__':
    main()
