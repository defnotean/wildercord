package dev.wildercord.wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.*;
/** A naturally generated flower on actual swamp terrain; no feature command or planted fixture. */
public final class WetlandTerrainTest implements FabricClientGameTest {
 // Keep the seed that failed native CI; this is a reproduction, not a passing-seed selection.
 private static final String REPRODUCTION_SEED="-7620530482425397421";
 private static final BlockPos REPRODUCTION_HABITAT=new BlockPos(-1536,64,-768);
 @Override public void runTest(ClientGameTestContext c) {
  try(var w=c.worldBuilder().setUseConsistentSettings(false).adjustSettings(settings -> settings.setSeed(REPRODUCTION_SEED)).create()) {
   c.waitTicks(35);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 6000");
   BlockPos habitat=w.getServer().computeOnServer(s -> {
    check(s.overworld().getSeed()==Long.parseLong(REPRODUCTION_SEED),"Failed native CI seed is preserved");
    dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.wildlife.WetlandTerrainTest\",\"seed\":\""+s.overworld().getSeed()+"\"}");
    var source=s.overworld().getChunkSource();var b=source.getGenerator().getBiomeSource().findBiomeHorizontal(0,64,0,6400,32,v -> v.is(Biomes.SWAMP),RandomSource.create(912),true,source.randomState());
    check(b!=null,"Normal terrain contains swamp");dev.wildercord.Wildercord.LOGGER.info("WETLAND_TERRAIN seed="+s.overworld().getSeed()+", habitat="+b.getFirst());check(b.getFirst().equals(REPRODUCTION_HABITAT),"Failed native CI habitat and search region are preserved");return b.getFirst();
   });
   BlockPos found=null;int loaded=0;var observations=new WetlandTerrainObservation();
   for(int radius=0;radius<=4 && found==null;radius++)for(int dx=-radius;dx<=radius && found==null;dx++)for(int dz=-radius;dz<=radius && found==null;dz++) {
    if(radius>0 && Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
    int cx=(habitat.getX()>>4)+dx,cz=(habitat.getZ()>>4)+dz;
    found=w.getServer().computeOnServer(s -> {
     var l=s.overworld();var chunk=l.getChunk(cx,cz);
     var candidate=findByHeightWindow(l,cx,cz);
     observations.observe(l,chunk,candidate);
     return candidate;
    });loaded++;c.waitTicks(1);
   }
   dev.wildercord.Wildercord.LOGGER.info("WETLAND_TERRAIN_DIAGNOSTIC seed="+REPRODUCTION_SEED+", habitat="+habitat+", searchedChunks="+loaded+", "+observations.summary());
   check(found!=null,"A native world-generation patch appears within eighty-one swamp chunks");var at=found;
   w.getServer().runOnServer(s -> {
    var l=s.overworld();var state=l.getBlockState(at);check(state.getValue(MoonreedBlock.AGE)==1 && state.canSurvive(l,at) && MoonreedBlock.moist(l,at) && MoonreedBlock.openSky(l,at),"Natural Moonreed is a supported damp open-sky bud");
    var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.setNoGravity(true);var eye=net.minecraft.world.phys.Vec3.atCenterOf(at).add(0,1.5,-3);var d=net.minecraft.world.phys.Vec3.atCenterOf(at).subtract(eye.add(0,p.getEyeHeight(),0));p.teleportTo(l,eye.x,eye.y,eye.z,Set.<Relative>of(),(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())),false);
   });
   c.waitTicks(15);w.getConnection().waitForChunksRender();c.runOnClient(mc -> {mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});c.takeScreenshot(TestScreenshotOptions.of("wetland_moonreed_natural_swamp").disableCounterPrefix());dev.wildercord.Wildercord.LOGGER.info("WETLAND_TERRAIN naturally generated bud="+at+", searched chunks="+loaded);
  }
 }
 // The original detector remains the only source of the pass/fail decision.
 private static BlockPos findByHeightWindow(ServerLevel l,int cx,int cz) {
  for(int x=cx*16;x<cx*16+16;x++)for(int z=cz*16;z<cz*16+16;z++) {
   int top=l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
   for(int y=top-1;y<=top+1;y++) {var at=new BlockPos(x,y,z);if(l.getBlockState(at).is(WetlandGarden.REED))return at;}
  }
  return null;
 }
 private static void check(boolean b,String why) {if(!b)throw new AssertionError(why);}
}
