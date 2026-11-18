#!/usr/bin/env python3
"""Generates the placeholder textures (pure Python, no dependencies). Run from the project root."""
import math, random, struct, zlib, os

OUT = "src/main/resources/assets/sorrowed/textures"

def write_png(path, w, h, px):
    raw = b"".join(b"\x00" + bytes(c for p in px[y*w:(y+1)*w] for c in p) for y in range(h))
    def chunk(t, d):
        c = struct.pack(">I", len(d)) + t + d
        return c + struct.pack(">I", zlib.crc32(t + d) & 0xffffffff)
    data = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(data)

def skin(path, head, shirt, legs, alpha, eyes=None, tear=None, seed=1):
    rnd = random.Random(seed)
    w = h = 64
    px = [(0, 0, 0, 0)] * (w * h)
    def fill(x0, y0, x1, y1, col):
        for y in range(y0, y1):
            for x in range(x0, x1):
                n = rnd.randint(-7, 7)
                px[y*w+x] = (max(0, min(255, col[0]+n)), max(0, min(255, col[1]+n)), max(0, min(255, col[2]+n)), alpha)
    fill(0, 0, 32, 16, head)      # head
    fill(16, 16, 40, 32, shirt)   # body
    fill(40, 16, 56, 32, shirt)   # right arm
    fill(32, 48, 48, 64, shirt)   # left arm
    fill(0, 16, 16, 32, legs)     # right leg
    fill(16, 48, 32, 64, legs)    # left leg
    if eyes:
        for (ex, ey) in ((9, 11), (13, 11)):
            for dx in range(2):
                for dy in range(2):
                    px[(ey+dy)*w+ex+dx] = (*eyes, min(255, alpha + 60))
    if tear:
        px[13*w+9] = (*tear, 255)
        px[14*w+9] = (*tear, 255)
    write_png(path, w, h, px)

def door(path, base, streak, seed, frames=8):
    """An animated doorway pane: 8 frames stacked vertically. A dark depth with slow vertical streaks
    drifting up through it and a faint lit rim, so the thing looks like a hole with light behind it."""
    rnd = random.Random(seed)
    cols = [rnd.random() for _ in range(16)]
    speeds = [1 + rnd.randint(0, 2) for _ in range(16)]
    px = []
    for fr in range(frames):
        for y in range(16):
            for x in range(16):
                edge = min(x, y, 15 - x, 15 - y)
                depth = 1.0 - edge / 7.0 if edge < 7 else 0.0           # darker toward the middle
                yy = (y + fr * speeds[x]) % 16
                streaky = cols[x] > 0.68 and (yy * 3 + x) % 7 < 3
                if edge == 0:
                    col = tuple(min(255, int(c * 1.3) + 40) for c in streak)
                    a = 230
                elif streaky:
                    col = streak
                    a = 150 + int(50 * depth)
                else:
                    col = tuple(max(0, int(c * (0.25 + 0.45 * (1 - depth)))) for c in base)
                    a = 120 + int(110 * depth)
                px.append((*col, a))
    write_png(path, 16, 16 * frames, px)

def tear(path):
    px = [(0, 0, 0, 0)] * 256
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 10
            in_drop = dx*dx + dy*dy <= 20 or (y < 10 and y >= 3 and abs(dx) <= (y - 2) * 0.5)
            if in_drop:
                edge = dx*dx + dy*dy > 14 and y >= 10
                col = (96, 140, 190) if edge else (170, 212, 240)
                if x in (6, 7) and 9 <= y <= 11:
                    col = (235, 248, 255)
                px[y*16+x] = (*col, 255)
    write_png(path, 16, 16, px)

door(f"{OUT}/block/hush_door.png", (16, 24, 52), (84, 112, 186), 12)
door(f"{OUT}/block/drowned_door.png", (18, 84, 96), (120, 220, 220), 13)
door(f"{OUT}/block/hallway_door.png", (196, 184, 76), (255, 248, 160), 14)
door(f"{OUT}/block/backup_door.png", (250, 226, 236), (255, 255, 255), 15)
door(f"{OUT}/block/archive_door.png", (62, 40, 22), (190, 150, 80), 16)
door(f"{OUT}/block/street_door.png", (10, 12, 30), (255, 170, 60), 17)
door(f"{OUT}/block/static_door.png", (120, 120, 124), (240, 240, 240), 18)
door(f"{OUT}/block/orchard_door.png", (70, 60, 40), (190, 170, 110), 19)
door(f"{OUT}/block/ward_door.png", (160, 190, 180), (240, 255, 250), 20)
door(f"{OUT}/block/server_door.png", (8, 20, 14), (60, 255, 120), 21)

