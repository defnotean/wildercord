"""Stonehorn's layered rock/moss and Galeclaw's banded feathers, hooked beak and useful field tools."""
from PIL import Image,ImageDraw
LANG={
 'entity.wildercord.stonehorn':'Stonehorn','entity.wildercord.galeclaw':'Galeclaw',
 'item.wildercord.stonehorn_spawn_egg':'Stonehorn Spawn Egg','item.wildercord.galeclaw_spawn_egg':'Galeclaw Spawn Egg',
 'guide.wildercord.stonehorn':'A highland grazer armoured in living rock. Approach quietly or offer wheat. A warning stamp precedes a straight charge: step sideways and punish its recovery. Spells meet its plates; blades cut normally. Wheat lets a peaceful animal shed one useful plate every two minutes, without harming its grazing ground.',
 'guide.wildercord.stonehorn.hint':'Look on dry, open slopes above sea level in meadows, windswept hills and stony peaks.',
 'guide.wildercord.galeclaw':'A ridge runner that follows fresh spellcasting and hunts rimehares. Its crouch marks a fixed landing in dust; move away before its leap. Spells are strongest during its long recovery. Offer raw rabbit or chicken before provoking it to collect a plume. A Ridge Whistle distracts stalking animals, but cannot stop committed attacks or revenge.',
 'guide.wildercord.galeclaw.hint':'Windswept ridges and frozen or jagged peaks shelter these feathered scavengers. Recent spellcasting draws their attention.',
 'message.wildercord.aura_beast.rest':'Give this animal time to settle: it sheds again after two minutes.',
}
for name,title,lore,use in [
 ('stonehorn_plate','Stonehorn Plate','A weathered plate, shed willingly from a mossy shoulder.','Craft a Bastion Poultice with clay and wheat.'),
 ('galeclaw_plume','Galeclaw Plume','Banded ridge feathers, caught as a runner settles.','Bind a Ridge Whistle with copper and string.'),
 ('bastion_poultice','Bastion Poultice','Stone grit and field herbs steady the body.','Use: Resistance I and Slowness I for 10 seconds; 30-second cooldown. Consumed.'),
 ('ridge_whistle','Ridge Whistle','A feather-bound copper call for the high passes.','Use: distract nearby stalking Galeclaws for 10 seconds. Cannot cancel attacks or revenge. 10-second cooldown.'),
]:
 LANG['item.wildercord.'+name]=title;LANG['item.wildercord.'+name+'.lore']=lore;LANG['item.wildercord.'+name+'.use']=use
for beast,actions in {'stonehorn':{'warn':'Stonehorn stamps: step sideways','charge':'Stonehorn charges','impact':'Stonehorn strikes stone','forage':'Stonehorn grazes'},'galeclaw':{'warn':'Galeclaw crouches: leave its marked landing','leap':'Galeclaw leaps','land':'Galeclaw settles','whistle':'Ridge whistle calls'}}.items():
 for action,caption in actions.items():LANG['subtitles.wildercord.kit.'+beast+'.'+action]=caption
for beast,actions in {'stonehorn':{'grumble':'Stonehorn grumbles','hurt':'Stonehorn plates crack','death':'Stonehorn falls'},'galeclaw':{'call':'Galeclaw calls across the ridge','hurt':'Galeclaw cries','death':'Galeclaw falls'}}.items():
 for action,caption in actions.items():LANG['subtitles.wildercord.kit.'+beast+'.'+action]=caption

