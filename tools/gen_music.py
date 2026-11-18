#!/usr/bin/env python3
"""Generates the boss music for The Memory Shatterer (pure Python, no samples, no dependencies).

A loopable jungle / drum & bass track at about 172 BPM in D minor: an amen-style break that is
chopped, stuttered and resequenced, a reese and sub bass, dark chord stabs and a glitchy music box.
Every sound is synthesized from scratch. The mix is circular, so tails wrap round into bar 1 and the
loop has no seam. The WAV goes to the temp dir and is encoded with oggenc (vorbis-tools).

Run from the project root:  python3 tools/gen_music.py
"""
import array, math, os, random, shutil, subprocess, sys, tempfile, wave
from itertools import accumulate, chain, repeat
from operator import add, mul

OUT = "src/main/resources/assets/sorrowed/sounds/music/memory_shatterer.ogg"
SR = 44100
STEP = 3846                            # a 16th note in samples: 172.004 BPM
BAR = 16 * STEP
SECTIONS = [("intro", 4), ("a", 16), ("break", 4), ("b", 16), ("final", 12)]
BARS = sum(n for _, n in SECTIONS)
N = BARS * BAR                         # the loop: 52 bars, 72.6 s
TAU = 2 * math.pi

# ------------------------------------------------------------------ helpers

def mtof(m):
    return 440.0 * 2.0 ** ((m - 69) / 12.0)

def at(bar, step, off=0.0):
    return int(round((bar * 16 + step + off) * STEP))

def locate(bar):
    for name, n in SECTIONS:
        if bar < n:
            return name, bar
        bar -= n

def pan_gains(p):
    a = (p + 1.0) * math.pi / 4.0
    return math.cos(a), math.sin(a)

def new():
    return array.array("d", bytes(8 * N))

def mix(buf, x, pos, gain=1.0):
    """Add x into the circular buffer at pos: anything past the loop end wraps to the start."""
    if gain != 1.0:
        x = [s * gain for s in x]
    pos %= N
    n = len(x)
    if pos + n <= N:
        buf[pos:pos + n] = array.array("d", map(add, buf[pos:pos + n], x))
    else:
        k = N - pos
        buf[pos:] = array.array("d", map(add, buf[pos:], x[:k]))
        buf[:n - k] = array.array("d", map(add, buf[:n - k], x[k:]))

# ------------------------------------------------------------------ dsp

def noise(rnd, n):
    r = rnd.random
    return [r() * 2.0 - 1.0 for _ in range(n)]

def lowpass(x, fc):
    a = 1.0 - math.exp(-TAU * fc / SR)
    y, out = 0.0, []
    for s in x:
        y += a * (s - y)
        out.append(y)
    return out

def highpass(x, fc, poles=1):
    for _ in range(poles):
        x = [s - l for s, l in zip(x, lowpass(x, fc))]
    return x

def sweep(x, fc_at, q, band=False, block=64):
    """Resonant 2-pole low-pass (or band-pass); the cutoff in Hz is fc_at(sample), refreshed per block."""
    n = len(x)
    out = [0.0] * n
    low = bnd = 0.0
    for k in range(0, n, block):
        f = 2.0 * math.sin(math.pi * min(max(fc_at(k), 20.0), 9000.0) / SR)
        e = min(k + block, n)
        if band:
            for i in range(k, e):
                low += f * bnd
                bnd += f * (x[i] - low - q * bnd)
                out[i] = bnd
        else:
            for i in range(k, e):
                low += f * bnd
                bnd += f * (x[i] - low - q * bnd)
                out[i] = low
    return out

def saw(incs, ph=0.0):
    """PolyBLEP sawtooth; incs is the phase step (freq / SR) for each sample."""
    out = []
    ap = out.append
    for dt in incs:
        ph += dt
        if ph >= 1.0:
            ph -= 1.0
        v = 2.0 * ph - 1.0
        if ph < dt:
            t = ph / dt
            v -= t + t - t * t - 1.0
        elif ph > 1.0 - dt:
            t = (ph - 1.0) / dt
            v -= t * t + t + t + 1.0
        ap(v)
    return out

def norm(x, peak=1.0):
    m = max(max(x), -min(x)) or 1.0
    k = peak / m
    return [s * k for s in x]

def fade_out(x, n):
    n = min(n, len(x))
    k = len(x) - n
    return x[:k] + [s * 0.5 * (1.0 + math.cos(math.pi * i / n)) for i, s in enumerate(x[k:])]

def gate(total, n_on, atk, rel):
    """Attack ramp, hold until n_on, then a raised-cosine release over rel samples."""
    out = []
    for i in range(total):
        g = i / atk if i < atk else 1.0
        if i >= n_on:
            g *= max(0.0, 0.5 * (1.0 + math.cos(math.pi * (i - n_on) / rel)))
        out.append(g)
    return out

def resample(x, ratio):
    out = []
    for i in range(int((len(x) - 1) / ratio)):
        j = i * ratio
        k = int(j)
        out.append(x[k] + (x[k + 1] - x[k]) * (j - k))
    return out

# ------------------------------------------------------------------ drum voices

METAL = (205.3, 304.4, 369.6, 522.7, 540.0, 800.0)   # the six square oscillators of an electronic hat

