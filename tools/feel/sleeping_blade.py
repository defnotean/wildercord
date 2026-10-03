"""A listening stone, three straining notes, freed steel and Oathkeeper's answer."""
from feel.core import sa,event

def listening(v,rng):
    x=sa.mix(sa.thump(92,30,.65,.035,drive=1.15,knock=.3),(.24,.35*sa.bell(sa.note(sa.D,1),1.1,.4,2,.6)))
    return sa.finish(sa.reverb(x,.22,.12,damp=2200),'effect')
def straining(v,rng):
    x=sa.mix(sa.partials(sa.note(sa.A,1),.6,((1,1,1),(2.31,.3,.6),(5.12,.12,.3)),.22),(.2,sa.thump(145,45,.4,.012,drive=1.6,knock=.5)))
    return sa.finish(sa.highpass(sa.reverb(x,.19,.08,damp=4100),70),'effect')
def drawing(v,rng):
    x=sa.mix(sa.thump(220,55,.5,.01,drive=1.3,knock=.8),(.08,sa.bell(sa.note(sa.D,2),1.3,.5,2,.7)),(.29,.65*sa.bell(sa.note(sa.A,2),1.0,.4,2,.6)))
    return sa.finish(sa.reverb(x,.3,.13,damp=4800),'effect')
def answering(v,rng):
    x=sa.mix(sa.partials(sa.note(sa.E,2),.28,((1,1,1),(3.4,.3,.5),(6.7,.12,.2)),.3),(.07,sa.thump(190,70,.2,.007,drive=1.7,knock=.6)))
    return sa.finish(sa.highpass(sa.reverb(x,.12,.04,damp=5600),150),'effect')
SLEEPING_BLADE_EVENTS=[event('aura_blade_stone_listen',listening,role='effect',subtitle='sleeping_blade.listen'),
 event('aura_blade_stone_strain',straining,role='effect',subtitle='sleeping_blade.strain'),
 event('aura_blade_stone_draw',drawing,role='effect',subtitle='sleeping_blade.draw'),
 event('aura_oathkeeper_answer',answering,role='effect',subtitle='sleeping_blade.answer')]
