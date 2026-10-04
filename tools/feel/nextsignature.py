"""24 original material-interaction voices. Not in live PARTS until complete promotion."""
from feel.core import sa,event
import numpy as np
def fit(x,duration):
    n=sa.samples(duration);return np.pad(x,(0,max(0,n-len(x))))[:n]
def nullcatch_cue(v,rng):
    teeth=sa.mix(*[(i*.055,.11*sa.partials(f,.09,((1,1,1),(2.73,.3,.4),(5.6,.08,.2)),.023,.001)) for i,f in enumerate((920,740,1120))])
    intake=sa.moving_band(.4,[(0,2300),(.2,850),(.4,170)],.55,rng)*sa.env(.4,(0,0),(.12,.8),(.4,0))
    return sa.finish(sa.mix(teeth,(.08,intake*.13)),'cast')
def nullcatch_impact(v,rng):
    crunch=fit(sa.bandpass(sa.grains(.16,130,rng),1800,6500),.16)*sa.env(.16,(0,1),(.16,0))
    void=sa.sine(sa.sweep(470,220,.19))*sa.env(.19,(0,0),(.03,.9),(.19,0))
    return sa.finish(sa.mix(crunch*.12,(.04,void*.17)),'impact')
def second_bell_cue(v,rng):
    first=sa.partials(420,.31,((1,1,1),(2.76,.4,.6),(4.92,.1,.25)),.12,.001)
    second=sa.partials(650,.25,((1,1,1),(2.63,.35,.7)),.12,.001)
    wire=sa.bandpass(sa.noise(.08,rng),2200,5000)*sa.env(.08,(0,.9),(.08,0))
    return sa.finish(sa.mix(first*.16,(.23,second*.13),(.12,wire*.05)),'cast')
def second_bell_impact(v,rng):
    struck=sa.partials(370,.23,((1,1,1),(2.82,.34,.5),(5.03,.07,.3)),.10,.001)
    arc=sa.bandpass(sa.noise(.06,rng),3100,7000)*sa.env(.06,(0,1),(.06,0))
    return sa.finish(sa.mix(struck*.20,arc*.09),'impact')
def red_ledger_cue(v,rng):
    knot=sa.partials(112,.27,((1,.14,1),(3.02,1,.8),(4.1,.25,.4)),.07,.004)
    marks=[]
    for i in range(3):
        scratch=sa.bandpass(sa.noise(.055,rng),700+i*160,3000+i*270)*sa.env(.055,(0,0),(.014,.8),(.055,0))
        marks.append((.07+i*.09,scratch*.09))
    return sa.finish(sa.mix(knot*.15,*marks),'cast')
def red_ledger_impact(v,rng):
    thread=sa.sine(sa.sweep(510,190,.13))*sa.env(.13,(0,0),(.008,1),(.13,0))
    ink=sa.lowpass(sa.bubbles(.09,6,rng,200,800),1400)
    return sa.finish(sa.mix(thread*.12,(.03,ink*.13)),'impact')
def quietus_cue(v,rng):
    stamps=sa.mix(*[(at,.14*sa.partials(f,.075,((1,1,1),(3.31,.18,.35)),.019,.003)) for at,f in ((0,320),(.11,390),(.22,345))])
    sand=fit(sa.bandpass(sa.grains(.3,85,rng),1300,4800),.3)*sa.env(.3,(0,.8),(.3,0))
    return sa.finish(sa.mix(stamps,(.04,sa.reverse(sand)*.08)),'cast')
def quietus_impact(v,rng):
    shut=sa.partials(310,.15,((1,1,1),(2.97,.26,.4)),.025,.003)
    spill=fit(sa.bandpass(sa.grains(.23,60,rng),1800,5300),.23)*sa.env(.23,(0,.5),(.23,0))
    return sa.finish(sa.mix(shut*.24,(.025,spill*.1)),'impact')
def blood_escrow_cue(v,rng):
    squeeze=sa.bandpass(sa.brown(.37,rng),260,950)*sa.env(.37,(0,0),(.08,.7),(.20,.9),(.37,0))
    vessel=sa.partials(340,.36,((1,1,1),(3.12,.18,.4)),.16,.055)
    return sa.finish(sa.mix(squeeze*.13,(.06,vessel*.12)),'cast')
def blood_escrow_impact(v,rng):
    pump=sa.bubbles(.22,10,rng,140,480)*sa.env(.22,(0,0),(.05,1),(.22,0))
    lip=sa.partials(480,.13,((1,1,1),(2.23,.2,.3)),.035,.003)
    return sa.finish(sa.mix(pump*.19,(.12,lip*.08)),'impact')
def frost_molt_cue(v,rng):
    peel=sa.moving_band(.4,[(0,900),(.19,3100),(.4,1700)],.6,rng)*sa.env(.4,(0,0),(.1,.6),(.25,1),(.4,0))
    scale=sa.partials(1020,.17,((1,1,1),(3.52,.22,.4)),.05,.006)
    return sa.finish(sa.mix(peel*.12,(.19,scale*.10)),'cast')
def frost_molt_impact(v,rng):
    crack=fit(sa.bandpass(sa.grains(.19,125,rng),1500,7000),.19)*sa.env(.19,(0,1),(.19,0))
    fibre=sa.sine(sa.sweep(320,190,.12))*sa.env(.12,(0,0),(.008,.9),(.12,0))
    return sa.finish(sa.mix(crack*.13,(.03,fibre*.09)),'impact')
