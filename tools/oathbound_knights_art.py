"""Asset and texture generator for the Oathbound High Knights (starting with Knight #1: Oathbound Bastion)."""
from __future__ import annotations

import math
from PIL import Image

from item_art import Canvas, hexc, mix
from world_art import box, noise

LANG = {
    "entity.wildercord.oathbound_bastion": "Oathbound Bastion",
    "item.wildercord.oathbound_bastion_spawn_egg": "Oathbound Bastion Spawn Egg",
    "guide.wildercord.oathbound_bastion": (
        "An unbroken champion knight of the Way of the Bulwark clad in gold-filigreed obsidian plate, crouched behind "
        "a towering Sun-Anvil Greatshield and wielding a glowing Flanged Greatmace. When braced behind its hexagonal Aura "
        "barrier, frontal blows bounce off and arrows fly straight back; shatter its stance with an axe, bait its "
        "Shield-Ram into a wall, and clear the expanding ring of its Anvil Slam."
    ),
    "guide.wildercord.oathbound_bastion.hint": (
        "Commands the deepest haunts — strongholds, ancient cities, and sunken expeditions — alongside fallen knights."
    ),
    "subtitles.wildercord.oathbound_bastion.phalanx": "Bastion locks its greatshield",
    "subtitles.wildercord.oathbound_bastion.anvil_slam": "Bastion hammers the floor",
    "entity.wildercord.oathbound_blade_champion": "Oathbound Blade-Champion",
    "item.wildercord.oathbound_blade_champion_spawn_egg": "Oathbound Blade-Champion Spawn Egg",
    "guide.wildercord.oathbound_blade_champion": (
        "A peerless duel-master of the Way of the Blade armored in fluted silver and gold plate with a trailing "
        "crimson plume, diagonal sash, and split parchment-crimson banner cape. When it sets its Zweihänder in "
        "Wrathful Riposte, frontal swings trigger an immediate golden-crimson counter-cleave and arrows bounce straight "
        "back; smash its stance with an axe, dodge the shield-shattering Pommel-Break Lunge, and clear the crimson "
        "ground fissure of its Sky-Rending Guillotine."
    ),
    "guide.wildercord.oathbound_blade_champion.hint": (
        "Commands the deepest haunts — strongholds, ancient cities, and sunken expeditions — alongside fallen knights."
    ),
    "entity.wildercord.oathbound_halberdier": "Oathbound Halberdier",
    "item.wildercord.oathbound_halberdier_spawn_egg": "Oathbound Halberdier Spawn Egg",
    "guide.wildercord.oathbound_halberdier": (
        "A swift storm-glaive sentinel of the Oathbound order in gunmetal-indigo and cobalt plate, masked behind a "
        "winged falcon-beak helm and glowing cyan eyes. It chains 360-degree Tempest Whirls, high-speed Shadowstep "
        "Thrusts, Hook-and-Reap pulls, and four-way Thunderbolt Crashes; interrupt its telegraphed halberd windups "
        "with an axe to break its poise."
    ),
    "guide.wildercord.oathbound_halberdier.hint": (
        "Commands the deepest haunts — strongholds, ancient cities, and sunken expeditions — alongside fallen knights."
    ),
}

# Exact UV boxes matching OathboundBastionModel.createLayer() on a 128x128 sheet.
UV_BOXES = {
    "head": (0, 0, 8, 9, 8),
    "crown_band": (32, 0, 9, 2, 9),
    "crown_peak_front": (68, 0, 2, 3, 2),
    "crown_peak_side": (76, 0, 2, 2, 2),
    "fur_front": (68, 6, 12, 3, 8),
    "body": (0, 18, 10, 12, 6),
    "fur_back": (32, 18, 11, 4, 3),
    "rondel": (60, 18, 2, 2, 1),
    "pauldron_outer": (68, 18, 7, 6, 7),
    "pauldron_ridge": (96, 18, 3, 2, 7),
    "tabard": (0, 37, 6, 10, 1),
    "tasset": (14, 37, 4, 7, 6),
    "cape": (36, 37, 12, 17, 1),
    "belt_buckle": (64, 37, 4, 3, 1),
    "upper_arm": (0, 56, 5, 7, 5),
    "fore_arm": (20, 56, 5, 7, 5),
    "thigh": (40, 56, 5, 6, 5),
    "shin": (60, 56, 5, 6, 5),
    "kneecap": (80, 56, 4, 4, 2),
    "sabaton_toe": (92, 56, 5, 2, 2),
    "mace_shaft": (0, 70, 2, 2, 16),
    "mace_core": (36, 70, 4, 4, 6),
    "mace_flange_x": (56, 70, 8, 2, 6),
    "mace_flange_y": (84, 70, 2, 8, 6),
    "mace_finial": (100, 70, 3, 3, 2),
    "mace_pommel": (110, 70, 3, 3, 2),
    "shield_upper": (0, 92, 2, 16, 12),
    "shield_step1": (28, 92, 2, 3, 10),
    "shield_step2": (28, 106, 2, 3, 6),
    "shield_step3": (44, 106, 2, 2, 3),
    "shield_emblem": (54, 92, 1, 9, 8),
    "shield_aura": (74, 92, 0, 16, 16),
}

# Obsidian-iron, antique gold, fur, royal navy cloth, leather straps, amber-gold glow.
STEEL = ((20, 24, 34), (34, 41, 56), (52, 63, 84), (78, 93, 122), (108, 124, 156))
GOLD = ((112, 68, 14), (176, 120, 24), (228, 178, 56), (255, 224, 118), (255, 246, 184))
FUR = ((22, 25, 32), (34, 38, 48), (50, 56, 70), (70, 78, 94))
NAVY = ((16, 28, 68), (24, 44, 102), (36, 66, 142), (52, 90, 176))
LEATHER = ((56, 34, 20), (88, 54, 32), (118, 76, 44))
AMBER = ((196, 86, 10), (246, 152, 26), (255, 214, 78), (255, 248, 192))

FACE_SHADE = {"top": 1, "bottom": -1, "front": 1, "right": 0, "left": 0, "back": 0}


def _assert_no_overlap(boxes: dict[str, tuple[int, int, int, int, int]] | None = None) -> None:
    rects = []
    target = UV_BOXES if boxes is None else boxes
    for name, (u, v, w, h, d) in target.items():
        rw, rh = 2 * (d + w), d + h
        assert u >= 0 and v >= 0 and u + rw <= 128 and v + rh <= 128, f"{name} out of 128x128 bounds"
        rect = (u, v, u + rw, v + rh)
        for other, orect in rects:
            if rect[0] < orect[2] and rect[2] > orect[0] and rect[1] < orect[3] and rect[3] > orect[1]:
                raise AssertionError(f"UV overlap between {name} {rect} and {other} {orect}")
        rects.append((name, rect))


def _tone(ramp_tuple, idx):
    return ramp_tuple[max(0, min(len(ramp_tuple) - 1, idx))]


def _rgba(rgb, alpha=255):
    return (int(rgb[0]), int(rgb[1]), int(rgb[2]), int(alpha))


def _fill_rect(img: Image.Image, area, color) -> None:
    x0, y0, w, h = area
    px = img.load()
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            px[x, y] = color


def _paint_steel_box(img: Image.Image, key: str, fluted: bool = True, gold_bottom: bool = False, seed: int = 11) -> None:
    px = img.load()
    b = box(*UV_BOXES[key])
    for face, area in b.items():
        x0, y0, w, h = area
        if w <= 0 or h <= 0:
            continue
        sh = FACE_SHADE[face]
        for ly in range(h):
            for lx in range(w):
                idx = 2 + sh
                if fluted and face not in ("top", "bottom"):
                    idx += 1 if (lx % 2 == 0) else -1
                if ly == 0 and face not in ("top", "bottom"):
                    idx += 1
                if ly == h - 1 and face not in ("top", "bottom"):
                    idx -= 1
                if noise(x0 + lx, y0 + ly, seed) > 0.88:
                    idx += 1
                col = _tone(STEEL, idx)
                if gold_bottom and face not in ("top", "bottom") and ly == h - 1:
                    col = _tone(GOLD, 2 if (lx + ly) % 2 == 0 else 1)
                px[x0 + lx, y0 + ly] = _rgba(col)


def _paint_gold_box(img: Image.Image, key: str, seed: int = 33) -> None:
    px = img.load()
    b = box(*UV_BOXES[key])
    for face, area in b.items():
        x0, y0, w, h = area
        if w <= 0 or h <= 0:
            continue
        sh = FACE_SHADE[face]
        for ly in range(h):
            for lx in range(w):
                idx = 2 + sh + (1 if ly == 0 else 0) + (1 if noise(x0 + lx, y0 + ly, seed) > 0.85 else 0)
                px[x0 + lx, y0 + ly] = _rgba(_tone(GOLD, idx))


