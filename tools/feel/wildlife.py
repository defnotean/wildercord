"""The voices of the magical wildlife: glimmerwings, lumen stags, mossback tortoises, cinderfoxes, skyrays and
rimehares. Names start with 'wildlife_', and each sound names its creature in its subtitle (core.CREATURE_SUBTITLES;
the text is in tools/wildlife_art.py).

Creatures aren't spells, so most of these are breath, wood, fur and wing rather than bells; where a creature is made
of magic (the stag's crystal antlers, the moths' glitter, the skyray's song) the tonal part sits in the shared key,
D major pentatonic, so it never jars against a spell going off nearby.
"""
import numpy as np

from feel.core import sa, event

D, E, FS, A, B = sa.D, sa.E, sa.FS, sa.A, sa.B


def flutter(dur, rate, lo, hi, rng, depth=0.85):
    """Wings beating: a band of air pulsed `rate` times a second, each beat a little uneven."""
    t = sa.timeline(dur)
    wobble = np.cumsum(rng.uniform(0.85, 1.15, len(t))) / sa.SR
    beats = (0.5 + 0.5 * np.sin(2 * np.pi * rate * wobble)) ** 2
    air = sa.norm(sa.bandpass(sa.noise(dur, rng), lo, hi))
    return air * (1 - depth + depth * beats)


def voice(path, dur, harmonics=8, cutoff=2400.0, vibrato=0.0, vib_rate=5.5):
    """An animal's voiced cry: a buzzy tone along a pitch path, rounded by a low-pass like a throat."""
    f = sa.glide(dur, *path)
    if vibrato:
        f = f * (1 + vibrato * np.sin(2 * np.pi * vib_rate * sa.timeline(dur)))
    return sa.lowpass(sa.soft_saw(f, harmonics=harmonics), cutoff)


def crunch(dur, rate, lo, hi, rng, shape=None):
    """Snow, leaves or sand underfoot: grains of noise, coloured, exactly `dur` long."""
    return sa.norm(sa.bandpass(sa.grains(dur, rate, rng, shape=shape), lo, hi))[:sa.samples(dur)]


def knock(freq, dur, rng, tau=0.03):
    """Something hollow struck: a woody tone with a dull click."""
    body = sa.sine(sa.sweep(freq * 1.3, freq, dur, 0.4)) * sa.decay(dur, tau, 0.001)
    click = 0.4 * sa.norm(sa.bandpass(sa.noise(0.03, rng), 600, 3200)) * sa.decay(0.03, 0.005, 0.0004)
    return sa.mix(body, click)


# ---------------------------------------------------------------- the glimmerwing


def wildlife_glimmerwing_flutter(v, rng):
    """A moth going by: a soft fast whirr of wings, and a faint glitter where its dust falls."""
    dur = 0.7
    wings = flutter(dur, 26 + 4 * v, 700, 2600, rng) * sa.swell(dur, 0.3)
    glitter = sa.sparkle(0.45, 9, rng, [sa.note(n, 3) for n in (D, FS, A, B)], tau=(0.04, 0.09))
    return sa.finish(sa.reverb(sa.mix(0.55 * wings, (0.1, 0.18 * sa.norm(glitter))), 0.5, 0.15), "ui")


def wildlife_glimmerwing_hurt(v, rng):
    """A moth struck: a scatter of tiny glass notes and a puff of dust."""
    dur = 0.4
    puff = sa.norm(sa.bandpass(sa.noise(dur, rng), 1800, 6000)) * sa.decay(dur, 0.05, 0.002)
    glints = sa.sparkle(0.25, 30, rng, [sa.note(n, 3) for n in ((A, FS, D), (B, A, E))[v]], tau=(0.03, 0.08))
    return sa.finish(sa.reverb(sa.mix(0.35 * puff, 0.6 * sa.norm(glints)), 0.5, 0.2), "effect")


# ---------------------------------------------------------------- the lumen stag


