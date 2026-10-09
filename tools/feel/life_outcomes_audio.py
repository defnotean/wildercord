"""Original Life voices: each rune has an authored cue and successful-transition landing.
No registry/assets writes. Shared instruments are DSP primitives; arrangements are authored below.
"""
from pathlib import Path
import sys
import numpy as np
ROOT=next(parent for parent in Path(__file__).resolve().parents if (parent / "tools" / "sound_art.py").exists())
sys.path.insert(0,str(ROOT / "tools"))
from feel.core import sa, event

# Instruments: hollow seed, sap membrane, dry fibre, tissue closure, damp root knock.
def seed(f,d=.2):return sa.sine(f,d)*sa.decay(d,.04,.002)+.23*sa.sine(f*3.1,d)*sa.decay(d,.016,.001)
def sap(f,d=.4):return sa.sine(sa.glide(d,(0,f*.72),(d*.38,f),(d,f*.94)),d)*sa.swell(d,d*.3)
def fibre(rng,d=.3,lo=900,hi=4000):return sa.norm(sa.bandpass(sa.noise(d,rng),lo,hi))*sa.decay(d,d*.25,.008)
def tissue(f,d=.45):return sa.sine(f,d)*sa.swell(d,d*.48)+.14*sa.sine(f*1.5,d)*sa.decay(d,.15,.01)+.45*sa.sine(f*2.7,d)*sa.swell(d,d*.38)
def root(f,d=.25):return sa.thump(f,f*.43,d,.05,drive=1.1)+.7*seed(f*4.2,d)
def tone(degree,octave=0):return sa.note(degree,octave)
D,E,F,A,B=sa.D,sa.E,sa.FS,sa.A,sa.B
# Each pair contains different gestures at cast and at observed outcome. Variant changes contact,
# thickness or resonant register locally; it never selects another rune's waveform.
def heal(v,r):
 return [(0,.5*tissue(tone(D,1+v),.31)),(.09,.17*sap(tone(F,1),.2))],[(0,.6*tissue(tone(F,1),.54)),(.16,.32*seed(tone(A,1))),(.05,.06*fibre(r,.4,2400,5100))]
def grow(v,r):
 return [(0,.4*seed(260+v*23)),(.07,.28*seed(390)),(.14,.2*fibre(r,.2,600,2200))],[(0,.25*root(145)),(.04,.3*sap(310+v*16)),(.2,.26*seed(490)),(.29,.16*fibre(r,.3,1100,2900))]
def regrowth(v,r):
 return [(0,.2*fibre(r,.4,1200,3400)),(.1,.4*tissue(290+v*14,.38))],[(0,.35*seed(tone(D,1))),(.18,.35*tissue(tone(F,1),.35)),(.43,.28*seed(tone(A,1))),(.06,.15*fibre(r,.7,1300,2600))]
def cleanse(v,r):
 return [(0,.3*fibre(r,.22,3500,7000)),(.11,.3*seed(670+v*30))],[(0,.32*fibre(r,.48,2800,5600)),(.19,.25*sap(540+v*14,.24)),(.38,.27*seed(860))]
def venom(v,r):
 return [(0,.55*seed(920+v*45,.08)),(.035,.22*fibre(r,.19,2200,5400))],[(0,.36*seed(1100,.065)),(.055,.3*sap(155+v*9,.33)),(.18,.22*fibre(r,.38,700,2400))]
def nourish(v,r):
 return [(0,.43*seed(350+v*14)),(.085,.28*seed(280))],[(0,.48*sap(240,.34)),(.13,.2*seed(420+v*16)),(.22,.19*tissue(310,.42))]
def harvest(v,r):
 return [(0,.3*fibre(r,.17,1800,4000)),(.08,.4*seed(510+v*18))],[(0,.32*fibre(r,.2,2800,6800)),(.055,.36*seed(710)),(.11,.31*seed(620)),(.2,.24*root(180+v*12))]
def reversal(v,r):
 return [(0,.35*sap(190+v*8,.39)),(.22,.3*seed(470))],[(0,.3*tissue(230,.29)),(.11,.4*sap(470+v*17,.28)),(.27,.28*seed(610))]
def restore(v,r):
 return [(0,.32*root(210+v*11)),(.1,.34*seed(630))],[(0,.42*seed(390)),(.075,.31*seed(470+v*20)),(.16,.19*fibre(r,.24,1600,3500)),(.28,.32*tissue(520,.36))]
def bramble(v,r):
 return [(0,.5*root(170+v*12)),(.09,.25*fibre(r,.16,1300,3900))],[(0,.46*seed(720+v*26,.09)),(.03,.4*fibre(r,.18,1700,5600)),(.13,.22*root(135))]
def haven(v,r):
 return [(0,.32*tissue(160+v*9,.56)),(.3,.24*seed(310))],[(0,.32*root(100+v*6,.35)),(.055,.23*fibre(r,.44,600,1800)),(.26,.34*tissue(260,.56))]
