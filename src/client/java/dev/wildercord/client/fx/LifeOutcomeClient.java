package dev.wildercord.client.fx;

import dev.wildercord.cast.LifeOutcomes;
import dev.wildercord.content.LifeOption;
import dev.wildercord.content.MaterialOption;
import dev.wildercord.net.LifeOutcomePayload;
import java.util.ArrayList;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;

/** One admitted actual outcome, rendered once with local quality. No gameplay or deferred owner inference. */
public final class LifeOutcomeClient {
 private record Pending(LifeOutcomePayload event,ClientLevel world,long received){}
 private record Piece(ParticleOptions option,Vec3 at){}
 private static final ArrayList<Pending> PENDING=new ArrayList<>();
 private static long dropped;
 private LifeOutcomeClient(){}
 public static void init(){
  ClientPlayNetworking.registerGlobalReceiver(LifeOutcomePayload.TYPE,(event,context)->{
   var mc=context.client();var world=mc.level;
   if(world==null||mc.player==null||!world.dimension().identifier().toString().equals(event.dimension())
    ||!world.hasChunkAt(BlockPos.containing(event.anchor())))return;
   if(PENDING.size()>=128){dropped++;return;}
   PENDING.add(new Pending(event,world,world.getGameTime()));
  });
  ClientTickEvents.END_CLIENT_TICK.register(LifeOutcomeClient::tick);
 }
 public static long dropped(){return dropped;}
 private static int cap(MagicQuality.Level quality){return quality==MagicQuality.Level.FULL?512:quality==MagicQuality.Level.BALANCED?256:96;}
 private static void tick(Minecraft mc){
  if(mc.level==null||mc.player==null){PENDING.clear();return;}
  if(mc.isPaused())return;
  int ownSpent=0,otherSpent=0,total=0;
  var batch=new ArrayList<>(PENDING);PENDING.clear();
  for(var pending:batch){
   var event=pending.event();long now=mc.level.getGameTime();
   if(pending.world()!=mc.level||now-pending.received()>2||now-pending.received()<0
    ||Math.abs(now-event.tick())>40||!mc.level.dimension().identifier().toString().equals(event.dimension())
    ||!mc.level.hasChunkAt(BlockPos.containing(event.anchor())))continue;
   // A secondary is decorative direction only; stale/unloaded history must not request a chunk.
   Vec3 second=event.secondary();if(second!=null&&!mc.level.hasChunkAt(BlockPos.containing(second)))second=null;
   boolean own=LifeOutcomePayload.own(event.source(),mc.player.getUUID());
   var quality=own?MagicQuality.own:MagicQuality.others;
   var observation=new LifeOutcomes.Observation(event.rune(),event.moment(),event.anchor(),second,event.units(),event.delta(),0,event.normal(),event.standoff());
   var pieces=new ArrayList<Piece>();int[] attempted={0};boolean[] invalid={false};
   LifeOutcomes.draw(observation,quality==MagicQuality.Level.MINIMAL,(option,at)->{
    if(++attempted[0]>LifeOutcomePayload.MAX_PIECES||!LifeOutcomePayload.coordinate(at)
     ||!(option instanceof LifeOption||option instanceof MaterialOption)){invalid[0]=true;return;}
    pieces.add(new Piece(option,at));
   });
   // Whole-body admission retains support ingredients; never randomly thin the selected recipe.
   int count=pieces.size(),spent=own?ownSpent:otherSpent;
   if(invalid[0]||total+count>512||spent+count>cap(quality)){dropped++;continue;}
   total+=count;if(own)ownSpent+=count;else otherSpent+=count;
   Vec3 eye=mc.player.getEyePosition(),camera=mc.gameRenderer.mainCamera().position();
   for(var piece:pieces){var at=piece.at();
    if(!mc.level.hasChunkAt(BlockPos.containing(at))||mc.player.blockPosition().distToCenterSqr(at.x,at.y,at.z)>=32*32
     ||at.distanceToSqr(eye)<1.1*1.1||at.distanceToSqr(camera)<1.1*1.1)continue;
    mc.level.addParticle(piece.option(),at.x,at.y,at.z,0,0,0);
   }
  }
 }
}
