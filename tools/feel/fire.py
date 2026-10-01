"""Fire's kit sounds. Own this file if you own fire: add builders and list them in EVENTS (see tools/feel/core.py).

Fire is dry and quick: a crackle, a tik, a whump. Every rune that is fire has one voice of its own here, so a Flashfire
(a hard white crack) never sounds like an Explode (a shell and a boom) or an Inferno (a slow low roar).
Everything tonal sits in D major pentatonic (sa.note(degree, octave) with the degrees sa.D, sa.E, sa.FS, sa.A, sa.B).

fire_flick and fire_whump are the worked examples the kit shipped with: kept as they were.
"""
from feel.core import sa, event


def fire_flick(v, rng):
    """A single ignition: a dry 'tik' and a short 'ssst'. Three variants, each a step higher up the scale."""
    dur = 0.25
    tik = sa.norm(sa.bandpass(sa.noise(0.03, rng), 2500, 7000)) * sa.decay(0.03, 0.004, 0.0003)
    hiss = sa.moving_band(dur, [(0, 4200), (dur, 2000)], 0.9, rng) * sa.decay(dur, 0.06, 0.002)
    embers = sa.norm(sa.bandpass(sa.grains(dur, 90, rng, shape=[(0, 1), (dur, 0)]), 1500, 6000))
    ping = sa.sine(sa.note((sa.D, sa.E, sa.FS)[v], 2), dur) * sa.decay(dur, 0.05, 0.002)
    x = sa.mix(0.8 * tik, (0.01, 0.5 * hiss), 0.3 * embers, 0.12 * ping)
    return sa.finish(sa.reverb(x, 0.4, 0.08), "impact")


def fire_whump(v, rng):
    """Catching: a soft low 'fwoomp' (low-passed noise, a 65 to 90 Hz thump, ember grains)."""
    dur = 0.6
    body = sa.thump(90 - 12 * v, 55, dur, 0.1, drive=1.4)
    fwoomp = sa.norm(sa.lowpass(sa.noise(dur, rng), 420)) * sa.decay(dur, 0.12, 0.01)
    embers = sa.norm(sa.bandpass(sa.grains(dur, 70, rng, shape=[(0, 1), (0.3, 0.5), (dur, 0)]), 1200, 5000))
    x = sa.mix(0.7 * body, 0.6 * fwoomp, 0.25 * embers)
    return sa.finish(sa.reverb(sa.saturate(x, 1.3), 0.6, 0.1), "impact")


def fire_flare(v, rng):
    """Flashfire: one hard white crack, a quick upward 'fwip' and a glassy ring. No tail: it is over before the smoke."""
    dur = 0.4
    crack = sa.norm(sa.highpass(sa.noise(0.05, rng), 1800)) * sa.decay(0.05, 0.007, 0.0003)
    fwip = sa.moving_band(0.22, [(0, 900), (0.22, 5200)], 0.8, rng) * sa.decay(0.22, 0.05, 0.002)
    ring = sa.bell(sa.note((sa.A, sa.B)[v], 2), dur, 0.09, ratio=3.0, brightness=1.2)
    x = sa.mix(0.9 * crack, (0.005, 0.7 * fwip), (0.01, 0.22 * ring))
    return sa.finish(sa.reverb(x, 0.25, 0.05), "impact")


def fire_blast(v, rng):
    """Explode: a shell cracking open, a deep boom, and debris coming down. The one big fire sound."""
    dur = 1.0
    boom = sa.thump(120 - 14 * v, 38, dur, 0.16, drive=1.8, knock=0.35)
    shell = sa.moving_band(0.35, [(0, 3200), (0.35, 500)], 1.0, rng) * sa.decay(0.35, 0.07, 0.001)
    crack = sa.norm(sa.bandpass(sa.noise(0.05, rng), 900, 6500)) * sa.decay(0.05, 0.008, 0.0004)
    debris = sa.norm(sa.bandpass(sa.grains(0.7, 90, rng, shape=[(0, 1), (0.7, 0)]), 500, 4000))
    x = sa.mix(0.9 * boom, 0.8 * shell, 0.9 * crack, (0.08, 0.35 * debris))
    return sa.finish(sa.reverb(sa.saturate(x, 1.5), 0.9, 0.14), "impact")


