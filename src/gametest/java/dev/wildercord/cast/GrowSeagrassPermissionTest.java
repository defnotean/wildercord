package dev.wildercord.cast;

import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.FluidTags;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Actual paid underwater-neighbor test for the production preflight. */
public final class GrowSeagrassPermissionTest implements FabricClientGameTest {
 private static final AtomicBoolean denyUpper=new AtomicBoolean();
 private static final BlockPos LOWER=new BlockPos(1,101,0),UPPER=LOWER.above();
 @Override public void runTest(ClientGameTestContext c){
  PlayerBlockBreakEvents.BEFORE.register((l,p,pos,state,be)->!denyUpper.get()||!pos.equals(UPPER));
  try(var world=c.worldBuilder().create()){
   c.waitTicks(30);var server=world.getServer();server.runCommand("gamerule spawn_mobs false");
   server.runCommand("fill -8 100 -8 8 100 8 dirt");
   server.runCommand("fill -5 101 -5 5 102 5 water");
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));var b=Spellbooks.get(p).withStarterGiven();for(var r:Runes.all())b=b.learn(r.id());Spellbooks.set(p,b);s.overworld().setBlockAndUpdate(LOWER,Blocks.SEAGRASS.defaultBlockState());});c.waitTicks(8);
   server.runOnServer(s->paid(s.getPlayerList().getPlayers().getFirst()));c.waitTicks(8);
   server.runOnServer(s->{check(s.overworld().getBlockState(LOWER).is(Blocks.TALL_SEAGRASS)&&s.overworld().getBlockState(UPPER).is(Blocks.TALL_SEAGRASS),"Allowed paid Grow writes both actual underwater plant halves");check(s.overworld().getFluidState(LOWER).is(FluidTags.WATER)&&s.overworld().getFluidState(UPPER).is(FluidTags.WATER),"Vanilla placement preserves water in both plant cells");});
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(s.overworld(),-2.5,101,-3.5,Set.<Relative>of(),-45,12,false);});c.waitTicks(8);
   clearOverlays(c);c.takeScreenshot(TestScreenshotOptions.of("grow_seagrass_allowed").disableCounterPrefix());
   server.runCommand("fill -5 101 -5 5 102 5 water");
   server.runOnServer(s->{s.overworld().setBlockAndUpdate(LOWER,Blocks.SEAGRASS.defaultBlockState());denyUpper.set(true);paid(s.getPlayerList().getPlayers().getFirst());});c.waitTicks(8);
   server.runOnServer(s->{check(s.overworld().getBlockState(LOWER).is(Blocks.SEAGRASS),"Protected upper leaves original short seagrass intact");check(s.overworld().getBlockState(UPPER).is(Blocks.WATER),"Protected adjacent upper water is not converted to plant");check(s.overworld().getFluidState(UPPER).isFull(),"Protected upper water retains its actual full fluid state");});
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(s.overworld(),-2.5,101,-3.5,Set.<Relative>of(),-45,12,false);});c.waitTicks(8);
   clearOverlays(c);c.takeScreenshot(TestScreenshotOptions.of("grow_seagrass_protected_upper").disableCounterPrefix());
   server.runOnServer(s->{
    denyUpper.set(false);var p=s.getPlayerList().getPlayers().getFirst();var source=s.overworld().getBlockState(LOWER);int cap=dev.wildercord.config.Config.get().maxBlocks();check(cap>=2,"Real pair-budget fixture");
    var one=new Cast(p);check(one.takeBlocks(cap-1),"Leave one real cell");check(!GrowSeagrassPreflight.reserve(one,LOWER,source)&&one.takeBlock()&&!one.takeBlock(),"Refused seagrass pair preserves one-cell remainder");
    var two=new Cast(p);check(two.takeBlocks(cap-2),"Leave two real cells");check(GrowSeagrassPreflight.reserve(two,LOWER,source)&&!two.takeBlock(),"Seagrass reserves exactly both actual destinations");
    check(s.overworld().getBlockState(LOWER).equals(source)&&s.overworld().getBlockState(UPPER).is(Blocks.WATER),"Budget preflight itself preserves plant and water");
   });
  }finally{denyUpper.set(false);}
 }
 private static void paid(ServerPlayer p){p.teleportTo(p.level(),.5,101,.5,Set.<Relative>of(),0,0,false);p.setAirSupply(300);check(SpellCaster.edit(p,0,List.of(Runes.SELF.id(),Runes.GROW.id()))==null,"Real learned Grow accepted");Spellbooks.setMana(p,100);Spellbooks.setReadyAt(p,0,0);float start=Spellbooks.mana(p);SpellCaster.cast(p,0);check(Spellbooks.mana(p)<start,"Survival pays actual spell mana");}
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
 private static void clearOverlays(ClientGameTestContext c){c.runOnClient(mc->{mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});}
}
