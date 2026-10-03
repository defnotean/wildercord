"""Raised woven reed canopy and the Tideward apprentice's folded field journal."""
from PIL import Image,ImageDraw
LANG={
 'block.wildercord.reed_refuge':'Reed Refuge',
 'item.wildercord.reed_roof_notes':'Under the Reed Roof',
 'book.wildercord.reed_roof.1':'UNDER THE REED ROOF\n\nTamsin, Tideward apprentice\n\nIona taught me to weave a roof, never a cage. Leave four sides open. Set the posts in shallow water, where a newt can swim beneath the shade without scraping its gills.',
 'book.wildercord.reed_roof.2':'Three bamboo, floss, string and seagrass make the refuge. Newts visit in daylight or rain. One rests beneath a roof at a time: eyes close, gills settle, the tail curls. A rest is brief; seagrass or answering magic can wake the visitor.',
 'book.wildercord.reed_roof.3':'A roof offers no extra pearl and heals no wound. Give a frightened animal time and leave its gathering rest alone. Our older rule was simple: one shelter holds one visitor; a living bank holds many. Build another roof, rather than another cage.',
 'subtitles.wildercord.kit.wetland.refuge_settle':'Newt settles under reeds',
 'subtitles.wildercord.kit.wetland.refuge_wake':'Newt stirs',
}
def write(g):
 for name in ['roof','frame','thread']:
  im=Image.new('RGBA',(16,16));px=im.load()
  for y in range(16):
   for x in range(16):
    if name=='roof':
     base=(176,158,103) if x%4 in (1,2) else (112,126,78);f=.8+((x*31+y*17)%11)/40;c=tuple(min(255,int(v*f)) for v in base);c=(217,206,149) if y%5==0 and x%4!=0 else c
    elif name=='frame':c=(81+x%3*12,104+y%4*7,64+x%3*9);c=(186,177,108) if y in (3,12) else c
    else:c=(201+x%3*7,215+y%2*5,186+x%2*9);c=(111,132,112) if (x+y)%6==0 else c
    px[x,y]=(*c,255)
  g.save(im,g.ASSETS/f'textures/block/refuge_{name}.png')
 elements=[]
 def box(a,b,m):elements.append({'from':a,'to':b,'faces':{f:{'texture':'#'+m} for f in ['up','down','north','south','east','west']}})
 for x in [0,14]:
  for z in [0,14]:
   box([x,0,z],[x+2,13,z+2],'frame')
   for y in [2,9]:box([max(0,x-.15),y,max(0,z-.15)],[min(16,x+2.15),y+1,min(16,z+2.15)],'thread')
 for x in range(0,16,2):box([x,12,0],[x+2,14+(1 if x in (6,8) else 0),16],'roof')
 for z in [2,12]:box([0,14,z],[16,15,z+2],'thread')
 box([6,15,0],[10,16,16],'frame')
 g.write_json(g.ASSETS/'models/block/reed_refuge.json',{'parent':'minecraft:block/block','textures':{'roof':'wildercord:block/refuge_roof','frame':'wildercord:block/refuge_frame','thread':'wildercord:block/refuge_thread','particle':'wildercord:block/refuge_roof'},'elements':elements})
 g.write_json(g.ASSETS/'blockstates/reed_refuge.json',{'variants':{'':{'model':'wildercord:block/reed_refuge'}}})
 g.write_json(g.ASSETS/'models/item/reed_refuge.json',{'parent':'wildercord:block/reed_refuge'});g.write_json(g.ASSETS/'items/reed_refuge.json',{'model':{'type':'minecraft:model','model':'wildercord:item/reed_refuge'}})
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
 d.polygon([(4,7),(22,3),(28,7),(28,26),(10,30),(4,26)],fill='#344C3F');d.polygon([(9,9),(24,5),(27,8),(27,25),(10,29)],fill='#B4B78C');d.polygon([(11,10),(25,7),(25,24),(11,27)],fill='#567562')
 d.line((6,9,6,25),fill='#E4D5A3',width=2);d.line((10,29,25,25),fill='#F0E2BA');d.line([(13,18),(17,13),(22,16)],fill='#DDD5A3',width=2)
 for x,y in [(13,18),(22,16)]:d.line((x,y,x,y+6),fill='#C7D6BB');d.line((x+1,y,x+1,y+5),fill='#8DAD8B')
 d.ellipse((16,19,20,23),fill='#9AC0A0');d.line((15,26,23,24),fill='#D4C999');d.line([(23,5),(22,10),(24,11),(25,7)],fill='#D1DBCE')
 g.save(im,g.ASSETS/'textures/item/reed_roof_notes.png');g.write_json(g.ASSETS/'models/item/reed_roof_notes.json',{'parent':'minecraft:item/generated','textures':{'layer0':'wildercord:item/reed_roof_notes'}});g.write_json(g.ASSETS/'items/reed_roof_notes.json',{'model':{'type':'minecraft:model','model':'wildercord:item/reed_roof_notes'}})
 g.write_json(g.DATA/'loot_table/blocks/reed_refuge.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'wildercord:reed_refuge'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
 for name,ingredients in [('reed_refuge',['minecraft:bamboo','minecraft:bamboo','minecraft:bamboo','wildercord:moonreed_floss','minecraft:string','minecraft:seagrass']),('reed_roof_notes',['minecraft:book','wildercord:moonreed_floss','minecraft:seagrass'])]:
  g.write_json(g.DATA/f'recipe/{name}.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':ingredients,'result':{'id':'wildercord:'+name}});g.unlock_advancement('wildercord:'+name,'wildercord:moonreed_floss')
