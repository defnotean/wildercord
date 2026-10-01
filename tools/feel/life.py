"""Life's kit sounds: one voice per rune, all on one instrument family (soft marimba and bell, leaf noise, wood taps).

Names start with 'life_'. Every tonal part sits in D major pentatonic (see tools/feel/fire.py for the pattern to copy).
"""
import numpy as np

from feel.core import sa, event

D, E, FS, A, B = sa.D, sa.E, sa.FS, sa.A, sa.B


def marimba(freq, dur, tau=0.12):
    """A soft wooden note: a sine, a quickly dying fourth-octave partial and a little knock."""
    return sa.sine(freq, dur) * sa.decay(dur, tau, 0.003) + 0.25 * sa.sine(freq * 4, dur) * sa.decay(dur, tau * 0.25, 0.002)


def leaves(dur, lo, hi, rng, peak=0.5, level=1.0):
    """Leaf rustle: moving band noise with a swell."""
    return level * sa.moving_band(dur, [(0, lo), (dur * peak, hi), (dur, lo)], 0.9, rng) * sa.swell(dur, dur * peak, 1.5)


def run(notes, step, dur, tau, octave=1, kind="marimba", first=1.0, fall=0.85):
    """Notes one after another, `step` seconds apart, each a little softer."""
    layers = []
    for i, n in enumerate(notes):
        f = sa.note(n, octave)
        voice = marimba(f, dur, tau) if kind == "marimba" else sa.bell(f, dur, tau, 2.0, 1.0, 0.01)
        layers.append((i * step, first * fall ** i * voice))
    return sa.mix(*layers)


def life_heal(v, rng):
    """Heal: a rising third of two warm bells, a glint, a breath of air."""
    dur = 0.9
    a = sa.bell(sa.note((D, E)[v], 1), dur, 0.4, 2.0, 0.9, 0.02)
    b = sa.bell(sa.note((FS, A)[v], 1), dur, 0.45, 2.0, 0.9, 0.02)
    air = leaves(dur, 2500, 5000, rng, 0.3, 0.12)
    glint = sa.glass(sa.note(D, 3), 0.4, 0.09) * 0.25
    return sa.finish(sa.reverb(sa.mix(a, (0.12, 0.9 * b), (0.2, glint), air), 0.8, 0.2), "effect")


def life_regrow(v, rng):
    """Regrowth: leaves swelling, and a marimba tick at each stage of the ramp (D, F#, A)."""
    dur = 1.0
    ticks = run([D, FS, A], 0.3, 0.35, 0.1)
    return sa.finish(sa.reverb(sa.mix(leaves(dur, 2200, 4800, rng, 0.6, 0.35), (0.05, ticks)), 0.6, 0.15), "effect")


def life_mend(v, rng):
    """Restore: two hollow wood taps, a little anvil ring, and a warm chime."""
    tap = lambda f: sa.mix(sa.sine(f, 0.08) * sa.decay(0.08, 0.012, 0.0005), 0.3 * sa.norm(sa.bandpass(sa.noise(0.04, rng), 900, 3000)) * sa.decay(0.04, 0.006, 0.0003))
    ring = sa.glass(sa.note(A, 2), 0.5, 0.1) * 0.4
    chime = sa.bell(sa.note(D, 2), 0.7, 0.3, 2.0, 0.8, 0.005)
    return sa.finish(sa.reverb(sa.mix(tap(300), (0.11, tap(360)), (0.2, ring), (0.24, 0.6 * chime)), 0.5, 0.12), "effect")


def life_purge(v, rng):
    """Cleanse: water poured (a falling band of noise) resolving into a clear bell."""
    dur = 0.8
    pour = sa.moving_band(dur, [(0, 5200), (dur * 0.6, 2400), (dur, 1500)], 0.8, rng) * sa.decay(dur, 0.25, 0.01)
    bell = sa.bell(sa.note(A, 1), 0.6, 0.25, 2.0, 0.7, 0.01)
    return sa.finish(sa.reverb(sa.mix(0.4 * pour, (0.35, 0.8 * bell)), 0.6, 0.15), "effect")


