#!/usr/bin/env python3
"""Generate an aapt2 source project with the subset of com.lge (0x02) resources
the V50 camera needs, preserving the original resource IDs (stable-ids)."""
import os, re, struct, sys, shutil, zipfile, collections
sys.path.insert(0, '/home/julian/v50-camera/tools')
import lgres_closure as C
from axml2xml import fmt_value, esc

OUT = sys.argv[1]
SEEDS = [int(x, 16) for x in sys.argv[2:]]
SEEDS += [C.byname[k] for k in [('layout', 'dialog_c_1'), ('layout', 'dialog_init_guide'),
                                 ('string', 'config_app_tier'), ('dimen', 'mini_activity_height')]]

lg, ent = C.lg, C.ent

# LG base styles the app inherits from. They are synthesized with an AOSP parent and only the
# LG theme attributes that the included resources reference (the full LG theme pulls ~1400
# resources and Android 11 private references).
SYNTH_STYLES = {
    'Theme.LGE.Default': '@android:style/Theme.Material.Light',
    'Theme.LGE.White': '@style/Theme.LGE.Default',
    'Widget.Holo.Light.ActionBar': '@android:style/Widget.Holo.Light.ActionBar',
}
SYNTH_IDS = {C.byname[('style', n)]: n for n in SYNTH_STYLES}

def used_lg_attrs(seen, files):
    out = set()
    for src in files.values():
        if src:
            out |= set(re.findall(r'\?com\.lge:attr/(\w+)', src))
    for r in seen:
        for cfg, v in ent[r][2]:
            if v[0] == 'bag':
                for nm, dt, dv in v[2]:
                    if dt in (2, 8) and dv >> 24 == 2:
                        out.add(ent[dv][1])
            elif v[1] in (2, 8) and v[2] >> 24 == 2:
                out.add(ent[v[2]][1])
    return out

def theme_items(name, attrs):
    items = []
    for cfg, v in ent[C.byname[('style', name)]][2]:
        if v[0] == 'bag' and cfg.strip(b'\0')[4:] == b'':
            for nm, dt, dv in v[2]:
                if nm >> 24 == 2 and ent.get(nm, [None, None])[1] in attrs:
                    items.append((nm, dt, dv))
    return items

seeds = list(SEEDS)
while True:
    seen, files = C.closure(seeds)
    attrs = used_lg_attrs(seen, files)
    extra = [nm for n in SYNTH_STYLES for nm, dt, dv in theme_items(n, attrs)]
    extra += [dv for n in SYNTH_STYLES for nm, dt, dv in theme_items(n, attrs) if dt in (1, 2, 7, 8)]
    new = [x for x in extra if x not in seen]
    if not new:
        break
    seeds += new
for sid in SYNTH_IDS:
    seen.discard(sid)

def qualifiers(cfg):
    q = []
    cfg = cfg + b'\0' * max(0, 64 - len(cfg))   # cfg starts with the 4-byte size field
    lang, country = cfg[8:10], cfg[10:12]
    if lang != b'\0\0':
        if lang[0] & 0x80:
            raise SystemExit('packed 3-letter locale not supported')
        q.append(lang.decode() + ('-r' + country.decode() if country != b'\0\0' else ''))
    screenLayout = cfg[28]
    if screenLayout & 0xc0 == 0x80:
        q.append('ldrtl')
    elif screenLayout & 0xc0 == 0x40:
        q.append('ldltr')
    sw = struct.unpack_from('<H', cfg, 30)[0]
    w = struct.unpack_from('<H', cfg, 32)[0]
    h = struct.unpack_from('<H', cfg, 34)[0]
    if sw: q.append('sw%ddp' % sw)
    if w: q.append('w%ddp' % w)
    if h: q.append('h%ddp' % h)
    orient = cfg[12]
    if orient == 1: q.append('port')
    elif orient == 2: q.append('land')
    ui = cfg[29]
    if ui & 0x30 == 0x20: q.append('night')
    elif ui & 0x30 == 0x10: q.append('notnight')
    dens = struct.unpack_from('<H', cfg, 14)[0]
    dmap = {120: 'ldpi', 160: 'mdpi', 213: 'tvdpi', 240: 'hdpi', 320: 'xhdpi', 480: 'xxhdpi',
            640: 'xxxhdpi', 0xfffe: 'anydpi', 0xffff: 'nodpi'}
    if dens: q.append(dmap.get(dens, '%ddpi' % dens))
    sdk = struct.unpack_from('<H', cfg, 24)[0]
    if sdk: q.append('v%d' % sdk)
    return '-'.join(q)

def ref_name(rid):
    n = C.resolve(rid)                  # 'android:type/name' or 'com.lge:type/name'
    pkg, rest = n.split(':', 1)
    if pkg == 'android':
        return '*android:' + rest
    return rest

