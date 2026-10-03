"""Carved stone, aged brass and burial cloth: the Marchkeepers' tomb and its articulated effigy."""
import json
from PIL import Image, ImageDraw

LANG={
 'block.wildercord.intent_gate':'Intent Gate', 'block.wildercord.tomb_reliquary':'Keeper Reliquary',
 'entity.wildercord.gravekeeper':'The Buried Keeper',
 'guide.wildercord.gravekeeper':'A blindfolded stone effigy defending the Marchkeepers\' buried blades. Its sweep covers a broad front; its thrust cuts a narrow lane. Both lock direction before striking. Move aside, punish its recovery, or break the forward guard with an axe. Its sides and back remain vulnerable.',
 'guide.wildercord.gravekeeper.hint':'A broken stone arch in open highland country hides the sword tomb. An Edge blade wakes its keeper.',
 'message.wildercord.tomb.gate':'This gate answers a blade carrying %s or greater Aura.',
 'message.wildercord.tomb.empty':'This reliquary has no buried keeper.',
 'message.wildercord.tomb.challenge':'An Edge blade can challenge the Buried Keeper. Use the reliquary while holding it.',
 'message.wildercord.tomb.active':'The keeper is already awake. Step into its chamber.',
 'message.wildercord.tomb.peaceful':'The keeper sleeps while this world is Peaceful.',
 'message.wildercord.tomb.obstructed':'Clear the keeper\'s place in the centre of the chamber.',
 'message.wildercord.tomb.wakes':'The Buried Keeper rises. Watch its blade: broad sweep, narrow thrust, then a forward guard.',
 'message.wildercord.tomb.spent':'The testament has already been claimed, or you did not take part in this encounter.',
 'message.wildercord.tomb.reward':'The keeper yields two technique scrolls, three Aura Shards and its testament. This tomb remembers your claim.',
 'boss.wildercord.gravekeeper.status':'%s — %s',
 'boss.wildercord.gravekeeper.move.0':'Watching', 'boss.wildercord.gravekeeper.move.1':'Broad sweep: leave the front',
 'boss.wildercord.gravekeeper.move.2':'Straight thrust: step sideways', 'boss.wildercord.gravekeeper.move.3':'Forward guard: flank or use an axe',
 'boss.wildercord.gravekeeper.move.4':'Recovering: an opening', 'boss.wildercord.gravekeeper.move.5':'Guard broken: an opening',
 'toast.wildercord.aura.sword_tomb':'The Keeper\'s Testament',
 'book.wildercord.keeper.1':'The Keeper\'s Testament\n\nWhen the Marchkeepers laid down their command, they buried their blades under a road nobody owned. The stone keeper wears a blindfold: it guards a promise, not a banner.',
 'book.wildercord.keeper.2':'Its first stroke measures the room. Its second follows a single footstep. Its guard faces only one direction. The old masters left openings deliberately; a student who sees them has earned the lesson.',
 'book.wildercord.keeper.3':'The three memorials remember an opening, a shelter and a return. Their tomb preserves what came after. Farther into the highlands, one blade still sleeps in living rock. The keeper never claimed it.',
}
for name,text in {
 'gate':'Intent gate grinds open', 'awake':'Buried Keeper rises', 'brace':'Buried Keeper braces its blade',
 'sweep_ready':'Buried Keeper prepares a broad sweep', 'thrust_ready':'Buried Keeper aligns a thrust',
 'sweep':'Buried Keeper sweeps', 'thrust':'Buried Keeper thrusts', 'break':'Keeper\'s guard cracks',
 'fall':'Buried Keeper falls', 'reward':'Keeper reliquary opens',
}.items():LANG['subtitles.wildercord.kit.gravekeeper.'+name]=text

