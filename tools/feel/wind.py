"""Wind's kit sounds. Own this file if you own wind: add builders and list them in EVENTS.

Names must start with 'wind_' (see tools/feel/core.py; tools/feel/fire.py has worked examples to copy).
Moving bands, whistles and thumps of air, with a little glass for the chimes.
"""
import numpy as np

from feel.core import sa, event


def _step(v, ratios=(1.0, 1.122, 1.26)):
    return ratios[v % len(ratios)]


def wind_slash(v, rng):
    """An airy razor: noise above 2 kHz with a fast pitch-down whistle."""
    dur = 0.2
    air = sa.norm(sa.bandpass(sa.noise(0.09, rng), 2000, 6500)) * sa.env(0.09, (0, 0), (0.012, 1), (0.09, 0))
    f0 = sa.note(sa.E, 3) * _step(v, (1.0, 1.05, 0.95))
    whistle = sa.sine(sa.glide(0.15, (0, f0), (0.1, f0 * 0.75))) * sa.decay(0.15, 0.05, 0.004)
    x = sa.mix(0.9 * air, (0.005, 0.35 * whistle))
    return sa.finish(x, "impact")


def wind_thump(v, rng):
    """A body blow of air: low thump and a mid puff."""
    body = sa.thump(150 - 10 * v, 60, 0.3, 0.07, drive=1.3, knock=0.6)
    puff = sa.moving_band(0.25, [(0, 300), (0.15, 900), (0.25, 600)], 0.7, rng) * sa.decay(0.25, 0.07, 0.004)
    x = sa.mix(0.4 * body, (0.01, 1.2 * puff))
    return sa.finish(sa.highpass(sa.reverb(x, 0.25, 0.06), 60), "impact")


def wind_rise(v, rng):
    """A moving band climbing with a whistle partial."""
    dur = 0.5
    band = sa.moving_band(dur, [(0, 250), (dur, 2200)], 0.8, rng) * sa.swell(dur, 0.35, 1.5)
    f = sa.note(sa.D, 2) * _step(v, (1.0, 1.122, 1.26))
    whistle = sa.sine(sa.glide(dur, (0, f * 0.7), (dur, f * 1.3))) * sa.swell(dur, 0.35, 1.5)
    x = sa.mix(band, 0.18 * whistle)
    return sa.finish(sa.reverb(x, 0.3, 0.08), "cast")


def wind_crash(v, rng):
    """A falling whoosh into a ground thud and a soft air thwomp."""
    whoosh = sa.moving_band(0.25, [(0, 2200), (0.25, 300)], 0.7, rng) * sa.env(0.25, (0, 0), (0.05, 1), (0.25, 0.7))
    thud = sa.thump(85 - 8 * v, 42, 0.35, 0.1, drive=1.6, knock=0.4)
    thwomp = sa.norm(sa.lowpass(sa.noise(0.3, rng), 500)) * sa.decay(0.3, 0.09, 0.006)
    x = sa.mix(0.9 * whoosh, (0.24, 0.8 * thud), (0.25, 0.7 * thwomp))
    return sa.finish(sa.reverb(x, 0.35, 0.08), "impact")


def wind_dash(v, rng):
    """A Doppler streak: a fast band 800 to 3500 Hz with a tiny onset pop."""
    dur = 0.2
    streak = sa.moving_band(dur, [(0, 800), (0.12, 3500 * _step(v, (1.0, 0.9, 1.1))), (dur, 2500)], 0.7, rng) * sa.env(dur, (0, 0), (0.05, 1), (dur, 0))
    pop = sa.norm(sa.lowpass(sa.noise(0.02, rng), 700)) * sa.decay(0.02, 0.005, 0.0005)
    x = sa.mix(streak, (0.0, 0.6 * pop))
    return sa.finish(x, "cast")


def wind_feather(v, rng):
    """A soft wind chime over a breath swell."""
    dur = 0.7
    notes = (sa.D, sa.A, sa.E)
    octs = (1, 1, 2)
    order = [(0, 1, 2), (1, 2, 0), (2, 0, 1)][v % 3]
    layers = []
    for k, i in enumerate(order):
        layers.append((0.09 * k, 0.8 * sa.glass(sa.note(notes[i], octs[i] + 1), 0.55, 0.4, 0.003)))
    breath = sa.moving_band(dur, [(0, 2000), (0.3, 4000), (dur, 3000)], 0.5, rng) * sa.swell(dur, 0.3)
    x = sa.mix(*layers, 0.25 * breath)
    return sa.finish(sa.reverb(x, 0.5, 0.12), "cast")


def wind_whirl(v, rng):
    """A swirling burst: a moving band with about 6 Hz eddy modulation and a low rumble."""
    dur = 1.2
    band = sa.moving_band(dur, [(0, 500), (0.5, 1400), (dur, 700)], 0.7, rng)
    eddy = 0.6 + 0.4 * np.sin(2 * np.pi * (6 + 0.5 * v) * sa.timeline(dur) + v)
    rumble = sa.norm(sa.lowpass(sa.brown(dur, rng), 200))
    x = sa.mix(0.9 * band * eddy * sa.swell(dur, 0.4, 1.2), 0.4 * rumble * sa.swell(dur, 0.5))
    return sa.finish(sa.reverb(x, 0.4, 0.08), "effect")


