"""Sounds that belong to no element (names have no element prefix): gestures, tells, notes, the HUD. Owned by the
shapes/modifiers/links work.

Families:
    gesture_<motion>   the spell leaving the hand, one per motion (Feels.cue plays it; ShapeFeels steps each shape's pitch)
    note_<family>      one pitched pluck at D per rune family: the spell's melody while charging, and the tells
    tell_* / gate_*    a modifier or link announcing itself
    field_pulse, toll  one landing of a field; a Totem's or Domain's beat
    tap_release, ready_ping, fizzle   the cast key and the HUD

Tonal sounds are tuned to D so the pentatonic ratios (1.0, 1.122, 1.26, 1.498, 1.682, 2.0) make a scale from one sample.
"""
import numpy as np

from feel.core import sa, event

D, E, FS, A, B = sa.D, sa.E, sa.FS, sa.A, sa.B


def _clean(x, role, verb=0.3, wet=0.08, damp=6000.0):
    """Reverb, then take out any DC before levelling (short sounds built from a thump drift off zero)."""
    return sa.finish(sa.highpass(sa.reverb(x, verb, wet, damp=damp), 60), role)


# ---------------------------------------------------------------- the cast key and the HUD


def tap_release(v, rng):
    """A short snap of air and a soft pop: a tap cast leaves the hand (charged casts have `release`)."""
    dur = 0.22
    air = sa.moving_band(dur, [(0, 700), (0.05, 3200), (dur, 1400)], 1.1, rng) * sa.decay(dur, 0.05, 0.004)
    pop = sa.thump(260 - 30 * v, 90, 0.12, 0.02, drive=1.4)
    x = sa.mix(0.8 * air, 0.5 * pop)
    return sa.finish(sa.highpass(sa.reverb(x, 0.4, 0.08), 90), "cast")


def ready_ping(v, rng):
    """The spell's cooldown is over: two quiet rising glass notes."""
    dur = 0.5
    x = sa.mix(sa.glass(sa.note(A, 1), dur, 0.15), (0.07, 0.8 * sa.glass(sa.note(D, 2), dur, 0.18)))
    return sa.finish(sa.reverb(x, 0.4, 0.1, damp=8000), "ui")


def fizzle(v, rng):
    """A spell that couldn't go off: a dull falling puff (not enough mana, still cooling down)."""
    dur = 0.3
    puff = sa.moving_band(dur, [(0, 1800), (dur, 500)], 0.9, rng) * sa.decay(dur, 0.07, 0.003)
    tone = sa.sine(sa.sweep(sa.note(A, 0), sa.note(D, 0), dur, 0.7), dur) * sa.decay(dur, 0.08, 0.004)
    return _clean(sa.mix(0.7 * puff, 0.35 * tone), "ui")


# ---------------------------------------------------------------- gestures, one per motion


def gesture_flick(v, rng):
    """FLICK: a quick dry snap."""
    dur = 0.14
    snap = sa.norm(sa.bandpass(sa.noise(0.04, rng), 2200, 7000)) * sa.decay(0.04, 0.005, 0.0003)
    ping = sa.glass(sa.note(D, 2), dur, 0.04)
    return _clean(sa.mix(0.8 * snap, 0.35 * ping), "cast", 0.25, 0.05)


def gesture_hurl(v, rng):
    """HURL: a thrown whoosh with a low push."""
    dur = 0.4
    whoosh = sa.moving_band(dur, [(0, 400), (0.08, 2200), (dur, 900)], 1.0, rng) * sa.decay(dur, 0.1, 0.01)
    push = sa.thump(150, 70, 0.2, 0.04, drive=1.3)
    return _clean(sa.mix(0.8 * whoosh, 0.4 * push), "cast")


def gesture_beam(v, rng):
    """BEAM: a bright rising hum that locks on."""
    dur = 0.45
    f = sa.glide(dur, (0, sa.note(D, 0)), (0.12, sa.note(A, 0)), (dur, sa.note(A, 0)))
    hum = sa.lowpass(sa.soft_saw(f, harmonics=8), 2600) * sa.env(dur, (0, 0), (0.03, 1), (0.25, 0.5), (dur, 0))
    zing = sa.glass(sa.note(A, 1), dur, 0.1)
    return _clean(sa.mix(0.45 * sa.norm(hum), 0.3 * zing), "cast")


def gesture_slash(v, rng):
    """SLASH: a blade of air, gliding down."""
    dur = 0.32
    blade = sa.moving_band(dur, [(0, 5000), (dur, 900)], 0.6, rng) * sa.env(dur, (0, 0), (0.04, 1), (dur, 0))
    edge = sa.sine(sa.sweep(sa.note(D, 3), sa.note(A, 1), dur, 0.8), dur) * sa.decay(dur, 0.06, 0.003)
    return _clean(sa.mix(0.9 * blade, 0.15 * edge), "cast")


