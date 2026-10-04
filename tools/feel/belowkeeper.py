"""Original physical reading, binding, struck hollow rim and anchoring fibres; no magical phrase."""
from feel.core import sa,event

def read(v,rng):
 # Three unequal paper movements: stiff corner, full leaf rasp, settling edge.
 first=sa.norm(sa.bandpass(sa.noise(.09,rng),900,4500))*sa.env(.09,(0,0),(.008,1),(.038,.38),(.09,0))*.045
 leaf=sa.highpass(sa.lowpass(sa.noise(.24,rng),6800),1450)*sa.env(.24,(0,0),(.025,.4),(.06,1),(.17,.45),(.24,0))*.065
 edge=sa.norm(sa.bandpass(sa.noise(.075,rng),1800,5700))*sa.env(.075,(0,0),(.008,.8),(.075,0))*.027
 return sa.finish(sa.lowpass(sa.mix(first,(.045,leaf),(.29,edge)),5200,order=4),'effect')

def restore(v,rng):
 # A copper seating tap followed by pull-through binding and a lower second contact.
 seat=sa.partials(286,.19,((1,1,.9),(2.63,.25,.35),(4.1,.10,.2)),.067)
 fibre=sa.highpass(sa.lowpass(sa.noise(.31,rng),3200),380)*sa.env(.31,(0,0),(.035,.35),(.18,1),(.31,0))*.049
 snug=sa.partials(198,.16,((1,1,1),(3.17,.16,.35)),.045)
 return sa.finish(sa.mix(seat,(.09,fibre),(.36,snug)),'effect')

def bell(v,rng):
 # Finger contact and four inharmonic copper rim modes, no clapper or melodic second note.
 touch=sa.highpass(sa.noise(.035,rng),2100)*sa.env(.035,(0,0),(.004,1),(.035,0))*.035
 rim=sa.partials(470,.86,((1,1,1),(2.34,.34,.31),(3.78,.17,.18),(5.16,.065,.11)),.12)
 leather=sa.lowpass(sa.noise(.055,rng),900)*sa.env(.055,(0,0),(.014,.4),(.055,0))*.02
 return sa.finish(sa.mix(touch,(.008,rim),leather),'effect')

def anchor(v,rng):
 # Two unequal grounded leather contacts with fibre tension closing between them.
 left=sa.norm(sa.bandpass(sa.noise(.105,rng),280,1600))*sa.env(.105,(0,0),(.008,1),(.105,0))*.25
 right=sa.norm(sa.bandpass(sa.noise(.085,rng),420,2300))*sa.env(.085,(0,0),(.006,.75),(.085,0))*.19
 fibre=sa.norm(sa.bandpass(sa.noise(.21,rng),620,2900))*sa.env(.21,(0,0),(.055,.85),(.21,0))*.12
 root=sa.partials(155,.13,((1,.18,1),(2.15,.22,.5),(3.1,.15,.35)),.025)*.30
 return sa.finish(sa.mix(left,(.07,fibre),(.16,right),(.17,root)),'effect')
EVENTS=[event('belowkeeper_'+n,f,role='effect',subtitle='drainhouse.'+n) for n,f in [('read',read),('restore',restore),('bell',bell),('anchor',anchor)]]
