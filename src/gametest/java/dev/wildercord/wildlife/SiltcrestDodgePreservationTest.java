package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.SiltcrestNative.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.phys.Vec3;
/** Actual unforced fish evasion; finite independent habitat trials never move fish or force a strike. */
public final class SiltcrestDodgePreservationTest implements FabricClientGameTest {
 private SiltcrestBittern bird;private Cod quarry;private Vec3 committed;private static int calls;
 private static final class WitnessCod extends Cod{WitnessCod(ServerLevel l){super(EntityTypes.COD,l);}@Override public boolean hurtServer(ServerLevel l,DamageSource d,float amount){if(d.getEntity() instanceof SiltcrestBittern)calls++;return super.hurtServer(l,d,amount);}}
 public void runTest(ClientGameTestContext c){boolean proved=false;
  for(int trial=0;trial<8&&!proved;trial++)try(var w=c.worldBuilder().create()){
   c.waitTicks(25);calls=0;w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 18000");w.getServer().runCommand("weather clear");
   w.getServer().runOnServer(s->{floor(s.overworld());observer(s.getPlayerList().getPlayers().getFirst(),new Vec3(.5,101.7,.5));for(int i=0;i<3;i++){var fish=new WitnessCod(s.overworld());fish.snapTo(1.5+i,100.1,.5,0,0);s.overworld().addFreshEntity(fish);}bird=bird(s.overworld(),.5,.5);});
   await(c,w,240,s->bird.pose()==SiltcrestBittern.COILING||calls>0,"Ordinary native stalking reaches a genuine commitment or actual catch");
   if(w.getServer().computeOnServer(s->calls>0))continue;
   w.getServer().runOnServer(s->{quarry=(Cod)field(bird,"quarry");committed=(Vec3)field(bird,"committed");check(quarry!=null&&committed!=null,"Actual live production commitment is observed");});
   for(int n=0;n<24;n++){boolean escaped=w.getServer().computeOnServer(s->bird.pose()==SiltcrestBittern.COILING&&quarry.isAlive()&&quarry.getBoundingBox().getCenter().distanceToSqr(committed)>.36);if(escaped){
     await(c,w,24,s->bird.pose()!=SiltcrestBittern.COILING,"Original finite coil resolves after actual fish movement");
     boolean refused=w.getServer().computeOnServer(s->calls==0&&quarry.isAlive()&&quarry.getHealth()==3&&bird.pose()==SiltcrestBittern.IDLE&&bird.huntReady()<=s.overworld().getGameTime()+200);
     if(refused)proved=true;break;
    }if(w.getServer().computeOnServer(s->bird.pose()!=SiltcrestBittern.COILING))break;c.waitTicks(1);}
  }
  check(proved,"At least one actually observed unforced >.6 fish escape must retain pre-damage refusal and finite failed-attempt rest");
 }
 private static Object field(Object o,String name){try{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
}
