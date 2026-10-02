"""Each breathing method's finisher in its own voice: what plays as a finisher lands on an opened foe, over the shared stinger
(aura_finisher, in tools/feel/aura.py) and the method's own impact. Not a part of its own: tools/feel/aura.py adds these to its
events (they live in the aura folder, named aura_finisher_<method>).

    ember       Pyrebrand: a brand's hiss, then a column of flame roaring up and crackling away
    rime        Winterbreak: ice cracking shut round the foe, a held cold ring, then a cascade of shattering glass
    thunder     Skysunder: a buzz drawn tight, then a crack out of the sky and the thunder rolling off
    gale        Windscour: a vortex winding up round the foe, whistles wheeling in it, and a howl as it lifts
    stone       Faultline: a deep crack through the ground, stone bursting up in heavy steps, rubble rolling
    verdant     Thornbloom: roots snapping up and creaking shut, then a chord opening in flower over rustling petals
    hollow      Nullfall: everything drawn in backwards to a point, a hollow implosion, and a ring falling away
    starlit     Starbreak: glitter gathering to a point, a bright bell bursting, sparks of starlight racing out
    hourglass   Hour's End: a clock's tick stopping, the hour tolled on a great bell, and a second cut landing in its ring
    crimson     Heartrend: a heartbeat, a wet tearing cut and its back-cut, and a deep drink drawn in

Everything tonal is in D pentatonic like the rest of the mod. Each is about a second and a half and heard far (a finisher is a
moment for everyone near).
"""
import numpy as np

from feel.core import sa, event

D, E, FS, A, B = sa.D, sa.E, sa.FS, sa.A, sa.B


def _clean(x, role, verb=0.6, wet=0.16, damp=7000.0):
    return sa.finish(sa.highpass(sa.reverb(x, verb, wet, damp=damp), 45), role)


def _crackle(dur, rate, rng, lo=2000, hi=8000, shape=None):
    return sa.norm(sa.bandpass(sa.grains(dur, rate, rng, shape=shape), lo, hi))


def finisher_ember(v, rng):
    dur = 1.7
    hiss = sa.norm(sa.bandpass(sa.noise(0.35, rng), 3500, 9500)) * sa.env(0.35, (0, 0), (0.02, 1), (0.35, 0))
    column = sa.moving_band(1.2, [(0, 300), (0.35, 3800), (1.2, 1200)], 0.9, rng) * sa.env(1.2, (0, 0), (0.25, 1), (1.2, 0))
    roar = sa.norm(sa.lowpass(sa.brown(1.3, rng), 900)) * sa.env(1.3, (0, 0), (0.2, 1), (1.3, 0))
    whump = sa.thump(95, 40, 0.5, 0.12, drive=2.0)
    crackle = _crackle(1.2, 140, rng, shape=[(0, 0.3), (0.3, 1), (1.2, 0.2)])
    x = sa.mix(0.4 * hiss, (0.08, 0.7 * sa.norm(column)), (0.1, 0.55 * roar), (0.08, 0.45 * sa.highpass(whump, 55)), (0.18, 0.4 * crackle))
    return _clean(x, "impact", 0.7, 0.15, 6500)


def finisher_rime(v, rng):
    dur = 1.8
    lock = _crackle(0.25, 260, rng, 3000, 9500, [(0, 0.3), (0.25, 1)])
    ring = sa.mix(sa.glass(sa.note(A, 2), 0.7, 0.35), 0.6 * sa.glass(sa.note(E, 3) * 1.002, 0.7, 0.3))
    shatter = sa.norm(sa.bandpass(sa.grains(1.0, 320, rng, length=(0.002, 0.01), shape=[(0, 1), (1.0, 0.05)]), 2500, 10000))
    shards = sa.sparkle(1.1, 34, rng, [sa.note(d, 3) for d in (D, E, FS, A, B)], tau=(0.03, 0.1), shape=[(0, 1), (1.1, 0.05)])
    thud = sa.thump(120, 55, 0.4, 0.09, drive=1.6, knock=0.3)
    cold = sa.moving_band(1.2, [(0, 2500), (1.2, 700)], 0.8, rng) * sa.decay(1.2, 0.5, 0.01)
    x = sa.mix(0.55 * lock, (0.18, 0.4 * ring), (0.42, 0.75 * shatter), (0.42, 0.35 * sa.norm(shards)), (0.4, 0.45 * sa.highpass(thud, 60)),
               (0.45, 0.3 * sa.norm(cold)))
    return _clean(x, "impact", 0.8, 0.17, 9500)


