package dev.wildercord.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.client.CordScreen;
import dev.wildercord.client.MagicSettingsScreen;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Runes;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** Native attachment/render/input acceptance; initial rune knowledge is a server fixture. */
public final class LifeJournalTest implements FabricClientGameTest {
 private static final int LEFT=InputConstants.MOUSE_BUTTON_LEFT;
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 private static ServerPlayer player(MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
 private static CordScreen cord(ClientGameTestContext c){return c.computeOnClient(mc -> mc.gui.screen() instanceof CordScreen s?s:null);}
 private static void click(ClientGameTestContext c,double[] point){
  check(point!=null,"The actual link must be fully visible before clicking");
  double scale=c.computeOnClient(mc -> mc.getWindow().getGuiScale());
  c.getInput().setCursorPos(point[0]*scale,point[1]*scale);c.waitTicks(1);
  try{c.getInput().pressMouse(LEFT);c.waitTicks(3);}finally{c.getInput().releaseMouse(LEFT);}
  c.waitTicks(2);
 }
 private static void shot(ClientGameTestContext c,String name){c.runOnClient(mc -> {mc.gui.toastManager().clear();mc.gui.hud.getChat().clearMessages(false);});c.waitTicks(3);c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());}
 private static void size(ClientGameTestContext c,int w,int h){c.runOnClient(mc -> {mc.getWindow().setWindowed(w,h);mc.options.guiScale().set(2);mc.resizeGui();});c.waitTicks(5);}
 private static void settingsLayout(ClientGameTestContext c){
  check(c.computeOnClient(mc -> {
   if(!(mc.gui.screen() instanceof MagicSettingsScreen s))return false;
   boolean own=false,other=false,done=false;
   for(var child:s.children())if(child instanceof Button b){
    if(b.getX()<0||b.getY()<0||b.getX()+b.getWidth()>s.width||b.getY()+b.getHeight()>s.height)return false;
    String label=b.getMessage().getString();own|=label.startsWith("Your spells: ");other|=label.startsWith("Other spells: ");done|=label.equals(Component.translatable("gui.done").getString());
   }
   var buttons=s.children().stream().filter(Button.class::isInstance).map(Button.class::cast).toList();
   if(buttons.size()!=18)return false;
   for(int i=0;i<buttons.size();i++)for(int j=i+1;j<buttons.size();j++){
    Button a=buttons.get(i),b=buttons.get(j);
    if(a.getX()<b.getX()+b.getWidth()&&b.getX()<a.getX()+a.getWidth()&&a.getY()<b.getY()+b.getHeight()&&b.getY()<a.getY()+a.getHeight())return false;
   }
   return own&&other&&done;
  }),"Actual own/other controls and every settings button fit the native viewport");
 }
 @Override public void runTest(ClientGameTestContext c){
  int[] original=c.computeOnClient(mc -> new int[]{mc.getWindow().getWidth(),mc.getWindow().getHeight(),mc.options.guiScale().get()});
  try(TestSingleplayerContext world=c.worldBuilder().create()){
   c.waitTicks(40);
   world.getServer().runOnServer(s -> {
    var p=player(s);p.setGameMode(GameType.SURVIVAL);
    Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));
    Spellbooks.set(p,Spellbook.EMPTY.withStarterGiven().learn(Runes.SELF.id()).learn(Runes.HEAL.id()).learn(Runes.RESTORE.id()).withSpell(0,List.of(Runes.SELF.id(),Runes.HEAL.id())));
   });
   c.waitFor(mc -> mc.player!=null&&Spellbooks.get(mc.player).knows(Runes.HEAL.id())&&Spellbooks.get(mc.player).knows(Runes.RESTORE.id())&&!Spellbooks.get(mc.player).knows(Runes.REMEDY.id()));
   List<List<String>> saved=world.getServer().computeOnServer(s -> Spellbooks.get(player(s)).spells().stream().map(List::copyOf).toList());
   size(c,1920,1080);c.setScreen(CordScreen::new);c.waitTicks(3);
   click(c,c.computeOnClient(mc -> ((CordScreen)mc.gui.screen()).pagePoint(2)));
   check(c.computeOnClient(mc -> ((CordScreen)mc.gui.screen()).lifeJournalKnown().equals(List.of(Runes.HEAL.id(),Runes.RESTORE.id()))),"Only the two actually synced known Life runes render named notes");
   check(c.computeOnClient(mc -> ((CordScreen)mc.gui.screen()).lifeJournalPoint(Runes.REMEDY.id())==null),"An unknown rune has no visible link");
   check(c.computeOnClient(mc -> ((CordScreen)mc.gui.screen()).lifeJournalPoint("wildercord:ashen_mercy")==null&&!Spellbooks.get(mc.player).knows("wildercord:ashen_mercy")),"An undiscovered secret fusion does not leak a named note or clickable link");
   double[] heal=c.computeOnClient(mc -> ((CordScreen)mc.gui.screen()).lifeJournalPoint(Runes.HEAL.id()));
   check(heal!=null,"Known Heal note is on screen");
   double scale=c.computeOnClient(mc -> mc.getWindow().getGuiScale());c.getInput().setCursorPos(heal[0]*scale,heal[1]*scale);
   shot(c,"life_journal_learned_only_1920");click(c,heal);
   check(c.computeOnClient(mc -> ((CordScreen)mc.gui.screen()).codexPoint(Runes.HEAL.id())!=null),"Actual journal click navigates to the known Heal Codex entry");
   check(c.computeOnClient(mc -> {
    String cost=Component.translatable("screen.wildercord.cost",12,"0.6",300).getString();
    String shape=Component.translatable("screen.wildercord.rune_cost_shape",1,"2").getString();
    String multiplier=Component.translatable("screen.wildercord.rune_cost_mult","2").getString();
    return cost.contains("\u00b7")&&!cost.contains("\u00c2")&&shape.contains("\u00d7")&&!shape.contains("\u00c3")&&multiplier.contains("\u00d7")&&!multiplier.contains("\u00c3");
   }),"Real loaded Codex cost and multiplier translations contain intended punctuation without mojibake");
   shot(c,"life_journal_heal_codex_1920");
   check(saved.equals(world.getServer().computeOnServer(s -> Spellbooks.get(player(s)).spells().stream().map(List::copyOf).toList())),"Journal navigation leaves server saved spells unchanged");
   click(c,c.computeOnClient(mc -> ((CordScreen)mc.gui.screen()).pagePoint(2)));
   c.getInput().scroll(-1);c.waitTicks(4);
   CordScreen parent=cord(c);int scroll=c.computeOnClient(mc -> parent.lifeJournalScroll());check(scroll>0,"The actual input event scrolls the journal");
   click(c,c.computeOnClient(mc -> parent.lifeJournalPoint("settings")));c.waitForScreen(MagicSettingsScreen.class);settingsLayout(c);shot(c,"life_journal_magic_visuals_1920");
   c.clickScreenButton(c.computeOnClient(mc -> Component.translatable("gui.done").getString()));c.waitTicks(4);
   check(cord(c)==parent&&c.computeOnClient(mc -> parent.lifeJournalScroll())==scroll,"Done restores the same journal and its actual scroll");
   c.getInput().setCursorPos(0,0);
   shot(c,"life_journal_done_preserves_scroll_1920");
   click(c,c.computeOnClient(mc -> parent.lifeJournalPoint("settings")));c.waitForScreen(MagicSettingsScreen.class);
   try{c.getInput().pressKey(InputConstants.KEY_ESCAPE);c.waitTicks(3);}finally{c.getInput().releaseKey(InputConstants.KEY_ESCAPE);}
   c.waitTicks(3);check(cord(c)==parent&&c.computeOnClient(mc -> parent.lifeJournalScroll())==scroll,"Escape restores the same journal and scroll");
   world.getServer().runOnServer(s -> Spellbooks.learn(player(s),Runes.REMEDY.id()));
   c.waitFor(mc -> Spellbooks.get(mc.player).knows(Runes.REMEDY.id()));c.waitTicks(4);
   check(c.computeOnClient(mc -> parent.lifeJournalKnown().equals(List.of(Runes.HEAL.id(),Runes.RESTORE.id(),Runes.REMEDY.id())))&&c.computeOnClient(mc -> parent.lifeJournalPoint(Runes.REMEDY.id())!=null),"A real server learning event updates the already open native notes");
   size(c,1280,720);c.getInput().setCursorPos(0,0);shot(c,"life_journal_learned_refresh_1280");
   click(c,c.computeOnClient(mc -> parent.lifeJournalPoint("settings")));c.waitForScreen(MagicSettingsScreen.class);settingsLayout(c);shot(c,"life_journal_magic_visuals_1280");
   c.clickScreenButton(c.computeOnClient(mc -> Component.translatable("gui.done").getString()));c.waitTicks(3);
   check(cord(c)==parent&&c.computeOnClient(mc -> parent.lifeJournalScroll())==scroll,"Resized settings retain the same parent journal state");
   for(int[] physical:new int[][]{{854,480},{640,480}}){
    size(c,physical[0],physical[1]);
    check(c.computeOnClient(mc -> mc.getWindow().getGuiScaledWidth()==physical[0]/2&&mc.getWindow().getGuiScaledHeight()==physical[1]/2),"The requested physical window really yields the supported small GUI at scale2");
    c.getInput().setCursorPos(0,0);
    shot(c,"life_journal_small_"+physical[0]);
    click(c,c.computeOnClient(mc -> parent.lifeJournalPoint("settings")));c.waitForScreen(MagicSettingsScreen.class);settingsLayout(c);
    shot(c,"life_journal_magic_visuals_"+physical[0]);
    c.clickScreenButton(c.computeOnClient(mc -> Component.translatable("gui.done").getString()));c.waitTicks(3);
    check(cord(c)==parent&&c.computeOnClient(mc -> parent.lifeJournalScroll())==scroll,"Small-GUI Done preserves the identical journal and scroll");
   }
   check(saved.equals(world.getServer().computeOnServer(s -> Spellbooks.get(player(s)).spells().stream().map(List::copyOf).toList())),"Settings and learning do not mutate the saved spell rows");
  }finally{
   c.getInput().releaseMouse(LEFT);c.getInput().releaseKey(InputConstants.KEY_ESCAPE);
   c.runOnClient(mc -> {mc.gui.setScreen(null);mc.getWindow().setWindowed(original[0],original[1]);mc.options.guiScale().set(original[2]);mc.resizeGui();});
  }
 }
}
