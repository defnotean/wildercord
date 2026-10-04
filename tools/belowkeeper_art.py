"""Belowkeeper restoration: oxidized copper, carved water gutters, woven root boots and a hollow bell."""
from PIL import Image,ImageDraw
from world_art import noise,Sheet,box
from wildlife_art import shade
LANG={
 'block.wildercord.drainhouse_mark':'Belowkeeper Ledger',
 'item.wildercord.maras_empty_bell':"Mara's Empty Bell",'item.wildercord.maras_empty_bell.lore':'The clapper was removed so the roots could hear the rim.',
 'item.wildercord.maras_empty_bell.use':'Stand still and hold use for one second near one visible Rootmolt warning or grab. Moving or changing the held bell cancels commitment. Interrupts that creature; one wear, twelve-second shared rest and two heavy seconds. Restore or repair at the authentic Drainhouse alcove.',
 'item.wildercord.rootbound_greaves':'Rootbound Greaves','item.wildercord.rootbound_greaves.lore':'Small roots woven through leather remember one still place.',
 'item.wildercord.rootbound_greaves.use':'Wear in the feet slot. Crouch still on dry ground for 1.5 seconds to resist one Rootmolt hold after its physical damage. Eight extra wear, ten-second shared rest and two heavy seconds. Movement, jumping, swimming, sprinting or swapping boots breaks preparation.',
 'item.wildercord.drainhouse_threshold':'The Borrowed Third Breath','item.wildercord.drainhouse_garden':'Six Feet at the Table','item.wildercord.drainhouse_alcove':'What the Empty Bell Keeps',
 **{'message.wildercord.drainhouse.'+k:v for k,v in {'snail':'Crouch with an empty hand and gather dew from a rested Sporeback carrying a bead.','threshold':'Read the threshold ledger of an authentic Belowkeeper Drainhouse.','meal':'Stay within six blocks, with a clear view, while a living Rootmolt eats a mature Glowcap.','garden':'Read the damp garden ledger after that actual meal.','counter':'After seeing a meal, strike the living Rootmolt during its warning or rake, or sidestep the rake and stay nearby until it ends.','alcove':'Read the raised copper alcove ledger after proving that counter.','restore':'Hold Mycelial Dew and use the raised alcove desk with two dew, two dried gills and four copper ingots.','done':'The bell is yours. Return with one dew, one gill and one copper to mend eight wear after the desk rests a minute.','replica':'This ledger has no authentic Drainhouse provenance, or you cannot reach it.'}.items()},
 'book.wildercord.drainhouse.0.1':"Mara, Belowkeeper\n\nA drain is a promise: water goes where we guide it. The hill settled, but green gutters still catch rain. The ribs still hold the roof. This house did not fall at once.",
 'book.wildercord.drainhouse.0.2':"The third breath\n\nThe pale spiral leaves the cap standing. The six-footed keeper eats it and guards the bank. We called both helpers too soon. Watch the meal before giving a creature a name.",
 'book.wildercord.drainhouse.0.3':"A safe visit\n\nGently gather real dew from a snail first. Prepare a bud with Life; leave the spiral a clear path. See a living Rootmolt eat a mature cap within six blocks, then read the garden desk.",
 'book.wildercord.drainhouse.1.1':"Six feet at table\n\nThe cap vanished beneath breathing gills. No dew bead filled my jar. The plated stranger left, then turned when I neared its bank. Hunger had ended. Ownership had begun.",
 'book.wildercord.drainhouse.1.2':"A refused line\n\nHit a living keeper during its warning, rake or hold. Or step aside: stay near the marked line, keep a clear view and let the rake finish. Running far away proves no counter.",
 'book.wildercord.drainhouse.1.3':"The upper desk\n\nThe hands that saw the meal must prove a counter. A corpse cannot show a warning. Bring that witness to the raised copper alcove and read its ledger. We left the empty bell unfinished.",
 'book.wildercord.drainhouse.2.1':"The empty bell\n\nI took out the cracked clapper and tapped the rim. The roots loosened. Others called it a charm. I call it a brief, close interruption that can leave both creatures alive.",
 'book.wildercord.drainhouse.2.2':"Restore the rim\n\nRead all three desks; prove the meal and counter. Bring two dew beads, two harvested gills and four copper ingots. Use dew on the authentic upper desk. Each traveller earns one bell.",
 'book.wildercord.drainhouse.2.3':"Rootbound feet\n\nCraft greaves with boots, copper, gills and dew. Crouch still on dry ground; one hold is stopped after its wound. Bell and roots must rest. Mend at this desk with dew, gills and copper.",
 **{'subtitles.wildercord.kit.drainhouse.'+k:v for k,v in [('read','Belowkeeper ledger opens'),('restore','Copper rim is bound'),('bell','Empty bell answers roots'),('anchor','Woven roots hold footing')]}
}

