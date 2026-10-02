"""Each breathing method's arts, each in its own voice: what plays as an art goes off (under it, quieter, the method's own technique
sound from tools/feel/aura_methods.py). Not a part of its own: tools/feel/aura.py adds these to its events (they live in the aura
folder, named aura_art_<art>).

  Ember (roar and crackle)
    kindling_draw       a blade drawn with a bright ring, then fuel catching and running away from you
    rising_cinders      a fwump of heat and a roar climbing, cinders popping as it tops out
    backdraft           a breath sucked in, then a gout of flame blasting out
    wildfire_rush       a rush of fire past you, crackle left burning behind
    sunfall             a sun gathering: a bright chord swelling and climbing, glittering
    sunfall_impact      the landing: a huge low boom, a roar of flame and the chord ringing out over it

  Rime (glass and cold air)
    frostbite           ice crackling as it forms, and a cold glassy ping
    hailfall            a whistle falling out of the sky, then a patter of ice breaking
    glacier_mirror      ice cracking shut, and a long resonant glass ring with a shimmer
    skate               a long smooth scrape of a blade over ice, glints along it
    winters_hush        cold wind falling away into silence over a high crystal drone
    winters_hush_shatter a cascade of breaking glass over a deep thud

  Thunder (snaps, buzz and rolling thunder)
    crackle             three electric snaps faster than you can count, rising
    skyfall             a crackle gathering, then a thunderclap and its roll
    static_riposte      a buzzing charge, then zaps leaping away down the scale
    bolt_step           one blink: a zip and a crack
    heavens_spear       a whine climbing as lightning gathers, then a crack-boom and the thunder rolling

  Gale (whistles and wind)
    cutting_breeze      a whistling blade of wind flying away from you
    updraft             a vortex whooshing upward, its whirl beating
    eye_of_the_storm    a gust, then two whistles wheeling round you
    tailwind            a long rush of air sweeping past, its whistle falling
    hundred_winds       a howling whirlwind, whistling cuts slicing through it

  Stone (thuds, grit and rumble)
    rockbreaker         a boulder split: a deep crunch and gravel
    avalanche           a slam, and rocks rolling away
    unmoved             a hollow stone note, deep, settling with a grind
    landslide           a charge rumbling up, rocks tumbling, a crash at its end
    mountain_splitter   the ground splitting with a crack, then stone rising in step after step

Everything tonal is in D pentatonic like the rest of the mod.
"""
import numpy as np

from feel.core import sa, event

D, E, FS, A, B = sa.D, sa.E, sa.FS, sa.A, sa.B


def _clean(x, role, verb=0.4, wet=0.12, damp=7000.0):
    return sa.finish(sa.highpass(sa.reverb(x, verb, wet, damp=damp), 50), role)


def _whoosh(dur, path, width, rng, peak=0.3):
    return sa.moving_band(dur, path, width, rng) * sa.env(dur, (0, 0), (dur * peak, 1), (dur * 0.75, 0.3), (dur, 0))


def _crackle(dur, rate, rng, lo=2000, hi=8000, shape=None):
    return sa.norm(sa.bandpass(sa.grains(dur, rate, rng, shape=shape), lo, hi))


def _zap(dur, f0, f1, rng):
    """An electric zap: a buzz sweeping down, chopped like an arc, with a crack on its front."""
    buzz = sa.soft_saw(sa.sweep(f0, f1, dur, 0.6), harmonics=10) * sa.chopper(dur, rng, (90, 220), 0.3) * sa.decay(dur, dur * 0.35, 0.001)
    crack = sa.norm(sa.bandpass(sa.noise(0.05, rng), 1800, 9000)) * sa.decay(0.05, 0.006, 0.0003)
    return sa.mix(0.7 * sa.norm(sa.bandpass(buzz, 300, 7000)), 0.8 * crack)


# ================================================================ Ember