def fire_meteor_fall(v, rng):
    """Meteor, the fall: a roar that climbs and closes in over 1.2 s, ending where the thud will land."""
    dur = 1.2
    roar = sa.moving_band(dur, [(0, 180), (0.9, 1700), (dur, 2600)], 1.0, rng)
    roar *= sa.env(dur, (0, 0), (0.25, 0.4), (1.0, 1.0), (dur, 0.7)) ** 1.3
    whistle = sa.sine(sa.glide(dur, (0, 420), (dur, 2100))) * sa.env(dur, (0, 0), (0.3, 0.1), (1.05, 0.5), (dur, 0.3)) * 0.35
    rumble = sa.norm(sa.lowpass(sa.noise(dur, rng), 240)) * sa.env(dur, (0, 0.2), (dur, 0.9))
    x = sa.mix(roar, whistle, 0.5 * rumble)
    return sa.finish(sa.reverb(x, 0.5, 0.08), "effect")


def fire_meteor_hit(v, rng):
    """Meteor, the landing: one very low blow, a slab of rubble, and coals settling."""
    dur = 1.5
    blow = sa.thump(85 - 10 * v, 30, dur, 0.28, drive=2.0, knock=0.3)
    rubble = sa.norm(sa.bandpass(sa.grains(1.0, 130, rng, length=(0.002, 0.012), shape=[(0, 1), (1.0, 0)]), 200, 2600))
    roar = sa.norm(sa.lowpass(sa.noise(0.8, rng), 320)) * sa.decay(0.8, 0.2, 0.004)
    coals = sa.norm(sa.bandpass(sa.grains(1.2, 40, rng, shape=[(0, 0.3), (1.2, 1)]), 1500, 5000)) * 0.4
    x = sa.mix(blow, (0.02, 0.9 * roar), (0.03, 0.5 * rubble), (0.25, 0.25 * coals))
    return sa.finish(sa.reverb(sa.saturate(x, 1.4), 1.1, 0.18), "impact")


def fire_field(v, rng):
    """Inferno, one pulse of the field: a low breathing roar with a crackle riding it. Three that can sit side by side."""
    dur = 0.55
    roar = sa.moving_band(dur, [(0, 260 + 30 * v), (0.18, 700), (dur, 320)], 1.1, rng) * sa.env(dur, (0, 0), (0.08, 1), (0.25, 0.5), (dur, 0))
    body = sa.sine(sa.note((sa.D, sa.A, sa.E)[v], -3), dur) * sa.decay(dur, 0.18, 0.01) * 0.6
    crackle = sa.norm(sa.bandpass(sa.grains(dur, 110, rng, shape=[(0, 1), (dur, 0.2)]), 1500, 6500)) * 0.45
    x = sa.mix(roar, body, crackle)
    return sa.finish(sa.reverb(x, 0.4, 0.06), "pulse")


def fire_fuse(v, rng):
    """Primer, lit: a wet-match hiss and a spit of sparks, a small rising whine under it."""
    dur = 0.6
    hiss = sa.moving_band(dur, [(0, 3400), (dur, 6200)], 0.7, rng) * sa.env(dur, (0, 0), (0.04, 1), (0.4, 0.8), (dur, 0))
    sparks = sa.norm(sa.bandpass(sa.grains(dur, 160, rng, shape=[(0, 1), (dur, 0.5)]), 2500, 9000)) * 0.6
    whine = sa.sine(sa.glide(dur, (0, sa.note(sa.A, 1)), (dur, sa.note(sa.A, 2)))) * sa.env(dur, (0, 0), (0.1, 0.25), (dur, 0.05)) * 0.3
    x = sa.mix(0.8 * hiss, sparks, whine)
    return sa.finish(sa.reverb(x, 0.3, 0.05), "tell")


def fire_fuse_tick(v, rng):
    """Primer, a beat of the fuse: a tiny tick that climbs the scale a step per variant, so the fuse can be heard shortening."""
    dur = 0.12
    freq = sa.note((sa.D, sa.FS, sa.A, sa.B)[v], 2)
    knock = sa.sine(freq, dur) * sa.decay(dur, 0.012, 0.0004)
    spark = sa.norm(sa.bandpass(sa.noise(dur, rng), 3000, 9000)) * sa.decay(dur, 0.006, 0.0002)
    return sa.finish(sa.mix(0.8 * knock, 0.5 * spark), "tick")