def gesture_blast(v, rng):
    """BLAST: a gathering in and a thump out."""
    dur = 0.45
    suck = sa.reverse(sa.moving_band(0.18, [(0, 3000), (0.18, 800)], 0.9, rng) * sa.decay(0.18, 0.05, 0.002))
    boom = sa.thump(110, 45, 0.3, 0.07, drive=1.6, knock=0.6)
    crack = sa.norm(sa.bandpass(sa.noise(0.2, rng), 500, 3000)) * sa.decay(0.2, 0.04, 0.001)
    return _clean(sa.mix(0.6 * suck, (0.15, 0.5 * boom), (0.15, 0.7 * crack)), "cast", 0.4, 0.1)


def gesture_seal(v, rng):
    """SEAL: a low swell pressing into the ground."""
    dur = 0.8
    drone = (sa.sine(sa.note(D, 0), dur) + 0.6 * sa.sine(sa.note(A, 0), dur) + 0.4 * sa.sine(sa.note(D, 1), dur)) * sa.swell(dur, 0.4)
    press = sa.norm(sa.bandpass(sa.noise(dur, rng), 300, 1500)) * sa.swell(dur, 0.35)
    return _clean(sa.mix(0.6 * drone, 0.4 * press), "cast", 0.7, 0.15)


def gesture_call(v, rng):
    """CALL: a rising call skyward, a bright chord at the top."""
    dur = 0.7
    rise = sa.moving_band(dur, [(0, 600), (0.5, 4000), (dur, 3000)], 0.8, rng) * sa.swell(dur, 0.5)
    chord = sa.mix((0.35, sa.glass(sa.note(D, 2), 0.35, 0.12)), (0.38, 0.8 * sa.glass(sa.note(FS, 2), 0.32, 0.1)),
                   (0.41, 0.7 * sa.glass(sa.note(A, 2), 0.29, 0.1)))
    return _clean(sa.mix(0.5 * rise, 0.4 * chord), "cast", 0.7, 0.15, 8000)


def gesture_aura(v, rng):
    """AURA: a soft shimmer closing round the caster."""
    dur = 0.6
    shimmer = sa.sparkle(dur, 30, rng, [sa.note(d, 2) for d in (D, E, FS, A, B)], tau=(0.04, 0.1), shape=[(0, 1), (dur, 0.2)])
    hum = sa.sine(sa.note(D, 0), dur) * sa.swell(dur, 0.2)
    return _clean(sa.mix(0.6 * sa.norm(shimmer), 0.3 * hum), "cast", 0.6, 0.15, 8000)


# ---------------------------------------------------------------- notes: the spell's melody


def note_shape(v, rng):
    """A shape's note: a wooden knock at D."""
    dur = 0.3
    x = sa.sine(sa.note(D, 1), dur) * sa.decay(dur, 0.04, 0.001) + 0.4 * sa.sine(sa.note(D, 1) * 2.31, dur) * sa.decay(dur, 0.015, 0.0005)
    return _clean(x, "tell", 0.3, 0.06)


def note_effect(v, rng):
    """An effect's note: a round glass pluck at D."""
    dur = 0.35
    return _clean(sa.glass(sa.note(D, 1), dur, 0.1), "tell", 0.4, 0.1, 8000)


def note_mod(v, rng):
    """A modifier's note: a small gold bell at D."""
    dur = 0.4
    return _clean(sa.bell(sa.note(D, 1), dur, 0.12, 2.0, 1.4), "tell", 0.4, 0.1, 8000)


def note_link(v, rng):
    """A link's note: a chain clink at D."""
    dur = 0.3
    clink = sa.partials(sa.note(D, 1), dur, ((1.0, 1.0, 1.0), (2.9, 0.5, 0.4), (5.1, 0.3, 0.2)), 0.05)
    tick = sa.norm(sa.bandpass(sa.noise(0.02, rng), 3000, 8000)) * sa.decay(0.02, 0.002, 0.0002)
    return _clean(sa.mix(clink, 0.3 * tick), "tell", 0.3, 0.06)


# ---------------------------------------------------------------- tells


def link_ting(v, rng):
    """A link handing on: one glass note at D. Play at 1.122 / 1.26 / 1.498 to climb the scale."""
    dur = 0.6
    x = sa.glass(sa.note(D, 1), dur, 0.25) + 0.3 * sa.bell(sa.note(D, 2), dur, 0.15, 3.0, 1.0, 0.002)
    return sa.finish(sa.reverb(x, 0.6, 0.15, damp=8000), "tell")


