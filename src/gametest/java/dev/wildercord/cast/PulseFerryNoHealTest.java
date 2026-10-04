package dev.wildercord.cast;
import dev.wildercord.client.fx.LifeParticle;
import dev.wildercord.client.fx.MagicQuality;
import dev.wildercord.content.LifeOption;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import static dev.wildercord.cast.NextSignatureNative.*;

/** ISOLATED DRAFT: real production Gash heal cancellation + paid Ferry, actual particles and sound listener.
 * Requires only current next12 runtime and standalone finite-positive-gain FX guard; no Life105 hooks.
 */
public final class PulseFerryNoHealTest implements FabricClientGameTest {
 private static ServerPlayer first,second;
 private static final List<String> impactVoices=new CopyOnWriteArrayList<>();
 private static final String IMPACT="wildercord:nextsignature_pulse_ferry_impact";
 @Override public void runTest(ClientGameTestContext c){
  var prior=c.computeOnClient(mc -> MagicQuality.own);
  SoundEventListener listener=(sound,event,range)->{if(sound.getIdentifier().toString().equals(IMPACT))impactVoices.add(IMPACT);};
  c.runOnClient(mc->{MagicQuality.own=MagicQuality.Level.FULL;mc.getSoundManager().addListener(listener);});
  try{
   for(boolean blocked:List.of(true,false)){
    try(var w=c.worldBuilder().create()){
     c.waitTicks(30);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("gamerule natural_health_regeneration false");
     w.getServer().runOnServer(s->{var p=player(s);floor(p);p.setHealth(p.getMaxHealth());
      first=guest(p,"FerryNoHealA",true);first.snapTo(.5,101,3.5,0,0);first.setHealth(10);
      second=guest(p,"FerryNoHealB",true);second.snapTo(2,101,1.5,0,0);second.setHealth(10);
      if(blocked){CraftedRunes.noHeal(first,100);CraftedRunes.noHeal(second,100);check(CraftedRunes.gashed(first)&&CraftedRunes.gashed(second),"Real production Gash state guards both wounded recipients");}
      cast(p,Runes.SELF,Runes.PULSE_FERRY);
     });
     boolean[] parcelSeen={false};impactVoices.clear();
     for(int tick=0;tick<48;tick++){
      c.waitTicks(1);c.runOnClient(mc->{for(var particle:particles(mc.particleEngine))if(particle instanceof LifeParticle){
       var option=(LifeOption)field(particle,LifeParticle.class,"material");
       // Exact actual outcome parcel material; preparation SAP has a different original material palette.
       if(option.style()==LifeOption.SAP && option.color()==0xA6BE78)parcelSeen[0]=true;
      }});
     }
     w.getServer().runOnServer(s->{float gained=first.getHealth()-10+second.getHealth()-10;
      if(blocked){check(first.getHealth()==10 && second.getHealth()==10,"Real paid Ferry reaches eligible recipients but Gash cancels actual healing");check(CraftedRunes.gashed(first)&&CraftedRunes.gashed(second),"Production prevention remains active through both real scheduled beats");}
      else check(gained>0 && gained<=6.01,"Positive control really heals two consenting recipients inside the six-health cap");
     });
     if(blocked){check(!parcelSeen[0],"Zero actual healing sends no success SAP parcel");check(impactVoices.isEmpty(),"Zero actual healing plays no success impact voice");}
     else{check(parcelSeen[0],"Positive actual healing retains its original SAP parcel");check(impactVoices.size()==2,"Exactly two true scheduled healing visits retain their sole impact voices");}
    }finally{first=second=null;impactVoices.clear();}
   }
  }finally{c.runOnClient(mc->{MagicQuality.own=prior;mc.getSoundManager().removeListener(listener);});}
 }
 private static List<Object> particles(ParticleEngine engine){var out=new ArrayList<Object>();for(var group:((Map<?,?>)field(engine,ParticleEngine.class,"particles")).values())out.addAll((Queue<?>)field(group,ParticleGroup.class,"particles"));out.addAll((Queue<?>)field(engine,ParticleEngine.class,"particlesToAdd"));return out;}
 private static Object field(Object target,Class<?> owner,String name){try{var f=owner.getDeclaredField(name);f.setAccessible(true);return f.get(target);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
}
