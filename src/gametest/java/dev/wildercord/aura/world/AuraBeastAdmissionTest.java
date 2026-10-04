package dev.wildercord.aura.world;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.*;

/** Actual native bounded query/whistle and authoritative feeding eligibility; one real client. */
public final class AuraBeastAdmissionTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext c){threads(c);queries(c);for(boolean gale:new boolean[]{false,true})feeding(c,gale);}
 private void threads(ClientGameTestContext c){try(var w=c.worldBuilder().create()){
  c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");
  var level=w.getServer().computeOnServer(server->{check(server.isSameThread(),"Actual level capture occurs on its owning server thread");return server.overworld();});
  check(!level.getServer().isSameThread(),"Actual native test thread is not the owning server thread");var at=new BlockPos(0,101,0);
  for(var type:List.of(AuraBeasts.STONEHORN,AuraBeasts.GALECLAW)){
   for(int n=0;n<16;n++){check(!SpawnPlacements.checkSpawnRules(type,level,EntitySpawnReason.NATURAL,at,net.minecraft.util.RandomSource.create(n)),"Actual off-thread registered natural predicate refuses before storage/population access");check(!SpawnPlacements.checkSpawnRules(type,level,EntitySpawnReason.CHUNK_GENERATION,at,net.minecraft.util.RandomSource.create(n)),"Actual off-thread generation predicate refuses before storage/population access");}
   check(SpawnPlacements.checkSpawnRules(type,level,EntitySpawnReason.COMMAND,at,net.minecraft.util.RandomSource.create(951))&&SpawnPlacements.checkSpawnRules(type,level,EntitySpawnReason.SPAWN_ITEM_USE,at,net.minecraft.util.RandomSource.create(951)),"Explicit command/egg reason bypass preserved without off-thread storage reads");
  }
 }}
 private void queries(ClientGameTestContext c){try(var w=c.worldBuilder().create()){
  c.waitTicks(25);configure(w.getServer());
  w.getServer().runOnServer(s->{var l=s.overworld();arena(l);var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.teleportTo(l,12,101,12,Set.<Relative>of(),0,0,false);
   var peers=new ArrayList<Zombie>();for(int n=0;n<13;n++){var e=EntityTypes.ZOMBIE.create(l,EntitySpawnReason.COMMAND);e.setCustomName(net.minecraft.network.chat.Component.literal("Native supplied Aura receiver"));e.setNoAi(true);e.snapTo((n%4)+.5,101,(n/4)+.5,0,0);l.addFreshEntity(e);peers.add(e);}
   var area=new AABB(-5,100,-5,8,106,8);
   check(AuraBeastQueries.complete(l,Zombie.class,area,Entity::isAlive).isEmpty(),"Actual thirteen native candidates refuse entire crowded pool, never hidden twelve-prefix");
   peers.removeLast().discard();var admitted=AuraBeastQueries.complete(l,Zombie.class,area,Entity::isAlive);check(admitted.size()==12&&new HashSet<>(admitted).equals(new HashSet<>(peers)),"Removing actual overflow reopens complete twelve-peer native pool");for(var e:peers)e.discard();
   var animals=new ArrayList<Galeclaw>();for(int n=0;n<13;n++){var e=AuraBeasts.GALECLAW.create(l,EntitySpawnReason.COMMAND);e.setNoAi(true);e.snapTo(n%4+.5,101,n/4+.5,0,0);l.addFreshEntity(e);animals.add(e);}
   p.teleportTo(l,5,101,5,Set.<Relative>of(),0,0,false);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(AuraBeasts.RIDGE_WHISTLE));
   p.getMainHandItem().getItem().use(l,p,InteractionHand.MAIN_HAND);
   for(var e:animals)check(e.calmTicks()==0,"Actual crowded whistle changes no hidden prefix recipient");check(p.getCooldowns().isOnCooldown(p.getMainHandItem()),"Crowded whistle retains original tool cooldown");
   animals.removeLast().discard();
   // Fresh real stack with cleared ordinary cooldown lets the same admitted twelve pool prove intended tool behavior.
   p.getCooldowns().removeCooldown(p.getCooldowns().getCooldownGroup(p.getMainHandItem()));p.getMainHandItem().getItem().use(l,p,InteractionHand.MAIN_HAND);
   for(var e:animals)check(e.calmTicks()==200&&e.getTarget()==null,"Actual complete whistle pool distracts every eligible visible native recipient");for(var e:animals)e.discard();
   var at=new BlockPos(0,101,0);check(AuraBeasts.maySpawn(AuraBeasts.STONEHORN,l,EntitySpawnReason.NATURAL,at),"Actual dry highland zero-population main-thread natural habitat admits");check(!AuraBeasts.maySpawn(AuraBeasts.STONEHORN,l,EntitySpawnReason.CHUNK_GENERATION,at),"Chunk generation refuses before live population reads");
   var local=new ArrayList<Stonehorn>();for(int n=0;n<64;n++){var e=AuraBeasts.STONEHORN.create(l,EntitySpawnReason.COMMAND);e.setNoAi(true);e.snapTo(4+n%4,101,4+n/4,0,0);l.addFreshEntity(e);local.add(e);}
   check(!AuraBeasts.maySpawn(AuraBeasts.STONEHORN,l,EntitySpawnReason.NATURAL,at),"Dense native local population safely refuses exact-two cap");
   for(int n=2;n<local.size();n++)local.get(n).discard();check(!AuraBeasts.maySpawn(AuraBeasts.STONEHORN,l,EntitySpawnReason.NATURAL,at),"Exact two actual animals retain original natural refusal");local.get(1).discard();check(AuraBeasts.maySpawn(AuraBeasts.STONEHORN,l,EntitySpawnReason.NATURAL,at),"Actual one-animal habitat reopens original natural threshold");
  });
 }}
 private void feeding(ClientGameTestContext c,boolean gale){try(var w=c.worldBuilder().create()){
  c.waitTicks(25);configure(w.getServer());
  int id=w.getServer().computeOnServer(s->{var l=s.overworld();arena(l);arena(s.getLevel(Level.NETHER));var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SPECTATOR);p.teleportTo(l,.5,101,-2,Set.<Relative>of(),0,0,false);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(gale?Items.RABBIT:Items.WHEAT,3));
   var e=(gale?AuraBeasts.GALECLAW:AuraBeasts.STONEHORN).create(l,EntitySpawnReason.COMMAND);e.setNoAi(true);e.snapTo(.5,101,.5,0,0);l.addFreshEntity(e);
   long before=rest(e);e.mobInteract(p,InteractionHand.MAIN_HAND);check(p.getMainHandItem().getCount()==3&&rest(e)==before&&material(l,gale)==0,"Direct authoritative spectator feed consumes nothing, creates no material and reserves no clock");
   p.setGameMode(GameType.SURVIVAL);p.teleportTo(s.getLevel(Level.NETHER),.5,101,.5,Set.<Relative>of(),0,0,false);e.mobInteract(p,InteractionHand.MAIN_HAND);check(p.getMainHandItem().getCount()==3&&rest(e)==before&&material(l,gale)==0,"Actual foreign-world player cannot feed old-world creature or reserve its reward");
   p.teleportTo(l,.5,101,-2,Set.<Relative>of(),0,0,false);return e.getId();
  });c.waitTicks(10);
  feed(c,id);c.waitTicks(10);feed(c,id);c.waitTicks(10);
  w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var l=s.overworld();var e=(AuraBeast)l.getEntity(id);
   check(p.getMainHandItem().getCount()==2&&material(l,gale)==1,"Actual ordinary Survival client packets consume exactly one food and shed exactly one material");check(rest(e)>l.getGameTime(),"Actual successful packet reserves original finite shed interval");
   var saved=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,s.registryAccess());e.saveWithoutId(saved);var restored=(AuraBeast)e.getType().create(l,EntitySpawnReason.LOAD);restored.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,s.registryAccess(),saved.buildResult()));restored.mobInteract(p,InteractionHand.MAIN_HAND);check(rest(restored)==rest(e)&&p.getMainHandItem().getCount()==2&&material(l,gale)==1,"Actual serialized successful feeding clock refuses another payment/material on load");restored.discard();
   var untouched=(gale?AuraBeasts.GALECLAW:AuraBeasts.STONEHORN).create(l,EntitySpawnReason.COMMAND);untouched.setNoAi(true);untouched.snapTo(5,101,5,0,0);l.addFreshEntity(untouched);long before=rest(untouched);
   check(p.hurtServer(l,l.damageSources().genericKill(),10000)&&!p.isAlive(),"Actual native lethal damage creates genuine dead-player admission case");untouched.mobInteract(p,InteractionHand.MAIN_HAND);
   check(p.getMainHandItem().getCount()==2&&rest(untouched)==before&&material(l,gale)==1,"Actual dead player cannot pay, mint material or reserve a fresh creature clock");
  });
 }}
 private static long rest(AuraBeast e){try{var f=AuraBeast.class.getDeclaredField("nextShed");f.setAccessible(true);return f.getLong(e);}catch(ReflectiveOperationException ex){throw new AssertionError(ex);}}
 private static int material(ServerLevel l,boolean gale){Item item=gale?AuraBeasts.GALECLAW_PLUME:AuraBeasts.STONEHORN_PLATE;int n=0;for(var e:l.getEntitiesOfClass(ItemEntity.class,new AABB(-20,98,-20,20,108,20),e->e.getItem().is(item)))n+=e.getItem().getCount();for(var p:l.players())for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n;}
 private static void feed(ClientGameTestContext c,int id){c.runOnClient(mc->{var e=mc.level.getEntity(id);mc.gameMode.interact(mc.player,e,new EntityHitResult(e,e.getBoundingBox().getCenter()),InteractionHand.MAIN_HAND);});}
 private static void arena(ServerLevel l){for(int x=-18;x<=18;x++)for(int z=-18;z<=18;z++){l.setBlock(new BlockPos(x,100,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);for(int y=101;y<=106;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}}
 private static void configure(TestServerContext s){s.runCommand("difficulty normal");s.runCommand("gamerule spawn_mobs false");s.runCommand("gamerule fall_damage false");s.runCommand("gamerule keep_inventory true");s.runCommand("time set 18000");}
 private static void check(boolean v,String message){if(!v)throw new AssertionError(message);}
}
