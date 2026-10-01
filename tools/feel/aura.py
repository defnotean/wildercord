"""Aura, the swordsman's path: a blade's own sounds, belonging to no element (each method's element is in what it strikes).

    aura_slash          a crescent of aura loosed off the blade: a ringing edge tearing through the air
    aura_guard          the guard braced: a low metallic hum settling round the blade
    aura_perfect_guard  the perfect moment: a bright ringing clash and a chime above it
    aura_breakthrough   a stage broken through: breath drawn in, then a rising chord blooming out
    aura_backlash       spent past empty: a dull falling exhale and a dry crackle (nothing hurts)
    aura_breath         the breathing stance: a soft breath with a hum under it (played at the pentatonic steps for the beat)

Tonal sounds are tuned to D so the pentatonic ratios make a scale from one sample.
"""
import numpy as np

from feel.core import sa, event

D, E, FS, A, B = sa.D, sa.E, sa.FS, sa.A, sa.B


def _clean(x, role, verb=0.35, wet=0.1, damp=7000.0):
    return sa.finish(sa.highpass(sa.reverb(x, verb, wet, damp=damp), 70), role)


def aura_slash(v, rng):
    """A crescent loosed: a bright edge sliding down through the air, ringing as it goes."""
    dur = 0.55
    air = sa.moving_band(dur, [(0, 6500), (0.12, 2600), (dur, 900)], 0.55, rng) * sa.env(dur, (0, 0), (0.02, 1), (0.25, 0.45), (dur, 0))
    ring = sa.sine(sa.sweep(sa.note(A, 2), sa.note(D, 2), dur, 0.7), dur) * sa.decay(dur, 0.16, 0.004)
    edge = sa.glass(sa.note(D, 3) * (1.0 + 0.003 * v), dur, 0.09)
    whump = sa.thump(180, 90, 0.18, 0.04, drive=1.3)
    x = sa.mix(0.85 * sa.norm(air), 0.32 * ring, 0.22 * edge, 0.35 * whump)
    return _clean(sa.chorus(x, voices=2, depth=0.002, rate=0.6), "cast", 0.4, 0.12, 8000)


def aura_guard(v, rng):
    """The guard braced: a soft metallic swell, settling into a low hum round the blade."""
    dur = 0.6
    hum = (sa.sine(sa.note(D, 0), dur) + 0.5 * sa.sine(sa.note(A, 0), dur) + 0.25 * sa.sine(sa.note(D, 1), dur)) * sa.swell(dur, 0.12)
    metal = sa.partials(sa.note(D, 1), dur, ((1.0, 1.0, 1.0), (2.76, 0.4, 0.5), (5.4, 0.2, 0.3)), 0.12)
    brace = sa.moving_band(0.25, [(0, 1800), (0.25, 600)], 0.8, rng) * sa.decay(0.25, 0.06, 0.004)
    return _clean(sa.mix(0.55 * hum, 0.35 * metal, 0.25 * brace), "effect", 0.4, 0.1)


def aura_perfect_guard(v, rng):
    """The perfect moment: a hard bright clash of blade on blade, and a chime ringing out over it."""
    dur = 1.0
    clash = sa.norm(sa.bandpass(sa.noise(0.08, rng), 1800, 7500)) * sa.decay(0.08, 0.012, 0.0005)
    strike = sa.partials(sa.note(A, 1), dur, ((1.0, 1.0, 1.0), (2.41, 0.7, 0.6), (3.9, 0.45, 0.4), (6.2, 0.25, 0.25)), 0.2)
    chime = sa.mix(sa.glass(sa.note(D, 3), dur, 0.3), (0.05, 0.7 * sa.glass(sa.note(A, 3), dur * 0.9, 0.28)))
    return _clean(sa.mix(0.7 * clash, 0.45 * strike, 0.4 * chime), "impact", 0.6, 0.15, 9000)


def aura_breakthrough(v, rng):
    """A stage broken through: a breath drawn in, a held moment, then a chord blooming out and up."""
    dur = 2.2
    inhale = sa.moving_band(0.7, [(0, 500), (0.7, 2600)], 0.7, rng) * sa.swell(0.7, 0.68)
    root = sa.sine(sa.note(D, 0), dur) * sa.env(dur, (0, 0), (0.75, 0), (0.8, 1), (dur, 0))
    chord = sa.mix((0.78, sa.bell(sa.note(D, 1), 1.4, 0.6, 2.0, 1.1)), (0.84, 0.8 * sa.bell(sa.note(FS, 1), 1.3, 0.55, 2.0, 1.1)),
                   (0.9, 0.7 * sa.bell(sa.note(A, 1), 1.25, 0.5, 2.0, 1.1)), (1.0, 0.6 * sa.glass(sa.note(D, 3), 1.1, 0.4)))
    rise = sa.sparkle(1.3, 26, rng, [sa.note(d, 2) for d in (D, E, FS, A, B)], tau=(0.05, 0.14), shape=[(0, 0.2), (1.3, 1)], rising=True)
    whoosh = sa.moving_band(1.0, [(0, 600), (0.4, 3500), (1.0, 1500)], 0.6, rng) * sa.decay(1.0, 0.3, 0.02)
    x = sa.mix(0.45 * sa.norm(inhale), 0.3 * root, 0.55 * chord, (0.8, 0.3 * sa.norm(rise)), (0.78, 0.4 * whoosh))
    return _clean(x, "grand", 1.2, 0.22, 8000)


def aura_backlash(v, rng):
    """Spent past empty: a dull falling exhale, a dry crackle, a low note sinking (it never hurts)."""
    dur = 0.7
    exhale = sa.moving_band(dur, [(0, 1600), (dur, 300)], 0.9, rng) * sa.decay(dur, 0.25, 0.01)
    fall = sa.decay(0.35, 0.12, 0.002)
    crackle = sa.grains(0.35, 60, rng)[: len(fall)] * fall
    sink = sa.sine(sa.sweep(sa.note(A, 0), sa.note(D, -1), dur, 0.6), dur) * sa.decay(dur, 0.25, 0.01)
    return _clean(sa.mix(0.6 * sa.norm(exhale), 0.3 * sa.norm(crackle), 0.45 * sink), "effect", 0.3, 0.08, 4000)


def aura_breath(v, rng):
    """A breath of the stance: air drawn in softly, a warm hum under it at D. Played at the scale's steps for the beat."""
    dur = 1.0
    air = sa.moving_band(dur, [(0, 700), (0.45, 1700), (dur, 900)], 0.5, rng) * sa.env(dur, (0, 0), (0.35, 1), (dur, 0))
    hum = (sa.sine(sa.note(D, 1), dur) + 0.4 * sa.sine(sa.note(A, 1), dur)) * sa.env(dur, (0, 0), (0.4, 1), (dur, 0))
    glint = 0.2 * sa.glass(sa.note(D, 3), dur, 0.25)
    return _clean(sa.mix(0.55 * sa.norm(air), 0.4 * hum, (0.3, glint)), "ui", 0.5, 0.12, 6500)


EVENTS = [
    event("aura_slash", aura_slash, variants=2, role="cast", subtitle="cast"),
    event("aura_guard", aura_guard, role="effect", subtitle="tell"),
    event("aura_perfect_guard", aura_perfect_guard, role="impact", subtitle="hit", attenuation=24),
    event("aura_breakthrough", aura_breakthrough, role="grand", subtitle="field", attenuation=32),
    event("aura_backlash", aura_backlash, role="effect", subtitle="tell"),
    event("aura_breath", aura_breath, role="ui", subtitle="tell"),
]
