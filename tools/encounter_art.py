"""Face-painted skins for the Root Guardian and copper Storm Conductor, matching their custom UVs."""
from world_art import Sheet, box, noise
from item_art import ramp, hexc

def root_skin(glow=False):
    cv=Sheet(128,64)
    bark=ramp('#20150e','#342116','#563923','#785332','#a17845','#c4a56b')
    leaves=ramp('#182d16','#294523','#3d6930','#618941','#93b657')
    for u,v,w,h,d,kind in [(0,0,12,18,10,'bark'),(48,0,10,10,10,'head'),(0,32,5,12,6,'bark'),(24,32,5,18,5,'bark'),(48,24,16,4,14,'leaf'),(96,0,6,8,1,'heart')]:
        for name,(x0,y0,fw,fh) in box(u,v,w,h,d).items():
            for y in range(fh):
                for x in range(fw):
                    seed=noise(x0+x,y0+y,61)
                    col=None
                    if kind=='heart':
                        edge=min(x,fw-1-x,y,fh-1-y)
                        col=hexc('#ffe6a0' if edge>1 else '#cc7b30')
                    elif kind=='head' and name=='front' and y in (3,4) and x in (2,3,6,7): col=hexc('#fff0a5')
                    elif not glow:
                        tones=leaves if kind=='leaf' else bark
                        t=int(seed*3)+1
                        if kind!='leaf' and (x+(y//4))%5==0: t=0
                        if y==0:t+=1
                        col=tones[min(len(tones)-1,t)]
                        if kind=='bark' and seed>.92:col=leaves[2]
                    if col is not None: cv.put(x0+x,y0+y,col)
    return cv.image()

def storm_skin(glow=False):
    cv=Sheet(128,64)
    copper=ramp('#30201d','#754332','#a86443','#d39062','#efbb83')
    blue=ramp('#123142','#1c5a78','#429bd0','#8bdbf4','#e1f8ff')
    for u,v,w,h,d,kind in [(0,0,8,8,8,'core'),(32,0,10,10,10,'cage'),(0,24,2,2,18,'metal'),(48,24,2,12,2,'rod'),(64,24,6,4,6,'cap')]:
        for name,(x0,y0,fw,fh) in box(u,v,w,h,d).items():
            for y in range(fh):
                for x in range(fw):
                    edge=min(x,fw-1-x,y,fh-1-y)
                    col=None
                    if kind=='core': col=blue[4 if (x+y)%4==0 else 2+int(noise(x,y,7)*2)]
                    elif kind=='rod' and y%4==0: col=blue[3]
                    elif not glow and (kind!='cage' or edge==0):
                        col=copper[1+int(noise(x0+x,y0+y,93)*3)]
                        if edge==0: col=copper[0 if y==fh-1 else 4]
                        if (x+y)%7==0:col=hexc('#437d70')
                    if col is not None:cv.put(x0+x,y0+y,col)
    return cv.image()

def write(g):
    for name,fn in [('root_guardian',root_skin),('storm_conductor',storm_skin)]:
        g.save(fn(),g.ASSETS/f'textures/entity/{name}.png')
        g.save(fn(True),g.ASSETS/f'textures/entity/{name}_glow.png')