def _paint_fur_box(img: Image.Image, key: str, seed: int = 55) -> None:
    px = img.load()
    b = box(*UV_BOXES[key])
    for face, area in b.items():
        x0, y0, w, h = area
        if w <= 0 or h <= 0:
            continue
        sh = 1 if face == "top" else 0
        for ly in range(h):
            for lx in range(w):
                n = noise(x0 + lx, y0 + ly, seed)
                tuft = ((lx + (ly % 2)) % 3 == 0)
                idx = 1 + sh + (1 if tuft else 0) + (1 if n > 0.72 else -1 if n < 0.2 else 0)
                px[x0 + lx, y0 + ly] = _rgba(_tone(FUR, idx))


def _paint_quilted_navy(img: Image.Image, area, gold_border: bool = True) -> None:
    px = img.load()
    x0, y0, w, h = area
    for ly in range(h):
        for lx in range(w):
            if gold_border and (lx == 0 or lx == w - 1 or ly == 0 or ly == h - 1):
                col = _tone(GOLD, 3 if (ly == 0 or lx == 0) else 2)
            else:
                checker = ((lx + ly) % 2 == 0)
                edge = (lx == 1 or lx == w - 2)
                col = _tone(NAVY, 2 if checker else 1 if not edge else 0)
            px[x0 + lx, y0 + ly] = _rgba(col)


def _shield_emblem_pattern() -> list[str]:
    # 8w x 9h sculpted Sun-Anvil emblem jutting off shield front (+X outer face)
    return [
        "...GG...",
        ".G.YY.G.",
        "..GYYG..",
        "GGGGGGGG",
        ".AAAAAA.",
        "..AAAA..",
        "...AA...",
        "..AAAA..",
        ".GGGGGG.",
    ]


def _hex_barrier_rgba(glow: bool) -> Image.Image:
    # 16x16 hollow concentric hexagonal Aura rings (clean interior so Sun-Anvil crest shines through)
    tile = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    if not glow:
        return tile
    px = tile.load()
    cx, cy = 7.5, 7.5
    for y in range(16):
        for x in range(16):
            dx = abs(x - cx)
            dy = abs(y - cy)
            # Point-topped hexagon distance metric: max(|x|, 0.5*|x| + 0.866*|y|)
            r = max(dx, 0.5 * dx + 0.866025 * dy)
            d_outer = abs(r - 6.7)
            d_inner = abs(r - 4.3)
            if d_outer < 0.65:
                a = int(255 * (1.0 - d_outer / 0.7))
                px[x, y] = (*AMBER[2], max(110, a))
            elif d_inner < 0.55:
                a = int(230 * (1.0 - d_inner / 0.65))
                px[x, y] = (*AMBER[3], max(110, a))
    return tile


def _notch_flanges(img: Image.Image) -> None:
    """Carves serrated sunburst notches on the outer flange tips of the Greatmace on both skin and glow."""
    px = img.load()
    fx = box(*UV_BOXES["mace_flange_x"])
    for side in ("top", "bottom"):
        x0, y0, w, h = fx[side]
        for lx in (0, w - 1):
            for ly in (0, 2, h - 1):
                px[x0 + lx, y0 + ly] = (0, 0, 0, 0)
    fy = box(*UV_BOXES["mace_flange_y"])
    for side in ("right", "left"):
        x0, y0, w, h = fy[side]
        for ly in (0, h - 1):
            for lx in (0, 2, w - 1):
                px[x0 + lx, y0 + ly] = (0, 0, 0, 0)


