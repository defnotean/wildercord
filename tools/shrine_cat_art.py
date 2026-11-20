"""Shrine Cats (Yuki, Odie, Freya, Kuro), Sakura Villagers, Karakuri Sentinel, Paper Lantern Spirit,
and Suzu Cat-Bell Charm 128x128 skins, icons, spawn eggs, loot tables, recipes, and text."""
from __future__ import annotations

import random
from PIL import Image, ImageDraw

from bobcat_art import egg_icon

LANG = {
    "entity.wildercord.shrine_cat": "Shrine Cat",
    "item.wildercord.shrine_cat_spawn_egg": "Shrine Cat Spawn Egg",
    "guide.wildercord.shrine_cat.hint": "Four guardian cats — Yuki, Odie, Freya, and Kuro — presiding over the four tiers of the Cat Sanctuary and strolling the Sakura Village.",
    "guide.wildercord.shrine_cat": "Four unique cats named Yuki, Odie, Freya, and Kuro. Each guards a floor of the Cat Sanctuary with a gentle trial, grants a shrine buff upon yielding, and may be tamed once Kuro's fourth-floor trial is complete.",
    "entity.wildercord.sakura_villager": "Sakura Villager",
    "item.wildercord.sakura_villager_spawn_egg": "Sakura Villager Spawn Egg",
    "guide.wildercord.sakura_villager.hint": "Six custom kimono-clad residents — Tea Masters, Shrine Maidens, Katana Swordsmiths, Lantern Artisans, Onsen Hostesses, and Koi Canal Fishermen — tending Sakura Villages.",
    "guide.wildercord.sakura_villager": "Peaceful residents of Japanese Sakura Villages across six professions who bow to visitors and trade Suzu Cat-Bell Charms, Blank Runes, lanterns, golden carrots, and salmon for emeralds.",
    "entity.wildercord.karakuri_sentinel": "Karakuri Sentinel",
    "item.wildercord.karakuri_sentinel_spawn_egg": "Karakuri Sentinel Spawn Egg",
    "guide.wildercord.karakuri_sentinel.hint": "Lacquered clockwork automata patrolling pagoda halls and cedar bathhouses.",
    "guide.wildercord.karakuri_sentinel": "Clockwork wooden-and-brass samurai automata that spin their interlocking chest gears before delivering a sweeping naginata slash.",
    "entity.wildercord.paper_lantern_spirit": "Paper Lantern Spirit",
    "item.wildercord.paper_lantern_spirit_spawn_egg": "Paper Lantern Spirit Spawn Egg",
    "guide.wildercord.paper_lantern_spirit.hint": "Drifting crimson-and-gold paper lanterns hovering through bamboo courts and bathhouses.",
    "guide.wildercord.paper_lantern_spirit": "Floating chochin-obake lantern spirits that unhinge their lower rim to expose a blazing ember crystal core before pulsing a flame ring.",
    "item.wildercord.cat_bell_charm": "Suzu Cat-Bell Charm",
}


def _px(c, delta=0):
    return tuple(max(0, min(255, ch + delta)) for ch in c) + (255,)


