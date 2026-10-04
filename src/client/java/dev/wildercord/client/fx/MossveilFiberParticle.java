package dev.wildercord.client.fx;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.world.phys.Vec3;
/** Tiny original stitched floss strip; sampled world light, physical droop and finite12-tick release. */
final class MossveilFiberParticle extends SingleQuadParticle implements SigilGroup.Extent{
 MossveilFiberParticle(ClientLevel world,Vec3 at,Vec3 drift,int strand){super(world,at.x,at.y,at.z,SpellCircleParticle.particleSprite("mossveil_fiber"));lifetime=12;quadSize=.045F;hasPhysics=false;xd=drift.x;yd=drift.y;zd=drift.z;roll=oRoll=strand*.36F;}
 public void tick(){xo=x;yo=y;zo=z;oRoll=roll;if(age++>=lifetime){remove();return;}xd*=.86;zd*=.86;yd-=.0015;roll+=Math.sin(age*.6)*.035F;x+=xd;y+=yd;z+=zd;alpha=Math.min(1,age*.6F)*Math.min(1,(1-age/(float)lifetime)*2);if(MagicQuality.reducedFlash)alpha*=.7F;}
 protected Layer getLayer(){return Layer.TRANSLUCENT;}
 public ParticleRenderType getGroup(){return SigilGroup.TYPE;}
 public double centreX(){return x;}public double centreY(){return y;}public double centreZ(){return z;}public double reach(){return .09;}
 public static final class Provider implements ParticleProvider<net.minecraft.core.particles.SimpleParticleType>{public Particle createParticle(net.minecraft.core.particles.SimpleParticleType o,ClientLevel l,double x,double y,double z,double vx,double vy,double vz,net.minecraft.util.RandomSource random){return new MossveilFiberParticle(l,new Vec3(x,y,z),new Vec3(vx,vy,vz),0);}}
}
