"""Aura, the swordsman's path: a blade's own sounds, belonging to no element (each method's element is in what it strikes).

    aura_slash          a crescent of aura loosed off the blade: a ringing edge tearing through the air
    aura_guard          the guard braced: a low metallic hum settling round the blade
    aura_perfect_guard  the perfect moment: a bright ringing clash and a chime above it
    aura_breakthrough   a stage broken through: breath drawn in, then a rising chord blooming out
    aura_backlash       spent past empty: a dull falling exhale and a dry crackle (nothing hurts)
    aura_breath         the breathing stance: a soft breath with a hum under it (played at the pentatonic steps for the beat)
    aura_step           Aura Step: a rush of air drawn through in an instant, an edge riding it, a soft landing
    aura_armour         aura armour takes a blow: the close ring of a struck shell and a shimmer over it
    aura_intent         Intent takes hold: a low pressure in the air, more felt than heard
    aura_dominion       Dominion: a deep strike into the ground and a chord rising out of it as the circle opens
    aura_dominion_fade  a Dominion ending: its ring falling away down the scale, a breath let out
    aura_spellblade     a spell drawn into the blade: its breath pulled in, then the blade ringing as it takes it
    aura_string_tick    a swing landing on a sword string: a small bright knock of steel and a glassy ping (played up the scale as it grows)
    aura_string_complete a sword string played to its end: a swish, two rising glassy notes, a blade's ring and a shimmer
    aura_string_fumble  a sword string broken: a dull, muted clank and a short note sliding down off the scale
    aura_momentum_rise  a tier of momentum gained: two soft glassy notes stepping up and a breath of shimmer (played up the scale by tier)
    aura_momentum_peak  momentum at its peak: a bright chord climbing, a warm swell under it and a glitter over it
    aura_stance_break   a foe's stance giving way: a sharp crack, something brittle breaking, a deep blow under it
    aura_finisher       a finisher landing: a blade's ring cut off by a deep blow, the air torn, a long shimmering tail

Tonal sounds are tuned to D so the pentatonic ratios make a scale from one sample.

Each breathing method's own family (a blade's swing, a blow landing, a technique loosed: aura_<method>_swing, _impact, _art) is
in tools/feel/aura_methods.py and added to these events; each method's arts' own voices (aura_art_<art>) are in tools/feel/aura_arts.py,
and each method's finisher's (aura_finisher_<method>, over aura_finisher) in tools/feel/aura_finishers.py. Awakening's (the shared
aura_awaken, each method's aura_awaken_<method> over it, and the spent state's) are in tools/feel/aura_awakening.py. The Ways' (the
crossroads, each Way's voice as it's chosen, the incense and what the nodes do: aura_crossroads, aura_way_*) are in tools/feel/aura_ways.py.
"""
import numpy as np

from feel.core import sa, event
from feel.aura_methods import METHOD_EVENTS
from feel.aura_arts import ART_EVENTS
from feel.aura_finishers import FINISHER_EVENTS
from feel.aura_awakening import AWAKENING_EVENTS
from feel.aura_ways import WAY_EVENTS

D, E, FS, A, B = sa.D, sa.E, sa.FS, sa.A, sa.B


def _clean(x, role, verb=0.35, wet=0.1, damp=7000.0):
    return sa.finish(sa.highpass(sa.reverb(x, verb, wet, damp=damp), 70), role)


def aura_slash(v, rng):
    """A crescent loosed: a bright edge sliding down through the air, ringing as it goes."""
    dur = 0.55
    air = sa.moving_band(dur, [(0, 6500), (0.12, 2600), (dur, 900)], 0.55, rng) * sa.env(dur, (0, 0), (0.02, 1), (0.25, 0.45), (dur, 0))
    ring = sa.sine(sa.sweep(sa.note(A, 2), sa.note(D, 2), dur, 0.7), dur) * sa.decay(dur, 0.16, 0.004)
    edge = sa.glass(sa.note(D, 3) * (1.0 + 0.003 * v), dur, 0.09)
    whump = sa.thump(180, 90, 0.18, 0.04, drive=1.3)
    x = sa.mix(0.85 * sa.norm(air), 0.32 * ring, 0.22 * edge, 0.35 * whump)
    return _clean(sa.chorus(x, voices=2, depth=0.002, rate=0.6), "cast", 0.4, 0.12, 8000)


