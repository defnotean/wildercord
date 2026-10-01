"""Small panels of hand-designed clockwork, a rooted heart and a wind anchor, with five visible stages."""
from PIL import Image, ImageDraw
from item_art import ramp

def write(g):
 for path,kind,pal in [
  ('clockwork_control','clock',ramp('#241c24','#5d4933','#ac8551','#e6bb68','#fff3bf')),
  ('greenhouse_heart','garden',ramp('#172d23','#354c31','#587744','#95b957','#e6eda3')),
  ('sky_anchor','sky',ramp('#1b243d','#394b68','#789daa','#bfdade','#f2fafb'))]:
  side=Image.new('RGBA',(16,16),pal[1]);d=ImageDraw.Draw(side)
  d.rectangle((0,0,15,15),outline=pal[0]);d.line((1,1,14,1),fill=pal[3]);d.line((1,14,14,14),fill=pal[0])
  for x in (2,13):
   for y in (3,12):d.rectangle((x,y,x+1,y+1),fill=pal[3])
  for y in range(4,12,3):d.line((4,y,11,y),fill=pal[0]);d.line((4,y+1,11,y+1),fill=pal[2])
  g.save(side,g.ASSETS/f'textures/block/{path}_side.png')
  variants={}
  for stage in range(5):
   im=Image.new('RGBA',(16,16),pal[1]);draw=ImageDraw.Draw(im);draw.rectangle((0,0,15,15),outline=pal[0]);draw.rectangle((1,1,14,14),outline=pal[3])
   if kind=='clock':
    draw.ellipse((3,3,12,12),fill=pal[0],outline=pal[3]);draw.line((8,4,8,8),fill=pal[4]);draw.line((8,8,11,8+stage//2),fill=pal[4]);draw.point((8,11),fill=pal[2])
   elif kind=='garden':
    draw.line((8,13,8,7),fill=pal[0],width=2);draw.line((8,11,4,9),fill=pal[2]);draw.line((8,10,12,8),fill=pal[3])
    draw.polygon([(8,9),(4,6),(5,3),(7,3),(8,5),(9,3),(11,3),(12,6)],fill=('#8f3b59' if stage<3 else pal[4]),outline=pal[0])
   else:
    for y in (4,8,12):draw.line([(3,y),(6,y-1),(10,y+1),(13,y)],fill=pal[4] if stage%2 else pal[3]);draw.point((13,y-1),fill=pal[4])
   for i in range(stage):draw.point((3+i*3,14),fill=pal[4])
   g.save(im,g.ASSETS/f'textures/block/{path}_{stage}.png')
   g.write_json(g.ASSETS/f'models/block/{path}_{stage}.json',{'parent':'minecraft:block/cube','textures':{
    'particle':f'wildercord:block/{path}_side','down':f'wildercord:block/{path}_side','up':f'wildercord:block/{path}_{stage}',
    **{f:f'wildercord:block/{path}_side' for f in ['north','south','west','east']}}})
   for facing,angle in [('north',0),('east',90),('south',180),('west',270)]:variants[f'facing={facing},stage={stage}']={'model':f'wildercord:block/{path}_{stage}','y':angle}
  g.write_json(g.ASSETS/f'blockstates/{path}.json',{'variants':variants})
