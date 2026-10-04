"""Mossveil: the rounded cave dormouse's skin (painted with monster_art's materials on its box-UV layout), the felt
Mossveil Cowl as worn and as an icon, the dormouse's spawn egg, the floss release sprite and the wiki field plate."""
from PIL import Image, ImageDraw

from item_art import Canvas, blit, hexc, mix, ramp
from monster_art import EGG, LIGHT, MOSS, cells, faces, fur, leaves, shade, smooth
from world_art import Sheet, box, noise

LANG = {
 'entity.wildercord.mossveil_dormouse': 'Mossveil Dormouse',
 'item.wildercord.mossveil_dormouse_spawn_egg': 'Mossveil Dormouse Spawn Egg',
 'item.wildercord.mossveil_cowl': 'Mossveil Cowl',
 'item.wildercord.mossveil_cowl.lore': 'A quiet breath beside a trusted nose.',
 'message.wildercord.mossveil.trim': 'The cowl filters %s ticks of poison. Your companion needs a rest.',
 'guide.wildercord.mossveil_dormouse.hint': 'Look for rounded little sleepers beside mature cave Glowcaps.',
 'guide.wildercord.mossveil_dormouse': 'Offer three dried Glowcap Gills, waiting five seconds between feedings. The first feeder keeps a five-minute claim; other players cannot finish it. Only her owner can order a curl with an empty hand. She follows a loaded owner by ordinary paths and never teleports across worlds. One Gill can heal a wounded companion by at most two health; full-health feeding refuses. She produces no materials and does not breed. A supported Fungal Nursery gives the Mossveil Cowl a real home: crouch still for two seconds beside your own curled companion to shorten finite poison by at most three seconds and one quarter of its remaining duration. Wear and both saved ten-second rests are paid. Stronger poison, physical damage and movement remain dangerous.',
}
for name, text in [('sniff','Dormouse nose twitches'),('nibble','Dormouse nibbles a Gill'),('trust','Dormouse answers softly'),('curl','Dormouse curls in cloth'),('idle','Dormouse chirps quietly'),('step','Tiny paws brush moss'),('filter','Stitched cowl takes a quiet breath')]:
 LANG['subtitles.wildercord.kit.mossveil.'+name] = text

# ============================================================== the dormouse (MossveilDormouseModel, 96x64)

HAZEL = ramp("#3A2212", "#5C381C", "#84532A", "#AA723E", "#C89254", "#E2B272", "#F4D09A")
CREAM = ramp("#9A8062", "#C0A47C", "#DEC69E", "#F0E0BE", "#FFF4DC")
PINK = ramp("#8A4A4C", "#BE6E70", "#E49C9A", "#F8C6BE")
PAW = ramp("#9A6656", "#C68C78", "#E2AE96", "#F4CCB4")
EYE = hexc("#140E0C")
WHISKER = hexc("#F6EEDA")

D_BELLY = box(0, 0, 8, 6, 10)
D_BREAST = box(0, 20, 6, 4, 3)
D_HEAD = box(40, 0, 6, 5, 6)
D_NOSE = box(40, 14, 3, 2, 2)
D_PINK_NOSE = box(54, 14, 1, 1, 1)
D_EAR = box(64, 0, 3, 3, 1)
D_EAR_INNER = box(74, 0, 2, 2, 1)
D_EYE = box(84, 0, 1, 1, 1)
D_PAW = box(20, 20, 2, 2, 3)
D_TOES = box(32, 20, 2, 1, 2)
D_TAIL = (box(40, 22, 2, 2, 5), box(56, 22, 2, 2, 4), box(70, 22, 1, 1, 3))
D_TUFT = box(0, 38, 4, 1, 3)


