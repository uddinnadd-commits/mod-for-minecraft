#!/usr/bin/env python3
"""The bodies, in one place.

Writes client/ModModels.java and the matching entity textures from the same spec, so the UV
layout can never drift from the geometry. Boxes are packed onto a 128x128 sheet automatically
and every face is shaded (top light, bottom dark, back dim) with a little grain.

Run from the project root:  python3 tools/gen_models.py
"""
import math, os, random, struct, zlib

OUT_TEX = "src/main/resources/assets/sorrowed/textures/entity"
OUT_JAVA = "src/main/java/com/sorrowed/client/ModModels.java"
SHEET = 128

# ------------------------------------------------------------------ png

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

# ------------------------------------------------------------------ spec

class Part:
    def __init__(self, name, parent, box, pose=(0, 0, 0), rot=(0, 0, 0), color=(128, 128, 128),
                 mirror=False, dilate=0.0, alpha=255, face=None, shade=1.0):
        self.name, self.parent, self.box, self.pose, self.rot = name, parent, box, pose, rot
        self.color, self.mirror, self.dilate, self.alpha, self.face, self.shade = color, mirror, dilate, alpha, face, shade
        self.uv = None

MODELS = {}

def model(name, style, parts, translucent=False):
    MODELS[name] = {"style": style, "parts": parts, "translucent": translucent}

def shade(col, f):
    return tuple(max(0, min(255, int(c * f))) for c in col)

# A believable two-legged body. All sizes in 1/16 block; feet end at y=24.
# Each limb is three jointed pieces so elbows and knees can bend in SorrowModel.
def humanoid(head, neck, chest, abdomen, upper_arm, forearm, hand, thigh, shin, foot,
             colors, leg_gap=0.5, arm_drop=1.0, head_pose_dz=0.0):
    hw, hh, hd = head
    nw, nh, nd = neck
    cw, ch, cd = chest
    aw, ah, ad = abdomen
    uw, uh, ud = upper_arm
    fw, fh, fd = forearm
    gw, gh, gd = hand
    tw, th, td = thigh
    sw, sh, sd = shin
    ow, oh, od = foot
    leg_total = th + sh + oh
    leg_root = 24 - leg_total
    chest_top = leg_root - ah - ch
    parts = [
        Part("head", None, (-hw/2, -hh, -hd/2, hw, hh, hd), pose=(0, chest_top - nh, head_pose_dz), color=colors["head"]),
        Part("body", None, (-cw/2, 0, -cd/2, cw, ch, cd), pose=(0, chest_top, 0), color=colors["body"]),
        Part("neck", "body", (-nw/2, -nh, -nd/2, nw, nh, nd), color=colors["skin"]),
        Part("abdomen", "body", (-aw/2, ch, -ad/2, aw, ah, ad), color=colors["body"], shade=0.95),
        Part("right_arm", None, (-uw/2, -arm_drop, -ud/2, uw, uh, ud), pose=(-(cw/2 + uw/2), chest_top + arm_drop, 0), color=colors["arm"]),
        Part("right_forearm", "right_arm", (-fw/2, 0, -fd/2, fw, fh, fd), pose=(0, uh - arm_drop, 0), color=colors["arm"], shade=0.95),
        Part("right_hand", "right_forearm", (-gw/2, 0, -gd/2, gw, gh, gd), pose=(0, fh, 0), color=colors["skin"]),
        Part("left_arm", None, (-uw/2, -arm_drop, -ud/2, uw, uh, ud), pose=((cw/2 + uw/2), chest_top + arm_drop, 0), color=colors["arm"], mirror=True),
        Part("left_forearm", "left_arm", (-fw/2, 0, -fd/2, fw, fh, fd), pose=(0, uh - arm_drop, 0), color=colors["arm"], mirror=True, shade=0.95),
        Part("left_hand", "left_forearm", (-gw/2, 0, -gd/2, gw, gh, gd), pose=(0, fh, 0), color=colors["skin"], mirror=True),
        Part("right_leg", None, (-tw/2, 0, -td/2, tw, th, td), pose=(-(tw/2 + leg_gap), leg_root, 0), color=colors["leg"]),
        Part("right_shin", "right_leg", (-sw/2, 0, -sd/2, sw, sh, sd), pose=(0, th, 0), color=colors["leg"], shade=0.92),
        Part("right_foot", "right_shin", (-ow/2, 0, -(od - sd/2), ow, oh, od), pose=(0, sh, 0), color=colors["foot"]),
        Part("left_leg", None, (-tw/2, 0, -td/2, tw, th, td), pose=((tw/2 + leg_gap), leg_root, 0), color=colors["leg"], mirror=True),
        Part("left_shin", "left_leg", (-sw/2, 0, -sd/2, sw, sh, sd), pose=(0, th, 0), color=colors["leg"], mirror=True, shade=0.92),
        Part("left_foot", "left_shin", (-ow/2, 0, -(od - sd/2), ow, oh, od), pose=(0, sh, 0), color=colors["foot"], mirror=True),
    ]
    return parts, chest_top, leg_root

