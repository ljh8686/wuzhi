# -*- coding: utf-8 -*-
"""Hand-drawn flat-vector art set for the wuzhi app.

19 illustrations: 11 item categories + 8 clothing slots.
Run:  python draw_all.py [--only name1,name2] [--sheet]
"""
import os
import sys
import math
from PIL import Image, ImageDraw, ImageFont

import artlib
from artlib import (Art, INK, CREAM, CREAM_D, TERRA, TERRA_L, TERRA_D,
                    AMBER, AMBER_L, GREEN, GREEN_D, BLUE, BLUE_D, PINK,
                    BROWN, BROWN_D, WHITE, heart_pts, star_pts, arc_pts)

OUT = r"C:/dsh/wuzhi/app/res/drawable-nodpi"
S = 1024

COAT = (60, 74, 66, 255)        # #3C4A42 dark green
COAT_L = (92, 110, 98, 255)
LENS = (56, 68, 78, 255)        # smoky sunglasses lens


def _tee_body(a, fill, rib=CREAM_D, pocket=None):
    """T-shirt silhouette with a scooped neck."""
    scoop = arc_pts(512, 316, 96, 25, 155)
    pts = [(318, 352), (250, 448), (296, 540), (372, 468),
           (352, 772), (672, 772), (652, 468),
           (728, 540), (774, 448), (706, 352)]
    pts += [(599, 356)]
    pts += scoop[2:-2]
    pts += [(425, 356)]
    a.poly(pts, fill)
    ribp = arc_pts(512, 316, 66, 25, 155)
    a.dline(ribp, rib, 26)
    if pocket:
        a.drrect([560, 546, 626, 626], 14, fill=pocket, outline=INK, w=10)


# ---------------------------------------------------------------- item categories
def item_phone():
    a = Art()
    a.rrect([330, 130, 694, 894], 86, TERRA)
    a.rrect([356, 156, 668, 868], 62, CREAM)
    cols = [392, 536]
    rows = [290, 426, 562]
    colors = [AMBER, GREEN, BLUE, TERRA_L, PINK, AMBER_L]
    i = 0
    for ry in rows:
        for rx in cols:
            a.drrect([rx, ry, rx + 96, ry + 96], 24, fill=colors[i])
            i += 1
    a.dcircle(420, 206, 15, fill=INK)
    a.drrect([470, 192, 556, 210], 9, fill=INK)
    a.drrect([452, 812, 572, 826], 7, fill=INK)
    return a


def item_appliance():
    a = Art()
    a.rrect([316, 120, 708, 900], 58, CREAM)
    a.rrect([338, 142, 686, 396], 36, AMBER_L)
    a.rrect([338, 424, 686, 878], 36, CREAM_D)
    a.drrect([596, 240, 624, 342], 14, fill=TERRA_D, outline=INK, w=10)
    a.drrect([596, 476, 624, 606], 14, fill=TERRA_D, outline=INK, w=10)
    a.dpoly(heart_pts(466, 264, 58, 52), fill=TERRA_L, outline=INK)
    a.drrect([356, 856, 668, 872], 8, fill=CREAM)
    return a


def item_furniture():
    a = Art()
    a.rrect([236, 286, 788, 700], 104, TERRA)
    a.rrect([266, 330, 494, 648], 54, TERRA_L)
    a.rrect([530, 330, 758, 648], 54, TERRA_L)
    a.drrect([344, 736, 402, 826], 16, fill=BROWN_D, outline=INK, w=12)
    a.drrect([622, 736, 680, 826], 16, fill=BROWN_D, outline=INK, w=12)
    a.rrect([312, 566, 502, 758], 54, AMBER_L)
    a.rrect([522, 566, 712, 758], 54, AMBER_L)
    a.rrect([200, 432, 338, 772], 70, TERRA_L)
    a.rrect([686, 432, 824, 772], 70, TERRA_L)
    return a


def item_clothing():
    a = Art()
    # hanger: hook sits well above the collar, stem drops into the neck scoop
    a.darc([458, 150, 566, 258], 20, 340, INK, 26)
    a.dline([(512, 252), (512, 400)], INK, 26)
    _tee_body(a, GREEN, CREAM_D, GREEN_D)
    return a


