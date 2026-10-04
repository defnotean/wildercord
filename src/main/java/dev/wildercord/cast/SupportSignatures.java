package dev.wildercord.cast;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Support signatures: finite paid health/guards, visible healing handoff and willing same-world return. */
public final class SupportSignatures {
 private SupportSignatures(){}
 private record Molt(Cast cast,LivingEntity target,long until){}
 private record Lantern(Cast cast,ServerPlayer target,Vec3 original,long until,boolean previous){}
 private static final Map<UUID,Molt> MOLTS=new HashMap<>();
 private static final Map<UUID,Lantern> LANTERNS=new HashMap<>();
 public static int molts(){return MOLTS.size();}public static int lanterns(){return LANTERNS.size();}
 public static void init(){
  ServerLivingEntityEvents.ALLOW_DAMAGE.register(SupportSignatures::moltDamage);
  ServerLifecycleEvents.SERVER_STOPPING.register(s -> {MOLTS.clear();LANTERNS.clear();NextSignaturePayments.clear();});
 }
 public static boolean apply(Cast c,SpellPlan.EffectNode node,Cast.Hit hit,List<LivingEntity> helped){
  if(c.passive || !Effects.builtIn(node.effect))return false;
  switch(node.effect.path()){
   case "blood_escrow" -> helped.forEach(t -> escrow(c,t));
   case "frost_molt" -> helped.forEach(t -> molt(c,t));
   case "pulse_ferry" -> ferry(c,hit.point());
   case "last_lantern" -> helped.forEach(t -> lantern(c,t));
   default -> {return false;}
  }return true;
 }
 private static boolean consenting(Cast c,LivingEntity t){return NextSignatureSafety.target(c,t,false)
  && !(t instanceof ServerPlayer p && p.isShiftKeyDown());}
 private static void escrow(Cast c,LivingEntity t){
  if(t==c.caster || !consenting(c,t) || t.hasEffect(MobEffects.ABSORPTION) || t.getAbsorptionAmount()>0
   || Statuses.claimed(t,"blood_escrow",160))return;
  float gift=NextSignatureRules.escrow(c.caster.getHealth(),t.getAbsorptionAmount());
  if(gift<=0 || !NextSignaturePayments.of(c).once("blood_escrow"))return;
  // As with Transfusion's genuine resource payment, this is health spent, not a farmable attack event.
  c.caster.setHealth(c.caster.getHealth()-gift);
  t.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,120,0,false,true));
  t.setAbsorptionAmount(gift);Statuses.claim(t,"blood_escrow",160);
  SupportSignatureFx.escrow(c.level,c.caster,t,gift);
 }
 private static void molt(Cast c,LivingEntity t){
  boolean cold=t.getTicksFrozen()>0 || Reactions.has(t,Reactions.Mark.FROZEN);
  if(MOLTS.size()>=128 || !cold || !consenting(c,t) || !t.onGround() || MOLTS.containsKey(t.getUUID())
   || Statuses.claimed(t,"frost_molt",160) || !NextSignaturePayments.of(c).once("frost_molt"))return;
  // Generic Slowness is deliberately untouched; only the actual frost condition pays for this plate.
  t.setTicksFrozen(0);Reactions.clear(t,Reactions.Mark.FROZEN);
  // The shared thaw helper clears generic Slowness/Weakness and AI holds without frost provenance.
  // Peel only actual frost exposure/reaction state; unrelated controls keep their own duration.
  var m=new Molt(c,t,c.level.getGameTime()+60);MOLTS.put(t.getUUID(),m);Statuses.claim(t,"frost_molt",160);
  SupportSignatureFx.molt(c.level,t,false);moltTick(m);
 }
 private static boolean moltValid(Molt m){return consenting(m.cast(),m.target()) && m.target().onGround()
  && m.cast().level.getGameTime()<m.until();}
 private static void moltTick(Molt m){
  if(MOLTS.get(m.target().getUUID())!=m)return;
  if(!moltValid(m)){MOLTS.remove(m.target().getUUID());return;}
  if(m.cast().level.getGameTime()%10==0)SupportSignatureFx.molt(m.cast().level,m.target(),false);
  Scheduler.later(2,Effects.carryContext(() -> moltTick(m)));
 }
 private static boolean moltDamage(LivingEntity t,DamageSource source,float amount){
  var m=MOLTS.get(t.getUUID());if(m==null)return true;
  if(!moltValid(m)){MOLTS.remove(t.getUUID());return true;}
  if(!Float.isFinite(amount) || amount<=0 || amount>6 || !source.is(DamageTypeTags.IS_PROJECTILE)
   || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypeTags.BYPASSES_ARMOR))return true;
  if(!(source.getEntity() instanceof LivingEntity owner) || !Targets.canHarm(owner,t)
   || owner.level()!=m.cast().level || !NextSignatureSafety.loaded(m.cast(),owner.blockPosition()))return true;
  MOLTS.remove(t.getUUID());SupportSignatureFx.molt(m.cast().level,t,true);return false;
 }
 private static List<LivingEntity> recipients(Cast c,Vec3 at,Set<UUID> visited){
  return c.level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(4),t -> !visited.contains(t.getUUID())
   && consenting(c,t) && t.getHealth()<t.getMaxHealth() && t.distanceToSqr(at)<16
   && NextSignatureSafety.open(c,at.add(0,.7,0),t.getEyePosition(),5))
   .stream().sorted(Comparator.<LivingEntity>comparingDouble(t -> t.getMaxHealth()-t.getHealth()).reversed()
    .thenComparing(t -> t.getUUID().toString())).limit(16).toList();
 }
 private static void ferry(Cast c,Vec3 at){
  var visited=new HashSet<UUID>();if(!c.alive() || !NextSignatureSafety.loaded(c,net.minecraft.core.BlockPos.containing(at))
   || recipients(c,at,visited).size()<2 || !NextSignaturePayments.of(c).once("pulse_ferry"))return;
  float[] left={6};Vec3[] from={at.add(0,.7,0)};
  for(int beat=1;beat<=2;beat++){int phase=beat;
   Scheduler.later(phase*20,Effects.carryContext(() -> {
    if(!c.alive() || left[0]<=0)return;var targets=recipients(c,at,visited);if(targets.isEmpty())return;
    var t=targets.getFirst();float amount=NextSignatureRules.ferry(t.getHealth(),t.getMaxHealth(),left[0]);
    if(amount<=0 || !NextSignaturePayments.of(c).target("pulse_ferry_targets",t.getUUID(),2))return;
    visited.add(t.getUUID());float before=t.getHealth();t.heal(amount);left[0]-=amount;
    // Admission spends the intended bounded gift even if a concurrent heal prevention consumes it.
    SupportSignatureFx.ferry(c.level,from[0],t,Math.max(0,t.getHealth()-before),phase);from[0]=t.getEyePosition();
   }));
  }
 }
 private static void lantern(Cast c,LivingEntity t){
  if(!(t instanceof ServerPlayer p) || t==c.caster || LANTERNS.size()>=128 || !NextSignatureSafety.mobile(c,t,false)
   || !t.onGround() || !NextSignatureSafety.body(c,t,t.position(),true)
   || Statuses.claimed(t,"last_lantern",160) || !NextSignaturePayments.of(c).once("last_lantern"))return;
  var l=new Lantern(c,p,t.position(),c.level.getGameTime()+120,p.isShiftKeyDown());
  LANTERNS.put(t.getUUID(),l);Statuses.claim(t,"last_lantern",160);SupportSignatureFx.lantern(c.level,l.original(),false);lanternTick(l);
 }
 private static void lanternTick(Lantern l){
  var c=l.cast();var p=l.target();if(LANTERNS.get(p.getUUID())!=l)return;
  if(!NextSignatureSafety.mobile(c,p,false) || c.level.getGameTime()>=l.until()) {LANTERNS.remove(p.getUUID());return;}
  boolean crouch=p.isShiftKeyDown();
  if(NextSignatureRules.crouchEdge(l.previous(),crouch)){
   LANTERNS.remove(p.getUUID());
   if(p.distanceToSqr(l.original())>64 || !NextSignatureSafety.body(c,p,l.original(),true)
    || !NextSignatureSafety.open(c,p.getEyePosition(),l.original().add(0,p.getEyeHeight(),0),9))return;
   SupportSignatureFx.lantern(c.level,l.original(),true);
   p.teleportTo(c.level,l.original().x,l.original().y,l.original().z,Set.<Relative>of(),p.getYRot(),p.getXRot(),false);p.resetFallDistance();return;
  }
  var next=new Lantern(c,p,l.original(),l.until(),crouch);LANTERNS.put(p.getUUID(),next);
  if(c.level.getGameTime()%10==0)SupportSignatureFx.lantern(c.level,l.original(),false);
  Scheduler.later(1,Effects.carryContext(() -> lanternTick(next)));
 }
}
