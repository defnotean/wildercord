"""Original ceramic-fur creature UV skin and branching cooling fern; dormant candidate."""
from wildlife_art import Skin,shade
from world_art import noise

def skin():
 s=Skin(128,128)
 def fur(f,x,y,z,tx,ty):
  c=(53,48,44) if int(x+z*2)%4 else (91,78,62)
  if f=='top':c=(104,87,67) if int(x+z)%3 else (46,42,39)
  return shade(c,.83+noise(tx,ty,8127)*.28)
 def clay(f,x,y,z,tx,ty):
  c=(143,81,54) if int(x+y)%5 else (76,44,34)
  if int(z*2+y)%7==0:c=(196,140,90)
  return shade(c,.87+noise(tx,ty,8191)*.24)
 def valve(f,x,y,z,tx,ty):
  c=(202,157,106) if int(x+z)%4 else (78,55,43)
  return shade(c,.84+noise(tx,ty,8233)*.3)
 for u,v,w,h,d,m in [(0,0,12,8,16,fur),(0,28,13,6,4,clay),(64,0,8,6,8,fur),(64,18,6,3,4,fur),(96,18,2,4,2,clay),(0,42,8,2,4,clay),(32,42,7,1,3,valve),(52,42,3,7,4,fur),(68,42,4,2,5,clay),(88,42,4,3,8,fur),(0,54,6,1,3,valve)]:s.box(u,v,w,h,d,m)
 s.box(112,18,1,1,1,lambda f,x,y,z,tx,ty:(223,168,83) if f=='left' or f=='right' else (28,24,22))
 return s.image()

def fern(age,cooled):
 from PIL import Image,ImageDraw
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im);stem=(126,101,65,255);leaf=(96,131,91,255) if cooled else (162,81,44,255);edge=(190,175,102,255) if cooled else (231,155,76,255)
 top=23-age*7;d.line((16,31,15,top),fill=stem,width=2)
 for y in range(top+2,31,4):
  span=min(11,3+(31-y)//2)
  if not cooled:span=max(2,span-3)
  for side in [-1,1]:
   tip=y-4 if cooled else min(31,y+1)
   d.line((16,y,16+side*span,tip),fill=leaf,width=3);d.line((16,y,16+side*span,tip),fill=edge,width=1)
   for k in range(2,span,3):d.line((16+side*k,y-1-k//3,16+side*(k+1),y-5-k//3),fill=leaf,width=2)
 return im
LANG={'entity.wildercord.cinder_bailiff':'Cinder Bailiff','item.wildercord.cinder_bailiff_spawn_egg':'Cinder Bailiff Spawn Egg','block.wildercord.cinder_fern':'Cinder Fern','guide.wildercord.cinder_bailiff.hint':'Listen for three ceramic vent clicks near a cooled woodland fern.','guide.wildercord.cinder_bailiff':'A Bailiff rests only after reaching and browsing a mature cooled fern. Raised vents warn of three fixed ash lanes. Step behind it or use solid cover; each fan can strike you once. Ordinary physical damage during its warning or a successful cooling spell interrupts preparation. Its fern root survives browsing and harvest. Killing it grants no special materials.'}
for n,t in [('warn','Ceramic vents lift'),('fan','Bailiff ash vents sweep'),('recover','Bailiff breath settles'),('browse','Bailiff browses dry fronds'),('cool','Hot vents cool')]:LANG['subtitles.wildercord.kit.ember_bailiff.'+n]=t
LANG['subtitles.wildercord.kit.ember_fern.pick']='Dry fern fronds pluck'
def write(g):
 g.save(skin(),g.ASSETS/'textures/entity/cinder_bailiff.png');variants={}
 for age in range(3):
  for cooled in [False,True]:
   name=f'cinder_fern_{age}_{"cool" if cooled else "hot"}';g.save(fern(age,cooled),g.ASSETS/f'textures/block/{name}.png');g.write_json(g.ASSETS/f'models/block/{name}.json',{'parent':'minecraft:block/cross','textures':{'cross':f'wildercord:block/{name}'}});variants[f'age={age},cooled={str(cooled).lower()}']={'model':f'wildercord:block/{name}'}
 g.write_json(g.ASSETS/'blockstates/cinder_fern.json',{'variants':variants})
 g.write_json(g.ASSETS/'models/item/cinder_fern.json',{'parent':'minecraft:item/generated','textures':{'layer0':'wildercord:block/cinder_fern_2_cool'}})
 for name in ['cinder_fern','cinder_bailiff_spawn_egg']:
  if name.endswith('spawn_egg'):
   from PIL import Image,ImageDraw
   egg=Image.new('RGBA',(16,16));ed=ImageDraw.Draw(egg);ed.ellipse((3,1,12,14),fill='#B88154',outline='#3D2C27');ed.arc((5,3,10,8),0,180,fill='#EAC28A',width=2);ed.line((5,10,10,10),fill='#4D3930',width=2);ed.point((4,8),fill='#DFAC6C');g.save(egg,g.ASSETS/'textures/item/cinder_bailiff_spawn_egg.png')
   g.write_json(g.ASSETS/f'models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'wildercord:item/{name}'}})
  g.write_json(g.ASSETS/f'items/{name}.json',{'model':{'type':'minecraft:model','model':f'wildercord:item/{name}'}})
 g.write_json(g.DATA/'loot_table/entities/cinder_bailiff.json',{'type':'minecraft:entity','pools':[]})
 g.write_json(g.DATA/'loot_table/blocks/cinder_fern.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'wildercord:cinder_fern'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
 g.write_json(g.DATA/'worldgen/feature/cinder_fern_patch.json',{'type':'wildercord:cinder_fern_patch'})
 g.write_json(g.DATA/'worldgen/placed_feature/cinder_fern_patch.json',{'feature':'wildercord:cinder_fern_patch','placement':[{'type':'minecraft:rarity_filter','chance':8},{'type':'minecraft:in_square'},{'type':'minecraft:heightmap','heightmap':'MOTION_BLOCKING_NO_LEAVES'},{'type':'minecraft:biome'}]})

 # Ordinary crafting supplies an existing-world route without replacing explored chunks.
 g.write_json(g.DATA/'recipe/cinder_fern.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':['minecraft:fern','minecraft:clay_ball','minecraft:charcoal'],'result':{'id':'wildercord:cinder_fern','count':1}})
 g.write_json(g.DATA/'advancement/recipes/cinder_fern.json',{'criteria':{'has_clay':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':'minecraft:clay_ball'}]}}},'requirements':[['has_clay']],'rewards':{'recipes':['wildercord:cinder_fern']}})
