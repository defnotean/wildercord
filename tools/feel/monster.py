"""The monsters of the wilds' own voices: a Bramblewalker's creak and lash, a Gloomstalker's purr and pounce, a harpy's
shriek, a Geode Crawler's chitter and crystal clatter, a Bog Witch-Frog's croak and gulp, a Mana Ooze's burble and the
glassy slurp of a spell going in.

Names start with 'monster_'; subtitles name the creature ('<creature>.<sound>', their text in tools/monster_art.py). The
voices are grain, noise and growl rather than notes, but what rings (crystal, the ooze's drinking) sits in D major
pentatonic like everything else, so it never clashes with the spells flying round it.
"""
import numpy as np

from feel.core import sa, event

D, E, FS, A, B = sa.D, sa.E, sa.FS, sa.A, sa.B


def fit(x, dur):
    n = sa.samples(dur)
    x = np.asarray(x, dtype=float)
    return np.pad(x, (0, max(0, n - len(x))))[:n]


def grains(dur, *args, **kwargs):
    return fit(sa.grains(dur, *args, **kwargs), dur)


def place(dur, *layers):
    """Layers laid on a `dur`-second bed: each (start, signal) or a signal at 0."""
    out = np.zeros(sa.samples(dur))
    for layer in layers:
        start, x = layer if isinstance(layer, tuple) else (0.0, layer)
        i = sa.samples(start)
        x = np.asarray(x, dtype=float)[:max(0, len(out) - i)]
        out[i:i + len(x)] += x
    return out


def growl(dur, f0, f1, rng, rasp=(18, 30), formant=(260, 1400), harmonics=14):
    """A throat's growl: a buzzing tone sliding f0 to f1, wavering, rasped by a fast flutter and shaped by a mouth."""
    t = sa.timeline(dur)
    pitch = sa.glide(dur, (0, f0), (dur, f1)) * (1 + 0.025 * np.sin(2 * np.pi * 5.5 * t) + 0.015 * sa.jitter(dur, rng, 9))
    buzz = sa.soft_saw(pitch, dur, harmonics=harmonics)
    buzz = buzz * sa.chopper(dur, rng, rasp, 0.35)
    return sa.norm(sa.bandpass(buzz, *formant))


def breath(dur, lo, hi, rng, peak=0.4):
    return sa.norm(sa.bandpass(sa.noise(dur, rng), lo, hi)) * sa.swell(dur, dur * peak, 1.5)


def creak(dur, f0, f1, rng, rate=(14, 26)):
    """Wood under strain: a stick-slip judder on a hard tone, through a woody resonance."""
    tone = sa.soft_saw(sa.glide(dur, (0, f0), (dur, f1)), dur, harmonics=10) * sa.chopper(dur, rng, rate, 0.05)
    return sa.norm(sa.bandpass(tone, 300, 2400))


def rustle(dur, rng, lo=1400, hi=4200, peak=0.5):
    return sa.moving_band(dur, [(0, lo), (dur * peak, hi), (dur, lo)], 0.9, rng) * sa.swell(dur, dur * peak, 1.4)


def knock(rng, lo=250, hi=1800, tau=0.02):
    return sa.norm(sa.bandpass(sa.noise(0.12, rng), lo, hi)) * sa.decay(0.12, tau, 0.0005)


def croak(dur, rate, f, rng, formant=(250, 1600)):
    """A frog's croak: a train of short, throaty pulses (each a tiny resonant knock), `rate` a second."""
    out = np.zeros(sa.samples(dur))
    step = 1.0 / rate
    t = 0.0
    while t < dur - 0.04:
        n = sa.samples(0.04)
        pulse = sa.soft_saw(f * (1 + rng.uniform(-0.03, 0.03)), 0.04, harmonics=8) * sa.decay(0.04, 0.012, 0.001)
        i = sa.samples(t)
        out[i:i + n] += pulse[:len(out) - i]
        t += step * rng.uniform(0.9, 1.1)
    return sa.norm(sa.bandpass(out, *formant))


def bubbles(dur, rate, rng, low=300.0, high=900.0):
    return sa.bubbles(dur, rate, rng, low, high)


def clatter(dur, rng, count=8, notes=(D, FS, A, B), octave=2, spread=0.25):
    """Crystal knocking on crystal: glassy pings in the key, scattered."""
    layers = []
    for i in range(count):
        start = rng.uniform(0, spread)
        f = sa.note(notes[rng.integers(len(notes))], octave) * rng.uniform(0.995, 1.005)
        layers.append((start, rng.uniform(0.4, 1.0) * sa.glass(f, 0.4, rng.uniform(0.04, 0.12))))
    return place(dur, *layers)