def fire_stack(v, rng):
    """Kindling, a stack: a warm little 'pup' with one bright note; Java plays it up the scale, one step a stack."""
    dur = 0.3
    note = sa.sine(sa.note(sa.D, 2), dur) * sa.decay(dur, 0.07, 0.002)
    tone = 0.3 * sa.sine(sa.note(sa.D, 3), dur) * sa.decay(dur, 0.04, 0.002)
    puff = sa.moving_band(0.12, [(0, 1600), (0.12, 900)], 0.8, rng) * sa.decay(0.12, 0.03, 0.002)
    ember = sa.norm(sa.bandpass(sa.grains(0.2, 90, rng), 2000, 7000)) * 0.3
    return sa.finish(sa.reverb(sa.mix(note, tone, 0.6 * puff, ember), 0.3, 0.06), "tell")


def fire_hop(v, rng):
    """Firestorm, a leap between creatures: a quick zip of flame, a pop where it lands."""
    dur = 0.3
    zip_ = sa.moving_band(0.2, [(0, 1200 + 400 * v), (0.2, 4200)], 0.7, rng) * sa.decay(0.2, 0.05, 0.002)
    pop = sa.thump(220 - 20 * v, 90, 0.18, 0.04, drive=1.3) * 0.7
    crackle = sa.norm(sa.bandpass(sa.grains(dur, 130, rng, shape=[(0, 0.2), (0.15, 1), (dur, 0)]), 1800, 7000)) * 0.4
    x = sa.mix(zip_, (0.12, pop), crackle)
    return sa.finish(sa.reverb(x, 0.3, 0.05), "impact")


def fire_launch(v, rng):
    """Blazecall, a fireball leaving: a hard whoosh with a cough of flame at the front. Each of the three a little higher."""
    dur = 0.55
    whoosh = sa.moving_band(dur, [(0, 500 + 120 * v), (0.12, 2100 + 200 * v), (dur, 700)], 1.0, rng) * sa.env(dur, (0, 0), (0.03, 1), (0.25, 0.5), (dur, 0)) ** 1.4
    cough = sa.thump(160, 70, 0.2, 0.05, drive=1.4) * 0.8
    embers = sa.norm(sa.bandpass(sa.grains(dur, 80, rng, shape=[(0, 1), (dur, 0)]), 1200, 5500)) * 0.4
    x = sa.mix(whoosh, cough, embers)
    return sa.finish(sa.reverb(x, 0.4, 0.07), "impact")


def fire_steam(v, rng):
    """Steam: a long soft hiss that billows, with a bubbling rumble below."""
    dur = 1.1
    hiss = sa.moving_band(dur, [(0, 5200), (0.4, 3600), (dur, 6800)], 0.9, rng) * sa.env(dur, (0, 0), (0.12, 1), (0.6, 0.7), (dur, 0))
    bubbles = sa.norm(sa.bandpass(sa.grains(dur, 45, rng, length=(0.004, 0.02), shape=[(0, 0.6), (dur, 0.2)]), 250, 1100)) * 0.5
    x = sa.mix(0.8 * hiss, bubbles)
    return sa.finish(sa.reverb(x, 0.6, 0.1), "effect")


def fire_sun(v, rng):
    """Sunscorch: a lens ring that focuses, then a white column of heat: bright bell, sizzle, a low warm swell."""
    dur = 0.95
    lens = sa.glass(sa.note(sa.B, 2), 0.3, 0.05) * sa.env(0.3, (0, 0.2), (0.28, 1), (0.3, 0))
    beam = sa.moving_band(0.7, [(0, 2500), (0.7, 6500)], 0.9, rng) * sa.decay(0.7, 0.25, 0.01)
    warm = sa.sine(sa.note(sa.D, -1), 0.8) * sa.decay(0.8, 0.3, 0.02) * 0.7
    sizzle = sa.norm(sa.bandpass(sa.grains(0.7, 200, rng, shape=[(0, 1), (0.7, 0.2)]), 3000, 9000)) * 0.4
    x = sa.mix(0.5 * lens, (0.28, 0.8 * beam), (0.28, warm), (0.3, sizzle))
    return sa.finish(sa.reverb(x, 0.5, 0.08), "impact")


def fire_soul(v, rng):
    """Soulfire: a cold breathy whisper with a hollow note rising through it: fire that is not warm."""
    dur = 1.0
    breath = sa.moving_band(dur, [(0, 1800), (0.5, 3200), (dur, 2000)], 0.5, rng) * sa.swell(dur, 0.3, 1.5)
    hollow = sa.fm(sa.glide(dur, (0, sa.note(sa.A, 1)), (dur, sa.note(sa.D, 2))), 1.0, 0.8 * sa.env(dur, (0, 1), (dur, 0.2))) * sa.swell(dur, 0.35, 1.5)
    x = sa.mix(0.7 * breath, 0.35 * hollow)
    return sa.finish(sa.reverb(sa.chorus(x, 3, 0.004, 0.4), 0.7, 0.14), "effect")


