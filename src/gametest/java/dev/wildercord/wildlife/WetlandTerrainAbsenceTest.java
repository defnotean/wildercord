package dev.wildercord.wildlife;

import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biomes;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** The original failed square legitimately has no reeds; every actual attempt is retained. */
public final class WetlandTerrainAbsenceTest implements FabricClientGameTest {
 private static final BlockPos HABITAT=new BlockPos(-1536,64,-768);
 @Override public void runTest(ClientGameTestContext context) {
  try(var probe=WetlandGenerationProbe.beginFixture();
      var world=context.worldBuilder().setUseConsistentSettings(false)
        .adjustSettings(settings -> settings.setSeed(Long.toString(WetlandGenerationProbe.SEED))).create()) {
   context.waitTicks(35);world.getServer().runCommand("gamerule spawn_mobs false");world.getServer().runCommand("time set 6000");
   BlockPos habitat=world.getServer().computeOnServer(server -> {
    var level=server.overworld();check(level.getSeed()==WetlandGenerationProbe.SEED,"Original absence seed");
    Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.wildlife.WetlandTerrainAbsenceTest\",\"seed\":\""+level.getSeed()+"\"}");
    var source=level.getChunkSource();
    var found=source.getGenerator().getBiomeSource().findBiomeHorizontal(0,64,0,6400,32,
      biome -> biome.is(Biomes.SWAMP),RandomSource.create(912),true,source.randomState());
    check(found!=null && found.getFirst().equals(HABITAT),"Original biome lookup and absence region; diagnose provenance drift before changing this fixture");
    return found.getFirst();
   });
   var observations=new WetlandTerrainObservation();int loaded=0;
   for(int radius=0;radius<=4;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++) {
    if(radius>0 && Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
    int cx=(habitat.getX()>>4)+dx,cz=(habitat.getZ()>>4)+dz;
    world.getServer().runOnServer(server -> {
     var level=server.overworld();var chunk=level.getChunk(cx,cz);
     var candidate=WetlandTerrainTest.findByHeightWindow(level,cx,cz);
     observations.observe(level,chunk,candidate);
     check(candidate==null,"Original height-window detector finds no reed in the recorded absence region");
    });loaded++;context.waitTicks(1);
   }
   check(loaded==81 && observations.reeds()==0,"Complete original 81-chunk all-height census remains empty");
   var receipt=probe.receipt();WetlandPlacementContractTest.complete(receipt,81);
   check(receipt.count("featureCalls")==5 && receipt.count("attempts")==120
     && receipt.count("reject_moist")==108 && receipt.count("reject_empty")==12
     && receipt.writes().isEmpty(),"Exactly five real feature calls reject 108 dry and 12 occupied candidates");
   check(receipt.count("rarityAdmitted")==9 && receipt.count("rarityRejected")==47
     && receipt.count("biomeAdmitted")==5 && receipt.count("biomeRejected")==4,
     "The recorded absence retains actual rarity and biome admission receipts");
   try(var input=WetlandTerrainAbsenceTest.class.getResourceAsStream("/wetland/absence-attempts.txt")) {
    check(input!=null,"Recorded native attempt resource is present");
    var expected=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8)).lines().sorted().toList();
    check(expected.size()==120 && receipt.attempts().stream().sorted().toList().equals(expected),
      "Every native source/origin/ordinal/coordinate/gate/outcome matches run 37380606913; investigate drift instead of replacing the seed");
   } catch(java.io.IOException exception) {throw new AssertionError("Cannot read absence receipts",exception);}
   Wildercord.LOGGER.info("WETLAND_ABSENCE verified seed="+WetlandGenerationProbe.SEED+", habitat="+habitat+", searchedChunks="+loaded+", "+probe.summary()+", "+observations.summary());
  }
 }
 private static void check(boolean condition,String reason) {if(!condition)throw new AssertionError(reason);}
}
