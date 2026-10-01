package dev.wildercord.player;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import java.util.*;

public final class ResearchLibraryTest implements FabricClientGameTest {
 public void runTest(ClientGameTestContext context) {
  try(var world=context.worldBuilder().create()) {
   context.waitTicks(35);
   world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);
    var spell=List.of(Runes.BOLT.id(),Runes.FIRE.id());
    var book=Spellbook.EMPTY;for(var r:Runes.all())book=book.learn(r.id());book=book.withSpell(0,spell);Spellbooks.set(p,book);
    Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));
    check(SpellLibrary.save(p,"Warm breeze",0)==1,"save build");Spellbooks.set(p,Spellbooks.get(p).withSpell(0,List.of(Runes.SELF.id(),Runes.HEAL.id())));
    check(SpellLibrary.load(p,"Warm breeze",0)==1&&Spellbooks.get(p).spells().getFirst().equals(spell),"atomic restore");
    check(SpellLibrary.save(p,"warm breeze",0)==1&&SpellLibrary.list(p).size()==1,"case insensitive overwrite");
    for(int i=0;i<23;i++)check(SpellLibrary.save(p,"Build "+i,0)==1,"fill library");
    check(SpellLibrary.save(p,"Overflow",0)==0&&SpellLibrary.list(p).size()==24,"bounded library");
    p.setAttached(SpellLibrary.BUILDS,List.of(new SpellLibrary.Build("Missing",List.of("addon:missing")),new SpellLibrary.Build("Too big",Collections.nCopies(4,Runes.FIRE.id()))));
    var before=Spellbooks.get(p);check(SpellLibrary.load(p,"Missing",0)==0&&Spellbooks.get(p).equals(before),"unlearned rune leaves slot and name unchanged");
    Spellbooks.setCord(p,new ItemStack(WildercordItems.TWINE_CORD));
    check(SpellLibrary.load(p,"Too big",0)==0&&Spellbooks.get(p).equals(before),"socket limit leaves slot unchanged");
    check(SpellLibrary.load(p,"Too big",4)==0&&SpellLibrary.save(p,"Invalid",99)==0,"locked and invalid slots");
    p.getInventory().clearContent();
    var shapes=Runes.all().stream().filter(r->r.family()==RuneFamily.SHAPE).limit(5).toList();RuneResearch.cast(p,shapes);
    var fusions=Fusions.RECIPES.stream().limit(3).map(Fusions.Recipe::result).toList();RuneResearch.cast(p,fusions);RuneResearch.garden(p);
    var notes=p.getAttachedOrElse(RuneResearch.NOTES,RuneResearch.Notebook.EMPTY);
    check(notes.rewarded()==7&&notes.shapes().size()==5&&notes.fusions().size()==3,"three experiments recorded");
    int count=p.getInventory().countItem(WildercordItems.TORN_PAGE)+p.getInventory().countItem(WildercordItems.BLANK_RUNE)+p.getInventory().countItem(WildercordItems.MANA_CRYSTAL);
    RuneResearch.cast(p,shapes);RuneResearch.cast(p,fusions);RuneResearch.garden(p);
    check(count==4&&count==p.getInventory().countItem(WildercordItems.TORN_PAGE)+p.getInventory().countItem(WildercordItems.BLANK_RUNE)+p.getInventory().countItem(WildercordItems.MANA_CRYSTAL),"rewards cannot be farmed");
    var json=RuneResearch.Notebook.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,notes).getOrThrow();
    check(RuneResearch.Notebook.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,json).getOrThrow().equals(notes),"persistent research codec round trip");
   });
   world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();
    p.setAttached(SpellLibrary.BUILDS,List.of());Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));
    var at=p.blockPosition().offset(1,0,0);p.level().setBlockAndUpdate(at,dev.wildercord.content.WildercordBlocks.RUNIC_HEARTH.defaultBlockState());
    ((dev.wildercord.content.RunicHearthEntity)p.level().getBlockEntity(at)).use(p,new ItemStack(net.minecraft.world.item.Items.BOOK));
   });context.waitTicks(5);
   check(context.computeOnClient(mc->mc.gui.screen() instanceof dev.wildercord.client.RuneNotebookScreen),"hearth book opens the native notebook through its server payload");
   context.runOnClient(mc->{mc.getWindow().setWindowed(1600,900);mc.options.guiScale().set(2);mc.resizeGui();
    var screen=mc.gui.screen();screen.children().stream().filter(e->e instanceof net.minecraft.client.gui.components.EditBox).map(e->(net.minecraft.client.gui.components.EditBox)e).findFirst().orElseThrow().setValue("Warm breeze");});
   context.waitTicks(8);context.runOnClient(mc->mc.gui.toastManager().clear());context.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of("research_library_before").disableCounterPrefix());
   click(context,"screen.wildercord.notebook.save");context.waitTicks(15);
   world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();check(SpellLibrary.list(p).size()==1,"native save button reaches the authoritative library");Spellbooks.set(p,Spellbooks.get(p).withSpell(0,List.of(Runes.SELF.id(),Runes.HEAL.id())));});
   click(context,"screen.wildercord.notebook.load");context.waitTicks(5);
   world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();check(Spellbooks.get(p).spells().getFirst().equals(List.of(Runes.BOLT.id(),Runes.FIRE.id())),"native load button restores the saved build");});
   context.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of("research_library").disableCounterPrefix());
   context.runOnClient(mc->{mc.getWindow().setWindowed(854,480);mc.resizeGui();});context.waitTicks(5);
   context.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions.of("research_library_854x480").disableCounterPrefix());
   click(context,"screen.wildercord.notebook.delete");context.waitTicks(5);
   world.getServer().runOnServer(server->check(SpellLibrary.list(server.getPlayerList().getPlayers().getFirst()).isEmpty(),"native delete button removes just its named build"));
   context.setScreen(()->null);
  }
 }
 private static void check(boolean v,String label){if(!v)throw new AssertionError(label);}
 private static void click(ClientGameTestContext c,String key){double[] point=c.computeOnClient(mc->{var b=mc.gui.screen().children().stream().filter(e->e instanceof net.minecraft.client.gui.components.Button).map(e->(net.minecraft.client.gui.components.Button)e).filter(e->e.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable(key).getString())).findFirst().orElseThrow();double scale=mc.getWindow().getGuiScale();return new double[]{(b.getX()+b.getWidth()/2.0)*scale,(b.getY()+10)*scale};});c.getInput().setCursorPos(point[0],point[1]);c.waitTicks(3);c.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);c.waitTicks(5);c.getInput().releaseMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);}
}
