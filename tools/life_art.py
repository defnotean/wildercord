"""Life material particles: leaf, seed, petal, spore, vine, sap, tissue and thorn.

Tinted in game by the colour each spell sends (LifeParticle), so drawn in greys; see particle_pixels.
Silhouettes are drawn with '#' and lit by particle_pixels.shade; veins and seams are drawn in tone.
Each style has two frames: the second is the same piece later in its life (turned, sprouted, curled).

Writes textures/particle/life_<style>_<frame>.png and particles/life.json.
"""
from pathlib import Path
import json

from particle_pixels import compose, edit, render, shade

# Spores are soft and pale: no dark outline, so they read as drifting motes rather than grit.
SOFT = {"edge_lit": "m", "edge": "s", "hi": "h", "hi2": "h", "body": "l", "lo": "m"}

# A pointed leaf on its stalk, a darker midrib running from stalk to tip.
LEAF = shade([
    "................",
    "...........####.",
    ".........######.",
    ".......########.",
    "......#######s#.",
    ".....######s###.",
    "....######s####.",
    "....#####s#####.",
    "...#####s#####..",
    "...####s#####...",
    "...###s#####....",
    "...##s#####.....",
    "...#s####.......",
    "..#s##..........",
    ".##.............",
    "#...............",
])
# Later: turned edge-on as it flutters, showing a narrower face and its side veins.
LEAF_TURNED = shade([
    "................",
    "............###.",
    "...........####.",
    ".........#####..",
    "........####s#..",
    ".......####s##..",
    "......###ss##...",
    ".....###s###....",
    ".....##s###.....",
    "....##s###......",
    "....#s###.......",
    "...#s###........",
    "...s###.........",
    "..##............",
    ".##.............",
    "#...............",
])

# An almond-shaped seed with a seam down it.
SEED = shade([
    "................",
    "................",
    "........##......",
    ".......####.....",
    "......######....",
    "......##s###....",
    ".....###s####...",
    ".....###s####...",
    ".....###s####...",
    ".....####s###...",
    ".....####s###...",
    "......###s##....",
    "......######....",
    ".......####.....",
    "................",
    "................",
])
# Later: a shoot has broken from its tip and opened two seed leaves.
SEED_SPROUTED = shade([
    "......##..##....",
    ".......####.....",
    "........#.......",
    "........##......",
    ".......####.....",
    "......######....",
    "......##s###....",
    ".....###s####...",
    ".....###s####...",
    ".....###s####...",
    ".....####s###...",
    ".....####s###...",
    "......###s##....",
    "......######....",
    ".......####.....",
    "................",
])

# A petal: broad and rounded at its tip, narrowing to its base, a crease down the middle.
PETAL = shade([
    "................",
    "........#####...",
    "......########..",
    ".....##########.",
    "....#######s###.",
    "....######s####.",
    "...######s#####.",
    "...#####s######.",
    "...####s######..",
    "...###s######...",
    "...##s######....",
    "...#s#####......",
    "...s####........",
    "...###..........",
    "..##............",
    "................",
])
# Later: its tip has curled back over, the fold in shadow.
PETAL_CURLED = shade([
    "................",
    "................",
    "........####....",
    ".....#######ss..",
    "....######ssss#.",
    "....#####kkkk##.",
    "...######s#####.",
    "...#####s######.",
    "...####s######..",
    "...###s######...",
    "...##s######....",
    "...#s#####......",
    "...s####........",
    "...###..........",
    "..##............",
    "................",
])

SPORE_BIG = [".###.", "#####", "#####", "#####", ".###."]
SPORE = [".##.", "####", "####", ".##."]
SPORE_SMALL = ["##", "##"]
# Spores: a few round motes of unequal size; later they have drifted further apart.
SPORES = shade(compose([(SPORE_BIG, 2, 3), (SPORE, 10, 2), (SPORE, 8, 10), (SPORE_SMALL, 3, 11), (SPORE_SMALL, 13, 8)]), SOFT)
SPORES_DRIFTED = shade(compose([(SPORE_BIG, 1, 2), (SPORE, 11, 1), (SPORE, 9, 11), (SPORE_SMALL, 2, 12), (SPORE_SMALL, 14, 7),
                                (SPORE_SMALL, 7, 7)]), SOFT)

