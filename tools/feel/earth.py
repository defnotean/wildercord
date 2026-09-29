"""Earth's kit sounds: one per verb (docs/audit/spell-feel-storm-earth.md, section 5).

Earth is weight and grit: thuds, grinds, clacks, crunches. Tonal parts sit in D major pentatonic.
"""
import numpy as np

from feel.core import sa, event


def fit(x, dur):
    """Pads or trims a sound to exactly `dur` seconds (grains and filters can run a little long)."""
    n = sa.samples(dur)
    x = np.asarray(x, dtype=float)
    return np.pad(x, (0, max(0, n - len(x))))[:n]


def grains(dur, *args, **kwargs):
    return fit(sa.grains(dur, *args, **kwargs), dur)


def _debris(dur, rng, rate=600, lo=180, hi=3200, shape=None):
    return sa.norm(sa.bandpass(grains(dur, rate, rng, length=(0.003, 0.012), shape=shape or [(0, 1), (0.2, 0.5), (dur, 0.02)]), lo, hi))


def _knock(rng, lo=200, hi=1500, tau=0.018):
    return sa.norm(sa.bandpass(sa.noise(0.12, rng), lo, hi)) * sa.decay(0.12, tau, 0.0005)


def _thud(dur, f0, rng, tau):
    body = sa.thump(f0, 45, dur, tau, drive=2.4)
    grit = sa.norm(sa.bandpass(sa.noise(dur, rng), 300, 2000)) * sa.decay(dur, tau * 0.6, 0.002)
    x = sa.mix(0.9 * body, 0.6 * _knock(rng), 0.7 * _debris(dur, rng), 0.3 * sa.norm(sa.lowpass(sa.brown(dur, rng), 150)) * sa.decay(dur, dur * 0.3, 0.01), 0.6 * grit)
    return sa.reverb(x, dur, 0.1, damp=2500)


def earth_stomp(v, rng):
    """S17 small: a short stamp of weight (a hoof, a small aftershock)."""
    return sa.finish(_thud(0.4, 140 - 10 * v, rng, 0.06), "impact")


def earth_quake(v, rng):
    """S17 mid: ground weight (a wave of a quake, the first blow of an aftershock)."""
    return sa.finish(_thud(0.7, 120 - 10 * v, rng, 0.11), "impact")


def earth_slam(v, rng):
    """S17 large: the ground heaves (Tremor, a Monolith landing)."""
    return sa.finish(sa.saturate(_thud(1.1, 105 - 8 * v, rng, 0.16), 1.2), "grand")


def earth_grind(v, rng):
    """S18: stone on stone, rising (a pillar or a wall coming up)."""
    dur = 0.7
    grind = sa.moving_band(dur, [(0, 300), (dur, 900 + 100 * v)], 0.8, rng) * sa.swell(dur, 0.55)
    grit = sa.norm(sa.lowpass(grains(dur, 420, rng, length=(0.002, 0.008), shape=[(0, 0.2), (dur * 0.8, 1), (dur, 0.3)]), 2800))
    rumble = sa.norm(sa.lowpass(sa.brown(dur, rng), 170)) * sa.swell(dur, 0.5)
    x = sa.mix(0.6 * grind, 0.6 * grit, 0.4 * rumble)
    return sa.finish(sa.reverb(x, 0.6, 0.08, damp=2500), "effect")


def earth_clamp(v, rng):
    """S19: plates locking on (Stoneskin mid, Brace short and high, Stoneform deep: the variants)."""
    dur = 0.35
    f = (180, 260, 120)[v]
    x = sa.mix(0.8 * sa.tick(900 + 300 * v, rng), 0.5 * sa.thump(f, f * 0.4, dur, 0.05, drive=1.8),
               0.8 * sa.norm(sa.bandpass(sa.noise(dur, rng), 400, 1600)) * sa.decay(dur, 0.04, 0.001))
    return sa.finish(sa.reverb(x, 0.3, 0.06, damp=3000), "impact")


def earth_press(v, rng):
    """S20: a sinking groan and an anvil-weight thump at the end (Weigh)."""
    dur = 0.6
    groan = sa.soft_saw(sa.sweep(240, 110, dur, 1.0), dur, harmonics=6) * sa.env(dur, (0, 0), (0.1, 1), (dur, 0.3))
    x = sa.mix(0.6 * groan, (0.42, 0.8 * sa.thump(90, 40, 0.18, 0.05, drive=2.0)), (0.42, 0.4 * _knock(rng, 300, 2500)))
    return sa.finish(sa.reverb(x, 0.4, 0.06, damp=2500), "impact")


