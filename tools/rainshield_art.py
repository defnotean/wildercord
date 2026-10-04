"""Original mended lacquer fan: icon, physical held ribs and material-only particle sprites."""
from PIL import Image, ImageDraw
import math
LANG={
 'item.wildercord.rooks_rainshield':"Rook's Rainshield",
 'message.wildercord.rainshield.offer':'%s offers arrow cover. Release sneak, then crouch within two seconds to accept.',
 'item.wildercord.rooks_rainshield.lore':'One crooked rib bears the storm; the others hold the cloth.',
 'item.wildercord.rooks_rainshield.use':'Hold still and use for 0.6 seconds, then cover one vanilla arrow within a 70-degree frontal cone for two seconds. Right-click a same-team player within three blocks to offer cover. They must begin a new crouch within two seconds then hold Use on that same teammate to open; already-crouching players release and recrouch. Both need grounded feet; the teammate must keep crouching. One catch: four wear, fifteen-second shared rest and heavy hands. Five intact durability needed. Magic, tridents, melee and rear shots pass through. No reflection or arrow pickup.',
 **{'subtitles.wildercord.kit.rainshield.'+k:v for k,v in {'pivot':'Copper fan pivot clicks','open':'Mended cloth draws taut','catch':'Reed ribs catch an arrow'}.items()}
}

def grain(x,y,seed):return ((x*13+y*31+seed*17)^((x+y)*7))%9-4

def icon():
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
 d.polygon([(7,27),(10,13),(17,3),(24,4),(26,10),(21,20),(14,29)],fill='#28282A')
 # Unequal folded cloth panels and dark pleats define a small mended artifact silhouette.
 for i,poly in enumerate([[(10,25),(11,14),(18,4),(21,5),(17,18)],[(12,26),(17,14),(22,5),(24,7),(20,20)],[(14,27),(19,19),(25,8),(24,13),(20,24)]]):
  d.polygon(poly,fill=['#C5B993','#998A65','#D6C9A0'][i]);d.line(poly+[poly[0]],fill='#4A4234',width=1)
  for y in range(6,26):
   for x in range(10,25):
    if im.getpixel((x,y))[:3] in ((197,185,147),(153,138,101),(214,201,160)) and (x+y+i)%4==0:d.point((x,y),fill='#B1A278')
 for start,end in [((10,27),(18,4)),((11,28),(22,5)),((13,28),(25,8))]:
  d.line((start,end),fill='#3E3326',width=2);d.line((start[0],start[1]-1,end[0]-1,end[1]),fill='#C28E4D')
 d.polygon([(16,15),(21,14),(22,18),(17,20)],fill='#7D856A');d.line([(16,15),(21,14),(22,18),(17,20),(16,15)],fill='#DCD4AE')
 for x,y in [(17,16),(19,15),(21,16),(20,18),(18,19)]:d.point((x,y),fill='#444132')
 d.ellipse((8,25,15,31),fill='#3E3225',outline='#D6A164');d.ellipse((10,27,13,29),fill='#84603C');d.point((10,26),fill='#F1D2A2')
 d.line([(9,29),(5,27),(3,20)],fill='#BCAC75');d.polygon([(3,20),(1,17),(2,11),(5,15),(5,20)],fill='#272B35');d.line((2,13,4,19),fill='#737880');d.point((3,16),fill='#B8B29B')
 return im

