"""Forged and boss weapons (Oathkeeper, Lumenedge, Bulwark Maul, Skyrend Glaive, Mojito's Fetchblade) and held elemental
staffs: one authored Minecraft-voxel 3D build per weapon, derived from Tripo 3D voxel reference meshes.

Each weapon is a list of stepped voxel boxes standing upright (handle at the bottom, centred on x = z = 8). The held model
tilts that build onto the same diagonal a vanilla sprite uses, so the vanilla hand poses hold it, and the inventory sprite
is drawn from the same boxes. The icon and the blade in hand therefore always agree.
"""
import math
from PIL import Image

# ---------------------------------------------------------------- materials

def _hex(c):
    c = c.lstrip("#")
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))


def _mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def _noise(x, y, seed):
    n = (x * 374761393 + y * 668265263 + seed * 2246822519) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 0xFFFF


def _material(kind, dark, mid, light, seed=1):
    """A 32x32 surface. The pattern is anchored to model space (16 units = 32 texels) so it runs on across boxes."""
    d, m, l = _hex(dark), _hex(mid), _hex(light)
    im = Image.new("RGBA", (32, 32))
    px = im.load()
    for y in range(32):
        for x in range(32):
            n = _noise(x, y, seed)
            if kind == "brushed":  # long-grain polished metal, brighter towards the top
                t = .35 + .4 * (1 - y / 31) + (_noise(x, 0, seed) - .5) * .25 + (n - .5) * .08
                c = _mix(m, l, max(0, min(1, t))) if t > .5 else _mix(d, m, max(0, t * 2))
            elif kind == "fuller":  # blade flat with a dark central groove and a lit ridge beside it
                t = .45 + .35 * (1 - y / 31) + (n - .5) * .08
                c = _mix(d, l, max(0, min(1, t)))
                if x in (15, 16): c = _mix(d, m, .35)
                elif x == 17: c = _mix(m, l, .85)
                elif x == 14: c = _mix(d, m, .7)
            elif kind == "rune":  # blade flat with a glowing stepped voxel rune fuller down the centre
                t = .4 + .35 * (1 - y / 31) + (n - .5) * .08
                c = _mix(d, m, max(0, min(1, t * 1.35)))
                if x in (15, 16):
                    c = l if (y % 4 != 0 or x == 15 + (y // 4) % 2) else _mix(m, l, .55)
                elif x in (14, 17):
                    c = _mix(m, l, .75) if (y + x) % 4 == 1 else _mix(d, m, .55)
            elif kind == "edge":  # honed edge: bright, with a faint pixel bevel line
                c = _mix(m, l, .55 + .45 * (1 - y / 31)) if x % 4 else _mix(d, m, .8)
            elif kind == "gilt":  # cast metal with worn high points
                t = .5 + (n - .5) * .5
                c = _mix(d, m, t * 2) if t < .5 else _mix(m, l, (t - .5) * 2)
                if n > .93: c = l
            elif kind == "wrap":  # leather or silk bound diagonally
                band = (x + y) % 6
                c = l if band == 0 else d if band in (4, 5) else _mix(d, m, .6 + (n - .5) * .3)
            elif kind == "bone":  # dark leather grip cross-strapped with ivory bone bands
                band = (x + y) % 6
                cross = (x - y) % 6
                c = l if band in (0, 1) or cross == 0 else d if band == 4 else _mix(d, m, .55 + (n - .5) * .25)
            elif kind == "grain":  # long wood grain
                t = .5 + .35 * math.sin(x * 1.3 + _noise(x // 3, y // 8, seed) * 2.5) + (n - .5) * .15
                c = _mix(d, m, max(0, t)) if t < 1 else _mix(m, l, min(1, t - 1))
            elif kind == "stone":  # riven stone with dark seams
                t = .55 + (n - .5) * .35 + (_noise(x // 4, y // 4, seed) - .5) * .3
                c = _mix(d, m, max(0, t * 1.6)) if t < .62 else _mix(m, l, min(1, (t - .62) * 2))
            elif kind == "iron":  # dark forged iron with rivet heads every eight texels
                t = .4 + (n - .5) * .2
                c = _mix(d, m, t * 2)
                if x % 8 == 3 and y % 8 == 3: c = l
                elif x % 8 == 4 and y % 8 == 4: c = d
            elif kind == "crystal":  # facetted glowing crystal
                t = abs(((x * 3 + y * 5) % 16) - 8) / 8
                c = _mix(m, l, t) if (x + 2 * y) % 7 else _mix(d, m, .5)
            elif kind == "storm":  # storm-steel with a lightning vein down the middle
                t = .45 + .35 * (1 - y / 31) + (n - .5) * .08
                c = _mix(d, m, min(1, t * 1.4))
                vein = 17 + (2 if (y // 3) % 2 else 0) - ((y % 3) if (y // 3) % 2 else 0)
                if x == vein: c = l
                elif abs(x - vein) == 1: c = _mix(m, l, .45)
            px[x, y] = c + (255,)
    for y in range(32):  # seams in stone
        for x in range(32):
            if kind == "stone" and (abs(x - 11 - (y // 5) % 3) == 0 and y < 20 or abs(y - 21 - (x // 6) % 2) == 0 and x > 8):
                px[x, y] = _mix(d, (0, 0, 0), .35) + (255,)
    return im


OATH = {
    "steel": ("brushed", "#232d32", "#566c75", "#b8cdd4"),
    "fuller": ("rune", "#1b2428", "#2d7a7d", "#7ef9f5"),
    "edge": ("edge", "#6c848e", "#bfd3da", "#f4fbfd"),
    "brass": ("gilt", "#3a1d07", "#8c5823", "#e5b567"),
    "wrap": ("wrap", "#1b0c04", "#3d200a", "#6b3d18"),
    "gem": ("crystal", "#124e52", "#35b8b4", "#aefcf9"),
    "dark": ("iron", "#1b2124", "#364449", "#6c8187"),
    "stone": ("stone", "#4a524c", "#747b70", "#a3a896"),
}
LUMEN = {
    "blade": ("brushed", "#966806", "#e4ac18", "#fff394"),
    "fuller": ("rune", "#743f03", "#f3bd1a", "#ffffd6"),
    "edge": ("edge", "#d7a51d", "#fbe76c", "#ffffff"),
    "gold": ("gilt", "#573101", "#b87908", "#f3bd1a"),
    "wrap": ("wrap", "#3d1800", "#7c3b04", "#c9740c"),
    "sun": ("crystal", "#b86e00", "#ffd83d", "#ffffff"),
}
MAUL = {
    "stone": ("stone", "#2b231d", "#5c5049", "#9c8e86"),
    "iron": ("iron", "#1a1614", "#3a3330", "#786c66"),
    "geode": ("crystal", "#4b1f6e", "#b85ce6", "#ffe8a3"),
    "wood": ("grain", "#3d220c", "#6e411c", "#a4673d"),
    "leather": ("wrap", "#221308", "#4c2c16", "#82512e"),
    "brass": ("gilt", "#4c2e16", "#966236", "#dda66a"),
}
GLAIVE = {
    "blade": ("storm", "#1b2b3d", "#4990c2", "#b8f2ff"),
    "steel": ("brushed", "#223244", "#587ea1", "#c2e2f8"),
    "edge": ("edge", "#68c6f2", "#ccf4ff", "#ffffff"),
    "haft": ("grain", "#1e1b18", "#38332e", "#595149"),
    "wrap": ("wrap", "#16253b", "#2d4c75", "#6aa2d6"),
    "gold": ("gilt", "#4c3803", "#a88224", "#ebdac0"),
    "spark": ("crystal", "#246da6", "#68c6f2", "#ffffff"),
}
FETCH = {
    "ruby": ("brushed", "#420408", "#860b13", "#d92b32"),
    "fuller": ("rune", "#2b0204", "#a8131d", "#ff7878"),
    "edge": ("edge", "#9e141b", "#e84849", "#ffb8b8"),
    "gold": ("gilt", "#553801", "#a67c0e", "#f0cb38"),
    "bone": ("bone", "#2b2008", "#8c7b56", "#e6d8b5"),
    "mint": ("crystal", "#145238", "#38bd7e", "#b8ffd8"),
}
GLOW = {"gem", "geode", "spark", "sun", "mint", "fuller", "crystal"}

# ---------------------------------------------------------------- builds (upright, handle at y≈0)

def _b(lo, hi, mat, front=None):
    return (tuple(lo), tuple(hi), mat, front or mat)


def oathkeeper():
    """A Marchkeeper longsword: dark-iron stepped blade with glowing cyan rune fuller, stepped bronze quillons and oath-stone."""
    parts = [
        _b((6.9, 0, 6.9), (9.1, 1.6, 9.1), "brass"), _b((7.45, .35, 6.75), (8.55, 1.25, 9.25), "gem"),
        _b((7.3, 1.6, 7.35), (8.7, 5.4, 8.65), "wrap"),
        _b((7.1, 1.6, 7.2), (8.9, 2.0, 8.8), "brass"), _b((7.18, 3.3, 7.25), (8.82, 3.7, 8.75), "brass"),
        _b((7.1, 5.0, 7.2), (8.9, 5.4, 8.8), "brass"),
        _b((6.5, 5.4, 6.95), (9.5, 6.6, 9.05), "brass"), _b((7.5, 5.55, 6.75), (8.5, 6.45, 9.25), "gem"),
        _b((4.2, 5.65, 7.25), (11.8, 6.45, 8.75), "brass"),
        _b((3.4, 4.9, 7.2), (4.5, 5.85, 8.8), "brass"), _b((11.5, 4.9, 7.2), (12.6, 5.85, 8.8), "brass"),
        _b((7.3, 6.6, 7.1), (8.7, 7.7, 8.9), "brass"),
        _b((7.05, 6.6, 7.68), (8.95, 16.8, 8.32), "steel", "fuller"),
        _b((6.55, 6.6, 7.84), (7.05, 16.8, 8.16), "edge"), _b((8.95, 6.6, 7.84), (9.45, 16.8, 8.16), "edge"),
        _b((7.25, 16.8, 7.72), (8.75, 18.8, 8.28), "steel", "fuller"),
        _b((6.8, 16.8, 7.86), (7.25, 18.8, 8.14), "edge"), _b((8.75, 16.8, 7.86), (9.2, 18.8, 8.14), "edge"),
        _b((7.45, 18.8, 7.75), (8.55, 20.1, 8.25), "steel"),
        _b((7.1, 18.8, 7.87), (7.45, 20.1, 8.13), "edge"), _b((8.55, 18.8, 7.87), (8.9, 20.1, 8.13), "edge"),
        _b((7.6, 20.1, 7.8), (8.4, 21.0, 8.2), "edge"),
    ]
    return parts, 21


def lumenedge():
    """A sun-forged greatsword: stepped sun-orb pommel, sweeping golden sun-wing crossguard, and blazing solar fuller."""
    parts = [
        _b((6.8, 0, 6.9), (9.2, 1.5, 9.1), "gold"), _b((7.4, .35, 6.75), (8.6, 1.15, 9.25), "sun"),
        _b((7.25, 1.5, 7.3), (8.75, 5.4, 8.7), "wrap"),
        _b((7.05, 1.5, 7.15), (8.95, 1.9, 8.85), "gold"), _b((7.15, 3.3, 7.2), (8.85, 3.65, 8.8), "gold"),
        _b((7.05, 5.0, 7.15), (8.95, 5.4, 8.85), "gold"),
        _b((6.4, 5.4, 6.9), (9.6, 6.8, 9.1), "gold"), _b((7.35, 5.55, 6.7), (8.65, 6.65, 9.3), "sun"),
        _b((4.5, 5.65, 7.2), (11.5, 6.55, 8.8), "gold"),
        _b((3.4, 6.0, 7.25), (4.7, 7.0, 8.75), "gold"), _b((11.3, 6.0, 7.25), (12.6, 7.0, 8.75), "gold"),
        _b((2.9, 6.4, 7.35), (3.6, 7.4, 8.65), "sun"), _b((12.4, 6.4, 7.35), (13.1, 7.4, 8.65), "sun"),
        _b((7.2, 6.8, 7.15), (8.8, 7.9, 8.85), "gold"),
        _b((7.0, 6.8, 7.68), (9.0, 17.0, 8.32), "blade", "fuller"),
        _b((6.5, 6.8, 7.84), (7.0, 17.0, 8.16), "edge"), _b((9.0, 6.8, 7.84), (9.5, 17.0, 8.16), "edge"),
        _b((7.25, 17.0, 7.72), (8.75, 19.2, 8.28), "blade", "fuller"),
        _b((6.8, 17.0, 7.86), (7.25, 19.2, 8.14), "edge"), _b((8.75, 17.0, 7.86), (9.2, 19.2, 8.14), "edge"),
        _b((7.45, 19.2, 7.76), (8.55, 20.6, 8.24), "blade"),
        _b((7.1, 19.2, 7.87), (7.45, 20.6, 8.13), "edge"), _b((8.55, 19.2, 7.87), (8.9, 20.6, 8.13), "edge"),
        _b((7.55, 20.6, 7.82), (8.45, 21.6, 8.18), "edge"),
    ]
    return parts, 21.6


def bulwark_maul():
    """A riven-basalt double-headed war maul bound in bronze and iron, a glowing amethyst-amber geode broken open in its faces."""
    parts = [
        _b((7.4, -.6, 7.4), (8.6, 0, 8.6), "iron"), _b((7.0, 0, 7.0), (9.0, 1.2, 9.0), "brass"),
        _b((7.25, 1.2, 7.25), (8.75, 5.6, 8.75), "leather"),
        _b((7.35, 5.6, 7.35), (8.65, 14.0, 8.65), "wood"),
        _b((7.15, 5.6, 7.15), (8.85, 6.1, 8.85), "brass"), _b((7.2, 9.5, 7.2), (8.8, 10.0, 8.8), "brass"),
        _b((7.7, 10.5, 7.25), (8.3, 13.2, 8.75), "iron"),
        _b((6.9, 13.0, 6.9), (9.1, 14.5, 9.1), "brass"),
        _b((4.3, 14.4, 5.5), (11.7, 19.7, 10.5), "stone"),
        _b((6.1, 14.2, 5.35), (6.9, 19.9, 10.65), "brass"), _b((9.1, 14.2, 5.35), (9.9, 19.9, 10.65), "brass"),
        _b((3.5, 14.1, 5.25), (4.3, 20.0, 10.75), "iron"), _b((11.7, 14.1, 5.25), (12.5, 20.0, 10.75), "iron"),
        _b((3.0, 15.4, 6.7), (3.5, 18.6, 9.3), "geode"), _b((12.5, 15.4, 6.7), (13.0, 18.6, 9.3), "geode"),
        _b((2.7, 16.3, 7.4), (3.0, 17.7, 8.6), "geode"), _b((13.0, 16.3, 7.4), (13.3, 17.7, 8.6), "geode"),
        _b((7.1, 15.6, 5.2), (8.9, 18.4, 5.5), "geode"), _b((7.1, 15.6, 10.5), (8.9, 18.4, 10.8), "geode"),
        _b((7.2, 19.7, 7.1), (8.4, 21.0, 8.3), "geode"), _b((8.0, 19.7, 7.9), (8.9, 20.6, 8.8), "geode"),
    ]
    return parts, 21.6


def skyrend_glaive():
    """A long-shafted storm-lacquered polearm with twin leather grip wraps, a storm-gold socket collar, and a sleek curved single-edged storm-crystal glaive blade."""
    parts = [
        _b((7.55, -.6, 7.55), (8.45, .2, 8.45), "steel"), _b((7.15, .2, 7.15), (8.85, 1.3, 8.85), "gold"),
        _b((7.48, 1.3, 7.48), (8.52, 15.8, 8.52), "haft"),
        # Rear-hand and lead-hand polearm grip wraps along the shaft.
        _b((7.28, 3.6, 7.28), (8.72, 4.0, 8.72), "gold"), _b((7.34, 4.0, 7.34), (8.66, 7.4, 8.66), "wrap"),
        _b((7.28, 7.4, 7.28), (8.72, 7.8, 8.72), "gold"),
        _b((7.28, 9.8, 7.28), (8.72, 10.2, 8.72), "gold"), _b((7.34, 10.2, 7.34), (8.66, 13.6, 8.66), "wrap"),
        _b((7.28, 13.6, 7.28), (8.72, 14.0, 8.72), "gold"),
        # Storm-gold polearm socket collar, wind-gem, and compact asymmetric guard spur + tassel.
        _b((6.95, 15.6, 7.05), (9.05, 17.2, 8.95), "gold"), _b((7.45, 15.95, 6.85), (8.55, 16.85, 9.15), "spark"),
        _b((5.3, 16.2, 7.45), (6.95, 17.3, 8.55), "gold"), _b((4.5, 15.4, 7.65), (5.4, 16.7, 8.35), "spark"),
        _b((9.05, 16.0, 7.45), (10.15, 17.0, 8.55), "gold"),
        _b((7.25, 17.2, 7.3), (8.75, 17.8, 8.7), "gold"),
        # Rear spine barb midway up the curved blade.
        _b((6.45, 20.9, 7.76), (7.10, 21.9, 8.24), "steel"),
    ]
    # Sleek, curved single-edged glaive blade: reinforced storm-steel spine (-x) and glowing cutting edge (+x).
    blade = [(17.6, 19.1, 7.35, 9.25), (19.1, 20.6, 7.25, 9.50), (20.6, 22.1, 7.10, 9.65),
             (22.1, 23.5, 6.95, 9.45), (23.5, 24.8, 6.80, 9.05), (24.8, 25.9, 6.65, 8.45)]
    for lo, hi, x0, x1 in blade:
        parts.append(_b((x0, lo, 7.72), (x1, hi, 8.28), "steel", "blade"))
        parts.append(_b((x1, lo, 7.84), (x1 + .52, hi, 8.16), "edge"))
    parts.append(_b((6.55, 25.9, 7.84), (8.20, 26.5, 8.16), "edge"))
    parts.append(_b((6.50, 26.5, 7.88), (7.50, 26.8, 8.12), "edge"))
    return parts, 26.8


def fetchblade():
    """Mojito's Fetchblade: bone-wrapped grip, ornate royal-gold crossguard with a mint-emerald charm, and crimson ruby blade."""
    parts = [
        _b((6.85, 0, 6.85), (9.15, 1.6, 9.15), "gold"), _b((7.45, .35, 6.75), (8.55, 1.25, 9.25), "mint"),
        _b((7.25, 1.6, 7.3), (8.75, 5.4, 8.7), "bone"),
        _b((7.05, 1.6, 7.15), (8.95, 2.0, 8.85), "gold"), _b((7.15, 3.3, 7.2), (8.85, 3.7, 8.8), "gold"),
        _b((7.05, 5.0, 7.15), (8.95, 5.4, 8.85), "gold"),
        _b((6.4, 5.4, 6.9), (9.6, 6.7, 9.1), "gold"), _b((7.4, 5.55, 6.7), (8.6, 6.55, 9.3), "mint"),
        _b((4.4, 5.6, 7.2), (11.6, 6.5, 8.8), "gold"),
        _b((3.4, 5.9, 7.25), (4.6, 7.0, 8.75), "gold"), _b((11.4, 5.9, 7.25), (12.6, 7.0, 8.75), "gold"),
        _b((7.2, 6.7, 7.1), (8.8, 7.8, 8.9), "gold"),
        _b((7.0, 6.7, 7.66), (9.0, 17.2, 8.34), "ruby", "fuller"),
        _b((6.5, 6.7, 7.84), (7.0, 17.2, 8.16), "edge"), _b((9.0, 6.7, 7.84), (9.5, 17.2, 8.16), "edge"),
        _b((7.25, 17.2, 7.72), (8.75, 19.4, 8.28), "ruby", "fuller"),
        _b((6.8, 17.2, 7.86), (7.25, 19.4, 8.14), "edge"), _b((8.75, 17.2, 7.86), (9.2, 19.4, 8.14), "edge"),
        _b((7.45, 19.4, 7.76), (8.55, 20.8, 8.24), "ruby"),
        _b((7.1, 19.4, 7.87), (7.45, 20.8, 8.13), "edge"), _b((8.55, 19.4, 7.87), (8.9, 20.8, 8.13), "edge"),
        _b((7.55, 20.8, 7.82), (8.45, 21.8, 8.18), "edge"),
    ]
    return parts, 21.8


def staff_build(greater=False):
    """A stepped voxel elemental staff with a curved crook head cradling a floating faceted elemental crystal."""
    parts = [
        _b((7.2, 0, 7.2), (8.8, 1.0, 8.8), "metal"),
        _b((7.4, 1.0, 7.4), (8.6, 13.8, 8.6), "shaft"),
        _b((7.2, 2.0, 7.2), (8.8, 2.4, 8.8), "metal"),
        _b((7.25, 2.4, 7.25), (8.75, 6.0, 8.75), "wrap"),
        _b((7.2, 6.0, 7.2), (8.8, 6.4, 8.8), "metal"),
        _b((7.25, 10.0, 7.25), (8.75, 10.5, 8.75), "metal"),
        _b((6.9, 13.4, 6.9), (9.1, 14.6, 9.1), "metal"),
        # Curved voxel crook sweeping left, arching over the top, and curling down above the crystal.
        _b((5.8, 14.4, 7.35), (7.4, 16.2, 8.65), "shaft"),
        _b((5.2, 15.8, 7.35), (6.6, 18.8, 8.65), "shaft"),
        _b((5.6, 18.6, 7.35), (7.4, 20.0, 8.65), "shaft"),
        _b((6.8, 19.4, 7.35), (9.6, 20.7, 8.65), "shaft"),
        _b((9.0, 18.2, 7.4), (10.4, 19.8, 8.6), "metal"),
        # Metal cradle prongs & floating faceted elemental crystal gem inside the crook.
        _b((8.4, 14.4, 7.45), (9.5, 15.6, 8.55), "metal"),
        _b((7.2, 15.5, 7.2), (9.2, 18.1, 8.8), "crystal"),
        _b((7.6, 15.0, 7.5), (8.8, 18.6, 8.5), "crystal"),
    ]
    if greater:
        parts.extend([
            _b((4.6, 16.4, 7.5), (5.2, 18.4, 8.5), "metal"),
            _b((7.2, 20.7, 7.5), (9.2, 21.3, 8.5), "metal"),
            _b((6.9, 16.1, 6.9), (9.5, 17.5, 9.1), "crystal"),
        ])
    return parts, 21.4


WEAPONS = {
    # name: (build, materials, texture prefix, diagonal: -45 = tip up-right)
    "oathkeeper": (oathkeeper, OATH, "oathkeeper", -45),
    "lumenedge": (lumenedge, LUMEN, "lumenedge", -45),
    "bulwark_maul": (bulwark_maul, MAUL, "bulwark_maul", -45),
    "skyrend_glaive": (skyrend_glaive, GLAIVE, "skyrend_glaive", -45),
    "fetchblade": (fetchblade, FETCH, "fetchblade", -45),
}

# ---------------------------------------------------------------- model JSON

def _span(a, b):
    """UV range for a face edge in model space, kept inside the 0..16 texture."""
    w = b - a
    if w >= 16: return 0, 16
    u = a % 16
    if u + w > 16: u = 16 - w
    return round(u, 3), round(u + w, 3)


def _faces(lo, hi, mat, front):
    x0, y0, z0 = lo; x1, y1, z1 = hi
    u_x, u_z = _span(x0, x1), _span(z0, z1)
    v_y = _span(16 - y1, 16 - y0) if 0 <= y0 and y1 <= 16 else _span(-y1, -y0)
    v_z = _span(z0, z1)
    face = lambda tex, u, v: {"texture": "#" + tex, "uv": [u[0], v[0], u[1], v[1]]}
    return {"north": face(front, u_x, v_y), "south": face(front, u_x, v_y), "east": face(mat, u_z, v_y),
            "west": face(mat, u_z, v_y), "up": face(mat, u_x, v_z), "down": face(mat, u_x, v_z)}


def elements(parts, length, angle=None, lift=0.0, flip=False, base=0.0):
    """Upright build to model elements. angle tilts the whole weapon about the model centre; flip stands it tip-down."""
    out = []
    shift = 8 - length / 2 if angle is not None else 0
    for lo, hi, mat, front in parts:
        y0, y1 = lo[1], hi[1]
        if flip: y0, y1 = length - hi[1], length - lo[1]
        y0, y1 = y0 + shift + lift + base, y1 + shift + lift + base
        a, b = (lo[0], y0, lo[2]), (hi[0], y1, hi[2])
        el = {"from": [round(v, 3) for v in a], "to": [round(v, 3) for v in b], "faces": _faces(a, b, mat, front)}
        if angle is not None:
            el["rotation"] = {"origin": [8, 8, 8], "axis": "z", "angle": angle}
        if mat in GLOW or front in GLOW:
            el["light_emission"] = 10
        out.append(el)
    return out


HANDHELD = {
    "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4, .5], "scale": [.85, .85, .85]},
    "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4, .5], "scale": [.85, .85, .85]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [.68, .68, .68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [.68, .68, .68]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
}
# Polearm display transforms: gripped along the lower-mid shaft so the long haft, collar, and curved glaive blade extend forward.
GLAIVE_DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4, .5], "scale": [.88, .88, .88]},
    "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4, .5], "scale": [.88, .88, .88]},
    "firstperson_righthand": {"rotation": [0, -100, 16], "translation": [.55, 1.75, 1.35], "scale": [.64, .64, .64]},
    "firstperson_lefthand": {"rotation": [0, 80, -16], "translation": [.55, 1.75, 1.35], "scale": [.64, .64, .64]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
}

# ---------------------------------------------------------------- inventory sprite (pure-Python raster of the build)

def _rot(v, axis, deg):
    a = math.radians(deg); c, s = math.cos(a), math.sin(a)
    x, y, z = v
    if axis == "x": return (x, y * c - z * s, y * s + z * c)
    if axis == "y": return (x * c + z * s, y, -x * s + z * c)
    return (x * c - y * s, x * s + y * c, z)


def _shade(rgb, k):
    return tuple(max(0, min(255, round(v * k))) for v in rgb)


def sprite(parts, length, angle, mats, size=32):
    """Draw the build onto the vanilla item diagonal, lit from the upper left, with a dark keyline round it."""
    tex = {k: _material(*v, seed=i + 3) for i, (k, v) in enumerate(mats.items())}
    S = 4  # supersample for coverage; each pixel then keeps one real texel
    big = size * S
    col = [[None] * big for _ in range(big)]
    zbuf = [[-1e9] * big for _ in range(big)]
    shift = 8 - length / 2
    view = lambda p: _rot(_rot(_rot((p[0] - 8, p[1] - 8, p[2] - 8), "z", angle), "y", -18), "x", 12)
    # Fit the whole weapon into the sprite, one pixel clear of each edge for the keyline.
    corners = [view((x, y + shift, z)) for lo, hi, _, _ in parts for x in (lo[0], hi[0]) for y in (lo[1], hi[1]) for z in (lo[2], hi[2])]
    xs_all, ys_all = [c[0] for c in corners], [c[1] for c in corners]
    cx, cy = (min(xs_all) + max(xs_all)) / 2, (min(ys_all) + max(ys_all)) / 2
    k = (size - 2) / max(max(xs_all) - min(xs_all), max(ys_all) - min(ys_all))
    for lo, hi, mat, front in parts:
        x0, y0, z0 = lo; x1, y1, z1 = hi
        y0 += shift; y1 += shift
        quads = [((x0, y1, z1), (x1, y1, z1), (x0, y0, z1), (0, 0, 1), front, (x0, x1, y0, y1)),
                 ((x0, y1, z0), (x0, y1, z1), (x0, y0, z0), (-1, 0, 0), mat, (z0, z1, y0, y1)),
                 ((x1, y1, z1), (x1, y1, z0), (x1, y0, z1), (1, 0, 0), mat, (z0, z1, y0, y1)),
                 ((x0, y1, z0), (x1, y1, z0), (x0, y1, z1), (0, 1, 0), mat, (x0, x1, z0, z1))]
        for tl, tr, bl, n, m, (ua, ub, va, vb) in quads:
            # Tilt the weapon onto the diagonal, then turn it a little towards the viewer to show its depth.
            P = [view(p) for p in (tl, tr, bl)]
            N = _rot(_rot(_rot(n, "z", angle), "y", -18), "x", 12)
            if N[2] <= 1e-6: continue
            light = .62 + .38 * max(0, -.45 * N[0] + .6 * N[1] + .65 * N[2])
            to = lambda p: (((p[0] - cx) * k + size / 2) * S, (size / 2 - (p[1] - cy) * k) * S)
            p0, p1, p2 = to(P[0]), to(P[1]), to(P[2])
            ax, ay = p1[0] - p0[0], p1[1] - p0[1]
            bx, by = p2[0] - p0[0], p2[1] - p0[1]
            det = ax * by - ay * bx
            if abs(det) < 1e-9: continue
            xs = [p0[0], p1[0], p2[0], p1[0] + bx]; ys = [p0[1], p1[1], p2[1], p1[1] + by]
            T = tex[m].load()
            for py in range(max(0, int(min(ys))), min(big, int(max(ys)) + 2)):
                for px in range(max(0, int(min(xs))), min(big, int(max(xs)) + 2)):
                    dx, dy = px + .5 - p0[0], py + .5 - p0[1]
                    s = (dx * by - dy * bx) / det
                    t = (ax * dy - ay * dx) / det
                    if not (0 <= s <= 1 and 0 <= t <= 1): continue
                    z = P[0][2] + s * (P[1][2] - P[0][2]) + t * (P[2][2] - P[0][2])
                    if z <= zbuf[py][px]: continue
                    zbuf[py][px] = z
                    u = ua + s * (ub - ua)
                    v = vb - t * (vb - va)
                    c = T[int(u * 2) % 32, int((16 - v) * 2) % 32]
                    col[py][px] = _shade(c[:3], light * (1.12 if m in GLOW else 1))
    im = Image.new("RGBA", (size, size))
    out = im.load()
    for y in range(size):
        for x in range(size):
            counts = {}
            for sy in range(S):
                for sx in range(S):
                    c = col[y * S + sy][x * S + sx]
                    if c is not None: counts[c] = counts.get(c, 0) + 1
            if sum(counts.values()) >= S * S * .4:
                # Keep a real texel rather than a blend, so the sprite stays crisp pixel art.
                for sy, sx in sorted(((sy, sx) for sy in range(S) for sx in range(S)), key=lambda p: (p[0] - S / 2 + .5) ** 2 + (p[1] - S / 2 + .5) ** 2):
                    c = col[y * S + sy][x * S + sx]
                    if c is not None:
                        out[x, y] = c + (255,)
                        break
    keyline = Image.new("RGBA", (size, size))
    k = keyline.load()
    for y in range(size):
        for x in range(size):
            if out[x, y][3]: continue
            near = [out[x + dx, y + dy] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
                    if 0 <= x + dx < size and 0 <= y + dy < size and out[x + dx, y + dy][3]]
            if near:
                avg = tuple(sum(c[i] for c in near) // len(near) for i in range(3))
                k[x, y] = _mix(avg, (12, 12, 20), .78) + (255,)
    im.alpha_composite(keyline)
    return im


# ---------------------------------------------------------------- writing

def textures(name):
    build, mats, prefix, angle = WEAPONS[name]
    return {k: f"wildercord:block/{prefix}_{k}" for k in mats}


def held_model(name):
    build, mats, prefix, angle = WEAPONS[name]
    parts, length = build()
    tex = textures(name)
    tex["particle"] = tex[next(iter(mats))]
    display = dict(GLAIVE_DISPLAY if name == "skyrend_glaive" else HANDHELD)
    display["ground"] = {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [.5, .5, .5]}
    display["fixed"] = {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]}
    return {"textures": tex, "elements": elements(parts, length, angle), "display": display}


def write(g):
    for name, (build, mats, prefix, angle) in WEAPONS.items():
        for i, (k, spec) in enumerate(mats.items()):
            g.save(_material(*spec, seed=i + 3), g.ASSETS / f"textures/block/{prefix}_{k}.png")
        parts, length = build()
        g.save(sprite(parts, length, angle, mats), g.ASSETS / f"textures/item/{name}.png")
        g.write_json(g.ASSETS / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"wildercord:item/{name}"}})
        g.write_json(g.ASSETS / f"models/item/{name}_in_hand.json", held_model(name))
        # The drawn sprite in menus, frames and on shelves; the built weapon in a hand or lying on the ground.
        g.write_json(g.ASSETS / f"items/{name}.json", {"model": {
            "type": "minecraft:select", "property": "minecraft:display_context",
            "cases": [{"when": ["gui", "fixed", "on_shelf"], "model": {"type": "minecraft:model", "model": f"wildercord:item/{name}"}}],
            "fallback": {"type": "minecraft:model", "model": f"wildercord:item/{name}_in_hand"}},
            **({"swap_animation_scale": 1.95} if angle > 0 else {})})


STAFF_BASE_MATS = {
    "wood": ("grain", "#4a2e16", "#7c522c", "#b47c48"),
    "blaze": ("grain", "#96460c", "#e08e24", "#ffe078"),
    "ebony": ("grain", "#24192e", "#463458", "#6e5684"),
    "wrap": ("wrap", "#2b140c", "#54281c", "#824432"),
    "wrap_gold": ("wrap", "#5e3e0c", "#a8781c", "#f4d47c"),
    "silver": ("gilt", "#4e5060", "#989cb0", "#e2e4f0"),
    "gold": ("gilt", "#5e3e0c", "#c69228", "#fff0a8"),
}
STAFF_GEMS = {
    "fire": ("crystal", "#7a1c08", "#e85c18", "#ffe478"),
    "frost": ("crystal", "#184a78", "#58b4e8", "#e8fcff"),
    "storm": ("crystal", "#4a2478", "#9e68f0", "#fff4a8"),
    "wind": ("crystal", "#1e5c54", "#68ccb8", "#e8fff8"),
    "earth": ("crystal", "#3e2c18", "#88683e", "#c8b078"),
    "life": ("crystal", "#185828", "#48c458", "#c8ffd0"),
    "void": ("crystal", "#1c0c34", "#582c8c", "#c498ff"),
    "arcane": ("crystal", "#4c1c68", "#b458dc", "#ffd8ff"),
    "time": ("crystal", "#5c4210", "#cca038", "#fff4b8"),
    "blood": ("crystal", "#480810", "#a81828", "#ff7880"),
}


def write_staffs(g, element_names):
    """Write 3D held models for the ten elemental staffs and their ten greater versions."""
    for i, (k, spec) in enumerate(STAFF_BASE_MATS.items()):
        g.save(_material(*spec, seed=i + 31), g.ASSETS / f"textures/block/staff_{k}.png")
    for i, el in enumerate(element_names):
        g.save(_material(*STAFF_GEMS[el], seed=i + 51), g.ASSETS / f"textures/block/staff_gem_{el}.png")
    display = dict(HANDHELD)
    display["ground"] = {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [.5, .5, .5]}
    display["fixed"] = {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]}
    for el in element_names:
        for greater in (False, True):
            name = f"greater_{el}_staff" if greater else f"{el}_staff"
            parts, length = staff_build(greater)
            shaft = "ebony" if greater else ("blaze" if el == "fire" else "wood")
            metal = "gold" if greater else "silver"
            wrap = "wrap_gold" if greater else "wrap"
            tex = {
                "shaft": f"wildercord:block/staff_{shaft}",
                "metal": f"wildercord:block/staff_{metal}",
                "wrap": f"wildercord:block/staff_{wrap}",
                "crystal": f"wildercord:block/staff_gem_{el}",
                "particle": f"wildercord:block/staff_gem_{el}",
            }
            g.write_json(g.ASSETS / f"models/item/{name}_in_hand.json", {
                "textures": tex, "elements": elements(parts, length, -45), "display": display})
            g.write_json(g.ASSETS / f"items/{name}.json", {"model": {
                "type": "minecraft:select", "property": "minecraft:display_context",
                "cases": [{"when": ["gui", "fixed", "on_shelf", "none"], "model": {"type": "minecraft:model", "model": f"wildercord:item/{name}"}}],
                "fallback": {"type": "minecraft:model", "model": f"wildercord:item/{name}_in_hand"}}})


def embedded_sword(lift=0.0):
    """Oathkeeper standing tip-down in its stone (block space): the tip buried, the hilt rising as it is drawn."""
    parts, length = oathkeeper()
    return elements(parts, length, None, lift=lift, flip=True, base=4)
