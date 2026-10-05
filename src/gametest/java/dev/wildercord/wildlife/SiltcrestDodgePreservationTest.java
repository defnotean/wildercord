package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.SiltcrestNative.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.phys.Vec3;
/** Actual unforced fish evasion; finite independent habitat trials never move fish or force a strike. */
public final class SiltcrestDodgePreservationTest implements FabricClientGameTest {
 private SiltcrestBittern bird;private Observation observed;private int calls;
 private record Sample(long tick,int phase,int left,double distanceSqr){}
 private static final class Observation {
  final Cod quarry;final Vec3 committed;final int epoch;Sample first,last,max,escape;
  Observation(Cod quarry,Vec3 committed,int epoch){this.quarry=quarry;this.committed=committed;this.epoch=epoch;}
  boolean active(SiltcrestBittern b){return b.pose()==SiltcrestBittern.COILING&&field(b,"quarry")==quarry&&field(b,"committed")==committed&&(Integer)field(b,"epoch")==epoch;}
  void sample(ServerLevel l,SiltcrestBittern b){
   last=new Sample(l.getGameTime(),b.phase(),(Integer)field(b,"left"),quarry.getBoundingBox().getCenter().distanceToSqr(committed));
   if(first==null)first=last;if(max==null||last.distanceSqr()>max.distanceSqr())max=last;if(escape==null&&last.distanceSqr()>.36)escape=last;
  }
 }
 private final class WitnessCod extends Cod {
  WitnessCod(ServerLevel l){super(EntityTypes.COD,l);}
  @Override public void tick(){
   super.tick();
   // Native fish movement precedes the bird's synchronous final coil/strike/cancel step.
   // Read only a still-live commitment: later movement after cancellation is never evidence.
   if(!(level() instanceof ServerLevel l)||bird==null||bird.level()!=l||bird.pose()!=SiltcrestBittern.COILING||!isAlive()||field(bird,"quarry")!=this)return;
   var committed=(Vec3)field(bird,"committed");if(committed==null)return;
   if(observed==null)observed=new Observation(this,committed,(Integer)field(bird,"epoch"));
   if(observed.active(bird))observed.sample(l,bird);
  }
  @Override public boolean hurtServer(ServerLevel l,DamageSource d,float amount){if(d.getEntity() instanceof SiltcrestBittern)calls++;return super.hurtServer(l,d,amount);}
 }
 public void runTest(ClientGameTestContext c){boolean proved=false;
  for(int trial=0;trial<8&&!proved;trial++)try(var w=c.worldBuilder().create()){
   c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 18000");w.getServer().runCommand("weather clear");
   w.getServer().runOnServer(s->{bird=null;observed=null;calls=0;floor(s.overworld());observer(s.getPlayerList().getPlayers().getFirst(),new Vec3(.5,101.7,.5));for(int i=0;i<3;i++){var fish=new WitnessCod(s.overworld());fish.snapTo(1.5+i,100.1,.5,0,0);s.overworld().addFreshEntity(fish);}bird=bird(s.overworld(),.5,.5);});
   try{
    await(c,w,240,s->observed!=null||calls>0,"Ordinary native stalking reaches a genuine commitment or actual catch");
    await(c,w,24,s->observed==null||!observed.active(bird),"Original finite coil resolves after actual fish movement");
    proved=w.getServer().computeOnServer(s->refusedEscape(s.overworld()));
   }finally{int number=trial+1;w.getServer().runOnServer(s->diagnose(s.overworld(),number));}
  }
  check(proved,"At least one actually observed unforced >.6 fish escape must retain pre-damage refusal and finite failed-attempt rest");
 }
 private boolean sameCancellation(){return observed!=null&&(Integer)field(bird,"epoch")==observed.epoch+1&&field(bird,"quarry")==null&&field(bird,"committed")==null;}
 private boolean refusedEscape(ServerLevel l){
  // Use the final active sample, not a mid-coil excursion that returned before resolution.
  return observed!=null&&observed.last!=null&&observed.last.distanceSqr()>.36&&sameCancellation()&&calls==0&&observed.quarry.isAlive()&&observed.quarry.getHealth()==3&&bird.isAlive()&&bird.pose()==SiltcrestBittern.IDLE&&bird.huntReady()<=l.getGameTime()+200;
 }
 private void diagnose(ServerLevel l,int trial){
  String outcome=refusedEscape(l)?"refused_escape":observed!=null&&observed.escape!=null&&observed.last.distanceSqr()<=.36?"escaped_returned":calls>0?"catch":observed==null||observed.escape==null?"no_escape":"escape_without_refusal";
  System.out.println("SILTCREST_DODGE trial="+trial+" outcome="+outcome+" quarry="+(observed==null?"none":observed.quarry.getUUID())+" committed="+(observed==null?"none":observed.committed)+" epoch="+(observed==null?"none":observed.epoch)+" actualEpoch="+field(bird,"epoch")+" sameCancellation="+sameCancellation()+" first="+(observed==null?"none":observed.first)+" firstEscape="+(observed==null?"none":observed.escape)+" max="+(observed==null?"none":observed.max)+" final="+(observed==null?"none":observed.last)+" now="+l.getGameTime()+" pose="+bird.pose()+" phase="+bird.phase()+" calls="+calls+" alive="+(observed!=null&&observed.quarry.isAlive())+" health="+(observed==null?"none":observed.quarry.getHealth())+" rest="+(bird.huntReady()-l.getGameTime()));
 }
 private static Object field(Object o,String name){try{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
}
