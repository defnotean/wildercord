package dev.wildercord.client.fx;
import dev.wildercord.cast.CampConcordFx;
import dev.wildercord.content.WildercordSounds;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import java.util.*;
/** Private bounded material playback; no mana/input/claim decision exists on the client. */
public final class CampConcordClient {
 private CampConcordClient(){}
 private record Pending(CampConcordFx.Event e,ClientLevel world,long received){}
 private static final List<Pending> ACTIVE=new ArrayList<>();private static long clock;
 private static final long[] accepted=new long[8];
 public static long accepted(int phase){return phase>=0&&phase<8?accepted[phase]:0;}
 public static void init(){ClientPlayNetworking.registerGlobalReceiver(CampConcordFx.Event.TYPE,(e,ctx)->{
  var mc=ctx.client();if(mc.player==null||mc.level==null||ACTIVE.size()>=32||!mc.level.dimension().identifier().toString().equals(e.world())||!e.source().equals(mc.player.getUUID())&&!e.target().equals(mc.player.getUUID())
   ||Math.abs(mc.level.getGameTime()-e.tick())>40||!mc.level.hasChunkAt(BlockPos.containing(e.from()))||!mc.level.hasChunkAt(BlockPos.containing(e.to())))return;
  ACTIVE.add(new Pending(e,mc.level,clock));accepted[e.phase()]++;
  String sound=switch(e.phase()){case CampConcordFx.ARM->"camp_watch_arm";case CampConcordFx.WARN->"camp_watch_warn";case CampConcordFx.OFFER->"camp_braid_offer";case CampConcordFx.TRANSFER->"camp_braid_transfer";case CampConcordFx.DECLINE->"camp_braid_decline";default->null;};
  if(sound!=null){var event=WildercordSounds.kit(sound);if(event!=null)mc.getSoundManager().play(SimpleSoundInstance.forUI(event,1));}
 });ClientTickEvents.END_CLIENT_TICK.register(CampConcordClient::tick);}
 private static void tick(Minecraft mc){if(mc.player==null||mc.level==null){ACTIVE.clear();return;}if(mc.isPaused())return;int[] spent={0};ACTIVE.removeIf(p->{int age=(int)(clock-p.received);var e=p.e;if(p.world!=mc.level||age<0||age>14||!mc.level.hasChunkAt(BlockPos.containing(e.from()))||!mc.level.hasChunkAt(BlockPos.containing(e.to())))return true;
  if(age%4==0){boolean minimal=(e.source().equals(mc.player.getUUID())?MagicQuality.own:MagicQuality.others)==MagicQuality.Level.MINIMAL;CampConcordForms.outcome(e,age,minimal,(option,at)->{if(spent[0]++<96&&at.distanceToSqr(mc.player.position())<=48*48&&at.distanceToSqr(mc.gameRenderer.mainCamera().position())>=.64)mc.level.addParticle(option,at.x,at.y,at.z,0,0,0);});}return false;});clock++;}
}
