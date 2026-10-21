#!/usr/bin/env python3
"""生成 Shared/Content/tarot-art.json：78 张塔罗牌面的星空线稿（两端共用）。

用法：python3 tools/tarot-art/generate.py [输出路径]
预览：python3 tools/tarot-art/preview.py <输出目录>（把 78 张牌渲染成 SVG 拼图）

牌面坐标是 100 × 160（y 向下），图案主要画在 x 10…90、y 24…124 之间，
上方留给牌号、下方留给牌名。图元和字段见 DOC。
"""
import json
import math
import os
import random
import sys

DOC = (
    "塔罗牌面线稿。坐标 100x160，y 向下，角度单位是度（0 指向右，顺时针）。"
    "t 图元：circle 实心圆 / ring 空心圆 / star n 角星（k 内径比，rot 起始角）/ crescent 月牙"
    "（a 亮面朝向，k 最厚处占直径的比例）/ line、polyline（p 扁平坐标）/ polygon（闭合）/ arc（a0→a1）"
    "/ suit 花色符号（s 花色，x、y 中心，r 半高，rot 旋转）/ glyph 单个字（g，r 字号）。"
    "c 语义色 accent|accent2|gold|ink|star|soft，o 不透明度，w 线宽，f 填充不透明度（乘在 o 上）。"
    "缺省：circle、star、crescent 只填充；ring、polygon、line、polyline、arc 只描边，w 缺省 0.8。"
    "suits 是四个花色符号的模板（-1…1 单位坐标），模板图元没写 c 时用 suit 图元的颜色，"
    "线宽按牌面单位画、乘以 suit 图元的 w（缺省 1）。"
)

W = 0.8  # 缺省线宽
# 坐标保留的小数位：牌面坐标 1 位就够，半径、角度等 2 位，花色模板（-1…1）3 位
PREC = {"p": 1, "f": 2}


def rnd(v, digits=None):
    r = round(v, PREC["p"] if digits is None else digits)
    return int(r) if r == int(r) else r


def shape(t, c=None, o=None, w=None, f=None, **kw):
    s = {"t": t}
    if c is not None:
        s["c"] = c
    for key, value in kw.items():
        if value is None:
            continue
        if key == "p":
            s["p"] = [rnd(v) for v in value]
        elif isinstance(value, float):
            s[key] = rnd(value, PREC["f"])
        else:
            s[key] = value
    if o is not None and o != 1:
        s["o"] = rnd2(o)
    if w is not None:
        s["w"] = rnd2(w)
    if f is not None:
        s["f"] = rnd2(f)
    return s


def rnd2(v):
    return rnd(v, 2)


def flat(points):
    out = []
    for x, y in points:
        out += [x, y]
    return out


# ---------- 基本图元 ----------

def circle(x, y, r, c="ink", o=1, f=None, w=None):
    return shape("circle", c, o, w, f, x=x, y=y, r=r)


def ring(x, y, r, c="ink", o=1, w=W, f=None):
    return shape("ring", c, o, w, f, x=x, y=y, r=r)


def star(x, y, r, n=5, k=None, c="star", o=1, rot=None, f=None, w=None):
    return shape("star", c, o, w, f, x=x, y=y, r=r, n=n, k=k, rot=rot)


def crescent(x, y, r, a, k=0.4, c="gold", o=1, f=None, w=None):
    return shape("crescent", c, o, w, f, x=x, y=y, r=r, a=a, k=k)


def line(x1, y1, x2, y2, c="ink", o=1, w=W):
    return shape("line", c, o, w, None, p=[x1, y1, x2, y2])


def pl(points, c="ink", o=1, w=W):
    return shape("polyline", c, o, w, None, p=flat(points))


def pg(points, c="ink", o=1, w=W, f=None):
    return shape("polygon", c, o, w, f, p=flat(points))


def arc(x, y, r, a0, a1, c="ink", o=1, w=W):
    return shape("arc", c, o, w, None, x=x, y=y, r=r, a0=a0, a1=a1)


def suit(s, x, y, r, c=None, o=1, rot=None, w=None):
    return shape("suit", c, o, w, None, s=s, x=x, y=y, r=r, rot=rot)


def glyph(g, x, y, r, c="ink", o=1):
    return shape("glyph", c, o, None, None, g=g, x=x, y=y, r=r)


# ---------- 组合图案（展开成基本图元） ----------

def head(x, y, r=4.0, c="ink"):
    return [circle(x, y, r, "soft", 0.35), ring(x, y, r, c, 1, 0.8)]


def robe(points, c="ink", fill="soft", fo=0.3, w=0.8):
    return [pg(points, fill, fo, 0, 1), pg(points, c, 1, w)]


def fill_only(points, c="soft", o=0.3):
    return pg(points, c, o, 0, 1)


def node(x, y, r=0.7, c="star", o=0.9):
    return circle(x, y, r, c, o)


def sun_rays(x, y, r0, r1, count, c="gold", o=0.7, w=0.6, start=0):
    out = []
    for i in range(count):
        a = math.radians(start + 360 / count * i)
        out.append(line(x + r0 * math.cos(a), y + r0 * math.sin(a), x + r1 * math.cos(a), y + r1 * math.sin(a), c, o, w))
    return out


def sun(x, y, r, rays=12, c="gold", ray_len=None, face=False):
    out = [circle(x, y, r, c, 0.22), ring(x, y, r, c, 1, 0.9)]
    out += sun_rays(x, y, r + 1.6, r + (ray_len or r * 0.7) + 1.6, rays, c, 0.75, 0.6, -90)
    if face:
        out += [circle(x - r * 0.32, y - r * 0.12, r * 0.07, c, 0.9), circle(x + r * 0.32, y - r * 0.12, r * 0.07, c, 0.9),
                arc(x, y + r * 0.05, r * 0.38, 30, 150, c, 0.8, 0.5)]
    return out


def lemniscate(x, y, a, c="gold", o=1, w=0.9, steps=48):
    pts = []
    for i in range(steps + 1):
        t = 2 * math.pi * i / steps
        d = 1 + math.sin(t) ** 2
        pts.append((x + a * math.cos(t) / d, y + a * math.sin(t) * math.cos(t) / d))
    return pl(pts, c, o, w)


def wave(x0, x1, y, amp=1.2, periods=3, c="ink", o=0.5, w=0.6, steps=None):
    steps = steps or periods * 10
    pts = [(x0 + (x1 - x0) * i / steps, y + amp * math.sin(2 * math.pi * periods * i / steps)) for i in range(steps + 1)]
    return pl(pts, c, o, w)


def ellipse_points(x, y, rx, ry, steps=48, a0=0, a1=360):
    return [(x + rx * math.cos(math.radians(a0 + (a1 - a0) * i / steps)),
             y + ry * math.sin(math.radians(a0 + (a1 - a0) * i / steps))) for i in range(steps + 1)]


def cloud(x, y, s=1.0, c="ink", o=0.55, w=0.6):
    """一朵小云：几段向上鼓的弧"""
    return [
        arc(x - 4 * s, y, 3 * s, 180, 360, c, o, w),
        arc(x + 0.5 * s, y - 1.5 * s, 3.8 * s, 195, 345, c, o, w),
        arc(x + 5 * s, y, 3 * s, 180, 360, c, o, w),
        line(x - 7 * s, y, x + 8 * s, y, c, o * 0.8, w),
    ]


def crown(x, y, wdt, h, points=3, c="gold", o=1, w=0.7, jewels=True):
    """王冠：底边在 y，尖朝上"""
    pts = [(x - wdt / 2, y)]
    n = points * 2
    for i in range(n + 1):
        px = x - wdt / 2 + wdt * i / n
        py = y - h if i % 2 == 0 else y - h * 0.45
        pts.append((px, py))
    pts.append((x + wdt / 2, y))
    out = [pg(pts, c, o, w, 0.25)]
    if jewels:
        for i in range(0, n + 1, 2):
            px = x - wdt / 2 + wdt * i / n
            out.append(circle(px, y - h - 0.9, 0.7, c, o))
    return out


def flag(x, y, h=16, c="accent", o=1):
    """骑士的小旗：旗杆 + 燕尾旗"""
    return [
        line(x, y, x, y - h, "ink", 0.9, 0.8),
        circle(x, y - h - 0.8, 0.8, "gold"),
        pg([(x, y - h), (x + 11, y - h + 2.2), (x + 7.5, y - h + 4.4), (x + 11, y - h + 6.6), (x, y - h + 7.6)], c, o, 0.6, 0.45),
    ]


def pillar(x, y0, y1, wdt=8, c="ink", dark=False, label=None):
    out = []
    body = [(x - wdt / 2, y0 + 4), (x + wdt / 2, y0 + 4), (x + wdt / 2, y1 - 3), (x - wdt / 2, y1 - 3)]
    if dark:
        out.append(fill_only(body, "soft", 0.55))
    out.append(pg(body, c, 0.85, 0.7))
    out.append(pg([(x - wdt / 2 - 1.5, y0), (x + wdt / 2 + 1.5, y0), (x + wdt / 2 + 1.5, y0 + 4), (x - wdt / 2 - 1.5, y0 + 4)], c, 0.85, 0.7))
    out.append(pg([(x - wdt / 2 - 1.5, y1 - 3), (x + wdt / 2 + 1.5, y1 - 3), (x + wdt / 2 + 1.5, y1), (x - wdt / 2 - 1.5, y1)], c, 0.85, 0.7))
    if label:
        out.append(glyph(label, x, y0 + 16, 6, "gold", 0.9))
    return out


def tower(x0, x1, y0, y1, c="ink", o=0.8, windows=1):
    """小塔楼：带垛口"""
    wdt = x1 - x0
    m = wdt / 5
    top = [(x0, y1), (x0, y0), (x0 + m, y0), (x0 + m, y0 + 1.6), (x0 + 2 * m, y0 + 1.6), (x0 + 2 * m, y0),
           (x0 + 3 * m, y0), (x0 + 3 * m, y0 + 1.6), (x0 + 4 * m, y0 + 1.6), (x0 + 4 * m, y0), (x1, y0), (x1, y1)]
    out = [fill_only(top, "soft", 0.3), pl(top, c, o, 0.7)]
    for i in range(windows):
        wy = y0 + 6 + i * 7
        out.append(pg([(x0 + wdt / 2 - 0.9, wy), (x0 + wdt / 2 + 0.9, wy), (x0 + wdt / 2 + 0.9, wy + 3), (x0 + wdt / 2 - 0.9, wy + 3)], "gold", 0.8, 0.4, 0.6))
    return out


def petals(x, y, r, count=5, c="accent2", o=0.9, w=0.5):
    out = []
    for i in range(count):
        a = math.radians(-90 + 360 / count * i)
        out.append(ring(x + r * 0.62 * math.cos(a), y + r * 0.62 * math.sin(a), r * 0.48, c, o, w))
    out.append(circle(x, y, r * 0.3, "gold", o))
    return out


