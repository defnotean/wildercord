"""Woven aura standards: stitched cloth, embossed crests, a bronze pole and the technique nameplate."""
import math
import random
from PIL import Image, ImageDraw

WAYS = ("blade", "bulwark", "shadowstep", "banner", "unknown")


def crest(d, way, x, y, scale=1):
    def pts(p):
        return [(x + a * scale, y + b * scale) for a, b in p]
    gold = (244, 218, 150, 255)
    dark = (75, 53, 31, 255)
    if way == "blade":
        d.polygon(pts([(0, -14), (4, -8), (2, 8), (0, 12), (-2, 8), (-4, -8)]), fill=gold)
        d.line(pts([(-8, 7), (8, 7)]), fill=gold, width=max(1, round(3 * scale)))
        d.line(pts([(0, 12), (0, 19)]), fill=dark, width=max(1, round(3 * scale)))
        d.line(pts([(0, -10), (0, 5)]), fill=(255, 248, 211, 255), width=1)
    elif way == "bulwark":
        d.polygon(pts([(-11, -12), (11, -12), (10, 5), (0, 17), (-10, 5)]), fill=gold)
        d.polygon(pts([(-7, -8), (7, -8), (6, 4), (0, 11), (-6, 4)]), fill=dark)
        d.line(pts([(0, -6), (0, 8)]), fill=gold, width=max(1, round(2 * scale)))
        d.line(pts([(-5, 0), (5, 0)]), fill=gold, width=max(1, round(2 * scale)))
    elif way == "shadowstep":
        d.ellipse((x-12*scale, y-13*scale, x+12*scale, y+11*scale), fill=gold)
        d.ellipse((x-5*scale, y-17*scale, x+17*scale, y+6*scale), fill=dark)
        for k in range(3):
            d.line(pts([(-12+k*5, 15), (-8+k*5, 20)]), fill=gold, width=max(1, round(2*scale)))
    else:
        d.polygon(pts([(-12, -1), (-6, -12), (0, -5), (6, -12), (12, -1), (8, 12), (-8, 12)]), fill=gold)
        d.line(pts([(-6, 6), (6, 6)]), fill=dark, width=max(1, round(2*scale)))
        d.polygon(pts([(0, -1), (3, 3), (0, 7), (-3, 3)]), fill=(255, 249, 214, 255))


def cloth(way):
    im = Image.new("RGBA", (64, 96))
    palette = {"blade": (127, 48, 39), "bulwark": (39, 86, 111), "shadowstep": (59, 38, 92),
               "banner": (135, 92, 33), "unknown": (75, 79, 84)}
    base = palette[way]
    for y in range(96):
        for x in range(64):
            # Open swallowtail cut, with a genuinely transparent edge.
            if y > 79 and abs(x-31.5) < (y-79)*1.7:
                continue
            weave = (3 if (x+y)%2 else -3) + (2 if y%4 == 0 else 0)
            fold = 0.82 + 0.18*math.cos(x*0.24) + 0.05*math.sin(y*0.2)
            im.putpixel((x, y), tuple(max(0, min(255, round(c*fold+weave))) for c in base)+(255,))
    d = ImageDraw.Draw(im)
    d.line([(3, 2), (3, 90)], fill=(220, 189, 120, 255), width=2)
    d.line([(60, 2), (60, 90)], fill=(220, 189, 120, 255), width=2)
    d.line([(4, 4), (59, 4)], fill=(244, 218, 151, 255), width=2)
    for y in range(9, 85, 5):
        d.point((6, y), fill=(247, 222, 156, 255))
        d.point((57, y), fill=(247, 222, 156, 255))
    # Inset brocade medallion is angular, with no runes or ritual rings.
    d.polygon([(32, 17), (52, 37), (48, 64), (32, 77), (16, 64), (12, 37)], fill=(54, 40, 30, 255),
              outline=(194, 153, 81, 255), width=2)
    crest(d, way, 32, 44)
    return im


