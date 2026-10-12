"""The Hollow Matron: a great spider's dry hiss and the clicking chitter of her fangs, silk creaking under her weight, the patter of
eight legs on stone, the snap of her bite, web spat wet, and her curling up at the last."""
from feel.core import sa, event


def _clicks(s, rng, rate=40, lo=1800, hi=7000):
    """Dry clicking over {s} seconds, like mandibles."""
    return sa.norm(sa.bandpass(sa.grains(s, rate, rng, shape=[(0, 1), (s, .7)]), lo, hi))


def _patter(times, rng, pitch=1.0):
    """Leg tips on stone at {times}: light hard taps."""
    return sa.mix(*[(t, (.5 + .3 * rng.random()) * sa.thump(900 * pitch, 400 * pitch, .06, .006, drive=1.6, knock=1.0)) for t in times])


def hiss(v, rng):
    """Her hiss: a long dry rasp of breath through spread fangs, swelling, with a rattle of clicks in it."""
    s = 1.8
    rasp = sa.norm(sa.bandpass(sa.noise(s, rng), 2200, 9000)) * sa.swell(s, .15, 1.4)
    throat = sa.norm(sa.moving_band(s, ((0, 1400), (.6, 2600), (s, 1800)), 1.4, rng)) * sa.swell(s, .25, 1.2)
    x = sa.mix(.8 * rasp, (0, .45 * throat), (.2, .35 * _clicks(1.2, rng, 55)))
    return sa.finish(sa.reverb(sa.highpass(x, 200), .9, .3, damp=6000), 'effect')


def chitter(v, rng):
    """Her fangs working: a quick run of clicks, trailing off."""
    s = .7
    run = sa.chopper(s, rng, rate=(26, 40), floor=0) * sa.noise(s, rng)
    x = sa.mix(sa.norm(sa.bandpass(run, 1600, 6500)) * sa.decay(s, .35, .02), (0, .4 * _clicks(s, rng, 30)))
    return sa.finish(sa.reverb(sa.highpass(x, 300), .4, .15, damp=6500), 'effect')


def creak(v, rng):
    """Her thread taking her weight: a thin stretched creak, rising and falling."""
    s = 1.4
    tone = sa.soft_saw(sa.glide(s, (0, 420), (.7, 520), (s, 460)), s) * sa.jitter(s, rng, 12) ** 2
    strain = sa.bandpass(sa.chopper(s, rng, rate=(10, 18), floor=.2) * tone, 500, 2400) * sa.swell(s, .4, 1.4)
    return sa.finish(sa.reverb(sa.highpass(.7 * sa.norm(strain), 200), .7, .3, damp=5000), 'effect')


def drop(v, rng):
    """Letting go: silk paying out fast in a long hiss, falling in pitch, and the rush of her coming down."""
    s = 1.1
    pay = sa.norm(sa.moving_band(s, ((0, 5000), (.5, 2500), (s, 1400)), 1.6, rng)) * sa.swell(s, .05, 1.6) * sa.decay(s, .6, .05)
    rush = sa.norm(sa.lowpass(sa.noise(s, rng), 900)) * sa.swell(s, .5, 1.3)
    x = sa.mix(.7 * pay, (0, .5 * rush))
    return sa.finish(sa.reverb(sa.highpass(x, 80), .8, .3, damp=5000), 'effect')


def land(v, rng):
    """Landing: a soft heavy thump of her body and a scatter of legs catching her, grit spraying."""
    blow = sa.thump(140, 50, .5, .06, drive=2.0, knock=.6)
    legs = _patter([.0, .02, .04, .07, .09, .12, .15, .18], rng, .8)
    grit = sa.norm(sa.bandpass(sa.noise(.6, rng), 1500, 6000)) * sa.decay(.6, .15, .005)
    x = sa.mix(.8 * blow, (0, .5 * legs), (.01, .4 * grit))
    return sa.finish(sa.reverb(sa.highpass(x, 60), 1.0, .3, damp=4500), 'effect')


def step(v, rng):
    """Her legs on stone: a quick ripple of taps."""
    times = sorted(.012 * i + .01 * rng.random() for i in range(4))
    return sa.finish(sa.reverb(sa.highpass(.6 * _patter(times, rng), 300), .3, .1, damp=6000), 'effect')


