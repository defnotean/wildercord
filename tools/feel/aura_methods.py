"""Aura's sound palette by breathing method: each of the ten methods has its own family of a blade's swing, a blow landing and
a technique loosed, so a listener could tell an Ember swordsman from a Rime one with their eyes shut. Not a part of its own:
tools/feel/aura.py adds these to its events (they live in the aura folder, named aura_<method>_swing, _impact and _art).

    ember      a roaring whoosh that crackles; a fiery thump and a spray of embers; a rising roar that blooms
    rime       a cold whistle with glassy shimmer; an icy crack and tinkling shards; a glass arpeggio that shatters
    thunder    an electric zip; a sharp snap with a buzz and a rumble; a thunderclap rolling away
    gale       a long airy swoosh with a whistle; a soft gust of a punch; two whistling winds swirling up
    stone      a heavy, low whoom with grit; a deep crunch and gravel; a rumbling slam and falling debris
    verdant    a rustling whoosh, warm under it; a woody knock, leaves and a soft chime; a chord blooming open
    hollow     a whoosh drawn backwards into a drone; a muffled implosion; a deep warp that falls into a hollow boom
    starlit    a whoosh glittering with pings; a bright bell struck with sparkle; a cascade of rising chimes
    hourglass  a whoosh ticking like clockwork; a clock bell's ding and a tick; a reversed swell into a ringing bell, ticking faster
    crimson    a heavy, wet swish; a thick slash over a pulse; a heartbeat, then a dark swell
    steel      plain steel and light, for a method without sounds of its own (an add-on's)

Swings are heard on every coated swing, so they're short and sit back ("effect"); impacts are punchy ("impact"); techniques
are a little longer ("cast"). Everything tonal is in D pentatonic like the rest of the mod, so it harmonises with whatever
else is playing.
"""
import numpy as np

from feel.core import sa, event

D, E, FS, A, B = sa.D, sa.E, sa.FS, sa.A, sa.B


def _clean(x, role, verb=0.3, wet=0.1, damp=7000.0):
    return sa.finish(sa.highpass(sa.reverb(x, verb, wet, damp=damp), 60), role)


def _fit(x, n):
    """`x` cut or padded to `n` samples."""
    x = np.asarray(x, dtype=float)
    return x[:n] if len(x) >= n else np.concatenate([x, np.zeros(n - len(x))])


def _whoosh(dur, path, width, rng, peak=0.3):
    """A blade through the air: noise through a moving band, rising to its loudest a third of the way through."""
    return sa.moving_band(dur, path, width, rng) * sa.env(dur, (0, 0), (dur * peak, 1), (dur * 0.75, 0.3), (dur, 0))


# ================================================================ ember: fire

def ember_swing(v, rng):
    dur = 0.42
    air = _whoosh(dur, [(0, 500 + 60 * v), (0.13, 2400), (dur, 850)], 1.0, rng)
    fwoomp = sa.norm(sa.lowpass(sa.noise(dur, rng), 380)) * sa.decay(dur, 0.09, 0.01)
    crackle = sa.bandpass(sa.grains(dur, 70, rng, shape=[(0, 0.3), (0.15, 1), (dur, 0.2)]), 2200, 7000)
    x = sa.mix(0.8 * sa.norm(air), 0.45 * fwoomp, (0.04, 0.35 * sa.norm(crackle)))
    return _clean(x, "effect", 0.25, 0.08, 6500)


def ember_impact(v, rng):
    dur = 0.55
    hit = sa.thump(150 - 10 * v, 58, 0.3, 0.07, drive=1.8)
    burst = sa.norm(sa.bandpass(sa.noise(0.25, rng), 700, 4200)) * sa.decay(0.25, 0.05, 0.002)
    embers = sa.bandpass(sa.grains(dur, 120, rng, shape=[(0, 1), (dur, 0)]), 1800, 8000)
    warmth = sa.sine(sa.note(D, 0), dur) * sa.decay(dur, 0.12, 0.004)
    bite = sa.norm(sa.bandpass(sa.noise(0.1, rng), 350, 1600)) * sa.decay(0.1, 0.025, 0.001)
    x = sa.mix(0.5 * sa.highpass(hit, 90), 0.8 * burst, 0.6 * bite, (0.02, 0.5 * sa.norm(embers)), 0.25 * warmth)
    return _clean(x, "impact", 0.35, 0.1, 6000)


