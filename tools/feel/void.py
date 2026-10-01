"""Void's kit sounds. The element's base voice is a reversed toll and an inhale; these are the verbs on top of it,
each with a transient in its first 100 ms (the base cast_void peaks nearly a second late): a hook, a corral, a
snap, a tear, a crack, a shriek, a bite, an unzip.

Names are void_<verb>. Played from cast/VoidFx.java (and the signatures in cast/feel/VoidFeels.java).
"""
import numpy as np

from feel.core import sa, event


def _inhale(dur, lo, hi, rng, peak=0.8):
    return sa.moving_band(dur, [(0, lo), (dur, hi)], 1.0, rng) * sa.env(dur, (0, 0), (dur * peak, 0.8), (dur - 0.01, 1), (dur, 0)) ** 2


def void_anchor_clank(v, rng):
    """A heavy chain link taking the strain: inharmonic metal partials over a low thump."""
    dur = 0.5
    metal = sa.partials(sa.note(sa.D, 0) * (1 + 0.05 * v), dur, ((1.0, 1.0, 1.0), (2.76, 0.5, 0.5), (5.4, 0.3, 0.3), (8.1, 0.15, 0.2)), 0.22, 0.001)
    x = sa.mix(0.8 * metal, 0.7 * sa.thump(130, 60, 0.3, 0.05, 1.5), (0.0, 0.3 * sa.norm(sa.bandpass(sa.noise(0.03, rng), 1500, 6000)) * sa.decay(0.03, 0.005, 0.0003)))
    return sa.finish(sa.reverb(x, 0.4, 0.1), "impact")


def void_blind_gulp(v, rng):
    """Sight closing: a noise burst that a low-pass shuts from 6 kHz to 500 Hz, and a thin tinnitus that fades."""
    dur = 0.9
    burst = sa.noise(0.2, rng) * sa.decay(0.2, 0.05, 0.001)
    closed = np.zeros_like(burst)
    parts = 6
    seg = len(burst) // parts
    for i in range(parts):
        cutoff = 6000 * (500 / 6000) ** (i / (parts - 1))
        closed[i * seg:(i + 1) * seg] = sa.lowpass(burst, cutoff)[i * seg:(i + 1) * seg]
    tinnitus = 0.08 * sa.sine(4900 + 200 * v, dur) * sa.decay(dur, 0.35, 0.05)
    x = sa.mix(sa.norm(closed), (0.05, tinnitus), (0.0, 0.5 * sa.thump(100, 50, 0.25, 0.05, 1.3)))
    return sa.finish(sa.reverb(x, 0.6, 0.15), "effect")


def void_collect_suck(v, rng):
    """A rising band sweep with a soft pop, and a spray of small glass ticks as the things arrive."""
    dur = 0.5
    up = sa.moving_band(dur, [(0, 300), (dur, 2600)], 0.9, rng) * sa.swell(dur, dur * 0.85, 1.5)
    pop = sa.sine(sa.sweep(700, 200, 0.06, 0.5), 0.06) * sa.decay(0.06, 0.015, 0.001)
    notes = [sa.note(d, 2) for d in range(5)]
    arrivals = sa.sparkle(0.35, 26, rng, notes, tau=(0.02, 0.06), rising=True)
    x = sa.mix(0.8 * up, (dur - 0.02, 0.6 * pop), (0.15, 0.3 * arrivals))
    return sa.finish(sa.reverb(x, 0.4, 0.1), "effect")


def void_hex_mark(v, rng):
    """Two detuned glass notes a minor second apart, a slow tremolo and a downward glide: a curse laid."""
    dur = 0.7
    base = sa.note(sa.FS, 1) * (0.5 if v else 1.0)
    a = sa.glass(base, dur, 0.3)
    b = sa.glass(base * 1.0595, dur, 0.3)
    trem = 0.7 + 0.3 * np.sin(2 * np.pi * 7 * sa.timeline(dur))
    glide = 0.15 * sa.sine(sa.sweep(base * 2, base * 0.7, dur, 0.8), dur) * sa.decay(dur, 0.25, 0.005)
    x = sa.mix((a + b) * trem[:len(a)], glide)
    return sa.finish(sa.reverb(x, 0.7, 0.2), "effect")


def void_hex_bite(v, rng):
    """One tick and a low blip: a hexed thing bitten by its hexer's spell."""
    x = sa.mix(sa.tick(2600 - 200 * (v % 3), rng, 0.8), 0.5 * sa.sine(sa.sweep(180, 90, 0.08, 0.5), 0.08) * sa.decay(0.08, 0.02, 0.001))
    return sa.finish(x, "tick")