def finisher_thunder(v, rng):
    dur = 2.2
    tight = sa.soft_saw(sa.sweep(sa.note(D, 1), sa.note(A, 2), 0.25, 1.4), 0.25, harmonics=10) * sa.chopper(0.25, rng, (120, 260), 0.3)
    tight = sa.norm(sa.bandpass(tight, 400, 6000)) * sa.swell(0.25, 0.22)
    crack = sa.norm(sa.bandpass(sa.noise(0.08, rng), 1500, 10000)) * sa.decay(0.08, 0.01, 0.0003)
    boom = sa.thump(90, 40, 1.0, 0.25, drive=2.8, knock=0.6)
    # The roll carried in the band small speakers play: a rumbling grain, chopped, over a little of its low end.
    roll = sa.norm(sa.bandpass(sa.brown(1.8, rng), 260, 1400)) * sa.chopper(1.8, rng, (6, 14), 0.4) * sa.env(1.8, (0, 0), (0.08, 1), (1.8, 0))
    low = sa.norm(sa.lowpass(sa.brown(1.4, rng), 300)) * sa.env(1.4, (0, 0), (0.1, 1), (1.4, 0))
    snap = sa.norm(sa.bandpass(sa.noise(0.3, rng), 700, 4000)) * sa.decay(0.3, 0.06, 0.001)
    sizzle = _crackle(0.6, 200, rng, 3000, 9000, [(0, 1), (0.6, 0)])
    x = sa.mix(0.35 * tight, (0.24, 0.9 * crack), (0.24, 0.6 * snap), (0.25, 0.35 * sa.highpass(boom, 60)), (0.3, 0.6 * roll), (0.3, 0.15 * low),
               (0.26, 0.3 * sizzle))
    return _clean(x, "impact", 1.0, 0.2, 6000)


def finisher_gale(v, rng):
    dur = 1.8
    t = sa.timeline(1.3)
    vortex = sa.moving_band(1.3, [(0, 500), (0.5, 3000), (1.3, 1600)], 0.8, rng) * sa.env(1.3, (0, 0), (0.45, 1), (1.3, 0))
    beat = 0.6 + 0.4 * np.sin(2 * np.pi * t * (4 + 6 * t))
    whistles = sa.mix(*[(0.1 + 0.18 * k, (0.5 - 0.05 * k) * sa.sine(sa.sweep(sa.note(*n), sa.note(*n) * 1.4, 0.35, 0.6), 0.35)
                         * sa.env(0.35, (0, 0), (0.15, 1), (0.35, 0))) for k, n in enumerate(((A, 2), (D, 3), (FS, 3), (A, 3)))])
    howl = sa.soft_saw(sa.glide(0.8, (0, sa.note(D, 1)), (0.8, sa.note(A, 1))), harmonics=6) * sa.swell(0.8, 0.5)
    x = sa.mix(0.75 * sa.norm(vortex) * beat, 0.3 * whistles, (0.5, 0.2 * sa.norm(sa.bandpass(howl, 300, 3000))))
    return _clean(x, "impact", 0.8, 0.17, 8000)


def finisher_stone(v, rng):
    dur = 2.0
    crack = sa.norm(sa.bandpass(sa.noise(0.18, rng), 600, 4000)) * sa.decay(0.18, 0.03, 0.0006)
    split = sa.thump(110, 45, 0.8, 0.2, drive=2.8, knock=0.8)
    steps = sa.mix(*[(0.12 + 0.11 * k + rng.uniform(-0.015, 0.015), (0.75 - 0.08 * k) * sa.thump(170 - 12 * k, 80, 0.3, 0.06, drive=2.4, knock=0.8))
                     for k in range(5)])
    # Each stone breaking the surface: a crunch in the band small speakers play.
    crunches = sa.mix(*[(0.12 + 0.11 * k, (0.6 - 0.06 * k) * sa.norm(sa.bandpass(sa.noise(0.12, rng), 300, 2400)) * sa.decay(0.12, 0.03, 0.0008))
                        for k in range(5)])
    grit = sa.norm(sa.bandpass(sa.grains(1.2, 110, rng, length=(0.003, 0.014), shape=[(0, 1), (1.2, 0.1)]), 350, 2600))
    rumble = sa.norm(sa.bandpass(sa.brown(1.4, rng), 120, 700)) * sa.env(1.4, (0, 0), (0.1, 1), (1.4, 0))
    x = sa.mix(0.75 * crack, 0.35 * sa.highpass(split, 60), 0.35 * sa.highpass(steps, 80), 0.6 * crunches, (0.1, 0.55 * grit), (0.05, 0.25 * rumble))
    return _clean(x, "impact", 0.7, 0.15, 6000)


