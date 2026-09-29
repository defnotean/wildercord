"""Time's kit sounds: wooden ticks and clock bells, but each verb has its own voice (a fuse, a ledger, a dodge, a
rewind, a stop). Everything tonal sits in D major pentatonic; the ticks are the mod's own clock family.

Names are time_<verb>. Played from cast/TimeFx.java (and the signatures in cast/feel/TimeFeels.java).
"""
import numpy as np

from feel.core import sa, event

TICK_HZ = (2200.0, 2600.0, 3000.0)


def time_tick(v, rng):
    """One dry wooden tick, three pitches: the fuse of a Countdown, the pips of a wound clock."""
    x = sa.tick(TICK_HZ[v % 3], rng, 1.0)
    return sa.finish(sa.reverb(x, 0.25, 0.05), "impact")


def time_strike(v, rng):
    """A low clock bell, a short thud, and the ticks winding down after it: the moment catches up."""
    dur = 1.3
    bell = sa.clock_bell(sa.note((sa.D, sa.A)[v % 2], -1), dur, 0.75)
    thud = sa.thump(160, 70, 0.25, 0.04, 1.2)
    gaps = np.cumsum([0.09, 0.11, 0.14, 0.18, 0.24])
    ticks = [(0.08 + g, sa.tick(2300 - 130 * i, rng, 0.32 * (1 - i / 6))) for i, g in enumerate(gaps)]
    x = sa.mix(bell, 0.45 * thud, *ticks)
    return sa.finish(sa.reverb(x, 1.1, 0.24), "impact")


def time_run(v, rng):
    """Twelve ticks whose gaps shrink from a tenth of a second to a hundredth, rising, into a whirr and a chime: time hurried."""
    n = 12
    t = 0.0
    parts = []
    for i in range(n):
        parts.append((t, sa.tick(1800 + 90 * i, rng, 0.45 + 0.05 * i)))
        t += 0.1 * (0.78 ** i) + 0.012
    whirr = sa.moving_band(t + 0.1, [(0, 900), (t, 4200)], 0.8, rng) * sa.swell(t + 0.1, t * 0.9)
    chime = sa.glass(sa.note(sa.A, 2), 0.7, 0.25)
    x = sa.mix(*parts, 0.16 * whirr, (t, 0.5 * chime))
    return sa.finish(sa.reverb(x, 0.6, 0.15), "effect")


def time_wind(v, rng):
    """A clockwork ratchet winding up with a pawl click, then the spring let go and a soft chime: another's clock turned forward."""
    parts = []
    t = 0.0
    for i in range(9):
        parts.append((t, sa.tick(1500 + 110 * i, rng, 0.6)))
        parts.append((t + 0.035, 0.4 * sa.tick(900 + 60 * i, rng, 0.6)))
        t += 0.085 - 0.004 * i
    spring = sa.sine(sa.sweep(300, 90, 0.4, 0.5), 0.4) * sa.decay(0.4, 0.09, 0.002)
    chime = sa.bell(sa.note(sa.FS, 1), 0.9, 0.4, 2.0, 1.4)
    x = sa.mix(*parts, (t, 0.5 * spring), (t + 0.02, 0.6 * chime))
    return sa.finish(sa.reverb(x, 0.7, 0.18), "effect")


def time_dodge(v, rng):
    """A cloth whip (a band sweeping up in a tenth of a second), a reversed glass chime, a faint ring: a blow that was not there."""
    whip = sa.moving_band(0.18, [(0, 800), (0.12, 6000)], 0.8, rng) * sa.decay(0.18, 0.05, 0.004)
    rev = sa.reverse(sa.glass(sa.note((sa.A, sa.E)[v % 2], 2), 0.35, 0.09))
    ring = sa.glass(sa.note(sa.D, 3), 0.5, 0.16)
    x = sa.mix(0.9 * whip, (0.02, 0.4 * sa.norm(rev)), (0.18, 0.35 * ring))
    return sa.finish(sa.reverb(x, 0.5, 0.15), "effect")


def time_sand(v, rng):
    """A soft hiss of falling grains that thins out and lands in a glass ping: time poured from one glass to another."""
    dur = 0.9
    grains = sa.norm(sa.bandpass(sa.grains(dur, 1200, rng, length=(0.0004, 0.002), shape=[(0, 1), (0.7, 0.8), (dur, 0.1)]), 3000, 9000))
    flutter = 0.5 + 0.5 * np.sin(2 * np.pi * 6 * sa.timeline(len(grains) / sa.SR))
    ping = sa.glass(sa.note((sa.E, sa.A)[v % 2], 2), 0.6, 0.2)
    x = sa.mix(0.7 * grains * flutter[:len(grains)] * sa.env(len(grains) / sa.SR, (0, 0), (0.1, 1), (0.8, 0.7), (dur, 0)), (dur - 0.05, 0.4 * ping))
    return sa.finish(sa.reverb(x, 0.6, 0.15), "effect")


def time_scratch(v, rng):
    """A quill's scratch: one wound written into the ledger."""
    dur = 0.12
    scratch = sa.norm(sa.bandpass(sa.grains(dur, 900, rng, length=(0.0003, 0.0015), shape=[(0, 1), (dur, 0.2)]), 2200, 6500))
    x = sa.mix(0.8 * scratch * sa.decay(len(scratch) / sa.SR, 0.05, 0.002), (0.0, 0.3 * sa.tick(1300 + 150 * (v % 3), rng, 0.5)))
    return sa.finish(x, "tick")


