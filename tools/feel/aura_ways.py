"""Ways' sounds: the crossroads where a swordsman chooses their Way, each Way's voice as it's chosen, the incense that unbinds one, and
what the nodes do. Not a part of its own: tools/feel/aura.py adds these to its events (they live in the aura folder).

    aura_crossroads      the crossroads rising: a low drone opening, four notes rising one by one (a Way each), wind through them
    aura_way_lean        a standard answering a strike: one resonant note and a shimmer after it (played up the scale by standard)
    aura_way_chosen      a Way walked: a deep blow, a chord swelling open over it, light rising
    aura_way_blade       the Blade's voice: a blade drawn and ringing long, the air cut clean
    aura_way_bulwark     the Bulwark's voice: a great shield struck, a gong's hum, a heavy footfall
    aura_way_shadowstep  the Shadowstep's voice: a rush of air drawn in backwards, a whisper, a hollow note
    aura_way_banner      the Banner's voice: a horn swelling, two drumbeats, a glitter of gold
    aura_way_unbound     a Way burned away by the incense: a smoky exhale, a note cracking and falling
    aura_way_slip        a perfect guard slipping behind its striker: a sharp rush past and a soft landing
    aura_way_afterimage  an afterimage striking: a hollow cut echoing where nobody stands now
    aura_way_cascade     a finisher cascading to the next foe: two cracks and a ping leaping up
    aura_way_reflect     a held guard throwing a blow (or a shot) back: a metallic rebound
    aura_way_cry         a rallying cry: a short horn call, a drum and a bright chord
    aura_way_bastion     a shot turned back at a bastion's edge: a dull glassy thunk

Everything tonal is in D pentatonic like the rest of the mod.
"""
import numpy as np

from feel.core import sa, event

D, E, FS, A, B = sa.D, sa.E, sa.FS, sa.A, sa.B


def _clean(x, role, verb=0.6, wet=0.14, damp=7500.0):
    return sa.finish(sa.highpass(sa.reverb(x, verb, wet, damp=damp), 50), role)


def _echo(x, gap, decay, count):
    """A dry copy and fading repeats of it, {@code gap} seconds apart."""
    return sa.mix(x, *[(gap * (k + 1), (decay ** (k + 1)) * x) for k in range(count)])


# ================================================================ the crossroads

def aura_crossroads(v, rng):
    """The crossroads rising: a low drone opening up, four notes rising one by one (D, E, F sharp, A: a Way each), wind through them
    and a shimmer over the last."""
    dur = 2.6
    drone = (sa.sine(sa.note(D, -1), 2.4) + 0.5 * sa.sine(sa.note(A, -1), 2.4)) * sa.env(2.4, (0, 0), (0.4, 1), (2.4, 0))
    notes = sa.mix(*[(0.25 + 0.28 * k, (0.8 - 0.05 * k) * sa.bell(sa.note(n, 1), 1.6, 0.8, 2.0, 1.3, attack=0.03))
                     for k, n in enumerate((D, E, FS, A))])
    wind = sa.moving_band(2.2, [(0, 600), (0.8, 2400), (2.2, 900)], 0.5, rng) * sa.env(2.2, (0, 0), (0.5, 1), (2.2, 0))
    shimmer = sa.sparkle(1.2, 24, rng, [sa.note(d, 3) for d in (D, E, FS, A, B)], tau=(0.04, 0.12), shape=[(0, 0.4), (0.3, 1), (1.2, 0.05)],
                         rising=True)
    x = sa.mix(0.3 * drone, 0.55 * sa.norm(sa.chorus(notes, voices=2)), 0.25 * sa.norm(wind), (1.1, 0.2 * sa.norm(shimmer)))
    return _clean(x, "grand", 1.3, 0.22, 8000)


def aura_way_lean(v, rng):
    """A standard answering: one resonant note, a glassy edge on it and a shimmer after."""
    dur = 1.1
    note = sa.bell(sa.note(D, 2), 1.0, 0.85, 2.0, 1.2, attack=0.01)
    edge = sa.glass(sa.note(A, 3), 0.6, 0.15)
    shimmer = sa.sparkle(0.7, 18, rng, [sa.note(d, 3) for d in (D, FS, A)], tau=(0.03, 0.09), shape=[(0, 1), (0.7, 0)])
    x = sa.mix(0.6 * sa.norm(note), (0.01, 0.25 * edge), (0.08, 0.2 * sa.norm(shimmer)))
    return _clean(x, "effect", 0.7, 0.14, 9500)


