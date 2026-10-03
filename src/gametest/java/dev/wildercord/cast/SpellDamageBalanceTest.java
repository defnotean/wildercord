package dev.wildercord.cast;

import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import java.util.*;

/** Paid casts reproduce the free exponential Vow exploit against an actual immortal dummy. */
public final class SpellDamageBalanceTest implements FabricClientGameTest {
 public void runTest(ClientGameTestContext c) {
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(40);var server=w.getServer();server.runCommand("gamerule spawn_mobs false");server.runCommand("time set 6000");
   server.runCommand("fill -6 100 -6 6 100 8 stone_bricks");
   int[] dummyId={0};float[] hit={0};
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);
    Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));var b=Spellbooks.get(p).withStarterGiven();
    for(var r:Runes.all())b=b.learn(r.id());Spellbooks.set(p,b);
    p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);
    var d=WildercordEntities.TRAINING_DUMMY.create(s.overworld(),EntitySpawnReason.COMMAND);d.snapTo(.5,101,2,0,0);s.overworld().addFreshEntity(d);dummyId[0]=d.getId();
   });c.waitTicks(15);
   var runes=new ArrayList<String>();runes.add(Runes.TOUCH.id());for(int i=0;i<9;i++)runes.add(Runes.VOW_MOD.id());runes.add(Runes.HARM.id());
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(SpellCaster.edit(p,0,runes)==null,"11-socket exploit accepted");
    Spellbooks.setMana(p,199);Spellbooks.setReadyAt(p,0,0);float before=Spellbooks.mana(p);SpellCaster.cast(p,0);
    check(before-Spellbooks.mana(p)>0&&before-Spellbooks.mana(p)<200,"Real Survival payment below 200 mana");
   });c.waitTicks(12);
   server.runOnServer(s->{var d=(TrainingDummy)s.overworld().getEntity(dummyId[0]);hit[0]=d.lastDamage();System.out.println("VOW_EXPLOIT_NATIVE damage="+hit[0]);check(hit[0]>0,"Actual dummy hit");});
   c.runOnClient(mc->{mc.getWindow().setWindowed(1280,720);mc.resizeGui();mc.gui.toastManager().clear();});
   c.takeScreenshot(TestScreenshotOptions.of("damage_vow_paid").disableCounterPrefix());
   check(hit[0]<1000,"Nine Vows must not inflict 1000 damage for under 200 mana; actual="+hit[0]);
   // The same cheap stack on Bleed used to request 6,656 damage across its initial cut and eight ticks.
   float[] beforeDot={0};
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var d=(TrainingDummy)s.overworld().getEntity(dummyId[0]);
    beforeDot[0]=total(d);runes.set(runes.size()-1,Runes.BLEED.id());
    check(SpellCaster.edit(p,0,runes)==null,"Bleed exploit accepted");Spellbooks.setMana(p,199);Spellbooks.setReadyAt(p,0,0);
    float mana=Spellbooks.mana(p);SpellCaster.cast(p,0);check(mana-Spellbooks.mana(p)>0 && mana-Spellbooks.mana(p)<200,"Paid Bleed below 200 mana");
   });c.waitTicks(95);
   server.runOnServer(s->{var d=(TrainingDummy)s.overworld().getEntity(dummyId[0]);float damage=total(d)-beforeDot[0];System.out.println("BLEED_NATIVE total="+damage);check(damage>0&&damage<1000,"All real Bleed ticks bounded");});
   c.takeScreenshot(TestScreenshotOptions.of("damage_bleed_paid").disableCounterPrefix());
   // Production Venom scheduler, not a simulated accumulator: enormous tick power must exhaust one price.
   Cast[] dot={null};float[] venomStart={0};
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var d=(TrainingDummy)s.overworld().getEntity(dummyId[0]);
    venomStart[0]=total(d);dot[0]=new Cast(p).damagePrice(100);Effects.venomDot(dot[0],d,1000,30);
   });c.waitTicks(85);
   server.runOnServer(s->{var d=(TrainingDummy)s.overworld().getEntity(dummyId[0]);float damage=total(d)-venomStart[0];
    System.out.println("VENOM_NATIVE total="+damage);check(Math.abs(damage-212)<.02,"Actual delayed venom shares the 212 raw damage allowance: "+damage);
    check(!dot[0].damageAvailable(d),"Venom allowance exhausted");
   });c.takeScreenshot(TestScreenshotOptions.of("damage_venom_exhausted").disableCounterPrefix());
   c.waitTicks(25);server.runOnServer(s->{var d=(TrainingDummy)s.overworld().getEntity(dummyId[0]);check(Math.abs(total(d)-venomStart[0]-212)<.02,"Later ticks do not renew allowance");
    var p=s.getPlayerList().getPlayers().getFirst();var source=s.overworld().damageSources().indirectMagic(p,p);
    var cast=new Cast(p).damagePrice(100);float before=total(d);
    for(var part:List.of(cast,cast.child(),cast.pulse(),cast.repeat(),cast.again(2),cast.reflected(p)))Effects.hurt(part,d,source,20000);
    check(Math.abs(total(d)-before-212)<.02,"Children/pulses/repeats/copies/reflections share payment");
    float end=total(d);Effects.hurt(new Cast(p).damagePrice(8),d,source,10);check(total(d)>end,"A new payment can hurt again");
   });

  }
 }
 private static float total(TrainingDummy dummy){try{var f=TrainingDummy.class.getDeclaredField("total");f.setAccessible(true);return f.getFloat(dummy);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);}
}