def val_text(dt, dv):
    if dt in (1, 7):
        return '@null' if dv == 0 else '@' + ref_name(dv)
    if dt in (2, 8):
        return '?' + ref_name(dv).replace('*android:', 'android:')
    return fmt_value(dt, dv, lg.values, C.resolve, set())

def attr_item_name(rid):
    n = C.resolve(rid)
    pkg, rest = n.split(':', 1)
    nm = rest.split('/', 1)[1]
    return ('android:' + nm) if pkg == 'android' else nm

ATTR_FMT = {0x1: 'reference', 0x2: 'string', 0x4: 'integer', 0x8: 'boolean', 0x10: 'color',
            0x20: 'float', 0x40: 'dimension', 0x80: 'fraction', 0x10000: 'enum', 0x20000: 'flags'}

values = collections.defaultdict(list)   # qualifier -> [xml lines]
copied = {}
for rid in sorted(seen):
    typ, name, vals = ent[rid]
    for cfg, v in vals:
        q = qualifiers(cfg)
        if v[0] == 'val' and v[1] == 3 and lg.values[v[2]].startswith('res/'):
            continue  # file resource, handled below
        if v[0] == 'val':
            dt, dv = v[1], v[2]
            text = val_text(dt, dv)
            if typ == 'string':
                s = lg.values[dv] if dt == 3 else text
                s = s.replace('\\', '\\\\').replace("'", "\\'").replace('"', '\\"').replace('@', '\\@').replace('?', '\\?')
                values[q].append('<string name="%s">%s</string>' % (name, esc(s).replace('&quot;', '\\"')))
            elif typ in ('dimen', 'color', 'bool', 'integer', 'drawable', 'fraction', 'layout', 'anim', 'id', 'style', 'attr'):
                values[q].append('<item type="%s" name="%s">%s</item>' % (typ, name, esc(text)))
            else:
                values[q].append('<item type="%s" name="%s">%s</item>' % (typ, name, esc(text)))
        else:
            _, parent, items = v
            if typ == 'style':
                p = ' parent="@%s"' % ref_name(parent) if parent else ' parent=""'
                lines = ['<style name="%s"%s>' % (name, p)]
                for nm, dt, dv in items:
                    ev = C.enum_fmt(nm, dt, dv)
                    lines.append('  <item name="%s">%s</item>' % (attr_item_name(nm), esc(ev if ev is not None else val_text(dt, dv))))
                lines.append('</style>')
                values[q].extend(lines)
            elif typ == 'attr':
                fmt = 0
                for nm, dt, dv in items:
                    if nm == 0x01000000:
                        fmt = dv
                fl = '|'.join(v for k, v in ATTR_FMT.items() if fmt & k and k < 0x10000)
                values[q].append('<attr name="%s"%s/>' % (name, (' format="%s"' % fl) if fl else ''))
            else:
                raise SystemExit('unhandled bag type %s/%s' % (typ, name))

for sid, n in SYNTH_IDS.items():
    lines = ['<style name="%s" parent="%s">' % (n, SYNTH_STYLES[n])]
    for nm, dt, dv in theme_items(n, attrs):
        lines.append('  <item name="%s">%s</item>' % (attr_item_name(nm), esc(val_text(dt, dv))))
    lines.append('</style>')
    values[''].extend(lines)

# Write project
if os.path.exists(OUT):
    shutil.rmtree(OUT)
os.makedirs(OUT + '/res')
for q, lines in values.items():
    d = OUT + '/res/values' + ('-' + q if q else '')
    os.makedirs(d, exist_ok=True)
    with open(d + '/lge.xml', 'w') as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n  ' + '\n  '.join(lines) + '\n</resources>\n')

z = zipfile.ZipFile(C.LGRES)
for path, src in files.items():
    dst = OUT + '/' + path
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    if src is None:
        if path.endswith('.9.png'):
            raise SystemExit('compiled 9-patch needs special handling: ' + path)
        open(dst, 'wb').write(z.read(path))
    else:
        # attribute and reference names: use *android: for android refs inside values
        src = re.sub(r'"@android:', '"@*android:', src)
        pass
        src = re.sub(r'"@com\.lge:', '"@', src)
        src = re.sub(r'"\?com\.lge:', '"?', src)
        src = src.replace(' lge:', ' app:')
        open(dst, 'w').write(src)

with open(OUT + '/AndroidManifest.xml', 'w') as f:
    f.write('<?xml version="1.0" encoding="utf-8"?>\n'
            '<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.lge"'
            ' android:versionCode="1" android:versionName="1.0">\n'
            '  <application android:hasCode="false"/>\n</manifest>\n')
with open(OUT + '/stable-ids.txt', 'w') as f:
    for rid in sorted(seen | set(SYNTH_IDS)):
        typ, name, _ = ent[rid]
        f.write('com.lge:%s/%s = 0x%08x\n' % (typ, name, rid))
print('resources', len(seen), 'value dirs', len(values), 'files', len(files))