def kindling_draw(v, rng):
    dur = 0.95
    ring = 0.6 * sa.glass(sa.note(A, 2), 0.5, 0.08) + 0.4 * sa.glass(sa.note(D, 3), 0.5, 0.06)
    shing = sa.moving_band(0.2, [(0, 7000), (0.2, 3000)], 0.4, rng) * sa.decay(0.2, 0.05, 0.002)
    catch = sa.moving_band(0.8, [(0, 500), (0.25, 2200), (0.8, 900)], 0.9, rng) * sa.env(0.8, (0, 0), (0.18, 1), (0.8, 0))
    roar = sa.norm(sa.lowpass(sa.brown(0.8, rng), 700)) * sa.env(0.8, (0, 0), (0.25, 0.8), (0.8, 0))
    crackle = _crackle(0.8, 110, rng, shape=[(0, 0.2), (0.3, 1), (0.8, 0.4)])
    x = sa.mix(0.55 * ring, 0.7 * sa.norm(shing), (0.1, 0.6 * sa.norm(catch)), (0.12, 0.4 * roar), (0.15, 0.35 * crackle))
    return _clean(x, "cast", 0.45, 0.12, 6500)


def rising_cinders(v, rng):
    dur = 1.05
    fwump = sa.thump(110, 50, 0.3, 0.07, drive=1.6)
    rise = sa.moving_band(dur, [(0, 300), (0.45, 3600), (dur, 1500)], 0.85, rng) * sa.env(dur, (0, 0), (0.42, 1), (dur, 0))
    roar = sa.norm(sa.lowpass(sa.brown(dur, rng), 900)) * sa.env(dur, (0, 0), (0.35, 0.9), (dur, 0))
    pops = sa.norm(sa.bandpass(sa.grains(0.6, 70, rng, length=(0.002, 0.008), shape=[(0, 1), (0.6, 0.2)]), 1500, 7000))
    x = sa.mix(0.45 * sa.highpass(fwump, 60), 0.75 * sa.norm(rise), 0.4 * roar, (0.42, 0.45 * pops))
    return _clean(x, "cast", 0.5, 0.13, 6500)


def backdraft(v, rng):
    dur = 1.2
    inhale = sa.moving_band(0.28, [(0, 600), (0.28, 2600)], 0.8, rng) * sa.env(0.28, (0, 0), (0.24, 1), (0.28, 0))
    blast = sa.norm(sa.lowpass(sa.brown(0.9, rng), 1200)) * sa.decay(0.9, 0.22, 0.004)
    burst = sa.norm(sa.bandpass(sa.noise(0.5, rng), 500, 4500)) * sa.decay(0.5, 0.09, 0.002)
    whomp = sa.thump(95, 40, 0.45, 0.1, drive=2.0)
    crackle = _crackle(0.9, 130, rng, shape=[(0, 1), (0.9, 0.1)])
    x = sa.mix(0.45 * sa.norm(inhale), (0.28, 0.8 * blast), (0.28, 0.75 * burst), (0.28, 0.55 * sa.highpass(whomp, 55)), (0.3, 0.35 * crackle))
    return _clean(x, "cast", 0.55, 0.14, 6000)


def wildfire_rush(v, rng):
    dur = 1.0
    rush = sa.moving_band(dur, [(0, 900), (0.22, 4200), (0.5, 1200), (dur, 500)], 0.9, rng) * sa.env(dur, (0, 0), (0.2, 1), (0.55, 0.35), (dur, 0))
    roar = sa.norm(sa.lowpass(sa.brown(dur, rng), 800)) * sa.env(dur, (0, 0), (0.15, 1), (0.5, 0.5), (dur, 0))
    trail = _crackle(dur, 140, rng, shape=[(0, 0.1), (0.25, 1), (dur, 0.6)])
    whump = sa.thump(120, 60, 0.25, 0.06, drive=1.5)
    x = sa.mix(0.8 * sa.norm(rush), 0.45 * roar, (0.15, 0.4 * trail), (0.55, 0.35 * sa.highpass(whump, 70)))
    return _clean(x, "cast", 0.5, 0.12, 6500)


