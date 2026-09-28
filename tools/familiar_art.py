"""Art for familiars and Cord cosmetics: the wisp's skin and glow layer, the Wisp Lantern item,
the beads' materials and the Cosmetics page's little icons.

Same house style as item_art.py and world_art.py: ASCII pictograms for items and icons, ramps lit
from the top-left, no anti-aliasing. The wisp and the glass bead are pale: they're tinted in game
(the wisp by its element, the bead by the rune or the chosen glow). Glow layers are pale on
transparency and drawn emissive.

Public API (imported by generate_assets.py):
    main()     writes every texture below and the lantern's item model
        textures/entity/wisp.png                     (128x16, the body's eight frames)
        textures/entity/wisp_glow.png                (64x32, its halo, spark and tail puff)
        textures/entity/cord/bead_<material>.png     (8x8, the beads' own colours)
        textures/entity/cord/bead_core.png           (8x8, the light inside a coloured bead)
        textures/item/wisp_lantern.png               (16x16)
        textures/gui/sprites/cord/bead_<material>.png, trail_<trail>.png, glow_spell.png (12x12)

Run this file directly to write them.
"""
from __future__ import annotations

import json
import math
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from item_art import Canvas, blit, hexc, mix  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/wildercord"
TEX = ASSETS / "textures"

MATERIALS = ("gold", "obsidian", "amethyst", "bone", "prismarine")
TRAILS = ("none", "sparks", "petals", "snow", "embers", "stars")

# ============================================================== the wisp (drawn as light, see WispRenderer)
#
# A wisp isn't a model: it's a few camera-facing sprites. wisp.png (128x16) is its body, eight 16x16
# frames of a small pale orb with two eyes, drawn over the light: the eyes looking far left, left,
# ahead, right and far right (so it seems to turn as it flies), its back (no eyes), a blink, and a
# happy squint for when it flares. wisp_glow.png (64x32) is the light around it, added to the
# world: a halo (0,0 32x32), a four-point spark (32,0 16x16) and a soft puff for its tail (48,0 16x16).
# Everything is pale: the element tints it in game.

EYE = (40, 28, 62)
EYE_GLINT = (236, 236, 255)
WISP_FRAMES = ("far_left", "left", "ahead", "right", "far_right", "back", "blink", "happy")


def _step(a: float, levels: int = 6) -> float:
    """Stepped, like pixel art, rather than a smooth gradient."""
    return math.floor(max(0.0, min(1.0, a)) * levels + 0.35) / levels


def _orb(px, ox: int):
    """The body: a round orb of pale light, brightest up and to the left, its rim soft."""
    c = 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - c, y - c)
            if d > 7.1:
                continue
            lit = max(0.0, 1 - math.hypot(x - 5.6, y - 5.4) / 9.0)
            g = 200 + round(55 * _step(lit, 5))
            rim = d > 6.2
            a = 150 if rim else 255
            if rim:
                g -= 18
            px[ox + x, y] = (g, g, min(255, g + 6), a)
    # A glint of light high on the left.
    for (x, y) in ((4, 4), (5, 4), (4, 5)):
        px[ox + x, y] = (255, 255, 255, 255)


def _eye(px, ox: int, x: int, y: int = 7):
    """One eye: a little upright bean, a glint in its top."""
    px[ox + x, y] = EYE_GLINT + (255,)
    px[ox + x, y + 1] = EYE + (255,)
    px[ox + x, y + 2] = EYE + (255,)


def wisp_texture() -> Image.Image:
    img = Image.new("RGBA", (16 * len(WISP_FRAMES), 16), (0, 0, 0, 0))
    px = img.load()
    for i, frame in enumerate(WISP_FRAMES):
        ox = 16 * i
        _orb(px, ox)
        if frame == "far_left":
            _eye(px, ox, 3)
            _eye(px, ox, 6)
        elif frame == "left":
            _eye(px, ox, 5)
            _eye(px, ox, 8)
        elif frame == "ahead":
            _eye(px, ox, 6)
            _eye(px, ox, 9)
        elif frame == "right":
            _eye(px, ox, 7)
            _eye(px, ox, 10)
        elif frame == "far_right":
            _eye(px, ox, 9)
            _eye(px, ox, 12)
        elif frame == "blink":
            for x in (6, 9):
                px[ox + x, 9] = EYE + (255,)
        elif frame == "happy":
            # ^ ^: each eye a little arch.
            for x in (5, 9):
                px[ox + x, 9] = EYE + (255,)
                px[ox + x + 1, 8] = EYE + (255,)
                px[ox + x + 2, 9] = EYE + (255,)
    return img


