"""Authored dry bark friction, shovel warning and snapping physical roots."""
from feel.core import sa,event

def call(v,rng):
 return sa.finish(sa.lowpass(sa.noise(.6,rng),1600)*sa.env(.6,(0,0),(.08,.7),(.18,.1),(.3,1),(.6,0))*.075,'effect')
def warn(v,rng):
 # A shovel-like carapace draws across two bark ridges before the roots commit.
 # Dry midrange friction carries the warning without a magical sustained tone.
 duration=.5
 rub=sa.norm(sa.bandpass(sa.noise(duration,rng),430,2600))*sa.env(duration,(0,0),(.045,.85),(.16,.22),(.29,1),(.43,.35),(duration,0))*.13
 fibres=sa.norm(sa.bandpass(sa.noise(duration,rng),1400,3900))*sa.env(duration,(0,0),(.09,.35),(.20,0),(.31,.55),(duration,0))*.035
 wood=sa.partials(175,.42,((1,.35,1),(2.8,.65,.8),(4.3,.20,.35)),.045)*.10
 return sa.finish(sa.mix(rub,fibres,wood),'effect')

def rake(v,rng):
 # A short root tooth catches, drags, then splinters; distinct from the warning's two rubs.
 duration=.24
 scrape=sa.norm(sa.bandpass(sa.noise(duration,rng),680,3400))*sa.env(duration,(0,0),(.012,1),(.055,.35),(.085,.7),(.15,.15),(duration,0))*.18
 splinter=sa.norm(sa.bandpass(sa.noise(.10,rng),1700,4300))*sa.env(.10,(0,0),(.003,1),(.025,.3),(.10,0))*.045
 knock=sa.partials(95,.15,((1,.35,1),(3.4,.7,.6),(6.2,.25,.3)),.055)*.07
 return sa.finish(sa.mix(scrape,(.035,splinter),knock),'effect',fade_in=.007,fade_out=.075)

def meal(v,rng):
 return sa.finish(sa.mix(sa.lowpass(sa.noise(.14,rng),1300)*sa.env(.14,(0,0),(.02,1),(.14,0))*.08,(.2,sa.lowpass(sa.noise(.17,rng),1800)*sa.env(.17,(0,0),(.03,1),(.17,0))*.065)),'effect')
def release(v,rng):
 return sa.finish(sa.mix(sa.highpass(sa.noise(.1,rng),2600)*sa.env(.1,(0,0),(.006,1),(.1,0))*.14,(.055,sa.partials(235,.22,((1,1,1),(3.2,.1,.7)),.035))),'effect')
EVENTS=[event('rootmolt_'+n,f,role='effect',subtitle='rootmolt.'+n) for n,f in [('call',call),('warn',warn),('rake',rake),('meal',meal),('release',release)]]