def earth_chain(v, rng):
    """S21: metal on metal, taut (a Shackle binding, v0; a yank, v1..)."""
    dur = 0.3
    clank = sa.partials(1900 + 250 * v, dur, [(1, 1.0, 1.0), (2.76, 0.5, 0.6), (5.4, 0.3, 0.3)], 0.05)
    x = sa.mix(0.6 * clank, 0.5 * sa.norm(sa.bandpass(sa.noise(0.05, rng), 2000, 7000)) * sa.decay(0.05, 0.006, 0.0003))
    return sa.finish(sa.reverb(x, 0.3, 0.06), "impact")


def earth_gloop(v, rng):
    """S22: a slow bubble and a squelch (Mire)."""
    dur = 0.5
    bloop = sa.sine(sa.sweep(300 - 40 * v, 120, dur, 0.3), dur) * sa.decay(dur, 0.08, 0.005)
    squelch = sa.norm(sa.bandpass(sa.noise(dur, rng), 150, 600)) * sa.decay(dur, 0.12, 0.02) * (1 + 0.5 * sa.chopper(dur, rng, rate=(18, 30)))
    return sa.finish(sa.reverb(sa.mix(0.6 * bloop, 0.5 * squelch), 0.3, 0.05, damp=2000), "impact")


def earth_creak(v, rng):
    """S23: wood stretching, leaves rustling (Root taking hold)."""
    dur = 0.5
    creak = sa.fm(sa.glide(dur, (0, 180 + 30 * v), (dur, 140)), 0.51, 3.0, dur) * sa.chopper(dur, rng, rate=(25, 45), floor=0.2) * sa.swell(dur, 0.3)
    rustle = sa.norm(sa.bandpass(grains(dur, 300, rng), 2500, 7000)) * sa.decay(dur, 0.2, 0.02)
    return sa.finish(sa.reverb(sa.mix(0.5 * creak, 0.3 * rustle), 0.3, 0.06), "impact")


def earth_hiss(v, rng):
    """S24: wind-blown grit (a Sandstorm's pulse)."""
    dur = 1.0
    hiss = sa.moving_band(dur, [(0, 1800), (0.5, 3500), (dur, 2400)], 0.8, rng) * sa.env(dur, (0, 0), (0.3, 1), (dur, 0))
    grit = sa.norm(sa.bandpass(grains(dur, 500, rng), 1500, 6000)) * sa.env(dur, (0, 0), (0.3, 1), (dur, 0))
    return sa.finish(sa.mix(0.6 * hiss, 0.4 * grit), "effect")


def earth_snap(v, rng):
    """S25: dry and hollow: a bone knock and a high crack, a step higher per variant (Bonespur)."""
    dur = 0.18
    knock = sa.partials(sa.note((sa.D, sa.E, sa.FS, sa.A)[v], 1), dur, [(1, 1.0, 1.0), (2.3, 0.5, 0.5)], 0.03)
    crack = sa.norm(sa.bandpass(sa.noise(0.05, rng), 2500, 8000)) * sa.decay(0.05, 0.005, 0.0002)
    return sa.finish(sa.mix(0.6 * knock, 0.7 * crack), "impact")


def earth_sink(v, rng):
    """S26: the floor going out from under you: a falling rumble and a reversed suction whoomp (Sinkhole)."""
    dur = 0.9
    rumble = sa.norm(sa.lowpass(sa.brown(dur, rng), 300)) * sa.swell(dur, 0.5)
    whoomp = sa.reverse(sa.norm(sa.lowpass(sa.noise(0.5, rng), 500)) * sa.decay(0.5, 0.1, 0.005))
    sub = sa.sine(sa.sweep(90, 35, dur, 1.0), dur) * sa.swell(dur, 0.6)
    mid = sa.norm(sa.bandpass(sa.noise(dur, rng), 300, 1500)) * sa.swell(dur, 0.5)
    return sa.finish(sa.reverb(sa.mix(0.5 * rumble, (0.35, 0.6 * whoomp), 0.4 * sub, 0.5 * mid), 0.8, 0.12, damp=2000), "impact")


def earth_tick(v, rng):
    """S27: a stone tick, a step higher per stage (Fossilize's ramp: variants D, E, F#)."""
    dur = 0.2
    f = sa.note((sa.D, sa.E, sa.FS)[v], 1)
    x = sa.mix(0.6 * sa.partials(f, dur, [(1, 1.0, 1.0), (2.7, 0.4, 0.5)], 0.04), 0.5 * _knock(rng, 400, 3000, 0.01))
    return sa.finish(x, "tick")


