"""Separate gate grinding, awakening, sweep scrape, thrust scrape and keeper impact voices."""
from feel.core import sa,event

def voice(kind):
    def build(v,rng):
        notes=(sa.D,sa.A,sa.E,sa.FS,sa.B,sa.D,sa.E,sa.D,sa.A,sa.FS)
        tone=sa.partials(sa.note(notes[kind],1),.45+kind*.035,((1,1,1),(2.71,.28,.45),(4.19,.13,.25)),.25)
        hit=sa.thump(170+kind*17,42+kind*4,.4,.015,drive=1.5,knock=.4+kind*.03)
        if kind==9:x=sa.mix(sa.bell(sa.note(sa.D,2),.8,.5,2,.6),(.18,.5*sa.bell(sa.note(sa.A,2),.9,.5,2,.7)),(.4,.3*tone))
        elif kind==2: x=sa.mix(tone,(.13,.4*tone),(.26,hit))
        elif kind==3:x=sa.mix(hit,(.3,tone))
        elif kind==4:x=sa.mix(tone,(.45,hit))
        else:x=sa.mix(hit,(.07,tone),(.19,.3*tone))
        return sa.finish(sa.highpass(sa.reverb(x,.25,.09,damp=3900),50),'effect')
    return build

TOMB_EVENTS=[event('aura_tomb_'+name,voice(i),role='effect',subtitle='gravekeeper.'+name) for i,name in enumerate(('gate','awake','brace','sweep_ready','thrust_ready','sweep','thrust','break','fall'))]
TOMB_EVENTS.append(event('aura_tomb_reward',voice(9),role='effect',subtitle='gravekeeper.reward'))
