"""Original void material sprites, isolated until the current Life/fusion batch is accepted."""
from pathlib import Path
from PIL import Image, ImageDraw
import json
import random

def sprite(style, frame):
    im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
    if style==0: # Two hinged space lips with an actual transparent intervening slit.
        d.polygon([(2,7),(12,3),(14+frame,13),(11,28),(3,25)],fill=(23,25,35,250))
        d.polygon([(19,4),(29,9),(27,27),(18,29),(20-frame,16)],fill=(36,39,48,250))
        d.line([(12,3),(14+frame,13),(11,28)],fill=(168,173,190,235),width=1)
        d.line([(19,4),(20-frame,16),(18,29)],fill=(118,135,154,235),width=1)
    elif style==1: # Unequal closing jaw plates, sharp broken inner lips.
        d.polygon([(2,7),(12,2),(28,8),(22,12+frame),(18,9),(14,15+frame),(10,10),(3,15)],fill=(27,30,40,255))
        d.polygon([(4,21),(9,17-frame),(15,22),(20,18-frame),(29,24),(21,29),(7,28)],fill=(47,49,58,255))
        d.line([(3,15),(10,10),(14,15+frame),(18,9),(22,12+frame),(28,8)],fill=(179,181,193,255),width=1)
        d.line([(4,21),(9,17-frame),(15,22),(20,18-frame)],fill=(121,128,147,255),width=1)
    elif style==2: # Crooked hook tooth with a broad unlit root and chipped cutting tip.
        d.polygon([(6,29),(6,18),(14,10),(28,3),(21,16),(17,18),(12,29)],fill=(32,37,49,255))
        d.line([(6,18),(14,10),(28,3),(21,16)],fill=(165,170,189,255),width=1)
        d.line([(11,25),(16,17),(21,13)],fill=(76,87,105,255),width=2)
        if frame:d.polygon([(21,9),(25,6),(23,11)],fill=(111,124,143,255))
    elif style==3: # Ragged fabric with offset folds and three independently torn ends.
        d.polygon([(4,3),(25,5),(28,14),(25,25),(21,22),(17,30),(13,25),(8,29),(5,18)],fill=(35,34,44,245))
        d.polygon([(4,3),(12+frame,9),(9,21),(8,29),(5,18)],fill=(77,75,88,250))
        d.line([(18-frame,6),(21,15),(17,24)],fill=(112,110,128,230),width=1)
        d.line([(9,9),(7,16)],fill=(137,135,151,230),width=1)
    elif style==4: # Dense irregular vapor pockets; granular margins, dark center, no halo.
        rng=random.Random(1071+frame)
        for y in range(32):
            for x in range(32):
                radius=min(((x-10)/8)**2+((y-14)/9)**2,((x-21)/8)**2+((y-19)/7)**2,((x-19)/6)**2+((y-8)/5)**2)
                if radius<1:
                    v=rng.randrange(21,59);im.putpixel((x,y),(v,v,v+11,int((1-radius)*160*rng.uniform(.6,1))))
    elif style==5: # A fractured angular remnant, material faces rather than a light ray.
        d.polygon([(4,23),(8,16),(6,13),(19,4),(28,7),(24,17),(18,19),(12,28)],fill=(47,51,63,255))
        d.polygon([(4,23),(8,16),(19,4),(16,18),(12,28)],fill=(93,101,117,255))
        d.line([(11,12),(16+frame,15),(14,20),(19,23)],fill=(18,21,29,255),width=2)
        d.line([(19,4),(28,7)],fill=(153,160,175,255),width=1)
    elif style==6: # Sculk-like fibres, unequal fork teeth, low blue edges around black stems.
        paths=[[(15,29),(17,20),(11,15),(12,4)],[(17,21),(24,15),(27,6)],[(12,15),(5,10),(4,4)],[(17,20),(20,10),(18,3)]]
        for path in paths:d.line(path,fill=(55,112,123,210),width=4);d.line(path,fill=(11,34,43,255),width=2)
        for x,y in [(12,4),(27,6),(4,4),(18,3)]:d.point((x+frame,y),fill=(119,177,182,220))
    elif style==7: # Offset hard compression plates: interrupted pressure faces, not rings.
        for i in range(3):
            y=6+i*9;offset=(i-1)*(1+frame)
            d.polygon([(3+offset,y+2),(12,y-2),(28-offset,y+1),(24,y+5),(13,y+3),(6,y+6)],fill=(32+8*i,49+7*i,57+8*i,210))
            d.line([(3+offset,y+2),(12,y-2),(28-offset,y+1)],fill=(94+15*i,144+7*i,159+6*i,235),width=1)
    elif style==8: # Hinge shell with layered shutters and a separated pin socket.
        d.polygon([(3,7),(13,3),(28,8),(27,24),(18,29),(4,25)],fill=(53,45,65,255))
        d.polygon([(3,7),(13,3),(28,8),(17,13)],fill=(127,116,139,255))
        d.line([(4,17),(17,20),(27,16)],fill=(24,23,33,255),width=3)
        d.line([(17,13),(17+frame,26)],fill=(169,152,181,255),width=1)
        d.rectangle((5,22,8,25),fill=(86,78,103,255))
    elif style==9: # Broken silhouette scrap, ragged lower edge, empty cutout at its center.
        d.polygon([(12,3),(21,4),(24,11),(21,15),(28,23),(25,29),(18,24),(15,30),(10,25),(4,27),(6,18),(12,14),(9,9)],fill=(37,33,47,235))
        d.line([(12,3),(21,4),(24,11)],fill=(135,124,148,215),width=1)
        d.polygon([(13,15),(18,16),(20,22),(14+frame,24),(11,21)],fill=(0,0,0,0))
        d.line([(7,20),(6,25)],fill=(88,81,103,195),width=1)
    elif style==10: # A running shadow hound: long muzzle, upright ear, chest, four legs and torn tail.
        shift=frame
        d.polygon([(2,13),(5,10),(5,5),(9,9),(13,10),(18,11),(24,8),(29,10),(26,14),(29,18),(25,17),(22,15),(19,17),(17,25),(14,25),(15,17),(11,17),(8+shift,24),(5+shift,25),(6,16)],fill=(27,30,42,245))
        d.polygon([(3,12),(9,10),(13,11),(11,14),(4,15),(1,14)],fill=(47,47,61,245))
        d.line([(5,5),(9,9),(13,10),(18,11),(24,8)],fill=(148,148,171,220),width=1)
        d.line([(6,16),(5+shift,25),(8+shift,24)],fill=(88,95,119,215),width=1)
        d.line([(19,17),(17,25),(14,25)],fill=(74,80,105,215),width=1)
        d.point((6,12),fill=(164,170,186,235))
        d.polygon([(14,12),(17,13),(16,15),(13,14)],fill=(0,0,0,0))
    return im

def main():
    root=Path(__file__).resolve().parent.parent
    tex=root/'src/main/resources/assets/wildercord/textures/particle';tex.mkdir(parents=True,exist_ok=True)
    names=[]
    for style in range(11):
        for frame in range(2):
            name=f'void_material_{style}_{frame}';sprite(style,frame).save(tex/f'{name}.png');names.append('wildercord:'+name)
    p=root/'src/main/resources/assets/wildercord/particles/void_material.json';p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps({'textures':names},indent=2)+'\n',encoding='utf-8',newline='\n')

if __name__=='__main__':main()