def bastion_skin(glow: bool = False) -> Image.Image:
    _assert_no_overlap()
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    px = img.load()

    if not glow:
        # 1. Greathelm & Crown
        _paint_steel_box(img, "head", fluted=True, gold_bottom=True, seed=101)
        _paint_gold_box(img, "crown_band", seed=102)
        _paint_gold_box(img, "crown_peak_front", seed=103)
        _paint_gold_box(img, "crown_peak_side", seed=104)
        # Crown dark recessed gem slots along crown band front/sides
        cb = box(*UV_BOXES["crown_band"])
        for side in ("front", "back", "right", "left"):
            sx, sy, sw, _ = cb[side]
            for ox in (2, 6):
                if ox < sw:
                    px[sx + ox, sy + 1] = _rgba(GOLD[0])

        # Head front: bold gold T-frame + dark recessed visor slot
        hf_x, hf_y, _, _ = box(*UV_BOXES["head"])["front"]
        for lx in range(8):
            px[hf_x + lx, hf_y + 2] = _rgba(GOLD[2])
        for ly in range(1, 8):
            px[hf_x + 3, hf_y + ly] = _rgba(GOLD[3])
            px[hf_x + 4, hf_y + ly] = _rgba(GOLD[2])
        for ex, ey in ((1, 3), (2, 3), (5, 3), (6, 3), (3, 4), (4, 4), (3, 5), (4, 5), (3, 6), (4, 6)):
            px[hf_x + ex, hf_y + ey] = _rgba(AMBER[2] if ey == 3 else AMBER[1])
        # Horizontal riveted ridge across back & side of greathelm (concept back 3/4 view)
        hb_x, hb_y, _, _ = box(*UV_BOXES["head"])["back"]
        for lx in range(8):
            px[hb_x + lx, hb_y + 5] = _rgba(GOLD[1] if lx % 2 == 0 else STEEL[3])

        # 2. Torso, Chest Surcoat, Back Filigree, Fur Mantle, Belts, Tabards, Tassets, Cape
        _paint_fur_box(img, "fur_front", seed=201)
        _paint_fur_box(img, "fur_back", seed=202)
        _paint_steel_box(img, "body", fluted=True, gold_bottom=False, seed=203)
        _paint_gold_box(img, "rondel", seed=204)
        _paint_gold_box(img, "belt_buckle", seed=205)
        bb = box(*UV_BOXES["belt_buckle"])["front"]
        px[bb[0] + 1, bb[1] + 1] = _rgba(LEATHER[0])
        px[bb[0] + 2, bb[1] + 1] = _rgba(LEATHER[0])

        bd = box(*UV_BOXES["body"])
        # Central royal-navy checkered chest crest on torso front
        bx, by, bw, bh = bd["front"]
        _paint_quilted_navy(img, (bx + 2, by + 2, 6, 6), gold_border=True)
        # Gold collar trim
        for lx in range(1, 9):
            px[bx + lx, by] = _rgba(GOLD[2])
        # Ornate gold spine + diagonal filigree ribs on backplate (matching back 3/4 concept inset!)
        kx, ky, kw, _ = bd["back"]
        for lx in range(kw):
            px[kx + lx, ky] = _rgba(GOLD[2])
            if lx in (0, kw - 1):
                for ly in range(bh):
                    px[kx + lx, ky + ly] = _rgba(GOLD[1])
        for ly in range(1, 8):
            px[kx + 4, ky + ly] = _rgba(GOLD[3])
            px[kx + 5, ky + ly] = _rgba(GOLD[2])
            if ly in (2, 4, 6):
                px[kx + 2, ky + ly] = _rgba(GOLD[2])
                px[kx + 3, ky + ly] = _rgba(GOLD[1])
                px[kx + 6, ky + ly] = _rgba(GOLD[1])
                px[kx + 7, ky + ly] = _rgba(GOLD[2])
        # Leather belt wrap + lower gold/navy fauld hem around whole waist
        for side in ("front", "back", "right", "left"):
            sx, sy, sw, _ = bd[side]
            for lx in range(sw):
                px[sx + lx, sy + 8] = _rgba(LEATHER[2 if lx % 3 == 0 else 1])
                px[sx + lx, sy + 9] = _rgba(LEATHER[0])
                px[sx + lx, sy + 10] = _rgba(NAVY[1])
                px[sx + lx, sy + 11] = _rgba(GOLD[2] if lx % 2 == 0 else GOLD[1])

        # Front & Back Tabards (royal navy checkered with crisp 1px gold trim)
        _paint_steel_box(img, "tabard", fluted=False, seed=210)
        tb = box(*UV_BOXES["tabard"])
        _paint_quilted_navy(img, tb["front"], gold_border=True)
        _paint_quilted_navy(img, tb["back"], gold_border=True)

        # Layered Hip Tassets (steel outer + thick 1px gold trim)
        _paint_steel_box(img, "tasset", fluted=True, gold_bottom=True, seed=211)
        ts = box(*UV_BOXES["tasset"])
        for side in ("right", "left", "front", "back"):
            sx, sy, sw, sh = ts[side]
            for lx in range(sw):
                px[sx + lx, sy] = _rgba(GOLD[2])
                px[sx + lx, sy + 3] = _rgba(GOLD[1])
            for ly in range(sh):
                px[sx, sy + ly] = _rgba(GOLD[2])
                px[sx + sw - 1, sy + ly] = _rgba(GOLD[1])

        # Armored Cape (rich royal-navy checkered & gold-trimmed on BOTH faces so Shield-Ram / Impact pop clearly)
        _paint_steel_box(img, "cape", fluted=False, seed=212)
        cp = box(*UV_BOXES["cape"])
        _paint_quilted_navy(img, cp["front"], gold_border=True)
        _paint_quilted_navy(img, cp["back"], gold_border=True)

        # 3. Cathedral Pauldrons & Articulated Limbs
        for pkey, bband in (("pauldron_outer", 3), ("pauldron_ridge", 0)):
            _paint_steel_box(img, pkey, fluted=True, gold_bottom=True, seed=301)
            pb = box(*UV_BOXES[pkey])
            for side in ("front", "back", "right", "left"):
                sx, sy, sw, sh = pb[side]
                for lx in range(sw):
                    px[sx + lx, sy] = _rgba(GOLD[3])
                    if bband and bband < sh:
                        px[sx + lx, sy + bband] = _rgba(GOLD[2])
                for ly in range(sh):
                    px[sx, sy + ly] = _rgba(GOLD[2])
                    px[sx + sw - 1, sy + ly] = _rgba(GOLD[1])
        # Highlight golden pauldron ridge top
        pr = box(*UV_BOXES["pauldron_ridge"])
        for side in ("top", "front", "back"):
            _fill_rect(img, pr[side], _rgba(GOLD[2]))

        for key in ("upper_arm", "fore_arm", "thigh", "shin"):
            _paint_steel_box(img, key, fluted=True, gold_bottom=True, seed=310)
        # Elbow / gauntlet / greave gold bands
        fa = box(*UV_BOXES["fore_arm"])
        for side in ("front", "back", "right", "left"):
            sx, sy, sw, _ = fa[side]
            for lx in range(sw):
                px[sx + lx, sy + 1] = _rgba(GOLD[2])
                px[sx + lx, sy + 5] = _rgba(GOLD[3] if lx % 2 == 0 else GOLD[1])
        _paint_steel_box(img, "kneecap", fluted=False, gold_bottom=True, seed=315)
        kf = box(*UV_BOXES["kneecap"])["front"]
        for lx in range(kf[2]):
            px[kf[0] + lx, kf[1]] = _rgba(GOLD[3])
        for ly in range(kf[3]):
            px[kf[0], kf[1] + ly] = _rgba(GOLD[2])
            px[kf[0] + kf[2] - 1, kf[1] + ly] = _rgba(GOLD[1])
        _paint_steel_box(img, "sabaton_toe", fluted=False, gold_bottom=True, seed=316)

        # 4. Sun-Flanged Greatmace
        _paint_steel_box(img, "mace_shaft", fluted=False, seed=401)
        ms = box(*UV_BOXES["mace_shaft"])
        for side in ("top", "bottom", "right", "left"):
            sx, sy, sw, sh = ms[side]
            for ly in range(sh):
                for lx in range(sw):
                    if (lx + ly) % 3 == 0:
                        px[sx + lx, sy + ly] = _rgba(LEATHER[1])
                    if ly < 2:
                        px[sx + lx, sy + ly] = _rgba(GOLD[2])
        _paint_gold_box(img, "mace_pommel", seed=402)
        _paint_gold_box(img, "mace_finial", seed=403)
        for mkey, tone_hot in (("mace_core", AMBER[3]), ("mace_flange_x", AMBER[2]), ("mace_flange_y", AMBER[2])):
            mb = box(*UV_BOXES[mkey])
            for face, area in mb.items():
                x0, y0, w, h = area
                if w <= 0 or h <= 0:
                    continue
                for ly in range(h):
                    for lx in range(w):
                        rim = (lx == 0 or ly == 0 or lx == w - 1 or ly == h - 1)
                        col = AMBER[1] if rim else tone_hot
                        if (lx + ly) % 3 == 0 and not rim:
                            col = AMBER[3]
                        px[x0 + lx, y0 + ly] = _rgba(col)

        # 5. Towering Sun-Anvil Greatshield
        for skey in ("shield_upper", "shield_step1", "shield_step2", "shield_step3"):
            _paint_steel_box(img, skey, fluted=False, gold_bottom=False, seed=501)
            sb = box(*UV_BOXES[skey])
            # All thin perimeter rims in glowing antique gold
            for rim_face in ("top", "bottom", "front", "back"):
                _fill_rect(img, sb[rim_face], _rgba(GOLD[2] if rim_face in ("top", "front") else GOLD[1]))
            # Inner (-X "right") backing with leather grip straps matching back 3/4 view!
            ix, iy, iw, ih = sb["right"]
            for ly in range(ih):
                for lx in range(iw):
                    col = STEEL[0]
                    if lx == 0 or lx == iw - 1 or (skey == "shield_upper" and ly == 0) or (skey == "shield_step3" and ly == ih - 1):
                        col = GOLD[1]
                    elif skey == "shield_upper" and (abs(lx - ly + 2) <= 1 or abs((iw - 1 - lx) - ly + 2) <= 1) and 4 <= ly <= 12:
                        col = LEATHER[1 if ly % 2 == 0 else 2]
                    px[ix + lx, iy + ly] = _rgba(col)
            # Outer (+X "left") front face: obsidian field, crisp gold outer trim, central spine & runes
            ox, oy, ow, oh = sb["left"]
            for ly in range(oh):
                for lx in range(ow):
                    col = STEEL[1] if (lx + ly) % 4 else STEEL[0]
                    if lx == 0 or lx == ow - 1 or (skey == "shield_upper" and ly == 0) or (skey == "shield_step3" and ly == oh - 1):
                        col = GOLD[3] if lx == 0 or ly == 0 else GOLD[2]
                    elif lx in (ow // 2 - 1, ow // 2):
                        col = GOLD[2] if ly % 3 else GOLD[3]
                    px[ox + lx, oy + ly] = _rgba(col)

        # Stepped runes on shield_upper outer face (left / +X)
        sox, soy, _, _ = box(*UV_BOXES["shield_upper"])["left"]
        for rx, ry in ((3, 2), (2, 3), (3, 3), (8, 2), (9, 2), (8, 3), (3, 12), (3, 13), (8, 12), (9, 13)):
            px[sox + rx, soy + ry] = _rgba(AMBER[2])

        # Embossed 3D Sun-Anvil Crest on shield_emblem (+X outer face cutaway alpha=0 around emblem)
        em = box(*UV_BOXES["shield_emblem"])["left"]
        pal = {"G": GOLD[2], "Y": GOLD[3], "A": GOLD[3]}
        for ly, row in enumerate(_shield_emblem_pattern()):
            for lx, ch in enumerate(row):
                if ch in pal:
                    px[em[0] + lx, em[1] + ly] = _rgba(pal[ch])

        _notch_flanges(img)
        return img

    # ---- GLOW LAYER (emissive white/amber pixels rendered via RenderTypes.eyes) ----
    # 1. Greathelm glowing amber T-slit visor
    hf_x, hf_y, _, _ = box(*UV_BOXES["head"])["front"]
    for ex, ey in ((1, 3), (2, 3), (5, 3), (6, 3)):
        px[hf_x + ex, hf_y + ey] = (*AMBER[3], 255)
    for ex, ey in ((3, 4), (4, 4), (3, 5), (4, 5), (3, 6), (4, 6)):
        px[hf_x + ex, hf_y + ey] = (*AMBER[2], 210)

    # 2. Radiant Sun-Flanged Greatmace head
    for mkey, alpha in (("mace_core", 255), ("mace_flange_x", 240), ("mace_flange_y", 240), ("mace_finial", 220)):
        for face, area in box(*UV_BOXES[mkey]).items():
            x0, y0, w, h = area
            for ly in range(h):
                for lx in range(w):
                    bright = AMBER[3] if mkey == "mace_core" else AMBER[2]
                    px[x0 + lx, y0 + ly] = (*bright, alpha)

    # Cutout serrated sunburst corners on flange ends (matching concept mace silhouette)
    _notch_flanges(img)

    # 3. Glowing Shield Runes & Sun-Anvil crest highlight
    sox, soy, _, _ = box(*UV_BOXES["shield_upper"])["left"]
    for rx, ry in ((3, 2), (2, 3), (3, 3), (8, 2), (9, 2), (8, 3), (3, 12), (3, 13), (8, 12), (9, 13)):
        px[sox + rx, soy + ry] = (*AMBER[2], 235)
    em = box(*UV_BOXES["shield_emblem"])["left"]
    for ly, row in enumerate(_shield_emblem_pattern()):
        for lx, ch in enumerate(row):
            if ch in ("Y", "A"):
                px[em[0] + lx, em[1] + ly] = (*AMBER[2], 200 if ch == "A" else 245)

    # 4. Double-sided Hexagonal Aura Barrier panels in front of shield
    hex_img = _hex_barrier_rgba(glow=True)
    sa = box(*UV_BOXES["shield_aura"])
    for side in ("right", "left"):
        x0, y0, w, h = sa[side]
        if w == 16 and h == 16:
            img.paste(hex_img, (x0, y0))

    return img


def bastion_spawn_egg() -> Image.Image:
    EGG = """
        ................
        ................
        ......oooo......
        ....oo1111oo....
        ...o11111111o...
        ..o1111111111o..
        ..o1111111111o..
        .o111111111111o.
        .o111111111111o.
        .o111111111111o.
        .o111111111111o.
        ..o1111111111o..
        ..o1111111111o..
        ...o11111111o...
        ....oooooooo....
        ................
    """
    MOTIF = """
        ................
        ................
        .....g.gg.g.....
        ......gggg......
        .....SSSSSS.....
        .....SvvvvS.....
        .....SSSSSS.....
        ....gSSSSSSg....
        .....SSggSS.....
        ......gggg......
        .....SSSSSS.....
        ................
    """
    pal = {
        "g": hexc("#F2C24B"),
        "S": hexc("#353F56"),
        "v": hexc("#FFA626"),
    }
    cv = Canvas()
    dark, mid, light = hexc("#1B202C"), hexc("#2E374C"), hexc("#4A5674")
    edge = mix(dark, (0, 0, 0), 0.55)
    for y, row in enumerate(r.strip() for r in EGG.strip("\n").splitlines()):
        for x, ch in enumerate(row):
            if ch == "o":
                cv.put(x, y, edge)
            elif ch == "1":
                t = (x + y) / 26.0
                cv.put(x, y, light if t < 0.4 else mid if t < 0.72 else dark)
    for y, row in enumerate(r.strip() for r in MOTIF.strip("\n").splitlines()):
        for x, ch in enumerate(row):
            if ch in pal and cv.get(x, y) is not None:
                cv.put(x, y, pal[ch])
    return cv.image()


BASTION_LOOT = {
    "type": "minecraft:entity",
    "pools": [
        {
            "rolls": {"type": "minecraft:uniform", "min": 2, "max": 3},
            "entries": [
                {
                    "type": "minecraft:item",
                    "name": "wildercord:manual_page",
                    "weight": 3,
                    "functions": [
                        {"function": "minecraft:set_components", "components": {"wildercord:breathing_method": "iron"}},
                    ],
                },
                {
                    "type": "minecraft:item",
                    "name": "wildercord:manual_page",
                    "weight": 2,
                    "functions": [
                        {"function": "minecraft:set_components", "components": {"wildercord:breathing_method": "stone"}},
                    ],
                },
            ],
        },
        {
            "rolls": 1,
            "entries": [
                {
                    "type": "minecraft:item",
                    "name": "wildercord:aura_shard",
                    "functions": [
                        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2, "max": 3}},
                        {
                            "function": "minecraft:enchanted_count_increase",
                            "enchantment": "minecraft:looting",
                            "count": {"type": "minecraft:uniform", "min": 0.0, "max": 1.0},
                        },
                    ],
                }
            ],
        },
        {
            "rolls": 1,
            "entries": [
                {
                    "type": "minecraft:item",
                    "name": "minecraft:gold_ingot",
                    "functions": [
                        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2, "max": 5}},
                    ],
                }
            ],
        },
        {
            "rolls": 1,
            "entries": [
                {
                    "type": "minecraft:item",
                    "name": "minecraft:iron_ingot",
                    "functions": [
                        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 3, "max": 6}},
                    ],
                }
            ],
        },
        {
            "rolls": 1,
            "entries": [
                {
                    "type": "minecraft:item",
                    "name": "minecraft:diamond",
                    "functions": [
                        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}},
                    ],
                }
            ],
        },
    ],
    "random_sequence": "wildercord:entities/oathbound_bastion",
}


