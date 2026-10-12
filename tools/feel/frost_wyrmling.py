"""The Frost Wyrmling: a young drake's roar, higher and rawer than a grown one's, with ice ringing in it; its frost breath, its snarls
and snaps, the leathery crack of its wings, claws on packed snow and ice, and its fall."""
from feel.core import sa, event


def _tinkle(s, rng, rate=40, lo=3500, hi=9000):
    """Ice crystals ringing and tinkling over {s} seconds."""
    return sa.norm(sa.bandpass(sa.grains(s, rate, rng, shape=[(0, 1), (s, .5)]), lo, hi))


def _voice(s, rng, curve, rough=18, bright=1400):
    """A young drake's cry along {curve}: a reedy, rasping roar, hoarser the rougher it is."""
    voice = sa.soft_saw(sa.glide(s, *curve), s) * sa.jitter(s, rng, rough) ** 2
    throat = sa.bandpass(voice, 260, bright * 1.8)
    return sa.mix(sa.lowpass(voice, bright), (0, .55 * throat))


def _hiss(s, rng, lo=1800, hi=7000):
    """Frost hissing out of its throat."""
    return sa.norm(sa.bandpass(sa.noise(s, rng), lo, hi))


def roar(v, rng):
    """Its roar, wings thrown wide: a rasping cry climbing and cracking at its top, frost hissing out with it and ice ringing off the walls."""
    s = 2.4
    cry = _voice(s, rng, ((0, 140), (.4, 260), (1.2, 310), (1.8, 230), (s, 150)), 22, 1600) * sa.swell(s, .15, 1.3)
    frost = _hiss(s, rng) * sa.swell(s, .4, 1.2)
    x = sa.mix(sa.norm(cry), (.1, .3 * frost), (.5, .3 * _tinkle(1.9, rng)))
    return sa.finish(sa.reverb(sa.highpass(x, 60), 1.6, .4, damp=6000), 'effect')


def breath(v, rng):
    """Its frost breath: a long icy roar of air, crystals rattling and ringing in it, a growl underneath."""
    s = 2.0
    blast = sa.moving_band(s, ((0, 1200), (.3, 3200), (s, 2200)), 1.2, rng) * sa.swell(s, .2, 1.4)
    growl = _voice(s, rng, ((0, 90), (s, 80)), 30, 500) * sa.swell(s, .3, 1.0)
    x = sa.mix(sa.norm(blast), (0, .35 * sa.norm(growl)), (.15, .45 * _tinkle(1.8, rng, 80)))
    return sa.finish(sa.reverb(sa.highpass(x, 60), .8, .3, damp=7000), 'effect')


def rise(v, rng):
    """Uncurling: a low trill, scales scraping over the snow, claws taking hold, snow sliding off its back."""
    s = 2.0
    trill = sa.lowpass(_voice(.8, rng, ((0, 110), (.8, 150)), 26, 700) * sa.chopper(.8, rng, rate=(18, 24), floor=.4), 1200)
    scrape = sa.norm(sa.bandpass(sa.noise(s, rng), 900, 5000)) * sa.swell(s, .7, 1.4)
    claws = sa.mix(*[(t, .5 * sa.thump(260, 110, .15, .02, drive=1.6, knock=.6)) for t in (.5, .75, 1.2, 1.4)])
    x = sa.mix(.6 * sa.norm(trill), (.2, .45 * scrape), (0, .6 * claws), (.4, .3 * _tinkle(1.4, rng, 30)))
    return sa.finish(sa.reverb(sa.highpass(x, 100), .9, .3, damp=6000), 'effect')


def snarl(v, rng):
    """A snarl before it strikes: a short rattling growl, frost puffing from its teeth."""
    s = .7
    growl = _voice(s, rng, ((0, 170), (.3, 210), (s, 150)), 34, 1100) * sa.decay(s, .25, .02)
    puff = _hiss(s, rng, 2500, 7500) * sa.decay(s, .15, .01)
    x = sa.mix(sa.norm(growl), (.02, .3 * puff))
    return sa.finish(sa.reverb(sa.highpass(x, 90), .35, .15, damp=6000), 'effect')


def step(v, rng):
    """A clawed foot on packed snow: a soft crunch and a click of claws on ice."""
    crunch = sa.norm(sa.bandpass(sa.chopper(.18, rng, rate=(60, 90), floor=0) * sa.noise(.18, rng), 700, 4000)) * sa.decay(.18, .06, .003)
    click = sa.thump(900, 500, .05, .006, drive=1.4, knock=.9)
    thud = sa.thump(140, 60, .2, .03, drive=1.6, knock=.4)
    return sa.finish(sa.reverb(sa.highpass(sa.mix(.6 * thud, (0, .7 * crunch), (.01, .3 * click)), 90), .3, .1, damp=6000), 'effect')


def bite(v, rng):
    """Its jaws snapping shut: a quick lunge of air and a hard clack of teeth, ice cracking off them."""
    s = .5
    lunge = sa.moving_band(s, ((0, 900), (.08, 2200), (s, 1000)), 1.0, rng) * sa.swell(s, .07, 2.4)
    clack = sa.thump(700, 260, .12, .01, drive=2.4, knock=1.0)
    x = sa.mix(.6 * lunge, (.08, clack), (.09, .3 * _tinkle(.3, rng, 120)))
    return sa.finish(sa.reverb(sa.highpass(x, 120), .3, .12, damp=6500), 'effect')