def eyes(hw, hh, color=(20, 20, 26), gap=2, y=None, size=1, shut=False):
    """Pixel dots on the front face of a head box, relative to the face's top-left."""
    y = hh // 2 if y is None else y
    cx = hw / 2
    dots = []
    for ex in (int(cx - gap/2 - size), int(cx + gap/2)):
        for dx in range(size):
            for dy in range(1 if shut else size):
                dots.append((ex + dx, y + dy, color))
    return dots

def fingers(parent, side, n=3, length=5, color=(40, 40, 46)):
    """Too-long fingers hanging from a hand. side -1 right, +1 left."""
    out = []
    for i in range(n):
        x = -1.0 + i * 1.0
        out.append(Part(f"finger_{parent}_{i}", parent, (x - 0.25, 0, -0.25, 0.5, length, 0.5), color=color, mirror=side > 0))
    return out

# ---- The Vigil (echo): thin, pale, see-through. Hollow eye sockets. The tear is black.
C = {"head": (206, 216, 228), "skin": (200, 210, 222), "body": (150, 170, 190), "arm": (150, 170, 190), "leg": (112, 128, 152), "foot": (90, 104, 126)}
parts, ct, lr = humanoid((6, 7, 6), (2, 3, 2), (6, 7, 3), (5, 5, 3), (2, 8, 2), (2, 8, 2), (2, 2, 2), (3, 8, 3), (3, 8, 3), (3, 1, 4), C)
parts += [
    Part("hair", "head", (-3, -7, -3, 6, 7, 6), dilate=0.4, color=(170, 182, 198), alpha=200),
    Part("collar", "body", (-3, 0, -1.5, 6, 2, 3), dilate=0.3, color=(120, 140, 160)),
] + fingers("right_hand", -1, length=5, color=(190, 200, 214)) + fingers("left_hand", 1, length=5, color=(190, 200, 214))
parts[0].face = [(x, y, (14, 16, 24)) for x in (1, 2, 4, 5) for y in (3, 4)] + [(2, 5, (10, 10, 14)), (2, 6, (10, 10, 14))]
model("echo", (0.25, 0.0, 0.0, 0.0, 0.6, 0.6), parts, translucent=True)

# ---- The Unsaid (hollow): hunched under a hood, the head cocked, no face, ribs under the rags, fingers to the ground.
C = {"head": (24, 24, 31), "skin": (18, 18, 24), "body": (18, 18, 24), "arm": (16, 16, 22), "leg": (12, 12, 16), "foot": (10, 10, 14)}
parts, ct, lr = humanoid((6, 8, 6), (2, 4, 2), (8, 8, 5), (7, 6, 4), (2.5, 11, 2.5), (2.5, 11, 2.5), (2.5, 2, 2.5), (3.5, 9, 3.5), (3.5, 9, 3.5), (3.5, 1, 5), C, head_pose_dz=1.5)
parts[0].rot = (0.15, 0, 0.35)                                   # the head hangs to one side
parts += [
    Part("hood", "head", (-3.5, -9, -3.5, 7, 9, 7), dilate=0.5, color=(12, 12, 17)),
    Part("shoulders", "body", (-5, 0, -3, 10, 3, 6), dilate=0.2, color=(14, 14, 19)),
    Part("rags", "abdomen", (-4, 10, -2.5, 8, 7, 5), dilate=0.1, color=(12, 12, 17), shade=0.8),
    Part("rib_1", "body", (-3.5, 3, -2.6, 7, 0.5, 0.3), color=(70, 66, 74)),
    Part("rib_2", "body", (-3.5, 5, -2.6, 7, 0.5, 0.3), color=(70, 66, 74)),
    Part("rib_3", "body", (-3, 7, -2.6, 6, 0.5, 0.3), color=(70, 66, 74)),
] + fingers("right_hand", -1, length=7, color=(14, 14, 19)) + fingers("left_hand", 1, length=7, color=(14, 14, 19))
model("hollow", (0.2, 0.3, 0.15, 0.15, 1.0, 0.7), parts)