def life_transmute(v, rng):
    """Remedy: the grey falling of a purge, then an upward glide that turns to a chime (bane into boon)."""
    dur = 1.0
    fall = sa.moving_band(0.4, [(0, 4200), (0.4, 1600)], 0.8, rng) * sa.decay(0.4, 0.15, 0.01)
    rise = sa.sine(sa.glide(0.5, (0, sa.note(D, 1)), (0.5, sa.note(A, 1))), 0.5) * sa.swell(0.5, 0.3)
    chime = sa.bell(sa.note(A, 2), 0.6, 0.3, 2.0, 0.8, 0.005)
    return sa.finish(sa.reverb(sa.mix(0.35 * fall, (0.3, 0.4 * rise), (0.65, 0.7 * chime)), 0.7, 0.2), "effect")


def life_grow(v, rng):
    """Grow: a quick run of soft bloops going up (three variants: bright, bright and quicker, glassy and quiet)."""
    notes = [[D, FS, A, D], [E, A, B, E], [A, D, FS, A]][v]
    step = (0.07, 0.05, 0.09)[v]
    oct_ = (1, 2, 2)[v]
    bloops = sa.mix(*[(i * step, sa.sine(sa.glide(0.12, (0, sa.note(n, oct_) * 0.8), (0.12, sa.note(n, oct_))), 0.12) * sa.decay(0.12, 0.05, 0.004)) for i, n in enumerate(notes)])
    creak = 0.2 * sa.norm(sa.bandpass(sa.noise(0.3, rng), 300, 900)) * sa.decay(0.3, 0.1, 0.01)
    return sa.finish(sa.reverb(sa.mix(bloops, creak), 0.4, 0.1), "effect")


def life_glimmer(v, rng):
    """Glimmer: a quiet glassy run for a wall lighting up."""
    x = sa.mix(*[(i * 0.06, 0.6 * sa.glass(sa.note(n, 2), 0.4, 0.08)) for i, n in enumerate((D, FS, A, B))])
    return sa.finish(sa.reverb(x, 0.6, 0.2), "effect")


def life_reap(v, rng):
    """Harvest: a dry scythe swish and three rustle ticks."""
    dur = 0.5
    swish = sa.moving_band(0.25, [(0, 3200), (0.25, 1500)], 0.8, rng) * sa.decay(0.25, 0.08, 0.005)
    ticks = sa.norm(sa.bandpass(sa.grains(0.3, 25, rng, shape=[(0, 1), (0.3, 0.2)]), 2000, 6000)) * 0.5
    return sa.finish(sa.mix(0.7 * swish, (0.15, ticks)), "effect")


def life_plip(v, rng):
    """Glowvine: descending amber plips, one per vine."""
    x = sa.mix(*[(i * 0.11, sa.sine(sa.glide(0.1, (0, sa.note(n, 2) * 1.3), (0.1, sa.note(n, 2))), 0.1) * sa.decay(0.1, 0.03, 0.002)) for i, n in enumerate((A, FS, E, D)[v % 4:] + (A, FS, E, D)[:v % 4])])
    return sa.finish(sa.reverb(x, 0.4, 0.12), "effect")


def life_crack(v, rng):
    """Ancient Seed: a shell cracking, then a soft tick."""
    crack = sa.norm(sa.bandpass(sa.noise(0.05, rng), 1500, 6000)) * sa.decay(0.05, 0.008, 0.0003)
    tick = marimba(sa.note(D, 2), 0.3, 0.08) * 0.5
    return sa.finish(sa.reverb(sa.mix(crack, (0.09, tick)), 0.4, 0.1), "effect")


def life_munch(v, rng):
    """Nourish: a soft double crunch under a warm hum."""
    crunch = lambda: sa.norm(sa.lowpass(sa.grains(0.08, 300, rng, shape=[(0, 1), (0.08, 0)]), 2600)) * 0.7
    hum = sa.sine(sa.note(D, 0), 0.4) * sa.swell(0.4, 0.15) * 0.3
    return sa.finish(sa.mix(crunch(), (0.13, crunch()), hum), "effect")