def wildlife_stag_call(v, rng):
    """A stag's call: a breathy bugle that climbs and falls away, ringing faintly in its crystal antlers."""
    dur = 2.0
    low, high = ((sa.note(A, -1), sa.note(FS, 1)), (sa.note(FS, -1), sa.note(D, 1)))[v]
    bugle = voice([(0, low), (0.35, high), (1.1, high * 0.97), (dur, low * 0.8)], dur, 7, 2200, 0.012)
    bugle *= sa.env(dur, (0, 0), (0.25, 0.8), (0.5, 1), (1.2, 0.7), (dur, 0)) ** 1.4
    breath = sa.moving_band(dur, [(0, 900), (0.4, 2600), (dur, 1200)], 1.0, rng) * sa.swell(dur, 0.5)
    ring = sa.mix((0.35, 0.5 * sa.glass(sa.note(D, 2), 1.5, 0.5)), (0.42, 0.35 * sa.glass(sa.note(A, 2), 1.4, 0.45)))
    return sa.finish(sa.reverb(sa.mix(0.6 * sa.norm(bugle), 0.15 * sa.norm(breath), 0.35 * ring), 1.6, 0.35), "effect")


def wildlife_stag_hurt(v, rng):
    """A stag struck: a sharp bleat and a crack of crystal."""
    dur = 0.55
    f0 = (sa.note(A, 0), sa.note(B, 0))[v]
    bleat = voice([(0, f0), (0.08, f0 * 1.25), (dur, f0 * 0.8)], dur, 9, 2800, 0.04, 9) * sa.decay(dur, 0.18, 0.01)
    crack = sa.shatter(rng, count=8, band=(2400, 6000), spread=0.06, tau=(0.02, 0.07))
    return sa.finish(sa.mix(0.75 * sa.norm(bleat), 0.4 * sa.norm(crack)), "impact")


def wildlife_stag_death(v, rng):
    """A stag falling: its light going out, a chord of crystal sinking note by note into silence."""
    dur = 2.4
    falls = [(0.0, (B, 2)), (0.3, (A, 2)), (0.6, (FS, 2)), (0.9, (E, 2)), (1.2, (D, 2))]
    chord = sa.mix(*[(t, 0.6 * (0.85 ** i) * sa.glass(sa.note(*n), dur - t, 0.6)) for i, (t, n) in enumerate(falls)])
    sigh = voice([(0, sa.note(FS, -1)), (dur * 0.6, sa.note(D, -1) * 0.8)], dur * 0.6, 6, 1500) * sa.swell(dur * 0.6, 0.1, 1.5)
    return sa.finish(sa.reverb(sa.mix(0.6 * sa.norm(chord), 0.3 * sa.norm(sigh)), 2.0, 0.4), "effect")


def wildlife_stag_shed(v, rng):
    """An antler let fall: a bright crystal chord, a soft knock on the ground."""
    dur = 1.6
    chord = sa.mix(*[(t, 0.55 * sa.glass(sa.note(*n), dur - t, 0.5)) for t, n in ((0.0, (D, 2)), (0.05, (FS, 2)), (0.1, (A, 2)), (0.16, (D, 3)))])
    thud = knock(sa.note(D, -1), 0.3, rng, 0.05)
    glints = sa.sparkle(0.9, 10, rng, [sa.note(n, 3) for n in (D, FS, A, B)], tau=(0.05, 0.14), rising=True)
    return sa.finish(sa.reverb(sa.mix(0.6 * chord, (0.32, 0.45 * thud), (0.1, 0.2 * sa.norm(glints))), 1.3, 0.3), "effect")


# ---------------------------------------------------------------- the mossback tortoise


def wildlife_tortoise_grumble(v, rng):
    """A tortoise thinking aloud: a slow, low, rumbling hum through its nose."""
    dur = 1.3
    f0 = (sa.note(D, -1), sa.note(A, -2))[v]
    hum = voice([(0, f0), (0.5, f0 * 1.06), (dur, f0 * 0.94)], dur, 16, 1500, 0.02, 3.5)
    hum *= sa.env(dur, (0, 0), (0.2, 1), (0.9, 0.8), (dur, 0))
    snort = sa.norm(sa.bandpass(sa.noise(0.25, rng), 400, 1800)) * sa.decay(0.25, 0.06, 0.02)
    return sa.finish(sa.reverb(sa.mix(0.75 * sa.norm(hum), (0.95, 0.3 * snort)), 0.8, 0.2), "effect")