def sunfall(v, rng):
    dur = 1.5
    chord = sa.mix(sa.soft_saw(sa.glide(dur, (0, sa.note(D, -1)), (dur, sa.note(D, 0))), harmonics=8),
                   0.7 * sa.soft_saw(sa.glide(dur, (0, sa.note(A, -1)), (dur, sa.note(A, 0))), harmonics=8),
                   0.5 * sa.soft_saw(sa.glide(dur, (0, sa.note(FS, 0)), (dur, sa.note(FS, 1))), harmonics=6))
    chord = sa.lowpass(chord, 2600) * sa.swell(dur, 1.1, 1.5)
    glitter = sa.sparkle(1.2, 16, rng, [sa.note(D, 2), sa.note(FS, 2), sa.note(A, 2), sa.note(D, 3)], shape=[(0, 0.2), (1.2, 1)], rising=True)
    air = sa.moving_band(dur, [(0, 600), (1.2, 3000)], 1.0, rng) * sa.swell(dur, 1.1, 1.5)
    x = sa.mix(0.6 * sa.norm(sa.chorus(chord, voices=3)), (0.2, 0.3 * sa.norm(glitter)), 0.3 * sa.norm(air))
    return _clean(x, "cast", 0.8, 0.16, 7000)


def sunfall_impact(v, rng):
    dur = 1.8
    boom = sa.thump(70, 28, 1.2, 0.3, drive=2.4, knock=0.4)
    roar = sa.norm(sa.lowpass(sa.brown(1.4, rng), 900)) * sa.decay(1.4, 0.4, 0.004)
    flare = sa.norm(sa.bandpass(sa.noise(0.6, rng), 600, 5000)) * sa.decay(0.6, 0.12, 0.002)
    crackle = _crackle(1.4, 150, rng, shape=[(0, 1), (1.4, 0.1)])
    bloom = sa.mix(sa.bell(sa.note(D, 0), 1.6, 0.6, 2.0, 1.2), 0.7 * sa.bell(sa.note(A, 0), 1.6, 0.55, 2.0, 1.2), 0.4 * sa.bell(sa.note(D, 1), 1.4, 0.5, 2.0, 1.0))
    debris = sa.norm(sa.bandpass(sa.grains(1.0, 60, rng, length=(0.003, 0.012), shape=[(0, 1), (1.0, 0)]), 300, 2500))
    x = sa.mix(0.7 * sa.highpass(boom, 35), 0.6 * roar, 0.6 * flare, (0.05, 0.35 * crackle), (0.04, 0.4 * sa.saturate(bloom, 1.4)), (0.1, 0.25 * debris))
    return _clean(x, "impact", 0.9, 0.16, 6000)


# ================================================================ Rime

def frostbite(v, rng):
    dur = 0.75
    form = sa.norm(sa.bandpass(sa.grains(0.45, 260, rng, length=(0.0005, 0.002), shape=[(0, 0.2), (0.45, 1)]), 3000, 10000))
    crunch = sa.norm(sa.bandpass(sa.noise(0.12, rng), 1500, 6000)) * sa.decay(0.12, 0.025, 0.001)
    ping = sa.mix(sa.glass(sa.note(B, 2), 0.6, 0.15), 0.5 * sa.glass(sa.note(FS, 3), 0.5, 0.1))
    whistle = _whoosh(0.3, [(0, 3000), (0.3, 5200)], 0.2, rng)
    x = sa.mix(0.5 * form, (0.38, 0.6 * crunch), (0.4, 0.45 * ping), 0.25 * sa.norm(whistle))
    return _clean(x, "cast", 0.45, 0.13, 9000)