def life_fang(v, rng):
    """Venom: a wet snap and a short hiss."""
    snap = sa.norm(sa.bandpass(sa.noise(0.04, rng), 900, 4500)) * sa.decay(0.04, 0.006, 0.0003)
    hiss = sa.moving_band(0.35, [(0, 6000), (0.35, 3000)], 0.9, rng) * sa.decay(0.35, 0.09, 0.005)
    drip = sa.sine(sa.glide(0.08, (0, 900), (0.08, 350)), 0.08) * sa.decay(0.08, 0.02, 0.002) * 0.4
    return sa.finish(sa.mix(snap, (0.03, 0.3 * hiss), (0.06, drip)), "effect")


def life_thorn(v, rng):
    """Thorns: a crackle of twigs (v0 growing), a sharp snap (v1), a dry double snap (v2)."""
    if v == 0:
        x = sa.norm(sa.bandpass(sa.grains(0.35, 90, rng, shape=[(0, 0.4), (0.3, 1)]), 1200, 5000)) * 0.8
    else:
        n = 1 + v
        x = sa.mix(*[(i * 0.05, sa.norm(sa.bandpass(sa.noise(0.03, rng), 1800, 7000)) * sa.decay(0.03, 0.004, 0.0002)) for i in range(n)])
    return sa.finish(x, "effect")


def life_whip(v, rng):
    """Vinelash: a bullwhip crack with a leaf rustle."""
    sweep_ = sa.moving_band(0.12, [(0, 1500), (0.12, 6500)], 0.9, rng) * sa.decay(0.12, 0.05, 0.002)
    crack = sa.norm(sa.bandpass(sa.noise(0.03, rng), 3000, 9000)) * sa.decay(0.03, 0.004, 0.0002)
    return sa.finish(sa.mix(0.5 * sweep_, (0.11, crack), (0.13, leaves(0.3, 3000, 5000, rng, 0.4, 0.15))), "effect")


def life_grip(v, rng):
    """Rootsnare: a wooden creak sweeping down, a snap and a soil thump."""
    creak = sa.soft_saw(sa.glide(0.5, (0, 220), (0.5, 90)), 0.5, 6, 1800) * sa.decay(0.5, 0.2, 0.02) * 0.35
    snap = sa.norm(sa.bandpass(sa.noise(0.04, rng), 700, 3000)) * sa.decay(0.04, 0.006, 0.0003)
    thump = sa.thump(80, 45, 0.3, 0.07)
    return sa.finish(sa.reverb(sa.mix(creak, (0.3, snap), (0.32, 0.8 * thump)), 0.4, 0.1), "effect")


def life_spore(v, rng):
    """Sporebloom: a muffled 'fwump', a granular hiss, a detuned wobble."""
    dur = 0.8
    fwump = sa.thump(110, 60, 0.4, 0.09, 1.2)
    hiss = sa.norm(sa.lowpass(sa.grains(dur, 220, rng, shape=[(0, 0.6), (0.2, 1), (dur, 0)]), 3500)) * 0.4
    wob = (sa.sine(sa.note(A, 0), dur) + sa.sine(sa.note(A, 0) * 1.03, dur)) * sa.swell(dur, 0.2) * 0.15
    return sa.finish(sa.reverb(sa.mix(fwump, hiss, wob), 0.6, 0.15), "effect")


def life_lull(v, rng):
    """Drowse: a detuned pad falling a fourth, a faint music box."""
    dur = 1.2
    pad = (sa.sine(sa.glide(dur, (0, sa.note(A, 0)), (dur, sa.note(E, 0))), dur) + sa.sine(sa.glide(dur, (0, sa.note(A, 0) * 1.01), (dur, sa.note(E, 0) * 1.01)), dur)) * sa.swell(dur, 0.3) * 0.3
    box = sa.mix(*[(0.25 + i * 0.16, 0.5 * sa.glass(sa.note(n, 3), 0.3, 0.06)) for i, n in enumerate((A, FS, E))])
    return sa.finish(sa.reverb(sa.mix(pad, box), 0.9, 0.25), "effect")


