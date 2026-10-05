package dev.wildercord.wildlife;

import dev.wildercord.Wildercord;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Read-only, post-generation evidence for the fixture's existing chunk search. */
final class WetlandTerrainObservation {
 private final Map<String,Integer> totals=new LinkedHashMap<>();
 private BlockPos firstReed,firstMissedReed;

 void observe(ServerLevel level,LevelChunk chunk,BlockPos originalMatch) {
  WetlandGenerationProbe.afterRequestedChunk(chunk.getPos());
  var counts=new LinkedHashMap<String,Integer>();
  for(String key:new String[]{"columns","at64Swamp","surfaceSwamp","surfaceMangrove",
    "surfaceEmpty","surfaceSupported","surfaceEmptySupported","moistureUnknown",
    "surfaceMoist","surfaceOpenSky","swampEligible","wetlandEligible",
    "reedSections","reeds","reedsInHeightWindow","reedsOutsideHeightWindow"})counts.put(key,0);
  var position=chunk.getPos();int minX=position.getMinBlockX(),minZ=position.getMinBlockZ();
  var reed=WetlandGarden.REED.defaultBlockState().setValue(MoonreedBlock.AGE,1);
  int minSurface=Integer.MAX_VALUE,maxSurface=Integer.MIN_VALUE;
  for(int x=minX;x<minX+16;x++)for(int z=minZ;z<minZ+16;z++) {
   increment(counts,"columns");
   int surface=chunk.getHeight(Heightmap.Types.WORLD_SURFACE,x&15,z&15)+1;
   minSurface=Math.min(minSurface,surface);maxSurface=Math.max(maxSurface,surface);
   var at=new BlockPos(x,surface,z);var biome=level.getBiome(at);
   boolean swamp=biome.is(Biomes.SWAMP),mangrove=biome.is(Biomes.MANGROVE_SWAMP);
   if(level.getBiome(new BlockPos(x,64,z)).is(Biomes.SWAMP))increment(counts,"at64Swamp");
   if(swamp)increment(counts,"surfaceSwamp");
   if(mangrove)increment(counts,"surfaceMangrove");
   boolean empty=chunk.getBlockState(at).isAir(),supported=reed.canSurvive(level,at);
   if(empty)increment(counts,"surfaceEmpty");
   if(supported)increment(counts,"surfaceSupported");
   if(!empty || !supported)continue;
   increment(counts,"surfaceEmptySupported");
   // Moonreed.moist reads adjacent columns. Never generate/load another chunk for evidence.
   if(!moistureNeighborsLoaded(level,at)) {increment(counts,"moistureUnknown");continue;}
   if(!MoonreedBlock.moist(level,at))continue;
   increment(counts,"surfaceMoist");
   if(!MoonreedBlock.openSky(level,at))continue;
   increment(counts,"surfaceOpenSky");
   if(swamp)increment(counts,"swampEligible");
   if(swamp || mangrove)increment(counts,"wetlandEligible");
  }

  // Inspect only the already loaded chunk, at every Y. Palette filtering bounds block reads.
  // A match here is evidence only; it cannot substitute for the fixture's original detector.
  var sections=chunk.getSections();BlockPos chunkFirstReed=null,chunkFirstMissed=null;
  for(int sectionIndex=0;sectionIndex<sections.length;sectionIndex++) {
   var section=sections[sectionIndex];
   if(!section.maybeHas(state -> state.is(WetlandGarden.REED)))continue;
   increment(counts,"reedSections");int minY=chunk.getMinY()+sectionIndex*16;
   for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=0;y<16;y++) {
    if(!section.getBlockState(x,y,z).is(WetlandGarden.REED))continue;
    var at=new BlockPos(minX+x,minY+y,minZ+z);increment(counts,"reeds");
    if(chunkFirstReed==null)chunkFirstReed=at;
    int top=chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)+1;
    if(at.getY()>=top-1 && at.getY()<=top+1)increment(counts,"reedsInHeightWindow");
    else {increment(counts,"reedsOutsideHeightWindow");if(chunkFirstMissed==null)chunkFirstMissed=at;}
   }
  }
  if(firstReed==null)firstReed=chunkFirstReed;
  if(firstMissedReed==null)firstMissedReed=chunkFirstMissed;
  counts.forEach((key,value) -> totals.merge(key,value,Integer::sum));
  Wildercord.LOGGER.info("WETLAND_TERRAIN_CHUNK stage=post_generation, chunk="+position
    +", surfaceY="+minSurface+".."+maxSurface+", counts="+counts+", originalMatch="+originalMatch
    +", firstReed="+chunkFirstReed+", firstMissedReed="+chunkFirstMissed);
 }

 String summary() {
  return "stage=post_generation, counts="+totals+", firstReed="+firstReed+", firstMissedReed="+firstMissedReed
    +", predicateStages=surfaceEmptySupported -> surfaceMoist -> surfaceOpenSky -> wetlandEligible"
    +", moistureUnknown=skipped_missing_resident_neighbor, generationAttempts=separate_passive_receipt";
 }
 int reeds() {return totals.getOrDefault("reeds",0);}

 private static boolean moistureNeighborsLoaded(ServerLevel level,BlockPos at) {
  for(var direction:Direction.Plane.HORIZONTAL) {
   var neighbor=at.relative(direction);
   if(level.getChunkSource().getChunkNow(neighbor.getX()>>4,neighbor.getZ()>>4)==null)return false;
  }
  return true;
 }

 private static void increment(Map<String,Integer> counts,String key) {counts.merge(key,1,Integer::sum);}
}