# ---------------------------------------------------------------------------
# Knight #2: Oathbound Blade-Champion (Way of the Blade)
# ---------------------------------------------------------------------------

BLADE_CHAMPION_UV_BOXES = {
    "head": (0, 0, 8, 8, 8),
    "visor_peak": (32, 0, 6, 3, 2),
    "plume_base": (48, 0, 3, 4, 7),
    "plume_tip": (68, 0, 3, 4, 7),
    "body": (0, 18, 9, 12, 5),
    "chest_crest": (28, 18, 6, 5, 1),
    "sash_clasp": (28, 24, 3, 3, 1),
    "gorget": (42, 18, 8, 2, 6),
    "tabard_front": (36, 26, 5, 8, 1),
    "tabard_back": (48, 26, 6, 8, 1),
    "tasset": (62, 26, 2, 6, 5),
    "cape_yoke": (76, 20, 10, 6, 1),
    "cape_tail": (76, 28, 5, 13, 1),
    "cape_tip": (88, 28, 3, 3, 1),
    "upper_arm": (0, 38, 4, 6, 4),
    "pauldron": (16, 38, 6, 5, 6),
    "haute": (40, 38, 2, 3, 5),
    "fore_arm": (0, 48, 4, 7, 4),
    "couter": (16, 49, 2, 3, 2),
    "thigh": (54, 38, 4, 6, 4),
    "shin": (54, 48, 4, 6, 5),
    "knee": (72, 48, 4, 3, 2),
    "sword_grip": (0, 62, 2, 8, 2),
    "sword_pommel": (8, 62, 3, 3, 3),
    "sword_guard": (20, 62, 9, 2, 3),
    "sword_quillon": (44, 62, 2, 2, 2),
    "sword_ricasso": (52, 62, 3, 5, 2),
    "sword_blade": (0, 72, 3, 18, 1),
    "sword_tip": (8, 72, 2, 4, 1),
    "sword_star_xy": (20, 72, 12, 12, 0),
    "sword_star_zy": (44, 72, 0, 12, 12),
}

SILVER = ((72, 78, 92), (110, 118, 134), (154, 164, 182), (196, 206, 222), (228, 236, 248))
CHARCOAL = ((26, 30, 40), (42, 48, 60), (60, 68, 82))
CRIMSON = ((96, 14, 20), (148, 22, 30), (196, 36, 44), (232, 68, 72))
CREAM = ((156, 146, 126), (198, 186, 162), (228, 218, 194), (246, 240, 224))
CRIMSON_GLOW = ((150, 14, 18), (218, 24, 28), (255, 42, 34), (255, 86, 58))


def _paint_ramp_box(
    img: Image.Image,
    boxes: dict[str, tuple[int, int, int, int, int]],
    key: str,
    ramp: tuple[tuple[int, int, int], ...],
    fluted: bool = True,
    gold_bottom: bool = False,
    seed: int = 31,
) -> None:
    px = img.load()
    for face, area in box(*boxes[key]).items():
        x0, y0, w, h = area
        bias = FACE_SHADE.get(face, 0)
        for ly in range(h):
            for lx in range(w):
                if gold_bottom and ly == h - 1 and face in ("front", "back", "left", "right"):
                    px[x0 + lx, y0 + ly] = _rgba(_tone(GOLD, 3 if lx % 2 == 0 else 2))
                    continue
                if lx == 0 or ly == 0 or lx == w - 1 or ly == h - 1:
                    idx = 3 + bias if (lx == 0 or ly == 0) else 1
                else:
                    flute = 1 if (fluted and (lx + ly * 0) % 2 == 0) else 0
                    jitter = 1 if noise(x0 + lx, y0 + ly, seed) > 0.65 else (-1 if noise(lx, ly, seed + 5) < 0.2 else 0)
                    idx = 2 + bias + flute + jitter
                px[x0 + lx, y0 + ly] = _rgba(_tone(ramp, idx))


def _riposte_star_rgba(size: int = 12) -> Image.Image:
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = im.load()
    c = (size - 1) / 2.0
    for y in range(size):
        for x in range(size):
            dx = abs(x - c)
            dy = abs(y - c)
            cross = min(dx, dy)
            diag = abs(dx - dy)
            dist = math.hypot(dx, dy)
            if dist <= 1.25:
                px[x, y] = (*AMBER[3], 255)
            elif cross <= 0.75 and dist <= 5.5:
                fade = max(0.0, 1.0 - dist / 6.0)
                px[x, y] = (*_tone(GOLD, 4 if dist < 3.0 else 3), int(235 * fade))
            elif diag <= 0.75 and dist <= 4.3:
                fade = max(0.0, 1.0 - dist / 4.8)
                px[x, y] = (*_tone(CRIMSON_GLOW, 2 if dist < 2.6 else 1), int(215 * fade))
    return im


