package dev.wildercord.aura.world;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Actual native injury -> Warning -> Charge/Leap. No pose, deadline, health or epoch injection. */
public final class AuraBeastCallbackTest implements FabricClientGameTest {
 private static boolean registered,observed;
 private static AuraBeast source;
 private static int armed;
 private static UUID receiver;
 private static Entity moved;
 private static final Set<UUID> expected=new HashSet<>();
 private final List<Zombie> peers=new ArrayList<>();
 @Override public void runTest(ClientGameTestContext c){
  register();try{for(boolean gale:new boolean[]{false,true})for(int mode=1;mode<=4;mode++)exercise(c,gale,mode);}
  finally{clear();}
 }
 private static void register(){if(registered)return;registered=true;
  ServerLivingEntityEvents.AFTER_DAMAGE.register((target,damage,base,taken,blocked)->{
   if(armed==0||source==null||damage.getEntity()!=source||damage.getDirectEntity()!=source||!damage.is(DamageTypes.MOB_ATTACK)||blocked||taken<=0)return;
   check(expected.contains(target.getUUID()),"Actual beast attack callback belongs to supplied known pool");
   check(source.pose()==(source.gale()?BeastRules.LEAP:BeastRules.CHARGE),"Callback occurs in actual authored attack phase");
   check(close(target.getHealth(),source.gale()?15:13),"Exact original unarmored Leap5/Charge7 health loss precedes callback");
   int mode=armed;armed=0;observed=true;receiver=target.getUUID();
   if(mode==1)source.discard();else if(mode==2)target.discard();else{
    var destination=((ServerLevel)target.level()).getServer().getLevel(Level.NETHER);var actor=mode==3?source:target;var uuid=actor.getUUID();
    moved=actor.teleport(new TeleportTransition(destination,new Vec3(.5,101,.5),Vec3.ZERO,0,0,Set.<Relative>of(),TeleportTransition.PLACE_PORTAL_TICKET));
    check(moved!=null&&moved!=actor&&moved.isAlive()&&!moved.isRemoved()&&moved.level()==destination&&moved.getUUID().equals(uuid),"Real callback transfer returns living same-UUID destination clone");
    check(actor.isRemoved(),"Original receiver/source departs its native action world");
   }
  });
 }
 private void exercise(ClientGameTestContext c,boolean gale,int mode){
  try(var w=c.worldBuilder().create()){
   c.waitTicks(25);configure(w.getServer());
   w.getServer().runOnServer(s->{arena(s.overworld());arena(s.getLevel(Level.NETHER));
    source=(gale?AuraBeasts.GALECLAW:AuraBeasts.STONEHORN).create(s.overworld(),EntitySpawnReason.COMMAND);
    check(source!=null,"Actual registered creature factory");source.snapTo(.5,101,.5,0,0);s.overworld().addFreshEntity(source);
    var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.teleportTo(s.overworld(),10,101,10,Set.<Relative>of(),0,0,false);
   });c.waitTicks(5);
   w.getServer().runOnServer(s->{
    var l=s.overworld();var attacker=peer(l,source.getX(),source.getZ()+2);peers.add(attacker);
    check(source.hurtServer(l,l.damageSources().mobAttack(attacker),1),"Actual admitted native injury earns retaliation");
    peers.add(peer(l,source.getX()-.65,source.getZ()+2));peers.add(peer(l,source.getX()+.65,source.getZ()+2));
    expected.clear();for(var e:peers){expected.add(e.getUUID());check(e.getHealth()==20&&e.getAttributeValue(Attributes.ARMOR)==0&&!e.isOnFire(),"Known peer begins at genuine twenty health and zero armor");}
    var pool=AuraBeastQueries.complete(l,LivingEntity.class,source.getBoundingBox().inflate(8),source,e->source.valid(e));
    check(pool.size()==3&&pool.stream().allMatch(e->expected.contains(e.getUUID())),"Actual complete native receiver pool contains exactly known three peers");
    observed=false;receiver=null;moved=null;armed=mode;
   });
   await(c,w.getServer(),()->source.pose()==BeastRules.WARN,30,"Genuine injury starts actual full Warning");
   await(c,w.getServer(),()->observed,100,"Real Charge/Leap admits original native damage before lifecycle injection");
   c.waitTicks(45);
   w.getServer().runOnServer(s->{
    float remaining=gale?15:13;int otherHits=0;
    for(var e:peers)if(!e.getUUID().equals(receiver)){
     if(mode==1||mode==3)check(e.getHealth()==20,"Source removal/transfer cancels remaining and later attack receivers");
     else{check(e.getHealth()==20||close(e.getHealth(),remaining),"Other peers receive at most one original strike");if(e.getHealth()<20)otherHits++;}
    }
    if(mode==2||mode==4)check(otherHits>0,"Victim departure allows unrelated actual receivers to complete");
    if(mode==1)check(source.isRemoved(),"Removed source stays removed");
    if(mode==3||mode==4){var destination=s.getLevel(Level.NETHER);check(destination.getEntity(moved.getUUID())==moved&&moved.isAlive(),"Ticketed destination clone becomes exact tracked native entity");}
    if(mode==4)check(close(((LivingEntity)moved).getHealth(),remaining),"Transferred target keeps one strike with no old-world repeated damage");
    if(mode==2)check(peers.stream().filter(e->e.getUUID().equals(receiver)).findFirst().orElseThrow().isRemoved(),"Real callback receiver removal remains authoritative");
   });
  }finally{clear();}
 }
 private static void await(ClientGameTestContext c,TestServerContext s,java.util.function.BooleanSupplier condition,int ticks,String why){for(int n=0;n<ticks;n+=2){if(s.computeOnServer(server->condition.getAsBoolean()))return;c.waitTicks(2);}check(s.computeOnServer(server->condition.getAsBoolean()),why);}
 private static Zombie peer(ServerLevel l,double x,double z){var e=EntityTypes.ZOMBIE.create(l,EntitySpawnReason.COMMAND);check(e!=null,"Native receiver factory");e.setCustomName(net.minecraft.network.chat.Component.literal("Native supplied Aura receiver"));e.setNoAi(true);e.setPersistenceRequired();e.getAttribute(Attributes.ARMOR).setBaseValue(0);e.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0);e.snapTo(x,101,z,0,0);check(l.addFreshEntity(e),"Named native receiver enters world");check(e.getHealth()==20&&e.getMaxHealth()==20&&e.getAbsorptionAmount()==0&&e.getAttributeValue(Attributes.ARMOR)==0&&e.getAttributeValue(Attributes.ARMOR_TOUGHNESS)==0,"Exact genuine receiver health/armor/absorption receipt after admission");return e;}
 private static void arena(ServerLevel l){check(l!=null,"Actual destination level");for(int x=-14;x<=14;x++)for(int z=-14;z<=14;z++){l.setBlock(new BlockPos(x,100,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);for(int y=101;y<=106;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}}
 private static void configure(TestServerContext s){s.runCommand("difficulty normal");s.runCommand("gamerule spawn_mobs false");s.runCommand("gamerule fall_damage false");s.runCommand("gamerule natural_health_regeneration false");s.runCommand("time set 18000");}
 private void clear(){armed=0;source=null;observed=false;receiver=null;moved=null;expected.clear();peers.clear();}
 private static boolean close(float a,float b){return Math.abs(a-b)<.001;}
 private static void check(boolean v,String message){if(!v)throw new AssertionError(message);}
}
