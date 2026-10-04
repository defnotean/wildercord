package dev.wildercord.cast;

import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.level.GameType;
import java.util.*;

/** Negative cases exercise the final damage gate directly, not public spell delivery.
 * The finite Fortune mark and positive Bolt/Harm cases use actual paid public casts.
 */
public final class FortuneDamageAdmissionTest implements FabricClientGameTest {
 private ServerPlayer caster;
 private Pillager target;
 private final List<LifeOwnerEvents.Event> events=new ArrayList<>();
 @Override public void runTest(ClientGameTestContext c){
  try(var world=c.worldBuilder().create()){
   c.waitTicks(30);var server=world.getServer();
   server.runCommand("gamerule spawn_mobs false");
   server.runCommand("gamerule natural_health_regeneration false");
   server.runCommand("fill -12 100 -12 12 100 20 polished_deepslate");
   server.runOnServer(s->{
    var viewer=s.getPlayerList().getPlayers().getFirst();viewer.setGameMode(GameType.SPECTATOR);
    caster=NextSignatureNative.guest(viewer,"FortuneGate",false);
    caster.snapTo(.5,101,.5,0,0);NextSignatureNative.ground(caster);
    caster.setAttached(WildercordAttachments.INNATE,Runes.FORTUNE.id());
    LifeOwnerEvents.observe(events::add);target=foe(caster);
    // Establish this fixture's unmarked final amount. Its callback denies before any damage.
    double[] base={Double.NaN};
    Effects.hurtCapped(new Cast(caster),target,s.overworld().damageSources().indirectMagic(caster,caster),2,a->{base[0]=a;return 0;});
    check(Double.isFinite(base[0])&&base[0]>0,"Unmarked real final gate was reached");
    unmarked=base[0];
    NextSignatureNative.cast(caster,Runes.SELF,Runes.FORTUNE);
   });c.waitTicks(8);
   server.runOnServer(s->{
    check(events.stream().anyMatch(e->e.rune().equals("fortune")&&e.moment()==LifeOwnerEvents.Moment.APPLY),"Paid owned Fortune really admitted its finite mark");
    events.clear();s.overworld().getRandom().setSeed(90210);
    int[] lucky={0},gates={0};float before=target.getHealth();
    for(int i=0;i<64;i++){
     Effects.hurtCapped(new Cast(caster),target,s.overworld().damageSources().indirectMagic(caster,caster),2,a->{
      gates[0]++;if(a>unmarked*1.9)lucky[0]++;return 0;
     });
    }
    check(gates[0]==64&&lucky[0]>0,"All bounded gate calls denied AFTER at least one genuine lucky roll: "+lucky[0]);
    check(target.getHealth()==before,"Denied final admission changes no real target health");
    check(events.stream().noneMatch(FortuneDamageAdmissionTest::trigger),"Denied lucky damage emits no successful Fortune TRIGGER");
    target.discard();events.clear();s.overworld().getRandom().setSeed(81273);
   });
   // Positive control: real projectile preparation, flight, collision and damage.
   // Fresh victims avoid previous Exposed marks or wounded-target modifiers.
   boolean accepted=false;
   for(int attempt=0;attempt<16&&!accepted;attempt++){
    if(attempt%4==0){server.runOnServer(s->NextSignatureNative.cast(caster,Runes.SELF,Runes.FORTUNE));c.waitTicks(8);}
    server.runOnServer(s->{events.clear();target=foe(caster);positiveBefore=target.getHealth();
     NextSignatureNative.cast(caster,Runes.BOLT,Runes.HARM);
    });c.waitTicks(16);
    accepted=server.computeOnServer(s->{
     float taken=positiveBefore-target.getHealth();check(taken>0,"Paid Bolt/Harm really reached and damaged fresh hostile");
     var procs=events.stream().filter(FortuneDamageAdmissionTest::trigger).toList();
     for(var e:procs)check(e.recipient().equals(target.getUUID())&&e.source().equals(caster.getUUID())&&e.delta()<0&&Math.abs(e.delta()+taken)<.001,"Accepted Fortune TRIGGER derives actual victim loss and caster authority");
     boolean result=!procs.isEmpty();target.discard();return result;
    });
   }
   check(accepted,"Bounded seeded paid Bolt/Harm casts include a real accepted lucky outcome");
   // Reward acceptance is the actual native death branch, not a fabricated XP entity
   // or a claim that Event.delta is an XP quantity. Observe synchronously after award.
   server.runOnServer(s->NextSignatureNative.cast(caster,Runes.SELF,Runes.FORTUNE));c.waitTicks(8);
   server.runOnServer(s->{
    s.overworld().getRandom().setSeed(37041);int[] rewards={0};
    LifeOwnerEvents.observe(e->{
     events.add(e);if(!trigger(e)||!e.detail().equals("kill_reward"))return;
     check(target!=null&&target.isDeadOrDying()&&!target.isRemoved()&&target.level()==caster.level(),"Reward observes an actual dead native victim in winner's world");
     check(e.recipient().equals(target.getUUID())&&e.source().equals(caster.getUUID())&&e.units()==1&&e.delta()==0,"Death receipt identifies real victim/winner and event count, not numeric XP");
     check(e.anchor().distanceTo(target.getBoundingBox().getCenter())<.001,"Death receipt preserves actual corpse anchor");
     var toward=caster.getBoundingBox().getCenter().subtract(e.anchor()).multiply(1,0,1).normalize();
     check(Math.abs(e.normal().length()-1)<.001&&e.normal().dot(toward)>.99&&e.standoff()>0,"Actual corpse outward frame approaches its real winner");
     check(!target.getBoundingBox().contains(e.anchor().add(e.normal().scale(e.standoff()))),"Death material surface lies outside actual opaque corpse bounds");
     var orbs=s.overworld().getEntitiesOfClass(ExperienceOrb.class,target.getBoundingBox().inflate(1.5),orb->!orb.isRemoved());
     check(!orbs.isEmpty(),"Actual reward orb exists immediately when real kill_reward fires");rewards[0]++;
    });
    for(int i=0;i<64&&rewards[0]==0;i++){
     // Remove prior genuine rewards between attempts so an old orb cannot satisfy
     // the synchronous assertion. This fixture never creates or awards XP itself.
     s.overworld().getEntitiesOfClass(ExperienceOrb.class,new net.minecraft.world.phys.AABB(-2,99,5,3,106,12)).forEach(Entity::discard);
     target=foe(caster);target.setHealth(1);target.damageCooldownTime=0;
     target.hurtServer(s.overworld(),s.overworld().damageSources().playerAttack(caster),4);
     check(target.isDeadOrDying(),"Ordinary actual player attack really kills each bounded hostile");target.discard();
    }
    check(rewards[0]>0,"Bounded seeded genuine kills enter Fortune's real XP reward branch");
   });
  }finally{LifeOwnerEvents.clear();events.clear();caster=null;target=null;}
 }
 private double unmarked;
 private float positiveBefore;
 private static boolean trigger(LifeOwnerEvents.Event e){return e.rune().equals("fortune")&&e.moment()==LifeOwnerEvents.Moment.TRIGGER;}
 private static Pillager foe(ServerPlayer p){var t=EntityTypes.PILLAGER.create(p.level(),EntitySpawnReason.COMMAND);check(t!=null,"Real hostile fixture");t.setNoAi(true);t.snapTo(.5,101,8.5,180,0);p.level().addFreshEntity(t);return t;}
 private static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
}
