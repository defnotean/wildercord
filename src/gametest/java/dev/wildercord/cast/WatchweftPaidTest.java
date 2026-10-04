package dev.wildercord.cast;
import dev.wildercord.config.*;
import dev.wildercord.player.*;
import dev.wildercord.spell.*;
import dev.wildercord.client.fx.CampConcordClient;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.monster.zombie.Zombie;
import java.util.*;
/** Actual paid Echo arm, vanilla hostile walking/crossing, spent watch and full saved-rest reopen. */
public final class WatchweftPaidTest implements FabricClientGameTest {
 private long rest;private int price;private Zombie approach;
 public void runTest(ClientGameTestContext c){TestWorldSave save;
  dev.wildercord.api.WildercordEvents.SPELL_HIT.register((who,entities,point,runes)->{if(runes.stream().anyMatch(r->r.path().equals("watchweft")))System.out.println("WATCHWEFT_ACTUAL_HIT point="+point+" entities="+entities+" runes="+runes);});
  try(var w=c.worldBuilder().create()){
   c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set midnight");var original=w.getServer().computeOnServer(s->Config.get());long warnings=c.computeOnClient(mc->CampConcordClient.accepted(CampConcordFx.WARN));
   try{w.getServer().runOnServer(s->{CampConcordNative.config(CampConcordNative.copy(original,Map.of("manaRegenMultiplier",0.0,"spellsEditBlocks",false)));CampConcordNative.arena(s);var p=CampConcordNative.player(s);price=CampConcordNative.edit(p,List.of(Runes.TOUCH,Runes.get("wildercord:watchweft").orElseThrow(),Runes.ECHO));Spellbooks.setMana(p,80);});c.waitTicks(4);CampConcordNative.cast(c);
    for(int n=0;n<90&&!w.getServer().computeOnServer(s->Watchweft.active(CampConcordNative.player(s).getUUID()));n++)c.waitTicks(1);
    w.getServer().runOnServer(s->{var p=CampConcordNative.player(s);System.out.println("WATCHWEFT_PAID_ARM active="+Watchweft.active(p.getUUID())+" mana="+Spellbooks.mana(p)+" price="+price+" rest="+p.getAttachedOrElse(Watchweft.READY,0L)+" body="+p.position()+" look="+p.getLookAngle()+" now="+s.overworld().getGameTime());CampConcordNative.check(Watchweft.active(p.getUUID())&&Spellbooks.mana(p)==80-price,"Real paid Touch plus Echo arms one read-only watch with terrain editing disabled");rest=p.getAttachedOrElse(Watchweft.READY,0L);CampConcordNative.check(rest>s.overworld().getGameTime(),"Actual arm stores finite saved rest");});
    c.takeScreenshot(TestScreenshotOptions.of("watchweft_paid_arm").disableCounterPrefix());c.waitTicks(16);w.getServer().runOnServer(s->{CampConcordNative.check(Watchweft.active()==1&&CampConcordNative.player(s).getAttachedOrElse(Watchweft.READY,0L)==rest,"Actual delayed Echo cannot renew or duplicate watch");approach=new Zombie(s.overworld());approach.setCustomName(Component.literal("Native vanilla approach"));approach.setPos(.5,101,5.5);s.overworld().addFreshEntity(approach);});
    // No setTarget, pose/clock override or injected warning; ordinary vanilla goals acquire the actual player.
    c.waitFor(mc->CampConcordClient.accepted(CampConcordFx.WARN)==warnings+1,600);
    w.getServer().runOnServer(s->{var p=CampConcordNative.player(s);CampConcordNative.check(approach.getTarget()==p&&approach.isAlive()&&approach.getHealth()==approach.getMaxHealth(),"Actual AI approach triggered a warning without hurting or retargeting the monster");CampConcordNative.check(!Watchweft.active(p.getUUID())&&p.getAttachedOrElse(Watchweft.READY,0L)==rest,"Warning consumes watch, preserves original rest");approach.discard();});c.takeScreenshot(TestScreenshotOptions.of("watchweft_actual_approach_warning").disableCounterPrefix());c.waitTicks(20);CampConcordNative.check(c.computeOnClient(mc->CampConcordClient.accepted(CampConcordFx.WARN)==warnings+1),"No subsequent duplicate warning");
   }finally{w.getServer().runOnServer(s->CampConcordNative.config(original));}save=w.getWorldSave();
  }
  try(var w=save.open()){c.waitTicks(25);w.getServer().runOnServer(s->{var p=CampConcordNative.player(s);CampConcordNative.check(p.getAttachedOrElse(Watchweft.READY,0L)==rest&&!Watchweft.active(p.getUUID()),"Full server reopen retains exact rest and no historical watch");});}
 }
}