def _face(draw: ImageDraw.ImageDraw, rng: random.Random, rect, base, fur=True, light=0):
    x0, y0, x1, y1 = rect
    if x1 <= x0 or y1 <= y0:
        return
    draw.rectangle((x0, y0, x1 - 1, y1 - 1), fill=_px(base, light))
    if fur:
        for _ in range(max(4, (x1 - x0) * (y1 - y0) * 2 // 3)):
            x = rng.randrange(x0, x1)
            y = rng.randrange(y0, y1)
            delta = rng.choice([-20, -12, -6, 6, 12, 20])
            draw.point((x, y), fill=_px(base, light + delta))


def _cube(draw: ImageDraw.ImageDraw, rng: random.Random, u, v, w, h, d, color, fur=True):
    _face(draw, rng, (u + d, v, u + d + w, v + d), color, fur=fur, light=10)
    _face(draw, rng, (u + d + w, v, u + d + 2 * w, v + d), color, fur=fur, light=-16)
    _face(draw, rng, (u, v + d, u + d, v + d + h), color, fur=fur, light=-8)
    _face(draw, rng, (u + d, v + d, u + d + w, v + d + h), color, fur=fur, light=6)
    _face(draw, rng, (u + d + w, v + d, u + 2 * d + w, v + d + h), color, fur=fur, light=-6)
    _face(draw, rng, (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h), color, fur=fur, light=-18)


def _cat_skin(variant: str) -> Image.Image:
    im = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)
    rng = random.Random(int.from_bytes(variant.encode("utf-8"), "big"))

    if variant == "yuki":
        coat, dark, bib, eye, nose = (118, 113, 107), (52, 48, 45), (242, 239, 234), (160, 212, 134), (48, 36, 36)
        inner_ear = (196, 146, 146)
    elif variant == "odie":
        coat, dark, bib, eye, nose = (220, 146, 70), (170, 92, 36), (246, 208, 154), (76, 44, 24), (232, 134, 144)
        inner_ear = (232, 166, 146)
    elif variant == "freya":
        coat, dark, bib, eye, nose = (116, 126, 140), (88, 96, 112), (144, 154, 168), (170, 214, 72), (56, 56, 66)
        inner_ear = (182, 136, 148)
    else:  # kuro
        coat, dark, bib, eye, nose = (24, 24, 29), (14, 14, 18), (38, 38, 46), (246, 198, 42), (18, 16, 20)
        inner_ear = (66, 52, 58)

    # Core ribcage (0,0: 5x5x6) & hindquarters (24,0: 5x4x5)
    _cube(draw, rng, 0, 0, 5, 5, 6, coat)
    _cube(draw, rng, 24, 0, 5, 4, 5, coat)
    # Rose-pink collar + gold clasp, rear ribbon knot & silver Suzu bell (for Freya + any tamed cat)
    _cube(draw, rng, 50, 0, 5, 3, 2, (230, 134, 164), fur=False)
    draw.point((54, 3), fill=(242, 204, 88, 255))
    _cube(draw, rng, 66, 0, 3, 2, 1, (226, 118, 152), fur=False)
    draw.point((68, 1), fill=(244, 168, 194, 255))
    _cube(draw, rng, 76, 0, 2, 2, 2, (218, 226, 236), fur=False)
    draw.point((78, 2), fill=(250, 252, 255, 255))
    draw.point((79, 3), fill=(92, 98, 112, 255))

    # Longhair fluffy coat (0,14: 6x5x9) & 3-tier chest ruff (34,14 / 34,20 / 48,20)
    _cube(draw, rng, 0, 14, 6, 5, 9, coat)
    _cube(draw, rng, 34, 14, 5, 2, 2, bib if variant in ("yuki", "odie") else coat)
    _cube(draw, rng, 34, 20, 4, 2, 2, bib if variant in ("yuki", "odie") else coat)
    _cube(draw, rng, 48, 20, 2, 1, 2, bib if variant in ("yuki", "odie") else coat)

    # Head (0,32: 6x5x5), muzzle (24,32: 3x2x2), chin tuft (24,38: 2x1x2)
    _cube(draw, rng, 0, 32, 6, 5, 5, coat)
    _cube(draw, rng, 24, 32, 3, 2, 2, bib if variant in ("yuki", "odie", "freya") else coat)
    _cube(draw, rng, 24, 38, 2, 1, 2, bib if variant in ("yuki", "odie") else coat)
    # Ears (36,32 & 44,32), Kuro notched ear (36,38 & 44,38), cheek ruffs (50,32 & 62,32), whisker swatch (70,32)
    _cube(draw, rng, 36, 32, 2, 2, 1, coat)
    draw.point((37, 34), fill=_px(inner_ear))
    _cube(draw, rng, 44, 32, 1, 1, 1, dark)
    _cube(draw, rng, 36, 38, 1, 2, 1, dark)
    _cube(draw, rng, 44, 38, 1, 1, 1, dark)
    _cube(draw, rng, 50, 32, 2, 3, 3, bib if variant == "yuki" else coat)
    _cube(draw, rng, 62, 32, 1, 2, 2, bib if variant in ("yuki", "odie") else coat)
    _cube(draw, rng, 70, 32, 3, 1, 1, (246, 244, 238), fur=False)

    # 2-segment forelegs (0,50 & 10,50) and hindlegs (20,50 & 32,50)
    _cube(draw, rng, 0, 50, 2, 4, 2, coat)
    _cube(draw, rng, 10, 50, 2, 3, 2, bib if variant == "odie" else coat)
    if variant == "yuki":
        draw.line((12, 54, 13, 54), fill=_px(bib))
    _cube(draw, rng, 20, 50, 2, 4, 3, coat)
    _cube(draw, rng, 32, 50, 2, 3, 2, coat)
    if variant == "odie":
        draw.line((34, 54, 35, 54), fill=_px(bib))

    # 3-segment sleek (0,64 / 12,64 / 24,64) & plume (0,74 / 14,74 / 28,74) tails
    _cube(draw, rng, 0, 64, 2, 2, 3, coat)
    _cube(draw, rng, 12, 64, 2, 2, 3, coat)
    _cube(draw, rng, 24, 64, 2, 2, 3, dark)
    _cube(draw, rng, 0, 74, 3, 3, 3, coat)
    _cube(draw, rng, 14, 74, 3, 3, 3, coat)
    _cube(draw, rng, 28, 74, 2, 2, 3, bib if variant == "odie" else dark)

    # Tabby mackerel stripes across back, flanks, chest legs & tails for Yuki and Odie.
    if variant in ("yuki", "odie"):
        for x in (6, 8, 10):
            draw.line((x, 1, x, 5), fill=_px(dark))
        for y in (16, 18, 20):
            draw.line((2, y, 22, y), fill=_px(dark))
        # Forehead 'M' marking & cheek tabby stripes on head front (5..10, 37..41)
        for x in (6, 9):
            draw.point((x, 37), fill=_px(dark))
        draw.point((7, 38), fill=_px(dark))
        draw.point((8, 38), fill=_px(dark))
        draw.line((2, 39, 4, 39), fill=_px(dark))
        draw.line((12, 39, 14, 39), fill=_px(dark))
        if variant == "yuki":
            # Crisp snowy-white cheek blaze beneath eyes.
            draw.rectangle((5, 40, 10, 41), fill=_px(bib))
        for u_leg in (2, 12, 23, 34):
            draw.line((u_leg, 53, u_leg + 1, 53), fill=_px(dark))
        draw.line((3, 75, 5, 75), fill=_px(dark))
        draw.line((17, 75, 19, 75), fill=_px(dark))
    elif variant == "kuro":
        # Subtle charcoal-slate highlight edge so Kuro's silhouettes stay sharp in darkness.
        draw.line((6, 0, 10, 0), fill=_px((44, 46, 54)))
        draw.line((5, 37, 10, 37), fill=_px((40, 42, 50)))

    # Expressive eyes on head front (x=5..10, y=37..41) & nose on muzzle front (26..28, 34..35)
    if variant == "odie":
        # Odie's peaceful sleepy squinted amber eyes + cute pink nose.
        draw.line((6, 39, 7, 39), fill=_px(eye))
        draw.line((8, 39, 9, 39), fill=_px(eye))
    else:
        draw.rectangle((6, 38, 6, 39), fill=_px(eye))
        draw.rectangle((9, 38, 9, 39), fill=_px(eye))
        draw.point((6, 39), fill=(18, 18, 22, 255))
        draw.point((9, 39), fill=(18, 18, 22, 255))
        draw.point((6, 38), fill=_px(eye, 35))
        draw.point((9, 38), fill=_px(eye, 35))
    draw.point((27, 34), fill=_px(nose))
    if variant == "yuki":
        draw.line((26, 35, 28, 35), fill=(246, 244, 240, 255))
    return im


def _villager_skin(role: str) -> Image.Image:
    im = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)
    rng = random.Random(int.from_bytes(role.encode("utf-8"), "big"))

    palettes = {
        "tea": dict(
            skin=(238, 196, 164), hair=(40, 32, 32), robe=(88, 126, 92), trim=(240, 232, 212),
            hakama=(56, 78, 58), obi=(222, 206, 160), eye=(28, 24, 30), hat=(208, 174, 106),
        ),
        "shrine": dict(
            skin=(238, 196, 164), hair=(26, 22, 28), robe=(244, 242, 238), trim=(196, 48, 54),
            hakama=(182, 42, 48), obi=(224, 178, 56), eye=(28, 24, 30), hat=(208, 174, 106),
        ),
        "smith": dict(
            skin=(224, 176, 142), hair=(166, 126, 100), robe=(44, 48, 60), trim=(196, 202, 212),
            hakama=(36, 40, 52), obi=(94, 62, 42), eye=(52, 128, 62), hat=(208, 174, 106),
        ),
        "lantern": dict(
            skin=(226, 178, 144), hair=(178, 134, 104), robe=(196, 106, 42), trim=(232, 164, 64),
            hakama=(92, 62, 42), obi=(232, 186, 60), eye=(52, 128, 62), hat=(208, 174, 106),
        ),
        "onsen": dict(
            skin=(238, 198, 168), hair=(26, 22, 30), robe=(196, 152, 212), trim=(232, 118, 164),
            hakama=(146, 104, 168), obi=(234, 188, 68), eye=(128, 56, 146), hat=(208, 174, 106),
        ),
        "fisher": dict(
            skin=(218, 170, 134), hair=(52, 40, 36), robe=(40, 82, 104), trim=(228, 238, 244),
            hakama=(34, 52, 74), obi=(198, 168, 106), eye=(52, 128, 62), hat=(166, 134, 78),
        ),
    }
    p = palettes[role]
    skin, hair, robe, trim = p["skin"], p["hair"], p["robe"], p["trim"]
    hakama, obi, eye_col, hat_col = p["hakama"], p["obi"], p["eye"], p["hat"]

    # Head (0,0: 8x8x8) -> front face (8,8..15,15)
    head_base = skin if role in ("smith", "lantern") else hair
    _cube(draw, rng, 0, 0, 8, 8, 8, head_base, fur=False)
    if role in ("smith", "lantern"):
        # Subtle short-cropped hair stubble cap on top/back while keeping clear warm face.
        draw.rectangle((8, 0, 15, 7), fill=_px(hair))
        draw.rectangle((16, 8, 31, 10), fill=_px(hair))
        draw.rectangle((0, 8, 7, 10), fill=_px(hair))
    _face(draw, rng, (8, 8, 16, 16), skin, fur=False, light=4)
    if role not in ("smith", "lantern"):
        draw.line((8, 8, 15, 8), fill=_px(hair))
        draw.line((8, 9, 15, 9), fill=_px(hair))
    if role in ("shrine", "onsen"):
        draw.point((9, 10), fill=_px(hair))
        draw.point((14, 10), fill=_px(hair))
        draw.point((9, 12), fill=(236, 162, 158, 255))
        draw.point((14, 12), fill=(236, 162, 158, 255))
    draw.point((10, 11), fill=_px(eye_col))
    draw.point((13, 11), fill=_px(eye_col))
    draw.point((10, 10), fill=(52, 40, 38, 255))
    draw.point((13, 10), fill=(52, 40, 38, 255))
    draw.line((11, 14, 12, 14), fill=(186, 134, 114, 255))
    if role == "lantern":
        # Painted upper brass spectacles bridge accents complementing 3D lower rim.
        draw.point((9, 11), fill=(218, 176, 68, 255))
        draw.point((11, 11), fill=(218, 176, 68, 255))
        draw.point((12, 11), fill=(218, 176, 68, 255))
        draw.point((14, 11), fill=(218, 176, 68, 255))

    # Rear bun (34,0), ponytail (48,0), side locks (58,0)
    _cube(draw, rng, 34, 0, 4, 4, 2, hair, fur=False)
    _cube(draw, rng, 48, 0, 3, 5, 1, hair, fur=False)
    _cube(draw, rng, 58, 0, 1, 6, 2, hair, fur=False)

    # Swordsmith white hachimaki headband (68,0), rear knot (102,0), twin tails (112,0)
    _cube(draw, rng, 68, 0, 8, 2, 8, (244, 242, 238), fur=False)
    _cube(draw, rng, 102, 0, 2, 2, 2, (238, 236, 232), fur=False)
    _cube(draw, rng, 112, 0, 1, 4, 1, (232, 230, 226), fur=False)

    # Kimono torso (0,18: 8x12x4) -> front chest (4..11, 22..33)
    _cube(draw, rng, 0, 18, 8, 12, 4, robe, fur=False)
    for dy in range(6):
        lx = 5 + dy // 2
        rx = 10 - dy // 2
        lapel_fill = (255, 255, 255) if role == "shrine" else (
            (238, 234, 224) if role == "tea" else _px(robe, 24)
        )
        draw.line((lx, 22 + dy, rx, 22 + dy), fill=lapel_fill if isinstance(lapel_fill, tuple) and len(lapel_fill) == 4 else _px(lapel_fill))
        draw.point((lx, 22 + dy), fill=_px(trim))
        draw.point((rx, 22 + dy), fill=_px(trim))
    if role in ("tea", "smith", "fisher"):
        inner = (242, 236, 220, 255) if role == "tea" else (_px(skin) if role == "fisher" else (62, 66, 78, 255))
        draw.rectangle((7, 22, 8, 24), fill=inner)
    else:
        draw.rectangle((7, 22, 8, 23), fill=_px(skin))

    if role == "lantern":
        # Woven orange-gold lattice hem panel on kimono skirt & back.
        for bx in range(4, 12):
            for by in range(29, 34):
                if (bx + by) % 2 == 0:
                    draw.point((bx, by), fill=(228, 148, 56, 255))
        for bx in range(16, 24):
            for by in range(29, 34):
                if (bx + by) % 2 == 0:
                    draw.point((bx, by), fill=(224, 142, 52, 255))
    elif role == "onsen":
        # Scatter delicate pink & ivory sakura petals over the wisteria yukata.
        for sx, sy in ((5, 23), (9, 25), (6, 29), (10, 31), (18, 24), (21, 28), (17, 32), (2, 26), (13, 30)):
            draw.point((sx, sy), fill=(246, 174, 206, 255))
            draw.point((sx + 1, sy), fill=(252, 228, 238, 255))
    elif role == "fisher":
        # Swelling white sea-wave crests around bottom happi hem.
        for wx in range(0, 24):
            crest_y = 32 if (wx % 4 in (1, 2)) else 33
            draw.line((wx, crest_y, wx, 33), fill=(238, 242, 246, 255))
            if wx % 4 == 2:
                draw.point((wx, 31), fill=(238, 242, 246, 255))

    # Obi sash (26,18), rear bow knot (52,18), pleated hakama waist (72,18), smith leather apron (102,18)
    _cube(draw, rng, 26, 18, 8, 4, 4, obi, fur=False)
    draw.line((30, 23, 37, 23), fill=_px(trim))
    _cube(draw, rng, 52, 18, 7, 4, 2, obi, fur=False)
    draw.line((54, 21, 60, 21), fill=_px(obi, -28))
    _cube(draw, rng, 72, 18, 9, 4, 5, hakama, fur=False)
    for px in range(77, 86, 2):
        draw.line((px, 23, px, 26), fill=_px(hakama, -26))
    if role == "fisher":
        for wx in range(72, 100):
            if wx % 3 == 0:
                draw.point((wx, 23), fill=(236, 242, 246, 255))
    _cube(draw, rng, 102, 18, 7, 10, 1, (58, 36, 26), fur=False)
    draw.point((103, 20), fill=(224, 178, 62, 255))
    draw.point((109, 20), fill=(224, 178, 62, 255))
    draw.line((103, 24, 109, 24), fill=(88, 56, 38, 255))

    # Woven conical amigasa / kasa bamboo hat tiers (0,42 / 58,42 / 98,42)
    for (u, v, w, h, d) in ((0, 42, 14, 1, 14), (58, 42, 10, 2, 10), (98, 42, 5, 2, 5)):
        _cube(draw, rng, u, v, w, h, d, hat_col, fur=True)
        for ry in range(v, v + d + h, 2):
            draw.line((u + d, ry, u + d + w - 1, ry), fill=_px(hat_col, -32))

    # White tabane-noshi hair band (0,62), 3D sakura kanzashi (34,62), ribbon (46,62), rear bow (54,62)
    _cube(draw, rng, 0, 62, 8, 1, 8, (246, 244, 242), fur=False)
    _cube(draw, rng, 34, 62, 2, 3, 3, (246, 148, 188), fur=False)
    draw.point((38, 66), fill=(252, 214, 88, 255))
    _cube(draw, rng, 46, 62, 1, 4, 2, (244, 182, 206), fur=False)
    _cube(draw, rng, 54, 62, 5, 2, 1, (246, 244, 242), fur=False)
    # Lantern Artisan flat cap (68,62), spectacles rim (102,62) & Onsen dual gold hairpins (102,67)
    _cube(draw, rng, 68, 62, 8, 3, 8, (34, 34, 40), fur=False)
    draw.line((76, 72, 83, 72), fill=(54, 54, 62, 255))
    # Transparent lenses with brass bridge & lower rim at (102,62..116,65)
    draw.line((105, 63, 106, 63), fill=(224, 182, 72, 255))
    draw.line((103, 64, 104, 64), fill=(224, 182, 72, 255))
    draw.line((107, 64, 108, 64), fill=(224, 182, 72, 255))
    draw.point((102, 63), fill=(192, 152, 54, 255))
    draw.point((109, 63), fill=(192, 152, 54, 255))
    _cube(draw, rng, 102, 67, 2, 3, 2, (238, 194, 72), fur=False)
    draw.point((104, 69), fill=(246, 148, 188, 255))

    # Articulated 2-segment arms (0,78), forearms (18,78) & hanging kimono sleeves (18,92)
    _cube(draw, rng, 0, 78, 4, 5, 4, robe, fur=False)
    _cube(draw, rng, 18, 78, 4, 5, 4, robe, fur=False)
    draw.rectangle((22, 85, 25, 86), fill=_px(skin))
    _cube(draw, rng, 18, 92, 4, 7, 4, robe, fur=False)
    if role in ("shrine", "onsen"):
        draw.line((22, 96, 22, 102), fill=_px(trim))
        draw.line((22, 102, 25, 102), fill=_px(trim))
    elif role == "lantern":
        draw.point((23, 98), fill=(228, 148, 56, 255))
        draw.point((24, 99), fill=(228, 148, 56, 255))

    # Hakama legs (38,78) with crisp pleats + white tabi socks & geta sandals (38,94)
    _cube(draw, rng, 38, 78, 4, 8, 5, hakama, fur=False)
    for px in (44, 46, 51, 53):
        draw.line((px, 83, px, 90), fill=_px(hakama, -26))
    sock_col = (244, 244, 242) if role != "smith" else (78, 82, 94)
    _cube(draw, rng, 38, 94, 4, 2, 5, sock_col, fur=False)
    draw.rectangle((43, 100, 46, 100), fill=(96, 60, 38, 255))

    # Gohei wand (70,78 shaft + 76,92 / 86,92 white shide)
    _cube(draw, rng, 70, 78, 1, 1, 10, (144, 96, 56), fur=False)
    _cube(draw, rng, 76, 92, 2, 5, 2, (246, 246, 244), fur=False)
    _cube(draw, rng, 86, 92, 2, 4, 2, (238, 238, 236), fur=False)

    # Onsen cedar bath bucket (98,78) + folded white towel (98,89), Fisherman wicker creel (98,96) + fishing rod (120,78 / 120,95)
    _cube(draw, rng, 98, 78, 5, 4, 5, (186, 126, 74), fur=False)
    draw.line((98, 84, 117, 84), fill=(96, 88, 84, 255))
    draw.line((98, 86, 117, 86), fill=(96, 88, 84, 255))
    _cube(draw, rng, 98, 89, 4, 2, 3, (246, 246, 244), fur=False)
    _cube(draw, rng, 98, 96, 3, 4, 4, (168, 122, 68), fur=True)
    for cx in range(98, 112, 2):
        draw.line((cx, 100, cx, 103), fill=(122, 84, 44, 255))
    _cube(draw, rng, 120, 78, 1, 14, 1, (172, 142, 70), fur=False)
    for ry in range(79, 93, 3):
        draw.line((120, ry, 123, ry), fill=(116, 90, 42, 255))
    _cube(draw, rng, 120, 95, 1, 6, 1, (236, 240, 244), fur=False)
    draw.rectangle((120, 100, 123, 101), fill=(214, 58, 48, 255))

    # Tea Tray (0,108 + 24,108), Swordsmith Hammer (36,108 handle + 56,108 steel head),
    # Lantern Artisan Pole (68,108) + Hanging Chochin Lantern (92,108 caps, 106,108 glowing body, 92,114 tassel)
    _cube(draw, rng, 0, 108, 6, 1, 5, (112, 68, 40), fur=False)
    _cube(draw, rng, 24, 108, 2, 2, 2, (84, 134, 78), fur=False)
    draw.rectangle((26, 108, 27, 109), fill=(116, 186, 94, 255))
    _cube(draw, rng, 36, 108, 1, 1, 8, (124, 78, 42), fur=False)
    _cube(draw, rng, 56, 108, 3, 3, 2, (132, 142, 156), fur=False)
    draw.line((58, 110, 60, 110), fill=(182, 194, 208, 255))
    _cube(draw, rng, 68, 108, 1, 1, 10, (182, 152, 76), fur=False)
    _cube(draw, rng, 92, 108, 3, 1, 3, (34, 30, 34), fur=False)
    _cube(draw, rng, 92, 114, 1, 3, 1, (196, 46, 46), fur=False)
    _cube(draw, rng, 106, 108, 4, 5, 4, (226, 62, 44), fur=False)
    for side_x in (106, 110, 114, 118):
        draw.rectangle((side_x + 1, 113, side_x + 2, 115), fill=(255, 214, 96, 255))
        draw.point((side_x + 1, 114), fill=(255, 244, 178, 255))
    return im


