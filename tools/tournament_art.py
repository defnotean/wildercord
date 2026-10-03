"""A timber steward's stand, embroidered cloth standards and a stitched village record."""
from PIL import Image,ImageDraw
from sleeping_blade_art import box
LANG={
'block.wildercord.tournament_board':'Village Tournament Stand',
'block.wildercord.tournament_standard':'Stewards\' Cloth Standard',
'message.wildercord.tournament.rules':'The Three Bows: win 3 Aura bouts. Stay within 8 blocks of the centre; each bout lasts at most 2 minutes. Spells hitting a steward disqualify you. Return here between bouts within 1 minute. Knockouts are safe.',
'message.wildercord.tournament.accept':'Use again within 10 seconds to enter. Stand on the arena side of this stand.',
'message.wildercord.tournament.disabled':'Village tournaments are disabled by this server.',
'message.wildercord.tournament.empty':'This stand has no village steward.',
'message.wildercord.tournament.closed':'The stewards are away. With residents nearby, a new tournament opens here every three Minecraft days, for one Minecraft day. Saved prizes may still be claimed.',
'message.wildercord.tournament.won_already':'You have already won this gathering. You may spectate or return for the next one.',
'message.wildercord.tournament.full':'This gathering has reached its ledger capacity. The stewards cannot accept another entry.',
'message.wildercord.tournament.busy':'Another challenger holds the ground. Watch the bout, then enter when their run ends.',
'message.wildercord.tournament.fighting':'Your bout is still under way. Return after the bow.',
'message.wildercord.tournament.eligible':'Bring a blade in Survival, with Aura enabled, and finish any duel or spar before entering.',
'message.wildercord.tournament.obstructed':'The arena is obstructed. Clear the fighting ground before entering.',
'message.wildercord.tournament.interrupted':'A steward is missing. This run ends without a prize; return for the next gathering.',
'message.wildercord.tournament.magic':'Your spell touched the steward. This Aura tournament run ends without a prize.',
'message.wildercord.tournament.lost':'Your tournament run has ended. You may try again while this gathering is open.',
'message.wildercord.tournament.victory':'Three bows: you win! Return to the stand to claim your prize. The initial scroll choice favours a part you do not know. Sneak-use to cycle choices; use normally to claim the displayed choice once.',
'message.wildercord.tournament.round':'Bout won. Return to the stand within one minute to begin bout %s.',
'message.wildercord.tournament.bout':'Bout %s of 3: %s. Watch the blade and answer its openings.',
'message.wildercord.tournament.choice':'Prize choice: %s. Sneak-use to change; use normally to claim.',
'message.wildercord.tournament.reward':'The stewards award %s, three Aura Shards and The Three Bows. This prize is now claimed.',
'book.wildercord.tournament.1':'The Three Bows\n\nThe Marchkeepers once chose captains by the longest reach of a blade. Villages paid for their pride. A steward asked for three bows instead: one to a rival, one to the ground, and one to the people who must live upon it.',
'book.wildercord.tournament.2':'Different Hands\n\nThree schools gather under stitched cloth. They fight at the challenger\'s stage, with warning, guard and recovery. The bell begins each bout. A fall costs no life. Step around a prepared cut; an axe can answer a raised guard. Those who leave yield the ground.',
'book.wildercord.tournament.3':'The Steward\'s Ledger\n\nWin three clean Aura bouts for a scroll of your choosing. A spell touching a rival ends the run. Sneak at the stand to change your scroll choice, then use it normally to claim. Each gathering awards once. The ledger remembers an unclaimed prize through saves.',
'toast.wildercord.aura.tournament':'The Three Bows',
}
for name,text in [('open','Village tournament opens'),('round','Tournament bout won'),('victory','Three bows: tournament won'),('prize','Stewards award a prize')]:LANG['subtitles.wildercord.kit.tournament.'+name]=text

def write(g):
 for name,base in [('timber','#664b35'),('brass','#ad854f'),('ledger','#e4d5af'),('cloth','#23464b')]:
  im=Image.new('RGBA',(32,32),base);d=ImageDraw.Draw(im)
  for y in range(32):
   for x in range(32):
    h=(x*11+y*23+x*y)%37
    if name=='timber' and (x%7==0 or h<4):d.point((x,y),fill='#392d24' if x%7==0 else '#94764f')
    if name=='brass' and h<7:d.point((x,y),fill='#e0bd77' if h<3 else '#775735')
    if name=='cloth' and (x+y)%3==0:d.point((x,y),fill='#31595c')
  if name=='cloth':
   d.rectangle((2,2,29,29),outline='#bd9a60',width=2)
   for x in (9,16,23):d.line((x-3,10,x,7,x+3,10,x,13,x-3,10),fill='#e6d6a3',width=2);d.line((x,13,x,24),fill='#b89859',width=2)
  if name=='ledger':
   d.rectangle((2,2,29,29),outline='#8a7253',width=1)
   for y in (7,15,23):
    d.line((9,y,25,y),fill='#746147',width=1);d.line((10,y+3,21,y+3),fill='#9c8968')
    d.ellipse((4,y-1,7,y+2),fill='#32666a')
  g.save(im,g.ASSETS/f'textures/block/tournament_{name}.png')
 tex={n:'wildercord:block/tournament_'+n for n in ('timber','brass','ledger','cloth')};tex['particle']=tex['timber']
 elements=[box([1,0,2],[4,13,5],'timber'),box([12,0,2],[15,13,5],'timber'),box([0,12,1],[16,15,8],'timber'),box([1,14,1],[15,15,7],'brass'),box([2,15,2],[14,15.2,6],'ledger'),box([2,2,3],[14,11,4],'cloth'),box([0,0,1],[5,2,7],'timber'),box([11,0,1],[16,2,7],'timber')]
 g.write_json(g.ASSETS/'models/block/tournament_board.json',{'textures':tex,'elements':elements})
 flag=[box([2,0,7],[4,16,9],'timber'),box([1,14,6],[5,16,10],'brass'),box([4,7,7.5],[15,14,8.5],'cloth'),box([4,5,7.5],[12,7,8.5],'cloth'),box([4,3,7.5],[8,5,8.5],'cloth'),box([0,0,5],[6,2,11],'timber')]
 g.write_json(g.ASSETS/'models/block/tournament_standard.json',{'textures':tex,'elements':flag})
 for name in ('tournament_board','tournament_standard'):g.write_json(g.ASSETS/f'blockstates/{name}.json',{'variants':{'':{'model':'wildercord:block/'+name}}})
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
 d.polygon([(6,3),(24,4),(28,26),(10,29),(4,24)],fill='#163238',outline='#090f16');d.polygon([(7,4),(23,5),(25,25),(10,27)],fill='#31585b',outline='#bd9a62')
 d.polygon([(4,5),(7,4),(10,27),(6,25)],fill='#764d35')
 for y in range(7,26,4):d.line((5,y,8,y+1),fill='#d3ba8b')
 for x in (12,17,22):d.line((x,10,x,21),fill='#d7ba80',width=1);d.polygon([(x-2,11),(x,8),(x+2,11),(x,13)],fill='#ebd5a0')
 g.save(im,g.ASSETS/'textures/item/three_bows.png');g.write_json(g.ASSETS/'models/item/three_bows.json',{'parent':'minecraft:item/generated','textures':{'layer0':'wildercord:item/three_bows'}});g.write_json(g.ASSETS/'items/three_bows.json',{'model':{'type':'minecraft:model','model':'wildercord:item/three_bows'}})
