"""GUI and HUD sprites for Wildercord: the Cord screen, the HUD and the Cord slot.

Sprites go to assets/wildercord/textures/gui/sprites/, which Minecraft stitches into its GUI
atlas. Panels use nine-slice scaling (declared in .mcmeta) so any size draws crisply.
Run from the project root:  python tools/gui_art.py [--preview]
"""
import json
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SPRITES = ROOT / "src/main/resources/assets/wildercord/textures/gui/sprites"

# Palette: dark carved stone, bronze-gold trim, violet gem accents.
OUTLINE = (11, 9, 16)
STONE = (27, 23, 38)
STONE_2 = (31, 26, 43)
STONE_DARK = (18, 15, 26)
BEVEL_LIGHT = (74, 63, 102)
BEVEL_MID = (52, 44, 74)
BEVEL_DARK = (14, 12, 21)
GOLD_DARK = (122, 92, 46)
GOLD = (176, 138, 62)
GOLD_LIGHT = (232, 196, 106)
GEM_DARK = (80, 40, 140)
GEM = (160, 100, 240)
GEM_LIGHT = (220, 190, 255)
RECESS = (15, 12, 22)
RECESS_2 = (21, 17, 30)
ROPE = (200, 168, 120)
ROPE_DARK = (138, 106, 72)
ROPE_SHADOW = (70, 52, 38)
RED = (224, 80, 80)
RED_DARK = (90, 24, 32)


def img(w, h):
    return Image.new("RGBA", (w, h), (0, 0, 0, 0))


def put(im, x, y, c, a=255):
    if 0 <= x < im.width and 0 <= y < im.height:
        im.putpixel((x, y), tuple(c) + (a,))


def rect(im, x0, y0, x1, y1, c, a=255):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(im, x, y, c, a)


def save(im, name, nine=None, tile=None):
    path = SPRITES / f"{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path)
    meta = SPRITES / f"{name}.png.mcmeta"
    if nine:
        border = nine
        meta.write_text(json.dumps({"gui": {"scaling": {"type": "nine_slice", "width": im.width, "height": im.height, "border": border}}}, indent=2) + "\n")
    elif tile:
        meta.write_text(json.dumps({"gui": {"scaling": {"type": "tile", "width": im.width, "height": im.height}}}, indent=2) + "\n")
    elif meta.exists():
        meta.unlink()
    return im


def speckle(im, x0, y0, x1, y1, base, alt, seed=7):
    """Fills with base and a sparse, regular stone speckle (tiles cleanly on a 16px grid)."""
    rect(im, x0, y0, x1, y1, base)
    pattern = [(2, 3), (9, 1), (13, 7), (5, 10), (11, 13), (1, 14), (7, 6)]
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if ((x - x0) % 16, (y - y0) % 16) in pattern:
                put(im, x, y, alt)


# ---------------------------------------------------------------- Cord screen