# ---- Hark (listener): a huge round head, eyes sewn shut, a mouth that is a black slot, torn ears, a thin stooped body.
C = {"head": (170, 170, 176), "skin": (160, 160, 168), "body": (146, 146, 154), "arm": (130, 130, 138), "leg": (110, 110, 120), "foot": (90, 90, 100)}
parts, ct, lr = humanoid((11, 11, 11), (3, 2, 3), (6, 8, 4), (5, 6, 4), (2.5, 9, 2.5), (2.5, 9, 2.5), (2.5, 3, 2.5), (3, 9, 3), (3, 8, 3), (3, 1, 4), C)
parts += [
    Part("right_ear", "head", (-1, -6, -3, 1, 11, 6), pose=(-5.5, -5, 0), color=(150, 140, 146)),
    Part("left_ear", "head", (0, -6, -3, 1, 11, 6), pose=(5.5, -5, 0), color=(150, 140, 146), mirror=True),
    Part("brow", "head", (-5.5, -11, -5.5, 11, 3, 11), dilate=0.3, color=(158, 158, 166)),
    Part("jaw", "head", (-4, -2.5, -5.6, 8, 2.5, 1), color=(140, 136, 146)),
] + fingers("right_hand", -1, length=6, color=(120, 120, 130)) + fingers("left_hand", 1, length=6, color=(120, 120, 130))
stitch = [(x, 5, (24, 24, 28)) for x in (2, 3, 4, 6, 7, 8)] + [(x, y, (24, 24, 28)) for x in (2, 4, 6, 8) for y in (4, 6)]
mouth = [(x, y, (8, 8, 10)) for x in range(2, 9) for y in (8, 9)]
parts[0].face = stitch + mouth
model("listener", (0.1, 0.15, 0.05, 0.05, 0.3, 0.9), parts)

# ---- The Finisher (mason): broad and stocky, hard hat, tool belt, hammer held out in front. The only kind face here.
C = {"head": (200, 160, 130), "skin": (200, 160, 130), "body": (112, 78, 46), "arm": (150, 120, 90), "leg": (70, 70, 84), "foot": (50, 44, 40)}
parts, ct, lr = humanoid((8, 8, 8), (4, 2, 4), (10, 7, 6), (9, 5, 5), (4, 6, 4), (4, 6, 4), (4, 3, 4), (4, 6, 4), (4, 6, 4), (4, 1, 5), C)
parts += [
    Part("hat_brim", "head", (-5, -9, -5, 10, 1, 10), color=(240, 200, 40)),
    Part("hat_crown", "head", (-4, -12, -4, 8, 3, 8), color=(240, 200, 40)),
    Part("vest", "body", (-5, 0, -3, 10, 7, 6), dilate=0.5, color=(190, 110, 30)),
    Part("belt", "abdomen", (-4.5, 9, -2.5, 9, 2, 5), dilate=0.6, color=(60, 40, 24)),
    Part("pouch", "abdomen", (1, 9, -4, 3, 3, 2), color=(80, 56, 32)),
    Part("hammer_handle", "right_hand", (-0.5, 1, -7, 1, 1, 8), color=(110, 80, 50)),
    Part("hammer_head", "right_hand", (-1.5, -0.5, -9, 3, 4, 3), color=(130, 130, 140)),
    Part("boot_r", "right_foot", (-2, -1, -3, 4, 2, 5), dilate=0.3, color=(50, 44, 40)),
    Part("boot_l", "left_foot", (-2, -1, -3, 4, 2, 5), dilate=0.3, color=(50, 44, 40), mirror=True),
]
parts[0].face = eyes(8, 8, (40, 30, 24), gap=2) + [(x, 6, (120, 80, 60)) for x in range(3, 5)]
model("mason", (0.0, 0.0, -0.8, 0.0, 0.2, 1.0), parts)

