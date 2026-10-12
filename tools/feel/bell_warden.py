"""The Drowned Bell-Warden: the cracked bronze bell it wears for a head, tolling low and buzzing where it split; its drowned
breath at its vigil; the grind of it rising; its challenge, its hurts, its fall and its tread."""
from feel.core import sa, event

# A great church bell's partials: the hum an octave down, the prime, a minor-third tierce, the quint, the nominal and its
# uppers; the lowest ring longest. The bell is cracked, so a second prime and tierce sit a little off and beat against them.
CRACKED = ((.5, .2, 1.6), (1.0, .6, 1.0), (1.013, .3, .8), (1.19, .55, .8), (1.205, .4, .6), (1.5, .4, .6),
           (2.0, .7, .7), (2.52, .4, .5), (3.0, .35, .45), (4.18, .22, .35), (5.4, .14, .25), (6.8, .09, .18))
TOLL = 196.0


def _bell(freq, s, tau, rng, muffle=4000):
    """The bell struck: its partials, a rattle where it is split, all of it heard through water."""
    ring = sa.partials(freq, s, CRACKED, tau, attack=.004)
    buzz = sa.bandpass(sa.chopper(s, rng, rate=(freq * .5, freq * .55), floor=.2) * sa.noise(s, rng), 400, 2200) * sa.decay(s, tau * .35, .004)
    strike = sa.mix(sa.thump(260, 120, .3, .04, drive=2.0, knock=.6), (0, .6 * sa.bell(freq * 4.2, .5, .08, ratio=1.41, brightness=2.5)))
    return sa.lowpass(sa.mix(sa.norm(ring), (0, .4 * buzz), (0, .4 * strike)), muffle)


def toll(v, rng):
    """One toll of its bell: deep, long, cracked, under the sea."""
    s = 3.2
    return sa.finish(sa.reverb(sa.highpass(_bell(TOLL, s, 1.1, rng), 30), 1.6, .4, damp=3000), 'effect')


def breath(v, rng):
    """At its vigil, breathing water: a slow gurgle in the bell and a few bubbles rising."""
    s = 2.4
    gurgle = sa.lowpass(sa.moving_band(s, ((0, 380), (1.2, 620), (s, 340)), .9, rng) * sa.jitter(s, rng, 9) ** 2, 1500) * sa.swell(s, .5, 1.4)
    bubbles = sa.norm(sa.bubbles(1.4, 10, rng))
    hum = .12 * sa.partials(TOLL, s, CRACKED[:3], .9) * sa.swell(s, .6, 1.2)
    x = sa.mix(.8 * gurgle, (.6, .25 * bubbles), (0, hum))
    return sa.finish(sa.reverb(sa.highpass(x, 40), .8, .3, damp=2600), 'effect')


def rise(v, rng):
    """Getting off its knees: joints grinding, water running off it, a chain swinging against the bronze."""
    s = 2.4
    grind = sa.moving_band(s, ((0, 300), (1.2, 700), (s, 380)), .9, rng) * sa.swell(s, .55, 1.3)
    run = sa.norm(sa.bandpass(sa.noise(s, rng), 500, 3200)) * sa.swell(s, .7, 1.6)
    clinks = sa.mix(*[(t, .4 * sa.bandpass(sa.partials(1300 + 200 * i, .3, ((1, 1, 1), (2.7, .5, .4)), .06), 600, 5000))
                     for i, t in enumerate((.5, .9, 1.25, 1.7))])
    x = sa.mix(.8 * grind, (0, .35 * run), (0, clinks))
    return sa.finish(sa.reverb(sa.highpass(x, 35), .9, .3, damp=3200), 'effect')


def challenge(v, rng):
    """On its feet: it strikes its own bell, and bellows into it, the sound coming back out of the bronze."""
    s = 3.4
    ring = _bell(TOLL * .89, s, 1.0, rng, muffle=4500)
    roar = sa.lowpass(sa.soft_saw(sa.glide(s, (0, 96), (.5, 122), (2.2, 104), (s, 80)), s) * sa.jitter(s, rng, 24) ** 2, 700) * sa.swell(s, .25, 1.5)
    roar = sa.mix(roar, (.013, .6 * roar))  # the bell's inside, throwing it back
    x = sa.mix(ring, (.25, .8 * roar))
    return sa.finish(sa.reverb(sa.highpass(x, 30), 1.6, .4, damp=3200), 'effect')


def hurt(v, rng):
    """A blow on the bronze: a dull clank, the crack buzzing, a grunt inside."""
    s = .7
    clank = _bell(TOLL * 1.5, s, .18, rng, muffle=5000)
    grunt = sa.lowpass(sa.soft_saw(sa.glide(s, (0, 140), (s, 104)), s), 500) * sa.decay(s, .15, .01)
    return sa.finish(sa.reverb(sa.highpass(sa.mix(clank, (.03, .5 * grunt)), 50), .5, .2, damp=4000), 'effect')


def death(v, rng):
    """It falls: a last bellow, the bell striking the floor and rolling, ringing out as it settles."""
    s = 4.5
    groan = sa.lowpass(sa.soft_saw(sa.glide(1.6, (0, 116), (1.6, 68)), 1.6) * sa.jitter(1.6, rng, 18) ** 2, 600) * sa.swell(1.6, .2, 1.2)
    fall = sa.thump(110, 40, 1.2, .3, drive=2.4, knock=1.0)
    ring = _bell(TOLL, 3.2, 1.2, rng)
    roll = sa.norm(sa.bandpass(sa.chopper(2.0, rng, rate=(6, 11), floor=.1) * sa.noise(2.0, rng), 300, 2000)) * sa.decay(2.0, .7, .05)
    x = sa.mix(.6 * groan, (1.3, .7 * fall), (1.32, .9 * ring), (1.5, .5 * roll))
    return sa.finish(sa.reverb(sa.highpass(x, 28), 1.8, .4, damp=3000), 'effect')


def step(v, rng):
    """Its tread: a heavy wet footfall, and a faint answer from the bell."""
    s = .6
    fall = sa.thump(90, 40, .35, .07, drive=1.8, knock=.3)
    slap = sa.norm(sa.bandpass(sa.noise(.25, rng), 350, 2400)) * sa.decay(.25, .05, .002)
    hum = .25 * sa.partials(TOLL, s, CRACKED[:6], .25)
    return sa.finish(sa.reverb(sa.highpass(sa.mix(.6 * fall, (0, slap), (.02, hum)), 60), .4, .15, damp=3500), 'effect')


BELL_WARDEN_EVENTS = [event('aura_bell_warden_' + name, build, role='effect', subtitle='bell_warden.' + name) for name, build in (
    ('toll', toll), ('breath', breath), ('rise', rise), ('challenge', challenge), ('hurt', hurt), ('death', death), ('step', step))]
