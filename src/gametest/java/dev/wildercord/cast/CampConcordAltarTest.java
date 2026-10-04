package dev.wildercord.cast;

import static dev.wildercord.cast.CampConcordNative.*;
import dev.wildercord.config.Config;
import dev.wildercord.content.*;
import dev.wildercord.menu.FusionAltarMenu;
import dev.wildercord.player.*;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Supplied altar ingredients/progression; actual Survival Fuse, pickup, learn and paid woven delivery packets. */
public final class CampConcordAltarTest implements FabricClientGameTest {
 private record Pair(RuneDef a,RuneDef b,boolean exact){}
 private RuneDef watchWeave,manaWeave;
 public void runTest(ClientGameTestContext c){try(var w=c.worldBuilder().create()){
  c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule natural_health_regeneration false");
  var original=w.getServer().computeOnServer(server->Config.get());
  try{
   w.getServer().runOnServer(server->{config(copy(original,Map.of("manaRegenMultiplier",0.0)));arena(server);var player=player(server);Spellbooks.set(player,Spellbooks.get(player).learn(Runes.SELF.id()));player.setExperienceLevels(100);var at=new BlockPos(0,101,2);server.overworld().setBlockAndUpdate(at,WildercordBlocks.FUSION_ALTAR.defaultBlockState());player.openMenu(server.overworld().getBlockState(at).getMenuProvider(server.overworld(),at));});
   c.waitFor(mc->mc.player.containerMenu instanceof FusionAltarMenu,60);
   // Innate Stoneform is deliberately unsupported by shard fusion; exercise the actual refusal before supported routes.
   int refusedXp=w.getServer().computeOnServer(server->player(server).experienceLevel);
   w.getServer().runOnServer(server->{var menu=(FusionAltarMenu)player(server).containerMenu;menu.getSlot(0).set(RuneItem.stack(Runes.WATCHWEFT));menu.getSlot(1).set(RuneItem.stack(Runes.STONEFORM));menu.getSlot(FusionAltarMenu.CATALYST).set(new ItemStack(Items.AMETHYST_SHARD));menu.broadcastChanges();});
   c.waitFor(mc->mc.player.containerMenu.getSlot(FusionAltarMenu.CATALYST).getItem().is(Items.AMETHYST_SHARD),60);
   c.runOnClient(mc->mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId,FusionAltarMenu.BUTTON_FUSE));c.waitTicks(4);
   w.getServer().runOnServer(server->{var p=player(server);var menu=(FusionAltarMenu)p.containerMenu;check(p.experienceLevel==refusedXp&&menu.getSlot(FusionAltarMenu.RESULT).getItem().isEmpty()&&RuneItem.runeOf(menu.getSlot(0).getItem()).map(r->r==Runes.WATCHWEFT).orElse(false)&&RuneItem.runeOf(menu.getSlot(1).getItem()).map(r->r==Runes.STONEFORM).orElse(false)&&menu.getSlot(FusionAltarMenu.CATALYST).getItem().getCount()==1,"Actual unsupported innate shard fusion refuses without consuming XP or ingredients");});
   var pairs=List.of(new Pair(Runes.WATCHWEFT,Runes.EMBER,false),new Pair(Runes.WATCHWEFT,Runes.TREMOR,false),new Pair(Runes.MANABRAID,Runes.HEAL,false),new Pair(Runes.MANABRAID,Runes.JOLT,false),new Pair(Runes.WATCHWEFT,Runes.WATCHWEFT,true),new Pair(Runes.MANABRAID,Runes.HEAL,true));
   for(var pair:pairs){
    RuneDef expected=pair.exact?WovenRunes.bind(pair.a,pair.b):Fusions.recipe(pair.a,pair.b).orElseThrow().result();int levels=pair.exact?Fusions.COMBINE_XP*(WovenRunes.contents(expected).size()-1):Fusions.COMBINE_XP;
    int xp=w.getServer().computeOnServer(server->player(server).experienceLevel);
    w.getServer().runOnServer(server->{var player=player(server);var menu=(FusionAltarMenu)player.containerMenu;check(menu.getSlot(FusionAltarMenu.RESULT).getItem().isEmpty(),"Previous genuine result was picked up");menu.getSlot(0).set(RuneItem.stack(pair.a));menu.getSlot(1).set(RuneItem.stack(pair.b));menu.getSlot(2).set(ItemStack.EMPTY);menu.getSlot(FusionAltarMenu.CATALYST).set(new ItemStack(pair.exact?Items.AMETHYST_BLOCK:Items.AMETHYST_SHARD));menu.broadcastChanges();});
    c.waitFor(mc->RuneItem.runeOf(mc.player.containerMenu.getSlot(0).getItem()).map(r->r.id().equals(pair.a.id())).orElse(false)&&RuneItem.runeOf(mc.player.containerMenu.getSlot(1).getItem()).map(r->r.id().equals(pair.b.id())).orElse(false)&&mc.player.containerMenu.getSlot(FusionAltarMenu.CATALYST).getItem().is(pair.exact?Items.AMETHYST_BLOCK:Items.AMETHYST_SHARD),60);
    c.runOnClient(mc->{mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});
    c.takeScreenshot(TestScreenshotOptions.of("camp_altar_"+pair.a.path()+"_"+pair.b.path()+"_"+(pair.exact?"weave":"element")).withSize(1280,720).disableCounterPrefix());
    c.runOnClient(mc->mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId,FusionAltarMenu.BUTTON_FUSE));c.waitTicks(4);
    w.getServer().runOnServer(server->{var player=player(server);var menu=(FusionAltarMenu)player.containerMenu;check(RuneItem.runeOf(menu.getSlot(FusionAltarMenu.RESULT).getItem()).map(r->r.id().equals(expected.id())).orElse(false),"Real client Fuse creates expected elemental or exact weave identity");check(player.experienceLevel==xp-levels,"Actual Survival fusion pays exact XP once");check(menu.getSlot(0).getItem().isEmpty()&&menu.getSlot(1).getItem().isEmpty()&&menu.getSlot(FusionAltarMenu.CATALYST).getItem().isEmpty(),"Actual Fuse consumes both supplied runes and catalyst exactly once");});
    c.waitFor(mc->RuneItem.runeOf(mc.player.containerMenu.getSlot(FusionAltarMenu.RESULT).getItem()).map(r->r.id().equals(expected.id())).orElse(false),60);
    c.runOnClient(mc->mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId,FusionAltarMenu.RESULT,0,ContainerInput.QUICK_MOVE,mc.player));c.waitTicks(4);
    w.getServer().runOnServer(server->{var player=player(server);var menu=(FusionAltarMenu)player.containerMenu;check(menu.getSlot(FusionAltarMenu.RESULT).getItem().isEmpty(),"Actual client pickup removes genuine altar output");ItemStack earned=null;for(int i=0;i<36;i++){var item=player.getInventory().getItem(i);if(RuneItem.runeOf(item).map(r->r.id().equals(expected.id())).orElse(false)){check(earned==null&&item.getCount()==1,"One actual result, no output duplication");earned=item;player.getInventory().setItem(i,ItemStack.EMPTY);}}check(earned!=null&&!Spellbooks.knows(player,expected.id()),"Actual output is newly earned and not supplied as learned");player.setItemInHand(InteractionHand.MAIN_HAND,earned);player.closeContainer();player.inventoryMenu.broadcastChanges();});
    c.waitFor(mc->RuneItem.runeOf(mc.player.getMainHandItem()).map(r->r.id().equals(expected.id())).orElse(false),60);
    c.runOnClient(mc->{mc.gui.setScreen(null);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);});c.waitTicks(4);
    w.getServer().runOnServer(server->check(Spellbooks.knows(player(server),expected.id())&&player(server).getMainHandItem().isEmpty(),"Actual Survival Rune use learns genuine fused output and consumes it"));
    if(pair.exact){if(pair.a==Runes.WATCHWEFT)watchWeave=expected;else manaWeave=expected;}
    w.getServer().runOnServer(server->{var at=new BlockPos(0,101,2);player(server).openMenu(server.overworld().getBlockState(at).getMenuProvider(server.overworld(),at));});c.waitFor(mc->mc.player.containerMenu instanceof FusionAltarMenu,60);
   }
   w.getServer().runOnServer(server->{player(server).closeContainer();server.overworld().setBlockAndUpdate(new BlockPos(0,101,2),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());aim(player(server),new net.minecraft.world.phys.Vec3(.5,100.999,.5));});c.waitTicks(4);
   int watchPrice=w.getServer().computeOnServer(server->{var p=player(server);check(WovenRunes.contents(watchWeave).size()==2&&WovenRunes.contents(watchWeave).stream().allMatch(r->r==Runes.WATCHWEFT),"Actual learned weave retains both duplicate constituents");Spellbooks.setMana(p,80);return edit(p,List.of(Runes.TOUCH,watchWeave,Runes.ECHO));});
   cast(c);for(int n=0;n<90&&!w.getServer().computeOnServer(server->Watchweft.active(player(server).getUUID()));n++)c.waitTicks(1);long rest=w.getServer().computeOnServer(server->{var p=player(server);check(Watchweft.active(p.getUUID())&&Watchweft.active()==1&&Spellbooks.mana(p)==80-watchPrice,"Actual paid duplicate weave creates one watch after one normal price");return p.getAttachedOrElse(Watchweft.READY,0L);});c.waitTicks(22);
   w.getServer().runOnServer(server->check(Watchweft.active()==1&&player(server).getAttachedOrElse(Watchweft.READY,0L)==rest&&Spellbooks.mana(player(server))==80-watchPrice,"Actual delayed Echo and duplicated woven node cannot buy a second watch or renew rest"));
   for(int n=0;n<400&&w.getServer().computeOnServer(server->server.overworld().getGameTime()<Spellbooks.readyAt(player(server),0));n++)c.waitTicks(1);
   w.getServer().runOnServer(server->check(server.overworld().getGameTime()>=Spellbooks.readyAt(player(server),0),"Original first cast cooldown expires naturally before the next paid weave"));
   AtomicInteger transfers=new AtomicInteger();try(var observation=Manabraid.observe(e->transfers.incrementAndGet())){
    int price=w.getServer().computeOnServer(server->{var p=player(server);p.setHealth(8);Spellbooks.setMana(p,80);return edit(p,List.of(Runes.SELF,manaWeave,Runes.ECHO));});cast(c);c.waitTicks(24);
    w.getServer().runOnServer(server->check(player(server).getHealth()>8&&Spellbooks.mana(player(server))==80-price&&transfers.get()==0&&Manabraid.active()==0,"Actual paid mixed helpful weave heals through its original constituent but refuses self mana gifting and Echo duplication"));
   }catch(Exception e){throw new AssertionError(e);}
  }finally{w.getServer().runOnServer(server->config(original));}
 }}
}