def aura_guard(v, rng):
    """The guard braced: a soft metallic swell, settling into a low hum round the blade."""
    dur = 0.6
    hum = (sa.sine(sa.note(D, 0), dur) + 0.5 * sa.sine(sa.note(A, 0), dur) + 0.25 * sa.sine(sa.note(D, 1), dur)) * sa.swell(dur, 0.12)
    metal = sa.partials(sa.note(D, 1), dur, ((1.0, 1.0, 1.0), (2.76, 0.4, 0.5), (5.4, 0.2, 0.3)), 0.12)
    brace = sa.moving_band(0.25, [(0, 1800), (0.25, 600)], 0.8, rng) * sa.decay(0.25, 0.06, 0.004)
    return _clean(sa.mix(0.55 * hum, 0.35 * metal, 0.25 * brace), "effect", 0.4, 0.1)


def aura_perfect_guard(v, rng):
    """The perfect moment: a hard bright clash of blade on blade, and a chime ringing out over it."""
    dur = 1.0
    clash = sa.norm(sa.bandpass(sa.noise(0.08, rng), 1800, 7500)) * sa.decay(0.08, 0.012, 0.0005)
    strike = sa.partials(sa.note(A, 1), dur, ((1.0, 1.0, 1.0), (2.41, 0.7, 0.6), (3.9, 0.45, 0.4), (6.2, 0.25, 0.25)), 0.2)
    chime = sa.mix(sa.glass(sa.note(D, 3), dur, 0.3), (0.05, 0.7 * sa.glass(sa.note(A, 3), dur * 0.9, 0.28)))
    return _clean(sa.mix(0.7 * clash, 0.45 * strike, 0.4 * chime), "impact", 0.6, 0.15, 9000)


def aura_breakthrough(v, rng):
    """A stage broken through: a breath drawn in, a held moment, then a chord blooming out and up."""
    dur = 2.2
    inhale = sa.moving_band(0.7, [(0, 500), (0.7, 2600)], 0.7, rng) * sa.swell(0.7, 0.68)
    root = sa.sine(sa.note(D, 0), dur) * sa.env(dur, (0, 0), (0.75, 0), (0.8, 1), (dur, 0))
    chord = sa.mix((0.78, sa.bell(sa.note(D, 1), 1.4, 0.6, 2.0, 1.1)), (0.84, 0.8 * sa.bell(sa.note(FS, 1), 1.3, 0.55, 2.0, 1.1)),
                   (0.9, 0.7 * sa.bell(sa.note(A, 1), 1.25, 0.5, 2.0, 1.1)), (1.0, 0.6 * sa.glass(sa.note(D, 3), 1.1, 0.4)))
    rise = sa.sparkle(1.3, 26, rng, [sa.note(d, 2) for d in (D, E, FS, A, B)], tau=(0.05, 0.14), shape=[(0, 0.2), (1.3, 1)], rising=True)
    whoosh = sa.moving_band(1.0, [(0, 600), (0.4, 3500), (1.0, 1500)], 0.6, rng) * sa.decay(1.0, 0.3, 0.02)
    x = sa.mix(0.45 * sa.norm(inhale), 0.3 * root, 0.55 * chord, (0.8, 0.3 * sa.norm(rise)), (0.78, 0.4 * whoosh))
    return _clean(x, "grand", 1.2, 0.22, 8000)


def aura_backlash(v, rng):
    """Spent past empty: a dull falling exhale, a dry crackle, a low note sinking (it never hurts)."""
    dur = 0.7
    exhale = sa.moving_band(dur, [(0, 1600), (dur, 300)], 0.9, rng) * sa.decay(dur, 0.25, 0.01)
    fall = sa.decay(0.35, 0.12, 0.002)
    crackle = sa.grains(0.35, 60, rng)[: len(fall)] * fall
    sink = sa.sine(sa.sweep(sa.note(A, 0), sa.note(D, -1), dur, 0.6), dur) * sa.decay(dur, 0.25, 0.01)
    return _clean(sa.mix(0.6 * sa.norm(exhale), 0.3 * sa.norm(crackle), 0.45 * sink), "effect", 0.3, 0.08, 4000)