def wisp_glow_texture() -> Image.Image:
    """The light round a wisp, white on transparency (its alpha is how much light it adds)."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = img.load()
    # The halo: stepped rings of light, with a faint four-point shimmer through it.
    c = 15.5
    for y in range(32):
        for x in range(32):
            dx, dy = x - c, y - c
            r = math.hypot(dx, dy)
            body = max(0.0, 1 - r / 15.5) ** 1.7
            ray = 0.4 * max(0.0, 1 - r / 15.5) ** 0.9 * max(0.0, 1 - min(abs(dx), abs(dy)) / 1.2)
            a = _step(max(body, ray), 7)
            if a > 0:
                px[x, y] = (255, 255, 255, round(255 * a))
    # The spark: a four-point star, bright in the middle.
    c = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = x - c, y - c
            r = math.hypot(dx, dy)
            core = max(0.0, 1 - r / 3.2) ** 1.2
            ray = max(0.0, 1 - r / 7.6) * max(0.0, 1 - min(abs(dx), abs(dy)) / 0.9)
            a = _step(max(core, ray), 5)
            if a > 0:
                px[32 + x, y] = (255, 255, 255, round(255 * a))
    # The puff: a small soft round light for the beads of its tail.
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - c, y - c)
            a = _step(max(0.0, 1 - r / 7.6) ** 1.3, 5)
            if a > 0:
                px[48 + x, y] = (255, 255, 255, round(255 * a))
    return img


# ============================================================== the Wisp Lantern (16x16)

LANTERN_PAL = {
    "o": hexc("#1C1826"), "i": hexc("#3A3348"), "I": hexc("#5C5270"), "h": hexc("#8A7FA0"),
    "g": hexc("#7A5A24"), "G": hexc("#C09A48"), "Y": hexc("#EBCB7A"),
    "w": hexc("#6FD8E8"), "W": hexc("#C8F8FF"), "*": hexc("#FFFFFF"), "e": hexc("#2A2440"),
    "p": hexc("#9C70EA"), "P": hexc("#D8BCFF"),
}

# A small lantern in dark iron and gold, its glass holding a wisp: a pale glowing orb with two eyes.
LANTERN = """
    ......oo........
    .....oGGo.......
    ......oo........
    ....oggGGo......
    ...oIhhhhIo.....
    ...oiPpWWio.....
    ...oiWW*Wio.....
    ...oiWeWeio.....
    ...oiwWWWio.....
    ...oipwwpio.....
    ...oIhhhhIo.....
    ....oggGGo......
    .....oooo.......
    ................
    ................
    ................
"""


def lantern_icon() -> Image.Image:
    cv = Canvas(16)
    blit(cv, LANTERN, LANTERN_PAL, 2, 1)
    return cv.image()


# ============================================================== bead materials (8x8, worn on the wrist)

BEAD_COLOURS = {
    # darkest, mid, light, highlight
    "gold": ((122, 84, 20), (214, 160, 52), (246, 214, 110), (255, 248, 210)),
    "obsidian": ((12, 8, 20), (34, 20, 54), (70, 44, 110), (170, 130, 240)),
    "amethyst": ((70, 38, 120), (136, 92, 204), (190, 150, 240), (240, 226, 255)),
    "bone": ((150, 138, 108), (214, 204, 176), (236, 230, 212), (255, 252, 240)),
    "prismarine": ((28, 92, 88), (82, 164, 150), (140, 212, 196), (220, 255, 246)),
}


def bead_texture(material: str) -> Image.Image:
    """A round bead in a material's own colours, lit from the top-left, with a little grain."""
    dark, mid, light, hi = BEAD_COLOURS[material]
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    px = img.load()
    for y in range(8):
        for x in range(8):
            d = math.hypot(x - 3.5, y - 3.5) / 5.0
            lit = max(0.0, 1 - math.hypot(x - 2.2, y - 2.2) / 6.0)
            c = mix(mid, light, lit) if d < 0.75 else mix(mid, dark, min(1, (d - 0.75) * 3))
            # A little grain: bone's pores, obsidian's glassy flecks, the facets of amethyst.
            if (x * 7 + y * 3) % 5 == 0 and material in ("bone", "obsidian", "amethyst", "prismarine"):
                c = mix(c, dark if material != "obsidian" else light, 0.35)
            px[x, y] = c + (255,)
    px[2, 2] = hi + (255,)
    return img


def bead_core() -> Image.Image:
    """The light inside a coloured bead: bright in the middle, gone by the rim (black adds no light)."""
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 255))
    px = img.load()
    for y in range(8):
        for x in range(8):
            v = max(0.0, 1 - math.hypot(x - 3.5, y - 3.5) / 3.2)
            g = round(255 * v ** 1.4)
            px[x, y] = (g, g, g, 255)
    return img


# ============================================================== Cosmetics page icons (12x12)

ICON_PAL = {
    "o": hexc("#0B0910"),
    # sparks
    "y": hexc("#FFE070"), "Y": hexc("#FFF8D0"), "k": hexc("#C89A30"),
    # petals
    "p": hexc("#F0A0C8"), "P": hexc("#FFD6EA"), "q": hexc("#C8709C"),
    # snow
    "s": hexc("#BCE8FF"), "S": hexc("#FFFFFF"), "t": hexc("#7FB8E0"),
    # embers
    "e": hexc("#FF8A3A"), "E": hexc("#FFD060"), "r": hexc("#C03A18"),
    # stars
    "v": hexc("#B8A8FF"), "V": hexc("#FFFFFF"), "u": hexc("#7C64D8"),
    # none
    "n": hexc("#5A5470"),
}