def panel():
    """Main window: 48x48 nine-slice, 10px border with gold trim and a gem in each corner."""
    w = h = 48
    im = img(w, h)
    speckle(im, 0, 0, w - 1, h - 1, STONE, STONE_2)
    # Outer outline and stone bevel.
    for x in range(w):
        put(im, x, 0, OUTLINE); put(im, x, h - 1, OUTLINE)
    for y in range(h):
        put(im, 0, y, OUTLINE); put(im, w - 1, y, OUTLINE)
    for i in range(1, w - 1):
        put(im, i, 1, BEVEL_LIGHT); put(im, 1, i, BEVEL_LIGHT)
        put(im, i, 2, BEVEL_MID); put(im, 2, i, BEVEL_MID)
        put(im, i, h - 2, BEVEL_DARK); put(im, w - 2, i, BEVEL_DARK)
    # Gold trim line inset 4px, with highlight on its top/left.
    for i in range(4, w - 4):
        put(im, i, 4, GOLD_LIGHT); put(im, i, h - 5, GOLD_DARK)
        put(im, 4, i, GOLD_LIGHT); put(im, w - 5, i, GOLD_DARK)
        put(im, i, 5, GOLD_DARK); put(im, 5, i, GOLD_DARK)
    # Corner settings: a gold diamond holding a violet gem.
    for cx, cy in ((6, 6), (w - 7, 6), (6, h - 7), (w - 7, h - 7)):
        for dy in range(-4, 5):
            for dx in range(-4, 5):
                d = abs(dx) + abs(dy)
                if d == 4:
                    put(im, cx + dx, cy + dy, OUTLINE)
                elif d == 3:
                    put(im, cx + dx, cy + dy, GOLD_LIGHT if dx + dy < -1 else GOLD if dx + dy <= 1 else GOLD_DARK)
                elif d == 2:
                    put(im, cx + dx, cy + dy, OUTLINE if (dx == 0 or dy == 0) and False else GEM_DARK if dx + dy > 0 else GEM)
                elif d == 1:
                    put(im, cx + dx, cy + dy, GEM if dx + dy <= 0 else GEM_DARK)
        put(im, cx, cy, GEM_LIGHT)
        put(im, cx - 1, cy - 1, GOLD_LIGHT)
    return save(im, "cord/panel", nine=11)


def inset():
    """A recessed well for the Codex and readout: 16x16 nine-slice, 2px border."""
    im = img(16, 16)
    speckle(im, 0, 0, 15, 15, RECESS, RECESS_2)
    for i in range(16):
        put(im, i, 0, OUTLINE); put(im, 0, i, OUTLINE)
        put(im, i, 1, BEVEL_DARK); put(im, 1, i, BEVEL_DARK)
        put(im, i, 15, BEVEL_MID); put(im, 15, i, BEVEL_MID)
    put(im, 0, 15, BEVEL_DARK); put(im, 15, 0, BEVEL_DARK)
    return save(im, "cord/inset", nine=2)


def row(name, fill, top, bottom, trim=None):
    """A spell row plate: 32x22 nine-slice, 3px border."""
    im = img(32, 22)
    rect(im, 0, 0, 31, 21, fill)
    for x in range(32):
        put(im, x, 0, OUTLINE); put(im, x, 21, OUTLINE)
        put(im, x, 1, top); put(im, x, 20, bottom)
    for y in range(22):
        put(im, 0, y, OUTLINE); put(im, 31, y, OUTLINE)
        put(im, 1, y, top); put(im, 30, y, bottom)
    if trim:
        light, dark = trim
        for x in range(2, 30):
            put(im, x, 2, light); put(im, x, 19, dark)
        for y in range(2, 20):
            put(im, 2, y, light); put(im, 29, y, dark)
    return save(im, name, nine=3)


def socket(name, rim_light, rim, rim_dark, inner, inner_2, glow=None):
    """A carved socket: an 18x18 recess with a metal rim (lit from the top-left)."""
    im = img(18, 18)
    for y in range(18):
        for x in range(18):
            cx, cy = x - 8.5, y - 8.5
            corner = max(abs(cx), abs(cy))
            rounded = abs(cx) + abs(cy) > 13.0
            if corner > 8.5 or rounded:
                continue
            if corner > 7.6 or abs(cx) + abs(cy) > 12.2:
                put(im, x, y, OUTLINE)
            elif corner > 6.6 or abs(cx) + abs(cy) > 11.2:
                put(im, x, y, rim_light if cx + cy < 0 else rim_dark)
            elif corner > 5.6 or abs(cx) + abs(cy) > 10.2:
                put(im, x, y, rim)
            else:
                # Inside the recess: shadow on the top-left, a faint lift on the bottom-right.
                c = inner
                if cx + cy < -7:
                    c = BEVEL_DARK
                elif cx + cy > 8:
                    c = inner_2
                put(im, x, y, c)
    if glow:
        for x, y in ((8, 3), (9, 3), (3, 8), (3, 9), (14, 8), (14, 9), (8, 14), (9, 14)):
            put(im, x, y, glow)
    return save(im, name)