def void_phantom_form(v, rng):
    """A reversed glass shimmer into a pop: an afterimage stepping out of you."""
    rev = sa.reverse(sa.mix(*[(0.02 * i, sa.glass(sa.note(d, 2), 0.35, 0.1)) for i, d in enumerate((sa.D, sa.FS, sa.A, sa.B))]))
    pop = sa.sine(sa.sweep(800, 200, 0.08, 0.5), 0.08) * sa.decay(0.08, 0.02, 0.001)
    x = sa.mix(0.6 * sa.norm(rev), (len(rev) / sa.SR - 0.02, 0.9 * pop))
    return sa.finish(sa.reverb(x, 0.6, 0.2), "effect")


def void_phantom_tick(v, rng):
    """A dry tick with a dark tail: the decoy's fuse (played faster and faster)."""
    x = sa.mix(sa.tick(1900 + 150 * (v % 3), rng, 0.8), 0.4 * sa.thump(150, 80, 0.12, 0.02, 1.2))
    return sa.finish(x, "impact")


def void_phantom_burst(v, rng):
    """Glass breaking over a low whump: the afterimage goes off."""
    x = sa.mix(sa.shatter(rng, 18, (1500, 6000)), (0.02, 0.9 * sa.thump(105, 45, 0.6, 0.12, 1.8)))
    return sa.finish(sa.reverb(x, 0.9, 0.2), "impact")


def void_shadow_bite(v, rng):
    """A bandpassed 'kchh' with a breath and a sub thump: the dark closes on something."""
    dur = 0.35
    kch = sa.moving_band(dur, [(0, 1500), (dur, 300)], 0.7, rng) * sa.decay(dur, 0.06, 0.002)
    breath = 0.3 * sa.moving_band(dur, [(0, 700), (dur, 500)], 0.9, rng) * sa.env(dur, (0, 0), (0.12, 1), (dur, 0))
    x = sa.mix(kch, breath, 0.8 * sa.thump(75 - 8 * v, 40, 0.3, 0.06, 1.5))
    return sa.finish(sa.reverb(x, 0.5, 0.12), "impact")


def void_banish_dissolve(v, rng):
    """Fine grains sliding from 4 kHz to 1 kHz, then a small chirp at the far end: sent away."""
    dur = 0.7
    g = sa.norm(sa.bandpass(sa.grains(dur, 900, rng, length=(0.0004, 0.002), shape=[(0, 1), (dur, 0.3)]), 1000, 4500))
    chirp = sa.sine(sa.sweep(300, 1200, 0.1, 0.7), 0.1) * sa.decay(0.1, 0.03, 0.002)
    x = sa.mix(0.7 * g, (dur * 0.7, 0.5 * chirp))
    return sa.finish(sa.reverb(x, 0.5, 0.15), "effect")


def void_sonar_ping(v, rng):
    """A pure ping, then three echoes, each softer and lower: the pulse coming back."""
    ping = sa.sine(1200, 0.14) * sa.decay(0.14, 0.04, 0.003)
    parts = [ping]
    t = 0.0
    for i in range(1, 4):
        t += 0.22 + 0.05 * i
        parts.append((t, sa.sine(1200 - 180 * i, 0.14) * sa.decay(0.14, 0.05, 0.003) * (0.55 ** i)))
    return sa.finish(sa.reverb(sa.mix(*parts), 0.8, 0.25), "effect")


def void_grapple_reel(v, rng):
    """A plucked rope twang, then a ratchet of ticks that speeds up, and a soft thud: reeling in."""
    dur = 0.18
    twang = sa.sine(sa.sweep(240, 200, dur, 0.5), dur) * sa.decay(dur, 0.06, 0.001) + 0.4 * sa.sine(sa.sweep(480, 400, dur, 0.5), dur) * sa.decay(dur, 0.04, 0.001)
    parts = [twang]
    t = 0.12
    for i in range(6):
        parts.append((t, sa.tick(1400 + 80 * i, rng, 0.5)))
        t += 0.07 - 0.008 * i
    x = sa.mix(*parts, (t, 0.6 * sa.thump(130, 60, 0.2, 0.04, 1.3)))
    return sa.finish(sa.reverb(x, 0.4, 0.1), "effect")


def void_hush_dome(v, rng):
    """A swell that drops out into near silence over a faint low hum, then one soft chime as it lifts."""
    dur = 1.3
    swell = sa.moving_band(0.5, [(0, 200), (0.5, 900)], 1.0, rng) * sa.swell(0.5, 0.42, 1.0)
    hum = 0.05 * sa.sine(sa.note(sa.D, -2), dur) * sa.env(dur, (0, 0), (0.5, 1), (dur, 0.7))
    chime = sa.glass(sa.note(sa.A, 2), 0.5, 0.2)
    x = sa.mix(swell, hum, (dur - 0.45, 0.35 * chime))
    return sa.finish(sa.reverb(x, 1.0, 0.2), "effect")