# Entity textures are generated with the geometry by tools/gen_models.py.

# ---------------------------------------------------------------- papers

def paper(path, w=200, h=250, sheet=256, seed=5):
    """A wrinkled cream sheet with soft creases, a torn edge and a faint stain, on a transparent sheet."""
    rnd = random.Random(seed)
    base = (236, 226, 198)
    px = [(0, 0, 0, 0)] * (sheet * sheet)
    # Creases: a few long folds, each a bright ridge beside a dark trough.
    creases = []
    for _ in range(9):
        x0, y0 = rnd.uniform(0, w), rnd.uniform(0, h)
        ang = rnd.uniform(0, math.pi)
        creases.append((x0, y0, math.cos(ang), math.sin(ang), rnd.uniform(0.6, 1.0)))
    for y in range(h):
        for x in range(w):
            # Torn, uneven border.
            edge = min(x, y, w - 1 - x, h - 1 - y)
            tear = 1.5 + 1.5 * math.sin(x * 0.7 + y * 0.3) + rnd.random() * 1.2
            if edge < tear:
                continue
            shade = 1.0 - 0.04 * ((x / w) ** 2 + (y / h) ** 2)       # gentle vignette
            for (cx, cy, dx, dy, strength) in creases:
                d = (x - cx) * dy - (y - cy) * dx                       # signed distance to the fold line
                if -3 < d < 3:
                    shade += (0.09 if d > 0 else -0.11) * strength * (1 - abs(d) / 3)
            n = rnd.randint(-4, 4)
            col = tuple(max(0, min(255, int(c * shade) + n)) for c in base)
            # A ring of something spilled, long ago.
            r = math.hypot(x - 150, y - 200)
            if 22 < r < 25:
                col = tuple(max(0, c - 18) for c in col)
            px[y * sheet + x] = (*col, 255)
    write_png(path, sheet, sheet, px)

def pencil(px, size, x0, y0, x1, y1, rnd, col=(52, 50, 60), width=1, alpha=220):
    """A hand-drawn line: a straight segment with a little tremor along it."""
    steps = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
    for i in range(steps + 1):
        t = i / max(1, steps)
        x = x0 + (x1 - x0) * t + rnd.uniform(-0.6, 0.6)
        y = y0 + (y1 - y0) * t + rnd.uniform(-0.6, 0.6)
        for dx in range(width):
            for dy in range(width):
                X, Y = int(x) + dx, int(y) + dy
                if 0 <= X < size and 0 <= Y < size:
                    a = alpha - rnd.randint(0, 60)
                    px[Y * size + X] = (*col, max(0, a))

def poly(px, size, pts, rnd, **kw):
    for (a, b) in zip(pts, pts[1:]):
        pencil(px, size, a[0], a[1], b[0], b[1], rnd, **kw)

def circle(px, size, cx, cy, r, rnd, **kw):
    pts = [(cx + r * math.cos(t * math.pi / 8), cy + r * math.sin(t * math.pi / 8)) for t in range(17)]
    poly(px, size, pts, rnd, **kw)

def stick(px, size, x, y, rnd, h=40, **kw):
    circle(px, size, x, y - h, 6, rnd, **kw)
    pencil(px, size, x, y - h + 6, x, y - h * 0.35, rnd, **kw)                # body
    pencil(px, size, x, y - h * 0.35, x - 9, y, rnd, **kw)                  # legs
    pencil(px, size, x, y - h * 0.35, x + 9, y, rnd, **kw)
    pencil(px, size, x, y - h * 0.75, x - 12, y - h * 0.5, rnd, **kw)       # arms
    pencil(px, size, x, y - h * 0.75, x + 12, y - h * 0.5, rnd, **kw)

def drawing(path, draw, size=160, seed=1):
    rnd = random.Random(seed)
    px = [(0, 0, 0, 0)] * (size * size)
    draw(px, size, rnd)
    write_png(path, size, size, px)

