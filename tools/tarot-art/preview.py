#!/usr/bin/env python3
"""把 tarot-art.json 渲染成 SVG 拼图，用来逐张检查构图。

用法：python3 tools/tarot-art/preview.py <输出目录> [主题 id] [json 路径]
输出 <输出目录>/tarot-<主题>.html（78 张牌的网格）和 majors-<主题>.html（只有大阿卡纳，放大）。
渲染规则与 iOS TarotArtView、Android TarotArt 一致：同样的语义色、同样把 arc / crescent 采样成折线。
"""
import html
import json
import math
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))

# 主题的深色版本：backgroundTop, backgroundBottom, accent, accentSecondary, star
THEMES = {
    "nebula": (0x0B0720, 0x1A0B3B, 0xA78BFA, 0xF472B6, 0xFFFFFF),
    "aurora": (0x02121A, 0x05282A, 0x34F5C5, 0x38BDF8, 0xFFFFFF),
    "sunset": (0x1A0A0F, 0x3A1022, 0xFF9F43, 0xFF4D6D, 0xFFE8D6),
    "abyss": (0x020817, 0x071C46, 0x60A5FA, 0x22D3EE, 0xE0F2FE),
    "redGiant": (0x160305, 0x3A0A0C, 0xFF6B6B, 0xFFB86B, 0xFFE4E1),
    "sakura": (0x1A0915, 0x351333, 0xFF8FC7, 0xC084FC, 0xFFE4F1),
    "eventHorizon": (0x000000, 0x0A0A10, 0xF5D78E, 0xE5E7EB, 0xF5F5F5),
    "solarGold": (0x120C02, 0x2A1B05, 0xFFD166, 0xF4A261, 0xFFF7E0),
}
GOLD = 0xF5D78E
SUIT_COLORS = {"wands": (0xB83A1B, 0x3A0D05), "cups": (0x1E6FB8, 0x0A1E45), "swords": (0x6B7686, 0x1C222C), "pentacles": (0xA97A12, 0x3A2806)}


def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


def mix(a, b, t):
    a, b = rgb(a) if isinstance(a, int) else a, rgb(b) if isinstance(b, int) else b
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def css(c):
    c = rgb(c) if isinstance(c, int) else c
    return "#%02x%02x%02x" % c


def palette(theme):
    top, bottom, accent, accent2, star = THEMES[theme]
    return {
        "accent": rgb(accent),
        "accent2": rgb(accent2),
        "gold": rgb(GOLD),
        "ink": mix(star, accent, 0.12),
        "star": mix(star, GOLD, 0.3),
        "soft": mix(accent, bottom, 0.45),
        "_top": rgb(top),
        "_bottom": rgb(bottom),
    }


def sample_arc(x, y, r, a0, a1):
    steps = max(6, math.ceil(abs(a1 - a0) / 8))
    return [(x + r * math.cos(math.radians(a0 + (a1 - a0) * i / steps)), y + r * math.sin(math.radians(a0 + (a1 - a0) * i / steps))) for i in range(steps + 1)]


def crescent_points(x, y, r, a, k):
    d = 2 * r * min(max(k, 0.05), 0.95)
    alpha = math.degrees(math.acos(-d / (2 * r)))
    beta = math.degrees(math.acos(d / (2 * r)))
    ux, uy = math.cos(math.radians(a)), math.sin(math.radians(a))
    outer = sample_arc(x, y, r, a - alpha, a + alpha)
    inner = sample_arc(x - ux * d, y - uy * d, r, a + beta, a - beta)
    return outer + inner


def star_points(x, y, r, n, k, rot):
    pts = []
    for i in range(2 * n):
        rr = r if i % 2 == 0 else r * k
        ang = math.radians(rot + i * 180 / n)
        pts.append((x + rr * math.cos(ang), y + rr * math.sin(ang)))
    return pts


DEFAULTS = {  # (填充, 线宽)
    "circle": (1, 0), "star": (1, 0), "crescent": (1, 0),
    "ring": (0, 0.8), "polygon": (0, 0.8), "line": (0, 0.8), "polyline": (0, 0.8), "arc": (0, 0.8),
}


def pts_attr(points):
    return " ".join("%.2f,%.2f" % p for p in points)


