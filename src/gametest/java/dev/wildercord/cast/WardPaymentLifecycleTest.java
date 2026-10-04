package dev.wildercord.cast;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.net.WildercordNetworking;
import dev.wildercord.player.*;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import java.util.*;
/** Actual normal paid CastSpell packets; supplied physical pressure and explicitly labelled paid-copy seam. */
public final class WardPaymentLifecycleTest implements FabricClientGameTest {
 private Husk pressure;
 public void runTest(ClientGameTestContext c){WardPaymentProbe.install();try(var w=c.worldBuilder().create()){
  c.waitTicks(30);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule natural_health_regeneration false");w.getServer().runCommand("fill -24 100 -24 24 100 24 stone");w.getServer().runCommand("fill -24 101 -24 24 108 24 air");
  w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);p.getInventory().clearContent();Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));var b=Spellbooks.get(p).withStarterGiven();for(var r:Runes.all())b=b.learn(r.id());Spellbooks.set(p,b);pressure=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);check(pressure!=null,"Physical pressure receiver created");pressure.setCustomName(Component.literal("WardPressure"));pressure.setNoAi(true);pressure.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);pressure.setHealth(100);pressure.setPos(6.5,101,.5);s.overworld().addFreshEntity(pressure);check(pressure.getHealth()==100,"Named admission preserves supplied health baseline");});
  for(var effect:List.of(Runes.FORESIGHT,Runes.REFLECT)){
   for(var route:List.of(List.of(Runes.PULSE,Runes.SELF,effect),List.of(Runes.SELF,effect,Runes.ECHO))){
    final int[] price={0};final float[] mana={0};
    w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(SpellCaster.edit(p,0,route.stream().map(RuneDef::id).toList())==null,"Editor accepts real repeating/echo route");price[0]=Heart.manaCost(p,SpellCompiler.compile(route),Heart.secretCost(p,route)*Mastery.costFactor(p,route));Spellbooks.setMana(p,300);Spellbooks.setReadyAt(p,0,0);mana[0]=Spellbooks.mana(p);WardPaymentProbe.arm(p);});
    c.runOnClient(mc->ClientPlayNetworking.send(new WildercordNetworking.CastSpell(0)));
    for(int i=0;i<100 && WardPaymentProbe.admitted==null;i++)c.waitTicks(1);
    w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(WardPaymentProbe.admitted!=null,"Actual paid packet reaches production ward");System.out.println("WARD_PRICE route="+route+" expected="+price[0]+" actualCost="+WardPaymentProbe.actualCost+" spent="+WardPaymentProbe.actualSpent+" before="+WardPaymentProbe.manaBeforePayment+" after="+WardPaymentProbe.manaAfterPayment+" ward="+WardPaymentProbe.manaAtAdmission+" beforeCount="+WardPaymentProbe.beforeCount+" afterCount="+WardPaymentProbe.afterCount);
     check(WardPaymentProbe.beforeCount==1 && WardPaymentProbe.afterCount==1,"One actual ordinary packet payment receipt");
     check(WardPaymentProbe.actualCost==price[0] && WardPaymentProbe.actualSpent==price[0],"Actual compiled price and spent receipt match exactly");
     check(Math.abs(WardPaymentProbe.manaBeforePayment-WardPaymentProbe.manaAfterPayment-price[0])<.01,"Exact synchronous mana debit, before delayed formation/Pulse regen");int count=effect==Runes.FORESIGHT?2:6;for(int i=0;i<count;i++)blow(p);check(remaining(effect,p)==0,"Actual pressure exhausts original charges");});
    c.waitTicks(24);
    w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(remaining(effect,p)==0,"Real Echo/delayed Pulse cannot refill exhausted original payment");var paid=WardPaymentProbe.admitted;check(paid.again(1).payment()==paid.payment() && paid.again(1).identity()!=paid.identity(),"Actual paid owner has distinct Shared-copy identity");
     // Explicit trusted paid-copy seam: no claim of a separate native parry here.
     if(effect==Runes.FORESIGHT){Wards.foresight(paid.again(1),p,300,2);Wards.foresight(paid.reflected(p),p,300,2);}else{Wards.reflect(paid.again(1),p,200,.6);Wards.reflect(paid.reflected(p),p,200,.6);}
     check(remaining(effect,p)==0,"Paid again/reflected Shared copies cannot refill");});
    c.waitTicks(24);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(remaining(effect,p)==0,"Final delayed pulse cannot refill either");check(WardPaymentProbe.payments.size()==1 && WardPaymentProbe.beforeCount==1 && WardPaymentProbe.afterCount==1,"Every actual continuation retains one payment owner and one real debit receipt");});
   }
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(SpellCaster.edit(p,0,List.of(Runes.SELF.id(),effect.id()))==null,"Separate ordinary ward editor route");Spellbooks.setMana(p,300);Spellbooks.setReadyAt(p,0,0);WardPaymentProbe.arm(p);});
   c.runOnClient(mc->ClientPlayNetworking.send(new WildercordNetworking.CastSpell(0)));
   for(int i=0;i<100 && WardPaymentProbe.admitted==null;i++)c.waitTicks(1);
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(remaining(effect,p)==(effect==Runes.FORESIGHT?2:6),"Genuinely separate paid cast refreshes normal charges");if(effect==Runes.FORESIGHT){blow(p);blow(p);check(remaining(effect,p)==0,"Drain positive separate Foresight before subsequent Reflect physical-pressure scene");}});
  }
 }finally{WardPaymentProbe.clear();}}
 private void blow(ServerPlayer p){Effects.readyToHurt(p);p.hurtServer(p.level(),p.level().damageSources().mobAttack(pressure),1);}
 private static int remaining(RuneDef effect,ServerPlayer p){try{var f=Wards.class.getDeclaredField(effect==Runes.FORESIGHT?"FORESIGHT":"REFLECT");f.setAccessible(true);var map=(Map<?,?>)f.get(null);var state=map.get(p.getUUID());if(state==null)return 0;var left=state.getClass().getDeclaredField(effect==Runes.FORESIGHT?"charges":"left");left.setAccessible(true);return left.getInt(state);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
