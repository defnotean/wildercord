package dev.wildercord.cast;
import java.util.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.entity.EntityTypeTest;
/** Complete local projectile pools have finite admission; overcrowded screens visibly refuse. */
final class BoundedCounterCandidates {
 static final int INSPECT=64, HANDLE=12;
 record Batch(List<Projectile> candidates,List<Projectile> completeSnapshot,int enumerated,boolean saturated){}
 static Batch nearby(Cast c,LivingEntity t){
  var scanned=new ArrayList<Projectile>(INSPECT+1);
  // Predicate intentionally admits every local projectile, INCLUDING benign/dead/unowned ones.
  // Filtering first would allow arbitrary rejected entities to defeat the traversal admission cap.
  c.level.getEntities(EntityTypeTest.<Entity,Projectile>forClass(Projectile.class),
   t.getBoundingBox().inflate(3),p -> true,scanned,INSPECT+1);
  // No global-nearest guarantee is possible after early abort. Fail closed on saturation instead
  // of falsely promising a shield while a hostile may hide beyond the benign prefix.
  if(scanned.size()>INSPECT)return new Batch(List.of(),List.of(),scanned.size(),true);
  int enumerated=scanned.size();var complete=List.copyOf(scanned);
  scanned.removeIf(p -> !p.isAlive() || !NextSignatureSafety.loaded(c,p.blockPosition()));
  scanned.sort(Comparator.<Projectile>comparingDouble(p -> p.distanceToSqr(t)).thenComparingInt(Entity::getId));
  return new Batch(List.copyOf(scanned.subList(0,Math.min(HANDLE,scanned.size()))),complete,enumerated,false);
 }
}
