"""Earth material particles: rock, slab, grit, dust, fracture, bone, root and crystal.

Tinted in game by the colour each spell sends (EarthParticle), so drawn in greys; see particle_pixels.
Each style has two frames: the second is the same piece later in its life (chipped, split, spread).

Writes textures/particle/earth_<style>_<frame>.png and particles/earth.json.
"""
from pathlib import Path
import json

from particle_pixels import compose, edit, grid, place, render

ROOT = Path(__file__).resolve().parent.parent

# A faceted boulder: lit top face, a mid front face, a shaded right face past the ridge, one crack.
ROCK = grid([
    "................",
    ".....oooooo.....",
    "...oohhhhhloo...",
    "..ohhhhhhllllo..",
    ".ohhhhllllllllo.",
    ".olllllllllllmso",
    ".ommmmmmmmlsssso",
    ".ommmmmmmmlsssso",
    ".ommmmkmmmlsskso",
    ".ommmmmkmmlsssso",
    ".ommmmmkmmlsksso",
    ".osmmmmmkmlssso.",
    "..osmmmmmmlsso..",
    "...ooossssooo...",
    "......oooo......",
    "................",
])
# Later: the lower right corner has broken away along the crack, a chip still falling beside it.
ROCK_CHIPPED = grid([
    "................",
    ".....oooooo.....",
    "...oohhhhhloo...",
    "..ohhhhhhllllo..",
    ".ohhhhllllllllo.",
    ".olllllllllllmso",
    ".ommmmmmmmlsssso",
    ".ommmmmmmmlsssso",
    ".ommmmkmmmlsskso",
    ".ommmmmkmmlssso.",
    ".ommmmmkmmlsko..",
    ".osmmmmmkmlko...",
    "..osmmmmmkko..oo",
    "...ooosssoo..olo",
    "......ooo.....o.",
    "................",
])

# A flat stratified plate: light top face, a front edge showing three courses of stone.
SLAB = grid([
    "................",
    "................",
    "....oooooooo....",
    "..oohhhhhhllooo.",
    ".ohhhhllllllllso",
    ".ohllllllllllsso",
    ".oommmmmmmmmssso",
    ".ommmmmmmmmmssso",
    ".okkkkkkkkkkkkso",
    ".ommmmmmmmmssso.",
    ".osssssssssssko.",
    ".okkkkkkkkkkkko.",
    "..ooooooooooooo.",
    "................",
    "................",
    "................",
])
# Later: split through, the two halves pulling apart.
SLAB_SPLIT = edit(SLAB, "8,3,k 7,4,k 7,5,k 8,6,k 8,7,k 7,9,k 7,10,k 6,4,h 6,5,l 9,6,l 9,9,l")

PEBBLE_BIG = ["oooo.", "ohllo", "olmso", "omsso", ".ooo."]
PEBBLE = [".oo.", "ohlo", "omso", ".oo."]
PEBBLE_TINY = ["oo", "lo", ".."]
PEBBLE_FLAT = [".ooo.", "ohlmo", "ossso", ".ooo."]
# Grit: separate pebbles of unequal size; later they've scattered and one has split.
GRIT = compose([(PEBBLE_BIG, 2, 3), (PEBBLE, 10, 2), (PEBBLE_FLAT, 9, 10), (PEBBLE, 3, 11), (PEBBLE_TINY, 7, 8), (PEBBLE_TINY, 13, 7)])
GRIT_SCATTERED = compose([(PEBBLE_BIG, 1, 4), (PEBBLE, 11, 1), (PEBBLE_FLAT, 10, 11), (PEBBLE_TINY, 2, 12), (PEBBLE_TINY, 5, 13),
                          (PEBBLE_TINY, 7, 7), (PEBBLE_TINY, 14, 7)])

PUFF_BIG = [".1111.", "112221", "122223", "122233", ".2333.", "..33.."]
PUFF = [".111.", "11223", "12233", ".333."]
PUFF_SMALL = [".11.", "1223", ".33."]
# Dust: a few soft puffs of fine soil, no outline; later they've drifted apart and thinned.
DUST = compose([(PUFF_BIG, 4, 5), (PUFF, 9, 3), (PUFF, 8, 9), (PUFF_SMALL, 2, 10), (PUFF_SMALL, 11, 8)])
DUST_SPREAD = compose([(PUFF_BIG, 3, 6), (PUFF_SMALL, 10, 2), (PUFF, 10, 10), (PUFF_SMALL, 1, 11), (PUFF_SMALL, 12, 7),
                       (["1"], 7, 2), (["2"], 14, 13), (["2"], 6, 13)])