def clicks(dur, rate, rng, freq=(1800, 3400), level=1.0):
    """Chitin clicks: little sharp ticks, `rate` a second."""
    out = np.zeros(sa.samples(dur))
    for start in sa.moments(dur, rate, rng):
        tick = sa.tick(rng.uniform(*freq), rng, level)
        i = sa.samples(start)
        out[i:i + len(tick)] += tick[:len(out) - i]
    return out


# ================================================================ the Bramblewalker: creaking wood, rustling leaves, the crack of a vine


def monster_bramble_ambient(v, rng):
    """A slow creak of wood under weight, and leaves stirring."""
    dur = 1.6
    c = creak(1.1, (180, 150)[v], (140, 120)[v], rng) * sa.env(1.1, (0, 0), (0.3, 1), (0.8, 0.6), (1.1, 0))
    x = place(dur, (0.1, 0.7 * c), 0.4 * rustle(dur, rng, 1600, 3800, 0.6), (0.05, 0.25 * knock(rng, 200, 900, 0.03)))
    return sa.finish(sa.reverb(x, 1.0, 0.18, damp=3000), "effect")


def monster_bramble_hurt(v, rng):
    """Green wood splitting: a crack, a splinter of grains, a shiver of leaves."""
    dur = 0.6
    crack = sa.norm(sa.bandpass(sa.noise(0.08, rng), 900, 4500)) * sa.decay(0.08, 0.012, 0.0004)
    splinter = sa.norm(sa.bandpass(grains(0.4, 420, rng, length=(0.001, 0.005), shape=[(0, 1), (0.25, 0.4), (0.4, 0)]), 800, 4200))
    groan = creak(0.45, (260, 220)[v], 160, rng, (20, 34)) * sa.decay(0.45, 0.15, 0.01)
    x = place(dur, 0.8 * crack, (0.02, 0.5 * splinter), (0.03, 0.5 * groan), 0.3 * rustle(dur, rng, 2000, 4400, 0.2))
    return sa.finish(sa.reverb(x, 0.5, 0.12, damp=3500), "impact")


def monster_bramble_death(v, rng):
    """It comes apart: a long groan of wood, cracks, and a heap of twigs and leaves falling in."""
    dur = 2.2
    groan = creak(1.4, 200, 90, rng, (10, 18)) * sa.env(1.4, (0, 0), (0.2, 1), (1.4, 0))
    cracks = place(dur, *[(t, 0.6 * sa.norm(sa.bandpass(sa.noise(0.07, rng), 700, 4000)) * sa.decay(0.07, 0.012, 0.0004)) for t in (0.3, 0.75, 1.1, 1.35)])
    fall = sa.norm(sa.bandpass(grains(1.0, 500, rng, length=(0.002, 0.008), shape=[(0, 0.2), (0.3, 1), (1.0, 0)]), 400, 3500))
    thud = sa.thump(110, 45, 0.6, 0.15, drive=2.0)
    x = place(dur, 0.6 * groan, cracks, (1.0, 0.5 * fall), (1.15, 0.4 * thud), (0.9, 0.35 * rustle(1.2, rng, 1500, 4000, 0.3)))
    return sa.finish(sa.reverb(x, 1.4, 0.22, damp=2800), "grand")


def monster_bramble_rear(v, rng):
    """The tell: wood drawing back with a rising creak, leaves hissing as the vines pull in."""
    dur = 0.8
    c = creak(0.8, 150, 320, rng, (16, 30)) * sa.swell(0.8, 0.7, 1.5)
    x = place(dur, 0.7 * c, 0.5 * rustle(dur, rng, 1800, 4600, 0.85))
    return sa.finish(sa.reverb(x, 0.6, 0.15, damp=3500), "effect")


def monster_bramble_lash(v, rng):
    """The vine whips out: a swish through the air and a sharp crack, leaves torn off."""
    dur = 0.6
    swish = sa.moving_band(0.22, [(0, 900), (0.2, 4200)], 1.0, rng) * sa.swell(0.22, 0.19, 2.5)
    crack = sa.norm(sa.bandpass(sa.noise(0.06, rng), 1200, 6000)) * sa.decay(0.06, 0.008, 0.0003)
    snap = sa.thump(240, 120, 0.15, 0.03, drive=1.8)
    x = place(dur, 0.7 * swish, (0.2, 0.9 * crack), (0.2, 0.4 * snap), (0.22, 0.3 * rustle(0.35, rng, 2400, 4800, 0.1)))
    return sa.finish(sa.reverb(x, 0.4, 0.1, damp=4000), "impact")


