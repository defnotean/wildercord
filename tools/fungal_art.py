"""Damp ivory caps, copper/leather fieldcraft and Belowkeeper carved field marks."""
from PIL import Image,ImageDraw
from wildlife_art import shade
from world_art import noise
LANG={
 'block.wildercord.glowcap':'Glowcap','block.wildercord.fungal_nursery':'Fungal Nursery','block.wildercord.breathmark':'Belowkeeper Breathmark',
 'item.wildercord.dried_glowcap_gills':'Dried Glowcap Gills','item.wildercord.dried_glowcap_gills.lore':'Paper-thin lamellae hold the memory of a damp bank.','item.wildercord.dried_glowcap_gills.use':'Craft a Fungal Nursery or a Cave Breather.',
 'item.wildercord.cave_breather':'Cave Breather','item.wildercord.cave_breather.lore':'A Belowkeeper filter, built to trade quick feet for one clear breath.','item.wildercord.cave_breather.use':'Hold in your offhand and use to clear current Poison. 32 filter uses; 10 second rest; 3 seconds of Slowness. Ongoing attacks can poison you again.',
 'item.wildercord.breathmark_roots':'Where the Roots Drink','item.wildercord.breathmark_air':'The Second Breath','item.wildercord.nursery_journal':'Three Breathmarks',
 'message.wildercord.fungal.replica':'This is an ordinary carved stone. Its field memory is quiet.',
 'message.wildercord.fungal.filter_rest':'Hold the filter in your offhand. It needs current Poison and a rested filter.',
 **{'message.wildercord.fungal.hint_'+k:v for k,v in {
 'snail':'First, kneel empty-handed to gather dew from a patient Sporeback Snail.',
 'root':'Find the root-glyph Breathmark on covered cave moss or clay below Y48.',
 'air':'Find a different Breathmark bearing the open-air glyph. Another root mark adds no step.',
 'harvest':'Grow a damp dark Glowcap bud, let a snail browse it, and harvest its mature gills yourself.',
 'roof':'Craft a Fungal Nursery and place it yourself after harvesting gills.',
 'breather':'Craft your Cave Breather after placing the nursery: dew, dried gills, leather and copper.',
 'return':'Show your Cave Breather to a Fungal Nursery to finish Mara\'s investigation.',
 'done':'Mara\'s Three Breathmarks are complete. The nursery still shelters; the filter still works.'}.items()},
 'book.wildercord.fungal.0.1':'WHERE ROOTS DRINK\n\nMara, Belowkeeper\n\nI followed a patient spiral to the cap below this stone. The stream stayed in its bed. Its roots drank from the side, never from water over their heads. The lamp was enough to spoil a bud.',
 'book.wildercord.fungal.0.2':'Set a Glowcap cutting on moss or clay, beside water and under cover. Keep it below bright torchlight. Life can prepare a bud. Only the slow mouth of a living snail can open its gills; leave the visitor its own resting clocks.',
 'book.wildercord.fungal.0.3':'Harvest the opened gills and leave the stem standing. Each cap begins again as a young bud. The next stone bears an open-air glyph. Seek another Belowkeeper mark on covered cave moss or clay, not a copy of this root.',
 'book.wildercord.fungal.1.1':'THE SECOND BREATH\n\nMara, Belowkeeper\n\nWe once boxed our spirals in. They went still, but it was fear, not rest. This mark remembers the apprentice who cut four windows into a shelter and let the visitors choose whether to stay.',
 'book.wildercord.fungal.1.2':'Two sticks, paper and dried Glowcap gills make an open nursery canopy. Put its roof one block above a walkable dark footing. One snail can tuck beneath it after browsing. It gives no extra dew and shortens no gathering rest.',
 'book.wildercord.fungal.1.3':'Dew, gills, leather and copper make a Cave Breather. Keep it in the offhand. One breath clears present poison, then leaves three heavy seconds. The filter rests ten seconds. A fresh bite can still reach you. It is no shield.',
 'book.wildercord.fungal.2.1':'THREE BREATHMARKS\n\nMara, Belowkeeper\n\nYou learned the living spiral, the drinking root and the open roof. Then you made your own breath. I leave three blank runes for what you discover next. This gift is made once.',
 'book.wildercord.fungal.2.2':'Keep the garden working: water beside the roots, dark cover above, and a visitor free to leave. A new harvest needs another prepared bud and another real browse. No lamp or chant pays the snail\'s resting time for it.',
 'book.wildercord.fungal.2.3':'We are not owners of the deep. We are guests who mark a safe step for the next traveller. Record the openings you find. Share a filter when a friend stumbles. The nursery remains useful after the journal is finished.',
 **{'subtitles.wildercord.kit.fungal.'+n:t for n,t in [('cap_open','Glowcap gills unfold'),('cap_harvest','Gills are gathered'),('mark_read','Stone memory is read'),('nursery_settle','Visitor settles'),('filter','Filter exhales')]}}