def _woodpop(rng, f=None):
    f = f or rng.uniform(380, 620)
    knock = sa.sine(sa.sweep(f, f * 0.6, 0.08, 0.5), 0.08) * sa.decay(0.08, 0.014, 0.0005)
    crack = sa.norm(sa.bandpass(sa.noise(0.05, rng), 1200, 5000)) * sa.decay(0.05, 0.006, 0.0003)
    return sa.mix(0.8 * knock, 0.5 * crack)


def finisher_verdant(v, rng):
    dur = 2.0
    roots = sa.mix(*[(0.02 + 0.06 * k + rng.uniform(-0.015, 0.015), (0.7 - 0.05 * k) * _woodpop(rng, 240 + 35 * k)) for k in range(7)])
    tone = sa.soft_saw(sa.sweep(110, 160, 0.6, 0.6), 0.6, harmonics=8) * sa.chopper(0.6, rng, (25, 70), 0.3)
    creak = sa.norm(sa.bandpass(tone, 180, 2400)) * sa.swell(0.6, 0.4)
    bloom = sa.mix(*[(0.06 * i, (1 - 0.12 * i) * sa.bell(sa.note(*n), 1.3, 0.65, 2.0, 0.9, attack=0.06))
                     for i, n in enumerate(((D, 1), (FS, 1), (A, 1), (D, 2), (FS, 2)))])
    petals = sa.norm(sa.bandpass(sa.grains(1.2, 90, rng, length=(0.001, 0.004), shape=[(0, 0.4), (0.4, 1), (1.2, 0.1)]), 2500, 8000))
    x = sa.mix(0.55 * roots, (0.1, 0.3 * creak), (0.5, 0.6 * sa.norm(sa.chorus(bloom, voices=2))), (0.5, 0.25 * petals))
    return _clean(x, "impact", 0.9, 0.18, 8500)


def finisher_hollow(v, rng):
    dur = 2.0
    pull = sa.reverse(sa.moving_band(0.45, [(0, 5000), (0.45, 500)], 0.9, rng) * sa.decay(0.45, 0.16, 0.004))
    sink = sa.sine(sa.sweep(sa.note(A, 1), sa.note(D, -1), 0.45, 0.7), 0.45) * sa.swell(0.45, 0.4)
    boom = sa.thump(65, 24, 1.0, 0.26, drive=2.4, knock=0.2)
    ring = sa.partials(sa.note(D, -1), 1.4, ((1.0, 1.0, 1.0), (2.0, 0.5, 0.7), (3.01, 0.3, 0.5)), 0.5)
    tail = sa.reverse(sa.moving_band(0.5, [(0, 2500), (0.5, 400)], 0.8, rng) * sa.decay(0.5, 0.2, 0.004))
    x = sa.mix(0.6 * sa.norm(pull), 0.3 * sink, (0.45, 0.7 * sa.highpass(boom, 35)), (0.5, 0.35 * sa.lowpass(ring, 1500)),
               (0.5, 0.3 * sa.norm(sa.reverse(tail))))
    return _clean(x, "impact", 1.2, 0.22, 3800)


def finisher_starlit(v, rng):
    dur = 2.0
    gather = sa.sparkle(0.35, 40, rng, [sa.note(*n) for n in ((D, 2), (FS, 2), (A, 2), (D, 3), (FS, 3))], tau=(0.02, 0.05),
                        shape=[(0, 0.3), (0.35, 1)], rising=True)
    bell = sa.mix(sa.bell(sa.note(D, 2), 1.4, 0.6, 2.0, 2.2), (0.01, 0.7 * sa.bell(sa.note(A, 2), 1.3, 0.55, 2.0, 2.2)),
                  (0.02, 0.5 * sa.bell(sa.note(FS, 3), 1.2, 0.5, 2.0, 2.4)))
    pop = sa.thump(180, 90, 0.2, 0.05, drive=1.5, knock=0.4)
    burst = sa.sparkle(1.2, 36, rng, [sa.note(d, 3) for d in (D, E, FS, A, B)], tau=(0.04, 0.14), shape=[(0, 1), (1.2, 0.05)])
    x = sa.mix(0.35 * sa.norm(gather), (0.32, 0.7 * sa.norm(bell)), (0.32, 0.4 * sa.highpass(pop, 70)), (0.34, 0.35 * sa.norm(burst)))
    return _clean(x, "impact", 1.0, 0.2, 10500)


