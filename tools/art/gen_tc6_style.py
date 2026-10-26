"""Thaumcraft Remaster: regenerate invented textures in TC6 style.
Palettes sampled from TC6 textures; shapes are hand-placed pixel maps."""
import os, random, math, sys
from PIL import Image, ImageDraw, ImageFilter

R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../src/main/resources/assets/thaumcraft/textures/')
OUT = sys.argv[1] if len(sys.argv) > 1 else R
REF = R   # TC6 originals used as bases/palette (gui_deconstruction only gets its bar repainted)

def hx(s):
    s = s.lstrip('#'); return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)

def load(p): return Image.open(REF + p + '.png').convert('RGBA')

def grid(rows, pal, size=None):
    h = len(rows); w = max(len(r) for r in rows)
    im = Image.new('RGBA', size or (w, h), (0, 0, 0, 0))
    for y, r in enumerate(rows):
        for x, c in enumerate(r):
            if c in pal and pal[c] is not None:
                im.putpixel((x, y), pal[c] if len(pal[c]) == 4 else pal[c] + (255,))
    return im

def save(im, p):
    path = OUT + p + '.png'
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path)

def mul(c, f): return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3] if len(c) > 3 else 255,)
def lerp(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)

# ---- palettes (sampled from TC6) ----
BRASS = dict(K=hx('1e1507'), d=hx('543b14'), D=hx('6c4d19'), b=hx('a17125'), B=hx('d2983a'), h=hx('e1bc80'), H=hx('ecd4ad'))
LEATH = dict(k=hx('0b0705'), o=hx('241913'), m=hx('453025'), l=hx('5f4232'), L=hx('664737'), y=hx('714f3c'))
POT = dict(q=hx('003d37'), g=hx('009485'), G=hx('49ffed'), w=hx('c8fff9'))
WOOD = dict(W=hx('211506'), x=hx('2e200e'), X=hx('3a2a14'), v=hx('4b341d'), V=hx('5c4226'))
PAL = {}
for p in (BRASS, LEATH, POT, WOOD): PAL.update(p)
PAL['.'] = None

# ---------------------------------------------------------------- harness item
def harness_item():
    rows = [
        "................",
        "..oo........oo..",
        ".oLyo......oyLo.",
        ".olLmo....omLlo.",
        ".olLmKKKKKKmLlo.",
        ".olLKhHHBBbKLlo.",
        ".olmKHBgGqbKmlo.",
        ".ollKBgGwgdKllo.",
        ".olmKBqggqdKmlo.",
        ".ollKbBddddKllo.",
        ".olLmKKKKKKmLlo.",
        ".oKKKKKKKKKKKKo.",
        ".oKhBBBHBBBBbKo.",
        ".oKddddKdddddKo.",
        ".olLmo....omLlo.",
        "..ooo......ooo..",
    ]
    im = grid(rows, PAL)
    save(im, 'item/hover_harness')

def harness_armor():
    im = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    def put(x, y, rows):
        for j, r in enumerate(rows):
            for i, c in enumerate(r):
                if PAL.get(c): im.putpixel((x + i, y + j), PAL[c])
    # body front (20,20) 8x12 : leather straps over shoulders + crossing buckle, brass belt
    put(20, 20, [
        "lL....Ll",
        "lL....Ll",
        "mlL..Llm",
        ".mlLLlm.",
        "..KhBK..",
        "..KBdK..",
        ".mlLLlm.",
        "mlL..Llm",
        "KKKKKKKK",
        "hBBHBBBb",
        "ddddKddd",
        "........",
    ])
    # body back (32,20): brass casing with potentia core
    put(32, 20, [
        "lKKKKKKl",
        "lKhHBBKl",
        "mKHgGqdm",
        "lKBGwgdl",
        "lKBqggdl",
        "mKbBdddm",
        "lKKKKKKl",
        "lL.KK.Ll",
        "KKKKKKKK",
        "hBBBHBBb",
        "dddKdddd",
        "........",
    ])
    # sides (16,20) and (28,20): strap + belt
    for sx in (16, 28):
        put(sx, 20, ["....", "....", "....", "....", "....", "....", "....", "....",
                     "KKKK", "BBBb", "dddd", "...."])
    # top of body (20,16) shoulder straps
    put(20, 16, ["lL....Ll", "lL....Ll", "lL....Ll", "lL....Ll"])
    # top back of casing on top face edge
    put(20, 19, ["KK....KK"])
    save(im, 'entity/equipment/humanoid/hover_harness')

