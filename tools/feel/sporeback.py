"""Quiet shell friction, wet breathing, cellulose browsing and a short spore breath."""
from feel.core import sa,event
def call(v,rng):
 return sa.finish(sa.lowpass(sa.noise(.4,rng),1900)*sa.env(.4,(0,0),(.14,1),(.4,0))*.055,'effect')
def browse(v,rng):
 return sa.finish(sa.mix(sa.highpass(sa.noise(.3,rng),1800)*sa.env(.3,(0,0),(.04,1),(.13,.2),(.2,.7),(.3,0))*.06,sa.partials(460,.18,((1,1,1),(2.6,.1,.7)),.035)),'effect')
def hide(v,rng):
 return sa.finish(sa.mix(sa.partials(330,.15,((1,1,1),(2.7,.2,.6)),.1),sa.lowpass(sa.noise(.27,rng),2100)*sa.env(.27,(0,0),(.03,1),(.27,0))*.065),'effect')
def answer(v,rng):
 return sa.finish(sa.mix(sa.highpass(sa.noise(.5,rng),2300)*sa.env(.5,(0,0),(.14,1),(.5,0))*.04,(.12,sa.partials(620,.22,((1,1,1),(1.4,.1,.7)),.04))),'effect')
def gather(v,rng):
 return sa.finish(sa.mix(sa.partials(720,.1,((1,1,1),(2.1,.14,.6)),.06),(.08,sa.partials(510,.2,((1,1,1),),.045))),'effect')
EVENTS=[event('sporeback_'+n,f,role='effect',subtitle='sporeback.'+n) for n,f in [('call',call),('browse',browse),('hide',hide),('answer',answer),('gather',gather)]]
