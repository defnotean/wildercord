package dev.wildercord.cast;
import dev.wildercord.content.LifeOption;
import dev.wildercord.client.fx.LifeParticle;
import net.minecraft.util.RandomSource;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
/** Actual paid Sporebloom admissions and read-only support frames. Direct helper case is labeled separately. */
public final class SporebloomSupportTest implements FabricClientGameTest {
 private static final AtomicBoolean denyBlocks=new AtomicBoolean();
 private static final List<LifeOwnerEvents.Event> events=new ArrayList<>();
 private static ServerPlayer caster;private static Mob target;
 private volatile LifeOwnerEvents.Event pendingCapture;private boolean captured;
 @Override public void runTest(ClientGameTestContext c){
  PlayerBlockBreakEvents.BEFORE.register((level,p,pos,state,entity)->!denyBlocks.get());
  try{for(String scenario:List.of("flat","slab","airborne","protected_floor","no_effect"))one(c,scenario);}
  finally{denyBlocks.set(false);LifeOwnerEvents.clear();events.clear();caster=null;target=null;}
 }
 private void one(ClientGameTestContext c,String scenario){
  pendingCapture=null;captured=false;
  try(var gallery=new LifeOutcomeGallery(c,"spore_support_"+scenario);var w=c.worldBuilder().create()){
   gallery.waitTicks(30);var server=w.getServer();server.runCommand("gamerule spawn_mobs false");server.runCommand("gamerule natural_health_regeneration false");
   server.runCommand("fill -8 100 -8 8 100 8 polished_deepslate");
   server.runOnServer(s->{caster=gallery.actor(s);caster.getFoodData().setFoodLevel(20);caster.getFoodData().setSaturation(20);events.clear();
    if(!scenario.equals("no_effect")){
     target=NextSignatureNative.foe(caster,.5,2.5);
     if(scenario.equals("slab")){
      for(var at:BlockPos.betweenClosed(new BlockPos(-1,101,1),new BlockPos(1,101,3)))s.overworld().setBlockAndUpdate(at,Blocks.STONE_SLAB.defaultBlockState());
      target.snapTo(.5,101.5,2.5,0,0);target.move(MoverType.SELF,new Vec3(0,-.125,0));check(target.onGround(),"Actual bottom slab collision grounds foe at its real surface");
     }else if(scenario.equals("airborne")){target.setNoGravity(true);target.snapTo(.5,102.5,2.5,0,0);}
     else{target.move(MoverType.SELF,new Vec3(0,-.125,0));check(target.onGround(),"Ordinary floor collision, no injected ground flags");}
    }else{
     // A genuine owned animal is neither harmful target nor a food recipient. Full caster food changes nothing.
     var pet=EntityTypes.WOLF.create(s.overworld(),EntitySpawnReason.COMMAND);check(pet!=null,"Actual owned animal");pet.tame(caster);pet.setNoAi(true);pet.setPos(.5,101,2.5);s.overworld().addFreshEntity(pet);
    }
    denyBlocks.set(scenario.equals("protected_floor"));
    LifeOwnerEvents.observe(e->{if(e.rune().equals("sporebloom")){events.add(e);if(target!=null&&target.getUUID().equals(e.recipient())&&e.moment()==LifeOwnerEvents.Moment.APPLY&&!captured&&pendingCapture==null){
      frameSupportViewer(e,s.getPlayerList().getPlayers().getFirst());pendingCapture=e;
     }}});
    NextSignatureNative.cast(caster,Runes.SELF,Runes.get("wildercord:sporebloom").orElseThrow());
   });
   for(int i=0;i<35;i++){
    c.waitTicks(1);var capture=pendingCapture;
    if(capture!=null&&!captured&&i+2<35){
     c.waitTicks(2);i+=2;
     server.runOnServer(s->check(s.overworld().getGameTime()-capture.tick()<=5,"Ground/air capture remains inside actual ten-tick material lifetime"));
     c.takeScreenshot(TestScreenshotOptions.of("spore_support_"+scenario+"_sporebloom").disableCounterPrefix());captured=true;pendingCapture=null;
    }
   }
   server.runOnServer(s->{
    if(scenario.equals("no_effect")){check(caster.getFoodData().getFoodLevel()==20&&caster.getFoodData().getSaturationLevel()==20,"Actual food and saturation both remain unchanged");check(events.stream().noneMatch(e->e.moment()!=LifeOwnerEvents.Moment.REFUSED),"Actual full-food caster and owned pet produce no successful spore frame");return;}
    check(target.getHealth()<target.getMaxHealth(),"Actual paid spores cause real damage, not fabricated HIT");
    var admitted=events.stream().filter(e->target.getUUID().equals(e.recipient())&&e.moment()==LifeOwnerEvents.Moment.APPLY).findFirst().orElseThrow();
    check(caster.getUUID().equals(admitted.source())&&target.getUUID().equals(admitted.recipient()),"Real paid source and admitted recipient preserved");
    check(events.stream().anyMatch(e->target.getUUID().equals(e.recipient())&&e.delta()<0),"Actual DOT damage observes negative real delta");
    if(scenario.equals("airborne")){
     check(admitted.normal().y==0&&admitted.anchor().distanceTo(target.getBoundingBox().getCenter())<.01,"Airborne recipient retains explicit horizontal body-spore variant");
     var pieces=pieces(admitted,false);check(pieces.stream().allMatch(v->v.y>101.5),"Airborne materials do not invent floor fruit");
     check(captured,"Actual unsupported body-spore screenshot captured within native lifetime");
    }else{
     double surface=scenario.equals("slab")?101.5:101;
     check(admitted.normal().equals(new Vec3(0,1,0))&&admitted.standoff()==0,"Actual UP surface normal is explicit grounded-fruit wire variant");
     check(Math.abs(admitted.anchor().y-(surface+.03))<1e-6,"Actual full-block/slab support collision determines anchor height");
     var full=pieces(admitted,false);var minimal=pieces(admitted,true);
     check(full.size()>minimal.size()&&full.size()<=128,"Full/Minimal use bounded authored fruit, not random tint thinning");
     check(full.stream().allMatch(v->v.y>=surface+.05&&v.y<=surface+.4),"All initial fruit/spores are physically just above actual support");
     check(captured,"Actual support material screenshot captured within native lifetime");
    }
    if(scenario.equals("protected_floor"))for(var at:BlockPos.betweenClosed(new BlockPos(-8,100,-8),new BlockPos(8,100,8)))check(s.overworld().getBlockState(at).is(Blocks.POLISHED_DEEPSLATE),"Read-only frame never modifies permission-protected support");
    // Ledge fallback is a direct frame fixture, not an additional paid spell-delivery claim.
    var ledge=NextSignatureNative.foe(caster,6.99,2.5);ledge.setNoGravity(true);
    s.overworld().setBlockAndUpdate(new BlockPos(7,100,2),Blocks.AIR.defaultBlockState());
    var resolved=SporeOutcomeFrame.resolve(ledge,new LifeOutcomeFrames.Frame(new Vec3(1,0,0),.4));
    check(resolved.frame().normal().equals(new Vec3(0,1,0))&&Math.abs(resolved.anchor().x-ledge.getX())<1e-6,"Unsupported outward ledge refuses and actual supported foot is selected");
    ledge.discard();
   });
   if(!scenario.equals("no_effect")&&!scenario.equals("airborne")){
    var admitted=server.computeOnServer(s->events.stream().filter(e->target.getUUID().equals(e.recipient())&&e.moment()==LifeOwnerEvents.Moment.APPLY).findFirst().orElseThrow());
    // Actual production particle factory and tick logic, sourced from the genuine paid observation.
    // This finite-motion gate does not insert synthetic events or claim another public delivery.
    c.runOnClient(mc->{
     var o=new LifeOutcomes.Observation(admitted.rune(),LifeOutcomes.Moment.APPLY,admitted.anchor(),admitted.secondary(),admitted.units(),admitted.delta(),0,admitted.normal(),admitted.standoff());
     for(boolean minimal:new boolean[]{false,true}){
      int[] seeds={0};LifeOutcomes.draw(o,minimal,(option,at)->{
       check(option instanceof LifeOption,"Actual authored Life particle option");var material=(LifeOption)option;
       var particle=new LifeParticle.Provider().createParticle(material,mc.level,at.x,at.y,at.z,0,0,0,RandomSource.create(719));
       check(particle instanceof LifeParticle,"Production registered LifeParticle factory");var life=(LifeParticle)particle;
       if(material.style()==LifeOption.SEED)seeds[0]++;
       double floor=admitted.anchor().y-.03;
       for(int tick=0;tick<material.lifetime();tick++){particle.tick();check(life.centreY()>=floor+.05,"Full/Minimal actual finite particle motion never sinks into real support: style="+material.style()+" tick="+tick);}
       particle.tick();check(!particle.isAlive(),"Actual finite material dies after its authored lifetime");
      });check(seeds[0]==1,"Grounded fruit keeps its unique finite split seed coat");
     }
    });
   }
  }finally{denyBlocks.set(false);LifeOwnerEvents.clear();events.clear();caster=null;target=null;}
 }
 /** Camera-only setup uses actual support X/Z, with at most sixteen loaded world/body rays.
  * Actual source, recipient and event are never altered to manufacture a preferred view.
  */
 private static void frameSupportViewer(LifeOwnerEvents.Event e,ServerPlayer viewer){
  var focus=e.anchor().add(e.normal().scale(e.standoff())).add(0,e.normal().y>.999?.15:0,0);
  var actualBody=e.level().getEntity(e.recipient());
  var outward=e.normal().y>.999&&actualBody!=null?e.anchor().subtract(actualBody.position()).multiply(1,0,1):new Vec3(e.normal().x,0,e.normal().z);
  if(outward.lengthSqr()<1e-6)outward=new Vec3(0,0,-1);else outward=outward.normalize();
  for(double lift:new double[]{.8,1.2})for(double angle:new double[]{0,Math.PI/4,-Math.PI/4,Math.PI/2,-Math.PI/2,3*Math.PI/4,-3*Math.PI/4,Math.PI}){
   double horizontal=Math.sqrt(3.2*3.2-lift*lift);var side=outward.yRot((float)angle);var eye=focus.add(side.scale(horizontal)).add(0,lift,0);
   boolean loaded=true;for(int i=0;i<=16;i++)if(!e.level().isLoaded(BlockPos.containing(eye.lerp(focus,i/16.)))){loaded=false;break;}
   if(!loaded)continue;
   // Include the actual recipient in obstruction checks: its body previously hid opposite-side floor fruit.
   boolean blocked=e.level().getEntities(viewer,new AABB(eye,focus).inflate(.03),body->body instanceof LivingEntity&&body.isAlive()&&!body.isRemoved()&&!body.isSpectator()).stream()
    .anyMatch(body->body.getBoundingBox().contains(eye)||body.getBoundingBox().clip(eye,focus).isPresent());
   if(blocked)continue;
   if(e.level().clip(new ClipContext(eye,focus,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,viewer)).getType()!=HitResult.Type.MISS)continue;
   var d=focus.subtract(eye);float yaw=(float)Math.toDegrees(Math.atan2(-d.x,d.z)),pitch=(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance()));
   viewer.teleportTo(e.level(),eye.x,eye.y-viewer.getEyeHeight(),eye.z,Set.<Relative>of(),yaw,pitch,false);return;
  }
  throw new AssertionError("No bounded unobstructed actual Spore material observer ray: "+focus);
 }
 private static List<Vec3> pieces(LifeOwnerEvents.Event e,boolean minimal){
  var list=new ArrayList<Vec3>();var o=new LifeOutcomes.Observation(e.rune(),LifeOutcomes.Moment.valueOf(e.moment().name()),e.anchor(),e.secondary(),e.units(),e.delta(),0,e.normal(),e.standoff());
  LifeOutcomes.draw(o,minimal,(option,at)->{check(option instanceof LifeOption,"Distinct Life physical materials");list.add(at);});return list;
 }
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