def kick(rnd, f0=165.0, f1=46.0, pitch_tau=0.022, tau=0.105, drive=1.8, length=0.4):
    n = int(length * SR)
    ph, out = 0.0, []
    for i in range(n):
        t = i / SR
        ph += TAU * (f1 + (f0 - f1) * math.exp(-t / pitch_tau)) / SR
        out.append(math.sin(ph) * math.exp(-t / tau) * min(1.0, i / 32.0))
    click = highpass(noise(rnd, 220), 1800.0)
    for i in range(220):
        out[i] += 0.5 * click[i] * math.exp(-i / 55.0)
    return fade_out(norm([math.tanh(drive * s) for s in out]), int(0.03 * SR))

def snare(rnd, decay=0.075, length=0.34, body=0.6, room=0.1, hp=1800.0):
    n = int(length * SR)
    snap = highpass(noise(rnd, n), hp, 2)
    wash = lowpass(noise(rnd, n), 4500.0)
    ph1 = ph2 = 0.0
    out = []
    for i in range(n):
        t = i / SR
        ph1 += TAU * (188.0 + 75.0 * math.exp(-t / 0.011)) / SR
        ph2 += TAU * (335.0 + 110.0 * math.exp(-t / 0.008)) / SR
        tone = body * (math.sin(ph1) * math.exp(-t / 0.05) + 0.45 * math.sin(ph2) * math.exp(-t / 0.03))
        crack = snap[i] * (math.exp(-t / decay) + 2.0 * math.exp(-t / 0.0025))
        out.append(tone + 0.55 * crack + room * wash[i] * math.exp(-t / 0.17))
    return fade_out(norm([math.tanh(1.4 * s) for s in out]), int(0.03 * SR))

def cymbal(rnd, decay, length, hp=6500.0, metallic=0.6, bell=0.0, scale=2.7):
    """Hats and ride: metallic squares + noise, high-passed; bell adds the ride's inharmonic ping."""
    n = int(length * SR)
    m = [0.0] * n
    for f in METAL:
        w = TAU * f * scale / SR
        m = [a + (1.0 if math.sin(w * i) >= 0.0 else -1.0) for i, a in enumerate(m)]
    nz = noise(rnd, n)
    x = highpass([metallic * a / 6.0 + (1.0 - metallic) * b for a, b in zip(m, nz)], hp, 3)
    out = []
    for i in range(n):
        t = i / SR
        v = x[i] * math.exp(-t / decay) * min(1.0, i / 12.0)
        if bell:
            v += bell * (0.5 * math.sin(TAU * 2480.0 * t) * math.exp(-t / 0.12)
                         + 0.35 * math.sin(TAU * 3720.0 * t) * math.exp(-t / 0.08)
                         + 0.25 * math.sin(TAU * 5300.0 * t) * math.exp(-t / 0.05))
        out.append(v)
    return fade_out(norm(out), int(0.01 * SR))

def crash(rnd, length=3.0):
    """Stereo crash: two different noise/metal mixes for width."""
    n = int(length * SR)
    pair = []
    for _ in range(2):
        m = [0.0] * n
        for f in METAL:
            w = TAU * f * 1.9 * rnd.uniform(0.98, 1.02) / SR
            m = [a + (1.0 if math.sin(w * i) >= 0.0 else -1.0) for i, a in enumerate(m)]
        x = highpass([0.45 * a / 6.0 + 0.55 * b for a, b in zip(m, noise(rnd, n))], 2200.0, 2)
        pair.append([s * (0.45 * math.exp(-i / (0.2 * SR)) + 0.55 * math.exp(-i / (1.0 * SR))) * min(1.0, i / 90.0)
                     for i, s in enumerate(x)])
    k = max(max(map(abs, pair[0])), max(map(abs, pair[1])))
    return [fade_out([s / k for s in c], int(0.4 * SR)) for c in pair]

def boom(rnd):
    """Sub impact for the drop into the break."""
    n = int(1.6 * SR)
    ph, out = 0.0, []
    for i in range(n):
        t = i / SR
        ph += TAU * (34.0 + 90.0 * math.exp(-t / 0.08)) / SR
        out.append(math.sin(ph) * math.exp(-t / 0.55))
    nz = lowpass(noise(rnd, n), 600.0)
    out = [a + 0.9 * b * math.exp(-i / (0.25 * SR)) for i, (a, b) in enumerate(zip(out, nz))]
    return fade_out(norm([math.tanh(1.5 * s) for s in out]), int(0.2 * SR))

def riser(rnd, n, f0, f1, tonal):
    """Band-passed noise swept up and swelling; tonal adds a dissonant rising pair."""
    x = sweep(noise(rnd, n), lambda k: f0 * (f1 / f0) ** (k / n), 0.3, band=True)
    out = [s * (i / n) ** 2.2 for i, s in enumerate(norm(x))]
    if tonal:
        ph, tn = 0.0, []
        for i in range(n):
            ph += TAU * 146.8 * 4.0 ** (i / n) / SR
            tn.append(math.sin(ph) + 0.8 * math.sin(ph * 1.0595))
        tn = norm(tn)
        out = [o + tonal * t * (i / n) ** 3.0 for i, (o, t) in enumerate(zip(out, tn))]
    return fade_out(norm(out), 130)