def ember_art(v, rng):
    dur = 1.15
    roar = sa.norm(sa.lowpass(sa.brown(dur, rng), 900)) * sa.env(dur, (0, 0), (0.35, 1), (0.6, 0.7), (dur, 0))
    rise = sa.moving_band(dur, [(0, 300), (0.4, 3200), (dur, 1200)], 0.9, rng) * sa.env(dur, (0, 0), (0.38, 1), (dur, 0))
    crackle = sa.bandpass(sa.grains(dur, 90, rng, shape=[(0, 0.2), (0.4, 1), (dur, 0.3)]), 2000, 8000)
    bloom = sa.mix((0.36, sa.bell(sa.note(D, 0), 0.8, 0.35, 2.0, 1.4)), (0.4, 0.7 * sa.bell(sa.note(A, 0), 0.75, 0.32, 2.0, 1.4)))
    x = sa.mix(0.55 * roar, 0.6 * sa.norm(rise), 0.3 * sa.norm(crackle), 0.45 * sa.saturate(bloom, 1.6))
    return _clean(x, "cast", 0.6, 0.14, 6000)


# ================================================================ rime: frost

def rime_swing(v, rng):
    dur = 0.4
    whistle = _whoosh(dur, [(0, 2000 + 200 * v), (0.12, 4600), (dur, 2600)], 0.22, rng)
    air = _whoosh(dur, [(0, 900), (0.12, 2600), (dur, 1300)], 0.8, rng)
    # Frost crackling along the edge: a crisp, dry fizz rather than a glitter.
    frost = sa.bandpass(sa.grains(dur, 220, rng, length=(0.0004, 0.0015), shape=[(0, 0.1), (0.1, 1), (dur, 0.3)]), 2500, 7000)
    x = sa.mix(0.55 * sa.norm(whistle), 0.4 * sa.norm(air), (0.02, 0.3 * sa.norm(frost)))
    return _clean(x, "effect", 0.35, 0.12, 8000)


def rime_impact(v, rng):
    dur = 0.6
    crack = sa.shatter(rng, count=18, band=(3000, 9000), spread=0.1, tau=(0.02, 0.09))
    ping = sa.glass(sa.note((B, FS, A)[v], 2), dur, 0.16)
    knock = sa.thump(220, 120, 0.12, 0.025, drive=1.2)
    x = sa.mix(0.6 * sa.norm(crack), 0.45 * ping, 0.35 * knock)
    return _clean(x, "impact", 0.45, 0.14, 10000)


def rime_art(v, rng):
    dur = 1.1
    steps = ((D, 2), (FS, 2), (A, 2), (D, 3), (FS, 3))
    arp = sa.mix(*[(0.07 * i, (0.55 + 0.08 * i) * sa.glass(sa.note(*n), 0.7, 0.2)) for i, n in enumerate(steps)])
    wind = _whoosh(dur, [(0, 1500), (0.4, 5000), (dur, 2500)], 0.6, rng, peak=0.4)
    shatter = sa.shatter(rng, count=24, band=(2500, 9000), spread=0.18, tau=(0.03, 0.12))
    x = sa.mix(0.6 * arp, 0.35 * sa.norm(wind), (0.4, 0.5 * sa.norm(shatter)))
    return _clean(x, "cast", 0.8, 0.2, 10000)


# ================================================================ thunder: storm