def sprite(kind):
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
 if kind=='rib':
  # Thin material shaft; lower hinge and upper split terminate at real fan endpoints.
  d.polygon([(14,31),(13,8),(13,2),(15,0),(17,1),(18,6),(17,31)],fill='#453125')
  d.line((14,3,15,29),fill='#C29656',width=1);d.line((16,3,17,27),fill='#E0B77A')
  d.line((13,9,17,9),fill='#77603D');d.line((14,20,17,20),fill='#77603D')
  d.ellipse((14,28,17,31),fill='#D1A069',outline='#554534');d.point((15,29),fill='#F1D8A4')
 elif kind=='cloth':
  # Tapered radial gore: broad scalloped outer hem, narrow bottom at the shared copper pivot.
  edge=[(7,3),(10,1),(15,0),(20,1),(24,3),(17,29),(16,31),(15,29)]
  inside=[(9,4),(12,2),(16,2),(20,3),(22,4),(16,28)]
  d.polygon(edge,fill='#504633');d.polygon(inside,fill='#C5B894')
  for y in range(2,29):
   for x in range(8,24):
    if im.getpixel((x,y))[:3]==(197,184,148):
     fold=8 if x<16 else -10
     d.point((x,y),fill=(max(0,min(255,187+fold+grain(x,y,83))),max(0,min(255,178+fold+grain(x,y,42))),max(0,min(255,137+fold+grain(x,y,12))),255))
  d.line([(16,28),(16,3)],fill='#897750');d.line([(15,26),(15,3)],fill='#DED0A7')
  # A small worn stitched repair, rather than a large repeating face-sized square.
  d.polygon([(13,7),(18,6),(19,10),(14,12)],fill='#798269')
  d.line([(13,7),(18,6),(19,10),(14,12),(13,7)],fill='#E1D3A6')
  for x,y in [(13,8),(15,7),(18,8),(17,11),(14,10)]:d.point((x,y),fill='#4D4B37')
  for t in (.14,.28,.42,.56,.70,.84):
   for x0 in (9,22):
    x=round(16+(x0-16)*(1-t));y=round(4+24*t)
    d.line((x,y,x+(-1 if x0<16 else 1),y+1),fill='#F0DEAE')
  d.line([(9,4),(13,3),(17,3),(21,4)],fill='#EAD7A5')
 else:
  d.line([(5,25),(11,18),(13,9)],fill='#D2B487',width=3);d.line((7,25,14,11),fill='#584431');d.polygon([(13,8),(11,4),(14,1),(16,5)],fill='#7B7F7D');d.line([(20,12),(25,8),(27,10)],fill='#B7A27A',width=2);d.point((26,8),fill='#EEE0BB')
 return im

def material(kind):
 im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
 base={'reed':(95,66,42),'pivot':(153,106,61),'cloth':(191,179,137),'feather':(43,47,57)}[kind]
 for y in range(16):
  for x in range(16):
   light=grain(x,y,71)+(8 if x==2 else -9 if x>12 else 0)
   if kind=='reed':light+=(9 if x%5==1 else -7 if x%5==0 else 0)
   elif kind=='pivot':light+=int((7-abs(x-6))*3)-y
   else:light+=4 if (x+y)%3==0 else -2
   d.point((x,y),fill=tuple(max(0,min(255,v+light)) for v in base)+(255,))
 if kind=='cloth':
  d.rectangle((7,4,12,10),fill='#7B856A');d.rectangle((6,3,13,11),outline='#E9DBAE')
  for y in (4,7,10):d.line((6,y,7,y+1),fill='#514D3D');d.line((12,y,13,y+1),fill='#514D3D')
 elif kind=='pivot':d.ellipse((3,4,11,12),outline='#E0B47A');d.ellipse((5,6,9,10),fill='#725032');d.point((4,5),fill='#F2D8A9')
 elif kind=='feather':
  d.line((7,0,8,15),fill='#B5AD90',width=1)
  for y in range(2,15,3):d.line((2,y-2,7,y),fill='#69717D');d.line((8,y,13,y-2),fill='#5B646F')
  d.point((6,4),fill='#9297A1');d.point((10,9),fill='#858B95')
 return im

