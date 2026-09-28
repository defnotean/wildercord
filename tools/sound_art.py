"""Wildercord's sounds, synthesised from nothing: every cast, impact, charge, circle and click.

Nothing is recorded or borrowed. Each sound is built in code from a few small pieces (envelopes,
FM bells, struck glass, filtered noise, pitch sweeps, a chorus and a synthetic reverb) and written
as mono Ogg Vorbis, so the whole palette can be retuned and rebuilt at will. Every random choice
comes from a seed fixed per sound, so a run always writes the same files.

Everything tonal sits in one scale, D major pentatonic (D E F# A B). Any of its notes sound well
together, so sounds that play at once (a cast, its magic circle, its impact, the charge chime)
always harmonise. Play them at pitch 1 (a little random spread is fine) and they stay in key.

    sounds/cast/<element>_<n>.ogg      two per element: the moment a spell leaves the hand
    sounds/impact/<element>_<n>.ogg    three per element: where it lands
    sounds/casting/*.ogg               charging, circles, beams, orbs, shields, domains, blinks
    sounds/ui/*.ogg                    the Cord screen, the spell wheel and the Grimoire
    sounds/heart/*.ogg                 a Heart Circle forming, and one cracking
    sounds/boss/*.ogg                  the dungeon bosses: their cries, blows and phases, and the tide
    sounds.json                        every event, its variants and its subtitle key

The subtitles' English text lives in generate_assets.py (NEW_LANG), like the rest of en_us.json.
Needs numpy, scipy and ffmpeg (built with libvorbis) on the PATH.
Run from the project root:  python tools/sound_art.py
"""
import json
import subprocess
import tempfile
import zlib
from pathlib import Path

import numpy as np
from scipy import signal
from scipy.io import wavfile

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/wildercord"
SOUNDS = ASSETS / "sounds"
SR = 44100

ELEMENTS = ("fire", "frost", "storm", "wind", "earth", "life", "void", "arcane", "time", "blood")

# ---------------------------------------------------------------- the shared scale

ROOT_HZ = 293.66  # D4
PENTATONIC = (0, 2, 4, 7, 9)
D, E, FS, A, B = range(5)


def note(degree, octave=0):
    """A note of the shared scale: degree 0-4 is D E F# A B, and `octave` counts up (or down) from D4."""
    octave += degree // 5
    return ROOT_HZ * 2 ** (octave + PENTATONIC[degree % 5] / 12)


# ---------------------------------------------------------------- time and mixing


def samples(seconds):
    return int(round(seconds * SR))


def timeline(seconds):
    return np.arange(samples(seconds)) / SR


def mix(*layers):
    """Sums layers into one sound. Each layer is an array, or (start in seconds, array)."""
    placed = []
    for layer in layers:
        start, x = layer if isinstance(layer, tuple) else (0.0, layer)
        placed.append((samples(start), np.asarray(x, dtype=float)))
    out = np.zeros(max(i + len(x) for i, x in placed))
    for i, x in placed:
        out[i:i + len(x)] += x
    return out


def norm(x, peak=1.0):
    """Scales a sound so its loudest sample is `peak`."""
    top = np.max(np.abs(x)) if len(x) else 0.0
    return x * (peak / top) if top > 0 else x


def reverse(x):
    return np.asarray(x, dtype=float)[::-1].copy()


def curve(spec, times, log=False):
    """A value over time: a number, or (time, value) points joined up (evenly in pitch when `log`)."""
    times = np.asarray(times, dtype=float)
    if np.isscalar(spec):
        return np.full(times.shape, float(spec))
    ts, values = zip(*spec)
    values = np.asarray(values, dtype=float)
    if log:
        return 2 ** np.interp(times, ts, np.log2(values))
    return np.interp(times, ts, values)


# ---------------------------------------------------------------- envelopes


def env(seconds, *points):
    """A piecewise-linear envelope through (time, level) points, held at the last level."""
    return curve(list(points), timeline(seconds))


def decay(seconds, tau, attack=0.003):
    """A soft attack (a quarter sine, so it never clicks), then an exponential fall with time constant
    `tau`. The last 30% eases down to silence, so a tone cut short still ends without a click."""
    t = timeline(seconds)
    rise = np.clip(t / attack, 0, 1) if attack > 0 else np.ones_like(t)
    end = np.clip((seconds - t) / (0.3 * seconds), 0, 1)
    return np.sin(rise * np.pi / 2) ** 2 * np.exp(-np.maximum(t - attack, 0) / tau) * np.sin(end * np.pi / 2) ** 2


def swell(seconds, peak, power=2.0):
    """Rises smoothly to full at `peak` seconds, then falls away to nothing at the end."""
    t = timeline(seconds)
    up = np.clip(t / peak, 0, 1)
    down = np.clip((seconds - t) / max(seconds - peak, 1e-6), 0, 1)
    return np.sin(up * np.pi / 2) ** 2 * down ** power


# ---------------------------------------------------------------- oscillators


def phase(freq, n):
    """The running phase of a frequency that may change every sample, starting at zero."""
    f = np.broadcast_to(np.asarray(freq, dtype=float), (n,))
    return 2 * np.pi * np.concatenate(([0.0], np.cumsum(f[:-1]))) / SR


def length_of(seconds, *signals):
    for s in signals:
        if np.ndim(s):
            return len(s)
    return samples(seconds)


def sine(freq, seconds=None):
    return np.sin(phase(freq, length_of(seconds, freq)))


def sweep(f0, f1, seconds, ease=1.0):
    """A frequency moving from f0 to f1, evenly in pitch; `ease` below 1 rushes the start, above 1 the end."""
    u = np.linspace(0, 1, samples(seconds)) ** ease
    return f0 * (f1 / f0) ** u


def glide(seconds, *points):
    """A frequency path through (time, Hz) points, moving evenly in pitch."""
    return curve(list(points), timeline(seconds), log=True)


def soft_saw(freq, seconds=None, harmonics=12, limit=9000.0):
    """A sawtooth built from its first few harmonics, fading out any that would climb past `limit`:
    buzzy, but never fizzy."""
    n = length_of(seconds, freq)
    f = np.broadcast_to(np.asarray(freq, dtype=float), (n,))
    ph = phase(f, n)
    out = np.zeros(n)
    for k in range(1, harmonics + 1):
        out += np.clip((limit - k * f) / (0.25 * limit), 0, 1) * np.sin(k * ph) / k
    return out


def fm(carrier, ratio, index, seconds=None):
    """Two-operator FM: a sine whose phase is pushed around by another at `ratio` times its frequency.
    `index` (how hard it is pushed, so how bright it sounds) may be an envelope."""
    n = length_of(seconds, carrier, index)
    modulator = np.sin(phase(np.asarray(carrier, dtype=float) * ratio, n))
    return np.sin(phase(carrier, n) + np.asarray(index) * modulator)


def bell(freq, seconds, tau, ratio=2.0, brightness=1.5, attack=0.004):
    """An FM bell: bright at the strike, mellowing as its modulation dies away faster than its tone.
    A whole-number `ratio` keeps it in tune; 1.41 makes a dark gong."""
    amp = decay(seconds, tau, attack)
    index = brightness * decay(seconds, tau * 0.3, attack)
    return fm(freq, ratio, index, seconds) * amp


def partials(freq, seconds, spec, tau, attack=0.002):
    """Additive synthesis: sines at (ratio, level, life) for each partial, each dying at its own rate."""
    t = timeline(seconds)
    out = np.zeros(len(t))
    for ratio, level, life in spec:
        if freq * ratio < 12000:
            out += level * np.sin(2 * np.pi * freq * ratio * t) * decay(seconds, tau * life, attack)
    return out


# A struck crystal bar: its inharmonic partials, the high ones dying first.
GLASS = ((1.0, 1.0, 1.0), (2.756, 0.4, 0.5), (5.404, 0.18, 0.28), (8.933, 0.08, 0.16))
# A small clock bell: a hum an octave down, a fifth, and slightly stretched upper partials; the
# doubled fundamental beats slowly against itself, the way real bells shimmer.
CLOCK = ((0.5, 0.25, 1.4), (1.0, 1.0, 1.0), (1.0025, 0.3, 0.9), (1.5, 0.18, 0.5), (2.0, 0.45, 0.55),
         (3.0, 0.2, 0.3), (4.07, 0.12, 0.2), (5.2, 0.06, 0.12))


def glass(freq, seconds, tau, attack=0.002):
    return partials(freq, seconds, GLASS, tau, attack)


def clock_bell(freq, seconds, tau, attack=0.003):
    return partials(freq, seconds, CLOCK, tau, attack)


# ---------------------------------------------------------------- noise and filters


def noise(seconds, rng):
    return rng.standard_normal(samples(seconds))


def brown(seconds, rng):
    """Deep, rumbling noise: white noise summed up, with its drift taken out."""
    return norm(highpass(np.cumsum(noise(seconds, rng)), 20))


def lowpass(x, hz, order=2):
    return signal.sosfilt(signal.butter(order, hz, "low", fs=SR, output="sos"), x)


def highpass(x, hz, order=2):
    return signal.sosfilt(signal.butter(order, hz, "high", fs=SR, output="sos"), x)


def bandpass(x, lo, hi, order=2):
    return signal.sosfilt(signal.butter(order, [lo, hi], "band", fs=SR, output="sos"), x)


def moving_band(seconds, centre, width, rng):
    """Noise through a band that moves: `centre` (Hz) and `width` (octaves) are numbers or
    (time, value) points. The heart of every whoosh; a narrow band becomes a breathy whistle."""
    n = samples(seconds)
    f, frames, spec = signal.stft(rng.standard_normal(n + 1024), SR, nperseg=1024, noverlap=768)
    c = curve(centre, frames, log=True)
    w = curve(width, frames)
    octaves = np.log2(np.maximum(f, 1.0))[:, None] - np.log2(c)[None, :]
    spec *= np.exp(-0.5 * (octaves / w[None, :]) ** 2)
    _, y = signal.istft(spec, SR, nperseg=1024, noverlap=768)
    return norm(y[:n])


