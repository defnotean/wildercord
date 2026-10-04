"""Original mended lacquer fan: icon, physical held ribs and material-only particle sprites."""
from PIL import Image, ImageDraw
import math
from item_art import Canvas, hexc
LANG={
 'item.wildercord.rooks_rainshield':"Rook's Rainshield",
 'message.wildercord.rainshield.offer':'%s offers arrow cover. Release sneak, then crouch within two seconds to accept.',
 'item.wildercord.rooks_rainshield.lore':'One crooked rib bears the storm; the others hold the cloth.',
 'item.wildercord.rooks_rainshield.use':'Hold still and use for 0.6 seconds, then cover one vanilla arrow within a 70-degree frontal cone for two seconds. Right-click a same-team player within three blocks to offer cover. They must begin a new crouch within two seconds then hold Use on that same teammate to open; already-crouching players release and recrouch. Both need grounded feet; the teammate must keep crouching. One catch: four wear, fifteen-second shared rest and heavy hands. Five intact durability needed. Magic, tridents, melee and rear shots pass through. No reflection or arrow pickup.',
 **{'subtitles.wildercord.kit.rainshield.'+k:v for k,v in {'pivot':'Copper fan pivot clicks','open':'Mended cloth draws taut','catch':'Reed ribs catch an arrow'}.items()}
}

def grain(x,y,seed):return ((x*13+y*31+seed*17)^((x+y)*7))%9-4

# ============================================================== icon (16x16): the fan opened, as it is used

FAN_PIVOT=(3.5,12.5)
FAN_RADIUS=11.6
FAN_RIBS=5                       # four cloth panels between five reed ribs
FAN={'outline':'#2A160C','cloth':('#B8A274','#D4C092','#ECDCB0'),'hem':('#7A2A1C','#A8402A','#CC5E3C'),
     'rib':('#4A2C14','#7E5430'),'patch':('#5E6A3E','#7E8A56'),'stitch':'#F2E6C0',
     'pivot':('#8A4A26','#D07E48','#F4B07C'),'feather':('#141620','#262A3A','#4A5470'),'cord':'#C8A870'}