def draw_first_night(px, size, rnd):
    # A crooked dirt house, two figures holding hands, rain.
    poly(px, size, [(20, 120), (20, 70), (60, 45), (100, 70), (100, 120), (20, 120)], rnd, width=2)
    poly(px, size, [(50, 120), (50, 95), (65, 95), (65, 120)], rnd)
    poly(px, size, [(75, 85), (88, 85), (88, 97), (75, 97), (75, 85)], rnd)
    stick(px, size, 120, 125, rnd)
    stick(px, size, 142, 125, rnd, h=34)
    pencil(px, size, 132, 95, 130, 100, rnd)   # joined hands
    for _ in range(60):
        x, y = rnd.uniform(5, 155), rnd.uniform(5, 60)
        pencil(px, size, x, y, x - 2, y + 7, rnd, col=(70, 90, 130), alpha=150)
    poly(px, size, [(5, 128), (155, 126)], rnd)

def draw_scribble(px, size, rnd):
    # Nothing you can read: a page of loops and crossings that get denser toward the middle.
    for _ in range(45):
        x, y = rnd.uniform(10, 150), rnd.uniform(10, 150)
        pts = [(x, y)]
        for _ in range(rnd.randint(4, 12)):
            x += rnd.uniform(-28, 28) * (0.5 + abs(x - 80) / 160)
            y += rnd.uniform(-22, 22)
            pts.append((max(4, min(156, x)), max(4, min(156, y))))
        poly(px, size, pts, rnd, width=rnd.choice((1, 1, 2)), alpha=rnd.randint(120, 230))
    for yy in range(30, 140, 11):   # ruled lines, ignored by the hand that wrote this
        pencil(px, size, 12, yy, 148, yy, rnd, col=(120, 130, 160), alpha=70)

def draw_gate(px, size, rnd):
    # Planks running off to a gate, drawn in one-point perspective.
    poly(px, size, [(10, 150), (74, 60)], rnd, width=2)
    poly(px, size, [(150, 150), (86, 60)], rnd, width=2)
    for i in range(12):
        t = i / 12
        y = 150 - 90 * (1 - (1 - t) ** 2)
        hw = 70 * (1 - t) + 6
        pencil(px, size, 80 - hw, y, 80 + hw, y, rnd, alpha=170)
    poly(px, size, [(70, 62), (70, 36), (90, 36), (90, 62)], rnd, width=2)
    pencil(px, size, 66, 36, 94, 36, rnd, width=2)
    pencil(px, size, 80, 30, 80, 36, rnd)
    circle(px, size, 80, 27, 2, rnd)
    for _ in range(8):   # the nothing on either side, drawn as scribbled dark
        x, y = rnd.uniform(5, 45), rnd.uniform(40, 120)
        poly(px, size, [(x, y), (x + 10, y + 3), (x + 2, y + 8)], rnd, alpha=90)
        x, y = rnd.uniform(115, 155), rnd.uniform(40, 120)
        poly(px, size, [(x, y), (x + 10, y + 3), (x + 2, y + 8)], rnd, alpha=90)

def draw_street(px, size, rnd):
    # A row of identical houses, a streetlight, one window shaded yellow.
    for i, x in enumerate((8, 58, 108)):
        poly(px, size, [(x, 110), (x, 70), (x + 20, 50), (x + 40, 70), (x + 40, 110), (x, 110)], rnd)
        poly(px, size, [(x + 16, 110), (x + 16, 92), (x + 24, 92), (x + 24, 110)], rnd)
        wx, wy = x + 28, 78
        poly(px, size, [(wx, wy), (wx + 8, wy), (wx + 8, wy + 8), (wx, wy + 8), (wx, wy)], rnd)
        if i == 1:
            for yy in range(wy + 1, wy + 8):
                pencil(px, size, wx + 1, yy, wx + 7, yy, rnd, col=(230, 190, 60), alpha=200)
    pencil(px, size, 80, 135, 80, 95, rnd, width=2)
    circle(px, size, 80, 92, 4, rnd, col=(230, 190, 60))
    poly(px, size, [(2, 135), (158, 135)], rnd, width=2)
    for _ in range(50):
        x, y = rnd.uniform(5, 155), rnd.uniform(5, 50)
        pencil(px, size, x, y, x - 1, y + 6, rnd, col=(70, 90, 130), alpha=120)