def blade_champion_skin(glow: bool = False) -> Image.Image:
    _assert_no_overlap(BLADE_CHAMPION_UV_BOXES)
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    px = img.load()

    if not glow:
        # 1. Head / Armet Helm, pointed visor prow, flowing 2-segment crimson plume
        _paint_ramp_box(img, BLADE_CHAMPION_UV_BOXES, "head", SILVER, fluted=True, gold_bottom=True, seed=101)
        hf_x, hf_y, _, _ = box(*BLADE_CHAMPION_UV_BOXES["head"])["front"]
        # Gold brow ridge & crimson T-visor recess on helm front
        for lx in range(8):
            px[hf_x + lx, hf_y + 1] = _rgba(GOLD[3] if lx % 2 == 0 else GOLD[2])
        for ex, ey in ((1, 3), (2, 3), (5, 3), (6, 3), (3, 4), (4, 4)):
            px[hf_x + ex, hf_y + ey] = (*CRIMSON_GLOW[1], 255)
        for side in ("left", "right"):
            sx, sy, _, _ = box(*BLADE_CHAMPION_UV_BOXES["head"])[side]
            for ox, oy in ((3, 3), (4, 3), (3, 4), (4, 4)):
                px[sx + ox, sy + oy] = _rgba(GOLD[3])

        _paint_ramp_box(img, BLADE_CHAMPION_UV_BOXES, "visor_peak", SILVER, fluted=True, gold_bottom=True, seed=103)
        vx, vy, _, _ = box(*BLADE_CHAMPION_UV_BOXES["visor_peak"])["front"]
        for ex in (1, 2, 3, 4):
            px[vx + ex, vy + 0] = (*CRIMSON_GLOW[2], 255)
        px[vx + 2, vy + 1] = (*CRIMSON_GLOW[1], 255)
        px[vx + 3, vy + 1] = (*CRIMSON_GLOW[1], 255)

        for pkey, pseed in (("plume_base", 107), ("plume_tip", 109)):
            _paint_ramp_box(img, BLADE_CHAMPION_UV_BOXES, pkey, CRIMSON, fluted=True, gold_bottom=False, seed=pseed)
        # Gold plume mount collar
        pbx, pby, pbw, pbh = box(*BLADE_CHAMPION_UV_BOXES["plume_base"])["front"]
        for ly in range(pbh):
            for lx in range(pbw):
                if ly >= pbh - 2:
                    px[pbx + lx, pby + ly] = _rgba(GOLD[3])

        # 2. Fluted Silver Cuirass + Crimson Diagonal Chest Sash + Studded Belt + Gold Medallion
        _paint_ramp_box(img, BLADE_CHAMPION_UV_BOXES, "body", SILVER, fluted=True, gold_bottom=False, seed=113)
        for face in ("front", "back"):
            bx0, by0, bw, bh = box(*BLADE_CHAMPION_UV_BOXES["body"])[face]
            for ly in range(bh):
                for lx in range(bw):
                    # Gunmetal chainmail lower waist strip before belt
                    if ly in (8, 9):
                        px[bx0 + lx, by0 + ly] = _rgba(CHARCOAL[1 if (lx + ly) % 2 == 0 else 0])
                    # Leather & gold belt
                    elif ly in (10, 11):
                        px[bx0 + lx, by0 + ly] = _rgba(GOLD[2] if ly == 10 and lx % 3 == 0 else LEATHER[1])
                    # Diagonal crimson cloth & gold sash running right-shoulder (lx=0..2,ly=0) -> left-hip (lx=7..8,ly=9)
                    diag = lx - int(round((ly / 9.0) * (bw - 1)))
                    if ly <= 9 and abs(diag) <= 1:
                        px[bx0 + lx, by0 + ly] = _rgba(GOLD[3] if abs(diag) == 1 and ly % 3 == 0 else CRIMSON[2 if diag == 0 else 1])

        _paint_ramp_box(img, BLADE_CHAMPION_UV_BOXES, "chest_crest", SILVER, fluted=True, gold_bottom=True, seed=115)
        cx0, cy0, _, _ = box(*BLADE_CHAMPION_UV_BOXES["chest_crest"])["front"]
        for ox, oy in ((2, 1), (3, 1), (2, 2), (3, 2)):
            px[cx0 + ox, cy0 + oy] = _rgba(GOLD[3])
        _paint_ramp_box(img, BLADE_CHAMPION_UV_BOXES, "sash_clasp", GOLD, fluted=False, gold_bottom=False, seed=117)
        _paint_ramp_box(img, BLADE_CHAMPION_UV_BOXES, "gorget", SILVER, fluted=True, gold_bottom=True, seed=119)

        # 3. Waist Tabards (crimson w/ gold border) & Fluted Thigh Tassets
        for tkey, tseed in (("tabard_front", 121), ("tabard_back", 123)):
            _paint_ramp_box(img, BLADE_CHAMPION_UV_BOXES, tkey, CRIMSON, fluted=True, gold_bottom=True, seed=tseed)
            for face in ("front", "back"):
                tx0, ty0, tw, th = box(*BLADE_CHAMPION_UV_BOXES[tkey])[face]
                for ly in range(th):
                    for lx in range(tw):
                        if lx == 0 or lx == tw - 1 or ly == th - 1:
                            px[tx0 + lx, ty0 + ly] = _rgba(GOLD[3] if ly % 2 == 0 else GOLD[2])
                        elif tw >= 5 and lx in (tw // 2 - 1, tw // 2) and 1 <= ly <= 4:
                            px[tx0 + lx, ty0 + ly] = _rgba(GOLD[2])
        _paint_ramp_box(img, BLADE_CHAMPION_UV_BOXES, "tasset", SILVER, fluted=True, gold_bottom=True, seed=125)

        # 4. Split Dual-Tail Banner Cape (crimson yoke -> cream parchment tails w/ crimson + gold trim)
        _paint_ramp_box(img, BLADE_CHAMPION_UV_BOXES, "cape_yoke", CRIMSON, fluted=True, gold_bottom=True, seed=127)
        for ckey, cseed in (("cape_tail", 131), ("cape_tip", 133)):
            _paint_ramp_box(img, BLADE_CHAMPION_UV_BOXES, ckey, CREAM, fluted=True, gold_bottom=False, seed=cseed)
            for face in ("front", "back"):
                cx0, cy0, cw, ch = box(*BLADE_CHAMPION_UV_BOXES[ckey])[face]
                for ly in range(ch):
                    for lx in range(cw):
                        if face == "front":
                            # Underside is deep crimson w/ gold edge
                            px[cx0 + lx, cy0 + ly] = _rgba(GOLD[2] if (lx == 0 or lx == cw - 1) else CRIMSON[1 if ly % 2 == 0 else 0])
                        elif lx == 0 or lx == cw - 1:
                            px[cx0 + lx, cy0 + ly] = _rgba(GOLD[3] if ly % 2 == 0 else CRIMSON[2])
                        elif ch >= 6 and ly == ch - 1:
                            px[cx0 + lx, cy0 + ly] = _rgba(CRIMSON[2])

        # 5. Arms, Haute-Piece Pauldrons, Elbow Wings, Gauntlets, Greaves & Knees
        _paint_ramp_box(img, BLADE_CHAMPION_UV_BOXES, "upper_arm", CHARCOAL, fluted=False, gold_bottom=False, seed=137)
        _paint_ramp_box(img, BLADE_CHAMPION_UV_BOXES, "pauldron", SILVER, fluted=True, gold_bottom=True, seed=139)
        _paint_ramp_box(img, BLADE_CHAMPION_UV_BOXES, "haute", SILVER, fluted=False, gold_bottom=False, seed=141)
        # Gold edge on top of haute piece
        hx0, hy0, hw, _ = box(*BLADE_CHAMPION_UV_BOXES["haute"])["top"]
        for lx in range(hw):
            px[hx0 + lx, hy0] = _rgba(GOLD[3])

        _paint_ramp_box(img, BLADE_CHAMPION_UV_BOXES, "fore_arm", SILVER, fluted=True, gold_bottom=True, seed=143)
        for face in ("front", "back", "left", "right"):
            fx0, fy0, fw, fh = box(*BLADE_CHAMPION_UV_BOXES["fore_arm"])[face]
            for ly in range(max(0, fh - 2), fh):
                for lx in range(fw):
                    px[fx0 + lx, fy0 + ly] = _rgba(CRIMSON[1] if ly == fh - 2 else GOLD[2])
        _paint_ramp_box(img, BLADE_CHAMPION_UV_BOXES, "couter", SILVER, fluted=False, gold_bottom=True, seed=145)

        _paint_ramp_box(img, BLADE_CHAMPION_UV_BOXES, "thigh", CHARCOAL, fluted=False, gold_bottom=False, seed=147)
        _paint_ramp_box(img, BLADE_CHAMPION_UV_BOXES, "shin", SILVER, fluted=True, gold_bottom=True, seed=149)
        _paint_ramp_box(img, BLADE_CHAMPION_UV_BOXES, "knee", GOLD, fluted=False, gold_bottom=False, seed=151)

        # 6. Ornate Zweihänder Greatsword (gold pommel, leather grip, swept silver-gold guard, glowing rune blade)
        _paint_ramp_box(img, BLADE_CHAMPION_UV_BOXES, "sword_grip", LEATHER, fluted=False, gold_bottom=False, seed=153)
        _paint_ramp_box(img, BLADE_CHAMPION_UV_BOXES, "sword_pommel", GOLD, fluted=False, gold_bottom=False, seed=155)
        _paint_ramp_box(img, BLADE_CHAMPION_UV_BOXES, "sword_guard", GOLD, fluted=False, gold_bottom=False, seed=157)
        _paint_ramp_box(img, BLADE_CHAMPION_UV_BOXES, "sword_quillon", GOLD, fluted=False, gold_bottom=False, seed=159)
        _paint_ramp_box(img, BLADE_CHAMPION_UV_BOXES, "sword_ricasso", LEATHER, fluted=False, gold_bottom=True, seed=161)
        _paint_ramp_box(img, BLADE_CHAMPION_UV_BOXES, "sword_blade", SILVER, fluted=False, gold_bottom=False, seed=163)
        _paint_ramp_box(img, BLADE_CHAMPION_UV_BOXES, "sword_tip", SILVER, fluted=False, gold_bottom=False, seed=165)

        for face in ("front", "back"):
            bx0, by0, bw, bh = box(*BLADE_CHAMPION_UV_BOXES["sword_blade"])[face]
            for ly in range(bh):
                px[bx0 + 0, by0 + ly] = _rgba(SILVER[4])
                px[bx0 + 2, by0 + ly] = _rgba(SILVER[3])
                if 2 <= ly <= bh - 3 and ly % 3 != 0:
                    px[bx0 + 1, by0 + ly] = (*CRIMSON_GLOW[2], 255)
                else:
                    px[bx0 + 1, by0 + ly] = _rgba(CHARCOAL[1])

        star_img = _riposte_star_rgba(12)
        for skey in ("sword_star_xy", "sword_star_zy"):
            for _, (x0, y0, w, h) in box(*BLADE_CHAMPION_UV_BOXES[skey]).items():
                if w == 12 and h == 12:
                    img.paste(star_img, (x0, y0))
        return img

    # ---- GLOW LAYER (emissive crimson T-visor, Zweihänder Aura runes, Riposte flare) ----
    hf_x, hf_y, _, _ = box(*BLADE_CHAMPION_UV_BOXES["head"])["front"]
    for ex, ey in ((1, 3), (2, 3), (5, 3), (6, 3)):
        px[hf_x + ex, hf_y + ey] = (*CRIMSON_GLOW[3], 255)
    for ex, ey in ((3, 4), (4, 4)):
        px[hf_x + ex, hf_y + ey] = (*CRIMSON_GLOW[2], 230)

    vx, vy, _, _ = box(*BLADE_CHAMPION_UV_BOXES["visor_peak"])["front"]
    for ex in (1, 2, 3, 4):
        px[vx + ex, vy] = (*CRIMSON_GLOW[3], 255)
    px[vx + 2, vy + 1] = (*CRIMSON_GLOW[2], 215)
    px[vx + 3, vy + 1] = (*CRIMSON_GLOW[2], 215)

    for face in ("front", "back"):
        bx0, by0, _, bh = box(*BLADE_CHAMPION_UV_BOXES["sword_blade"])[face]
        for ly in range(2, bh - 2):
            if ly % 3 != 0:
                px[bx0 + 1, by0 + ly] = (*CRIMSON_GLOW[3], 255)

    star_img = _riposte_star_rgba(12)
    for skey in ("sword_star_xy", "sword_star_zy"):
        for _, (x0, y0, w, h) in box(*BLADE_CHAMPION_UV_BOXES[skey]).items():
            if w == 12 and h == 12:
                img.paste(star_img, (x0, y0))
    return img


def blade_champion_spawn_egg() -> Image.Image:
    EGG = """
        ................
        ................
        ......oooo......
        ....oo1111oo....
        ...o11111111o...
        ..o1111111111o..
        ..o1111111111o..
        .o111111111111o.
        .o111111111111o.
        .o111111111111o.
        .o111111111111o.
        ..o1111111111o..
        ..o1111111111o..
        ...o11111111o...
        ....oooooooo....
        ................
    """
    MOTIF = """
        ................
        ................
        ......rrrr......
        .....rrrrrr.....
        .....SSSSSS.....
        .....SvvrvS.....
        .....SSSSSS.....
        ....g.rgg.g.....
        ......rg........
        .....grS........
        ......gS........
        ......gS........
        .......S........
        ................
    """
    pal = {
        "g": hexc("#F2C24B"),
        "S": hexc("#C4CEDE"),
        "r": hexc("#D62830"),
        "v": hexc("#FF5038"),
    }
    cv = Canvas()
    dark, mid, light = hexc("#5B6374"), hexc("#8F9AAC"), hexc("#C4CEDE")
    edge = mix(dark, (0, 0, 0), 0.55)
    for y, row in enumerate(r.strip() for r in EGG.strip("\n").splitlines()):
        for x, ch in enumerate(row):
            if ch == "o":
                cv.put(x, y, edge)
            elif ch == "1":
                t = (x + y) / 26.0
                cv.put(x, y, light if t < 0.4 else mid if t < 0.72 else dark)
    for y, row in enumerate(r.strip() for r in MOTIF.strip("\n").splitlines()):
        for x, ch in enumerate(row):
            if ch in pal and cv.get(x, y) is not None:
                cv.put(x, y, pal[ch])
    return cv.image()


BLADE_CHAMPION_LOOT = {
    "type": "minecraft:entity",
    "pools": [
        {
            "rolls": {"type": "minecraft:uniform", "min": 2, "max": 3},
            "entries": [
                {
                    "type": "minecraft:item",
                    "name": "wildercord:manual_page",
                    "weight": 3,
                    "functions": [
                        {"function": "minecraft:set_components", "components": {"wildercord:breathing_method": "crimson"}},
                    ],
                },
                {
                    "type": "minecraft:item",
                    "name": "wildercord:manual_page",
                    "weight": 2,
                    "functions": [
                        {"function": "minecraft:set_components", "components": {"wildercord:breathing_method": "ember"}},
                    ],
                },
            ],
        },
        {
            "rolls": 1,
            "entries": [
                {
                    "type": "minecraft:item",
                    "name": "wildercord:aura_shard",
                    "functions": [
                        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2, "max": 4}},
                        {
                            "function": "minecraft:enchanted_count_increase",
                            "enchantment": "minecraft:looting",
                            "count": {"type": "minecraft:uniform", "min": 0.0, "max": 1.0},
                        },
                    ],
                }
            ],
        },
        {
            "rolls": 1,
            "entries": [
                {
                    "type": "minecraft:item",
                    "name": "minecraft:gold_ingot",
                    "functions": [
                        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2, "max": 5}},
                    ],
                }
            ],
        },
        {
            "rolls": 1,
            "entries": [
                {
                    "type": "minecraft:item",
                    "name": "minecraft:iron_ingot",
                    "functions": [
                        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 3, "max": 6}},
                    ],
                }
            ],
        },
        {
            "rolls": 1,
            "entries": [
                {
                    "type": "minecraft:item",
                    "name": "minecraft:diamond",
                    "functions": [
                        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}},
                    ],
                }
            ],
        },
    ],
    "random_sequence": "wildercord:entities/oathbound_blade_champion",
}


