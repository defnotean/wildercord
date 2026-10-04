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
 g.write_json(g.ASSETS/'models/item/rootmolt_strider_spawn_egg.json',{'parent':'minecraft:item/template_spawn_egg'})
 g.write_json(g.ASSETS/'items/rootmolt_strider_spawn_egg.json',{'model':{'type':'minecraft:model','model':'wildercord:item/rootmolt_strider_spawn_egg'}})
 g.write_json(g.DATA/'loot_table/entities/rootmolt_strider.json',{'type':'minecraft:entity','pools':[]})
