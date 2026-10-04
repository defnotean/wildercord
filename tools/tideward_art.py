"""Original leather/reed footwear, mended smoked lenses, and a forked crossing spool."""
from PIL import Image,ImageDraw
from world_art import noise,Sheet,box
from wildlife_art import shade

LANG={
 'item.wildercord.reedwater_waders':'Reedwater Waders',
 'item.wildercord.reedwater_waders.lore':'Turned cuffs remember the banks, not the depths.',
 'item.wildercord.reedwater_waders.use':'Feet slot. Walk on real shallow-water support without sprinting for a modest water movement aid. One extra wear per second of actual movement; five walking seconds require two seconds of shared rest. No breathing, damage or root immunity.',
 'item.wildercord.dewglass_spectacles':'Dewglass Spectacles',
 'item.wildercord.dewglass_spectacles.lore':'Tamsin repaired the left hinge with reed wire.',
 'item.wildercord.dewglass_spectacles.use':'Head slot, no armour. Crouch and look at a visible Reedback warning within eight blocks for half a second. Physical reed notches identify raised claws. One wear and three seconds of shared rest. Walls, hidden animals and committed sweeps refuse.',
 'item.wildercord.bank_surveyors_line':"Bank Surveyor's Line",
 'item.wildercord.bank_surveyors_line.lore':'The spool measures a crossing; it does not make one.',
 'item.wildercord.bank_surveyors_line.use':'Use two visible supported banks within five seconds, up to eight blocks apart and one step high. Surveys a short direct shallow-water or dry route. Two wear, four-second shared rest, five-second private braid. Sneak-use cancels. Protected, deep, obstructed or unloaded cells refuse.',
 **{'message.wildercord.bank_line.'+k:v for k,v in {'rest':'The line needs a moment to settle.','bank':'Choose a nearby visible solid bank with two clear body blocks.','first':'First peg observed. Choose the other bank within five seconds.','crossing':'No clear supported crossing: check depth, clearance, protection and distance.'}.items()},
 **{'subtitles.wildercord.kit.tideward.'+k:v for k,v in {'weave_rest':'Reed cuffs draw tight','glass_warning':'Mended glass hinge ticks','spool_commit':'Braided line unwinds'}.items()}
}

def waders_icon():
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
 # Offset silhouettes: left upright cuff, right folded mouth and angled toe.
 for x in (3,17):
  d.polygon([(x,4),(x+10,4),(x+10,19),(x+12,22),(x+12,28),(x-1,28),(x-1,22),(x,19)],fill='#312D26')
  d.rectangle((x+1,7,x+8,20),fill='#796047');d.polygon([(x,22),(x+7,21),(x+10,24),(x+10,26),(x,26)],fill='#A88B61')
  d.rectangle((x,5,x+10,10),fill='#A3AA74')
  for yy in (6,8,10):
   for xx in range(x,x+10,2):d.line((xx,yy,xx+1,yy+1),fill='#E2D5A6' if xx%4 else '#647559')
  d.line((x+1,11,x+1,20),fill='#D4BC8C');d.line((x,27,x+11,27),fill='#554B35')
  for yy in (13,16,19):d.point((x+8,yy),fill='#E6D6AF')
 d.polygon([(17,4),(25,3),(27,7),(19,8)],fill='#D4CC96');d.line((19,6,25,5),fill='#69795E')
 d.line([(6,14),(9,15),(7,17)],fill='#C5AD77');d.point((8,15),fill='#806443')
 return im

def spectacles_icon():
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
 d.line([(4,13),(1,5),(6,3)],fill='#4B3F31',width=3);d.line([(28,13),(30,7),(27,4)],fill='#8D7144',width=3)
 d.ellipse((2,11,14,24),fill='#392F28',outline='#DBBE7C',width=2)
 d.ellipse((18,10,30,23),fill='#342F2B',outline='#B8995F',width=2)
 d.ellipse((5,14,11,21),fill='#566D6B');d.ellipse((21,13,27,20),fill='#43565B')
 d.line([(13,16),(15,13),(18,15)],fill='#E0C78B',width=2)
 d.line((6,15,8,14),fill='#B7C9B7');d.line((23,14,25,13),fill='#AEC1B5')
 d.line([(3,12),(6,10),(8,12),(5,14)],fill='#C9D1A0');d.point((7,11),fill='#6D8066')
 d.point((28,18),fill='#E7D09A');d.line((19,22,24,23),fill='#6A5539')
 return im

