package dev.wildercord.client.fx;

import dev.wildercord.cast.*;
import dev.wildercord.content.*;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Actual paid alternate Void deliveries, exact metadata, mixed fallback and material retirement. */
public final class VoidFlightVariantsTest implements FabricClientGameTest {
 private record Case(String name,List<String> spell,String effects,int style,boolean arc,boolean covered,Set<Integer> materials){}
 private static final List<Case> CASES=List.of(
  new Case("arc_pull",List.of("arc","pull"),"wildercord:pull",0,true,true,Set.of(VoidOption.CLOTH,VoidOption.FOLD)),
  new Case("arc_wither",List.of("arc","wither"),"wildercord:wither",0,true,true,Set.of(VoidOption.TOOTH,VoidOption.SHARD)),
  new Case("pierce_umbra",List.of("bolt","umbra","pierce"),"wildercord:umbra",RuneBolt.STYLE_PIERCE,false,true,Set.of(VoidOption.TOOTH,VoidOption.CLOTH,VoidOption.HAZE)),
  new Case("frugal_collect",List.of("bolt","collect","frugal"),"wildercord:collect",RuneBolt.STYLE_FRUGAL,false,true,Set.of(VoidOption.CLOTH,VoidOption.SHARD)),
  new Case("mixed_umbra_shock",List.of("bolt","umbra","shock"),"wildercord:umbra,wildercord:shock",0,false,true,Set.of(VoidOption.TOOTH,VoidOption.CLOTH,VoidOption.HAZE)),
  new Case("mixed_umbra_harm",List.of("bolt","umbra","harm"),"wildercord:umbra,wildercord:harm",0,false,false,Set.of(VoidOption.TOOTH,VoidOption.CLOTH,VoidOption.HAZE)));
 private static int cameraId;
 private static LivingEntity firstTarget,secondTarget;
 @Override public void runTest(ClientGameTestContext c){
  var previous=c.computeOnClient(mc->MagicQuality.own);var hidden=c.computeOnClient(mc->mc.gui.hud.isHidden());
  var window=c.computeOnClient(mc->new int[]{mc.getWindow().getWidth(),mc.getWindow().getHeight()});
  try(var world=c.worldBuilder().create()){
   c.waitTicks(40);var server=world.getServer();server.runCommand("gamerule spawn_mobs false");server.runCommand("gamerule natural_regeneration false");server.runCommand("time set 6000");server.runCommand("weather clear");
   server.runCommand("fill -16 100 -12 16 100 40 polished_deepslate");server.runCommand("fill -12 101 32 12 109 32 gray_concrete");
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);
    Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));var book=Spellbooks.get(p).withStarterGiven();for(var rune:Runes.all())book=book.learn(rune.id());Spellbooks.set(p,book);
    var camera=EntityTypes.TEXT_DISPLAY.create(s.overworld(),EntitySpawnReason.COMMAND);check(camera!=null,"Actual display camera exists");camera.snapTo(3,102,10,90,0);camera.setNoGravity(true);camera.setInvisible(true);s.overworld().addFreshEntity(camera);cameraId=camera.getId();});c.waitTicks(15);
   c.runOnClient(mc->{mc.getWindow().setWindowed(1280,720);mc.resizeGui();if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();materialContracts(mc);});
   check(FlightBodies.covers("wildercord:umbra,wildercord:shock"),"Void+Storm covered group has both authored routes");
   check(!FlightBodies.covers("wildercord:umbra,wildercord:harm"),"Void+uncovered Arcane must retain fallback");
   for(var quality:List.of(MagicQuality.Level.FULL,MagicQuality.Level.MINIMAL))for(var sample:CASES){
    server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.level().getEntitiesOfClass(RuneBolt.class,p.getBoundingBox().inflate(64)).forEach(Entity::discard);p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,sample.arc()?-15:0,false);});c.waitTicks(12);
    c.runOnClient(mc->{mc.setCameraEntity(mc.player);mc.particleEngine.clearParticles();mc.gui.toastManager().clear();MagicQuality.own=quality;});
    server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(SpellCaster.edit(p,0,sample.spell().stream().map(id->"wildercord:"+id).toList())==null,"Actual edit accepts "+sample.name());Spellbooks.setReadyAt(p,0,0);Spellbooks.setMana(p,100);float before=Spellbooks.mana(p);SpellCaster.cast(p,0);check(Spellbooks.mana(p)<before,"Actual Survival cast pays "+sample.name());});
    c.waitTicks(4);Vec3[] first={Vec3.ZERO};
    server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var bolts=p.level().getEntitiesOfClass(RuneBolt.class,p.getBoundingBox().inflate(64));check(bolts.size()==1,"Exactly one paid projectile, no fixture stand-in");first[0]=bolts.getFirst().getDeltaMovement();});c.waitTicks(4);
    server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var bolts=p.level().getEntitiesOfClass(RuneBolt.class,p.getBoundingBox().inflate(64));check(bolts.size()==1,"Live native alternate projectile");var bolt=bolts.getFirst();
     if(sample.arc())check(bolt.getDeltaMovement().y<first[0].y-.1,"Native Arc trajectory actually falls");else check(bolt.getDeltaMovement().distanceTo(first[0])<.001,"Straight modifier route retains native velocity");});
    c.runOnClient(mc->{var bolt=clientBolt(mc);check(bolt.getEntityData().get(RuneBolt.DATA_EFFECTS).equals(sample.effects()),"Exact synchronized effect group");
     int modifierMask=RuneBolt.STYLE_PIERCE|RuneBolt.STYLE_FRUGAL;check((bolt.getEntityData().get(RuneBolt.DATA_STYLE)&modifierMask)==sample.style(),"Exact Pierce/Frugal metadata");
     check(VoidFlightTest.authoredNear(mc,bolt),"Actual short Void body follows alternate projectile");
     var near=VoidFlightTest.particles(mc.particleEngine).stream().filter(p->p.isAlive() && lifetime(p)==5 && VoidFlightTest.at(p).distanceTo(bolt.position())<1+2*bolt.getDeltaMovement().length()).toList();
     var styles=new HashSet<Integer>();for(var particle:near)if(particle instanceof VoidParticle)styles.add(material(particle).style());check(styles.containsAll(sample.materials()),"Recipe-specific physical Void styles survive "+sample.name()+"/"+quality);
     if(sample.style()==RuneBolt.STYLE_PIERCE)check(near.stream().filter(p->p instanceof VoidParticle).map(VoidFlightVariantsTest::material).anyMatch(o->o.style()==VoidOption.TOOTH && Math.abs(o.size()-.19*.65*.65)<.002),"Pierce visibly narrows the actual material body");
     if(sample.style()==RuneBolt.STYLE_FRUGAL)check(near.stream().filter(p->p instanceof VoidParticle).map(VoidFlightVariantsTest::material).anyMatch(o->o.style()==VoidOption.CLOTH && Math.abs(o.size()-.2*.65*.7)<.002),"Frugal visibly reduces the actual pouch material");
     check(VoidFlightTest.particles(mc.particleEngine).stream().anyMatch(p->p.isAlive() && p.getClass().getSimpleName().equals("Comet") && (Boolean)VoidFlightTest.field(p,p.getClass(),"authored")==sample.covered()),"Covered/uncovered group retains the correct fallback state");
     if(sample.name().equals("mixed_umbra_shock"))check(near.stream().anyMatch(p->p instanceof MaterialParticle && ((Number)VoidFlightTest.field(p,MaterialParticle.class,"style")).intValue()==MaterialOption.STORM),"Actual mixed spell retains authored Storm ingredient as well as Void");
    });
    c.runOnClient(VoidFlightVariantsTest::positionCamera);c.waitTicks(2);
    var shot=c.computeOnClient(mc->captureMaterial(mc,"void_variant_"+sample.name()+"_"+quality.name().toLowerCase(java.util.Locale.ROOT)));c.waitFor(mc->shot.isDone());shot.join();
    c.runOnClient(mc->mc.particleEngine.clearParticles());c.waitTicks(4);
    c.runOnClient(mc->check(VoidFlightTest.authoredNear(mc,clientBolt(mc)),"Entity resumes authored Void emission after particle clear"));
    server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.level().getEntitiesOfClass(RuneBolt.class,p.getBoundingBox().inflate(64)).forEach(Entity::discard);});c.waitTicks(12);
    c.runOnClient(mc->{check(((Set<?>)VoidFlightTest.field(null,BoltComets.class,"DRAWN")).isEmpty(),"Removed entity IDs retire");check(VoidFlightTest.particles(mc.particleEngine).stream().noneMatch(p->p.isAlive() && p instanceof VoidParticle && lifetime(p)==5),"Five-tick material retires after projectile removal");});
   }
   // A real paid Pierce/Umbra goes through the first living target and delivers to the second.
   server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(s.overworld(),.5,101,.5,Set.<Relative>of(),0,0,false);
    var a=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);var b=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);check(a!=null && b!=null,"Actual two-target delivery fixtures");a.setNoAi(true);b.setNoAi(true);a.snapTo(.5,101,8.5,0,0);b.snapTo(.5,101,12.5,0,0);s.overworld().addFreshEntity(a);s.overworld().addFreshEntity(b);firstTarget=a;secondTarget=b;
    check(SpellCaster.edit(p,0,List.of(Runes.BOLT.id(),Runes.UMBRA.id(),Runes.PIERCE_MOD.id()))==null,"Actual Pierce delivery edit");Spellbooks.setReadyAt(p,0,0);Spellbooks.setMana(p,100);float before=Spellbooks.mana(p);SpellCaster.cast(p,0);check(Spellbooks.mana(p)<before,"Pierce delivery pays actual mana");});c.waitTicks(20);
   server.runOnServer(s->{check(firstTarget.getHealth()<firstTarget.getMaxHealth() && secondTarget.getHealth()<secondTarget.getMaxHealth(),"Actual paid piercing Void effect reaches both living targets");firstTarget.discard();secondTarget.discard();});
  }finally{c.runOnClient(mc->{MagicQuality.own=previous;if(mc.gui.hud.isHidden()!=hidden)mc.gui.hud.toggle();mc.getWindow().setWindowed(window[0],window[1]);mc.resizeGui();if(mc.player!=null)mc.setCameraEntity(mc.player);});}
 }
 private static void materialContracts(Minecraft mc){
  var option=new VoidOption(VoidOption.CLOTH,0xC7B8CB,.2F,8,new Vec3(.01,-.01,0),.15F);
  var cloth=new VoidParticle(mc.level,0,80,0,option);check(cloth.getLightCoords(0)!=net.minecraft.util.LightCoordsUtil.FULL_BRIGHT,"Torn cloth uses sampled light below the platform");
  var sculk=new VoidParticle(mc.level,0,80,0,new VoidOption(VoidOption.SCULK,0xABCBD0,.2F,8,Vec3.ZERO,0));check(sculk.getLightCoords(0)==net.minecraft.util.LightCoordsUtil.FULL_BRIGHT,"Sculk pressure has deliberate luminous contrast");
  boolean previous=MagicQuality.reducedFlash;
  try{MagicQuality.reducedFlash=false;cloth.tick();float normal=((Number)VoidFlightTest.field(cloth,SingleQuadParticle.class,"alpha")).floatValue();var softened=new VoidParticle(mc.level,0,80,0,option);MagicQuality.reducedFlash=true;softened.tick();float reduced=((Number)VoidFlightTest.field(softened,SingleQuadParticle.class,"alpha")).floatValue();check(reduced>0 && reduced<normal,"Reduced Flash softens material without hiding it");}finally{MagicQuality.reducedFlash=previous;}
  for(var velocity:List.of(Vec3.ZERO,new Vec3(0,1,0),new Vec3(0,-1,0))){int[] count={0};VoidFlights.draw("wildercord:umbra",4,2,2,Vec3.ZERO,velocity,true,(particle,point)->{count[0]++;check(Double.isFinite(point.lengthSqr()) && point.length()<2,"Stationary/vertical Void body finite and bounded");check(particle instanceof VoidOption,"Umbra body uses dedicated Void materials");});check(count[0]>=3,"Finite-orientation contract actually emits its recipe");}
 }
 private static RuneBolt clientBolt(Minecraft mc){var bolts=new ArrayList<RuneBolt>();for(var entity:mc.level.entitiesForRendering())if(entity instanceof RuneBolt bolt)bolts.add(bolt);check(bolts.size()==1,"Exactly one live synchronized client projectile");return bolts.getFirst();}
 private static int lifetime(Particle p){return ((Number)VoidFlightTest.field(p,Particle.class,"lifetime")).intValue();}
 private static VoidOption material(Particle p){return (VoidOption)VoidFlightTest.field(p,VoidParticle.class,"material");}
 private static void positionCamera(Minecraft mc){
  var bolt=clientBolt(mc);var camera=mc.level.getEntity(cameraId);check(camera!=null,"Synced side review camera");camera.snapTo(bolt.getX()+2.7,bolt.getY()-camera.getEyeHeight(),bolt.getZ()+2,90,0);mc.setCameraEntity(camera);
 }
 private static CompletableFuture<Void> captureMaterial(Minecraft mc,String name){
  check(mc.level.getEntity(cameraId)!=null,"Settled side review camera remains synchronized");
  var actual=VoidFlightTest.particles(mc.particleEngine).stream().filter(p->p.isAlive() && lifetime(p)==5 && (p instanceof VoidParticle || p instanceof MaterialParticle)).toList();check(actual.stream().anyMatch(p->p instanceof VoidParticle),"Capture retains actual production Void pieces");
  // Frame the exact retained production objects at this capture step, not the Arc's earlier moving entity position.
  var camera=mc.level.getEntity(cameraId);
  double minX=Double.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX,maxZ=maxX;
  for(var particle:actual){var pos=VoidFlightTest.at(particle);minX=Math.min(minX,pos.x);minY=Math.min(minY,pos.y);minZ=Math.min(minZ,pos.z);maxX=Math.max(maxX,pos.x);maxY=Math.max(maxY,pos.y);maxZ=Math.max(maxZ,pos.z);}
  var centre=new Vec3((minX+maxX)/2,(minY+maxY)/2,(minZ+maxZ)/2);
  check(Double.isFinite(centre.x) && Double.isFinite(centre.y) && Double.isFinite(centre.z),"Actual retained body bounds are finite");
  var eye=centre.add(2.7,.6,2);var aim=centre.subtract(eye);
  float yaw=(float)Math.toDegrees(Math.atan2(-aim.x,aim.z));
  float pitch=(float)-Math.toDegrees(Math.atan2(aim.y,Math.hypot(aim.x,aim.z)));
  camera.snapTo(eye.x,eye.y-camera.getEyeHeight(),eye.z,yaw,pitch);mc.setCameraEntity(camera);
  var view=camera.getLookAngle().normalize();
  long inView=actual.stream().filter(p->p instanceof VoidParticle).filter(p->{var ray=VoidFlightTest.at(p).subtract(camera.getEyePosition());return ray.lengthSqr()>.01 && ray.normalize().dot(view)>Math.cos(Math.toRadians(20));}).count();
  System.out.println("Void variant diagnostic "+name+" bolt="+clientBolt(mc).position()+" retainedBounds=["+minX+","+minY+","+minZ+" -> "+maxX+","+maxY+","+maxZ+"] eye="+camera.getEyePosition()+" view="+view+" nativeVoidInside20deg="+inView);
  check(inView>0,"Retained production Void lies inside diagnostic camera's conservative view cone: "+name);
  mc.particleEngine.clearParticles();var empty=VoidFlightTest.capturePixels(mc,name+"_background");for(var particle:actual)mc.particleEngine.add(particle);mc.particleEngine.tick();var drawn=VoidFlightTest.capturePixels(mc,name);mc.setCameraEntity(mc.player);
  return empty.thenCombine(drawn,(a,b)->{check(a.length==b.length,"Matched native captures");int changed=0;for(int i=0;i<a.length;i++){int x=a[i],y=b[i];if(Math.abs((x>>16&255)-(y>>16&255))+Math.abs((x>>8&255)-(y>>8&255))+Math.abs((x&255)-(y&255))>20)changed++;}check(changed>10,"Production physical body changes visible pixels: "+name+" changed="+changed);return (Void)null;});
 }
 private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
