package dev.wildercord.wildlife;

import dev.wildercord.cast.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Conservative compatibility query for a read-only field tool, not a standard read-claims API. */
public final class TidewardReadAdmission {
 private TidewardReadAdmission(){}
 private record ReadKey(java.util.UUID player,net.minecraft.core.GlobalPos cell){}
 private static final ThreadLocal<java.util.Set<ReadKey>> ACTIVE=ThreadLocal.withInitial(java.util.HashSet::new);
 public static boolean actor(ServerPlayer p){return p.isAlive() && !p.isRemoved() && !p.isSpectator();}
 private static boolean visible(ServerPlayer p,ServerLevel l,BlockPos pos){
  if(p.position().distanceToSqr(Vec3.atCenterOf(pos))>14*14)return false;
  // Solid footing is observed on its exposed top face; its buried center can be occluded by an adjacent valid bank.
  var target=Vec3.atCenterOf(pos);if(l.getBlockState(pos).isCollisionShapeFullBlock(l,pos)&&target.y<p.getEyePosition().y)target=target.add(0,.499,0);
  var hit=l.clip(new ClipContext(p.getEyePosition(),target,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
  return hit.getType()==HitResult.Type.MISS || hit.getBlockPos().equals(pos);
 }
 public static boolean allows(ServerPlayer p,ServerLevel l,BlockPos pos){
  var active=ACTIVE.get();var key=new ReadKey(p.getUUID(),net.minecraft.core.GlobalPos.of(l.dimension(),pos.immutable()));
  if(active.size()>=64 || !active.add(key))return false;
  try{return allowsLeased(p,l,pos);}finally{active.remove(key);if(active.isEmpty())ACTIVE.remove();}
 }
 private static boolean allowsLeased(ServerPlayer p,ServerLevel l,BlockPos pos){
  if(!actor(p) || p.level()!=l || !l.hasChunkAt(pos) || l.isOutsideBuildHeight(pos) || !l.getWorldBorder().isWithinBounds(pos)
   || p.position().distanceToSqr(Vec3.atCenterOf(pos))>14*14 || !l.mayInteract(p,pos) || Effects.isTemporary(l,pos))return false;
  var before=l.getBlockState(pos);if(before.hasBlockEntity())return false;
  if(!visible(p,l,pos))return false;
  // Wildercord's existing claim compatibility is break-based. No break is performed.
  // A claim mod may still decline reading because it has no distinct read permission.
  if(!dev.wildercord.cast.Casters.probeBreak(l,p,pos,before,null))return false;
  return actor(p) && p.level()==l && l.hasChunkAt(pos) && l.getWorldBorder().isWithinBounds(pos) && l.mayInteract(p,pos)
   && l.getBlockState(pos)==before && !Effects.isTemporary(l,pos) && visible(p,l,pos);
 }
}
