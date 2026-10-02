"""Awakening's sounds: the moment a swordsman lets everything go at once, each method's voice over it, and what comes after. Not a
part of its own: tools/feel/aura.py adds these to its events (they live in the aura folder).

    aura_awaken          the shared stinger: breath and air drawn in hard, a deep blow as it bursts (a third of a second in, as the
                         burst lands), and a chord swelling open over a rumble and a long shimmer
    aura_awaken_ember    a column of flame roaring up and crackling
    aura_awaken_rime     ice cracking outward in a ring, a cold wind, a cascade of glass
    aura_awaken_thunder  a thunderclap out of a clear sky, rolling away, a buzz under it
    aura_awaken_gale     a cyclone winding up, whistles wheeling in it, a gust as it breaks
    aura_awaken_stone    the ground quaking: a deep grinding rumble and stones heaving up in heavy steps
    aura_awaken_verdant  leaves rushing, wood creaking and a warm chord opening in flower
    aura_awaken_hollow   everything drawn in backwards, a sub-deep blow and a hollow drone falling away
    aura_awaken_starlit  starlight gathering and a bright bell chord bursting into glitter
    aura_awaken_hourglass  a clock's ticks quickening, then a great bell tolled and its hum
    aura_awaken_crimson  a heartbeat quickening into a dark surge and a wet roar
    aura_awaken_steel    plain steel: a blade's long ring and an airy surge (a method without a voice of its own)
    aura_awaken_fed      a finisher feeding the awakening: a short bright swell
    aura_spent           the awakening burned out: a long falling exhale, a drone sinking, ash crackling away
    aura_recovered       spent no longer: a soft breath in and two glassy notes stepping up

Each method's voice starts at its burst (a third of a second in, over the shared stinger) and lasts under two seconds. Everything
tonal is in D pentatonic like the rest of the mod. The moment is heard far (it's a moment for everyone near).
"""
import numpy as np

from feel.core import sa, event

D, E, FS, A, B = sa.D, sa.E, sa.FS, sa.A, sa.B

#: Where the burst lands in every awakening sound (seconds): aura.AwakeningRules.BURST_AT ticks.
BURST = 0.3


def _clean(x, role, verb=0.8, wet=0.18, damp=7000.0):
    return sa.finish(sa.highpass(sa.reverb(x, verb, wet, damp=damp), 45), role)


def _crackle(dur, rate, rng, lo=2000, hi=8000, shape=None):
    return sa.norm(sa.bandpass(sa.grains(dur, rate, rng, shape=shape), lo, hi))


def aura_awaken(v, rng):
    """The shared stinger: an inrush of air (a breath drawn hard, rising to the burst), a deep blow on it, then a chord swelling open,
    a rumble under it and a long shimmer over it."""
    dur = 2.6
    inrush = sa.reverse(sa.moving_band(BURST + 0.05, [(0, 6000), (BURST + 0.05, 700)], 0.8, rng) * sa.decay(BURST + 0.05, 0.12, 0.004))
    breath = sa.norm(sa.bandpass(sa.noise(BURST, rng), 500, 2400)) * sa.swell(BURST, BURST * 0.95)
    blow = sa.thump(70, 28, 1.3, 0.32, drive=2.6, knock=0.55)
    crack = sa.norm(sa.bandpass(sa.noise(0.12, rng), 1200, 9000)) * sa.decay(0.12, 0.02, 0.0005)
    chord = sa.mix(*[(0.04 * i, (1 - 0.1 * i) * sa.bell(sa.note(*n), 2.0, 0.85, 2.0, 1.4, attack=0.04))
                     for i, n in enumerate(((D, 0), (A, 0), (D, 1), (FS, 1), (A, 1)))])
    swell = (sa.sine(sa.note(D, -1), 2.1) + 0.6 * sa.sine(sa.note(A, -1), 2.1)) * sa.env(2.1, (0, 0), (0.25, 1), (2.1, 0))
    rumble = sa.norm(sa.bandpass(sa.brown(1.8, rng), 90, 600)) * sa.env(1.8, (0, 0), (0.1, 1), (1.8, 0))
    shimmer = sa.sparkle(1.6, 30, rng, [sa.note(d, 3) for d in (D, E, FS, A, B)], tau=(0.04, 0.14), shape=[(0, 1), (1.6, 0.1)], rising=True)
    x = sa.mix(0.6 * sa.norm(inrush), 0.3 * breath, (BURST, 0.8 * sa.highpass(blow, 30)), (BURST, 0.55 * crack),
               (BURST + 0.02, 0.55 * sa.norm(sa.chorus(chord, voices=2))), (BURST, 0.3 * swell), (BURST, 0.35 * rumble),
               (BURST + 0.1, 0.25 * sa.norm(shimmer)))
    return _clean(x, "grand", 1.5, 0.24, 7000)