def thunder_swing(v, rng):
    dur = 0.34
    zip_ = sa.soft_saw(sa.sweep(sa.note(A, -1), sa.note(A, 1) * (1 + 0.04 * v), dur, 0.6), dur, harmonics=14) * sa.chopper(dur, rng, (60, 180), 0.2)
    zip_ = _fit(zip_, sa.samples(dur)) * sa.env(dur, (0, 0), (0.08, 1), (0.25, 0.4), (dur, 0))
    air = _whoosh(dur, [(0, 1500), (0.1, 4200), (dur, 2000)], 0.6, rng)
    x = sa.mix(0.5 * sa.norm(sa.bandpass(zip_, 300, 5000)), 0.5 * sa.norm(sa.lowpass(air, 7000, order=4)))
    return _clean(x, "effect", 0.2, 0.08, 7000)


def thunder_impact(v, rng):
    dur = 0.7
    snap = sa.norm(sa.bandpass(sa.noise(0.05, rng), 2000, 10000)) * sa.decay(0.05, 0.004, 0.0002)
    buzz = sa.soft_saw(sa.note(D, -1) * (1 + 0.03 * v), 0.12, harmonics=16) * sa.chopper(0.12, rng, (90, 220), 0.1)
    buzz = _fit(buzz, sa.samples(0.12)) * sa.decay(0.12, 0.04, 0.001)
    rumble = sa.norm(sa.lowpass(sa.brown(dur, rng), 260)) * sa.env(dur, (0, 0), (0.05, 1), (dur, 0))
    x = sa.mix(0.9 * snap, 0.4 * sa.norm(buzz), (0.02, 0.5 * rumble))
    return _clean(x, "impact", 0.4, 0.12, 8000)


def thunder_art(v, rng):
    dur = 1.4
    gather = sa.soft_saw(sa.sweep(sa.note(D, -1), sa.note(D, 1), 0.3, 1.5), 0.3, harmonics=12) * sa.chopper(0.3, rng, (80, 200), 0.15)
    gather = _fit(gather, sa.samples(0.3)) * sa.swell(0.3, 0.28)
    clap = sa.norm(sa.bandpass(sa.noise(0.08, rng), 1200, 9000)) * sa.decay(0.08, 0.008, 0.0003)
    roll = sa.norm(sa.lowpass(sa.brown(dur - 0.25, rng), 320)) * sa.env(dur - 0.25, (0, 0), (0.06, 1), (0.5, 0.55), (dur - 0.25, 0))
    crackle = sa.chopper(0.5, rng, (40, 140), 0.0) * sa.norm(sa.bandpass(sa.noise(0.5, rng), 2500, 9000))
    x = sa.mix(0.45 * sa.norm(sa.bandpass(gather, 200, 5000)), (0.28, 0.95 * clap), (0.3, 0.7 * roll), (0.3, 0.25 * sa.norm(crackle)))
    return _clean(x, "cast", 0.9, 0.18, 7000)


# ================================================================ gale: wind

def gale_swing(v, rng):
    dur = 0.5
    air = _whoosh(dur, [(0, 600), (0.17, 2600 + 150 * v), (dur, 1000)], 0.9, rng, peak=0.36)
    whistle = _whoosh(dur, [(0, 1400), (0.2, 2600), (dur, 1700)], 0.12, rng, peak=0.4)
    x = sa.mix(0.85 * sa.norm(sa.lowpass(air, 6500, order=4)), 0.25 * sa.norm(whistle))
    return _clean(x, "effect", 0.3, 0.1, 7000)


def gale_impact(v, rng):
    dur = 0.5
    punch = sa.thump(140 + 8 * v, 85, 0.14, 0.03, drive=1.1, knock=0.15)
    gust = sa.moving_band(dur, [(0, 3200), (0.06, 1600), (dur, 700)], 0.9, rng) * sa.decay(dur, 0.11, 0.003)
    flutter = sa.norm(sa.bandpass(sa.noise(dur, rng), 900, 3200)) * sa.chopper(dur, rng, (18, 30), 0.3)[: sa.samples(dur)] * sa.decay(dur, 0.12, 0.01)
    x = sa.mix(0.35 * punch, 0.75 * sa.norm(gust), (0.03, 0.35 * flutter))
    return _clean(x, "impact", 0.4, 0.12, 7000)


