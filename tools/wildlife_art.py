"""Magical wildlife: every creature's skin and glow layer, the items they leave behind, their spawn eggs and the
rimehare's frost print, and their data (loot tables, recipes, brews, tags) and English text.

Called from generate_assets.py (write(g) and LANG). The skins are painted box by box onto the exact UV layouts of the
models in src/client/java/dev/wildercord/client/wildlife/*Model.java: each box is painted from a function of the point
on it (X across, Y down, Z from front to back, in the box's own pixels), so a stripe down a spine or a belly paling
underneath is written once and lands on every face it crosses. Boxes that would overlap on the sheet are refused.

Run on its own (python tools/wildlife_art.py) for a review sheet of everything in build/art-preview/.
"""
import math
from pathlib import Path

from PIL import Image

from item_art import Canvas, blit, hexc
from world_art import noise

ROOT = Path(__file__).resolve().parent.parent

CREATURES = ["glimmerwing", "lumen_stag", "mossback_tortoise", "cinderfox", "skyray", "rimehare"]


# ============================================================== colour


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def shade(c, k):
    return tuple(max(0, min(255, int(round(v * k)))) for v in c[:3])


def along(cols, t):
    """A colour partway along a list of colours (t from 0 to 1)."""
    t = max(0.0, min(1.0, t)) * (len(cols) - 1)
    i = min(int(t), len(cols) - 2)
    return mix(cols[i], cols[i + 1], t - i)


def rgba(c, a):
    return (c[0], c[1], c[2], max(0, min(255, int(a))))


def H(*hexes):
    return [hexc(h) for h in hexes]


# ============================================================== skins