def aura_breath(v, rng):
    """A breath of the stance: air drawn in softly, a warm hum under it at D. Played at the scale's steps for the beat."""
    dur = 1.0
    air = sa.moving_band(dur, [(0, 700), (0.45, 1700), (dur, 900)], 0.5, rng) * sa.env(dur, (0, 0), (0.35, 1), (dur, 0))
    hum = (sa.sine(sa.note(D, 1), dur) + 0.4 * sa.sine(sa.note(A, 1), dur)) * sa.env(dur, (0, 0), (0.4, 1), (dur, 0))
    glint = 0.2 * sa.glass(sa.note(D, 3), dur, 0.25)
    return _clean(sa.mix(0.55 * sa.norm(air), 0.4 * hum, (0.3, glint)), "ui", 0.5, 0.12, 6500)


def aura_step(v, rng):
    """Aura Step: a hard rush of air drawn through in an instant, a bright edge riding it, and a soft landing thud."""
    dur = 0.5
    gone = 0.16
    rush = sa.moving_band(dur, [(0, 800), (gone, 4200), (0.3, 1800), (dur, 600)], 0.7, rng) * sa.env(dur, (0, 0), (0.03, 0.7), (gone, 1), (0.32, 0.25), (dur, 0))
    edge = sa.sine(sa.sweep(sa.note(D, 2), sa.note(A, 2), gone, 1.4), gone) * sa.env(gone, (0, 0), (gone * 0.8, 1), (gone, 0))
    ring = sa.glass(sa.note((A, FS)[v], 2), 0.4, 0.1)
    land = sa.thump(160, 70, 0.2, 0.05, drive=1.2)
    x = sa.mix(0.9 * sa.norm(rush), 0.25 * edge, (gone, 0.2 * ring), (gone, 0.3 * land))
    return _clean(x, "cast", 0.35, 0.1, 5500)


def aura_armour(v, rng):
    """Aura armour takes a blow: a dull, close ring of a struck shell, a shimmer spreading over it."""
    dur = 0.45
    knock = sa.norm(sa.lowpass(sa.noise(0.06, rng), 2200)) * sa.decay(0.06, 0.012, 0.0005)
    shell = sa.partials(sa.note(D, 1), dur, ((1.0, 1.0, 1.0), (2.2, 0.5, 0.6), (3.7, 0.3, 0.35)), 0.12)
    shimmer = sa.sparkle(0.35, 22, rng, [sa.note(d, 3) for d in (D, FS, A)], tau=(0.03, 0.08), shape=[(0, 1), (0.35, 0)])
    return _clean(sa.mix(0.5 * knock, 0.45 * shell, (0.02, 0.18 * sa.norm(shimmer))), "effect", 0.3, 0.08, 7000)


def aura_intent(v, rng):
    """Intent takes hold: a low pressure in the air, a slow throb under hearing more felt than heard."""
    dur = 1.4
    throb = (sa.sine(sa.note(D, 0), dur) + 0.5 * sa.sine(sa.note(D, 0) * 1.006, dur) + 0.4 * sa.sine(sa.note(A, 0), dur))
    throb = throb * sa.env(dur, (0, 0), (0.35, 1), (0.8, 0.6), (dur, 0))
    weight = sa.moving_band(dur, [(0, 350), (0.5, 900), (dur, 400)], 0.9, rng) * sa.env(dur, (0, 0), (0.4, 1), (dur, 0))
    return _clean(sa.mix(0.6 * sa.saturate(throb, 1.6), 0.4 * sa.norm(weight)), "effect", 0.6, 0.12, 3500)