def wildlife_tortoise_hurt(v, rng):
    """A tortoise struck: a hollow knock on its shell and a grunt."""
    shell = knock((300, 260)[v], 0.35, rng, 0.06)
    grunt = voice([(0, sa.note(E, -1)), (0.3, sa.note(D, -1) * 0.85)], 0.3, 14, 1600) * sa.decay(0.3, 0.1, 0.01)
    return sa.finish(sa.mix(0.8 * shell, (0.04, 0.5 * sa.norm(grunt))), "impact")


def wildlife_tortoise_death(v, rng):
    """A tortoise's last breath: a long low sigh, and the shell settling."""
    dur = 2.0
    sigh = voice([(0, sa.note(D, -1)), (dur, sa.note(A, -2) * 0.8)], dur, 14, 1500) * sa.swell(dur, 0.3, 1.2)
    breath = sa.moving_band(dur, [(0, 700), (dur, 300)], 1.2, rng) * sa.swell(dur, 0.4)
    settle = knock(240, 0.4, rng, 0.08)
    return sa.finish(sa.reverb(sa.mix(0.6 * sa.norm(sigh), 0.2 * sa.norm(breath), (1.5, 0.5 * settle)), 1.0, 0.25), "effect")


def wildlife_tortoise_hide(v, rng):
    """Into the shell: a scrape of scales and a solid knock as it closes up."""
    dur = 0.45
    scrape = sa.moving_band(0.25, [(0, 2400), (0.25, 900)], 0.9, rng) * sa.decay(0.25, 0.08, 0.01)
    return sa.finish(sa.mix(0.5 * scrape, (0.18, 0.8 * knock(300, 0.3, rng, 0.05))), "effect")


def wildlife_tortoise_step(v, rng):
    """A heavy, soft footfall in mud and moss."""
    dur = 0.25
    thud = sa.mix(0.25 * sa.thump(140, 80, dur, 0.035, 1.6, 0.6), knock((240, 220, 260)[v], dur, rng, 0.035))
    squelch = crunch(0.15, 160, 400, 2200, rng) * sa.decay(0.15, 0.04, 0.003)
    return sa.finish(sa.highpass(sa.mix(0.7 * thud, 0.45 * squelch), 110), "ui")


def wildlife_tortoise_scute(v, rng):
    """A scute coming loose: a dry clack of shell and a rustle of moss."""
    clack = knock(420, 0.2, rng, 0.02)
    rustle = sa.moving_band(0.4, [(0, 3000), (0.4, 1800)], 0.8, rng) * sa.swell(0.4, 0.1)
    return sa.finish(sa.mix(0.7 * clack, (0.05, 0.3 * sa.norm(rustle))), "effect")


# ---------------------------------------------------------------- the cinderfox


def wildlife_cinderfox_yip(v, rng):
    """A cinderfox's yip: a quick bright bark that flicks up, with an ember's crackle under it."""
    dur = 0.35
    f0 = (sa.note(A, 1), sa.note(B, 1), sa.note(FS, 1))[v]
    yip = voice([(0, f0 * 0.8), (0.05, f0 * 1.3), (dur, f0 * 0.9)], dur, 8, 3400) * sa.decay(dur, 0.08, 0.004)
    crackle = crunch(dur, 60, 2000, 7000, rng) * sa.swell(dur, 0.1)
    return sa.finish(sa.mix(0.8 * sa.norm(yip), 0.15 * crackle), "effect")


def wildlife_cinderfox_hurt(v, rng):
    """A cinderfox struck: a high yelp, and a spit of sparks."""
    dur = 0.4
    f0 = (sa.note(D, 2), sa.note(E, 2))[v]
    yelp = voice([(0, f0 * 1.2), (dur, f0 * 0.7)], dur, 7, 3800, 0.03, 12) * sa.decay(dur, 0.12, 0.003)
    sparks = crunch(0.2, 120, 2500, 7500, rng) * sa.decay(0.2, 0.06, 0.002)
    return sa.finish(sa.mix(0.75 * sa.norm(yelp), 0.25 * sparks), "impact")