def time_rewind(v, rng):
    """A reversed tick train and a glide down: time running backward, then an arrival."""
    dur = 0.8
    forward = []
    t = 0.0
    for i in range(10):
        forward.append((t, sa.tick(2600 - 90 * i, rng, 0.5)))
        t += 0.06
    rev = sa.reverse(sa.mix(*forward))
    glide = sa.sine(sa.sweep(2400, 300, dur, 0.6), dur) * sa.env(dur, (0, 0), (0.1, 0.5), (dur, 0)) ** 2
    tail = sa.reverse(sa.reverb(sa.clock_bell(sa.note(sa.A, 0), 0.6, 0.3), 0.6, 0.5))[-sa.samples(0.5):]
    x = sa.mix(0.9 * rev, 0.12 * glide, (0.3, 0.5 * sa.norm(tail)), (dur - 0.05, 0.5 * sa.glass(sa.note(sa.D, 2), 0.5, 0.2)))
    return sa.finish(sa.reverb(x, 0.6, 0.16), "effect")


def time_tock(v, rng):
    """A low wooden tock: a payment made on a borrowed moment."""
    x = sa.mix(sa.thump(210 - 20 * (v % 3), 120, 0.2, 0.03, 1.2), 0.5 * sa.tick(900 + 70 * (v % 3), rng, 0.7))
    return sa.finish(x, "impact")


def time_stutter(v, rng):
    """A slice of a tick and a bell repeated three times a step higher, then silence and a soft thud: a second skipped."""
    piece = sa.mix(sa.tick(2400, rng, 0.8), 0.6 * sa.bell(sa.note(sa.A, 1), 0.06, 0.03, 2.0, 1.0))[:sa.samples(0.045)]
    parts = []
    for i, ratio in enumerate((1.0, 1.26, 1.498)):
        stretched = np.interp(np.arange(int(len(piece) / ratio)) * ratio, np.arange(len(piece)), piece)
        parts.append((i * 0.05, stretched))
    thud = sa.thump(120, 55, 0.25, 0.05, 1.2)
    x = sa.mix(*parts, (0.32, 0.6 * thud))
    return sa.finish(sa.reverb(x, 0.4, 0.1), "effect")


def time_stop(v, rng):
    """A bell, then every sound gates out to near silence over a faint held tone: time stops."""
    dur = 1.2
    bell = sa.clock_bell(sa.note(sa.D, 0), dur, 0.9)
    gate = sa.env(dur, (0, 1), (0.22, 1), (0.3, 0.0), (dur, 0.0))
    hold = 0.06 * sa.sine(sa.note(sa.A, 1), dur) * sa.env(dur, (0, 0), (0.3, 1), (dur, 0.6)) ** 2
    x = sa.mix(bell * gate, hold, (0.0, 0.5 * sa.thump(110, 50, 0.35, 0.07, 1.4)))
    return sa.finish(sa.reverb(x, 1.0, 0.2), "effect")


def time_resume(v, rng):
    """The reverse of a stop, and a crack of everything held arriving at once: time moves again."""
    dur = 0.8
    rush = sa.reverse(sa.moving_band(0.45, [(0, 4000), (0.45, 500)], 0.9, rng) * sa.decay(0.45, 0.2, 0.002))
    crack = sa.norm(sa.bandpass(sa.noise(0.08, rng), 600, 6000)) * sa.decay(0.08, 0.012, 0.0004)
    bell = sa.clock_bell(sa.note(sa.A, 0), 0.7, 0.4)
    x = sa.mix(0.6 * rush, (0.44, 0.9 * crack), (0.44, 0.55 * sa.thump(140, 60, 0.4, 0.07, 1.6)), (0.46, 0.4 * bell))
    return sa.finish(sa.reverb(x, 0.9, 0.2), "impact")


def time_doomtick(v, rng):
    """A heavier tick with a sub thump under it: the clock that ends in a burst."""
    x = sa.mix(sa.tick(1500 + 120 * v, rng, 1.0), 0.7 * sa.thump(95, 55, 0.18, 0.03, 1.4))
    return sa.finish(sa.reverb(x, 0.3, 0.05), "impact")


def time_ring(v, rng):
    """A bright blade ring rising into a clear chime: the answer to a blow."""
    ring = sa.glass(sa.note(sa.D, 3), 0.7, 0.3)
    edge = sa.norm(sa.bandpass(sa.noise(0.05, rng), 3000, 9000)) * sa.decay(0.05, 0.008, 0.0004)
    x = sa.mix(ring, 0.5 * edge, (0.05, 0.4 * sa.glass(sa.note(sa.A, 3), 0.5, 0.2)))
    return sa.finish(sa.reverb(x, 0.6, 0.2), "impact")


EVENTS = [
    event("time_tick", time_tick, variants=3, role="impact", subtitle="tell"),
    event("time_strike", time_strike, variants=2, role="impact", subtitle="hit"),
    event("time_run", time_run, variants=1, role="effect", subtitle="cast"),
    event("time_wind", time_wind, variants=1, role="effect", subtitle="cast"),
    event("time_dodge", time_dodge, variants=2, role="effect", subtitle="hit"),
    event("time_sand", time_sand, variants=2, role="effect", subtitle="hit"),
    event("time_scratch", time_scratch, variants=3, role="tick", subtitle="tell"),
    event("time_rewind", time_rewind, variants=1, role="effect", subtitle="cast"),
    event("time_tock", time_tock, variants=3, role="impact", subtitle="tell"),
    event("time_stutter", time_stutter, variants=2, role="effect", subtitle="cast"),
    event("time_stop", time_stop, variants=1, role="effect", subtitle="field"),
    event("time_resume", time_resume, variants=1, role="impact", subtitle="hit"),
    event("time_doomtick", time_doomtick, variants=3, role="impact", subtitle="tell"),
    event("time_ring", time_ring, variants=1, role="impact", subtitle="hit"),
]
