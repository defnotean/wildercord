"""Seven original paper/copper and braided-fibre voices. Not synthesized or listened at draft stage."""
from feel.core import sa,event

def watch_cue(v,rng):
 n=.27;paper=sa.bandpass(sa.noise(n,rng),550,2600)*sa.env(n,(0,0),(.03,.8),(.11,.25),(.18,.6),(n,0))*.036
 pin=sa.partials(460,.11,((1,1,1),(2.7,.18,.8)),.032)
 return sa.finish(sa.mix(paper,(.17,pin)),'effect')
def watch_arm(v,rng):
 n=.22;fold=sa.bandpass(sa.noise(n,rng),330,1500)*sa.env(n,(0,0),(.025,1),(.12,.2),(n,0))*.04
 return sa.finish(sa.mix(fold,(.08,sa.partials(285,.15,((1,.8,1),(3.1,.16,.5)),.026))),'effect')
def watch_warn(v,rng):
 n=.30;tear=sa.bandpass(sa.noise(n,rng),700,3300)*sa.env(n,(0,0),(.012,1),(.06,.6),(.14,0),(n,0))*.043
 a=sa.partials(570,.11,((1,.8,1),(2.45,.18,.5)),.038);b=sa.partials(410,.17,((1,.8,1),(3.6,.12,.6)),.031)
 return sa.finish(sa.mix(tear,(.09,a),(.20,b)),'effect')
def braid_cue(v,rng):
 a=sa.partials(310,.22,((1,.8,1),(2,.18,.8),(4.3,.07,.6)),.032);b=sa.partials(390,.16,((1,.6,1),(3,.12,.7)),.025)
 return sa.finish(sa.mix(a,(.11,b)),'effect')
def braid_offer(v,rng):
 return sa.finish(sa.mix(sa.partials(270,.14,((1,.7,1),(2.9,.15,.7)),.035),(.08,sa.partials(330,.12,((1,.5,1),(4.1,.08,.5)),.025))),'effect')
def braid_transfer(v,rng):
 n=.42;fray=sa.bandpass(sa.noise(n,rng),450,1800)*sa.env(n,(0,0),(.05,.3),(.12,.6),(.26,.2),(n,0))*.025
 a=sa.partials(350,.19,((1,.7,1),(2,.23,.8),(3.7,.1,.6)),.042);b=sa.partials(520,.24,((1,.8,1),(2.2,.19,.7)),.038)
 return sa.finish(sa.mix(a,fray,(.18,b)),'effect')
def braid_decline(v,rng):
 n=.23;rub=sa.bandpass(sa.noise(n,rng),240,1100)*sa.env(n,(0,0),(.015,.6),(.07,.8),(n,0))*.03
 return sa.finish(sa.mix(rub,(.035,sa.partials(215,.19,((1,.7,1),(2.6,.1,.5)),.022))),'effect')
EVENTS=[event('camp_'+n,f,role='effect',subtitle='camp.'+n) for n,f in [('watch_cue',watch_cue),('watch_arm',watch_arm),('watch_warn',watch_warn),('braid_cue',braid_cue),('braid_offer',braid_offer),('braid_transfer',braid_transfer),('braid_decline',braid_decline)]]
