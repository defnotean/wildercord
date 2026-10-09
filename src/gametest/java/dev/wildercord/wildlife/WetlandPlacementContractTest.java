package dev.wildercord.wildlife;

import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.placement.*;
import java.util.LinkedHashSet;
import java.util.Set;

/** Registration and deterministic rarity are separate from the existence of a natural flower. */
public final class WetlandPlacementContractTest implements FabricClientGameTest {
 static final int FEATURE_INDEX=103,STEP_FEATURES=114;
 @Override public void runTest(ClientGameTestContext context) {
  try(var world=context.worldBuilder().create()) {
   world.getServer().runOnServer(server -> {
    Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.wildlife.WetlandPlacementContractTest\",\"seed\":\""+server.overworld().getSeed()+"\"}");
    var feature=registered(server.overworld());var rarity=(RarityFilter)feature.placement().getFirst();
    var admitted=new LinkedHashSet<String>();
    for(int x=-100;x<=-92;x++)for(int z=-52;z<=-44;z++) {
     var random=new WorldgenRandom(new XoroshiroRandomSource(0));
     long decoration=random.setDecorationSeed(WetlandGenerationProbe.SEED,x*16,z*16);
     random.setFeatureSeed(decoration,FEATURE_INDEX,GenerationStep.Decoration.VEGETAL_DECORATION.ordinal());
     if(rarity.shouldPlace(null,random,BlockPos.ZERO))admitted.add(x+"/"+z);
    }
    check(admitted.equals(Set.of(RARITY_SOURCES)),"Registered rarity preserves the fixed native seed/source draw corpus independently of biome and habitat");
    Wildercord.LOGGER.info("WETLAND_RARITY seed="+WetlandGenerationProbe.SEED+", candidates=81, admitted="+admitted+", chance="+rarity.chance());
   });
  }
 }
 private static final String[] RARITY_SOURCES={"-100/-49","-100/-47","-99/-49","-99/-46","-98/-49","-98/-46","-97/-50","-97/-44","-96/-50","-96/-48","-96/-44","-95/-44","-94/-46","-92/-49"};

 static PlacedFeature registered(ServerLevel level) {
  var key=ResourceKey.create(Registries.PLACED_FEATURE,Wildercord.id("moonreed_patch"));
  var feature=level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).getOrThrow(key).value();
  check(feature.feature().value() instanceof MoonreedFeature
    && feature.feature().unwrapKey().orElseThrow().identifier().equals(Wildercord.id("moonreed_patch")),"Registered production Moonreed feature");
  var chain=feature.placement();
  check(chain.size()==4 && chain.get(0) instanceof RarityFilter rarity && rarity.chance()==6
    && chain.get(1) instanceof InSquarePlacement
    && chain.get(2) instanceof HeightmapPlacement height && height.heightmap()==Heightmap.Types.WORLD_SURFACE_WG
    && chain.get(3) instanceof BiomeFilter,"Production placement remains rarity(6), in-square, WORLD_SURFACE_WG, biome");
  int stage=GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
  for(var biome:level.registryAccess().lookupOrThrow(Registries.BIOME).listElements().toList()) {
   var steps=biome.value().getGenerationSettings().features();
   long atStage=steps.size()>stage ? steps.get(stage).stream().filter(holder -> holder.is(key)).count() : 0;
   long allStages=steps.stream().flatMap(holders -> holders.stream()).filter(holder -> holder.is(key)).count();
   long expected=biome.is(Biomes.SWAMP)||biome.is(Biomes.MANGROVE_SWAMP) ? 1 : 0;
   check(atStage==expected && allStages==expected,"Exactly one stage-9 registration in each swamp/mangrove biome, none in other biomes/stages: "+biome.key());
  }
  return feature;
 }
 static void complete(WetlandGenerationProbe.Receipt receipt,int chunks) {
  check(receipt.checkpoints()==chunks,"Every requested chunk completed and was observed");
  check(receipt.indexes().equals(Set.of(FEATURE_INDEX)) && receipt.stepSizes().equals(Set.of(STEP_FEATURES)),
    "Native feature graph provenance: expected stage-9 index 103 among 114; investigate graph drift before changing a site");
  check(receipt.count("featureCalls")>0 && receipt.count("attempts")==24*receipt.count("featureCalls")
    && receipt.count("featuresReturned")==receipt.count("featureCalls")
    && receipt.count("terminals")==receipt.count("attempts")
    && receipt.attempts().size()==receipt.count("attempts"),"Every real feature call has exactly 24 recorded terminal outcomes");
  check(receipt.count("placementsReturned")==receipt.count("rarityAdmitted")+receipt.count("rarityRejected")
    && receipt.count("rarityAdmitted")==receipt.count("biomeAdmitted")+receipt.count("biomeRejected")
    && receipt.count("biomeAdmitted")==receipt.count("featureCalls")
    && receipt.count("writesTrue")==receipt.writes().size(),"Placement returns, modifier admissions, feature calls and writes reconcile exactly");
  for(String fault:new String[]{"placementExceptions","featureExceptions","extraFeatureCalls","extraAttempts",
    "unterminatedAttempts","duplicateTerminals","non24Invocations","duplicateSources","omittedAttempts","writesFalse"})
   check(receipt.count(fault)==0,"No native observation fault: "+fault);
  check(receipt.omittedLines()==0 && receipt.sinkFailures()==0,"Native receipts are complete and delivered");
 }
 private static void check(boolean condition,String reason) {if(!condition)throw new AssertionError(reason);}
}
