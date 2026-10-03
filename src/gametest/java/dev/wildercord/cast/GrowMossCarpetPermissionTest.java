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
import net.minecraft.world.level.block.MossyCarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WallSide;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Real paid topper/claim regression; no fabricated feature or outcome packet. */
public final class GrowMossCarpetPermissionTest implements FabricClientGameTest {
 private static final BlockPos BASE=new BlockPos(1,101,0),UPPER=BASE.above();
 private static final AtomicBoolean denyUpper=new AtomicBoolean();
 @Override public void runTest(ClientGameTestContext c){
  PlayerBlockBreakEvents.BEFORE.register((l,p,pos,state,be)->!denyUpper.get()||!pos.equals(UPPER));
  try(var world=c.worldBuilder().create()){
   c.waitTicks(30);var server=world.getServer();server.runCommand("gamerule spawn_mobs false");
   server.runCommand("fill -8 100 -8 8 100 8 dirt");server.runCommand("fill 2 101 0 2 104 0 stone");
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));var b=Spellbooks.get(p).withStarterGiven();for(var r:Runes.all())b=b.learn(r.id());Spellbooks.set(p,b);s.overworld().setBlockAndUpdate(BASE,baseState());});c.waitTicks(8);
   server.runOnServer(s->paid(s.getPlayerList().getPlayers().getFirst()));c.waitTicks(8);
   server.runOnServer(s->{var top=s.overworld().getBlockState(UPPER);check(top.is(Blocks.PALE_MOSS_CARPET)&&!top.getValue(MossyCarpetBlock.BASE),"Allowed paid Grow creates a real wall-supported topper");check(s.overworld().getBlockState(BASE).getValue(MossyCarpetBlock.BASE),"Native base carpet remains a base");});
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(s.overworld(),-2.5,101,-3.5,Set.<Relative>of(),-45,12,false);});c.waitTicks(8);
   clearOverlays(c);c.takeScreenshot(TestScreenshotOptions.of("grow_moss_allowed").disableCounterPrefix());
   server.runOnServer(s->{s.overworld().setBlockAndUpdate(UPPER,Blocks.AIR.defaultBlockState());s.overworld().setBlockAndUpdate(BASE,baseState());denyUpper.set(true);});c.waitTicks(4);
   BlockState original=server.computeOnServer(s->s.overworld().getBlockState(BASE));
   server.runOnServer(s->paid(s.getPlayerList().getPlayers().getFirst()));c.waitTicks(8);
   server.runOnServer(s->{check(s.overworld().getBlockState(UPPER).isAir(),"Protected actual upper destination stays empty");check(s.overworld().getBlockState(BASE).equals(original),"Refused topper leaves actual base unchanged");});
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(s.overworld(),-2.5,101,-3.5,Set.<Relative>of(),-45,12,false);});c.waitTicks(8);
   clearOverlays(c);c.takeScreenshot(TestScreenshotOptions.of("grow_moss_protected_upper").disableCounterPrefix());
   server.runOnServer(s->{
    denyUpper.set(false);var p=s.getPlayerList().getPlayers().getFirst();var source=s.overworld().getBlockState(BASE);int cap=dev.wildercord.config.Config.get().maxBlocks();check(cap>=1,"Real single-budget fixture");
    var zero=new Cast(p);check(zero.takeBlocks(cap),"Exhaust real block budget");check(!GrowMossCarpetPreflight.reserve(zero,BASE,source),"No-budget topper cannot be admitted");
    var one=new Cast(p);check(one.takeBlocks(cap-1),"Leave one real destination");check(GrowMossCarpetPreflight.reserve(one,BASE,source)&&!one.takeBlock(),"Topper reserves exactly one actual destination, without charging base twice");
    check(s.overworld().getBlockState(BASE).equals(source)&&s.overworld().getBlockState(UPPER).isAir(),"Budget preflight itself preserves real base and empty upper");
   });
  }finally{denyUpper.set(false);}
 }
 private static BlockState baseState(){return Blocks.PALE_MOSS_CARPET.defaultBlockState().setValue(MossyCarpetBlock.BASE,true).setValue(MossyCarpetBlock.EAST,WallSide.LOW);}
 private static void paid(ServerPlayer p){p.teleportTo(p.level(),.5,101,.5,Set.<Relative>of(),0,0,false);check(SpellCaster.edit(p,0,List.of(Runes.SELF.id(),Runes.GROW.id()))==null,"Real learned Grow accepted");Spellbooks.setReadyAt(p,0,0);Spellbooks.setMana(p,100);float start=Spellbooks.mana(p);SpellCaster.cast(p,0);check(Spellbooks.mana(p)<start,"Survival actually pays");}
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
 private static void clearOverlays(ClientGameTestContext c){c.runOnClient(mc->{mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});}
}
