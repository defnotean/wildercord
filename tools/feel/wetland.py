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

def refuge_settle(v,rng):
 return sa.finish(sa.mix(sa.lowpass(sa.noise(.32,rng),1800)*sa.env(.32,(0,0),(.05,1),(.32,0))*.035,(.12,sa.partials(330,.2,((1,1,1),(2,.08,.6)),.025))),'effect')
def refuge_wake(v,rng):
 return sa.finish(sa.mix(sa.partials(470,.12,((1,1,1),),.035),(.08,sa.partials(550,.16,((1,1,1),(2,.05,.7)),.025))),'effect')
EVENTS += [event('wetland_'+n,f,role='effect',subtitle='wetland.'+n) for n,f in [('refuge_settle',refuge_settle),('refuge_wake',refuge_wake)]]

# Shell percussion and reed friction rather than magical chimes.
def crab_call(v,rng):
 return sa.finish(sa.mix(sa.partials(390,.1,((1,1,1),(2.3,.18,.7)),.09),(.15,sa.partials(520,.08,((1,1,1),),.06))),'effect')
def crab_warn(v,rng):
 return sa.finish(sa.mix(sa.highpass(sa.noise(.45,rng),1300)*sa.env(.45,(0,0),(.06,1),(.45,0))*.08,sa.partials(330,.32,((1,1,1),(2,.2,.6)),.1)),'effect')
def crab_sweep(v,rng):
 return sa.finish(sa.mix(sa.lowpass(sa.noise(.3,rng),4300)*sa.env(.3,(0,0),(.09,1),(.3,0))*.15,(.1,sa.partials(280,.16,((1,1,1),(2.6,.2,.5)),.12))),'effect')
def crab_calm(v,rng):
 return sa.finish(sa.lowpass(sa.noise(.45,rng),900)*sa.env(.45,(0,0),(.08,1),(.45,0))*.08,'effect')
def crab_stagger(v,rng):
 return sa.finish(sa.mix(sa.partials(470,.1,((1,1,1),(3.1,.2,.5)),.1),(.11,sa.partials(380,.15,((1,1,1),),.09)),sa.highpass(sa.noise(.32,rng),1700)*sa.env(.32,(0,0),(.05,1),(.32,0))*.05),'effect')
def crab_hurt(v,rng):
 return sa.finish(sa.partials(420,.22,((1,1,1),(2.7,.35,.4)),.14),'effect')
def crab_death(v,rng):
 return sa.finish(sa.mix(sa.partials(320,.4,((1,1,1),(1.6,.2,.7)),.12),(.2,sa.lowpass(sa.noise(.25,rng),1200)*sa.env(.25,(0,0),(.04,1),(.25,0))*.1)),'effect')
EVENTS += [event('wetland_crab_'+n,f,role='effect',subtitle='wetland.crab_'+n) for n,f in [('call',crab_call),('warn',crab_warn),('sweep',crab_sweep),('calm',crab_calm),('stagger',crab_stagger),('hurt',crab_hurt),('death',crab_death)]]

def reed_rattle(v,rng):
 """Three hand shakes: dry pebbles knock a clay vessel beneath soft fibre friction."""
 layers=[]
 for at,strength in [(0,.8),(.18,1),(.39,.65)]:
  friction=sa.bandpass(sa.noise(.16,rng),1400,4300)*sa.env(.16,(0,0),(.035,1),(.16,0))*.075
  layers.append((at,friction*strength))
  for delay in [.012,.043,.083]:
   knock=sa.partials(rng.uniform(570,830),.065,((1,1,1),(2.43,.32,.55),(3.7,.14,.3)),.012,.001)
   layers.append((at+delay,knock*.18*strength))
 return sa.finish(sa.lowpass(sa.mix(*layers),5000),'effect',fade_out=.045)
EVENTS += [event('wetland_reed_rattle',reed_rattle,role='effect',attenuation=10,subtitle='wetland.reed_rattle')]
