"""A quiet bubbling trill, startled squeak and three answering lantern drops."""
from feel.core import sa,event

def call(v,rng):
 return sa.finish(sa.mix(sa.partials(540,.22,((1,1,1),(2.1,.15,.8)),.12),(.18,sa.partials(620,.18,((1,1,1),(3,.08,.6)),.08)),(.31,sa.partials(470,.20,((1,1,1),),.09))),'effect')
def hurt(v,rng):
 return sa.finish(sa.mix(sa.partials(830,.12,((1,1,1),(1.5,.2,.5)),.12),sa.highpass(sa.noise(.16,rng),1400)*sa.env(.16,(0,0),(.02,1),(.16,0))*.05),'effect')
def death(v,rng):
 return sa.finish(sa.mix(sa.partials(390,.32,((1,1,1),(2,.1,.5)),.07),(.1,sa.partials(290,.27,((1,1,1),),.05))),'effect')
def answer(v,rng):
 return sa.finish(sa.mix(sa.partials(460,.3,((1,1,1),(2,.08,.6)),.07),(.16,sa.partials(610,.27,((1,1,1),),.06)),(.32,sa.partials(760,.24,((1,1,1),),.05))),'effect')
def pearl(v,rng):
 return sa.finish(sa.mix(sa.partials(580,.23,((1,1,1),(2.7,.12,.6)),.09),(.09,sa.partials(910,.2,((1,1,1),),.05))),'effect')
EVENTS=[event('wetland_newt_'+n,f,role='effect',subtitle='wetland.'+n) for n,f in [('call',call),('hurt',hurt),('death',death),('answer',answer),('pearl',pearl)]]

def reed_open(v,rng):
 return sa.finish(sa.mix(sa.highpass(sa.noise(.24,rng),1600)*sa.env(.24,(0,0),(.05,1),(.24,0))*.025,(.08,sa.partials(710,.3,((1,1,1),(2.4,.07,.7)),.045))),'effect')
def reed_harvest(v,rng):
 return sa.finish(sa.lowpass(sa.highpass(sa.noise(.35,rng),1900),5500)*sa.env(.35,(0,0),(.04,1),(.15,.25),(.22,.65),(.35,0))*.065,'effect')
def lens_focus(v,rng):
 return sa.finish(sa.mix(sa.partials(420,.09,((1,1,1),(3,.12,.7)),.035),(.08,sa.partials(840,.22,((1,1,1),(1.8,.08,.6)),.045))),'effect')
EVENTS += [event('wetland_'+n,f,role='effect',subtitle='wetland.'+n) for n,f in [('reed_open',reed_open),('reed_harvest',reed_harvest),('lens_focus',lens_focus)]]