def wildlife_cinderfox_death(v, rng):
    """A cinderfox's ember going out: a falling whine and a last hiss."""
    dur = 1.1
    whine = voice([(0, sa.note(A, 1)), (dur, sa.note(D, 0) * 0.8)], dur, 7, 3000) * sa.swell(dur, 0.1, 1.5)
    hiss = sa.moving_band(dur, [(0, 5000), (dur, 2000)], 0.7, rng) * sa.env(dur, (0, 0), (0.5, 0), (0.7, 1), (dur, 0))
    return sa.finish(sa.reverb(sa.mix(0.65 * sa.norm(whine), 0.25 * sa.norm(hiss)), 0.6, 0.2), "effect")


def wildlife_cinderfox_spark(v, rng):
    """Embers catching on a bitten foe: a soft whoomp and a crackle."""
    dur = 0.6
    whoomp = sa.norm(sa.lowpass(sa.noise(dur, rng), 500)) * sa.decay(dur, 0.1, 0.01)
    crackle = crunch(dur, 90, 1800, 7000, rng, shape=[(0, 1), (dur, 0.2)])
    return sa.finish(sa.mix(0.6 * whoomp, 0.4 * crackle), "effect")


# ---------------------------------------------------------------- the skyray


def wildlife_skyray_call(v, rng):
    """A skyray singing high overhead: a long, low, gliding song carried on the wind."""
    dur = 3.2
    path = ([(0, sa.note(D, -1)), (0.9, sa.note(A, -1)), (1.8, sa.note(FS, -1)), (dur, sa.note(D, -1) * 0.9)],
            [(0, sa.note(A, -2)), (1.0, sa.note(E, -1)), (2.0, sa.note(D, -1)), (dur, sa.note(A, -2) * 0.9)])[v]
    song = sa.lowpass(sa.fm(sa.glide(dur, *path), 1.0, 0.8 + 0.6 * sa.swell(dur, 1.0), dur), 1800)
    song *= sa.env(dur, (0, 0), (0.4, 0.9), (1.6, 1.0), (2.6, 0.7), (dur, 0)) ** 1.3
    wind = sa.moving_band(dur, [(0, 500), (1.4, 1300), (dur, 600)], 1.2, rng) * sa.swell(dur, 1.4)
    return sa.finish(sa.reverb(sa.mix(0.7 * sa.chorus(sa.norm(song), 2, 0.004, 0.3), 0.25 * sa.norm(wind)), 2.4, 0.5), "effect")


def wildlife_skyray_hurt(v, rng):
    """A skyray struck: a sharp, warbling cry."""
    dur = 0.6
    f0 = (sa.note(A, 0), sa.note(FS, 0))[v]
    cry = sa.lowpass(sa.fm(sa.glide(dur, (0, f0 * 1.3), (dur, f0 * 0.8)), 1.0, 1.4, dur), 2600)
    cry *= sa.decay(dur, 0.2, 0.005) * (1 + 0.3 * np.sin(2 * np.pi * 14 * sa.timeline(dur)))
    rush = sa.norm(sa.bandpass(sa.noise(0.25, rng), 1200, 4000)) * sa.decay(0.25, 0.06, 0.003)
    return sa.finish(sa.mix(0.85 * sa.norm(cry), 0.15 * rush), "impact")


def wildlife_skyray_death(v, rng):
    """A skyray falling: its song sinking away, and the wind closing over it."""
    dur = 3.0
    song = sa.lowpass(sa.fm(sa.glide(dur, (0, sa.note(A, -1)), (dur, sa.note(D, -2) * 0.85)), 1.0, 0.9, dur), 1500) * sa.swell(dur, 0.3, 1.3)
    wind = sa.moving_band(dur, [(0, 1400), (dur, 400)], 1.3, rng) * sa.swell(dur, 1.5)
    return sa.finish(sa.reverb(sa.mix(0.6 * sa.norm(song), 0.35 * sa.norm(wind)), 2.2, 0.45), "effect")


# ---------------------------------------------------------------- the rimehare


def wildlife_rimehare_squeak(v, rng):
    """A rimehare's squeak: tiny and high, a twitch of a sound."""
    dur = 0.18
    f0 = (sa.note(B, 2), sa.note(A, 2))[v]
    squeak = voice([(0, f0), (0.04, f0 * 1.15), (dur, f0 * 0.95)], dur, 5, 5000) * sa.decay(dur, 0.05, 0.003)
    return sa.finish(squeak, "ui")


