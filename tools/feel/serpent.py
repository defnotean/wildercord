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


def strike(v, rng):
    """A bite: a spat hiss and the snap of its jaws closing on air or armour."""
    s = .5
    spit = sa.moving_band(s, ((0, 1800), (.08, 3800), (s, 2600)), .6, rng) * sa.decay(s, .1, .004)
    snap = sa.bandpass(sa.thump(420, 220, .25, .03, drive=2.4, knock=.9), 300, 3000)
    x = sa.mix(.8 * spit, (.05, .7 * snap))
    return sa.finish(sa.reverb(sa.highpass(x, 200), .3, .15, damp=6500), 'effect')


def lunge(v, rng):
    """Its whole length thrown forward: a rush of scales over the ground under a long hiss."""
    s = .9
    rush = sa.moving_band(s, ((0, 600), (.3, 1600), (s, 800)), 1.0, rng) * sa.swell(s, .12, 1.8)
    scale = sa.bandpass(sa.grains(s, 140, rng)[:len(rush)], 2000, 6000) * sa.swell(s, .2, 1.6)
    hiss_ = sa.moving_band(s, ((0, 3000), (s, 4600)), .5, rng) * sa.swell(s, .1, 1.4)
    x = sa.mix(.8 * rush, (0, .45 * scale), (0, .4 * hiss_))
    return sa.finish(sa.reverb(sa.highpass(x, 50), .4, .18, damp=5000), 'effect')


def constrict(v, rng):
    """The coils closing: a slow scaly grind and a creak of pressure under it."""
    s = 1.0
    grind = sa.moving_band(s, ((0, 500), (.5, 900), (s, 450)), .8, rng) * sa.swell(s, .25, 1.3)
    scale = sa.bandpass(sa.grains(s, 60, rng)[:len(grind)], 1500, 4500) * sa.swell(s, .3, 1.2)
    creak = sa.partials(70, s, ((1, 1, 1), (2.05, .4, .5), (3.2, .2, .3)), .3)
    x = sa.mix(.8 * grind, (0, .5 * scale), (.2, .45 * creak))
    return sa.finish(sa.reverb(sa.highpass(x, 40), .4, .2, damp=4500), 'effect')


def sidestep(v, rng):
    """A blow slipped: a quick skid of scales."""
    s = .35
    x = sa.moving_band(s, ((0, 1400), (s, 2800)), .7, rng) * sa.decay(s, .08, .003)
    grit = sa.bandpass(sa.grains(s, 120, rng)[:len(x)], 2500, 7000) * sa.decay(s, .06, .002)
    return sa.finish(sa.reverb(sa.highpass(sa.mix(x, (0, .5 * grit)), 200), .2, .1, damp=7000), 'effect')


def dive(v, rng):
    """It rears and hisses, then slides under: a heavy splash and the water closing over it."""
    s = 1.8
    hiss_ = sa.moving_band(s, ((0, 3600), (.3, 5200), (.8, 3000)), .5, rng) * sa.swell(s, .15, 2.4)
    splash = sa.norm(sa.bandpass(sa.noise(.6, rng), 250, 2800)) * sa.decay(.6, .1, .002)
    plunge = sa.thump(80, 36, .6, .12, drive=1.8, knock=.4)
    bubbles = sa.norm(sa.bubbles(.8, 26, rng))
    x = sa.mix(.7 * hiss_, (.8, .8 * splash), (.8, .5 * plunge), (1.0, .35 * bubbles))
    return sa.finish(sa.reverb(sa.highpass(x, 35), .7, .25, damp=4500), 'effect')


def burst(v, rng):
    """Out of the water at you: a surge of bubbles, the surface torn open, and a hiss as it comes."""
    s = 1.2
    rise = sa.norm(sa.bubbles(.5, 40, rng)) * sa.swell(.5, .45, .05)
    tear = sa.norm(sa.bandpass(sa.noise(.7, rng), 300, 3600)) * sa.decay(.7, .14, .002)
    hiss_ = sa.moving_band(.7, ((0, 4200), (.7, 3200)), .5, rng) * sa.decay(.7, .25, .01)
    hit = sa.thump(110, 40, .4, .08, drive=2, knock=.5)
    x = sa.mix(.4 * rise, (.45, .9 * tear), (.45, .5 * hit), (.5, .5 * hiss_))
    return sa.finish(sa.reverb(sa.highpass(x, 40), .6, .22, damp=5000), 'effect')


def slam(v, rng):
    """Its tail brought down on the island: a deep crack through the ground and the earth torn up and thrown."""
    s = 1.4
    crack = sa.thump(70, 30, .7, .1, drive=2.4, knock=.8)
    snap = sa.norm(sa.bandpass(sa.noise(.25, rng), 600, 3200)) * sa.decay(.25, .04, .001)
    rubble = sa.norm(sa.bandpass(sa.noise(1.0, rng), 300, 2600)) * sa.decay(1.0, .35, .01)
    whoosh = sa.moving_band(.5, ((0, 900), (.5, 2400)), .6, rng) * sa.swell(.5, .4, .05)
    x = sa.mix(.5 * whoosh, (.4, .9 * crack), (.4, .7 * snap), (.45, .55 * rubble))
    return sa.finish(sa.reverb(sa.highpass(x, 30), .7, .28, damp=4000), 'effect')


def debris(v, rng):
    """A clod of the island hitting home: a dull knock and a spill of grit."""
    knock = sa.norm(sa.bandpass(sa.thump(420, 200, .25, .05, drive=2.2, knock=.9), 260, 2400))
    grit = sa.norm(sa.bandpass(sa.noise(.45, rng), 700, 4200)) * sa.decay(.45, .12, .004)
    x = sa.mix(.8 * knock, (.01, .8 * grit))
    return sa.finish(sa.reverb(sa.highpass(x, 45), .4, .15, damp=5000), 'effect')


SERPENT_EVENTS = [event('aura_serpent_' + name, build, role='effect', subtitle='serpent.' + name) for name, build in (
    ('hiss', hiss), ('wake', wake), ('uncoil', uncoil), ('challenge', challenge), ('hurt', hurt), ('death', death),
    ('strike', strike), ('lunge', lunge), ('constrict', constrict), ('sidestep', sidestep), ('dive', dive), ('burst', burst),
    ('slam', slam), ('debris', debris))]
