"""Void material particles: fold, jaw, tooth, cloth, haze, shard, sculk, pressure, shell, remnant and hound.

Void matter takes light away, so these are near-black bodies with a rim of light on the edges that
face it (particle_pixels.DARK); the tint each spell sends (VoidParticle) colours the rim, and the
body stays dark whatever it is. Silhouettes are drawn with '#' and lit by particle_pixels.shade.
Each style has two frames: the second is the same piece later in its life (closing, torn, turned).

Writes textures/particle/void_material_<style>_<frame>.png and particles/void_material.json.
"""
from pathlib import Path
import json

from particle_pixels import DARK, compose, edit, render, shade

# Haze is soft: no bright rim, only a slightly lighter edge round a dark middle.
HAZE_RAMP = {"edge_lit": "B", "edge": "D", "hi": "D", "hi2": "K", "body": "K", "lo": "K"}


def dark(rows):
    return shade(rows, DARK)


# A fold in space: two dark lips either side of a clear slit.
FOLD = dark([
    "................",
    "..###......##...",
    ".####.....####..",
    ".#####....#####.",
    ".#####....#####.",
    "..#####..######.",
    "..#####..#####..",
    "..#####..#####..",
    "..#####..#####..",
    ".#####..######..",
    ".#####..######..",
    ".#####...#####..",
    ".######..#####..",
    "..####...####...",
    "...##.....##....",
    "................",
])
# Later: the lips are closing on the slit.
FOLD_CLOSING = dark([
    "................",
    "...###.....##...",
    "..####....####..",
    "..#####...####..",
    "..######.#####..",
    "...######.####..",
    "...######.####..",
    "...######.####..",
    "...######.####..",
    "..######.#####..",
    "..######.#####..",
    "..######.####...",
    "..#######.###...",
    "...####..####...",
    "....##....##....",
    "................",
])

# Two jaw plates with jagged teeth, upper and lower.
JAW = dark([
    "................",
    "..##########....",
    ".##############.",
    ".##############.",
    ".##.###.###.##..",
    "..#..#...#...#..",
    "................",
    "................",
    "................",
    "...#...#...#..#.",
    "..###.###.##.##.",
    ".##############.",
    ".##############.",
    "...##########...",
    "................",
    "................",
])
# Later: the jaws have snapped nearly shut, teeth between teeth.
JAW_SHUT = dark([
    "................",
    "................",
    "..##########....",
    ".##############.",
    ".##############.",
    ".##.###.###.##..",
    "..#..#...#...#..",
    "....#...#...#...",
    "...###.###.###..",
    ".##############.",
    ".##############.",
    "...##########...",
    "................",
    "................",
    "................",
    "................",
])

# A hooked tooth: a broad root, a curved shaft, a sharp point.
TOOTH = dark([
    "........##......",
    "........###.....",
    ".........###....",
    ".........####...",
    ".........#####..",
    "........######..",
    "........######..",
    ".......#######..",
    "......########..",
    ".....########...",
    "....#########...",
    "...##########...",
    "..###########...",
    "..##########....",
    "..##########....",
    "................",
])
# Later: its point has chipped away.
TOOTH_CHIPPED = edit(TOOTH, "8,0,. 9,0,. 8,1,. 9,1,. 10,1,. 9,2,r 10,2,r 11,2,k 12,0,r 13,0,k 12,1,k")

# Ragged dark cloth: a straight upper hem, folds hanging down, three torn tails.
CLOTH = dark([
    "................",
    ".#############..",
    ".##############.",
    ".##B####B######.",
    ".##B#####B#####.",
    ".###B####B####..",
    ".###B#####B###..",
    ".####B####B###..",
    "..###B#####B##..",
    "..###B#####B###.",
    "..####B####B###.",
    "..#####.###.###.",
    "...###..##...##.",
    "....#....#....#.",
    "................",
    "................",
])
# Later: it has fluttered, the folds and tails swinging over.
CLOTH_FLUTTER = dark([
    "................",
    "..#############.",
    ".##############.",
    ".####B####B####.",
    ".####B#####B###.",
    "..####B####B###.",
    "..####B#####B##.",
    "..#####B####B##.",
    "..#####B#####B#.",
    ".#####B#####B##.",
    ".#####B####B###.",
    ".####.####.####.",
    "..###..###..##..",
    "...#....#....#..",
    "................",
    "................",
])

HAZE_BIG = [".####.", "######", "######", "######", ".####."]
HAZE_PUFF = [".###.", "#####", "#####", ".###."]
HAZE_SMALL = [".##.", "####", ".##."]
# Haze: a dark clump of vapour pockets, no glow; later the pockets have spread and thinned.
HAZE = shade(compose([(HAZE_BIG, 3, 5), (HAZE_PUFF, 8, 2), (HAZE_PUFF, 8, 9), (HAZE_SMALL, 1, 10), (HAZE_SMALL, 12, 7)]), HAZE_RAMP)
HAZE_SPREAD = shade(compose([(HAZE_BIG, 2, 5), (HAZE_SMALL, 9, 1), (HAZE_PUFF, 10, 10), (HAZE_SMALL, 0, 11), (HAZE_SMALL, 13, 6),
                             (HAZE_SMALL, 6, 12)]), HAZE_RAMP)

# An angular shard of dark matter with one lit facet.
SHARD = dark([
    "................",
    "..........####..",
    "........######..",
    "......#######...",
    "....###B####....",
    "...###B#####....",
    "..###B######....",
    "..##B######.....",
    "...#B######.....",
    "...#B#####......",
    "....B#####......",
    "....#####.......",
    ".....###........",
    ".....##.........",
    "................",
    "................",
])
# Later: a crack of light has opened across it.
SHARD_CRACKED = edit(SHARD, "11,2,r 10,3,r 9,4,r 9,5,k 8,6,r 7,7,r 7,8,k 6,9,r")