def pulse_ferry_cue(v,rng):
    sap=sa.bubbles(.32,16,rng,180,620)*sa.env(.32,(0,0),(.12,1),(.32,0))
    cradle=sa.partials(260,.13,((1,1,1),(3.73,.14,.3)),.03,.001)
    return sa.finish(sa.mix(sap*.14,(.04,cradle*.13),(.29,cradle*.10)),'cast')
def pulse_ferry_impact(v,rng):
    receive=sa.sine(sa.sweep(330,490,.19))*sa.env(.19,(0,0),(.03,.8),(.19,0))
    fold=sa.bandpass(sa.noise(.16,rng),800,2100)*sa.env(.16,(0,0),(.065,.6),(.16,0))
    return sa.finish(sa.mix(receive*.11,(.08,fold*.07)),'impact')
def last_lantern_cue(v,rng):
    handle=sa.partials(710,.22,((1,1,1),(2.85,.21,.45)),.08,.001)
    sand=fit(sa.bandpass(sa.grains(.4,75,rng),2100,4600),.4)*sa.env(.4,(0,.75),(.4,0))
    return sa.finish(sa.mix(handle*.12,(.05,sa.reverse(sand)*.11)),'cast')
def last_lantern_impact(v,rng):
    latch=sa.partials(390,.17,((1,1,1),(3.19,.28,.3)),.04,.001)
    wick=sa.moving_band(.26,[(0,2100),(.26,350)],.6,rng)*sa.env(.26,(0,0),(.035,1),(.26,0))
    return sa.finish(sa.mix(latch*.19,(.03,wick*.1)),'impact')
def pocket_current_cue(v,rng):
    fold=sa.bubbles(.37,14,rng,110,420)*sa.env(.37,(0,0),(.1,.8),(.37,0))
    hinge=sa.partials(155,.12,((1,1,1),(4.43,.09,.3)),.025,.002)
    return sa.finish(sa.mix(fold*.19,(.17,hinge*.13)),'cast')
def pocket_current_impact(v,rng):
    glug=sa.bubbles(.23,9,rng,170,540)*sa.env(.23,(0,.8),(.23,0))
    close=sa.lowpass(sa.noise(.045,rng),900)*sa.env(.045,(0,.9),(.045,0))
    return sa.finish(sa.mix(glug*.17,(.15,close*.11)),'impact')
def wayline_cue(v,rng):
    glass=sa.mix(*[(i*.10,.11*sa.partials(f,.15,((1,1,1),(3.16,.18,.45)),.06,.001)) for i,f in enumerate((690,870,760))])
    rope=sa.bandpass(sa.brown(.35,rng),180,900)*sa.env(.35,(0,0),(.25,.8),(.35,0))
    return sa.finish(sa.mix(glass,rope*.12),'cast')
def wayline_impact(v,rng):
    tug=sa.partials(120,.23,((1,1,1),(2.04,.3,.5),(3.9,.05,.3)),.05,.003)
    clasp=sa.partials(560,.09,((1,1,1),(2.7,.2,.3)),.02,.001)
    return sa.finish(sa.mix(tug*.14,(.02,clasp*.11)),'impact')
def night_seam_cue(v,rng):
    incision=sa.moving_band(.3,[(0,1400),(.3,3300)],.42,rng)*sa.env(.3,(0,0),(.07,.8),(.3,0))
    bead=sa.partials(940,.17,((1,1,1),(2.87,.17,.35)),.07,.002)
    return sa.finish(sa.mix(incision*.1,(.21,bead*.11)),'cast')
def night_seam_impact(v,rng):
    stitch=sa.bandpass(sa.noise(.06,rng),1100,3900)*sa.env(.06,(0,0),(.01,1),(.06,0))
    clink=sa.partials(780,.16,((1,1,1),(3.09,.18,.4)),.07,.001)
    return sa.finish(sa.mix(stitch*.11,(.045,clink*.09)),'impact')
def shard_compass_cue(v,rng):
    grit=fit(sa.bandpass(sa.grains(.43,90,rng),650,3100),.43)*sa.env(.43,(0,0),(.13,.9),(.43,0))
    needle=sa.partials(570,.11,((1,1,1),(2.61,.22,.4)),.03,.001)
    return sa.finish(sa.mix(grit*.13,(.34,needle*.15)),'cast')
def shard_compass_impact(v,rng):
    pebble=sa.partials(430,.10,((1,1,1),(4.27,.12,.25)),.025,.004)
    scuff=sa.bandpass(sa.noise(.15,rng),350,1600)*sa.env(.15,(0,0),(.025,.8),(.15,0))
    return sa.finish(sa.mix(pebble*.15,(.025,scuff*.07)),'impact')
EVENTS=[]
for name in ('nullcatch','second_bell','red_ledger','quietus','blood_escrow','frost_molt','pulse_ferry','last_lantern','pocket_current','wayline','night_seam','shard_compass'):
    EVENTS += [event('nextsignature_'+name+'_cue',globals()[name+'_cue'],role='cast',subtitle='cast'),
               event('nextsignature_'+name+'_impact',globals()[name+'_impact'],role='impact',subtitle='hit')]