def loop_noise(seconds, lo, hi, rng):
    """Band-limited noise that repeats exactly every `seconds`, for loops: built straight from a spectrum."""
    n = samples(seconds)
    freqs = np.fft.rfftfreq(n, 1 / SR)
    octaves = np.log2(np.maximum(freqs, 1.0) / np.sqrt(lo * hi))
    spectrum = np.exp(-0.5 * (octaves / (np.log2(hi / lo) / 2)) ** 2) * np.exp(2j * np.pi * rng.uniform(0, 1, len(freqs)))
    spectrum[0] = 0
    return norm(np.fft.irfft(spectrum, n))


def jitter(seconds, rng, hz):
    """A smooth random wander between -1 and 1, changing about `hz` times a second."""
    return norm(lowpass(noise(seconds, rng), hz))


def chopper(seconds, rng, rate=(50, 160), floor=0.15):
    """A gate that jumps between random levels (and sometimes off) at random moments: what makes a
    steady tone crackle like an electric arc. Its edges are rounded off so it never clicks."""
    n = samples(seconds)
    out = np.zeros(n)
    i = 0
    while i < n:
        length = max(1, samples(1 / rng.uniform(*rate)))
        out[i:i + length] = rng.uniform(floor, 1) if rng.uniform() < 0.8 else 0.0
        i += length
    return np.clip(lowpass(out, 400), 0, None)


# ---------------------------------------------------------------- grains: crackle, grit and glitter


def moments(seconds, rate, rng, shape=None):
    """Random moments, `rate` a second on average; `shape` ((time, 0-1) points) thins them out over time."""
    times = np.sort(rng.uniform(0, seconds, rng.poisson(rate * seconds)))
    if shape is not None:
        times = times[rng.uniform(0, 1, len(times)) < curve(shape, times)]
    return times


def grains(seconds, rate, rng, length=(0.0008, 0.004), shape=None):
    """Tiny bursts of noise at random moments, mostly faint with the odd loud one: embers popping,
    sparks, gravel. Filter the result to give it its colour."""
    out = np.zeros(samples(seconds) + samples(length[1] * 6) + 1)
    for t0 in moments(seconds, rate, rng, shape):
        life = rng.uniform(*length)
        n = max(2, samples(life * 6))
        grain = rng.standard_normal(n) * np.exp(-np.arange(n) / SR / life)
        i = samples(t0)
        out[i:i + n] += grain * min(3.0, rng.pareto(3.0) + 0.3)
    return out


def sparkle(seconds, rate, rng, notes, tau=(0.06, 0.2), shape=None, rising=False):
    """Soft glass pings at random moments, each on a note of the scale: the glitter over a spell.
    With `rising`, later pings take higher notes."""
    notes = sorted(notes)
    out = np.zeros(samples(seconds + tau[1] * 7))
    for t0 in moments(seconds, rate, rng, shape):
        if rising:
            k = min(max(int(t0 / seconds * len(notes)) + int(rng.integers(-1, 2)), 0), len(notes) - 1)
        else:
            k = int(rng.integers(len(notes)))
        life = rng.uniform(*tau)
        ping = glass(notes[k], life * 7, life) * rng.uniform(0.35, 1.0)
        i = samples(t0)
        out[i:i + len(ping)] += ping[:len(out) - i]
    return out


def shatter(rng, count=22, band=(2000, 6500), spread=0.15, tau=(0.03, 0.14)):
    """Glass breaking: a sharp crack, then shards ringing out at random pitches, thickest at the start."""
    shards = []
    for _ in range(count):
        t0 = min(spread, rng.exponential(spread / 3))
        life = rng.uniform(*tau)
        freq = np.exp(rng.uniform(np.log(band[0]), np.log(band[1])))
        shards.append((t0, glass(freq, life * 6, life, attack=0.0008) * rng.uniform(0.3, 1.0) * np.exp(-t0 / spread)))
    crack = norm(bandpass(noise(0.1, rng), band[0] * 0.6, min(band[1] * 1.3, 11000))) * decay(0.1, 0.01, 0.0005)
    return mix(0.8 * crack, *shards)


def thump(f0, f1, seconds, tau, drive=1.5, knock=0.2):
    """A drum-like thud: a sine falling fast in pitch as it dies, saturated for weight, with a short
    knock on top so it still reads on small speakers that can't play the low part."""
    body = saturate(sine(sweep(f0, f1, seconds, 0.4)) * decay(seconds, tau, 0.001), drive)
    # The knock is 6 ms of filtered noise; one fixed seed is plenty for something that short.
    hit = norm(bandpass(np.random.default_rng(5).standard_normal(samples(0.03)), 350, 2200)) * decay(0.03, 0.006, 0.0005)
    return mix(body, knock * hit)


def tick(freq, rng, level=1.0):
    """A clock's tick: a small wooden knock with a bright edge."""
    dur = 0.06
    body = sine(freq, dur) * decay(dur, 0.006, 0.0003)
    edge = 0.35 * sine(freq * 2.31, dur) * decay(dur, 0.0025, 0.0002)
    click = 0.25 * norm(bandpass(noise(dur, rng), 2500, 7500)) * decay(dur, 0.001, 0.0001)
    return level * (body + edge + click)


def bead(rng, freq=2600.0):
    """A glass bead knocking against another: a short ceramic knock."""
    dur = 0.06
    ping = sine(freq, dur) * decay(dur, 0.005, 0.0003)
    clack = 0.6 * sine(freq * 0.37, dur) * decay(dur, 0.009, 0.0005)
    click = 0.3 * norm(bandpass(noise(dur, rng), 2000, 6500)) * decay(dur, 0.0012, 0.0001)
    return ping + clack + click


def blip(rng):
    """A drop of liquid: a tiny sine chirping upward."""
    f0 = rng.uniform(300, 600)
    return sine(sweep(f0, f0 * 2.2, 0.03, 0.7)) * decay(0.03, 0.009, 0.001)


# ---------------------------------------------------------------- colour and space


def saturate(x, drive=2.0):
    """Soft clipping: rounds off the peaks and adds warm harmonics, like tape pushed a little."""
    top = np.max(np.abs(x))
    return np.tanh(drive * x / top) / np.tanh(drive) * top if top > 0 else x


def chorus(x, voices=3, depth=0.003, rate=0.5, base=0.012):
    """Copies of the sound, each delayed by a slowly wandering amount: a shimmer, a choir rather than a soloist."""
    x = np.concatenate([np.asarray(x, dtype=float), np.zeros(samples(base + depth) + 1)])
    idx = np.arange(len(x))
    t = idx / SR
    out = x.copy()
    for k in range(voices):
        delay = base + depth * np.sin(2 * np.pi * rate * (1 + 0.37 * k) * t + 2.1 * k)
        out += np.interp(idx - delay * SR, idx, x, left=0.0)
    return out / (1 + 0.6 * voices)


def reverb(x, seconds, wet, damp=6000.0, predelay=0.012):
    """A space for a sound: convolution with a synthetic impulse, noise decaying to -60 dB at
    `seconds` and darkening as it fades, like air soaking up the highs. Every sound shares the same
    room (one seed). Returns the dry sound plus `wet` of the reverb, tail and all."""
    rng = np.random.default_rng(1729)
    n = samples(seconds * 1.1)
    t = np.arange(n) / SR
    tail = rng.standard_normal(n) * 10 ** (-3 * t / seconds)
    blend = np.exp(-t / (seconds * 0.3))
    ir = np.concatenate([np.zeros(samples(predelay)), lowpass(tail, damp) * blend + lowpass(tail, damp * 0.25) * (1 - blend)])
    ir /= np.sqrt(np.sum(ir ** 2))
    return mix(x, wet * signal.fftconvolve(x, ir))


# ---------------------------------------------------------------- finishing

# Each role's ceiling (peak, dBFS) and loudness (weighted RMS of its loudest 0.4 s, dBFS): whichever
# is met first sets the level.
ROLES = {
    "impact": (-1.5, -15.0),   # punchy
    "cast": (-3.0, -17.0),
    "effect": (-3.0, -18.0),
    "grand": (-3.0, -19.0),    # long and wide: circles and domains
    "loop": (-6.0, -20.0),     # beds that play under everything else
    "ui": (-7.0, -22.0),       # quieter: heard close up, over and over
    "tick": (-12.0, -27.0),    # the quietest: the wheel's hover tick
}


def weighted(x):
    """Roughly how loud each part of the spectrum sounds (after ITU-R BS.1770's K-weighting): deep
    bass counts for less, and presence above 2 kHz for a little more."""
    low_cut = highpass(x, 60)
    return low_cut + 0.585 * highpass(low_cut, 2000)


def loudness(x, window=0.4):
    """The weighted level of the loudest `window` seconds, so short and long sounds, and deep and
    bright ones, compare fairly."""
    x = weighted(x)
    n = samples(window)
    if len(x) <= n:
        return np.sqrt(np.mean(x ** 2))
    energy = np.cumsum(np.concatenate(([0.0], x ** 2)))
    return np.sqrt(np.max(energy[n:] - energy[:-n]) / n)


def trim(x, floor_db=-50.0):
    """Cuts off the silence at the end: everything after the sound last rises above `floor_db` of its peak."""
    above = np.nonzero(np.abs(x) > np.max(np.abs(x)) * 10 ** (floor_db / 20))[0]
    return x[:min(len(x), above[-1] + samples(0.01))] if len(above) else x


