"""The Lantern-Bearer: a hollow spirit's wail with wind in it, earth sliding off it as it rises from its grave, the creak and rattle of
its lantern on its chain, the hiss of its spade through the air and its thud into the turf, the soft rush of soul wisps and their burst,
the smoke-hush of its fading, and its long sigh as it sinks back into the earth for good."""
from feel.core import sa, event


def _ghost(s, rng, curve, breath=.35):
    """A hollow, wavering voice along {curve}: two detuned sines beating against each other, a breath of wind through them."""
    f = sa.glide(s, *curve)
    wobble = sa.jitter(s, rng, 5)
    tone = sa.sine(f * (1 + .012 * wobble), s) + .7 * sa.sine(f * 1.505, s) + .4 * sa.sine(f * 2.01, s)
    wind = sa.norm(sa.bandpass(sa.noise(s, rng), 500, 2400))
    return sa.mix(sa.norm(sa.lowpass(tone, 2200)), (0, breath * wind))


def _chain(s, rng, rate=14):
    """Chain links clinking and the lantern's hoop creaking."""
    return sa.norm(sa.bandpass(sa.grains(s, rate, rng, length=(.002, .008)), 2500, 7500))[:sa.samples(s)]


def _earth(s, rng):
    """Loose earth sliding and pattering."""
    return sa.norm(sa.lowpass(sa.grains(s, 90, rng, length=(.003, .012), shape=[(0, 1), (s, .3)]), 2200))[:sa.samples(s)]


def roar(v, rng):
    """Its wail as the candles kindle: a long hollow cry rising and falling away, wind howling through it."""
    s = 2.8
    cry = _ghost(s, rng, ((0, 180), (.5, 330), (1.4, 360), (2.2, 240), (s, 150)), .5) * sa.swell(s, .2, 1.4)
    under = _ghost(s, rng, ((0, 90), (s, 75)), .2) * sa.swell(s, .4, 1.2)
    x = sa.mix(sa.norm(cry), (0, .4 * sa.norm(under)), (.2, .25 * _chain(1.5, rng)))
    return sa.finish(sa.reverb(sa.highpass(x, 60), 2.4, .5, damp=5000), 'effect')


def rise(v, rng):
    """Up out of the grave: earth sliding off it, a low breath, then the lantern's chain rattling as it takes it down."""
    s = 2.2
    earth = _earth(s, rng) * sa.swell(s, .6, 1.3)
    breath = _ghost(1.4, rng, ((0, 110), (1.4, 140)), .7) * sa.swell(1.4, .5, 1.5)
    x = sa.mix(.8 * earth, (.4, .45 * sa.norm(breath)), (1.4, .4 * _chain(.8, rng, 20)))
    return sa.finish(sa.reverb(sa.highpass(x, 70), 1.2, .35, damp=5000), 'effect')


def step(v, rng):
    """Its robe dragging over the grass as it glides."""
    s = .5
    x = sa.norm(sa.bandpass(sa.noise(s, rng), 300, 2000)) * sa.swell(s, .2, 1.5) * .6
    return sa.finish(sa.reverb(x, .3, .15, damp=4000), 'effect')


def reap(v, rng):
    """The spade swung flat round: a long whoosh through the air and a hollow ring of iron."""
    s = .7
    whoosh = sa.moving_band(s, ((0, 500), (.3, 1800), (s, 700)), 1.0, rng) * sa.swell(s, .3, 2.0)
    ring = sa.bell(520, .6, .18, ratio=2.7, brightness=1.0)
    x = sa.mix(sa.norm(whoosh), (.28, .25 * ring))
    return sa.finish(sa.reverb(sa.highpass(x, 120), .5, .2, damp=6000), 'effect')


def hook(v, rng):
    """The crook thrust out: a quick swish, the lantern swinging wildly on its chain."""
    s = .6
    swish = sa.moving_band(s, ((0, 900), (.15, 2600), (s, 1200)), .8, rng) * sa.decay(s, .15, .01)
    x = sa.mix(sa.norm(swish), (.05, .5 * _chain(.5, rng, 30)))
    return sa.finish(sa.reverb(sa.highpass(x, 150), .4, .2, damp=6000), 'effect')