def write(g):
    skin=Image.new('RGBA',(128,64));d=ImageDraw.Draw(skin)
    for x in range(128):
        for y in range(64):
            v=(x*19+y*23+x*y*3)%19
            skin.putpixel((x,y),(94+v,101+v,100+v,255))
    # UV regions are deliberately separate materials. Stone gets incised seams and chipped corners.
    for rect in [(0,0,39,28),(0,32,43,51),(72,0,95,23),(96,0,119,19)]:
        x0,y0,x1,y1=rect;d.rectangle(rect,outline='#4a5556');d.line((x0+1,y0+1,x1-1,y0+1),fill='#c0c8bf')
        for x in range(x0+3,x1,7):d.line([(x,y0+4),(x+1,y0+8),(x-1,y0+11)],fill='#566260')
    d.rectangle((40,0,71,31),fill='#806a42')
    for y in range(0,30,4):d.line((40,y,71,y),fill='#baa474' if y%8==0 else '#594d36')
    for x,y in [(43,3),(65,3),(43,9),(65,9)]:d.rectangle((x,y,x+1,y+1),fill='#e1ce91')
    d.rectangle((48,34,95,47),fill='#504757')
    for y in range(35,47,3):d.line((48,y,95,y),fill='#716479')
    d.line((48,36,95,36),fill='#c3a66b');d.line((48,45,95,45),fill='#a38d60')
    d.rectangle((64,48,83,63),fill='#433b4c')
    for x in (65,73,81):d.line((x,48,x,62),fill='#6c5d77')
    d.line((65,60,81,60),fill='#c2a46c')
    d.line([(70,52),(76,58),(76,52),(70,58)],fill='#b49e78',width=1)
    d.rectangle((104,32,115,62),fill='#697675');d.line((105,32,105,62),fill='#d6d6bf',width=2);d.line((111,32,111,62),fill='#363f43')
    for y in (36,47,57):d.line((107,y,109,y+2),fill='#8d6645')
    g.save(skin,g.ASSETS/'textures/entity/gravekeeper.png')
    for name,base in [('tomb_stone',(75,80,81)),('tomb_brass',(126,103,63)),('tomb_cloth',(73,61,83))]:
        im=Image.new('RGBA',(32,32));draw=ImageDraw.Draw(im)
        for x in range(32):
            for y in range(32):
                n=(x*31+y*17+x*y)%21;im.putpixel((x,y),tuple(min(255,c+n) for c in base)+(255,))
        draw.rectangle((1,1,30,30),outline=tuple(max(0,c-30) for c in base),width=2);draw.line((3,3,28,3),fill='#b8b6a0')
        draw.line([(8,4),(11,11),(9,17),(15,25)],fill='#3d4546');g.save(im,g.ASSETS/f'textures/block/{name}.png')
    for stage in (2,3):
        im=Image.new('RGBA',(32,32),'#444c50');draw=ImageDraw.Draw(im)
        for x in (2,11,20,29):draw.rectangle((x,0,x+2,31),fill='#a0a69a');draw.line((x,0,x,31),fill='#d2ccaf')
        draw.rectangle((0,13,31,18),fill='#8c764e');draw.line((0,13,31,13),fill='#dbc692')
        for i in range(stage):draw.rectangle((9+i*6,14,11+i*6,16),fill='#eee0b7')
        g.save(im,g.ASSETS/f'textures/block/intent_gate_{stage}.png')
        g.write_json(g.ASSETS/f'models/block/intent_gate_{stage}.json',{'parent':'minecraft:block/cube_all','textures':{'all':f'wildercord:block/intent_gate_{stage}'}})
    g.write_json(g.ASSETS/'blockstates/intent_gate.json',{'variants':{f'facing={f},stage={s}':{'model':f'wildercord:block/intent_gate_{s}'} for f in ('north','south','east','west') for s in (2,3)}})
    def cube(a,b,material,rotation=None):
        e={'from':a,'to':b,'faces':{f:{'texture':'#'+material} for f in ('up','down','north','south','east','west')}}
        if rotation:e['rotation']=rotation
        return e
    for state in range(3):
        lid=cube([2,12,2],[14,14,14],'cloth',{'origin':[8,12,14],'axis':'x','angle':22.5} if state==2 else None)
        elements=[cube([0,0,0],[16,2,16],'stone'),cube([2,2,2],[14,12,14],'stone'),cube([1,9,1],[15,11,15],'brass'),lid,cube([7,14,3],[9,15,13],'brass')]
        g.write_json(g.ASSETS/f'models/block/tomb_reliquary_{state}.json',{'textures':{k:'wildercord:block/tomb_'+k for k in ('stone','brass','cloth')}|{'particle':'wildercord:block/tomb_stone'},'elements':elements})
    g.write_json(g.ASSETS/'blockstates/tomb_reliquary.json',{'variants':{f'facing={f},state={s}':{'model':f'wildercord:block/tomb_reliquary_{s}','y':yaw} for f,yaw in [('north',0),('east',90),('south',180),('west',270)] for s in range(3)}})
    book=Image.new('RGBA',(32,32));b=ImageDraw.Draw(book)
    b.polygon([(5,4),(24,2),(28,7),(28,27),(9,30),(4,25)],fill='#322d3a');b.polygon([(8,5),(24,4),(26,7),(26,25),(9,28)],fill='#6c5d77')
    for y in (7,11,15,19,23):b.line((5,y,8,y+1),fill='#c3a66b')
    b.polygon([(9,27),(26,24),(26,27),(9,30)],fill='#d9c9a5');b.line((10,28,24,26),fill='#8f7c5a')
    b.rectangle((13,9,22,20),fill='#afb4a9');b.line((12,13,23,13),fill='#433b4c',width=3);b.line((17,20,17,24),fill='#dac18b')
    for x,y in [(9,6),(23,5),(10,24),(23,23)]:b.rectangle((x,y,x+2,y+2),fill='#d6af64')
    g.save(book,g.ASSETS/'textures/item/keeper_testament.png')
    g.write_json(g.ASSETS/'models/item/keeper_testament.json',{'parent':'minecraft:item/generated','textures':{'layer0':'wildercord:item/keeper_testament'}})
    g.write_json(g.ASSETS/'items/keeper_testament.json',{'model':{'type':'minecraft:model','model':'wildercord:item/keeper_testament'}})
    g.write_json(g.DATA/'worldgen/structure/sword_tomb.json',{'type':'wildercord:dungeon','dungeon':'sword_tomb','biomes':'#wildercord:has_structure/sword_tomb','spawn_overrides':{},'step':'surface_structures','terrain_adaptation':'none'})
    g.write_json(g.DATA/'worldgen/structure_set/sword_tombs.json',{'placement':{'type':'minecraft:random_spread','salt':202610022,'spacing':76,'separation':28},'structures':[{'structure':'wildercord:sword_tomb','weight':1}]})
    g.write_json(g.DATA/'tags/worldgen/biome/has_structure/sword_tomb.json',{'replace':False,'values':['minecraft:meadow','minecraft:windswept_hills','minecraft:windswept_gravelly_hills','minecraft:plains','minecraft:snowy_plains']})
    g.write_json(g.DATA/'tags/worldgen/structure/sword_tomb.json',{'replace':False,'values':['wildercord:sword_tomb']})
    g.write_json(g.DATA/'loot_table/chests/sword_tomb_hall.json',{'type':'minecraft:chest','pools':[{'rolls':3,'entries':[{'type':'minecraft:item','name':item,'weight':w} for item,w in [('minecraft:iron_ingot',3),('minecraft:paper',4),('minecraft:bread',3),('minecraft:gold_nugget',2)] ]}]})
    g.write_json(g.DATA/'loot_table/entities/gravekeeper.json',{'type':'minecraft:entity','pools':[]})