def tail(v, rng):
    """Its tail swept round: a long heavy whoosh along the ground, snow spraying off its fin."""
    s = .9
    sweep = sa.moving_band(s, ((0, 400), (.3, 1400), (s, 600)), 1.1, rng) * sa.swell(s, .25, 1.8)
    spray = sa.norm(sa.bandpass(sa.noise(s, rng), 1500, 6000)) * sa.swell(s, .4, 2.0) * sa.decay(s, .5, .02)
    x = sa.mix(sa.norm(sweep), (.15, .4 * spray))
    return sa.finish(sa.reverb(sa.highpass(x, 60), .45, .15, damp=6000), 'effect')


def wing(v, rng):
    """One great beat of its wings: the leathery crack of membrane snapping taut and a gust of air and snow."""
    s = 1.0
    crack = sa.thump(220, 90, .25, .02, drive=2.6, knock=1.0)
    snap = sa.norm(sa.bandpass(sa.noise(.15, rng), 600, 3000)) * sa.decay(.15, .04, .002)
    gust = sa.moving_band(s, ((0, 300), (.15, 900), (s, 400)), 1.3, rng) * sa.swell(s, .12, 1.6)
    x = sa.mix(.6 * crack, (0, .6 * snap), (.02, .9 * gust), (.2, .25 * _tinkle(.8, rng, 30)))
    return sa.finish(sa.reverb(sa.highpass(x, 50), .6, .2, damp=5000), 'effect')


def frost(v, rng):
    """Frost taking hold: a crackle of ice spreading fast and a thin ringing."""
    s = .9
    spread = sa.norm(sa.bandpass(sa.chopper(s, rng, rate=(70, 110), floor=0) * sa.noise(s, rng), 2500, 9000)) * sa.decay(s, .35, .01)
    x = sa.mix(.7 * spread, (.05, .5 * _tinkle(.8, rng, 60, 4500, 10000)))
    return sa.finish(sa.reverb(sa.highpass(x, 800), .6, .2, damp=9000), 'effect')


def land(v, rng):
    """Down from a pounce: a heavy thud into the snow, ice cracking under it and a burst of powder."""
    blow = sa.thump(120, 42, .7, .12, drive=2.4, knock=1.0)
    burst = sa.norm(sa.bandpass(sa.noise(.9, rng), 700, 6000)) * sa.decay(.9, .22, .01)
    crack = sa.norm(sa.bandpass(sa.chopper(.4, rng, rate=(40, 70), floor=0) * sa.noise(.4, rng), 2000, 8000)) * sa.decay(.4, .12, .005)
    x = sa.mix(.8 * blow, (.01, .8 * burst), (.02, .5 * crack))
    return sa.finish(sa.reverb(sa.highpass(x, 70), 1.0, .3, damp=5000), 'effect')


def bound(v, rng):
    """A hop aside: the push-off, wings flicking, and the landing in the snow."""
    s = .7
    push = sa.thump(220, 90, .2, .02, drive=1.8, knock=.6)
    flick = sa.moving_band(.3, ((0, 800), (.1, 1800), (.3, 900)), .9, rng) * sa.swell(.3, .08, 2.0)
    land_ = sa.thump(170, 70, .25, .03, drive=1.8, knock=.6)
    puff = sa.norm(sa.bandpass(sa.noise(.3, rng), 1000, 5000)) * sa.decay(.3, .08, .004)
    x = sa.mix(.6 * push, (.05, .6 * flick), (.32, .7 * land_), (.33, .4 * puff))
    return sa.finish(sa.reverb(sa.highpass(x, 70), .35, .15, damp=6000), 'effect')


def hurt(v, rng):
    """A blow that lands: a sharp yelping shriek, and ice knocked off its scales."""
    s = .55
    cry = _voice(s, rng, ((0, 300), (.1, 420), (s, 240)), 14, 2000) * sa.decay(s, .18, .012)
    x = sa.mix(sa.norm(cry), (.02, .3 * _tinkle(.4, rng, 100)))
    return sa.finish(sa.reverb(sa.highpass(x, 100), .45, .15, damp=6500), 'effect')


def death(v, rng):
    """It falls: a long cry sinking to a whimper, a thud into the snow, and frost settling with a last ring of ice."""
    cry = _voice(2.0, rng, ((0, 280), (.5, 240), (1.4, 130), (2.0, 90)), 22, 1400) * sa.swell(2.0, .15, 1.0) * sa.decay(2.0, 1.1, .05)
    thud = sa.thump(100, 36, 1.0, .25, drive=2.2, knock=1.0)
    powder = sa.norm(sa.bandpass(sa.noise(1.0, rng), 800, 5000)) * sa.decay(1.0, .3, .01)
    x = sa.mix(.8 * sa.norm(cry), (1.4, .7 * thud), (1.42, .6 * powder), (1.6, .4 * _tinkle(1.8, rng, 25)))
    return sa.finish(sa.reverb(sa.highpass(x, 80), 1.6, .4, damp=6000), 'effect')


FROST_WYRMLING_EVENTS = [event('aura_frost_wyrmling_' + name, build, role='effect', subtitle='frost_wyrmling.' + name) for name, build in (
    ('roar', roar), ('breath', breath), ('rise', rise), ('snarl', snarl), ('step', step), ('bite', bite), ('tail', tail),
    ('wing', wing), ('frost', frost), ('land', land), ('bound', bound), ('hurt', hurt), ('death', death))]
