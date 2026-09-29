"""Fire's kit sounds. Own this file if you own fire: add builders and list them in EVENTS (see tools/feel/core.py).

The two below are worked examples to copy: a short one with a pitch step per variant, and a low one built from noise, a body and grains.
Everything tonal sits in D major pentatonic (sa.note(degree, octave) with the degrees sa.D, sa.E, sa.FS, sa.A, sa.B).
"""
from feel.core import sa, event


def fire_flick(v, rng):
    """A single ignition: a dry 'tik' and a short 'ssst'. Three variants, each a step higher up the scale."""
    dur = 0.25
    tik = sa.norm(sa.bandpass(sa.noise(0.03, rng), 2500, 7000)) * sa.decay(0.03, 0.004, 0.0003)
    hiss = sa.moving_band(dur, [(0, 4200), (dur, 2000)], 0.9, rng) * sa.decay(dur, 0.06, 0.002)
    embers = sa.norm(sa.bandpass(sa.grains(dur, 90, rng, shape=[(0, 1), (dur, 0)]), 1500, 6000))
    ping = sa.sine(sa.note((sa.D, sa.E, sa.FS)[v], 2), dur) * sa.decay(dur, 0.05, 0.002)
    x = sa.mix(0.8 * tik, (0.01, 0.5 * hiss), 0.3 * embers, 0.12 * ping)
    return sa.finish(sa.reverb(x, 0.4, 0.08), "impact")


def fire_whump(v, rng):
    """Catching: a soft low 'fwoomp' (low-passed noise, a 65 to 90 Hz thump, ember grains)."""
    dur = 0.6
    body = sa.thump(90 - 12 * v, 55, dur, 0.1, drive=1.4)
    fwoomp = sa.norm(sa.lowpass(sa.noise(dur, rng), 420)) * sa.decay(dur, 0.12, 0.01)
    embers = sa.norm(sa.bandpass(sa.grains(dur, 70, rng, shape=[(0, 1), (0.3, 0.5), (dur, 0)]), 1200, 5000))
    x = sa.mix(0.7 * body, 0.6 * fwoomp, 0.25 * embers)
    return sa.finish(sa.reverb(sa.saturate(x, 1.3), 0.6, 0.1), "impact")


EVENTS = [
    event("fire_flick", fire_flick, variants=3, role="impact", subtitle="hit"),
    event("fire_whump", fire_whump, variants=2, role="impact", subtitle="hit"),
]