def earth_crack(v, rng):
    """S27 end: stone locks and cracks (Fossilize's stone, the crack of a brittle thing)."""
    dur = 0.6
    lock = sa.thump(120, 50, 0.3, 0.05, drive=2.0)
    crack = sa.norm(sa.bandpass(sa.noise(0.3, rng), 500, 4000)) * sa.decay(0.3, 0.03, 0.0005)
    x = sa.mix(0.7 * lock, (0.08, 0.8 * crack), (0.08, 0.6 * _debris(0.5, rng)))
    return sa.finish(sa.reverb(x, 0.5, 0.08, damp=3000), "impact")


def earth_bloop(v, rng):
    """S28: slow, thick lava: a bubble pop with a hiss (Magma)."""
    dur = 0.35
    pop = sa.sine(sa.sweep(420 + 60 * v, 180, dur, 0.3), dur) * sa.decay(dur, 0.05, 0.004)
    hiss = sa.norm(sa.bandpass(sa.noise(dur, rng), 3000, 8000)) * sa.env(dur, (0, 0), (0.05, 0.6), (dur, 0))
    return sa.finish(sa.mix(0.7 * pop, 0.3 * hiss), "pulse")


def earth_drop(v, rng):
    """S29: a whistle and a crunch (a Stalactite falling)."""
    dur = 0.5
    whistle = sa.sine(sa.sweep(2500, 600, dur, 1.3), dur) * sa.env(dur, (0, 0), (0.05, 0.6), (0.35, 1), (0.36, 0))
    crunch = _debris(0.2, rng, 900, 400, 4000) * sa.decay(0.2, 0.05, 0.001)
    x = sa.mix(0.35 * whistle, (0.35, 0.9 * crunch), (0.35, 0.6 * _knock(rng)))
    return sa.finish(sa.reverb(x, 0.3, 0.06), "impact")


def earth_ping(v, rng):
    """S30: a clean cut crystal (Geode), a step higher per variant."""
    dur = 0.6
    return sa.finish(sa.reverb(sa.glass(sa.note((sa.A, sa.B, sa.D + 5, sa.E + 5)[v], 1), dur, 0.25), 0.6, 0.15), "impact")


def earth_dig(v, rng):
    """S31: a pick biting rock: a noise burst and a wooden tap, pitched per variant (the mining runes)."""
    dur = 0.2
    bite = sa.norm(sa.bandpass(sa.noise(dur, rng), 800 + 300 * v, 4000)) * sa.decay(dur, 0.03, 0.0005)
    tap = sa.partials(300 + 120 * v, dur, [(1, 1.0, 1.0), (2.2, 0.3, 0.5)], 0.03)
    return sa.finish(sa.mix(0.7 * bite, 0.4 * tap, 0.4 * _debris(dur, rng, 500)), "impact")


def earth_rattle(v, rng):
    """S32: small stones on stone (Pelt, Infest's gnawing)."""
    dur = 0.25
    x = sa.norm(sa.bandpass(grains(dur, 40 + 10 * v, rng, length=(0.002, 0.006), shape=[(0, 1), (dur, 0.1)]), 300, 3000))
    return sa.finish(sa.mix(0.9 * x, 0.3 * _knock(rng, 600, 3000, 0.008)), "impact")


EVENTS = [
    event("earth_stomp", earth_stomp, variants=3, role="impact", subtitle="hit"),
    event("earth_quake", earth_quake, variants=3, role="impact", subtitle="hit", attenuation=32),
    event("earth_slam", earth_slam, variants=2, role="grand", subtitle="hit", attenuation=48),
    event("earth_grind", earth_grind, variants=2, role="effect", subtitle="field"),
    event("earth_clamp", earth_clamp, variants=3, role="impact", subtitle="cast"),
    event("earth_press", earth_press, variants=1, role="impact", subtitle="hit"),
    event("earth_chain", earth_chain, variants=3, role="impact", subtitle="hit"),
    event("earth_gloop", earth_gloop, variants=3, role="impact", subtitle="field"),
    event("earth_creak", earth_creak, variants=2, role="impact", subtitle="hit"),
    event("earth_hiss", earth_hiss, variants=3, role="effect", subtitle="field"),
    event("earth_snap", earth_snap, variants=4, role="impact", subtitle="hit"),
    event("earth_sink", earth_sink, variants=1, role="impact", subtitle="hit", attenuation=32),
    event("earth_tick", earth_tick, variants=3, role="tick", subtitle="tell"),
    event("earth_crack", earth_crack, variants=2, role="impact", subtitle="hit"),
    event("earth_bloop", earth_bloop, variants=3, role="pulse", subtitle="field"),
    event("earth_drop", earth_drop, variants=2, role="impact", subtitle="hit"),
    event("earth_ping", earth_ping, variants=4, role="impact", subtitle="hit"),
    event("earth_dig", earth_dig, variants=3, role="impact", subtitle="hit"),
    event("earth_rattle", earth_rattle, variants=3, role="impact", subtitle="hit"),
]