def aura_way_chosen(v, rng):
    """A Way walked: a deep blow, a chord swelling open, light rising through it."""
    dur = 2.6
    blow = sa.thump(72, 30, 1.0, 0.26, drive=2.2, knock=0.4)
    chord = sa.mix(*[(0.03 * i, (1 - 0.1 * i) * sa.bell(sa.note(*n), 2.0, 0.85, 2.0, 1.4, attack=0.05))
                     for i, n in enumerate(((D, 0), (A, 0), (D, 1), (FS, 1), (A, 1), (D, 2)))])
    rise = sa.moving_band(1.4, [(0, 800), (1.4, 6000)], 0.6, rng) * sa.env(1.4, (0, 0), (0.6, 1), (1.4, 0))
    glitter = sa.sparkle(1.6, 32, rng, [sa.note(d, 3) for d in (D, E, FS, A, B)], tau=(0.04, 0.14), shape=[(0, 0.3), (0.5, 1), (1.6, 0.05)],
                         rising=True)
    x = sa.mix(0.6 * sa.highpass(blow, 30), (0.02, 0.6 * sa.norm(sa.chorus(chord, voices=2))), (0.1, 0.3 * sa.norm(rise)),
               (0.3, 0.25 * sa.norm(glitter)))
    return _clean(x, "grand", 1.4, 0.22, 8000)


# ================================================================ each Way's voice

def aura_way_blade(v, rng):
    """The Blade: a blade drawn and ringing long, the air cut clean in front of it."""
    dur = 2.0
    draw = sa.moving_band(0.4, [(0, 2200), (0.25, 7000), (0.4, 4000)], 0.6, rng) * sa.env(0.4, (0, 0), (0.08, 1), (0.4, 0))
    ring = sa.partials(sa.note(D, 1), 1.8, ((1.0, 1.0, 1.0), (2.41, 0.6, 0.55), (3.9, 0.35, 0.35), (5.6, 0.2, 0.25)), 0.7)
    cut = sa.moving_band(0.5, [(0, 7000), (0.12, 2600), (0.5, 900)], 0.55, rng) * sa.env(0.5, (0, 0), (0.02, 1), (0.25, 0.4), (0.5, 0))
    edge = sa.glass(sa.note(A, 3), 1.2, 0.35)
    x = sa.mix(0.45 * sa.norm(draw), (0.15, 0.45 * ring), (0.45, 0.55 * sa.norm(cut)), (0.18, 0.2 * edge))
    return _clean(x, "effect", 0.9, 0.16, 8500)


def aura_way_bulwark(v, rng):
    """The Bulwark: a great shield struck, a gong's hum after it, a heavy step."""
    dur = 2.2
    strike = sa.thump(120, 60, 0.5, 0.1, drive=2.0, knock=0.8)
    gong = sa.partials(sa.note(D, 0), 2.0, ((1.0, 1.0, 1.0), (1.48, 0.8, 0.8), (2.1, 0.6, 0.6), (2.9, 0.45, 0.4), (4.2, 0.3, 0.3)), 0.9)
    shimmer = sa.partials(sa.note(A, 1), 1.4, ((1.0, 0.7, 0.8), (2.76, 0.4, 0.5)), 0.5)
    step = sa.thump(80, 44, 0.4, 0.12, drive=1.8, knock=0.4)
    x = sa.mix(0.45 * sa.highpass(strike, 70), (0.01, 0.6 * sa.norm(gong)), (0.02, 0.35 * shimmer), (0.55, 0.3 * sa.highpass(step, 60)))
    return _clean(x, "effect", 1.0, 0.18, 6500)


def aura_way_shadowstep(v, rng):
    """The Shadowstep: a rush of air drawn in backwards, a whisper sliding past, a hollow note left behind."""
    dur = 1.8
    rush = sa.reverse(sa.moving_band(0.7, [(0, 6000), (0.7, 900)], 0.7, rng) * sa.decay(0.7, 0.2, 0.004))
    whisper = sa.norm(sa.bandpass(sa.noise(0.8, rng), 1800, 5200)) * sa.env(0.8, (0, 0), (0.2, 1), (0.8, 0))
    hollow = (sa.sine(sa.note(D, 0), 1.2) + 0.4 * sa.sine(sa.note(A, 0) * 1.003, 1.2)) * sa.env(1.2, (0, 0), (0.1, 1), (1.2, 0))
    x = sa.mix(0.55 * sa.norm(rush), (0.6, 0.25 * whisper), (0.68, 0.35 * hollow))
    return _clean(x, "effect", 1.0, 0.2, 6000)