# ---------------------------------------------------------------- girdle
def girdle():
    im = load('item/girdle_fancy')
    px = im.load()
    # recolour gold buckle -> brass, cyan gem -> potentia (already cyan in TC6 fancy girdle: keep, brighten)
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            if a and r > g > b and r - b > 60 and g > 30 and r > 60:  # gold
                lum = (r + g) / 2 / 240
                ramp = [BRASS['d'], BRASS['b'], BRASS['B'], BRASS['h'], BRASS['H']]
                px[x, y] = ramp[min(4, int(lum * 5))]
    # brass studs + little wing tufts (thaumostatic vanes) on the band
    for (x, y, c) in [(1, 7, 'h'), (14, 7, 'h'), (1, 8, 'b'), (14, 8, 'b'),
                      (5, 3, 'B'), (10, 3, 'B')]:
        px[x, y] = PAL[c]
    save(im, 'item/hover_girdle')

# ---------------------------------------------------------------- travelling trunk item
def trunk_item():
    rows = [
        "................",
        "................",
        "..WWWWWWWWWWWW..",
        ".WVvVvVvVvVvVvW.",
        ".WhvVvXvvVvXvbW.",
        ".WKKKKKKKKKKKKW.",
        ".WhxXxhHBhxXxbW.",
        ".WvXvXKdDKvXvXW.",
        ".WXvXvXKKXvXvXW.",
        ".WvXvXvXvXvXvXW.",
        ".WhXvXvXvXvXvbW.",
        ".WKKKKKKKKKKKKW.",
        "..WxW......WxW..",
        "..WWW......WWW..",
        "................",
        "................",
    ]
    im = grid(rows, PAL)
    save(im, 'item/travelling_trunk')

# ---------------------------------------------------------------- seals
SEAL_FR = [  # per-frame (edge, mid, hi, shadow)
    (hx('a0621d'), hx('a68f12'), hx('a6915f'), hx('55221c')),
    (hx('c17025'), hx('c3b014'), hx('c3b373'), hx('5c291d')),
    (hx('d4752e'), hx('d7c814'), hx('d7c981'), hx('62301e')),
]
def seal(name, glyph):
    base = load('item/seals/seal_guard')
    out = base.copy(); px = out.load()
    rnd = random.Random(name)
    for f in range(3):
        e, m, h, s = SEAL_FR[f]
        # clear interior to the seal's wax noise
        for y in range(3, 13):
            for x in range(3, 13):
                d = math.hypot(x - 7.5, y - 7.5)
                if d < 4.6:
                    t = 0.35 + 0.25 * (d / 4.6) + rnd.uniform(-0.08, 0.08)
                    px[x, y + 16 * f] = lerp(hx('2e0f0d'), hx('7f2a27'), max(0, min(1, t)))
        for j, r in enumerate(glyph):
            for i, c in enumerate(r):
                col = {'s': s, 'o': e, 'y': m, 'h': h}.get(c)
                if col: px[4 + i, 4 + j + 16 * f] = col
    save(out, 'item/seals/' + name)

def seals():
    seal('seal_fishing', [
        "....sy..",
        "...soys.",
        "....sy..",
        "....sy..",
        "....sy..",
        "y..syo..",
        "oy.hys..",
        ".oyos...",
    ])
    seal('seal_fluid', [
        "...so...",
        "...yo...",
        "..soys..",
        ".soyhos.",
        ".oyhhyo.",
        ".oyyhyo.",
        ".soyyos.",
        "..soos..",
    ])
    seal('seal_essentia', [
        "..oyyo..",
        "...hy...",
        "..syos..",
        ".soyyos.",
        ".oyhhyo.",
        ".oyhyyo.",
        ".soyyos.",
        "..oooo..",
    ])

