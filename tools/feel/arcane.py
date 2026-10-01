"""Arcane's kit sounds: one voice per rune on glass and bell, sparkle and shimmer (a bright, cool instrument family).

Names start with 'arcane_'. Every tonal part sits in D major pentatonic (see tools/feel/fire.py for the pattern to copy).
"""
import numpy as np

from feel.core import sa, event

D, E, FS, A, B = sa.D, sa.E, sa.FS, sa.A, sa.B


def glassrun(notes, step, octave=2, tau=0.09, dur=0.5):
    """Glass pings one after another, `step` seconds apart."""
    return sa.mix(*[(i * step, sa.glass(sa.note(n, octave), dur, tau)) for i, n in enumerate(notes)])


def air(dur, lo, hi, rng, level=1.0, peak=0.5):
    return level * sa.moving_band(dur, [(0, lo), (dur * peak, hi), (dur, lo)], 0.9, rng) * sa.swell(dur, dur * peak, 1.5)


def arcane_needle(v, rng):
    """Harm: a dry high 'tsk' and a resonant glass thump."""
    tsk = sa.norm(sa.bandpass(sa.noise(0.03, rng), 4000, 10000)) * sa.decay(0.03, 0.004, 0.0002)
    thump = sa.mix(sa.glass(sa.note((D, E)[v % 2], 1), 0.35, 0.07) * 0.6, 0.5 * sa.thump(220, 110, 0.14, 0.03))
    return sa.finish(sa.reverb(sa.mix(tsk, (0.01, thump)), 0.4, 0.1), "impact")


def arcane_fangs(v, rng):
    """Fangs: three bone clacks."""
    clack = lambda f: sa.mix(sa.sine(f, 0.05) * sa.decay(0.05, 0.007, 0.0003), 0.4 * sa.norm(sa.bandpass(sa.noise(0.03, rng), 1500, 6000)) * sa.decay(0.03, 0.004))
    return sa.finish(sa.mix(clack(420), (0.09, clack(500)), (0.17, clack(380))), "impact")


def arcane_toll(v, rng):
    """Smite: a rising tone through the wind-up, a low bell toll, a sharp crack."""
    rise = sa.sine(sa.glide(0.7, (0, sa.note(D, 1)), (0.7, sa.note(D, 2))), 0.7) * sa.swell(0.7, 0.6, 0.5) * 0.3
    toll = sa.bell(sa.note(D, -1), 1.2, 0.6, 1.41, 1.2, 0.004)
    crack = sa.norm(sa.bandpass(sa.noise(0.06, rng), 1200, 8000)) * sa.decay(0.06, 0.01, 0.0003)
    return sa.finish(sa.reverb(sa.mix(rise, (0.7, 0.8 * toll), (0.7, 0.7 * crack)), 1.1, 0.2), "grand")


def arcane_starfall(v, rng):
    """Starfall: a falling whistle and a pentatonic plink per star (six variants, one per scale step)."""
    n = (D, E, FS, A, B, D)[v]
    whistle = sa.sine(sa.glide(0.25, (0, sa.note(n, 3) * 1.6), (0.25, sa.note(n, 3))), 0.25) * sa.swell(0.25, 0.2, 0.4) * 0.25
    plink = sa.glass(sa.note(n, 2), 0.45, 0.08)
    return sa.finish(sa.reverb(sa.mix(whistle, (0.24, plink)), 0.6, 0.15), "effect")


def arcane_comet(v, rng):
    """Cometfall: a one-second rising whistle over a sub rumble, then a boom and a crystal shatter."""
    dur = 1.0
    whistle = sa.sine(sa.glide(dur, (0, 400), (dur, 2400)), dur) * sa.swell(dur, 0.95, 1.0) * 0.25
    rumble = sa.norm(sa.lowpass(sa.noise(dur, rng), 160)) * sa.swell(dur, 0.9, 1.0) * 0.5
    boom = sa.thump(70, 32, 0.9, 0.2, 1.6)
    return sa.finish(sa.reverb(sa.mix(whistle, rumble, (dur, boom), (dur, 0.5 * sa.shatter(rng))), 1.4, 0.25), "grand")


