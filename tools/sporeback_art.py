"""Patient cave spiral: pale ridged limestone, lilac foot, spotted whorls and dew crown."""
import math
from PIL import Image,ImageDraw
from wildlife_art import Skin,mix,shade
from world_art import noise
LANG={
 'entity.wildercord.sporeback_snail':'Sporeback Snail','item.wildercord.sporeback_snail_spawn_egg':'Sporeback Snail Spawn Egg',
 'item.wildercord.mycelial_dew':'Mycelial Dew','item.wildercord.mycelial_dew.lore':'A single cool bead, gathered after a patient fungal visit.','item.wildercord.mycelial_dew.use':'Combine with paper and a brown mushroom for Fungal Poultice.',
 'item.wildercord.fungal_poultice':'Fungal Poultice','item.wildercord.fungal_poultice.lore':'Belowkeepers call its pale glow borrowed sight.','item.wildercord.fungal_poultice.use':'Use for 30 seconds of Night Vision; Slowness for the first 6 seconds. Cannot refresh active Night Vision.',
 'item.wildercord.sporeback_journal':'The Patient Spiral',
 'message.wildercord.sporeback.quiet':'Crouch with an empty hand to gather a ready dew bead.',
 'message.wildercord.sporeback.wait':'The spiral has no ready dew. Let it browse living fungi and recover in peace.',
 'message.wildercord.sporeback.poultice_rest':'Your sight is already clear; the poultice is kept.',
 'guide.wildercord.sporeback_snail':'A peaceful cave browser with an offset spiral shell. Find it on moss or clay beneath Y48 in lush or dripstone caves. It visits mushrooms without eating the block, then holds one dew bead. Crouch empty-handed to gather; each snail rests two minutes between gatherings and must browse again. Fire and physical harm retract its antennae and prevent gathering. Life produces a brief spore answer without minting dew. Brightness sends it toward nearby cover. No breeding or unique death loot.',
 'guide.wildercord.sporeback_snail.hint':'Below the moss banks, watch pale spiral shells beside mushrooms. Their antennae move slowly.',
 'book.wildercord.sporeback.1':'Mara, Belowkeeper\n\nI thought the pale spiral was another stone. Then its eyes opened. It followed the damp bank to a brown mushroom and rested its mouth against the cap. The mushroom stayed. A cool bead appeared above the shell.',
 'book.wildercord.sporeback.2':'Borrowed sight\n\nWe gather kneeling, with bare hands. A frightened spiral closes its doors. No fire, no knocking. Even a second living chant cannot hurry the bead. Leave the mushroom and the snail both standing; return after its rest.',
 'book.wildercord.sporeback.3':'The first stairs\n\nFold dew into paper with brown mushroom pulp. Darkness parts for half a minute, but the first steps feel heavy. Carry a torch as well. The Belowkeepers never mistake a glimpse for a map; deeper doors still have to be found.',
 **{'subtitles.wildercord.kit.sporeback.'+n:t for n,t in [('call','Sporeback exhales'),('browse','Sporeback browses'),('hide','Spiral closes'),('answer','Spores answer'),('gather','Dew is gathered')]}}
def skin():
 s=Skin(128,64)
 def foot(f,x,y,z,tx,ty):return shade(mix((101,85,123),(169,153,174),max(0,min(1,y/3))),.85+noise(tx,ty,903)*.25)
 def shell(f,x,y,z,tx,ty):
  c=(188,180,158) if int(x+z+y*2)%5 else (126,117,103)
  if f=='top':c=(211,207,173) if int(x+z)%3 else (157,152,125)
  if int(x*3+z*5)%11==0:c=(127,112,153)
  if f in ('left','right'):
   # Archimedean spiral engraved across the broad shell faces, with a lit lip and dark groove.
   dz=z-4;dy=y-4;radius=math.hypot(dz,dy);angle=(math.atan2(dy,dz)+math.pi)%(math.pi*2)
   groove=min(abs(radius-(.45+.35*(angle+turn*math.pi*2))) for turn in range(2))
   if groove<.28:c=(102,85,119)
   elif groove<.58:c=(228,218,188)
  if f=='bottom':c=(118,104,103)
  return shade(c,.9+noise(tx,ty,918)*.2)
 s.box(0,0,8,2,13,foot);s.box(44,0,5,3,5,foot);s.box(66,0,1,4,1,foot);s.box(74,0,2,2,2,lambda f,x,y,z,tx,ty:(39,32,47) if f=='front' else (188,181,196))
 for u,v,w,h,d in [(0,18,8,8,8),(36,18,6,6,6),(64,18,4,4,4)]:s.box(u,v,w,h,d,shell)
 s.box(86,18,2,2,2,lambda f,x,y,z,tx,ty:(186,230,191) if f=='top' else (92,162,137))
 return s.image()
