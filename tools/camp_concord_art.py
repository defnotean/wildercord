"""Original folded camp-ledger and three-comb mana fibre assets; explicit LF/UTF-8 writes."""
from PIL import Image,ImageDraw
import json,math
LANG={
 'rune.wildercord.watchweft':'Watchweft','rune.wildercord.manabraid':'Manabraid',
 'message.wildercord.watchweft.warn':'Watchweft noticed a hostile approach. The watch is spent.',
 'message.wildercord.watchweft.obscured':'Watchweft is obscured; its next clear sample starts a new watch history.',
 'message.wildercord.manabraid.offer':'Mana offered: release crouch, then crouch once within three seconds to accept up to 16.',
 'message.wildercord.manabraid.given':'Gave %s mana through a lossy braid.',
 'message.wildercord.manabraid.received':'Received %s mana from an allied caster.',
}
for name,text in [('watch_cue','Paper folds around copper staples'),('watch_arm','Camp ledger tensions'),('watch_warn','Ledger tears a warning'),('braid_cue','Mana fibres pass through combs'),('braid_offer','Empty spool opens'),('braid_transfer','Mana braid tightens and clasps'),('braid_decline','Empty spool falls open')]:
 LANG['subtitles.wildercord.kit.camp.'+name]=text

# Camp particles: paper, staple, tear, fibre, comb and knot. Tinted in game by the colour each cue sends
# (CampParticle), so drawn in greys like every material particle; see particle_pixels. The second frame of
# each is the same piece later in its life (creased, bent, curled, twisted, caught, drawn tight).
import math
import particle_pixels as px

# A ledger page: written lines, its top right corner folded down over itself.
CAMP_PAPER=px.shade([
 "................",
 "..########......",
 "..#########.....",
 "..##sssss#ll#...",
 "..#######lllk#..",
 "..###########k..",
 "..##sssssss###..",
 "..############..",
 "..##ssssssss##..",
 "..############..",
 "..##sssss#####..",
 "..############..",
 "..##sssssss###..",
 "..############..",
 "................",
 "................",
])
# Later: folded across, a crease down the page.
CAMP_PAPER_CREASED=px.edit(CAMP_PAPER,"7,2,s 7,3,k 7,4,k 7,5,s 7,6,k 7,7,s 7,8,k 7,9,s 7,10,s 7,11,s 7,12,k 6,2,h 6,4,h 6,5,h 6,7,h 6,9,h 6,11,h")

# A copper staple: a square-shouldered bar on two pointed legs.
CAMP_STAPLE=px.shade([
 "................",
 "................",
 ".##############.",
 ".##############.",
 ".##############.",
 ".####......####.",
 ".####......####.",
 ".####......####.",
 ".####......####.",
 ".####......####.",
 ".####......####.",
 ".####......####.",
 "..###.......###.",
 "..##........##..",
 "................",
 "................",
])
# Later: driven home, one leg bent out under the strain.
CAMP_STAPLE_BENT=px.shade([
 "................",
 "................",
 ".##############.",
 ".##############.",
 ".##############.",
 ".####......####.",
 ".####......####.",
 ".####......####.",
 ".####.......####",
 ".####.......####",
 ".####........###",
 ".####.........##",
 "..###...........",
 "..##............",
 "................",
 "................",
])

# A torn strip of paper, ragged at both ends, a line of writing across it.
CAMP_TEAR=px.shade([
 "................",
 "...##.##........",
 "..#########.....",
 "..##########....",
 "...##sssss###...",
 "....##########..",
 ".....#ssssss###.",
 "......#########.",
 ".....#########..",
 "....###sss###...",
 "...#########....",
 "..#########.....",
 "..##.##.##......",
 "................",
 "................",
 "................",
])
# Later: its lower end has curled back on itself.
CAMP_TEAR_CURLED=px.shade([
 "................",
 "...##.##........",
 "..#########.....",
 "..##########....",
 "...##sssss###...",
 "....##########..",
 ".....#ssssss###.",
 "......#########.",
 ".....#########..",
 "....###sss###...",
 "...########.....",
 "...##kkkk##.....",
 "....######......",
 "................",
 "................",
 "................",
])

# The back strand of a braid sits in shade.
_BACK={"edge_lit":"o","edge":"o","hi":"l","hi2":"m","body":"s","lo":"s"}

def _braid(phase):
 """Two mana fibres twisting round each other down the tile, crossing over and under in turn."""
 masks=[[["."]*16 for _ in range(16)] for _ in range(2)]
 front={}
 for y in range(16):
  for k in range(2):
   a=y*.45+k*math.pi+phase
   cx=7.5+2.6*math.sin(a)
   for x in range(16):
    if abs(x-cx)<2.4:
     masks[k][y][x]="#"
     if math.cos(a)>0:front[(x,y)]=k
 lit=[px.shade(masks[0]),px.shade(masks[1],_BACK)]
 out=px.blank()
 for y in range(16):
  for x in range(16):
   on=[k for k in range(2) if lit[k][y][x]!="."]
   if on:
    k=front.get((x,y),on[0])
    out[y][x]=lit[k if k in on else on[0]][y][x]
 return out