def render_shape(s, pal, templates, color_override=None, opacity=1.0, width_scale=1.0, transform=None):
    t = s["t"]
    color = css(pal[s["c"]] if "c" in s else (color_override or pal["ink"]))
    o = s.get("o", 1) * opacity
    if t == "suit":
        tpl = templates[s["s"]]
        r = s["r"]
        tr = f'translate({s["x"]},{s["y"]}) rotate({s.get("rot", 0)}) scale({r})'
        inner = "".join(render_shape(child, pal, templates, pal[s["c"]] if "c" in s else color_override, o, s.get("w", 1) / r) for child in tpl)
        return f'<g transform="{tr}">{inner}</g>'
    if t == "glyph":
        return (f'<text x="{s["x"]}" y="{s["y"]}" font-size="{s["r"]}" fill="{color}" fill-opacity="{o}" '
                f'text-anchor="middle" dominant-baseline="central" font-family="Georgia, serif">{html.escape(s["g"])}</text>')
    fill, width = DEFAULTS[t]
    fill = s.get("f", fill)
    width = s.get("w", width) * width_scale
    closed = True
    if t in ("circle", "ring"):
        path = None
    elif t == "star":
        points = star_points(s["x"], s["y"], s["r"], s.get("n", 5), s.get("k", 0.45), s.get("rot", -90))
    elif t == "crescent":
        points = crescent_points(s["x"], s["y"], s["r"], s.get("a", 0), s.get("k", 0.4))
    elif t in ("line", "polyline"):
        p = s["p"]
        points = list(zip(p[0::2], p[1::2]))
        closed = False
        fill = 0
    elif t == "polygon":
        p = s["p"]
        points = list(zip(p[0::2], p[1::2]))
    elif t == "arc":
        points = sample_arc(s["x"], s["y"], s["r"], s["a0"], s["a1"])
        closed = False
        fill = 0
    out = []
    if t in ("circle", "ring"):
        if fill > 0:
            out.append(f'<circle cx="{s["x"]}" cy="{s["y"]}" r="{s["r"]}" fill="{color}" fill-opacity="{o * fill:.3f}"/>')
        if width > 0:
            out.append(f'<circle cx="{s["x"]}" cy="{s["y"]}" r="{s["r"]}" fill="none" stroke="{color}" stroke-opacity="{o:.3f}" stroke-width="{width:.3f}"/>')
        return "".join(out)
    tag = "polygon" if closed else "polyline"
    if fill > 0:
        out.append(f'<{tag} points="{pts_attr(points)}" fill="{color}" fill-opacity="{o * fill:.3f}" stroke="none"/>')
    if width > 0:
        out.append(f'<{tag} points="{pts_attr(points)}" fill="none" stroke="{color}" stroke-opacity="{o:.3f}" stroke-width="{width:.3f}" stroke-linecap="round" stroke-linejoin="round"/>')
    return "".join(out)


ROMAN_RANK = {"Page": "侍从", "Knight": "骑士", "Queen": "王后", "King": "国王"}


