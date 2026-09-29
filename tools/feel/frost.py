"""Frost's kit sounds. Own this file if you own frost: add builders and list them in EVENTS.

Names must start with 'frost_' (see tools/feel/core.py; tools/feel/fire.py has worked examples to copy).
Glass, grains and shatter for the ice half; bubbles, moving bands and beads for the water half.
"""
import numpy as np

from feel.core import sa, event


def _fit(x, seconds):
    """Pads or cuts to an exact length so layers line up."""
    n = sa.samples(seconds)
    return np.pad(x, (0, max(0, n - len(x))))[:n]


def _step(v, ratios=(1.0, 1.122, 1.26)):
    """A small pitch step per variant."""
    return ratios[v % len(ratios)]


def frost_needle(v, rng):
    """A thin dry pierce: a noise tick into a falling glass partial, no tail."""
    dur = 0.35
    tk = sa.norm(sa.bandpass(sa.noise(0.012, rng), 4000, 6000)) * sa.decay(0.012, 0.002, 0.0003)
    f0 = sa.note(sa.A, 2) * _step(v, (1.0, 1.03, 0.97))
    ping = sa.sine(sa.glide(0.3, (0, f0), (0.12, f0 * 0.75))) * sa.decay(0.3, 0.08, 0.0008)
    x = sa.mix(0.7 * tk, (0.002, 0.6 * ping))
    return sa.finish(x, "impact")


def frost_crust(v, rng):
    """A sheet freezing over: grains rising in rate, closed by one low glass tock."""
    dur = 0.5
    g = sa.grains(0.36, 120, rng, shape=[(0, 0.2), (0.36, 1)])
    g = sa.norm(sa.bandpass(g, 3000, 8000)) * sa.env(len(g) / sa.SR, (0, 0.1), (0.36, 1), (0.42, 0))
    tock = sa.glass(sa.note(sa.D, 0) * _step(v, (1.0, 1.0, 1.06)), 0.3, 0.15, 0.001)
    x = sa.mix(0.6 * g, (0.36, 0.9 * tock))
    return sa.finish(sa.reverb(x, 0.3, 0.06), "impact")


def frost_creep(v, rng):
    """Crystals growing: three glass partials rising a step each over a brown bed, slow attack."""
    dur = 0.6
    layers = [0.25 * sa.norm(sa.lowpass(sa.brown(dur, rng), 900)) * sa.swell(dur, 0.35)]
    for i, deg in enumerate((sa.D, sa.E, sa.FS)):
        f = sa.note(deg, 1) * _step(v, (1.0, 1.0, 1.0))
        t = i * 0.13 + 0.03 * v
        p = sa.glass(f, 0.35, 0.16, attack=0.12) * 0.7
        layers.append((t, p))
    x = sa.mix(*layers)
    return sa.finish(sa.reverb(x, 0.5, 0.15), "tell")


def frost_lock(v, rng):
    """A deep resonant lock: thump under a long low glass partial, a short shatter in front."""
    dur = 0.9
    sh = sa.shatter(rng, 12, (3000, 7000), 0.02, (0.01, 0.04))[:sa.samples(0.05)]
    body = sa.thump(150, 60, 0.3, 0.09, drive=1.3, knock=0.6)
    low = sa.glass(sa.note(sa.D, -1) * _step(v, (1.0, 0.891, 1.122)), dur, 0.7, 0.004)
    up = sa.glass(sa.note(sa.D, 1) * _step(v, (1.0, 0.891, 1.122)), 0.5, 0.25, 0.004)
    x = sa.mix(0.9 * sh, (0.02, 0.6 * body), (0.02, 0.6 * low), (0.03, 0.5 * up))
    return sa.finish(sa.reverb(x, 0.6, 0.12), "impact")


def frost_break(v, rng):
    """A fast shatter cluster over a soft thump."""
    cluster = sa.shatter(rng, 26, (2500, 7000), 0.08, (0.03, 0.14))
    body = sa.thump(120 - 8 * v, 55, 0.25, 0.07, drive=1.2)
    x = sa.mix(cluster, (0.005, 0.5 * sa.norm(body)))
    return sa.finish(sa.reverb(x, 0.4, 0.1), "impact")


