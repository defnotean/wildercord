"""Art for Wildercord's backpacks: the three items, what they look like worn, and the Backpack slot's hint.

Like vanilla's leather armour, every backpack is drawn twice: its leather (or woven cloth) in greys, which the
game tints with the backpack's colour (its own, or a dye's), and an untinted layer over it for what isn't dyed:
brass and iron buckles, iron corners, the bedroll, gold trim and amethyst runes.

Writes (under src/main/resources/assets/wildercord/textures):
    item/<backpack>.png, item/<backpack>_overlay.png      16x16 icons (tinted layer, untinted layer)
    entity/backpack/<backpack>.png, <backpack>_overlay.png 64x32, the worn model's two layers
    gui/sprites/container/slot/backpack.png               the empty Backpack slot's hint
for <backpack> in backpack, reinforced_backpack, runewoven_backpack.

The worn textures follow the layout of client/render/BackpackModel.java (every box's six faces, unfolded the
game's way): keep the two in step. Run from the project root:  python tools/backpack_art.py
(generate_assets.py runs it too). Run it with --preview to also draw a review sheet into build/art-preview/.
"""
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/wildercord/textures"

TIERS = ["backpack", "reinforced_backpack", "runewoven_backpack"]
# Each backpack's colour before it's dyed (as in backpack/BackpackTier.java), for previews and the wiki.
COLOURS = {"backpack": 0xA06540, "reinforced_backpack": 0x8C5A36, "runewoven_backpack": 0x6048A8}

# The tinted greys, dark to light: vanilla leather's ramp, lifted a little, since the tint only darkens.
K, D, M, N, L, H = (62,) * 3, (110,) * 3, (150,) * 3, (186,) * 3, (216,) * 3, (240,) * 3
# What isn't dyed.
BRASS = [(122, 88, 32), (196, 150, 58), (240, 206, 110)]
IRON = [(70, 70, 78), (140, 140, 150), (206, 206, 214)]
GOLD = [(140, 96, 20), (222, 176, 60), (255, 236, 150)]
AMETHYST = [(84, 44, 140), (150, 96, 220), (214, 176, 255)]
WOOL = [(88, 104, 128), (130, 148, 170), (178, 192, 206)]
STRAP = [(58, 36, 22), (96, 62, 38), (128, 86, 54)]


def _img(w, h):
    return Image.new("RGBA", (w, h), (0, 0, 0, 0))


def _put(img, x, y, c):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), tuple(c) + (255,))