def icon():
 """The opened fan from its copper pivot: cream oiled-linen pleats on dark reed ribs, a lacquer-red hem, one panel
 mended with an olive patch, and the black rook feather hanging from the pivot."""
 cv=Canvas();px,py=FAN_PIVOT;step=90/(FAN_RIBS-1);C={k:([hexc(c) for c in v] if isinstance(v,tuple) else hexc(v)) for k,v in FAN.items()}
 for y in range(16):
  for x in range(16):
   dx,dy=x+.5-px,py-(y+.5);r=math.hypot(dx,dy);a=math.degrees(math.atan2(dy,dx))
   if r>FAN_RADIUS or a<-6 or a>96:continue
   k=min(FAN_RIBS-2,max(0,int(a//step)));inside=(a-k*step)/step
   if r<3.6:c=C['rib'][1]                                  # the gathered sticks round the pivot
   elif r>FAN_RADIUS-1.6:c=C['hem'][2 if k%2 else 1]
   elif k==2 and 5<r<8.4 and .15<inside<.85:c=C['patch'][1 if r<6.8 else 0]
   else:c=C['cloth'][2 if k%2 else 1]
   cv.put(x,y,c)
 # Inner ribs, one pixel wide, from the sticks out to the hem.
 for j in range(1,FAN_RIBS-1):
  t=math.radians(j*step);cx,cy=math.cos(t),-math.sin(t);n=max(abs(cx),abs(cy))
  for i in range(3,12):
   x,y=math.floor(px+cx/n*i*.999),math.floor(py+cy/n*i*.999)
   if math.hypot(x+.5-px,py-(y+.5))>FAN_RADIUS-1.6:break
   cv.put(x,y,C['rib'][0])
 # A few stitches round the mend.
 for x,y in [(8,7),(9,8),(7,5)]:
  if cv.get(x,y) in C['patch']:cv.put(x,y,C['stitch'])
 # Copper pivot.
 for (x,y),t in {(3,12):2,(4,12):1,(3,13):1,(4,13):0,(2,12):1,(3,11):1}.items():cv.put(x,y,C['pivot'][t])
 out=C['outline'];filled={(x,y) for y in range(16) for x in range(16) if cv.get(x,y) is not None}
 for y in range(16):
  for x in range(16):
   if (x,y) not in filled and any((x+dx,y+dy) in filled for dx,dy in ((1,0),(-1,0),(0,1),(0,-1))):cv.put(x,y,out)
 # The rook feather on its cord, hanging below the pivot.
 for (x,y),c in {(2,14):C['cord'],(1,14):out,(1,15):C['feather'][1],(0,15):C['feather'][0],(2,15):C['feather'][2],(3,15):out}.items():cv.put(x,y,c)
 return cv.image()

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

# ============================================================== held-model materials (16x16, vanilla-like)

MAT={'reed':('#3A2412','#5A3A1E','#7A522C','#9A6C3C'),'pivot':('#7A3E20','#A85C32','#D07E48','#F4B07C'),
     'cloth':('#B8A274','#D4C092','#ECDCB0','#F8EED0'),'feather':('#141620','#1E2230','#2C3244','#4A5470','#8A92A8'),
     'hem':('#7A2A1C','#A8402A','#CC5E3C'),'patch':('#5E6A3E','#7E8A56')}


def material(kind):
 """Clean 16x16 materials for the held fan: lacquered reed with nodes, a bevelled copper plate, linen with a red
 hem along its top (the ties use that strip) and a stitched olive mend, and a rook's black feather."""
 im=Image.new('RGBA',(16,16));px=im.load();T=[hexc(c) for c in MAT[kind]]
 for y in range(16):
  for x in range(16):
   if kind=='reed':
    t=[2,3,2,1,1,2,2,1,0,1,2,3,2,1,1,0][x]
    if y in (5,12):t=0
    elif y in (4,11):t=min(3,t+1)
    c=T[t]
   elif kind=='pivot':
    t=1
    if x==0 or y==0:t=3
    elif x==15 or y==15:t=0
    elif 5<=x<=10 and 5<=y<=10:t=0 if (x in (5,10) or y in (5,10)) else 2
    elif x+y in (7,8):t=2
    if (x,y) in ((6,6),(7,6),(6,7)):t=3
    c=T[t]
   elif kind=='cloth':
    t=2 if (x//4)%2==0 else 1
    if x%4==0:t=1 if t==2 else 0
    c=T[t]
    if y<2:c=hexc(MAT['hem'][2 if y==0 else 1])
    elif y==2:c=hexc(MAT['hem'][0])
    elif 6<=x<=11 and 6<=y<=11:
     c=hexc(MAT['patch'][1 if (x<11 and y<11) else 0])
     if (x in (6,11) or y in (6,11)) and (x+y)%2==0:c=T[3]
   else:
    # A feather down the middle: pale shaft, barbs angled back from it, a blue sheen on the lit side.
    d=abs(x-7.5)
    t=2 if x<8 else 1
    if (y+int(d))%3==0:t-=1
    if x<8 and (y+int(d))%3==2 and d<5:t=3
    if x in (0,15):t=0
    c=T[max(0,t)]
    if x in (7,8):c=T[4] if x==7 else T[3]
   px[x,y]=(*c,255)
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
 return {'textures':{'particle':'wildercord:item/rainshield_cloth',**{k:'wildercord:item/rainshield_'+k for k in ('reed','pivot','cloth','feather')}},'elements':elements,'display':{'firstperson_righthand':{'rotation':[0,-20,12],'translation':[.5,1,-1.5],'scale':[.65,.65,.65]},'thirdperson_righthand':{'rotation':[90,0,0],'translation':[0,2,0],'scale':[.7,.7,.7]},'firstperson_lefthand':{'rotation':[0,20,-12],'translation':[-.5,1,-1.5],'scale':[.65,.65,.65]},'thirdperson_lefthand':{'rotation':[90,0,0],'translation':[0,2,0],'scale':[.7,.7,.7]}}}

def write(g):
 g.save(icon(),g.ASSETS/'textures/item/rooks_rainshield.png');g.item_model('rooks_rainshield','rooks_rainshield')
 for kind in ('rib','cloth','splinter'):g.save(sprite(kind),g.ASSETS/f'textures/particle/rainshield_{kind}.png')
 for kind in ('reed','pivot','cloth','feather'):g.save(material(kind),g.ASSETS/f'textures/item/rainshield_{kind}.png')
 g.write_json(g.ASSETS/'models/item/rooks_rainshield_held.json',held())
 g.write_json(g.ASSETS/'items/rooks_rainshield.json',{'model':{'type':'minecraft:select','property':'minecraft:display_context','cases':[{'when':['gui','ground','fixed','on_shelf'],'model':{'type':'minecraft:model','model':'wildercord:item/rooks_rainshield'}}],'fallback':{'type':'minecraft:model','model':'wildercord:item/rooks_rainshield_held'}}})
 g.write_json(g.DATA/'recipe/rooks_rainshield.json',{'type':'minecraft:crafting_shapeless','category':'equipment','ingredients':['wildercord:windreed_braid','wildercord:moonreed_floss','minecraft:copper_ingot','minecraft:leather'],'result':{'id':'wildercord:rooks_rainshield','count':1}})
 g.unlock_advancement('wildercord:rooks_rainshield','wildercord:moonreed_floss')
