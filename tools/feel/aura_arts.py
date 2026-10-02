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

  Verdant (rustle, wood and blooming chords)
    thorn_lash          a whip of vine cracking out, then roots creaking as they close
    blossom_fall        an airy fall, a soft whump, and a chord blooming open under rustling petals
    rooted_parry        a woody knock, roots cracking up out of the ground, a warm chime climbing
    wild_growth         a rustling rush, brambles snapping up one after another behind it
    groves_heart        a deep thud of the blade planted, growth creaking up, a great chord in flower

  Hollow (pulled backwards, drones, implosions)
    void_cut            air sucked in backwards, a hollow implosion, a short warp falling
    collapse            a drone sliding down as the suction grows, then a collapse into a hollow boom
    null_parry          a gulp swallowing the blow, then a high whine cut off into silence
    rift_step           a tear unzipping, a pop of pressure, a cut
    event_horizon       a sub drone swelling, suction rising round a slow warp
    event_horizon_crush a crushing boom falling in on itself, and a long hollow ring

  Starlit (glitter, bells and rising chimes)
    star_needle         a bright pluck, three quick chimes darting away, a shimmer
    meteor_shower       whistles falling out of the sky and chiming as they land, a bell at the last
    constellation_guard a clear ring of the parry, then four star notes set one by one
    comet_dash          a streak of air trailing glitter
    comet_dash_burst    a string of bright bursts racing away, climbing
    nova                glitter gathering and climbing, then a great bloom of bells and sparkle

  Hourglass (ticks, clock bells, time run backward)
    echo_cut            a cut, a tick and a tock, and the cut again through a reversed swell
    rewind_leap         a falling cut, then a warble rewinding up to a soft bell
    stopped_moment      a sharp tick, and everything held in one frozen ringing note
    blur                a rush slowed like stretched tape, a clock ticking slower and slower
    thousand_moments    ticking that stops dead, a frozen shimmer hanging in the air
    thousand_moments_release a hundred cuts landing at once over a great clock bell

  Crimson (heartbeat, wet and dark swells)
    bloodletting        a thick wet slash over a pulse, then drops falling heavily
    red_rain            a wet fall, a heavy splash, then a patter of rain over a slow heartbeat
    sanguine_parry      a clash smothered into a deep heartbeat, then a wet draw inward
    frenzy              a heartbeat racing faster and faster under a wet rush
    crimson_moon        a heartbeat (the price), a dark swell rising, a great wet arc and a long drink

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


# ================================================================ Verdant

def _rustle(dur, rate, rng, shape=None, lo=2000, hi=7000):
    """Leaves: tiny dry grains, bright."""
    return sa.norm(sa.bandpass(sa.grains(dur, rate, rng, length=(0.001, 0.004), shape=shape), lo, hi))


def _creak(dur, f0, f1, rng, rough=0.35):
    """Wood or root bending: a low buzz sliding in pitch, chopped like grain under strain."""
    tone = sa.soft_saw(sa.sweep(f0, f1, dur, 0.6), harmonics=8) * sa.chopper(dur, rng, (25, 70), rough)
    return sa.norm(sa.bandpass(tone, 180, 2400))


def _woodpop(rng, f=None):
    """A stalk snapping up: a short woody knock and a crack."""
    f = f or rng.uniform(380, 620)
    knock = sa.sine(sa.sweep(f, f * 0.6, 0.08, 0.5), 0.08) * sa.decay(0.08, 0.014, 0.0005)
    crack = sa.norm(sa.bandpass(sa.noise(0.05, rng), 1200, 5000)) * sa.decay(0.05, 0.006, 0.0003)
    return sa.mix(0.8 * knock, 0.5 * crack)


def thorn_lash(v, rng):
    dur = 1.05
    swish = sa.moving_band(0.22, [(0, 900), (0.18, 5200), (0.22, 3000)], 0.6, rng) * sa.env(0.22, (0, 0), (0.17, 1), (0.22, 0.2))
    crack = sa.norm(sa.highpass(sa.noise(0.06, rng), 1500)) * sa.decay(0.06, 0.007, 0.0002)
    creak = _creak(0.7, 160, 110, rng) * sa.env(0.7, (0, 0), (0.12, 1), (0.7, 0))
    leaves = _rustle(0.8, 120, rng, [(0, 1), (0.8, 0.1)])
    x = sa.mix(0.55 * sa.norm(swish), (0.19, 0.9 * crack), (0.24, 0.45 * creak), (0.2, 0.3 * leaves))
    return _clean(x, "cast", 0.45, 0.12, 8000)


