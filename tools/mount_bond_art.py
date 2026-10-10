"""0.13 mount bonds: the bond messages, and barding for the Wildercord mounts. Each barding texture follows its model's
128x64 box-UV layout and holds only the plates, in greys: the renderer tints it with the horse armor's colour."""
from world_art import Sheet, box, noise

LANG = {
    'message.wildercord.mount_bond.level': 'Bond %s \N{EM DASH} %s / %s',
    'message.wildercord.mount_bond.full': 'Bond %s \N{EM DASH} as close as can be',
    'message.wildercord.mount_bond.deepened': 'Your bond with %s deepens to level %s. It runs quicker and stands sturdier.',
    'message.wildercord.mount_bond.deepened.dash': 'Your bond with %s deepens to level %s. Sprint while riding and it dashes ahead.',
    'message.wildercord.mount_bond.deepened.strike': 'Your bond with %s deepens to level %s. Land from a jump and it strikes the ground, throwing back what\N{RIGHT SINGLE QUOTATION MARK}s around it.',
}

LIGHT = (236, 236, 236)
MID = (204, 204, 204)
SHADE = (164, 164, 164)
SEAM = (112, 112, 112)
RIVET = (255, 255, 255)


def plates(cv, area, rows=None, band=3):
    """Overlapping plates in bands: a bright top edge, a darker seam beneath, rivets every four pixels. With rows, only
    the bottom that many rows of the face are plated (a skirt)."""
    x0, y0, w, h = area
    start = 0 if rows is None else max(0, h - rows)
    for y in range(start, h):
        k = (y - start) % band
        for x in range(w):
            c = LIGHT if k == 0 else SEAM if k == band - 1 else MID
            if x in (0, w - 1):
                c = SHADE if c != SEAM else SEAM
            if k == 1 and (x + (y // band) * 2) % 4 == 1:
                c = RIVET
            elif c == MID and noise(x0 + x, y0 + y, 7) > 0.9:
                c = SHADE
            cv.put(x0 + x, y0 + y, c)


def chanfron(cv, part):
    """A head plate: the top and the face, with a ridge down the middle."""
    plates(cv, part["top"], band=4)
    x0, y0, w, h = part["front"]
    plates(cv, part["front"], band=4)
    for y in range(h):
        cv.put(x0 + w // 2, y0 + y, LIGHT)


def stag():
    """LumenStagModel: a skirt down each flank, a breastplate, plates on the neck and a chanfron on the head."""
    cv = Sheet(128, 64)
    body, neck, head = box(0, 0, 8, 8, 17), box(50, 0, 4, 9, 5), box(82, 0, 5, 5, 6)
    for side in ("right", "left"):
        plates(cv, body[side], rows=5)
    plates(cv, body["front"])
    for side in ("front", "right", "left"):
        plates(cv, neck[side])
    chanfron(cv, head)
    return cv.image()


def turtle():
    """ReefbackTurtleModel: a banded rim round the shell's edge, and a plate over the head."""
    cv = Sheet(128, 64)
    shell, head = box(0, 0, 16, 6, 20), box(72, 0, 6, 5, 7)
    for side in ("right", "left", "front", "back"):
        plates(cv, shell[side], rows=4, band=2)
    chanfron(cv, head)
    return cv.image()


def mole():
    """DelverMoleModel: a skirt down each flank, a breastplate and a plate over the head."""
    cv = Sheet(128, 64)
    body, head = box(0, 0, 14, 10, 18), box(64, 0, 8, 6, 7)
    for side in ("right", "left"):
        plates(cv, body[side], rows=6)
    plates(cv, body["front"])
    chanfron(cv, head)
    return cv.image()


def write(g):
    for name, image in (('ridgeback_stag', stag()), ('reefback_turtle', turtle()), ('delver_mole', mole())):
        g.save(image, g.ASSETS / f'textures/entity/wildlife/{name}_barding.png')
