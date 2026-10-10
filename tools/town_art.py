"""0.12 "Tempering" town life at the Wayfarer Inn: the bounty board (block, model), the Ridgeback Deed (icon, model) and the
keepers', bounties' and reputation's text. Behaviour is in src/main/java/dev/wildercord/town/."""
from item_art import Canvas, hexc, mix
from world_art import noise

RSQ = '\N{RIGHT SINGLE QUOTATION MARK}'

LANG = {
    'block.wildercord.bounty_board': 'Bounty Board',
    'item.wildercord.ridgeback_deed': 'Ridgeback Deed',
    'tooltip.wildercord.ridgeback_deed': 'Use it and a tame, saddled Ridgeback Stag of yours is led up beside you.',
    'entity.wildercord.wayfarer_keeper': 'Wayfarer Keeper',
    'entity.wildercord.wayfarer_keeper.cook': 'Inn Cook',
    'entity.wildercord.wayfarer_keeper.stablemaster': 'Stablemaster',
    'entity.wildercord.wayfarer_keeper.emissary': f'Master{RSQ}s Emissary',
    'town.wildercord.tier.stranger': 'Stranger',
    'town.wildercord.tier.known': 'Known',
    'town.wildercord.tier.friend': 'Friend',
    'town.wildercord.tier.honoured': 'Honoured',
    'message.wildercord.bounty.offer': 'Bounty: slay %s %s within %s blocks of this board.',
    'message.wildercord.bounty.reward': 'Pays %s emeralds and %s reputation with the inn.',
    'message.wildercord.bounty.accept': 'Click the board again to take it.',
    'message.wildercord.bounty.taken': f'Bounty taken: slay %s %s. Kills count near this board; come back when it{RSQ}s done.',
    'message.wildercord.bounty.hunting': 'Your bounty: %s of %s %s slain.',
    'message.wildercord.bounty.abandon_hint': 'Sneak and click the board to give it up.',
    'message.wildercord.bounty.abandoned': 'You gave up the %s bounty.',
    'message.wildercord.bounty.tomorrow': f'You{RSQ}ve done today{RSQ}s bounty. The board has nothing new for you until tomorrow.',
    'message.wildercord.bounty.paid': 'Bounty paid: %s emeralds and %s reputation.',
    'message.wildercord.bounty.tier_up': f'The inn{RSQ}s keepers now count you %s. They have more to sell you.',
    'message.wildercord.bounty.standing': 'Standing: %s (%s reputation; %s for %s).',
    'message.wildercord.bounty.standing_top': 'Standing: %s (%s reputation).',
    'message.wildercord.bounty.progress': 'Bounty: %s / %s %s',
    'message.wildercord.bounty.complete': 'Bounty done (%s). Turn it in at the board.',
    'message.wildercord.bounty.offer_gather': 'Bounty: bring this board %s %s.',
    'message.wildercord.bounty.offer_elite': 'Bounty: a named elite, %s, is loose within %s blocks of here. Hunt it down.',
    'message.wildercord.bounty.offer_dungeon': f'Bounty: slay a dungeon{RSQ}s guardian, wherever you find one.',
    'message.wildercord.bounty.offer_great': 'Great bounty of the week: slay %s %s within %s blocks of this board.',
    'message.wildercord.bounty.reward_great': 'Pays %s emeralds, %s Mana Crystals and %s reputation.',
    'message.wildercord.bounty.gathering': 'Your bounty: you carry %s of the %s %s it asks for.',
    'message.wildercord.bounty.taken_gather': 'Bounty taken: bring %s %s back to this board.',
    'message.wildercord.bounty.taken_elite': 'Bounty taken: %s was last seen to the %s. Only you can claim it.',
    'message.wildercord.bounty.taken_dungeon': f'Bounty taken: slay any dungeon{RSQ}s guardian, then come back.',
    'message.wildercord.bounty.elite_hidden': f'The elite{RSQ}s trail has gone cold. Try again in a moment.',
    'message.wildercord.bounty.opens.gather': 'The board now offers you gathering bounties, and a great bounty each week.',
    'message.wildercord.bounty.opens.elite': 'The board now offers you named elites to hunt.',
    'message.wildercord.bounty.opens.dungeon': f'The board now offers you bounties on dungeon guardians.',
    'town.wildercord.named_elite': '%s the %s',
    'town.wildercord.dungeon_guardian': 'dungeon guardian',
    'town.wildercord.way.north': 'north',
    'town.wildercord.way.south': 'south',
    'town.wildercord.way.east': 'east',
    'town.wildercord.way.west': 'west',
    'message.wildercord.keeper.more': 'Take bounties at the board: as %s they will sell you more.',
    'message.wildercord.keeper.honoured': f'The keepers hold you Honoured: you see everything they have.',
}

WOOD = [hexc(c) for c in ('#3A2614', '#553820', '#6E4A2C', '#87603A', '#9E7448')]
PAPER = [hexc(c) for c in ('#BBAE90', '#D8CCAA', '#EEE4C8')]
INK = hexc('#3A3028')
WAX = hexc('#A82A22')


