"""Techniques of one's own: every part's glyph (GUI sprites the writing page tints by the part's kind), the seals the page threads them
between, the technique scroll (one item, its seal coloured by the kind of part it carries), and every word the techniques say. Called by
generate_assets.py (write(g) with the rest of the pack, LANG with the language file).

The glyphs are drawn by code at 16x16 in white and greys, one pixel at a time (straight lines, arcs and filled shapes), and given a dark
outline afterwards, so the page can tint them (a stroke's ember red, a release's sky blue, an intent's gold, a dark grey for a part not
yet known): a lance for the thrust, an arc climbing for the rising cut and falling for the falling cut, a wide arc for the sweep, a turning
circle for the spin, a fast level slash for the draw; a blade for the cut on the blade, a crescent flying for the wave, a ring breaking
out for the burst, two figures for the afterimage; an arrow through a bar for pierce, a cracked shield for sunder, two links for bind,
arcs going out for echo, a shell for ward, a pennant calling for rally, a drop turning for infuse. The seal is a round gold frame with a
notched rim. Everything is seeded and drawn by code, like the rest of the mod's art.

    python tools/technique_art.py      renders a review sheet into build/art-preview/technique_art.png
"""
import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent

# The parts, in the writing page's order (aura.TechniqueRules): kept in step by TechniqueRulesTest.
STROKES = ["thrust", "rising_cut", "falling_cut", "sweep", "spin", "draw"]
RELEASES = ["on_the_blade", "wave", "burst", "afterimage"]
INTENTS = ["pierce", "sunder", "bind", "echo", "ward", "rally", "infuse"]
PARTS = STROKES + RELEASES + INTENTS
# Each kind's colour (aura.Techniques.familyColor).
FAMILY = {"stroke": 0xE07A5A, "release": 0x7AB8E8, "intent": 0xE8C46A}

# Grey levels (tinted on the page): white, highlight, mid, deep, and the outline.
LEVELS = {4: (255, 255, 255, 255), 3: (222, 222, 228, 255), 2: (176, 176, 186, 255), 1: (124, 124, 136, 255)}
OUTLINE = (30, 26, 38, 255)


class Grid:
    """A 16x16 glyph being drawn: a grey level per pixel (0 none), the highest kept where strokes cross."""

    def __init__(self, size=16):
        self.size = size
        self.cells = [[0] * size for _ in range(size)]

    def px(self, x, y, v=4):
        x, y = int(round(x)), int(round(y))
        if 0 <= x < self.size and 0 <= y < self.size:
            self.cells[y][x] = max(self.cells[y][x], v)

    def clear(self, x, y):
        if 0 <= x < self.size and 0 <= y < self.size:
            self.cells[y][x] = 0

    def line(self, x0, y0, x1, y1, v=4):
        """A one-pixel line (Bresenham)."""
        x0, y0, x1, y1 = int(round(x0)), int(round(y0)), int(round(x1)), int(round(y1))
        dx, dy = abs(x1 - x0), -abs(y1 - y0)
        sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
        err = dx + dy
        while True:
            self.px(x0, y0, v)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * err
            if e2 >= dy:
                err += dy
                x0 += sx
            if e2 <= dx:
                err += dx
                y0 += sy

    def arc(self, cx, cy, r, a0, a1, v=4):
        """An arc of a circle round (cx, cy), from a0 to a1 degrees (0 is +x, 90 is down the page)."""
        steps = max(8, int(abs(a1 - a0) * r / 12))
        last = None
        for i in range(steps + 1):
            a = math.radians(a0 + (a1 - a0) * i / steps)
            p = (int(round(cx + r * math.cos(a))), int(round(cy + r * math.sin(a))))
            if last is not None and p != last:
                self.line(last[0], last[1], p[0], p[1], v)
            last = p

    def disc(self, cx, cy, r, v=4):
        for y in range(self.size):
            for x in range(self.size):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                    self.px(x, y, v)

    def poly(self, points, v=4):
        """A filled polygon (even-odd, pixel centres)."""
        for y in range(self.size):
            for x in range(self.size):
                inside = False
                n = len(points)
                for i in range(n):
                    (ax, ay), (bx, by) = points[i], points[(i + 1) % n]
                    if (ay > y + 0.5) != (by > y + 0.5):
                        t = (y + 0.5 - ay) / (by - ay)
                        if x + 0.5 < ax + t * (bx - ax):
                            inside = not inside
                if inside:
                    self.px(x, y, v)

    def image(self, outline=True):
        img = Image.new("RGBA", (self.size, self.size), (0, 0, 0, 0))
        for y in range(self.size):
            for x in range(self.size):
                v = self.cells[y][x]
                if v:
                    img.putpixel((x, y), LEVELS[v])
        if outline:
            for y in range(self.size):
                for x in range(self.size):
                    if self.cells[y][x]:
                        continue
                    for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        nx, ny = x + ox, y + oy
                        if 0 <= nx < self.size and 0 <= ny < self.size and self.cells[ny][nx]:
                            img.putpixel((x, y), OUTLINE)
                            break
        return img