def papers_icon(path):
    px = [(0, 0, 0, 0)] * 256
    rnd = random.Random(9)
    for (ox, oy, shade) in ((3, 4, 0.82), (2, 2, 0.92), (1, 0, 1.0)):
        for y in range(2 + oy, 14 + oy):
            for x in range(2 + ox, 11 + ox):
                if x < 16 and y < 16:
                    c = tuple(int(v * shade) for v in (236, 226, 198))
                    px[y * 16 + x] = (*c, 255)
    for yy in (4, 6, 8, 10):
        for x in range(4, 10):
            if (x + yy) % 3:
                px[yy * 16 + x] = (90, 86, 100, 255)
    write_png(path, 16, 16, px)

paper(f"{OUT}/gui/paper.png")

# ---------------------------------------------------------------- the title screen

def title_bg(path, W=480, H=270):
    """His house across a field at night, in the rain. One window is lit."""
    rnd = random.Random(42)
    px = [(0, 0, 0, 255)] * (W * H)
    def put(x, y, c, a=255):
        if 0 <= x < W and 0 <= y < H:
            px[y * W + x] = (*c, a)
    horizon = 176
    for y in range(H):
        for x in range(W):
            if y < horizon:
                f = y / horizon
                c = (int(6 + 22 * f), int(8 + 24 * f), int(14 + 30 * f))
                # a faint glow behind the clouds, where the moon would be
                g = max(0.0, 1.0 - math.hypot(x - 120, y - 46) / 90.0)
                c = tuple(min(255, int(v + 26 * g * g)) for v in c)
                # clouds: slow banding
                band = math.sin(x * 0.013 + y * 0.09) * math.sin(x * 0.004 - y * 0.05)
                c = tuple(max(0, min(255, int(v + 6 * band))) for v in c)
            else:
                n = rnd.randint(-3, 3)
                c = (max(0, 13 + n), max(0, 19 + n), max(0, 15 + n))
            put(x, y, c)
    # Hills on the horizon.
    for x in range(W):
        top = int(horizon - 10 - 9 * math.sin(x * 0.011) - 5 * math.sin(x * 0.037 + 1.3))
        for y in range(top, horizon + 2):
            put(x, y, (11, 13, 18))
    # Mist along the horizon.
    for y in range(horizon - 8, horizon + 14):
        for x in range(W):
            r, g, b, _ = px[y * W + x]
            m = 1.0 - abs(y - horizon - 3) / 11.0
            put(x, y, (min(255, int(r + 14 * m)), min(255, int(g + 16 * m)), min(255, int(b + 20 * m))))
    # The field: grass strokes.
    for _ in range(2600):
        x = rnd.randint(0, W - 1)
        y = rnd.randint(horizon + 4, H - 1)
        h = 1 + int((y - horizon) / 28)
        for k in range(h):
            put(x, y - k, (20, 30, 22))
    # The path from the house toward you.
    for y in range(192, H):
        f = (y - 192) / (H - 192)
        cx = int(309 - f * 120)
        half = int(2 + f * 16)
        for x in range(cx - half, cx + half):
            put(x, y, (26, 26, 26))
    # The house: walls, gable roof, chimney, door, two windows.
    for y in range(150, 191):
        for x in range(280, 341):
            put(x, y, (21, 19, 23))
    for y in range(118, 151):
        half = int((y - 118) * 36 / 32)
        for x in range(310 - half, 311 + half):
            put(x, y, (16, 14, 18))
    for y in range(124, 141):
        for x in range(326, 333):
            put(x, y, (18, 16, 20))
    for y in range(172, 191):
        for x in range(305, 314):
            put(x, y, (10, 9, 12))
    for y in range(158, 165):
        for x in range(288, 295):
            put(x, y, (26, 30, 42))
        for x in range(311, 318):
            put(x, y, (205, 150, 70))
    # Window light falling on the grass.
    for y in range(191, 205):
        for x in range(300, 330):
            r, g, b, _ = px[y * W + x]
            f = max(0.0, 1.0 - math.hypot(x - 314, (y - 191) * 1.6) / 18.0)
            put(x, y, (min(255, int(r + 50 * f)), min(255, int(g + 36 * f)), min(255, int(b + 14 * f))))
    # A dead tree to the right of the house.
    for y in range(120, 192):
        put(392, y, (12, 11, 13)); put(393, y, (12, 11, 13))
    for (x0, y0, dx, dy, n) in ((392, 140, -1, -1, 18), (393, 132, 1, -1, 16), (392, 155, -1, -1, 12), (393, 150, 1, -1, 22), (392, 126, -1, -2, 10)):
        x, y = x0, y0
        for i in range(n):
            put(x, y, (12, 11, 13))
            x += dx
            if i % abs(dy) == 0:
                y -= 1
    # The fence along the field.
    for x in range(220, 470, 11):
        for y in range(186, 197):
            put(x, y, (14, 13, 15))
    for x in range(220, 470):
        put(x, 189, (14, 13, 15))
    write_png(path, W, H, px)

