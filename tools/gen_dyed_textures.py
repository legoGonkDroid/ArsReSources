#!/usr/bin/env python3
"""Generates the 16 dyed Source Vessel sheets that Ars Deco swaps in when the vessel is dyed.

Usage: python3 tools/gen_dyed_textures.py <path to ars_deco jar> <path to ars_nouveau jar>

Ars Deco recolours each block with a fixed colour-to-colour mapping per dye (purple stone/source tones
become the dye's tones; gold is kept). The vessel's sheet only uses Arcane Pedestal colours, so the mapping
is read from Ars Deco's own dyed Arcane Pedestal textures and applied pixel for pixel.
Output: src/main/resources/assets/ars_deco/textures/block/source_vessel/<color>.png
"""
import io
import sys
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
BASE = ROOT / "src/main/resources/assets/resources/textures/block/source_vessel.png"
OUT = ROOT / "src/main/resources/assets/ars_deco/textures/block/source_vessel"
COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
          "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"]


def read_png(jar, name):
    with zipfile.ZipFile(jar) as z:
        return Image.open(io.BytesIO(z.read(name))).convert("RGBA")


def main(deco_jar, an_jar):
    original = read_png(an_jar, "assets/ars_nouveau/textures/block/arcane_pedestal.png")
    base = Image.open(BASE).convert("RGBA")
    OUT.mkdir(parents=True, exist_ok=True)
    for color in COLORS:
        dyed = read_png(deco_jar, f"assets/ars_deco/textures/block/arcane_pedestal/{color}.png")
        mapping = {}
        for src, dst in zip(original.getdata(), dyed.getdata()):
            if src[3]:
                mapping[src[:3]] = dst[:3]
        missing = {p[:3] for p in base.getdata() if p[3] and p[:3] not in mapping}
        if missing:
            sys.exit(f"{color}: colours not in Ars Deco's pedestal mapping: {sorted(missing)}")
        out = Image.new("RGBA", base.size)
        out.putdata([mapping[p[:3]] + (p[3],) if p[3] else p for p in base.getdata()])
        out.save(OUT / f"{color}.png")
    print(f"wrote {len(COLORS)} sheets to {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])
