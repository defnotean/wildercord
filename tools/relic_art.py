"""Distinct hand-painted relic silhouettes: sandglass, living seed and a tipped flight feather."""
from PIL import Image,ImageDraw
def write(g):
 for path in ('keepers_hourglass','living_seedpod','sky_feather'):
  im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
  if path=='keepers_hourglass':
   d.rectangle((3,1,12,3),fill='#4c2f34',outline='#d9af6d');d.rectangle((3,12,12,14),fill='#4c2f34',outline='#d9af6d')
   d.line((4,4,7,7,7,8,4,11),fill='#93bcc9');d.line((11,4,8,7,8,8,11,11),fill='#d9f3f2')
   d.polygon(((5,4),(10,4),(8,6),(7,6)),fill='#e3c684');d.polygon(((7,9),(8,9),(10,11),(5,11)),fill='#f7dda0');d.point((7,8),fill='#ffedc5')
  elif path=='living_seedpod':
   d.ellipse((3,5,12,14),fill='#364a2f',outline='#182d29');d.ellipse((5,6,10,12),fill='#759350');d.line((7,5,9,1,12,2),fill='#b5cd75');d.polygon(((8,5),(11,3),(14,4),(12,6)),fill='#8ac878');d.line((7,8,7,11),fill='#dfdca1');d.point((8,7),fill='#fff0c2')
  else:
   d.polygon(((2,13),(4,6),(9,1),(13,2),(13,6),(7,11)),fill='#467b92',outline='#19334b');d.line((3,14,11,3),fill='#eed9a2',width=1)
   for x,y in ((5,9),(7,6),(9,4)):d.line((x,y,x+4,y),fill='#b9eff1');d.point((x+4,y-1),fill='#f5ffff')
   d.polygon(((1,14),(3,12),(5,14),(3,15)),fill='#bc9256',outline='#523f37')
  g.save(im,g.ASSETS/f'textures/item/{path}.png');g.item_model(path,path)
  g.write_json(g.ASSETS/f'items/{path}.json',{'model':{'type':'minecraft:model','model':f'wildercord:item/{path}'}})
