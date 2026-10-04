"""Three physical bittern voices: low throat breath, wet bill impact, layered feather friction."""
from feel.core import sa,event

def boom(v,rng):
 d=.75
 throat=sa.partials(155,d,((1,.6,1),(2,.25,.6),(3.2,.15,.35)),.025)*sa.env(d,(0,0),(.12,1),(.38,.65),(d,0))*.14
 breath=sa.norm(sa.bandpass(sa.noise(d,rng),390,1900))*sa.env(d,(0,0),(.10,.2),(.32,.6),(d,0))*.025
 return sa.finish(sa.mix(throat,breath),'effect')
def catch(v,rng):
 clack=sa.partials(430,.13,((1,.25,1),(2.8,.65,.6),(4.7,.2,.3)),.025)*.10
 water=sa.norm(sa.bandpass(sa.noise(.3,rng),620,3100))*sa.env(.3,(0,0),(.008,.9),(.065,.4),(.18,.2),(.3,0))*.08
 return sa.finish(sa.mix(clack,(.025,water)),'effect')
def rustle(v,rng):
 d=.46
 feather=sa.norm(sa.bandpass(sa.noise(d,rng),650,3800))*sa.env(d,(0,0),(.04,.7),(.12,.12),(.23,1),(.34,.16),(d,0))*.09
 reed=sa.partials(280,.16,((1,.25,1),(3.7,.45,.6),(5.2,.15,.35)),.045)*.025
 return sa.finish(sa.mix(feather,(.18,reed)),'effect')
EVENTS=[event('bittern_'+n,f,role='effect',subtitle='bittern.'+n) for n,f in [('boom',boom),('catch',catch),('rustle',rustle)]]
