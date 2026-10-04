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
 """Root Carry's ring and emblem, in the same grammar as every rune's (see circle_art): an Effect's solid
 line and round frame, a sprout above the line with its roots forking below, and a carried clod as glyph."""
 import circle_art as ca
 bands['root_carry']=ca.band_tile('effect','sprout','out','none')|{(8,9),(8,10),(7,11),(9,11),(6,12),(10,12)}
 marks['root_carry']=ca.mark_tile('effect',['.#.#.#.','..###..','...#...','.#####.','#######','.#.#.#.','#..#..#'],'plain','none',False)
# Registration also adds RUNE_RECIPES['root_carry'] to the central themed table:
# ['minecraft:rooted_dirt','minecraft:bone_meal','minecraft:string'].
# Normal rank-II catalysts (two lapis +gold) are preserved by rune_ingredients.
