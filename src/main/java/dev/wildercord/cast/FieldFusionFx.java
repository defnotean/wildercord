package dev.wildercord.cast;

import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.EarthOption;
import dev.wildercord.content.MaterialOption;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Material interactions at successful transactions. Every emission uses the shared recipient budget. */
final class FieldFusionFx {
 private FieldFusionFx() {}
 private static void dot(ServerLevel l,ParticleOptions option,Vec3 at){Fx.send(l,option,at,1,0,0);}
 private static MaterialOption water(float size){return new MaterialOption(MaterialOption.WATER,0x8ED9CF,size,12);}
 private static EarthOption earth(int style,int color,float size,Vec3 drift){return new EarthOption(style,color,size,14,drift,.035F);}
 private static void cue(ServerLevel l,Vec3 at,String id){Feels.sound(l,at,"fieldfusion_"+id+"_impact",.42F,1);}
 static void rootlet(ServerLevel l,Vec3 from,Vec3 to){
  // Hydration leads; a real fibrous tip follows along the bank rather than a colored ring.
  var d=to.subtract(from);
  for(int i=1;i<=7;i++){var at=from.add(d.scale(i/7.0));dot(l,water(.035F),at.add(0,.04,0));dot(l,earth(EarthOption.ROOT,0x849657,.07F,d.normalize().scale(.018)),at.add(0,.08,0));}
 }
 static void spring(ServerLevel l,Vec3 at){
  for(int i=0;i<8;i++){double a=i*Math.PI/4;dot(l,water(.055F),at.add(Math.cos(a)*.22,-.18,Math.sin(a)*.22));}
  cue(l,at,"springbed");
 }
 static void sieve(ServerLevel l,Vec3 from,Vec3 to){
  var d=to.subtract(from);
  for(int i=0;i<7;i++){double t=i/6.0;var at=from.add(d.scale(t)).add(0,.18*Math.sin(Math.PI*t),0);
   dot(l,new MaterialOption(i<3?MaterialOption.EMBER:MaterialOption.VOID,i<3?0xCF763D:0x564447,.055F,10),at);
   if(i<3)dot(l,earth(EarthOption.GRIT,0x54463B,.04F,new Vec3(0,-.02,0)),at.add(.035,.06,0));
  }
  cue(l,from,"cinder_sieve");
 }
 static void mercy(ServerLevel l,LivingEntity t){
  var at=t.position().add(0,.45,0);
  // Separate scorched tissue flakes and living leaves; heat never turns into an explosion.
  for(int i=0;i<6;i++){double a=i*2.39996;var p=at.add(Math.cos(a)*.35,i*.07,Math.sin(a)*.35);
   dot(l,earth(EarthOption.DUST,0x63564A,.055F,new Vec3(0,.014,0)),p);
   dot(l,new MaterialOption(MaterialOption.PETAL,0x92B77E,.075F,18),p.add(0,.13,0));
   if(i%2==0)dot(l,new MaterialOption(MaterialOption.EMBER,0xD7864C,.028F,6),p.add(0,.04,0));
  }
  cue(l,at,"ashen_mercy");
 }
 static void imprint(ServerLevel l,Vec3 at){
  // Two low root teeth and falling soil grains identify a grounded memory, with no magical floor ring.
  for(int side:new int[]{-1,1})for(int i=0;i<4;i++)dot(l,earth(EarthOption.ROOT,0x796249,.055F,new Vec3(0,-.008,0)),at.add(side*(.16+i*.045),.03,i*.09-.14));
  for(int i=0;i<3;i++)dot(l,earth(EarthOption.GRIT,0xB7A17B,.035F,new Vec3(0,-.04,0)),at.add((i-1)*.12,.28,0));
 }
 static void clockReturn(ServerLevel l,Vec3 from,Vec3 to){
  var d=to.subtract(from);for(int i=0;i<9;i++){var p=from.add(d.scale(i/8.0)).add(0,.12,0);
   dot(l,earth(EarthOption.GRIT,0xB4A17B,.045F,d.normalize().scale(.12)),p);
   if(i%2==0)dot(l,earth(EarthOption.ROOT,0x746146,.06F,d.normalize().scale(.07)),p);
  }cue(l,to,"clockroot");
 }
 static void lift(ServerLevel l,LivingEntity t){
  for(int i=0;i<8;i++){double a=i*.9;dot(l,new MaterialOption(MaterialOption.WIND,0xB9CEBD,.065F,16),t.position().add(Math.cos(a)*.3,i*.09,Math.sin(a)*.3));}
  tether(l,t,t.getY()+1.1);
 }
 static void tether(ServerLevel l,LivingEntity t,double anchorY){
  var base=new Vec3(t.getX(),Math.max(t.getY()+.08,anchorY-.7),t.getZ());
  for(int i=0;i<4;i++)dot(l,new MaterialOption(MaterialOption.VOID,0x5A505E,.035F,12),base.add(.2,i*.12,0));
  dot(l,new MaterialOption(MaterialOption.WIND,0xC2D9CC,.055F,12),base.add(.2,.48,0));
 }
 static void unlatch(ServerLevel l,LivingEntity t){
  var at=t.position().add(0,.35,0);for(int i=0;i<4;i++)dot(l,new MaterialOption(MaterialOption.WIND,0xC7DCCC,.065F,14),at.add((i-1.5)*.08,-i*.07,.12));cue(l,at,"skylatch");
 }
 static void sickle(ServerLevel l,Vec3 at,Direction direction,int row){
  var f=new Vec3(direction.getStepX(),0,direction.getStepZ());var r=new Vec3(-f.z,0,f.x);
  for(int i=0;i<9;i++){double t=i/8.0;var p=at.add(r.scale((t-.5)*2.5)).add(f.scale(.24*Math.sin(Math.PI*t))).add(0,.1,0);
   dot(l,new MaterialOption(MaterialOption.WIND,0xCED9B1,.065F,10),p);
   if(i%3==0)dot(l,earth(EarthOption.ROOT,0xA39353,.065F,f.scale(.025)),p.add(0,-.06,0));
  }if(row==0)cue(l,at,"thresherwind");
 }
 static void grain(ServerLevel l,Vec3 from,Vec3 to){
  var d=to.subtract(from);for(int i=0;i<5;i++){double t=i/4.0;dot(l,earth(EarthOption.GRIT,0xBCA76D,.045F,d.normalize().scale(.08)),from.add(d.scale(t)).add(0,.3*Math.sin(Math.PI*t),0));}
 }
}
