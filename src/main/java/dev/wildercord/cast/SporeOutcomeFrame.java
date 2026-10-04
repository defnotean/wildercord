package dev.wildercord.cast;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
/** Read-only actual recipient support, at most two short loaded collision rays.
 * UP normal explicitly means supported fruit; horizontal normal means body spores.
 * This resolves presentation only after actual owner admission, never permissions or gameplay.
 */
final class SporeOutcomeFrame {
 record Resolved(Vec3 anchor,LifeOutcomeFrames.Frame frame){}
 private SporeOutcomeFrame(){}
 static Resolved resolve(LivingEntity body,LifeOutcomeFrames.Frame approached){
  var feet=body.position();var outward=feet.add(approached.normal().scale(approached.standoff()));
  Vec3 hit=support(body,outward);
  if(hit==null&&outward.distanceToSqr(feet)>1e-8)hit=support(body,feet);
  if(hit!=null)return new Resolved(hit.add(0,.03,0),new LifeOutcomeFrames.Frame(new Vec3(0,1,0),0));
  return new Resolved(body.getBoundingBox().getCenter(),approached);
 }
 private static Vec3 support(LivingEntity body,Vec3 feet){
  // The finite slab spans only the immediate real foot support; no downward world search.
  var from=feet.add(0,.10,0);var to=feet.add(0,-.20,0);
  if(!body.level().isLoaded(BlockPos.containing(from))||!body.level().isLoaded(BlockPos.containing(to)))return null;
  var hit=body.level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,body));
  if(hit.getType()!=HitResult.Type.BLOCK||hit.getDirection()!=Direction.UP)return null;
  var at=hit.getLocation();
  return at.y<=feet.y+.001&&at.y>=feet.y-.20?at:null;
 }
}
