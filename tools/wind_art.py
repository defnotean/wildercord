"""Wind-specific fine air filaments, separate from glowing spell bands."""
from pathlib import Path
from PIL import Image
import math
ROOT=Path(__file__).resolve().parent.parent

def main():
    im=Image.new('RGBA',(32,32))
    for x in range(32):
        taper=math.sin(math.pi*(x+.5)/32)**.55
        for y in range(32):
            # Three fine laminar striations with a curved middle thread and soft edges.
            center=15.5+1.3*math.sin(x*.16)
            alpha=sum(strength*math.exp(-((y-center-offset)/thickness)**2)
                      for offset,strength,thickness in [(-3,80,1.1),(0,190,1.4),(3,65,.8)])
            im.putpixel((x,y),(227,242,237,min(255,int(alpha*taper))))
    p=ROOT/'src/main/resources/assets/wildercord/textures/particle/wind_filament.png'
    p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
if __name__=='__main__':main()
