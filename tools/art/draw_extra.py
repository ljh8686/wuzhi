# -*- coding: utf-8 -*-
"""Extra item art: keyboard / headphone / desk. Same flat style as the main set."""
import sys
from PIL import Image, ImageDraw
import artlib
from artlib import (Art, INK, CREAM, CREAM_D, TERRA, TERRA_L, TERRA_D,
                    AMBER, AMBER_L, GREEN, GREEN_D, BLUE, BLUE_D, BROWN, BROWN_D, WHITE)

OUT = r"C:/dsh/wuzhi/app/res/drawable-nodpi"
S = 1024


def keyboard():
    a = Art()
    # body
    a.rrect([120, 330, 904, 760], 70, CREAM_D)
    # inner plate
    a.drrect([156, 366, 868, 724], 48, fill=CREAM, outline=INK, w=12)
    # keys: 4 rows
    xs = [210, 330, 450, 570, 690, 810]
    for row in range(3):
        y = 410 + row * 78
        for i, x in enumerate(xs):
            a.drrect([x - 44, y, x + 44, y + 56], 14, fill=WHITE, outline=INK, w=8)
    # space bar
    a.drrect([360, 648, 664, 706], 16, fill=WHITE, outline=INK, w=8)
    a.drrect([210, 648, 336, 706], 16, fill=WHITE, outline=INK, w=8)
    a.drrect([688, 648, 814, 706], 16, fill=WHITE, outline=INK, w=8)
    a.save(OUT + "/art_item_keyboard.png", 256)


def headphone():
    import math
    a = Art()
    # headband: a solid ring segment (outer 300 / inner 196), angles 195..345
    cx, cy, ro, ri = 512, 640, 300, 196
    outer, inner = [], []
    for k in range(61):
        t = math.radians(195 + (345 - 195) * k / 60.0)
        outer.append((cx + ro * math.cos(t), cy + ro * math.sin(t)))
        inner.append((cx + ri * math.cos(t), cy + ri * math.sin(t)))
    a.poly(outer + inner[::-1], TERRA)
    # ear cups
    a.rrect([150, 470, 330, 760], 70, TERRA)
    a.rrect([694, 470, 874, 760], 70, TERRA)
    a.drrect([190, 520, 290, 710], 44, fill=TERRA_D, outline=INK, w=10)
    a.drrect([734, 520, 834, 710], 44, fill=TERRA_D, outline=INK, w=10)
    a.save(OUT + "/art_item_headphone.png", 256)


def desk():
    a = Art()
    # legs
    a.rrect([240, 470, 330, 850], 24, BROWN_D)
    a.rrect([694, 470, 784, 850], 24, BROWN_D)
    # top
    a.rrect([140, 350, 884, 500], 34, BROWN)
    # drawer unit
    a.rrect([380, 500, 644, 700], 26, CREAM_D)
    a.drrect([410, 530, 614, 610], 14, fill=CREAM, outline=INK, w=10)
    a.drrect([410, 628, 614, 676], 12, fill=CREAM, outline=INK, w=10)
    a.line([(490, 570), (534, 570)], INK, 14)
    a.line([(490, 652), (534, 652)], INK, 12)
    # a small book on top
    a.rrect([600, 300, 780, 356], 12, GREEN)
    a.save(OUT + "/art_item_desk.png", 256)


if __name__ == "__main__":
    keyboard(); headphone(); desk()
    print("done")