# ================================================================ each method's voice, from its burst

def aura_awaken_ember(v, rng):
    dur = 2.0
    column = sa.moving_band(1.5, [(0, 250), (0.3, 3600), (1.5, 1000)], 0.95, rng) * sa.env(1.5, (0, 0), (0.18, 1), (1.5, 0))
    roar = sa.norm(sa.lowpass(sa.brown(1.6, rng), 800)) * sa.env(1.6, (0, 0), (0.15, 1), (1.6, 0))
    whump = sa.thump(90, 36, 0.6, 0.15, drive=2.2)
    crackle = _crackle(1.5, 160, rng, shape=[(0, 0.2), (0.25, 1), (1.5, 0.15)])
    x = sa.mix((BURST, 0.7 * sa.norm(column)), (BURST, 0.6 * roar), (BURST, 0.4 * sa.highpass(whump, 50)), (BURST + 0.1, 0.4 * crackle))
    return _clean(x, "effect", 0.8, 0.16, 6500)


def aura_awaken_rime(v, rng):
    dur = 2.0
    crack = _crackle(0.3, 300, rng, 2500, 9500, [(0, 1), (0.3, 0.3)])
    shatter = sa.shatter(rng, count=30, band=(2400, 8000), spread=0.3, tau=(0.04, 0.16))
    wind = sa.moving_band(1.4, [(0, 1800), (0.4, 3400), (1.4, 900)], 0.4, rng) * sa.env(1.4, (0, 0), (0.25, 1), (1.4, 0))
    ring = sa.mix(sa.glass(sa.note(D, 2), 1.3, 0.5), (0.03, 0.6 * sa.glass(sa.note(A, 2), 1.2, 0.45)), (0.06, 0.4 * sa.glass(sa.note(E, 3), 1.1, 0.4)))
    x = sa.mix((BURST, 0.6 * crack), (BURST, 0.55 * sa.norm(shatter)), (BURST, 0.35 * sa.norm(wind)), (BURST + 0.05, 0.4 * sa.norm(ring)))
    return _clean(x, "effect", 0.9, 0.18, 9500)


def aura_awaken_thunder(v, rng):
    dur = 2.4
    crack = sa.norm(sa.bandpass(sa.noise(0.09, rng), 1500, 10000)) * sa.decay(0.09, 0.012, 0.0003)
    boom = sa.thump(85, 36, 1.1, 0.28, drive=2.8, knock=0.6)
    roll = sa.norm(sa.bandpass(sa.brown(1.9, rng), 240, 1300)) * sa.chopper(1.9, rng, (6, 14), 0.4) * sa.env(1.9, (0, 0), (0.1, 1), (1.9, 0))
    buzz = sa.soft_saw(sa.note(D, 0), 1.2, harmonics=10) * sa.chopper(1.2, rng, (90, 200), 0.3)
    buzz = sa.norm(sa.bandpass(buzz, 300, 4000)) * sa.env(1.2, (0, 0), (0.1, 1), (1.2, 0))
    x = sa.mix((BURST, 0.9 * crack), (BURST, 0.5 * sa.highpass(boom, 55)), (BURST + 0.05, 0.6 * roll), (BURST, 0.18 * buzz))
    return _clean(x, "effect", 1.0, 0.2, 6000)


def aura_awaken_gale(v, rng):
    dur = 2.0
    t = sa.timeline(1.6)
    cyclone = sa.moving_band(1.6, [(0, 400), (0.5, 3200), (1.6, 1300)], 0.8, rng) * sa.env(1.6, (0, 0), (0.4, 1), (1.6, 0))
    beat = 0.6 + 0.4 * np.sin(2 * np.pi * t * (3 + 7 * t))
    whistles = sa.mix(*[(0.08 + 0.15 * k, (0.5 - 0.06 * k) * sa.sine(sa.sweep(sa.note(*n), sa.note(*n) * 1.5, 0.4, 0.6), 0.4)
                         * sa.env(0.4, (0, 0), (0.15, 1), (0.4, 0))) for k, n in enumerate(((D, 2), (FS, 2), (A, 2), (D, 3), (FS, 3)))])
    gust = sa.norm(sa.bandpass(sa.noise(0.5, rng), 400, 3000)) * sa.swell(0.5, 0.08)
    x = sa.mix((BURST, 0.75 * sa.norm(cyclone) * beat), (BURST, 0.3 * whistles), (BURST, 0.4 * gust))
    return _clean(x, "effect", 0.8, 0.17, 8500)