# ---- The Archivist: 3 blocks of ink-dark robe, a neck far too long, a small bowed head with no eyes and a thin wide smile, the book held up.
C = {"head": (214, 206, 186), "skin": (204, 196, 176), "body": (30, 42, 34), "arm": (30, 42, 34), "leg": (22, 30, 26), "foot": (18, 24, 20)}
parts, ct, lr = humanoid((6, 6, 6), (2, 9, 2), (6, 7, 4), (5, 7, 3), (2, 9, 2), (2, 9, 2), (2, 2, 2), (2, 9, 2), (2, 9, 2), (2, 1, 3), C)
parts += [
    Part("robe", "abdomen", (-4, 7, -3, 8, 10, 6), dilate=0.2, color=(26, 36, 30), shade=0.9),
    Part("mantle", "body", (-4, 0, -3, 8, 4, 6), dilate=0.3, color=(36, 50, 40)),
    Part("sleeve_r", "right_forearm", (-1, 0, -1, 2, 6, 2), dilate=0.8, color=(26, 36, 30)),
    Part("sleeve_l", "left_forearm", (-1, 0, -1, 2, 6, 2), dilate=0.8, color=(26, 36, 30), mirror=True),
    Part("book", "left_hand", (-3, -1, -3, 5, 6, 1), color=(120, 80, 40)),
    Part("pages", "left_hand", (-2.5, -0.5, -2.2, 4, 5, 0.4), color=(236, 230, 210)),
] + fingers("right_hand", -1, length=6, color=(196, 188, 170))
parts[0].face = [(x, 4, (60, 50, 50)) for x in range(1, 5)] + [(0, 3, (60, 50, 50)), (5, 3, (60, 50, 50))]   # the smile, and nothing above it
model("archivist", (0.45, 0.08, 0.0, -1.3, 0.15, 0.35), parts)

# ---- The Neighbour: an ordinary person in a long coat and a wide hat. Under the brim, two pale points where the eyes would be.
C = {"head": (96, 80, 72), "skin": (120, 100, 90), "body": (116, 96, 64), "arm": (116, 96, 64), "leg": (38, 36, 40), "foot": (30, 28, 30)}
parts, ct, lr = humanoid((8, 8, 8), (4, 2, 4), (8, 7, 4), (8, 5, 4), (4, 6, 4), (4, 6, 4), (4, 3, 4), (4, 6, 4), (4, 6, 4), (4, 1, 5), C)
parts += [
    Part("hat_brim", "head", (-6, -8, -6, 12, 1, 12), color=(40, 40, 46)),
    Part("hat_crown", "head", (-3.5, -11.5, -3.5, 7, 4, 7), color=(44, 44, 50)),
    Part("hat_band", "head", (-3.5, -9, -3.5, 7, 1, 7), dilate=0.2, color=(90, 70, 50)),
    Part("coat", "body", (-4.5, 0, -2.5, 9, 7, 5), dilate=0.5, color=(108, 88, 58)),
    Part("coat_skirt", "abdomen", (-5, 5, -3, 10, 8, 6), color=(104, 84, 56), shade=0.9),
    Part("lapel", "body", (-3, 0, -2.6, 6, 4, 0.6), color=(90, 72, 46)),
    Part("cuff_r", "right_forearm", (-2, 4, -2, 4, 2, 4), dilate=0.4, color=(96, 78, 50)),
    Part("cuff_l", "left_forearm", (-2, 4, -2, 4, 2, 4), dilate=0.4, color=(96, 78, 50), mirror=True),
]
parts[0].face = [(x, y, (28, 24, 24)) for x in range(0, 8) for y in range(2, 8)] + [(2, 4, (220, 220, 200)), (5, 4, (220, 220, 200))]
model("neighbour", (0.0, 0.0, 0.0, 0.0, 0.1, 1.0), parts)

