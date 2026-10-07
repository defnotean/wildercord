package dev.wildercord.wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.*;
/** Actual live contact before full shutdown; control is transient while attack/rest clocks remain saved. */
public final class RootmoltRestartTest implements FabricClientGameTest {
 private RootmoltStrider root;private SporebackSnail visible,blocked,distant;
 private static volatile RootmoltRestartTest observing;
 static {ServerTickEvents.END_LEVEL_TICK.register(l -> {var test=observing;if(test!=null)test.captureAlarmBoundary(l);});}
 private AlarmFrame previousAlarmFrame,beforeAlarmFrame,afterAlarmFrame;
 private RuntimeException alarmCaptureFailure;
 public void runTest(ClientGameTestContext c) {
  previousAlarmFrame=beforeAlarmFrame=afterAlarmFrame=null;alarmCaptureFailure=null;
  TestWorldSave saved;UUID id,snailId;long deadline,threat,hidden;long[] economy;boolean dew;
  try(var w=c.worldBuilder().create()) {
   c.waitTicks(25);w.getServer().runCommand("difficulty normal");w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 18000");
   w.getServer().runOnServer(s -> {
    var l=s.overworld();l.getChunkAt(new BlockPos(8,30,8));
    for(int x=0;x<=16;x++)for(int z=0;z<=16;z++){l.setBlock(new BlockPos(x,29,z),Blocks.STONE.defaultBlockState(),2);for(int y=30;y<=33;y++)l.setBlock(new BlockPos(x,y,z),y==33?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);}
    // Center the occluded eye ray inside a real wall, with clearance from both collision boxes.
    // The former x=0 ray grazed the wall's west edge; ordinary pre-warning movement could expose it.
    for(int x=6;x<=10;x++)for(int y=30;y<=32;y++)l.setBlock(new BlockPos(x,y,6),Blocks.STONE.defaultBlockState(),3);
    var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleportTo(l,10.5,30,8.5,Set.<Relative>of(),90,0,false);
    root=RootmoltContent.STRIDER.create(l,EntitySpawnReason.COMMAND);check(root!=null,"Actual Rootmolt fixture");root.snapTo(8.5,30,8.5,0,0);l.addFreshEntity(root);root.setTarget(p);
    visible=snail(l,8.5,10.5);blocked=snail(l,8.5,5.5);distant=snail(l,14.5,8.5);
    alarmGeometry();
    for(var snail:List.of(visible,blocked,distant))check(snail.threatReady()==0 && snail.hiddenUntil()==0,"Fresh alarm receiver starts with unspent physical clocks; "+alarmState(snail));
    observing=this;
   });
   long[] originalEconomy=w.getServer().computeOnServer(s -> clocks(visible));
   await(c,w,s -> {
    check(alarmCaptureFailure==null,"Read-only alarm boundary capture failed: "+alarmCaptureFailure);
    if(afterAlarmFrame!=null)checkAlarmBoundary(s.getPlayerList().getPlayers().getFirst().getUUID());
    alarmGeometry();
    if(root.pose()!=RootmoltStrider.WARNING || visible.threatReady()==0 || afterAlarmFrame==null)return false;
    check(root.getTarget()==s.getPlayerList().getPlayers().getFirst(),"The real warning still targets the Survival player; "+alarmState(visible));return true;
   },"Actual WARNING proximity and line of sight retract the nearby Sporeback");
   w.getServer().runOnServer(s -> {check(visible.pose()==2 && visible.hiddenUntil()>s.overworld().getGameTime(),"Actual physical alarm creates a finite hide");alarmGeometry();check(blocked.threatReady()==0 && blocked.hiddenUntil()==0,"Occluded in-range snail refuses the actual scheduled alarm; "+alarmState(blocked));check(distant.threatReady()==0 && distant.hiddenUntil()==0,"Visible out-of-range snail refuses the actual scheduled alarm; "+alarmState(distant));check(Arrays.equals(originalEconomy,clocks(visible)) && !visible.dew(),"Physical danger changes no gather/forage/magic-response/nursery clock or dew reserve");long ready=visible.threatReady(),hide=visible.hiddenUntil();RootmoltContent.alarm(root);RootmoltContent.alarm(root);check(!visible.answerThreat() && visible.threatReady()==ready && visible.hiddenUntil()==hide,"Repeated actual warning and hidden response cannot renew physical clocks");});
   boolean grabbed=false;for(int i=0;i<100;i++){c.waitTicks(1);if(w.getServer().computeOnServer(s -> root.holding(s.getPlayerList().getPlayers().getFirst()))){grabbed=true;break;}}check(grabbed,"Actual living source owns control before shutdown");id=root.getUUID();deadline=root.attackReady();snailId=visible.getUUID();threat=visible.threatReady();hidden=visible.hiddenUntil();economy=w.getServer().computeOnServer(s -> clocks(visible));dew=visible.dew();saved=w.getWorldSave();
  }finally{observing=null;}
  try(var w=saved.open()) {c.waitTicks(3);w.getServer().runOnServer(s -> {var source=(RootmoltStrider)s.overworld().getEntity(id);var p=s.getPlayerList().getPlayers().getFirst();check(source!=null && source.attackReady()==deadline,"Exact saved attack rest survives full shutdown");check(source.pose()!=RootmoltStrider.HOLDING && !p.hasEffect(RootmoltContent.TETHER) && p.getAttachedOrElse(RootmoltContent.GRAB_OWNER,RootmoltContent.NO_OWNER).equals(RootmoltContent.NO_OWNER),"Saved player effects cannot restore a transient source grab after restart");
    visible=(SporebackSnail)s.overworld().getEntity(snailId);check(visible!=null && visible.threatReady()==threat && visible.hiddenUntil()==hidden,"Full reopen preserves exact physical threat and hide timestamps");check(Arrays.equals(economy,clocks(visible)) && visible.dew()==dew,"Restart preserves independent reserve clocks and dew");check(!visible.answerThreat() && visible.threatReady()==threat,"Saved hidden deadline refuses even before the first active AI pose update");source.discard();RootmoltContent.alarm(source);check(visible.threatReady()==threat && visible.hiddenUntil()==hidden,"Removed source cannot renew its former physical alarm");visible.setNoAi(false);visible.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(0);});c.waitTicks(3);
   w.getServer().runOnServer(s -> check(visible.pose()==2,"Ordinary resumed AI respects the saved hide"));
   await(c,w,s -> s.overworld().getGameTime()>hidden && visible.pose()!=2,"Physical hide expires through normal elapsed AI ticks after source removal");
   w.getServer().runOnServer(s -> {check(s.overworld().getGameTime()<threat && !visible.answerThreat(),"Independent threat rest still refuses after the shorter hide expires");check(visible.threatReady()==threat && visible.hiddenUntil()==hidden && Arrays.equals(economy,clocks(visible)) && visible.dew()==dew,"Expiry/refusal does not renew deadlines or mint resources");});
  }finally{root=null;visible=null;blocked=null;distant=null;}

 }
 private static SporebackSnail snail(net.minecraft.server.level.ServerLevel l,double x,double z){var snail=SporebackContent.SNAIL.create(l,EntitySpawnReason.COMMAND);check(snail!=null,"Actual Sporeback fixture");snail.setNoAi(true);snail.setPersistenceRequired();snail.snapTo(x,30,z,0,0);l.addFreshEntity(snail);return snail;}
 private record AlarmReceiver(String name,Vec3 position,AABB bounds,boolean live,boolean sameWorld,boolean loaded,boolean clear,boolean sight,double distanceSquared,int pose,long threat,long hidden){}
 private record AlarmFrame(Vec3 position,AABB bounds,boolean live,boolean loaded,boolean clear,boolean noAi,int tick,long time,int pose,UUID target,AABB targetBounds,List<AlarmReceiver> receivers){}
 /** Read-only end-of-world snapshots bracket the actual scheduled pulse; assertions stay outside the native tick callback. */
 private void captureAlarmBoundary(ServerLevel l){
  if(root==null || visible==null || blocked==null || distant==null || root.level()!=l)return;
  try{
   var frame=new AlarmFrame(root.position(),root.getBoundingBox(),root.isAlive() && !root.isRemoved(),l.hasChunkAt(root.blockPosition()),l.noCollision(root),root.isNoAi(),root.tickCount,l.getGameTime(),root.pose(),root.getTarget()==null?null:root.getTarget().getUUID(),root.getTarget()==null?null:root.getTarget().getBoundingBox(),List.of(alarmReceiver("visible",visible),alarmReceiver("blocked",blocked),alarmReceiver("distant",distant)));
   if(frame.receivers().getFirst().threat()>0){beforeAlarmFrame=previousAlarmFrame;afterAlarmFrame=frame;observing=null;}else previousAlarmFrame=frame;
  }catch(RuntimeException failure){alarmCaptureFailure=failure;observing=null;}
 }
 private AlarmReceiver alarmReceiver(String name,SporebackSnail snail){return new AlarmReceiver(name,snail.position(),snail.getBoundingBox(),snail.isAlive() && !snail.isRemoved(),snail.level()==root.level(),snail.level().hasChunkAt(snail.blockPosition()),snail.level().noCollision(snail),root.hasLineOfSight(snail),root.distanceToSqr(snail),snail.pose(),snail.threatReady(),snail.hiddenUntil());}
 private void checkAlarmBoundary(UUID player){
  check(alarmCaptureFailure==null,"Read-only alarm boundary capture failed: "+alarmCaptureFailure);
  check(beforeAlarmFrame!=null && afterAlarmFrame!=null,"The real alarm has both adjacent end-world-tick receipts; "+alarmState(visible));
  var before=beforeAlarmFrame;var after=afterAlarmFrame;
  for(var phase:List.of("before","after")){var frame=phase.equals("before")?before:after;for(var receiver:frame.receivers())System.out.println("WILDERCORD_ROOTMOLT_ALARM phase="+phase+" sourceTick="+frame.tick()+" gameTime="+frame.time()+" source="+frame.position()+" sourceBounds="+frame.bounds()+" sourcePose="+frame.pose()+" playerBounds="+frame.targetBounds()+" receiver="+receiver);}
  check(after.tick()==before.tick()+1 && after.time()==before.time()+1 && after.tick()%20==0,"Adjacent native world ticks bracket the real twenty-tick scheduled alarm");
  check(before.position().equals(after.position()) && before.bounds().equals(after.bounds()),"The committed source body stays fixed across the actual alarm boundary");
  for(var frame:List.of(before,after)){
   check(frame.live() && frame.loaded() && frame.clear() && !frame.noAi() && frame.pose()==RootmoltStrider.WARNING && player.equals(frame.target()),"Both native boundary receipts show the live ordinary-AI warning source, player target and body clearance");
   var bodies=new ArrayList<AABB>();bodies.add(frame.bounds());bodies.add(frame.targetBounds());for(var receiver:frame.receivers())bodies.add(receiver.bounds());
   check(separated(bodies),"All actor bodies remain outside each other's native push neighborhood at the actual alarm boundary");
   for(int i=0;i<frame.receivers().size();i++){
    var receiver=frame.receivers().get(i);
    check(receiver.live() && receiver.sameWorld() && receiver.loaded() && receiver.clear(),"Native alarm boundary receiver has live loaded-world and body clearance: "+receiver);
    check((receiver.distanceSquared()>16)==receiver.name().equals("distant") && receiver.sight()!=receiver.name().equals("blocked"),"Native alarm boundary verifies each actual range and eye-ray precondition: "+receiver);
    check(receiver.position().equals(before.receivers().get(i).position()) && receiver.bounds().equals(before.receivers().get(i).bounds()),"Receiver body stays fixed across the actual alarm boundary: "+receiver);
   }
  }
  for(var receiver:before.receivers())check(receiver.threat()==0 && receiver.hidden()==0,"Each receiver has fresh physical clocks immediately before the real pulse: "+receiver);
  var admitted=after.receivers().getFirst();
  check(admitted.pose()==2 && admitted.threat()==after.time()+200 && admitted.hidden()==after.time()+SporebackRules.HIDE_TICKS,"The scheduled pulse alone creates the exact finite physical deadlines: "+admitted);
  for(int i=1;i<after.receivers().size();i++){var refused=after.receivers().get(i);check(refused.threat()==0 && refused.hidden()==0,"Actual scheduled alarm preserves the named receiver's refused clocks: "+refused);}
 }
 private static boolean separated(List<AABB> bodies){for(int i=0;i<bodies.size();i++)for(int j=i+1;j<bodies.size();j++)if(bodies.get(i)==null || bodies.get(j)==null || bodies.get(i).inflate(.2,0,.2).intersects(bodies.get(j)))return false;return true;}
 private void alarmGeometry(){
  check(root.isAlive() && !root.isRemoved(),"The actual alarm source remains live; "+alarmState(visible));
  check(root.getTarget()!=null && separated(List.of(root.getBoundingBox(),root.getTarget().getBoundingBox(),visible.getBoundingBox(),blocked.getBoundingBox(),distant.getBoundingBox())),"Actor bodies remain outside each other's native push neighborhood; "+alarmState(visible));
  check(root.level().noCollision(root),"The live source body has actual block and entity clearance; "+alarmState(visible));
  for(var snail:List.of(visible,blocked,distant))check(snail.isAlive() && !snail.isRemoved() && snail.level()==root.level() && root.level().hasChunkAt(root.blockPosition()) && root.level().hasChunkAt(snail.blockPosition()),"Alarm actors remain live in the same loaded world; "+alarmState(snail));
  for(var snail:List.of(visible,blocked,distant))check(snail.level().noCollision(snail),"The alarm receiver body has actual block and entity clearance; "+alarmState(snail));
  check(root.distanceToSqr(visible)<=16 && root.hasLineOfSight(visible),"Nearby receiver remains in range with actual eye line of sight; "+alarmState(visible));
  check(root.distanceToSqr(blocked)<=16 && !root.hasLineOfSight(blocked),"Blocked receiver remains in range behind the actual wall; "+alarmState(blocked));
  check(root.distanceToSqr(distant)>16 && root.hasLineOfSight(distant),"Distant receiver remains out of range with actual eye line of sight; "+alarmState(distant));
 }
 private String alarmState(SporebackSnail snail){return "gameTime="+root.level().getGameTime()+" source="+root.position()+" sourceBounds="+root.getBoundingBox()+" tick="+root.tickCount+" pose="+root.pose()+" receiver="+snail.position()+" receiverBounds="+snail.getBoundingBox()+" distanceSquared="+root.distanceToSqr(snail)+" lineOfSight="+root.hasLineOfSight(snail)+" receiverPose="+snail.pose()+" threatReady="+snail.threatReady()+" hiddenUntil="+snail.hiddenUntil();}
 private static long[] clocks(SporebackSnail snail){return new long[]{snail.gatherReady(),snail.forageReady(),snail.responseReady(),snail.nurseryReady()};}
 private static void await(ClientGameTestContext c,TestSingleplayerContext w,java.util.function.Predicate<net.minecraft.server.MinecraftServer> yes,String why){for(int i=0;i<240;i++){c.waitTicks(1);if(w.getServer().computeOnServer(yes::test))return;}throw new AssertionError(why);}
 private static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
}
