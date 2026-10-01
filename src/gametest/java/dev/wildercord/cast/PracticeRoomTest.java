package dev.wildercord.cast;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.world.phys.AABB;

public final class PracticeRoomTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext context) {
  try(var world=context.worldBuilder().create()) {
   context.waitTicks(35);
   world.getServer().runOnServer(server -> {
    var p=server.getPlayerList().getPlayers().getFirst();
    check(PracticeRoom.enter(p)==1,"practice dimension loads");
    check(p.level().dimension().equals(PracticeRoom.DIMENSION),"practice entry transports player");
    check(p.level().getEntitiesOfClass(TrainingDummy.class,arena()).size()==3,"three practice targets");
    PracticeRoom.targets(p.level(),24,true);
    check(p.level().getEntitiesOfClass(TrainingDummy.class,arena()).size()==24,"24-target stress scene");
   });
   context.waitTicks(30);
   context.takeScreenshot(TestScreenshotOptions.of("practice_stress_arena").disableCounterPrefix());
   world.getServer().runOnServer(server -> {
    var p=server.getPlayerList().getPlayers().getFirst();
    check(PracticeRoom.targets(p.level(),3,false)==3,"practice reset");
    check(p.level().getEntitiesOfClass(TrainingDummy.class,arena()).size()==3,"reset removes stress targets");
    check(PracticeRoom.leave(p)==1,"return point survives dimension transfer");
    check(p.level()==server.overworld(),"practice return restores original world");
    check(PracticeRoom.targets(p.level(),24,true)==0,"practice command cannot replace targets in ordinary worlds");
   });
  }
 }
 private static AABB arena(){return new AABB(-12,79,-8,13,85,33);}
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