def write(g) -> None:
    g.save(bastion_skin(glow=False), g.ASSETS / "textures/entity/oathbound_bastion/skin.png")
    g.save(bastion_skin(glow=True), g.ASSETS / "textures/entity/oathbound_bastion/glow.png")
    egg = "oathbound_bastion_spawn_egg"
    g.save(bastion_spawn_egg(), g.ASSETS / f"textures/item/{egg}.png")
    g.item_model(egg, egg)
    g.write_json(
        g.ASSETS / f"items/{egg}.json",
        {"model": {"type": "minecraft:model", "model": f"wildercord:item/{egg}"}},
    )
    g.write_json(g.DATA / "loot_table/entities/oathbound_bastion.json", BASTION_LOOT)

    g.save(blade_champion_skin(glow=False), g.ASSETS / "textures/entity/oathbound_blade_champion/skin.png")
    g.save(blade_champion_skin(glow=True), g.ASSETS / "textures/entity/oathbound_blade_champion/glow.png")
    bc_egg = "oathbound_blade_champion_spawn_egg"
    g.save(blade_champion_spawn_egg(), g.ASSETS / f"textures/item/{bc_egg}.png")
    g.item_model(bc_egg, bc_egg)
    g.write_json(
        g.ASSETS / f"items/{bc_egg}.json",
        {"model": {"type": "minecraft:model", "model": f"wildercord:item/{bc_egg}"}},
    )
    g.write_json(g.DATA / "loot_table/entities/oathbound_blade_champion.json", BLADE_CHAMPION_LOOT)

    g.save(halberdier_skin(glow=False), g.ASSETS / "textures/entity/oathbound_halberdier/skin.png")
    g.save(halberdier_skin(glow=True), g.ASSETS / "textures/entity/oathbound_halberdier/glow.png")
    hb_egg = "oathbound_halberdier_spawn_egg"
    g.save(halberdier_spawn_egg(), g.ASSETS / f"textures/item/{hb_egg}.png")
    g.item_model(hb_egg, hb_egg)
    g.write_json(
        g.ASSETS / f"items/{hb_egg}.json",
        {"model": {"type": "minecraft:model", "model": f"wildercord:item/{hb_egg}"}},
    )
    g.write_json(g.DATA / "loot_table/entities/oathbound_halberdier.json", HALBERDIER_LOOT)


