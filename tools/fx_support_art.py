"""Hand-drawn rune icons for the support pack (Worst First to Faithful), merged into item_art.GLYPHS.

Same format as item_art.GLYPHS: "." empty, "-" shade, "#" main, "+" light, "*" core, "o" darkens the stone,
"k" near-black, "w" white.
"""

GLYPHS: dict[str, str] = {
    # Worst First: a cross with a bright heart at its centre.
    "worst_first": """
        ..###..
        ..#+#..
        ###+###
        #++*++#
        ###+###
        ..#+#..
        ..###..
    """,
    # Salve: a drop of balm.
    "salve": """
        ...#...
        ..#+#..
        .#+++#.
        #++*++#
        #+***+#
        .#+++#.
        ..###..
    """,
    # Mending Mist: a cloud with a small cross under it.
    "mending_mist": """
        ..##...
        .#++##.
        #+++++#
        .#####.
        ...*...
        ..***..
        ...*...
    """,
    # Hearthglow: a flame under a roof.
    "hearthglow": """
        ...#...
        ..#.#..
        .#...#.
        #..*..#
        #.*+*.#
        #.+*+.#
        #######
    """,
    # Aftercare: a heart with a tick of time beside it.
    "aftercare": """
        .##.##.
        #++#++#
        #+++++#
        .#+*+#.
        ..#+#..
        ...#...
        .*.*.*.
    """,
    # Hearthsong: a note over a heart.
    "hearthsong": """
        ...##..
        ...#.#.
        ..*#...
        .**....
        ##.##..
        #+#+#..
        .#+#...
    """,
    # Grace: a halo over a kneeling figure.
    "grace": """
        .+++++.
        +.....+
        .+++++.
        ...#...
        ..###..
        .#.#.#.
        ..#.#..
    """,
    # Managift: an orb passing from one hand to another.
    "managift": """
        ...*...
        ..*+*..
        ...*...
        .......
        #.....#
        ##...##
        .##+##.
    """,
    # Manawell: a well brimming with light.
    "manawell": """
        .#####.
        ...#...
        .+*+*+.
        #+***+#
        #.....#
        #.....#
        #######
    """,
    # Guardlink: two links of a chain.
    "guardlink": """
        ###....
        #.#....
        #.###..
        ###+###
        ..###.#
        ....#.#
        ....###
    """,
    # Rally: a banner on a pole.
    "rally": """
        #####..
        #+++#..
        #+*+#..
        #+++#..
        ###.#..
        #......
        #......
    """,
    # Morale: three small hearts.
    "morale": """
        .#.#...
        #+#+#..
        .#+#.#.
        ..#.#+#
        .#.#.#.
        #+#+#..
        .#+#...
    """,
    # Shrug Off: a weight lifting off, an arrow rising.
    "shrug_off": """
        ...*...
        ..***..
        .*.*.*.
        ...*...
        .......
        #######
        #+++++#
    """,
    # Hexguard: a shield striking out a curse.
    "hexguard": """
        #######
        #*...*#
        #.*.*.#
        #..*..#
        .#*.*#.
        .##.##.
        ...#...
    """,
    # Stoutheart: a heart inside a shield.
    "stoutheart": """
        #######
        #.....#
        #+#.#+#
        #.+*+.#
        .#.+.#.
        ..#.#..
        ...#...
    """,
    # Ironhold: an anvil.
    "ironhold": """
        #######
        .#+++##
        ..###..
        ...#...
        ..###..
        .#####.
        .......
    """,
    # Evade: a figure slipping aside, trailing lines.
    "evade": """
        ....##.
        -.-.##.
        ...###.
        -.-.#.#
        ...#...
        -.-#.#.
        ...#..#
    """,
    # Emberguard: a flame held inside a shield.
    "emberguard": """
        #######
        #..*..#
        #.*+*.#
        #.*+*.#
        .#.*.#.
        ..#.#..
        ...#...
    """,
    # Beastguard: a paw print.
    "beastguard": """
        .#...#.
        .#.#.#.
        ...#...
        #.....#
        ..###..
        .#+*+#.
        ..###..
    """,
    # Hearthguard: a house with a small shield on its door.
    "hearthguard": """
        ...#...
        ..#.#..
        .#...#.
        #######
        #.###.#
        #.#*#.#
        #..#..#
    """,
    # Heel: a whistle with its note.
    "heel": """
        .....*.
        ....*.*
        .....*.
        ####...
        #++###.
        #+++++#
        .#####.
    """,
    # Bellward: a bell.
    "bellward": """
        ...#...
        ..###..
        .#+++#.
        .#+++#.
        #+++++#
        #######
        ...*...
    """,
    # Sanctuary: a circle with its quiet centre.
    "sanctuary": """
        ..###..
        .#...#.
        #..+..#
        #.+*+.#
        #..+..#
        .#...#.
        ..###..
    """,
    # Arrowveil: an arrow stopped under a dome.
    "arrowveil": """
        ..###..
        .#...#.
        #.....#
        ...*...
        ...+...
        ...+...
        ..+.+..
    """,
    # Blastward: a burst held inside a box.
    "blastward": """
        #######
        #*.+.*#
        #.*+*.#
        #++*++#
        #.*+*.#
        #*.+.*#
        #######
    """,
    # Firebreak: a flame struck through.
    "firebreak": """
        #..*...
        .#*+*..
        ..#+*..
        .*+#+*.
        .*++#*.
        ..***#.
        ......#
    """,
    # Pacify: a closed, sleeping eye.
    "pacify": """
        .......
        .......
        #.....#
        .#...#.
        ..###..
        .+.+.+.
        .......
    """,
    # Lure: a crook drawing inward.
    "lure": """
        ..###..
        .#...#.
        .#...#.
        ....#..
        ...#...
        ..*....
        .*+*...
    """,
    # Stillbind: a pin through two rings.
    "stillbind": """
        ...#...
        ...#...
        .+###+.
        +..#..+
        .+###+.
        ...#...
        ...*...
    """,
    # Taunt: a raised fist.
    "taunt": """
        .#.#.#.
        #+#+#+#
        #+++++#
        #+++++#
        .#####.
        ..#*#..
        ..###..
    """,
    # Nudge: an open hand and the air it pushes.
    "nudge": """
        .#.....
        .#.#...
        .#.#.#.
        ######.
        ######.
        .####.-
        ..##.-.
    """,
    # Hobble: a shackle round an ankle.
    "hobble": """
        ..#....
        ..#....
        ..#....
        .###+++
        .#+#..+
        .###+++
        .......
    """,
    # Corral: a ring of fence posts.
    "corral": """
        #.#.#.#
        #######
        #.....#
        #..*..#
        #.....#
        #######
        #.#.#.#
    """,
    # Truce: a white flag.
    "truce": """
        #wwww..
        #wwwww.
        #wwww..
        #......
        #......
        #......
        #......
    """,
    # Spook: a ghost.
    "spook": """
        ..###..
        .#+++#.
        #+k+k+#
        #+++++#
        #++k++#
        #+++++#
        #.#.#.#
    """,
    # Aegis: a great shield with a star.
    "aegis": """
        #######
        #+++++#
        #++*++#
        #+***+#
        .#+*+#.
        ..#+#..
        ...#...
    """,
    # Accord: two hands clasped.
    "accord": """
        .......
        ##...##
        .##.##.
        ..#*#..
        .##.##.
        ##...##
        .......
    """,
    # Citadel: a tower with battlements.
    "citadel": """
        #.#.#.#
        #######
        .#+++#.
        .#+*+#.
        .#+++#.
        .#+#+#.
        #######
    """,
    # Shieldwall: three shields side by side.
    "shieldwall": """
        ........
        ##.##.##
        #+.#+.#+
        #+.#+.#+
        .#..#..#
        ........
        ++++++++
    """,
    # Staunch: a drop with a bar across it.
    "staunch": """
        ...#...
        ..#.#..
        .#...#.
        #######
        #.....#
        .#...#.
        ..###..
    """,
    # Sentry: an open, watching eye.
    "sentry": """
        .......
        ..###..
        .#+++#.
        #++*++#
        .#+++#.
        ..###..
        .......
    """,
    # Tend: a leaf with a small cross.
    "tend": """
        ....###
        ..##++#
        .#++*+#
        #+***+#
        #++*+#.
        #+++#..
        ###....
    """,
    # Soothe: three gentle waves.
    "soothe": """
        .......
        .##..##
        #..##..
        .......
        .##..##
        #..##..
        .......
    """,
    # Withdraw: footsteps fading away.
    "withdraw": """
        .....##
        .....##
        ..++...
        ..++...
        --.....
        --.....
        .......
    """,
    # Keepsafe: a padlock.
    "keepsafe": """
        ..###..
        .#...#.
        .#...#.
        #######
        #++*++#
        #+++++#
        #######
    """,
    # Faithful: a paw inside a heart.
    "faithful": """
        .##.##.
        #++#++#
        #+*.*+#
        #+.*.+#
        .#+*+#.
        ..#+#..
        ...#...
    """,
}
