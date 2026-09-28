"""Art for Wildercord's world events: the Fallen Star, a lump of dark starstone split by cracks of
pale blue starlight, its light pulsing out from a bright heart.

Same house style as world_art.py (ramps lit from the top-left, no anti-aliasing, grooves carved
with a lit lower lip), and built from its helpers.

Public API (imported by generate_assets.py):
    fallen_star_textures() -> dict      ("fallen_star_side", "fallen_star_top": 8-frame lists;
                                         "fallen_star_bottom": one image)

Run this file directly to render a review sheet into build/art-preview/event_art.png.
"""
from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from item_art import Canvas, ramp  # noqa: E402
from world_art import WELL_FRAMES, bevel, carve, cells, noise, path_distance, pulse, stone_fill  # noqa: E402

# starstone, darkest..lightest: a blue-black, faintly metallic
SS = ramp("#07080F", "#0C0F1C", "#12172A", "#1A2138", "#232C48", "#2F3A5A", "#3E4B70", "#56648C")
# starlight by level 0..1: a dim crack -> pale blue -> white
STAR = ramp("#1A2244", "#2A3A70", "#40589E", "#6282CC", "#8EACEE", "#BCD0FF", "#E6EEFF", "#FFFFFF")

# Cracks in the side: '#' a crack, 'n' the heart they run from.
SIDE_CRACKS = """
    ................
    .......#........
    .......#.....#..
    ......#.....#...
    ..#...#....#....
    ...#..#...#.....
    ....#.##.#......
    .....#nn#.......
    ....##nn##......
    ...#..#..#......
    ..#...#...##....
    .#....#.....#...
    ......#......#..
    .....#..........
    ................
    ................
"""

# The top: a four-pointed star of light with cracks running off its points.
TOP_CRACKS = """
    ................
    ..#.............
    ...#.....#......
    ....#....#......
    .....#..###.....
    ......#.#n#..#..
    .......#nnn.#...
    ....###nnnnn###.
    ...#...#nnn#....
    .......#.n#.....
    ......#..#.#....
    .....#...#..#...
    ....#....#...#..
    .........#......
    ................
    ................
"""


def star_at(level: float):
    i = max(0.0, min(1.0, level)) * (len(STAR) - 1)
    return STAR[int(round(i))]


def _base(seed: int) -> Canvas:
    """Rough starstone, pitted, with a few glints of metal in it."""
    cv = Canvas()
    stone_fill(cv, SS, seed, 2, 5)
    for y in range(16):
        for x in range(16):
            r = noise(x, y, seed + 7)
            if r > 0.965:
                cv.put(x, y, SS[7])
            elif r < 0.05:
                cv.put(x, y, SS[1])
    bevel(cv, SS, light=6, dark=1)
    return cv


def _face(text: str, seed: int, frame: int | None) -> Canvas:
    cv = _base(seed)
    crack = {(x, y) for x, y, ch in cells(text) if ch == "#"}
    heart = [(x, y) for x, y, ch in cells(text) if ch == "n"]
    dist = path_distance(crack | set(heart), heart)
    for p in crack:
        dist.setdefault(p, 8)
    groove = {p: dist[p] for p in crack}
    if frame is None:
        carve(cv, groove, lambda p, d: star_at(0.45 - 0.03 * d))
        for p in heart:
            cv.put(*p, STAR[5])
        return cv
    # A pulse of light rolls out from the heart along every crack.
    carve(cv, groove, lambda p, d: star_at(0.45 + 0.5 * pulse(d, frame, wavelength=12.0) - 0.02 * d), bleed=(STAR[3], 0.18))
    beat = pulse(0, frame, wavelength=12.0, width=4)
    for p in heart:
        cv.put(*p, star_at(0.82 + 0.18 * beat))
    return cv


def fallen_star_textures() -> dict:
    """The side and top pulse over 8 frames; the bottom is plain starstone."""
    return {
        "fallen_star_side": [_face(SIDE_CRACKS, 41, f).image() for f in range(WELL_FRAMES)],
        "fallen_star_top": [_face(TOP_CRACKS, 53, f).image() for f in range(WELL_FRAMES)],
        "fallen_star_bottom": _base(67).image(),
    }


def preview(out_dir: str) -> str:
    textures = fallen_star_textures()
    S = 8
    tiles = []
    for name, im in textures.items():
        for i, frame in enumerate(im if isinstance(im, list) else [im]):
            tiles.append(frame.resize((16 * S, 16 * S), Image.NEAREST))
    sheet = Image.new("RGBA", (len(tiles) * (16 * S + 8) + 8, 16 * S + 16), (0x8B, 0x8B, 0x8B, 255))
    for i, tile in enumerate(tiles):
        sheet.alpha_composite(tile, (8 + i * (16 * S + 8), 8))
    Path(out_dir).mkdir(parents=True, exist_ok=True)
    out = str(Path(out_dir) / "event_art.png")
    sheet.save(out)
    return out


if __name__ == "__main__":
    default = str(Path(__file__).resolve().parent.parent / "build" / "art-preview")
    print(preview(sys.argv[1] if len(sys.argv) > 1 else default))