def planks(seed):
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            c = WOOD[2] if noise(x, y // 4, seed) < 0.6 else WOOD[3]
            if y % 4 == 3:
                c = WOOD[1]
            if noise(x, y, seed + 3) > 0.92:
                c = WOOD[4]
            cv.put(x, y, c)
    return cv


def board_front():
    cv = planks(21)
    # A frame round the edge and three notices pinned up, one with a wanted creature drawn on it.
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            cv.put(x, y, WOOD[0] if (x, y) in ((0, 0), (15, 0), (0, 15), (15, 15)) else WOOD[1])
    notices = ((2, 2, 6, 7), (9, 3, 13, 9), (3, 9, 8, 13))
    for n, (x0, y0, x1, y1) in enumerate(notices):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                c = PAPER[1] if noise(x, y, 40 + n) < 0.7 else PAPER[2]
                if y == y1 or x == x1:
                    c = PAPER[0]
                cv.put(x, y, c)
        for y in range(y0 + 2, y1, 2):
            for x in range(x0 + 1, x1):
                if noise(x, y, 60 + n) < 0.65:
                    cv.put(x, y, mix(INK, PAPER[1], 0.35))
        cv.put((x0 + x1) // 2, y0, WAX if n != 1 else hexc('#8A8E95'))
    # The wanted notice: a dark beast's head with two eyes.
    for (x, y) in ((10, 5), (11, 5), (12, 5), (10, 6), (11, 6), (12, 6), (11, 7)):
        cv.put(x, y, INK)
    cv.put(10, 5, hexc('#E8D040'))
    cv.put(12, 5, hexc('#E8D040'))
    return cv.image()


def board_side():
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv.put(x, y, WOOD[1] if noise(x, y, 33) < 0.6 else WOOD[2])
    return cv.image()


def deed_icon():
    cv = Canvas()
    outline = mix(PAPER[0], (0, 0, 0), 0.55)
    # A rolled parchment, tied with a cord and sealed in red wax.
    for y in range(3, 13):
        for x in range(3, 13):
            if abs((x - 8) + (y - 8)) > 6 or abs((x - 8) - (y - 8)) > 3:
                continue
            edge = abs((x - 8) + (y - 8)) == 6 or abs((x - 8) - (y - 8)) == 3
            t = ((x - 8) - (y - 8) + 3) / 6
            cv.put(x, y, outline if edge else PAPER[2] if t < 0.35 else PAPER[1] if t < 0.75 else PAPER[0])
    for (x, y) in ((3, 9), (4, 10), (5, 11), (6, 12), (2, 10), (3, 11), (4, 12)):
        cv.put(x, y, PAPER[0] if (x + y) % 2 else outline)
    for (x, y) in ((7, 7), (8, 8), (9, 9), (7, 9), (9, 7)):
        cv.put(x, y, hexc('#6A4426'))
    for (x, y) in ((8, 9), (9, 8), (8, 10), (10, 8), (9, 10), (10, 9), (10, 10)):
        cv.put(x, y, WAX)
    cv.put(9, 9, mix(WAX, (255, 255, 255), 0.35))
    return cv.image()


def write(g):
    g.save(board_front(), g.ASSETS / 'textures/block/bounty_board_front.png')
    g.save(board_side(), g.ASSETS / 'textures/block/bounty_board_side.png')
    face = {'texture': '#front'}
    side = {'texture': '#side'}
    g.write_json(g.ASSETS / 'models/block/bounty_board.json', {'parent': 'minecraft:block/block', 'textures': {
        'particle': 'wildercord:block/bounty_board_side', 'front': 'wildercord:block/bounty_board_front', 'side': 'wildercord:block/bounty_board_side'},
        'elements': [{'from': [0, 0, 6], 'to': [16, 16, 10], 'faces': {
            'north': {**face, 'uv': [0, 0, 16, 16]}, 'south': {**face, 'uv': [0, 0, 16, 16]},
            'east': {**side, 'uv': [6, 0, 10, 16]}, 'west': {**side, 'uv': [6, 0, 10, 16]},
            'up': {**side, 'uv': [0, 6, 16, 10]}, 'down': {**side, 'uv': [0, 6, 16, 10]}}}]})
    g.write_json(g.ASSETS / 'blockstates/bounty_board.json', {'variants': {
        f'facing={d}': {'model': 'wildercord:block/bounty_board', **({'y': r} if r else {})}
        for d, r in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}})
    g.write_json(g.ASSETS / 'items/bounty_board.json', {'model': {'type': 'minecraft:model', 'model': 'wildercord:block/bounty_board'}})
    g.save(deed_icon(), g.ASSETS / 'textures/item/ridgeback_deed.png')
    g.item_model('ridgeback_deed', 'ridgeback_deed')
    g.write_json(g.ASSETS / 'items/ridgeback_deed.json', {'model': {'type': 'minecraft:model', 'model': 'wildercord:item/ridgeback_deed'}})
