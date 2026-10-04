package dev.wildercord.wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** Actual loaded native entity pools; does not claim a gameplay warning or a timing benchmark. */
public final class RootmoltQueryBoundsTest implements FabricClientGameTest {
 public void runTest(ClientGameTestContext c){try(var w=c.worldBuilder().create()){
  c.waitTicks(25);w.getServer().runCommand("difficulty normal");w.getServer().runCommand("gamerule spawn_mobs false");
  w.getServer().runOnServer(s->{
   var l=s.overworld();var site=new BlockPos(0,30,0);l.getChunkAt(site);
   for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++){
    l.setBlock(new BlockPos(x,29,z),Blocks.MOSS_BLOCK.defaultBlockState(),2);
    for(int y=30;y<=32;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);
    l.setBlock(new BlockPos(x,33,z),Blocks.STONE.defaultBlockState(),2);
   }
   var roots=new ArrayList<RootmoltStrider>();
   check(RootmoltContent.localRoom(l,RootmoltContent.STRIDER,site),"Empty actual population admits a peer");
   for(int i=0;i<65;i++){
    var e=RootmoltContent.STRIDER.create(l,EntitySpawnReason.COMMAND);check(e!=null,"Native strider created");e.setNoAi(true);e.snapTo(.5,30,.5,0,0);l.addFreshEntity(e);roots.add(e);
    if(i==0)check(RootmoltContent.localRoom(l,RootmoltContent.STRIDER,site),"One actual live peer leaves one slot");
    else check(!RootmoltContent.localRoom(l,RootmoltContent.STRIDER,site),"Two or more actual live peers refuse admission");
   }
   var source=roots.getFirst();roots.stream().skip(1).forEach(RootmoltStrider::discard);
   check(RootmoltContent.localRoom(l,RootmoltContent.STRIDER,site),"Removing actual excess peers reopens admission");
   var snails=new ArrayList<SporebackSnail>();
   for(int i=0;i<33;i++){
    var e=SporebackContent.SNAIL.create(l,EntitySpawnReason.COMMAND);check(e!=null,"Native snail created");e.setNoAi(true);e.snapTo(.8+(i%8)*.25,30,.7+(i/8)*.25,0,0);l.addFreshEntity(e);snails.add(e);
    if(i<32)check(RootmoltContent.alarmCandidates(source).size()==i+1,"Complete small actual pool preserves all eligible receivers");
   }
   check(RootmoltContent.alarmCandidates(source).isEmpty(),"Thirty-three actual matching snails refuse the overcrowded pulse");
   var overflow=snails.getLast();overflow.answerThreat();
   check(overflow.pose()==2,"Actual physical threat makes the overflow receiver hide");
   check(RootmoltContent.alarmCandidates(source).isEmpty(),"A hidden overflow receiver still counts before candidate filtering");
   overflow.discard();snails.removeLast();
   var expected=snails.stream().sorted(Comparator.<SporebackSnail>comparingDouble(source::distanceToSqr).thenComparing(e->e.getUUID().toString())).map(e->e.getUUID()).toList();
   var first=RootmoltContent.alarmCandidates(source).stream().map(e->e.getUUID()).toList();
   check(first.equals(expected) && first.equals(RootmoltContent.alarmCandidates(source).stream().map(e->e.getUUID()).toList()),"Removing overflow reopens the same complete pool in deterministic distance/UUID order");
   snails.forEach(SporebackSnail::discard);source.discard();
   check(RootmoltContent.localRoom(l,RootmoltContent.STRIDER,site) && RootmoltContent.alarmCandidates(source).isEmpty(),"Removed fixtures leave no local candidates");
  });
 }}
 private static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
}