# ---------------------------------------------------------------------------
# Knight #3: Oathbound Halberdier (Way of the Storm-Glaive)
# ---------------------------------------------------------------------------

HALBERDIER_UV_BOXES = {
    "head": (0, 0, 8, 8, 8),
    "beak_upper": (32, 0, 4, 2, 2),
    "beak_tip": (44, 0, 2, 2, 2),
    "wing": (52, 0, 1, 5, 6),
    "body": (0, 18, 9, 12, 5),
    "cowl": (28, 18, 10, 3, 7),
    "scarf_base": (62, 18, 3, 8, 1),
    "scarf_tip": (70, 18, 3, 8, 1),
    "tabard_front": (28, 28, 5, 8, 1),
    "tabard_back": (40, 28, 5, 8, 1),
    "tasset": (52, 28, 2, 6, 5),
    "upper_arm": (0, 40, 4, 6, 4),
    "pauldron": (16, 40, 6, 5, 6),
    "fore_arm": (0, 52, 4, 7, 4),
    "thigh": (40, 40, 4, 6, 4),
    "shin": (40, 52, 4, 6, 5),
    "knee": (58, 52, 4, 3, 2),
    "halberd_shaft": (0, 66, 2, 32, 2),
    "halberd_butt": (8, 66, 3, 3, 3),
    "halberd_tassel": (20, 66, 1, 7, 2),
    "halberd_axe": (26, 66, 1, 9, 6),
    "halberd_hook": (40, 66, 1, 4, 4),
    "halberd_spear": (50, 66, 2, 8, 2),
    "halberd_star_xy": (60, 66, 12, 12, 0),
    "halberd_star_zy": (84, 66, 0, 12, 12),
}

INDIGO = ((32, 40, 64), (48, 62, 98), (72, 94, 144), (112, 138, 188), (168, 194, 232))
COBALT = ((22, 36, 82), (34, 58, 124), (50, 86, 174), (82, 128, 214))
FEATHER = ((132, 144, 166), (176, 190, 214), (214, 226, 244), (240, 246, 255))
CYAN_GLOW = ((32, 176, 206), (62, 234, 255), (144, 248, 255), (222, 255, 255))
VIOLET_GLOW = ((116, 62, 200), (164, 98, 255), (206, 148, 255))


def _storm_star_rgba(size: int = 12) -> Image.Image:
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = im.load()
    c = (size - 1) / 2.0
    for y in range(size):
        for x in range(size):
            dx = abs(x - c)
            dy = abs(y - c)
            cross = min(dx, dy)
            diag = abs(dx - dy)
            dist = math.hypot(dx, dy)
            if dist <= 1.25:
                px[x, y] = (*CYAN_GLOW[3], 255)
            elif cross <= 0.75 and dist <= 5.5:
                fade = max(0.0, 1.0 - dist / 6.0)
                px[x, y] = (*_tone(CYAN_GLOW, 2 if dist < 3.0 else 1), int(235 * fade))
            elif diag <= 0.75 and dist <= 4.3:
                fade = max(0.0, 1.0 - dist / 4.8)
                px[x, y] = (*_tone(VIOLET_GLOW, 2 if dist < 2.6 else 1), int(215 * fade))
    return im