def gale_art(v, rng):
    dur = 1.3
    howl_a = _whoosh(dur, [(0, 700), (0.6, 2400), (dur, 1300)], 0.1, rng, peak=0.55)
    howl_b = _whoosh(dur, [(0, 2200), (0.5, 1100), (dur, 2900)], 0.1, rng, peak=0.5)
    gust = _whoosh(dur, [(0, 400), (0.55, 3800), (dur, 900)], 1.0, rng, peak=0.55)
    lift = sa.sine(sa.sweep(sa.note(A, 0), sa.note(E, 2), dur, 0.8), dur) * sa.env(dur, (0, 0), (0.6, 0.6), (dur, 0))
    x = sa.mix(0.35 * sa.norm(howl_a), 0.3 * sa.norm(howl_b), 0.7 * sa.norm(gust), 0.12 * lift)
    return _clean(x, "cast", 0.8, 0.16, 8000)


# ================================================================ stone: earth

def stone_swing(v, rng):
    dur = 0.45
    whoom = _whoosh(dur, [(0, 200), (0.16, 750 - 40 * v), (dur, 260)], 1.0, rng, peak=0.36)
    body = sa.sine(sa.sweep(sa.note(D, -2), sa.note(A, -2), dur, 0.7), dur) * sa.env(dur, (0, 0), (0.16, 1), (dur, 0))
    grit = sa.lowpass(sa.grains(dur, 60, rng, length=(0.002, 0.008)), 1600)
    x = sa.mix(0.85 * sa.norm(whoom), 0.4 * body, (0.05, 0.25 * sa.norm(grit)))
    return _clean(x, "effect", 0.25, 0.08, 4000)


def stone_impact(v, rng):
    dur = 0.6
    crunch = sa.thump(110 - 6 * v, 42, 0.4, 0.09, drive=2.2, knock=0.35)
    knock = sa.norm(sa.bandpass(sa.noise(0.08, rng), 300, 1300)) * sa.decay(0.08, 0.018, 0.0005)
    gravel = sa.bandpass(sa.grains(0.4, 140, rng, length=(0.002, 0.01), shape=[(0, 1), (0.4, 0)]), 400, 3000)
    crack = sa.norm(sa.bandpass(sa.noise(0.15, rng), 250, 1100)) * sa.decay(0.15, 0.04, 0.001)
    x = sa.mix(0.55 * sa.highpass(crunch, 70), 0.7 * knock, 0.5 * crack, (0.02, 0.6 * sa.norm(gravel)))
    return _clean(x, "impact", 0.35, 0.1, 4500)


def stone_art(v, rng):
    dur = 1.3
    rumble = sa.norm(sa.lowpass(sa.brown(dur, rng), 220)) * sa.env(dur, (0, 0), (0.3, 1), (0.6, 0.6), (dur, 0))
    slam = sa.thump(90, 32, 0.9, 0.22, drive=2.4, knock=0.4)
    debris = sa.bandpass(sa.grains(0.9, 70, rng, length=(0.003, 0.012), shape=[(0, 1), (0.9, 0)]), 350, 3000)
    crash = sa.norm(sa.bandpass(sa.noise(0.5, rng), 300, 2200)) * sa.decay(0.5, 0.12, 0.002)
    drone = sa.sine(sa.note(D, -1), dur) * sa.env(dur, (0, 0), (0.3, 0.8), (dur, 0))
    x = sa.mix(0.4 * rumble, (0.28, 0.6 * sa.highpass(slam, 60)), (0.28, 0.6 * crash), (0.32, 0.55 * sa.norm(debris)), 0.2 * drone)
    return _clean(x, "cast", 0.7, 0.14, 4500)


# ================================================================ verdant: life