def thread():
    """A twisted rope segment, tiled behind a spell's sockets: 8x4."""
    im = img(8, 4)
    for x in range(8):
        twist = x % 4
        put(im, x, 0, ROPE_SHADOW if twist in (0, 3) else OUTLINE)
        put(im, x, 1, ROPE if twist < 2 else ROPE_DARK)
        put(im, x, 2, ROPE_DARK if twist < 2 else ROPE)
        put(im, x, 3, ROPE_SHADOW)
    return save(im, "cord/thread", tile=True)


def tab(name, fill, top, bottom):
    """A filter tab: 16x13 nine-slice, 2px border, open at the bottom."""
    im = img(16, 13)
    rect(im, 0, 0, 15, 12, fill)
    for x in range(16):
        put(im, x, 0, OUTLINE); put(im, x, 1, top)
    for y in range(13):
        put(im, 0, y, OUTLINE); put(im, 15, y, OUTLINE)
        put(im, 1, y, top); put(im, 14, y, bottom)
    return save(im, name, nine=2)


def gem(name, color):
    """A 5x5 faceted gem that marks a family on its tab."""
    im = img(5, 5)
    light = tuple(min(255, c + 90) for c in color)
    dark = tuple(max(0, int(c * 0.5)) for c in color)
    shape = ["  o  ", " o#o ", "o+##o", " o#o ", "  o  "]
    shape = [" o#o ", "o+##o", "o###o", "o#-#o", " o-o "]
    for y, line in enumerate(shape):
        for x, ch in enumerate(line):
            if ch == "o":
                put(im, x, y, OUTLINE)
            elif ch == "#":
                put(im, x, y, color)
            elif ch == "+":
                put(im, x, y, light)
            elif ch == "-":
                put(im, x, y, dark)
    return save(im, name)


def lock():
    """A 7x8 padlock."""
    im = img(7, 8)
    shape = [" ooooo ", " o   o ", " o   o ", "ooooooo", "o+###-o", "o##o#-o", "o##o#-o", "ooooooo"]
    for y, line in enumerate(shape):
        for x, ch in enumerate(line):
            if ch == "o":
                put(im, x, y, OUTLINE)
            elif ch == "#":
                put(im, x, y, GOLD)
            elif ch == "+":
                put(im, x, y, GOLD_LIGHT)
            elif ch == "-":
                put(im, x, y, GOLD_DARK)
    # Shackle interior shading.
    for y in (1, 2):
        put(im, 2, y, GOLD_DARK); put(im, 4, y, GOLD_DARK)
    return save(im, "cord/lock")


def scroller():
    im = img(4, 12)
    rect(im, 0, 0, 3, 11, GOLD)
    for y in range(12):
        put(im, 0, y, GOLD_LIGHT); put(im, 3, y, GOLD_DARK)
    for x in range(4):
        put(im, x, 0, GOLD_LIGHT); put(im, x, 11, GOLD_DARK)
    return save(im, "cord/scroller", nine=1)


def link_mark():
    """A tiny gold hook drawn under a modifier and the rune it changes."""
    im = img(3, 3)
    for x, y in ((1, 0), (0, 1), (1, 1), (2, 1), (1, 2)):
        put(im, x, y, GOLD_LIGHT if y < 1 else GOLD)
    return save(im, "cord/attach")


# ---------------------------------------------------------------- HUD


def mana_frame():
    """Frame around the mana bar: 24x8 nine-slice, 2px border."""
    im = img(24, 8)
    rect(im, 0, 0, 23, 7, RECESS)
    for x in range(24):
        put(im, x, 0, OUTLINE); put(im, x, 7, OUTLINE)
        put(im, x, 1, GOLD_DARK); put(im, x, 6, GOLD)
    for y in range(8):
        put(im, 0, y, OUTLINE); put(im, 23, y, OUTLINE)
        put(im, 1, y, GOLD_DARK); put(im, 22, y, GOLD)
    put(im, 1, 1, OUTLINE); put(im, 22, 6, GOLD_LIGHT)
    return save(im, "hud/mana_frame", nine=2)


