"""The bonded blade: the bond ceremony, a blade growing a tier, being named, taking its trait, going home, released and passed on. Not a
part of its own: tools/feel/aura.py adds these to its events (they live in the aura folder).

    aura_bond_kindle    the ceremony begins: a low hum rising out of the ground, a breath drawn in, steel starting to sing
    aura_bond_join      the ley lines run in: two glassy tones sliding together into one, the hum swelling under them
    aura_bond_seal      the seal: a deep bell struck, a chord blooming out of it, the blade ringing on, a long shimmer
    aura_bond_fail      the ceremony slipping: the hum sagging away, a muted note falling off the scale
    aura_bond_tier      a tier reached: the blade's ring, a chord stepping up a fifth, a glitter (pitched up by tier)
    aura_bond_name      a name given: a soft chime and a whisper of steel, as if it answered to it
    aura_bond_trait     a trait taken: a seal pressed home, a low chord settling, a bright edge after
    aura_bond_home      a blade going home: the air drawn away (played low where it leaves, higher where it arrives)
    aura_bond_release   a bond released: a few notes falling, a long breath let out, the ring fading to nothing
    aura_bond_pass      a blade passed on: two chords, one high and one low, braiding into one, a bell after

Everything tonal is in D pentatonic like the rest of the mod.
"""
from feel.core import sa, event

D, E, FS, A, B = sa.D, sa.E, sa.FS, sa.A, sa.B


def _clean(x, role, verb=0.6, wet=0.14, damp=7500.0):
    return sa.finish(sa.highpass(sa.reverb(x, verb, wet, damp=damp), 45), role)


def _ring(dur, note=(D, 1), tail=0.7):
    """A blade's long ring: a struck bar's inharmonic partials."""
    return sa.partials(sa.note(*note), dur, ((1.0, 1.0, 1.0), (2.41, 0.6, 0.55), (3.9, 0.35, 0.35), (5.6, 0.2, 0.25)), tail)


def aura_bond_kindle(v, rng):
    """The ceremony begins: a low hum rising out of the ground, a breath drawn in, the steel beginning to sing over it."""
    dur = 3.0
    hum = (0.5 * sa.sine(sa.note(D, -1), dur) + sa.sine(sa.note(D, 0), dur) + 0.7 * sa.sine(sa.note(A, 0), dur) + 0.4 * sa.sine(sa.note(D, 1) * 1.004, dur))
    hum = hum * sa.env(dur, (0, 0), (1.6, 0.8), (2.4, 1), (dur, 0))
    breath = sa.reverse(sa.moving_band(1.2, [(0, 4200), (1.2, 900)], 0.5, rng) * sa.decay(1.2, 0.4, 0.004))
    sing = sa.glass(sa.note(D, 3), 1.6, 0.6) * sa.env(1.6, (0, 0), (1.0, 1), (1.6, 0.4))
    x = sa.mix(0.45 * sa.norm(hum), (0.2, 0.32 * sa.norm(breath)), (1.2, 0.22 * sing))
    return _clean(x, "effect", 0.9, 0.18, 6500)


def aura_bond_join(v, rng):
    """The ley lines run in: two glassy tones sliding toward each other and meeting in one, the hum swelling under them."""
    dur = 2.2
    upper = sa.sine(sa.glide(1.4, (0, sa.note(B, 2)), (1.2, sa.note(A, 2)), (1.4, sa.note(A, 2))), 1.4) * sa.env(1.4, (0, 0), (0.2, 1), (1.4, 0.6))
    lower = sa.sine(sa.glide(1.4, (0, sa.note(FS, 2)), (1.2, sa.note(A, 2) * 0.998), (1.4, sa.note(A, 2) * 0.998)), 1.4) * sa.env(1.4, (0, 0), (0.2, 1),
                                                                                                                                (1.4, 0.6))
    met = sa.bell(sa.note(A, 2), 1.2, 0.85, 2.0, 1.2, attack=0.02)
    swell = (sa.sine(sa.note(D, 0), dur) + 0.5 * sa.sine(sa.note(A, 0), dur)) * sa.env(dur, (0, 0), (1.2, 1), (dur, 0))
    x = sa.mix(0.3 * upper, 0.3 * lower, (1.2, 0.4 * sa.norm(met)), 0.3 * sa.norm(swell))
    return _clean(x, "effect", 0.9, 0.2, 7000)