# A fault through the ground: a dark crack with freshly broken, lighter lips, one branch.
FRACTURE = grid([
    "..lo............",
    "..hko...........",
    "...lko..........",
    "....lkol........",
    ".....okkol......",
    "......lokkl.....",
    ".......lkol.....",
    "......lkol......",
    "......okoll.....",
    ".......lkkoool..",
    "........lokkkkl.",
    "........lkol.lo.",
    ".........lko....",
    "..........lko...",
    "...........lkl..",
    "............ll..",
])
# Later: the crack has opened a pixel wider along its length.
FRACTURE_WIDE = edit(FRACTURE, "3,1,k 4,2,k 5,3,k 6,4,k 7,5,k 8,6,k 7,7,k 7,8,k 8,9,k 9,10,k 10,12,k 11,13,k 12,14,k")

# A bone claw: a broad knuckled socket, a joint ridge, a shaft curving to a hooked point.
BONE = grid([
    "........oo......",
    "........olo.....",
    ".........olo....",
    ".........ohso...",
    ".........ohlso..",
    "........ohllso..",
    "........ohlmso..",
    ".......ohlmmso..",
    "......ohlmmsso..",
    ".....ohllmmso...",
    "....ohllmmsso...",
    "...ohsssssso....",
    "..ohllmmmsso....",
    "..olllmmsso.....",
    "..ooooooooo.....",
    "................",
])
# Later: the hooked point has snapped off and tumbles beside it.
BONE_CHIPPED = edit(BONE, "8,0,. 9,0,. 8,1,. 9,1,. 10,1,. 10,2,k 12,0,o 13,0,o 12,1,o 13,1,l 14,1,o 13,2,o")

# A root: a forked woody root with bark ridges and a clod of earth still on it.
ROOT_PIECE = grid([
    "..oo........oo..",
    ".olko......olo..",
    ".olsko....olso..",
    "..olsko..olso...",
    "...olsko.olso...",
    "....olskolso....",
    ".....olsklso....",
    ".....olsksso....",
    "......olskso....",
    "......olksso....",
    ".....oolskso....",
    "....ommlksmoo...",
    "...omllmmsmsso..",
    "...omlmssmssso..",
    "....ossssskso...",
    ".....ooooooo....",
])
# Later: the clod is crumbling, a lump of soil dropping away from it.
ROOT_RUNNING = place(edit(ROOT_PIECE, "12,13,o 12,14,o 11,14,o 11,15,."), ["oo.", "lso", "oo."], 13, 13)

# A six-sided crystal: a lit left face, a light front face, a shaded right face, pointed both ends.
CRYSTAL = grid([
    ".......oo.......",
    "......ohso......",
    ".....ohhmso.....",
    "....ohhlmsso....",
    "...ohhlllmsso...",
    "...ohhlllmsso...",
    "...ohhlllmsso...",
    "...ohhlllmsso...",
    "...ohhlllmsso...",
    "...ohhlllmsso...",
    "...ohhlllmsso...",
    "...ohhlllmsso...",
    "....olllmsso....",
    ".....ollmso.....",
    "......omso......",
    ".......oo.......",
])
# Later: it has turned, the lit face narrowing as the front face comes round.
CRYSTAL_TURNED = grid([
    ".......oo.......",
    "......ohso......",
    ".....ohhmso.....",
    "....ohhlmsso....",
    "...ohllllmsso...",
    "...ohllllmsso...",
    "...ohllllmsso...",
    "...ohllllmsso...",
    "...ohllllmsso...",
    "...ohllllmsso...",
    "...ohllllmsso...",
    "...ohllllmsso...",
    "....olllmsso....",
    ".....ollmso.....",
    "......omso......",
    ".......oo.......",
])

SPRITES = [
    (ROCK, ROCK_CHIPPED), (SLAB, SLAB_SPLIT), (GRIT, GRIT_SCATTERED), (DUST, DUST_SPREAD),
    (FRACTURE, FRACTURE_WIDE), (BONE, BONE_CHIPPED), (ROOT_PIECE, ROOT_RUNNING), (CRYSTAL, CRYSTAL_TURNED),
]


def sprite(style, frame):
    return render(SPRITES[style][frame])


def main():
    tex = ROOT / 'src/main/resources/assets/wildercord/textures/particle'
    tex.mkdir(parents=True, exist_ok=True)
    textures = []
    for style in range(8):
        for frame in range(2):
            name = f'earth_{style}_{frame}'
            sprite(style, frame).save(tex / f'{name}.png')
            textures.append('wildercord:' + name)
    p = ROOT / 'src/main/resources/assets/wildercord/particles/earth.json'
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps({'textures': textures}, indent=2) + '\n', encoding='utf-8', newline='\n')


if __name__ == '__main__':
    main()