def _sneaker(a, ox, oy, k, body, sole=CREAM, lace=CREAM_D):
    """Side-view sneaker, toe to the left. k scales the geometry only."""
    def P(x, y):
        return (ox + x * k, oy + y * k)

    def B(x0, y0, x1, y1):
        return [ox + x0 * k, oy + y0 * k, ox + x1 * k, oy + y1 * k]

    a.rrect(B(22, 196, 598, 286), 44 * k, sole)
    up = [(36, 208), (26, 166), (62, 128), (152, 108), (252, 88),
          (316, 56), (378, 60), (402, 128), (472, 104), (556, 148),
          (586, 198), (588, 214)]
    a.poly([P(x, y) for x, y in up], body)
    a.dellipse(B(408, 110, 536, 170), fill=INK)          # shoe opening
    for i in range(3):
        a.dline([P(240 + i * 8, 122 + i * 32), P(302 + i * 8, 102 + i * 32)], lace, 13 * k)
    a.dline([P(60, 250), P(560, 250)], lace, 9 * k)      # midsole line
    a.drrect(B(540, 128, 586, 174), 10 * k, fill=lace, outline=INK, w=9)


def item_shoe():
    sub = Art()
    _sneaker(sub, 94, 280, 1.35, TERRA, CREAM)
    a = Art()
    a.paste_rotated(sub, -14)
    return a


def item_beauty():
    a = Art()
    a.circle(664, 706, 152, CREAM)
    a.circle(664, 706, 104, PINK)
    a.darc([600, 640, 728, 770], 200, 290, WHITE, 16)
    a.rrect([300, 556, 452, 884], 28, AMBER)
    a.poly([(312, 560), (438, 560), (438, 436), (352, 392), (312, 468)], TERRA_L)
    a.drrect([290, 534, 462, 582], 18, fill=TERRA_D, outline=INK, w=12)
    a.dpoly(star_pts(742, 402, 52, 0.40), fill=AMBER_L, outline=INK)
    return a


def item_sport():
    sub = Art()
    sub.rrect([330, 470, 694, 554], 42, CREAM_D)
    sub.rrect([214, 356, 346, 668], 54, TERRA)
    sub.rrect([678, 356, 810, 668], 54, TERRA)
    sub.rrect([346, 392, 392, 632], 22, TERRA_L)
    sub.rrect([632, 392, 678, 632], 22, TERRA_L)
    sub.rrect([392, 470, 632, 554], 30, AMBER_L)
    for x in (452, 492, 532, 572):
        sub.dline([(x, 486), (x, 538)], AMBER, 10)
    a = Art()
    a.paste_rotated(sub, 22)
    return a


def item_kitchen():
    a = Art()
    a.rrect([276, 448, 748, 800], 62, TERRA)
    a.ellipse([300, 296, 724, 412], AMBER)
    a.circle(512, 288, 46, TERRA_D)
    a.rrect([256, 398, 768, 470], 34, TERRA_L)
    a.rrect([188, 556, 292, 652], 32, TERRA_L)
    a.rrect([732, 556, 836, 652], 32, TERRA_L)
    return a


def item_book():
    a = Art()
    # cover behind the pages
    a.poly([(120, 330), (512, 394), (904, 330), (904, 742), (512, 812), (120, 742)], TERRA)
    a.poly([(154, 350), (500, 406), (500, 780), (154, 718)], CREAM)
    a.poly([(870, 350), (524, 406), (524, 780), (870, 718)], CREAM)
    a.dline([(512, 400), (512, 796)], INK, 12)
    for i in range(3):
        y = 478 + i * 84
        a.dline([(196, y), (462, y - 16)], CREAM_D, 11)
        a.dline([(562, y - 16), (828, y)], CREAM_D, 11)
    return a


def item_transport():
    a = Art()
    a.circle(320, 738, 86, INK)
    a.circle(320, 738, 34, CREAM)
    a.circle(712, 738, 86, INK)
    a.circle(712, 738, 34, CREAM)
    a.rrect([340, 376, 700, 566], 88, TERRA)
    a.rrect([176, 518, 848, 744], 96, TERRA)
    a.rrect([376, 404, 502, 532], 28, CREAM)
    a.rrect([526, 404, 662, 532], 28, CREAM)
    a.drrect([800, 596, 846, 648], 18, fill=AMBER_L)
    a.drrect([556, 598, 612, 622], 10, fill=TERRA_D)
    a.dline([(520, 560), (520, 706)], TERRA_D, 10)
    return a


def item_other():
    a = Art()
    a.poly([(268, 470), (356, 384), (852, 384), (756, 470)], AMBER)
    a.rrect([268, 470, 756, 826], 34, AMBER_L)
    a.drrect([474, 492, 550, 812], 6, fill=CREAM_D)
    a.drrect([580, 596, 706, 708], 16, fill=CREAM, outline=INK, w=10)
    a.dline([(602, 632), (684, 632)], CREAM_D, 10)
    a.dline([(602, 668), (662, 668)], CREAM_D, 10)
    a.dpoly([(470, 470), (554, 470), (642, 392), (558, 392)], CREAM_D)
    return a