def aura_bond_seal(v, rng):
    """The seal: a deep bell struck, a chord blooming out of it, the blade ringing on, a long shimmer over everything."""
    dur = 3.4
    strike = sa.thump(70, 28, 1.1, 0.26, drive=2.2, knock=0.45)
    bell = sa.partials(sa.note(D, 0), 2.8, ((1.0, 1.0, 1.0), (2.0, 0.6, 0.7), (2.76, 0.5, 0.55), (4.1, 0.3, 0.35), (5.4, 0.18, 0.25)), 1.4)
    chord = sa.mix(*[(0.04 * i, (1 - 0.12 * i) * sa.bell(sa.note(*n), 2.4, 0.85, 2.0, 1.4, attack=0.05))
                     for i, n in enumerate(((D, 1), (A, 1), (D, 2), (FS, 2)))])
    ring = _ring(2.4, (D, 2), 1.0)
    glitter = sa.sparkle(2.2, 36, rng, [sa.note(d, 3) for d in (D, E, FS, A, B)], tau=(0.05, 0.16), shape=[(0, 0.4), (0.4, 1), (2.2, 0.05)])
    x = sa.mix(0.55 * sa.highpass(strike, 30), (0.01, 0.45 * sa.norm(bell)), (0.08, 0.45 * sa.norm(sa.chorus(chord, voices=2))),
               (0.15, 0.22 * ring), (0.3, 0.2 * sa.norm(glitter)))
    return _clean(x, "grand", 1.2, 0.22, 8000)


def aura_bond_fail(v, rng):
    """The ceremony slipping: the hum sagging away, and a muted note falling off the scale."""
    dur = 1.3
    sag = sa.sine(sa.glide(1.0, (0, sa.note(D, 0)), (1.0, sa.note(D, 0) * 0.84)), 1.0) * sa.env(1.0, (0, 1), (1.0, 0))
    fall = sa.sine(sa.sweep(sa.note(A, 1), sa.note(D, 1) * 0.94, 0.6, 0.6), 0.6) * sa.decay(0.6, 0.2, 0.004)
    dull = sa.thump(160, 90, 0.2, 0.05, drive=1.2, knock=0.2)
    x = sa.mix(0.35 * sag, (0.05, 0.4 * fall), 0.2 * sa.highpass(dull, 80))
    return _clean(x, "ui", 0.6, 0.14, 6000)


def aura_bond_tier(v, rng):
    """A tier reached: the blade's ring, a chord stepping up a fifth over it, a glitter climbing after (played higher by tier)."""
    dur = 2.4
    ring = _ring(2.0, (D, 1), 0.8)
    first = sa.mix(*[(0.02 * i, sa.bell(sa.note(*n), 1.4, 0.85, 2.0, 1.3, attack=0.03)) for i, n in enumerate(((D, 2), (FS, 2)))])
    second = sa.mix(*[(0.02 * i, sa.bell(sa.note(*n), 1.6, 0.85, 2.0, 1.3, attack=0.03)) for i, n in enumerate(((A, 2), (D, 3)))])
    glitter = sa.sparkle(1.4, 24, rng, [sa.note(d, 3) for d in (FS, A, B)], tau=(0.04, 0.12), shape=[(0, 0.3), (0.5, 1), (1.4, 0.05)])
    x = sa.mix(0.35 * ring, (0.05, 0.4 * sa.norm(first)), (0.32, 0.45 * sa.norm(second)), (0.5, 0.2 * sa.norm(glitter)))
    return _clean(x, "effect", 1.0, 0.2, 8000)


def aura_bond_name(v, rng):
    """A name given: a soft chime, and a whisper of steel after it, as if it answered to the name."""
    dur = 1.6
    chime = sa.mix(sa.bell(sa.note(D, 3), 1.0, 0.8, 2.0, 1.2, attack=0.01), (0.12, 0.6 * sa.bell(sa.note(A, 2), 1.0, 0.8, 2.0, 1.2, attack=0.01)))
    whisper = sa.moving_band(0.8, [(0, 6500), (0.5, 3200), (0.8, 5200)], 0.3, rng) * sa.env(0.8, (0, 0), (0.2, 1), (0.8, 0))
    x = sa.mix(0.5 * sa.norm(chime), (0.35, 0.2 * sa.norm(whisper)))
    return _clean(x, "ui", 0.8, 0.18, 8500)


