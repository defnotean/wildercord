package dev.wildercord.cast;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Relative;
import java.util.Set;

/** Actual interaction and rod-position checks, including a rotated Storm Spire layout. */
public final class NewEncountersTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext context) {
  try(var world=context.worldBuilder().create()) {
   context.waitTicks(40);
   world.getServer().runCommand("gamerule spawn_mobs false");
   world.getServer().runCommand("time set 6000");
   world.getServer().runCommand("fill -12 98 -12 12 98 12 stone");
   context.runOnClient(mc->{mc.getWindow().setWindowed(1600,900);mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);});
   int rootId=world.getServer().computeOnServer(server->{
    var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
    p.teleportTo(p.level(),.5,99,-4.5,Set.<Relative>of(),0,0,false);
    var root=RootGuardian.rise(p.level(),new BlockPos(0,98,2));root.setNoAi(true);root.setYRot(180);root.setYBodyRot(180);root.setYHeadRot(180);
    check(root.bindings()==3,"root begins with three bindings");
    check(Math.abs(root.resist(p.level(),p.damageSources().generic(),10)-3.5)<.01,"root bindings soften incoming damage");
    p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHEARS));
    root.mobInteract(p,InteractionHand.MAIN_HAND);check(root.bindings()==2,"real shears interaction prunes root");
    root.mobInteract(p,InteractionHand.MAIN_HAND);check(root.bindings()==2,"pruning cooldown stops click spam");
    return root.getId();
   });
   context.waitTicks(15);
   world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();((RootGuardian)p.level().getEntity(rootId)).mobInteract(p,InteractionHand.MAIN_HAND);});
   context.waitTicks(15);
   world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();var root=(RootGuardian)p.level().getEntity(rootId);
    root.mobInteract(p,InteractionHand.MAIN_HAND);check(root.bindings()==0,"all bindings can be pruned");
    check(root.resist(p.level(),p.damageSources().generic(),10)==10,"exposed heart takes full damage");
   });
   context.waitTicks(5);context.takeScreenshot(TestScreenshotOptions.of("root_guardian_exposed").disableCounterPrefix());
   world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();var root=(RootGuardian)p.level().getEntity(rootId);root.setNoAi(false);});
   context.waitTicks(125);
   world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();var root=(RootGuardian)p.level().getEntity(rootId);check(root.bindings()==3,"root bindings regrow after vulnerability window");root.setNoAi(true);});
   context.takeScreenshot(TestScreenshotOptions.of("root_guardian_bound").disableCounterPrefix());
   int stormId=world.getServer().computeOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.level().getEntity(rootId).discard();
    BlockPos altar=new BlockPos(0,98,2);
    // Same layout rotated through ninety degrees: the boss must discover actual rods.
    for(int z:new int[]{-3,2,7})p.level().setBlockAndUpdate(new BlockPos(-1,98,z),Blocks.LIGHTNING_ROD.weathering().unaffected().defaultBlockState());
    var boss=StormConductor.rise(p.level(),altar);boss.setNoAi(true);
    check(boss.activeRod().equals(new BlockPos(-1,98,-3)),"rod lookup follows rotated structure layout");
    check(boss.resist(p.level(),p.damageSources().generic(),10)==5,"charged conductor has partial defence");
    p.teleportTo(p.level(),-1.5,99,-3.5,Set.<Relative>of(),0,0,false);boss.setNoAi(false);
    return boss.getId();
   });
   context.waitTicks(20);
   world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();var boss=(StormConductor)p.level().getEntity(stormId);
    check(boss.state(DungeonBoss.EXPOSED),"standing beside actual rod grounds conductor");
    check(boss.resist(p.level(),p.damageSources().generic(),10)==10,"grounded conductor takes full damage");
    check(!boss.ground(p.level()),"grounding cooldown prevents permanent stun");
    check(boss.activeRod().equals(new BlockPos(-1,98,2)),"active rod moves after grounding");
    boss.setNoAi(true);p.teleportTo(p.level(),.5,99,-4.5,Set.<Relative>of(),0,0,false);
   });
   context.waitTicks(5);context.takeScreenshot(TestScreenshotOptions.of("storm_conductor_grounded").disableCounterPrefix());
  }
 }
 private static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
}
