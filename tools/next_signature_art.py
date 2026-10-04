"""Twelve individually drawn carved glyphs. Draft output only until root promotion."""
from pathlib import Path
import json
from PIL import Image, ImageDraw
IDS=('nullcatch','second_bell','red_ledger','quietus','blood_escrow','frost_molt',
     'pulse_ferry','last_lantern','pocket_current','wayline','night_seam','shard_compass')
def icon(name):
    im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
    d.polygon([(6,3),(22,2),(27,6),(29,23),(24,28),(7,29),(3,24),(2,8)],fill='#444940',outline='#B7BA9E')
    d.polygon([(6,7),(9,5),(22,4),(25,7),(27,22),(22,26),(8,26),(5,23),(4,9)],fill='#727764')
    d.line([(6,7),(9,5),(22,4),(25,7)],fill='#D5CEAD')
    for x,y in ((6,20),(24,12),(10,25),(21,6),(7,10)):d.point((x,y),fill='#555C4B')
    def line(points,color,width=2):
        d.line([(x+1,y+1) for x,y in points],fill='#30392F',width=width+1)
        d.line(points,fill=color,width=width)
    def bead(x,y,color,size=1):d.rectangle((x,y,x+size,y+size),fill=color)
    if name=='nullcatch':
        line([(9,7),(7,12),(8,20),(12,24)],'#B8C5BE');line([(22,7),(25,13),(23,21),(19,24)],'#CCD4C8')
        for x,y in ((11,10),(10,16),(20,11),(21,18)):line([(x,y),(x+(-2 if x<16 else 2),y+2)],'#818E89',1)
        d.polygon([(14,10),(19,14),(18,20),(12,19),(11,14)],fill='#443E52');bead(15,15,'#857094')
    elif name=='second_bell':
        line([(8,14),(9,7),(14,7),(15,14),(8,14)],'#C7A66A');line([(18,20),(19,11),(24,11),(25,20),(18,20)],'#D8C18A')
        line([(11,15),(11,18),(15,17),(16,22),(21,21)],'#EBD9A2',1);bead(10,10,'#F3DFB4');bead(21,15,'#E4C78F')
    elif name=='red_ledger':
        line([(10,6),(9,24),(22,24)],'#AD655A');line([(17,6),(17,22)],'#D8C9AC',1)
        for y in (9,15,21):line([(7,y+1),(14,y),(21,y-1)],'#D08770',1);bead(21,y-2,'#DAC6AD')
    elif name=='quietus':
        d.rectangle((12,10,20,20),fill='#434149');line([(11,8),(22,8),(23,21),(11,21)],'#B9A6CB',1)
        for x,y in ((8,10),(8,16),(21,23)):bead(x,y,'#DAC696',2)
        line([(15,7),(18,13),(14,17),(17,23)],'#B79C6C',1)
    elif name=='blood_escrow':
        line([(12,7),(22,7),(24,11),(22,23),(12,23),(10,11),(12,7)],'#CFB99F',1)
        d.polygon([(13,17),(21,17),(20,22),(14,22)],fill='#BA685B')
        for y in (11,15,19):line([(6,y),(9,y),(13,y+2)],'#CE806E',1)
        bead(17,19,'#E4A084')
    elif name=='frost_molt':
        d.polygon([(10,6),(20,7),(24,16),(19,24),(9,21),(7,13)],fill='#819E9D',outline='#D2E8D6')
        line([(11,9),(13,15),(10,19)],'#536F66',1);line([(19,10),(16,16),(18,22)],'#D9F0DF',1)
        line([(8,20),(7,24),(13,25)],'#AABD7C',1);bead(22,20,'#AABC7C')
    elif name=='pulse_ferry':
        for x in (9,23):line([(x-3,8),(x,6),(x+3,8),(x+2,13),(x-2,13),(x-3,8)],'#C9BD8E',1)
        d.polygon([(15,11),(18,15),(17,21),(14,21),(12,16)],fill='#97B76F');bead(15,16,'#D4DE93')
        line([(9,15),(12,23),(21,23),(24,16)],'#859765',1)
    elif name=='last_lantern':
        line([(11,8),(13,5),(20,5),(22,8)],'#DCD0A9',1)
        line([(9,10),(24,10),(22,24),(11,24),(9,10)],'#C4B485')
        line([(13,12),(19,12),(16,17),(19,22),(13,22),(16,17)],'#EEE0A7',1)
        bead(16,20,'#BBA064');line([(7,25),(9,24)],'#E5D39D',1)
    elif name=='pocket_current':
        d.polygon([(7,12),(10,6),(20,7),(24,13),(23,19),(16,24),(9,20)],fill='#85ABA4',outline='#CDE3C6')
        d.rectangle((12,12,20,18),fill='#534651',outline='#A395AD');line([(13,18),(18,18),(20,21)],'#DAD4B1',1)
        bead(10,10,'#DBEFDD');bead(21,14,'#A9D4C6')
    elif name=='wayline':
        line([(7,22),(12,17),(17,13),(24,7)],'#766384',1)
        for x,y in ((9,20),(15,15),(21,10)):line([(x-3,y),(x,y-3),(x+3,y),(x,y+3),(x-3,y)],'#C3D1C1',1)
        line([(23,6),(25,9),(23,12)],'#9A8A72',1)
    elif name=='night_seam':
        d.polygon([(9,7),(21,6),(23,24),(11,25)],fill='#44404D')
        for y in (10,16,22):line([(7,y+2),(15,y),(23,y-2)],'#82768C',1);bead(16,y-1,'#E4D5A1')
        line([(12,8),(13,23),(21,22)],'#B6A789',1)
    elif name=='shard_compass':
        for points,color in [([(7,15),(10,9),(12,16)],'#B7A07A'), ([(18,7),(23,9),(19,12)],'#CAB48D'), ([(22,19),(23,24),(17,22)],'#AA9576')]:d.polygon(points,fill=color)
        line([(10,22),(16,14),(21,10)],'#DAD2B3');line([(16,14),(13,17),(18,18),(16,14)],'#A299B8',1)
        for x,y in ((7,24),(10,25),(13,25)):bead(x,y,'#D4BA89')
    else:raise ValueError(name)
    return im
def write(g):
    for name in IDS:g.save(icon(name),g.ASSETS/f'textures/item/rune/{name}.png')
