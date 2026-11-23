# -*- coding: utf-8 -*-
"""Tiny flat-vector illustration toolkit built on Pillow.

Draws at 1024x1024 with a thick ink outline, then auto-normalises the
silhouette into an 84% box and downsamples with LANCZOS to 256x256.
"""
import math
from PIL import Image, ImageDraw, ImageFilter, ImageChops

S = 1024
CONTENT = 848          # 82.8% of 1024 -> 8.6% safe margin (8% + AA headroom)
STROKE = 34            # ink outline width at work resolution
STROKE_MUL = 1.0       # set per-drawing so the final outline is always ~34px

INK = (35, 32, 28, 255)
CREAM = (246, 242, 235, 255)
CREAM_D = (228, 220, 208, 255)
TERRA = (194, 85, 46, 255)
TERRA_L = (232, 130, 90, 255)
TERRA_D = (160, 66, 36, 255)
AMBER = (242, 166, 90, 255)
AMBER_L = (247, 201, 139, 255)
GREEN = (127, 166, 106, 255)
GREEN_D = (100, 136, 82, 255)
BLUE = (110, 147, 184, 255)
BLUE_D = (84, 116, 150, 255)
PINK = (233, 160, 160, 255)
BROWN = (138, 106, 79, 255)
BROWN_D = (104, 78, 56, 255)
WHITE = (255, 255, 255, 255)


def heart_pts(cx, cy, w, h, n=64):
    pts = []
    for i in range(n):
        t = 2 * math.pi * i / n
        x = 16 * math.sin(t) ** 3
        y = -(13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t))
        pts.append((cx + x * w / 32.0, cy + y * h / 32.0))
    return pts


def star_pts(cx, cy, r, inner=0.44, points=4, rot=0.0):
    pts = []
    for i in range(points * 2):
        a = rot + math.pi * i / points
        rr = r if i % 2 == 0 else r * inner
        pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
    return pts


def arc_pts(cx, cy, r, a0, a1, n=40):
    return [(cx + r * math.cos(math.radians(a0 + (a1 - a0) * i / (n - 1))),
             cy + r * math.sin(math.radians(a0 + (a1 - a0) * i / (n - 1))))
            for i in range(n)]


