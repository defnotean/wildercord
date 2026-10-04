package dev.wildercord.cast;
import dev.wildercord.client.fx.*;
import dev.wildercord.config.*;
import dev.wildercord.player.*;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.*;
import net.minecraft.client.particle.*;
import java.util.*;
/** Actual connected paid Bolt preparation and moving authored bodies in Full/Minimal; recipient acceptance is the two-client suite. */
public final class CampConcordPresentationTest implements FabricClientGameTest {
 public void runTest(ClientGameTestContext c){var quality=c.computeOnClient(mc->MagicQuality.own);var camera=c.computeOnClient(mc->mc.options.getCameraType());boolean hidden=c.computeOnClient(mc->mc.gui.hud.isHidden());int[] size=c.computeOnClient(mc->new int[]{mc.getWindow().getWidth(),mc.getWindow().getHeight()});
  try{c.runOnClient(mc->{mc.getWindow().setWindowed(1024,640);mc.resizeGui();mc.options.setCameraType(CameraType.FIRST_PERSON);if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();});
   for(String rune:List.of("watchweft","manabraid"))for(var q:List.of(MagicQuality.Level.FULL,MagicQuality.Level.MINIMAL))try(var w=c.worldBuilder().create()){
    c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set noon");var original=w.getServer().computeOnServer(s->Config.get());
    try{w.getServer().runOnServer(s->{CampConcordNative.config(CampConcordNative.copy(original,Map.of("manaRegenMultiplier",0.0)));CampConcordNative.arena(s);var p=CampConcordNative.player(s);CampConcordNative.aim(p,new net.minecraft.world.phys.Vec3(2.5,102.62,-45.5));CampConcordNative.edit(p,List.of(Runes.BOLT,Runes.get("wildercord:"+rune).orElseThrow()));Spellbooks.setMana(p,80);});c.waitTicks(4);c.runOnClient(mc->{MagicQuality.own=q;mc.particleEngine.clearParticles();});CampConcordNative.cast(c);
     String prefix="camp_"+rune+"_"+q.name().toLowerCase(Locale.ROOT);
     var prepare=capturePhase(c,prefix+"_paid_prepare",rune,12);
     var flight=capturePhase(c,prefix+"_paid_bolt",rune,5);
     // Submit real short-phase frames without waiting for GPU readback or PNG disk work.
     var frames=CompletableFuture.allOf(prepare,flight);c.waitFor(mc->frames.isDone());frames.join();
     w.getServer().runOnServer(s->CampConcordNative.check(Spellbooks.mana(CampConcordNative.player(s))<80,"Real CastSpell paid before authored formation/flight; no cosmetic packet injection"));
    }finally{w.getServer().runOnServer(s->CampConcordNative.config(original));}
   }
  }finally{c.runOnClient(mc->{MagicQuality.own=quality;mc.options.setCameraType(camera);if(mc.gui.hud.isHidden()!=hidden)mc.gui.hud.toggle();mc.getWindow().setWindowed(size[0],size[1]);mc.resizeGui();});}
 }
 private static CompletableFuture<Void> capturePhase(ClientGameTestContext c,String name,String rune,int life){
  var pending=new AtomicReference<CompletableFuture<Void>>();
  c.waitFor(mc->{
   var actual=particles(mc.particleEngine).stream().filter(p->p instanceof CampParticle&&p.isAlive()
    &&((Number)field(p,Particle.class,"lifetime")).intValue()==life
    &&((Number)field(p,Particle.class,"age")).intValue()>=1
    &&((Number)field(p,SingleQuadParticle.class,"alpha")).floatValue()>.1F).toList();
   if(actual.isEmpty())return false;
   var styles=new HashSet<Integer>();for(var p:actual)styles.add(((dev.wildercord.content.CampOption)field(p,CampParticle.class,"material")).style());
   CampConcordNative.check(styles.contains(rune.equals("watchweft")?dev.wildercord.content.CampOption.PAPER:dev.wildercord.content.CampOption.FIBER),"Actual live authored ingredient owns capture "+name+" styles="+styles);
   if(life==5){
    var bolt=java.util.stream.StreamSupport.stream(mc.level.entitiesForRendering().spliterator(),false).filter(e->e instanceof RuneBolt).map(e->(RuneBolt)e).findFirst().orElseThrow(()->new AssertionError("Real tracked paid bolt owns flight capture"));
    freshComet(mc,bolt);
   }
   System.out.println("CAMP_NATIVE_CAPTURE name="+name+" life="+life+" visibleMaterials="+actual.size()+" styles="+styles+" particleTypes="+particles(mc.particleEngine).stream().map(p->p.getClass().getSimpleName()).distinct().toList());
   var done=new CompletableFuture<Void>();pending.set(done);
   mc.gameRenderer.update(DeltaTracker.ONE);mc.gameRenderer.extract(DeltaTracker.ONE,true);mc.gameRenderer.render();
   com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder().submit();
   net.minecraft.client.Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{try(image){var path=java.nio.file.Path.of("screenshots",name+".png");java.nio.file.Files.createDirectories(path.getParent());image.writeToFile(path);done.complete(null);}catch(Throwable error){done.completeExceptionally(error);}});
   return true;
  },120);return pending.get();
 }
 private static void freshComet(Minecraft mc,RuneBolt bolt){try{
  var type=Class.forName("dev.wildercord.client.fx.BoltComets$Comet");var ctor=type.getDeclaredConstructor(net.minecraft.client.multiplayer.ClientLevel.class,RuneBolt.class);ctor.setAccessible(true);var comet=ctor.newInstance(mc.level,bolt);
  CampConcordNative.check((Boolean)field(comet,type,"authored"),"Actual pure Camp tracked bolt suppresses fallback before any Comet tick");
  CampConcordNative.check(field(comet,type,"effects").equals(bolt.getEntityData().get(RuneBolt.DATA_EFFECTS))&&((Number)field(comet,type,"color")).intValue()==bolt.getEntityData().get(RuneBolt.DATA_COLOR)&&((Number)field(comet,type,"secondary")).intValue()==bolt.getEntityData().get(RuneBolt.DATA_SECONDARY)&&((Number)field(comet,type,"style")).intValue()==bolt.getEntityData().get(RuneBolt.DATA_STYLE),"Actual tracked metadata is initialized before first extraction");
  CampConcordNative.check(!FlightBodies.covers(bolt.getEntityData().get(RuneBolt.DATA_EFFECTS)+",wildercord:harm"),"Uncovered mixed identities retain conservative generic fallback");
  // This constructor-only admission control never adds particles or changes the actual paid bolt.
 }catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static Object field(Object value,Class<?> owner,String name){try{var f=owner.getDeclaredField(name);f.setAccessible(true);return f.get(value);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static List<Particle> particles(ParticleEngine engine){var result=new ArrayList<Particle>();for(var g:((Map<?,?>)field(engine,ParticleEngine.class,"particles")).values())for(var p:(Queue<?>)field(g,ParticleGroup.class,"particles"))result.add((Particle)p);for(var p:(Queue<?>)field(engine,ParticleEngine.class,"particlesToAdd"))result.add((Particle)p);return result;}
}