def finish(x, role, loop=False, fade_in=0.003, fade_out=0.06, soften=12000.0):
    """Makes a sound ready to ship: no DC offset, no inaudible rumble, nothing fizzing above
    `soften`, no click at either end, and a level set by its role. A loop is only levelled: any
    filter or fade would break its seam."""
    x = np.asarray(x, dtype=float)
    if loop:
        x = x - x.mean()
    else:
        x = trim(highpass(lowpass(x, soften), 35))
        fi = min(samples(fade_in), len(x) // 4)
        fo = min(samples(fade_out), len(x) // 2)
        x[:fi] *= np.sin(np.linspace(0, np.pi / 2, fi)) ** 2
        x[len(x) - fo:] *= np.cos(np.linspace(0, np.pi / 2, fo)) ** 2
    peak_db, loud_db = ROLES[role]
    gain = min(10 ** (peak_db / 20) / np.max(np.abs(x)), 10 ** (loud_db / 20) / loudness(x))
    return x * gain


# ================================================================ the palette
# Each builder takes (variant, rng) and returns a finished sound.

# ---------------------------------------------------------------- fire: a crackling whoosh with a warm body


def cast_fire(v, rng):
    dur = 0.9
    whoosh = moving_band(dur, [(0, 320), (0.16, 1500 + 400 * v), (dur, 650)], 1.1, rng)
    whoosh *= env(dur, (0, 0), (0.05, 1), (0.3, 0.5), (dur, 0)) ** 1.5
    fwoomp = norm(lowpass(noise(dur, rng), 420)) * decay(dur, 0.13, 0.012)
    root = note(D, -2) if v == 0 else note(A, -3)
    body = sine(root, dur) + 0.5 * sine(root * 1.5, dur) + 0.25 * sine(root * 2, dur)
    body = saturate(body, 2.0) * env(dur, (0, 0), (0.03, 1), (0.35, 0.3), (dur, 0))
    embers = norm(bandpass(grains(dur, 60 + 20 * v, rng, shape=[(0, 1), (0.4, 0.6), (dur, 0)]), 1400, 6000))
    x = mix(0.9 * whoosh, 0.5 * fwoomp, 0.35 * body, 0.3 * embers)
    return finish(reverb(x, 0.7, 0.14), "cast")


def impact_fire(v, rng):
    dur = 0.85
    whump = norm(lowpass(noise(dur, rng), 650)) * decay(dur, 0.08 + 0.02 * v, 0.002)
    body = thump(130 - 10 * v, 45, dur, 0.09)
    roar = moving_band(dur, [(0, 2600), (dur, 1100)], 1.2, rng) * decay(dur, 0.22, 0.01)
    embers = norm(bandpass(grains(dur, 140, rng, shape=[(0, 1), (0.12, 0.9), (dur, 0.03)]), 1200, 6500))
    x = mix(0.7 * whump, 0.5 * body, 0.55 * roar, 0.6 * embers)
    return finish(reverb(saturate(x, 1.4), 0.8, 0.12), "impact")


# ---------------------------------------------------------------- frost: a crystalline chime and icy shimmer; a shatter

FROST_NOTES = (((B, 1), (FS, 2), (E, 2)), ((A, 1), (E, 2), (B, 2)))


def cast_frost(v, rng):
    dur = 1.2
    chimes = [(0.03 + 0.04 * i, (0.9 - 0.2 * i) * glass(note(*n), dur, 0.45 - 0.08 * i)) for i, n in enumerate(FROST_NOTES[v])]
    breath = moving_band(dur, [(0, 1200), (0.3, 3200), (dur, 2200)], 1.0, rng) * swell(dur, 0.18)
    rime = sparkle(dur, 26, rng, [note(d, 3) for d in (D, E, FS)], tau=(0.03, 0.08), shape=[(0, 0.3), (0.2, 1), (dur, 0)])
    x = mix(*chimes, 0.22 * breath, 0.18 * norm(rime))
    return finish(reverb(x, 1.3, 0.25, damp=7000), "cast")


def impact_frost(v, rng):
    dur = 1.0
    shards = shatter(rng, count=20 + 4 * v, band=(2200, 6500), spread=0.14)
    tinkle = sparkle(dur, 22, rng, [note(d, o) for o in (1, 2) for d in range(5)], tau=(0.06, 0.16), shape=[(0, 0), (0.08, 1), (dur, 0)])
    ring = glass(note((D, A, FS)[v], 2), dur, 0.5)
    thud = thump(220, 90, 0.3, 0.04, drive=1.2)
    x = mix(0.8 * shards, 0.3 * norm(tinkle), 0.35 * ring, 0.35 * thud)
    return finish(reverb(lowpass(x, 9500), 1.0, 0.2, damp=7000), "impact")


# ---------------------------------------------------------------- storm: an electric zap and crackle


def cast_storm(v, rng):
    dur = 0.8
    rise = 0.16 + 0.03 * v
    f = glide(dur, (0, note(A, -2)), (rise, note(A, 0)), (dur, note(A, 0))) * (1 + 0.025 * jitter(dur, rng, 30))
    buzz = lowpass(soft_saw(f, harmonics=10), 4000) * chopper(dur, rng) * env(dur, (0, 0), (rise, 1), (dur, 0))
    arcs = norm(bandpass(grains(dur, 180, rng, length=(0.0004, 0.0015), shape=[(0, 0.2), (rise, 1), (dur, 0.2)]), 2000, 7500))
    snap = norm(bandpass(noise(0.1, rng), 2500, 8000)) * decay(0.1, 0.007, 0.0003)
    zap = fm(sweep(1800, 420, 0.14, 0.6), 1.5, 2.5 * decay(0.14, 0.05)) * decay(0.14, 0.05, 0.001)
    x = mix(0.4 * buzz, 0.35 * arcs, (rise, 0.5 * snap), (rise, 0.35 * zap))
    return finish(reverb(lowpass(x, 8500), 0.5, 0.12), "cast")


def impact_storm(v, rng):
    dur = 1.1
    crack = norm(bandpass(noise(0.3, rng), 500, 8000)) * decay(0.3, 0.012 + 0.003 * v, 0.0003)
    roll = moving_band(dur, [(0, 1500), (dur, 500)], 1.0, rng) * decay(dur, 0.2, 0.006)
    boom = norm(lowpass(noise(dur, rng), 220)) * decay(dur, 0.28, 0.004)
    body = thump(95, 40, 0.6, 0.14)
    sizzle = norm(bandpass(grains(dur, 220, rng, length=(0.0004, 0.002), shape=[(0, 1), (0.25, 0.4), (dur, 0.02)]), 2500, 8000))
    arc = lowpass(soft_saw(note(D, -1) * (1 + 0.03 * jitter(dur, rng, 40)), harmonics=9), 3500) * chopper(dur, rng) * decay(dur, 0.12, 0.01)
    x = mix(1.0 * crack, 0.35 * boom, 0.45 * body, 0.5 * roll, 0.6 * sizzle, (0.02, 0.45 * arc))
    return finish(reverb(lowpass(saturate(x, 1.3), 9000), 1.0, 0.15), "impact")


# ---------------------------------------------------------------- wind: an airy whoosh


def cast_wind(v, rng):
    dur = 0.95
    peak = 0.32 + 0.04 * v
    whoosh = moving_band(dur, [(0, 300), (peak, 1700 + 400 * v), (dur, 800)], [(0, 1.4), (dur, 1.0)], rng) * swell(dur, peak)
    whistle = moving_band(dur, [(0, note(E, 1) * 0.94), (peak, note((E, A)[v], 1)), (dur, note(D, 1))], 0.05, rng) * swell(dur, peak + 0.05)
    air = norm(lowpass(noise(dur, rng), 320)) * swell(dur, peak * 0.8)
    x = mix(whoosh, 0.22 * whistle, 0.3 * air)
    return finish(reverb(x, 0.6, 0.12), "cast")


def impact_wind(v, rng):
    dur = 0.85
    puff = moving_band(dur, [(0, 900), (0.05, 1500), (dur, 380)], 1.5, rng) * decay(dur, 0.11 + 0.02 * v, 0.005)
    whump = norm(lowpass(noise(dur, rng), 260)) * decay(dur, 0.07, 0.004)
    body = thump(150, 60, 0.3, 0.05, 1.2)
    eddy = 1 + 0.5 * np.sin(2 * np.pi * (6 + v) * timeline(dur))
    swirl = moving_band(dur, [(0, 2100), (dur, 650)], 0.8, rng) * env(dur, (0, 0), (0.12, 1), (dur, 0)) * eddy
    x = mix(0.9 * puff, 0.5 * whump, 0.4 * body, 0.35 * swirl)
    return finish(reverb(x, 0.7, 0.14), "impact")


# ---------------------------------------------------------------- earth: a deep thud with grit


def cast_earth(v, rng):
    dur = 0.95
    hit = 0.2 + 0.03 * v
    heave = saturate(sine(glide(dur, (0, 50), (hit, 72), (dur, 60))), 2.5) * swell(dur, hit, 1.5)
    thud = thump(140, 46, 0.4, 0.08, 2.0)
    gravel = norm(lowpass(grains(dur, 420, rng, length=(0.002, 0.008), shape=[(0, 0.2), (hit, 1), (hit + 0.3, 0.35), (dur, 0)]), 2800))
    rumble = norm(lowpass(brown(dur, rng), 170)) * swell(dur, hit)
    grind = moving_band(dur, [(0, 300), (hit, 900), (dur, 400)], 0.8, rng) * swell(dur, hit)
    x = mix(0.3 * heave, (hit, 0.75 * thud), 0.9 * gravel, 0.3 * rumble, 0.5 * grind)
    return finish(reverb(x, 0.6, 0.08, damp=2500), "cast")


def impact_earth(v, rng):
    dur = 1.1
    thud = thump(120 - 8 * v, 45, 0.8, 0.14, 2.6)
    knock = norm(bandpass(noise(0.1, rng), 200, 1500)) * decay(0.1, 0.018, 0.0005)
    crack = norm(bandpass(noise(0.2, rng), 500, 3000)) * decay(0.2, 0.03, 0.0005)
    debris = norm(bandpass(grains(dur, 650, rng, length=(0.003, 0.012), shape=[(0, 1), (0.2, 0.5), (dur, 0.02)]), 180, 3200))
    rumble = norm(lowpass(brown(dur, rng), 150)) * decay(dur, 0.32, 0.01)
    x = mix(0.8 * thud, 0.6 * knock, 0.8 * crack, 0.9 * debris, 0.3 * rumble)
    return finish(reverb(x, 0.9, 0.1, damp=2500), "impact")


# ---------------------------------------------------------------- life: a soft bell bloom

LIFE_CHORDS = (((D, 1), (FS, 1), (A, 1)), ((A, 0), (D, 1), (E, 1)))


def cast_life(v, rng):
    dur = 1.6
    bells = mix(*[(0.07 * i, (1 - 0.15 * i) * bell(note(*n), dur, 0.8, 2.0, 1.1, attack=0.035)) for i, n in enumerate(LIFE_CHORDS[v])])
    pad = (sine(note(D, 0), dur) + 0.4 * sine(note(A, 0), dur)) * swell(dur, 0.4)
    glints = sparkle(dur, 9, rng, [note(d, 2) for d in (D, FS, A)], tau=(0.12, 0.25), shape=[(0, 0), (0.2, 1), (dur, 0.2)])
    x = mix(chorus(bells, 3, 0.003, 0.4), 0.2 * pad, 0.15 * norm(glints))
    return finish(reverb(x, 1.6, 0.3), "cast")


def impact_life(v, rng):
    dur = 1.2
    root = note((FS, A, D)[v], 1 + (v == 2))
    bloom = bell(root, dur, 0.65, 2.0, 0.9, attack=0.04) + 0.5 * bell(root / 2, dur, 0.8, 2.0, 0.6, attack=0.06)
    rustle = moving_band(dur, [(0, 2500), (0.3, 4500), (dur, 3200)], 0.9, rng) * swell(dur, 0.25)
    rise = [(0.08 + 0.07 * i, 0.3 * glass(note(d, 2), 0.6, 0.18)) for i, d in enumerate((D, FS, A))]
    x = mix(chorus(bloom, 2, 0.003, 0.5), 0.1 * rustle, *rise)
    return finish(reverb(x, 1.3, 0.28), "cast")


# ---------------------------------------------------------------- void: a low swell, sucked in backwards


def cast_void(v, rng):
    dur = 0.95
    root = note(D, -2) if v == 0 else note(A, -2)
    toll = bell(root, 1.2, 0.5, 1.41, 4.0, attack=0.002) + norm(lowpass(noise(1.2, rng), 600)) * decay(1.2, 0.15)
    inward = reverse(reverb(toll, 1.0, 0.5, damp=1800))[-samples(dur):]
    inhale = moving_band(dur, [(0, 220), (dur, 950)], 1.0, rng) * env(dur, (0, 0), (dur * 0.8, 0.7), (dur - 0.015, 1), (dur, 0)) ** 2
    thup = thump(95, 42, 0.35, 0.05, 1.5)
    x = mix(norm(inward), 0.6 * inhale, (dur - 0.01, 0.6 * thup))
    return finish(reverb(x, 0.5, 0.12, damp=1500), "cast")


def impact_void(v, rng):
    pre = 0.32 + 0.04 * v
    toll = bell(note(D, -2), 0.8, 0.3, 1.41, 3.0, attack=0.002) + norm(lowpass(noise(0.8, rng), 500)) * decay(0.8, 0.1)
    implode = norm(reverse(reverb(toll, 0.6, 0.5, damp=1500))[-samples(pre):])
    suck = moving_band(pre, [(0, 1400), (pre, 300)], 0.9, rng) * env(pre, (0, 0), (pre * 0.85, 1), (pre, 0)) ** 2
    boom = thump(80, 36, 0.9, 0.2, 2.2, knock=0.3)
    tail = norm(lowpass(noise(1.0, rng), 500)) * decay(1.0, 0.25, 0.01)
    whine = sine(sweep(note(A, 0), note(A, -2), 0.7)) * decay(0.7, 0.18, 0.005)
    x = mix(0.8 * implode, 0.7 * suck, (pre, 0.8 * boom), (pre, 0.25 * tail), (pre, 0.45 * whine))
    return finish(reverb(x, 1.3, 0.28, damp=1500), "impact")


# ---------------------------------------------------------------- arcane: a bright shimmering chord

ARCANE_CHORDS = (((D, 1), (FS, 1), (A, 1), (E, 2)), ((A, 0), (E, 1), (A, 1), (B, 1)))
ARCANE_STABS = (((A, 1), (D, 2), (FS, 2)), ((FS, 1), (A, 1), (D, 2)), ((B, 1), (E, 2), (A, 2)))


def cast_arcane(v, rng):
    dur = 1.4
    strum = mix(*[(0.035 * i, (1 - 0.12 * i) * bell(note(*n), dur, 0.7, 2.0, 2.2, attack=0.012)) for i, n in enumerate(ARCANE_CHORDS[v])])
    whirl = sine(sweep(note(A, 0), note(A, 1), 0.35, 0.8)) * swell(0.35, 0.25)
    twinkle = sparkle(dur, 24, rng, [note(d, o) for o in (2, 3) for d in (D, E, FS, A)], tau=(0.05, 0.14), shape=[(0, 0.2), (0.15, 1), (dur, 0)])
    x = mix(chorus(strum, 3, 0.004, 0.9), 0.15 * whirl, 0.2 * norm(twinkle))
    return finish(reverb(x, 1.5, 0.3, damp=7000), "cast")


def impact_arcane(v, rng):
    dur = 1.0
    pop = sine(sweep(620, 160, 0.08, 0.5)) * decay(0.08, 0.022, 0.001)
    pop += 0.4 * norm(bandpass(noise(0.08, rng), 800, 4000)) * decay(0.08, 0.012, 0.0005)
    chord = mix(*[(0.008 * i, glass(note(*n), dur, 0.35)) for i, n in enumerate(ARCANE_STABS[v])])
    burst = sparkle(0.7, 50, rng, [note(d, o) for o in (2, 3) for d in range(5)], tau=(0.03, 0.09), shape=[(0, 1), (0.7, 0)])
    x = mix(0.6 * pop, 0.6 * chord, 0.3 * norm(burst))
    return finish(reverb(x, 1.1, 0.25, damp=7000), "impact")


# ---------------------------------------------------------------- time: ticking into a clock chime

TICK_TIMES = ((0.0, 0.17, 0.3, 0.4, 0.47), (0.0, 0.15, 0.27, 0.36, 0.43, 0.49))


def cast_time(v, rng):
    times = TICK_TIMES[v]
    ticks = [(t, tick((3000, 2250)[i % 2], rng, 0.4 + 0.1 * i)) for i, t in enumerate(times)]
    at = times[-1] + 0.1
    chime = clock_bell(note((A, D)[v], 1 + v), 1.0, 0.55)
    whirr = moving_band(at, [(0, 800), (at, 2400)], 0.8, rng) * swell(at, at * 0.9)
    x = mix(*ticks, 0.12 * whirr, (at, 0.8 * chime))
    return finish(reverb(x, 1.2, 0.22), "cast")


def impact_time(v, rng):
    ding = clock_bell(note((A, D, FS)[v], 1 + (v > 0)), 1.2, 0.6)
    # The clock winding down: ticks slowing after the strike.
    gaps = np.cumsum([0.05, 0.06, 0.075, 0.095, 0.12, 0.15])
    ticks = [(0.04 + g, tick(2600 - 120 * i, rng, 0.35 * (1 - i / 7))) for i, g in enumerate(gaps)]
    thud = thump(300, 120, 0.2, 0.03, 1.0)
    x = mix(ding, 0.3 * thud, *ticks)
    return finish(reverb(x, 1.2, 0.25), "impact")


# ---------------------------------------------------------------- blood: a wet, low pulse


def cast_blood(v, rng):
    dur = 0.95
    beat = 0.15 + 0.02 * v
    lub = thump(95, 48, 0.35, 0.07, 2.2, knock=0.3)
    wet = moving_band(dur, [(0.02, 350), (0.25, 1250), (dur, 480)], 0.9, rng) * env(dur, (0, 0), (0.1, 1), (dur, 0)) ** 2
    drops = [(t, 0.25 * blip(rng)) for t in np.sort(rng.uniform(0.1, 0.55, 3 + v))]
    drone = saturate(sine(note(D, -2), dur) + 0.5 * sine(note(D, -2) * 1.006, dur), 2.0) * swell(dur, 0.3)
    # The pulse's body: a dark reed on D, swelling with each beat, so it carries on small speakers.
    beats = mix(decay(0.3, 0.07, 0.01), (beat, 0.75 * decay(0.3, 0.07, 0.01)))
    reed = lowpass(soft_saw(note(D, -1), len(beats) / SR, harmonics=8), 700) * beats
    x = mix(0.85 * lub, (beat, 0.65 * lub), 0.6 * wet, *drops, 0.12 * drone, 0.7 * norm(reed))
    return finish(reverb(x, 0.6, 0.1, damp=2000), "cast")


def impact_blood(v, rng):
    thud = thump(110, 48, 0.4, 0.06, 2.4, knock=0.3)
    splat = moving_band(0.3, [(0, 1900), (0.12, 600)], 0.8, rng) * decay(0.3, 0.04, 0.001)
    drops = [(t, 0.4 * blip(rng)) for t in np.sort(rng.uniform(0.06, 0.45, 4 + v))]
    after = thump(80, 42, 0.3, 0.05, 2.0)
    x = mix(0.8 * thud, 1.2 * splat, *drops, (0.2 + 0.02 * v, 0.35 * after))
    return finish(reverb(x, 0.6, 0.1, damp=2000), "impact")


CASTS = {"fire": cast_fire, "frost": cast_frost, "storm": cast_storm, "wind": cast_wind, "earth": cast_earth,
         "life": cast_life, "void": cast_void, "arcane": cast_arcane, "time": cast_time, "blood": cast_blood}
IMPACTS = {"fire": impact_fire, "frost": impact_frost, "storm": impact_storm, "wind": impact_wind, "earth": impact_earth,
           "life": impact_life, "void": impact_void, "arcane": impact_arcane, "time": impact_time, "blood": impact_blood}


# ---------------------------------------------------------------- casting


def charge_loop(v, rng):
    """A warm hum on D, its overtones slowly trading places. Seamless: every partial and every
    wobble fits a whole number of times into the loop's 2 seconds (so D3 is rounded to 147 Hz)."""
    dur = 2.0
    t = timeline(dur)

    def tone(hz, level, wobble=0.0, rate=0.5, offset=0.0):
        return level * (1 - wobble + wobble * np.sin(2 * np.pi * rate * t + offset)) * np.sin(2 * np.pi * hz * t)

    x = (tone(147.0, 1.0) + tone(147.5, 0.35)            # the root, beating slowly once a loop
         + tone(220.5, 0.25, 0.4, 0.5, 1.0)              # the fifth
         + tone(294.0, 0.6, 0.25, 0.5)                   # overtones, each swelling in its own time
         + tone(441.0, 0.4, 0.4, 0.5, 2.0)
         + tone(588.0, 0.25, 0.5, 1.0, 1.0)
         + tone(735.0, 0.07, 0.5, 1.5, 0.5)
         + tone(882.0, 0.05, 0.5, 0.5, 4.0))
    air = 0.04 * loop_noise(dur, 500, 2500, rng) * (0.7 + 0.3 * np.sin(2 * np.pi * t))
    return finish(x + air, "loop", loop=True)


def charge_full(v, rng):
    dur = 1.5
    run = ((0.0, (A, 1)), (0.06, (D, 2)), (0.12, (FS, 2)))
    chime = mix(*[(t, (1 - 0.2 * i) * (glass(note(*n), dur, 0.6) + 0.5 * bell(note(*n), dur, 0.5, 3.0, 1.2, 0.002)))
                  for i, (t, n) in enumerate(run)])
    flare = moving_band(0.25, [(0, 2000), (0.2, 6000)], 0.8, rng) * swell(0.25, 0.18)
    glints = sparkle(dur, 14, rng, [note(d, 3) for d in (D, FS, A)], tau=(0.05, 0.12), shape=[(0, 0), (0.15, 1), (dur, 0)])
    x = mix(0.12 * flare, (0.02, norm(chime)), 0.18 * norm(glints))
    return finish(reverb(x, 1.4, 0.3, damp=8000), "cast")


def release(v, rng):
    dur = 0.75
    whoosh = moving_band(dur, [(0, 500), (0.09, 2800 + 400 * v), (dur, 1100)], 1.2, rng) * decay(dur, 0.12, 0.012)
    pop = mix(thump(240 - 30 * v, 70, 0.2, 0.03, 1.5), 0.3 * norm(lowpass(noise(0.05, rng), 1500)) * decay(0.05, 0.008, 0.0005))
    flick = sine(sweep(note(D, 1), note((A, D)[v], 1 + v), 0.18, 0.6)) * decay(0.18, 0.07, 0.004)
    x = mix(0.85 * whoosh, 0.7 * pop, 0.22 * flick)
    return finish(reverb(x, 0.7, 0.14), "cast")


def circle_open(v, rng):
    dur = 1.4
    steps = ((D, 1), (E, 1), (FS, 1), (A, 1), (B, 1), (D, 2), (E, 2), (FS, 2), (A, 2))
    pings = mix(*[(0.05 * i, (0.5 + 0.3 * i / 8) * glass(note(*n), 0.8, 0.28)) for i, n in enumerate(steps)])
    sheen = moving_band(dur, [(0, 1500), (0.6, 5500), (dur, 4000)], 0.7, rng) * swell(dur, 0.55)
    hum = (sine(note(D, 0), dur) + 0.5 * sine(note(A, 0), dur)) * swell(dur, 0.5)
    x = mix(chorus(pings, 2, 0.003, 0.6), 0.12 * sheen, 0.15 * hum)
    return finish(reverb(x, 1.4, 0.3, damp=7000), "effect")


def beam_fire(v, rng):
    dur = 1.1
    root = note(D, 0) if v == 0 else note(A, -1)
    f = root * (1 + 0.004 * np.sin(2 * np.pi * 5.5 * timeline(dur)))
    core = lowpass(soft_saw(f, harmonics=14) + 0.6 * soft_saw(f * 2.003, harmonics=8), 3200)
    core = chorus(core, 3, 0.003, 0.7)[:samples(dur)] * env(dur, (0, 0), (0.025, 1), (0.3, 0.55), (dur, 0))
    zing = fm(note(A, 1), 2.0, 3.0 * decay(dur, 0.12, 0.002)) * decay(dur, 0.25, 0.003)
    blast = moving_band(dur, [(0, 800), (0.05, 3200), (dur, 1400)], 1.3, rng) * decay(dur, 0.18, 0.004)
    punch = thump(170, 60, 0.3, 0.05, 1.5)
    x = mix(0.4 * norm(core), 0.25 * zing, 0.45 * blast, 0.5 * punch)
    return finish(reverb(x, 0.9, 0.18), "cast")


def orb_hum(v, rng):
    """One second, shaped so that copies started every half second (an orb replays it every 10
    ticks) add up to a steady, throbbing hum: a full sine-squared window, and partials that fit a
    whole number of times into half a second so the copies line up."""
    dur = 1.0
    t = timeline(dur)
    x = (np.sin(2 * np.pi * 294 * t) + 0.25 * np.sin(2 * np.pi * 296 * t)      # D4, and a gentle 2 Hz throb
         + 0.35 * np.sin(2 * np.pi * 440 * t) + 0.2 * np.sin(2 * np.pi * 588 * t) + 0.08 * np.sin(2 * np.pi * 882 * t))
    x *= (1 + 0.1 * np.sin(2 * np.pi * 4 * t)) * np.sin(np.pi * t / dur) ** 2
    return finish(x, "loop", loop=True)


def shield_up(v, rng):
    dur = 1.2
    rise = (sine(sweep(note(D, -1), note(D, 0), 0.3, 0.7)) + 0.6 * sine(sweep(note(A, -1), note(A, 0), 0.3, 0.7))) * swell(0.3, 0.25)
    dome = mix((0.2, bell(note(A, 0), dur, 0.6, 2.0, 1.4, attack=0.02)), (0.24, 0.7 * bell(note(D, 1), dur, 0.55, 2.0, 1.2, attack=0.02)))
    sheen = mix((0.22, glass(note(FS, 1), dur, 0.5)), (0.26, 0.8 * glass(note(A, 1), dur, 0.45)))
    x = mix(0.5 * rise, dome, 0.25 * chorus(sheen, 2, 0.003, 0.8))
    return finish(reverb(x, 1.1, 0.25), "effect")


def shield_break(v, rng):
    shards = shatter(rng, count=26 + 4 * v, band=(1200, 5200), spread=0.16 + 0.03 * v)
    # Then the pieces coming down: small bright ticks, thinning out, as shards land and skitter.
    rain = shatter(rng, count=16 + 2 * v, band=(3200, 8000), spread=0.55, tau=(0.008, 0.035))
    fall = sine(sweep(note(A, 0), note(A, -1) * 0.94, 0.7, 0.8)) + 0.7 * sine(sweep(note(D, 0), note(D, -1) * 0.94, 0.7, 0.8))
    fall *= decay(0.7, 0.22, 0.002)
    thud = thump(160, 55, 0.4, 0.06, 1.6)
    x = mix(0.8 * shards, (0.14, 0.3 * rain), 0.35 * fall, 0.6 * thud)
    return finish(reverb(lowpass(x, 9500), 1.1, 0.22), "impact")


def shield_block(v, rng):
    """A spell rings off a shield: a hard glassy strike, a bright ring shimmering out over the low hum
    of the shell holding, and the hiss of the spell turned aside."""
    dur = 1.3
    strike = shatter(rng, count=5 + 2 * v, band=(2800, 7000), spread=0.012, tau=(0.015, 0.05))
    ring = mix((0.0, glass(note((A, FS)[v], 1), dur, 0.42)), (0.004, 0.6 * glass(note((D, A)[v], 2), dur, 0.3)))
    hum = sine(note(D, 0), 0.5) * decay(0.5, 0.18, 0.004)
    hiss = moving_band(0.35, [(0, 5000), (0.35, 1800)], 0.8, rng) * env(0.35, (0, 0), (0.02, 1), (0.35, 0))
    x = mix(0.7 * strike, 0.55 * chorus(ring, 2, 0.002, 1.1), 0.25 * hum, 0.18 * hiss)
    return finish(reverb(x, 1.0, 0.22), "impact")


def imbue(v, rng):
    """Magic sinking into an item or a block: a shimmer drawn inward, sealed with a soft struck chord."""
    draw = reverse(mix(*[(0.02 * i, 0.3 * glass(note(d, 2), 0.6, 0.18)) for i, d in enumerate((D, FS, A, B))]))
    seal = mix((0.0, 0.5 * bell(note(D, 0), 0.9, 0.4, 2.0, 1.2)), (0.0, 0.35 * bell(note(A, 0), 0.9, 0.35, 2.0, 1.2)))
    thud = thump(140, 70, 0.3, 0.05, 1.2)
    x = mix(0.5 * draw, (0.6, seal), (0.6, 0.4 * thud))
    return finish(reverb(x, 1.0, 0.25), "effect")


def domain_open(v, rng):
    dur = 4.6
    crest = 1.4
    drone = sine(note(D, -2), dur) + 0.6 * sine(note(A, -2), dur) + 0.4 * sine(note(D, -1), dur) + 0.3 * sine(note(D, -2) * 1.004, dur)
    drone = saturate(drone, 1.8) * env(dur, (0, 0), (crest, 1), (crest + 0.8, 0.6), (dur, 0)) ** 1.5
    wash = moving_band(dur, [(0, 200), (crest, 3500), (dur, 1200)], 1.4, rng) * env(dur, (0, 0), (crest, 1), (crest + 0.3, 0.4), (dur, 0)) ** 2
    chord = mix(*[(crest + 0.02 * i, (1 - 0.1 * i) * bell(note(*n), 3.0, 1.6, 2.0, 2.2, attack=0.008))
                  for i, n in enumerate(((D, -1), (A, -1), (D, 0), (FS, 0), (A, 0)))])
    boom = thump(95, 38, 1.0, 0.3, 2.0)
    glints = sparkle(2.8, 10, rng, [note(d, o) for o in (2, 3) for d in (D, FS, A)], tau=(0.1, 0.3), shape=[(0, 1), (2.8, 0)])
    x = mix(0.25 * drone, 0.35 * wash, (crest, 0.5 * boom), 0.6 * norm(chord), (crest, 0.12 * norm(glints)))
    return finish(reverb(x, 3.5, 0.45, damp=5000, predelay=0.03), "grand")


def domain_close(v, rng):
    hit = 0.4
    lead = reverse(reverb(bell(note(D, -1), 0.6, 0.25, 2.0, 1.5), 0.5, 0.4))[-samples(hit):]
    steps = ((A, 2), (FS, 2), (E, 2), (D, 2), (B, 1), (A, 1), (FS, 1), (D, 1))
    fall = mix(*[(0.045 * i, (0.7 - 0.05 * i) * glass(note(*n), 0.7, 0.25)) for i, n in enumerate(steps)])
    sink = (sine(sweep(note(A, 0), note(A, -1), 0.9, 0.7)) + 0.8 * sine(sweep(note(D, 0), note(D, -1), 0.9, 0.7))) * decay(0.9, 0.35, 0.05)
    thud = thump(100, 36, 1.0, 0.2, 2.2)
    crumble = norm(lowpass(grains(1.0, 500, rng, length=(0.003, 0.01), shape=[(0, 1), (1.0, 0)]), 2600))
    x = mix(0.4 * norm(lead), 0.5 * fall, 0.35 * sink, (hit, 0.9 * thud), (hit, 0.35 * crumble))
    return finish(reverb(x, 2.5, 0.4, damp=4500, predelay=0.025), "grand")


def blink(v, rng):
    gone = 0.13
    inrush = reverse(glass(note((A, E)[v], 2), 0.4, 0.12))[-samples(gone):]
    fwip = moving_band(gone, [(0, 600), (gone, 5200)], 0.8, rng) * env(gone, (0, 0), (gone * 0.85, 1), (gone, 0)) ** 2
    chirp = sine(sweep(400, 2400, gone, 1.5)) * env(gone, (0, 0), (gone * 0.9, 1), (gone, 0)) ** 2
    pop = sine(sweep(900, 300, 0.05, 0.5)) * decay(0.05, 0.014, 0.0005)
    glints = sparkle(0.5, 30, rng, [note(d, 2) for d in range(5)], tau=(0.04, 0.1), shape=[(0, 1), (0.5, 0)])
    x = mix(0.4 * norm(inrush), 0.45 * fwip, 0.2 * chirp, (gone, 0.6 * pop), (gone, 0.25 * norm(glints)))
    return finish(reverb(x, 0.8, 0.2), "effect")


def magic_break(v, rng):
    crunch = norm(bandpass(grains(0.35, 1600, rng, length=(0.001, 0.006), shape=[(0, 1), (0.08, 0.6), (0.35, 0)]), 250, 3500))
    knock = norm(lowpass(noise(0.1, rng), 1200)) * decay(0.1, 0.02, 0.0005)
    thud = thump(170 - 15 * v, 70, 0.25, 0.04, 1.6)
    shine = mix(*[(0.03 + 0.05 * i, 0.3 * glass(note(d, 2), 0.5, 0.16)) for i, d in enumerate(((A, D), (FS, B), (E, A))[v])])
    x = mix(0.7 * crunch, 0.5 * knock, 0.6 * thud, shine)
    return finish(reverb(x, 0.6, 0.12), "effect")


# ---------------------------------------------------------------- the interface


def rune_thread(v, rng):
    chime = glass(note((B, D, E)[v], 1 + (v > 0)), 0.5, 0.18)
    x = mix(0.8 * bead(rng, 2500 + 120 * v), (0.012, 0.45 * chime))
    return finish(reverb(x, 0.5, 0.12), "ui")


def rune_unthread(v, rng):
    first, second = ((A, 1), (E, 1)) if v == 0 else ((FS, 1), (D, 1))
    slide = moving_band(0.08, [(0, 3000), (0.08, 1200)], 0.7, rng) * env(0.08, (0, 0), (0.02, 1), (0.08, 0))
    x = mix(0.2 * slide, 0.6 * bead(rng, 1800 + 100 * v), (0.012, 0.35 * glass(note(*first), 0.4, 0.12)),
            (0.07, 0.3 * glass(note(*second), 0.4, 0.14)))
    return finish(reverb(x, 0.5, 0.1), "ui")


def wheel_open(v, rng):
    dur = 0.7
    whoosh = moving_band(0.45, [(0, 500), (0.25, 2400), (0.45, 1600)], 1.0, rng) * swell(0.45, 0.2)
    bloom = mix(bell(note(D, 1), dur, 0.3, 2.0, 1.0, attack=0.02), (0.04, 0.7 * bell(note(A, 1), dur, 0.28, 2.0, 0.9, attack=0.02)))
    x = mix(0.35 * whoosh, (0.08, 0.5 * bloom))
    return finish(reverb(x, 0.8, 0.2), "ui")


def wheel_hover(v, rng):
    dur = 0.05
    f = note((A, B, FS)[v], 2)
    x = sine(f, dur) * decay(dur, 0.006, 0.0005) + 0.25 * sine(f * 2, dur) * decay(dur, 0.002, 0.0003)
    return finish(x, "tick", fade_in=0.0005, fade_out=0.008)


def wheel_select(v, rng):
    dur = 0.6
    x = mix(0.5 * bead(rng, 1500), (0.01, 0.5 * glass(note(D, 2), dur, 0.2)), (0.06, 0.45 * glass(note(A, 2), dur, 0.22)))
    return finish(reverb(x, 0.6, 0.15), "ui")


def discovery(v, rng):
    dur = 2.0
    run = ((0.0, (D, 1)), (0.09, (FS, 1)), (0.18, (A, 1)), (0.3, (D, 2)))
    fanfare = mix(*[(t, (0.8 + 0.1 * i) * bell(note(*n), dur, 0.9, 2.0, 1.6, attack=0.006)) for i, (t, n) in enumerate(run)])
    hold = mix(*[(0.3, 0.3 * bell(note(*n), dur, 1.1, 2.0, 0.8, attack=0.05)) for n in ((D, 0), (A, 0))])
    glints = sparkle(1.6, 14, rng, [note(d, 3) for d in (D, E, FS, A)], tau=(0.06, 0.16), shape=[(0, 0), (0.3, 1), (1.6, 0)])
    x = mix(chorus(mix(fanfare, hold), 2, 0.003, 0.5), (0.3, 0.18 * norm(glints)))
    return finish(reverb(x, 1.6, 0.3), "effect")


# ---------------------------------------------------------------- the heart


def circle_formed(v, rng):
    dur = 4.0
    toll = clock_bell(note(D, -1), dur, 1.6, attack=0.004)
    boom = thump(90, 40, 1.2, 0.35, 1.8)
    # A choir-like chord swelling up: soft saws, each with its own vibrato, through two 'ah' formants.
    t = timeline(dur)
    chord = sum(soft_saw(note(*n) * (1 + 0.003 * np.sin(2 * np.pi * (4.5 + 0.4 * i) * t)), harmonics=10)
                for i, n in enumerate(((A, -1), (D, 0), (FS, 0), (A, 0), (D, 1))))
    choir = bandpass(chord, 550, 900) + 0.7 * bandpass(chord, 1000, 1400) + 0.3 * lowpass(chord, 500)
    choir = chorus(choir, 3, 0.005, 0.35)[:samples(dur)] * env(dur, (0, 0), (0.9, 1), (2.4, 0.8), (dur, 0)) ** 1.5
    rising = sparkle(2.2, 26, rng, [note(d, o) for o in (1, 2, 3) for d in range(5)], tau=(0.08, 0.2),
                     shape=[(0, 0.05), (2.0, 1), (2.2, 0.3)], rising=True)
    crown = mix(*[(2.0 + 0.05 * i, 0.4 * glass(note(d, 2), 1.5, 0.5)) for i, d in enumerate((D, FS, A))])
    x = mix(0.8 * toll, 0.5 * boom, 0.4 * norm(choir), 0.15 * norm(rising), crown)
    return finish(reverb(x, 3.2, 0.45, damp=5500, predelay=0.03), "grand")


def overcast(v, rng):
    snap = 1.05
    t = timeline(snap)
    strain = 1 + 0.03 * (t / snap) ** 2                                                  # pushed sharp as it strains
    rough = 1 - (0.25 + 0.35 * t / snap) * (0.5 + 0.5 * np.sin(2 * np.pi * 27 * t))     # a flutter that grows rough
    tritone = 2 ** (6 / 12)
    tones = soft_saw(note(D, 0) * strain, harmonics=6) + 0.8 * soft_saw(note(D, 0) * tritone * 1.002 * strain, harmonics=6)
    tones = lowpass(tones, 2400) * rough * env(snap, (0, 0), (0.15, 0.4), (snap - 0.03, 1), (snap, 0)) ** 1.5
    creaks = norm(bandpass(grains(snap, 30, rng, length=(0.002, 0.01), shape=[(0, 0.1), (snap, 1)]), 700, 4000))
    crack = shatter(rng, count=10, band=(1500, 4500), spread=0.1)
    thud = thump(130, 45, 0.5, 0.09, 1.8)
    ring = glass(note(D, 1), 0.8, 0.3) + 0.8 * glass(note(D, 1) * tritone, 0.8, 0.3)
    x = mix(0.4 * norm(tones), 0.3 * creaks, (snap, 0.8 * crack), (snap, 0.6 * thud), (snap + 0.02, 0.25 * ring))
    return finish(reverb(x, 1.0, 0.2), "impact")


# ================================================================ the dungeon bosses


def boss_phase(v, rng):
    """A boss gathering itself: a low, straining chord swells, sharpens, and breaks into a boom."""
    build = 2.2
    t = timeline(build)
    strain = 1 + 0.02 * (t / build) ** 2
    chord = sum(soft_saw(note(*n) * strain * (1 + 0.004 * np.sin(2 * np.pi * (4.5 + i) * t)), harmonics=8)
                for i, n in enumerate(((D, -1), (A, -1), (D, 0), (E, 0))))
    chord = lowpass(chord, 2400) * env(build, (0, 0), (build - 0.15, 1), (build, 0.5)) ** 2
    rush = moving_band(build, [(0, 400), (build, 2400)], 1.0, rng) * env(build, (0, 0), (build, 1)) ** 3
    boom = thump(85, 34, 1.4, 0.4, 2.2, knock=0.3)
    crash = norm(bandpass(noise(1.2, rng), 250, 2800)) * decay(1.2, 0.3, 0.004)
    x = mix(0.6 * norm(chord), 0.4 * rush, (build, 0.35 * boom), (build, 0.6 * crash))
    return finish(reverb(x, 2.2, 0.35, damp=3500), "grand")


def boss_rise(v, rng):
    """A boss waking: a deep toll, a rising swell and a crown of low bells."""
    dur = 3.2
    toll = clock_bell(note(D, -1), dur, 1.4, attack=0.004)
    rise = soft_saw(glide(dur, (0, note(D, -2)), (1.6, note(A, -1)), (dur, note(A, -1))), harmonics=6) * swell(dur, 1.6)
    breath = moving_band(dur, [(0, 400), (1.6, 1600), (dur, 800)], 1.0, rng) * swell(dur, 1.5)
    crown = mix(*[(1.2 + 0.12 * i, 0.35 * bell(note(d, 0), 1.8, 0.7, 2.0, 1.0, attack=0.01)) for i, d in enumerate((D, A, D + 5))])
    x = mix(0.7 * toll, 0.3 * lowpass(rise, 2000), 0.3 * breath, 1.6 * crown)
    return finish(reverb(x, 2.6, 0.4, damp=4000, predelay=0.03), "grand")


# ---------------------------------------------------------------- the Cinder Warden: iron, coals and chain


def warden_ambient(v, rng):
    dur = 1.8
    coals = norm(bandpass(brown(dur, rng), 180, 900)) * swell(dur, 0.7)
    crackle = norm(bandpass(grains(dur, 30 + 15 * v, rng, length=(0.001, 0.004), shape=[(0, 0.4), (0.8, 1), (dur, 0.2)]), 900, 5000))
    creak = soft_saw(glide(0.5, (0, 360 + 60 * v), (0.5, 280)), 0.5, harmonics=6) * chopper(0.5, rng, (40, 70), 0.2) * swell(0.5, 0.15)
    links = mix(*[(0.9 + 0.11 * i, 0.4 * tick(2200 + 300 * i, rng)) for i in range(3)])
    x = mix(0.5 * coals, 0.6 * crackle, (0.3, 0.4 * lowpass(creak, 1800)), 0.5 * links)
    return finish(reverb(x, 1.0, 0.15, damp=2500), "effect")


def warden_hurt(v, rng):
    dur = 1.4
    clang = partials(note(A, 0) * (1 + 0.03 * v), dur, ((1.0, 1.0, 1.0), (2.41, 0.5, 0.5), (3.98, 0.3, 0.3), (5.3, 0.15, 0.2)), 0.35)
    knock = thump(160, 60, 0.3, 0.05, 2.2)
    hiss = norm(bandpass(noise(dur, rng), 1500, 6000)) * env(dur, (0, 0), (0.08, 1), (dur, 0)) ** 2
    x = mix(0.8 * clang, 0.3 * knock, (0.05, 0.35 * hiss))
    return finish(reverb(x, 0.9, 0.18, damp=3000), "impact")


def warden_death(v, rng):
    dur = 4.0
    hiss = norm(bandpass(noise(dur, rng), 1200, 5000)) * env(dur, (0, 0), (0.2, 1), (2.5, 0.5), (dur, 0)) ** 1.5
    slump = mix(thump(110, 40, 1.0, 0.25, 2.4), (0.9, 0.9 * thump(90, 34, 1.2, 0.3, 2.4, knock=0.3)))
    groan = soft_saw(glide(2.4, (0, 220), (2.4, 110)), 2.4, harmonics=8) * swell(2.4, 0.4)
    ember = bell(note(D, 0), 2.0, 0.9, 2.0, 0.8, attack=0.02)
    x = mix(0.5 * hiss, 0.35 * slump, 0.35 * lowpass(groan, 900), (2.2, 0.4 * ember))
    return finish(reverb(x, 2.0, 0.3, damp=2500), "grand")


def warden_slam(v, rng):
    dur = 1.5
    thud = thump(95, 32, 1.1, 0.25, 3.0, knock=0.35)
    roar = moving_band(dur, [(0, 500), (0.1, 1800), (dur, 600)], 1.2, rng) * decay(dur, 0.4, 0.01)
    debris = norm(bandpass(grains(dur, 500, rng, length=(0.002, 0.01), shape=[(0, 1), (0.3, 0.4), (dur, 0)]), 200, 3000))
    x = mix(0.35 * thud, 0.8 * roar, 0.8 * debris)
    return finish(reverb(x, 1.0, 0.18, damp=2500), "impact")


def warden_immune(v, rng):
    dur = 0.7
    dead = partials(note(E, 0) * (1 + 0.05 * v), dur, ((1.0, 1.0, 1.0), (1.47, 0.6, 0.6), (2.09, 0.3, 0.4)), 0.05)
    knock = norm(bandpass(noise(0.12, rng), 300, 2200)) * decay(0.12, 0.02, 0.0005)
    x = mix(0.6 * lowpass(dead, 2500), 0.7 * knock)
    return finish(reverb(x, 0.4, 0.08, damp=2000), "effect")


# ---------------------------------------------------------------- the Star-Eater: hollow void and glass


def star_eater_ambient(v, rng):
    dur = 2.4
    t = timeline(dur)
    hum = (soft_saw(note(A, -1) * (1 + 0.01 * np.sin(2 * np.pi * 0.7 * t)), harmonics=5) + 0.5 * sine(note(D, 0) * 1.003, dur)) * swell(dur, 1.0)
    glitter = sparkle(dur, 6 + 2 * v, rng, [note(d, 2) for d in (D, FS, A, B)], tau=(0.1, 0.3), shape=[(0, 0.2), (1.0, 1), (dur, 0)])
    x = mix(0.6 * lowpass(hum, 1500), 0.4 * norm(glitter))
    return finish(reverb(x, 2.0, 0.4, damp=3500), "effect")


def star_eater_hurt(v, rng):
    dur = 1.2
    crack = shatter(rng, count=8, band=(1500, 5000), spread=0.05)
    groan = saturate(sine(glide(0.8, (0, note(D, -2) * (1 + 0.05 * v)), (0.8, note(A, -3)))), 1.8) * decay(0.8, 0.3, 0.01)
    ring = glass(note(FS, 1), dur, 0.25)
    x = mix(0.7 * crack, 0.5 * lowpass(groan, 1000), 0.25 * ring)
    return finish(reverb(x, 1.0, 0.25, damp=3000), "impact")


def star_eater_death(v, rng):
    pre = 2.2
    tone = bell(note(D, -1), 2.0, 0.6, 1.41, 3.0, attack=0.002) + norm(bandpass(noise(2.0, rng), 300, 1500)) * decay(2.0, 0.4)
    inward = norm(reverse(reverb(tone, 1.8, 0.6, damp=1800))[-samples(pre):])
    suck = moving_band(pre, [(0, 2000), (pre, 200)], 1.0, rng) * env(pre, (0, 0), (pre * 0.9, 1), (pre, 0)) ** 2
    boom = thump(70, 28, 2.0, 0.6, 2.4, knock=0.25)
    glints = sparkle(1.6, 16, rng, [note(d, 2) for d in (D, E, FS, A)], tau=(0.08, 0.2), shape=[(0, 1), (1.6, 0)])
    x = mix(0.8 * inward, 0.6 * suck, (pre, 0.35 * boom), (pre + 0.05, 0.35 * norm(glints)))
    return finish(reverb(x, 2.4, 0.4, damp=2500), "grand")


def star_eater_reflect(v, rng):
    pre = 0.28
    ping = glass(note(A, 1) * (1 + 0.02 * v), 0.6, 0.2)
    back = norm(reverse(reverb(ping, 0.5, 0.5))[-samples(pre):])
    snap = mix(glass(note(D, 2), 0.5, 0.08), 0.6 * norm(bandpass(noise(0.08, rng), 1500, 6000)) * decay(0.08, 0.012, 0.0005))
    x = mix(0.7 * back, (pre, 0.8 * snap))
    return finish(reverb(x, 0.7, 0.2), "effect")


def shard_parry(v, rng):
    dur = 1.0
    clang = partials(note(FS, 1) * (1 + 0.03 * v), dur, ((1.0, 1.0, 1.0), (2.76, 0.5, 0.45), (5.4, 0.25, 0.3)), 0.3)
    hit = norm(bandpass(noise(0.06, rng), 1200, 5000)) * decay(0.06, 0.01, 0.0003)
    x = mix(0.7 * clang, 0.6 * hit)
    return finish(reverb(x, 0.8, 0.2), "impact")


# ---------------------------------------------------------------- the Tide Scribe: water, bubbles and ink


def bubbles(seconds, rate, rng, low=300.0, high=900.0):
    """Little rising blips: each bubble a short sine sweeping up."""
    out = np.zeros(samples(seconds))
    for start in moments(seconds, rate, rng):
        f0 = rng.uniform(low, high)
        blip = sine(sweep(f0, f0 * 1.8, 0.05)) * decay(0.05, 0.015, 0.002)
        i = samples(start)
        out[i:i + len(blip)] += blip[:len(out) - i]
    return out


def tide_scribe_ambient(v, rng):
    dur = 2.0
    murmur = moving_band(dur, [(0, 350), (0.6, 600), (1.2, 400), (dur, 500)], 0.6, rng) * chopper(dur, rng, (4, 9), 0.2) * swell(dur, 0.8)
    pops = norm(bubbles(dur, 10 + 4 * v, rng))
    scratch = norm(bandpass(grains(0.5, 300, rng, length=(0.0005, 0.002)), 2000, 6000))[:samples(0.5)] * swell(0.5, 0.2)
    x = mix(0.6 * lowpass(murmur, 1500), 0.35 * pops, (1.2, 0.15 * scratch))
    return finish(reverb(x, 1.2, 0.25, damp=2500), "effect")


def tide_scribe_hurt(v, rng):
    dur = 0.9
    cry = saturate(sine(glide(0.6, (0, 380 + 40 * v), (0.6, 220))), 1.6) * chopper(0.6, rng, (18, 30), 0.3) * decay(0.6, 0.2, 0.01)
    splash = norm(bandpass(noise(dur, rng), 600, 4000)) * decay(dur, 0.15, 0.003)
    x = mix(0.6 * lowpass(cry, 1800), 0.4 * splash, 0.3 * norm(bubbles(0.6, 20, rng)))
    return finish(reverb(x, 0.8, 0.2, damp=2500), "impact")


def tide_scribe_death(v, rng):
    dur = 3.4
    sigh = saturate(sine(glide(2.6, (0, 300), (2.6, 110))), 1.5) * chopper(2.6, rng, (6, 12), 0.4) * swell(2.6, 0.3)
    fizz = norm(bubbles(dur, 30, rng, 200, 700)) * env(dur, (0, 1), (dur, 0))
    sink = thump(90, 30, 1.2, 0.4, 1.8)
    x = mix(0.5 * lowpass(sigh, 1500), 0.4 * fizz, (2.2, 0.6 * sink))
    return finish(reverb(x, 2.0, 0.35, damp=2200), "grand")


def tide_rise(v, rng):
    dur = 3.0
    rush = moving_band(dur, [(0, 250), (dur * 0.7, 1300), (dur, 900)], 1.4, rng) * swell(dur, dur * 0.75, 1.0)
    swash = norm(lowpass(brown(dur, rng), 400)) * swell(dur, dur * 0.7)
    pops = norm(bubbles(dur, 18, rng, 250, 800)) * env(dur, (0, 0.2), (dur, 1))
    tone = sine(glide(dur, (0, note(D, -2)), (dur, note(A, -2)))) * swell(dur, dur * 0.8)
    x = mix(0.7 * rush, 0.5 * swash, 0.25 * pops, 0.25 * tone)
    return finish(reverb(x, 1.8, 0.3, damp=2500), "grand")


def tide_ebb(v, rng):
    dur = 2.6
    drain = moving_band(dur, [(0, 1100), (dur, 250)], 1.2, rng) * swell(dur, 0.4, 1.2)
    gurgle = norm(bubbles(dur, 24, rng, 150, 500)) * env(dur, (0, 0.5), (1.5, 1), (dur, 0))
    tone = sine(glide(dur, (0, note(A, -2)), (dur, note(D, -2)))) * swell(dur, 0.5)
    x = mix(0.7 * drain, 0.45 * gurgle, 0.2 * tone)
    return finish(reverb(x, 1.6, 0.3, damp=2500), "grand")


# ================================================================ writing it all out


def palette():
    """Every event: (name, folder, file stem, builder, variants, extra sounds.json fields per file)."""
    events = []
    for element in ELEMENTS:
        events.append((f"cast_{element}", "cast", element, CASTS[element], 2, {}))
        events.append((f"impact_{element}", "impact", element, IMPACTS[element], 3, {}))
    far = {"attenuation_distance": 32}
    events += [
        ("charge_loop", "casting", "charge_loop", charge_loop, 1, {}),
        ("charge_full", "casting", "charge_full", charge_full, 1, {}),
        ("release", "casting", "release", release, 2, {}),
        ("circle_open", "casting", "circle_open", circle_open, 1, {}),
        ("beam_fire", "casting", "beam_fire", beam_fire, 2, {}),
        ("orb_hum", "casting", "orb_hum", orb_hum, 1, {}),
        ("shield_up", "casting", "shield_up", shield_up, 1, {}),
        ("shield_break", "casting", "shield_break", shield_break, 2, {}),
        ("shield_block", "casting", "shield_block", shield_block, 2, {}),
        ("imbue", "casting", "imbue", imbue, 1, {}),
        ("domain_open", "casting", "domain_open", domain_open, 1, far),
        ("domain_close", "casting", "domain_close", domain_close, 1, far),
        ("blink", "casting", "blink", blink, 2, {}),
        ("magic_break", "casting", "magic_break", magic_break, 3, {}),
        ("rune_thread", "ui", "rune_thread", rune_thread, 3, {}),
        ("rune_unthread", "ui", "rune_unthread", rune_unthread, 2, {}),
        ("wheel_open", "ui", "wheel_open", wheel_open, 1, {}),
        ("wheel_hover", "ui", "wheel_hover", wheel_hover, 3, {}),
        ("wheel_select", "ui", "wheel_select", wheel_select, 1, {}),
        ("discovery", "ui", "discovery", discovery, 1, {}),
        ("circle_formed", "heart", "circle_formed", circle_formed, 1, {}),
        ("overcast", "heart", "overcast", overcast, 1, {}),
    ]
    boss = {"attenuation_distance": 48}
    tide = {"attenuation_distance": 40}
    events += [
        ("boss_phase", "boss", "phase", boss_phase, 1, boss),
        ("boss_rise", "boss", "rise", boss_rise, 1, boss),
        ("warden_ambient", "boss", "warden_ambient", warden_ambient, 2, {}),
        ("warden_hurt", "boss", "warden_hurt", warden_hurt, 2, {}),
        ("warden_death", "boss", "warden_death", warden_death, 1, boss),
        ("warden_slam", "boss", "warden_slam", warden_slam, 2, {}),
        ("warden_immune", "boss", "warden_immune", warden_immune, 2, {}),
        ("star_eater_ambient", "boss", "star_eater_ambient", star_eater_ambient, 2, {}),
        ("star_eater_hurt", "boss", "star_eater_hurt", star_eater_hurt, 2, {}),
        ("star_eater_death", "boss", "star_eater_death", star_eater_death, 1, boss),
        ("star_eater_reflect", "boss", "star_eater_reflect", star_eater_reflect, 2, {}),
        ("shard_parry", "boss", "shard_parry", shard_parry, 2, {}),
        ("tide_scribe_ambient", "boss", "tide_scribe_ambient", tide_scribe_ambient, 2, {}),
        ("tide_scribe_hurt", "boss", "tide_scribe_hurt", tide_scribe_hurt, 2, {}),
        ("tide_scribe_death", "boss", "tide_scribe_death", tide_scribe_death, 1, boss),
        ("tide_rise", "boss", "tide_rise", tide_rise, 1, tide),
        ("tide_ebb", "boss", "tide_ebb", tide_ebb, 1, tide),
    ]
    return events


def encode(x, path, scratch):
    path.parent.mkdir(parents=True, exist_ok=True)
    wavfile.write(scratch, SR, x.astype(np.float32))
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(scratch), "-ac", "1", "-c:a", "libvorbis", "-q:a", "5",
                    "-map_metadata", "-1", "-fflags", "+bitexact", "-flags:a", "+bitexact", str(path)], check=True)


