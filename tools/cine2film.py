#!/usr/bin/env python3
"""Convert LG Cine Video LUTs to LG film-filter LUTs.

LG Cine Video applies /vendor/etc/camera/cinema/N_cine-max.dat (64^3 cube, RGBA8 entries,
indexed [V][U][Y] with Y fastest, output = Y,U,V full range) inside a 10-bit HDR10 usecase
that LineageOS' gralloc cannot back. The normal video usecase instead applies film LUTs
/vendor/etc/camera/film/<name> (36^3 cube, RGB8 entries, indexed [B][G][R], grid i*255/35).
This bakes each cine look into a film LUT so Cine Video can run on the normal usecase.

Usage: cine2film.py <cinema_dir> <out_dir>
"""
import os
import sys

import numpy as np

N_FILM = 36


def rgb_to_yuv(rgb):
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    y = 0.299 * r + 0.587 * g + 0.114 * b
    u = -0.168736 * r - 0.331264 * g + 0.5 * b + 128
    v = 0.5 * r - 0.418688 * g - 0.081312 * b + 128
    return np.stack([y, u, v], -1)


def yuv_to_rgb(yuv):
    y, u, v = yuv[..., 0], yuv[..., 1] - 128, yuv[..., 2] - 128
    return np.stack([y + 1.402 * v, y - 0.344136 * u - 0.714136 * v, y + 1.772 * u], -1)


def sample(cube, yuv):
    """Trilinear lookup in cube[v][u][y][3] (float), yuv in 0..255."""
    n = cube.shape[0]
    p = np.clip(yuv, 0, 255) * (n - 1) / 255.0
    i0 = np.floor(p).astype(int)
    i0 = np.minimum(i0, n - 2)
    t = p - i0
    out = 0
    for dy in (0, 1):
        for du in (0, 1):
            for dv in (0, 1):
                w = ((t[..., 0] if dy else 1 - t[..., 0]) *
                     (t[..., 1] if du else 1 - t[..., 1]) *
                     (t[..., 2] if dv else 1 - t[..., 2]))
                out = out + w[..., None] * cube[i0[..., 2] + dv, i0[..., 1] + du, i0[..., 0] + dy]
    return out


def convert(path):
    cube = np.fromfile(path, np.uint8).reshape(64, 64, 64, 4)[..., :3].astype(np.float64)
    g = np.arange(N_FILM) * 255.0 / (N_FILM - 1)
    b, gg, r = np.meshgrid(g, g, g, indexing='ij')          # film order [B][G][R]
    rgb = np.stack([r, gg, b], -1)
    out = yuv_to_rgb(sample(cube, rgb_to_yuv(rgb)))
    return np.clip(np.rint(out), 0, 255).astype(np.uint8)


def main():
    src, dst = sys.argv[1], sys.argv[2]
    os.makedirs(dst, exist_ok=True)
    for name in sorted(os.listdir(src)):
        if not name.endswith('_cine-max.dat'):
            continue
        n = int(name.split('_')[0])
        lut = convert(os.path.join(src, name))
        lut.tofile(os.path.join(dst, 'cine_%d.dat' % n))
        mid = lut[18, 18, 18].tolist()
        print('cine_%d.dat  mid-gray -> %s  black -> %s  white -> %s'
              % (n, mid, lut[0, 0, 0].tolist(), lut[-1, -1, -1].tolist()))


if __name__ == '__main__':
    main()