def gate_pass(v, rng):
    """A condition holds: a latch clicks open and a short rising note."""
    dur = 0.3
    click = sa.norm(sa.bandpass(sa.noise(0.02, rng), 1500, 6000)) * sa.decay(0.02, 0.003, 0.0002)
    up = sa.sine(sa.sweep(sa.note(D, 1), sa.note(A, 1), dur, 0.4), dur) * sa.decay(dur, 0.06, 0.003)
    return _clean(sa.mix(0.6 * click, (0.02, 0.4 * up)), "tell")


def gate_fail(v, rng):
    """A condition doesn't hold: a dull wooden tock."""
    dur = 0.2
    x = (sa.sine(sa.note(A, 0), dur) + 0.5 * sa.sine(sa.note(A, 0) * 2.4, dur)) * sa.decay(dur, 0.025, 0.001)
    return _clean(x, "tell", 0.2, 0.04)


def tell_tick(v, rng):
    """A fuse's tick: Delay counting down, Pulse's beats, Rapid, Combo's count."""
    return _clean(sa.tick(1400 + 160 * v, rng), "tell", 0.2, 0.05)


def tell_toll(v, rng):
    """A large tuned bell: Vow's oath, a Totem's beat, On Kill."""
    dur = 1.8
    x = sa.clock_bell(sa.note((D, A)[v], -1), dur, 0.5)
    return _clean(x, "effect", 1.2, 0.2)


def tell_rumble(v, rng):
    """A low roll under the cast: Amplify and Overcharge."""
    dur = 0.8
    roll = sa.brown(dur, rng) * sa.swell(dur, 0.15)
    sub = (sa.sine(sa.note(D, -1), dur) + 0.6 * sa.sine(sa.note(A, -1), dur) + 0.4 * sa.sine(sa.note(D, 0), dur)) * sa.decay(dur, 0.3, 0.02)
    grit = sa.norm(sa.bandpass(sa.noise(dur, rng), 300, 1200)) * sa.swell(dur, 0.15)
    return _clean(sa.mix(0.3 * roll, 0.6 * sub, 0.3 * grit), "effect", 0.4, 0.08, 3000)


def tell_zap(v, rng):
    """A short electric sting: Chain's jumps, Pierce running through."""
    dur = 0.15
    zap = sa.fm(sa.sweep(2000 - 200 * v, 600, dur, 0.6), 1.5, 2.5 * sa.decay(dur, 0.05)) * sa.decay(dur, 0.04, 0.001)
    return _clean(zap, "tell", 0.2, 0.05)


def tell_drip(v, rng):
    """A drop: Blood Price paid, Thirst drinking."""
    x = sa.mix(sa.blip(rng), (0.05, 0.4 * sa.blip(rng)))
    return _clean(x, "tell", 0.3, 0.08, 3000)


def tell_crack(v, rng):
    """A solid crack: Execute finishing, a Wall stopping a projectile."""
    dur = 0.25
    crack = sa.norm(sa.bandpass(sa.noise(dur, rng), 900, 6000)) * sa.decay(dur, 0.02, 0.0004)
    body = sa.thump(240, 120, 0.15, 0.03, drive=1.5)
    return _clean(sa.highpass(sa.mix(0.8 * crack, 0.4 * body), 120), "impact", 0.3, 0.06)


# ---------------------------------------------------------------- fields


def field_pulse(v, rng):
    """One landing of a field (Zone, Wall, Vortex): a soft short beat. Three variants, round robin."""
    dur = 0.16
    beat = sa.highpass(sa.thump(220 + 25 * v, 110, dur, 0.04, drive=1.1, knock=0.5), 180)
    air = sa.moving_band(dur, [(0, 1600), (dur, 700)], 0.8, rng) * sa.decay(dur, 0.03, 0.002)
    return _clean(sa.mix(0.5 * beat, 0.5 * air), "pulse", 0.3, 0.05)


# ---------------------------------------------------------------- casting as a performance


def overchannel_crack(v, rng):
    """An overchannel stage lands: a glassy crack through the circle and a strained tone pulled upward,
    wavering. Charging plays it a step higher for each stage (Feels.step), so the three climb the scale."""
    dur = 0.55
    crack = sa.shatter(rng, count=6 + 2 * v, band=(2600, 7200), spread=0.02, tau=(0.012, 0.045))
    strain_f = sa.sweep(sa.note(A, 0), sa.note(D, 1), dur, 0.5)
    waver = 1 + 0.012 * sa.sine(np.full(sa.samples(dur), 9.0 + v))
    strain = sa.lowpass(sa.soft_saw(strain_f * waver, harmonics=6), 2400) * sa.env(dur, (0, 0), (0.03, 1), (0.3, 0.55), (dur, 0))
    knock = sa.thump(200, 90, 0.18, 0.035, drive=1.3, knock=0.4)
    return _clean(sa.mix(0.65 * crack, 0.35 * sa.norm(strain), 0.35 * knock), "tell", 0.45, 0.12, 8000)