def frost_whump(v, rng):
    """A soft heavy landing in powder: thump, low-passed noise, a patter of fine grains."""
    dur = 0.9
    body = sa.thump(110 - 8 * v, 50, 0.4, 0.12, drive=1.3, knock=0.5)
    puff = sa.norm(sa.bandpass(sa.noise(0.6, rng), 250, 1500)) * sa.decay(0.6, 0.15, 0.01)
    pat = sa.norm(sa.bandpass(sa.grains(0.6, 60, rng, shape=[(0, 0.3), (0.3, 1), (0.6, 0)]), 2500, 7000))
    x = sa.mix(0.6 * body, 0.7 * puff, (0.25, 0.4 * pat))
    return sa.finish(sa.reverb(x, 0.5, 0.1), "impact")


def frost_hail(v, rng):
    """Four to six fast staccato glass ticks, rising slightly."""
    count = (4, 5, 6)[v % 3]
    layers = []
    t = 0.0
    for i in range(count):
        f = rng.uniform(2600, 3000) + i * 90
        layers.append((t, sa.partials(f, 0.06, ((1.0, 1.0, 1.0), (2.4, 0.3, 0.4)), 0.008, 0.0005) * rng.uniform(0.7, 1.0)))
        t += rng.uniform(0.04, 0.06)
    x = sa.mix(*layers)
    return sa.finish(sa.reverb(x, 0.25, 0.06), "impact")


def frost_splat(v, rng):
    """A wet splat cut by a glass ping and shatter grains."""
    splat = sa.norm(sa.bandpass(sa.noise(0.15, rng), 600, 2500)) * sa.decay(0.15, 0.03, 0.001)
    ping = sa.glass(sa.note(sa.E, 3) * _step(v, (1.0, 1.0, 1.0)), 0.25, 0.08, 0.0008)
    sh = sa.shatter(rng, 14, (3000, 7000), 0.06, (0.02, 0.06))
    x = sa.mix(0.9 * splat, (0.012, 0.5 * ping), (0.01, 0.5 * sh))
    return sa.finish(x, "impact")


def frost_zero(v, rng):
    """Air leaves the room, then one glass note in the silence."""
    air = sa.norm(sa.highpass(sa.noise(0.22, rng), 1500)) * sa.env(0.22, (0, 0), (0.02, 1), (0.22, 0))
    note = sa.glass(sa.note(sa.D, 3) * _step(v, (1.0, 1.0)), 0.4, 0.4, 0.003)
    x = sa.mix(0.6 * air, (0.24, 0.8 * note))
    return sa.finish(sa.reverb(x, 0.5, 0.15), "impact")


def frost_lotus(v, rng):
    """Warm ice: two glass partials with chorus and a soft breath swell."""
    dur = 0.8
    a = sa.glass(sa.note(sa.D, 1), dur, 0.35, 0.01)
    b = sa.glass(sa.note(sa.FS, 1) * _step(v, (1.0, 1.0)), dur - 0.1, 0.3, 0.01)
    breath = sa.norm(sa.bandpass(sa.noise(dur, rng), 1500, 4500)) * sa.swell(dur, 0.35)
    x = sa.chorus(sa.mix(a, (0.1, b)), 3, 0.004, 0.6)
    x = sa.mix(x, 0.12 * breath)
    return sa.finish(sa.reverb(x, 0.6, 0.15), "cast")


def frost_ward(v, rng):
    """A warm glass chord with a brief brown-noise breath."""
    dur = 0.6
    chord = sa.mix(sa.glass(sa.note(sa.D, 0), dur, 0.3, 0.006), (0.03, 0.8 * sa.glass(sa.note(sa.A, 0) * _step(v, (1.0, 1.0)), dur - 0.03, 0.3, 0.006)))
    breath = sa.norm(sa.lowpass(sa.brown(0.3, rng), 700)) * sa.swell(0.3, 0.1)
    x = sa.mix(chord, 0.3 * breath)
    return sa.finish(sa.reverb(sa.chorus(x, 2, 0.003, 0.4), 0.5, 0.12), "cast")


