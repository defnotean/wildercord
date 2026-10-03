"""Weathered memorials, three cloth-bound field books, world placement, supplies and discoverable histories."""
from PIL import Image, ImageDraw, ImageColor

IDS = ('broken_line', 'last_shelter', 'returned_step')
TITLES = ('The Line That Broke', 'The Last Shelter', 'The Returned Step')
PALETTES = (('#362b24','#766052','#c38a54','#eed7b3'),('#24362d','#566e5a','#78ad86','#d3e2bd'),('#302c3d','#625e78','#b5a6d4','#e2dded'))
LANG = {
    'block.wildercord.battlefield_memorial': 'Marchkeeper Memorial',
    'message.wildercord.battlefield.empty': 'This marker holds no memory of the old ground.',
    'message.wildercord.battlefield.stance': 'Settle into the breathing stance beside the marker, then use it with your blade.',
    'message.wildercord.battlefield.listening': 'Listening to the old ground: %s / %s seconds',
    'message.wildercord.battlefield.broken': 'The memory fades. Keep your stance and stay beside the marker.',
    'message.wildercord.battlefield.remembered': 'Recovered history: %s. Its lesson and field book are yours.',
}
PAGES = (
    (
        'The Line That Broke\n\nThe Marchkeepers held a road for three nights. On the fourth, their captain cut the barricade herself. A line that cannot bend eventually traps the people it protects.',
        'A smith gathered the broken nails and buried them beside her blade. The marker honours the opening she made, not the soldiers who refused to follow. No name survives on its weathered stone.',
        'Lesson: Sunder\n\nPressure can break a foe\'s footing. Write the Sunder intent into a technique from Edge. The crossed blades remember an escape; seek sheltered ruins for the other side of this history.',
    ),
    (
        'The Last Shelter\n\nBeyond the broken road, the wounded planted a garden inside a ruined wall. Their keeper held enemies at the doorway while the last cart left. Her blade never followed the retreat.',
        'Those who returned found roots around the empty scabbard. The Marchkeepers called this place a victory, though no enemy fell here. Keeping someone alive was enough.',
        'Lesson: Bind\n\nA well-placed cut can hold a foe instead of pursuing them. Write the Bind intent into a technique from Edge. Paired cairns mark where the surviving Marchkeepers went next.',
    ),
    (
        'The Returned Step\n\nThe survivors raised two cairns at every turn. One stood for those still walking; the other for those carried home. Their scout retraced the path each dawn so nobody would be forgotten.',
        'Years later, an apprentice followed those stones and found the scout\'s last lesson carved under a watchtower. The Marchkeepers ended as an army and began again as guides.',
        'Lesson: Echo\n\nA stroke can leave an answer behind it. Write the Echo intent into a technique from Edge. These three memories each teach once; other memorials of the same tradition retell the history.',
    ),
)
for i, sid in enumerate(IDS):
    LANG['toast.wildercord.aura.battlefield_'+sid] = TITLES[i]
    for page, text in enumerate(PAGES[i], 1):
        LANG[f'book.wildercord.battlefield.{sid}.{page}'] = text