def hailfall(v, rng):
    dur = 1.3
    fall = sa.moving_band(0.5, [(0, 5200), (0.5, 1800)], 0.18, rng) * sa.env(0.5, (0, 0), (0.1, 1), (0.5, 0.4))
    stones = []
    for k in range(7):
        t0 = 0.4 + k * 0.11 + rng.uniform(-0.03, 0.03)
        stones.append((t0, sa.shatter(rng, count=6, band=(2500, 7500), spread=0.03, tau=(0.02, 0.06)) * rng.uniform(0.5, 1.0)))
        stones.append((t0, 0.3 * sa.thump(220, 120, 0.06, 0.015, drive=1.2)))
    x = sa.mix(0.55 * sa.norm(fall), *stones)
    return _clean(x, "cast", 0.45, 0.12, 9000)


def glacier_mirror(v, rng):
    dur = 1.3
    crack = sa.shatter(rng, count=10, band=(1800, 6000), spread=0.06, tau=(0.03, 0.09))
    ring = sa.mix(sa.glass(sa.note(D, 2), 1.2, 0.5), 0.6 * sa.glass(sa.note(A, 2), 1.1, 0.45), 0.35 * sa.bell(sa.note(D, 3), 1.0, 0.4, 3.0, 0.8))
    shimmer = sa.sparkle(0.9, 10, rng, [sa.note(A, 2), sa.note(D, 3), sa.note(E, 3)])
    x = sa.mix(0.55 * crack, (0.06, 0.6 * sa.chorus(ring, voices=2, depth=0.002, rate=0.7)), (0.15, 0.25 * sa.norm(shimmer)))
    return _clean(x, "cast", 0.8, 0.16, 9000)


def skate(v, rng):
    dur = 1.0
    scrape = sa.norm(sa.bandpass(sa.noise(dur, rng), 2400, 7200)) * sa.env(dur, (0, 0), (0.06, 1), (0.6, 0.8), (dur, 0))
    hiss = sa.moving_band(dur, [(0, 4000), (dur, 5600)], 0.25, rng) * sa.env(dur, (0, 0), (0.1, 1), (dur, 0))
    glints = sa.sparkle(0.85, 14, rng, [sa.note(FS, 3), sa.note(A, 3), sa.note(B, 3)], tau=(0.04, 0.1))
    glide = sa.sine(sa.sweep(sa.note(D, 1), sa.note(A, 1), dur, 1.0), dur) * sa.swell(dur, 0.3) * 0.25
    x = sa.mix(0.55 * scrape, 0.35 * sa.norm(hiss), 0.3 * sa.norm(glints), glide)
    return _clean(x, "cast", 0.45, 0.12, 9500)


def winters_hush(v, rng):
    dur = 2.0
    wind = sa.moving_band(dur, [(0, 2200), (1.2, 600)], 0.9, rng) * sa.env(dur, (0, 1), (1.2, 0.15), (dur, 0))
    drone = (sa.sine(sa.note(D, 3), dur) + 0.7 * sa.sine(sa.note(A, 3) * 1.002, dur) + 0.3 * sa.sine(sa.note(D, 4) * 0.998, dur)) * sa.swell(dur, 0.9, 1.4)
    frost = sa.norm(sa.bandpass(sa.grains(0.8, 180, rng, length=(0.0005, 0.002), shape=[(0, 1), (0.8, 0.1)]), 4000, 11000))
    x = sa.mix(0.6 * sa.norm(wind), 0.3 * sa.norm(drone), 0.3 * frost)
    return _clean(x, "grand", 0.9, 0.16, 9000)


def winters_hush_shatter(v, rng):
    dur = 1.6
    glass = sa.shatter(rng, count=46, band=(1500, 8000), spread=0.45, tau=(0.04, 0.2))
    thud = sa.thump(85, 40, 0.5, 0.12, drive=1.8)
    tinkle = sa.sparkle(1.2, 22, rng, [sa.note(D, 3), sa.note(FS, 3), sa.note(A, 3), sa.note(B, 3), sa.note(D, 4)], shape=[(0, 1), (1.2, 0.1)])
    x = sa.mix(0.85 * glass, 0.5 * sa.highpass(thud, 50), (0.15, 0.3 * sa.norm(tinkle)))
    return _clean(x, "impact", 0.8, 0.15, 9000)