def flower(x, y, r=2.2, c="accent2"):
    return [star(x, y, r, 5, 0.55, c, 0.85), circle(x, y, r * 0.3, "gold")]


def sunflower(x, y, r=4):
    return [star(x, y, r, 12, 0.62, "gold", 0.9, f=0, w=0.5), circle(x, y, r * 0.42, "gold", 0.55), ring(x, y, r * 0.42, "gold", 0.9, 0.5)]


def wing(points, c="accent"):
    return [pg(points, c, 0.9, 0.7, 0.14)]


def bezier(p0, p1, p2, steps=12):
    return [((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0],
             (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]) for t in (i / steps for i in range(steps + 1))]


def feather_wing(sx, sy, d, span=30, lift=12, drop=12, feathers=5, c="accent"):
    """天使的羽翼：上缘一道弧，下缘是一排圆润的羽尖，里面几道羽毛线。d = -1 向左展开，1 向右"""
    tip = (sx + d * span, sy - lift)
    top = bezier((sx, sy), (sx + d * span * 0.3, sy - lift * 1.35), tip, 14)
    base = bezier(tip, (sx + d * span * 0.55, sy + drop * 0.7), (sx + d * 1.5, sy + drop), feathers * 4)
    lower = []
    for i, (x, y) in enumerate(base):
        phase = (i % 4) / 4
        bulge = 2.4 * math.sin(math.pi * phase)
        # 羽尖向外下方鼓出
        lower.append((x + d * bulge * 0.35, y + bulge))
    outline = top + lower[1:]
    out = [pg(outline, c, 0.9, 0.7, 0.14)]
    for i in range(4, len(base) - 1, 4):
        x, y = base[i]
        out.append(line(sx + d * 2, sy + 2, x, y, c, 0.45, 0.4))
    return out


def mirror(points, cx=50):
    return [(2 * cx - x, y) for x, y in points]


def wheat(x, y, h=12, c="gold", o=0.85):
    out = [line(x, y, x, y - h, c, o, 0.5)]
    for i in range(4):
        yy = y - h + 1.2 + i * 2.1
        out.append(line(x, yy + 1.4, x - 1.6, yy - 0.4, c, o, 0.5))
        out.append(line(x, yy + 1.4, x + 1.6, yy - 0.4, c, o, 0.5))
    return out


def cypress(x, y, h, wdt=5, c="soft", o=0.55):
    pts = [(x, y - h), (x + wdt * 0.5, y - h * 0.55), (x + wdt * 0.45, y - h * 0.15), (x, y), (x - wdt * 0.45, y - h * 0.15), (x - wdt * 0.5, y - h * 0.55)]
    return [pg(pts, c, o, 0.6, 0.35)]


def mountains(points, c="soft", o=0.75, w=0.7, fill=0.18):
    out = []
    if fill:
        base = max(p[1] for p in points)
        out.append(fill_only(points + [(points[-1][0], base), (points[0][0], base)], c, fill))
    out.append(pl(points, c, o, w))
    return out


def zodiac_medallion(g, x, y):
    return [circle(x, y, 5.6, "soft", 0.3), ring(x, y, 5.6, "gold", 0.7, 0.5), glyph(g, x, y + 0.2, 6.2, "ink", 0.95)]


def pentagram(x, y, r, rot=-90, c="gold", o=1, w=0.7):
    pts = []
    for i in range(6):
        a = math.radians(rot + 144 * i)
        pts.append((x + r * math.cos(a), y + r * math.sin(a)))
    return pl(pts, c, o, w)


AVOID_DEFAULT = [(0, 0, 100, 24), (0, 124, 100, 160)]


def sparkles(seed, count, region=(10, 24, 90, 124), avoid=(), big=0.25):
    """散落的星点：小四角星 + 圆点。avoid 是不放星点的矩形"""
    rng = random.Random(seed)
    out = []
    tries = 0
    placed = []
    while len(placed) < count and tries < 2000:
        tries += 1
        x = rng.uniform(region[0], region[2])
        y = rng.uniform(region[1], region[3])
        if any(a[0] <= x <= a[2] and a[1] <= y <= a[3] for a in avoid):
            continue
        if any((x - px) ** 2 + (y - py) ** 2 < 36 for px, py in placed):
            continue
        placed.append((x, y))
        if rng.random() < big:
            out.append(star(x, y, rng.uniform(1.4, 2.3), 4, 0.28, "star", rng.uniform(0.6, 0.95)))
        else:
            out.append(circle(x, y, rng.uniform(0.35, 0.7), "star", rng.uniform(0.45, 0.9)))
    return out


# ---------- 花色符号模板（单位坐标 -1…1） ----------

def suit_templates():
    saved = dict(PREC)
    PREC.update(p=3, f=3)
    try:
        return _suit_templates()
    finally:
        PREC.update(saved)


def _suit_templates():
    cups_bowl = [(-0.62, -0.8)] + [(0.62 * math.cos(math.radians(a)), -0.8 + 0.74 * math.sin(math.radians(a))) for a in range(0, 181, 15)]
    cups = [
        pg(cups_bowl, None, 1, 0.8, 0.28),
        line(-0.5, -0.62, 0.5, -0.62, None, 0.45, 0.4),
        line(0, -0.06, 0, 0.62, None, 1, 0.9),
        circle(0, 0.26, 0.08, None, 1),
        pg([(-0.44, 0.9), (0.44, 0.9), (0.14, 0.64), (-0.14, 0.64)], None, 1, 0.8, 0.28),
    ]

    wands = [
        line(0, -0.92, 0, 0.92, None, 1, 1.0),
        circle(0, 0.94, 0.09, None, 1),
        circle(0, -0.94, 0.07, None, 1),
    ]
    for (y, side) in [(-0.5, 1), (-0.12, -1), (0.3, 1)]:
        leaf = [(0, y), (0.14 * side, y - 0.17), (0.36 * side, y - 0.22), (0.2 * side, y - 0.04)]
        wands.append(pg(leaf, None, 1, 0.5, 0.45))
    wands.append(pg([(0, -0.82), (-0.16, -1.0), (0, -0.92), (0.16, -1.0)], None, 1, 0.5, 0.45))

    swords = [
        pg([(0, -1.0), (0.11, -0.8), (0.11, 0.42), (-0.11, 0.42), (-0.11, -0.8)], None, 1, 0.8, 0.22),
        line(0, -0.76, 0, 0.36, None, 0.5, 0.4),
        line(-0.46, 0.46, 0.46, 0.46, None, 1, 1.0),
        circle(-0.48, 0.46, 0.055, None, 1),
        circle(0.48, 0.46, 0.055, None, 1),
        line(0, 0.5, 0, 0.82, None, 1, 1.2),
        ring(0, 0.9, 0.08, None, 1, 0.7),
    ]

    pent_pts = []
    for i in range(6):
        a = math.radians(-90 + 144 * i)
        pent_pts.append((0.62 * math.cos(a), 0.62 * math.sin(a)))
    pentacles = [
        circle(0, 0, 0.86, None, 0.18),
        ring(0, 0, 0.86, None, 1, 0.9),
        ring(0, 0, 0.74, None, 0.55, 0.4),
        pl(pent_pts, None, 1, 0.75),
    ]
    return {"cups": cups, "wands": wands, "swords": swords, "pentacles": pentacles}


SUIT_COLOR = {"cups": "accent", "wands": "accent2", "swords": "ink", "pentacles": "gold"}


# ---------- 大阿卡纳：每张一个专属构图（参照韦特塔罗 1909 原画的标志性元素） ----------

def fool():
    """0 愚者：悬崖边迈步的旅人、肩上的行囊、手里的白玫瑰、脚边跳起的小狗、右上角的太阳"""
    s = []
    s += sun(76, 36, 6.5, 12)
    s += mountains([(10, 92), (20, 76), (27, 84), (38, 66), (47, 80), (54, 74), (60, 84)], fill=0.12)
    cliff = [(10, 106), (30, 103), (50, 101), (62, 100), (66, 103), (64, 111), (68, 117), (66, 124)]
    s.append(fill_only(cliff + [(10, 124)], "soft", 0.32))
    s.append(pl(cliff, "ink", 0.9, 0.8))
    # 旅人：向右迈步，抬头望天
    s += robe([(46, 63), (54, 63), (59, 84), (41, 85)])
    s += head(51, 56, 4.2)
    s.append(pl([(48.5, 52.4), (53, 50.4), (57, 51.8)], "accent2", 0.9, 0.6))  # 帽上的羽毛
    s.append(pl([(47, 85), (44, 93), (42, 100)], "ink", 1, 0.8))
    s.append(pl([(53, 85), (58, 92), (62, 99)], "ink", 1, 0.8))
    # 肩上的手杖和行囊（向后上方）
    s.append(pl([(54, 65), (58, 70)], "ink", 1, 0.8))
    s.append(line(62, 74, 28, 44, "gold", 0.95, 0.8))
    s.append(circle(27, 43, 3.6, "accent", 0.25))
    s.append(ring(27, 43, 3.6, "accent", 0.95, 0.7))
    s.append(line(25, 40, 29.5, 46, "accent", 0.6, 0.4))
    # 另一只手拈着白玫瑰
    s.append(pl([(46, 66), (40, 72), (36, 72)], "ink", 1, 0.8))
    s += flower(34.5, 70.5, 2.3, "star")
    # 小狗：在身后跳起
    dog = [(22, 98), (25, 94.5), (31, 93.5), (34, 91), (36.5, 89.5), (37, 92), (35, 94), (33, 97)]
    s.append(pl(dog, "ink", 0.95, 0.7))
    s.append(pl([(36.4, 89.6), (35.6, 87.2), (34.6, 89.6)], "ink", 0.95, 0.6))  # 耳朵
    s.append(pl([(25, 94.5), (22.5, 90), (21, 89)], "ink", 0.9, 0.6))  # 尾巴
    s.append(pl([(25.5, 95.5), (23, 100)], "ink", 0.9, 0.6))
    s.append(pl([(31, 95.6), (33, 99.5)], "ink", 0.9, 0.6))
    s.append(node(35.2, 91.2, 0.45, "ink"))
    s += sparkles(0, 9, avoid=[(66, 26, 90, 48), (20, 40, 64, 124), (10, 64, 66, 124)])
    return s


