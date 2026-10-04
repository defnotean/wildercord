package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.SiltcrestNative.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.fish.Cod;
import java.util.*;
/** Genuine AI damage against instrumented live vanilla Cod subclasses; synchronous fault gates, not natural distribution proof. */
public final class SiltcrestDamageAdmissionTest implements FabricClientGameTest {
 private static int MODE,CALLS;private static SiltcrestBittern source;private static CallbackCod foreignVictim;
 private static final class CallbackCod extends Cod {
  CallbackCod(ServerLevel l){super(EntityTypes.COD,l);}
  @Override public boolean hurtServer(ServerLevel l,DamageSource d,float amount){if(!(d.getEntity() instanceof SiltcrestBittern bird))return super.hurtServer(l,d,amount);CALLS++;source=bird;check(amount==4,"Actual committed bird strike supplies original fixed damage");if(MODE==4)return false;boolean hit=super.hurtServer(l,d,amount);if(MODE==1)bird.teleportTo(12.5,101,6.5);if(MODE==2)bird.discard();if(MODE==3)bird.hurtServer(l,l.damageSources().playerAttack(l.getServer().getPlayerList().getPlayers().getFirst()),1);if(MODE==5){check(hit&&getHealth()==16,"Original real four damage is admitted and nonfatal against supplied20-health wildCod");foreignVictim=this;boolean killed=super.hurtServer(l,l.damageSources().genericKill(),Float.MAX_VALUE);check(killed&&getHealth()<=0&&getLastDamageSource()!=d,"Actual nested foreign fatal damage owns final death");}return hit;}
 }
 public void runTest(ClientGameTestContext c){try{for(int mode=1;mode<=5;mode++){MODE=mode;CALLS=0;source=null;foreignVictim=null;try(var w=c.worldBuilder().create()){c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 18000");w.getServer().runCommand("weather clear");
  w.getServer().runOnServer(s->{floor(s.overworld());observer(s.getPlayerList().getPlayers().getFirst(),new net.minecraft.world.phys.Vec3(.5,101.7,-1));for(int i=0;i<3;i++){var fish=new CallbackCod(s.overworld());if(MODE==5){fish.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);fish.setHealth(20);}fish.snapTo(1.5+i,100.1,.5,0,0);s.overworld().addFreshEntity(fish);}bird(s.overworld(),.5,.5);});
  await(c,w,500,s->CALLS>0,"Actual retained stalking must reach a real instrumented fish and call real damage");
  w.getServer().runOnServer(s->{check(source!=null&&source.huntReady()<=s.overworld().getGameTime()+200,"Refused hurt or synchronous source movement/removal/reaction cannot own a successful meal deadline");check(source.pose()!=SiltcrestBittern.PREENING,"Invalidated source cannot manufacture success preening");if(MODE==5)check(foreignVictim!=null&&foreignVictim.getHealth()<=0&&foreignVictim.getLastDamageSource().getEntity()!=source,"Foreign fatal callback preserves actual real death without awarding bird meal or preen");});c.waitTicks(20);w.getServer().runOnServer(s->check(CALLS==1,"One committed attempt cannot issue another hurt inside its finite failed-attempt rest"));
 }}}finally{MODE=0;CALLS=0;source=null;foreignVictim=null;}}
}
