"""Techniques of one's own: the writing page, a part learned, a rank reached, and each release's voice as a technique is struck (the method's
own technique sound plays under every one, as for the arts). Not a part of its own: tools/feel/aura.py adds these to its events (they live
in the aura folder).

    aura_technique_write              a technique written: a brush drawn across paper, a seal pressed into wax, a soft bell
    aura_technique_learn              a part learned from a scroll: paper unrolled, a few notes rising
    aura_technique_rank               a rank reached: a blade drawn along a whetstone, ringing, and a chime stepping up
    aura_technique_on_the_blade       a technique on the blade: a short bright edge, cut clean
    aura_technique_wave               a wave thrown: the air torn and carried away, a tone riding it out
    aura_technique_burst              a burst: a blow at the heart and a ring breaking out round it
    aura_technique_afterimage         an afterimage left: a breath drawn in, a hollow shimmer that stays
    aura_technique_afterimage_strike  the afterimage striking where nobody stands now: a hollow cut, echoing
    aura_technique_echo               a technique's echo: a ring struck twice, the second fainter and higher
    aura_technique_sunder             a stance sundered: a crack splitting, a dull break
    aura_technique_ward               a ward: a shell humming up round its swordsman
    aura_technique_rally              a rally: a short call stepping up, a drum and a bright chord

Everything tonal is in D pentatonic like the rest of the mod.
"""
from feel.core import sa, event

D, E, FS, A, B = sa.D, sa.E, sa.FS, sa.A, sa.B


def _clean(x, role, verb=0.6, wet=0.14, damp=7500.0):
    return sa.finish(sa.highpass(sa.reverb(x, verb, wet, damp=damp), 50), role)


def _echo(x, gap, decay, count):
    """A dry copy and fading repeats of it, {@code gap} seconds apart."""
    return sa.mix(x, *[(gap * (k + 1), (decay ** (k + 1)) * x) for k in range(count)])


# ================================================================ the page

def aura_technique_write(v, rng):
    """A technique written: a brush drawn across paper (a soft rasp rising and falling), a seal pressed into wax, a bell after."""
    dur = 1.6
    brush = sa.moving_band(0.55, [(0, 1800), (0.25, 4200), (0.55, 2200)], 0.45, rng) * sa.env(0.55, (0, 0), (0.08, 1), (0.4, 0.7), (0.55, 0))
    press = sa.thump(150, 80, 0.18, 0.05, drive=1.2, knock=0.2)
    bell = sa.bell(sa.note(D, 2), 1.0, 0.8, 2.0, 1.2, attack=0.01)
    fifth = sa.bell(sa.note(A, 2), 0.8, 0.7, 2.0, 1.1, attack=0.01)
    x = sa.mix(0.45 * sa.norm(sa.lowpass(brush, 6000)), (0.52, 0.4 * sa.highpass(press, 70)), (0.58, 0.35 * sa.norm(bell)), (0.66, 0.2 * sa.norm(fifth)))
    return _clean(x, "ui", 0.6, 0.14, 8500)


def aura_technique_learn(v, rng):
    """A part learned: paper unrolled with a soft crackle, three notes rising after it."""
    dur = 1.6
    paper = sa.norm(sa.bandpass(sa.noise(0.5, rng), 1200, 6000)) * sa.env(0.5, (0, 0), (0.05, 1), (0.25, 0.5), (0.5, 0))
    crackle = sa.shatter(rng, count=10, band=(2500, 8000), spread=0.35, tau=(0.004, 0.02))
    notes = sa.mix(*[(0.35 + 0.16 * k, (0.75 - 0.1 * k) * sa.bell(sa.note(n, o), 0.9, 0.8, 2.0, 1.2, attack=0.01))
                     for k, (n, o) in enumerate(((D, 2), (FS, 2), (A, 2)))])
    x = sa.mix(0.35 * paper, 0.15 * sa.norm(crackle), 0.45 * sa.norm(notes))
    return _clean(x, "effect", 0.7, 0.16, 8000)