def arcane_split(v, rng):
    """Starshard: a crystal ping, then three quick descending ticks."""
    ping = sa.glass(sa.note(A, 2), 0.5, 0.1)
    ticks = sa.mix(*[(0.12 + i * 0.07, 0.7 * sa.glass(sa.note(n, 3), 0.2, 0.04)) for i, n in enumerate((B, A, FS))])
    return sa.finish(sa.reverb(sa.mix(ping, ticks), 0.5, 0.12), "impact")


def arcane_resonate(v, rng):
    """Resonance: a struck fork ring with beating overtones (v1 softer, for the echoes)."""
    dur = 1.2
    f = sa.note(A, 1)
    ring = (sa.sine(f, dur) + sa.sine(f * 1.006, dur)) * sa.decay(dur, 0.5, 0.004) + 0.3 * sa.sine(f * 2.76, dur) * sa.decay(dur, 0.2, 0.002)
    return sa.finish(sa.reverb(ring * (0.5 if v else 1.0), 0.8, 0.2), "impact")


def arcane_gong(v, rng):
    """Decree: a low gong with a formant 'ah', cut short."""
    dur = 0.9
    gong = sa.bell(sa.note(D, 0), dur, 0.4, 1.41, 1.6, 0.004)
    voice = sa.soft_saw(sa.note(D, 0), dur, 10, 1500) * sa.swell(dur, 0.25, 2.5)
    voice = sa.bandpass(voice, 700, 1200) * 0.6
    return sa.finish(sa.reverb(sa.mix(gong, (0.05, voice)), 0.7, 0.15), "impact")


def arcane_hush(v, rng):
    """Silence: a low-pass sweep down to nothing, and a tiny tick."""
    dur = 0.6
    hiss = sa.noise(dur, rng)
    shaped = sa.norm(sa.lowpass(hiss, 3000)) * sa.decay(dur, 0.18, 0.005)
    tick = sa.tick(sa.note(D, 3), rng, 0.4)
    return sa.finish(sa.mix(0.6 * shaped, (0.5, tick)), "effect")


def arcane_scorch(v, rng):
    """Manaburn: electric fizz and an inward whoosh."""
    dur = 0.55
    fizz = sa.norm(sa.bandpass(sa.grains(dur, 260, rng, shape=[(0, 1), (dur, 0.1)]), 2500, 8000)) * 0.5
    suck = sa.moving_band(dur, [(0, 5000), (dur, 700)], 0.9, rng) * sa.swell(dur, 0.4, 1.0) * 0.5
    return sa.finish(sa.reverb(sa.mix(fizz, suck), 0.4, 0.1), "impact")


def arcane_inhale(v, rng):
    """Manatide: a swelling upward shimmer; the short variant is one quick inhale tick per refund."""
    if v == 1:
        return sa.finish(sa.mix(air(0.15, 2500, 6000, rng, 0.6, 0.8)), "tick")
    dur = 0.9
    shim = sa.mix(*[sa.sine(sa.glide(dur, (0, sa.note(n, 1)), (dur, sa.note(n, 2))), dur) * sa.swell(dur, 0.7) * 0.3 for n in (D, A)])
    return sa.finish(sa.reverb(sa.mix(shim, air(dur, 1800, 4500, rng, 0.25, 0.8)), 0.8, 0.2), "effect")


def arcane_snuff(v, rng):
    """Nullify: a reversed chime and a candle-snuff 'thup'."""
    chime = sa.reverse(sa.bell(sa.note(A, 2), 0.5, 0.2, 2.0, 0.9, 0.003))
    thup = sa.thump(180, 70, 0.12, 0.03, 1.0) * 0.8
    return sa.finish(sa.reverb(sa.mix(chime, (0.5, thup)), 0.4, 0.1), "impact")


def arcane_mirror(v, rng):
    """Reflect: a glass 'ting' and a shard tinkle; v1 a soft crack per reflection."""
    if v == 1:
        return sa.finish(sa.mix(sa.norm(sa.bandpass(sa.noise(0.04, rng), 2500, 9000)) * sa.decay(0.04, 0.006), (0.02, 0.5 * sa.glass(sa.note(B, 3), 0.2, 0.04))), "impact")
    return sa.finish(sa.reverb(sa.mix(sa.glass(sa.note(FS, 3), 0.6, 0.12), (0.05, 0.5 * sa.shatter(rng, 10, (3000, 8000), 0.1))), 0.5, 0.12), "effect")


