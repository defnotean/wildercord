"""Layered mudstone shell, reeds, bronze pincers and exact articulated-crab UVs."""
from PIL import Image,ImageDraw
from wildlife_art import Skin,mix,shade
from world_art import noise
LANG={
 'entity.wildercord.reedback_crab':'Reedback Crab',
 'item.wildercord.reedback_crab_spawn_egg':'Reedback Crab Spawn Egg',
 'guide.wildercord.reedback_crab':'A territorial keeper of shallow swamp banks. Raised claws and trembling reeds warn before a fixed forward sweep; step sideways and answer during its recovery. Sneak past outside its close personal space. Tidebreath calms it briefly; wind interrupts its claws. Its response rest survives reloading. It leaves small pieces of wet-bank clay, useful in fieldcraft. Peaceful worlds suppress aggression.',
 'guide.wildercord.reedback_crab.hint':'Watch low, open swamp banks near water. A reed crown on a mudstone shell may walk sideways.',
 **{'subtitles.wildercord.kit.wetland.crab_'+n:t for n,t in [('call','Reedback clicks'),('warn','Reedback raises claws'),('sweep','Claws sweep'),('calm','Reedback settles'),('stagger','Reed crown rattles'),('hurt','Shell knocks'),('death','Shell falls still')]},
}
def skin():
 s=Skin(128,64)
 def shell(f,x,y,z,tx,ty):
  c=mix((70,83,57),(157,156,111),max(0,min(1,y/5)))
  if f=='top':
   c=(74,103,62) if int(x+z*2)%7<2 else (135,129,87)
   if int(x)%4==0:c=(181,166,110)
  if f=='bottom':c=(86,65,47)
  return shade(c,.84+noise(tx,ty,853)*.24)
 s.box(0,0,12,5,10,shell);s.box(0,20,14,2,12,lambda f,x,y,z,tx,ty:shade((96,74,51),.85+noise(tx,ty,872)*.3))
 s.box(60,0,1,3,1,lambda f,x,y,z,tx,ty:(120,137,85))
 s.box(66,0,2,1,2,lambda f,x,y,z,tx,ty:(26,37,29) if f=='front' else (178,172,98))
 for u,v,w,h,d in [(64,12,5,3,7),(92,12,2,2,5),(54,28,2,2,6),(0,38,2,2,7)]:
  s.box(u,v,w,h,d,lambda f,x,y,z,tx,ty:shade(mix((100,63,37),(184,137,75),max(0,min(1,y/3))),.83+noise(tx,ty,884)*.3))
 s.box(22,40,1,7,1,lambda f,x,y,z,tx,ty:(107,142,67) if int(y)%3 else (174,174,95))
 s.box(30,40,2,3,2,lambda f,x,y,z,tx,ty:shade((137,105,63),.85+noise(tx,ty,831)*.25))
 return s.image()
def write(g):
 g.save(skin(),g.ASSETS/'textures/entity/wildlife/reedback_crab.png')
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im);d.ellipse((7,3,25,29),fill='#384734');d.ellipse((8,4,24,27),fill='#8A9666');d.ellipse((10,6,21,25),fill='#BAC18A');d.polygon([(11,13),(19,10),(22,15),(18,21),(10,20)],fill='#6F6546');d.line([(11,20),(8,22),(10,25)],fill='#B58046',width=2);d.line([(20,18),(24,19),(22,23)],fill='#B58046',width=2);d.line((15,15,14,8),fill='#487F4A',width=2);d.line((18,14,19,6),fill='#487F4A',width=2)
 g.save(im,g.ASSETS/'textures/item/reedback_crab_spawn_egg.png');g.write_json(g.ASSETS/'models/item/reedback_crab_spawn_egg.json',{'parent':'minecraft:item/generated','textures':{'layer0':'wildercord:item/reedback_crab_spawn_egg'}});g.write_json(g.ASSETS/'items/reedback_crab_spawn_egg.json',{'model':{'type':'minecraft:model','model':'wildercord:item/reedback_crab_spawn_egg'}})
 g.write_json(g.DATA/'loot_table/entities/reedback_crab.json',{'type':'minecraft:entity','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'minecraft:clay_ball','functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':1,'max':3}}]}]}]})