# ================================================================ Thunder

def crackle(v, rng):
    dur = 0.6
    snaps = []
    for k, f in enumerate((1900, 2400, 3100)):
        snaps.append((k * 0.1, _zap(0.12, f, f * 0.4, rng) * (0.8 + 0.1 * k)))
    tail = sa.norm(sa.bandpass(sa.noise(0.3, rng), 2000, 8000)) * sa.chopper(0.3, rng, (120, 260), 0.1) * sa.decay(0.3, 0.08, 0.002)
    x = sa.mix(*snaps, (0.22, 0.3 * tail))
    return _clean(x, "cast", 0.35, 0.1, 9000)


def skyfall(v, rng):
    dur = 1.7
    charge = sa.norm(sa.bandpass(sa.noise(0.32, rng), 1500, 7000)) * sa.chopper(0.32, rng, (60, 200), 0.1) * sa.env(0.32, (0, 0.1), (0.3, 1), (0.32, 0.6))
    clap = sa.norm(sa.bandpass(sa.noise(0.3, rng), 500, 6000)) * sa.decay(0.3, 0.05, 0.0005)
    rumble = sa.norm(sa.bandpass(sa.brown(1.35, rng), 90, 900)) * sa.decay(1.35, 0.45, 0.01)
    roll = sa.norm(sa.bandpass(sa.noise(1.2, rng), 300, 1500)) * sa.decay(1.2, 0.35, 0.01) * (0.6 + 0.4 * sa.chopper(1.2, rng, (8, 20), 0.3))
    body = sa.thump(110, 45, 0.5, 0.12, drive=2.0)
    x = sa.mix(0.4 * charge, (0.3, 0.9 * clap), (0.3, 0.4 * sa.highpass(body, 80)), (0.32, 0.4 * rumble), (0.34, 0.45 * roll))
    return _clean(x, "cast", 0.7, 0.15, 8000)


def static_riposte(v, rng):
    dur = 1.0
    hum = sa.soft_saw(sa.note(D, 0), 0.3, harmonics=14) * sa.chopper(0.3, rng, (80, 200), 0.4) * sa.swell(0.3, 0.25)
    zaps = []
    for k in range(5):
        f = 2600 * (0.85 ** k)
        zaps.append((0.2 + k * 0.1, _zap(0.14, f, f * 0.45, rng) * (1.0 - 0.12 * k)))
    x = sa.mix(0.45 * sa.norm(sa.bandpass(hum, 200, 6000)), *zaps)
    return _clean(x, "cast", 0.45, 0.12, 9000)


def bolt_step(v, rng):
    dur = 0.45
    zip_ = sa.sine(sa.sweep(3200, 500, 0.18, 0.5), 0.18) * sa.decay(0.18, 0.06, 0.001)
    x = sa.mix(0.6 * zip_, 0.8 * _zap(0.2, 2200, 800, rng), (0.02, 0.25 * sa.thump(160, 80, 0.15, 0.04, drive=1.4)))
    return _clean(x, "cast", 0.35, 0.1, 9000)


def heavens_spear(v, rng):
    dur = 2.2
    whine = sa.soft_saw(sa.glide(0.72, (0, sa.note(D, 0)), (0.72, sa.note(D, 2))), harmonics=10) * sa.swell(0.72, 0.68, 0.5)
    crackle = sa.norm(sa.bandpass(sa.noise(0.72, rng), 1500, 8000)) * sa.chopper(0.72, rng, (60, 240), 0.05) * sa.env(0.72, (0, 0.1), (0.7, 1))
    crack = sa.norm(sa.highpass(sa.noise(0.3, rng), 500)) * sa.decay(0.3, 0.05, 0.0005)
    boom = sa.thump(75, 30, 1.0, 0.25, drive=2.4, knock=0.35)
    roll = sa.norm(sa.lowpass(sa.brown(1.5, rng), 280)) * sa.decay(1.5, 0.55, 0.01)
    lance = sa.soft_saw(sa.note(A, 1), 0.6, harmonics=12) * sa.chopper(0.6, rng, (100, 260), 0.3) * sa.decay(0.6, 0.18, 0.002)
    x = sa.mix(0.4 * sa.norm(sa.lowpass(whine, 4000)), 0.3 * crackle, (0.72, 0.9 * crack), (0.72, 0.6 * sa.highpass(boom, 35)),
               (0.74, 0.55 * roll), (0.72, 0.3 * sa.norm(sa.bandpass(lance, 300, 6000))))
    return _clean(x, "grand", 0.8, 0.15, 8000)