def void_portal_drop(v, rng):
    """A reversed whoosh in, a breathy pop, a whoosh out from above and a landing thud: through a portal and down."""
    inn = sa.reverse(sa.moving_band(0.25, [(0, 3000), (0.25, 400)], 1.0, rng) * sa.decay(0.25, 0.1, 0.002))
    out = sa.moving_band(0.3, [(0, 1200), (0.3, 3500)], 0.9, rng) * sa.env(0.3, (0, 0), (0.05, 1), (0.3, 0)) ** 2
    thud = sa.thump(90, 45, 0.4, 0.09, 1.7)
    x = sa.mix(0.7 * inn, (0.25, 0.5 * out), (0.62, 0.9 * thud))
    return sa.finish(sa.reverb(x, 0.6, 0.15), "effect")


def void_hook(v, rng):
    """A thin inhale rising and a dry 'thock': a hook set and pulled."""
    dur = 0.35
    inh = sa.moving_band(dur, [(0, 600), (dur, 3000)], 0.8, rng) * sa.swell(dur, dur * 0.9, 1.2)
    thock = sa.mix(sa.tick(1100, rng, 0.9), 0.8 * sa.thump(140, 70, 0.12, 0.025, 1.3))
    x = sa.mix(0.6 * inh, (dur - 0.03, thock))
    return sa.finish(sa.reverb(x, 0.3, 0.08), "impact")


def void_veil_fade(v, rng):
    """A reversed breath and a soft cloth flutter fading to nothing: someone no longer there."""
    dur = 1.0
    breath = sa.reverse(sa.moving_band(dur, [(0, 900), (dur, 250)], 1.0, rng) * sa.decay(dur, 0.3, 0.02))
    flutter = 0.4 * sa.lowpass(sa.grains(dur, 120, rng, length=(0.002, 0.01), shape=[(0, 1), (dur, 0)]), 1800)
    x = sa.mix(0.6 * breath, flutter)
    return sa.finish(sa.reverb(x, 0.9, 0.25), "effect")


def void_step_tick(v, rng):
    """A quiet tick: the return of a Warp Step coming closer."""
    return sa.finish(sa.mix(sa.tick(2200 + 250 * (v % 3), rng, 0.8), 0.3 * sa.sine(sa.note(sa.A, 1), 0.08) * sa.decay(0.08, 0.03, 0.002)), "tick")


def void_unzip(v, rng):
    """Twenty-odd ratchet ticks whose gaps shrink from 45 to 8 ms over a ripping band: a wall unzipped."""
    parts = []
    t = 0.0
    for i in range(22):
        parts.append((t, sa.tick(3200 - 40 * i, rng, 0.4)))
        t += max(0.008, 0.045 - 0.0018 * i)
    dur = t + 0.1
    rip = sa.norm(sa.bandpass(sa.grains(dur, 1500, rng, length=(0.0005, 0.004), shape=[(0, 0.6), (dur, 1)]), 500, 5000))
    rip = rip[:sa.samples(dur)]
    x = sa.mix(*parts, 0.5 * rip * sa.env(dur, (0, 0.2), (dur * 0.5, 1), (dur, 0.3)))
    return sa.finish(sa.reverb(x, 0.4, 0.1), "effect")


def void_black_fire(v, rng):
    """A lowpassed crackle with a soft heartbeat thump: a black flame burning."""
    dur = 0.45
    crackle = sa.lowpass(sa.grains(dur, 260, rng, shape=[(0, 1), (dur, 0.2)]), 1800)
    beat = sa.thump(85 - 6 * (v % 3), 45, 0.3, 0.07, 1.4)
    x = sa.mix(0.5 * sa.norm(crackle), 0.8 * beat)
    return sa.finish(sa.reverb(x, 0.5, 0.1), "effect")


def void_black_ignite(v, rng):
    """A hollow roar swelling from 150 to 600 Hz: the flame takes."""
    dur = 0.7
    roar = sa.moving_band(dur, [(0, 150), (dur, 600)], 0.9, rng) * sa.swell(dur, 0.4, 1.3)
    x = sa.mix(roar, (0.0, 0.6 * sa.thump(80, 40, 0.5, 0.1, 1.5)), 0.2 * sa.norm(sa.lowpass(sa.grains(dur, 200, rng), 1500)))
    return sa.finish(sa.reverb(x, 0.7, 0.15), "impact")


def void_spark_crit(v, rng):
    """A bass hit and a red crackle, then a low sting: the spark takes."""
    dur = 0.6
    crackle = sa.norm(sa.bandpass(sa.grains(dur, 500, rng, length=(0.0004, 0.003), shape=[(0, 1), (dur, 0)]), 1000, 6000))
    sting = sa.sine(sa.sweep(300, 90, 0.3, 0.6), 0.3) * sa.decay(0.3, 0.1, 0.001)
    x = sa.mix(1.0 * sa.thump(85, 40, 0.5, 0.1, 2.0), 0.6 * crackle, (0.03, 0.3 * sting))
    return sa.finish(sa.reverb(x, 0.6, 0.15), "impact")


