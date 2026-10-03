"""Hand-drawn earth materials: layered rock, sediment, fracture, bone, roots and geodes."""
from pathlib import Path
from PIL import Image, ImageDraw
import math
import random
import json

ROOT = Path(__file__).resolve().parent.parent

def sprite(style, frame):
    im = Image.new('RGBA', (32, 32))
    d = ImageDraw.Draw(im)
    if style == 0:  # Three shaded faces and a fractured seam: irregular, weighty rock.
        d.polygon([(4,10),(11,3),(24,5),(29,17),(23,28),(9,27),(3,20)], fill=(160,156,142,255))
        d.polygon([(4,10),(11,3),(24,5),(18,13)], fill=(226,221,204,255))
        d.polygon([(18,13),(24,5),(29,17),(23,28),(18,23)], fill=(116,116,111,255))
        d.line([(7,12),(11,17),(10,22),(17,25)], fill=(81,79,72,255), width=2)
        d.line([(20,9),(22,13),(18,18)], fill=(93,91,84,255), width=1)
        d.point((9,8), fill=(243,237,220,255))
    elif style == 1:  # Stratified plate with chipped ends and exposed courses.
        d.polygon([(3,9),(23,5),(29,10),(28,20),(8,25),(3,20)], fill=(147,146,130,255))
        d.polygon([(3,9),(23,5),(29,10),(8,15)], fill=(222,215,189,255))
        for y in [16,19,22]: d.line([(6,y),(26,y-5)], fill=(87+y*2,84+y*2,73+y*2,255), width=2)
        d.line([(11,14),(15,19),(14,23)], fill=(58,57,49,255), width=1)
    elif style == 2:  # Disconnected sediment grains of unequal size.
        for x,y,r,c in [(6,10,3,211),(17,6,2,160),(23,19,3,194),(9,24,2,124),(16,17,1,231),(26,7,1,151)]:
            d.polygon([(x-r,y),(x,y-r),(x+r,y),(x+1,y+r)], fill=(c,c-8,c-22,255))
            d.point((x,y-1), fill=(245,231,206,255))
    elif style == 3:  # Dust is soft sediment, with granular ragged edges and no glow.
        rng = random.Random(931 + frame)
        for y in range(32):
            for x in range(32):
                r=((x-16)/13)**2+((y-16)/9)**2
                if r < 1.1:
                    a=int(max(0,1-r)*150*rng.uniform(.5,1))
                    v=rng.randrange(146,219)
                    im.putpixel((x,y),(v,v-9,v-25,a))
    elif style == 4:  # A branching dark fault, bordered with newly exposed chipped rock.
        path=[(4,4),(11,11),(9,16),(20,21),(24,29)]
        d.line(path,fill=(198,185,154,255),width=5)
        d.line(path,fill=(53,49,41,255),width=2)
        d.line([(10,12),(21,9),(28,11)],fill=(84,76,61,255),width=2)
        d.line([(17,20),(8,26)],fill=(68,64,53,255),width=2)
    elif style == 5:  # Dense ivory spur: broad socket, ribbed shaft, cutting tip.
        d.polygon([(10,28),(7,21),(12,13),(17,3),(19,14),(23,22),(21,28)],fill=(219,217,196,255))
        d.polygon([(10,27),(12,14),(17,3),(15,24)],fill=(249,243,217,255))
        d.polygon([(17,9),(19,14),(23,22),(21,28),(16,25)],fill=(139,143,132,255))
        for y in [17,21,25]:d.line([(11,y),(19,y-1)],fill=(180,179,156,255),width=1)
    elif style == 6:  # Living woody roots, bark ridges and earth still clinging to the fork.
        d.line([(16,29),(15,21),(20,14),(18,6)],fill=(87,80,57,255),width=6)
        d.line([(16,23),(8,16),(5,5)],fill=(103,94,61,255),width=4)
        d.line([(20,14),(27,7)],fill=(142,129,86,255),width=3)
        d.line([(15,29),(14,22),(19,14),(17,6)],fill=(219,198,145,255),width=1)
        d.ellipse((10,25,19,31),fill=(128,114,80,255))
    elif style == 7:  # Split hexagonal crystal with shaded mineral inclusions, never self-lit.
        d.polygon([(16,2),(24,10),(22,25),(16,30),(9,24),(8,10)],fill=(161,172,176,255))
        d.polygon([(16,2),(16,29),(9,24),(8,10)],fill=(216,224,220,255))
        d.polygon([(16,2),(24,10),(22,25),(16,30)],fill=(107,124,136,255))
        d.line([(16,3),(17,13),(15,21),(16,29)],fill=(239,242,224,255),width=1)
        d.polygon([(10,17),(14,13),(15,21),(11,23)],fill=(127,146,153,255))
    if frame and style != 3:
        # Chipping exposes a second textured face instead of a pulsing recolour.
        d.line([(21,22),(23,25),(20,27)],fill=(74,73,66,235),width=1)
    return im

def main():
    tex=ROOT/'src/main/resources/assets/wildercord/textures/particle'
    tex.mkdir(parents=True,exist_ok=True)
    textures=[]
    for style in range(8):
        for frame in range(2):
            name=f'earth_{style}_{frame}'
            sprite(style,frame).save(tex/f'{name}.png')
            textures.append('wildercord:'+name)
    p=ROOT/'src/main/resources/assets/wildercord/particles/earth.json'
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps({'textures':textures},indent=2)+'\n',encoding='utf-8',newline='\n')

if __name__ == '__main__':main()
