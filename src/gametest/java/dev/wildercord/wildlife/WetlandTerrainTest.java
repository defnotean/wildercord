package dev.wildercord.wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.*;
/** A naturally generated flower on actual swamp terrain; no feature command or planted fixture. */
public final class WetlandTerrainTest implements FabricClientGameTest {
 // Same seed as the retained absence case. This separate bank window has a fixed sampling plan.
 // Native evidence and production/configuration hashes are recorded in docs/audit/wetland-terrain-contract.md.
 private static final String REPRESENTATIVE_SEED="-7620530482425397421";
 private static final BlockPos REPRESENTATIVE_CENTER=new BlockPos(-1744,64,-800);
 private static final WetlandGenerationProbe.Scope SCOPE=new WetlandGenerationProbe.Scope("representative_bank",Long.parseLong(REPRESENTATIVE_SEED),-110,-108,-51,-49);
 @Override public void runTest(ClientGameTestContext c) {
  try(var probe=WetlandGenerationProbe.begin(SCOPE);
      var w=c.worldBuilder().setUseConsistentSettings(false).adjustSettings(settings -> settings.setSeed(REPRESENTATIVE_SEED)).create()) {
   c.waitTicks(35);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 6000");
   BlockPos habitat=w.getServer().computeOnServer(s -> {
    var level=s.overworld();check(level.getSeed()==Long.parseLong(REPRESENTATIVE_SEED),"Representative world seed");
    WetlandPlacementContractTest.registered(level);
    dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.wildlife.WetlandTerrainTest\",\"seed\":\""+level.getSeed()+"\"}");
    dev.wildercord.Wildercord.LOGGER.info("WETLAND_POSITIVE_PROVENANCE seed="+level.getSeed()+", sourceChunk=-109/-50, sourceOriginXZ=-1737/-786, modelOriginY=63, nativeOriginY=in_attempt_receipts, examined=-110..-108/-51..-49, expectedFeatureIndex=103, expectedStepFeatures=114");
    return REPRESENTATIVE_CENTER;
   });
   BlockPos found=null;int loaded=0;var observations=new WetlandTerrainObservation();
   for(int radius=0;radius<=1;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++) {
    if(radius>0 && Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
    int cx=(habitat.getX()>>4)+dx,cz=(habitat.getZ()>>4)+dz;
    var match=w.getServer().computeOnServer(s -> {
     var l=s.overworld();var chunk=l.getChunk(cx,cz);
     var candidate=findByHeightWindow(l,cx,cz);
     observations.observe(l,chunk,candidate);
     return candidate;
    });if(found==null)found=match;loaded++;c.waitTicks(1);
   }
   dev.wildercord.Wildercord.LOGGER.info("WETLAND_TERRAIN_DIAGNOSTIC seed="+REPRESENTATIVE_SEED+", habitat="+habitat+", searchedChunks="+loaded+", "+observations.summary());
   var receipt=probe.receipt();WetlandPlacementContractTest.complete(receipt,9);
   check(loaded==9 && found!=null,"A real natural Moonreed must exist in the entire fixed representative 3 by 3 bank window; never expand or replace this site on failure");
   check(receipt.writes().contains(found),"The independently detected natural bud has an actual successful generation write receipt");
   check(receipt.attempts().stream().anyMatch(row -> row.matches("source=-109/-50, origin=-1737/-?\\d+/-786, .*writeReturn=true")),"The fixed model-selected source and origin X/Z make an actual native write; decorated origin Y is observed, never taken from the model");var at=found;
   w.getServer().runOnServer(s -> {
    var l=s.overworld();var state=l.getBlockState(at);check(state.getValue(MoonreedBlock.AGE)==1 && state.canSurvive(l,at) && MoonreedBlock.moist(l,at) && MoonreedBlock.openSky(l,at),"Natural Moonreed is a supported damp open-sky bud");
    var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.setNoGravity(true);var eye=net.minecraft.world.phys.Vec3.atCenterOf(at).add(0,1.5,-3);var d=net.minecraft.world.phys.Vec3.atCenterOf(at).subtract(eye.add(0,p.getEyeHeight(),0));p.teleportTo(l,eye.x,eye.y,eye.z,Set.<Relative>of(),(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())),false);
   });
   c.waitTicks(15);w.getConnection().waitForChunksRender();c.runOnClient(mc -> {mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});c.takeScreenshot(TestScreenshotOptions.of("wetland_moonreed_natural_swamp").disableCounterPrefix());dev.wildercord.Wildercord.LOGGER.info("WETLAND_TERRAIN naturally generated bud="+at+", searched chunks="+loaded);
  }
 }
 // Both natural terrain cases retain the original detector; the independent census observes all heights.
 static BlockPos findByHeightWindow(ServerLevel l,int cx,int cz) {
  for(int x=cx*16;x<cx*16+16;x++)for(int z=cz*16;z<cz*16+16;z++) {
   int top=l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
   for(int y=top-1;y<=top+1;y++) {var at=new BlockPos(x,y,z);if(l.getBlockState(at).is(WetlandGarden.REED))return at;}
  }
  return null;
 }
 private static void check(boolean b,String why) {if(!b)throw new AssertionError(why);}
}