def title_figure(path):
    """Someone standing in the field. Arms at their sides."""
    W, H = 16, 32
    px = [(0, 0, 0, 0)] * (W * H)
    c = (6, 6, 9, 235)
    for y in range(1, 8):
        for x in range(5, 11):
            if (x - 7.5) ** 2 + (y - 4) ** 2 <= 9:
                px[y * W + x] = c
    for y in range(8, 21):
        for x in range(5, 11):
            px[y * W + x] = c
        if y < 19:
            px[y * W + 4] = c
            px[y * W + 11] = c
    for y in range(21, 32):
        for x in (5, 6, 9, 10):
            px[y * W + x] = c
    write_png(path, W, H, px)

def loose_papers(path):
    """A few sheets lying on the floor, overlapping, one with writing on it."""
    rnd = random.Random(31)
    px = [(0, 0, 0, 0)] * 256
    sheets = ((1, 2, 9, 11, 0.86), (6, 5, 14, 14, 0.94), (3, 7, 11, 15, 1.0))
    for (x0, y0, x1, y1, shade) in sheets:
        for y in range(y0, y1):
            for x in range(x0, x1):
                n = rnd.randint(-6, 6)
                px[y * 16 + x] = (int(236 * shade) + n, int(226 * shade) + n, int(198 * shade) + n, 255)
    for yy in (9, 11, 13):
        for x in range(4, 10):
            if (x + yy) % 3:
                px[yy * 16 + x] = (80, 76, 90, 255)
    write_png(path, 16, 16, px)

title_bg(f"{OUT}/gui/title_bg.png")
title_figure(f"{OUT}/gui/title_figure.png")
loose_papers(f"{OUT}/block/loose_papers.png")
drawing(f"{OUT}/gui/drawing_first_night.png", draw_first_night, seed=11)
drawing(f"{OUT}/gui/drawing_scribble.png", draw_scribble, seed=12)
drawing(f"{OUT}/gui/drawing_gate.png", draw_gate, seed=13)
drawing(f"{OUT}/gui/drawing_street.png", draw_street, seed=14)
papers_icon(f"{OUT}/item/papers.png")

tear(f"{OUT}/item/rain_tear.png")

# ---------------------------------------------------------------- the story: screens, the journal, the end

def mcmeta(path, frametime):
    with open(path + ".mcmeta", "w") as f:
        f.write('{\n  "animation": {\n    "frametime": %d\n  }\n}\n' % frametime)

def vignette(path, size=256):
    """White, transparent in the middle and closing in at the edges. The game tints it per world."""
    px = []
    for y in range(size):
        for x in range(size):
            dx = (x + 0.5) / size * 2 - 1
            dy = (y + 0.5) / size * 2 - 1
            d = math.sqrt(dx * dx * 0.92 + dy * dy * 1.08)
            t = max(0.0, min(1.0, (d - 0.42) / 0.78))
            a = int(255 * (t * t * (3 - 2 * t)) ** 1.25)
            px.append((255, 255, 255, a))
    write_png(path, size, size, px)

def grain(path, size=64):
    rnd = random.Random(64)
    px = []
    for _ in range(size * size):
        v = max(0, min(255, int(rnd.gauss(128, 46))))
        px.append((v, v, v, 255))
    write_png(path, size, size, px)

def arrow(path):
    """A pencil arrow pointing up, with a dark edge so it reads on anything."""
    px = [(0, 0, 0, 0)] * 256
    strokes = [(8, y) for y in range(3, 14)]
    strokes += [(8 - i, 3 + i) for i in range(1, 5)] + [(8 + i, 3 + i) for i in range(1, 5)]
    strokes += [(7, 4), (9, 4)]
    for (x, y) in strokes:
        for ox in (-1, 0, 1):
            for oy in (-1, 0, 1):
                xx, yy = x + ox, y + oy
                if 0 <= xx < 16 and 0 <= yy < 16 and px[yy * 16 + xx][3] == 0:
                    px[yy * 16 + xx] = (12, 10, 14, 150)
    for (x, y) in strokes:
        px[y * 16 + x] = (236, 226, 198, 255)
    write_png(path, 16, 16, px)