# ================================================================ the Gloomstalker: a purr in the dark, a hiss, a snarl


def monster_gloom_ambient(v, rng):
    """A low purr in the dark (a growl's rumble, pulsing slowly), and a breath."""
    dur = 1.8
    purr = growl(dur, (58, 52)[v], (54, 50)[v], rng, rasp=(22, 26), formant=(180, 900), harmonics=16) * sa.chopper(dur, rng, (2.5, 3.5), 0.4)
    x = place(dur, 0.8 * purr * sa.env(dur, (0, 0), (0.3, 1), (1.4, 0.8), (dur, 0)), 0.2 * breath(dur, 600, 2400, rng, 0.5))
    return sa.finish(sa.reverb(x, 1.0, 0.2, damp=2500), "effect")


def monster_gloom_hurt(v, rng):
    """A spitting hiss with a yowl under it."""
    dur = 0.55
    hiss = sa.norm(sa.bandpass(sa.noise(dur, rng), 2200, 6500)) * sa.env(dur, (0, 0), (0.03, 1), (dur, 0)) ** 1.5
    yowl = growl(0.4, (420, 380)[v], 260, rng, rasp=(30, 45), formant=(400, 2400)) * sa.decay(0.4, 0.14, 0.01)
    x = place(dur, 0.6 * hiss, (0.02, 0.7 * yowl))
    return sa.finish(sa.reverb(x, 0.5, 0.15, damp=3000), "impact")


def monster_gloom_death(v, rng):
    """A last yowl that thins into a rush of dark, drawn away to nothing."""
    dur = 2.0
    yowl = growl(0.9, 360, 140, rng, rasp=(24, 34), formant=(300, 2000)) * sa.env(0.9, (0, 0), (0.05, 1), (0.9, 0))
    rush = sa.moving_band(1.6, [(0, 3200), (1.6, 400)], 1.0, rng) * sa.env(1.6, (0, 0), (0.3, 1), (1.6, 0))
    low = sa.sine(sa.glide(1.2, (0, sa.note(D, -1)), (1.2, sa.note(A, -2))), 1.2) * sa.env(1.2, (0, 0), (0.4, 1), (1.2, 0))
    x = place(dur, 0.7 * yowl, (0.4, 0.5 * rush), (0.6, 0.3 * low))
    return sa.finish(sa.reverb(x, 1.4, 0.3, damp=2500), "grand")


def monster_gloom_growl(v, rng):
    """The tell: a low growl rising as it crouches."""
    dur = 0.8
    g = growl(dur, 70, 110, rng, rasp=(26, 34), formant=(220, 1300)) * sa.swell(dur, 0.65, 1.3)
    x = place(dur, 0.85 * g, 0.2 * breath(dur, 500, 2000, rng, 0.7))
    return sa.finish(sa.reverb(x, 0.6, 0.15, damp=2800), "effect")


def monster_gloom_pounce(v, rng):
    """The pounce: a snarl and a rush of air."""
    dur = 0.55
    snarl = growl(0.35, 180, 120, rng, rasp=(34, 44), formant=(300, 2200)) * sa.decay(0.35, 0.12, 0.004)
    rush = sa.moving_band(0.4, [(0, 700), (0.15, 3000), (0.4, 900)], 1.0, rng) * sa.env(0.4, (0, 0), (0.12, 1), (0.4, 0))
    x = place(dur, 0.8 * snarl, (0.03, 0.55 * rush))
    return sa.finish(sa.reverb(x, 0.4, 0.12, damp=3500), "impact")


def monster_gloom_reveal(v, rng):
    """Light falls on it: a bright shimmer, and a startled hiss."""
    dur = 0.9
    shimmer = sa.sparkle(0.6, 26, rng, [sa.note(d, 2) for d in (D, FS, A, B)], tau=(0.06, 0.18), shape=[(0, 1), (0.6, 0)])
    hiss = sa.norm(sa.bandpass(sa.noise(0.4, rng), 2000, 6000)) * sa.env(0.4, (0, 0), (0.04, 1), (0.4, 0)) ** 2
    x = place(dur, 0.6 * sa.norm(shimmer), (0.05, 0.4 * hiss))
    return sa.finish(sa.reverb(x, 0.7, 0.25, damp=4500), "effect")


# ================================================================ the Thunderwing Harpy: a raptor's cry, a stoop's rush


