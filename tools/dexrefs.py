#!/usr/bin/env python3
"""List external (not defined in these dex files) class/method/field refs.
Usage: dexrefs.py out_prefix classes*.dex
Writes <prefix>_defined.txt, <prefix>_ext_methods.txt, <prefix>_ext_fields.txt, <prefix>_strings.txt
"""
import struct, sys

def uleb(d, p):
    r = s = 0
    while True:
        b = d[p]; p += 1
        r |= (b & 0x7f) << s; s += 7
        if not b & 0x80:
            return r, p

def parse(path):
    d = open(path, 'rb').read()
    h = struct.unpack_from('<20I', d, 0x38)
    (str_sz, str_off, typ_sz, typ_off, pro_sz, pro_off, fld_sz, fld_off,
     mth_sz, mth_off, cls_sz, cls_off) = h[0:12]
    S = []
    for i in range(str_sz):
        o = struct.unpack_from('<I', d, str_off + 4 * i)[0]
        _, p = uleb(d, o)
        e = d.index(b'\0', p)
        S.append(d[p:e].decode('utf8', 'replace'))
    T = [S[struct.unpack_from('<I', d, typ_off + 4 * i)[0]] for i in range(typ_sz)]
    P = []
    for i in range(pro_sz):
        _, ret, poff = struct.unpack_from('<III', d, pro_off + 12 * i)
        params = []
        if poff:
            n = struct.unpack_from('<I', d, poff)[0]
            params = [T[x] for x in struct.unpack_from('<%dH' % n, d, poff + 4)]
        P.append('(' + ''.join(params) + ')' + T[ret])
    F = []
    for i in range(fld_sz):
        c, t, n = struct.unpack_from('<HHI', d, fld_off + 8 * i)
        F.append((T[c], S[n], T[t]))
    M = []
    for i in range(mth_sz):
        c, pr, n = struct.unpack_from('<HHI', d, mth_off + 8 * i)
        M.append((T[c], S[n], P[pr]))
    D = set(T[struct.unpack_from('<I', d, cls_off + 32 * i)[0]] for i in range(cls_sz))
    return S, D, M, F

def main():
    prefix = sys.argv[1]
    allS, allD, allM, allF = set(), set(), set(), set()
    for f in sys.argv[2:]:
        S, D, M, F = parse(f)
        allS |= set(S); allD |= D; allM |= set(M); allF |= set(F)
    ext_m = sorted(m for m in allM if m[0] not in allD)
    ext_f = sorted(f for f in allF if f[0] not in allD)
    open(prefix + '_defined.txt', 'w').write('\n'.join(sorted(allD)) + '\n')
    open(prefix + '_ext_methods.txt', 'w').write('\n'.join('%s->%s%s' % m for m in ext_m) + '\n')
    open(prefix + '_ext_fields.txt', 'w').write('\n'.join('%s->%s:%s' % f for f in ext_f) + '\n')
    open(prefix + '_strings.txt', 'w').write('\n'.join(sorted(allS)) + '\n')
    print('defined classes', len(allD), 'ext methods', len(ext_m), 'ext fields', len(ext_f))

main()