def _karakuri_skin() -> Image.Image:
    im = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)
    rng = random.Random(7731)

    wood, dark_wood, red_lacq = (122, 68, 44), (66, 36, 26), (168, 48, 38)
    brass, steel, glow = (228, 182, 62), (206, 218, 232), (255, 212, 82)

    # Cuirass chest (0,0: 8x10x6)
    _cube(draw, rng, 0, 0, 8, 10, 6, wood, fur=False)
    draw.rectangle((6, 6, 13, 15), outline=_px(brass), width=1)
    # Main gear (30,0: 5x5x1), pinion gear (44,0: 3x3x1), rear winding key (54,0 / 66,0)
    _cube(draw, rng, 30, 0, 5, 5, 1, brass, fur=False)
    draw.point((33, 3), fill=_px(dark_wood))
    _cube(draw, rng, 44, 0, 3, 3, 1, brass, fur=False)
    _cube(draw, rng, 54, 0, 1, 1, 3, brass, fur=False)
    _cube(draw, rng, 66, 0, 3, 5, 1, brass, fur=False)
    # Crimson lacquered kusazuri hip tassets (78,0: 9x4x7)
    _cube(draw, rng, 78, 0, 9, 4, 7, red_lacq, fur=False)
    for gx in range(85, 94, 3):
        draw.line((gx, 7, gx, 10), fill=_px(dark_wood))
        draw.point((gx + 1, 10), fill=_px(brass))

    # Menpo head (0,20: 8x8x8), kabuto dome (34,20: 9x4x9), shikoro neck plates (72,20: 10x4x7), kuwagata horns (0,40 / 10,40 / 18,40)
    _cube(draw, rng, 0, 20, 8, 8, 8, wood, fur=False)
    draw.rectangle((9, 31, 10, 31), fill=_px(glow))
    draw.rectangle((13, 31, 14, 31), fill=_px(glow))
    draw.line((10, 34, 13, 34), fill=_px(dark_wood))
    _cube(draw, rng, 34, 20, 9, 4, 9, dark_wood, fur=False)
    draw.line((43, 32, 51, 32), fill=_px(brass))
    _cube(draw, rng, 72, 20, 10, 4, 7, red_lacq, fur=False)
    for sx in range(79, 89, 2):
        draw.point((sx, 30), fill=_px(brass))
    for (u, v, w, h, d) in ((0, 40, 3, 2, 1), (10, 40, 2, 4, 1), (18, 40, 2, 3, 1)):
        _cube(draw, rng, u, v, w, h, d, brass, fur=False)

    # Sode pauldrons (0,52), upper arms (20,52), forearms (38,52), Naginata pole/collar/blade (56,52 / 62,52 / 72,52), Lantern buckler (82,52)
    _cube(draw, rng, 0, 52, 4, 4, 5, red_lacq, fur=False)
    draw.line((5, 58, 8, 58), fill=_px(brass))
    draw.line((5, 60, 8, 60), fill=_px(brass))
    _cube(draw, rng, 20, 52, 4, 5, 4, wood, fur=False)
    _cube(draw, rng, 38, 52, 4, 5, 4, red_lacq, fur=False)
    _cube(draw, rng, 56, 52, 1, 20, 1, red_lacq, fur=False)
    _cube(draw, rng, 62, 52, 2, 2, 2, brass, fur=False)
    _cube(draw, rng, 72, 52, 1, 6, 3, steel, fur=False)
    draw.line((72, 55, 72, 60), fill=(244, 248, 255, 255))
    _cube(draw, rng, 82, 52, 1, 7, 7, dark_wood, fur=False)
    draw.rectangle((83, 60, 87, 64), fill=(238, 198, 128, 255))
    draw.rectangle((84, 61, 86, 63), fill=(255, 232, 152, 255))

    # Articulated legs (0,74) & shin greaves (18,74)
    _cube(draw, rng, 0, 74, 4, 5, 4, wood, fur=False)
    _cube(draw, rng, 18, 74, 4, 5, 4, red_lacq, fur=False)
    draw.line((22, 82, 25, 82), fill=_px(brass))
    return im


