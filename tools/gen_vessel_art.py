#!/usr/bin/env python3
"""Generates the Source Vessel's texture sheet and block model.

Usage: python3 tools/gen_vessel_art.py

The sheet only uses colours from the Arcane Pedestal's palette, so Ars Deco's colour mapping can be
reused exactly for the dyed variants (tools/gen_dyed_textures.py). The sheet is 64x64 and holds four
16x16 material tiles; every model face samples a sub-rectangle of a tile at 1 texel per model pixel.
"""
import json
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/resources"
TEXTURE = ASSETS / "textures/block/source_vessel.png"
MODEL = ASSETS / "models/block/source_vessel.json"

# Arcane Pedestal palette (ars_nouveau:block/arcane_pedestal).
STONE = [(49, 43, 77), (56, 45, 80), (65, 51, 86), (75, 59, 93)]
STONE_HI = (104, 75, 109)
SOURCE = [(104, 64, 117), (117, 71, 121), (132, 83, 127), (142, 96, 130), (159, 115, 140)]
GOLD = [(255, 254, 208), (255, 253, 156), (237, 229, 116), (226, 211, 94), (220, 197, 69)]

SIZE = 64
TILES = {  # material -> top-left of its 16x16 tile, in sheet pixels
    "stone": (0, 0),
    "brick": (16, 0),
    "gold": (32, 0),
    "source": (48, 0),
}


def paint(img):
    rnd = random.Random(7)
    px = img.load()

    # Mottled sourcestone with a darker 1px rim, like Ars Nouveau's stone blocks.
    ox, oy = TILES["stone"]
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            n = (math.sin(x * 1.7 + y * 0.9) + math.cos(x * 0.6 - y * 1.3)) / 2 + rnd.uniform(-0.45, 0.45)
            i = 0 if edge else min(3, max(1, int((n + 1) * 2)))
            c = STONE[i]
            if not edge and rnd.random() < 0.05:
                c = STONE_HI
            px[ox + x, oy + y] = c + (255,)

    # Smooth sourcestone bricks: 8x4 courses, offset every other row.
    ox, oy = TILES["brick"]
    for y in range(16):
        for x in range(16):
            row = y // 4
            bx = (x + (4 if row % 2 else 0)) % 8
            mortar = y % 4 == 3 or bx == 7
            if mortar:
                c = STONE[1]
            elif y % 4 == 0 or bx == 0:
                c = STONE_HI
            else:
                c = STONE[3] if rnd.random() < 0.8 else STONE[2]
            px[ox + x, oy + y] = c + (255,)

    # Gold trim: bright top edge shading down, with small engraved diamonds.
    ox, oy = TILES["gold"]
    for y in range(16):
        for x in range(16):
            i = min(4, y % 4 + (1 if (x + y) % 5 == 0 else 0))
            if y % 4 == 2 and x % 4 == 1:
                i = 4
            px[ox + x, oy + y] = GOLD[i] + (255,)

    # Swirling source pool.
    ox, oy = TILES["source"]
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            a = math.atan2(dy, dx)
            v = (math.sin(r * 1.1 - a * 2) + 1) / 2
            i = min(4, int(v * 5))
            if r < 2:
                i = 4
            px[ox + x, oy + y] = SOURCE[i] + (255,)


def uv(material, w, h, sx=0, sy=0):
    """UV rect (model units, sheet = 16) for a w x h face sampled from a material tile."""
    tx, ty = TILES[material]
    sx = min(sx, 16 - w)
    sy = min(sy, 16 - h)
    u0, v0 = (tx + sx) / 4, (ty + sy) / 4
    return [u0, v0, u0 + w / 4, v0 + h / 4]


def element(name, frm, to, side, top=None, bottom=None):
    """A cuboid; side/top/bottom are material names (None = face omitted)."""
    (x1, y1, z1), (x2, y2, z2) = frm, to
    wx, hy, dz = x2 - x1, y2 - y1, z2 - z1
    top = top or side
    bottom = bottom or side
    faces = {
        "north": {"uv": uv(side, wx, hy, int(x1), int(16 - y2)), "texture": "#vessel"},
        "south": {"uv": uv(side, wx, hy, int(x1), int(16 - y2)), "texture": "#vessel"},
        "east": {"uv": uv(side, dz, hy, int(z1), int(16 - y2)), "texture": "#vessel"},
        "west": {"uv": uv(side, dz, hy, int(z1), int(16 - y2)), "texture": "#vessel"},
        "up": {"uv": uv(top, wx, dz, int(x1), int(z1)), "texture": "#vessel"},
        "down": {"uv": uv(bottom, wx, dz, int(x1), int(z1)), "texture": "#vessel"},
    }
    return {"name": name, "from": list(frm), "to": list(to), "faces": faces}


def model():
    els = [
        element("plinth", (2, 0, 2), (14, 2, 14), "stone"),
        element("plinth_trim", (3, 2, 3), (13, 3, 13), "gold"),
        element("stem", (6, 3, 6), (10, 6, 10), "brick"),
        element("bowl_floor", (3, 6, 3), (13, 8, 13), "brick", top="source"),
        element("wall_north", (3, 8, 3), (13, 10, 4), "brick"),
        element("wall_south", (3, 8, 12), (13, 10, 13), "brick"),
        element("wall_west", (3, 8, 4), (4, 10, 12), "brick"),
        element("wall_east", (12, 8, 4), (13, 10, 12), "brick"),
        element("rim_north", (3, 10, 3), (13, 11, 4), "gold"),
        element("rim_south", (3, 10, 12), (13, 11, 13), "gold"),
        element("rim_west", (3, 10, 4), (4, 11, 12), "gold"),
        element("rim_east", (12, 10, 4), (13, 11, 12), "gold"),
    ]
    for cx, cz in ((3, 3), (12, 3), (3, 12), (12, 12)):
        els.append(element(f"prong_{cx}_{cz}", (cx, 11, cz), (cx + 1, 14, cz + 1), "gold"))
    return {
        "credit": "Ars ReSources - generated by tools/gen_vessel_art.py",
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout",
        "texture_size": [SIZE, SIZE],
        "textures": {
            "vessel": "resources:block/source_vessel",
            "particle": "resources:block/source_vessel",
        },
        "elements": els,
    }


def main():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    paint(img)
    TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    img.save(TEXTURE)
    MODEL.parent.mkdir(parents=True, exist_ok=True)
    MODEL.write_text(json.dumps(model(), indent=2) + "\n")
    print(f"wrote {TEXTURE.relative_to(ROOT)} and {MODEL.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