def life_dome(v, rng):
    """Haven: an airy chord swell (D, A, E) with a soft breathing pulse."""
    dur = 1.4
    chord = sa.mix(*[sa.sine(sa.note(n, 0 if n != E else 1), dur) * sa.swell(dur, 0.5) for n in (D, A, E)])
    breath = leaves(dur, 1800, 3600, rng, 0.5, 0.25)
    return sa.finish(sa.reverb(sa.mix(0.3 * chord, breath), 1.0, 0.25), "effect")


def life_bond(v, rng):
    """Soulbond: two bells a fifth apart beating slowly; v1 is the dissonant crack of a snapped bond."""
    if v == 1:
        crack = sa.norm(sa.bandpass(sa.noise(0.08, rng), 1500, 7000)) * sa.decay(0.08, 0.012, 0.0005)
        low = sa.bell(sa.note(D, 0) * 1.06, 0.5, 0.2, 1.41, 1.5, 0.004)
        return sa.finish(sa.reverb(sa.mix(crack, 0.5 * low), 0.6, 0.15), "effect")
    a = sa.bell(sa.note(D, 1), 1.0, 0.5, 2.0, 0.8, 0.01) + sa.bell(sa.note(D, 1) * 1.004, 1.0, 0.5, 2.0, 0.8, 0.01)
    b = sa.bell(sa.note(A, 1), 1.0, 0.5, 2.0, 0.8, 0.01)
    return sa.finish(sa.reverb(sa.mix(a, (0.08, 0.8 * b)), 1.0, 0.25), "effect")


def life_door(v, rng):
    """Reversal: two low heartbeats, a door creak, a rising chorus chord and one bright bell."""
    beat = lambda: sa.thump(70, 42, 0.25, 0.06, 1.2)
    creak = sa.soft_saw(sa.glide(0.6, (0, 140), (0.6, 260)), 0.6, 6, 1500) * sa.swell(0.6, 0.4) * 0.15
    chord = sa.chorus(sa.mix(*[sa.sine(sa.note(n, 1), 1.0) * sa.swell(1.0, 0.7) for n in (D, FS, A, D + 5)]), 4, 0.004, 0.4) * 0.22
    bell = sa.bell(sa.note(D, 3), 1.0, 0.5, 2.0, 0.8, 0.004)
    return sa.finish(sa.reverb(sa.mix(beat(), (0.3, 0.8 * beat()), (0.6, creak), (0.9, chord), (1.5, 0.6 * bell)), 1.4, 0.25), "grand")


def life_gasp(v, rng):
    """Second Wind: a sharp rising inhale into a gust and a bell."""
    inhale = sa.moving_band(0.25, [(0, 1200), (0.25, 5200)], 0.9, rng) * sa.swell(0.25, 0.22, 0.5)
    gust = sa.moving_band(0.5, [(0, 2500), (0.5, 900)], 0.8, rng) * sa.decay(0.5, 0.15, 0.01)
    bell = sa.bell(sa.note(A, 2), 0.6, 0.3, 2.0, 0.8, 0.005)
    return sa.finish(sa.reverb(sa.mix(0.6 * inhale, (0.25, 0.7 * gust), (0.3, 0.5 * bell)), 0.6, 0.15), "effect")


def life_coin(v, rng):
    """Fortune: a flipped coin's metallic flutter and sparkle; v1 a brighter ding for a proc."""
    dur = 0.6
    ring = sa.partials(sa.note(A, 2) if v else sa.note(D, 2), dur, ((1, 1, 1), (2.32, 0.5, 0.6), (4.16, 0.3, 0.4)), 0.25)
    flutter = ring * (0.7 + 0.3 * np.sin(sa.timeline(dur) * 38))
    spark = sa.sparkle(dur, 14, rng, [sa.note(d, 3) for d in (D, E, FS, A)], tau=(0.04, 0.09), shape=[(0, 1), (dur, 0)])
    return sa.finish(sa.reverb(sa.mix(flutter, 0.15 * sa.norm(spark)), 0.5, 0.15), "effect")


def ripen_burst(v, rng):
    chord = sa.mix(*[sa.bell(sa.note(n, 1), 0.9, 0.4, 2.0, 0.8, 0.01) for n in (D, FS, A)])
    return sa.finish(sa.reverb(chord, 0.8, 0.2), "effect")