def decode(path):
    raw = subprocess.run(["ffmpeg", "-loglevel", "error", "-i", str(path), "-f", "f32le", "-ac", "1", "-"],
                         check=True, capture_output=True).stdout
    return np.frombuffer(raw, dtype=np.float32).astype(float)


def db(value):
    return 20 * np.log10(max(value, 1e-9))


def check(name, x, path, loop):
    """Measures a written sound (decoded back from the Ogg) and complains if anything is off."""
    y = decode(path)
    power = np.abs(np.fft.rfft(y)) ** 2
    freqs = np.fft.rfftfreq(len(y), 1 / SR)
    row = {"file": path.relative_to(SOUNDS).as_posix(), "seconds": len(x) / SR, "peak": db(np.max(np.abs(y))),
           "loud": db(loudness(y)), "dc": float(np.mean(x)), "centroid": float(np.sum(freqs * power) / np.sum(power)),
           "mid": float(np.sum(power[(freqs > 250) & (freqs < 5000)]) / np.sum(power)),
           "high": float(np.sum(power[freqs > 8000]) / np.sum(power)), "kb": path.stat().st_size / 1024}
    problems = []
    if row["peak"] > -0.3:
        problems.append(f"clips ({row['peak']:.1f} dBFS)")
    if abs(row["dc"]) > 1e-3:
        problems.append(f"DC offset {row['dc']:.4f}")
    if row["mid"] < 0.1:
        problems.append(f"only {row['mid']:.0%} of its energy where small speakers can play it (250 Hz-5 kHz)")
    if row["high"] > 0.05:
        problems.append(f"{row['high']:.0%} of its energy above 8 kHz")
    if loop:
        # Across the seam the wave must move no more than it does anywhere else.
        seam = abs(x[0] - x[-1])
        if seam > np.percentile(np.abs(np.diff(x)), 99.9) * 1.05:
            problems.append(f"loop seam jumps by {seam:.4f}")
    elif max(abs(x[0]), abs(x[-1])) > 1e-3:
        problems.append("starts or ends away from zero (a click)")
    for problem in problems:
        print(f"  !! {name}: {problem}")
    return row