def aura_technique_rank(v, rng):
    """A rank reached: a blade drawn along a whetstone (a long bright hiss), the steel ringing after it, a chime stepping up."""
    dur = 2.0
    hone = sa.moving_band(0.7, [(0, 3000), (0.5, 7500), (0.7, 5000)], 0.35, rng) * sa.env(0.7, (0, 0), (0.1, 1), (0.6, 0.8), (0.7, 0))
    ring = sa.partials(sa.note(D, 1), 1.5, ((1.0, 1.0, 1.0), (2.41, 0.6, 0.55), (3.9, 0.35, 0.35), (5.6, 0.2, 0.25)), 0.6)
    chime = sa.mix(sa.bell(sa.note(A, 2), 1.0, 0.8, 2.0, 1.2, attack=0.01), (0.14, 0.8 * sa.bell(sa.note(D, 3), 1.0, 0.8, 2.0, 1.2, attack=0.01)))
    x = sa.mix(0.35 * sa.norm(hone), (0.6, 0.4 * ring), (0.75, 0.35 * sa.norm(chime)))
    return _clean(x, "effect", 0.9, 0.18, 8500)


# ================================================================ each release's voice

def aura_technique_on_the_blade(v, rng):
    """On the blade: a short bright edge, the air cut clean."""
    dur = 0.7
    cut = sa.moving_band(0.3, [(0, 6500), (0.08, 3000), (0.3, 1200)], 0.5, rng) * sa.env(0.3, (0, 0), (0.015, 1), (0.3, 0))
    edge = sa.glass(sa.note(A, 3) * (1 + 0.006 * v), 0.5, 0.12)
    x = sa.mix(0.7 * sa.norm(cut), (0.02, 0.3 * edge))
    return _clean(x, "cast", 0.5, 0.12, 9000)


def aura_technique_wave(v, rng):
    """A wave thrown: the air torn and carried away, a tone riding it out and falling as it goes."""
    dur = 1.2
    tear = sa.moving_band(0.9, [(0, 900), (0.15, 5000), (0.9, 1400)], 0.55, rng) * sa.env(0.9, (0, 0), (0.05, 1), (0.9, 0))
    tone = sa.sine(sa.glide(0.9, (0, sa.note(A, 2)), (0.9, sa.note(D, 2))), 0.9) * sa.env(0.9, (0, 0), (0.06, 1), (0.9, 0))
    x = sa.mix(0.65 * sa.norm(tear), (0.04, 0.25 * tone))
    return _clean(x, "cast", 0.6, 0.14, 7500)


def aura_technique_burst(v, rng):
    """A burst: a blow at the heart and a ring of air breaking out round it."""
    dur = 1.2
    blow = sa.thump(90, 40, 0.5, 0.12, drive=2.0, knock=0.5)
    ring = sa.moving_band(0.6, [(0, 600), (0.1, 3800), (0.6, 900)], 0.6, rng) * sa.decay(0.6, 0.18, 0.002)
    shimmer = sa.sparkle(0.7, 14, rng, [sa.note(d, 3) for d in (D, FS, A)], tau=(0.03, 0.09), shape=[(0, 1), (0.7, 0)])
    x = sa.mix(0.6 * sa.highpass(blow, 40), (0.02, 0.45 * sa.norm(ring)), (0.08, 0.15 * sa.norm(shimmer)))
    return _clean(x, "impact", 0.7, 0.16, 7500)


def aura_technique_afterimage(v, rng):
    """An afterimage left: a breath drawn in backwards, a hollow shimmer staying where it was."""
    dur = 1.1
    breath = sa.reverse(sa.moving_band(0.5, [(0, 5000), (0.5, 1100)], 0.6, rng) * sa.decay(0.5, 0.15, 0.004))
    hollow = (sa.sine(sa.note(FS, 1), 0.8) + 0.4 * sa.sine(sa.note(B, 1) * 1.004, 0.8)) * sa.env(0.8, (0, 0), (0.1, 1), (0.8, 0))
    x = sa.mix(0.5 * sa.norm(breath), (0.42, 0.3 * hollow))
    return _clean(x, "cast", 0.9, 0.2, 6500)


def aura_technique_afterimage_strike(v, rng):
    """The afterimage striking: a hollow cut, echoing where nobody stands now (a little lower than the Shadowstep's own)."""
    dur = 1.0
    cut = sa.moving_band(0.3, [(0, 4200), (0.1, 1800), (0.3, 700)], 0.55, rng) * sa.env(0.3, (0, 0), (0.02, 1), (0.3, 0))
    ring = sa.glass(sa.note(D, 2) * (1 + 0.004 * v), 0.45, 0.12)
    x = _echo(sa.mix(0.7 * sa.norm(cut), 0.25 * ring), 0.09, 0.4, 3)
    return _clean(x, "effect", 0.8, 0.2, 6500)