def aura_awaken_stone(v, rng):
    dur = 2.2
    quake = sa.norm(sa.bandpass(sa.brown(1.8, rng), 60, 500)) * sa.chopper(1.8, rng, (8, 18), 0.5) * sa.env(1.8, (0, 0), (0.08, 1), (1.8, 0))
    split = sa.thump(100, 40, 0.9, 0.22, drive=2.8, knock=0.8)
    steps = sa.mix(*[(0.1 + 0.13 * k + rng.uniform(-0.02, 0.02), (0.75 - 0.1 * k) * sa.thump(160 - 15 * k, 70, 0.3, 0.07, drive=2.4, knock=0.8))
                     for k in range(5)])
    grind = sa.norm(sa.bandpass(sa.grains(1.4, 120, rng, length=(0.003, 0.015), shape=[(0, 1), (1.4, 0.1)]), 300, 2400))
    # Each stone heaving up: a crunch in the band small speakers play, so the quake still reads on a laptop.
    crunches = sa.mix(*[(0.1 + 0.13 * k, (0.7 - 0.08 * k) * sa.norm(sa.bandpass(sa.noise(0.14, rng), 300, 2600)) * sa.decay(0.14, 0.035, 0.0008))
                        for k in range(5)])
    rumble = sa.norm(sa.bandpass(sa.brown(1.5, rng), 250, 900)) * sa.env(1.5, (0, 0), (0.1, 1), (1.5, 0))
    x = sa.mix((BURST, 0.4 * quake), (BURST, 0.4 * sa.highpass(split, 45)), (BURST, 0.35 * sa.highpass(steps, 70)), (BURST, 0.7 * crunches),
               (BURST + 0.05, 0.55 * grind), (BURST, 0.35 * rumble))
    return _clean(x, "effect", 0.7, 0.15, 6000)


def aura_awaken_verdant(v, rng):
    dur = 2.2
    rush = sa.norm(sa.bandpass(sa.grains(1.4, 140, rng, length=(0.001, 0.005), shape=[(0, 0.3), (0.2, 1), (1.4, 0.1)]), 2000, 8000))
    tone = sa.soft_saw(sa.sweep(100, 150, 0.7, 0.6), 0.7, harmonics=8) * sa.chopper(0.7, rng, (25, 70), 0.3)
    creak = sa.norm(sa.bandpass(tone, 180, 2400)) * sa.swell(0.7, 0.4)
    bloom = sa.mix(*[(0.07 * i, (1 - 0.12 * i) * sa.bell(sa.note(*n), 1.6, 0.75, 2.0, 0.9, attack=0.07))
                     for i, n in enumerate(((D, 1), (FS, 1), (A, 1), (D, 2), (FS, 2)))])
    x = sa.mix((BURST, 0.45 * rush), (BURST, 0.25 * creak), (BURST + 0.1, 0.6 * sa.norm(sa.chorus(bloom, voices=2))))
    return _clean(x, "effect", 1.0, 0.18, 8500)


def aura_awaken_hollow(v, rng):
    dur = 2.2
    pull = sa.reverse(sa.moving_band(0.5, [(0, 5000), (0.5, 400)], 0.9, rng) * sa.decay(0.5, 0.18, 0.004))
    boom = sa.thump(58, 22, 1.2, 0.3, drive=2.4, knock=0.2)
    drone = sa.partials(sa.note(D, -1), 1.6, ((1.0, 1.0, 1.0), (2.0, 0.5, 0.7), (3.01, 0.3, 0.5)), 0.6)
    sink = sa.sine(sa.sweep(sa.note(A, 0), sa.note(D, -1), 1.0, 0.6), 1.0) * sa.decay(1.0, 0.4, 0.01)
    x = sa.mix((max(0.0, BURST - 0.45), 0.55 * sa.norm(pull)), (BURST, 0.7 * sa.highpass(boom, 30)), (BURST, 0.35 * sa.lowpass(drone, 1500)),
               (BURST + 0.05, 0.3 * sink))
    return _clean(x, "effect", 1.2, 0.22, 3800)