def shriek(dur, f0, f1, rng, vib=7.0):
    """A raptor's scream: a hard, bright tone sliding down, with a fast vibrato and a rasp of breath."""
    t = sa.timeline(dur)
    pitch = sa.glide(dur, (0, f0), (dur * 0.3, f0 * 1.05), (dur, f1)) * (1 + 0.03 * np.sin(2 * np.pi * vib * t))
    tone = sa.fm(pitch, 2.0, 1.4, dur)
    rasp = sa.norm(sa.bandpass(sa.noise(dur, rng), 1500, 5000)) * 0.3
    return sa.norm(sa.bandpass(tone + rasp, 700, 5500))


def monster_harpy_ambient(v, rng):
    """A short, clipped call, twice."""
    dur = 1.0
    call = lambda f: shriek(0.18, f, f * 0.8, rng, 9) * sa.env(0.18, (0, 0), (0.02, 1), (0.18, 0))
    f = (1500, 1700)[v]
    x = place(dur, 0.8 * call(f), (0.26, 0.6 * call(f * 0.92)))
    return sa.finish(sa.reverb(x, 0.8, 0.25, damp=4000), "effect")


def monster_harpy_shriek(v, rng):
    """The tell: a long, piercing scream with a crackle of static under it."""
    dur = 1.1
    s = shriek(0.9, 2300, 1300, rng) * sa.env(0.9, (0, 0), (0.05, 1), (0.6, 0.8), (0.9, 0))
    static = sa.norm(sa.bandpass(grains(0.9, 260, rng, length=(0.0006, 0.002)), 1800, 6000)) * sa.swell(0.9, 0.5)
    x = place(dur, 0.85 * s, (0.1, 0.25 * static))
    return sa.finish(sa.reverb(x, 1.0, 0.3, damp=4200), "cast")


def monster_harpy_hurt(v, rng):
    """A ragged squawk, and a ruffle of feathers."""
    dur = 0.5
    sq = shriek(0.25, (1800, 1600)[v], 1100, rng, 12) * sa.decay(0.25, 0.09, 0.004)
    ruffle = sa.norm(sa.bandpass(grains(0.35, 300, rng, length=(0.002, 0.006)), 1200, 4500)) * sa.decay(0.35, 0.12, 0.01)
    x = place(dur, 0.8 * sq, (0.03, 0.4 * ruffle))
    return sa.finish(sa.reverb(x, 0.5, 0.15, damp=3800), "impact")


def monster_harpy_death(v, rng):
    """A falling cry, wings beating out of time, and a last flutter."""
    dur = 1.8
    cry = shriek(1.0, 1900, 700, rng, 5) * sa.env(1.0, (0, 0), (0.05, 1), (1.0, 0))
    flaps = place(1.2, *[(t, 0.6 * sa.norm(sa.bandpass(sa.noise(0.12, rng), 400, 2200)) * sa.env(0.12, (0, 0), (0.04, 1), (0.12, 0)))
                         for t in (0.0, 0.22, 0.5, 0.75, 1.05)])
    x = place(dur, 0.7 * cry, (0.4, 0.5 * flaps))
    return sa.finish(sa.reverb(x, 1.2, 0.25, damp=3500), "grand")


def monster_harpy_dive(v, rng):
    """The stoop: wings snapping shut, and air screaming past."""
    dur = 1.0
    snap = sa.norm(sa.bandpass(sa.noise(0.08, rng), 500, 2500)) * sa.decay(0.08, 0.02, 0.001)
    rush = sa.moving_band(0.9, [(0, 600), (0.7, 3200), (0.9, 2000)], 0.8, rng) * sa.swell(0.9, 0.75, 1.4)
    whistle = sa.sine(sa.glide(0.9, (0, 900), (0.9, 1600)), 0.9) * sa.swell(0.9, 0.8) * 0.15
    x = place(dur, 0.5 * snap, (0.05, 0.75 * rush), (0.05, whistle))
    return sa.finish(sa.reverb(x, 0.6, 0.12, damp=4000), "effect")


# ================================================================ the Geode Crawler: chitin, crystal, a ball rolling


def monster_geode_ambient(v, rng):
    """Chittering clicks, and a crystal on its back chiming now and then."""
    dur = 1.2
    chitter = clicks(0.5, 40, rng, level=0.8) * sa.env(0.5, (0, 0), (0.05, 1), (0.5, 0))
    chime = sa.glass(sa.note((A, FS)[v], 2), 0.7, 0.15)
    x = place(dur, (0.05, 0.7 * chitter), (0.45, 0.35 * chime), (0.7, 0.4 * clicks(0.3, 30, rng)))
    return sa.finish(sa.reverb(x, 0.6, 0.2, damp=4000), "effect")