def skin():
    """A round hazel dormouse: a darker line down the back, a cream belly and breast, cream cheeks, pink ear cups, nose and
    paws, glossy black eyes, pale whiskers and a bushy tail that lightens to its tip, with a cushion of moss on its back."""
    cv = Sheet(96, 64)
    # Coat: fur stroked along the body (down the length on the top and underside, across on the flanks).
    for part, seed in ((D_BELLY, 1), (D_HEAD, 2), (D_EAR, 3)) + tuple((t, 4 + i) for i, t in enumerate(D_TAIL)):
        for name, area in faces(part):
            fur(cv, area, HAZEL, seed * 10 + len(name), name, base=3, along_y=name in ("top", "bottom") and part is not D_HEAD)
    # A darker line down the middle of the back, as wild dormice have.
    x0, y0, w, h = D_BELLY["top"]
    for y in range(h):
        for x in (3, 4):
            cv.put(x0 + x, y0 + y, HAZEL[3 if (x + y) % 3 else 2])
    # Cream belly, wrapping up the lower flanks with a ragged fur edge.
    fur(cv, D_BELLY["bottom"], CREAM, 21, "bottom", base=3, along_y=True)
    for name in ("right", "left", "front", "back"):
        x0, y0, w, h = D_BELLY[name]
        for x, y, px, py in cells((x0, y0, w, h)):
            if y == h - 1 or y == h - 2 and smooth(px, py, 23, 1.5) > 0.4:
                cv.put(px, py, shade(CREAM, 2 + LIGHT[name] + (1 if y == h - 2 else 0)))
    for name, area in faces(D_BREAST):
        fur(cv, area, CREAM if name != "top" else HAZEL, 30 + len(name), name, base=3)
    # The face: cream cheeks either side of the muzzle and under the chin, carried round onto the sides of the head.
    x0, y0, w, h = D_HEAD["front"]
    for x, y in ((0, 2), (0, 3), (1, 3), (0, 4), (1, 4), (2, 4), (3, 4), (4, 4), (5, 4), (4, 3), (5, 3), (5, 2)):
        cv.put(x0 + x, y0 + y, CREAM[3] if y < 4 else CREAM[2])
    fur(cv, D_HEAD["bottom"], CREAM, 33, "bottom", base=3)
    for name, cols in (("right", (4, 5)), ("left", (0, 1))):
        x0, y0, w, h = D_HEAD[name]
        for x in cols:
            cv.put(x0 + x, y0 + 4, CREAM[2])
            cv.put(x0 + x, y0 + 3, CREAM[3] if x in (5, 0) else HAZEL[4])
        # A dark ring round each eye, where the eye bead sits.
        ex = 3 if name == "right" else 2
        cv.put(x0 + ex, y0 + 1, HAZEL[1])
        cv.put(x0 + ex, y0 + 2, HAZEL[2])
    # Muzzle: cream with a fur bridge on top and a mouth line; the nose bead pink.
    for name, area in faces(D_NOSE):
        fur(cv, area, HAZEL if name == "top" else CREAM, 35 + len(name), name, base=4 if name == "top" else 3)
    x0, y0, w, h = D_NOSE["front"]
    cv.put(x0 + 1, y0 + 1, PINK[0])
    for name, (x0, y0, w, h) in D_PINK_NOSE.items():
        cv.put(x0, y0, PINK[3] if name == "top" else PINK[1] if name == "bottom" else PINK[2])
    # Ear cups: a lighter fur rim, pink inside.
    x0, y0, w, h = D_EAR["top"]
    for x in range(w):
        cv.put(x0 + x, y0, HAZEL[5])
    for name, (x0, y0, w, h) in D_EAR_INNER.items():
        for x, y, px, py in cells((x0, y0, w, h)):
            t = 2 if name == "front" else 1
            if name == "front" and (x == 0 or y == 0):
                t = 3
            cv.put(px, py, PINK[t])
    # Glossy black eyes, a dull sheen on top.
    for name, (x0, y0, w, h) in D_EYE.items():
        cv.put(x0, y0, hexc("#4A3A34") if name == "top" else EYE)
    # Whiskers: the strand boxes are a fraction of a pixel thick and sample the first row of their offset.
    for x in range(10):
        cv.put(x, 32, WHISKER)
    # Pink paws under a fur cuff; toes paler, with dark claw tips.
    for name, (x0, y0, w, h) in D_PAW.items():
        for x, y, px, py in cells((x0, y0, w, h)):
            if name in ("top", "bottom"):
                cv.put(px, py, PAW[1] if name == "bottom" else HAZEL[3])
            else:
                cv.put(px, py, HAZEL[3 + (1 if x == 0 else 0)] if y == 0 else shade(PAW, 2 + LIGHT[name]))
    for name, (x0, y0, w, h) in D_TOES.items():
        for x, y, px, py in cells((x0, y0, w, h)):
            cv.put(px, py, PAW[1] if name == "bottom" else shade(PAW, 3 + min(0, LIGHT[name])))
    x0, y0, w, h = D_TOES["front"]
    for x in range(w):
        cv.put(x0 + x, y0, hexc("#5A3A30") if x % 2 == 0 else PAW[3])
    # The tail lightens toward its tip; the very end is cream.
    for name, (x0, y0, w, h) in faces(D_TAIL[2]):
        for x, y, px, py in cells((x0, y0, w, h)):
            if name == "back":
                cv.put(px, py, CREAM[3])
            elif name in ("right", "left"):
                # The sides run front to back on the left (+X) face and back to front on the right.
                if x == (w - 1 if name == "left" else 0):
                    cv.put(px, py, CREAM[2])
    # A cushion of moss on the back.
    for name, area in faces(D_TUFT):
        leaves(cv, area, MOSS[1:], 40 + len(name), name)
    return cv.image()