def write(g):
 g.save(skin(),g.ASSETS/'textures/entity/wildlife/sporeback_snail.png')
 for name in ['mycelial_dew','fungal_poultice','sporeback_journal','sporeback_snail_spawn_egg']:
  im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
  if name=='mycelial_dew':
   d.polygon([(16,4),(23,16),(24,22),(20,27),(12,28),(7,24),(7,18)],fill='#38695C');d.polygon([(15,7),(20,16),(21,23),(15,25),(10,23),(10,17)],fill='#9AC8AE');d.line((13,12,11,20),fill='#DAF2CE',width=2)
  elif name=='fungal_poultice':
   d.rounded_rectangle((6,7,25,26),radius=3,fill='#71634F');d.rounded_rectangle((7,7,24,23),radius=2,fill='#D9CEAC');d.line((9,8,9,22),fill='#F4E9C8');d.rectangle((11,11,21,20),fill='#899B72');d.ellipse((12,12,19,18),fill='#AFCD9C');d.line((6,18,25,13),fill='#B69C70',width=2)
  elif name=='sporeback_journal':
   d.polygon([(6,6),(23,4),(26,23),(10,28)],fill='#4A3B52');d.polygon([(8,7),(22,6),(24,22),(11,25)],fill='#988997');d.arc((11,11,21,21),0,320,fill='#E7DDB4',width=2);d.line((9,8,12,23),fill='#D2BC8B',width=2)
  else:
   d.ellipse((7,3,25,29),fill='#73637C');d.ellipse((9,5,23,26),fill='#C8C0AA');d.arc((10,10,23,23),0,310,fill='#74677E',width=3);d.arc((13,13,20,20),-80,200,fill='#9E8BAC',width=2)
  # Hand-authored surfaces: directional material shading, dry paper fibres and wet mineral highlights.
  px=im.load()
  for y in range(32):
   for x in range(32):
    if px[x,y][3]==0:continue
    c=px[x,y][:3];n=noise(x,y,1041 if name=='mycelial_dew' else 1049)
    light=.86+(31-y)*.004+(31-x)*.002+n*.12
    if name=='fungal_poultice':light=.88+n*.2+(31-y)*.002
    px[x,y]=(*shade(c,light),255)
  d=ImageDraw.Draw(im)
  if name=='mycelial_dew':
   # The dark inner meniscus, luminous bend and tiny suspended spores make the bead translucent.
   d.arc((8,13,22,27),10,155,fill='#416F64',width=2)
   d.arc((9,12,21,25),-45,60,fill='#B9E3C0')
   d.line([(13,11),(11,16),(11,19)],fill='#EFFCDF',width=2)
   for x,y,c in [(17,20,'#D9ECC2'),(14,22,'#CBE7B4'),(19,23,'#718E72'),(12,25,'#ACCBA7')]:d.point((x,y),fill=c)
   d.line((13,28,20,27),fill='#244F47');d.point((16,5),fill='#E6F7D3')
  elif name=='fungal_poultice':
   for x,y in [(8,10),(9,13),(22,9),(21,21),(8,20),(18,9)]:d.line((x,y,x+1,y+1),fill='#B5A788')
   d.line([(8,7),(15,7),(16,8),(23,8)],fill='#F0E5C7');d.line((8,23,23,23),fill='#9B8C70')
   # Visible mushroom lamellae under the folded fibre wrapper.
   d.pieslice((12,11,22,18),180,360,fill='#715B4B');d.arc((12,11,22,18),180,360,fill='#BBA086')
   d.rectangle((16,16,17,20),fill='#D1BE9B')
   for x in [13,15,19,21]:d.line((17,16,x,14),fill='#CCAF8C')
   d.line([(6,18),(13,16),(24,13)],fill='#B69462',width=2);d.line([(6,17),(13,15),(24,12)],fill='#E2C894')
   d.ellipse((14,14,18,18),outline='#735B3D');d.line((16,18,18,22),fill='#BCA36E');d.point((12,19),fill='#D3E0A2')
  elif name=='sporeback_journal':
   for y in [10,13,16,19,22]:d.line((7,y,10,y),fill='#C8B17C')
   d.line([(11,26),(24,22),(24,24),(11,28)],fill='#DBCCA3');d.line((11,27,24,23),fill='#947F61')
   d.arc((12,12,21,21),20,290,fill='#E3D8B5');d.arc((15,14,20,19),-90,190,fill='#D6C9A7');d.point((19,17),fill='#493D53')
   for x,y in [(14,8),(20,9),(13,21),(22,20)]:d.point((x,y),fill='#B3A5AD')
  else:
   d.arc((9,5,23,27),190,290,fill='#E7DEC2',width=2);d.arc((10,10,23,23),35,150,fill='#4B3E5B');d.line((13,25,20,25),fill='#9D8D78')
   for x,y in [(13,7),(20,9),(11,22),(20,23),(15,24)]:d.rectangle((x,y,x+1,y+1),fill='#95849D')
  g.save(im,g.ASSETS/f'textures/item/{name}.png');g.write_json(g.ASSETS/f'models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'wildercord:item/{name}'}});g.write_json(g.ASSETS/f'items/{name}.json',{'model':{'type':'minecraft:model','model':f'wildercord:item/{name}'}})
 g.write_json(g.DATA/'recipe/fungal_poultice.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':['wildercord:mycelial_dew','minecraft:paper','minecraft:brown_mushroom'],'result':{'id':'wildercord:fungal_poultice','count':1}})
 g.unlock_advancement('wildercord:fungal_poultice','wildercord:mycelial_dew')
 g.write_json(g.DATA/'loot_table/entities/sporeback_snail.json',{'type':'minecraft:entity','pools':[]})
