"""Original living particle silhouettes; promote into tools only after the Earth milestone."""
from pathlib import Path
from PIL import Image, ImageDraw
import json
import random

def sprite(style, frame):
    im = Image.new('RGBA', (32,32))
    d = ImageDraw.Draw(im)
    if style == 0: # folded asymmetric leaf, serrations and branching veins
        d.polygon([(4,26),(6,16),(10,13),(9,9),(17,5),(27,4),(26,14),(23,15),(22,21),(15,25)],fill=(194,215,165,255))
        d.polygon([(4,26),(15,18),(27,4),(22,21),(15,25)],fill=(106,146,94,255))
        d.line([(3,29),(14,19),(25,7)],fill=(228,225,180,255),width=1)
        for a,b in [((11,22),(9,16)),((15,18),(15,11)),((19,14),(24,15))]:d.line([a,b],fill=(167,189,134,255),width=1)
    elif style == 1: # ribbed seed with pale embryonic shoot
        d.ellipse((8,6,24,27),fill=(185,158,109,255))
        d.arc((9,5,22,27),85,260,fill=(239,216,169,255),width=3)
        d.line([(15,8),(14,18),(17,25)],fill=(104,91,58,255),width=2)
        d.line([(21,10),(25,5),(28,5)],fill=(186,204,143,255),width=2)
    elif style == 2: # curled petal, scalloped edge and distinct folded face
        d.polygon([(5,8),(8,3),(13,5),(18,3),(26,8),(25,18),(18,27),(12,29),(8,20)],fill=(231,210,220,255))
        d.polygon([(5,8),(13,10),(18,27),(12,29),(8,20)],fill=(159,131,151,255))
        d.line([(13,7),(16,18),(14,27)],fill=(246,232,220,255),width=2)
    elif style == 3: # separated mushroom spores with granular membranes
        for x,y,r in [(8,8,3),(22,10,4),(13,22,4),(25,25,2)]:
            d.ellipse((x-r,y-r,x+r,y+r),fill=(206,204,160,190),outline=(142,147,110,220))
            d.point((x-1,y-1),fill=(240,230,187,210))
            d.point((x+1,y+1),fill=(107,124,86,170))
    elif style == 4: # twisted living fibre, sprouting side leaf
        d.line([(12,30),(19,23),(14,15),(20,5),(18,1)],fill=(93,134,80,255),width=6)
        d.line([(11,29),(17,22),(13,14),(18,5)],fill=(208,216,159,255),width=2)
        d.polygon([(17,20),(24,15),(29,16),(24,22),(18,23)],fill=(164,193,130,255))
        d.line([(16,21),(27,17)],fill=(222,229,176,255),width=1)
    elif style == 5: # viscous droplet with solid lip, internal streak and transparent middle
        d.polygon([(16,2),(12,12),(7,20),(9,27),(16,30),(24,26),(25,20),(20,12)],fill=(195,217,156,195))
        d.arc((7,14,25,29),5,175,fill=(105,149,80,230),width=3)
        d.line([(13,14),(10,21),(12,25)],fill=(243,242,202,235),width=2)
        d.line([(19,16),(20,24)],fill=(138,173,108,160),width=1)
    elif style == 6: # living tissue tile with cell walls; changing seam closure
        d.polygon([(4,9),(11,3),(24,5),(29,17),(23,27),(8,28),(2,18)],fill=(190,211,164,255))
        for pts in [[(4,9),(12,13),(11,3)],[(12,13),(20,16),(24,5)],[(12,13),(10,24),(3,18)],[(20,16),(23,27),(10,24)]]:
            d.line(pts,fill=(123,163,105,255),width=2)
        for x,y in [(8,12),(18,9),(17,22),(25,18)]:d.ellipse((x-1,y-1,x+1,y+1),fill=(227,231,194,255))
    elif style == 7: # curved thorn, woody heel and sharp unlit tip
        d.polygon([(7,28),(10,17),(16,11),(27,3),(23,15),(19,22),(14,29)],fill=(193,194,147,255))
        d.polygon([(7,28),(10,17),(16,11),(27,3),(15,20),(12,29)],fill=(229,225,186,255))
        d.line([(13,24),(19,17),(23,10)],fill=(103,129,79,255),width=2)
    if frame:
        if style in (0,2):d.line([(13,12),(16,18),(16,23)],fill=(209,214,177,230),width=2)
        elif style==6:d.line([(12,13),(18,16)],fill=(208,222,184,255),width=2)
        elif style==3:d.point((7,5),fill=(215,221,177,170))
        else:d.point((18,20),fill=(224,225,183,210))
    return im

def main():
    root=Path(__file__).resolve().parent.parent
    tex=root/'src/main/resources/assets/wildercord/textures/particle'
    tex.mkdir(parents=True,exist_ok=True)
    names=[]
    for style in range(8):
        for frame in range(2):
            name=f'life_{style}_{frame}'
            sprite(style,frame).save(tex/f'{name}.png')
            names.append('wildercord:'+name)
    p=root/'src/main/resources/assets/wildercord/particles/life.json'
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps({'textures':names},indent=2)+'\n',encoding='utf-8',newline='\n')

if __name__=='__main__':main()