def material(name):
 im=Image.new('RGBA',(16,16));px=im.load()
 for y in range(16):
  for x in range(16):
   if name=='copper':c=(151,107,65) if (x+y)%7 else (80,124,101)
   elif name=='stone':c=(67,77,78) if y%5 else (44,54,57)
   elif name=='leather':c=(89,61,45) if x%4 else (145,103,65)
   elif name=='paper':c=(183,171,132) if (x+y)%4 else (213,197,149)
   else:c=(95,108,70) if x%3 else (180,166,111)
   px[x,y]=(*shade(c,.82+noise(x,y,2093)*.3),255)
 d=ImageDraw.Draw(im)
 if name=='copper':
  d.line((1,2,14,2),fill='#E2C58D');d.line((2,14,13,14),fill='#594932')
  for x,y in [(2,3),(13,3),(2,13),(13,13)]:d.point((x,y),fill='#E8D4A1');d.point((x+1,y+1),fill='#6B795A')
  d.line([(4,11),(6,8),(10,9),(12,6)],fill='#7EA48A')
 elif name=='stone':
  d.line([(5,1),(6,6),(4,10),(6,15)],fill='#313E40');d.line([(10,2),(9,7),(12,11)],fill='#9BA693')
 elif name=='paper':
  for y in [4,7,10,13]:d.line((3,y,12-(y%3),y),fill='#615B46')
  for x in [2,6,10,14]:d.point((x,1),fill='#ECE0B5');d.point((x,14),fill='#938C6C')
 elif name=='leather':
  for y in [2,5,8,11,14]:d.line((1,y,2,y+1),fill='#D7BA82');d.line((13,y,14,y+1),fill='#D7BA82')
 else:
  d.line([(4,0),(6,4),(4,8),(7,13),(6,15)],fill='#D1C794');d.line([(12,0),(10,5),(12,9),(9,15)],fill='#394F39')
 return im

def boots_icon():
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
 for shift in [0,13]:
  d.polygon([(5+shift,5),(12+shift,5),(12+shift,20),(15+shift,23),(15+shift,27),(3+shift,27),(3+shift,23),(5+shift,20)],fill='#3C3028')
  d.rectangle((6+shift,6,11+shift,20),fill='#815A3D');d.rectangle((4+shift,23,13+shift,25),fill='#AB8D5A')
  px=im.load()
  for yy in range(6,26):
   for xx in range(4+shift,14+shift):
    if px[xx,yy][3] and px[xx,yy][:3] in [(129,90,61),(171,141,90)]:
     base=px[xx,yy][:3];f=.78+(.2 if xx<9+shift else 0)+noise(xx,yy,2309)*.18;px[xx,yy]=(*shade(base,f),255)
  d.line((4+shift,26,14+shift,26),fill='#D4BC88');d.line((7+shift,7,7+shift,18),fill='#AD855B');d.line((10+shift,21,13+shift,23),fill='#E5CC95')
  for y in [8,13,18]:d.line((5+shift,y,12+shift,y),fill='#A67845');d.point((11+shift,y+1),fill='#80A081')
  d.line([(7+shift,7),(9+shift,12),(7+shift,17),(10+shift,23)],fill='#C1B080');d.line([(10+shift,7),(8+shift,11),(10+shift,16),(8+shift,22)],fill='#75885C')
  for y in [9,12,15,18]:d.point((6+shift,y),fill='#E0CB95')
 # Left copper clasp and right stitched repair are deliberately unequal, not palette-swapped boots.
 d.rectangle((5,8,9,10),fill='#B99459');d.point((6,8),fill='#ECD3A0');d.rectangle((6,9,8,9),fill='#453D2B')
 d.line([(20,15),(22,16),(20,17),(22,18)],fill='#E2D2A0');d.line((18,24,21,24),fill='#3C4030')
 return im

