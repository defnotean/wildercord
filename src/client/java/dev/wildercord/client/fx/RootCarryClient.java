package dev.wildercord.client.fx;
import dev.wildercord.wildlife.RootCarryFx;
import dev.wildercord.wildlife.RootCarryPictures;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import java.util.ArrayList;
/** Three finite admitted transfer beats, actual source own/others quality; no deferred gameplay. */
public final class RootCarryClient {
 private record Pending(RootCarryFx.Event event,ClientLevel world,long received){}
 private static final ArrayList<Pending> ACTIVE=new ArrayList<>();
 private static long playbackTick;
 private RootCarryClient(){}
 public static void init(){ClientPlayNetworking.registerGlobalReceiver(RootCarryFx.Event.TYPE,(e,context)->{
  var mc=context.client();if(mc.level==null||mc.player==null||ACTIVE.size()>=32||!mc.level.dimension().identifier().toString().equals(e.dimension())||!mc.level.hasChunkAt(e.from())||!mc.level.hasChunkAt(e.to())||Math.abs(mc.level.getGameTime()-e.tick())>40)return;ACTIVE.add(new Pending(e,mc.level,playbackTick));
 });ClientTickEvents.END_CLIENT_TICK.register(RootCarryClient::tick);}
 private static void tick(Minecraft mc){
  if(mc.level==null||mc.player==null){ACTIVE.clear();return;}if(mc.isPaused())return;int[] spent={0};
  ACTIVE.removeIf(p->{long age=playbackTick-p.received();var e=p.event();if(p.world()!=mc.level||age<0||age>12||!mc.level.hasChunkAt(e.from())||!mc.level.hasChunkAt(e.to()))return true;
   if(age%6==0&&(e.moved()||age==0)){boolean minimal=(e.source().equals(mc.player.getUUID())?MagicQuality.own:MagicQuality.others)==MagicQuality.Level.MINIMAL;int count=minimal?2:6;if(spent[0]+count>96)return false;spent[0]+=count;
    RootCarryPictures.transfer(e,(int)(age/6),minimal,(option,at)->{if(at.distanceToSqr(mc.player.position())<=48*48&&at.distanceToSqr(mc.player.getEyePosition())>=1.21&&at.distanceToSqr(mc.gameRenderer.mainCamera().position())>=1.21)mc.level.addParticle(option,at.x,at.y,at.z,0,0,0);});
   }return false;
  });playbackTick++;
 }
}
