package dev.wildercord.wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
/** Actual registered predicates; thread/reason refusal only, not supplied habitat or natural population credit. */
public final class WildlifeSpawnThreadTest implements FabricClientGameTest {
 public void runTest(ClientGameTestContext c){try(var w=c.worldBuilder().create()){
  c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");
  var level=w.getServer().computeOnServer(s->{check(s.isSameThread(),"Captured actual level on its owning server thread");return s.overworld();});
  check(!level.getServer().isSameThread(),"Actual native test thread is not the owning server thread");
  var at=new BlockPos(0,101,0);
  for(var type:java.util.List.of(WetlandContent.NEWT,ReedbackContent.CRAB,RootmoltContent.STRIDER,EmberContent.BAILIFF)){
   for(int n=0;n<16;n++){
    check(!SpawnPlacements.checkSpawnRules(type,level,EntitySpawnReason.NATURAL,at,RandomSource.create(n)),"Actual off-thread runtime predicate refuses before world/query access: "+type);
    check(!SpawnPlacements.checkSpawnRules(type,level,EntitySpawnReason.CHUNK_GENERATION,at,RandomSource.create(n)),"Chunk-generation policy refuses: "+type);
   }
   check(SpawnPlacements.checkSpawnRules(type,level,EntitySpawnReason.COMMAND,at,RandomSource.create(951)),"Explicit command bypass retained: "+type);
   check(SpawnPlacements.checkSpawnRules(type,level,EntitySpawnReason.SPAWN_ITEM_USE,at,RandomSource.create(951)),"Explicit egg bypass retained: "+type);
  }
  w.getServer().runOnServer(s->{for(var type:java.util.List.of(WetlandContent.NEWT,ReedbackContent.CRAB,RootmoltContent.STRIDER,EmberContent.BAILIFF))check(!SpawnPlacements.checkSpawnRules(type,s.overworld(),EntitySpawnReason.CHUNK_GENERATION,at,RandomSource.create(951)),"Actual server thread still refuses original generation policy: "+type);});
 }}
 private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
}
