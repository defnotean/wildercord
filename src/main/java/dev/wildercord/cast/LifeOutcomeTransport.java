package dev.wildercord.cast;

import dev.wildercord.net.LifeOutcomePayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.phys.Vec3;

/** Charges the existing recipient budget for the full authored body before one decorative packet. */
final class LifeOutcomeTransport {
 private LifeOutcomeTransport(){}
 static void send(LifeOwnerEvents.Event e){
  if(Fx.muted()||e.moment()==LifeOwnerEvents.Moment.REFUSED||!LifeOutcomePayload.coordinate(e.anchor())
   ||!e.level().isLoaded(net.minecraft.core.BlockPos.containing(e.anchor())))return;
  Vec3 secondary=e.secondary();
  if(secondary!=null&&(!LifeOutcomePayload.coordinate(secondary)||secondary.distanceToSqr(e.anchor())>64*64
   ||!e.level().isLoaded(net.minecraft.core.BlockPos.containing(secondary))))secondary=null;
  var payload=new LifeOutcomePayload(e.source(),e.level().dimension().identifier().toString(),e.rune(),
   LifeOutcomes.Moment.valueOf(e.moment().name()),e.anchor(),secondary,Math.min(6,e.units()),Math.clamp(e.delta(),-1_000_000,1_000_000),e.tick(),e.normal(),e.standoff());
  // Pure recipe preflight, bounded collection: refuse a future recipe that exceeds the cost contract.
  List<Vec3> points=new ArrayList<>();int[] count={0};boolean[] invalid={false};
  LifeOutcomes.draw(payload.observation(),false,(option,at)->{
   if(++count[0]>LifeOutcomePayload.MAX_PIECES||!LifeOutcomePayload.coordinate(at)){invalid[0]=true;return;}
   points.add(at);
  });
  if(invalid[0]||points.isEmpty()){if(invalid[0])VisualMetrics.dropped();return;}
  for(var player:e.level().players()){
   // Only the known source may defer eye clearance to its real local camera. The server cannot
   // know whether that camera is in first or third person; range and full recipe cost still apply.
   boolean owner=LifeOutcomePayload.own(e.source(),player.getUUID());
   boolean visible=points.stream().anyMatch(at->Fx.inRange(e.level(),player,false,at.x,at.y,at.z)
    &&(owner||at.distanceToSqr(player.getEyePosition())>=1.1*1.1));
   if(!visible||!DecorationBudget.accept(player,count[0]))continue;
   ServerPlayNetworking.send(player,payload);VisualMetrics.recipient();
  }
 }
}
