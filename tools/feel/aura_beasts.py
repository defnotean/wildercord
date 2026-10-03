"""Heavy hoof/stone voices and short feathered ridge calls; distinct warning, movement and settling cues."""
from feel.core import sa,event
def stamp(v,rng):
 x=sa.mix(sa.thump(86,25,.4,.025,drive=1.8,knock=.5),(.28,sa.thump(72,28,.5,.04,drive=1.4,knock=.3)),(.08,sa.partials(62,.8,((1,1,1),(1.8,.4,.7),(3.7,.12,.3)),.2)))
 x=sa.mix(x,sa.partials(340,.35,((1,1,1),(2.6,.2,.3)),.32))
 return sa.finish(sa.highpass(sa.reverb(x,.16,.08,damp=1800),65),'effect')
def charge(v,rng):
 x=sa.mix(sa.thump(140,35,.25,.008,drive=2,knock=.7),(.13,sa.thump(120,30,.25,.012,drive=1.7,knock=.5)),(.27,sa.thump(155,40,.3,.01,drive=1.8,knock=.5)))
 x=sa.mix(x,sa.partials(380,.3,((1,1,1),(3.2,.2,.25)),.32))
 return sa.finish(sa.highpass(x,75),'effect')
def stone_impact(v,rng):
 x=sa.mix(sa.thump(260,38,.55,.004,drive=2.2,knock=.9),(.03,sa.partials(115,.35,((1,1,1),(3.17,.6,.4),(7.2,.12,.2)),.25)))
 return sa.finish(sa.reverb(x,.24,.09,damp=2400),'effect')
def grazing(v,rng):
 x=sa.mix(sa.thump(45,28,.18,.035,drive=1,knock=.1),(.17,sa.partials(72,.24,((1,1,1),(2.2,.18,.4)),.09)))
 x=sa.mix(x,sa.partials(420,.22,((1,1,1),(2,.13,.3)),.19))
 return sa.finish(sa.highpass(sa.lowpass(x,1200),80),'effect')
def ridge_warning(v,rng):
 x=sa.mix(sa.partials(710,.22,((1,1,1),(2.01,.18,.4),(3,.08,.2)),.2),(.29,sa.partials(530,.32,((1,1,1),(2.02,.16,.4)),.17)),(.13,sa.thump(150,80,.2,.02,drive=1,knock=.15)))
 return sa.finish(sa.reverb(x,.18,.07,damp=6200),'effect')
def leap(v,rng):
 x=sa.mix(sa.thump(220,95,.18,.01,drive=1.2,knock=.3),(.03,sa.partials(430,.25,((1,1,1),(2.2,.2,.3),(4.1,.08,.15)),.12)))
 return sa.finish(sa.highpass(x,170),'effect')
def feather_land(v,rng):
 x=sa.mix(sa.thump(125,40,.22,.013,drive=1.1,knock=.3),(.11,sa.thump(175,65,.14,.01,drive=1,knock=.25)))
 x=sa.mix(x,sa.partials(460,.14,((1,1,1),(3.1,.2,.3)),.25))
 return sa.finish(sa.highpass(sa.lowpass(x,3400),100),'effect')
def whistle(v,rng):
 x=sa.mix(sa.partials(870,.18,((1,1,1),(2,.1,.4)),.18),(.23,sa.partials(1160,.35,((1,1,1),(2,.09,.3)),.16)))
 return sa.finish(sa.reverb(x,.28,.1,damp=6500),'effect')
BEAST_EVENTS=[event('aura_stonehorn_'+name,build,role='effect',subtitle='stonehorn.'+name) for name,build in [('warn',stamp),('charge',charge),('impact',stone_impact),('forage',grazing)]]
BEAST_EVENTS += [event('aura_galeclaw_'+name,build,role='effect',subtitle='galeclaw.'+name) for name,build in [('warn',ridge_warning),('leap',leap),('land',feather_land),('whistle',whistle)]]
def grumble(v,rng):
 x=sa.partials(118,.6,((1,1,1),(2.8,.55,.8),(4.2,.18,.3)),.18)
 return sa.finish(sa.highpass(sa.reverb(x,.12,.03,damp=1600),85),'effect')
def plate_hurt(v,rng):
 x=sa.mix(sa.thump(240,65,.22,.006,drive=1.7,knock=.7),sa.partials(450,.16,((1,1,1),(2.73,.32,.3)),.28))
 return sa.finish(sa.highpass(x,100),'effect')
def stone_death(v,rng):
 x=sa.mix(sa.partials(210,.7,((1,1,1),(1.7,.4,.5),(3.1,.18,.2)),.22),(.4,sa.thump(230,35,.45,.01,drive=1.3,knock=.6)))
 x=sa.mix(x,(.12,sa.partials(410,.42,((1,1,1),(2.3,.18,.3)),.28)))
 return sa.finish(sa.highpass(sa.reverb(x,.3,.1,damp=2400),70),'effect')
def ridge_call(v,rng):
 x=sa.mix(sa.partials(990,.14,((1,1,1),(2,.12,.3)),.16),(.19,sa.partials(790,.19,((1,1,1),(2,.1,.3)),.16)),(.44,sa.partials(990,.25,((1,1,1),(2,.12,.3)),.13)))
 return sa.finish(sa.reverb(x,.22,.08,damp=5800),'effect')
def bird_hurt(v,rng):
 x=sa.partials(1280,.2,((1,1,1),(1.45,.2,.4),(2.01,.1,.2)),.22)
 return sa.finish(sa.highpass(x,350),'effect')
def bird_death(v,rng):
 x=sa.mix(sa.partials(650,.28,((1,1,1),(2,.14,.3)),.2),(.19,sa.partials(430,.46,((1,1,1),(2.8,.16,.3)),.19)))
 return sa.finish(sa.reverb(x,.27,.08,damp=3900),'effect')
BEAST_EVENTS += [event('aura_stonehorn_'+name,build,role='effect',subtitle='stonehorn.'+name) for name,build in [('grumble',grumble),('hurt',plate_hurt),('death',stone_death)]]
BEAST_EVENTS += [event('aura_galeclaw_'+name,build,role='effect',subtitle='galeclaw.'+name) for name,build in [('call',ridge_call),('hurt',bird_hurt),('death',bird_death)]]
