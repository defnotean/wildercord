"""Authored pale flight feathers, with a shaded vane, central shaft and individual barbs."""
import math
from PIL import Image


def write(g):
    image = Image.new("RGBA", (24, 96))
    for y in range(96):
        t = y / 95
        centre = 11.5 + math.sin(t * 2.8) * 1.2
        width = 9.5 * math.sin(math.pi * t) ** 0.42
        for x in range(24):
            d = (x - centre) / max(0.4, width)
            if abs(d) > 1:
                continue
            barb = (y + int(abs(x - centre) * 2.1)) % 7
            shade = int(18 * abs(d)) + (13 if barb == 0 else 0)
            if abs(x - centre) < 0.8:
                rgb = (245, 255, 255)
            elif d < 0:
                rgb = (223 - shade, 244 - shade, 251 - shade)
            else:
                rgb = (174 - shade, 211 - shade, 227 - shade)
            image.putpixel((x, y), (*rgb, 255))
    image.save(g.ASSETS / "textures/particle/soar_feather.png")