def blossom_fall(v, rng):
    dur = 1.6
    fall = sa.moving_band(0.45, [(0, 3600), (0.45, 900)], 0.7, rng) * sa.env(0.45, (0, 0), (0.12, 1), (0.45, 0.3))
    whump = sa.thump(120, 60, 0.3, 0.07, drive=1.3, knock=0.1)
    bloom = sa.mix(*[(0.05 * i, (1 - 0.14 * i) * sa.bell(sa.note(*n), 1.1, 0.55, 2.0, 0.9, attack=0.05))
                     for i, n in enumerate(((D, 1), (FS, 1), (A, 1), (D, 2)))])
    petals = _rustle(1.1, 90, rng, [(0, 1), (1.1, 0.2)], 2500, 8000)
    x = sa.mix(0.5 * sa.norm(fall), (0.42, 0.45 * sa.highpass(whump, 60)), (0.45, 0.6 * sa.norm(bloom)), (0.45, 0.3 * petals))
    return _clean(x, "cast", 0.8, 0.16, 8500)


def rooted_parry(v, rng):
    dur = 1.4
    knock = sa.mix(sa.sine(sa.note(A, -1) * 1.5, 0.15) * sa.decay(0.15, 0.03, 0.0005), 0.5 * sa.sine(sa.note(D, 0) * 1.7, 0.15) * sa.decay(0.15, 0.015, 0.0005))
    roots = sa.mix(*[(0.08 + k * 0.07 + rng.uniform(-0.02, 0.02), 0.7 * _woodpop(rng, 260 + 40 * k)) for k in range(6)])
    creak = _creak(0.5, 120, 180, rng) * sa.swell(0.5, 0.3)
    chime = sa.mix(sa.glass(sa.note(A, 1), 0.7, 0.25), (0.12, 0.8 * sa.glass(sa.note(D, 2), 0.7, 0.28)), (0.24, 0.6 * sa.glass(sa.note(FS, 2), 0.6, 0.25)))
    x = sa.mix(0.8 * sa.norm(knock), (0.05, 0.5 * roots), (0.08, 0.3 * creak), (0.55, 0.45 * sa.norm(chime)))
    return _clean(x, "cast", 0.6, 0.15, 8000)


def wild_growth(v, rng):
    dur = 1.5
    rush = sa.moving_band(0.7, [(0, 900), (0.2, 3600), (0.7, 1100)], 0.9, rng) * sa.env(0.7, (0, 0), (0.18, 1), (0.7, 0))
    leaves = _rustle(0.9, 160, rng, [(0, 0.4), (0.2, 1), (0.9, 0.3)])
    snaps = []
    for k in range(7):
        t0 = 0.28 + k * 0.11 + rng.uniform(-0.02, 0.02)
        snaps.append((t0, (0.7 - 0.05 * k) * _woodpop(rng)))
        snaps.append((t0 + 0.01, 0.25 * _rustle(0.12, 200, rng)))
    x = sa.mix(0.7 * sa.norm(rush), 0.35 * leaves, *snaps)
    return _clean(x, "cast", 0.5, 0.12, 8000)


def groves_heart(v, rng):
    dur = 2.4
    plant = sa.thump(85, 36, 0.6, 0.15, drive=2.0, knock=0.4)
    earth = sa.norm(sa.bandpass(sa.brown(0.6, rng), 90, 700)) * sa.decay(0.6, 0.18, 0.004)
    growth = sa.mix(_creak(1.4, 90, 150, rng, 0.25), 0.6 * _creak(1.4, 135, 220, rng, 0.25)) * sa.swell(1.4, 1.0)
    chord = sa.mix(*[(0.08 * i, (1 - 0.1 * i) * sa.bell(sa.note(*n), 1.5, 0.75, 2.0, 0.9, attack=0.08))
                     for i, n in enumerate(((D, 0), (A, 0), (D, 1), (FS, 1), (A, 1)))])
    birds = sa.sparkle(1.2, 9, rng, [sa.note(d, 3) for d in (D, E, FS, A)], tau=(0.04, 0.09), shape=[(0, 0.2), (1.2, 1)])
    leaves = _rustle(1.6, 70, rng, [(0, 0.2), (0.6, 1), (1.6, 0.3)])
    x = sa.mix(0.6 * sa.highpass(plant, 40), 0.5 * earth, (0.2, 0.3 * growth), (0.9, 0.6 * sa.norm(sa.chorus(chord, voices=2))),
               (1.1, 0.2 * sa.norm(birds)), (0.6, 0.25 * leaves))
    return _clean(x, "grand", 0.9, 0.17, 8000)


