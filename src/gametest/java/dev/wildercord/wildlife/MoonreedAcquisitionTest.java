package dev.wildercord.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.api.WildercordEvents;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Explicit native heightmap adapter contracts, then passive observations of ordinary scheduled lure searches. */
public final class MoonreedAcquisitionTest implements FabricClientGameTest {
 private static final BlockPos BODY=new BlockPos(0,90,0);
 private Glimmerwing moth;

 @Override public void runTest(ClientGameTestContext context) {
  try(var world=context.worldBuilder().create()) {
   try {
    context.waitTicks(25);
    world.getServer().runCommand("gamerule spawn_mobs false");
    world.getServer().runCommand("gamerule random_tick_speed 0");
    world.getServer().runCommand("time set 18000");
    world.getServer().runCommand("weather clear");
    world.getServer().runOnServer(server->{
     var level=server.overworld();var player=server.getPlayerList().getPlayers().getFirst();
     player.setGameMode(GameType.CREATIVE);player.teleportTo(level,.5,92,.5,Set.<Relative>of(),0,0,false);
     adapterContracts(level);
     garden(level,true);
    });
    // Light propagation and fluid ticks settle before there is any observed actor.
    context.waitTicks(25);
    for(var mode:MoonreedPriorityProbe.Mode.values()) {
     world.getServer().runOnServer(server->{
      var level=server.overworld();var player=server.getPlayerList().getPlayers().getFirst();
      if(mode==MoonreedPriorityProbe.Mode.CASTER) {
       player.setGameMode(GameType.CREATIVE);
       // The real public event is the stimulus. This is not a paid-cast test.
       WildercordEvents.AFTER_CAST.invoker().afterCast(player,0,List.of(),10);
      } else player.setGameMode(GameType.SPECTATOR);
      if(mode==MoonreedPriorityProbe.Mode.LAMP)garden(level,false);
      moth=Wildlife.GLIMMERWING.create(level,EntitySpawnReason.COMMAND);
      check(moth!=null,"Native priority moth factory");moth.snapTo(2.5,92,.5,0,0);moth.setPersistenceRequired();
      check(level.addFreshEntity(moth),"Native priority moth is tracked");
      MoonreedPriorityProbe.begin(level,moth,mode);
     });
     boolean observed=false;
     for(int attempt=0;attempt<48;attempt++) {
      context.waitTicks(5);
      if(world.getServer().computeOnServer(server->MoonreedPriorityProbe.observed())){observed=true;break;}
     }
     boolean completed=observed;
     world.getServer().runOnServer(server->{MoonreedPriorityProbe.finish(completed);moth.discard();moth=null;});
    }
   } finally {
    MoonreedPriorityProbe.clear();
    world.getServer().runOnServer(server->{if(moth!=null){moth.discard();moth=null;}});
   }
  }
 }