def life_ripen(v, rng):
    """Lifebloom: a marimba tick one pentatonic step higher each beat (five variants); the last is the burst chord."""
    if v == 4:
        chord = sa.mix(*[sa.bell(sa.note(n, 1), 0.9, 0.4, 2.0, 0.8, 0.01) for n in (D, FS, A)])
        return sa.finish(sa.reverb(chord, 0.8, 0.2), "effect")
    return sa.finish(sa.reverb(marimba(sa.note((D, E, FS, A)[v], 1), 0.4, 0.14), 0.4, 0.1), "effect")


def life_step(v, rng):
    """Bloomstep: an airy shoop, a petal flutter and two soft footsteps."""
    shoop = sa.moving_band(0.3, [(0, 1500), (0.15, 5500), (0.3, 2000)], 0.9, rng) * sa.swell(0.3, 0.12, 1.2)
    step = lambda: sa.thump(150, 80, 0.12, 0.03, 1.1) * 0.5
    flutter = sa.norm(sa.bandpass(sa.grains(0.4, 60, rng, shape=[(0, 1), (0.4, 0)]), 2500, 6500)) * 0.3
    return sa.finish(sa.reverb(sa.mix(0.6 * shoop, (0.2, flutter), (0.34, step()), (0.48, 0.8 * step())), 0.5, 0.12), "effect")


def life_stitch(v, rng):
    """Stitchtime: a thread pulled, a run of clock ticks, and a tight pluck."""
    zip_ = sa.moving_band(0.2, [(0, 2000), (0.2, 6000)], 0.9, rng) * sa.swell(0.2, 0.15, 1.0)
    ticks = sa.mix(*[(0.22 + i * 0.09, sa.tick(sa.note(D, 3), rng, 0.5)) for i in range(4)])
    pluck = marimba(sa.note(A, 2), 0.25, 0.06)
    return sa.finish(sa.reverb(sa.mix(0.5 * zip_, ticks, (0.62, pluck)), 0.4, 0.1), "effect")


def life_petals(v, rng):
    """Moonpetal: swirling airy noise, a descending moon-bell and glitter."""
    dur = 1.2
    swirl = leaves(dur, 1800, 6000, rng, 0.5, 0.5)
    bell = sa.bell(sa.note(A, 1), dur, 0.5, 2.0, 0.7, 0.02)
    glit = sa.sparkle(dur, 16, rng, [sa.note(d, 3) for d in (D, E, FS, A, B)], tau=(0.05, 0.12), shape=[(0, 0.4), (0.4, 1), (dur, 0)])
    return sa.finish(sa.reverb(sa.mix(0.35 * swirl, 0.6 * bell, 0.15 * sa.norm(glit)), 0.9, 0.25), "effect")


def life_pollen(v, rng):
    """Bloom: a chord bloom with pollen puff."""
    puff = sa.norm(sa.lowpass(sa.grains(0.5, 200, rng, shape=[(0, 1), (0.5, 0)]), 4000)) * 0.3
    chord = sa.mix(*[(i * 0.05, sa.bell(sa.note(n, 1), 0.9, 0.4, 2.0, 0.8, 0.02)) for i, n in enumerate((D, A, FS, B))])
    return sa.finish(sa.reverb(sa.mix(chord, puff), 0.8, 0.2), "effect")


def stinger(kind):
    """The 0.15 to 0.4 s role stingers layered under life's cast sound."""
    def build(v, rng):
        if kind == "restore":
            x = sa.mix(marimba(sa.note(D, 1), 0.3, 0.1), (0.09, marimba(sa.note(FS, 1), 0.3, 0.1)))
        elif kind == "ward":
            x = sa.mix(*[sa.sine(sa.note(n, 0), 0.35) * sa.swell(0.35, 0.15) * 0.4 for n in (D, A)], 0.2 * sa.norm(sa.bandpass(sa.noise(0.03, rng), 1500, 4000)) * sa.decay(0.03, 0.004))
        elif kind == "world":
            x = sa.mix(sa.sine(sa.glide(0.1, (0, sa.note(A, 1) * 0.8), (0.1, sa.note(A, 1))), 0.1) * sa.decay(0.1, 0.04, 0.003), 0.3 * sa.norm(sa.bandpass(sa.noise(0.03, rng), 600, 2000)) * sa.decay(0.03, 0.006))
        else:
            x = sa.mix(sa.norm(sa.bandpass(sa.noise(0.04, rng), 1800, 7000)) * sa.decay(0.04, 0.005, 0.0002), 0.3 * sa.norm(sa.bandpass(sa.noise(0.15, rng), 3000, 6000)) * sa.decay(0.15, 0.04))
        return sa.finish(x, "tick")
    return build


