package dev.wildercord.client.fx;
import dev.wildercord.cast.*;
import dev.wildercord.spell.Runes;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.content.WildercordItems;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.particle.*;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Paid native Bolt launches, synchronized effect identity and real flight particles. */
public final class FireFlightTest implements FabricClientGameTest {
 private static int[] background;
 @Override public void runTest(ClientGameTestContext c) {
  var previous=c.computeOnClient(mc->MagicQuality.own);
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(40);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 6000");w.getServer().runCommand("weather clear");
   w.getServer().runCommand("fill -16 100 -12 16 100 40 polished_deepslate");w.getServer().runCommand("fill -12 101 32 12 109 32 gray_concrete");
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));var b=Spellbooks.get(p).withStarterGiven();for(var r:Runes.all())b=b.learn(r.id());Spellbooks.set(p,b);});c.waitTicks(15);
   c.runOnClient(mc->{mc.getWindow().setWindowed(1280,720);mc.resizeGui();if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();mc.gui.toastManager().clear();recipes();});
   var empty=c.computeOnClient(mc->snapshot(mc,"fire_flight_background"));c.waitFor(mc->empty.isDone());empty.join();
   for(var q:List.of(MagicQuality.Level.FULL,MagicQuality.Level.MINIMAL))for(String rune:FireFlights.RUNES) {
    c.waitTicks(12);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.level().getEntitiesOfClass(RuneBolt.class,p.getBoundingBox().inflate(64)).forEach(net.minecraft.world.entity.Entity::discard);});
    c.runOnClient(mc->{mc.particleEngine.clearParticles();MagicQuality.own=q;});
    w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();SpellCaster.edit(p,0,List.of(Runes.BOLT.id(),"wildercord:"+rune));Spellbooks.setMana(p,100);Spellbooks.setReadyAt(p,0,0);float before=Spellbooks.mana(p);SpellCaster.cast(p,0);check(Spellbooks.mana(p)<before,"Paid Bolt/"+rune);});
    c.waitTicks(8);
    c.runOnClient(mc->{var bolts=new ArrayList<RuneBolt>();for(var e:mc.level.entitiesForRendering())if(e instanceof RuneBolt b)bolts.add(b);check(!bolts.isEmpty(),"Live actual Bolt for "+rune);var b=bolts.getFirst();check(b.getEntityData().get(RuneBolt.DATA_EFFECTS).equals("wildercord:"+rune),"Synced exact flight identity");check(b.position().distanceTo(mc.player.position())>2,"Actual flight advances");check(particles(mc.particleEngine).stream().anyMatch(p->p.isAlive() && p instanceof MaterialParticle && at(p).distanceTo(b.position())<3),"Authored material follows real projectile "+rune);});
    if(q==MagicQuality.Level.FULL){var shot=c.computeOnClient(mc->snapshot(mc,"fire_flight_"+rune));c.waitFor(mc->shot.isDone());shot.join();c.waitTicks(6);var travel=c.computeOnClient(mc->snapshot(mc,"fire_flight_travel_"+rune));c.waitFor(mc->travel.isDone());travel.join();}
   }
  }finally{c.runOnClient(mc->MagicQuality.own=previous);}
 }
 private static void recipes(){
  var prints=new HashSet<String>();
  for(String rune:FireFlights.RUNES)for(boolean minimal:new boolean[]{false,true}) {
   var traces=new ArrayList<String>();
   for(int age:new int[]{4,8}){var trace=new ArrayList<String>();FireFlights.draw("wildercord:"+rune,age,1,Vec3.ZERO,new Vec3(0,0,1),minimal,(o,p)->{check(Double.isFinite(p.lengthSqr()) && p.length()<1,"Bounded flight "+rune);trace.add(o.toString()+"@"+p);});check(!trace.isEmpty()&&trace.size()<16,"Bounded emissions "+rune);traces.add(String.join(";",trace));}
   if(!minimal)check(prints.add(traces.getFirst()),"Distinct flight "+rune);
  }
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
     if(!name.equals("never_skip"))check(changed>10,"Visible formation differs inside aim-area region: "+name+", changed="+changed);
    }result.complete(null);
   }catch(Throwable e){result.completeExceptionally(e);}
  });return result;
 }
 private static Object field(Object value,Class<?> owner,String name){try{var f=owner.getDeclaredField(name);f.setAccessible(true);return f.get(value);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static Vec3 at(Particle p){return new Vec3(((Number)field(p,Particle.class,"x")).doubleValue(),((Number)field(p,Particle.class,"y")).doubleValue(),((Number)field(p,Particle.class,"z")).doubleValue());}
 private static List<Particle> particles(ParticleEngine engine){var found=new ArrayList<Particle>();for(var group:((Map<?,?>)field(engine,ParticleEngine.class,"particles")).values())for(var p:(Queue<?>)field(group,ParticleGroup.class,"particles"))found.add((Particle)p);for(var p:(Queue<?>)field(engine,ParticleEngine.class,"particlesToAdd"))found.add((Particle)p);return found;}
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
