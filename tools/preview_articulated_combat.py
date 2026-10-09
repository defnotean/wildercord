#!/usr/bin/env python3
"""Source-driven, explicitly OFFLINE articulated-combat inspection.

Exports actual ArticulatedRig ModelPart polygons using publisher Minecraft classes, then
rasterizes with Pillow/numpy. This is NOT game footage, native acceptance, or a GPU test.
No downloaded package, native client, external asset, or public upload is used.
"""
from __future__ import annotations
import argparse, hashlib, io, json, math, pathlib, subprocess, zipfile
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = pathlib.Path(__file__).resolve().parents[1]
DEFAULT_VERIFY = pathlib.Path('/workspace/shared/wildercord_compile_verification')
DEFAULT_OUT = pathlib.Path('/workspace/shared/articulated_preview')
BG = (15, 22, 31); FG = (224, 236, 242); ACCENT = (74, 222, 198)
FONT = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
def font(n): return ImageFont.truetype(FONT, n)
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def matrix(v): return np.array(v, dtype=np.float64).reshape(4, 4, order='F')
def trans(x=0,y=0,z=0):
    m=np.eye(4);m[:3,3]=[x,y,z];return m
def scale(x,y,z): return np.diag([x,y,z,1.])
def rot(axis,a):
    a=math.radians(a); c,s=math.cos(a),math.sin(a);m=np.eye(4)
    i,j={'x':(1,2),'y':(2,0),'z':(0,1)}[axis]
    m[i,i]=m[j,j]=c;m[i,j]=-s;m[j,i]=s;return m
def transform(p,m): return np.c_[np.asarray(p),np.ones(len(p))].dot(m.T)[:,:3]

def export(out,verify):
    java=pathlib.Path(json.loads((verify/'toolchain_manifest.json').read_text())['java']['directory'])/'bin'
    deps=[verify/'originals/minecraft-client.jar',verify/'originals/minecraft-server-inner.jar'];seen=set()
    for name in ['dependency_manifest.json','embedded_manifest.json']:
        for rec in json.loads((verify/name).read_text()):
            p=verify/rec['path']
            if p.suffix=='.jar' and '/originals/' not in str(p) and 'iris' not in str(p) and p.name not in seen:
                deps.append(p);seen.add(p.name)
    cp=':'.join(map(str,deps)); classes=out/'classes';classes.mkdir(exist_ok=True)
    sources=['src/main/java/dev/wildercord/aura/ArticulatedCombatPose.java','src/client/java/dev/wildercord/client/mixin/ModelPartChildrenAccessor.java','src/client/java/dev/wildercord/client/combat/ArticulatedRig.java','tools/ExportArticulatedGeometry.java','tools/ExportArticulatedPose.java']
    commands=[[str(java/'javac'),'-proc:none','--release','25','-sourcepath',str(ROOT/'src/main/java'),'-cp',cp,'-d',str(classes),*[str(ROOT/s) for s in sources]]]
    with (out/'export.log').open('w') as log:
        subprocess.run(commands[-1],stdout=log,stderr=log,check=True)
        for cls,name in [('ExportArticulatedGeometry','geometry.json'),('ExportArticulatedPose','poses.json')]:
            cmd=[str(java/'java'),'-cp',str(classes)+':'+cp,cls];commands.append(cmd)
            with (out/name).open('w') as f: subprocess.run(cmd,stdout=f,stderr=log,check=True)
    (out/'commands.json').write_text(json.dumps(commands,indent=2)+'\n')

