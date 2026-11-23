#!/usr/bin/env python3
# Generate launcher icons for the "物资" app: terracotta rounded square with a white ¥ glyph.
import os
from PIL import Image, ImageDraw

BASE = r"C:\dsh\wuzhi\app\res"

SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

TOP = (226, 100, 58)
BOT = (166, 60, 34)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def rounded_mask(size, radius):
    m = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(m)
    d.rounded_rectangle([0, 0, size - 1, size - 1], radius=radius, fill=255)
    return m


def make(size, supersample=4):
    S = size * supersample
    # vertical gradient background
    bg = Image.new("RGB", (S, S))
    px = bg.load()
    for y in range(S):
        c = lerp(TOP, BOT, y / max(1, S - 1))
        for x in range(S):
            px[x, y] = c
    # rounded corners
    mask = rounded_mask(S, int(S * 0.22))
    img = Image.new("RGB", (S, S), (255, 255, 255))
    img.paste(bg, (0, 0), mask)
    d = ImageDraw.Draw(img)

    white = (255, 255, 255)
    w = int(S * 0.072)
    cx = S * 0.5
    # ¥ diagonals
    d.line([(cx - S * 0.17, S * 0.27), (cx, S * 0.47)], fill=white, width=w, joint="curve")
    d.line([(cx + S * 0.17, S * 0.27), (cx, S * 0.47)], fill=white, width=w, joint="curve")
    # vertical stem
    d.line([(cx, S * 0.46), (cx, S * 0.75)], fill=white, width=w)
    # two horizontal bars
    for yy in (S * 0.545, S * 0.635):
        d.line([(cx - S * 0.155, yy), (cx + S * 0.155, yy)], fill=white, width=w)
    # round caps / joints
    r = w // 2
    for (x, y) in [(cx - S * 0.17, S * 0.27), (cx + S * 0.17, S * 0.27),
                   (cx, S * 0.75), (cx - S * 0.155, S * 0.545), (cx + S * 0.155, S * 0.545),
                   (cx - S * 0.155, S * 0.635), (cx + S * 0.155, S * 0.635)]:
        d.ellipse([x - r, y - r, x + r, y + r], fill=white)

    return img.resize((size, size), Image.LANCZOS)


def main():
    for folder, size in SIZES.items():
        d = os.path.join(BASE, folder)
        os.makedirs(d, exist_ok=True)
        img = make(size)
        out = os.path.join(d, "ic_launcher.png")
        img.save(out, "PNG")
        print("wrote", out, size)
    # a large preview
    prev = os.path.join(r"C:\dsh\wuzhi", "icon_preview.png")
    make(512).save(prev, "PNG")
    print("wrote", prev)


if __name__ == "__main__":
    main()