def void_spark_plain(v, rng):
    """A small dry 'tk' and a low blip: the spark that did not take."""
    return sa.finish(sa.mix(sa.tick(1500 + 100 * (v % 3), rng, 0.8), 0.4 * sa.thump(120, 70, 0.1, 0.02, 1.2)), "tick")


def void_blink_fizzle(v, rng):
    """A blink's ping cut short with a 'phft': nowhere to go."""
    ping = sa.reverse(sa.glass(sa.note(sa.A, 2), 0.2, 0.06))
    puff = sa.moving_band(0.15, [(0, 2500), (0.15, 600)], 0.9, rng) * sa.decay(0.15, 0.04, 0.001)
    return sa.finish(sa.mix(0.5 * sa.norm(ping), (0.18, 0.5 * puff)), "effect")


def void_chomp(v, rng):
    """Two wet crunches, a gulp, and a rising three-note chime if it fed: devoured."""
    def crunch():
        return sa.norm(sa.lowpass(sa.grains(0.06, 700, rng, length=(0.0006, 0.004)), 1300)[:sa.samples(0.06)]) * sa.decay(0.06, 0.02, 0.001)
    gulp = sa.sine(sa.sweep(200, 90, 0.16, 0.6), 0.16) * sa.decay(0.16, 0.05, 0.003)
    chime = sa.mix(*[(0.07 * i, sa.glass(sa.note(d, 2), 0.4, 0.15)) for i, d in enumerate((sa.D, sa.FS, sa.A))])
    x = sa.mix(crunch(), (0.09, crunch()), (0.2, 0.9 * gulp), (0.4, 0.4 * chime))
    return sa.finish(sa.reverb(x, 0.4, 0.1), "impact")


def void_eclipse_hum(v, rng):
    """A slow beating drone of two detuned saws, low-passed and fading in: the light going."""
    dur = 2.2
    f = sa.note(sa.D, -2)
    saw = sa.soft_saw(f, dur, 8) + sa.soft_saw(f * 1.006, dur, 8)
    x = sa.lowpass(saw, 500) * sa.env(dur, (0, 0), (0.7, 1), (dur, 0.5)) ** 1.5 * (0.75 + 0.25 * np.sin(2 * np.pi * 3 * sa.timeline(dur)))
    return sa.finish(sa.reverb(x, 1.2, 0.25), "grand")


def void_eclipse_lift(v, rng):
    """A bright shimmer as the light comes back."""
    x = sa.mix(*[(0.05 * i, sa.glass(sa.note(d, 2 + (i > 3)), 0.5, 0.18)) for i, d in enumerate((sa.D, sa.E, sa.FS, sa.A, sa.B))])
    return sa.finish(sa.reverb(x, 0.8, 0.25), "effect")


def void_entropy_tick(v, rng):
    """A dry tick that rises a step each variant: one more wound of the ramp."""
    ratio = (1.0, 1.122, 1.26, 1.498, 1.682, 2.0)[v % 6]
    x = sa.mix(sa.tick(1700 * ratio, rng, 0.9), 0.3 * sa.sine(sa.note(sa.D, 1) * ratio, 0.1) * sa.decay(0.1, 0.03, 0.002))
    return sa.finish(sa.reverb(x, 0.3, 0.08), "impact")


def void_corral(v, rng):
    """A bed of inward-sweeping band noise over a D1 drone with a slow tremolo: something pulling for a while."""
    dur = 2.0
    inward = sa.moving_band(dur, [(0, 1400), (dur, 300)], 1.2, rng) * sa.env(dur, (0, 0), (0.4, 0.8), (dur, 1.0)) ** 1.3
    drone = 0.5 * sa.sine(sa.note(sa.D, -3), dur) * (0.8 + 0.2 * np.sin(2 * np.pi * 2.5 * sa.timeline(dur)))
    x = sa.mix(0.7 * inward, drone * sa.env(dur, (0, 0), (0.3, 1), (dur, 0.8)))
    return sa.finish(sa.reverb(x, 1.0, 0.2), "effect")


def void_snap(v, rng):
    """A reversed toll into a hard crack and a low whump: the pull lets go."""
    pre = 0.28
    toll = sa.bell(sa.note(sa.D, -2), 0.6, 0.25, 1.41, 3.0, attack=0.002)
    rev = sa.norm(sa.reverse(sa.reverb(toll, 0.5, 0.5, damp=1500))[-sa.samples(pre):])
    crack = sa.norm(sa.bandpass(sa.noise(0.03, rng), 500, 7000)) * sa.decay(0.03, 0.004, 0.0002)
    whump = sa.thump(115 - 15 * (v % 2), 38, 0.7, 0.14, 2.0)
    x = sa.mix(0.6 * rev, (pre, 0.9 * crack), (pre, 0.9 * whump), (pre, 0.15 * sa.norm(sa.lowpass(sa.noise(0.6, rng), 450)) * sa.decay(0.6, 0.15, 0.01)))
    return sa.finish(sa.reverb(x, 0.9, 0.22, damp=1800), "impact")


