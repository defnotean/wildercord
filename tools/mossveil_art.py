"""Original rounded dormouse UV skin, stitched physical cowl and thin floss release sprite."""
from wildlife_art import Skin, shade
from world_art import noise
from PIL import Image, ImageDraw

LANG = {
 'entity.wildercord.mossveil_dormouse': 'Mossveil Dormouse',
 'item.wildercord.mossveil_dormouse_spawn_egg': 'Mossveil Dormouse Spawn Egg',
 'item.wildercord.mossveil_cowl': 'Mossveil Cowl',
 'item.wildercord.mossveil_cowl.lore': 'A quiet breath beside a trusted nose.',
 'message.wildercord.mossveil.trim': 'The cowl filters %s ticks of poison. Your companion needs a rest.',
 'guide.wildercord.mossveil_dormouse.hint': 'Look for rounded little sleepers beside mature cave Glowcaps.',
 'guide.wildercord.mossveil_dormouse': 'Offer three dried Glowcap Gills, waiting five seconds between feedings. The first feeder keeps a five-minute claim; other players cannot finish it. Only her owner can order a curl with an empty hand. She follows a loaded owner by ordinary paths and never teleports across worlds. One Gill can heal a wounded companion by at most two health; full-health feeding refuses. She produces no materials and does not breed. A supported Fungal Nursery gives the Mossveil Cowl a real home: crouch still for two seconds beside your own curled companion to shorten finite poison by at most three seconds and one quarter of its remaining duration. Wear and both saved ten-second rests are paid. Stronger poison, physical damage and movement remain dangerous.',
}
for name, text in [('sniff','Dormouse nose twitches'),('nibble','Dormouse nibbles a Gill'),('trust','Dormouse answers softly'),('curl','Dormouse curls in cloth'),('idle','Dormouse chirps quietly'),('step','Tiny paws brush moss'),('filter','Stitched cowl takes a quiet breath')]:
 LANG['subtitles.wildercord.kit.mossveil.'+name] = text

def skin():
 s=Skin(96,64)
 def fur(f,x,y,z,tx,ty):
  c=(88,78,57) if int(x+z*2)%5 else (119,112,79)
  if f=='top': c=(100,103,68) if int(x*2+z)%4 else (57,68,48)
  return shade(c,.88+noise(tx,ty,9413)*.22)
 def cream(f,x,y,z,tx,ty):return shade((199,178,135),.88+noise(tx,ty,9479)*.2)
 def ear(f,x,y,z,tx,ty):return shade((157,113,97),.88+noise(tx,ty,9503)*.18)
 def tail(f,x,y,z,tx,ty):return shade((103,93,72) if int(z)%2 else (151,131,96),.9+noise(tx,ty,9551)*.16)
 for u,v,w,h,d,m in [(0,0,8,6,10,fur),(0,20,6,4,3,cream),(40,0,6,5,6,fur),(40,14,3,2,2,cream),(64,0,3,3,1,fur),(74,0,2,2,1,ear),(20,20,2,2,3,fur),(32,20,2,1,2,ear),(40,22,2,2,5,tail),(56,22,2,2,4,tail),(70,22,1,1,3,tail),(0,38,4,1,3,lambda f,x,y,z,tx,ty:shade((117,143,76),.85+noise(tx,ty,9619)*.2))]:s.box(u,v,w,h,d,m)
 s.box(54,14,1,1,1,lambda f,x,y,z,tx,ty:(129,81,74))
 s.box(84,0,1,1,1,lambda f,x,y,z,tx,ty:(24,21,19) if f!='top' else (225,221,187))
 im=s.image();ImageDraw.Draw(im).line((0,32,9,32),fill=(216,200,160,255));return im

def hood():
 im=Image.new('RGBA',(64,32));d=ImageDraw.Draw(im)
 for y in range(16):
  for x in range(32):
   n=noise(x,y,9721);c=(95+int(n*15),114+int(n*17),74+int(n*12),255)
   if (x+y)%5==0:c=(167,161,113,255)
   d.point((x,y),fill=c)
 # Lower face remains open; two stitched cheeks and a darker respirator fold are real worn pixels.
 d.rectangle((9,10,14,15),fill=(0,0,0,0));d.line((8,8,15,8),fill=(205,193,142,255));d.line((8,14,15,14),fill=(199,185,132,255))
 for x in range(9,15,2):d.line((x,8,x,10),fill=(52,68,46,255))
 return im

def icon(egg=False):
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
 if egg:
  d.ellipse((7,2,25,29),fill='#9C9A70',outline='#34392B',width=2);d.ellipse((10,11,22,24),fill='#CDB68A');d.arc((9,5,23,18),170,350,fill='#667449',width=3);d.point((13,16),fill='#23211C');d.point((20,16),fill='#23211C')
 else:
  d.polygon([(5,23),(6,10),(12,4),(22,5),(28,12),(27,27),(22,28),(20,17),(12,17),(10,27)],fill='#65794C',outline='#303C28')
  d.arc((7,6,26,26),190,355,fill='#CCBF88',width=2);d.line((7,12,10,24),fill='#B4AC78',width=2);d.line((24,12,25,23),fill='#B4AC78',width=2)
  for y in range(12,24,3):d.line((11,y,13,y+1),fill='#9B9D67');d.line((21,y,23,y+1),fill='#9B9D67')
  d.rectangle((13,14,20,19),fill='#414F37');d.line((14,16,19,16),fill='#D0C591')
 return im

