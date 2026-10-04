"""Draft original root-plated six-legged cave creature materials; no generator integration yet."""
from wildlife_art import Skin,shade,mix
from world_art import noise

def skin():
 s=Skin(128,128)
 def bark(f,x,y,z,tx,ty):
  k=(int(x*3+z*2)%7);c=(74,62,52) if k<2 else (118,91,66)
  if int(y)%4==0:c=(158,125,79)
  if f=='top' and int(x+z)%9==0:c=(118,140,95)
  return shade(c,.86+noise(tx,ty,1707)*.22)
 def plate(f,x,y,z,tx,ty):
  c=(135,143,119) if int(x+z*2)%5 else (91,106,85)
  if f=='top':c=(177,183,145) if int(x+z)%4 else (115,135,105)
  if int(z)%4==0:c=(195,183,123)
  return shade(c,.84+noise(tx,ty,1751)*.25)
 def gill(f,x,y,z,tx,ty):
  c=(203,180,141) if int(z)%2 else (106,91,83)
  if f in ('left','right'):c=(176,150,124) if int(y)%2 else (88,74,73)
  return shade(c,.9+noise(tx,ty,1791)*.15)
 def copper(f,x,y,z,tx,ty):
  c=(169,123,73) if int(x+z)%5 else (78,119,87)
  if f=='top':c=(215,183,115)
  return shade(c,.82+noise(tx,ty,1799)*.25)
 s.box(0,0,10,4,12,bark);s.box(48,0,6,4,5,bark);s.box(0,24,10,5,8,bark);s.box(48,24,12,1,14,plate)
 s.box(96,0,2,3,6,gill);s.box(0,48,2,2,7,bark);s.box(24,48,1,6,1,bark);s.box(32,48,3,6,2,plate);s.box(64,48,4,1,3,copper);s.box(48,48,1,1,5,bark)
 s.box(82,48,1,1,1,lambda f,x,y,z,tx,ty:(197,150,75) if f=='front' else (45,37,30))
 return s.image()

def spawn_egg():
 """Rootmolt egg: overlapping pale plates, bark seams, gill slits and six root-foot marks."""
 from PIL import Image
 im=Image.new('RGBA',(16,16),(0,0,0,0));px=im.load()
 rows={2:(7,8),3:(6,9),4:(5,10),5:(4,11),6:(4,11),7:(3,12),8:(3,12),9:(3,12),10:(3,12),11:(3,12),12:(4,11),13:(4,11),14:(6,9)}
 inside={(x,y) for y,(left,right) in rows.items() for x in range(left,right+1)}
 for x,y in inside:
  edge=any((x+dx,y+dy) not in inside for dx,dy in ((1,0),(-1,0),(0,1),(0,-1)))
  if edge:c=(49,42,34)
  else:
   plate=(y-3)//3;c=(165,172,137) if plate%2==0 else (126,141,111)
   if y in (6,9,12):c=(93,73,52)
   if x in (5,9) and y not in (6,9,12):c=(201,181,125)
   c=shade(c,.78+(12-x)*.035+(9-y)*.016)
  px[x,y]=(*c,255)
 # Offset plate rims remain readable at the real16px inventory size.
 for x,y in [(5,6),(6,6),(7,5),(8,5),(9,6),(10,6),(4,9),(5,9),(6,8),(7,8),(8,9),(9,9),(4,12),(5,12),(6,11),(7,11),(8,12),(9,12)]:
  if (x,y) in inside:px[x,y]=(70,61,43,255)
 for x,y in [(5,7),(6,7),(7,6),(8,6),(5,10),(6,9),(7,9),(6,12),(7,12)]:
  if (x,y) in inside:px[x,y]=(214,206,148,255)
 # Exposed gills beside the copper-edged shoulder: asymmetric organism detail.
 for x,y,c in [(9,4,(216,183,112)),(10,5,(168,121,70)),(10,7,(205,181,143)),(11,7,(95,75,66)),(10,8,(193,165,129)),(11,8,(91,75,66)),(5,5,(230,219,166)),(6,4,(206,207,157))]:
  if (x,y) in inside:px[x,y]=(*c,255)
 for x,y in [(5,10),(7,11),(9,10),(5,12),(7,13),(9,12)]:
  px[x,y]=(198,175,116,255)
 return im

LANG={
 'entity.wildercord.rootmolt_strider':'Rootmolt Strider',
 'item.wildercord.rootmolt_strider_spawn_egg':'Rootmolt Strider Spawn Egg',
 'effect.wildercord.root_tether':'Root Tether',
 'guide.wildercord.rootmolt_strider.hint':'Watch broad bark plates below damp cave banks where a snail has matured Glowcaps. Raised shovel arms announce a straight physical rake.',
 'guide.wildercord.rootmolt_strider':'Rootmolts eat mature Glowcaps, then guard the same garden while their next meal settles. Six feet carry bark plates and exposed gills; raised shovel arms commit to one straight physical rake. Step sideways during the warning, strike the creature to break its grip, or cleanse the short Root Tether. Sheltered narrow nurseries can admit a snail while excluding this broader rival. Killing it yields no materials.',
 'subtitles.wildercord.kit.rootmolt.call':'Rootmolt plates rasp',
 'subtitles.wildercord.kit.rootmolt.warn':'Rootmolt shovel arms rise',
 'subtitles.wildercord.kit.rootmolt.rake':'Roots rake across stone',
 'subtitles.wildercord.kit.rootmolt.meal':'Rootmolt gills chew',
 'subtitles.wildercord.kit.rootmolt.release':'Rootmolt roots snap loose',
}
def write(g):
 from PIL import Image,ImageDraw
 g.save(skin(),g.ASSETS/'textures/entity/rootmolt_strider.png')
 im=Image.new('RGBA',(18,18),(0,0,0,0));d=ImageDraw.Draw(im)
 for path in [[(3,2),(5,6),(4,10),(7,15)],[(13,2),(11,6),(13,10),(10,16)],[(8,1),(9,7),(7,11),(9,16)]]:
  d.line(path,fill='#40352B',width=3);d.line(path,fill='#AC9368',width=1)
 for x,y in [(4,7),(12,8),(8,12)]:d.line((x,y,x+3,y-2),fill='#DBC492')
 g.save(im,g.ASSETS/'textures/mob_effect/root_tether.png')
 g.save(spawn_egg(),g.ASSETS/'textures/item/rootmolt_strider_spawn_egg.png')
 g.write_json(g.ASSETS/'models/item/rootmolt_strider_spawn_egg.json',{'parent':'minecraft:item/generated','textures':{'layer0':'wildercord:item/rootmolt_strider_spawn_egg'}})
 g.write_json(g.ASSETS/'items/rootmolt_strider_spawn_egg.json',{'model':{'type':'minecraft:model','model':'wildercord:item/rootmolt_strider_spawn_egg'}})
 g.write_json(g.DATA/'loot_table/entities/rootmolt_strider.json',{'type':'minecraft:entity','pools':[]})