def aura_technique_echo(v, rng):
    """An echo: a ring struck twice, the second fainter and a step higher."""
    dur = 1.1
    first = sa.mix(sa.glass(sa.note(D, 3), 0.6, 0.14), 0.5 * sa.norm(sa.moving_band(0.15, [(0, 5000), (0.15, 2000)], 0.5, rng) * sa.decay(0.15, 0.04, 0.002)))
    second = sa.glass(sa.note(E, 3), 0.6, 0.14)
    x = sa.mix(0.55 * sa.norm(first), (0.16, 0.3 * second), (0.3, 0.15 * second))
    return _clean(x, "effect", 0.8, 0.2, 8000)


# ================================================================ what the intents do

def aura_technique_sunder(v, rng):
    """A stance sundered: a crack splitting through, and a dull break under it."""
    dur = 0.8
    crack = sa.shatter(rng, count=16, band=(1800, 7500), spread=0.18, tau=(0.01, 0.05))
    brk = sa.thump(110, 55, 0.3, 0.07, drive=1.8, knock=0.6)
    x = sa.mix(0.5 * sa.norm(crack), 0.45 * sa.highpass(brk, 60))
    return _clean(x, "impact", 0.5, 0.12, 8000)


def aura_technique_ward(v, rng):
    """A ward: a glassy shell humming up round its swordsman and holding."""
    dur = 1.4
    hum = (sa.sine(sa.note(D, 1), 1.2) + 0.5 * sa.sine(sa.note(A, 1), 1.2) + 0.25 * sa.sine(sa.note(D, 2) * 1.003, 1.2))
    hum = hum * sa.env(1.2, (0, 0), (0.25, 1), (0.9, 0.7), (1.2, 0))
    glass = sa.glass(sa.note(FS, 3), 1.0, 0.3)
    x = sa.mix(0.45 * sa.norm(hum), (0.18, 0.25 * glass))
    return _clean(x, "effect", 0.9, 0.18, 7000)


def aura_technique_rally(v, rng):
    """A rally: a short call stepping up a fifth, a drum under it, a bright chord after."""
    dur = 1.3
    call = sa.lowpass(sa.soft_saw(sa.glide(0.7, (0, sa.note(A, 0)), (0.1, sa.note(E, 1)), (0.7, sa.note(E, 1))), harmonics=7), 2400)
    call = call * sa.env(0.7, (0, 0), (0.05, 1), (0.5, 0.7), (0.7, 0))
    drum = sa.thump(85, 42, 0.35, 0.09, drive=2.0, knock=0.35)
    chord = sa.mix(*[(0.02 * i, sa.bell(sa.note(*n), 0.8, 0.8, 2.0, 1.2, attack=0.02)) for i, n in enumerate(((A, 1), (D, 2), (FS, 2)))])
    x = sa.mix(0.45 * sa.norm(call), 0.4 * sa.highpass(drum, 40), (0.14, 0.3 * sa.norm(chord)))
    return _clean(x, "effect", 0.7, 0.16, 8000)


TECHNIQUE_EVENTS = [
    event("aura_technique_write", aura_technique_write, role="ui", subtitle="tell"),
    event("aura_technique_learn", aura_technique_learn, role="effect", subtitle="tell"),
    event("aura_technique_rank", aura_technique_rank, role="effect", subtitle="tell", attenuation=24),
    event("aura_technique_on_the_blade", aura_technique_on_the_blade, variants=2, role="cast", subtitle="cast"),
    event("aura_technique_wave", aura_technique_wave, variants=2, role="cast", subtitle="cast", attenuation=24),
    event("aura_technique_burst", aura_technique_burst, variants=2, role="impact", subtitle="cast", attenuation=24),
    event("aura_technique_afterimage", aura_technique_afterimage, role="cast", subtitle="cast"),
    event("aura_technique_afterimage_strike", aura_technique_afterimage_strike, variants=2, role="effect", subtitle="hit", attenuation=24),
    event("aura_technique_echo", aura_technique_echo, variants=2, role="effect", subtitle="hit"),
    event("aura_technique_sunder", aura_technique_sunder, variants=2, role="impact", subtitle="hit"),
    event("aura_technique_ward", aura_technique_ward, role="effect", subtitle="cast"),
    event("aura_technique_rally", aura_technique_rally, role="effect", subtitle="tell", attenuation=24),
]
