package dev.wildercord.client.fx;

import dev.wildercord.content.LifeOption;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.RandomSource;

/** Living matter has material-specific movement; all silhouettes use the sampled world light. */
public final class LifeParticle extends SingleQuadParticle implements SigilGroup.Extent {
 private final LifeOption material;
 private final java.util.UUID outcomeSource=LifeOwnerClearance.source();
 private final boolean outcomeOwner=LifeOwnerClearance.owner();
 LifeParticle(ClientLevel level,double x,double y,double z,LifeOption option) {
  super(level,x,y,z,SpellCircleParticle.particleSprite("life_"+option.style()+"_0"));
  material=option;lifetime=option.lifetime();hasPhysics=false;quadSize=option.size();
  setColor((option.color()>>16&255)/255F,(option.color()>>8&255)/255F,(option.color()&255)/255F);
  xd=option.drift().x;yd=option.drift().y;zd=option.drift().z;oRoll=roll=option.spin();
 }
 @Override public void extract(QuadParticleRenderState state,Camera camera,float partial){
  if(!outcomeOwner||LifeOwnerClearance.visible(outcomeSource,level,camera,partial,xo,yo,zo,x,y,z))super.extract(state,camera,partial);
 }
 @Override public void tick() {
  xo=x;yo=y;zo=z;oRoll=roll;if(age++>=lifetime){remove();return;}
  switch(material.style()) {
   case LifeOption.LEAF -> {xd+=Math.sin(age*1.2)*.002;yd-=.001;roll+=material.spin()*Math.cos(age*.7);}
   case LifeOption.SEED -> {yd-=.0028;roll+=material.spin();}
   case LifeOption.PETAL -> {yd-=.0007;zd+=Math.sin(age*.8)*.0015;roll+=material.spin()*.6;}
   case LifeOption.SPORE -> {yd+=.0004;xd*=.92;zd*=.92;quadSize=material.size()*(.75F+age/(float)lifetime*.3F);}
   case LifeOption.VINE -> {xd*=.72;yd*=.72;zd*=.72;}
   case LifeOption.SAP -> {yd-=.0019;quadSize=material.size()*(1-age/(float)lifetime*.2F);}
   case LifeOption.TISSUE -> {xd*=.85;yd*=.85;zd*=.85;quadSize=material.size()*(.9F+.08F*(float)Math.sin(age*.8));}
   case LifeOption.THORN -> {roll+=material.spin()*.2;yd-=.001;}
  }
  x+=xd;y+=yd;z+=zd;
  alpha=Math.min(1,age*.8F)*Math.min(1,(1-age/(float)lifetime)*3);
  if(material.style()==LifeOption.SPORE)alpha*=.65F;
  if(MagicQuality.reducedFlash)alpha*=.72F;
  setSprite(SpellCircleParticle.particleSprite("life_"+material.style()+"_"+(age<lifetime/2?0:1)));
 }
 @Override protected Layer getLayer(){return Layer.TRANSLUCENT;}
 @Override public int getLightCoords(float partial){return super.getLightCoords(partial);}
 @Override public ParticleRenderType getGroup(){return SigilGroup.TYPE;}
 @Override public double centreX(){return x;}
 @Override public double centreY(){return y;}
 @Override public double centreZ(){return z;}
 @Override public double reach(){return material.size()*1.8;}
 public static final class Provider implements ParticleProvider<LifeOption> {
  @Override public Particle createParticle(LifeOption option,ClientLevel level,double x,double y,double z,double vx,double vy,double vz,RandomSource random){return new LifeParticle(level,x,y,z,option);}
 }
}
