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
import static dev.wildercord.content.MaterialOption.*;

/** Paid native Bolt launches, synchronized effect identity and real flight particles. */
public final class WindFlightTest implements FabricClientGameTest {
 private static int[] background;
 private static int cameraId;
 @Override public void runTest(ClientGameTestContext c) {
  var previous=c.computeOnClient(mc->MagicQuality.own);
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(40);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 6000");w.getServer().runCommand("weather clear");
   w.getServer().runCommand("fill -16 100 -12 16 100 40 polished_deepslate");w.getServer().runCommand("fill -12 101 32 12 109 32 gray_concrete");
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));var b=Spellbooks.get(p).withStarterGiven();for(var r:Runes.all())b=b.learn(r.id());Spellbooks.set(p,b);var camera=net.minecraft.world.entity.EntityTypes.TEXT_DISPLAY.create(s.overworld(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);camera.snapTo(3,102,10,90,0);camera.setNoGravity(true);camera.setInvisible(true);s.overworld().addFreshEntity(camera);cameraId=camera.getId();});c.waitTicks(15);
   c.runOnClient(mc->{mc.getWindow().setWindowed(1280,720);mc.resizeGui();if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();mc.gui.toastManager().clear();recipes();});
   var empty=c.computeOnClient(mc->snapshot(mc,"wind_flight_background"));c.waitFor(mc->empty.isDone());empty.join();
   check(WindForms.RUNES.size()==27,"Explicit complete wind roster");
   check(Runes.all().stream().filter(r->r.family()==dev.wildercord.spell.RuneFamily.EFFECT && r.element().equals("wind")).map(r->r.path()).collect(java.util.stream.Collectors.toSet()).equals(new HashSet<>(WindForms.RUNES)),"Authored runtime wind roster matches");
   for(var q:List.of(MagicQuality.Level.FULL,MagicQuality.Level.MINIMAL))for(String rune:WindForms.RUNES) {
    w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.level().getEntitiesOfClass(RuneBolt.class,p.getBoundingBox().inflate(64)).forEach(net.minecraft.world.entity.Entity::discard);});c.waitTicks(12);
    c.runOnClient(mc->{mc.particleEngine.clearParticles();MagicQuality.own=q;});
    w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(SpellCaster.edit(p,0,List.of(Runes.BOLT.id(),"wildercord:"+rune))==null,"Accepted edit: "+rune);Spellbooks.setMana(p,100);Spellbooks.setReadyAt(p,0,0);float before=Spellbooks.mana(p);
    if(Runes.innate(Runes.get("wildercord:"+rune).orElseThrow())) {
     p.setAttached(dev.wildercord.player.WildercordAttachments.INNATE,"");SpellCaster.cast(p,0);check(Spellbooks.mana(p)==before,"Foreign innate refused before payment");
     p.setAttached(dev.wildercord.player.WildercordAttachments.INNATE,"wildercord:"+rune);
    }
    boolean free=WildSurge.freeRecast(p,p.level().getGameTime());var active=SpellCaster.activeRunes(Spellbooks.get(p),0,Spellbooks.tier(p));var plan=dev.wildercord.spell.SpellCompiler.compile(active);int price=dev.wildercord.player.Heart.manaCost(p,plan);SpellCaster.cast(p,0);check(Spellbooks.mana(p)<before,"Paid Bolt/"+rune+" alive="+p.isAlive()+" locked="+CastLock.locked(p)+" free="+free+" cost="+price+" empty="+plan.isEmpty()+" active="+active+" ready="+Spellbooks.readyAt(p,0)+" now="+p.level().getGameTime()+" mana="+Spellbooks.mana(p)+" runes="+Spellbooks.get(p).spells().get(0));});
    c.waitTicks(8);
    c.runOnClient(mc->{var bolts=new ArrayList<RuneBolt>();for(var e:mc.level.entitiesForRendering())if(e instanceof RuneBolt b)bolts.add(b);check(!bolts.isEmpty(),"Live actual Bolt for "+rune);var b=bolts.getFirst();check(b.getEntityData().get(RuneBolt.DATA_EFFECTS).equals("wildercord:"+rune),"Synced exact flight identity");check(b.position().distanceTo(mc.player.position())>2,"Actual flight advances");check(authoredNear(mc,b),"Authored five-tick material follows real projectile "+rune);});
    if(q==MagicQuality.Level.FULL){var shot=c.computeOnClient(mc->snapshot(mc,"wind_flight_"+rune));c.waitFor(mc->shot.isDone());shot.join();c.waitTicks(2);var travel=c.computeOnClient(mc->snapshot(mc,"wind_flight_travel_"+rune));c.waitFor(mc->travel.isDone());travel.join();c.runOnClient(WindFlightTest::sideCamera);c.waitTicks(2);var side=c.computeOnClient(mc->sideCapture(mc,"wind_view_"+rune));c.waitFor(mc->side.isDone());side.join();}
    c.runOnClient(mc->mc.particleEngine.clearParticles());c.waitTicks(4);
    c.runOnClient(mc->{var b=mc.level.entitiesForRendering().iterator();RuneBolt live=null;while(b.hasNext()){var e=b.next();if(e instanceof RuneBolt bolt)live=bolt;}check(live!=null,"Live after particle clear");check(authoredNear(mc,live),"Live entity restarts authored emission after particle clear: "+rune);});
   }
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.level().getEntitiesOfClass(RuneBolt.class,p.getBoundingBox().inflate(64)).forEach(net.minecraft.world.entity.Entity::discard);});c.waitTicks(8);
   c.runOnClient(mc->check(((Set<?>)field(null,BoltComets.class,"DRAWN")).isEmpty(),"Removed entity IDs retire independently of particle lifetime"));
  }finally{c.runOnClient(mc->MagicQuality.own=previous);}
 }
 static boolean authoredNear(net.minecraft.client.Minecraft mc,RuneBolt bolt){
  // Two emission ticks plus the authored body offset; network movement may arrive after the latest client emission.
  double reach=1+2*bolt.getDeltaMovement().length();
  return particles(mc.particleEngine).stream().anyMatch(p->p.isAlive() && p instanceof AirflowParticle
   && ((Number)field(p,Particle.class,"lifetime")).intValue()==5 && at(p).distanceTo(bolt.position())<reach);
 }
 private static void recipes(){
  ThresherwindRecipeChecks.verify();
  check(FlightBodies.covers("wildercord:windcut,wildercord:cyclone"),"Complete authored mixed group");
  for(String ids:List.of("","wildercord:windcut,!","wildercord:windcut,wildercord:harm","other:windcut","wildercord:windcut,"))
   check(!FlightBodies.covers(ids),"Incomplete or foreign identity retains fallback: "+ids);
  check(!WindForms.supports("other:windcut") && !WindForms.supports("wildercord:fire"),"Exact wind dispatch");
  var prints=new HashSet<String>();
  for(String rune:WindForms.RUNES)for(boolean minimal:new boolean[]{false,true}) {
   var traces=new ArrayList<String>();var materials=new HashSet<Integer>();
   for(int age:new int[]{4,8,12}){var trace=new ArrayList<String>();final int[] currents={0};WindForms.flight("wildercord:"+rune,age,1,1,Vec3.ZERO,new Vec3(0,0,1),minimal,(o,p)->{check(Double.isFinite(p.lengthSqr()) && p.length()<1,"Bounded flight "+rune);if(o instanceof dev.wildercord.content.MaterialOption m)materials.add(m.style());if(o instanceof dev.wildercord.content.AirflowOption flow){currents[0]++;for(double f:new double[]{0,.25,.5,.75,1})check(Double.isFinite(AirflowParticle.point(flow,f).lengthSqr()),"Finite curve samples");}check(!(o instanceof dev.wildercord.content.LightOption),"Wind recipes do not use luminous lines");trace.add(o.toString()+"@"+p);});check(currents[0]>0,"Dedicated air curve in every spell");check(!trace.isEmpty()&&trace.size()<16,"Bounded emissions "+rune);traces.add(String.join(";",trace));}
   check(new HashSet<>(traces).size() > 1,"Authored motion evolves: "+rune+" minimal="+minimal);
   if(!minimal)check(prints.add(traces.getFirst()),"Distinct flight "+rune);
   check(materials.containsAll(ingredients(rune)),"Supporting materials remain in "+rune+" minimal="+minimal);
  }
 }
 private static Set<Integer> ingredients(String rune){
  return switch(rune){
   case "dust_devil","downdraft" -> Set.of(WIND,STONE);
   case "recoil" -> Set.of(WIND,TIME);
   case "razorgale" -> Set.of(WIND,BLOOD);
   case "prune","zephyr","thresherwind" -> Set.of(WIND,PETAL);
   case "skylatch" -> Set.of(WIND,VOID);
   case "skyglyph" -> Set.of(WIND,ARCANE);
   case "summit_wind" -> Set.of(WIND,VAPOUR);
   default -> Set.of(WIND);
  };
 }
 static void sideCamera(net.minecraft.client.Minecraft mc){
  RuneBolt bolt=null;for(var e:mc.level.entitiesForRendering())if(e instanceof RuneBolt b)bolt=b;
  check(bolt!=null,"Paid projectile before native side camera");
  var camera=mc.level.getEntity(cameraId);check(camera!=null,"Synced camera");
  camera.snapTo(bolt.getX()+2.7,bolt.getY()-camera.getEyeHeight(),bolt.getZ()+2,90,0);
  mc.setCameraEntity(camera);
 }
 // Matched diagnostic close view of the actual paid projectile's production flight particles.
 // Empty and populated captures are submitted in one client step, preserving the camera pose; one native particle-engine step admits the retained production particles.
 static java.util.concurrent.CompletableFuture<Void> sideCapture(net.minecraft.client.Minecraft mc,String name){
  RuneBolt bolt=null;for(var e:mc.level.entitiesForRendering())if(e instanceof RuneBolt b)bolt=b;
  check(bolt!=null,"Live paid projectile for close view");
  var camera=mc.level.getEntity(cameraId);check(camera!=null,"Synced review camera");
  var live=particles(mc.particleEngine).stream().filter(p->p.isAlive() && (p instanceof AirflowParticle || p instanceof MaterialParticle)
    && ((Number)field(p,Particle.class,"lifetime")).intValue()==5).toList();
  check(live.stream().anyMatch(p->p instanceof AirflowParticle),"Actual production airflow retained for close view");
  mc.particleEngine.clearParticles();var empty=capturePixels(mc,name+"_background");
  for(var particle:live)mc.particleEngine.add(particle);
  mc.particleEngine.tick();
  var drawn=capturePixels(mc,name);mc.setCameraEntity(mc.player);
  return empty.thenCombine(drawn,(a,b)->{
   int changed=0;for(int i=0;i<a.length;i++){int x=a[i],y=b[i];int d=Math.abs((x>>16&255)-(y>>16&255))+Math.abs((x>>8&255)-(y>>8&255))+Math.abs((x&255)-(y&255));if(d>20)changed++;}
   check(changed>10,"Isolated close flight changes visible pixels: "+name+" changed="+changed);return (Void)null;
  });
 }
 static java.util.concurrent.CompletableFuture<int[]> capturePixels(net.minecraft.client.Minecraft mc,String name){
  var result=new java.util.concurrent.CompletableFuture<int[]>();
  mc.gameRenderer.update(net.minecraft.client.DeltaTracker.ONE);mc.gameRenderer.extract(net.minecraft.client.DeltaTracker.ONE,true);mc.gameRenderer.render();
  com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder().submit();
  net.minecraft.client.Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{try(image){var path=java.nio.file.Path.of("screenshots",name+".png");java.nio.file.Files.createDirectories(path.getParent());image.writeToFile(path);result.complete(image.getPixels());}catch(Throwable e){result.completeExceptionally(e);}});
  return result;
 }
 static java.util.concurrent.CompletableFuture<Void> snapshot(net.minecraft.client.Minecraft mc,String name) {
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
 static Object field(Object value,Class<?> owner,String name){try{var f=owner.getDeclaredField(name);f.setAccessible(true);return f.get(value);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 static Vec3 at(Particle p){return new Vec3(((Number)field(p,Particle.class,"x")).doubleValue(),((Number)field(p,Particle.class,"y")).doubleValue(),((Number)field(p,Particle.class,"z")).doubleValue());}
 static List<Particle> particles(ParticleEngine engine){var found=new ArrayList<Particle>();for(var group:((Map<?,?>)field(engine,ParticleEngine.class,"particles")).values())for(var p:(Queue<?>)field(group,ParticleGroup.class,"particles"))found.add((Particle)p);for(var p:(Queue<?>)field(engine,ParticleEngine.class,"particlesToAdd"))found.add((Particle)p);return found;}
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