def halberdier_skin(glow: bool = False) -> Image.Image:
    _assert_no_overlap(HALBERDIER_UV_BOXES)
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    px = img.load()

    if not glow:
        # 1. Winged falcon-beak helm & swept silver temple wings
        _paint_ramp_box(img, HALBERDIER_UV_BOXES, "head", INDIGO, fluted=True, gold_bottom=False, seed=201)
        hf_x, hf_y, _, _ = box(*HALBERDIER_UV_BOXES["head"])["front"]
        for ex, ey in ((1, 3), (2, 3), (5, 3), (6, 3)):
            px[hf_x + ex, hf_y + ey] = (*CYAN_GLOW[1], 255)
        for lx in range(8):
            px[hf_x + lx, hf_y + 0] = _rgba(FEATHER[1])
        for ly in range(8):
            px[hf_x + 3, hf_y + ly] = _rgba(FEATHER[2])
            px[hf_x + 4, hf_y + ly] = _rgba(FEATHER[2])

        _paint_ramp_box(img, HALBERDIER_UV_BOXES, "beak_upper", FEATHER, fluted=False, gold_bottom=False, seed=203)
        _paint_ramp_box(img, HALBERDIER_UV_BOXES, "beak_tip", FEATHER, fluted=False, gold_bottom=False, seed=205)

        _paint_ramp_box(img, HALBERDIER_UV_BOXES, "wing", FEATHER, fluted=False, gold_bottom=False, seed=207)
        for side in ("left", "right"):
            wx0, wy0, ww, wh = box(*HALBERDIER_UV_BOXES["wing"])[side]
            for ly in range(wh):
                for lx in range(ww):
                    if ly == 0 and lx < 2:
                        px[wx0 + lx, wy0 + ly] = (0, 0, 0, 0)
                    elif ly == wh - 1 and lx > ww - 3:
                        px[wx0 + lx, wy0 + ly] = (0, 0, 0, 0)
                    elif (ly + lx) % 2 == 0:
                        px[wx0 + lx, wy0 + ly] = _rgba(FEATHER[3])

        # 2. Gunmetal-indigo & cobalt plate cuirass, dark navy cowl, flowing fringed scarf
        _paint_ramp_box(img, HALBERDIER_UV_BOXES, "body", COBALT, fluted=True, gold_bottom=False, seed=211)
        for face in ("front", "back"):
            bx0, by0, bw, bh = box(*HALBERDIER_UV_BOXES["body"])[face]
            for ly in range(bh):
                for lx in range(bw):
                    if ly == 9:
                        px[bx0 + lx, by0 + ly] = _rgba(FEATHER[1])
                    elif ly in (10, 11):
                        px[bx0 + lx, by0 + ly] = _rgba(FEATHER[2] if (ly == 10 and lx in (bw // 2 - 1, bw // 2)) else CHARCOAL[1])
                    elif face == "front" and (lx in (bw // 2 - 1, bw // 2) or ly == 4):
                        px[bx0 + lx, by0 + ly] = _rgba(FEATHER[2])

        _paint_ramp_box(img, HALBERDIER_UV_BOXES, "cowl", CHARCOAL, fluted=False, gold_bottom=False, seed=213)
        for skey, sseed in (("scarf_base", 215), ("scarf_tip", 217)):
            _paint_ramp_box(img, HALBERDIER_UV_BOXES, skey, CHARCOAL, fluted=False, gold_bottom=False, seed=sseed)
            for face in ("front", "back"):
                sx0, sy0, sw, sh = box(*HALBERDIER_UV_BOXES[skey])[face]
                for ly in range(sh):
                    for lx in range(sw):
                        if skey == "scarf_tip" and ly >= sh - 2 and lx == 1:
                            px[sx0 + lx, sy0 + ly] = (0, 0, 0, 0)
                        elif lx == 0 or lx == sw - 1:
                            px[sx0 + lx, sy0 + ly] = _rgba(COBALT[3])
                        elif (lx + ly) % 2 == 0 and ly > 2:
                            px[sx0 + lx, sy0 + ly] = _rgba(INDIGO[2])

        for tkey, tseed in (("tabard_front", 219), ("tabard_back", 221)):
            _paint_ramp_box(img, HALBERDIER_UV_BOXES, tkey, COBALT, fluted=True, gold_bottom=False, seed=tseed)
            for face in ("front", "back"):
                tx0, ty0, tw, th = box(*HALBERDIER_UV_BOXES[tkey])[face]
                for ly in range(th):
                    for lx in range(tw):
                        if ly == th - 1 and lx in (0, tw - 1):
                            px[tx0 + lx, ty0 + ly] = (0, 0, 0, 0)
                        elif lx == 0 or lx == tw - 1:
                            px[tx0 + lx, ty0 + ly] = _rgba(FEATHER[2])
        _paint_ramp_box(img, HALBERDIER_UV_BOXES, "tasset", INDIGO, fluted=True, gold_bottom=False, seed=223)

        # 3. Pauldrons, rune-lit gauntlets & greaves, knee cops
        _paint_ramp_box(img, HALBERDIER_UV_BOXES, "upper_arm", CHARCOAL, fluted=False, gold_bottom=False, seed=225)
        _paint_ramp_box(img, HALBERDIER_UV_BOXES, "pauldron", INDIGO, fluted=True, gold_bottom=False, seed=227)
        for face in ("front", "left", "right", "back"):
            px0, py0, pw, _ = box(*HALBERDIER_UV_BOXES["pauldron"])[face]
            for lx in range(pw):
                px[px0 + lx, py0 + 0] = _rgba(FEATHER[2])

        _paint_ramp_box(img, HALBERDIER_UV_BOXES, "fore_arm", INDIGO, fluted=True, gold_bottom=False, seed=229)
        _paint_ramp_box(img, HALBERDIER_UV_BOXES, "thigh", CHARCOAL, fluted=False, gold_bottom=False, seed=231)
        _paint_ramp_box(img, HALBERDIER_UV_BOXES, "shin", INDIGO, fluted=True, gold_bottom=False, seed=233)
        _paint_ramp_box(img, HALBERDIER_UV_BOXES, "knee", FEATHER, fluted=False, gold_bottom=False, seed=235)
        for key in ("fore_arm", "shin"):
            for face in ("front", "left", "right"):
                x0, y0, w, h = box(*HALBERDIER_UV_BOXES[key])[face]
                px[x0 + 1, y0 + 2] = (*VIOLET_GLOW[1], 255)
                px[x0 + 1, y0 + 3] = (*CYAN_GLOW[1], 255)
                if h >= 5:
                    px[x0 + min(w - 1, 2), y0 + 4] = (*VIOLET_GLOW[1], 255)

        # 4. Storm-Halberd: iron shaft, glowing cyan tassel, crescent blade w/ lightning veins, rear fluke, spear point
        _paint_ramp_box(img, HALBERDIER_UV_BOXES, "halberd_shaft", CHARCOAL, fluted=False, gold_bottom=False, seed=237)
        _paint_ramp_box(img, HALBERDIER_UV_BOXES, "halberd_butt", FEATHER, fluted=False, gold_bottom=False, seed=239)
        _paint_ramp_box(img, HALBERDIER_UV_BOXES, "halberd_tassel", CYAN_GLOW, fluted=False, gold_bottom=False, seed=241)
        _paint_ramp_box(img, HALBERDIER_UV_BOXES, "halberd_spear", FEATHER, fluted=False, gold_bottom=False, seed=243)
        _paint_ramp_box(img, HALBERDIER_UV_BOXES, "halberd_hook", FEATHER, fluted=False, gold_bottom=False, seed=245)

        _paint_ramp_box(img, HALBERDIER_UV_BOXES, "halberd_axe", INDIGO, fluted=False, gold_bottom=False, seed=247)
        for side in ("left", "right"):
            ax0, ay0, aw, ah = box(*HALBERDIER_UV_BOXES["halberd_axe"])[side]
            for ly in range(ah):
                for lx in range(aw):
                    if (ly == 0 or ly == ah - 1) and lx in (0, 1):
                        px[ax0 + lx, ay0 + ly] = (0, 0, 0, 0)
                    elif lx == 0 or lx == 1:
                        px[ax0 + lx, ay0 + ly] = _rgba(FEATHER[3])
                    elif abs(ly - ah // 2) + abs(lx - 3) in (2, 3) and lx >= 2:
                        px[ax0 + lx, ay0 + ly] = (*CYAN_GLOW[2], 255)

        star_img = _storm_star_rgba(12)
        for skey in ("halberd_star_xy", "halberd_star_zy"):
            for _, (x0, y0, w, h) in box(*HALBERDIER_UV_BOXES[skey]).items():
                if w == 12 and h == 12:
                    img.paste(star_img, (x0, y0))
        return img

    # ---- GLOW LAYER (emissive cyan eyes, violet-cyan gauntlet/greave circuits, lightning axe runes, tassel & flare) ----
    hf_x, hf_y, _, _ = box(*HALBERDIER_UV_BOXES["head"])["front"]
    for ex, ey in ((1, 3), (2, 3), (5, 3), (6, 3)):
        px[hf_x + ex, hf_y + ey] = (*CYAN_GLOW[2], 255)

    for key in ("fore_arm", "shin"):
        for face in ("front", "left", "right"):
            x0, y0, w, h = box(*HALBERDIER_UV_BOXES[key])[face]
            px[x0 + 1, y0 + 2] = (*VIOLET_GLOW[2], 220)
            px[x0 + 1, y0 + 3] = (*CYAN_GLOW[2], 235)
            if h >= 5:
                px[x0 + min(w - 1, 2), y0 + 4] = (*VIOLET_GLOW[2], 220)

    tx0, ty0, tw, th = box(*HALBERDIER_UV_BOXES["halberd_tassel"])["front"]
    for ly in range(th):
        for lx in range(tw):
            px[tx0 + lx, ty0 + ly] = (*CYAN_GLOW[1], 190)

    for side in ("left", "right"):
        ax0, ay0, aw, ah = box(*HALBERDIER_UV_BOXES["halberd_axe"])[side]
        for ly in range(ah):
            for lx in range(aw):
                if (ly == 0 or ly == ah - 1) and lx in (0, 1):
                    continue
                if lx == 0 or (abs(ly - ah // 2) + abs(lx - 3) in (2, 3) and lx >= 2):
                    px[ax0 + lx, ay0 + ly] = (*CYAN_GLOW[2], 245)

    star_img = _storm_star_rgba(12)
    for skey in ("halberd_star_xy", "halberd_star_zy"):
        for _, (x0, y0, w, h) in box(*HALBERDIER_UV_BOXES[skey]).items():
            if w == 12 and h == 12:
                img.paste(star_img, (x0, y0))
    return img


def halberdier_spawn_egg() -> Image.Image:
    EGG = """
        ................
        ................
        ......oooo......
        ....oo1111oo....
        ...o11111111o...
        ..o1111111111o..
        ..o1111111111o..
        .o111111111111o.
        .o111111111111o.
        .o111111111111o.
        .o111111111111o.
        ..o1111111111o..
        ..o1111111111o..
        ...o11111111o...
        ....oooooooo....
        ................
    """
    MOTIF = """
        ................
        ................
        ...w........w...
        ...ww.bbbb.ww...
        ....wbbbbbbw....
        .....bcvvcb.....
        ......bbbb......
        .....bbssbb.....
        .......sc.......
        .......sb.......
        .......sb.......
        ................
    """
    pal = {
        "w": hexc("#D8E2F4"),
        "b": hexc("#3458A8"),
        "c": hexc("#3EEAFF"),
        "v": hexc("#9E68FF"),
        "s": hexc("#98AABF"),
    }
    cv = Canvas()
    dark, mid, light = hexc("#1E2844"), hexc("#30426C"), hexc("#4C669E")
    edge = mix(dark, (0, 0, 0), 0.55)
    for y, row in enumerate(r.strip() for r in EGG.strip("\n").splitlines()):
        for x, ch in enumerate(row):
            if ch == "o":
                cv.put(x, y, edge)
            elif ch == "1":
                t = (x + y) / 26.0
                cv.put(x, y, light if t < 0.4 else mid if t < 0.72 else dark)
    for y, row in enumerate(r.strip() for r in MOTIF.strip("\n").splitlines()):
        for x, ch in enumerate(row):
            if ch in pal and cv.get(x, y) is not None:
                cv.put(x, y, pal[ch])
    return cv.image()


HALBERDIER_LOOT = {
    "type": "minecraft:entity",
    "pools": [
        {
            "rolls": {"type": "minecraft:uniform", "min": 2, "max": 3},
            "entries": [
                {
                    "type": "minecraft:item",
                    "name": "wildercord:manual_page",
                    "weight": 3,
                    "functions": [
                        {"function": "minecraft:set_components", "components": {"wildercord:breathing_method": "thunder"}},
                    ],
                },
                {
                    "type": "minecraft:item",
                    "name": "wildercord:manual_page",
                    "weight": 2,
                    "functions": [
                        {"function": "minecraft:set_components", "components": {"wildercord:breathing_method": "gale"}},
                    ],
                },
            ],
        },
        {
            "rolls": 1,
            "entries": [
                {
                    "type": "minecraft:item",
                    "name": "wildercord:aura_shard",
                    "functions": [
                        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2, "max": 4}},
                        {
                            "function": "minecraft:enchanted_count_increase",
                            "enchantment": "minecraft:looting",
                            "count": {"type": "minecraft:uniform", "min": 0.0, "max": 1.0},
                        },
                    ],
                }
            ],
        },
        {
            "rolls": 1,
            "entries": [
                {
                    "type": "minecraft:item",
                    "name": "minecraft:iron_ingot",
                    "functions": [
                        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 3, "max": 6}},
                    ],
                }
            ],
        },
        {
            "rolls": 1,
            "entries": [
                {
                    "type": "minecraft:item",
                    "name": "minecraft:gold_ingot",
                    "functions": [
                        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2, "max": 4}},
                    ],
                }
            ],
        },
        {
            "rolls": 1,
            "entries": [
                {
                    "type": "minecraft:item",
                    "name": "minecraft:diamond",
                    "functions": [
                        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}},
                    ],
                }
            ],
        },
    ],
    "random_sequence": "wildercord:entities/oathbound_halberdier",
}