# ---- The Carrier: a wood-veneer television for a head, wires hanging from the neck, a face forming in the static.
C = {"head": (96, 66, 40), "skin": (170, 150, 140), "body": (58, 58, 64), "arm": (58, 58, 64), "leg": (40, 40, 46), "foot": (28, 28, 32)}
parts, ct, lr = humanoid((10, 8, 10), (3, 2, 3), (8, 7, 4), (7, 5, 3), (3, 7, 3), (3, 7, 3), (3, 3, 3), (3, 8, 3), (3, 8, 3), (3, 1, 4), C)
parts += [
    Part("screen_frame", "head", (-4, -7, -5.3, 8, 6, 0.4), color=(40, 30, 24)),
    Part("dial_a", "head", (3.2, -3, -5.4, 1, 1, 0.4), color=(200, 200, 210)),
    Part("dial_b", "head", (3.2, -5, -5.4, 1, 1, 0.4), color=(200, 200, 210)),
    Part("right_antenna", "head", (-0.5, -6, -0.5, 1, 6, 1), pose=(-2, -8, 0), rot=(0, 0, 0.5), color=(190, 190, 200)),
    Part("left_antenna", "head", (-0.5, -6, -0.5, 1, 6, 1), pose=(2, -8, 0), rot=(0, 0, -0.5), color=(190, 190, 200)),
    Part("wire_a", "neck", (-1.5, -2, -1.5, 0.5, 6, 0.5), color=(20, 20, 24)),
    Part("wire_b", "neck", (1.0, -2, 1.0, 0.5, 7, 0.5), color=(120, 30, 30)),
    Part("tie", "body", (-0.5, 0, -2.4, 1, 6, 0.4), color=(120, 30, 30)),
    Part("jacket", "body", (-4, 0, -2, 8, 7, 4), dilate=0.4, color=(50, 50, 56)),
]
rnd = random.Random("carrier-screen")
face = []
for x in range(1, 9):
    for y in range(1, 7):
        v = rnd.choice((20, 90, 170, 235))
        if (x in (3, 6) and y == 3) or (y == 5 and 3 <= x <= 6):   # eyes and a mouth, in the noise
            v = 6
        face.append((x, y, (v, v, v)))
parts[0].face = face
model("carrier", (0.0, 0.05, 0.0, 0.0, 0.5, 0.8), parts)

# ---- The Orderly: three blocks tall, a white coat, a mask, bent forward over the empty bed it pushes.
C = {"head": (210, 200, 190), "skin": (210, 200, 190), "body": (236, 236, 240), "arm": (236, 236, 240), "leg": (90, 110, 120), "foot": (40, 40, 44)}
parts, ct, lr = humanoid((7, 8, 7), (3, 4, 3), (8, 9, 4), (7, 6, 4), (3, 9, 3), (3, 9, 3), (3, 2, 3), (3, 10, 3), (3, 10, 3), (3, 1, 4), C)
parts += [
    Part("mask", "head", (-3.5, -4, -3.8, 7, 3.5, 0.6), color=(170, 200, 190)),
    Part("cap", "head", (-3.5, -9, -3.5, 7, 2, 7), dilate=0.2, color=(170, 200, 190)),
    Part("coat", "body", (-4, 0, -2, 8, 9, 4), dilate=0.6, color=(232, 232, 236)),
    Part("coat_skirt", "abdomen", (-4.5, 6, -2.5, 9, 9, 5), color=(228, 228, 232), shade=0.92),
    Part("gurney_frame", "body", (-6, 14, -22, 12, 1, 20), color=(150, 150, 158)),
    Part("gurney_mattress", "body", (-5.5, 13, -21, 11, 1, 18), color=(240, 240, 240)),
    Part("gurney_pillow", "body", (-4, 12, -20, 8, 1, 4), color=(250, 250, 250)),
    Part("gurney_rail", "body", (-6, 10, -22, 12, 0.6, 0.6), color=(150, 150, 158)),
    Part("leg_a", "body", (-5.5, 15, -20, 1, 9, 1), color=(120, 120, 128)),
    Part("leg_b", "body", (4.5, 15, -20, 1, 9, 1), color=(120, 120, 128)),
    Part("leg_c", "body", (-5.5, 15, -5, 1, 9, 1), color=(120, 120, 128)),
    Part("leg_d", "body", (4.5, 15, -5, 1, 9, 1), color=(120, 120, 128)),
] + fingers("right_hand", -1, length=5, color=(200, 190, 180)) + fingers("left_hand", 1, length=5, color=(200, 190, 180))
parts[0].face = [(2, 2, (30, 30, 36)), (4, 2, (30, 30, 36))]   # two small eyes above the mask; nothing else
model("orderly", (0.35, 0.45, -1.35, -1.35, 0.1, 0.9), parts)