def frost_mirror(v, rng):
    """A shatter cluster in reverse, converging into a glass ring."""
    sh = sa.reverse(sa.shatter(rng, 22, (2500, 7000), 0.12, (0.03, 0.1)))
    sh = sh[:sa.samples(0.3)] if len(sh) > sa.samples(0.3) else sh
    sh = sh * np.linspace(0.3, 1, len(sh))
    at = len(sh) / sa.SR
    ring = sa.mix(sa.glass(sa.note(sa.E, 1), 0.45, 0.25, 0.002), 0.7 * sa.glass(sa.note(sa.A, 1) * _step(v, (1.0, 1.0)), 0.45, 0.22, 0.002))
    x = sa.mix(0.6 * sh, (at, 0.9 * ring))
    return sa.finish(sa.reverb(x, 0.4, 0.1), "cast")


def frost_crack(v, rng):
    """A hairline crack tell: one or two thin sharp cracks and a tiny tick."""
    layers = []
    times = (0.0, 0.07 + 0.02 * v) if v != 1 else (0.0,)
    for t in times:
        lo = rng.uniform(2000, 3000)
        c = sa.norm(sa.bandpass(sa.noise(0.05, rng), lo, lo + 2200)) * sa.decay(0.05, 0.007, 0.0004)
        layers.append((t, c))
    tk = sa.tick(3400, rng, 0.4)
    x = sa.mix(*layers, (0.13, tk))
    return sa.finish(x, "tell")


def frost_tick(v, rng):
    """A small clock-like glass tick with a soft resonance."""
    tk = sa.tick(2400 * _step(v, (1.0, 1.122)), rng, 0.6)
    res = sa.glass(sa.note(sa.A, 2) * _step(v, (1.0, 1.122)), 0.2, 0.06, 0.001)
    x = sa.mix(tk, (0.003, 0.4 * res))
    return sa.finish(x, "tick")


def frost_surge(v, rng):
    """A rising wave: a moving band, bubbling grains and a low swell thump."""
    dur = 0.9
    wave = sa.moving_band(dur, [(0, 300), (0.4, 1800), (dur, 600)], 0.8, rng) * sa.swell(dur, 0.4, 1.5)
    bub = sa.norm(sa.bandpass(sa.grains(dur, 80, rng, shape=[(0, 0.3), (0.4, 1), (dur, 0.2)]), 500, 2500))
    body = sa.thump(90, 45, 0.5, 0.15, drive=1.2) * (1 + 0)
    x = sa.mix(0.9 * wave, 0.3 * _fit(bub, dur), (0.15 + 0.03 * v, 0.6 * body))
    return sa.finish(sa.reverb(x, 0.5, 0.1), "impact")


def frost_drag(v, rng):
    """A muffled gurgle: bubbles under a descending glide, low-passed."""
    dur = 0.8
    bub = sa.bubbles(dur, 25, rng, 300, 900)
    glide = sa.sine(sa.sweep(700, 300, dur)) * sa.swell(dur, 0.2, 1.3)
    x = sa.lowpass(sa.mix(0.8 * bub, 0.35 * glide), 700 + 100 * v)
    return sa.finish(sa.reverb(x, 0.4, 0.1, damp=1500), "tell")


def frost_bubble_in(v, rng):
    """Inflate: a sine glide with FM shimmer."""
    dur = 0.35
    f = sa.sweep(300 * _step(v, (1.0, 1.122)), 900 * _step(v, (1.0, 1.122)), dur, 1.2)
    x = sa.fm(f, 2.0, 0.6 + 0.3 * sa.swell(dur, 0.25)) * sa.env(dur, (0, 0), (0.02, 1), (0.3, 0.8), (dur, 0))
    x = sa.mix(x, 0.2 * sa.sine(f * 3.01) * sa.env(dur, (0, 0), (0.3, 1), (dur, 0)))
    return sa.finish(sa.reverb(x, 0.12, 0.05), "cast")