def arcane_swap(v, rng):
    """Swap: a snap and a reverse swoosh."""
    snap = sa.norm(sa.bandpass(sa.noise(0.03, rng), 1800, 7000)) * sa.decay(0.03, 0.005, 0.0002)
    whoosh = sa.reverse(sa.moving_band(0.25, [(0, 1500), (0.25, 5000)], 0.9, rng) * sa.decay(0.25, 0.08, 0.005))
    return sa.finish(sa.mix(0.5 * whoosh, (0.22, snap)), "impact")


def arcane_hex(v, rng):
    """Barrier: a bright hex hum with a click (v1 a crystal crack with a soft pulse, for breaking)."""
    if v == 1:
        return sa.finish(sa.reverb(sa.mix(0.7 * sa.shatter(rng, 14, (2500, 7000), 0.1), (0.05, sa.thump(140, 70, 0.2, 0.05))), 0.5, 0.12), "impact")
    dur = 0.8
    hum = sum(sa.sine(sa.note(n, 1), dur) for n in (D, A, FS)) * sa.swell(dur, 0.3) * 0.2
    click = sa.tick(sa.note(A, 3), rng, 0.5)
    return sa.finish(sa.reverb(sa.mix(hum, click), 0.6, 0.15), "effect")


def arcane_glassrun(v, rng):
    """Span: a ladder of glass pings (v0) or a crash cascade of shatters (v1)."""
    if v == 1:
        return sa.finish(sa.reverb(sa.mix(*[(i * 0.05, sa.shatter(rng, 8, (2500, 7000), 0.08) * 0.5) for i in range(6)]), 0.7, 0.2), "effect")
    return sa.finish(sa.reverb(glassrun((D, E, FS, A, B, D + 5, E + 5, FS + 5), 0.06, 2, 0.07, 0.4), 0.7, 0.2), "effect")


def arcane_twin(v, rng):
    """Twin Star: a doubled bell with a 150 ms echo; v1 an octave lower for the second cast."""
    o = 1 - v
    bell = sa.bell(sa.note(A, o), 0.7, 0.3, 2.0, 1.0, 0.004)
    return sa.finish(sa.reverb(sa.mix(bell, (0.15, 0.6 * bell)), 0.6, 0.15), "effect")


def arcane_glint(v, rng):
    """Treasure Sense: a coin jingle with glitter; v1 a single soft ping per pulse."""
    if v == 1:
        return sa.finish(sa.reverb(sa.glass(sa.note(E, 3), 0.5, 0.1) * 0.7, 0.5, 0.15), "tick")
    jingle = sa.mix(*[(i * 0.05, 0.6 * sa.partials(sa.note(n, 3), 0.25, ((1, 1, 1), (2.32, 0.5, 0.5), (4.16, 0.3, 0.3)), 0.08)) for i, n in enumerate((A, B, D + 5, A))])
    return sa.finish(sa.reverb(sa.mix(jingle, 0.15 * sa.norm(sa.sparkle(0.5, 14, rng, [sa.note(d, 3) for d in (D, E, FS, A)]))), 0.5, 0.15), "effect")


def arcane_pluck(v, rng):
    """Starlight Tether: a harp pluck on each pull over a thin thread hum."""
    pluck = sa.sine(sa.note((D, FS, A)[v % 3], 1), 0.6) * sa.decay(0.6, 0.18, 0.002) + 0.3 * sa.sine(sa.note((D, FS, A)[v % 3], 2), 0.6) * sa.decay(0.6, 0.08)
    hum = sa.sine(sa.note(D, 3), 0.5) * sa.swell(0.5, 0.15) * 0.06
    return sa.finish(sa.reverb(sa.mix(pluck, hum), 0.6, 0.15), "tick")


def arcane_flutter(v, rng):
    """Haste: three fast rising notes."""
    return sa.finish(sa.reverb(glassrun((A, D + 5, FS + 5), 0.05, 1, 0.05, 0.25), 0.4, 0.1), "effect")