def overchannel_backfire(v, rng):
    """An overchannel held too long tears loose: a breath drawn in, the circle bursting like glass, and a
    dull falling thud as the gathered mana scatters. Never loud enough to read as an explosion."""
    dur = 0.9
    draw = sa.reverse(sa.moving_band(0.25, [(0, 4200), (0.25, 900)], 0.9, rng) * sa.decay(0.25, 0.06, 0.002))
    burst = sa.shatter(rng, count=20, band=(1400, 6000), spread=0.14)
    fall = (sa.sine(sa.sweep(sa.note(A, 0), sa.note(A, -1) * 0.9, dur, 0.7)) + 0.6 * sa.sine(sa.sweep(sa.note(D, 0), sa.note(D, -1) * 0.9, dur, 0.7)))
    fall = fall * sa.decay(dur, 0.25, 0.003)
    thud = sa.thump(130, 50, 0.4, 0.07, drive=1.4)
    return _clean(sa.mix(0.45 * draw, (0.22, 0.75 * burst), (0.22, 0.35 * fall), (0.22, 0.5 * thud)), "effect", 0.9, 0.2)


def beat_release(v, rng):
    """A charge let go on the beat: one clean bright chime, glass and a small bell together at D."""
    dur = 0.6
    x = sa.mix(sa.glass(sa.note(D, 2), dur, 0.16), (0.004, 0.55 * sa.bell(sa.note(A, 2), dur, 0.12, 2.0, 1.2)))
    return _clean(x, "tell", 0.5, 0.14, 9000)


def incant_whisper(v, rng):
    """One syllable of an incantation: a breath shaped like a spoken vowel, and the faintest glass note at D
    under it so it stays in key when played on the rune's own degree. Kept soft: it sits under the note."""
    dur = 0.38
    vowels = ((700, 1150), (450, 1900), (550, 950))
    lo, hi = vowels[v % len(vowels)]
    breath = sa.moving_band(dur, [(0, lo), (0.12, hi), (dur, lo)], 0.55, rng)
    breath = breath * sa.env(dur, (0, 0), (0.05, 1), (0.18, 0.7), (dur, 0))
    tone = 0.18 * sa.glass(sa.note(D, 2), dur, 0.08)
    return _clean(sa.highpass(sa.mix(0.8 * sa.norm(breath), tone), 250), "ui", 0.35, 0.1, 7000)


EVENTS = [
    event("tap_release", tap_release, variants=2, role="cast", subtitle="cast"),
    event("ready_ping", ready_ping, role="ui", subtitle="tell"),
    event("fizzle", fizzle, variants=2, role="ui", subtitle="tell"),
    event("gesture_flick", gesture_flick, variants=2, role="cast", subtitle="cast"),
    event("gesture_hurl", gesture_hurl, variants=2, role="cast", subtitle="cast"),
    event("gesture_beam", gesture_beam, variants=2, role="cast", subtitle="cast"),
    event("gesture_slash", gesture_slash, variants=2, role="cast", subtitle="cast"),
    event("gesture_blast", gesture_blast, variants=2, role="cast", subtitle="cast"),
    event("gesture_seal", gesture_seal, role="cast", subtitle="cast"),
    event("gesture_call", gesture_call, role="cast", subtitle="cast"),
    event("gesture_aura", gesture_aura, role="cast", subtitle="cast"),
    event("note_shape", note_shape, role="tell", subtitle="tell"),
    event("note_effect", note_effect, role="tell", subtitle="tell"),
    event("note_mod", note_mod, role="tell", subtitle="tell"),
    event("note_link", note_link, role="tell", subtitle="tell"),
    event("link_ting", link_ting, role="tell", subtitle="tell"),
    event("gate_pass", gate_pass, role="tell", subtitle="tell"),
    event("gate_fail", gate_fail, role="tell", subtitle="tell"),
    event("tell_tick", tell_tick, variants=3, role="tell", subtitle="tell"),
    event("tell_toll", tell_toll, variants=2, role="effect", subtitle="tell", attenuation=24),
    event("tell_rumble", tell_rumble, role="effect", subtitle="tell"),
    event("tell_zap", tell_zap, variants=3, role="tell", subtitle="tell"),
    event("tell_drip", tell_drip, variants=3, role="tell", subtitle="tell"),
    event("tell_crack", tell_crack, variants=2, role="impact", subtitle="hit"),
    event("field_pulse", field_pulse, variants=3, role="pulse", subtitle="field"),
    event("overchannel_crack", overchannel_crack, variants=2, role="tell", subtitle="tell"),
    event("overchannel_backfire", overchannel_backfire, role="effect", subtitle="hit"),
    event("beat_release", beat_release, role="tell", subtitle="tell"),
    event("incant_whisper", incant_whisper, variants=3, role="ui", subtitle="tell"),
]