# ---- The Scarecrow: a burlap sack for a head with an X for each eye, a crossbar for shoulders, straw at every seam.
C = {"head": (150, 120, 80), "skin": (150, 120, 80), "body": (80, 60, 40), "arm": (80, 60, 40), "leg": (60, 48, 36), "foot": (40, 32, 24)}
parts, ct, lr = humanoid((7, 8, 7), (2, 3, 2), (7, 9, 4), (6, 5, 3), (2.5, 8, 2.5), (2.5, 8, 2.5), (2.5, 2, 2.5), (3, 9, 3), (3, 9, 3), (3, 1, 4), C)
parts += [
    Part("sack_tie", "head", (-1.5, 0, -1.5, 3, 2, 3), color=(100, 80, 50)),
    Part("hat", "head", (-5, -9, -5, 10, 1, 10), color=(50, 40, 30)),
    Part("hat_top", "head", (-3, -12, -3, 6, 3, 6), color=(50, 40, 30)),
    Part("crossbar", "body", (-11, 1, -0.5, 22, 1, 1), color=(110, 90, 60)),
    Part("pole", "abdomen", (-0.5, 5, 0.5, 1, 16, 1), color=(110, 90, 60)),
    Part("straw_neck", "neck", (-2, -1, -2, 4, 2, 4), dilate=0.3, color=(200, 170, 90)),
    Part("straw_r", "right_hand", (-1.5, 0, -1.5, 3, 4, 3), color=(200, 170, 90)),
    Part("straw_l", "left_hand", (-1.5, 0, -1.5, 3, 4, 3), color=(200, 170, 90), mirror=True),
    Part("straw_leg_r", "right_shin", (-2, 7, -2, 4, 3, 4), color=(200, 170, 90)),
    Part("straw_leg_l", "left_shin", (-2, 7, -2, 4, 3, 4), color=(200, 170, 90), mirror=True),
    Part("patch", "body", (-3, 3, -2.3, 3, 3, 0.4), color=(60, 40, 30)),
]
xeye = []
for (cx, cy) in ((2, 3), (5, 3)):
    xeye += [(cx - 1, cy - 1, (30, 20, 16)), (cx + 1, cy - 1, (30, 20, 16)), (cx, cy, (30, 20, 16)), (cx - 1, cy + 1, (30, 20, 16)), (cx + 1, cy + 1, (30, 20, 16))]
stitchmouth = [(x, 6, (30, 20, 16)) for x in range(1, 7)] + [(x, 5, (30, 20, 16)) for x in (2, 4, 6)] + [(x, 7, (30, 20, 16)) for x in (1, 3, 5)]
parts[0].face = xeye + stitchmouth
model("scarecrow", (0.0, 0.0, 0.0, 0.0, 0.0, 1.1), parts)

# ------------------------------------------------------------------ uv packing

def footprint(p):
    _, _, _, w, h, d = p.box
    w, h, d = math.ceil(w), math.ceil(h), math.ceil(d)
    return 2 * (w + d), h + d

def pack(parts):
    """Shelf packing, tallest first, onto the sheet. Returns False if it does not fit."""
    order = sorted(parts, key=lambda p: -footprint(p)[1])
    x = y = shelf = 0
    for p in order:
        fw, fh = footprint(p)
        if x + fw > SHEET:
            x = 0
            y += shelf
            shelf = 0
        if y + fh > SHEET:
            return False
        p.uv = (x, y)
        x += fw
        shelf = max(shelf, fh)
    return True

# ------------------------------------------------------------------ texture painting

def paint(name, m):
    px = [(0, 0, 0, 0)] * (SHEET * SHEET)
    rnd = random.Random(name)
    for p in m["parts"]:
        u, v = p.uv
        _, _, _, w, h, d = p.box
        w, h, d = math.ceil(w), math.ceil(h), math.ceil(d)
        base = shade(p.color, p.shade)
        faces = {  # (x0, y0, width, height, brightness)
            "top": (u + d, v, w, d, 1.14), "bottom": (u + d + w, v, w, d, 0.66),
            "right": (u, v + d, d, h, 0.88), "front": (u + d, v + d, w, h, 1.0),
            "left": (u + d + w, v + d, d, h, 0.88), "back": (u + 2 * d + w, v + d, w, h, 0.78),
        }
        for fname, (x0, y0, fw, fh, bright) in faces.items():
            for yy in range(fh):
                grad = 1.0 - 0.10 * (yy / max(1, fh - 1))   # darker toward the bottom of each face
                for xx in range(fw):
                    edge = 0.9 if (xx == 0 or xx == fw - 1 or yy == 0 or yy == fh - 1) and fw > 2 and fh > 2 else 1.0
                    n = rnd.randint(-5, 5)
                    col = shade(base, bright * grad * edge)
                    col = tuple(max(0, min(255, c + n)) for c in col)
                    X, Y = x0 + xx, y0 + yy
                    if 0 <= X < SHEET and 0 <= Y < SHEET:
                        px[Y * SHEET + X] = (*col, p.alpha)
        if p.face:
            fx, fy = faces["front"][0], faces["front"][1]
            for (dx, dy, col) in p.face:
                X, Y = fx + dx, fy + dy
                if 0 <= X < SHEET and 0 <= Y < SHEET:
                    px[Y * SHEET + X] = (*col, 255)
    write_png(f"{OUT_TEX}/{name}.png", SHEET, SHEET, px)