def arcane_thrum(v, rng):
    """Empower: a sub-bass rise with crackle; v1 a soft exhale for the comedown."""
    if v == 1:
        return sa.finish(sa.mix(air(0.5, 1200, 500, rng, 0.6, 0.2)), "effect")
    dur = 0.6
    sub = sa.sine(sa.glide(dur, (0, 90), (dur, 180)), dur) * sa.swell(dur, 0.5, 1.0) * 0.6
    crackle = sa.norm(sa.bandpass(sa.grains(dur, 180, rng, shape=[(0, 0.2), (dur * 0.8, 1), (dur, 0)]), 800, 4500)) * 0.7
    return sa.finish(sa.reverb(sa.mix(sa.saturate(sub, 1.5), crackle), 0.5, 0.1), "effect")


def arcane_open(v, rng):
    """Night Eye: an airy inhale and two soft high chimes."""
    return sa.finish(sa.reverb(sa.mix(air(0.35, 1800, 4500, rng, 0.4, 0.7), (0.3, glassrun((A, D + 5), 0.12, 2, 0.1, 0.5))), 0.7, 0.2), "effect")


def arcane_ping(v, rng):
    """Reveal: a sonar ping with a ring."""
    ping = sa.sine(sa.note(D, 2), 0.7) * sa.decay(0.7, 0.3, 0.002)
    ring = sa.sine(sa.glide(0.5, (0, sa.note(D, 2)), (0.5, sa.note(D, 2) * 0.99)), 0.5) * sa.decay(0.5, 0.15) * 0.3
    return sa.finish(sa.reverb(sa.mix(ping, (0.02, ring)), 0.9, 0.3), "effect")


def arcane_kindle(v, rng):
    """Light: a match strike into a bell (v1 a faint hum for a lamp that follows)."""
    if v == 1:
        return sa.finish(sa.mix(sa.sine(sa.note(D, 1), 0.6) * sa.swell(0.6, 0.3) * 0.08), "tick")
    strike = sa.norm(sa.bandpass(sa.noise(0.12, rng), 2000, 8000)) * sa.decay(0.12, 0.03, 0.001)
    bell = sa.bell(sa.note(A, 2), 0.8, 0.35, 2.0, 0.8, 0.006)
    return sa.finish(sa.reverb(sa.mix(0.6 * strike, (0.1, bell)), 0.6, 0.15), "effect")


def arcane_howl(v, rng):
    """Summon: an ethereal formant howl over a bell; v1 a small yip."""
    dur = 1.5 if v == 0 else 0.25
    f = sa.glide(dur, (0, sa.note(A, 0)), (dur * 0.4, sa.note(D, 1)), (dur, sa.note(A, 0) * 0.9)) if v == 0 else sa.glide(dur, (0, 500), (dur, 800))
    howl = sa.bandpass(sa.soft_saw(f, dur, 10, 3000), 500, 1400) * sa.swell(dur, dur * 0.4, 1.5)
    bell = sa.bell(sa.note(D, 2), 1.0, 0.5, 2.0, 0.8, 0.01) * (0.4 if v == 0 else 0.0)
    return sa.finish(sa.reverb(sa.mix(howl * 0.6, bell), 1.2 if v == 0 else 0.4, 0.3), "grand" if v == 0 else "effect")


def arcane_halo(v, rng):
    """Halo: an angelic bell chorus on opening (v0); a 'ting-zap' per smite (v1)."""
    if v == 1:
        return sa.finish(sa.mix(sa.glass(sa.note(B, 3), 0.3, 0.06), (0.03, 0.5 * sa.norm(sa.bandpass(sa.noise(0.05, rng), 3000, 9000)) * sa.decay(0.05, 0.008))), "impact")
    chord = sa.chorus(sa.mix(*[sa.bell(sa.note(n, 1), 1.0, 0.5, 2.0, 0.7, 0.02) for n in (D, A, FS, D + 5)]), 3, 0.004, 0.5)
    return sa.finish(sa.reverb(chord, 1.0, 0.25), "effect")


