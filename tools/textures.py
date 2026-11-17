"""Generates the gas mask textures (no external libraries). Run: python tools/textures.py"""
import struct
import zlib
import os

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'fumes', 'textures')


def png(path, w, h, px):
    raw = b''.join(b'\x00' + b''.join(bytes(px[y][x]) for x in range(w)) for y in range(h))
    def chunk(t, d):
        return struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)) \
        + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'wb').write(data)


T = (0, 0, 0, 0)
RUBBER = (58, 62, 56, 255)
RUBBER_D = (38, 41, 37, 255)
RUBBER_L = (80, 86, 76, 255)
GLASS = (150, 205, 220, 255)
GLASS_L = (215, 240, 248, 255)
RIM = (120, 120, 112, 255)
FILTER = (104, 112, 96, 255)
FILTER_D = (70, 76, 64, 255)
STRAP = (92, 70, 48, 255)


def item():
    rows = [
        "................",
        "....dddddddd....",
        "...drrrrrrrrd...",
        "..drrrrrrrrrrd..",
        "..drmmrrrrmmrd..",
        "..dmggmrrmggmd..",
        "..dmgwmrrmgwmd..",
        "..drmmrrrrmmrd..",
        "..drrrrlrrrrrd..",
        "...drrrllrrrd...",
        "....drffffrd....",
        ".....dfFFfd.....",
        ".....dffffd.....",
        ".....dFFFFd.....",
        "......dddd......",
        "................",
    ]
    pal = {'.': T, 'd': RUBBER_D, 'r': RUBBER, 'l': RUBBER_L, 'm': RIM, 'g': GLASS, 'w': GLASS_L, 'f': FILTER, 'F': FILTER_D}
    png(os.path.join(ROOT, 'item', 'gas_mask.png'), 16, 16, [[pal[c] for c in r] for r in rows])


def armor():
    w, h = 64, 32
    px = [[T for _ in range(w)] for _ in range(h)]

    def put(x, y, c):
        px[y][x] = c

    # head cube faces (8x8 each): top (8,0) bottom (16,0) right (0,8) front (8,8) left (16,8) back (24,8)
    # straps around the head at eye height
    for x in range(0, 32):
        put(x, 11, STRAP)
    for x in range(8, 16):  # strap over the top
        put(x, 4, STRAP)
    # front face: lower 6 rows rubber, round eye glasses, filter at the bottom
    for y in range(10, 16):
        for x in range(8, 16):
            put(x, y, RUBBER)
    for x in range(8, 16):
        put(x, 10, RUBBER_D)
    for (ex, ey) in ((9, 11), (13, 11)):
        for dx in range(2):
            for dy in range(2):
                put(ex + dx, ey + dy, GLASS if (dx, dy) != (0, 0) else GLASS_L)
    for x in range(11, 13):
        put(x, 13, RUBBER_L)
    for x in range(10, 14):
        put(x, 14, FILTER)
        put(x, 15, FILTER_D)
    # cheeks on the side faces
    for y in range(12, 16):
        for x in range(4, 8):
            put(x, y, RUBBER)
        for x in range(16, 20):
            put(x, y, RUBBER)
    png(os.path.join(ROOT, 'entity', 'equipment', 'humanoid', 'gas_mask.png'), w, h, px)


item()
armor()
print('textures written')
