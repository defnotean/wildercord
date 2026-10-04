package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.TidewardNative.*;
import dev.wildercord.client.fx.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import java.util.ArrayList;

/** Overfull actual population refuses instead of sorting an unbounded list; Minimal retains a real cue. */
public final class TidewardWarningBoundsTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext c){
  try(var settings=new TidewardNative(c);var w=c.worldBuilder().create()){
   c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("difficulty normal");w.getServer().runCommand("time set 6000");
   var crabs=new ArrayList<ReedbackCrab>();
   w.getServer().runOnServer(s -> {floor(s.overworld());player(s).setGameMode(GameType.SURVIVAL);place(player(s),.5,101,.5);player(s).setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TidewardEquipment.SPECTACLES));});c.waitTicks(4);
   c.runOnClient(mc -> {mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);mc.options.keyShift.setDown(true);MagicQuality.own=MagicQuality.Level.MINIMAL;MagicQuality.others=MagicQuality.Level.FULL;});c.waitTicks(4);
   long initial=c.computeOnClient(mc -> TidewardClient.warningAccepted());
   w.getServer().runOnServer(s -> {for(int i=0;i<13;i++){var crab=ReedbackContent.CRAB.create(s.overworld(),EntitySpawnReason.COMMAND);crab.snapTo(.5+(i%3)*.04,101,2.5+(i/3)*.04,180,0);crab.setPersistenceRequired();s.overworld().addFreshEntity(crab);crab.setTarget(player(s));crabs.add(crab);}});
   await(c,w,s -> crabs.stream().allMatch(x -> x.pose()==ReedbackCrab.WARNING),15,"Actual supplied dense population enters territorial preparation");c.waitTicks(11);
   w.getServer().runOnServer(s -> {check(player(s).getAttachedOrElse(TidewardEquipment.SIGHT_READY,0L)==0 && player(s).getItemBySlot(EquipmentSlot.HEAD).getDamageValue()==0,"Thirteen nearby creatures refuse observation without wear or rest");for(var crab:crabs)crab.discard();crabs.clear();var crab=ReedbackContent.CRAB.create(s.overworld(),EntitySpawnReason.COMMAND);crab.snapTo(.5,101,2.5,180,0);crab.setPersistenceRequired();s.overworld().addFreshEntity(crab);crab.setTarget(player(s));crabs.add(crab);});
   check(c.computeOnClient(mc -> TidewardClient.warningAccepted())==initial,"Dense population sends no accepted private warning");
   await(c,w,s -> player(s).getAttachedOrElse(TidewardEquipment.SIGHT_READY,0L)>s.overworld().getGameTime(),28,"One real visible warning completes actual Minimal observation");c.waitTicks(1);
   check(c.computeOnClient(mc -> TidewardClient.warningAccepted())==initial+1,"Opposing preferences preserve the genuine own Minimal warning cue");shot(c,"tideward_spectacles_actual_warning_minimal");
   w.getServer().runOnServer(s -> {check(player(s).getItemBySlot(EquipmentSlot.HEAD).getDamageValue()==1,"Minimal display does not change actual lens payment");for(var crab:crabs)crab.discard();});
  }
 }
}