def boots_skin():
 cv=Sheet(64,32)
 for u in [0,40]:
  for face,(x0,y0,w,h) in box(u,16,4,12,4).items():
   for y in range(h):
    for x in range(w):
     c=(72,49,37) if y<6 else (131,93,60)
     if y in (6,9):c=(177,129,74)
     if y>=6 and (x+y)%4==0:c=(139,151,96)
     if y==h-1:c=(49,42,35)
     if y>=7 and x==1:c=(209,190,130)
     cv.put(x0+x,y0+y,(*shade(c,.8+noise(x0+x,y0+y,2171)*.3),255))
 return cv.image()

def ledger_paper(kind):
 # Actual room-specific writing materials; same 16px paper UV and #paper material slot.
 im=material('paper');d=ImageDraw.Draw(im)
 d.rectangle((3,3,12,12),fill=['#C8BA8C','#B6B78B','#CEC090'][kind])
 if kind==0: # Threshold: carved drainage ribs and a single cut-water line.
  d.line([(4,11),(4,4),(11,4),(11,11)],fill='#544F3C',width=2)
  d.line([(6,10),(6,6),(8,6),(8,11)],fill='#698B81')
  d.line((5,12,11,12),fill='#ECE0B2');d.point((10,5),fill='#F3E6B3')
 elif kind==1: # Garden: six foot witnesses around the consumed cap stump.
  d.ellipse((5,4,10,8),fill='#6F7950',outline='#E2D6A3');d.line((7,8,7,12),fill='#685C3D',width=2)
  for x,y in [(3,5),(3,8),(3,11),(12,5),(12,8),(12,11)]:d.line((x,y,5 if x<7 else 10,y+1),fill='#58694A')
  d.point((8,5),fill='#EFF0BD')
 else: # Alcove: empty rim, exposed throat and copper seam.
  d.polygon([(6,4),(9,4),(10,9),(12,11),(4,11),(6,9)],fill='#A27743')
  d.line((4,12,12,12),fill='#534B36');d.line((5,10,11,10),fill='#79A193')
  d.line((7,5,7,8),fill='#F0DBA4');d.arc((6,2,9,5),180,360,fill='#756744')
 return im