def magician():
    """1 魔术师：头顶的无限符号、一手举杖指天一手指地、桌上的杯剑杖币四元素"""
    s = []
    s.append(lemniscate(50, 31, 8.5, "gold", 1, 0.9))
    s += robe([(44, 52), (56, 52), (61, 88), (39, 88)])
    s += head(50, 45.5, 4.2)
    s.append(line(44.5, 70, 55.5, 70, "accent2", 0.8, 0.6))  # 腰带
    # 举起的手与双头魔杖
    s.append(pl([(55, 54), (60, 47), (62, 40)], "ink", 1, 0.8))
    s.append(line(61, 44, 63.6, 28, "gold", 1, 1.1))
    s.append(circle(63.6, 27.6, 1.1, "gold"))
    s.append(circle(61, 44.4, 1.1, "gold"))
    # 指向地面的手
    s.append(pl([(45, 54), (39, 63), (36, 70), (35, 74)], "ink", 1, 0.8))
    # 桌子与四元素
    s.append(pg([(18, 90), (82, 90), (82, 94), (18, 94)], "ink", 0.9, 0.7, 0.25))
    s.append(line(22, 94, 22, 116, "ink", 0.8, 0.7))
    s.append(line(78, 94, 78, 116, "ink", 0.8, 0.7))
    s.append(suit("cups", 27, 83.5, 5.6, "accent"))
    s.append(suit("pentacles", 41, 84.5, 4.8, "gold"))
    s.append(suit("swords", 59, 83, 6.4, "ink", rot=0))
    s.append(suit("wands", 73, 83, 6.4, "accent2", rot=18))
    # 桌下的玫瑰与百合
    for x in (30, 42, 58, 70):
        s += flower(x, 108, 2.2, "accent2")
        s.append(line(x, 110, x, 118, "soft", 0.7, 0.5))
    for x in (36, 50, 64):
        s.append(star(x, 112, 2.0, 6, 0.4, "star", 0.75))
        s.append(line(x, 114, x, 120, "soft", 0.7, 0.5))
    s += sparkles(1, 7, avoid=[(14, 24, 86, 124)], region=(10, 24, 90, 124))
    s += sparkles(101, 4, region=(12, 36, 88, 80), avoid=[(30, 24, 70, 92)])
    return s


def high_priestess():
    """2 女祭司：黑白双柱 B 与 J、三相月冠、胸前十字、手中经卷、脚下新月"""
    s = []
    s += pillar(21, 30, 118, 8, "ink", dark=True, label="B")
    s += pillar(79, 30, 118, 8, "ink", dark=False, label="J")
    # 帷幔上的石榴
    for (x, y) in [(33, 40), (41, 34), (59, 34), (67, 40), (33, 54), (67, 54)]:
        s.append(circle(x, y, 1.6, "accent2", 0.45))
        s.append(ring(x, y, 1.6, "accent2", 0.8, 0.4))
    s += robe([(44, 54), (56, 54), (66, 108), (34, 108)])
    s += head(50, 47.5, 4.2)
    # 三相月冠：满月夹在两弯新月之间
    s.append(circle(50, 38.6, 2.4, "gold", 0.9))
    s.append(crescent(45.2, 38.6, 2.8, 180, 0.32, "gold"))
    s.append(crescent(54.8, 38.6, 2.8, 0, 0.32, "gold"))
    # 胸前的等臂十字
    s.append(line(50, 58, 50, 66, "gold", 1, 0.7))
    s.append(line(46, 62, 54, 62, "gold", 1, 0.7))
    # 经卷
    s.append(pg([(43, 74), (53, 72), (54, 77), (44, 79)], "ink", 0.9, 0.6, 0.35))
    s.append(line(46, 75.6, 51.5, 74.4, "gold", 0.7, 0.4))
    # 脚下的新月
    s.append(crescent(50, 109, 9.5, 90, 0.3, "gold", 0.95))
    s += sparkles(2, 6, region=(28, 62, 72, 100), avoid=[(32, 50, 68, 112)])
    return s


def empress():
    """3 皇后：十二星冠、权杖、金星盾、麦田、柏树与瀑布"""
    s = []
    s += cypress(16, 92, 40, 6)
    s += cypress(24, 88, 30, 5)
    # 瀑布
    for i, x in enumerate((80, 83, 86)):
        s.append(pl([(x, 40 + i), (x - 0.8, 50), (x + 0.6, 60), (x - 0.6, 70), (x + 0.4, 80)], "accent", 0.6, 0.5))
    s += robe([(44, 54), (56, 54), (70, 104), (30, 104)])
    s += head(50, 47.5, 4.2)
    # 十二星冠
    for i in range(12):
        a = math.radians(195 + 150 / 11 * i)
        s.append(star(50 + 10.5 * math.cos(a), 46 + 10.5 * math.sin(a), 1.35, 6, 0.42, "gold"))
    # 权杖
    s.append(pl([(56, 58), (61, 62), (62, 60)], "ink", 1, 0.8))
    s.append(line(62, 42, 62, 76, "gold", 1, 0.9))
    s.append(ring(62, 39.6, 2.3, "gold", 1, 0.7))
    s.append(line(62, 35.6, 62, 37.3, "gold", 1, 0.6))
    # 金星盾
    shield = [(28, 82), (40, 82), (40, 92), (34, 99), (28, 92)]
    s.append(pg(shield, "accent2", 0.9, 0.7, 0.2))
    s.append(ring(34, 87, 2.6, "accent2", 1, 0.7))
    s.append(line(34, 89.6, 34, 95, "accent2", 1, 0.7))
    s.append(line(31.6, 92.6, 36.4, 92.6, "accent2", 1, 0.7))
    # 麦穗
    for i, x in enumerate((44, 52, 60, 68, 76, 84)):
        s += wheat(x, 122, 11 + (i % 2) * 2.5)
    s += sparkles(3, 6, avoid=[(10, 30, 90, 124)], region=(10, 24, 90, 60))
    return s


def emperor():
    """4 皇帝：石座上的羊头、安卡权杖、宝球、身后的群山"""
    s = []
    s += mountains([(10, 82), (16, 60), (22, 70), (28, 56)], fill=0.14)
    s += mountains([(72, 56), (78, 66), (84, 52), (90, 78)], fill=0.14)
    throne = [(28, 42), (72, 42), (72, 116), (28, 116)]
    s.append(fill_only(throne, "soft", 0.2))
    s.append(pg(throne, "ink", 0.75, 0.7))
    # 羊头螺角
    for cx, d in ((32.5, 1), (67.5, -1)):
        s.append(arc(cx, 47, 3.2, 180 if d == 1 else 0, 450 if d == 1 else -270, "gold", 0.95, 0.6))
        s.append(arc(cx + 0.6 * d, 47.4, 1.6, 180 if d == 1 else 0, 400 if d == 1 else -220, "gold", 0.95, 0.5))
    # 君王
    s += robe([(42, 60), (58, 60), (63, 106), (37, 106)])
    s += head(50, 53, 4.4)
    s += crown(50, 49.4, 9, 4.2, 2)
    s.append(pg([(47, 56.5), (53, 56.5), (50, 62)], "ink", 0.8, 0.6, 0.4))  # 胡须
    s.append(line(37, 84, 63, 84, "ink", 0.5, 0.5))  # 膝
    # 安卡权杖
    s.append(pl([(43, 62), (38, 70), (37, 72)], "ink", 1, 0.8))
    s.append(line(36, 60, 36, 86, "gold", 1, 0.9))
    s.append(ring(36, 56.8, 2.4, "gold", 1, 0.8))
    s.append(line(33, 60, 39, 60, "gold", 1, 0.8))
    # 宝球
    s.append(pl([(57, 62), (61, 70), (63, 73)], "ink", 1, 0.8))
    s.append(circle(64, 74.5, 2.6, "gold", 0.3))
    s.append(ring(64, 74.5, 2.6, "gold", 1, 0.7))
    s.append(line(64, 70.4, 64, 71.9, "gold", 1, 0.6))
    s += [line(24, 116, 76, 116, "ink", 0.6, 0.6), line(20, 120, 80, 120, "ink", 0.45, 0.6)]
    s += sparkles(4, 5, avoid=[(26, 24, 74, 124)], region=(10, 24, 90, 50))
    return s


def hierophant():
    """5 教皇：三重冠、祝福的手、三重十字杖、双柱、脚下交叉的钥匙与两位侍僧"""
    s = []
    s += pillar(18, 34, 108, 7, "ink")
    s += pillar(82, 34, 108, 7, "ink")
    s += robe([(43, 54), (57, 54), (67, 104), (33, 104)])
    s.append(line(50, 56, 50, 103, "gold", 0.6, 0.5))
    s += head(50, 47.5, 4.2)
    tiara = [(45.5, 43.6), (46, 37), (47.5, 32.5), (50, 30), (52.5, 32.5), (54, 37), (54.5, 43.6)]
    s.append(pg(tiara, "gold", 1, 0.7, 0.25))
    for y, half in ((41.5, 4.3), (38, 3.9), (34.5, 3.0)):
        s.append(line(50 - half, y, 50 + half, y, "gold", 0.9, 0.5))
    s.append(line(50, 30, 50, 26.5, "gold", 1, 0.6))
    s.append(line(48.6, 27.8, 51.4, 27.8, "gold", 1, 0.6))
    # 祝福的手
    s.append(pl([(44, 56), (38, 52), (37, 46)], "ink", 1, 0.8))
    s.append(pl([(36.4, 46.4), (36, 43.6)], "ink", 1, 0.6))
    s.append(pl([(37.6, 46), (37.8, 43.4)], "ink", 1, 0.6))
    # 三重十字杖
    s.append(pl([(56, 57), (61, 64), (62, 66)], "ink", 1, 0.8))
    s.append(line(62.5, 32, 62.5, 96, "gold", 1, 0.9))
    for y, half in ((35, 2.2), (39, 3.2), (43, 4.2)):
        s.append(line(62.5 - half, y, 62.5 + half, y, "gold", 1, 0.8))
    # 交叉的钥匙
    for d in (1, -1):
        x0, x1 = 50 - 9 * d, 50 + 9 * d
        s.append(line(x0, 104, x1, 120, "gold", 0.95, 0.8))
        s.append(ring(x0 - 1.4 * d, 102.2, 2.0, "gold", 0.95, 0.7))
        s.append(pl([(x1 - 1.2 * d, 117.8), (x1 + 1.2 * d, 116.4), (x1 + 2.2 * d, 118.2)], "gold", 0.95, 0.6))
    # 两位侍僧（背影）
    for x in (30, 70):
        s.append(ring(x, 110, 3.0, "ink", 0.85, 0.7))
        s.append(arc(x, 121, 7, 200, 340, "ink", 0.85, 0.7))
    s += sparkles(5, 5, avoid=[(12, 24, 88, 124)], region=(10, 24, 90, 124))
    s += sparkles(105, 4, region=(24, 30, 76, 60), avoid=[(36, 24, 68, 60)])
    return s


