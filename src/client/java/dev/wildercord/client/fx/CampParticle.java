package dev.wildercord.client.fx;
import dev.wildercord.content.CampOption;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.util.RandomSource;
/** Physical paper hinges and copper; mana fibres tighten rather than billow as recolored light. */
public final class CampParticle extends SingleQuadParticle implements SigilGroup.Extent {
 private final CampOption material;
 CampParticle(ClientLevel l,double x,double y,double z,CampOption m){super(l,x,y,z,SpellCircleParticle.particleSprite("camp_"+m.style()+"_0"));material=m;lifetime=m.lifetime();hasPhysics=false;quadSize=m.size();setColor((m.color()>>16&255)/255F,(m.color()>>8&255)/255F,(m.color()&255)/255F);xd=m.drift().x;yd=m.drift().y;zd=m.drift().z;roll=oRoll=m.spin();}
 @Override public void tick(){xo=x;yo=y;zo=z;oRoll=roll;if(age++>=lifetime){remove();return;}
  switch(material.style()){
   case CampOption.PAPER->{roll+=material.spin()*Math.cos(age*.8);xd*=.88;yd*=.88;zd*=.88;}
   case CampOption.STAPLE->{xd*=.65;yd*=.65;zd*=.65;}
   case CampOption.TEAR->{yd-=.0015;roll+=material.spin();}
   case CampOption.FIBER->{xd*=.92;yd*=.92;zd*=.92;quadSize=material.size()*(.85F+.15F*(float)Math.sin(age*.7));}
   case CampOption.COMB->{xd*=.7;yd*=.7;zd*=.7;roll+=material.spin()*.2;}
   case CampOption.KNOT->{xd*=.86;yd*=.86;zd*=.86;roll+=material.spin()*.35;}
  }
  x+=xd;y+=yd;z+=zd;alpha=Math.min(1,age*.8F)*Math.min(1,(1-age/(float)lifetime)*3);if(MagicQuality.reducedFlash)alpha*=.72F;
  setSprite(SpellCircleParticle.particleSprite("camp_"+material.style()+"_"+(age<lifetime/2?0:1)));
 }
 @Override protected Layer getLayer(){return Layer.TRANSLUCENT;}
 @Override public ParticleRenderType getGroup(){return SigilGroup.TYPE;}
 @Override public double centreX(){return x;}@Override public double centreY(){return y;}@Override public double centreZ(){return z;}@Override public double reach(){return material.size()*1.8;}
 public static final class Provider implements ParticleProvider<CampOption>{@Override public Particle createParticle(CampOption o,ClientLevel l,double x,double y,double z,double vx,double vy,double vz,RandomSource r){return new CampParticle(l,x,y,z,o);}}
}
