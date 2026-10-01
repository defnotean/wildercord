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

black = (30,27,29)
charcoal = (43,38,40)
tan = (171,111,64)
gold = (194,139,87)
cream = (204,163,112)
cube(0,0,11,8,13,black)       # dark saddle
cube(0,25,10,5,12,(61,51,48)) # soft rounded belly
cube(50,26,8,6,4,(135,88,53)) # chest
cube(50,64,2,6,9,(42,36,38)) # soft side fluffs
cube(78,65,7,4,2,(168,111,67)) # chest bib
cube(52,0,9,8,7,tan)         # face
cube(0,64,9,2,7,(120,88,64)) # fringe
cube(52,43,7,4,3,(184,126,77)) # muzzle
cube(80,24,1,1,1,(24,21,23)) # nose
cube(88,25,2,4,2,gold)       # cheeks
cube(104,46,3,3,2,(150,101,66)) # central forehead lock
cube(0,46,3,7,3,(107,80,60)) # floppy ears
cube(20,46,3,6,3,(174,119,74)) # legs
cube(40,46,4,3,4,cream)      # shaggy feet
cube(66,47,3,4,5,charcoal)   # tail
cube(88,45,4,5,4,(108,79,60))

# Small deep brown eyes and warm brow points on the front of the face.
draw.point((61,11), fill=(16,14,17,255))
draw.point((65,11), fill=(16,14,17,255))
draw.point((61,10),fill=(244,219,177,255));draw.point((65,10),fill=(244,219,177,255))
draw.line((60,9,61,9), fill=(110,67,43,255))
draw.line((65,9,66,9), fill=(110,67,43,255))
# A darker mouth tucked under the nose on the muzzle's front face.
draw.line((58,49,60,49), fill=(71,45,38,255))

OUT.parent.mkdir(parents=True,exist_ok=True)
im.save(OUT)
sleeping = im.copy()
sleep_draw = ImageDraw.Draw(sleeping)
for x in (61,65):
    sleep_draw.point((x,10),fill=tan+(255,))
    sleep_draw.line((x-1,11,x+1,11),fill=(47,29,24,255))
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
print(OUT)