def lovers():
    """6 恋人：天使展翼于太阳前、下方一男一女、中间的山峰、智慧树与蛇、火焰之树"""
    s = []
    s += sun(50, 34, 7.5, 16, ray_len=5)
    s += feather_wing(46.5, 48, -1, 30, 10, 9, 5)
    s += feather_wing(53.5, 48, 1, 30, 10, 9, 5)
    s.append(circle(50, 45.5, 3.2, "soft", 0.5))
    s.append(ring(50, 45.5, 3.2, "accent", 1, 0.7))
    s.append(pg([(46.5, 49), (53.5, 49), (55, 58), (45, 58)], "accent", 0.9, 0.7, 0.2))
    s += cloud(42, 62, 1.0)
    s += cloud(58, 62, 1.0)
    s += mountains([(38, 112), (50, 76), (62, 112)], "soft", 0.7, 0.7, 0.2)
    # 火焰之树（男子身后）
    s.append(line(14, 118, 14, 80, "ink", 0.7, 0.7))
    for i in range(5):
        y = 80 + i * 6
        s.append(pg([(14, y + 3), (10.5, y + 1.2), (12.4, y - 2.2)], "gold", 0.8, 0.5, 0.4))
        s.append(pg([(14, y + 3), (17.5, y + 1.2), (15.6, y - 2.2)], "gold", 0.8, 0.5, 0.4))
    # 智慧树与蛇（女子身后）
    s.append(line(86, 118, 86, 82, "ink", 0.7, 0.7))
    for (x, y) in [(86, 78), (82, 82), (90, 82)]:
        s.append(ring(x, y, 3.2, "soft", 0.9, 0.6))
    for (x, y) in [(84, 80), (88.5, 83.5), (86, 76)]:
        s.append(circle(x, y, 0.9, "accent2", 0.9))
    s.append(pl([(86, 116), (83, 112), (89, 106), (83, 100), (89, 94), (85, 89)], "accent2", 0.95, 0.6))
    # 男子与女子
    for x, d in ((28, 1), (72, -1)):
        s += head(x, 80, 3.6)
        s += robe([(x - 3.2, 85), (x + 3.2, 85), (x + 2.2, 93), (x + 2.8, 101), (x - 2.8, 101), (x - 2.2, 93)], fo=0.2)
        s += [pl([(x - 1.4, 101), (x - 1.8, 116)]), pl([(x + 1.4, 101), (x + 1.8, 116)])]
        # 一只手伸向对方，一只手垂下
        s += [pl([(x + 3.2 * d, 86.5), (x + 7 * d, 91), (x + 9 * d, 89)]), pl([(x - 3.2 * d, 86.5), (x - 5 * d, 95)])]
    s.append(pl([(69.2, 77.6), (68, 84), (68.6, 88)], "ink", 0.8, 0.6))  # 长发
    s.append(pl([(74.8, 77.6), (76, 84), (75.4, 88)], "ink", 0.8, 0.6))
    s += sparkles(6, 4, avoid=[(12, 24, 88, 124)])
    return s


def chariot():
    """7 战车：星辰华盖、头戴八芒星冠的御者、车前一黑一白两只斯芬克斯、背后的城墙"""
    s = []
    # 城墙
    for x0, x1 in ((10, 26), (74, 90)):
        pts = [(x0, 74)]
        x = x0
        while x < x1:
            pts += [(x, 70), (x + 2, 70), (x + 2, 74), (x + 4, 74)]
            x += 4
        s.append(pl(pts, "soft", 0.7, 0.5))
    # 华盖与立柱
    s.append(pg([(26, 36), (74, 36), (72, 41), (28, 41)], "accent", 0.9, 0.7, 0.18))
    for i in range(7):
        s.append(star(32 + i * 6, 38.5, 1.25, 6, 0.42, "gold"))
    s.append(line(30, 41, 30, 80, "ink", 0.8, 0.7))
    s.append(line(70, 41, 70, 80, "ink", 0.8, 0.7))
    # 御者
    s += head(50, 52, 4.0)
    s.append(star(50, 45.4, 2.3, 8, 0.45, "gold"))
    s += robe([(43.5, 58), (56.5, 58), (57, 76), (43, 76)])
    s.append(crescent(42.6, 59.5, 2.1, 180, 0.4, "gold"))
    s.append(crescent(57.4, 59.5, 2.1, 0, 0.4, "gold"))
    s.append(pg([(47.5, 63), (52.5, 63), (52.5, 68), (47.5, 68)], "gold", 0.8, 0.5, 0.2))
    s.append(pl([(56, 60), (61, 62), (63, 56)], "ink", 1, 0.8))
    s.append(line(62, 62, 65, 47, "gold", 1, 0.9))
    s.append(circle(65.2, 46, 1.0, "gold"))
    # 车身与带翼日轮
    body = [(31, 76), (69, 76), (69, 92), (31, 92)]
    s.append(fill_only(body, "soft", 0.3))
    s.append(pg(body, "ink", 0.95, 0.8))
    s.append(circle(50, 83.5, 1.9, "gold"))
    s.append(pl([(47.6, 83.5), (43, 81.5), (39.5, 82.5), (43, 84.5), (47.6, 84)], "gold", 0.9, 0.5))
    s.append(pl([(52.4, 83.5), (57, 81.5), (60.5, 82.5), (57, 84.5), (52.4, 84)], "gold", 0.9, 0.5))
    for cx in (27, 73):
        s.append(ring(cx, 93, 5.2, "ink", 0.85, 0.7))
        for a in (0, 45, 90, 135):
            r = math.radians(a)
            s.append(line(cx - 5 * math.cos(r), 93 - 5 * math.sin(r), cx + 5 * math.cos(r), 93 + 5 * math.sin(r), "ink", 0.5, 0.4))
    # 两只斯芬克斯：一黑一白
    for cx, dark in ((38, True), (62, False)):
        nemes = [(cx - 3.4, 104.5), (cx - 2.4, 99.4), (cx + 2.4, 99.4), (cx + 3.4, 104.5)]
        if dark:
            s.append(fill_only(nemes, "soft", 0.7))
        s.append(pg(nemes, "ink", 0.95, 0.6))
        s.append(ring(cx, 101.6, 1.7, "ink", 0.95, 0.5))
        bodyp = [(cx - 4, 104.5), (cx + 4, 104.5), (cx + 6, 114), (cx + 7, 118), (cx - 7, 118), (cx - 6, 114)]
        if dark:
            s.append(fill_only(bodyp, "soft", 0.6))
        s.append(pg(bodyp, "ink", 0.95, 0.6))
        s.append(line(cx - 2.5, 118, cx - 2.5, 113, "ink", 0.7, 0.5))
        s.append(line(cx + 2.5, 118, cx + 2.5, 113, "ink", 0.7, 0.5))
    s += sparkles(7, 5, avoid=[(10, 34, 90, 124)], region=(10, 24, 90, 124))
    return s


def strength():
    """8 力量：头顶的无限符号、花冠女子温柔地合上狮子的嘴"""
    s = []
    s += mountains([(10, 104), (24, 92), (34, 100), (46, 90)], fill=0.12)
    s.append(lemniscate(40, 32.5, 7, "gold", 1, 0.9))
    s += robe([(36.5, 51), (44.5, 51), (48, 80), (51, 112), (29, 112), (33, 80)])
    s += head(40.5, 44.5, 4.0)
    for i in range(5):
        a = math.radians(200 + 35 * i)
        s.append(circle(40.5 + 4.6 * math.cos(a), 44.5 + 4.6 * math.sin(a), 0.75, "accent2", 0.9))
    # 花环：从腰间绕到狮子颈上
    for i in range(7):
        t = i / 6
        x = 44 + 22 * t
        y = 74 + 10 * math.sin(math.pi * t) + 4 * t
        s.append(circle(x, y, 0.85, "accent2", 0.85))
    # 狮子
    s.append(star(66, 72, 11, 16, 0.76, "gold", 0.95, f=0.12, w=0.7))
    s.append(circle(66, 72, 6.6, "soft", 0.35))
    s.append(ring(66, 72, 6.6, "ink", 0.95, 0.7))
    s.append(circle(63.6, 70, 0.6, "ink"))
    s.append(circle(68.4, 70, 0.6, "ink"))
    s.append(pg([(64.8, 73), (67.2, 73), (66, 74.6)], "ink", 0.95, 0.5, 0.6))
    s.append(arc(66, 75.4, 2.6, 20, 160, "ink", 0.9, 0.6))
    lion = [(75, 66), (84, 70), (88, 80), (88, 90)]
    s.append(pl(lion, "ink", 0.9, 0.7))
    s.append(pl([(75, 82), (80, 88), (88, 90)], "ink", 0.9, 0.7))
    s.append(pl([(64, 83), (63, 100), (66, 100)], "ink", 0.9, 0.7))
    s.append(pl([(71, 82.5), (72, 100), (75, 100)], "ink", 0.9, 0.7))
    s.append(pl([(86, 94), (85, 100), (88, 100)], "ink", 0.9, 0.7))
    s.append(pl([(88, 84), (89, 72), (86, 66), (88, 63)], "ink", 0.8, 0.6))
    s.append(circle(88.2, 62.6, 1.0, "gold", 0.9))
    # 女子的双手捧着狮口
    s.append(pl([(45, 54), (54, 62), (60, 66)], "ink", 1, 0.8))
    s.append(pl([(41, 56), (50, 70), (61, 76.5)], "ink", 1, 0.8))
    s += sparkles(8, 6, avoid=[(24, 24, 92, 124)], region=(10, 24, 90, 124))
    s += sparkles(108, 3, region=(52, 26, 90, 54))
    return s


def hermit():
    """9 隐者：雪峰上的斗篷老人、提着装有六芒星的灯笼、手拄长杖"""
    s = []
    s += mountains([(10, 120), (24, 104), (34, 112), (48, 100), (62, 108), (76, 96), (90, 116)], "soft", 0.75, 0.7, 0.2)
    cloak = [(56, 36), (61.5, 42), (64, 56), (67, 104), (42, 104), (46, 72), (47, 52), (50, 40)]
    s.append(fill_only(cloak, "soft", 0.35))
    s.append(pg(cloak, "ink", 1, 0.8))
    s.append(circle(53.4, 47.5, 3.1, "soft", 0.4))
    s.append(arc(53.4, 47.5, 3.1, 100, 260, "ink", 0.9, 0.6))
    s.append(pl([(51, 50.5), (50.2, 56), (52, 60), (53.5, 52)], "ink", 0.8, 0.5))  # 胡须
    # 长杖
    s.append(pl([(62, 62), (66, 64)], "ink", 1, 0.8))
    s.append(line(66, 50, 71, 110, "gold", 0.9, 0.9))
    # 灯笼与六芒星
    s.append(pl([(48, 58), (40, 60), (35, 59)], "ink", 1, 0.8))
    s.append(line(34, 59, 34, 63, "ink", 0.8, 0.5))
    s.append(circle(34, 69, 9, "gold", 0.08))
    s.append(circle(34, 69, 5.5, "gold", 0.12))
    lamp = [(30.5, 63), (37.5, 63), (38.5, 66), (37.5, 75), (30.5, 75), (29.5, 66)]
    s.append(pg(lamp, "gold", 0.95, 0.6, 0.12))
    s.append(star(34, 69.2, 3.2, 6, 0.55, "gold"))
    s += sparkles(9, 9, avoid=[(26, 30, 74, 104), (20, 56, 46, 84)], region=(10, 24, 90, 100))
    return s


