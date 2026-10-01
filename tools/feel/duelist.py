"""The world of aura: the wandering duelist's blade and manners, the clash of two slashes, and the fallen knight's armour.

    duelist_challenge      a blade drawn from its scabbard: steel sliding free, and a clear ring
    duelist_sheathe        the blade slid home: a falling slide and a soft click
    duelist_bow            cloth moving as it bows, a quiet chime
    duelist_yield          the blade's point set down on the ground, and a long breath out
    duelist_clash          two crescents of aura meeting in the air: a hard bright clash, a burst of wind, a chord hanging over it
    duelist_knight_ambient a hollow suit of armour shifting: a creak of old plate, a low hum inside it
    duelist_knight_hurt    struck plate, ringing hollow
    duelist_knight_death   the suit falling apart: plates clattering down, the aura in it guttering out
    duelist_knight_windup  its tell: a hum rising under the raised blade, a shimmer of steel on top
    duelist_knight_step    a heavy armoured step

Tonal sounds are tuned to D so they sit with aura's own (tools/feel/aura.py) and the rest of the mod.
"""
import numpy as np

from feel.core import sa, event

D, E, FS, A, B = sa.D, sa.E, sa.FS, sa.A, sa.B


def _clean(x, role, verb=0.35, wet=0.1, damp=7000.0):
    return sa.finish(sa.highpass(sa.reverb(x, verb, wet, damp=damp), 70), role)


def _clank(rng, freq, tau=0.12, bright=1.0):
    """A struck piece of plate: an inharmonic ring over a short knock."""
    ring = sa.partials(freq, 0.6, ((1.0, 1.0, 1.0), (2.32, 0.7 * bright, 0.6), (3.71, 0.45 * bright, 0.45), (5.13, 0.25 * bright, 0.3)), tau)
    knock = sa.norm(sa.bandpass(sa.noise(0.05, rng), 900, 5200)) * sa.decay(0.05, 0.008, 0.0005)
    return sa.mix(0.6 * ring, 0.5 * knock)


def duelist_challenge(v, rng):
    """Steel sliding out of a scabbard, rising, then ringing clear."""
    dur = 0.9
    slide = sa.moving_band(0.42, [(0, 2200), (0.42, 6200)], 0.35, rng) * sa.env(0.42, (0, 0), (0.05, 0.6), (0.38, 1), (0.42, 0))
    ring = sa.mix(sa.glass(sa.note(A, 2), dur, 0.32), 0.5 * sa.glass(sa.note(D, 3), dur * 0.9, 0.25))
    x = sa.mix(0.7 * sa.norm(slide), (0.4, 0.45 * ring))
    return _clean(x, "effect", 0.5, 0.12, 9000)


def duelist_sheathe(v, rng):
    """The blade slid home, falling in pitch, and the click of the collar."""
    dur = 0.55
    slide = sa.moving_band(0.38, [(0, 5200), (0.38, 1800)], 0.35, rng) * sa.env(0.38, (0, 0), (0.04, 1), (0.34, 0.5), (0.38, 0))
    click = _clank(rng, sa.note(D, 2), 0.04, 0.6)
    return _clean(sa.mix(0.7 * sa.norm(slide), (0.38, 0.6 * click)), "effect", 0.25, 0.08)


def duelist_bow(v, rng):
    """Cloth moving as it bows, and one quiet chime."""
    dur = 1.0
    cloth = sa.moving_band(0.6, [(0, 900), (0.3, 1500), (0.6, 700)], 0.9, rng) * sa.env(0.6, (0, 0), (0.25, 1), (0.6, 0))
    chime = sa.glass(sa.note(FS, 2), dur * 0.8, 0.3)
    return _clean(sa.mix(0.5 * sa.norm(cloth), (0.3, 0.3 * chime)), "ui", 0.5, 0.12)


def duelist_yield(v, rng):
    """The point set down on the ground (a dull ring), and a long breath out."""
    dur = 1.2
    down = _clank(rng, sa.note(D, 1), 0.18, 0.5)
    breath = sa.moving_band(0.9, [(0, 1400), (0.9, 500)], 0.8, rng) * sa.env(0.9, (0, 0), (0.15, 1), (0.9, 0))
    return _clean(sa.mix(0.6 * down, (0.2, 0.55 * sa.norm(breath))), "effect", 0.5, 0.12, 5000)


def duelist_clash(v, rng):
    """Two slashes meeting: a hard clash of light on light, a burst of air thrown out, a chord hanging after."""
    dur = 1.4
    clash = sa.norm(sa.bandpass(sa.noise(0.1, rng), 1500, 8000)) * sa.decay(0.1, 0.014, 0.0005)
    strike = _clank(rng, sa.note(A, 1), 0.25, 1.2)
    burst = sa.moving_band(0.8, [(0, 4500), (0.15, 1800), (0.8, 600)], 0.8, rng) * sa.decay(0.8, 0.22, 0.004)
    chord = sa.mix(sa.bell(sa.note(D, 1), 1.2, 0.5, 2.0, 1.2), 0.7 * sa.bell(sa.note(A, 1), 1.1, 0.45, 2.0, 1.2),
                   (0.05, 0.5 * sa.glass(sa.note(D, 3), 1.0, 0.35)))
    whump = sa.thump(160, 70, 0.3, 0.07, drive=1.4)
    x = sa.mix(0.75 * clash, 0.5 * strike, 0.6 * sa.norm(burst), (0.03, 0.4 * chord), 0.5 * whump)
    return _clean(x, "impact", 0.8, 0.18, 8500)