# ================================================================ Gale

def cutting_breeze(v, rng):
    dur = 0.85
    whistle = _whoosh(dur, [(0, 2600), (0.12, 3400), (dur, 1300)], 0.16, rng, peak=0.18)
    air = _whoosh(dur, [(0, 900), (0.15, 3000), (dur, 700)], 0.8, rng, peak=0.2)
    x = sa.mix(0.6 * sa.norm(whistle), 0.6 * sa.norm(air))
    return _clean(x, "cast", 0.5, 0.12, 9000)


def updraft(v, rng):
    dur = 1.05
    rise = sa.moving_band(dur, [(0, 400), (0.7, 2600), (dur, 1800)], [(0, 0.6), (dur, 1.1)], rng) * sa.env(dur, (0, 0), (0.6, 1), (dur, 0))
    whirl = 0.7 + 0.3 * np.sin(2 * np.pi * sa.timeline(dur) * (6 + 6 * sa.timeline(dur)))
    whistle = _whoosh(dur, [(0, 1500), (dur, 3600)], 0.15, rng, peak=0.65)
    x = sa.mix(0.8 * sa.norm(rise) * whirl, 0.3 * sa.norm(whistle))
    return _clean(x, "cast", 0.5, 0.12, 9000)


def eye_of_the_storm(v, rng):
    dur = 1.4
    gust = _whoosh(0.4, [(0, 700), (0.1, 2600), (0.4, 800)], 0.9, rng, peak=0.2)
    t = sa.timeline(dur)
    centre_a = [(x, 1800 + 700 * np.sin(2 * np.pi * 2.2 * x)) for x in np.linspace(0, dur, 30)]
    centre_b = [(x, 2600 + 700 * np.sin(2 * np.pi * 2.2 * x + np.pi)) for x in np.linspace(0, dur, 30)]
    whistle_a = sa.moving_band(dur, centre_a, 0.12, rng)
    whistle_b = sa.moving_band(dur, centre_b, 0.12, rng)
    level = sa.env(dur, (0, 0), (0.25, 1), (1.1, 0.6), (dur, 0))
    x = sa.mix(0.6 * sa.norm(gust), 0.35 * whistle_a * level, 0.3 * whistle_b * level)
    return _clean(x, "cast", 0.6, 0.13, 9000)


def tailwind(v, rng):
    dur = 1.15
    rush = sa.moving_band(dur, [(0, 600), (0.35, 2800), (dur, 700)], 1.0, rng) * sa.env(dur, (0, 0), (0.35, 1), (dur, 0))
    whistle = sa.moving_band(dur, [(0, 4200), (0.3, 3600), (0.55, 1600), (dur, 1200)], 0.12, rng) * sa.env(dur, (0, 0), (0.3, 1), (0.7, 0.3), (dur, 0))
    x = sa.mix(0.8 * sa.norm(rush), 0.4 * sa.norm(whistle))
    return _clean(x, "cast", 0.55, 0.12, 9000)


