package dev.wildercord.aura.world;

import dev.wildercord.wildlife.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** Genuine Leap5: positive-health discard is not a kill; a second naturally earned attack is. */
public final class GaleclawMealCallbackTest implements FabricClientGameTest {
 private static boolean registered,discard,foreign;
 private static Galeclaw source;private static Rimehare prey;
 private static int hits,fatalEpoch,fatalReceipts;private static long mealBefore;private static net.minecraft.world.damagesource.DamageSource fatalDamage;
 public void runTest(ClientGameTestContext c){register();try{exercise(c,true,false);exercise(c,false,false);exercise(c,false,true);}finally{clear();}}
 private static void register(){if(registered)return;registered=true;
  // Fabric's AFTER_DAMAGE explicitly skips isDeadOrDying. Capture the original five-health request,
  // then credit it only when actual AFTER_DEATH confirms the identical admitted damage/source/attack.
  ServerLivingEntityEvents.ALLOW_DAMAGE.register((target,damage,amount)->{
   if(source!=null&&prey!=null&&target==prey&&!discard&&!foreign&&hits==1&&damage.getEntity()==source&&damage.getDirectEntity()==source&&damage.is(DamageTypes.MOB_ATTACK)){
    check(amount==5&&prey.getHealth()==1&&source.pose()==BeastRules.LEAP,"Second earned native Leap requests its original five health against the genuine remaining one-health prey");fatalDamage=damage;fatalEpoch=source.attackEpoch();
   }return true;
  });
  ServerLivingEntityEvents.AFTER_DEATH.register((target,damage)->{
   if(source==null||prey==null||target!=prey||damage!=fatalDamage)return;
   check(!discard&&!foreign&&hits==1&&fatalReceipts==0&&damage.getEntity()==source&&damage.getDirectEntity()==source&&damage.is(DamageTypes.MOB_ATTACK)&&target.level()==source.level()&&target.getHealth()<=0&&source.attackActive((ServerLevel)source.level(),fatalEpoch,BeastRules.LEAP),"Only the exact second native Leap damage identity and live owned attack yields the actual fatal receipt");
   fatalReceipts++;hits++;
  });
  ServerLivingEntityEvents.AFTER_DAMAGE.register((target,damage,base,taken,blocked)->{
  if(source==null||prey==null||target!=prey||damage.getEntity()!=source||damage.getDirectEntity()!=source||!damage.is(DamageTypes.MOB_ATTACK)||taken<=0||blocked)return;
  check(source.pose()==BeastRules.LEAP,"Actual registered predator is in its real committed Leap");
  hits++;if(hits==1){check(prey.getHealth()==1,"Original six-health prey suffers exactly original five-health nonlethal strike");check(source.fedUntil()==mealBefore,"No meal was fabricated before the nonlethal damage callback");if(discard){prey.discard();check(prey.getHealth()==1&&!prey.isAlive()&&prey.isRemoved(),"Actual discard keeps positive health while changing removed/isAlive status");}if(foreign){boolean killed=prey.hurtServer((ServerLevel)source.level(),source.level().damageSources().genericKill(),Float.MAX_VALUE);check(killed&&prey.getHealth()<=0&&prey.getLastDamageSource()!=damage,"Actual nested foreign fatal damage owns final death after admitted nonlethal Leap5");}}
  else{check(hits==2&&prey.getHealth()<=0,"Only a second original native strike genuinely kills this prey");}
 });}
 private static void exercise(ClientGameTestContext c,boolean remove,boolean foreignKill){try(var w=c.worldBuilder().create()){
  c.waitTicks(25);w.getServer().runCommand("difficulty normal");w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule fall_damage false");w.getServer().runCommand("time set 18000");
  w.getServer().runOnServer(s->{var l=s.overworld();for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++){l.setBlock(new BlockPos(x,100,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);for(int y=101;y<=105;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}
   var player=s.getPlayerList().getPlayers().getFirst();player.setGameMode(GameType.CREATIVE);player.teleportTo(l,10,101,10,Set.<Relative>of(),0,0,false);
   source=AuraBeasts.GALECLAW.create(l,EntitySpawnReason.COMMAND);check(source!=null,"Registered predator factory");source.snapTo(.5,101,.5,0,0);source.setPersistenceRequired();check(l.addFreshEntity(source),"Native predator is tracked");
  });c.waitTicks(5);
  w.getServer().runOnServer(s->{var l=s.overworld();prey=Wildlife.RIMEHARE.create(l,EntitySpawnReason.COMMAND);check(prey!=null,"Registered prey factory");prey.setNoAi(true);prey.setPersistenceRequired();prey.snapTo(source.getX(),101,source.getZ()+2,0,0);check(l.addFreshEntity(prey),"Native recipient is tracked");check(prey.getHealth()==6&&prey.getMaxHealth()==6,"Unmodified actual species health supplies the nonlethal control");mealBefore=source.fedUntil();check(mealBefore==0&&source.hungry(),"Fresh unmodified predator has no supplied meal deadline");discard=remove;foreign=foreignKill;hits=0;check(source.hurtServer(l,l.damageSources().mobAttack(prey),1),"Genuine injury starts ordinary predator retaliation");});
  boolean warning=false,recovery=false,secondWarning=false;int previous=BeastRules.IDLE,lastHits=-1,traceCount=0;
  for(int n=0;n<240;n++){
   int[] state=w.getServer().computeOnServer(s->new int[]{source.pose(),hits});
   if(traceCount<32&&(state[0]!=previous||state[1]!=lastHits)){final int sample=n;w.getServer().runOnServer(server->diagnostic(server,"transition sample="+sample+" remove="+remove+" foreign="+foreignKill));traceCount++;}lastHits=state[1];
   warning|=state[0]==BeastRules.WARN;
   if(state[1]>=1&&state[0]==BeastRules.RECOVER)recovery=true;
   if(recovery&&state[0]==BeastRules.WARN&&previous!=BeastRules.WARN)secondWarning=true;
   previous=state[0];
   if((remove||foreignKill)&&recovery||!remove&&!foreignKill&&state[1]>=2)break;
   c.waitTicks(1);
  }
  final boolean warned=warning,recovered=recovery,warnedAgain=secondWarning;
  w.getServer().runOnServer(s->{diagnostic(s,"terminal remove="+remove+" foreign="+foreignKill+" warned="+warned+" recovered="+recovered+" warnedAgain="+warnedAgain);check(warned&&recovered,"Actual encounter earns Warning, Leap and ordinary finite recovery");if(foreignKill){check(hits==1&&fatalReceipts==0&&prey.getHealth()<=0&&prey.getLastDamageSource().getEntity()!=source,"Actual foreign fatal callback follows one admitted real nonlethal strike");check(source.fedUntil()==mealBefore&&source.hungry(),"Foreign fatal owner grants no Galeclaw meal or renewal despite real death");}else if(remove){check(hits==1&&fatalReceipts==0&&prey.isRemoved()&&prey.getHealth()==1,"One genuine nonlethal strike precedes removal");check(source.fedUntil()==mealBefore&&source.hungry(),"Discarding a still-positive-health prey grants no meal or renewal");}else{check(warnedAgain&&hits==2&&fatalReceipts==1&&prey.getHealth()<=0,"Actual recovery and second earned Warning precede lethal native strike");check(source.fedUntil()>s.overworld().getGameTime()&&!source.hungry(),"A genuine earned kill preserves ordinary functional meal reward");}});
 }finally{clear();}}
 private static void diagnostic(net.minecraft.server.MinecraftServer server,String stage){
  String target=source.getTarget()==null?"null":body(source.getTarget());
  var l=(ServerLevel)source.level();var at=source.blockPosition();var under=at.below();var landing=BlockPos.containing(source.landing());
  System.out.println("GALECLAW_MEAL_DIAGNOSTIC "+stage+" time="+l.getGameTime()+" source="+body(source)+" prey="+body(prey)+" target="+target+" hits="+hits+" discard="+discard+" foreign="+foreign+" pose="+source.pose()+" elapsed="+source.elapsed(0)+" phaseLeft="+source.left+" attackEpoch="+source.attackEpoch()+" aggression="+source.aggression+" calm="+source.calmTicks()+" home="+source.home()+" landing="+source.landing()+" fedBefore="+mealBefore+" fed="+source.fedUntil()+" hungry="+source.hungry()+" navigationDone="+source.getNavigation().isDone()+" lineOfSight="+source.hasLineOfSight(prey)+" horizontalDistanceSquared="+prey.position().subtract(source.position()).multiply(1,0,1).lengthSqr()+" height="+(prey.getY()-source.getY())+" leapGeometry="+BeastRules.leapHit(prey.position().subtract(source.position()).multiply(1,0,1).lengthSqr(),prey.getY()-source.getY())+" floor="+l.getBlockState(under)+" floorShape="+l.getBlockState(under).getCollisionShape(l,under)+" landingFloor="+l.getBlockState(landing.below())+" landingCell="+l.getBlockState(landing)+" sourceCollisionFree="+l.noCollision(source,source.getBoundingBox())+" sourceTracked="+(l.getEntity(source.getUUID())==source)+" preyTracked="+(prey.level() instanceof ServerLevel pl&&pl.getEntity(prey.getUUID())==prey));
 }
 private static String body(net.minecraft.world.entity.LivingEntity e){return "{uuid="+e.getUUID()+",id="+e.getId()+",world="+e.level().dimension()+",hp="+e.getHealth()+",max="+e.getMaxHealth()+",alive="+e.isAlive()+",removed="+e.isRemoved()+",pos="+e.position()+",velocity="+e.getDeltaMovement()+",bounds="+e.getBoundingBox()+",ground="+e.onGround()+",horizontalCollision="+e.horizontalCollision+",verticalCollision="+e.verticalCollision+",hurtTime="+e.hurtTime+",tick="+e.tickCount+",noAI="+(e instanceof net.minecraft.world.entity.Mob m&&m.isNoAi())+",lastDamage="+e.getLastDamageSource()+"}";}
 private static void clear(){source=null;prey=null;discard=false;foreign=false;hits=0;mealBefore=0;fatalDamage=null;fatalEpoch=0;fatalReceipts=0;}
 private static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
}