def mana_fill():
    """Mana liquid: a vertical gradient with a bright top line; 8x4, stretched to fit."""
    im = img(8, 4)
    rows = [(196, 176, 255), (156, 124, 255), (112, 80, 222), (80, 52, 178)]
    for y, c in enumerate(rows):
        for x in range(8):
            put(im, x, y, c)
    for x in (1, 5):
        put(im, x, 0, (236, 228, 255))
    return save(im, "hud/mana_fill", tile=True)


def spell_plate():
    """Backing plate for the selected spell: 16x14 nine-slice, translucent."""
    im = img(16, 14)
    rect(im, 0, 0, 15, 13, (12, 10, 18), 170)
    for x in range(16):
        put(im, x, 0, GOLD_DARK, 220); put(im, x, 13, OUTLINE, 220)
    for y in range(14):
        put(im, 0, y, GOLD_DARK, 220); put(im, 15, y, OUTLINE, 220)
    put(im, 0, 0, GOLD, 255)
    return save(im, "hud/spell_plate", nine=2)


def cooldown_fill():
    im = img(4, 2)
    for x in range(4):
        put(im, x, 0, GOLD_LIGHT)
        put(im, x, 1, GOLD)
    return save(im, "hud/cooldown", tile=True)


def hud_frame():
    """The HUD panel beside the hotbar: 16x16 nine-slice, 3px border, translucent dark with a gold edge."""
    im = img(16, 16)
    rect(im, 0, 0, 15, 15, (14, 11, 20), 205)
    for i in range(16):
        put(im, i, 0, OUTLINE, 230); put(im, 0, i, OUTLINE, 230)
        put(im, i, 15, OUTLINE, 230); put(im, 15, i, OUTLINE, 230)
    for i in range(1, 15):
        put(im, i, 1, GOLD_DARK); put(im, 1, i, GOLD_DARK)
        put(im, i, 14, (40, 32, 20), 230); put(im, 14, i, (40, 32, 20), 230)
    put(im, 1, 1, GOLD)
    put(im, 2, 1, GOLD_LIGHT); put(im, 1, 2, GOLD_LIGHT)
    return save(im, "hud/frame", nine=3)


def bar_frame():
    """A slim mana bar well: 12x6 nine-slice, 1px border."""
    im = img(12, 6)
    rect(im, 0, 0, 11, 5, RECESS)
    for x in range(12):
        put(im, x, 0, OUTLINE); put(im, x, 5, (60, 48, 30))
    for y in range(6):
        put(im, 0, y, OUTLINE); put(im, 11, y, (60, 48, 30))
    return save(im, "hud/bar_frame", nine=1)


def badge():
    """The spell-number badge: a 20x20 round socket with a gold rim."""
    im = img(20, 20)
    for y in range(20):
        for x in range(20):
            cx, cy = x - 9.5, y - 9.5
            d = (cx * cx + cy * cy) ** 0.5
            if d > 9.6:
                continue
            if d > 8.7:
                put(im, x, y, OUTLINE)
            elif d > 7.6:
                put(im, x, y, GOLD_LIGHT if cx + cy < -3 else GOLD if cx + cy < 4 else GOLD_DARK)
            elif d > 6.8:
                put(im, x, y, GOLD_DARK)
            else:
                put(im, x, y, BEVEL_DARK if cx + cy < -6 else RECESS_2 if cx + cy > 5 else RECESS)
    return save(im, "hud/badge")


# ---------------------------------------------------------------- inventory


def inventory_slot():
    """The Cord slot's well in the inventory: vanilla's slot bevel with gold rivets so it reads as special."""
    im = img(18, 18)
    rect(im, 0, 0, 17, 17, (139, 139, 139))
    for i in range(17):
        put(im, i, 0, (55, 55, 55)); put(im, 0, i, (55, 55, 55))
    for i in range(1, 18):
        put(im, i, 17, (255, 255, 255)); put(im, 17, i, (255, 255, 255))
    put(im, 17, 0, (139, 139, 139)); put(im, 0, 17, (139, 139, 139))
    for x, y in ((1, 1), (16, 1), (1, 16), (16, 16)):
        put(im, x, y, GOLD)
    put(im, 1, 1, GOLD_LIGHT)
    return save(im, "cord/inventory_slot")