def verdant_swing(v, rng):
    dur = 0.42
    air = _whoosh(dur, [(0, 900), (0.14, 2300 + 120 * v), (dur, 1100)], 0.9, rng)
    rustle = sa.bandpass(sa.grains(dur, 110, rng, length=(0.001, 0.004), shape=[(0, 0.3), (0.15, 1), (dur, 0.2)]), 2000, 6500)
    warmth = sa.sine(sa.note(A, 0), dur) * sa.swell(dur, 0.18) * 0.6
    x = sa.mix(0.7 * sa.norm(air), 0.35 * sa.norm(rustle), 0.2 * warmth)
    return _clean(x, "effect", 0.3, 0.1, 7500)


def verdant_impact(v, rng):
    dur = 0.55
    wood = sa.mix(sa.sine(sa.note((A, FS, D)[v], -1) * 1.5, 0.12) * sa.decay(0.12, 0.025, 0.0005),
                  0.5 * sa.sine(sa.note(D, 0) * 1.7, 0.12) * sa.decay(0.12, 0.012, 0.0005))
    leaves = sa.bandpass(sa.grains(0.35, 200, rng, length=(0.001, 0.003), shape=[(0, 1), (0.35, 0)]), 2500, 8000)
    chime = sa.glass(sa.note(FS, 2), dur, 0.14)
    x = sa.mix(0.8 * sa.norm(wood), (0.01, 0.4 * sa.norm(leaves)), (0.02, 0.35 * chime))
    return _clean(x, "impact", 0.4, 0.12, 8000)


def verdant_art(v, rng):
    dur = 1.25
    chord = sa.mix(*[(0.05 * i, (1 - 0.12 * i) * sa.bell(sa.note(*n), 1.0, 0.5, 2.0, 1.0, attack=0.06))
                     for i, n in enumerate(((D, 0), (FS, 0), (A, 0), (D, 1)))])
    breath = sa.moving_band(dur, [(0, 600), (0.5, 2400), (dur, 1000)], 0.8, rng) * sa.swell(dur, 0.45)
    rustle = sa.bandpass(sa.grains(dur, 80, rng, length=(0.001, 0.004), shape=[(0, 0.2), (0.4, 1), (dur, 0)]), 2000, 7000)
    x = sa.mix(0.6 * sa.norm(chord), 0.35 * sa.norm(breath), 0.25 * sa.norm(rustle))
    return _clean(x, "cast", 0.8, 0.16, 8000)


# ================================================================ hollow: void

def hollow_swing(v, rng):
    dur = 0.44
    pull = sa.reverse(_whoosh(dur, [(0, 2600), (0.15, 900), (dur, 400)], 0.9, rng))
    drone = sa.sine(sa.note(D, -1) * (1 + 0.02 * v), dur) * sa.swell(dur, 0.38) + 0.5 * sa.sine(sa.note(D, -1) * 1.007, dur) * sa.swell(dur, 0.38)
    x = sa.mix(0.75 * sa.norm(pull), 0.4 * sa.norm(drone))
    return _clean(x, "effect", 0.4, 0.14, 4500)


def hollow_impact(v, rng):
    dur = 0.6
    suck = sa.reverse(sa.norm(sa.bandpass(sa.noise(0.12, rng), 500, 3500)) * sa.decay(0.12, 0.04, 0.002))
    boom = sa.thump(95 - 5 * v, 34, 0.45, 0.12, drive=1.6, knock=0.05)
    drop = sa.sine(sa.sweep(sa.note(A, 0), sa.note(D, -1), 0.4, 0.5), 0.4) * sa.decay(0.4, 0.14, 0.002)
    thud = sa.norm(sa.bandpass(sa.noise(0.12, rng), 250, 900)) * sa.decay(0.12, 0.035, 0.001)
    x = sa.mix(0.6 * suck, (0.11, 0.5 * sa.lowpass(sa.highpass(boom, 60), 1200)), (0.11, 0.6 * thud), (0.11, 0.45 * drop))
    return _clean(x, "impact", 0.6, 0.16, 3500)


