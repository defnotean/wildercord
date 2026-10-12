"""The Ashen Stag: a rutting stag's roar gone hoarse with smoke, embers crackling in its hide and antlers; its snorts and breath,
the scramble of it getting up, the crack of antlers and hooves, and its fall into the ash."""
from feel.core import sa, event


def _crackle(s, rng, rate=60, lo=1400, hi=6000):
    """Embers crackling over {s} seconds."""
    return sa.norm(sa.bandpass(sa.grains(s, rate, rng, shape=[(0, 1), (s, .6)]), lo, hi))


def _roar(s, rng, curve, rough=22, bright=900):
    """A stag's roar along {curve}: a hoarse buzzing groan, throatier the rougher it is."""
    voice = sa.soft_saw(sa.glide(s, *curve), s) * sa.jitter(s, rng, rough) ** 2
    throat = sa.bandpass(voice, 180, bright * 1.6)
    return sa.mix(sa.lowpass(voice, bright), (0, .5 * throat))


def bellow(v, rng):
    """Its bellow: a long hoarse roar rising out of its chest and breaking, smoke rasping in it, embers spitting off its antlers."""
    s = 2.6
    roar = _roar(s, rng, ((0, 88), (.5, 150), (1.4, 168), (2.0, 120), (s, 80)), 26, 1000) * sa.swell(s, .2, 1.4)
    rasp = sa.bandpass(sa.noise(s, rng), 900, 3200) * sa.swell(s, .35, 1.3)
    x = sa.mix(sa.norm(roar), (0, .25 * rasp), (.3, .25 * _crackle(2.0, rng, 80)))
    return sa.finish(sa.reverb(sa.highpass(x, 40), 1.2, .35, damp=3500), 'effect')


def breath(v, rng):
    """Bedded down: slow breath through its nostrils, and the embers in its hide ticking."""
    s = 2.6
    draw = sa.lowpass(sa.bandpass(sa.noise(s, rng), 300, 1800) * sa.swell(s, .5, 1.3), 2000)
    x = sa.mix(.6 * sa.norm(draw), (0, .3 * _crackle(s, rng, 25)))
    return sa.finish(sa.reverb(sa.highpass(x, 60), .6, .2, damp=3000), 'effect')


def rise(v, rng):
    """Getting up out of its bed: a grunt, hooves scrabbling in the ash, ash sliding off its back."""
    s = 2.2
    grunt = _roar(.6, rng, ((0, 120), (.6, 90)), 18, 600) * sa.decay(.6, .2, .02)
    scrabble = sa.mix(*[(t, .5 * sa.thump(160, 70, .2, .03, drive=1.6, knock=.5)) for t in (.3, .55, 1.1, 1.35, 1.7)])
    slide = sa.norm(sa.bandpass(sa.noise(s, rng), 600, 4000)) * sa.swell(s, .6, 1.4)
    x = sa.mix(.7 * grunt, (0, .6 * scrabble), (0, .6 * slide), (.2, .35 * _crackle(1.8, rng, 40)))
    return sa.finish(sa.reverb(sa.highpass(x, 110), .8, .3, damp=3500), 'effect')


def snort(v, rng):
    """A sharp snort through its nostrils, sparks blown out with it."""
    s = .55
    blast = sa.bandpass(sa.noise(s, rng), 400, 2600) * sa.decay(s, .12, .006)
    flutter = sa.lowpass(sa.chopper(s, rng, rate=(28, 36), floor=.3) * sa.noise(s, rng), 900) * sa.decay(s, .1, .005)
    x = sa.mix(sa.norm(blast), (0, .5 * sa.norm(flutter)), (.05, .3 * _crackle(.4, rng, 120)))
    return sa.finish(sa.reverb(sa.highpass(x, 120), .3, .12, damp=5000), 'effect')


def step(v, rng):
    """A hoof on burnt ground: a hard knock, a puff of ash."""
    knock = sa.thump(320, 140, .2, .025, drive=1.8, knock=.8)
    puff = sa.norm(sa.bandpass(sa.noise(.2, rng), 900, 5000)) * sa.decay(.2, .05, .002)
    return sa.finish(sa.reverb(sa.highpass(sa.mix(.6 * knock, (.005, .7 * puff)), 160), .3, .1, damp=4500), 'effect')


def gore(v, rng):
    """Its antlers flung up: a heavy whoosh of tines, a grunt behind them, sparks thrown off the tips."""
    s = .8
    whoosh = sa.moving_band(s, ((0, 600), (.18, 1800), (s, 800)), 1.1, rng) * sa.swell(s, .12, 2.2)
    grunt = _roar(.4, rng, ((0, 150), (.4, 110)), 16, 700) * sa.decay(.4, .12, .01)
    x = sa.mix(.9 * whoosh, (0, .6 * grunt), (.08, .35 * _crackle(.6, rng, 140)))
    return sa.finish(sa.reverb(sa.highpass(x, 60), .4, .15, damp=5000), 'effect')


def kick(v, rng):
    """Both hind hooves lashed back: a snap of air and a double crack."""
    s = .6
    snap = sa.moving_band(s, ((0, 1200), (.08, 2600), (s, 1200)), 1.0, rng) * sa.swell(s, .06, 2.6)
    crack = sa.mix((0, sa.thump(260, 100, .25, .02, drive=2.2, knock=1.0)), (.035, .8 * sa.thump(240, 90, .25, .02, drive=2.2, knock=1.0)))
    x = sa.mix(.7 * snap, (.05, crack))
    return sa.finish(sa.reverb(sa.highpass(x, 70), .35, .15, damp=5000), 'effect')