# ================================================================ Hollow

def _suck(dur, lo, hi, rng, width=0.8):
    """Air drawn in backwards: a whoosh run in reverse, so it swells to a stop."""
    return sa.reverse(sa.moving_band(dur, [(0, hi), (dur, lo)], width, rng) * sa.decay(dur, dur * 0.35, 0.004))


def void_cut(v, rng):
    dur = 1.0
    pull = _suck(0.32, 500, 3200, rng)
    boom = sa.thump(90, 32, 0.45, 0.11, drive=1.7, knock=0.05)
    warp = sa.sine(sa.sweep(sa.note(A, 0), sa.note(D, -1), 0.45, 0.5), 0.45) * sa.decay(0.45, 0.16, 0.002)
    x = sa.mix(0.65 * sa.norm(pull), (0.3, 0.6 * sa.lowpass(sa.highpass(boom, 50), 1200)), (0.3, 0.4 * warp))
    return _clean(x, "cast", 1.0, 0.22, 3800)


def collapse(v, rng):
    dur = 1.8
    drone = sa.soft_saw(sa.sweep(sa.note(D, 1), sa.note(D, -1), 0.8, 0.8), 0.8, harmonics=10) * sa.swell(0.8, 0.75)
    suction = sa.reverse(sa.moving_band(0.8, [(0, 4200), (0.8, 600)], 0.9, rng) * sa.decay(0.8, 0.3, 0.004))
    boom = sa.thump(80, 34, 0.8, 0.2, drive=2.4, knock=0.3)
    crunch = sa.norm(sa.bandpass(sa.noise(0.3, rng), 300, 2200)) * sa.decay(0.3, 0.06, 0.001)
    tail = _suck(0.5, 300, 2400, rng)
    x = sa.mix(0.35 * sa.norm(sa.bandpass(drone, 200, 2000)), 0.6 * sa.norm(suction), (0.8, 0.6 * sa.highpass(boom, 60)), (0.8, 0.55 * crunch),
               (0.82, 0.35 * sa.norm(sa.reverse(tail))))
    return _clean(x, "cast", 1.1, 0.22, 4500)


def null_parry(v, rng):
    dur = 1.1
    gulp = sa.reverse(sa.norm(sa.bandpass(sa.noise(0.16, rng), 400, 2600)) * sa.decay(0.16, 0.05, 0.002))
    thud = sa.thump(110, 45, 0.3, 0.06, drive=1.4, knock=0.05)
    whine = sa.sine(sa.note(A, 3), 0.42) * sa.env(0.42, (0, 0), (0.08, 0.7), (0.38, 0.5), (0.4, 0))
    hush = sa.norm(sa.lowpass(sa.brown(0.5, rng), 500)) * sa.env(0.5, (0, 0.4), (0.5, 0))
    x = sa.mix(0.7 * gulp, (0.15, 0.55 * sa.lowpass(thud, 900)), (0.22, 0.22 * whine), (0.62, 0.25 * hush))
    return _clean(x, "cast", 1.0, 0.2, 4000)


def rift_step(v, rng):
    dur = 0.85
    tear = sa.moving_band(0.3, [(0, 500), (0.3, 4500)], 0.4, rng) * sa.chopper(0.3, rng, (60, 160), 0.2) * sa.env(0.3, (0, 0.2), (0.28, 1), (0.3, 0))
    pop = sa.thump(160, 70, 0.2, 0.04, drive=1.6, knock=0.2)
    cut = sa.moving_band(0.2, [(0, 5000), (0.2, 1800)], 0.5, rng) * sa.decay(0.2, 0.05, 0.002)
    x = sa.mix(0.6 * sa.norm(tear), (0.3, 0.6 * sa.highpass(pop, 70)), (0.36, 0.6 * sa.norm(cut)), (0.3, 0.3 * sa.norm(_suck(0.25, 400, 2000, rng))))
    return _clean(x, "cast", 0.8, 0.18, 4500)