def wheel():
    """10 命运之轮：刻着 T A R O 的轮盘、上方的斯芬克斯、左侧下行的蛇、右侧上升的阿努比斯、四角的四活物"""
    s = []
    cx, cy = 50, 76
    s.append(circle(cx, cy, 25, "soft", 0.15))
    s.append(ring(cx, cy, 25, "gold", 1, 1.0))
    s.append(ring(cx, cy, 18.5, "gold", 0.8, 0.6))
    s.append(ring(cx, cy, 6, "gold", 0.9, 0.6))
    s.append(circle(cx, cy, 2.2, "gold", 0.8))
    for i in range(8):
        a = math.radians(i * 45)
        s.append(line(cx + 6 * math.cos(a), cy + 6 * math.sin(a), cx + 18.5 * math.cos(a), cy + 18.5 * math.sin(a), "gold", 0.7, 0.5))
    for g, a in (("T", -90), ("A", 0), ("R", 90), ("O", 180)):
        r = math.radians(a)
        s.append(glyph(g, cx + 21.8 * math.cos(r), cy + 21.8 * math.sin(r), 4.6, "ink", 0.95))
    for a in (-45, 45, 135, 225):
        r = math.radians(a)
        s.append(circle(cx + 21.8 * math.cos(r), cy + 21.8 * math.sin(r), 0.9, "accent2", 0.9))
        s.append(circle(cx + 12.2 * math.cos(r + math.pi / 8), cy + 12.2 * math.sin(r + math.pi / 8), 0.7, "gold", 0.8))
    # 斯芬克斯
    s.append(pg([(46.4, 50.6), (47.4, 45.6), (52.6, 45.6), (53.6, 50.6)], "ink", 0.95, 0.6, 0.35))
    s.append(ring(50, 47.4, 1.7, "ink", 0.95, 0.5))
    s.append(line(55, 50, 58, 39, "ink", 0.9, 0.7))
    s.append(line(56, 43, 59.6, 44.2, "ink", 0.9, 0.6))
    # 下行的蛇
    s.append(pl([(23, 50), (20, 56), (24, 62), (20, 68), (24, 74), (20, 80), (23, 86), (21, 92)], "accent2", 0.95, 0.7))
    s.append(circle(23.4, 49.5, 1.0, "accent2"))
    # 上升的阿努比斯（胡狼头）
    jackal = [(78, 98), (79, 84), (77.5, 78), (78.4, 72), (79, 68.5), (80.2, 72), (81.5, 70), (81.4, 73.6), (84.6, 75.8), (81.6, 77), (81, 84), (82, 98)]
    s.append(pg(jackal, "ink", 0.9, 0.6, 0.3))
    # 四角的四活物：天使、鹰、牛、狮（水瓶、天蝎、金牛、狮子）
    s += zodiac_medallion("♒︎", 17, 32)
    s += zodiac_medallion("♏︎", 83, 32)
    s += zodiac_medallion("♉︎", 17, 116)
    s += zodiac_medallion("♌︎", 83, 116)
    s += sparkles(10, 6, avoid=[(10, 24, 26, 40), (74, 24, 90, 40), (10, 108, 26, 124), (74, 108, 90, 124), (16, 44, 88, 104)])
    return s


def justice():
    """11 正义：双柱间端坐的审判者、右手竖举的剑、左手的天平、方冠"""
    s = []
    s += pillar(17, 32, 116, 7, "ink")
    s += pillar(83, 32, 116, 7, "ink")
    s.append(line(21, 38, 79, 38, "accent", 0.5, 0.6))
    s += robe([(43, 56), (57, 56), (65, 112), (35, 112)])
    s.append(fill_only([(36, 112), (64, 112), (66, 118), (34, 118)], "soft", 0.25))
    s += head(50, 49.5, 4.2)
    s.append(pg([(45.6, 45.8), (54.4, 45.8), (54.4, 41.6), (52.2, 43.4), (50, 40.4), (47.8, 43.4), (45.6, 41.6)], "gold", 1, 0.6, 0.3))
    s.append(circle(50, 43.6, 0.8, "accent2"))
    # 剑
    s.append(suit("swords", 34, 47, 20, "ink", w=1.0))
    s.append(pl([(44, 58), (38, 64), (34, 63.5)], "ink", 1, 0.8))
    # 天平
    s.append(pl([(56, 58), (63, 66), (66, 63)], "ink", 1, 0.8))
    s.append(line(66, 58, 66, 64, "gold", 1, 0.7))
    s.append(line(57, 58, 75, 58, "gold", 1, 0.8))
    for x in (57, 75):
        s.append(line(x, 58, x - 3.4, 67, "gold", 0.8, 0.4))
        s.append(line(x, 58, x + 3.4, 67, "gold", 0.8, 0.4))
        s.append(arc(x, 67, 3.6, 0, 180, "gold", 1, 0.7))
        s.append(line(x - 3.6, 67, x + 3.6, 67, "gold", 0.6, 0.4))
    s.append(circle(66, 57, 0.9, "gold"))
    s += sparkles(11, 4, region=(24, 64, 76, 110), avoid=[(32, 54, 68, 120)])
    return s


def hanged_man():
    """12 倒吊人：活木做的 T 形架、一足倒悬、双腿交成「4」字、头顶的光环"""
    s = []
    s.append(line(24, 31, 76, 31, "ink", 0.95, 1.2))
    s.append(line(28, 31, 28, 120, "ink", 0.8, 1.0))
    s.append(line(72, 31, 72, 120, "ink", 0.8, 1.0))
    for (x, y, d) in ((28, 60, -1), (72, 76, 1), (40, 31, -1), (62, 31, 1)):
        if y == 31:
            s.append(pg([(x, y), (x + 2 * d, y - 3.2), (x + 3.6 * d, y - 2.0)], "accent", 0.85, 0.4, 0.45))
        else:
            s.append(pg([(x, y), (x + 3.2 * d, y - 2.4), (x + 2.2 * d, y - 4.2)], "accent", 0.85, 0.4, 0.45))
    s.append(line(50, 31, 50, 39, "ink", 0.9, 0.6))
    # 倒悬的身体
    s.append(pl([(50, 39), (50, 64)], "ink", 1, 0.9))
    s.append(pl([(50, 64), (59, 55), (50.6, 50)], "ink", 1, 0.9))
    s += robe([(45, 64), (55, 64), (57, 86), (43, 86)])
    s.append(pl([(44.2, 85), (40.5, 80), (47, 75)], "ink", 1, 0.8))
    s.append(pl([(55.8, 85), (59.5, 80), (53, 75)], "ink", 1, 0.8))
    # 光环
    s.append(circle(50, 92.5, 9, "gold", 0.1))
    s.append(ring(50, 92.5, 7.6, "gold", 0.95, 0.7))
    s += sun_rays(50, 92.5, 9, 12, 12, "gold", 0.7, 0.5)
    s += head(50, 92.5, 4.2)
    s += sparkles(12, 7, avoid=[(24, 24, 76, 124)], region=(10, 24, 90, 124))
    s += sparkles(112, 4, region=(32, 98, 68, 122))
    return s


def death():
    """13 死神：黑旗上的五瓣神秘玫瑰、白马与骷髅骑士、远方两座塔之间升起的太阳"""
    s = []
    # 远景：两塔之间升起的太阳
    s.append(arc(76, 96, 6, 180, 360, "gold", 1, 0.8))
    s.append(fill_only(ellipse_points(76, 96, 6, 6, 24, 180, 360), "gold", 0.25))
    s += [l for l in sun_rays(76, 96, 7.6, 10.6, 9, "gold", 0.6, 0.5, -180) if l["p"][3] < 96.1]
    s += tower(64, 69, 82, 98)
    s += tower(83, 88, 82, 98)
    s.append(line(10, 98, 90, 98, "soft", 0.8, 0.6))
    s.append(wave(52, 90, 104, 0.8, 4, "ink", 0.4, 0.5))
    s.append(wave(56, 90, 109, 0.8, 4, "ink", 0.3, 0.5))
    # 骷髅骑士与旗
    s.append(line(46, 28, 46, 92, "ink", 0.9, 0.8))
    flagp = [(46, 30), (70, 31), (66, 42), (70, 53), (46, 52)]
    s.append(fill_only(flagp, "soft", 0.55))
    s.append(pg(flagp, "ink", 0.95, 0.7))
    s += petals(56, 41.5, 6.4, 5, "star", 0.95, 0.5)
    s.append(ring(30, 50, 4.2, "star", 0.95, 0.8))
    s.append(circle(28.6, 49.4, 0.9, "star", 0.9))
    s.append(circle(31.6, 49.4, 0.9, "star", 0.9))
    s.append(line(28.8, 52.6, 31.2, 52.6, "star", 0.7, 0.5))
    s.append(pg([(26, 55), (34, 55), (35, 70), (25, 70)], "ink", 0.95, 0.7, 0.55))
    s.append(pl([(34, 57), (42, 62), (46, 60)], "ink", 0.95, 0.7))
    # 白马
    horse = [(12, 104), (14, 90), (18, 81), (21.5, 77), (21.8, 73.2), (23.6, 76.2), (26, 77), (31, 82), (36, 88.5),
             (36.4, 91), (34.2, 92.2), (29.6, 90.4), (26.6, 90), (25, 94), (24.6, 104)]
    s.append(fill_only(horse, "star", 0.1))
    s.append(pl(horse, "star", 0.95, 0.8))
    s.append(circle(26.4, 81, 0.65, "star"))
    s.append(circle(34.4, 89.6, 0.45, "star", 0.8))
    s.append(pl([(18.6, 80), (14.6, 86), (12.4, 94)], "star", 0.55, 0.5))  # 鬃毛
    s.append(pl([(23, 78.6), (30, 86), (29.6, 90.4)], "ink", 0.6, 0.5))  # 辔头
    s.append(pl([(21, 74), (17.5, 69), (15, 73)], "accent2", 0.85, 0.6))  # 头饰
    # 倒下的王冠
    s += crown(26, 112, 8, 3.4, 2, "gold", 0.9)
    s += sparkles(13, 7, avoid=[(10, 24, 74, 124), (60, 78, 92, 124)], region=(10, 24, 90, 124))
    return s