def hollow_art(v, rng):
    dur = 1.4
    fall = sa.soft_saw(sa.sweep(sa.note(A, 0), sa.note(D, -2), 0.9, 0.7), 0.9, harmonics=8) * sa.env(0.9, (0, 0), (0.2, 1), (0.9, 0.3))
    warp = sa.reverse(sa.moving_band(0.7, [(0, 3500), (0.7, 500)], 0.8, rng) * sa.decay(0.7, 0.25, 0.004))
    boom = sa.thump(80, 28, 0.6, 0.2, drive=2.0, knock=0.05)
    x = sa.mix(0.45 * sa.norm(sa.lowpass(fall, 1500)), 0.5 * sa.norm(warp), (0.7, 0.85 * sa.lowpass(boom, 900)))
    return _clean(x, "cast", 1.2, 0.24, 3500)


# ================================================================ starlit: arcane

def starlit_swing(v, rng):
    dur = 0.44
    air = _whoosh(dur, [(0, 1100), (0.13, 3000 + 150 * v), (dur, 1500)], 0.8, rng)
    glitter = sa.sparkle(dur, 40, rng, [sa.note(d, 2) for d in (D, E, FS, A, B)], tau=(0.03, 0.09), shape=[(0, 0.4), (0.13, 1), (dur, 0.1)], rising=True)
    hum = sa.mix(sa.glass(sa.note(D, 1), dur, 0.12), (0.03, 0.7 * sa.glass(sa.note(A, 1), dur - 0.03, 0.12)))
    x = sa.mix(0.5 * sa.norm(air), (0.02, 0.45 * sa.norm(glitter)), 0.3 * sa.norm(hum))
    return _clean(x, "effect", 0.45, 0.16, 9000)


def starlit_impact(v, rng):
    dur = 0.7
    bell = sa.bell(sa.note((A, D, FS)[v], 1), dur, 0.25, 2.0, 2.2)
    high = sa.glass(sa.note(D, 3), dur, 0.18)
    sparkle = sa.sparkle(0.4, 50, rng, [sa.note(d, 3) for d in (FS, A, B)], tau=(0.02, 0.06), shape=[(0, 1), (0.4, 0)])
    tap = sa.thump(200, 110, 0.1, 0.02, drive=1.1)
    x = sa.mix(0.55 * bell, 0.3 * high, (0.01, 0.3 * sa.norm(sparkle)), 0.35 * tap)
    return _clean(x, "impact", 0.5, 0.16, 10500)


def starlit_art(v, rng):
    dur = 1.3
    cascade = sa.sparkle(1.0, 34, rng, [sa.note(*n) for n in ((D, 2), (FS, 2), (A, 2), (D, 3), (FS, 3), (A, 3))], tau=(0.05, 0.16),
                         shape=[(0, 0.4), (1.0, 1)], rising=True)
    chord = sa.mix((0.45, sa.bell(sa.note(D, 1), 0.85, 0.4, 2.0, 1.6)), (0.5, 0.75 * sa.bell(sa.note(A, 1), 0.8, 0.38, 2.0, 1.6)),
                   (0.55, 0.6 * sa.bell(sa.note(FS, 2), 0.75, 0.36, 2.0, 1.6)))
    swell = sa.moving_band(dur, [(0, 2000), (0.6, 7000), (dur, 4000)], 0.7, rng) * sa.swell(dur, 0.55)
    x = sa.mix(0.45 * sa.norm(cascade), 0.55 * sa.norm(chord), 0.2 * sa.norm(swell))
    return _clean(x, "cast", 0.9, 0.2, 10500)


# ================================================================ hourglass: time