def aura_dominion(v, rng):
    """Dominion: a deep strike into the ground, a chord rising out of it, and a long ringing as the circle opens."""
    dur = 3.4
    hit = 0.18
    draw = sa.moving_band(hit, [(0, 400), (hit, 3000)], 0.8, rng) * sa.env(hit, (0, 0), (hit * 0.9, 1), (hit, 0)) ** 2
    boom = sa.thump(90, 34, 1.1, 0.32, 2.2)
    drone = (sa.sine(sa.note(D, -2), dur) + 0.6 * sa.sine(sa.note(A, -2), dur) + 0.3 * sa.sine(sa.note(D, -1), dur))
    drone = sa.saturate(drone, 1.6) * sa.env(dur, (0, 0), (hit, 1), (1.2, 0.6), (dur, 0))
    chord = sa.mix(*[(hit + 0.05 * i, (1 - 0.12 * i) * sa.bell(sa.note(*n), 2.6, 1.3, 2.0, 1.8, attack=0.006))
                     for i, n in enumerate(((D, 0), (A, 0), (D, 1), (FS, 1), (A, 1)))])
    ring = sa.glass(sa.note(D, 3), 2.4, 0.9)
    x = sa.mix(0.35 * sa.norm(draw), (hit, 0.75 * boom), 0.3 * drone, 0.55 * sa.norm(chord), (hit + 0.3, 0.2 * ring))
    return _clean(x, "grand", 2.6, 0.32, 6000)


def aura_dominion_fade(v, rng):
    """A Dominion ending: the circle's ring falling away down the scale, and a breath let out."""
    steps = ((A, 2), (FS, 2), (D, 2), (A, 1), (FS, 1), (D, 1))
    fall = sa.mix(*[(0.06 * i, (0.7 - 0.07 * i) * sa.glass(sa.note(*n), 0.6, 0.22)) for i, n in enumerate(steps)])
    exhale = sa.moving_band(0.9, [(0, 1800), (0.9, 400)], 0.9, rng) * sa.decay(0.9, 0.35, 0.02)
    return _clean(sa.mix(0.55 * fall, 0.35 * sa.norm(exhale)), "effect", 1.0, 0.25, 6000)


def aura_spellblade(v, rng):
    """A spell drawn into the blade: the spell's breath pulled in, then a bright metallic ring as the blade takes it."""
    dur = 0.9
    pull = sa.reverse(sa.moving_band(0.3, [(0, 4500), (0.3, 900)], 0.7, rng) * sa.decay(0.3, 0.1, 0.004))
    ring = sa.partials(sa.note(A, 1), dur, ((1.0, 1.0, 1.0), (2.41, 0.6, 0.55), (3.9, 0.35, 0.35), (5.6, 0.2, 0.25)), 0.25)
    chime = sa.glass(sa.note(D, 3), dur, 0.3)
    x = sa.mix(0.55 * sa.norm(pull), (0.28, 0.45 * ring), (0.3, 0.3 * chime))
    return _clean(x, "effect", 0.6, 0.14, 9000)


def aura_string_tick(v, rng):
    """A swing landing on a sword string: a small bright knock of steel on steel, a short glassy ping at A over it. Played
    climbing the scale (Feels.step) as the string grows, so a string sounds like a rising run; heard often, so it's soft."""
    dur = 0.22
    knock = sa.tick(sa.note(A, 2), rng, 0.8)
    ping = sa.glass(sa.note(A, 2), dur, 0.05)
    air = sa.norm(sa.bandpass(sa.noise(0.05, rng), 3000, 8000)) * sa.decay(0.05, 0.008, 0.0005)
    x = sa.mix(0.5 * sa.norm(knock), 0.45 * ping, 0.12 * air)
    return _clean(x, "ui", 0.18, 0.06, 9500)


def aura_string_complete(v, rng):
    """A sword string played to its end: a quick swish, two glassy notes rising (A, then the D above), a blade's ring under
    them and a shimmer as they settle. Distinct from the ticks, short enough to sit under the art's own sound."""
    dur = 0.9
    swish = sa.moving_band(0.22, [(0, 1500), (0.22, 5500)], 0.7, rng) * sa.swell(0.22, 0.18)
    first = sa.glass(sa.note(A, 2), 0.45, 0.1)
    second = sa.glass(sa.note(D, 3), dur - 0.08, 0.22)
    ring = sa.partials(sa.note(D, 1), dur - 0.08, ((1.0, 1.0, 1.0), (2.41, 0.6, 0.55), (3.9, 0.35, 0.35)), 0.25)
    shimmer = sa.sparkle(0.5, 30, rng, [sa.note(d, 3) for d in (D, FS, A)], tau=(0.03, 0.08), shape=[(0, 1), (0.5, 0)])
    x = sa.mix(0.3 * sa.norm(swish), (0.02, 0.5 * first), (0.08, 0.65 * second), (0.08, 0.3 * ring), (0.12, 0.16 * sa.norm(shimmer)))
    return _clean(x, "effect", 0.5, 0.12, 9500)