# ============================================================== the Mossveil Cowl worn (64x32 humanoid layer)

FELT = ramp("#1E3016", "#304A22", "#46682E", "#5E8A3E", "#7CA852", "#A2C874")
FLOSS = ramp("#8E7E56", "#C8B88A", "#E8DCAE", "#FFF4D2")
STITCH = hexc("#3E5A2A")


def hood():
    """A hood of moss-green felt on the head box (offset 0,0; 8x8x8): stitched floss seams over the crown and down the
    back, a floss-trimmed face opening, and a pale floss veil over nose and mouth that does the filtering. The underside
    stays open for the neck, as on vanilla helmets."""
    cv = Sheet(64, 32)
    head = box(0, 0, 8, 8, 8)
    for name in ("top", "right", "left", "back", "front"):
        fur(cv, head[name], FELT, 50 + len(name), name, base=3 if name != "top" else 2, along_y=name != "top")
    # Seams: down the middle of the crown and the back, floss stitches every other pixel.
    x0, y0, w, h = head["top"]
    for y in range(h):
        cv.put(x0 + 3, y0 + y, FLOSS[2] if y % 2 == 0 else FELT[1])
    x0, y0, w, h = head["back"]
    for y in range(h - 1):
        cv.put(x0 + 4, y0 + y, FLOSS[1] if y % 2 == 0 else FELT[1])
    # A darker turned hem along the bottom of the sides and back.
    for name in ("right", "left", "back"):
        x0, y0, w, h = head[name]
        for x in range(w):
            cv.put(x0 + x, y0 + h - 1, FELT[1])
    # The front: felt brim, floss trim round the opening (eyes clear), and the veil below it.
    x0, y0, w, h = head["front"]
    for x in range(1, w - 1):
        cv.put(x0 + x, y0 + 2, FLOSS[3] if x < 4 else FLOSS[2])
        cv.put(x0 + x, y0 + 3, None)
        cv.put(x0 + x, y0 + 4, None)
        cv.put(x0 + x, y0 + 5, FLOSS[3] if x < 4 else FLOSS[2])
        cv.put(x0 + x, y0 + 6, STITCH if x == 3 else FLOSS[2])
        cv.put(x0 + x, y0 + 7, STITCH if x == 3 else FLOSS[1])
    for y in range(2, h):
        cv.put(x0, y0 + y, FELT[4])
        cv.put(x0 + w - 1, y0 + y, FELT[2])
    return cv.image()


# ============================================================== icons (16x16)

