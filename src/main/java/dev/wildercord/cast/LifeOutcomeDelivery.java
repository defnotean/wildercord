package dev.wildercord.cast;
import dev.wildercord.cast.feel.Feels;
import java.util.*;
/** Consumer for actual owner events. Source-aware event transport
 * retains recipient decoration budget, first-person clearance and world range.
 * Particle drift supplies the short motion; this never invents delayed gameplay events.
 */
final class LifeOutcomeDelivery {
 private record Voice(net.minecraft.server.level.ServerLevel level,String rune,UUID recipient) {}
 private static final Map<Voice,Long> VOICES=new HashMap<>();
 static void render(LifeOwnerEvents.Event e){
  if(Fx.muted()||e.moment()==LifeOwnerEvents.Moment.REFUSED)return;
  LifeOutcomeTransport.send(e);
  String sound=LifeOutcomeVoices.outcome(e.rune());if(sound==null)return; // Mercy retains its single FieldFusionFx landing.
  var key=new Voice(e.level(),e.rune(),e.recipient());long now=e.tick();int delay=e.moment()==LifeOwnerEvents.Moment.PULSE?20:5;
  Long last=VOICES.get(key);if(last!=null&&now-last<delay)return;
  if(VOICES.size()>=128)prune(e.level().getServer());
  if(VOICES.size()>=128&&!VOICES.containsKey(key))return;
  VOICES.put(key,now);Feels.sound(e.level(),e.anchor(),sound,e.moment()==LifeOwnerEvents.Moment.PULSE?.12F:.38F,1);
 }
 static void prune(net.minecraft.server.MinecraftServer server){
  // Use each voice owner's world clock; a full old dimension cannot starve another dimension.
  VOICES.entrySet().removeIf(v->{var l=v.getKey().level();return server.getLevel(l.dimension())!=l||l.getGameTime()-v.getValue()>40;});
 }
 static void clear(){VOICES.clear();}
}
