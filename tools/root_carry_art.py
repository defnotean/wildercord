"""Original carved earth-clod/root glyph; dormant until registered by the main generator."""
from PIL import Image,ImageDraw
LANG={'rune.wildercord.root_carry':'Root Carry','subtitles.wildercord.kit.root_carry.cue':'Root fibres tension','subtitles.wildercord.kit.root_carry.select':'Living root marked','subtitles.wildercord.kit.root_carry.settle':'Root and soil settle'}
def icon():
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
 d.polygon([(7,2),(24,2),(29,7),(29,24),(24,29),(7,29),(2,24),(2,7)],fill=(64,72,64),outline=(150,158,127))
 d.line([(7,3),(23,3),(27,7)],fill=(209,210,176));d.line([(28,9),(28,23),(23,28),(9,28)],fill=(29,37,33))
 d.polygon([(11,15),(20,13),(23,18),(19,22),(11,21),(8,18)],fill=(115,82,52),outline=(207,169,111))
 for x,y in [(12,17),(15,15),(19,16),(17,20),(10,19),(21,18)]:d.point((x,y),fill=(57,45,37))
 d.line([(16,16),(16,8)],fill=(186,194,139),width=2)
 for side in [-1,1]:
  d.polygon([(16,11),(16+side*5,7),(16+side*7,8),(16+side*3,12)],fill=(122,151,95),outline=(184,195,134))
  d.line([(12 if side<0 else 20,20),(11 if side<0 else 21,24),(14 if side<0 else 18,25),(16,23)],fill=(218,197,150))
 d.point((26,6),fill=(168,142,99));d.point((6,25),fill=(168,142,99));return im
def write(g):
 g.save(icon(),g.ASSETS/'textures/item/rune/root_carry.png')
def circles(bands,marks):
 """Original carried clod nested in root fibres; checked by the unchanged uniqueness gate."""
 # A short central soil stitch, two lifting roots and two folded fronds.
 band={(x,11) for x in range(16)}|{(x,12) for x in (2,3,6,7,10,11,14,15)}
 band|={(2,9),(3,8),(4,7),(5,6),(6,7),(7,8),(8,9),(9,8),(10,7),(11,6),(12,7),(13,8),(14,9)}
 band|={(4,4),(5,3),(6,4),(10,4),(11,3),(12,4),(7,10),(8,10),(9,10),(6,13),(5,14),(10,13),(11,14)}
 mark={(7,3),(8,3),(7,4),(8,4),(7,5),(8,5),(6,5),(5,4),(4,4),(9,5),(10,4),(11,4)}
 mark|={(5,8),(6,7),(7,7),(8,7),(9,7),(10,8),(10,9),(9,10),(8,10),(7,10),(6,10),(5,9)}
 mark|={(4,8),(3,9),(3,10),(4,11),(5,12),(6,12),(7,11),(11,8),(12,9),(12,10),(11,11),(10,12),(9,12),(8,11),(7,13),(8,13)}
 bands['root_carry']=band;marks['root_carry']=mark
# Registration also adds RUNE_RECIPES['root_carry'] to the central themed table:
# ['minecraft:rooted_dirt','minecraft:bone_meal','minecraft:string'].
# Normal rank-II catalysts (two lapis +gold) are preserved by rune_ingredients.
