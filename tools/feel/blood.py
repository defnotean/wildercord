"""Blood's kit sounds. Own this file if you own blood: add builders and list them in EVENTS (see tools/feel/core.py).

Blood is wet and close: a slice, a drip, a heartbeat. Its runes each get a voice: a Leech sips, a Gash rips ragged, a Rend
cracks metal, Overdrive races, Heartstopper stops. blood_slice and blood_slice_heavy are the worked examples the kit
shipped with (one recipe from a factory, used for two events): kept as they were.
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


def _lub_dub(gap, f0, f1):
    """A heartbeat: a firm 'lub' and a softer 'dub' `gap` seconds later, both low."""
    # A low body for the chest, and a mid 'thock' (a 240 Hz tone and a knock) so it still reads on small speakers.
    lub = sa.mix(sa.thump(f0, 44, 0.24, 0.05, drive=1.5, knock=0.6), 0.5 * sa.sine(240, 0.14) * sa.decay(0.14, 0.03, 0.002))
    dub = sa.mix(sa.thump(f1, 40, 0.2, 0.04, drive=1.4, knock=0.5), 0.4 * sa.sine(210, 0.12) * sa.decay(0.12, 0.025, 0.002)) * 0.7
    return lub, dub


def blood_sip(v, rng):
    """Leech: a wet sip: two drops of liquid and a low swallow."""
    dur = 0.35
    drops = sa.mix(sa.blip(rng) * 0.7, (0.06, sa.blip(rng) * 0.9))
    gulp = sa.sine(sa.glide(0.2, (0, 150 - 10 * v), (0.2, 95)), 0.2) * sa.decay(0.2, 0.05, 0.004) * 0.8
    wet = sa.moving_band(0.18, [(0, 1600), (0.18, 500)], 0.7, rng) * sa.decay(0.18, 0.04, 0.003) * 0.5
    x = sa.mix(drops, (0.09, gulp), (0.07, wet))
    return sa.finish(sa.reverb(x, 0.25, 0.05, damp=3000), "impact")


def blood_drip(v, rng):
    """A drop of blood landing: one soft 'tik' with a low round body. The aftermath of any bleeding."""
    dur = 0.2
    tik = sa.sine(sa.glide(0.06, (0, 900 + 140 * v), (0.06, 420)), 0.06) * sa.decay(0.06, 0.012, 0.001)
    body = sa.sine(sa.note(sa.A, -1) * (1 + 0.06 * v), dur) * sa.decay(dur, 0.04, 0.002) * 0.5
    wet = sa.norm(sa.bandpass(sa.noise(0.04, rng), 900, 3500)) * sa.decay(0.04, 0.008, 0.001) * 0.4
    return sa.finish(sa.reverb(sa.mix(tik, body, wet), 0.3, 0.08, damp=3500), "tick")


def blood_rip(v, rng):
    """Gash: a ragged tear: cloth and flesh, uneven, a wet crunch at the end."""
    dur = 0.5
    tear = sa.moving_band(dur, [(0, 2200), (0.25, 900), (dur, 500)], 0.9, rng) * sa.chopper(dur, rng, rate=(30, 90), floor=0.3) ** 0.6
    tear *= sa.decay(dur, 0.16, 0.002)
    crunch = sa.thump(190 - 15 * v, 70, 0.18, 0.04, drive=1.6) * 0.8
    splash = sa.norm(sa.bandpass(sa.noise(0.1, rng), 600, 2600)) * sa.decay(0.1, 0.025, 0.001) * 0.6
    x = sa.mix(0.9 * tear, (0.22, crunch), (0.24, splash))
    return sa.finish(sa.reverb(x, 0.35, 0.06, damp=3000), "impact")


def blood_rend(v, rng):
    """Rend: armour giving way: a metal crack, straps snapping, plate ringing off."""
    dur = 0.6
    crack = sa.norm(sa.bandpass(sa.noise(0.06, rng), 1200, 7500)) * sa.decay(0.06, 0.01, 0.0005)
    ring = sa.partials(sa.note((sa.D, sa.FS)[v], 2), dur, ((1.0, 1.0, 1.0), (2.32, 0.5, 0.6), (4.1, 0.3, 0.4), (6.3, 0.15, 0.25)), 0.09)
    snap = sa.norm(sa.bandpass(sa.grains(0.3, 120, rng, length=(0.001, 0.006)), 700, 4500)) * 0.5
    tear = sa.moving_band(0.3, [(0, 1800), (0.3, 700)], 0.8, rng) * sa.decay(0.3, 0.08, 0.002) * 0.6
    x = sa.mix(crack, 0.35 * ring, (0.04, snap), (0.02, tear))
    return sa.finish(sa.reverb(x, 0.4, 0.07), "impact")


def blood_heart(v, rng):
    """A heartbeat, lub-dub, at a resting pace: the tell of every blood rune that has a pulse."""
    lub, dub = _lub_dub(0.2, 68 + 6 * v, 60 + 5 * v)
    x = sa.mix(lub, (0.2, dub))
    return sa.finish(sa.reverb(x, 0.35, 0.06, damp=2500), "pulse")


def blood_race(v, rng):
    """Overdrive, the surge: a heartbeat that runs away with itself, then a rush of breath."""
    dur = 1.0
    beats = []
    t = 0.0
    gap = 0.24
    while t < 0.7:
        lub, dub = _lub_dub(gap, 72, 64)
        beats.append((t, lub * 0.9))
        beats.append((t + gap * 0.42, dub * 0.6))
        t += gap
        gap *= 0.82
    rush = sa.moving_band(dur, [(0, 600), (0.6, 2400), (dur, 900)], 1.0, rng) * sa.swell(dur, 0.6, 1.4) * 0.6
    x = sa.mix(*beats, (0.1, rush))
    return sa.finish(sa.reverb(x, 0.4, 0.07, damp=3000), "impact")


def blood_horn(v, rng):
    """Warcry: a war horn: a broad low note with its fifth, brassy, with a shout of breath at the front."""
    dur = 1.3
    root = sa.note(sa.D, -1)
    horn = sa.soft_saw(root * (1 + 0.006 * sa.sine(5.5, dur)), dur, harmonics=14, limit=3500) * sa.env(dur, (0, 0), (0.12, 1), (0.9, 0.8), (dur, 0))
    fifth = 0.5 * sa.soft_saw(root * 1.498, dur, harmonics=10, limit=3000) * sa.env(dur, (0, 0), (0.2, 0.7), (0.9, 0.6), (dur, 0))
    breath = sa.moving_band(0.3, [(0, 1500), (0.3, 900)], 0.9, rng) * sa.decay(0.3, 0.08, 0.005) * 0.5
    x = sa.mix(horn, fifth, breath)
    return sa.finish(sa.reverb(sa.saturate(x, 1.3), 0.7, 0.12, damp=3500), "impact")


def blood_moss(v, rng):
    """Blood Moss: a soft squelch: moss drinking. Wet, low and slow."""
    dur = 0.7
    squelch = sa.moving_band(dur, [(0, 500), (0.25, 1100), (dur, 380)], 0.8, rng) * sa.env(dur, (0, 0), (0.1, 1), (dur, 0)) * 0.9
    pops = sa.norm(sa.bandpass(sa.grains(dur, 40, rng, length=(0.004, 0.015), shape=[(0, 1), (dur, 0.2)]), 200, 1400)) * 0.6
    body = sa.sine(sa.glide(dur, (0, 130), (dur, 90)), dur) * sa.decay(dur, 0.2, 0.01) * 0.4
    x = sa.mix(squelch, pops, body)
    return sa.finish(sa.reverb(x, 0.4, 0.08, damp=2500), "effect")


def blood_drain(v, rng):
    """Lifesteal, the mark: life pulled through a straw: a falling sine, wet grains, a last low beat."""
    dur = 0.8
    pull = sa.sine(sa.glide(dur, (0, sa.note(sa.A, 1)), (dur, sa.note(sa.A, -1))), dur) * sa.env(dur, (0, 0), (0.05, 1), (dur, 0)) * 0.6
    wet = sa.moving_band(dur, [(0, 2000), (dur, 400)], 0.8, rng) * sa.decay(dur, 0.25, 0.005) * 0.6
    grains = sa.norm(sa.bandpass(sa.grains(dur, 60, rng, shape=[(0, 1), (dur, 0.2)]), 500, 3000)) * 0.4
    beat = sa.thump(66, 42, 0.3, 0.07, drive=1.4) * 0.7
    x = sa.mix(pull, wet, grains, (0.5, beat))
    return sa.finish(sa.reverb(x, 0.4, 0.07, damp=3000), "impact")


def blood_mist(v, rng):
    """Crimson Mist: a slow breath of red fog: a low swell of air and a distant heartbeat."""
    dur = 1.6
    air = sa.moving_band(dur, [(0, 700), (0.7, 1500), (dur, 600)], 1.0, rng) * sa.swell(dur, 0.6, 1.2) * 0.9
    hum = (sa.sine(sa.note(sa.D, -2), dur) + 0.4 * sa.sine(sa.note(sa.A, -2), dur)) * sa.swell(dur, 0.7, 1.2) * 0.4
    lub, dub = _lub_dub(0.2, 62, 56)
    x = sa.mix(air, hum, (0.3, lub * 0.5), (0.5, dub * 0.35), (1.0, lub * 0.4), (1.2, dub * 0.3))
    return sa.finish(sa.reverb(x, 0.8, 0.14, damp=2500), "effect")


def blood_stop(v, rng):
    """Heartstopper, the last beat: a hard thump, then everything falling away into silence."""
    dur = 1.2
    thump = sa.thump(76, 34, 0.5, 0.12, drive=1.9, knock=0.3)
    fall = sa.sine(sa.glide(dur, (0, sa.note(sa.A, 0)), (dur, 40)), dur) * sa.decay(dur, 0.4, 0.004) * 0.5
    hush = sa.moving_band(dur, [(0, 1800), (dur, 200)], 1.0, rng) * sa.decay(dur, 0.25, 0.004) * 0.5
    x = sa.mix(thump, fall, (0.02, hush))
    return sa.finish(sa.reverb(sa.saturate(x, 1.4), 0.9, 0.16, damp=2500), "impact")


def blood_rite(v, rng):
    """Sanguine Rite: a sigil drawn (a dark, rising ring) and a blade dropped through it."""
    dur = 1.0
    sigil = sa.bell(sa.note(sa.D, 1), 0.6, 0.25, ratio=1.41, brightness=1.6) * sa.env(0.6, (0, 0.2), (0.5, 1), (0.6, 0.6))
    cut = sa.moving_band(0.3, [(0, 3600), (0.3, 700)], 0.7, rng) * sa.decay(0.3, 0.07, 0.001)
    thud = sa.thump(120, 50, 0.3, 0.06, drive=1.5) * 0.8
    x = sa.mix(0.6 * sigil, (0.5, cut), (0.52, thud))
    return sa.finish(sa.reverb(x, 0.6, 0.12, damp=3200), "impact")


def blood_tempo(v, rng):
    """Hemomancy: a quick low pulse with a tone in it: a rune ring turning over a heart."""
    dur = 0.6
    pulse = sa.thump(90, 55, 0.25, 0.06, drive=1.3) * 0.8
    tone = sa.bell(sa.note((sa.D, sa.A)[v % 2], 1), dur, 0.14, ratio=2.0, brightness=1.0) * 0.5
    x = sa.mix(pulse, (0.05, tone), (0.24, pulse * 0.5))
    return sa.finish(sa.reverb(x, 0.4, 0.08, damp=3500), "impact")


def blood_gift(v, rng):
    """Transfusion: a warm rising chime: something given. Red at the front, green at the end."""
    dur = 0.8
    a = sa.bell(sa.note(sa.D, 1), 0.5, 0.15, ratio=2.0, brightness=0.9) * 0.6
    b = sa.bell(sa.note(sa.A, 1), 0.6, 0.18, ratio=2.0, brightness=0.8) * 0.6
    c = sa.bell(sa.note(sa.D, 2), 0.7, 0.2, ratio=2.0, brightness=0.7) * 0.6
    drops = sa.mix(sa.blip(rng) * 0.5, (0.05, sa.blip(rng) * 0.4))
    x = sa.mix(a, (0.14, b), (0.28, c), drops)
    return sa.finish(sa.reverb(x, 0.6, 0.14), "effect")


def blood_twang(v, rng):
    """Blood Thread: a taut thread plucked: a bright string with a short wet end. Three tunings for the three hops of a share."""
    dur = 0.5
    freq = sa.note((sa.A, sa.B, sa.D)[v], 1 if v < 2 else 2)
    string = sa.partials(freq, dur, ((1.0, 1.0, 1.0), (2.0, 0.5, 0.7), (3.0, 0.3, 0.5), (4.0, 0.15, 0.4)), 0.1, attack=0.0008)
    pluck = sa.norm(sa.bandpass(sa.noise(0.02, rng), 1800, 6500)) * sa.decay(0.02, 0.004, 0.0002)
    x = sa.mix(0.7 * string, 0.5 * pluck)
    return sa.finish(sa.reverb(x, 0.35, 0.07), "impact")


def blood_triple(v, rng):
    """Dismantle: three hairline cuts a hair apart: quick swishes at rising pitch, each dry and clean."""
    dur = 0.4
    cuts = []
    for i in range(3):
        cuts.append((i * 0.07, sa.moving_band(0.09, [(0, 3400 + 700 * i), (0.09, 1400)], 0.7, rng) * sa.decay(0.09, 0.02, 0.0008)))
    tink = sa.glass(sa.note(sa.B, 3), 0.25, 0.04) * 0.25
    x = sa.mix(*cuts, (0.2, tink))
    return sa.finish(sa.reverb(x, 0.25, 0.05, damp=5000), "impact")


def blood_cleave(v, rng):
    """Cleave: one heavy blade through meat: a low whoosh, a thud with a wet crack, a bone ring."""
    dur = 0.7
    whoosh = sa.moving_band(0.3, [(0, 500), (0.15, 1600), (0.3, 500)], 0.9, rng) * sa.env(0.3, (0, 0), (0.15, 1), (0.3, 0))
    thud = sa.thump(105 - 10 * v, 44, 0.4, 0.09, drive=1.8, knock=0.3)
    crack = sa.norm(sa.bandpass(sa.noise(0.06, rng), 500, 4000)) * sa.decay(0.06, 0.012, 0.0008)
    ring = sa.bell(sa.note(sa.D, 1), 0.5, 0.1, ratio=1.41, brightness=1.0) * 0.25
    x = sa.mix(0.7 * whoosh, (0.14, thud), (0.14, 0.8 * crack), (0.15, ring))
    return sa.finish(sa.reverb(sa.saturate(x, 1.3), 0.5, 0.09, damp=3000), "impact")


EVENTS = [
    event("blood_slice", _slice(0.15, 500, False), variants=2, role="impact", subtitle="hit"),
    event("blood_slice_heavy", _slice(0.35, 350, True), variants=2, role="impact", subtitle="hit"),
    event("blood_sip", blood_sip, variants=2, role="impact", subtitle="hit"),
    event("blood_drip", blood_drip, variants=4, role="tick", subtitle="tell"),
    event("blood_rip", blood_rip, variants=2, role="impact", subtitle="hit"),
    event("blood_rend", blood_rend, variants=2, role="impact", subtitle="hit"),
    event("blood_heart", blood_heart, variants=3, role="pulse", subtitle="field"),
    event("blood_race", blood_race, variants=1, role="impact", subtitle="cast"),
    event("blood_horn", blood_horn, variants=1, role="impact", subtitle="cast"),
    event("blood_moss", blood_moss, variants=1, role="effect", subtitle="hit"),
    event("blood_drain", blood_drain, variants=1, role="impact", subtitle="hit"),
    event("blood_mist", blood_mist, variants=1, role="effect", subtitle="field"),
    event("blood_stop", blood_stop, variants=1, role="impact", subtitle="hit"),
    event("blood_rite", blood_rite, variants=1, role="impact", subtitle="hit"),
    event("blood_tempo", blood_tempo, variants=2, role="impact", subtitle="hit"),
    event("blood_gift", blood_gift, variants=1, role="effect", subtitle="hit"),
    event("blood_twang", blood_twang, variants=3, role="impact", subtitle="hit"),
    event("blood_triple", blood_triple, variants=1, role="impact", subtitle="hit"),
    event("blood_cleave", blood_cleave, variants=2, role="impact", subtitle="hit"),
]


# ---------------------------------------------------------------- a world's resonances: the twists' voices (cast/TwistVfx)

def blood_moon_toll(v, rng):
    """Red Moon: a low, dark gong and a heartbeat under it."""
    dur = 2.4
    gong = sa.bell(sa.note(sa.D, -1), dur, 0.9, ratio=1.41, brightness=1.6) + 0.45 * sa.bell(sa.note(sa.A, 0), dur, 0.6, ratio=2.0, brightness=0.8)
    beat = sa.mix((0.3, sa.thump(70, 45, 0.25, 0.06, drive=1.4, knock=0.6)), (0.55, 0.7 * sa.thump(65, 42, 0.25, 0.06, drive=1.4, knock=0.6)))
    air = sa.moving_band(dur, 900, 0.6, rng) * sa.swell(dur, 0.9) * 0.3
    return sa.finish(sa.reverb(sa.mix(gong, 0.8 * beat, air), 1.3, 0.22), "grand")


def blood_blade_fall(v, rng):
    """Falling Blades: a whoosh down, a blade's ring and the chop as it lands; three, a step apart."""
    dur = 0.7
    whoosh = sa.moving_band(0.25, [(0, 1200), (0.25, 4500)], 0.7, rng) * sa.env(0.25, (0, 0), (0.2, 1), (0.25, 0))
    ring = sa.partials(1800 + 150 * v, dur, ((1.0, 1.0, 1.0), (2.41, 0.5, 0.5), (3.9, 0.3, 0.3)), 0.12)
    chop = sa.norm(sa.bandpass(sa.noise(0.05, rng), 800, 5000)) * sa.decay(0.05, 0.01, 0.0005)
    return sa.finish(sa.reverb(sa.mix(0.6 * whoosh, (0.22, 0.5 * ring), (0.22, 0.8 * chop)), 0.5, 0.1), "impact")