def _lantern_spirit_skin() -> Image.Image:
    im = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)
    rng = random.Random(8842)

    cedar, paper, glow_paper = (56, 38, 30), (208, 68, 42), (242, 156, 72)
    rib, gold, flame, ofuda = (128, 32, 24), (242, 196, 68), (255, 224, 96), (244, 238, 224)

    # Stepped cedar canopy (0,0 & 50,0) + brass hanging loop (84,0)
    _cube(draw, rng, 0, 0, 12, 2, 12, cedar, fur=False)
    _cube(draw, rng, 50, 0, 8, 2, 8, cedar, fur=False)
    _cube(draw, rng, 84, 0, 4, 3, 2, gold, fur=False)
    draw.rectangle((87, 3, 88, 3), fill=(32, 24, 20, 255))

    # Upper pleated drum (0,18: 10x7x10)
    _cube(draw, rng, 0, 18, 10, 7, 10, paper, fur=False)
    # Warm glow gradient center & horizontal bamboo accordion ribs on all 4 walls
    for y in (29, 31, 33):
        draw.line((0, y, 39, y), fill=_px(rib))
    # Glowing haunted eyes on front face (x=10..19, y=28..34)
    draw.rectangle((12, 30, 13, 31), fill=(255, 248, 190, 255))
    draw.rectangle((16, 30, 17, 31), fill=(255, 248, 190, 255))
    # Swirling golden tomoe emblems on left, right, back faces
    for cx in (4, 24, 34):
        draw.point((cx, 30), fill=_px(gold))
        draw.point((cx - 1, 31), fill=_px(gold))
        draw.point((cx + 1, 32), fill=_px(gold))

    # Spinning crystal ember core (42,18: 5x5x5)
    _cube(draw, rng, 42, 18, 5, 5, 5, flame, fur=False)
    draw.rectangle((48, 24, 50, 26), fill=(255, 252, 218, 255))

    # Articulated lower jaw drum (0,38: 10x4x10) & dark cedar base rim (42,38: 9x2x9)
    _cube(draw, rng, 0, 38, 10, 4, 10, glow_paper, fur=False)
    draw.rectangle((10, 38, 19, 47), fill=_px(flame))
    draw.line((0, 49, 39, 49), fill=_px(rib))
    _cube(draw, rng, 42, 38, 9, 2, 9, cedar, fur=False)

    # 2-segment silk tassel (80,38 / 90,38) + bronze bell (100,38)
    _cube(draw, rng, 80, 38, 2, 4, 2, gold, fur=False)
    _cube(draw, rng, 90, 38, 2, 3, 2, gold, fur=False)
    _cube(draw, rng, 100, 38, 3, 2, 3, (198, 148, 58), fur=False)

    # 2-segment white ofuda talisman calligraphy wings (0,56 / 12,56)
    _cube(draw, rng, 0, 56, 1, 5, 4, ofuda, fur=False)
    draw.rectangle((1, 61, 2, 63), fill=(36, 34, 40, 255))
    draw.line((0, 60, 3, 60), fill=(192, 48, 42, 255))
    _cube(draw, rng, 12, 56, 1, 4, 4, ofuda, fur=False)
    draw.point((13, 61), fill=(36, 34, 40, 255))
    return im