def _arrowhead(g, x, y, dx, dy, v=4):
    """A small solid arrowhead with its tip at (x, y), pointing along (dx, dy)."""
    n = math.hypot(dx, dy) or 1
    ux, uy = dx / n, dy / n
    px, py = -uy, ux
    g.poly([(x + ux * 0.8 + 0.5, y + uy * 0.8 + 0.5), (x - ux * 3.2 + px * 2.6 + 0.5, y - uy * 3.2 + py * 2.6 + 0.5),
            (x - ux * 3.2 - px * 2.6 + 0.5, y - uy * 3.2 - py * 2.6 + 0.5)], v)


def glyph(part):
    g = Grid()
    if part == "thrust":
        # A lance driven straight ahead: guard, shaft, a long point.
        g.line(1, 7, 1, 9, 1)
        g.line(3, 4, 3, 12, 2)
        g.line(4, 8, 10, 8, 4)
        g.line(4, 9, 10, 9, 3)
        g.poly([(10.5, 5.5), (15.8, 8.5), (10.5, 11.5)], 4)
        g.px(13, 8, 3)
    elif part == "rising_cut":
        # An arc climbing from low on the left to high on the right.
        g.arc(14, 15, 11, 180, 262, 4)
        g.arc(14, 15, 10, 182, 262, 3)
        _arrowhead(g, 14, 3, 1, -0.15, 4)
        g.line(3, 14, 4, 15, 2)
    elif part == "falling_cut":
        # Down from high on the left to low on the right.
        g.arc(2, 14, 11, 272, 348, 4)
        g.arc(2, 14, 10, 274, 346, 3)
        _arrowhead(g, 13, 14, 0.2, 1, 4)
        g.line(1, 3, 2, 2, 2)
    elif part == "sweep":
        # A wide level arc round the front.
        g.arc(8, 0, 11, 28, 152, 4)
        g.arc(8, 0, 10, 32, 148, 3)
        _arrowhead(g, 1, 5, -0.6, -1, 4)
        g.line(14, 5, 15, 4, 2)
    elif part == "spin":
        # A whole turn.
        g.arc(7.5, 8, 5.5, 20, 300, 4)
        g.arc(7.5, 8, 4.5, 24, 296, 3)
        _arrowhead(g, 12, 3.4, 1, 0.55, 4)
        g.px(7, 8, 2)
        g.px(8, 8, 2)
    elif part == "draw":
        # A fast level slash out of the sheath, the air streaming behind it.
        g.line(6, 8, 14, 7, 4)
        g.line(6, 9, 13, 8, 3)
        g.px(15, 7, 4)
        g.line(1, 5, 4, 5, 2)
        g.line(0, 8, 4, 8, 2)
        g.line(1, 11, 4, 11, 1)
    elif part == "on_the_blade":
        # A blade, point up to the right, a gleam on its edge.
        for i in range(10):
            g.px(3 + i, 12 - i, 4)
            g.px(4 + i, 12 - i, 3)
        g.px(13, 2, 4)
        g.px(14, 1, 3)
        g.line(1, 10, 5, 14, 1)
        g.line(1, 15, 2, 14, 2)
        g.px(10, 4, 4)
        g.px(11, 5, 4)
    elif part == "wave":
        # A crescent thrown on, the air streaming behind.
        g.arc(4, 8, 8, -58, 58, 4)
        g.arc(4, 8, 7, -52, 52, 3)
        g.arc(4, 8, 6, -40, 40, 2)
        g.line(1, 5, 3, 5, 2)
        g.line(0, 8, 3, 8, 1)
        g.line(1, 11, 3, 11, 2)
    elif part == "burst":
        # A heart of light and a ring breaking out round it.
        g.disc(7.5, 7.5, 1.7, 4)
        for a in range(0, 360, 45):
            r0, r1 = 3.5, 6.8
            x0, y0 = 7.5 + r0 * math.cos(math.radians(a)), 7.5 + r0 * math.sin(math.radians(a))
            x1, y1 = 7.5 + r1 * math.cos(math.radians(a)), 7.5 + r1 * math.sin(math.radians(a))
            g.line(x0, y0, x1, y1, 4 if a % 90 == 0 else 2)
        g.arc(7.5, 7.5, 4.5, 20, 70, 3)
        g.arc(7.5, 7.5, 4.5, 200, 250, 3)
    elif part == "afterimage":
        # A figure, and its afterimage standing behind it.
        def figure(ox, v, dither):
            for (x, y) in [(ox + 2, 1), (ox + 3, 1), (ox + 2, 2), (ox + 3, 2)]:
                g.px(x, y, v)
            for y in range(4, 10):
                for x in range(ox + 1, ox + 5):
                    if not dither or (x + y) % 2 == 0:
                        g.px(x, y, v)
            for y in range(10, 15):
                for x in (ox + 1, ox + 4):
                    if not dither or (x + y) % 2 == 0:
                        g.px(x, y, v)
        figure(1, 1, True)
        figure(9, 4, False)
        g.line(9, 5, 7, 7, 3)
    elif part == "pierce":
        # An arrow driven through a bar.
        g.line(9, 2, 9, 13, 1)
        g.line(10, 2, 10, 13, 2)
        g.line(1, 8, 13, 8, 4)
        g.line(1, 7, 3, 7, 3)
        g.line(1, 9, 3, 9, 3)
        _arrowhead(g, 15, 8, 1, 0, 4)
    elif part == "sunder":
        # A shield split by a crack.
        g.poly([(2.5, 2.5), (13.5, 2.5), (13.5, 8.5), (8, 14.5), (2.5, 8.5)], 2)
        g.line(2, 2, 13, 2, 4)
        g.line(2, 2, 2, 8, 3)
        for (x, y) in [(8, 2), (7, 3), (8, 4), (9, 5), (8, 6), (7, 7), (8, 8), (9, 9), (8, 10), (8, 11), (7, 12)]:
            g.clear(x, y)
        g.px(5, 4, 4)
    elif part == "bind":
        # Two links of a chain.
        g.arc(5.5, 8, 3.5, 0, 360, 4)
        g.arc(10.5, 8, 3.5, 0, 360, 3)
        g.px(8, 5, 4)
        g.px(8, 11, 3)
    elif part == "echo":
        # A strike and its echoes going out.
        g.line(1, 4, 1, 12, 4)
        g.arc(1, 8, 5, -48, 48, 4)
        g.arc(1, 8, 9, -40, 40, 3)
        g.arc(1, 8, 13, -30, 30, 2)
    elif part == "ward":
        # A shell round its swordsman.
        g.poly([(3, 2), (13, 2), (13, 9), (8, 14.5), (3, 9)], 2)
        g.line(3, 2, 12, 2, 4)
        g.line(3, 2, 3, 9, 4)
        g.line(4, 3, 4, 8, 3)
        g.poly([(6, 5), (10, 5), (10, 8), (8, 10.5), (6, 8)], 3)
    elif part == "rally":
        # A pennant on its pole, and its call going out.
        g.line(2, 1, 2, 15, 1)
        g.line(3, 1, 3, 15, 2)
        g.poly([(4, 1.5), (11, 4.5), (4, 7.5)], 4)
        g.arc(9, 11, 2.5, -60, 60, 3)
        g.arc(9, 11, 4.5, -50, 50, 2)
    elif part == "infuse":
        # A drop of the method's own element, turning.
        g.disc(8, 10, 4.2, 3)
        g.poly([(4.2, 9.2), (8, 1), (11.8, 9.2)], 3)
        g.arc(8, 10, 2.2, 160, 420, 4)
        g.px(6, 7, 4)
    else:
        # A part without a glyph of its own (an add-on's): a plain diamond.
        g.poly([(8, 1), (15, 8), (8, 15), (1, 8)], 2)
        g.poly([(8, 4), (12, 8), (8, 12), (4, 8)], 4)
    return g.image()


