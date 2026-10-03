"""Reed rattles, a canvas snap and a softer herb-fibre twist. Physical, short voices."""
from feel.core import sa,event

def harvest(v,rng):
 x=sa.noise(.28,rng)*sa.env(.28,(0,0),(.015,1),(.08,.9),(.28,0))
 x=sa.highpass(x,2100)*.16
 return sa.finish(sa.mix(x,(.08,sa.partials(780,.15,((1,1,1),(2.6,.15,.4)),.04))),'effect')

def kite(v,rng):
 x=sa.highpass(sa.noise(.45,rng),350)*sa.env(.45,(0,0),(.03,1),(.08,.7),(.45,0))*.16
 return sa.finish(sa.mix(x,sa.partials(330,.16,((1,1,1),(2,.2,.6)),.15)),'effect')

def braid(v,rng):
 x=sa.highpass(sa.noise(.22,rng),1000)*sa.env(.22,(0,0),(.025,1),(.05,.9),(.22,0))*.10
 return sa.finish(sa.mix(x,(.10,sa.partials(340,.2,((1,1,1),(1.5,.08,.6)),.035))),'ui')

EVENTS=[event('highland_reed_harvest',harvest,role='effect',subtitle='highland.harvest'),event('highland_kite_open',kite,role='effect',subtitle='highland.kite'),event('highland_braid_rustle',braid,role='ui',subtitle='highland.braid')]