def screen(path, frames, draw, frametime=None, seed=1):
    """16x16 frames stacked vertically; the visible glass is x 2..13, y 4..11 (12 by 8)."""
    rnd = random.Random(seed)
    px = [(0, 0, 0, 255)] * (16 * 16 * frames)
    for f in range(frames):
        def put(x, y, c, f=f):
            if 0 <= x < 16 and 0 <= y < 16:
                px[(f * 16 + y) * 16 + x] = (*c, 255)
        draw(put, f, rnd)
    write_png(path, 16, 16 * frames, px)
    if frametime:
        mcmeta(path, frametime)

def draw_chat(put, f, rnd):
    for y in range(16):
        for x in range(16):
            put(x, y, (14, 20, 34))
    for x in range(2, 14):
        put(x, 4, (52, 74, 120))                       # the title bar
    for (y, n) in ((6, 9), (7, 6), (8, 10), (9, 4)):    # his last few lines
        for x in range(3, 3 + n):
            if (x * 7 + y) % 5:
                put(x, y, (176, 188, 206))
    for x in range(2, 14):
        put(x, 10, (30, 40, 60))
    put(3, 11, (176, 188, 206))
    if f == 0:
        put(5, 11, (230, 236, 246))                     # the cursor, blinking

def draw_console(put, f, rnd):
    for y in range(16):
        for x in range(16):
            put(x, y, (4, 10, 6))
    for (y, n) in ((5, 8), (6, 11), (7, 5), (8, 9)):
        for x in range(3, 3 + n):
            if (x * 5 + y * 3) % 4:
                put(x, y, (90, 230, 120))
    put(3, 10, (90, 230, 120))
    if f == 0:
        put(5, 10, (180, 255, 190))

def draw_snow(put, f, rnd):
    for y in range(16):
        for x in range(16):
            v = rnd.randint(30, 230)
            put(x, y, (v, v, v))
    roll = (f * 2) % 8
    for x in range(2, 14):
        put(x, 4 + roll, (240, 240, 240))

def draw_clear(put, f, rnd):
    """The picture that comes through: his world, the sun out."""
    for y in range(16):
        for x in range(16):
            put(x, y, (8, 8, 10))
    for y in range(4, 9):
        for x in range(2, 14):
            k = (y - 4) / 5
            put(x, y, (int(110 + 90 * k), int(170 + 50 * k), int(240 - 20 * k)))
    for y in range(9, 12):
        for x in range(2, 14):
            put(x, y, (70 + (x % 2) * 8, 150, 60))
    put(11, 5, (255, 236, 150)); put(12, 5, (255, 236, 150)); put(11, 6, (255, 236, 150)); put(12, 6, (255, 236, 150))
    for x in range(4, 8):
        put(x, 9, (150, 104, 62)); put(x, 10, (150, 104, 62))
    for x in range(5, 7):
        put(x, 8, (110, 50, 40))
    put(5, 10, (40, 30, 26))

def draw_laptop(put, f, rnd):
    for y in range(16):
        for x in range(16):
            put(x, y, (8, 8, 14))
    for x in range(3, 13):
        put(x, 6, (120, 200, 120))                     # the bar, full
    for x in range(3, 10):
        if x % 3:
            put(x, 8, (200, 200, 200))
    put(3, 10, (200, 200, 200))
    if f == 0:
        put(5, 10, (240, 240, 240))

