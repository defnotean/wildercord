package dev.wildercord.wildlife;

import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Real injury, ordinary AI warning and physical five-damage sweep; no forced phase/deadline. */
public final class ReedbackSweepSafetyTest implements FabricClientGameTest {
 private static ReedbackCrab crab;private static Receiver victim;private static int armed=-1;private static boolean registered,observed;private static float admittedHealth;private static Entity moved;
 private static final class Receiver extends Zombie {
  int sweepKnockbacks;
  Receiver(ServerLevel l){super(EntityTypes.ZOMBIE,l);setNoAi(true);setPersistenceRequired();setCustomName(net.minecraft.network.chat.Component.literal("Supplied sweep receiver"));getAttribute(Attributes.ARMOR).setBaseValue(0);getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0);}
  @Override public void knockback(double amount,double x,double z,DamageSource damage,float scale){if(damage.getEntity()==crab&&Math.abs(amount-.45)<.000001)sweepKnockbacks++;super.knockback(amount,x,z,damage,scale);}
 }
 public void runTest(ClientGameTestContext c){register();try{acquisition(c);for(int mode=0;mode<=8;mode++)callback(c,mode);dense(c,12);dense(c,13);}finally{armed=-1;crab=null;victim=null;moved=null;}}
 private static void register(){if(registered)return;registered=true;ServerLivingEntityEvents.AFTER_DAMAGE.register((target,damage,base,taken,blocked)->{
  if(armed<0||target!=victim||crab==null||damage.getEntity()!=crab||damage.getDirectEntity()!=crab||!damage.is(DamageTypes.MOB_ATTACK)||blocked||taken<=0)return;
  check(crab.pose()==ReedbackCrab.SWEEP,"Callback belongs to real committed sweep");int mode=armed;armed=-1;observed=true;admittedHealth=target.getHealth();check(Math.abs(admittedHealth-15)<.001,"Callback follows genuine five-health physical strike");
  if(mode==1)crab.discard();else if(mode==2)victim.discard();else if(mode==3)crab.snapTo(crab.getX()+.1,crab.getY(),crab.getZ(),crab.getYRot(),crab.getXRot());else if(mode==4)victim.snapTo(victim.getX()+.1,victim.getY(),victim.getZ(),victim.getYRot(),victim.getXRot());else if(mode==5)check(crab.answerMagic(true),"Actual water response changes the committed action during callback");else if(mode==6){var e=EntityTypes.ZOMBIE.create((ServerLevel)crab.level(),EntitySpawnReason.COMMAND);e.setNoAi(true);e.snapTo(crab.getX()+1,crab.getY(),crab.getZ(),0,0);((ServerLevel)crab.level()).addFreshEntity(e);crab.setTarget(e);}else if(mode==7||mode==8){var actor=mode==7?crab:victim;var old=(ServerLevel)actor.level();var destination=old.getServer().getLevel(Level.NETHER);moved=actor.teleport(new net.minecraft.world.level.portal.TeleportTransition(destination,new Vec3(.5,100,.5),Vec3.ZERO,0,0,Set.<Relative>of(),net.minecraft.world.level.portal.TeleportTransition.PLACE_PORTAL_TICKET));check(moved!=null&&moved!=actor&&moved.getUUID().equals(actor.getUUID())&&moved.level()==destination&&moved.isAlive()&&!moved.isRemoved()&&(actor.isRemoved()||actor.level()!=old),"Actual callback dimension transfer returns live authoritative clone");}
 });}
 private void callback(ClientGameTestContext c,int mode){try(var w=c.worldBuilder().create()){
  setup(c,w);w.getServer().runOnServer(s->{victim=new Receiver(s.overworld());victim.snapTo(.5,100,1.5,0,0);s.overworld().addFreshEntity(victim);check(victim.getHealth()==20,"Actual supplied post-admission baseline remains20");observed=false;admittedHealth=0;moved=null;armed=mode;check(crab.hurtServer(s.overworld(),victim.damageSources().mobAttack(victim),1),"Real physical injury primes ordinary retaliation");});
  await(c,w,()->observed,"Native sweep must invoke real callback");
  w.getServer().runOnServer(s->{check(Math.abs(admittedHealth-15)<.001,"Mutation does not undo admitted strike");check(victim.sweepKnockbacks==(mode==0?1:0),"Only unchanged actors receive original once-only follow-up knockback");if(mode==5)check(crab.pose()==ReedbackCrab.CALM,"Callback calm remains authoritative");});
  if(mode==7||mode==8){c.waitTicks(45);w.getServer().runOnServer(s->check(s.getLevel(Level.NETHER).getEntity(moved.getUUID())==moved&&moved.isAlive()&&!moved.isRemoved(),"Actual destination tracking confirmed after native portal ticks"));}
 }finally{armed=-1;crab=null;victim=null;moved=null;}}
 private void dense(ClientGameTestContext c,int count){try(var w=c.worldBuilder().create()){
  setup(c,w);var peers=new ArrayList<Zombie>();w.getServer().runOnServer(s->{victim=new Receiver(s.overworld());victim.snapTo(.5,100,1.5,0,0);s.overworld().addFreshEntity(victim);check(victim.getHealth()==20,"Actual dense supplied receiver baseline remains20");peers.add(victim);for(int n=1;n<count;n++){var e=EntityTypes.ZOMBIE.create(s.overworld(),EntitySpawnReason.COMMAND);e.setNoAi(true);e.setCustomName(net.minecraft.network.chat.Component.literal("Supplied sweep peer"));e.snapTo(1.5,100,1.5,0,0);s.overworld().addFreshEntity(e);peers.add(e);}var pool=WetlandQueries.scan(s.overworld(),LivingEntity.class,crab.getBoundingBox().inflate(2.8),crab);check(pool.enumerated()==count&&pool.saturated()==(count>12),"Actual entire peer pool matches completeness boundary");check(crab.hurtServer(s.overworld(),victim.damageSources().mobAttack(victim),1),"Dense case starts through genuine injury");});
  await(c,w,()->crab.pose()==ReedbackCrab.WARNING,"Actual readable warning remains");await(c,w,()->crab.pose()==ReedbackCrab.SWEEP,"Finite native commitment remains at saturation");
  w.getServer().runOnServer(s->{var pool=WetlandQueries.scan(s.overworld(),LivingEntity.class,crab.getBoundingBox().inflate(2.8),crab);check(pool.enumerated()==count,"Known dense peers remain in actual query during sweep");check(Math.abs(victim.getHealth()-(count==12?15:20))<.001,"Complete12 admits real five-damage strike; saturated13 harms no hidden prefix");for(var e:peers)if(e!=victim)check(e.getHealth()==20,"Non-player nonquarry remains unharmed");});
 }finally{crab=null;victim=null;}}
 private void acquisition(ClientGameTestContext c){try(var w=c.worldBuilder().create()){
  setup(c,w);var peers=new ArrayList<net.minecraft.server.level.ServerPlayer>();
  w.getServer().runOnServer(s->{var l=s.overworld();var viewer=s.getPlayerList().getPlayers().getFirst();
   for(int n=0;n<13;n++){var peer=new net.fabricmc.fabric.api.entity.FakePlayer(l,new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes(("wet-query-"+n).getBytes(java.nio.charset.StandardCharsets.UTF_8)),"WetPeer"+n)){};peer.setGameMode(n==12?GameType.SURVIVAL:GameType.CREATIVE);peer.snapTo(n==12?.5:-3+(n%4)*1.5,100,n==12?2.5:-3+(n/4)*1.5,0,0);viewer.connection.send(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(peer)));l.addNewPlayer(peer);peers.add(peer);}
   var pool=WetlandQueries.scan(l,net.minecraft.world.entity.player.Player.class,crab.getBoundingBox().inflate(5),null);check(pool.saturated()&&pool.enumerated()==13,"Actual thirteen-player acquisition pool includes benign players before eligibility");
  });
  c.waitTicks(25);w.getServer().runOnServer(s->{check(crab.getTarget()==null&&crab.pose()!=ReedbackCrab.WARNING,"Dense player pool refuses rather than losing an eligible hidden-prefix target");peers.getFirst().discard();var pool=WetlandQueries.scan(s.overworld(),net.minecraft.world.entity.player.Player.class,crab.getBoundingBox().inflate(5),null);check(!pool.saturated()&&pool.enumerated()==12&&pool.entities().contains(peers.getLast()),"Removing one actual peer restores complete pool and retains eligible recipient");});
  await(c,w,()->crab.pose()==ReedbackCrab.WARNING,"Complete sparse player pool acquires actual survival target and warns");w.getServer().runOnServer(s->check(crab.getTarget()==peers.getLast(),"Actual owner query chooses its sole eligible tracked player"));
 }finally{crab=null;}}
 private void setup(ClientGameTestContext c,TestSingleplayerContext w){c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 18000");w.getServer().runCommand("difficulty normal");w.getServer().runOnServer(s->{arena(s.overworld());arena(s.getLevel(Level.NETHER));var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.teleportTo(s.overworld(),8,100,8,Set.<Relative>of(),0,0,false);crab=ReedbackContent.CRAB.create(s.overworld(),EntitySpawnReason.COMMAND);crab.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);crab.snapTo(.5,100,.5,0,0);s.overworld().addFreshEntity(crab);});c.waitTicks(5);}
 private static void arena(ServerLevel l){for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++){l.setBlock(new BlockPos(x,99,z),Blocks.MUD.defaultBlockState(),2);for(int y=100;y<=104;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}}
 private static void await(ClientGameTestContext c,TestSingleplayerContext w,java.util.function.BooleanSupplier yes,String why){for(int n=0;n<65;n++){c.waitTicks(1);if(w.getServer().computeOnServer(s->yes.getAsBoolean()))return;}throw new AssertionError(w.getServer().computeOnServer(s->why+" mode="+armed+" source="+(crab==null?null:crab.position())+" pose="+(crab==null?-1:crab.pose())+" quarry="+(crab==null?null:crab.getTarget())+" recipient="+(victim==null?null:victim.position())+" health="+(victim==null?0:victim.getHealth())));}
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
