"""Tide, Iron and Dune's sounds (the methods-a pack): each method's family of a swing, a blow landing and a technique loosed,
its five arts, its awakening and its finisher. Not a part of its own: tools/feel/aura.py adds these to its events (in one methods-a block).

    tide   a wet, rushing swoosh; a slap of water and spray; a wave that rises, curls and breaks
    iron   a ringing steel swish; an anvil's clang and sparks; a forge's roar under hammer strikes
    dune   a hissing, gritty swoosh; a dull thud in sand and a trickle; a wind that lifts a wall of grit

Everything tonal is in D pentatonic like the rest of the mod.
"""
import numpy as np

from feel.core import sa, event

D, E, FS, A, B = sa.D, sa.E, sa.FS, sa.A, sa.B


def _clean(x, role, verb=0.35, wet=0.1, damp=7000.0):
    return sa.finish(sa.highpass(sa.reverb(x, verb, wet, damp=damp), 60), role)


def _whoosh(dur, path, width, rng, peak=0.3):
    return sa.moving_band(dur, path, width, rng) * sa.env(dur, (0, 0), (dur * peak, 1), (dur * 0.75, 0.3), (dur, 0))


# ================================================================ the three voices

def _water(dur, rng, lo=250, hi=2400, shape=None):
    """Rushing water: brown noise through a band, with bubbles under it."""
    body = sa.norm(sa.bandpass(sa.brown(dur, rng), lo, hi))
    if shape is not None:
        body = body * sa.env(dur, *shape)
    pops = sa.norm(sa.bubbles(dur, 30, rng)) if dur > 0.2 else np.zeros(sa.samples(dur))
    return sa.mix(0.85 * body, (0.0, 0.3 * pops[:len(body)]))


def _clang(f, dur, tau, rng, sparks=60):
    """Struck steel: an inharmonic bell and a spray of sparks."""
    ring = sa.mix(sa.bell(f, dur, tau, 2.76, 2.2), 0.5 * sa.bell(f * 1.5, dur, tau * 0.7, 2.4, 1.8))
    spray = sa.bandpass(sa.grains(dur, sparks, rng, shape=[(0, 1), (dur, 0)]), 3000, 9000)
    return sa.mix(0.8 * sa.norm(ring), 0.4 * sa.norm(spray))


def _grit(dur, rng, rate=160, lo=900, hi=4500, shape=None):
    """Sand: dense grains hissing through a high band."""
    shape = shape or [(0, 0.3), (dur * 0.3, 1), (dur, 0.1)]
    return sa.norm(sa.bandpass(sa.grains(dur, rate, rng, shape=shape), lo, hi))


# ================================================================ tide: brine

def tide_swing(v, rng):
    dur = 0.4
    air = _whoosh(dur, [(0, 500 + 40 * v), (0.13, 1900), (dur, 700)], 0.9, rng)
    x = sa.mix(0.7 * sa.norm(air), 0.5 * _water(dur, rng, 300, 1800, [(0, 0), (0.12, 1), (dur, 0)]))
    return _clean(x, "effect", 0.25, 0.08, 6000)


def tide_impact(v, rng):
    dur = 0.5
    slap = sa.norm(sa.bandpass(sa.noise(0.18, rng), 500, 3600)) * sa.decay(0.18, 0.04, 0.001)
    hit = sa.thump(130 - 8 * v, 55, 0.28, 0.07, drive=1.4)
    x = sa.mix(0.7 * slap, 0.5 * sa.highpass(hit, 80), (0.03, 0.45 * _water(dur - 0.03, rng, 600, 4200, [(0, 1), (dur, 0)])))
    return _clean(x, "impact", 0.35, 0.1, 6500)


def tide_art(v, rng):
    dur = 1.1
    wave = _water(dur, rng, 180, 2600, [(0, 0), (0.55, 1), (0.7, 0.8), (dur, 0)])
    crash = sa.norm(sa.bandpass(sa.noise(0.45, rng), 700, 6000)) * sa.decay(0.45, 0.12, 0.002)
    chord = sa.mix(sa.bell(sa.note(D, 0), 0.9, 0.35, 2.0, 1.2), 0.6 * sa.bell(sa.note(FS, 0), 0.9, 0.32, 2.0, 1.2))
    x = sa.mix(0.7 * wave, (0.55, 0.7 * crash), 0.3 * chord)
    return _clean(x, "cast", 0.6, 0.14, 6000)


# ================================================================ iron: metal