def fire_brand(v, rng):
    """Cinderbrand: a hot iron on skin: a sharp sear, a hiss, and a low sizzling thud as it sets."""
    dur = 0.6
    sear = sa.moving_band(0.4, [(0, 5500), (0.4, 2200)], 0.6, rng) * sa.decay(0.4, 0.1, 0.001)
    thud = sa.thump(110, 60, 0.3, 0.06, drive=1.3) * 0.8
    sizzle = sa.norm(sa.bandpass(sa.grains(dur, 150, rng, shape=[(0, 1), (dur, 0.1)]), 2500, 8000)) * 0.5
    x = sa.mix(0.8 * sear, (0.01, thud), sizzle)
    return sa.finish(sa.reverb(x, 0.3, 0.05), "impact")


def fire_ash(v, rng):
    """Ashen Veil: a soft dry 'fff' of ash, a few flecks popping in it."""
    dur = 0.45
    puff = sa.moving_band(dur, [(0, 2400), (dur, 900)], 1.0, rng) * sa.decay(dur, 0.09, 0.004)
    flecks = sa.norm(sa.bandpass(sa.grains(dur, 70, rng, shape=[(0, 1), (dur, 0)]), 1800, 5500)) * 0.35
    x = sa.mix(puff, flecks)
    return sa.finish(sa.reverb(x, 0.35, 0.08, damp=3500), "effect")


def fire_coals(v, rng):
    """Cinderheart, a pulse: a low thrum like a furnace door, a crackle of coals over it."""
    dur = 0.6
    thrum = sa.thump(72 + 6 * v, 46, dur, 0.14, drive=1.3, knock=0.1) * 0.9
    roar = sa.moving_band(dur, [(0, 230), (0.2, 520), (dur, 260)], 1.0, rng) * sa.env(dur, (0, 0), (0.1, 1), (dur, 0))
    coals = sa.norm(sa.bandpass(sa.grains(dur, 90, rng, shape=[(0, 1), (dur, 0.2)]), 1200, 5000)) * 0.4
    x = sa.mix(thrum, 0.7 * roar, coals)
    return sa.finish(sa.reverb(x, 0.4, 0.07), "pulse")


def fire_blade(v, rng):
    """Searing Edge: a blade drawn hot: a steel ring, a searing hiss."""
    dur = 0.55
    ring = sa.partials(sa.note(sa.A, 2), dur, ((1.0, 1.0, 1.0), (2.76, 0.35, 0.5), (5.4, 0.15, 0.3)), 0.12)
    hiss = sa.moving_band(dur, [(0, 6500), (dur, 3000)], 0.6, rng) * sa.decay(dur, 0.12, 0.002)
    x = sa.mix(0.45 * ring, 0.8 * hiss)
    return sa.finish(sa.reverb(x, 0.35, 0.06), "impact")


def fire_ward(v, rng):
    """Fireward: a warm hush folding in: a low swell with a soft chorus of amber tones."""
    dur = 0.8
    tone = sa.sine(sa.note(sa.A, 0), dur) + 0.5 * sa.sine(sa.note(sa.D, 1), dur) + 0.25 * sa.sine(sa.note(sa.FS, 1), dur)
    x = tone * sa.swell(dur, 0.25, 1.4) * 0.5
    hush = sa.moving_band(dur, [(0, 1400), (dur, 700)], 0.8, rng) * sa.swell(dur, 0.2, 1.5) * 0.35
    return sa.finish(sa.reverb(sa.chorus(sa.mix(x, hush), 3, 0.003, 0.35), 0.6, 0.12), "effect")


