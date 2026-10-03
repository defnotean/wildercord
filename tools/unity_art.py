"""Two woven paths: an authored small insignia, compact controls and transfer captions."""
from PIL import Image, ImageDraw
LANG={
 'screen.wildercord.unity.start':'Unity · 12 mana + 12 Aura',
 'screen.wildercord.unity.active':'Unity · %s s remaining',
 'screen.wildercord.unity.rest':'Unity · rests %s s',
 'screen.wildercord.unity.desc':'For 12 s, paid mana returns 25% Aura (12 max); paid Aura returns 50% mana (24 max). Rest: 120 s.',
 'message.wildercord.unity.ready':'Ready: Form, five working Circles, a Cord and a weapon.',
 'message.wildercord.unity.survival':'Unity needs a living Survival or Adventure player.',
 'message.wildercord.unity.paths':'Unity needs Form, five working Heart Circles and a Cord.',
 'message.wildercord.unity.spent':'Unity cannot hold while spent or silenced.',
 'message.wildercord.unity.spar':'Unity waits outside Aura lessons and sparring bouts.',
 'message.wildercord.unity.weapon':'Hold an Aura weapon to bring the two paths together.',
 'message.wildercord.unity.active':'Unity is already working.',
 'message.wildercord.unity.rest':'Unity is still resting.',
 'message.wildercord.unity.price':'Unity needs 12 mana and 12 Aura.',
 'message.wildercord.unity.begin':'Unity · blade and heart share one breath',
 'toast.wildercord.aura.unity':'Two paths, one breath',
 'subtitles.wildercord.kit.unity.begin':'Two voices settle together',
 'subtitles.wildercord.kit.unity.flow':'A breath passes between paths',
 'subtitles.wildercord.kit.unity.end':'The shared breath releases',
}
def write(g):
 im=Image.new('RGBA',(24,24));d=ImageDraw.Draw(im)
 gold=[(3,19),(7,16),(9,11),(14,9),(17,5),(20,3)]
 blue=[(3,4),(7,7),(9,12),(14,14),(17,18),(20,20)]
 for points,shadow,base,light in [(gold,'#473323','#C8974E','#FFF0AC'),(blue,'#1A3557','#559AD0','#C0ECFF')]:
  d.line([(x+1,y+1) for x,y in points],fill=shadow,width=5)
  d.line(points,fill=shadow,width=5);d.line(points,fill=base,width=3)
  d.line([(x-1,y-1) for x,y in points],fill=light,width=1)
 # The crossing is laced over and under, with exposed thread and tiny steel ties.
 d.line([(8,12),(10,11),(14,9)],fill='#473323',width=5)
 d.line([(8,12),(10,11),(14,9)],fill='#D5A35B',width=3)
 d.line([(8,11),(10,10),(14,8)],fill='#FFF0AC',width=1)
 for x,y in [(5,17),(16,7),(6,6),(17,18)]:
  d.line([(x-1,y),(x+1,y+1)],fill='#D6D9D8',width=1)
 for x,y in [(3,19),(20,3),(3,4),(20,20)]:d.point((x,y),fill='#FFFFFF')
 g.save(im,g.ASSETS/'textures/gui/sprites/aura/unity.png')
