"""Oathkeeper, the Bulwark Maul and the Skyrend Glaive: one authored 3D build per weapon.

Each weapon is a list of boxes standing upright (handle at the bottom, centred on x = z = 8). The held model tilts that
build onto the same diagonal a vanilla sprite uses, so the vanilla hand poses hold it, and the inventory sprite is drawn
from the same boxes. The icon and the blade in hand therefore always agree.
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
                c = _mix(m, l, max(0, t)) if t > .5 else _mix(d, m, t * 2)
            elif kind == "fuller":  # blade flat with a dark central groove and a lit ridge beside it
                t = .45 + .35 * (1 - y / 31) + (n - .5) * .08
                c = _mix(d, l, max(0, min(1, t)))
                if x in (15, 16): c = _mix(d, m, .35)
                elif x == 17: c = _mix(m, l, .85)
                elif x == 14: c = _mix(d, m, .7)
            elif kind == "edge":  # honed edge: bright, with a faint bevel line
                c = _mix(m, l, .55 + .45 * (1 - y / 31)) if x % 4 else _mix(d, m, .8)
            elif kind == "gilt":  # cast metal with worn high points
                t = .5 + (n - .5) * .5
                c = _mix(d, m, t * 2) if t < .5 else _mix(m, l, (t - .5) * 2)
                if n > .93: c = l
            elif kind == "wrap":  # leather or silk bound diagonally
                band = (x + y) % 6
                c = l if band == 0 else d if band in (4, 5) else _mix(d, m, .6 + (n - .5) * .3)
            elif kind == "grain":  # long wood grain
                t = .5 + .35 * math.sin(x * 1.3 + _noise(x // 3, y // 8, seed) * 2.5) + (n - .5) * .15
                c = _mix(d, m, max(0, t)) if t < 1 else _mix(m, l, t - 1)
            elif kind == "stone":  # riven stone with dark seams
                t = .55 + (n - .5) * .35 + (_noise(x // 4, y // 4, seed) - .5) * .3
                c = _mix(d, m, t * 1.6) if t < .62 else _mix(m, l, (t - .62) * 2)
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
    "steel": ("brushed", "#3b4a52", "#8fa2ab", "#e4eef0"),
    "fuller": ("fuller", "#33424a", "#8a9ea8", "#dfe9ec"),
    "edge": ("edge", "#7d929b", "#c9d8dc", "#ffffff"),
    "brass": ("gilt", "#5e4521", "#b48a43", "#f2d58c"),
    "wrap": ("wrap", "#14231f", "#2c4a41", "#4f7a68"),
    "gem": ("crystal", "#0f4a3a", "#2fbf8a", "#b8ffe0"),
    "dark": ("iron", "#20282b", "#3c4a4e", "#7c8e92"),
    "stone": ("stone", "#4f5750", "#7a8073", "#a8ac98"),
}
MAUL = {
    "stone": ("stone", "#4a4d57", "#80848f", "#bfc3cc"),
    "iron": ("iron", "#1c1d24", "#3d3f4a", "#8c8f9c"),
    "geode": ("crystal", "#3a1f73", "#8a5ae0", "#efe2ff"),
    "wood": ("grain", "#2e1d12", "#5a3c24", "#8a6440"),
    "leather": ("wrap", "#1d130d", "#4a2f1e", "#7c5634"),
    "brass": ("gilt", "#5e4521", "#b48a43", "#f2d58c"),
}
GLAIVE = {
    "blade": ("storm", "#2b3a5c", "#7f9fd4", "#fff3a0"),
    "steel": ("brushed", "#283552", "#6f8cc0", "#cfe0ff"),
    "edge": ("edge", "#9fb4d8", "#e8f2ff", "#fffbe0"),
    "haft": ("grain", "#10141f", "#222b40", "#3a4766"),
    "wrap": ("wrap", "#132447", "#2c4f8f", "#7fa6e6"),
    "gold": ("gilt", "#5e4521", "#c29a3a", "#fff0a8"),
    "spark": ("crystal", "#8a6a10", "#ffe066", "#ffffff"),
}
GLOW = {"gem", "geode", "spark"}

# ---------------------------------------------------------------- builds (upright, handle at y≈0)

def _b(lo, hi, mat, front=None):
    return (tuple(lo), tuple(hi), mat, front or mat)


def oathkeeper():
    """A Marchkeeper longsword: fullered blade, drooping gilt quillons, a green oath-stone in guard and pommel."""
    parts = [
        _b((7, 0, 7), (9, 1.6, 9), "brass"), _b((7.5, .4, 6.8), (8.5, 1.2, 9.2), "gem"),
        _b((7.3, 1.6, 7.35), (8.7, 5.4, 8.65), "wrap"),
        _b((7.15, 1.6, 7.2), (8.85, 2, 8.8), "brass"), _b((7.2, 3.4, 7.25), (8.8, 3.6, 8.75), "brass"),
        _b((7.15, 5, 7.2), (8.85, 5.4, 8.8), "brass"),
        _b((6.6, 5.4, 7), (9.4, 6.6, 9), "brass"), _b((7.6, 5.6, 6.8), (8.4, 6.4, 9.2), "gem"),
        _b((4.2, 5.7, 7.3), (11.8, 6.4, 8.7), "brass"),
        _b((3.6, 4.7, 7.25), (4.6, 5.7, 8.75), "brass"), _b((11.4, 4.7, 7.25), (12.4, 5.7, 8.75), "brass"),
        _b((7.4, 6.6, 7.1), (8.6, 7.6, 8.9), "brass"),
        _b((7.1, 6.6, 7.7), (8.9, 17, 8.3), "steel", "fuller"),
        _b((6.6, 6.6, 7.85), (7.1, 17, 8.15), "edge"), _b((8.9, 6.6, 7.85), (9.4, 17, 8.15), "edge"),
        _b((7.3, 17, 7.72), (8.7, 19, 8.28), "steel", "fuller"),
        _b((6.85, 17, 7.86), (7.3, 19, 8.14), "edge"), _b((8.7, 17, 7.86), (9.15, 19, 8.14), "edge"),
        _b((7.5, 19, 7.75), (8.5, 20.2, 8.25), "steel"),
        _b((7.15, 19, 7.87), (7.5, 20.2, 8.13), "edge"), _b((8.5, 19, 7.87), (8.85, 20.2, 8.13), "edge"),
        _b((7.6, 20.2, 7.8), (8.4, 21, 8.2), "edge"),
    ]
    return parts, 21


def bulwark_maul():
    """A riven-stone maul bound in iron, a geode broken open in each striking face."""
    parts = [
        _b((7.5, -.6, 7.5), (8.5, 0, 8.5), "iron"), _b((7.1, 0, 7.1), (8.9, 1.2, 8.9), "iron"),
        _b((7.3, 1.2, 7.3), (8.7, 5.6, 8.7), "leather"),
        _b((7.4, 5.6, 7.4), (8.6, 14, 8.6), "wood"),
        _b((7.2, 5.6, 7.2), (8.8, 6.1, 8.8), "brass"), _b((7.25, 9.5, 7.25), (8.75, 9.9, 8.75), "iron"),
        _b((7.75, 10.5, 7.3), (8.25, 13, 8.7), "iron"),
        _b((7, 13, 7), (9, 14.5, 9), "iron"),
        _b((4.5, 14.5, 5.6), (11.5, 19.6, 10.4), "stone"),
        _b((6.2, 14.3, 5.4), (6.9, 19.8, 10.6), "iron"), _b((9.1, 14.3, 5.4), (9.8, 19.8, 10.6), "iron"),
        _b((3.7, 14.2, 5.3), (4.5, 19.9, 10.7), "iron"), _b((11.5, 14.2, 5.3), (12.3, 19.9, 10.7), "iron"),
        _b((3.2, 15.6, 6.8), (3.7, 18.4, 9.2), "geode"), _b((12.3, 15.6, 6.8), (12.8, 18.4, 9.2), "geode"),
        _b((2.9, 16.4, 7.5), (3.2, 17.6, 8.5), "geode"), _b((12.8, 16.4, 7.5), (13.1, 17.6, 8.5), "geode"),
        _b((7.2, 15.8, 5.3), (8.8, 18.2, 5.6), "geode"), _b((7.2, 15.8, 10.4), (8.8, 18.2, 10.7), "geode"),
        _b((7.3, 19.6, 7.2), (8.3, 21, 8.2), "geode"), _b((8.1, 19.6, 8), (8.8, 20.5, 8.7), "geode"),
    ]
    return parts, 21.6


def skyrend_glaive():
    """A long storm-lacquered haft and a broad crescent of storm-steel, its edge holding a caught lightning."""
    parts = [
        _b((7.6, -.8, 7.6), (8.4, 0, 8.4), "steel"), _b((7.2, 0, 7.2), (8.8, 1, 8.8), "gold"),
        _b((7.45, 1, 7.45), (8.55, 14, 8.55), "haft"),
        _b((7.25, 2.4, 7.25), (8.75, 2.7, 8.75), "gold"), _b((7.3, 2.7, 7.3), (8.7, 6.2, 8.7), "wrap"),
        _b((7.25, 6.2, 7.25), (8.75, 6.5, 8.75), "gold"), _b((7.3, 9.6, 7.3), (8.7, 10.8, 8.7), "wrap"),
        _b((7, 13.4, 7), (9, 15, 9), "gold"), _b((7.5, 13.8, 6.8), (8.5, 14.6, 9.2), "spark"),
        _b((7.3, 15, 7.3), (8.7, 15.6, 8.7), "gold"),
        # The back spike: a short hooked tooth behind the blade.
        _b((5.4, 16.2, 7.8), (7, 17.2, 8.2), "steel"), _b((4.7, 16.7, 7.85), (5.4, 17.9, 8.15), "steel"),
        _b((4.3, 17.6, 7.88), (4.9, 18.4, 8.12), "edge"),
    ]
    # The crescent swells out to the cutting side (+x) and sweeps to a point.
    blade = [(15.6, 17, 7, 10.4), (17, 18.5, 6.9, 11.4), (18.5, 20, 7, 12), (20, 21.5, 7.3, 12.2),
             (21.5, 23, 7.8, 12), (23, 24.2, 8.5, 11.4), (24.2, 25.2, 9.3, 10.8)]
    for lo, hi, x0, x1 in blade:
        parts.append(_b((x0, lo, 7.75), (x1, hi, 8.25), "steel", "blade"))
        parts.append(_b((x1, lo, 7.85), (x1 + .45, hi, 8.15), "edge"))
    parts.append(_b((9.7, 25.2, 7.85), (10.6, 26, 8.15), "edge"))
    parts.append(_b((10, 26, 7.9), (10.4, 26.6, 8.1), "edge"))
    return parts, 27.4


WEAPONS = {
    # name: (build, materials, texture prefix, diagonal: -45 = tip up-right like a sword, 45 = tip up-left like a spear)
    "oathkeeper": (oathkeeper, OATH, "oathkeeper", -45),
    "bulwark_maul": (bulwark_maul, MAUL, "bulwark_maul", -45),
    "skyrend_glaive": (skyrend_glaive, GLAIVE, "skyrend_glaive", 45),
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
# The vanilla spear poses, scaled to the glaive's longer build so it reaches as far as a diamond spear does.
SPEAR = {
    "firstperson_righthand": {"rotation": [-20, 90, -35], "translation": [3.13, 2, .13], "scale": [1.1, 1.1, 1.1]},
    "firstperson_lefthand": {"rotation": [-20, -90, 35], "translation": [3.13, 2, .13], "scale": [1.1, 1.1, 1.1]},
    "thirdperson_righthand": {"rotation": [5, 270, -40], "translation": [0, 2, 2], "scale": [1.4, 1.4, 1.4]},
    "thirdperson_lefthand": {"rotation": [5, -270, 40], "translation": [0, 2, 2], "scale": [1.4, 1.4, 1.4]},
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
    display = dict(SPEAR if angle > 0 else HANDHELD)
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


def embedded_sword(lift=0.0):
    """Oathkeeper standing tip-down in its stone (block space): the tip buried, the hilt rising as it is drawn."""
    parts, length = oathkeeper()
    return elements(parts, length, None, lift=lift, flip=True, base=4)
