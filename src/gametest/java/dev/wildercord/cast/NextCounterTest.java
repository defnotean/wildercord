package dev.wildercord.cast;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import static dev.wildercord.cast.NextSignatureNative.*;

/** Four real paid casts, actual projectile/health/mana outcomes, shared linked caps and reopen. */
public final class NextCounterTest implements FabricClientGameTest {
 private static Mob shooter,stationary,moving,walking;
 private static ServerPlayer rival,rearAlly;
 private static Arrow first,second;
 private static float firstHealth,enemyAfterPayment,walkingStartHealth;
 private static Vec3 walkingOrigin;
 private static Cast continued;
 private static RuneBolt turned;
 private static ServerPlayer parrier,reflectionReceiver;
 private static Mob cancelledDuringDamage;
 private static int cancellationMode;
 private static boolean cancellationObserved,resurrectionObserved;
 private static ServerPlayer cancellationOwner;
 private static float cancellationAmount,cancellationBefore,cancellationAfter;
 private static boolean cancellationAfterDamage;
 public void runTest(ClientGameTestContext c){
  net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((target,source,amount)->{
   if(target!=cancelledDuringDamage || cancellationMode==0)return true;
   // This fixture cancels the paid movement strike, not an unrelated admission event.
   if(!Dungeons.spellLanding() || source.getEntity()!=cancellationOwner
      || !source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC)
      || !Float.isFinite(amount) || amount<=0){
    System.out.println("Ledger cancellation ignored admission: source="+source.getMsgId()+" amount="+amount+" spell="+Dungeons.spellLanding()+" owner="+source.getEntity()+" health="+target.getHealth()+" absorption="+target.getAbsorptionAmount()+" cooldown="+target.damageCooldownTime);return true;
   }
   int mode=cancellationMode;cancellationMode=0;cancellationObserved=true;
   cancellationAmount=amount;cancellationBefore=target.getHealth();
   System.out.println("Ledger cancellation admitted: mode="+mode+" amount="+amount+" health="+target.getHealth()+" absorption="+target.getAbsorptionAmount()+" cooldown="+target.damageCooldownTime+" position="+target.position());
   if(mode==1){
    var before=target.position();
    boolean moved=target.teleportTo((net.minecraft.server.level.ServerLevel)target.level(),before.x+.2,before.y,before.z,Set.<Relative>of(),target.getYRot(),target.getXRot(),false);
    check(moved && Math.abs(target.getX()-before.x-.2)<.0001,"Actual cancellation callback completes its small same-world teleport");
    System.out.println("Ledger cancellation after teleport: health="+target.getHealth()+" absorption="+target.getAbsorptionAmount()+" cooldown="+target.damageCooldownTime+" active="+CounterSignatures.active());return true;
   }
   target.discard();return false;
  });
  net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DAMAGE.register((target,source,base,taken,blocked)->{
   if(target!=cancelledDuringDamage || !cancellationObserved || source.getEntity()!=cancellationOwner
      || !source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC))return;
   cancellationAfterDamage=true;cancellationAfter=target.getHealth();
   System.out.println("Ledger cancellation actual return: base="+base+" taken="+taken+" blocked="+blocked+" health="+target.getHealth()+" absorption="+target.getAbsorptionAmount()+" cooldown="+target.damageCooldownTime+" active="+CounterSignatures.active());
  });
  net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server->{
   if(cancellationObserved && cancelledDuringDamage!=null && CounterSignatures.active()!=0)resurrectionObserved=true;
  });
  TestWorldSave save;
  try(var w=c.worldBuilder().create()){
   c.waitTicks(30);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule natural_health_regeneration false");
   w.getServer().runOnServer(s->{var p=player(s);floor(p);p.setHealth(20);
    var boss=EntityTypes.WITHER.create(p.level(),EntitySpawnReason.COMMAND);check(boss!=null,"Counter safety boss created");boss.setNoAi(true);boss.snapTo(6,101,6,0,0);p.level().addFreshEntity(boss);
    apply(new Cast(p),Runes.QUIETUS,List.of(boss),boss.position());apply(new Cast(p),Runes.RED_LEDGER,List.of(boss),boss.position());check(CounterSignatures.active()==0,"Boss cannot enter projectile escrow or movement ledger");boss.discard();
    var immune=counterFoe(p,6,6);immune.setPermanentlyInvulnerable(true);apply(new Cast(p),Runes.QUIETUS,List.of(immune),immune.position());apply(new Cast(p),Runes.RED_LEDGER,List.of(immune),immune.position());check(CounterSignatures.active()==0,"Permanent immunity refuses hostile escrow and movement ledger");immune.discard();
    shooter=counterFoe(p,5,5);cast(p,Runes.SELF,Runes.NULLCATCH);});c.waitTicks(12);
   w.getServer().runOnServer(s->{var p=player(s);firstHealth=p.getHealth();first=arrow(p,shooter,true);});c.waitTicks(4);
   w.getServer().runOnServer(s->{var p=player(s);check(!first.isAlive() && p.getHealth()==firstHealth,"Actual paid front capture removes one hostile arrow without a hit");second=arrow(p,shooter,true);});c.waitTicks(6);
   w.getServer().runOnServer(s->{var p=player(s);check(p.getHealth()<firstHealth,"Second hostile arrow crosses spent capture screen");p.setHealth(20);pose(p,.5,.5,0);
    // Prove the setup really increases ordinary damage before asserting the signature's final cap.
    var bonusProbe=counterFoe(p,7,7);var setup=new Cast(p).damagePrice(100);Effects.hex(setup,bonusProbe,200,3,false);
    Effects.hurt(setup,bonusProbe,p.level().damageSources().indirectMagic(p,p),2);
    check(20-bonusProbe.getHealth()>2.5,"Actual hex multiplier increases uncapped ordinary magic damage");bonusProbe.discard();
    stationary=counterFoe(p,.5,4.5);Effects.hex(new Cast(p),stationary,200,3,false);cast(p,Runes.BEAM,Runes.SECOND_BELL);});c.waitTicks(50);
   w.getServer().runOnServer(s->{check(stationary.getHealth()<20 && 20-stationary.getHealth()<=4.01,"Two paid stationary bell beats have target cap four after bonuses");stationary.discard();
    var p=player(s);pose(p,.5,.5,0);moving=counterFoe(p,.5,4.5);cast(p,Runes.BEAM,Runes.SECOND_BELL);});c.waitTicks(28);
   w.getServer().runOnServer(s->{firstHealth=moving.getHealth();check(firstHealth<20,"Actual first bell landed");moving.move(MoverType.SELF,new Vec3(2,0,0));});c.waitTicks(24);
   w.getServer().runOnServer(s->{check(moving.getHealth()==firstHealth,"Ordinary movement counters second bell");moving.discard();var p=player(s);pose(p,.5,.5,0);walking=counterFoe(p,.5,4.5);walkingOrigin=walking.position();walkingStartHealth=walking.getHealth();Effects.hex(new Cast(p),walking,200,3,false);cast(p,Runes.BEAM,Runes.RED_LEDGER);});c.waitTicks(12);
   String inputs=c.computeOnClient(mc->"up="+mc.options.keyUp.isDown()+" down="+mc.options.keyDown.isDown()+" left="+mc.options.keyLeft.isDown()+" right="+mc.options.keyRight.isDown()+" jump="+mc.options.keyJump.isDown()+" sneak="+mc.options.keyShift.isDown()+" attack="+mc.options.keyAttack.isDown()+" use="+mc.options.keyUse.isDown());
   w.getServer().runOnServer(s->{var p=player(s);var displacement=walking.position().subtract(walkingOrigin);var details=" beforeHealth="+walkingStartHealth+" health="+walking.getHealth()+" max="+walking.getMaxHealth()+" absorption="+walking.getAbsorptionAmount()+" beforeBody="+walkingOrigin+" endBody="+walking.position()+" horizontalDisplacement="+displacement.multiply(1,0,1).length()+" velocity="+walking.getDeltaMovement()+" noAI="+walking.isNoAi()+" onGround="+walking.onGround()+" registered="+(p.level().getEntity(walking.getUUID())==walking)+" runebound="+Runebound.spellOf(walking)+" active="+CounterSignatures.active()+" casterBody="+p.position()+" casterVelocity="+p.getDeltaMovement()+" clientInputs="+inputs;System.out.println("LEDGER_STILLNESS"+details);check(walking.getHealth()==20,"Standing still avoids paid movement ledger"+details);});
   for(int gate=0;gate<3;gate++){w.getServer().runOnServer(s->walking.move(MoverType.SELF,new Vec3(1.1,0,0)));c.waitTicks(10);}
   w.getServer().runOnServer(s->{check(20-walking.getHealth()>0 && 20-walking.getHealth()<=6.01,"Three ordinary movement gates remain within six post-bonus damage");walking.discard();
    var p=player(s);pose(p,.5,.5,0);rival=guest(p,"QuietusRival",false);cast(p,Runes.BEAM,Runes.QUIETUS);});c.waitTicks(12);
   w.getServer().runOnServer(s->{cast(rival,Runes.BOLT,Runes.HARM);enemyAfterPayment=Spellbooks.mana(rival);});c.waitTicks(10);
   w.getServer().runOnServer(s->{check(Spellbooks.mana(rival)<=enemyAfterPayment-7.9 && Spellbooks.mana(rival)>=enemyAfterPayment-8.1,"Actual newly paid enemy magic projectile has bounded eight-mana tax");
    var p=player(s);p.level().getEntitiesOfClass(RuneBolt.class,p.getBoundingBox().inflate(30)).forEach(Entity::discard);rival.discard();
    // Same real damage seam with absorption and linked copies: spent shield contributions are not refunded.
    var a=counterFoe(p,3,4);var b=counterFoe(p,5,4);a.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.ABSORPTION,200,0));a.setAbsorptionAmount(4);continued=new Cast(p).damagePrice(100);Effects.hex(continued,a,200,3,false);Effects.hex(continued,b,200,3,false);
    apply(continued,Runes.SECOND_BELL,List.of(a,b),a.position());apply(continued.pulse(),Runes.SECOND_BELL,List.of(a,b),a.position());
    stationary=a;moving=b;});c.waitTicks(48);
   w.getServer().runOnServer(s->{System.out.println("Bell overlap: health="+stationary.getHealth()+" absorption="+stationary.getAbsorptionAmount()+" other="+moving.getHealth()+" budget="+NextSignaturePayments.of(continued).left("second_bell_damage",8));
    check(stationary.getHealth()==20 && stationary.getAbsorptionAmount()==0,"Actual absorption consumes four while linked contributions spend their shared allowance without health loss");
    check(20-moving.getHealth()<=4.01,"Linked copies cannot refill target damage");
    check(NextSignaturePayments.of(continued).left("second_bell_damage",8)==0,"Both defended and landed contributions spend shared global eight");
    stationary.discard();moving.discard();var p=player(s);walking=counterFoe(p,3,4);continued=new Cast(p).damagePrice(100);
    apply(continued,Runes.RED_LEDGER,List.of(walking),walking.position());walking.teleportTo(p.level(),4,101,4,Set.<Relative>of(),0,0,false);});c.waitTicks(10);
   w.getServer().runOnServer(s->{check(walking.getHealth()==20,"Actual small same-world teleport releases ledger before movement cuts");walking.move(MoverType.SELF,new Vec3(1.2,0,0));});c.waitTicks(10);
   w.getServer().runOnServer(s->{check(walking.getHealth()==20,"Released teleport ledger cannot resume after walking");walking.discard();});
   synchronousLedgerCancellation(c,w.getServer());
   reflectedAfterQuarryDeath(c,w.getServer());
   boundedProjectilePools(c,w.getServer());
   w.getServer().runOnServer(s->{var p=player(s);rearAlly=guest(p,"RearCounterAlly",true);rearAlly.snapTo(8,101,8,0,0);rearAlly.setHealth(20);apply(new Cast(p),Runes.NULLCATCH,List.of(rearAlly),rearAlly.position());});c.waitTicks(8);
   w.getServer().runOnServer(s->{firstHealth=rearAlly.getHealth();second=arrow(rearAlly,shooter,false);});c.waitTicks(5);
   w.getServer().runOnServer(s->{check(rearAlly.getHealth()<firstHealth && CounterSignatures.active()==1,"Rear hostile arrow is not intercepted by front-only active capture window");});save=w.getWorldSave();
  }
  try(var reopened=save.open()){c.waitTicks(20);reopened.getServer().runOnServer(s->check(CounterSignatures.active()==0,"Counter maps clear across full native reopen"));}
 }
 /** Named BEFORE actual entity admission: controlled health fixtures must not randomly become Runebound. */
 private static Mob counterFoe(ServerPlayer p,double x,double z){
  var entity=EntityTypes.HUSK.create(p.level(),EntitySpawnReason.COMMAND);
  check(entity instanceof Mob,"Counter native Husk factory");var mob=(Mob)entity;
  mob.setNoAi(true);mob.setCustomName(net.minecraft.network.chat.Component.literal("Counter practice target"));
  mob.snapTo(x,101,z,0,0);check(p.level().addFreshEntity(mob),"Actual counter fixture admitted");
  check(mob.getHealth()==20&&mob.getMaxHealth()==20&&mob.getAbsorptionAmount()==0,
   "Counter target retains genuine twenty-health baseline after entity-load callbacks: health="+mob.getHealth()+" max="+mob.getMaxHealth()+" absorption="+mob.getAbsorptionAmount()+" runebound="+Runebound.spellOf(mob)+" body="+mob.position());
  return mob;
 }
 private static void boundedProjectilePools(ClientGameTestContext c,net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server){
  var arrows=new ArrayList<Arrow>();var oldBolts=new ArrayList<RuneBolt>();
  server.runOnServer(s->{var p=player(s);pose(p,.5,.5,0);
   for(int i=0;i<256;i++){
    var a=new Arrow(p.level(),p,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW),null);
    a.setPos(p.getEyePosition().add(.2+(i%8)*.04,0,.2+(i/8)*.04));a.setNoGravity(true);a.setDeltaMovement(Vec3.ZERO);
    check(p.level().addFreshEntity(a),"Native crowd arrow admitted");arrows.add(a);
    if(i==64){var batch=BoundedCounterCandidates.nearby(new Cast(p),p);check(batch.enumerated()==65 && batch.saturated() && batch.candidates().isEmpty(),"65 native arrows reach early-abort query cap");}
   }
   for(int repeat=0;repeat<32;repeat++){var batch=BoundedCounterCandidates.nearby(new Cast(p),p);check(batch.enumerated()==65 && batch.saturated() && batch.candidates().isEmpty(),"256 native arrows still admit65 per query");}
   cast(p,Runes.SELF,Runes.NULLCATCH);
   var refused=new Cast(p);apply(refused,Runes.NULLCATCH,List.of(p),p.position());
   check(NextSignaturePayments.of(refused).target("nullcatch",p.getUUID(),4),"Saturated refusal leaves shared target allowance unspent");
  });c.waitTicks(12);
  server.runOnServer(s->{var p=player(s);check(CounterSignatures.active()==0 && !Statuses.claimed(p,"nullcatch",NextSignatureRules.REST),"Actual paid crowded opening refuses without rest or retained screen");
   check(arrows.stream().allMatch(Entity::isAlive),"Crowded refusal discards no native projectile");arrows.forEach(Entity::discard);arrows.clear();
   cast(p,Runes.SELF,Runes.NULLCATCH);
  });c.waitTicks(8);
  server.runOnServer(s->{var p=player(s);check(CounterSignatures.active()==1,"Refused opening can immediately make a later uncrowded paid screen");
   for(int i=0;i<65;i++){var a=new Arrow(p.level(),p,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW),null);a.setPos(p.getEyePosition().add(.3,0,.3+i*.01));a.setDeltaMovement(Vec3.ZERO);a.setNoGravity(true);p.level().addFreshEntity(a);arrows.add(a);}
  });c.waitTicks(2);
  server.runOnServer(s->{check(CounterSignatures.active()==0 && arrows.stream().allMatch(Entity::isAlive),"Existing actual paid screen releases on later saturation without deleting projectiles");arrows.forEach(Entity::discard);arrows.clear();
   var p=player(s);rival=guest(p,"CrowdQuietus",false);var group=SpellCompiler.compile(List.of(Runes.BOLT,Runes.HARM)).root().groups.getFirst();
   for(int i=0;i<13;i++){var bolt=RuneBolt.launch(new Cast(rival),group,null,rival.getEyePosition().add((i-6)*.04,0,1.6),Vec3.ZERO,false);check(bolt!=null,"Native pre-existing magic bolt created");oldBolts.add(bolt);}
   var batch=BoundedCounterCandidates.nearby(new Cast(p),rival);check(batch.enumerated()==13 && batch.completeSnapshot().size()==13 && batch.candidates().size()==12,"Complete13 magic provenance is separate from nearest12 handling");
   cast(p,Runes.BEAM,Runes.QUIETUS);
  });c.waitTicks(8);
  server.runOnServer(s->{check(CounterSignatures.active()==1,"Actual paid Quietus opens over13 existing magic bolts");
   var originals=oldBolts.stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet());
   check(screenExisting(rival.getUUID()).containsAll(originals),"Real admitted screen snapshots all13 native pre-existing bolts");
   check(oldBolts.stream().allMatch(Entity::isAlive),"Pre-existing magic is not escrowed by paid screen");oldBolts.forEach(Entity::discard);oldBolts.clear();
   cast(rival,Runes.BOLT,Runes.HARM);enemyAfterPayment=Spellbooks.mana(rival);
  });c.waitTicks(10);
  server.runOnServer(s->{check(CounterSignatures.active()==0 && Spellbooks.mana(rival)<=enemyAfterPayment-7.9 && Spellbooks.mana(rival)>=enemyAfterPayment-8.1,"Same paid screen captures genuinely later paid magic once with eight-mana tax");
   rival.level().getEntitiesOfClass(RuneBolt.class,rival.getBoundingBox().inflate(32)).forEach(Entity::discard);rival.discard();
  });
 }
 @SuppressWarnings("unchecked") private static Set<UUID> screenExisting(UUID target){
  try{var field=CounterSignatures.class.getDeclaredField("SCREENS");field.setAccessible(true);var screen=((Map<UUID,?>)field.get(null)).get(target);check(screen!=null,"Actual screen record retained");var accessor=screen.getClass().getDeclaredMethod("existing");accessor.setAccessible(true);return (Set<UUID>)accessor.invoke(screen);}
  catch(ReflectiveOperationException e){throw new AssertionError("Native screen provenance inspection",e);}
 }
 private static void synchronousLedgerCancellation(ClientGameTestContext c,net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server){
  try{
   for(int mode:List.of(1,2)){
    server.runOnServer(s->{var p=player(s);pose(p,.5,.5,0);cancelledDuringDamage=counterFoe(p,.5,4.5);cancellationObserved=false;resurrectionObserved=false;cancellationMode=0;cancellationOwner=p;cancellationAfterDamage=false;cancellationAmount=0;cancellationBefore=0;cancellationAfter=0;cast(p,Runes.BEAM,Runes.RED_LEDGER);});c.waitTicks(12);
    server.runOnServer(s->{check(CounterSignatures.active()==1,"Paid ledger is active before synchronous cancellation fixture");
     check(cancelledDuringDamage.getHealth()==20 && cancelledDuringDamage.getAbsorptionAmount()==0,"Cancellation target reaches its movement gate at full health without absorption");
     cancellationMode=mode;cancelledDuringDamage.move(MoverType.SELF,new Vec3(1.1,0,0));});c.waitTicks(4);
    float after=server.computeOnServer(s->{check(cancellationObserved,"Actual ledger damage invokes native cancellation callback");check(!resurrectionObserved,"No cancelled record is transiently resurrected at the actual damage tick boundary");check(CounterSignatures.active()==0,"Synchronous "+(mode==1?"teleport":"removal")+" cannot be overwritten by stale post-damage ledger continuation");if(mode==1){System.out.println("Ledger cancellation final: admitted="+cancellationAmount+" before="+cancellationBefore+" afterEvent="+cancellationAfter+" event="+cancellationAfterDamage+" health="+cancelledDuringDamage.getHealth()+" absorption="+cancelledDuringDamage.getAbsorptionAmount()+" cooldown="+cancelledDuringDamage.damageCooldownTime);check(cancelledDuringDamage.getHealth()<20,"Actual damage is allowed despite synchronous small teleport");cancelledDuringDamage.move(MoverType.SELF,new Vec3(1.1,0,0));}else check(cancelledDuringDamage.isRemoved(),"Actual damage callback removed its target");return cancelledDuringDamage.getHealth();});c.waitTicks(12);
    server.runOnServer(s->{check(CounterSignatures.active()==0 && cancelledDuringDamage.getHealth()==after,"Cancelled ledger has no resumed movement damage or retained record");cancelledDuringDamage.discard();});
   }
  }finally{cancellationMode=0;cancellationObserved=false;resurrectionObserved=false;cancelledDuringDamage=null;cancellationOwner=null;cancellationAfterDamage=false;cancellationAmount=0;cancellationBefore=0;cancellationAfter=0;}
 }
 private static void reflectedAfterQuarryDeath(ClientGameTestContext c,net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext server){
  // Actual paid enemy bolt meets an actual paid Shield and uses RuneBolt's ordinary parry path.
  server.runOnServer(s->{turned=null;var p=player(s);pose(p,.5,.5,0);p.setHealth(20);parrier=p;rival=guest(p,"ReflectionQuarry",false);rival.snapTo(.5,101,12.5,180,0);cast(rival,Runes.BOLT,Runes.HARM);});c.waitTicks(3);
  server.runOnServer(s->cast(parrier,Runes.SELF,Runes.SHIELD));
  for(int i=0;i<10 && turned==null;i++){c.waitTicks(1);server.runOnServer(s->{turned=parrier.level().getEntitiesOfClass(RuneBolt.class,parrier.getBoundingBox().inflate(32)).stream().filter(RuneBolt::isReflected).findFirst().orElse(null);});}
  server.runOnServer(s->{check(turned!=null && turned.isAlive() && turned.getOwner()==parrier,"Actual paid Shield parries paid enemy magic and transfers owner");rival.setHealth(0);});c.waitTicks(1);
  server.runOnServer(s->{check(turned.isAlive() && turned.isReflected(),"Reflection provenance survives original quarry death and ordinary hunt tick");
   reflectionReceiver=guest(parrier,"ReflReceiver",false);reflectionReceiver.snapTo(8,101,8,180,0);cast(reflectionReceiver,Runes.SELF,Runes.NULLCATCH);});c.waitTicks(12);
  server.runOnServer(s->{check(turned.isAlive(),"Reflected projectile is retained before capture approach");
   // Bring that same real reflected entity into the front cone after its old quarry has died.
   turned.setPos(reflectionReceiver.getEyePosition().add(0,0,-2.1));turned.setDeltaMovement(0,0,.15);
   firstHealth=reflectionReceiver.getHealth();});c.waitTicks(2);
  server.runOnServer(s->{check(turned.isAlive() && turned.isReflected() && CounterSignatures.active()==1,"An open front Nullcatch refuses the real reflected bolt after quarry death");
   check(reflectionReceiver.getHealth()==firstHealth,"Refusal assertion occurs before projectile contact");turned.discard();rival.discard();reflectionReceiver.discard();turned=null;});c.waitTicks(2);
 }
}