TRAIL_ICONS = {
    "none": """
        ............
        ...nnnnnn...
        ..n......n..
        .n......nn..
        .n.....n.n..
        .n....n..n..
        .n...n...n..
        .n..n....n..
        .n.n.....n..
        ..nn....n...
        ...nnnnnn...
        ............
    """,
    "sparks": """
        ............
        .....y......
        .....Y....k.
        ..yYYYYy....
        .....Y......
        .....y...y..
        .k......yYy.
        .........y..
        ...y........
        ..yYy.....k.
        ...y........
        ............
    """,
    "petals": """
        ............
        ....pp......
        ...pPPp.....
        ...pPPq.....
        ....qq...p..
        ........pPp.
        ..p.....qPq.
        .pPp.....q..
        .qPq........
        ..q...pp....
        .....pPPq...
        ......qq....
    """,
    "snow": """
        ............
        .....S......
        ...t.S.t....
        ....sSs.....
        .SSSSSSSS...
        ....sSs.....
        ...t.S.t..s.
        .....S...sSs
        ..........s.
        ..s.........
        .sSs........
        ..s.........
    """,
    "embers": """
        ............
        ......E.....
        .....eEe....
        ....eEEEe...
        ....rEEEr.e.
        ...e.rer..E.
        ..eEe.......
        ..rEr...e...
        ...r...eEe..
        .......rEr..
        ........r...
        ............
    """,
    "stars": """
        ............
        ....V.......
        ....V.......
        ..VVVVV.....
        ....V....u..
        ...V.V..uVu.
        .........u..
        ......v.....
        .u...vVv....
        uVu...v.....
        .u..........
        ............
    """,
}


def trail_icon(trail: str) -> Image.Image:
    cv = Canvas(12)
    blit(cv, TRAIL_ICONS[trail], ICON_PAL)
    return cv.image()


def bead_icon(material: str) -> Image.Image:
    """A bead of a material, 12x12, for its swatch: the worn bead scaled up with a dark rim."""
    dark, mid, light, hi = BEAD_COLOURS.get(material, ((120, 150, 180), (200, 230, 250), (235, 248, 255), (255, 255, 255)))
    img = Image.new("RGBA", (12, 12), (0, 0, 0, 0))
    px = img.load()
    for y in range(12):
        for x in range(12):
            d = math.hypot(x - 5.5, y - 5.5)
            if d > 5.2:
                continue
            if d > 4.4:
                px[x, y] = (11, 9, 16, 255)
                continue
            lit = max(0.0, 1 - math.hypot(x - 3.5, y - 3.5) / 7.0)
            c = mix(mid, light, lit) if d < 3.2 else mix(mid, dark, min(1, (d - 3.2)))
            px[x, y] = c + (255,)
    px[4, 3] = hi + (255,)
    px[3, 4] = hi + (255,)
    return img


def glow_spell_icon() -> Image.Image:
    """'Follow the spell': a bead split into four element colours."""
    quarters = [(0xF0, 0x6E, 0x32), (0x8C, 0xDC, 0xFF), (0x6E, 0xDC, 0x64), (0xE6, 0x78, 0xDC)]
    img = Image.new("RGBA", (12, 12), (0, 0, 0, 0))
    px = img.load()
    for y in range(12):
        for x in range(12):
            d = math.hypot(x - 5.5, y - 5.5)
            if d > 5.2:
                continue
            if d > 4.4:
                px[x, y] = (11, 9, 16, 255)
                continue
            q = (1 if x >= 6 else 0) + (2 if y >= 6 else 0)
            px[x, y] = mix(quarters[q], (255, 255, 255), max(0.0, 0.5 - d / 8)) + (255,)
    return img


# ============================================================== writing

def _save(img: Image.Image, path: Path):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def main():
    _save(wisp_texture(), TEX / "entity/wisp.png")
    _save(wisp_glow_texture(), TEX / "entity/wisp_glow.png")
    for material in MATERIALS:
        _save(bead_texture(material), TEX / f"entity/cord/bead_{material}.png")
    _save(bead_core(), TEX / "entity/cord/bead_core.png")
    _save(lantern_icon(), TEX / "item/wisp_lantern.png")
    sprites = TEX / "gui/sprites/cord"
    for material in ("glass",) + MATERIALS:
        _save(bead_icon(material), sprites / f"bead_{material}.png")
    for trail in TRAILS:
        _save(trail_icon(trail), sprites / f"trail_{trail}.png")
    _save(glow_spell_icon(), sprites / "glow_spell.png")
    model = ASSETS / "models/item/wisp_lantern.json"
    model.parent.mkdir(parents=True, exist_ok=True)
    model.write_text(json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": "wildercord:item/wisp_lantern"}}, indent=2) + "\n",
                     encoding="utf-8", newline="\n")
    item = ASSETS / "items/wisp_lantern.json"
    item.write_text(json.dumps({"model": {"type": "minecraft:model", "model": "wildercord:item/wisp_lantern"}}, indent=2) + "\n",
                    encoding="utf-8", newline="\n")
    print("familiar and cosmetic textures written")


if __name__ == "__main__":
    main()