def seal(lit):
    """The round frame a part sits in on the writing page: a gold rim notched like a seal's, a dark heart."""
    size = 26
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    c = (size - 1) / 2
    gold = (232, 196, 106, 255) if lit else (196, 160, 84, 255)
    hi = (255, 236, 168, 255) if lit else (226, 196, 120, 255)
    deep = (118, 86, 40, 255)
    heart = (20, 16, 28, 255)
    ring = (40, 32, 52, 255)
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - c, y - c)
            a = math.degrees(math.atan2(y - c, x - c)) % 360
            notch = (a % 30) < 6
            if d <= 9.4:
                img.putpixel((x, y), heart)
                if 8.2 < d <= 9.4:
                    img.putpixel((x, y), ring)
            elif d <= 12.4:
                if notch and d > 11.4:
                    continue
                shade = hi if (y < c - 2 and x < c + 3) else deep if (y > c + 3 and x > c - 3) else gold
                img.putpixel((x, y), shade)
            elif d <= 13.0 and not notch:
                img.putpixel((x, y), (30, 22, 14, 255))
    if lit:
        # A soft glow on the heart's rim.
        for y in range(size):
            for x in range(size):
                d = math.hypot(x - c, y - c)
                if 7.4 < d <= 8.2:
                    img.putpixel((x, y), (72, 58, 34, 255))
    return img


