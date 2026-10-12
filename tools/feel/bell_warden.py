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


def _chain(s, rng, at, n=5, base=1500):
    """The chain wound round its arm, swinging against itself: a run of iron clinks from {at}."""
    return sa.mix(*[(at + .045 * i + .02 * rng.random(), .35 * sa.bandpass(sa.partials(base + 260 * ((i * 3) % 5), .25, ((1, 1, 1), (2.76, .5, .4), (5.4, .2, .2)), .05), 700, 6000))
                    for i in range(n)])


def swing(v, rng):
    """The clapper swung like a mace: a heavy whoosh through the air and the chain rattling after it."""
    s = .7
    whoosh = sa.moving_band(s, ((0, 500), (.22, 1500), (s, 700)), 1.1, rng) * sa.swell(s, .18, 2.0)
    x = sa.mix(.9 * whoosh, (0, _chain(s, rng, .05)))
    return sa.finish(sa.reverb(sa.highpass(x, 60), .3, .15, damp=5000), 'effect')


def hammer(v, rng):
    """The clapper brought down overhand onto the floor: a crack of iron on stone, and the floor ringing under it."""
    s = 1.0
    crack = sa.thump(180, 60, .4, .05, drive=2.6, knock=1.0)
    clang = sa.bandpass(sa.partials(620, s, ((1, 1, 1), (2.4, .6, .5), (3.9, .4, .3), (5.1, .3, .2)), .3), 300, 5000)
    grit = sa.norm(sa.bandpass(sa.noise(.4, rng), 800, 4000)) * sa.decay(.4, .08, .003)
    x = sa.mix(.8 * crack, (0, .5 * clang), (0, .45 * grit))
    return sa.finish(sa.reverb(sa.highpass(x, 40), .6, .25, damp=4500), 'effect')


def slam(v, rng):
    """Both fists into the floor: a deep blow, water thrown up off the stone, the bell humming on its shoulders."""
    s = 1.6
    blow = sa.thump(120, 36, 1.0, .2, drive=2.6, knock=1.0)
    spray = sa.norm(sa.bandpass(sa.noise(1.0, rng), 600, 4200)) * sa.decay(1.0, .3, .01)
    hum = .5 * sa.partials(TOLL, s, CRACKED[:7], .7)
    x = sa.mix(.9 * blow, (.02, .5 * spray), (.03, hum))
    return sa.finish(sa.reverb(sa.highpass(x, 30), 1.0, .35, damp=3500), 'effect')


def charge(v, rng):
    """It drops its bell like a ram and comes: a bellow into the bronze and the chain flying."""
    s = 1.4
    roar = sa.lowpass(sa.soft_saw(sa.glide(s, (0, 110), (.4, 150), (s, 120)), s) * sa.jitter(s, rng, 26) ** 2, 1400) * sa.swell(s, .15, 1.4)
    roar = sa.mix(roar, (.011, .6 * roar))
    rush = sa.moving_band(s, ((0, 400), (.6, 1200), (s, 600)), .9, rng) * sa.swell(s, .3, 1.6)
    x = sa.mix(.8 * roar, (0, .45 * rush), (.2, _chain(s, rng, .2, 7)))
    return sa.finish(sa.reverb(sa.highpass(x, 40), .7, .3, damp=4000), 'effect')


def backstep(v, rng):
    """It heaves itself back out of a blow: a scrape of feet on wet stone and a jolt of the chain."""
    s = .5
    scrape = sa.moving_band(s, ((0, 900), (s, 1800)), .8, rng) * sa.decay(s, .14, .005)
    x = sa.mix(.8 * scrape, (.03, .8 * _chain(s, rng, 0, 3, 1800)))
    return sa.finish(sa.reverb(sa.highpass(x, 120), .3, .12, damp=6000), 'effect')


def shell(v, rng):
    """It hunches down under its bell: the rim grinding onto the stone, and the bronze settling with a low ring."""
    s = 1.8
    grind = sa.moving_band(s, ((0, 700), (.4, 400), (s, 300)), .8, rng) * sa.swell(s, .15, 1.6) * sa.decay(s, .5, .01)
    ring = .7 * _bell(TOLL * 1.12, s, .6, rng, muffle=3500)
    x = sa.mix(.8 * grind, (.25, ring))
    return sa.finish(sa.reverb(sa.highpass(x, 40), .9, .3, damp=3500), 'effect')


def ring(v, rng):
    """A blow on the shelled bell: a bright clang that rings out, the crack buzzing under it."""
    s = 1.4
    x = _bell(TOLL * 2.0, s, .5, rng, muffle=6500)
    return sa.finish(sa.reverb(sa.highpass(x, 80), .6, .25, damp=5000), 'effect')


BELL_WARDEN_EVENTS = [event('aura_bell_warden_' + name, build, role='effect', subtitle='bell_warden.' + name) for name, build in (
    ('toll', toll), ('breath', breath), ('rise', rise), ('challenge', challenge), ('hurt', hurt), ('death', death), ('step', step),
    ('swing', swing), ('hammer', hammer), ('slam', slam), ('charge', charge), ('backstep', backstep), ('shell', shell), ('ring', ring))]
