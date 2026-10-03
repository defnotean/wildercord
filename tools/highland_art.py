"""Wind-carved fronds, braided field herbs and a stitched canvas descent kite."""
from PIL import Image, ImageDraw

LANG={
 'block.wildercord.windreed':'Windreed Tassels',
 'effect.wildercord.downwind':'Downwind',
 'message.wildercord.draft_kite.fuel':'The kite needs one Windreed Tassel in your inventory.',
 'item.wildercord.draft_kite':'Draft Kite',
 'item.wildercord.draft_kite.lore':'Stitched sailcloth, reed ribs and a runner\'s feather: a little borrowed lift.',
 'item.wildercord.draft_kite.use':'Use: 8 seconds of Slow Falling; spends one Windreed Tassel. Reusable, 20-second cooldown. Does not fly upward.',
 'item.wildercord.windreed_braid':'Windreed Braid',
 'item.wildercord.windreed_braid.lore':'The scent of the pass, wound through a patient knot.',
 'item.wildercord.windreed_braid.use':'Use: Downwind for 30 seconds, 10% slower movement. Calm approaches attract less wildlife attention; sprinting, close hare approaches and retaliation remain dangerous. Consumed.',
 'subtitles.wildercord.kit.highland.harvest':'Windreed tassels rustle',
 'subtitles.wildercord.kit.highland.kite':'Draft kite unfolds',
 'subtitles.wildercord.kit.highland.braid':'Herbal braid unwinds',
}