def _cat_bell_charm_icon() -> Image.Image:
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)
    draw.arc((4, 1, 11, 7), 180, 360, fill=(236, 134, 164, 255))
    draw.line((6, 5, 9, 5), fill=(214, 96, 134, 255))
    draw.ellipse((4, 6, 11, 13), fill=(210, 218, 228, 255), outline=(126, 136, 152, 255))
    draw.point((6, 8), fill=(250, 252, 255, 255))
    draw.line((6, 11, 9, 11), fill=(72, 78, 92, 255))
    draw.point((7, 10), fill=(72, 78, 92, 255))
    draw.point((8, 10), fill=(72, 78, 92, 255))
    return im


def validate_uv_layouts() -> None:
    """Checks that every UV box in all four 128x128 skins fits within 128x128 with zero overlap."""
    layouts = {
        "shrine_cat": [
            (0, 0, 5, 5, 6), (24, 0, 5, 4, 5), (50, 0, 5, 3, 2), (66, 0, 3, 2, 1), (76, 0, 2, 2, 2),
            (0, 14, 6, 5, 9), (34, 14, 5, 2, 2), (34, 20, 4, 2, 2), (48, 20, 2, 1, 2),
            (0, 32, 6, 5, 5), (24, 32, 3, 2, 2), (24, 38, 2, 1, 2), (36, 32, 2, 2, 1), (44, 32, 1, 1, 1),
            (36, 38, 1, 2, 1), (44, 38, 1, 1, 1), (50, 32, 2, 3, 3), (62, 32, 1, 2, 2), (70, 32, 3, 1, 1),
            (0, 50, 2, 4, 2), (10, 50, 2, 3, 2), (20, 50, 2, 4, 3), (32, 50, 2, 3, 2),
            (0, 64, 2, 2, 3), (12, 64, 2, 2, 3), (24, 64, 2, 2, 3),
            (0, 74, 3, 3, 3), (14, 74, 3, 3, 3), (28, 74, 2, 2, 3),
        ],
        "sakura_villager": [
            (0, 0, 8, 8, 8), (34, 0, 4, 4, 2), (48, 0, 3, 5, 1), (58, 0, 1, 6, 2),
            (68, 0, 8, 2, 8), (102, 0, 2, 2, 2), (112, 0, 1, 4, 1),
            (0, 18, 8, 12, 4), (26, 18, 8, 4, 4), (52, 18, 7, 4, 2), (72, 18, 9, 4, 5), (102, 18, 7, 10, 1),
            (0, 42, 14, 1, 14), (58, 42, 10, 2, 10), (98, 42, 5, 2, 5),
            (0, 62, 8, 1, 8), (34, 62, 2, 3, 3), (46, 62, 1, 4, 2), (54, 62, 5, 2, 1),
            (68, 62, 8, 3, 8), (102, 62, 6, 2, 1), (102, 67, 2, 3, 2),
            (0, 78, 4, 5, 4), (18, 78, 4, 5, 4), (18, 92, 4, 7, 4),
            (38, 78, 4, 8, 5), (38, 94, 4, 2, 5),
            (70, 78, 1, 1, 10), (76, 92, 2, 5, 2), (86, 92, 2, 4, 2),
            (98, 78, 5, 4, 5), (98, 89, 4, 2, 3), (98, 96, 3, 4, 4),
            (120, 78, 1, 14, 1), (120, 95, 1, 6, 1),
            (0, 108, 6, 1, 5), (24, 108, 2, 2, 2),
            (36, 108, 1, 1, 8), (56, 108, 3, 3, 2),
            (68, 108, 1, 1, 10), (92, 108, 3, 1, 3), (92, 114, 1, 3, 1), (106, 108, 4, 5, 4),
        ],
        "karakuri_sentinel": [
            (0, 0, 8, 10, 6), (30, 0, 5, 5, 1), (44, 0, 3, 3, 1), (54, 0, 1, 1, 3), (66, 0, 3, 5, 1), (78, 0, 9, 4, 7),
            (0, 20, 8, 8, 8), (34, 20, 9, 4, 9), (72, 20, 10, 4, 7), (0, 40, 3, 2, 1), (10, 40, 2, 4, 1), (18, 40, 2, 3, 1),
            (0, 52, 4, 4, 5), (20, 52, 4, 5, 4), (38, 52, 4, 5, 4), (56, 52, 1, 20, 1), (62, 52, 2, 2, 2), (72, 52, 1, 6, 3), (82, 52, 1, 7, 7),
            (0, 74, 4, 5, 4), (18, 74, 4, 5, 4),
        ],
        "paper_lantern_spirit": [
            (0, 0, 12, 2, 12), (50, 0, 8, 2, 8), (84, 0, 4, 3, 2),
            (0, 18, 10, 7, 10), (42, 18, 5, 5, 5),
            (0, 38, 10, 4, 10), (42, 38, 9, 2, 9), (80, 38, 2, 4, 2), (90, 38, 2, 3, 2), (100, 38, 3, 2, 3),
            (0, 56, 1, 5, 4), (12, 56, 1, 4, 4),
        ],
    }
    for name, boxes in layouts.items():
        rects = []
        for u, v, w, h, d in boxes:
            rw, rh = 2 * d + 2 * w, d + h
            assert 0 <= u and u + rw <= 128 and 0 <= v and v + rh <= 128, f"{name} OOB: {(u, v, w, h, d)}"
            cur = (u, v, u + rw, v + rh)
            for prev in rects:
                if not (cur[2] <= prev[0] or prev[2] <= cur[0] or cur[3] <= prev[1] or prev[3] <= cur[1]):
                    raise AssertionError(f"{name} UV overlap: {cur} vs {prev}")
            rects.append(cur)