def aura_awaken_starlit(v, rng):
    dur = 2.2
    gather = sa.sparkle(BURST, 40, rng, [sa.note(*n) for n in ((D, 2), (FS, 2), (A, 2), (D, 3))], tau=(0.02, 0.05),
                        shape=[(0, 0.3), (BURST, 1)], rising=True)
    bell = sa.mix(sa.bell(sa.note(D, 2), 1.6, 0.7, 2.0, 2.2), (0.01, 0.7 * sa.bell(sa.note(A, 2), 1.5, 0.65, 2.0, 2.2)),
                  (0.02, 0.5 * sa.bell(sa.note(FS, 3), 1.4, 0.6, 2.0, 2.4)))
    glitter = sa.sparkle(1.5, 40, rng, [sa.note(d, 3) for d in (D, E, FS, A, B)], tau=(0.04, 0.14), shape=[(0, 1), (1.5, 0.05)])
    x = sa.mix(0.35 * sa.norm(gather), (BURST, 0.6 * sa.norm(bell)), (BURST + 0.03, 0.35 * sa.norm(glitter)))
    return _clean(x, "effect", 1.0, 0.2, 10500)


def aura_awaken_hourglass(v, rng):
    dur = 2.6
    # Ticks quickening into the burst.
    gaps = [0.0, 0.11, 0.2, 0.26]
    ticks = sa.mix(*[(g, sa.tick(sa.note(A, 2) * (1 + 0.04 * k), rng, 0.6 + 0.1 * k)) for k, g in enumerate(gaps)])
    toll = sa.mix(sa.clock_bell(sa.note(D, 0), 2.1, 0.9), 0.6 * sa.clock_bell(sa.note(A, 0), 2.0, 0.8), 0.4 * sa.clock_bell(sa.note(D, 1), 1.9, 0.7))
    hum = (sa.sine(sa.note(D, -1), 1.8) + 0.5 * sa.sine(sa.note(A, -1), 1.8)) * sa.env(1.8, (0, 0), (0.3, 1), (1.8, 0))
    x = sa.mix(0.45 * sa.norm(ticks), (BURST, 0.65 * sa.norm(toll)), (BURST, 0.25 * hum))
    return _clean(x, "effect", 1.0, 0.2, 9000)


def _beat(f0=62, strength=1.0):
    lub = sa.thump(f0, f0 * 0.55, 0.28, 0.07, drive=1.8, knock=0.05)
    dub = sa.thump(f0 * 1.12, f0 * 0.6, 0.26, 0.06, drive=1.8, knock=0.05)
    return strength * sa.mix(0.9 * lub, (0.15, 0.75 * dub))


def aura_awaken_crimson(v, rng):
    dur = 2.2
    beats = sa.mix(*[(t0, (0.6 + 0.2 * k) * _beat(60 + 3 * k)) for k, t0 in enumerate((0.0, 0.17, 0.3))])
    surge = sa.sine(sa.sweep(sa.note(D, -1), sa.note(A, -1), 1.0, 0.5), 1.0) * sa.swell(1.0, 0.35)
    roar = sa.saturate(sa.norm(sa.moving_band(1.3, [(0, 300), (0.35, 1800), (1.3, 400)], 0.9, rng)), 1.6) * sa.env(1.3, (0, 0), (0.2, 1), (1.3, 0))
    x = sa.mix(0.5 * sa.highpass(beats, 40), (BURST, 0.3 * surge), (BURST, 0.55 * roar))
    return _clean(x, "effect", 0.8, 0.16, 5500)


def aura_awaken_steel(v, rng):
    dur = 2.0
    ring = sa.partials(sa.note(D, 1), 1.6, ((1.0, 1.0, 1.0), (2.41, 0.6, 0.55), (3.9, 0.35, 0.35), (5.6, 0.2, 0.25)), 0.6)
    surge = sa.moving_band(1.2, [(0, 600), (0.3, 4200), (1.2, 1200)], 0.6, rng) * sa.env(1.2, (0, 0), (0.2, 1), (1.2, 0))
    edge = sa.glass(sa.note(A, 3), 1.2, 0.4)
    x = sa.mix((BURST, 0.45 * ring), (BURST, 0.5 * sa.norm(surge)), (BURST + 0.05, 0.25 * edge))
    return _clean(x, "effect", 0.8, 0.16, 8000)