def aura_string_fumble(v, rng):
    """A sword string broken: a dull, muted clank of steel, a flat body under it, and a short note sliding down off the scale."""
    dur = 0.42
    clank = sa.norm(sa.lowpass(sa.noise(0.05, rng), 1800)) * sa.decay(0.05, 0.01, 0.0005)
    body = sa.partials(sa.note(A, 0) * 1.03, dur, ((1.0, 1.0, 1.0), (2.2, 0.5, 0.5), (3.1, 0.3, 0.3)), 0.07)
    slide = sa.sine(sa.sweep(sa.note(D, 1) * 1.04, sa.note(A, 0) * 0.97, dur, 0.8), dur) * sa.decay(dur, 0.12, 0.003)
    x = sa.mix(0.5 * clank, 0.4 * body, 0.35 * slide)
    return _clean(x, "ui", 0.2, 0.06, 5000)


def aura_momentum_rise(v, rng):
    """A tier of momentum gained: two soft glassy notes stepping up (D, then A), a breath of air and a shimmer. Played up the
    scale (Feels.step) with each tier, quiet enough to sit under a fight."""
    dur = 0.6
    first = sa.glass(sa.note(D, 2), 0.35, 0.09)
    second = sa.glass(sa.note(A, 2), 0.5, 0.16)
    air = sa.moving_band(0.3, [(0, 1200), (0.3, 4200)], 0.6, rng) * sa.swell(0.3, 0.25)
    shimmer = sa.sparkle(0.35, 20, rng, [sa.note(d, 3) for d in (D, FS, A)], tau=(0.02, 0.06), shape=[(0, 0.6), (0.35, 0)], rising=True)
    x = sa.mix(0.5 * first, (0.07, 0.6 * second), 0.2 * sa.norm(air), (0.08, 0.15 * sa.norm(shimmer)))
    return _clean(x, "ui", 0.45, 0.12, 9500)


def aura_momentum_peak(v, rng):
    """Momentum at its peak: a bright chord climbing (D, F#, A, D), a warm swell rising under it and glitter over the top."""
    dur = 1.6
    chord = sa.mix(*[(0.06 * i, (1 - 0.12 * i) * sa.bell(sa.note(*n), 1.2, 0.5, 2.0, 1.6, attack=0.01))
                     for i, n in enumerate(((D, 1), (FS, 1), (A, 1), (D, 2)))])
    swell = (sa.sine(sa.note(D, 0), dur) + 0.5 * sa.sine(sa.note(A, 0), dur)) * sa.env(dur, (0, 0), (0.25, 1), (dur, 0))
    rush = sa.moving_band(0.45, [(0, 800), (0.45, 5000)], 0.6, rng) * sa.swell(0.45, 0.4)
    glitter = sa.sparkle(1.0, 32, rng, [sa.note(d, 3) for d in (D, E, FS, A, B)], tau=(0.03, 0.1), shape=[(0, 1), (1.0, 0.1)], rising=True)
    x = sa.mix(0.6 * sa.norm(sa.chorus(chord, voices=2)), 0.25 * swell, 0.25 * sa.norm(rush), (0.1, 0.25 * sa.norm(glitter)))
    return _clean(x, "effect", 0.8, 0.18, 9500)