def frost_bubble_pop(v, rng):
    """A bead plus a short splash."""
    b = sa.bead(rng, 2600 * _step(v, (1.0, 1.06, 0.94)))
    sp = sa.norm(sa.bandpass(sa.noise(0.1, rng), 800, 3500)) * sa.decay(0.1, 0.02, 0.001)
    x = sa.mix(b, (0.004, 0.4 * sp))
    return sa.finish(x, "impact")


def frost_hook(v, rng):
    """A line whip and plip, then a taut twang."""
    whip = sa.moving_band(0.08, [(0, 3000), (0.08, 6000)], 0.5, rng) * sa.decay(0.08, 0.03, 0.002)
    plip = sa.blip(rng)
    f = sa.note(sa.A, 0) * _step(v, (1.0, 1.122, 1.26))
    twang = sa.sine(sa.glide(0.3, (0, f * 1.04), (0.06, f))) * sa.decay(0.3, 0.09, 0.002)
    twang = twang + 0.3 * sa.sine(f * 2.0, 0.3) * sa.decay(0.3, 0.05, 0.002)
    x = sa.mix(0.8 * whip, (0.07, 0.4 * plip), (0.09, 0.6 * twang))
    return sa.finish(x, "impact")


def frost_breath(v, rng):
    """A soft stream of rising bubbles under a two-note chime."""
    dur = 0.6
    bub = sa.bubbles(dur, 25, rng, 500, 1400)
    chime = sa.mix(sa.glass(sa.note(sa.A, 0), 0.5, 0.2, 0.004), (0.14, sa.glass(sa.note(sa.D, 1) * _step(v, (1.0, 1.0)), 0.45, 0.22, 0.004)))
    x = sa.mix(0.35 * bub * sa.swell(dur, 0.25), 0.8 * chime)
    return sa.finish(sa.reverb(x, 0.4, 0.12), "cast")


EVENTS = [
    event("frost_needle", frost_needle, variants=3, role="impact", subtitle="hit"),
    event("frost_crust", frost_crust, variants=3, role="impact", subtitle="hit"),
    event("frost_creep", frost_creep, variants=3, role="tell", subtitle="tell"),
    event("frost_lock", frost_lock, variants=3, role="impact", subtitle="hit"),
    event("frost_break", frost_break, variants=3, role="impact", subtitle="hit"),
    event("frost_whump", frost_whump, variants=2, role="impact", subtitle="hit"),
    event("frost_hail", frost_hail, variants=3, role="impact", subtitle="hit"),
    event("frost_splat", frost_splat, variants=3, role="impact", subtitle="hit"),
    event("frost_zero", frost_zero, variants=2, role="impact", subtitle="hit"),
    event("frost_lotus", frost_lotus, variants=2, role="cast", subtitle="cast"),
    event("frost_ward", frost_ward, variants=2, role="cast", subtitle="cast"),
    event("frost_mirror", frost_mirror, variants=2, role="cast", subtitle="cast"),
    event("frost_crack", frost_crack, variants=3, role="tell", subtitle="tell"),
    event("frost_tick", frost_tick, variants=2, role="tick", subtitle="tell"),
    event("frost_surge", frost_surge, variants=3, role="impact", subtitle="hit"),
    event("frost_drag", frost_drag, variants=3, role="tell", subtitle="tell"),
    event("frost_bubble_in", frost_bubble_in, variants=2, role="cast", subtitle="cast"),
    event("frost_bubble_pop", frost_bubble_pop, variants=3, role="impact", subtitle="hit"),
    event("frost_hook", frost_hook, variants=3, role="impact", subtitle="hit"),
    event("frost_breath", frost_breath, variants=2, role="cast", subtitle="cast"),
]
