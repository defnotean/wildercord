package dev.wildercord.cast;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import static dev.wildercord.cast.NextSignatureNative.*;

/** Four actual Survival-paid support casts, cold-specific resource, willingness and native reopen. */
public final class NextSupportTest implements FabricClientGameTest {
 private static ServerPlayer ally,secondAlly,protectedAlly;
 private static final java.util.concurrent.atomic.AtomicBoolean returnVeto=new java.util.concurrent.atomic.AtomicBoolean(false);
 private static Mob shooter;
 private static Vec3 remembered;
 public void runTest(ClientGameTestContext c){
  TestWorldSave save;boolean jumpWasDown=c.computeOnClient(mc -> mc.options.keyJump.isDown());
  try(var w=c.worldBuilder().create()){
   c.waitTicks(30);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule natural_health_regeneration false");
   w.getServer().runOnServer(s->{var p=player(s);floor(p);p.setHealth(8);ally=guest(p,"EscrowAlly",true);ally.setAbsorptionAmount(0);cast(p,Runes.BEAM,Runes.BLOOD_ESCROW);});c.waitTicks(14);
   w.getServer().runOnServer(s->{var p=player(s);check(p.getHealth()==5 && ally.getAbsorptionAmount()==3,"Actual paid health becomes exactly three allied absorption");
    float before=p.getHealth();apply(new Cast(p),Runes.BLOOD_ESCROW,List.of(ally),ally.position());check(p.getHealth()==before,"Existing ward/admission rest cannot drain health or refill shield");
    ally.removeAllEffects();ally.setAbsorptionAmount(0);ally.setShiftKeyDown(true);apply(new Cast(p),Runes.BLOOD_ESCROW,List.of(ally),ally.position());check(p.getHealth()==before,"Crouching ally refuses without cost");ally.setShiftKeyDown(false);
    p.setHealth(4);var other=guest(p,"EscrowFloor",true);other.setAbsorptionAmount(0);apply(new Cast(p),Runes.BLOOD_ESCROW,List.of(other),other.position());check(p.getHealth()==4 && other.getAbsorptionAmount()==0,"Health survival floor refuses gift");other.discard();
    p.setHealth(20);p.removeAllEffects();pose(p,.5,.5,0);ground(p);p.setTicksFrozen(180);});c.waitTicks(3);
   c.getInput().holdKey(o -> o.keyJump);c.waitTicks(1);
   for(int step=0;step<4 && w.getServer().computeOnServer(s -> player(s).onGround());step++)c.waitTicks(1);
   w.getServer().runOnServer(s->{var p=player(s);check(!p.onGround() && p.getY()>101,"Real client jump is observed airborne on the authoritative server before the paid cast");cast(p,Runes.SELF,Runes.FROST_MOLT);});c.waitTicks(4);
   w.getServer().runOnServer(s->{var p=player(s);check(!p.onGround(),"Frost release/refusal is inspected during the actual client jump");check(SupportSignatures.molts()==0 && p.getTicksFrozen()>0,"Paid airborne Molt refuses without consuming its real cold or creating a guard");});
   c.getInput().releaseKey(o -> o.keyJump);
   for(int step=0;step<22 && !w.getServer().computeOnServer(s -> player(s).onGround());step++)c.waitTicks(1);
   w.getServer().runOnServer(s->{var p=player(s);check(p.onGround(),"Real client jump naturally lands before the grounded paid retry");p.setTicksFrozen(180);shooter=foe(p,5,5);cast(p,Runes.SELF,Runes.FROST_MOLT);});c.waitTicks(14);
   w.getServer().runOnServer(s->{var p=player(s);check(p.getTicksFrozen()==0 && SupportSignatures.molts()==1,"Actual frost exposure pays for one finite guard");
    var arrow=new Arrow(p.level(),shooter,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW),null);
    float before=p.getHealth();boolean landed=p.hurtServer(p.level(),p.level().damageSources().arrow(arrow,shooter),5);
    check(!landed && p.getHealth()==before && SupportSignatures.molts()==0,"One actual projectile damage event consumes frost plate");
    check(p.hurtServer(p.level(),p.level().damageSources().arrow(arrow,shooter),5),"Second projectile is not globally invulnerable");
    p.removeAllEffects();p.setTicksFrozen(0);p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,100,1));
    apply(new Cast(p),Runes.FROST_MOLT,List.of(p),p.position());check(SupportSignatures.molts()==0 && p.hasEffect(MobEffects.SLOWNESS),"Generic Slowness is not a frost payment and is untouched");p.removeAllEffects();p.setHealth(20);
    var mixedCold=guest(p,"MoltMixedControl",true);ground(mixedCold);mixedCold.setTicksFrozen(180);mixedCold.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,100,6));mixedCold.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,100,4));
    apply(new Cast(p),Runes.FROST_MOLT,List.of(mixedCold),mixedCold.position());check(mixedCold.getTicksFrozen()==0 && mixedCold.hasEffect(MobEffects.SLOWNESS) && mixedCold.hasEffect(MobEffects.WEAKNESS),"Real frost resource cannot be used as a generic high-level debuff cleanse");mixedCold.discard();
    ally.snapTo(.5,101,3.5,180,0);ally.setHealth(10);secondAlly=guest(p,"FerryAlly",true);secondAlly.snapTo(2,101,1.5,0,0);secondAlly.setHealth(10);
    cast(p,Runes.SELF,Runes.PULSE_FERRY);});c.waitTicks(54);
   w.getServer().runOnServer(s->{check(ally.getHealth()>10 && secondAlly.getHealth()>10,"Actual parcel visits two wounded allied recipients");
    check(ally.getHealth()-10+secondAlly.getHealth()-10<=6.01,"Actual ferry total remains six, no overheal");
    var p=player(s);var paid=new Cast(p);ally.setHealth(10);secondAlly.setHealth(10);apply(paid,Runes.PULSE_FERRY,List.of(p),p.position());apply(paid.pulse(),Runes.PULSE_FERRY,List.of(p),p.position());});c.waitTicks(54);
   w.getServer().runOnServer(s->{check(ally.getHealth()-10+secondAlly.getHealth()-10<=6.01,"Linked copies share one ferry resource");
    var p=player(s);secondAlly.discard();pose(p,.5,.5,0);ally.snapTo(.5,101,3.5,0,0);ally.setShiftKeyDown(false);remembered=ally.position();cast(p,Runes.BEAM,Runes.LAST_LANTERN);});c.waitTicks(12);
   w.getServer().runOnServer(s->{check(SupportSignatures.lanterns()==1,"Actual paid allied lantern records safe ground");ally.move(MoverType.SELF,new Vec3(2,0,0));});c.waitTicks(5);
   w.getServer().runOnServer(s->{check(ally.position().distanceToSqr(remembered)>1,"Ordinary ally movement does not force return");ally.setShiftKeyDown(true);});c.waitTicks(3);
   w.getServer().runOnServer(s->{check(ally.position().distanceToSqr(remembered)<.05 && SupportSignatures.lanterns()==0,"Fresh voluntary crouch returns once to eligible safe ground");ally.setShiftKeyDown(false);
    var p=player(s);protectedAlly=guest(p,"LanternClaimAlly",true);ground(protectedAlly);remembered=protectedAlly.position();apply(new Cast(p),Runes.LAST_LANTERN,List.of(protectedAlly),remembered);
    check(SupportSignatures.lanterns()==1,"Return fixture records initially permitted body and floor");var body=protectedAlly.blockPosition();net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((level,owner,pos,state,entity)->!returnVeto.get() || !pos.equals(body));
    protectedAlly.move(MoverType.SELF,new Vec3(2,0,0));returnVeto.set(true);protectedAlly.setShiftKeyDown(true);
   });c.waitTicks(3);
   w.getServer().runOnServer(s->{check(protectedAlly.position().distanceToSqr(remembered)>1 && SupportSignatures.lanterns()==0,"A newly protected destination body cell refuses voluntary return despite its still-permitted floor");returnVeto.set(false);protectedAlly.discard();
    var p=player(s);secondAlly=guest(p,"MoltRestart",true);ground(secondAlly);secondAlly.setTicksFrozen(180);apply(new Cast(p),Runes.FROST_MOLT,List.of(secondAlly),secondAlly.position());
    check(SupportSignatures.molts()==1,"Pending guard exists before full restart");});save=w.getWorldSave();
  }finally{returnVeto.set(false);if(jumpWasDown)c.getInput().holdKey(o -> o.keyJump);else c.getInput().releaseKey(o -> o.keyJump);}
  try(var reopened=save.open()){c.waitTicks(20);reopened.getServer().runOnServer(s->check(SupportSignatures.molts()==0 && SupportSignatures.lanterns()==0,"Support watchers clear across full native reopen"));}
 }
}