class Art(object):
    def __init__(self, size=S):
        self.size = (size, size)
        self.obj = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        self.mask = Image.new("L", (size, size), 0)
        self.d = ImageDraw.Draw(self.obj)
        self.md = ImageDraw.Draw(self.mask)
        self.stroke_max = 0.0

    # ---------------- filled primitives (ink outline + flat fill) ----------------
    def rrect(self, box, r, fill, stroke=STROKE, ink=INK):
        s = stroke * STROKE_MUL
        self.stroke_max = max(self.stroke_max, s)
        x0, y0, x1, y1 = box
        self.d.rounded_rectangle([x0 - s, y0 - s, x1 + s, y1 + s], radius=r + s, fill=ink)
        self.d.rounded_rectangle(box, radius=r, fill=fill)
        self.md.rounded_rectangle(box, radius=r, fill=255)

    def ellipse(self, box, fill, stroke=STROKE, ink=INK):
        s = stroke * STROKE_MUL
        self.stroke_max = max(self.stroke_max, s)
        x0, y0, x1, y1 = box
        self.d.ellipse([x0 - s, y0 - s, x1 + s, y1 + s], fill=ink)
        self.d.ellipse(box, fill=fill)
        self.md.ellipse(box, fill=255)

    def circle(self, cx, cy, r, fill, stroke=STROKE, ink=INK):
        self.ellipse([cx - r, cy - r, cx + r, cy + r], fill, stroke, ink)

    def poly(self, pts, fill, stroke=STROKE, ink=INK):
        s = stroke * STROKE_MUL
        self.stroke_max = max(self.stroke_max, s)
        self.d.line(list(pts) + [pts[0]], fill=ink, width=int(2 * s), joint="curve")
        self.d.polygon(pts, fill=fill)
        self.md.polygon(pts, fill=255)

    def blob(self, pts, fill, stroke=STROKE, ink=INK, smooth=0):
        """polygon with optional extra rounding pass (smooth ignored, kept for api)."""
        self.poly(pts, fill, stroke, ink)

    # ---------------- raw details (no outline growth, not in mask) ----------------
    def line(self, pts, color=INK, w=18, joint="curve"):
        self.d.line(pts, fill=color, width=int(w), joint=joint)

    def dline(self, pts, color=INK, w=18, joint="curve"):
        self.d.line(pts, fill=color, width=int(w), joint=joint)

    def darc(self, box, a0, a1, color=INK, w=18):
        self.d.arc(box, a0, a1, fill=color, width=int(w))

    def dellipse(self, box, fill=None, outline=None, w=1):
        self.d.ellipse(box, fill=fill, outline=outline, width=int(w))

    def dcircle(self, cx, cy, r, fill=None, outline=None, w=1):
        self.d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=fill, outline=outline, width=int(w))

    def drrect(self, box, r, fill=None, outline=None, w=1):
        self.d.rounded_rectangle(box, radius=r, fill=fill, outline=outline, width=int(w))

    def dpoly(self, pts, fill=None, outline=None):
        self.d.polygon(pts, fill=fill, outline=outline)

    # ---------------- composition ----------------
    def paste_rotated(self, sub, angle, center=(S // 2, S // 2)):
        o = sub.obj.rotate(angle, resample=Image.BICUBIC, center=center)
        m = sub.mask.rotate(angle, resample=Image.BICUBIC, center=center)
        self.obj = Image.alpha_composite(self.obj, o)
        self.mask = ImageChops.lighter(self.mask, m)
        self.stroke_max = max(self.stroke_max, sub.stroke_max)
        self.d = ImageDraw.Draw(self.obj)
        self.md = ImageDraw.Draw(self.mask)

    def _light_layer(self, bbox):
        """Vertical light->shade gradient plus a soft top-left highlight."""
        x0, y0, x1, y1 = bbox
        h = max(1, y1 - y0)
        grad = Image.new("RGBA", (1, h))
        for y in range(h):
            t = y / float(h - 1) if h > 1 else 0.0
            if t < 0.45:
                a = int(52 * (1.0 - t / 0.45))
                grad.putpixel((0, y), (255, 255, 255, a))
            else:
                a = int(38 * ((t - 0.45) / 0.55))
                grad.putpixel((0, y), (46, 36, 30, a))
        grad = grad.resize((x1 - x0, h), Image.BILINEAR)
        light = Image.new("RGBA", self.size, (0, 0, 0, 0))
        light.paste(grad, (x0, y0))

        hl = Image.new("L", self.size, 0)
        hd = ImageDraw.Draw(hl)
        hd.ellipse([x0 - 260, y0 - 320, x0 + (x1 - x0) * 0.62, y0 + (y1 - y0) * 0.55], fill=105)
        hl = hl.filter(ImageFilter.GaussianBlur(110))
        hi = Image.new("RGBA", self.size, (255, 255, 255, 0))
        hi.putalpha(hl)
        return Image.alpha_composite(light, hi)

    def render(self):
        bbox = self.mask.getbbox()
        if bbox is None:
            raise RuntimeError("empty art")
        light = self._light_layer(bbox)
        # clip light to the flat-fill region only (ink outline stays crisp)
        clip = self.mask.filter(ImageFilter.GaussianBlur(1.2))
        la = ImageChops.multiply(light.getchannel("A"), clip)
        light.putalpha(la)
        comp = Image.alpha_composite(self.obj, light)

        pad = int(self.stroke_max) + 6
        cb = (max(0, bbox[0] - pad), max(0, bbox[1] - pad),
              min(self.size[0], bbox[2] + pad), min(self.size[1], bbox[3] + pad))
        comp = comp.crop(cb)
        m = self.mask.crop(cb)

        w, h = comp.size
        scale = float(CONTENT) / max(w, h)
        nw, nh = max(1, int(round(w * scale))), max(1, int(round(h * scale)))
        comp = comp.resize((nw, nh), Image.LANCZOS)
        m = m.resize((nw, nh), Image.LANCZOS)

        canvas = Image.new("RGBA", self.size, (0, 0, 0, 0))
        ox, oy = (self.size[0] - nw) // 2, (self.size[1] - nh) // 2
        canvas.paste(comp, (ox, oy))

        # soft drop shadow from the normalised silhouette
        sh_mask = Image.new("L", self.size, 0)
        sh_mask.paste(m, (ox, oy))
        sh_mask = sh_mask.filter(ImageFilter.GaussianBlur(40))
        shadow = Image.new("RGBA", self.size, (0, 0, 0, 0))
        shadow.paste((40, 34, 28, 62), (0, 24), sh_mask)
        out = Image.alpha_composite(shadow, canvas)
        return out

    def save(self, path, out_size=256):
        img = self.render()
        img = img.resize((out_size, out_size), Image.LANCZOS)
        img.save(path, "PNG", optimize=True)
        return path