def main():
    panel()
    inset()
    row("cord/row", (35, 30, 51), BEVEL_MID, BEVEL_DARK)
    row("cord/row_selected", (44, 37, 70), GOLD_LIGHT, GOLD_DARK, trim=(GOLD_DARK, (30, 25, 44)))
    row("cord/row_locked", (20, 17, 28), (32, 27, 44), BEVEL_DARK)
    socket("cord/socket", GOLD_LIGHT, GOLD, GOLD_DARK, RECESS, RECESS_2)
    socket("cord/socket_quiet", (240, 120, 120), RED, RED_DARK, (30, 12, 18), (44, 18, 26))
    socket("cord/socket_hover", GEM_LIGHT, GEM, GEM_DARK, RECESS_2, (34, 28, 48), glow=GEM)
    thread()
    tab("cord/tab", (30, 26, 44), BEVEL_MID, BEVEL_DARK)
    tab("cord/tab_active", (46, 38, 72), GOLD_LIGHT, GOLD_DARK)
    for name, color in {"all": (200, 200, 210), "shape": (64, 200, 190), "effect": (240, 110, 50), "modifier": (240, 196, 64), "link": (160, 100, 240)}.items():
        gem(f"cord/gem_{name}", color)
    lock()
    scroller()
    link_mark()
    mana_frame()
    mana_fill()
    spell_plate()
    cooldown_fill()
    hud_frame()
    bar_frame()
    badge()
    inventory_slot()
    if "--preview" in sys.argv:
        preview()
    print("gui sprites written")


def preview():
    """A rough mock of the Cord screen from the sprites, for reviewing the art."""
    def nine(src, w, h, b):
        out = img(w, h)
        sw, sh = src.size
        for y in range(h):
            sy = y if y < b else (sh - (h - y) if y >= h - b else b + (y - b) % (sh - 2 * b))
            for x in range(w):
                sx = x if x < b else (sw - (w - x) if x >= w - b else b + (x - b) % (sw - 2 * b))
                out.putpixel((x, y), src.getpixel((sx, sy)))
        return out

    def load(n):
        return Image.open(SPRITES / f"{n}.png").convert("RGBA")

    W, H = 320, 236
    canvas = Image.new("RGBA", (W + 40, H + 40), (60, 90, 60, 255))
    canvas.alpha_composite(nine(load("cord/panel"), W, H, 10), (20, 20))
    for s in range(4):
        name = "cord/row_selected" if s == 0 else "cord/row" if s < 2 else "cord/row_locked"
        canvas.alpha_composite(nine(load(name), W - 20, 22, 3), (30, 20 + 30 + s * 21))
        if s < 2:
            y = 20 + 30 + s * 21 + 2
            th = load("cord/thread")
            for x in range(20 + 34 + 9, 20 + 34 + 5 * 20 - 9, 8):
                canvas.alpha_composite(th, (x, y + 7))
            for i in range(5):
                canvas.alpha_composite(load("cord/socket" if i != 2 else "cord/socket_hover"), (20 + 34 + i * 20, y))
    canvas.alpha_composite(nine(load("cord/inset"), W - 20, 58, 2), (30, 20 + 138))
    canvas.alpha_composite(nine(load("cord/inset"), W - 20, 50, 2), (30, 20 + 200))
    preview_dir = Path(__file__).resolve().parent.parent / "build" / "art-preview"
    preview_dir.mkdir(parents=True, exist_ok=True)
    canvas.resize((canvas.width * 3, canvas.height * 3), Image.NEAREST).save(preview_dir / "gui_preview.png")


if __name__ == "__main__":
    main()