def fire_pit(v, rng):
    """Hellmouth: a pit opening: a sinking drone, a slow tearing crackle, a groan at the bottom."""
    dur = 1.6
    drone = sa.soft_saw(sa.glide(dur, (0, sa.note(sa.D, -2)), (dur, sa.note(sa.D, -3)))) * sa.swell(dur, 0.4, 1.2) * 0.5
    tear = sa.moving_band(dur, [(0, 900), (dur, 200)], 1.0, rng) * sa.swell(dur, 0.3, 1.0) * 0.8
    crackle = sa.norm(sa.bandpass(sa.grains(dur, 80, rng, length=(0.002, 0.01), shape=[(0, 0.6), (dur, 1)]), 300, 3000)) * 0.4
    x = sa.mix(drone, tear, crackle)
    return sa.finish(sa.reverb(sa.saturate(x, 1.3), 0.8, 0.14), "effect")


def fire_star(v, rng):
    """Starfire: gold chimes drawn in a rising line, ember pops between them."""
    dur = 1.0
    notes = [sa.note(d, 2) for d in (sa.D, sa.FS, sa.A, sa.B)]
    chimes = sa.sparkle(dur, 9, rng, notes, tau=(0.08, 0.2), rising=True)
    embers = sa.norm(sa.bandpass(sa.grains(dur, 50, rng), 1500, 6000)) * 0.2
    x = sa.mix(chimes, embers)
    return sa.finish(sa.reverb(x, 0.6, 0.12), "effect")


def fire_clock(v, rng):
    """Everburn: a clock's tick with a flame in it, and a low relight whump."""
    dur = 0.5
    tick = sa.tick(sa.note(sa.A, 2), rng) * 0.8
    flame = sa.moving_band(0.3, [(0, 1600), (0.3, 3800)], 0.8, rng) * sa.decay(0.3, 0.07, 0.004)
    whump = sa.thump(90, 55, 0.3, 0.07, drive=1.2) * 0.6
    x = sa.mix(tick, (0.02, 0.6 * flame), (0.02, whump))
    return sa.finish(sa.reverb(x, 0.35, 0.07), "impact")


def fire_pyre(v, rng):
    """Phoenix Pyre: a cry that rises and breaks, a great wing beat of flame under it."""
    dur = 1.3
    cry = sa.fm(sa.glide(dur, (0, sa.note(sa.A, 1)), (0.5, sa.note(sa.B, 2)), (dur, sa.note(sa.FS, 2))), 2.0, 2.0 * sa.env(dur, (0, 0.5), (0.5, 2.5), (dur, 0.3))) * sa.swell(dur, 0.35, 1.3)
    beat = sa.moving_band(dur, [(0, 400), (0.3, 1300), (dur, 500)], 1.0, rng) * sa.env(dur, (0, 0), (0.1, 1), (0.3, 0.4), (0.5, 0.8), (dur, 0))
    whump = sa.thump(85, 50, 0.5, 0.12, drive=1.3) * 0.7
    x = sa.mix(0.5 * cry, 0.8 * beat, whump)
    return sa.finish(sa.reverb(sa.chorus(x, 3, 0.003, 0.5), 0.8, 0.14), "effect")


def fire_out(v, rng):
    """A fire going out: a short sigh of smoke and a last crackle. The end cue of every timed fire."""
    dur = 0.6
    sigh = sa.moving_band(dur, [(0, 2200), (dur, 700)], 0.9, rng) * sa.decay(dur, 0.14, 0.01)
    last = sa.norm(sa.bandpass(sa.grains(dur, 60, rng, shape=[(0, 1), (0.4, 0.1), (dur, 0)]), 1400, 5000)) * 0.5
    x = sa.mix(sigh, last)
    return sa.finish(sa.reverb(x, 0.4, 0.08, damp=4000), "tell")


