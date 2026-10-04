package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.TidewardNative.*;
import dev.wildercord.client.fx.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.List;

/** Actual living warning, head-slot wear and an owner-only observation with obstruction refusal. */
public final class DewglassSpectaclesTest implements FabricClientGameTest {
 private ReedbackCrab crab;
 @Override public void runTest(ClientGameTestContext c){long deadline;TestWorldSave save;
  try(var settings=new TidewardNative(c)){
   try(var w=c.worldBuilder().create()){
    c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("difficulty normal");w.getServer().runCommand("time set 6000");
    w.getServer().runOnServer(s -> {floor(s.overworld());player(s).setGameMode(GameType.SURVIVAL);place(player(s),.5,101,.5);});
    BelowkeeperTestSupport.craft(c,w,List.of(new ItemStack(Items.GLASS_PANE),new ItemStack(Items.COPPER_INGOT),new ItemStack(WetlandGarden.FLOSS),ItemStack.EMPTY),TidewardEquipment.SPECTACLES);
    w.getServer().runOnServer(s -> player(s).setItemInHand(InteractionHand.MAIN_HAND,BelowkeeperTestSupport.take(player(s),TidewardEquipment.SPECTACLES,1)));c.waitTicks(4);
    c.runOnClient(mc -> {mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);MagicQuality.own=MagicQuality.Level.FULL;MagicQuality.others=MagicQuality.Level.MINIMAL;mc.options.keyShift.setDown(true);});c.waitTicks(5);
    long before=c.computeOnClient(mc -> TidewardClient.warningAccepted());
    w.getServer().runOnServer(s -> {var l=s.overworld();l.setBlock(new BlockPos(0,101,1),Blocks.STONE.defaultBlockState(),2);l.setBlock(new BlockPos(0,102,1),Blocks.STONE.defaultBlockState(),2);
     crab=ReedbackContent.CRAB.create(l,EntitySpawnReason.COMMAND);crab.snapTo(.5,101,2.5,180,0);crab.setPersistenceRequired();l.addFreshEntity(crab);crab.setTarget(player(s));
    });
    await(c,w,s -> crab.pose()==ReedbackCrab.WARNING,30,"Actual territorial AI enters its warning behind the obstruction");c.waitTicks(11);
    check(c.computeOnClient(mc -> TidewardClient.warningAccepted())==before,"Obstructed native warning sends no accepted client observation");
    w.getServer().runOnServer(s -> {check(player(s).getItemBySlot(EquipmentSlot.HEAD).getDamageValue()==0 && player(s).getAttachedOrElse(TidewardEquipment.SIGHT_READY,0L)==0,"Occlusion spends neither spectacle wear nor saved rest");s.overworld().removeBlock(new BlockPos(0,101,1),false);s.overworld().removeBlock(new BlockPos(0,102,1),false);});
    await(c,w,s -> player(s).getAttachedOrElse(TidewardEquipment.SIGHT_READY,0L)>s.overworld().getGameTime(),20,"Real crouched viewing completes half-second preparation");c.waitTicks(1);
    check(c.computeOnClient(mc -> TidewardClient.warningAccepted())==before+1,"Exactly one genuine private warning packet reaches the connected wearer");shot(c,"tideward_spectacles_actual_warning_full");
    deadline=w.getServer().computeOnServer(s -> {var p=player(s);check(p.getItemBySlot(EquipmentSlot.HEAD).getDamageValue()==1,"One real warning spends one actual worn lens durability");return p.getAttachedOrElse(TidewardEquipment.SIGHT_READY,0L);});
    w.getServer().runOnServer(s -> {player(s).setItemSlot(EquipmentSlot.HEAD,new ItemStack(TidewardEquipment.SPECTACLES));crab.discard();crab=ReedbackContent.CRAB.create(s.overworld(),EntitySpawnReason.COMMAND);crab.snapTo(.5,101,2.5,180,0);crab.setPersistenceRequired();s.overworld().addFreshEntity(crab);crab.setTarget(player(s));});
    await(c,w,s -> crab.pose()==ReedbackCrab.WARNING,12,"A second actual living warning supplies an eligible fresh-copy rest attempt");c.waitTicks(11);
    w.getServer().runOnServer(s -> {check(crab.pose()==ReedbackCrab.WARNING,"The rest refusal is observed while the real second warning is still active");check(player(s).getAttachedOrElse(TidewardEquipment.SIGHT_READY,0L)==deadline && player(s).getItemBySlot(EquipmentSlot.HEAD).getDamageValue()==0,"A supplied fresh copy cannot bypass shared rest against an eligible real warning");crab.discard();});
    check(c.computeOnClient(mc -> TidewardClient.warningAccepted())==before+1,"Fresh-copy warning attempt emits no second observation");
    save=w.getWorldSave();
   }
   try(var w=save.open()){c.waitTicks(5);w.getServer().runOnServer(s -> check(player(s).getAttachedOrElse(TidewardEquipment.SIGHT_READY,0L)==deadline,"Full world reopen retains the exact observation rest"));}
  }
 }
}
