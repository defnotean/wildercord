package dev.wildercord.wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.*;
/** A naturally generated flower on actual swamp terrain; no feature command or planted fixture. */
public final class WetlandTerrainTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext c) {
  try(var w=c.worldBuilder().setUseConsistentSettings(false).create()) {
   c.waitTicks(35);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 6000");
   BlockPos habitat=w.getServer().computeOnServer(s -> {
    var source=s.overworld().getChunkSource();var b=source.getGenerator().getBiomeSource().findBiomeHorizontal(0,64,0,6400,32,v -> v.is(Biomes.SWAMP),RandomSource.create(912),true,source.randomState());
    check(b!=null,"Normal terrain contains swamp");dev.wildercord.Wildercord.LOGGER.info("WETLAND_TERRAIN seed="+s.overworld().getSeed()+", habitat="+b.getFirst());return b.getFirst();
   });
   BlockPos found=null;int loaded=0;
   for(int radius=0;radius<=4 && found==null;radius++)for(int dx=-radius;dx<=radius && found==null;dx++)for(int dz=-radius;dz<=radius && found==null;dz++) {
    if(radius>0 && Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
    int cx=(habitat.getX()>>4)+dx,cz=(habitat.getZ()>>4)+dz;
    found=w.getServer().computeOnServer(s -> {
     var l=s.overworld();l.getChunk(cx,cz);
     for(int x=cx*16;x<cx*16+16;x++)for(int z=cz*16;z<cz*16+16;z++) {
      int top=l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
      for(int y=top-1;y<=top+1;y++) {var at=new BlockPos(x,y,z);if(l.getBlockState(at).is(WetlandGarden.REED))return at;}
     }
     return null;
    });loaded++;c.waitTicks(1);
   }
   check(found!=null,"A native world-generation patch appears within eighty-one swamp chunks");var at=found;
   w.getServer().runOnServer(s -> {
    var l=s.overworld();var state=l.getBlockState(at);check(state.getValue(MoonreedBlock.AGE)==1 && state.canSurvive(l,at) && MoonreedBlock.moist(l,at) && MoonreedBlock.openSky(l,at),"Natural Moonreed is a supported damp open-sky bud");
    var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.setNoGravity(true);var eye=net.minecraft.world.phys.Vec3.atCenterOf(at).add(0,1.5,-3);var d=net.minecraft.world.phys.Vec3.atCenterOf(at).subtract(eye.add(0,p.getEyeHeight(),0));p.teleportTo(l,eye.x,eye.y,eye.z,Set.<Relative>of(),(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())),false);
   });
   c.waitTicks(15);w.getConnection().waitForChunksRender();c.runOnClient(mc -> {mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});c.takeScreenshot(TestScreenshotOptions.of("wetland_moonreed_natural_swamp").disableCounterPrefix());dev.wildercord.Wildercord.LOGGER.info("WETLAND_TERRAIN naturally generated bud="+at+", searched chunks="+loaded);
  }
 }
 private static void check(boolean b,String why) {if(!b)throw new AssertionError(why);}
}