# ------------------------------------------------------------------ java emit

def f(v):
    return f"{float(v):.1f}F" if float(v) == int(v) else f"{float(v):.2f}F"

def emit_java():
    out = []
    out.append("package com.sorrowed.client;\n")
    out.append("import com.sorrowed.Sorrowed;")
    out.append("import net.minecraft.client.model.geom.ModelLayerLocation;")
    out.append("import net.minecraft.client.model.geom.PartPose;")
    out.append("import net.minecraft.client.model.geom.builders.CubeDeformation;")
    out.append("import net.minecraft.client.model.geom.builders.CubeListBuilder;")
    out.append("import net.minecraft.client.model.geom.builders.LayerDefinition;")
    out.append("import net.minecraft.client.model.geom.builders.MeshDefinition;")
    out.append("import net.minecraft.client.model.geom.builders.PartDefinition;")
    out.append("import net.minecraft.resources.ResourceLocation;\n")
    out.append("/**")
    out.append(" * GENERATED by tools/gen_models.py together with the entity textures. Edit the spec there, not here.")
    out.append(" * Coordinates are 1/16 block, y down, feet at y=24. Limbs are jointed (arm > forearm > hand,")
    out.append(" * leg > shin > foot) so SorrowModel can bend elbows and knees.")
    out.append(" */")
    out.append("public final class ModModels {")
    out.append("    private ModModels() {}\n")
    out.append("    private static ModelLayerLocation layer(String name) {")
    out.append("        return new ModelLayerLocation(new ResourceLocation(Sorrowed.MODID, name), \"main\");")
    out.append("    }\n")
    for name in MODELS:
        out.append(f"    public static final ModelLayerLocation {name.upper()} = layer(\"{name}\");")
    out.append("")
    out.append("    // headBow, hunch, rightArmRest, leftArmRest, armSway, walk")
    for name, m in MODELS.items():
        s = ", ".join(f(v) for v in m["style"])
        out.append(f"    public static final SorrowModel.Style {name.upper()}_STYLE = new SorrowModel.Style({s});")
    out.append("")
    for name, m in MODELS.items():
        out.append(f"    public static LayerDefinition {name}Layer() {{")
        out.append("        MeshDefinition mesh = new MeshDefinition();")
        out.append("        PartDefinition root = mesh.getRoot();")
        var = {None: "root"}
        for p in m["parts"]:
            parent = var[p.parent]
            x, y, z, w, h, d = p.box
            cube = f"CubeListBuilder.create().texOffs({p.uv[0]}, {p.uv[1]})" + (".mirror()" if p.mirror else "")
            cube += f".addBox({f(x)}, {f(y)}, {f(z)}, {f(w)}, {f(h)}, {f(d)}"
            cube += f", new CubeDeformation({f(p.dilate)}))" if p.dilate else ")"
            px_, py_, pz_ = p.pose
            rx, ry, rz = p.rot
            if any(p.rot):
                pose = f"PartPose.offsetAndRotation({f(px_)}, {f(py_)}, {f(pz_)}, {f(rx)}, {f(ry)}, {f(rz)})"
            elif any(p.pose):
                pose = f"PartPose.offset({f(px_)}, {f(py_)}, {f(pz_)})"
            else:
                pose = "PartPose.ZERO"
            v = p.name.replace("_", "")
            var[p.name] = v
            out.append(f"        PartDefinition {v} = {parent}.addOrReplaceChild(\"{p.name}\", {cube}, {pose});")
        out.append(f"        return LayerDefinition.create(mesh, {SHEET}, {SHEET});")
        out.append("    }\n")
    out.append("}")
    with open(OUT_JAVA, "w") as fh:
        fh.write("\n".join(out) + "\n")

for name, m in MODELS.items():
    if not pack(m["parts"]):
        raise SystemExit(f"{name}: parts do not fit on a {SHEET}x{SHEET} sheet")
    paint(name, m)
emit_java()
print("models and textures written")
