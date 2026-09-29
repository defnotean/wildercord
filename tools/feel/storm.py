"""Storm's kit sounds: one per verb, so no two storm runes sound alike (docs/audit/spell-feel-storm-earth.md, section 5).

Storm is static and air: dry crackle, cracks, rolls and whines. Tonal parts sit in D major pentatonic.
"""
import numpy as np

from feel.core import sa, event


def fit(x, dur):
    """Pads or trims a sound to exactly `dur` seconds (grains and filters can run a little long)."""
    n = sa.samples(dur)
    x = np.asarray(x, dtype=float)
    return np.pad(x, (0, max(0, n - len(x))))[:n]


def grains(dur, *args, **kwargs):
    return fit(sa.grains(dur, *args, **kwargs), dur)

DEG = (sa.D, sa.FS, sa.A)


def _crackle(dur, rng, rate=180, lo=2000, hi=7500, shape=None):
    return sa.norm(sa.bandpass(grains(dur, rate, rng, length=(0.0004, 0.0015), shape=shape or [(0, 1), (dur, 0)]), lo, hi))


def storm_zap(v, rng):
    """S1: a dry tick of static with no rumble; three variants a step apart (Shock's arc, Surge's arc, Stormheart's answer)."""
    dur = 0.22
    f = sa.note(DEG[v], 2)
    zap = sa.fm(sa.sweep(f * 3.2, f, dur, 0.4), 1.5, 2.2 * sa.decay(dur, 0.04), dur) * sa.decay(dur, 0.045, 0.0005)
    x = sa.lowpass(sa.mix(0.55 * _crackle(dur, rng, 180, 1500, 6000, shape=[(0, 1), (0.08, 0.3), (dur, 0)]), 0.5 * zap), 6500)
    return sa.finish(sa.reverb(x, 0.25, 0.06), "impact")


def storm_clamp(v, rng):
    """S2: a buzz that cuts to a hard thunk, then a beat of quiet (a stun)."""
    buzz_t = 0.12
    buzz = sa.lowpass(sa.soft_saw(sa.note(sa.D, -1 + v), buzz_t, harmonics=10), 3000) * sa.chopper(buzz_t, rng) * sa.env(buzz_t, (0, 0), (0.02, 1), (buzz_t, 1))
    thunk = sa.thump(160, 60, 0.25, 0.05, drive=1.8)
    x = sa.mix(0.5 * buzz, (buzz_t, 0.9 * thunk), (buzz_t, 0.4 * sa.tick(1800, rng)))
    return sa.finish(sa.reverb(x, 0.3, 0.05), "impact")


def storm_crack(v, rng):
    """S3: the sky strike: one hard broadband crack with almost no tail."""
    dur = 0.3
    crack = sa.norm(sa.bandpass(sa.noise(dur, rng), 500, 8500)) * sa.decay(dur, 0.011 + 0.003 * v, 0.0002)
    body = sa.thump(110, 50, dur, 0.05, drive=1.6)
    x = sa.highpass(sa.mix(1.0 * crack, 0.3 * body, 0.3 * _crackle(dur, rng, 240, 1500, 6000)), 40)
    return sa.finish(sa.saturate(x, 1.3), "impact")


def storm_roll(v, rng):
    """S3b: the roll that follows a strike (played a few ticks after the crack, as real thunder lags its flash)."""
    dur = 1.1
    roll = sa.moving_band(dur, [(0, 1300), (dur, 380)], 1.0, rng) * sa.swell(dur, 0.12) * sa.decay(dur, 0.35, 0.05)
    boom = sa.norm(sa.lowpass(sa.brown(dur, rng), 200)) * sa.swell(dur, 0.15) * sa.decay(dur, 0.4, 0.05)
    x = sa.mix(0.6 * roll, 0.8 * boom)
    return sa.finish(sa.reverb(x, 1.0, 0.2, damp=3000), "effect")


def storm_boom(v, rng):
    """S4: a round whump rather than a crack (a clap, a shockwave)."""
    dur = 0.8
    body = sa.thump(95 - 8 * v, 40, dur, 0.14, drive=2.2)
    soft = sa.norm(sa.bandpass(sa.noise(0.2, rng), 400, 4000)) * sa.decay(0.2, 0.02, 0.001)
    air = sa.norm(sa.lowpass(sa.noise(dur, rng), 300)) * sa.decay(dur, 0.2, 0.005)
    mid = sa.norm(sa.bandpass(sa.noise(dur, rng), 300, 1800)) * sa.decay(dur, 0.12, 0.003)
    x = sa.mix(0.9 * body, 0.5 * soft, 0.3 * air, 0.6 * mid)
    return sa.finish(sa.reverb(sa.saturate(x, 1.4), 0.8, 0.14, damp=3000), "impact")


def storm_gale(v, rng):
    """S5: a strike that turns into wind (Tempest's fling)."""
    dur = 0.75
    crack = sa.norm(sa.bandpass(sa.noise(0.2, rng), 800, 8000)) * sa.decay(0.2, 0.012, 0.0003)
    whoosh = sa.moving_band(dur, [(0, 500), (0.45, 3000), (dur, 1400)], 1.2, rng) * sa.swell(dur, 0.4)
    x = sa.mix(0.8 * crack, (0.05, 0.8 * whoosh))
    return sa.finish(sa.reverb(x, 0.6, 0.12), "impact")


