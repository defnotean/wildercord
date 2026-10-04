package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.SiltcrestNative.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;
/** Genuine unpaused predation and grounded covered-bank travel, plus full restart of exact appetite/refuge deadlines. */
public final class SiltcrestEcologyTest implements FabricClientGameTest {
 private SiltcrestBittern bird;private UUID id;private long appetite,until;private BlockPos roof=new BlockPos(-2,103,-2);
 public void runTest(ClientGameTestContext c){var camera=c.computeOnClient(mc->mc.options.getCameraType());boolean hud=c.computeOnClient(mc->mc.gui.hud.isHidden());TestWorldSave save;
  try{c.runOnClient(mc->{mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();});
   try(var w=c.worldBuilder().create()){c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 18000");w.getServer().runCommand("weather clear");
    w.getServer().runOnServer(s->{floor(s.overworld());observer(s.getPlayerList().getPlayers().getFirst(),new Vec3(.5,101.7,-.5));for(int i=0;i<3;i++)fish(s.overworld(),i);bird=bird(s.overworld(),.5,.5);id=bird.getUUID();});
    await(c,w,500,s->bird.pose()==SiltcrestBittern.COILING,"Actual stalking must reach a real fish and coil without teleport/pose injection");c.takeScreenshot(TestScreenshotOptions.of("siltcrest_actual_coil").disableCounterPrefix());
    await(c,w,500,s->bird.huntReady()-s.overworld().getGameTime()>SiltcrestBittern.APPETITE-100,"A real admitted wild-fish death owns saved appetite");
    w.getServer().runOnServer(s->{appetite=bird.huntReady();check(bird.preyPool(s.overworld()).size()>=2,"Successful hunt leaves at least two live eligible prey");check(bird.isAlive()&&!bird.isRemoved(),"Current live bird owns outcome");});c.takeScreenshot(TestScreenshotOptions.of("siltcrest_actual_catch").disableCounterPrefix());
    await(c,w,20,s->bird.pose()==SiltcrestBittern.PREENING,"Six-tick real strike becomes visible preening");c.takeScreenshot(TestScreenshotOptions.of("siltcrest_actual_preen").disableCounterPrefix());
    w.getServer().runOnServer(s->{s.overworld().setBlock(roof,Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT,true),2);s.overworld().setBlock(new BlockPos(-1,100,-2),Blocks.WATER.defaultBlockState(),2);});w.getServer().runCommand("time set 6000");
    await(c,w,420,s->bird.pose()==SiltcrestBittern.SHELTERING,"Actual daylight travel reaches a dry covered bank");w.getServer().runOnServer(s->{check(bird.onGround()&&bird.shelter()!=null&&BitternHabitat.shelter(s.overworld(),bird.shelter())&&bird.distanceToSqr(bird.shelter().getX()+.5,bird.shelter().getY(),bird.shelter().getZ()+.5)<=.36,"Rest is genuinely grounded under actual shelter");check(bird.huntReady()==appetite,"Shelter does not renew appetite");until=bird.shelterUntil();observer(s.getPlayerList().getPlayers().getFirst(),bird.position().add(0,.5,0));});c.waitTicks(2);c.takeScreenshot(TestScreenshotOptions.of("siltcrest_actual_shelter").disableCounterPrefix());save=w.getWorldSave();
   }
   try(var w=save.open()){c.waitTicks(25);w.getServer().runOnServer(s->{bird=(SiltcrestBittern)s.overworld().getEntity(id);check(bird!=null&&bird.huntReady()==appetite&&bird.shelterUntil()==until,"Exact shared appetite and refuge deadline persist across full server reopen");check(bird.pose()==SiltcrestBittern.SHELTERING&&bird.onGround(),"A still-valid real refuge resumes grounded rest without a new deadline");});}
  }finally{c.runOnClient(mc->{mc.options.setCameraType(camera);if(mc.gui.hud.isHidden()!=hud)mc.gui.hud.toggle();});}
 }
}