def event_horizon(v, rng):
    dur = 2.2
    sub = (sa.sine(sa.note(D, -2), 2.0) + 0.6 * sa.sine(sa.note(D, -2) * 1.01, 2.0)) * sa.swell(2.0, 1.6)
    wobble = 0.65 + 0.35 * np.sin(2 * np.pi * sa.timeline(2.0) * (1.5 + 2.5 * sa.timeline(2.0)))
    suction = sa.reverse(sa.moving_band(2.0, [(0, 5000), (2.0, 400)], 1.0, rng) * sa.decay(2.0, 0.8, 0.004))
    whine = sa.soft_saw(sa.glide(2.0, (0, sa.note(A, 0)), (2.0, sa.note(D, 2))), harmonics=6) * sa.swell(2.0, 1.8)
    x = sa.mix(0.5 * sa.norm(sub) * wobble, 0.5 * sa.norm(suction), 0.18 * sa.norm(sa.lowpass(whine, 2500)))
    return _clean(x, "grand", 1.3, 0.25, 3500)


def event_horizon_crush(v, rng):
    dur = 2.2
    boom = sa.thump(60, 22, 1.4, 0.35, drive=2.6, knock=0.3)
    fall = _suck(0.4, 300, 3000, rng)
    crunch = sa.norm(sa.bandpass(sa.noise(0.4, rng), 200, 1800)) * sa.decay(0.4, 0.08, 0.002)
    ring = sa.partials(sa.note(D, -1), 1.8, ((1.0, 1.0, 1.0), (2.0, 0.5, 0.7), (3.01, 0.3, 0.5), (4.2, 0.18, 0.35)), 0.6)
    x = sa.mix(0.55 * sa.norm(fall), (0.36, 0.8 * sa.highpass(boom, 30)), (0.36, 0.45 * crunch), (0.4, 0.35 * sa.lowpass(ring, 1500)))
    return _clean(x, "impact", 1.3, 0.24, 3500)


# ================================================================ Starlit

def _pew(f, rng, dur=0.18):
    """A dart of starlight: a glassy chirp sweeping up, bright."""
    chirp = sa.sine(sa.sweep(f * 0.7, f * 1.25, dur, 0.4), dur) * sa.decay(dur, dur * 0.3, 0.001)
    return sa.mix(0.7 * chirp, 0.4 * sa.glass(f * 2, dur, dur * 0.25))


def star_needle(v, rng):
    dur = 1.0
    pluck = sa.mix(sa.bell(sa.note(D, 2), 0.5, 0.12, 2.0, 2.2), 0.4 * sa.thump(220, 120, 0.08, 0.02, drive=1.1))
    darts = [(0.06 + k * 0.07, (0.9 - 0.1 * k) * _pew(sa.note((A, D, FS)[k], 2 + (k > 0)), rng)) for k in range(3)]
    shimmer = sa.sparkle(0.6, 26, rng, [sa.note(d, 3) for d in (D, FS, A, B)], tau=(0.03, 0.08), shape=[(0, 1), (0.6, 0.1)])
    x = sa.mix(0.5 * pluck, *darts, (0.18, 0.3 * sa.norm(shimmer)))
    return _clean(x, "cast", 0.7, 0.17, 10500)


def meteor_shower(v, rng):
    dur = 2.0
    layers = []
    for k in range(5):
        t0 = 0.08 + k * 0.17 + rng.uniform(-0.02, 0.02)
        f = sa.note((A, FS, D, B, E)[k], 3)
        whistle = sa.sine(sa.sweep(f, f * 0.55, 0.22, 0.6), 0.22) * sa.env(0.22, (0, 0), (0.05, 0.6), (0.22, 0.3))
        layers.append((t0, 0.3 * whistle))
        layers.append((t0 + 0.2, 0.55 * sa.mix(sa.glass(sa.note((D, A, FS, D, A)[k], 2 + (k % 2)), 0.5, 0.12),
                                                0.35 * sa.thump(200, 110, 0.08, 0.02, drive=1.1))))
    great = sa.mix(sa.bell(sa.note(D, 1), 1.0, 0.45, 2.0, 1.8), 0.6 * sa.bell(sa.note(A, 1), 0.9, 0.4, 2.0, 1.8), 0.4 * sa.thump(120, 60, 0.3, 0.07, drive=1.4))
    glitter = sa.sparkle(0.8, 20, rng, [sa.note(d, 3) for d in (D, FS, A)], tau=(0.04, 0.12), shape=[(0, 1), (0.8, 0.1)])
    x = sa.mix(*layers, (1.05, 0.7 * great), (1.05, 0.25 * sa.norm(glitter)))
    return _clean(x, "cast", 0.9, 0.18, 10500)