def monster_geode_curl(v, rng):
    """It balls up: plates sliding shut over each other, and crystal knocking."""
    dur = 0.6
    plates = sa.norm(sa.bandpass(grains(0.3, 160, rng, length=(0.002, 0.006)), 600, 3000)) * sa.env(0.3, (0, 0.6), (0.2, 1), (0.3, 0))
    shut = sa.thump(200, 90, 0.2, 0.04, drive=1.6)
    x = place(dur, 0.7 * plates, (0.22, 0.5 * shut), (0.24, 0.5 * clatter(0.4, rng, 5, spread=0.08)))
    return sa.finish(sa.reverb(x, 0.4, 0.12, damp=3800), "impact")


def monster_geode_rattle(v, rng):
    """The tell: the ball shivers, crystals rattling against each other faster and faster."""
    dur = 0.7
    rattle = place(dur, *[(t, 0.6 * sa.glass(sa.note([D, FS, A][i % 3], 2), 0.12, 0.03)) for i, t in enumerate(np.cumsum(np.linspace(0.06, 0.025, 16)))
                          if t < dur - 0.1])
    buzz = clicks(dur, 70, rng, (1200, 2600), 0.6) * sa.swell(dur, 0.6)
    x = place(dur, 0.7 * rattle, 0.4 * buzz)
    return sa.finish(sa.reverb(x, 0.4, 0.12, damp=4000), "effect")


def monster_geode_roll(v, rng):
    """Rolling: a grinding rumble over stone, with crystal ticking on each turn."""
    dur = 1.0
    grind = sa.moving_band(dur, [(0, 400), (dur, 700)], 0.8, rng) * sa.chopper(dur, rng, (8, 12), 0.5)
    rumble = sa.norm(sa.lowpass(sa.brown(dur, rng), 300))
    ticks = place(dur, *[(t, 0.3 * sa.glass(sa.note(A, 2), 0.1, 0.03)) for t in np.arange(0.05, dur - 0.1, 0.11)])
    x = place(dur, 0.6 * sa.norm(grind), 0.3 * rumble, ticks)
    return sa.finish(sa.reverb(x, 0.5, 0.1, damp=3000), "effect")


def monster_geode_crack(v, rng):
    """The crystal gives: a hard crack and a spray of shards."""
    dur = 1.0
    crack = sa.norm(sa.bandpass(sa.noise(0.07, rng), 1000, 5000)) * sa.decay(0.07, 0.01, 0.0003)
    shards = sa.shatter(rng, count=14, band=(1500, 6000), spread=0.25)
    ring = clatter(0.8, rng, 6, spread=0.3)
    x = place(dur, 0.8 * crack, (0.01, 0.6 * sa.norm(shards)), (0.04, 0.4 * ring))
    return sa.finish(sa.reverb(x, 0.8, 0.2, damp=4500), "impact")


def monster_geode_hurt(v, rng):
    """Chitin cracking, a squeal of clicks."""
    dur = 0.45
    k = knock(rng, 500, 2500, 0.015)
    squeal = clicks(0.3, 90, rng, (2200, 3600))
    x = place(dur, 0.8 * k, (0.02, 0.5 * squeal), (0.0, 0.3 * clatter(0.4, rng, 3, spread=0.05)))
    return sa.finish(sa.reverb(x, 0.4, 0.12, damp=4000), "impact")


def monster_geode_death(v, rng):
    """It falls apart: the shell splits, the crystals shatter and scatter ringing."""
    dur = 1.8
    split = sa.thump(160, 60, 0.4, 0.08, drive=2.0)
    shards = sa.shatter(rng, count=24, band=(1500, 6000), spread=0.4)
    ring = clatter(1.4, rng, 12, spread=0.8)
    x = place(dur, 0.5 * split, (0.03, 0.6 * sa.norm(shards)), (0.1, 0.45 * ring))
    return sa.finish(sa.reverb(x, 1.2, 0.25, damp=4500), "grand")


# ================================================================ the Bog Witch-Frog: croaks, gurgles, a wet pop


def monster_frog_ambient(v, rng):
    """A deep, throaty croak, twice."""
    dur = 1.3
    c = lambda d, f: croak(d, 34, f, rng) * sa.env(d, (0, 0), (0.05, 1), (d * 0.7, 0.8), (d, 0))
    f = (110, 98)[v]
    x = place(dur, 0.85 * c(0.42, f), (0.6, 0.7 * c(0.35, f * 0.9)))
    return sa.finish(sa.reverb(x, 0.8, 0.2, damp=2500), "effect")


