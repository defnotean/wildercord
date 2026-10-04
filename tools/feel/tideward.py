"""Three unrelated physical voices: drying cuffs, a repaired hinge, and an unwound spool."""
from feel.core import sa,event

def weave_rest(v,rng):
 damp=sa.norm(sa.bandpass(sa.noise(.16,rng),280,1250))*sa.env(.16,(0,0),(.025,1),(.16,0))*.16
 reed=sa.norm(sa.bandpass(sa.noise(.10,rng),1000,3400))*sa.env(.10,(0,0),(.008,.75),(.10,0))*.075
 return sa.finish(sa.mix(damp,(.09,reed)),'effect')

def glass_warning(v,rng):
 # A small glass rim and the lower asymmetric wire hinge, not a two-note magic cue.
 rim=sa.partials(1380,.11,((1,1,.5),(2.71,.13,.21)),.038)
 wire=sa.partials(361,.065,((1,1,.6),(3.1,.21,.2)),.022)
 scrape=sa.norm(sa.bandpass(sa.noise(.045,rng),550,2600))*sa.env(.045,(0,0),(.009,1),(.045,0))*.021
 return sa.finish(sa.mix(rim,(.028,wire),scrape),'effect')

def spool_commit(v,rng):
 # Wooden seating contact followed by continuously unwinding fibres and a single peg tap.
 wood=sa.partials(213,.12,((1,1,.7),(2.89,.29,.25),(4.7,.09,.13)),.054)
 thread=sa.norm(sa.bandpass(sa.noise(.34,rng),470,2300))*sa.env(.34,(0,0),(.025,.4),(.16,1),(.34,0))*.075
 peg=sa.partials(572,.095,((1,1,.6),(2.4,.18,.25)),.026)
 return sa.finish(sa.mix(wood,(.045,thread),(.37,peg)),'effect')

EVENTS=[event('tideward_'+n,f,role='effect',subtitle='tideward.'+n) for n,f in [('weave_rest',weave_rest),('glass_warning',glass_warning),('spool_commit',spool_commit)]]
