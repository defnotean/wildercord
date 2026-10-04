package dev.wildercord.cast;
import dev.wildercord.player.*;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import java.util.UUID;
/** Synchronous admission faults on the two actual connected profiles; these direct gates are not paid deliveries. */
public final class ManabraidFaultCases {
 private ManabraidFaultCases(){}
 private static UUID donor,target;private static String mode="off";private static int claims,nested;private static boolean registered;
 private static void register(){if(registered)return;registered=true;PlayerBlockBreakEvents.BEFORE.register((l,actor,at,state,be)->{
  if(mode.equals("off")||!(actor instanceof ServerPlayer p)||!p.getUUID().equals(donor))return true;
  claims++;var t=p.level().getServer().getPlayerList().getPlayer(target);if(t==null)return false;
  switch(mode){
   case "deny" -> {return false;}
   case "nested" -> {if(Manabraid.offer(new Cast(p),t))nested++;}
   case "mana", "accept_mana" -> Spellbooks.setMana(p,Spellbooks.mana(p)-1);
   case "cord" -> Spellbooks.setCord(t,Spellbooks.cord(t).copy());
   case "body" -> t.setPos(t.getX()+.2,t.getY(),t.getZ());
   case "floor" -> l.setBlock(t.blockPosition().below(),Blocks.POLISHED_ANDESITE.defaultBlockState(),2);
  }
  return true;
 });}
 public static void direct(ServerPlayer p,ServerPlayer t){register();donor=p.getUUID();target=t.getUUID();check(!p.isShiftKeyDown()&&!t.isShiftKeyDown(),"Real profiles initially release input before direct owner gates");
  var cord=Spellbooks.cord(t);var pos=t.position();var floor=t.blockPosition().below();var state=t.level().getBlockState(floor);
  try{for(var scenario:new String[]{"deny","nested","mana","cord","body","floor"}){
   claims=0;nested=0;mode=scenario;Spellbooks.setMana(p,80);Spellbooks.setMana(t,0);boolean admitted=Manabraid.offer(new Cast(p),t);mode="off";
   check(claims==1&&nested==0,"Whole-pair lease prevents recursive new-payment offering under claims");
   check(admitted==scenario.equals("nested"),"Only harmless rejected reentry permits the one outer offer; every changed snapshot refuses");
   check(Spellbooks.mana(t)==0&&Spellbooks.mana(p)==(scenario.equals("mana")?79:80),"Direct owner admission never credits recipient or reverses a callback's real resource mutation");
   if(scenario.equals("cord"))check(Spellbooks.cord(t)!=cord&&ItemStack.matches(Spellbooks.cord(t),cord),"Visually identical replacement Cord identity actually changed");
   if(scenario.equals("floor"))check(t.level().getBlockState(floor).is(Blocks.POLISHED_ANDESITE),"Callback-created floor remains intact; owner does not rollback foreign changes");
   Manabraid.depart(p.getUUID());Spellbooks.setCord(t,cord);t.setPos(pos.x,pos.y,pos.z);t.level().setBlock(floor,state,2);
  }}finally{mode="off";Manabraid.depart(p.getUUID());Spellbooks.setCord(t,cord);t.setPos(pos.x,pos.y,pos.z);t.level().setBlock(floor,state,2);Spellbooks.setMana(p,80);Spellbooks.setMana(t,0);}
 }
 public static float injectAcceptance(ServerPlayer p,ServerPlayer t){register();donor=p.getUUID();target=t.getUUID();claims=0;nested=0;mode="accept_mana";return Spellbooks.mana(p);}
 public static void verifyAcceptance(ServerPlayer p,ServerPlayer t,float before){try{check(claims==1&&Manabraid.active()==0,"Actual crouch acceptance enters claim once, consumes offer, and refuses changed mana snapshot");check(Spellbooks.mana(p)==before-1&&Spellbooks.mana(t)==0,"Failed actual consent preserves external donor debit with zero gift; no refund or invented credit");check(p.getAttachedOrElse(Manabraid.DONOR_READY,0L)==0&&t.getAttachedOrElse(Manabraid.RECEIVE_READY,0L)==0,"Refused donation has no successful saved rests");}finally{mode="off";}}
 public static void clear(){mode="off";donor=null;target=null;}
 private static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
}
