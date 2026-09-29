"""Sounds that belong to no element (names have no element prefix): tells, the HUD, gestures. Owned by the shapes/modifiers/links work.

`tap_release` is used by SpellCaster (a tap cast's release), the others are the pattern for tells: a pitched note played at
the pentatonic ratios (1.0, 1.122, 1.26, 1.498, 1.682, 2.0) makes a scale from one sample.
"""
from feel.core import sa, event


def tap_release(v, rng):
    """A short snap of air and a soft pop: a tap cast leaves the hand (charged casts have `release`)."""
    dur = 0.22
    air = sa.moving_band(dur, [(0, 700), (0.05, 3200), (dur, 1400)], 1.1, rng) * sa.decay(dur, 0.05, 0.004)
    pop = sa.thump(260 - 30 * v, 90, 0.12, 0.02, drive=1.4)
    x = sa.mix(0.8 * air, 0.5 * pop)
    return sa.finish(sa.highpass(sa.reverb(x, 0.4, 0.08), 90), "cast")


def link_ting(v, rng):
    """A link handing on: one glass note at D. Play at 1.122 / 1.26 / 1.498 to climb the scale."""
    dur = 0.6
    x = sa.glass(sa.note(sa.D, 1), dur, 0.25) + 0.3 * sa.bell(sa.note(sa.D, 2), dur, 0.15, 3.0, 1.0, 0.002)
    return sa.finish(sa.reverb(x, 0.6, 0.15, damp=8000), "tell")


def ready_ping(v, rng):
    """The spell's cooldown is over: two quiet rising glass notes."""
    dur = 0.5
    x = sa.mix(sa.glass(sa.note(sa.A, 1), dur, 0.15), (0.07, 0.8 * sa.glass(sa.note(sa.D, 2), dur, 0.18)))
    return sa.finish(sa.reverb(x, 0.4, 0.1, damp=8000), "ui")


EVENTS = [
    event("tap_release", tap_release, variants=2, role="cast", subtitle="cast"),
    event("link_ting", link_ting, role="tell", subtitle="tell"),
    event("ready_ping", ready_ping, role="ui", subtitle="tell"),
]