def temperance():
    """14 节制：展翼天使、胸前方中三角、在两只杯子间倒水、一脚踏水、远方路尽头的日冕"""
    s = []
    s += mountains([(10, 86), (18, 74), (26, 84)], fill=0.12)
    s.append(ring(18, 70.4, 2.2, "gold", 0.95, 0.6))
    s += sun_rays(18, 70.4, 3, 4.6, 8, "gold", 0.7, 0.4)
    s.append(pl([(20, 124), (26, 112), (20, 100), (24, 92), (20, 86)], "soft", 0.8, 0.6))
    s += feather_wing(45, 54, -1, 28, 20, 8, 5)
    s += feather_wing(55, 54, 1, 28, 20, 8, 5)
    s += robe([(44, 52), (56, 52), (61, 106), (39, 106)])
    s.append(circle(50, 45, 6.4, "gold", 0.08))
    s.append(ring(50, 45, 6.4, "gold", 0.6, 0.5))
    s += head(50, 45, 4.0)
    s.append(circle(50, 42.6, 0.8, "gold"))
    s.append(pg([(47, 57), (53, 57), (53, 63), (47, 63)], "gold", 0.9, 0.5))
    s.append(pg([(47.8, 62.2), (52.2, 62.2), (50, 58)], "gold", 0.9, 0.5))
    # 两只杯子与水流
    s.append(pl([(45, 56), (40, 63), (37, 66)], "ink", 1, 0.8))
    s.append(pl([(55, 56), (60, 70), (62, 74)], "ink", 1, 0.8))
    s.append(suit("cups", 35, 64, 4.4, "accent", rot=-38))
    s.append(suit("cups", 63, 79, 4.4, "accent"))
    s.append(pl([(37.6, 61.4), (44, 66), (52, 71), (58, 74), (61.6, 75.6)], "accent2", 0.9, 0.7))
    # 一脚踏水、一脚在岸
    s.append(pl([(46, 106), (45, 114)], "ink", 1, 0.8))
    s.append(pl([(54, 106), (58, 115)], "ink", 1, 0.8))
    for i, y in enumerate((112, 116, 120)):
        s.append(wave(54 + i * 2, 90, y, 0.8, 4, "accent", 0.55, 0.5))
    for x in (80, 86):
        s += flower(x, 100, 2.0, "accent")
        s.append(line(x, 102, x, 110, "soft", 0.8, 0.5))
    s += sparkles(14, 5, avoid=[(10, 26, 90, 124)], region=(10, 24, 90, 40))
    return s


def devil():
    """15 恶魔：额上倒五芒星、羊角与蝙蝠翼、垂下的火炬、半方石台上锁着的两人"""
    s = []
    s.append(pentagram(50, 31.5, 5.6, 90, "gold", 1, 0.7))
    lw = [(44, 52), (34, 42), (24, 40), (14, 46), (20, 49), (14, 55), (22, 57), (17, 64), (30, 62), (44, 60)]
    s.append(fill_only(lw, "soft", 0.5))
    s.append(pg(lw, "ink", 0.9, 0.7))
    s.append(fill_only(mirror(lw), "soft", 0.5))
    s.append(pg(mirror(lw), "ink", 0.9, 0.7))
    s.append(pl([(46, 41), (41, 36), (40, 30), (42.5, 27)], "ink", 1, 0.8))
    s.append(pl([(54, 41), (59, 36), (60, 30), (57.5, 27)], "ink", 1, 0.8))
    s += head(50, 45, 4.6)
    s.append(circle(48.3, 44.2, 0.6, "accent2"))
    s.append(circle(51.7, 44.2, 0.6, "accent2"))
    s.append(pg([(48.5, 49), (51.5, 49), (50, 53)], "ink", 0.8, 0.5, 0.4))
    s += robe([(44, 52), (56, 52), (55, 72), (45, 72)], fo=0.45)
    s.append(pl([(44.5, 54), (38, 50), (35, 44)], "ink", 1, 0.8))
    s.append(pl([(34, 44.6), (33.4, 41.6)], "ink", 1, 0.6))
    s.append(pl([(35.6, 44.2), (35.6, 41.2)], "ink", 1, 0.6))
    s.append(pl([(55.5, 56), (60, 63)], "ink", 1, 0.8))
    s.append(line(59, 61, 64, 76, "ink", 0.9, 0.9))
    s.append(pg([(63, 76), (66.4, 75), (66.8, 80.6), (65, 84), (63.2, 80.4)], "accent2", 0.95, 0.5, 0.6))
    # 半方石台与锁链
    ped = [(40, 72), (60, 72), (60, 84), (40, 84)]
    s.append(fill_only(ped, "soft", 0.45))
    s.append(pg(ped, "ink", 0.9, 0.7))
    s.append(ring(50, 81, 1.4, "gold", 0.95, 0.5))
    for d in (-1, 1):
        for i in range(1, 8):
            t = i / 8
            s.append(ring(50 + d * (1.6 + 15 * t), 81 + 14 * t + 2.5 * math.sin(math.pi * t), 0.85, "gold", 0.85, 0.4))
    # 被锁的两人
    for x, tail in ((34, "accent2"), (66, "accent")):
        s += head(x, 99.5, 3.2)
        s.append(pl([(x - 1.6, 96.6), (x - 2.6, 94)], "ink", 0.9, 0.5))
        s.append(pl([(x + 1.6, 96.6), (x + 2.6, 94)], "ink", 0.9, 0.5))
        s += robe([(x - 2.6, 103.6), (x + 2.6, 103.6), (x + 3.2, 116), (x - 3.2, 116)], fo=0.2)
        s.append(pl([(x - 1.6, 116), (x - 2, 122)]))
        s.append(pl([(x + 1.6, 116), (x + 2, 122)]))
        d = -1 if x < 50 else 1
        s.append(pl([(x + 3 * d, 112), (x + 7 * d, 114), (x + 8 * d, 110)], tail, 0.9, 0.6))
        s.append(circle(x + 8 * d, 109.4, 1.1, tail, 0.9))
    s += sparkles(15, 4, avoid=[(12, 24, 88, 124)], region=(10, 24, 90, 124))
    return s


def tower_card():
    """16 高塔：岩峰上的高塔被闪电劈中、王冠被掀落、窗中喷火、两人坠落、火星四散"""
    s = []
    rock = [(22, 124), (30, 112), (36, 113), (42, 106), (58, 106), (64, 104), (70, 112), (78, 124)]
    s.append(fill_only(rock, "soft", 0.35))
    s.append(pl(rock, "ink", 0.85, 0.7))
    tw = [(41, 106), (59, 106), (57.5, 46), (42.5, 46)]
    s.append(fill_only(tw, "soft", 0.3))
    s.append(pg(tw, "ink", 1, 0.85))
    for (x, y) in ((47, 60), (52, 72), (47, 86)):
        s.append(pg([(x, y), (x + 2.4, y), (x + 2.4, y + 5), (x, y + 5)], "gold", 0.9, 0.5, 0.55))
        s.append(pg([(x + 1.2, y - 4.6), (x + 3.4, y - 0.6), (x - 1, y - 0.6)], "accent2", 0.9, 0.5, 0.6))
    s.append(pg([(43, 46), (57, 46), (56, 42), (44, 42)], "ink", 0.9, 0.6))
    # 掀落的王冠（歪向右上）
    cr = [(57, 38), (69, 34.6), (68.6, 29), (65.4, 32.4), (62.8, 27.6), (61.2, 33.4), (57.4, 31.6)]
    s.append(pg(cr, "gold", 1, 0.7, 0.3))
    # 闪电
    bolt = [(12, 25), (31, 37), (27, 38.4), (45, 47), (40.8, 48.2), (51, 55), (35.6, 43.6), (39.8, 42.4), (23, 33.6), (26.6, 32.4)]
    s.append(pg(bolt, "gold", 1, 0.5, 0.75))
    # 坠落的两人
    for (x, y, tilt) in ((27, 74, -1), (72, 84, 1)):
        s += head(x, y + 10, 2.8)
        s.append(pg([(x - 2.4, y + 6.6), (x + 2.4, y + 6.6), (x + 1.8, y - 2), (x - 1.8, y - 2)], "ink", 0.9, 0.6, 0.25))
        s.append(pl([(x - 2.2, y + 5.6), (x - 7, y + 9 * 1.0)], "ink", 0.9, 0.6))
        s.append(pl([(x + 2.2, y + 5.6), (x + 7, y + 8)], "ink", 0.9, 0.6))
        s.append(pl([(x - 1, y - 2), (x - 3 * tilt, y - 9)], "ink", 0.9, 0.6))
        s.append(pl([(x + 1, y - 2), (x + 2 - 2 * tilt, y - 8)], "ink", 0.9, 0.6))
    # 四散的火星（ yod ）
    rng = random.Random(1616)
    for i in range(14):
        x = rng.choice([rng.uniform(14, 36), rng.uniform(64, 88)])
        y = rng.uniform(30, 118)
        s.append(pg([(x, y - 1.6), (x + 0.9, y + 0.4), (x, y + 1.2), (x - 0.9, y + 0.4)], "gold", rng.uniform(0.6, 0.95), 0, 1))
    return s


def star_card():
    """17 星星：头顶的大八芒星与环绕的七颗小星、跪在池边双手倒水的女子、远处树上的朱鹭"""
    s = []
    s.append(circle(50, 42, 15, "gold", 0.07))
    s.append(star(50, 42, 12.5, 8, 0.42, "gold", 0.95))
    s.append(star(50, 42, 8.5, 8, 0.38, "star", 0.9, rot=-67.5))
    for i in range(7):
        a = math.radians(-90 + 360 / 7 * (i + 0.5))
        s.append(star(50 + 31 * math.cos(a), 46 + 19 * math.sin(a), 2.6, 8, 0.4, "star", 0.95))
    # 远处的树与朱鹭
    s.append(line(16, 98, 16, 82, "ink", 0.7, 0.6))
    s.append(ring(16, 79, 4, "soft", 0.8, 0.6))
    s.append(pl([(18, 76), (20.4, 74), (22, 75.2), (24.6, 77.6)], "accent", 0.9, 0.5))
    # 地面与水池
    s.append(pl([(10, 104), (24, 102), (40, 103), (48, 106)], "soft", 0.9, 0.7))
    pool = ellipse_points(70, 112, 20, 7, 32)
    s.append(fill_only(pool, "accent", 0.12))
    s.append(pl(pool, "accent", 0.7, 0.6))
    s.append(wave(56, 84, 112, 0.6, 4, "accent", 0.5, 0.4))
    # 女子：一膝跪地、一足入池
    s += head(48, 74, 3.8)
    s += robe([(45.5, 79), (50.5, 79), (53, 96), (44, 96)], fo=0.18)
    s.append(pl([(45, 96), (38, 100), (36, 106)], "ink", 1, 0.8))
    s.append(pl([(52, 96), (58, 101), (60, 110)], "ink", 1, 0.8))
    s.append(pl([(45.5, 81), (38, 86), (34, 87)], "ink", 1, 0.8))
    s.append(pl([(50.5, 81), (58, 86), (62, 87)], "ink", 1, 0.8))
    for x, d in ((32, -1), (64, 1)):
        jug = [(x - 2.2 * d, 84), (x + 2.2 * d, 84), (x + 3 * d, 88), (x + 1.6 * d, 92), (x - 1.6 * d, 92), (x - 3 * d, 88)]
        s.append(pg(jug, "gold", 0.95, 0.6, 0.25))
    s.append(pl([(29, 91), (26, 96), (25, 102)], "accent", 0.9, 0.6))
    s.append(pl([(67, 91), (70, 99), (71, 108)], "accent", 0.9, 0.6))
    s += sparkles(17, 5, region=(10, 60, 90, 100), avoid=[(26, 66, 74, 124), (10, 70, 26, 100)])
    return s