def aura_way_banner(v, rng):
    """The Banner: a horn swelling, two drumbeats under it, a glitter of gold."""
    dur = 2.0
    horn = sa.lowpass(sa.soft_saw(sa.glide(1.4, (0, sa.note(A, 0)), (0.25, sa.note(D, 1)), (1.4, sa.note(D, 1))), harmonics=7), 2200)
    horn = horn * sa.env(1.4, (0, 0), (0.25, 1), (1.0, 0.8), (1.4, 0))
    fifth = sa.lowpass(sa.soft_saw(sa.glide(1.2, (0, sa.note(A, 1)), (1.2, sa.note(A, 1))), harmonics=5), 2000) * sa.env(1.2, (0, 0), (0.35, 0.6), (1.2, 0))
    drums = sa.mix(sa.thump(80, 40, 0.4, 0.1, drive=2.0, knock=0.3), (0.32, sa.thump(80, 40, 0.5, 0.12, drive=2.0, knock=0.3)))
    glitter = sa.sparkle(1.0, 26, rng, [sa.note(d, 3) for d in (D, E, FS, A, B)], tau=(0.03, 0.1), shape=[(0, 0.2), (0.3, 1), (1.0, 0)])
    x = sa.mix(0.5 * sa.norm(horn), (0.25, 0.25 * sa.norm(fifth)), 0.45 * sa.highpass(drums, 35), (0.5, 0.2 * sa.norm(glitter)))
    return _clean(x, "effect", 0.9, 0.18, 7000)


def aura_way_unbound(v, rng):
    """A Way burned away: a smoky exhale, its note cracking and falling away down the scale."""
    dur = 2.0
    smoke = sa.norm(sa.lowpass(sa.noise(1.4, rng), 1400)) * sa.env(1.4, (0, 0), (0.2, 1), (1.4, 0))
    falling = sa.sine(sa.glide(1.4, (0, sa.note(A, 1)), (1.4, sa.note(D, 0))), 1.4) * sa.env(1.4, (0, 0), (0.05, 1), (1.4, 0))
    crack = sa.shatter(rng, count=18, band=(2000, 7000), spread=0.25, tau=(0.03, 0.1))
    x = sa.mix(0.4 * smoke, (0.1, 0.4 * falling), (0.12, 0.35 * sa.norm(crack)))
    return _clean(x, "effect", 0.9, 0.18, 6000)


# ================================================================ what the nodes do

def aura_way_slip(v, rng):
    """Slipping behind a striker: a sharp rush of air past, and a soft landing."""
    dur = 0.6
    rush = sa.moving_band(0.3, [(0, 1000), (0.15, 4200), (0.3, 2200)], 0.6, rng) * sa.env(0.3, (0, 0), (0.05, 1), (0.3, 0))
    land = sa.thump(140, 70, 0.2, 0.05, drive=1.2)
    x = sa.mix(0.7 * sa.lowpass(sa.norm(rush), 6500), (0.22, 0.35 * sa.highpass(land, 70)))
    return _clean(x, "cast", 0.4, 0.1, 6500)


def aura_way_afterimage(v, rng):
    """An afterimage striking: a hollow cut, echoing where nobody stands now."""
    dur = 1.0
    cut = sa.moving_band(0.3, [(0, 5000), (0.1, 2200), (0.3, 800)], 0.55, rng) * sa.env(0.3, (0, 0), (0.02, 1), (0.3, 0))
    ring = sa.glass(sa.note(FS, 2) * (1 + 0.004 * v), 0.4, 0.12)
    x = _echo(sa.mix(0.7 * sa.norm(cut), 0.25 * ring), 0.11, 0.45, 3)
    return _clean(x, "effect", 0.8, 0.2, 6500)