def arcane_stamp(v, rng):
    """Spellbrand: a sealed-page thunk (v0); a glass burst (v1, three pitches for the trigger's element)."""
    if v == 0:
        thunk = sa.thump(280, 120, 0.2, 0.05, 1.3, 0.8)
        shim = sa.sparkle(0.4, 20, rng, [sa.note(d, 3) for d in (D, E, FS, A)], tau=(0.04, 0.09))
        return sa.finish(sa.reverb(sa.mix(thunk, 0.2 * sa.norm(shim)), 0.5, 0.12), "impact")
    burst = sa.shatter(rng, 20, (2000 + 600 * v, 6500), 0.12)
    return sa.finish(sa.reverb(sa.mix(burst, (0.0, sa.glass(sa.note((D, E, FS, A)[v % 4], 2), 0.5, 0.08) * 0.5)), 0.6, 0.15), "impact")


def stinger(kind):
    """The 0.15 to 0.4 s role stingers layered under arcane's cast sound."""
    def build(v, rng):
        if kind == "strike":
            x = sa.mix(sa.norm(sa.bandpass(sa.noise(0.03, rng), 3500, 9000)) * sa.decay(0.03, 0.004, 0.0002), 0.5 * sa.thump(320, 160, 0.12, 0.03, 1.5, 0.8))
        elif kind == "mark":
            x = sa.mix(sa.thump(260, 130, 0.15, 0.04, 1.3, 0.8), 0.2 * sa.norm(sa.sparkle(0.25, 30, rng, [sa.note(d, 3) for d in (D, E, FS, A)])))
        elif kind == "glass":
            x = sa.mix(sa.tick(sa.note(A, 3), rng, 0.6), sum(sa.sine(sa.note(n, 1), 0.3) for n in (D, A)) * sa.swell(0.3, 0.1) * 0.15)
        elif kind == "step":
            x = sa.mix(sa.norm(sa.bandpass(sa.noise(0.03, rng), 1500, 6000)) * sa.decay(0.03, 0.005, 0.0002), 0.4 * sa.moving_band(0.2, [(0, 1500), (0.2, 4500)], 0.9, rng) * sa.decay(0.2, 0.06))
        else:
            x = sa.mix(sa.bell(sa.note(D, 1), 0.4, 0.2, 2.0, 0.7, 0.01) * 0.6, 0.25 * air(0.3, 1500, 3500, rng, 1.0, 0.4))
        return sa.finish(x, "tick")
    return build