def spool_icon():
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
 d.polygon([(7,3),(12,3),(13,8),(21,8),(22,3),(26,4),(25,13),(20,16),(20,25),(15,28),(10,25),(10,16),(6,12)],fill='#3E342A')
 d.polygon([(8,5),(11,5),(12,11),(22,11),(23,5),(25,6),(23,13),(18,15),(18,24),(15,26),(12,24),(12,15),(8,11)],fill='#AC8854')
 for yy in range(14,24,2):
  d.line((10,yy,20,yy+1),fill='#D9CEA0',width=2);d.line((12,yy+1,20,yy+1),fill='#75855C')
 d.line([(21,21),(26,20),(28,26),(25,27)],fill='#C5BA83');d.line([(28,24),(30,23),(30,27),(27,29)],fill='#C0A464',width=2)
 d.line((8,5,9,10),fill='#EAD5A0');d.point((15,24),fill='#4D4F36')
 return im

def armour(kind):
 s=Sheet(64,32)
 if kind=='reedwater':
  for u in (0,40):
   for face,(xx,yy,w,h) in box(u,16,4,12,4).items():
    for y in range(h):
     for x in range(w):
      c=(100,82,58) if y>3 else (139,151,102)
      if y<4 and (x+y)%3==0:c=(216,210,157)
      if y==h-1:c=(43,41,31)
      if y>4 and x==0:c=(177,145,94)
      s.put(xx+x,yy+y,(*shade(c,.83+noise(xx+x,yy+y,7184)*.2),255))
 else:
  # Transparent native helmet UV: physical rims/temples only, no opaque face box.
  front=box(0,0,8,8,8)['front'];xx,yy,w,h=front
  for x in range(8):
   for y in range(8):
    rim=(y in (3,5) and x in (0,1,2,5,6,7)) or (y==4 and x in (0,2,3,4,5,7))
    lens=y==4 and x in (1,6)
    if rim:s.put(xx+x,yy+y,(191,161,99,255))
    elif lens:s.put(xx+x,yy+y,(80,107,108,115))
  for face in ('left','right'):
   xx,yy,w,h=box(0,0,8,8,8)[face]
   for x in range(6):s.put(xx+x,yy+4,(121,98,61,255))
 return s.image()

