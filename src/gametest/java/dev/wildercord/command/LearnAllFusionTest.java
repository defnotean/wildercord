package dev.wildercord.command;

import dev.wildercord.client.CordScreen;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.*;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Actual command, attachment synchronization, repeat safety and complete saved-world restart. */
public final class LearnAllFusionTest implements FabricClientGameTest {
 public void runTest(ClientGameTestContext c) {
  TestWorldSave save;
  var spell=List.of(Runes.BOLT.id(),Runes.FIRESTORM.id());
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(35);w.getServer().runCommand("gamerule spawn_mobs false");
   w.getServer().runOnServer(s -> {var p=s.getPlayerList().getPlayers().getFirst();
    Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));
    Spellbooks.set(p,Spellbook.EMPTY.learn("unloaded:old_rune").withSpell(0,spell).withName(0,"Old Flame"));
    p.setAttached(WildercordAttachments.GRIMOIRE,List.of("hint:existing"));
    p.setAttached(WildercordAttachments.CONDENSED,123);p.setAttached(WildercordAttachments.INNATE,Runes.KINDLING.id());
   });
   w.getServer().runCommand("execute as @p run wildercord learnall");c.waitTicks(8);
   w.getServer().runOnServer(s -> verify(s.getPlayerList().getPlayers().getFirst(),spell));
   c.runOnClient(mc -> {verify(mc.player,spell);mc.getWindow().setWindowed(1280,720);mc.resizeGui();mc.gui.toastManager().clear();});
   c.takeScreenshot(TestScreenshotOptions.of("learnall_fusion_command").disableCounterPrefix());
   w.getServer().runCommand("execute as @p run wildercord learnall");c.waitTicks(4);
   w.getServer().runOnServer(s -> verify(s.getPlayerList().getPlayers().getFirst(),spell));
   c.runOnClient(mc -> {var screen=new CordScreen();mc.gui.setScreen(screen);screen.showPage(2);});c.waitTicks(3);
   c.takeScreenshot(TestScreenshotOptions.of("learnall_fusion_grimoire").disableCounterPrefix());
   c.runOnClient(mc -> mc.gui.setScreen(null));save=w.getWorldSave();
  }
  try(var w=save.open()) {c.waitTicks(30);w.getServer().runOnServer(s -> verify(s.getPlayerList().getPlayers().getFirst(),spell));c.runOnClient(mc -> verify(mc.player,spell));}
 }
 private static void verify(net.minecraft.world.entity.player.Player p,List<String> spell) {
  var book=Spellbooks.get(p);var found=Heart.grimoire(p);
  check(Runes.all().stream().allMatch(r -> book.knows(r.id())),"Every registered rune learned, including named fusion results");
  check(Fusions.RECIPES.stream().allMatch(r -> book.knows(r.result().id()) && found.contains(r.key())),"Every elemental fusion learned and revealed");
  check(Fusions.SIGNATURES.stream().allMatch(r -> book.knows(r.result().id()) && found.contains(r.key())),"Every signature fusion learned and revealed");
  check(found.size()==1+Fusions.RECIPES.size()+Fusions.SIGNATURES.size(),"No duplicate discoveries");
  check(book.knows("unloaded:old_rune") && found.contains("hint:existing"),"Existing missing-addon knowledge and discoveries retained");
  check(book.spells().getFirst().equals(spell) && book.names().getFirst().equals("Old Flame"),"Saved spells retained");
  check(Heart.condensed(p)==123 && Heart.innate(p).equals(Runes.KINDLING.id()),"Admin discovery grants no rewards or new innate ownership");
 }
 private static void check(boolean yes,String why) {if(!yes)throw new AssertionError(why);}
}