def fiber():
 im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
 d.line((2,13,5,10,8,8,10,4,13,2),fill='#B7B787',width=2)
 d.line((2,12,5,9,8,7,10,3,13,1),fill='#E4DDB5',width=1)
 for x,y in [(4,11),(7,8),(10,5)]:d.line((x,y,x+3,y+1),fill='#768359')
 return im

def plate():
 # Original field-journal illustration; caption explicitly separates this from future native screenshots.
 im=Image.new('RGBA',(1200,640),'#ECE3CB');d=ImageDraw.Draw(im)
 for y in range(20,620,6):
  for x in range(20,1180,6):
   if noise(x,y,9833)>.8:d.point((x,y),fill='#DDD1B3')
 d.rectangle((22,22,1177,617),outline='#7B8160',width=2);d.line((60,108,1140,108),fill='#A5A582',width=2)
 d.text((60,45),'MOSSVEIL  /  A HOME FOR A QUIET NOSE',fill='#334630',stroke_width=0)
 d.text((60,78),'Illustrated field plate - not native game evidence',fill='#72674D')
 # Short, rounded belly, cream cheek, hinged ears and a curling segmented tail echo the authored rig vocabulary.
 d.ellipse((142,242,555,483),fill='#6E704A',outline='#354C30',width=4);d.ellipse((207,325,440,480),fill='#C4AE81')
 d.ellipse((102,188,352,376),fill='#80785A',outline='#354C30',width=3);d.ellipse((125,134,186,228),fill='#766D51',outline='#354C30',width=3);d.ellipse((137,147,173,211),fill='#B58070')
 d.ellipse((278,145,341,236),fill='#766D51',outline='#354C30',width=3);d.ellipse((291,158,327,217),fill='#B58070')
 d.ellipse((144,291,311,378),fill='#D0B88B');d.ellipse((147,238,175,268),fill='#29271F');d.ellipse((287,242,316,273),fill='#29271F');d.ellipse((153,240,160,247),fill='#E8DFC2');d.ellipse((295,244,302,251),fill='#E8DFC2')
 d.ellipse((215,304,244,325),fill='#A46C60');
 for side in [-1,1]:
  for step in range(3):d.line((231+side*30,329+step*7,231+side*98,318+step*12),fill='#EFE2BC',width=2)
 for x,y in [(175,470),(255,480),(391,471),(480,461)]:d.ellipse((x,y,x+45,y+20),fill='#AB876D',outline='#4D4F37',width=2)
 d.arc((397,326,660,552),20,340,fill='#C0AB81',width=18);d.arc((448,369,594,507),80,338,fill='#736647',width=14)
 d.polygon([(350,256),(382,226),(408,260),(430,229),(451,267),(420,278)],fill='#8B9D58')
 for x,y in [(194,202),(191,214),(360,288),(430,323),(323,302),(480,385)]:d.line((x,y,x+11,y+3),fill='#A5AA73',width=3)
 d.text((144,552),'Three paid Gills. Four paws. One trusted owner.',fill='#455638')
 cowl=icon(False).resize((256,256),Image.Resampling.NEAREST);im.alpha_composite(cowl,(790,167))
 d.text((753,449),'MOSSVEIL COWL',fill='#334630');d.text((753,480),'Helmet slot / one armour / finite wear',fill='#635C43');d.text((753,510),'A still breath beside your curled companion',fill='#635C43');d.text((753,540),'Floss + Gills + Mycelial Dew',fill='#635C43')
 return im

def write(g):
 g.save(skin(),g.ASSETS/'textures/entity/mossveil_dormouse.png')
 g.save(hood(),g.ASSETS/'textures/entity/equipment/humanoid/mossveil.png')
 g.save(fiber(),g.ASSETS/'textures/particle/mossveil_fiber.png')
 g.save(plate(),g.ROOT/'wiki/assets/mossveil/illustrated-field-plate.png')
 g.write_json(g.ASSETS/'equipment/mossveil.json',{'layers':{'humanoid':[{'texture':'wildercord:mossveil'}]}})
 # Registered independent physical particle requests its standalone sprite from the ordinary particle atlas.
 g.write_json(g.ASSETS/'particles/mossveil_fiber.json',{'textures':['wildercord:mossveil_fiber']})
 for name,egg in [('mossveil_cowl',False),('mossveil_dormouse_spawn_egg',True)]:
  g.save(icon(egg),g.ASSETS/f'textures/item/{name}.png')
  g.write_json(g.ASSETS/f'models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'wildercord:item/{name}'}})
  g.write_json(g.ASSETS/f'items/{name}.json',{'model':{'type':'minecraft:model','model':f'wildercord:item/{name}'}})
 g.write_json(g.DATA/'loot_table/entities/mossveil_dormouse.json',{'type':'minecraft:entity','pools':[]})
 g.write_json(g.DATA/'recipe/mossveil_cowl.json',{'type':'minecraft:crafting_shapeless','category':'equipment','ingredients':['wildercord:moonreed_floss','wildercord:dried_glowcap_gills','wildercord:mycelial_dew'],'result':{'id':'wildercord:mossveil_cowl','count':1}})
 g.write_json(g.DATA/'tags/item/repairs_mossveil.json',{'replace':False,'values':['wildercord:moonreed_floss']})
 g.write_json(g.DATA/'advancement/recipes/mossveil_cowl.json',{'criteria':{'has_gills':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':'wildercord:dried_glowcap_gills'}]}}},'requirements':[['has_gills']],'rewards':{'recipes':['wildercord:mossveil_cowl']}})