def book_icon(index):
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
 if index==0: # Folded threshold folio with gutter diagram, exposed stitched spine and dog-ear.
  d.polygon([(4,6),(21,3),(26,7),(26,25),(8,29),(4,26)],fill='#292E2A')
  d.polygon([(8,7),(23,4),(24,24),(8,27)],fill='#A4956C');d.polygon([(20,4),(23,4),(23,8)],fill='#E5D7A9')
  d.line((5,7,5,24),fill='#687B64',width=2);d.line((9,28,25,25),fill='#E7D6A1',width=2)
  for y in [9,13,17,21]:d.line((4,y,7,y-1),fill='#D8CE9E')
  d.line([(11,23),(11,11),(20,9),(21,21)],fill='#554B36',width=2)
  d.line([(14,21),(14,14),(18,13),(18,22)],fill='#739A8E');d.line((12,24,21,22),fill='#E8D6A0')
 elif index==1: # Rounded soft garden notebook: strapped leather, real root stitch and six-foot plate.
  d.polygon([(6,4),(22,3),(26,6),(26,24),(22,28),(6,29),(3,25),(3,8)],fill='#2F392C')
  d.polygon([(8,6),(22,5),(24,7),(24,24),(21,26),(8,27),(6,24),(6,9)],fill='#586346')
  d.line((7,7,7,25),fill='#C9BF8B');d.line((9,28,21,27),fill='#D9CCA0')
  d.line([(10,5),(12,10),(10,15),(13,20),(11,26)],fill='#93875E')
  d.ellipse((13,10,22,18),fill='#A6AC79',outline='#DAD2A1');d.line((17,18,17,23),fill='#BCA576',width=2)
  for x,y in [(11,12),(11,16),(11,21),(24,11),(24,15),(24,20)]:d.line((x,y,14 if x<17 else 21,y+1),fill='#DED6A7')
  d.rectangle((4,18,8,21),fill='#86623E');d.point((7,19),fill='#DDC58C');d.point((22,6),fill='#B1B76F')
 else: # Copper-bound workshop ledger: uneven corners, riveted spine and open bell throat.
  d.polygon([(4,5),(22,3),(27,7),(27,25),(8,29),(4,25)],fill='#293531')
  d.polygon([(8,7),(23,5),(25,8),(25,24),(9,27)],fill='#927549')
  d.line((5,6,5,24),fill='#80A395',width=2);d.line((10,28,26,25),fill='#D7C396',width=2)
  for y in [8,13,18,23]:d.rectangle((4,y,6,y+1),fill='#DCC28B')
  for x,y in [(9,8),(23,7),(10,24),(23,22)]:d.point((x,y),fill='#F2DCAB');d.point((x+1,y+1),fill='#5A705B')
  d.polygon([(15,10),(19,9),(21,19),(23,21),(11,23),(13,20)],fill='#D2B471',outline='#594932')
  d.line((13,21,21,20),fill='#51604E',width=2);d.line((14,20,22,19),fill='#89AC9B')
  d.line((15,12,15,18),fill='#F1DBA2');d.arc((15,6,20,12),180,360,fill='#ECD6A1')
 return im