def void_curse_pass(v, rng):
    """A descending glass arpeggio panned across three notes: a curse handed on."""
    x = sa.mix(*[(0.09 * i, sa.glass(sa.note(d, 1), 0.45, 0.16)) for i, d in enumerate((sa.B, sa.A, sa.FS))])
    return sa.finish(sa.reverb(x, 0.7, 0.2), "effect")


def void_shriek(v, rng):
    """An FM scream with a vibrato and a bone crack: the sculk answering."""
    dur = 0.9
    carrier = sa.sweep(1250, 1550, dur, 1.0) * (1 + 0.02 * np.sin(2 * np.pi * 40 * sa.timeline(dur)))
    scream = sa.fm(carrier, 1.5, sa.env(dur, (0, 5), (dur, 1.5)), dur) * sa.env(dur, (0, 0), (0.06, 1), (0.6, 0.7), (dur, 0)) ** 1.3
    crack = sa.norm(sa.bandpass(sa.noise(0.04, rng), 900, 6500)) * sa.decay(0.04, 0.006, 0.0003)
    x = sa.mix(0.7 * scream, 0.7 * crack, 0.4 * sa.thump(100, 50, 0.3, 0.06, 1.4))
    return sa.finish(sa.reverb(x, 0.9, 0.22), "impact")


def void_shriek_echo(v, rng):
    """The shriek again, lowpassed and thinner: an echo a second late."""
    dur = 0.7
    carrier = sa.sweep(1000, 1200, dur, 1.0)
    scream = sa.fm(carrier, 1.5, 3.0, dur) * sa.env(dur, (0, 0), (0.05, 1), (dur, 0)) ** 1.5
    return sa.finish(sa.reverb(sa.lowpass(scream, 1800) * 0.7, 1.2, 0.4), "effect")


def void_riftcall_open(v, rng):
    """A tearing rip of grains rising over a D1 drone: the air torn."""
    dur = 0.8
    rip = sa.norm(sa.bandpass(sa.grains(dur, 1300, rng, length=(0.001, 0.006), shape=[(0, 0.3), (0.35, 1), (dur, 0.2)]), 400, 4500))
    drone = 0.5 * sa.sine(sa.note(sa.D, -3), dur) * sa.env(dur, (0, 0), (0.3, 1), (dur, 0.5))
    x = sa.mix(0.7 * rip, drone, (0.0, 0.5 * sa.thump(90, 45, 0.4, 0.08, 1.4)))
    return sa.finish(sa.reverb(x, 1.0, 0.25, damp=4500), "effect")


def void_hound_growl(v, rng):
    """A low saw through a moving formant with a chatter: something hunting."""
    dur = 0.7
    f = 95 - 10 * (v % 2)
    saw = sa.soft_saw(sa.sweep(f, f * 0.75, dur, 1.0), dur, 10)
    formant = sa.lowpass(saw, 700) * sa.env(dur, (0, 0), (0.1, 1), (0.5, 0.8), (dur, 0)) * (0.6 + 0.4 * sa.chopper(dur, rng, (18, 40), 0.3)[:sa.samples(dur)])
    x = sa.mix(formant, 0.4 * sa.moving_band(dur, [(0, 500), (dur, 900)], 0.9, rng) * sa.env(dur, (0, 0), (0.15, 1), (dur, 0)))
    return sa.finish(sa.reverb(x, 0.5, 0.1), "effect")


def void_shadow_cut(v, rng):
    """A reversed whisper and a sharp 'shk': stepping out of the dark."""
    whisper = sa.reverse(sa.moving_band(0.2, [(0, 3000), (0.2, 8000)], 0.6, rng) * sa.decay(0.2, 0.08, 0.002))
    shk = sa.norm(sa.highpass(sa.noise(0.03, rng), 3000)) * sa.decay(0.03, 0.005, 0.0002)
    return sa.finish(sa.mix(0.5 * whisper, (0.2, 0.9 * shk), (0.2, 0.3 * sa.thump(110, 60, 0.15, 0.03, 1.3))), "effect")


def void_shell_close(v, rng):
    """A heavy stone-and-wood slam: the shell shuts."""
    x = sa.mix(sa.thump(100, 50, 0.4, 0.09, 1.7), 0.7 * sa.tick(800, rng, 1.0), 0.4 * sa.norm(sa.lowpass(sa.grains(0.2, 300, rng), 1500)))
    return sa.finish(sa.reverb(x, 0.5, 0.15), "impact")


