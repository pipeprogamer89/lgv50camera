#!/usr/bin/env python3
"""Compute the closure of com.lge (0x02) resources needed by the camera app."""
import sys, zipfile
sys.path.insert(0, '/home/julian/v50-camera/tools')
import arsc
from axml2xml import decompile

LGRES = '/home/julian/v50-camera/p1/sys/system/lge-res/lge-res.apk'
LGFW = '/home/julian/v50-camera/p1/sys/system/framework-res.apk'

lg = arsc.load_apk(LGRES)
fw = arsc.load_apk(LGFW)
z = zipfile.ZipFile(LGRES)
ent = lg.pkgs[2][1]
byname = {(e[0], e[1]): rid for rid, e in ent.items()}

def resolve(rid):
    t = lg if rid >> 24 == 2 else fw
    n = t.name(rid)
    if n is None:
        return 'UNRESOLVED_0x%08x' % rid
    pkg, rest = n.split(':', 1)
    return ('android:' if pkg == 'android' else 'com.lge:') + rest

def attr_name(rid):
    n = resolve(rid)
    if n.startswith('android:attr/'):
        return 'android:' + n.split('/', 1)[1]
    return 'lge:' + n.split('/', 1)[1]

def enum_fmt(attr_rid, dt, dv):
    if dt not in (0x10, 0x11):
        return None
    t = lg if attr_rid >> 24 == 2 else fw
    pkg = t.pkgs.get(attr_rid >> 24)
    e = pkg[1].get(attr_rid) if pkg else None
    if not e or e[0] != 'attr':
        return None
    for cfg, v in e[2]:
        if v[0] != 'bag':
            continue
        items = v[2]
        fmt = next((x[2] for x in items if x[0] == 0x01000000), 0)
        names = [((t.name(nm) or '/').split('/', 1)[1], val) for nm, _, val in items if nm >> 24 != 0x01 or nm >= 0x01010000]
        names = [(n, val) for n, val in names if n]
        if fmt & 0x10000:  # enum
            for n, val in names:
                if val == dv:
                    return n
            return None if fmt & 0x4 else str(dv)
        if fmt & 0x20000:  # flags
            if dv == 0:
                z = [n for n, val in names if val == 0]
                return z[0] if z else None
            out, rem = [], dv
            for n, val in sorted(names, key=lambda x: -bin(x[1]).count('1')):
                if val and (rem & val) == val:
                    out.append(n); rem &= ~val
            return '|'.join(out) if rem == 0 and out else None
    return None

def closure(seeds):
    todo = list(seeds)
    seen = set()
    files = {}
    while todo:
        rid = todo.pop()
        if rid in seen or rid >> 24 != 2 or rid not in ent:
            continue
        seen.add(rid)
        refs = set()
        for cfg, v in ent[rid][2]:
            if v[0] == 'val':
                if v[1] in (1, 2, 7, 8):
                    refs.add(v[2])
                elif v[1] == 3 and lg.values[v[2]].startswith('res/'):
                    path = lg.values[v[2]]
                    if path.endswith('.xml'):
                        src, r = decompile(z.read(path), resolve, attr_name, enum_fmt)
                        files[path] = src
                        refs |= r
                    else:
                        files[path] = None
            else:
                _, parent, items = v
                if parent:
                    refs.add(parent)
                for nm, dt, dv in items:
                    refs.add(nm)
                    if dt in (1, 2, 7, 8):
                        refs.add(dv)
        todo.extend(r for r in refs if r >> 24 == 2)
    return seen, files

if __name__ == '__main__':
    seeds = [int(x, 16) for x in sys.argv[1:]]
    seeds += [byname[k] for k in [('layout', 'dialog_c_1'), ('layout', 'dialog_init_guide'),
                                   ('string', 'config_app_tier'), ('dimen', 'mini_activity_height')]]
    seen, files = closure(seeds)
    import collections
    print('resources:', len(seen), collections.Counter(ent[r][0] for r in seen))
    print('files:', len(files))
    unresolved = set()
    for p, src in files.items():
        if src and 'UNRESOLVED' in src:
            unresolved.add(p)
    print('files with unresolved refs:', sorted(unresolved))
    tags = collections.Counter()
    import re
    for p, src in files.items():
        if src:
            for m in re.finditer(r'^\s*<([A-Za-z0-9_.]+)', src, re.M):
                tags[m.group(1)] += 1
    print('xml tags:', dict(tags))