def hundred_winds(v, rng):
    dur = 2.7
    howl_path = [(x, 500 + 300 * np.sin(2 * np.pi * 1.4 * x)) for x in np.linspace(0, dur, 40)]
    howl = sa.moving_band(dur, howl_path, 0.5, rng) * sa.env(dur, (0, 0), (0.3, 1), (2.2, 0.9), (dur, 0))
    roar = sa.norm(sa.lowpass(sa.brown(dur, rng), 500)) * sa.env(dur, (0, 0), (0.4, 0.8), (2.3, 0.7), (dur, 0))
    slices = []
    for t0 in sa.moments(2.2, 6, rng):
        f = rng.uniform(1800, 4200)
        slices.append((t0 + 0.15, sa.moving_band(0.18, [(0, f * 1.4), (0.18, f * 0.7)], 0.15, rng) * sa.decay(0.18, 0.05, 0.002) * rng.uniform(0.5, 1.0)))
    x = sa.mix(0.6 * sa.norm(howl), 0.5 * roar, *[(t, 0.35 * s) for t, s in slices])
    return _clean(x, "grand", 0.7, 0.14, 8000)


# ================================================================ Stone

def _rubble(dur, rate, rng, shape=None):
    """Stones knocking and grinding: grit in the band small speakers carry, so stone reads on anything."""
    grit = sa.norm(sa.bandpass(sa.grains(dur, rate, rng, length=(0.003, 0.014), shape=shape), 350, 2600))
    knocks = sa.norm(sa.bandpass(sa.grains(dur, rate * 0.3, rng, length=(0.01, 0.03), shape=shape), 250, 1200))
    return sa.mix(0.7 * grit, 0.6 * knocks)


def rockbreaker(v, rng):
    dur = 0.85
    crunch = sa.thump(110, 45, 0.45, 0.1, drive=2.2, knock=0.5)
    split = sa.norm(sa.bandpass(sa.noise(0.25, rng), 300, 2400)) * sa.decay(0.25, 0.045, 0.0008)
    crack = sa.norm(sa.bandpass(sa.noise(0.1, rng), 800, 3500)) * sa.decay(0.1, 0.015, 0.0004)
    x = sa.mix(0.5 * sa.highpass(crunch, 80), 0.8 * split, 0.5 * crack, (0.04, 0.5 * _rubble(0.6, 90, rng, [(0, 1), (0.6, 0)])))
    return _clean(x, "cast", 0.45, 0.12, 6000)


def avalanche(v, rng):
    dur = 1.5
    slam = sa.thump(95, 38, 0.6, 0.14, drive=2.3, knock=0.5)
    crash = sa.norm(sa.bandpass(sa.noise(0.4, rng), 300, 2200)) * sa.decay(0.4, 0.09, 0.002)
    rumble = sa.norm(sa.bandpass(sa.brown(1.4, rng), 100, 700)) * sa.env(1.4, (0, 0), (0.1, 1), (1.4, 0))
    tumble = 0.55 + 0.45 * np.abs(np.sin(2 * np.pi * sa.timeline(1.4) * 5.5))
    rocks = _rubble(1.3, 80, rng, [(0, 1), (1.3, 0.1)])
    x = sa.mix(0.5 * sa.highpass(slam, 80), 0.7 * crash, (0.05, 0.4 * rumble * tumble), (0.06, 0.6 * rocks))
    return _clean(x, "cast", 0.6, 0.13, 5500)


def unmoved(v, rng):
    dur = 1.2
    note = sa.partials(sa.note(D, -1), dur, ((1.0, 1.0, 1.0), (2.0, 0.6, 0.6), (2.76, 0.4, 0.4), (4.1, 0.25, 0.25), (5.4, 0.12, 0.2)), 0.35)
    knock = sa.thump(110, 60, 0.35, 0.08, drive=1.8, knock=0.5)
    clack = sa.norm(sa.bandpass(sa.noise(0.08, rng), 400, 1800)) * sa.decay(0.08, 0.02, 0.0005)
    grind = sa.norm(sa.bandpass(sa.noise(0.7, rng), 300, 1600)) * sa.swell(0.7, 0.3) * (0.6 + 0.4 * sa.chopper(0.7, rng, (20, 50), 0.3))
    x = sa.mix(0.55 * note, 0.45 * sa.highpass(knock, 80), 0.5 * clack, (0.3, 0.45 * grind))
    return _clean(x, "cast", 0.6, 0.14, 5000)