def bite(v, rng):
    """The bite: a short lunge of air and her fangs snapping shut, hard and wet."""
    s = .5
    lunge = sa.moving_band(s, ((0, 900), (.1, 2200), (s, 900)), 1.0, rng) * sa.swell(s, .06, 2.4)
    snap = sa.mix((0, sa.thump(1400, 600, .08, .006, drive=2.4, knock=1.0)), (.025, .8 * sa.thump(1200, 500, .08, .006, drive=2.4, knock=1.0)))
    wet = sa.norm(sa.bandpass(sa.noise(.2, rng), 700, 2500)) * sa.decay(.2, .04, .002)
    x = sa.mix(.6 * lunge, (.09, snap), (.1, .3 * wet))
    return sa.finish(sa.reverb(sa.highpass(x, 120), .35, .15, damp=6000), 'effect')


def sweep(v, rng):
    """Her forelegs swept across: two long whips of air, one just behind the other, bristles rasping."""
    s = .7
    whip = lambda: sa.moving_band(.45, ((0, 900), (.12, 3000), (.45, 1000)), 1.2, rng) * sa.swell(.45, .1, 2.6)
    bristle = sa.norm(sa.bandpass(sa.noise(s, rng), 3500, 9000)) * sa.swell(s, .2, 2.0) * sa.decay(s, .3, .02)
    x = sa.mix(.8 * whip(), (.06, .7 * whip()), (.05, .25 * bristle))
    return sa.finish(sa.reverb(sa.highpass(x, 100), .4, .15, damp=5500), 'effect')


def spit(v, rng):
    """The spit: a wet gurgling push from her abdomen and the glob leaving with a thick puff."""
    s = .6
    gurgle = sa.bandpass(sa.chopper(.35, rng, rate=(18, 30), floor=.1) * sa.noise(.35, rng), 300, 1400) * sa.swell(.35, .25, 1.4)
    puff = sa.norm(sa.bandpass(sa.noise(.3, rng), 500, 2400)) * sa.swell(.3, .03, 2.0) * sa.decay(.3, .1, .004)
    x = sa.mix(.6 * sa.norm(gurgle), (.3, .9 * puff), (.3, .4 * sa.thump(300, 120, .15, .015, drive=1.4, knock=.4)))
    return sa.finish(sa.reverb(sa.highpass(x, 90), .4, .15, damp=4500), 'effect')


def splat(v, rng):
    """A glob of web landing: a thick wet slap and strands snapping tight."""
    s = .5
    slap = sa.norm(sa.lowpass(sa.noise(.2, rng), 1800)) * sa.decay(.2, .05, .003)
    strands = sa.norm(sa.bandpass(sa.chopper(s, rng, rate=(30, 50), floor=0) * sa.noise(s, rng), 1500, 5000)) * sa.decay(s, .15, .006)
    x = sa.mix(.9 * slap, (0, .5 * sa.thump(220, 90, .15, .02, drive=1.6, knock=.5)), (.03, .35 * strands))
    return sa.finish(sa.reverb(sa.highpass(x, 80), .35, .12, damp=5000), 'effect')


def pounce(v, rng):
    """The leap: a scrabbling push-off, a hiss as she flies, and the weight of her coming down."""
    s = 1.0
    push = _patter([0, .015, .03, .045], rng, .7)
    air = sa.norm(sa.moving_band(.5, ((0, 1200), (.25, 3200), (.5, 1600)), 1.3, rng)) * sa.swell(.5, .1, 1.5)
    land_ = sa.thump(130, 45, .5, .06, drive=2.2, knock=.7)
    x = sa.mix(.5 * push, (.03, .55 * air), (.05, .3 * _clicks(.4, rng, 60)), (.5, .9 * land_))
    return sa.finish(sa.reverb(sa.highpass(x, 60), .7, .25, damp=5000), 'effect')


def skitter(v, rng):
    """A skitter aside: a fast flurry of legs across the stone."""
    times = sorted(.25 * rng.random() for _ in range(14))
    return sa.finish(sa.reverb(sa.highpass(.6 * _patter(times, rng, 1.1), 300), .35, .12, damp=6000), 'effect')


