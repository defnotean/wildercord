"""Original tiny nose breath, irregular Gill bites, soft chirps, cloth curl and woven filter voice."""
from feel.core import sa,event

def sniff(v,rng):
 def puff(d):return sa.norm(sa.bandpass(sa.noise(d,rng),1200,3400))*sa.env(d,(0,0),(.02,1),(d,0))*.04
 return sa.finish(sa.mix(puff(.08),(.14,puff(.11))),'effect')
def nibble(v,rng):
 def bite(d):return sa.norm(sa.bandpass(sa.noise(d,rng),760,2800))*sa.env(d,(0,0),(.005,1),(.018,.25),(d,0))*.035
 return sa.finish(sa.mix(bite(.06),(.09,bite(.05)),(.23,bite(.08))),'effect')
def trust(v,rng):
 return sa.finish(sa.mix(sa.partials(870,.16,((1,.5,1),(2.12,.14,.6)),.04)*.035,(.14,sa.partials(690,.18,((1,.5,1),(2.19,.12,.6)),.05)*.03)),'effect')
def curl(v,rng):
 d=.38;return sa.finish(sa.norm(sa.bandpass(sa.noise(d,rng),190,1400))*sa.env(d,(0,0),(.06,.6),(.16,.25),(.23,.7),(d,0))*.055,'effect')
def idle(v,rng):
 d=.22;return sa.finish(sa.partials(740,d,((1,.6,1),(2.37,.15,.7),(3.1,.05,.4)),.05)*sa.env(d,(0,0),(.04,1),(.13,.55),(d,0))*.03,'effect')
def step(v,rng):
 d=.055;return sa.finish(sa.norm(sa.lowpass(sa.noise(d,rng),950))*sa.env(d,(0,0),(.006,1),(d,0))*.025,'effect')
def filter(v,rng):
 d=.56;cloth=sa.norm(sa.bandpass(sa.noise(d,rng),350,1900))*sa.env(d,(0,0),(.08,.35),(.18,.65),(.31,.2),(.41,.5),(d,0))*.07
 return sa.finish(sa.mix(cloth,(.32,sa.partials(310,.17,((1,.2,1),(4.3,.12,.5)),.04)*.014)),'effect')
EVENTS=[event('mossveil_'+n,f,role='effect',subtitle='mossveil.'+n)for n,f in [('sniff',sniff),('nibble',nibble),('trust',trust),('curl',curl),('idle',idle),('step',step),('filter',filter)]]