SCROLL = """
    ................
    ................
    ................
    .oo..........oo.
    oRRooooooooooRRo
    oRrPPPPPPPPPPrRo
    oRrPllPlllPPPrRo
    oRrPPPPPPPPPPrRo
    oRrPlllPPllPPrRo
    oRrPPPPoSSoPPrRo
    oRrPPPoSHsSoPrRo
    oRRoooSssssSoRRo
    .oo...oSsSSo.oo.
    .......oSSo.....
    ........tt......
    .......t..t.....
"""


def _grid(text):
    rows = [r.strip() for r in text.strip("\n").split("\n")]
    return [r for r in rows if r]


def _rgb(c):
    return (c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF


def _tone(c, k):
    r, g, b = _rgb(c)
    return (min(255, round(r * k)), min(255, round(g * k)), min(255, round(b * k)), 255)


def scroll_icon(color):
    """A technique scroll: parchment rolled at both ends, lines of ink, a wax seal of its part's kind and a ribbon under it."""
    pal = {
        "o": (44, 32, 24, 255),
        "R": (150, 112, 70, 255),
        "r": (196, 156, 102, 255),
        "P": (232, 220, 190, 255),
        "l": (120, 104, 84, 255),
        "S": _tone(color, 0.95),
        "s": _tone(color, 0.62),
        "H": _tone(color, 1.25),
        "t": _tone(color, 0.75),
    }
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(_grid(SCROLL)):
        for x, ch in enumerate(row[:16]):
            if ch in pal:
                img.putpixel((x, y), pal[ch])
    return img


def write(g):
    sprites = g.ASSETS / "textures/gui/sprites/technique"
    for part in PARTS + ["unknown"]:
        g.save(glyph(part), sprites / f"{part}.png")
    g.save(seal(False), sprites / "seal.png")
    g.save(seal(True), sprites / "seal_open.png")
    tex = g.ASSETS / "textures/item"
    # One item, its part a component; the seal's colour by the part's kind.
    cases = []
    for family, parts in (("stroke", STROKES), ("release", RELEASES), ("intent", INTENTS)):
        g.save(scroll_icon(FAMILY[family]), tex / f"technique_scroll/{family}.png")
        g.item_model(f"technique_scroll/{family}", f"technique_scroll/{family}")
        for part in parts:
            cases.append({"when": part, "model": {"type": "minecraft:model", "model": f"wildercord:item/technique_scroll/{family}"}})
    g.save(scroll_icon(0x8A84A0), tex / "technique_scroll/unknown.png")
    g.item_model("technique_scroll/unknown", "technique_scroll/unknown")
    g.write_json(g.ASSETS / "items/technique_scroll.json", {"model": {
        "type": "minecraft:select", "property": "minecraft:component", "component": "wildercord:technique_part", "cases": cases,
        "fallback": {"type": "minecraft:model", "model": "wildercord:item/technique_scroll/unknown"}}})


# ============================================================== English

PART_TEXT = {
    # ---- strokes
    "thrust": ("Thrust", "A straight lance of a blow down your look, up and down with it: long and narrow, its first foe taking the most."),
    "rising_cut": ("Rising Cut", "Up across the foes in front, throwing them into the air."),
    "falling_cut": ("Falling Cut", "Overhead and down onto one or two: narrow and heavy, and hard on a stance."),
    "sweep": ("Sweep", "Wide and level round the front: many foes, a little each."),
    "spin": ("Spin", "A whole turn: every foe round about you."),
    "draw": ("Draw", "A fast level cut out of the sheath: the lightest stroke, and the cheapest. Every blade knows it from Edge."),
    # ---- releases
    "on_the_blade": ("On the Blade", "The stroke lands at once, where your blade reaches. Every blade knows it from Edge."),
    "wave": ("Wave", "The stroke thrown on as a crescent of aura flying seven blocks past its reach (a spin's as a ring racing out), a little "
                     "lighter; a wall stops it."),
    "burst": ("Burst", "The stroke's force breaking out all round you at once: lighter, but nobody near is left out."),
    "afterimage": ("Afterimage", "A lighter stroke now, and an afterimage left standing where you struck it, striking again from there a "
                                 "moment later. The Way of the Shadowstep lends it."),
    # ---- intents
    "pierce": ("Pierce", "Through whatever stands in the way: further, and three more foes, a little lighter. The Way of the Blade lends it."),
    "sunder": ("Sunder", "Wears a stance two and a half times as hard as an art, to open a foe for your finisher."),
    "bind": ("Bind", "Roots each foe it strikes where it stands for a moment (a player only briefly)."),
    "echo": ("Echo", "Strikes each foe again a moment later for half as much, wherever it has gone, if it's still near."),
    "ward": ("Ward", "You take a fifth less from foes for four seconds after. The Way of the Bulwark's own: only it lends it."),
    "rally": ("Rally", "You and your allies near take a tenth less from foes for five seconds, and allied swordsmen build momentum. The "
                       "Way of the Banner's own: only it lends it."),
    "infuse": ("Infuse", "Your method's own element, twice as strong: an Ember technique burns longer, a Rime one chills deeper, and so on. "
                         "Every blade knows it from Edge."),
}


def _parts():
    out = {}
    for part, (name, desc) in PART_TEXT.items():
        out[f"aura.wildercord.technique_part.{part}"] = name
        out[f"aura.wildercord.technique_part.{part}.desc"] = desc
    return out


LANG = {
    # ---- the writing page
    "screen.wildercord.aura.writing": "Writing",
    "screen.wildercord.aura.writing.header": "%s · %s · %s/%s aura",
    "screen.wildercord.aura.writing.stroke": "Stroke",
    "screen.wildercord.aura.writing.release": "Release",
    "screen.wildercord.aura.writing.intent": "Intent",
    "screen.wildercord.aura.writing.stroke.word": "stroke",
    "screen.wildercord.aura.writing.release.word": "release",
    "screen.wildercord.aura.writing.intent.word": "intent",
    "screen.wildercord.aura.writing.slot_1": "your first slot",
    "screen.wildercord.aura.writing.slot_2": "your second slot",
    "screen.wildercord.aura.writing.slot_3": "your third slot",
    "screen.wildercord.aura.writing.opens": "Opens at %s",
    "screen.wildercord.aura.writing.empty": "Write a technique",
    "screen.wildercord.aura.writing.empty_tip": "An empty slot: compose a technique below and write it in.",
    "screen.wildercord.aura.writing.locked_tip": "A second slot opens at Form and a third at Sovereign. This one opens at %s.",
    "screen.wildercord.aura.writing.card_parts": "%s · %s · %s",
    "screen.wildercord.aura.writing.card_rank": "%s: %s / %s",
    "screen.wildercord.aura.writing.slot_closed": "Rests until you reach %s",
    "screen.wildercord.aura.writing.broken": "One of its parts is gone (an add-on's, removed): write it again",
    "screen.wildercord.aura.writing.part_missing": "Rests: you don't know the %s now (its Way is no longer yours)",
    "screen.wildercord.aura.writing.name_tip": "Click to name it, then type. Enter when you're done.",
    "screen.wildercord.aura.writing.name_suggest": "Left empty, it's called %s.",
    "screen.wildercord.aura.writing.where.learned": "Learned",
    "screen.wildercord.aura.writing.where.innate": "Every blade knows it",
    "screen.wildercord.aura.writing.where.innate_later": "Every blade knows it from %s",
    "screen.wildercord.aura.writing.where.lent": "Lent by the %s while you walk it",
    "screen.wildercord.aura.writing.where.way_only": "Only the %s lends it",
    "screen.wildercord.aura.writing.where.scroll_or_way": "Found on technique scrolls, or lent by the %s",
    "screen.wildercord.aura.writing.where.scroll": "Found on technique scrolls in old places, or shown by a duelist you beat",
    "screen.wildercord.aura.writing.where.own": "An add-on's own: learned where it says",
    "screen.wildercord.aura.writing.price": "%s aura · rests %s s",
    "screen.wildercord.aura.writing.shape.line": "A line %s blocks out, %s foes at most",
    "screen.wildercord.aura.writing.shape.cone": "%s blocks ahead, %s° wide, %s foes at most",
    "screen.wildercord.aura.writing.shape.ring": "All round, %s blocks out, %s foes at most",
    "screen.wildercord.aura.writing.blow": "Strikes %s of your blade (about %s)",
    "screen.wildercord.aura.writing.release.wave": "Flies on %s blocks more as a wave",
    "screen.wildercord.aura.writing.release.burst": "Breaks out all round you at once",
    "screen.wildercord.aura.writing.release.afterimage": "Its afterimage strikes again %s s later for %s",
    "screen.wildercord.aura.writing.intent.pierce": "Pierces: up to %s foes, %s blocks out",
    "screen.wildercord.aura.writing.intent.sunder": "Wears a stance %s times as hard as an art",
    "screen.wildercord.aura.writing.intent.bind": "Roots each foe it strikes for %s s",
    "screen.wildercord.aura.writing.intent.echo": "Strikes each foe again for %s a moment later",
    "screen.wildercord.aura.writing.intent.ward": "You take %s%% less from foes for %s s",
    "screen.wildercord.aura.writing.intent.rally": "You and allies within %s blocks take %s%% less for %s s; allied swordsmen build %s momentum",
    "screen.wildercord.aura.writing.intent.infuse": "Your element %s times as strong",
    "screen.wildercord.aura.writing.intent.own": "%s: its own effect",
    "screen.wildercord.aura.writing.element": "%s: %s",
    "screen.wildercord.aura.writing.element.ignite": "sets foes alight %s s",
    "screen.wildercord.aura.writing.element.chill": "chills foes %s s",
    "screen.wildercord.aura.writing.element.spark": "interrupts, and a spark leaps to a foe it missed for %s",
    "screen.wildercord.aura.writing.element.gale": "throws foes back, and reaches %s block further",
    "screen.wildercord.aura.writing.element.stone": "wears a stance harder, a creature staggered %s s",
    "screen.wildercord.aura.writing.element.mend": "mends you %s for each foe, up to %s",
    "screen.wildercord.aura.writing.element.pull": "draws foes in",
    "screen.wildercord.aura.writing.element.starlit": "gives back %s aura for each foe, up to %s",
    "screen.wildercord.aura.writing.element.haste": "a faint echo strikes the first foes for %s",
    "screen.wildercord.aura.writing.element.leech": "a wound that bleeds, drinking %s%% of it",
    "screen.wildercord.aura.writing.element.none": "strikes a little harder",
    "screen.wildercord.aura.writing.worth": "about a %s",
    "screen.wildercord.aura.writing.worth_label": "Worth",
    "screen.wildercord.aura.writing.worth_tip": "%s W in the balance the arts were weighed with: its price and rest follow from it, so it's another "
                                                 "answer to the arts, never a better or a worse one.",
    "screen.wildercord.aura.writing.string": "String",
    "screen.wildercord.aura.writing.back": "Take back the last swing (right click: all of them)",
    "screen.wildercord.aura.writing.effort.hard": "Demanding: %s%% cheaper",
    "screen.wildercord.aura.writing.effort.easy": "Easy: %s%% dearer",
    "screen.wildercord.aura.writing.effort.even": "As it's priced",
    "screen.wildercord.aura.writing.token_tip": "Click to add it to the string",
    "screen.wildercord.aura.writing.write": "Write",
    "screen.wildercord.aura.writing.written": "Written",
    "screen.wildercord.aura.writing.erase": "Erase",
    "screen.wildercord.aura.writing.erase_sure": "Erase?",
    "screen.wildercord.aura.writing.status.same": "Written. Change anything and write it again.",
    "screen.wildercord.aura.writing.status.no_string": "Give it a string: click the swings below.",
    "screen.wildercord.aura.writing.status.goes_first": "Ready, but %s goes first when its swings fit both.",
    "screen.wildercord.aura.writing.status.goes_before": "Ready. It goes before %s when its swings fit both.",
    "screen.wildercord.aura.writing.status.ready": "Ready to write.",
    "screen.wildercord.aura.writing.foot": "Compose a stroke, a release and an intent, name it and give it a string. It ranks up as it lands on real "
                                           "foes, as a spell grows with mastery.",
    "screen.wildercord.aura.writing.foot_before": "Techniques of your own are written from %s. The parts you find before then are kept for it.",
    "screen.wildercord.aura.writing.rank": "Rank: %s",
    "screen.wildercord.aura.writing.rank_tip": "Earned as it lands on real foes, more in danger and against stronger ones; less doing the same in "
                                               "the same place, and only a little on training dummies.",
    "screen.wildercord.aura.writing.inscribe": "Inscribe:",
    "screen.wildercord.aura.writing.inscribe_tip": "Click to set this part down on a technique scroll for another swordsman (a paper and an Aura "
                                                   "Shard).",
    "screen.wildercord.aura.writing.inscribe_no": "Every blade knows it, or only a Way gives it: no scroll can carry it.",
    "screen.wildercord.aura.writing.temper": "Temper",
    "screen.wildercord.aura.writing.edge": "Edge",
    "screen.wildercord.aura.writing.temper.none": "As written",
    "screen.wildercord.aura.writing.temper.none.tip": "As it was written.",
    "screen.wildercord.aura.writing.temper.swift": "Swift",
    "screen.wildercord.aura.writing.temper.swift.tip": "A fifth lighter, its holds and burns too; its price and rest follow.",
    "screen.wildercord.aura.writing.temper.heavy": "Heavy",
    "screen.wildercord.aura.writing.temper.heavy.tip": "A quarter harder, its holds and burns too; its price and rest follow.",
    "screen.wildercord.aura.writing.edge.none": "As written",
    "screen.wildercord.aura.writing.edge.none.tip": "As it was written.",
    "screen.wildercord.aura.writing.edge.long": "Long",
    "screen.wildercord.aura.writing.edge.long.tip": "Reaches a quarter further (a wave flies a fifth further), a little lighter.",
    "screen.wildercord.aura.writing.edge.broad": "Broad",
    "screen.wildercord.aura.writing.edge.broad.tip": "Wider, and two foes more, a little lighter.",
    "screen.wildercord.aura.writing.opens_at": "Opens at %s",
    "screen.wildercord.aura.writing.change_cost": "Changing a choice made costs %s experience levels",
    # ---- ranks
    "aura.wildercord.technique_rank.raw": "Raw",
    "aura.wildercord.technique_rank.honed": "Honed",
    "aura.wildercord.technique_rank.tempered": "Tempered",
    "aura.wildercord.technique_rank.keen": "Keen",
    "aura.wildercord.technique_rank.peerless": "Peerless",
    "aura.wildercord.banner.technique_ranked": "%s technique",
    "aura.wildercord.banner.technique_rank_up": "Now %s",
    # ---- messages
    "message.wildercord.aura.technique.off": "Techniques of your own don't work on this server",
    "message.wildercord.aura.technique.edge": "Techniques of your own are written from %s",
    "message.wildercord.aura.technique.no_slot": "There's no such slot",
    "message.wildercord.aura.technique.slot": "That slot opens at %s",
    "message.wildercord.aura.technique.not_part": "That isn't a %s",
    "message.wildercord.aura.technique.unknown": "You don't know the %s yet",
    "message.wildercord.aura.technique.string_too_short": "A technique's string is %s to %s swings",
    "message.wildercord.aura.technique.string_too_long": "A technique's string is %s to %s swings",
    "message.wildercord.aura.technique.string_too_plain": "Too plain: let it ask more of the hand, with another full, low, leaping or running swing, a counter or a step cut",
    "message.wildercord.aura.technique.string_no_mark": "Full swings alone would go off by themselves in any fight: give it a low, leaping or running swing, a counter or a step cut",
    "message.wildercord.aura.technique.clash": "That string meets %s: one of the two could never be played as written",
    "message.wildercord.aura.technique.fight": "Catch your breath first: nobody writes in the heat of a fight",
    "message.wildercord.aura.technique.written": "You write %s into %s.",
    "message.wildercord.aura.technique.rewritten": "You write %s again into %s.",
    "message.wildercord.aura.technique.erased": "You erase %s. Write it again and its rank comes back.",
    "message.wildercord.aura.technique.choose_rank": "That opens at %s",
    "message.wildercord.aura.technique.choose_levels": "Changing a choice made costs %s experience levels",
    "message.wildercord.aura.technique.inscribe_rank": "A technique's parts are set down on scrolls from %s",
    "message.wildercord.aura.technique.inscribe_part": "No scroll can carry the %s",
    "message.wildercord.aura.technique.inscribe_needs": "Setting a part down takes a paper and an Aura Shard",
    "message.wildercord.aura.technique.inscribed": "You set the %s of %s down on a scroll.",
    "message.wildercord.aura.technique.learned": "You learn the %s: one more %s for techniques of your own.",
    "message.wildercord.aura.technique.learned_later": "Techniques of your own are written from %s: it waits for you there.",
    "message.wildercord.aura.technique.duelist": "%s shows you the %s before it goes.",
    "message.wildercord.aura.technique.ranked": "%s is %s now.",
    "message.wildercord.aura.technique.opens_temper": "  You can temper it now (the writing page): swift or heavy.",
    "message.wildercord.aura.technique.opens_deep": "  Its intent has deepened.",
    "message.wildercord.aura.technique.opens_edge": "  You can choose its edge now (the writing page): long or broad.",
    "message.wildercord.aura.technique.opens_peerless": "  Its name rings out when you strike it, and its parts can be set down on scrolls.",
    "message.wildercord.aura.technique.scroll_method": "Learn a breathing method before the %s means anything to you",
    "message.wildercord.aura.technique.scroll_known": "You know the %s already. Someone else might not",
    # ---- the scroll
    "item.wildercord.technique_scroll": "Technique Scroll",
    "item.wildercord.technique_scroll.named": "Scroll of the %s",
    "item.wildercord.technique_scroll.lore": "An old swordsman's notes on one part of a technique, in a hand that never stopped moving.",
    "tooltip.wildercord.technique_scroll.kind": "Part of a technique: a %s",
    "tooltip.wildercord.technique_scroll.use": "Read it to learn the part for techniques of your own",
    "tooltip.wildercord.technique_scroll.unknown": "The ink has run: nothing can be read",
    # ---- the Grimoire
    "toast.wildercord.aura.technique": "A Technique of Your Own",
    "toast.wildercord.aura.technique_part": "A Technique's Part",
    "toast.wildercord.aura.technique_peerless": "A Peerless Technique",
    # ---- operators
    "command.wildercord.aura.technique.unknown": "No part called %s",
    "command.wildercord.aura.technique.empty": "Nothing is written in that slot",
    "command.wildercord.aura.technique.parts": "Parts: %s",
    "command.wildercord.aura.technique.slot": "%s. %s: %s, %s experience",
}
LANG.update(_parts())


def preview():
    sheet = Image.new("RGBA", (64 * 9, 64 * 3), (40, 34, 52, 255))
    for i, part in enumerate(PARTS + ["unknown"]):
        icon = glyph(part)
        family = "stroke" if part in STROKES else "release" if part in RELEASES else "intent"
        color = FAMILY[family]
        tinted = Image.new("RGBA", icon.size)
        r0, g0, b0 = _rgb(color)
        for y in range(16):
            for x in range(16):
                r, g, b, a = icon.getpixel((x, y))
                tinted.putpixel((x, y), (r * r0 // 255, g * g0 // 255, b * b0 // 255, a))
        sheet.alpha_composite(tinted.resize((64, 64), Image.NEAREST), ((i % 9) * 64, (i // 9) * 64))
    sheet.alpha_composite(seal(False).resize((52, 52), Image.NEAREST), (0, 134))
    sheet.alpha_composite(seal(True).resize((52, 52), Image.NEAREST), (64, 134))
    for k, family in enumerate(("stroke", "release", "intent")):
        sheet.alpha_composite(scroll_icon(FAMILY[family]).resize((64, 64), Image.NEAREST), (128 + k * 64, 128))
    out = ROOT / "build/art-preview"
    out.mkdir(parents=True, exist_ok=True)
    sheet.save(out / "technique_art.png")
    print(f"wrote {out / 'technique_art.png'}")


if __name__ == "__main__":
    preview()
