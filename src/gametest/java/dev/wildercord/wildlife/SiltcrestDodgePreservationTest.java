package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.SiltcrestNative.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.phys.Vec3;
import java.util.*;
/** Actual unforced fish evasion; finite independent habitat trials never move fish or force a strike. */
public final class SiltcrestDodgePreservationTest implements FabricClientGameTest {
 private SiltcrestBittern bird;private Observation observed;private Admission admission;private int calls;
 private final List<WitnessCod> fish=new ArrayList<>();
 private record SwimSample(long tick,Vec3 center,boolean navigating){}
 private record Admission(Cod quarry,BlockPos bank,long tick,Vec3 from,Vec3 to,Vec3 routeEnd){}
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
  private final ArrayDeque<SwimSample> swimming=new ArrayDeque<>();
  private double maxSwimProgressSqr;private int navigatingTicks;
  WitnessCod(ServerLevel l){super(EntityTypes.COD,l);}
  @Override public void tick(){
   super.tick();
   if(!(level() instanceof ServerLevel l))return;
   if(bird==null){
    boolean navigating=routeEnd()!=null;if(navigating)navigatingTicks++;
    if(swimming.size()==9)swimming.removeFirst();swimming.addLast(new SwimSample(l.getGameTime(),getBoundingBox().getCenter(),navigating));
    if(swimming.size()==9)maxSwimProgressSqr=Math.max(maxSwimProgressSqr,horizontal(swimming.getLast().center().subtract(swimming.getFirst().center())).lengthSqr());
    return;
   }
   // Native fish movement precedes the bird's synchronous final coil/strike/cancel step.
   // Read only a still-live commitment: later movement after cancellation is never evidence.
   if(bird.level()!=l||bird.pose()!=SiltcrestBittern.COILING||!isAlive()||field(bird,"quarry")!=this)return;
   var committed=(Vec3)field(bird,"committed");if(committed==null)return;
   if(observed==null)observed=new Observation(this,committed,(Integer)field(bird,"epoch"));
   if(observed.active(bird))observed.sample(l,bird);
  }
  private Vec3 routeEnd(){var path=getNavigation().getPath();return getNavigation().isDone()||path==null||path.isDone()||!path.canReach()||path.getNodeCount()==0?null:path.getEntityPosAtNode(this,path.getNodeCount()-1);}
  private boolean sustainedSwim(ServerLevel l){
   if(swimming.size()!=9)return false;
   var first=swimming.getFirst();var last=swimming.getLast();var progress=horizontal(last.center().subtract(first.center()));var end=routeEnd();
   if(last.tick()<l.getGameTime()-1||last.tick()-first.tick()!=8||progress.lengthSqr()<.16||end==null)return false;
   var remaining=horizontal(end.subtract(last.center()));if(remaining.lengthSqr()<=1.44||progress.dot(remaining)<=0)return false;
   SwimSample previous=null;
   for(var sample:swimming){if(!sample.navigating()||previous!=null&&(sample.tick()!=previous.tick()+1||horizontal(sample.center().subtract(previous.center())).dot(progress)<=0))return false;previous=sample;}
   return true;
  }
  private String swimDiagnostic(){return "id="+getUUID()+" body="+position()+" health="+getHealth()+" wild="+SiltcrestBittern.wildFish(this,(ServerLevel)level())+" noAI="+isNoAi()+" navigatingTicks="+navigatingTicks+" maxEightTickProgressSqr="+maxSwimProgressSqr+" first="+swimming.peekFirst()+" last="+swimming.peekLast()+" routeEnd="+routeEnd();}
  @Override public boolean hurtServer(ServerLevel l,DamageSource d,float amount){if(d.getEntity() instanceof SiltcrestBittern)calls++;return super.hurtServer(l,d,amount);}
 }
 private static Vec3 horizontal(Vec3 v){return new Vec3(v.x,0,v.z);}
 private static BlockPos firstDryBank(ServerLevel l,BlockPos origin){
  // These are the first four directions, in order, in the ordinary hunt's bank scan.
  // Only admit a real cardinal shore; no alternate route or goal is supplied to the hunter.
  for(var offset:new int[][]{{0,1},{1,0},{0,-1},{-1,0}})for(int y=-1;y<=1;y++){
   var at=origin.offset(offset[0],y,offset[1]);if(BitternHabitat.bank(l,at)&&l.getFluidState(at).isEmpty())return at;
  }
  return null;
 }
 private boolean admitPredator(ServerLevel l){
  if(fish.size()!=3||fish.stream().anyMatch(f->!SiltcrestBittern.wildFish(f,l)||f.getHealth()!=3||f.isNoAi()))return false;
  for(var candidate:fish){
   if(!candidate.sustainedSwim(l))continue;
   var bank=firstDryBank(l,candidate.blockPosition());if(bank==null||bank.getY()!=101)continue;
   var body=new Vec3(bank.getX()+.5,bank.getY(),bank.getZ()+.5);
   var nearest=fish.stream().min(Comparator.<WitnessCod>comparingDouble(f->f.position().distanceToSqr(body)).thenComparing(f->f.getUUID().toString())).orElseThrow();
   if(nearest!=candidate||candidate.position().distanceToSqr(body)>3.24)continue;
   // Native motion owns admission. Initial placement is the only predator position write.
   admission=new Admission(candidate,bank,l.getGameTime(),candidate.swimming.getFirst().center(),candidate.swimming.getLast().center(),candidate.routeEnd());
   bird=bird(l,body.x,body.z);
   check(bird.blockPosition().equals(bank)&&BitternHabitat.bank(l,bank)&&l.getFluidState(bank).isEmpty(),"Initial predator stands at the real dry bank selected by the normal hunt scan");
   var pool=bird.preyPool(l);check(pool.size()==3&&pool.getFirst()==candidate,"Actual production prey ordering admits the observed swimming quarry and all three healthy wild fish");
   System.out.println("SILTCREST_DODGE_ADMISSION tick="+admission.tick()+" quarry="+candidate.getUUID()+" bank="+bank+" from="+admission.from()+" to="+admission.to()+" eightTickProgressSqr="+horizontal(admission.to().subtract(admission.from())).lengthSqr()+" routeEnd="+admission.routeEnd()+" remainingRouteSqr="+horizontal(admission.routeEnd().subtract(admission.to())).lengthSqr());
   return true;
  }
  return false;
 }
 public void runTest(ClientGameTestContext c){boolean proved=false;
  for(int trial=0;trial<8&&!proved;trial++)try(var w=c.worldBuilder().create()){
   c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 18000");w.getServer().runCommand("weather clear");
   w.getServer().runOnServer(s->{bird=null;observed=null;admission=null;calls=0;fish.clear();floor(s.overworld());observer(s.getPlayerList().getPlayers().getFirst(),new Vec3(.5,101.7,.5));for(int i=0;i<3;i++){var prey=new WitnessCod(s.overworld());prey.snapTo(1.5+i,100.1,.5,0,0);check(s.overworld().addFreshEntity(prey),"Native swimming witness is added to the original pond");fish.add(prey);}});
   try{
    // Candidate fixture: await measured native swimming, never a fixed delay or a supplied impulse.
    await(c,w,500,s->admitPredator(s.overworld()),"Original pond must establish sustained ordinary swimming before predator admission");
    await(c,w,240,s->observed!=null||calls>0,"Ordinary native stalking reaches a genuine commitment or actual catch");
    w.getServer().runOnServer(s->check(observed!=null&&observed.quarry==admission.quarry(),"Actual native hunt commits to the same fish whose own swimming admitted this trial"));
    await(c,w,24,s->observed==null||!observed.active(bird),"Original finite coil resolves after actual fish movement");
    proved=w.getServer().computeOnServer(s->refusedEscape(s.overworld()));
   }finally{int number=trial+1;w.getServer().runOnServer(s->diagnose(s.overworld(),number));}
  }
  check(proved,"At least one actually observed unforced >.6 fish escape must retain pre-damage refusal and finite failed-attempt rest");
 }
 private boolean sameCancellation(){return observed!=null&&(Integer)field(bird,"epoch")==observed.epoch+1&&field(bird,"quarry")==null&&field(bird,"committed")==null;}
 private boolean refusedEscape(ServerLevel l){
  // Use the final active sample, not a mid-coil excursion that returned before resolution.
  return observed!=null&&admission!=null&&observed.quarry==admission.quarry()&&observed.last!=null&&observed.last.distanceSqr()>.36&&sameCancellation()&&calls==0&&observed.quarry.isAlive()&&observed.quarry.getHealth()==3&&bird.isAlive()&&bird.pose()==SiltcrestBittern.IDLE&&bird.huntReady()<=l.getGameTime()+200;
 }
 private void diagnose(ServerLevel l,int trial){
  if(bird==null){System.out.println("SILTCREST_DODGE trial="+trial+" outcome=admission_timeout now="+l.getGameTime()+" calls="+calls+" fish="+fish.stream().map(WitnessCod::swimDiagnostic).toList());return;}
  String outcome=refusedEscape(l)?"refused_escape":observed!=null&&observed.escape!=null&&observed.last.distanceSqr()<=.36?"escaped_returned":calls>0?"catch":observed==null||observed.escape==null?"no_escape":"escape_without_refusal";
  System.out.println("SILTCREST_DODGE trial="+trial+" outcome="+outcome+" quarry="+(observed==null?"none":observed.quarry.getUUID())+" committed="+(observed==null?"none":observed.committed)+" epoch="+(observed==null?"none":observed.epoch)+" actualEpoch="+field(bird,"epoch")+" sameCancellation="+sameCancellation()+" first="+(observed==null?"none":observed.first)+" firstEscape="+(observed==null?"none":observed.escape)+" max="+(observed==null?"none":observed.max)+" final="+(observed==null?"none":observed.last)+" now="+l.getGameTime()+" pose="+bird.pose()+" phase="+bird.phase()+" calls="+calls+" alive="+(observed!=null&&observed.quarry.isAlive())+" health="+(observed==null?"none":observed.quarry.getHealth())+" rest="+(bird.huntReady()-l.getGameTime()));
 }
 private static Object field(Object o,String name){try{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
}