def void_shell_open(v, rng):
    """A dull gong and three light pings sent away: the shell opens and looses its bullets."""
    gong = sa.bell(sa.note(sa.D, -1), 0.9, 0.5, 1.41, 2.5)
    pings = [(0.05 + 0.07 * i, 0.45 * sa.glass(f, 0.3, 0.08)) for i, f in enumerate((2000, 2400, 2800))]
    return sa.finish(sa.reverb(sa.mix(gong, *pings), 0.8, 0.2), "impact")


def void_warp_ping(v, rng):
    """Two glass pings crossing in pitch, one rising and one falling: places swapped."""
    dur = 0.45
    up = sa.sine(sa.sweep(sa.note(sa.A, 1), sa.note(sa.A, 2), dur, 0.8), dur) * sa.decay(dur, 0.14, 0.002)
    down = sa.sine(sa.sweep(sa.note(sa.E, 2), sa.note(sa.E, 1), dur, 0.8), dur) * sa.decay(dur, 0.14, 0.002)
    spark = sa.sparkle(0.4, 30, rng, [sa.note(d, 2) for d in range(5)], tau=(0.03, 0.08))
    return sa.finish(sa.reverb(sa.mix(0.5 * up, 0.5 * down, (0.1, 0.25 * spark)), 0.5, 0.18), "effect")


def void_breath_roar(v, rng):
    """A quiet roar band with a slow breathing and a little crackle: a breath rolling on."""
    dur = 2.4
    roar = sa.moving_band(dur, [(0, 250), (dur * 0.5, 700), (dur, 300)], 1.0, rng) * (0.6 + 0.4 * np.sin(2 * np.pi * 0.9 * sa.timeline(dur))) * sa.env(dur, (0, 0), (0.3, 1), (dur, 0)) ** 1.2
    crackle = 0.2 * sa.lowpass(sa.grains(dur, 60, rng), 1400)
    return sa.finish(sa.reverb(sa.mix(roar, crackle[:len(roar)]), 0.9, 0.2), "effect")


def void_zeno_beat(v, rng):
    """A two-note thump over a glassy hum: a heartbeat in a bubble where time thickens."""
    beat = sa.mix(sa.thump(58, 40, 0.3, 0.07, 1.4), (0.22, 0.7 * sa.thump(52, 38, 0.3, 0.07, 1.4)))
    hum = 0.06 * sa.mix(sa.glass(sa.note(sa.A, 1), 0.9, 0.5), sa.glass(sa.note(sa.D, 2), 0.9, 0.5))
    return sa.finish(sa.reverb(sa.mix(beat, hum), 0.8, 0.2), "effect")


def void_sonic_crack(v, rng):
    """A crack, a whine falling from 3.5 kHz to 400 Hz, a short ring and a long tail: a boom that pierces."""
    dur = 0.6
    crack = sa.norm(sa.noise(0.02, rng)) * sa.decay(0.02, 0.003, 0.0001)
    whine = sa.sine(sa.sweep(3500, 400, 0.35, 0.5), 0.35) * sa.decay(0.35, 0.12, 0.001)
    ring = sa.glass(1800, 0.5, 0.2)
    x = sa.mix(0.9 * crack, 0.4 * whine, (0.02, 0.3 * ring), (0.0, 0.9 * sa.thump(95, 42, 0.5, 0.1, 1.9)))
    return sa.finish(sa.reverb(x, 1.0, 0.3), "impact")


def void_starmaw_gulp(v, rng):
    """An inhale, a reversed glass-shard swallow and a low thump: the light devoured."""
    inh = _inhale(0.4, 500, 2500, rng)
    swallow = sa.reverse(sa.shatter(rng, 14, (1800, 5500)))
    x = sa.mix(0.6 * inh, (0.2, 0.5 * sa.norm(swallow)), (0.5, 0.9 * sa.thump(80, 38, 0.5, 0.1, 1.8)))
    return sa.finish(sa.reverb(x, 0.8, 0.2), "impact")


def void_wither_rot(v, rng):
    """Slow creaks, a wet lowpassed crackle and a fading moan: rot taking hold."""
    dur = 1.0
    creak = sa.soft_saw(sa.sweep(120, 85, dur, 1.0), dur, 8) * sa.chopper(dur, rng, (8, 22), 0.2)[:sa.samples(dur)]
    wet = sa.lowpass(sa.grains(dur, 180, rng, shape=[(0, 1), (dur, 0.3)]), 800)
    moan = 0.3 * sa.sine(sa.sweep(200, 110, dur, 1.0), dur) * sa.env(dur, (0, 0), (0.2, 1), (dur, 0))
    x = sa.mix(0.5 * sa.lowpass(creak, 600), 0.5 * sa.norm(wet), moan)
    return sa.finish(sa.reverb(x, 0.7, 0.15), "effect")