def iron_swing(v, rng):
    dur = 0.38
    air = _whoosh(dur, [(0, 900 + 80 * v), (0.12, 3200), (dur, 1400)], 0.5, rng)
    ring = sa.glass(sa.note(A, 1), dur, 0.12) * 0.25
    x = sa.mix(0.85 * sa.norm(air), ring)
    return _clean(x, "effect", 0.25, 0.08, 7500)


def iron_impact(v, rng):
    dur = 0.6
    hit = sa.thump(160 - 10 * v, 70, 0.25, 0.05, drive=2.0)
    x = sa.mix(0.55 * sa.highpass(hit, 90), 0.8 * _clang(sa.note(D, 1) * (1 + 0.02 * v), dur, 0.18, rng, 80))
    return _clean(x, "impact", 0.35, 0.1, 7000)


def iron_art(v, rng):
    dur = 1.15
    roar = sa.norm(sa.lowpass(sa.brown(dur, rng), 700)) * sa.env(dur, (0, 0), (0.3, 1), (0.7, 0.6), (dur, 0))
    blows = sa.mix((0.1, _clang(sa.note(D, 1), 0.6, 0.16, rng)), (0.42, _clang(sa.note(A, 0), 0.7, 0.2, rng)))
    x = sa.mix(0.45 * roar, 0.8 * blows)
    return _clean(x, "cast", 0.5, 0.14, 7000)


# ================================================================ dune: sand

def dune_swing(v, rng):
    dur = 0.42
    air = _whoosh(dur, [(0, 700 + 60 * v), (0.13, 2600), (dur, 1000)], 0.9, rng)
    x = sa.mix(0.6 * sa.norm(air), 0.5 * _grit(dur, rng, 180, 900, 4500, [(0, 0), (0.13, 1), (dur, 0)]))
    return _clean(x, "effect", 0.25, 0.08, 6500)


def dune_impact(v, rng):
    dur = 0.55
    thud = sa.thump(110 - 6 * v, 48, 0.3, 0.06, drive=1.3)
    trickle = _grit(dur, rng, 120, 700, 4000, [(0, 1), (dur, 0)])
    scuff = sa.norm(sa.bandpass(sa.noise(0.2, rng), 400, 2200)) * sa.decay(0.2, 0.05, 0.002)
    x = sa.mix(0.35 * sa.highpass(thud, 120), 0.6 * scuff, (0.03, 0.7 * trickle))
    return _clean(x, "impact", 0.3, 0.1, 6000)


def dune_art(v, rng):
    dur = 1.2
    wind = _whoosh(dur, [(0, 300), (0.5, 1800), (dur, 600)], 1.1, rng, peak=0.45)
    wall = _grit(dur, rng, 260, 900, 4500, [(0, 0), (0.5, 1), (dur, 0)])
    low = sa.sine(sa.note(D, -1), dur) * sa.env(dur, (0, 0), (0.4, 1), (dur, 0))
    x = sa.mix(0.6 * sa.norm(wind), 0.55 * wall, 0.35 * low)
    return _clean(x, "cast", 0.55, 0.14, 6000)


# ================================================================ the arts

def _art(voice, dur, f, accent, sparks=0):
    def build(v, rng):
        if voice == "tide":
            body = _water(dur, rng, 200, 2800, [(0, 0), (dur * accent, 1), (dur, 0)])
            hit = sa.norm(sa.bandpass(sa.noise(0.3, rng), 600, 5000)) * sa.decay(0.3, 0.08, 0.002)
        elif voice == "iron":
            body = sa.norm(sa.lowpass(sa.brown(dur, rng), 800)) * sa.env(dur, (0, 0), (dur * accent, 1), (dur, 0))
            hit = _clang(f, 0.7, 0.2, rng, sparks or 70)
        else:
            body = _grit(dur, rng, 220, 900, 4500, [(0, 0), (dur * accent, 1), (dur, 0)])
            hit = sa.mix(0.4 * sa.highpass(sa.thump(f * 2, 60, 0.3, 0.07, drive=1.4), 150),
                         0.7 * sa.norm(sa.bandpass(sa.noise(0.3, rng), 400, 2500)) * sa.decay(0.3, 0.07, 0.002))
        tone = sa.bell(f, min(dur, 0.8), 0.3, 2.0, 1.2)
        x = sa.mix(0.7 * body, (dur * accent, 0.7 * hit), 0.2 * tone)
        return _clean(x, "cast", 0.5, 0.12, 6500)
    return build


