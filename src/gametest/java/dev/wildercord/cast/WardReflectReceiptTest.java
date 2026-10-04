package dev.wildercord.cast;
import dev.wildercord.Wildercord;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.net.WildercordNetworking;
import dev.wildercord.player.*;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import java.util.*;
/** Genuine paid mirror; an earlier AFTER_DAMAGE phase applies real foreign nonlethal damage. */
public final class WardReflectReceiptTest implements FabricClientGameTest {
 private static boolean registered,foreign,observed;
 private static ServerPlayer recipient;
 private static Husk attacker;
 private static DamageSource original,foreignReceipt;
 public void runTest(ClientGameTestContext c){register();WardPaymentProbe.install();try{for(boolean mutation:new boolean[]{false,true})exercise(c,mutation);}finally{clear();WardPaymentProbe.clear();}}
 private static void register(){if(registered)return;registered=true;var phase=Wildercord.id("ward_reflect_receipt_test");ServerLivingEntityEvents.AFTER_DAMAGE.addPhaseOrdering(phase,Event.DEFAULT_PHASE);ServerLivingEntityEvents.AFTER_DAMAGE.register(phase,(target,source,base,taken,blocked)->{
  if(observed||recipient==null||target!=recipient||source!=original||blocked||taken<=0)return;
  observed=true;check(recipient.getHealth()==15,"Original genuine five-point physical wound remains before callback");
  if(foreign){foreignReceipt=recipient.level().damageSources().genericKill();boolean admitted=recipient.hurtServer(recipient.level(),foreignReceipt,6);
   System.out.println("WARD_REFLECT_RECEIPT foreign admitted="+admitted+" health="+recipient.getHealth()+" lastForeign="+(recipient.getLastDamageSource()==foreignReceipt)+" attackerHealth="+attacker.getHealth());
   // Ordinary vanilla cooldown admits the one-point excess. Never clear cooldown or reset health here.
   check(admitted && recipient.isAlive() && recipient.getHealth()==14 && recipient.getLastDamageSource()==foreignReceipt,"Actual nested nonlethal foreign injury supersedes original receipt");}
 });}
 private static void exercise(ClientGameTestContext c,boolean mutation){try(var w=c.worldBuilder().create()){
  c.waitTicks(30);w.getServer().runCommand("difficulty normal");w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule natural_health_regeneration false");w.getServer().runCommand("fill -8 100 -8 8 100 8 stone");w.getServer().runCommand("fill -8 101 -8 8 106 8 air");
  final int[] price={0};w.getServer().runOnServer(s->{recipient=s.getPlayerList().getPlayers().getFirst();recipient.setGameMode(GameType.SURVIVAL);recipient.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);recipient.getInventory().clearContent();check(recipient.getHealth()==20 && recipient.getAbsorptionAmount()==0,"Unmodified real receiver health and absorption baseline");Spellbooks.setCord(recipient,new ItemStack(WildercordItems.ECHO_CORD));var b=Spellbooks.get(recipient).withStarterGiven().learn(Runes.SELF.id()).learn(Runes.REFLECT.id());Spellbooks.set(recipient,b);var route=List.of(Runes.SELF,Runes.REFLECT);check(SpellCaster.edit(recipient,0,route.stream().map(RuneDef::id).toList())==null,"Editor admits normal paid mirror");price[0]=Heart.manaCost(recipient,SpellCompiler.compile(route),Heart.secretCost(recipient,route)*Mastery.costFactor(recipient,route));Spellbooks.setMana(recipient,300);Spellbooks.setReadyAt(recipient,0,0);WardPaymentProbe.arm(recipient);
   attacker=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);check(attacker!=null,"Actual registered physical source");attacker.setCustomName(Component.literal("WardReceiptSource"));attacker.setNoAi(true);attacker.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);attacker.getAttribute(Attributes.ARMOR).setBaseValue(0);attacker.setHealth(100);attacker.setPos(3.5,101,.5);s.overworld().addFreshEntity(attacker);check(attacker.getHealth()==100,"Named physical source retains supplied exact native baseline");});
  c.runOnClient(mc->ClientPlayNetworking.send(new WildercordNetworking.CastSpell(0)));for(int i=0;i<100 && WardPaymentProbe.admitted==null;i++)c.waitTicks(1);
  w.getServer().runOnServer(s->{check(WardPaymentProbe.admitted!=null && WardPaymentProbe.beforeCount==1 && WardPaymentProbe.afterCount==1 && WardPaymentProbe.actualCost==price[0] && WardPaymentProbe.actualSpent==price[0],"Actual ordinary mirror payment receipts");check(Math.abs(WardPaymentProbe.manaBeforePayment-WardPaymentProbe.manaAfterPayment-price[0])<.01,"Exact synchronous real mirror debit");check(left()==6,"Actual initial mirror has six original returns");foreign=mutation;observed=false;foreignReceipt=null;original=recipient.level().damageSources().mobAttack(attacker);check(recipient.hurtServer(recipient.level(),original,5),"Actual physical outer strike admitted");
   check(observed && recipient.getHealth()==(mutation?14:15),"Original physical wound preserved in both controls");check(Math.abs(attacker.getHealth()-(mutation?100:97))<.01,"Superseded incoming receipt refuses return; unchanged receipt retains actual sixty-percent return");check(left()==(mutation?6:5),"Refused original receipt spends no facet; unchanged return spends exactly one");check(mutation?recipient.getLastDamageSource()==foreignReceipt:recipient.getLastDamageSource()==original,"Actual final damage-source identity matches branch");System.out.println("WARD_REFLECT_FINAL foreign="+mutation+" receiver="+recipient.getHealth()+" attacker="+attacker.getHealth()+" returns="+left());});
 }finally{clear();WardPaymentProbe.clear();}}
 private static int left(){try{var f=Wards.class.getDeclaredField("REFLECT");f.setAccessible(true);var ward=((Map<?,?>)f.get(null)).get(recipient.getUUID());if(ward==null)return 0;var n=ward.getClass().getDeclaredField("left");n.setAccessible(true);return n.getInt(ward);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void clear(){recipient=null;attacker=null;original=null;foreignReceipt=null;foreign=false;observed=false;}
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