def held():
 # Folded handheld fan: substantial three-faceted linen, seven exposed unequal reed ends.
 # The opened fan is a separate real-use material effect, not this item's idle state.
 elements=[]
 def box(lo,hi,kind,linen=False):
  faces={f:{'texture':'#'+kind} for f in ('north','south','east','west','up','down')}
  if kind=='cloth':
   # Material coordinates cover the WHOLE folded linen, rather than a model-coordinate crop of its repair.
   # Twenty-one stepped pleat faces share a continuous woven field; narrow ties use the plain hem strip.
   if linen:
    u0=max(0,min(16,(lo[0]-5.9)*16/4.25));u1=max(0,min(16,(hi[0]-5.9)*16/4.25))
    v0=max(0,min(16,(12.65-hi[1])*16/9.55));v1=max(0,min(16,(12.65-lo[1])*16/9.55))
    faces['north']['uv']=[16-u1,v0,16-u0,v1]
    faces['south']['uv']=[u0,v0,u1,v1]
    for f in ('east','west'):faces[f]['uv']=[0,v0,2,v1]
    for f in ('up','down'):faces[f]['uv']=[0,0,16,2]
   else:
    for face in faces.values():face['uv']=[0,0,16,2]
  elements.append({'from':lo,'to':hi,'faces':faces})
 # Three pleats converge toward the copper foot, and widen into a compact closed bundle.
 for target,width,front,back,top in [(6.55,1.25,7.0,8.3,12.25),(8.0,1.55,6.82,8.08,12.65),(9.43,1.25,7.02,8.32,12.15)]:
  y=3.1
  while y<top:
   high=min(top,y+1.5);t=(high-3.1)/(top-3.1)
   centre=8+(target-8)*t;half=.16+(width*.5-.16)*t
   box([centre-half,y,front],[centre+half,high,back],'cloth',True)
   y=high
 # Real ribs lie on alternating front/back fold crests, leaving the linen face exposed.
 for x,z,top in [(6.12,6.72,12.35),(6.76,8.29,12.7),(7.34,6.64,13.0),(7.96,8.1,13.32),(8.58,6.64,12.95),(9.18,8.3,12.65),(9.78,6.76,12.25)]:
  box([x,2.8,z],[x+.22,top,z+.24],'reed')
 # Root binding physically joins the folded ribs to the original compact central hinge.
 box([6.05,2.42,7.06],[10.02,3.13,8.34],'reed')
 box([6.13,4.75,6.7],[9.9,5.0,8.55],'cloth')
 box([7.03,1.05,7.08],[9.14,2.5,8.18],'pivot')
 box([7.3,.72,7.34],[8.88,1.24,8.0],'reed')
 box([7.77,1.34,6.83],[8.34,1.97,7.12],'pivot')
 box([7.77,1.34,8.15],[8.34,1.97,8.44],'pivot')
 # Original off-centre cord, quilled rook feather and stepped silhouette remain.
 box([6.72,1.02,7.5],[7.06,1.42,7.83],'cloth')
 box([6.36,.5,7.5],[6.67,1.48,7.79],'cloth')
 box([5.62,.15,7.44],[6.33,1.18,7.79],'feather')
 box([5.37,1.1,7.44],[6.1,2.23,7.79],'feather')
 box([5.57,2.18,7.44],[6.02,2.95,7.79],'feather')
 box([5.82,.05,7.36],[5.99,2.74,7.49],'reed')
 return {'textures':{k:'wildercord:item/rainshield_'+k for k in ('reed','pivot','cloth','feather')},'elements':elements,'display':{'firstperson_righthand':{'rotation':[0,-20,12],'translation':[.5,1,-1.5],'scale':[.65,.65,.65]},'thirdperson_righthand':{'rotation':[90,0,0],'translation':[0,2,0],'scale':[.7,.7,.7]},'firstperson_lefthand':{'rotation':[0,20,-12],'translation':[-.5,1,-1.5],'scale':[.65,.65,.65]},'thirdperson_lefthand':{'rotation':[90,0,0],'translation':[0,2,0],'scale':[.7,.7,.7]}}}

def write(g):
 g.save(icon(),g.ASSETS/'textures/item/rooks_rainshield.png');g.item_model('rooks_rainshield','rooks_rainshield')
 for kind in ('rib','cloth','splinter'):g.save(sprite(kind),g.ASSETS/f'textures/particle/rainshield_{kind}.png')
 for kind in ('reed','pivot','cloth','feather'):g.save(material(kind),g.ASSETS/f'textures/item/rainshield_{kind}.png')
 g.write_json(g.ASSETS/'models/item/rooks_rainshield_held.json',held())
 g.write_json(g.ASSETS/'items/rooks_rainshield.json',{'model':{'type':'minecraft:select','property':'minecraft:display_context','cases':[{'when':['gui','ground','fixed','on_shelf'],'model':{'type':'minecraft:model','model':'wildercord:item/rooks_rainshield'}}],'fallback':{'type':'minecraft:model','model':'wildercord:item/rooks_rainshield_held'}}})
 g.write_json(g.DATA/'recipe/rooks_rainshield.json',{'type':'minecraft:crafting_shapeless','category':'equipment','ingredients':['wildercord:windreed_braid','wildercord:moonreed_floss','minecraft:copper_ingot','minecraft:leather'],'result':{'id':'wildercord:rooks_rainshield','count':1}})
 g.unlock_advancement('wildercord:rooks_rainshield','wildercord:moonreed_floss')