def title_bg_after(path, W=480, H=270):
    """The same view, the morning after: the rain gone, the sky coming up gold, the window dark."""
    rnd = random.Random(43)
    px = [(0, 0, 0, 255)] * (W * H)
    def put(x, y, c, a=255):
        if 0 <= x < W and 0 <= y < H:
            px[y * W + x] = (*c, a)
    horizon = 176
    sun = (366, 170)
    for y in range(H):
        for x in range(W):
            if y < horizon:
                f = y / horizon
                c = (int(58 + 190 * f ** 1.6), int(84 + 112 * f ** 1.4), int(140 + 20 * f))
                g = max(0.0, 1.0 - math.hypot(x - sun[0], (y - sun[1]) * 1.4) / 170.0)
                c = (min(255, int(c[0] + 70 * g * g)), min(255, int(c[1] + 48 * g * g)), min(255, int(c[2] + 10 * g * g)))
                band = math.sin(x * 0.013 + y * 0.09) * math.sin(x * 0.004 - y * 0.05)
                c = (max(0, min(255, int(c[0] + 10 * band))), max(0, min(255, int(c[1] + 6 * band))), max(0, min(255, int(c[2] + 4 * band))))
            else:
                n = rnd.randint(-3, 3)
                c = (max(0, 34 + n), max(0, 50 + n), max(0, 32 + n))
            put(x, y, c)
    for y in range(sun[1] - 9, sun[1] + 10):
        for x in range(sun[0] - 9, sun[0] + 10):
            if math.hypot(x - sun[0], y - sun[1]) <= 9:
                put(x, y, (255, 238, 204))
    for x in range(W):
        top = int(horizon - 10 - 9 * math.sin(x * 0.011) - 5 * math.sin(x * 0.037 + 1.3))
        for y in range(top, horizon + 2):
            put(x, y, (74, 62, 84))
    for y in range(horizon - 8, horizon + 14):
        for x in range(W):
            r, g, b, _ = px[y * W + x]
            m = 1.0 - abs(y - horizon - 3) / 11.0
            put(x, y, (min(255, int(r + 60 * m)), min(255, int(g + 42 * m)), min(255, int(b + 26 * m))))
    for _ in range(2600):
        x = rnd.randint(0, W - 1)
        y = rnd.randint(horizon + 4, H - 1)
        h = 1 + int((y - horizon) / 28)
        for k in range(h):
            put(x, y - k, (62, 92, 50))
    for y in range(192, H):
        f = (y - 192) / (H - 192)
        cx = int(309 - f * 120)
        half = int(2 + f * 16)
        for x in range(cx - half, cx + half):
            put(x, y, (96, 86, 74))
    for y in range(150, 191):
        for x in range(280, 341):
            put(x, y, (60, 50, 58))
    for y in range(118, 151):
        half = int((y - 118) * 36 / 32)
        for x in range(310 - half, 311 + half):
            put(x, y, (46, 38, 46))
    for y in range(124, 141):
        for x in range(326, 333):
            put(x, y, (52, 42, 50))
    for y in range(172, 191):
        for x in range(305, 314):
            put(x, y, (38, 32, 38))
    for y in range(158, 165):
        for x in list(range(288, 295)) + list(range(311, 318)):
            put(x, y, (64, 72, 92))
    for y in range(120, 192):
        put(392, y, (52, 44, 48)); put(393, y, (52, 44, 48))
    for (x0, y0, dx, dy, n) in ((392, 140, -1, -1, 18), (393, 132, 1, -1, 16), (392, 155, -1, -1, 12), (393, 150, 1, -1, 22), (392, 126, -1, -2, 10)):
        x, y = x0, y0
        for i in range(n):
            put(x, y, (52, 44, 48))
            if i > n - 4:
                put(x, y - 1, (96, 140, 70)); put(x + 1, y, (96, 140, 70))   # a few new leaves
            x += dx
            if i % abs(dy) == 0:
                y -= 1
    for x in range(220, 470, 11):
        for y in range(186, 197):
            put(x, y, (58, 50, 54))
    for x in range(220, 470):
        put(x, 189, (58, 50, 54))
    write_png(path, W, H, px)

vignette(f"{OUT}/gui/vignette.png")
grain(f"{OUT}/gui/grain.png")
arrow(f"{OUT}/gui/arrow.png")
title_bg_after(f"{OUT}/gui/title_bg_after.png")
screen(f"{OUT}/block/screen_chat.png", 2, draw_chat, frametime=10, seed=81)
screen(f"{OUT}/block/screen_console.png", 2, draw_console, frametime=10, seed=82)
screen(f"{OUT}/block/screen_snow.png", 8, draw_snow, frametime=2, seed=83)
screen(f"{OUT}/block/screen_clear.png", 1, draw_clear, seed=84)
screen(f"{OUT}/block/screen_laptop.png", 2, draw_laptop, frametime=12, seed=85)

# ---------------------------------------------------------------- the fight: the sword, the shards, the cracks, the halo