def write(g):
 for age in range(3):
  im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
  for n,(base,bend,tip) in enumerate([(8,4,11),(13,5,18),(18,6,23),(23,4,28)]):
   y=26-age*7+n%2
   stem=[(base,31),(base+1,25),(base+bend,y+4),(tip,y)]
   d.line(stem,fill='#354A3D',width=3);d.line([(x-1,y) for x,y in stem],fill='#96AB6F',width=1)
   d.polygon([(base,28),(base-5,23),(base-7,19),(base-1,23),(base+2,28)],fill='#5F8059')
   d.line((base-6,21,base,27),fill='#B2C486')
   if age:
    for k in range(3+age*2):
     px=tip-(k//2);py=y+k
     d.ellipse((px-3,py,px+1,py+2),fill='#5C6954');d.line((px-2,py,px,py),fill='#CDD0A0' if age==2 else '#A5B082')
    d.line((tip-1,y-2,tip-2,y+6),fill='#E8DEC0')
  g.save(im,g.ASSETS/f'textures/block/windreed_{age}.png')
  g.write_json(g.ASSETS/f'models/block/windreed_{age}.json',{'parent':'minecraft:block/cross','textures':{'cross':f'wildercord:block/windreed_{age}'}})
 g.write_json(g.ASSETS/'blockstates/windreed.json',{'variants':{f'age={i}':{'model':f'wildercord:block/windreed_{i}'} for i in range(3)}})
 for name in ('windreed','draft_kite','windreed_braid'):
  im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
  if name=='draft_kite':
   d.polygon([(15,2),(28,15),(18,26),(3,16)],fill='#293E4C');d.polygon([(15,4),(26,15),(18,24),(5,16)],fill='#75999D')
   d.polygon([(15,4),(15,16),(5,16)],fill='#CED3B5');d.polygon([(15,16),(26,15),(18,24)],fill='#BFC6A5')
   d.line([(15,3),(15,16),(18,25)],fill='#E3C590',width=2);d.line((4,16,27,15),fill='#604C35',width=2)
   for x,y in [(11,8),(8,12),(21,11),(24,14),(11,20),(20,21)]:d.line((x,y,x+1,y+1),fill='#E6E1C8')
   d.line([(18,25),(16,28),(21,30),(24,28)],fill='#E1CBA0');d.polygon([(23,27),(27,24),(29,27),(25,30)],fill='#537B8D');d.point((26,27),fill='#CCD5BF')
  elif name=='windreed_braid':
   d.line([(9,29),(8,23),(15,17),(12,12),(18,6)],fill='#34493C',width=7)
   for x,y in [(8,24),(12,19),(15,15),(13,11),(17,7)]:
    d.line((x-3,y+2,x+3,y-2),fill='#A7AD70',width=3);d.line((x-2,y+1,x+2,y-2),fill='#DFD3A3')
   d.ellipse((13,13,23,23),outline='#BA8550',width=3);d.line((17,14,21,21),fill='#F2D6A0')
   for x,y in [(18,4),(24,5),(25,11)]:d.polygon([(x,y+7),(x-3,y),(x+3,y+1)],fill='#78915B');d.line((x,y+7,x-1,y+2),fill='#CDD1A0')
  else:
   for n,x in enumerate((8,13,18,22)):
    d.line([(x,29),(x+1,18),(x+5,7)],fill='#435D43',width=3);d.line((x,28,x+4,9),fill='#B1BB81')
    for k in range(5):d.ellipse((x+1-k//2,5+k*2,x+7-k//2,8+k*2),fill='#8A9871');d.line((x+2-k//2,6+k*2,x+5-k//2,6+k*2),fill='#DED5A6')
   d.line((7,24,24,22),fill='#634831',width=3);d.line((8,24,23,22),fill='#D5B179')
  g.save(im,g.ASSETS/f'textures/item/{name}.png')
  g.write_json(g.ASSETS/f'models/item/{name}.json',{'parent':'minecraft:item/handheld' if name=='draft_kite' else 'minecraft:item/generated','textures':{'layer0':f'wildercord:item/{name}'}})
  g.write_json(g.ASSETS/f'items/{name}.json',{'model':{'type':'minecraft:model','model':f'wildercord:item/{name}'}})
 # Distinct status icon: a grass knot alongside a broken scent trail.
 im=Image.new('RGBA',(18,18));d=ImageDraw.Draw(im)
 d.line([(4,16),(5,11),(10,6),(13,2)],fill='#6F8D58',width=3)
 for x,y in [(5,12),(8,8),(11,4)]:d.line((x-2,y-1,x+2,y+1),fill='#D8CEA1',width=2)
 for x,y in [(13,13),(15,9),(16,4)]:d.line((x-1,y,x+1,y),fill='#AFC2C3')
 g.save(im,g.ASSETS/'textures/mob_effect/downwind.png')
 g.write_json(g.DATA/'loot_table/blocks/windreed.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'wildercord:windreed'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
 g.write_json(g.DATA/'tags/block/windreed_ground.json',{'replace':False,'values':['#minecraft:dirt','minecraft:grass_block']})
 for name,ingredients in [('draft_kite',['wildercord:windreed','wildercord:galeclaw_plume','minecraft:leather','minecraft:string','minecraft:stick']),('windreed_braid',['wildercord:windreed','minecraft:string','minecraft:sweet_berries'])]:
  g.write_json(g.DATA/f'recipe/{name}.json',{'type':'minecraft:crafting_shapeless','category':'equipment','ingredients':ingredients,'result':{'id':'wildercord:'+name}})
  g.unlock_advancement('wildercord:'+name,'wildercord:windreed')
 g.write_json(g.DATA/'worldgen/feature/windreed_patch.json',{'type':'minecraft:simple_block','to_place':{'id':'wildercord:windreed','properties':{'age':'2'}}})
 offset=lambda n:{'type':'minecraft:trapezoid','min':-n,'max':n,'plateau':0}
 g.write_json(g.DATA/'worldgen/placed_feature/windreed_patch.json',{'feature':'wildercord:windreed_patch','placement':[{'type':'minecraft:rarity_filter','chance':8},{'type':'minecraft:in_square'},{'type':'minecraft:heightmap','heightmap':'WORLD_SURFACE_WG'},{'type':'minecraft:biome'},{'type':'minecraft:count','count':24},{'type':'minecraft:offset','x':offset(5),'y':offset(2),'z':offset(5)},{'type':'minecraft:block_predicate_filter','predicate':{'type':'minecraft:all_of','predicates':[{'type':'minecraft:matching_block_tag','tag':'minecraft:air'},{'type':'minecraft:matching_block_tag','tag':'wildercord:windreed_ground','offset':[0,-1,0]}]}}]})