def monster_frog_croak(v, rng):
    """Its mouth opening: a short, wet croak."""
    dur = 0.4
    x = croak(0.3, 40, 140, rng) * sa.env(0.3, (0, 0), (0.03, 1), (0.3, 0))
    return sa.finish(sa.reverb(place(dur, x), 0.4, 0.15, damp=2500), "effect")


def monster_frog_swell(v, rng):
    """The tell: its throat filling, a gurgle rising in pitch."""
    dur = 0.9
    gurgle = bubbles(dur, 30, rng, 200, 600) * sa.swell(dur, 0.8)
    stretch = croak(dur, 22, 90, rng, (200, 1000)) * sa.swell(dur, 0.8, 1.2)
    rise = sa.sine(sa.glide(dur, (0, 160), (dur, 320)), dur) * sa.swell(dur, 0.85) * 0.4
    x = place(dur, 0.6 * sa.norm(gurgle), 0.5 * stretch, rise)
    return sa.finish(sa.reverb(x, 0.6, 0.18, damp=2500), "effect")


def monster_frog_spit(v, rng):
    """The spit: a wet pop out of its mouth and a slosh."""
    dur = 0.5
    pop = sa.sine(sa.sweep(500, 180, 0.08), 0.08) * sa.decay(0.08, 0.02, 0.001)
    slosh = sa.norm(sa.bandpass(sa.noise(0.3, rng), 500, 2500)) * sa.decay(0.3, 0.08, 0.005)
    x = place(dur, 0.8 * pop, (0.02, 0.4 * slosh), (0.04, 0.3 * sa.norm(bubbles(0.3, 25, rng))))
    return sa.finish(sa.reverb(x, 0.4, 0.12, damp=3000), "impact")


def monster_frog_tongue(v, rng):
    """The tongue: a wet slap through the air."""
    dur = 0.4
    whip = sa.moving_band(0.12, [(0, 800), (0.12, 2600)], 1.0, rng) * sa.swell(0.12, 0.1, 2.0)
    slap = sa.norm(sa.bandpass(sa.noise(0.08, rng), 400, 2200)) * sa.decay(0.08, 0.015, 0.0005)
    x = place(dur, 0.6 * whip, (0.11, 0.8 * slap))
    return sa.finish(sa.reverb(x, 0.3, 0.1, damp=3000), "impact")


def monster_frog_gulp(v, rng):
    """A gulp: something large going down whole, and a satisfied croak."""
    dur = 0.9
    gulp = place(0.25, sa.thump(150, 60, 0.25, 0.06, drive=1.5), 0.4 * sa.sine(sa.sweep(380, 120, 0.15), 0.15) * sa.decay(0.15, 0.05, 0.002))
    after = croak(0.3, 30, 100, rng) * sa.env(0.3, (0, 0), (0.05, 1), (0.3, 0))
    x = place(dur, 0.7 * gulp, (0.4, 0.5 * after))
    return sa.finish(sa.reverb(x, 0.5, 0.15, damp=2500), "effect")


def monster_frog_hurt(v, rng):
    """A croaking yelp."""
    dur = 0.45
    yelp = croak(0.3, 55, (190, 170)[v], rng, (300, 2000)) * sa.decay(0.3, 0.1, 0.003)
    x = place(dur, 0.85 * yelp, 0.3 * knock(rng, 300, 1600, 0.02))
    return sa.finish(sa.reverb(x, 0.4, 0.15, damp=2800), "impact")


def monster_frog_death(v, rng):
    """A long, sinking croak that runs out of air, and a gurgle."""
    dur = 1.8
    sink = croak(1.1, 28, 100, rng) * sa.env(1.1, (0, 0), (0.05, 1), (1.1, 0))
    gurgle = bubbles(0.9, 22, rng, 150, 450) * sa.env(0.9, (0, 1), (0.9, 0))
    x = place(dur, 0.7 * sink, (0.8, 0.5 * sa.norm(gurgle)))
    return sa.finish(sa.reverb(x, 1.0, 0.22, damp=2500), "grand")


def monster_bubble_pop(v, rng):
    """A bubble of bog poison bursting: a wet pop and a spatter."""
    dur = 0.6
    pop = sa.sine(sa.sweep(900, 300, 0.05), 0.05) * sa.decay(0.05, 0.012, 0.0005)
    spatter = sa.norm(sa.bandpass(grains(0.4, 300, rng, length=(0.001, 0.004), shape=[(0, 1), (0.4, 0)]), 600, 3800))
    fizz = sa.norm(bubbles(0.5, 40, rng, 400, 1200)) * sa.env(0.5, (0, 0.8), (0.5, 0))
    x = place(dur, 0.8 * pop, (0.01, 0.5 * spatter), (0.03, 0.3 * fizz))
    return sa.finish(sa.reverb(x, 0.4, 0.12, damp=3200), "impact")