# ================================================================ around it

def aura_awaken_fed(v, rng):
    """A finisher feeding the awakening: a short bright swell up and two glassy notes."""
    dur = 0.8
    swell = sa.moving_band(0.3, [(0, 1200), (0.3, 5000)], 0.6, rng) * sa.swell(0.3, 0.25)
    first = sa.glass(sa.note(A, 2), 0.4, 0.12)
    second = sa.glass(sa.note(D, 3), 0.6, 0.2)
    x = sa.mix(0.35 * sa.norm(swell), (0.05, 0.5 * first), (0.12, 0.6 * second))
    return _clean(x, "effect", 0.6, 0.14, 9500)


def aura_spent(v, rng):
    """The awakening burned out: a long exhale falling away, a drone sinking down the scale, ash crackling off and fading."""
    dur = 2.0
    exhale = sa.norm(sa.bandpass(sa.noise(1.2, rng), 300, 1800)) * sa.env(1.2, (0, 0), (0.08, 1), (1.2, 0))
    exhale = exhale * sa.curve([(0, 1.0), (1.2, 0.4)], sa.timeline(1.2))
    drone = sa.soft_saw(sa.glide(1.6, (0, sa.note(A, 0)), (1.6, sa.note(D, -1))), harmonics=6) * sa.env(1.6, (0, 0), (0.1, 1), (1.6, 0))
    ash = _crackle(1.3, 70, rng, 1500, 6000, shape=[(0, 1), (1.3, 0)])
    thud = sa.thump(110, 50, 0.4, 0.1, drive=1.4, knock=0.1)
    x = sa.mix(0.5 * exhale, (0.05, 0.3 * sa.norm(sa.lowpass(drone, 1400))), (0.1, 0.25 * ash), 0.25 * sa.highpass(thud, 60))
    return _clean(x, "effect", 0.6, 0.14, 5000)


def aura_recovered(v, rng):
    """Spent no longer: a soft breath in and two glassy notes stepping up (D, then A), quiet."""
    dur = 0.9
    breath = sa.norm(sa.bandpass(sa.noise(0.4, rng), 600, 2600)) * sa.swell(0.4, 0.35)
    first = sa.glass(sa.note(D, 2), 0.4, 0.1)
    second = sa.glass(sa.note(A, 2), 0.6, 0.18)
    x = sa.mix(0.3 * breath, (0.3, 0.5 * first), (0.42, 0.6 * second))
    return _clean(x, "ui", 0.5, 0.12, 9500)


AWAKENING_EVENTS = [
    event("aura_awaken", aura_awaken, role="grand", subtitle="field", attenuation=48),
    event("aura_awaken_ember", aura_awaken_ember, role="effect", subtitle="cast", attenuation=40),
    event("aura_awaken_rime", aura_awaken_rime, role="effect", subtitle="cast", attenuation=40),
    event("aura_awaken_thunder", aura_awaken_thunder, role="effect", subtitle="cast", attenuation=48),
    event("aura_awaken_gale", aura_awaken_gale, role="effect", subtitle="cast", attenuation=40),
    event("aura_awaken_stone", aura_awaken_stone, role="effect", subtitle="cast", attenuation=40),
    event("aura_awaken_verdant", aura_awaken_verdant, role="effect", subtitle="cast", attenuation=40),
    event("aura_awaken_hollow", aura_awaken_hollow, role="effect", subtitle="cast", attenuation=40),
    event("aura_awaken_starlit", aura_awaken_starlit, role="effect", subtitle="cast", attenuation=40),
    event("aura_awaken_hourglass", aura_awaken_hourglass, role="effect", subtitle="cast", attenuation=40),
    event("aura_awaken_crimson", aura_awaken_crimson, role="effect", subtitle="cast", attenuation=40),
    event("aura_awaken_steel", aura_awaken_steel, role="effect", subtitle="cast", attenuation=40),
    event("aura_awaken_fed", aura_awaken_fed, role="effect", subtitle="tell", attenuation=24),
    event("aura_spent", aura_spent, role="effect", subtitle="tell", attenuation=24),
    event("aura_recovered", aura_recovered, role="ui", subtitle="tell"),
]