EVENTS = [
    event("life_heal", life_heal, variants=2, subtitle="cast"),
    event("life_regrow", life_regrow, subtitle="cast"),
    event("life_mend", life_mend, subtitle="hit"),
    event("life_purge", life_purge, subtitle="hit"),
    event("life_transmute", life_transmute, subtitle="hit"),
    event("life_grow", life_grow, variants=3, subtitle="hit"),
    event("life_glimmer", life_glimmer, subtitle="field"),
    event("life_reap", life_reap, subtitle="hit"),
    event("life_plip", life_plip, variants=3, subtitle="field"),
    event("life_crack", life_crack, subtitle="hit"),
    event("life_munch", life_munch, subtitle="hit"),
    event("life_fang", life_fang, subtitle="hit"),
    event("life_thorn", lambda v, rng: life_thorn(v + 1, rng), variants=2, subtitle="hit"),
    event("life_thorn_grow", lambda v, rng: life_thorn(0, rng), subtitle="hit"),
    event("life_whip", life_whip, subtitle="hit"),
    event("life_grip", life_grip, subtitle="hit"),
    event("life_spore", life_spore, subtitle="field"),
    event("life_lull", life_lull, subtitle="field"),
    event("life_dome", life_dome, subtitle="field"),
    event("life_bond", lambda v, rng: life_bond(0, rng), subtitle="field"),
    event("life_bond_snap", lambda v, rng: life_bond(1, rng), subtitle="field"),
    event("life_door", life_door, subtitle="cast"),
    event("life_gasp", life_gasp, subtitle="cast"),
    event("life_coin", lambda v, rng: life_coin(0, rng), subtitle="hit"),
    event("life_coin_proc", lambda v, rng: life_coin(1, rng), subtitle="hit"),
    event("life_ripen", life_ripen, variants=2, subtitle="field"),
    event("life_ripen_burst", ripen_burst, subtitle="field"),
    event("life_step", life_step, subtitle="cast"),
    event("life_stitch", life_stitch, subtitle="field"),
    event("life_petals", life_petals, subtitle="hit"),
    event("life_pollen", life_pollen, subtitle="hit"),
    event("life_stinger_restore", stinger("restore"), variants=2, role="tick", subtitle="cast"),
    event("life_stinger_ward", stinger("ward"), role="tick", subtitle="cast"),
    event("life_stinger_world", stinger("world"), variants=2, role="tick", subtitle="cast"),
    event("life_stinger_thorn", stinger("thorn"), variants=2, role="tick", subtitle="cast"),
]


# ---------------------------------------------------------------- a world's resonances: the twists' voices (cast/TwistVfx)

def life_frost_bloom(v, rng):
    """Winter Blossom: flowers opening out of the cold, a tone rising and crystal chimes climbing."""
    dur = 1.6
    bloom = sa.sine(sa.glide(dur, (0, sa.note(sa.D, 1)), (0.8, sa.note(sa.A, 1)), (dur, sa.note(sa.A, 1)))) * sa.swell(dur, 0.6) * 0.5
    crystals = sa.sparkle(1.0, 12, rng, [sa.note(d, 3) for d in (sa.D, sa.FS, sa.A, sa.B)], tau=(0.08, 0.25), rising=True)
    leaves = sa.moving_band(dur, [(0, 2500), (dur, 4000)], 0.6, rng) * sa.swell(dur, 0.5) * 0.2
    return sa.finish(sa.reverb(sa.mix(bloom, 0.6 * crystals, leaves), 1.0, 0.2), "effect")


