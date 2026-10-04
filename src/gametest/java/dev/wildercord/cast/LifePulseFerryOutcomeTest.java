package dev.wildercord.cast;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import static dev.wildercord.cast.NextSignatureNative.*;

/** Genuine paid Life30 gate; requires actual next12 runtime plus observed owner hooks. */
public final class LifePulseFerryOutcomeTest implements FabricClientGameTest {
 private static final List<LifeOwnerEvents.Event> events=new ArrayList<>();
 private static ServerPlayer first,second,refusing;
 @Override public void runTest(ClientGameTestContext c){
  for(String scenario:List.of("two_recipients","one_eligible","removed_second","moved_first")){
   try(var gallery=new LifeOutcomeGallery(c,"life_outcome_ferry_"+scenario);var world=c.worldBuilder().create()){
    gallery.waitTicks(30);var server=world.getServer();server.runCommand("gamerule spawn_mobs false");server.runCommand("gamerule natural_health_regeneration false");
    server.runOnServer(s->{
     var observer=player(s);floor(observer);var p=gallery.actor(s);p.setHealth(p.getMaxHealth());events.clear();LifeOwnerEvents.observe(e->{events.add(e);gallery.observe(e);});
     first=guest(observer,"LifeFerryFirst",true);first.snapTo(.5,101,2.5,0,0);first.setHealth(10);
     second=guest(observer,"LifeFerrySecond",true);second.snapTo(2,101,.5,0,0);second.setHealth(10);
     refusing=guest(observer,"LifeFerryCrouch",true);refusing.snapTo(-1,101,1.5,0,0);refusing.setHealth(8);refusing.setShiftKeyDown(true);
     p.level().getScoreboard().addPlayerToTeam(p.getScoreboardName(),first.getTeam());
     if(scenario.equals("one_eligible"))second.setHealth(second.getMaxHealth());
     cast(p,Runes.SELF,Runes.PULSE_FERRY);
    });
    if(scenario.equals("moved_first")){
     net.minecraft.world.phys.Vec3[] previous={null};gallery.waitTicks(30);
     server.runOnServer(s->{var actual=visits();check(actual.size()==1,"Exactly one actual parcel precedes endpoint movement");var lead=first.getUUID().equals(actual.getFirst().recipient())?first:second;previous[0]=lead.getEyePosition();lead.snapTo(-3,101,-3,0,0);});gallery.waitTicks(26);
     server.runOnServer(s->{var actual=visits();check(actual.size()==2&&actual.get(1).secondary()!=null&&actual.get(1).secondary().distanceTo(previous[0])<.01,"Second actual parcel keeps the first visit snapshot rather than following a moved source entity");});
    }else if(scenario.equals("removed_second")){
     gallery.waitTicks(30);server.runOnServer(s->{
      var visits=visits();check(visits.size()==1,"First actual paid parcel already healed one recipient");
      var healed=visits.getFirst().recipient();if(first.getUUID().equals(healed))second.discard();else first.discard();
     });gallery.waitTicks(26);
     server.runOnServer(s->check(visits().size()==1,"Removed unvisited recipient receives no fabricated second outcome"));
    }else{
     gallery.waitTicks(56);server.runOnServer(s->{
      var visits=visits();
      if(scenario.equals("one_eligible")){
       check(visits.isEmpty()&&first.getHealth()==10,"Fewer than two consenting wounded allies produce no parcel healing or outcome");return;
      }
      check(visits.size()==2&&visits.stream().map(LifeOwnerEvents.Event::recipient).distinct().count()==2,"Two true healing visits have two exact recipient UUIDs");
      check(visits.stream().allMatch(e->e.delta()>0&&e.units()>=1&&e.units()<=2&&e.detail().equals("ferry_phase:"+e.units())),"Actual health gains retain real phase metadata");
      double observed=visits.stream().mapToDouble(LifeOwnerEvents.Event::delta).sum();
      check(observed>0&&observed<=6.01&&Math.abs(observed-(first.getHealth()-10+second.getHealth()-10))<.01,"Observed gains equal actual capped health changes");
      var lead=first.getUUID().equals(visits.getFirst().recipient())?first:second;
      check(visits.get(1).secondary().distanceTo(lead.getEyePosition())<.01,"Second parcel starts at the actual first visited recipient");
      check(refusing.getHealth()==8&&visits.stream().noneMatch(e->refusing.getUUID().equals(e.recipient())),"Crouching wounded ally refuses without a successful observation");
      check(LifeOutcomeVoices.outcome("pulse_ferry")==null&&LifeOutcomeVoices.cue("pulse_ferry").equals("nextsignature_pulse_ferry_cue"),"Existing next-signature cue/impact own the voices; Life adds neither duplicate");
     });
    }
   }finally{LifeOwnerEvents.clear();events.clear();first=second=refusing=null;}
  }
 }
 private static List<LifeOwnerEvents.Event> visits(){return events.stream().filter(e->e.rune().equals("pulse_ferry")&&e.moment()==LifeOwnerEvents.Moment.PULSE).toList();}
}