def duelist_knight_ambient(v, rng):
    """Old plate shifting on nothing: a creak, and a low hum inside the hollow."""
    dur = 1.3
    creak = sa.moving_band(0.5, [(0, 600 + 120 * v), (0.5, 900)], 0.25, rng) * sa.chopper(0.5, rng, rate=(18, 40), floor=0.2) \
        * sa.env(0.5, (0, 0), (0.1, 1), (0.5, 0))
    hum = (sa.sine(sa.note(D, -1), dur) + 0.5 * sa.sine(sa.note(A, -1), dur) + 0.6 * sa.sine(sa.note(D, 0), dur)) \
        * sa.env(dur, (0, 0), (0.4, 1), (dur, 0))
    tap = _clank(rng, sa.note(E, 1) * (1 + 0.02 * v), 0.08, 0.5)
    return _clean(sa.mix(0.7 * sa.norm(creak), 0.25 * hum, (0.35, 0.5 * tap)), "effect", 0.6, 0.18, 4500)


def duelist_knight_hurt(v, rng):
    """Struck plate, ringing hollow."""
    hit = _clank(rng, sa.note(B, 0) * (1 + 0.03 * v), 0.22, 1.0)
    thud = sa.thump(140, 70, 0.25, 0.05, drive=1.3)
    return _clean(sa.mix(0.8 * hit, 0.4 * thud), "impact", 0.5, 0.12, 6000)


def duelist_knight_death(v, rng):
    """The suit falls apart: plates clattering down one after another, the aura in it guttering out."""
    dur = 1.8
    parts = [(0.0, 0.7 * _clank(rng, sa.note(A, 0), 0.18))]
    t = 0.12
    for i in range(6):
        parts.append((t, (0.6 - 0.07 * i) * _clank(rng, sa.note((D, E, FS, A, B)[i % 5], 1) * rng.uniform(0.95, 1.05), 0.12)))
        t += rng.uniform(0.08, 0.16)
    gutter = sa.sine(sa.sweep(sa.note(A, 0), sa.note(D, -1), dur, 0.6), dur) * sa.decay(dur, 0.5, 0.05)
    return _clean(sa.mix(*parts, (0.1, 0.4 * gutter)), "effect", 0.7, 0.16, 5000)


def duelist_knight_windup(v, rng):
    """The tell: a hum rising under the raised blade, a shimmer of steel above it."""
    dur = 1.1
    hum = sa.sine(sa.sweep(sa.note(D, 0), sa.note(A, 0), dur, 0.8), dur) * sa.swell(dur, 0.85)
    edge = sa.moving_band(dur, [(0, 2500), (dur, 6500)], 0.25, rng) * sa.swell(dur, 0.9)
    shimmer = sa.sparkle(dur, 14, rng, [sa.note(d, 2) for d in (D, FS, A)], tau=(0.04, 0.1), shape=[(0, 0.2), (dur, 1)])
    return _clean(sa.mix(0.55 * hum, 0.35 * sa.norm(edge), 0.3 * sa.norm(shimmer)), "cast", 0.45, 0.12, 8000)


def duelist_knight_step(v, rng):
    """A heavy armoured step."""
    thud = sa.thump(110, 60, 0.18, 0.04, drive=1.2)
    clink = _clank(rng, sa.note((D, E, A)[v % 3], 2), 0.05, 0.6)
    return _clean(sa.mix(0.6 * thud, 0.4 * clink), "pulse", 0.2, 0.06, 5000)


EVENTS = [
    event("duelist_challenge", duelist_challenge, role="effect", subtitle="duelist.challenge"),
    event("duelist_sheathe", duelist_sheathe, role="effect", subtitle="duelist.sheathe"),
    event("duelist_bow", duelist_bow, role="ui", subtitle="duelist.bow"),
    event("duelist_yield", duelist_yield, role="effect", subtitle="duelist.yield"),
    event("duelist_clash", duelist_clash, role="impact", subtitle="duelist.clash", attenuation=32),
    event("duelist_knight_ambient", duelist_knight_ambient, variants=2, role="effect", subtitle="fallen_knight.ambient"),
    event("duelist_knight_hurt", duelist_knight_hurt, variants=2, role="impact", subtitle="fallen_knight.hurt"),
    event("duelist_knight_death", duelist_knight_death, role="effect", subtitle="fallen_knight.death"),
    event("duelist_knight_windup", duelist_knight_windup, role="cast", subtitle="fallen_knight.windup", attenuation=24),
    event("duelist_knight_step", duelist_knight_step, variants=3, role="pulse", subtitle="fallen_knight.step"),
]