def faces(u, v, w, h, d):
    """Each face's rectangle (x, y, width, height) on the sheet, for a box at texture offset (u, v)."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def point(face, x, y, w, h, d):
    """Where pixel (x, y) of a face sits on its box: X across (+X), Y down from the top, Z from the front (-Z) back.

    Read from vanilla's cube: the top and bottom faces run +X left to right with the front along their last row; the
    right side (-X) runs back to front, the left side (+X) front to back, the back face +X to -X.
    """
    if face == "top":
        return x + 0.5, 0.0, d - (y + 0.5)
    if face == "bottom":
        return x + 0.5, float(h), d - (y + 0.5)
    if face == "right":
        return 0.0, y + 0.5, d - (x + 0.5)
    if face == "front":
        return x + 0.5, y + 0.5, 0.0
    if face == "left":
        return float(w), y + 0.5, x + 0.5
    return w - (x + 0.5), y + 0.5, float(d)


# The game shades every face by the light; these only lift the top a touch and sink the underside, as vanilla's own
# skins do, so a creature reads as lit from above even in flat light.
TONE = {"top": 1.05, "bottom": 0.86, "right": 0.97, "front": 1.0, "left": 0.97, "back": 0.94}


class Skin:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.px = self.im.load()
        self.used = []

    def put(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h and c is not None:
            self.px[x, y] = c if len(c) == 4 else (c[0], c[1], c[2], 255)

    def claim(self, key, rect):
        x, y, w, h = rect
        if w == 0 or h == 0:
            return
        assert x >= 0 and y >= 0 and x + w <= self.w and y + h <= self.h, f"{key}: {rect} runs off the {self.w}x{self.h} sheet"
        for other, (ox, oy, ow, oh) in self.used:
            if other != key and x < ox + ow and ox < x + w and y < oy + oh and oy < y + h:
                raise AssertionError(f"box {key} face {rect} overlaps box {other} face {(ox, oy, ow, oh)}")
        self.used.append((key, rect))

    def box(self, u, v, w, h, d, paint, tone=True):
        """Paints one box: paint(face, X, Y, Z, tx, ty) gives an RGB(A) colour, or None to leave a pixel clear."""
        key = (u, v, w, h, d)
        for face, rect in faces(u, v, w, h, d).items():
            self.claim(key, rect)
            x0, y0, fw, fh = rect
            for y in range(fh):
                for x in range(fw):
                    X, Y, Z = point(face, x, y, w, h, d)
                    c = paint(face, X, Y, Z, x0 + x, y0 + y)
                    if c is None:
                        continue
                    if tone and len(c) == 3:
                        c = shade(c, TONE[face])
                    self.put(x0 + x, y0 + y, c)

    def at(self, u, v, w, h, d, face, x, y, c):
        """Sets one pixel of a face, by its place on that face (for eyes and other fine work)."""
        x0, y0, fw, fh = faces(u, v, w, h, d)[face]
        assert 0 <= x < fw and 0 <= y < fh, (face, x, y)
        self.put(x0 + x, y0 + y, c)

    def image(self):
        return self.im


def fur(c, tx, ty, seed, amount=0.08):
    """A coat's grain: each hair a shade lighter or darker, a few streaks longer than one pixel."""
    n = noise(tx, ty, seed)
    streak = noise(tx, ty // 2, seed + 7)
    k = 1 + (n - 0.5) * 2 * amount + (0.035 if streak > 0.86 else -0.03 if streak < 0.1 else 0)
    return shade(c, k)


# ============================================================== the lumen stag

STAG_COAT = H("#7E6B55", "#A8937A", "#C2AF93", "#D9CCB3", "#EFE6D4")
STAG_STRIPE = hexc("#8C7860")
STAG_SPOT = hexc("#F6F0E2")
STAG_HOOF = H("#2E2926", "#463E38")
STAG_INNER_EAR = hexc("#E9D3C3")
STAG_NOSE = hexc("#2A2525")
STAG_EYE = hexc("#17151C")
STAG_BONE = hexc("#E6DDC8")
STAG_CRYSTAL = H("#4EAAE6", "#7ACCFA", "#B2EAFF", "#E8FCFF")

# Pale spots in two rows down the back, like a fawn's that never faded: (X, Z) on the body's top, in pixels.
STAG_SPOTS = [(2.2, 3.5), (5.8, 5.0), (2.0, 7.0), (5.9, 8.6), (2.3, 10.4), (5.6, 12.0), (2.6, 13.8), (5.2, 15.0), (3.9, 2.0)]
# ... and a few down the flanks: (Z, Y) on each side.
STAG_FLANK = [(4.0, 1.5), (7.5, 2.2), (10.5, 1.4), (13.5, 2.0), (6.0, 3.6), (11.8, 3.4)]


def stag_skin(glow=False):
    sk = Skin(128, 64)
    clear = None

    def coat(Y, h, tx, ty, seed=11, lift=0.0):
        """Darker along the back, paling to cream underneath."""
        return fur(along(STAG_COAT, 0.42 + 0.55 * (Y / max(h, 1)) + lift), tx, ty, seed)

    def body(face, X, Y, Z, tx, ty):
        if glow:
            near = any((X - sx) ** 2 + (Z - sz) ** 2 < 0.9 for sx, sz in STAG_SPOTS) if face == "top" else False
            near = near or (face in ("left", "right") and any((Z - fz) ** 2 + (Y - fy) ** 2 < 0.7 for fz, fy in STAG_FLANK))
            return rgba(hexc("#CFF6FF"), 60) if near else clear
        if face == "top":
            c = coat(0, 8, tx, ty)
            if abs(X - 4) < 1.1:
                c = fur(STAG_STRIPE, tx, ty, 12)
            for sx, sz in STAG_SPOTS:
                if (X - sx) ** 2 + (Z - sz) ** 2 < 0.9:
                    c = STAG_SPOT
            return c
        if face == "bottom":
            return fur(STAG_COAT[4], tx, ty, 13, 0.05)
        if face == "back":
            # The pale rump patch, rimmed darker.
            r = ((X - 4) / 3.2) ** 2 + ((Y - 3.6) / 3.6) ** 2
            if r < 1:
                return fur(STAG_SPOT, tx, ty, 14, 0.04)
            if r < 1.45:
                return fur(STAG_STRIPE, tx, ty, 15)
            return coat(Y, 8, tx, ty)
        c = coat(Y, 8, tx, ty)
        if face in ("left", "right"):
            for fz, fy in STAG_FLANK:
                if (Z - fz) ** 2 + (Y - fy) ** 2 < 0.7:
                    c = STAG_SPOT
        return c

    def chest(face, X, Y, Z, tx, ty):
        if glow:
            return clear
        return fur(mix(STAG_COAT[3], STAG_COAT[4], Y / 4), tx, ty, 16, 0.07)

    def neck(face, X, Y, Z, tx, ty):
        if glow:
            return clear
        if face == "front":
            return fur(mix(STAG_COAT[3], STAG_COAT[4], 0.5), tx, ty, 17)
        c = coat(Y, 9, tx, ty, 18, -0.08)
        if face == "back" and abs(X - 2) < 0.9:
            c = fur(STAG_STRIPE, tx, ty, 19)
        return c

    def ruff(face, X, Y, Z, tx, ty):
        if glow:
            return clear
        # Shaggy cream, streaked.
        c = mix(STAG_COAT[3], STAG_COAT[4], 0.55)
        return fur(c, tx, ty, 20, 0.12)

    def head(face, X, Y, Z, tx, ty):
        if glow:
            return clear
        if face == "top":
            return fur(mix(STAG_COAT[1], STAG_STRIPE, 0.4), tx, ty, 21)
        if face == "front":
            # A pale blaze down the middle of the brow.
            c = coat(Y, 5, tx, ty, 22, -0.1)
            if abs(X - 2.5) < 0.8:
                c = fur(STAG_COAT[4], tx, ty, 23, 0.04)
            return c
        if face == "bottom":
            return fur(STAG_COAT[4], tx, ty, 24, 0.04)
        return coat(Y, 5, tx, ty, 25, -0.12)

    def muzzle(face, X, Y, Z, tx, ty):
        if glow:
            return clear
        if face == "top":
            return fur(mix(STAG_COAT[2], STAG_COAT[3], 0.4), tx, ty, 26)
        return fur(mix(STAG_COAT[3], STAG_COAT[4], 0.6), tx, ty, 27, 0.05)

    def ear(face, X, Y, Z, tx, ty):
        if glow:
            return clear
        if face == "front":
            return STAG_INNER_EAR if X < 2.4 and 0.4 < Y < 1.6 else fur(STAG_COAT[2], tx, ty, 28)
        if X > 2.3:
            return fur(STAG_COAT[0], tx, ty, 29)
        return fur(STAG_COAT[1], tx, ty, 30)

    def crystal(length, part):
        """An antler's crystal: bone at the burr, then ice-clear blue brightening to white tips, faceted."""
        def paint(face, X, Y, Z, tx, ty):
            t = 1 - Y / length
            if part == "beam" and t < 0.14:
                return rgba(hexc("#B9F0FF"), 60) if glow else fur(STAG_BONE, tx, ty, 31, 0.05)
            base = 0.25 if part == "beam" else 0.45
            c = along(STAG_CRYSTAL, base + (1 - base) * t)
            facet = face in ("left", "back")
            if facet:
                c = shade(c, 0.82)
            if noise(tx, ty, 33) > 0.82:
                c = mix(c, hexc("#FFFFFF"), 0.5)
            if glow:
                return rgba(mix(c, hexc("#FFFFFF"), 0.25), 150 + 105 * t)
            return c
        return paint

    def leg(top, low, length):
        def paint(face, X, Y, Z, tx, ty):
            if glow:
                return clear
            t = Y / length
            if low and Y > length - 1.6:
                return along(STAG_HOOF, 0.3 if face != "bottom" else 0)
            c = along(STAG_COAT, (0.5 - 0.25 * t) if top else (0.32 - 0.2 * t))
            if face == "back" and top:
                c = mix(c, STAG_SPOT, 0.55)
            return fur(c, tx, ty, 34)
        return paint

    def tail(face, X, Y, Z, tx, ty):
        if glow:
            return clear
        if face in ("front", "bottom"):
            return fur(STAG_SPOT, tx, ty, 35, 0.04)
        return fur(STAG_STRIPE if Y < 2 else STAG_COAT[1], tx, ty, 36)

    sk.box(0, 0, 8, 8, 17, body)
    sk.box(0, 25, 6, 4, 3, chest)
    sk.box(50, 0, 4, 9, 5, neck)
    sk.box(68, 0, 4, 4, 1, ruff)
    sk.box(82, 0, 5, 5, 6, head)
    sk.box(104, 0, 3, 3, 4, muzzle)
    sk.box(104, 7, 3, 2, 1, ear)
    sk.box(112, 7, 1, 10, 1, crystal(10, "beam"), tone=False)
    sk.box(116, 7, 1, 5, 1, crystal(5, "tine"), tone=False)
    sk.box(120, 7, 1, 5, 1, crystal(5, "tine"), tone=False)
    sk.box(124, 7, 1, 4, 1, crystal(4, "tine"), tone=False)
    sk.box(116, 13, 1, 3, 1, crystal(3, "tine"), tone=False)
    sk.box(0, 36, 3, 5, 3, leg(True, False, 5))
    sk.box(12, 36, 2, 6, 2, leg(False, True, 6))
    sk.box(20, 36, 4, 6, 4, leg(True, False, 6))
    sk.box(36, 36, 2, 7, 2, leg(False, True, 7))
    sk.box(44, 36, 3, 4, 2, tail)

    # Eyes, set on the sides of the head near the front, dark and wet with a glint of the antlers' light.
    for face, x in (("right", 4), ("left", 1)):
        if glow:
            sk.at(82, 0, 5, 5, 6, face, x, 1, rgba(hexc("#A8F2FF"), 210))
        else:
            sk.at(82, 0, 5, 5, 6, face, x, 1, STAG_EYE)
            sk.at(82, 0, 5, 5, 6, face, x, 2, shade(STAG_COAT[3], 1.05))
            sk.at(82, 0, 5, 5, 6, face, x + (-1 if face == "right" else 1), 1, shade(STAG_EYE, 2.2))
    if not glow:
        # A dark nose on the muzzle's front, and its mouth.
        for x in (0, 1, 2):
            sk.at(104, 0, 3, 3, 4, "front", x, 0, STAG_NOSE if x == 1 else shade(STAG_NOSE, 1.6))
        sk.at(104, 0, 3, 3, 4, "front", 1, 2, shade(STAG_NOSE, 2.4))
    return sk.image()


# ============================================================== the mossback tortoise

SHELL = H("#2F2818", "#4E4329", "#655836", "#7D7047", "#978A5B")
MOSS = H("#2C4A1A", "#3E6424", "#557F2E", "#6F9C38", "#8EBC48")
PLASTRON = H("#7A6A44", "#A8955F", "#C8B37A", "#DCCB94")
SKIN = H("#45432E", "#5E5C3E", "#777452", "#908C68")
BEAK = hexc("#A8925A")
LICHEN = hexc("#A8FFD0")


def scute(X, Z, cell, ox=0.0, oz=0.0):
    """Where a point falls among a shell's plates: (distance to the nearest seam, distance from the plate's middle)."""
    gx = (X + ox) / cell
    gz = (Z + oz) / cell
    fx, fz = gx - math.floor(gx), gz - math.floor(gz)
    seam = min(fx, 1 - fx, fz, 1 - fz) * cell
    middle = math.hypot(fx - 0.5, fz - 0.5) * cell
    return seam, middle


def mossy(cover, tx, ty, seed):
    """Whether a pixel is under moss, for a tier this mossy (0 to 1): clumps, not speckle."""
    n = noise(tx // 2, ty // 2, seed) * 0.6 + noise(tx, ty, seed + 1) * 0.4
    return n < cover


def tortoise_skin(glow=False):
    sk = Skin(128, 128)
    clear = None

    def plates(cover, cell, seed):
        def paint(face, X, Y, Z, tx, ty):
            under = face == "top" and mossy(cover, tx, ty, seed)
            # Moss drips over the top edge of each tier.
            drip = face not in ("top", "bottom") and Y < (0.8 + 1.6 * noise(tx, 0, seed + 3)) * cover
            if glow:
                if (under or drip) and noise(tx, ty, seed + 9) > 0.965:
                    return rgba(LICHEN, 235)
                return clear
            if under or drip:
                m = along(MOSS, 0.35 + 0.5 * noise(tx, ty, seed + 4))
                return shade(m, 0.85) if drip and Y > 1.0 else m
            if face == "bottom":
                return along(SHELL, 0.25)
            seam, middle = scute(X, Z if face == "top" else Y * 3 + (X if face in ("front", "back") else Z), cell)
            if seam < 0.6:
                return SHELL[0]
            ring = (middle * 1.6) % 2 < 0.55
            c = along(SHELL, 0.45 + 0.35 * (1 - middle / cell) + (0.15 if ring else 0))
            return fur(c, tx, ty, seed + 5, 0.06)
        return paint

    def crown(face, X, Y, Z, tx, ty):
        if glow:
            return rgba(LICHEN, 235) if noise(tx, ty, 91) > 0.955 else clear
        return along(MOSS, 0.45 + 0.5 * noise(tx, ty, 90)) if face != "bottom" else MOSS[1]

    def plastron(face, X, Y, Z, tx, ty):
        if glow:
            return clear
        if face == "top":
            return SHELL[1]
        seam = abs(X - 8.5) < 0.5 or abs(((Z if face == "bottom" else 0) - 10.5) % 5.25) < 0.45
        if face == "bottom":
            return PLASTRON[0] if seam else fur(along(PLASTRON, 0.55 + 0.4 * noise(tx, ty, 92)), tx, ty, 93, 0.04)
        return fur(PLASTRON[1], tx, ty, 94, 0.05)

    def strands(face, X, Y, Z, tx, ty):
        """Moss hanging from the rim: a strand per column, each its own length, darker at the tip."""
        along_rim = Z if face in ("right", "left") else X
        column = int(along_rim)
        length = 1 + int(noise(column, 0, 95 if face in ("right", "left") else 96) * 5.2)
        if noise(column, 3, 97) < 0.18:
            return clear
        if Y > length:
            return clear
        if glow:
            return rgba(LICHEN, 225) if noise(column, int(Y), 98) > 0.93 else clear
        c = along(MOSS, 0.75 - 0.45 * (Y / 5))
        return fur(c, tx, ty, 99, 0.1)

    def hide(face, X, Y, Z, tx, ty, seed=100):
        """Leathery, scaled skin."""
        if glow:
            return clear
        c = along(SKIN, 0.35 + 0.35 * (1 - Y / 6))
        if (int(X * 1.5) + int(Y * 1.5) + int(Z * 1.5)) % 3 == 0 and noise(tx, ty, seed) > 0.4:
            c = SKIN[3]
        return fur(c, tx, ty, seed + 1, 0.07)

    def head(face, X, Y, Z, tx, ty):
        if glow:
            return clear
        if face == "top":
            return fur(along(SKIN, 0.5), tx, ty, 102)
        return hide(face, X, Y, Z, tx, ty, 103)

    def jaw(face, X, Y, Z, tx, ty):
        if glow:
            return clear
        return shade(BEAK, 0.75) if face == "bottom" else fur(BEAK, tx, ty, 104, 0.05)

    def leg(face, X, Y, Z, tx, ty):
        if glow:
            return clear
        if Y > 6.0 and face == "front" and int(X) in (1, 3, 5):
            return hexc("#D6CDA4")
        if face == "bottom":
            return SKIN[0]
        return hide(face, X, Y, Z, tx, ty, 105)

    def tail(face, X, Y, Z, tx, ty):
        return clear if glow else hide(face, X, Y, Z, tx, ty, 106)

    sk.box(0, 0, 20, 3, 24, plates(0.12, 6.0, 110))
    sk.box(0, 27, 18, 3, 22, plates(0.3, 5.5, 120))
    sk.box(0, 52, 14, 2, 17, plates(0.58, 4.5, 130))
    sk.box(62, 52, 9, 1, 10, crown)
    sk.box(0, 71, 17, 2, 21, plastron)
    sk.box(88, 0, 0, 5, 20, strands, tone=False)
    sk.box(88, 25, 18, 5, 0, strands, tone=False)
    sk.box(76, 71, 5, 5, 5, hide)
    sk.box(96, 71, 5, 5, 6, head)
    sk.box(76, 81, 3, 2, 2, jaw)
    sk.box(0, 94, 6, 7, 6, leg)
    sk.box(24, 94, 3, 2, 4, tail)
    if not glow:
        # Old, patient eyes ringed in gold, and two nostrils.
        for face, x in (("right", 4), ("left", 1)):
            sk.at(96, 71, 5, 5, 6, face, x, 2, hexc("#141410"))
            sk.at(96, 71, 5, 5, 6, face, x, 1, hexc("#B89236"))
            sk.at(96, 71, 5, 5, 6, face, x + (-1 if face == "right" else 1), 2, hexc("#B89236"))
        sk.at(96, 71, 5, 5, 6, "front", 1, 3, SKIN[0])
        sk.at(96, 71, 5, 5, 6, "front", 3, 3, SKIN[0])
        for x in range(3):
            sk.at(76, 81, 3, 2, 2, "front", x, 1, shade(BEAK, 0.7))
    return sk.image()


# ============================================================== the cinderfox

SAND = H("#7A4A22", "#A86C36", "#CB914F", "#E2B272", "#F3DCB0")
SOCK = H("#4A2E1E", "#6A4228")
EMBER = H("#6A2410", "#C8461A", "#F0762A", "#FFAA3A", "#FFDA6A", "#FFF6C8")
CHAR = hexc("#3E241A")
FOX_EYE = hexc("#FFB030")


def cinderfox_skin(glow=False):
    sk = Skin(64, 64)
    clear = None

    def coat(Y, h, tx, ty, seed):
        return fur(along(SAND, 0.5 + 0.45 * (Y / h)), tx, ty, seed)

    def body(face, X, Y, Z, tx, ty):
        if glow:
            return clear
        if face == "top":
            # A darker line down the spine and a faint saddle over the shoulders.
            if abs(X - 2.5) < 0.7:
                return fur(SAND[1], tx, ty, 39)
            return fur(mix(SAND[2], SAND[1], 0.35 if 2 < Z < 7 else 0.12), tx, ty, 40)
        if face == "bottom":
            return fur(SAND[4], tx, ty, 41, 0.05)
        if face == "front":
            return fur(SAND[4] if Y > 1.5 else SAND[3], tx, ty, 42, 0.05)
        return coat(Y, 5, tx, ty, 43)

    def head(face, X, Y, Z, tx, ty):
        if glow:
            return clear
        if face == "top":
            return fur(SAND[2] if abs(X - 2.5) > 0.7 else SAND[1], tx, ty, 44)
        if face == "front":
            # A cream mask below the eyes, a dark tear-line from each eye down toward the snout.
            if Y > 1.9:
                return fur(SAND[4], tx, ty, 45, 0.04)
            return fur(SAND[2], tx, ty, 45)
        if face == "bottom":
            return SAND[4]
        return fur(SAND[3] if Y > 2.2 else SAND[2], tx, ty, 46)

    def snout(face, X, Y, Z, tx, ty):
        if glow:
            return clear
        if face == "top":
            return fur(SAND[3], tx, ty, 47, 0.04)
        if face == "bottom":
            return SAND[4]
        return fur(SAND[4], tx, ty, 47, 0.03)

    def chin(face, X, Y, Z, tx, ty):
        if glow:
            return clear
        return fur(mix(SAND[4], hexc("#FFFFFF"), 0.25), tx, ty, 48, 0.1)

    def ear(face, X, Y, Z, tx, ty):
        if glow:
            return clear
        if Y < 1.2:
            return SOCK[0]
        if face == "front":
            # Pale pink inside, fringed with cream fur.
            if 0.7 < X < 2.3 and Y > 1.4:
                return mix(hexc("#F2A784"), hexc("#F8D8C0"), (Y - 1.4) / 3.6)
            return fur(SAND[3], tx, ty, 49)
        return fur(SAND[2], tx, ty, 50)

    def leg(face, X, Y, Z, tx, ty):
        if glow:
            return clear
        if Y > 2.6:
            return fur(SOCK[1] if Y < 4.2 else SOCK[0], tx, ty, 51)
        return fur(SAND[2], tx, ty, 52)

    def tail(face, X, Y, Z, tx, ty):
        """The root of the brush: sand, deepening toward rust."""
        if glow:
            return clear
        c = mix(SAND[2], hexc("#D07A38"), Z / 5)
        return fur(c if face != "bottom" else SAND[3], tx, ty, 53, 0.08)

    def brush(face, X, Y, Z, tx, ty):
        """The thick of the brush: rust, smouldering darker toward the ember, flecked with char."""
        t = Z / 4
        if glow:
            return rgba(EMBER[2], 90 * (t - 0.55) / 0.45) if t > 0.55 and face != "front" else clear
        c = mix(hexc("#C8642A"), hexc("#8E3A1C"), t * 0.8)
        if t > 0.5 and noise(tx, ty, 54) > 0.68:
            c = CHAR
        return fur(c, tx, ty, 55, 0.1)

    def ember(face, X, Y, Z, tx, ty):
        """The living ember: deep red where it meets the fur, white-hot at the very end."""
        t = Z / 3 if face != "back" else 1.0
        core = 1 - min(1.0, math.hypot(X - 1.5, Y - 1.5) / 1.8) if face == "back" else 0
        heat = 0.25 + 0.6 * t + 0.3 * core
        c = along(EMBER, heat)
        if noise(tx, ty, 56) > 0.86:
            c = mix(c, EMBER[5], 0.6)
        if glow:
            return rgba(mix(c, EMBER[5], 0.2), 130 + 125 * min(1.0, heat))
        return c

    sk.box(0, 0, 5, 5, 11, body)
    sk.box(32, 0, 5, 4, 4, head)
    sk.box(32, 8, 2, 2, 3, snout)
    sk.box(42, 8, 4, 1, 3, chin)
    sk.box(54, 0, 3, 5, 1, ear)
    sk.box(0, 16, 2, 5, 2, leg)
    sk.box(8, 16, 3, 3, 5, tail)
    sk.box(24, 16, 4, 4, 4, brush)
    sk.box(40, 16, 3, 3, 3, ember, tone=False)
    # Bright amber eyes with dark inner corners and a dark line running down from each; a dark nose.
    for x, inner in ((0, 1), (4, 3)):
        if glow:
            sk.at(32, 0, 5, 4, 4, "front", x, 1, rgba(FOX_EYE, 210))
        else:
            sk.at(32, 0, 5, 4, 4, "front", x, 1, FOX_EYE)
            sk.at(32, 0, 5, 4, 4, "front", inner, 1, hexc("#2A1810"))
            sk.at(32, 0, 5, 4, 4, "front", inner, 2, shade(SAND[1], 0.8))
    if not glow:
        sk.at(32, 8, 2, 2, 3, "front", 0, 0, hexc("#2A1C18"))
        sk.at(32, 8, 2, 2, 3, "front", 1, 0, hexc("#2A1C18"))
    return sk.image()


# ============================================================== the rimehare

SNOW = H("#C4D2E0", "#DAE5EF", "#EBF1F7", "#F7FAFD", "#FFFFFF")
FROST = hexc("#BCD6EE")
EAR_TIP = hexc("#3C4858")
RIME = hexc("#D2F2FF")


def rimehare_skin(glow=False):
    sk = Skin(64, 32)
    clear = None

    def sparkle(tx, ty, seed):
        return noise(tx, ty, seed) > 0.93

    def coat(face, X, Y, Z, tx, ty, h, seed):
        if face == "top":
            if glow:
                return rgba(hexc("#E8FAFF"), 120) if sparkle(tx, ty, seed) else clear
            c = mix(SNOW[3], FROST, 0.45 + 0.25 * noise(tx, ty, seed + 1))
            return SNOW[4] if sparkle(tx, ty, seed) else c
        if glow:
            return clear
        if face == "bottom":
            return SNOW[4]
        # White, shading a little toward the belly line, with flecks of frost-blue caught in the fur.
        t = Y / h
        c = along(SNOW, 0.95 - 0.55 * max(0.0, t - 0.55) / 0.45)
        if noise(tx, ty, seed + 5) > 0.9:
            c = mix(c, FROST, 0.6)
        return fur(c, tx, ty, seed + 2, 0.04)

    def body(face, X, Y, Z, tx, ty):
        return coat(face, X, Y, Z, tx, ty, 5, 60)

    def head(face, X, Y, Z, tx, ty):
        return coat(face, X, Y, Z, tx, ty, 4, 63)

    def muzzle(face, X, Y, Z, tx, ty):
        if glow:
            return clear
        return SNOW[4] if face != "top" else SNOW[3]

    def ear(face, X, Y, Z, tx, ty):
        if Y < 1.6:
            if glow:
                return rgba(hexc("#BFF0FF"), 255 if Y < 0.6 else 160)
            return RIME if Y < 0.6 else EAR_TIP
        if glow:
            return clear
        if face == "front" and 0.5 < X < 1.5 and Y > 2:
            return hexc("#E6D6DC")
        return fur(SNOW[3], tx, ty, 66, 0.04)

    def haunch(face, X, Y, Z, tx, ty):
        return coat(face, X, Y, Z, tx, ty, 4, 67)

    def foot(face, X, Y, Z, tx, ty):
        if glow:
            return clear
        return SNOW[1] if face == "bottom" else SNOW[3]

    def leg(face, X, Y, Z, tx, ty):
        if glow:
            return clear
        return fur(SNOW[3] if Y < 4 else SNOW[2], tx, ty, 70, 0.04)

    def tail(face, X, Y, Z, tx, ty):
        if glow:
            return clear
        return SNOW[4]

    sk.box(0, 0, 5, 5, 8, body)
    sk.box(26, 0, 4, 4, 4, head)
    sk.box(42, 0, 3, 2, 2, muzzle)
    sk.box(52, 0, 2, 6, 1, ear, tone=False)
    sk.box(0, 13, 2, 4, 5, haunch)
    sk.box(14, 13, 2, 1, 5, foot)
    sk.box(28, 13, 2, 5, 2, leg)
    sk.box(36, 13, 3, 3, 2, tail)
    # Big dark eyes on the sides of the head, ringed with ice; a pink nose.
    for face, x in (("right", 2), ("left", 1)):
        if glow:
            sk.at(26, 0, 4, 4, 4, face, x, 1, rgba(hexc("#9FE4FF"), 150))
        else:
            sk.at(26, 0, 4, 4, 4, face, x, 1, hexc("#1C2028"))
            sk.at(26, 0, 4, 4, 4, face, x, 2, hexc("#8FC8E4"))
    if not glow:
        sk.at(42, 0, 3, 2, 2, "front", 1, 0, hexc("#E7A2B0"))
        sk.at(42, 0, 3, 2, 2, "front", 1, 1, hexc("#A8B4C4"))
    return sk.image()


# ============================================================== the glimmerwing

# Each colouring: wing at the root, wing toward the tip, its veins, its eyespot's ring and heart, its fur, and its glow.
GLIMMER = {
    "moonlit": dict(root="#B4D2F2", tip="#E6F2FF", vein="#8CAAD6", ring="#46659F", heart="#F0FAFF", fur="#E6E2F2", fur2="#C9C2DE",
                    glow="#B8ECFF"),
    "rose": dict(root="#F0B6D2", tip="#FCE6F2", vein="#D28EB4", ring="#A8326E", heart="#FFF0F8", fur="#F4E2EC", fur2="#DCC0D2",
                 glow="#FFC6E6"),
    "amber": dict(root="#EECB80", tip="#FCEEC8", vein="#CA9E52", ring="#9E521C", heart="#FFF6DC", fur="#F4ECD8", fur2="#DCCCA6",
                  glow="#FFE2A0"),
}

# Wing shapes on their sheets (x from the root out, rows from the trailing edge to the leading one); '#' is wing.
FOREWING = ["###....", "#####..", "######.", "#######", "#######"]
HINDWING = ["###..", "#####", "#####", "####."]


def glimmerwing_skin(colouring, glow=False):
    p = {k: hexc(v) for k, v in GLIMMER[colouring].items()}
    sk = Skin(64, 32)
    clear = None

    def wing(shape, eyespot):
        rows = shape
        w = len(rows[0])
        d = len(rows)

        def inside(x, y):
            return 0 <= x < w and 0 <= y < d and rows[y][x] == "#"

        def paint(face, X, Y, Z, tx, ty):
            x = int(X)
            y = int(d - Z)
            if not inside(x, y):
                return clear
            edge = not all(inside(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if 0 <= x + dx)
            spot = eyespot and (x, y) in eyespot
            heart = eyespot and (x, y) == eyespot[0]
            if glow:
                if heart:
                    return rgba(p["heart"], 255)
                if edge and x > 0:
                    return rgba(p["glow"], 150)
                return clear
            if heart:
                return p["heart"]
            if spot:
                return p["ring"]
            c = mix(p["root"], p["tip"], X / w)
            if face == "bottom":
                c = mix(c, hexc("#FFFFFF"), 0.3)
            if y == d // 2 and face == "top" and 0 < x < w - 1:
                c = p["vein"]
            if edge:
                return rgba(mix(c, hexc("#FFFFFF"), 0.45), 255)
            return rgba(c, 215)
        return paint

    def thorax(face, X, Y, Z, tx, ty):
        if glow:
            return clear
        return fur(p["fur"] if face != "bottom" else p["fur2"], tx, ty, 80, 0.1)

    def abdomen(face, X, Y, Z, tx, ty):
        if face == "back" or Z > 3.4:
            # The lantern at the tip of its abdomen.
            return rgba(p["glow"], 255) if glow else mix(p["glow"], hexc("#FFFFFF"), 0.35)
        if glow:
            return clear
        return p["fur"] if int(Z) % 2 == 0 else p["fur2"]

    def head(face, X, Y, Z, tx, ty):
        if glow:
            return clear
        if face == "front":
            return hexc("#1E1C2A") if (X < 0.9 or X > 1.1) and Y < 1.2 else p["fur"]
        return fur(p["fur"], tx, ty, 81, 0.1)

    def antenna(face, X, Y, Z, tx, ty):
        # A feathery frond: a stem on the diagonal with barbs either side.
        stem = abs((3 - Y) - Z) < 0.6
        barb = abs((3 - Y) - Z) < 1.3 and (int(Y) + int(Z)) % 2 == 0
        if not (stem or barb):
            return clear
        if glow:
            return rgba(p["glow"], 120) if Y < 1 else clear
        return rgba(mix(p["fur2"], p["vein"], 0.4) if stem else p["fur"], 255)

    sk.box(0, 0, 2, 2, 2, thorax)
    sk.box(8, 0, 2, 2, 4, abdomen, tone=False)
    sk.box(20, 0, 2, 2, 2, head)
    sk.box(28, 0, 0, 3, 3, antenna, tone=False)
    sk.box(0, 6, 7, 0, 5, wing(FOREWING, [(4, 2), (3, 2), (4, 3), (3, 3)]), tone=False)
    sk.box(0, 11, 5, 0, 4, wing(HINDWING, [(2, 1)]), tone=False)
    return sk.image()


# ============================================================== the skyray

SKY = H("#1B2646", "#24355E", "#2E4878", "#3C5C90")
PEARL = H("#A9B6CC", "#C8D2E2", "#E2E8F2", "#F2F5FA")
TEAL = hexc("#3FA6B8")
STAR = hexc("#DCE8FF")

# Its constellations, in body pixels: (x across from the middle, z from the nose back). Mirrored on both sides.
SKY_STARS = [(1.2, -5.0), (2.4, -2.0), (1.0, 1.5), (2.8, 4.0), (6.5, -3.0), (10.5, 0.2), (14.0, -0.4), (18.0, 1.0), (8.4, 2.8)]


def wing_front(x):
    """The leading edge of a skyray's wing, in body pixels, x out from the body's side."""
    return -6 + 0.2 * x + 0.012 * x * x


def wing_back(x):
    return 5 - 0.08 * x - 0.003 * x * x


def skyray_skin(glow=False):
    sk = Skin(128, 64)
    clear = None

    def starry(bx, bz, r=0.75):
        ax = abs(bx)
        return any((ax - sx) ** 2 + (bz - sz) ** 2 < r * r for sx, sz in SKY_STARS)

    def body(face, X, Y, Z, tx, ty):
        bx, bz = X - 4, Z - 7
        if face == "top":
            if glow:
                return rgba(STAR, 230) if starry(bx, bz) else clear
            c = SKY[0] if abs(bx) < 0.9 else along(SKY, 0.65 - 0.12 * abs(bx))
            return STAR if starry(bx, bz) else fur(c, tx, ty, 140, 0.05)
        if glow:
            return clear
        if face == "bottom":
            # Pale underneath, with its gill slits in two rows.
            gill = 1.4 < abs(bx) < 2.6 and -3 < bz < 2 and int(bz + 10) % 2 == 0
            return PEARL[0] if gill else fur(along(PEARL, 0.75), tx, ty, 141, 0.03)
        return along(SKY, 0.55) if Y < 1.5 else along(PEARL, 0.6)

    def lobe(face, X, Y, Z, tx, ty):
        if glow:
            return clear
        if face == "top":
            return fur(along(SKY, 0.5), tx, ty, 142, 0.05)
        if face == "front":
            # The wide mouth.
            return hexc("#20283E") if Y > 0.9 and 0.5 < X < 5.5 else along(SKY, 0.6)
        if face == "bottom":
            return along(PEARL, 0.7)
        return along(SKY, 0.55)

    def horn(face, X, Y, Z, tx, ty):
        tip = Z < 1.2
        if glow:
            return rgba(hexc("#BFE0FF"), 200) if tip else clear
        return hexc("#C8DCFF") if tip else SKY[1]

    def sheet(x0, pivot_z, min_z, w, d):
        """A wing sheet, cut to the wing's outline, starry above and pearly below, its leading edge banded."""
        def paint(face, X, Y, Z, tx, ty):
            span = x0 + X
            bz = pivot_z + min_z + Z
            front, back = wing_front(span), wing_back(span)
            if not front <= bz <= back:
                return clear
            leading = bz - front < 0.7
            trailing = back - bz < 0.8
            if face == "top":
                if glow:
                    if starry(span + 4, bz, 0.55):
                        return rgba(STAR, 230)
                    if leading:
                        return rgba(hexc("#7FE6F0"), 150)
                    return clear
                if starry(span + 4, bz, 0.55):
                    return STAR
                if leading:
                    return mix(TEAL, SKY[2], 0.3)
                c = along(SKY, 0.62 - 0.4 * span / 24)
                return shade(c, 0.85) if trailing else fur(c, tx, ty, 143, 0.04)
            if glow:
                return clear
            c = along(PEARL, 0.8 - 0.45 * span / 24)
            return shade(c, 0.9) if leading or trailing else c
        return paint

    def root(face, X, Y, Z, tx, ty):
        if glow:
            return clear
        if face == "bottom":
            return along(PEARL, 0.75)
        if face == "top":
            return fur(along(SKY, 0.6), tx, ty, 144, 0.04)
        return along(SKY, 0.5) if Y < 1 else along(PEARL, 0.55)

    def tail(face, X, Y, Z, tx, ty):
        if glow:
            return clear
        return SKY[0] if face != "bottom" else SKY[1]

    def tail_tip(face, X, Y, Z, tx, ty):
        end = Z > 6.8
        if glow:
            return rgba(hexc("#BFE0FF"), 200) if end else clear
        return hexc("#C8DCFF") if end else SKY[0]

    sk.box(0, 0, 8, 3, 14, body)
    sk.box(44, 0, 6, 2, 3, lobe)
    sk.box(62, 0, 1, 1, 4, horn)
    # The inner sheet hangs from the wing's pivot (z -1 on the body) and spans z -5 to 6 from there; the middle sheet's
    # pivot is half a pixel further back, the tip's another pixel.
    sk.box(0, 17, 9, 0, 11, sheet(0, -1, -5, 9, 11), tone=False)
    sk.box(30, 17, 3, 2, 9, root)
    sk.box(54, 17, 8, 0, 8, sheet(9, -0.5, -4, 8, 8), tone=False)
    sk.box(78, 17, 7, 0, 4, sheet(17, 0.5, -2, 7, 4), tone=False)
    sk.box(0, 30, 1, 1, 10, tail)
    sk.box(22, 30, 1, 1, 8, tail_tip)
    # Small pale eyes on the sides of the head lobe.
    for face, x in (("right", 1), ("left", 1)):
        if glow:
            sk.at(44, 0, 6, 2, 3, face, x, 0, rgba(hexc("#CFE8FF"), 150))
        else:
            sk.at(44, 0, 6, 2, 3, face, x, 0, hexc("#E8F4FF"))
    return sk.image()


# ============================================================== icons

ICONS = {
    "glimmer_dust": ("""
        ................
        ..........w.....
        .....w...wWw....
        ....wWw...w.....
        .....w.......w..
        ............wWw.
        .......oooo..w..
        .....ooLLbboo...
        ....oLLbbBbbbo..
        ...oLbbbbbbBbo..
        ...obbBbbbbbbbo.
        ..obbbbbbBbbbdo.
        ..odbbbbbbbbddo.
        ...oddddddddoo..
        ....oooooooo....
        ................
    """, {"o": "#3A4A6C", "L": "#F4FAFF", "b": "#B8D4F2", "B": "#FFFFFF", "d": "#7C98C4", "w": "#D8ECFF", "W": "#FFFFFF"}),
    "lumen_antler": ("""
        ................
        ..........o.....
        ..o......oWo....
        .oWo....oWCo....
        .oCWo..oWCo.....
        ..oCWooWCo..o...
        ...oCCWCo..oWo..
        ....oCCCo.oWCo..
        .....oCCCoWCo...
        ......oCCCCo....
        .......oCCo.....
        ......obCo......
        .....obbo.......
        ....obbo........
        ....ooo.........
        ................
    """, {"o": "#1E4A66", "W": "#F4FFFF", "C": "#9EE0FF", "b": "#E6DDC8"}),
    "mossback_scute": ("""
        ................
        ................
        .....oooooo.....
        ....oMmMMmMo....
        ...oMMMmMMMmo...
        ..oMmrrrrrrMMo..
        ..orrRRRRRRrro..
        ..orRRSSSSRRro..
        ..orRSSDDSSRro..
        ..orRRSSSSRRro..
        ..orrRRRRRRrro..
        ...orrrrrrrro...
        ....oddddddo....
        .....oooooo.....
        ................
        ................
    """, {"o": "#241E12", "M": "#6F9C38", "m": "#3E6424", "r": "#4E4329", "R": "#7D7047", "S": "#978A5B", "D": "#B8A870",
          "d": "#3A3220"}),
    "ember_tuft": ("""
        ................
        .........y......
        ........yYy.....
        .......oyEyo....
        ......oEEeEo....
        ......oeEeeo....
        .....oaeeceo....
        ....oaaecceo....
        ....oAacaaeo....
        ...oAAaaaaao....
        ...oAAAaaaao....
        ...oAAaaaao.....
        ....oaaaao......
        .....oooo.......
        ................
        ................
    """, {"o": "#3A2014", "a": "#CB914F", "A": "#E2B272", "c": "#3E241A", "e": "#F0762A", "E": "#FFAA3A", "y": "#FFDA6A",
          "Y": "#FFF6C8"}),
    "skyray_membrane": ("""
        ................
        ................
        ..oooooo........
        .ottSbbbooo.....
        .otbbbbbSbboo...
        ..obbSbbbbbbbo..
        ..obbbbbbSbbbbo.
        ...obbbbbbbbSbo.
        ...obSbbbbbbbbo.
        ....obbbbSbbbo..
        ....obbbbbbbdo..
        .....obbSbbddo..
        .....obbbbddo...
        ......oddddo....
        .......oooo.....
        ................
    """, {"o": "#141C34", "b": "#2E4878", "d": "#22325A", "t": "#3FA6B8", "S": "#DCE8FF"}),
    "rime_fur": ("""
        ................
        ................
        ......o.o.......
        .....oWoWo..o...
        ....oWWoWWooWo..
        ...oWWWWWWWWWo..
        ...oWwWWfWWWWo..
        ..oWWWWWWWwWWo..
        ..oWfWWwWWWWo...
        ..oWWWWWWfWWo...
        ...owWWWWWWwo...
        ...osWwWWWsso...
        ....osssssso....
        .....oooooo.....
        ................
        ................
    """, {"o": "#5C6E82", "W": "#F4F8FC", "w": "#D2E2F0", "f": "#FFFFFF", "s": "#B8C8D8"}),
}


def icon(name):
    art, pal = ICONS[name]
    cv = Canvas()
    blit(cv, art, {k: hexc(v) for k, v in pal.items()})
    return cv.image()


# Spawn eggs: the egg's own colour, its speckles, and the shade it darkens to, then a few creature marks on top.
EGGS = {
    "glimmerwing": ("#C8DCF4", "#F4FAFF", "#7C98C4", "#2E3A58", [(6, 5), (9, 8), (5, 10), (10, 12)], "#FFE2A0"),
    "lumen_stag": ("#C2AF93", "#F6F0E2", "#8C7860", "#3A3022", [(7, 4), (5, 7), (9, 6), (8, 10), (6, 12)], "#9EE0FF"),
    "mossback_tortoise": ("#6E6240", "#6F9C38", "#3E3522", "#1E1A10", [(6, 4), (8, 5), (5, 6), (9, 7), (7, 8)], None),
    "cinderfox": ("#D29A58", "#F3DCB0", "#8E5A2C", "#3A2414", [(6, 10), (9, 11), (8, 12)], "#FFAA3A"),
    "skyray": ("#2E4878", "#DCE8FF", "#1B2646", "#0C1222", [(6, 5), (9, 4), (8, 8), (5, 9), (10, 10), (7, 12)], "#3FA6B8"),
    "rimehare": ("#F4F8FC", "#9CC2E4", "#AFC0D2", "#4C5C70", [(6, 6), (9, 9), (7, 11), (10, 5), (5, 10)], None),
}


def egg(creature):
    base, speck, dark, outline, specks, mark = (EGGS[creature][0], EGGS[creature][1], EGGS[creature][2], EGGS[creature][3],
                                                EGGS[creature][4], EGGS[creature][5])
    base, speck, dark, outline = hexc(base), hexc(speck), hexc(dark), hexc(outline)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = im.load()
    inside = set()
    for y in range(16):
        for x in range(16):
            # An egg: narrower at the top.
            cx, cy = 7.5, 8.6
            rx = 5.0 if y > cy else 5.0 - (cy - y) * 0.18
            if ((x - cx) / rx) ** 2 + ((y - cy) / 6.4) ** 2 <= 1.0:
                inside.add((x, y))
    for x, y in inside:
        edge = any((x + dx, y + dy) not in inside for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if edge:
            p[x, y] = outline + (255,)
            continue
        light = (x - 7.5) * -0.08 + (y - 8.6) * -0.07
        c = mix(base, dark, 0.5 - light * 2.4) if light < 0 else mix(base, hexc("#FFFFFF"), light * 1.2)
        p[x, y] = c + (255,)
    for x, y in specks:
        if (x, y) in inside:
            p[x, y] = speck + (255,)
            if (x + 1, y) in inside and creature in ("lumen_stag", "mossback_tortoise"):
                p[x + 1, y] = speck + (255,)
    if mark:
        m = hexc(mark)
        if creature == "glimmerwing":
            for x, y in ((4, 7), (11, 7), (5, 8), (10, 8)):
                p[x, y] = m + (255,)
        elif creature == "lumen_stag":
            for x, y in ((5, 3), (6, 2), (10, 3), (9, 2), (7, 3), (8, 3)):
                if (x, y) in inside:
                    p[x, y] = m + (255,)
        elif creature == "cinderfox":
            for x, y in ((7, 12), (8, 13), (7, 13)):
                p[x, y] = m + (255,)
        elif creature == "skyray":
            for x in range(4, 12):
                if (x, 7) in inside:
                    p[x, 7] = m + (255,)
    return im


def frost_print():
    """A hare's print in rime: a long pad and four toes, white (tinted pale blue in game), soft at the edges."""
    art = """
        ................
        ................
        ....+#+..+#+....
        ....+#+..+#+....
        .....+....+.....
        ..+#+......+#+..
        ..+#+......+#+..
        .....+####+.....
        ....+######+....
        ....+######+....
        ....+######+....
        .....+####+.....
        .....+####+.....
        ......+##+......
        .......++.......
        ................
    """
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = im.load()
    rows = [ln.strip() for ln in art.strip("\n").splitlines() if ln.strip()]
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in "#+":
                grain = 1 if noise(x, y, 150) > 0.3 else 0.8
                p[x, y] = (255, 255, 255, int((235 if ch == "#" else 120) * grain))
    return im


# ============================================================== data

def loot(entries):
    return {"type": "minecraft:entity", "pools": entries}


def drop(item, low, high, looting=1.0, player=False):
    entry = {"type": "minecraft:item", "name": item, "modifier": [
        {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}},
        {"type": "minecraft:enchanted_count_increase", "count": {"type": "minecraft:uniform", "min": 0.0, "max": looting},
         "enchantment": "minecraft:looting"}]}
    pool = {"rolls": 1, "entries": [entry]}
    if player:
        pool["condition"] = {"type": "minecraft:killed_by_player"}
    return pool


LOOT = {
    "glimmerwing": loot([drop("wildercord:glimmer_dust", 1, 1)]),
    # Antlers are only ever shed: a stag that dies leaves nothing at all.
    "lumen_stag": loot([]),
    "mossback_tortoise": loot([drop("wildercord:mossback_scute", 0, 1)]),
    "cinderfox": loot([drop("wildercord:ember_tuft", 0, 1)]),
    "skyray": loot([drop("wildercord:skyray_membrane", 1, 2)]),
    "rimehare": loot([drop("wildercord:rime_fur", 0, 2)]),
}

# What each creature may stand on to spawn naturally (block tags under data/wildercord/tags/block/spawns_on/).
SPAWNS_ON = {
    "lumen_stag": ["#minecraft:wolves_spawnable_on", "#minecraft:animals_spawnable_on", "minecraft:podzol", "minecraft:coarse_dirt"],
    "mossback_tortoise": ["#minecraft:frogs_spawnable_on", "#minecraft:animals_spawnable_on", "#minecraft:parrots_spawnable_on",
                          "minecraft:moss_block", "minecraft:mud", "minecraft:podzol", "minecraft:clay"],
    "cinderfox": ["#minecraft:camels_spawnable_on", "#minecraft:armadillo_spawnable_on", "#minecraft:sand", "#minecraft:badlands_terracotta"],
    # Not the rabbits' tag: that has sand in it, and a rimehare is a creature of the snow.
    "rimehare": ["minecraft:grass_block", "minecraft:snow", "minecraft:snow_block", "minecraft:powder_snow", "minecraft:ice", "minecraft:packed_ice",
                 "minecraft:podzol", "minecraft:coarse_dirt", "minecraft:dirt", "minecraft:stone"],
}

# Brews from an Awkward Potion: (the creature's gift, the potion it makes).
BREWS = [("glimmer_dust", "minecraft:night_vision"), ("mossback_scute", "minecraft:turtle_master"),
         ("ember_tuft", "minecraft:fire_resistance"), ("skyray_membrane", "minecraft:slow_falling")]

DROPS = ["glimmer_dust", "lumen_antler", "mossback_scute", "ember_tuft", "skyray_membrane", "rime_fur"]

LANG = {
    "entity.wildercord.glimmerwing": "Glimmerwing",
    "entity.wildercord.lumen_stag": "Lumen Stag",
    "entity.wildercord.mossback_tortoise": "Mossback Tortoise",
    "entity.wildercord.cinderfox": "Cinderfox",
    "entity.wildercord.skyray": "Skyray",
    "entity.wildercord.rimehare": "Rimehare",
    **{f"item.wildercord.{c}_spawn_egg": f"{n} Spawn Egg" for c, n in (("glimmerwing", "Glimmerwing"), ("lumen_stag", "Lumen Stag"),
        ("mossback_tortoise", "Mossback Tortoise"), ("cinderfox", "Cinderfox"), ("skyray", "Skyray"), ("rimehare", "Rimehare"))},
    "item.wildercord.glimmer_dust": "Glimmer Dust",
    "item.wildercord.glimmer_dust.lore": "Shaken from a moth's wing. It still remembers the lamp it circled.",
    "item.wildercord.glimmer_dust.use": "Brews Night Vision from an Awkward Potion; with an ink sac, makes a glow ink sac",
    "item.wildercord.lumen_antler": "Lumen Antler",
    "item.wildercord.lumen_antler.lore": "Shed, never taken. It holds a little of the moon it grew under.",
    "item.wildercord.lumen_antler.use": "Sits at the heart of a Mana Crystal in place of the diamond",
    "item.wildercord.mossback_scute": "Mossback Scute",
    "item.wildercord.mossback_scute.lore": "A plate of old shell, its moss still green.",
    "item.wildercord.mossback_scute.use": "Brews the Turtle Master from an Awkward Potion; mends a turtle shell",
    "item.wildercord.ember_tuft": "Ember Tuft",
    "item.wildercord.ember_tuft.lore": "A tuft from a cinderfox's tail, warm to the touch and smouldering at the tip.",
    "item.wildercord.ember_tuft.use": "Brews Fire Resistance from an Awkward Potion; burns long in a furnace",
    "item.wildercord.skyray_membrane": "Skyray Membrane",
    "item.wildercord.skyray_membrane.lore": "A shed scrap of wing, light as a breath, with stars in it.",
    "item.wildercord.skyray_membrane.use": "Brews Slow Falling from an Awkward Potion; mends an elytra",
    "item.wildercord.rime_fur": "Rime Fur",
    "item.wildercord.rime_fur.lore": "Frost never quite melts out of it.",
    "item.wildercord.rime_fur.use": "Weaves Rimebound armour in place of packed ice; four make leather",
    "message.wildercord.lumen_stag.shed": "The stag bows its head, and an antler falls at your feet",
    "message.wildercord.lumen_stag.curse": "The forest falls silent around you. Bad luck follows",
    "message.wildercord.cinderfox.brushed": "Its tail has given all its embers for today",
    # The Grimoire's field guide.
    "screen.wildercord.grimoire.field_guide": "Creatures (%s of %s met)",
    "screen.wildercord.grimoire.field_guide_about": "Creatures you've seen up close. Each one you meet for the first time condenses a little mana",
    "screen.wildercord.grimoire.field_guide_unknown": "??? %s",
    "screen.wildercord.grimoire.field_guide_wildlife": "Wildlife",
    "screen.wildercord.grimoire.field_guide_monsters": "Monsters",
    "toast.wildercord.creature": "Met: %s",
    "guide.wildercord.glimmerwing": "Soft-glowing moths that flutter in small swarms at night in forests and flower fields, circling lamps and anyone fresh from a spell. Drops Glimmer Dust",
    "guide.wildercord.glimmerwing.hint": "Something glows among the trees on a dark night",
    "guide.wildercord.lumen_stag": "A rare, shy deer of old forests whose crystal antlers burn brighter as the moon fills. Approach it sneaking and still and it sheds an antler for you, once a day. Never harm one",
    "guide.wildercord.lumen_stag.hint": "Old woods, a moonlit glint, and patience",
    "guide.wildercord.mossback_tortoise": "A huge, slow tortoise of swamps, mangroves and jungles with a garden on its shell. Hides when struck; loves melon; now and then leaves a Mossback Scute",
    "guide.wildercord.mossback_tortoise.hint": "A garden that walks, where the ground is wet",
    "guide.wildercord.cinderfox": "A desert fox whose tail ends in a living ember. Tame it with rabbit or fish; its bite sets fire-weak foes alight. Brush it for an Ember Tuft",
    "guide.wildercord.cinderfox.hint": "Sparks on the sand after dark",
    "guide.wildercord.skyray": "A manta of the open sky, gliding in slow loops high over mountains and meadows. Its back is full of stars at night. Now and then a membrane falls from it",
    "guide.wildercord.skyray.hint": "Look up, above the peaks",
    "guide.wildercord.rimehare": "A quick snow hare that leaves fleeting frost prints. It bolts unless you hold out sweet berries. Drops Rime Fur",
    "guide.wildercord.rimehare.hint": "Tracks in the frost that fade as you look",
}

# The feel kit's subtitles for the creatures' own voices (tools/feel/wildlife.py picks them by name).
SUBTITLES = {
    "glimmerwing_flutter": "Glimmerwing flutters",
    "glimmerwing_hurt": "Glimmerwing scatters",
    "stag_call": "Lumen Stag calls",
    "stag_hurt": "Lumen Stag cries",
    "stag_death": "Lumen Stag falls",
    "stag_shed": "Antler falls",
    "tortoise_grumble": "Mossback grumbles",
    "tortoise_hurt": "Mossback hurts",
    "tortoise_death": "Mossback dies",
    "tortoise_hide": "Mossback hides",
    "tortoise_step": "Mossback plods",
    "tortoise_scute": "Scute drops",
    "cinderfox_yip": "Cinderfox yips",
    "cinderfox_hurt": "Cinderfox yelps",
    "cinderfox_death": "Cinderfox dies",
    "cinderfox_spark": "Embers catch",
    "skyray_call": "Skyray sings",
    "skyray_hurt": "Skyray cries",
    "skyray_death": "Skyray falls",
    "rimehare_squeak": "Rimehare squeaks",
    "rimehare_hurt": "Rimehare cries",
    "rimehare_hop": "Rimehare hops",
}
LANG.update({f"subtitles.wildercord.kit.{k}": v for k, v in SUBTITLES.items()})


# ============================================================== writing

def skins():
    """Every entity texture: name -> image."""
    out = {"lumen_stag": stag_skin(), "lumen_stag_glow": stag_skin(True), "mossback_tortoise": tortoise_skin(),
           "mossback_tortoise_glow": tortoise_skin(True), "cinderfox": cinderfox_skin(), "cinderfox_glow": cinderfox_skin(True),
           "rimehare": rimehare_skin(), "rimehare_glow": rimehare_skin(True), "skyray": skyray_skin(), "skyray_glow": skyray_skin(True)}
    for colouring in GLIMMER:
        out[f"glimmerwing_{colouring}"] = glimmerwing_skin(colouring)
        out[f"glimmerwing_{colouring}_glow"] = glimmerwing_skin(colouring, True)
    return out


def write(g):
    for name, image in skins().items():
        g.save(image, g.ASSETS / f"textures/entity/wildlife/{name}.png")
    g.save(frost_print(), g.ASSETS / "textures/particle/frost_print.png")

    for item in DROPS:
        g.save(icon(item), g.ASSETS / f"textures/item/{item}.png")
        g.item_model(item, item)
        g.write_json(g.ASSETS / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{item}"}})
    for creature in CREATURES:
        name = f"{creature}_spawn_egg"
        g.save(egg(creature), g.ASSETS / f"textures/item/{name}.png")
        g.item_model(name, name)
        g.write_json(g.ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"wildercord:item/{name}"}})
        g.write_json(g.DATA / f"loot_table/entities/{creature}.json", LOOT[creature])

    for creature, ground in SPAWNS_ON.items():
        g.write_json(g.DATA / f"tags/block/spawns_on/{creature}.json", {"values": ground})
    g.write_json(g.RES / "data/minecraft/tags/item/repairs_turtle_helmet.json", {"replace": False, "values": ["wildercord:mossback_scute"]})

    for item, potion in BREWS:
        for container in ("potion", "splash_potion", "lingering_potion"):
            g.brewing(f"{container}_awkward_{item}", f"minecraft:{container}", "minecraft:awkward", f"wildercord:{item}",
                      f"minecraft:{container}", potion)

    g.write_json(g.DATA / "recipe/glow_ink_sac_from_glimmer_dust.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": ["wildercord:glimmer_dust", "minecraft:ink_sac"], "result": {"id": "minecraft:glow_ink_sac"}})
    g.unlock_advancement("wildercord:glow_ink_sac_from_glimmer_dust", "wildercord:glimmer_dust")
    g.write_json(g.DATA / "recipe/mana_crystal_from_lumen_antler.json", {"type": "minecraft:crafting_shaped", "category": "misc",
        "key": {"L": "minecraft:lapis_lazuli", "A": "minecraft:amethyst_shard", "X": "wildercord:lumen_antler"},
        "pattern": ["LAL", "AXA", "LAL"], "result": {"id": "wildercord:mana_crystal"}})
    g.unlock_advancement("wildercord:mana_crystal_from_lumen_antler", "wildercord:lumen_antler")
    g.write_json(g.DATA / "recipe/leather_from_rime_fur.json", {"type": "minecraft:crafting_shaped", "category": "misc",
        "key": {"F": "wildercord:rime_fur"}, "pattern": ["FF", "FF"], "result": {"id": "minecraft:leather"}})
    g.unlock_advancement("wildercord:leather_from_rime_fur", "wildercord:rime_fur")
    for slot in ("helmet", "chestplate", "leggings", "boots"):
        recipe = f"rimebound_{slot}_from_rime_fur"
        g.write_json(g.DATA / f"recipe/{recipe}.json", {"type": "minecraft:crafting_shapeless", "category": "equipment",
            "ingredients": [f"minecraft:leather_{slot}", "wildercord:rime_fur", "wildercord:mana_crystal"],
            "result": {"id": f"wildercord:rimebound_{slot}", "count": 1}})
        g.unlock_advancement(f"wildercord:{recipe}", "wildercord:rime_fur")


def preview(out_dir):
    """A review sheet: every skin and glow at 4x on a dark ground, then the icons, eggs and the print at 8x."""
    images = skins()
    cells = []
    for name, im in images.items():
        scale = 4 if im.width <= 128 else 2
        cells.append(im.resize((im.width * scale, im.height * scale), Image.NEAREST))
    small = [icon(i) for i in DROPS] + [egg(c) for c in CREATURES] + [frost_print()]
    width = max(c.width for c in cells) * 2 + 24
    rows = [cells[i:i + 2] for i in range(0, len(cells), 2)]
    height = sum(max(c.height for c in row) + 8 for row in rows) + 16 * 8 + 24
    sheet = Image.new("RGBA", (width, height), (46, 42, 56, 255))
    y = 8
    for row in rows:
        x = 8
        for c in row:
            back = Image.new("RGBA", c.size, (70, 66, 82, 255))
            back.alpha_composite(c)
            sheet.paste(back, (x, y))
            x += c.width + 8
        y += max(c.height for c in row) + 8
    x = 8
    for im in small:
        big = im.resize((128, 128), Image.NEAREST)
        back = Image.new("RGBA", big.size, (70, 66, 82, 255))
        back.alpha_composite(big)
        sheet.paste(back, (x, y))
        x += 136
    out = Path(out_dir) / "wildlife_art.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    return str(out)


if __name__ == "__main__":
    print(preview(str(ROOT / "build/art-preview")))