def pole():
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            s = 0.5 + 0.5*math.sin((x+0.5)/16*math.pi)
            c = (round(148*s), round(105*s), round(53*s), 255)
            if y in (2, 3, 12, 13):
                c = (205, 174, 102, 255)
            im.putpixel((x, y), c)
    return im


def plate():
    im = Image.new("RGBA", (256, 64))
    d = ImageDraw.Draw(im)
    d.polygon([(0, 3), (242, 3), (254, 14), (247, 31), (254, 51), (235, 61), (0, 61)], fill=(45, 42, 43, 250))
    for y in range(6, 58):
        for x in range(4, 240):
            if (x+y)%4 == 0:
                d.point((x, y), fill=(61, 56, 54, 245))
    d.line([(4, 6), (238, 6), (248, 15)], fill=(218, 199, 160, 255), width=2)
    d.line([(4, 58), (232, 58), (245, 51)], fill=(157, 135, 97, 255), width=2)
    for x in range(12, 235, 8):
        d.line([(x, 10), (x+3, 10)], fill=(156, 142, 116, 255))
        d.line([(x, 54), (x+3, 54)], fill=(156, 142, 116, 255))
    return im


def finial():
    im = Image.new("RGBA", (16, 16))
    d = ImageDraw.Draw(im)
    d.polygon([(8, 0), (13, 9), (10, 12), (10, 15), (6, 15), (6, 12), (3, 9)], fill=(190, 142, 63, 255))
    d.polygon([(8, 1), (8, 11), (4, 9)], fill=(246, 216, 139, 255))
    d.line([(6, 13), (10, 13)], fill=(251, 222, 157, 255))
    return im


def write(g):
    for way in WAYS:
        g.save(cloth(way), g.ASSETS / f"textures/particle/aura_standard_{way}.png")
    g.save(pole(), g.ASSETS / "textures/particle/aura_standard_pole.png")
    g.save(finial(), g.ASSETS / "textures/particle/aura_standard_finial.png")
    g.save(plate(), g.ASSETS / "textures/gui/sprites/hud/aura_banner.png")

    for kind in range(3):
        g.save(scar(kind), g.ASSETS / f"textures/particle/aura_scar_{kind}.png")


def scar(kind):
    im = Image.new("RGBA", (192, 192))
    d = ImageDraw.Draw(im)
    r = random.Random(739 + kind)
    if kind == 1:
        # A ragged rim of overturned earth, with an offset shadow inside its depression.
        rim = [(96 + math.cos(i * math.tau / 48) * r.uniform(67, 78),
                96 + math.sin(i * math.tau / 48) * r.uniform(64, 76)) for i in range(48)]
        d.polygon(rim, fill=(30, 25, 22, 70))
        d.line(rim + rim[:1], fill=(74, 67, 56, 175), width=9)
        for x, y in rim[::2]:
            d.polygon([(x-3,y-3),(x+5,y-1),(x+3,y+3),(x-4,y+2)], fill=(132, 122, 102, 145))
    if kind == 2:
        for j in range(3):
            pts = [(45+j*25+r.uniform(-4,4), 22+i*19) for i in range(8)]
            d.line([(x+2,y) for x,y in pts], fill=(149,139,118,145),width=3)
            d.line(pts,fill=(22,21,20,215),width=3)
    else:
        for j in range(11):
            a = j * math.tau / 11 + r.uniform(-0.12,0.12)
            pts = [(96,96)]
            for k in range(1,7):
                t = k * r.uniform(10,13)
                pts.append((96+math.cos(a)*t+r.uniform(-4,4),96+math.sin(a)*t+r.uniform(-4,4)))
            d.line([(x+1,y+1) for x,y in pts],fill=(164,152,129,150),width=3)
            d.line(pts,fill=(22,21,20,220),width=2)
            x,y=pts[3]
            d.line([(x,y),(x+math.cos(a+0.8)*17,y+math.sin(a+0.8)*17)],fill=(25,23,21,200),width=1)
    return im


if __name__ == "__main__":
    import generate_assets as g
    write(g)
