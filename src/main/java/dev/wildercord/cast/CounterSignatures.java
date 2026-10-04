package dev.wildercord.cast;

import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Finite counterplay signatures with post-bonus damage admission and permanent projectile provenance. */
public final class CounterSignatures {
 private CounterSignatures(){}
 private record Screen(Cast cast,LivingEntity target,long until,long opened,Set<UUID> existing,boolean quiet){}
 private record Record(Cast cast,LivingEntity target,Vec3 original,long until,int cuts,long previous){}
 private static final Map<UUID,Screen> SCREENS=new HashMap<>();
 private static final Map<UUID,Record> RECORDS=new HashMap<>();
 public static int active(){return SCREENS.size()+RECORDS.size();}
 public static void teleported(UUID id){RECORDS.remove(id);}
 public static void init(){ServerLifecycleEvents.SERVER_STOPPING.register(s -> {SCREENS.clear();RECORDS.clear();NextSignaturePayments.clear();});}
 public static boolean apply(Cast c,SpellPlan.EffectNode node,List<LivingEntity> helped,List<LivingEntity> harmed){
  if(c.passive || !Effects.builtIn(node.effect))return false;
  switch(node.effect.path()){
   case "nullcatch" -> helped.forEach(t -> screen(c,t,false));
   case "quietus" -> harmed.forEach(t -> screen(c,t,true));
   case "second_bell" -> harmed.forEach(t -> bell(c,t));
   case "red_ledger" -> harmed.forEach(t -> ledger(c,t));
   default -> {return false;}
  }return true;
 }
 private static boolean valid(Cast c,LivingEntity t,boolean hostile){
  return NextSignatureSafety.target(c,t,hostile) && !Spirits.isBoss(t) && !VoidTime.anchored(t);
 }
 private static BoundedCounterCandidates.Batch nearby(Cast c,LivingEntity t){
  return BoundedCounterCandidates.nearby(c,t);
 }
 private static void screen(Cast c,LivingEntity t,boolean quiet){
  String id=quiet?"quietus":"nullcatch";
  if(SCREENS.size()>=NextSignatureRules.ACTIVE || !valid(c,t,quiet) || Statuses.claimed(t,id,NextSignatureRules.REST))return;
  var initial=nearby(c,t);
  // Overcrowding visibly refuses before target allowance or rest is consumed.
  if(initial.saturated()){CounterSignatureFx.release(c.level,t,quiet);return;}
  if(!NextSignaturePayments.of(c).target(id,t.getUUID(),quiet?1:4))return;
  var old=new HashSet<UUID>();if(quiet)for(var p:initial.completeSnapshot())old.add(p.getUUID());
  long now=c.level.getGameTime();var s=new Screen(c,t,now+(quiet?40:60),now+6,old,quiet);
  Statuses.claim(t,id,NextSignatureRules.REST);SCREENS.put(t.getUUID(),s);CounterSignatureFx.open(c.level,t,quiet);screenTick(s);
 }
 private static void screenTick(Screen s){
  var c=s.cast();var t=s.target();if(SCREENS.get(t.getUUID())!=s)return;
  String id=s.quiet()?"quietus":"nullcatch";
  if(!valid(c,t,s.quiet()) || c.level.getGameTime()>=s.until() || NextSignaturePayments.of(c).left(id+"_capture",1)<=0){
   SCREENS.remove(t.getUUID());if(t.isAlive() && t.level()==c.level)CounterSignatureFx.release(c.level,t,s.quiet());return;
  }
  var batch=nearby(c,t);
  if(batch.saturated()){
   SCREENS.remove(t.getUUID());CounterSignatureFx.release(c.level,t,s.quiet());return;
  }
  if(c.level.getGameTime()>=s.opened())for(var p:batch.candidates()){
   if(!(p.getOwner() instanceof LivingEntity owner) || !owner.isAlive() || owner.level()!=c.level || Spirits.isBoss(owner) || owner.isPermanentlyInvulnerable()
    || !NextSignatureSafety.loaded(c,owner.blockPosition()) || DungeonSafety.warded(c,owner)
    || !NextSignatureSafety.open(c,p.position(),t.getEyePosition(),4))continue;
   if(s.quiet()){
    // A reflected spell has no admission, even if its changed owner matches the marked target.
    if(!(p instanceof RuneBolt bolt) || bolt.isReflected() || owner!=t || p.tickCount>6 || s.existing().contains(p.getUUID())
     || !Targets.canHarm(t,c.caster) || p.getDeltaMovement().dot(p.position().subtract(t.position()))<=0)continue;
   }else{
    if(!Targets.canHarm(owner,t) || p instanceof RuneBolt b && b.isReflected())continue;
    var to=p.position().subtract(t.getEyePosition());
    if(to.lengthSqr()<.01 || to.normalize().dot(t.getLookAngle())<.35
     || p.getDeltaMovement().dot(t.getEyePosition().subtract(p.position()))<=0)continue;
   }
   if(NextSignaturePayments.of(c).resource(id+"_capture",1,1)!=1)break;
   p.discard();SCREENS.remove(t.getUUID());
   if(s.quiet() && t instanceof ServerPlayer player){
    float mana=Spellbooks.mana(player);if(Float.isFinite(mana))Spellbooks.setMana(player,Math.max(0,mana-8));
   }
   CounterSignatureFx.capture(c.level,t,p.position(),s.quiet());return;
  }
  if(c.level.getGameTime()%8==0)CounterSignatureFx.open(c.level,t,s.quiet());
  Scheduler.later(1,Effects.carryContext(() -> screenTick(s)));
 }
 private static void bell(Cast c,LivingEntity t){
  if(!NextSignatureSafety.target(c,t,true) || !NextSignaturePayments.of(c).target("second_bell",t.getUUID(),2))return;
  Vec3 original=t.position();CounterSignatureFx.bell(c.level,t,0);
  for(int beat=1;beat<=2;beat++) {int phase=beat;
   Scheduler.later(phase*20,Effects.carryContext(() -> {
    if(!NextSignatureSafety.target(c,t,true) || !NextSignatureSafety.open(c,c.caster.getEyePosition(),t.getEyePosition(),24))return;
    var moved=t.position().subtract(original);
    if(phase==2 && NextSignatureRules.escapedBell(moved.x,moved.z)){CounterSignatureFx.cancelBell(c.level,original);return;}
    // Cap AFTER all reaction/performance/PvP bonuses, before normal paid damage/defence admission.
    Effects.hurtCapped(c,t,c.level.damageSources().indirectMagic(c.caster,c.caster),2,
     amount -> NextSignaturePayments.of(c).contribution("second_bell_damage",t.getUUID(),8,4,2,(float)amount));
    CounterSignatureFx.bell(c.level,t,phase);
   }));
  }
 }
 private static void ledger(Cast c,LivingEntity t){
  if(RECORDS.size()>=NextSignatureRules.ACTIVE || !NextSignatureSafety.mobile(c,t,true)
   || Statuses.claimed(t,"red_ledger",NextSignatureRules.REST)
   || !NextSignaturePayments.of(c).target("red_ledger",t.getUUID(),3))return;
  long now=c.level.getGameTime();var r=new Record(c,t,t.position(),now+60,0,now);
  RECORDS.put(t.getUUID(),r);Statuses.claim(t,"red_ledger",NextSignatureRules.REST);
  CounterSignatureFx.gates(c.level,t,3);ledgerTick(r);
 }
 private static void ledgerTick(Record r){
  var c=r.cast();var t=r.target();if(RECORDS.get(t.getUUID())!=r)return;
  long now=c.level.getGameTime();var movement=t.position().subtract(r.original());
  if(!NextSignatureSafety.mobile(c,t,true) || now>=r.until() || r.cuts()>=3
   || movement.lengthSqr()>9 || NextSignaturePayments.of(c).left("red_ledger_damage",6)<=0){RECORDS.remove(t.getUUID());return;}
  Record next=r;
  if(now-r.previous()>=8 && NextSignatureRules.ledgerStep(movement.x,movement.z)
   && NextSignatureSafety.open(c,c.caster.getEyePosition(),t.getEyePosition(),24)){
   Effects.hurtCapped(c,t,c.level.damageSources().indirectMagic(c.caster,c.caster),2,
    amount -> NextSignaturePayments.of(c).contribution("red_ledger_damage",t.getUUID(),6,6,2,(float)amount));
   // Damage callbacks may teleport/remove the target or replace its lease synchronously.
   // A cancelled owner cannot be resurrected by this earlier tick's post-damage continuation.
   if(RECORDS.get(t.getUUID())!=r)return;
   if(!NextSignatureSafety.mobile(c,t,true)){RECORDS.remove(t.getUUID(),r);return;}
   next=new Record(c,t,t.position(),r.until(),r.cuts()+1,now);RECORDS.put(t.getUUID(),next);
   CounterSignatureFx.cut(c.level,t,3-next.cuts());
  }else if(now%10==0)CounterSignatureFx.gates(c.level,t,3-r.cuts());
  Record continuing=next;Scheduler.later(2,Effects.carryContext(() -> ledgerTick(continuing)));
 }
 /** Separate method keeps ward checks loaded before querying dungeon fields. */
 private static final class DungeonSafety {
  static boolean warded(Cast c,LivingEntity t){return dev.wildercord.world.dungeons.DungeonWards.warded(c.level,t.blockPosition());}
 }
}
