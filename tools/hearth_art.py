"""Carved brass hearth, with inset glass, braided roots and visible charge crystals."""
from PIL import Image,ImageDraw
def write(g):
 side=Image.new('RGBA',(16,16),'#352b3b');d=ImageDraw.Draw(side)
 d.rectangle((0,0,15,15),outline='#181720');d.rectangle((1,1,14,14),outline='#bd9461')
 for y in (4,8,12):d.line((2,y,13,y),fill='#5c465a');d.line((3,y+1,12,y+1),fill='#756075')
 for x in (2,13):
  for y in (2,13):d.point((x,y),fill='#ffe2a2')
 g.save(side,g.ASSETS/'textures/block/runic_hearth_side.png')
 variants={}
 for charge in range(4):
  top=Image.new('RGBA',(16,16),'#352b3b');d=ImageDraw.Draw(top);d.rectangle((0,0,15,15),outline='#181720');d.rectangle((1,1,14,14),outline='#bd9461')
  d.ellipse((3,3,12,12),fill='#191d2f',outline='#d4ab74');d.line((4,10,7,8,10,11,12,7),fill='#577858',width=1)
  for i,(x,y) in enumerate(((5,5),(10,5),(8,10))):
   d.polygon(((x,y-2),(x+1,y),(x,y+2),(x-1,y)),fill='#ddc8ff' if i<charge else '#67547f',outline='#362a52');d.point((x,y-1),fill='#fbecff' if i<charge else '#93829f')
  g.save(top,g.ASSETS/f'textures/block/runic_hearth_{charge}.png')
  g.write_json(g.ASSETS/f'models/block/runic_hearth_{charge}.json',{'parent':'minecraft:block/cube_bottom_top','textures':{'side':'wildercord:block/runic_hearth_side','bottom':'wildercord:block/runic_hearth_side','top':f'wildercord:block/runic_hearth_{charge}'}})
  variants[f'charge={charge}']={'model':f'wildercord:block/runic_hearth_{charge}'}
 g.write_json(g.ASSETS/'blockstates/runic_hearth.json',{'variants':variants})
 g.write_json(g.ASSETS/'items/runic_hearth.json',{'model':{'type':'minecraft:model','model':'wildercord:block/runic_hearth_3'}})
 g.write_json(g.DATA/'loot_table/blocks/runic_hearth.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'wildercord:runic_hearth'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
 g.write_json(g.DATA/'recipe/runic_hearth.json',{'type':'minecraft:crafting_shaped','category':'misc','pattern':['GBG','SMS','SSS'],'key':{'G':'minecraft:gold_ingot','B':'minecraft:book','S':'minecraft:stone_bricks','M':'wildercord:mana_crystal'},'result':{'id':'wildercord:runic_hearth'}})
 g.unlock_advancement('wildercord:runic_hearth','wildercord:mana_crystal')
