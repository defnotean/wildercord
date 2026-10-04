"""Original ceramic valve clicks, ventilated ash fan, dry browse and cooling hiss."""
from feel.core import sa,event

def warn(v,rng):
 a=sa.partials(420,.22,((1,.6,1),(2.73,.45,.4),(5.17,.2,.2)),.04)*.09
 b=sa.partials(315,.24,((1,.5,1),(3.21,.35,.4)),.05)*.08
 return sa.finish(sa.mix(a,(.12,b),(.27,a*.7)),'effect')
def fan(v,rng):
 def puff(n):return sa.norm(sa.bandpass(sa.noise(n,rng),280,2600))*sa.env(n,(0,0),(.02,.85),(.07,1),(n,0))*.11
 return sa.finish(sa.mix(puff(.18),(.21,puff(.2)),(.43,puff(.24))),'effect')
def recover(v,rng):
 return sa.finish(sa.mix(sa.lowpass(sa.noise(.35,rng),620)*sa.env(.35,(0,0),(.02,1),(.13,.5),(.35,0))*.10,(.13,sa.partials(190,.2,((1,.6,1),(2.4,.2,.5)),.035)*.06)),'effect')
def browse(v,rng):
 a=sa.norm(sa.bandpass(sa.noise(.14,rng),650,1800))*sa.env(.14,(0,0),(.015,1),(.14,0))*.055
 return sa.finish(sa.mix(a,(.18,a*.8),(.37,a*.6)),'effect')
def cool(v,rng):
 return sa.finish(sa.bandpass(sa.noise(.48,rng),1300,3800)*sa.env(.48,(0,0),(.04,.8),(.15,.3),(.48,0))*.09,'effect')
def pick(v,rng):
 return sa.finish(sa.mix(sa.bandpass(sa.noise(.12,rng),1100,3300)*sa.env(.12,(0,0),(.01,.7),(.12,0))*.055,(.05,sa.partials(600,.14,((1,.5,1),(3.8,.15,.4)),.04)*.04)),'effect')
EVENTS=[event('ember_bailiff_'+n,f,role='effect',subtitle='ember_bailiff.'+n) for n,f in [('warn',warn),('fan',fan),('recover',recover),('browse',browse),('cool',cool)]]+[event('ember_fern_pick',pick,role='effect',subtitle='ember_fern.pick')]