def write(g):
    variants={}
    for i,(sid,pal) in enumerate(zip(IDS,PALETTES)):
        # Stone has grain, worn raised borders, fractures and an incised tradition mark.
        stone=Image.new('RGBA',(32,32));d=ImageDraw.Draw(stone)
        for x in range(32):
            for y in range(32):
                n=(x*31+y*17+x*y*7)%29
                stone.putpixel((x,y),ImageColor.getcolor('#898779' if n<4 else '#77796d' if n<18 else '#60685d','RGBA'))
        d.rectangle((1,1,30,30),outline='#454d43',width=2);d.line((3,3,28,3),fill='#b5b39d');d.line((4,28,27,28),fill='#444a43')
        d.line([(24,0),(21,9),(25,16),(19,24),(20,31)],fill='#373e36',width=1)
        if i==0: d.line((9,9,22,22),fill=pal[0],width=3);d.line((22,9,9,22),fill=pal[2],width=2)
        elif i==1: d.arc((8,7,24,25),180,360,fill=pal[0],width=3);d.line((9,15,9,24),fill=pal[2],width=2);d.line((23,15,23,24),fill=pal[2],width=2)
        else: d.line([(9,23),(9,17),(15,17),(15,11),(22,11)],fill=pal[0],width=3);d.line((10,22,10,18),fill=pal[2])
        metal=Image.new('RGBA',(16,16),'#55545a');m=ImageDraw.Draw(metal)
        m.line((1,0,1,15),fill='#c7c5ba',width=2);m.line((11,0,11,15),fill='#2d3035',width=3)
        for x,y in [(8,3),(9,4),(6,11),(7,12),(4,6)]:m.point((x,y),fill='#8c6140')
        cloth=Image.new('RGBA',(16,16),pal[1]);c=ImageDraw.Draw(cloth)
        for y in range(16):c.line((0,y,15,y),fill=pal[0] if y%4==0 else pal[1])
        c.line((2,0,2,15),fill=pal[2]);c.line((13,0,13,15),fill=pal[3])
        for texture,im in [('stone',stone),('steel',metal),('cloth',cloth)]:g.save(im,g.ASSETS/f'textures/block/battlefield_{sid}_{texture}.png')
        def cuboid(a,b,tex,rotation=None):
            e={'from':a,'to':b,'faces':{f:{'texture':'#'+tex} for f in ('up','down','north','south','east','west')}}
            if rotation:e['rotation']=rotation
            return e
        elements=[cuboid([1,0,1],[15,3,15],'stone'),cuboid([3,3,4],[13,11,12],'stone'),cuboid([2,5,3.5],[14,7,4],'cloth')]
        for angle in (-22.5,22.5):
            rot={'origin':[8,6,8],'axis':'z','angle':angle}
            elements.extend([cuboid([7.5,2,7],[8.5,13,8],'steel',rot),cuboid([5,11,6.5],[11,12,8.5],'steel',rot),cuboid([7,12,6.5],[9,15,8.5],'cloth',rot)])
        model=f'battlefield_memorial_{i}'
        g.write_json(g.ASSETS/f'models/block/{model}.json',{'textures':{t:f'wildercord:block/battlefield_{sid}_{t}' for t in ('stone','steel','cloth')}|{'particle':f'wildercord:block/battlefield_{sid}_stone'},'elements':elements})
        variants[f'kind={i}']={'model':'wildercord:block/'+model}
        # Bound folio: curved spine, paper edges, brass corners and the same three distinct crests.
        book=Image.new('RGBA',(32,32));b=ImageDraw.Draw(book)
        b.polygon([(5,4),(24,2),(28,7),(28,27),(9,30),(4,25)],fill=pal[0])
        b.polygon([(8,5),(24,4),(26,7),(26,25),(9,28)],fill=pal[1])
        for y in (7,11,15,19,23):b.line((5,y,8,y+1),fill=pal[2])
        b.polygon([(9,27),(26,24),(26,27),(9,30)],fill='#d9c9a5')
        b.line((10,28,24,26),fill='#8f7c5a');b.line((9,29,25,27),fill='#f1e2c0')
        for x,y in [(9,6),(23,5),(10,24),(23,23)]:b.rectangle((x,y,x+2,y+2),fill='#d6af64')
        if i==0:b.line((13,11,22,20),fill=pal[3],width=2);b.line((22,10,13,21),fill=pal[2],width=2)
        elif i==1:b.arc((12,9,23,21),180,360,fill=pal[3],width=2);b.line((13,15,13,21),fill=pal[3],width=2);b.line((22,15,22,20),fill=pal[2],width=2)
        else:b.line([(13,21),(13,17),(17,17),(17,13),(22,13)],fill=pal[3],width=2)
        path='battlefield_'+sid;g.save(book,g.ASSETS/f'textures/item/{path}.png')
        g.write_json(g.ASSETS/f'models/item/{path}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'wildercord:item/'+path}})
        g.write_json(g.ASSETS/f'items/{path}.json',{'model':{'type':'minecraft:model','model':'wildercord:item/'+path}})
    g.write_json(g.ASSETS/'blockstates/battlefield_memorial.json',{'variants':variants})
    g.write_json(g.DATA/'worldgen/structure/old_battlefield.json',{'type':'wildercord:dungeon','dungeon':'old_battlefield','biomes':'#wildercord:has_structure/old_battlefield','spawn_overrides':{},'step':'surface_structures','terrain_adaptation':'none'})
    g.write_json(g.DATA/'worldgen/structure_set/old_battlefields.json',{'placement':{'type':'minecraft:random_spread','salt':202610021,'spacing':64,'separation':24},'structures':[{'structure':'wildercord:old_battlefield','weight':1}]})
    g.write_json(g.DATA/'tags/worldgen/biome/has_structure/old_battlefield.json',{'replace':False,'values':['minecraft:plains','minecraft:sunflower_plains','minecraft:meadow','minecraft:savanna','minecraft:snowy_plains']})
    g.write_json(g.DATA/'tags/worldgen/structure/old_battlefield.json',{'replace':False,'values':['wildercord:old_battlefield']})
    g.write_json(g.DATA/'loot_table/chests/old_battlefield.json',{'type':'minecraft:chest','pools':[{'rolls':{'type':'minecraft:uniform','min':3,'max':5},'entries':[{'type':'minecraft:item','name':'minecraft:'+item,'weight':weight} for item,weight in [('iron_nugget',6),('paper',5),('string',4),('leather',3),('bread',3),('iron_ingot',1)]]}]})