def make_kit():
    r = random.Random(11)
    return {
        "kick": [kick(r), kick(r, f0=172.0, f1=47.0), kick(r, f0=160.0, tau=0.1)],
        "kick_s": [kick(r, tau=0.04, length=0.14, drive=2.2)],
        "snare": [snare(r) for _ in range(3)],
        "ghost": [snare(r, decay=0.03, length=0.16, body=0.35, room=0.0, hp=2600.0) for _ in range(3)],
        "roll": [snare(r, decay=0.04, length=0.12, body=0.5, room=0.0) for _ in range(3)],
        "hat": [cymbal(r, 0.02, 0.12, hp=7000.0, metallic=0.35) for _ in range(4)],
        "ride": [cymbal(r, 0.11, 0.5, hp=5200.0, metallic=0.45, bell=0.1) for _ in range(3)],
        "open": [cymbal(r, 0.22, 0.7, hp=6000.0, metallic=0.4) for _ in range(2)],
        "crash": crash(r),
        "boom": [boom(r)],
    }

# ------------------------------------------------------------------ the break, chopped
# A bar is 16 slices (one per 16th); a slice is a list of (voice, velocity, offset in 16ths).

def amen(kicks, snares, ride=range(0, 16, 2), accent=(), extra=()):
    c = [[] for _ in range(16)]
    for s, v in kicks:
        c[s].append(("kick", v, 0.0))
    for s, v in snares:
        c[s].append(("snare" if v > 0.7 else "ghost", v, 0.0))
    for s in ride:
        c[s].append(("ride", 1.0 if s in accent else (0.8 if s % 4 == 0 else 0.6), 0.0))
    for s, voice, v in extra:
        c[s].append((voice, v, 0.0))
    return c

# the classic break: two identical bars, the displaced third bar, and the crash-led fourth
BRK1 = amen([(0, 1), (2, .8), (10, .9), (11, .7)], [(4, 1), (7, .55), (9, .5), (12, 1), (15, .5)])
BRK2 = amen([(0, 1), (2, .8), (10, .9)], [(4, 1), (7, .55), (9, .5), (14, .95)], accent=(10,))
BRKF = amen([(2, .9), (3, .8), (10, .9)], [(1, .4), (4, 1), (7, .55), (9, .5), (14, .95)],
            ride=[s for s in range(0, 16, 2) if s != 10], extra=[(10, "open", 1.0)])

SHORT = {"kick": "kick_s", "snare": "roll", "ghost": "roll", "ride": "hat", "open": "hat"}

def chop(c, order):
    return [list(c[i]) for i in order]

def beats(*order):
    return [b * 4 + i for b in order for i in range(4)]

def half_repeat(c):
    return chop(c, list(range(8)) * 2)

def half_swap(c):
    return chop(c, list(range(8, 16)) + list(range(8)))

def flip(c, beat):
    o = list(range(16))
    o[beat * 4:beat * 4 + 4] = o[beat * 4:beat * 4 + 4][::-1]
    return chop(c, o)

def strip(c, *voices, start=0):
    return [[h for h in sl if h[0] not in voices] if s >= start else list(sl) for s, sl in enumerate(c)]

def add_hits(c, hits):
    c = [list(sl) for sl in c]
    for s, v, vel, off in hits:
        c[s].append((v, vel, off))
    return c

def hats16(c, vel=0.3):
    return add_hits(c, [(s, "hat", vel + (0.08 if s % 4 == 3 else 0.0), 0.0) for s in range(1, 16, 2)])

def roll(c, start, divs, v0, v1):
    """Snare roll from step `start`, divs[i] hits in each following 16th, crescendo v0 -> v1."""
    n = sum(divs)
    hits, k = [], 0
    for i, d in enumerate(divs):
        for j in range(d):
            hits.append((start + i, "roll", v0 + (v1 - v0) * k / max(1, n - 1), j / d))
            k += 1
    return add_hits(strip(c, "snare", "ghost", start=start), hits)

def stutter(c, step, div=2, vel=0.9):
    """Retrigger the slice at `step` div times per 16th to the end of the bar, with short voices."""
    src = c[step]
    out = [list(sl) for sl in c]
    for s in range(step, 16):
        ramp = 0.7 + 0.3 * (s - step) / max(1, 15 - step)
        out[s] = [(SHORT.get(v, v), hv * vel * ramp, k / div) for k in range(div) for v, hv, _ in src]
    return out