COWL = """
................
......oooo......
....ooHHhhoo....
...oHHhthhhmo...
..oHhhhhhhhhmo..
..oHhhhthhhhmo..
.oHhTTTTTTTTmmo.
.oHhTkkkkkkTmmo.
.oHhTkkkkkkTmmo.
.ohhTVVVVVVTmmo.
.ohhTvvsvvvTmmo.
.ohmTwwswwwTmdo.
.ohmTTTTTTTTmdo.
..ohmmmmmmmmdo..
...oooooooooo...
................
"""
COWL_PAL = {"o": "#142010", "H": "#A2C874", "h": "#6E9C48", "m": "#46682E", "d": "#304A22", "t": "#E8DCAE",
            "T": "#E8DCAE", "k": "#101A0C", "v": "#E0D2A2", "V": "#FFF4D2", "w": "#BCAC7C", "s": "#5E8A3E"}


def cowl_icon():
    cv = Canvas()
    blit(cv, COWL, {k: hexc(v) for k, v in COWL_PAL.items()})
    return cv.image()


# The spawn egg: the established egg (monster_art.EGG) in hazel, speckled cream, with the dormouse's ear cups, eyes,
# cheeks and pink nose on it.
EGG_SHELL = ("#6A4022", "#A86A38", "#D49A5E")
EGG_SPOT = "#F0DCB0"
EGG_MOTIF = """
    ................
    ................
    ...ee....ee.....
    ...epe..epe.....
    ....e....e......
    ................
    ................
    .....k..k.......
    ................
    ....cc.n.cc.....
    ................
    ................
    ................
    ................
    ................
    ................
"""
EGG_PAL = {"e": "#4A2A14", "p": "#E49C9A", "k": "#140E0C", "c": "#F4E4C2", "n": "#D46E78"}


def egg_icon():
    dark, mid, light = (hexc(c) for c in EGG_SHELL)
    outline = mix(dark, (0, 0, 0), 0.55)
    cv = Canvas()
    for y, row in enumerate(r.strip() for r in EGG.strip("\n").splitlines()):
        for x, ch in enumerate(row):
            if ch == "o":
                cv.put(x, y, outline)
            elif ch == "1":
                t = (x + y) / 26
                c = light if t < 0.42 else mid if t < 0.72 else dark
                if noise(x, y, len("mossveil_dormouse")) > 0.8:
                    c = hexc(EGG_SPOT)
                cv.put(x, y, c)
    # The ears stand out past the shell, as the bramblewalker's twigs do.
    blit(cv, EGG_MOTIF, {k: hexc(v) for k, v in EGG_PAL.items()})
    return cv.image()


def fiber():
    im = Image.new('RGBA', (16, 16))
    d = ImageDraw.Draw(im)
    d.line((2, 13, 5, 10, 8, 8, 10, 4, 13, 2), fill='#B7B787', width=2)
    d.line((2, 12, 5, 9, 8, 7, 10, 3, 13, 1), fill='#E4DDB5', width=1)
    for x, y in [(4, 11), (7, 8), (10, 5)]:
        d.line((x, y, x + 3, y + 1), fill='#768359')
    return im


