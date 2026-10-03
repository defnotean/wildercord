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
public final class FrostFlightTest implements FabricClientGameTest {
 private static int[] background;
 @Override public void runTest(ClientGameTestContext c) {
  var previous=c.computeOnClient(mc->MagicQuality.own);
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(40);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 6000");w.getServer().runCommand("weather clear");
   w.getServer().runCommand("fill -16 100 -12 16 100 40 polished_deepslate");w.getServer().runCommand("fill -12 101 32 12 109 32 gray_concrete");
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));var b=Spellbooks.get(p).withStarterGiven();for(var r:Runes.all())b=b.learn(r.id());Spellbooks.set(p,b);});c.waitTicks(15);
   c.runOnClient(mc->{mc.getWindow().setWindowed(1280,720);mc.resizeGui();if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();mc.gui.toastManager().clear();recipes();});
   var empty=c.computeOnClient(mc->snapshot(mc,"frost_flight_background"));c.waitFor(mc->empty.isDone());empty.join();
   check(FrostFlights.RUNES.size()==30,"Explicit complete frost roster");
   check(Runes.all().stream().filter(r->r.family()==dev.wildercord.spell.RuneFamily.EFFECT && r.element().equals("frost")).map(r->r.path()).collect(java.util.stream.Collectors.toSet()).equals(new HashSet<>(FrostFlights.RUNES)),"Authored runtime frost roster matches");
   for(var q:List.of(MagicQuality.Level.FULL,MagicQuality.Level.MINIMAL))for(String rune:FrostFlights.RUNES) {
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
    if(q==MagicQuality.Level.FULL){var shot=c.computeOnClient(mc->snapshot(mc,"frost_flight_"+rune));c.waitFor(mc->shot.isDone());shot.join();c.waitTicks(6);var travel=c.computeOnClient(mc->snapshot(mc,"frost_flight_travel_"+rune));c.waitFor(mc->travel.isDone());travel.join();}
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
  return particles(mc.particleEngine).stream().anyMatch(p->p.isAlive() && p instanceof MaterialParticle
   && ((Number)field(p,Particle.class,"lifetime")).intValue()==5 && at(p).distanceTo(bolt.position())<reach);
 }
 private static void recipes(){
  check(FlightBodies.covers("wildercord:frost,wildercord:tidebreath"),"Complete authored mixed group");
  for(String ids:List.of("","wildercord:frost,!","wildercord:frost,wildercord:umbra","other:frost","wildercord:frost,"))
   check(!FlightBodies.covers(ids),"Incomplete or foreign identity retains fallback: "+ids);
  check(!FrostFlights.supports("other:frost") && !FrostFlights.supports("wildercord:fire"),"Exact frost dispatch");
  var prints=new HashSet<String>();
  for(String rune:FrostFlights.RUNES)for(boolean minimal:new boolean[]{false,true}) {
   var traces=new ArrayList<String>();var materials=new HashSet<Integer>();
   for(int age:new int[]{4,8}){var trace=new ArrayList<String>();FrostFlights.draw("wildercord:"+rune,age,1,1,Vec3.ZERO,new Vec3(0,0,1),minimal,(o,p)->{check(Double.isFinite(p.lengthSqr()) && p.length()<1,"Bounded flight "+rune);if(o instanceof dev.wildercord.content.MaterialOption m)materials.add(m.style());trace.add(o.toString()+"@"+p);});check(!trace.isEmpty()&&trace.size()<16,"Bounded emissions "+rune);traces.add(String.join(";",trace));}
   if(!minimal)check(prints.add(traces.getFirst()),"Distinct flight "+rune);
   check(materials.containsAll(ingredients(rune)),"Supporting materials remain in "+rune+" minimal="+minimal);
  }
 }
 private static Set<Integer> ingredients(String rune){
  // Fusion ingredients must survive both quality modes; wet forms must keep water rather than generic ice.
  return switch(rune){
   case "black_ice" -> Set.of(FROST,VOID);
   case "blizzard" -> Set.of(FROST,WIND,VAPOUR);
   case "avalanche" -> Set.of(FROST,STONE,VAPOUR);
   case "glacier" -> Set.of(FROST,STONE);
   case "hail" -> Set.of(FROST,STORM);
   case "frostbloom" -> Set.of(FROST,PETAL);
   case "frostbite" -> Set.of(FROST,BLOOD);
   case "mirrorfrost","rime_seal" -> Set.of(FROST,ARCANE);
   case "cryostasis" -> Set.of(FROST,PETAL,TIME);
   case "rime_causeway" -> Set.of(FROST,WIND);
   case "tidebreath","tidecall","tidewrit" -> Set.of(WATER,VAPOUR);
   case "undertow" -> Set.of(WATER,STONE);
   case "flash_freeze" -> Set.of(WATER,FROST);
   case "bubble","current","drowning_word","tidal_lift","tidehook" -> Set.of(WATER);
   default -> Set.of(FROST);
  };
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