EVENTS += [
    event("blood_moon_toll", blood_moon_toll, variants=1, role="grand", subtitle="field"),
    event("blood_blade_fall", blood_blade_fall, variants=3, role="impact", subtitle="hit"),
]
# ---------------------------------------------------------------- residues: what's left behind


def blood_pulse(v, rng):
    """Bloodmoss: a slow heartbeat under it, lub and dub, wet."""
    dur = 1.0
    lub = sa.thump(72, 46, 0.35, 0.07, drive=1.8, knock=0.9)
    dub = sa.thump(64, 42, 0.3, 0.06, drive=1.6, knock=0.8) * 0.7
    # The body of each beat where small speakers can play it, and the wet of the moss over it.
    body = sa.norm(sa.bandpass(sa.noise(0.12, rng), 250, 900)) * sa.decay(0.12, 0.03, 0.004)
    wet = sa.norm(sa.bandpass(sa.noise(0.1, rng), 700, 2400)) * sa.decay(0.1, 0.02, 0.003)
    x = sa.mix(0.45 * lub, (0.22 + 0.02 * v, 0.45 * dub), body, (0.22 + 0.02 * v, 0.7 * body), (0.02, 0.6 * wet))
    return sa.finish(sa.reverb(x, 0.5, 0.12), "tell", fade_out=0.3)


EVENTS += [event("blood_pulse", blood_pulse, variants=2, role="tell", subtitle="field")]
