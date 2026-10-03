"""Buried iron and weathered wood, with three separate remembered cadences."""
from feel.core import sa, event

def memory_voice(kind):
    def build(v, rng):
        if kind == 0:
            x=sa.mix(sa.thump(130,48,.55,.025,drive=1.2,knock=.3),(.08,.25*sa.partials(sa.note(sa.D,1),.9,((1,1,1),(2.41,.2,.4)),.4)))
        elif kind == 1:
            x=sa.mix(sa.thump(180,60,.3,.02,drive=1.4,knock=.65),(.18,.5*sa.bell(sa.note(sa.A,1),1.1,.5,2,.7,attack=.02)))
        elif kind == 2:
            first=sa.bell(sa.note(sa.E,1),.8,.5,2,.65,attack=.04)
            x=sa.mix(first,(.24,.5*sa.bell(sa.note(sa.B,1),1,.5,2,.6,attack=.04)))
        else:
            iron=sa.partials(sa.note(sa.FS,1),.6,((1,1,1),(2.7,.22,.5),(4.1,.1,.25)),.3)
            x=sa.mix(iron,(.28,.45*iron),(.5,.25*iron))
        return sa.finish(sa.highpass(sa.reverb(x,.4,.13,damp=4800),55),'effect')
    return build

BATTLEFIELD_EVENTS=[event('aura_memory_'+name,memory_voice(i),role='effect',subtitle='tell')
                   for i,name in enumerate(('begin','broken_line','last_shelter','returned_step'))]