class Preview:
    def __init__(self,out,verify):
        self.out=out;self.geom=json.loads((out/'geometry.json').read_text());self.poses=json.loads((out/'poses.json').read_text())
        self.joints={v['name']:i for i,v in enumerate(self.poses['joints'])}
        self.variants={v['id']:v for v in self.geom['variants']}
        self.clips={(c['id'],c['leftHanded']):c for c in self.poses['clips']}
        jar=verify/'originals/minecraft-client.jar';self.asset_sources={}
        with zipfile.ZipFile(jar) as z:
            self.tex={}
            for key,name in [('wide','assets/minecraft/textures/entity/player/wide/steve.png'),('slim','assets/minecraft/textures/entity/player/slim/alex.png'),('sword','assets/minecraft/textures/item/iron_sword.png')]:
                b=z.read(name);self.tex[key]=np.array(Image.open(io.BytesIO(b)).convert('RGBA'));self.asset_sources[key]={'jar':str(jar),'entry':name,'sha256':hashlib.sha256(b).hexdigest()}
            handheld=json.loads(z.read('assets/minecraft/models/item/handheld.json'))
            assert handheld['display']['thirdperson_righthand']=={'rotation':[0,-90,55],'translation':[0,4.,.5],'scale':[.85,.85,.85]}
            assert handheld['display']['thirdperson_lefthand']=={'rotation':[0,90,-55],'translation':[0,4.,.5],'scale':[.85,.85,.85]}
        p=ROOT/'src/main/resources/assets/wildercord/textures/entity/duelist/ember.png'
        self.tex['master']=np.array(Image.open(p).convert('RGBA'));self.asset_sources['master']={'path':str(p.relative_to(ROOT)),'sha256':sha(p)}
        self.sword=self.sword_faces()
    def frame(self,clip,left,age):
        return min(self.clips[(clip,left)]['frames'],key=lambda f:abs(f['age']-age))
    def sword_faces(self):
        # The engine's generated-item slab is z=7.5..8.5, x/y=0..16. We keep its two
        # alpha-masked surfaces exactly; edge quads follow opaque sprite pixel boundaries.
        t=self.tex['sword'];h,w=t.shape[:2];faces=[]
        faces.append((np.array([[0,0,8.5],[16,0,8.5],[16,16,8.5],[0,16,8.5]])/16,np.array([[0,1],[1,1],[1,0],[0,0]])))
        faces.append((np.array([[16,0,7.5],[0,0,7.5],[0,16,7.5],[16,16,7.5]])/16,np.array([[1,1],[0,1],[0,0],[1,0]])))
        opaque=t[:,:,3]>127
        for y,x in zip(*np.where(opaque)):
            a,b=x/w,(x+1)/w;c,d=1-(y+1)/h,1-y/h;u,v=(x+.5)/w,(y+.5)/h
            for dx,dy,p in [(0,-1,[[a,d,7.5/16],[b,d,7.5/16],[b,d,8.5/16],[a,d,8.5/16]]),(0,1,[[a,c,7.5/16],[b,c,7.5/16],[b,c,8.5/16],[a,c,8.5/16]]),(-1,0,[[a,c,7.5/16],[a,d,7.5/16],[a,d,8.5/16],[a,c,8.5/16]]),(1,0,[[b,c,7.5/16],[b,d,7.5/16],[b,d,8.5/16],[b,c,8.5/16]])]:
                yy,xx=y+dy,x+dx
                if not (0<=xx<w and 0<=yy<h and opaque[yy,xx]):faces.append((np.array(p),np.tile([u,v],(4,1))))
        return faces
    def meshes(self,variant,frame,left,first=False,yaw=0,pitch=0,layers=True):
        palette=frame['firstPerson'] if first else frame;world=[matrix(m) for m in palette['world']]
        clearance=next((v[2] for v in self.geom.get('clearanceGrid',[]) if v[0]==yaw and v[1]==pitch),0)*frame['weight']
        yaw*=frame['weight'];pitch*=frame['weight']
        origin=np.array(palette.get('origin',[0,0,0]))+np.array([0,-.8*clearance,-clearance])
        root=trans(*origin)@rot('y',-yaw)@rot('x',-pitch)@scale(-1/16,-1/16,1/16) if first else np.eye(4)
        meshes=[]
        for c in self.variants[variant]['cubes']:
            bits=c['path'].strip('/').split('/');name=bits[-1];overlay=name not in self.joints
            joint=bits[-2] if overlay else name
            if overlay and not layers: continue
            if first and not any(s in joint for s in ['UPPER_ARM','FOREARM','HAND']): continue
            m=root@world[self.joints[joint]]
            for face in c['faces']:
                vertices=np.array(face['vertices']);points=transform(vertices[:,:3],m)
                meshes.append((points,vertices[:,3:],self.tex[variant],joint,overlay))
        j='LEFT_SOCKET' if left else 'RIGHT_SOCKET';offset=(1 if left else -1)*(.5 if variant=='slim' else 1)
        # Exact production orientItemAtSocket, followed by the official ItemTransform matrix.
        socket=world[self.joints[j]].copy();socket[:3,3]+=world[self.joints[j.replace('SOCKET','HAND')]][:3,0]*offset
        orientation=rot('x',-90)@rot('y',180)@trans(0,1.327,-1.439)
        model=scale(16,16,16)@matrix(self.geom['itemDisplay'][int(left)])
        item=root@socket@orientation@model
        for p,uv in self.sword:meshes.append((transform(p,item),uv,self.tex['sword'],'SWORD',False))
        return meshes,transform([[0,0,0]],root@socket)[0]
    def geometry_checks(self):
        checks={}
        chains=[['CHEST','SPINE','PELVIS']]+[[s+a for a in ['UPPER_ARM','FOREARM','HAND']] for s in ['RIGHT_','LEFT_']]+[[s+a for a in ['THIGH','SHIN','FOOT']] for s in ['RIGHT_','LEFT_']]
        for name,var in self.variants.items():
            cubes={c['path'].split('/')[-1]:c for c in var['cubes'] if c['path'].split('/')[-1] in self.joints}
            max_uv=0;seam_vertices=0;volume=0;partition=[]
            for chain in chains:
                ranges=[]
                for joint in chain:
                    c=cubes[joint];m=matrix(c['bindWorld']);m[:3,3]*=16
                    p=transform([v[:3] for f in c['faces'] for v in f['vertices']],m);lo=p.min(0);hi=p.max(0);volume+=float(np.prod(hi-lo));ranges.append((lo,hi))
                gaps=[abs(ranges[i][1][1]-ranges[i+1][0][1]) for i in range(len(ranges)-1)]
                partition.append({'chain':chain,'maxBindGapPixels':max(gaps)})
                for a,b in zip(chain,chain[1:]):
                    def side_vertices(j):
                        c=cubes[j];m=matrix(c['bindWorld']);m[:3,3]*=16;result=[]
                        for f in c['faces']:
                            if abs(f['normal'][1])>.5:continue
                            v=np.array(f['vertices']);p=transform(v[:,:3],m)
                            result.extend((tuple(np.round(pos,6)),tuple(f['normal']),uv) for pos,uv in zip(p,v[:,3:]))
                        return result
                    av,bv=side_vertices(a),side_vertices(b)
                    for p,n,uv in av:
                        for q,k,vv in bv:
                            if p==q and n==k:seam_vertices+=1;max_uv=max(max_uv,float(np.max(np.abs(uv-vv))))
            checks[name]={'matchingSideSeamVertices':seam_vertices,'maxSideUVErrorTexels':max_uv*64,'bindPartition':partition,'bodyLimbVolumePixels3':volume}
        return checks

