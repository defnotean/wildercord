package dev.wildercord.wildlife;

import static dev.wildercord.wildlife.SiltcrestNative.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.client.DeltaTracker;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.Vec3;

/** Fixed three-pond positive witness, one shared allowance, then the selected original bird's full saved lifecycle. */
public final class SiltcrestEcologyTest implements FabricClientGameTest {
 private SiltcrestBittern bird;
 private UUID id;
 private long appetite,until;
 private BlockPos refuge;
 private SiltcrestEcologyProbe probe;
 public void runTest(ClientGameTestContext c) {
  SiltcrestEcologyWitnessChecks.verify();
  var camera=c.computeOnClient(mc->mc.options.getCameraType());boolean hud=c.computeOnClient(mc->mc.gui.hud.isHidden());TestWorldSave save;
  try {
   c.runOnClient(mc->{mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();});
   try(var w=c.worldBuilder().create()) {
    c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 18000");w.getServer().runCommand("weather clear");
    long started=w.getServer().computeOnServer(s->{
     var l=s.overworld();var cohorts=new ArrayList<SiltcrestEcologyProbe.Cohort>();
     // Exactly three original habitats, twenty blocks apart. No retries, reseeding, or later actor placement.
     for(int index=0;index<SiltcrestEcologyWitness.COHORTS;index++){
      int x=(index-1)*20;floor(l,x,0);checkPond(l,x);
      var prey=new ArrayList<AbstractFish>();for(int f=0;f<3;f++)prey.add(fish(l,f,x,0));
      var source=bird(l,x+.5,.5);cohorts.add(new SiltcrestEcologyProbe.Cohort(index,x,0,source,List.copyOf(prey)));
     }
     // This unchanged central Creative observer stays within 32 blocks of all pond fish.
     // During hunting only its look angles change, so camera work cannot alter residency.
     observer(s.getPlayerList().getPlayers().getFirst(),new Vec3(.5,101.7,-.5));
     probe=new SiltcrestEcologyProbe(s,cohorts);
     System.out.println("SILTCREST_ECOLOGY_HABITAT cohorts=3 offsets=[-20,0,20] waterCellsPerPond=16 ordinaryFishPerPond=3 sharedPostFirstCoilTicks="+SiltcrestPresentationWitness.Budget.HUNT+" aggregateSequentialRetries=0");
     return l.getGameTime();
    });
    int selected=hunt(c,w,started);var cohort=probe.cohorts.get(selected);
    var roof=new BlockPos(cohort.x()-2,103,cohort.z()-2);
    w.getServer().runOnServer(s->{
     check(s.overworld().getEntity(id)==bird&&bird.isAlive()&&bird.huntReady()==appetite,"Selected original living bird owns its earned meal");
     s.overworld().setBlock(roof,Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true),2);
     s.overworld().setBlock(new BlockPos(cohort.x()-1,100,cohort.z()-2),Blocks.WATER.defaultBlockState(),2);
    });
    w.getServer().runCommand("time set 6000");
    await(c,w,420,s->bird.pose()==SiltcrestBittern.SHELTERING,"Selected ordinary bird travels to its dry covered bank");
    w.getServer().runOnServer(s->{
     check(bird.onGround()&&bird.shelter()!=null&&BitternHabitat.shelter(s.overworld(),bird.shelter())&&bird.distanceToSqr(bird.shelter().getX()+.5,bird.shelter().getY(),bird.shelter().getZ()+.5)<=.36,"Selected rest is genuinely grounded under actual shelter");
     check(bird.huntReady()==appetite,"Shelter does not renew earned appetite");until=bird.shelterUntil();refuge=bird.shelter().immutable();
     place(s.getPlayerList().getPlayers().getFirst(),cohort.x()+5.5,101,cohort.z()-3.5,bird.getBoundingBox().getCenter());
    });
    c.waitTicks(2);var shelterFrame=capture(c,w,bird,cohort.index(),-1,"shelter",SiltcrestBittern.SHELTERING);
    for(int advance=0;shelterFrame==null&&advance<20;advance++){c.waitTicks(1);shelterFrame=capture(c,w,bird,cohort.index(),-1,"shelter",SiltcrestBittern.SHELTERING);}
    check(shelterFrame!=null,"The selected grounded shelter is visible on the connected client");
    long captureStarted=w.getServer().computeOnServer(s->s.overworld().getGameTime());
    for(int advance=0;!shelterFrame.isDone()&&advance<40;advance++){
     check(w.getServer().computeOnServer(s->s.overworld().getGameTime())<captureStarted+40,"Shelter readback stays within its separate bounded forty-tick drain");c.waitTicks(1);
    }
    check(shelterFrame.isDone(),"Shelter readback completes before world closure");shelterFrame.join();save=w.getWorldSave();
   }
   try(var w=save.open()) {
    c.waitTicks(25);w.getServer().runOnServer(s->{
     bird=(SiltcrestBittern)s.overworld().getEntity(id);
     check(bird!=null&&bird.isAlive()&&bird.huntReady()==appetite&&bird.shelterUntil()==until,"Exact selected UUID, appetite and refuge deadline persist across full server reopen");
     check(bird.pose()==SiltcrestBittern.SHELTERING&&bird.onGround()&&refuge.equals(bird.shelter())&&BitternHabitat.shelter(s.overworld(),refuge)&&bird.distanceToSqr(refuge.getX()+.5,refuge.getY(),refuge.getZ()+.5)<=.36,"The same still-valid real refuge resumes grounded rest without a new deadline");
     System.out.println("SILTCREST_ECOLOGY_REOPEN source="+id+" appetite="+appetite+" shelterUntil="+until+" refuge="+refuge+" clock="+s.overworld().getGameTime());
    });
   }
  }finally {
   if(probe!=null){probe.close();probe=null;}
   c.runOnClient(mc->{mc.options.setCameraType(camera);if(mc.gui.hud.isHidden()!=hud)mc.gui.hud.toggle();});
  }
 }
 private int hunt(ClientGameTestContext c,TestSingleplayerContext w,long started) {
  var budget=new SiltcrestEcologyWitness.Budget(started);Map<String,String> coils=new HashMap<>();
  var frames=new ArrayList<CompletableFuture<Void>>();
  int selected=-1;boolean catchFrame=false,preenFrame=false;SiltcrestPresentationWitness.Candidate accepted=null;
  while(true) {
   var state=w.getServer().computeOnServer(s->probe.state());budget.observe(state.clock(),state.firstCoil());
   for(var frame:frames)if(frame.isCompletedExceptionally())frame.join();
   // Every cohort has already been qualified and audited; selection never short-circuits another source's failure.
   if(selected<0) {
    selected=SiltcrestEcologyWitness.winner(state.candidates());
    if(selected>=0){accepted=state.candidates().get(selected);bird=probe.cohorts.get(selected).bird();id=bird.getUUID();appetite=accepted.huntReady();
     check(coils.containsKey(key(selected,accepted.epoch())),"The selected successful source and exact commitment own their previously captured coil");}
   }
   for(int i=0;i<state.candidates().size();i++) {
    var candidate=state.candidates().get(i);if(candidate==null||candidate.outcome()!=SiltcrestPresentationWitness.Outcome.PENDING||coils.containsKey(key(i,candidate.epoch())))continue;
    var frame=capture(c,w,probe.cohorts.get(i).bird(),i,candidate.epoch(),"coil",SiltcrestBittern.COILING);
    if(frame!=null){frames.add(frame);coils.put(key(i,candidate.epoch()),name(i,candidate.epoch(),"coil"));}
    var after=w.getServer().computeOnServer(s->probe.state());budget.observe(after.clock(),after.firstCoil());
   }
   if(selected>=0) {
    final int chosen=selected;final var success=accepted;
    int pose=w.getServer().computeOnServer(s->{check(bird.huntReady()==appetite&&bird.preyPool(s.overworld()).size()==2,"Exact earned appetite and two live eligible prey remain owned by the selected bird");return bird.pose();});
    if(!catchFrame&&pose==SiltcrestBittern.STRIKING){var frame=capture(c,w,bird,selected,accepted.epoch(),"catch",SiltcrestBittern.STRIKING);if(frame!=null){frames.add(frame);catchFrame=true;}}
    if(!preenFrame) {
     check(state.clock()<=accepted.strikeTick()+20,"Original twenty-tick preen observation window is unchanged");
     if(pose==SiltcrestBittern.PREENING){check(state.clock()>=accepted.strikeTick()+6,"Original six-tick strike completes before actual preening");var frame=capture(c,w,bird,selected,accepted.epoch(),"preen",SiltcrestBittern.PREENING);if(frame!=null){frames.add(frame);preenFrame=true;}}
    }
    var after=w.getServer().computeOnServer(s->probe.state());budget.observe(after.clock(),after.firstCoil());
    if(preenFrame&&SiltcrestEcologyWitness.resolved(after.candidates())&&frames.stream().allMatch(CompletableFuture::isDone)) {
     for(var frame:frames)frame.join();
     check(catchFrame,"The same selected source owns an actual native strike frame");
     boolean finished=w.getServer().computeOnServer(s->{
      var current=probe.state();budget.observe(current.clock(),current.firstCoil());
      if(!SiltcrestEcologyWitness.resolved(current.candidates()))return false;
      check(bird.pose()==SiltcrestBittern.PREENING&&bird.huntReady()==appetite,"Selected earned preening survives resolution of every other witnessed commitment");
      probe.finish();
      System.out.println("SILTCREST_ECOLOGY_ACCEPTED cohort="+(chosen+1)+" source="+id+" quarry="+success.quarry()+" epoch="+success.epoch()+" strikeTick="+success.strikeTick()+" appetite="+appetite+" firstCoil="+budget.firstCoil()+" sharedDeadline="+budget.deadline()+" controlledAdvances="+budget.advances()+" completed="+current.clock()+" coilFrame="+coils.get(key(chosen,success.epoch()))+" cohorts=3 perPondSuccessGuarantee=false");return true;
     });
     if(finished)return selected;
    }
   }
   budget.advance();c.waitTicks(1);
  }
 }
 private static void checkPond(net.minecraft.server.level.ServerLevel l,int dx) {
  int water=0;
  for(int x=0;x<=5;x++)for(int z=-1;z<=4;z++){
   var at=new BlockPos(dx+x,100,z);boolean wet=x>=1&&x<=4&&z>=0&&z<=3;
   check(l.getBlockState(at.below()).is(Blocks.CLAY)&&l.getBlockState(at.above()).isAir(),"Original clay lining and clear pond headspace are preserved");
   if(wet){check(l.getBlockState(at).is(Blocks.WATER)&&l.getFluidState(at).is(FluidTags.WATER)&&l.getFluidState(at).isSource()&&l.getBlockState(at).getCollisionShape(l,at).isEmpty(),"Each supplied source-water cell retains ordinary open swimming space");water++;}
   else check(l.getBlockState(at).is(Blocks.CLAY)&&l.getFluidState(at).isEmpty(),"Original dry clay boundary is preserved");
  }
  check(water==16,"Each original four-by-four pond retains sixteen water cells");
 }
 private static String key(int cohort,int epoch){return cohort+":"+epoch;}
 private static String name(int cohort,int epoch,String phase){return "siltcrest_cohort_"+(cohort+1)+(epoch<0?"":"_epoch_"+epoch)+"_"+phase;}
 private static CompletableFuture<Void> capture(ClientGameTestContext c,TestSingleplayerContext w,SiltcrestBittern source,int cohort,int epoch,String phase,int expectedPose) {
  UUID sourceId=source.getUUID();int entityId=source.getId();
  long before=w.getServer().computeOnServer(s->{check(source.isAlive()&&s.overworld().getEntity(sourceId)==source,"Original tracked source owns capture admission");return s.overworld().getGameTime();});
  boolean current=w.getServer().computeOnServer(s->source.pose()==expectedPose&&(epoch<0||SiltcrestEcologyProbe.epoch(source)==epoch));
  if(!current)return null;
  String filename=name(cohort,epoch,phase);
  var frame=c.computeOnClient(mc->{
   var entity=mc.level==null?null:mc.level.getEntity(entityId);
   if(!(entity instanceof SiltcrestBittern shown)||!shown.getUUID().equals(sourceId)||!shown.isAlive()||shown.pose()!=expectedPose)return (CompletableFuture<Void>)null;
   boolean ready=switch(expectedPose){case SiltcrestBittern.COILING->shown.coil>=.3F;case SiltcrestBittern.STRIKING->shown.strike>=.5F;case SiltcrestBittern.PREENING->shown.preen>=.3F;case SiltcrestBittern.SHELTERING->shown.rest>=.3F;default->false;};
   if(!ready)return (CompletableFuture<Void>)null;
   var delta=shown.getBoundingBox().getCenter().subtract(mc.player.getEyePosition());
   mc.player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));mc.player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance())));
   // Render the identified current native model now. Readback/file I/O never waits or advances the harness here.
   var result=new CompletableFuture<Void>();mc.gameRenderer.update(DeltaTracker.ONE);mc.gameRenderer.extract(DeltaTracker.ONE,true);mc.gameRenderer.render();
   com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder().submit();
   net.minecraft.client.Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{try(image){var path=java.nio.file.Path.of("screenshots",filename+".png");java.nio.file.Files.createDirectories(path.getParent());image.writeToFile(path);result.complete(null);}catch(Throwable error){result.completeExceptionally(error);}});
   return result;
  });
  if(frame==null)return null;
  long after=w.getServer().computeOnServer(s->s.overworld().getGameTime());
  check(after-before<SiltcrestBittern.COIL,"Immediate native capture cannot span a later commitment epoch");
  System.out.println("SILTCREST_ECOLOGY_SCREENSHOT name="+filename+" cohort="+(cohort+1)+" source="+sourceId+" epoch="+epoch+" renderedPose="+expectedPose+" before="+before+" after="+after+" elapsed="+(after-before)+" asynchronousReadback=true");return frame;
 }
}