def borrowed_sword(path):
    """A pale sword with a gold guard, drawn the way vanilla swords are: tip top right, pommel bottom left."""
    px = [(0, 0, 0, 0)] * 256
    def put(x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            px[y * 16 + x] = (*c, 255)
    body = []
    for i in range(8):                                   # the blade
        body.append((13 - i, 2 + i, (250, 246, 228)))
        body.append((14 - i, 2 + i, (214, 222, 236)))
    body.append((14, 1, (255, 255, 255)))
    for k in range(-2, 3):                               # the guard
        body.append((5 + k, 10 + k, (226, 184, 72)))
    for (x, y) in ((4, 11), (3, 12), (2, 13)):           # the grip
        body.append((x, y, (92, 64, 48)))
    body.append((1, 14, (240, 200, 90)))                  # the pommel
    filled = {(x, y) for (x, y, _) in body}
    for (x, y) in list(filled):                          # a dark edge, so it reads on anything
        for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + ox, y + oy) not in filled:
                put(x + ox, y + oy, (40, 34, 52))
    for (x, y, c) in body:
        put(x, y, c)
    write_png(path, 16, 16, px)

def memory_shard(path):
    """A jagged piece of glass with a picture still caught in it."""
    px = [(0, 0, 0, 0)] * 256
    poly_pts = [(8, 1), (12, 6), (11, 14), (5, 12), (4, 5)]
    def inside(x, y):
        n = len(poly_pts)
        c = False
        for i in range(n):
            x0, y0 = poly_pts[i]
            x1, y1 = poly_pts[(i + 1) % n]
            if (y0 > y) != (y1 > y) and x < (x1 - x0) * (y - y0) / (y1 - y0) + x0:
                c = not c
        return c
    cells = [(x, y) for y in range(16) for x in range(16) if inside(x + 0.5, y + 0.5)]
    cs = set(cells)
    for (x, y) in cells:
        k = (y - 1) / 13
        px[y * 16 + x] = (int(200 - 60 * k), int(236 - 40 * k), int(255 - 20 * k), 235)
        if (x + 1, y) not in cs or (x, y + 1) not in cs or (x - 1, y) not in cs or (x, y - 1) not in cs:
            px[y * 16 + x] = (70, 120, 150, 255)
    for i in range(5):                                   # a highlight
        x, y = 7 + i // 2, 3 + i
        if (x, y) in cs:
            px[y * 16 + x] = (255, 255, 255, 255)
    write_png(path, 16, 16, px)

def shatterer_cracks(path):
    """The light through the cracks, mapped onto a player skin's base layer, and two red eyes. Black is no light."""
    rnd = random.Random(91)
    W = 64
    px = [(0, 0, 0, 255)] * (W * W)
    def put(x, y, c):
        if 0 <= x < W and 0 <= y < W:
            px[y * W + x] = (*c, 255)
    regions = [(0, 8, 32, 16), (16, 20, 40, 32), (40, 20, 54, 32), (32, 52, 46, 64), (0, 20, 16, 32), (16, 52, 32, 64)]
    for (x0, y0, x1, y1) in regions:
        for _ in range(max(2, (x1 - x0) * (y1 - y0) // 60)):
            x = rnd.randint(x0, x1 - 1)
            y = rnd.randint(y0, y1 - 1)
            for _ in range(rnd.randint(4, 9)):
                put(x, y, (255, 214, 196))
                if rnd.random() < 0.4:
                    put(x + 1, y, (150, 90, 90))
                x = max(x0, min(x1 - 1, x + rnd.choice((-1, 0, 1))))
                y = max(y0, min(y1 - 1, y + rnd.choice((0, 1, 1))))
    for (x, y) in ((9, 12), (10, 12), (13, 12), (14, 12)):
        put(x, y, (255, 40, 40))
    write_png(path, W, W, px)

def halo(path):
    """A soft band of gold, brightest across the middle; v runs from the inside of the ring to the outside."""
    px = []
    for y in range(16):
        k = max(0.0, 1.0 - abs(y - 7.5) / 8.0) ** 1.4
        for x in range(16):
            px.append((255, 240, 196, int(255 * k)))
    write_png(path, 16, 16, px)

borrowed_sword(f"{OUT}/item/borrowed_sword.png")
memory_shard(f"{OUT}/item/memory_shard.png")
shatterer_cracks(f"{OUT}/entity/shatterer_cracks.png")
halo(f"{OUT}/entity/halo.png")
print("textures written")