class Raster:
    def __init__(self,w,h,first=False,fov=90):
        self.w=w;self.h=h;self.first=first;self.fov=fov
        self.rgb=np.zeros((h,w,3),np.uint8);self.rgb[:]=BG
        self.depth=np.full((h,w),np.inf);self.ids=np.zeros((h,w),np.uint8)
        if not first:
            eye=np.array([37.,-11.,-65.]);target=np.array([0.,9.,0.]);f=target-eye;f/=np.linalg.norm(f)
            r=np.cross(f,[0,-1,0]);r/=np.linalg.norm(r);u=np.cross(r,f)
            self.camera=np.array([r,u,f]);self.eye=eye;self.factor=min(w/42,h/46)
    def project(self,p):
        if self.first:
            z=-p[:,2];fac=self.h/(2*math.tan(math.radians(self.fov)/2))
            return np.c_[self.w/2+p[:,0]/z*fac,self.h/2-p[:,1]/z*fac,z]
        v=(p-self.eye).dot(self.camera.T)
        return np.c_[self.w/2+v[:,0]*self.factor,self.h/2-v[:,1]*self.factor,v[:,2]]
    def triangle(self,p,uv,t,intensity,identity):
        if np.any(p[:,2]<=.05): return
        lo=np.maximum(np.floor(p[:,:2].min(0)).astype(int),0);hi=np.minimum(np.ceil(p[:,:2].max(0)).astype(int),[self.w-1,self.h-1])
        if np.any(lo>hi):return
        x,y=np.meshgrid(np.arange(lo[0],hi[0]+1)+.5,np.arange(lo[1],hi[1]+1)+.5)
        a,b,c=p;den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
        if abs(den)<1e-10:return
        u=((b[1]-c[1])*(x-c[0])+(c[0]-b[0])*(y-c[1]))/den
        v=((c[1]-a[1])*(x-c[0])+(a[0]-c[0])*(y-c[1]))/den;ww=1-u-v
        mask=(u>=-1e-6)&(v>=-1e-6)&(ww>=-1e-6)
        if self.first:
            iz=u/a[2]+v/b[2]+ww/c[2];z=1/iz
            tuv=(u[:,:,None]*uv[0]/a[2]+v[:,:,None]*uv[1]/b[2]+ww[:,:,None]*uv[2]/c[2])/iz[:,:,None]
        else:z=u*a[2]+v*b[2]+ww*c[2];tuv=u[:,:,None]*uv[0]+v[:,:,None]*uv[1]+ww[:,:,None]*uv[2]
        th,tw=t.shape[:2];tx=np.clip(np.floor(tuv[:,:,0]*tw).astype(int),0,tw-1);ty=np.clip(np.floor(tuv[:,:,1]*th).astype(int),0,th-1);color=t[ty,tx]
        sl=np.s_[lo[1]:hi[1]+1,lo[0]:hi[0]+1];mask&=(color[:,:,3]>127)&(z<self.depth[sl])
        self.depth[sl][mask]=z[mask];self.rgb[sl][mask]=np.clip(color[:,:,:3]*intensity,0,255).astype(np.uint8)[mask];self.ids[sl][mask]=identity
    def draw(self,meshes):
        for p,uv,t,joint,overlay in meshes:
            q=self.project(p);normal=np.cross(p[1]-p[0],p[2]-p[0]);normal/=max(np.linalg.norm(normal),1e-10)
            light=np.array([-.3,-.7,-.65]);light/=np.linalg.norm(light);shade=.68+.32*abs(float(normal.dot(light)))
            for idx in [[0,1,2],[0,2,3]]:self.triangle(q[idx],uv[idx],t,shade,2 if joint=='SWORD' else 1)
        im=Image.fromarray(self.rgb);d=ImageDraw.Draw(im)
        if self.first:
            cx,cy=self.w//2,self.h//2;d.line((cx-7,cy,cx+7,cy),fill=(220,228,230),width=1);d.line((cx,cy-7,cx,cy+7),fill=(220,228,230),width=1)
            d.rectangle((cx-self.w*.025,cy-self.h*.025,cx+self.w*.025,cy+self.h*.025),outline=(52,79,88))
        return im

