package dev.wildercord.aura;

import dev.wildercord.client.AuraScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import java.util.Set;

/** Practice exhaustion must not prevent real combat or a valid breakthrough, even after restart. */
public final class AuraProgressionTest implements FabricClientGameTest {
 public void runTest(ClientGameTestContext c) {
  TestWorldSave save;
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(35);w.getServer().runCommand("gamerule spawn_mobs false");
   w.getServer().runCommand("difficulty normal");w.getServer().runCommand("weather clear");
   w.getServer().runCommand("fill -6 159 -6 6 159 6 stone");
   w.getServer().runCommand("fill -1 160 -1 1 160 1 stone");
   w.getServer().runCommand("fill 2 167 -2 6 167 2 mossy_cobblestone");
   w.getServer().runCommand("setblock 2 167 0 water");
   w.getServer().runOnServer(s -> {var p=s.getPlayerList().getPlayers().getFirst();
    p.teleportTo(p.level(),.5,161,.5,Set.<Relative>of(),0,0,false);
    p.setGameMode(GameType.SURVIVAL);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));
    check(AuraMethods.learn(p,BreathingMethods.byId("stone").orElseThrow(),"test",true),"A manual's learning path opens Glow");
    Aura.set(p,Aura.data(p).withXp(39.9,39.9));
    check(Math.abs(AuraExperience.earn(p,2,true)-.1)<.00001,"Practice stops exactly at40");
    check(AuraExperience.earn(p,20,true)==0,"Exhausted practice cannot farm XP");
    check(Aura.data(p).xp()==40 && Aura.stage(p)==AuraRules.GLOW,"40practice is below Flow150");
   });
   c.runOnClient(mc -> {mc.getWindow().setWindowed(1280,720);mc.options.guiScale().set(2);mc.resizeGui();mc.gui.toastManager().clear();
    for(int next=AuraRules.FLOW;next<=AuraRules.SOVEREIGN;next++)for(String trial:dev.wildercord.api.AuraApi.trials(next))
     check(net.minecraft.locale.Language.getInstance().has("screen.wildercord.aura.trial."+trial.replace(':','.')),"Every registered breakthrough trial has player-facing instructions: "+trial);
   });
   c.waitTicks(35);
   w.getServer().runOnServer(s -> {var p=s.getPlayerList().getPlayers().getFirst();
    var husk=EntityTypes.HUSK.create(p.level(),EntitySpawnReason.COMMAND);husk.setNoAi(true);husk.snapTo(.5,161,2.5,180,0);p.level().addFreshEntity(husk);
    p.attack(husk);check(Aura.data(p).xp()>40,"A real recovered sword attack still earns after practice exhaustion");husk.discard();
   });
   c.waitTicks(4);c.runOnClient(mc -> mc.gui.setScreen(new AuraScreen(null)));c.waitTicks(3);
   c.takeScreenshot(TestScreenshotOptions.of("aura_progression_practice_complete").disableCounterPrefix());
   c.runOnClient(mc -> mc.gui.setScreen(null));
   w.getServer().runOnServer(s -> {var p=s.getPlayerList().getPlayers().getFirst();
    AuraExperience.grant(p,999);check(Aura.data(p).xp()==150 && Aura.stage(p)==AuraRules.GLOW,"Full XP waits for the trial");
    check(!AuraBreakthroughs.complete(p,AuraBreakthroughs.GUARDIAN),"A wrong stage trial cannot advance Glow");
   });
   c.waitTicks(6);c.runOnClient(mc -> mc.gui.setScreen(new AuraScreen(null)));c.waitTicks(3);
   c.takeScreenshot(TestScreenshotOptions.of("aura_progression_trial_required").disableCounterPrefix());
   c.runOnClient(mc -> {mc.gui.setScreen(null);mc.options.toggleCrouch().set(false);mc.options.keyShift.setDown(true);});
   c.waitTicks(AuraRules.SETTLE_TICKS+AuraRules.STILLNESS_TICKS+30);
   w.getServer().runOnServer(s -> {var p=s.getPlayerList().getPlayers().getFirst();
    check(Aura.stage(p)==AuraRules.FLOW,"A real uninterrupted waterfall stance advances to Flow");
    check(Aura.data(p).practice()==40,"A breakthrough does not reset the lifetime practice cap");
   });
   c.runOnClient(mc -> mc.options.keyShift.setDown(false));c.waitTicks(15);save=w.getWorldSave();
  }
  try(var w=save.open()) {
   c.waitTicks(35);w.getServer().runOnServer(s -> {var p=s.getPlayerList().getPlayers().getFirst();
    check(Aura.stage(p)==AuraRules.FLOW && Aura.data(p).practice()==40,"Earned rank and practice cap survive restart");
    check(AuraExperience.earn(p,2,false)>0,"Real XP keeps growing toward Edge after restart");
    check(!AuraBreakthroughs.complete(p,AuraBreakthroughs.STILLNESS),"Trials cannot skip missing XP");
    AuraExperience.grant(p,999);check(AuraBreakthroughs.complete(p,AuraBreakthroughs.STILLNESS),"Early-stage trial allows Edge after600XP");
    AuraExperience.grant(p,99999);check(!AuraBreakthroughs.complete(p,AuraBreakthroughs.STILLNESS),"Ordinary stillness cannot skip Form's higher trial");
    check(AuraBreakthroughs.complete(p,AuraBreakthroughs.GUARDIAN),"Guardian trial opens Form at1800XP");
    AuraExperience.grant(p,99999);check(AuraBreakthroughs.complete(p,AuraBreakthroughs.GUARDIAN),"Guardian trial opens Sovereign at4500XP");
   });
  } finally {c.runOnClient(mc -> mc.options.keyShift.setDown(false));}
 }
 private static void check(boolean yes,String why) {if(!yes)throw new AssertionError(why);}
}
