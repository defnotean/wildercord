"""Paint Cinnamon's tiny custom-model skin from a repeatable, hand-tuned palette."""
from pathlib import Path
from PIL import Image, ImageDraw
import random

OUT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/wildercord/textures/entity/cinnamon.png"
im = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
draw = ImageDraw.Draw(im)
rng = random.Random(94813)

def face(rect, base, fur=True, light=0):
    x0, y0, x1, y1 = rect
    if x1 <= x0 or y1 <= y0: return
    r, g, b = base
    draw.rectangle((x0, y0, x1-1, y1-1), fill=(max(0,min(255,r+light)), max(0,min(255,g+light)), max(0,min(255,b+light)),255))
    if fur:
        for _ in range(max(4, (x1-x0)*(y1-y0)//2)):
            x = rng.randrange(x0,x1); y = rng.randrange(y0,y1)
            delta = rng.choice([-29,-19,-11,9,16,25])
            color = tuple(max(0,min(255,c+light+delta)) for c in base)+(255,)
            draw.line((x,y,min(x1-1,x+rng.choice([0,1])),min(y1-1,y+rng.choice([0,1,2]))), fill=color)

def cube(u,v,w,h,d,color):
    # Vanilla cuboid net: top, bottom, left, front, right, back.
    face((u+d,v,u+d+w,v+d), color,light=12)
    face((u+d+w,v,u+d+w+w,v+d), color,light=-19)
    face((u,v+d,u+d,v+d+h),color,light=-12)
    face((u+d,v+d,u+d+w,v+d+h),color,light=8)
    face((u+d+w,v+d,u+2*d+w,v+d+h),color,light=-9)
    face((u+2*d+w,v+d,u+2*d+2*w,v+d+h),color,light=-24)

def lower(u,v,w,h,d,rows,color):
    """Repaints a cube's underside and the bottom `rows` of its four sides in `color`."""
    face((u+d+w,v,u+d+2*w,v+d), color,light=-6)
    for x0,x1,light in ((u,u+d,-12),(u+d,u+d+w,8),(u+d+w,u+2*d+w,-9),(u+2*d+w,u+2*d+2*w,-24)):
        face((x0,v+d+h-rows,x1,v+d+h), color,light=light)

black = (30,27,29)
white = (234,228,218)
tan = (171,111,64)
cream = (204,163,112)
cube(0,0,11,8,13,black)       # dark saddle
cube(0,25,10,5,12,(61,51,48)) # soft rounded belly
cube(50,26,8,6,4,(135,88,53)) # chest
cube(50,64,2,6,9,(42,36,38)) # soft side fluffs
cube(78,65,7,4,2,white)       # white chest bib
hair = (120,88,64)
cube(52,0,9,6,7,tan)         # round face: the wide core...
cube(0,82,7,8,6,tan)         # ...and the taller, narrower block that rounds its corners
face((59,0,68,7), hair,light=12)                 # darker hair over the top of her head
face((6,82,13,88), hair,light=12)
for x0,x1,light in ((0,6,-12),(6,13,8),(13,19,-9),(19,26,-24)):
    face((x0,88,x1,89), hair,light=light)
for x0,x1,light in ((52,59,-12),(68,75,-9),(75,84,-24)):
    face((x0,7,x1,8), hair,light=light)
cube(52,43,5,3,2,(184,126,77)) # small snout
cube(80,24,2,1,1,(24,21,23)) # black nose
cube(104,46,3,1,2,(150,101,66)) # little tuft on top
cube(0,46,3,5,3,(107,80,60)) # floppy ears
cube(20,46,4,6,4,(174,119,74)) # fluffy legs, lightening to cream at the feet
lower(20,46,4,6,4,2,cream)
cube(40,46,3,3,3,black)      # little black nub of a tail

# Her underside turns white from the chest back: the chest's lower half (over a soft edge), the belly and the body's underside.
lower(50,26,8,6,4,5,(196,160,128))
lower(50,26,8,6,4,4,white)
lower(0,25,10,5,12,3,white)
face((24,0,35,13), white,light=-6)

# Her collar (red, like her bone), its little gold bell, the tip of her tongue, and her pink bow.
cube(0,100,8,2,2,(159,41,39))
cube(24,100,2,2,2,(222,178,64))
draw.point((26,103), fill=(92,64,20,255))   # the bell's slot, on its front
cube(34,100,1,1,1,(232,118,138))
cube(40,100,1,1,1,(196,64,122))             # the knot
cube(46,100,2,2,1,(240,116,170))            # the loops
draw.point((47,101), fill=(255,186,214,255)); draw.point((49,101), fill=(255,186,214,255))

# Plain black eyes under little angry brows, a shade darker than her face, dipping toward the middle.
# The fur speckle is smoothed away around them so they read.
face((59,7,68,10), tan, fur=False, light=8)
draw.point((61,9), fill=(12,10,12,255))
draw.point((65,9), fill=(12,10,12,255))
brow = (104,60,32,255)
for x,y in ((60,7),(61,7),(62,8),(64,8),(65,7),(66,7)):
    draw.point((x,y), fill=brow)

OUT.parent.mkdir(parents=True,exist_ok=True)
im.save(OUT)
sleeping = im.copy()
sleep_draw = ImageDraw.Draw(sleeping)
for x in (61,65):
    sleep_draw.line((x-1,9,x+1,9),fill=(47,29,24,255))
sleeping.save(OUT.with_name("cinnamon_sleeping.png"))

# Cinnamon's red rubber bone, shaded in small panels so it reads beside the older item icons.
toy = Image.new("RGBA",(16,16),(0,0,0,0))
toy_draw = ImageDraw.Draw(toy)
toy_draw.rectangle((4,6,11,9),fill=(159,41,39,255))
for x,y in ((3,5),(3,9),(10,5),(10,9)):
    toy_draw.rectangle((x,y,x+2,y+2),fill=(114,29,31,255))
    toy_draw.line((x,y,x+1,y),fill=(242,121,87,255))
toy_draw.line((5,6,10,6),fill=(228,88,62,255))
toy_draw.line((5,9,10,9),fill=(100,26,32,255))
for x in (6,8,10):toy_draw.point((x,7),fill=(242,139,105,255))
toy.save(OUT.parents[1]/"item/cinnamon_toy.png")

# Her bow, as an item: two pink loops either side of a deeper knot, with short ribbon tails.
bow = Image.new("RGBA",(16,16),(0,0,0,0))
bow_draw = ImageDraw.Draw(bow)
pink, deep, light, edge = (240,116,170,255), (196,64,122,255), (255,186,214,255), (150,40,92,255)
for side in (-1, 1):
    cx = 8 + side * 4
    bow_draw.polygon([(8, 7), (cx, 3), (cx + side, 4), (cx + side, 10), (cx, 11), (8, 8)], fill=pink, outline=edge)
    bow_draw.point((cx, 5), fill=light); bow_draw.point((cx - side, 4), fill=light)
    bow_draw.line((8 + side, 9, 8 + side * 3, 13), fill=deep)
    bow_draw.point((8 + side * 3, 14), fill=edge)
bow_draw.rectangle((7, 6, 8, 9), fill=deep, outline=edge)
bow_draw.point((7, 6), fill=pink)
bow.save(OUT.parents[1]/"item/cinnamon_bow.png")
print(OUT)

# A little warm copper whistle with the familiar red cord, made by the same pixel-art generator.
whistle = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
w = ImageDraw.Draw(whistle)
w.line((3, 4, 5, 2, 8, 2, 10, 4), fill=(155, 48, 43, 255), width=2)
w.polygon([(3, 7), (7, 4), (11, 6), (11, 10), (7, 13), (3, 10)], fill=(185, 94, 57, 255), outline=(83, 48, 39, 255))
w.line((4, 7, 7, 5, 10, 6), fill=(255, 186, 119, 255))
w.rectangle((9, 7, 14, 9), fill=(198, 112, 66, 255), outline=(83, 48, 39, 255))
w.rectangle((7, 7, 8, 8), fill=(55, 40, 36, 255))
whistle.save(OUT.parents[1]/"item/cinnamon_whistle.png")
