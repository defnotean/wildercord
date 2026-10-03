"""Ten material answers to a steel incision, each with its own envelope and cadence."""
from feel.core import sa,event

def fire(v,rng):
 x=sa.mix(sa.thump(390,100,.16,.008,drive=1.6,knock=.5),(.06,sa.highpass(sa.noise(.35,rng),1800)*.16),(.15,sa.partials(780,.2,((1,1,1),(2.7,.25,.4)),.06)))
 return sa.finish(sa.lowpass(sa.highpass(x,120),6200),'effect')
def frost(v,rng):
 x=sa.mix(sa.partials(1500,.36,((1,1,1),(1.63,.45,.8),(2.41,.2,.5)),.13),(.08,sa.partials(2300,.27,((1,1,1),(3.2,.15,.3)),.08)),(.19,sa.thump(700,240,.11,.005,drive=1.2,knock=.6)))
 return sa.finish(sa.highpass(x,250),'effect')
def storm(v,rng):
 x=sa.mix(sa.thump(220,60,.2,.006,drive=2,knock=.8),sa.highpass(sa.noise(.12,rng),2600)*.14,(.09,sa.partials(930,.28,((1,1,1),(1.1,.45,.5),(3,.2,.3)),.07)))
 return sa.finish(sa.highpass(x,110),'effect')
def wind(v,rng):
 x=sa.mix(sa.bandpass(sa.noise(.38,rng),700,2100)*.2,sa.partials(440,.28,((1,1,1),(2,.14,.4)),.08),(.14,sa.partials(660,.34,((1,1,1),(2.1,.1,.4)),.13)))
 return sa.finish(sa.highpass(x,130),'effect')
def earth(v,rng):
 x=sa.mix(sa.thump(130,38,.35,.02,drive=1.5,knock=.6),(.09,sa.thump(280,80,.13,.008,drive=1.7,knock=.8)),(.19,sa.lowpass(sa.noise(.18,rng),1400)*.12))
 x=sa.mix(x,sa.partials(680,.25,((1,1,1),(2.6,.2,.4)),.08)*.45)
 return sa.finish(sa.highpass(x,100),'effect')
def life(v,rng):
 x=sa.mix(sa.partials(392,.3,((1,1,1),(2,.13,.6)),.12),(.1,sa.partials(523,.35,((1,1,1),(3,.09,.4)),.14)),(.22,sa.partials(784,.4,((1,1,1),(2,.13,.5)),.18)))
 return sa.finish(sa.reverb(sa.highpass(x,140),.16,.05,damp=4800),'effect')
def void(v,rng):
 x=sa.mix(sa.partials(260,.38,((1,1,1),(1.19,.5,.8),(2.7,.12,.4)),.17),(.08,sa.thump(180,42,.32,.014,drive=1.3,knock=.2)),(.18,sa.lowpass(sa.noise(.12,rng),1000)*.1))
 return sa.finish(sa.highpass(x,90),'effect')
def arcane(v,rng):
 x=sa.mix(sa.partials(740,.32,((1,1,1),(2.37,.28,.6),(3.8,.11,.3)),.12),(.11,sa.partials(1108,.38,((1,1,1),(2.1,.15,.5)),.15)),(.24,sa.partials(1480,.35,((1,1,1),(3.1,.08,.3)),.12)))
 return sa.finish(sa.reverb(sa.highpass(x,220),.2,.06,damp=5200),'effect')
def time(v,rng):
 x=sa.mix(sa.thump(630,220,.085,.004,drive=1.1,knock=.5),(.115,sa.thump(560,200,.085,.004,drive=1.1,knock=.4)),(.23,sa.partials(440,.36,((1,1,1),(2.2,.1,.5)),.16)))
 return sa.finish(sa.highpass(x,150),'effect')
def blood(v,rng):
 x=sa.mix(sa.thump(220,65,.18,.008,drive=1.5,knock=.4),(.14,sa.thump(160,45,.24,.014,drive=1.3,knock=.25)),sa.partials(620,.25,((1,1,1),(2.8,.17,.3)),.07))
 return sa.finish(sa.highpass(x,100),'effect')
RESONANT_EVENTS=[event('aura_resonant_'+n,b,role='effect',subtitle='resonant.'+n) for n,b in [('fire',fire),('frost',frost),('storm',storm),('wind',wind),('earth',earth),('life',life),('void',void),('arcane',arcane),('time',time),('blood',blood)]]