def glimmer(v,r):
 return [(0,.32*seed(1200+v*50,.1)),(.055,.23*seed(1530,.12))],[(0,.22*fibre(r,.23,3000,6400)),(.08,.33*seed(930+v*25,.17)),(.19,.25*sap(620,.2))]
def fortune(v,r):
 return [(0,.45*seed(780+v*22,.1)),(.13,.3*seed(1130,.13))],[(0,.34*seed(890)),(.11,.26*seed(1340+v*35)),(.21,.23*seed(1020)),(.04,.12*fibre(r,.4,2000,4100))]
def bloom(v,r):
 return [(0,.3*tissue(330,.33)),(.13,.33*seed(490+v*16))],[(0,.3*sap(300,.35)),(.12,.3*tissue(410,.45)),(.29,.3*seed(620+v*21)),(.4,.12*fibre(r,.25,2600,4800))]
def soulbond(v,r):
 return [(0,.34*tissue(280+v*8,.48)),(.04,.29*tissue(420,.48))],[(0,.4*seed(410)),(.12,.38*seed(615+v*11)),(.2,.23*tissue(205,.66)),(.2,.19*tissue(307.5,.66))]
def second_wind(v,r):
 return [(0,.34*root(130+v*6)),(.19,.3*seed(650))],[(0,.45*fibre(r,.16,2200,5100)),(.065,.43*sap(430+v*13,.48)),(.29,.28*tissue(570,.55))]
def lifebloom(v,r):
 return [(0,.4*seed(330)),(.095,.3*tissue(440+v*10,.35)),(.22,.16*seed(550))],[(0,.35*sap(360+v*14,.54)),(.16,.31*seed(480)),(.31,.28*seed(720)),(.39,.17*fibre(r,.42,1900,4700))]
def root_bulwark(v,r):
 return [(0,.54*root(95+v*5)),(.12,.25*fibre(r,.3,420,1700))],[(0,.48*root(120,.38)),(.07,.32*fibre(r,.4,520,2200)),(.19,.3*seed(240+v*12)),(.32,.24*tissue(150,.4))]
def bloomstep(v,r):
 return [(0,.26*fibre(r,.27,2900,5900)),(.08,.34*sap(530+v*18,.22))],[(0,.42*root(125)),(.045,.29*seed(620+v*25)),(.18,.3*tissue(390,.5)),(.3,.18*fibre(r,.32,1500,3300))]
def stitchtime(v,r):
 return [(0,.38*seed(460,.085)),(.13,.3*seed(465+v*5,.085)),(.27,.19*tissue(230,.3))],[(0,.27*seed(480+v*9,.075)),(.1,.28*seed(510,.075)),(.21,.34*tissue(340,.49)),(.42,.24*sap(380,.28))]
def vinelash(v,r):
 return [(0,.44*fibre(r,.18,1100,4600)),(.085,.33*root(175+v*7))],[(0,.41*fibre(r,.14,1900,7000)),(.04,.4*seed(880+v*38,.09)),(.1,.32*root(165)),(.22,.21*sap(200,.25))]
def remedy(v,r):
 return [(0,.31*fibre(r,.24,2400,3900)),(.12,.32*sap(310+v*10,.3))],[(0,.26*seed(230)),(.06,.2*fibre(r,.24,1200,3200)),(.19,.38*sap(510,.32)),(.37,.29*seed(770+v*15))]
def ancient_seed(v,r):
 return [(0,.52*root(78+v*4,.39)),(.19,.32*seed(230))],[(0,.4*seed(175+v*9)),(.11,.3*fibre(r,.48,600,2700)),(.24,.34*sap(340,.41)),(.48,.22*seed(510))]
def moonpetal(v,r):
 return [(0,.3*tissue(670+v*21,.41)),(.16,.2*fibre(r,.3,3900,7500))],[(0,.24*fibre(r,.21,4500,8200)),(.055,.31*seed(1030+v*23,.13)),(.21,.36*tissue(770,.49))]
def sporebloom(v,r):
 return [(0,.39*sap(150+v*8,.35)),(.08,.25*tissue(410,.35)),(.13,.2*fibre(r,.32,1500,2800))],[(0,.34*root(115)),(.065,.4*fibre(r,.6,1100,3400)),(.2,.23*sap(260,.41)),(.32,.22*seed(420+v*17))]
def glowvine(v,r):
 return [(0,.25*seed(940)),(.07,.28*sap(620+v*22,.22))],[(0,.25*fibre(r,.26,700,2100)),(.09,.41*sap(410,.31)),(.26,.32*seed(730+v*19)),(.38,.2*tissue(550,.3))]
def rootsnare(v,r):
 return [(0,.48*root(110+v*7)),(.055,.3*seed(280))],[(0,.46*root(145,.3)),(.055,.35*fibre(r,.27,450,2600)),(.12,.33*seed(530+v*26,.09)),(.25,.23*root(90))]