def landslide(v, rng):
    dur = 1.4
    build = sa.norm(sa.bandpass(sa.brown(1.1, rng), 90, 800)) * sa.env(1.1, (0, 0.2), (0.95, 1), (1.1, 0.6))
    rocks = _rubble(1.1, 90, rng, [(0, 0.3), (1.0, 1)])
    crash = sa.thump(100, 40, 0.45, 0.11, drive=2.2, knock=0.5)
    crack = sa.norm(sa.bandpass(sa.noise(0.25, rng), 300, 2600)) * sa.decay(0.25, 0.05, 0.0008)
    x = sa.mix(0.45 * build, 0.55 * rocks, (0.95, 0.5 * sa.highpass(crash, 80)), (0.95, 0.7 * crack))
    return _clean(x, "cast", 0.55, 0.13, 5500)


def mountain_splitter(v, rng):
    dur = 2.4
    split = sa.norm(sa.bandpass(sa.noise(0.35, rng), 400, 5000)) * sa.decay(0.35, 0.06, 0.0005)
    boom = sa.thump(85, 34, 0.9, 0.22, drive=2.4, knock=0.5)
    rumble = sa.norm(sa.bandpass(sa.brown(2.2, rng), 90, 700)) * sa.env(2.2, (0, 0), (0.2, 1), (2.2, 0))
    steps = []
    for k in range(9):
        t0 = 0.22 + k * 0.16
        rise = sa.mix(sa.highpass(sa.thump(130 - 5 * k, 60, 0.22, 0.05, drive=1.8, knock=0.5), 80),
                      0.8 * sa.norm(sa.bandpass(sa.noise(0.12, rng), 300, 1800)) * sa.decay(0.12, 0.025, 0.0006))
        steps.append((t0, rise * (0.5 + 0.05 * k)))
    x = sa.mix(0.8 * split, 0.45 * sa.highpass(boom, 70), (0.1, 0.35 * rumble), *steps, (0.2, 0.5 * _rubble(1.8, 90, rng)))
    return _clean(x, "grand", 0.7, 0.14, 5500)


# ================================================================ the events

_ARTS = [
    ("kindling_draw", kindling_draw, "cast", 24), ("rising_cinders", rising_cinders, "cast", 24), ("backdraft", backdraft, "cast", 24),
    ("wildfire_rush", wildfire_rush, "cast", 24), ("sunfall", sunfall, "cast", 32), ("sunfall_impact", sunfall_impact, "hit", 32),
    ("frostbite", frostbite, "cast", 24), ("hailfall", hailfall, "cast", 24), ("glacier_mirror", glacier_mirror, "cast", 24),
    ("skate", skate, "cast", 24), ("winters_hush", winters_hush, "cast", 32), ("winters_hush_shatter", winters_hush_shatter, "hit", 32),
    ("crackle", crackle, "cast", 24), ("skyfall", skyfall, "cast", 32), ("static_riposte", static_riposte, "cast", 24),
    ("bolt_step", bolt_step, "cast", 24), ("heavens_spear", heavens_spear, "cast", 40),
    ("cutting_breeze", cutting_breeze, "cast", 24), ("updraft", updraft, "cast", 24), ("eye_of_the_storm", eye_of_the_storm, "cast", 24),
    ("tailwind", tailwind, "cast", 24), ("hundred_winds", hundred_winds, "cast", 32),
    ("rockbreaker", rockbreaker, "cast", 24), ("avalanche", avalanche, "cast", 24), ("unmoved", unmoved, "cast", 24),
    ("landslide", landslide, "cast", 24), ("mountain_splitter", mountain_splitter, "cast", 40),
]

#: Each art's own voice, as aura.arts names them (aura_art_<art>).
ART_EVENTS = [event(f"aura_art_{name}", build, variants=1, role="cast", subtitle=subtitle, attenuation=reach)
              for name, build, subtitle, reach in _ARTS]
