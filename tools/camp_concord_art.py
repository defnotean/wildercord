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

def sprite(style,frame):
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
 if style==0:
  d.polygon([(6,4),(22,2),(28,10),(26,28),(8,26),(4,18)],fill=(221,206,169),outline=(86,67,53));d.polygon([(22,2),(28,10),(20,9)],fill=(247,231,190),outline=(118,96,66));d.line([(7,5),(10,24),(25,27)],fill=(255,239,198));
  for y,end in [(12,23),(16,19),(20,24)]:d.line([(11,y),(end,y+1)],fill=(145,118,87))
  for x,y in [(7,12),(14,8),(24,21),(13,24)]:d.point((x,y),fill=(180,153,116))
 elif style==1:
  d.line([(7,25),(8,7),(21,5),(24,23)],fill=(65,43,33),width=5);d.line([(8,25),(9,8),(21,6),(23,23)],fill=(192,139,93),width=3);d.line([(10,24),(10,9),(20,7)],fill=(247,200,137));d.line([(23,8),(24,23)],fill=(123,78,53))
 elif style==2:
  d.polygon([(4,4),(13,2),(18,9),(14,13),(21,20),(18,29),(11,22),(7,14),(10,10)],fill=(201,179,144),outline=(105,78,57));d.line([(7,5),(12,10),(11,14),(17,22)],fill=(252,226,182));d.line([(13,3),(16,9),(12,13),(20,20)],fill=(129,99,72))
 elif style==3:
  for offset,color in [(-3,(109,85,138,190)),(0,(197,180,219,230)),(3,(231,214,235,210))]:
   points=[(16+offset+round(math.sin(y*.3+offset)*3),y) for y in range(3,30)];d.line(points,fill=color,width=2 if offset==0 else 1)
  d.point((17,10),fill=(252,235,252));d.point((12,23),fill=(239,218,241))
 elif style==4:
  d.polygon([(4,5),(27,5),(27,10),(24,10),(24,25),(21,25),(21,11),(17,11),(17,28),(14,28),(14,11),(10,11),(10,23),(7,23),(7,10),(4,10)],fill=(215,189,141),outline=(103,78,64));d.line([(5,6),(26,6)],fill=(251,222,178));d.line([(8,12),(8,22)],fill=(249,220,175));d.line([(15,12),(15,27)],fill=(242,211,166))
 elif style==5:
  d.polygon([(10,5),(19,6),(26,14),(23,23),(14,27),(5,20),(7,11)],fill=(131,104,150),outline=(220,192,227));d.line([(8,12),(21,21),(23,15),(12,8),(9,19),(18,24)],fill=(238,212,241),width=2);d.line([(9,14),(21,23)],fill=(69,54,85),width=2)
 if frame:d.line([(11,13),(17,16),(20,12)],fill=(246,217,184,210))
 return im

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
 # Independent asymmetrical stitched folio corners and perforations; no borrowed ring mask.
 bands['watchweft']={(x,3) for x in range(2,9)}|{(2,y) for y in range(3,12)}|{(x,12) for x in range(5,14)}|{(13,y) for y in range(7,13)}|{(4,5),(4,7),(4,9),(6,5),(8,6),(10,8),(11,10),(7,14),(8,14),(11,2),(12,3),(13,4)}
 marks['watchweft']={(x,5) for x in range(4,11)}|{(4,y) for y in range(5,12)}|{(11,y) for y in range(6,12)}|{(x,11) for x in range(4,12)}|{(6,7),(8,7),(10,8),(6,9),(8,9),(12,4),(12,5),(3,12),(5,13)}
 bands['manabraid']={(x,2) for x in range(1,7)}|{(x,13) for x in range(9,15)}|{(x,y) for x,y in [(2,3),(2,4),(4,3),(4,5),(6,3),(6,6),(9,10),(9,12),(11,9),(11,12),(13,8),(13,12),(4,8),(5,9),(6,10),(7,8),(8,7),(9,5),(10,6),(11,7),(8,14)]}
 marks['manabraid']={(3,y) for y in range(3,11)}|{(12,y) for y in range(5,14)}|{(x,y) for x,y in [(4,4),(5,4),(4,7),(5,7),(4,10),(5,10),(10,6),(11,6),(10,9),(11,9),(10,12),(11,12),(6,5),(7,6),(8,7),(9,8),(6,8),(7,9),(8,10),(9,11),(7,12),(8,13)]}
