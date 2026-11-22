#!/usr/bin/env python3
"""Renders the Source Vessel (its real model and texture) as a square CurseForge project logo.

Usage: python3 tools/render_logo.py [size]   (default 1024)
Output: art/curseforge_logo.png

The block is drawn isometrically texel by texel from models/block/source_vessel.json, with a diamond floating
in the bowl (vanilla texture, read from the dev Minecraft resources jar that `./gradlew build` sets up),
on a dark Ars-purple background with source sparkles.
"""
import io
import json
import math
import random
import sys
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
MODEL = ROOT / "src/main/resources/assets/resources/models/block/source_vessel.json"
TEXTURE = ROOT / "src/main/resources/assets/resources/textures/block/source_vessel.png"
MC_JAR = next((ROOT / "build/moddev/artifacts").glob("neoforge-*-client-extra-aka-minecraft-resources.jar"))
OUT = ROOT / "art/curseforge_logo.png"

SS = 2  # supersampling factor
SHADE = {"up": 1.0, "south": 0.78, "east": 0.62}
COS30 = math.cos(math.radians(30))
# Vertical squash of the ground plane. 0.5 is classic isometric; lower = lower camera, so the stem under the bowl shows.
TILT = 0.36


def load_item(name):
    with zipfile.ZipFile(MC_JAR) as z:
        return Image.open(io.BytesIO(z.read(f"assets/minecraft/textures/item/{name}.png"))).convert("RGBA")


class Projector:
    def __init__(self, size):
        self.scale = size * 0.027  # screen px per model unit: the block spans ~75% of the image
        self.cx = size / 2
        self.cy = size * 0.50

    def __call__(self, x, y, z):
        # Model space: x east, y up, z south (0..16). Camera looks from south-east, above.
        x, z = x - 8, z - 8
        sx = (x - z) * COS30
        sy = (x + z) * TILT - y
        return (self.cx + sx * self.scale, self.cy + (sy + 7) * self.scale)


def face_texels(el, face, tex):
    """Yields (corner points in model space, colour) per texel of one visible face."""
    (x1, y1, z1), (x2, y2, z2) = el["from"], el["to"]
    u0, v0, _, _ = (c * 4 for c in el["faces"][face]["uv"])  # sheet is 64px, uv units are /16
    u0, v0 = int(u0), int(v0)
    if face == "up":
        for i in range(int(x2 - x1)):
            for j in range(int(z2 - z1)):
                x, z = x1 + i, z1 + j
                yield [(x, y2, z), (x + 1, y2, z), (x + 1, y2, z + 1), (x, y2, z + 1)], tex.getpixel((u0 + i, v0 + j))
    elif face == "south":
        for i in range(int(x2 - x1)):
            for j in range(int(y2 - y1)):
                x, y = x1 + i, y2 - j
                yield [(x, y, z2), (x + 1, y, z2), (x + 1, y - 1, z2), (x, y - 1, z2)], tex.getpixel((u0 + i, v0 + j))
    elif face == "east":
        for i in range(int(z2 - z1)):
            for j in range(int(y2 - y1)):
                z, y = z2 - i, y2 - j
                yield [(x2, y, z), (x2, y, z - 1), (x2, y - 1, z - 1), (x2, y - 1, z)], tex.getpixel((u0 + i, v0 + j))


def background(size):
    img = Image.new("RGBA", (size, size))
    px = img.load()
    inner, outer = (92, 52, 128), (20, 12, 34)
    for y in range(size):
        for x in range(size):
            d = min(1.0, math.hypot(x - size / 2, y - size * 0.47) / (size * 0.68))
            t = d ** 1.3
            px[x, y] = tuple(int(inner[k] * (1 - t) + outer[k] * t) for k in range(3)) + (255,)
    return img