def _rect(img, x0, y0, x1, y1, c):
    """Fills x0..x1, y0..y1 (inclusive)."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            _put(img, x, y, c)


# ================================================================ icons (16x16)


def _panel(img, x0, y0, x1, y1, lit, face, shade, edge=K, round_top=False, round_bottom=False):
    """A leather panel: outlined, lit along its top and left, shaded along its bottom and right, its corners
    cut round where asked."""
    _rect(img, x0, y0, x1, y1, edge)
    _rect(img, x0 + 1, y0 + 1, x1 - 1, y1 - 1, face)
    _rect(img, x0 + 1, y0 + 1, x1 - 1, y0 + 1, lit)
    _rect(img, x0 + 1, y0 + 1, x0 + 1, y1 - 1, lit)
    _rect(img, x0 + 1, y1 - 1, x1 - 1, y1 - 1, shade)
    _rect(img, x1 - 1, y0 + 2, x1 - 1, y1 - 1, shade)
    for x, y, cut in ((x0, y0, round_top), (x1, y0, round_top), (x0, y1, round_bottom), (x1, y1, round_bottom)):
        if cut:
            img.putpixel((x, y), (0, 0, 0, 0))
            _put(img, x + (1 if x == x0 else -1), y + (1 if y == y0 else -1), edge)


def icon(tier):
    """The item: (tinted layer, untinted layer). A pack seen from behind: a handle, the lid and its flap, a pocket."""
    base, over = _img(16, 16), _img(16, 16)
    reinforced, runewoven = tier == "reinforced_backpack", tier == "runewoven_backpack"
    bottom = 12 if reinforced else 14
    # Side pouches (the Runewoven's), behind the bag.
    if runewoven:
        for x0 in (1, 12):
            _panel(base, x0, 7, x0 + 2, 12, H, L, N, round_bottom=True)
    # The handle loop on top.
    for x in range(6, 10):
        _put(base, x, 1, K)
    _put(base, 5, 2, K), _put(base, 10, 2, K)
    _put(base, 6, 2, N), _put(base, 9, 2, N)
    # The bag.
    _panel(base, 3, 5, 12, bottom, L, N, M, round_bottom=True)
    # The lid, a little wider, and its flap over the bag.
    _panel(base, 2, 3, 13, 6, H, H, L, round_top=True)
    _panel(base, 4, 6, 11, 10, H, L, N)
    _rect(base, 5, 11, 10, 11, M)  # the flap's shadow
    # The pocket.
    _panel(base, 5, 11, 10, bottom - 1, L, N, M)
    # Cut leather catches light unevenly. Seams and creases make the flat panels read as a sewn bag.
    for x, y, shade in ((4, 7, L), (4, 9, M), (11, 8, D), (11, 10, M),
                        (6, 4, L), (8, 4, H), (10, 4, N), (6, 7, N), (9, 7, L),
                        (6, 12, M), (9, 12, L)):
        _put(base, x, y, shade)
    for x, y in ((3, 7), (3, 10), (12, 7), (12, 10), (5, 10), (10, 10)):
        _put(over, x, y, STRAP[2])
    # The straps sit proud of the flap, with shaded leather on one side and a stitch on the other.
    for x in (5, 10):
        _put(over, x, 6, STRAP[0])
        _put(over, x, 7, STRAP[1])
        _put(over, x + (1 if x == 5 else -1), 7, STRAP[2])
    if reinforced:
        # A bedroll strapped underneath, and iron at the corners.
        _rect(over, 3, 12, 12, 15, K)
        _put(over, 2, 13, K), _put(over, 2, 14, K), _put(over, 13, 13, K), _put(over, 13, 14, K)
        _rect(over, 3, 13, 12, 13, WOOL[2])
        _rect(over, 3, 14, 12, 14, WOOL[1])
        for x in (5, 10):
            _rect(over, x, 12, x, 15, STRAP[1])
        for x, y in ((4, 11), (11, 11), (3, 4), (12, 4)):
            _put(over, x, y, IRON[2])
        _put(over, 3, 11, IRON[1]), _put(over, 12, 11, IRON[0]), _put(over, 2, 5, IRON[1]), _put(over, 13, 5, IRON[0])
    metal = GOLD if runewoven else IRON if reinforced else BRASS
    # Two buckles where the flap is strapped down.
    for x in (5, 10):
        _put(over, x, 8, metal[2])
        _put(over, x, 9, metal[1])
    if runewoven:
        # A wide gold lid binding, inset clasps and a faceted amethyst rune on the flap.
        _rect(over, 3, 6, 12, 6, GOLD[1])
        _put(over, 2, 5, GOLD[0]), _put(over, 13, 5, GOLD[0])
        for x, y, tone in ((7, 7, 2), (8, 7, 1), (6, 8, 1), (7, 8, 2),
                           (8, 8, 2), (9, 8, 0), (7, 9, 1), (8, 9, 0)):
            _put(over, x, y, AMETHYST[tone])
        _put(over, 7, 8, (245, 226, 255))
        for x, y in ((6, 12), (7, 13), (8, 12), (9, 13)):
            _put(over, x, y, AMETHYST[2])
        for x0 in (1, 12):
            _put(over, x0 + 1, 9, GOLD[2])
            _put(over, x0 + 1, 11, GOLD[1])
    else:
        _put(over, 7, 12, metal[2]), _put(over, 8, 12, metal[1])
    return base, over


def slot_icon():
    """The empty Backpack slot's hint: a pack in grey line art, as vanilla's empty slots draw theirs."""
    art = [
        "................",
        "......####......",
        ".....#....#.....",
        "...##########...",
        "..#..........#..",
        "..############..",
        "..#.#......#.#..",
        "..#.#......#.#..",
        "..#.########.#..",
        "..#..........#..",
        "..#...####...#..",
        "..#...#..#...#..",
        "..#...####...#..",
        "..#..........#..",
        "...##########...",
        "................",
    ]
    img = _img(16, 16)
    for y, row in enumerate(art):
        assert len(row) == 16, row
        for x, ch in enumerate(row):
            if ch == "#":
                img.putpixel((x, y), (85, 85, 85, 255))
    return img


# ================================================================ the worn model (64x32)

# Each box of BackpackModel: texture offset and size (width, height, depth), in pixels.
BOXES = {
    "body": ((0, 0), (8, 10, 4)),
    "pocket": ((24, 0), (6, 3, 1)),
    "pouch": ((40, 0), (1, 5, 3)),
    "strap": ((48, 0), (2, 8, 1)),
    "lid": ((0, 14), (9, 2, 5)),
    "flap": ((28, 14), (7, 4, 1)),
    "bedroll": ((0, 21), (9, 2, 3)),
}


def faces(name):
    """A box's faces as (x, y, width, height) on the sheet, unfolded the game's way."""
    (u, v), (w, h, d) = BOXES[name]
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "side_a": (u, v + d, d, h), "inner": (u + d, v + d, w, h),
        "side_b": (u + d + w, v + d, d, h), "outer": (u + 2 * d + w, v + d, w, h),
    }


def _grain(x, y, woven):
    """Leather's grain, or a cloth's weave: a grey for this pixel."""
    if woven:
        return L if (x + y) % 2 == 0 else N if (x // 2 + y) % 3 else M
    n = (x * 7 + y * 13 + (x * y) % 5) % 9
    return H if n == 0 else L if n < 5 else N


def _paint(img, rect, woven, edge=M):
    """A face of grained leather (or woven cloth) with a darker seam round its edge."""
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            seam = (x == x0 or x == x0 + w - 1 or y == y0 or y == y0 + h - 1) and w > 1 and h > 1
            _put(img, x, y, edge if seam else _grain(x, y, woven))


def worn(tier):
    """The worn model's two layers: (tinted, untinted)."""
    base, over = _img(64, 32), _img(64, 32)
    woven = tier == "runewoven_backpack"
    reinforced = tier == "reinforced_backpack"
    parts = ["body", "pocket", "strap", "lid", "flap"] + (["pouch"] if woven else [])
    for part in parts:
        for face, rect in faces(part).items():
            _paint(base, rect, woven)
    # The lid's top a shade lighter, the bag's bottom a shade darker.
    x0, y0, w, h = faces("lid")["top"]
    for y in range(y0 + 1, y0 + h - 1):
        for x in range(x0 + 1, x0 + w - 1):
            _put(base, x, y, H if (x + y) % 3 else L)
    x0, y0, w, h = faces("body")["bottom"]
    _rect(base, x0, y0, x0 + w - 1, y0 + h - 1, D)
    # Stitching down the middle of the bag's outer face and round the pocket.
    x0, y0, w, h = faces("body")["outer"]
    for y in range(y0 + 1, y0 + h - 1, 2):
        _put(base, x0 + 1, y, D)
        _put(base, x0 + w - 2, y, D)

    metal = GOLD if woven else IRON if reinforced else BRASS
    # The flap: two straps down it, buckled at the bottom.
    x0, y0, w, h = faces("flap")["outer"]
    for x in range(x0 + 1, x0 + w - 1):
        _put(over, x, y0, STRAP[2] if x % 2 == 0 else STRAP[1])
        _put(over, x, y0 + h - 1, STRAP[0])
    for x in (x0 + 1, x0 + w - 2):
        _rect(over, x, y0, x, y0 + h - 3, STRAP[1] if not woven else GOLD[0])
        _put(over, x, y0 + h - 2, metal[2])
        _put(over, x, y0 + h - 1, metal[1])
    # The pocket's button.
    x0, y0, w, h = faces("pocket")["outer"]
    for x in range(x0, x0 + w):
        _put(over, x, y0 + h - 1, STRAP[0] if x % 2 else STRAP[2])
    _put(over, x0 + w // 2 - 1, y0, metal[2])
    _put(over, x0 + w // 2, y0, metal[1])
    # A buckle on each strap, halfway down the chest.
    x0, y0, w, h = faces("strap")["outer"]
    x1, y1, w1, h1 = faces("strap")["inner"]
    for fx, fy, fw in ((x0, y0, w), (x1, y1, w1)):
        _rect(over, fx, fy + 3, fx + fw - 1, fy + 3, metal[2])
        _rect(over, fx, fy + 4, fx + fw - 1, fy + 4, metal[1])
    if reinforced:
        # Iron at the bag's lower corners and the lid's, and a bedroll strapped underneath.
        x0, y0, w, h = faces("body")["outer"]
        for x, y in ((x0, y0 + h - 1), (x0 + 1, y0 + h - 1), (x0, y0 + h - 2),
                     (x0 + w - 1, y0 + h - 1), (x0 + w - 2, y0 + h - 1), (x0 + w - 1, y0 + h - 2)):
            _put(over, x, y, IRON[1])
        for side in ("side_a", "side_b"):
            sx, sy, sw, sh = faces("body")[side]
            _rect(over, sx, sy + sh - 2, sx + sw - 1, sy + sh - 1, IRON[0])
        x0, y0, w, h = faces("lid")["outer"]
        for x in (x0, x0 + w - 1):
            _rect(over, x, y0, x, y0 + h - 1, IRON[2])
        for face, (fx, fy, fw, fh) in faces("bedroll").items():
            for y in range(fy, fy + fh):
                for x in range(fx, fx + fw):
                    band = WOOL[2] if (y - fy) == 0 else WOOL[0] if (y - fy) == fh - 1 else WOOL[1]
                    _put(over, x, y, band)
            if face in ("outer", "top", "bottom", "inner"):
                # Two leather straps round the roll.
                for x in (fx + 2, fx + fw - 3):
                    _rect(over, x, fy, x, fy + fh - 1, STRAP[1])
    if woven:
        # Gold trim along the lid, flap and pocket; faceted runes visible from behind.
        x0, y0, w, h = faces("lid")["outer"]
        _rect(over, x0, y0 + h - 1, x0 + w - 1, y0 + h - 1, GOLD[1])
        _rect(over, x0, y0, x0 + w - 1, y0, GOLD[2])
        for side in ("side_a", "side_b"):
            sx, sy, sw, sh = faces("lid")[side]
            _rect(over, sx, sy + sh - 1, sx + sw - 1, sy + sh - 1, GOLD[1])
        # A rune on the flap: a small diamond of amethyst round a spot of cloth.
        x0, y0, w, h = faces("flap")["outer"]
        mid = x0 + w // 2
        for x, y, c in ((mid, y0, AMETHYST[2]), (mid - 1, y0 + 1, AMETHYST[2]),
                        (mid, y0 + 1, (255, 240, 255)), (mid + 1, y0 + 1, AMETHYST[1]),
                        (mid, y0 + 2, AMETHYST[1]), (mid, y0 + 3, AMETHYST[0])):
            _put(over, x, y, c)
        # A bound pocket with a smaller matching glyph.
        x0, y0, w, h = faces("pocket")["outer"]
        for x in range(x0, x0 + w):
            _put(over, x, y0, GOLD[1] if x % 2 else GOLD[2])
        for x, y, c in ((x0 + 1, y0 + 1, AMETHYST[1]), (x0 + 2, y0 + 1, AMETHYST[2]),
                        (x0 + 3, y0 + 1, (255, 240, 255)), (x0 + 4, y0 + 1, AMETHYST[1]),
                        (x0 + 2, y0 + 2, AMETHYST[0]), (x0 + 3, y0 + 2, AMETHYST[1])):
            _put(over, x, y, c)
        x0, y0, w, h = faces("body")["outer"]
        for x in range(x0 + 1, x0 + w - 1):
            if x % 2 == 0:
                _put(over, x, y0 + 5, AMETHYST[1])
        for side in ("side_a", "side_b"):
            sx, sy, sw, sh = faces("pouch")[side]
            _put(over, sx + sw // 2, sy + 1, GOLD[2])
        sx, sy, sw, sh = faces("pouch")["outer"]
        _put(over, sx, sy + 1, GOLD[2])
    return base, over


def _tinted(base, over, colour):
    """A preview of both layers, the first tinted by {@code colour} as the game does it."""
    r, g, b = (colour >> 16) & 255, (colour >> 8) & 255, colour & 255
    out = Image.new("RGBA", base.size, (0, 0, 0, 0))
    bp, op, pp = base.load(), over.load(), out.load()
    for y in range(base.height):
        for x in range(base.width):
            c = bp[x, y]
            if c[3]:
                pp[x, y] = (c[0] * r // 255, c[1] * g // 255, c[2] * b // 255, 255)
            if op[x, y][3]:
                pp[x, y] = op[x, y]
    return out


def tinted_icon(tier, colour=None):
    """The item as it shows in the game, undyed (for the wiki's pictures)."""
    base, over = icon(tier)
    return _tinted(base, over, COLOURS[tier] if colour is None else colour)


def main():
    for tier in TIERS:
        base, over = icon(tier)
        (TEX / "item").mkdir(parents=True, exist_ok=True)
        base.save(TEX / f"item/{tier}.png")
        over.save(TEX / f"item/{tier}_overlay.png")
        base, over = worn(tier)
        (TEX / "entity/backpack").mkdir(parents=True, exist_ok=True)
        base.save(TEX / f"entity/backpack/{tier}.png")
        over.save(TEX / f"entity/backpack/{tier}_overlay.png")
    (TEX / "gui/sprites/container/slot").mkdir(parents=True, exist_ok=True)
    slot_icon().save(TEX / "gui/sprites/container/slot/backpack.png")


def preview(out_dir):
    """Every icon undyed and dyed, the worn sheets tinted, and the slot hint, scaled up."""
    tiles = []
    for tier in TIERS:
        base, over = icon(tier)
        for colour in (COLOURS[tier], 0xB02E26, 0x3C44AA, 0xF9FFFE):
            tiles.append(_tinted(base, over, colour))
    tiles.append(slot_icon())
    sheet = Image.new("RGBA", (len(tiles) * 18 + 2, 18 + 2 + 3 * 34), (0x8B, 0x8B, 0x8B, 255))
    for i, tile in enumerate(tiles):
        sheet.alpha_composite(tile, (2 + i * 18, 2))
    for i, tier in enumerate(TIERS):
        base, over = worn(tier)
        sheet.alpha_composite(_tinted(base, over, COLOURS[tier]), (2, 20 + i * 34))
    Path(out_dir).mkdir(parents=True, exist_ok=True)
    out = Path(out_dir) / "backpack_art.png"
    sheet.resize((sheet.width * 6, sheet.height * 6), Image.NEAREST).save(out)
    return out


if __name__ == "__main__":
    main()
    if "--preview" in sys.argv:
        print(preview(ROOT / "build/art-preview"))
    print("backpack textures written")
