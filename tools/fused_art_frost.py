"""Hand-drawn rune icons for the fused effects of frost (merged into item_art.GLYPHS).

Same format as item_art.GLYPHS: a pictogram at most 9 wide and 9 tall, "." empty, "-" shade,
"#" main, "+" light, "*" core (the rune's element colours), "o" darkens the stone.

Geode is an earth rune, so its stone is brown, but its crystal is amethyst: it brings three amethyst
colours of its own, "a" deep, "A" main and "V" pale, added to item_art's fixed extra colours (EXTRA) when
item_art loads this file.
"""

import sys

AMETHYST = {
    "a": (0x5A, 0x2C, 0x9C),
    "A": (0x9A, 0x5A, 0xE6),
    "V": (0xDC, 0xC0, 0xFF),
}


def _add_colours() -> None:
    """Adds the amethyst colours to the item_art module loading this file (never replacing one it has)."""
    for name in ("item_art", "__main__"):
        art = sys.modules.get(name)
        extra = getattr(art, "EXTRA", None) if art is not None else None
        if isinstance(extra, dict):
            for key, colour in AMETHYST.items():
                extra.setdefault(key, colour)


_add_colours()

GLYPHS: dict[str, str] = {
    # snowflakes driven between two gusts of wind, each curling at its end
    "blizzard": """
        #####....
        .....#.+.
        ..+.#.+*+
        .+*+...+.
        ..+......
        ....#####
        .+.#.....
        +*+.#....
        .+...###.
    """,
    # a lotus of ice opening: a tall middle petal, two either side, a cup of frost under them
    "frostbloom": """
        ....+....
        ...+*+...
        .+.+*+.+.
        +*+#*#+*+
        .+*#*#*+.
        ..#***#..
        ...###...
    """,
    # a shard of black ice cracking through, splinters flying off it
    "black_ice": """
        ....#....
        ...#k#..+
        ..#kkk#..
        ..#kk*#.+
        .#kk*kk#.
        .#k*kkk#.
        .#kk*kk#.
        ..#kkk#..
        ...###...
    """,
    # a six-pointed frost seal, ice at its heart
    "rime_seal": """
        ....+....
        ...+.+...
        +#######+
        .#+...+#.
        ..#.*.#..
        .#+...+#.
        +#######+
        ...+.+...
        ....+....
    """,
    # a clock stopped inside a cocoon of ice
    "cryostasis": """
        ...###...
        ..#+++#..
        .#+.*.+#.
        .#+.*.+#.
        .#+.**+#.
        .#+...+#.
        .#+...+#.
        ..#+++#..
        ...###...
    """,
    # jaws of frost closing on a drop of blood
    "frostbite": """
        .+++++++.
        .#+#+#+#.
        ..#.#.#..
        ....R....
        ...RRR...
        ...RpR...
        ..#.R.#..
        .#+#+#+#.
        .+++++++.
    """,
    # a crown of ice spikes bursting out of the ground, frozen solid
    "absolute_zero": """
        ....*....
        .+..+..+.
        .#..#..#.
        .#.+#+.#.
        +#.#*#.#+
        ##+#*#+##
        #########
    """,
    # an ammonite: a coiled shell turned to stone, its ribs round the rim
    "fossilize": """
        ..#####..
        .#+#+#+#.
        #+.....+#
        ##.###.##
        #+.#*#.+#
        ##.#.#.##
        #+...#.+#
        .#+#+#+#.
        ..#####..
    """,
    # a geode cracking open: a dark shell of stone, amethyst crystal packed inside, bright points glinting
    "geode": """
        ..kkk.k..
        .kaAV.ak.
        kaAVw.Aak
        kAVV.wVAk
        kAVw.VVAk
        kaAV.wVAk
        kaAVw.Aak
        .kaA.Vak.
        ..kk.kk..
    """,
}