def finisher_hourglass(v, rng):
    dur = 2.4
    ticks = sa.mix(*[(0.09 * k, sa.tick(sa.note(A, 2) * (1 - 0.03 * k), rng, 0.8 - 0.1 * k)) for k in range(4)])
    toll = sa.mix(sa.clock_bell(sa.note(D, 0), 2.0, 0.85), 0.6 * sa.clock_bell(sa.note(A, 0), 1.9, 0.75), 0.4 * sa.clock_bell(sa.note(D, 1), 1.8, 0.65))
    boom = sa.thump(85, 34, 0.6, 0.16, drive=1.8, knock=0.3)
    cut = sa.moving_band(0.22, [(0, 1300), (0.07, 5200), (0.22, 1100)], 0.7, rng) * sa.env(0.22, (0, 0), (0.07, 1), (0.22, 0))
    x = sa.mix(0.45 * sa.norm(ticks), (0.36, 0.65 * sa.norm(toll)), (0.36, 0.4 * sa.highpass(boom, 40)), (0.86, 0.5 * sa.norm(cut)),
               (0.88, 0.25 * sa.clock_bell(sa.note(A, 1), 0.6, 0.22)))
    return _clean(x, "impact", 1.0, 0.2, 9000)


def _beat(rng, f0=64, strength=1.0):
    lub = sa.thump(f0, f0 * 0.55, 0.28, 0.07, drive=1.8, knock=0.05)
    dub = sa.thump(f0 * 1.12, f0 * 0.6, 0.26, 0.06, drive=1.8, knock=0.05)
    return strength * sa.mix(0.9 * lub, (0.17, 0.75 * dub))


def _wet(dur, rng, lo=380, hi=1600, peak=0.3):
    swish = sa.moving_band(dur, [(0, lo), (dur * peak, hi), (dur, lo)], 0.9, rng) * sa.env(dur, (0, 0), (dur * peak, 1), (dur, 0))
    return sa.saturate(sa.norm(swish), 1.7)


def finisher_crimson(v, rng):
    dur = 2.0
    beat = _beat(rng, 62)
    tear = _wet(0.32, rng, 450, 2600)
    back = _wet(0.28, rng, 400, 2000)
    flesh = sa.norm(sa.bandpass(sa.noise(0.2, rng), 280, 1300)) * sa.decay(0.2, 0.06, 0.001)
    drink = sa.reverse(sa.moving_band(0.7, [(0, 1800), (0.7, 300)], 0.9, rng) * sa.decay(0.7, 0.3, 0.004))
    swell = sa.sine(sa.sweep(sa.note(D, -1), sa.note(A, -1), 0.8, 0.6), 0.8) * sa.swell(0.8, 0.6)
    x = sa.mix(0.5 * sa.highpass(beat, 45), 0.75 * tear, (0.06, 0.55 * flesh), (0.15, 0.6 * back), (0.5, 0.45 * sa.norm(drink)), (0.5, 0.25 * swell))
    return _clean(x, "impact", 0.7, 0.15, 5500)


FINISHER_EVENTS = [
    event("aura_finisher_ember", finisher_ember, role="impact", subtitle="hit", attenuation=32),
    event("aura_finisher_rime", finisher_rime, role="impact", subtitle="hit", attenuation=32),
    event("aura_finisher_thunder", finisher_thunder, role="impact", subtitle="hit", attenuation=40),
    event("aura_finisher_gale", finisher_gale, role="impact", subtitle="hit", attenuation=32),
    event("aura_finisher_stone", finisher_stone, role="impact", subtitle="hit", attenuation=32),
    event("aura_finisher_verdant", finisher_verdant, role="impact", subtitle="hit", attenuation=32),
    event("aura_finisher_hollow", finisher_hollow, role="impact", subtitle="hit", attenuation=32),
    event("aura_finisher_starlit", finisher_starlit, role="impact", subtitle="hit", attenuation=32),
    event("aura_finisher_hourglass", finisher_hourglass, role="impact", subtitle="hit", attenuation=32),
    event("aura_finisher_crimson", finisher_crimson, role="impact", subtitle="hit", attenuation=32),
]