def wind_deflect(v, rng):
    """A bright ricochet: a glass partial at A6 with a fast pitch-down zip."""
    f = sa.note(sa.A, 2) * 2 * _step(v, (1.0, 1.06, 0.94))
    zip_ = sa.sine(sa.glide(0.2, (0, f * 1.5), (0.06, f))) * sa.decay(0.2, 0.05, 0.001)
    ring = sa.glass(f, 0.25, 0.09, 0.001)
    x = sa.mix(0.5 * zip_, (0.02, 0.6 * ring))
    return sa.finish(sa.reverb(x, 0.25, 0.08), "impact")


def wind_snatch(v, rng):
    """A sharp snatch, then paper flutter."""
    tk = sa.tick(1800 * _step(v, (1.0, 1.1, 0.9)), rng, 0.7)
    burst = sa.norm(sa.bandpass(sa.noise(0.06, rng), 1000, 2200)) * sa.decay(0.06, 0.015, 0.001)
    flut = sa.norm(sa.bandpass(sa.noise(0.28, rng), 1500, 5000)) * sa.chopper(0.28, rng, (35, 90), 0.05) * sa.env(0.28, (0, 1), (0.28, 0))
    x = sa.mix(tk, 0.6 * burst, (0.08, 0.5 * flut))
    return sa.finish(x, "impact")


def wind_glyph(v, rng):
    """A low hum: D3 with slow chorus shimmer and a soft attack."""
    dur = 0.8
    f = sa.note(sa.D, -1) * (1.0, 1.5)[v % 2]
    hum = sa.sine(f, dur) + 0.8 * sa.sine(f * 2.0, dur) + 0.6 * sa.sine(f * 3.0, dur) + 0.3 * sa.sine(f * 4.0, dur)
    hum = sa.chorus(hum, 3, 0.004, 0.3) [:sa.samples(dur)] * sa.swell(dur, 0.3)
    return sa.finish(hum, "effect")


def wind_rewind(v, rng):
    """A reversed whoosh into a single clock tick."""
    dur = 0.4
    w = sa.reverse(sa.moving_band(dur, [(0, 3200), (dur, 400)], 0.7, rng) * sa.decay(dur, 0.12, 0.004))
    tk = sa.tick(1900 * _step(v, (1.0, 1.122)), rng, 1.0)
    x = sa.mix(0.6 * w, (dur, tk))
    return sa.finish(x, "impact")


def wind_muffle(v, rng):
    """A voice silenced: a vocal-ish formant burst cut off by a low-passed noise thud."""
    f0 = 170 * _step(v, (1.0, 1.12, 0.9))
    voice = sa.soft_saw(f0 * (1 + 0.05 * sa.timeline(0.14) / 0.14), 0.14, 20, 4000)
    voice = sa.mix(sa.bandpass(voice, 600, 900), 0.6 * sa.bandpass(voice, 1500, 2300))
    voice = sa.norm(voice) * sa.env(0.14, (0, 0), (0.02, 1), (0.1, 1), (0.14, 0))
    thud = sa.norm(sa.lowpass(sa.noise(0.2, rng), 350)) * sa.decay(0.2, 0.05, 0.004)
    body = sa.thump(110, 55, 0.2, 0.06, drive=1.3, knock=0.05)
    x = sa.mix(0.8 * voice, (0.1, 0.9 * thud), (0.1, 0.5 * body))
    return sa.finish(x, "tell")


def wind_lift(v, rng):
    """A short bright rising glissando of airy sparkles."""
    dur = 0.3
    notes = [sa.note(d, 2) for d in (sa.D, sa.E, sa.FS, sa.A, sa.B)]
    sp = sa.sparkle(dur - 0.05, 55 + 10 * v, rng, notes, tau=(0.02, 0.05), rising=True)
    air = sa.moving_band(dur, [(0, 1500), (dur, 5000)], 0.5, rng) * sa.swell(dur, 0.2)
    x = sa.mix(sp, 0.35 * air)
    return sa.finish(x, "tell")


def wind_gale(v, rng):
    """A rushing cloak-flare: shorter and higher than the dash, with a soft flutter tail."""
    dur = 0.25
    rush = sa.moving_band(0.18, [(0, 1500), (0.1, 4500 * _step(v, (1.0, 1.06, 0.94))), (0.18, 3500)], 0.6, rng) * sa.env(0.18, (0, 0), (0.04, 1), (0.18, 0))
    flut = sa.norm(sa.bandpass(sa.noise(0.15, rng), 1200, 3500)) * sa.chopper(0.15, rng, (30, 70), 0.1) * sa.env(0.15, (0, 0.7), (0.15, 0))
    x = sa.mix(rush, (0.1, 0.35 * flut))
    return sa.finish(sa.reverb(x, 0.2, 0.06), "cast")