# ---------------------------------------------------------------- mana pod / bean (grayscale, tinted)
def mana():
    g = lambda v: (v, v, v, 255)
    rnd = random.Random(7)
    im = Image.new('RGBA', (16, 16)); px = im.load()
    ribs = [0.55, 0.85, 1.0, 0.8, 0.5]  # rib profile repeated
    for y in range(16):
        for x in range(16):
            rib = [0.62, 0.9, 1.0, 0.78][x % 4]
            vshade = 1.0 - 0.35 * abs(y - 6) / 10
            v = 120 + 120 * rib * vshade + rnd.randint(-6, 6)
            px[x, y] = g(int(max(40, min(250, v))))
    for x in range(16):  # groove lines
        if x % 4 == 3:
            for y in range(16): px[x, y] = mul(px[x, y], 0.62)
    for y in (0, 15):
        for x in range(16): px[x, y] = mul(px[x, y], 0.7)
    for y in (2, 3):  # pale crown band near the stem (TC4 pods had lighter tips)
        for x in range(16): px[x, y] = mul(px[x, y], 1.08)
    save(im, 'block/mana_pod')
    stem = Image.new('RGBA', (16, 16)); sp = stem.load()
    for y in range(16):
        for x in range(16):
            sp[x, y] = [hx('2e4a14'), hx('4c6b1f'), hx('6d8a2c'), hx('3d5a18')][(x + y // 3) % 4]
    save(stem, 'block/mana_pod_stem')
    rows = [
        "................",
        "................",
        "................",
        "................",
        "......kkkk......",
        "....kkhwwhkk....",
        "...khwwhhwlmk...",
        "..khwhllllmmdk..",
        "..kwhlllmmmddk..",
        "..klllkkmmmddk..",
        "...kmk..kmddk...",
        "....k....kkk....",
        "................",
        "................",
        "................",
        "................",
    ]
    bp = {'k': g(52), 'd': g(110), 'm': g(150), 'l': g(185), 'h': g(220), 'w': g(250), '.': None}
    save(grid(rows, bp), 'item/mana_bean')

# ---------------------------------------------------------------- deconstruction table top
def decon_top():
    im = load('block/table_wood_top'); px = im.load()
    # silverwood-inlaid ring with a split (broken) aspect triangle, thaumium glyph glow
    sil = [hx('8f8478'), hx('c9bba4'), hx('efe3c8')]
    th = [hx('2e1f3a'), hx('604179'), hx('9a73c0'), hx('d0b0ff')]
    ring = [(5,2),(6,2),(7,2),(8,2),(9,2),(10,2),(4,3),(11,3),(3,4),(12,4),(2,5),(13,5),(2,6),(13,6),(2,7),(13,7),
            (2,8),(13,8),(2,9),(13,9),(2,10),(13,10),(3,11),(12,11),(4,12),(11,12),(5,13),(6,13),(7,13),(8,13),(9,13),(10,13)]
    for i, (x, y) in enumerate(ring):
        px[x, y] = sil[2] if (y < 6 and x < 9) else sil[1] if y < 10 else sil[0]
        # dark groove inside
    for (x, y) in [(5,3),(6,3),(7,3),(8,3),(9,3),(10,3),(4,4),(11,4),(3,5),(12,5),(3,6),(12,6),(3,7),(12,7),(3,8),(12,8),(3,9),(12,9),(3,10),(12,10),(4,11),(11,11),(5,12),(10,12),(6,12),(7,12),(8,12),(9,12)]:
        px[x, y] = mul(px[x, y], 0.55)
    tri = {
        (7,5):3,(8,5):3,
        (6,6):2,(7,6):1,(9,7):2,
        (6,7):2,(5,8):2,(10,8):2,(5,9):1,(10,9):1,
        (5,10):1,(6,10):2,(9,10):2,(10,10):1,
        (8,6):0,(8,7):0,(7,8):0,(8,9):0,(7,10):0,  # crack
        (9,6):1,
    }
    for (x, y), c in tri.items(): px[x, y] = th[c]
    save(im, 'block/deconstruction_table_top')

def decon_gui():
    im = load('gui/gui_deconstruction'); px = im.load()
    # progress fill 176,0  w<=? x 9: thaumium-to-vis gradient with highlight row
    w = 0
    while w < 80 and px[176 + w, 4][3] > 0: w += 1
    for x in range(w):
        t = x / max(1, w - 1)
        base = lerp(hx('4a2c66'), hx('b07ce0'), t)
        for y in range(9):
            f = [0.55, 1.25, 1.1, 1.0, 1.0, 0.95, 0.85, 0.75, 0.5][y]
            px[176 + x, y] = mul(base, f)
        if x % 6 == 2: px[176 + x, 2] = hx('f0dcff')
    save(im, 'gui/gui_deconstruction')

# ---------------------------------------------------------------- Outer Lands stones
def crust_overlay(im, seed, glow=False, density=1.0):
    px = im.load(); rnd = random.Random(seed)
    cr = [hx('1a1616'), hx('3b3333'), hx('6a5e58'), hx('8c7f76'), hx('a89a8f')]
    blobs = [(3, 3, 2.6), (11, 4, 2.2), (6, 10, 3.0), (13, 12, 2.0), (1, 13, 1.6), (9, 1, 1.4)]
    mask = {}
    for bx, by, r in blobs:
        for y in range(16):
            for x in range(16):
                dx = min(abs(x - bx), 16 - abs(x - bx)); dy = min(abs(y - by), 16 - abs(y - by))
                d = math.hypot(dx, dy) + rnd.uniform(-0.5, 0.5)
                if d < r: mask[(x, y)] = max(mask.get((x, y), 0), 1 - d / r)
    for (x, y), v in mask.items():
        up = mask.get((x, (y - 1) % 16), 0); dn = mask.get((x, (y + 1) % 16), 0)
        edge = up == 0 or dn == 0 or mask.get(((x - 1) % 16, y), 0) == 0 or mask.get(((x + 1) % 16, y), 0) == 0
        if edge and dn == 0: c = cr[0]
        elif edge: c = cr[2]
        else: c = cr[3] if up > dn else cr[2]
        if not edge and v > 0.75: c = cr[4]
        px[x, y] = c
        if glow and not edge and v > 0.45:
            px[x, y] = [hx('c87a10'), hx('f0b020'), hx('fff27a')][min(2, int((v - 0.45) * 6))]
        elif glow and not edge and v > 0.3:
            px[x, y] = hx('7a4410')
    return im

def outer():
    save(crust_overlay(load('block/ancient_stone_0'), 1), 'block/crusted_stone')
    save(crust_overlay(load('block/ancient_stone_0'), 1, glow=True), 'block/crusted_stone_glow')
    # rune stone: ancient stone with inlay_rune0 glyph column carved + faint violet glow
    im = load('block/ancient_stone_0'); px = im.load()
    rune = load('block/inlay_rune0').load()
    for y in range(16):
        for x in range(16):
            if rune[x, y][3] > 0 and rune[x, y][0] > 100:
                px[x, y] = hx('8a6fb0') if (y % 4) < 2 else hx('6a4f90')
                if x + 1 < 16: px[x + 1, y] = mul(px[x + 1, y], 0.5)
    save(im, 'block/eldritch_rune_stone')
    # crab spawner: crusted ring with a dark burrow
    im = crust_overlay(load('block/ancient_stone_0'), 5); px = im.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 3.2: px[x, y] = hx('050404') if d < 2.2 else hx('120e0e')
            elif d < 4.6: px[x, y] = hx('8c7f76') if y < 7 else hx('5a4e4a') if y < 10 else hx('3b3333')
            elif d < 5.4: px[x, y] = hx('1a1616')
    save(im, 'block/eldritch_crab_spawner')
    # lock (32x32 = 16 design x2): eldritch stone with inlaid brass/void ring and keyhole glyph
    st = load('block/eldritch_stone_1'); sp = st.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 4.3 < d < 5.6: sp[x, y] = hx('8a6fb0') if y < 8 else hx('5a3f80')
            elif 5.6 <= d < 6.3 and y > 7: sp[x, y] = hx('050406')
            elif d <= 4.3 and d > 3.4: sp[x, y] = hx('0c0b0f')
    key = ["..##..", ".#oo#.", ".#oo#.", "..oo..", ".oooo.", "......"]
    for j, r in enumerate(key):
        for i, c in enumerate(r):
            if c == '#': sp[5 + i, 4 + j] = hx('b090e0')
            elif c == 'o': sp[5 + i, 4 + j] = hx('d8c0ff')
    for (x, y) in [(7, 5), (8, 5), (7, 6), (8, 6), (7, 7), (8, 7), (6, 8), (7, 8), (8, 8), (9, 8)]:
        sp[x, y] = hx('d8c0ff') if y < 7 else hx('b090e0')
    for (x, y) in [(7, 9), (8, 9)]: sp[x, y] = hx('050406')
    save(st.resize((32, 32), Image.NEAREST), 'block/eldritch_lock')

def eye():
    rows = [
        "................",
        "................",
        "................",
        ".....kkkkkk.....",
        "....kpPPPPpk....",
        "...kpPLLLLPpk...",
        "..kpPLhwwhLPpk..",
        "..kPLhwKKwhLPk..",
        "..kPLhwKKwhLPk..",
        "..kpPLhwwhLPpk..",
        "...kpPLLLLPpk...",
        "....kpPPPPpk....",
        ".....kkkkkk.....",
        "................",
        "................",
        "................",
    ]
    p = {'k': hx('0d0617'), 'p': hx('30174f'), 'P': hx('4f2a7c'), 'L': hx('7450b8'),
         'h': hx('b58ae0'), 'w': hx('e8d4ff'), 'K': hx('000000'), '.': None}
    im = grid(rows, p); px = im.load()
    px[6, 5] = hx('9a73c0'); px[5, 6] = hx('9a73c0')  # gloss
    save(im, 'item/eldritch_eye')

def tablet():
    st = load('block/ancient_stone_0').load()
    im = Image.new('RGBA', (16, 16)); px = im.load()
    for y in range(2, 15):
        for x in range(3, 13):
            px[x, y] = st[x, y]
    for y in range(2, 15):
        px[3, y] = hx('141212'); px[12, y] = hx('141212')
    for x in range(3, 13):
        px[x, 2] = hx('141212'); px[x, 14] = hx('141212')
    for y in range(3, 14): px[4, y] = mul(px[4, y], 1.25)
    for x in range(4, 12): px[x, 3] = mul(px[x, 3], 1.25)
    for y in range(3, 14): px[11, y] = mul(px[11, y], 0.7)
    for x in range(4, 12): px[x, 13] = mul(px[x, 13], 0.7)
    glyph = ["......", ".#..#.", ".####.", "..#...", ".###..", "...#..", ".#.##.", ".####.", "......"]
    for j, r in enumerate(glyph):
        for i, c in enumerate(r):
            if c == '#': px[5 + i, 4 + j] = hx('8a6fb0') if j % 2 else hx('b090e0')
    # chipped corner
    px[12, 2] = (0, 0, 0, 0); px[11, 2] = hx('141212'); px[12, 3] = hx('141212')
    save(im, 'item/runed_tablet')

# ---------------------------------------------------------------- foci (brushy white, like TC6 icons)
def soft_icon(draw_fn, name):
    S = 256
    big = Image.new('L', (S, S), 0); d = ImageDraw.Draw(big)
    draw_fn(d, S)
    small = big.resize((32, 32), Image.LANCZOS)
    a = small.point(lambda v: 255 if v > 150 else (int(v * 1.4) if v > 50 else 0))
    im = Image.new('RGBA', (32, 32), (255, 255, 255, 0)); im.putalpha(a)
    save(im, 'foci/' + name)

def ward_icon(d, S):
    k = S / 32
    W = int(2.4 * k)
    shield = [(16, 3), (26, 7), (25, 17), (16, 29), (7, 17), (6, 7)]
    pts = [(x * k, y * k) for x, y in shield]
    d.line(pts + [pts[0], pts[1]], fill=255, width=W, joint='curve')
    # inner rune: ward sigil (vertical bar + two arms + ring)
    d.line([(16 * k, 9 * k), (16 * k, 24 * k)], fill=255, width=int(2.2 * k))
    d.line([(16 * k, 16 * k), (11 * k, 10 * k)], fill=255, width=int(2 * k))
    d.line([(16 * k, 16 * k), (21 * k, 10 * k)], fill=255, width=int(2 * k))
    d.line([(10 * k, 5.5 * k), (16 * k, 3 * k), (22 * k, 5.5 * k)], fill=0, width=int(0.8 * k))

def primal_icon(d, S):
    k = S / 32
    c = 16 * k
    for i in range(6):
        a = math.radians(i * 60 - 90)
        a2 = a + math.radians(30)
        tip = (c + math.cos(a) * 14 * k, c + math.sin(a) * 14 * k)
        # curved swirl arm
        pts = []
        for t in range(0, 11):
            tt = t / 10
            r = 4 * k + tt * 10 * k
            ang = a - (1 - tt) * 0.9
            pts.append((c + math.cos(ang) * r, c + math.sin(ang) * r))
        d.line(pts, fill=255, width=int((2.6) * k), joint='curve')
        d.ellipse([tip[0] - 1.6 * k, tip[1] - 1.6 * k, tip[0] + 1.6 * k, tip[1] + 1.6 * k], fill=255)
    d.ellipse([c - 3.6 * k, c - 3.6 * k, c + 3.6 * k, c + 3.6 * k], outline=255, width=int(1.8 * k))
    d.ellipse([c - 1.2 * k, c - 1.2 * k, c + 1.2 * k, c + 1.2 * k], fill=255)

def ward_overlay():
    im = Image.new('RGBA', (32, 32), (0, 0, 0, 0)); px = im.load()
    rnd = random.Random(3)
    for y in range(32):
        for x in range(32):
            e = min(x, y, 31 - x, 31 - y)
            if e == 0: a = 150
            elif e == 1: a = 80
            elif e == 2: a = 30
            else: a = 0
            if a: px[x, y] = (255, 255, 255, a)
    # small rune marks along the frame, TC4 ward style
    runes = [["#.#", ".#.", "#.#"], ["##.", ".##", ".#."], [".#.", "###", ".#."], ["#..", "###", "..#"]]
    spots = [(5, 4), (14, 4), (24, 4), (4, 14), (25, 14), (5, 25), (14, 25), (24, 25)]
    for n, (sx, sy) in enumerate(spots):
        r = runes[n % 4]
        for j, row in enumerate(r):
            for i, c in enumerate(row):
                if c == '#': px[sx + i, sy + j] = (255, 255, 255, 120)
    # central faint sigil
    for (x, y) in [(15, 12), (16, 12), (14, 13), (17, 13), (14, 14), (17, 14), (15, 15), (16, 15), (15, 16), (16, 16), (15, 17), (16, 17), (15, 18), (16, 18), (13, 17), (18, 17)]:
        px[x, y] = (255, 255, 255, 70)
    save(im, 'misc/ward_overlay')

# ---------------------------------------------------------------- autocaster (arcane bore recoloured eldritch)
def autocaster():
    im = load('entity/arcanebore'); px = im.load()
    voidr = [hx('050208'), hx('0d0617'), hx('200d36'), hx('30174f'), hx('4f2a7c'), hx('7450b8'), hx('a07ee0')]
    stone = [hx('141212'), hx('201b1b'), hx('3a3133'), hx('463b3d'), hx('514547'), hx('5a4d4e'), hx('6e6062')]
    lens = [hx('300a40'), hx('7a2aa0'), hx('c060ff'), hx('f0c8ff')]
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if not a: continue
            lum = (0.3 * r + 0.59 * g + 0.11 * b) / 255
            if g > r + 20 and g > b + 20:  # green lens/gem
                px[x, y] = lens[min(3, int(lum * 4.5))]
            elif r > b + 40 and (r - g) > 15 and r > 90:  # brass / gold
                px[x, y] = voidr[min(6, int(lum * 7.5))]
            elif b > r + 5 and lum > 0.35:  # bluish metal (pale plates)
                px[x, y] = stone[min(6, 2 + int(lum * 5))]
            else:  # wood & dark parts -> eldritch stone
                px[x, y] = stone[min(6, int(lum * 16))]
    save(im, 'entity/autocaster_eldritch')

ALL = [harness_item, harness_armor, girdle, trunk_item, seals, mana, decon_top, decon_gui, outer, eye, tablet,
       lambda: soft_icon(ward_icon, 'ward'), lambda: soft_icon(primal_icon, 'primal'), ward_overlay, autocaster]
if __name__ == '__main__':
    for f in ALL: f()