def main():
    rows = []
    written = set()
    events = {}
    with tempfile.TemporaryDirectory() as tmp:
        scratch = Path(tmp) / "sound.wav"
        for name, folder, stem, build, variants, extra in palette():
            entries = []
            for v in range(variants):
                rng = np.random.default_rng(zlib.crc32(f"{name}/{v}".encode()))
                x = build(v, rng)
                rel = f"{folder}/{stem}_{v + 1}" if variants > 1 else f"{folder}/{stem}"
                path = SOUNDS / f"{rel}.ogg"
                encode(x, path, scratch)
                written.add(path)
                rows.append(check(name, x, path, loop=name in ("charge_loop", "orb_hum")))
                entries.append({"name": f"wildercord:{rel}", **extra} if extra else f"wildercord:{rel}")
            events[name] = {"subtitle": f"subtitles.wildercord.{name}", "sounds": entries}
    for stale in SOUNDS.rglob("*.ogg"):
        if stale not in written:
            print(f"removing {stale.relative_to(ROOT).as_posix()}, no longer made")
            stale.unlink()
    (ASSETS / "sounds.json").write_text(json.dumps(events, indent=2) + "\n", encoding="utf-8", newline="\n")

    print(f"{'file':34} {'sec':>5} {'peak':>6} {'loud':>6} {'centre':>7} {'mid':>5} {'>8k':>5} {'KB':>5}")
    for r in rows:
        print(f"{r['file']:34} {r['seconds']:5.2f} {r['peak']:6.1f} {r['loud']:6.1f} {r['centroid']:7.0f} {r['mid']:5.0%} {r['high']:5.1%} {r['kb']:5.1f}")
    total = sum(r["kb"] for r in rows)
    print(f"{len(events)} events, {len(rows)} files, {sum(r['seconds'] for r in rows):.1f} s, {total / 1024:.2f} MB")

    lang = ASSETS / "lang/en_us.json"
    if lang.exists():
        known = json.loads(lang.read_text(encoding="utf-8"))
        missing = [e["subtitle"] for e in events.values() if e["subtitle"] not in known]
        if missing:
            print(f"  !! {len(missing)} subtitles have no English text yet: add them to NEW_LANG in generate_assets.py "
                  f"and run it (first: {missing[0]})")


if __name__ == "__main__":
    main()
