"""Original reed-feather/wading-foot textures. Independent256px atlas with explicit disjoint box islands."""
from wildlife_art import Skin,shade
from world_art import noise
import json
# x,y,width,height,depth,material; every model part has an explicit independent island.
BOXES=[(0,0,8,7,10,'plume'),(40,0,6,6,2,'breast'),(60,0,2,6,3,'neck'),(76,0,2,5,2,'neck'),(90,0,4,4,5,'head'),(114,0,1,1,6,'bill'),(132,0,1,1,4,'bill'),(148,0,1,2,3,'crest'),(160,0,1,1,1,'eye'),(168,0,1,1,1,'eye'),(0,24,2,7,8,'wing'),(24,24,2,7,8,'wing'),(138,24,4,1,5,'tail'),(158,24,3,1,4,'tail'),(68,48,2,3,1,'breast')]
BOXES += [(48+i*42+k*14,24,1,5,4,'flight') for i in range(2) for k in range(3)]
BOXES += [(i*8,48,1,4,1,'leg') for i in range(2)]+[(16+i*8,48,1,4,1,'leg') for i in range(2)]+[(32+i*16,48,3,1,4,'toe') for i in range(2)]
def validate_uv():
 seen=set()
 for x,y,w,h,d,_ in BOXES:
  cells={(u,v) for u in range(x,x+2*(w+d)) for v in range(y,y+h+d)}
  assert not cells&seen,(x,y,'overlap')
  assert max(u for u,v in cells)<256 and max(v for u,v in cells)<128
  seen|=cells
 return len(BOXES)
def skin():
 validate_uv();s=Skin(256,128)
 def paint(material):
  def sample(f,x,y,z,tx,ty):
   grain=noise(tx,ty,2517)
   if material=='eye':return (27,22,16) if f in ('front','left','right') else (181,144,67)
   if material in ('bill','leg','toe'):
    c=(148,126,76) if material=='bill' else (108,104,64)
    if int(y*2+z)%5==0:c=(194,172,104)
    if f in ('bottom','back'):c=(82,77,48)
    return shade(c,.83+grain*.26)
   if material in ('breast','neck'):
    c=(201,179,134) if int(x*2+z)%3 else (100,78,48)
    if f=='front' and int(y)%4==0:c=(231,212,165)
   elif material=='head':c=(161,128,72) if int(x+z)%3 else (84,63,42)
   elif material=='crest':c=(69,54,32) if int(z)%2 else (146,117,69)
   else:
    k=(int(z)+int(x*2))%5;c=(162,143,95) if k<3 else (93,79,50)
    if int(y+z)%4==0:c=(192,172,122)
    if material=='flight' and f in ('left','right'):c=(80,70,47) if int(z)%2 else (179,158,112)
    if material=='wing' and f=='top':c=(194,170,115)
   return shade(c,.86+grain*.24)
  return sample
 for x,y,w,h,d,material in BOXES:s.box(x,y,w,h,d,paint(material))
 return s.image()
def egg():
 from PIL import Image
 im=Image.new('RGBA',(16,16),(0,0,0,0));p=im.load()
 rows={2:(7,8),3:(6,9),4:(5,10),5:(4,11),6:(4,11),7:(3,12),8:(3,12),9:(3,12),10:(3,12),11:(3,12),12:(4,11),13:(4,11),14:(6,9)}
 for y,(a,z) in rows.items():
  for x in range(a,z+1):
   edge=x in (a,z) or y in (2,14);c=(68,56,35) if edge else (182,162,110)
   if not edge and (2*x+y)%5==0:c=(102,81,48)
   if not edge and x<7:c=(206,185,131)
   p[x,y]=(*c,255)
 for x,y in [(6,5),(6,6),(7,7),(8,8),(8,9),(9,10)]:p[x,y]=(64,52,32,255)
 return im
LANG={'entity.wildercord.siltcrest_bittern':'Siltcrest Bittern','item.wildercord.siltcrest_bittern_spawn_egg':'Siltcrest Bittern Spawn Egg','guide.wildercord.siltcrest_bittern.hint':'A quiet reed bird stalks wild cod and salmon at dusk. Crouch nearby; rain sends it toward a covered bank.','guide.wildercord.siltcrest_bittern':'The bittern slowly coils its neck before one short fish strike. It leaves a small pond with at least two eligible fish and rests its appetite after a meal. Bucket fish, named fish and persistent fish are excluded. Offer raw cod or salmon while crouching for close observation; feeding creates no material or breeding reward. An admitted Tidebreath from an actual ally can interrupt a hunt into visible preening; wild birds are not automatically spell allies. Dry cover two blocks above a shallow bank can shelter it during rain or daylight.','subtitles.wildercord.kit.bittern.boom':'Bittern throat booms','subtitles.wildercord.kit.bittern.catch':'Bittern bill clacks in water','subtitles.wildercord.kit.bittern.rustle':'Bittern feathers rustle'}
def write(root):
 resources=root;root=root/'assets/wildercord';(root/'textures/entity').mkdir(parents=True,exist_ok=True);skin().save(root/'textures/entity/siltcrest_bittern.png');(root/'textures/item').mkdir(parents=True,exist_ok=True);egg().save(root/'textures/item/siltcrest_bittern_spawn_egg.png');(root/'items').mkdir(parents=True,exist_ok=True);(root/'models/item').mkdir(parents=True,exist_ok=True)
 (root/'items/siltcrest_bittern_spawn_egg.json').write_text(json.dumps({'model':{'type':'minecraft:model','model':'wildercord:item/siltcrest_bittern_spawn_egg'}},indent=2)+'\n',encoding='utf-8',newline='\n')
 (root/'models/item/siltcrest_bittern_spawn_egg.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'wildercord:item/siltcrest_bittern_spawn_egg'}},indent=2)+'\n',encoding='utf-8',newline='\n')

 (resources/'data/wildercord/loot_table/entities').mkdir(parents=True,exist_ok=True)
 (resources/'data/wildercord/loot_table/entities/siltcrest_bittern.json').write_text(json.dumps({'type':'minecraft:entity','pools':[]},indent=2)+'\n',encoding='utf-8',newline='\n')
