"""The Rootcoil Serpent: a sleeping hiss, the waking rattle of its scales, the long grind of its coils sliding off the bark,
its rearing challenge, a hurt hiss and the death rattle."""
from feel.core import sa, event


def hiss(v, rng):
    """A slow breath of a hiss from high in the tree: breathy, not loud."""
    s = 1.6
    x = sa.moving_band(s, ((0, 2600), (.6, 3800), (s, 3000)), .5, rng) * sa.swell(s, .55, 1.6)
    return sa.finish(sa.reverb(sa.highpass(x, 1200), .5, .2, damp=6000), 'effect')


def wake(v, rng):
    """Its eyes open: a sharp intake, a rattle of scales down the coils, a low thrum through the trunk."""
    s = 1.4
    breath = sa.moving_band(s, ((0, 1800), (.25, 4200), (s, 2200)), .6, rng) * sa.swell(s, .3, 2)
    rattle = sa.bandpass(sa.chopper(s, rng, rate=(26, 44), floor=.05) * sa.noise(s, rng), 2500, 7000) * sa.swell(s, .5, 1.4)
    thrum = sa.thump(70, 38, s, .5, drive=1.3, knock=.15)
    x = sa.mix(breath, (.15, .7 * rattle), (.05, .3 * thrum))
    return sa.finish(sa.reverb(sa.highpass(x, 90), .7, .25, damp=5200), 'effect')


def uncoil(v, rng):
    """Body sliding off bark: a long scaly drag that rises and falls, with creaks from the tree."""
    s = 2.0
    drag = sa.moving_band(s, ((0, 700), (1, 1300), (s, 600)), 1.0, rng) * sa.swell(s, 1.0, 1.4)
    scale = sa.bandpass(sa.grains(s, 90, rng)[:len(drag)], 1800, 5200) * sa.swell(s, .8, 1.2)
    creak = sa.mix(sa.partials(92, .9, ((1, 1, 1), (2.03, .4, .5), (3.1, .2, .3)), .35), (.8, .7 * sa.partials(78, .9, ((1, 1, 1), (2.1, .35, .4)), .3)))
    x = sa.mix(.8 * drag, (0, .4 * scale), (.25, .3 * creak))
    return sa.finish(sa.reverb(sa.highpass(x, 45), .6, .22, damp=4200), 'effect')


def challenge(v, rng):
    """Reared over the island: a hood-wide hiss over a deep chest growl, and the fight begins."""
    s = 1.8
    hiss_ = sa.moving_band(s, ((0, 3000), (.2, 5600), (s, 3600)), .6, rng) * sa.swell(s, .18, 1.5)
    growl = sa.lowpass(sa.soft_saw(sa.glide(s, (0, 62), (.4, 74), (s, 55)), s) * sa.jitter(s, rng, 30) ** 2, 600) * sa.swell(s, .3, 1.6)
    hit = sa.thump(120, 40, .6, .1, drive=1.8, knock=.4)
    x = sa.mix(hiss_, (0, .7 * growl), (0, .6 * hit))
    return sa.finish(sa.reverb(sa.highpass(x, 35), .9, .3, damp=5000), 'effect')


def hurt(v, rng):
    """A short, spitting hiss."""
    s = .45
    x = sa.moving_band(s, ((0, 5200), (s, 3600)), .5, rng) * sa.decay(s, .14, .006)
    return sa.finish(sa.reverb(sa.highpass(x, 1500), .25, .12, damp=7000), 'effect')


def death(v, rng):
    """A long hiss running out of breath, a rattle, and the weight of it coming down."""
    s = 3.0
    breath = sa.moving_band(s, ((0, 4800), (1.5, 2400), (s, 900)), .7, rng) * sa.swell(s, .2, 1.1)
    rattle = sa.bandpass(sa.chopper(s, rng, rate=(14, 30), floor=.0) * sa.noise(s, rng), 1500, 5000) * sa.swell(s, 1.2, 2)
    fall = sa.thump(90, 30, 1.2, .35, drive=2, knock=.5)
    x = sa.mix(breath, (.4, .4 * rattle), (1.6, fall))
    return sa.finish(sa.reverb(sa.highpass(x, 30), 1.0, .3, damp=4000), 'effect')


SERPENT_EVENTS = [event('aura_serpent_' + name, build, role='effect', subtitle='serpent.' + name) for name, build in (
    ('hiss', hiss), ('wake', wake), ('uncoil', uncoil), ('challenge', challenge), ('hurt', hurt), ('death', death))]