def storm_cry(v, rng):
    """S6: a raptor's scream from above (the Thunderbird arriving)."""
    dur = 0.55
    f = sa.glide(dur, (0, 2200), (0.15, 2400), (dur, 1400)) * (1 + 0.02 * sa.jitter(dur, rng, 20))
    cry = sa.fm(f, 1.5, 1.4, dur) * sa.env(dur, (0, 0), (0.04, 1), (0.3, 0.7), (dur, 0))
    x = sa.mix(0.6 * cry, 0.25 * _crackle(dur, rng, 90))
    return sa.finish(sa.reverb(x, 0.6, 0.18), "cast")


def storm_dive(v, rng):
    """S6b: a falling whistle into a snap (a Thunderbird's dive)."""
    dur = 0.4
    whistle = sa.sine(sa.sweep(3000, 800, dur, 1.2), dur) * sa.env(dur, (0, 0), (0.05, 0.8), (0.3, 1), (0.32, 0))
    snap = sa.norm(sa.bandpass(sa.noise(0.08, rng), 2000, 8000)) * sa.decay(0.08, 0.008, 0.0003)
    x = sa.mix(0.4 * whistle, (0.3, 0.9 * snap))
    return sa.finish(sa.reverb(x, 0.3, 0.08), "tell")


def storm_hum(v, rng):
    """S7: a faint hum with a flutter (the Thunderbird's wings)."""
    dur = 1.0
    hum = sa.lowpass(sa.soft_saw(100, dur, harmonics=6), 600) * sa.chopper(dur, rng, rate=(10, 14), floor=0.3)
    x = sa.mix(0.5 * hum, 0.2 * _crackle(dur, rng, 30)) * sa.env(dur, (0, 0), (0.2, 1), (0.8, 1), (dur, 0))
    return sa.finish(x, "effect")


def storm_cloud(v, rng):
    """S8: a low swell with rain under it (a thunderhead gathering)."""
    dur = 2.0
    rumble = sa.norm(sa.lowpass(sa.brown(dur, rng), 250)) * sa.swell(dur, 0.9) * sa.decay(dur, 1.2, 0.3)
    rain = sa.norm(sa.bandpass(grains(dur, 400, rng, length=(0.001, 0.003)), 2000, 6000)) * sa.swell(dur, 1.1)
    mid = sa.moving_band(dur, [(0, 700), (dur, 400)], 0.8, rng) * sa.swell(dur, 0.9)
    x = sa.mix(0.6 * rumble, 0.3 * rain, 0.4 * mid)
    return sa.finish(sa.reverb(x, 1.0, 0.2, damp=3000), "effect")


def storm_pluck(v, rng):
    """S9: a taut wire struck (one Stormweave mark); variants D E F# A, so four marks play a phrase."""
    dur = 0.45
    f = sa.note((sa.D, sa.E, sa.FS, sa.A)[v], 1)
    pluck = sa.partials(f, dur, [(1, 1.0, 1.0), (2, 0.5, 0.6), (3, 0.3, 0.4), (5, 0.12, 0.25)], 0.12)
    buzz = 0.15 * _crackle(dur, rng, 60, 3000, 7000)
    return sa.finish(sa.reverb(sa.mix(pluck, buzz), 0.5, 0.12), "tell")


def storm_tick(v, rng):
    """S10: the clock's ratchet."""
    return sa.finish(sa.tick(1500 + 200 * v, rng) + 0.4 * sa.tick(3200, rng), "tick")


def storm_hour(v, rng):
    """S10: the clock's hour: a bell with a crack under it."""
    dur = 0.9
    bell = sa.clock_bell(sa.note(sa.D, 1), dur, 0.35)
    crack = sa.norm(sa.bandpass(sa.noise(0.2, rng), 600, 8000)) * sa.decay(0.2, 0.012, 0.0003)
    return sa.finish(sa.reverb(sa.mix(0.7 * bell, 0.6 * crack), 0.8, 0.15), "impact")


def storm_tear(v, rng):
    """S11: black lightning: a crack that unzips, a sub drop and a short dark ring (Riftbolt)."""
    dur = 0.75
    crack = sa.norm(sa.bandpass(sa.noise(dur, rng), 500, 8000)) * sa.decay(dur, 0.02, 0.0003)
    zip_ = crack
    sub = sa.sine(sa.sweep(200, 40, dur, 1.0), dur) * sa.decay(dur, 0.25, 0.01)
    ring = sa.bell(sa.note(sa.A, 0), dur, 0.2, ratio=1.41)
    x = sa.mix(0.8 * zip_, 0.5 * sub, 0.4 * ring)
    return sa.finish(sa.reverb(x, 0.7, 0.14, damp=2500), "impact")


