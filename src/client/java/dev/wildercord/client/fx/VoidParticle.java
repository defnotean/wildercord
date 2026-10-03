package dev.wildercord.client.fx;
import dev.wildercord.content.VoidOption;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.util.RandomSource;
import net.minecraft.util.LightCoordsUtil;

/** Original dark interiors, material edge contrast and timed spatial fragments; no screen overlay. */
public final class VoidParticle extends SingleQuadParticle implements SigilGroup.Extent {
 private final VoidOption material;
 VoidParticle(ClientLevel level,double x,double y,double z,VoidOption option){
  super(level,x,y,z,SpellCircleParticle.particleSprite("void_material_"+option.style()+"_0"));
  material=option;lifetime=option.lifetime();hasPhysics=false;quadSize=option.size();
  setColor((option.color()>>16&255)/255F,(option.color()>>8&255)/255F,(option.color()&255)/255F);
  xd=option.drift().x;yd=option.drift().y;zd=option.drift().z;oRoll=roll=option.spin();
 }
 @Override public void tick(){
  xo=x;yo=y;zo=z;oRoll=roll;if(age++>=lifetime){remove();return;}
  switch(material.style()){
   case VoidOption.FOLD -> {xd*=.75;yd*=.75;zd*=.75;quadSize=material.size()*(1-.15F*age/lifetime);}
   case VoidOption.JAW -> {roll+=material.spin()*.25;xd*=.85;yd*=.85;zd*=.85;}
   case VoidOption.TOOTH -> {yd-=.0005;roll+=material.spin()*.1;}
   case VoidOption.CLOTH -> {xd+=Math.sin(age*.9)*.0017;roll+=material.spin()*Math.cos(age*.55);}
   case VoidOption.HAZE -> {xd*=.93;zd*=.93;quadSize=material.size()*(.8F+.3F*age/lifetime);}
   case VoidOption.SHARD -> {roll+=material.spin();yd-=.0018;}
   case VoidOption.SCULK -> {roll+=material.spin()*.15;quadSize=material.size()*(.9F+.05F*(float)Math.sin(age*1.1));}
   case VoidOption.PRESSURE -> {quadSize=material.size()*(.7F+.3F*(float)Math.sin(age/(float)lifetime*Math.PI));}
   case VoidOption.SHELL -> {yd-=.0007;xd*=.9;zd*=.9;}
   case VoidOption.REMNANT -> {xd+=Math.cos(age*.6)*.001;yd+=.0005;roll+=material.spin()*.4;}
   case VoidOption.HOUND -> {yd+=Math.sin(age*.8)*.0006;roll+=material.spin()*.08;quadSize=material.size()*(1-.06F*age/lifetime);}
  }
  x+=xd;y+=yd;z+=zd;
  alpha=Math.min(1,age*.8F)*Math.min(1,(1-age/(float)lifetime)*3);
  if(material.style()==VoidOption.HAZE)alpha*=.65F;
  if(MagicQuality.reducedFlash)alpha*=.72F;
  setSprite(SpellCircleParticle.particleSprite("void_material_"+material.style()+"_"+(age<lifetime/2?0:1)));
 }
 @Override protected Layer getLayer(){return Layer.TRANSLUCENT;}
 @Override public int getLightCoords(float partial){return material.style()==VoidOption.SCULK || material.style()==VoidOption.PRESSURE?LightCoordsUtil.FULL_BRIGHT:super.getLightCoords(partial);}
 @Override public ParticleRenderType getGroup(){return SigilGroup.TYPE;}
 @Override public double centreX(){return x;}
 @Override public double centreY(){return y;}
 @Override public double centreZ(){return z;}
 @Override public double reach(){return material.size()*1.8;}
 public static final class Provider implements ParticleProvider<VoidOption>{
  @Override public Particle createParticle(VoidOption o,ClientLevel l,double x,double y,double z,double vx,double vy,double vz,RandomSource random){return new VoidParticle(l,x,y,z,o);}
 }
}