def aura_way_cascade(v, rng):
    """A finisher cascading on: two cracks, and a ping leaping up to the next foe."""
    dur = 0.8
    crack = sa.norm(sa.bandpass(sa.noise(0.1, rng), 1500, 9000)) * sa.decay(0.1, 0.02, 0.0005)
    leap = sa.sine(sa.sweep(sa.note(D, 2), sa.note(A, 3), 0.5, 0.6), 0.5) * sa.decay(0.5, 0.12, 0.004)
    ping = sa.glass(sa.note(A, 3), 0.5, 0.14)
    x = sa.mix(0.6 * crack, (0.12, 0.5 * crack), (0.05, 0.3 * leap), (0.16, 0.35 * ping))
    return _clean(x, "impact", 0.6, 0.14, 9000)


def aura_way_reflect(v, rng):
    """A held guard throwing a blow back: a short metallic rebound."""
    dur = 0.6
    clang = sa.partials(sa.note(A, 1), 0.5, ((1.0, 1.0, 1.0), (2.76, 0.5, 0.5), (5.4, 0.25, 0.3)), 0.1)
    knock = sa.thump(170, 90, 0.15, 0.04, drive=1.4)
    x = sa.mix(0.55 * sa.norm(clang), 0.4 * sa.highpass(knock, 80))
    return _clean(x, "impact", 0.5, 0.12, 9000)


def aura_way_cry(v, rng):
    """A rallying cry: a short horn call stepping up, a drum and a bright chord."""
    dur = 1.4
    call = sa.lowpass(sa.soft_saw(sa.glide(0.8, (0, sa.note(D, 1)), (0.12, sa.note(A, 1)), (0.8, sa.note(A, 1))), harmonics=7), 2600)
    call = call * sa.env(0.8, (0, 0), (0.06, 1), (0.6, 0.7), (0.8, 0))
    drum = sa.thump(90, 44, 0.35, 0.09, drive=2.0, knock=0.35)
    chord = sa.mix(*[(0.02 * i, sa.bell(sa.note(*n), 0.9, 0.8, 2.0, 1.2, attack=0.02)) for i, n in enumerate(((D, 1), (FS, 1), (A, 1)))])
    x = sa.mix(0.5 * sa.norm(call), 0.4 * sa.highpass(drum, 40), (0.15, 0.3 * sa.norm(chord)))
    return _clean(x, "effect", 0.7, 0.16, 8000)


def aura_way_bastion(v, rng):
    """A shot turned back at a bastion's edge: a dull glassy thunk."""
    dur = 0.5
    thunk = sa.thump(120, 60, 0.25, 0.06, drive=1.6, knock=0.3)
    glass = sa.glass(sa.note(D, 2) * (1 + 0.01 * v), 0.35, 0.08)
    puff = sa.norm(sa.bandpass(sa.noise(0.12, rng), 800, 3500)) * sa.decay(0.12, 0.03, 0.001)
    x = sa.mix(0.5 * sa.highpass(thunk, 60), 0.3 * glass, 0.25 * puff)
    return _clean(x, "effect", 0.5, 0.12, 8000)


WAY_EVENTS = [
    event("aura_crossroads", aura_crossroads, role="grand", subtitle="field", attenuation=32),
    event("aura_way_lean", aura_way_lean, role="effect", subtitle="tell", attenuation=24),
    event("aura_way_chosen", aura_way_chosen, role="grand", subtitle="field", attenuation=40),
    event("aura_way_blade", aura_way_blade, role="effect", subtitle="cast", attenuation=32),
    event("aura_way_bulwark", aura_way_bulwark, role="effect", subtitle="cast", attenuation=32),
    event("aura_way_shadowstep", aura_way_shadowstep, role="effect", subtitle="cast", attenuation=32),
    event("aura_way_banner", aura_way_banner, role="effect", subtitle="cast", attenuation=32),
    event("aura_way_unbound", aura_way_unbound, role="effect", subtitle="tell", attenuation=24),
    event("aura_way_slip", aura_way_slip, variants=2, role="cast", subtitle="cast"),
    event("aura_way_afterimage", aura_way_afterimage, variants=2, role="effect", subtitle="hit", attenuation=24),
    event("aura_way_cascade", aura_way_cascade, role="impact", subtitle="hit", attenuation=24),
    event("aura_way_reflect", aura_way_reflect, variants=2, role="impact", subtitle="hit"),
    event("aura_way_cry", aura_way_cry, role="effect", subtitle="tell", attenuation=32),
    event("aura_way_bastion", aura_way_bastion, variants=2, role="effect", subtitle="hit"),
]