def storm_sizzle(v, rng):
    """S12: a superheated hiss, steady rather than jagged (Plasma): a soft fwoop into a crackle."""
    dur = 0.6
    fwoop = sa.moving_band(dur, [(0, 900), (0.25, 5000), (dur, 3500)], 0.7, rng) * sa.env(dur, (0, 0), (0.2, 1), (dur, 0))
    crackle = _crackle(dur, rng, 320, 3000, 9000, shape=[(0, 0), (0.2, 1), (dur, 0.2)])
    hum = sa.sine(sa.note(sa.A, 0), dur) * sa.env(dur, (0, 0), (0.2, 0.3), (dur, 0))
    x = sa.mix(0.6 * fwoop, 0.45 * crackle, 0.2 * hum)
    return sa.finish(sa.reverb(x, 0.4, 0.08), "impact")


def storm_flash(v, rng):
    """S13, short: the whine of a flash before its crack."""
    return _whine(0.22, rng)


def storm_whine(v, rng):
    """S13: a rising whine and a snap (charging up)."""
    return _whine(0.5, rng)


def _whine(dur, rng):
    f = sa.glide(dur, (0, sa.note(sa.D, -1)), (dur * 0.85, sa.note(sa.D, 1)), (dur, sa.note(sa.D, 1)))
    whine = sa.lowpass(sa.soft_saw(f, dur, harmonics=8), 5000) * sa.swell(dur, dur * 0.85)
    snap = sa.norm(sa.bandpass(sa.noise(0.06, rng), 2500, 8000)) * sa.decay(0.06, 0.006, 0.0003)
    x = sa.mix(0.4 * whine, (dur * 0.85, 0.7 * snap))
    return sa.finish(sa.reverb(x, 0.3, 0.08), "cast")


def storm_magnet(v, rng):
    """S14: a metallic hum that leans in: two sines beating at 3 Hz, swelling, and a lodestone click."""
    dur = 1.0
    f = sa.note(sa.A, 0)
    hum = (sa.soft_saw(f, dur, harmonics=6) + sa.soft_saw(f + 3, dur, harmonics=6)) * sa.swell(dur, 0.6)
    click = sa.tick(1200, rng)
    x = sa.mix(0.45 * hum, 0.5 * click, 0.15 * _crackle(dur, rng, 40))
    return sa.finish(sa.reverb(x, 0.5, 0.1), "effect")


def storm_pip(v, rng):
    """S15: a tiny pip of static (a spark humming, a charged stance)."""
    dur = 0.14
    pip = sa.sine(sa.note(DEG[v], 3), dur) * sa.decay(dur, 0.02, 0.001)
    x = sa.mix(0.3 * pip, 0.5 * _crackle(dur, rng, 150))
    return sa.finish(x, "tick")


def storm_sun(v, rng):
    """S16: a warm bell swell with nothing electric in it (Ripple's sunlight)."""
    dur = 0.7
    chord = sa.bell(sa.note(sa.D, 1), dur, 0.3, ratio=2.0, brightness=0.8) + 0.7 * sa.bell(sa.note(sa.A, 1), dur, 0.25, ratio=2.0, brightness=0.8)
    x = sa.chorus(chord * sa.env(dur, (0, 0), (0.15, 1), (dur, 0)))
    return sa.finish(sa.reverb(x, 0.8, 0.2), "impact")


EVENTS = [
    event("storm_zap", storm_zap, variants=3, role="impact", subtitle="hit"),
    event("storm_clamp", storm_clamp, variants=2, role="impact", subtitle="hit"),
    event("storm_crack", storm_crack, variants=3, role="impact", subtitle="hit", attenuation=48),
    event("storm_roll", storm_roll, variants=2, role="effect", subtitle="field", attenuation=64),
    event("storm_boom", storm_boom, variants=2, role="impact", subtitle="hit", attenuation=32),
    event("storm_gale", storm_gale, variants=2, role="impact", subtitle="hit"),
    event("storm_cry", storm_cry, variants=2, role="cast", subtitle="cast", attenuation=32),
    event("storm_dive", storm_dive, variants=3, role="tell", subtitle="tell"),
    event("storm_hum", storm_hum, variants=1, role="effect", subtitle="field"),
    event("storm_cloud", storm_cloud, variants=2, role="effect", subtitle="field", attenuation=32),
    event("storm_pluck", storm_pluck, variants=4, role="tell", subtitle="tell"),
    event("storm_tick", storm_tick, variants=3, role="tick", subtitle="tell"),
    event("storm_hour", storm_hour, variants=1, role="impact", subtitle="hit", attenuation=32),
    event("storm_tear", storm_tear, variants=2, role="impact", subtitle="hit"),
    event("storm_sizzle", storm_sizzle, variants=2, role="impact", subtitle="hit"),
    event("storm_whine", storm_whine, variants=2, role="cast", subtitle="cast"),
    event("storm_flash", storm_flash, variants=2, role="cast", subtitle="tell"),
    event("storm_magnet", storm_magnet, variants=2, role="effect", subtitle="field"),
    event("storm_pip", storm_pip, variants=3, role="tick", subtitle="tell"),
    event("storm_sun", storm_sun, variants=2, role="impact", subtitle="hit"),
]