def caption(im,line,sub=None):
    content=im;im=Image.new('RGB',(content.width,content.height+74),BG);im.paste(content,(0,74));d=ImageDraw.Draw(im);d.rounded_rectangle((10,9,im.width-10,67 if sub else 43),radius=8,fill=(23,34,47));d.text((22,16),line,font=font(18),fill=FG)
    if sub:d.text((22,42),sub,font=font(13),fill=(151,177,190))
    return im

def sheet(cells,cols,title,subtitle,out):
    w,h=cells[0].size;rows=math.ceil(len(cells)/cols);im=Image.new('RGB',(cols*w,rows*h+106),BG);d=ImageDraw.Draw(im)
    d.text((24,16),'OFFLINE SOURCE-DRIVEN PREVIEW, NOT GAME FOOTAGE',font=font(24),fill=ACCENT)
    d.text((24,52),title,font=font(21),fill=FG);d.text((24,80),subtitle,font=font(14),fill=(151,177,190))
    for i,c in enumerate(cells): im.paste(c,((i%cols)*w,106+(i//cols)*h))
    im.save(out)


def intersection_audit(p):
    """SAT + convex halfspace intersection of exported BASE cuboids, in model pixels.

    This distinguishes the expected small wedge overlap at a hinged wrist from collisions
    between independently posed hands/forearms. scipy is already installed; no installer runs.
    """
    from itertools import combinations
    from scipy.spatial import ConvexHull
    def obb(variant,f,j,first=False):
        c=next(c for c in p.variants[variant]['cubes'] if c['path'].split('/')[-1]==j)
        v=np.array([v[:3] for face in c['faces'] for v in face['vertices']]);lo,hi=v.min(0),v.max(0)
        m=matrix((f['firstPerson'] if first else f)['world'][p.joints[j]])
        return transform([(lo+hi)/2],m)[0],m[:3,:3],(hi-lo)/2
    def sat(a,b):
        ac,aa,ah=a;bc,ba,bh=b;values=[]
        for n in [*aa.T,*ba.T,*[np.cross(i,j) for i in aa.T for j in ba.T]]:
            length=np.linalg.norm(n)
            if length<1e-8:continue
            n=n/length;overlap=np.abs(aa.T@n)@ah+np.abs(ba.T@n)@bh-abs((ac-bc)@n)
            if overlap < -1e-7:return False,float(overlap)
            values.append(overlap)
        return True,float(min(values))
    def volume(a,b):
        normals=[];dist=[]
        for center,axes,half in [a,b]:
            for n,h in zip(axes.T,half):
                for sign in [-1,1]:normals.append(sign*n);dist.append(sign*n@center+h)
        normals=np.array(normals);dist=np.array(dist);points=[]
        for ijk in combinations(range(12),3):
            n=normals[list(ijk)];d=dist[list(ijk)]
            if abs(np.linalg.det(n))<1e-8:continue
            v=np.linalg.solve(n,d)
            if np.all(normals@v <= dist+1e-6):points.append(v)
        if len(points)<4:return 0.
        return float(ConvexHull(np.unique(np.round(points,7),axis=0)).volume)
    result=[]
    for variant,clip in [('wide','spellcut'),('slim','spellcut'),('master','master_sweep')]:
        for first in ([False,True] if variant!='master' else [False]):
            frames=p.clips[(clip,False)]['frames']
            pairs=[('RIGHT_FOREARM','RIGHT_HAND'),('LEFT_FOREARM','LEFT_HAND'),('RIGHT_HAND','LEFT_HAND'),('RIGHT_FOREARM','LEFT_HAND'),('RIGHT_HAND','LEFT_FOREARM'),('RIGHT_FOREARM','LEFT_FOREARM')]
            for j,k in pairs:
                worst=(0,None);intersect=[]
                for f in frames:
                    hit,over=sat(obb(variant,f,j,first),obb(variant,f,k,first))
                    if hit and over>1e-5:
                        intersect.append(f['age'])
                        if over>worst[0]:worst=over,f
                rec={'variant':variant,'firstPerson':first,'pair':[j,k],'sameWrist':j.split('_')[0]==k.split('_')[0],'intersectingSamples':len(intersect),'maxSATPenetrationPixels':worst[0]}
                if worst[1]:
                    over,f=worst;rec.update({'age':f['age'],'intersectionVolumeAtMaxSATPixels3':volume(obb(variant,f,j,first),obb(variant,f,k,first)),'firstAge':min(intersect),'lastAge':max(intersect)})
                result.append(rec)
            for guard in ['LEFT_HAND','LEFT_FOREARM','LEFT_UPPER_ARM']:
                closest=(1e9,None)
                for f in frames:
                    palette=(f['firstPerson'] if first else f)['world'];m=matrix(palette[p.joints['RIGHT_SOCKET']]);hand=matrix(palette[p.joints['RIGHT_HAND']]);h=m[:3,3]+hand[:3,0]*(-.5 if variant=='slim' else -1)
                    center,axes,half=obb(variant,f,guard,first);d=np.abs(axes.T@(h-center))-half;distance=np.linalg.norm(np.maximum(d,0))
                    if distance<closest[0]:closest=distance,f['age']
                result.append({'variant':variant,'firstPerson':first,'guardSegment':guard,'minGripPointClearancePixels':float(closest[0]),'age':closest[1]})
    return result

def main():
    ap=argparse.ArgumentParser(description=__doc__);ap.add_argument('--out',type=pathlib.Path,default=DEFAULT_OUT);ap.add_argument('--verification',type=pathlib.Path,default=DEFAULT_VERIFY);ap.add_argument('--reuse-export',action='store_true');ap.add_argument('--skip-gif',action='store_true');ap.add_argument('--visual-only',action='store_true');args=ap.parse_args();out=args.out;out.mkdir(parents=True,exist_ok=True)
    if not args.reuse_export:export(out,args.verification)
    p=Preview(out,args.verification);checks=p.geometry_checks();cells=[]
    for variant,left,clip in [('wide',False,'spellcut'),('slim',True,'spellcut'),('master',False,'master_sweep')]:
        ages=[0,2.625,4,7] if clip=='spellcut' else [0,11.75,18,22]
        for label,age in zip(['BIND','CHAMBER','IMPACT','FOLLOW'],ages):
            f=p.frame(clip,left,age);r=Raster(500,470);m,hilt=p.meshes(variant,f,left);im=r.draw(m)
            im=caption(im,f'{variant.upper()} / {"LEFT" if left else "RIGHT"} / {label}',f'{clip} t={f["age"]:g} | '+('geometry bind only' if not f['weight'] else 'full-weight segmented geometry'))
            # Small geometric inspection markers, explicitly not in-game effects.
            d=ImageDraw.Draw(im)
            for j in ['PELVIS','SPINE','CHEST','RIGHT_FOREARM','LEFT_FOREARM','RIGHT_SHIN','LEFT_SHIN']:
                q=r.project(transform([[0,0,0]],matrix(f['world'][p.joints[j]])))[0];x,y=q[:2];y+=74
                d.ellipse((x-3,y-3,x+3,y+3),outline=ACCENT,width=1)
            cells.append(im)
    sheet(cells,4,'Actual three-part torso, elbow / wrist and knee / ankle chains','Bind = geometry inspection. Other columns are full-weight poses. Master clothing/layers and vanilla idle-blend baseline are omitted.',out/'01_segmented_third_person.png')
    cells=[]
    for variant in ['wide','slim']:
        for left in [False,True]:
            for fov in [70,90,110]:
                f=p.frame('spellcut',left,4);r=Raster(600,338,True,fov);m,hilt=p.meshes(variant,f,left,True);im=r.draw(m)
                im=caption(im,f'{variant.upper()} / {"LEFT" if left else "RIGHT"} / FOV {fov}',f'impact at 4 ticks | '+('native HUD projection' if fov==70 else 'hypothetical stress projection'))
                cells.append(im)
    sheet(cells,3,'First-person impact: native HUD FOV 70, plus hypothetical 90 / 110 stress tests','Minecraft 26.3 holds hand FOV at 70 regardless of world-FOV setting. Crosshair and target rectangle are inspection overlays.',out/'02_first_person_fov_matrix.png')
    cells=[]
    for left in [False,True]:
        for age in [0,2.625,4,7]:
            f=p.frame('spellcut',left,age);r=Raster(500,282,True,70);m,h=p.meshes('wide',f,left,True);im=r.draw(m)
            im=caption(im,f'{"LEFT" if left else "RIGHT"} / {f["phase"]}',f'Native HUD FOV 70 | t={f["age"]:g} ticks')
            cells.append(im)
    sheet(cells,4,'First-person camera-space composition through the cut','Same hand and socket hierarchy as the body, independently authored placement. Iron sword uses vanilla third-person display.',out/'03_first_person_sequence.png')
    cells=[]
    for left in [False,True]:
        for age,yaw,pitch in [(2.625,0,0),(2.625,-25 if left else 25,-20),(4,-25 if left else 25,-20),(7,-25 if left else 25,-20)]:
            f=p.frame('spellcut',left,age);r=Raster(500,282,True,70);m,h=p.meshes('wide',f,left,True,yaw,pitch);im=r.draw(m)
            im=caption(im,f'{"LEFT" if left else "RIGHT"} / t={age:g}',f'native HUD70 | yaw={yaw:+g}, pitch={pitch:+g}');cells.append(im)
    sheet(cells,4,'Clamped free-look stress: actual mesh plus root clearance','Conditional clearance exported from production pose helper. No camera rotation is changed by the combat pose.',out/'05_free_look_near_plane.png')
    if args.visual_only:
        print('Visual sheets rendered; geometric audits were not rerun.',flush=True);return
    report={'label':'OFFLINE SOURCE-DRIVEN PREVIEW, NOT GAME FOOTAGE','geometrySource':p.geom['provenance'],'officialModelPartParity':p.geom['verification'],'geometryChecks':checks,'assets':p.asset_sources,'limits':[]}
    # Strict near-plane test over all opaque-sprite vertices plus all arm base/overlay vertices,
    # through the entire Spellcut and all clamped accepted-aim offsets used by the renderer.
    near=[];occlusion=[];worst=[];hilt_error=0
    for variant in ['wide','slim']:
        for left in [False,True]:
            maxz=-np.inf;where=None;tested=0
            for f in p.clips[('spellcut',left)]['frames']:
                meshes,h=p.meshes(variant,f,left,True)
                # Alpha-transparent slab corners are excluded; opaque edge vertices bound its silhouette.
                canonical=np.concatenate([v[0] for v in meshes if v[3]!='SWORD']+[v[0] for v in meshes if v[3]=='SWORD'][2:])
                origin=np.array(f['firstPerson']['origin']);relative=canonical-origin
                for yaw,pitch,clearance in p.geom['clearanceGrid']:
                    effectiveYaw=yaw*f['weight'];effectivePitch=pitch*f['weight'];effectiveClearance=clearance*f['weight']
                    vertices=transform(relative,rot('y',-effectiveYaw)@rot('x',-effectivePitch))+origin+[0,-.8*effectiveClearance,-effectiveClearance]
                    z=float(vertices[:,2].max());tested+=1
                    if z>maxz:maxz=z;where={'age':f['age'],'yaw':yaw,'pitch':pitch,'clearance':clearance}
            print('near completed',variant,left,maxz,where,flush=True)
            near.append({'variant':variant,'left':left,'placementsTested':tested,'nearestCameraZ':maxz,'nearPlaneZ':-.05,'clearanceBlocks':-.05-maxz,'passes':maxz<-.05,'worst':where})
            # Pixel-level central target occlusion at canonical aim. Lower-resolution audit keeps
            # all 1/8-tick frames; selected full-size contact sheets are rendered separately.
            for fov in [70,90,110]:
                max_center=0;max_screen=0;cross=[];worst_age=0;min_sword=1e9
                for f in p.clips[('spellcut',left)]['frames']:
                    r=Raster(256,144,True,fov);m,h=p.meshes(variant,f,left,True);r.draw(m)
                    center=r.ids[68:76,121:135]>0;fraction=float(center.mean());screen=float((r.ids>0).mean());sword=int((r.ids==2).sum());min_sword=min(min_sword,sword)
                    if fraction>max_center:max_center=fraction;worst_age=f['age']
                    max_screen=max(max_screen,screen)
                    if r.ids[72,128]>0:cross.append(f['age'])
                print('occlusion completed',variant,left,fov,max_center,len(cross),flush=True)
                occlusion.append({'variant':variant,'left':left,'fov':fov,'frames':129,'maxCentral5PercentCoverage':max_center,'worstAge':worst_age,'maxScreenCoverage':max_screen,'crosshairOccludedAges':cross,'minVisibleSwordPixelsAt256x144':min_sword})
    report['firstPersonNearPlane']=near;report['firstPersonOcclusionCanonicalAim']=occlusion
    report['baseCuboidIntersections']=intersection_audit(p)
    (out/'intersection_report.json').write_text(json.dumps(report['baseCuboidIntersections'],indent=2)+'\n')
    official_hilt=transform([[3.5/16,3.5/16,8/16]],matrix(p.geom['itemDisplay'][0]))[0]*16
    report['hiltDisplayResidualPixels']=float(np.linalg.norm(official_hilt-np.array([0,-1.327,1.439])))
    report['lateralHiltPlacement']='hand-space lateral centering before socket rotation, matching ArticulatedRig.socket'
    report['thirdPersonPartialWeightScope']='pure authored clip; live vanilla baseline/head-look composite omitted'
    report['firstPersonAngleScope']='finite yaw/pitch clamped to25/20 degrees, then multiplied by clip weight; clearance grid is likewise weighted'
    report['centerAuditResolution']={'width':256,'height':144,'targetRectangle':[121,68,135,76],'crosshairSample':[128,72]}
    report['limits']=['Offline software rasterization of actual exported ModelPart polygons, not the Minecraft deferred renderer or GPU. No native test or in-game footage claim.','Sword main surfaces and silhouette use the official iron-sword sprite; edge tessellation is reconstructed from opaque texel boundaries, not read from the baked item cache. Official ItemTransform.apply supplies the display matrix.','Master preview includes actual base rig and the original Ember skin, but excludes attached hood, cloak, mantle, scabbard and all render layers. Those require the native fixture.','No camera movement, attack input, targeting, damage, networking, multiplayer, lifecycle cleanup, deferred pass, resource-pack, Iris, held-layer, or mixin test is implied.','Rigid cut surfaces are closed cuboids, not weighted skinning. Joint interiors reuse original limb cap UVs; geometric bends can reveal hard corners and overlap. SAT intersections measure base cuboids only, not inflated skin layers. Overlap volume is convex halfspace intersection; SAT depth is minimum separating-axis translation, not contact dynamics.','Near-plane coverage includes clamped aim offsets; pixel-level target occlusion covers canonical aim only. Camera.calculateHudFov bytecode uses 70 degrees modified only for death/fluid; GameRenderer.render3dHud uses hudFov and near=0.05. Thus 90/110 are hypothetical hand-projection stress tests, not world-FOV acceptance claims. Native handed transforms, transparency sorting and pose-stack conventions still require gameplay verification.','The 0-tick body bind frame is an inspection baseline: third-person articulation is weight-gated. Compatible first-person sword arms retain their independently authored idle pose at zero weight. Partial-weight third-person animation/SAT exports omit ArticulatedCombat.baselineArm, live vanilla head look, breathing and held-arm baselines; the body contact sheet selects weight=1 chamber/impact/follow for source-matched geometry. GIF partial-body frames show the pure clip, not the full resolved in-game baseline.']
    source_paths=['src/main/java/dev/wildercord/aura/ArticulatedCombatPose.java','src/client/java/dev/wildercord/client/combat/ArticulatedRig.java','src/client/java/dev/wildercord/client/combat/ArticulatedViewModel.java','src/client/java/dev/wildercord/client/combat/ArticulatedCombat.java','tools/ExportArticulatedGeometry.java','tools/ExportArticulatedPose.java','tools/preview_articulated_combat.py']
    report['sourceSha256']={s:sha(ROOT/s) for s in source_paths};report['dataSha256']={s:sha(out/s) for s in ['geometry.json','poses.json']}
    (out/'verification_report.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({'parity':report['officialModelPartParity'],'near':near,'occlusion':occlusion},indent=2),flush=True)
    if not args.skip_gif:
        frames=[]
        for age in np.linspace(0,16,65):
            f=p.frame('spellcut',False,float(age));im=Image.new('RGB',(1000,590),BG);d=ImageDraw.Draw(im);d.text((17,10),'OFFLINE SOURCE-DRIVEN PREVIEW, NOT GAME FOOTAGE',font=font(19),fill=ACCENT);d.text((17,33),'Playback slowed 3.2x; source timing is 20 ticks/second.',font=font(12),fill=(151,177,190))
            for i,first in enumerate([False,True]):
                r=Raster(500,282 if first else 460,first,70);m,h=p.meshes('wide',f,False,first);v=r.draw(m);v=caption(v,('FIRST PERSON' if first else 'THIRD PERSON')+f' / {f["phase"]}',f't={f["age"]:g} ticks'+(' | pure clip; idle baseline omitted' if not first else ' | native HUD70, 16:9'));im.paste(v,(i*500,47))
            frames.append(im)
        frames[0].save(out/'04_spellcut_offline.gif',save_all=True,append_images=frames[1:],duration=40,loop=0,optimize=False)
    assert all(n['passes'] for n in near),'Near-plane failure: see verification_report.json'
    assert all(c['maxSideUVErrorTexels']<1e-4 for c in checks.values()),'UV seam mismatch'
    print('DONE',out,flush=True)
if __name__=='__main__':main()