def aura_bond_trait(v, rng):
    """A trait taken: a seal pressed home, a low chord settling into place, a bright edge after it."""
    dur = 1.8
    press = sa.thump(130, 64, 0.25, 0.06, drive=1.4, knock=0.3)
    chord = sa.mix(*[(0.02 * i, sa.bell(sa.note(*n), 1.4, 0.85, 2.0, 1.3, attack=0.04)) for i, n in enumerate(((D, 1), (A, 1), (D, 2)))])
    edge = sa.glass(sa.note(A, 3), 0.8, 0.25)
    x = sa.mix(0.45 * sa.highpass(press, 60), (0.03, 0.5 * sa.norm(chord)), (0.25, 0.2 * edge))
    return _clean(x, "effect", 0.8, 0.18, 8000)


def aura_bond_home(v, rng):
    """A blade going home: the air drawn away into a point, a short ring where it went."""
    dur = 1.0
    draw = sa.reverse(sa.moving_band(0.5, [(0, 5500), (0.5, 1200)], 0.6, rng) * sa.decay(0.5, 0.14, 0.004))
    ring = sa.glass(sa.note(D, 3) * (1 + 0.004 * v), 0.6, 0.18)
    x = sa.mix(0.55 * sa.norm(draw), (0.45, 0.3 * ring))
    return _clean(x, "effect", 0.7, 0.16, 7500)


def aura_bond_release(v, rng):
    """A bond released: a few notes falling down the scale, a long breath let out, the blade's ring fading to nothing."""
    dur = 2.4
    notes = sa.mix(*[(0.22 * k, (0.8 - 0.12 * k) * sa.bell(sa.note(n, o), 1.0, 0.8, 2.0, 1.2, attack=0.02))
                     for k, (n, o) in enumerate(((A, 2), (FS, 2), (D, 2), (A, 1)))])
    breath = sa.moving_band(1.4, [(0, 2400), (1.4, 700)], 0.6, rng) * sa.env(1.4, (0, 0), (0.2, 1), (1.4, 0))
    ring = _ring(1.6, (D, 1), 0.5)
    x = sa.mix(0.45 * sa.norm(notes), (0.5, 0.2 * sa.norm(breath)), (0.2, 0.15 * ring))
    return _clean(x, "effect", 1.0, 0.2, 6500)


def aura_bond_pass(v, rng):
    """A blade passed on: two chords, one high and one low, braiding into one, a bell after (the master's and the disciple's)."""
    dur = 2.8
    high = sa.mix(*[(0.02 * i, sa.bell(sa.note(*n), 1.6, 0.85, 2.0, 1.3, attack=0.06)) for i, n in enumerate(((A, 2), (D, 3)))])
    low = sa.mix(*[(0.02 * i, sa.bell(sa.note(*n), 1.6, 0.85, 2.0, 1.3, attack=0.06)) for i, n in enumerate(((D, 1), (A, 1)))])
    one = sa.mix(*[(0.03 * i, sa.bell(sa.note(*n), 2.0, 0.85, 2.0, 1.4, attack=0.04)) for i, n in enumerate(((D, 1), (FS, 2), (A, 2), (D, 3)))])
    bell = sa.partials(sa.note(D, 1), 1.6, ((1.0, 1.0, 1.0), (2.0, 0.5, 0.6), (2.76, 0.4, 0.5)), 0.9)
    x = sa.mix(0.3 * sa.norm(high), (0.1, 0.3 * sa.norm(low)), (0.7, 0.45 * sa.norm(sa.chorus(one, voices=2))), (1.1, 0.25 * sa.norm(bell)))
    return _clean(x, "grand", 1.1, 0.2, 7500)


BOND_EVENTS = [
    event("aura_bond_kindle", aura_bond_kindle, role="effect", subtitle="cast", attenuation=24),
    event("aura_bond_join", aura_bond_join, role="effect", subtitle="field", attenuation=24),
    event("aura_bond_seal", aura_bond_seal, role="grand", subtitle="field", attenuation=40),
    event("aura_bond_fail", aura_bond_fail, role="ui", subtitle="tell"),
    event("aura_bond_tier", aura_bond_tier, role="effect", subtitle="tell", attenuation=32),
    event("aura_bond_name", aura_bond_name, role="ui", subtitle="tell"),
    event("aura_bond_trait", aura_bond_trait, role="effect", subtitle="tell"),
    event("aura_bond_home", aura_bond_home, variants=2, role="effect", subtitle="tell", attenuation=24),
    event("aura_bond_release", aura_bond_release, role="effect", subtitle="tell"),
    event("aura_bond_pass", aura_bond_pass, role="grand", subtitle="field", attenuation=32),
]
