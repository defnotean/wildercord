package dev.wildercord.cast;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.net.WildercordNetworking;
import dev.wildercord.player.*;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import java.util.*;
/** Two distinct real packet payments, real natural expiry cleanup, labelled trusted old-paid replay. */
public final class WardPaymentRetirementTest implements FabricClientGameTest {
 public void runTest(ClientGameTestContext c){WardPaymentProbe.install();try(var w=c.worldBuilder().create()){
  c.waitTicks(30);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("fill -8 100 -8 8 100 8 stone");w.getServer().runCommand("fill -8 101 -8 8 106 8 air");
  w.getServer().runOnServer(s->{var p=player(s);p.setGameMode(GameType.SURVIVAL);p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);p.getInventory().clearContent();Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));var b=Spellbooks.get(p).withStarterGiven();for(var r:Runes.all())b=b.learn(r.id());Spellbooks.set(p,b);});
  for(var effect:List.of(Runes.FORESIGHT,Runes.REFLECT)){
   Cast a=packet(c,w,effect);Object first=w.getServer().computeOnServer(s->state(effect,player(s)));
   Cast b=packet(c,w,effect);check(a.payment()!=b.payment(),"Actual second packet has a distinct payment owner");
   final Object replacement=w.getServer().computeOnServer(s->{var p=player(s);var actual=state(effect,p);check(actual!=null && actual!=first,"Separate paid ward really replaces attached first ward");return actual;});
   final long until=w.getServer().computeOnServer(s->number(replacement,"until").longValue());
   w.getServer().runOnServer(s->{var p=player(s);int charges=number(replacement,effect==Runes.FORESIGHT?"charges":"left").intValue();
    // Trusted old-paid replay seam only: neither call represents a second native parry or an extra packet.
    replay(effect,a.again(1),p);replay(effect,a.reflected(p),p);
    check(state(effect,p)==replacement,"Old payment cannot replace a genuinely newer paid ward");check(number(replacement,"until").longValue()==until && number(replacement,effect==Runes.FORESIGHT?"charges":"left").intValue()==charges,"Old replay changes neither newer deadline nor charges");});
   boolean cleared=false;for(int i=0;i<900;i++){cleared=w.getServer().computeOnServer(s->s.overworld().getGameTime()>until && state(effect,player(s))==null);if(cleared)break;c.waitTicks(1);}
   check(cleared,"Actual original expiry passes and normal 200-tick cleanup removes ward; no forced timer");
   w.getServer().runOnServer(s->{var p=player(s);check(state(effect,p)==null && s.overworld().getGameTime()>until,"Real expired map state before replay");replay(effect,b.again(1),p);replay(effect,b.reflected(p),p);check(state(effect,p)==null,"Old payment cannot rearm after actual expiry cleanup");System.out.println("WARD_RETIREMENT effect="+effect.id()+" originalUntil="+until+" actualNow="+s.overworld().getGameTime()+" replacedOwnerPreserved=true expiredReplayRefused=true");});
   Cast fresh=packet(c,w,effect);check(fresh.payment()!=b.payment(),"Post-expiry packet really pays a fresh owner");w.getServer().runOnServer(s->{var current=state(effect,player(s));check(current!=null && number(current,effect==Runes.FORESIGHT?"charges":"left").intValue()==(effect==Runes.FORESIGHT?2:6),"Genuine fresh payment reopens ordinary charges after expiry");});
  }
 }finally{WardPaymentProbe.clear();}}
 private static Cast packet(ClientGameTestContext c,TestSingleplayerContext w,RuneDef effect){final int[] expected={0};w.getServer().runOnServer(s->{var p=player(s);var route=List.of(Runes.SELF,effect);check(SpellCaster.edit(p,0,route.stream().map(RuneDef::id).toList())==null,"Real editor permits learned ordinary ward");expected[0]=Heart.manaCost(p,SpellCompiler.compile(route),Heart.secretCost(p,route)*Mastery.costFactor(p,route));Spellbooks.setMana(p,300);Spellbooks.setReadyAt(p,0,0);WardPaymentProbe.arm(p);});
  c.runOnClient(mc->ClientPlayNetworking.send(new WildercordNetworking.CastSpell(0)));for(int i=0;i<100 && WardPaymentProbe.admitted==null;i++)c.waitTicks(1);
  return w.getServer().computeOnServer(s->{check(WardPaymentProbe.admitted!=null,"Real packet enters production ward");check(WardPaymentProbe.beforeCount==1 && WardPaymentProbe.afterCount==1 && WardPaymentProbe.actualCost==expected[0] && WardPaymentProbe.actualSpent==expected[0],"Exact ordinary packet cost/spent receipts");check(Math.abs(WardPaymentProbe.manaBeforePayment-WardPaymentProbe.manaAfterPayment-expected[0])<.01,"Exact synchronous actual mana debit");return WardPaymentProbe.admitted;});
 }
 private static void replay(RuneDef effect,Cast paid,ServerPlayer p){if(effect==Runes.FORESIGHT)Wards.foresight(paid,p,300,2);else Wards.reflect(paid,p,200,.6);}
 private static ServerPlayer player(net.minecraft.server.MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
 private static Object state(RuneDef effect,ServerPlayer p){try{var f=Wards.class.getDeclaredField(effect==Runes.FORESIGHT?"FORESIGHT":"REFLECT");f.setAccessible(true);return ((Map<?,?>)f.get(null)).get(p.getUUID());}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static Number number(Object state,String name){try{var f=state.getClass().getDeclaredField(name);f.setAccessible(true);return (Number)f.get(state);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
