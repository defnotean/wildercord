"""The Echo, Dawn and Venom breathing methods' sounds (the methods-b pack). Not a part of its own: tools/feel/aura.py adds these
to its events, so they live in the aura folder next to the other methods' families.

    echo   a ringing whoosh with a delayed copy of itself; a bell struck twice; a chord that rings back three times
    dawn   a bright airy swish with a glint; a warm chime and a soft flare; a rising choir-like swell into a bell
    venom  a hissing slither; a wet bite with a hiss after it; a low coil-hiss that snaps shut

Each art has its own voice (aura_art_<art>), built from its method's palette. Everything tonal is in D pentatonic.
"""
import numpy as np

from feel.core import sa, event

D, E, FS, A, B = sa.D, sa.E, sa.FS, sa.A, sa.B


def _clean(x, role, verb=0.3, wet=0.1, damp=7000.0):
    return sa.finish(sa.highpass(sa.reverb(x, verb, wet, damp=damp), 60), role)


def _whoosh(dur, path, width, rng, peak=0.3):
    return sa.moving_band(dur, path, width, rng) * sa.env(dur, (0, 0), (dur * peak, 1), (dur * 0.75, 0.3), (dur, 0))


def _echoes(x, gap, count, fall=0.5):
    """`x` repeated `count` times, `gap` seconds apart, each quieter: a sound ringing back."""
    return sa.mix(*[(gap * i, x * (fall ** i)) for i in range(count)])


def _hiss(dur, rng, lo=3500, hi=9000):
    return sa.norm(sa.bandpass(sa.noise(dur, rng), lo, hi))


# ================================================================ echo: sound and resonance

def echo_swing(v, rng):
    dur = 0.36
    air = sa.norm(_whoosh(dur, [(0, 700 + 80 * v), (0.12, 2600), (dur, 1100)], 0.7, rng))
    ring = sa.glass(sa.note(A, 1), 0.3, 0.06) * 0.25
    x = _echoes(sa.mix(air, (0.08, ring)), 0.11, 2, 0.4)
    return _clean(x, "effect", 0.3, 0.1, 7000)


def echo_impact(v, rng):
    hit = sa.thump(160 - 10 * v, 70, 0.25, 0.05, drive=1.4)
    bell = sa.clock_bell(sa.note(D, 1), 0.6, 0.18)
    x = sa.mix(0.5 * sa.highpass(hit, 90), 0.6 * _echoes(bell, 0.12, 2, 0.45))
    return _clean(x, "impact", 0.35, 0.12, 6500)


def echo_art(v, rng):
    chord = sa.mix(sa.bell(sa.note(D, 0), 0.7, 0.3), 0.7 * sa.bell(sa.note(FS, 0), 0.7, 0.3), 0.6 * sa.bell(sa.note(A, 0), 0.7, 0.3))
    x = sa.mix(0.5 * sa.norm(_whoosh(0.5, [(0, 400), (0.2, 2200), (0.5, 900)], 0.8, rng)), (0.12, _echoes(chord, 0.18, 3, 0.55)))
    return _clean(x, "cast", 0.55, 0.14, 6500)


# ================================================================ dawn: light

def dawn_swing(v, rng):
    dur = 0.38
    air = sa.norm(_whoosh(dur, [(0, 1200 + 100 * v), (0.12, 3800), (dur, 1800)], 0.5, rng))
    glint = sa.glass(sa.note(B, 1), 0.25, 0.05) * 0.3
    return _clean(sa.mix(air, (0.1, glint)), "effect", 0.25, 0.08, 8000)


def dawn_impact(v, rng):
    hit = sa.thump(140 - 8 * v, 66, 0.22, 0.05, drive=1.3)
    chime = sa.mix(sa.glass(sa.note(D, 1), 0.5, 0.12), 0.5 * sa.glass(sa.note(FS, 1), 0.5, 0.1))
    flare = sa.norm(sa.bandpass(sa.noise(0.2, rng), 2000, 7000)) * sa.decay(0.2, 0.04, 0.003)
    x = sa.mix(0.45 * sa.highpass(hit, 90), 0.6 * chime, 0.35 * flare)
    return _clean(x, "impact", 0.35, 0.12, 8000)


def dawn_art(v, rng):
    dur = 1.0
    swell = sa.mix(sa.sine(sa.note(D, 0), dur), 0.6 * sa.sine(sa.note(A, 0), dur), 0.4 * sa.sine(sa.note(FS, 1), dur)) * sa.swell(dur, 0.6)
    bell = sa.bell(sa.note(D, 1), 0.7, 0.3)
    shine = sa.sparkle(0.8, 25, rng, [sa.note(n, 1) for n in (D, E, FS, A, B)], rising=True)
    x = sa.mix(0.5 * swell, (0.55, 0.7 * bell), 0.3 * sa.norm(shine))
    return _clean(x, "cast", 0.6, 0.15, 8000)


# ================================================================ venom: poison and serpent