def life_thorn_crown(v, rng):
    """Crown of Thorns: wood creaking as the crown tightens, twigs snapping."""
    dur = 0.8
    creak = sa.soft_saw(sa.glide(0.5, (0, 140 + 20 * v), (0.5, 90)), 0.5, harmonics=10) * sa.chopper(0.5, rng, rate=(30, 60), floor=0.3)
    creak = creak * sa.decay(0.5, 0.18, 0.02)
    snaps = sa.norm(sa.bandpass(sa.grains(dur, 40, rng, length=(0.001, 0.004)), 1500, 6000))
    tone = sa.bell(sa.note(sa.E, 1), dur, 0.2, ratio=2.0, brightness=0.6) * 0.3
    return sa.finish(sa.reverb(sa.mix(0.5 * sa.lowpass(creak, 2500), 0.6 * snaps, tone), 0.5, 0.1), "impact")


def life_firefly_hush(v, rng):
    """Lantern Flies: soft twinkles over the hush of a summer night."""
    dur = 2.2
    twinkles = sa.sparkle(dur, 9, rng, [sa.note(d, 3) for d in (sa.D, sa.E, sa.FS, sa.A, sa.B)], tau=(0.05, 0.15))
    night = sa.moving_band(dur, 3600, 0.25, rng) * sa.chopper(dur, rng, rate=(14, 22), floor=0.0) * sa.swell(dur, 0.6) * 0.15
    hum = sa.sine(sa.note(sa.A, -1), dur) * sa.swell(dur, 0.8) * 0.2
    return sa.finish(sa.reverb(sa.mix(0.6 * twinkles, night, hum), 1.0, 0.2), "effect")


def life_thicket_burst(v, rng):
    """Sudden Thicket: a thud of roots breaking ground, a rustle of leaves, a creak of wood."""
    dur = 1.0
    thud = sa.thump(130 - 15 * v, 60, 0.5, 0.08, drive=1.3)
    rustle = sa.norm(sa.bandpass(sa.grains(0.7, 500, rng, length=(0.002, 0.008), shape=[(0, 1), (0.7, 0)]), 1200, 6000))
    creak = sa.soft_saw(sa.glide(0.6, (0, 220), (0.6, 130)), 0.6, harmonics=8) * sa.decay(0.6, 0.2, 0.01) * 0.3
    return sa.finish(sa.reverb(sa.mix(0.7 * thud, (0.02, 0.7 * rustle), (0.05, sa.lowpass(creak, 2000))), 0.5, 0.1), "impact")


EVENTS += [
    event("life_frost_bloom", life_frost_bloom, variants=2, role="effect", subtitle="field"),
    event("life_thorn_crown", life_thorn_crown, variants=2, role="impact", subtitle="hit"),
    event("life_firefly_hush", life_firefly_hush, variants=1, role="effect", subtitle="field"),
    event("life_thicket_burst", life_thicket_burst, variants=2, role="impact", subtitle="hit"),
]
# ---------------------------------------------------------------- residues: what's left behind


def life_bloom(v, rng):
    """A wildbloom: a soft three-note chime opening upward, and a rustle of petals."""
    dur = 1.4
    notes = ((sa.D, 1), (sa.FS, 1), (sa.A, 1)) if v == 0 else ((sa.E, 1), (sa.A, 1), (sa.B, 1))
    chime = [((i * 0.14), sa.bell(sa.note(*n), 0.9, 0.3, ratio=2.0, brightness=0.8, attack=0.02) * (0.5 - i * 0.1)) for i, n in enumerate(notes)]
    rustle = sa.norm(sa.bandpass(sa.grains(dur, 40, rng, length=(0.002, 0.008), shape=[(0, 0.5), (0.5, 1), (dur, 0)]), 1800, 5000)) * 0.15
    x = sa.mix(*chime, rustle)
    return sa.finish(sa.reverb(x, 0.9, 0.25), "tell", fade_out=0.35)


EVENTS += [event("life_bloom", life_bloom, variants=2, role="tell", subtitle="field")]
