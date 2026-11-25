#!/usr/bin/env python3
"""Re-parent app styles whose parent is an LG framework style (package 0x02) to the closest
AOSP style, editing resources.arsc in place (the parent is a plain uint32 in ResTable_map_entry).
Usage: patch_arsc.py in.arsc out.arsc"""
import struct, sys

# LG parent style -> AOSP public style (ids are stable public framework ids)
REPARENT = {
    0x020a01c2: 'Theme.Material.Light',          # com.lge:style/Theme.LGE.White
    0x020a01ac: 'Theme.Material.Light',          # com.lge:style/Theme.LGE.Default
    0x020a019c: 'Widget.Holo.Light.ActionBar',   # com.lge:style/Widget.Holo.Light.ActionBar
}
FWRES = '/mnt/Datos/backup/Archivos/lineage/out/target/product/flashlmdd/system/framework/framework-res.apk'

def aosp_style_ids():
    sys.path.insert(0, '/home/julian/v50-camera/tools')
    import arsc
    t = arsc.load_apk(FWRES)
    return {e[1]: rid for rid, e in t.pkgs[1][1].items() if e[0] == 'style'}

def main(src, dst):
    ids = aosp_style_ids()
    mapping = {k: ids[v] for k, v in REPARENT.items()}
    d = bytearray(open(src, 'rb').read())
    off = struct.unpack_from('<H', d, 2)[0]
    patched = 0
    while off < len(d):
        t, h, s = struct.unpack_from('<HHI', d, off)
        if t == 0x0200:
            p, end = off + h, off + s
            while p < end:
                tt, hh, ss = struct.unpack_from('<HHI', d, p)
                if tt == 0x0201:
                    flags = d[p + 9]
                    cnt, estart = struct.unpack_from('<II', d, p + 12)
                    offs = []
                    for i in range(cnt):
                        if flags & 0x01:
                            offs.append(struct.unpack_from('<HH', d, p + hh + 4 * i)[1] * 4)
                        elif flags & 0x02:
                            o = struct.unpack_from('<H', d, p + hh + 2 * i)[0]
                            if o != 0xffff: offs.append(o * 4)
                        else:
                            o = struct.unpack_from('<I', d, p + hh + 4 * i)[0]
                            if o != 0xffffffff: offs.append(o)
                    for o in offs:
                        e = p + estart + o
                        esz, eflags = struct.unpack_from('<HH', d, e)
                        if eflags & 0x1 and not eflags & 0x8:   # complex (bag) entry
                            parent = struct.unpack_from('<I', d, e + 8)[0]
                            if parent in mapping:
                                struct.pack_into('<I', d, e + 8, mapping[parent])
                                patched += 1
                p += ss
        off += s
    open(dst, 'wb').write(d)
    print('re-parented %d style entries -> %s' % (patched, {hex(k): hex(v) for k, v in mapping.items()}))

main(sys.argv[1], sys.argv[2])
