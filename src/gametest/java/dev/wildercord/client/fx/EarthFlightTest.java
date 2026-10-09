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
public final class EarthFlightTest implements FabricClientGameTest {
 private static int[] background;
 private static int cameraId;
 @Override public void runTest(ClientGameTestContext c) {
  var previous=c.computeOnClient(mc->MagicQuality.own);
  var previousCamera=c.computeOnClient(mc->mc.options.getCameraType());
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(40);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 6000");w.getServer().runCommand("weather clear");
   w.getServer().runCommand("fill -16 100 -12 16 100 40 polished_deepslate");w.getServer().runCommand("fill -12 101 32 12 109 32 gray_concrete");
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));var b=Spellbooks.get(p).withStarterGiven();for(var r:Runes.all())b=b.learn(r.id());Spellbooks.set(p,b);var camera=net.minecraft.world.entity.EntityTypes.TEXT_DISPLAY.create(s.overworld(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);camera.snapTo(3,102,10,90,0);camera.setNoGravity(true);camera.setInvisible(true);s.overworld().addFreshEntity(camera);cameraId=camera.getId();});c.waitTicks(15);
   c.runOnClient(mc->{mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);mc.getWindow().setWindowed(1280,720);mc.resizeGui();if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();mc.gui.toastManager().clear();recipes();
    System.out.println("WILDERCORD_EARTH_FLIGHT_VIEW "+new com.google.gson.Gson().toJson(Map.of("phase","setup","incomingCameraType",previousCamera.name(),"cameraType",mc.options.getCameraType().name())));});
   var empty=c.computeOnClient(mc->snapshot(mc,"earth_flight_background"));c.waitFor(mc->empty.isDone());empty.join();
   check(EarthForms.RUNES.size()==33,"Explicit complete earth roster");
   check(EverydayRunes.combatPaths("earth").equals(new HashSet<>(EarthForms.RUNES)),"Authored runtime earth roster matches");
   for(var q:List.of(MagicQuality.Level.FULL,MagicQuality.Level.MINIMAL))for(String rune:EarthForms.RUNES) {
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
    if(q==MagicQuality.Level.FULL){var shot=c.computeOnClient(mc->snapshot(mc,"earth_flight_"+rune));c.waitFor(mc->shot.isDone());shot.join();c.waitTicks(2);var travel=c.computeOnClient(mc->snapshot(mc,"earth_flight_travel_"+rune));c.waitFor(mc->travel.isDone());travel.join();c.runOnClient(EarthFlightTest::sideCamera);c.waitTicks(2);var side=c.computeOnClient(mc->sideCapture(mc,"earth_view_"+rune));c.waitFor(mc->side.isDone());side.join();}
    c.runOnClient(mc->mc.particleEngine.clearParticles());c.waitTicks(4);
    c.runOnClient(mc->{var b=mc.level.entitiesForRendering().iterator();RuneBolt live=null;while(b.hasNext()){var e=b.next();if(e instanceof RuneBolt bolt)live=bolt;}check(live!=null,"Live after particle clear");check(authoredNear(mc,live),"Live entity restarts authored emission after particle clear: "+rune);});
   }
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.level().getEntitiesOfClass(RuneBolt.class,p.getBoundingBox().inflate(64)).forEach(net.minecraft.world.entity.Entity::discard);});c.waitTicks(8);
   c.runOnClient(mc->check(((Set<?>)field(null,BoltComets.class,"DRAWN")).isEmpty(),"Removed entity IDs retire independently of particle lifetime"));
  }finally{c.runOnClient(mc->{MagicQuality.own=previous;mc.options.setCameraType(previousCamera);});}
 }
 static boolean authoredNear(net.minecraft.client.Minecraft mc,RuneBolt bolt){
  // Two emission ticks plus the authored body offset; network movement may arrive after the latest client emission.
  double reach=1+2*bolt.getDeltaMovement().length();
  return particles(mc.particleEngine).stream().anyMatch(p->p.isAlive() && p instanceof EarthParticle
   && ((Number)field(p,Particle.class,"lifetime")).intValue()==5 && at(p).distanceTo(bolt.position())<reach);
 }
 private static void recipes(){
  check(FlightBodies.covers("wildercord:pelt,wildercord:tremor"),"Complete authored mixed group");
  for(String ids:List.of("","wildercord:pelt,!","wildercord:pelt,wildercord:harm","other:pelt","wildercord:pelt,"))
   check(!FlightBodies.covers(ids),"Incomplete or foreign identity retains fallback: "+ids);
  check(!EarthForms.supports("other:pelt") && !EarthForms.supports("wildercord:fire"),"Exact earth dispatch");
  var prints=new HashSet<String>();
  for(String rune:EarthForms.RUNES)for(boolean minimal:new boolean[]{false,true}) {
   var traces=new ArrayList<String>();var materials=new HashSet<Integer>();
   for(int age:new int[]{4,8,12}){var trace=new ArrayList<String>();final int[] currents={0};EarthFlights.draw("wildercord:"+rune,age,1,1,Vec3.ZERO,new Vec3(0,0,1),minimal,(o,p)->{check(Double.isFinite(p.lengthSqr()) && p.length()<1,"Bounded flight "+rune);if(o instanceof dev.wildercord.content.MaterialOption m)materials.add(m.style());if(o instanceof dev.wildercord.content.EarthOption rock){currents[0]++;check(rock.lifetime()==5,"Short physical body lifetime");}check(!(o instanceof dev.wildercord.content.LightOption),"Earth recipes do not use luminous lines");trace.add(o.toString()+"@"+p);});check(currents[0]>0,"Dedicated physical earth material in every spell");check(!trace.isEmpty()&&trace.size()<16,"Bounded emissions "+rune);traces.add(String.join(";",trace));}
   check(new HashSet<>(traces).size() > 1,"Authored motion evolves: "+rune+" minimal="+minimal);
   if(!minimal)check(prints.add(traces.getFirst()),"Distinct flight "+rune);
   check(materials.containsAll(ingredients(rune)),"Supporting materials remain in "+rune+" minimal="+minimal);
  }
 }
 private static Set<Integer> ingredients(String rune){
  return switch(rune){
   case "magma" -> Set.of(EMBER);
   case "sinkhole" -> Set.of(VOID);
   case "geode","prospect" -> Set.of(ARCANE);
   case "fossilize","clockroot" -> Set.of(TIME);
   case "bonespur" -> Set.of(BLOOD);
   case "thunderquake" -> Set.of(STORM);
   case "mire" -> Set.of(WATER);
   default -> Set.of();
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
  var live=particles(mc.particleEngine).stream().filter(p->p.isAlive() && (p instanceof EarthParticle || p instanceof MaterialParticle)
    && ((Number)field(p,Particle.class,"lifetime")).intValue()==5).toList();
  check(live.stream().anyMatch(p->p instanceof EarthParticle),"Actual production earth retained for close view");
  var views=new ArrayList<CloseViewReceipt>(2);
  try {
   mc.particleEngine.clearParticles();var empty=capturePixels(mc,name+"_background",camera,bolt,views);
   for(var particle:live)mc.particleEngine.add(particle);
   mc.particleEngine.tick();
   var drawn=capturePixels(mc,name,camera,bolt,views);
   return empty.thenCombine(drawn,(a,b)->{
    check(a.length==b.length,"Matched close-view pixel dimensions: "+name);
    int changed=0;for(int i=0;i<a.length;i++){int x=a[i],y=b[i];int d=Math.abs((x>>16&255)-(y>>16&255))+Math.abs((x>>8&255)-(y>>8&255))+Math.abs((x&255)-(y&255));if(d>20)changed++;}
    if(changed<=10 || name.equals("earth_view_shield"))System.out.println("WILDERCORD_EARTH_FLIGHT_VIEW "+new com.google.gson.Gson().toJson(Map.of("name",name,"phase","comparison","changedPixels",changed,"emptyPixels",a.length,"drawnPixels",b.length,"views",views)));
    check(changed>10,"Isolated close flight changes visible pixels: "+name+" changed="+changed);return (Void)null;
   });
  } finally {mc.setCameraEntity(mc.player);}
 }
 static java.util.concurrent.CompletableFuture<int[]> capturePixels(net.minecraft.client.Minecraft mc,String name){
  return capturePixels(mc,name,null,null,null);
 }
 private static java.util.concurrent.CompletableFuture<int[]> capturePixels(net.minecraft.client.Minecraft mc,String name,net.minecraft.world.entity.Entity reviewCamera,RuneBolt bolt,List<CloseViewReceipt> views){
  var result=new java.util.concurrent.CompletableFuture<int[]>();
  mc.gameRenderer.update(net.minecraft.client.DeltaTracker.ONE);
  if(reviewCamera!=null){
   var view=closeViewReceipt(mc,name,reviewCamera,bolt);views.add(view);
   boolean aligned=mc.options.getCameraType()==net.minecraft.client.CameraType.FIRST_PERSON
    && mc.gameRenderer.mainCamera().entity()==reviewCamera && !view.detached()
    && Math.abs(net.minecraft.util.Mth.wrapDegrees(view.yaw()-90))<.001F && Math.abs(view.pitch())<.001F;
   boolean matched=views.size()==1 || views.getFirst().position().equals(view.position())
    && views.getFirst().yaw()==view.yaw() && views.getFirst().pitch()==view.pitch();
   if(!aligned || !matched)System.out.println("WILDERCORD_EARTH_FLIGHT_VIEW "+new com.google.gson.Gson().toJson(views));
   check(aligned,"Actual review camera uses the authored first-person side view: "+name);
   check(matched,"Matched actual camera pose across close-view captures: "+name);
  }
  mc.gameRenderer.extract(net.minecraft.client.DeltaTracker.ONE,true);mc.gameRenderer.render();
  com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder().submit();
  net.minecraft.client.Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{try(image){var path=java.nio.file.Path.of("screenshots",name+".png");java.nio.file.Files.createDirectories(path.getParent());image.writeToFile(path);result.complete(image.getPixels());}catch(Throwable e){result.completeExceptionally(e);}});
  return result;
 }
 // Passive receipts of the existing camera and particles after the original update. No extra tick or extract.
 private record ParticleView(String type,int age,int lifetime,float alpha,Vec3 position,double reach,boolean inFrustum){}
 private record CloseViewReceipt(String name,String phase,long gameTime,int boltId,int boltAge,Vec3 boltPosition,Vec3 boltVelocity,
  String cameraType,int expectedEntityId,int actualEntityId,boolean detached,Vec3 expectedPosition,Vec3 position,float yaw,float pitch,
  int fov,String ownQuality,String otherQuality,boolean reducedFlash,int particleCount,int positiveAlphaInFrustum,List<ParticleView> particles){}
 private static CloseViewReceipt closeViewReceipt(net.minecraft.client.Minecraft mc,String name,net.minecraft.world.entity.Entity expected,RuneBolt bolt){
  var camera=mc.gameRenderer.mainCamera();var samples=new ArrayList<ParticleView>();int count=0,visible=0;
  // Match the native LevelExtractor's expanded copy; never modify the camera's own frustum.
  var particleFrustum=new net.minecraft.client.renderer.culling.Frustum(camera.getCullFrustum()).offset(-3.0F);
  for(var particle:particles(mc.particleEngine)){
   if(!(particle instanceof EarthParticle || particle instanceof MaterialParticle))continue;
   count++;var position=at(particle);double reach=((SigilGroup.Extent)particle).reach();
   boolean inFrustum=particleFrustum.isVisible(new net.minecraft.world.phys.AABB(position.x-reach,position.y-reach,position.z-reach,position.x+reach,position.y+reach,position.z+reach));
   float alpha=((Number)field(particle,SingleQuadParticle.class,"alpha")).floatValue();
   if(particle.isAlive() && alpha>0 && inFrustum)visible++;
   if(samples.size()<24)samples.add(new ParticleView(particle.getClass().getSimpleName(),
    ((Number)field(particle,Particle.class,"age")).intValue(),((Number)field(particle,Particle.class,"lifetime")).intValue(),alpha,position,reach,inFrustum));
  }
  return new CloseViewReceipt(name,"after_update_before_extract",mc.level.getGameTime(),bolt.getId(),bolt.tickCount,bolt.position(),bolt.getDeltaMovement(),
   mc.options.getCameraType().name(),expected.getId(),camera.entity()==null?-1:camera.entity().getId(),camera.isDetached(),expected.position(),camera.position(),camera.yRot(),camera.xRot(),
   mc.options.fov().get(),MagicQuality.own.name(),MagicQuality.others.name(),MagicQuality.reducedFlash,count,visible,List.copyOf(samples));
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