def write(g):
 for name in ['copper','stone','leather','paper','root']:g.save(material(name),g.ASSETS/f'textures/block/drainhouse_{name}.png')
 for kind in range(3):g.save(ledger_paper(kind),g.ASSETS/f'textures/block/drainhouse_ledger_{kind}.png')
 def model(path,boxes,display=None):
  textures={name:f'wildercord:block/drainhouse_{name}' for name in ['copper','stone','leather','paper','root']}
  if path.startswith('block/drainhouse_mark_'):textures['paper']='wildercord:block/drainhouse_ledger_'+path.split('_')[-2]
  data={'parent':'minecraft:block/block','textures':{'particle':textures['stone'],**textures},'elements':[{'from':a,'to':b,'faces':{f:{'texture':'#'+m,'uv':[0,0,16,16]} for f in ['up','down','north','south','east','west']}} for a,b,m in boxes]}
  if display:data['display']=display
  g.write_json(g.ASSETS/f'models/{path}.json',data)
 for k in range(3):
  for restored in [False,True]:
   boxes=([([2,0,2],[14,3,14],'stone'),([3,3,5],[13,13,11],'paper'),([2,12,4],[14,14,12],'copper')] if k==0 else [([2,0,2],[14,3,14],'root'),([3,3,3],[13,5,13],'paper'),([4,5,4],[12,7,12],'paper'),([2,3,2],[4,14,4],'copper'),([12,3,12],[14,14,14],'copper')] if k==1 else [([1,0,1],[15,4,15],'stone'),([2,4,3],[14,7,13],'copper'),([3,7,4],[13,9,12],'paper'),([2,7,3],[4,12,5],'root'),([12,7,11],[14,12,13],'root')])
   if restored and k==2:boxes +=[([6,9,6],[10,12,10],'copper'),([5,9,5],[11,10,11],'copper')]
   model(f'block/drainhouse_mark_{k}_{int(restored)}',boxes)
 g.write_json(g.ASSETS/'blockstates/drainhouse_mark.json',{'variants':{f'kind={k},restored={str(r).lower()}':{'model':f'wildercord:block/drainhouse_mark_{k}_{int(r)}'} for k in range(3) for r in [False,True]}})
 # A hollow real held bell, stepped rim, leather loop and open cavity; not a flat generic charm.
 bell=[([6,11,7],[7,14,9],'leather'),([9,11,7],[10,14,9],'leather'),([7,13,7],[9,14,9],'leather'),([5,7,5],[6,11,11],'copper'),([10,7,5],[11,11,11],'copper'),([6,7,5],[10,11,6],'copper'),([6,7,10],[10,11,11],'copper'),([4,5,4],[5,7,12],'copper'),([11,5,4],[12,7,12],'copper'),([5,5,4],[11,7,5],'copper'),([5,5,11],[11,7,12],'copper'),([6,10,6],[10,11,10],'copper')]
 model('item/maras_empty_bell',bell,{'gui':{'rotation':[25,225,0],'scale':[1.1,1.1,1.1]},'firstperson_righthand':{'rotation':[0,80,15],'translation':[0,3,0],'scale':[.7,.7,.7]},'thirdperson_righthand':{'rotation':[0,0,0],'translation':[0,3,0],'scale':[.6,.6,.6]}})
 g.save(boots_icon(),g.ASSETS/'textures/item/rootbound_greaves.png');g.item_model('rootbound_greaves','rootbound_greaves')
 for name in ['rootbound_greaves','maras_empty_bell']:g.write_json(g.ASSETS/f'items/{name}.json',{'model':{'type':'minecraft:model','model':f'wildercord:item/{name}'}})
 g.save(boots_skin(),g.ASSETS/'textures/entity/equipment/humanoid/rootbound.png');g.write_json(g.ASSETS/'equipment/rootbound.json',{'layers':{'humanoid':[{'texture':'wildercord:rootbound'}]}})
 g.write_json(g.DATA/'recipe/rootbound_greaves.json',{'type':'minecraft:crafting_shapeless','category':'equipment','ingredients':['minecraft:leather_boots','minecraft:copper_ingot','wildercord:dried_glowcap_gills','wildercord:mycelial_dew'],'result':{'id':'wildercord:rootbound_greaves','count':1}});g.unlock_advancement('wildercord:rootbound_greaves','wildercord:dried_glowcap_gills')
 g.write_json(g.DATA/'worldgen/feature/belowkeeper_drainhouse.json',{'type':'wildercord:belowkeeper_drainhouse'})
 g.write_json(g.DATA/'worldgen/placed_feature/belowkeeper_drainhouse.json',{'feature':'wildercord:belowkeeper_drainhouse','placement':[{'type':'minecraft:rarity_filter','chance':48},{'type':'minecraft:in_square'},{'type':'minecraft:height_range','height':{'type':'minecraft:uniform','min_inclusive':{'absolute':-32},'max_inclusive':{'absolute':40}}}]})
 g.write_json(g.DATA/'loot_table/blocks/drainhouse_mark.json',{'type':'minecraft:block','pools':[]})
 # Separate illustrated books, not three plain recolours.
 for index,name in enumerate(['drainhouse_threshold','drainhouse_garden','drainhouse_alcove']):
  im=book_icon(index)
  g.save(im,g.ASSETS/f'textures/item/{name}.png');g.item_model(name,name);g.write_json(g.ASSETS/f'items/{name}.json',{'model':{'type':'minecraft:model','model':f'wildercord:item/{name}'}})
 # Preserve the preceding armor generator's existing tag members.
 import json
 target=g.RES/'data/minecraft/tags/item/foot_armor.json';prior=json.loads(target.read_text()) if target.exists() else {'replace':False,'values':[]}
 prior['values']=sorted(set(prior['values']+['wildercord:rootbound_greaves']));g.write_json(target,prior)
