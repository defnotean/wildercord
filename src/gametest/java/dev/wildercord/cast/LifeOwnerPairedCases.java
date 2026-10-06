package dev.wildercord.cast;

import dev.wildercord.client.fx.LifeParticle;
import dev.wildercord.client.fx.MagicQuality;
import dev.wildercord.client.fx.MaterialParticle;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Runes;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Four explicit additions to the supervised two-process ledger, never hidden in its existing cases.
 * The supervisor must acknowledge preparePeer before payment, then call finishPeer on ready.
 * A tick sampler preserves independent evidence while the original 10/8-tick particles are alive.
 */
public final class LifeOwnerPairedCases {
 public static final List<String> CASES=List.of("LIFE_SELF_HEAL_FIRST_PERSON","LIFE_SELF_HEAL_THIRD_PERSON",
  "LIFE_SELF_HEAL_CAMERA_TRANSITIONS","LIFE_SELF_SECOND_WIND_REDUCED_FLASH");
 private static Capture active;
 private static boolean registered;
 private volatile LifeOwnerEvents.Event outcome;

 public void runConnectedPair(ClientGameTestContext c,TestServerContext server,UUID host,UUID peer,
   BiConsumer<String,Map<String,String>> preparePeer,BiConsumer<String,Map<String,String>> observed,Consumer<String> completed){
  check(!host.equals(peer),"Two distinct connected profiles required for Life comparisons");
  var modes=server.computeOnServer(s->List.of(connected(s,host).gameMode.getGameModeForPlayer(),connected(s,peer).gameMode.getGameModeForPlayer()));
  String innate=server.computeOnServer(s->connected(s,host).getAttached(WildercordAttachments.INNATE));
  try{
   for(String id:CASES){
    server.waitFor(s->!CastLock.locked(connected(s,host))&&!dev.wildercord.aura.MastersArts.committed(connected(s,host)),NextSignatureRules.REST+5);
    outcome=null;
    server.runOnServer(s->{var actor=connected(s,host);var viewer=connected(s,peer);
     for(var pos:BlockPos.betweenClosed(-6,101,-6,6,107,8))actor.level().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
     for(var pos:BlockPos.betweenClosed(-6,100,-6,6,100,8))actor.level().setBlockAndUpdate(pos,Blocks.POLISHED_DEEPSLATE.defaultBlockState());
     NextSignatureNative.teach(actor);NextSignatureNative.teach(viewer);
     check(actor.teleportTo(actor.level(),.5,101,.5,Set.of(),0,0,false),"Actual connected owner reaches fixture before payment");
     check(viewer.teleportTo(actor.level(),3.5,101,3.5,Set.of(),135,10,false),"Actual second viewer reaches a separate clear viewpoint away from the owner's front camera");
     actor.setDeltaMovement(Vec3.ZERO);viewer.setDeltaMovement(Vec3.ZERO);actor.removeAllEffects();viewer.removeAllEffects();actor.setAbsorptionAmount(0);actor.setHealth(4);
     LifeOwnerEvents.observe(e->{if(e.rune().equals(id.equals(CASES.get(3))?"second_wind":"heal")
       &&e.moment()==LifeOwnerEvents.Moment.APPLY){
      check(outcome==null&&host.equals(e.source())&&host.equals(e.recipient()),"One real self event preserves exact connected source/recipient");outcome=e;
     }});
    });c.waitTicks(3);
    arm(c,id,host,true);
    preparePeer.accept(id,Map.of("case",id,"lifeSource",host.toString(),"lifeOwnerEntity",Integer.toString(server.computeOnServer(s->connected(s,host).getId()))));
    // preparePeer is a barrier: the other real JVM has already configured and armed its sampler.
    server.runOnServer(s->{var actor=connected(s,host);var rune=id.equals(CASES.get(3))?Runes.get("wildercord:second_wind").orElseThrow():Runes.HEAL;
     if(id.equals(CASES.get(3)))actor.setAttached(WildercordAttachments.INNATE,rune.id());NextSignatureNative.cast(actor,Runes.SELF,rune);
    });c.waitTicks(5);
    check(outcome!=null,"Genuine paid event reaches independent presentation comparison");
    var proof=proof(id,outcome);
    if(!id.equals(CASES.get(0)))c.waitFor(mc->active!=null&&!active.resident(mc).isEmpty(),5);
    c.runOnClient(mc->verifyCamera(mc,host));
    c.takeScreenshot(TestScreenshotOptions.of("life_paired_owner_"+id.toLowerCase(Locale.ROOT)).disableCounterPrefix());
    if(id.equals(CASES.get(2)))transitions(c);
    Map<String,String> owner=finish(c,id,proof,true);proof.putAll(owner);
    observed.accept(id,Map.copyOf(proof));
    server.runOnServer(s->{var actor=connected(s,host);check(actor.isAlive()&&(id.equals(CASES.get(3))?actor.getHealth()==4:actor.getHealth()>4)
      &&actor.gameMode.getGameModeForPlayer()==GameType.SURVIVAL&&connected(s,peer).gameMode.getGameModeForPlayer()==GameType.SURVIVAL,"Both exact connected bodies remain Survival after genuine self admission");
     if(id.equals(CASES.get(3)))check(((Map<?,?>)field(null,FusedLife.class,"WINDS")).containsKey(host),"Actual Second Wind owner holds its paid finite ward; this comparison captures original APPLY");
    });
    completed.accept(id);LifeOwnerEvents.clear();c.waitTicks(12);
   }
  }finally{
   disarm(c);LifeOwnerEvents.clear();outcome=null;
   server.runOnServer(s->{connected(s,host).setAttached(WildercordAttachments.INNATE,innate);connected(s,host).setGameMode(modes.get(0));connected(s,peer).setGameMode(modes.get(1));});
  }
 }
 /** The peer must call this before acknowledging the supervisor's prepare request. */
 public static void armPeer(ClientGameTestContext c,String id,UUID source){arm(c,id,source,false);}
 /** After writing armed, the peer waits for its actual live material and captures its own framebuffer.
  * This must run before the later ready/seen exchange; packet evidence alone is not a screenshot.
  */
 public static void capturePeer(ClientGameTestContext c,String id){
  c.waitFor(mc->active!=null&&active.id.equals(id)&&!active.owner&&!active.resident(mc).isEmpty(),120);
  c.runOnClient(mc->{check(active!=null&&active.id.equals(id)&&extracted(mc,active.resident(mc))>0,"Independent viewer captures original material already inserted in the native render group");verifyCamera(mc,active.source);});
  c.takeScreenshot(TestScreenshotOptions.of("life_paired_peer_"+id.toLowerCase(Locale.ROOT)).disableCounterPrefix());
 }
 /** Returns only evidence independently read by this JVM; the supervisor compares it to host proof. */
 public static Map<String,String> finishPeer(ClientGameTestContext c,String id,Map<String,String> proof){return finish(c,id,proof,false);}
 public static void abortPeer(ClientGameTestContext c){disarm(c);}

