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

/** Actual native packets for each void preparation and a mana-paid Cord cast. */
public final class VoidFormationTest implements FabricClientGameTest {
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
    try {for(int style=0;style<=dev.wildercord.content.VoidOption.REMNANT;style++) {
     var material=new dev.wildercord.content.VoidOption(style,0xC7B897,.17F,8,new Vec3(.01,-.02,.03),-.1F);
     dev.wildercord.content.VoidOption.STREAM_CODEC.encode(buffer,material);
     check(material.equals(dev.wildercord.content.VoidOption.STREAM_CODEC.decode(buffer)),"Physical fragment wire preserves material, drift and spin");
    }}finally{buffer.release();}
   });
   var baseline=c.computeOnClient(mc -> snapshot(mc,"void_formation_background"));c.waitFor(mc -> baseline.isDone());baseline.join();
   var roster=Runes.all().stream().filter(r -> r.family()==RuneFamily.EFFECT && r.element().equals("void")).map(r -> r.id()).sorted().toList();
   check(roster.size()==VoidForms.RUNES.size(),"Reviewed void roster changes require explicit expansion of this suite");
   for(var quality:List.of(MagicQuality.Level.FULL,MagicQuality.Level.MINIMAL)) {
    c.runOnClient(mc -> MagicQuality.own=quality);
    for(var id:roster) {
     c.waitTicks(9);c.runOnClient(mc -> mc.particleEngine.clearParticles());
     w.getServer().runOnServer(s -> {var p=s.getPlayerList().getPlayers().getFirst();ServerPlayNetworking.send(p,new FormationPayload(p.getId(),"bolt",List.of(Runes.BOLT.id(),id),dev.wildercord.spell.VisualElements.of(List.of(Runes.BOLT,Runes.get(id).orElseThrow())),0xC5B58F,1));});c.waitTicks(4);
     var captured=c.computeOnClient(mc -> {int materials=0,circles=0;for(var particle:particles(mc.particleEngine)) {if(!particle.isAlive())continue;
      if(particle instanceof SpellCircleParticle){circles++;check(at(particle).subtract(mc.player.getEyePosition()).dot(mc.player.getLookAngle())<0,"Rear circle for "+id);}
      if(particle instanceof LightParticle)throw new AssertionError("Void-only Bolt preparation must not retain generic luminous body");
      if(particle instanceof VoidParticle){materials++;check(at(particle).distanceTo(mc.player.getEyePosition().add(0,-.65,3.2))<1.65,"Bounded front preparation for "+id);}
     }check(materials>0 && circles>0,"Actual authored particles and rear circle for "+id+" / "+quality);return quality==MagicQuality.Level.FULL?snapshot(mc,"void_formation_"+id.substring(11)):java.util.concurrent.CompletableFuture.completedFuture(null);});
     c.waitFor(mc -> captured.isDone());captured.join();
    }
   }
   c.runOnClient(mc -> MagicQuality.own=MagicQuality.Level.FULL);
   // The ordinary paid cast verifies that server staging reaches the authored client route.
   c.waitTicks(12);c.runOnClient(mc -> mc.particleEngine.clearParticles());
   w.getServer().runOnServer(s -> {var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);
    Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));var book=Spellbooks.get(p).withStarterGiven();for(var r:Runes.all())book=book.learn(r.id());Spellbooks.set(p,book);
    SpellCaster.edit(p,0,List.of(Runes.BOLT.id(),Runes.UMBRA.id()));Spellbooks.setReadyAt(p,0,0);Spellbooks.setMana(p,100);float before=Spellbooks.mana(p);SpellCaster.cast(p,0);check(Spellbooks.mana(p)<before,"Native Survival cast spends mana");
   });c.waitTicks(4);var paid=c.computeOnClient(mc -> snapshot(mc,"void_formation_paid_umbra"));c.waitFor(mc -> paid.isDone());paid.join();
   c.waitTicks(12);c.runOnClient(mc -> {mc.particleEngine.clearParticles();mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);});
   w.getServer().runOnServer(s -> {var p=s.getPlayerList().getPlayers().getFirst();SpellCaster.edit(p,0,List.of(Runes.SELF.id(),Runes.VEIL.id()));Spellbooks.setReadyAt(p,0,0);Spellbooks.setMana(p,100);float before=Spellbooks.mana(p);SpellCaster.cast(p,0);check(Spellbooks.mana(p)<before,"Paid Veil spends mana");
   });c.waitTicks(4);c.runOnClient(mc -> {int found=0;for(var particle:particles(mc.particleEngine))if(particle.isAlive() && particle instanceof VoidParticle && at(particle).distanceTo(mc.player.position().add(0,.7,0))<1.65)found++;check(found>0,"Paid Self preparation stays on caster");});
   var self=c.computeOnClient(mc -> snapshot(mc,"void_formation_paid_veil"));c.waitFor(mc -> self.isDone());self.join();
   w.getServer().runOnServer(s -> check(s.getPlayerList().getPlayers().getFirst().hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY),"Self Veil applies actual Invisibility after release"));
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
     if(!name.equals("void_formation_paid_veil"))check(changed>20,"Visible formation differs inside aim-area region: "+name+", changed="+changed);
    }result.complete(null);
   }catch(Throwable e){result.completeExceptionally(e);}
  });return result;
 }
 private static void verifyRecipes() {
  check(!VoidForms.supports("other:wind"),"Foreign namespace does not dispatch built-in art");
  var fingerprints=new HashSet<String>();
  for(String rune:VoidForms.RUNES) {
   check(VoidForms.supports("wildercord:"+rune),"Authored rune dispatch "+rune);
   var beats=new ArrayList<String>();
   for(int beat=1;beat<=2;beat++)for(boolean minimal:new boolean[]{false,true}) {
    var trace=new ArrayList<String>();
    VoidForms.prepare("wildercord:"+rune,beat,1,Vec3.ZERO,new Vec3(1,0,0),new Vec3(0,1,0),new Vec3(0,0,1),minimal,(option,at)-> {
     check(Double.isFinite(at.lengthSqr()) && at.length()<1.8,"Finite bounded geometry: "+rune);
     check(!(option instanceof dev.wildercord.content.LightOption),"Custom void recipe avoids light primitives");
     trace.add(option.toString()+"@"+at);
    });check(!trace.isEmpty() && trace.size()<96,"Recipe retains identity within minimal canvas budget: "+rune);beats.add(String.join(";",trace));
   }
   check(!beats.get(0).equals(beats.get(2)),"Two formation beats evolve: "+rune);
   check(fingerprints.add(beats.get(0)),"No duplicate authored recipe: "+rune);
  }
  var ingredients=Map.of("blackflame",dev.wildercord.content.MaterialOption.EMBER,"warp",dev.wildercord.content.MaterialOption.WIND,
   "entropy",dev.wildercord.content.MaterialOption.TIME,"devour",dev.wildercord.content.MaterialOption.BLOOD,
   "malison",dev.wildercord.content.MaterialOption.ARCANE);
  for(var entry:ingredients.entrySet()){var styles=new HashSet<Integer>();VoidForms.prepare("wildercord:"+entry.getKey(),2,1,Vec3.ZERO,new Vec3(1,0,0),new Vec3(0,1,0),new Vec3(0,0,1),true,(option,at)->{if(option instanceof dev.wildercord.content.MaterialOption m)styles.add(m.style());});check(styles.contains(entry.getValue()),"Supporting materials remain in Minimal: "+entry.getKey());}
  check(Runes.all().stream().filter(r->r.family()==RuneFamily.EFFECT && r.element().equals("void")).map(r->r.path()).collect(java.util.stream.Collectors.toSet()).equals(new HashSet<>(VoidForms.RUNES)),"Exact runtime void roster includes fusions and innate");


 }
 private static Object field(Object value,Class<?> owner,String name){try{var f=owner.getDeclaredField(name);f.setAccessible(true);return f.get(value);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static Vec3 at(Particle p){return new Vec3(((Number)field(p,Particle.class,"x")).doubleValue(),((Number)field(p,Particle.class,"y")).doubleValue(),((Number)field(p,Particle.class,"z")).doubleValue());}
 private static List<Particle> particles(ParticleEngine engine){var found=new ArrayList<Particle>();for(var group:((Map<?,?>)field(engine,ParticleEngine.class,"particles")).values())for(var p:(Queue<?>)field(group,ParticleGroup.class,"particles"))found.add((Particle)p);for(var p:(Queue<?>)field(engine,ParticleEngine.class,"particlesToAdd"))found.add((Particle)p);return found;}
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
