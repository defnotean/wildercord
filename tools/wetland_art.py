"""Lantern Newt anatomy, reed-caged Marshlight, pearl and Tideward journal; exact model UVs."""
import math
from PIL import Image, ImageDraw
from wildlife_art import Skin, mix, shade
from world_art import noise

LANG={
 'entity.wildercord.lantern_newt':'Lantern Newt',
 'item.wildercord.lantern_newt_spawn_egg':'Lantern Newt Spawn Egg',
 'item.wildercord.dusk_pearl':'Dusk Pearl',
 'item.wildercord.dusk_pearl.lore':'A bead of living light, offered by a patient river dweller.',
 'item.wildercord.dusk_pearl.use':'Offer Seagrass to a wet, undisturbed Lantern Newt. One pearl; its saved gathering rest lasts two minutes. Crafts Marshlights or Tideward field notes.',
 'block.wildercord.marshlight':'Marshlight',
 'item.wildercord.tideward_notes':'Lights Along the Bank',
 'message.wildercord.lantern_newt.water':'Her gills need water or rain before she can offer a pearl.',
 'message.wildercord.lantern_newt.rest':'The little lantern is gathering its light. Let her rest.',
 'guide.wildercord.lantern_newt':'A small amphibian of open swamp shallows. It visits seagrass without consuming the plants; its nodules brighten at night, in rain and after Tidebreath or Life magic. Offer seagrass while it is wet and undisturbed for one Dusk Pearl, with a saved two-minute rest. Hurt animals flee and refuse feeding briefly. Pearls weave into waterloggable Marshlights, or bind Tideward field notes. It cannot be bred and gives no pearl when killed.',
 'guide.wildercord.lantern_newt.hint':'Watch open, shallow water in swamps and mangrove swamps. At night, look for three small lights along a paddle tail.',
 'book.wildercord.tideward.1':'LIGHTS ALONG THE BANK\n\nIona, Tideward surveyor\n\nWe mark safe banks with reed cages. Their light comes from a gift, never a knife. The small newts browse living seagrass and carry the wetland night on their backs.',
 'book.wildercord.tideward.2':'Keep the gills wet. Offer one seagrass and wait two minutes before gathering again. A hurt newt flees and refuses your hand. Tidebreath and Life make its lights answer, but that answer offers no second pearl.',
 'book.wildercord.tideward.3':'Bind pearl, glass, string and seagrass into a Marshlight. It shines below water as well as above. Moths find such lamps, and travellers find the bank. Leave the plants standing. A living shore is worth more than a bag of beads.',
 **{'subtitles.wildercord.kit.wetland.'+n:t for n,t in [('call','Lantern Newt trills'),('hurt','Lantern Newt squeaks'),('death','Lantern Newt falls silent'),('answer','Newt lanterns answer'),('pearl','Dusk Pearl drops')]},
}

def skin(glow=False):
 s=Skin(128,32)
 def coat(face,x,y,z,tx,ty):
  if glow:return None
  c=mix((52,93,79),(170,199,143),min(1,y/3))
  if face=='bottom':c=(190,207,154)
  if face=='top' and (int(x*2)+int(z*3))%9 in (0,1):c=(200,183,97)
  return shade(c,.92+noise(tx,ty,411)*.16)
 for u,v,w,h,d in [(0,0,6,3,9),(32,0,7,3,5),(0,16,4,2,6),(22,16,2,2,6),(42,16,2,1,3)]:s.box(u,v,w,h,d,coat)
 s.box(54,16,1,3,1,lambda f,x,y,z,tx,ty: (169,232,185,180) if glow and y<1 else None if glow else mix((162,223,184),(80,133,109),y/3))
 s.box(60,16,2,2,2,lambda f,x,y,z,tx,ty: (222,247,162,240) if glow else mix((245,242,168),(105,175,115),y/2),tone=not glow)
 s.box(70,16,1,1,1,lambda f,x,y,z,tx,ty: None if glow else (23,41,39))
 if not glow:
  for x in (1,5):s.at(32,0,7,3,5,'front',x,1,(28,46,41));s.at(32,0,7,3,5,'front',x,0,(183,203,153))
  s.at(70,16,1,1,1,'top',0,0,(221,241,221))
  for x in (2,3,4):s.at(32,0,7,3,5,'front',x,2,(94,117,83))
 return s.image()