def plate():
    # Original field-journal illustration; caption explicitly separates this from future native screenshots.
    im=Image.new('RGBA',(1200,640),'#ECE3CB');d=ImageDraw.Draw(im)
    for y in range(20,620,6):
        for x in range(20,1180,6):
            if noise(x,y,9833)>.8:d.point((x,y),fill='#DDD1B3')
    d.rectangle((22,22,1177,617),outline='#7B8160',width=2);d.line((60,108,1140,108),fill='#A5A582',width=2)
    d.text((60,45),'MOSSVEIL  /  A HOME FOR A QUIET NOSE',fill='#334630',stroke_width=0)
    d.text((60,78),'Illustrated field plate - not native game evidence',fill='#72674D')
    # Short, rounded belly, cream cheek, hinged ears and a curling segmented tail echo the authored rig vocabulary.
    d.ellipse((142,242,555,483),fill='#6E704A',outline='#354C30',width=4);d.ellipse((207,325,440,480),fill='#C4AE81')
    d.ellipse((102,188,352,376),fill='#80785A',outline='#354C30',width=3);d.ellipse((125,134,186,228),fill='#766D51',outline='#354C30',width=3);d.ellipse((137,147,173,211),fill='#B58070')
    d.ellipse((278,145,341,236),fill='#766D51',outline='#354C30',width=3);d.ellipse((291,158,327,217),fill='#B58070')
    d.ellipse((144,291,311,378),fill='#D0B88B');d.ellipse((147,238,175,268),fill='#29271F');d.ellipse((287,242,316,273),fill='#29271F');d.ellipse((153,240,160,247),fill='#E8DFC2');d.ellipse((295,244,302,251),fill='#E8DFC2')
    d.ellipse((215,304,244,325),fill='#A46C60');
    for side in [-1,1]:
        for step in range(3):d.line((231+side*30,329+step*7,231+side*98,318+step*12),fill='#EFE2BC',width=2)
    for x,y in [(175,470),(255,480),(391,471),(480,461)]:d.ellipse((x,y,x+45,y+20),fill='#AB876D',outline='#4D4F37',width=2)
    d.arc((397,326,660,552),20,340,fill='#C0AB81',width=18);d.arc((448,369,594,507),80,338,fill='#736647',width=14)
    d.polygon([(350,256),(382,226),(408,260),(430,229),(451,267),(420,278)],fill='#8B9D58')
    for x,y in [(194,202),(191,214),(360,288),(430,323),(323,302),(480,385)]:d.line((x,y,x+11,y+3),fill='#A5AA73',width=3)
    d.text((144,552),'Three paid Gills. Four paws. One trusted owner.',fill='#455638')
    cowl=cowl_icon().resize((256,256),Image.Resampling.NEAREST);im.alpha_composite(cowl,(790,167))
    d.text((753,449),'MOSSVEIL COWL',fill='#334630');d.text((753,480),'Helmet slot / one armour / finite wear',fill='#635C43');d.text((753,510),'A still breath beside your curled companion',fill='#635C43');d.text((753,540),'Floss + Gills + Mycelial Dew',fill='#635C43')
    return im


def write(g):
    g.save(skin(),g.ASSETS/'textures/entity/mossveil_dormouse.png')
    g.save(hood(),g.ASSETS/'textures/entity/equipment/humanoid/mossveil.png')
    g.save(fiber(),g.ASSETS/'textures/particle/mossveil_fiber.png')
    g.save(plate(),g.ROOT/'wiki/assets/mossveil/illustrated-field-plate.png')
    g.write_json(g.ASSETS/'equipment/mossveil.json',{'layers':{'humanoid':[{'texture':'wildercord:mossveil'}]}})
    # Registered independent physical particle requests its standalone sprite from the ordinary particle atlas.
    g.write_json(g.ASSETS/'particles/mossveil_fiber.json',{'textures':['wildercord:mossveil_fiber']})
    for name,paint in [('mossveil_cowl',cowl_icon),('mossveil_dormouse_spawn_egg',egg_icon)]:
        g.save(paint(),g.ASSETS/f'textures/item/{name}.png')
        g.write_json(g.ASSETS/f'models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'wildercord:item/{name}'}})
        g.write_json(g.ASSETS/f'items/{name}.json',{'model':{'type':'minecraft:model','model':f'wildercord:item/{name}'}})
    g.write_json(g.DATA/'loot_table/entities/mossveil_dormouse.json',{'type':'minecraft:entity','pools':[]})
    g.write_json(g.DATA/'recipe/mossveil_cowl.json',{'type':'minecraft:crafting_shapeless','category':'equipment','ingredients':['wildercord:moonreed_floss','wildercord:dried_glowcap_gills','wildercord:mycelial_dew'],'result':{'id':'wildercord:mossveil_cowl','count':1}})
    g.write_json(g.DATA/'tags/item/repairs_mossveil.json',{'replace':False,'values':['wildercord:moonreed_floss']})
    g.write_json(g.DATA/'advancement/recipes/mossveil_cowl.json',{'criteria':{'has_gills':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':'wildercord:dried_glowcap_gills'}]}}},'requirements':[['has_gills']],'rewards':{'recipes':['wildercord:mossveil_cowl']}})