def constellation_guard(v, rng):
    dur = 1.6
    ring = sa.mix(sa.bell(sa.note(A, 1), 1.0, 0.35, 2.0, 2.4), 0.5 * sa.glass(sa.note(A, 2), 0.8, 0.3), 0.3 * sa.thump(240, 140, 0.08, 0.02, drive=1.2))
    stars = [(0.32 + k * 0.13, (0.5 + 0.05 * k) * sa.glass(sa.note(*n), 0.7, 0.3)) for k, n in enumerate(((D, 2), (FS, 2), (A, 2), (D, 3)))]
    shimmer = sa.sparkle(0.9, 14, rng, [sa.note(d, 3) for d in (FS, A, B)], tau=(0.05, 0.12), shape=[(0, 0.3), (0.9, 1)])
    x = sa.mix(0.6 * ring, *stars, (0.4, 0.2 * sa.norm(shimmer)))
    return _clean(x, "cast", 0.9, 0.18, 10500)


def comet_dash(v, rng):
    dur = 1.0
    streak = sa.moving_band(dur, [(0, 900), (0.18, 3600), (dur, 1300)], 0.7, rng) * sa.env(dur, (0, 0), (0.15, 1), (0.6, 0.3), (dur, 0))
    glitter = sa.sparkle(0.85, 40, rng, [sa.note(d, 2) for d in (D, E, FS, A, B)], tau=(0.02, 0.06), shape=[(0, 0.3), (0.2, 1), (0.85, 0.4)], rising=True)
    x = sa.mix(0.65 * sa.norm(streak), (0.05, 0.35 * sa.norm(glitter)))
    return _clean(x, "cast", 0.7, 0.16, 8000)


def comet_dash_burst(v, rng):
    dur = 1.0
    pops = []
    for k in range(7):
        t0 = k * 0.06
        pop = sa.mix(sa.glass(sa.note((D, E, FS, A, B, D, FS)[k], 2 + (k >= 5)), 0.3, 0.07), 0.45 * sa.thump(260 + 20 * k, 150, 0.07, 0.015, drive=1.3, knock=0.3))
        pops.append((t0, (0.6 + 0.04 * k) * pop))
    crackle = sa.norm(sa.bandpass(sa.grains(0.5, 120, rng, shape=[(0, 1), (0.5, 0.2)]), 3000, 9000))
    x = sa.mix(*pops, (0.02, 0.2 * crackle))
    return _clean(x, "impact", 0.8, 0.16, 10500)


def nova(v, rng):
    dur = 2.6
    gather = sa.sparkle(0.6, 46, rng, [sa.note(*n) for n in ((D, 2), (FS, 2), (A, 2), (D, 3), (FS, 3), (A, 3))], tau=(0.03, 0.08),
                        shape=[(0, 0.3), (0.6, 1)], rising=True)
    rise = sa.moving_band(0.6, [(0, 1500), (0.6, 7500)], 0.6, rng) * sa.swell(0.6, 0.58, 0.3)
    boom = sa.thump(110, 45, 0.6, 0.14, drive=1.6, knock=0.2)
    bloom = sa.mix(sa.bell(sa.note(D, 1), 1.8, 0.8, 2.0, 1.8), (0.02, 0.8 * sa.bell(sa.note(A, 1), 1.7, 0.75, 2.0, 1.8)),
                   (0.04, 0.6 * sa.bell(sa.note(FS, 2), 1.6, 0.7, 2.0, 1.8)), (0.06, 0.45 * sa.bell(sa.note(D, 3), 1.5, 0.6, 2.0, 2.0)))
    burst = sa.sparkle(1.4, 30, rng, [sa.note(d, 3) for d in (D, E, FS, A, B)], tau=(0.05, 0.18), shape=[(0, 1), (1.4, 0.05)])
    x = sa.mix(0.4 * sa.norm(gather), 0.3 * sa.norm(rise), (0.6, 0.4 * sa.highpass(boom, 60)), (0.6, 0.6 * sa.norm(sa.chorus(bloom, voices=2))),
               (0.62, 0.35 * sa.norm(burst)))
    return _clean(x, "grand", 1.0, 0.2, 10500)


# ================================================================ Hourglass

