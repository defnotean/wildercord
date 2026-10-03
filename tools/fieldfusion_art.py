"""Six hand drawn inventory reliefs. Intended to run after the central rune generator."""
from PIL import Image,ImageDraw
IDS=('springbed','cinder_sieve','ashen_mercy','clockroot','skylatch','thresherwind')
PALETTES=((90,167,142),(179,99,50),(128,151,96),(146,118,77),(129,150,137),(162,163,99))
def icon(index):
    im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
    # Unequal stone chips, bevel, aged inset and relief shadow preserve the existing carved rune vocabulary.
    d.polygon([(5,3),(23,2),(28,6),(29,24),(24,29),(6,28),(2,23),(3,7)],fill=(64,66,59),outline=(169,172,150))
    d.polygon([(5,7),(8,4),(23,4),(26,7),(27,23),(23,26),(7,26),(4,22)],fill=(104,108,94))
    d.line([(5,7),(8,4),(23,4),(26,7)],fill=(198,197,166),width=1)
    for x,y in [(7,9),(24,19),(5,22),(22,6),(11,25)]:d.point((x,y),fill=(79,84,73))
    color=PALETTES[index];hi=tuple(min(255,c+55) for c in color)
    def line(points,width=2,c=None):
        d.line([(x+1,y+1) for x,y in points],fill=(44,49,40),width=width+1)
        d.line(points,fill=c or color,width=width)
    if index==0:
        line([(8,9),(9,16),(12,19),(20,19),(23,16),(24,9)],2)
        for x in (11,16,21):line([(x,7),(x-1,10),(x,13)],1,hi)
        line([(16,18),(16,23),(12,24)],2,(118,146,76));line([(16,22),(21,24)],1,(158,172,94))
    elif index==1:
        line([(8,11),(10,22),(22,22),(24,11),(8,11)],2)
        for x in (12,16,20):line([(x,12),(x,20)],1,hi)
        d.rectangle((12,5,14,8),fill=(235,155,75));d.rectangle((18,6,20,9),fill=(197,107,44))
        line([(10,24),(15,25),(21,24)],1,(114,88,122))
    elif index==2:
        d.polygon([(16,22),(8,18),(7,10),(13,12)],fill=(68,59,47));d.polygon([(16,22),(24,18),(25,10),(19,12)],fill=(94,77,53))
        line([(16,23),(12,18),(11,11)],2,color);line([(16,23),(20,18),(22,12)],2,hi)
        d.polygon([(15,17),(13,12),(16,7),(19,12),(17,17)],fill=(207,132,66));d.point((16,10),fill=(248,209,136))
    elif index==3:
        line([(7,24),(10,14),(12,6)],2);line([(25,24),(22,14),(20,6)],2)
        line([(12,7),(20,7),(16,14),(20,21),(12,21),(16,14),(12,7)],1,hi)
        for y in (9,10,11,18,19):d.point((16,y),fill=(214,192,132))
        line([(9,22),(6,24)],1);line([(22,22),(26,24)],1)
    elif index==4:
        line([(8,22),(7,16),(10,10),(12,7)],2,hi);line([(24,22),(25,16),(22,10),(20,7)],2,hi)
        line([(12,10),(20,14),(12,16),(20,10),(12,10)],2,(118,87,128));line([(16,15),(16,24),(13,25)],1,(100,82,115))
        d.point((11,6),fill=(221,234,213));d.point((21,6),fill=(221,234,213))
    else:
        for y in (9,14,19):line([(7,y+2),(12,y-1),(20,y),(25,y+2)],1,hi)
        for x,y in [(11,21),(16,23),(21,22)]:d.rectangle((x,y,x+1,y+2),fill=(210,181,107))
        line([(9,22),(10,18)],1,(113,139,67));line([(20,24),(20,19)],1,(113,139,67))
    return im
def write(g):
    for index,name in enumerate(IDS):g.save(icon(index),g.ASSETS/f'textures/item/rune/{name}.png')
