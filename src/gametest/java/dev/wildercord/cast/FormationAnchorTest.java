package dev.wildercord.cast;
import dev.wildercord.net.FormationPayload;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.CameraType;
import net.minecraft.client.particle.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.util.*;
/** Actual encoded formation packets, client particle positions and rear-circle placement at three aim pitches. */
public final class FormationAnchorTest implements FabricClientGameTest {
 @Override public void runTest(ClientGameTestContext c) {
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(30);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 6000");w.getServer().runCommand("fill -10 100 -10 10 100 10 polished_deepslate");
   w.getServer().runOnServer(s -> {var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);});c.waitTicks(10);c.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
   for(var shape:List.of("self","domain","orbit","trail","bolt","beam"))for(float pitch:new float[]{-70,0,70}) {
    c.waitTicks(15);w.getServer().runOnServer(s -> {var p=s.getPlayerList().getPlayers().getFirst();p.setXRot(pitch);p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,pitch,false);});c.waitTicks(3);c.runOnClient(mc -> mc.particleEngine.clearParticles());
    w.getServer().runOnServer(s -> {var p=s.getPlayerList().getPlayers().getFirst();ServerPlayNetworking.send(p,new FormationPayload(p.getId(),shape,List.of(Runes.SELF.id(),Runes.HEAL.id()),List.of("fire","wind","frost","storm","earth","life","void","time","blood","arcane"),0xA8D8C0,1));});c.waitTicks(3);
    var measured=c.computeOnClient(mc -> {
     var particles=particles(mc.particleEngine);var position=mc.player.position();var forward=mc.player.getLookAngle().normalize();int circle=0,material=0;double furthestBody=0,nearestFront=Double.POSITIVE_INFINITY;
     for(var particle:particles) {
      if(!particle.isAlive())continue;var delta=at(particle).subtract(position);
      if(particle instanceof dev.wildercord.client.fx.SpellCircleParticle) {check(delta.dot(forward)<-.5,"Rear casting circle stays behind at pitch "+pitch);circle++;continue;}
      if(particle instanceof dev.wildercord.client.fx.LightParticle)continue;
      if(particle.getClass().getName().startsWith("dev.wildercord.client.fx.")) {material++;furthestBody=Math.max(furthestBody,delta.length());nearestFront=Math.min(nearestFront,at(particle).distanceTo(mc.player.getEyePosition().add(forward.scale(3.2)).subtract(forward.cross(new Vec3(0,1,0)).normalize().cross(forward).normalize().scale(.65))));}
     }
     check(circle>0 && material>0,"Native formation must create rear circle and elemental particles: "+shape+", pitch "+pitch+", circle="+circle+", material="+material);
     boolean centered=Set.of("self","domain","orbit","trail").contains(shape);check(centered?furthestBody<2.1:nearestFront<1.3,"Native material anchor follows delivery: "+shape+", pitch="+pitch+", body="+furthestBody+", front="+nearestFront);
     return shape+" at "+pitch+": "+material+" material particles";
    });
    if(pitch==0 && (shape.equals("self") || shape.equals("orbit") || shape.equals("beam"))) {c.runOnClient(mc -> {mc.gui.hud.getChat().clearMessages(false);mc.gui.toastManager().clear();});c.takeScreenshot(TestScreenshotOptions.of("formation_"+shape+"_anchor").disableCounterPrefix());}
   }
  }
 }
 private static Object field(Object value,Class<?> owner,String name) {try {var f=owner.getDeclaredField(name);f.setAccessible(true);return f.get(value);}catch(ReflectiveOperationException e) {throw new AssertionError(e);}}
 private static Vec3 at(Particle p) {return new Vec3(((Number)field(p,Particle.class,"x")).doubleValue(),((Number)field(p,Particle.class,"y")).doubleValue(),((Number)field(p,Particle.class,"z")).doubleValue());}
 private static List<Particle> particles(ParticleEngine engine) {var found=new ArrayList<Particle>();for(var group:((Map<?,?>)field(engine,ParticleEngine.class,"particles")).values())for(var p:(Queue<?>)field(group,ParticleGroup.class,"particles"))found.add((Particle)p);for(var p:(Queue<?>)field(engine,ParticleEngine.class,"particlesToAdd"))found.add((Particle)p);return found;}
 private static void check(boolean yes,String why) {if(!yes)throw new AssertionError(why);}
}