def hourglass_swing(v, rng):
    dur = 0.42
    air = _whoosh(dur, [(0, 800), (0.14, 2800 + 100 * v), (dur, 1200)], 0.9, rng)
    ticks = sa.mix(*[(t, sa.tick(sa.note(A, 2) * (1 + 0.12 * i), rng, 0.8 - 0.15 * i)) for i, t in enumerate((0.03, 0.12, 0.2))])
    x = sa.mix(0.7 * sa.norm(air), 0.35 * sa.norm(ticks))
    return _clean(x, "effect", 0.3, 0.1, 8000)


def hourglass_impact(v, rng):
    dur = 0.8
    ding = sa.clock_bell(sa.note((D, A, FS)[v], 1), dur, 0.28)
    tick = sa.tick(sa.note(D, 3), rng, 1.0)
    knock = sa.thump(180, 100, 0.12, 0.025, drive=1.2)
    x = sa.mix(0.6 * ding, 0.35 * sa.norm(tick), 0.4 * knock, (0.18, 0.25 * sa.norm(sa.tick(sa.note(A, 2), rng, 0.8))))
    return _clean(x, "impact", 0.5, 0.15, 9000)


def hourglass_art(v, rng):
    dur = 1.4
    rewind = sa.reverse(sa.clock_bell(sa.note(A, 1), 0.55, 0.2)) * 0.8
    bell = sa.clock_bell(sa.note(D, 1), 0.9, 0.38)
    # Ticks coming faster and faster, as time gathers.
    times = []
    t = 0.0
    gap = 0.16
    while t < 0.55:
        times.append(t)
        t += gap
        gap *= 0.78
    ticks = sa.mix(*[(tt, sa.tick(sa.note(A, 2) * (1 + 0.05 * i), rng, 0.6 + 0.04 * i)) for i, tt in enumerate(times)])
    x = sa.mix(0.45 * sa.norm(rewind), 0.4 * sa.norm(ticks), (0.55, 0.7 * bell), (0.6, 0.3 * sa.glass(sa.note(D, 3), 0.7, 0.25)))
    return _clean(x, "cast", 0.9, 0.18, 9000)


# ================================================================ crimson: blood

def crimson_swing(v, rng):
    dur = 0.44
    swish = _whoosh(dur, [(0, 450), (0.15, 1300 + 60 * v), (dur, 500)], 0.9, rng, peak=0.34)
    wet = sa.norm(sa.lowpass(sa.noise(dur, rng), 600)) * sa.env(dur, (0, 0), (0.12, 1), (dur, 0))
    whum = (sa.sine(sa.note(D, -1), dur) + 0.5 * sa.sine(sa.note(D, 0) * 1.5, dur)) * sa.swell(dur, 0.16)
    x = sa.mix(0.75 * sa.saturate(sa.norm(swish), 1.6), 0.35 * wet, 0.35 * sa.norm(whum))
    return _clean(x, "effect", 0.3, 0.1, 5000)


def crimson_impact(v, rng):
    dur = 0.55
    slash = sa.norm(sa.bandpass(sa.noise(0.18, rng), 450, 3200)) * sa.decay(0.18, 0.04, 0.002)
    pulse = sa.thump(72 + 4 * v, 40, 0.35, 0.09, drive=1.7, knock=0.08)
    dark = sa.sine(sa.note(D, 0), dur) * sa.decay(dur, 0.14, 0.004) + 0.4 * sa.sine(sa.note(A, -1), dur) * sa.decay(dur, 0.12, 0.004)
    flesh = sa.norm(sa.bandpass(sa.noise(0.14, rng), 280, 1200)) * sa.decay(0.14, 0.045, 0.001)
    x = sa.mix(0.9 * slash, 0.55 * flesh, 0.5 * sa.highpass(pulse, 60), 0.2 * dark)
    return _clean(x, "impact", 0.35, 0.1, 5500)


