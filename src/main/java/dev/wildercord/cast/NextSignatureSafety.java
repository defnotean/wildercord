package dev.wildercord.cast;

import dev.wildercord.world.dungeons.DungeonWards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Shared permission/loaded checks, not shared signature mechanics or silhouettes. */
final class NextSignatureSafety {
 private NextSignatureSafety() {}
 static boolean loaded(Cast c,BlockPos p) {
  return c.alive() && c.level.isLoaded(p) && !c.level.isOutsideBuildHeight(p) && c.level.getWorldBorder().isWithinBounds(p);
 }
 static boolean editable(Cast c,BlockPos p) {
  return loaded(c,p) && !TemporaryBlocks.recorded(c.level,p) && !DungeonWards.warded(c.level,p)
   && Casters.mayEdit(c.caster,c.level,p);
 }
 static boolean open(Cast c,Vec3 from,Vec3 to,double maximum) {
  double length=to.subtract(from).length();if(!Double.isFinite(length) || length>maximum)return false;
  int n=Math.max(1,(int)Math.ceil(length*2));var delta=to.subtract(from);
  for(int i=0;i<=n;i++)if(!loaded(c,BlockPos.containing(from.add(delta.scale(i/(double)n)))))return false;
  return c.level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,c.caster)).getType()==HitResult.Type.MISS;
 }
 static boolean target(Cast c,LivingEntity t,boolean hostile) {
  if(!c.alive() || !t.isAlive() || t.isRemoved() || t.level()!=c.level || !loaded(c,t.blockPosition())
   || DungeonWards.warded(c.level,t.blockPosition()) || hostile && t.isPermanentlyInvulnerable())return false;
  if(t instanceof ServerPlayer p && (p.isSpectator() || p.isCreative()))return false;
  return hostile?Targets.canHarm(c.caster,t):Targets.canHelp(c.caster,t);
 }
 static boolean mobile(Cast c,LivingEntity t,boolean hostile) {
  return target(c,t,hostile) && !t.isPassenger() && !Spirits.isBoss(t) && !VoidTime.anchored(t)
   && (!hostile || !(t instanceof dev.wildercord.pet.CinnamonDog));
 }
 static boolean body(Cast c,LivingEntity t,Vec3 at,boolean grounded) {
  if(t.getBbWidth()>2 || t.getBbHeight()>3)return false;
  var box=t.getBoundingBox().move(at.subtract(t.position()));
  for(var p:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))
   if(!editable(c,p) || DungeonWards.warded(c.level,p))return false;
  if(!c.level.noCollision(t,box) || c.level.containsAnyLiquid(box))return false;
  if(!grounded)return true;
  var floor=BlockPos.containing(at).below();if(!editable(c,floor) || DungeonWards.warded(c.level,floor))return false;
  var state=c.level.getBlockState(floor);
  return state.isFaceSturdy(c.level,floor,Direction.UP) && !state.is(Blocks.MAGMA_BLOCK)
   && !state.is(Blocks.CACTUS) && !state.is(Blocks.CAMPFIRE) && !state.is(Blocks.SOUL_CAMPFIRE);
 }
}