def venom_swing(v, rng):
    dur = 0.4
    hiss = _hiss(dur, rng, 3000 + 300 * v, 9000) * sa.env(dur, (0, 0), (0.1, 1), (dur, 0))
    air = sa.norm(_whoosh(dur, [(0, 500), (0.14, 1800), (dur, 700)], 0.9, rng))
    return _clean(sa.mix(0.6 * air, 0.5 * hiss), "effect", 0.2, 0.06, 6500)


def venom_impact(v, rng):
    bite = sa.norm(sa.bandpass(sa.noise(0.08, rng), 500, 2500)) * sa.decay(0.08, 0.015, 0.001)
    hit = sa.thump(130 - 8 * v, 60, 0.22, 0.05, drive=1.6)
    hiss = _hiss(0.4, rng) * sa.env(0.4, (0, 0), (0.08, 1), (0.4, 0))
    drip = sa.mix(*[(0.1 + 0.07 * i, 0.3 * sa.glass(sa.note(E, 1) * (1.0 - 0.06 * i), 0.15, 0.03)) for i in range(3)])
    x = sa.mix(0.7 * bite, 0.45 * sa.highpass(hit, 90), (0.05, 0.4 * hiss), drip)
    return _clean(x, "impact", 0.3, 0.1, 6000)


def venom_art(v, rng):
    dur = 0.9
    coil = sa.norm(sa.bandpass(sa.brown(dur, rng), 250, 1600)) * sa.env(dur, (0, 0), (0.6, 1), (0.7, 0.2), (dur, 0))
    hiss = _hiss(dur, rng, 1800, 6000) * sa.env(dur, (0, 0), (0.6, 0.8), (0.65, 1), (dur, 0))
    snap = sa.thump(180, 60, 0.25, 0.05, drive=2.0)
    x = sa.mix(0.5 * coil, 0.45 * hiss, (0.6, 0.7 * snap))
    return _clean(x, "cast", 0.4, 0.1, 6000)


# ================================================================ the arts


def _art(base, pitch=1.0, gap=0.0, extra=None, verb=0.5):
    def build(v, rng):
        x = sa.norm(base(v, rng))
        if gap:
            x = _echoes(x, gap, 3, 0.5)
        if extra is not None:
            x = sa.mix(x, 0.5 * sa.norm(extra(v, rng)))
        if pitch != 1.0:
            idx = np.arange(0, len(x) - 1, pitch)
            x = np.interp(idx, np.arange(len(x)), x)
        return _clean(x, "cast", verb, 0.12, 7000)
    return build


def _toll(v, rng):
    return sa.clock_bell(sa.note(D, -1), 1.2, 0.5)


def _glare(v, rng):
    return sa.mix(sa.glass(sa.note(B, 1), 0.8, 0.3), sa.sparkle(0.6, 40, rng, [sa.note(n, 2) for n in (D, FS, A)]))


def _rattle(v, rng):
    return sa.bandpass(sa.grains(0.6, 120, rng, shape=[(0, 1), (0.6, 0.2)]), 2500, 8000)


ARTS = {
    "ringing_cut": _art(echo_impact, 1.0, 0.1),
    "resonant_chord": _art(echo_art, 1.0),
    "counterpoint": _art(echo_swing, 0.9, 0.14),
    "reverb_step": _art(echo_swing, 1.15, 0.08),
    "grand_resonance": _art(echo_art, 0.85, 0.2, _toll, 0.7),
    "first_light": _art(dawn_impact, 1.1),
    "sunrise_arc": _art(dawn_art, 1.0),
    "halo_guard": _art(dawn_impact, 0.85, extra=_glare),
    "dawnbreak_rush": _art(dawn_swing, 0.9, extra=_glare),
    "noon_zenith": _art(dawn_art, 0.8, extra=_glare, verb=0.7),
    "fang_strike": _art(venom_impact, 1.1),
    "spitting_cobra": _art(venom_swing, 1.0, extra=_rattle),
    "shed_skin": _art(venom_swing, 0.85),
    "serpent_slither": _art(venom_swing, 0.75, 0.1),
    "hydra_coil": _art(venom_art, 0.9, 0.15, _rattle, 0.6),
}

_FAMILIES = {
    "echo": (echo_swing, echo_impact, echo_art),
    "dawn": (dawn_swing, dawn_impact, dawn_art),
    "venom": (venom_swing, venom_impact, venom_art),
}

EVENTS = []
for _name, (_swing, _impact, _cast) in _FAMILIES.items():
    EVENTS.append(event(f"aura_{_name}_swing", _swing, variants=3, role="effect", subtitle="cast"))
    EVENTS.append(event(f"aura_{_name}_impact", _impact, variants=3, role="impact", subtitle="hit"))
    EVENTS.append(event(f"aura_{_name}_art", _cast, variants=1, role="cast", subtitle="cast", attenuation=24))
for _name, _build in ARTS.items():
    _grand = _name in ("grand_resonance", "noon_zenith", "hydra_coil")
    EVENTS.append(event(f"aura_art_{_name}", _build, variants=1, role="cast", subtitle="cast", attenuation=32 if _grand else 24))
