"""Physical copper pivot, woven cloth tension, brittle reed/arrow contact."""
from feel.core import sa,event

def pivot(v,rng):
 n=.14
 return sa.finish(sa.mix(sa.bandpass(sa.noise(n,rng),1400,4500)*sa.env(n,(0,0),(.005,1),(.03,.4),(n,0))*.045,(.012,sa.partials(730,.11,((1,.8,1),(2.4,.25,.35)),.04))),'effect')

def open(v,rng):
 n=.28
 cloth=sa.bandpass(sa.noise(n,rng),700,3600)*sa.env(n,(0,0),(.04,.5),(.17,1),(n,0))*.06
 return sa.finish(sa.mix(cloth,(.07,sa.partials(280,.18,((1,.7,1),(2.8,.2,.4)),.035))),'effect')

def catch(v,rng):
 n=.32
 grit=sa.bandpass(sa.noise(n,rng),800,4800)*sa.env(n,(0,0),(.007,1),(.04,.6),(n,0))*.055
 wood=sa.partials(520,.22,((1,.8,1),(1.8,.35,.65),(3.7,.1,.3)),.05)
 return sa.finish(sa.mix(grit,wood,(.035,sa.partials(370,.20,((1,.6,1),(2.5,.1,.4)),.035))),'effect')
EVENTS=[event('rainshield_'+n,f,role='effect',subtitle='rainshield.'+n) for n,f in [('pivot',pivot),('open',open),('catch',catch)]]
