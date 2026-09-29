"""Blood's kit sounds. Own this file if you own blood: add builders and list them in EVENTS (see tools/feel/core.py).

The worked example below is one recipe used for two events (a light and a heavy cut): a builder may take its shape from a factory.
"""
from feel.core import sa, event


def _slice(dur, low, thud):
    def build(v, rng):
        cut = sa.moving_band(dur, [(0, 1200), (dur, low)], 0.8, rng) * sa.decay(dur, dur * 0.3, 0.001)
        splash = sa.norm(sa.bandpass(sa.noise(0.08, rng), 700, 3000)) * sa.decay(0.08, 0.02, 0.001)
        body = sa.thump(160 - 15 * v, 60, 0.2, 0.05, drive=1.2) if thud else sa.sine(sa.note(sa.A, -1), 0.05) * sa.decay(0.05, 0.02)
        x = sa.mix(0.9 * cut, (0.03, 0.4 * splash), (0.0, (0.6 if thud else 0.15) * body))
        return sa.finish(sa.reverb(x, 0.35, 0.06, damp=3000), "impact")
    return build


EVENTS = [
    event("blood_slice", _slice(0.15, 500, False), variants=2, role="impact", subtitle="hit"),
    event("blood_slice_heavy", _slice(0.35, 350, True), variants=2, role="impact", subtitle="hit"),
]
