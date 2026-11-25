#!/usr/bin/env python3
"""Minimal binary AndroidManifest.xml (AXML) to text dumper."""
import struct, sys

def strpool(d, off):
    _, hsz, csz, cnt, scnt, flags, sstart, _ = struct.unpack_from('<HHIIIIII', d, off)
    utf8 = flags & 0x100
    offs = struct.unpack_from('<%dI' % cnt, d, off + hsz)
    base = off + sstart
    out = []
    for o in offs:
        p = base + o
        if utf8:
            n = d[p]; p += 2 if n & 0x80 else 1
            n = d[p]
            if n & 0x80:
                n = ((n & 0x7f) << 8) | d[p+1]; p += 2
            else:
                p += 1
            out.append(d[p:p+n].decode('utf8', 'replace'))
        else:
            n = struct.unpack_from('<H', d, p)[0]; p += 2
            if n & 0x8000:
                n = ((n & 0x7fff) << 16) | struct.unpack_from('<H', d, p)[0]; p += 2
            out.append(d[p:p+2*n].decode('utf-16le', 'replace'))
    return out, off + csz

def main(path):
    d = open(path, 'rb').read()
    off = 8
    S = []
    depth = 0
    while off < len(d):
        typ, hsz, csz = struct.unpack_from('<HHI', d, off)
        if typ == 0x0001:
            S, _ = strpool(d, off)
        elif typ == 0x0102:
            ns, name, aoff, asz, acnt = struct.unpack_from('<IIHHH', d, off + 16)
            attrs = []
            p = off + 16 + aoff
            for i in range(acnt):
                ans, an, araw, _, _, dtype, dval = struct.unpack_from('<IIIHBBI', d, p + i * asz)
                k = S[an] if an < len(S) else '?'
                if araw != 0xffffffff:
                    v = S[araw]
                elif dtype == 0x03:
                    v = S[dval]
                elif dtype == 0x12:
                    v = 'true' if dval else 'false'
                elif dtype == 0x01:
                    v = '@0x%08x' % dval
                else:
                    v = str(dval) if dtype == 0x10 else '0x%x' % dval
                attrs.append('%s="%s"' % (k, v))
            print('  ' * depth + '<' + S[name] + (' ' + ' '.join(attrs) if attrs else '') + '>')
            depth += 1
        elif typ == 0x0103:
            depth -= 1
        off += csz

main(sys.argv[1])