EVENTS = [
    event("fire_flick", fire_flick, variants=3, role="impact", subtitle="hit"),
    event("fire_whump", fire_whump, variants=2, role="impact", subtitle="hit"),
    event("fire_flare", fire_flare, variants=2, role="impact", subtitle="hit"),
    event("fire_blast", fire_blast, variants=2, role="impact", subtitle="hit"),
    event("fire_meteor_fall", fire_meteor_fall, variants=1, role="effect", subtitle="cast"),
    event("fire_meteor_hit", fire_meteor_hit, variants=2, role="impact", subtitle="hit"),
    event("fire_field", fire_field, variants=3, role="pulse", subtitle="field"),
    event("fire_fuse", fire_fuse, variants=1, role="tell", subtitle="tell"),
    event("fire_fuse_tick", fire_fuse_tick, variants=4, role="tick", subtitle="tell"),
    event("fire_stack", fire_stack, variants=1, role="tell", subtitle="tell"),
    event("fire_hop", fire_hop, variants=3, role="impact", subtitle="hit"),
    event("fire_launch", fire_launch, variants=3, role="impact", subtitle="cast"),
    event("fire_steam", fire_steam, variants=1, role="effect", subtitle="field"),
    event("fire_sun", fire_sun, variants=1, role="impact", subtitle="hit"),
    event("fire_soul", fire_soul, variants=1, role="effect", subtitle="hit"),
    event("fire_brand", fire_brand, variants=1, role="impact", subtitle="hit"),
    event("fire_ash", fire_ash, variants=2, role="effect", subtitle="hit"),
    event("fire_coals", fire_coals, variants=3, role="pulse", subtitle="field"),
    event("fire_blade", fire_blade, variants=1, role="impact", subtitle="hit"),
    event("fire_ward", fire_ward, variants=1, role="effect", subtitle="cast"),
    event("fire_pit", fire_pit, variants=1, role="effect", subtitle="field"),
    event("fire_star", fire_star, variants=1, role="effect", subtitle="hit"),
    event("fire_clock", fire_clock, variants=1, role="impact", subtitle="hit"),
    event("fire_pyre", fire_pyre, variants=1, role="effect", subtitle="cast"),
    event("fire_out", fire_out, variants=2, role="tell", subtitle="tell"),
]


# ---------------------------------------------------------------- a world's resonances: the twists' voices (cast/TwistVfx)

def fire_kindly(v, rng):
    """Kindly Flame: a warm crackle under a soft major chord and a bell: fire that mends."""
    dur = 1.4
    chord = sum(sa.sine(sa.note(d, 1), dur) * w for d, w in ((sa.D, 1.0), (sa.FS, 0.6), (sa.A, 0.5))) * sa.swell(dur, 0.25)
    bell = sa.bell(sa.note((sa.A, sa.B)[v % 2], 2), dur, 0.4, ratio=2.0, brightness=0.8)
    crackle = sa.norm(sa.bandpass(sa.grains(dur, 60, rng, shape=[(0, 1), (dur, 0.2)]), 1200, 5000)) * 0.3
    breath = sa.moving_band(dur, [(0, 500), (0.4, 1100), (dur, 600)], 1.0, rng) * sa.swell(dur, 0.3) * 0.3
    return sa.finish(sa.reverb(sa.mix(0.5 * chord, 0.5 * bell, crackle, breath), 0.9, 0.18), "effect")


def fire_sun_seal(v, rng):
    """Sun Seal: a bright flare catching, a low gong under it, and the seal's long hum."""
    dur = 1.8
    flare = sa.moving_band(0.6, [(0, 600), (0.25, 3200), (0.6, 1400)], 1.0, rng) * sa.env(0.6, (0, 0), (0.12, 1), (0.6, 0))
    gong = sa.bell(sa.note(sa.D, -1), dur, 0.7, ratio=1.41, brightness=1.2)
    halo = sa.chorus(sa.sine(sa.note(sa.A, 1), dur) * sa.swell(dur, 0.5), voices=2)
    roar = sa.norm(sa.lowpass(sa.noise(dur, rng), 500)) * sa.swell(dur, 0.4) * 0.4
    return sa.finish(sa.reverb(sa.mix(0.8 * flare, 0.7 * gong, 0.3 * halo, roar), 1.1, 0.2), "grand")


EVENTS += [
    event("fire_kindly", fire_kindly, variants=2, role="effect", subtitle="field"),
    event("fire_sun_seal", fire_sun_seal, variants=1, role="grand", subtitle="field"),
]
# ---------------------------------------------------------------- residues: what's left behind


def fire_smoulder(v, rng):
    """Smouldering ash: embers still popping in it, one or two at a time, over a warm breath of heat."""
    dur = 1.3
    pops = sa.norm(sa.bandpass(sa.grains(dur, 14 + 4 * v, rng, length=(0.001, 0.005), shape=[(0, 1), (dur, 0.6)]), 1400, 5200))
    breath = sa.norm(sa.lowpass(sa.noise(dur, rng), 900)) * sa.swell(dur, 0.45)
    glow = sa.sine(sa.note(sa.D, -1), dur) * sa.swell(dur, 0.5) * 0.15
    x = sa.mix(0.8 * pops, 0.35 * breath, glow)
    return sa.finish(sa.reverb(x, 0.4, 0.08), "tell", fade_out=0.25)


EVENTS += [event("fire_smoulder", fire_smoulder, variants=2, role="tell", subtitle="field")]