def particle(name):
 im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
 if name=='notch':
  d.polygon([(3,14),(4,3),(8,1),(11,5),(10,13),(8,15)],fill='#53644B')
  d.polygon([(4,12),(5,4),(8,2),(8,14)],fill='#D8D2A2');d.line((6,5,6,11),fill='#7D8A5E')
  d.line((8,6,10,7),fill='#9CAC77');d.point((5,4),fill='#EFE6B8')
 elif name=='braid':
  for x in range(2,14):
   y=7+(x%4//2);d.point((x,y),fill='#D5CA99');d.point((x,y+1),fill='#718258');d.point((x,y-1),fill='#9EAB76')
 else:
  d.polygon([(6,2),(10,2),(11,5),(9,7),(9,13),(7,15),(6,12)],fill='#594D35')
  d.polygon([(7,3),(9,3),(9,6),(8,8),(8,13),(7,12)],fill='#DFCA8F');d.line((5,6,11,6),fill='#A59461')
 return im

def spool_material(name):
 im=Image.new('RGBA',(16,16));px=im.load()
 for y in range(16):
  for x in range(16):
   if name=='wood':c=(156,119,72) if x%5 else (89,69,43)
   elif name=='thread':c=(216,202,152) if (x+y)%4<2 else (108,130,84)
   else:c=(181,148,87) if (x*3+y)%7 else (83,116,84)
   px[x,y]=(*shade(c,.85+noise(x,y,7197)*.18),255)
 return im

def held_spool():
 # Three wooden fork bars, bound spool waist, short closing peg. Separate material faces.
 def element(a,b,material):return {'from':a,'to':b,'faces':{f:{'uv':[0,0,16,16],'texture':'#'+material} for f in ('north','south','east','west','up','down')}}
 return {'textures':{'particle':'wildercord:item/tideward_spool_wood',**{n:'wildercord:item/tideward_spool_'+n for n in ('wood','thread','brass')}},
  'elements':[element([5,3,7],[7,14,9],'wood'),element([10,3,7],[12,14,9],'wood'),element([7,5,7],[10,7,9],'wood'),
   element([4.7,7,6.7],[12.3,10.8,9.3],'thread'),element([11.8,11,7],[13,12,9],'brass'),element([6,2,7],[11,3.5,9],'brass')],
  'display':{'firstperson_righthand':{'rotation':[0,-90,25],'translation':[1,1,-2],'scale':[.7,.7,.7]},
   'firstperson_lefthand':{'rotation':[0,90,-25],'translation':[1,1,-2],'scale':[.7,.7,.7]},
   'thirdperson_righthand':{'rotation':[0,90,0],'translation':[0,2,0],'scale':[.65,.65,.65]},
   'thirdperson_lefthand':{'rotation':[0,-90,0],'translation':[0,2,0],'scale':[.65,.65,.65]}}}

def write(g):
 for name,draw in [('reedwater_waders',waders_icon),('dewglass_spectacles',spectacles_icon),('bank_surveyors_line',spool_icon)]:
  g.save(draw(),g.ASSETS/f'textures/item/{name}.png');g.item_model(name,name)
  g.write_json(g.ASSETS/f'items/{name}.json',{'model':{'type':'minecraft:model','model':f'wildercord:item/{name}'}})
 for name in ('reedwater','dewglass'):
  g.save(armour(name),g.ASSETS/f'textures/entity/equipment/humanoid/{name}.png')
  g.write_json(g.ASSETS/f'equipment/{name}.json',{'layers':{'humanoid':[{'texture':f'wildercord:{name}'}]}})
 for name in ('notch','braid','peg'):g.save(particle(name),g.ASSETS/f'textures/particle/tideward_{name}.png')
 for name in ('wood','thread','brass'):g.save(spool_material(name),g.ASSETS/f'textures/item/tideward_spool_{name}.png')
 g.write_json(g.ASSETS/'models/item/bank_surveyors_line_held.json',held_spool())
 # Actual26.3 spyglass descriptor verifies these display contexts and separate held model.
 g.write_json(g.ASSETS/'items/bank_surveyors_line.json',{'model':{'type':'minecraft:select','property':'minecraft:display_context',
  'cases':[{'when':['gui','ground','fixed','on_shelf'],'model':{'type':'minecraft:model','model':'wildercord:item/bank_surveyors_line'}}],
  'fallback':{'type':'minecraft:model','model':'wildercord:item/bank_surveyors_line_held'}}})
 g.write_json(g.DATA/'tags/item/repairs_reedwater.json',{'replace':False,'values':['wildercord:moonreed_floss']})
 g.write_json(g.DATA/'tags/item/repairs_dewglass.json',{'replace':False,'values':['minecraft:glass_pane']})
 import json
 for tag,item in [('foot_armor','reedwater_waders'),('head_armor','dewglass_spectacles')]:
  target=g.RES/f'data/minecraft/tags/item/{tag}.json';prior=json.loads(target.read_text(encoding='utf-8')) if target.exists() else {'replace':False,'values':[]}
  prior['values']=sorted(set(prior['values']+[f'wildercord:{item}']));g.write_json(target,prior)
 recipes={
  'reedwater_waders':['minecraft:leather_boots','wildercord:moonreed_floss','wildercord:windreed_braid','minecraft:copper_ingot'],
  'dewglass_spectacles':['minecraft:glass_pane','minecraft:copper_ingot','wildercord:moonreed_floss'],
  'bank_surveyors_line':['minecraft:stick','minecraft:copper_ingot','wildercord:windreed_braid','wildercord:moonreed_floss']}
 for name,inputs in recipes.items():
  g.write_json(g.DATA/f'recipe/{name}.json',{'type':'minecraft:crafting_shapeless','category':'equipment','ingredients':inputs,'result':{'id':f'wildercord:{name}','count':1}})
  g.unlock_advancement(f'wildercord:{name}','wildercord:moonreed_floss')