def _cut(dur, rng, hi=4800, lo=1400):
    """A blade's cut through the air, short and clean."""
    return sa.moving_band(dur, [(0, lo), (dur * 0.3, hi), (dur, lo * 0.8)], 0.7, rng) * sa.env(dur, (0, 0), (dur * 0.3, 1), (dur, 0))


def echo_cut(v, rng):
    dur = 1.2
    first = _cut(0.22, rng)
    ticktock = sa.mix(sa.tick(sa.note(A, 2), rng, 0.9), (0.13, sa.tick(sa.note(D, 2), rng, 0.8)))
    swell = sa.reverse(sa.clock_bell(sa.note(A, 1), 0.4, 0.15)) * 0.7
    second = _cut(0.22, rng, 3600, 1100)
    x = sa.mix(0.65 * sa.norm(first), (0.2, 0.5 * sa.norm(ticktock)), (0.28, 0.35 * sa.norm(swell)), (0.6, 0.45 * sa.norm(second)),
               (0.62, 0.25 * sa.clock_bell(sa.note(D, 2), 0.5, 0.18)))
    return _clean(x, "cast", 0.8, 0.18, 9000)


def rewind_leap(v, rng):
    dur = 1.5
    fall = _cut(0.3, rng, 4200, 900)
    knock = sa.thump(150, 70, 0.2, 0.05, drive=1.3)
    rewind = sa.reverse(sa.moving_band(0.55, [(0, 3800), (0.55, 700)], 0.6, rng) * sa.decay(0.55, 0.2, 0.003))
    warble = sa.sine(sa.glide(0.55, (0, sa.note(D, 1)), (0.55, sa.note(A, 2))) * (1 + 0.04 * np.sin(2 * np.pi * 9 * sa.timeline(0.55)))) * sa.swell(0.55, 0.5)
    bell = sa.clock_bell(sa.note(D, 1), 0.7, 0.3)
    x = sa.mix(0.55 * sa.norm(fall), (0.22, 0.35 * sa.highpass(knock, 70)), (0.38, 0.45 * sa.norm(rewind)), (0.38, 0.25 * warble), (0.93, 0.55 * bell))
    return _clean(x, "cast", 0.8, 0.18, 9000)


def stopped_moment(v, rng):
    dur = 1.8
    tick = sa.tick(sa.note(D, 3), rng, 1.0)
    clack = sa.thump(220, 140, 0.1, 0.02, drive=1.2)
    held = sa.mix(sa.sine(sa.note(A, 1), 1.5), 0.6 * sa.sine(sa.note(A, 2) * 1.002, 1.5), 0.3 * sa.sine(sa.note(E, 3) * 0.998, 1.5))
    held = held * sa.env(1.5, (0, 0), (0.04, 1), (1.2, 0.85), (1.5, 0))
    sand = sa.norm(sa.bandpass(sa.noise(1.4, rng), 3000, 9000)) * sa.env(1.4, (0, 0), (0.2, 0.25), (1.4, 0))
    x = sa.mix(0.9 * sa.norm(tick), 0.4 * clack, (0.05, 0.3 * sa.norm(held)), (0.1, 0.12 * sand))
    return _clean(x, "cast", 0.9, 0.18, 9000)


def blur(v, rng):
    dur = 1.6
    rush = sa.moving_band(1.2, [(0, 1600), (0.15, 2800), (1.2, 380)], 0.9, rng) * sa.env(1.2, (0, 0), (0.12, 1), (1.2, 0))
    times = [0.0]
    gap = 0.07
    while times[-1] < 1.2:
        gap *= 1.28
        times.append(times[-1] + gap)
    ticks = sa.mix(*[(t, sa.tick(sa.note(A, 2) * (1 - 0.04 * i), rng, 0.8 - 0.06 * i)) for i, t in enumerate(times[:9])])
    x = sa.mix(0.7 * sa.norm(sa.lowpass(rush, 3500)), (0.05, 0.4 * sa.norm(ticks)))
    return _clean(x, "cast", 0.7, 0.16, 8000)