def drowse(v,r):
 return [(0,.31*tissue(195+v*5,.61)),(.21,.16*fibre(r,.35,2600,3900))],[(0,.34*tissue(230,.66)),(.22,.2*sap(170+v*6,.42)),(.44,.18*seed(345,.13))]
def ashen_mercy(v,r):
 return [(0,.26*fibre(r,.27,2800,5800)),(.08,.34*root(190+v*8)),(.18,.2*tissue(380,.29))],[(0,.32*fibre(r,.19,1800,6200)),(.095,.36*sap(330+v*12,.39)),(.27,.33*tissue(480,.54)),(.43,.18*seed(640))]

BUILDERS=[heal,grow,regrowth,cleanse,venom,nourish,harvest,reversal,restore,bramble,haven,glimmer,fortune,bloom,soulbond,second_wind,lifebloom,root_bulwark,bloomstep,stitchtime,vinelash,remedy,ancient_seed,moonpetal,sporebloom,glowvine,rootsnare,drowse,ashen_mercy]
def voice(builder,phase):
 def build(v,rng):
  x=sa.finish(sa.mix(*builder(v,rng)[phase]),"cast" if phase==0 else "effect",fade_out=.09)
  # Smooth DC compensation preserves the zero endpoints and the authored contact envelope.
  window=np.sin(np.linspace(0,np.pi,len(x)))**2
  return x-x.mean()*window/window.mean()
 return build
EVENTS=[]
for builder in BUILDERS:
 EVENTS.extend([event("life_auth_"+builder.__name__+"_cue",voice(builder,0),variants=2,role="cast",subtitle="cast"),event("life_auth_"+builder.__name__+"_outcome",voice(builder,1),variants=2,role="effect",subtitle="hit")])

# Existing FieldFusionFeels/FieldFusionFx own Mercy's two active voices. Keep both Life
# alternatives only for audition; the central Life part must merge ACTIVE_EVENTS, not EVENTS.
ACTIVE_EVENTS=tuple(e for e in EVENTS if not e.name.startswith("life_auth_ashen_mercy_"))
assert len(ACTIVE_EVENTS)==56

# ---- fx-passive pack: the gentle hearth Life runes, each with its own cue and its own landing.
def slowburn(v,r):
 return [(0,.3*sap(330+v*12,.42)),(.18,.24*seed(520))],[(0,.3*tissue(210,.6)),(.25,.18*sap(260+v*9,.4))]
def lullaby(v,r):
 return [(0,.3*tissue(tone(A,0+v),.5)),(.24,.24*tissue(tone(F,0),.5))],[(0,.26*tissue(tone(D,1),.62)),(.3,.22*tissue(tone(A,0),.62)),(.5,.1*fibre(r,.3,2200,3600))]
def dew_drink(v,r):
 return [(0,.38*seed(1040+v*30,.09)),(.09,.3*seed(1320,.09))],[(0,.3*sap(520+v*12,.28)),(.14,.28*seed(880,.12)),(.24,.12*fibre(r,.2,3200,6000))]
def petward(v,r):
 return [(0,.38*root(150+v*8)),(.12,.26*tissue(300,.36))],[(0,.32*tissue(260,.44)),(.18,.3*seed(520+v*14)),(.06,.1*fibre(r,.3,900,2200))]
def luckcharm(v,r):
 return [(0,.38*seed(990+v*28,.1)),(.07,.3*seed(1480,.1)),(.15,.22*seed(1240,.1))],[(0,.3*seed(740)),(.09,.28*seed(1110+v*20)),(.18,.24*seed(1480))]
def steedmend(v,r):
 return [(0,.4*root(120+v*6)),(.1,.24*root(150))],[(0,.32*sap(280+v*10,.36)),(.2,.28*tissue(350,.4))]
def hearthbond(v,r):
 return [(0,.3*tissue(240+v*7,.5)),(.06,.26*tissue(360,.5)),(.2,.2*seed(480))],[(0,.34*tissue(300,.58)),(.15,.3*seed(450+v*12)),(.3,.24*seed(600))]
def trailblaze(v,r):
 return [(0,.32*fibre(r,.16,1500,3600)),(.07,.34*seed(560+v*20,.12))],[(0,.3*seed(620,.1)),(.12,.26*seed(700+v*15,.1)),(.24,.2*seed(780,.1))]
HEARTH_EVENTS=[]
for builder in [slowburn,lullaby,dew_drink,petward,luckcharm,steedmend,hearthbond,trailblaze]:
 HEARTH_EVENTS.extend([event("life_auth_"+builder.__name__+"_cue",voice(builder,0),variants=2,role="cast",subtitle="cast"),event("life_auth_"+builder.__name__+"_outcome",voice(builder,1),variants=2,role="effect",subtitle="hit")])