def void_step_leap(v, rng):
    """A short rising band and a glass ping at the end: the leap of a Warp Step (a there-and-back, so it says 'there')."""
    dur = 0.22
    up = sa.moving_band(dur, [(0, 400), (dur, 2600)], 0.8, rng) * sa.swell(dur, dur * 0.85, 1.4)
    ping = sa.glass(sa.note(sa.A, 2), 0.35, 0.14)
    x = sa.mix(0.8 * up, (dur - 0.02, 0.5 * ping), (dur - 0.02, 0.5 * sa.thump(160, 80, 0.12, 0.03, 1.3)))
    return sa.finish(sa.reverb(x, 0.4, 0.12), "effect")


def void_step_snap(v, rng):
    """The leap reversed and a dry tick: pulled back where you started."""
    dur = 0.22
    down = sa.moving_band(dur, [(0, 2600), (dur, 400)], 0.8, rng) * sa.env(dur, (0, 0), (0.03, 1), (dur, 0.2))
    ping = sa.reverse(sa.glass(sa.note(sa.A, 2), 0.25, 0.1))
    x = sa.mix(0.6 * ping, (0.2, 0.8 * down), (0.2 + dur, 0.7 * sa.tick(1400, rng, 0.9)))
    return sa.finish(sa.reverb(x, 0.4, 0.1), "effect")


def void_cue(v, rng):
    """The first 80 ms of any void spell: a low dark knock and a breath of air drawn in, so the cast is heard the moment the hand moves
    (the element's own cast swell peaks nearly a second later). Played at a different pitch for each rune."""
    dur = 0.12
    knock = sa.thump(150 - 15 * v, 70, dur, 0.03, 1.4)
    breath = 0.5 * sa.moving_band(dur, [(0, 500), (dur, 2200)], 0.9, rng) * sa.env(dur, (0, 0), (0.05, 1), (dur, 0))
    return sa.finish(sa.reverb(sa.mix(knock, breath), 0.25, 0.08), "cast")


EVENTS = [
    event("void_cue", void_cue, variants=3, role="cast", subtitle="cast"),
    event("void_step_leap", void_step_leap, variants=2, role="effect", subtitle="cast"),
    event("void_step_snap", void_step_snap, variants=2, role="effect", subtitle="cast"),
    event("void_anchor_clank", void_anchor_clank, variants=3, role="impact", subtitle="hit"),
    event("void_blind_gulp", void_blind_gulp, variants=2, role="effect", subtitle="cast"),
    event("void_collect_suck", void_collect_suck, variants=1, role="effect", subtitle="cast"),
    event("void_hex_mark", void_hex_mark, variants=2, role="effect", subtitle="cast"),
    event("void_hex_bite", void_hex_bite, variants=3, role="tick", subtitle="tell"),
    event("void_phantom_form", void_phantom_form, variants=1, role="effect", subtitle="cast"),
    event("void_phantom_tick", void_phantom_tick, variants=3, role="impact", subtitle="tell"),
    event("void_phantom_burst", void_phantom_burst, variants=2, role="impact", subtitle="hit"),
    event("void_shadow_bite", void_shadow_bite, variants=3, role="impact", subtitle="hit"),
    event("void_banish_dissolve", void_banish_dissolve, variants=1, role="effect", subtitle="hit"),
    event("void_sonar_ping", void_sonar_ping, variants=2, role="effect", subtitle="field", attenuation=32),
    event("void_grapple_reel", void_grapple_reel, variants=1, role="effect", subtitle="cast"),
    event("void_hush_dome", void_hush_dome, variants=1, role="effect", subtitle="field"),
    event("void_portal_drop", void_portal_drop, variants=1, role="effect", subtitle="hit"),
    event("void_hook", void_hook, variants=2, role="impact", subtitle="hit"),
    event("void_veil_fade", void_veil_fade, variants=1, role="effect", subtitle="cast"),
    event("void_step_tick", void_step_tick, variants=3, role="tick", subtitle="tell"),
    event("void_unzip", void_unzip, variants=1, role="effect", subtitle="cast"),
    event("void_black_fire", void_black_fire, variants=3, role="effect", subtitle="field"),
    event("void_black_ignite", void_black_ignite, variants=1, role="impact", subtitle="hit"),
    event("void_spark_crit", void_spark_crit, variants=2, role="impact", subtitle="hit"),
    event("void_spark_plain", void_spark_plain, variants=3, role="tick", subtitle="hit"),
    event("void_blink_fizzle", void_blink_fizzle, variants=1, role="effect", subtitle="cast"),
    event("void_chomp", void_chomp, variants=2, role="impact", subtitle="hit"),
    event("void_eclipse_hum", void_eclipse_hum, variants=1, role="grand", subtitle="field", attenuation=32),
    event("void_eclipse_lift", void_eclipse_lift, variants=1, role="effect", subtitle="field"),
    event("void_entropy_tick", void_entropy_tick, variants=6, role="impact", subtitle="tell"),
    event("void_corral", void_corral, variants=1, role="effect", subtitle="field", attenuation=32),
    event("void_snap", void_snap, variants=2, role="impact", subtitle="hit", attenuation=32),
    event("void_curse_pass", void_curse_pass, variants=1, role="effect", subtitle="hit"),
    event("void_shriek", void_shriek, variants=2, role="impact", subtitle="hit"),
    event("void_shriek_echo", void_shriek_echo, variants=1, role="effect", subtitle="hit"),
    event("void_riftcall_open", void_riftcall_open, variants=1, role="effect", subtitle="field", attenuation=32),
    event("void_hound_growl", void_hound_growl, variants=2, role="effect", subtitle="cast"),
    event("void_shadow_cut", void_shadow_cut, variants=1, role="effect", subtitle="cast"),
    event("void_shell_close", void_shell_close, variants=1, role="impact", subtitle="hit"),
    event("void_shell_open", void_shell_open, variants=1, role="impact", subtitle="hit"),
    event("void_warp_ping", void_warp_ping, variants=1, role="effect", subtitle="cast"),
    event("void_breath_roar", void_breath_roar, variants=1, role="effect", subtitle="field"),
    event("void_zeno_beat", void_zeno_beat, variants=2, role="effect", subtitle="field"),
    event("void_sonic_crack", void_sonic_crack, variants=2, role="impact", subtitle="hit", attenuation=32),
    event("void_starmaw_gulp", void_starmaw_gulp, variants=1, role="impact", subtitle="hit"),
    event("void_wither_rot", void_wither_rot, variants=2, role="effect", subtitle="hit"),
]


