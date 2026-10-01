"""Original spell material sprites. Each silhouette is drawn explicitly, never copied from Minecraft."""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'src/main/resources/assets/wildercord/textures/particle'

def sprite(style, frame):
    im = Image.new('RGBA', (32,32)); d = ImageDraw.Draw(im)
    bright=(255,255,255,255); soft=(205,224,242,195); dark=(132,158,190,170)
    if style==0: # curling tongue, detached ember, hot narrow core
        d.polygon([(10,27),(5,21),(8,14),(15,17),(13,10),(22,3+frame),(20,14),(26,19),(23,26),(17,29)],fill=soft)
        d.polygon([(12,25),(11,20),(17,15),(17,22),(22,23),(18,27)],fill=bright); d.ellipse((5+frame,3,8+frame,7),fill=bright)
    elif style==1: # asymmetrical ice splinter with etched facets
        d.polygon([(16,2),(24,18),(18,30),(7,19)],fill=dark); d.polygon([(16,2),(17,21),(7,19)],fill=bright)
        d.line([(17,21),(24,18),(18,30)],fill=soft,width=2); d.line([(11,14),(16,11+frame)],fill=soft)
    elif style==2: # lightning forks, different fork on alternate frame
        d.line([(22,2),(13,12),(19,13),(8,29)],fill=bright,width=3)
        d.line([(15,11),(5,10),(3,4+frame*3)],fill=soft,width=2); d.line([(14,20),(26,22),(29,17)],fill=soft,width=2)
    elif style==3: # tapered double wind ribbon
        d.arc((1,5,29,23),195+frame*8,345,fill=bright,width=3); d.arc((4,9,32,27),180,325,fill=soft,width=2)
        d.line([(21,8),(28,10),(25,14)],fill=bright,width=2)
    elif style==4: # beveled chunk with a fracture
        d.polygon([(7,6),(20,3),(28,12),(24,26),(10,29),(3,18)],fill=dark)
        d.polygon([(7,6),(20,3),(16,14),(3,18)],fill=bright); d.line([(16,14),(21,18),(18+frame,27)],fill=soft,width=2)
    elif style==5: # lobed leaf/petal with central vein
        d.polygon([(4,27),(7,12),(15,4),(27,5),(27,17),(17,26)],fill=soft)
        d.line([(5,27),(15,17),(26,6)],fill=bright,width=2); d.line([(15,17),(10,11),(16,8)],fill=dark)
    elif style==6: # hollow hooked void crescent
        d.arc((3,2,29,29),35+frame*12,290,fill=soft,width=4); d.arc((8,7,25,23),40,230,fill=bright,width=2)
        d.polygon([(5,23),(3,31),(12,27)],fill=bright)
    elif style==7: # fractured rune, open lower diamond
        d.line([(15,2),(27,13),(16,29),(5,21)],fill=bright,width=2)
        d.line([(6,16),(3,12),(10,5)],fill=soft,width=2); d.line([(10,15),(16,20),(22,12)],fill=soft,width=2)
        d.rectangle((13,10+frame,16,13+frame),fill=bright)
    elif style==8: # gear teeth and two clock hands
        d.ellipse((7,7,25,25),outline=soft,width=3)
        for x,y in [(14,3),(14,26),(3,14),(26,14),(5,5),(24,24),(5,24),(24,5)]:d.rectangle((x,y,x+3,y+3),fill=bright)
        d.line([(16,9),(16,16),(21+frame,18)],fill=bright,width=2)
    elif style==9: # stylized pointed blood drop
        d.polygon([(16,3),(25,18),(23,26),(16,29),(9,26),(7,18)],fill=soft)
        d.arc((10,12,20,25),100,210,fill=bright,width=2); d.rectangle((20,20,22,22),fill=dark)
    elif style==10: # water teardrop with clear cavity and crescent glint
        d.polygon([(16,2),(25,16),(26,23),(22,29),(10,29),(6,23),(7,16)],fill=dark)
        d.arc((9,12,24,27),25,210,fill=bright,width=3); d.line([(13,13),(16,8),(20,14)],fill=soft,width=2)
    else: # hand outlined three-lobed vapour, translucent interior
        d.ellipse((2,12,20,29),fill=(230,240,255,75)); d.ellipse((9,5,28,25),fill=(230,240,255,90))
        d.arc((3,12,21,29),80,260,fill=soft,width=2); d.arc((10,5+frame,28,25),185,335,fill=soft,width=2)
    return im

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    for style in range(12):
        for frame in range(2):sprite(style,frame).save(OUT/f'material_{style}_{frame}.png')

if __name__=='__main__':main()