def drum_plan(sec, p):
    """The bar's slices plus one-off hits (step, voice, velocity) such as crashes."""
    base = BRK1 if p % 2 == 0 else BRK2
    if sec == "intro":
        if p == 0:
            return strip(BRK1, "snare", "ghost"), [(0, "crash", 1.0)]
        if p == 1:
            return strip(BRK1, "ghost"), []
        if p == 2:
            return hats16(BRK2), []
        return roll(hats16(BRKF), 12, [2] * 4, 0.3, 1.0), []
    if sec == "a":
        extra = [(0, "crash", 1.0)] if p == 0 else [(0, "crash", 0.6)] if p == 8 else []
        if p % 4 != 3:
            return base, extra
        return [half_repeat(BRK2),
                roll(half_repeat(BRK2), 12, [1] * 4, 0.4, 1.0),
                flip(BRK2, 3),
                roll(BRKF, 8, [1] * 4 + [2] * 4, 0.3, 1.0)][p // 4], extra
    if sec == "break":
        if p == 0:
            return amen([(0, 1), (10, .8)], [(8, 1), (14, .45), (15, .5)], ride=(0, 4, 8, 12)), [(0, "boom", 1.0)]
        if p == 1:
            return stutter(chop(BRK1, beats(0, 2, 0, 3)), 14), []
        if p == 2:
            return add_hits(chop(BRK2, beats(1, 3, 1, 3)), [(0, "kick", 1.0, 0.0), (8, "kick", 1.0, 0.0)]), []
        return roll(amen([(0, 1), (8, .9)], [], ride=()), 0, [1] * 10 + [2] * 6, 0.25, 1.0), []
    if sec == "b":
        extra = [(0, "crash", 1.0)] if p == 0 else [(0, "crash", 0.6)] if p == 8 else []
        c = hats16(base) if p % 4 >= 2 or p >= 8 else base
        if p % 4 != 3:
            return c, extra
        return [half_swap(hats16(BRK2)),
                roll(stutter(half_repeat(hats16(BRK1)), 12), 14, [2, 2], 0.6, 1.0),
                add_hits(flip(hats16(BRK2), 3), [(14, "kick", .9, 0.0), (15, "kick", .8, 0.0)]),
                roll(hats16(BRKF), 6, [1] * 6 + [2] * 4, 0.3, 1.0)][p // 4], extra
    extra = [(0, "crash", 1.0)] if p == 0 else [(0, "crash", 0.6)] if p == 4 else []
    c = hats16(base)
    if p < 8:
        if p == 3:
            return half_swap(c), extra
        if p == 7:
            return roll(stutter(add_hits(half_repeat(hats16(BRK1)), [(9, "kick", .7, 0.0), (11, "kick", .8, 0.0)]), 12),
                        14, [2, 2], 0.5, 1.0), extra
        return c, extra
    return [half_repeat(hats16(BRK1)),
            add_hits(flip(hats16(BRK2), 2), [(s, "kick_s", .6 + .1 * (s - 12), 0.0) for s in range(12, 16)]),
            roll(hats16(BRK1), 4, [1] * 8 + [2] * 4, 0.3, 1.0),
            roll(hats16(BRKF), 0, [1] * 8 + [2] * 8, 0.25, 1.0)][p - 8], extra

JIT = {"kick": 30, "kick_s": 15, "snare": 45, "ghost": 60, "roll": 15, "hat": 70, "ride": 60, "open": 40}
LATE = {"ghost": 40, "hat": 10}
GAIN = {"kick": 1.0, "kick_s": 0.8, "snare": 0.85, "ghost": 0.7, "roll": 0.7, "hat": 0.28, "ride": 0.32, "open": 0.32, "boom": 0.8}
SEND = {"snare": 0.28, "ghost": 0.12, "roll": 0.1, "open": 0.15, "ride": 0.04}

def pan_of(voice, step):
    if voice == "hat":
        return -0.3 if (step // 2) % 2 == 0 else 0.3
    return {"ride": 0.25, "open": -0.25, "roll": 0.1 if step % 2 else -0.1}.get(voice, 0.0)

def drum_events(rnd):
    ev, kicks = [], []
    def put(bar, step, voice, vel, off=0.0, human=True):
        pos = at(bar, step, off)
        if human:
            pos += int(rnd.gauss(0.0, JIT.get(voice, 40))) + LATE.get(voice, 0)
            vel *= 1.0 + rnd.uniform(-0.07, 0.07)
        ev.append((pos, voice, vel, pan_of(voice, step)))
        if voice in ("kick", "kick_s"):
            kicks.append((pos, vel))
    for bar in range(BARS):
        sec, p = locate(bar)
        slices, extra = drum_plan(sec, p)
        for step, sl in enumerate(slices):
            for voice, vel, off in sl:
                put(bar, step, voice, vel, off)
        for step, voice, vel in extra:
            put(bar, step, voice, vel, human=False)
    return ev, kicks

def render_drums(ev, kit, rnd):
    dc, dl, dr, send = new(), new(), new(), new()
    for pos, voice, vel, pan in ev:
        if voice == "crash":
            mix(dl, kit["crash"][0], pos, 0.42 * vel)
            mix(dr, kit["crash"][1], pos, 0.42 * vel)
            mix(send, kit["crash"][0], pos, 0.1 * vel)
            continue
        shots = kit[voice]
        shot = shots[rnd.randrange(len(shots))]
        g = GAIN[voice] * vel
        if pan == 0.0:
            mix(dc, shot, pos, g)
        else:
            gl, gr = pan_gains(pan)
            mix(dl, shot, pos, g * gl * 1.414)
            mix(dr, shot, pos, g * gr * 1.414)
        if voice in SEND:
            mix(send, shot, pos, g * SEND[voice])
    return dc, dl, dr, send

# ------------------------------------------------------------------ bass

D2, EB2, F2, G2, A2, BB2, C3, D3, E3 = 38, 39, 41, 43, 45, 46, 48, 50, 52
# notes per bar: (step, length in 16ths, midi, velocity, glide-from midi)
RIFF_A = [
    [(0, 5, D2, 1.0, None), (5, 1, D2, .7, None), (6, 2, F2, .9, D2), (8, 2, D2, .95, None), (10, 1, D2, .9, None),
     (11, 1, D2, .8, None), (12, 2, EB2, .95, None), (14, 2, D2, 1.0, None)],
    [(0, 6, D2, 1.0, None), (6, 2, A2, .9, D2), (8, 2, G2, .9, None), (10, 2, F2, .9, None), (12, 4, D2, 1.0, None)],
    [(0, 6, BB2, 1.0, None), (6, 2, BB2, .8, None), (8, 4, A2, .9, BB2), (12, 2, G2, .9, None), (14, 2, A2, .9, None)],
    [(0, 6, A2, 1.0, None), (6, 2, A2, .8, None), (8, 2, BB2, .9, None), (10, 2, A2, .9, None), (12, 2, G2, .85, None),
     (14, 2, D2, 1.0, None)],
]
RIFF_B = [
    [(0, 3, D2, 1.0, None), (3, 3, D2, .85, None), (6, 2, F2, .9, D2), (8, 4, D2, 1.0, None), (12, 2, EB2, .9, None), (14, 2, D2, .95, None)],
    [(0, 3, BB2, 1.0, None), (3, 3, BB2, .85, None), (6, 2, D3, .9, BB2), (8, 4, BB2, 1.0, None), (12, 2, A2, .9, None), (14, 2, BB2, .9, None)],
    [(0, 3, G2, 1.0, None), (3, 3, G2, .85, None), (6, 2, BB2, .9, G2), (8, 4, G2, 1.0, None), (12, 2, F2, .9, None), (14, 2, G2, .9, None)],
    [(0, 3, A2, 1.0, None), (3, 3, A2, .85, None), (6, 2, E3, .9, A2), (8, 2, A2, .95, None), (10, 2, BB2, .9, None),
     (12, 2, A2, .9, None), (14, 2, D2, 1.0, None)],
]
RIFF_C = RIFF_B[:2] + [
    [(0, 3, C3, 1.0, None), (3, 3, C3, .85, None), (6, 2, E3, .9, C3), (8, 4, C3, 1.0, None), (12, 2, BB2, .9, None), (14, 2, A2, .9, None)],
] + RIFF_B[3:]
RIFF_INTRO = [
    [(0, 16, D2, 1.0, None)],
    [(0, 6, D2, 1.0, None), (8, 6, D2, .95, None), (14, 2, EB2, .9, None)],
    RIFF_A[0], RIFF_A[1],
]

def riff_for(sec, p):
    if sec == "intro":
        return RIFF_INTRO[p]
    if sec == "a":
        return RIFF_A[p % 4]
    if sec == "b":
        return RIFF_B[p % 4]
    if sec == "final":
        return RIFF_C[p % 4] if p < 8 else RIFF_A[p % 4]
    return []

def bass_cut(pos):
    """Reese cutoff (Hz): opens through the intro, wanders slowly, closes again into the loop point."""
    bar = pos / BAR
    if bar < 4:
        return 120.0 * (900.0 / 120.0) ** (bar / 4.0)
    if bar >= 48:
        return 760.0 * (125.0 / 760.0) ** ((bar - 48.0) / 4.0)
    base = 520.0 if bar < 24 else 640.0 if bar < 40 else 720.0
    return base * 2.0 ** (1.15 * math.sin(TAU * (bar - 4.0) / 4.0 + 0.64))

DET = (-0.0125, 0.0, 0.0105)

def reese(f_to, f_from, n, rel, fc_at):
    """One bass note: three detuned saws -> two resonant low-pass stages -> soft clip."""
    total = n + rel
    if f_from == f_to:
        fs = [f_to] * total
    else:
        k = 1.0 / (0.02 * SR)
        fs = [f_to + (f_from - f_to) * math.exp(-i * k) for i in range(total)]
    x = None
    for d, ph in zip(DET, (0.0, 0.37, 0.71)):
        s = saw([f * (1.0 + d) / SR for f in fs], ph)
        x = s if x is None else [a + b for a, b in zip(x, s)]
    x = [math.tanh(0.9 * s) for s in sweep(sweep(x, fc_at, 0.6), fc_at, 1.0)]
    m = sum(x) / total                    # saturating a filtered saw leaves a DC offset; remove it before the envelope
    return [(s - m) * e for s, e in zip(x, gate(total, n, 110, rel))]

def sub(f_to, f_from, n, rel):
    total = n + rel
    k = 1.0 / (0.02 * SR)
    ph = accumulate(TAU * (f_to + (f_from - f_to) * math.exp(-i * k)) / SR for i in range(total))
    return [math.sin(p) * e for p, e in zip(ph, gate(total, n, 130, rel))]

def render_bass():
    bass = new()
    for bar in range(BARS):
        sec, p = locate(bar)
        for st, ln, midi, vel, gl in riff_for(sec, p):
            pos = at(bar, st)
            n = ln * STEP - 90
            f_to = mtof(midi)
            f_from = mtof(gl) if gl else f_to
            fc_at = lambda k, pos=pos: bass_cut(pos + k) * (1.0 + 2.0 * math.exp(-k / (0.11 * SR)))
            mix(bass, reese(f_to, f_from, n, 880, fc_at), pos, 0.5 * vel)
            if not (sec == "intro" and p < 2):
                mix(bass, sub(f_to / 2, f_from / 2, n, 880), pos, 0.62 * vel)
    return bass

# ------------------------------------------------------------------ harmony

CHORD = {"Dm": [50, 53, 57, 60], "Bb": [46, 53, 58, 62], "Gm": [55, 58, 62, 65], "A": [52, 55, 57, 61], "C": [48, 55, 60, 64]}
PROG = {"intro": "Dm Dm Dm Dm".split(), "a": "Dm Dm Bb A".split(), "break": "Dm Bb Gm A".split(),
        "b": "Dm Bb Gm A".split(), "final": "Dm Bb C A".split()}
S_A = [(3, 2), (6, 1), (10, 2), (14, 1)]
S_B = [(0, 2), (3, 2), (6, 2), (10, 2), (12, 1), (14, 2)]
S_L = [(0, 12)]

def stab_plan(sec, p):
    chord = PROG[sec][p % 4]
    if sec == "a":
        return (chord, S_A) if p % 4 >= 2 or p >= 8 else None
    if sec == "break":
        return chord, [S_L, S_A, S_B, S_B][p]
    if sec == "b":
        return chord, S_A if p % 4 == 0 else S_B
    if sec == "final":
        return (chord, S_B if p < 8 else S_A) if p < 10 else None
    return None

def stab(notes, n):
    """Detuned saw chord with a closing filter, rendered twice with different detune for width."""
    total = n + 1300
    env = gate(total, n, 110, 1300)
    chans = []
    for dets in ((-0.007, 0.0, 0.004), (-0.004, 0.0, 0.007)):
        x = [0.0] * total
        for m in notes:
            f = mtof(m)
            for d, ph in zip(dets, (0.0, 0.3, 0.6)):
                x = [a + b for a, b in zip(x, saw(repeat(f * (1.0 + d) / SR, total), ph))]
        x = sweep(x, lambda k: 650.0 + 3400.0 * math.exp(-k / (0.06 * SR)), 0.9)
        chans.append(norm([s * e * math.exp(-i / (0.55 * n)) for i, (s, e) in enumerate(zip(x, env))]))
    return chans

def pad(notes, n):
    """Slow-swelling dark pad whose filter opens across the note."""
    total = n + int(0.5 * SR)
    atk = int(0.45 * SR)
    env = [0.5 * (1.0 - math.cos(math.pi * min(1.0, i / atk))) for i in range(total)]
    env = [e * g for e, g in zip(env, gate(total, n, 1, int(0.5 * SR)))]
    chans = []
    for dets in ((-0.004, 0.003), (-0.003, 0.004)):
        x = [0.0] * total
        for m in notes:
            for d, ph in zip(dets, (0.0, 0.5)):
                x = [a + b for a, b in zip(x, saw(repeat(mtof(m) * (1.0 + d) / SR, total), ph))]
        x = sweep(x, lambda k: 220.0 + 1500.0 * (0.5 - 0.5 * math.cos(math.pi * min(1.0, k / (0.7 * n)))), 1.1)
        chans.append(norm([s * e for s, e in zip(x, env)]))
    return chans

def render_harmony():
    hl, hr, send = new(), new(), new()
    cache = {}
    for bar in range(BARS):
        sec, p = locate(bar)
        plan = stab_plan(sec, p)
        if plan:
            chord, pattern = plan
            for st, ln in pattern:
                key = (chord, ln)
                if key not in cache:
                    cache[key] = stab(CHORD[chord], ln * STEP - 150)
                mix(hl, cache[key][0], at(bar, st), 0.2)
                mix(hr, cache[key][1], at(bar, st), 0.2)
                mix(send, cache[key][0], at(bar, st), 0.1)
        if (sec == "break") or (sec == "final" and p >= 8):
            key = ("pad", PROG[sec][p % 4])
            if key not in cache:
                cache[key] = pad(CHORD[key[1]], int(1.2 * BAR))
            mix(hl, cache[key][0], at(bar, 0), 0.08)
            mix(hr, cache[key][1], at(bar, 0), 0.08)
            mix(send, cache[key][0], at(bar, 0), 0.06)
    return hl, hr, send

# ------------------------------------------------------------------ music box

# one 4-bar cycle over Dm | Bb | Gm | A: (step in cycle, length in 16ths, midi)
LINE = [(0, 6, 81), (6, 2, 82), (8, 4, 81), (12, 4, 77),
        (16, 6, 86), (22, 2, 84), (24, 4, 82), (28, 4, 81),
        (32, 6, 79), (38, 2, 81), (40, 4, 82), (44, 4, 86),
        (48, 6, 85), (54, 2, 86), (56, 4, 88), (60, 4, 81)]

def box(midi):
    """Music-box tine: bar-mode partials with a slow pitch wobble, a little out of tune."""
    f = mtof(midi + ((midi * 37) % 21 - 10 + 6) / 100.0)
    n = int(1.8 * SR)
    w = TAU * f / SR
    wl = TAU * (0.6 + 0.07 * (midi % 5)) / SR
    ph = list(accumulate(w * (1.0 + 0.0035 * math.sin(wl * i)) for i in range(n)))
    out = [0.0] * n
    for r, a, tau in ((1.0, 1.0, 0.7), (2.0, 0.1, 0.35), (6.27, 0.22, 0.16), (17.55, 0.05, 0.05)):
        if f * r > 14000.0:
            continue
        k = 1.0 / (tau * SR)
        out = [o + a * math.sin(r * p) * math.exp(-i * k) for i, (o, p) in enumerate(zip(out, ph))]
    out = [s * min(1.0, i / 30.0) for i, s in enumerate(out)]
    return fade_out(norm(out), int(0.3 * SR))

def glitch(x, kind):
    g = STEP // 2
    if kind == "stutter":
        return list(chain.from_iterable([s * 0.78 ** r for s in x[:g]] for r in range(6)))
    if kind == "crush":
        return [round(x[i - i % 7] * 32) / 32 for i in range(len(x))]
    if kind == "gate":
        return [s if (i // (STEP // 4)) % 2 == 0 else 0.0 for i, s in enumerate(x)]
    return x

def glitch_for(sec, p, idx):
    """Which notes break up: more as the track goes on, dissolving at the end."""
    cyc, b = p // 4, p % 4
    last = idx == max(i for i, (st, _, _) in enumerate(LINE) if st // 16 == b)
    if sec == "b":
        return {(1, 3): "crush", (2, 1): "stutter", (2, 3): "stutter", (3, 3): "rev"}.get((cyc, b)) if last else None
    if cyc == 0:
        return "stutter" if last else None
    if cyc == 1:
        return "crush" if idx % 4 == 0 else "gate" if last else None
    return "rev" if b < 2 else None

TAPS = ((3, 0.7, 0.5), (6, -0.7, 0.3), (9, 0.5, 0.18), (12, -0.5, 0.1))   # steps, pan, gain: ping-pong

def render_box():
    ll, lr, send = new(), new(), new()
    cache = {}
    for bar in range(24, BARS):
        sec, p = locate(bar)
        cyc, b = p // 4, p % 4
        if sec == "final" and cyc == 2 and b >= 2:
            continue
        for idx, (st, ln, midi) in enumerate(LINE):
            if st // 16 != b:
                continue
            kind = glitch_for(sec, p, idx)
            pos = at(bar, st % 16)
            if midi not in cache:
                cache[midi] = box(midi)
            if kind == "rev":
                r = cache[midi][:int(0.7 * SR)][::-1]
                mix(ll, r, pos - len(r), 0.18)
                mix(lr, r, pos - len(r), 0.18)
            key = (midi, None if kind == "rev" else kind)
            if key not in cache:
                x = glitch(cache[midi], key[1])
                cache[key] = (x, lowpass(x, 2600.0))
            x, dull = cache[key]
            gl, gr = pan_gains(-0.2 if midi % 2 else 0.2)
            mix(ll, x, pos, 0.2 * gl * 1.414)
            mix(lr, x, pos, 0.2 * gr * 1.414)
            mix(send, x, pos, 0.18)
            for steps, tp, g in TAPS:
                tl, tr = pan_gains(tp)
                mix(ll, dull, pos + steps * STEP, 0.2 * g * tl * 1.414)
                mix(lr, dull, pos + steps * STEP, 0.2 * g * tr * 1.414)
                mix(send, dull, pos + steps * STEP, 0.1 * g)
    return ll, lr, send

# ------------------------------------------------------------------ fx

def render_fx(kit, rnd):
    fl, fr = new(), new()
    def swell(x, end, gain):
        mix(fl, x, end - len(x), gain)
        mix(fr, x, end - len(x), gain)
    rev_crash = kit["crash"][0][:BAR][::-1]
    rev_long = kit["crash"][0][:int(1.5 * BAR)][::-1]
    rev_snare = kit["snare"][0][::-1]
    for bar in (4, 24, 40):
        swell(rev_crash, at(bar, 0), 0.34)
    swell(rev_long, at(BARS, 0), 0.4)
    for bar in (12, 32):
        swell(rev_snare, at(bar, 0), 0.3)
    swell(rev_snare, at(20, 0), 0.4)
    swell(riser(rnd, 4 * BAR, 200.0, 7000.0, 0.5), at(24, 0), 0.34)
    swell(riser(rnd, 2 * BAR, 300.0, 8000.0, 0.0), at(BARS, 0), 0.34)
    swell(riser(rnd, 1 * BAR, 300.0, 8000.0, 0.0), at(40, 0), 0.26)
    return fl, fr

# ------------------------------------------------------------------ reverb

def reverb(send, tail=int(3.2 * SR)):
    """Schroeder room at half rate (dark on purpose); the tail past the loop end is folded onto the start."""
    x = highpass(list(send), 180.0) + [0.0] * tail          # keep the low end out of the reverb
    half = [(a + b) * 0.5 for a, b in zip(x[0::2], x[1::2])]
    outs = []
    for spread in (0, 13):
        d0, d1, d2, d3 = (d + spread for d in (761, 809, 853, 907))
        b0, b1, b2, b3 = [0.0] * d0, [0.0] * d1, [0.0] * d2, [0.0] * d3
        i0 = i1 = i2 = i3 = 0
        f0 = f1 = f2 = f3 = 0.0
        a1, a2 = [0.0] * 225, [0.0] * 341
        j1 = j2 = 0
        fb, damp = 0.85, 0.35
        out = []
        ap = out.append
        for s in half:
            s *= 0.08
            y = b0[i0]; f0 = y * (1.0 - damp) + f0 * damp; b0[i0] = s + f0 * fb; acc = y
            i0 += 1
            if i0 == d0: i0 = 0
            y = b1[i1]; f1 = y * (1.0 - damp) + f1 * damp; b1[i1] = s + f1 * fb; acc += y
            i1 += 1
            if i1 == d1: i1 = 0
            y = b2[i2]; f2 = y * (1.0 - damp) + f2 * damp; b2[i2] = s + f2 * fb; acc += y
            i2 += 1
            if i2 == d2: i2 = 0
            y = b3[i3]; f3 = y * (1.0 - damp) + f3 * damp; b3[i3] = s + f3 * fb; acc += y
            i3 += 1
            if i3 == d3: i3 = 0
            y = a1[j1]; a1[j1] = acc + y * 0.5; acc = y - acc
            j1 += 1
            if j1 == 225: j1 = 0
            y = a2[j2]; a2[j2] = acc + y * 0.5; acc = y - acc
            j2 += 1
            if j2 == 341: j2 = 0
            ap(acc)
        up = [0.0] * (2 * len(out))
        up[0::2] = out
        up[1::2] = [(a + b) * 0.5 for a, b in zip(out, out[1:] + [0.0])]
        y = array.array("d", up)
        y[:tail] = array.array("d", map(add, y[:tail], y[N:N + tail]))
        outs.append(y[:N])
    return outs

# ------------------------------------------------------------------ mix and master

def duck_curve(kicks, depth, tau=0.05, length=0.2):
    """Sidechain gain: dips on every kick and recovers, wrapping round the loop."""
    n = int(length * SR)
    shape = [min(1.0, i / 88.0) * math.exp(-i / (tau * SR)) for i in range(n)]
    d = array.array("d", [1.0]) * N
    for pos, vel in kicks:
        k = depth * min(1.0, vel)
        mix_mul(d, [1.0 - k * s for s in shape], pos)
    return d

def mix_mul(buf, x, pos):
    pos %= N
    n = len(x)
    if pos + n <= N:
        buf[pos:pos + n] = array.array("d", map(mul, buf[pos:pos + n], x))
    else:
        k = N - pos
        buf[pos:] = array.array("d", map(mul, buf[pos:], x[:k]))
        buf[:n - k] = array.array("d", map(mul, buf[:n - k], x[k:]))

LEVELS = {"drums": 0.5, "bass": 0.38, "harm": 3.0, "box": 1.0, "fx": 1.0, "wet": 1.0}

def total(*parts):
    """Sum (buffer, gain) pairs sample by sample."""
    gains = [g for _, g in parts]
    return array.array("d", [sum(map(mul, row, gains)) for row in zip(*(b for b, _ in parts))])

def master(L, R, drive=1.0, ceiling=0.85):
    """Soft clip, then scale so the loudest peak sits at the ceiling."""
    out = []
    for ch in (L, R):
        m = sum(ch) / N
        out.append([math.tanh(drive * (s - m)) for s in ch])
    k = ceiling / max(max(map(abs, out[0])), max(map(abs, out[1])))
    return [s * k for s in out[0]], [s * k for s in out[1]]

def build():
    rnd = random.Random(12)
    kit = make_kit()
    print("drums")
    ev, kicks = drum_events(rnd)
    dc, dl, dr, dsend = render_drums(ev, kit, rnd)
    print("bass")
    bass = render_bass()
    print("harmony")
    hl, hr, hsend = render_harmony()
    print("music box")
    ll, lr, lsend = render_box()
    print("fx")
    fl, fr = render_fx(kit, random.Random(13))
    return dict(kicks=kicks, dc=dc, dl=dl, dr=dr, dsend=dsend, bass=bass, hl=hl, hr=hr, hsend=hsend,
                ll=ll, lr=lr, lsend=lsend, fl=fl, fr=fr)

def mixdown(st):
    lv = LEVELS
    bass = array.array("d", map(mul, st["bass"], duck_curve(st["kicks"], 0.8)))
    duck = duck_curve(st["kicks"], 0.35)
    hl = array.array("d", map(mul, st["hl"], duck))
    hr = array.array("d", map(mul, st["hr"], duck))
    print("reverb")
    rl, rr = reverb(total((st["dsend"], lv["drums"]), (st["hsend"], lv["harm"]), (st["lsend"], lv["box"])))
    print("master")
    L = total((st["dc"], lv["drums"]), (st["dl"], lv["drums"]), (bass, lv["bass"]), (hl, lv["harm"]),
              (st["ll"], lv["box"]), (st["fl"], lv["fx"]), (rl, lv["wet"]))
    R = total((st["dc"], lv["drums"]), (st["dr"], lv["drums"]), (bass, lv["bass"]), (hr, lv["harm"]),
              (st["lr"], lv["box"]), (st["fr"], lv["fx"]), (rr, lv["wet"]))
    return master(L, R)

def write_wav(path, L, R):
    pcm = array.array("h", [int(round(s * 32767.0)) for s in chain.from_iterable(zip(L, R))])
    if sys.byteorder == "big":
        pcm.byteswap()
    with wave.open(path, "wb") as w:
        w.setnchannels(2)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())

def main():
    if shutil.which("oggenc") is None:
        sys.exit("oggenc not found: install vorbis-tools (e.g. apt-get install vorbis-tools) and run again")
    L, R = mixdown(build())
    wav = os.path.join(tempfile.gettempdir(), "sorrowed_memory_shatterer.wav")
    write_wav(wav, L, R)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    subprocess.run(["oggenc", "-Q", "-q", "3", "-s", "172", "-t", "The Memory Shatterer", "-o", OUT, wav], check=True)
    os.remove(wav)
    print(f"music written: {OUT} ({N / SR:.1f} s, {os.path.getsize(OUT) / 1024:.0f} KB)")

if __name__ == "__main__":
    main()
