"""Original root-fibre pull, soil suction and two dry settling creaks."""
from feel.core import sa,event
def cue(v,rng):
 n=.24
 return sa.finish(sa.mix(sa.norm(sa.bandpass(sa.noise(n,rng),450,1700))*sa.env(n,(0,0),(.045,.55),(.10,1),(n,0))*.035,(.1,sa.partials(330,.19,((1,1,1),(2.6,.15,.6)),.035))),'effect')
def select(v,rng):
 n=.19
 return sa.finish(sa.mix(sa.bandpass(sa.noise(n,rng),600,2200)*sa.env(n,(0,0),(.02,1),(.07,.3),(n,0))*.045,(.04,sa.partials(250,.17,((1,.6,1),(1.7,.22,.5)),.04))),'effect')
def settle(v,rng):
 a=sa.partials(380,.20,((1,.8,1),(2.3,.2,.6)),.045)
 b=sa.partials(290,.24,((1,.8,1),(3.1,.15,.5)),.045)
 soil=sa.lowpass(sa.noise(.38,rng),950)*sa.env(.38,(0,0),(.025,.7),(.11,1),(.38,0))*.06
 return sa.finish(sa.mix(soil,(.08,a),(.26,b)),'effect')
EVENTS=[event('root_carry_'+n,f,role='effect',subtitle='root_carry.'+n) for n,f in [('cue',cue),('select',select),('settle',settle)]]