def thousand_moments(v, rng):
    dur = 2.4
    times = []
    t = 0.0
    gap = 0.13
    while t < 0.6:
        times.append(t)
        t += gap
        gap *= 0.82
    ticks = sa.mix(*[(tt, sa.tick(sa.note(A, 2) * (1 + 0.04 * i), rng, 0.6 + 0.04 * i)) for i, tt in enumerate(times)])
    stop = sa.mix(sa.tick(sa.note(D, 3), rng, 1.2), 0.5 * sa.thump(200, 120, 0.12, 0.03, drive=1.2))
    frozen = sa.mix(sa.sine(sa.note(D, 2), 1.6), 0.7 * sa.sine(sa.note(A, 2) * 1.003, 1.6), 0.4 * sa.sine(sa.note(FS, 3) * 0.997, 1.6))
    frozen = frozen * sa.env(1.6, (0, 0), (0.1, 1), (1.3, 0.8), (1.6, 0))
    shimmer = sa.sparkle(1.4, 8, rng, [sa.note(d, 3) for d in (D, FS, A)], tau=(0.1, 0.3))
    x = sa.mix(0.45 * sa.norm(ticks), (t, 0.7 * sa.norm(stop)), (t + 0.05, 0.3 * sa.norm(frozen)), (t + 0.2, 0.15 * sa.norm(shimmer)))
    return _clean(x, "grand", 1.0, 0.2, 9000)


def thousand_moments_release(v, rng):
    dur = 2.4
    cuts = []
    for k in range(18):
        t0 = rng.uniform(0, 0.12) + (k % 3) * 0.015
        cuts.append((t0, rng.uniform(0.25, 0.6) * sa.norm(_cut(rng.uniform(0.12, 0.22), rng, rng.uniform(3000, 6500), rng.uniform(900, 1800)))))
    crack = sa.norm(sa.highpass(sa.noise(0.1, rng), 800)) * sa.decay(0.1, 0.012, 0.0003)
    toll = sa.mix(sa.clock_bell(sa.note(D, 0), 2.0, 0.8), 0.6 * sa.clock_bell(sa.note(A, 0), 1.8, 0.7), 0.35 * sa.clock_bell(sa.note(D, 1), 1.6, 0.6))
    boom = sa.thump(80, 32, 0.8, 0.2, drive=2.0, knock=0.3)
    x = sa.mix(*cuts, 0.6 * crack, (0.03, 0.55 * sa.highpass(boom, 40)), (0.05, 0.6 * sa.norm(toll)))
    return _clean(x, "impact", 1.0, 0.2, 8500)


# ================================================================ Crimson

def _beat(rng, f0=68, strength=1.0):
    """One heartbeat: lub, then dub."""
    lub = sa.thump(f0, f0 * 0.55, 0.28, 0.07, drive=1.8, knock=0.05)
    dub = sa.thump(f0 * 1.12, f0 * 0.6, 0.26, 0.06, drive=1.8, knock=0.05)
    return strength * sa.mix(0.9 * lub, (0.17, 0.75 * dub))


def _wet(dur, rng, lo=380, hi=1600, peak=0.3):
    """A heavy wet swish: a low moving band, saturated."""
    swish = sa.moving_band(dur, [(0, lo), (dur * peak, hi), (dur, lo)], 0.9, rng) * sa.env(dur, (0, 0), (dur * peak, 1), (dur, 0))
    return sa.saturate(sa.norm(swish), 1.7)


def bloodletting(v, rng):
    dur = 1.3
    slash = _wet(0.3, rng, 500, 2200)
    flesh = sa.norm(sa.bandpass(sa.noise(0.15, rng), 300, 1300)) * sa.decay(0.15, 0.05, 0.001)
    drips = sa.mix(*[(0.42 + k * 0.21 + rng.uniform(-0.03, 0.03), 0.6 * sa.blip(rng)) for k in range(3)])
    x = sa.mix(0.75 * slash, (0.16, 0.6 * flesh), (0.1, 0.5 * sa.highpass(_beat(rng, 72), 50)), (0.0, 0.6 * sa.lowpass(drips, 2500)))
    return _clean(x, "cast", 0.6, 0.14, 5500)


def red_rain(v, rng):
    dur = 2.0
    fall = _wet(0.35, rng, 700, 2000, 0.6)
    splash = sa.mix(sa.norm(sa.bandpass(sa.noise(0.35, rng), 250, 2500)) * sa.decay(0.35, 0.07, 0.001), 0.5 * sa.thump(95, 40, 0.4, 0.09, drive=2.0))
    rain = sa.norm(sa.bandpass(sa.grains(1.4, 70, rng, length=(0.002, 0.006), shape=[(0, 1), (1.4, 0.3)]), 600, 3500))
    beats = sa.mix(_beat(rng, 62, 0.8), (0.8, _beat(rng, 62, 0.6)))
    x = sa.mix(0.5 * fall, (0.33, 0.8 * splash), (0.4, 0.35 * rain), (0.4, 0.45 * sa.highpass(beats, 45)))
    return _clean(x, "cast", 0.7, 0.15, 5500)


