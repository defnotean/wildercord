"""Hand drawn terrain, rune glyphs, and construct models for physical magic."""
from pathlib import Path
import json
import math
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parent.parent
ASSETS=ROOT/'src/main/resources/assets/wildercord'
# A wall with strata; lifted droplet; ascending platforms; flame battlement; leaf wall;
# boiling crest; forked water; frozen stairs; charged footprints.
GLYPHS={
 'strata_rise':['#.#.#.#','#######','#.....#','#######','#.....#','#######','..#.#..'],
 'tidal_lift':['...#...','..#.#..','.#...#.','#..#..#','.#...#.','..###..','##...##'],
 'wind_steps':['.....##','...##..','.......','.##..#.','.......','##..#..','...##..'],
 'cinder_bulwark':['...#...','.#.#.#.','#.#.#.#','#######','#..#..#','#.###.#','#######'],
 'root_bulwark':['.##.##.','#..#..#','..###..','#######','#..#..#','#######','.#.#.#.'],
 'boiling_surge':['.#...#.','...#...','..#.#..','.#...#.','##.#.##','#.###.#','.##.##.'],
 'thunder_tide':['....##.','..###..','...#...','.##.##.','#.....#','.#####.','##...##'],
 'rime_causeway':['....#.#','....###','..#.#..','..###..','#.#....','###....','.......'],
 'thunder_walk':['....##.','...##..','....#..','..##.#.','.##....','..#....','##.....'],
}
def write(path,value):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value,indent=2)+'\n')
def main():
 colors={'raised_strata':(113,106,98),'cinder_bulwark':(67,45,37),'root_bulwark':(64,88,43),'lifted_water':(41,124,164),'wind_step':(61,147,141),'rime_step':(89,161,186),'thunder_step':(108,99,57)}
 for name,color in colors.items():
  im=Image.new('RGBA',(16,16),color+(255,))
  # Small shaded strata and pores give surfaces depth without photographic noise.
  for y in range(16):
   for x in range(16):
    tone=((x*7+y*11+x*y*3)%9)-4
    alpha=145 if name=='lifted_water' else 200 if name=='wind_step' else 255
    im.putpixel((x,y),tuple(max(0,min(255,c+tone)) for c in color)+(alpha,))
  d=ImageDraw.Draw(im)
  if name in ('raised_strata','cinder_bulwark'):
   for y,points in [(3,[(0,3),(5,4),(11,2),(15,3)]),(9,[(0,9),(4,8),(10,10),(15,8)]),(13,[(0,13),(7,12),(15,14)])]:
    d.line(points,fill=(224,125,53) if name=='cinder_bulwark' else (157,145,127),width=1)
   for x,y in [(2,2),(8,6),(13,12),(4,14)]:d.rectangle((x,y,x+1,y+1),fill=tuple(min(255,c+21) for c in color))
  elif name=='root_bulwark':
   for points in [[(2,15),(4,9),(8,6),(7,0)],[(14,15),(11,10),(8,6),(12,1)],[(0,6),(4,9),(8,6),(15,6)]]:d.line(points,fill=(152,116,66),width=2)
   for x,y in [(3,3),(11,8),(4,12)]:d.polygon([(x,y),(x+3,y-1),(x+2,y+2)],fill=(138,185,85))
  elif name=='lifted_water':
   for y in range(16):
    for x in range(16):
     wave=int(11*math.sin(x*.6+y*.42))
     im.putpixel((x,y),(41+wave,124+wave,164+wave,120+(x+y)%4*8))
   d.arc((1,1,14,14),200,350,fill=(178,239,246,185),width=1);d.arc((-4,8,10,19),180,330,fill=(73,192,214,175),width=1)
   d.line([(3,5),(5,3),(10,3)],fill=(216,253,255,210));d.rectangle((11,9,13,10),fill=(81,166,202,155))
  else:
   d.rectangle((1,1,14,14),outline=(188,230,228));d.arc((3,3,12,11),190,350,fill=(230,249,247),width=1)
   if name=='rime_step':d.line([(8,3),(8,12),(4,8),(12,8)],fill=(242,253,255))
   elif name=='thunder_step':d.line([(10,3),(6,8),(10,8),(6,13)],fill=(255,234,115),width=2)
   else:d.line([(4,12),(7,10),(10,11),(12,9)],fill=(203,252,235))
  out=ASSETS/f'textures/block/{name}.png';out.parent.mkdir(parents=True,exist_ok=True);im.save(out)
  if name.endswith('step'):
   model={'textures':{'all':f'wildercord:block/{name}','particle':f'wildercord:block/{name}'},'elements':[{'from':[0,0,0],'to':[16,3,16],'faces':{face:{'texture':'#all'} for face in ['up','down','north','south','east','west']}}]}
  elif name=='lifted_water':
   model={'textures':{'all':f'wildercord:block/{name}','particle':f'wildercord:block/{name}'},'elements':[
    {'from':low,'to':high,'faces':{face:{'texture':'#all'} for face in ['up','down','north','south','east','west']}}
    for low,high in [([1,2,1],[15,13,15]),([3,0,3],[13,15,13]),([5,14,5],[11,16,11])]]}
  else:model={'parent':'minecraft:block/cube_all','textures':{'all':f'wildercord:block/{name}'}}
  write(ASSETS/f'models/block/{name}.json',model);write(ASSETS/f'blockstates/{name}.json',{'variants':{'':{'model':f'wildercord:block/{name}'}}})
 write(ASSETS/'models/block/water_reservation.json',{'textures':{'particle':'wildercord:block/lifted_water'},'elements':[]})
 write(ASSETS/'blockstates/water_reservation.json',{'variants':{'':{'model':'wildercord:block/water_reservation'}}})

def circles(bands,marks,second):
 # Each has a distinct manually illustrated glyph; rings have alternating stratified bands
 # and an explicit index of corner ticks. These only extend the new physical roster.
 for index,(name,rows) in enumerate(GLYPHS.items()):
  glyph={(4+x,4+y) for y,row in enumerate(rows) for x,ch in enumerate(row) if ch=='#'}
  rim={(x,y) for x in range(2,14) for y in (1,14)}|{(x,y) for y in range(2,14) for x in (1,14)}
  whole=glyph|rim
  band={(x,y) for x in range(16) for y in (6,9)}|{(x,3) for x in range(index+1)}|{(x,12) for x in range(15-index,16)}
  if name in second:
   color=second[name][2];bands[name]={p for p in band if p[0]<8};marks[name]={p for p in whole if p[0]<8}
   second[name]=({p for p in band if p[0]>=8},{p for p in whole if p[0]>=8},color)
  else:bands[name]=band;marks[name]=whole
if __name__=='__main__':main()
