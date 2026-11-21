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


def _assert_no_overlap() -> None:
    rects = []
    for name, (u, v, w, h, d) in UV_BOXES.items():
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
