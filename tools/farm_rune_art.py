"""Hand-drawn rune icons for the farmstead runes (fx-farm pack; merged into item_art.GLYPHS).

Same format as item_art.GLYPHS: a pictogram at most 9 wide and 9 tall, "." empty, "-" shade,
"#" main, "+" light, "*" core (the stone's own glow colours), "o" darkens the stone, "k" near-black,
"w" white.
"""

GLYPHS: dict[str, str] = {
    # ---------------------------------------------------------------- the field
    # Furrow: a hoe's blade over ploughed ridges.
    "tillage": """
        .....##..
        ....#+#..
        ...#.....
        ..#......
        .........
        #.#.#.#.#
        +#+#+#+#+
        #########
    """,
    # Dewfall: drops falling onto a wet ridge.
    "dewfall": """
        ..+...+..
        .+#+.+#+.
        ..#...#..
        ....+....
        ...+#+...
        ....#....
        .........
        -#-#-#-#-
        #########
    """,
    # Tilth: a clod broken in two, a root curling out of it.
    "tilth": """
        .........
        .###.###.
        #+##.##+#
        ####.####
        .##...##.
        ....#....
        ...#.....
        ....#....
        ...#.....
    """,
    # Plowline: a straight furrow running off into the distance.
    "plowline": """
        ....#....
        ....#....
        ...#+#...
        ...#+#...
        ..#+*+#..
        ..#+*+#..
        .#+***+#.
        .#+***+#.
        #########
    """,
    # Sow: a hand scattering seeds onto a ridge.
    "sow": """
        ##.......
        ###.+....
        .###..+..
        ..##.....
        ......+..
        ....+....
        ........+
        -#-#-#-#-
        #########
    """,
    # Ripen: a wheat ear swelling beside an hourglass.
    "ripen": """
        ###...+..
        .#...+#+.
        .*...+#+.
        .#...+#+.
        ###..+#+.
        ......#..
        ......#..
        ......#..
    """,
    # Dewkeep: a cloud hanging over farmland, dripping.
    "dewkeep": """
        ..####...
        .#++++#..
        #++++++#.
        .######..
        ..+..+...
        .....+...
        ..+......
        -#-#-#-#-
        #########
    """,
    # Fieldsense: an eye over a row of crops, one shining.
    "fieldsense": """
        ..#####..
        .#.+*+.#.
        ..#####..
        .........
        .#..*..#.
        .#.+#+.#.
        .#..#..#.
        #########
    """,
    # Thawfield: a flame melting a snow layer.
    "thawfield": """
        ....#....
        ...#+#...
        ..#+*+#..
        ..#***#..
        ...###...
        .+.....+.
        .........
        wwwwwwwww
        #########
    """,
    # Cloche: a glass bell over a sprout, a clock tick on its top.
    "cloche": """
        ....*....
        ..#####..
        .#.....#.
        #...#...#
        #..+#+..#
        #...#...#
        #...#...#
        #########
    """,
    # Scarecrow: a figure on a cross with a straw hat.
    "scarecrow": """
        ...###...
        ..#####..
        ...+*+...
        #########
        ....#....
        ...#+#...
        ....#....
        ....#....
        ...###...
    """,
    # Fallow: flat, resting earth with a single dry stalk.
    "fallow": """
        .........
        .........
        ......#..
        .....#...
        ......#..
        .........
        ---------
        #########
        #+#+#+#+#
    """,
    # Ditchwater: a channel cut in the ground, water in it.
    "ditchwater": """
        .........
        .........
        ##.....##
        ##.....##
        ##+-+-+##
        ##-+-+-##
        #########
        #########
    """,
    # Compost: a barrel with a sprout rising from its rich top.
    "compost": """
        ....#....
        ...#+#...
        ....#....
        #.......#
        #*-*-*-*#
        #-------#
        #-------#
        #########
    """,
    # Stalkrise: segmented cane stalks with a rising tip.
    "stalkrise": """
        ....*....
        ...*.*...
        .#.....#.
        .#..#..#.
        .##.#.##.
        .#..#..#.
        .#.##..#.
        .#..#..#.
        #########
    """,
    # Gourdcall: a round pumpkin on a curling stem.
    "gourdcall": """
        ....#.#..
        ....#..#.
        .###.###.
        #+#+#+#+#
        #+#+#+#+#
        #+#+#+#+#
        .#######.
    """,
    # Berrybless: a bush hung with bright berries.
    "berrybless": """
        ...###...
        .##*##*#.
        #*#####*#
        ##*###*##
        .#######.
        ..*#.#*..
        ....#....
        ....#....
    """,
    # ---------------------------------------------------------------- the herd
    # Courtship: two hearts, overlapping.
    "courtship": """
        .##.##...
        #++#++#..
        #+++.##.#
        .#+#++#+#
        ..#+++++#
        ...#+++#.
        ....#+#..
        .....#...
    """,
    # Herdcall: a shepherd's crook with a call.
    "herdcall": """
        ..###....
        .#...#...
        .#...#.+.
        .....#.#.
        .....#.#.
        .....#...
        .....#.+.
        .....#...
        .....#...
    """,
    # Fleece: a fluffy sheep's body under shears.
    "fleece": """
        .#.....#.
        ..#...#..
        ...#.#...
        ....#....
        .+++++++.
        ++w+w+w++
        +w+w+w+w+
        .+++++++.
        .#.#.#.#.
    """,
    # Milkmaid: a bucket brimming white.
    "milkmaid": """
        ..#####..
        .#.....#.
        #wwwwwww#
        #+wwwww+#
        #+++++++#
        .#+++++#.
        .#+++++#.
        ..#####..
    """,
    # Henhouse: an egg in a nest.
    "henhouse": """
        ...###...
        ..#www#..
        .#wwww+#.
        .#www++#.
        ..#+++#..
        #-#####-#
        .#-#-#-#.
        ..#####..
    """,
    # Gentlehand: an open palm with a little heart above it.
    "gentlehand": """
        ...#.#...
        ..#*#*#..
        ...#*#...
        ....#....
        #.#.#.#..
        #+#+#+#..
        #+++++#.#
        #++++++#.
        .######..
    """,
    # Fodder: a bale of hay, bound.
    "fodder": """
        .........
        #########
        #+#+#+#+#
        ####*####
        #+#+*+#+#
        ####*####
        #+#+#+#+#
        #########
    """,
    # Barnwarmth: a barn with a warm glow in its door.
    "barnwarmth": """
        ....#....
        ...#+#...
        ..#+++#..
        .#+++++#.
        #+++++++#
        .#++*++#.
        .#+***+#.
        .#+***+#.
        .#######.
    """,
    # Herdsense: an eye seeing three glowing heads.
    "herdsense": """
        ..#####..
        .#..*..#.
        #..*#*..#
        .#..*..#.
        ..#####..
        .........
        *...*...*
        #...#...#
    """,
    # ---------------------------------------------------------------- the kitchen
    # Hearthcook: a drumstick over flames.
    "hearthcook": """
        .....##..
        ....#++#.
        ...#+++#.
        ..#+++#..
        .#.###...
        #.#......
        ..+.+.+..
        .+*+*+*+.
        #########
    """,
    # Stewpot: a bowl steaming.
    "stewpot": """
        ..+..+...
        ...+..+..
        ..+..+...
        .........
        #########
        #+*+*+*+#
        .#+++++#.
        ..#####..
    """,
    # Bakehouse: a loaf of bread, scored.
    "bakehouse": """
        .........
        ..#####..
        .#+#+#+#.
        #+#+#+#+#
        #+++++++#
        #+++++++#
        .#######.
    """,
    # ---------------------------------------------------------------- the hive
    # Pollinate: a bee over a flower.
    "pollinate": """
        .ww.ww...
        .#####...
        #k#k#k#..
        .#####...
        .......+.
        .....+*+.
        ......+..
        ......#..
        ......#..
    """,
    # Hivehum: a hive skep, honey dripping.
    "hivehum": """
        ...###...
        ..#+++#..
        .#######.
        .#+++++#.
        #########
        #++.k.++#
        #########
        ...*.*...
        .....*...
    """,
    # Calmsmoke: a smoker puffing curls.
    "calmsmoke": """
        ..-..-...
        .-..-..-.
        ..-..-...
        ...-.....
        ..###....
        .#+++#...
        .#+*+#...
        .#+++#...
        ..###....
    """,
    # ---------------------------------------------------------------- the wood
    # Wildflower: three flowers of a meadow.
    "wildflower": """
        .+...*...
        +#+.*#*..
        .+...*.+.
        .#...#+#+
        .#.+.#.+.
        .#+#+#.#.
        .#.+.#.#.
        .#...#.#.
        #########
    """,
    # Saplingrise: a sapling with an arrow climbing.
    "saplingrise": """
        ....*....
        ...*+*...
        ..*.+.*..
        ....+....
        .##.#.##.
        #++##++#.
        .##.#.##.
        ....#....
        ..#####..
    """,
    # Saplingsow: two small saplings in a row.
    "saplingsow": """
        .........
        .#.....#.
        #+#...#+#
        .#.....#.
        .#.....#.
        .#.....#.
        ---------
        #########
    """,
    # Leaffall: leaves drifting down.
    "leaffall": """
        .##......
        #++#.....
        .##...##.
        .....#++#
        ..##..##.
        .#++#....
        ..##.....
        ......##.
        .....#++#
    """,
    # Barkstrip: a log, one end bare and pale.
    "barkstrip": """
        .........
        .#######.
        #-#-#++++
        #-#-#++++
        #-#-#++++
        #-#-#++++
        .#######.
    """,
    # Coppice: a stump with a fresh shoot.
    "coppice": """
        ....+....
        ...+#+...
        ....#....
        .........
        .#######.
        #+#*#*#+#
        #-------#
        #-------#
        #########
    """,
    # ---------------------------------------------------------------- the table and the lane
    # Feastday: a laden table under a crown.
    "feastday": """
        .#.#.#...
        .#####...
        .........
        .*+.+*.+.
        .***.**+*
        #########
        .#.....#.
        .#.....#.
    """,
    # Picnic: a checked cloth with an apple.
    "picnic": """
        ....#....
        ...*#*...
        ..*****..
        ...***...
        .........
        #+#+#+#+#
        +#+#+#+#+
        #+#+#+#+#
    """,
    # Honeydew: a honey bottle.
    "honeydew": """
        ...###...
        ....#....
        ...#.#...
        ..#***#..
        .#*****#.
        .#*****#.
        .#*****#.
        ..#####..
    """,
    # Canopy: a broad leaf roof.
    "leafshade": """
        .#######.
        #+++++++#
        #++#+#++#
        .###.###.
        ....#....
        ....#....
        ....#....
        ...###...
    """,
    # Barkhide: a shield of bark plates.
    "barkhide": """
        #########
        #-#-#-#-#
        #-#-#-#-#
        #-#-#-#-#
        .#-#-#-#.
        .#-#-#-#.
        ..#-#-#..
        ...###...
    """,
    # Sapflow: a drop rising in a stem.
    "sapflow": """
        ....*....
        ...*+*...
        ...*+*...
        ....*....
        ...#.#...
        ...#+#...
        ...#+#...
        ...#+#...
        ..##.##..
    """,
    # Trot: a horseshoe with dust kicked up.
    "trot": """
        .#######.
        #+#...#+#
        #+.....+#
        #+.....+#
        #+.....+#
        .#.....#.
        .........
        --.....--
        -.......-
    """,
    # Beeline: a bee darting along a dotted line.
    "beeline": """
        .....ww..
        .....ww..
        ....####.
        ....#kk##
        +.+.####.
        .........
        .........
    """,
    # Fieldstride: a boot mid-stride over furrows.
    "fieldstride": """
        ...###...
        ...#+#...
        ...#+#...
        ...#+###.
        ..#++++#-
        ..######-
        .........
        #.#.#.#.#
        #########
    """,
    # Hayloft: a hay bale with a toss rising from it.
    "hayloft": """
        ....*....
        ...***...
        ..*.*.*..
        ....*....
        .........
        #########
        #+#+#+#+#
        #+#+#+#+#
        #########
    """,
}