def wildlife_rimehare_hurt(v, rng):
    """A rimehare struck: a thin squeal and a puff of snow."""
    dur = 0.35
    f0 = (sa.note(D, 3), sa.note(E, 3))[v]
    squeal = voice([(0, f0 * 1.1), (dur, f0 * 0.75)], dur, 5, 5500, 0.03, 14) * sa.decay(dur, 0.1, 0.003)
    puff = crunch(0.2, 200, 1500, 6000, rng) * sa.decay(0.2, 0.05, 0.002)
    return sa.finish(sa.mix(0.75 * sa.norm(squeal), 0.25 * puff), "impact")


def wildlife_rimehare_hop(v, rng):
    """A bound landing in snow: a soft crunch and a tinkle of rime."""
    dur = 0.2
    snow = crunch(dur, (220, 260, 180)[v], 900, 4500, rng, shape=[(0, 1), (dur, 0.1)]) * sa.decay(dur, 0.05, 0.002)
    rime = 0.15 * sa.glass(sa.note((D, A, FS)[v], 3), 0.2, 0.04)
    return sa.finish(sa.mix(0.8 * snow, rime), "ui")


EVENTS = [
    event("wildlife_glimmerwing_flutter", wildlife_glimmerwing_flutter, variants=3, role="ui", subtitle="glimmerwing_flutter"),
    event("wildlife_glimmerwing_hurt", wildlife_glimmerwing_hurt, variants=2, subtitle="glimmerwing_hurt"),
    event("wildlife_stag_call", wildlife_stag_call, variants=2, attenuation=32, subtitle="stag_call"),
    event("wildlife_stag_hurt", wildlife_stag_hurt, variants=2, role="impact", subtitle="stag_hurt"),
    event("wildlife_stag_death", wildlife_stag_death, subtitle="stag_death"),
    event("wildlife_stag_shed", wildlife_stag_shed, subtitle="stag_shed"),
    event("wildlife_tortoise_grumble", wildlife_tortoise_grumble, variants=2, subtitle="tortoise_grumble"),
    event("wildlife_tortoise_hurt", wildlife_tortoise_hurt, variants=2, role="impact", subtitle="tortoise_hurt"),
    event("wildlife_tortoise_death", wildlife_tortoise_death, subtitle="tortoise_death"),
    event("wildlife_tortoise_hide", wildlife_tortoise_hide, subtitle="tortoise_hide"),
    event("wildlife_tortoise_step", wildlife_tortoise_step, variants=3, role="ui", subtitle="tortoise_step"),
    event("wildlife_tortoise_scute", wildlife_tortoise_scute, subtitle="tortoise_scute"),
    event("wildlife_cinderfox_yip", wildlife_cinderfox_yip, variants=3, subtitle="cinderfox_yip"),
    event("wildlife_cinderfox_hurt", wildlife_cinderfox_hurt, variants=2, role="impact", subtitle="cinderfox_hurt"),
    event("wildlife_cinderfox_death", wildlife_cinderfox_death, subtitle="cinderfox_death"),
    event("wildlife_cinderfox_spark", wildlife_cinderfox_spark, variants=2, subtitle="cinderfox_spark"),
    event("wildlife_skyray_call", wildlife_skyray_call, variants=2, attenuation=64, subtitle="skyray_call"),
    event("wildlife_skyray_hurt", wildlife_skyray_hurt, variants=2, role="impact", attenuation=48, subtitle="skyray_hurt"),
    event("wildlife_skyray_death", wildlife_skyray_death, attenuation=48, subtitle="skyray_death"),
    event("wildlife_rimehare_squeak", wildlife_rimehare_squeak, variants=2, role="ui", subtitle="rimehare_squeak"),
    event("wildlife_rimehare_hurt", wildlife_rimehare_hurt, variants=2, role="impact", subtitle="rimehare_hurt"),
    event("wildlife_rimehare_hop", wildlife_rimehare_hop, variants=3, role="ui", subtitle="rimehare_hop"),
]