def hurt(v, rng):
    """A blow that lands: a sharp hissing screech, clicks bursting out of her."""
    s = .55
    shriek = sa.soft_saw(sa.glide(s, (0, 900), (.1, 1300), (s, 700)), s) * sa.jitter(s, rng, 30) ** 2
    x = sa.mix(sa.norm(sa.bandpass(shriek, 700, 4500)) * sa.decay(s, .15, .01),
               (0, .5 * sa.norm(sa.bandpass(sa.noise(s, rng), 3000, 9000)) * sa.decay(s, .12, .006)), (.02, .35 * _clicks(.4, rng, 70)))
    return sa.finish(sa.reverb(sa.highpass(x, 200), .4, .15, damp=6000), 'effect')


def death(v, rng):
    """She dies: a long shuddering hiss sinking away, her body rolling over with a soft thump, and her legs ticking as they curl."""
    s = 2.4
    rasp = sa.norm(sa.bandpass(sa.noise(s, rng), 1500, 7000)) * sa.swell(s, .1, 1.0) * sa.decay(s, 1.0, .05)
    shudder = sa.chopper(s, rng, rate=(8, 14), floor=.4)
    thump = sa.thump(110, 40, .8, .12, drive=2.0, knock=.6)
    ticks = _patter(sorted(.8 + 1.2 * rng.random() for _ in range(9)), rng, .9)
    x = sa.mix(.7 * rasp * shudder, (.6, .7 * thump), (.6, .3 * ticks))
    return sa.finish(sa.reverb(sa.highpass(x, 60), 1.4, .35, damp=4000), 'effect')


def climb(v, rng):
    """Scrabbling up silk: legs snagging and tugging free in quick runs, strands creaking under her."""
    s = .9
    times = sorted(.6 * rng.random() for _ in range(10))
    snag = sa.norm(sa.bandpass(sa.chopper(s, rng, rate=(20, 34), floor=0) * sa.noise(s, rng), 1200, 4500)) * sa.swell(s, .2, 1.4) * sa.decay(s, .5, .03)
    x = sa.mix(.45 * _patter(times, rng, .8), (0, .4 * snag))
    return sa.finish(sa.reverb(sa.highpass(x, 150), .5, .2, damp=5500), 'effect')


def hatch(v, rng):
    """An egg sac splitting: a soft wet tear, then a spreading patter of many tiny legs."""
    s = 1.2
    split = sa.norm(sa.bandpass(sa.noise(.35, rng), 600, 3000)) * sa.swell(.35, .05, 1.8) * sa.decay(.35, .15, .004)
    tiny = _patter(sorted(.15 + .9 * rng.random() for _ in range(26)), rng, 2.2)
    x = sa.mix(.7 * split, (.1, .5 * tiny), (.05, .25 * _clicks(.8, rng, 70, 3000, 9000)))
    return sa.finish(sa.reverb(sa.highpass(x, 200), .5, .2, damp=6500), 'effect')


def tear(v, rng):
    """Silk straining under a blow: a fibrous rip, strands snapping one after another."""
    s = .6
    rip = sa.norm(sa.moving_band(s, ((0, 1500), (.15, 4000), (s, 2000)), 1.3, rng)) * sa.swell(s, .03, 2.0) * sa.decay(s, .2, .01)
    snaps = sa.mix(*[(t, (.4 + .4 * rng.random()) * sa.thump(1800, 900, .04, .003, drive=2.0, knock=1.0))
                     for t in sorted(.3 * rng.random() for _ in range(6))])
    x = sa.mix(.7 * rip, (.02, .5 * snaps), (0, .35 * sa.thump(260, 120, .15, .02, drive=1.4, knock=.5)))
    return sa.finish(sa.reverb(sa.highpass(x, 120), .5, .2, damp=6000), 'effect')


HOLLOW_MATRON_EVENTS = [event('aura_hollow_matron_' + name, build, role='effect', subtitle='hollow_matron.' + name) for name, build in (
    ('hiss', hiss), ('chitter', chitter), ('creak', creak), ('drop', drop), ('land', land), ('step', step), ('bite', bite),
    ('sweep', sweep), ('spit', spit), ('splat', splat), ('pounce', pounce), ('skitter', skitter), ('hurt', hurt), ('death', death),
    ('climb', climb), ('hatch', hatch), ('tear', tear))]