def crimson_art(v, rng):
    dur = 1.3
    lub = sa.thump(68, 38, 0.3, 0.08, drive=1.8, knock=0.05)
    dub = sa.thump(76, 40, 0.3, 0.07, drive=1.8, knock=0.05)
    swell = (sa.soft_saw(sa.note(D, -1), 0.9, harmonics=10) + 0.6 * sa.soft_saw(sa.note(A, -1) * 1.004, 0.9, harmonics=10))
    swell = sa.lowpass(swell, 1100) * sa.swell(0.9, 0.55)
    wet = sa.moving_band(0.6, [(0, 500), (0.3, 1800), (0.6, 600)], 1.0, rng) * sa.swell(0.6, 0.35)
    x = sa.mix(0.8 * lub, (0.2, 0.75 * dub), (0.38, 0.55 * sa.norm(swell)), (0.6, 0.35 * sa.norm(wet)))
    return _clean(x, "cast", 0.8, 0.16, 5000)


# ================================================================ steel: no element

def steel_swing(v, rng):
    dur = 0.38
    air = _whoosh(dur, [(0, 900), (0.12, 3000 + 150 * v), (dur, 1300)], 0.9, rng)
    ring = sa.glass(sa.note(D, 2), dur, 0.06)
    x = sa.mix(0.85 * sa.norm(air), (0.05, 0.15 * ring))
    return _clean(x, "effect", 0.25, 0.08, 8000)


def steel_impact(v, rng):
    dur = 0.6
    clang = sa.partials(sa.note((A, D, FS)[v], 1), dur, ((1.0, 1.0, 1.0), (2.41, 0.6, 0.55), (3.9, 0.4, 0.35), (6.2, 0.2, 0.2)), 0.12)
    knock = sa.norm(sa.bandpass(sa.noise(0.06, rng), 1200, 6000)) * sa.decay(0.06, 0.01, 0.0004)
    x = sa.mix(0.55 * clang, 0.55 * knock, 0.35 * sa.thump(160, 90, 0.12, 0.03, drive=1.2))
    return _clean(x, "impact", 0.4, 0.12, 9000)


def steel_art(v, rng):
    dur = 1.0
    air = _whoosh(dur, [(0, 700), (0.3, 3500), (dur, 1200)], 0.8, rng, peak=0.3)
    ring = sa.partials(sa.note(D, 1), dur, ((1.0, 1.0, 1.0), (2.41, 0.6, 0.55), (3.9, 0.35, 0.35)), 0.3)
    chime = sa.glass(sa.note(A, 2), 0.8, 0.25)
    x = sa.mix(0.6 * sa.norm(air), (0.2, 0.45 * ring), (0.24, 0.3 * chime))
    return _clean(x, "cast", 0.6, 0.14, 9000)


# ================================================================ the events

_FAMILIES = {
    "ember": (ember_swing, ember_impact, ember_art),
    "rime": (rime_swing, rime_impact, rime_art),
    "thunder": (thunder_swing, thunder_impact, thunder_art),
    "gale": (gale_swing, gale_impact, gale_art),
    "stone": (stone_swing, stone_impact, stone_art),
    "verdant": (verdant_swing, verdant_impact, verdant_art),
    "hollow": (hollow_swing, hollow_impact, hollow_art),
    "starlit": (starlit_swing, starlit_impact, starlit_art),
    "hourglass": (hourglass_swing, hourglass_impact, hourglass_art),
    "crimson": (crimson_swing, crimson_impact, crimson_art),
    "steel": (steel_swing, steel_impact, steel_art),
}

#: The methods' sound families, as aura.AuraFx.SoundFamily names them (aura_<method>_swing / _impact / _art).
METHOD_EVENTS = []
for _name, (_swing, _impact, _art) in _FAMILIES.items():
    METHOD_EVENTS.append(event(f"aura_{_name}_swing", _swing, variants=3, role="effect", subtitle="cast"))
    METHOD_EVENTS.append(event(f"aura_{_name}_impact", _impact, variants=3, role="impact", subtitle="hit"))
    METHOD_EVENTS.append(event(f"aura_{_name}_art", _art, variants=1, role="cast", subtitle="cast", attenuation=24))