 /** Synchronous helper comparisons deliberately make no claim about a moth's flight or search cadence. */
 private static void adapterContracts(ServerLevel level) {
  // A read-only search must leave a genuinely absent remote FULL chunk absent.
  var absent=new BlockPos(65544,90,65544);var chunks=level.getChunkSource();
  check(chunks.getChunkNow(absent.getX()>>4,absent.getZ()>>4)==null,"Remote negative fixture has no completed FULL chunk");
  int residentBefore=chunks.getLoadedChunksCount();
  check(MoonreedBlock.findBud(level,absent)==null,"Missing FULL residency refuses acquisition without a terrain query");
  check(chunks.getLoadedChunksCount()==residentBefore&&chunks.getChunkNow(absent.getX()>>4,absent.getZ()>>4)==null,"Negative acquisition leaves the completed chunk count and remote FULL residency unchanged");
  clearColumns(level,5,84);
  for(var root:List.of(BODY.below(),BODY,BODY.above(),BODY.offset(-3,1,-3),BODY.offset(3,-1,3))) {
   clearFixture(level);bud(level,root,true);
   check(level.getBlockState(root.below(2)).isAir(),"Exposed raised soil platform is separate from the underlying terrain");
   compareOld(level,root,"exposed old-band/edge root "+root);
  }
  clearFixture(level);bud(level,BODY,true);
  // Deliberately retain an invalid reed; ordinary neighbor shape updates would remove it.
  level.setBlock(BODY.below(),Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
  check(level.getBlockState(BODY).is(WetlandGarden.REED)&&!level.getBlockState(BODY).canSurvive(level,BODY),"Unsupported floating reed remains after a soil write that explicitly suppresses shape updates");
  compareOld(level,BODY,"acquisition preserves the old floating-reed predicate; survival belongs to later pollination admission");
  clearFixture(level);
  bud(level,BODY.offset(-3,1,-2),true);bud(level,BODY.offset(3,-1,-2),true);bud(level,BODY.offset(-3,-1,0),true);
  compareOld(level,BODY.offset(3,-1,-2),"old iteration keeps Z, then Y, then X ordering across different heights");
  clearFixture(level);bud(level,BODY.offset(2,0,-2),true);bud(level,BODY.offset(-2,0,-2),true);
  compareOld(level,BODY.offset(-2,0,-2),"old equal-height ordering keeps the lowest X");
  clearFixture(level);bud(level,BODY.offset(-3,-4,-3),true);bud(level,BODY.offset(3,1,3),true);
  compareOld(level,BODY.offset(3,1,3),"an old-band root wins even when a new lower root sorts earlier");
  for(int drop=2;drop<=4;drop++) {
   clearFixture(level);var root=BODY.below(drop);bud(level,root,true);
   check(oldBand(level)==null,"Lower exposed root is outside the old body band");
   check(root.equals(MoonreedBlock.findBud(level,BODY)),"Native surface adapter finds exposed lower root at drop "+drop);
  }
  clearFixture(level);bud(level,BODY.below(5),true);compareOld(level,null,"five-block drop remains outside acquisition");
  clearFixture(level);bud(level,BODY,true);level.setBlock(BODY.above(3),Blocks.STONE.defaultBlockState(),2);
  compareOld(level,null,"a real placed roof changes WORLD_SURFACE and hides the bud immediately");
  clearFixture(level);bud(level,BODY,false);compareOld(level,null,"dry exposed root remains ineligible");
  clearFixture(level);bud(level,BODY,true);
  level.setBlock(BODY,WetlandGarden.REED.defaultBlockState().setValue(MoonreedBlock.AGE,2),2);
  compareOld(level,null,"mature exposed root remains ineligible");
  Wildercord.LOGGER.info("WILDERCORD_MOONREED_ADAPTER nativeWorldSurface=true oldOrdering=true lowerDrops=2..4 roofDryMatureRefused=true");
 }

 private static void compareOld(ServerLevel level,BlockPos expected,String why) {
  var old=oldBand(level);var found=MoonreedBlock.findBud(level,BODY);
  check(Objects.equals(expected,old),"Native original search fixture: "+why+" (found "+old+")");
  check(Objects.equals(old,found),"Native surface/original equivalence: "+why+" (found "+found+")");
  if(found!=null)check(level.getHeight(Heightmap.Types.WORLD_SURFACE,found.getX(),found.getZ())==found.getY()+1,"Actual WORLD_SURFACE selects precisely the exposed non-air reed");
 }

 /** Copy of the pre-change public-world predicate and actual BlockPos iterator, solely as a native oracle. */
 private static BlockPos oldBand(ServerLevel level) {
  if(!WetlandRules.night(level.getOverworldClockTime()))return null;
  for(var at:BlockPos.betweenClosed(BODY.offset(-3,-1,-3),BODY.offset(3,1,3))) {
   if(!level.hasChunkAt(at))continue;var state=level.getBlockState(at);
   if(state.is(WetlandGarden.REED)&&state.getValue(MoonreedBlock.AGE)==1&&MoonreedBlock.canBloom(level,at,level.getOverworldClockTime()))return at.immutable();
  }
  return null;
 }

 private static void bud(ServerLevel level,BlockPos root,boolean moist) {
  level.setBlock(root.below(),Blocks.DIRT.defaultBlockState(),2);
  if(moist)level.setBlock(root.east().below(),Blocks.WATER.defaultBlockState(),2);
  level.setBlock(root,WetlandGarden.REED.defaultBlockState().setValue(MoonreedBlock.AGE,1),2);
  check(level.getBlockState(root).canSurvive(level,root),"Native adapter root has its actual required soil");
 }

 private static void clearFixture(ServerLevel level) {
  for(var at:BlockPos.betweenClosed(new BlockPos(-5,83,-5),new BlockPos(5,96,5)))level.setBlock(at,Blocks.AIR.defaultBlockState(),2);
 }

 private static void clearColumns(ServerLevel level,int radius,int floor) {
  for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)
   for(int y=level.getHeight(Heightmap.Types.WORLD_SURFACE,x,z)-1;y>=floor;y--)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);
 }

 /** Many ordinary bright cells avoid dependence on finding a single lamp in five random samples. */
 private static void garden(ServerLevel level,boolean flowers) {
  if(flowers)clearColumns(level,20,89);
  for(int x=-20;x<=20;x++)for(int z=-20;z<=20;z++) {
   var floor=new BlockPos(x,90,z);level.setBlock(floor.below(),Blocks.STONE.defaultBlockState(),2);
   boolean root=flowers&&(x&1)==0&&(z&1)==0;
   level.setBlock(floor,root?Blocks.DIRT.defaultBlockState():flowers&&(z&1)==0?Blocks.WATER.defaultBlockState():Blocks.GLOWSTONE.defaultBlockState(),2);
   level.setBlock(floor.above(),root?WetlandGarden.REED.defaultBlockState().setValue(MoonreedBlock.AGE,1):Blocks.AIR.defaultBlockState(),2);
  }
 }

 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
