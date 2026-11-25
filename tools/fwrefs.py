import sys,zipfile,struct,os
sys.path.insert(0,'/home/julian/v50-camera/tools')
import arsc
aosp=arsc.load_apk(sys.argv[1]); lgfw=arsc.load_apk(sys.argv[2])
A=aosp.pkgs[1][1]; L=lgfw.pkgs[1][1]
print('AOSP fw entries',len(A),'LG fw entries',len(L))
def refs(apk):
    z=zipfile.ZipFile(apk); out={}
    for n in z.namelist():
        if not (n=='resources.arsc' or n.endswith('.xml')): continue
        d=z.read(n)
        for i in range(0,len(d)-8,4):
            if d[i]==8 and d[i+1]==0 and d[i+2]==0 and d[i+3] in (1,2):
                v=struct.unpack_from('<I',d,i+4)[0]
                if v>>24==1: out.setdefault(v,set()).add(n)
        if n.endswith('.xml') and n!='AndroidManifest.xml':
            off=8
            while off+8<=len(d):
                t,h,s=struct.unpack_from('<HHI',d,off)
                if s==0:break
                if t==0x0180:
                    for j in range((s-h)//4):
                        v=struct.unpack_from('<I',d,off+h+4*j)[0]
                        if v>>24==1: out.setdefault(v,set()).add(n)
                off+=s
    return out
for apk in sys.argv[3:]:
    r=refs(apk)
    miss=[(hex(v),lgfw.name(v),len(f),sorted(f)[0]) for v,f in r.items() if v not in A]
    diff=[(hex(v),lgfw.name(v),aosp.name(v)) for v in r if v in A and v in L and lgfw.name(v)!=aosp.name(v)]
    print('\n==',os.path.basename(apk),'android refs',len(r),'missing in AOSP',len(miss),'name-mismatch',len(diff))
    for m in sorted(miss,key=lambda x:-x[2])[:30]: print('  MISS',m)
    for m in diff[:15]: print('  DIFF',m)