# ---------------------------------------------------------------- clothing slots
def cloth_top():
    a = Art()
    _tee_body(a, TERRA, CREAM_D, AMBER)
    return a


def cloth_pants():
    a = Art()
    pts = [(322, 248), (702, 248), (722, 380), (714, 890), (570, 890),
           (512, 528), (454, 890), (310, 890), (302, 380)]
    a.poly(pts, BLUE)
    a.rrect([312, 220, 712, 314], 22, BLUE_D)
    a.dcircle(512, 300, 17, fill=AMBER)
    a.dline([(512, 314), (512, 470)], BLUE_D, 10)
    # slanted front pockets
    a.dline([(342, 330), (398, 440)], CREAM_D, 11)
    a.dline([(682, 330), (626, 440)], CREAM_D, 11)
    # outer leg stitching
    a.dline([(326, 400), (338, 868)], CREAM_D, 9)
    a.dline([(698, 400), (686, 868)], CREAM_D, 9)
    # belt loops
    for x in (368, 512, 656):
        a.drrect([x - 9, 214, x + 9, 258], 6, fill=BLUE)
    return a


def cloth_skirt():
    a = Art()
    pts = [(360, 362), (664, 362), (762, 806), (700, 836), (600, 846),
           (512, 850), (424, 846), (324, 836), (262, 806)]
    a.poly(pts, TERRA_L)
    a.rrect([354, 298, 670, 372], 22, TERRA_D)
    a.dline([(440, 386), (418, 828)], TERRA, 14)
    a.dline([(512, 386), (512, 844)], TERRA, 14)
    a.dline([(584, 386), (606, 828)], TERRA, 14)
    return a


def cloth_outer():
    a = Art()
    scoop = arc_pts(512, 282, 92, 25, 155)
    pts = [(350, 292), (238, 412), (288, 584), (362, 478),
           (326, 880), (698, 880), (662, 478),
           (736, 584), (786, 412), (674, 292)]
    pts += [(600, 310)]
    pts += scoop[2:-2]
    pts += [(424, 310)]
    a.poly(pts, COAT)
    # lapels
    a.poly([(430, 300), (512, 356), (500, 620), (396, 464)], CREAM)
    a.poly([(594, 300), (512, 356), (524, 620), (628, 464)], CREAM)
    # placket + buttons
    a.dline([(512, 600), (512, 872)], COAT_L, 12)
    for y in (660, 748, 836):
        a.dcircle(512, y, 22, fill=AMBER, outline=INK, w=9)
    # welt pockets
    a.drrect([344, 690, 442, 750], 14, fill=COAT_L, outline=INK, w=10)
    a.drrect([582, 690, 680, 750], 14, fill=COAT_L, outline=INK, w=10)
    # cuffs
    a.dline([(262, 512), (300, 578)], COAT_L, 16)
    a.dline([(762, 512), (724, 578)], COAT_L, 16)
    return a


def cloth_shoes():
    a = Art()
    _sneaker(a, 60, 180, 1.15, AMBER, CREAM_D)
    _sneaker(a, 250, 420, 1.15, TERRA, CREAM)
    return a


def cloth_acc():
    """Sunglasses (wayfarer)."""
    a = Art()
    # temples: filled bars so they survive the bbox crop
    a.poly([(168, 500), (168, 548), (24, 492), (24, 444)], TERRA)
    a.poly([(856, 500), (856, 548), (1000, 492), (1000, 444)], TERRA)
    # frames + lenses
    a.rrect([110, 428, 478, 668], 92, TERRA)
    a.rrect([148, 458, 440, 638], 62, LENS, stroke=0)
    a.rrect([546, 428, 914, 668], 92, TERRA)
    a.rrect([584, 458, 876, 638], 62, LENS, stroke=0)
    # nose bridge
    a.rrect([452, 446, 572, 502], 26, TERRA)
    # specular dots
    a.dellipse([188, 486, 248, 546], fill=(255, 255, 255, 95))
    a.dellipse([624, 486, 684, 546], fill=(255, 255, 255, 95))
    return a