def glow(size, center, radius, colour, strength):
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    cx, cy = center
    d.ellipse([cx - radius, cy - radius, cx + radius, cy + radius], fill=colour + (strength,))
    return layer.filter(ImageFilter.GaussianBlur(radius * 0.45))


def sparkles(size, rnd, count, area):
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    x0, y0, x1, y1 = area
    for _ in range(count):
        x, y = rnd.uniform(x0, x1), rnd.uniform(y0, y1)
        r = rnd.uniform(size * 0.004, size * 0.011)
        colour = rnd.choice([(255, 170, 240), (220, 140, 255), (255, 220, 255)])
        d.ellipse([x - r, y - r, x + r, y + r], fill=colour + (rnd.randint(150, 240),))
        if rnd.random() < 0.35:  # little four-point stars
            s = r * 3
            d.line([x - s, y, x + s, y], fill=colour + (160,), width=max(1, int(r / 2)))
            d.line([x, y - s, x, y + s], fill=colour + (160,), width=max(1, int(r / 2)))
    return Image.alpha_composite(layer.filter(ImageFilter.GaussianBlur(size * 0.004)), layer)


def main(size):
    big = size * SS
    rnd = random.Random(11)
    model = json.loads(MODEL.read_text())
    tex = Image.open(TEXTURE).convert("RGBA")
    proj = Projector(big)

    img = background(big)
    img = Image.alpha_composite(img, glow(big, proj(8, 9, 8), big * 0.30, (190, 90, 230), 120))
    img = Image.alpha_composite(img, sparkles(big, rnd, 45, (0, 0, big, big)))

    # Soft shadow under the plinth.
    shadow = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    pts = [proj(0.5, 0, 0.5), proj(15.5, 0, 0.5), proj(15.5, 0, 15.5), proj(0.5, 0, 15.5)]
    sd.polygon(pts, fill=(8, 4, 16, 150))
    img = Image.alpha_composite(img, shadow.filter(ImageFilter.GaussianBlur(big * 0.02)))

    # Painter's algorithm over every visible texel, back to front.
    quads = []
    for el in model["elements"]:
        for face in ("up", "south", "east"):
            for corners, colour in face_texels(el, face, tex):
                cx = sum(p[0] for p in corners) / 4
                cy = sum(p[1] for p in corners) / 4
                cz = sum(p[2] for p in corners) / 4
                quads.append((cx + 2 * TILT * cy + cz, corners, colour, SHADE[face]))  # depth along the view direction (1, 2*TILT, 1)
    quads.sort(key=lambda q: q[0])
    block = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    bd = ImageDraw.Draw(block)
    for _, corners, colour, shade in quads:
        r, g, b, a = colour
        if a == 0:
            continue
        fill = (int(r * shade), int(g * shade), int(b * shade), 255)
        bd.polygon([proj(*p) for p in corners], fill=fill, outline=fill)
    img = Image.alpha_composite(img, block)

    # The floating template: a diamond above the source pool, with its own glow.
    item_center = proj(8, 15.2, 8)
    img = Image.alpha_composite(img, glow(big, item_center, proj.scale * 4, (255, 150, 245), 170))
    item_px = int(proj.scale * 6.5)
    item = load_item("diamond").resize((item_px, item_px), Image.NEAREST)
    img.alpha_composite(item, (int(item_center[0] - item_px / 2), int(item_center[1] - item_px / 2)))

    # Source wisps rising from the pool to the item.
    img = Image.alpha_composite(img, sparkles(big, rnd, 14, (
        proj(5, 9, 11)[0], item_center[1], proj(11, 9, 5)[0], proj(8, 8, 8)[1])))

    OUT.parent.mkdir(parents=True, exist_ok=True)
    img.resize((size, size), Image.LANCZOS).convert("RGB").save(OUT)
    print(f"wrote {OUT.relative_to(ROOT)} ({size}x{size})")


if __name__ == "__main__":
    main(int(sys.argv[1]) if len(sys.argv) > 1 else 1024)
