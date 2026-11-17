#!/usr/bin/env python3
"""Generates the placeholder textures (pure Python, no dependencies). Run from the project root."""
import random, struct, zlib, os

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

def door(path, base, streak, seed):
    rnd = random.Random(seed)
    cols = [rnd.random() for _ in range(16)]
    px = []
    for y in range(16):
        for x in range(16):
            s = cols[x] > 0.72
            col = streak if s else base
            a = 150 if s else 100 + (x * 3 + y) % 20
            px.append((*col, a))
    write_png(path, 16, 16, px)

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

door(f"{OUT}/block/unfinished_door.png", (236, 226, 170), (255, 250, 214), 11)
door(f"{OUT}/block/hush_door.png", (16, 24, 52), (84, 112, 186), 12)
door(f"{OUT}/block/drowned_door.png", (18, 84, 96), (120, 220, 220), 13)
door(f"{OUT}/block/hallway_door.png", (196, 184, 76), (255, 248, 160), 14)
door(f"{OUT}/block/backup_door.png", (250, 226, 236), (255, 255, 255), 15)
door(f"{OUT}/block/archive_door.png", (62, 40, 22), (190, 150, 80), 16)
door(f"{OUT}/block/street_door.png", (10, 12, 30), (255, 170, 60), 17)
door(f"{OUT}/block/static_door.png", (120, 120, 124), (240, 240, 240), 18)

def zones(path, alpha, rects, dots=()):
    """Paint rectangles (x0,y0,w,h,(r,g,b)) onto a transparent 64x64 sheet, with light noise, then single pixels."""
    rnd = random.Random(path)
    w = h = 64
    px = [(0, 0, 0, 0)] * (w * h)
    for (x0, y0, rw, rh, col) in rects:
        for y in range(y0, min(h, y0 + rh)):
            for x in range(x0, min(w, x0 + rw)):
                n = rnd.randint(-6, 6)
                px[y*w+x] = (max(0, min(255, col[0]+n)), max(0, min(255, col[1]+n)), max(0, min(255, col[2]+n)), alpha)
    for (x, y, col, a) in dots:
        px[y*w+x] = (*col, a)
    write_png(path, w, h, px)

# These zones mirror the texOffs/box sizes in client/ModModels.java. Keep them in step.
# Vigil: pale and see-through, head bowed, one tear.
zones(f"{OUT}/entity/echo.png", 170, [
    (0, 0, 24, 13, (205, 218, 230)),      # head
    (0, 16, 20, 19, (150, 172, 192)),     # body
    (24, 16, 12, 21, (150, 172, 192)),    # arms
    (40, 16, 12, 18, (112, 128, 152)),    # legs
], dots=[(7, 8, (40, 48, 66), 230), (7, 9, (40, 48, 66), 230), (10, 8, (40, 48, 66), 230), (10, 9, (40, 48, 66), 230),
         (7, 11, (150, 200, 240), 255), (7, 12, (150, 200, 240), 255)])

# The Unsaid: dark and faceless. Nothing where the face should be.
zones(f"{OUT}/entity/hollow.png", 255, [
    (0, 0, 28, 16, (26, 26, 33)),
    (0, 18, 28, 21, (18, 18, 24)),
    (36, 0, 12, 27, (14, 14, 20)),
    (0, 40, 16, 21, (12, 12, 16)),
])

# Hark: grey, eyes sewn shut, mouth a straight line, ears like pale leather.
face = []
for x in (11, 12, 13, 16, 17, 18):
    face.append((x, 14, (24, 24, 28), 255))
for x in (11, 13, 16, 18):
    face.append((x, 13, (24, 24, 28), 255))
    face.append((x, 15, (24, 24, 28), 255))
for x in range(12, 18):
    face.append((x, 18, (24, 24, 28), 255))
zones(f"{OUT}/entity/listener.png", 255, [
    (0, 0, 40, 20, (170, 170, 176)),
    (44, 0, 12, 13, (150, 140, 146)),
    (0, 22, 20, 20, (146, 146, 154)),
    (24, 22, 12, 25, (130, 130, 138)),
    (40, 22, 12, 20, (110, 110, 120)),
], dots=face)

# The Finisher: faceless, yellow hard hat, brown vest, a wooden hammer.
zones(f"{OUT}/entity/mason.png", 255, [
    (0, 0, 32, 16, (200, 160, 130)),
    (0, 16, 40, 12, (240, 200, 40)),
    (0, 28, 32, 11, (240, 200, 40)),
    (0, 40, 40, 20, (112, 78, 46)),
    (40, 0, 16, 17, (150, 120, 90)),
    (40, 18, 16, 16, (70, 70, 84)),
    (40, 40, 20, 10, (110, 80, 50)),
    (40, 50, 16, 8, (130, 130, 140)),
])

# The Archivist: a pale, bowed head on a long neck, a robe the colour of old ink, a book with cream pages.
zones(f"{OUT}/entity/archivist.png", 255, [
    (0, 0, 24, 12, (214, 206, 186)),      # head
    (24, 0, 8, 8, (200, 192, 172)),       # neck
    (0, 14, 20, 18, (30, 42, 34)),        # body
    (24, 14, 8, 20, (30, 42, 34)),        # arms
    (36, 14, 8, 20, (22, 30, 26)),        # legs
    (44, 0, 10, 6, (120, 80, 40)),        # book cover
], dots=[(x, y, (236, 230, 210), 255) for x in range(45, 49) for y in range(1, 5)]
        + [(8, 8, (60, 60, 70), 255), (9, 8, (60, 60, 70), 255)])   # eyes shut, reading

# The Neighbour: a face in shadow under a wide hat, a long tan coat, dark trousers.
rnd_face = []
for x in range(8, 16):
    for y in range(8, 16):
        rnd_face.append((x, y, (54, 46, 44), 255))
zones(f"{OUT}/entity/neighbour.png", 255, [
    (0, 0, 32, 16, (120, 100, 90)),       # head
    (0, 16, 40, 11, (40, 40, 46)),        # hat brim
    (0, 28, 24, 9, (40, 40, 46)),         # hat crown
    (0, 38, 24, 16, (116, 96, 64)),       # body (coat)
    (24, 38, 32, 12, (108, 88, 58)),      # coat skirt
    (40, 0, 16, 16, (116, 96, 64)),       # arms
    (40, 16, 16, 18, (38, 36, 40)),       # legs
], dots=rnd_face)

# The Carrier: a wood-veneer television for a head, the screen a field of static, a grey suit below.
rnd = random.Random("carrier")
screen = []
for x in range(10, 20):
    for y in range(10, 18):
        v = rnd.choice((20, 90, 170, 235))
        screen.append((x, y, (v, v, v), 255))
zones(f"{OUT}/entity/carrier.png", 255, [
    (0, 0, 40, 18, (96, 66, 40)),         # tv casing
    (40, 0, 4, 7, (190, 190, 200)),       # antenna
    (0, 18, 24, 16, (58, 58, 64)),        # body
    (24, 18, 12, 17, (58, 58, 64)),       # arms
    (36, 18, 12, 23, (40, 40, 46)),       # legs
], dots=screen)

tear(f"{OUT}/item/rain_tear.png")
print("textures written")