def write(g):
    validate_uv_layouts()
    tex = g.ASSETS / "textures"
    for name in ("yuki", "odie", "freya", "kuro"):
        g.save(_cat_skin(name), tex / f"entity/shrine_cat_{name}.png")
    for role in ("tea", "shrine", "smith", "lantern", "onsen", "fisher"):
        g.save(_villager_skin(role), tex / f"entity/sakura_villager_{role}.png")
    g.save(_karakuri_skin(), tex / "entity/karakuri_sentinel.png")
    g.save(_lantern_spirit_skin(), tex / "entity/paper_lantern_spirit.png")

    g.save(_cat_bell_charm_icon(), tex / "item/cat_bell_charm.png")
    g.item_model("cat_bell_charm", "cat_bell_charm")
    g.write_json(g.ASSETS / "items/cat_bell_charm.json", {"model": {"type": "minecraft:model", "model": "wildercord:item/cat_bell_charm"}})
    g.write_json(g.DATA / "recipe/cat_bell_charm.json", {
        "type": "minecraft:crafting_shaped",
        "category": "equipment",
        "pattern": [" S ", "IAI", " N "],
        "key": {
            "S": "minecraft:string",
            "I": "minecraft:iron_ingot",
            "A": "minecraft:amethyst_shard",
            "N": "minecraft:pink_dye",
        },
        "result": {"id": "wildercord:cat_bell_charm", "count": 1},
    })

    eggs = {
        "shrine_cat_spawn_egg": (("#8D8782", "#D9924A", "#F0ECE6"), "#E67CA0", "shrine_cat"),
        "sakura_villager_spawn_egg": (("#F2C4D0", "#587A5C", "#F6E8EC"), "#D94452", "sakura_villager"),
        "karakuri_sentinel_spawn_egg": (("#6B3A26", "#9E5C38", "#DAAE3E"), "#FFE066", "karakuri_sentinel"),
        "paper_lantern_spirit_spawn_egg": (("#B83228", "#EEB86C", "#FFF0BE"), "#FF6B3D", "paper_lantern_spirit"),
    }
    for egg_name, (shell, spots, label) in eggs.items():
        g.save(egg_icon(shell, spots, name=label), tex / f"item/{egg_name}.png")
        g.write_json(g.ASSETS / f"models/item/{egg_name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"wildercord:item/{egg_name}"}})
        g.write_json(g.ASSETS / f"items/{egg_name}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{egg_name}"}})

    for mob_id, drop in (("karakuri_sentinel", "minecraft:gold_nugget"), ("paper_lantern_spirit", "minecraft:lantern")):
        g.write_json(g.DATA / f"loot_table/entities/{mob_id}.json", {
            "type": "minecraft:entity",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": drop}]}],
            "random_sequence": f"wildercord:entities/{mob_id}",
        })