def card_svg(card, shapes, pal, templates, px=180):
    suit = card.get("suit")
    top, bottom = mix(pal["_bottom"], pal["accent"], 0.12), mix(pal["_top"], (0, 0, 0), 0.2)
    if suit:
        c0, c1 = SUIT_COLORS[suit]
        top = mix(top, mix(c0, pal["accent"], 0.2), 0.3)
        bottom = mix(bottom, mix(c1, pal["accent"], 0.2), 0.35)
    cid = card["id"]
    body = []
    body.append(f'<defs><linearGradient id="bg{cid}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="{css(top)}"/><stop offset="1" stop-color="{css(bottom)}"/></linearGradient>'
                f'<radialGradient id="gl{cid}" cx="50" cy="72" r="52" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="{css(pal["accent"])}" stop-opacity="0.2"/><stop offset="1" stop-color="{css(pal["accent"])}" stop-opacity="0"/></radialGradient></defs>')
    body.append(f'<rect width="100" height="160" rx="9" fill="url(#bg{cid})"/>')
    body.append(f'<rect width="100" height="160" rx="9" fill="url(#gl{cid})"/>')
    gold = css(pal["gold"])
    body.append(f'<rect x="5" y="5" width="90" height="150" rx="7" fill="none" stroke="{gold}" stroke-width="1.2"/>')
    body.append(f'<rect x="7.4" y="7.4" width="85.2" height="145.2" rx="5.2" fill="none" stroke="{gold}" stroke-opacity="0.4" stroke-width="0.4"/>')
    for s in shapes:
        body.append(render_shape(s, pal, templates))
    # 牌号
    if suit:
        rank = ROMAN_RANK.get(card["roman"], card["roman"])
        body.append(f'<text x="50" y="16" font-size="9" fill="{gold}" text-anchor="middle" dominant-baseline="central" font-family="Songti SC, serif" font-weight="600">{rank}</text>')
        for x in (19, 81):
            body.append(render_shape({"t": "suit", "s": suit, "x": x, "y": 16, "r": 4.2, "c": "gold"}, pal, templates))
    else:
        body.append(f'<text x="50" y="16" font-size="10" fill="{gold}" text-anchor="middle" dominant-baseline="central" font-family="Georgia, serif" font-weight="600">{card["roman"]}</text>')
    # 分隔饰线
    body.append(f'<line x1="26" y1="126.5" x2="45" y2="126.5" stroke="{gold}" stroke-opacity="0.5" stroke-width="0.5"/><line x1="55" y1="126.5" x2="74" y2="126.5" stroke="{gold}" stroke-opacity="0.5" stroke-width="0.5"/>')
    body.append(render_shape({"t": "star", "x": 50, "y": 126.5, "r": 1.8, "n": 4, "k": 0.3, "c": "gold"}, pal, templates))
    body.append(f'<text x="50" y="136.5" font-size="12.5" fill="{css(pal["ink"])}" text-anchor="middle" dominant-baseline="central" font-family="Songti SC, Noto Serif CJK SC, serif" font-weight="700">{card["name"]}</text>')
    body.append(f'<text x="50" y="146.5" font-size="5" letter-spacing="0.6" fill="{gold}" fill-opacity="0.85" text-anchor="middle" dominant-baseline="central" font-family="Georgia, serif">{html.escape(card["en"].upper())}</text>')
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="{px}" height="{px * 1.6:.0f}" viewBox="0 0 100 160">{"".join(body)}</svg>'


def page(cards, art, pal, templates, px, cols):
    cells = "".join(f'<div class="c">{card_svg(card, art[str(card["id"])], pal, templates, px)}<div class="l">{card["id"]} {card["name"]}</div></div>' for card in cards)
    return (f'<!doctype html><html><head><meta charset="utf-8"><style>body{{margin:0;padding:16px;background:#14121c;font:12px sans-serif;color:#ccc}}'
            f'.g{{display:grid;grid-template-columns:repeat({cols},{px}px);gap:14px}}.l{{text-align:center;margin-top:4px}}</style></head>'
            f'<body><div class="g">{cells}</div></body></html>')


def main():
    out_dir = sys.argv[1]
    theme = sys.argv[2] if len(sys.argv) > 2 else "nebula"
    art_path = sys.argv[3] if len(sys.argv) > 3 else os.path.join(ROOT, "Shared", "Content", "tarot-art.json")
    with open(art_path, encoding="utf-8") as fp:
        data = json.load(fp)
    with open(os.path.join(ROOT, "XingXia", "Resources", "tarot.json"), encoding="utf-8") as fp:
        cards = json.load(fp)
    pal = palette(theme)
    os.makedirs(out_dir, exist_ok=True)
    with open(os.path.join(out_dir, f"tarot-{theme}.html"), "w", encoding="utf-8") as fp:
        fp.write(page(cards, data["cards"], pal, data["suits"], 150, 8))
    with open(os.path.join(out_dir, f"majors-{theme}.html"), "w", encoding="utf-8") as fp:
        fp.write(page(cards[:22], data["cards"], pal, data["suits"], 240, 6))
    for card in cards:
        with open(os.path.join(out_dir, f"card-{card['id']:02d}.svg"), "w", encoding="utf-8") as fp:
            fp.write(card_svg(card, data["cards"][str(card["id"])], pal, data["suits"], 300))
    print("wrote", out_dir)


if __name__ == "__main__":
    main()