def write(g):
 for kind in ('stonehorn','galeclaw'):
  im=Image.new('RGBA',(128,128));d=ImageDraw.Draw(im)
  base=(100,105,92) if kind=='stonehorn' else (69,89,104)
  for x in range(128):
   for y in range(128):
    v=(x*17+y*31+x*y)%19;im.putpixel((x,y),tuple(c+v for c in base)+(255,))
  if kind=='stonehorn':
   for x,y,w,h in [(0,0,67,32),(0,36,55,21),(0,60,34,18)]:
    d.rectangle((x,y,x+w,y+h),outline='#4d564e');d.line((x+1,y+1,x+w-1,y+1),fill='#cad0b2')
    for n in range(5):
     px=x+4+n*7;d.line([(px,y+3),(px+2,y+7),(px-1,y+12),(px+3,y+16)],fill='#59624f')
   d.rectangle((72,0,94,30),fill='#496142')
   for y in range(2,30,3):d.line((72,y,94,y+1),fill='#78925b');d.point((76,y),fill='#b4bd76')
   d.rectangle((96,0,111,30),fill='#a69b78')
   for y in range(2,29,3):d.line((96,y,111,y),fill='#665e45');d.line((96,y+1,111,y+1),fill='#d8cbaa')
   d.rectangle((96,32,110,42),fill='#e3dcc0');d.rectangle((96,48,116,58),fill='#363e39')
   for x in range(98,116,5):d.line((x,50,x,58),fill='#7b8070')
   d.rectangle((42,60,63,70),fill='#5a6156')
   for x,y in [(10,70),(14,70),(5,71),(20,71)]:d.rectangle((x,y,x+1,y+1),fill='#131b19');d.point((x,y),fill='#d9debd')
  else:
   for x0,y0,w,h in [(0,0,43,24),(0,32,21,14),(0,52,25,13),(48,0,27,16),(64,32,15,24),(48,64,23,11)]:
    for y in range(y0,y0+h,3):
     d.line((x0,y,x0+w,y),fill='#bdc7c2' if y%6==0 else '#425b6d')
     for x in range(x0+2,x0+w,5):d.line((x,y,x+2,y+2),fill='#7f9cac')
   d.rectangle((96,0,112,10),fill='#ae9659');d.line((96,1,112,1),fill='#e3cb7b')
   d.rectangle((96,32,105,42),fill='#8b7953');d.rectangle((96,48,112,54),fill='#303943')
   for x,y in [(8,60),(11,60),(3,60),(16,60)]:d.rectangle((x,y,x+1,y+1),fill='#e1b355');d.point((x,y+1),fill='#152430')
  g.save(im,g.ASSETS/f'textures/entity/{kind}.png')
 for name in ('stonehorn_plate','galeclaw_plume','bastion_poultice','ridge_whistle'):
  im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
  if name=='stonehorn_plate':
   d.polygon([(4,11),(11,4),(24,5),(29,16),(23,27),(9,29),(3,21)],fill='#414b42');d.polygon([(6,11),(12,6),(23,7),(27,16),(21,25),(10,27),(5,20)],fill='#8a9680')
   d.line([(7,12),(13,9),(23,10)],fill='#d2d1ae',width=2);d.line([(17,7),(15,14),(20,20),(17,26)],fill='#4b574d',width=2)
   for x,y in [(7,21),(9,23),(23,15),(25,18)]:d.rectangle((x,y,x+3,y+2),fill='#647845')
  elif name=='galeclaw_plume':
   d.polygon([(6,25),(7,13),(19,3),(26,4),(27,11),(16,23)],fill='#2f4453');d.polygon([(8,23),(9,14),(20,5),(25,5),(25,11),(15,21)],fill='#9cb9c3')
   for i in range(5):d.line((10+i*3,19-i*3,17+i*2,18-i*3),fill='#537488',width=2)
   d.line((5,29,25,6),fill='#e4dbaf',width=2)
  elif name=='bastion_poultice':
   d.polygon([(5,10),(9,5),(25,6),(28,13),(25,27),(7,28),(3,19)],fill='#3f4b36');d.rectangle((7,9,24,25),fill='#a29972');d.rectangle((9,12,22,23),fill='#687b50')
   for x,y in [(10,14),(17,16),(12,20),(20,21)]:d.rectangle((x,y,x+3,y+2),fill='#c1c3a6')
   d.line((5,10,26,24),fill='#d3b573',width=2);d.line((24,8,7,26),fill='#d3b573',width=2)
  else:
   d.polygon([(5,23),(22,5),(28,9),(11,28)],fill='#553b2c');d.polygon([(7,23),(23,7),(26,9),(11,25)],fill='#c58b57')
   d.line((8,22,23,7),fill='#efc393',width=2);d.rectangle((17,12,20,15),fill='#2e3335')
   d.line((9,25,5,29),fill='#ead4a0');d.polygon([(5,28),(2,22),(4,17),(8,22)],fill='#9cb9c3');d.line((4,20,6,25),fill='#4c6a7c')
  g.save(im,g.ASSETS/f'textures/item/{name}.png');g.write_json(g.ASSETS/f'models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'wildercord:item/{name}'}});g.write_json(g.ASSETS/f'items/{name}.json',{'model':{'type':'minecraft:model','model':f'wildercord:item/{name}'}})
 for name in ('stonehorn','galeclaw'):
  im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im);d.ellipse((6,3,25,29),fill='#82917a' if name=='stonehorn' else '#7b9aaf',outline='#344743',width=2)
  for x,y in [(10,8),(18,6),(19,17),(9,21),(14,25)]:d.ellipse((x,y,x+4,y+3),fill='#c5c5a3' if name=='stonehorn' else '#d6ba78')
  g.save(im,g.ASSETS/f'textures/item/{name}_spawn_egg.png')
  g.write_json(g.ASSETS/f'models/item/{name}_spawn_egg.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'wildercord:item/{name}_spawn_egg'}})
  g.write_json(g.ASSETS/f'items/{name}_spawn_egg.json',{'model':{'type':'minecraft:model','model':f'wildercord:item/{name}_spawn_egg'}})
  # A deliberate combat alternative to peaceful feeding; no experience/rare-drop farming loop is introduced.
  g.write_json(g.DATA/f'loot_table/entities/{name}.json',{'type':'minecraft:entity','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'wildercord:'+('stonehorn_plate' if name=='stonehorn' else 'galeclaw_plume')}]}]})
 g.write_json(g.DATA/'tags/block/aura_beast_ground.json',{'replace':False,'values':['minecraft:grass_block','minecraft:stone','minecraft:gravel','minecraft:snow_block','minecraft:calcite']})
 for name,ingredients in [('bastion_poultice',['wildercord:stonehorn_plate','minecraft:clay_ball','minecraft:wheat']),('ridge_whistle',['wildercord:galeclaw_plume','minecraft:copper_ingot','minecraft:string'])]:
  g.write_json(g.DATA/f'recipe/{name}.json',{'type':'minecraft:crafting_shapeless','category':'equipment','ingredients':ingredients,'result':{'id':'wildercord:'+name}})
  g.unlock_advancement('wildercord:'+name,ingredients[0])
