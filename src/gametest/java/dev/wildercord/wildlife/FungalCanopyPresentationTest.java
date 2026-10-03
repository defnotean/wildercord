package dev.wildercord.wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.Set;
/** Clear native model review after real navigation/browse/rest; earned acquisition is covered separately. */
public final class FungalCanopyPresentationTest implements FabricClientGameTest {
 private SporebackSnail snail;
 public void runTest(ClientGameTestContext c) {
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule random_tick_speed 0");w.getServer().runCommand("time set 18000");
   var cap=new BlockPos(0,30,0);var roof=new BlockPos(2,31,0);
   w.getServer().runOnServer(s -> {
    var l=s.overworld();for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++) {l.setBlock(new BlockPos(x,29,z),Blocks.MOSS_BLOCK.defaultBlockState(),2);for(int y=30;y<=32;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);l.setBlock(new BlockPos(x,33,z),Blocks.STONE.defaultBlockState(),2);}
    l.setBlock(cap.below().east(),Blocks.WATER.defaultBlockState(),2);l.setBlock(cap,FungalGarden.GLOWCAP.defaultBlockState().setValue(GlowcapBlock.AGE,1),2);l.setBlock(roof,FungalGarden.NURSERY.defaultBlockState(),2);
    check(l.getBlockState(roof).getCollisionShape(l,roof).bounds().minY==-1,"Actual supports reach the lower walking floor");snail=SporebackContent.SNAIL.create(l,EntitySpawnReason.COMMAND);snail.snapTo(-2.5,30,.5,0,0);l.addFreshEntity(snail);
    var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.setNoGravity(true);p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,1200,0,false,false,false));
    var camera=new Vec3(4.8,30,-2);var focus=Vec3.atCenterOf(roof).add(0,-.7,0);var d=focus.subtract(camera.add(0,p.getEyeHeight(),0));p.teleportTo(l,camera.x,camera.y,camera.z,Set.<Relative>of(),(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())),false);
   });
   boolean resting=false;for(int i=0;i<150;i++) {c.waitTicks(5);if(w.getServer().computeOnServer(s -> snail.dew() && snail.pose()==2 && snail.blockPosition().equals(roof.below()))) {resting=true;break;}}
   check(resting,"Actual snail physically browses prepared cap and enters grounded canopy passage");w.getServer().runOnServer(s -> {check(s.overworld().getBlockState(cap).getValue(GlowcapBlock.AGE)==2,"Actual visit opens cap without resource fixtures");check(snail.nurseryReady()>s.overworld().getGameTime() && snail.forageReady()>s.overworld().getGameTime(),"Real independent visitor rests remain finite");dev.wildercord.Wildercord.LOGGER.info("FUNGAL_CANOPY actual resting visitor="+snail.position()+" under="+roof+" forage="+snail.forageReady()+" nursery="+snail.nurseryReady());});
   c.waitTicks(5);w.getConnection().waitForChunksRender();c.runOnClient(mc -> {mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});c.takeScreenshot(TestScreenshotOptions.of("fungal_grounded_canopy_visitor").disableCounterPrefix());
  }
 }
 private static void check(boolean b,String why) {if(!b)throw new AssertionError(why);}
}