def chain(v, rng):
    """The lantern's chain rattling and creaking."""
    s = .6
    creak = sa.lowpass(sa.soft_saw(sa.glide(s, (0, 240), (s, 200)), s) * sa.chopper(s, rng, rate=(30, 45), floor=0), 1600) * sa.decay(s, .25, .02)
    x = sa.mix(sa.norm(_chain(s, rng, 24)) * sa.decay(s, .3, .005), (0, .3 * sa.norm(creak)))
    return sa.finish(sa.reverb(sa.highpass(x, 200), .3, .2, damp=7000), 'effect')


def wisp(v, rng):
    """Soul wisps loosed: a soft rising rush, a pale whispering chime."""
    s = 1.2
    rush = sa.moving_band(s, ((0, 700), (s, 2600)), .9, rng) * sa.swell(s, .3, 1.5)
    chime = sa.mix(*[(i * .12, .4 * sa.glass(880 * r, .7, .3)) for i, r in enumerate((1, 1.26, 1.5))])
    x = sa.mix(sa.norm(rush), (.1, .5 * sa.norm(chime)))
    return sa.finish(sa.reverb(sa.highpass(x, 200), 1.0, .4, damp=6000), 'effect')


def burst(v, rng):
    """A wisp bursting: a soft cold puff and a sigh."""
    s = .6
    puff = sa.norm(sa.bandpass(sa.noise(s, rng), 400, 3000)) * sa.decay(s, .12, .004)
    sigh = _ghost(s, rng, ((0, 300), (s, 160)), .4) * sa.decay(s, .3, .01)
    x = sa.mix(puff, (0, .4 * sa.norm(sigh)))
    return sa.finish(sa.reverb(sa.highpass(x, 120), .6, .3, damp=5000), 'effect')


def slam(v, rng):
    """The spade driven down into the turf: a heavy thud, earth thrown up, iron ringing."""
    s = 1.2
    thud = sa.thump(150, 60, 1.0, .2, drive=2.5, knock=1.0)
    dirt = _earth(.8, rng) * sa.decay(.8, .3, .005)
    ring = sa.bell(330, 1.0, .3, ratio=2.4, brightness=1.0)
    x = sa.mix(.7 * thud, (0, dirt), (0, .35 * ring))
    return sa.finish(sa.reverb(sa.highpass(x, 40), .8, .3, damp=5000), 'effect')


def fade(v, rng):
    """Gone to smoke: a quick hush of air, like a breath blown out."""
    s = .5
    x = sa.moving_band(s, ((0, 2400), (s, 700)), 1.2, rng) * sa.swell(s, .15, 1.5)
    return sa.finish(sa.reverb(sa.highpass(sa.norm(x), 200), .5, .3, damp=5000), 'effect')


def hurt(v, rng):
    """Struck: a short hollow moan."""
    s = .6
    x = _ghost(s, rng, ((0, 260), (.15, 300), (s, 190)), .3) * sa.decay(s, .25, .01)
    return sa.finish(sa.reverb(sa.highpass(sa.norm(x), 90), .5, .25, damp=5000), 'effect')


def death(v, rng):
    """Its end: a long sigh falling away into the earth, the lantern's chain going still, a last wind."""
    s = 3.2
    sigh = _ghost(s, rng, ((0, 300), (.6, 260), (2.4, 110), (s, 70)), .6) * sa.swell(s, .15, 1.0) * sa.decay(s, 1.6, .05)
    earth = _earth(2.0, rng) * sa.swell(2.0, .5, 1.5)
    x = sa.mix(sa.norm(sigh), (.8, .5 * earth), (.3, .3 * _chain(1.0, rng, 10) * sa.decay(1.0, .4, .01)))
    return sa.finish(sa.reverb(sa.highpass(x, 50), 2.4, .5, damp=4500), 'effect')


LANTERN_BEARER_EVENTS = [event('aura_lantern_bearer_' + name, build, role='effect', subtitle='lantern_bearer.' + name) for name, build in (
    ('roar', roar), ('rise', rise), ('step', step), ('reap', reap), ('hook', hook), ('chain', chain), ('wisp', wisp),
    ('burst', burst), ('slam', slam), ('fade', fade), ('hurt', hurt), ('death', death))]
