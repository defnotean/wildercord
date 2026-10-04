package dev.wildercord.wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.*;
import java.util.*;
/** Real generated seed samples: no feature commands, planted resources or fabricated clue provenance. */
public final class DrainhouseTerrainTest implements FabricClientGameTest {
 private static final String[] SEEDS={"-7775421970293783948","1716203","716843"};
 private static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 private static BlockPos center(BlockPos mark,int kind){return kind==0?mark.offset(6,0,-1):kind==1?mark.offset(0,0,-1):mark.offset(-6,-1,-1);}
 private static boolean authentic(ServerLevel l,BlockPos at,int kind){return l.hasChunkAt(at) && l.getBlockState(at).is(DrainhouseContent.MARK) && l.getBlockState(at).getValue(DrainhouseMark.KIND)==kind && l.getBlockEntity(at) instanceof DrainhouseMarkEntity e && e.authentic();}
 @Override public void runTest(ClientGameTestContext c){int totalSites=0,seedsWithSites=0,totalChunks=0,totalRequests=0;
  for(String seed:SEEDS){var trace=new Trace(seed);check(DrainhouseFeature.observer==null,"Natural diagnostic owns its isolated observer");DrainhouseFeature.observer=trace;try{TestWorldSave saved=null;BlockPos review=null;Set<BlockPos> sites=new HashSet<>();Set<Long> requested=new HashSet<>();int[] totals=new int[6];
   try(var w=c.worldBuilder().setUseConsistentSettings(false).adjustSettings(settings -> settings.setSeed(seed)).create()){
    c.waitTicks(30);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule random_tick_speed 0");w.getServer().runCommand("time set 18000");
    BlockPos habitat=w.getServer().computeOnServer(s -> {var source=s.overworld().getChunkSource();var hit=source.getGenerator().getBiomeSource().findBiomeHorizontal(0,0,0,6400,32,b -> b.is(Biomes.LUSH_CAVES),RandomSource.create(912),true,source.randomState());check(hit!=null,"Real generated seed has a cave-biome observation sample");return hit.getFirst();});
    for(int radius=0;radius<=4;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
     if(radius>0 && Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;int cx=(habitat.getX()>>4)+dx,cz=(habitat.getZ()>>4)+dz;
     w.getServer().runOnServer(s -> {var l=s.overworld();l.getChunk(cx,cz);requested.add(net.minecraft.world.level.ChunkPos.pack(cx,cz));totals[0]++;
      for(int x=cx*16;x<cx*16+16;x++)for(int z=cz*16;z<cz*16+16;z++)for(int y=Math.max(-64,l.getMinY());y<50;y++){
       var at=new BlockPos(x,y,z);var state=l.getBlockState(at);
       if(state.is(DrainhouseContent.MARK) && l.getBlockEntity(at) instanceof DrainhouseMarkEntity e && e.authentic()){int kind=state.getValue(DrainhouseMark.KIND);totals[kind+1]++;sites.add(center(at,kind));}
       if(y<48 && state.isAir() && GlowcapBlock.footing(l,at) && GlowcapBlock.structuralCover(l,at) && (l.getBiome(at).is(Biomes.LUSH_CAVES) || l.getBiome(at).is(Biomes.DRIPSTONE_CAVES))){totals[4]++;if(totals[4]<=32 && DrainhouseFeature.room(l,at))totals[5]++;}
      }
     });c.waitTicks(1);
    }
    // Genuine natural commits may lie in already-generated dependency chunks beside the81 observations.
    // Inspect only retained accepted centers, never another chunk-wide search or feature invocation.
    int scannedSites=sites.size();int[] committedWitnesses=new int[3];
    w.getServer().runOnServer(s -> {var l=s.overworld();var commits=trace.localAcceptedCenters(l);committedWitnesses[0]=commits.size();for(var at:commits){if(!withinBorder(at,habitat)){committedWitnesses[2]++;continue;}completeAndVerify(l,at,habitat,requested);if(sites.add(at))committedWitnesses[1]++;}
     dev.wildercord.Wildercord.LOGGER.info("DRAINHOUSE_NATURAL_COMMIT_WITNESSES seed={} scannedLedgerSites={} retainedActualAcceptedCenters={} additionalVerifiedDependencySites={} outsideFixedBorderUnverified={} acceptedTrackerOverflow={} actualVerifiedCenters={}; accepted WorldGenRegion result only, exact ledgers/buds verified, no broader chunk search",seed,scannedSites,committedWitnesses[0],committedWitnesses[1],committedWitnesses[2],trace.acceptedOverflow(),sites);
    });
    w.getServer().runOnServer(server -> translationProbe(server.overworld(),trace));
    dev.wildercord.Wildercord.LOGGER.info("DRAINHOUSE_TERRAIN seed={} habitat={} chunks={} threshold/garden/alcove={}/{}/{} eligibleFloors={} first32FullFits={} uniqueSites={}",seed,habitat,totals[0],totals[1],totals[2],totals[3],totals[4],totals[5],sites.size());
    // Only naturally discovered centers are checked; complete their immediate boundary chunks explicitly.
    for(var at:sites)w.getServer().runOnServer(s -> completeAndVerify(s.overworld(),at,habitat,requested));
    if(!sites.isEmpty()){seedsWithSites++;review=sites.iterator().next();var focus=review;ordinaryEntry(c,w,focus,seed);w.getServer().runOnServer(s -> {var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.setNoGravity(true);p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,1200,0,false,false,false));p.teleportTo(s.overworld(),focus.getX()-6.5,focus.getY(),focus.getZ()+.5,Set.<Relative>of(),-90,10,false);});c.waitTicks(15);w.getConnection().waitForChunksRender();c.takeScreenshot(TestScreenshotOptions.of("drainhouse_natural_"+seed).disableCounterPrefix());saved=w.getWorldSave();}
    check(requested.size()<=121,"Boundary completion stays within the sampled nine-by-nine footprint plus one chunk border");dev.wildercord.Wildercord.LOGGER.info("DRAINHOUSE_TERRAIN_LOADS seed={} observedChunks={} uniqueExplicitChunkRequests={} boundaryCompletionRequests={}; ordinary generator dependencies/render/reopen loads are not included",seed,totals[0],requested.size(),requested.size()-totals[0]);totalRequests+=requested.size();totalSites+=sites.size();totalChunks+=totals[0];
   }
   if(saved!=null){var at=review;try(var w=saved.open()){c.waitTicks(10);w.getServer().runOnServer(s -> check(authentic(s.overworld(),at.offset(-6,0,1),0) && authentic(s.overworld(),at.offset(0,0,1),1) && authentic(s.overworld(),at.offset(6,1,1),2),"Natural site's authentic three-room provenance survives full saved-world reopen"));}}
   }finally{DrainhouseFeature.observer=null;trace.report();}
  }
  dev.wildercord.Wildercord.LOGGER.info("DRAINHOUSE_TERRAIN_TOTAL seeds={} seedsWithSites={} sampledChunks={} explicitSampleAndBoundaryRequests={} sites={}",SEEDS.length,seedsWithSites,totalChunks,totalRequests,totalSites);
  check(totalChunks==243,"Natural sampling remains fixed at81 chunks across three seeds");check(totalSites>0,"At least one authentic house must naturally generate in the three seed worlds with81 observed chunks each, including verified generation dependencies within the121-chunk border; zero cannot be replaced with fabricated placement");
 }


 /** Coordinate bound, not merely a count: the known footprint plus mouths stays in base±5 chunks. */
 private static boolean withinBorder(BlockPos house,BlockPos habitat){int cx=habitat.getX()>>4,cz=habitat.getZ()>>4;return ((house.getX()-10)>>4)>=cx-5 && ((house.getX()+10)>>4)<=cx+5 && ((house.getZ()-4)>>4)>=cz-5 && ((house.getZ()+4)>>4)<=cz+5;}
 private static void completeAndVerify(ServerLevel l,BlockPos at,BlockPos habitat,Set<Long> requested){
  check(withinBorder(at,habitat),"Known natural house's complete footprint/mouths retain the fixed121-chunk coordinate border before requests");
  int baseX=habitat.getX()>>4,baseZ=habitat.getZ()>>4;
  for(int cx=(at.getX()-10)>>4;cx<=((at.getX()+10)>>4);cx++)for(int cz=(at.getZ()-4)>>4;cz<=((at.getZ()+4)>>4);cz++){check(Math.abs(cx-baseX)<=5 && Math.abs(cz-baseZ)<=5,"Every known-footprint explicit request stays within the fixed border");if(requested.add(net.minecraft.world.level.ChunkPos.pack(cx,cz)))l.getChunk(cx,cz);}
  check(requested.size()<=121,"Verified known-footprint completion retains121 maximum unique explicit requests");
  check(authentic(l,at.offset(-6,0,1),0) && authentic(l,at.offset(0,0,1),1) && authentic(l,at.offset(6,1,1),2),"Every naturally committed house retains all three authentic distinct ledgers before it earns site credit");
  for(int x:new int[]{-1,1}){var bud=at.offset(x,0,-2);check(l.getBlockState(bud).is(FungalGarden.GLOWCAP) && l.getBlockState(bud).getValue(GlowcapBlock.AGE)==0 && GlowcapBlock.conditions(l,bud),"Natural Drainhouse contains actual supported damp covered buds, not mature loot");}
 }

 /** Naturally discovered house only: one initial observation teleport, then real client input. */
 private static void ordinaryEntry(ClientGameTestContext c,TestSingleplayerContext w,BlockPos house,String seed){
  boolean[] keys=c.computeOnClient(mc -> new boolean[]{mc.options.keyUp.isDown(),mc.options.keyDown.isDown(),mc.options.keyLeft.isDown(),mc.options.keyRight.isDown(),mc.options.keyJump.isDown(),mc.options.keyShift.isDown(),mc.options.keySprint.isDown()});
  try{
   c.runOnClient(mc -> {mc.options.keyUp.setDown(false);mc.options.keyDown.setDown(false);mc.options.keyLeft.setDown(false);mc.options.keyRight.setDown(false);mc.options.keyJump.setDown(false);mc.options.keyShift.setDown(false);mc.options.keySprint.setDown(false);});
   w.getServer().runOnServer(s -> {var l=s.overworld();int[][] mouths={{-10,0,0,1,-90},{-6,-4,1,0,0},{0,4,1,0,180},{10,0,0,1,90}};BlockPos chosen=null;int yaw=0;
    for(var m:mouths){var outside=house.offset(m[0],0,m[1]);if(!DrainhouseFeature.dryMouthAt(l,outside,m[2],m[3]))continue;
     int dx=m[0]-Integer.signum(m[0])*(m[2]==0?1:0),dz=m[1]-Integer.signum(m[1])*(m[3]==0?1:0);boolean clear=true;
     for(int d=-1;d<=1 && clear;d++){var door=house.offset(dx+d*m[2],0,dz+d*m[3]);var body=new AABB(door.getX()+.2,door.getY(),door.getZ()+.2,door.getX()+.8,door.getY()+1.8,door.getZ()+.8);if(l.getBlockCollisions(null,body).iterator().hasNext() || !l.getFluidState(door).isEmpty() || !l.getFluidState(door.above()).isEmpty())clear=false;}
     if(clear){for(int d:new int[]{0,-1,1}){var next=house.offset(dx+d*m[2]-Integer.signum(m[0])*(m[2]==0?1:0),0,dz+d*m[3]-Integer.signum(m[1])*(m[3]==0?1:0));var body=new AABB(next.getX()+.2,next.getY(),next.getZ()+.2,next.getX()+.8,next.getY()+1.8,next.getZ()+.8);if(!l.getBlockCollisions(null,body).iterator().hasNext() && l.getFluidState(next).isEmpty() && l.getFluidState(next.above()).isEmpty()){chosen=outside.offset(d*m[2],0,d*m[3]);yaw=m[4];break;}}if(chosen!=null)break;}
    }
    check(chosen!=null,"A genuinely discovered house retains one dry player-clear entrance after all ore infill");var foot=chosen.below();var shape=l.getBlockState(foot).getCollisionShape(l,foot);if(shape.isEmpty()){foot=foot.below();shape=l.getBlockState(foot).getCollisionShape(l,foot);}check(!shape.isEmpty(),"The actual outside observation position has its original solid footing");double floorY=foot.getY()+shape.max(Direction.Axis.Y);
    var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.getAbilities().flying=false;p.setNoGravity(false);p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,1200,0,false,false,false));p.teleportTo(l,chosen.getX()+.5,floorY,chosen.getZ()+.5,Set.<Relative>of(),yaw,0,false);
    dev.wildercord.Wildercord.LOGGER.info("DRAINHOUSE_ENTRY seed={} discoveredHouse={} originalMouth={} actualFoot={} initialPlayerY={} facing={}; no blocks, feature calls, inventory grants or velocity writes",seed,house,chosen,foot,floorY,yaw);
   });c.waitTicks(10);w.getConnection().waitForChunksRender();
   check(w.getServer().computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().onGround()),"Actual Survival player stands on original natural entrance footing before movement");
   c.runOnClient(mc -> {mc.options.keyUp.setDown(true);mc.options.keyJump.setDown(true);});boolean entered=false;
   for(int step=0;step<20;step++){c.waitTicks(2);entered=w.getServer().computeOnServer(s -> {var p=s.getPlayerList().getPlayers().getFirst();return p.getX()>house.getX()-8.5 && p.getX()<house.getX()+8.5 && p.getZ()>house.getZ()-2.5 && p.getZ()<house.getZ()+2.5;});if(entered)break;}
   c.runOnClient(mc -> {mc.options.keyUp.setDown(false);mc.options.keyJump.setDown(false);});check(entered,"Ordinary real client walking/jump enters the naturally generated house, without further teleport or motion injection");
   for(int step=0;step<30 && !w.getServer().computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().onGround());step++)c.waitTicks(1);
   w.getServer().runOnServer(s -> {var p=s.getPlayerList().getPlayers().getFirst();check(p.onGround() && p.isAlive() && !p.getAbilities().flying,"Real entrant lands alive on the authored floor with flying disabled");check(authentic(s.overworld(),house.offset(-6,0,1),0) && authentic(s.overworld(),house.offset(0,0,1),1) && authentic(s.overworld(),house.offset(6,1,1),2),"Ordinary entrance traversal preserves all natural authentic ledgers");dev.wildercord.Wildercord.LOGGER.info("DRAINHOUSE_ENTRY_ACCEPTED seed={} actualPlayer={} ground={} survival={}",seed,p.position(),p.onGround(),!p.isCreative());});
   c.takeScreenshot(TestScreenshotOptions.of("drainhouse_natural_entry_"+seed).disableCounterPrefix());
  }finally{c.runOnClient(mc -> {mc.options.keyUp.setDown(keys[0]);mc.options.keyDown.setDown(keys[1]);mc.options.keyLeft.setDown(keys[2]);mc.options.keyRight.setDown(keys[3]);mc.options.keyJump.setDown(keys[4]);mc.options.keyShift.setDown(keys[5]);mc.options.keySprint.setDown(keys[6]);});}
 }

 private record Fitted(BlockPos originalOrigin,BlockPos actualFit,String mode){
  BlockPos sampledAnchor(){return mode.equals("canonical-north")?actualFit.offset(-6,0,-4):actualFit;}
 }

 /** Actual existing mouth geometry only; no clearing, teleports, placement or relaxed gates. */
 private static void mouthOccupancy(ServerLevel l,String seed,Fitted observed){
  var p=observed.sampledAnchor();boolean loaded=!l.isOutsideBuildHeight(p.below()) && !l.isOutsideBuildHeight(p.above(2));
  // Includes collision cursor neighbors and the outward two-cell approach in every direction.
  for(int cx=(p.getX()-3)>>4;cx<=((p.getX()+3)>>4) && loaded;cx++)for(int cz=(p.getZ()-3)>>4;cz<=((p.getZ()+3)>>4);cz++)if(!l.hasChunkAt(new BlockPos(cx<<4,p.getY(),cz<<4))){loaded=false;break;}
  if(!loaded){dev.wildercord.Wildercord.LOGGER.info("DRAINHOUSE_MOUTH seed={} actualFit={} occupancyDiagnostic=unloaded; no chunk request",seed,p);return;}
  // Five distinct columns times four heights: raw cross-section, including actual foot states.
  var states=new ArrayList<String>(20);
  for(int[] offset:new int[][]{{0,0},{-1,0},{1,0},{0,-1},{0,1}})for(int y=-1;y<=2;y++){
   var at=p.offset(offset[0],y,offset[1]);var state=l.getBlockState(at);
   states.add(offset[0]+","+y+","+offset[1]+"="+state+" collisionEmpty="+state.getCollisionShape(l,at).isEmpty()+" fluid="+state.getFluidState());
  }
  var bodyRoutes=new ArrayList<String>(12);
  // West/north/south/east: the sampled point is the targeted external mouth; depth goes outward.
  for(int[] direction:new int[][]{{-1,0},{0,-1},{0,1},{1,0}})for(int depth=0;depth<=2;depth++){
   var at=p.offset(direction[0]*depth,0,direction[1]*depth);var floor=at.below();var floorState=l.getBlockState(floor);var floorShape=floorState.getCollisionShape(l,floor);
   var body=new AABB(at.getX()+.2,at.getY(),at.getZ()+.2,at.getX()+.8,at.getY()+1.8,at.getZ()+.8);
   boolean bodyCollisionFree=!l.getBlockCollisions(null,body).iterator().hasNext();
   boolean bodyDry=l.getFluidState(at).isEmpty() && l.getFluidState(at.above()).isEmpty();
   bodyRoutes.add("outward="+direction[0]+","+direction[1]+" depth="+depth+" player0.6x1.8BlockClear="+bodyCollisionFree+" bodyDry="+bodyDry+" foot="+floorState+" footSturdy="+floorState.isFaceSturdy(l,floor,Direction.UP)+" footShapeTop="+(floorShape.isEmpty()?0:floorShape.max(Direction.Axis.Y))+" footFluid="+floorState.getFluidState());
  }
  dev.wildercord.Wildercord.LOGGER.info("DRAINHOUSE_MOUTH seed={} sampledAnchor={} actualRoomFit={} fitMode={} originalOrigin={} cross20States={} outward12PlayerBodyChecks={}; existing generated cells only, no terrain edits; body clearance alone is not feature admission or an accepted natural site",seed,p,observed.actualFit(),observed.mode(),observed.originalOrigin(),states,bodyRoutes);
 }

 private record Shift(int dx,int dz,String label){}
 /** Diagnostic only: five observed real fits times twelve distinct translations, never a placement. */
 private static void translationProbe(ServerLevel l,Trace trace){
  var actual=trace.localCandidates(l);int evaluated=0,loadedRefused=0,geometryAccepted=0,anchorAccepted=0;
  for(var observed:actual){
   mouthOccupancy(l,trace.seed,observed);
   var reasons=new TreeMap<String,Integer>();var successful=new ArrayList<String>(12);var canonical=new ArrayList<String>(4);
   var shifts=List.of(new Shift(-4,-4,"grid"),new Shift(-4,0,"grid"),new Shift(-4,4,"grid"),new Shift(0,-4,"canonical-south+grid"),new Shift(0,0,"original"),new Shift(0,4,"grid"),new Shift(4,-4,"grid"),new Shift(4,0,"grid"),new Shift(4,4,"grid"),new Shift(10,0,"canonical-west"),new Shift(6,4,"canonical-north"),new Shift(-10,0,"canonical-east"));
   for(var shift:shifts){int dx=shift.dx(),dz=shift.dz();
    var at=observed.sampledAnchor().offset(dx,0,dz);evaluated++;
    // Check all support, blueprint, gutter and mouth columns before any terrain query.
    boolean loaded=!l.isOutsideBuildHeight(at.below(3)) && !l.isOutsideBuildHeight(at.above(4));
    int originX=observed.originalOrigin().getX()>>4,originZ=observed.originalOrigin().getZ()>>4;
    for(int cx=(at.getX()-10)>>4;cx<=((at.getX()+10)>>4) && loaded;cx++)for(int cz=(at.getZ()-4)>>4;cz<=((at.getZ()+4)>>4);cz++){
     if(Math.abs(cx-originX)>1 || Math.abs(cz-originZ)>1 || !l.hasChunkAt(new BlockPos(cx<<4,at.getY(),cz<<4))){loaded=false;break;}
    }
    if(!loaded){loadedRefused++;reasons.merge("unloaded-or-original-write-zone",1,Integer::sum);if(shift.label().startsWith("canonical"))canonical.add(shift.label()+"=unloaded-or-original-write-zone");continue;}
    boolean anchor=l.isEmptyBlock(at) && GlowcapBlock.footing(l,at) && GlowcapBlock.structuralCover(l,at) && (l.getBiome(at).is(Biomes.LUSH_CAVES) || l.getBiome(at).is(Biomes.DRIPSTONE_CAVES));
    String reason=DrainhouseFeature.diagnose(l,at);reasons.merge(reason,1,Integer::sum);if(shift.label().startsWith("canonical"))canonical.add(shift.label()+"="+reason+" originalHabitatAdmission="+anchor);
    if(reason.equals("accepted")){geometryAccepted++;if(anchor)anchorAccepted++;successful.add("alignment="+shift.label()+" offset="+dx+","+dz+" center="+at+" originalHabitatAdmission="+anchor);}
   }
   dev.wildercord.Wildercord.LOGGER.info("DRAINHOUSE_TRANSLATION seed={} sampledAnchor={} actualRoomFit={} fitMode={} originalOrigin={} twelveDistinctSameYOffsets(grid[-4,0,4]+canonicalWest10,0North6,4South0,-4East-10,0) reasons={} canonicalResults={} genuinelyAcceptedGeometry={}; no block writes, no added chunk requests, post-generation terrain only",trace.seed,observed.sampledAnchor(),observed.actualFit(),observed.mode(),observed.originalOrigin(),reasons,canonical,successful);
  }
  check(evaluated<=60,"Translation diagnostic evaluates at most five genuine candidates times twelve distinct offsets");
  dev.wildercord.Wildercord.LOGGER.info("DRAINHOUSE_TRANSLATION_TOTAL seed={} retainedActualCandidates={} observedCandidatesOverflow={} evaluated={} loadedOrWriteZoneRefused={} acceptedGeometry={} acceptedWithOriginalHabitat={} limit=60; accepted fits add zero natural site credit; an accepted fit proves some real mouth, not necessarily the targeted canonical mouth",trace.seed,actual.size(),trace.fittedOverflow(),evaluated,loadedRefused,geometryAccepted,anchorAccepted);
 }

 /** Only genuine WorldGenRegion events count; post-generation room probes do not. */
 private static final class Trace implements DrainhouseFeature.Observer {
  private final String seed;private ServerLevel level;private long calls,candidates,fits,budget,commits,accepted;
  private final long[] columns=new long[12],height=new long[112],chunks=new long[256];private int chunkCount;private boolean chunksOverflow;
  private final Map<String,Long> refusals=new TreeMap<>();private final List<String> samples=new ArrayList<>(16);private final Fitted[] fitted=new Fitted[5];private int fittedCount;private boolean fittedOverflow;private final BlockPos[] acceptedCenters=new BlockPos[16];private int acceptedCount;private boolean acceptedOverflow;
  Trace(String seed){this.seed=seed;}
  @Override public synchronized void event(net.minecraft.world.level.WorldGenLevel world,String event,int index,BlockPos origin,BlockPos cell,net.minecraft.world.level.block.state.BlockState state,String reason){
   if(!(world instanceof net.minecraft.server.level.WorldGenRegion) || !world.getLevel().dimension().equals(net.minecraft.world.level.Level.OVERWORLD))return;
   if(level==null)level=world.getLevel();if(level!=world.getLevel())return;
   switch(event){
    case "place" -> {calls++;long key=net.minecraft.world.level.ChunkPos.pack(origin.getX()>>4,origin.getZ()>>4);boolean known=false;for(int i=0;i<chunkCount;i++)if(chunks[i]==key){known=true;break;}if(!known){if(chunkCount<chunks.length)chunks[chunkCount++]=key;else chunksOverflow=true;}}
    case "column" -> columns[index]++;
    case "candidate" -> {candidates++;int y=cell.getY()+64;if(y>=0 && y<height.length)height[y]++;}
    case "fit" -> {fits++;if(fittedCount<fitted.length)fitted[fittedCount++]=new Fitted(origin.immutable(),cell.immutable(),reason.isEmpty()?"original-center":reason);else fittedOverflow=true;}
    case "budget" -> budget++;
    case "commit" -> {commits++;if(reason.equals("accepted")){accepted++;if(acceptedCount<acceptedCenters.length)acceptedCenters[acceptedCount++]=cell.immutable();else acceptedOverflow=true;}}
    case "refusal" -> {refusals.merge(reason,1L,Long::sum);if(samples.size()<16)samples.add("origin="+origin+" cell="+cell+" offset="+(cell==null?"unknown":cell.subtract(origin))+" state="+(state==null?"not retained by existing predicate":state)+" reason="+reason);}
    default -> throw new AssertionError("Unknown finite generation diagnostic event "+event);
   }
  }
  synchronized List<Fitted> localCandidates(ServerLevel current){return level==current?List.copyOf(Arrays.asList(Arrays.copyOf(fitted,fittedCount))):List.of();}
  synchronized boolean fittedOverflow(){return fittedOverflow;}
  synchronized List<BlockPos> localAcceptedCenters(ServerLevel current){return level==current?List.copyOf(Arrays.asList(Arrays.copyOf(acceptedCenters,acceptedCount))):List.of();}
  synchronized boolean acceptedOverflow(){return acceptedOverflow;}
  synchronized void report(){
   dev.wildercord.Wildercord.LOGGER.info("DRAINHOUSE_GENERATION seed={} actualNaturalCalls={} originChunksTracked={} originTrackerOverflow={} candidates={} fitAttempts={} budgetStops={} commitAttempts={} accepted={} columnVisits={} candidateYHistogram[-64..47]={} refusalCounts={} first16Samples={}",seed,calls,chunkCount,chunksOverflow,candidates,fits,budget,commits,accepted,Arrays.toString(columns),Arrays.toString(height),refusals,samples);
   dev.wildercord.Wildercord.LOGGER.info("DRAINHOUSE_ACCEPTED_TRACKER seed={} actualAcceptedEvents={} retainedCenters={} overflow={}; actual centers only, event origin not credited as original invocation",seed,accepted,acceptedCount,acceptedOverflow);
   level=null;
  }
 }
}
