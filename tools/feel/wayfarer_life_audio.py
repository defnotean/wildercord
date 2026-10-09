"""The fx-explore pack's Life voices: a cue at the hand and a landing for each of its five Life runes.
Built from tools/feel/life_outcomes_audio.py's instruments (seed, sap, fibre, tissue, root); merged by tools/feel/life.py.
"""
from feel.life_outcomes_audio import seed, sap, fibre, tissue, root, tone, voice, D, E, F, A, B
from feel.core import event


# Slime Sense: a wet gulp under the floor, then a slow bubble rising.
def slime_sense(v, r):
    return [(0, .34*sap(420+v*14, .3)), (.1, .3*seed(640, .14))], [(0, .38*root(95+v*5, .3)), (.1, .3*sap(200, .4)), (.3, .2*seed(330+v*12, .14))]


# Potion Steep: a glass tap and a long brewing swell.
def potion_steep(v, r):
    return [(0, .36*seed(tone(A, 1), .1)), (.08, .24*seed(tone(D, 2), .1))], [(0, .3*tissue(tone(D, 1), .6)), (.18, .26*sap(tone(F, 1)+v*8, .44)), (.4, .18*seed(tone(A, 1)))]


# Dye Wash: a soft brush stroke washing over, and a bright settling note.
def dye_wash(v, r):
    return [(0, .32*fibre(r, .3, 1400, 4200)), (.1, .26*seed(560+v*20))], [(0, .3*fibre(r, .4, 900, 3000)), (.12, .32*tissue(tone(E, 1), .4)), (.3, .2*seed(tone(B, 1)))]


# Checker Dye: two alternating dabs, like stamping squares.
def checker_dye(v, r):
    return [(0, .34*seed(620+v*18, .08)), (.1, .3*seed(470, .08))], [(0, .3*seed(tone(E, 1), .09)), (.09, .28*seed(tone(B, 1), .09)), (.18, .26*seed(tone(E, 1), .09)), (.27, .24*seed(tone(B, 1), .09)), (.05, .1*fibre(r, .35, 2000, 4400))]


# Sign Glow: a faint ink shimmer that brightens.
def sign_glow(v, r):
    return [(0, .26*fibre(r, .2, 3200, 6800)), (.07, .3*seed(1050+v*30, .12))], [(0, .28*sap(520+v*15, .3)), (.12, .3*tissue(tone(A, 1), .45)), (.28, .22*seed(tone(D, 2), .16))]


BUILDERS = [slime_sense, potion_steep, dye_wash, checker_dye, sign_glow]
EVENTS = []
for builder in BUILDERS:
    EVENTS.extend([event("life_auth_" + builder.__name__ + "_cue", voice(builder, 0), variants=2, role="cast", subtitle="cast"),
                   event("life_auth_" + builder.__name__ + "_outcome", voice(builder, 1), variants=2, role="effect", subtitle="hit")])
