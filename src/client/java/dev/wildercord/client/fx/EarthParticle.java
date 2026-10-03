package dev.wildercord.client.fx;

import dev.wildercord.content.EarthOption;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.util.RandomSource;

/** Faceted stone, soil, roots, bone and crystals: all use the world's light, with physical debris motion. */
public final class EarthParticle extends SingleQuadParticle implements SigilGroup.Extent {
 private final EarthOption material;
 EarthParticle(ClientLevel level,double x,double y,double z,EarthOption option) {
  super(level,x,y,z,SpellCircleParticle.particleSprite("earth_"+option.style()+"_0"));
  material=option;lifetime=option.lifetime();hasPhysics=false;quadSize=option.size();
  setColor((option.color()>>16&255)/255F,(option.color()>>8&255)/255F,(option.color()&255)/255F);
  xd=option.drift().x;yd=option.drift().y;zd=option.drift().z;
  roll=option.style()==EarthOption.FRACTURE || option.style()==EarthOption.ROOT?0:random.nextFloat()*.35F-.175F;oRoll=roll;
 }
 @Override public void tick() {
  xo=x;yo=y;zo=z;oRoll=roll;if(age++>=lifetime){remove();return;}
  switch(material.style()) {
   case EarthOption.ROCK,EarthOption.GRIT,EarthOption.BONE -> {yd-=.0025;roll+=material.spin();}
   case EarthOption.SLAB -> {yd-=.0008;roll+=material.spin()*.35F;}
   case EarthOption.DUST -> {yd-=.0004;xd*=.82;zd*=.82;quadSize=material.size()*(.7F+age/(float)lifetime*.5F);}
   case EarthOption.ROOT -> {xd*=.8;yd*=.8;zd*=.8;}
   case EarthOption.CRYSTAL -> {roll+=material.spin()*.2F;}
   case EarthOption.FRACTURE -> {xd*=.7;yd*=.7;zd*=.7;}
  }
  x+=xd;y+=yd;z+=zd;
  float remaining=1-age/(float)lifetime;
  alpha=Math.min(1,age*.8F)*Math.min(1,remaining*3);
  if(material.style()==EarthOption.DUST)alpha*=.62F;
  if(MagicQuality.reducedFlash)alpha*=.72F;
  setSprite(SpellCircleParticle.particleSprite("earth_"+material.style()+"_"+(age<lifetime/2?0:1)));
 }
 @Override protected Layer getLayer(){return Layer.TRANSLUCENT;}
 @Override public int getLightCoords(float partial){return super.getLightCoords(partial);}
 @Override public ParticleRenderType getGroup(){return SigilGroup.TYPE;}
 @Override public double centreX(){return x;}
 @Override public double centreY(){return y;}
 @Override public double centreZ(){return z;}
 @Override public double reach(){return material.size()*1.8;}
 public static final class Provider implements ParticleProvider<EarthOption> {
  @Override public Particle createParticle(EarthOption option,ClientLevel level,double x,double y,double z,double vx,double vy,double vz,RandomSource random){return new EarthParticle(level,x,y,z,option);}
 }
}