# A length of vine: a winding stem with a leaf on either side.
VINE = shade([
    ".......####.....",
    "......####......",
    ".....####..##...",
    ".....#####s####.",
    ".....######s###.",
    "......####.###..",
    ".......####.....",
    "........####....",
    "........####....",
    "........####....",
    "..###..####.....",
    ".##s######......",
    ".###s####.......",
    "...######.......",
    ".....####.......",
    "......####......",
])
# Later: it has swayed, its leaves lifting.
VINE_WOUND = shade([
    ".......####.....",
    "......####..##..",
    ".....#####s####.",
    ".....######s###.",
    ".....####..###..",
    "......####......",
    ".......####.....",
    "........####....",
    "........####....",
    "........####....",
    ".......####.....",
    "..###.####......",
    ".##s#####.......",
    ".###s####.......",
    "...######.......",
    "......####......",
])

# A bead of sap: a teardrop with a bright wet glint.
SAP = shade([
    "................",
    ".......##.......",
    ".......##.......",
    "......####......",
    "......####......",
    ".....######.....",
    "....########....",
    "...##########...",
    "...##########...",
    "..############..",
    "..############..",
    "..############..",
    "...##########...",
    "....########....",
    ".....######.....",
    "................",
])
# Later: it has stretched as it drips.
SAP_STRETCHED = shade([
    "................",
    ".......##.......",
    ".......##.......",
    ".......##.......",
    "......####......",
    "......####......",
    ".....######.....",
    "....########....",
    "...##########...",
    "...##########...",
    "...##########...",
    "...##########...",
    "....########....",
    ".....######.....",
    "......####......",
    "................",
])

# Living tissue: a rounded mass of a few large cells, their walls in shadow, each with a light nucleus.
TISSUE = shade([
    "................",
    ".....######.....",
    "...##########...",
    "..#####s######..",
    "..#l###s###l##..",
    ".######s#######.",
    ".######s#######.",
    ".sssssss#######.",
    ".#######sssssss.",
    ".##l#####s###l#.",
    ".########s#####.",
    "..#######s####..",
    "..###l###s###...",
    "...######s##....",
    ".....######.....",
    "................",
])
# Later: the cells have shifted, the walls pulling into new seams.
TISSUE_SHIFTED = shade([
    "................",
    ".....######.....",
    "...####s#####...",
    "..#####s######..",
    "..#l###s##l###..",
    ".#####s########.",
    ".#####s########.",
    ".ssssss########.",
    ".######ssssssss.",
    ".#l#####s####l#.",
    ".#######s######.",
    "..######s#####..",
    "..##l###s####...",
    "...#####s###....",
    ".....######.....",
    "................",
])

# A rose thorn: a point curving up from a broad heel on a short length of stem.
THORN = shade([
    "................",
    "..............#.",
    ".............##.",
    "............###.",
    "...........###..",
    "..........####..",
    ".........####...",
    "........#####...",
    ".......######...",
    ".....########...",
    "...##########...",
    ".##############.",
    ".#s##########s#.",
    "..############..",
    "................",
    "................",
])
# Later: a glint runs along its point as it turns.
THORN_GLINT = edit(THORN, "13,3,h 12,4,h 11,5,h")

SPRITES = [
    (LEAF, LEAF_TURNED), (SEED, SEED_SPROUTED), (PETAL, PETAL_CURLED), (SPORES, SPORES_DRIFTED),
    (VINE, VINE_WOUND), (SAP, SAP_STRETCHED), (TISSUE, TISSUE_SHIFTED), (THORN, THORN_GLINT),
]


def sprite(style, frame):
    return render(SPRITES[style][frame])


def main():
    root = Path(__file__).resolve().parent.parent
    tex = root / 'src/main/resources/assets/wildercord/textures/particle'
    tex.mkdir(parents=True, exist_ok=True)
    names = []
    for style in range(8):
        for frame in range(2):
            name = f'life_{style}_{frame}'
            sprite(style, frame).save(tex / f'{name}.png')
            names.append('wildercord:' + name)
    p = root / 'src/main/resources/assets/wildercord/particles/life.json'
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps({'textures': names}, indent=2) + '\n', encoding='utf-8', newline='\n')


if __name__ == '__main__':
    main()
