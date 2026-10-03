"""An emergent bank flower, fine lunar floss and a brass field lens."""
from PIL import Image,ImageDraw
LANG={
 'block.wildercord.moonreed':'Moonreed',
 'item.wildercord.moonreed_floss':'Moonreed Floss',
 'item.wildercord.moonreed_floss.lore':'Soft silver fibres from a flower opened by moth wings.',
 'item.wildercord.moonreed_floss.use':'Harvest a blooming Moonreed without breaking its root. Floss binds a Dewglass Lens.',
 'item.wildercord.dewglass_lens':'Dewglass Lens',
 'item.wildercord.dewglass_lens.lore':'A brass field instrument whose thread remembers the moon.',
 'item.wildercord.dewglass_lens.use':'Use to locate a visible Moonreed within eight blocks; use on reeds or Lantern Newts to inspect them. Five-second rest; 96 uses.',
 'message.wildercord.dewglass.none':'No Moonreed is visible nearby. Search open swamp banks.',
 'message.wildercord.dewglass.dry':'This root needs water beside it, or rain.',
 'message.wildercord.dewglass.covered':'Moonreed needs an open sky to bloom.',
 'message.wildercord.dewglass.day':'Moonreed prepares its buds under a wetland night.',
 'message.wildercord.dewglass.growing':'A damp root is preparing a bud. Life magic can help.',
 'message.wildercord.dewglass.moth':'The bud awaits a visiting Glimmerwing.',
 'message.wildercord.dewglass.ready':'The flower is open. Gather its floss; leave the root.',
 'message.wildercord.dewglass.newt_dry':'This newt needs water or rain before gathering.',
 'message.wildercord.dewglass.newt_hurt':'This newt is frightened. Give it time to settle.',
 'message.wildercord.dewglass.newt_rest':'This newt is gathering light: about %s seconds remain.',
 'message.wildercord.dewglass.newt_ready':'This wet newt is ready for an offering of Seagrass.',
 'subtitles.wildercord.kit.wetland.reed_open':'Moonreed opens',
 'subtitles.wildercord.kit.wetland.reed_harvest':'Moonreed floss rustles',
 'subtitles.wildercord.kit.wetland.lens_focus':'Dewglass focuses',
}
def write(g):
 g.write_json(g.DATA/'tags/block/moonreed_ground.json', {'replace':False,'values':['#minecraft:dirt','minecraft:grass_block','minecraft:mud','minecraft:muddy_mangrove_roots','minecraft:clay','minecraft:moss_block','minecraft:podzol']})
 for age in range(3):
  im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
  for x,top in [(8,14-age*5),(17,9-age*3),(24,17-age*4)]:
   d.line([(x-2,31),(x,23),(x,top)],fill='#345E50',width=3);d.line((x,30,x+1,top),fill='#B8CEA2')
   for y,side in [(24,-1),(20,1)]:
    d.polygon([(x,y+5),(x+side*8,y-2),(x+side*3,y-1)],fill='#658F6D');d.line((x,y+4,x+side*6,y),fill='#B6CCA1')
   if age==0:d.ellipse((x-2,top-2,x+2,top+3),fill='#658364');d.line((x,top,x,top+2),fill='#D2CF9E')
   elif age==1:d.ellipse((x-3,top-2,x+3,top+4),fill='#7A8573');d.line((x,top-1,x,top+3),fill='#DFDABD',width=2)
   else:
    for dx,dy in [(-4,0),(4,0),(0,-3),(0,3)]:d.ellipse((x+dx-3,top+dy-2,x+dx+3,top+dy+2),fill='#A7BFA9');d.line((x+dx-1,top+dy,x+dx+1,top+dy),fill='#E5EDD2')
    d.ellipse((x-2,top-2,x+2,top+2),fill='#DACD7F');d.point((x-1,top-1),fill='#FFF1BC')
  g.save(im,g.ASSETS/f'textures/block/moonreed_{age}.png');g.write_json(g.ASSETS/f'models/block/moonreed_{age}.json',{'parent':'minecraft:block/cross','textures':{'cross':f'wildercord:block/moonreed_{age}'},'render_type':'minecraft:cutout'})
 g.write_json(g.ASSETS/'blockstates/moonreed.json',{'variants':{f'age={a}':{'model':f'wildercord:block/moonreed_{a}'} for a in range(3)}})
 for name in ['moonreed','moonreed_floss','dewglass_lens']:
  im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
  if name=='moonreed':
   im=Image.open(g.ASSETS/'textures/block/moonreed_1.png').convert('RGBA')
  elif name=='moonreed_floss':
   d.polygon([(5,25),(8,13),(18,8),(27,14),(24,25),(15,29)],fill='#536D67')
   for x,y in [(7,18),(11,13),(16,12),(21,15),(9,23),(16,21),(22,23)]:
    d.arc((x-2,y-3,x+6,y+5),185,390,fill='#B3CEC7',width=2);d.line((x,y,x+3,y-2),fill='#EFF1CF')
   d.line((8,24,24,15),fill='#8F7547',width=3);d.line((9,24,23,16),fill='#DEC28A')
  else:
   d.line((8,28,16,19),fill='#473D30',width=6);d.line((8,27,15,20),fill='#B99557',width=3)
   d.ellipse((10,2,29,22),fill='#514635');d.ellipse((11,3,28,21),fill='#D5B479');d.ellipse((13,5,26,19),fill='#537D72');d.ellipse((14,6,25,17),fill='#82B5A2');d.arc((14,6,25,17),190,275,fill='#ECF4D6',width=2);d.line((20,7,23,9),fill='#D8E6CE');d.ellipse((17,11,20,14),fill='#D5D8A1');d.line((11,25,17,27),fill='#B5C9BA');d.point((26,5),fill='#FFF0BE')
  g.save(im,g.ASSETS/f'textures/item/{name}.png');g.write_json(g.ASSETS/f'models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'wildercord:item/{name}'}});g.write_json(g.ASSETS/f'items/{name}.json',{'model':{'type':'minecraft:model','model':f'wildercord:item/{name}'}})
 g.write_json(g.DATA/'loot_table/blocks/moonreed.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'wildercord:moonreed'}],'conditions':[{'condition':'minecraft:survives_explosion'}]},{'rolls':1,'entries':[{'type':'minecraft:item','name':'wildercord:moonreed_floss'}],'conditions':[{'condition':'minecraft:survives_explosion'},{'condition':'minecraft:match_block','blocks':'wildercord:moonreed','state':{'age':'2'}}]}]})
 g.write_json(g.DATA/'recipe/dewglass_lens.json',{'type':'minecraft:crafting_shapeless','category':'equipment','ingredients':['wildercord:moonreed_floss','wildercord:dusk_pearl','minecraft:copper_ingot','minecraft:glass'],'result':{'id':'wildercord:dewglass_lens'}});g.unlock_advancement('wildercord:dewglass_lens','wildercord:moonreed_floss')
 g.write_json(g.DATA/'worldgen/feature/moonreed_patch.json',{'type':'wildercord:moonreed_patch'})
 g.write_json(g.DATA/'worldgen/placed_feature/moonreed_patch.json',{'feature':'wildercord:moonreed_patch','placement':[{'type':'minecraft:rarity_filter','chance':6},{'type':'minecraft:in_square'},{'type':'minecraft:heightmap','heightmap':'WORLD_SURFACE_WG'},{'type':'minecraft:biome'}]})

 # Brass octagon, recessed lens, angled grip and two tied floss bands: a real held instrument.
 for material in ['brass','glass','grip','thread']:
  im=Image.new('RGBA',(16,16));px=im.load()
  for y in range(16):
   for x in range(16):
    if material=='brass':
     n=((x*19+y*31)%13)/13;c=tuple(int(v*(.77+n*.16)) for v in (211,170,99));c=(243,214,148) if x in (1,14) or y in (1,14) else c;c=(91,76,48) if x in (4,11) and y%4==0 else c
    elif material=='glass':
     shade=min(1,((x-6)**2+(y-6)**2)**.5/13);c=tuple(int(a+(b-a)*shade) for a,b in zip((173,220,196),(55,98,91)));c=(237,247,219) if abs(x+y-8)<2 and x<9 else c
    elif material=='grip':c=(105+(x%3)*9, 67+(x%3)*7,42+(y%4)*3)
    else:c=(203+(x%3)*7,217+(y%3)*5,192+(x%2)*9)
    px[x,y]=(*c,255)
  g.save(im,g.ASSETS/f'textures/item/dewglass_{material}.png')
 elements=[]
 def box(a,b,material,rotate=False):
  e={'from':a,'to':b,'faces':{f:{'texture':'#'+material,'uv':[0,0,16,16]} for f in ['up','down','north','south','east','west']}}
  if rotate:e['rotation']={'origin':[6,7,8],'axis':'z','angle':-22.5}
  elements.append(e)
 for a,b in [([7,13,7],[13,15,9]),([7,5,7],[13,7,9]),([5,7,7],[7,13,9]),([13,7,7],[15,13,9]),([6,12,7],[8,14,9]),([12,12,7],[14,14,9]),([6,6,7],[8,8,9]),([12,6,7],[14,8,9])]:box(a,b,'brass')
 box([7,7,7.3],[13,13,7.7],'glass');box([4,1,7],[6,7,9],'grip',True);box([4,6,6.8],[6.3,7.5,9.2],'brass',True)
 for y in [2,4]:box([3.8,y,6.8],[6.2,y+.65,9.2],'thread',True)
 g.write_json(g.ASSETS/'models/item/dewglass_lens.json',{'parent':'minecraft:item/handheld','gui_light':'front','textures':{m:f'wildercord:item/dewglass_{m}' for m in ['brass','glass','grip','thread']},'elements':elements,'display':{'gui':{'rotation':[15,-20,-20],'translation':[0,0,0],'scale':[.85,.85,.85]},'firstperson_righthand':{'rotation':[0,-18,8],'translation':[0,0,0],'scale':[.62,.62,.62]},'firstperson_lefthand':{'rotation':[0,18,-8],'translation':[0,0,0],'scale':[.62,.62,.62]}}})
