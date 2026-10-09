package dev.wildercord.client.fx;

import java.util.UUID;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Pure safety boundary checks. Actual cameras, rendered quads and paid casts have native gates. */
class LifeOwnerClearanceTest {
 @Test void eyeClearanceDependsOnTheActualOwnerViewButCameraClearanceNeverDoes(){
  Vec3 eye=new Vec3(0,1.62,0),body=new Vec3(0,1.05,.35),third=new Vec3(0,1.62,4);
  assertTrue(LifeOwnerClearance.clear(true,body,eye,third));
  assertFalse(LifeOwnerClearance.clear(false,body,eye,third));
  assertFalse(LifeOwnerClearance.clear(false,body,eye,eye));
  assertFalse(LifeOwnerClearance.clear(true,body,eye,body.add(0,0,1.09)));
  assertTrue(LifeOwnerClearance.clear(true,body,eye,body.add(0,0,1.11)));
 }
 @Test void freeLookUsesCameraPositionRatherThanDistanceFromOwner(){
  Vec3 eye=Vec3.ZERO,body=new Vec3(0,0,.5);
  for(Vec3 camera:new Vec3[]{new Vec3(4,0,0),new Vec3(-4,0,0),new Vec3(0,0,-4)})
   assertTrue(LifeOwnerClearance.clear(true,body,eye,camera));
  assertFalse(LifeOwnerClearance.clear(true,body,eye,new Vec3(.3,0,.5)));
  assertFalse(LifeOwnerClearance.clear(true,body,eye,new Vec3(Double.NaN,0,0)));
 }
 @Test void scopedOwnerNeverLeaksAcrossUnrelatedParticlesOrFailures(){
  UUID outer=new UUID(1,2),inner=new UUID(3,4);
  assertNull(LifeOwnerClearance.source());
  LifeOwnerClearance.emit(outer,true,()->{
   assertEquals(outer,LifeOwnerClearance.source());
   assertTrue(LifeOwnerClearance.owner());
   LifeOwnerClearance.emit(inner,false,()->{assertEquals(inner,LifeOwnerClearance.source());assertFalse(LifeOwnerClearance.owner());});
   assertEquals(outer,LifeOwnerClearance.source());
   assertThrows(IllegalStateException.class,()->LifeOwnerClearance.emit(null,false,()->{
    assertNull(LifeOwnerClearance.source());throw new IllegalStateException();
   }));
   assertEquals(outer,LifeOwnerClearance.source());
  });
  assertNull(LifeOwnerClearance.source());
  assertFalse(LifeOwnerClearance.owner());
 }
}