# ================================================================ the Mana Ooze: jelly, and spells going down


def squelch(dur, rng, lo=180, hi=900):
    return sa.norm(sa.moving_band(dur, [(0, lo), (dur * 0.5, hi), (dur, lo)], 0.7, rng) * sa.chopper(dur, rng, (10, 18), 0.3))


def monster_ooze_ambient(v, rng):
    """A slow burble of jelly, and a faint shimmer of the mana turning in it."""
    dur = 1.6
    burble = sa.norm(bubbles(dur, 9 + 3 * v, rng, 180, 520)) * sa.env(dur, (0, 0), (0.3, 1), (dur, 0.3))
    shimmer = sa.sparkle(dur, 5, rng, [sa.note(d, 2) for d in (D, FS, A)], tau=(0.12, 0.3), shape=[(0, 0.3), (0.8, 1), (dur, 0)])
    x = place(dur, 0.7 * burble, 0.3 * squelch(dur, rng), 0.2 * sa.norm(shimmer))
    return sa.finish(sa.reverb(x, 1.0, 0.22, damp=3000), "effect")


def monster_ooze_absorb(v, rng):
    """A spell going in: a glassy slurp drawn inward, ending on a chord in the key."""
    pre = 0.4
    chord = sa.mix(sa.bell(sa.note(D, 1), 0.7, 0.3, 2.0, 1.2), 0.7 * sa.bell(sa.note(A, 1), 0.7, 0.3, 2.0, 1.2), 0.5 * sa.glass(sa.note(FS, 2), 0.7, 0.2))
    inward = sa.norm(sa.reverse(sa.reverb(sa.glass(sa.note(A, 2), 0.6, 0.2), 0.5, 0.6))[-sa.samples(pre):])
    slurp = sa.moving_band(pre, [(0, 3000), (pre, 500)], 0.8, rng) * sa.swell(pre, pre * 0.9, 1.5)
    x = place(pre + 0.8, 0.6 * inward, 0.4 * slurp, (pre, 0.6 * chord))
    return sa.finish(sa.reverb(x, 0.8, 0.25, damp=4000), "effect")


def monster_ooze_grow(v, rng):
    """It swells a size: a stretching wobble of jelly, rising."""
    dur = 1.0
    wobble = sa.sine(sa.glide(dur, (0, 140), (dur, 280)), dur) * (0.6 + 0.4 * np.sin(2 * np.pi * 7 * sa.timeline(dur)))
    x = place(dur, 0.6 * wobble * sa.swell(dur, 0.7), 0.4 * squelch(dur, rng, 200, 1100), 0.3 * sa.glass(sa.note(D, 2), 0.8, 0.3))
    return sa.finish(sa.reverb(x, 0.7, 0.2, damp=3000), "effect")


def monster_ooze_split(v, rng):
    """Overfull, it bursts: a wet blast, and a ringing burst of raw mana."""
    dur = 1.4
    blast = sa.norm(sa.bandpass(sa.noise(0.3, rng), 300, 3000)) * sa.decay(0.3, 0.07, 0.002)
    thud = sa.thump(120, 50, 0.4, 0.1, drive=2.0)
    ring = sa.sparkle(1.0, 30, rng, [sa.note(d, 2) for d in (D, E, FS, A, B)], tau=(0.08, 0.25), shape=[(0, 1), (1.0, 0)])
    x = place(dur, 0.6 * blast, 0.5 * thud, (0.02, 0.4 * sa.norm(ring)), (0.05, 0.3 * sa.norm(bubbles(0.6, 40, rng))))
    return sa.finish(sa.reverb(x, 1.0, 0.25, damp=3500), "impact")


def monster_ooze_hurt(v, rng):
    """Struck: a squelch."""
    dur = 0.4
    x = place(dur, squelch(0.3, rng, (240, 200)[v], 1200) * sa.decay(0.3, 0.1, 0.003), 0.3 * knock(rng, 300, 1400, 0.02))
    return sa.finish(sa.reverb(x, 0.4, 0.12, damp=3000), "impact")


