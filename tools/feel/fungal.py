"""Living cellulose, soft canopy knocks and a dry leather-filter breath."""
from feel.core import sa,event
def cap_open(v,rng):
 return sa.finish(sa.mix(sa.highpass(sa.noise(.42,rng),1800)*sa.env(.42,(0,0),(.08,1),(.42,0))*.065,(.1,sa.partials(530,.2,((1,1,1),(2.3,.1,.7)),.04))),'effect')
def cap_harvest(v,rng):
 return sa.finish(sa.mix(sa.lowpass(sa.noise(.35,rng),3100)*sa.env(.35,(0,0),(.04,1),(.18,.2),(.26,.65),(.35,0))*.085,sa.partials(310,.09,((1,1,1),(2.2,.18,.8)),.045)),'effect')
def mark_read(v,rng):
 return sa.finish(sa.mix(sa.partials(380,.18,((1,1,1),(2.1,.2,.7)),.075),(.14,sa.partials(570,.26,((1,1,1),(2.6,.1,.8)),.05))),'effect')
def nursery_settle(v,rng):
 return sa.finish(sa.mix(sa.partials(440,.1,((1,1,1),(2.4,.2,.7)),.045),(.09,sa.lowpass(sa.noise(.32,rng),1800)*sa.env(.32,(0,0),(.04,1),(.32,0))*.045)),'effect')
def filter(v,rng):
 return sa.finish(sa.mix(sa.highpass(sa.noise(.58,rng),1500)*sa.env(.58,(0,0),(.08,.8),(.24,.5),(.4,1),(.58,0))*.095,(.08,sa.partials(420,.12,((1,1,1),(3,.1,.6)),.035))),'effect')
EVENTS=[event('fungal_'+n,f,role='effect',subtitle='fungal.'+n) for n,f in [('cap_open',cap_open),('cap_harvest',cap_harvest),('mark_read',mark_read),('nursery_settle',nursery_settle),('filter',filter)]]
