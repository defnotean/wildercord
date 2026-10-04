package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.TidewardNative.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.List;

/** Native crafting, real input displacement, finite wear and fresh-copy/reopen rest accounting. */
public final class ReedwaterWadersTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext c){long rest;int progress;TestWorldSave save;
  try(var settings=new TidewardNative(c)){
   try(var w=c.worldBuilder().create()){
    c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 6000");
    w.getServer().runOnServer(s -> {var p=player(s);floor(s.overworld());for(int x=-3;x<=3;x++)for(int z=-3;z<=23;z++)s.overworld().setBlock(new BlockPos(x,101,z),(x==-3 || x==3 || z==-3 || z==23)?Blocks.STONE.defaultBlockState():Blocks.WATER.defaultBlockState(),2);p.setGameMode(GameType.SURVIVAL);place(p,.5,101,.5);});
    BelowkeeperTestSupport.craft(c,w,List.of(new ItemStack(Items.LEATHER_BOOTS),new ItemStack(WetlandGarden.FLOSS),new ItemStack(HighlandContent.WINDREED_BRAID),new ItemStack(Items.COPPER_INGOT)),TidewardEquipment.WADERS);
    w.getServer().runOnServer(s -> {player(s).setItemInHand(InteractionHand.MAIN_HAND,BelowkeeperTestSupport.take(player(s),TidewardEquipment.WADERS,1));});c.waitTicks(4);
    c.runOnClient(mc -> mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND));c.waitTicks(5);
    w.getServer().runOnServer(s -> check(player(s).getItemBySlot(EquipmentSlot.FEET).is(TidewardEquipment.WADERS),"Actual native use equips crafted waders in feet"));
    double start=w.getServer().computeOnServer(s -> player(s).getZ());c.runOnClient(mc -> mc.options.keyUp.setDown(true));c.waitTicks(30);c.runOnClient(mc -> mc.options.keyUp.setDown(false));
    double worn=w.getServer().computeOnServer(s -> {var p=player(s);check(p.getZ()>start+.4,"Actual walking input moves the connected wearer through water");check(p.getItemBySlot(EquipmentSlot.FEET).getDamageValue()>0 && p.getAttachedOrElse(TidewardEquipment.WALK_PROGRESS,0)>0,"Actual supported moving ticks spend finite wear and progress");return p.getZ()-start;});
    c.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));c.waitTicks(3);shot(c,"tideward_waders_shallow_actual_player");
    // Ordinary inventory swap removes the worn item rather than forging its attribute state.
    c.runOnClient(mc -> mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId,8,mc.player.getInventory().getSelectedSlot(),net.minecraft.world.inventory.ContainerInput.SWAP,mc.player));c.waitTicks(3);
    w.getServer().runOnServer(s -> {check(!player(s).getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY).hasModifier(dev.wildercord.Wildercord.id("reedwater_walk")),"Native unequip clears the actual transient water modifier");place(player(s),.5,101,.5);});c.waitTicks(3);
    double normalStart=w.getServer().computeOnServer(s -> player(s).getZ());c.runOnClient(mc -> mc.options.keyUp.setDown(true));c.waitTicks(30);c.runOnClient(mc -> mc.options.keyUp.setDown(false));
    double ordinary=w.getServer().computeOnServer(s -> player(s).getZ()-normalStart);check(worn>ordinary*1.08,"Same genuine input interval shows measured shallow-water gain, not just an attribute value");
    c.runOnClient(mc -> mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND));c.waitTicks(4);
    // Genuine forward/backward walking stays on the supplied track until the actual shared rest is earned.
    for(int run=0;run<12;run++){final boolean back=run%2==1;c.runOnClient(mc -> {mc.options.keyUp.setDown(!back);mc.options.keyDown.setDown(back);});c.waitTicks(20);if(w.getServer().computeOnServer(s -> player(s).getAttachedOrElse(TidewardEquipment.WALK_READY,0L)>s.overworld().getGameTime()))break;}
    c.runOnClient(mc -> {mc.options.keyUp.setDown(false);mc.options.keyDown.setDown(false);});
    rest=w.getServer().computeOnServer(s -> {var p=player(s);long deadline=p.getAttachedOrElse(TidewardEquipment.WALK_READY,0L);check(deadline>s.overworld().getGameTime(),"A real hundred supported movement ticks earns shared rest");check(!p.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY).hasModifier(dev.wildercord.Wildercord.id("reedwater_walk")),"Rest removes water movement modifier");return deadline;});
    // Move only the save fixture to actual dry support: residual water motion must not change
    // partial accounting between recording it and the reopened assertion.
    w.getServer().runOnServer(s -> place(player(s),4.5,101,.5));c.waitTicks(3);
    progress=w.getServer().computeOnServer(s -> player(s).getAttachedOrElse(TidewardEquipment.WALK_PROGRESS,0));save=w.getWorldSave();
   }
   try(var w=save.open()){c.waitTicks(5);w.getServer().runOnServer(s -> {check(player(s).getAttachedOrElse(TidewardEquipment.WALK_READY,0L)==rest,"Full native reopen retains exact shared rest");check(player(s).getAttachedOrElse(TidewardEquipment.WALK_PROGRESS,0)==progress,"Unspent wear progress is not reset by reconnect");});}
  }
 }
}