def write(g):
 for name in ['cap','stem','gills','roof','frame','copper','leather','stone','root_glyph','air_glyph']:
  im=Image.new('RGBA',(16,16));px=im.load()
  for y in range(16):
   for x in range(16):
    n=noise(x,y,1180+len(name));f=.87+n*.22+(15-y)*.002
    if name=='cap':
     c=(167,193,159) if (x*3+y*5)%13<4 else (202,217,177);c=(123,149,139) if x in (0,15) or y in (0,15) else c
     if (x-5)**2+(y-5)**2<4 or (x-11)**2+(y-10)**2<2:c=(226,234,201)
    elif name=='stem':c=(140+x%3*9,118+x%3*8,146+x%3*8)
    elif name=='gills':c=(230,223,181) if x%3 else (132,153,132)
    elif name=='roof':c=(101,123,102) if x%4 else (160,154,108);c=(205,198,148) if y%5==0 else c
    elif name=='frame':c=(91+x%3*10,64+x%3*7,52+x%3*5)
    elif name=='copper':c=(164,119,75) if (x+y)%7 else (95,137,109);c=(222,182,113) if y in (1,14) else c
    elif name=='leather':c=(102,71,57) if y%4 else (147,108,73)
    else:
     c=(111,120,116) if y%5 else (85,94,95)
     if name=='root_glyph' and (x in (6,7) or (y>7 and abs(x-7)==(y-6)//2)):c=(184,169,119)
     if name=='air_glyph' and (abs(x-8)+abs(y-8) in (5,6) or (y==8 and x<8)):c=(173,195,172)
    px[x,y]=(*shade(c,f),255)
  # Authored material details survive close views: woven crossings, frayed paper fibres,
  # cut wood grain, seams and riveted weathered copper rather than flat tinted planes.
  d=ImageDraw.Draw(im)
  if name=='roof':
   for y in [2,7,12]:
    for x in [1,5,9,13]:
     d.line((x,y,x+2,y+1),fill='#D6CC96');d.point((x+2,y+2),fill='#536F60')
   for x,y in [(2,1),(11,4),(6,9),(13,13)]:d.line((x,y,x+1,y+2),fill='#E6DCAF')
  elif name=='frame':
   d.line([(3,0),(2,4),(4,8),(3,15)],fill='#AE8662');d.line([(11,0),(10,6),(12,11),(11,15)],fill='#3E302D');d.ellipse((5,6,9,10),outline='#4E392F');d.point((7,8),fill='#C69B71')
  elif name=='copper':
   for x,y in [(2,2),(13,2),(2,13),(13,13)]:d.point((x,y),fill='#FFE4A2');d.point((x+1,y+1),fill='#72593F')
   d.line((3,10,7,8),fill='#749B7A');d.line((8,12,12,10),fill='#618672')
  elif name=='leather':
   for y in [1,5,9,13]:d.line((1,y,2,y+1),fill='#DCC391');d.line((13,y,14,y+1),fill='#DCC391')
  elif name=='cap':
   d.line((4,3,8,2),fill='#EDF0CC');d.point((3,4),fill='#E7EAC3')
  g.save(im,g.ASSETS/f'textures/block/fungal_{name}.png')
 def model(path,textures,boxes):
  elements=[{'from':a,'to':b,'faces':{f:{'texture':'#'+m,'uv':[0,0,16,16]} for f in ['up','down','north','south','east','west']}} for a,b,m in boxes]
  g.write_json(g.ASSETS/f'models/{path}.json',{'parent':'minecraft:block/block','textures':{'particle':next(iter(textures.values())),**textures},'elements':elements})
 for age in range(3):
  top=5+age*3;half=3+age;boxes=[([7,0,7],[9,top,9],'stem'),([8-half,top-1,8-half],[8+half,top,8+half],'gills'),([7-half,top,7-half],[9+half,top+1,9+half],'cap'),([8-half,top+1,8-half],[8+half,top+2,8+half],'cap')]
  if age==2:boxes +=[([3,0,10],[4,5,11],'stem'),([1,5,8],[6,6,13],'gills'),([1,6,8],[6,7,13],'cap'),([2,7,9],[5,8,12],'cap')]
  model(f'block/glowcap_{age}',{m:f'wildercord:block/fungal_{m}' for m in ['cap','stem','gills']},boxes)
 g.write_json(g.ASSETS/'blockstates/glowcap.json',{'variants':{f'age={a}':{'model':f'wildercord:block/glowcap_{a}'} for a in range(3)}})
 boxes=[]
 for x in [0,14]:
  for z in [0,14]:boxes.append(([x,-16,z],[x+2,12,z+2],'frame'))
 for x in [0,4,8,12]:boxes.append(([x,12,0],[x+4,14,16],'roof'))
 for z in [0,14]:boxes.append(([0,14,z],[16,15,z+2],'copper'));boxes.append(([0,10,z],[16,12,z+2],'frame'))
 for x in [0,14]:
  for z in [0,14]:boxes.append(([x,8,z],[x+2,9,z+2],'copper'))
 boxes +=[([4,14,3],[6,16,13],'gills'),([10,14,3],[12,16,13],'gills')]
 model('block/fungal_nursery',{m:f'wildercord:block/fungal_{m}' for m in ['frame','roof','copper','gills']},boxes)
 g.write_json(g.ASSETS/'blockstates/fungal_nursery.json',{'variants':{'':{'model':'wildercord:block/fungal_nursery'}}})
 for kind,glyph in [(0,'root_glyph'),(1,'air_glyph')]:
  model(f'block/breathmark_{kind}',{m:f'wildercord:block/fungal_{m}' for m in ['stone',glyph]},[([2,0,2],[14,3,14],'stone'),([3,3,4],[13,11,12],glyph),([4,11,5],[12,13,11],'stone')])
 g.write_json(g.ASSETS/'blockstates/breathmark.json',{'variants':{f'kind={k}':{'model':f'wildercord:block/breathmark_{k}'} for k in range(2)}})
 for name,parent in [('glowcap','glowcap_2'),('fungal_nursery','fungal_nursery')]:
  g.write_json(g.ASSETS/f'models/item/{name}.json',{'parent':f'wildercord:block/{parent}',**({'display':{'gui':{'rotation':[30,225,0],'translation':[0,4,0],'scale':[.45,.45,.45]},'firstperson_righthand':{'rotation':[0,45,0],'translation':[0,3,0],'scale':[.35,.35,.35]},'firstperson_lefthand':{'rotation':[0,225,0],'translation':[0,3,0],'scale':[.35,.35,.35]}}} if name=='fungal_nursery' else {})});g.write_json(g.ASSETS/f'items/{name}.json',{'model':{'type':'minecraft:model','model':f'wildercord:item/{name}'}})
 # A real held filter: stepped clay chamber, copper bands, leather grip and open vent gills.
 boxes=[([5,5,6],[11,12,10],'roof'),([6,12,6],[10,14,10],'copper'),([4,6,6],[5,11,10],'copper'),([11,6,6],[12,11,10],'copper'),([7,2,7],[9,5,9],'leather')]
 for y in [6,8,10]:boxes.append(([6,y,5.7],[10,y+.7,6.2],'gills'))
 model('item/cave_breather',{m:f'wildercord:block/fungal_{m}' for m in ['roof','copper','leather','gills']},boxes)
 path=g.ASSETS/'models/item/cave_breather.json'
 import json
 held=json.loads(path.read_text())
 held['display']={'gui':{'rotation':[20,-30,0],'translation':[0,0,0],'scale':[1.25,1.25,1.25]},'firstperson_lefthand':{'rotation':[0,0,-15],'translation':[0,1,0],'scale':[.7,.7,.7]},'firstperson_righthand':{'rotation':[0,0,15],'translation':[0,1,0],'scale':[.7,.7,.7]},'thirdperson_lefthand':{'rotation':[0,0,-15],'translation':[0,2,0],'scale':[.7,.7,.7]},'thirdperson_righthand':{'rotation':[0,0,15],'translation':[0,2,0],'scale':[.7,.7,.7]}}
 g.write_json(path,held)
 g.write_json(g.ASSETS/'items/cave_breather.json',{'model':{'type':'minecraft:model','model':'wildercord:item/cave_breather'}})
 for name,index in [('dried_glowcap_gills',-1),('breathmark_roots',0),('breathmark_air',1),('nursery_journal',2)]:
  im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
  if index<0:
   for shift in [0,4,8]:
    d.polygon([(5+shift,9),(10+shift,5),(16+shift,8),(20+shift,16),(15+shift,24),(9+shift,25),(4+shift,20)],fill='#7E917F');d.polygon([(6+shift,9),(11+shift,7),(15+shift,10),(18+shift,16),(14+shift,22),(9+shift,23),(6+shift,19)],fill='#C7CDB0')
    for dy in [11,14,17,20]:d.line((8+shift,dy,15+shift,dy+2),fill='#8C9C84');d.line((8+shift,dy-1,15+shift,dy+1),fill='#E1DAB7')
  else:
   d.polygon([(5,7),(22,3),(27,7),(27,25),(10,30),(5,26)],fill='#3D4B46');d.polygon([(9,9),(24,5),(26,8),(26,24),(10,28)],fill='#789182');d.line((7,9,7,25),fill='#D1B582',width=2);d.line((11,28,25,24),fill='#E6D4AC');d.line((11,27,25,23),fill='#A08D6A')
   for y in [10,14,18,22]:d.line((5,y,9,y),fill='#C2A96F')
   if index==0:d.line([(17,10),(17,20),(13,24),(17,20),(21,22)],fill='#DED1A3',width=2)
   elif index==1:d.arc((12,11,23,22),30,300,fill='#DCE3BC',width=2);d.line((12,17,20,17),fill='#DBE8C5')
   else:
    for x,y in [(14,13),(20,13),(17,20)]:d.ellipse((x-2,y-2,x+2,y+2),outline='#E0D8B3');d.line([(14,15),(17,18),(20,15)],fill='#C6DAAC')
  px=im.load()
  for y in range(32):
   for x in range(32):
    if px[x,y][3]:px[x,y]=(*shade(px[x,y],.88+noise(x,y,1251)*.18+(31-y)*.002),255)
  g.save(im,g.ASSETS/f'textures/item/{name}.png');g.write_json(g.ASSETS/f'models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'wildercord:item/{name}'}});g.write_json(g.ASSETS/f'items/{name}.json',{'model':{'type':'minecraft:model','model':f'wildercord:item/{name}'}})
 for name,ingredients in [('fungal_nursery',['minecraft:stick','minecraft:stick','minecraft:paper','wildercord:dried_glowcap_gills']),('cave_breather',['wildercord:mycelial_dew','wildercord:dried_glowcap_gills','minecraft:leather','minecraft:copper_ingot'])]:
  g.write_json(g.DATA/f'recipe/{name}.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':ingredients,'result':{'id':'wildercord:'+name}});g.unlock_advancement('wildercord:'+name,'wildercord:dried_glowcap_gills')
 for name in ['glowcap','fungal_nursery']:
  g.write_json(g.DATA/f'loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'wildercord:'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
 for name,rarity in [('glowcap_patch',3),('breathmark_site',4)]:
  g.write_json(g.DATA/f'worldgen/feature/{name}.json',{'type':'wildercord:'+name})
  placement=[{'type':'minecraft:count','count':2},{'type':'minecraft:rarity_filter','chance':rarity},{'type':'minecraft:in_square'},{'type':'minecraft:height_range','height':{'type':'minecraft:uniform','min_inclusive':{'absolute':-32},'max_inclusive':{'absolute':40}}}]
  if name=='breathmark_site': placement.append({'type':'minecraft:biome'})
  g.write_json(g.DATA/f'worldgen/placed_feature/{name}.json',{'feature':'wildercord:'+name,'placement':placement})