CAMP_FIBER=_braid(0)
CAMP_FIBER_TWISTED=_braid(1.6)

# A three-toothed comb that the mana fibres pass through.
CAMP_COMB=px.shade([
 "................",
 "................",
 ".##############.",
 ".##############.",
 ".##############.",
 ".##############.",
 ".####.####.####.",
 ".####.####.####.",
 ".####.####.####.",
 ".####.####.####.",
 ".####.####.####.",
 "..##..####..##..",
 "......####......",
 ".......##.......",
 "................",
 "................",
])
# Later: a fibre is caught across its teeth.
CAMP_COMB_CAUGHT=px.edit(CAMP_COMB,"0,9,h 1,9,h 2,9,h 3,9,h 4,9,h 5,8,h 6,8,h 7,8,h 8,8,h 9,8,h 10,9,h 11,9,h 12,9,h 13,9,h 14,9,h 15,10,h "
                                   "0,10,o 1,10,o 5,9,o 6,9,o 10,10,o 11,10,o")

def _clasp(outer,inner):
 """A ring of braided fibre with a band bound across it."""
 ring=[["#" if inner<math.hypot(x-7.5,y-7.5)<outer else "." for x in range(16)] for y in range(16)]
 band=[["#" if 6<=x<=9 and abs(y-7.5)<outer+.5 else "." for x in range(16)] for y in range(16)]
 lit_ring,lit_band=px.shade(ring),px.shade(band)
 return [[lit_band[y][x] if lit_band[y][x]!="." else lit_ring[y][x] for x in range(16)] for y in range(16)]

CAMP_KNOT=_clasp(7.3,3.4)
# Later: drawn tight, the ring pulled smaller round the band.
CAMP_KNOT_TIGHT=_clasp(6.6,3.0)

CAMP_SPRITES=[(CAMP_PAPER,CAMP_PAPER_CREASED),(CAMP_STAPLE,CAMP_STAPLE_BENT),(CAMP_TEAR,CAMP_TEAR_CURLED),
 (CAMP_FIBER,CAMP_FIBER_TWISTED),(CAMP_COMB,CAMP_COMB_CAUGHT),(CAMP_KNOT,CAMP_KNOT_TIGHT)]

def sprite(style,frame):
 return px.render(CAMP_SPRITES[style][frame])

def icon(which):
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
 d.polygon([(8,2),(25,3),(30,10),(28,25),(21,30),(7,28),(2,21),(3,8)],fill=(67,60,80),outline=(185,167,183));d.line([(9,3),(24,4),(28,10)],fill=(231,213,206));d.line([(28,13),(26,25),(21,28),(9,27)],fill=(35,31,44))
 if which=='watchweft':
  page=sprite(0,0).resize((20,20));im.alpha_composite(page,(6,6));d.line([(9,6),(9,20)],fill=(209,151,99),width=2);d.line([(9,6),(13,6)],fill=(252,210,143));d.polygon([(21,19),(26,21),(23,25),(20,23)],fill=(196,124,91),outline=(241,184,123))
 else:
  d.line([(10,8),(10,22)],fill=(227,199,151),width=3);d.line([(22,10),(22,25)],fill=(169,140,192),width=3)
  for i in range(3):d.line([(10,10+i*4),(15,9+i*4),(19,17+i*2),(22,12+i*4)],fill=[(219,197,227),(151,125,181),(238,215,226)][i],width=2)
  d.polygon([(15,15),(18,13),(20,17),(17,20),(14,18)],fill=(237,212,223),outline=(95,73,113))
 return im

def write(g):
 names=[]
 for style in range(6):
  for frame in range(2):
   name=f'camp_{style}_{frame}';g.save(sprite(style,frame),g.ASSETS/'textures/particle'/f'{name}.png');names.append('wildercord:'+name)
 p=g.ASSETS/'particles/camp.json';p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps({'textures':names},indent=2)+'\n',encoding='utf-8',newline='\n')
 for name in ['watchweft','manabraid']:g.save(icon(name),g.ASSETS/'textures/item/rune'/f'{name}.png')

def circles(bands,marks):
 # Written in the same grammar as every rune's ring and emblem (see circle_art): an Effect's solid line
 # and round frame, Arcane's motifs on the line, and an authored glyph for each mechanism in the frame.
 import circle_art as ca
 # Watchweft: a star watching over the line, weft stitches under it; an open eye over a woven rule.
 bands['watchweft']=ca.band_tile('effect','star','out','none')|{(4,9),(4,10),(12,9),(12,10)}
 marks['watchweft']=ca.mark_tile('effect',['..###..','.#.#.#.','#..#..#','.#...#.','..###..','.......','#.#.#.#'],'plain','ticks2',False)
 # Manabraid: trines handed back and forth across the line; two strands braided round one crossing.
 bands['manabraid']=ca.band_tile('effect','trine','alt','none')|{(8,6),(8,9)}
 marks['manabraid']=ca.mark_tile('effect',['.#...#.','#.#.#.#','...#...','..#.#..','...#...','#.#.#.#','.#...#.'],'dots','none',False)
