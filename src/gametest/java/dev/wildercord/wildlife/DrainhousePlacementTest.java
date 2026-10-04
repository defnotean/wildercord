package dev.wildercord.wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import java.util.*;
/** Actual world writer with one-shot forward faults, followed by exact ordinary rollback. Not natural discovery. */
public final class DrainhousePlacementTest implements FabricClientGameTest {
 private static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 @Override public void runTest(ClientGameTestContext c){try(var w=c.worldBuilder().create()){
  c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule random_tick_speed 0");
  w.getServer().runOnServer(s -> {
   var l=s.overworld();var center=new BlockPos(0,30,0);var original=new LinkedHashMap<BlockPos,BlockState>();
   for(int cx=-1;cx<=1;cx++)for(int cz=-1;cz<=1;cz++)l.getChunk(cx,cz);
   for(int x=-10;x<=10;x++)for(int z=-4;z<=4;z++)for(int y=-3;y<=4;y++){
    var at=center.offset(x,y,z);var desired=DrainhouseFeature.plan(center).get(at);boolean solidOre=y==-1 && (x+z)%3==0 && (desired==null || desired.isSolidRender());var state=y<0?(solidOre?Blocks.IRON_ORE:Blocks.MOSS_BLOCK).defaultBlockState():y==4?Blocks.GRANITE.defaultBlockState():Blocks.AIR.defaultBlockState();l.setBlock(at,state,DrainhouseFeature.WRITE_FLAGS);original.put(at,state);
   }
   var gap=center.offset(-5,-1,-2);l.setBlock(gap,Blocks.AIR.defaultBlockState(),DrainhouseFeature.WRITE_FLAGS);l.setBlock(gap.below(),Blocks.AIR.defaultBlockState(),DrainhouseFeature.WRITE_FLAGS);original.put(gap,Blocks.AIR.defaultBlockState());original.put(gap.below(),Blocks.AIR.defaultBlockState());
   var carved=center.offset(2,1,1);check(DrainhouseFeature.plan(center).get(carved).isAir(),"Rollback witness is an actual carved opening");l.setBlock(carved,Blocks.STONE.defaultBlockState(),DrainhouseFeature.WRITE_FLAGS);original.put(carved,Blocks.STONE.defaultBlockState());
   var carpet=center.offset(2,0,2);l.setBlock(carpet,Blocks.MOSS_CARPET.defaultBlockState(),DrainhouseFeature.WRITE_FLAGS);original.put(carpet,Blocks.MOSS_CARPET.defaultBlockState());
   check(DrainhouseFeature.room(l,center),"Deliberate empty-room fixture admits its actual distinct original floor and roof");
   var area=new AABB(net.minecraft.world.phys.Vec3.atLowerCornerOf(center.offset(-11,-4,-5)),net.minecraft.world.phys.Vec3.atLowerCornerOf(center.offset(12,6,6)));int drops=l.getEntitiesOfClass(ItemEntity.class,area).size();
   int[] writes={0};
   check(!DrainhouseFeature.build(l,center,(at,state) -> {boolean wrote=l.setBlock(at,state,DrainhouseFeature.WRITE_FLAGS);return ++writes[0]!=80 && wrote;}),"A writer that changes the80th block but returns false aborts the build");
   intact(l,original);check(writes[0]==80,"Forward denial cannot continue constructing");
   try{DrainhouseFeature.build(l,center,(at,state) -> {l.setBlock(at,state,DrainhouseFeature.WRITE_FLAGS);if(state.is(DrainhouseContent.MARK))throw new IllegalStateException("fixture denied after marker creation");return true;});throw new AssertionError("Expected deliberately injected marker-write exception");}catch(IllegalStateException expected){check(expected.getMessage().equals("fixture denied after marker creation"),"Rollback must preserve the original forward failure");}
   intact(l,original);
   // Level.getBlockEntity uses IMMEDIATE creation; deleting its map entry would merely recreate a valid entity.
   // A removed entry instead makes the next native verification return null before it can authenticate the ledger.
   check(!DrainhouseFeature.build(l,center,(at,state) -> {boolean wrote=l.setBlock(at,state,DrainhouseFeature.WRITE_FLAGS);if(state.is(DrainhouseContent.MARK) && state.getValue(DrainhouseMark.KIND)==2){var entity=l.getBlockEntity(at);check(entity!=null,"Actual final ledger exists before its injected removal");entity.setRemoved();}return wrote;}),"Removed final ledger entity refuses the complete-but-unauthenticated structure");
   intact(l,original);check(l.getEntitiesOfClass(ItemEntity.class,area).size()==drops,"Failed transactions neither drop original floor resources nor mint ledger items");
   check(DrainhouseFeature.build(l,center,(at,state) -> l.setBlock(at,state,DrainhouseFeature.WRITE_FLAGS)),"Ordinary actual-world writer commits the complete planned architecture");
   var plan=DrainhouseFeature.plan(center);check(plan.size()<=918,"Unique placements remain within the original admitted footprint budget");
   for(var e:plan.entrySet()){var old=original.get(e.getKey());var expected=old.is(Blocks.IRON_ORE)?old:e.getValue();check(l.getBlockState(e.getKey()).equals(expected),"Successful commit retains every exact plan state, preserving original solid ore");}
   check(l.getBlockState(gap.below()).is(Blocks.DEEPSLATE_BRICKS),"Extra support outside the798-cell architecture is committed");
   for(var e:original.entrySet()){if(e.getValue().is(Blocks.IRON_ORE))check(l.getBlockState(e.getKey()).equals(e.getValue()),"Actual original ore is never removed");if(!plan.containsKey(e.getKey()) && !e.getKey().equals(gap.below()))check(l.getBlockState(e.getKey()).equals(e.getValue()),"Unplanned mouth and support witnesses remain untouched");}
   for(var at:new BlockPos[]{center.offset(-6,0,1),center.offset(0,0,1),center.offset(6,1,1)})check(l.getBlockEntity(at) instanceof DrainhouseMarkEntity e && e.authentic(),"Only complete verified placement authenticates all three actual ledgers");
   var nether=s.getLevel(net.minecraft.world.level.Level.NETHER);check(nether!=null,"Actual Nether level exists for the dimension refusal");
   var netherOriginal=new LinkedHashMap<BlockPos,BlockState>();
   for(int x=-10;x<=10;x++)for(int z=-4;z<=4;z++)for(int y=-3;y<=4;y++){
    var at=center.offset(x,y,z);var state=y<0?Blocks.MOSS_BLOCK.defaultBlockState():y==4?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState();nether.setBlock(at,state,DrainhouseFeature.WRITE_FLAGS);netherOriginal.put(at,state);
   }
   int[] netherWrites={0};
   check(!new DrainhouseFeature().place(nether,nether.getChunkSource().getGenerator(),net.minecraft.util.RandomSource.create(716843),center),"Actual otherwise empty covered Nether room refuses manual feature placement");
   check(!DrainhouseFeature.build(nether,center,(at,state)->{netherWrites[0]++;return nether.setBlock(at,state,DrainhouseFeature.WRITE_FLAGS);}) && netherWrites[0]==0,"Actual non-Overworld room refuses before any transactional writer call");
   intact(nether,netherOriginal);
  });
 }}
 private static void intact(net.minecraft.server.level.ServerLevel l,Map<BlockPos,BlockState> original){for(var e:original.entrySet())check(l.getBlockState(e.getKey()).equals(e.getValue()) && l.getBlockEntity(e.getKey())==null,"Abort restores every original footprint/support/mouth witness cell exactly, with no partial ledger entity");}
}