EVENTS = [
    event("arcane_needle", arcane_needle, variants=2, role="impact", subtitle="hit"),
    event("arcane_fangs", arcane_fangs, role="impact", subtitle="hit"),
    event("arcane_toll", arcane_toll, role="grand", subtitle="hit"),
    event("arcane_starfall", lambda v, rng: arcane_starfall(0, rng), subtitle="hit"),
    event("arcane_comet", arcane_comet, role="grand", subtitle="hit"),
    event("arcane_split", arcane_split, role="impact", subtitle="hit"),
    event("arcane_resonate", lambda v, rng: arcane_resonate(0, rng), role="impact", subtitle="hit"),
    event("arcane_resonate_echo", lambda v, rng: arcane_resonate(1, rng), role="impact", subtitle="hit"),
    event("arcane_gong", arcane_gong, role="impact", subtitle="hit"),
    event("arcane_hush", arcane_hush, subtitle="hit"),
    event("arcane_scorch", arcane_scorch, role="impact", subtitle="hit"),
    event("arcane_inhale", lambda v, rng: arcane_inhale(0, rng), subtitle="field"),
    event("arcane_inhale_tick", lambda v, rng: arcane_inhale(1, rng), variants=2, role="tick", subtitle="field"),
    event("arcane_snuff", arcane_snuff, role="impact", subtitle="hit"),
    event("arcane_mirror", lambda v, rng: arcane_mirror(0, rng), subtitle="hit"),
    event("arcane_mirror_crack", lambda v, rng: arcane_mirror(1, rng), variants=2, role="impact", subtitle="hit"),
    event("arcane_swap", arcane_swap, role="impact", subtitle="hit"),
    event("arcane_hex", lambda v, rng: arcane_hex(0, rng), subtitle="field"),
    event("arcane_hex_pop", lambda v, rng: arcane_hex(1, rng), role="impact", subtitle="hit"),
    event("arcane_glassrun", lambda v, rng: arcane_glassrun(0, rng), subtitle="field"),
    event("arcane_glassshatter", lambda v, rng: arcane_glassrun(1, rng), variants=2, subtitle="field"),
    event("arcane_twin", lambda v, rng: arcane_twin(0, rng), subtitle="cast"),
    event("arcane_twin_echo", lambda v, rng: arcane_twin(1, rng), subtitle="cast"),
    event("arcane_glint", lambda v, rng: arcane_glint(0, rng), subtitle="field"),
    event("arcane_glint_ping", lambda v, rng: arcane_glint(1, rng), role="tick", subtitle="field"),
    event("arcane_pluck", arcane_pluck, variants=3, role="tick", subtitle="hit"),
    event("arcane_flutter", arcane_flutter, subtitle="cast"),
    event("arcane_thrum", lambda v, rng: arcane_thrum(0, rng), subtitle="cast"),
    event("arcane_thrum_exhale", lambda v, rng: arcane_thrum(1, rng), subtitle="cast"),
    event("arcane_open", arcane_open, subtitle="cast"),
    event("arcane_ping", arcane_ping, subtitle="hit"),
    event("arcane_kindle", lambda v, rng: arcane_kindle(0, rng), subtitle="cast"),
    event("arcane_kindle_hum", lambda v, rng: arcane_kindle(1, rng), role="tick", subtitle="field"),
    event("arcane_howl", lambda v, rng: arcane_howl(0, rng), role="grand", subtitle="cast"),
    event("arcane_yip", lambda v, rng: arcane_howl(1, rng), variants=2, subtitle="cast"),
    event("arcane_halo", lambda v, rng: arcane_halo(0, rng), subtitle="cast"),
    event("arcane_halo_zap", lambda v, rng: arcane_halo(1, rng), role="impact", subtitle="hit"),
    event("arcane_stamp", lambda v, rng: arcane_stamp(0, rng), role="impact", subtitle="hit"),
    event("arcane_stamp_burst", lambda v, rng: arcane_stamp(v + 1, rng), variants=3, role="impact", subtitle="hit"),
    event("arcane_stinger_strike", stinger("strike"), variants=2, role="tick", subtitle="cast"),
    event("arcane_stinger_mark", stinger("mark"), variants=2, role="tick", subtitle="cast"),
    event("arcane_stinger_glass", stinger("glass"), variants=2, role="tick", subtitle="cast"),
    event("arcane_stinger_step", stinger("step"), variants=2, role="tick", subtitle="cast"),
    event("arcane_stinger_summon", stinger("summon"), role="tick", subtitle="cast"),
]


# ---------------------------------------------------------------- a world's resonances: the twists' voices (cast/TwistVfx)

def arcane_wingflock(v, rng):
    """Birds of Light: a flutter of wings and chirps of glass climbing the scale."""
    dur = 1.0
    flutter = sa.moving_band(dur, [(0, 900), (dur, 1800)], 0.9, rng) * sa.chopper(dur, rng, rate=(18, 30), floor=0.2) * sa.swell(dur, 0.1)
    chirps = sa.sparkle(0.8, 16, rng, [sa.note(d, 3) for d in (sa.D, sa.E, sa.FS, sa.A, sa.B)], tau=(0.03, 0.08), rising=True)
    burst = sa.norm(sa.bandpass(sa.noise(0.08, rng), 1500, 6000)) * sa.decay(0.08, 0.02, 0.001)
    return sa.finish(sa.reverb(sa.mix(0.6 * flutter, 0.5 * chirps, 0.4 * burst), 0.6, 0.15), "effect")


def arcane_page_storm(v, rng):
    """Paper Storm: pages rustling round and round, a whisper in them, and a few glyphs chiming."""
    dur = 2.0
    rustle = sa.norm(sa.bandpass(sa.grains(dur, 400, rng, length=(0.002, 0.01), shape=[(0, 0.3), (0.3, 1), (dur, 0.4)]), 1800, 6500))
    whisper = sa.moving_band(dur, [(0, 1500), (1.0, 3000), (dur, 2000)], 0.5, rng) * sa.swell(dur, 0.6) * 0.4
    glyphs = sa.sparkle(dur, 6, rng, [sa.note(d, 2) for d in (sa.D, sa.FS, sa.A)], tau=(0.1, 0.3))
    return sa.finish(sa.reverb(sa.mix(0.7 * rustle, whisper, 0.4 * glyphs), 0.9, 0.16), "effect")