_ARTS = [
    ("riptide_cut", _art("tide", 0.6, sa.note(D, 0), 0.25), "cast", 24),
    ("breaker", _art("tide", 0.8, sa.note(A, 0), 0.3), "cast", 24),
    ("whirlpool", _art("tide", 1.1, sa.note(FS, 0), 0.5), "field", 32),
    ("surge", _art("tide", 0.7, sa.note(E, 0), 0.2), "cast", 24),
    ("maelstrom", _art("tide", 1.4, sa.note(D, -1), 0.7), "field", 40),
    ("sunder_cut", _art("iron", 0.6, sa.note(D, 1), 0.25), "cast", 24),
    ("anvil_fall", _art("iron", 0.9, sa.note(A, 0), 0.6, 120), "hit", 32),
    ("bulwark", _art("iron", 0.8, sa.note(FS, 0), 0.2), "cast", 24),
    ("forge_charge", _art("iron", 0.7, sa.note(E, 1), 0.3), "cast", 24),
    ("worldforge", _art("iron", 1.4, sa.note(D, 0), 0.7, 160), "field", 40),
    ("grit_flick", _art("dune", 0.5, sa.note(A, -1), 0.2), "cast", 24),
    ("quicksand", _art("dune", 1.0, sa.note(D, -1), 0.4), "field", 32),
    ("sandveil", _art("dune", 0.9, sa.note(FS, -1), 0.3), "cast", 24),
    ("dune_runner", _art("dune", 0.7, sa.note(E, -1), 0.25), "cast", 24),
    ("sea_of_sand", _art("dune", 1.4, sa.note(D, -1), 0.7), "field", 40),
]


# ================================================================ awakenings and finishers

def awaken_tide(v, rng):
    return tide_art(v, rng)


def awaken_iron(v, rng):
    return iron_art(v + 1, rng)


def awaken_dune(v, rng):
    return dune_art(v, rng)


def finisher_tide(v, rng):
    dur = 1.0
    crash = sa.norm(sa.bandpass(sa.noise(0.6, rng), 400, 6000)) * sa.decay(0.6, 0.18, 0.002)
    x = sa.mix(0.8 * crash, 0.6 * _water(dur, rng, 150, 1800, [(0, 1), (dur, 0)]), 0.4 * sa.highpass(sa.thump(90, 40, 0.5, 0.1), 50))
    return _clean(x, "impact", 0.6, 0.16, 6000)


def finisher_iron(v, rng):
    x = sa.mix(_clang(sa.note(D, 0), 1.2, 0.35, rng, 140), 0.5 * sa.highpass(sa.thump(120, 50, 0.4, 0.08, drive=2.2), 70))
    return _clean(x, "impact", 0.6, 0.16, 7000)


def finisher_dune(v, rng):
    dur = 1.1
    spin = _whoosh(dur, [(0, 400), (0.3, 2400), (0.6, 900), (dur, 2000)], 1.0, rng, peak=0.5)
    x = sa.mix(0.6 * sa.norm(spin), 0.6 * _grit(dur, rng, 300), 0.4 * sa.highpass(sa.thump(100, 42, 0.4, 0.08), 50))
    return _clean(x, "impact", 0.6, 0.16, 6000)


METHODS_A_EVENTS = []
EVENTS = METHODS_A_EVENTS
for _name, (_swing, _impact, _art_fn) in {"tide": (tide_swing, tide_impact, tide_art), "iron": (iron_swing, iron_impact, iron_art),
                                         "dune": (dune_swing, dune_impact, dune_art)}.items():
    EVENTS.append(event(f"aura_{_name}_swing", _swing, variants=3, role="effect", subtitle="cast"))
    EVENTS.append(event(f"aura_{_name}_impact", _impact, variants=3, role="impact", subtitle="hit"))
    EVENTS.append(event(f"aura_{_name}_art", _art_fn, variants=1, role="cast", subtitle="cast", attenuation=24))
EVENTS += [event(f"aura_art_{name}", build, variants=1, role="cast", subtitle=subtitle, attenuation=reach) for name, build, subtitle, reach in _ARTS]
EVENTS += [
    event("aura_awaken_tide", awaken_tide, role="effect", subtitle="cast", attenuation=40),
    event("aura_awaken_iron", awaken_iron, role="effect", subtitle="cast", attenuation=40),
    event("aura_awaken_dune", awaken_dune, role="effect", subtitle="cast", attenuation=40),
    event("aura_finisher_tide", finisher_tide, role="impact", subtitle="hit", attenuation=32),
    event("aura_finisher_iron", finisher_iron, role="impact", subtitle="hit", attenuation=32),
    event("aura_finisher_dune", finisher_dune, role="impact", subtitle="hit", attenuation=32),
]
