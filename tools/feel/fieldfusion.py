"""Six original field-signature cue and impact voices.

Each voice carries its material interaction. Runtime registration lives in FieldFusionFeels;
finite successful actions play their impact voices through FieldFusionFx.
"""
from feel.core import sa,event
import numpy as np

def _fit(x,seconds):
    n=sa.samples(seconds)
    return np.pad(x,(0,max(0,n-len(x))))[:n]

def springbed_cue(v,rng):
    trickle=sa.bandpass(sa.noise(.45,rng),800,2700)*sa.env(.45,(0,0),(.09,.6),(.25,1),(.45,0))
    seed=sa.partials(620,.14,((1,1,1),(3.17,.2,.5)),.025,.001)
    return sa.finish(sa.mix(trickle*.12,(.22,seed*.32)),'cast')
def springbed_impact(v,rng):
    drops=sa.bubbles(.35,24,rng,230,800)
    fibres=sa.bandpass(sa.noise(.42,rng),1700,4500)*sa.env(.42,(0,0),(.12,.8),(.42,0))
    return sa.finish(sa.mix(drops*.18,(.18,fibres*.1)),'impact')

def cinder_sieve_cue(v,rng):
    kiln=sa.lowpass(sa.brown(.5,rng),1200)*sa.env(.5,(0,0),(.08,1),(.5,0))
    knocks=sa.mix(*[(at,.14*sa.partials(f,.08,((1,1,1),(2.7,.3,.6)),.02)) for at,f in [(0,460),(.14,530),(.28,390)]])
    return sa.finish(sa.mix(kiln*.15,knocks),'cast')
def cinder_sieve_impact(v,rng):
    intake=sa.moving_band(.42,[(0,2600),(.25,700),(.42,180)],.7,rng)*sa.env(.42,(0,0),(.24,1),(.42,0))
    coal=_fit(sa.bandpass(sa.grains(.3,90,rng),2200,6500),.3)*sa.env(.3,(0,.7),(.3,0))
    return sa.finish(sa.mix(intake*.22,coal*.1),'impact')

def ashen_mercy_cue(v,rng):
    ember=sa.bandpass(sa.noise(.25,rng),1800,4100)*sa.env(.25,(0,0),(.04,1),(.25,0))
    opening=sa.partials(sa.note(sa.D,0),.42,((1,1,1),(2,.1,.8)),.16,.07)
    return sa.finish(sa.mix(ember*.1,(.12,opening*.2)),'cast')
def ashen_mercy_impact(v,rng):
    douse=sa.bandpass(sa.noise(.2,rng),500,2300)*sa.env(.2,(0,0),(.02,1),(.2,0))
    petals=sa.mix(*[(.12+i*.085,.12*sa.partials(sa.note(d,0),.2,((1,1,1),(2.01,.12,.6)),.10,.035)) for i,d in enumerate((sa.D,sa.E,sa.A))])
    return sa.finish(sa.mix(douse*.18,petals),'impact')

def clockroot_cue(v,rng):
    stitch=sa.partials(230,.12,((1,1,1),(3.8,.12,.3)),.025,.001)
    sand=_fit(sa.bandpass(sa.grains(.45,150,rng),1600,5500),.45)*sa.env(.45,(0,0),(.12,.8),(.45,0))
    return sa.finish(sa.mix(stitch*.25,(.05,sand*.1)),'cast')
def clockroot_impact(v,rng):
    scrape=sa.bandpass(sa.noise(.35,rng),350,2700)*sa.env(.35,(0,.8),(.35,0))
    back=sa.reverse(scrape)*.12
    shut=sa.partials(180,.13,((1,1,1),(2.7,.25,.4)),.035,.001)
    return sa.finish(sa.mix(back,(.35,shut*.28)),'impact')

def skylatch_cue(v,rng):
    lift=sa.moving_band(.4,[(0,280),(.2,900),(.4,1900)],.75,rng)*sa.env(.4,(0,0),(.2,1),(.4,0))
    tether=sa.partials(135,.26,((1,1,1),(3,.08,.7)),.11,.02)
    return sa.finish(sa.mix(lift*.15,(.17,tether*.18)),'cast')
def skylatch_impact(v,rng):
    release=sa.lowpass(sa.noise(.4,rng),1700)*sa.env(.4,(0,0),(.04,.8),(.4,0))
    unlace=sa.sine(sa.sweep(190,110,.20))*sa.env(.20,(0,0),(.02,.6),(.20,0))
    return sa.finish(sa.mix(release*.15,unlace*.08),'impact')

def thresherwind_cue(v,rng):
    layers=[]
    for at in (0,.15,.30):
        blade=sa.moving_band(.12,[(0,3600),(.12,1400)],.5,rng)*sa.env(.12,(0,0),(.025,1),(.12,0))
        layers.append((at,blade*.16))
    return sa.finish(sa.mix(*layers),'cast')
def thresherwind_impact(v,rng):
    grain=_fit(sa.bandpass(sa.grains(.45,70,rng),1700,5300),.45)*sa.env(.45,(0,.8),(.45,0))
    intake=sa.lowpass(sa.noise(.35,rng),900)*sa.env(.35,(0,0),(.14,1),(.35,0))
    return sa.finish(sa.mix(grain*.1,(.15,intake*.12)),'impact')

EVENTS=[]
for name,cue,impact in [('springbed',springbed_cue,springbed_impact),('cinder_sieve',cinder_sieve_cue,cinder_sieve_impact),
    ('ashen_mercy',ashen_mercy_cue,ashen_mercy_impact),('clockroot',clockroot_cue,clockroot_impact),
    ('skylatch',skylatch_cue,skylatch_impact),('thresherwind',thresherwind_cue,thresherwind_impact)]:
    EVENTS += [event('fieldfusion_'+name+'_cue',cue,role='cast',subtitle='cast'),
               event('fieldfusion_'+name+'_impact',impact,role='impact',subtitle='hit')]
