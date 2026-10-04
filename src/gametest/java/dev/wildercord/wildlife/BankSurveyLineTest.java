package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.TidewardNative.*;
import dev.wildercord.client.fx.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraft.core.Direction;
import java.util.List;

/** Two native uses read real cells, pay finite wear and cancel when that actual terrain changes. */
public final class BankSurveyLineTest implements FabricClientGameTest {
 private final BlockPos first=new BlockPos(0,100,0),last=new BlockPos(0,100,4);
 private void use(ClientGameTestContext c,BlockPos at){c.runOnClient(mc -> mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at).add(0,.49,0),Direction.UP,at,false)));c.waitTicks(2);}
 @Override public void runTest(ClientGameTestContext c){long deadline;TestWorldSave save;
  try(var settings=new TidewardNative(c)){
   try(var w=c.worldBuilder().create()){
    c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 6000");
    w.getServer().runOnServer(s -> {var p=player(s);floor(s.overworld());p.setGameMode(GameType.SURVIVAL);place(p,2.5,101,2.5);for(int z=1;z<4;z++)s.overworld().setBlock(new BlockPos(0,101,z),Blocks.WATER.defaultBlockState(),2);});
    BelowkeeperTestSupport.craft(c,w,List.of(new ItemStack(Items.STICK),new ItemStack(Items.COPPER_INGOT),new ItemStack(HighlandContent.WINDREED_BRAID),new ItemStack(WetlandGarden.FLOSS)),TidewardSurvey.ITEM);
    w.getServer().runOnServer(s -> player(s).setItemInHand(InteractionHand.MAIN_HAND,BelowkeeperTestSupport.take(player(s),TidewardSurvey.ITEM,1)));c.waitTicks(5);
    long baseline=c.computeOnClient(mc -> TidewardClient.routeAccepted());
    // A real hole makes the second native use refuse; no route, rest or wear is authored.
    w.getServer().runOnServer(s -> s.overworld().setBlock(new BlockPos(0,100,2),Blocks.AIR.defaultBlockState(),2));use(c,first);use(c,last);
    w.getServer().runOnServer(s -> {check(player(s).getMainHandItem().getDamageValue()==0 && player(s).getAttachedOrElse(TidewardSurvey.READY,0L)==0,"Unsupported route pays no wear or rest");s.overworld().setBlock(new BlockPos(0,100,2),Blocks.STONE.defaultBlockState(),2);});
    check(c.computeOnClient(mc -> TidewardClient.routeAccepted())==baseline,"Real unsupported route sends no accepted client route");
    w.getServer().runOnServer(s -> {var p=player(s);dev.wildercord.Wildercord.LOGGER.info("SURVEY_PRE pos={} held={} ready={} floor={}",p.position(),p.getMainHandItem(),p.getAttachedOrElse(TidewardSurvey.READY,0L),TidewardSurvey.columns(first,last).stream().map(at->at+"="+TidewardSurvey.footing(p,at)).toList());});
    use(c,last);await(c,w,s -> player(s).getAttachedOrElse(TidewardSurvey.READY,0L)>s.overworld().getGameTime(),8,"Second native use commits an actual supported route");c.waitTicks(1);
    check(c.computeOnClient(mc -> TidewardClient.routeAccepted())==baseline+1,"One accepted private route reaches the actual connected owner: before="+baseline+" actual="+c.computeOnClient(mc->TidewardClient.routeAccepted())+" local="+c.computeOnClient(mc->TidewardSurvey.columns(first,last).stream().map(at->at+"="+mc.level.getBlockState(at)+"/"+mc.level.getBlockState(at.above())+"/"+mc.level.getBlockState(at.above(2))).toList()));
    deadline=w.getServer().computeOnServer(s -> {check(player(s).getMainHandItem().getDamageValue()==2,"Actual crossing survey costs exactly two wear");return player(s).getAttachedOrElse(TidewardSurvey.READY,0L);});
    c.runOnClient(mc -> {MagicQuality.own=MagicQuality.Level.FULL;MagicQuality.others=MagicQuality.Level.MINIMAL;mc.player.setYRot(90);mc.player.setXRot(24);});c.waitTicks(3);shot(c,"tideward_survey_supported_full");
    // Let all12-tick Full pieces actually expire before recording an own Minimal route.
    c.runOnClient(mc -> {MagicQuality.own=MagicQuality.Level.MINIMAL;MagicQuality.others=MagicQuality.Level.FULL;});c.waitTicks(14);shot(c,"tideward_survey_supported_minimal_and_held");
    c.runOnClient(mc -> mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));c.waitTicks(3);shot(c,"tideward_survey_actual_crafted_icon");c.runOnClient(mc -> mc.gui.setScreen(null));
    long cancelled=c.computeOnClient(mc -> TidewardClient.routeCancelled());w.getServer().runOnServer(s -> s.overworld().setBlock(new BlockPos(0,102,2),Blocks.STONE.defaultBlockState(),2));c.waitTicks(12);
    check(c.computeOnClient(mc -> TidewardClient.routeCancelled())>cancelled,"A real later head obstruction cancels the finite route instead of retaining stale guidance");
    w.getServer().runOnServer(s -> {check(player(s).getMainHandItem().getDamageValue()==2,"Terrain invalidation grants no extra cost or reward");check(s.overworld().getBlockState(first).is(Blocks.STONE) && s.overworld().getBlockState(last).is(Blocks.STONE),"The survey leaves its actual banks unmodified");});save=w.getWorldSave();
   }
   try(var w=save.open()){c.waitTicks(5);w.getServer().runOnServer(s -> {check(player(s).getAttachedOrElse(TidewardSurvey.READY,0L)==deadline,"World reopen preserves exact tool rest without restoring a historic route");check(player(s).getCooldowns().isOnCooldown(new ItemStack(TidewardSurvey.ITEM)),"Join restores the actual visible cooldown for a fresh tool copy while its saved rest remains");});}
  }
 }
}