def arcane_star_chime(v, rng):
    """Star Wake, one star: a falling whistle and a bright chime where it bursts."""
    dur = 1.0
    whistle = sa.sine(sa.glide(0.3, (0, 2600), (0.3, 900))) * sa.env(0.3, (0, 0), (0.2, 0.3), (0.3, 0)) * 0.4
    chime = sa.glass(sa.note((sa.D, sa.FS, sa.A)[v], 3), dur, 0.25) + 0.5 * sa.bell(sa.note(sa.D, 2), dur, 0.3, ratio=3.0)
    pop = sa.norm(sa.bandpass(sa.noise(0.05, rng), 1500, 6000)) * sa.decay(0.05, 0.008, 0.0005)
    return sa.finish(sa.reverb(sa.mix(whistle, (0.28, 0.8 * chime), (0.28, 0.5 * pop)), 0.8, 0.15), "impact")


def arcane_aurora(v, rng):
    """Aurora: a slow shimmering chord swelling in, and glitter falling through it."""
    dur = 2.6
    pad = sum(sa.soft_saw(sa.note(d, o), dur, harmonics=6) * w for d, o, w in ((sa.D, 0, 0.5), (sa.A, 0, 0.4), (sa.FS, 1, 0.35), (sa.B, 1, 0.25)))
    pad = sa.chorus(sa.lowpass(pad, 2400), voices=4, depth=0.004, rate=0.3)
    pad = pad[:sa.samples(dur)] * sa.swell(dur, 0.9, 1.5)
    glitter = sa.sparkle(dur, 7, rng, [sa.note(d, 3) for d in (sa.D, sa.E, sa.A, sa.B)], tau=(0.1, 0.3))
    return sa.finish(sa.reverb(sa.mix(pad, 0.35 * glitter), 1.4, 0.25), "grand")


def arcane_great_bell(v, rng):
    """Great Bell: one great bell struck, its hum an octave below hanging on."""
    dur = 3.0
    bell = sa.clock_bell(sa.note(sa.D, -1), dur, 1.1)
    hum = sa.sine(sa.note(sa.D, -2), dur) * sa.decay(dur, 1.2, 0.01) * 0.3
    strike = sa.norm(sa.bandpass(sa.noise(0.04, rng), 300, 2500)) * sa.decay(0.04, 0.008, 0.0005)
    return sa.finish(sa.reverb(sa.mix(bell, hum, 0.4 * strike), 1.6, 0.25), "grand")


EVENTS += [
    event("arcane_wingflock", arcane_wingflock, variants=2, role="effect", subtitle="hit"),
    event("arcane_page_storm", arcane_page_storm, variants=1, role="effect", subtitle="field"),
    event("arcane_star_chime", arcane_star_chime, variants=3, role="impact", subtitle="hit"),
    event("arcane_aurora", arcane_aurora, variants=1, role="grand", subtitle="field"),
    event("arcane_great_bell", arcane_great_bell, variants=1, role="grand", subtitle="field", attenuation=32),
]
# ---------------------------------------------------------------- residues: what's left behind


def arcane_glyph(v, rng):
    """A star glyph: starlight rising off its lines, a little rising run of glass notes."""
    dur = 1.2
    notes = [sa.note(d, 2) for d in (sa.D, sa.E, sa.FS, sa.A, sa.B)]
    run = sa.sparkle(0.8, 7, rng, notes, tau=(0.06, 0.16), rising=True)
    shimmer = sa.norm(sa.bandpass(sa.noise(dur, rng), 3000, 6500)) * sa.swell(dur, 0.6) * 0.08
    x = sa.mix(sa.norm(run) * 0.8, shimmer)
    return sa.finish(sa.reverb(x, 1.0, 0.3), "tell", fade_out=0.35)


EVENTS += [event("arcane_glyph", arcane_glyph, variants=2, role="tell", subtitle="field")]
