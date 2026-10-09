#!/usr/bin/env python3
"""Bounded armored ACTIVE cutting-edge check, isolated by the sword's raster ID. OFFLINE ONLY."""
import argparse, hashlib, io, json
from pathlib import Path
from zipfile import ZipFile
import numpy as np
from PIL import Image
import preview_articulated_combat as base
from preview_articulated_shared_armor import combined, armor_textures, ArmorRaster
from check_brace_null_blade_readability import measure
from earned_counter_offline import dependencies
ROOT = Path(__file__).resolve().parents[1]

def main():
    parser = argparse.ArgumentParser(description=__doc__); parser.add_argument('--export', dest='export_dir', type=Path, required=True); parser.add_argument('--verification', type=Path, required=True); parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args(); out = args.out.resolve(); out.mkdir(parents=True, exist_ok=True)
    if any(out.iterdir()): raise ValueError('Use a fresh evidence directory')
    export = args.export_dir.resolve(); before = {str(p.relative_to(ROOT)): base.sha(p) for p in [Path(__file__), ROOT / 'tools/check_brace_null_blade_readability.py', ROOT / 'src/main/java/dev/wildercord/aura/ArticulatedCombatPose.java']}
    source = json.loads((export / 'report.json').read_text())
    if not source['passed'] or not source['sourceUnchanged'] or source['sourceSha256']['src/main/java/dev/wildercord/aura/ArticulatedCombatPose.java'] != before['src/main/java/dev/wildercord/aura/ArticulatedCombatPose.java']:
        raise ValueError('Stale armor export')
    jars, hashes = dependencies(args.verification); jar = next(p for p in jars if p.name == 'minecraft-26.3-client.jar')
    with ZipFile(jar) as archive: data = archive.read('assets/minecraft/textures/item/diamond_sword.png'); diamond = np.array(Image.open(io.BytesIO(data)).convert('RGBA'))
    preview = base.Preview(export, export / 'asset-cache'); preview.tex['sword'] = diamond
    report = {'label': 'OFFLINE armored source raster, not native acceptance', 'armorAssets': {}, 'sourceBefore': before, 'dependencySha256': hashes, 'diamondTextureSha256': hashlib.sha256(data).hexdigest(), 'results': []}
    textures = armor_textures(export / 'asset-cache', report); cells = []
    for record in json.loads((export / 'armor.json').read_text())['frames']:
        if record['phase'] != 'ACTIVE': continue
        for placement, view in enumerate(record['viewPlacements']):
            meshes = combined(preview, record, textures, True, placement)
            for width, height, gui in [(854, 480, 2), (1280, 720, 3), (1280, 960, 4), (1920, 810, 3)]:
                raster = ArmorRaster(width, height, True, 70); image = raster.draw(meshes); result = measure(raster, gui)
                row = {'clip': record['clip'], 'skin': record['variant'], 'left': record['left'], 'age': record['age'], 'placement': placement, 'view': view, 'viewport': [width, height, gui], **result}; report['results'].append(row)
                if width == 854 and record['variant'] == 'wide' and placement in [0,4]:
                    name = f'{record["clip"]}_{"left" if record["left"] else "right"}_placement{placement}.png'; image.save(out / name)
                    cells.append(base.caption(image.resize((427,240)), name[:-4], f'Sword-only connected edge{result["largestCuttingEdgeSpanGui"]:g} GUIpx / minimum16'))
                if not result['passes']: print('FAIL', row, flush=True)
    base.sheet(cells, 4, 'Armored ACTIVE sword-only readability', 'Original netherite polygons; skin/cuffs cannot count; unchanged16-GUI threshold. OFFLINE ONLY.', out / 'armored_active_readability.png')
    report['sourceAfter'] = {p: base.sha(ROOT / p) for p in before}; report['sourceUnchanged'] = before == report['sourceAfter']; report['passed'] = report['sourceUnchanged'] and all(r['passes'] for r in report['results'])
    (out / 'report.json').write_text(json.dumps(report, indent=2) + '\n'); print('Armored samples',len(report['results']),'minimum span',min(r['largestCuttingEdgeSpanGui'] for r in report['results']),'passed', report['passed'])
    if not report['passed']: raise SystemExit(1)
if __name__ == '__main__': main()
