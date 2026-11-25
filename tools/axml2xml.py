#!/usr/bin/env python3
"""Decompile binary XML to source XML, resolving resource references by name.
resolve(id) -> 'pkg:type/name' callback. Used to rebuild LG resources with aapt2."""
import struct
from arsc import read_pool

ANDROID_NS = 'http://schemas.android.com/apk/res/android'

def fmt_dim(v, frac=False):
    units = ['px', 'dip', 'sp', 'pt', 'in', 'mm'] if not frac else ['%', '%p']
    mant = (v >> 8) * [1.0 / (1 << 0), 1.0 / (1 << 7), 1.0 / (1 << 15), 1.0 / (1 << 23)][(v >> 4) & 3]
    if mant >= (1 << 23):
        mant -= (1 << 24)
    if frac:
        mant *= 100
    s = ('%f' % mant).rstrip('0').rstrip('.')
    return s + units[v & 0xf]

def fmt_value(dt, dv, strings, resolve, refs):
    if dt == 0x01 or dt == 0x07:
        if dv == 0:
            return '@null'
        refs.add(dv)
        return '@' + resolve(dv)
    if dt == 0x02 or dt == 0x08:
        refs.add(dv)
        return '?' + resolve(dv)
    if dt == 0x03:
        return strings[dv]
    if dt == 0x04:
        return repr(struct.unpack('<f', struct.pack('<I', dv))[0])
    if dt == 0x05:
        return fmt_dim(dv)
    if dt == 0x06:
        return fmt_dim(dv, True)
    if dt == 0x10:
        return str(struct.unpack('<i', struct.pack('<I', dv))[0])
    if dt == 0x11:
        return '0x%x' % dv
    if dt == 0x12:
        return 'true' if dv else 'false'
    if 0x1c <= dt <= 0x1f:
        return '#%08x' % dv
    return '0x%x' % dv

def esc(s):
    return s.replace('&', '&amp;').replace('<', '&lt;').replace('"', '&quot;').replace('\n', '\\n')

def decompile(d, resolve, attr_name, enum_fmt=None):
    """attr_name(id) -> 'android:foo' or 'app:foo' for attribute ids."""
    off = 8
    S = []
    rmap = []
    out = ['<?xml version="1.0" encoding="utf-8"?>']
    depth = 0
    refs = set()
    first = True
    while off < len(d):
        t, h, s = struct.unpack_from('<HHI', d, off)
        if t == 0x0001:
            S = read_pool(d, off)
        elif t == 0x0180:
            rmap = list(struct.unpack_from('<%dI' % ((s - h) // 4), d, off + h))
        elif t == 0x0102:
            ns, name, aoff, asz, acnt = struct.unpack_from('<IIHHH', d, off + 16)
            attrs = []
            p = off + 16 + aoff
            for i in range(acnt):
                ans, an, araw, _, _, dtype, dval = struct.unpack_from('<IIIHBBI', d, p + i * asz)
                if an < len(rmap) and rmap[an]:
                    refs.add(rmap[an])
                    k = attr_name(rmap[an])
                else:
                    k = S[an]
                ev = enum_fmt(rmap[an], dtype, dval) if (enum_fmt and an < len(rmap) and rmap[an]) else None
                if araw != 0xffffffff and dtype == 0x03:
                    v = S[araw]
                elif ev is not None:
                    v = ev
                else:
                    v = fmt_value(dtype, dval, S, resolve, refs)
                attrs.append('%s="%s"' % (k, esc(v)))
            tag = S[name]
            nsdecl = ' xmlns:android="%s" xmlns:app="http://schemas.android.com/apk/res-auto"' % ANDROID_NS if first else ''
            first = False
            out.append('  ' * depth + '<' + tag + nsdecl + ('\n' + '  ' * (depth + 2) if attrs else '') +
                       ('\n' + '  ' * (depth + 2)).join(attrs) + '>')
            depth += 1
        elif t == 0x0103:
            depth -= 1
            ns, name = struct.unpack_from('<II', d, off + 16)
            out.append('  ' * depth + '</' + S[name] + '>')
        elif t == 0x0104:
            idx = struct.unpack_from('<I', d, off + 16)[0]
            out.append('  ' * depth + esc(S[idx]))
        off += s
    return '\n'.join(out) + '\n', refs