 private static void arm(ClientGameTestContext c,String id,UUID source,boolean owner){
  check(CASES.contains(id),"Declared Life comparison ID required");
  c.runOnClient(mc->{check(active==null&&mc.player!=null&&mc.level!=null,"No overlapping Life comparison sampler");
   check(owner==mc.player.getUUID().equals(source),"Owner and second viewer roles follow their actual profiles");
   if(!registered){ClientTickEvents.END_CLIENT_TICK.register(client->{if(active!=null)active.sample(client);});registered=true;}
   active=new Capture(mc,id,source,owner);mc.setCameraEntity(mc.player);
   mc.options.setCameraType(owner&&!id.equals(CASES.get(0))?CameraType.THIRD_PERSON_FRONT:CameraType.FIRST_PERSON);
   MagicQuality.own=owner?MagicQuality.Level.FULL:MagicQuality.Level.BALANCED;
   MagicQuality.others=MagicQuality.Level.MINIMAL;MagicQuality.reducedFlash=id.equals(CASES.get(3));
   mc.particleEngine.clearParticles();
  });
  c.waitTicks(1); // Native camera settles before payment.
 }
 private static Map<String,String> finish(ClientGameTestContext c,String id,Map<String,String> proof,boolean owner){
  return c.computeOnClient(mc->{var capture=active;check(capture!=null&&capture.id.equals(id)&&capture.owner==owner,"Exact independently armed Life comparison");
   capture.sample(mc);
   // Rear transition samples prove clearance/extraction only: the owner's body may occlude its
   // front-facing material. The earlier actual front-view screenshot has its separate ray proof.
   if(!(owner&&id.equals(CASES.get(2))))verifyCamera(mc,capture.source);
   int expected=Integer.parseInt(proof.get(owner?"lifeFullPieces":"lifeMinimalPieces"));
   if(owner&&id.equals(CASES.get(0)))check(capture.peak==0&&capture.seen.isEmpty(),"Actual first-person owner never admits self Heal particles");
   else{
    check(capture.peak==expected&&capture.seen.size()==expected,"Independent receiver retains exactly one authored source-owned body: "+capture.peak+"/"+expected);
    check(capture.extracted>0,"Actual native camera extracted original live outcome material");
    if(id.equals(CASES.get(3)))check(capture.time>0&&capture.reducedSamples>0,"Reduced-flash comparison retains Time support and reduced Life alpha");
   }
   String prefix=owner?"lifeOwner":"lifePeer";
   var result=Map.of(prefix+"Source",capture.source.toString(),prefix+"Pieces",Integer.toString(capture.peak),
    prefix+"Unique",Integer.toString(capture.seen.size()),prefix+"Extracted",Integer.toString(capture.extracted),
    prefix+"CameraNative","true",prefix+"Time",Integer.toString(capture.time),prefix+"ReducedSamples",Integer.toString(capture.reducedSamples));
   capture.restore(mc);active=null;return result;
  });
 }
 private static void transitions(ClientGameTestContext c){
  c.runOnClient(mc->{check(active!=null&&active.owner,"Live owner sampler required for transition");
   var before=active.current(mc);check(!before.isEmpty(),"Original real self particles are still alive before view switch");
   mc.options.setCameraType(CameraType.FIRST_PERSON);check(extracted(mc,before)==0,"Existing owner particles suppress immediately on third-to-first switch");
   check(active.current(mc).equals(before),"Immediate camera switch neither re-emits nor removes particles");
  });c.waitTicks(1);
  c.runOnClient(mc->{check(extracted(mc,active.current(mc))==0&&!mc.gameRenderer.mainCamera().isDetached(),"Actual first-person camera keeps surviving particles out of view");mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);});c.waitTicks(1);
  c.runOnClient(mc->check(extracted(mc,active.current(mc))>0&&mc.gameRenderer.mainCamera().isDetached(),"Native third-person return extracts the same unextended particles"));
 }
 private static void disarm(ClientGameTestContext c){c.runOnClient(mc->{if(active!=null){active.restore(mc);active=null;}});}
 private static void verifyCamera(Minecraft mc,UUID source){
  var owner=mc.level.getPlayerByUUID(source);check(owner!=null,"Actual source body is tracked by this independent client");
  var eye=mc.gameRenderer.mainCamera().position();var focus=owner.getBoundingBox().getCenter().add(0,0,.5);
  check(mc.getCameraEntity()==mc.player&&mc.gameRenderer.mainCamera().entity()==mc.player,"Actual local player remains camera entity");
  check(mc.level.clip(new ClipContext(eye,focus,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player)).getType()==HitResult.Type.MISS,"Actual camera-to-original self material ray is unobstructed by blocks");
  check(mc.level.getEntities(mc.player,new AABB(eye,focus).inflate(.01),body->body instanceof net.minecraft.world.entity.LivingEntity&&body.isAlive()&&!body.isSpectator())
   .stream().noneMatch(body->body.getBoundingBox().contains(eye)||body.getBoundingBox().clip(eye,focus).isPresent()),"Actual second viewer and source bodies do not occlude the material focus or occupy the camera");
 }
 private static HashMap<String,String> proof(String id,LifeOwnerEvents.Event event){
  var proof=new HashMap<String,String>();proof.put("caseLife",id);proof.put("lifeSource",event.source().toString());
  proof.put("lifeMoment",event.moment().name());proof.put("lifeTick",Long.toString(event.tick()));
  for(boolean minimal:new boolean[]{false,true}){int[] count={0};LifeOutcomes.draw(new LifeOutcomes.Observation(event.rune(),LifeOutcomes.Moment.valueOf(event.moment().name()),event.anchor(),event.secondary(),event.units(),event.delta(),0,event.normal(),event.standoff()),minimal,(o,at)->count[0]++);
   proof.put(minimal?"lifeMinimalPieces":"lifeFullPieces",Integer.toString(count[0]));}
  return proof;
 }
 private static final class Capture {
  final String id;final UUID source;final boolean owner;final ClientLevel world;
  final net.minecraft.client.player.LocalPlayer body;
  final CameraType camera;final MagicQuality.Level own,others;final boolean reduced;
  final Set<Particle> seen=Collections.newSetFromMap(new IdentityHashMap<>());
  int peak,extracted,time,reducedSamples;
  Capture(Minecraft mc,String id,UUID source,boolean owner){this.id=id;this.source=source;this.owner=owner;world=mc.level;body=mc.player;
   camera=mc.options.getCameraType();own=MagicQuality.own;others=MagicQuality.others;reduced=MagicQuality.reducedFlash;}
  List<Particle> current(Minecraft mc){return selected(mc,true);}
  List<Particle> resident(Minecraft mc){return selected(mc,false);}
  List<Particle> selected(Minecraft mc,boolean queued){return particles(mc.particleEngine,queued).stream().filter(p->p.isAlive()&&(p instanceof LifeParticle||p instanceof MaterialParticle)
   &&source.equals(field(p,p instanceof LifeParticle?LifeParticle.class:MaterialParticle.class,"outcomeSource"))).toList();}
  void sample(Minecraft mc){
   check(mc.level==world&&mc.player==body&&mc.getCameraEntity()==body&&mc.gameRenderer.mainCamera().entity()==body,"Camera sampler remains on exact current native local body/world");
   var live=current(mc);peak=Math.max(peak,live.size());seen.addAll(live);extracted=Math.max(extracted,LifeOwnerPairedCases.extracted(mc,resident(mc)));
   for(var p:live){boolean life=p instanceof LifeParticle;
    check((Boolean)field(p,life?LifeParticle.class:MaterialParticle.class,"outcomeOwner")==owner,"Source metadata chooses owner versus independent second viewer");
    check(p.getLifetime()==(life?10:8),"Comparison preserves authored original material lifetime");
    if(!life&&(Integer)field(p,MaterialParticle.class,"style")==dev.wildercord.content.MaterialOption.TIME)time++;
    int age=(Integer)field(p,Particle.class,"age");
    if(life&&age>0&&MagicQuality.reducedFlash){float alpha=(Float)field(p,SingleQuadParticle.class,"alpha");float normal=Math.min(1,age*.8F)*Math.min(1,(1-age/10F)*3);
     check(Math.abs(alpha-normal*.72F)<.0001,"Real Life sample honors reduced-flash alpha");reducedSamples++;}
   }
  }
  void restore(Minecraft mc){mc.setCameraEntity(mc.player);mc.options.setCameraType(camera);MagicQuality.own=own;MagicQuality.others=others;MagicQuality.reducedFlash=reduced;}
 }
 private static int extracted(Minecraft mc,List<Particle> particles){int count=0;for(var p:particles)if(p.isAlive()){
  var state=new QuadParticleRenderState();((SingleQuadParticle)p).extract(state,mc.gameRenderer.mainCamera(),1);if(!state.isEmpty())count++;
 }return count;}
 private static List<Particle> particles(ParticleEngine engine,boolean queued){var out=new ArrayList<Particle>();
  for(var group:((Map<?,?>)field(engine,ParticleEngine.class,"particles")).values())for(var p:(Queue<?>)field(group,ParticleGroup.class,"particles"))out.add((Particle)p);
  if(queued)for(var p:(Queue<?>)field(engine,ParticleEngine.class,"particlesToAdd"))out.add((Particle)p);return out;
 }
 private static ServerPlayer connected(MinecraftServer server,UUID id){var p=server.getPlayerList().getPlayer(id);
  check(p!=null&&p.isAlive()&&p.connection!=null&&p.connection.player==p&&p.connection.hasClientLoaded()&&server.getPlayerList().getPlayers().size()==2,"Two exact loaded PlayerList/connection bodies required");return p;}
 private static Object field(Object o,Class<?> type,String name){try{var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(ReflectiveOperationException failure){throw new AssertionError(failure);}}
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
