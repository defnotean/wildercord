"""Two voices find a shared pitch, exchange a short breath, then part. No continuous loop."""
from feel.core import sa,event
def begin(v,rng):
 x=sa.mix(sa.partials(294,.65,((1,1,1),(2,.15,.7)),.13),(.09,sa.partials(440,.6,((1,1,1),(2.1,.12,.5)),.17)),
  (.28,sa.partials(588,.45,((1,1,1),(3,.08,.6)),.14)))
 return sa.finish(sa.reverb(sa.highpass(x,120),.16,.04,damp=4500),'effect')
def flow(v,rng):
 x=sa.mix(sa.partials(588,.14,((1,1,1),(2,.08,.4)),.03),(.06,sa.partials(440,.19,((1,1,1),(2.5,.07,.4)),.07)))
 return sa.finish(sa.highpass(x,170),'ui')
def end(v,rng):
 x=sa.mix(sa.partials(440,.35,((1,1,1),(2,.1,.4)),.08),(.13,sa.partials(294,.4,((1,1,1),(3,.08,.6)),.14)))
 return sa.finish(sa.reverb(sa.highpass(x,120),.12,.03,damp=3600),'effect')
UNITY_EVENTS=[event('aura_unity_begin',begin,role='effect',subtitle='unity.begin'),
 event('aura_unity_flow',flow,role='ui',subtitle='unity.flow'),event('aura_unity_end',end,role='effect',subtitle='unity.end')]
