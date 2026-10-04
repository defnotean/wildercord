package dev.wildercord.cast;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Server-side observations only. This does not decide gameplay admission.
 * Owners publish AFTER their real mutation. No scheduling, healing, damage or block writes here.
 * The observer is intentionally injectable for native acceptance; network binding is separate.
 */
final class LifeOwnerEvents {
 enum Moment { APPLY,PULSE,TRIGGER,RENEW,END,REFUSED }
 record Event(ServerLevel level,String rune,Moment moment,UUID recipient,Vec3 anchor,
              Vec3 secondary,int units,double delta,long tick,String detail,Vec3 normal,double standoff,UUID source) {
  Event(ServerLevel level,String rune,Moment moment,UUID recipient,Vec3 anchor,Vec3 secondary,int units,double delta,long tick,String detail,Vec3 normal,double standoff){this(level,rune,moment,recipient,anchor,secondary,units,delta,tick,detail,normal,standoff,null);}
  Event(ServerLevel level,String rune,Moment moment,UUID recipient,Vec3 anchor,Vec3 secondary,int units,double delta,long tick,String detail){this(level,rune,moment,recipient,anchor,secondary,units,delta,tick,detail,new Vec3(0,0,1),0,null);}
 }
 record Before(float health,float absorption,int food,int harmful,boolean fire,int frozen,int reactions) {}
 private record MarkKey(ServerLevel level,String rune,UUID recipient){}
 private record Mark(LivingEntity body,Supplier<Long> ownerStamp,boolean consumedByTrigger,long lastUntil,UUID source){}
 private static final Map<MarkKey,Mark> MARKS=new HashMap<>();
 private record DotOwner(Cast cast,String rune,LivingEntity body){}
 private static final Map<Object,DotOwner> DOTS=new IdentityHashMap<>();
 private static boolean initialized;
 private static Consumer<Event> observer=e->{};
 private static final Map<ServerLevel,Long> TICK=new IdentityHashMap<>();
 private static final Map<ServerLevel,Integer> COUNT=new IdentityHashMap<>();
 static AutoCloseable observe(Consumer<Event> sink){var old=observer;observer=Objects.requireNonNull(sink);return ()->observer=old;}
 static void init(){
  if(initialized)return;initialized=true;
  ServerTickEvents.END_SERVER_TICK.register(s->{
   // Release presentation ownership when its real body/caster/world is no longer active.
   DOTS.entrySet().removeIf(e->{var o=e.getValue();return !o.cast().alive()||o.body().isRemoved()||!o.body().isAlive()||o.body().level()!=o.cast().level||s.getLevel(o.cast().level.dimension())!=o.cast().level;});
   TICK.keySet().removeIf(l->s.getLevel(l.dimension())!=l);
   COUNT.keySet().removeIf(l->s.getLevel(l.dimension())!=l);
   LifeOutcomeDelivery.prune(s);
   for(var it=MARKS.entrySet().iterator();it.hasNext();){
    var entry=it.next();var key=entry.getKey();var mark=entry.getValue();var t=mark.body();
    if(t.isRemoved()||!t.isAlive()||t.level()!=key.level()||s.getLevel(key.level().dimension())!=key.level()){it.remove();continue;}
    // Observe an actual owner's timestamp; refresh naturally updates the supplied live map value.
    Long until=mark.ownerStamp().get();
    if(until==null){
     // Owner removal before its observed deadline is cancellation/consumption, not natural expiry.
     it.remove();if(key.level().getGameTime()>mark.lastUntil())transition(key.level(),key.rune(),t,Moment.END,1,0,null,mark.source());
    }else if(key.level().getGameTime()>until){it.remove();transition(key.level(),key.rune(),t,Moment.END,1,0,null,mark.source());}
    else if(until!=mark.lastUntil())entry.setValue(new Mark(t,mark.ownerStamp(),mark.consumedByTrigger(),until,mark.source()));
   }
  });
  ServerLifecycleEvents.SERVER_STOPPED.register(s->clear());
 }
 static void track(Cast c,String rune,LivingEntity t,Supplier<Long> ownerStamp,boolean consumedByTrigger){
  var key=new MarkKey(c.level,rune,t.getUUID());
  if(MARKS.size()>=128&&!MARKS.containsKey(key))return;
  Long until=ownerStamp.get();if(until!=null)MARKS.put(key,new Mark(t,ownerStamp,consumedByTrigger,until,c.caster.getUUID()));
 }
 static int tracked(){return MARKS.size();}
 static void clear(){MARKS.clear();DOTS.clear();TICK.clear();COUNT.clear();observer=e->{};LifeOutcomeDelivery.clear();}
 static void ownDot(Cast c,String rune,LivingEntity t,Object actualState){
  if(DOTS.size()<128)DOTS.put(actualState,new DotOwner(c,rune,t));
 }
 static void renewDot(Object actualState,LivingEntity t){
  var owner=DOTS.get(actualState);
  // Shared venom/spore renewal retains the original scheduler's actual owner and material identity.
  if(owner!=null&&owner.body()==t&&owner.cast().alive())admitted(owner.cast(),owner.rune(),t,Moment.RENEW,1,null);
 }
 static void forgetDot(Object actualState){DOTS.remove(actualState);}
 static Before before(LivingEntity t){return new Before(t.getHealth(),t.getAbsorptionAmount(),
   t instanceof net.minecraft.world.entity.player.Player p?p.getFoodData().getFoodLevel():0,
   (int)t.getActiveEffects().stream().filter(e->e.getEffect().value().getCategory()==net.minecraft.world.effect.MobEffectCategory.HARMFUL).count(),t.isOnFire(),t.getTicksFrozen(),Reactions.marks(t).size());}
 static void changed(Cast c,String rune,LivingEntity t,Before old,Vec3 secondary,Moment phase){
  if(t.isRemoved()||t.level()!=c.level)return;
  var now=before(t);double health=now.health()-old.health();
  int removals=Math.max(0,old.harmful()-now.harmful())+(old.fire()&&!now.fire()?1:0)+(old.frozen()>0&&now.frozen()==0?1:0)+Math.max(0,old.reactions()-now.reactions());
  int food=Math.max(0,now.food()-old.food());double absorb=Math.max(0,now.absorption()-old.absorption());
  // No invented successful heal or cleanse when the real mutation changed nothing.
  if(health==0&&removals==0&&food==0&&absorb==0)return;
  emit(c,rune,phase,t.getUUID(),LifeOutcomeFrames.anchor(rune,t),secondary,removals+food+(absorb>0?1:0),health);
 }
 static void mutation(Cast c,String rune,LivingEntity t,Moment phase,Vec3 secondary,Runnable actualMutation){
  var old=before(t);actualMutation.run();changed(c,rune,t,old,secondary,phase);
 }
 static void status(Cast c,String rune,LivingEntity t,Moment phase,Vec3 secondary,java.util.function.BooleanSupplier actualAdmission){
  if(actualAdmission.getAsBoolean())admitted(c,rune,t,phase,1,secondary);
 }
 static void admitted(Cast c,String rune,LivingEntity t,Moment phase,int remaining,Vec3 secondary){
  if(!t.isAlive()||t.isRemoved()||t.level()!=c.level)return;
  emit(c,rune,phase,t.getUUID(),LifeOutcomeFrames.anchor(rune,t),secondary,Math.max(0,remaining),0);
 }
 static void transition(ServerLevel l,String rune,LivingEntity t,Moment phase,int units,double actualDelta,Vec3 secondary){
  if(t.isRemoved()||!t.isAlive()||t.level()!=l)return;
  var mark=MARKS.get(new MarkKey(l,rune,t.getUUID()));
  transition(l,rune,t,phase,units,actualDelta,secondary,mark!=null&&mark.body()==t?mark.source():null);
 }
 static void transition(ServerLevel l,String rune,LivingEntity t,Moment phase,int units,double actualDelta,Vec3 secondary,UUID source){
  if(t.isRemoved()||!t.isAlive()||t.level()!=l)return;
  var frame=source!=null&&l.getEntity(source) instanceof LivingEntity caster?LifeOutcomeFrames.body(caster,t,""):LifeOutcomeFrames.event(l,t.getUUID());
  emit(l,source,rune,phase,t.getUUID(),LifeOutcomeFrames.anchor(rune,t),secondary,units,actualDelta,"",frame);
 }
 static void cell(Cast c,String rune,BlockPos at,BlockState before,BlockState after,Moment phase){
  if(before.equals(after)||!c.level.isLoaded(at)||!c.level.getBlockState(at).equals(after))return;
  emit(c,rune,phase,null,Vec3.atCenterOf(at),null,1,0);
 }
 static void point(Cast c,String rune,Moment phase,Vec3 at,Vec3 secondary,int units,double delta){
  emit(c,rune,phase,null,at,secondary,units,delta);
 }
 static void refused(Cast c,String rune,LivingEntity t){
  emit(c,rune,Moment.REFUSED,t.getUUID(),t.getBoundingBox().getCenter(),null,0,0);
 }
 static void repair(Cast c,LivingEntity t,EquipmentSlot slot,ItemStack stack,int beforeDamage){
  if(t.isRemoved()||t.level()!=c.level||t.getItemBySlot(slot)!=stack||stack.isEmpty())return;
  int restored=beforeDamage-stack.getDamageValue();if(restored<=0)return;
  var look=t.getLookAngle();var flat=new Vec3(look.x,0,look.z);if(flat.lengthSqr()<1e-5)flat=new Vec3(0,0,1);else flat=flat.normalize();
  var side=new Vec3(-flat.z,0,flat.x);double h=t.getBbHeight();
  Vec3 anchor=switch(slot){
   case MAINHAND -> t.position().add(0,h*.5,0).add(side.scale(.32)).add(flat.scale(.2));
   case OFFHAND -> t.position().add(0,h*.5,0).add(side.scale(-.32)).add(flat.scale(.2));
   case HEAD -> t.position().add(0,h*.9,0);
   case CHEST -> t.position().add(0,h*.65,0);
   case LEGS -> t.position().add(0,h*.3,0);
   case FEET -> t.position().add(0,h*.1,0);
   default -> t.getBoundingBox().getCenter();
  };
  emit(c,"restore",Moment.TRIGGER,t.getUUID(),anchor,null,1,restored,"equipment:"+slot.getName());
 }
 static void counted(Cast c,LivingEntity t,double actualCountedDamage){
  if(t.isRemoved()||t.level()!=c.level||actualCountedDamage<=0||!Double.isFinite(actualCountedDamage))return;
  // The owner counts the damage-event amount, which need not equal health lost through absorption.
  // Keep it separate from delta: zero healing/damage is invented by this counter observation.
  emit(c,"stitchtime",Moment.PULSE,t.getUUID(),LifeOutcomeFrames.anchor("stitchtime",t),null,1,0,"recorded_wound:"+actualCountedDamage);
 }
 // A fortunate kill is an actual death branch, distinct from a surviving victim's damage receipt.
 static void fortunateKill(Cast c,LivingEntity victim){
  if(victim.isRemoved()||victim.level()!=c.level||!victim.isDeadOrDying())return;
  emit(c,"fortune",Moment.TRIGGER,victim.getUUID(),victim.getBoundingBox().getCenter(),c.caster.getBoundingBox().getCenter(),1,0,"kill_reward");
 }
 static void ferry(Cast c,LivingEntity t,float beforeHealth,Vec3 from,int phase){
  if(t.isRemoved()||t.level()!=c.level||phase<1||phase>2)return;
  double gained=t.getHealth()-beforeHealth;if(gained<=0||!Double.isFinite(gained))return;
  if(from!=null&&!c.level.isLoaded(net.minecraft.core.BlockPos.containing(from)))from=null;
  emit(c,"pulse_ferry",Moment.PULSE,t.getUUID(),LifeOutcomeFrames.anchor("pulse_ferry",t),from,phase,gained,"ferry_phase:"+phase);
 }
 private static void emit(Cast c,String rune,Moment m,UUID who,Vec3 at,Vec3 other,int units,double delta){emit(c,rune,m,who,at,other,units,delta,"");}
 private static void emit(Cast c,String rune,Moment m,UUID who,Vec3 at,Vec3 other,int units,double delta,String detail){
  emit(c.level,c.caster.getUUID(),rune,m,who,at,other,units,delta,detail,LifeOutcomeFrames.event(c,rune,who,at,detail));
 }
 private static void emit(ServerLevel l,String rune,Moment m,UUID who,Vec3 at,Vec3 other,int units,double delta){
  emit(l,rune,m,who,at,other,units,delta,"");
 }
 private static void emit(ServerLevel l,String rune,Moment m,UUID who,Vec3 at,Vec3 other,int units,double delta,String detail){
  emit(l,rune,m,who,at,other,units,delta,detail,LifeOutcomeFrames.event(l,who));
 }
 private static void emit(ServerLevel l,String rune,Moment m,UUID who,Vec3 at,Vec3 other,int units,double delta,String detail,LifeOutcomeFrames.Frame frame){
  emit(l,null,rune,m,who,at,other,units,delta,detail,frame);
 }
 private static void emit(ServerLevel l,UUID source,String rune,Moment m,UUID who,Vec3 at,Vec3 other,int units,double delta,String detail,LifeOutcomeFrames.Frame frame){
  if(!LifeOutcomeRoster.RUNES.contains(rune)||m==null||units<0||!Double.isFinite(delta)||!Double.isFinite(at.lengthSqr())
    ||other!=null&&!Double.isFinite(other.lengthSqr())||!l.isLoaded(BlockPos.containing(at)))return;
  long tick=l.getGameTime();if(!Objects.equals(TICK.get(l),tick)){TICK.put(l,tick);COUNT.put(l,0);}
  int n=COUNT.getOrDefault(l,0);if(n>=128)return;COUNT.put(l,n+1);
  // Resolve only an already-admitted real recipient. REFUSED never gets a success body.
  if(rune.equals("sporebloom")&&m!=Moment.REFUSED&&who!=null&&l.getEntity(who) instanceof LivingEntity body){
   var resolved=SporeOutcomeFrame.resolve(body,frame);at=resolved.anchor();frame=resolved.frame();
  }
  if(!Double.isFinite(at.lengthSqr())||!l.isLoaded(BlockPos.containing(at)))return;
  if(who!=null){var key=new MarkKey(l,rune,who);var mark=MARKS.get(key);if(m==Moment.END||m==Moment.TRIGGER&&mark!=null&&mark.consumedByTrigger())MARKS.remove(key);}
  var event=new Event(l,rune,m,who,at,other,units,delta,tick,detail,frame.normal(),frame.standoff(),source);
  observer.accept(event);LifeOutcomeDelivery.render(event);
 }
}