def wind_soar(v, rng):
    """Soar's cast: a breath of air swelling upward under an open glass chord that climbs as it opens."""
    dur = 0.9
    band = sa.moving_band(dur, [(0, 400), (0.5, 1800), (dur, 2600)], 0.9, rng) * sa.swell(dur, 0.45, 1.6)
    root = (sa.D, sa.E, sa.A)[v % 3]
    chord = [
        (0.08, 0.6 * sa.glass(sa.note(root, 1), 0.7, 0.35, 0.004)),
        (0.2, 0.5 * sa.glass(sa.note(root + 2, 1), 0.6, 0.3, 0.004)),
        (0.32, 0.45 * sa.glass(sa.note(root, 2), 0.55, 0.3, 0.004)),
    ]
    x = sa.mix(0.8 * band, *chord)
    return sa.finish(sa.reverb(x, 0.6, 0.15), "cast")


def wind_soar_takeoff(v, rng):
    """Take-off: a wingbeat of air (a soft low push) and a whoosh that lifts away."""
    dur = 0.6
    beat = sa.norm(sa.lowpass(sa.noise(0.18, rng), 420)) * sa.decay(0.18, 0.05, 0.006)
    body = sa.thump(110 - 8 * v, 55, 0.2, 0.06, drive=1.1, knock=0.2)
    whoosh = sa.moving_band(dur, [(0, 600), (0.25, 2800 * _step(v, (1.0, 1.08, 0.93))), (dur, 1800)], 0.7, rng) * sa.env(dur, (0, 0), (0.12, 1), (dur, 0))
    x = sa.mix(0.9 * beat, (0.0, 0.35 * body), (0.04, whoosh))
    return sa.finish(sa.reverb(x, 0.35, 0.08), "cast")


def wind_soar_gust(v, rng):
    """A soft gust past the ears while flying: a slow swell of moving air with a faint eddy in it."""
    dur = 1.4
    c = (700, 900, 600, 800)[v % 4]
    band = sa.moving_band(dur, [(0, c), (0.6, c * 2.2), (dur, c * 1.3)], 1.0, rng) * sa.swell(dur, 0.6, 1.8)
    eddy = 0.8 + 0.2 * np.sin(2 * np.pi * (3.5 + 0.5 * v) * sa.timeline(dur) + v)
    low = sa.norm(sa.lowpass(sa.brown(dur, rng), 260)) * sa.swell(dur, 0.7, 2.0)
    x = sa.mix(band * eddy, 0.3 * low)
    return sa.finish(x, "effect")


def wind_soar_fade(v, rng):
    """The wind fading: a flutter of air thinning and falling away, and two glass notes stepping down."""
    dur = 0.9
    flut = sa.moving_band(dur, [(0, 3000), (dur, 700)], 0.6, rng) * sa.chopper(dur, rng, (18, 40), 0.35) * sa.env(dur, (0, 0), (0.05, 1), (dur, 0))
    hi, lo = [(sa.A, sa.E), (sa.B, sa.FS), (sa.A, sa.D)][v % 3]
    notes = sa.mix((0.0, 0.6 * sa.glass(sa.note(hi, 1), 0.5, 0.25, 0.003)), (0.28, 0.55 * sa.glass(sa.note(lo, 1), 0.6, 0.3, 0.003)))
    x = sa.mix(0.7 * flut, notes)
    return sa.finish(sa.reverb(x, 0.45, 0.12), "tell")


EVENTS = [
    event("wind_slash", wind_slash, variants=3, role="impact", subtitle="hit"),
    event("wind_thump", wind_thump, variants=3, role="impact", subtitle="hit"),
    event("wind_rise", wind_rise, variants=3, role="cast", subtitle="cast"),
    event("wind_crash", wind_crash, variants=2, role="impact", subtitle="hit"),
    event("wind_dash", wind_dash, variants=3, role="cast", subtitle="cast"),
    event("wind_feather", wind_feather, variants=3, role="cast", subtitle="cast"),
    event("wind_whirl", wind_whirl, variants=2, role="effect", subtitle="field"),
    event("wind_deflect", wind_deflect, variants=3, role="impact", subtitle="hit"),
    event("wind_snatch", wind_snatch, variants=3, role="impact", subtitle="hit"),
    event("wind_glyph", wind_glyph, variants=2, role="effect", subtitle="field"),
    event("wind_rewind", wind_rewind, variants=2, role="impact", subtitle="hit"),
    event("wind_muffle", wind_muffle, variants=3, role="tell", subtitle="tell"),
    event("wind_lift", wind_lift, variants=3, role="tell", subtitle="tell"),
    event("wind_gale", wind_gale, variants=3, role="cast", subtitle="cast"),
    event("wind_soar", wind_soar, variants=3, role="cast", subtitle="cast"),
    event("wind_soar_takeoff", wind_soar_takeoff, variants=3, role="cast", subtitle="cast"),
    event("wind_soar_gust", wind_soar_gust, variants=4, role="effect", subtitle="field"),
    event("wind_soar_fade", wind_soar_fade, variants=3, role="tell", subtitle="tell"),
]