# Sculk-like fibres: a dark stem forking into thin rim-lit tendrils, their tips burning.
SCULK = edit(dark([
    "...#......#.....",
    "...##.....#...#.",
    "....#....##..##.",
    "....##...#..##..",
    ".....#..##.##...",
    ".....##.#.##....",
    "......####......",
    "......####......",
    "......###.......",
    ".....####.......",
    ".....###........",
    ".....###........",
    "....####........",
    "....###.........",
    "...#####........",
    "................",
]), "3,0,h 10,0,h 14,1,h")
# Later: the tendrils have curled and the light has moved to new tips.
SCULK_CURLED = edit(dark([
    "..#........#....",
    "...#......#....#",
    "...##.....#..##.",
    "....##...##.##..",
    ".....#..##.##...",
    ".....##.#.##....",
    "......####......",
    "......####......",
    "......###.......",
    ".....####.......",
    ".....###........",
    ".....###........",
    "....####........",
    "....###.........",
    "...#####........",
    "................",
]), "2,0,h 11,0,h 15,1,h")

# Pressure: three hard plates pushed out of line with each other.
PRESSURE = dark([
    "................",
    "...#########....",
    ".############...",
    "..###########...",
    "................",
    ".....##########.",
    "...############.",
    "....##########..",
    "................",
    ".#########......",
    "############....",
    ".###########....",
    "................",
    "................",
    "................",
    "................",
])
# Later: the plates are squeezed thin and pushed further out of line.
PRESSURE_SQUEEZED = dark([
    "................",
    "................",
    "................",
    "....#########...",
    "..###########...",
    "................",
    "#########.......",
    "###########.....",
    "................",
    "......##########",
    ".....###########",
    "................",
    "................",
    "................",
    "................",
    "................",
])

# A hinged shell: a rounded lid over a base, the seam between them and a pin socket.
SHELL = dark([
    "................",
    "....########....",
    "..##DDDDDDDD##..",
    ".##DDDDDDDDDD##.",
    ".##############.",
    ".kkkkkkkkkkkkkk.",
    ".##############.",
    ".##############.",
    ".######rr######.",
    ".######rr######.",
    ".##############.",
    "..############..",
    "....########....",
    "................",
    "................",
    "................",
])
# Later: the lid has lifted off its seam.
SHELL_OPEN = dark([
    "....########....",
    "..##DDDDDDDD##..",
    ".##DDDDDDDDDD##.",
    ".##############.",
    "..############..",
    "................",
    ".##############.",
    ".##############.",
    ".######rr######.",
    ".######rr######.",
    ".##############.",
    "..############..",
    "....########....",
    "................",
    "................",
    "................",
])

# A remnant: a ragged scrap of shadow with a hole torn through it.
REMNANT = dark([
    "................",
    "....#####.......",
    "...#########....",
    "..###########...",
    ".#####..######..",
    ".####....######.",
    ".#####..#######.",
    "..############..",
    "..###########...",
    "...##########...",
    "..####.#####....",
    "..###...####....",
    "..##.....###....",
    "...#......#.....",
    "................",
    "................",
])
# Later: it has torn further, the hole widening and a tail coming loose.
REMNANT_TORN = dark([
    "................",
    "....#####.......",
    "...#########....",
    "..####..#####...",
    ".####....#####..",
    ".####.....#####.",
    ".#####...######.",
    "..############..",
    "..####.######...",
    "...###..#####...",
    "..###...#####...",
    "..##.....###....",
    "..#.......#.....",
    "...........#....",
    "................",
    "................",
])

# A running shadow hound: pricked ear, long muzzle, a raised tail, legs at full stretch.
HOUND = edit(dark([
    "................",
    "............#...",
    "...........##...",
    "..........#####.",
    "#.........######",
    "##.......######.",
    ".##.....#####...",
    "..##########....",
    "..##########....",
    "..##########....",
    "...########.....",
    "...##....###....",
    "..##......##....",
    ".##........##...",
    "##..........##..",
    "................",
]), "12,4,h")
# Later: legs gathered under it mid-stride.
HOUND_GATHERED = edit(dark([
    "................",
    "............#...",
    "...........##...",
    "#.........#####.",
    "##........######",
    ".##......######.",
    "..##....#####...",
    "..##########....",
    "..##########....",
    "..##########....",
    "...########.....",
    "....##.###......",
    ".....####.......",
    "....##..##......",
    "...##....##.....",
    "................",
]), "12,4,h")

SPRITES = [
    (FOLD, FOLD_CLOSING), (JAW, JAW_SHUT), (TOOTH, TOOTH_CHIPPED), (CLOTH, CLOTH_FLUTTER), (HAZE, HAZE_SPREAD),
    (SHARD, SHARD_CRACKED), (SCULK, SCULK_CURLED), (PRESSURE, PRESSURE_SQUEEZED), (SHELL, SHELL_OPEN),
    (REMNANT, REMNANT_TORN), (HOUND, HOUND_GATHERED),
]


def sprite(style, frame):
    return render(SPRITES[style][frame])


def main():
    root = Path(__file__).resolve().parent.parent
    tex = root / 'src/main/resources/assets/wildercord/textures/particle'
    tex.mkdir(parents=True, exist_ok=True)
    names = []
    for style in range(11):
        for frame in range(2):
            name = f'void_material_{style}_{frame}'
            sprite(style, frame).save(tex / f'{name}.png')
            names.append('wildercord:' + name)
    p = root / 'src/main/resources/assets/wildercord/particles/void_material.json'
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps({'textures': names}, indent=2) + '\n', encoding='utf-8', newline='\n')


if __name__ == '__main__':
    main()
