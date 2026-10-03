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
/** Native lush-cave terrain: find both plant and authentic carved clues without feature commands. */
public final class FungalTerrainTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext c) {
  try(var w=c.worldBuilder().setUseConsistentSettings(false).adjustSettings(settings -> settings.setSeed("-7775421970293783948")).create()) {
   c.waitTicks(35);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 6000");
   BlockPos habitat=w.getServer().computeOnServer(s -> {
    var source=s.overworld().getChunkSource();var b=source.getGenerator().getBiomeSource().findBiomeHorizontal(0,0,0,6400,32,v -> v.is(Biomes.LUSH_CAVES),RandomSource.create(912),true,source.randomState());
    check(b!=null,"Normal terrain contains lush caves");dev.wildercord.Wildercord.LOGGER.info("FUNGAL_TERRAIN seed="+s.overworld().getSeed()+", habitat="+b.getFirst());return b.getFirst();
   });
   BlockPos found=null;BlockPos[] clues=new BlockPos[2];int[] counts=new int[7];int loaded=0;
   for(int radius=0;radius<=4;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++) {
    if(radius>0 && Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
    int cx=(habitat.getX()>>4)+dx,cz=(habitat.getZ()>>4)+dz;
    BlockPos plant=w.getServer().computeOnServer(s -> {
     var l=s.overworld();l.getChunk(cx,cz);BlockPos result=null;
     for(int x=cx*16;x<cx*16+16;x++)for(int z=cz*16;z<cz*16+16;z++)for(int y=Math.max(l.getMinY(),-64);y<48;y++) {
      var at=new BlockPos(x,y,z);var state=l.getBlockState(at);if(state.isAir() && GlowcapBlock.footing(l,at)) {counts[3]++;if(GlowcapBlock.moist(l,at)) {counts[4]++;if(GlowcapBlock.structuralCover(l,at)) {counts[5]++;if(l.getBiome(at).is(Biomes.LUSH_CAVES) || l.getBiome(at).is(Biomes.DRIPSTONE_CAVES))counts[6]++;if(counts[5]<=8)dev.wildercord.Wildercord.LOGGER.info("FUNGAL_FLOOR_SAMPLE "+at+" biome="+l.getBiome(at).unwrapKey());}}}
      if(state.is(FungalGarden.GLOWCAP)) {counts[0]++;if(result==null)result=at;}
      if(state.is(FungalGarden.BREATHMARK) && l.getBlockEntity(at) instanceof BreathmarkEntity e && e.authentic()) {int kind=state.getValue(BreathmarkBlock.KIND);clues[kind]=at;counts[kind+1]++;}
     }return result;
    });if(found==null)found=plant;loaded++;c.waitTicks(1);

   }
   dev.wildercord.Wildercord.LOGGER.info("FUNGAL_TERRAIN findings cap="+found+", roots="+clues[0]+", air="+clues[1]+", chunks="+loaded+", counts cap/root/air/foot/moist/coveredMoist/caveCoveredMoist="+java.util.Arrays.toString(counts));
   check(found!=null && clues[0]!=null && clues[1]!=null,"Native cave decoration contains damp cap and both authentic clue kinds within81chunks");var at=found;
   w.getServer().runOnServer(s -> {
    var l=s.overworld();var state=l.getBlockState(at);if(state.isAir() && GlowcapBlock.footing(l,at)) {counts[3]++;if(GlowcapBlock.moist(l,at)) {counts[4]++;if(GlowcapBlock.structuralCover(l,at)) {counts[5]++;if(l.getBiome(at).is(Biomes.LUSH_CAVES) || l.getBiome(at).is(Biomes.DRIPSTONE_CAVES))counts[6]++;if(counts[5]<=8)dev.wildercord.Wildercord.LOGGER.info("FUNGAL_FLOOR_SAMPLE "+at+" biome="+l.getBiome(at).unwrapKey());}}}check(state.canSurvive(l,at) && GlowcapBlock.moist(l,at) && GlowcapBlock.structuralCover(l,at),"Natural cap has supported damp covered habitat");
    var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.setNoGravity(true);p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,1200,0,false,false,false));var focus=net.minecraft.world.phys.Vec3.atBottomCenterOf(at).add(0,.25,0);net.minecraft.world.phys.Vec3 camera=null;
    outer:for(double dy:new double[]{0,.5,1})for(double[] offset:new double[][]{{2,0},{-2,0},{0,2},{0,-2},{1.6,1.6},{-1.6,1.6},{1.6,-1.6},{-1.6,-1.6}}) {
     var feet=net.minecraft.world.phys.Vec3.atBottomCenterOf(at).add(offset[0],dy,offset[1]);var eye=feet.add(0,p.getEyeHeight(),0);var foot=BlockPos.containing(feet);var head=BlockPos.containing(eye);
     if(!l.getBlockState(foot).getCollisionShape(l,foot).isEmpty() || !l.getBlockState(head).getCollisionShape(l,head).isEmpty())continue;
     var hit=l.clip(new net.minecraft.world.level.ClipContext(eye,focus,net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
     if(hit.getType()!=net.minecraft.world.phys.HitResult.Type.MISS && !hit.getBlockPos().equals(at))continue;
     camera=feet;break outer;
    }
    check(camera!=null,"Bounded unobstructed native camera exists around actual generated cap");var d=focus.subtract(camera.add(0,p.getEyeHeight(),0));p.teleportTo(l,camera.x,camera.y,camera.z,Set.<Relative>of(),(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())),false);
   });
   c.waitTicks(15);w.getConnection().waitForChunksRender();c.runOnClient(mc -> {mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});c.takeScreenshot(TestScreenshotOptions.of("fungal_glowcap_natural_cave").disableCounterPrefix());dev.wildercord.Wildercord.LOGGER.info("FUNGAL_TERRAIN naturally generated cap="+at+", searched chunks="+loaded);
  }
 }
 private static void check(boolean b,String why) {if(!b)throw new AssertionError(why);}
}