def write(g):
 g.write_json(g.DATA/'tags/item/fox_taming_fish.json', {'replace':False,'values':['minecraft:cod','minecraft:salmon','minecraft:cooked_cod','minecraft:cooked_salmon','minecraft:tropical_fish']})
 for glow in (False,True):g.save(skin(glow),g.ASSETS/f'textures/entity/wildlife/lantern_newt{"_glow" if glow else ""}.png')
 for name in ('dusk_pearl','marshlight','tideward_notes','lantern_newt_spawn_egg'):
  im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
  if name=='dusk_pearl':
   d.ellipse((5,7,27,28),fill='#253E36');d.ellipse((6,7,26,26),fill='#537B59');d.ellipse((7,7,24,23),fill='#B2C982');d.ellipse((8,8,21,20),fill='#DAE8AB');d.ellipse((9,8,17,15),fill='#FAF3CD');d.line([(12,10),(11,15),(15,19),(19,17)],fill='#7FA575',width=2);d.point((20,12),fill='#FFFFFF');d.line((10,28,22,27),fill='#719370')
  elif name=='marshlight':
   d.polygon([(7,8),(16,4),(25,8),(25,24),(16,28),(7,24)],fill='#344B3C');d.ellipse((10,10,22,23),fill='#A6C978');d.ellipse((11,10,20,19),fill='#EFF0AF')
   for x in (8,13,20,24):d.line((x,9,x,25),fill='#816D42',width=2);d.line((x+1,10,x+1,24),fill='#CEB780')
   d.line([(7,9),(16,5),(25,9)],fill='#DDD0A1',width=2);d.line([(7,24),(16,28),(25,24)],fill='#A89766',width=3);d.line((8,16,24,16),fill='#997D4A')
  elif name=='tideward_notes':
   d.polygon([(5,8),(22,4),(28,8),(28,26),(11,30),(5,26)],fill='#263E3D');d.polygon([(9,9),(24,6),(27,9),(27,25),(11,28)],fill='#93AC85');d.polygon([(11,10),(25,8),(25,24),(12,26)],fill='#456E64');d.line((8,10,8,25),fill='#D8CBA0',width=2);d.ellipse((15,13,22,21),fill='#D5DCA0');d.line([(14,24),(18,21),(22,23)],fill='#BBCEAE');d.line((11,28,25,25),fill='#ECE4C3')
  else:
   d.ellipse((7,3,25,29),fill='#314B42');d.ellipse((8,4,23,26),fill='#84AC81');
   for x,y in [(12,8),(18,10),(11,17),(20,21)]:d.ellipse((x-2,y-2,x+2,y+2),fill='#E4E7AB');d.point((x,y-1),fill='#FFF4D2')
  g.save(im,g.ASSETS/f'textures/item/{name}.png');g.write_json(g.ASSETS/f'models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'wildercord:item/{name}'}});g.write_json(g.ASSETS/f'items/{name}.json',{'model':{'type':'minecraft:model','model':f'wildercord:item/{name}'}})
 # Two materials: a shaded living pearl and hand-woven reed ribs.
 for name in ('marshlight_pearl','marshlight_reed'):
  im=Image.new('RGBA',(16,16));px=im.load()
  for y in range(16):
   for x in range(16):
    if name.endswith('pearl'):
     radial=min(1,math.hypot(x-6,y-5)/14);c=mix((244,240,167),(104,158,98),radial);c=shade(c,.94+noise(x,y,515)*.1)
    else:c=shade((151,123,72),.65+noise(x,y,213)*.2+(0.35 if x%4==1 else 0));c=(206,179,118) if y%5==0 else c
    px[x,y]=(*c,255)
  g.save(im,g.ASSETS/f'textures/block/{name}.png')
 elements=[]
 def box(a,b,tex):elements.append({'from':a,'to':b,'faces':{f:{'texture':'#'+tex} for f in ('up','down','north','south','east','west')}})
 box([3,0,3],[13,2,13],'reed');box([4,12,4],[12,14,12],'reed');box([5,3,5],[11,11,11],'pearl');box([4,5,5],[12,9,11],'pearl')
 for x in (3,11):
  for z in (3,11):box([x,2,z],[x+2,12,z+2],'reed')
 for y in (4,9):
  for z in (3,12):box([3,y,z],[13,y+1,z+1],'reed')
 g.write_json(g.ASSETS/'models/block/marshlight.json',{'parent':'minecraft:block/block','textures':{'reed':'wildercord:block/marshlight_reed','pearl':'wildercord:block/marshlight_pearl','particle':'wildercord:block/marshlight_reed'},'elements':elements})
 g.write_json(g.ASSETS/'blockstates/marshlight.json',{'variants':{'':{'model':'wildercord:block/marshlight'}}})
 g.write_json(g.DATA/'loot_table/blocks/marshlight.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'wildercord:marshlight'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
 g.write_json(g.DATA/'loot_table/entities/lantern_newt.json',{'type':'minecraft:entity','pools':[]})
 for name,ingredients in [('marshlight',['wildercord:dusk_pearl','minecraft:glass','minecraft:string','minecraft:seagrass']),('tideward_notes',['wildercord:dusk_pearl','minecraft:book'])]:
  g.write_json(g.DATA/f'recipe/{name}.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':ingredients,'result':{'id':'wildercord:'+name}});g.unlock_advancement('wildercord:'+name,'wildercord:dusk_pearl')
