"""Wooden steward knocks, a bell-led gathering and short ceremonial cadences."""
from feel.core import sa,event
def opening(v,rng):
 x=sa.mix(sa.partials(440,.9,((1,1,1),(2.76,.25,.6),(4.03,.1,.4)),.25),(.32,sa.thump(160,65,.22,.008,drive=1.1,knock=.5)),(.65,sa.thump(190,80,.25,.008,drive=1.1,knock=.5)))
 return sa.finish(sa.highpass(sa.reverb(x,.22,.08,damp=3600),90),'effect')
def round_won(v,rng):
 x=sa.mix(sa.thump(220,90,.18,.006,drive=1.2,knock=.6),(.18,sa.partials(587,.3,((1,1,1),(2,.18,.4)),.24)))
 return sa.finish(sa.highpass(x,110),'effect')
def victory(v,rng):
 x=sa.mix(sa.partials(294,.5,((1,1,1),(2,.2,.5)),.23),(.23,sa.partials(440,.55,((1,1,1),(2,.2,.5)),.23)),(.48,sa.partials(587,.8,((1,1,1),(2,.2,.5),(3,.1,.3)),.23)))
 return sa.finish(sa.highpass(sa.reverb(x,.22,.09,damp=4200),100),'effect')
def prize(v,rng):
 x=sa.mix(sa.thump(330,140,.12,.005,drive=1.1,knock=.4),(.15,sa.partials(880,.35,((1,1,1),(2.8,.13,.4)),.24)),(.36,sa.partials(660,.5,((1,1,1),(2,.13,.4)),.24)))
 return sa.finish(sa.highpass(sa.reverb(x,.18,.06,damp=4400),110),'effect')
TOURNAMENT_EVENTS=[event('aura_tournament_'+n,b,role='effect',subtitle='tournament.'+n) for n,b in [('open',opening),('round',round_won),('victory',victory),('prize',prize)]]