def cloth_bag():
    a = Art()
    a.darc([390, 350, 634, 590], 180, 360, INK, 50)
    a.darc([390, 350, 634, 590], 180, 360, CREAM, 24)
    a.rrect([296, 462, 728, 824], 54, TERRA)
    a.rrect([316, 482, 708, 620], 40, TERRA_L)
    a.drrect([484, 596, 540, 652], 16, fill=AMBER, outline=INK, w=12)
    return a


def cloth_underwear():
    """Bra."""
    a = Art()
    # straps
    a.dline([(338, 416), (250, 264)], INK, 58)
    a.dline([(338, 416), (250, 264)], CREAM_D, 32)
    a.dline([(686, 416), (774, 264)], INK, 58)
    a.dline([(686, 416), (774, 264)], CREAM_D, 32)
    # cups
    a.ellipse([262, 368, 510, 716], PINK)
    a.ellipse([514, 368, 762, 716], PINK)
    # underband
    a.rrect([270, 614, 754, 740], 50, PINK)
    # centre bow
    a.dpoly([(512, 672), (466, 638), (466, 706)], TERRA)
    a.dpoly([(512, 672), (558, 638), (558, 706)], TERRA)
    a.dcircle(512, 672, 15, fill=TERRA)
    return a


DRAW = [
    ("art_item_phone", item_phone),
    ("art_item_appliance", item_appliance),
    ("art_item_furniture", item_furniture),
    ("art_item_clothing", item_clothing),
    ("art_item_shoe", item_shoe),
    ("art_item_beauty", item_beauty),
    ("art_item_sport", item_sport),
    ("art_item_kitchen", item_kitchen),
    ("art_item_book", item_book),
    ("art_item_transport", item_transport),
    ("art_item_other", item_other),
    ("art_cloth_top", cloth_top),
    ("art_cloth_pants", cloth_pants),
    ("art_cloth_skirt", cloth_skirt),
    ("art_cloth_outer", cloth_outer),
    ("art_cloth_shoes", cloth_shoes),
    ("art_cloth_acc", cloth_acc),
    ("art_cloth_bag", cloth_bag),
    ("art_cloth_underwear", cloth_underwear),
]


def _font(sz):
    for p in ("C:/Windows/Fonts/msyh.ttc", "C:/Windows/Fonts/arial.ttf"):
        try:
            return ImageFont.truetype(p, sz)
        except Exception:
            pass
    return ImageFont.load_default()


def build(fn):
    """Draw twice: the second pass normalises the outline to ~34px final."""
    artlib.STROKE_MUL = 1.0
    probe = fn()
    bb = probe.mask.getbbox()
    m = max(bb[2] - bb[0], bb[3] - bb[1])
    # want s*848/(m+2s) == 34  ->  s = 34m/780
    artlib.STROKE_MUL = (34.0 * m / 780.0) / 34.0
    return fn()


def contact_sheet(path, cols=5, cell=300):
    rows = (len(DRAW) + cols - 1) // cols
    pad = 14
    W = cols * (cell + pad) + pad
    H = rows * (cell + 40 + pad) + pad
    sheet = Image.new("RGB", (W, H), (252, 249, 244))
    d = ImageDraw.Draw(sheet)
    f = _font(22)
    for i, (name, _) in enumerate(DRAW):
        c, r = i % cols, i // cols
        x = pad + c * (cell + pad)
        y = pad + r * (cell + 40 + pad)
        d.rounded_rectangle([x, y, x + cell, y + cell], 18, fill=(240, 234, 224))
        p = os.path.join(OUT, name + ".png")
        if os.path.exists(p):
            im = Image.open(p).convert("RGBA")
            im.thumbnail((cell - 16, cell - 16), Image.LANCZOS)
            sheet.paste(im, (x + (cell - im.width) // 2, y + (cell - im.height) // 2), im)
        d.text((x + 6, y + cell + 8), name.replace("art_", ""), font=f, fill=(60, 52, 44))
    sheet.save(path, "PNG", optimize=True)
    return path


def main():
    only = None
    for i, a in enumerate(sys.argv):
        if a == "--only" and i + 1 < len(sys.argv):
            only = set(sys.argv[i + 1].split(","))
    os.makedirs(OUT, exist_ok=True)
    for name, fn in DRAW:
        key = name.replace("art_", "")
        if only and key not in only:
            continue
        art = build(fn)
        p = os.path.join(OUT, name + ".png")
        art.save(p, 256)
        print("wrote", os.path.basename(p), os.path.getsize(p))
    if "--sheet" in sys.argv:
        print("sheet", contact_sheet(r"C:/dsh/wuzhi/tools/art/contact_sheet.png"))


if __name__ == "__main__":
    main()