def aura_stance_break(v, rng):
    """A foe's stance giving way: a sharp crack like a guard splitting, something brittle breaking apart, a deep blow under it."""
    dur = 0.95
    crack = sa.norm(sa.bandpass(sa.noise(0.07, rng), 1400, 9000)) * sa.decay(0.07, 0.008, 0.0003)
    brittle = sa.norm(sa.bandpass(sa.grains(0.45, 260, rng, length=(0.002, 0.008), shape=[(0, 1), (0.45, 0.05)]), 1800, 7500))
    body = sa.partials(sa.note(A, 0) * 1.02, 0.6, ((1.0, 1.0, 1.0), (2.3, 0.6, 0.5), (3.7, 0.35, 0.35)), 0.12)
    blow = sa.thump(85, 32, 0.6, 0.15, drive=2.2, knock=0.4)
    slide = sa.sine(sa.sweep(sa.note(D, 1), sa.note(A, -1), 0.5, 0.7), 0.5) * sa.decay(0.5, 0.18, 0.003)
    x = sa.mix(0.85 * crack, (0.01, 0.55 * brittle), 0.3 * body, 0.6 * sa.highpass(blow, 45), (0.04, 0.25 * slide))
    return _clean(x, "impact", 0.5, 0.13, 8000)


def aura_finisher(v, rng):
    """A finisher landing: a blade's ring cut off by a deep blow, the air torn round it, and a long shimmering ring after."""
    dur = 1.9
    shing = sa.moving_band(0.18, [(0, 3000), (0.05, 8000), (0.18, 2500)], 0.5, rng) * sa.decay(0.18, 0.05, 0.002)
    blow = sa.thump(72, 26, 1.1, 0.28, drive=2.6, knock=0.5)
    tear = sa.norm(sa.bandpass(sa.noise(0.4, rng), 600, 5000)) * sa.decay(0.4, 0.09, 0.002)
    ring = sa.partials(sa.note(D, 1), 1.6, ((1.0, 1.0, 1.0), (2.41, 0.6, 0.55), (3.9, 0.35, 0.35), (5.6, 0.2, 0.25)), 0.55)
    tail = sa.mix(sa.glass(sa.note(D, 3), 1.5, 0.6), (0.04, 0.6 * sa.glass(sa.note(A, 3), 1.4, 0.55)))
    x = sa.mix(0.6 * sa.norm(shing), (0.02, 0.75 * sa.highpass(blow, 32)), (0.02, 0.5 * tear), (0.03, 0.3 * ring), (0.05, 0.25 * tail))
    return _clean(x, "grand", 1.1, 0.2, 8000)


EVENTS = [
    event("aura_slash", aura_slash, variants=2, role="cast", subtitle="cast"),
    event("aura_guard", aura_guard, role="effect", subtitle="tell"),
    event("aura_perfect_guard", aura_perfect_guard, role="impact", subtitle="hit", attenuation=24),
    event("aura_breakthrough", aura_breakthrough, role="grand", subtitle="field", attenuation=32),
    event("aura_backlash", aura_backlash, role="effect", subtitle="tell"),
    event("aura_breath", aura_breath, role="ui", subtitle="tell"),
    event("aura_step", aura_step, variants=2, role="cast", subtitle="cast"),
    event("aura_armour", aura_armour, role="effect", subtitle="hit"),
    event("aura_intent", aura_intent, role="effect", subtitle="tell"),
    event("aura_dominion", aura_dominion, role="grand", subtitle="field", attenuation=40),
    event("aura_dominion_fade", aura_dominion_fade, role="effect", subtitle="field", attenuation=24),
    event("aura_spellblade", aura_spellblade, role="effect", subtitle="cast"),
    event("aura_string_tick", aura_string_tick, role="ui", subtitle="tell"),
    event("aura_string_complete", aura_string_complete, role="effect", subtitle="cast"),
    event("aura_string_fumble", aura_string_fumble, role="ui", subtitle="tell"),
    event("aura_momentum_rise", aura_momentum_rise, role="ui", subtitle="tell"),
    event("aura_momentum_peak", aura_momentum_peak, role="effect", subtitle="tell", attenuation=24),
    event("aura_stance_break", aura_stance_break, role="impact", subtitle="hit", attenuation=32),
    event("aura_finisher", aura_finisher, role="grand", subtitle="hit", attenuation=40),
] + METHOD_EVENTS + ART_EVENTS + FINISHER_EVENTS + AWAKENING_EVENTS + WAY_EVENTS
