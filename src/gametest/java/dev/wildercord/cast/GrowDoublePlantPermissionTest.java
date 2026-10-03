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
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Actual paid permission gate; no Life outcome observer dependency.
 * Requires the exact two-cell production preflight.
 */
public final class GrowDoublePlantPermissionTest implements FabricClientGameTest {
 private static final AtomicBoolean denyUpper=new AtomicBoolean();
 private static final BlockPos LOWER=new BlockPos(1,101,0),UPPER=LOWER.above();
 @Override public void runTest(ClientGameTestContext c){
  PlayerBlockBreakEvents.BEFORE.register((l,p,pos,state,be)->!denyUpper.get()||!pos.equals(UPPER));
  for(BlockState source:List.of(Blocks.SHORT_GRASS.defaultBlockState(),Blocks.FERN.defaultBlockState())){
   try(var world=c.worldBuilder().create()){
    c.waitTicks(30);var server=world.getServer();server.runCommand("gamerule spawn_mobs false");
    server.runCommand("fill -8 100 -8 8 100 8 dirt");
    server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));var b=Spellbooks.get(p).withStarterGiven();for(var r:Runes.all())b=b.learn(r.id());Spellbooks.set(p,b);});c.waitTicks(8);
    server.runOnServer(s->{s.overworld().setBlockAndUpdate(LOWER,source);paid(s.getPlayerList().getPlayers().getFirst());});c.waitTicks(8);
    server.runOnServer(s->{var expected=source.is(Blocks.FERN)?Blocks.LARGE_FERN:Blocks.TALL_GRASS;check(s.overworld().getBlockState(LOWER).is(expected)&&s.overworld().getBlockState(UPPER).is(expected),"Allowed paid cast actually grows both vanilla plant halves");});
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(s.overworld(),-2.5,101,-3.5,Set.<Relative>of(),-45,12,false);});c.waitTicks(8);
   clearOverlays(c);c.takeScreenshot(TestScreenshotOptions.of("grow_"+(source.is(Blocks.FERN)?"fern":"grass")+"_allowed").disableCounterPrefix());
    server.runCommand("fill -8 101 -8 8 105 8 air");
    server.runOnServer(s->{s.overworld().setBlockAndUpdate(LOWER,source);denyUpper.set(true);paid(s.getPlayerList().getPlayers().getFirst());});c.waitTicks(8);
    server.runOnServer(s->{check(s.overworld().getBlockState(LOWER).equals(source),"Denied upper preserves original lower, without partial feature");check(s.overworld().getBlockState(UPPER).isAir(),"Allowed source cannot grow a vetoed adjacent upper cell");});
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(s.overworld(),-2.5,101,-3.5,Set.<Relative>of(),-45,12,false);});c.waitTicks(8);
   clearOverlays(c);c.takeScreenshot(TestScreenshotOptions.of("grow_"+(source.is(Blocks.FERN)?"fern":"grass")+"_protected_upper").disableCounterPrefix());
    // Additional helper-level proof against the real shared Cast budget, without invoking gameplay.
    server.runOnServer(s->{
     denyUpper.set(false);var p=s.getPlayerList().getPlayers().getFirst();int cap=dev.wildercord.config.Config.get().maxBlocks();check(cap>=2,"Native config admits a two-cell budget fixture");
     var one=new Cast(p);check(one.takeBlocks(cap-1),"Prepare real one-cell remainder");
     check(!GrowDoublePlantPreflight.reserve(one,LOWER,source),"One remaining block cannot reserve half of a double plant");
     check(one.takeBlock()&&!one.takeBlock(),"Failed pair reservation leaves that one block intact");
     var two=new Cast(p);check(two.takeBlocks(cap-2),"Prepare exact real two-cell remainder");
     check(GrowDoublePlantPreflight.reserve(two,LOWER,source)&&!two.takeBlock(),"Accepted pair consumes exactly two blocks atomically");
     check(s.overworld().getBlockState(LOWER).equals(source)&&s.overworld().getBlockState(UPPER).isAir(),"Preflight and reservation themselves never mutate the world");
    });
   }finally{denyUpper.set(false);}
  }
 }
 private static void paid(ServerPlayer p){p.teleportTo(p.level(),.5,101,.5,Set.<Relative>of(),0,0,false);check(SpellCaster.edit(p,0,List.of(Runes.SELF.id(),Runes.GROW.id()))==null,"Real learned Grow accepted");Spellbooks.setMana(p,100);Spellbooks.setReadyAt(p,0,0);float start=Spellbooks.mana(p);SpellCaster.cast(p,0);check(Spellbooks.mana(p)<start,"Survival actually pays");}
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
 private static void clearOverlays(ClientGameTestContext c){c.runOnClient(mc->{mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});}
}
