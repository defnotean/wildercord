"""Four face-painted equipment skins, readable item icons, recipes and armor tags."""
from PIL import Image
from world_art import Sheet, box, noise
from item_art import hexc, ramp

PALETTES={
 'emberweave':ramp('#271719','#683227','#b5542d','#e99a48','#ffe0a0'),
 'rimebound':ramp('#182a43','#315779','#5590ad','#9dd9e8','#effcff'),
 'stonebound':ramp('#202522','#434c40','#737761','#a8aa86','#d8d2aa'),
 'mirror_thread':ramp('#242239','#514767','#8b769e','#c7b9dd','#f4ebff')}

def skin(kind):
 cv=Sheet(64,32);pal=PALETTES[kind]
 for u,v,w,h,d in [(0,0,8,8,8),(16,16,8,12,4),(40,16,4,12,4),(0,16,4,12,4)]:
  for face,(x0,y0,fw,fh) in box(u,v,w,h,d).items():
   for y in range(fh):
    for x in range(fw):
     edge=min(x,fw-1-x,y,fh-1-y);t=1+int(noise(x0+x,y0+y,81)*2)
     if edge==0:t=0 if y==fh-1 else 3
     if kind=='emberweave' and y>fh-5 and (x+y)%4<2:t=3+(y%2)
     if kind=='rimebound' and (x+y)%5==0:t=4
     if kind=='stonebound' and (x+(y//3)*2)%5==0:t=0
     if kind=='mirror_thread' and (x-y)%6<2:t=3
     if u==0 and v==0 and face=='front' and 2<=y<=4:continue # open face, hood/helmet surround
     if u==16 and face=='front' and 2<=x<=5 and 2<=y<=7:
      if kind=='emberweave':t=4 if x==3+y%2 else 2
      if kind=='rimebound':t=4 if abs(x-3)+abs(y-4)<=2 else 1
      if kind=='stonebound':t=3 if x%3 else 0
      if kind=='mirror_thread':t=4 if (x+y)%3==0 else 2
     cv.put(x0+x,y0+y,pal[t])
 return cv.image()

SHAPES={
 'helmet':['....########....','...##########...','...##......##...','...##......##...','...##......##...','...###....###...'],
 'chestplate':['..####....####..','..############..','...##########...','....########....','....########....','....########....','....########....','....########....','....########....'],
 'leggings':['....########....','....########....','....###..###....','....###..###....','....###..###....','....###..###....','....###..###....','....###..###....','....###..###....'],
 'boots':['...###....###...','...###....###...','...###....###...','..####....####..','..####....####..','..####....####..']}

def icon(kind,slot):
 im=Image.new('RGBA',(16,16));pal=PALETTES[kind];shape=SHAPES[slot];top=(16-len(shape))//2
 for y,row in enumerate(shape):
  for x,ch in enumerate(row):
   if ch!='#':continue
   edge=x==0 or row[x-1]!='#' or x==15 or row[x+1]!='#' or y==0 or y==len(shape)-1
   t=0 if edge else (3 if x<8 else 2)
   if not edge:
    if kind=='emberweave' and y>len(shape)-4 and (x+y)%3==0:t=4
    if kind=='rimebound' and (x-y)%4==0:t=4
    if kind=='stonebound' and (x+y*2)%5==0:t=1
    if kind=='mirror_thread' and (x+y)%3==0:t=4
   im.putpixel((x,top+y),pal[t])
 return im

def write(g):
 tags={s:[] for s in SHAPES}
 for kind in PALETTES:
  g.write_json(g.ASSETS/f'equipment/{kind}.json',{'layers':{layer:[{'texture':f'wildercord:{kind}'}] for layer in ['humanoid','humanoid_leggings']}})
  for layer in ['humanoid','humanoid_leggings']:g.save(skin(kind),g.ASSETS/f'textures/entity/equipment/{layer}/{kind}.png')
  for slot in SHAPES:
   if kind=='mirror_thread' and slot!='chestplate':continue
   path='mirror_thread_mantle' if kind=='mirror_thread' else f'{kind}_{slot}'
   tags[slot].append('wildercord:'+path)
   g.save(icon(kind,slot),g.ASSETS/f'textures/item/{path}.png');g.item_model(path,path)
   g.write_json(g.ASSETS/f'items/{path}.json',{'model':{'type':'minecraft:model','model':f'wildercord:item/{path}'}})
   material={'emberweave':'blaze_powder','rimebound':'packed_ice','stonebound':'mossy_cobblestone','mirror_thread':'echo_shard'}[kind]
   g.write_json(g.DATA/f'recipe/{path}.json',{'type':'minecraft:crafting_shapeless','category':'equipment',
    'ingredients':[f'minecraft:leather_{slot}',f'minecraft:{material}','wildercord:mana_crystal'], 'result':{'id':f'wildercord:{path}','count':1}})
   g.unlock_advancement('wildercord:'+path,f'minecraft:leather_{slot}')
 for slot,values in tags.items():
  tag={'helmet':'head_armor','chestplate':'chest_armor','leggings':'leg_armor','boots':'foot_armor'}[slot]
  g.write_json(g.RES/f'data/minecraft/tags/item/{tag}.json',{'replace':False,'values':values})
