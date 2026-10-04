package dev.wildercord.cast;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
/** Decorative frames derived from an actual body or edited cell; anchor provenance is not moved. */
final class LifeOutcomeFrames {
 record Frame(Vec3 normal,double standoff){}
 static final Frame LOCAL=new Frame(new Vec3(0,0,1),0);
 private LifeOutcomeFrames(){}
 static Vec3 horizontal(Vec3 value){var n=new Vec3(value.x,0,value.z);return n.lengthSqr()<1e-6?new Vec3(0,0,1):n.normalize();}
 static Frame body(LivingEntity source,LivingEntity recipient,String detail){
  // A real source determines the approached face; Self or unavailable source uses the body's front.
  Vec3 n=source!=null&&source!=recipient&&source.level()==recipient.level()
   ?horizontal(source.getBoundingBox().getCenter().subtract(recipient.getBoundingBox().getCenter()))
   :horizontal(recipient.getLookAngle());
  double half=(recipient.getBoundingBox().maxX-recipient.getBoundingBox().minX)*.5;
  if(detail.startsWith("equipment:")){
   n=horizontal(recipient.getLookAngle());
   // Existing hand anchors already have a .2 forward offset; armour anchors stay at their actual slot height.
   return new Frame(n,detail.endsWith("mainhand")||detail.endsWith("offhand")?.12:Math.min(4,half+.08));
  }
  return new Frame(n,Math.min(4,half*(Math.abs(n.x)+Math.abs(n.z))+.08));
 }
 static Frame root(LivingEntity source,Vec3 cell){
  Vec3 to=source.getEyePosition().subtract(cell);
  Vec3 n=Math.abs(to.x)>=Math.abs(to.z)?new Vec3(to.x<0?-1:1,0,0):new Vec3(0,0,to.z<0?-1:1);
  return new Frame(n,.58);
 }
 static Vec3 anchor(String rune,LivingEntity body){
  // Minimum local y is -.38: put Bloomstep's foot tread above the actual ground/feet rather than inside it.
  return rune.equals("bloomstep")?body.position().add(0,.42,0):body.getBoundingBox().getCenter();
 }
 static Frame event(Cast cast,String rune,java.util.UUID who,Vec3 at,String detail){
  if(who!=null&&cast.level.getEntity(who) instanceof LivingEntity body)return body(cast.caster,body,detail);
  return rune.equals("root_bulwark")?root(cast.caster,at):LOCAL;
 }
 static Frame event(net.minecraft.server.level.ServerLevel level,java.util.UUID who){
  return who!=null&&level.getEntity(who) instanceof LivingEntity body?body(null,body,""):LOCAL;
 }
}
