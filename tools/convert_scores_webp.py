#!/usr/bin/env python3
"""Convert the bundled score pages to lossless WebP, pixel for pixel (1.6.0).

Usage: python3 tools/convert_scores_webp.py [--check] [--jobs N]
  (no option)  every hymnchtv/src/main/assets/lyrics_*_score/*.png (some are JPEG data under a .png name) becomes
               <name>.webp (cwebp -lossless -z 8 -exact -metadata none). The decoded pixels of each WebP must equal the
               source's (Pillow, as RGBA) before any .png is deleted; the first mismatch stops the run with nothing deleted.
  --check      verify only: no .png is left and every .webp is lossless (VP8L).
Needs cwebp (brew install webp) and Pillow with WebP support.
"""
import argparse
import concurrent.futures
import subprocess
import sys
import tempfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "hymnchtv/src/main/assets"


def score_dirs():
    return sorted(p for p in ASSETS.glob("lyrics_*_score") if p.is_dir())


def pixels(path: Path):
    with Image.open(path) as image:
        return image.size, image.convert("RGBA").tobytes()


def convert(png: Path):
    webp = png.with_suffix(".webp")
    with tempfile.TemporaryDirectory() as tmp:
        source = png
        with Image.open(png) as image:
            if image.format != "PNG":
                # JPEG data under a .png name: encode the pixels Pillow decodes, so the check below is exact
                source = Path(tmp) / "source.png"
                image.convert("RGB").save(source)
        subprocess.run(["cwebp", "-quiet", "-lossless", "-z", "8", "-exact", "-metadata", "none",
                        str(source), "-o", str(webp)], check=True)
    if pixels(png) != pixels(webp):
        webp.unlink(missing_ok=True)
        raise RuntimeError(f"pixel mismatch: {png}")
    return png, png.stat().st_size, webp.stat().st_size


def check():
    problems = []
    for directory in score_dirs():
        problems += [f"left over: {p}" for p in directory.glob("*.png")]
        for webp in directory.glob("*.webp"):
            head = webp.read_bytes()[:16]
            if head[:4] != b"RIFF" or head[8:12] != b"WEBP" or head[12:16] != b"VP8L":
                problems.append(f"not lossless WebP: {webp}")
    for problem in problems:
        print(problem)
    print("OK" if not problems else f"{len(problems)} problem(s)")
    return 0 if not problems else 1


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--jobs", type=int, default=4)
    args = parser.parse_args()
    if args.check:
        return check()
    pngs = [p for d in score_dirs() for p in sorted(d.glob("*.png"))]
    if not pngs:
        print("nothing to convert")
        return check()
    before = after = 0
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.jobs) as pool:
        for done, (png, old, new) in enumerate(pool.map(convert, pngs), 1):
            before += old
            after += new
            if done % 200 == 0:
                print(f"{done}/{len(pngs)}", flush=True)
    for png in pngs:
        png.unlink()
    print(f"{len(pngs)} pages: {before / 1048576:.1f} MB -> {after / 1048576:.1f} MB ({100 * after / before:.0f}%)")
    return check()


if __name__ == "__main__":
    sys.exit(main())
