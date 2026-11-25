#!/usr/bin/env python3
"""Minimal resources.arsc reader: resolve resource ids to type/name, list entries."""
import struct, zipfile, sys

def read_pool(d, off):
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
                n = ((n & 0x7f) << 8) | d[p + 1]; p += 2
            else:
                p += 1
            out.append(d[p:p + n].decode('utf8', 'replace'))
        else:
            n = struct.unpack_from('<H', d, p)[0]; p += 2
            if n & 0x8000:
                n = ((n & 0x7fff) << 16) | struct.unpack_from('<H', d, p)[0]; p += 2
            out.append(d[p:p + 2 * n].decode('utf-16le', 'replace'))
    return out

class Table:
    def __init__(self, data):
        self.d = d = data
        hsz = struct.unpack_from('<H', d, 2)[0]
        off = hsz
        self.values = None
        self.pkgs = {}
        while off < len(d):
            t, h, s = struct.unpack_from('<HHI', d, off)
            if t == 0x0001 and self.values is None:
                self.values = read_pool(d, off)
            elif t == 0x0200:
                self._pkg(off, h, s)
            off += s

    def _pkg(self, off, h, s):
        d = self.d
        pid = struct.unpack_from('<I', d, off + 8)[0]
        name = d[off + 12:off + 268].decode('utf-16le').split('\0')[0]
        tstr, _, kstr, _ = struct.unpack_from('<IIII', d, off + 268)
        types = read_pool(d, off + tstr)
        keys = read_pool(d, off + kstr)
        ent = self.pkgs[pid][1] if pid in self.pkgs else {}  # id -> (type, key, [ (config_bytes, value) ])
        p = off + h
        end = off + s
        while p < end:
            t, hh, ss = struct.unpack_from('<HHI', d, p)
            if t == 0x0201:  # type
                tid = d[p + 8]
                flags = d[p + 9]
                cnt, estart = struct.unpack_from('<II', d, p + 12)
                cfg = d[p + 20:p + hh]
                sparse = flags & 0x01
                offs16 = flags & 0x02
                eoffs = []
                if sparse:
                    for i in range(cnt):
                        idx, o = struct.unpack_from('<HH', d, p + hh + 4 * i)
                        eoffs.append((idx, o * 4))
                elif offs16:
                    for i in range(cnt):
                        o = struct.unpack_from('<H', d, p + hh + 2 * i)[0]
                        if o != 0xffff:
                            eoffs.append((i, o * 4))
                else:
                    for i in range(cnt):
                        o = struct.unpack_from('<I', d, p + hh + 4 * i)[0]
                        if o != 0xffffffff:
                            eoffs.append((i, o))
                for idx, o in eoffs:
                    e = p + estart + o
                    esz, eflags, kidx = struct.unpack_from('<HHI', d, e)
                    rid = (pid << 24) | (tid << 16) | idx
                    if eflags & 0x8:  # FLAG_COMPACT (Android 14+): key=esz, type in flags>>8
                        rec = ent.setdefault(rid, [types[tid - 1], keys[esz], []])
                        rec[2].append((cfg, ('val', eflags >> 8, kidx)))
                        continue
                    rec = ent.setdefault(rid, [types[tid - 1], keys[kidx], []])
                    if eflags & 0x1:  # complex
                        parent, n = struct.unpack_from('<II', d, e + 8)
                        items = []
                        for j in range(n):
                            nm, _, _, dt, dv = struct.unpack_from('<IHBBI', d, e + 16 + 12 * j)
                            items.append((nm, dt, dv))
                        rec[2].append((cfg, ('bag', parent, items)))
                    else:
                        _, _, dt, dv = struct.unpack_from('<HBBI', d, e + 8)
                        rec[2].append((cfg, ('val', dt, dv)))
            p += ss
        self.pkgs[pid] = (name, ent)

    def name(self, rid):
        pkg = self.pkgs.get(rid >> 24)
        if not pkg:
            return None
        e = pkg[1].get(rid)
        return '%s:%s/%s' % (pkg[0], e[0], e[1]) if e else None

def load_apk(path):
    return Table(zipfile.ZipFile(path).read('resources.arsc'))

if __name__ == '__main__':
    t = load_apk(sys.argv[1])
    for a in sys.argv[2:]:
        print(a, t.name(int(a, 16)))