def monster_ooze_death(v, rng):
    """It collapses: jelly slumping, and the mana in it fading out in a falling shimmer."""
    dur = 1.6
    slump = squelch(0.7, rng, 150, 700) * sa.env(0.7, (0, 1), (0.7, 0))
    fade = sa.sparkle(1.2, 12, rng, [sa.note(d, 2) for d in (B, A, FS, E, D)], tau=(0.1, 0.3), shape=[(0, 1), (1.2, 0)])
    x = place(dur, 0.6 * slump, (0.2, 0.4 * sa.norm(fade)))
    return sa.finish(sa.reverb(x, 1.0, 0.25, damp=3000), "grand")


EVENTS = [
    event("monster_bramble_ambient", monster_bramble_ambient, variants=2, subtitle="bramblewalker.ambient"),
    event("monster_bramble_hurt", monster_bramble_hurt, variants=2, role="impact", subtitle="bramblewalker.hurt"),
    event("monster_bramble_death", monster_bramble_death, role="grand", subtitle="bramblewalker.death"),
    event("monster_bramble_rear", monster_bramble_rear, subtitle="bramblewalker.rear"),
    event("monster_bramble_lash", monster_bramble_lash, role="impact", subtitle="bramblewalker.lash"),
    event("monster_gloom_ambient", monster_gloom_ambient, variants=2, subtitle="gloomstalker.ambient"),
    event("monster_gloom_hurt", monster_gloom_hurt, variants=2, role="impact", subtitle="gloomstalker.hurt"),
    event("monster_gloom_death", monster_gloom_death, role="grand", subtitle="gloomstalker.death"),
    event("monster_gloom_growl", monster_gloom_growl, subtitle="gloomstalker.growl"),
    event("monster_gloom_pounce", monster_gloom_pounce, role="impact", subtitle="gloomstalker.pounce"),
    event("monster_gloom_reveal", monster_gloom_reveal, subtitle="gloomstalker.reveal"),
    event("monster_harpy_ambient", monster_harpy_ambient, variants=2, attenuation=32, subtitle="harpy.ambient"),
    event("monster_harpy_shriek", monster_harpy_shriek, role="cast", attenuation=48, subtitle="harpy.shriek"),
    event("monster_harpy_hurt", monster_harpy_hurt, variants=2, role="impact", subtitle="harpy.hurt"),
    event("monster_harpy_death", monster_harpy_death, role="grand", subtitle="harpy.death"),
    event("monster_harpy_dive", monster_harpy_dive, attenuation=32, subtitle="harpy.dive"),
    event("monster_geode_ambient", monster_geode_ambient, variants=2, subtitle="geode_crawler.ambient"),
    event("monster_geode_curl", monster_geode_curl, role="impact", subtitle="geode_crawler.curl"),
    event("monster_geode_rattle", monster_geode_rattle, subtitle="geode_crawler.rattle"),
    event("monster_geode_roll", monster_geode_roll, subtitle="geode_crawler.roll"),
    event("monster_geode_crack", monster_geode_crack, role="impact", subtitle="geode_crawler.crack"),
    event("monster_geode_hurt", monster_geode_hurt, variants=2, role="impact", subtitle="geode_crawler.hurt"),
    event("monster_geode_death", monster_geode_death, role="grand", subtitle="geode_crawler.death"),
    event("monster_frog_ambient", monster_frog_ambient, variants=2, subtitle="frog.ambient"),
    event("monster_frog_croak", monster_frog_croak, subtitle="frog.ambient"),
    event("monster_frog_swell", monster_frog_swell, subtitle="frog.swell"),
    event("monster_frog_spit", monster_frog_spit, role="impact", subtitle="frog.spit"),
    event("monster_frog_tongue", monster_frog_tongue, role="impact", subtitle="frog.tongue"),
    event("monster_frog_gulp", monster_frog_gulp, subtitle="frog.gulp"),
    event("monster_frog_hurt", monster_frog_hurt, variants=2, role="impact", subtitle="frog.hurt"),
    event("monster_frog_death", monster_frog_death, role="grand", subtitle="frog.death"),
    event("monster_bubble_pop", monster_bubble_pop, role="impact", subtitle="frog.pop"),
    event("monster_ooze_ambient", monster_ooze_ambient, variants=2, subtitle="ooze.ambient"),
    event("monster_ooze_absorb", monster_ooze_absorb, subtitle="ooze.absorb"),
    event("monster_ooze_grow", monster_ooze_grow, subtitle="ooze.grow"),
    event("monster_ooze_split", monster_ooze_split, role="impact", subtitle="ooze.split"),
    event("monster_ooze_hurt", monster_ooze_hurt, variants=2, role="impact", subtitle="ooze.hurt"),
    event("monster_ooze_death", monster_ooze_death, role="grand", subtitle="ooze.death"),
]
