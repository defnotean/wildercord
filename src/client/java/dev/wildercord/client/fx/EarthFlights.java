package dev.wildercord.client.fx;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import java.util.function.BiConsumer;
/** The same gathered physical materials travel on the paid projectile, along its actual velocity. */
final class EarthFlights {
 private EarthFlights() {}
 static void draw(String id,int age,double scale,double length,Vec3 head,Vec3 velocity,boolean minimal,BiConsumer<ParticleOptions,Vec3> emit){
  if(!EarthForms.supports(id) || !Double.isFinite(velocity.lengthSqr()))return;
  var f=velocity.lengthSqr()<.0001?new Vec3(0,0,1):velocity.normalize();
  var r=f.cross(new Vec3(0,1,0));r=r.lengthSqr()<.0001?new Vec3(1,0,0):r.normalize();
  EarthForms.fly(id,age,Math.clamp(scale,.4,2)*.65,head,r,r.cross(f).normalize(),f.scale(Math.clamp(length,1,2)),minimal,emit);
 }
}