def moon_card():
    """18 月亮：带人脸的月亮与滴落的露珠、两座塔、狗与狼对月嚎叫、池中爬出的螯虾、蜿蜒的小径"""
    s = []
    s.append(circle(50, 40, 11, "gold", 0.1))
    s.append(ring(50, 40, 11, "gold", 0.95, 0.8))
    s.append(crescent(50, 40, 9.6, 180, 0.42, "gold", 0.9))
    s.append(circle(53.4, 38, 0.7, "gold", 0.9))
    for i in range(16):
        a = math.radians(i * 22.5)
        r1 = 15.5 if i % 2 == 0 else 13.8
        s.append(line(50 + 12.6 * math.cos(a), 40 + 12.6 * math.sin(a), 50 + r1 * math.cos(a), 40 + r1 * math.sin(a), "gold", 0.6, 0.5))
    for (x, y) in ((40, 59), (46, 63), (54, 61), (60, 58), (50, 66), (36, 65), (64, 65)):
        s.append(pg([(x, y - 1.6), (x + 0.9, y + 0.4), (x, y + 1.3), (x - 0.9, y + 0.4)], "gold", 0.85, 0, 1))
    s += tower(12, 22, 68, 100, windows=2)
    s += tower(78, 88, 68, 100, windows=2)
    s.append(line(10, 100, 90, 100, "soft", 0.6, 0.5))
    s.append(pl([(50, 106), (46, 100), (54, 92), (47, 84), (52, 76), (50, 72)], "soft", 0.9, 0.7))
    # 狗（左）与狼（右），仰头嚎叫
    dog = [(28, 100), (28, 92), (30, 87), (32, 82), (31.4, 79.6), (34, 81), (36.2, 79), (35, 83.6), (34, 88), (36, 100)]
    s.append(fill_only(dog, "star", 0.1))
    s.append(pl(dog, "star", 0.95, 0.7))
    wolf = [(72, 100), (72, 92), (70, 87), (68, 82), (68.6, 79.4), (66, 81), (63.6, 78.6), (64.8, 83.6), (66, 88), (64, 100)]
    s.append(fill_only(wolf, "soft", 0.5))
    s.append(pl(wolf, "ink", 0.95, 0.7))
    # 水池与螯虾
    pool = ellipse_points(50, 114, 26, 6.5, 32)
    s.append(fill_only(pool, "accent", 0.14))
    s.append(pl(pool, "accent", 0.7, 0.6))
    s.append(pg([(48.4, 107), (51.6, 107), (52, 116), (48, 116)], "accent2", 0.95, 0.6, 0.35))
    s.append(pl([(48.6, 108), (45, 104), (44, 101.6), (46.4, 102.6)], "accent2", 0.95, 0.6))
    s.append(pl([(51.4, 108), (55, 104), (56, 101.6), (53.6, 102.6)], "accent2", 0.95, 0.6))
    s.append(wave(28, 72, 118, 0.6, 5, "accent", 0.5, 0.4))
    s += sparkles(18, 6, avoid=[(10, 24, 90, 124)], region=(10, 24, 90, 124))
    s += sparkles(118, 6, region=(14, 26, 86, 66), avoid=[(34, 24, 66, 58)])
    return s


def sun_card():
    """19 太阳：带笑脸的大太阳（直光与曲光相间）、墙头的向日葵、骑白马张开双臂的孩子与红旗"""
    s = []
    cx, cy, r = 50, 42, 11
    s.append(circle(cx, cy, r + 9, "gold", 0.06))
    s.append(circle(cx, cy, r, "gold", 0.22))
    s.append(ring(cx, cy, r, "gold", 1, 1.0))
    s += [circle(cx - 3.6, cy - 1.4, 0.8, "gold"), circle(cx + 3.6, cy - 1.4, 0.8, "gold"), arc(cx, cy + 0.6, 4.2, 35, 145, "gold", 0.9, 0.6)]
    for i in range(16):
        a = math.radians(-90 + i * 22.5)
        ux, uy = math.cos(a), math.sin(a)
        if i % 2 == 0:
            s.append(line(cx + (r + 2) * ux, cy + (r + 2) * uy, cx + (r + 10) * ux, cy + (r + 10) * uy, "gold", 0.85, 0.7))
        else:
            px, py = -uy, ux
            pts = []
            for j in range(9):
                t = j / 8
                d = r + 2 + 7 * t
                off = 0.9 * math.sin(t * 2 * math.pi)
                pts.append((cx + d * ux + off * px, cy + d * uy + off * py))
            s.append(pl(pts, "gold", 0.75, 0.5))
    # 墙与向日葵
    s.append(pg([(10, 84), (90, 84), (90, 90), (10, 90)], "ink", 0.6, 0.6, 0.15))
    for x in range(18, 90, 12):
        s.append(line(x, 84, x, 90, "ink", 0.4, 0.4))
    for (x, y) in ((18, 76), (30, 73), (70, 73), (82, 76)):
        s.append(line(x, y + 3, x, 84, "soft", 0.9, 0.6))
        s += sunflower(x, y, 3.8)
    # 白马与孩子
    horse = [(35, 101), (41, 99.5), (55, 99.5), (59, 95), (62, 89.5), (62.4, 86.4), (64, 88.6), (66.4, 90.4), (71, 95.4),
             (70.2, 97.4), (66.6, 96.6), (64.6, 97.6), (63.6, 102), (62, 106.5), (58, 108.4), (42, 108.4), (37, 106.6)]
    s.append(fill_only(horse, "star", 0.1))
    s.append(pg(horse, "star", 0.95, 0.8))
    s.append(pl([(35.4, 101.6), (31.6, 105), (30.6, 111)], "star", 0.85, 0.6))
    s.append(circle(65.6, 91.6, 0.55, "star"))
    for (x0, x1) in ((40, 39), (45, 46.5), (57, 58.6), (61, 63)):
        s.append(line(x0, 107.6, x1, 119, "star", 0.85, 0.7))
    s += head(49, 81.5, 3.4)
    s.append(circle(49, 77.6, 0.9, "accent2"))
    s.append(pl([(49, 85), (49, 97)], "ink", 1, 0.9))
    s.append(pl([(38, 82), (43, 88), (49, 87.4), (55, 88), (60, 83)], "ink", 1, 0.8))
    s.append(pl([(49, 97), (46, 104), (47.5, 106)], "ink", 1, 0.8))
    s.append(line(39, 85, 34, 66, "ink", 0.9, 0.6))
    s.append(pg([(34.4, 67), (24, 64), (27.6, 70), (22, 76), (34.4, 75)], "accent2", 0.95, 0.6, 0.45))
    s += sparkles(19, 4, avoid=[(10, 24, 90, 124)], region=(10, 24, 90, 124))
    return s


def judgement():
    """20 审判：云端吹号的天使与号上的十字旗、从棺木中张臂起身的人们、远山与水"""
    s = []
    s += mountains([(10, 100), (20, 88), (28, 96), (38, 84), (48, 96)], fill=0.12)
    s += mountains([(56, 96), (66, 82), (76, 94), (90, 86)], fill=0.12)
    s += feather_wing(46, 41, -1, 33, 9, 6, 5)
    s += feather_wing(54, 41, 1, 33, 9, 6, 5)
    s += head(50, 34, 3.6)
    s.append(pg([(46.5, 38), (53.5, 38), (55, 48), (45, 48)], "accent", 0.9, 0.7, 0.2))
    for x in (26, 40, 58, 74):
        s += cloud(x, 54, 1.1, "ink", 0.6)
    # 号角与十字旗
    s.append(line(52, 37, 70, 58, "gold", 1, 1.0))
    s.append(pg([(68.4, 57.4), (72.8, 54.6), (75, 64), ], "gold", 1, 0.6, 0.4))
    s.append(pg([(62, 49), (70.6, 41.6), (75, 50), (66.4, 57.4)], "star", 0.95, 0.5, 0.15))
    s.append(line(66.3, 45.3, 70.7, 53.7, "accent2", 1, 0.8))
    s.append(line(64.2, 53.2, 72.8, 45.8, "accent2", 1, 0.8))
    for i, y in enumerate((116, 120)):
        s.append(wave(10, 90, y, 0.7, 7, "accent", 0.5 - i * 0.1, 0.5))
    # 棺木与张臂起身的人
    for (x, y, w2) in ((24, 104, 9), (50, 102, 10), (76, 104, 9)):
        s += head(x, y - 13, 3.2)
        s.append(pg([(x - 2.4, y - 9), (x + 2.4, y - 9), (x + 2.8, y), (x - 2.8, y)], "ink", 0.95, 0.6, 0.25))
        s.append(pl([(x - 2.4, y - 8), (x - 7, y - 16), (x - 8, y - 20)], "ink", 1, 0.7))
        s.append(pl([(x + 2.4, y - 8), (x + 7, y - 16), (x + 8, y - 20)], "ink", 1, 0.7))
        box = [(x - w2, y), (x + w2, y), (x + w2 - 1.6, y + 7), (x - w2 + 1.6, y + 7)]
        s.append(fill_only(box, "soft", 0.4))
        s.append(pg(box, "ink", 0.9, 0.7))
    s += sparkles(20, 6, avoid=[(12, 24, 88, 60), (10, 60, 90, 124)], region=(10, 24, 90, 124))
    return s