def sanguine_parry(v, rng):
    dur = 1.5
    clash = sa.lowpass(sa.mix(sa.glass(sa.note(A, 1), 0.3, 0.06), 0.6 * sa.norm(sa.bandpass(sa.noise(0.08, rng), 1500, 5000)) * sa.decay(0.08, 0.012, 0.0003)), 2500)
    beat = _beat(rng, 64, 1.0)
    draw = sa.reverse(_wet(0.4, rng, 400, 1500, 0.4)) * sa.env(0.4, (0, 1), (0.4, 1))
    x = sa.mix(0.55 * sa.norm(clash), (0.06, 0.7 * sa.highpass(beat, 45)), (0.55, 0.5 * draw))
    return _clean(x, "cast", 0.7, 0.15, 5000)


def frenzy(v, rng):
    dur = 1.5
    rush = _wet(0.9, rng, 450, 1900, 0.25)
    beats = []
    t = 0.0
    gap = 0.34
    k = 0
    while t < 1.2:
        beats.append((t, (0.55 + 0.05 * k) * sa.highpass(_beat(rng, 70 + 3 * k, 1.0), 45)))
        t += gap
        gap *= 0.74
        k += 1
    x = sa.mix(0.55 * rush, *beats)
    return _clean(x, "cast", 0.6, 0.13, 5000)


def crimson_moon(v, rng):
    dur = 2.8
    toll = _beat(rng, 58, 1.0)
    swell = sa.mix(sa.soft_saw(sa.note(D, -1), 1.2, harmonics=10), 0.7 * sa.soft_saw(sa.note(A, -1) * 1.004, 1.2, harmonics=10),
                   0.5 * sa.soft_saw(sa.note(D, 0) * 0.997, 1.2, harmonics=8))
    swell = sa.lowpass(swell, 1300) * sa.swell(1.2, 1.0, 0.6)
    arc = _wet(0.8, rng, 400, 2400, 0.35)
    drink = sa.reverse(_wet(0.9, rng, 300, 1200, 0.3)) * 0.8
    x = sa.mix(0.7 * sa.highpass(toll, 40), (0.35, 0.45 * sa.norm(sa.chorus(swell, voices=3))), (1.15, 0.8 * arc), (1.6, 0.4 * drink),
               (1.2, 0.4 * sa.highpass(_beat(rng, 60, 0.9), 40)))
    return _clean(x, "grand", 0.9, 0.17, 5000)


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
    ("thorn_lash", thorn_lash, "cast", 24), ("blossom_fall", blossom_fall, "cast", 24), ("rooted_parry", rooted_parry, "cast", 24),
    ("wild_growth", wild_growth, "cast", 24), ("groves_heart", groves_heart, "cast", 40),
    ("void_cut", void_cut, "cast", 24), ("collapse", collapse, "cast", 24), ("null_parry", null_parry, "cast", 24),
    ("rift_step", rift_step, "cast", 24), ("event_horizon", event_horizon, "cast", 40), ("event_horizon_crush", event_horizon_crush, "hit", 40),
    ("star_needle", star_needle, "cast", 24), ("meteor_shower", meteor_shower, "cast", 32), ("constellation_guard", constellation_guard, "cast", 24),
    ("comet_dash", comet_dash, "cast", 24), ("comet_dash_burst", comet_dash_burst, "hit", 24), ("nova", nova, "cast", 40),
    ("echo_cut", echo_cut, "cast", 24), ("rewind_leap", rewind_leap, "cast", 24), ("stopped_moment", stopped_moment, "cast", 24),
    ("blur", blur, "cast", 24), ("thousand_moments", thousand_moments, "cast", 40),
    ("thousand_moments_release", thousand_moments_release, "hit", 40),
    ("bloodletting", bloodletting, "cast", 24), ("red_rain", red_rain, "cast", 24), ("sanguine_parry", sanguine_parry, "cast", 24),
    ("frenzy", frenzy, "cast", 24), ("crimson_moon", crimson_moon, "cast", 40),
]

#: Each art's own voice, as aura.arts names them (aura_art_<art>).
ART_EVENTS = [event(f"aura_art_{name}", build, variants=1, role="cast", subtitle=subtitle, attenuation=reach)
              for name, build, subtitle, reach in _ARTS]