def stomp(v, rng):
    """Both forehooves down from its full height: a deep blow into the ground and a burst of ash and embers."""
    blow = sa.thump(110, 38, .9, .16, drive=2.6, knock=1.0)
    burst = sa.norm(sa.bandpass(sa.noise(1.0, rng), 500, 4500)) * sa.decay(1.0, .25, .01)
    x = sa.mix(.8 * blow, (.01, .9 * burst), (.03, .7 * _crackle(1.2, rng, 160, 1000)))
    return sa.finish(sa.reverb(sa.highpass(x, 90), 1.0, .3, damp=3500), 'effect')


def paw(v, rng):
    """Pawing the ash before a charge: three scrapes of a hoof, and a low rumbling snort."""
    scrapes = sa.mix(*[(t, .6 * sa.moving_band(.25, ((0, 1600), (.25, 700)), .8, rng) * sa.decay(.25, .08, .004)) for t in (0, .35, .7)])
    rumble = _roar(1.0, rng, ((0, 80), (1.0, 70)), 30, 400) * sa.swell(1.0, .3, 1.0)
    x = sa.mix(.8 * scrapes, (.3, .5 * rumble))
    return sa.finish(sa.reverb(sa.highpass(x, 40), .5, .2, damp=4000), 'effect')


def crash(v, rng):
    """A charge run into a trunk: a great crack of antler into wood, splinters, and bark and ash falling."""
    s = 1.6
    blow = sa.thump(240, 80, .6, .08, drive=3.0, knock=1.0)
    splinter = sa.norm(sa.bandpass(sa.chopper(.6, rng, rate=(30, 60), floor=0) * sa.noise(.6, rng), 1200, 6000)) * sa.decay(.6, .18, .005)
    fall = sa.norm(sa.bandpass(sa.noise(s, rng), 400, 3000)) * sa.swell(s, .4, 1.2) * sa.decay(s, .6, .02)
    x = sa.mix(.6 * blow, (.01, 1.0 * splinter), (.15, .8 * fall))
    return sa.finish(sa.reverb(sa.highpass(x, 140), .9, .3, damp=4000), 'effect')


def strain(v, rng):
    """Heaving at its stuck antlers: a strained groan, wood creaking."""
    s = 1.2
    groan = _roar(s, rng, ((0, 100), (.6, 130), (s, 95)), 20, 700) * sa.swell(s, .3, 1.2)
    creak = sa.bandpass(sa.chopper(s, rng, rate=(14, 22), floor=.1) * sa.noise(s, rng), 400, 1600) * sa.swell(s, .5, 1.5)
    x = sa.mix(.8 * sa.norm(groan), (.1, .5 * sa.norm(creak)))
    return sa.finish(sa.reverb(sa.highpass(x, 40), .5, .2, damp=4000), 'effect')


def bound(v, rng):
    """A spring aside: the push-off, a rush of air, and the landing."""
    s = .7
    push = sa.thump(200, 80, .2, .025, drive=1.8, knock=.7)
    air = sa.moving_band(s, ((0, 700), (.2, 1600), (s, 900)), .9, rng) * sa.swell(s, .15, 2.0)
    land = sa.thump(160, 60, .3, .04, drive=2.0, knock=.8)
    x = sa.mix(.6 * push, (0, .5 * air), (.3, .8 * land))
    return sa.finish(sa.reverb(sa.highpass(x, 60), .35, .15, damp=4500), 'effect')


def hurt(v, rng):
    """A blow that lands: a short pained bleat-roar, and embers knocked loose."""
    s = .6
    cry = _roar(s, rng, ((0, 190), (.15, 230), (s, 140)), 14, 1200) * sa.decay(s, .2, .015)
    x = sa.mix(sa.norm(cry), (.02, .35 * _crackle(.5, rng, 150)))
    return sa.finish(sa.reverb(sa.highpass(x, 60), .4, .15, damp=4500), 'effect')


def death(v, rng):
    """It falls: a last long groan sinking away, a heavy thud into the ash, and its embers hissing out."""
    groan = _roar(2.0, rng, ((0, 140), (.6, 120), (2.0, 60)), 24, 700) * sa.swell(2.0, .2, 1.0) * sa.decay(2.0, 1.2, .05)
    thud = sa.thump(90, 32, 1.2, .3, drive=2.4, knock=1.0)
    dust = sa.norm(sa.bandpass(sa.noise(1.0, rng), 400, 3000)) * sa.decay(1.0, .3, .01)
    hiss = sa.norm(sa.bandpass(sa.noise(2.4, rng), 2000, 8000)) * sa.decay(2.4, .9, .05)
    x = sa.mix(.8 * sa.norm(groan), (1.4, .7 * thud), (1.45, .7 * dust), (1.6, .4 * hiss), (1.6, .4 * _crackle(2.0, rng, 50)))
    return sa.finish(sa.reverb(sa.highpass(x, 80), 1.6, .4, damp=3000), 'effect')


ASHEN_STAG_EVENTS = [event('aura_ashen_stag_' + name, build, role='effect', subtitle='ashen_stag.' + name) for name, build in (
    ('bellow', bellow), ('breath', breath), ('rise', rise), ('snort', snort), ('step', step), ('gore', gore), ('kick', kick),
    ('stomp', stomp), ('paw', paw), ('crash', crash), ('strain', strain), ('bound', bound), ('hurt', hurt), ('death', death))]
