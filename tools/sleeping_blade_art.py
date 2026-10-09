"""Oathkeeper's shaped steel, wrapped grip, carved stone socket and Marchkeeper oath folio."""
from PIL import Image, ImageDraw

LANG = {
 'block.wildercord.sleeping_blade_stone':'The Sleeping Blade',
 'item.wildercord.oathkeeper':'Oathkeeper',
 'item.wildercord.oathkeeper.lore':'The last shelter needed an answer, not another pursuit.',
 'item.wildercord.oathkeeper.use':'While bonded and held: guard costs 15% less; perfect guards build 30% more momentum; ordinary hits build 20% less.',
 'toast.wildercord.aura.sleeping_blade':'The Last Oath',
 'message.wildercord.sleeping_blade.empty':'This stone holds no sleeping blade.',
 'message.wildercord.sleeping_blade.off':'Aura and bonded blades must be enabled to draw this blade.',
 'message.wildercord.sleeping_blade.bonded':'You already carry a blade bond. Release it deliberately from the Aura page before seeking another.',
 'message.wildercord.sleeping_blade.intent':'Reach Form, empty your main hand, kneel on solid ground beside the blade, then use its stone.',
 'message.wildercord.sleeping_blade.busy':'Another swordsman is drawing this blade. Wait for their intent to settle.',
 'message.wildercord.sleeping_blade.begin':'The stone listens. Keep kneeling, still and unhurt, with an empty main hand.',
 'message.wildercord.sleeping_blade.progress':'Drawing the sleeping blade: %s / %s seconds',
 'message.wildercord.sleeping_blade.broken':'Your intent falters. The blade settles back into the stone.',
 'message.wildercord.sleeping_blade.failed':'The bond could not take hold. The blade remains in its stone.',
 'message.wildercord.sleeping_blade.claimed':'Oathkeeper answers your intent and bonds to you. Its last oath is yours to read.',
 'message.wildercord.sleeping_blade.drawn':'This blade has found its swordsman. The empty stone keeps the last oath.',
 'book.wildercord.last_oath.1':"The Last Oath\n\nThe Marchkeepers' scout returned to the shelter after the carts had gone. She found the keeper's blade, carried it to open country, and set it in stone. Nobody would command its next hand.",
 'book.wildercord.last_oath.2':"A hand that grasps another blade cannot receive this one. From Form, kneel beside the stone with an empty main hand. Stay still and unhurt for six seconds. The first completed draw gives the site its lasting owner.",
 'book.wildercord.last_oath.3':"Oathkeeper rewards an answer: guard costs 15% less; perfect guards build 30% more momentum. Ordinary hits build 20% less. Its bond grows through real deeds. The empty stone preserves this oath for later travellers.",
}
for sid, text in [('listen','Stone listens'),('strain','Steel strains in stone'),('draw','The sleeping blade is drawn'),('answer','Oathkeeper answers a perfect guard')]:
    LANG['subtitles.wildercord.kit.sleeping_blade.'+sid]=text

def box(a,b,t,uv=(0,0,16,16)):
    return {'from':a,'to':b,'faces':{f:{'texture':'#'+t,'uv':list(uv)} for f in ('up','down','north','south','east','west')}}

def write(g):
    import forged_weapon_art as forged
    tex=forged.textures('oathkeeper')
    tex['particle']=tex['stone']
    variants={}
    for phase in range(5):
        rock=[box([1,0,1],[15,6,15],'stone'),box([3,6,3],[13,9,13],'stone'),box([0,0,4],[4,4,13],'stone'),box([12,0,2],[16,3,12],'stone'),
              box([4,5.9,1],[12,6.2,2],'brass'),box([5,9,5],[11,9.2,11],'dark')]
        if phase<4:rock+=forged.embedded_sword(phase*1.5)
        g.write_json(g.ASSETS/f'models/block/sleeping_blade_{phase}.json',{'textures':tex,'elements':rock})
        for facing,angle in [('north',0),('east',90),('south',180),('west',270)]:
            variants[f'facing={facing},phase={phase}']={'model':f'wildercord:block/sleeping_blade_{phase}','y':angle}
    g.write_json(g.ASSETS/'blockstates/sleeping_blade_stone.json',{'variants':variants})
    # The held blade, its inventory sprite and its item definition come from forged_weapon_art with the other forged weapons.
    book=Image.new('RGBA',(32,32));d=ImageDraw.Draw(book)
    d.polygon([(4,5),(24,2),(28,6),(28,27),(8,31),(3,26)],fill='#1b2d2b')
    d.polygon([(8,6),(24,4),(26,7),(26,25),(8,28)],fill='#49655b')
    for y in (8,12,16,20,24):d.line((4,y,7,y+1),fill='#c5a671',width=2)
    d.polygon([(8,28),(26,25),(26,28),(8,31)],fill='#e3d6ae');d.line((10,29,25,27),fill='#8c805e')
    d.rectangle((10,8,23,24),outline='#a5b9a2');d.line((16,10,16,21),fill='#e1ded0',width=2)
    d.line((12,19,21,17),fill='#d4b477',width=2);d.line((16,22,18,21),fill='#d4b477',width=2)
    for x,y in ((9,7),(22,5),(9,25),(24,23)):d.rectangle((x,y,x+2,y+2),fill='#cab17b')
    g.save(book,g.ASSETS/'textures/item/last_oath.png')
    g.write_json(g.ASSETS/'models/item/last_oath.json',{'parent':'minecraft:item/generated','textures':{'layer0':'wildercord:item/last_oath'}})
    g.write_json(g.ASSETS/'items/last_oath.json',{'model':{'type':'minecraft:model','model':'wildercord:item/last_oath'}})
    # Native sword behavior and both Aura/bond tags are intentional, not inferred from the item name.
    g.write_json(g.DATA.parent/'minecraft/tags/item/swords.json',{'replace':False,'values':['wildercord:oathkeeper']})
    g.write_json(g.DATA/'worldgen/structure/sleeping_blade.json',{'type':'wildercord:dungeon','dungeon':'sleeping_blade','biomes':'#wildercord:has_structure/sleeping_blade','spawn_overrides':{},'step':'surface_structures','terrain_adaptation':'none'})
    g.write_json(g.DATA/'worldgen/structure_set/sleeping_blades.json',{'placement':{'type':'minecraft:random_spread','salt':202610023,'spacing':92,'separation':36},'structures':[{'structure':'wildercord:sleeping_blade','weight':1}]})
    g.write_json(g.DATA/'tags/worldgen/biome/has_structure/sleeping_blade.json',{'replace':False,'values':['minecraft:meadow','minecraft:windswept_hills','minecraft:windswept_gravelly_hills','minecraft:stony_peaks','minecraft:plains']})
    g.write_json(g.DATA/'tags/worldgen/structure/sleeping_blade.json',{'replace':False,'values':['wildercord:sleeping_blade']})