# ---------------------------------------------------------------- a world's resonances: the twists' voices (cast/TwistVfx)

def void_upfall(v, rng):
    """Upfall: a whoosh played backwards, a tone rising under it, the ground's rumble letting go."""
    dur = 1.6
    whoosh = sa.reverse(sa.moving_band(dur, [(0, 2400), (dur, 300)], 1.2, rng) * sa.decay(dur, 0.5, 0.01))
    rise = sa.sine(sa.glide(dur, (0, sa.note(sa.D, -1)), (dur, sa.note(sa.D, 1)))) * sa.swell(dur, 1.2) * 0.5
    under = sa.norm(sa.lowpass(sa.brown(dur, rng), 200)) * sa.swell(dur, 0.8) * 0.4
    return sa.finish(sa.reverb(sa.mix(0.8 * whoosh, rise, under), 1.0, 0.2), "effect")


def void_soul_wisp(v, rng):
    """Soul Lanterns: a breathy, hollow tone drifting upward, and a far chime as it arrives."""
    dur = 1.4
    breath = sa.moving_band(dur, [(0, 700), (dur, 1400)], 0.15, rng) * sa.swell(dur, 0.5) * 0.7
    hollow = sa.sine(sa.glide(dur, (0, sa.note(sa.FS, 0)), (dur, sa.note(sa.A, 0)))) * sa.swell(dur, 0.6) * 0.4
    chime = sa.glass(sa.note(sa.B, 2), 0.8, 0.2) * 0.4
    return sa.finish(sa.reverb(sa.mix(breath, hollow, (0.6, chime)), 1.0, 0.25), "effect")


EVENTS += [
    event("void_upfall", void_upfall, variants=1, role="effect", subtitle="field"),
    event("void_soul_wisp", void_soul_wisp, variants=2, role="effect", subtitle="hit"),
]
# ---------------------------------------------------------------- residues: what's left behind


def void_hum(v, rng):
    """A void scar: a low hum, slowly beating, and air being drawn in towards it."""
    dur = 1.8
    f = sa.note(sa.D, -1)
    hum = sa.mix(sa.soft_saw(f, dur, harmonics=8, limit=1600) * 0.5, sa.sine(f * 1.007, dur) * 0.5, sa.sine(f * 2.0, dur) * 0.25)
    hum = sa.lowpass(hum, 900) * sa.swell(dur, 0.5, 1.5)
    draw = sa.moving_band(dur, [(0, 2200), (dur, 500)], 0.7, rng) * sa.env(dur, (0, 0), (0.8, 0.35), (dur, 0))
    x = sa.mix(hum, draw)
    return sa.finish(sa.reverb(x, 1.0, 0.2), "tell", fade_in=0.2, fade_out=0.4)


EVENTS += [event("void_hum", void_hum, variants=2, role="tell", subtitle="field")]
