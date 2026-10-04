"""Root Carry's lang and spell-circle band; its rune icon is drawn by item_art (new_rune_art.GLYPHS)."""
LANG={'rune.wildercord.root_carry':'Root Carry','subtitles.wildercord.kit.root_carry.cue':'Root fibres tension','subtitles.wildercord.kit.root_carry.select':'Living root marked','subtitles.wildercord.kit.root_carry.settle':'Root and soil settle'}
def circles(bands,marks):
 """Root Carry's ring and emblem, in the same grammar as every rune's (see circle_art): an Effect's solid
 line and round frame, a sprout above the line with its roots forking below, and a carried clod as glyph."""
 import circle_art as ca
 bands['root_carry']=ca.band_tile('effect','sprout','out','none')|{(8,9),(8,10),(7,11),(9,11),(6,12),(10,12)}
 marks['root_carry']=ca.mark_tile('effect',['.#.#.#.','..###..','...#...','.#####.','#######','.#.#.#.','#..#..#'],'plain','none',False)
# Registration also adds RUNE_RECIPES['root_carry'] to the central themed table:
# ['minecraft:rooted_dirt','minecraft:bone_meal','minecraft:string'].
# Normal rank-II catalysts (two lapis +gold) are preserved by rune_ingredients.
