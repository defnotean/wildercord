package dev.wildercord.client.fx;

import dev.wildercord.net.FormationPayload;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.CameraType;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Actual native packets for each wind preparation and a mana-paid Cord cast. */
public final class WindFormationTest implements FabricClientGameTest {
 private static int[] background;
 @Override public void runTest(ClientGameTestContext c) {
  var previous=c.computeOnClient(mc -> MagicQuality.own);
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(40);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 6000");w.getServer().runCommand("weather clear");
   w.getServer().runCommand("fill -16 100 -12 16 100 20 polished_deepslate");w.getServer().runCommand("fill -12 101 8 12 109 8 gray_concrete");
   w.getServer().runOnServer(s -> {var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);});c.waitTicks(15);
   c.runOnClient(mc -> {mc.getWindow().setWindowed(1280,720);mc.resizeGui();mc.options.setCameraType(CameraType.FIRST_PERSON);if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();mc.gui.toastManager().clear();verifyRecipes();});
   w.getServer().runOnServer(s -> {
    var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),s.registryAccess());
    try {
     for(boolean minimal:new boolean[]{false,true}) {
      var o=new dev.wildercord.content.AirflowOption(0xC5E7DB,new Vec3(.1,.2,.3),new Vec3(-.3,.1,.2),.06F,8,minimal);
      dev.wildercord.content.AirflowOption.STREAM_CODEC.encode(buffer,o);
      check(o.equals(dev.wildercord.content.AirflowOption.STREAM_CODEC.decode(buffer)),"Airflow wire retains curve and owner-selected quality");
     }
    } finally {buffer.release();}
   });
   var baseline=c.computeOnClient(mc -> snapshot(mc,"wind_formation_background"));c.waitFor(mc -> baseline.isDone());baseline.join();
   var wind=Runes.all().stream().filter(r -> r.family()==RuneFamily.EFFECT && r.element().equals("wind")).map(r -> r.id()).sorted().toList();
   check(wind.size()==25,"Reviewed wind roster changes require explicit expansion of this suite");
   for(var quality:List.of(MagicQuality.Level.FULL,MagicQuality.Level.MINIMAL)) {
    c.runOnClient(mc -> MagicQuality.own=quality);
    for(var id:wind) {
     c.waitTicks(9);c.runOnClient(mc -> mc.particleEngine.clearParticles());
     w.getServer().runOnServer(s -> {var p=s.getPlayerList().getPlayers().getFirst();ServerPlayNetworking.send(p,new FormationPayload(p.getId(),"bolt",List.of(Runes.BOLT.id(),id),dev.wildercord.spell.VisualElements.of(List.of(Runes.BOLT,Runes.get(id).orElseThrow())),0xFFF2C0,1));});c.waitTicks(4);
     var captured=c.computeOnClient(mc -> {int materials=0,circles=0;for(var particle:particles(mc.particleEngine)) {if(!particle.isAlive())continue;
      if(particle instanceof SpellCircleParticle){circles++;check(at(particle).subtract(mc.player.getEyePosition()).dot(mc.player.getLookAngle())<0,"Rear circle for "+id);}
      if(particle instanceof LightParticle)throw new AssertionError("Wind-only Bolt preparation must not retain generic luminous body");
      if(particle instanceof AirflowParticle){materials++;check(at(particle).distanceTo(mc.player.getEyePosition().add(0,-.65,3.2))<1.65,"Bounded front preparation for "+id);}
     }check(materials>0 && circles>0,"Actual authored particles and rear circle for "+id+" / "+quality);return quality==MagicQuality.Level.FULL?snapshot(mc,"wind_formation_"+id.substring(11)):java.util.concurrent.CompletableFuture.completedFuture(null);});
     c.waitFor(mc -> captured.isDone());captured.join();
    }
   }
   c.runOnClient(mc -> MagicQuality.own=MagicQuality.Level.FULL);
   // The ordinary paid cast verifies that server staging reaches the authored client route.
   c.waitTicks(12);c.runOnClient(mc -> mc.particleEngine.clearParticles());
   w.getServer().runOnServer(s -> {var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);
    Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));var book=Spellbooks.get(p).withStarterGiven();for(var r:Runes.all())book=book.learn(r.id());Spellbooks.set(p,book);
    SpellCaster.edit(p,0,List.of(Runes.BOLT.id(),Runes.WINDCUT.id()));Spellbooks.setReadyAt(p,0,0);Spellbooks.setMana(p,100);float before=Spellbooks.mana(p);SpellCaster.cast(p,0);check(Spellbooks.mana(p)<before,"Native Survival cast spends mana");
   });c.waitTicks(4);var paid=c.computeOnClient(mc -> snapshot(mc,"wind_formation_paid_windcut"));c.waitFor(mc -> paid.isDone());paid.join();
   c.waitTicks(12);c.runOnClient(mc -> {mc.particleEngine.clearParticles();mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);});
   w.getServer().runOnServer(s -> {var p=s.getPlayerList().getPlayers().getFirst();SpellCaster.edit(p,0,List.of(Runes.SELF.id(),Runes.SWIFT.id()));Spellbooks.setReadyAt(p,0,0);Spellbooks.setMana(p,100);float before=Spellbooks.mana(p);SpellCaster.cast(p,0);check(Spellbooks.mana(p)<before,"Paid Swift spends mana");
   });c.waitTicks(4);c.runOnClient(mc -> {int found=0;for(var particle:particles(mc.particleEngine))if(particle.isAlive() && particle instanceof AirflowParticle && at(particle).distanceTo(mc.player.position().add(0,.7,0))<1.65)found++;check(found>0,"Paid Self preparation stays on caster");});
   var self=c.computeOnClient(mc -> snapshot(mc,"wind_formation_paid_swift"));c.waitFor(mc -> self.isDone());self.join();
   w.getServer().runOnServer(s -> check(s.getPlayerList().getPlayers().getFirst().hasEffect(net.minecraft.world.effect.MobEffects.SPEED),"Self Swift grants Speed after release"));
  } finally {c.runOnClient(mc -> MagicQuality.own=previous);}
 }
 private static java.util.concurrent.CompletableFuture<Void> snapshot(net.minecraft.client.Minecraft mc,String name) {
  // Capture this exact production-particle step, without extra screenshot helper ticks.
  var result=new java.util.concurrent.CompletableFuture<Void>();
  mc.gameRenderer.update(net.minecraft.client.DeltaTracker.ONE);mc.gameRenderer.extract(net.minecraft.client.DeltaTracker.ONE,true);mc.gameRenderer.render();
  com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder().submit();
  net.minecraft.client.Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image -> {
   try(image) {
    var file=java.nio.file.Path.of("screenshots",name+".png");java.nio.file.Files.createDirectories(file.getParent());image.writeToFile(file);
    var pixels=image.getPixels();
    if(name.endsWith("background"))background=pixels;else {
     int changed=0,width=image.getWidth(),height=image.getHeight();
     for(int y=height/3;y<height*9/10;y++)for(int x=width/4;x<width*3/4;x++){int i=y*width+x,pixel=pixels[i],old=background[i];int difference=Math.abs((pixel>>16&255)-(old>>16&255))+Math.abs((pixel>>8&255)-(old>>8&255))+Math.abs((pixel&255)-(old&255));if(difference>20)changed++;}
     if(!name.equals("wind_formation_paid_swift"))check(changed>20,"Visible formation differs inside aim-area region: "+name+", changed="+changed);
    }result.complete(null);
   }catch(Throwable e){result.completeExceptionally(e);}
  });return result;
 }
 private static void verifyRecipes() {
  for(var direction:List.of(new org.joml.Vector3f(0,1,0),new org.joml.Vector3f(0,-1,0),new org.joml.Vector3f(1,0,0))) {
   var q=Facing.along(direction,direction.x,direction.y,direction.z,new org.joml.Quaternionf());
   check(Float.isFinite(q.x)&&Float.isFinite(q.y)&&Float.isFinite(q.z)&&Float.isFinite(q.w),"Camera-aligned wind stream has a finite orientation");
  }

  check(!WindForms.supports("other:wind"),"Foreign namespace does not dispatch built-in art");
  var fingerprints=new HashSet<String>();
  for(String rune:WindForms.RUNES) {
   check(WindForms.supports("wildercord:"+rune),"Authored rune dispatch "+rune);
   var beats=new ArrayList<String>();
   for(int beat=1;beat<=2;beat++)for(boolean minimal:new boolean[]{false,true}) {
    var trace=new ArrayList<String>();
    WindForms.draw("wildercord:"+rune,beat,false,1,Vec3.ZERO,new Vec3(1,0,0),new Vec3(0,1,0),new Vec3(0,0,1),minimal,(option,at)-> {
     check(Double.isFinite(at.lengthSqr()) && at.length()<1.8,"Finite bounded geometry: "+rune);
     check(!(option instanceof dev.wildercord.content.LightOption),"Custom wind recipe avoids light primitives");
     trace.add(option.toString()+"@"+at);
    });check(!trace.isEmpty() && trace.size()<96,"Recipe retains identity within minimal canvas budget: "+rune);beats.add(String.join(";",trace));
   }
   check(!beats.get(0).equals(beats.get(2)),"Two formation beats evolve: "+rune);
   check(fingerprints.add(beats.get(0)),"No duplicate authored recipe: "+rune);
  }
  var ingredients=Map.of(
   "dust_devil",List.of(dev.wildercord.content.MaterialOption.WIND,dev.wildercord.content.MaterialOption.STONE),
   "downdraft",List.of(dev.wildercord.content.MaterialOption.WIND,dev.wildercord.content.MaterialOption.STONE),
   "recoil",List.of(dev.wildercord.content.MaterialOption.WIND,dev.wildercord.content.MaterialOption.TIME),
   "razorgale",List.of(dev.wildercord.content.MaterialOption.WIND,dev.wildercord.content.MaterialOption.BLOOD),
   "prune",List.of(dev.wildercord.content.MaterialOption.WIND,dev.wildercord.content.MaterialOption.PETAL),
   "zephyr",List.of(dev.wildercord.content.MaterialOption.WIND,dev.wildercord.content.MaterialOption.PETAL),
   "skyglyph",List.of(dev.wildercord.content.MaterialOption.WIND,dev.wildercord.content.MaterialOption.ARCANE),
   "summit_wind",List.of(dev.wildercord.content.MaterialOption.WIND,dev.wildercord.content.MaterialOption.VAPOUR));
  for(var entry:ingredients.entrySet()){var styles=new HashSet<Integer>();WindForms.draw("wildercord:"+entry.getKey(),2,false,1,Vec3.ZERO,new Vec3(1,0,0),new Vec3(0,1,0),new Vec3(0,0,1),true,(option,at)->{if(option instanceof dev.wildercord.content.MaterialOption m)styles.add(m.style());});check(styles.containsAll(entry.getValue()),"Supporting materials remain in Minimal: "+entry.getKey());}

 }
 private static Object field(Object value,Class<?> owner,String name){try{var f=owner.getDeclaredField(name);f.setAccessible(true);return f.get(value);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static Vec3 at(Particle p){return new Vec3(((Number)field(p,Particle.class,"x")).doubleValue(),((Number)field(p,Particle.class,"y")).doubleValue(),((Number)field(p,Particle.class,"z")).doubleValue());}
 private static List<Particle> particles(ParticleEngine engine){var found=new ArrayList<Particle>();for(var group:((Map<?,?>)field(engine,ParticleEngine.class,"particles")).values())for(var p:(Queue<?>)field(group,ParticleGroup.class,"particles"))found.add((Particle)p);for(var p:(Queue<?>)field(engine,ParticleEngine.class,"particlesToAdd"))found.add((Particle)p);return found;}
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