def world():
    """21 世界：系着∞形红缎带的椭圆桂冠、冠中手持双杖起舞的人、四角的四活物"""
    s = []
    cx, cy, rx, ry = 50, 75, 23, 37
    s.append(pl(ellipse_points(cx, cy, rx, ry, 64), "accent", 0.85, 0.9))
    s.append(pl(ellipse_points(cx, cy, rx - 3, ry - 3, 64), "accent", 0.45, 0.5))
    for i in range(36):
        a = math.radians(i * 10 + 5)
        if abs(math.cos(a)) < 0.17 and math.sin(a) != 0:
            continue
        x = cx + (rx - 1.5) * math.cos(a)
        y = cy + (ry - 1.5) * math.sin(a)
        # 桂叶：沿冠向外斜出的短叶
        tx, ty = -math.sin(a) * rx, math.cos(a) * ry
        n = math.hypot(tx, ty)
        tx, ty = tx / n, ty / n
        nx, ny = math.cos(a), math.sin(a)
        s.append(pg([(x, y), (x + 1.6 * tx + 1.4 * nx, y + 1.6 * ty + 1.4 * ny), (x + 3.2 * tx, y + 3.2 * ty), (x + 1.6 * tx - 1.0 * nx, y + 1.6 * ty - 1.0 * ny)], "accent", 0.75, 0.4, 0.4))
    s.append(lemniscate(cx, cy - ry, 4.6, "accent2", 1, 0.9))
    s.append(lemniscate(cx, cy + ry, 4.6, "accent2", 1, 0.9))
    # 舞者
    s += head(50, 54, 3.6)
    scarf = [(46, 60), (54, 59), (56, 72), (52, 82), (45, 84), (47, 72)]
    s.append(fill_only(scarf, "accent2", 0.25))
    s.append(pg(scarf, "accent2", 0.9, 0.6))
    s.append(pl([(50, 58), (50, 82)], "ink", 0.9, 0.8))
    s.append(pl([(49.5, 82), (48, 96), (47, 102)], "ink", 1, 0.8))
    s.append(pl([(50.5, 82), (57, 90), (50.5, 94)], "ink", 1, 0.8))
    s.append(pl([(47, 61), (41, 66), (38, 70)], "ink", 1, 0.8))
    s.append(pl([(53, 61), (59, 66), (62, 70)], "ink", 1, 0.8))
    for x in (38, 62):
        s.append(line(x - 0.8, 64, x + 0.8, 76, "gold", 1, 0.8))
        s.append(circle(x - 0.8, 63.6, 0.8, "gold"))
        s.append(circle(x + 0.8, 76.4, 0.8, "gold"))
    s += zodiac_medallion("♒︎", 17, 32)
    s += zodiac_medallion("♏︎", 83, 32)
    s += zodiac_medallion("♉︎", 17, 116)
    s += zodiac_medallion("♌︎", 83, 116)
    s += sparkles(21, 6, avoid=[(10, 24, 24, 40), (76, 24, 90, 40), (10, 108, 24, 124), (76, 108, 90, 124), (24, 30, 76, 120)])
    return s


MAJORS = [fool, magician, high_priestess, empress, emperor, hierophant, lovers, chariot, strength, hermit, wheel,
          justice, hanged_man, death, temperance, devil, tower_card, star_card, moon_card, sun_card, judgement, world]


# ---------- 小阿卡纳：1–10 按点数排版，宫廷牌是花色符号 + 身份标记 ----------

# 点数阵列（像扑克牌），x 列 32 / 50 / 68，y 在 30…118 之间
PIPS = {
    2: [(50, 48), (50, 100)],
    3: [(50, 40), (50, 74), (50, 108)],
    4: [(33, 48), (67, 48), (33, 100), (67, 100)],
    5: [(33, 44), (67, 44), (50, 74), (33, 104), (67, 104)],
    6: [(33, 40), (67, 40), (33, 74), (67, 74), (33, 108), (67, 108)],
    7: [(33, 40), (67, 40), (50, 57), (33, 74), (67, 74), (33, 108), (67, 108)],
    8: [(33, 40), (67, 40), (50, 57), (33, 74), (67, 74), (50, 91), (33, 108), (67, 108)],
    9: [(33, 36), (67, 36), (33, 61), (67, 61), (50, 74), (33, 87), (67, 87), (33, 112), (67, 112)],
    10: [(33, 36), (67, 36), (50, 48.5), (33, 61), (67, 61), (33, 87), (67, 87), (50, 99.5), (33, 112), (67, 112)],
}

# 点数越多符号越小
PIP_SIZE = {2: 11, 3: 10, 4: 10, 5: 9.5, 6: 9, 7: 8.6, 8: 8.4, 9: 8, 10: 7.6}


def constellation(points, o=0.28):
    """把点数连成一笔星座线（淡），点上缀星"""
    out = []
    if len(points) >= 2:
        order = sorted(points, key=lambda p: (p[1], p[0]))
        out.append(pl(order, "soft", o, 0.5))
    return out


def ace(s, seed):
    color = SUIT_COLOR[s]
    out = [circle(50, 74, 30, color, 0.07), ring(50, 74, 27, "gold", 0.55, 0.5), ring(50, 74, 24.5, "gold", 0.3, 0.4)]
    for i in range(24):
        a = math.radians(i * 15)
        r1 = 33 if i % 2 == 0 else 30.5
        out.append(line(50 + 28.5 * math.cos(a), 74 + 28.5 * math.sin(a), 50 + r1 * math.cos(a), 74 + r1 * math.sin(a), "gold", 0.55, 0.45))
    # 云中伸出的手托着花色（韦特原画的意象），这里画成一朵小云
    out += cloud(22, 112, 1.0, "ink", 0.5)
    out += cloud(80, 38, 0.8, "ink", 0.45)
    out.append(suit(s, 50, 74, 21, color, w=1.25))
    out += sparkles(seed, 6, avoid=[(18, 40, 82, 108)])
    return out


def pips(s, n, seed):
    color = SUIT_COLOR[s]
    points = PIPS[n]
    size = PIP_SIZE[n]
    out = constellation(points)
    # 剑和杖是细长的竖条，放大一点才和杯、币一样有分量；杖微微向外倾，免得像栅栏
    if s in ("swords", "wands"):
        size *= 1.25
    for (x, y) in points:
        rot = None
        if s == "wands" and x != 50:
            rot = -10 if x < 50 else 10
        out.append(suit(s, x, y, size, color, rot=rot))
    out += sparkles(seed, 4, avoid=[(x - size - 2, y - size - 2, x + size + 2, y + size + 2) for x, y in points])
    return out


def court(s, rank, seed):
    color = SUIT_COLOR[s]
    out = [circle(50, 84, 24, color, 0.07), ring(50, 84, 22, "gold", 0.45, 0.5)]
    # 台座
    out.append(arc(50, 120, 20, 200, 340, "gold", 0.6, 0.6))
    out.append(line(32, 114, 68, 114, "gold", 0.45, 0.5))
    out.append(suit(s, 50, 84, 17, color, w=1.15))
    if rank == "Page":
        out.append(circle(50, 47, 6, "star", 0.08))
        out.append(star(50, 47, 6.2, 4, 0.3, "star", 0.95))
        out.append(star(50, 47, 3, 4, 0.35, "gold", 0.9, rot=-45))
    elif rank == "Knight":
        out += flag(44, 58, 22, "accent")
        out.append(circle(44, 58, 0.9, "ink", 0.8))
    elif rank == "Queen":
        out += crown(50, 56, 15, 6.5, 2, "gold", 1, 0.7)
        out.append(line(42.5, 56, 57.5, 56, "gold", 0.9, 0.7))
    elif rank == "King":
        out += crown(50, 58, 26, 12, 3, "gold", 1, 0.8)
        out.append(pg([(37, 58), (63, 58), (63, 61), (37, 61)], "gold", 0.95, 0.7, 0.35))
        for i in range(3):
            out.append(circle(43 + i * 7, 59.5, 0.9, "accent2", 0.95))
    out += sparkles(seed, 6, avoid=[(24, 26, 76, 124)])
    return out


def minor(card):
    s = card["suit"]
    roman = card["roman"]
    seed = card["id"]
    if roman in ("Page", "Knight", "Queen", "King"):
        return court(s, roman, seed)
    n = 1 if roman == "A" else int(roman)
    return ace(s, seed) if n == 1 else pips(s, n, seed)


KINDS = {"circle", "ring", "star", "crescent", "line", "polyline", "polygon", "arc", "suit", "glyph"}
COLORS = {"accent", "accent2", "gold", "ink", "star", "soft"}


def validate(cards, suits):
    """和 android/core 的 TarotArtTest 同样的检查：图元都认识、颜色都是语义名、坐标不出牌面"""
    problems = []

    def inside(cid, x, y, pad=0.0):
        if not (-pad <= x <= 100 + pad and -pad <= y <= 160 + pad):
            problems.append(f"{cid}: ({x}, {y}) 出界")

    for cid, shapes in cards.items():
        for s in shapes:
            t = s["t"]
            if t not in KINDS:
                problems.append(f"{cid}: 未知图元 {t}")
            if "c" in s and s["c"] not in COLORS:
                problems.append(f"{cid}: 未知颜色 {s['c']}")
            if t in ("line", "polyline", "polygon"):
                p = s["p"]
                if len(p) < 4 or len(p) % 2:
                    problems.append(f"{cid}: {t} 坐标数 {len(p)}")
                for x, y in zip(p[0::2], p[1::2]):
                    inside(cid, x, y)
            elif t == "glyph":
                inside(cid, s["x"] - s["r"] / 2, s["y"] - s["r"] / 2)
                inside(cid, s["x"] + s["r"] / 2, s["y"] + s["r"] / 2)
            else:
                r = s["r"]
                inside(cid, s["x"] - r, s["y"] - r)
                inside(cid, s["x"] + r, s["y"] + r)
            if t == "suit" and s["s"] not in suits:
                problems.append(f"{cid}: 未知花色 {s['s']}")
    if problems:
        raise SystemExit("\n".join(problems))


def build(tarot_path):
    with open(tarot_path, encoding="utf-8") as fp:
        cards = json.load(fp)
    out = {}
    for card in cards:
        cid = card["id"]
        if card.get("suit"):
            shapes = minor(card)
        else:
            shapes = MAJORS[cid]()
        out[str(cid)] = shapes
    return out


def main():
    root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
    target = sys.argv[1] if len(sys.argv) > 1 else os.path.join(root, "Shared", "Content", "tarot-art.json")
    cards = build(os.path.join(root, "XingXia", "Resources", "tarot.json"))
    validate(cards, suit_templates())
    dump = lambda value: json.dumps(value, ensure_ascii=False, separators=(",", ":"))
    # 每个花色模板、每张牌各占一行，方便 diff 和人工微调
    lines = ["{", f'"_doc":{dump(DOC)},', '"version":1,', '"width":100,', '"height":160,', '"suits":{']
    suits = suit_templates()
    lines += [f'{dump(name)}:{dump(shapes)}' + ("," if i < len(suits) - 1 else "") for i, (name, shapes) in enumerate(suits.items())]
    lines += ["},", '"cards":{']
    lines += [f'{dump(cid)}:{dump(shapes)}' + ("," if i < len(cards) - 1 else "") for i, (cid, shapes) in enumerate(cards.items())]
    lines += ["}", "}"]
    text = "\n".join(lines) + "\n"
    json.loads(text)
    with open(target, "w", encoding="utf-8") as fp:
        fp.write(text)
    count = sum(len(v) for v in cards.values())
    print(f"wrote {target}: {len(cards)} cards, {count} shapes, {len(text) // 1024} KB")


if __name__ == "__main__":
    main()
